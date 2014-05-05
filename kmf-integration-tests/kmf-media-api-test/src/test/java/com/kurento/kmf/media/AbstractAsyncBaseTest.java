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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 */
public abstract class AbstractAsyncBaseTest extends MediaApiTest {

	protected MediaPipeline pipeline;

	@Before
	public void abstractSetup() throws InterruptedException {
		final BlockingQueue<MediaPipeline> events = new ArrayBlockingQueue<MediaPipeline>(
				1);
		pipelineFactory.create(new Continuation<MediaPipeline>() {
			@Override
			public void onSuccess(MediaPipeline result) {
				events.add(result);
			}

			@Override
			public void onError(Throwable cause) {
				throw new KurentoMediaFrameworkException(cause);
			}
		});
		pipeline = events.poll(500, MILLISECONDS);

		if (pipeline == null) {
			Assert.fail();
		}
	}

	@After
	public void abstractTeardown() throws InterruptedException {
		if (pipeline != null) {
			releaseMediaObject(pipeline);
		}
	}

	protected static void releaseMediaObject(final MediaObject mo)
			throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);

		mo.release(new Continuation<Void>() {
			@Override
			public void onSuccess(Void result) {
				latch.countDown();
			}

			@Override
			public void onError(Throwable cause) {
				throw new KurentoMediaFrameworkException(cause);
			}
		});
		Assert.assertTrue(latch.await(500, MILLISECONDS));
	}

}
