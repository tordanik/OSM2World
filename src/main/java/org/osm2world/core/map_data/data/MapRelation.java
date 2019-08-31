package org.osm2world.core.map_data.data;

import static de.topobyte.osm4j.core.model.util.OsmModelUtil.getTagsAsMap;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.plugins.graphview.core.data.MapBasedTagGroup;
import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;

import de.topobyte.osm4j.core.model.iface.OsmRelation;

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
	public static abstract class Element implements MapElement {

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

	private final OsmRelation osmRelation;
	private final List<Membership> memberships = new ArrayList<MapRelation.Membership>();

	public MapRelation(OsmRelation osmRelation) {
		this.osmRelation = osmRelation;
	}

	public List<Membership> getMemberships() {
		return memberships;
	}

	/** add a membership to the relation. Also adds it to the element at the same time! */
	public void addMembership(String role, Element element) {
		Membership membership = new Membership(this, role, element);
		memberships.add(membership);
		element.addMembership(membership);
	}

	public OsmRelation getOsmElement() {
		return osmRelation;
	}

	public TagGroup getTags() {
		return new MapBasedTagGroup(getTagsAsMap(osmRelation));
	}

	@Override
	public String toString() {
		return "r" + osmRelation.getId();
	}

}
