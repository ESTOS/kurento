package com.kurento.tool.rom.client;

import com.kurento.kmf.media.ListenerRegistration;
import com.kurento.tool.rom.client.RemoteObject.EventListener;

public class ListenerSubscription implements ListenerRegistration {

	private String subscription;
	private String type;
	private EventListener listener;

	public ListenerSubscription(String subscription, String type,
			EventListener listener) {
		this.subscription = subscription;
		this.type = type;
		this.listener = listener;
	}

	public String getSubscription() {
		return subscription;
	}

	public String getType() {
		return type;
	}

	public EventListener getListener() {
		return listener;
	}

	@Override
	public String getRegistrationId() {
		return subscription;
	}
}
