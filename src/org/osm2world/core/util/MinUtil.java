package org.osm2world.core.util;

import com.google.common.base.Function;

public final class MinUtil {
			
	/** prevents instantiation */
	private MinUtil() { }
	
	/**
	 * from an iterable of objects, this returns the one
	 * with the lowest associated value of a function f
	 * 
	 * @return  any of the inputs that generate the minimum function value,
	 *          null if objects is empty
	 */
	public static final <T> T min(Iterable<T> objects, Function<T, Double> f) {
		
		T currentMinObject = null;
		Double currentMinValue = null;
		
		for (T object : objects) {
			if (currentMinObject == null
					|| f.apply(object) < currentMinValue) {
				currentMinObject = object;
				currentMinValue = f.apply(object);
			}
		}
		
		return currentMinObject;
		
	}

}
