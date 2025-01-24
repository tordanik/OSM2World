package org.osm2world.core.map_data.data;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Something that can be a member of a {@link MapRelation}.
 * TODO: eventually merge with {@link MapElement}
 */
public abstract class MapRelationElement {

	private List<MapRelation.Membership> memberships = emptyList();

	/**
	 * returns all relation memberships containing this element
	 */
	public Collection<MapRelation.Membership> getMemberships() {
		return memberships;
	}

	/**
	 * adds a relation membership; must only be called from {@link MapRelation#addMember(String, MapRelationElement)}.
	 */
	void addMembership(MapRelation.Membership membership) {

		assert membership.getElement() == this;

		if (memberships.isEmpty()) {
			memberships = new ArrayList<>();
		}

		memberships.add(membership);

	}

	/**
	 * returns the element's id
	 */
	public abstract long getId();

	/**
	 * see {@link MapElement#getTags()}
	 */
	public abstract TagSet getTags();

	/**
	 * returns the element's id, prefixed with a letter (n for node, w for way, r for relation)
	 */
	@Override
	public abstract String toString();

}
