# Welcome

*Open3270 provides a high level API to connect to mainframe 3270 sessions from a .NET application.*

## Key features include

* Support for TN3270 and TN3270E from all Microsoft .NET applications (VB.NET and C#) 
* Support for formatted and unformatted screens 
* Support for multiple LUs and connections that require a specific LU to be specified 
* Support for unlimited TN3270 connections from a single application 
* Support for ASP.NET and WinForms applications
* 100% Managed Microsoft .NET 3270 emulation
* Support for multiple LUs, formatted and unformatted screens and most 3270 "bugs".
* Coded entirely in C#, with overlapped I/O replacing the frequently used multi-threaded server approach to 3270 connectivity, this library is extremely efficient and lightweight, suitable both for WinForms applications as well as ASP.NET server applications

## Install via NuGet

    Install-Package Open3270

---

## Java Port

A complete Java 17 Maven port of this library is available under `src/main/java/com/open3270/`.

### Requirements

* Java 17+
* Maven 3.6+

### Build

```bash
mvn compile
mvn test
```

### Quick start

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

### Spring Boot autoconfiguration

Add the jar to your Spring Boot application. Properties prefix: `open3270`:

```yaml
open3270:
  host: mainframe.example.com
  port: 23
  term-type: IBM-3278-2-E
  use-ssl: false
  connect-timeout-ms: 5000
```
