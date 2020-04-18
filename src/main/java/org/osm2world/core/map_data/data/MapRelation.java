package org.osm2world.core.map_data.data;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A relation from an OSM dataset.
 * Multipolygon relations will have been turned into {@link MapArea}s instead.
 *
 * @See {@link MapData} for context
 */
public class MapRelation {

	/**
	 * TODO: eventually merge with {@link MapElement}
	 * Something that can be a member of a relation
	 */
	public static abstract class Element {

		private List<Membership> memberships = emptyList();

		/** returns all relation memberships containing this element */
		public Collection<Membership> getMemberships() {
			return memberships;
		}

		private void addMembership(Membership membership) {

			assert membership.getElement() == this;

			if (memberships.isEmpty()) {
				memberships = new ArrayList<>();
			}

			memberships.add(membership);

		}

		public abstract long getId();

		/** see {@link MapElement#getTags()} */
		public abstract TagSet getTags();

		/** returns the element's id, prefixed with a letter (n for node, w for way, r for relation) */
		@Override
		public abstract String toString();

	}

	/** a member of the relation along with its role */
	public static class Membership {

		private final MapRelation relation;
		private final String role;
		private final Element element;

		public Membership(MapRelation relation, String role, Element element) {
			this.relation = relation;
			this.role = role;
			this.element = element;
		}

		public MapRelation getRelation() {
			return relation;
		}

		public String getRole() {
			return role;
		}

		public Element getElement() {
			return element;
		}

	}

	private final long id;
	private final TagSet tags;

	public MapRelation(long id, TagSet tags) {
		this.id = id;
		this.tags = tags;
	}

	private final List<Membership> memberships = new ArrayList<MapRelation.Membership>();

	public List<Membership> getMemberships() {
		return memberships;
	}

	/** add a membership to the relation. Also adds it to the element at the same time! */
	public void addMembership(String role, Element element) {
		Membership membership = new Membership(this, role, element);
		memberships.add(membership);
		element.addMembership(membership);
	}

	public TagSet getTags() {
		return tags;
	}

	@Override
	public String toString() {
		return "r" + id;
	}

}
