package nl.topo.app.data.quiz

import nl.topo.app.data.i18n.Txt

data class Province(val id: String, val name: Txt, val capital: String, val lat: Double, val lon: Double, val alt: List<String> = emptyList())

object NederlandData {
    /** Provincies; id = naam in de kaartdata. Ligging = plek voor het label. */
    val provinces = listOf(
        Province("Groningen", Txt("Groningen", "Groningen"), "Groningen", 53.27, 6.78),
        Province("Fryslân", Txt("Friesland", "Fryslân"), "Leeuwarden", 53.03, 5.78, listOf("friesland", "fryslan")),
        Province("Drenthe", Txt("Drenthe", "Drenthe"), "Assen", 52.86, 6.55),
        Province("Overijssel", Txt("Overijssel", "Overijssel"), "Zwolle", 52.42, 6.45),
        Province("Flevoland", Txt("Flevoland", "Flevoland"), "Lelystad", 52.58, 5.62),
        Province("Gelderland", Txt("Gelderland", "Gelderland"), "Arnhem", 52.13, 5.9),
        Province("Utrecht", Txt("Utrecht", "Utrecht"), "Utrecht", 52.04, 5.25),
        Province("Noord-Holland", Txt("North Holland", "Noord-Holland"), "Haarlem", 52.62, 4.83, listOf("noord holland")),
        Province("Zuid-Holland", Txt("South Holland", "Zuid-Holland"), "Den Haag", 51.97, 4.6, listOf("zuid holland")),
        Province("Zeeland", Txt("Zeeland", "Zeeland"), "Middelburg", 51.58, 3.72),
        Province("Noord-Brabant", Txt("North Brabant", "Noord-Brabant"), "'s-Hertogenbosch", 51.62, 5.05, listOf("brabant")),
        Province("Limburg", Txt("Limburg", "Limburg"), "Maastricht", 51.12, 5.83),
    )

    /** Andere goede namen voor steden bij typen. */
    val cityAlt = mapOf(
        "'s-Hertogenbosch" to listOf("den bosch", "s-hertogenbosch", "hertogenbosch"),
        "Den Haag" to listOf("the hague", "'s-gravenhage", "s-gravenhage"),
        "Zaandam" to listOf("zaanstad"),
    )

    private fun city(n: String, lat: Double, lon: Double, prov: String, en: String = n) = Place(n, Txt(en, n), lat, lon, Txt(prov, prov), cityAlt[n].orEmpty())

    /** 50 steden, grootste eerst. Toelichting = provincie. */
    val cities = listOf(
        city("Amsterdam", 52.3676, 4.9041, "Noord-Holland"), city("Rotterdam", 51.9244, 4.4777, "Zuid-Holland"),
        city("Den Haag", 52.0705, 4.3007, "Zuid-Holland", "The Hague"), city("Utrecht", 52.0907, 5.1214, "Utrecht"),
        city("Eindhoven", 51.4416, 5.4697, "Noord-Brabant"), city("Groningen", 53.2194, 6.5665, "Groningen"),
        city("Tilburg", 51.5555, 5.0913, "Noord-Brabant"), city("Almere", 52.3508, 5.2647, "Flevoland"),
        city("Breda", 51.5719, 4.7683, "Noord-Brabant"), city("Nijmegen", 51.8126, 5.8372, "Gelderland"),
        city("Arnhem", 51.9851, 5.8987, "Gelderland"), city("Haarlem", 52.3874, 4.6462, "Noord-Holland"),
        city("Enschede", 52.2215, 6.8937, "Overijssel"), city("Apeldoorn", 52.2112, 5.9699, "Gelderland"),
        city("Amersfoort", 52.1561, 5.3878, "Utrecht"), city("Zaandam", 52.4389, 4.8258, "Noord-Holland"),
        city("'s-Hertogenbosch", 51.6978, 5.3037, "Noord-Brabant"), city("Zwolle", 52.5168, 6.083, "Overijssel"),
        city("Leiden", 52.1601, 4.497, "Zuid-Holland"), city("Maastricht", 50.8514, 5.691, "Limburg"),
        city("Dordrecht", 51.8133, 4.6901, "Zuid-Holland"), city("Ede", 52.0402, 5.6649, "Gelderland"),
        city("Leeuwarden", 53.2012, 5.7999, "Fryslân"), city("Alkmaar", 52.6324, 4.7534, "Noord-Holland"),
        city("Emmen", 52.7792, 6.9006, "Drenthe"), city("Deventer", 52.2661, 6.1552, "Overijssel"),
        city("Delft", 52.0116, 4.3571, "Zuid-Holland"), city("Venlo", 51.3704, 6.1724, "Limburg"),
        city("Heerlen", 50.8882, 5.9795, "Limburg"), city("Helmond", 51.4793, 5.657, "Noord-Brabant"),
        city("Lelystad", 52.5185, 5.4714, "Flevoland"), city("Assen", 52.9925, 6.5649, "Drenthe"),
        city("Middelburg", 51.4988, 3.6136, "Zeeland"), city("Vlissingen", 51.4426, 3.5736, "Zeeland"),
        city("Den Helder", 52.9563, 4.76, "Noord-Holland"), city("Hoorn", 52.6425, 5.0597, "Noord-Holland"),
        city("Roermond", 51.1942, 5.987, "Limburg"), city("Hilversum", 52.2292, 5.1669, "Noord-Holland"),
        city("Gouda", 52.0115, 4.7105, "Zuid-Holland"), city("Hengelo", 52.2659, 6.7931, "Overijssel"),
        city("Terneuzen", 51.3355, 3.8276, "Zeeland"), city("Harderwijk", 52.3423, 5.6206, "Gelderland"),
        city("Sneek", 53.0322, 5.6597, "Fryslân"), city("Bergen op Zoom", 51.4946, 4.2878, "Noord-Brabant"),
        city("Oss", 51.765, 5.518, "Noord-Brabant"), city("Doetinchem", 51.9654, 6.2886, "Gelderland"),
        city("Zutphen", 52.1386, 6.2014, "Gelderland"), city("Meppel", 52.6956, 6.1944, "Drenthe"),
        city("Drachten", 53.1089, 6.0997, "Fryslân"), city("Kampen", 52.555, 5.9114, "Overijssel"),
    )

    /** Waddeneilanden; id = naam in de kaartdata. */
    val wadden = listOf(
        Place("Texel", Txt("Texel", "Texel"), 53.06, 4.62, Txt("North Holland", "Noord-Holland")),
        Place("Vlieland", Txt("Vlieland", "Vlieland"), 53.30, 5.02, Txt("Friesland", "Fryslân")),
        Place("Terschelling", Txt("Terschelling", "Terschelling"), 53.40, 5.33, Txt("Friesland", "Fryslân")),
        Place("Ameland", Txt("Ameland", "Ameland"), 53.45, 5.75, Txt("Friesland", "Fryslân")),
        Place("Schiermonnikoog", Txt("Schiermonnikoog", "Schiermonnikoog"), 53.48, 6.17, Txt("Friesland", "Fryslân")),
    )

    /** Wateren, als punt op een kenmerkende plek. */
    val waters = listOf(
        Place("Noordzee", Txt("North Sea", "Noordzee"), 52.75, 4.25, Txt("Sea", "Zee")),
        Place("Waddenzee", Txt("Wadden Sea", "Waddenzee"), 53.22, 5.25, Txt("Sea", "Zee")),
        Place("IJsselmeer", Txt("IJsselmeer", "IJsselmeer"), 52.85, 5.3, Txt("Lake", "Meer"), listOf("ijsselmeer", "ysselmeer")),
        Place("Markermeer", Txt("Markermeer", "Markermeer"), 52.53, 5.17, Txt("Lake", "Meer")),
        Place("Rijn", Txt("Rhine", "Rijn"), 51.86, 6.08, Txt("River (enters the Netherlands at Lobith)", "Rivier (komt binnen bij Lobith)"), listOf("rhine", "rhein")),
        Place("Waal", Txt("Waal", "Waal"), 51.86, 5.55, Txt("River", "Rivier")),
        Place("Maas", Txt("Meuse", "Maas"), 51.73, 5.35, Txt("River", "Rivier"), listOf("meuse")),
        Place("IJssel", Txt("IJssel", "IJssel"), 52.33, 6.12, Txt("River", "Rivier"), listOf("ijssel", "yssel")),
        Place("Nederrijn", Txt("Lower Rhine", "Nederrijn"), 51.97, 5.7, Txt("River, becomes the Lek", "Rivier, gaat over in de Lek"), listOf("lower rhine")),
        Place("Lek", Txt("Lek", "Lek"), 51.95, 5.0, Txt("River", "Rivier")),
        Place("Oosterschelde", Txt("Eastern Scheldt", "Oosterschelde"), 51.6, 3.95, Txt("Estuary", "Zeearm"), listOf("eastern scheldt")),
        Place("Westerschelde", Txt("Western Scheldt", "Westerschelde"), 51.42, 3.78, Txt("Estuary", "Zeearm"), listOf("western scheldt")),
        Place("Haringvliet", Txt("Haringvliet", "Haringvliet"), 51.78, 4.2, Txt("Estuary", "Zeearm")),
        Place("Veluwemeer", Txt("Veluwemeer", "Veluwemeer"), 52.39, 5.7, Txt("Lake", "Meer")),
        Place("Lauwersmeer", Txt("Lauwersmeer", "Lauwersmeer"), 53.37, 6.22, Txt("Lake", "Meer")),
        Place("Nieuwe Waterweg", Txt("Nieuwe Waterweg", "Nieuwe Waterweg"), 51.94, 4.17, Txt("Canal to the sea", "Kanaal naar zee")),
    )
}
