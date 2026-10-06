# Geladene, gebaute und abgelöste Objekte - und warum `setVersion(...)` nichts schützt

Entstanden in Section 18, Chapter 186 ("Optimistic Locking Demo").

## 1. Das Problem: zwei Leute ändern dasselbe Bier

| Zeit | Anna | Ben | Datenbank |
|---|---|---|---|
| 10:00 | öffnet "Galaxy Cat", Version 0 | öffnet "Galaxy Cat", Version 0 | Version 0 |
| 10:01 | speichert einen neuen Namen | | Version 1, Annas Name |
| 10:02 | | speichert, ausgehend von Version 0 | **soll abgelehnt werden** |

Wird Ben angenommen, überschreibt er Annas Änderung, ohne es zu merken: ein **"lost update"**.

## 2. Die Versionsprüfung

`@Version private Integer version;` lässt Hibernate jedes Update so schreiben:

```sql
update beer set ..., version = 2 where id = ? and version = 1
```

Passt die Version in der Datenbank nicht mehr, ändert sich keine Zeile, und Hibernate wirft
`ObjectOptimisticLockingFailureException`.

## 3. Woher kommt das Objekt, das man `save(...)` gibt?

Entscheidend ist, **welches Objekt man `save(...)` übergibt**, nicht was `save(...)` zurückgibt (die
Prüfung passiert innerhalb von `save`).

| Das Objekt kommt aus … | Wir nennen es | Hibernate-Begriff | Hat Hibernate ein "Foto"? |
|---|---|---|---|
| `beerRepository.findById(...)` | **geladen** | *managed* (verwaltet) | **ja** |
| `beerMapper.beerDtoToBeer(...)`, mit der id eines bestehenden Biers | **gebaut** | *detached* (abgelöst) | nein |
| `Beer.builder()...build()` für ein ganz neues Bier | neu | *transient* | nein |

**Das "Foto":** Beim Laden hält Hibernate in seinem Speicher (dem *Persistenzkontext*) eine Kopie der
Zeile, wie sie in der Datenbank stand. Die sieht man im Code nicht.

**Ein abgelöstes (*detached*) Objekt** steht also für ein Bier, das es in der Datenbank gibt, das
Hibernate aber gerade **nicht** überwacht: kein Foto. Dazu kommt es, wenn man es aus Daten baut (wie aus
dem DTO), wenn die Transaktion vorbei ist, oder nach `entityManager.clear()`.

## 4. Warum `foundBeer.setVersion(beer.getVersion())` nichts schützt

`BeerServiceJPA.updateBeerById`, Bens PUT (Ben schickt Version 0, die Datenbank hat Version 1):

| Zeile | Code | Objekt `foundBeer` | Hibernates Foto |
|---|---|---|---|
| 110 | `beerRepository.findById(beerId)` | Version 1 | **Version 1** (Foto gemacht) |
| 115 | `foundBeer.setVersion(beer.getVersion())` | Version 0 | Version 1 |
| 117 | `beerRepository.save(foundBeer)` | | prüft mit dem **Foto**: `where version = 1` → angenommen |

Die Prüfung nimmt die Version aus dem **Foto**, und das Foto ist gerade erst in Zeile 110 gemacht worden,
also immer aktuell. `setVersion(...)` ändert nur das Objekt, nicht das Foto.

Gemessen mit dem Demo-Test `BeerControllerIT.testUpdateBeerBadVersion` (ohne `@Disabled`):

```
### after PUT 1: status=204 dto.version=0 db.version=1 db.name=Updated Name
### after PUT 2: status=204 dto.version=0 db.version=2 db.name=Updated Name 2
```

Ben kommt mit Version 0, wird trotzdem angenommen, und sein Name überschreibt Annas.

## 5. Die Lösung (geplant, noch nicht umgesetzt)

`findById` behalten und die Versionen **selbst vergleichen**:

```java
return beerRepository.findById(beerId).map(foundBeer -> {
    // der Client ging von einer anderen Version aus als der in der Datenbank
    if (!Objects.equals(beer.getVersion(), foundBeer.getVersion())) {
        throw new ObjectOptimisticLockingFailureException(Beer.class, beerId);
    }
    foundBeer.setBeerName(beer.getBeerName());
    ...                                    // ohne setVersion
```

Dazu im `CustomErrorController` die Ausnahme in **`409 Conflict`** übersetzen, und im Demo-Test beim
zweiten PUT `409` statt `204` erwarten.

(JTs Alternative in der Lektion, `beerRepository.save(beerMapper.beerDtoToBeer(beer))`, lehnt Ben zwar ab,
verliert aber den 404 für unbekannte Biere und löscht die Kategorien des Biers. Ausprobiert am
06.10.2026; Details zu einem späteren Zeitpunkt.)

**Diese Seite beschreibt den Stand von Chapter 186.** Wenn Abschnitt 5 umgesetzt ist, muss er angepasst
werden.
