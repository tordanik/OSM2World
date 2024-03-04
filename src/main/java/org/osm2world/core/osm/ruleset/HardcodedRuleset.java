package org.osm2world.core.osm.ruleset;

import de.topobyte.osm4j.core.model.iface.OsmTag;
import org.osm2world.core.map_data.data.Tag;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.world.modules.SurfaceAreaModule;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;

public class HardcodedRuleset implements Ruleset {

	private static final Collection<Tag> areaTags = new HashSet<>();
	private static final Collection<String> areaKeys = new HashSet<>();

	private static final Collection<Tag> landTags = new HashSet<>();
	private static final Collection<Tag> seaTags = new HashSet<>();

	private static final Collection<String> relationTypeWhitelist;

	static {
		areaTags.add(new Tag("area", "yes"));
		areaTags.add(new Tag("aeroway", "apron"));
		areaTags.add(new Tag("aeroway", "helipad"));
		areaTags.add(new Tag("amenity", "bicycle_parking"));
		areaTags.add(new Tag("amenity", "fountain"));
		areaTags.add(new Tag("amenity", "parking"));
		areaTags.add(new Tag("amenity", "parking_space"));
		areaTags.add(new Tag("indoor", "area"));
		areaTags.add(new Tag("indoor", "corridor"));
		areaTags.add(new Tag("indoor", "room"));
		areaTags.add(new Tag("leisure", "pitch"));
		areaTags.add(new Tag("leisure", "swimming_pool"));
		areaTags.add(new Tag("natural", "beach"));
		areaTags.add(new Tag("natural", "sand"));
		areaTags.add(new Tag("natural", "water"));
		areaTags.add(new Tag("natural", "wood"));
		areaTags.add(new Tag("natural", "scrub"));
		areaTags.add(new Tag("natural", "shrubbery"));
		areaTags.add(new Tag("power", "generator"));
		areaTags.add(new Tag("tourism", "artwork"));
		areaTags.add(new Tag("waterway", "riverbank"));

		areaTags.addAll(SurfaceAreaModule.defaultSurfaceMap.keySet());

		areaKeys.add("area:highway");
		areaKeys.add("bridge:support");
		areaKeys.add("building");
		areaKeys.add("building:part");
		areaKeys.add("golf");
		areaKeys.add("landuse");
		areaKeys.add("landcover");
		areaKeys.add("leisure");

		landTags.add(new Tag("landuse", "forest"));
		landTags.add(new Tag("natural", "water"));
		landTags.add(new Tag("natural", "wood"));
		landTags.add(new Tag("waterway", "river"));
		landTags.add(new Tag("waterway", "stream"));

		seaTags.add(new Tag("maritime", "yes"));
		seaTags.add(new Tag("route", "ferry"));
		seaTags.add(new Tag("seamark", "buoy"));
		seaTags.add(new Tag("seamark:type", "buoy_cardinal"));
		seaTags.add(new Tag("seamark:type", "buoy_isolated_danger"));
		seaTags.add(new Tag("seamark:type", "buoy_lateral"));
		seaTags.add(new Tag("seamark:type", "buoy_safe_water"));
		seaTags.add(new Tag("seamark:type", "buoy_special_purpose"));
		seaTags.add(new Tag("seamark:type", "cable_submarine"));
		seaTags.add(new Tag("submarine", "yes"));
		seaTags.add(new Tag("wetland", "tidalflat"));

		relationTypeWhitelist = asList(
				"multipolygon",
				"destination_sign",
				"building",
				"enforcement",
				"bridge",
				"connectivity",
				"tunnel"
				);

	}

	@Override
	public boolean isAreaTag(Tag tag) {
		return areaKeys.contains(tag.key) || areaTags.contains(tag);
	}

	@Override
	public boolean isLandTag(OsmTag tag) {
		return landTags.contains(new Tag(tag.getKey(), tag.getValue()));
	}

	@Override
	public boolean isSeaTag(OsmTag tag) {
		return seaTags.contains(new Tag(tag.getKey(), tag.getValue()));
	}

	/**
	 * checks whether a relation is relevant to OSM2World.
	 * This is intended to filter out giant relations for things like place names
	 * which cause performance issues (e.g. during intersection checks)
	 * despite not being visible in a 3D rendering.
	 *
	 * @param tags  the relation's tags
	 */
	@Override
	public boolean isRelevantRelation(TagSet tags) {
		if ("multipolygon".equals(tags.getValue("type"))) {
			// check whether the multipolygon has a relevant main tag
			// TODO once there is proper style support, check whether any of the style rules matches
			return tags.containsKey("building")
					|| tags.containsKey("building:part")
					|| tags.containsKey("landcover")
					|| tags.containsKey("highway")
					|| tags.containsKey("barrier")
					|| tags.containsKey("golf")
					|| tags.containsAny(List.of("man_made"), List.of("bridge", "tunnel"))
					|| tags.containsAny(List.of("natural"), List.of("shrubbery", "wood", "mud", "water"))
					|| tags.containsAny(List.of("landuse"), List.of("forest", "orchard"))
					|| tags.containsAny(List.of("aeroway"), List.of("apron", "helipad"))
					|| tags.containsAny(List.of("amenity"), List.of("parking", "parking_space", "bicycle_parking", "fountain"))
					|| tags.containsAny(List.of("leisure"), List.of("swimming_pool", "pitch"))
					|| tags.containsAny(List.of("power"), List.of("generator"));
		} else {
			return relationTypeWhitelist.contains(tags.getValue("type"));
		}
	}

}
