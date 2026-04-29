# Open3270Client — Java / Spring Boot Translation

## Objective

Translate the **Open3270Client** library from C# to Java, targeting compatibility with the **Spring Boot** framework.

- **Source repository:** https://github.com/Open3270/Open3270
- **Source language:** C# (.NET)
- **Target language:** Java 21 (LTS)
- **Target framework:** Spring Boot 3.x
- **Output type:** Standalone library (JAR)
- **Root package:** `com.open3270client`
- **Fork destination:** https://github.com/ivanlopezmolina

---

## Scope of Work

### 1. Repository Setup
- Fork the upstream repository (`Open3270/Open3270`) into **https://github.com/ivanlopezmolina**.
- Clone the forked repository to the local development machine.
- Create a dedicated branch (e.g., `java-port`) for all translation work.

### 2. Code Translation
- Translate all C# source files to idiomatic Java, preserving the original logic and behaviour.
- Replace C# / .NET-specific constructs with their Java equivalents:
  | C# / .NET | Java Equivalent |
  |---|---|
  | `class` / `interface` | `class` / `interface` |
  | Properties (`get`/`set`) | Getters / Setters (or Lombok `@Data`) |
  | `async` / `await` / `Task<T>` | `CompletableFuture<T>` / Project Reactor |
  | `IDisposable` | `AutoCloseable` / try-with-resources |
  | `byte[]` / `Stream` | `byte[]` / `InputStream` / `OutputStream` |
  | NuGet packages | Maven / Gradle equivalents |
- Apply Spring Boot conventions where applicable (e.g., `@Service`, `@Component`, `@Configuration`).
- This is a **standalone library (JAR)** — do not create a runnable Spring Boot application entry point. The library should be importable as a Maven dependency in any Spring Boot project.
- Use `com.open3270client` as the root package.
- Structure the project as a standard Maven single-module layout.

### 3. Build & Dependency Management
- Use **Maven** (`pom.xml`) as the build tool. Maven is the most widely adopted, best-documented, and most consistently maintained build system in the Spring Boot ecosystem. It is the default on Spring Initializr and has the largest body of community support.
- Map all NuGet dependencies to equivalent Maven Central artifacts.

### 4. Testing
- Translate or rewrite existing unit and integration tests using **JUnit 5** and **Mockito**.
- Ensure all tests pass before merging.

### 5. Documentation
- Update `README.md` to reflect the Java / Spring Boot setup, build instructions, and usage examples.
- Add inline Javadoc comments to public APIs where C# XML-doc comments existed.

---

## Workflow

```
Upstream (Open3270/Open3270)
        │
        └── Fork
              │
              └── github.com/ivanlopezmolina/Open3270
                        │
                        └── Clone to local machine
                                  │
                                  └── Branch: java-port
                                            │
                                            └── Translate → Test → PR
```

---

## Attribution & License

This project is a Java / Spring Boot port of the original **Open3270** library, which is the intellectual property of its contributors.

| Field | Details |
|---|---|
| **Original project** | [Open3270/Open3270](https://github.com/Open3270/Open3270) |
| **Original authors** | Mike Warriner ([@MikeWarriner](https://github.com/MikeWarriner)), Francois Botha ([@igitur](https://github.com/igitur)) |
| **Original language** | C# / .NET |
| **Original license** | [MIT License](https://github.com/Open3270/Open3270/blob/master/LICENSE.txt) |

### License for this port

This Java port should also be released under the **MIT License**. The original MIT license permits derivative works to be licensed under any terms, but adopting MIT is the most consistent and least restrictive choice — it automatically satisfies the original license requirements and keeps the library open for the community.

Steps to apply:
1. Create a `LICENSE` file in the root of this repository containing the standard MIT license text, with copyright holder set to **Ivanlopezmolina** and year **2026**.
2. Acknowledge the original work in the same file or in a separate `NOTICE` file.

The MIT license requires that the original copyright notice and license text be included in all copies or substantial portions of the software. Accordingly:

- Retain the original `LICENSE.txt` (or an equivalent `NOTICE` file) in the root of this repository.
- Include the following attribution in the project `README.md`:

  > This library is a Java / Spring Boot port of [Open3270](https://github.com/Open3270/Open3270), originally developed by Mike Warriner and Francois Botha and released under the MIT License.

---

## Acceptance Criteria

- [ ] All C# source files have a corresponding Java translation.
- [ ] The project builds successfully with Maven (`mvn clean install`).
- [ ] The library compiles and packages as a JAR (`mvn package`) without a runnable main class.
- [ ] Root package is `com.open3270client`.
- [ ] All existing tests have been ported and pass.
- [ ] `README.md` reflects the new Java / Spring Boot setup.
