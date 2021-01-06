package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource (var reminders: MutableList<ReminderDTO>? = mutableListOf()) : ReminderDataSource {

    companion object {
        const val ERROR_MESSAGE = "Reminder not found"
    }

    private var shouldReturnError = false

//  Create a fake data source to act as a double to the real data source
    fun setShouldReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (!shouldReturnError)
            reminders?.let { return Result.Success(ArrayList(it)) }
        return Result.Error(ERROR_MESSAGE)
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        val result = reminders?.find { it.id == id }
        return if (result != null)
            Result.Success(result)
        else
            Result.Error(ERROR_MESSAGE)
    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }


}