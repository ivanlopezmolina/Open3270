# Open3270 Java Client Library

A standalone Java 21 Maven library for connecting to IBM mainframe 3270 sessions using
the TN3270 and TN3270E protocols.  Ready for use in **Spring Boot 3.x** applications.

## Credits

This library is a Java translation of the original **Open3270** project:

| | |
|---|---|
| **Original Authors** | Michael Warriner and contributors |
| **Original Copyright** | Copyright (c) 2004-2020 Michael Warriner |
| **Original Repository** | https://github.com/Open3270/Open3270 |
| **Original License** | MIT License |

> The original Open3270 library is a C# implementation of the TN3270/TN3270E protocol
> and was the authoritative source for this Java port.

## License

MIT License — Copyright (c) 2026 ivanlopezmolina  
(See [LICENSE.txt](LICENSE.txt) for full text.)

## Key features

* TN3270 and TN3270E protocol support
* Formatted and unformatted screen support
* Multiple LU / connection support
* Spring Boot 3.x autoconfiguration (`open3270.*` properties)
* Java 21, Maven build, zero external runtime dependencies beyond SLF4J

## Requirements

* Java 21+
* Maven 3.6+

## Build

```bash
mvn compile
mvn test
```

## Maven dependency

```xml
<dependency>
    <groupId>io.github.ivanlopezmolina</groupId>
    <artifactId>open3270client</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick start

```java
try (TNEmulator em = new TNEmulator()) {
    em.connect("mainframe.example.com", 23);
    if (em.waitForConnect(5000)) {
        System.out.println(em.getScreen().dump());
        em.sendEnter();
        em.waitForUnlock(5000);
        System.out.println(em.getText(1, 1, 80)); // row 1, col 1, 80 chars
    }
}
```

## Spring Boot autoconfiguration

Add the jar to your Spring Boot 3.x application.  Properties prefix: `open3270`:

```yaml
open3270:
  host: mainframe.example.com
  port: 23
  term-type: IBM-3278-2-E
  use-ssl: false
  connect-timeout-ms: 5000
```

A `ConnectionConfig` bean is registered automatically.  Use it to build a
`TNEmulator` wherever you need a connection:

```java
@Autowired
private ConnectionConfig open3270ConnectionConfig;

public void run() throws Exception {
    try (TNEmulator em = new TNEmulator(open3270ConnectionConfig)) {
        em.connect(open3270ConnectionConfig.getHostName(),
                   open3270ConnectionConfig.getHostPort());
        // ...
    }
}
```
