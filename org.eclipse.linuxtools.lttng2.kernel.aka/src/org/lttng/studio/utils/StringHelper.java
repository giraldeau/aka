package org.lttng.studio.utils;

public class StringHelper {

	public static String unquote(String str) {
		if (str.startsWith("\"") && str.endsWith("\""))
			return str.substring(1, str.length() - 1);
		else
			return str;
	}

}
