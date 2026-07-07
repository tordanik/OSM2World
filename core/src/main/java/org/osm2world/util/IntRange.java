package org.osm2world.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.ToIntFunction;

import javax.annotation.Nonnull;

public record IntRange(int min, int max) implements Iterable<Integer> {

	public IntRange {
		if (min > max) {
			throw new IllegalArgumentException("min must be <= max");
		}
	}

	@Override
	public @Nonnull String toString() {
		return min + ".." + max;
	}

	public int size() {
		return max - min + 1;
	}

	@Override
	public @Nonnull Iterator<Integer> iterator() {
		return new Iterator<>() {
			int next = min;

			@Override
			public boolean hasNext() {
				return next <= max;
			}

			@Override
			public Integer next() {
				return next++;
			}
		};
	}

	/** returns the range between the minimum and maximum value of a list of integers */
	public static IntRange around(List<Integer> values) {
		return new IntRange(Collections.min(values), Collections.max(values));
	}

	public static <T> IntRange around(List<T> values, ToIntFunction<T> mapToInt) {
		return around(values.stream().mapToInt(mapToInt).boxed().toList());
	}

}
