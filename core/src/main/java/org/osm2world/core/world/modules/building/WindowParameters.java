package org.osm2world.core.world.modules.building;

import static java.lang.Math.max;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableMap;
import static org.osm2world.core.util.ValueParseUtil.parseMeasure;
import static org.osm2world.core.util.ValueParseUtil.parseUInt;

import java.util.*;

import javax.annotation.Nullable;

import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.*;
import org.osm2world.core.math.shapes.CircleXZ;
import org.osm2world.core.math.shapes.CircularSectorXZ;
import org.osm2world.core.math.shapes.SimpleClosedShapeXZ;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.util.enums.LeftRightBoth;

/**
 * data about the window(s) on a wall, door, or for a single window.
 * This object type is immutable after its construction from a set of tags.
 */
public class WindowParameters {

	private static enum WindowType {
		PLAIN, DISPLAY_WINDOW
	}

	static enum WindowShape {

		RECTANGLE, CIRCLE, TRIANGLE, SEMICIRCLE;

		/**
		 * convenient case-insensitive and exception-free alternative to valueOf
		 * @return  the matching value, can be null
		 */
		public static WindowParameters.WindowShape getValue(String shapeName) {

			if (shapeName == null) {
				return null;
			}

			try {
				return valueOf(shapeName.toUpperCase());
			} catch (IllegalArgumentException e) {
				return null;
			}

		}

		public SimpleClosedShapeXZ buildShapeXZ(VectorXZ position, double width, double height) {

			switch (this) {

			case CIRCLE:
				return new CircleXZ(position.add(0, height / 2), max(height, width)/2);

			case TRIANGLE:
				return new TriangleXZ(
						position.add(0, height),
						position.add(-width / 2, 0),
						position.add(+width / 2, 0));

			case SEMICIRCLE:
				return new CircularSectorXZ(position, width / 2, Angle.ofDegrees(-90), Angle.ofDegrees(90))
						.scale(position, 1, height / (width / 2));

			case RECTANGLE:
			default:
				return new AxisAlignedRectangleXZ(position.add(0, height/2), width, height);

			}
		}

		public SimpleClosedShapeXZ buildShapeXZ(LineSegmentXZ baseSegment, double height) {

			switch (this) {

			case TRIANGLE:
				return new TriangleXZ(baseSegment.p2, baseSegment.p1,
						baseSegment.getCenter().add(baseSegment.getDirection().rightNormal().mult(height)));

			case SEMICIRCLE:
				return new CircularSectorXZ(baseSegment.getCenter(), baseSegment.getLength() / 2,
						Angle.ofDegrees(0), Angle.ofDegrees(180))
						.rotatedCW(baseSegment.getDirection().angle())
						.scale(baseSegment.getCenter(), 1.0, height / (baseSegment.getLength() / 2));

			case RECTANGLE:
			default:
				return new AxisAlignedRectangleXZ(
						baseSegment.getCenter().add(baseSegment.getDirection().rightNormal().mult(height / 2)),
						baseSegment.getLength(), height)
						.rotatedCW(baseSegment.getDirection().rightNormal().angle());

			}
		}

	}

	static enum WindowRegion {
		CENTER, TOP, LEFT, RIGHT, BOTTOM;
		public String keyInfix() {
			return this.toString().toLowerCase() + ":";
		}
	}

	static class PaneLayout {

		public final int panesHorizontal;
		public final int panesVertical;
		public final boolean radialPanes;

		public PaneLayout(int panesHorizontal, int panesVertical, boolean radialPanes) {
			if (panesHorizontal <= 0) throw new IllegalArgumentException("number of panels must be positive");
			if (panesVertical <= 0) throw new IllegalArgumentException("number of panels must be positive");
			this.panesHorizontal = panesHorizontal;
			this.panesVertical = panesVertical;
			this.radialPanes = radialPanes;
		}

		@Override
		public String toString() {
			return panesHorizontal + "x" + panesVertical + (radialPanes ? " radial" : " grid");
		}

	}

	/** parameters that exist for each {@link WindowRegion} as well as for the overall window */
	static class RegionProperties {

		public final WindowShape shape;

		public final double width;
		public final double height;

		public final @Nullable PaneLayout panes;

		public RegionProperties(double width, double height, TagSet tags, String infix) {

			this.width = width;
			this.height = height;

			WindowParameters.WindowShape tempShape = WindowShape.getValue(tags.getValue("window:" + infix + "shape"));
			shape = tempShape != null ? tempShape : WindowShape.RECTANGLE;

			String panesValue = tags.getValue("window:" + infix + "panes");

			if (panesValue != null && panesValue.matches("^\\d+x\\d+$")) {
				String[] s = panesValue.split("x");
				panes = new PaneLayout(
						max(1, parseUInt(s[0], 1)),
						max(1, parseUInt(s[1], 1)),
						tags.contains("window:" + infix + "panes:arrangement", "radial"));
			} else {
				panes = null;
			}

		}

	}

	private static final double DEFAULT_WIDTH = 1;
	private static final double DEFAULT_HEIGHT_RELATIVE_TO_LEVEL = 0.5f;
	private static final double DEFAULT_BREAST_RELATIVE_TO_LEVEL = 0.3f;

	public final WindowParameters.WindowType type;

	public final @Nullable LeftRightBoth shutterSide;

	public final @Nullable Integer numberWindows;
	public final int groupSize;

	public final double breast;

	public final RegionProperties overallProperties;
	public final Map<WindowRegion, RegionProperties> regionProperties;

	/** the material to use for the window pane if it should appear opaque (non-transparent) */
	public final Material opaqueWindowMaterial;
	/** the material to use for the window pane if it should appear transparent */
	public final Material transparentWindowMaterial;

	public final Material frameMaterial;
	public final Material shutterMaterial;

	public WindowParameters(TagSet tags, double levelHeight) {

		/* window */

		String windowString = tags.getValue("window");

		if ("display_window".equals(windowString)) {
			type = WindowType.DISPLAY_WINDOW;
		} else {
			type = WindowType.PLAIN;
		}

		String materialString = tags.getValue("window:material");
		materialString = "glass".equals(materialString) ? null : materialString;

		opaqueWindowMaterial = BuildingPart.buildMaterial(
				materialString,
				tags.getValue("window:colour"),
				Materials.GLASS, false);

		transparentWindowMaterial = BuildingPart.buildMaterial(
				null,
				tags.getValue("window:colour"),
				Materials.GLASS_TRANSPARENT, false);

		numberWindows = parseUInt(tags.getValue("window:count"));
		groupSize = parseUInt(tags.getValue("window:group_size"), 1);

		breast = parseMeasure(tags.getValue("window:breast"), DEFAULT_BREAST_RELATIVE_TO_LEVEL * levelHeight);

		/* find out which "regions" this window has */

		Set<WindowRegion> regions = new HashSet<>(asList(WindowRegion.CENTER));

		stream(WindowRegion.values())
				.filter(it -> tags.containsKey("window:" + it.keyInfix() + "shape"))
				.forEach(regions::add);

		/* determine width and height for the window as a whole and each region */

		double parsedWidth = parseMeasure(tags.getValue("window:width"), DEFAULT_WIDTH);
		double parsedHeight = parseMeasure(tags.getValue("window:height"), DEFAULT_HEIGHT_RELATIVE_TO_LEVEL * levelHeight);

		double width, height;
		Map<WindowRegion, Double> regionHeights = new EnumMap<>(WindowRegion.class);
		Map<WindowRegion, Double> regionWidths = new EnumMap<>(WindowRegion.class);

		if (regions.size() == 1) {

			assert regions.contains(WindowRegion.CENTER);

			width = parsedWidth;
			height = parsedHeight;

			regionWidths.put(WindowRegion.CENTER, width);
			regionHeights.put(WindowRegion.CENTER, height);

		} else {

			{ /* determine height by distributing the total height equally among regions without explicit height */

				double sumHeights = 0;
				List<WindowRegion> verticalRegionsWithoutHeight = new ArrayList<>();

				for (WindowRegion region : asList(WindowRegion.TOP, WindowRegion.CENTER, WindowRegion.BOTTOM)) {
					if (!regions.contains(region)) continue;
					Double regionHeight = parseMeasure(tags.getValue("window:" + region.keyInfix() + "height"));
					if (regionHeight == null) {
						verticalRegionsWithoutHeight.add(region);
					} else {
						sumHeights += regionHeight;
						regionHeights.put(region, regionHeight);
					}
				}

				if (verticalRegionsWithoutHeight.isEmpty()) {
					height = sumHeights;
				} else {

					height = max(parsedHeight, sumHeights + 0.1);

					double remainingHeight = height - sumHeights;

					if (verticalRegionsWithoutHeight.contains(WindowRegion.CENTER)) {
						// double weight for center
						double centerHeight = 2 * remainingHeight / (verticalRegionsWithoutHeight.size() + 1);
						regionHeights.put(WindowRegion.CENTER, centerHeight);
						verticalRegionsWithoutHeight.remove(WindowRegion.CENTER);
						remainingHeight -= centerHeight;
					}

					if (verticalRegionsWithoutHeight.size() > 0) {
						double dividedHeight = remainingHeight / verticalRegionsWithoutHeight.size();
						verticalRegionsWithoutHeight.forEach(region -> regionHeights.put(region, dividedHeight));
					}

				}
			}

			{ /* same as above, but for width */

				double sumWidths = 0;
				List<WindowRegion> horizontalRegionsWithoutWidth = new ArrayList<>();

				for (WindowRegion region : asList(WindowRegion.LEFT, WindowRegion.CENTER, WindowRegion.RIGHT)) {
					if (!regions.contains(region)) continue;
					Double regionWidth = parseMeasure(tags.getValue("window:" + region.keyInfix() + "width"));
					if (regionWidth == null) {
						horizontalRegionsWithoutWidth.add(region);
					} else {
						sumWidths += regionWidth;
						regionWidths.put(region, regionWidth);
					}
				}

				if (horizontalRegionsWithoutWidth.isEmpty()) {
					width = sumWidths;
				} else {

					width = max(parsedWidth, sumWidths + 0.1);

					double remainingWidth = width - sumWidths;

					if (horizontalRegionsWithoutWidth.contains(WindowRegion.CENTER)) {
						// double weight for center
						double centerWidth = 2 * remainingWidth / (horizontalRegionsWithoutWidth.size() + 1);
						regionWidths.put(WindowRegion.CENTER, centerWidth);
						horizontalRegionsWithoutWidth.remove(WindowRegion.CENTER);
						remainingWidth -= centerWidth;
					}

					if (horizontalRegionsWithoutWidth.size() > 0) {
						double dividedWidth = remainingWidth / horizontalRegionsWithoutWidth.size();
						horizontalRegionsWithoutWidth.forEach(region -> regionWidths.put(region, dividedWidth));
					}

				}
			}

			{ /* set remaining heights and widths (those "off to the side") */

				if (regions.contains(WindowRegion.LEFT)) {
					regionHeights.put(WindowRegion.LEFT, regionHeights.get(WindowRegion.CENTER));
				}
				if (regions.contains(WindowRegion.RIGHT)) {
					regionHeights.put(WindowRegion.RIGHT, regionHeights.get(WindowRegion.CENTER));
				}

				if (regions.contains(WindowRegion.TOP)) {
					regionWidths.put(WindowRegion.TOP, regionWidths.get(WindowRegion.CENTER));
				}
				if (regions.contains(WindowRegion.BOTTOM)) {
					regionWidths.put(WindowRegion.BOTTOM, regionWidths.get(WindowRegion.CENTER));
				}

			}

		}

		/* other region-based properties */

		overallProperties = new RegionProperties(width, height, tags, "");

		EnumMap<WindowRegion, RegionProperties> regionProperties = new EnumMap<>(WindowRegion.class);
		for (WindowRegion region : regions) {
			regionProperties.put(region, new RegionProperties(
					regionWidths.get(region),
					regionHeights.get(region),
					tags, region.keyInfix()));
		}
		this.regionProperties = unmodifiableMap(regionProperties);

		/* frame */

		frameMaterial = BuildingPart.buildMaterial(
				tags.getValue("window:frame:material"),
				tags.getValue("window:frame:colour"),
				Materials.PLASTIC, false);

		/* shutters */

		shutterSide = LeftRightBoth.of(tags.getValue("window:shutter"));

		shutterMaterial = BuildingPart.buildMaterial(
				tags.getValue("window:shutter:material"),
				tags.getValue("window:shutter:colour"),
				Materials.WOOD, false);

	}

}
