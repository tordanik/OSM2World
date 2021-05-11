package org.osm2world.core.map_data.data;

import static java.util.Arrays.sort;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.collect.Iterators;

/**
 * represents a group of OSM tags (e.g. all tags of a way).
 * TagSets are immutable, so modifying the tags means creation of a new set.
 * Keys are unique, which is required since OSM API 0.6.
 * The order of the tags does not matter. (Tags are provided in alphabetical order for iteration.)
 */
public final class TagSet implements Iterable<Tag> {

	private static final TagSet EMPTY_SET = new TagSet(new Tag[0]);

	/** the backing array. Will not be modified after construction. Sorted alphabetically (for equality behavior). */
	private final Tag[] tags;

	private TagSet(Tag[] tags) {

		this.tags = tags;
		sort(tags, Comparator.comparing((Tag t) -> t.key).thenComparing(t -> t.value));

		// validate uniqueness of keys (relies on sort order)
		for (int i = 0; i + 1 < tags.length; i++) {
			if (tags[i].key.equals(tags[i + 1].key)) {
				throw new IllegalArgumentException("duplicate key '" + tags[i].key + "' in: " + Arrays.toString(tags));
			}
		}

	}

	public static final TagSet of() {
		return EMPTY_SET;
	}

	/**
	 * creates a {@link TagSet} from tags
	 * @throws IllegalArgumentException  if keys are not unique
	 */
	public static final TagSet of(Collection<Tag> tags) {
		switch (tags.size()) {
		case 0: return EMPTY_SET;
		default: return new TagSet(tags.toArray(new Tag[0]));
		}
	}

	/**
	 * creates a {@link TagSet} from tags
	 * @throws IllegalArgumentException  if keys are not unique
	 */
	public static final TagSet of(Tag... tags) {
		switch (tags.length) {
		case 0: return EMPTY_SET;
		default: return new TagSet(tags.clone());
		}
	}

	/**
	 * creates a {@link TagSet} from keys and values
	 * @param keyValuePairs  alternating keys and values: key0, value0, key1, value1, ...
	 * @throws IllegalArgumentException  if keys are not unique, the tags would invalid, or for odd numbers of strings
	 */
	public static final TagSet of(String... keyValuePairs) {

		if (keyValuePairs.length % 2 != 0) {
			throw new IllegalArgumentException("there must be one value for each key, an even number of strings");
		}

		if (keyValuePairs.length == 0) return EMPTY_SET;

		Tag[] tags = new Tag[keyValuePairs.length / 2];
		for (int i = 0; i < tags.length; i++) {
			tags[i] = new Tag(keyValuePairs[i * 2], keyValuePairs[i * 2 + 1]);
		}
		return new TagSet(tags);

	}

	/** returns true if this set contains any tags */
	public boolean isEmpty() {
		return tags.length == 0;
	}

	/** returns the number of tags in this set */
	public int size() {
		return tags.length;
	}

	/**
	 * returns the value for the given key or null if no tag in this set uses that key
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
	 * returns true if this tag set contains the given tag
	 * @param tag  tag to check for; != null
	 */
	public boolean contains(Tag tag) {
		assert tag != null;
		for (Tag t : tags) {
			if (t.equals(tag)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * returns true if this tag set contains the given tag
	 * @param key  key of the tag to check for; != null
	 * @param value  value of the tag to check for; != null
	 */
	public boolean contains(String key, String value) {
		return contains(new Tag(key, value));
	}

	/**
	 * returns true if this tag set contains a tag with the given key
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
	 * returns true if this tag set contains at least one tag with the given value
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
	 * returns true if this tag set contains one of the given tags
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
	 * returns true if this tag set contains at least one of the keys with one of the values
	 * @param keys    keys of the tag to check for; null to allow any key
	 * @param values  values of the tag to check for; null to allow any value
	 */
	public boolean containsAny(@Nullable Collection<String> keys, @Nullable Collection<String> values) {
		if (keys == null && values == null) {
			return !isEmpty();
		} else if (keys == null) {
			return values.stream().anyMatch(it -> containsValue(it));
		} else if (values == null) {
			return keys.stream().anyMatch(it -> containsKey(it));
		} else {
			for (String key : keys) {
				String value = getValue(key);
				if (values.contains(value)) {
					return true;
				}
			}
			return false;
		}
	}

	/** returns a stream of all tags in this set */
	public Stream<Tag> stream() {
		return Arrays.stream(tags);
	}

	/**
	 * returns an {@link Iterator} providing access to all {@link Tag}s.
	 * The Iterator does not support the {@link Iterator#remove()} method.
	 */
	@Override
	public Iterator<Tag> iterator() {
		return Iterators.forArray(tags);
	}

	/** two {@link TagSet}s are equal iff they contain the same tags */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TagSet) {
			return Arrays.equals(this.tags, ((TagSet) obj).tags);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return tags.hashCode();
	}

	@Override
	public String toString() {
		return Arrays.toString(tags);
	}

}