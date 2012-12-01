package org.lttng.studio.tests.collect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;

import org.junit.Test;
import org.lttng.studio.collect.ArrayListBiMultimap;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

public class TestArrayListBiMultimap {

	boolean verbose = false;

	public class ArrayListBiMultimapWrapper<K, V> extends ArrayListBiMultimap<K, V> {
		public ArrayListBiMultimapWrapper() {
			super();
		}
		public ArrayListMultimap<K, V> getForwardMap() {
			return this.forward;
		}
		public HashMap<V, K> getReverseMap() {
			return this.reverse;
		}
		public void checkInvariant() {
			// forward values are unique
			HashSet<V> forwardValues = new HashSet<V>(forward.values());
			if (forwardValues.size() != forward.values().size())
				throw new RuntimeException("ERROR: forward values not unique");

			// reverse keys is equal to forward values
			HashSet<V> reverseKeys = new HashSet<V>(reverse.keySet());
			SetView<V> d1 = Sets.symmetricDifference(reverseKeys, forwardValues);
			if (d1.size() != 0)
				throw new RuntimeException("ERROR: forward.values != reverse.keys");

			// reverse unique values is equal to forward keys
			HashSet<K> reverseUniqueValues = new HashSet<K>(reverse.values());
			SetView<K> d2 = Sets.symmetricDifference(forward.keySet(), reverseUniqueValues);
			if (d2.size() != 0)
				throw new RuntimeException("ERROR: unique forward.keys != unique reverse.values");

			// reverse values is equal to forward keys, including duplicates
			if (reverse.values().size() != forward.keys().size())
				throw new RuntimeException("ERROR: unique forward.keys != unique reverse.values");
		}
	}

	public class Samples {
		String[] keys;
		String[] values;
	}

	Samples samples = new Samples();
	static int numKeys = 2;
	static int numValues = 2;
	static int numEntries = numKeys * numValues;
	static ArrayListBiMultimapWrapper<String, String> map;

	public ArrayListBiMultimapWrapper<String, String> getMap() {
		// Map of 8 keys with 8 values each
		ArrayListBiMultimapWrapper<String, String> m = new ArrayListBiMultimapWrapper<String, String>();
		samples.keys = new String[numKeys];
		samples.values = new String[numEntries];
		for (int i = 0; i < numKeys; i++) {
			samples.keys[i] = Integer.toHexString(i);
			int offset = i * numValues;
			for (int j = offset + 0; j < offset + numValues; j++) {
				samples.values[j] = Integer.toHexString(j);
				m.put(samples.keys[i], samples.values[j]);
				m.checkInvariant();
			}
			m.checkInvariant();
		}
		return m;
	}

	public <K, V> void printTestMap(ArrayListBiMultimapWrapper<K, V> map) {
		System.out.println("forward:");
		System.out.println(map.getForwardMap().toString());
		System.out.println("reverse:");
		System.out.println(map.getReverseMap().toString());
	}

	@Test
	public void testMapCreate() {
		map = getMap();
		assertEquals(numKeys, map.keySet().size());
		assertEquals(numEntries, map.values().size());
	}

	@Test
	public void testContains() {
		map = getMap();
		String k = samples.keys[0];
		String v = samples.values[0];

		assertTrue(map.getForwardMap().containsKey(k));
		assertTrue(map.getForwardMap().containsValue(v));
		assertTrue(map.getForwardMap().containsEntry(k, v));

		assertTrue(map.getReverseMap().containsKey(k));
		assertTrue(map.getReverseMap().containsValue(v));

		map.checkInvariant();
		if (verbose)
			printTestMap(map);
	}

	@Test
	public void testRemoveKey() {
		map = getMap();
		map.remove(samples.keys[0], samples.values[0]);
		checkEntries(numKeys, numEntries - 1);
	}

	@Test
	public void testAddDuplicateValue() {
		map = getMap();
		String key = "foo";
		String value = "bar";
		boolean r1 = map.put(key, value);
		boolean r2 = map.put(key, value);
		assertTrue(r1);
		assertFalse(r2);
		checkEntries(numKeys + 1, numEntries + 1);
	}

	@Test
	public void testRemoveInexistingValue() {
		map = getMap();
		String key = "foo";
		String value = "bar";
		map.remove(key, value);
		checkEntries(numKeys, numEntries);
	}

	@Test
	public void testRemoveAllValuesOfAKey() {
		map = getMap();
		String key = "foo";
		String value1 = "bar";
		String value2 = "baz";

		map.put(key, value1);
		map.put(key, value2);
		checkEntries(numKeys + 1, numEntries + 2);

		map.remove(key, value1);
		map.remove(key, value2);
		checkEntries(numKeys, numEntries);

	}

	public void checkEntries(int numKeys, int entries) {
		assertEquals(numKeys, map.getForwardMap().keySet().size());
		assertEquals(entries, map.getForwardMap().values().size());

		assertEquals(entries, map.getReverseMap().keySet().size());
		assertEquals(entries, map.getReverseMap().values().size());
		map.checkInvariant();

		if (verbose)
			printTestMap(map);
	}

}
