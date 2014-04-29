package com.kurento.tool.rom.client;

import java.lang.reflect.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.kurento.kmf.jsonrpcconnector.Props;
import com.kurento.kmf.media.Continuation;
import com.kurento.tool.rom.transport.serialization.ParamsFlattener;

public class RemoteObject {

	private static Logger LOG = LoggerFactory.getLogger(RemoteObject.class);

	private static ParamsFlattener FLATTENER = ParamsFlattener.getInstance();

	public interface EventListener {
		public void onEvent(String eventType, Props data);
	}

	private final String objectRef;
	private final RomClient client;
	private final RomClientObjectManager manager;
	private final String type;

	// This object is used in the process of unflatten. It is common that
	// RemoteObject is used with a Typed wrapper (with reflexion, with code
	// generation or by hand). In this cases, the object reference is unflatten
	// to this value instead of RemoteObject itself.
	private Object wrapperForUnflatten;

	private final Multimap<String, EventListener> listeners = Multimaps
			.synchronizedMultimap(ArrayListMultimap
					.<String, EventListener> create());

	public RemoteObject(String objectRef, String type, RomClient client,
			RomClientObjectManager manager) {
		this.objectRef = objectRef;
		this.client = client;
		this.manager = manager;
		this.type = type;

		this.manager.registerObject(objectRef, this);
	}

	public Object getWrapperForUnflatten() {
		return wrapperForUnflatten;
	}

	public void setWrapperForUnflatten(Object wrapperForUnflatten) {
		this.wrapperForUnflatten = wrapperForUnflatten;
	}

	@SuppressWarnings("unchecked")
	public <E> E invoke(String method, Props params, Class<E> clazz) {

		Type flattenType = FLATTENER.calculateFlattenType(clazz);

		Object obj = invoke(method, params, flattenType);

		return (E) FLATTENER.unflattenValue("return", clazz, obj, manager);
	}

	public Object invoke(String method, Props params, Type type) {

		Type flattenType = FLATTENER.calculateFlattenType(type);

		Object obj = client.invoke(objectRef, method, params, flattenType);

		return FLATTENER.unflattenValue("return", type, obj, manager);
	}

	@SuppressWarnings("rawtypes")
	public void invoke(String method, Props params, final Type type,
			final Continuation cont) {

		Type flattenType = FLATTENER.calculateFlattenType(type);

		client.invoke(objectRef, method, params, flattenType,
				new DefaultContinuation<Object>(cont) {
					@SuppressWarnings("unchecked")
					@Override
					public void onSuccess(Object result) {
						cont.onSuccess(FLATTENER.unflattenValue("return", type,
								result, manager));
					}
				});
	}

	public void release() {
		client.release(objectRef);
		this.manager.releaseObject(objectRef);
	}

	public void release(final Continuation<Void> cont) {
		client.release(objectRef, new DefaultContinuation<Void>(cont) {
			@Override
			public void onSuccess(Void result) {
				manager.releaseObject(objectRef);
				cont.onSuccess(null);
			}
		});
	}

	@Override
	protected void finalize() {
		release(new ErrorLogContinuation<Void>(
				"Exception while releasing object " + objectRef));
	}

	public ListenerSubscription addEventListener(String eventType,
			EventListener listener) {

		String subscription = client.subscribe(objectRef, eventType);

		listeners.put(eventType, listener);

		return new ListenerSubscription(subscription, eventType, listener);
	}

	public void addEventListener(final String eventType,
			final Continuation<ListenerSubscription> cont,
			final EventListener listener) {

		client.subscribe(objectRef, eventType, new DefaultContinuation<String>(
				cont) {
			@Override
			public void onSuccess(String subscription) {
				listeners.put(eventType, listener);
				cont.onSuccess(new ListenerSubscription(subscription,
						eventType, listener));
			}
		});
	}

	public String getObjectRef() {
		return objectRef;
	}

	public void fireEvent(String type, Props data) {
		for (EventListener eventListener : this.listeners.get(type)) {
			try {
				eventListener.onEvent(type, data);
			} catch (Exception e) {
				LOG.error("Exception executing event listener", e);
			}
		}
	}

	public String getType() {
		return type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((objectRef == null) ? 0 : objectRef.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		RemoteObject other = (RemoteObject) obj;
		if (objectRef == null) {
			if (other.objectRef != null) {
				return false;
			}
		} else if (!objectRef.equals(other.objectRef)) {
			return false;
		}
		return true;
	}

}
