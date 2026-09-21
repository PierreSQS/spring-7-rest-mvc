# Who generates the entity IDs?

**The application does, never the database.** `@UuidGenerator` creates the UUID in Java, and MySQL/H2 only store it.

## Evidence from this project

**1. The insert already contains `id`**

```sql
insert into customer (created_date,email,name,update_date,version,id) values (?,?,?,?,?,?)
```

`id` is in the column list with a value bound to it: Hibernate knows the ID before the statement is sent. If the database generated it, `id` would be missing here and the database would fill it in.

**2. The column has no generation rule** (`V1__init-mysql-database.sql`)

```sql
id varchar(36) not null,
primary key (id)
```

A text column and a primary key, nothing else. No `AUTO_INCREMENT`, no `DEFAULT (uuid())`. MySQL could not invent a value here; an insert without `id` would fail.

**3. The mapping names who does it** (`Beer`, `Customer`)

```java
@Id
@UuidGenerator
@Column(length = 36)
@JdbcTypeCode(SqlTypes.VARCHAR)
private UUID id;
```

`@UuidGenerator` is Hibernate's generator, running in Java. On `save()` it produces a random UUID (RFC 4122 version 4) and sets it on the object; the insert follows later.

## The two arrangements compared

|                        | This project: app-generated UUID | Classic DB-generated ID          |
| ---------------------- | -------------------------------- | -------------------------------- |
| Mapping                | `@Id @UuidGenerator`             | `@Id @GeneratedValue(strategy = IDENTITY)` |
| Column                 | `varchar(36)`                    | `bigint AUTO_INCREMENT`          |
| Who invents the value  | Hibernate, in Java               | the database, during the insert  |
| When the ID exists     | right after `save()`, before any SQL | only after the insert has run |
| Insert statement       | includes `id`                    | omits `id`, then reads it back   |

## What happens on save

1. `repository.save(entity)` - Hibernate calls `@UuidGenerator`; the object has its UUID immediately.
2. The insert is queued in the persistence context. **No SQL yet.**
3. `flush()`, `saveAndFlush()` or a commit sends the insert, `id` included, written as `varchar(36)` because of `@JdbcTypeCode(SqlTypes.VARCHAR)`.
4. Under `@DataJpaTest` the transaction is rolled back afterwards, so the row disappears - but the SQL did run and the constraints were checked.

## Why this matters in tests

`CustomerRepositoryTest.testSaveCustomer` used to call `save()` only:

```java
Customer customer = customerRepository.save(Customer.builder().name("New Name").build());
assertThat(customer.getId()).isNotNull();
```

It passed while **no `insert into customer` ever appeared in the log**. The ID was set in Java at step 1, and the queued insert was discarded when the test transaction rolled back. The test proved only that the UUID generator had run - not that the database accepted the row.

With an `IDENTITY` column this could not happen: Hibernate would have to execute the insert immediately just to learn the ID.

The fix is to force the write, which is what both repository tests do now:

```java
Customer customer = customerRepository.saveAndFlush(Customer.builder().name("New Name").build());
```

The transaction is still rolled back, so nothing is left behind, but the insert is genuinely sent and column limits, `NOT NULL` and validation are checked.

## Related: the timestamps work the same way

`createdDate` and `updateDate` are filled by Hibernate's `@CreationTimestamp` and `@UpdateTimestamp` - also in Java, not by MySQL. Do not set them by hand.
