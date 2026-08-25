
## Two xml files (/server/src/main/resources/..)

- application.xml
- application-dev.xml

--- 

## application.xml 

```
spring:
  profiles:
    active: dev
  application:
    name: quicktix
```

- during deployment: We don't need to make 'profiles' as 'prod' 
- Just (spring.profile.active: PROD) at .env file at deployment 
- so we could push (profiles.active.dev) on github

---

## application-dev.xml

```
server:
  port: 8080

spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/quicktix_db?options=-c%20timezone%3DUTC}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    open-in-view: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          time_zone: UTC
```

- anything here needs not be chamged - we just have to put those values inside the .env file while deploying 
- the `${... : ...}` field is important
- `username: ${DB_USERNAME:postgres}` means if the .env file have a custom 'DB_USERNAME', use that - otherwise use 'postgres'

---


