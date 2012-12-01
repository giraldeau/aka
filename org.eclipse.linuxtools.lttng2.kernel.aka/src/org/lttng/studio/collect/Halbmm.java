package org.lttng.studio.collect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/*
 * Manages links between system object and vertex
 *
 * Halbmm stands for Heterogeneous Array List Bidirectional Multi Map
 */

public class Halbmm <E> {

	/**
	 * Based on item 29: Typesafe heterogeneous container
	 * Effective Java Second Edition, Joshua Blosh
	 */
	HashMap<Class<?>, ArrayListBiMultimap<Object, E>> ownerMap;

	public Halbmm() {
		ownerMap = new HashMap<Class<?>, ArrayListBiMultimap<Object, E>>();
	}

	private <T> ArrayListBiMultimap<Object, E> getOrCreateContainer(Class<T> type) {
		if (type == null)
			throw new NullPointerException("Type is null");
		ArrayListBiMultimap<Object, E> map = ownerMap.get(type);
		if (map == null) {
			map = new ArrayListBiMultimap<Object, E>();
			ownerMap.put(type, map);
		}
		return map;
	}

	public <T> List<E> get(Class<T> type, T object) {
		return getOrCreateContainer(type).get(object);
	}

	public <T> E first(Class<T> type, T object) {
		List<E> v = get(type, object);
		if (v.size() > 0)
			return v.get(0);
		return null;
	}

	public <T> E last(Class<T> type, T object) {
		List<E> v = get(type, object);
		if (v.size() > 0)
			return v.get(v.size() - 1);
		return null;
	}

	public <T> boolean add(Class<T> type, T object, E instance) {
		return get(type, object).add(instance);
	}

	public <T> T getReverse(Class<T> type, E object) {
		if (type == null)
			throw new NullPointerException("Type is null");
		if (object == null)
			throw new NullPointerException("Object is null");
		if (!ownerMap.containsKey(type))
			return null;
		Map<E, Object> map = ownerMap.get(type).inverse();
		if (map.containsKey(object))
			return type.cast(map.get(object));
		return null;
	}

	public Object getReverseRaw(E object) {
		if (object == null)
			throw new NullPointerException("Object is null");
		for (Class<?> type: ownerMap.keySet()) {
			Map<E, Object> inverse = ownerMap.get(type).inverse();
			if (inverse.containsKey(object)) {
				return inverse.get(object);
			}
		}
		return null;
	}

	public static <T> Halbmm<T> create() {
		return new Halbmm<T>();
	}

}
