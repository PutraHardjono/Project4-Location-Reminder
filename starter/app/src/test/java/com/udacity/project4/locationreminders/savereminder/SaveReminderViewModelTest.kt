package com.udacity.project4.locationreminders.savereminder

import android.content.Context
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.Marker
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class SaveReminderViewModelTest {
    // provide testing to the SaveReminderView and its live data objects

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var saveReminderViewModel: SaveReminderViewModel
    private lateinit var reminderDataSource: FakeDataSource

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setupViewModel() {
        stopKoin()
        reminderDataSource = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(getApplicationContext(), reminderDataSource)
    }

    @Test
    fun validateEnteredData_shouldReturnSuccess() {
        // GIVEN - correct ReminderDataItem
        val successReminder = ReminderDataItem("TITLE2","DESCRIPTION2", "LOCATION2")

        // When - Validating
        val valid = saveReminderViewModel.validateEnteredData(successReminder)

        // Then, it should return true
        assertThat(valid, `is`(true ))
    }

    @Test
    fun validateEnteredData_shouldReturnError() {
        // GIVEN - error ReminderDataItem
        val errorReminder1 = ReminderDataItem()
        val errorReminder2 = ReminderDataItem("TITLE1", "DESCRIPTION2")

        // Condition 1 - Error enter title
        val invalid1 = saveReminderViewModel.validateEnteredData(errorReminder1)
        var showSnackBarInt: Int? = saveReminderViewModel.showSnackBarInt.getOrAwaitValue()
        assertThat(invalid1, `is`(false ))
        assertThat(showSnackBarInt, `is`(R.string.err_enter_title))

        // Condition 2 - Error select Location
        val invalid2 = saveReminderViewModel.validateEnteredData(errorReminder2)
        showSnackBarInt = saveReminderViewModel.showSnackBarInt.getOrAwaitValue()
        assertThat(invalid2, `is`(false ))
        assertThat(showSnackBarInt, `is`(R.string.err_select_location))
    }

    @Test
    fun saveReminder_retrieveReminder() = mainCoroutineRule.runBlockingTest {
        // GIVEN - ReminderDataItem
        val reminder = ReminderDataItem("TITLE1","DESCRIPTION1", "LOCATION1")
        val reminder2 = ReminderDataItem("TITLE2","DESCRIPTION2", "LOCATION2")

        // Then - save the reminder
        saveReminderViewModel.saveReminder(reminder)
        saveReminderViewModel.saveReminder(reminder2)
        val navigationToast = saveReminderViewModel.showToast.getOrAwaitValue()
        val toastMessage =  getApplicationContext<Context>().getString(R.string.reminder_saved)

        // Check Toast
        assertThat(navigationToast, `is`(toastMessage))

        // Check list
        val list = reminderDataSource.reminders
        assertThat(list?.size, `is`(2))
        assertThat(list?.get(0)?.id, `is`(reminder.id))
        assertThat(list?.get(0)?.title, `is`(reminder.title))
        assertThat(list?.get(1)?.id, `is`(reminder2.id))
        assertThat(list?.get(1)?.title, `is`(reminder2.title))
    }

    @Test
    fun validateAndSaveReminder_onError() = mainCoroutineRule.runBlockingTest {
        // GIVEN - ReminderDataItem
        val errorReminder1 = ReminderDataItem()
        val errorReminder2 = ReminderDataItem("TITLE1", "DESCRIPTION2")

        // Then - save the reminder
        saveReminderViewModel.validateAndSaveReminder(errorReminder1)
        saveReminderViewModel.validateAndSaveReminder(errorReminder2)

        // check list
        val list = reminderDataSource.reminders
        assertThat(list?.size, `is`(0))
    }

    @Test
    fun validateAndSaveReminder_onSuccess() = mainCoroutineRule.runBlockingTest {
        // GIVEN - ReminderDataItem
        val successReminder = ReminderDataItem("TITLE2","DESCRIPTION2", "LOCATION2")

        // Then - save the reminder
        saveReminderViewModel.validateAndSaveReminder(successReminder)

        // check list
        val list = reminderDataSource.reminders
        assertThat(list?.size, `is`(1))
        assertThat(list?.get(0)?.id, `is`(successReminder.id))
        assertThat(list?.get(0)?.title, `is`(successReminder.title))
    }

    @Test
    fun saveReminder_checkLoading() {
        // GIVEN - ReminderDataItem
        val reminder = ReminderDataItem("TITLE1","DESCRIPTION1", "LOCATION1")

        // Pause dispatcher so you can verify initial values.
        mainCoroutineRule.pauseDispatcher()

        // Then - save the reminder
        saveReminderViewModel.saveReminder(reminder)
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Execute pending coroutines actions.
        mainCoroutineRule.resumeDispatcher()
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun onClear() = mainCoroutineRule.runBlockingTest {
        // GIVEN - Livedata
        saveReminderViewModel.longitude.value = 101.1
        saveReminderViewModel.latitude.value = 101.2
        saveReminderViewModel.reminderSelectedLocationStr.value = "LOCATION1"
        saveReminderViewModel.reminderTitle.value = "TITLE1"
        saveReminderViewModel.reminderDescription.value = "DESCRIPTION"

        // Then - clear
        saveReminderViewModel.onClear()
        val location = saveReminderViewModel.reminderSelectedLocationStr.getOrAwaitValue()
        val latitude = saveReminderViewModel.latitude.getOrAwaitValue()
        val longitude = saveReminderViewModel.longitude.getOrAwaitValue()
        val title = saveReminderViewModel.reminderTitle.getOrAwaitValue()
        val description = saveReminderViewModel.reminderDescription.getOrAwaitValue()

        // check liveData value
        assertThat(location, nullValue())
        assertThat(latitude, nullValue())
        assertThat(longitude, nullValue())
        assertThat(title, nullValue())
        assertThat(description, nullValue())
    }
}