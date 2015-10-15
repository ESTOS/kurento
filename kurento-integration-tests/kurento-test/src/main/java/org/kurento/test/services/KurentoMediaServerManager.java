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
package org.kurento.test.services;

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.TestConfiguration.FAKE_KMS_AUTOSTART_DEFAULT;
import static org.kurento.test.TestConfiguration.FAKE_KMS_AUTOSTART_PROP;
import static org.kurento.test.TestConfiguration.FAKE_KMS_LOGIN_PROP;
import static org.kurento.test.TestConfiguration.FAKE_KMS_PASSWD_PROP;
import static org.kurento.test.TestConfiguration.FAKE_KMS_PEM_PROP;
import static org.kurento.test.TestConfiguration.FAKE_KMS_WS_URI_PROP;
import static org.kurento.test.TestConfiguration.KMS_AUTOSTART_DEFAULT;
import static org.kurento.test.TestConfiguration.KMS_AUTOSTART_PROP;
import static org.kurento.test.TestConfiguration.KMS_DOCKER_IMAGE_FORCE_PULLING_DEFAULT;
import static org.kurento.test.TestConfiguration.KMS_DOCKER_IMAGE_FORCE_PULLING_PROP;
import static org.kurento.test.TestConfiguration.KMS_DOCKER_IMAGE_NAME_DEFAULT;
import static org.kurento.test.TestConfiguration.KMS_DOCKER_IMAGE_NAME_PROP;
import static org.kurento.test.TestConfiguration.KMS_WS_URI_PROP;
import static org.kurento.test.TestConfiguration.KURENTO_GST_PLUGINS_DEFAULT;
import static org.kurento.test.TestConfiguration.KURENTO_GST_PLUGINS_PROP;
import static org.kurento.test.TestConfiguration.KURENTO_KMS_LOGIN_PROP;
import static org.kurento.test.TestConfiguration.KURENTO_KMS_PASSWD_PROP;
import static org.kurento.test.TestConfiguration.KURENTO_SERVER_COMMAND_DEFAULT;
import static org.kurento.test.TestConfiguration.KURENTO_SERVER_COMMAND_PROP;
import static org.kurento.test.TestConfiguration.KURENTO_SERVER_DEBUG_DEFAULT;
import static org.kurento.test.TestConfiguration.KURENTO_SERVER_DEBUG_PROP;
import static org.kurento.test.TestConfiguration.KURENTO_WORKSPACE_DEFAULT;
import static org.kurento.test.TestConfiguration.KURENTO_WORKSPACE_PROP;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.apache.commons.io.FileUtils;
import org.kurento.commons.Address;
import org.kurento.commons.PropertiesManager;
import org.kurento.commons.exception.KurentoException;
import org.kurento.test.Shell;
import org.kurento.test.docker.Docker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.google.common.io.CharStreams;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * Initializer/stopper class for Kurento Media Server (KMS).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class KurentoMediaServerManager {

	public static Logger log = LoggerFactory
			.getLogger(KurentoMediaServerManager.class);

	private static String lastWorkspace;

	private SshConnection remoteKms = null;
	private String workspace;
	private int httpPort;
	private String testClassName;
	private String testMethodName;
	private String testDir;
	private String serverCommand;
	private String gstPlugins;
	private String debugOptions;
	private Address rabbitMqAddress;
	private String wsUri;
	private String registrarUri;
	private String registrarLocalAddress = "127.0.0.1";
	private boolean isKmsRemote;
	private boolean docker;
	private String dockerContainerName = "kms";

	public static KurentoMediaServerManager createWithWsTransport(String wsUri,
			int httpPort) {

		KurentoMediaServerManager manager = new KurentoMediaServerManager();
		manager.wsUri = wsUri;
		manager.httpPort = httpPort;
		return manager;
	}

	public static KurentoMediaServerManager createWithRabbitMqTransport(
			Address rabbitMqAddress, int httpPort) {
		KurentoMediaServerManager manager = new KurentoMediaServerManager();
		manager.rabbitMqAddress = rabbitMqAddress;
		manager.httpPort = httpPort;
		return manager;
	}

	private KurentoMediaServerManager() {
	}

	public void copyLogs() throws IOException {
		String kmsLogsPath = getKmsLogPath() + "logs";
		String targetFolder = testDir + testClassName;

		if (remoteKms != null) { // Remote KMS
			log.info("Copying KMS logs from remote host {}",
					remoteKms.getConnection());
			List<String> ls = remoteKms.listFiles(kmsLogsPath, true, false);

			for (String s : ls) {
				String targetFile = targetFolder + "/" + testMethodName + "-"
						+ s.substring(s.lastIndexOf("/") + 1);
				remoteKms.getFile(targetFile, s);
				KurentoServicesTestHelper
						.addServerLogFilePath(new File(targetFile));
			}
		} else { // Local KMS
			log.info("Copying KMS logs from local path {}", kmsLogsPath);

			Collection<File> kmsFiles = FileUtils
					.listFiles(new File(kmsLogsPath), null, true);

			for (File f : kmsFiles) {
				File destFile = new File(targetFolder,
						testMethodName + "-" + f.getName());
				FileUtils.copyFile(f, destFile);
				KurentoServicesTestHelper.addServerLogFilePath(destFile);
				log.debug("Log file: {}", destFile);
			}
		}
	}

	public String getKmsLogPath() {
		return isKmsRemote ? remoteKms.getTmpFolder() + "/" : workspace;
	}

	public void setTestDir(String testDir) {
		this.testDir = testDir;
	}

	public void setTestClassName(String testClassName) {
		this.testClassName = testClassName;
	}

	public void setTestMethodName(String testMethodName) {
		this.testMethodName = testMethodName;
	}

	public void start() throws IOException {
		start(false);
	}

	public void start(boolean isFake) throws IOException {

		// Properties
		String kmsLoginProp = isFake ? FAKE_KMS_LOGIN_PROP
				: KURENTO_KMS_LOGIN_PROP;
		String kmsPasswdProp = isFake ? FAKE_KMS_PASSWD_PROP
				: KURENTO_KMS_PASSWD_PROP;
		String kmsPemProp = isFake ? FAKE_KMS_PEM_PROP
				: KURENTO_KMS_PASSWD_PROP;
		String kmsAutostartProp = isFake ? FAKE_KMS_AUTOSTART_PROP
				: KMS_AUTOSTART_PROP;
		String kmsAutostartDefaultProp = isFake ? FAKE_KMS_AUTOSTART_DEFAULT
				: KMS_AUTOSTART_DEFAULT;
		String kmsWsUriProp = isFake ? FAKE_KMS_WS_URI_PROP : KMS_WS_URI_PROP;

		// Values
		String kmsLogin = getProperty(kmsLoginProp);
		String kmsPasswd = getProperty(kmsPasswdProp);
		String kmsPem = getProperty(kmsPemProp);

		String wsUri;
		if (this.wsUri != null) {
			wsUri = this.wsUri;
		} else {
			wsUri = getProperty(kmsWsUriProp, this.wsUri);
		}

		isKmsRemote = !wsUri.contains("localhost")
				&& !wsUri.contains("127.0.0.1") && !docker;

		if (isKmsRemote && kmsLogin == null
				&& (kmsPem == null || kmsPasswd == null)) {
			String kmsAutoStart = getProperty(kmsAutostartProp,
					kmsAutostartDefaultProp);
			throw new RuntimeException("Bad test parameters: "
					+ kmsAutostartProp + "=" + kmsAutoStart + " and "
					+ kmsWsUriProp + "=" + wsUri
					+ ". Remote KMS should be started but its credentials are not present: "
					+ kmsLoginProp + "=" + kmsLogin + ", " + kmsPasswdProp + "="
					+ kmsPasswd + ", " + kmsPemProp + "=" + kmsPem);
		}

		serverCommand = PropertiesManager.getProperty(
				KURENTO_SERVER_COMMAND_PROP, KURENTO_SERVER_COMMAND_DEFAULT);

		gstPlugins = PropertiesManager.getProperty(KURENTO_GST_PLUGINS_PROP,
				KURENTO_GST_PLUGINS_DEFAULT);

		try {
			workspace = Files.createTempDirectory("kurento-test").toString();
			lastWorkspace = workspace;
		} catch (IOException e) {
			workspace = PropertiesManager.getProperty(KURENTO_WORKSPACE_PROP,
					KURENTO_WORKSPACE_DEFAULT);
			log.error(
					"Exception loading temporal folder; instead folder {} will be used",
					workspace, e);
		}

		debugOptions = PropertiesManager.getProperty(KURENTO_SERVER_DEBUG_PROP,
				KURENTO_SERVER_DEBUG_DEFAULT);

		if (!workspace.endsWith("/")) {
			workspace += "/";
		}
		log.debug("Local folder to store temporal files: {}", workspace);

		if (rabbitMqAddress != null) {
			log.info(
					"Starting KMS with RabbitMQ: RabbitMQAddress:'{}'"
							+ " serverCommand:'{}' gstPlugins:'{}' workspace: '{}'",
					rabbitMqAddress, serverCommand, gstPlugins, workspace);
		} else {

			if (docker) {
				log.info(
						"Starting KMS dockerized with"
								+ " serverCommand:'{}' gstPlugins:'{}' workspace: '{}'",
						serverCommand, gstPlugins, workspace);
			} else {
				log.info(
						"Starting KMS with Ws uri: '{}'"
								+ " serverCommand:'{}' gstPlugins:'{}' workspace: '{}'",
						wsUri, serverCommand, gstPlugins, workspace);
			}

			if (!docker && !isKmsRemote && !isFreePort(wsUri)) {
				throw new RuntimeException("KMS cannot be started in URI: "
						+ wsUri + ". Port is not free");
			}
		}

		if (isKmsRemote) {
			String remoteKmsStr = wsUri.substring(wsUri.indexOf("//") + 2,
					wsUri.lastIndexOf(":"));
			log.info("Using remote KMS at {}", remoteKmsStr);
			remoteKms = new SshConnection(remoteKmsStr, kmsLogin, kmsPasswd,
					kmsPem);
			if (kmsPem != null) {
				remoteKms.setPem(kmsPem);
			}
			remoteKms.start();
			remoteKms.createTmpFolder();
		}

		createKurentoConf();

		if (isKmsRemote) {
			String[] filesToBeCopied = { "kurento.conf.json", "kurento.sh" };
			for (String s : filesToBeCopied) {
				remoteKms.scp(workspace + s,
						remoteKms.getTmpFolder() + "/" + s);
			}
			remoteKms.runAndWaitCommand("chmod", "+x",
					remoteKms.getTmpFolder() + "/kurento.sh");
		}

		startKms(wsUri);

		waitForKurentoMediaServer(this.wsUri);
	}

	private void startKms(String wsUri) throws IOException {

		String kmsLogPath = getKmsLogPath();

		if (isKmsRemote) {

			remoteKms.runAndWaitCommand("sh", "-c",
					kmsLogPath + "kurento.sh > /dev/null");
			log.info("Kurento Media Server started in wsUri: " + wsUri);

		} else if (docker) {

			startDockerizedKms();

		} else {

			Shell.run("sh", "-c", kmsLogPath + "kurento.sh");
			log.info("Kurento Media Server started in wsUri: " + wsUri);
		}
	}

	private void startDockerizedKms() {

		Docker dockerClient = Docker.getSingleton();

		String kmsImageName = PropertiesManager.getProperty(
				KMS_DOCKER_IMAGE_NAME_PROP, KMS_DOCKER_IMAGE_NAME_DEFAULT);

		boolean forcePulling = PropertiesManager.getProperty(
				KMS_DOCKER_IMAGE_FORCE_PULLING_PROP,
				KMS_DOCKER_IMAGE_FORCE_PULLING_DEFAULT);

		if (!dockerClient.existsImage(kmsImageName) || forcePulling) {
			log.info("Pulling kms image {}", kmsImageName);

			dockerClient.getClient().pullImageCmd(kmsImageName)
					.exec(new PullImageResultCallback()).awaitSuccess();

			log.info("Kms image {} pulled", kmsImageName);

		}

		if (dockerClient.existsContainer(dockerContainerName)) {
			throw new KurentoException(
					"Tryint to create a new container named '"
							+ dockerContainerName + "' but it already exist");
		}

		log.debug("Starting kms container...");

		CreateContainerResponse kmsContainer = dockerClient.getClient()
				.createContainerCmd(kmsImageName).withName(dockerContainerName)
				.withEnv("GST_DEBUG=" + debugOptions)
				.withCmd("--gst-debug-no-color").exec();

		dockerClient.getClient().startContainerCmd(kmsContainer.getId()).exec();

		wsUri = "ws://" + dockerClient.inspectContainer(dockerContainerName)
				.getNetworkSettings().getIpAddress() + ":8888/kurento";

		// Update wsUri to containerized KMS
		System.setProperty(KMS_WS_URI_PROP, wsUri);

		log.info("Kurento Media Server started in docker container wsUri: "
				+ wsUri);
	}

	private boolean isFreePort(String wsUri) {

		try {
			URI wsUrl = new URI(wsUri);

			String result = Shell.runAndWait("/bin/bash", "-c", "nc -z "
					+ wsUrl.getHost() + " " + wsUrl.getPort() + "; echo $?");

			if (result.trim().equals("0")) {
				log.warn("Port " + wsUrl.getPort()
						+ " is used. Maybe another KMS instance is running in this port");
				return false;
			}

		} catch (URISyntaxException e) {
			log.warn("WebSocket URI {} is malformed: " + e.getMessage(), wsUri);
		}

		return true;
	}

	private void waitForKurentoMediaServer(String wsUri) {

		long initTime = System.nanoTime();

		@ClientEndpoint
		class WebSocketClient extends Endpoint {

			@OnClose
			@Override
			public void onClose(Session session, CloseReason closeReason) {
			}

			@OnOpen
			@Override
			public void onOpen(Session session, EndpointConfig config) {
			}
		}

		if (wsUri != null) {

			javax.websocket.WebSocketContainer container = javax.websocket.ContainerProvider
					.getWebSocketContainer();

			int NUM_RETRIES = 300;
			int WAIT_MILLIS = 100;

			for (int i = 0; i < NUM_RETRIES; i++) {
				try {
					Session wsSession = container.connectToServer(
							new WebSocketClient(),
							ClientEndpointConfig.Builder.create().build(),
							new URI(wsUri));
					wsSession.close();

					double time = (System.nanoTime() - initTime)
							/ (double) 1000000;

					log.debug("Connected to KMS in "
							+ String.format("%3.2f", time) + " milliseconds");
					return;
				} catch (DeploymentException | IOException
						| URISyntaxException e) {
					try {
						Thread.sleep(WAIT_MILLIS);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
			}

			throw new KurentoException("Timeout of " + NUM_RETRIES * WAIT_MILLIS
					+ " millis waiting for KMS " + wsUri);

		} else {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				log.error("InterruptedException {}", e.getMessage());
			}
		}
	}

	private void createKurentoConf() {
		Configuration cfg = new Configuration(
				Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);

		// Data-model
		Map<String, Object> data = new HashMap<String, Object>();

		if (rabbitMqAddress != null) {
			data.put("transport", "rabbitmq");
			data.put("rabbitAddress", rabbitMqAddress.getHost());
			data.put("rabbitPort", String.valueOf(rabbitMqAddress.getPort()));
		} else {

			URI wsAsUri;
			try {
				wsAsUri = new URI(wsUri);
				int port = wsAsUri.getPort();
				String path = wsAsUri.getPath();
				data.put("transport", "ws");
				data.put("wsPort", String.valueOf(port));
				data.put("wsPath", path.substring(1));
				data.put("registrar", registrarUri);
				data.put("registrarLocalAddress", registrarLocalAddress);

			} catch (URISyntaxException e) {
				throw new KurentoException("Invalid ws uri: " + wsUri);
			}
		}

		data.put("gstPlugins", gstPlugins);
		data.put("debugOptions", debugOptions);
		data.put("serverCommand", serverCommand);
		data.put("workspace", getKmsLogPath());
		data.put("httpEndpointPort", String.valueOf(httpPort));

		cfg.setClassForTemplateLoading(KurentoMediaServerManager.class,
				"/templates/");

		createFileFromTemplate(cfg, data, "kurento.conf.json");
		createFileFromTemplate(cfg, data, "kurento.sh");
		Shell.runAndWait("chmod", "+x", workspace + "kurento.sh");
	}

	private void createFileFromTemplate(Configuration cfg,
			Map<String, Object> data, String filename) {

		try {

			Template template = cfg.getTemplate(filename + ".ftl");
			File file = new File(workspace + filename);
			Writer writer = new FileWriter(file);
			template.process(data, writer);
			writer.flush();
			writer.close();

			log.debug("Created file '" + file.getAbsolutePath() + "'");

		} catch (Exception e) {
			throw new RuntimeException(
					"Exception while creating file from template", e);
		}
	}

	public void destroy() throws IOException {

		if (docker) {

			Docker.getSingleton().stopAndRemoveContainer(dockerContainerName);

		} else {

			int numKmsProcesses = 0;
			// Max timeout waiting kms ending: 5 seconds
			long timeout = System.currentTimeMillis() + 5000;
			do {
				// If timeout, break the loop
				if (System.currentTimeMillis() > timeout) {
					break;
				}

				// Sending SIGTERM signal to KMS process
				kmsSigTerm();

				// Wait 100 msec to order kms termination
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				numKmsProcesses = countKmsProcesses();

			} while (numKmsProcesses > 0);

			if (numKmsProcesses > 0) {
				// If at this point there is still kms process (after trying to
				// kill it with SIGTERM during 5 seconds), we send the SIGKILL
				// signal to the process
				kmsSigKill();
			}

			// Copy KMS logs
			copyLogs();

			// Stop remote KMS
			if (remoteKms != null) {
				remoteKms.stop();
			}
		}
	}

	private void kmsSigTerm() throws IOException {
		log.trace("Sending SIGTERM to KMS process");
		if (remoteKms != null) {
			String kmsPid = remoteKms.execAndWaitCommandNoBr("cat",
					remoteKms.getTmpFolder() + "/kms-pid");
			remoteKms.runAndWaitCommand("kill", kmsPid);
		} else {
			Shell.runAndWait("sh", "-c", "kill `cat " + workspace + "kms-pid`");
		}
	}

	private void kmsSigKill() throws IOException {
		log.trace("Sending SIGKILL to KMS process");
		if (remoteKms != null) {
			String kmsPid = remoteKms.execAndWaitCommandNoBr("cat",
					remoteKms.getTmpFolder() + "/kms-pid");
			remoteKms.runAndWaitCommand("sh", "-c", "kill -9 " + kmsPid);
		} else {
			Shell.runAndWait("sh", "-c",
					"kill -9 `cat " + workspace + "kms-pid`");
		}
	}

	public String getDebugOptions() {
		return debugOptions;
	}

	public void setDebugOptions(String debugOptions) {
		this.debugOptions = debugOptions;
	}

	public void setHttpPort(int httpPort) {
		this.httpPort = httpPort;
	}

	public int countKmsProcesses() {
		int result = 0;
		try {
			// This command counts number of process (given its PID, stored in
			// kms-pid file)

			if (remoteKms != null) {
				String kmsPid = remoteKms.execAndWaitCommandNoBr("cat",
						remoteKms.getTmpFolder() + "/kms-pid");
				result = Integer.parseInt(remoteKms.execAndWaitCommandNoBr(
						"ps --pid " + kmsPid + " --no-headers | wc -l"));
			} else {
				String[] command = { "sh", "-c", "ps --pid `cat " + workspace
						+ "kms-pid` --no-headers | wc -l" };
				Process countKms = Runtime.getRuntime().exec(command);
				String stringFromStream = CharStreams.toString(
						new InputStreamReader(countKms.getInputStream(),
								"UTF-8"));
				result = Integer.parseInt(stringFromStream.trim());
			}
		} catch (IOException e) {
			log.error("Exception counting KMS processes", e);
		}

		return result;
	}

	public static String getWorkspace() {
		return lastWorkspace;
	}

	public String getLocalhostWsUrl() {
		return wsUri;
	}

	public void setRegistrarUri(String registrarUri) {
		this.registrarUri = registrarUri;
	}

	public void setRegistrarLocalAddress(String registrarLocalAddress) {
		this.registrarLocalAddress = registrarLocalAddress;
	}

	public String getRegistrarLocalAddress() {
		return registrarLocalAddress;
	}

	public String getRegistrarUri() {
		return registrarUri;
	}

	public void restart() throws IOException {
		kmsSigKill();
		startKms(this.wsUri);
		waitForKurentoMediaServer(wsUri);
	}

	public void setDocker(boolean docker) {
		this.docker = docker;
	}

	public void setDockerContainerName(String containerName) {
		this.dockerContainerName = containerName;
	}

}
