package com.paulwithers.vertxWorlds;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.openntf.domino.thread.DominoExecutor;
import org.openntf.domino.utils.Factory;

import io.vertx.core.spi.launcher.Command;
import lotus.domino.NotesException;
import lotus.domino.NotesFactory;
import lotus.domino.NotesThread;

/*

<!--
Copyright 2016 Daniele Vistalli, Paul Withers

import java.lang.Thread.State;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.openntf.domino.utils.Factory;
import org.openntf.domino.xots.Xots;
import org.openntf.xworlds.core.Command;

import lotus.domino.NotesException;
import lotus.domino.NotesFactory;
import lotus.domino.NotesThread;

/**
 * @author Daniele Vistalli
 * @since 1.0.0
 * 
 *        This Manager class acts as an appserver-wide management service to
 *        ensure ODA services are just started once.<br>
 *        <br>
 *        The application listener embedded in every XWorlds enabled application
 *        will startup the engine by calling XWorldsManager.startup()<br>
 *        <br>
 *        The startup code will initialize application tracking code to list all
 *        enabled applications and to manage monitoring and service reporting
 *        and will:
 *        <ul>
 *        <li>Setup a map of initialized "servletContexts" using XWorlds
 *        services
 *        <li>Startup Factory.startup() if not already done globally (managing
 *        concurrency)
 *        <li>Startup a "locker" NotesThread to ensure domino is loaded for the
 *        whole execution of the application server. This will eventually be
 *        replaced by a service similar to ODA's Xots to submit batches. Ideally
 *        XWolds should also come to support Java Batch extensions as supported
 *        by WAS Libery.
 *        </ul>
 *        </br>
 *        The application listener will even manage XWorldsManager.shutdown() to
 *        reduce the number of loaded instances and to update monitoring<br>
 *
 */
public class XWorldsManager {

	private static Logger log = Logger.getLogger(XWorldsManager.class.getName());
	private static XWorldsManager _instance = null;

	private boolean _started = false;
	private boolean _napiStarted = false;

	private AtomicInteger xwmStartedCount = new AtomicInteger(0);
	private AtomicInteger xwmRunningContextsCount = new AtomicInteger(0);

	private int xotsTasks = 10;
	private int xotsStopDelay = 15;

	lotus.domino.NotesThread NotesLockerThread = null;

	/**
	 * Get or creates an instance of this class
	 * 
	 * @return XWorldsManager
	 */
	public static XWorldsManager getInstance() {

		synchronized (XWorldsManager.class) {
			if (_instance == null) {
				_instance = new XWorldsManager();
			}
		}

		return _instance;
	}

	/**
	 * Identifies if the XWorldsManager has been started or not for this server
	 * 
	 * @return boolean whether or not XWorlds is started
	 */
	public boolean isStarted() {
		return _started;
	}

	/**
	 * Starts XWorlds
	 */
	public void Startup() {

		if (_started == true) {
			System.out.println("Cannot call XWorldsManager.Startup() if already running.");
		}
		if (Factory.isStarted()) {
			System.out.println("ODA Factory already started, something is wrong");
		}

		// Start OpenNTF Domino API Factory

		log.info("XWorlds:Manager - Starting manager");

		// Synchronize ODA Factory initialization
		synchronized (Factory.class) {

			if (!Factory.isStarted()) {

				Factory.startup();
				NotesThread.sinitThread();

				String userIdPassword = System.getProperty("xworlds.userid.password");

				if (userIdPassword != null) {
					log.info("XWorlds:Manager - Opening UserID for this system");
					try {
						// Open the id if the password is specified
						NotesFactory.createSession((String) null, (String) null, userIdPassword);
					} catch (NotesException e) {
						e.printStackTrace();
					}
				}

				log.info("XWorlds:Manager - ODA has started for: " + Factory.getLocalServerName());
				log.fine("XWorlds:Manager - Initializing nApi");

				log.info("XWorlds:Manager - Starting system Domino thread");

				NotesLockerThread = new NotesThread(new Runnable() {
					@Override
					public void run() {
						boolean stopped = false;
						log.info("XWorlds:Manager - Domino thread started");
						NotesThread.sinitThread();

						while (!stopped) {
							try {
								Thread.sleep(4000);
							} catch (InterruptedException e) {
								log.info("XWorlds:Manager - Interrupted, shutting dowm system Domino thread.");
								NotesThread.stermThread();
								stopped = true;
							}
						}

					}
				}, "XWorlds System Thread");

				NotesLockerThread.setDaemon(true);
				NotesLockerThread.start();

				NotesThread.stermThread();

				log.info("XWorlds:Manager - Starting XOTS");
				DominoExecutor de = new DominoExecutor(xotsTasks);

				_started = true;

			} else {
				log.severe("ODA's Factory failed to start");
			}

		}

		xwmStartedCount.incrementAndGet();

	}

	/**
	 * Outputs XWorlds statistics, called from OSGi command in
	 * {@linkplain Command#report()}
	 * 
	 * @return String report of XWorlds statistics
	 */
	public String getXWorldsReportAsString() {

		if (_started == false) {
			System.out.println("XWorldsManger should bestarted to provide a report.");
		}

		StringBuilder report = new StringBuilder();

		report.append("Started environments count: ");
		report.append(xwmStartedCount.get());
		report.append("\n");

		report.append("Running application contexts: ");
		report.append(xwmRunningContextsCount.get());
		report.append("\n");

		report.append("Active object count: " + Factory.getActiveObjectCount() + "\n");
		report.append("Auto recycle object count: " + Factory.getAutoRecycleCount() + "\n");
		report.append("Factory counters dump:\n" + Factory.dumpCounters(true) + "\n");

		return report.toString();
	}

	/**
	 * Shut down XWorlds
	 */
	public void Shutdown() {

		if (_started == false) {
			System.out.println("XWorldsManager.Shutdown() must not be called if not started");
		}

		if (xwmStartedCount.decrementAndGet() == 0 && _started == true) {

			// On startedCount = 0 shutdown everything
			NotesLockerThread.interrupt();

			int secs = 0;
			while (NotesLockerThread.getState() != Thread.State.TERMINATED && secs < 10) {
				secs++;
				log.fine("Waiting for domino system thread to terminate [" + NotesLockerThread.getState() + "]");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				}
			}

			log.info("Shutting down ODA Factory");
			if (Factory.isStarted()) {
				log.fine("Shutting down ODA on thread " + Thread.currentThread().getId() + " / "
						+ Thread.currentThread().getName());
				Factory.shutdown();
			}

			_started = false;
		}

	}

}
