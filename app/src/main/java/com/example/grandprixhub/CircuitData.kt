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
        "hungaroring" to CircuitDetail(
            70,
            "Often called 'Monaco without walls.' A twisty, technical track where heat and tire management are the biggest challenges.",
            R.drawable.hungaroring
        ),
        "zandvoort" to CircuitDetail(
            72,
            "A rollercoaster by the North Sea. Famous for its steep 18-degree banked corners and incredible atmosphere.",
            R.drawable.zandvoort
        ),
        "monza" to CircuitDetail(
            53,
            "The Temple of Speed. The fastest track on the calendar, where cars run minimum downforce to fly through the Royal Park.",
            R.drawable.monza
        ),
        "madring" to CircuitDetail(
            54,
            "The 2026 debutant. A new street-style circuit around the IFEMA exhibition center, marking a new era for the Spanish GP.",
            R.drawable.madrid
        ),
        "baku" to CircuitDetail(
            51,
            "City of Winds. Features a massive 2km main straight and a tight section passing the historic Old City castle walls.",
            R.drawable.baku
        ),
        "marina_bay" to CircuitDetail(
            62,
            "F1's original night race. A grueling street circuit where high humidity and 19 corners test the drivers' physical limits.",
            R.drawable.marina_bay
        ),
        "americas" to CircuitDetail(
            56,
            "COTA is a modern classic. It draws inspiration from great European tracks and features a steep climb into Turn 1.",
            R.drawable.americas
        ),
        "rodriguez" to CircuitDetail(
            71,
            "High altitude racing. Located 2,200m above sea level, thin air makes the cars extremely fast on straights but low on downforce.",
            R.drawable.rodriguez
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