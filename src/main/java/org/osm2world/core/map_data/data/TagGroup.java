package org.osm2world.core.map_data.data;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

/**
 * represents a group of OSM tags (e.g. all tags of a way).
 * TagGroups are expected to be immutable, so modifying the tags means creation of a new group.
 * This interface requires that keys are unique, which is guaranteed since OSM API 0.6.
 */
public final class TagGroup implements Iterable<Tag> {

	private static final TagGroup EMPTY_GROUP = new TagGroup(emptyList());

	private final List<Tag> tags;

	private TagGroup(List<Tag> tags) {
		this.tags = tags;
	}

	public static final TagGroup of() {
		return EMPTY_GROUP;
	}

	/** creates a {@link TagGroup} from tags */
	public static final TagGroup of(Collection<Tag> tags) {
		switch (tags.size()) {
		case 0: return EMPTY_GROUP;
		case 1: return new TagGroup(singletonList(tags.iterator().next()));
		default: return new TagGroup(ImmutableList.copyOf(tags));
		}
	}

	/** creates a {@link TagGroup} from tags */
	public static final TagGroup of(Tag... tags) {
		switch (tags.length) {
		case 0: return EMPTY_GROUP;
		case 1: return new TagGroup(singletonList(tags[0]));
		default: return new TagGroup(ImmutableList.copyOf((tags)));
		}
	}

	/**
	 * creates a {@link TagGroup} from keys and values
	 * @param keyValuePairs  alternating keys and values: key0, value0, key1, value1, ...
	 */
	public static final TagGroup of(String... keyValuePairs) {

		if (keyValuePairs.length % 2 != 0) {
			throw new IllegalArgumentException("there must be one value for each key, an even number of strings");
		}

		Tag[] tags = new Tag[keyValuePairs.length / 2];
		for (int i = 0; i < tags.length; i++) {
			tags[i] = new Tag(keyValuePairs[i * 2], keyValuePairs[i * 2 + 1]);
		}
		return TagGroup.of(tags);

	}

	/** creates a {@link TagGroup} from a {@link Map} of keys and values */
	public static final TagGroup of(Map<String, String> keyValueMap) {
		List<Tag> tags = keyValueMap.entrySet().stream().map(e -> new Tag(e.getKey(), e.getValue())).collect(toList());
		return TagGroup.of(tags);
	}

	/** returns true if this group contains any tags */
	public boolean isEmpty() {
		return tags.isEmpty();
	}

	/** returns the number of tags in this group */
	public int size() {
		return tags.size();
	}

	/**
	 * returns the value for the given key or null if no tag in this group uses that key
	 * @param key  key whose value will be returned; != null
	 */
	public String getValue(String key) {
		assert key != null;
		for (Tag tag : tags) {
			if (tag.key.equals(key)) {
				return tag.value;
			}
		}
		return null;
	}

	/**
	 * returns true if this tag group contains the given tag
	 * @param tag  tag to check for; != null
	 */
	public boolean contains(Tag tag) {
		return tags.contains(tag);
	}

	/**
	 * returns true if this tag group contains the given tag
	 * @param key  key of the tag to check for; != null
	 * @param value  value of the tag to check for; != null
	 */
	public boolean contains(String key, String value) {
		return contains(new Tag(key, value));
	}

	/**
	 * returns true if this tag group contains a tag with the given key
	 * @param key  key to check for; != null
	 */
	public boolean containsKey(String key) {
		for (Tag tag : tags) {
			if (tag.key.equals(key)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * returns true if this tag group contains at least one tag with the given value
	 * @param value  value to check for; != null
	 */
	public boolean containsValue(String value) {
		for (Tag tag : tags) {
			if (tag.value.equals(value)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * returns true if this tag group contains one of the given tags
	 * @param tags  tags to check for; != null
	 */
	public boolean containsAny(Iterable<Tag> tags) {
		for (Tag tag : tags) {
			if (this.contains(tag)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * returns true if this tag group contains at least one of the keys with one of the values
	 * @param keys    keys of the tag to check for; != null
	 * @param values  values of the tag to check for; != null
	 */
	public boolean containsAny(Collection<String> keys, Collection<String> values) {
		for (String key : keys) {
			String value = getValue(key);
			if (values.contains(value)) {
				return true;
			}
		}
		return false;
	}

	/** returns a stream of all tags in this set */
	public Stream<Tag> stream() {
		return tags.stream();
	}

	/**
	 * returns an {@link Iterator} providing access to all {@link Tag}s.
	 * The Iterator does not support the {@link Iterator#remove()} method.
	 */
	@Override
	public Iterator<Tag> iterator() {
		return tags.iterator();
	}

	@Override
	public String toString() {
		return tags.toString();
	}

}