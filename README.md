# POKEMON API

A simple Pokemon application managing user login, and displaying pokemon information upon research by name.

## FrontEnd

Using Tyrian, it must be compiled with `sbt frontend/fastLinkJS` - it generates the `main.js` file fetched by the `resources/index.html` file. 

## backend
 To launch the backend, a mysql DB must be running. 
 ```
 cd mysql/
 docker compose up -d
 ```
 
 Make configuration change to `backend/src/main/resources/config.yaml` file if needed, then launch `sub backend/run`. 
 
Everything should be set.

## Default DB

There's no subscribe implemented so, the users have to be inserted by hand in the DB.

```
mysql -u root -h 127.0.0.1
use pokemon; # should exist already after sbt backend/run
source mysql/create.sql;
```
- if launched with docker compose, the `-h` with the real ip is mandatory (localhost does not work)

## Application

Connect to http://localhost:8080
- default user: 'alice', password: 'pass'


