package com.example.grandprixhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.shape.CircleShape
import java.time.*
enum class TimeMode { MY_TIME, TRACK_TIME }
enum class SessionStatus { PAST, LIVE, UPCOMING }
//enum class ComparisonMode { SEASON, CAREER }
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = viewModel()
            var selectedTeamId by remember { mutableStateOf<String?>(null) }

            // State for Bottom Navigation
            var currentBottomTab by remember { mutableStateOf("Results") }

            Scaffold(
                containerColor = Color(0xFF15151E),
                topBar = {
                    // Hide header if in Race Detail or Comparison mode
                    val isComparisonMode = viewModel.selectedDriver1 != null && viewModel.selectedDriver2 != null
                    if (selectedTeamId == null && viewModel.selectedRace.value == null && !isComparisonMode) {
                        Column {
                            CenterAlignedTopAppBar(
                                title = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "F1 GRAND PRIX HUB",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontStyle = FontStyle.Italic
                                            )
                                        )
                                        Text(
                                            text = if (currentBottomTab == "Schedule")
                                                "${viewModel.selectedYear.value} CALENDAR"
                                            else "STANDINGS",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = Color.Gray,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                },
                                actions = { SeasonDropdown(viewModel) },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = Color(0xFF15151E),
                                    titleContentColor = Color.White
                                )
                            )

                            if (currentBottomTab == "Results") {
                                TabRow(
                                    selectedTabIndex = if (viewModel.isDriversTab.value) 0 else 1,
                                    containerColor = Color(0xFF15151E),
                                    contentColor = Color(0xFFE10600),
                                    indicator = { tabPositions ->
                                        TabRowDefaults.SecondaryIndicator(
                                            Modifier.tabIndicatorOffset(tabPositions[if (viewModel.isDriversTab.value) 0 else 1]),
                                            color = Color(0xFFE10600)
                                        )
                                    }
                                ) {
                                    Tab(
                                        selected = viewModel.isDriversTab.value,
                                        onClick = { viewModel.isDriversTab.value = true },
                                        text = { Text("DRIVERS", color = Color.White, fontWeight = FontWeight.Bold) }
                                    )
                                    Tab(
                                        selected = !viewModel.isDriversTab.value,
                                        onClick = { viewModel.isDriversTab.value = false },
                                        text = { Text("TEAMS", color = Color.White, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = Color(0xFF1F1F27),
                        contentColor = Color.White
                    ) {
                        NavigationBarItem(
                            selected = currentBottomTab == "Schedule",
                            onClick = {
                                currentBottomTab = "Schedule"
                                selectedTeamId = null
                                viewModel.clearSelectedRace()
                                viewModel.clearComparison()
                            },
                            label = { Text("Schedule", fontWeight = FontWeight.Bold) },
                            icon = { Icon(painterResource(android.R.drawable.ic_menu_today), contentDescription = null) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFFE10600),
                                selectedTextColor = Color(0xFFE10600),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color.Transparent
                            )
                        )
                        NavigationBarItem(
                            selected = currentBottomTab == "Results",
                            onClick = {
                                currentBottomTab = "Results"
                                viewModel.clearSelectedRace()
                            },
                            label = { Text("Results", fontWeight = FontWeight.Bold) },
                            icon = { Icon(painterResource(android.R.drawable.ic_menu_sort_by_size), contentDescription = null) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFFE10600),
                                selectedTextColor = Color(0xFFE10600),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color.Transparent
                            )
                        )
                        // Inside NavigationBar in MainActivity.kt
                        NavigationBarItem(
                            selected = currentBottomTab == "Compare",
                            onClick = {
                                currentBottomTab = "Compare"
                                selectedTeamId = null
                                viewModel.clearSelectedRace()
                            },
                            label = { Text("Compare", fontWeight = FontWeight.Bold) },
                            icon = { Icon(painterResource(android.R.drawable.ic_menu_share), contentDescription = null) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFFE10600),
                                selectedTextColor = Color(0xFFE10600),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    when (currentBottomTab) {
                        "Schedule" -> {
                            if (viewModel.selectedRace.value == null) {
                                ScheduleScreen(viewModel)
                            } else {
                                RaceDetailScreen(viewModel)
                            }
                        }
                        "Results" -> {
                            if (selectedTeamId == null) {
                                if (viewModel.isDriversTab.value) {
                                    DriverListScreen(viewModel)
                                } else {
                                    ConstructorListScreen(viewModel) { teamId -> selectedTeamId = teamId }
                                }
                            } else {
                                TeamDetailScreen(
                                    teamId = selectedTeamId!!,
                                    viewModel = viewModel,
                                    onBack = { selectedTeamId = null }
                                )
                            }
                        }
                        "Compare" -> {
                            if (viewModel.selectedDriver1 != null && viewModel.selectedDriver2 != null) {
                                DriverComparisonScreen(viewModel)
                            } else {
                                ComparisonSelectionScreen(viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- TIME & FORMATTING HELPERS ---

fun formatToDisplayTime(apiDate: String, apiTime: String, mode: TimeMode, circuitCountry: String): String {
    return try {
        val utcTime = apiTime.replace("Z", "")
        val utcDateTime = LocalDateTime.parse("${apiDate}T$utcTime").atZone(ZoneId.of("UTC"))
        val targetTime = when (mode) {
            TimeMode.MY_TIME -> utcDateTime.withZoneSameInstant(ZoneId.systemDefault())
            TimeMode.TRACK_TIME -> utcDateTime.withZoneSameInstant(ZoneId.of(getTrackTimeZone(circuitCountry)))
        }
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
        val startDate = raceDate.minusDays(2)
        "${startDate.dayOfMonth}-${raceDate.dayOfMonth} ${raceDate.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH))}"
    } catch (e: Exception) { apiDate }
}

fun formatSessionDate(apiDate: String): String {
    return try {
        LocalDate.parse(apiDate).format(DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH))
    } catch (e: Exception) { apiDate }
}

// --- SCREENS ---

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
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFE10600))
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            if (viewModel.countdownText.value.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    color = Color(0xFFE10600),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NEXT RACE COUNTDOWN", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                        Text(viewModel.countdownText.value, color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic))
                    }
                }
            }
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), state = viewModel.scheduleListState) {
                items(raceList) { race ->
                    RaceCard(race = race, onClick = { viewModel.selectRace(race) })
                }
            }
        }
    }
}

@Composable
fun RaceCard(race: APIRace, onClick: () -> Unit) {
    val winner = race.Results?.firstOrNull()?.Driver
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F27)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF38383F))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "ROUND ${race.round}", color = Color(0xFFE10600), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(text = race.raceName.uppercase(), color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic))
                }
                Text(text = formatRaceWeekend(race.date), color = Color.White, fontWeight = FontWeight.Bold)
            }
            if (winner != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painter = painterResource(id = android.R.drawable.btn_star_big_on), contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "WINNER: ${winner.givenName} ${winner.familyName.uppercase()}", color = Color.LightGray, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RaceDetailScreen(viewModel: MainViewModel) {
    val race = viewModel.selectedRace.value ?: return
    val circuitData = CircuitRepository.getDetails(race.Circuit.circuitId)

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF15151E))) {
        TextButton(onClick = { viewModel.clearSelectedRace() }, modifier = Modifier.padding(8.dp)) {
            Text("< BACK TO CALENDAR", color = Color(0xFFE10600), fontWeight = FontWeight.Bold)
        }

        LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
            item {
                Text(text = race.raceName.uppercase(), style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Black)
                Text(text = race.Circuit.circuitName, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(24.dp))
                if (circuitData.imageRes != null) {
                    Image(painter = painterResource(id = circuitData.imageRes), contentDescription = null, modifier = Modifier.fillMaxWidth().height(250.dp), contentScale = ContentScale.Fit)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DetailInfo("LAPS", "${circuitData.laps}")
                    DetailInfo("ROUND", race.round)
                    DetailInfo("DATE", formatRaceWeekend(race.date))
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text("HISTORY", color = Color(0xFFE10600), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
                Text(text = circuitData.description, color = Color.LightGray, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("WEEKEND SCHEDULE", color = Color(0xFFE10600), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = { viewModel.toggleTimeMode() }) {
                        Text(text = if (viewModel.timeMode == TimeMode.MY_TIME) "SHOW TRACK TIME" else "SHOW MY TIME", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    val mode = viewModel.timeMode
                    val country = race.Circuit.Location.country
                    race.FirstPractice?.let { TimelineItem("Practice 1", formatSessionDate(it.date), formatToDisplayTime(it.date, it.time, mode, country), false, getSessionStatus(it.date, it.time)) }
                    if (race.Sprint != null) {
                        race.Qualifying?.let { TimelineItem("Sprint Qualifying", formatSessionDate(it.date), formatToDisplayTime(it.date, it.time, mode, country), false, getSessionStatus(it.date, it.time)) }
                        race.Sprint?.let { TimelineItem("Sprint Race", formatSessionDate(it.date), formatToDisplayTime(it.date, it.time, mode, country), false, getSessionStatus(it.date, it.time)) }
                    } else {
                        race.SecondPractice?.let { TimelineItem("Practice 2", formatSessionDate(it.date), formatToDisplayTime(it.date, it.time, mode, country), false, getSessionStatus(it.date, it.time)) }
                        race.ThirdPractice?.let { TimelineItem("Practice 3", formatSessionDate(it.date), formatToDisplayTime(it.date, it.time, mode, country), false, getSessionStatus(it.date, it.time)) }
                    }
                    race.Qualifying?.let { TimelineItem("Qualifying", formatSessionDate(it.date), formatToDisplayTime(it.date, it.time, mode, country), false, getSessionStatus(it.date, it.time)) }
                    TimelineItem("Grand Prix", formatSessionDate(race.date), formatToDisplayTime(race.date, race.time ?: "15:00:00Z", mode, country), true, getSessionStatus(race.date, race.time ?: "15:00:00Z"))
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun DriverListScreen(viewModel: MainViewModel) {
    val driverStandings = viewModel.drivers.value
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(driverStandings) { standing ->
            DriverCard(standing) // No longer passing viewModel here
        }
    }
}

@Composable
fun ConstructorListScreen(viewModel: MainViewModel, onTeamClick: (String) -> Unit) {
    val constructorStandings = viewModel.constructors.value
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(constructorStandings) { standing ->
            ConstructorCard(standing, onTeamClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailScreen(teamId: String, viewModel: MainViewModel, onBack: () -> Unit) {
    val teamDrivers = viewModel.getDriversForTeam(teamId)
    val teamColor = getTeamColor(teamId)
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF15151E))) {
        CenterAlignedTopAppBar(
            title = { Text(teamId.replace("_", " ").uppercase(), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            navigationIcon = { TextButton(onClick = onBack) { Text("< BACK", color = teamColor, fontWeight = FontWeight.Bold) } },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF15151E), titleContentColor = Color.White)
        )
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            items(teamDrivers) { standing -> DriverDetailCard(standing, teamColor) }
        }
    }
}

// --- COMPONENTS ---

@Composable
fun DriverCard(standing: DriverStanding) { // Removed viewModel parameter
    val driver = standing.Driver
    val teamId = standing.Constructors.lastOrNull()?.constructorId
    val teamColor = getTeamColor(teamId)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 12.dp), // Removed .clickable
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F27)),
        shape = RoundedCornerShape(topStart = 0.dp, bottomEnd = 12.dp),
        border = BorderStroke(1.dp, Color(0xFF38383F)) // Constant border
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = standing.position, color = Color.White, modifier = Modifier.width(24.dp), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.width(4.dp).height(45.dp).background(teamColor))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = driver.familyName.uppercase(), color = Color.White, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic))
                Text(text = driver.givenName, color = Color.LightGray)
            }
            Spacer(modifier = Modifier.weight(1f))
            Surface(color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))) {
                Text(text = "${standing.points} PTS", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = driver.permanentNumber, color = Color.White.copy(alpha = 0.07f), style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun DriverComparisonScreen(viewModel: MainViewModel) {
    val d1 = viewModel.selectedDriver1 ?: return
    val d2 = viewModel.selectedDriver2 ?: return

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF15151E)).padding(16.dp)) {
        TextButton(onClick = { viewModel.clearComparison() }) {
            Text("< BACK TO STANDINGS", color = Color(0xFFE10600), fontWeight = FontWeight.Bold)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Button(onClick = { viewModel.comparisonMode = ComparisonMode.SEASON }, colors = ButtonDefaults.buttonColors(containerColor = if (viewModel.comparisonMode == ComparisonMode.SEASON) Color(0xFFE10600) else Color.DarkGray)) { Text("SEASON") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { viewModel.comparisonMode = ComparisonMode.CAREER }, colors = ButtonDefaults.buttonColors(containerColor = if (viewModel.comparisonMode == ComparisonMode.CAREER) Color(0xFFE10600) else Color.DarkGray)) { Text("CAREER") }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Side-by-Side Images and Stats
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            // Driver 1 Column
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                DriverImage(d1.Driver.driverId) // Helper function added below
                Spacer(modifier = Modifier.height(8.dp))
                Text(d1.Driver.familyName.uppercase(), color = Color.White, fontWeight = FontWeight.Black)
                StatBox("Points", if(viewModel.comparisonMode == ComparisonMode.SEASON) d1.points else "---")
            }

            Text("VS", modifier = Modifier.padding(bottom = 40.dp), color = Color.Gray, fontWeight = FontWeight.Bold)

            // Driver 2 Column
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                DriverImage(d2.Driver.driverId) // Helper function added below
                Spacer(modifier = Modifier.height(8.dp))
                Text(d2.Driver.familyName.uppercase(), color = Color.White, fontWeight = FontWeight.Black)
                StatBox("Points", if(viewModel.comparisonMode == ComparisonMode.SEASON) d2.points else "---")
            }
        }
    }
}
@Composable
fun TimelineItem(sessionName: String, date: String, time: String, isLast: Boolean, status: SessionStatus) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
            when (status) {
                SessionStatus.PAST -> Text("🏁", fontSize = 14.sp)
                SessionStatus.LIVE -> Box(modifier = Modifier.size(12.dp).background(Color.Green, CircleShape))
                else -> Box(modifier = Modifier.size(12.dp).background(Color(0xFFE10600), CircleShape))
            }
            if (!isLast) Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.Gray.copy(alpha = 0.3f)))
        }
        Column(modifier = Modifier.padding(start = 12.dp, bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(sessionName, color = Color.White, fontWeight = FontWeight.Bold)
                if (status == SessionStatus.LIVE) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("LIVE", color = Color.Green, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
                }
            }
            Text("$date | $time", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 12.dp)) {
        Text(text = label.uppercase(), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        Text(text = value, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DetailInfo(label: String, value: String) {
    Column {
        Text(label, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DriverDetailCard(standing: DriverStanding, teamColor: Color) {
    val driver = standing.Driver
    val cleanId = if (driver.driverId.contains("colapinto", ignoreCase = true)) "franco-colapinto" else driver.driverId.split("_").last()
    val imageUrl = "https://media.formula1.com/content/dam/fom-website/drivers/2025Drivers/${cleanId}.png"
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F27)), border = BorderStroke(1.dp, teamColor.copy(alpha = 0.3f))) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.size(100.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)), contentScale = ContentScale.Fit, error = painterResource(android.R.drawable.ic_menu_gallery))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(driver.familyName.uppercase(), color = Color.White, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                Text(driver.givenName, color = Color.LightGray)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Text("NO. ${driver.permanentNumber}", color = teamColor, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("RANK ${standing.position}", color = Color.Gray, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(text = standing.points, color = Color.White, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonDropdown(viewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val seasons = listOf("2024", "2025", "2026")
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        Text(text = viewModel.selectedYear.value, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.menuAnchor().padding(16.dp).clickable { expanded = true })
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color(0xFF1F1F27))) {
            seasons.forEach { year ->
                DropdownMenuItem(text = { Text(year, color = Color.White) }, onClick = { viewModel.updateYear(year); expanded = false })
            }
        }
    }
}

@Composable
fun ConstructorCard(standing: ConstructorStanding, onTeamClick: (String) -> Unit) {
    val team = standing.Constructor
    val teamColor = getTeamColor(team.constructorId)
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 12.dp).clickable { onTeamClick(team.constructorId) }, colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F27)), shape = RoundedCornerShape(topStart = 0.dp, bottomEnd = 12.dp), border = BorderStroke(1.dp, Color(0xFF38383F))) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = standing.position, color = Color.White, modifier = Modifier.width(24.dp), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.width(4.dp).height(40.dp).background(teamColor))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = team.name.uppercase(), color = Color.White, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic))
                Text(text = team.nationality, color = teamColor, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "${standing.points} PTS", color = Color.White, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = ">", color = Color.Gray)
        }
    }
}

fun getSessionStatus(apiDate: String, apiTime: String): SessionStatus {
    return try {
        val utcTime = apiTime.replace("Z", "")
        val sessionStart = LocalDateTime.parse("${apiDate}T$utcTime").atZone(ZoneId.of("UTC"))
        val now = ZonedDateTime.now(ZoneId.of("UTC"))
        val sessionEnd = sessionStart.plusHours(2)
        when {
            now.isAfter(sessionEnd) -> SessionStatus.PAST
            now.isAfter(sessionStart) && now.isBefore(sessionEnd) -> SessionStatus.LIVE
            else -> SessionStatus.UPCOMING
        }
    } catch (e: Exception) { SessionStatus.UPCOMING }
}
@Composable
fun ComparisonSelectionScreen(viewModel: MainViewModel) {
    val allDrivers = viewModel.drivers.value

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("SELECT TWO DRIVERS", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
        Text("Tap drivers to add to comparison", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(16.dp))

        // Showing current selection slots
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectionSlot("Driver 1", viewModel.selectedDriver1,modifier = Modifier.weight(1f))
            SelectionSlot("Driver 2", viewModel.selectedDriver2,modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(allDrivers) { standing ->
                val isSelected = viewModel.selectedDriver1 == standing || viewModel.selectedDriver2 == standing

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { viewModel.selectDriverForComparison(standing) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFFE10600).copy(alpha = 0.2f) else Color(0xFF1F1F27)
                    ),
                    border = if (isSelected) BorderStroke(1.dp, Color(0xFFE10600)) else null
                ) {
                    Text(
                        text = "${standing.Driver.givenName} ${standing.Driver.familyName.uppercase()}",
                        modifier = Modifier.padding(16.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SelectionSlot(label: String, driver: DriverStanding?, modifier: Modifier = Modifier) {
    Surface(
        // Use the passed-in modifier here instead of fillMaxWidth(0.5f)
        modifier = modifier.height(60.dp),
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (driver != null) Color(0xFFE10600) else Color.Gray.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.Center) {
            Text(label, color = Color.Gray, fontSize = 10.sp)
            Text(
                text = driver?.Driver?.familyName?.uppercase() ?: "EMPTY",
                color = if (driver != null) Color.White else Color.DarkGray,
                fontWeight = FontWeight.Bold,
                maxLines = 1 // Prevents layout breaks if names are long
            )
        }
    }
}
@Composable
fun DriverImage(driverId: String) {
    // Standardize IDs for the F1 media server
    val cleanId = if (driverId.contains("colapinto", ignoreCase = true)) "franco-colapinto"
    else driverId.split("_").last()

    val imageUrl = "https://media.formula1.com/content/dam/fom-website/drivers/2025Drivers/${cleanId}.png"

    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        modifier = Modifier
            .size(120.dp)
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Fit,
        error = painterResource(android.R.drawable.ic_menu_gallery)
    )
}
fun getTeamColor(id: String?): Color = when (id?.lowercase()) {
    "red_bull" -> Color(0xFF3671C6)
    "mercedes" -> Color(0xFF27F4D2)
    "ferrari" -> Color(0xFFE80020)
    "mclaren" -> Color(0xFFFF8000)
    "aston_martin" -> Color(0xFF229971)
    "alpine" -> Color(0xFF0093CC)
    "williams" -> Color(0xFF64C4FF)
    "rb", "racing_bulls" -> Color(0xFF6692FF)
    "sauber", "kick_sauber", "audi" -> Color(0xFF52E252)
    "haas" -> Color(0xFFB6BABD)
    else -> Color(0xFFE10600)
}