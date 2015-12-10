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
package org.kurento.test.functional.dispatcher;

import java.awt.Color;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.Dispatcher;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.HubPort;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;

/**
 * A PlayerEndpoint is connected to a WebRtcEndpoint through a Dispatcher <br>
 *
 * Media Pipeline(s): <br>
 * · 2xPlayerEndpoint -> Dispatcher -> WebRtcEndpoint <br>
 *
 * Browser(s): <br>
 * · Chrome <br>
 * · Firefox <br>
 *
 * Test logic: <br>
 * 1. (KMS) Media server switchs the media from two PlayerEndpoint using a
 * Dispatcher, streaming the result through a WebRtcEndpoint<br>
 * 2. (Browser) WebRtcPeer in rcv-only receives media <br>
 *
 * Main assertion(s): <br>
 * · Playing event should be received in remote video tag <br>
 * · The color of the received video should be as expected (red and the blue)
 * <br>
 * · EOS event should arrive to player <br>
 * · Play time in remote video should be as expected <br>
 *
 * Secondary assertion(s): <br>
 * -- <br>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class DispatcherPlayerTest extends FunctionalTest {

	private static final int PLAYTIME = 10; // seconds

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return TestScenario.localChromeAndFirefox();
	}

	@Test
	public void testDispatcherPlayer() throws Exception {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();

		PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/10sec/red.webm").build();
		PlayerEndpoint playerEP2 = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/10sec/blue.webm").build();
		WebRtcEndpoint webRtcEP = new WebRtcEndpoint.Builder(mp).build();

		Dispatcher dispatcher = new Dispatcher.Builder(mp).build();
		HubPort hubPort1 = new HubPort.Builder(dispatcher).build();
		HubPort hubPort2 = new HubPort.Builder(dispatcher).build();
		HubPort hubPort3 = new HubPort.Builder(dispatcher).build();

		playerEP.connect(hubPort1);
		playerEP2.connect(hubPort3);
		hubPort2.connect(webRtcEP);
		dispatcher.connect(hubPort1, hubPort2);

		final CountDownLatch eosLatch = new CountDownLatch(1);
		playerEP2.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
			@Override
			public void onEvent(EndOfStreamEvent event) {
				eosLatch.countDown();
			}
		});

		// Test execution
		getPage().subscribeEvents("playing");
		getPage().initWebRtc(webRtcEP, WebRtcChannel.AUDIO_AND_VIDEO,
				WebRtcMode.RCV_ONLY);
		playerEP.play();

		// Assertions
		Assert.assertTrue("Not received media (timeout waiting playing event)",
				getPage().waitForEvent("playing"));
		Assert.assertTrue("The color of the video should be red",
				getPage().similarColor(Color.RED));

		Thread.sleep(5000);
		playerEP2.play();
		dispatcher.connect(hubPort3, hubPort2);
		Assert.assertTrue("The color of the video should be blue",
				getPage().similarColor(Color.BLUE));

		Assert.assertTrue("Not received EOS event in player",
				eosLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));
		double currentTime = getPage().getCurrentTime();
		Assert.assertTrue(
				"Error in play time (expected: " + PLAYTIME + " sec, real: "
						+ currentTime + " sec)",
				getPage().compare(PLAYTIME, currentTime));

		// Release Media Pipeline
		mp.release();
	}
}
