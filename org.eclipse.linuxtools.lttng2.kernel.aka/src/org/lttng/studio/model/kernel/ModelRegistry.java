package org.lttng.studio.model.kernel;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.lttng.studio.reader.TraceReader;
import org.lttng.studio.reader.handler.IModelKeys;

public class ModelRegistry {

	HashMap<Object, HashMap<Class<?>, Object>> reg;
	HashMap<Class<? extends ITraceModel>, IModelKeys> typeModelKey;

	static final Object sharedKey = new Object();

	public ModelRegistry() {
		reg = new HashMap<Object, HashMap<Class<?>, Object>>();
		typeModelKey = new HashMap<Class<? extends ITraceModel>, IModelKeys>();
	}

	public void init(TraceReader reader) {
		List<CtfTmfTrace> traceList = reader.getTraceList();

		for (Entry<Class<? extends ITraceModel>, IModelKeys> entry: typeModelKey.entrySet()) {
			switch(entry.getValue()){
			case PER_TRACE:
				createForEach(entry.getKey(), traceList, reader);
				break;
			case PER_HOST:
				createForEach(entry.getKey(), hostList, reader);
			case SHARED:
				createOne(entry.getKey(), sharedKey, reader);
				break;
			default:
				break;
			}
		}
	}

	private void createForEach(Class<? extends ITraceModel> key, Collection<? extends Object> context, TraceReader reader) {
		for (Object ctx: context) {
			createOne(key, ctx, reader);
		}
	}

	private void createOne(Class<? extends ITraceModel> key, Object context, TraceReader reader) {
		ITraceModel model = getOrCreateModel(context, key);
		model.init(reader);
	}

	public HashMap<Class<?>, Object> getOrCreateContext(Object context) {
		if (!reg.containsKey(context)) {
			reg.put(context, new HashMap<Class<?>, Object>());
		}
		return reg.get(context);
	}

	public <T extends ITraceModel> T getOrCreateModel(Object context, Class<T> klass) {
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

	public <T extends ITraceModel> T getModel(Object context, Class<T> klass) {
		if (!reg.containsKey(context))
			return null;
		return klass.cast(reg.get(context).get(klass));
	}

	public void registerType(IModelKeys modelKey, Class<? extends ITraceModel> klass) {
		typeModelKey.put(klass, modelKey);
	}

}
