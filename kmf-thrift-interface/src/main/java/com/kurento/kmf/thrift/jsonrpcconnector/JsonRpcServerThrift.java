package com.kurento.kmf.thrift.jsonrpcconnector;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kurento.kmf.jsonrpcconnector.JsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.JsonUtils;
import com.kurento.kmf.jsonrpcconnector.internal.JsonRpcHandlerManager;
import com.kurento.kmf.jsonrpcconnector.internal.client.TransactionImpl;
import com.kurento.kmf.jsonrpcconnector.internal.client.TransactionImpl.ResponseSender;
import com.kurento.kmf.jsonrpcconnector.internal.message.Message;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.jsonrpcconnector.internal.message.Response;
import com.kurento.kmf.jsonrpcconnector.internal.message.ResponseError;
import com.kurento.kmf.jsonrpcconnector.internal.server.ServerSession;
import com.kurento.kmf.thrift.ThriftServer;
import com.kurento.kmf.thrift.internal.ThriftInterfaceExecutorService;
import com.kurento.kms.thrift.api.KmsMediaServerService.Iface;
import com.kurento.kms.thrift.api.KmsMediaServerService.Processor;

public class JsonRpcServerThrift {

	private static Logger LOG = LoggerFactory
			.getLogger(JsonRpcServerThrift.class);

	private ThriftServer server;

	private JsonRpcHandler<?> handler;

	private ServerSession session;

	private Class<?> paramsClass;

	public JsonRpcServerThrift(JsonRpcHandler<?> jsonRpcHandler,
			ThriftInterfaceExecutorService executorService,
			InetSocketAddress inetSocketAddress) {

		this.handler = jsonRpcHandler;
		this.paramsClass = JsonRpcHandlerManager.getParamsType(handler
				.getHandlerType());

		LOG.info("Starting JsonRpcServer on {}", inetSocketAddress);

		Processor<Iface> serverProcessor = new Processor<Iface>(new Iface() {

			@Override
			public String invokeJsonRpc(final String requestStr)
					throws TException {

				Request<?> request = JsonUtils.fromJsonRequest(requestStr,
						paramsClass);

				Response<JsonObject> response = processRequest(request);

				return response.toString();
			}
		});

		session = new ServerSession("XXX", null, null, "YYY") {
			@Override
			public void handleResponse(Response<JsonElement> response) {
				LOG.error("Trying to send a response from by means of session but it is not supported");
			}
		};

		server = new ThriftServer(serverProcessor, executorService,
				inetSocketAddress);
	}

	public Response<JsonObject> processRequest(Request<?> request) {

		@SuppressWarnings("unchecked")
		final Response<JsonObject>[] response = new Response[1];

		TransactionImpl t = new TransactionImpl(session, request,
				new ResponseSender() {
					@SuppressWarnings("unchecked")
					@Override
					public void sendResponse(Message message)
							throws IOException {
						response[0] = (Response<JsonObject>) message;
					}
				});

		try {

			JsonRpcHandler genericHandler = handler;
			genericHandler.handleRequest(t, request);

		} catch (Exception e) {

			ResponseError error = ResponseError.newFromException(e);
			return new Response<>(request.getId(), error);
		}

		if (response[0] != null) {
			// Simulate receiving json string from net
			String jsonResponse = response[0].toString();

			LOG.debug("<-- {}", jsonResponse);

			Response<JsonObject> newResponse = JsonUtils.fromJsonResponse(
					jsonResponse, JsonObject.class);

			newResponse.setId(request.getId());

			return newResponse;

		} else {
			return new Response<>(request.getId());
		}
	}

	public void start() {
		LOG.info("Starting Thrift Server");
		server.start();
		LOG.info("Thrift Server started");
	}

	public void destroy() {
		if (server != null) {
			server.destroy();
		}
	}

}
