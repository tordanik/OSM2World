package org.osm2world.core.osm.ruleset;

import static java.util.Arrays.asList;

import java.util.Collection;
import java.util.HashSet;

import org.osm2world.core.map_data.data.Tag;
import org.osm2world.core.world.modules.SurfaceAreaModule;

import de.topobyte.osm4j.core.model.iface.OsmTag;

public class HardcodedRuleset implements Ruleset {

	private static Collection<Tag> areaTags = new HashSet<>();
	private static Collection<String> areaKeys = new HashSet<>();

	private static Collection<Tag> landTags = new HashSet<>();
	private static Collection<Tag> seaTags = new HashSet<>();

	private static final Collection<String> relationTypeWhitelist;

	static {
		areaTags.add(new Tag("area", "yes"));
		areaTags.add(new Tag("aeroway", "apron"));
		areaTags.add(new Tag("aeroway", "helipad"));
		areaTags.add(new Tag("amenity", "bicycle_parking"));
		areaTags.add(new Tag("amenity", "fountain"));
		areaTags.add(new Tag("amenity", "parking"));
		areaTags.add(new Tag("amenity", "parking_space"));
		areaTags.add(new Tag("amenity", "swimming_pool"));
		areaTags.add(new Tag("leisure", "pitch"));
		areaTags.add(new Tag("leisure", "swimming_pool"));
		areaTags.add(new Tag("natural", "beach"));
		areaTags.add(new Tag("natural", "sand"));
		areaTags.add(new Tag("natural", "water"));
		areaTags.add(new Tag("natural", "wood"));
		areaTags.add(new Tag("natural", "scrub"));
		areaTags.add(new Tag("power", "generator"));
		areaTags.add(new Tag("waterway", "riverbank"));
		areaTags.add(new Tag("indoor", "room"));
		areaTags.add(new Tag("indoor", "area"));
		areaTags.add(new Tag("indoor", "corridor"));

		areaTags.addAll(SurfaceAreaModule.defaultSurfaceMap.keySet());

		areaKeys.add("area:highway");
		areaKeys.add("bridge:support");
		areaKeys.add("building");
		areaKeys.add("building:part");
		areaKeys.add("golf");
		areaKeys.add("landuse");
		areaKeys.add("landcover");

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
	public boolean isAreaTag(OsmTag tag) {
		return areaKeys.contains(tag.getKey())
			|| areaTags.contains(new Tag(tag.getKey(), tag.getValue()));
	}

	@Override
	public boolean isLandTag(OsmTag tag) {
		return landTags.contains(new Tag(tag.getKey(), tag.getValue()));
	}

	@Override
	public boolean isSeaTag(OsmTag tag) {
		return seaTags.contains(new Tag(tag.getKey(), tag.getValue()));
	}

	@Override
	public boolean isWhitelistedRelationType(String type) {
		return relationTypeWhitelist.contains(type);
	}

}
