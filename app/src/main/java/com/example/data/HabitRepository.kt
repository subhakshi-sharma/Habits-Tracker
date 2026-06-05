package com.example.data

import kotlinx.coroutines.flow.Flow

class HabitRepository(private val habitDao: HabitDao) {
    fun getHabitsWithCompletions(year: Int, month: Int): Flow<List<HabitWithCompletions>> {
        return habitDao.getHabitsWithCompletions(year, month)
    }

    suspend fun insertHabit(habit: Habit): Long {
        return habitDao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteHabit(habit)
    }

    suspend fun toggleCompletion(habitId: Int, day: Int, isChecked: Boolean) {
        val completion = HabitCompletion(habitId, day)
        if (isChecked) {
            habitDao.insertCompletion(completion)
        } else {
            habitDao.deleteCompletion(completion)
        }
    }

    suspend fun getHabitsForMonthDirect(year: Int, month: Int): List<Habit> {
        return habitDao.getHabitsForMonthDirect(year, month)
    }

    suspend fun duplicateHabitsToNewMonth(
        sourceYear: Int,
        sourceMonth: Int,
        targetYear: Int,
        targetMonth: Int
    ) {
        val calendar = java.util.Calendar.getInstance().apply {
            set(targetYear, targetMonth - 1, 1)
        }
        val maxDays = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

        val sourceHabits = habitDao.getHabitsForMonthDirect(sourceYear, sourceMonth)
        sourceHabits.forEach { template ->
            val duplicate = Habit(
                name = template.name,
                targetGoal = template.targetGoal.coerceIn(1, maxDays),
                colorHex = template.colorHex,
                year = targetYear,
                month = targetMonth
            )
            habitDao.insertHabit(duplicate)
        }
    }
}
