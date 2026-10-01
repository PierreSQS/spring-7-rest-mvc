# Zwei Welten: Java und Datenbank

Entstanden in Section 17, Chapter 180, bei der Besprechung der One-to-One-Beziehung Bestellung ↔
Sendung. Siehe auch `relationship-owner-and-mirror.md`.

## Worum es geht

**Ziel:** zu zeigen, dass es zwei Welten gibt, Java und Datenbank, und dass die Helper-Methode in der
Java-Welt hilft.

**Ausgangslage:** Eine Beziehung mit einem Besitzer und einem Spiegel. In der **Datenbank** steht die
Verbindung nur **einmal**, zum Beispiel in `beer_order.beer_order_shipment_id`. In **Java** gibt es aber
weiterhin **zwei Felder**:

- `order.beerOrderShipment`, der Besitzer;
- `shipment.beerOrder`, der Spiegel.

## Was passieren kann, wenn man nur eines setzt

```java
order.beerOrderShipment = shipment;    // nur der Besitzer wird gesetzt
// shipment.beerOrder bleibt null
```

| Welt | Was gilt | Stimmt es? |
|---|---|---|
| Java, im Speicher | `order` kennt `shipment`, aber `shipment` kennt `order` nicht | ✘ nicht einheitlich |
| Datenbank, nach dem Speichern | die Spalte `beer_order_shipment_id` = s1 | ✔ korrekt, denn gespeichert wird nur der Besitzer |

Die Datenbank ist also in Ordnung. Nur im Java-Code, solange die Objekte im Speicher sind, kennt die
Sendung ihre Bestellung nicht. Wer dann `shipment.getBeerOrder()` fragt, bekommt `null`.

## Was die Helper-Methode daran ändert

```java
order.setBeerOrderShipment(shipment);  // setzt BEIDE Felder in Java
```

Jetzt stimmen beide Welten: Java (beide Felder gesetzt) und die Datenbank (eine Spalte).

**Merksatz:** Mit einem Spiegel ist die Datenbank immer richtig. Die Helper-Methode sorgt dafür, dass auch
Java richtig ist.
