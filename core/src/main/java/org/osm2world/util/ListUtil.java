package org.osm2world.util;

import java.util.List;

/** Utility class with static methods for working with lists. To be replaced with Java 21 methods in the future. */
public class ListUtil {

	private ListUtil() {}

	public static <T> T getFirst(List<T> list) {
		return list.get(0);
	}

	public static <T> T getLast(List<T> list) {
		return list.get(list.size() - 1);
	}

}
