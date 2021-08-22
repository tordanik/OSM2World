package org.osm2world.core.world.modules.building;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static org.osm2world.core.util.ValueParseUtil.*;
import static org.osm2world.core.util.ValueParseUtil.parseInt;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.shapes.PolygonShapeXZ;
import org.osm2world.core.world.modules.building.LevelAndHeightData.Level.LevelType;

import com.google.common.collect.Lists;

/**
 * immutable representation of level and height info for a building part.
 * This tells you how many levels a building part has, how high (and how high up) each level is,
 * as well as some properties for each level.
 */
public class LevelAndHeightData {

	/**
	 * all the levels from bottom (including underground levels) to top (including roof levels).
	 * Will have ascending level numbers (but there may be gaps)
	 */
	public final List<Level> levels;

	public static final class Level {

		public static enum LevelType {
			/** level is entirely below ground */
			UNDERGROUND,
			/** level is at least partially above ground */
			ABOVEGROUND,
			/** level is above ground and within the roof */
			ROOF
		}

		/** the level number as used in OSM level tags */
		public final int level;

		public final LevelType type;

		public final @Nullable String name;
		public final @Nullable String ref;

		/** distance in meters between the top and bottom of this level */
		public final double height;

		/**
		 * elevation (meters) of the floor of this level relative to the building's reference elevation.
		 * Most commonly, the reference elevation is equal to the floor of the lowest non-underground level
		 * (typically level 0).
		 */
		public final double relativeEle;

		Level(int level, LevelType type, double height, double relativeEle, TagSet tags) {
			this.level = level;
			this.type = type;
			this.name = tags.getValue("name");
			this.ref = tags.containsKey("ref") ? tags.getValue("ref") : tags.getValue("level:ref");
			this.height = height;
			this.relativeEle = relativeEle;
		}

		public String label() {
			if (name != null) return name;
			if (ref != null) return ref;
			return Integer.toString(level);
		}

		public double relativeEleTop() {
			return relativeEle + height;
		}

		@Override
		public String toString() {
			return String.format("%3d %c " + relativeEle + " .. " + relativeEleTop() + " m (%s)",
					level, type.toString().charAt(0), label());
		}

	}

	/**
	 * figures out level and height information based on the building's and building part's tags.
	 * If available, explicitly tagged data is used, with tags on indoor=level elements having the highest priority.
	 */
	public LevelAndHeightData(TagSet buildingTags, TagSet buildingPartTags, Map<Integer, TagSet> levelTagSets,
			String roofShape, PolygonShapeXZ outline) {

		BuildingDefaults defaults = BuildingDefaults.getDefaultsFor(inheritTags(buildingPartTags, buildingTags));

		TagSet tags = buildingPartTags;
		if (!buildingPartTags.containsAny(asList("building:levels", "roof:levels", "height"), null)) {
			tags = inheritTags(buildingPartTags, buildingTags);
		}

		/* determine level information from Simple 3D Buildings tagging */

		Double parsedLevels = parseOsmDecimal(tags.getValue("building:levels"), false);

		int buildingLevels;
		if (parsedLevels != null) {
			buildingLevels = max(0, (int)(double)parsedLevels);
		} else if (parseHeight(tags, -1) > 0) {
			buildingLevels = max(1, (int) (parseHeight(tags, -1) / defaults.heightPerLevel));
		} else {
			buildingLevels = defaults.levels;
		}

		final int buildingMinLevel = parseInt(tags.getValue("building:min_level"), 0);
		final int buildingUndergroundLevels = parseUInt(tags.getValue("building:levels:underground"), 0);

		final int buildingMinLevelWithUnderground = (buildingMinLevel > 0)
				? buildingMinLevel
				: min(buildingMinLevel, -1 * buildingUndergroundLevels);

		/* determine roof height and roof levels */

		int roofLevels = parseUInt(tags.getValue("roof:levels"), 1);

		Double roofHeight = parseMeasure(tags.getValue("roof:height"));

		if ("flat".equals(roofShape)) {
			roofHeight = 0.0;
		} else if (roofHeight == null && buildingLevels == 0 && parseHeight(tags, 0) > 0) {
			//building with only roof levels
			roofHeight = parseHeight(tags, 0);
		}

		if (roofHeight == null && tags.containsKey("roof:levels")) {
			try {
				roofHeight = defaults.heightPerLevel * Integer.parseInt(tags.getValue("roof:levels"));
			} catch (NumberFormatException e) {}
		}

		if (roofHeight == null) {
			if ("dome".equals(roofShape)) {
				roofHeight = outline.getDiameter();
			} else if (buildingLevels == 1) {
				roofHeight = 1.0;
			} else {
				roofHeight = BuildingPart.DEFAULT_RIDGE_HEIGHT;
			}
		}

		if (roofHeight == 0) {
			roofLevels = 0;
		}

		/* determine building height */

		double height = parseHeight(tags, buildingLevels * defaults.heightPerLevel + roofHeight);
		height = max(height, 0.01); // make sure buildings have at least some height

		double heightWithoutRoof = height - roofHeight;

		double minHeight;
		if (parseMeasure(tags.getValue("min_height")) != null) {
			minHeight = parseMeasure(tags.getValue("min_height"));
		} else if (buildingMinLevel > 0) {
			minHeight = (heightWithoutRoof / buildingLevels) * buildingMinLevel;
		} else if (tags.contains("building", "roof") || tags.contains("building:part", "roof")) {
			minHeight = heightWithoutRoof - 0.3;
		} else {
			minHeight = 0;
		}

		if (minHeight > heightWithoutRoof) {
			System.err.println("Warning: min_height exceeds building (part) height without roof");
			minHeight = heightWithoutRoof - 0.1;
		}

		/* determine the level numbers (for interpreting level=* tags) with Simple Indoor Tagging */

		final int totalLevels = (roofLevels + buildingLevels) - buildingMinLevelWithUnderground;

		if (totalLevels == 0) {
			throw new IllegalArgumentException("zero levels for building part with tags: " + tags
					+ " (including inherited tags from building)");
		}

		Integer minLevel = parseInt(tags.getValue("min_level"));
		Integer maxLevel = parseInt(tags.getValue("max_level"));
		List<Integer> nonExistentLevels = parseLevels(tags.getValue("non_existent_levels"), emptyList());

		if (minLevel != null) {
			int limit = minLevel;
			nonExistentLevels.removeIf(it -> it < limit);
		}
		if (maxLevel != null) {
			int limit = maxLevel;
			nonExistentLevels.removeIf(it -> it > limit);
		}

		boolean ignoreIndoorLevelNumbers = (parsedLevels == null); //TODO this should probably deactivate indoor rendering? Either boolean flag or by disabling level numbers in Level objects.

		if (minLevel == null && maxLevel == null) {
			minLevel = buildingMinLevelWithUnderground;
		}

		if (minLevel != null && maxLevel != null) {
			if (minLevel > maxLevel) {
				System.err.println("Warning: min_level is larger than max_level");
				ignoreIndoorLevelNumbers = true;
			} else if ((maxLevel - minLevel) + 1 - nonExistentLevels.size() != totalLevels) {
				System.err.println("Warning: min_level, max_level and non_existent_levels do not match S3DB levels");
				ignoreIndoorLevelNumbers = true;
			}
		} else if (minLevel != null && maxLevel == null) {
			maxLevel = minLevel + (totalLevels - 1) + nonExistentLevels.size();
		} else if (minLevel == null && maxLevel != null) {
			minLevel = maxLevel - (totalLevels - 1) + nonExistentLevels.size();
		}

		List<Integer> levelNumbers;

		if (!ignoreIndoorLevelNumbers) {
			levelNumbers = IntStream.range(minLevel, maxLevel + 1)
					.filter(l -> !nonExistentLevels.contains(l))
					.boxed().collect(toList());
		} else {
			levelNumbers = IntStream.range(buildingMinLevelWithUnderground, roofLevels + buildingLevels)
					.boxed().collect(toList());
		}

		Function<Integer, LevelType> getLevelType = level -> {
			int index = levelNumbers.indexOf(level);
			if (index < buildingUndergroundLevels) return LevelType.UNDERGROUND;
			if (index < buildingUndergroundLevels + (buildingLevels - buildingMinLevel)) return LevelType.ABOVEGROUND;
			return LevelType.ROOF;
		};



		List<Integer> levelNumbersAboveground = levelNumbers.stream().filter(l -> getLevelType.apply(l) == LevelType.ABOVEGROUND).collect(toList());
		List<Integer> levelNumbersRoof = levelNumbers.stream().filter(l -> getLevelType.apply(l) == LevelType.ROOF).collect(toList());

		/* determine explicit and default level heights */

		Map<Integer, Double> explicitLevelHeights = new HashMap<>();

		if (!ignoreIndoorLevelNumbers) {
			for (int level : levelTagSets.keySet()) {
				Double levelHeight = parseMeasure(levelTagSets.get(level).getValue("height"));
				if (levelHeight != null) explicitLevelHeights.put(level, levelHeight);
			}
		}

		EnumMap<LevelType, Double> defaultLevelHeights = new EnumMap<>(LevelType.class);

		while (defaultLevelHeights.isEmpty()) {

			defaultLevelHeights.put(LevelType.ROOF,
					calculateDefaultLevelHeight(roofHeight, levelNumbersRoof, explicitLevelHeights));
			defaultLevelHeights.put(LevelType.ABOVEGROUND,
					calculateDefaultLevelHeight(heightWithoutRoof - minHeight, levelNumbersAboveground, explicitLevelHeights));
			defaultLevelHeights.put(LevelType.UNDERGROUND, defaults.heightPerLevel);

			if (defaultLevelHeights.values().stream().anyMatch(it -> it < 0)) {
				System.err.println("Warning: Sum of explicit level heights exceeds total available height, ignoring them.");
				explicitLevelHeights.clear();
				defaultLevelHeights.clear();
			}

		}

		Function<Integer, Double> getLevelHeight = level ->
			explicitLevelHeights.getOrDefault(level, defaultLevelHeights.get(getLevelType.apply(level)));

		/* determine the reference floor (where relative ele is 0, usually the ground floor).
		 * This is needed for calculating relativeEle. */

		Integer indexOfGroundFloor = null;

		for (int i = 0; i < levelNumbers.size(); i++) {
			if (getLevelType.apply(levelNumbers.get(i)) != LevelType.UNDERGROUND) {
				indexOfGroundFloor = i;
				break;
			}
		}

		if (indexOfGroundFloor == null) {
			indexOfGroundFloor = levelNumbers.size();
		}

		/* finally, create the level objects with all the information */

		List<Level> levels = new ArrayList<>(levelNumbers.size());

		for (int i = 0; i < levelNumbers.size(); i++) {

			int level = levelNumbers.get(i);
			LevelType type = getLevelType.apply(level);
			double levelHeight = getLevelHeight.apply(level);

			double relativeEle = minHeight;
			if (i >= indexOfGroundFloor) {
				for (int l = indexOfGroundFloor; l < i; l++) {
					relativeEle += getLevelHeight.apply(levelNumbers.get(l));
				}
			} else {
				for (int l = indexOfGroundFloor - 1; l >= i; l--) {
					relativeEle -= getLevelHeight.apply(levelNumbers.get(l));
				}
			}

			levels.add(new Level(level, type, levelHeight, relativeEle, levelTagSets.getOrDefault(level, TagSet.of())));

		}

		this.levels = unmodifiableList(levels);

	}

	/**
	 * calculates the default height for one of the levels without explicitly mapped height.
	 * @return  height for each of the levels with implicit height, < 0 if the sum of explicit heights exceeds the total
	 */
	private static double calculateDefaultLevelHeight(double totalHeight, List<Integer> levelNumbers,
			Map<Integer, Double> explicitLevelHeights) {

		double remainingHeight = totalHeight;
		int numLevelsWithImplicitHeight = 0;

		for (int level : levelNumbers) {
			if (explicitLevelHeights.containsKey(level)) {
				remainingHeight -= explicitLevelHeights.get(level);
			} else {
				numLevelsWithImplicitHeight ++;
			}
		}

		if (numLevelsWithImplicitHeight > 0) {
			return remainingHeight / numLevelsWithImplicitHeight;
		} else if (remainingHeight < 0) {
			return -1; //signals that sum of known heights is too large, which is bad even if all have explicit height
		} else {
			return 0;
		}

	}

	/** returns all levels of that particular type or types. A subset of {@link #levels}, ordered bottom to top. */
	public List<Level> levels(EnumSet<LevelType> acceptedTypes) {
		return levels.stream().filter(it -> acceptedTypes.contains(it.type)).collect(toList());
	}

	public @Nullable Level level(int levelNumber) {
		return levels.stream().filter(it -> it.level == levelNumber).findFirst().orElse(null);
	}

	public boolean hasLevel(int levelNumber) {
		return level(levelNumber) != null;
	}

	/** returns the above-ground height */
	public double height() {
		Level topLevel = levels.get(levels.size() - 1);
		if (topLevel.type == LevelType.UNDERGROUND) {
			return 0;
		} else {
			return topLevel.relativeEleTop();
		}
	}

	/** returns the above-ground height without any roof levels */
	public double heightWithoutRoof() {
		List<Level> levelsWithoutRoof = levels(EnumSet.of(LevelType.ABOVEGROUND));
		if (levelsWithoutRoof.isEmpty()) {
			List<Level> roofLevels = levels(EnumSet.of(LevelType.ROOF));
			if (roofLevels.isEmpty()) {
				return 0;
			} else {
				return roofLevels.get(0).relativeEle;
			}
		} else {
			Level topLevel = levelsWithoutRoof.get(levelsWithoutRoof.size() - 1);
			return topLevel.relativeEleTop();
		}
	}

	/** returns the base elevation of the lowest above-ground level (corresponds to OSM's building:min_height) */
	public double bottomHeight() {
		List<Level> levelsAboveGround = levels(EnumSet.of(LevelType.ABOVEGROUND, LevelType.ROOF));
		if (levelsAboveGround.isEmpty()) {
			return 0;
		} else {
			return levelsAboveGround.get(0).relativeEle;
		}
	}

	// TODO: add an "is compatible" method to check if an indoor space can span multiple building parts

	@Override
	public String toString() {
		return Lists.reverse(levels).stream().map(Level::toString).collect(joining("\n"));
	}

}
