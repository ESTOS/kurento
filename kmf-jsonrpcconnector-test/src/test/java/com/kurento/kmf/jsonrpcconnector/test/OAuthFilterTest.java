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
package com.kurento.kmf.jsonrpcconnector.test;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.jsonrpcconnector.internal.server.config.JsonRpcProperties;
import com.kurento.kmf.jsonrpcconnector.test.BasicEchoTest.Params;
import com.kurento.kmf.jsonrpcconnector.test.base.JsonRpcConnectorBaseTest;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 * 
 */
public class OAuthFilterTest extends JsonRpcConnectorBaseTest {

	private static final Logger log = LoggerFactory
			.getLogger(OAuthFilterTest.class);

	@Configuration
	@ComponentScan("com.kurento.kmf")
	public static class KmfJsonRpcProperties {

		@Bean
		public JsonRpcProperties jsonRpcProperties() {

			JsonRpcProperties configuration = new JsonRpcProperties();
			configuration.setKeystoneHost("http://cloud.lab.fi-ware.org");
			return configuration;
		}
	}

	// TODO this test is disabled since the properties are not loaded correctly
	@Ignore
	@Test
	public void testOAuthFilter() throws IOException {
		HttpHeaders headers = new HttpHeaders();
		// TODO this token should be obtained, instead of being fixed
		headers.add(
				"X-Auth-Token",
				"Grx7fubc-ZH_3VH9K69qGSN97T2Hhl19Los153sZYghrad-1vW4cz-6Bfhqjeh86LeSWpONLybl0ZycMBBbdeg");

		try (JsonRpcClient client = createJsonRpcClient("/jsonrpc", headers)) {
			log.info("Client started");

			Params params = new Params();
			params.param1 = "Value1";
			params.param2 = "Value2";

			Params result = client.sendRequest("echo", params, Params.class);

			log.info("Response:" + result);

			Assert.assertEquals(params.param1, result.param1);
			Assert.assertEquals(params.param2, result.param2);
		}

		log.info("Client finished");
	}

}
