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

    val currentWeather = mutableStateOf<APIWeather?>(null)

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
            val now = LocalDateTime.now()
            val sessionDate = LocalDateTime.parse(sessionTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            // Calculate delay for 15 minutes before start
            val delayInMinutes = Duration.between(now, sessionDate).toMinutes() - 15
            if (delayInMinutes > 0) {
                val data = Data.Builder()
                    .putString("SESSION_NAME", sessionName)
                    .putString("RACE_NAME", raceName)
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                    .setInitialDelay(delayInMinutes, TimeUnit.MINUTES)
                    .setInputData(data)
                    .build()

                // Use unique work name to avoid duplicates per session
                WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                    "${raceName}_${sessionName}",
                    ExistingWorkPolicy.KEEP,
                    workRequest
                )
            }
        } catch (e: Exception) {
            // Log parsing errors if the API date format is unexpected
        }
    }

    // --- DATA FETCHING ---

    private fun fetchSchedule(year: String) {
        viewModelScope.launch {
            try {
                // 1. Cleanup old notifications before scheduling new ones
                WorkManager.getInstance(getApplication()).cancelAllWork()

                // 2. Fetch the FULL calendar
                val scheduleResponse = apiService.getSeasonSchedule(year)
                val fullCalendar = scheduleResponse.MRData.RaceTable.Races

                // 3. Fetch current results (e.g., Round 1 results in 2026)
                val resultsResponse = apiService.getFullSeasonResults(year)
                val resultsList = resultsResponse.MRData.RaceTable.Races

                // 4. Merge results into the calendar (keep future races as-is)
                val mergedList = fullCalendar.map { calendarRace ->
                    resultsList.find { it.round == calendarRace.round } ?: calendarRace
                }

                if (mergedList.isNotEmpty()) {
                    schedule.value = mergedList
                    // Only schedule future notifications
                    mergedList.forEach { scheduleAllSessions(it) }
                }
                updateCountdown()
            } catch (e: Exception) {
                // Fallback to schedule-only if results fail
                try {
                    val scheduleResponse = apiService.getSeasonSchedule(year)
                    schedule.value = scheduleResponse.MRData.RaceTable.Races
                    updateCountdown()
                } catch (inner: Exception) {
                    schedule.value = emptyList()
                }
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
}