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
package org.kurento.test.base;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.junit.internal.runners.model.MultipleFailureException;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to print KMS log when a test fails.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
@SuppressWarnings("deprecation")
public class KmsLogOnFailure extends TestWatcher {

	public static Logger log = LoggerFactory.getLogger(KmsLogOnFailure.class);

	private KurentoClientManager kurentoClientManager;

	private boolean succees = false;
	private boolean deleteLogsIfSuccess = true;

	@Override
	protected void starting(Description description) {
		log.info(
				"+--------------------------------------------------------------------------------------");
		log.info("|       TEST STARTING: {}",
				description.getClassName() + "." + description.getMethodName());
		log.info(
				"+--------------------------------------------------------------------------------------");
	}

	@Override
	protected void succeeded(Description description) {

		log.info(
				"+--------------------------------------------------------------------------------------");
		log.info("|       TEST SUCCEEDED: {}",
				description.getClassName() + "." + description.getMethodName());
		log.info(
				"+--------------------------------------------------------------------------------------");

		super.succeeded(description);
		succees = true;
	}

	@Override
	protected void failed(Throwable e, Description description) {

		log.info(
				"+--------------------------------------------------------------------------------------");
		log.info("|       TEST FAILED: {}",
				description.getClassName() + "." + description.getMethodName());
		log.info(
				"+--------------------------------------------------------------------------------------");

		retrieveGstreamerDots();

		printKmsLogs();
	}

	private void printKmsLogs() {

		if (KurentoServicesTestHelper.printKmsLog()) {

			List<File> logFiles = KurentoServicesTestHelper.getServerLogFiles();

			if (logFiles != null) {

				final String separator = "******************************************************************************";

				for (File logFile : logFiles) {

					if (logFile != null && logFile.exists()) {

						System.err.println(separator);
						System.err.println(
								"Log file path: " + logFile.getAbsolutePath());
						System.err.println("Content:");

						try {
							for (String line : FileUtils.readLines(logFile)) {
								System.err.println(line);
							}
						} catch (IOException e1) {
							System.err
									.println("Error reading lines in log file");
							e1.printStackTrace();
						}
						System.err.println(separator);
					}
				}
			}
		}
	}

	private void retrieveGstreamerDots() {

		if (getKurentoClientManager() != null) {

			try {
				KurentoClient kurentoClient = getKurentoClientManager()
						.getKurentoClient();

				List<MediaPipeline> pipelines = kurentoClient.getServerManager()
						.getPipelines();

				log.debug(
						"Retrieving GStreamerDots for all pipelines in KMS ({})",
						pipelines.size());

				for (MediaPipeline pipeline : pipelines) {

					String pipelineName = pipeline.getName();
					log.debug("Saving GstreamerDot for pipeline {}",
							pipelineName);

					String gstreamerDotFile = KurentoClientWebPageTest
							.getDefaultOutputFile("-" + pipelineName);

					try {
						FileUtils.writeStringToFile(new File(gstreamerDotFile),
								pipeline.getGstreamerDot());

					} catch (IOException ioe) {
						log.error("Exception writing GstreamerDot file", ioe);
					}
				}
			} catch (WebSocketException e) {
				log.warn(
						"WebSocket exception while reading existing pipelines. Maybe KMS is closed: {}:{}",
						e.getClass().getName(), e.getMessage());
			}
		}
	}

	@Override
	protected void finished(Description description) {

		super.finished(description);

		try {
			if (succees && deleteLogsIfSuccess) {
				// Delete logs
				File folder = new File(KurentoServicesTestHelper.getTestDir()
						+ "/" + KurentoServicesTestHelper.getTestCaseName());

				final File[] files = folder.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(final File dir, final String name) {
						return name.contains(
								KurentoServicesTestHelper.getSimpleTestName());
					}
				});
				if (files != null) {
					for (final File file : files) {
						if (!file.delete()) {
							log.error("Can't remove {}",
									file.getAbsolutePath());
						}
					}
				}
			}

		} catch (Exception e) {
			log.error("Exception closing kurento client manager", e);
		}
	}

	@SuppressWarnings({ "unused" })
	private void showException(Throwable e) {
		if (e instanceof org.junit.internal.runners.model.MultipleFailureException) {

			MultipleFailureException multipleEx = (MultipleFailureException) e;
			for (Throwable failure : multipleEx.getFailures()) {
				failure.printStackTrace();
			}

		} else {
			e.printStackTrace();
		}
	}

	public KurentoClientManager getKurentoClientManager() {
		return kurentoClientManager;
	}

	public void setKurentoClientManager(
			KurentoClientManager kurentoClientManager) {
		this.kurentoClientManager = kurentoClientManager;
	}

	public boolean isDeleteLogsIfSuccess() {
		return deleteLogsIfSuccess;
	}

	public void setDeleteLogsIfSuccess(boolean deleteLogsIfSuccess) {
		this.deleteLogsIfSuccess = deleteLogsIfSuccess;
	}

}
