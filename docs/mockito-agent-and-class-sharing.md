# Why the tests start the JVM with `-javaagent` and `-Xshare:off`

`pom.xml` configures Surefire like this:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>-javaagent:${org.mockito:mockito-core:jar} -Xshare:off</argLine>
    </configuration>
</plugin>
```

Both flags exist to remove warnings that appeared on every test run under Java 25. Neither changes how the tests behave.

## Part 1: the Mockito agent

### What Mockito needs

Mocking means replacing a class's behaviour at runtime. To do that, Mockito has to rewrite classes as they are loaded, and the JVM only allows that to a **Java agent** - a library the JVM loads at startup for exactly this purpose, with `-javaagent:<path-to-jar>`.

### What it was doing instead

Started without that flag, Mockito attached itself to the already-running JVM ("self-attaching"), which is why every test run printed:

```
Mockito is currently self-attaching to enable the inline-mock-maker.
This will no longer work in future releases of the JDK.
```

Newer JDKs are closing that door: a program attaching an agent to itself is exactly how an attacker would inject code, so the JVM now warns and will eventually refuse. The fix is to hand Mockito over at startup, the intended way.

### How it is wired up

```xml
<argLine>-javaagent:${org.mockito:mockito-core:jar}</argLine>
```

`${org.mockito:mockito-core:jar}` is a placeholder for "the path to that dependency's jar on this machine". Two things are needed to make it resolve, and without either one the literal text is passed to `java` and **every test fails to start**:

1. **The `dependency:properties` goal**, which defines the `${groupId:artifactId:type}` properties. Surefire does not resolve them by itself.

   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-dependency-plugin</artifactId>
       <executions>
           <execution>
               <goals><goal>properties</goal></goals>
           </execution>
       </executions>
   </plugin>
   ```

2. **`mockito-core` declared as a test dependency** (the version still comes from the Spring Boot parent). It arrives transitively through the test starters, but a placeholder only resolves for a declared dependency.

Maven's debug output (`mvnw -X test`) shows the resolved value:

```
(s) argLine = -javaagent:C:\Users\...\.m2\repository\org\mockito\mockito-core\5.23.0\mockito-core-5.23.0.jar
```

## Part 2: `-Xshare:off`

### What class data sharing is

Every JVM start has to load thousands of core classes (`String`, `List`, ...). To avoid re-parsing them each time, the JDK ships a ready-made memory image of those classes - Class Data Sharing, or CDS. The JVM maps that file into memory instead of reading class files, which makes startup slightly faster and saves memory across JVMs.

### Why it complained

That image is only valid if the set of core classes is exactly what the JVM expects. The Mockito agent is appended to the bootstrap classpath, so the image no longer matches, and the JVM reduced what it shares and said so:

```
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes
because bootstrap classpath has been appended
```

Nothing was broken. It was the JVM reporting that it could not fully use its startup shortcut.

### What the flag does

`-Xshare:off` disables CDS explicitly. With the shortcut switched off there is no mismatch to report, so the message disappears. The cost is the small startup benefit that the agent had already spoiled: a full `clean test` run still takes about the same time as before (~41s on this machine).

## Result

| Warning | Cause | Fix |
| --- | --- | --- |
| `Mockito is currently self-attaching` | no agent given at startup | `-javaagent:` + `dependency:properties` + declared `mockito-core` |
| `Sharing is only supported for boot loader classes` | the agent invalidates the CDS archive | `-Xshare:off` |

Test runs are now free of JVM and Mockito warnings, with all tests passing.
