# Lodestar — Android-app

Topografie leren: Nederland en Europa, in de donkere *Nachtkaart*-stijl.
Gebouwd met Kotlin en Jetpack Compose.

Dit is het **fundament**: startscherm met kaart, menu per regio, voortgang, instellingen,
opslag en navigatie. Het quizscherm zelf is de volgende stap.

## Op je telefoon zetten (Linux Mint + Fairphone)

### 1. Android Studio installeren

Kies één van de twee:

- **Via Softwarebeheer** (makkelijkst): zoek op *Android Studio* en installeer de Flatpak-versie.
- **Via de terminal:**
  ```bash
  flatpak install flathub com.google.AndroidStudio
  ```

Start Android Studio en loop de installatiewizard door met de standaardkeuzes. Hij downloadt
dan zelf de Android SDK. Dat duurt even.

### 2. Het project openen

1. Pak `topo-android.zip` uit, bijvoorbeeld in `~/Projecten/topo-android`.
2. In Android Studio: **Open** en kies die map.
3. Wacht tot onderin *Gradle sync* klaar is. De eerste keer haalt hij alle bibliotheken op
   (een paar minuten).

### 3. Je Fairphone klaarmaken

1. **Instellingen → Over de telefoon** → tik 7 keer op **Buildnummer**. Je bent nu ontwikkelaar.
2. **Instellingen → Systeem → Ontwikkelaarsopties** → zet **USB-foutopsporing** aan.
3. Sluit de telefoon met USB aan op je laptop en tik op de telefoon op **Toestaan**.

Ziet Android Studio je telefoon niet? Installeer dan de USB-regels voor Android en
sluit de telefoon opnieuw aan:

```bash
sudo apt install android-sdk-platform-tools-common
```

### 4. Starten

Kies bovenin je Fairphone in de apparatenlijst en druk op de groene **▶ Run**.
De app verschijnt als **Topo** tussen je apps.

**Alternatief zonder kabel:** *Build → Build App Bundle(s) / APK(s) → Build APK(s)*.
Het bestand staat daarna in `app/build/outputs/apk/debug/app-debug.apk`. Zet het op je
telefoon en open het (sta "installeren uit onbekende bron" toe).

### Lukt iets niet?

Kopieer de foutmelding uit het **Build**-venster onderin Android Studio en plak hem in
het gesprek met Claude. Dan lossen we het samen op.

## Wat zit erin

| Scherm | Wat het doet |
| --- | --- |
| **Kaart** (start) | Europakaart. Tik op Nederland of op een ander land om een regio te kiezen. Onderin: voortgang van die regio, *Kies onderwerp* en *Verder spelen*. |
| **Regio-opties** | Tabel zoals in de quiz-app: onderwerpen in rijen, niveaus ★ tot ★★★★ in kolommen. Elk vakje toont je beste score. |
| **Quiz** | Nog een tijdelijk scherm met testknoppen (45%, 75%, 100%) om de voortgang uit te proberen. |
| **Voortgang** | Reeks (dagen op rij), aantal rondes, gekende vakjes, balkjes per niveau en wat je nog moet oefenen. |
| **Instellingen** | Rondelengte, namen op de kaart, geluid, trillen, voortgang wissen, bronnen. |

## Projectstructuur

```
app/src/main/
├── assets/geo/          kaartdata (europe.json, nederland.json)
├── res/font/            Young Serif (titels) en Figtree (tekst)
└── java/nl/topo/app/
    ├── data/
    │   ├── content/     regio's, onderwerpen, niveaus  ← hier voeg je onderwerpen toe
    │   ├── geo/         kaart inlezen, projectie, "welk land is hier"
    │   └── prefs/       instellingen en voortgang (DataStore)
    └── ui/
        ├── theme/       Nachtkaart-kleuren en lettertypen
        ├── components/  sterrenhemel, gloeiende kaart, ringen, score-vakjes
        ├── home/        startscherm met kaart
        ├── region/      optietabel per regio
        ├── progress/    voortgangsscherm
        ├── settings/    instellingen
        └── quiz/        (volgende stap)
```

Een nieuw scherm voeg je toe in `ui/TopoApp.kt` (zie `Routes`).
Een nieuwe regio of nieuw onderwerp voeg je toe in `data/content/Content.kt`.

## Bronnen en licenties

- Kaarten: [Natural Earth](https://www.naturalearthdata.com) (publiek domein), CBS-gebiedsindelingen via [cartomap/nl](https://github.com/cartomap/nl)
- Landgegevens: [mledoze/countries](https://github.com/mledoze/countries) (ODbL)
- Lettertypen: Young Serif en Figtree, SIL Open Font License (zie `assets/`)

## Online ranglijst (Firebase) — nog aan te zetten

De app bewaart nu alle uitslagen van de dagelijkse uitdaging lokaal (`OfflineLeaderboard`)
en zet ze in een wachtrij. Zodra Firebase aan staat, gaan ze automatisch naar de online lijst.
De schermen gebruiken alleen de interface `LeaderboardService` (`data/online/Leaderboard.kt`),
dus alleen de implementatie hoeft te wisselen.

1. Maak een project op https://console.firebase.google.com en voeg een Android-app toe met
   package `nl.topo.app`. Vul de SHA-1 van `app/topo-debug.keystore` in
   (`keytool -list -v -keystore app/topo-debug.keystore -storepass android`).
2. Download `google-services.json` en zet die in `app/`.
3. Zet **Authentication → Anonymous** aan en maak een **Firestore**-database (productiemodus).
4. Voeg de plugin `com.google.gms.google-services` en de dependencies
   `firebase-bom`, `firebase-auth`, `firebase-firestore` toe.
5. Schrijf `FirebaseLeaderboard : LeaderboardService`:
   - documenten `scores/{day}_{difficulty}_{uid}` met de velden van `ScoreSubmission`;
   - `ranking(DAY)`: query op `day` + `difficulty`, gesorteerd op `points` desc, `timeMs` asc, limit 50;
   - `ranking(WEEK)`: query `day >= weekStart`, optellen per speler;
   - `flush()`: de wachtrij uit `online/pending` versturen en leegmaken.
6. Vervang in `TopoApplication.kt` `OfflineLeaderboard(...)` door `FirebaseLeaderboard(...)`.

Firestore-regels (alleen je eigen score, één keer, met grenzen):

```
rules_version = '2';
service cloud.firestore {
  match /databases/{db}/documents {
    match /scores/{id} {
      allow read: if request.auth != null;
      allow create: if request.auth != null
        && id == string(request.resource.data.day) + '_' + request.resource.data.difficulty + '_' + request.auth.uid
        && request.resource.data.points is int && request.resource.data.points >= 0 && request.resource.data.points <= 2000
        && request.resource.data.nickname is string && request.resource.data.nickname.size() <= 20;
      allow delete: if request.auth != null && resource.data.playerId == request.auth.uid;
    }
  }
}
```
