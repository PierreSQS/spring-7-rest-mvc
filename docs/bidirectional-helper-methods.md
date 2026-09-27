# Die Helper-Methode `setCustomer(...)`: warum und was sie bewirkt

Übersicht zu Section 17, Chapter 176. Wie es zu dem Problem kam (lazy Listen, flush, `save` gegen
`saveAndFlush`), steht in `lazy-collections-and-flush.md`.

## Die Schritte in einem Satz

1. **Das Problem:** Hibernate pflegt nur `order.customer` (die Spalte `customer_id`). Die Liste des
   Kunden musst du selbst pflegen.
2. **Die Lösung:** `setCustomer(...)` setzt beide Seiten auf einmal: die Bestellung zeigt auf den Kunden,
   **und** der Kunde bekommt die Bestellung in seine Liste.
3. **Der Haken:** Der Builder benutzt den Konstruktor, nicht `setCustomer(...)`. Mit Lomboks
   `@AllArgsConstructor` ging er an der Helper-Methode vorbei.
4. **Die Lösung dafür:** Ein eigener Konstruktor ruft `this.setCustomer(customer)` auf. So setzt auch der
   Builder beide Seiten.
5. **Leere Liste statt `null`:** Jeder Kunde startet mit einer leeren Liste
   (`@Builder.Default ... = new HashSet<>()`), damit `setCustomer(...)` immer etwas zum Hineinlegen hat.
6. **Kein Kunde:** `if (customer != null)`. Ohne Kunden gibt es keine Liste, also wird Seite 2
   übersprungen.
7. **Im Test:** Die Bestellung kommt **beim Bauen** in Java in die Liste des Kunden, nicht über die
   Datenbank. Deshalb reicht `save()`.
8. **Die Folgen:** Das Problem ist für Kunde ↔ Bestellung gelöst; es kostet aber das Laden aller
   Bestellungen des Kunden, und beim Kundenwechsel wird der alte Kunde vergessen.

## Übersicht

| Schritt | Was | Wo | Warum | Ohne das … |
|---|---|---|---|---|
| 1 | **Das Problem:** Hibernate pflegt nur `order.customer` (die Spalte `customer_id`) | – | die Liste des Kunden ist nur ein Spiegel | kennt der Kunde seine neue Bestellung in Java nicht |
| 2 | **`setCustomer(...)`** setzt beide Seiten | `BeerOrder` | nie nur eine Seite vergessen | musst du die Liste des Kunden immer selbst füllen |
| 3–4 | **Eigener Konstruktor** ruft `setCustomer(...)` auf | `BeerOrder`, statt `@AllArgsConstructor` | der Builder benutzt den Konstruktor | umgeht der Builder deine Methode |
| 5 | **Leere Liste als Startwert:** `@Builder.Default ... = new HashSet<>()` | `Customer.beerOrders` | `.add(...)` braucht eine Liste | wirft ein neuer Kunde eine `NullPointerException` |
| 6 | **`if (customer != null)`** | `setCustomer` | Bestellung ohne Kunden erlauben | stürzt `BeerOrder.builder().build()` ab (wie bei JT) |
| 7 | **Test mit `save()`** | `BeerOrderRepositoryTest` | die Bestellung liegt schon in Java in der Liste | nichts, `saveAndFlush` war nur ein Umweg |

## Folgen

| Folge | Art | Kurz |
|---|---|---|
| Beide Seiten stimmen sofort in Java | ✔ gut | egal ob `save` oder `saveAndFlush` |
| `save()` statt `saveAndFlush()` | ✔ günstiger beim **Schreiben** | das `insert` kommt gesammelt am Ende (JTs Punkt) |
| `.add(order)` lädt alle Bestellungen des Kunden | ⚠ Kosten beim **Lesen** | Thema für das Ende des Kurses |
| Kunde wechseln trägt beim alten Kunden nicht aus | ⚠ Lücke | in Java steht die Bestellung dann bei beiden |
| Nur Kunde ↔ Bestellung hat einen Helper | ⚠ Lücke | Bestellposition ↔ Bestellung / Bier noch ohne |

## Schreiben gegen Lesen: wer hat recht?

Beide, JT und diese Seite, sprechen von **verschiedenen Kosten**:

- **JT, Schreiben:** `save()` lässt Hibernate das `insert` später und gesammelt machen; `saveAndFlush()`
  schreibt sofort. `save()` ist günstiger.
- **Diese Seite, Lesen:** `.add(order)` öffnet die lazy Liste des Kunden und lädt dabei seine bisherigen
  Bestellungen (`select ... from beer_order where customer_id = ?`).

Aus den Test-Logs:

| Version | `insert` während des Tests | `select` der Kundenbestellungen |
|---|---|---|
| Chapter 175-b: `saveAndFlush`, ohne Helper | 1, sofort | 1 |
| Chapter 176: `save` + Helper | 0 (erst am Ende; im Test wegen Rollback nie) | 1, ausgelöst durch `.add(order)` |

**Diese Seite beschreibt den Stand von Chapter 176.** Ändert sich die Helper-Methode oder kommen weitere
dazu, muss sie angepasst werden.
