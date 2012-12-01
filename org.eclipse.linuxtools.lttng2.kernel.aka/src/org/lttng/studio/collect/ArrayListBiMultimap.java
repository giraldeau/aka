package org.lttng.studio.collect;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

/**
 * A collection that maps keys to an array of unique values of invariant
 * type, and maintains reverse relationship for fast lookup.
 *
 * @author Francis Giraldeau
 *
 */
public class ArrayListBiMultimap<K, V> implements ListMultimap<K, V>, Multimap<K, V> {

	protected ArrayListMultimap<K, V> forward;
	protected HashMap<V, K> reverse;

	public ArrayListBiMultimap () {
		forward = ArrayListMultimap.create();
		reverse = new HashMap<V, K>();
	}

	public ArrayListBiMultimap<K, V> create() {
		return new ArrayListBiMultimap<K, V>();
	}

	@Override
	public void clear() {
		forward.clear();
		reverse.clear();
	}

	@Override
	public boolean containsEntry(Object key, Object value) {
		return forward.containsEntry(key, value);
	}

	@Override
	public boolean containsKey(Object key) {
		return forward.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return forward.containsValue(value);
	}

	@Override
	public Collection<Entry<K, V>> entries() {
		return forward.entries();
	}

	@Override
	public boolean isEmpty() {
		return forward.isEmpty();
	}

	@Override
	public Set<K> keySet() {
		return forward.keySet();
	}

	@Override
	public Multiset<K> keys() {
		return forward.keys();
	}

	@Override
	public boolean put(K key, V value) {
		if (reverse.containsKey(value)) {
			return false;
		}
		boolean ret = forward.put(key, value);
		if (ret == true) {
			reverse.put(value, key);
		}
		return ret;
	}

	@Override
	public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
		// verify key uniqueness in the reverse set
		for (Map.Entry<? extends K, ? extends V> entry: multimap.entries()) {
			if (reverse.containsKey(entry.getValue()))
					return false;
		}
		// really adds entries
		for (Map.Entry<? extends K, ? extends V> entry: multimap.entries()) {
			reverse.put(entry.getValue(), entry.getKey());
		}
		boolean ret = forward.putAll(multimap);
		return ret;
	}

	@Override
	public boolean putAll(K key, Iterable<? extends V> values) {
		if (!values.iterator().hasNext())
			return false;
		// verify uniqueness
		for (V value: values) {
			if (reverse.containsKey(value)) {
				return false;
			}
		}
		// really adds entries
		for (V value: values) {
			reverse.put(value, key);
		}
		return forward.putAll(key, values);
	}

	@Override
	public boolean remove(Object key, Object value) {
		reverse.remove(value);
		return forward.remove(key, value);
	}

	@Override
	public int size() {
		return forward.size();
	}

	@Override
	public Collection<V> values() {
		return forward.values();
	}

	@Override
	public Map<K, Collection<V>> asMap() {
		return forward.asMap();
	}

	@Override
	public List<V> get(K key) {
		return forward.get(key);
	}

	@Override
	public List<V> removeAll(Object key) {
		List<V> values = forward.removeAll(key);
		for (V value: values) {
			reverse.remove(value);
		}
		return Collections.unmodifiableList(values);
	}

	public Map<V, K> inverse() {
		return Collections.unmodifiableMap(reverse);
	}

	@Override
	public List<V> replaceValues(K key, Iterable<? extends V> values) {
		if (!values.iterator().hasNext())
			return removeAll(key);
		List<V> replaced = forward.replaceValues(key, values);
		for (V val: replaced) {
			reverse.remove(val);
		}
		for (V val: values) {
			reverse.put(val, key);
		}
		return replaced;
	}

}
