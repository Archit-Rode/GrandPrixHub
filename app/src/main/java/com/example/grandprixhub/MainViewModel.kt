package com.example.grandprixhub

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainViewModel : ViewModel() {
    // 1. UI State: Track active tab and selected year
    val isDriversTab = mutableStateOf(true)
    val selectedYear = mutableStateOf("2025") // Default season

    // Data lists for the UI to observe
    val drivers = mutableStateOf<List<DriverStanding>>(emptyList())
    // CHANGED: Now storing ConstructorStanding to keep points and rank data
    val constructors = mutableStateOf<List<ConstructorStanding>>(emptyList())

    // 2. Setup Retrofit with a Mirror URL and User-Agent
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

    // 3. Trigger new fetch when the season is changed via the dropdown
    fun updateYear(newYear: String) {
        selectedYear.value = newYear
        fetchData()
    }

    // 4. Helper function to filter drivers by team ID
    fun getDriversForTeam(constructorId: String): List<DriverStanding> {
        return drivers.value.filter { standing ->
            // Uses lastOrNull to handle mid-season team swaps
            standing.Constructors.lastOrNull()?.constructorId == constructorId
        }
    }

    private fun fetchData() {
        viewModelScope.launch {
            try {
                val year = selectedYear.value // Get current selected year

                // Fetch Driver Standings for the selected year
                val driverResponse = apiService.getDriverStandings(year)
                val dLists = driverResponse.MRData.StandingsTable.StandingsLists

                if (dLists.isNotEmpty()) {
                    drivers.value = dLists[0].DriverStandings
                    println("F1DEBUG: Found ${drivers.value.size} drivers for $year!")
                } else {
                    drivers.value = emptyList() // Clear list if no data
                }

                // Fetch Constructor Standings for the selected year
                val constructorResponse = apiService.getConstructorStandings(year)
                val cLists = constructorResponse.MRData.StandingsTable.StandingsLists

                if (cLists.isNotEmpty()) {
                    // CHANGED: We now save the full standings list instead of mapping to .Constructor
                    constructors.value = cLists[0].ConstructorStandings
                    println("F1DEBUG: Found ${constructors.value.size} team standings for $year!")
                } else {
                    constructors.value = emptyList() // Clear list if no data
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