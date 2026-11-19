# Workshop Registration System

EX1 (tag `EX1-Vanilla`) was a plain Java implementation.
EX2 (tag `EX2-Maven-Evolution`) moves it into a Maven multi-module project.

## Structure

```
root                  aggregator + top parent (all versions live here)
|-- core              aggregator + parent of the implementation modules
|   |-- model-impl    Person, Instructor, Student, Workshop, Team
|   `-- logic-impl    WorkshopSystemImpl, JSON export, CSV import
`-- app               workshop-cli: Main + the uber jar workshop-cli.jar
```

## One-time setup: install the given jars into the local repository

The API jars and the tester are not on Maven Central. Run these commands once,
from the folder that holds `model-2.0.jar`, `logic-2.0.jar` and `workshop-tester-2.0.jar`:

```
mvn install:install-file -Dfile=model-2.0.jar -DgroupId=patmal.workshops -DartifactId=model -Dversion=2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=logic-2.0.jar -DgroupId=patmal.workshops -DartifactId=logic -Dversion=2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=workshop-tester-2.0.jar -DgroupId=patmal.workshops -DartifactId=workshop-tester -Dversion=2.0 -Dpackaging=jar -DgeneratePom=true
```

`-DgeneratePom=true` is needed because the jars carry a pom inside them that points to a
parent (`workshops-aggregator`) we don't have. Without it the installed pom is broken.

## Build

```
mvn clean install
```

This builds all modules, creates `app/target/workshop-cli.jar` and, in the `verify` phase,
runs the tester against the build. A failing tester fails the build.

## Run

```
java -jar app/target/workshop-cli.jar                 # uses the students.csv packed in the jar
java -jar app/target/workshop-cli.jar students.csv    # uses your own CSV file
```

The application writes `instructors.json`, `students.json` and `teams.json` to the current folder.

Run the tester by hand (Windows uses `;` instead of `:`):

```
java -cp "workshop-tester-2.0.jar:app/target/workshop-cli.jar" patmal.workshops.main.Main
```

## Libraries

| Library | Module | Scope | Used for |
|---|---|---|---|
| log4j-api (Log4j 2, via log4j-bom) | logic-impl, app | compile | logging calls |
| log4j-core | app | runtime | logging implementation, configured in `app/src/main/resources/log4j2.xml` |
| commons-validator | logic-impl | compile | e-mail validation |
| jackson-databind | logic-impl | compile | JSON export |
| commons-csv | logic-impl | compile | CSV import (bonus) |
| workshop-tester | app | test | runs the tester in the build (bonus), not packed into the jar |
