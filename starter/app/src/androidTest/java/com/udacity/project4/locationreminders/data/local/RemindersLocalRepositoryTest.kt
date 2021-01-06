package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.di.roomTestModule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest: KoinTest {

//  Add testing implementation to the RemindersLocalRepository.kt
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private val database: RemindersDatabase by inject()
    private val repo: ReminderDataSource by inject()

    @Before
    fun setup() {
        stopKoin()
        startKoin {
            androidContext(getApplicationContext())
            loadKoinModules(roomTestModule)
        }
    }

    @After
    fun clearUp() {
        database.close()
        stopKoin()
    }

    @Test
    fun saveReminderAndGetReminderById() = runBlocking {
        // GIVEN - Insert a reminderDTO.
        val reminder = ReminderDTO("Title1", "Description1",
            "Pak Datuk", 1.6765196982043675, 101.44888919150442)
        repo.saveReminder(reminder)

        // WHEN - Get reminder by id from the database.
        val result = repo.getReminder(reminder.id )

        // THEN - The loaded data contains the expected values.
        assertThat(result is Result.Success, `is`(true))
        if (result is Result.Success) {
            assertThat<ReminderDTO>(result.data, CoreMatchers.notNullValue())
            assertThat(result.data.id, `is`(reminder.id))
            assertThat(result.data.title, `is`(reminder.title))
            assertThat(result.data.description, `is`(reminder.description))
            assertThat(result.data.location, `is`(reminder.location))
            assertThat(result.data.latitude, `is`(reminder.latitude))
            assertThat(result.data.longitude, `is`(reminder.longitude))
        }
    }

    @Test
    fun saveAndGetReminders() = runBlocking {
        // GIVEN - Insert a reminderDTO.
        val reminder = ReminderDTO("Title1", "Description1",
            "Pak Datuk", 1.6765196982043675, 101.44888919150442)
        val reminder2 = ReminderDTO("Title2", "Description2",
            "Pak Datuk", 11.6765196982043675, 111.44888919150442)
        val reminder3 = ReminderDTO("Title3", "Description3",
            "Pak Datuk", 12.6765196982043675, 121.44888919150442)
        repo.saveReminder(reminder)
        repo.saveReminder(reminder2)
        repo.saveReminder(reminder3)

        // WHEN - Get a list of reminder from the database.
        val result = repo.getReminders()

        // THEN - The loaded data contains the expected values.
        assertThat(result is Result.Success, `is`(true))
        if (result is Result.Success) {
            assertThat(result.data.size, `is` (3))
            assertThat(result.data.first().id, `is`(reminder.id))
            assertThat(result.data.last().id, `is`(reminder3.id))
        }
    }

    @Test
    fun saveDeleteAndGetReminders() = runBlocking {
        // GIVEN - Insert a reminderDTO.
        val reminder = ReminderDTO("Title1", "Description1",
            "Pak Datuk", 1.6765196982043675, 101.44888919150442)
        val reminder2 = ReminderDTO("Title2", "Description2",
            "Pak Datuk", 11.6765196982043675, 111.44888919150442)
        val reminder3 = ReminderDTO("Title3", "Description3",
            "Pak Datuk", 12.6765196982043675, 121.44888919150442)
        repo.saveReminder(reminder)
        repo.saveReminder(reminder2)
        repo.saveReminder(reminder3)

        // WHEN - Delete and get a list of reminder from the database.
        repo.deleteAllReminders()
        val result = repo.getReminders()

        // THEN - The loaded data contains the expected values.
        assertThat(result is Result.Success, `is`(true))
        if (result is Result.Success) {
            assertThat(result.data.size, `is` (0))
        }
    }
}