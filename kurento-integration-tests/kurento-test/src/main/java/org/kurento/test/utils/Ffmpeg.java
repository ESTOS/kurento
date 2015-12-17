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

package org.kurento.test.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.kurento.test.base.KurentoTest;
import org.kurento.test.config.AudioChannel;
import org.kurento.test.grid.GridNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audio recorder using FFMPEG and audio quality assessment with PESQ.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.11
 * @see <a href="https://www.ffmpeg.org/">FFMPEG</a>
 * @see <a href="http://en.wikipedia.org/wiki/PESQ">PESQ</a>
 */
public class Ffmpeg {

  private static Logger log = LoggerFactory.getLogger(Ffmpeg.class);

  private static final String HTTP_TEST_FILES = "http://files.kurento.org";
  private static final String PESQ_RESULTS = "pesq_results.txt";
  private static final String RECORDED_WAV = KurentoTest.getDefaultOutputFile("recorded.wav");

  public static void recordRemote(GridNode node, int seconds, int sampleRate,
      AudioChannel audioChannel) {
    node.getSshConnection().execCommand("ffmpeg", "-y", "-t", String.valueOf(seconds), "-f", "alsa",
        "-i", "pulse", "-q:a", "0", "-ac", audioChannel.toString(), "-ar",
        String.valueOf(sampleRate), RECORDED_WAV);
  }

  public static void record(int seconds, int sampleRate, AudioChannel audioChannel) {
    Shell.run("sh", "-c", "ffmpeg -y -t " + seconds + " -f alsa -i pulse -q:a 0 -ac " + audioChannel
        + " -ar " + sampleRate + " " + RECORDED_WAV);
  }

  public static float getRemotePesqMos(GridNode node, String audio, int sampleRate) {
    node.getSshConnection().getFile(RECORDED_WAV, RECORDED_WAV);
    return getPesqMos(audio, sampleRate);
  }

  public static float getPesqMos(String audio, int sampleRate) {
    float pesqmos = 0;

    try {
      String pesq = KurentoTest.getTestFilesPath() + "/bin/pesq/PESQ";
      String origWav = "";
      if (audio.startsWith(HTTP_TEST_FILES)) {
        origWav = KurentoTest.getTestFilesPath() + audio.replace(HTTP_TEST_FILES, "");
      } else {
        // Download URL
        origWav = KurentoTest.getDefaultOutputFile("downloaded.wav");
        URL url = new URL(audio);
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        FileOutputStream fos = new FileOutputStream(origWav);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
      }

      Shell.runAndWait(pesq, "+" + sampleRate, origWav, RECORDED_WAV);
      List<String> lines = FileUtils.readLines(new File(PESQ_RESULTS), "utf-8");
      pesqmos = Float.parseFloat(lines.get(1).split("\t")[2].trim());
      log.info("PESQMOS " + pesqmos);

      Shell.runAndWait("rm", PESQ_RESULTS);

    } catch (IOException e) {
      log.error("Exception recording local audio", e);
    }

    return pesqmos;
  }

}
