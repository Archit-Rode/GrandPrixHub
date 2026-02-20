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
                    // We only show this dual header if we aren't viewing a specific team detail or race detail
                    if (selectedTeamId == null && viewModel.selectedRace.value == null) {
                        Column {
                            CenterAlignedTopAppBar(
                                title = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        // PRIMARY TITLE
                                        Text(
                                            text = "F1 GRAND PRIX HUB",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontStyle = FontStyle.Italic
                                            )
                                        )
                                        // DYNAMIC SUBTITLE BASED ON TAB
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

                            // SHOW TABS ONLY ON RESULTS PAGE
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
                                viewModel.clearSelectedRace() // Clear race when switching back
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
                    }
                }
            }
        }
    }
}

// Helper to calculate and format the race weekend
fun formatRaceWeekend(apiDate: String): String {
    return try {
        val raceDate = LocalDate.parse(apiDate)
        val startDate = raceDate.minusDays(2)
        val monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
        val month = raceDate.format(monthFormatter)

        "${startDate.dayOfMonth}-${raceDate.dayOfMonth} $month"
    } catch (e: Exception) {
        apiDate
    }
}

@Composable
fun ScheduleScreen(viewModel: MainViewModel) {
    val raceList = viewModel.schedule.value

    // Trigger countdown updates safely
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
            // Only show header if we have text
            if (viewModel.countdownText.value.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    color = Color(0xFFE10600),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("NEXT RACE COUNTDOWN", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                        Text(viewModel.countdownText.value, color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic))
                    }
                }
            }

            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
                items(raceList) { race ->
                    RaceCard(race = race, onClick = { viewModel.selectRace(race) })
                }
            }
        }
    }
}
@Composable
fun RaceCard(race: APIRace, onClick: () -> Unit) {
    val winner = race.Results?.firstOrNull()?.Driver // Get the P1 driver

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

            // NEW: WINNER DISPLAY
            if (winner != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.btn_star_big_on),
                        contentDescription = null,
                        tint = Color(0xFFFFD700), // Gold color
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "WINNER: ${winner.givenName} ${winner.familyName.uppercase()}",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RaceDetailScreen(viewModel: MainViewModel) {
    val race = viewModel.selectedRace.value ?: return
    val circuitData = CircuitRepository.getDetails(race.Circuit.circuitId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF15151E))
    ) {
        TextButton(
            onClick = { viewModel.clearSelectedRace() },
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                "< BACK TO CALENDAR",
                color = Color(0xFFE10600),
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
            item {
                Text(
                    text = race.raceName.uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = race.Circuit.circuitName,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                // CHANGED: Use Image with painterResource for local drawables
                if (circuitData.imageRes != null) {
                    Image(
                        painter = painterResource(id = circuitData.imageRes),
                        contentDescription = "Circuit Layout",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailInfo("LAPS", "${circuitData.laps}")
                    DetailInfo("ROUND", race.round)
                    DetailInfo("DATE", formatRaceWeekend(race.date))
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "HISTORY",
                    color = Color(0xFFE10600),
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.labelLarge
                )

                Text(
                    text = circuitData.description,
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                )
            }
        }
    }
}

@Composable
fun DetailInfo(label: String, value: String) {
    Column {
        Text(label, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonDropdown(viewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val seasons = listOf("2024", "2025", "2026")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        Text(
            text = viewModel.selectedYear.value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .menuAnchor()
                .padding(16.dp)
                .clickable { expanded = true }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF1F1F27))
        ) {
            seasons.forEach { year ->
                DropdownMenuItem(
                    text = { Text(year, color = Color.White) },
                    onClick = {
                        viewModel.updateYear(year)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DriverListScreen(viewModel: MainViewModel) {
    val driverStandings = viewModel.drivers.value
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(driverStandings) { standing ->
            DriverCard(standing)
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
            title = {
                Text(
                    teamId.replace("_", " ").uppercase(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            navigationIcon = {
                TextButton(onClick = onBack) {
                    Text("< BACK", color = teamColor, fontWeight = FontWeight.Bold)
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color(0xFF15151E),
                titleContentColor = Color.White
            )
        )
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            items(teamDrivers) { standing ->
                DriverDetailCard(standing, teamColor)
            }
        }
    }
}

@Composable
fun DriverCard(standing: DriverStanding) {
    val driver = standing.Driver
    val teamId = standing.Constructors.lastOrNull()?.constructorId
    val teamColor = getTeamColor(teamId)

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F27)),
        shape = RoundedCornerShape(topStart = 0.dp, bottomEnd = 12.dp),
        border = BorderStroke(1.dp, Color(0xFF38383F))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = standing.position,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.width(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(modifier = Modifier.width(4.dp).height(45.dp).background(teamColor))

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = driver.familyName.uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic
                    )
                )
                Text(text = driver.givenName, color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Text(
                    text = "${standing.points} PTS",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = driver.permanentNumber,
                color = Color.White.copy(alpha = 0.07f),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun ConstructorCard(standing: ConstructorStanding, onTeamClick: (String) -> Unit) {
    val team = standing.Constructor
    val teamColor = getTeamColor(team.constructorId)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 12.dp)
            .clickable { onTeamClick(team.constructorId) },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F27)),
        shape = RoundedCornerShape(topStart = 0.dp, bottomEnd = 12.dp),
        border = BorderStroke(1.dp, Color(0xFF38383F))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = standing.position,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.width(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(modifier = Modifier.width(4.dp).height(40.dp).background(teamColor))

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = team.name.uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic
                    )
                )
                Text(text = team.nationality, color = teamColor, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "${standing.points} PTS",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
            )

            Spacer(modifier = Modifier.width(12.dp))
            Text(text = ">", color = Color.Gray)
        }
    }
}

@Composable
fun DriverDetailCard(standing: DriverStanding, teamColor: Color) {
    val driver = standing.Driver
    val cleanId = if (driver.driverId.contains("colapinto", ignoreCase = true)) "franco-colapinto" else driver.driverId.split("_").last()
    val imageUrl = "https://media.formula1.com/content/dam/fom-website/drivers/2025Drivers/${cleanId}.png"

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F27)),
        border = BorderStroke(1.dp, teamColor.copy(alpha = 0.3f))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.size(100.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit,
                error = painterResource(android.R.drawable.ic_menu_gallery)
            )
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

fun getTeamColor(id: String?): Color {
    return when (id?.lowercase()) {
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
}