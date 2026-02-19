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

class MainViewModel : ViewModel() {
    // 1. UI State: Track active tab, selected year, and data lists
    val isDriversTab = mutableStateOf(true)
    val selectedYear = mutableStateOf("2025")

    val drivers = mutableStateOf<List<DriverStanding>>(emptyList())
    val constructors = mutableStateOf<List<ConstructorStanding>>(emptyList())

    // Real-time Schedule state
    val schedule = mutableStateOf<List<APIRace>>(emptyList())

    // NEW: State for the selected race to handle navigation to the detail screen
    val selectedRace = mutableStateOf<APIRace?>(null)
    val countdownText = mutableStateOf("")

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

    // NEW: Helper functions for race selection navigation
    fun selectRace(race: APIRace) {
        selectedRace.value = race
    }

    fun clearSelectedRace() {
        selectedRace.value = null
    }

    // 3. Trigger new fetch when the season is changed via the dropdown
    fun updateYear(newYear: String) {
        selectedYear.value = newYear
        fetchData()
    }
    fun updateCountdown() {
        // 1. Early exit if schedule is empty to prevent crashes
        if (schedule.value.isEmpty()) {
            countdownText.value = "SCHEDULE NOT LOADED"
            return
        }

        try {
            val now = java.time.LocalDateTime.now()

            // 2. Single lookup for the next upcoming race
            val nextRace = schedule.value.firstOrNull { race ->
                // Parse date and handle missing/formatted time (e.g., removing 'Z' from UTC)
                val raceDate = java.time.LocalDate.parse(race.date)
                val raceTime = java.time.LocalTime.parse(race.time?.replace("Z", "") ?: "15:00:00")
                val raceDateTime = raceDate.atTime(raceTime)

                raceDateTime.isAfter(now)
            }

            // 3. Calculate and format the countdown string
            if (nextRace != null) {
                val raceDate = java.time.LocalDate.parse(nextRace.date)
                val raceTime = java.time.LocalTime.parse(nextRace.time?.replace("Z", "") ?: "15:00:00")
                val raceDateTime = raceDate.atTime(raceTime)

                val diff = java.time.Duration.between(now, raceDateTime)

                val days = diff.toDays()
                val hours = diff.toHours() % 24
                val minutes = diff.toMinutes() % 60

                countdownText.value = "${nextRace.raceName.uppercase()}: ${days}D ${hours}H ${minutes}M"
            } else {
                countdownText.value = "SEASON COMPLETED"
            }
        } catch (e: Exception) {
            // 4. Graceful error handling for parsing failures
            countdownText.value = "TIMER UNAVAILABLE"
            android.util.Log.e("F1DEBUG", "Countdown Error: ${e.message}")
        }
    }
    // 4. Helper function to filter drivers by team ID
    fun getDriversForTeam(constructorId: String): List<DriverStanding> {
        return drivers.value.filter { standing ->
            standing.Constructors.lastOrNull()?.constructorId == constructorId
        }
    }

    // Fetch Schedule for the current selected year
    private fun fetchSchedule(year: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getSeasonSchedule(year)
                schedule.value = response.MRData.RaceTable.Races
                println("F1DEBUG: Found ${schedule.value.size} races for $year!")
            } catch (e: Exception) {
                println("F1DEBUG SCHEDULE ERROR: ${e.message}")
                schedule.value = emptyList()
            }
        }
    }

    private fun fetchData() {
        val year = selectedYear.value

        // Always fetch schedule along with standings
        fetchSchedule(year)

        viewModelScope.launch {
            try {
                // Fetch Driver Standings
                val driverResponse = apiService.getDriverStandings(year)
                val dLists = driverResponse.MRData.StandingsTable.StandingsLists

                if (dLists.isNotEmpty()) {
                    drivers.value = dLists[0].DriverStandings
                } else {
                    drivers.value = emptyList()
                }

                // Fetch Constructor Standings
                val constructorResponse = apiService.getConstructorStandings(year)
                val cLists = constructorResponse.MRData.StandingsTable.StandingsLists

                if (cLists.isNotEmpty()) {
                    constructors.value = cLists[0].ConstructorStandings
                } else {
                    constructors.value = emptyList()
                }

            } catch (e: Exception) {
                println("F1DEBUG ERROR: ${e.message}")
                e.printStackTrace()
                drivers.value = emptyList()
                constructors.value = emptyList()
            }
        }
    }
}