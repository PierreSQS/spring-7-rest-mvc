# `@Builder` mit eigenem Konstruktor: Reihenfolge der Parameter

Entstanden in Section 17, Chapter 179.

## Die Regel

Hat eine Klasse `@Builder` **und** einen **selbst geschriebenen** Konstruktor mit allen Feldern, dann
müssen die Parameter dieses Konstruktors in **derselben Reihenfolge** stehen wie die **Felder in der
Klasse**.

## Warum

Lomboks Builder ruft beim `.build()` den Konstruktor mit allen Feldern auf und übergibt die Werte **in
der Reihenfolge, in der die Felder in der Klasse stehen**. Er schaut nicht auf die Namen der
Parameter, nur auf ihre Position.

## Beispiel: `BeerOrder`

`BeerOrder` braucht einen eigenen Konstruktor, weil er `setCustomer(...)` aufrufen muss (Chapter 176,
siehe `bidirectional-helper-methods.md`).

**Falsch:** Felder und Parameter in unterschiedlicher Reihenfolge

| Position | Feld in der Klasse (das übergibt der Builder) | Parameter im Konstruktor (das erwartet er) |
|---|---|---|
| 6 | `BeerOrderShipment beerOrderShipment` | `Customer customer` |
| 7 | `Customer customer` | `BeerOrderShipment beerOrderShipment` |

Der Compiler meldet dann beim `@Builder`:

```
BeerOrder.java:[31,1] Inkompatible Typen: BeerOrderShipment kann nicht in Customer konvertiert werden
```

**Richtig:** gleiche Reihenfolge, auf eine von zwei Arten

- die **Parameter** im Konstruktor umstellen, oder
- das **Feld** in der Klasse an die passende Stelle verschieben.

## Die Falle, die der Compiler nicht meldet

Der Fehler oben fällt nur auf, weil `Customer` und `BeerOrderShipment` **verschiedene Typen** sind.
Hätten zwei vertauschte Parameter **denselben Typ**, zum Beispiel `LocalDateTime createdDate` und
`LocalDateTime updateDate`, würde alles kompilieren, und der Builder würde die Werte **still
vertauschen**.

**Merksatz:** Neues Feld in einer Klasse mit `@Builder` und eigenem Konstruktor → das Feld **und** den
Parameter an **dieselbe Stelle** setzen.
