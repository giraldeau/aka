package org.lttng.studio.collect;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BinarySearch {

	public static <T> int floor(List<? extends Comparable <? super T>> list, T key) {
		return BinarySearch.floor(list, key, null);
	}

	public static <T> int floor(List<? extends T> list, T key, Comparator<? super T> c) {
		int i = Collections.binarySearch(list, key, c);
		if (i < 0)
			i = -i - 2;
		return i;
	}

	public static <T> int ceiling(List<? extends T> list, T key) {
		return BinarySearch.ceiling(list, key, null);
	}

	public static <T> int ceiling(List<? extends T> list, T key, Comparator<? super T> c) {
		int i = Collections.binarySearch(list, key, c);
		int x = i;
		if (i < 0)
			x = -i - 1;
		if (x >= list.size())
			x = i;
		return x;
	}

	public static <T> List<? extends Comparable<? super T>> range(List<? extends Comparable<? super T>> list, T from, T to) {
		int fromIndex = BinarySearch.floor(list, from);
		int toIndex = BinarySearch.ceiling(list, to);
		if (fromIndex < 0)
			fromIndex = 0;
		if (toIndex < 0)
			toIndex = list.size();
		return list.subList(fromIndex, toIndex);
	}

	public static <T> List<? extends T> range(List<? extends T> list, T from, T to, Comparator<? super T> c) {
		int fromIndex = BinarySearch.floor(list, from, c);
		int toIndex = BinarySearch.ceiling(list, to, c);
		if (fromIndex < 0)
			fromIndex = 0;
		if (toIndex < 0)
			toIndex = list.size();
		return list.subList(fromIndex, toIndex);
	}

}
