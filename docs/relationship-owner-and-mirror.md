# Besitzer und Spiegel einer Beziehung

Entstanden in Section 17, Chapter 177.

## So erkennst du Besitzer und Spiegel

| Erkennungszeichen | Besitzer | Spiegel |
|---|---|---|
| Hat `mappedBy = "..."` | nein | ja, immer |
| Hat `@JoinTable` (many-to-many) | ja | nein |
| Hat `@JoinColumn` / eine Fremdschlüssel-Spalte (one-to-many) | ja, bei `@ManyToOne` | nein |
| Schreibt beim Speichern in die Datenbank | ja | nein, Änderungen werden ignoriert |
| Wofür man ihn benutzt | zum Verbinden und Speichern („Montag“) | für spätere Fragen von der anderen Seite („Dienstag“) |
| Braucht man ihn? | ja, sonst gibt es keine Beziehung | freiwillig, nur zum bequemen Lesen |

**Die schnellste Regel:** Steht `mappedBy` dran, ist es der Spiegel. Sonst ist es der Besitzer.

## In unserem Projekt

| Beziehung | Besitzer | Spiegel |
|---|---|---|
| Kunde ↔ Bestellung | `BeerOrder.customer` (`@ManyToOne` + `@JoinColumn`) | `Customer.beerOrders` (`mappedBy = "customer"`) |
| Bestellung ↔ Bestellposition | `BeerOrderLine.beerOrder` | `BeerOrder.beerOrderLines` (`mappedBy = "beerOrder"`) |
| Bier ↔ Bestellposition | `BeerOrderLine.beer` | `Beer.beerOrderLines` (`mappedBy = "beer"`) |
| Bier ↔ Kategorie | `Beer.categories` (`@JoinTable`) | `Category.beers`: soll `mappedBy = "categories"` werden (Fix #1) |

**Merksatz:** Bei `@ManyToOne` ↔ `@OneToMany` ist die `@ManyToOne`-Seite immer der Besitzer. Bei
`@ManyToMany` entscheidest du, und die andere Seite bekommt `mappedBy`.
