package org.lttng.studio.model.kernel;

import java.util.HashMap;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.lttng.studio.reader.handler.IModelKeys;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class ModelRegistry {

	private static final String uuidField = "uuid";

	HashMap<Object, HashMap<Class<?>, Object>> reg; // (trace/host/global), type, instance
	BiMap<IModelKeys, Class<?>> modelTypes;

	private static final Object shared = new Object();

	public ModelRegistry() {
		reg = new HashMap<Object, HashMap<Class<?>, Object>>();
		modelTypes = HashBiMap.create();
	}

	public HashMap<Class<?>, Object> getOrCreateContext(Object context) {
		if (!reg.containsKey(context)) {
			reg.put(context, new HashMap<Class<?>, Object>());
		}
		return reg.get(context);
	}

	public <T> T getOrCreateModel(Object context, Class<T> klass) {
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

	public <T> T getModel(Object context, Class<T> klass) {
		if (!reg.containsKey(context))
			return null;
		return klass.cast(reg.get(context).get(klass));
	}

	public void register(IModelKeys key, Class<?> klass) {
		modelTypes.put(key, klass);
	}

	public <T> T getModelForEvent(CtfTmfEvent ev, Class<T> klass) {
		IModelKeys arrity = modelTypes.inverse().get(klass);
		T model = null;
		switch (arrity) {
		case PER_HOST:
			String uuid = (String) ev.getTrace().getCTFTrace().getClock().getProperty(uuidField);
			model = getOrCreateModel(uuid, klass);
			break;
		case PER_TRACE:
			model = getOrCreateModel(ev.getTrace().getCTFTrace(), klass);
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
