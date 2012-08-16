package org.osm2world.core.osm.ruleset;

import java.util.Collection;
import java.util.HashSet;

import org.openstreetmap.josm.plugins.graphview.core.data.Tag;

public class HardcodedRuleset implements Ruleset {

	private static Collection<Tag> areaTags = new HashSet<Tag>();
	private static Collection<String> areaKeys = new HashSet<String>();
	
	static {
		areaTags.add(new Tag("area", "yes"));
		areaTags.add(new Tag("amenity", "fountain"));
		areaTags.add(new Tag("amenity", "parking"));
		areaTags.add(new Tag("amenity", "swimming_pool"));
		areaTags.add(new Tag("leisure", "pitch"));
		areaTags.add(new Tag("leisure", "swimming_pool"));
		areaTags.add(new Tag("natural", "water"));
		areaTags.add(new Tag("natural", "wood"));
		areaTags.add(new Tag("waterway", "riverbank"));
		
		areaKeys.add("building");
		areaKeys.add("building:part");
		areaKeys.add("landuse");
	}
	
	@Override
	public boolean isAreaTag(Tag tag) {
		return areaKeys.contains(tag.key)
			|| areaTags.contains(tag);
	}
	
}
