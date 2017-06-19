package com.paulwithers.vertx;

import java.io.IOException;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import lotus.domino.NotesFactory;
import lotus.domino.NotesThread;
import lotus.domino.Session;

public class BasicDominoDemo extends AbstractVerticle {

	public static void main(String[] args) throws IOException {
		new BasicDominoDemo();
		int quit = 0;
		while (quit != 113) {
			System.out.println("Press q<Enter> to stop the verticle");
			quit = System.in.read();
		}
		System.out.println("Verticle terminated");
		System.exit(0);
	}

	private static final int listenport = 8111;

	public BasicDominoDemo() {
		Vertx vertx = Vertx.factory.vertx();
		HttpServerOptions options = new HttpServerOptions();
		options.setPort(listenport);
		vertx.createHttpServer(options).requestHandler(new Handler<HttpServerRequest>() {


			@Override
			public void handle(HttpServerRequest req) {
				HttpServerResponse resp = req.response();
				resp.headers().set("Content-Type", "text/plain charset=UTF-8");
				StringBuilder sb = new StringBuilder();
				try {
					NotesThread.sinitThread();
					Session s = NotesFactory.createSession();
					sb.append("Hello " + s.getUserName());
					NotesThread.stermThread();
				} catch (Exception e) {
					e.printStackTrace();
					sb.append(e.getMessage());
				}
				resp.end(sb.toString());
			}
		}).listen();
	}
}
