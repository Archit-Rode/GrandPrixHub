package com.example.grandprixhub

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.material.icons.filled.Home

enum class TimeMode { MY_TIME, TRACK_TIME }
enum class SessionStatus { PAST, LIVE, UPCOMING }
// NOTE: TimeMode, AuthStatus, and SessionStatus are now defined at the top level
// of MainViewModel.kt to avoid duplicate class errors.

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()

        setContent {
            val viewModel: MainViewModel = viewModel()
            var selectedTeamId by remember { mutableStateOf<String?>(null) }
            var currentBottomTab by remember { mutableStateOf("Home") }
            var showAuthDialog by remember { mutableStateOf(false) }
            var showProfileMenu by remember { mutableStateOf(false) }
            val authStatus by viewModel.authStatus

            // Notification Permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { }
                LaunchedEffect(Unit) {
                    launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            Scaffold(
                containerColor = Color(0xFF15151E),
                topBar = {
                    val isComparisonMode = viewModel.selectedDriver1 != null && viewModel.selectedDriver2 != null
                    if (selectedTeamId == null && viewModel.selectedRace.value == null && !isComparisonMode) {
                        Column {
                            CenterAlignedTopAppBar(
                                title = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("F1 GRAND PRIX HUB", fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic)
                                        Text(
                                            text = when (currentBottomTab) {
                                                "Home" -> "HOME"
                                                "Schedule" -> "${viewModel.selectedYear.value} CALENDAR"
                                                else -> "STANDINGS"
                                            },
                                            style = MaterialTheme.typography.labelMedium.copy(color = Color.Gray, fontWeight = FontWeight.Bold)
                                        )
                                    }
                                },
                                actions = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SeasonDropdown(viewModel)
                                        Box {
                                            IconButton(onClick = {
                                                if (authStatus == AuthStatus.LoggedIn) showProfileMenu = true
                                                else showAuthDialog = true
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.AccountCircle,
                                                    contentDescription = "Profile",
                                                    tint = if (authStatus == AuthStatus.LoggedIn) Color(0xFFE10600) else Color.White
                                                )
                                            }

                                            DropdownMenu(
                                                expanded = showProfileMenu,
                                                onDismissRequest = { showProfileMenu = false },
                                                modifier = Modifier
                                                    .background(Color(0xFF1F1F27))
                                                    .width(240.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    // --- User Info Header ---
                                                    Text(
                                                        text = viewModel.userName.value.uppercase(),
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                    Text(
                                                        text = viewModel.userEmail.value,
                                                        color = Color.Gray,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )

                                                    Spacer(modifier = Modifier.height(12.dp))

                                                    // --- Favorite Driver Selection Section ---
                                                    Text(
                                                        text = "FAVORITE DRIVER",
                                                        color = Color(0xFFE10600),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )

                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    // 🏎️ Fully Clickable Dropdown Menu Layout Block
                                                    var expanded by remember { mutableStateOf(false) }
                                                    val driverStandingsList by viewModel.drivers

                                                    ExposedDropdownMenuBox(
                                                        expanded = expanded,
                                                        onExpandedChange = { expanded = it }
                                                    ) {
                                                        OutlinedTextField(
                                                            value = viewModel.favDriverName.value,
                                                            onValueChange = {},
                                                            readOnly = true, // Locks user input so hardware keyboards don't push the panel layout
                                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                                            modifier = Modifier
                                                                .menuAnchor()
                                                                .fillMaxWidth(),
                                                            colors = OutlinedTextFieldDefaults.colors(
                                                                focusedTextColor = Color.White,
                                                                unfocusedTextColor = Color.White,
                                                                focusedContainerColor = Color.Transparent,
                                                                unfocusedContainerColor = Color.Transparent,
                                                                focusedTrailingIconColor = Color.White,
                                                                unfocusedTrailingIconColor = Color.Gray,
                                                                focusedBorderColor = Color(0xFFE10600),
                                                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                                            )
                                                        )

                                                        ExposedDropdownMenu(
                                                            expanded = expanded,
                                                            onDismissRequest = { expanded = false },
                                                            modifier = Modifier.background(Color(0xFF1F1F24)) // Dark theme background matching
                                                        ) {
                                                            driverStandingsList.forEach { standing ->
                                                                val driverFullName = "${standing.Driver.givenName} ${standing.Driver.familyName}"

                                                                DropdownMenuItem(
                                                                    text = { Text(driverFullName, color = Color.White) },
                                                                    onClick = {
                                                                        // 1. Instantly force the state string to update on the layout thread
                                                                        viewModel.favDriverName.value = driverFullName
                                                                        // 2. Shut the dropdown view block smoothly
                                                                        expanded = false
                                                                        // 3. Persist the change to Firebase Firestore
                                                                        viewModel.saveUserPrefs(standing.Driver.driverId)
                                                                    },
                                                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding // Spans the touch surface to full row boundaries
                                                                )
                                                            }
                                                        }
                                                    }

                                                    // --- Action Button Footer ---
                                                    HorizontalDivider(
                                                        modifier = Modifier.padding(vertical = 16.dp),
                                                        color = Color.White.copy(alpha = 0.1f)
                                                    )

                                                    Button(
                                                        onClick = {
                                                            showProfileMenu = false
                                                            viewModel.logout()
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE10600))
                                                    ) {
                                                        Text("LOGOUT", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF15151E), titleContentColor = Color.White)
                            )

                            if (currentBottomTab == "Results") {
                                TabRow(
                                    selectedTabIndex = if (viewModel.isDriversTab.value) 0 else 1,
                                    containerColor = Color(0xFF15151E),
                                    contentColor = Color(0xFFE10600),
                                    indicator = { tabPositions ->
                                        TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[if (viewModel.isDriversTab.value) 0 else 1]), color = Color(0xFFE10600))
                                    }
                                ) {
                                    Tab(selected = viewModel.isDriversTab.value, onClick = { viewModel.isDriversTab.value = true }, text = { Text("DRIVERS", color = Color.White, fontWeight = FontWeight.Bold) })
                                    Tab(selected = !viewModel.isDriversTab.value, onClick = { viewModel.isDriversTab.value = false }, text = { Text("TEAMS", color = Color.White, fontWeight = FontWeight.Bold) })
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    NavigationBar(containerColor = Color(0xFF1F1F27), contentColor = Color.White) {
                        // 🏎️ Look for NavigationBar inside MainActivity.kt and append this item at the front!
                        NavigationBarItem(
                            selected = currentBottomTab == "Home",
                            onClick = { currentBottomTab = "Home"; selectedTeamId = null; viewModel.clearSelectedRace() },
                            label = { Text("Home", fontWeight = FontWeight.Bold) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Home View"
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFFE10600),
                                selectedTextColor = Color(0xFFE10600),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color.Transparent
                            )
                        )
                        NavigationBarItem(
                            selected = currentBottomTab == "Schedule",
                            onClick = { currentBottomTab = "Schedule"; selectedTeamId = null; viewModel.clearSelectedRace(); viewModel.clearComparison() },
                            label = { Text("Schedule", fontWeight = FontWeight.Bold) },
                            icon = { Icon(painterResource(android.R.drawable.ic_menu_today), null) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFE10600), selectedTextColor = Color(0xFFE10600), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color.Transparent)
                        )
                        NavigationBarItem(
                            selected = currentBottomTab == "Results",
                            onClick = { currentBottomTab = "Results"; viewModel.clearSelectedRace() },
                            label = { Text("Results", fontWeight = FontWeight.Bold) },
                            icon = { Icon(painterResource(android.R.drawable.ic_menu_sort_by_size), null) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFE10600), selectedTextColor = Color(0xFFE10600), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color.Transparent)
                        )
                        NavigationBarItem(
                            selected = currentBottomTab == "Compare",
                            onClick = { currentBottomTab = "Compare"; selectedTeamId = null; viewModel.clearSelectedRace() },
                            label = { Text("Compare", fontWeight = FontWeight.Bold) },
                            icon = { Icon(painterResource(android.R.drawable.ic_menu_share), null) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFE10600), selectedTextColor = Color(0xFFE10600), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color.Transparent)
                        )
                    }
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    when (currentBottomTab) {
                        "Home" -> HomeScreen(viewModel)
                        "Schedule" -> if (viewModel.selectedRace.value == null) ScheduleScreen(viewModel) else RaceDetailScreen(viewModel)
                        "Results" -> if (selectedTeamId == null) {
                            if (viewModel.isDriversTab.value) DriverListScreen(viewModel) else ConstructorListScreen(viewModel) { selectedTeamId = it }
                        } else TeamDetailScreen(teamId = selectedTeamId!!, viewModel = viewModel, onBack = { selectedTeamId = null })
                        "Compare" -> if (viewModel.selectedDriver1 != null && viewModel.selectedDriver2 != null) DriverComparisonScreen(viewModel) else ComparisonSelectionScreen(viewModel)
                    }

                    if (showAuthDialog) {
                        Dialog(onDismissRequest = { showAuthDialog = false }) {
                            Box(modifier = Modifier
                                .fillMaxHeight(0.85f)
                                .clip(RoundedCornerShape(16.dp))) {
                                // 🏎️ Added the trailing lambda to listen for success!
                                AuthScreen(viewModel) {
                                    showAuthDialog = false
                                }
                            }
                        }
                    }

                    if (authStatus == AuthStatus.Onboarding) {
                        Dialog(onDismissRequest = { }) {
                            OnboardingScreen(viewModel) { viewModel.authStatus.value = AuthStatus.LoggedIn }
                        }
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "F1 Session Reminders"
            val channel = NotificationChannel("F1_NOTIFS", name, NotificationManager.IMPORTANCE_HIGH).apply { description = "Practice, Qualifying, and Races" }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }
}

// --- HELPERS & COMPONENTS ---

fun formatToDisplayTime(apiDate: String, apiTime: String, mode: TimeMode, circuitCountry: String): String {
    return try {
        val utcDateTime = LocalDateTime.parse("${apiDate}T${apiTime.replace("Z", "")}").atZone(ZoneId.of("UTC"))
        val targetTime = if (mode == TimeMode.MY_TIME) utcDateTime.withZoneSameInstant(ZoneId.systemDefault()) else utcDateTime.withZoneSameInstant(ZoneId.of(getTrackTimeZone(circuitCountry)))
        targetTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) { apiTime.take(5) }
}

fun getTrackTimeZone(country: String): String = when (country.lowercase()) {
    "australia" -> "Australia/Melbourne"
    "china" -> "Asia/Shanghai"
    "japan" -> "Asia/Tokyo"
    "bahrain" -> "Asia/Bahrain"
    "saudi arabia" -> "Asia/Riyadh"
    "usa", "united states" -> "America/New_York"
    "monaco", "italy", "spain", "austria", "belgium", "hungary", "netherlands" -> "Europe/Paris"
    "uk", "united kingdom" -> "Europe/London"
    "singapore" -> "Asia/Singapore"
    "azerbaijan" -> "Asia/Baku"
    "mexico" -> "America/Mexico_City"
    "brazil" -> "America/Sao_Paulo"
    "uae", "abu dhabi" -> "Asia/Dubai"
    "qatar" -> "Asia/Qatar"
    else -> "UTC"
}

fun formatRaceWeekend(apiDate: String): String {
    return try {
        val raceDate = LocalDate.parse(apiDate)
        "${raceDate.minusDays(2).dayOfMonth}-${raceDate.dayOfMonth} ${raceDate.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH))}"
    } catch (e: Exception) { apiDate }
}

fun formatSessionDate(apiDate: String): String = try { LocalDate.parse(apiDate).format(DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)) } catch (e: Exception) { apiDate }

@Composable
fun ScheduleScreen(viewModel: MainViewModel) {
    val raceList = viewModel.schedule.value
    LaunchedEffect(raceList) {
        if (raceList.isNotEmpty()) {
            while(true) {
                viewModel.updateCountdown()
                kotlinx.coroutines.delay(60000)
            }
        }
    }
    if (raceList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFE10600)) }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            if (viewModel.countdownText.value.isNotEmpty()) {
                Surface(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp), color = Color(0xFFE10600), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NEXT RACE COUNTDOWN", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                        Text(viewModel.countdownText.value, color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic))
                    }
                }
            }
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
                items(raceList) { race -> RaceCard(race = race, onClick = { viewModel.selectRace(race) }) }
            }
        }
    }
}

@Composable
fun RaceCard(race: APIRace, onClick: () -> Unit) {
    val winner = race.Results?.firstOrNull()?.Driver
    val isSprint = race.Sprint != null || race.raceName.contains("China", true) || race.raceName.contains("Miami", true)
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)
        .clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = if (isSprint) Color(0xFF25252E) else Color(0xFF1F1F27)), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ROUND ${race.round}", color = Color(0xFFE10600), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        if (isSprint) {
                            Spacer(modifier = Modifier.width(8.dp)); Surface(color = Color(0xFFE10600), shape = RoundedCornerShape(4.dp)) { Text("SPRINT", color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Black)) }
                        }
                    }
                    Text(race.raceName.uppercase(), color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic))
                }
                Text(formatRaceWeekend(race.date), color = Color.White, fontWeight = FontWeight.Bold)
            }
            if (winner != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(android.R.drawable.btn_star_big_on), null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp)); Text("WINNER: ${winner.givenName} ${winner.familyName.uppercase()}", color = Color.LightGray, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaceDetailScreen(viewModel: MainViewModel) {
    val race = viewModel.selectedRace.value ?: return
    val circuitData = CircuitRepository.getDetails(race.Circuit.circuitId)
    val weather = viewModel.currentWeather.value

    LaunchedEffect(race.round) { viewModel.fetchWeather() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF15151E))) {
            TextButton(onClick = { viewModel.clearSelectedRace() }, modifier = Modifier.padding(8.dp)) { Text("< BACK TO CALENDAR", color = Color(0xFFE10600), fontWeight = FontWeight.Bold) }
            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                item {
                    Text(race.raceName.uppercase(), style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Black)
                    Text(race.Circuit.circuitName, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    WeatherWidget(weather)
                    Spacer(modifier = Modifier.height(24.dp))
                    if (circuitData.imageRes != null) { CircuitMapWithHotspots(circuitImage = circuitData.imageRes, hotspots = circuitData.hotspots) }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DetailInfo("LAPS", "${circuitData.laps}"); DetailInfo("ROUND", race.round); DetailInfo("DATE", formatRaceWeekend(race.date))
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("HISTORY", color = Color(0xFFE10600), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
                    Text(circuitData.description, color = Color.LightGray, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("WEEKEND SCHEDULE", color = Color(0xFFE10600), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
                        TextButton(onClick = { viewModel.toggleTimeMode() }) { Text(if (viewModel.timeMode == TimeMode.MY_TIME) "SHOW TRACK TIME" else "SHOW MY TIME", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        val mode = viewModel.timeMode; val country = race.Circuit.Location.country; val season = viewModel.selectedYear.value
                        val onSessionClick: (String) -> Unit = { sessionType ->
                            val type = when {
                                sessionType.contains("Practice 1") -> "fp1"; sessionType.contains("Practice 2") -> "fp2"; sessionType.contains("Practice 3") -> "fp3"
                                sessionType.contains("Sprint Qualifying") -> "sprint_qualifying";sessionType.contains("Qualifying") -> "qualifying"; sessionType.contains("Sprint Race") -> "sprint"; else -> "results"
                            }
                            viewModel.fetchSessionResults(season, race.round, type)

                            if (type == "fp1" || type == "fp2" || type == "fp3" || type == "sprint_qualifying") {
                                viewModel.startLiveTiming(season, race.round, type)
                            }
                        }
                        race.FirstPractice?.let { Box(modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSessionClick("Practice 1") }) { TimelineItem("Practice 1", formatSessionDate(it.date), formatToDisplayTime(it.date, it.time, mode, country), false, getSessionStatus(it.date, it.time)) } }
                        if (race.Sprint != null) {
                            race.SprintShootout?.let { Box(modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSessionClick("Sprint Qualifying") }) { TimelineItem("Sprint Qualifying", formatSessionDate(it.date), formatToDisplayTime(it.date, it.time, mode, country), false, getSessionStatus(it.date, it.time)) } }
                            race.Sprint?.let { Box(modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSessionClick("Sprint Race") }) { TimelineItem("Sprint Race", formatSessionDate(it.date), formatToDisplayTime(it.date, it.time, mode, country), false, getSessionStatus(it.date, it.time)) } }
                        } else {
                            race.SecondPractice?.let { Box(modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSessionClick("Practice 2") }) { TimelineItem("Practice 2", formatSessionDate(it.date), formatToDisplayTime(it.date, it.time, mode, country), false, getSessionStatus(it.date, it.time)) } }
                            race.ThirdPractice?.let { Box(modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSessionClick("Practice 3") }) { TimelineItem("Practice 3", formatSessionDate(it.date), formatToDisplayTime(it.date, it.time, mode, country), false, getSessionStatus(it.date, it.time)) } }
                        }
                        race.Qualifying?.let { Box(modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSessionClick("Qualifying") }) { TimelineItem("Qualifying", formatSessionDate(it.date), formatToDisplayTime(it.date, it.time, mode, country), false, getSessionStatus(it.date, it.time)) } }
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSessionClick("Grand Prix") }) { TimelineItem("Grand Prix", formatSessionDate(race.date), formatToDisplayTime(race.date, race.time ?: "15:00:00Z", mode, country), true, getSessionStatus(race.date, race.time ?: "15:00:00Z")) }
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
        if (viewModel.isShowingResults.value) {
            ModalBottomSheet(
                onDismissRequest = {
                    // 1. Dismiss the visibility of the overlay smoothly
                    viewModel.isShowingResults.value = false

                    // 2. 🏎️ STOP THE POLLING THREAD IMMEDIATELY ON SHEET DISMISSAL
                    viewModel.stopLiveTiming()
                },
                containerColor = Color(0xFF1C1C1C),
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
            ) {
                SessionResultsList(viewModel)
            }
        }
    }
}

@Composable
fun DriverListScreen(viewModel: MainViewModel) {
    val driverStandings = viewModel.drivers.value
    LazyColumn(modifier = Modifier.fillMaxSize()) { items(driverStandings) { standing -> DriverCard(standing) } }
}

@Composable
fun ConstructorListScreen(viewModel: MainViewModel, onTeamClick: (String) -> Unit) {
    val constructorStandings = viewModel.constructors.value
    LazyColumn(modifier = Modifier.fillMaxSize()) { items(constructorStandings) { standing -> ConstructorCard(standing, onTeamClick) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailScreen(teamId: String, viewModel: MainViewModel, onBack: () -> Unit) {
    val teamDrivers = viewModel.getDriversForTeam(teamId)
    val teamColor = getTeamColor(teamId)
    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF15151E))) {
        CenterAlignedTopAppBar(title = { Text(teamId.replace("_", " ").uppercase(), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }, navigationIcon = { TextButton(onClick = onBack) { Text("< BACK", color = teamColor, fontWeight = FontWeight.Bold) } }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF15151E), titleContentColor = Color.White))
        LazyColumn(contentPadding = PaddingValues(16.dp)) { items(teamDrivers) { standing -> DriverDetailCard(standing, teamColor) } }
    }
}

@Composable
fun DriverCard(standing: DriverStanding) {
    val driver = standing.Driver; val teamId = standing.Constructors?.lastOrNull()?.constructorId; val teamColor = getTeamColor(teamId)
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp, horizontal = 12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F27)), shape = RoundedCornerShape(topStart = 0.dp, bottomEnd = 12.dp), border = BorderStroke(1.dp, Color(0xFF38383F))) {
        Row(modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = standing.position ?: "NR", color = Color.White, modifier = Modifier.width(24.dp), fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.width(8.dp)); Box(modifier = Modifier
            .width(4.dp)
            .height(45.dp)
            .background(teamColor)); Spacer(modifier = Modifier.width(16.dp))
            Column { Text(text = driver.familyName.uppercase(), color = Color.White, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)); Text(text = driver.givenName, color = Color.LightGray) }; Spacer(modifier = Modifier.weight(1f))
            Surface(color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))) { Text(text = "${standing.points ?: "0"} PTS", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.width(12.dp)); Text(text = driver.permanentNumber ?: "--", color = Color.White.copy(alpha = 0.07f), style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun DriverComparisonScreen(viewModel: MainViewModel) {
    val d1 = viewModel.selectedDriver1 ?: return; val d2 = viewModel.selectedDriver2 ?: return
    val d1DNA by viewModel.driver1DNA; val d2DNA by viewModel.driver2DNA
    val chartColor1 = Color(0xFFE10600); val chartColor2 = Color(0xFF64C4FF)
    val scrollState = rememberScrollState()
    LaunchedEffect(d1.Driver.driverId, d2.Driver.driverId) { viewModel.loadDriverStats(d1.Driver.driverId, true); viewModel.loadDriverStats(d2.Driver.driverId, false) }
    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF15151E))
        .verticalScroll(scrollState)
        .padding(16.dp)) {
        TextButton(onClick = { viewModel.clearComparison() }) { Text("< BACK TO STANDINGS", color = Color(0xFFE10600), fontWeight = FontWeight.Bold) }
        Text(text = "${viewModel.selectedYear.value} SEASON COMPARISON", color = Color.White, modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black))
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { ComparisonLegendItem(d1.Driver.familyName, chartColor1); Text(" VS ", color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.labelSmall); ComparisonLegendItem(d2.Driver.familyName, chartColor2) }
        Spacer(modifier = Modifier.height(8.dp)); Box(modifier = Modifier
        .fillMaxWidth()
        .height(320.dp), contentAlignment = Alignment.Center) { ComparisonRadar(driver1Name = d1.Driver.familyName, driver2Name = d2.Driver.familyName, driver1Scores = d1DNA, driver2Scores = d2DNA) }
        if (d1DNA.isNotEmpty() && d2DNA.isNotEmpty()) { DriverInsightCard(d1Name = d1.Driver.familyName, d2Name = d2.Driver.familyName, d1Scores = d1DNA, d2Scores = d2DNA) }
        Spacer(modifier = Modifier.height(16.dp)); Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { DriverImage(d1.Driver.driverId); Spacer(modifier = Modifier.height(8.dp)); Text(d1.Driver.familyName.uppercase(), color = chartColor1, fontWeight = FontWeight.Black); Spacer(modifier = Modifier.height(8.dp)); StatBox("POINTS", d1.points ?: "0"); StatBox("WINS", d1.wins ?: "0"); StatBox("RANK", if (d1.position != null) "#${d1.position}" else "NR") }
        Text(text = "VS", modifier = Modifier.padding(bottom = 60.dp), color = Color.Gray, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { DriverImage(d2.Driver.driverId); Spacer(modifier = Modifier.height(8.dp)); Text(d2.Driver.familyName.uppercase(), color = chartColor2, fontWeight = FontWeight.Black); Spacer(modifier = Modifier.height(8.dp)); StatBox("POINTS", d2.points ?: "0"); StatBox("WINS", d2.wins ?: "0"); StatBox("RANK", if (d2.position != null) "#${d2.position}" else "NR") }
    }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ComparisonLegendItem(name: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier
        .size(10.dp)
        .background(color, CircleShape)); Spacer(modifier = Modifier.width(6.dp)); Text(text = name.uppercase(), color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
}

@Composable
fun TimelineItem(sessionName: String, date: String, time: String, isLast: Boolean, status: SessionStatus) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
            when (status) { SessionStatus.PAST -> Text("🏁", fontSize = 14.sp); SessionStatus.LIVE -> Box(modifier = Modifier
                .size(12.dp)
                .background(Color.Green, CircleShape)); else -> Box(modifier = Modifier
                .size(12.dp)
                .background(Color(0xFFE10600), CircleShape)) }
            if (!isLast) Box(modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(Color.Gray.copy(alpha = 0.3f)))
        }
        Column(modifier = Modifier.padding(start = 12.dp, bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text(sessionName, color = Color.White, fontWeight = FontWeight.Bold); if (status == SessionStatus.LIVE) { Spacer(modifier = Modifier.width(8.dp)); Text("LIVE", color = Color.Green, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall) } }
            Text("$date | $time", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 12.dp)) { Text(text = label.uppercase(), color = Color.Gray, style = MaterialTheme.typography.labelSmall); Text(text = value, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
}

@Composable
fun DetailInfo(label: String, value: String) {
    Column { Text(label, color = Color.Gray, style = MaterialTheme.typography.labelSmall); Text(value, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
}

@Composable
fun DriverDetailCard(standing: DriverStanding, teamColor: Color) {
    val driver = standing.Driver; val cleanId = if (driver.driverId.contains("colapinto")) "franco-colapinto" else driver.driverId.split("_").last(); val imageUrl = "https://media.formula1.com/content/dam/fom-website/drivers/2025Drivers/${cleanId}.png"
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F27)), border = BorderStroke(1.dp, teamColor.copy(alpha = 0.3f))) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier
                .size(100.dp)
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)), contentScale = ContentScale.Fit, error = painterResource(android.R.drawable.ic_menu_gallery)); Spacer(modifier = Modifier.width(16.dp))
            Column { Text(driver.familyName.uppercase(), color = Color.White, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)); Text(driver.givenName, color = Color.LightGray); Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) { Text("NO. ${driver.permanentNumber ?: "--"}", color = teamColor, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.width(12.dp)); Text("RANK ${standing.position ?: "NR"}", color = Color.Gray, fontSize = 12.sp) } }
            Spacer(modifier = Modifier.weight(1f)); Text(text = standing.points ?: "0", color = Color.White, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonDropdown(viewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val seasons = listOf("2024", "2025", "2026")
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        Text(text = viewModel.selectedYear.value, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier
            .menuAnchor()
            .padding(16.dp)
            .clickable { expanded = true })
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color(0xFF1F1F27))) { seasons.forEach { year -> DropdownMenuItem(text = { Text(year, color = Color.White) }, onClick = { viewModel.updateYear(year); expanded = false }) } }
    }
}

@Composable
fun ConstructorCard(standing: ConstructorStanding, onTeamClick: (String) -> Unit) {
    val team = standing.Constructor; val teamId = team.constructorId; val teamColor = getTeamColor(teamId)
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp, horizontal = 12.dp)
        .clickable { onTeamClick(teamId) }, colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F27)), shape = RoundedCornerShape(topStart = 0.dp, bottomEnd = 12.dp), border = BorderStroke(1.dp, Color(0xFF38383F))) {
        Row(modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = standing.position ?: "NR", color = Color.White, modifier = Modifier.width(28.dp), fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.width(8.dp)); Box(modifier = Modifier
            .width(4.dp)
            .height(40.dp)
            .background(teamColor)); Spacer(modifier = Modifier.width(16.dp))
            Column { Text(text = (team.name ?: "UNKNOWN TEAM").uppercase(), color = Color.White, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)); Text(text = team.nationality ?: "International", color = teamColor, style = MaterialTheme.typography.bodyMedium) }; Spacer(modifier = Modifier.weight(1f)); Text(text = "${standing.points ?: "0"} PTS", color = Color.White, fontWeight = FontWeight.ExtraBold); Spacer(modifier = Modifier.width(12.dp)); Text(text = ">", color = Color.Gray)
        }
    }
}

fun getSessionStatus(apiDate: String, apiTime: String): SessionStatus {
    return try {
        val utcTime = apiTime.replace("Z", ""); val sessionStart = LocalDateTime.parse("${apiDate}T$utcTime").atZone(ZoneId.of("UTC")); val now = ZonedDateTime.now(ZoneId.of("UTC")); val sessionEnd = sessionStart.plusHours(2)
        when { now.isAfter(sessionEnd) -> SessionStatus.PAST; now.isAfter(sessionStart) && now.isBefore(sessionEnd) -> SessionStatus.LIVE; else -> SessionStatus.UPCOMING }
    } catch (e: Exception) { SessionStatus.UPCOMING }
}

@Composable
fun ComparisonSelectionScreen(viewModel: MainViewModel) {
    val allDrivers = viewModel.drivers.value
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("SELECT TWO DRIVERS", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall); Text("Tap drivers to add to comparison", color = Color.Gray, style = MaterialTheme.typography.bodySmall); Spacer(modifier = Modifier.height(16.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { SelectionSlot("Driver 1", viewModel.selectedDriver1, modifier = Modifier.weight(1f)); SelectionSlot("Driver 2", viewModel.selectedDriver2, modifier = Modifier.weight(1f)) }; Spacer(modifier = Modifier.height(16.dp)); LazyColumn(modifier = Modifier.weight(1f)) { items(allDrivers) { standing -> val isSelected = viewModel.selectedDriver1 == standing || viewModel.selectedDriver2 == standing; Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)
        .clickable { viewModel.selectDriverForComparison(standing) }, colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFE10600).copy(alpha = 0.2f) else Color(0xFF1F1F27)), border = if (isSelected) BorderStroke(1.dp, Color(0xFFE10600)) else null) { Text(text = "${standing.Driver.givenName} ${standing.Driver.familyName.uppercase()}", modifier = Modifier.padding(16.dp), color = Color.White, fontWeight = FontWeight.Bold) } } }
    }
}

@Composable
fun SelectionSlot(label: String, driver: DriverStanding?, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.height(60.dp), color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, if (driver != null) Color(0xFFE10600) else Color.Gray.copy(alpha = 0.3f))) { Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.Center) { Text(label, color = Color.Gray, fontSize = 10.sp); Text(text = driver?.Driver?.familyName?.uppercase() ?: "EMPTY", color = if (driver != null) Color.White else Color.DarkGray, fontWeight = FontWeight.Bold, maxLines = 1) } }
}

@Composable
fun DriverImage(driverId: String) {
    val cleanId = if (driverId.contains("colapinto")) "franco-colapinto" else driverId.split("_").last(); val imageUrl = "https://media.formula1.com/content/dam/fom-website/drivers/2025Drivers/${cleanId}.png"
    AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier
        .size(120.dp)
        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)), contentScale = ContentScale.Fit, error = painterResource(android.R.drawable.ic_menu_gallery))
}

@Composable
fun WeatherWidget(weather: APIWeather?) {
    if (weather == null) return
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F27)), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))) {
        Row(modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("TRACK CONDITIONS", color = Color.Gray, style = MaterialTheme.typography.labelSmall); Row(verticalAlignment = Alignment.CenterVertically) { Text(text = if (weather.rainfall == 1) "🌧️ WET" else "☀️ DRY", color = if (weather.rainfall == 1) Color(0xFF64C4FF) else Color(0xFFFFD700), fontWeight = FontWeight.Bold) } }
            WeatherStat("AIR", "${weather.air_temperature}°C"); WeatherStat("TRACK", "${weather.track_temperature}°C"); WeatherStat("HUMIDITY", "${weather.humidity}%")
        }
    }
}

@Composable
fun WeatherStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(label, color = Color.Gray, fontSize = 10.sp); Text(value, color = Color.White, fontWeight = FontWeight.Bold) }
}

@Composable
fun DriverInsightCard(d1Name: String, d2Name: String, d1Scores: Map<String, Float>, d2Scores: Map<String, Float>) {
    val d1Archetype = getDriverArchetype(d1Scores); val d2Archetype = getDriverArchetype(d2Scores); val pillars = listOf("Qualy Pace", "Race Craft", "Peak Performance"); val biggestGapPillar = pillars.maxByOrNull { kotlin.math.abs((d1Scores[it] ?: 0f) - (d2Scores[it] ?: 0f)) } ?: "Qualy Pace"
    val d1Val = d1Scores[biggestGapPillar] ?: 0f; val d2Val = d2Scores[biggestGapPillar] ?: 0f; val leaderName = if (d1Val > d2Val) d1Name else d2Name; val trailingName = if (d1Val > d2Val) d2Name else d1Name; val delta = kotlin.math.abs(d1Val - d2Val); val intensity = when { delta > 2.5f -> "dominates"; delta > 1.0f -> "outperforms"; else -> "marginally leads" }
    val annotatedAnalysis = buildAnnotatedString { append("$d1Name ("); withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFE10600))) { append(d1Archetype) }; append(") and $d2Name ("); withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF64C4FF))) { append(d2Archetype) }; append(") show contrasting styles. The primary battleground is "); withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append(biggestGapPillar) }; append(", where $leaderName $intensity $trailingName. "); append(when(biggestGapPillar) { "Qualy Pace" -> "This suggests $leaderName is the favorite to dictate the tempo from the front on Saturday."; "Race Craft" -> "Expect $leaderName to be the more clinical overtaker, making them a threat regardless of grid position."; "Peak Performance" -> "In high-pressure scenarios, $leaderName has shown a superior ability to convert opportunities into podiums."; else -> "" }) }
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F27)), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFF38383F))) {
        Column(modifier = Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(painterResource(android.R.drawable.ic_dialog_info), null, tint = Color(0xFFE10600), modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("STRATEGIC ANALYSIS", color = Color(0xFFE10600), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black) }; Spacer(modifier = Modifier.height(12.dp)); Text(text = annotatedAnalysis, color = Color.White, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp) }
    }
}

private fun getDriverArchetype(scores: Map<String, Float>): String {
    val q = scores["Qualy Pace"] ?: 0f; val r = scores["Race Craft"] ?: 0f; val p = scores["Peak Performance"] ?: 0f
    return when { q > 8.5f && r < 7.5f -> "Qualy Specialist"; r > 8.5f && q < 7.5f -> "Sunday Warrior"; p > 9.0f -> "Clutch Performer"; q > 7.5f && r > 7.5f -> "Balanced Elite"; else -> "Tactical Driver" }
}

@Composable
fun HighlightThumbnailPlayer(videoId: String, thumbnailUrl: String) {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"));
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/watch?v=$videoId")
                ); try {
                context.startActivity(appIntent)
            } catch (ex: Exception) {
                context.startActivity(webIntent)
            }
            }, contentAlignment = Alignment.Center) { AsyncImage(model = thumbnailUrl, contentDescription = "F1 Highlights Thumbnail", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop); Surface(color = Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(50), modifier = Modifier.size(64.dp)) { Icon(painterResource(android.R.drawable.ic_media_play), "Play Video", tint = Color.White, modifier = Modifier.padding(16.dp)) } }
        Spacer(modifier = Modifier.height(12.dp)); Text(text = "TAP TO WATCH OFFICIAL HIGHLIGHTS", color = Color(0xFFE10600), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun SessionResultsList(viewModel: MainViewModel) {
    var selectedTab by remember { mutableStateOf("RESULTS") }; val race = viewModel.selectedRace.value; val session = viewModel.selectedSessionType.value
    LaunchedEffect(selectedTab) { if (selectedTab == "HIGHLIGHTS" && race != null) { viewModel.fetchLiveHighlight(race.raceName, session) } }
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { listOf("RESULTS", "HIGHLIGHTS").forEach { tab -> val isSelected = selectedTab == tab; Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
            .padding(horizontal = 24.dp)
            .clickable { selectedTab = tab }) { Text(text = tab, color = if (isSelected) Color(0xFFE10600) else Color.Gray, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge); if (isSelected) { Box(modifier = Modifier
            .padding(top = 4.dp)
            .width(20.dp)
            .height(2.dp)
            .background(Color(0xFFE10600))) } } } }
        Spacer(modifier = Modifier.height(24.dp))
        if (selectedTab == "RESULTS") {
            val results = viewModel.selectedSessionResults.value; if (results.isEmpty()) { Box(modifier = Modifier
                .fillMaxWidth()
                .height(200.dp), contentAlignment = Alignment.Center) { Text("No results data available yet.", color = Color.Gray) } } else { LazyColumn(modifier = Modifier.fillMaxHeight(0.8f)) { items(results) { result -> SessionResultRow(result); HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp)) } } }
        } else {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { if (viewModel.selectedVideoId.value.isNotEmpty()) { HighlightThumbnailPlayer(videoId = viewModel.selectedVideoId.value, thumbnailUrl = viewModel.selectedThumbnailUrl.value); Spacer(modifier = Modifier.height(16.dp)); Text(text = "OFFICIAL ${session.uppercase()} HIGHLIGHTS", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) } else { Box(modifier = Modifier
                .fillMaxWidth()
                .height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFE10600)) } }; Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun SessionResultRow(result: Any) {
    val name: String; val pos: String; val teamColor: Color; val detail: String; val subDetail: String?
    when (result) {
        is RaceResult -> { name = "${result.Driver.givenName} ${result.Driver.familyName.uppercase()}"; pos = result.position; teamColor = getTeamColor(result.Constructor.constructorId); detail = result.status; subDetail = null }
        is QualifyingResult -> { name = "${result.Driver.givenName} ${result.Driver.familyName.uppercase()}"; pos = result.position; teamColor = getTeamColor(result.Constructor.constructorId); detail = result.Q3 ?: result.Q2 ?: result.Q1 ?: "--"; subDetail = null }
        is PracticeResultDisplay -> { name = result.driverName.uppercase(); pos = result.position.toString(); val driverStanding = viewModel<MainViewModel>().drivers.value.find { it.Driver.permanentNumber == result.driverNumber }; teamColor = getTeamColor(driverStanding?.Constructors?.lastOrNull()?.constructorId); detail = result.bestLapTime; subDetail = result.gap }
        else -> return
    }
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text(text = pos, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp)); Box(modifier = Modifier
        .width(4.dp)
        .height(24.dp)
        .background(teamColor)); Spacer(modifier = Modifier.width(12.dp)); Column(modifier = Modifier.weight(1f)) { Text(text = name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1); if (subDetail != null) { Text(text = subDetail, color = Color.Gray, style = MaterialTheme.typography.labelSmall) } }; Text(text = detail, color = Color.LightGray, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) }
}

fun getTeamColor(id: String?): Color = when (id?.lowercase()) {
    "red_bull" -> Color(0xFF3671C6); "mercedes" -> Color(0xFF27F4D2); "ferrari" -> Color(0xFFE80020); "mclaren" -> Color(0xFFFF8000); "aston_martin" -> Color(0xFF229971); "alpine" -> Color(0xFF0093CC); "williams" -> Color(0xFF64C4FF); "rb", "racing_bulls" -> Color(0xFF6692FF); "sauber", "kick_sauber" -> Color(0xFF52E252); "audi" -> Color(0xFFB1B3B3); "haas" -> Color(0xFFFFFFFF); "cadillac" -> Color(0xFFD4AF37); else -> Color(0xFFE10600)
}
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val newsArticles by viewModel.f1News
    val isLoading by viewModel.isNewsLoading
    val context = LocalContext.current

    // Automatically poll the countdown refresh state on page mount
    LaunchedEffect(Unit) {
        viewModel.updateCountdown()
        viewModel.fetchF1News()
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF15151E))) {
        // 🏁 HERO OVERVIEW TIMER (Always Fixed at Top)
        if (viewModel.countdownText.value.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = Color(0xFFE10600),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("NEXT RACE COUNTDOWN", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = viewModel.countdownText.value,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
                    )
                }
            }
        }

        Text(
            text = "LATEST HEADLINES",
            color = Color.White,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // 📰 NEWS ARTICLES LAYOUT COLUMN
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFE10600))
            }
        } else if (newsArticles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No news updates available right now.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(newsArticles) { article ->
                    NewsCard(article = article) {
                        val webUrl = article.links?.web?.href
                        if (!webUrl.isNullOrEmpty()) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
                            context.startActivity(intent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewsCard(article: EspnArticle, onClick: () -> Unit) {
    val imageUrl = article.images?.firstOrNull()?.url

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F27)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF38383F))
    ) {
        Column {
            if (!imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "News Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = article.headline,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!article.description.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = article.description,
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3
                    )
                }
            }
        }
    }
}