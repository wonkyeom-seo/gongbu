package kr.kro.gongbu

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kr.kro.gongbu.ui.theme.GongbuTheme
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestNotificationPermission()

        setContent {
            GongbuTheme {
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
    var timerDate by rememberSaveable { mutableStateOf(TimerStateStore.todayString()) }
    var viewerDate by rememberSaveable { mutableStateOf(TimerStateStore.todayString()) }
    var subject by rememberSaveable { mutableStateOf("") }
    var fullScreenClock by rememberSaveable { mutableStateOf(false) }
    var timerSnapshot by remember { mutableStateOf(TimerStateStore.snapshot(context)) }
    var sessions by remember { mutableStateOf(repository.readSessions()) }
    var nowElapsedMillis by remember { mutableStateOf(SystemClock.elapsedRealtime()) }

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
            onExit = { fullScreenClock = false }
        )
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        containerColor = Color.Black
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
            ModeTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            when (selectedTab) {
                0 -> TimerScreen(
                    date = timerDate,
                    onDateChange = { timerDate = it },
                    subject = subject,
                    onSubjectChange = { subject = it },
                    snapshot = timerSnapshot,
                    nowElapsedMillis = nowElapsedMillis,
                    onFullScreen = { fullScreenClock = true }
                )

                else -> ViewerScreen(
                    date = viewerDate,
                    onDateChange = { viewerDate = it },
                    sessions = sessions,
                    onClearAll = {
                        repository.clearAll()
                        sessions = repository.readSessions()
                    }
                )
            }
        }
    }
}

@Composable
private fun ModeTabs(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.Black,
        contentColor = Color.White,
        divider = {
            HorizontalDivider(color = Color(0xFF202020))
        }
    ) {
        listOf("측정", "뷰어").forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        color = if (selectedTab == index) Color.White else Color(0xFF8E8E8E),
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
    onDateChange: (String) -> Unit,
    subject: String,
    onSubjectChange: (String) -> Unit,
    snapshot: TimerSnapshot,
    nowElapsedMillis: Long,
    onFullScreen: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val active = snapshot.active
    val elapsed = snapshot.elapsedMillis(nowElapsedMillis)
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
                colors = outlineButtonColors(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("시계만 보기", color = Color.White, fontSize = 13.sp)
            }
        }

        DateSelector(
            label = "날짜",
            date = shownDate,
            enabled = !active,
            onDateChange = onDateChange
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
            colors = darkTextFieldColors()
        )

        Spacer(modifier = Modifier.height(0.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = formatDuration(elapsed),
            color = Color.White,
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
            color = Color(0xFFB5B5B5),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

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
                    colors = primaryButtonColors(),
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
                    colors = secondaryButtonColors(),
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
                    colors = primaryButtonColors(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("저장", fontWeight = FontWeight.SemiBold)
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
            onDateChange = onDateChange
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF101010),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("전체 공부시간", color = Color(0xFFB5B5B5), fontSize = 14.sp)
                Text(
                    text = formatDuration(totalMillis),
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Light
                )
            }
        }

        Text(
            text = "과목별 공부시간",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        if (subjectTotals.isEmpty()) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "기록 없음",
                color = Color(0xFF8E8E8E),
                textAlign = TextAlign.Center
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                subjectTotals.forEach { (subject, durationMillis) ->
                    SubjectRow(subject = subject, durationMillis = durationMillis)
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
            colors = dangerButtonColors(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("전체 기록 초기화", fontWeight = FontWeight.SemiBold)
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = Color(0xFF101010),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFDDDDDD),
            title = { Text("전체 기록 초기화") },
            text = { Text("저장된 모든 공부 기록을 삭제합니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAll()
                        showClearDialog = false
                    }
                ) {
                    Text("초기화", color = Color(0xFFFF6B6B), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("취소", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun SubjectRow(subject: String, durationMillis: Long) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF101010),
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
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = formatDuration(durationMillis),
                color = Color(0xFFDDDDDD),
                fontSize = 16.sp,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun DateSelector(
    label: String,
    date: String,
    enabled: Boolean,
    onDateChange: (String) -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Color(0xFFB5B5B5), fontSize = 13.sp)
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
                colors = outlineButtonColors(),
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
                colors = outlineButtonColors(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(date, textAlign = TextAlign.Center)
            }
            OutlinedButton(
                modifier = Modifier.size(44.dp),
                enabled = enabled,
                onClick = { onDateChange(TimerStateStore.shiftDate(date, 1)) },
                contentPadding = PaddingValues(0.dp),
                colors = outlineButtonColors(),
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
    onExit: () -> Unit
) {
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
            .background(Color.Black)
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
            color = Color.White,
            fontSize = 64.sp,
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
private fun primaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = Color.White,
    contentColor = Color.Black,
    disabledContainerColor = Color(0xFF303030),
    disabledContentColor = Color(0xFF7E7E7E)
)

@Composable
private fun secondaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = Color(0xFF303030),
    contentColor = Color.White
)

@Composable
private fun dangerButtonColors() = ButtonDefaults.buttonColors(
    containerColor = Color(0xFF401818),
    contentColor = Color.White,
    disabledContainerColor = Color(0xFF181818),
    disabledContentColor = Color(0xFF555555)
)

@Composable
private fun outlineButtonColors() = ButtonDefaults.outlinedButtonColors(
    contentColor = Color.White,
    disabledContentColor = Color(0xFF666666)
)

@Composable
private fun darkTextFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    disabledTextColor = Color(0xFFB5B5B5),
    focusedContainerColor = Color.Black,
    unfocusedContainerColor = Color.Black,
    disabledContainerColor = Color.Black,
    cursorColor = Color.White,
    focusedIndicatorColor = Color.White,
    unfocusedIndicatorColor = Color(0xFF4A4A4A),
    disabledIndicatorColor = Color(0xFF303030),
    focusedLabelColor = Color.White,
    unfocusedLabelColor = Color(0xFF8E8E8E),
    disabledLabelColor = Color(0xFF666666)
)

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
