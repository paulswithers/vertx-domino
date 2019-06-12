package com.paulwithers.vertx;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class BasicTest {

	private Vertx vertx;
	private Integer port;

	@Before
	public void setUp(TestContext context) throws IOException {
		vertx = Vertx.vertx();

		// Let's configure the verticle to listen on the 'test' port (randomly picked).
		// We create deployment options and set the _configuration_ json object:
		ServerSocket socket = new ServerSocket(0);
		port = socket.getLocalPort();
		System.out.println("Port is " + port);
		socket.close();

		DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("http.port", 8119));

		vertx.deployVerticle(BasicDominoDemo.class.getName(), options, context.asyncAssertSuccess());
	}

	@After
	public void tearDown(TestContext context) {
		vertx.close(context.asyncAssertSuccess());
	}

	@Test
	public void testMyApplication(TestContext context) {
		final Async async = context.async();

		vertx.createHttpClient().getNow(8119, "localhost", "/", response -> {
			response.handler(body -> {
				context.assertTrue(body.toString().contains("Intec"));
				async.complete();
			});
		});
	}

}
