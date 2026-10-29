package nl.topo.app.data.puzzle

import nl.topo.app.data.geo.WorldMap
import nl.topo.app.data.i18n.Txt
import kotlin.random.Random

/**
 * Vlaggenweetjes: een kleine quizshow met Pico (een eigenwijze, grasgroene papegaai).
 * Elke vraag heeft één goed land (ISO3) en vaste of willekeurige foute keuzes; na het antwoord volgt een weetje.
 */
data class FlagQuestion(val q: Txt, val answer: String, val wrong: List<String>, val fact: Txt)

object FlagShow {
    const val PER_EPISODE = 8

    private fun q(en: String, nl: String, answer: String, wrong: String, factEn: String, factNl: String) =
        FlagQuestion(Txt(en, nl), answer, wrong.split(' '), Txt(factEn, factNl))

    val questions = listOf(
        q("Which country has the only national flag that isn't a rectangle?", "Welk land heeft de enige nationale vlag die geen rechthoek is?", "NPL", "BTN IND CHE",
            "Nepal's flag is two stacked triangles. Its constitution even explains, step by step, how to draw it with a ruler and compass.",
            "De vlag van Nepal bestaat uit twee gestapelde driehoeken. De grondwet legt zelfs stap voor stap uit hoe je hem met liniaal en passer tekent."),
        q("Which flag shows a modern assault rifle?", "Op welke vlag staat een modern aanvalsgeweer?", "MOZ", "AGO ZWE KEN",
            "Mozambique's flag shows an AK-47 with a bayonet, crossed with a hoe, on an open book: defence, farming and education.",
            "Op de vlag van Mozambique staat een AK-47 met bajonet, gekruist met een schoffel, op een open boek: verdediging, landbouw en onderwijs."),
        q("Which country's flag is often called the oldest national flag still in use?", "Welke vlag wordt vaak de oudste nog gebruikte nationale vlag genoemd?", "DNK", "NOR SWE FIN",
            "Denmark's Dannebrog. Legend says it fell from the sky during a battle in 1219. All the other Nordic crosses are modelled on it.",
            "De Deense Dannebrog. Volgens de legende viel hij in 1219 tijdens een veldslag uit de lucht. Alle andere Scandinavische kruisen zijn erop gebaseerd."),
        q("Which country's national flag is square?", "Welk land heeft een vierkante nationale vlag?", "CHE", "DNK AUT CAN",
            "Switzerland! Only the Vatican City shares this honour. On Swiss lakes, though, boats fly a rectangular version.",
            "Zwitserland! Alleen Vaticaanstad deelt die eer. Op de Zwitserse meren varen boten wel met een rechthoekige versie."),
        q("Which flag has a different emblem on its front and its back?", "Welke vlag heeft aan de voor- en achterkant een ander embleem?", "PRY", "URY ARG BOL",
            "Paraguay: the national coat of arms on the front, the treasury seal with a lion on the back.",
            "Paraguay: voorop het wapen van het land, achterop het zegel van de schatkist met een leeuw."),
        q("Which flag shows a map of its own country?", "Op welke vlag staat een kaart van het land zelf?", "CYP", "GRC TUR MLT",
            "Cyprus. The island is copper-coloured because its name is linked to the old word for copper. The olive branches stand for peace.",
            "Cyprus. Het eiland is koperkleurig, omdat de naam verwant is aan het oude woord voor koper. De olijftakken staan voor vrede."),
        q("Which country's flag is almost identical to Romania's?", "Welke vlag lijkt bijna precies op die van Roemenië?", "TCD", "MLI BEL GIN",
            "Chad. Both are blue, yellow and red vertical stripes; Chad's blue is just a tiny bit darker.",
            "Tsjaad. Allebei blauw, geel en rood in verticale banen; het blauw van Tsjaad is alleen een fractie donkerder."),
        q("Which flag looks the same as Indonesia's, only a different shape?", "Welke vlag ziet eruit als die van Indonesië, alleen met een andere vorm?", "MCO", "POL AUT SGP",
            "Monaco. Both are red over white. Monaco's is 4:5, Indonesia's 2:3. Poland is the same colours upside down.",
            "Monaco. Allebei rood boven wit. Die van Monaco is 4:5, die van Indonesië 2:3. Polen is dezelfde kleuren, maar omgekeerd."),
        q("Which country had a completely plain green flag from 1977 to 2011?", "Welk land had van 1977 tot 2011 een volledig effen groene vlag?", "LBY", "SAU PAK MRT",
            "Libya. It was the only national flag with no design at all, just green.",
            "Libië. Het was de enige nationale vlag zonder enig ontwerp: alleen groen."),
        q("Which flag shows a frigatebird flying over a rising sun?", "Op welke vlag vliegt een fregatvogel boven een opkomende zon?", "KIR", "TUV FJI SLB",
            "Kiribati. The waves below stand for the Pacific Ocean around its many atolls.",
            "Kiribati. De golven eronder staan voor de Grote Oceaan rond de vele atollen."),
        q("Which flag has a thunder dragon on it?", "Op welke vlag staat een donderdraak?", "BTN", "CHN VNM NPL",
            "Bhutan, the 'Land of the Thunder Dragon'. The dragon holds jewels in its claws: the wealth of the country.",
            "Bhutan, het 'land van de donderdraak'. De draak houdt juwelen in zijn klauwen: de rijkdom van het land."),
        q("Which flag shows the night sky over its old capital on one particular evening?", "Welke vlag toont de sterrenhemel boven de oude hoofdstad op één bepaalde avond?", "BRA", "AUS NZL CHN",
            "Brazil: the sky over Rio de Janeiro on 15 November 1889, the day it became a republic. Each star is a state.",
            "Brazilië: de hemel boven Rio de Janeiro op 15 november 1889, de dag dat het een republiek werd. Elke ster is een deelstaat."),
        q("Which flag has 50 stars, one for every state?", "Welke vlag heeft 50 sterren, één voor elke staat?", "USA", "LBR MYS URY",
            "The United States. The 13 stripes are for the original colonies. The current design has been used since 1960.",
            "De Verenigde Staten. De 13 strepen staan voor de oorspronkelijke kolonies. Dit ontwerp wordt sinds 1960 gebruikt."),
        q("Which country only got its current flag, with a red leaf, in 1965?", "Welk land kreeg pas in 1965 zijn huidige vlag, met een rood blad?", "CAN", "USA GBR AUS",
            "Canada. Before that it used a red flag with the Union Jack. The maple leaf has 11 points.",
            "Canada. Daarvoor gebruikte het een rode vlag met de Union Jack. Het esdoornblad heeft 11 punten."),
        q("Which flag is nicknamed the 'circle of the sun'?", "Welke vlag heeft de bijnaam 'cirkel van de zon'?", "JPN", "BGD LAO KOR",
            "Japan's Hinomaru. Japan's name itself means something like 'origin of the sun'.",
            "De Japanse Hinomaru. De naam Japan betekent zelf iets als 'oorsprong van de zon'."),
        q("Which flag has a cedar tree on it?", "Op welke vlag staat een ceder?", "LBN", "SYR JOR ISR",
            "Lebanon. The cedar of Lebanon is mentioned many times in the Bible and stands for strength and eternity.",
            "Libanon. De ceder van Libanon komt vaak voor in de Bijbel en staat voor kracht en eeuwigheid."),
        q("Which flag is never flown at half-mast, because of the text on it?", "Welke vlag hangt nooit halfstok, vanwege de tekst erop?", "SAU", "IRQ ARE PAK",
            "Saudi Arabia: the flag carries the Islamic declaration of faith, which may never be lowered.",
            "Saoedi-Arabië: op de vlag staat de islamitische geloofsbelijdenis, en die mag nooit gestreken worden."),
        q("Which flag shows a golden lion holding a sword?", "Op welke vlag staat een gouden leeuw met een zwaard?", "LKA", "IND MDV BGD",
            "Sri Lanka. The four leaves in the corners stand for kindness, compassion, joy and calm.",
            "Sri Lanka. De vier bladeren in de hoeken staan voor goedheid, mededogen, vreugde en kalmte."),
        q("Which flag shows an eagle on a cactus, eating a snake?", "Op welke vlag zit een adelaar op een cactus die een slang eet?", "MEX", "GTM ECU BOL",
            "Mexico. Aztec legend says they had to build their city where they saw exactly that. It became Tenochtitlan, now Mexico City.",
            "Mexico. Volgens een Azteekse legende moesten ze hun stad bouwen waar ze precies dat zagen. Dat werd Tenochtitlan, nu Mexico-Stad."),
        q("Which flag shows a famous temple?", "Op welke vlag staat een beroemde tempel?", "KHM", "JPN THA VNM",
            "Cambodia, with Angkor Wat: the largest religious monument in the world.",
            "Cambodja, met Angkor Wat: het grootste religieuze monument ter wereld."),
        q("Which flag has a parrot, and is one of the very few with purple?", "Op welke vlag staat een papegaai, en is een van de weinige met paars?", "DMA", "GRD JAM LCA",
            "Dominica. The Sisserou parrot lives only on that island.",
            "Dominica. De sisserou-papegaai leeft alleen op dat eiland."),
        q("Which flag shows a grey crowned crane?", "Op welke vlag staat een grijze kroonkraanvogel?", "UGA", "KEN TZA RWA",
            "Uganda. The crane stands on one leg, as if the country is still moving forward.",
            "Oeganda. De kraanvogel staat op één poot, alsof het land nog vooruit stapt."),
        q("Which is one of the very few flags that show people?", "Welke is een van de weinige vlaggen waarop mensen staan?", "BLZ", "HTI GTM HND",
            "Belize: two woodcutters under a mahogany tree. It is also one of the most colourful flags.",
            "Belize: twee houthakkers onder een mahonieboom. Het is ook een van de kleurrijkste vlaggen."),
        q("Which flag shows a traditional Maasai shield and spears?", "Op welke vlag staan een traditioneel Maasai-schild en speren?", "KEN", "TZA UGA ETH",
            "Kenya. The shield stands for defending freedom.",
            "Kenia. Het schild staat voor het verdedigen van de vrijheid."),
        q("Which country flies its flag upside down in times of war?", "Welk land hangt zijn vlag in oorlogstijd ondersteboven?", "PHL", "IDN THA MYS",
            "The Philippines: in peace the blue is on top, in war the red.",
            "De Filipijnen: in vredestijd zit het blauw boven, in oorlogstijd het rood."),
        q("Which flag has a big seven-pointed star for its federation?", "Welke vlag heeft een grote zevenpuntige ster voor de federatie?", "AUS", "NZL TUV FJI",
            "Australia's Commonwealth Star: one point for each of the six states and one for the territories.",
            "De Commonwealth-ster van Australië: een punt voor elk van de zes staten en één voor de territoria."),
        q("Which flag shows the Southern Cross as four red stars?", "Welke vlag toont het Zuiderkruis als vier rode sterren?", "NZL", "AUS WSM TUV",
            "New Zealand. Australia's Southern Cross is white and has an extra small star.",
            "Nieuw-Zeeland. Het Zuiderkruis van Australië is wit en heeft een extra klein sterretje."),
        q("Which flag uses six colours, without any symbol?", "Welke vlag gebruikt zes kleuren, zonder enig symbool?", "ZAF", "NAM ZWE MOZ",
            "South Africa, since 1994. The Y shape is often explained as different paths coming together.",
            "Zuid-Afrika, sinds 1994. De Y-vorm wordt vaak uitgelegd als verschillende wegen die samenkomen."),
        q("Which flag originally had orange instead of red?", "Welke vlag had oorspronkelijk oranje in plaats van rood?", "NLD", "LUX FRA RUS",
            "The Netherlands: the 'Prince's Flag' of William of Orange. Orange dye faded quickly at sea, which is one reason red took over.",
            "Nederland: de prinsenvlag van Willem van Oranje. Oranje verf verbleekte snel op zee, en dat is een van de redenen dat rood het won."),
        q("Whose nine stripes are said to stand for the syllables of 'Freedom or Death'?", "Van welke vlag zouden de negen strepen staan voor de lettergrepen van 'Vrijheid of dood'?", "GRC", "URY ISR FIN",
            "Greece: 'E-lef-the-ri-a i Tha-na-tos'. Nine syllables, nine stripes.",
            "Griekenland: 'E-lef-the-ri-a i Tha-na-tos'. Negen lettergrepen, negen strepen."),
        q("Which flag has 14 stripes, for its states and federal territory?", "Welke vlag heeft 14 strepen, voor de deelstaten en het federale gebied?", "MYS", "USA LBR URY",
            "Malaysia. The 14-pointed star means the same: unity of the members of the federation.",
            "Maleisië. De veertienpuntige ster betekent hetzelfde: eenheid van de leden van de federatie."),
        q("Which flag has its red disc placed slightly towards the pole on purpose?", "Bij welke vlag staat de rode schijf expres iets naar de stok toe?", "BGD", "JPN LAO NER",
            "Bangladesh. When the flag flutters in the wind, the disc then looks perfectly centred.",
            "Bangladesh. Als de vlag in de wind wappert, lijkt de schijf zo precies in het midden te staan."),
        q("Which is the widest national flag in proportion (11:28)?", "Welke nationale vlag is verhoudingsgewijs het breedst (11:28)?", "QAT", "BHR ARE OMN",
            "Qatar. Its maroon colour came from red dye that darkened in the desert sun.",
            "Qatar. De kastanjebruine kleur kwam van rode verf die in de woestijnzon donkerder werd."),
        q("Whose white stripe stands for peace between green and orange?", "Van welke vlag staat de witte baan voor vrede tussen groen en oranje?", "IRL", "CIV ITA IND",
            "Ireland: green for the Catholic tradition, orange for the Protestant one, white for peace between them.",
            "Ierland: groen voor de katholieke traditie, oranje voor de protestantse, wit voor vrede daartussen."),
        q("Which flag has a wheel with 24 spokes?", "Op welke vlag staat een wiel met 24 spaken?", "IND", "NER IRL NPL",
            "India's Ashoka Chakra, the 'wheel of law'. By law the flag must be made of khadi, a hand-spun cloth.",
            "Het Ashoka-chakra van India, het 'wiel van de wet'. Volgens de wet moet de vlag van khadi zijn, met de hand gesponnen stof."),
        q("Which national flag has no red, white or blue at all?", "Welke nationale vlag heeft helemaal geen rood, wit of blauw?", "JAM", "GHA BRA GUY",
            "Jamaica: black, green and gold. It is the only one.",
            "Jamaica: zwart, groen en goud. Het is de enige."),
        q("Whose yellow star's five points stand for workers, farmers, soldiers, intellectuals and youth?", "Van welke vlag staan de vijf punten van de gele ster voor arbeiders, boeren, soldaten, intellectuelen en jongeren?", "VNM", "CHN PRK CUB",
            "Vietnam. The red background stands for the revolution.",
            "Vietnam. De rode achtergrond staat voor de revolutie."),
        q("Which flag shows a bird carved from soapstone?", "Op welke vlag staat een vogel van speksteen?", "ZWE", "MWI BWA MOZ",
            "Zimbabwe. The bird was found in the ruins of Great Zimbabwe, the city of stone the country is named after.",
            "Zimbabwe. De vogel werd gevonden in de ruïnes van Groot-Zimbabwe, de stenen stad waar het land naar genoemd is."),
    )

    /** Een aflevering: [PER_EPISODE] willekeurige vragen; de antwoorden in willekeurige volgorde. */
    fun episode(w: WorldMap, rnd: Random): List<Pair<FlagQuestion, List<String>>> =
        questions.filter { it.answer in w.countries && it.wrong.all { c -> c in w.countries } }
            .shuffled(rnd).take(PER_EPISODE)
            .map { it to (it.wrong + it.answer).shuffled(rnd) }

    fun points(correct: Int, total: Int) = correct * 10 + if (correct == total) 20 else 0
}
