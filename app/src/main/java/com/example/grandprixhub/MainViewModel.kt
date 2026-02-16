package com.example.grandprixhub

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainViewModel : ViewModel() {
    // 1. UI State: Track active tab, selected year, and data lists
    val isDriversTab = mutableStateOf(true)
    val selectedYear = mutableStateOf("2025")

    val drivers = mutableStateOf<List<DriverStanding>>(emptyList())
    val constructors = mutableStateOf<List<ConstructorStanding>>(emptyList())

    // NEW: Real-time Schedule state
    val schedule = mutableStateOf<List<APIRace>>(emptyList())

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

    // 3. Trigger new fetch when the season is changed via the dropdown
    fun updateYear(newYear: String) {
        selectedYear.value = newYear
        fetchData()
    }

    // 4. Helper function to filter drivers by team ID
    fun getDriversForTeam(constructorId: String): List<DriverStanding> {
        return drivers.value.filter { standing ->
            standing.Constructors.lastOrNull()?.constructorId == constructorId
        }
    }

    // NEW: Fetch Schedule for the current selected year
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