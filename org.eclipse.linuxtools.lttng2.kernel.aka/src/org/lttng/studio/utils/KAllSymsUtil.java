package org.lttng.studio.utils;

import java.io.IOException;
import java.math.BigInteger;


public class KAllSymsUtil {

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Error: missing arguments. Provides System.map file followed by address");
			return;
		}
		KAllSyms syms = new KAllSyms();
		try {
			syms.load(args[0]);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		System.out.println(String.format("%d symbols loaded", syms.getSymbols().size()));
		int i;
		for (i = 1; i < args.length; i++) {
			String sym = syms.getSymbol(new BigInteger(args[i], 16));
			System.out.println(args[i] + " " + sym);
		}
	}
}
