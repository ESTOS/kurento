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

package com.kurento.kmf.repository.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.kurento.kmf.repository.RepositoryHttpRecorder;
import com.kurento.kmf.repository.RepositoryItem;
import com.kurento.kmf.repository.test.util.HttpRepositoryTest;
import com.kurento.kmf.repository.test.util.TestUtils;

public class RangePutTests extends HttpRepositoryTest {

	private static final Logger log = LoggerFactory
			.getLogger(RangePutTests.class);

	@Test
	public void testFileUploadWithSeqPUTs() throws Exception {

		RepositoryItem repositoryItem = getRepository().createRepositoryItem();

		String id = repositoryItem.getId();

		File fileToUpload = new File("test-files/logo.png");

		uploadFileWithSeqPUTs(repositoryItem.createRepositoryHttpRecorder(),
				fileToUpload, repositoryItem);

		RepositoryItem newRepositoryItem = getRepository()
				.findRepositoryItemById(id);

		File downloadedFile = new File("test-files/tmp/" + id);
		downloadFromURL(
				newRepositoryItem.createRepositoryHttpPlayer().getURL(),
				downloadedFile);

		assertTrue("The uploaded file and downloaded one are different",
				TestUtils.equalFiles(fileToUpload, downloadedFile));
	}

	protected void uploadFileWithSeqPUTs(RepositoryHttpRecorder recorder,
			File fileToUpload, RepositoryItem repositoryItem) throws Exception {

		recorder.setAutoTerminationTimeout(500000);
		String url = recorder.getURL();

		DataInputStream is = null;

		try {

			is = new DataInputStream(new FileInputStream(fileToUpload));

			int sentBytes = 0;

			byte[] info = new byte[40000];

			int readBytes;

			int numRequest = 0;

			while ((readBytes = is.read(info)) != -1) {

				ResponseEntity<String> response = putContent(url,
						Arrays.copyOf(info, readBytes), sentBytes);

				sentBytes += readBytes;

				log.info(numRequest + ": " + response.toString());

				assertEquals("Returned response: " + response.getBody(),
						HttpStatus.OK, response.getStatusCode());

				if (numRequest == 3) {

					// Simulating retry

					response = putContent(url, Arrays.copyOf(info, readBytes),
							sentBytes - readBytes);

					log.info(numRequest + ": " + response.toString());

					assertEquals("Returned response: " + response.getBody(),
							HttpStatus.OK, response.getStatusCode());

				} else if (numRequest == 4) {

					// Simulating retry with new data

					byte[] newInfo = new byte[500];
					int newReadBytes = is.read(newInfo);

					response = putContent(
							url,
							concat(Arrays.copyOf(info, readBytes),
									Arrays.copyOf(newInfo, newReadBytes)),
							sentBytes - readBytes);

					sentBytes += newReadBytes;

					log.info(numRequest + ": " + response.toString());

					assertEquals("Returned response: " + response.getBody(),
							HttpStatus.OK, response.getStatusCode());

				} else if (numRequest == 5) {

					// Simulating send ahead data

					response = putContent(url, Arrays.copyOf(info, readBytes),
							sentBytes + 75000);

					log.info(numRequest + ": " + response.toString());

					assertEquals("Returned response: " + response.getBody(),
							HttpStatus.NOT_IMPLEMENTED,
							response.getStatusCode());

				}

				numRequest++;
			}

		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}

			recorder.stop();
		}
	}

	private ResponseEntity<String> putContent(String url, byte[] info,
			int firstByte) {

		RestTemplate httpClient = getRestTemplate();

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Content-Range", "bytes " + firstByte + "-"
				+ (firstByte + info.length) + "/*");
		requestHeaders.set("Content-Length", Integer.toString(info.length));

		HttpEntity<byte[]> requestEntity = new HttpEntity<byte[]>(info,
				requestHeaders);

		ResponseEntity<String> response = httpClient.exchange(url,
				HttpMethod.PUT, requestEntity, String.class);

		log.info("Put " + info.length + " bytes from " + firstByte + " to "
				+ (firstByte + info.length));

		return response;

	}

	private byte[] concat(byte[] first, byte[] second) {
		byte[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}
}