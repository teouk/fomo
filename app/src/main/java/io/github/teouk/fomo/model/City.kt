package io.github.teouk.fomo.model

/**
 * Places the user can choose on Discover ("Events near Swansea ▾").
 * A fixed list keeps the choice offline and needs no geocoding service.
 */
enum class City(val displayName: String, val latitude: Double, val longitude: Double) {
    SWANSEA("Swansea", 51.6214, -3.9436),
    CARDIFF("Cardiff", 51.4816, -3.1791),
    BRISTOL("Bristol", 51.4545, -2.5879),
    LONDON("London", 51.5072, -0.1276),
    BIRMINGHAM("Birmingham", 52.4862, -1.8904),
    MANCHESTER("Manchester", 53.4808, -2.2426),
    LIVERPOOL("Liverpool", 53.4084, -2.9916),
    LEEDS("Leeds", 53.8008, -1.5491),
    GLASGOW("Glasgow", 55.8642, -4.2518),
    EDINBURGH("Edinburgh", 55.9533, -3.1883),
    ;

    companion object {
        val DEFAULT = SWANSEA
    }
}
