# What leaves the service, what enters it, and how it writes

Three rules the service layer of this project follows, each of them learned from something that was
wrong. They have nothing to do with one lecture: they apply to every service method that will ever be
added here.

| Rule | In short |
|---|---|
| **What leaves** | Change the DTOs, never the entities. An entity is a database row in disguise. |
| **What enters** | Validate the request body where the request arrives, not three layers down in the database. |
| **How it writes** | A read plus a write is one transaction, not two. |

---

## 1. What leaves: never modify an entity to shape a response

`GET /api/v1/beer?showInventory=false` must hide the stock figures. The straightforward way is also a
trap:

```java
// wrong: beerPage holds the entities, the rows themselves
if (showInventory != null && !showInventory) {
    beerPage.forEach(beer -> beer.setQuantityOnHand(null));
}
```

An entity is not a copy of a row, it **is** the row as Hibernate sees it. Emptying the field does not
hide a number, it erases one.

**Why the application survived it for months.** By the time that line runs, the entities are detached:
the query's session has closed, nothing watches them any more, so the change stays in memory. That is
luck, not design.

**How it becomes data loss.** Let the call happen inside an open transaction and the entities are
managed again. At commit Hibernate compares them with what it loaded, sees `122 -> null`, and writes:

```sql
update beer set quantity_on_hand = NULL where id = ...
```

A request that only wanted to *read* a list has destroyed the stock figures. One `@Transactional`
somewhere up the call chain is enough - or a `@Transactional` test, or open-in-view switched back on.

**The rule.** Map to DTOs first, then shape the DTOs:

```java
Page<BeerDTO> beerDtoPage = beerPage.map(beerMapper::beerToBeerDto);

if (showInventory != null && !showInventory) {
    beerDtoPage.forEach(beerDto -> beerDto.setQuantityOnHand(null));
}

return beerDtoPage;
```

**Pinned by** `BeerControllerIT.testListBeersWithoutInventoryKeepsTheStockInTheDatabase`. It runs
`@Transactional` on purpose, so the beers stay managed, and it checks the entities after the call. With
the old line restored it fails on exactly the right thing:

```
Expecting all elements of: ... Expecting actual not to be null
```

## 2. What enters: validate the request body, not the database

The beer endpoints annotate the body with `@Validated`, so a bad request comes back as `400` with a list
of `{field: message}`. The customer endpoints did neither, and `CustomerDTO` carried no constraints. A
PUT with a blank name therefore travelled all the way to the database, failed there against the entity's
`@NotBlank`, and `CustomErrorController` turned the `TransactionSystemException` into a **bare 400 with
an empty body** - a client could not tell what was wrong.

```java
public class CustomerDTO {

    /** Checked on POST and PUT, which carry a whole customer; a PATCH carries only what it changes. */
    @NotBlank
    private String name;

    @Email
    private String email;
```

**POST and PUT are validated, PATCH is not, on purpose.** PATCH carries only the fields it changes, so
its body legitimately has a `null` name; demanding one there would make partial updates impossible. The
entity keeps its own `@NotBlank` as the last line of defence - the DTO check is about answering the
client properly, not about replacing the database constraint.

**Pinned by** three tests in `CustomerControllerTest`: a blank name on PUT, a blank name on POST and a
malformed email each answer `400` with one field error.

## 3. How it writes: one transaction for the read and the write

Every update and patch does two things - find the entity, then save it. Without `@Transactional` each
step runs in its own transaction, so the entity is detached in between and Hibernate has to reload the
row before it can tell what changed.

Measured on the running application, one `PATCH /api/v1/beer/{id}` with `hibernate.show_sql` on:

**Without `@Transactional` - three statements**

```sql
select b1_0.id, b1_0.beer_name, b1_0.beer_style, ... from beer b1_0 where b1_0.id=?
select b1_0.id, b1_0.beer_name, b1_0.beer_style, ... from beer b1_0 where b1_0.id=?
update beer set beer_name=?, beer_style=?, price=?, quantity_on_hand=?, upc=?, update_date=?, version=? where id=? and version=?
```

**With `@Transactional` - two statements**

```sql
select b1_0.id, b1_0.beer_name, b1_0.beer_style, ... from beer b1_0 where b1_0.id=?
update beer set beer_name=?, beer_style=?, price=?, quantity_on_hand=?, upc=?, update_date=?, version=? where id=? and version=?
```

The second `select` is the reload. It disappears because the entity never leaves the transaction, so
Hibernate still knows its original state and writes the `update` directly.

Applied to the four write methods that read before they write: `updateBeerById`, `patchBeerById`,
`updateCustomerById`, `patchCustomerById`. `saveNewBeer` and `deleteById` need nothing - they are single
operations, and Spring Data's own methods are transactional already.

`where id=? and version=?` in both runs shows optimistic locking is untouched by the change.

**How to repeat the measurement** (nothing committed, nothing configured):

```bash
./mvnw.cmd package -DskipTests
java -jar target/spring-7-rest-mvc-0.0.1-SNAPSHOT.jar --server.port=8086 \
     --spring.jpa.properties.hibernate.show_sql=true --spring.jpa.properties.hibernate.format_sql=false
# then PATCH a beer and count the "Hibernate:" lines in the output
```

A `@Transactional` **test** cannot show this difference: the test's own transaction wraps both steps, so
the reload never happens there either. The measurement has to run against the real application.
