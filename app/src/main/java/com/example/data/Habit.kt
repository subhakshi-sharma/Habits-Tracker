package com.example.data

import androidx.room.*

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetGoal: Int, // e.g. 20 (complete at least 20 days)
    val colorHex: String, // Hex string representation (e.g. "#4CAF50")
    val year: Int,
    val month: Int // 1-indexed (1 to 12)
)

@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "dayOfMonth"],
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("habitId")]
)
data class HabitCompletion(
    val habitId: Int,
    val dayOfMonth: Int
)

data class HabitWithCompletions(
    @Embedded val habit: Habit,
    @Relation(
        parentColumn = "id",
        entityColumn = "habitId"
    )
    val completions: List<HabitCompletion>
)
