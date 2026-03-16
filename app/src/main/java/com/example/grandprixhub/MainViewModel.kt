package com.example.grandprixhub

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.Duration
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyListState
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import androidx.work.workDataOf

// Changed to AndroidViewModel to access application context for WorkManager
class MainViewModel(application: Application) : AndroidViewModel(application) {

    // 1. UI State
    val isDriversTab = mutableStateOf(true)
    val selectedYear = mutableStateOf("2025")

    val drivers = mutableStateOf<List<DriverStanding>>(emptyList())
    val constructors = mutableStateOf<List<ConstructorStanding>>(emptyList())
    val schedule = mutableStateOf<List<APIRace>>(emptyList())

    val selectedRace = mutableStateOf<APIRace?>(null)
    val countdownText = mutableStateOf("")
    val scheduleListState = LazyListState()

    // Comparison States
    var selectedDriver1 by mutableStateOf<DriverStanding?>(null)
    var selectedDriver2 by mutableStateOf<DriverStanding?>(null)
    var timeMode by mutableStateOf(TimeMode.MY_TIME)

    var driver1DNA = mutableStateOf<Map<String, Float>>(emptyMap())
    var driver2DNA = mutableStateOf<Map<String, Float>>(emptyMap())

    val currentWeather = mutableStateOf<APIWeather?>(null)

    val selectedSessionResults = mutableStateOf<List<Any>>(emptyList())
    val selectedSessionType = mutableStateOf("") // e.g., "QUALIFYING"
    val isShowingResults = mutableStateOf(false)

    // 2. Retrofit Setup
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.jolpi.ca/ergast/f1/")
        .client(
            okhttp3.OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", "GrandPrixHub/1.0")
                        .build()
                    chain.proceed(request)
                }
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(F1ApiService::class.java)

    init {
        fetchData()
    }

    // --- NOTIFICATION SCHEDULING LOGIC ---

    private fun scheduleAllSessions(race: APIRace) {
        fun formatTime(date: String, time: String?) = "${date}T${time?.replace("Z", "") ?: "15:00:00"}"

        // Schedule Practice Sessions
        race.FirstPractice?.let { scheduleNotification(formatTime(it.date, it.time), "Free Practice 1", race.raceName) }
        race.SecondPractice?.let { scheduleNotification(formatTime(it.date, it.time), "Free Practice 2", race.raceName) }
        race.ThirdPractice?.let { scheduleNotification(formatTime(it.date, it.time), "Free Practice 3", race.raceName) }

        // Schedule Qualifying & Sprint
        race.Qualifying?.let { scheduleNotification(formatTime(it.date, it.time), "Qualifying", race.raceName) }
        race.Sprint?.let { scheduleNotification(formatTime(it.date, it.time), "Sprint Race", race.raceName) }

        // Schedule Main Race
        scheduleNotification(formatTime(race.date, race.time), "Main Race", race.raceName)
    }

    fun scheduleNotification(sessionTime: String, sessionName: String, raceName: String) {
        try {
            // 1. Get the current moment in absolute time (UTC)
            val now = Instant.now()

            // 2. Parse the API time and explicitly tell Kotlin it is UTC
            val sessionInstant = LocalDateTime.parse(sessionTime)
                .atZone(ZoneId.of("UTC"))
                .toInstant()

            // 3. Calculate delay in seconds for better precision
            // Subtract 900 seconds (15 minutes)
            val delayInSeconds = ChronoUnit.SECONDS.between(now, sessionInstant) - (15 * 60)

            if (delayInSeconds > 0) {
                val data = workDataOf(
                    "SESSION_NAME" to sessionName,
                    "RACE_NAME" to raceName
                )

                val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                    .setInitialDelay(delayInSeconds, TimeUnit.SECONDS)
                    .setInputData(data)
                    .build()

                // 4. Use REPLACE to overwrite any old, incorrectly timed tasks
                WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                    "${raceName}_${sessionName}",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- DATA FETCHING ---

    private fun fetchSchedule(year: String) {
        viewModelScope.launch {
            try {
                // 1. Fetch Calendar and Results in parallel for speed
                val scheduleResponse = apiService.getSeasonSchedule(year)
                val fullCalendar = scheduleResponse.MRData.RaceTable.Races

                // Try to get results, but don't crash if it's empty (like at start of 2026)
                val resultsList = try {
                    apiService.getFullSeasonResults(year).MRData.RaceTable.Races
                } catch (e: Exception) {
                    emptyList()
                }

                // 2. Smart Merge with a fallback
                val mergedList = fullCalendar.map { calendarRace ->
                    val matchingResult = resultsList.find { it.round == calendarRace.round }

                    // If results exist, add them. If not, keep the calendar as is.
                    if (matchingResult != null) {
                        calendarRace.copy(Results = matchingResult.Results)
                    } else {
                        calendarRace
                    }
                }

                schedule.value = mergedList

                // Only schedule notifications for races that HAVEN'T happened yet
                mergedList.forEach { race ->
                    val raceDate = LocalDate.parse(race.date)
                    if (raceDate.isAfter(LocalDate.now()) || raceDate.isEqual(LocalDate.now())) {
                        scheduleAllSessions(race)
                    }
                }
                updateCountdown()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun fetchData() {
        val year = selectedYear.value
        fetchSchedule(year)

        viewModelScope.launch {
            try {
                val driverResponse = apiService.getDriverStandings(year)
                // Use .orEmpty() to ensure you never handle a null list
                val dLists = driverResponse.MRData.StandingsTable.StandingsLists
                drivers.value = dLists.firstOrNull()?.DriverStandings?.filterNotNull() ?: emptyList()

                val constructorResponse = apiService.getConstructorStandings(year)
                val cLists = constructorResponse.MRData.StandingsTable.StandingsLists
                constructors.value = cLists.firstOrNull()?.ConstructorStandings?.filterNotNull() ?: emptyList()
            } catch (e: Exception) {
                drivers.value = emptyList()
                constructors.value = emptyList()
            }
        }
    }

    // --- HELPERS ---

    fun updateYear(newYear: String) {
        selectedYear.value = newYear
        fetchData()
    }

    fun selectRace(race: APIRace) { selectedRace.value = race }
    fun clearSelectedRace() { selectedRace.value = null }
    fun toggleTimeMode() { timeMode = if (timeMode == TimeMode.MY_TIME) TimeMode.TRACK_TIME else TimeMode.MY_TIME }

    fun selectDriverForComparison(driver: DriverStanding) {
        when {
            selectedDriver1 == null -> selectedDriver1 = driver
            selectedDriver2 == null && driver != selectedDriver1 -> selectedDriver2 = driver
            driver == selectedDriver1 -> selectedDriver1 = null
            driver == selectedDriver2 -> selectedDriver2 = null
            else -> {
                selectedDriver1 = driver
                selectedDriver2 = null
            }
        }
    }

    fun clearComparison() {
        selectedDriver1 = null
        selectedDriver2 = null
    }

    fun updateCountdown() {
        if (schedule.value.isEmpty()) return
        try {
            val now = LocalDateTime.now()

            // Find the next race where the race date/time is in the future
            val nextRace = schedule.value.firstOrNull { race ->
                val raceDateTime = LocalDate.parse(race.date)
                    .atTime(LocalTime.parse(race.time?.replace("Z", "") ?: "15:00:00"))
                raceDateTime.isAfter(now)
            }

            if (nextRace != null) {
                val raceDateTime = LocalDate.parse(nextRace.date)
                    .atTime(LocalTime.parse(nextRace.time?.replace("Z", "") ?: "15:00:00"))
                val diff = Duration.between(now, raceDateTime)

                // Format to show Days, Hours, and Minutes
                countdownText.value = "${nextRace.raceName.uppercase()}: ${diff.toDays()}D ${diff.toHours() % 24}H ${diff.toMinutes() % 60}M"
            } else {
                // Only show completed if there truly are no more races left in the list
                countdownText.value = "SEASON COMPLETED"
            }
        } catch (e: Exception) {
            countdownText.value = "TIMER UNAVAILABLE"
        }
    }

    fun getDriversForTeam(constructorId: String): List<DriverStanding> {
        return drivers.value.filter { it.Constructors.lastOrNull()?.constructorId == constructorId }
    }

    fun fetchWeather() {
        viewModelScope.launch {
            try {
                // We use the full OpenF1 URL here to override the Ergast base URL
                val openF1Url = "https://api.openf1.org/v1/weather?session_key=latest"
                val response = apiService.getSessionWeather(openF1Url)

                // OpenF1 returns a list of logs; we want the most recent one
                currentWeather.value = response.lastOrNull()
            } catch (e: Exception) {
                currentWeather.value = null
            }
        }
    }

    fun loadDriverStats(driverId: String, isDriverOne: Boolean) {
        viewModelScope.launch {
            try {
                // Fetch the last 10-20 results for this driver
                val response = apiService.getDriverCareerResults(driverId)
                val results = response.MRData.RaceTable.Races.flatMap { it.Results ?: emptyList() }

                val scores = calculateRadarMetrics(results)
                if (isDriverOne) driver1DNA.value = scores else driver2DNA.value = scores
            } catch (e: Exception) { /* Handle error */ }
        }
    }

    private fun calculateRadarMetrics(results: List<RaceResult>): Map<String, Float> {
        if (results.isEmpty()) return emptyMap()

        // 1. Qualy Pace: 10 - (Avg Grid / 2). Capped 1-10.
        val avgGrid = results.map { it.grid.toFloat() }.average().toFloat()
        val qualyScore = (11f - avgGrid).coerceIn(1f, 10f)

        // 2. Race Craft: Gain/Loss. Only count finished races.
        val classified = results.filter { it.status == "Finished" || it.status.contains("Lap") }
        val avgGained = if (classified.isNotEmpty()) {
            classified.map { it.grid.toInt() - it.position.toInt() }.average().toFloat()
        } else 0f
        val craftScore = (5f + avgGained).coerceIn(1f, 10f)

        // 3. Peak Performance: Wins (3pt) and Podiums (1pt).
        val wins = results.count { it.position == "1" }
        val podiums = results.count { it.position.toInt() in 1..3 }
        val peakScore = ((wins * 3f) + podiums).coerceIn(1f, 10f)

        return mapOf(
            "Qualy Pace" to qualyScore,
            "Race Craft" to craftScore,
            "Peak Performance" to peakScore
        )
    }
    fun fetchSessionResults(year: String, round: String, type: String) {
        viewModelScope.launch {
            try {
                when (type) {
                    "qualifying" -> {
                        val response = apiService.getQualifyingResults(year, round)
                        val race = response.MRData.RaceTable.Races.firstOrNull()
                        selectedSessionResults.value = race?.QualifyingResults ?: emptyList()
                        selectedSessionType.value = "QUALIFYING"
                    }
                    "sprint" -> {
                        val response = apiService.getSprintResults(year, round)
                        val race = response.MRData.RaceTable.Races.firstOrNull()
                        selectedSessionResults.value = race?.SprintResults ?: emptyList()
                        selectedSessionType.value = "SPRINT RACE"
                    }
                    "results" -> {
                        val response = apiService.getRaceResults(year, round)
                        val race = response.MRData.RaceTable.Races.firstOrNull()
                        selectedSessionResults.value = race?.Results ?: emptyList()
                        selectedSessionType.value = "GRAND PRIX"
                    }
                }
                isShowingResults.value = selectedSessionResults.value.isNotEmpty()
            } catch (e: Exception) {
                selectedSessionResults.value = emptyList()
                isShowingResults.value = false
            }
        }
    }
}