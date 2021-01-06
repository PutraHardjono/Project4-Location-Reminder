package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.di.roomTestModule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
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
//Unit test the DAO
@SmallTest
class RemindersDaoTest: KoinTest {

//  Add testing implementation to the RemindersDao.kt
    @get: Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val database: RemindersDatabase by inject()
    private val reminderDao: RemindersDao by inject()

    @Before
    fun setup() {
        stopKoin() // to remove Koin Application has already been started
        startKoin {
            androidContext(getApplicationContext())
            loadKoinModules( listOf(roomTestModule))
        }
    }

    @After
    fun clearUp() {
        database.close()
        stopKoin()
    }

    @Test
    fun saveReminderAndGetReminderById() = runBlockingTest {
        // GIVEN - Insert a reminder.
        val reminder = ReminderDTO("Title1", "Description1",
                "Pak Datuk", 1.6765196982043675, 101.44888919150442)
        reminderDao.saveReminder(reminder)

        // WHEN - Get reminder by id from the database.
        val loaded = reminderDao.getReminderById(reminder.id )

        // THEN - The loaded data contains the expected values.
        assertThat<ReminderDTO>(loaded as ReminderDTO, notNullValue())
        assertThat(loaded.id, `is`(reminder.id))
        assertThat(loaded.title, `is`(reminder.title))
        assertThat(loaded.description, `is`(reminder.description))
        assertThat(loaded.location, `is`(reminder.location))
        assertThat(loaded.latitude, `is`(reminder.latitude))
        assertThat(loaded.longitude, `is`(reminder.longitude))
    }

    @Test
    fun saveAndGetReminders() = runBlockingTest {
        // GIVEN - Insert a reminderDTO.
        val reminder = ReminderDTO("Title1", "Description1",
                "Pak Datuk", 1.6765196982043675, 101.44888919150442)
        val reminder2 = ReminderDTO("Title2", "Description2",
                "Pak Datuk", 11.6765196982043675, 111.44888919150442)
        val reminder3 = ReminderDTO("Title3", "Description3",
                "Pak Datuk", 12.6765196982043675, 121.44888919150442)
        reminderDao.saveReminder(reminder)
        reminderDao.saveReminder(reminder2)
        reminderDao.saveReminder(reminder3)

        // WHEN - Delete and get a list of reminder from the database.
        val list = reminderDao.getReminders()

        // THEN - The loaded data contains the expected values.
        assertThat(list.size, `is` (3))
        assertThat(list.first().id, `is`(reminder.id))
        assertThat(list.last().id, `is`(reminder3.id))
    }

    @Test
    fun saveDeleteAndGetReminders() = runBlockingTest {
        // GIVEN - Insert a reminderDTO.
        val reminder = ReminderDTO("Title1", "Description1",
                "Pak Datuk", 1.6765196982043675, 101.44888919150442)
        val reminder2 = ReminderDTO("Title2", "Description2",
                "Pak Datuk", 11.6765196982043675, 111.44888919150442)
        val reminder3 = ReminderDTO("Title3", "Description3",
                "Pak Datuk", 12.6765196982043675, 121.44888919150442)
        reminderDao.saveReminder(reminder)
        reminderDao.saveReminder(reminder2)
        reminderDao.saveReminder(reminder3)

        // WHEN - Delete and get a list of reminder from the database.
        reminderDao.deleteAllReminders()
        val list = reminderDao.getReminders()

        // THEN - The loaded data contains the expected values.
        assertThat(list.size, `is`(0))
    }
}