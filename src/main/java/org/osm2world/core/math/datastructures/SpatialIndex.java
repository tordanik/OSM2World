package org.osm2world.core.math.datastructures;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.osm2world.core.math.BoundedObject;

/**
 * index structure intended to speed up retrieval of candidates for intersection and overlap tests
 */
public interface SpatialIndex<T extends BoundedObject> {

	/**
	 * inserts the element into the index structure
	 */
	public void insert(T e);

	/**
	 * returns the leaves containing nearby elements in the index structure
	 *
	 * @return leaves the element would end up in. A subset of {@link #getLeaves()}.
	 */
	public Collection<? extends Iterable<T>> probeLeaves(BoundedObject e);

	/**
	 * returns all nearby elements contained in the index structure.
	 * Amounts to a flattening of {@link #probeLeaves(BoundedObject)}.
	 */
	public default Iterable<T> probe(BoundedObject e) {

		Collection<? extends Iterable<T>> leaves = probeLeaves(e);

		if (leaves.size() == 1) {
			return leaves.iterator().next();
		} else {
			// collect and de-duplicate elements from all the leaves
			Set<T> elementSet = new HashSet<>();
			leaves.forEach(it -> it.forEach(elementSet::add));
			return elementSet;
		}

	}

	/**
	 * inserts the element into the index structure,
	 * and returns all nearby elements contained in the index structure
	 *
	 * @return leaves the element ends up in. A subset of {@link #getLeaves()}.
	 * The leaves already contain the new element.
	 */
	public default Iterable<T> insertAndProbe(T e) {
		insert(e);
		return probe(e);
	}

	/**
	 * returns all leaves of this index structure
	 *
	 * @return duplicate-free groups of elements
	 */
	public abstract Iterable<? extends Iterable<T>> getLeaves();

}
