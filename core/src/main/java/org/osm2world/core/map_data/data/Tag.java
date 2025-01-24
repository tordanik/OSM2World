package org.osm2world.core.map_data.data;

/**
 * immutable representation of an OSM tag (key-value-pair)
 */
public class Tag {

	/** key of the tag; != null */
	public final String key;

	/** value of the tag; != null */
	public final String value;

	public Tag(String key, String value) {
		assert key != null && value != null;
		this.key = key;
		this.value = value;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Tag otherTag && key.equals(otherTag.key) && value.equals(otherTag.value);
	}

	@Override
	public int hashCode() {
		return 31 * key.hashCode() + value.hashCode();
	}

	@Override
	public String toString() {
		return key + "=" + value;
	}

}
