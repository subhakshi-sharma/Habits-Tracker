package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Transaction
    @Query("SELECT * FROM habits WHERE year = :year AND month = :month ORDER BY id ASC")
    fun getHabitsWithCompletions(year: Int, month: Int): Flow<List<HabitWithCompletions>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletion)

    @Delete
    suspend fun deleteCompletion(completion: HabitCompletion)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId")
    suspend fun clearCompletionsForHabit(habitId: Int)

    @Query("SELECT * FROM habits WHERE year = :year AND month = :month")
    suspend fun getHabitsForMonthDirect(year: Int, month: Int): List<Habit>
}
