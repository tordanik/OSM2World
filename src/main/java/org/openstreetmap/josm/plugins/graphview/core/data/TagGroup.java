package org.openstreetmap.josm.plugins.graphview.core.data;

import java.util.List;

/**
 * represents a group of OSM tags (e.g. all tags of a way).
 * TagGroups are expected to be immutable, so modifying the tags means creation of a new group.
 * This interface requires that keys are unique, which is guaranteed since OSM API 0.6.
 */
public interface TagGroup extends Iterable<Tag> {

	/**
	 * returns the value for the given key or null if no tag in this group uses that key
	 * @param key  key whose value will be returned; != null
	 */
	public String getValue(String key);

	/**
	 * returns true if this tag group contains a tag with the given key
	 * @param key  key to check for; != null
	 */
	public boolean containsKey(String key);

	/**
	 * returns true if this tag group contains a tag with one of the given keys
	 * @param keys  keys to check for; != null
	 */
	public boolean containsAnyKey(Iterable<String> keys);

	/**
	 * returns a List of the keys found in this tag group. Empty list if no match was found
	 * @param keys keys to check for; != null
	 */
	public List<String> containsWhichKeys(Iterable<String> keys);

	/**
	 * returns true if this tag group contains at least one tag with the given value
	 * @param value  value to check for; != null
	 */
	public boolean containsValue(String value);

	/**
	 * returns true if this tag group contains at least one tag with one of the given values
	 * @param values  values to check for; != null
	 */
	public boolean containsAnyValue(Iterable<String> values);

	/**
	 * returns true if this tag group contains the given tag
	 * @param tag  tag to check for; != null
	 */
	public boolean contains(Tag tag);

	/**
	 * returns true if this tag group contains one of the given tags
	 * @param tags  tags to check for; != null
	 */
	public boolean containsAny(Iterable<Tag> tags);

	/**
	 * returns true if this tag group contains the tag
	 * @param key    key of the tag to check for; != null
	 * @param value  value of the tag to check for; != null
	 */
	public boolean contains(String key, String value);

	/**
	 * returns true if this tag group contains one of the keys
	 * with value
	 * @param keys   keys of the tag to check for; != null
	 * @param value  value of the tag to check for; != null
	 */
	public boolean containsAny(Iterable<String> keys, String value);

	/**
	 * returns true if this tag group contains one of the keys
	 * with one of the values
	 * @param keys 	   keys of the tag to check for; != null
	 * @param values  values of the tag to check for; != null
	 */
	public boolean containsAny(Iterable<String> keys, Iterable<String> values);

	/**
	 * returns true if this tag group contains the key with
	 * one of the values
	 * @param key     key of the tag to check for; != null
	 * @param values  values of the tag to check for; != null
	 */
	public boolean containsAny(String key, Iterable<String> values);

	/**
	 * returns the number of tags in this group
	 */
	public int size();

	/**
	 * returns true if this group contains any tags
	 */
	public boolean isEmpty();

}