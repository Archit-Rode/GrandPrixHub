package com.example.grandprixhub

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import java.util.Locale

class MainViewModel : ViewModel() {
    // 1. UI State: Track active tab, selected year, and data lists
    val isDriversTab = mutableStateOf(true)
    val selectedYear = mutableStateOf("2025")

    val drivers = mutableStateOf<List<DriverStanding>>(emptyList())
    val constructors = mutableStateOf<List<ConstructorStanding>>(emptyList())

    // Real-time Schedule state
    val schedule = mutableStateOf<List<APIRace>>(emptyList())

    // State for the selected race to handle navigation to the detail screen
    val selectedRace = mutableStateOf<APIRace?>(null)
    val countdownText = mutableStateOf("")

    val scheduleListState = LazyListState()

    // --- NEW: COMPARISON STATES ---
    var selectedDriver1 by mutableStateOf<DriverStanding?>(null)
    var selectedDriver2 by mutableStateOf<DriverStanding?>(null)
    var comparisonMode by mutableStateOf(ComparisonMode.SEASON)
    var timeMode by mutableStateOf(TimeMode.MY_TIME)

    // 2. Setup Retrofit with Mirror URL and User-Agent
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
        // Load initial data for the default year (2025)
        fetchData()
    }

    // --- SELECTION & NAVIGATION HELPERS ---

    fun selectRace(race: APIRace) {
        selectedRace.value = race
    }

    fun toggleTimeMode() {
        timeMode = if (timeMode == TimeMode.MY_TIME) TimeMode.TRACK_TIME else TimeMode.MY_TIME
    }

    fun clearSelectedRace() {
        selectedRace.value = null
    }

    // Logic to select two distinct drivers for comparison
    fun selectDriverForComparison(driver: DriverStanding) {
        when {
            selectedDriver1 == null -> selectedDriver1 = driver
            selectedDriver2 == null && driver != selectedDriver1 -> selectedDriver2 = driver
            driver == selectedDriver1 -> selectedDriver1 = null // Deselect if tapped again
            driver == selectedDriver2 -> selectedDriver2 = null
            else -> {
                // Restart selection if a third different driver is picked
                selectedDriver1 = driver
                selectedDriver2 = null
            }
        }
    }

    fun clearComparison() {
        selectedDriver1 = null
        selectedDriver2 = null
    }

    // 3. Trigger new fetch when the season is changed via the dropdown
    fun updateYear(newYear: String) {
        selectedYear.value = newYear
        fetchData()
    }

    fun updateCountdown() {
        if (schedule.value.isEmpty()) {
            countdownText.value = "SCHEDULE NOT LOADED"
            return
        }

        try {
            val now = LocalDateTime.now()

            val nextRace = schedule.value.firstOrNull { race ->
                val raceDate = LocalDate.parse(race.date)
                val raceTime = LocalTime.parse(race.time?.replace("Z", "") ?: "15:00:00")
                val raceDateTime = raceDate.atTime(raceTime)
                raceDateTime.isAfter(now)
            }

            if (nextRace != null) {
                val raceDate = LocalDate.parse(nextRace.date)
                val raceTime = LocalTime.parse(nextRace.time?.replace("Z", "") ?: "15:00:00")
                val raceDateTime = raceDate.atTime(raceTime)

                val diff = Duration.between(now, raceDateTime)
                val days = diff.toDays()
                val hours = diff.toHours() % 24
                val minutes = diff.toMinutes() % 60

                countdownText.value = "${nextRace.raceName.uppercase()}: ${days}D ${hours}H ${minutes}M"
            } else {
                countdownText.value = "SEASON COMPLETED"
            }
        } catch (e: Exception) {
            countdownText.value = "TIMER UNAVAILABLE"
        }
    }

    fun getDriversForTeam(constructorId: String): List<DriverStanding> {
        return drivers.value.filter { standing ->
            standing.Constructors.lastOrNull()?.constructorId == constructorId
        }
    }

    // --- DATA FETCHING LOGIC ---

    private fun fetchSchedule(year: String) {
        viewModelScope.launch {
            try {
                val resultsResponse = apiService.getSeasonResults(year)
                val races = resultsResponse.MRData.RaceTable.Races

                if (races.isNotEmpty()) {
                    schedule.value = races
                } else {
                    val scheduleResponse = apiService.getSeasonSchedule(year)
                    schedule.value = scheduleResponse.MRData.RaceTable.Races
                }
                updateCountdown()
            } catch (e: Exception) {
                schedule.value = emptyList()
            }
        }
    }

    private fun fetchData() {
        val year = selectedYear.value
        fetchSchedule(year)

        viewModelScope.launch {
            try {
                val driverResponse = apiService.getDriverStandings(year)
                val dLists = driverResponse.MRData.StandingsTable.StandingsLists
                drivers.value = dLists.firstOrNull()?.DriverStandings ?: emptyList()

                val constructorResponse = apiService.getConstructorStandings(year)
                val cLists = constructorResponse.MRData.StandingsTable.StandingsLists
                constructors.value = cLists.firstOrNull()?.ConstructorStandings ?: emptyList()

            } catch (e: Exception) {
                drivers.value = emptyList()
                constructors.value = emptyList()
            }
        }
    }

    // Function to calculate career stats from a list of results
    fun calculateCareerStats(races: List<APIRace>): Map<String, Int> {
        val results = races.flatMap { it.Results ?: emptyList() }
        return mapOf(
            "Wins" to results.count { it.position == "1" },
            "Podiums" to results.count { it.position.toIntOrNull() ?: 10 <= 3 },
            "Entries" to races.size
        )
    }
}

// Global Enums for consistency
enum class ComparisonMode { SEASON, CAREER }