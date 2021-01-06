package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.databinding.DataBindingUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.*


class SelectLocationFragment : BaseFragment() {

    companion object {
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
        private const val DEFAULT_ZOOM = 15f
    }

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var _map: GoogleMap

    private var marker: Marker? = null // To mark the location
    private var markerOptions: MarkerOptions? = null // to get reminder location

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        Timber.d("onCreateView()")
        binding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

//      add the map setup implementation
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        if (!_viewModel.reminderSelectedLocationStr.value.isNullOrEmpty()) {
            val snippet = String.format(
                    Locale.getDefault(),
                    getString(R.string.lat_long_snippet),
                    _viewModel.latitude.value, _viewModel.longitude.value)
            val latLng = LatLng(_viewModel.latitude.value
                    ?: 37.9337293539, _viewModel.longitude.value ?: -122.143383042)

            markerOptions = MarkerOptions()
                    .title(_viewModel.reminderSelectedLocationStr.value)
                    .snippet(snippet)
                    .position(latLng)
        }

        mapFragment.getMapAsync { googleMap ->
            _map = googleMap

//          zoom to the user location after taking his permission
            markerOptions?.let { options ->
                _map.moveCamera(CameraUpdateFactory.newLatLngZoom(options.position, DEFAULT_ZOOM))
                marker = _map.addMarker(options)
            }

//          add style to the map
            setMapStyle(googleMap)

//            put a marker to location that the user selected
            setPoiClick(googleMap)
            setMapLongClick(googleMap)
            enableMyLocation()
        }

//      call this function after the user confirms on the selected location
        binding.fabConfirm.setOnClickListener { onLocationSelected() }

        return binding.root
    }

    /*
     * Enable or Request location permission, so that we can get the location of the
     * device. The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (isLocationPermissionApproved()) {
            _map.isMyLocationEnabled = true
        }
        else
            requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
    }

    // Check location permission
    private fun isLocationPermissionApproved() : Boolean {
        return (checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    enableMyLocation()
            }
        }
    }

    // Set style on map
    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style)
            )
            if (!success) Timber.e(getString(R.string.error_style_parsing_failed))
        } catch (ex: Resources.NotFoundException) {
            Timber.e(getString(R.string.error_style, ex))
        }
    }

    // set Point of Interest: Mark and put their name
    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            if (marker == null)
                marker = map.addMarker(
                        MarkerOptions()
                                .position(poi.latLng)
                                .title(poi.name)
                )
            else {
                marker?.apply {
                    position = poi.latLng
                    title = poi.name
                    snippet = null
                }
            }

            Timber.d("Point of interest: ${poi.placeId}")
            marker?.showInfoWindow()
        }
    }

    // Add marker when user longClick
    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            val snippet = String.format(
                    Locale.getDefault(),
                    getString(R.string.lat_long_snippet),
                    latLng.latitude, latLng.longitude)

            if (marker == null)
                marker = map.addMarker(
                        MarkerOptions()
                                .position(latLng)
                                .title(getString(R.string.dropped_pin))
                                .snippet(snippet))
            else {
                marker?.apply {
                    position = latLng
                    title = getString(R.string.dropped_pin)
                    this.snippet = snippet
                }
            }
            // Because InfoWindow is not reDraw automatically, so we have to call it everytime
            marker?.hideInfoWindow()
        }
    }

    private fun onLocationSelected() {
//                 When the user confirms on the selected location,
//                 send back the selected location details to the view model
//                 and navigate back to the previous fragment to save the reminder and add the geofence
        if (marker == null)
            Toast.makeText(requireContext(), getString(R.string.select_location), Toast.LENGTH_LONG).show()
        else {
            _viewModel.setLocation(marker!!)
            _viewModel.navigationCommand.value = NavigationCommand.Back
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
//      Change the map type based on the user's selection.
        R.id.normal_map -> {
            _map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            _map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            _map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            _map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("onDestroyView()")
    }
}
