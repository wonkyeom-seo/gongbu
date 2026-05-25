package kr.kro.gongbu33

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kr.kro.gongbu33.ui.theme.Gongbu33Theme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestNotificationPermission()

        setContent {
            Gongbu33Theme {
                StudyApp()
            }
        }
    }

    override fun onDestroy() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                200
            )
        }
    }
}

@Composable
private fun StudyApp() {
    val context = LocalContext.current
    val repository = remember { StudySessionRepository(context) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var viewerDate by rememberSaveable { mutableStateOf(TimerStateStore.todayString()) }
    var subject by rememberSaveable { mutableStateOf("") }
    var fullScreenClock by rememberSaveable { mutableStateOf(false) }
    var timerSnapshot by remember { mutableStateOf(TimerStateStore.snapshot(context)) }
    var sessions by remember { mutableStateOf(repository.readSessions()) }
    var nowElapsedMillis by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    var themeSettings by remember { mutableStateOf(ThemeSettingsStore.read(context)) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val colors = themeSettings.toAppColors()

    LaunchedEffect(Unit) {
        while (true) {
            nowElapsedMillis = SystemClock.elapsedRealtime()
            timerSnapshot = TimerStateStore.snapshot(context)
            delay(250)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            sessions = repository.readSessions()
            delay(1000)
        }
    }

    FullScreenSystemUiEffect(fullScreenClock)

    if (fullScreenClock) {
        BackHandler { fullScreenClock = false }
        FullScreenClock(
            snapshot = timerSnapshot,
            nowElapsedMillis = nowElapsedMillis,
            colors = colors,
            onExit = { fullScreenClock = false }
        )
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        containerColor = colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showSettings) {
                SettingsScreen(
                    settings = themeSettings,
                    colors = colors,
                    onBack = { showSettings = false },
                    onPresetSelected = { preset ->
                        themeSettings = ThemeSettingsStore.savePreset(context, preset)
                    },
                    onCustomApply = { backgroundArgb, textArgb ->
                        themeSettings = ThemeSettingsStore.saveCustom(context, backgroundArgb, textArgb)
                    }
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModeTabs(
                        modifier = Modifier.weight(1f),
                        selectedTab = selectedTab,
                        colors = colors,
                        onTabSelected = { selectedTab = it }
                    )

                    OutlinedButton(
                        modifier = Modifier.height(40.dp),
                        onClick = { showSettings = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = outlineButtonColors(colors),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("설정", color = colors.text, fontSize = 13.sp)
                    }
                }

                when (selectedTab) {
                    0 -> TimerScreen(
                        date = TimerStateStore.todayString(),
                        subject = subject,
                        onSubjectChange = { subject = it },
                        snapshot = timerSnapshot,
                        nowElapsedMillis = nowElapsedMillis,
                        colors = colors,
                        onFullScreen = { fullScreenClock = true }
                    )

                    else -> ViewerScreen(
                        date = viewerDate,
                        onDateChange = { viewerDate = it },
                        sessions = sessions,
                        colors = colors,
                        onClearAll = {
                            repository.clearAll()
                            sessions = repository.readSessions()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeTabs(
    modifier: Modifier = Modifier,
    selectedTab: Int,
    colors: AppColors,
    onTabSelected: (Int) -> Unit
) {
    TabRow(
        modifier = modifier,
        selectedTabIndex = selectedTab,
        containerColor = colors.background,
        contentColor = colors.text,
        divider = {
            HorizontalDivider(color = colors.outline)
        }
    ) {
        listOf("측정", "뷰어").forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        color = if (selectedTab == index) colors.text else colors.mutedText,
                        fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

@Composable
private fun TimerScreen(
    date: String,
    subject: String,
    onSubjectChange: (String) -> Unit,
    snapshot: TimerSnapshot,
    nowElapsedMillis: Long,
    colors: AppColors,
    onFullScreen: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val active = snapshot.active
    val elapsed = snapshot.elapsedMillis(nowElapsedMillis)
    val breakElapsed = snapshot.breakMillis()
    val shownDate = if (active) snapshot.date else date
    val shownSubject = if (active) snapshot.subject else subject

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = onFullScreen,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                colors = outlineButtonColors(colors),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("시계만 보기", color = colors.text, fontSize = 13.sp)
            }
        }

        ReadOnlyDateCard(
            date = shownDate,
            colors = colors
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = shownSubject,
            onValueChange = onSubjectChange,
            enabled = !active,
            singleLine = true,
            label = { Text("과목 또는 공부 이름") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = { focusManager.clearFocus() }
            ),
            colors = darkTextFieldColors(colors)
        )

        Spacer(modifier = Modifier.height(0.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = formatDuration(elapsed),
            color = colors.text,
            fontSize = 54.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center
        )

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = if (active) {
                if (snapshot.running) "측정 중 · ${snapshot.subject}" else "일시정지 · ${snapshot.subject}"
            } else {
                "시작하면 백그라운드 알림에서도 제어됩니다"
            },
            color = colors.mutedText,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        if (active) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "쉰 시간 ${formatDuration(breakElapsed)}",
                color = colors.mutedText,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!active) {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    enabled = subject.trim().isNotBlank(),
                    onClick = {
                        focusManager.clearFocus()
                        ContextCompat.startForegroundService(
                            context,
                            StudyTimerService.startIntent(context, date, subject)
                        )
                    },
                    colors = primaryButtonColors(colors),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("시작", fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    onClick = {
                        val action = if (snapshot.running) {
                            StudyTimerService.ACTION_PAUSE
                        } else {
                            StudyTimerService.ACTION_RESUME
                        }
                        ContextCompat.startForegroundService(
                            context,
                            StudyTimerService.actionIntent(context, action)
                        )
                    },
                    colors = secondaryButtonColors(colors),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (snapshot.running) "멈춤" else "계속", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    onClick = {
                        ContextCompat.startForegroundService(
                            context,
                            StudyTimerService.actionIntent(context, StudyTimerService.ACTION_SAVE)
                        )
                    },
                    colors = primaryButtonColors(colors),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("저장", fontWeight = FontWeight.SemiBold)
                }

                if (!snapshot.running) {
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        onClick = {
                            ContextCompat.startForegroundService(
                                context,
                                StudyTimerService.actionIntent(context, StudyTimerService.ACTION_STOP)
                            )
                        },
                        colors = dangerButtonColors(colors),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("초기화", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerScreen(
    date: String,
    onDateChange: (String) -> Unit,
    sessions: List<StudySession>,
    colors: AppColors,
    onClearAll: () -> Unit
) {
    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    val dailySessions = sessions.filter { it.date == date }
    val totalMillis = dailySessions.sumOf { it.durationMillis }
    val subjectTotals = dailySessions
        .groupBy { it.subject.trim() }
        .mapValues { (_, values) -> values.sumOf { it.durationMillis } }
        .toList()
        .sortedByDescending { it.second }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DateSelector(
            label = "조회 날짜",
            date = date,
            enabled = true,
            colors = colors,
            onDateChange = onDateChange
        )

        MonthCalendar(
            selectedDate = date,
            sessions = sessions,
            colors = colors,
            onDateSelected = onDateChange
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.surface,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("전체 공부시간", color = colors.mutedText, fontSize = 14.sp)
                Text(
                    text = formatDuration(totalMillis),
                    color = colors.text,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Light
                )
            }
        }

        Text(
            text = "과목별 공부시간",
            color = colors.text,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        if (subjectTotals.isEmpty()) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "기록 없음",
                color = colors.mutedText,
                textAlign = TextAlign.Center
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                subjectTotals.forEach { (subject, durationMillis) ->
                    SubjectRow(subject = subject, durationMillis = durationMillis, colors = colors)
                }
            }
        }

        Text(
            text = "세부 기록",
            color = colors.text,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        if (dailySessions.isEmpty()) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "기록 없음",
                color = colors.mutedText,
                textAlign = TextAlign.Center
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                dailySessions
                    .sortedBy { it.startMillis.takeIf { millis -> millis > 0L } ?: it.savedAtMillis }
                    .forEach { session ->
                        SessionDetailRow(session = session, colors = colors)
                    }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = sessions.isNotEmpty(),
            onClick = { showClearDialog = true },
            colors = dangerButtonColors(colors),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("전체 기록 초기화", fontWeight = FontWeight.SemiBold)
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = colors.surface,
            titleContentColor = colors.text,
            textContentColor = colors.mutedText,
            title = { Text("전체 기록 초기화") },
            text = { Text("저장된 모든 공부 기록을 삭제합니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAll()
                        showClearDialog = false
                    }
                ) {
                    Text("초기화", color = colors.dangerText, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("취소", color = colors.text)
                }
            }
        )
    }
}

@Composable
private fun SettingsScreen(
    settings: ThemeSettings,
    colors: AppColors,
    onBack: () -> Unit,
    onPresetSelected: (ThemePreset) -> Unit,
    onCustomApply: (Int, Int) -> Unit
) {
    var backgroundHex by rememberSaveable(settings.backgroundArgb) {
        mutableStateOf(ThemeSettingsStore.toHexColor(settings.backgroundArgb))
    }
    var textHex by rememberSaveable(settings.textArgb) {
        mutableStateOf(ThemeSettingsStore.toHexColor(settings.textArgb))
    }
    var errorText by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "색상 설정",
                color = colors.text,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedButton(
                modifier = Modifier.height(38.dp),
                onClick = onBack,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                colors = outlineButtonColors(colors),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("닫기", color = colors.text, fontSize = 13.sp)
            }
        }

        Text("프리셋", color = colors.mutedText, fontSize = 13.sp)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeSettingsStore.presets.forEach { preset ->
                PresetButton(
                    preset = preset,
                    selected = settings.presetId == preset.id,
                    colors = colors,
                    onClick = {
                        errorText = ""
                        onPresetSelected(preset)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text("커스텀", color = colors.mutedText, fontSize = 13.sp)

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = backgroundHex,
            onValueChange = { backgroundHex = it },
            singleLine = true,
            label = { Text("배경색 HEX") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Next
            ),
            colors = darkTextFieldColors(colors)
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = textHex,
            onValueChange = { textHex = it },
            singleLine = true,
            label = { Text("글씨색 HEX") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            ),
            colors = darkTextFieldColors(colors)
        )

        if (errorText.isNotBlank()) {
            Text(errorText, color = colors.dangerText, fontSize = 13.sp)
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            onClick = {
                val background = ThemeSettingsStore.parseHexColor(backgroundHex)
                val text = ThemeSettingsStore.parseHexColor(textHex)
                if (background == null || text == null) {
                    errorText = "#000000 형식으로 입력하세요"
                } else {
                    errorText = ""
                    onCustomApply(background, text)
                }
            },
            colors = primaryButtonColors(colors),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("커스텀 적용", fontWeight = FontWeight.SemiBold)
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.surface,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("미리보기", color = colors.mutedText, fontSize = 13.sp)
                Text("00:25:10", color = colors.text, fontSize = 34.sp, fontWeight = FontWeight.Light)
                Text("측정 중 · 국어", color = colors.mutedText, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun PresetButton(
    preset: ThemePreset,
    selected: Boolean,
    colors: AppColors,
    onClick: () -> Unit
) {
    OutlinedButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        onClick = onClick,
        colors = outlineButtonColors(colors),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorSwatch(backgroundArgb = preset.backgroundArgb, textArgb = preset.textArgb)
                Text(
                    text = preset.label,
                    color = colors.text,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            Text(
                text = if (selected) "적용중" else "적용",
                color = if (selected) colors.text else colors.mutedText,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun ColorSwatch(backgroundArgb: Int, textArgb: Int) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(Color(backgroundArgb), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(Color(textArgb), RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun ReadOnlyDateCard(date: String, colors: AppColors) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("측정 날짜", color = colors.mutedText, fontSize = 13.sp)
            Text(date, color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MonthCalendar(
    selectedDate: String,
    sessions: List<StudySession>,
    colors: AppColors,
    onDateSelected: (String) -> Unit
) {
    val weeks = remember(selectedDate, sessions) {
        buildMonthWeeks(selectedDate, sessions)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    modifier = Modifier.size(38.dp),
                    onClick = { onDateSelected(shiftMonth(selectedDate, -1)) },
                    contentPadding = PaddingValues(0.dp),
                    colors = outlineButtonColors(colors),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("<")
                }
                Text(
                    text = formatMonthTitle(selectedDate),
                    color = colors.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedButton(
                    modifier = Modifier.size(38.dp),
                    onClick = { onDateSelected(shiftMonth(selectedDate, 1)) },
                    contentPadding = PaddingValues(0.dp),
                    colors = outlineButtonColors(colors),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(">")
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("일", "월", "화", "수", "목", "금", "토").forEach { label ->
                    Text(
                        modifier = Modifier.weight(1f),
                        text = label,
                        color = colors.mutedText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            weeks.forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        CalendarDayCell(
                            day = day,
                            selected = day?.date == selectedDate,
                            colors = colors,
                            onDateSelected = onDateSelected
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.CalendarDayCell(
    day: CalendarDay?,
    selected: Boolean,
    colors: AppColors,
    onDateSelected: (String) -> Unit
) {
    val background = if (selected) colors.primaryButton else colors.background
    val textColor = if (selected) colors.primaryButtonText else colors.text
    val mutedColor = if (selected) colors.primaryButtonText else colors.mutedText
    val modifier = if (day == null) {
        Modifier
    } else {
        Modifier.clickable { onDateSelected(day.date) }
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .height(58.dp)
            .padding(2.dp)
            .then(modifier)
            .background(background, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (day != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(day.day.toString(), color = textColor, fontSize = 14.sp)
                Text(
                    text = if (day.totalMillis > 0L) formatCompactDuration(day.totalMillis) else "",
                    color = mutedColor,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SubjectRow(subject: String, durationMillis: Long, colors: AppColors) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = subject,
                color = colors.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = formatDuration(durationMillis),
                color = colors.mutedText,
                fontSize = 16.sp,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun SessionDetailRow(session: StudySession, colors: AppColors) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = session.subject,
                    color = colors.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = formatDuration(session.durationMillis),
                    color = colors.text,
                    fontSize = 15.sp
                )
            }
            Text(
                text = "시작 ${formatClockTime(session.startMillis)} · 끝 ${formatClockTime(session.endMillis)}",
                color = colors.mutedText,
                fontSize = 13.sp
            )
            Text(
                text = "쉰 시간 ${formatDuration(session.breakMillis)}",
                color = colors.mutedText,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun DateSelector(
    label: String,
    date: String,
    enabled: Boolean,
    colors: AppColors,
    onDateChange: (String) -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = colors.mutedText, fontSize = 13.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                modifier = Modifier.size(44.dp),
                enabled = enabled,
                onClick = { onDateChange(TimerStateStore.shiftDate(date, -1)) },
                contentPadding = PaddingValues(0.dp),
                colors = outlineButtonColors(colors),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("<")
            }
            OutlinedButton(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                enabled = enabled,
                onClick = { showDatePickerDialog(context, date, onDateChange) },
                colors = outlineButtonColors(colors),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(date, textAlign = TextAlign.Center)
            }
            OutlinedButton(
                modifier = Modifier.size(44.dp),
                enabled = enabled,
                onClick = { onDateChange(TimerStateStore.shiftDate(date, 1)) },
                contentPadding = PaddingValues(0.dp),
                colors = outlineButtonColors(colors),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(">")
            }
        }
    }
}

@Composable
private fun FullScreenClock(
    snapshot: TimerSnapshot,
    nowElapsedMillis: Long,
    colors: AppColors,
    onExit: () -> Unit
) {
    var clockScale by rememberSaveable { mutableStateOf(1f) }
    val elapsed = snapshot.elapsedMillis(nowElapsedMillis)
    val offsetStep = ((elapsed / 30000L) % 5L).toInt()
    val offsets = listOf(
        IntOffset(0, 0),
        IntOffset(4, 2),
        IntOffset(-3, 4),
        IntOffset(3, -2),
        IntOffset(-4, -3)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    clockScale = (clockScale * zoom).coerceIn(0.55f, 2.4f)
                }
            }
            .background(colors.background)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(56.dp)
                .clickable { onExit() }
        )
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .offset { offsets[offsetStep] },
            text = formatDuration(elapsed),
            color = colors.text,
            fontSize = (64f * clockScale).sp,
            fontWeight = FontWeight.ExtraLight,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FullScreenSystemUiEffect(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        val activity = view.context as? Activity
        val window = activity?.window
        val controller = if (window != null) {
            WindowCompat.getInsetsController(window, view)
        } else {
            null
        }

        if (enabled && window != null && controller != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else if (window != null && controller != null) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            if (window != null && controller != null) {
                controller.show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
        }
    }
}

@Composable
private fun primaryButtonColors(colors: AppColors) = ButtonDefaults.buttonColors(
    containerColor = colors.primaryButton,
    contentColor = colors.primaryButtonText,
    disabledContainerColor = colors.disabledButton,
    disabledContentColor = colors.disabledText
)

@Composable
private fun secondaryButtonColors(colors: AppColors) = ButtonDefaults.buttonColors(
    containerColor = colors.secondaryButton,
    contentColor = colors.text
)

@Composable
private fun dangerButtonColors(colors: AppColors) = ButtonDefaults.buttonColors(
    containerColor = colors.dangerButton,
    contentColor = colors.text,
    disabledContainerColor = colors.disabledButton,
    disabledContentColor = colors.disabledText
)

@Composable
private fun outlineButtonColors(colors: AppColors) = ButtonDefaults.outlinedButtonColors(
    contentColor = colors.text,
    disabledContentColor = colors.disabledText
)

@Composable
private fun darkTextFieldColors(colors: AppColors) = TextFieldDefaults.colors(
    focusedTextColor = colors.text,
    unfocusedTextColor = colors.text,
    disabledTextColor = colors.mutedText,
    focusedContainerColor = colors.background,
    unfocusedContainerColor = colors.background,
    disabledContainerColor = colors.background,
    cursorColor = colors.text,
    focusedIndicatorColor = colors.text,
    unfocusedIndicatorColor = colors.outline,
    disabledIndicatorColor = colors.disabledButton,
    focusedLabelColor = colors.text,
    unfocusedLabelColor = colors.mutedText,
    disabledLabelColor = colors.disabledText
)

private data class CalendarDay(
    val date: String,
    val day: Int,
    val totalMillis: Long
)

private fun buildMonthWeeks(selectedDate: String, sessions: List<StudySession>): List<List<CalendarDay?>> {
    val totalsByDate = sessions
        .groupBy { it.date }
        .mapValues { (_, values) -> values.sumOf { it.durationMillis } }
    val calendar = parseDateCalendar(selectedDate).apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val firstOffset = calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val weeks = mutableListOf<List<CalendarDay?>>()
    var day = 1

    repeat(6) { weekIndex ->
        val week = mutableListOf<CalendarDay?>()
        repeat(7) { dayIndex ->
            val cellIndex = weekIndex * 7 + dayIndex
            if (cellIndex >= firstOffset && day <= daysInMonth) {
                calendar.set(Calendar.DAY_OF_MONTH, day)
                val date = formatCalendarDate(calendar)
                week.add(
                    CalendarDay(
                        date = date,
                        day = day,
                        totalMillis = totalsByDate[date] ?: 0L
                    )
                )
                day += 1
            } else {
                week.add(null)
            }
        }
        weeks.add(week)
    }

    return weeks
}

private fun shiftMonth(date: String, months: Int): String {
    val calendar = parseDateCalendar(date).apply {
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, months)
    }
    return formatCalendarDate(calendar)
}

private fun formatMonthTitle(date: String): String {
    val calendar = parseDateCalendar(date)
    return String.format(
        Locale.US,
        "%04d.%02d",
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1
    )
}

private fun formatCalendarDate(calendar: Calendar): String {
    return String.format(
        Locale.US,
        "%04d-%02d-%02d",
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )
}

private fun parseDateCalendar(date: String): Calendar {
    val calendar = Calendar.getInstance()
    runCatching {
        val parts = date.split("-").map { it.toInt() }
        calendar.set(parts[0], parts[1] - 1, parts[2])
    }
    return calendar
}

private fun formatClockTime(millis: Long): String {
    if (millis <= 0L) return "--:--"
    return CLOCK_FORMAT.get()?.format(Date(millis)) ?: "--:--"
}

private fun formatCompactDuration(durationMillis: Long): String {
    val totalMinutes = (durationMillis / 60000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours > 0L && minutes > 0L -> "${hours}h${minutes}m"
        hours > 0L -> "${hours}h"
        minutes > 0L -> "${minutes}m"
        else -> "1m"
    }
}

private val CLOCK_FORMAT = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat {
        return SimpleDateFormat("HH:mm", Locale.KOREA)
    }
}

private fun showDatePickerDialog(
    context: Context,
    currentDate: String,
    onDateSelected: (String) -> Unit
) {
    val calendar = Calendar.getInstance()
    runCatching {
        val parts = currentDate.split("-").map { it.toInt() }
        calendar.set(parts[0], parts[1] - 1, parts[2])
    }

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected(
                String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            )
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}
