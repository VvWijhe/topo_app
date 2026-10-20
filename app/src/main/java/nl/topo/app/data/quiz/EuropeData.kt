package nl.topo.app.data.quiz

import nl.topo.app.data.i18n.Txt

/** Een plek op de kaart: naam, ligging en een korte toelichting. [alt]: andere goede namen bij typen. */
data class Place(val id: String, val name: Txt, val lat: Double, val lon: Double, val note: Txt? = null, val alt: List<String> = emptyList())

private fun place(nl: String, en: String, lat: Double, lon: Double, noteEn: String? = null, noteNl: String? = null, vararg alt: String) =
    Place(nl, Txt(en, nl), lat, lon, if (noteEn != null && noteNl != null) Txt(noteEn, noteNl) else null, alt.toList())

/** Feiten over Europese landen die niet in de wereldkaartdata zitten. */
object EuropeData {
    private val language = mapOf(
        "IJslands" to "Icelandic", "Noors" to "Norwegian", "Zweeds" to "Swedish", "Fins" to "Finnish", "Deens" to "Danish",
        "Estisch" to "Estonian", "Lets" to "Latvian", "Litouws" to "Lithuanian", "Iers" to "Irish", "Engels" to "English",
        "Nederlands" to "Dutch", "Fries" to "Frisian", "Frans" to "French", "Duits" to "German", "Luxemburgs" to "Luxembourgish",
        "Pools" to "Polish", "Tsjechisch" to "Czech", "Slowaaks" to "Slovak", "Italiaans" to "Italian", "Reto-Romaans" to "Romansh",
        "Catalaans" to "Catalan", "Spaans" to "Spanish", "Portugees" to "Portuguese", "Latijn" to "Latin", "Maltees" to "Maltese",
        "Sloveens" to "Slovene", "Kroatisch" to "Croatian", "Bosnisch" to "Bosnian", "Servisch" to "Serbian",
        "Montenegrijns" to "Montenegrin", "Albanees" to "Albanian", "Macedonisch" to "Macedonian", "Grieks" to "Greek",
        "Bulgaars" to "Bulgarian", "Roemeens" to "Romanian", "Oekraïens" to "Ukrainian", "Belarussisch" to "Belarusian",
        "Russisch" to "Russian", "Hongaars" to "Hungarian", "Turks" to "Turkish", "Georgisch" to "Georgian",
        "Armeens" to "Armenian", "Azerbeidzjaans" to "Azerbaijani",
    )
    private fun l(vararg nl: String) = nl.map { Txt(language[it] ?: it, it) }

    /** Officiële talen per land. */
    val languages: Map<String, List<Txt>> = mapOf(
        "ISL" to l("IJslands"), "NOR" to l("Noors"), "SWE" to l("Zweeds"), "FIN" to l("Fins", "Zweeds"), "DNK" to l("Deens"),
        "EST" to l("Estisch"), "LVA" to l("Lets"), "LTU" to l("Litouws"), "IRL" to l("Iers", "Engels"), "GBR" to l("Engels"),
        "NLD" to l("Nederlands", "Fries"), "BEL" to l("Nederlands", "Frans", "Duits"), "LUX" to l("Luxemburgs", "Frans", "Duits"),
        "DEU" to l("Duits"), "POL" to l("Pools"), "CZE" to l("Tsjechisch"), "SVK" to l("Slowaaks"), "AUT" to l("Duits"),
        "CHE" to l("Duits", "Frans", "Italiaans", "Reto-Romaans"), "LIE" to l("Duits"), "FRA" to l("Frans"), "MCO" to l("Frans"),
        "AND" to l("Catalaans"), "ESP" to l("Spaans"), "PRT" to l("Portugees"), "ITA" to l("Italiaans"), "SMR" to l("Italiaans"),
        "VAT" to l("Italiaans", "Latijn"), "MLT" to l("Maltees", "Engels"), "SVN" to l("Sloveens"), "HRV" to l("Kroatisch"),
        "BIH" to l("Bosnisch", "Kroatisch", "Servisch"), "SRB" to l("Servisch"), "MNE" to l("Montenegrijns"), "KOS" to l("Albanees", "Servisch"),
        "ALB" to l("Albanees"), "MKD" to l("Macedonisch", "Albanees"), "GRC" to l("Grieks"), "BGR" to l("Bulgaars"), "ROU" to l("Roemeens"),
        "MDA" to l("Roemeens"), "UKR" to l("Oekraïens"), "BLR" to l("Belarussisch", "Russisch"), "RUS" to l("Russisch"), "HUN" to l("Hongaars"),
        "CYP" to l("Grieks", "Turks"), "TUR" to l("Turks"), "GEO" to l("Georgisch"), "ARM" to l("Armeens"), "AZE" to l("Azerbeidzjaans"),
    )

    private val euro = Txt("Euro", "Euro")
    private fun c(en: String, nl: String) = Txt(en, nl)
    private val currencyOther = mapOf(
        "ISL" to c("Icelandic króna", "IJslandse kroon"), "NOR" to c("Norwegian krone", "Noorse kroon"), "SWE" to c("Swedish krona", "Zweedse kroon"),
        "DNK" to c("Danish krone", "Deense kroon"), "GBR" to c("Pound sterling", "Britse pond"), "POL" to c("Polish złoty", "Poolse złoty"),
        "CZE" to c("Czech koruna", "Tsjechische kroon"), "CHE" to c("Swiss franc", "Zwitserse frank"), "LIE" to c("Swiss franc", "Zwitserse frank"),
        "BIH" to c("Convertible mark", "Convertibele mark"), "SRB" to c("Serbian dinar", "Servische dinar"), "ALB" to c("Albanian lek", "Albanese lek"),
        "MKD" to c("Macedonian denar", "Macedonische denar"), "ROU" to c("Romanian leu", "Roemeense leu"), "MDA" to c("Moldovan leu", "Moldavische leu"),
        "UKR" to c("Ukrainian hryvnia", "Oekraïense hryvnja"), "BLR" to c("Belarusian ruble", "Belarussische roebel"), "RUS" to c("Russian ruble", "Russische roebel"),
        "HUN" to c("Hungarian forint", "Hongaarse forint"), "TUR" to c("Turkish lira", "Turkse lira"), "GEO" to c("Georgian lari", "Georgische lari"),
        "ARM" to c("Armenian dram", "Armeense dram"), "AZE" to c("Azerbaijani manat", "Azerbeidzjaanse manat"),
    )
    fun currency(code: String): Txt = currencyOther[code] ?: euro

    /** Hoe het land zichzelf noemt, met uitspraak als het schrift anders is. */
    val localNames: Map<String, Pair<String, String?>> = mapOf(
        "ISL" to ("Ísland" to null), "NOR" to ("Norge" to null), "SWE" to ("Sverige" to null), "FIN" to ("Suomi" to null), "DNK" to ("Danmark" to null),
        "EST" to ("Eesti" to null), "LVA" to ("Latvija" to null), "LTU" to ("Lietuva" to null), "IRL" to ("Éire" to null), "BEL" to ("Belgique" to null),
        "LUX" to ("Lëtzebuerg" to null), "DEU" to ("Deutschland" to null), "POL" to ("Polska" to null), "CZE" to ("Česko" to null),
        "SVK" to ("Slovensko" to null), "AUT" to ("Österreich" to null), "CHE" to ("Schweiz" to null), "FRA" to ("France" to null),
        "ESP" to ("España" to null), "ITA" to ("Italia" to null), "VAT" to ("Città del Vaticano" to null), "SVN" to ("Slovenija" to null),
        "HRV" to ("Hrvatska" to null), "BIH" to ("Bosna i Hercegovina" to null), "SRB" to ("Србија" to "Srbija"), "MNE" to ("Crna Gora" to null),
        "KOS" to ("Kosova" to null), "ALB" to ("Shqipëria" to null), "MKD" to ("Северна Македонија" to "Severna Makedonija"),
        "GRC" to ("Ελλάδα" to "Elláda"), "BGR" to ("България" to "Bǎlgarija"), "ROU" to ("România" to null), "MDA" to ("Moldova" to null),
        "UKR" to ("Україна" to "Ukrajina"), "BLR" to ("Беларусь" to "Bielaruś"), "RUS" to ("Россия" to "Rossija"), "HUN" to ("Magyarország" to null),
        "CYP" to ("Κύπρος" to "Kýpros"), "TUR" to ("Türkiye" to null), "GEO" to ("საქართველო" to "Sakartvelo"), "ARM" to ("Հայաստան" to "Hayastan"),
        "AZE" to ("Azərbaycan" to null),
    )

    /** Grote steden (geen hoofdsteden), belangrijkste eerst. */
    val cities: List<Place> = listOf(
        place("Hamburg", "Hamburg", 53.551, 9.994), place("München", "Munich", 48.135, 11.582, null, null, "munchen", "munich"),
        place("Barcelona", "Barcelona", 41.385, 2.173), place("Milaan", "Milan", 45.464, 9.190, null, null, "milano"),
        place("Istanbul", "Istanbul", 41.008, 28.978), place("Sint-Petersburg", "Saint Petersburg", 59.934, 30.336, null, null, "st petersburg", "petersburg"),
        place("Marseille", "Marseille", 43.296, 5.370), place("Manchester", "Manchester", 53.481, -2.243),
        place("Antwerpen", "Antwerp", 51.219, 4.402), place("Rotterdam", "Rotterdam", 51.924, 4.478),
        place("Keulen", "Cologne", 50.938, 6.960, null, null, "koln"), place("Frankfurt", "Frankfurt", 50.110, 8.682),
        place("Napels", "Naples", 40.852, 14.268, null, null, "napoli"), place("Venetië", "Venice", 45.441, 12.316, null, null, "venezia"),
        place("Lyon", "Lyon", 45.764, 4.836), place("Porto", "Porto", 41.150, -8.611), place("Valencia", "Valencia", 39.470, -0.376),
        place("Edinburgh", "Edinburgh", 55.953, -3.188), place("Zürich", "Zurich", 47.377, 8.542), place("Kraków", "Kraków", 50.065, 19.945, null, null, "krakau", "cracow"),
        place("Stuttgart", "Stuttgart", 48.776, 9.183), place("Dresden", "Dresden", 51.050, 13.737), place("Sevilla", "Seville", 37.389, -5.984),
        place("Málaga", "Málaga", 36.721, -4.421), place("Bilbao", "Bilbao", 43.263, -2.935), place("Turijn", "Turin", 45.070, 7.687, null, null, "torino"),
        place("Florence", "Florence", 43.770, 11.256, null, null, "firenze"), place("Palermo", "Palermo", 38.116, 13.361),
        place("Toulouse", "Toulouse", 43.605, 1.444), place("Bordeaux", "Bordeaux", 44.838, -0.579), place("Nice", "Nice", 43.710, 7.262),
        place("Birmingham", "Birmingham", 52.486, -1.890), place("Glasgow", "Glasgow", 55.864, -4.252), place("Belfast", "Belfast", 54.597, -5.930),
        place("Göteborg", "Gothenburg", 57.709, 11.975, null, null, "goteborg"), place("Bergen", "Bergen", 60.391, 5.322),
        place("Gdańsk", "Gdańsk", 54.352, 18.646, null, null, "danzig"), place("Thessaloniki", "Thessaloniki", 40.640, 22.944),
        place("Odesa", "Odesa", 46.482, 30.723, null, null, "odessa"), place("Lviv", "Lviv", 49.840, 24.030, null, null, "lemberg"),
        place("Genève", "Geneva", 46.204, 6.143, null, null, "geneve", "genf"), place("Salzburg", "Salzburg", 47.809, 13.055),
    )

    val seas: List<Place> = listOf(
        place("Noordzee", "North Sea", 56.0, 3.0), place("Middellandse Zee", "Mediterranean Sea", 37.8, 6.0, null, null, "mediterranean"),
        place("Atlantische Oceaan", "Atlantic Ocean", 47.0, -14.0, null, null, "atlantic"), place("Oostzee", "Baltic Sea", 56.0, 18.8, null, null, "baltische zee", "baltic"),
        place("Zwarte Zee", "Black Sea", 43.4, 34.0), place("Het Kanaal", "English Channel", 50.2, -1.8, null, null, "kanaal", "channel"),
        place("Adriatische Zee", "Adriatic Sea", 43.0, 15.6), place("Golf van Biskaje", "Bay of Biscay", 45.5, -4.5, null, null, "biscay"),
        place("Noorse Zee", "Norwegian Sea", 67.0, 0.0), place("Straat van Gibraltar", "Strait of Gibraltar", 35.95, -5.55, null, null, "gibraltar"),
        place("Egeïsche Zee", "Aegean Sea", 38.9, 25.0), place("Ierse Zee", "Irish Sea", 53.8, -5.3), place("Botnische Golf", "Gulf of Bothnia", 62.5, 20.0),
        place("Kaspische Zee", "Caspian Sea", 42.5, 50.6), place("Bosporus", "Bosporus", 41.12, 29.07, null, null, "bosphorus"),
        place("Skagerrak", "Skagerrak", 58.0, 9.5), place("Kattegat", "Kattegat", 56.8, 11.6), place("Tyrreense Zee", "Tyrrhenian Sea", 40.0, 12.3),
        place("Ionische Zee", "Ionian Sea", 38.0, 18.5), place("Barentszzee", "Barents Sea", 70.3, 37.0), place("Finse Golf", "Gulf of Finland", 59.85, 26.5),
        place("Ligurische Zee", "Ligurian Sea", 43.6, 9.0), place("Zee van Azov", "Sea of Azov", 46.1, 36.6, null, null, "azov"), place("Witte Zee", "White Sea", 65.6, 38.2),
    )

    val mountains: List<Place> = listOf(
        place("Alpen", "Alps", 46.6, 10.0, "Switzerland, Austria, Italy, France", "Zwitserland, Oostenrijk, Italië, Frankrijk"),
        place("Pyreneeën", "Pyrenees", 42.7, 0.6, "Spain and France", "Spanje en Frankrijk", "pyreneeen"),
        place("Karpaten", "Carpathians", 47.6, 25.0, "Romania, Ukraine, Slovakia, Poland", "Roemenië, Oekraïne, Slowakije, Polen"),
        place("Apennijnen", "Apennines", 42.6, 13.2, "Italy", "Italië"),
        place("Kaukasus", "Caucasus", 42.9, 44.0, "Russia, Georgia, Azerbaijan", "Rusland, Georgië, Azerbeidzjan"),
        place("Scandinavisch Hooggebergte", "Scandinavian Mountains", 64.5, 13.5, "Norway and Sweden", "Noorwegen en Zweden", "skanden", "scandes"),
        place("Schotse Hooglanden", "Scottish Highlands", 57.1, -4.6, "Scotland", "Schotland", "highlands"),
        place("Ardennen", "Ardennes", 50.1, 5.6, "Belgium, Luxembourg, France", "België, Luxemburg, Frankrijk"),
        place("Zwarte Woud", "Black Forest", 48.1, 8.2, "Germany", "Duitsland", "schwarzwald"),
        place("Balkangebergte", "Balkan Mountains", 42.75, 25.0, "Bulgaria", "Bulgarije"),
        place("Dinarische Alpen", "Dinaric Alps", 43.9, 17.4, "Western Balkans", "Westelijke Balkan"),
        place("Centraal Massief", "Massif Central", 45.3, 2.9, "France", "Frankrijk"),
        place("Sierra Nevada", "Sierra Nevada", 37.05, -3.3, "Spain", "Spanje"),
        place("Ertsgebergte", "Ore Mountains", 50.55, 13.0, "Germany and Czechia", "Duitsland en Tsjechië", "erzgebirge"),
        place("Pindus", "Pindus", 39.7, 21.2, "Greece", "Griekenland"),
        place("Tatra", "Tatra", 49.2, 20.0, "Slovakia and Poland", "Slowakije en Polen"),
    )

    val islands: List<Place> = listOf(
        place("Sicilië", "Sicily", 37.55, 14.2, "Italy", "Italië", "sicilie"), place("Sardinië", "Sardinia", 40.1, 9.0, "Italy", "Italië", "sardinie"),
        place("Corsica", "Corsica", 42.15, 9.1, "France", "Frankrijk"), place("Kreta", "Crete", 35.25, 24.9, "Greece", "Griekenland"),
        place("Mallorca", "Majorca", 39.62, 2.98, "Spain", "Spanje"), place("Gotland", "Gotland", 57.48, 18.5, "Sweden", "Zweden"),
        place("Faeröer", "Faroe Islands", 62.05, -6.9, "Denmark", "Denemarken", "faroer"), place("Lofoten", "Lofoten", 68.2, 14.0, "Norway", "Noorwegen"),
        place("Rhodos", "Rhodes", 36.2, 28.0, "Greece", "Griekenland"), place("Ibiza", "Ibiza", 38.98, 1.43, "Spain", "Spanje"),
        place("Bornholm", "Bornholm", 55.13, 14.92, "Denmark", "Denemarken"), place("Seeland", "Zealand", 55.45, 11.75, "Denmark", "Denemarken", "sjaelland"),
        place("Shetlandeilanden", "Shetland", 60.35, -1.25, "United Kingdom", "Verenigd Koninkrijk", "shetland"),
        place("Orkney", "Orkney", 59.0, -3.0, "United Kingdom", "Verenigd Koninkrijk"),
        place("Buiten-Hebriden", "Outer Hebrides", 57.8, -7.0, "United Kingdom", "Verenigd Koninkrijk", "hebriden", "hebrides"),
        place("Man", "Isle of Man", 54.23, -4.55, "British Crown", "Britse Kroon", "isle of man"),
        place("Lesbos", "Lesbos", 39.2, 26.3, "Greece", "Griekenland"), place("Saaremaa", "Saaremaa", 58.42, 22.5, "Estonia", "Estland"),
        place("Åland", "Åland", 60.2, 20.0, "Finland", "Finland", "aland"), place("Elba", "Elba", 42.78, 10.25, "Italy", "Italië"),
    )

    /** Rivieren in de wereldkaartdata, met Engelse naam en loop. */
    val rivers: List<Place> = listOf(
        place("Rijn", "Rhine", 0.0, 0.0, "Switzerland → North Sea", "Zwitserland → Noordzee", "rhein", "rhine"),
        place("Donau", "Danube", 0.0, 0.0, "Germany → Black Sea", "Duitsland → Zwarte Zee", "danube"),
        place("Seine", "Seine", 0.0, 0.0, "France → English Channel", "Frankrijk → Het Kanaal"),
        place("Theems", "Thames", 0.0, 0.0, "England → North Sea", "Engeland → Noordzee", "thames"),
        place("Elbe", "Elbe", 0.0, 0.0, "Czechia → North Sea", "Tsjechië → Noordzee"),
        place("Wolga", "Volga", 0.0, 0.0, "Russia → Caspian Sea", "Rusland → Kaspische Zee", "volga"),
        place("Loire", "Loire", 0.0, 0.0, "France → Atlantic Ocean", "Frankrijk → Atlantische Oceaan"),
        place("Rhône", "Rhône", 0.0, 0.0, "Switzerland → Mediterranean", "Zwitserland → Middellandse Zee", "rhone"),
        place("Po", "Po", 0.0, 0.0, "Italy → Adriatic Sea", "Italië → Adriatische Zee"),
        place("Taag", "Tagus", 0.0, 0.0, "Spain → Atlantic Ocean", "Spanje → Atlantische Oceaan", "tejo", "tajo"),
        place("Oder", "Oder", 0.0, 0.0, "Czechia → Baltic Sea", "Tsjechië → Oostzee", "odra"),
        place("Wisła", "Vistula", 0.0, 0.0, "Poland → Baltic Sea", "Polen → Oostzee", "wisla", "weichsel"),
        place("Dnjepr", "Dnieper", 0.0, 0.0, "Russia → Black Sea", "Rusland → Zwarte Zee", "dnipro", "dnepr"),
        place("Ebro", "Ebro", 0.0, 0.0, "Spain → Mediterranean", "Spanje → Middellandse Zee"),
        place("Garonne", "Garonne", 0.0, 0.0, "Spain → Bay of Biscay", "Spanje → Golf van Biskaje"),
        place("Douro", "Douro", 0.0, 0.0, "Spain → Atlantic Ocean", "Spanje → Atlantische Oceaan", "duero"),
        place("Don", "Don", 0.0, 0.0, "Russia → Sea of Azov", "Rusland → Zee van Azov"),
        place("Dnjestr", "Dniester", 0.0, 0.0, "Ukraine → Black Sea", "Oekraïne → Zwarte Zee", "dniester", "dnister"),
        place("Daugava", "Daugava", 0.0, 0.0, "Belarus → Baltic Sea", "Belarus → Oostzee", "dvina"),
        place("Tisza", "Tisza", 0.0, 0.0, "Ukraine → Danube", "Oekraïne → Donau", "tisa"),
    )
}
