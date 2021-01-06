package com.udacity.project4.locationreminders.reminderslist

import android.Manifest
import android.annotation.TargetApi
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.*
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentRemindersBinding
import com.udacity.project4.utils.AuthenticationState
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.setTitle
import com.udacity.project4.utils.setup
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1

class ReminderListFragment : BaseFragment() {
    //use Koin to retrieve the ViewModel instance
    override val _viewModel: RemindersListViewModel by viewModel()
    private lateinit var binding: FragmentRemindersBinding

    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_reminders, container, false
        )
        binding.viewModel = _viewModel

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(false)
        setTitle(getString(R.string.app_name))

        binding.refreshLayout.setOnRefreshListener { _viewModel.loadReminders() }

        _viewModel.remindersList.observe(viewLifecycleOwner) { list ->
            for (item in list)
                Timber.i("reminders id: ${item.id}")
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        setupRecyclerView()
        binding.addReminderFAB.setOnClickListener { navigateToAddReminder() }

        // Check user authentication
        _viewModel.authenticationState.observe(viewLifecycleOwner) { authenticationState ->
            when(authenticationState) {
                AuthenticationState.AUTHENTICATED -> checkPermission()
                else -> findNavController().navigate(ReminderListFragmentDirections.actionReminderListFragmentToAuthenticationActivity())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        //load the reminders list on the ui
        _viewModel.loadReminders()
    }

    private fun navigateToAddReminder() {
        //use the navigationCommand live data to navigate between the fragments
        _viewModel.navigationCommand.postValue(
                NavigationCommand.To(
                        ReminderListFragmentDirections.toSaveReminder()
                )
        )
    }

    private fun setupRecyclerView() {
        val adapter = RemindersListAdapter {
        }

//        setup the recycler view using the extension function
        binding.reminderssRecyclerView.setup(adapter)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logout -> {
//              add the logout implementation
                AuthUI
                        .getInstance()
                        .signOut(requireContext())
                        .addOnCompleteListener {
                            findNavController().navigate(
                                    ReminderListFragmentDirections.actionReminderListFragmentToAuthenticationActivity())
                        }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
//        display logout as menu item
        inflater.inflate(R.menu.main_menu, menu)
    }

    private fun checkPermission() {
        Timber.i("isForegroundAndBackgroundLocationPermissionApproved(): ${isForegroundAndBackgroundLocationPermissionApproved()}")
        if (isForegroundAndBackgroundLocationPermissionApproved())
            checkDeviceLocationSetting()
        else
            requestForegroundAndBackgroundLocationPermission()
    }

    /*
     *  Determines whether the app has the appropriate permissions across Android 10+ and all other
     *  Android versions.
     */
    @TargetApi(29)
    private fun isForegroundAndBackgroundLocationPermissionApproved(): Boolean {
        val isForegroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION))

        val isBackgroundPermissionApproved =
                if (runningQOrLater)
                    PackageManager.PERMISSION_GRANTED ==
                            checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                else true

        return isForegroundLocationApproved && isBackgroundPermissionApproved
    }

    /*
     *  Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn on location services within our app.
     */
    private fun checkDeviceLocationSetting(resolve: Boolean = true) {
        Timber.i("checkDeviceLocationSetting(resolve: $resolve)")
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            Timber.e("locationSettingsResponseTask.addOnFailureListener exception ${exception.message}")
            if (exception is ResolvableApiException && resolve) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    Timber.i("checkDeviceLocationSetting(resolve: $resolve) - resolution")
                    startIntentSenderForResult(
                            exception.resolution.intentSender,
                            REQUEST_TURN_DEVICE_LOCATION_ON,
                            null,0,0,0,null)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Timber.e(getString(R.string.error_location_setting_resolution, sendEx.message))
                }
            } else {
                Snackbar.make(
                        binding.refreshLayout,
                        getString(R.string.location_required_error), Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSetting()
                }.show()
            }
        }
    }

    /*
     *  Requests ACCESS_FINE_LOCATION and (on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.
     */
    @TargetApi(29 )
    private fun requestForegroundAndBackgroundLocationPermission() {
        if (isForegroundAndBackgroundLocationPermissionApproved()) return

        // Else request the permission
        // this provides the result[LOCATION_PERMISSION_INDEX]
        var permissionArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        val resultCode = when {
            runningQOrLater -> {
                // this provides the result[BACKGROUND_LOCATION_PERMISSION_INDEX]
                permissionArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else ->
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }

        Timber.i("requestForegroundAndBackgroundLocationPermission() - resultCode: $resultCode")
        requestPermissions(permissionArray, resultCode)
    }

    /*
    *  When we get the result from asking the user to turn on device location, we call
    *  checkDeviceLocationSettingsAndStartGeofence again to make sure it's actually on, but
    *  we don't resolve the check to keep the user from seeing an endless loop.
    */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Timber.i("onActivityResult: $requestCode")

        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON)
            checkDeviceLocationSetting(false)
    }


    /*
     * In all cases, we need to have the location permission.  On Android 10+ (Q) we need to have
     * the background permission as well.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Timber.i("onRequestPermissionsResult")

        if (grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED)) {

            Snackbar.make(
                    binding.refreshLayout,
                    getString(R.string.permission_denied_explanation),
                    Snackbar.LENGTH_INDEFINITE
            ).setAction("Settings") {
                // Displays App settings screen
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }.show()
        } else
            checkDeviceLocationSetting()
    }

    @VisibleForTesting
    fun testShowErrorMessage(errorMessage: String){
        _viewModel.setShowErrorMessage(errorMessage)
    }

    @VisibleForTesting
    fun testShowToast(message: String) {
        _viewModel.setShowToast(message)
    }

    @VisibleForTesting
    fun testShowSnackBar(message: String) {
        _viewModel.setShowSnackBar(message)
    }

    @VisibleForTesting
    fun testShowSnackBarInt(stringId: Int) {
        _viewModel.setShowSnackBarInt(stringId)
    }
}
