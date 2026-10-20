package nl.topo.app.data.quiz

import androidx.compose.ui.geometry.Rect
import nl.topo.app.data.content.Region
import nl.topo.app.data.content.RegionKind
import nl.topo.app.data.geo.Continent
import nl.topo.app.data.geo.Country
import nl.topo.app.data.geo.NederlandMap
import nl.topo.app.data.geo.WorldMap
import nl.topo.app.data.i18n.Lang
import nl.topo.app.data.i18n.Txt
import java.text.NumberFormat
import kotlin.random.Random

/**
 * Maakt bij elk onderwerp en niveau een [QuizSetup].
 *
 * Niveaus: ★ en ★★ vier keuzes (★★ met lastige, nabijgelegen afleiders), ★★★ zes keuzes,
 * ★★★★ zelf typen. Bij kaartvragen tik je op ★–★★★ zelf aan; op ★★★★ is het
 * aangewezen kaartdeel uitgelicht en typ je de naam.
 */
object QuizFactory {

    fun create(region: Region, topicId: String, level: Int, world: WorldMap?, nl: NederlandMap?): QuizSetup? = when (region.kind) {
        RegionKind.COUNTRY -> nl?.let { nederland(topicId, level, it) }
        else -> world?.let { countries(region, topicId, level, it) }
    }

    // ================================================================
    // Landen (wereld en werelddelen)
    // ================================================================

    private fun countries(region: Region, topicId: String, level: Int, w: WorldMap): QuizSetup? {
        val cont = region.continent
        val all = w.countriesIn(cont).sortedByDescending { it.population }
        val viewport = cont?.let { w.continents[it]?.viewport } ?: w.worldViewport
        fun pool(filter: (Country) -> Boolean = { true }) = Levels4.pool(all.filter(filter), level)
        val nOpt = Levels4.options(level)
        val typing = nOpt == 0

        fun near(c: Country, among: List<Country>, n: Int, rnd: Random): List<Country> {
            val others = among.filter { it.code != c.code }
            return if (level == 1) others.shuffled(rnd).take(n)
            else others.sortedBy { (it.label - c.label).getDistance() }.take(n + 3).shuffled(rnd).take(n)
        }
        fun countryOptions(c: Country, among: List<Country>, rnd: Random, asFlags: Boolean = false): List<Opt> =
            (near(c, among, nOpt - 1, rnd) + c).distinctBy { it.names.en }.shuffled(rnd)
                .map { Opt(it.code, it.names, if (asFlags) it else null) }
        fun capInfo(c: Country): Txt? = c.capitals?.let { Txt("Capital: ${it.en}", "Hoofdstad: ${it.nl}") }

        val areaMap = QuizMap(
            geo = w.geo, viewport = viewport,
            features = all.map { Feature.Area(it.code, it.names, w.geo.countries.getValue(it.code), it.label) },
            activeAreas = all.map { it.code }.toSet(),
        )

        return when (topicId) {
            "kaart" -> {
                val p = pool()
                QuizSetup(Txt("Find on the map", "Zoeken op de kaart"), p.map { it.code }, areaMap) { id, _ ->
                    val c = w.countries.getValue(id)
                    if (typing) Question(id, Prompt.Words(Txt("Which country is highlighted?", "Welk land is uitgelicht?")), Mode.TYPE,
                        correct = setOf(id), accept = Names.country(c), answer = c.names, info = capInfo(c), focus = id)
                    else Question(id, Prompt.Words(Txt("Tap", "Tik aan:"), c.names), Mode.TAP, correct = setOf(id), answer = c.names, info = capInfo(c), focus = id)
                }
            }

            "hoofdstad-kaart" -> {
                val p = pool { it.capitalPos != null && it.capitals != null }
                val map = QuizMap(
                    geo = w.geo, viewport = viewport,
                    features = p.map { Feature.Point(it.code, it.capitals!!, it.capitalPos!!) },
                    activeAreas = all.map { it.code }.toSet(),
                )
                QuizSetup(Txt("Capital on the map", "Hoofdstad op de kaart"), p.map { it.code }, map) { id, _ ->
                    val c = w.countries.getValue(id)
                    val cap = c.capitals!!
                    val info = Txt("Capital of ${c.names.en}", "Hoofdstad van ${c.names.nl}")
                    if (typing) Question(id, Prompt.Words(Txt("Which capital is highlighted?", "Welke hoofdstad is uitgelicht?")), Mode.TYPE,
                        correct = setOf(id), accept = Names.both(cap), answer = cap, info = info, focus = id)
                    else Question(id, Prompt.Words(Txt("Tap the capital of ${c.names.en}", "Tik de hoofdstad van ${c.names.nl} aan"), cap), Mode.TAP,
                        correct = setOf(id), answer = cap, info = info, focus = id)
                }
            }

            "hoofdstad" -> {
                val p = pool { it.capitals != null }
                val withCap = all.filter { it.capitals != null }
                QuizSetup(Txt("Capital", "Hoofdstad"), p.map { it.code }, null) { id, rnd ->
                    val c = w.countries.getValue(id)
                    val cap = c.capitals!!
                    val prompt = Prompt.Words(Txt("What is the capital of", "Wat is de hoofdstad van"), c.names, flag = c)
                    if (typing) Question(id, prompt, Mode.TYPE, correct = setOf(id), accept = Names.both(cap), answer = cap)
                    else Question(id, prompt, Mode.CHOICE,
                        options = (near(c, withCap, nOpt - 1, rnd) + c).distinctBy { it.capitals!!.en }.shuffled(rnd).map { Opt(it.code, it.capitals!!) },
                        correct = setOf(id), answer = cap)
                }
            }

            "vlag" -> {
                val p = pool()
                QuizSetup(Txt("Flag", "Vlag"), p.map { it.code }, null) { id, rnd ->
                    val c = w.countries.getValue(id)
                    val reverse = level in 2..3 && rnd.nextFloat() < (if (level == 2) 0.3f else 0.45f)
                    when {
                        typing -> Question(id, Prompt.FlagPic(Txt("Which country's flag is this? Type the name.", "Van welk land is deze vlag? Typ de naam."), c),
                            Mode.TYPE, correct = setOf(id), accept = Names.country(c), answer = c.names, info = capInfo(c))
                        reverse -> Question(id, Prompt.Words(Txt("Which flag belongs to", "Welke vlag hoort bij"), c.names), Mode.CHOICE,
                            options = (near(c, all, nOpt - 1, rnd) + c).distinctBy { it.flagAsset }.shuffled(rnd).map { Opt(it.code, it.names, it) },
                            correct = setOf(id), answer = c.names, info = capInfo(c))
                        else -> Question(id, Prompt.FlagPic(Txt("Which country's flag is this?", "Van welk land is deze vlag?"), c), Mode.CHOICE,
                            options = countryOptions(c, all, rnd), correct = setOf(id), answer = c.names, info = capInfo(c))
                    }
                }
            }

            "contour" -> {
                // Microstaten en piepkleine eilanden zijn als silhouet niet te herkennen.
                val p = pool { it.bounds.width >= 3f && it.bounds.height >= 3f }
                val eligible = all.filter { it.bounds.width >= 3f && it.bounds.height >= 3f }
                QuizSetup(Txt("Outline", "Grenscontouren"), p.map { it.code }, null) { id, rnd ->
                    val c = w.countries.getValue(id)
                    val b = c.bounds
                    val shape = w.geo.countries.getValue(id).within(Rect(b.left - 1, b.top - 1, b.right + 1, b.bottom + 1))
                    val prompt = Prompt.ShapePic(Txt("Which country has this shape?", "Welk land heeft deze vorm?"), shape)
                    if (typing) Question(id, prompt, Mode.TYPE, correct = setOf(id), accept = Names.country(c), answer = c.names, focus = id)
                    else Question(id, prompt, Mode.CHOICE, options = countryOptions(c, eligible, rnd), correct = setOf(id), answer = c.names, focus = id)
                }
            }

            "buurlanden" -> {
                val p = pool { c -> c.neighbours.any { it in w.countries } }
                QuizSetup(Txt("Neighbours", "Buurlanden"), p.map { it.code }, null) { id, rnd ->
                    val c = w.countries.getValue(id)
                    val nb = c.neighbours.mapNotNull { w.countries[it] }
                    val answer = Txt(nb.joinToString(", ") { it.names.en }, nb.joinToString(", ") { it.names.nl })
                    val prompt = Prompt.Words(Txt("Which country borders", "Welk land grenst aan"), c.names, flag = c)
                    if (typing) Question(id, prompt, Mode.TYPE, correct = nb.map { it.code }.toSet(), accept = nb.flatMap { Names.country(it) }, answer = answer)
                    else {
                        val right = nb.random(rnd)
                        val wrong = w.all.filter { it.code != id && it.code !in c.neighbours }
                            .sortedBy { (it.label - c.label).getDistance() }.take(nOpt + 4).shuffled(rnd).take(nOpt - 1)
                        Question(id, prompt, Mode.CHOICE,
                            options = (wrong + right).shuffled(rnd).map { Opt(it.code, it.names) },
                            correct = nb.map { it.code }.toSet(), answer = answer,
                            info = Txt("All neighbours: ${answer.en}", "Alle buurlanden: ${answer.nl}"))
                    }
                }
            }

            "bevolking" -> numberQuiz(Txt("Population", "Bevolking"), pool(), level, Txt("How many people live in", "Hoeveel inwoners heeft"),
                value = { it.population.toDouble() }, format = Numbers::population,
                note = Txt("Estimate from 2019", "Schatting uit 2019"))

            "oppervlakte" -> numberQuiz(Txt("Area", "Oppervlakte"), pool { it.areaKm2 != null }, level, Txt("How big is", "Hoe groot is"),
                value = { it.areaKm2!! }, format = Numbers::area, note = null)

            "werelddeel" -> {
                val p = pool()
                QuizSetup(Txt("Continent", "Werelddeel"), p.map { it.code }, null) { id, _ ->
                    val c = w.countries.getValue(id)
                    val names = c.continents.mapNotNull { w.continents[it]?.names }
                    Question(id, Prompt.Words(Txt("Which continent is it in?", "In welk werelddeel ligt"), c.names, flag = c), Mode.CHOICE,
                        options = Continent.entries.mapNotNull { k -> w.continents[k]?.let { Opt(k.code, it.names) } },
                        correct = c.continents.map { it.code }.toSet(),
                        answer = Txt(names.joinToString(" / ") { it.en }, names.joinToString(" / ") { it.nl }))
                }
            }

            // ---------- alleen Europa ----------
            "taal" -> {
                val p = pool { it.code in EuropeData.languages }
                val allLangs = EuropeData.languages.values.flatten().distinctBy { it.nl }
                QuizSetup(Txt("Language", "Taal"), p.map { it.code }, null) { id, rnd ->
                    val c = w.countries.getValue(id)
                    val own = EuropeData.languages.getValue(id)
                    val answer = Txt(own.joinToString(", ") { it.en }, own.joinToString(", ") { it.nl })
                    val prompt = Prompt.Words(Txt("Which language is official in", "Welke taal is officieel in"), c.names, flag = c)
                    if (typing) Question(id, prompt, Mode.TYPE, correct = own.map { it.nl }.toSet(), accept = own.flatMap { Names.both(it) }, answer = answer)
                    else {
                        val right = own.random(rnd)
                        val nearby = near(c, all.filter { it.code in EuropeData.languages }, 10, rnd).flatMap { EuropeData.languages.getValue(it.code) }
                        val wrong = (nearby + allLangs.shuffled(rnd)).filter { l -> own.none { it.nl == l.nl } }.distinctBy { it.nl }.take(nOpt - 1)
                        Question(id, prompt, Mode.CHOICE, options = (wrong + right).shuffled(rnd).map { Opt(it.nl, it) },
                            correct = own.map { it.nl }.toSet(), answer = answer)
                    }
                }
            }

            "valuta" -> {
                val p = pool { it.code in EuropeData.languages }
                val allCur = all.filter { it.code in EuropeData.languages }.map { EuropeData.currency(it.code) }.distinctBy { it.nl }
                QuizSetup(Txt("Currency", "Valuta"), p.map { it.code }, null) { id, rnd ->
                    val c = w.countries.getValue(id)
                    val cur = EuropeData.currency(id)
                    val prompt = Prompt.Words(Txt("What money do they use in", "Met welk geld betaal je in"), c.names, flag = c)
                    if (typing) Question(id, prompt, Mode.TYPE, correct = setOf(cur.nl),
                        accept = Names.both(cur) + listOf(cur.en.substringAfterLast(' '), cur.nl.substringAfterLast(' ')), answer = cur)
                    else {
                        val nearby = near(c, all.filter { it.code in EuropeData.languages }, 12, rnd).map { EuropeData.currency(it.code) }
                        val wrong = (nearby + allCur.shuffled(rnd)).filter { it.nl != cur.nl }.distinctBy { it.nl }.take(nOpt - 1)
                        Question(id, prompt, Mode.CHOICE, options = (wrong + cur).shuffled(rnd).map { Opt(it.nl, it) }, correct = setOf(cur.nl), answer = cur)
                    }
                }
            }

            "lokaal" -> {
                val p = pool { it.code in EuropeData.localNames }
                QuizSetup(Txt("Local name", "Lokale naam"), p.map { it.code }, null) { id, rnd ->
                    val c = w.countries.getValue(id)
                    val (local, roman) = EuropeData.localNames.getValue(id)
                    val prompt = Prompt.Words(Txt("Which country calls itself", "Welk land noemt zichzelf"), Txt(local, local),
                        note = roman?.let { Txt("pronounced: $it", "uitspraak: $it") })
                    if (typing) Question(id, prompt, Mode.TYPE, correct = setOf(id), accept = Names.country(c), answer = c.names)
                    else Question(id, prompt, Mode.CHOICE, options = countryOptions(c, all, rnd), correct = setOf(id), answer = c.names)
                }
            }

            "tld" -> {
                val p = pool { it.code in w.tld }
                QuizSetup(Txt("Internet domain", "Internetextensie"), p.map { it.code }, null) { id, rnd ->
                    val c = w.countries.getValue(id)
                    val tld = w.tld.getValue(id)
                    val prompt = Prompt.Words(Txt("Which internet domain belongs to", "Welke internetextensie hoort bij"), c.names, flag = c)
                    if (typing) Question(id, prompt, Mode.TYPE, correct = setOf(id), accept = listOf(tld, tld.removePrefix(".")), answer = Txt(tld, tld))
                    else Question(id, prompt, Mode.CHOICE,
                        options = (near(c, all.filter { it.code in w.tld }, nOpt - 1, rnd) + c).distinctBy { w.tld[it.code] }.shuffled(rnd)
                            .map { Opt(it.code, Txt(w.tld.getValue(it.code), w.tld.getValue(it.code))) },
                        correct = setOf(id), answer = Txt(tld, tld))
                }
            }

            // ---------- kaart van Europa ----------
            "steden", "zeeen", "gebergten", "eilanden" -> {
                val places = when (topicId) {
                    "steden" -> EuropeData.cities
                    "zeeen" -> EuropeData.seas
                    "gebergten" -> EuropeData.mountains
                    else -> EuropeData.islands
                }
                val title = region.topics.first { it.id == topicId }.name
                pointQuiz(title, Levels4.pool(places, level), places, level, w.geo, viewport, all.map { it.code }.toSet(), true) { pl ->
                    pl.note ?: w.geo.countryAt(w.geo.projection.project(pl.lat, pl.lon))?.let { w.countries[it]?.names }
                }
            }

            "rivieren" -> {
                val rivers = EuropeData.rivers.filter { it.id in w.rivers }
                val p = Levels4.pool(rivers, level)
                val map = QuizMap(
                    geo = w.geo, viewport = viewport,
                    features = p.map { Feature.Line(it.id, it.name, w.rivers.getValue(it.id)) },
                    activeAreas = all.map { it.code }.toSet(),
                )
                QuizSetup(Txt("Rivers", "Rivieren"), p.map { it.id }, map) { id, _ ->
                    val r = rivers.first { it.id == id }
                    if (typing) Question(id, Prompt.Words(Txt("Which river is highlighted?", "Welke rivier is uitgelicht?")), Mode.TYPE,
                        correct = setOf(id), accept = Names.both(r.name) + r.alt, answer = r.name, info = r.note, focus = id)
                    else Question(id, Prompt.Words(Txt("Tap the river", "Tik de rivier aan:"), r.name), Mode.TAP, correct = setOf(id), answer = r.name, info = r.note, focus = id)
                }
            }

            else -> null
        }
    }

    private fun numberQuiz(
        title: Txt, pool: List<Country>, level: Int, lead: Txt,
        value: (Country) -> Double, format: (Double, Lang) -> String, note: Txt?,
    ): QuizSetup {
        // Hoe dicht de foute antwoorden bij het goede liggen, per niveau.
        val factors = when (level) {
            1 -> listOf(0.2, 0.35, 2.8, 4.5, 7.0)
            2 -> listOf(0.3, 0.5, 2.0, 3.0)
            3 -> listOf(0.45, 0.6, 1.6, 2.1, 2.8, 0.75)
            else -> listOf(0.55, 0.7, 1.35, 1.7, 2.2, 0.8)
        }
        val n = if (level >= 3) 6 else 4
        return QuizSetup(title, pool.map { it.code }, null) { id, rnd ->
            val c = pool.first { it.code == id }
            val v = value(c)
            fun txt(x: Double) = Txt(format(x, Lang.EN), format(x, Lang.NL))
            val values = mutableListOf(v)
            for (k in factors.shuffled(rnd)) {
                if (values.size >= n) break
                val x = v * k
                if (values.none { txt(it).en == txt(x).en }) values.add(x)
            }
            val opts = values.sorted().mapIndexed { i, x -> Opt(if (x == v) "ok" else "o$i", txt(x)) }
            Question(id, Prompt.Words(lead, c.names, note = note, flag = c), Mode.CHOICE, options = opts, correct = setOf("ok"), answer = txt(v))
        }
    }

    /** Aanwijsquiz met punten (steden, zeeën, gebergten, eilanden). */
    private fun pointQuiz(
        title: Txt, pool: List<Place>, all: List<Place>, level: Int,
        geo: nl.topo.app.data.geo.GeoMap, viewport: Rect, active: Set<String>, graticule: Boolean,
        info: (Place) -> Txt?,
    ): QuizSetup {
        val map = QuizMap(
            geo = geo, viewport = viewport,
            features = pool.map { Feature.Point(it.id, it.name, geo.projection.project(it.lat, it.lon)) },
            activeAreas = active, graticule = graticule,
        )
        val typing = Levels4.options(level) == 0
        return QuizSetup(title, pool.map { it.id }, map) { id, _ ->
            val pl = all.first { it.id == id }
            if (typing) Question(id, Prompt.Words(Txt("What is highlighted?", "Wat is hier uitgelicht?")), Mode.TYPE,
                correct = setOf(id), accept = Names.both(pl.name) + pl.alt, answer = pl.name, info = info(pl), focus = id)
            else Question(id, Prompt.Words(Txt("Tap", "Tik aan:"), pl.name), Mode.TAP, correct = setOf(id), answer = pl.name, info = info(pl), focus = id)
        }
    }

    // ================================================================
    // Nederland
    // ================================================================

    private fun nederland(topicId: String, level: Int, nl: NederlandMap): QuizSetup? {
        val geo = nl.geo
        val viewport = Rect(-5f, -5f, geo.width + 5f, geo.height + 5f)
        val typing = Levels4.options(level) == 0
        fun pos(lat: Double, lon: Double) = geo.projection.project(lat, lon)
        val allProv = NederlandData.provinces.map { it.id }.toSet()

        return when (topicId) {
            "provincies" -> {
                val p = Levels4.pool(NederlandData.provinces, level)
                val map = QuizMap(geo, viewport, NederlandData.provinces.map { Feature.Area(it.id, it.name, geo.countries.getValue(it.id), pos(it.lat, it.lon)) }, allProv, graticule = false)
                QuizSetup(Txt("Provinces", "Provincies"), p.map { it.id }, map) { id, _ ->
                    val pr = NederlandData.provinces.first { it.id == id }
                    val info = Txt("Capital: ${pr.capital}", "Hoofdstad: ${pr.capital}")
                    if (typing) Question(id, Prompt.Words(Txt("Which province is highlighted?", "Welke provincie is uitgelicht?")), Mode.TYPE,
                        correct = setOf(id), accept = Names.both(pr.name) + pr.alt, answer = pr.name, info = info, focus = id)
                    else Question(id, Prompt.Words(Txt("Tap", "Tik aan:"), pr.name), Mode.TAP, correct = setOf(id), answer = pr.name, info = info, focus = id)
                }
            }

            "hoofdsteden" -> {
                val p = Levels4.pool(NederlandData.provinces, level)
                val cityPos = NederlandData.cities.associateBy { it.id }
                val map = QuizMap(geo, viewport, NederlandData.provinces.mapNotNull { pr ->
                    cityPos[pr.capital]?.let { c -> Feature.Point(pr.id, c.name, pos(c.lat, c.lon)) }
                }, allProv, graticule = false)
                QuizSetup(Txt("Provincial capitals", "Hoofdsteden"), p.map { it.id }, map) { id, _ ->
                    val pr = NederlandData.provinces.first { it.id == id }
                    val cap = cityPos.getValue(pr.capital).name
                    val info = Txt("Capital of ${pr.name.en}", "Hoofdstad van ${pr.name.nl}")
                    if (typing) Question(id, Prompt.Words(Txt("Which capital is highlighted?", "Welke hoofdstad is uitgelicht?")), Mode.TYPE,
                        correct = setOf(id), accept = Names.both(cap) + NederlandData.cityAlt[pr.capital].orEmpty(), answer = cap, info = info, focus = id)
                    else Question(id, Prompt.Words(Txt("Tap the capital of ${pr.name.en}", "Tik de hoofdstad van ${pr.name.nl} aan"), cap), Mode.TAP,
                        correct = setOf(id), answer = cap, info = info, focus = id)
                }
            }

            "steden" -> pointQuiz(Txt("Cities", "Steden"), Levels4.pool(NederlandData.cities, level), NederlandData.cities, level,
                geo, viewport, allProv, false) { it.note }

            "wateren" -> pointQuiz(Txt("Waters", "Wateren"), Levels4.pool(NederlandData.waters, level), NederlandData.waters, level,
                geo, viewport, allProv, false) { it.note }

            "wadden" -> {
                val isl = NederlandData.wadden.filter { it.id in nl.islands }
                val map = QuizMap(geo, viewport, isl.map { Feature.Area(it.id, it.name, nl.islands.getValue(it.id), pos(it.lat, it.lon)) }, allProv, graticule = false)
                QuizSetup(Txt("Wadden Islands", "Waddeneilanden"), isl.map { it.id }, map) { id, _ ->
                    val i = isl.first { it.id == id }
                    if (typing) Question(id, Prompt.Words(Txt("Which island is highlighted?", "Welk eiland is uitgelicht?")), Mode.TYPE,
                        correct = setOf(id), accept = Names.both(i.name), answer = i.name, info = i.note, focus = id)
                    else Question(id, Prompt.Words(Txt("Tap", "Tik aan:"), i.name), Mode.TAP, correct = setOf(id), answer = i.name, info = i.note, focus = id)
                }
            }

            else -> null
        }
    }
}

/** Goede namen bij typen. */
object Names {
    private val countryAliases: Map<String, List<String>> = mapOf(
        "USA" to listOf("vs", "usa", "us", "amerika", "america", "verenigde staten van amerika", "united states of america"),
        "GBR" to listOf("vk", "uk", "groot-brittannie", "great britain", "britain", "engeland", "england"),
        "BLR" to listOf("wit-rusland", "belorussia"),
        "CZE" to listOf("tsjechie", "czech republic"),
        "NLD" to listOf("holland", "the netherlands"),
        "CIV" to listOf("cote d'ivoire"),
        "COD" to listOf("dr congo", "drc", "democratische republiek congo", "democratic republic of the congo"),
        "COG" to listOf("congo", "republiek congo"),
        "MKD" to listOf("macedonie", "macedonia"),
        "ARE" to listOf("vae", "uae", "emiraten", "emirates"),
        "KOR" to listOf("korea"),
        "MMR" to listOf("birma", "burma"),
        "SWZ" to listOf("swaziland"),
        "TLS" to listOf("timor-leste"),
        "BIH" to listOf("bosnie", "bosnia"),
        "VAT" to listOf("vaticaan", "vatican", "holy see"),
        "TUR" to listOf("turkiye"),
        "CPV" to listOf("cape verde", "cabo verde"),
    )

    fun both(t: Txt): List<String> = listOf(t.en, t.nl)
    fun country(c: Country): List<String> = both(c.names) + countryAliases[c.code].orEmpty()
}

/** Getallen netjes, in de stijl van de taal. */
object Numbers {
    fun population(v: Double, lang: Lang): String {
        val n = v.toLong()
        return when {
            n >= 1_000_000 -> NumberFormat.getNumberInstance(lang.locale).apply { maximumFractionDigits = if (n >= 10_000_000) 0 else 1 }
                .format(n / 1_000_000.0) + lang.t(" million", " mln")
            n >= 10_000 -> NumberFormat.getIntegerInstance(lang.locale).format((n / 1000) * 1000)
            else -> NumberFormat.getIntegerInstance(lang.locale).format((n / 10) * 10)
        }
    }

    fun area(v: Double, lang: Lang): String {
        if (v < 10) return NumberFormat.getNumberInstance(lang.locale).apply { maximumFractionDigits = 2 }.format(v) + " km²"
        val step = Math.pow(10.0, (Math.floor(Math.log10(v)) - 2).coerceAtLeast(0.0))
        return NumberFormat.getIntegerInstance(lang.locale).format((Math.round(v / step) * step).toLong()) + " km²"
    }
}
