package com.example.grandprixhub

// Data class to hold the extra info not provided by the API
data class CircuitDetail(
    val laps: Int,
    val description: String,
    val imageRes: Int?
)

object CircuitRepository {
    // A Map is used for O(1) constant time lookup
    private val extraData = mapOf(
        "albert_park" to CircuitDetail(
            laps = 58,
            description = "Melbourne's Albert Park is a high-speed street circuit. It has hosted the Australian GP since 1996 and is known for its bumpy surface and close walls.",
            imageRes = R.drawable.albert_park
        ),
        "shanghai" to CircuitDetail(
            laps = 56,
            description = "Designed to look like the Chinese character 'shang', this track features one of the longest straights in F1 and a demanding 'snail' turn 1.",
            imageRes = R.drawable.shanghai_circuit
        ),
        "suzuka" to CircuitDetail(
            53,
            "A legendary driver favorite. The only 'figure-eight' track on the calendar, featuring the high-speed 130R corner.",
            R.drawable.suzuka
        ),
        
    )

    fun getDetails(circuitId: String): CircuitDetail {
        return extraData[circuitId] ?: CircuitDetail(
            laps = 50,
            description = "Circuit history and details for the 2026 season are currently being updated.",
            imageRes = null // Fallback for missing data
        )
    }
}