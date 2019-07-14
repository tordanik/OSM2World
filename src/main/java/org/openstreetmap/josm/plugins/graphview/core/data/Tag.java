package org.openstreetmap.josm.plugins.graphview.core.data;

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
		if (!(obj instanceof Tag)) {
			return false;
		} else {
			Tag otherTag = (Tag)obj;
			return key.equals(otherTag.key) && value.equals(otherTag.value);
		}
	}

	@Override
	public int hashCode() {
		return key.hashCode() + value.hashCode(); //TODO: might be a less than optimal hash function
	}

	@Override
	public String toString() {
		return key + "=" + value;
	}

}
