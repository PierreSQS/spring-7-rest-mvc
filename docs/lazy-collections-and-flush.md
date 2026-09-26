# Warum der Kunde seine neue Bestellung (nicht) kennt

Erklärung zu `BeerOrderRepositoryTest.testBeerOrders`, entstanden in Section 17, Chapter 175. Der Test
wurde in zwei Varianten ausgeführt:

| Commit | Variante | Speichern mit | Ergebnis |
|---|---|---|---|
| `a402631e` | `Sec17_Chap175-a` | `beerOrderRepo.save(order)` | Kunde kennt die Bestellung **nicht** |
| `e7958ffa` | `Sec17_Chap175-b` | `beerOrderRepo.saveAndFlush(order)` | Kunde kennt die Bestellung |

Der Test fragt jedes Mal dasselbe:

```java
assertThat(savedOrder.getCustomer().getBeerOrders()).contains(savedOrder);
```

Um den Unterschied zu verstehen, braucht man drei Begriffe: **lazy**, **die Frage an die Datenbank** und
**flush**.

---

## 1. Lazy: erst laden, wenn man es braucht

„Lazy“ heißt **faul**: Hibernate lädt etwas erst, wenn man es wirklich braucht.

Wird ein Kunde geladen, holt Hibernate **nur den Kunden**, nicht seine Bestellungen. Statt der
Bestellliste bekommt man eine **leere Schachtel mit einem Zettel**: „Bei Bedarf aus der Datenbank
holen“.

```
customerRepo.findAll()            →  select ... from customer
                                     (keine Bestellungen)

... später, beim ersten Hineinschauen in die Liste:
                                  →  select ... from beer_order where customer_id = ?
```

- Beim **ersten Öffnen** der Schachtel holt Hibernate den Inhalt.
- Danach bleibt der Inhalt in der Schachtel und wird **nie wieder** geholt.

**Warum so?** Ein Kunde kann Tausende Bestellungen haben. Die will man nicht jedes Mal mitladen, wenn
man nur seinen Namen braucht.

In diesem Projekt sind alle drei Listen lazy: `Customer.beerOrders`, `BeerOrder.beerOrderLines` und
`Beer.beerOrderLines` (das ist der Standard für `@OneToMany`).

---

## 2. Die Liste des Kunden ist eine Frage, keine gespeicherte Liste

In der Datenbank gibt es **keine Bestellliste beim Kunden**. Die Tabelle `customer` hat keine Spalte
„orders“. Die Verbindung steht **nur bei der Bestellung**:

```
Tabelle customer            Tabelle beer_order
| id | name       |         | id | customer_id |
| c1 | Customer 1 |         | o1 | c1          |   ← nur hier steht: "o1 gehört zu c1"
```

Die Bestellliste des Kunden ist also die **Antwort auf eine Frage** an die Datenbank:

> „Welche Bestellungen haben `customer_id = c1`?“

Im Java-Code sieht man das an den zwei Seiten der Beziehung:

| Seite | Code | Rolle |
|---|---|---|
| Bestellung → Kunde | `BeerOrder.customer` mit `@ManyToOne` | **schreibt** die Spalte `customer_id` |
| Kunde → Bestellungen | `Customer.beerOrders` mit `@OneToMany(mappedBy = "customer")` | nur ein **Spiegel**: wird beim Laden mit der Antwort auf die Frage gefüllt |

Hibernate aktualisiert den Spiegel im Speicher **nie von selbst**. Wer eine Bestellung speichert, ändert
die Liste des Kunden in Java nicht.

---

## 3. Flush: den Merkzettel in die Datenbank schreiben

`save()` schreibt **nicht sofort** in die Datenbank. Hibernate legt die Änderung auf einen
**Merkzettel** und schreibt sie später, spätestens am Ende der Transaktion.

**Flush** heißt: den Merkzettel **jetzt** in die Datenbank schreiben.

| Methode | Was passiert |
|---|---|
| `save(order)` | Bestellung auf den Merkzettel, **kein** `insert` |
| `saveAndFlush(order)` | Bestellung auf den Merkzettel **und sofort** `insert into beer_order ...` |

Wichtig: Das Öffnen der lazy Liste (die Frage aus Abschnitt 2) löst **keinen** Flush aus. Hibernate
fragt die Datenbank, ohne vorher den Merkzettel zu schreiben.

---

## 4. Schritt für Schritt: wann wird die Frage gestellt?

Die Frage wird **nicht** bei `getBeerOrders()` gestellt. `getBeerOrders()` gibt nur die Schachtel zurück.
Die Frage kommt beim **ersten Hineinschauen**: `.contains(...)`, `.size()`, eine Schleife darüber usw.
Im Test macht das AssertJ in `.contains(savedOrder)`.

### Variante `-a` mit `save()`

| Schritt | Zeile im Test | In der Datenbank |
|---|---|---|
| 1 | `setUp`: `customerRepo.findAll()` | `select ... from customer`: Kunde geladen, seine Liste ist eine geschlossene Schachtel |
| 2 | `BeerOrder.builder()...build()` | nichts, nur ein Java-Objekt |
| 3 | `beerOrderRepo.save(order)` | **nichts**: Bestellung nur auf dem Merkzettel |
| 4 | `...getBeerOrders()` | nichts, nur die Schachtel in der Hand |
| 5 | `.contains(savedOrder)` | Schachtel wird geöffnet → **Frage**: `select ... from beer_order where customer_id = ?` → Tabelle noch leer → **Antwort leer** |
| 6 | Testende | Rollback: der Merkzettel wird verworfen, es gab **nie** ein `insert` |

### Variante `-b` mit `saveAndFlush()`

| Schritt | Zeile im Test | In der Datenbank |
|---|---|---|
| 1–2 | wie oben | wie oben |
| 3 | `beerOrderRepo.saveAndFlush(order)` | **`insert into beer_order ...` sofort** |
| 4 | `...getBeerOrders()` | nichts |
| 5 | `.contains(savedOrder)` | Schachtel wird geöffnet → **Frage** → die Zeile ist da → **Antwort enthält die Bestellung** |
| 6 | Testende | Rollback: das `insert` wird rückgängig gemacht |

**Der einzige Unterschied ist Schritt 3:** Steht die Zeile schon in der Datenbank, wenn in Schritt 5
gefragt wird?

---

## 5. Warum Variante `-b` zerbrechlich ist

Hibernate stellt die Frage nur **beim ersten Öffnen** der Schachtel. Öffnet man sie **vor** dem
Speichern, wird sie mit der Antwort „leer“ gefüllt und danach **nie wieder** gefragt.

Ausprobiert am 25.09.2026 (nicht committet): in Variante `-b` vor dem Speichern eine Zeile eingefügt:

```java
log.info("### orders before save: {}", testCustomer.getBeerOrders().size());
```

Ergebnis:

```
select ... from beer_order where customer_id = ?   ← Schachtel geöffnet: leer
### orders before save: 0
insert into beer_order ...                          ← saveAndFlush schreibt
→ Test rot                                          ← die Schachtel wird nicht neu gefüllt
```

`saveAndFlush()` macht den Test also nur grün, weil die Schachtel **zufällig erst nach** dem `insert`
geöffnet wird.

---

## 6. Die saubere Lösung (nächste Lektion)

Man verlässt sich nicht auf die Reihenfolge, sondern setzt beim Verknüpfen **beide Seiten selbst in
Java**: Bestellung → Kunde **und** Kunde → Bestellung. JT zeigt das in der nächsten Lektion mit
*Helper-Methoden*. So steht es in seinem `BeerOrder` (Branch `104-rel-helper-methods`):

```java
public void setCustomer(Customer customer) {
    this.customer = customer;
    customer.getBeerOrders().add(this);   // den Spiegel selbst aktualisieren
}
```

Dann kennt der Kunde seine Bestellung **sofort**, egal ob `save` oder `saveAndFlush`, und egal wann die
Schachtel geöffnet wird.

**Diese Seite beschreibt den Stand von Chapter 175.** Wenn die Helper-Methoden eingebaut sind, muss
Abschnitt 6 an den dann aktuellen Code angepasst werden.
