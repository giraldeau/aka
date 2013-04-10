package org.lttng.studio.utils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
		String hex = ".*(0x[a-fA-F0-9]+).*";
		Pattern pattern = Pattern.compile(hex);
		
		for (i = 1; i < args.length; i++) {
			Matcher matcher = pattern.matcher(args[i]);
			while(matcher.find()) {
				String sub = matcher.group();
				sub = sub.substring(2);
				if (sub.endsWith(","))
					sub = sub.substring(0, sub.length() - 1);
				String sym = syms.getSymbol(new BigInteger(sub, 16));
				System.out.println("0x" + sub + " " + sym);
			}
		}
	}
}
