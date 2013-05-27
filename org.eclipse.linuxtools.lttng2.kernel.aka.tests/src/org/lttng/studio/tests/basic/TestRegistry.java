package org.lttng.studio.tests.basic;

import org.junit.Test;
import org.lttng.studio.model.kernel.ModelRegistry;
import org.lttng.studio.reader.handler.IModelKeys;

public class TestRegistry {

	@Test
	public void testRegistryShared() {
		ModelRegistry registry = new ModelRegistry();
		registry.registerType(IModelKeys.SHARED, Object.class);
	}

}
