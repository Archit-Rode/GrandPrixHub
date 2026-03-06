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

    // Real-time Schedule state (now holds full race results for podium calculation)
    val schedule = mutableStateOf<List<APIRace>>(emptyList())

    // State for navigation and countdown
    val selectedRace = mutableStateOf<APIRace?>(null)
    val countdownText = mutableStateOf("")
    val scheduleListState = LazyListState()

    // --- SIMPLIFIED COMPARISON STATES (Season Only) ---
    var selectedDriver1 by mutableStateOf<DriverStanding?>(null)
    var selectedDriver2 by mutableStateOf<DriverStanding?>(null)
    var timeMode by mutableStateOf(TimeMode.MY_TIME)

    // 2. Setup Retrofit
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

    // --- SELECTION & NAVIGATION HELPERS ---

    fun selectRace(race: APIRace) { selectedRace.value = race }
    fun toggleTimeMode() { timeMode = if (timeMode == TimeMode.MY_TIME) TimeMode.TRACK_TIME else TimeMode.MY_TIME }
    fun clearSelectedRace() { selectedRace.value = null }

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

    // --- DATA FETCHING ---

    fun updateYear(newYear: String) {
        selectedYear.value = newYear
        fetchData()
    }

    private fun fetchSchedule(year: String) {
        viewModelScope.launch {
            try {
                // Fetch full results to ensure we have P1, P2, and P3 data
                val resultsResponse = apiService.getFullSeasonResults(year)
                val races = resultsResponse.MRData.RaceTable.Races

                if (races.isNotEmpty()) {
                    schedule.value = races
                } else {
                    // Fallback to basic calendar if no results exist yet (e.g., 2026)
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

    fun updateCountdown() {
        if (schedule.value.isEmpty()) return
        try {
            val now = LocalDateTime.now()
            val nextRace = schedule.value.firstOrNull { race ->
                val raceDateTime = LocalDate.parse(race.date).atTime(LocalTime.parse(race.time?.replace("Z", "") ?: "15:00:00"))
                raceDateTime.isAfter(now)
            }

            if (nextRace != null) {
                val raceDateTime = LocalDate.parse(nextRace.date).atTime(LocalTime.parse(nextRace.time?.replace("Z", "") ?: "15:00:00"))
                val diff = Duration.between(now, raceDateTime)
                countdownText.value = "${nextRace.raceName.uppercase()}: ${diff.toDays()}D ${diff.toHours() % 24}H ${diff.toMinutes() % 60}M"
            } else {
                countdownText.value = "SEASON COMPLETED"
            }
        } catch (e: Exception) {
            countdownText.value = "TIMER UNAVAILABLE"
        }
    }

    fun getDriversForTeam(constructorId: String): List<DriverStanding> {
        return drivers.value.filter { it.Constructors.lastOrNull()?.constructorId == constructorId }
    }
}