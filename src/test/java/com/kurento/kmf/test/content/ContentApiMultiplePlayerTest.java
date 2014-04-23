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
package com.kurento.kmf.test.content;

import org.junit.Assert;
import org.junit.Test;

import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.test.base.ContentApiTest;
import com.kurento.kmf.test.client.Browser;
import com.kurento.kmf.test.client.BrowserClient;
import com.kurento.kmf.test.client.Client;

/**
 * Test of multiple HTTP Players, using directly a MediaPipeline and HttpClient.
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class ContentApiMultiplePlayerTest extends ContentApiTest {

	private static final String HANDLER = "/player";

	@HttpPlayerService(path = HANDLER, redirect = true, useControlProtocol = false)
	public static class PlayerRedirect extends HttpPlayerHandler {

		private PlayerEndpoint playerEP;

		@Override
		public void onContentRequest(HttpPlayerSession session)
				throws Exception {
			MediaPipeline mp = session.getMediaPipelineFactory().create();
			session.releaseOnTerminate(mp);
			playerEP = mp.newPlayerEndpoint(
					"http://ci.kurento.com/video/small.webm").build();
			HttpGetEndpoint httpEP = mp.newHttpGetEndpoint().terminateOnEOS()
					.build();
			playerEP.connect(httpEP);
			session.start(httpEP);
		}

		@Override
		public void onContentStarted(HttpPlayerSession contentSession)
				throws Exception {
			playerEP.play();
		}
	}

	@Test
	public void testPlayerMultiple() throws InterruptedException {
		for (int i = 0; i < 2; i++) {
			try (BrowserClient browser = new BrowserClient(getServerPort(),
					Browser.CHROME, Client.PLAYER)) {
				browser.setURL(HANDLER);
				browser.subscribeEvents("playing", "ended");
				browser.start();

				// Assertions
				Assert.assertTrue(browser.waitForEvent("playing"));
				Assert.assertTrue(browser.waitForEvent("ended"));
			}
		}
	}

}
