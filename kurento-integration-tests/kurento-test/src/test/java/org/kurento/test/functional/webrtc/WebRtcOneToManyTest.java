/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package org.kurento.test.functional.webrtc;

import static org.kurento.commons.PropertiesManager.getProperty;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.browser.WebPageType;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.BrowserConfig;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestScenario;
import org.kurento.test.latency.LatencyController;
import org.kurento.test.monitor.SystemMonitorManager;

/**
 * WebRTC one to many test. <br>
 *
 * Media Pipeline(s): <br>
 * · WebRtcEndpoint -> N X WebRtcEndpoint <br>
 *
 * Browser(s): <br>
 * · Chrome <br>
 * · Firefox <br>
 *
 * Test logic: <br>
 * 1. (KMS) WebRtcEndpoint presenter connected to N viewers<br>
 * 2. (Browser) 1 WebRtcPeer in send-only sends media. N WebRtcPeer in rcv-only
 * receives media <br>
 *
 * Main assertion(s): <br>
 * · Playing event should be received in remote video tag of the viewers<br>
 *
 * Secondary assertion(s): <br>
 * -- <br>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class WebRtcOneToManyTest extends FunctionalTest {

	private static final int PLAYTIME = 40; // seconds
	private static final int DEFAULT_NUM_VIEWERS = 3;
	private static int numViewers;
	private SystemMonitorManager monitor;

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		numViewers = getProperty("webrtc.one2many.numviewers",
				DEFAULT_NUM_VIEWERS);

		// Test: 1 presenter + N viewers (all local Chrome's)
		TestScenario test = new TestScenario();
		test.addBrowser(BrowserConfig.PRESENTER, new Browser.Builder()
				.webPageType(WebPageType.WEBRTC).browserType(BrowserType.CHROME)
				.scope(BrowserScope.LOCAL)
				.video(getTestFilesPath() + "/video/15sec/rgbHD.y4m").build());
		test.addBrowser(BrowserConfig.VIEWER, new Browser.Builder()
				.webPageType(WebPageType.WEBRTC).browserType(BrowserType.CHROME)
				.scope(BrowserScope.LOCAL).numInstances(numViewers).build());
		return Arrays.asList(new Object[][] { { test } });
	}

	@Before
	public void setupMonitor() {
		// monitor = new SystemMonitorManager();
		// monitor.start();
	}

	@After
	public void teardownMonitor() {
		// monitor.stop();
		// monitor.writeResults(getDefaultOutputFile("-monitor.csv"));
		// monitor.destroy();
	}

	@Test
	public void testWebRtcOneToManyChrome()
			throws InterruptedException, IOException {
		// Media Pipeline
		final MediaPipeline mp = kurentoClient.createMediaPipeline();
		final WebRtcEndpoint masterWebRtcEP = new WebRtcEndpoint.Builder(mp)
				.build();

		// Assets for viewers
		final LatencyController[] cs = new LatencyController[numViewers];
		final WebRtcEndpoint[] viewerWebRtcEPs = new WebRtcEndpoint[numViewers];
		final CountDownLatch latch = new CountDownLatch(numViewers);

		// Presenter
		getPresenter().subscribeLocalEvents("playing");
		getPresenter().initWebRtc(masterWebRtcEP, WebRtcChannel.VIDEO_ONLY,
				WebRtcMode.SEND_ONLY);

		// monitor.addWebRtcClientAndActivateOutboundStats(
		// getPresenter().getBrowser().getId(), masterWebRtcEP,
		// getPresenter(), "webRtcPeer.peerConnection");

		// Viewers
		ExecutorService exec = Executors.newFixedThreadPool(numViewers);
		for (int j = 0; j < numViewers; j++) {
			final int i = j;
			Thread thread = new Thread() {
				public void run() {
					try {
						viewerWebRtcEPs[i] = new WebRtcEndpoint.Builder(mp)
								.build();
						masterWebRtcEP.connect(viewerWebRtcEPs[i]);
						if (monitor != null) {
							monitor.incrementNumClients();
						}

						// Latency control
						String name = getViewer(i).getBrowser().getId();
						cs[i] = new LatencyController(name, monitor);

						// WebRTC
						getViewer(i).subscribeEvents("playing");
						getViewer(i).initWebRtc(viewerWebRtcEPs[i],
								WebRtcChannel.VIDEO_ONLY, WebRtcMode.RCV_ONLY);
						if (monitor != null) {
							monitor.addWebRtcClientAndActivateInboundStats(
									getViewer(i).getBrowser().getId(),
									viewerWebRtcEPs[i], getViewer(i),
									"webRtcPeer.peerConnection");
						}

						// Latency assessment
						cs[i].checkRemoteLatency(PLAYTIME, TimeUnit.SECONDS,
								getPresenter(), getViewer(i));
						cs[i].drawChart(getDefaultOutputFile(
								"-" + name + "-latency.png"), 500, 270);
						cs[i].writeCsv(getDefaultOutputFile(
								"-" + name + "-latency.csv"));
						cs[i].logLatencyErrorrs();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						latch.countDown();
						if (monitor != null) {
							monitor.decrementNumClients();
						}
					}
				}
			};
			exec.execute(thread);
		}

		// Wait to finish viewers threads
		latch.await();

		// Release Media Pipeline
		mp.release();
	}
}
