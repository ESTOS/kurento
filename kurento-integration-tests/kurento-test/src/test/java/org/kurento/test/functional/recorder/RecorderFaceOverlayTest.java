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
package org.kurento.test.functional.recorder;

import java.awt.Color;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.FaceOverlayFilter;
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaProfileSpecType;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;

/**
 *
 * <strong>Description</strong>: Test of a Recorder, using the stream source
 * from a PlayerEndpoint with FaceOverlayFilter through an WebRtcEndpoint.<br/>
 * <strong>Pipelines</strong>:
 * <ol>
 * <li>PlayerEndpoint -> FaceOverlayFilter -> RecorderEndpoint & WebRtcEndpoint
 * </li>
 * <li>PlayerEndpoint -> WebRtcEndpoint</li>
 * </ol>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Media should be received in the video tag</li>
 * <li>EOS event should arrive to player</li>
 * <li>Color above the head of the video should be the expected (image overlaid)
 * </li>
 * <li>Media should be received in the video tag (in the recording)</li>
 * <li>Color of the video should be the expected (in the recording)</li>
 * <li>Ended event should arrive to player (in the recording)</li>
 * <li>Play time should be the expected (in the recording)</li>
 * <li>Codecs should be as expected (in the recording)</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class RecorderFaceOverlayTest extends BaseRecorder {

	private static final int PLAYTIME = 30; // seconds
	private static final int THRESHOLD = 20; // seconds
	private static final Color EXPECTED_COLOR = Color.RED;
	private static final int EXPECTED_COLOR_X = 420;
	private static final int EXPECTED_COLOR_Y = 45;

	public RecorderFaceOverlayTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return TestScenario.localChromeAndFirefox();
	}

	@Test
	public void testRecorderFaceOverlayWebm() throws Exception {
		doTest(MediaProfileSpecType.WEBM, EXPECTED_VIDEO_CODEC_WEBM,
				EXPECTED_AUDIO_CODEC_WEBM, EXTENSION_WEBM);
	}

	@Test
	public void testRecorderFaceOverlayMp4() throws Exception {
		doTest(MediaProfileSpecType.MP4, EXPECTED_VIDEO_CODEC_MP4,
				EXPECTED_AUDIO_CODEC_MP4, EXTENSION_MP4);
	}

	public void doTest(MediaProfileSpecType mediaProfileSpecType,
			String expectedVideoCodec, String expectedAudioCodec,
			String extension) throws Exception {

		// Media Pipeline #1
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/fiwarecut.mp4").build();
		WebRtcEndpoint webRtcEP1 = new WebRtcEndpoint.Builder(mp).build();

		String recordingFile = getDefaultOutputFile(extension);

		RecorderEndpoint recorderEP = new RecorderEndpoint.Builder(mp,
				Protocol.FILE + recordingFile)
						.withMediaProfile(mediaProfileSpecType).build();
		FaceOverlayFilter filter = new FaceOverlayFilter.Builder(mp).build();
		filter.setOverlayedImage("http://files.kurento.org/imgs/red-square.png",
				-0.2F, -1.2F, 1.6F, 1.6F);

		playerEP.connect(filter);
		filter.connect(webRtcEP1);
		filter.connect(recorderEP);

		// Test execution #1. Play and record
		getPage().setThresholdTime(THRESHOLD);
		launchBrowser(mp, webRtcEP1, playerEP, recorderEP, expectedVideoCodec,
				expectedAudioCodec, recordingFile, EXPECTED_COLOR,
				EXPECTED_COLOR_X, EXPECTED_COLOR_Y, PLAYTIME);

		// Release Media Pipeline #1
		mp.release();

		// Reloading browser
		getPage().reload();

		// Media Pipeline #2
		MediaPipeline mp2 = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerEP2 = new PlayerEndpoint.Builder(mp2,
				Protocol.FILE + recordingFile).build();
		WebRtcEndpoint webRtcEP2 = new WebRtcEndpoint.Builder(mp2).build();
		playerEP2.connect(webRtcEP2);

		// Playing the recording
		launchBrowser(mp, webRtcEP2, playerEP2, null, expectedVideoCodec,
				expectedAudioCodec, recordingFile, EXPECTED_COLOR,
				EXPECTED_COLOR_X, EXPECTED_COLOR_Y, PLAYTIME);

		// Release Media Pipeline #2
		mp2.release();

		success = true;
	}

}
