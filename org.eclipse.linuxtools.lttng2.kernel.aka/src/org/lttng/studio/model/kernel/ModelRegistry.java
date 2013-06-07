package org.lttng.studio.model.kernel;

import java.util.HashMap;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.lttng.studio.reader.handler.IModelKeys;

public class ModelRegistry {

	private static final String uuidField = "uuid";

	HashMap<Object, HashMap<Class<?>, Object>> reg; // (trace/host/global), type, instance
	HashMap<Class<?>, IModelKeys> modelTypes;

	private static final Object shared = new Object();

	public ModelRegistry() {
		reg = new HashMap<Object, HashMap<Class<?>, Object>>();
		modelTypes = new HashMap<Class<?>, IModelKeys>();
	}

	private HashMap<Class<?>, Object> getOrCreateContext(Object context) {
		if (!reg.containsKey(context)) {
			reg.put(context, new HashMap<Class<?>, Object>());
		}
		return reg.get(context);
	}

	private <T> T getOrCreateModel(Object context, Class<T> klass) {
		HashMap<Class<?>, Object> map = getOrCreateContext(context);
		if (!map.containsKey(klass)) {
			Object inst = null;
			try {
				inst = klass.newInstance();
			} catch (Exception e) {
				throw new RuntimeException("Error creating model " + klass.getName());
			}
			map.put(klass, inst);
		}
		return klass.cast(map.get(klass));
	}

	/*
	private <T> T getModel(Object context, Class<T> klass) {
		if (!reg.containsKey(context))
			return null;
		return klass.cast(reg.get(context).get(klass));
	}
	*/

	public void register(IModelKeys key, Class<?> klass) {
		modelTypes.put(klass, key);
	}

	public <T> T getModelForTrace(CtfTmfTrace trace, Class<T> klass) {
		IModelKeys arrity = modelTypes.get(klass);
		T model = null;
		switch (arrity) {
		case PER_HOST:
			String uuid = (String) trace.getCTFTrace().getClock().getProperty(uuidField);
			model = getOrCreateModel(uuid, klass);
			break;
		case PER_TRACE:
			model = getOrCreateModel(trace.getCTFTrace(), klass);
			break;
		case SHARED:
			model = getOrCreateModel(shared, klass);
			break;
		default:
			break;
		}
		return model;
	}

}
