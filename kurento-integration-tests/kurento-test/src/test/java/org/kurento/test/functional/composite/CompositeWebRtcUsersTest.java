/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
package org.kurento.test.functional.composite;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.Composite;
import org.kurento.client.HubPort;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.browser.WebPageType;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestScenario;

/**
 * Four synthetic videos are played by four WebRtcEndpoint and mixed by a
 * Composite. The resulting video is played in an WebRtcEndpoint <br>
 *
 * Media Pipeline(s): <br>
 * · 4xWebRtcEndpoint -> Composite -> WebRtcEndpoint <br>
 *
 * Browser(s): <br>
 * · 5 x Chrome <br>
 *
 * Test logic: <br>
 * 1. (KMS) Media server implements a grid with the media from 4 WebRtcEndpoints
 * and sends the resulting media to another WebRtcEndpoint <br>
 * 2. (Browser) WebRtcPeer in rcv-only receives media <br>
 *
 * Main assertion(s): <br>
 * · Color of the video should be the expected in the right position (grid) <br>
 *
 * Secondary assertion(s): <br>
 * · Playing event should be received in remote video tag <br>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @since 5.0.5
 */
public class CompositeWebRtcUsersTest extends FunctionalTest {

	private static final String BROWSER1 = "browser1";
	private static final String BROWSER2 = "browser2";
	private static final String BROWSER3 = "browser3";
	private static final String BROWSER4 = "browser4";
	private static final String BROWSER5 = "browser5";

	private static int PLAYTIME = 5;

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		// Test: 5 local Chrome's
		TestScenario test = new TestScenario();
		test.addBrowser(BROWSER1,
				new Browser.Builder().browserType(BrowserType.CHROME)
						.webPageType(WebPageType.WEBRTC)
						.scope(BrowserScope.LOCAL).build());
		test.addBrowser(BROWSER2, new Browser.Builder()
				.browserType(BrowserType.CHROME).webPageType(WebPageType.WEBRTC)
				.scope(BrowserScope.LOCAL)
				.video(getTestFilesPath() + "/video/10sec/red.y4m").build());
		test.addBrowser(BROWSER3, new Browser.Builder()
				.browserType(BrowserType.CHROME).webPageType(WebPageType.WEBRTC)
				.scope(BrowserScope.LOCAL)
				.video(getTestFilesPath() + "/video/10sec/green.y4m").build());
		test.addBrowser(BROWSER4, new Browser.Builder()
				.browserType(BrowserType.CHROME).webPageType(WebPageType.WEBRTC)
				.scope(BrowserScope.LOCAL)
				.video(getTestFilesPath() + "/video/10sec/blue.y4m").build());
		test.addBrowser(BROWSER5, new Browser.Builder()
				.browserType(BrowserType.CHROME).webPageType(WebPageType.WEBRTC)
				.scope(BrowserScope.LOCAL)
				.video(getTestFilesPath() + "/video/10sec/white.y4m").build());
		return Arrays.asList(new Object[][] { { test } });
	}

	@Test
	public void testCompositeWebRtcUsers() throws Exception {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEPRed = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEPGreen = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEPBlue = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEPWhite = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEPComposite = new WebRtcEndpoint.Builder(mp)
				.build();

		Composite composite = new Composite.Builder(mp).build();
		HubPort hubPort1 = new HubPort.Builder(composite).build();
		HubPort hubPort2 = new HubPort.Builder(composite).build();
		HubPort hubPort3 = new HubPort.Builder(composite).build();
		HubPort hubPort4 = new HubPort.Builder(composite).build();
		HubPort hubPort5 = new HubPort.Builder(composite).build();

		webRtcEPRed.connect(hubPort1);
		webRtcEPGreen.connect(hubPort2);
		hubPort5.connect(webRtcEPComposite);

		// Test execution
		getPage(BROWSER2).initWebRtc(webRtcEPRed, WebRtcChannel.AUDIO_AND_VIDEO,
				WebRtcMode.SEND_ONLY);
		getPage(BROWSER3).initWebRtc(webRtcEPGreen,
				WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);
		getPage(BROWSER4).initWebRtc(webRtcEPBlue,
				WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);
		getPage(BROWSER5).initWebRtc(webRtcEPWhite,
				WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);

		getPage(BROWSER1).subscribeEvents("playing");
		getPage(BROWSER1).initWebRtc(webRtcEPComposite,
				WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);

		// Assertions
		Assert.assertTrue("Not received media (timeout waiting playing event)",
				getPage(BROWSER1).waitForEvent("playing"));
		Assert.assertTrue("Left part of the video must be red",
				getPage(BROWSER1).similarColorAt(Color.RED, 0, 200));
		Assert.assertTrue("Upper right part of the video must be green",
				getPage(BROWSER1).similarColorAt(Color.GREEN, 450, 300));

		hubPort2.release();
		Thread.sleep(3000);

		Assert.assertTrue("All the video must be red",
				getPage(BROWSER1).similarColorAt(Color.RED, 300, 200));

		webRtcEPWhite.connect(hubPort4);
		Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME));

		Assert.assertTrue("Left part of the video must be red",
				getPage(BROWSER1).similarColorAt(Color.RED, 0, 300));
		Assert.assertTrue("Left part of the video must be white",
				getPage(BROWSER1).similarColorAt(Color.WHITE, 450, 300));

		hubPort4.release();
		hubPort2 = new HubPort.Builder(composite).build();
		hubPort4 = new HubPort.Builder(composite).build();

		webRtcEPGreen.connect(hubPort2);
		webRtcEPBlue.connect(hubPort3);
		webRtcEPWhite.connect(hubPort4);
		Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME));

		Assert.assertTrue("Upper left part of the video must be red",
				getPage(BROWSER1).similarColorAt(Color.RED, 0, 0));
		Assert.assertTrue("Upper right part of the video must be blue",
				getPage(BROWSER1).similarColorAt(Color.BLUE, 450, 0));
		Assert.assertTrue("Lower left part of the video must be green",
				getPage(BROWSER1).similarColorAt(Color.GREEN, 0, 450));
		Assert.assertTrue("Lower right part of the video must be white",
				getPage(BROWSER1).similarColorAt(Color.WHITE, 450, 450));

		// Release Media Pipeline
		mp.release();
	}

}
