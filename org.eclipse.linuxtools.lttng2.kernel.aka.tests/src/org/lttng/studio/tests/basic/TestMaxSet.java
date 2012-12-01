package org.lttng.studio.tests.basic;

import static org.junit.Assert.assertEquals;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.collect.MinMaxPriorityQueue.Builder;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

public class TestMaxSet {

	@Test
	public void testMaxHeap() {
		int size = 10;
		Set<Long> exp = new TreeSet<Long>();
		Builder<Long> builder2 = MinMaxPriorityQueue.orderedBy(new Comparator<Long>() {
			@Override
			public int compare(Long self, Long other) {
				return other.compareTo(self);  // max heap
				//return self.compareTo(other); // min heap
			}
		});
		builder2.maximumSize(size);
		MinMaxPriorityQueue<Long> heap = builder2.create();


		for(long i = -100; i < 100; i++) {
			heap.add(i);
		}
		for (long i = 90; i < 100; i++) {
			exp.add(i);
		}
		/*
		System.out.println(exp);
		System.out.println(set2);
		*/
		SetView<Long> diff = Sets.symmetricDifference(exp, new TreeSet<Long>(heap));
		assertEquals(0, diff.size());

	}

}
