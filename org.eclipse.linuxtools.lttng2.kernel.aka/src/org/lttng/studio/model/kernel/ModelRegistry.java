package org.lttng.studio.model.kernel;

import java.util.HashMap;

public class ModelRegistry {

	HashMap<Object, HashMap<Class<?>, Object>> reg;

	public ModelRegistry() {
		reg = new HashMap<Object, HashMap<Class<?>, Object>>();
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

}
