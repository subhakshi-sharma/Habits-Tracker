package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Habit
import com.example.data.HabitRepository
import com.example.data.HabitWithCompletions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class HabitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HabitRepository

    // Selected Year and Month (1-indexed, e.g., 1 for Jan, 5 for May)
    private val _selectedYear = MutableStateFlow(2026)
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(5) // Default to May based on CURRENT local time metadata
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = HabitRepository(database.habitDao())

        // Initialize based on current local calendar
        val calendar = Calendar.getInstance()
        _selectedYear.value = calendar.get(Calendar.YEAR)
        _selectedMonth.value = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-indexed
    }

    // Reactively load habits and their completions for the selected year & month
    val habits: StateFlow<List<HabitWithCompletions>> = combine(
        _selectedYear,
        _selectedMonth
    ) { year, month ->
        Pair(year, month)
    }.flatMapLatest { (year, month) ->
        repository.getHabitsWithCompletions(year, month)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun changeMonth(year: Int, month: Int) {
        _selectedYear.value = year
        // Clamp month to 1..12
        _selectedMonth.value = month.coerceIn(1, 12)
    }

    fun addHabit(name: String, targetGoal: Int, colorHex: String) {
        viewModelScope.launch {
            val newHabit = Habit(
                name = name,
                targetGoal = targetGoal,
                colorHex = colorHex,
                year = _selectedYear.value,
                month = _selectedMonth.value
            )
            repository.insertHabit(newHabit)
        }
    }

    fun updateHabit(habitId: Int, name: String, targetGoal: Int, colorHex: String) {
        viewModelScope.launch {
            val updated = Habit(
                id = habitId,
                name = name,
                targetGoal = targetGoal,
                colorHex = colorHex,
                year = _selectedYear.value,
                month = _selectedMonth.value
            )
            repository.updateHabit(updated)
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }

    fun toggleCompletion(habitId: Int, day: Int, isChecked: Boolean) {
        viewModelScope.launch {
            repository.toggleCompletion(habitId, day, isChecked)
        }
    }

    fun duplicateHabitsToNewMonth(targetYear: Int, targetMonth: Int) {
        viewModelScope.launch {
            repository.duplicateHabitsToNewMonth(
                sourceYear = _selectedYear.value,
                sourceMonth = _selectedMonth.value,
                targetYear = targetYear,
                targetMonth = targetMonth
            )
            // Automatically switch to the newly duplicated month
            changeMonth(targetYear, targetMonth)
        }
    }
}
