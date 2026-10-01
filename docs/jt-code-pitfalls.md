# Fehler und Schwächen in JTs Code (Section 17)

Gefunden beim Durcharbeiten von Section 17. Jeder Punkt wurde mit einem Test oder dem SQL-Log
nachgewiesen. Die Lehre daraus: nicht vertrauen, sondern beweisen.

| Wo | Was bei JT steht | Art |
|---|---|---|
| Chapter 177, `Beer` + `Category` | `@JoinTable` auf **beiden** Seiten | **Fehler**: mit beiden Seiten verbunden → Absturz (Primärschlüssel-Verletzung) |
| Chapter 177, `Category` | `equals`/`hashCode` mit `super.equals` | **Fehler**: vergleicht nur Objekt-Identität |
| Chapter 178, `removeCategory` | `category.getBeers().remove(category)` | **Fehler**: entfernt das Falsche, der Spiegel wird nie aufgeräumt |
| Chapter 173, `isNew()` | Methode ohne `Persistable` | **nutzlos**: wird nie aufgerufen |
| Chapter 176/180, Helper | ohne `null`-Prüfung | **Absturz** bei Bestellung ohne Kunden oder Sendung |
| Chapter 179/180, One-to-One | zwei Besitzer, zwei Spalten | **fragwürdiges Design**: Kreis, Zusatz-`update`, Widerspruch möglich |

**Fazit:** Die ersten drei sind klare Fehler, keine Vereinfachungen für den Kurs. JT zeigt keinen Test,
der diese Fälle prüft; deshalb fallen sie bei ihm nicht auf.
