package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Habit
import com.example.data.HabitWithCompletions
import java.util.Calendar

// Predefined palette of modern colors
val CustomColorPalette = listOf(
    "#2ECC71" to "Emerald Green",
    "#00BEC4" to "Vibrant Teal",
    "#3498DB" to "Sky Blue",
    "#5C6BC0" to "Royal Indigo",
    "#9C27B0" to "Deep Purple",
    "#E67E22" to "Sunset Orange",
    "#FF6B81" to "Coral Pink",
    "#E74C3C" to "Crimson Red"
)

fun parseHexColor(hex: String, fallback: Color = Color.Gray): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}

// Utility to get month name
fun getMonthName(monthNumber: Int): String {
    return when (monthNumber) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> "Unknown"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitGridScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier
) {
    val habitsWithCompletions by viewModel.habits.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Daily Tracker Progress, 1: Analytics Dashboard

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialogForHabit by remember { mutableStateOf<Habit?>(null) }
    var showMonthDialog by remember { mutableStateOf(false) }

    // Constants for weeks divider
    val totalDays = remember(selectedYear, selectedMonth) {
        val calendar = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth - 1, 1)
        }
        calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val daysWithLabels = remember(selectedYear, selectedMonth, totalDays) {
        (1..totalDays).map { day ->
            val calendar = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth - 1, day)
            }
            val dayOfWeekInt = calendar.get(Calendar.DAY_OF_WEEK)
            val label = when (dayOfWeekInt) {
                Calendar.SUNDAY -> "Su"
                Calendar.MONDAY -> "Mo"
                Calendar.TUESDAY -> "Tu"
                Calendar.WEDNESDAY -> "We"
                Calendar.THURSDAY -> "Th"
                Calendar.FRIDAY -> "Fr"
                Calendar.SATURDAY -> "Sa"
                else -> ""
            }
            Pair(day, label)
        }
    }

    // Dynamic weeks grouping: Week 1 ends on the first Sunday of the month.
    // Subsequent weeks start on Monday.
    val weeks = remember(selectedYear, selectedMonth, totalDays) {
        val calendar = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth - 1, 1)
        }
        val dayOfWeek1st = calendar.get(Calendar.DAY_OF_WEEK)
        // ISO day (Monday = 1, ..., Sunday = 7)
        val isoDay = if (dayOfWeek1st == Calendar.SUNDAY) 7 else dayOfWeek1st - 1
        
        val weeksList = mutableListOf<IntRange>()
        // Week 1 ends on first Sunday
        val endOfFirstWeek = 7 - isoDay + 1
        weeksList.add(1..endOfFirstWeek)
        
        var currentStart = endOfFirstWeek + 1
        while (currentStart <= totalDays) {
            val currentEnd = minOf(currentStart + 6, totalDays)
            weeksList.add(currentStart..currentEnd)
            currentStart = currentEnd + 1
        }
        weeksList
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Habit Tracker",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                actions = {
                    // Month switching card
                    Card(
                        onClick = { showMonthDialog = true },
                        modifier = Modifier.padding(end = 8.dp).testTag("select_month_button"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Switch Month",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${getMonthName(selectedMonth)} $selectedYear",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        floatingActionButton = {
            if (activeTab == 0) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("add_habit_fab"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Habit")
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Header to switch views
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tracker")
                        }
                    }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Analytics")
                        }
                    }
                )
            }

            if (habitsWithCompletions.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Habits Configured",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add some and start tracking for ${getMonthName(selectedMonth)}!",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.testTag("create_first_habit_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create a Habit")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showMonthDialog = true },
                            modifier = Modifier.testTag("open_month_setup_button")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Duplicate Setup From Previous Month")
                        }
                    }
                }
            } else {
                if (activeTab == 0) {
                    // Tab 0: Grid Tracker View
                    Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                        TrackerGrid(
                            daysWithLabels = daysWithLabels,
                            weeks = weeks,
                            habits = habitsWithCompletions,
                            onToggle = { habitId, day, checked -> viewModel.toggleCompletion(habitId, day, checked) },
                            onEdit = { habit -> showEditDialogForHabit = habit }
                        )
                    }
                } else {
                    // Tab 1: Analytics View
                    Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                        AnalyticsDashboard(
                            daysWithLabels = daysWithLabels,
                            weeks = weeks,
                            habits = habitsWithCompletions,
                            selectedMonth = selectedMonth,
                            selectedYear = selectedYear
                        )
                    }
                }
            }
        }
    }

    // ModalDialog for Add Habit
    if (showAddDialog) {
        HabitEditDialog(
            title = "Add Habit",
            maxDays = totalDays,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, target, color ->
                viewModel.addHabit(name, target, color)
                showAddDialog = false
            }
        )
    }

    // ModalDialog for Edit Habit
    showEditDialogForHabit?.let { habitToEdit ->
        HabitEditDialog(
            title = "Edit Habit",
            maxDays = totalDays,
            initialName = habitToEdit.name,
            initialTarget = habitToEdit.targetGoal,
            initialColor = habitToEdit.colorHex,
            isEdit = true,
            onDismiss = { showEditDialogForHabit = null },
            onConfirm = { name, target, color ->
                viewModel.updateHabit(habitToEdit.id, name, target, color)
                showEditDialogForHabit = null
            },
            onDelete = {
                viewModel.deleteHabit(habitToEdit)
                showEditDialogForHabit = null
            }
        )
    }

    // Month Select & Reset Dialogue
    if (showMonthDialog) {
        MonthResetDialog(
            currentYear = selectedYear,
            currentMonth = selectedMonth,
            onDismiss = { showMonthDialog = false },
            onSwitchView = { year, month ->
                viewModel.changeMonth(year, month)
                showMonthDialog = false
            },
            onDuplicateSetup = { year, month ->
                viewModel.duplicateHabitsToNewMonth(year, month)
                showMonthDialog = false
            }
        )
    }
}

@Composable
fun TrackerGrid(
    daysWithLabels: List<Pair<Int, String>>,
    weeks: List<IntRange>,
    habits: List<HabitWithCompletions>,
    onToggle: (Int, Int, Boolean) -> Unit,
    onEdit: (Habit) -> Unit
) {
    val horizontalScrollState = rememberScrollState()

    // Height parameters matching pixel alignments
    val headerHeight = 64.dp
    val rowHeight = 76.dp
    val footerRowHeight = 36.dp

    Row(modifier = Modifier.fillMaxSize()) {
        // --- STICKY LEFT COLUMN: habit names, goals, progress metric & custom bar ---
        Column(
            modifier = Modifier
                .width(180.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    .padding(8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Habit & Goal",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // List of habit cards
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                habits.forEach { hc ->
                    val habit = hc.habit
                    val completionsCount = hc.completions.size
                    val percentage = if (habit.targetGoal == 0) 0f else (completionsCount.toFloat() / habit.targetGoal).coerceAtMost(1f)
                    val progressColor = parseHexColor(habit.colorHex)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight)
                            .clickable { onEdit(habit) }
                            .padding(8.dp)
                            .testTag("habit_row_${habit.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = habit.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Habit",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // Goal tracker details
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Goal: ${habit.targetGoal}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$completionsCount completed",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (completionsCount >= habit.targetGoal) parseHexColor(habit.colorHex) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // Sparkline progress bar
                            LinearProgressIndicator(
                                progress = { percentage },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = progressColor,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            // Footer names
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(footerRowHeight)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "Completed (\u2713)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(footerRowHeight)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "Incomplete (\u2717)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // --- RIGHT SIDE: Horizontally scrollable checklist grid ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(horizontalScrollState)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            // Headers for each day
            Row {
                daysWithLabels.forEach { (day, label) ->
                    val weekIndex = getWeekIndexForDay(day, weeks)
                    val weekBgColor = getWeekColor(weekIndex)

                    Column(
                        modifier = Modifier
                            .width(52.dp)
                            .height(headerHeight)
                            .background(weekBgColor.copy(alpha = 0.15f))
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = day.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Checkboxes grid (Scrollable column of rows matching sticky column list)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                habits.forEach { hc ->
                    val habit = hc.habit
                    val completions = hc.completions

                    Row {
                        daysWithLabels.forEach { (day, _) ->
                            val isCompleted = completions.any { it.dayOfMonth == day }
                            val weekIndex = getWeekIndexForDay(day, weeks)
                            val weekBgColor = getWeekColor(weekIndex)
                            val progressColor = parseHexColor(habit.colorHex)

                            Box(
                                modifier = Modifier
                                    .width(52.dp)
                                    .height(rowHeight)
                                    .background(weekBgColor.copy(alpha = 0.05f))
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                val scale by androidx.compose.animation.core.animateFloatAsState(
                                    targetValue = if (isCompleted) 1.2f else 1.0f,
                                    animationSpec = androidx.compose.animation.core.spring(
                                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                    ),
                                    label = "CheckboxScale"
                                )
                                Checkbox(
                                    checked = isCompleted,
                                    onCheckedChange = { checked -> onToggle(habit.id, day, checked) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = progressColor,
                                        checkmarkColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .scale(scale)
                                        .testTag("checkbox_${habit.id}_$day")
                                )
                            }
                        }
                    }
                }
            }

            // Daily Analytics Footer Row
            Row {
                daysWithLabels.forEach { (day, _) ->
                    // Calculate completed/incomplete for this day
                    val dayCompletedCount = habits.count { activeHabit ->
                        activeHabit.completions.any { it.dayOfMonth == day }
                    }
                    val dayIncompleteCount = habits.size - dayCompletedCount

                    val weekIndex = getWeekIndexForDay(day, weeks)
                    val weekBgColor = getWeekColor(weekIndex)

                    Column(
                        modifier = Modifier
                            .width(52.dp)
                            .background(weekBgColor.copy(alpha = 0.12f))
                    ) {
                        // Total Completed
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(footerRowHeight)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayCompletedCount.toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        // Total Incomplete
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(footerRowHeight)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayIncompleteCount.toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsDashboard(
    daysWithLabels: List<Pair<Int, String>>,
    weeks: List<IntRange>,
    habits: List<HabitWithCompletions>,
    selectedMonth: Int,
    selectedYear: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // --- 1. TOTAL MONTHLY ANNOTATION & PROGRESS CHART ---
        Text(
            text = "Total Monthly Progress",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val totalMonthDays = daysWithLabels.size
        val totalMonthSlots = totalMonthDays * habits.size
        val totalMonthCompleted = habits.sumOf { it.completions.size }
        val totalMonthIncomplete = totalMonthSlots - totalMonthCompleted
        val overallCompletionPercentage = if (totalMonthSlots == 0) 0 else (totalMonthCompleted * 100 / totalMonthSlots)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large elegant Pie chart
                PieChart(
                    completed = totalMonthCompleted,
                    incomplete = totalMonthIncomplete,
                    completedColor = MaterialTheme.colorScheme.primary,
                    incompleteColor = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier
                        .size(120.dp)
                        .testTag("monthly_pie_chart"),
                    isDonut = true
                )

                Spacer(modifier = Modifier.width(24.dp))

                Column {
                    Text(
                        text = "${getMonthName(selectedMonth)} Summary",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Completed: $totalMonthCompleted habits",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Remaining: $totalMonthIncomplete habits",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Overall Success Rate: $overallCompletionPercentage%",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // --- 2. WEEKLY GROUPED ANALYTICS CARDS (1 to 5) ---
        Text(
            text = "Weekly Grouped Analytics",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Generate analytics dynamically per calendar-built Week
        val weeksList = weeks.mapIndexed { index, range ->
            Triple(index + 1, "Week ${index + 1}", range)
        }

        // Filter out non-existent weeks (should generally not apply for pre-constructed ranges, but kept as slot guard)
        val validWeeks = weeksList.filter { it.third.first <= totalMonthDays }

        // Horizontal Grid/Row or flowing columns of week cards
        validWeeks.forEach { (weekIndex, weekTitle, dayRange) ->
            // Filter days that actually belong to this month
            val weekDays = dayRange.filter { it <= totalMonthDays }
            val weekSlotsCount = weekDays.size * habits.size
            var weekCompletedCount = 0

            habits.forEach { hc ->
                weekCompletedCount += hc.completions.count { it.dayOfMonth in weekDays }
            }
            val weekIncompleteCount = weekSlotsCount - weekCompletedCount
            val weekPercentage = if (weekSlotsCount == 0) 0 else (weekCompletedCount * 100 / weekSlotsCount)
            val weekThemeColor = getWeekColor(weekIndex - 1)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("week_card_${weekIndex}"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Visual Color Pill corresponding closely to Grid Header highlights
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(56.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(weekThemeColor)
                        )
                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = weekTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Days ${weekDays.first()} - ${weekDays.last()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Checked: $weekCompletedCount / Remaining: $weekIncompleteCount",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Compact donut PieChart representing week progress
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        PieChart(
                            completed = weekCompletedCount,
                            incomplete = weekIncompleteCount,
                            completedColor = weekThemeColor,
                            incompleteColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(54.dp),
                            isDonut = true
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$weekPercentage% Complete",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (weekPercentage >= 80) Color(0xFF2ECC71) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// Custom rendered donut/pie slice arc Composable
@Composable
fun PieChart(
    completed: Int,
    incomplete: Int,
    completedColor: Color,
    incompleteColor: Color,
    modifier: Modifier = Modifier,
    isDonut: Boolean = true
) {
    val total = completed + incomplete
    val completedAngle = if (total == 0) 0f else (completed.toFloat() / total * 360f)
    val incompleteAngle = 360f - completedAngle

    // Animate arc sweeps smoothly
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = if (isDonut) size.width * 0.16f else size.width
            val borderPadding = strokeWidth / 2f
            val sizeToUse = size.width - strokeWidth
            
            // Draw completed slice
            if (completedAngle > 0) {
                drawArc(
                    color = completedColor,
                    startAngle = -90f,
                    sweepAngle = completedAngle,
                    useCenter = !isDonut,
                    style = if (isDonut) Stroke(width = strokeWidth) else Fill,
                    size = Size(sizeToUse, sizeToUse),
                    topLeft = Offset(borderPadding, borderPadding)
                )
            }
            // Draw incomplete slice
            if (incompleteAngle > 0) {
                drawArc(
                    color = incompleteColor,
                    startAngle = -90f + completedAngle,
                    sweepAngle = incompleteAngle,
                    useCenter = !isDonut,
                    style = if (isDonut) Stroke(width = strokeWidth) else Fill,
                    size = Size(sizeToUse, sizeToUse),
                    topLeft = Offset(borderPadding, borderPadding)
                )
            }
        }

        // Inner percentage layout inside donut spacer
        if (isDonut) {
            val percentageText = if (total == 0) "0%" else "${(completed * 100 / total)}%"
            Text(
                text = percentageText,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Helper to locate a day's week index inside current dynamic calendar list
fun getWeekIndexForDay(day: Int, weeks: List<IntRange>): Int {
    val idx = weeks.indexOfFirst { day in it }
    return if (idx != -1) idx else 0
}

// Legacy static fallback for compatibility
fun getWeekIndex(day: Int): Int {
    return when (day) {
        in 1..7 -> 0
        in 8..14 -> 1
        in 15..21 -> 2
        in 22..28 -> 3
        else -> 4
    }
}

@Composable
fun getWeekColor(weekIndex: Int): Color {
    return when (weekIndex) {
        0 -> parseHexColor("#E57373") // Premium Pastel Red
        1 -> parseHexColor("#64B5F6") // Premium Pastel Sky Blue
        2 -> parseHexColor("#81C784") // Premium Pastel Green
        3 -> parseHexColor("#BA68C8") // Premium Pastel Purple
        else -> parseHexColor("#4DD0E1") // Premium Pastel Teal
    }
}

// Custom ModalDialog for Add / Edit Habit
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditDialog(
    title: String,
    maxDays: Int = 31,
    initialName: String = "",
    initialTarget: Int = 15,
    initialColor: String = "#2ECC71",
    isEdit: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String) -> Unit,
    onDelete: () -> Unit = {}
) {
    var name by remember { mutableStateOf(initialName) }
    var target by remember(initialTarget, maxDays) { 
        mutableStateOf(initialTarget.coerceIn(1, maxDays)) 
    }
    var selectedColorHex by remember { mutableStateOf(initialColor) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("habit_edit_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit Name") },
                    placeholder = { Text("e.g. Drink 3L Water") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("dialog_habit_name_input"),
                    singleLine = true
                )

                // Number Box Drop Down for Goal Target
                var dropdownExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    OutlinedTextField(
                        value = "$target ${if (target == 1) "day" else "days"}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Goal Target") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("dialog_target_dropdown_trigger")
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier
                            .testTag("dialog_target_dropdown_menu")
                            .heightIn(max = 240.dp)
                    ) {
                        (1..maxDays).forEach { dayValue ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = "$dayValue ${if (dayValue == 1) "day" else "days"}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                onClick = {
                                    target = dayValue
                                    dropdownExpanded = false
                                },
                                modifier = Modifier.testTag("dialog_target_option_$dayValue")
                            )
                        }
                    }
                }

                Text(
                    text = "Days completed needed to check off this habit goal.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Custom Color Selector Palettes
                Text(
                    text = "Progress Indicator Color",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                ) {
                    items(CustomColorPalette) { (hex, name) ->
                        val isSelected = selectedColorHex == hex
                        val borderCol = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex))
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = borderCol,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                                .testTag("color_circle_$hex"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = name,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Dialog action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEdit) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.testTag("dialog_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Habit",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("dialog_cancel_button")) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(name.trim(), target.toInt(), selectedColorHex)
                            }
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.testTag("dialog_save_button")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// Dialogue mapping Month / Year Switcher and setup copier
@Composable
fun MonthResetDialog(
    currentYear: Int,
    currentMonth: Int,
    onDismiss: () -> Unit,
    onSwitchView: (Int, Int) -> Unit,
    onDuplicateSetup: (Int, Int) -> Unit
) {
    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMonth by remember { mutableStateOf(currentMonth) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("month_reset_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Calendar Configuration",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = "Configure month settings, reset data, or copy your list setup to a new active tracker.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Select Month Dropdown list / manual picker triggers
                Text(
                    text = "Target Month",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )

                // Horizontal selector of months for simplicity
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    (1..12).forEach { month ->
                        val isSelected = selectedMonth == month
                        val containerBg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        val textCol = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(containerBg)
                                .clickable { selectedMonth = month }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("month_select_option_$month"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = getMonthName(month),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = textCol
                            )
                        }
                    }
                }

                // Target Year manually configured
                Text(
                    text = "Target Year",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    listOf(2025, 2026, 2027, 2028).forEach { year ->
                        val isSelected = selectedYear == year
                        val containerBg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        val textCol = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(containerBg)
                                .clickable { selectedYear = year }
                                .padding(vertical = 8.dp)
                                .testTag("year_select_option_$year"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = year.toString(),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = textCol
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Column
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSwitchView(selectedYear, selectedMonth) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("switch_view_only_button")
                    ) {
                        Text("Switch View only")
                    }

                    Button(
                        onClick = { onDuplicateSetup(selectedYear, selectedMonth) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("duplicate_clean_slate_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Setup to Month (Clean Slate)")
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_month_cancel_button")
                    ) {
                        Text("Close / Cancel")
                    }
                }
            }
        }
    }
}
