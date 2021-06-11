package org.osm2world.core.world.modules.building;

import static java.lang.Math.max;
import static java.util.Collections.unmodifiableMap;
import static org.osm2world.core.util.ValueParseUtil.*;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.Angle;
import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.CircleXZ;
import org.osm2world.core.math.shapes.CircularSectorXZ;
import org.osm2world.core.math.shapes.SimpleClosedShapeXZ;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;

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
		CENTER, TOP, LEFT, RIGHT, BOTTOM
	}

	static class PaneLayout {

		public final int panesHorizontal;
		public final int panesVertical;
		public final boolean radialPanes;

		public PaneLayout(int panesHorizontal, int panesVertical, boolean radialPanes) {
			this.panesHorizontal = panesHorizontal;
			this.panesVertical = panesVertical;
			this.radialPanes = radialPanes;
		}

	}

	/** parameters that exist for each {@link WindowRegion} as well as for the overall window */
	static class RegionProperties {

		public final WindowShape shape;

		public final @Nullable PaneLayout panes;

		public RegionProperties(TagSet tags, String infix) {

			WindowParameters.WindowShape tempShape = WindowShape.getValue(tags.getValue("window:" + infix + "shape"));
			shape = tempShape != null ? tempShape : WindowShape.RECTANGLE;

			String panesValue = tags.getValue("window:" + infix + "panes");

			if (panesValue != null && panesValue.matches("^\\d+x\\d+$")) {
				String[] s = panesValue.split("x");
				panes = new PaneLayout(
						parseUInt(s[0], 1),
						parseUInt(s[1], 1),
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

	public final boolean hasLeftShutter;
	public final boolean hasRightShutter;

	public final @Nullable Integer numberWindows;
	public final int groupSize;

	public final double width;
	public final double height;
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

		opaqueWindowMaterial = BuildingPart.buildMaterial(
				tags.getValue("window:material"),
				tags.getValue("window:colour"),
				Materials.GLASS, false);

		transparentWindowMaterial = BuildingPart.buildMaterial(
				null,
				tags.getValue("window:colour"),
				Materials.GLASS_TRANSPARENT, false);

		numberWindows = parseUInt(tags.getValue("window:count"));
		groupSize = parseUInt(tags.getValue("window:group_size"), 1);

		width = parseMeasure(tags.getValue("window:width"), DEFAULT_WIDTH);
		height = parseMeasure(tags.getValue("window:height"), DEFAULT_HEIGHT_RELATIVE_TO_LEVEL * levelHeight);
		breast = parseMeasure(tags.getValue("window:breast"), DEFAULT_BREAST_RELATIVE_TO_LEVEL * levelHeight);

		overallProperties = new RegionProperties(tags, "");

		EnumMap<WindowRegion, RegionProperties> regionProperties = new EnumMap<>(WindowRegion.class);
		for (WindowRegion region : WindowRegion.values()) {
			String infix = region.toString().toLowerCase() + ":";
			if (tags.containsKey("window:" + infix + "shape")) {
				regionProperties.put(region, new RegionProperties(tags, infix));
			}
		}
		this.regionProperties = unmodifiableMap(regionProperties);

		/* frame */

		frameMaterial = BuildingPart.buildMaterial(
				tags.getValue("window:frame:material"),
				tags.getValue("window:frame:colour"),
				Materials.PLASTIC, false);

		/* shutters */

		if (tags.contains("window:shutter", "both")) {
			hasLeftShutter = true;
			hasRightShutter = true;
		} else if (tags.contains("window:shutter", "left")) {
			hasLeftShutter = true;
			hasRightShutter = false;
		} else if (tags.contains("window:shutter", "right")) {
			hasLeftShutter = false;
			hasRightShutter = true;
		} else {
			hasLeftShutter = false;
			hasRightShutter = false;
		}

		shutterMaterial = BuildingPart.buildMaterial(
				tags.getValue("window:shutter:material"),
				tags.getValue("window:shutter:colour"),
				Materials.WOOD, false);

	}

}
