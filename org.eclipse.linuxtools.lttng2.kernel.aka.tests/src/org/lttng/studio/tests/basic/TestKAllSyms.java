package org.lttng.studio.tests.basic;

import static org.junit.Assert.assertTrue;

import java.math.BigInteger;

import org.junit.Test;
import org.lttng.studio.utils.KAllSyms;

public class TestKAllSyms {

	static String line1 = "000000000000b020 D cpu_core_map\n";
	static String line2 = "000000000000b040 D cpu_sibling_map\n";
	static String line3 = "000000000000bed8 d lock_kicker_irq";

	@Test
	public void testParseLine() {
		KAllSyms syms = new KAllSyms();
		syms.parseLine(line1);
		syms.parseLine(line2);
		syms.parseLine(line3);
		String sym = syms.getSymbol(new BigInteger("b03A", 16));
		assertTrue(sym.compareTo("cpu_core_map") == 0);
	}

}
