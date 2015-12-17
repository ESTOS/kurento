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

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.kurento.commons.exception.KurentoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CharStreams;

/**
 * Local shell.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class Shell {

  public static Logger log = LoggerFactory.getLogger(Shell.class);

  public static Process run(final String... command) {
    return run(true, command);
  }

  public static Process run(boolean redirectOutputs, final String... command) {
    log.trace("Running command on the shell: {}", Arrays.toString(command));

    try {
      ProcessBuilder p = new ProcessBuilder(command);
      p.redirectErrorStream(true);
      if (redirectOutputs) {
        p.redirectOutput(ProcessBuilder.Redirect.INHERIT);
      }
      return p.start();
    } catch (IOException e) {
      throw new RuntimeException(
          "Exception while executing command '" + Arrays.toString(command) + "'", e);
    }
  }

  public static String runAndWaitString(final String command) {
    return runAndWait(command.split(" "));
  }

  public static String runAndWaitArray(final String[] command) {
    log.trace("Running command on the shell: {}", Arrays.toString(command));
    String result = runAndWaitNoLog(command);
    log.trace("Result:" + result);
    return result;
  }

  public static String runAndWait(final String... command) {
    return runAndWaitArray(command);
  }

  public static String runAndWaitNoLog(final String... command) {
    Process p;
    try {
      p = new ProcessBuilder(command).redirectErrorStream(true).start();

      String output = CharStreams.toString(new InputStreamReader(p.getInputStream(), "UTF-8"));

      p.destroy();

      return output;

    } catch (IOException e) {
      throw new KurentoException(
          "Exception executing command on the shell: " + Arrays.toString(command), e);
    }
  }

}
