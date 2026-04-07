# SupportDesk

This repository contains a Spring Boot-based customer support desk application.

## Build

Use the included Maven wrapper to build the project:

```bash
./mvnw.cmd -DskipTests=true compile
```

## Run

Start the application:

```bash
./mvnw.cmd spring-boot:run
```

Logs are configured with Log4j2 using `src/main/resources/log4j2.xml`. By default, application logs for `com.support.desk` are printed to the console at INFO level.

## Notes

- I added Log4j2 logging statements to `AuthService`, `UserService`, and `TicketService` to improve observability around create/update/delete and exception handling.
- If you'd like a different logging format or file-based logging, I can add a file appender and environment-based configuration.
