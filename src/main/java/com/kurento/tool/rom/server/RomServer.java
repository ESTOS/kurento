package com.kurento.tool.rom.server;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.kurento.kmf.jsonrpcconnector.Props;
import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.transport.serialization.ParamsFlattener;

public class RomServer {

	private RemoteObjectManager manager = new RemoteObjectManager();

	private static ParamsFlattener FLATTENER = ParamsFlattener.getInstance();

	private String packageName;
	private String classSuffix;

	public RomServer(String packageName, String classSuffix) {
		this.packageName = packageName;
		this.classSuffix = classSuffix;
	}

	public String create(String remoteClassType, Props constructorParams)
			throws RomException {

		try {

			Class<?> clazz = Class.forName(packageName + "." + remoteClassType
					+ classSuffix);

			if (clazz.getAnnotation(RemoteClass.class) == null) {
				throw new RomException(
						"Remote classes must be annotated with @RemoteClass");
			}

			Constructor<?> constructor = clazz.getConstructors()[0];

			Object[] unflattenedConstParams = FLATTENER.unflattenParams(
					constructor.getParameterAnnotations(),
					constructor.getGenericParameterTypes(), constructorParams,
					manager);

			Object object = constructor.newInstance(unflattenedConstParams);

			return manager.putObject(object);

		} catch (Exception e) {
			// TODO Improve exception reporting
			throw new RomException(
					"Exception while creating an object with remoteClass='"
							+ remoteClassType + "' and params="
							+ constructorParams, e);
		}
	}

	@SuppressWarnings("unchecked")
	public <E> E invoke(String objectRef, String methodName, Props params,
			Class<E> clazz) throws RomException {
		return (E) invoke(objectRef, methodName, params, (Type) clazz);
	}

	public Object invoke(String objectRef, String methodName, Props params,
			Type type) throws RomException {

		Object remoteObject = manager.getObject(objectRef);

		if (remoteObject == null) {
			throw new RomException("Invalid remote object reference");
		}

		Class<?> remoteObjClass = remoteObject.getClass();

		try {

			Method method = getMethod(remoteObjClass, methodName);

			Object[] unflattenParams = FLATTENER.unflattenParams(
					method.getParameterAnnotations(),
					method.getGenericParameterTypes(), params, manager);

			Object result = method.invoke(remoteObject, unflattenParams);

			return FLATTENER.flattenResult(result, manager);

		} catch (Exception e) {
			// TODO Improve exception reporting
			throw new RomException(
					"Invocation exception of object with remoteClass='"
							+ remoteObjClass.getSimpleName() + "', method="
							+ methodName + " and params=" + params, e);
		}
	}

	private Method getMethod(Class<?> remoteObjClass, String methodName) {
		for (Method method : remoteObjClass.getMethods()) {
			if (method.getName().equals(methodName)) {
				return method;
			}
		}
		throw new RuntimeException("Method '" + methodName
				+ "' not found in class '"
				+ remoteObjClass.getClass().getSimpleName() + "'");
	}

	public void release(String objectRef) {
		this.manager.releaseObject(objectRef);
	}
}
