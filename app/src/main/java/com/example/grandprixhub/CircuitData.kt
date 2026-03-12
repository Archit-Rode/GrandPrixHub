package com.example.grandprixhub

// Data class to hold the extra info not provided by the API
data class CircuitDetail(
    val laps: Int,
    val description: String,
    val imageRes: Int?,
    val hotspots: List<CircuitHotspot> = emptyList()
)

object CircuitRepository {
    // A Map is used for O(1) constant time lookup
    private val extraData = mapOf(
        "albert_park" to CircuitDetail(
            laps = 58,
            description = "Melbourne's Albert Park is a high-speed street circuit. It has hosted the Australian GP since 1996 and is known for its bumpy surface and close walls.",
            imageRes = R.drawable.albert_park,
            hotspots = listOf(
                CircuitHotspot("Jones", "Turn 1: High-speed braking zone.", 0.3f, 0.6f),
                CircuitHotspot("Lakeside", "A flat-out blast along the water.", 0.25f, 0.65f)
            )
        ),
        "shanghai" to CircuitDetail(
            laps = 56,
            description = "Designed to look like the Chinese character 'shang', this track features one of the longest straights in F1 and a demanding 'snail' turn 1.",
            imageRes = R.drawable.shanghai_circuit,
            hotspots = listOf(
                CircuitHotspot("Turn 1-3", "The tightening snail-shell entry.", 0.72f, 0.28f),
                CircuitHotspot("Back Straight", "One of the longest straights in F1.", 0.45f, 0.85f)
            )
        ),
        "suzuka" to CircuitDetail(
            53,
            "A legendary driver favorite. The only 'figure-eight' track on the calendar, featuring the high-speed 130R corner.",
            R.drawable.suzuka,
            listOf(
                CircuitHotspot("130R", "A legendary high-speed left-hander.", 0.15f, 0.42f),
                CircuitHotspot("The S-Curves", "Testing the car's aerodynamic balance.", 0.68f, 0.22f)
            )
        ),
        "bahrain" to CircuitDetail(
            57,
            "A desert oasis. The Bahrain International Circuit is famous for its night racing and heavy braking zones that encourage overtaking.",
            R.drawable.sakhir,
            listOf(
                CircuitHotspot("Turn 1", "Michael Schumacher turn - prime overtaking.", 0.52f, 0.08f),
                CircuitHotspot("Turn 10", "Technical downhill left-hander.", 0.35f, 0.55f)
            )
        ),
        "jeddah" to CircuitDetail(
            laps = 50,
            description = "The world's fastest street circuit. Hugging the Red Sea coast, it features 27 high-speed corners and walls that leave no room for error.",
            imageRes = R.drawable.jeddah,
            hotspots = listOf(
                CircuitHotspot("Turn 1", "Tight chicane. Start of the lap.", 0.6f, 0.45f),
                CircuitHotspot("Turn 13", "Banked hairpin.", 0.05f, 0.7f),
                CircuitHotspot("Turn 27", "Final corner before DRS.", 0.9f, 0.2f)
            )
        ),
        "miami" to CircuitDetail(
            57,
            "A temporary circuit around the Hard Rock Stadium. It blends a street-circuit feel with a permanent track's high-speed sweeps.",
            R.drawable.miami,
            listOf(CircuitHotspot("Turn 11-16", "The tight and technical 'Misty' section.", 0.42f, 0.72f))
        ),
        "villeneuve" to CircuitDetail(
            70,
            "Held on a man-made island, this track is famous for the 'Wall of Champions' and its close proximity to the St. Lawrence River.",
            R.drawable.montreal,
            listOf(CircuitHotspot("Wall of Champions", "The famous final chicane.", 0.9f, 0.15f))
        ),
        "monaco" to CircuitDetail(
            laps = 78,
            description = "The crown jewel of F1. A tight, narrow street circuit through Monte Carlo where precision is everything and overtaking is nearly impossible.",
            imageRes = R.drawable.monaco,
            hotspots = listOf(
                CircuitHotspot("Sainte-Dévote", "Famous Turn 1 bottleneck.", 0.3f, 0.8f),
                CircuitHotspot("Grand Hotel Hairpin", "Slowest turn in F1.", 0.6f, 0.3f)
            )
        ),
        "catalunya" to CircuitDetail(
            66,
            "A classic testing venue. Its mix of high and low-speed corners makes it the perfect benchmark for a car's aerodynamic efficiency.",
            R.drawable.catalunya,
            listOf(CircuitHotspot("Turn 3", "Long, high-G right-hander.", 0.8f, 0.4f))
        ),
        "red_bull_ring" to CircuitDetail(
            71,
            "Short and punchy. Set in the Styrian mountains, it features three long straights and intense elevation changes.",
            R.drawable.red_bull_ring,
            listOf(CircuitHotspot("Turn 1", "Steep uphill braking zone.", 0.5f, 0.05f))
        ),
        "silverstone" to CircuitDetail(
            52,
            "The home of British motor racing. A former airfield known for legendary corner complexes like Maggotts, Becketts, and Chapel.",
            R.drawable.silverstone,
            listOf(
                CircuitHotspot("Maggots & Becketts", "The fastest sequence on the calendar.", 0.45f, 0.25f),
                CircuitHotspot("Copse", "Flat-out at nearly 300km/h.", 0.62f, 0.12f)
            )
        ),
        "spa" to CircuitDetail(
            44,
            "The longest track on the calendar. Features the iconic Eau Rouge/Raidillon climb and unpredictable Ardennes forest weather.",
            R.drawable.spa,
            listOf(CircuitHotspot("Eau Rouge", "The legendary uphill 'flick'.", 0.38f, 0.85f))
        ),
        "hungaroring" to CircuitDetail(
            70,
            "Often called 'Monaco without walls.' A twisty, technical track where heat and tire management are the biggest challenges.",
            R.drawable.hungaroring,
            listOf(CircuitHotspot("Turn 1", "The primary overtaking spot.", 0.5f, 0.1f))
        ),
        "zandvoort" to CircuitDetail(
            72,
            "A rollercoaster by the North Sea. Famous for its steep 18-degree banked corners and incredible atmosphere.",
            R.drawable.zandvoort,
            listOf(CircuitHotspot("Hugenholtz", "Steep 18-degree banked corner.", 0.35f, 0.25f))
        ),
        "monza" to CircuitDetail(
            53,
            "The Temple of Speed. The fastest track on the calendar, where cars run minimum downforce to fly through the Royal Park.",
            R.drawable.monza,
            listOf(CircuitHotspot("Parabolica", "The long final high-speed turn.", 0.5f, 0.95f))
        ),
        "madring" to CircuitDetail(
            54,
            "The 2026 debutant. A new street-style circuit around the IFEMA exhibition center, marking a new era for the Spanish GP.",
            R.drawable.madrid,
            listOf(CircuitHotspot("The Loop", "Technical section weaving through the city.", 0.5f, 0.5f))
        ),
        "baku" to CircuitDetail(
            51,
            "City of Winds. Features a massive 2km main straight and a tight section passing the historic Old City castle walls.",
            R.drawable.baku,
            listOf(CircuitHotspot("Castle Section", "Only 7.6m wide.", 0.45f, 0.45f))
        ),
        "marina_bay" to CircuitDetail(
            62,
            "F1's original night race. A grueling street circuit where high humidity and 19 corners test the drivers' physical limits.",
            R.drawable.marina_bay,
            listOf(CircuitHotspot("The Bridge", "Iconic crossing under the floodlights.", 0.2f, 0.5f))
        ),
        "americas" to CircuitDetail(
            56,
            "COTA is a modern classic. It draws inspiration from great European tracks and features a steep climb into Turn 1.",
            R.drawable.americas,
            listOf(CircuitHotspot("Turn 1", "Blind uphill left-hand apex.", 0.45f, 0.15f))
        ),
        "rodriguez" to CircuitDetail(
            71,
            "High altitude racing. Located 2,200m above sea level, thin air makes the cars extremely fast on straights but low on downforce.",
            R.drawable.rodriguez,
            listOf(CircuitHotspot("Foro Sol", "The stadium section finish.", 0.85f, 0.85f))
        ),
        "interlagos" to CircuitDetail(
            71,
            "An anti-clockwise classic in São Paulo. Known for its passionate fans and the famous 'Senna S' sequence.",
            R.drawable.interlagos,
            listOf(CircuitHotspot("Senna S", "The tricky Turn 1-2 sequence.", 0.35f, 0.15f))
        ),
        "vegas" to CircuitDetail(
            50,
            "Racing down the Strip. A high-speed street circuit featuring a 1.9km blast past the iconic casinos of Las Vegas.",
            R.drawable.vegas,
            listOf(CircuitHotspot("The Strip", "1.9km flat-out straight.", 0.5f, 0.8f))
        ),
        "losail" to CircuitDetail(
            57,
            "A fast, flowing layout in Qatar. Originally a MotoGP track, it requires high-speed commitment through its long, sweeping turns.",
            R.drawable.losail,
            listOf(CircuitHotspot("Triple Right", "Turns 12, 13, and 14 taken as one.", 0.75f, 0.6f))
        ),
        "yas_marina" to CircuitDetail(
            58,
            "The sunset finale. A spectacular facility in Abu Dhabi where the race starts in daylight and ends under thousands of floodlights.",
            R.drawable.yas_marina,
            listOf(CircuitHotspot("Hotel Bridge", "Racing under the illuminated hotel.", 0.6f, 0.75f))
        )
    )

    fun getDetails(circuitId: String): CircuitDetail {
        return extraData[circuitId] ?: CircuitDetail(
            laps = 50,
            description = "Circuit history and details for the 2026 season are currently being updated.",
            imageRes = null // Fallback for missing data
        )
    }
}