package com.kurento.kmf.jsonrpc.test;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.kurento.kmf.jsonrpcconnector.DefaultJsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.Transaction;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClientLocal;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;

public class JsonRpcClientLocalTest {

	private static final Logger LOG = LoggerFactory
			.getLogger(JsonRpcClientLocalTest.class);

	static class EchoJsonRpcHandler extends DefaultJsonRpcHandler<JsonObject> {

		@Override
		public void handleRequest(Transaction transaction,
				Request<JsonObject> request) throws Exception {

			LOG.info("Request id:" + request.getId());
			LOG.info("Request method:" + request.getMethod());
			LOG.info("Request params:" + request.getParams());

			transaction.sendResponse(request.getParams());
		}
	}

	static class Params {
		String param1;
		String param2;
	}

	@Test
	public void echoTest() throws Exception {

		LOG.info("Client started");

		JsonRpcClient client = new JsonRpcClientLocal(new EchoJsonRpcHandler());

		Params params = new Params();
		params.param1 = "Value1";
		params.param2 = "Value2";

		Params result = client.sendRequest("echo", params, Params.class);

		LOG.info("Response:" + result);

		Assert.assertEquals(params.param1, result.param1);
		Assert.assertEquals(params.param2, result.param2);

		client.close();

		LOG.info("Client finished");

	}

}
