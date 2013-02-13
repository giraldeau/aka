package org.lttng.studio.tests.collect;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.lttng.studio.collect.BinarySearch;

public class TestBinarySearch {

	@Test
	public void testBinarySearch1() {
		List<Integer> list = new ArrayList<Integer>();
		// [100, 200, 300, 400, 500, 600, 700, 800, 900] size = 9
		for (int i = 100; i < 1000; i += 100) {
			list.add(i);
		}

		int[][] floor = new int[][] { {Integer.MIN_VALUE, -1}, {0, -1},
				{99, -1}, {100, 0}, {101, 0}, {899, 7}, {900, 8},
				{901, 8}, {Integer.MAX_VALUE, 8} };
		int[][] ceiling = new int[][] { {Integer.MIN_VALUE, 0}, {0, 0},
				{99, 0}, {100, 0}, {101, 1}, {899, 8}, {900, 8},
				{901, -10}, {Integer.MAX_VALUE, -10} };
		for (int i = 0; i < floor.length; i++) {
			assertEquals(floor[i][1], BinarySearch.floor(list, floor[i][0]));
			assertEquals(ceiling[i][1], BinarySearch.ceiling(list, ceiling[i][0]));
			//System.out.println("exp = " + floor[i][1] + " floor(" + floor[i][0] + ") = " + BinarySearch.floor(list, floor[i][0]));
			//System.out.println("exp = " + ceiling[i][1] + " ceiling(" + ceiling[i][0] + ") = " + BinarySearch.ceiling(list, ceiling[i][0]));
		}
	}

}
