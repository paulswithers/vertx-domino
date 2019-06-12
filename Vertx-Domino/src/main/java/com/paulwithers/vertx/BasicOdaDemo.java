package com.paulwithers.vertx;

import java.io.IOException;

import org.openntf.domino.Database;
import org.openntf.domino.Document;
import org.openntf.domino.View;
import org.openntf.domino.thread.AbstractDominoRunnable;
import org.openntf.domino.thread.DominoExecutor;
import org.openntf.domino.utils.Factory;
import org.openntf.domino.xots.Tasklet;

import com.paulwithers.vertxWorlds.XWorldsManagedThread;
import com.paulwithers.vertxWorlds.XWorldsManager;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

public class BasicOdaDemo extends AbstractVerticle {
	private static final int listenport = 8113;
	private DominoExecutor de = null;
	private boolean _started = false;

	public static void main(String[] args) throws IOException {
		XWorldsManager.getInstance().Startup();
		// 11-07-17 SHOULD THIS BE IN A WORKER VERTICLE, IN THE start() METHOD? TRIGGERED VIA vertx.deployVerticle()??
		// See
		// https://github.com/vert-x3/vertx-examples/blob/master/core-examples/src/main/java/io/vertx/example/core/verticle/worker/MainVerticle.java
		// and
		// https://github.com/vert-x3/vertx-examples/blob/master/core-examples/src/main/java/io/vertx/example/core/verticle/worker/WorkerVerticle.java
		DominoExecutor de = new DominoExecutor(10);
		new BasicOdaDemo(de);
		int quit = 0;
		while (quit != 113) {
			System.out.println("Press q<Enter> to stop the verticle");
			quit = System.in.read();
		}
		XWorldsManager.getInstance().Shutdown();
		System.out.println("Verticle terminated");
		System.exit(0);
	}

	/**
	 * Identifies if the XWorldsManager has been started or not for this server
	 * 
	 * @return boolean whether or not XWorlds is started
	 */
	public boolean isStarted() {
		return _started;
	}

	public BasicOdaDemo(DominoExecutor de) {
		this.de = de;
		Vertx vertx = Vertx.factory.vertx();
		HttpServerOptions options = new HttpServerOptions();
		options.setPort(listenport);
		vertx.createHttpServer(options).requestHandler(new Handler<HttpServerRequest>() {

			@Override
			public void handle(HttpServerRequest req) {
				try {

					HttpServerResponse resp = req.response();

					XWorldsManagedThread.setupAsDominoThread(req);
					de.execute(new VertxRunnable(resp));
				} catch (Exception e) {
					// TODO: handle exception
				} finally {
					XWorldsManagedThread.shutdownDominoThread();
				}
			}
		}).listen();
	}

	@Tasklet(session = Tasklet.Session.CLONE)
	private class VertxRunnable extends AbstractDominoRunnable {
		private static final long serialVersionUID = 1L;
		private HttpServerResponse resp;

		public VertxRunnable(HttpServerResponse resp) {
			this.resp = resp;
		}

		@Override
		public void run() {
			StringBuilder txt = new StringBuilder();
			resp.headers().set("Content-Type", "text/html; charset=UTF-8");
			txt.append("<html><body><h1>Hello from vert.x</h1>");
			txt.append(Factory.getSession().getEffectiveUserName());
			txt.append("<br/>");
			Database db = Factory.getSession().getDatabase("names.nsf");
			View vw = db.getView("($Users)");
			Document doc = vw.getFirstDocument();
			txt.append(doc.getItemValueString("FullName"));
			txt.append("</body></html>");
			resp.end(txt.toString());
		}

		@Override
		public boolean shouldStop() {
			return false;
		}
	}

}
