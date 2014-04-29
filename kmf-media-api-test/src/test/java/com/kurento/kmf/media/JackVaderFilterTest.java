/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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
package com.kurento.kmf.media;

import static com.kurento.kmf.media.SyncMediaServerTest.URL_SMALL;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.MediaEventListener;

/**
 * {@link JackVaderFilter} test suite.
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 * 
 */
public class JackVaderFilterTest extends MediaApiTest {

	private MediaPipeline pipeline;

	private JackVaderFilter jackVader;

	@Before
	public void setup() throws KurentoMediaFrameworkException {
		pipeline = pipelineFactory.create();
		jackVader = pipeline.newJackVaderFilter().build();
	}

	@After
	public void teardown() {
		jackVader.release();
		pipeline.release();
	}

	/**
	 * Test if a {@link JackVaderFilter} can be created in the KMS. The filter
	 * is pipelined with a {@link PlayerEndpoint}, which feeds video to the
	 * filter. This test depends on the correct behaviour of the player and its
	 * events.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testJackVaderFilter() throws InterruptedException {
		PlayerEndpoint player = pipeline.newPlayerEndpoint(URL_SMALL).build();
		player.connect(jackVader);

		final BlockingQueue<EndOfStreamEvent> events = new ArrayBlockingQueue<EndOfStreamEvent>(
				1);
		player.addEndOfStreamListener(new MediaEventListener<EndOfStreamEvent>() {

			@Override
			public void onEvent(EndOfStreamEvent event) {
				events.add(event);
			}
		});

		player.play();

		Assert.assertNotNull(events.poll(10, SECONDS));

		player.stop();
		player.release();
	}

}
