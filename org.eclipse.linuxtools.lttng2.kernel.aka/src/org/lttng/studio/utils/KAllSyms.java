package org.lttng.studio.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.TreeSet;

public class KAllSyms {

	HashMap<BigInteger, String> symbols;
	TreeSet<BigInteger> address;

	public KAllSyms() {
		symbols = new HashMap<BigInteger, String>();
		address = new TreeSet<BigInteger>();
	}

	public void load(String path) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(path));
		while(reader.ready()) {
			//000000000000b020 D cpu_core_map
			String line = reader.readLine();
			parseLine(line);
		}
		reader.close();
	}

	public void parseLine(String line) {
		String[] items = line.split(" ");
		BigInteger addr = new BigInteger(items[0], 16);
		String sym = items[2].trim();
		addSymbol(addr, sym);
	}

	public void addSymbol(BigInteger addr, String sym) {
		address.add(addr);
		symbols.put(addr, sym);
	}

	public String getSymbol(BigInteger addr) {
		BigInteger near = address.floor(addr);
		return symbols.get(near);
	}

	public void reset() {
		address.clear();
		symbols.clear();
	}

	public HashMap<BigInteger, String> getSymbols() {
		return symbols;
	}
}
