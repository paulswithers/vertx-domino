## Configuring

Configuration can be put in a .json file (e.g. "src/main/resources/application.json"). Then loaded dynamically.

### Via Maven
In pom.xml, have package phase using `maven-shade-plugin`. Set configuration to:

	<configuration>
		<transformers>
			<transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
				<manifestEntries>
					<Main-Class>io.vertx.core.Starter</Main-Class>
					<Main-Verticle>org.opencode4workspace.javasamples.DemoVerticle</Main-Verticle>
				</manifestEntries>
			</transformer>
		</transformers>
		<artifactSet />
		<outputFile>${project.build.directory}/${project.artifactId}-${project.version}-fat.jar</outputFile>
	</configuration>

Main-Verticle points to the primary verticle to load. This needs to extend `AbstractVerticle` and have a `start()` method.  
Run `mvn clean package`. The fat jar will get put in the target folder.  
Run the fat jar from the project folder, pointing to the config file - `java -jar target/Vertx-0.0.1-SNAPSHOT-fat.jar -conf src/main/resources/application.json`

### Via Eclipse
Create a class that has a `main()` method like this:

	public static void main(String[] args) throws IOException, WWException, URISyntaxException {
		DemoVerticle demo = new DemoVerticle();
		//Get file from resources folder
		ClassLoader classLoader = demo.getClass().getClassLoader();
		Path path = Paths.get(classLoader.getResource("application.json").toURI());
		StringBuilder b = new StringBuilder();
		Files.lines(path, StandardCharsets.UTF_8).forEach(b::append);
		
		JsonObject obj = new JsonObject(b.toString());
		
		Vertx vertx = Vertx.factory.vertx();
		DeploymentOptions options = new DeploymentOptions();
		options.setConfig(obj);
		vertx.deployVerticle(demo, options);
		int quit = 0;
		while (quit != 113) {
			System.out.println("Press q<Enter> to stop the verticle");
			quit = System.in.read();
		}
		System.out.println("Verticle terminated");
		System.exit(0);
	}

Run the class as a Java application.

## Proper Applications
### TODO
- Need to understand Event Bus properly
- Need to look at Authentication
- Need to get to grips with Clustering
- Need to use SessionHandler, CookieHandler, SessionStore / ClusteredSessonStore. Session timeout is 30 mins by default, can be configured.

## Database Backend - Sample Application
There's a wiki walk-through http://vertx.io/docs/guide-for-java-devs/.  GitHub repo is [here](https://github.com/vert-x3/vertx-guide-for-java-devs). It uses HSQLDB, useful for dev. Add this to pom.xml.

	<dependency>
	  <groupId>io.vertx</groupId>
	  <artifactId>vertx-jdbc-client</artifactId>
	</dependency>
	<dependency>
	  <groupId>org.hsqldb</groupId>
	  <artifactId>hsqldb</artifactId>
	  <version>2.3.4</version>
	</dependency>

Another option is PostgreSQL, details of how to use the client are http://vertx.io/docs/vertx-mysql-postgresql-client/java/. Add this to pom.xml:

	<dependency>
	  <groupId>io.vertx</groupId>
	  <artifactId>vertx-mysql-postgresql-client</artifactId>
	  <version>3.4.2</version>
	</dependency>

Can use configuration file (json).

## UI Framework
Templating seems the best option. [Thymeleaf](http://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html) is one that works well with Vertx or Spring Boot. It can have sub-templates. The demo for Thymeleaf is [here](https://github.com/thymeleaf/thymeleafexamples-gtvg).

Accessing data etc from Vertx is slightly different. It has access to the `RoutingContext` object for the request etc, see the [test template](https://github.com/vert-x3/vertx-web/blob/master/vertx-template-engines/vertx-web-templ-thymeleaf/src/test/filesystemtemplates/test-thymeleaf-template3.html). The handler can call `context.put()` to add content to that, see the [test](https://github.com/vert-x3/vertx-web/blob/master/vertx-template-engines/vertx-web-templ-thymeleaf/src/test/java/io/vertx/ext/web/templ/ThymeleafTemplateTest.java).

Controllers have a `process()` method that processes the template. The TemplateEngine has a `render()` method that puts the RoutingContext into a `context` variable in data.

From JS, looks like we need to manually do AJAX requests. Could use jQuery, but an alternative is on [youmightnotneedjquery.com](http://youmightnotneedjquery.com/).