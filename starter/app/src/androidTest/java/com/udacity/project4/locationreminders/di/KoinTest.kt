package com.udacity.project4.locationreminders.di

import androidx.room.Room
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

// In Memory room database definition
val roomTestModule = module {

    viewModel {
        RemindersListViewModel(
                get(),
                get() as ReminderDataSource
        )
    }

    // Test Room Database
    single {
        Room.inMemoryDatabaseBuilder(get(), RemindersDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    single { RemindersLocalRepository(get()) as ReminderDataSource }

    // Test reminder dao
    factory { get<RemindersDatabase>().reminderDao() }
}