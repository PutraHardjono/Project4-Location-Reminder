package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.di.roomTestModule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.not
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
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify


@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
/**
* Before running this test, make sure you have login in the app.
* */
class ReminderListFragmentTest: KoinTest {

//  test the navigation of the fragments.
//  test the displayed data on the UI.
//  add testing for the error messages.
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var activityRule = ActivityTestRule(RemindersActivity::class.java)

    // To grant permission
    @get:Rule
    var permissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)

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
    }

    @Test
    fun clickAddReminderButton_navigateToSaveReminder() {
        // GIVEN - On the home screen
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // WHEN - Click on the "+" button
        onView(withId(R.id.addReminderFAB)).perform(click())

        // THEN - Verify that we navigate to the add Reminder
        verify(navController).navigate(
                ReminderListFragmentDirections.toSaveReminder()
        )
    }

    @Test
    fun noDataReminderList_DisplayUI() {
        // WHEN - ReminderListFragment launched to display Reminder with empty reminder
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // THEN - No Data should appear on the screen
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun reminderList_DisplayUi()  {
        runBlocking {
            // GIVEN - Add reminder
            val reminder = ReminderDTO("Title1", "Description1",
                    "Testing1", 1.6765196982043675, 101.44888919150442)
            val reminder2 = ReminderDTO("Title2", "Description2",
                    "Testing2", 11.6765196982043675, 111.44888919150442)
            repo.saveReminder(reminder)
            repo.saveReminder(reminder2)

            // WHEN - ReminderListFragment launched to display Reminder
            launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

            onView(withId(R.id.reminderssRecyclerView))
                    .check(matches(atPosition(0, hasDescendant(withText("Title1")))))
            onView(withId(R.id.reminderssRecyclerView))
                    .check(matches(atPosition(0, hasDescendant(withText("Description1")))))
            onView(withText("Title2")).check(matches(isDisplayed()))
            onView(withText("Description2")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun showErrorMessage() {
        // WHEN - ReminderListFragment launched to display Reminder with empty reminder
        val errorMessage = "This is error message"
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        scenario.onFragment {
            it.testShowErrorMessage(errorMessage)
        }

        // THEN - check Toast ErrorMessage is displayed
        onView(withText(errorMessage)).inRoot(withDecorView(not(activityRule.activity.window.decorView))).check(matches(isDisplayed()))
    }

    @Test
    fun showToast() {
        // WHEN - ReminderListFragment launched to display Reminder with empty reminder
        val message = "This is a amessage"
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        scenario.onFragment { it.testShowToast(message) }

        // THEN - check ToastMessage is displayed
        onView(withText(message)).inRoot(withDecorView(not(activityRule.activity.window.decorView))).check(matches(isDisplayed()))
    }

    @Test
    fun showSnackbar() {
        // WHEN - ReminderListFragment launched to display Reminder with empty reminder
        val message = "This is a amessage"
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        scenario.onFragment { it.testShowSnackBar(message) }

        // THEN - check Snackbar is displayed
        onView(withId(com.google.android.material.R.id.snackbar_text))
                .check(matches(withText(message)))
    }

    @Test
    fun showSnackbarInt() {
        // WHEN - ReminderListFragment launched to display Reminder with empty reminder
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        scenario.onFragment { it.testShowSnackBarInt(R.string.error_happened) }

        // THEN - check Snackbar is displayed
        onView(withId(com.google.android.material.R.id.snackbar_text))
                .check(matches(withText(R.string.error_happened)))
    }
}

// https://stackoverflow.com/questions/31394569/how-to-assert-inside-a-recyclerview-in-espresso
fun atPosition(position: Int, itemMatcher: Matcher<View?>): Matcher<View?> {
    return object : BoundedMatcher<View?, RecyclerView>(RecyclerView::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("has item at position $position: ")
            itemMatcher.describeTo(description)
        }

        override fun matchesSafely(view: RecyclerView): Boolean {
            val viewHolder = view.findViewHolderForAdapterPosition(position)
                    ?: // has no item on such position
                    return false
            return itemMatcher.matches(viewHolder.itemView)
        }
    }
}
