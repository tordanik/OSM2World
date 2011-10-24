package org.openstreetmap.josm.plugins.graphview.core.data;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * TagGroup that uses a key-value-Map to store tags
 */
public class MapBasedTagGroup implements TagGroup {

	private final Map<String, String> tagMap;

	/**
	 * @param tagMap  map from keys to values; != null;
	 *                must not be modified after being used as parameter
	 */
	public MapBasedTagGroup(Map<String, String> tagMap) {
		if (tagMap == null) {
			throw new IllegalArgumentException();
		}

		this.tagMap = tagMap;
	}

	/**
	 * @param tags  tags to add to the group; != null, each != null
	 */
	public MapBasedTagGroup(Iterable<Tag> tags) {
		if (tags == null) {
			throw new IllegalArgumentException();
		}
		this.tagMap = new HashMap<String, String>();
		for (Tag tag : tags) {
			if (tag == null) {
				throw new IllegalArgumentException();
			} else {
				this.tagMap.put(tag.key, tag.value);
			}
		}
	}

	/**
	 * @param tags  tags to add to the group; each != null
	 */
	public MapBasedTagGroup(Tag... tags) {
		this.tagMap = new HashMap<String, String>(tags.length);
		for (Tag tag : tags) {
			if (tag == null) {
				throw new IllegalArgumentException();
			} else {
				this.tagMap.put(tag.key, tag.value);
			}
		}
	}

	public String getValue(String key) {
		assert key != null;
		return tagMap.get(key);
	}

	public boolean containsKey(String key) {
		assert key != null;
		return tagMap.containsKey(key);
	}

	public boolean containsValue(String value) {
		assert value != null;
		return tagMap.containsValue(value);
	}

	public boolean contains(Tag tag) {
		assert tag != null;
		return tag.value.equals(tagMap.get(tag.key));
	}
	
	@Override
	public boolean contains(String key, String value) {
		assert key != null;
		assert value != null;
		return value.equals(tagMap.get(key));
	}

	public int size() {
		return tagMap.size();
	}
	
	@Override
	public boolean isEmpty() {
		return tagMap.isEmpty();
	}

	/**
	 * returns an Iterator providing access to all Tags.
	 * The Iterator does not support the {@link Iterator#remove()} method.
	 */
	public Iterator<Tag> iterator() {

		Collection<Tag> tagCollection = new LinkedList<Tag>();

		for (String key : tagMap.keySet()) {
			tagCollection.add(new Tag(key, tagMap.get(key)));
		}

		return Collections.unmodifiableCollection(tagCollection).iterator();

	}

	@Override
	public String toString() {
		return tagMap.toString();
	}

}
