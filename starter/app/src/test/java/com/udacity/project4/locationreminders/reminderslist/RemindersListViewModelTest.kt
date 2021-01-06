package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.P])
class RemindersListViewModelTest {

    //provide testing to the RemindersListViewModel and its live data objects

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var reminderListViewModel: RemindersListViewModel
    private lateinit var reminderDataSource: FakeDataSource

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setupViewModel() = runBlocking {
        stopKoin()
        FirebaseApp.initializeApp(getApplicationContext())
        reminderDataSource = FakeDataSource()
        reminderListViewModel = RemindersListViewModel(getApplicationContext(), reminderDataSource)
    }

    @Test
    fun loadReminders_checkReminderList() = mainCoroutineRule.runBlockingTest {
        // GIVEN - Insert reminderDTO.
        val reminder = ReminderDTO("Title1", "Description1",
            "Pak Datuk", 1.6765196982043675, 101.44888919150442)
        val reminder2 = ReminderDTO("Title2", "Description2",
            "Lala", 11.6765196982043675, 111.44888919150442)
        val reminder3 = ReminderDTO("Title3", "Description3",
            "Tata", 12.6765196982043675, 121.44888919150442)
        reminderDataSource.saveReminder(reminder)
        reminderDataSource.saveReminder(reminder2)
        reminderDataSource.saveReminder(reminder3)

        // Then - Trigger loadReminders() and get the list of reminders from livedata.
        reminderListViewModel.loadReminders()
        val list = reminderListViewModel.remindersList.getOrAwaitValue()

        // THEN - The reminderList contains the expected values.
        assertThat(list.size, `is`(3))
        assertThat(list.first().id, `is`(reminder.id))
        assertThat(list.last().id, `is`(reminder3.id))
    }

    @Test
    fun loadReminders_shouldReturnError()  = mainCoroutineRule.runBlockingTest {
        // Make the repository return errors.
        reminderDataSource.setShouldReturnError(true)
        reminderListViewModel.loadReminders()

        // Then showSnackBar is showing error message
        assertThat(reminderListViewModel.showSnackBar.getOrAwaitValue(), `is`(FakeDataSource.ERROR_MESSAGE))
    }

    @Test
    fun loadReminders_checkLoading() {
        // Pause dispatcher so you can verify initial values.
        mainCoroutineRule.pauseDispatcher()

        reminderListViewModel.loadReminders()
        assertThat(reminderListViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Execute pending coroutines actions.
        mainCoroutineRule.resumeDispatcher()
        assertThat(reminderListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun loadReminders_emptyReminder_showNoData() = mainCoroutineRule.runBlockingTest {
        // Then - Trigger loadReminders() and get the list of reminders from livedata.
        reminderListViewModel.loadReminders()

        // THEN - list should be empty and show no data
        assertThat(reminderListViewModel.showNoData.value, `is`(true))
        assertThat(reminderListViewModel.remindersList.getOrAwaitValue().isNullOrEmpty(), `is`(true))
    }
}