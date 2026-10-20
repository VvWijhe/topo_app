package nl.topo.app.data.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import java.text.Collator
import java.util.Locale

/**
 * Talen van de app. Een nieuwe taal toevoegen: voeg hier een waarde toe en geef
 * [Txt] en [Lang.t] een extra veld; ontbrekende vertalingen vallen terug op Engels.
 */
enum class Lang(val label: String, val locale: Locale) {
    EN("English", Locale.UK),
    NL("Nederlands", Locale("nl", "NL"));

    /** Kies de tekst voor deze taal. */
    fun t(en: String, nl: String): String = when (this) { EN -> en; NL -> nl }

    val collator: Collator get() = Collator.getInstance(locale)
}

/** Een tekst in alle talen; voor vaste inhoud zoals onderwerpen en regio's. */
data class Txt(val en: String, val nl: String) {
    operator fun get(lang: Lang): String = lang.t(en, nl)
}

/** Huidige taal van de app; wordt in MainActivity gezet vanuit de instellingen. */
val LocalLang = staticCompositionLocalOf { Lang.EN }

/** Korte schrijfwijze in schermen: tr("Settings", "Instellingen"). */
@Composable
@ReadOnlyComposable
fun tr(en: String, nl: String): String = LocalLang.current.t(en, nl)

/** Een [Txt] in de huidige taal. */
val Txt.text: String
    @Composable @ReadOnlyComposable get() = this[LocalLang.current]
