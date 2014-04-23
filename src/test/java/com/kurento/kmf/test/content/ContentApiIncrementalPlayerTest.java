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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
import com.kurento.kmf.test.client.Client;
import com.kurento.kmf.test.client.EventListener;
import com.kurento.kmf.test.client.VideoTagBrowser;

/**
 * Test of a HTTP Player with incremental connection strategies:
 * <ul>
 * <li>N incremental, each X seconds and with a hold time of 10 minutes.</li>
 * <li>When N is reached, connect and disconnect elements from the pipeline</li>
 * </ul>
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class ContentApiIncrementalPlayerTest extends ContentApiTest {

	private static final String HANDLER = "/incrementalPlayer";
	private static final int NCLIENTS = 5;

	@HttpPlayerService(path = HANDLER, redirect = true, useControlProtocol = false)
	public static class PlayerRedirect extends HttpPlayerHandler {

		private static MediaPipeline mp;
		private static PlayerEndpoint playerEP;

		@Override
		public synchronized void onContentRequest(HttpPlayerSession session)
				throws Exception {
			if (mp == null) {
				mp = session.getMediaPipelineFactory().create();
				playerEP = mp.newPlayerEndpoint(
						"http://ci.kurento.com/video/sintel.webm").build();
				playerEP.play();
			}
			HttpGetEndpoint httpEP = mp.newHttpGetEndpoint().terminateOnEOS()
					.build();
			playerEP.connect(httpEP);
			session.start(httpEP);
		}
	}

	@Test
	public void testIncrementalPlayer() throws InterruptedException, ExecutionException {
		ExecutorService exec = Executors.newFixedThreadPool(NCLIENTS);

		List<Future<Boolean>> results = new ArrayList<>();
		for (int i = 0; i < NCLIENTS; i++) {
			results.add(exec.submit(new Callable<Boolean>() {
				public Boolean call() throws InterruptedException {
					return createPlayer();
				}
			}));
			Thread.sleep(2000);
		}

		boolean result = true;

		for (Future<Boolean> r : results) {
			result &= r.get();
		}
		Assert.assertTrue(result);
	}

	private boolean createPlayer() throws InterruptedException {
		final CountDownLatch startEvent = new CountDownLatch(1);
		final CountDownLatch terminationEvent = new CountDownLatch(1);

		try (VideoTagBrowser vtb = new VideoTagBrowser(getServerPort(),
				Browser.CHROME, Client.PLAYER)) {

			vtb.setURL(HANDLER);
			vtb.addEventListener("playing", new EventListener() {
				@Override
				public void onEvent(String event) {
					log.info("*** playing ***");
					startEvent.countDown();
				}
			});
			vtb.addEventListener("ended", new EventListener() {
				@Override
				public void onEvent(String event) {
					log.info("*** ended ***");
					terminationEvent.countDown();
				}
			});
			vtb.start();

			return terminationEvent.await(TIMEOUT, TimeUnit.SECONDS);
		}
	}
}
