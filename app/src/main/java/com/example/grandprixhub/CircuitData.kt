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
        "bahrain" to CircuitDetail(
            57,
            "A desert oasis. The Bahrain International Circuit is famous for its night racing and heavy braking zones that encourage overtaking.",
            R.drawable.sakhir
        ),
        "jeddah" to CircuitDetail(
            50,
            "The world's fastest street circuit. Hugging the Red Sea coast, it features 27 high-speed corners and walls that leave no room for error.",
            R.drawable.jeddah
        ),
        "miami" to CircuitDetail(
            57,
            "A temporary circuit around the Hard Rock Stadium. It blends a street-circuit feel with a permanent track's high-speed sweeps.",
            R.drawable.miami
        ),
        "villeneuve" to CircuitDetail(
            70,
            "Held on a man-made island, this track is famous for the 'Wall of Champions' and its close proximity to the St. Lawrence River.",
            R.drawable.montreal
        ),
        "monaco" to CircuitDetail(
            78,
            "The crown jewel of F1. A tight, narrow street circuit through Monte Carlo where precision is everything and overtaking is nearly impossible.",
            R.drawable.monaco
        ),
        "catalunya" to CircuitDetail(
            66,
            "A classic testing venue. Its mix of high and low-speed corners makes it the perfect benchmark for a car's aerodynamic efficiency.",
            R.drawable.catalunya
        ),
        "red_bull_ring" to CircuitDetail(
            71,
            "Short and punchy. Set in the Styrian mountains, it features three long straights and intense elevation changes.",
            R.drawable.red_bull_ring
        ),
        "silverstone" to CircuitDetail(
            52,
            "The home of British motor racing. A former airfield known for legendary corner complexes like Maggotts, Becketts, and Chapel.",
            R.drawable.silverstone
        ),
        "spa" to CircuitDetail(
            44,
            "The longest track on the calendar. Features the iconic Eau Rouge/Raidillon climb and unpredictable Ardennes forest weather.",
            R.drawable.spa
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