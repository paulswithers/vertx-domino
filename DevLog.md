# Vert.x

**Requires Java 8, so needs Domino server or Notes Client**

## Basic Notes Verticle

### pom.xml
- Added ODA repo
- Added vertx-core dependency

### Classpath
Added notes.jar from \domino\jvm\lib\ext

Can then run a basic Verticle to output current username, by right-clicking BasicCoreDominoDemo.java and choosing Run As > Java Application

Can also be run from vertx\bin folder in npm using `vertx start com.paulwithers.vertx.BasicDominoDemo -cp "C:\Users\withersp\GitRepositories\Intec Internal\vertx\Vertx-Domino\target\vertx-domino-0.0.1-SNAPSHOT.jar";"C:\Program Files\IBM\Domino\jvm\lib\ext\Notes.jar" --vertx-id domino`
Use `vertx stop domino` to end it.

Can also be run from command line via `java -cp "C:\Program Files\IBM\Domino\jvm\lib\ext\Notes.jar";target\vertx-domino-0.0.1-SNAPSHOT-fat.jar com.paulwithers.vertx.BasicDominoDemo`.

Running the jars doesn't get classpath correctly, so you need to use `-cp` to specify the classpath. `java -jar` overrides the `cp` switch - a known Java gotcha.