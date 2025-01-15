package org.osm2world.core.map_data.data;

import java.util.ArrayList;
import java.util.List;

/**
 * A relation from an OSM dataset.
 * Most multipolygon relations will have been turned into {@link MapArea}s instead.
 * (Only multipolygons with multiple outer rings will additionally be represented as a {@link MapMultipolygonRelation}.)
 *
 * @see MapData
 */
public class MapRelation extends MapRelationElement {

	/** a member of the relation along with its role */
	public static class Membership {

		private final MapRelation relation;
		private final String role;
		private final MapRelationElement element;

		public Membership(MapRelation relation, String role, MapRelationElement element) {
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

		public MapRelationElement getElement() {
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

	public List<Membership> getMembers() {
		return memberships;
	}

	/** add a membership to the relation. Also adds it to the element at the same time! */
	public void addMember(String role, MapRelationElement element) {
		Membership membership = new Membership(this, role, element);
		memberships.add(membership);
		element.addMembership(membership);
	}

	@Override
	public long getId() {
		return id;
	}

	@Override
	public TagSet getTags() {
		return tags;
	}

	@Override
	public String toString() {
		return "r" + id;
	}

}
