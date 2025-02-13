package org.osm2world.world.modules.building.roof;

import static java.lang.Math.PI;
import static java.util.Comparator.comparingDouble;
import static org.osm2world.math.Angle.radiansBetween;
import static org.osm2world.util.ValueParseUtil.parseAngle;
import static org.osm2world.util.ValueParseUtil.parseMeasure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.conversion.ConversionLog;
import org.osm2world.map_data.data.MapArea;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.Angle;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.output.CommonTarget;
import org.osm2world.output.common.material.Material;
import org.osm2world.world.attachment.AttachmentSurface;
import org.osm2world.world.modules.building.BuildingPart;
import org.osm2world.world.modules.building.LevelAndHeightData;

/** the roof of a {@link BuildingPart} */
abstract public class Roof {

	/**
	 * the polygon of the {@link BuildingPart}.
	 * The roof code may modify it by inserting additional points, see {@link #getPolygon()}
	 */
	protected final PolygonWithHolesXZ originalPolygon;

	protected final TagSet tags;
	protected final Material material;

	protected Double roofHeight;

	public Roof(PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {
		this.originalPolygon = originalPolygon;
		this.tags = tags;
		this.material = material;
		this.roofHeight = calculatePreliminaryHeight();
	}

	/**
	 * returns the outline (with holes) of the roof.
	 * The shape will be generally identical to that of the building part (as passed to the constructor),
	 * but additional vertices might have been inserted into segments.
	 */
	abstract public PolygonWithHolesXZ getPolygon();

	public void setRoofHeight(double roofHeight) {
		this.roofHeight = roofHeight;
	}

	/**
	 * attempts to derive the roof height from the roof's shape and explicit tags such as
	 * roof:height and roof:angle.
	 * If this fails, the roof must have its height assigned through other means,
	 * e.g. based on {@link LevelAndHeightData} calculations,
	 * and that height must then be passed to {@link #setRoofHeight(double)}.
	 *
	 * @return  the height, or null if it could not be reliably determined
	 */
	public Double calculatePreliminaryHeight() {
		if (tags.containsKey("roof:angle")) {
			ConversionLog.warn("No roof:angle implemented for roof type " + this.getClass().getSimpleName());
		}
		return parseMeasure(tags.getValue("roof:height"));
	}

	/**
	 * returns roof height at a position. Must be non-negative for sensible inputs.
	 */
	abstract public double getRoofHeightAt(VectorXZ coord);

	/** returns segments within the roof polygon that define ridges or edges of the roof */
	public abstract Collection<LineSegmentXZ> getInnerSegments();

	/**
	 * returns the attachment surfaces for this roof
	 *
	 * @param baseEle  the lower elevation of the roof, as in {@link #renderTo(CommonTarget, double)}
	 * @param level  the roof's level number. This allows distinction between multiple vertically stacked roofs
	 * when attaching objects to the roofs' {@link AttachmentSurface}s.
	 */
	public Collection<AttachmentSurface> getAttachmentSurfaces(double baseEle, int level) {
		return Collections.emptyList();
	}

	/**
	 * renders the roof. Needs the lower elevation of the roof (which is not yet known at construction time).
	 */
	public abstract void renderTo(CommonTarget target, double baseEle);

	/**
	 * creates the correct roof for the given roof:shape value
	 */
	public static final Roof createRoofForShape(String roofShape, MapArea area, PolygonWithHolesXZ originalPolygon,
			TagSet tags, Material material) {

		return switch (roofShape) {
			case "pyramidal" -> new PyramidalRoof(originalPolygon, tags, material);
			case "onion" -> new OnionRoof(originalPolygon, tags, material);
			case "skillion" -> new SkillionRoof(originalPolygon, tags, material);
			case "gabled" -> new GabledRoof(originalPolygon, tags, material);
			case "hipped" -> new HippedRoof(originalPolygon, tags, material);
			case "half-hipped" -> new HalfHippedRoof(originalPolygon, tags, material);
			case "gambrel" -> new GambrelRoof(originalPolygon, tags, material);
			case "mansard" -> new MansardRoof(originalPolygon, tags, material);
			case "dome" -> new DomeRoof(originalPolygon, tags, material);
			case "round" -> new RoundRoof(originalPolygon, tags, material);
			case "chimney" -> new ChimneyRoof(originalPolygon, tags, material);
			case "complex" -> new ComplexRoof(area, originalPolygon, tags, material);
			default -> new FlatRoof(originalPolygon, tags, material);
		};

	}

	/**
	 * attempts to slightly modify the angle from an OSM direction tag
	 * so that it is parallel or orthogonal to one of the segments.
	 * Returns the modified angle, or the original angle if the required modification would be too great.
	 */
	protected static @Nullable Angle snapDirection(String directionValue, List<LineSegmentXZ> segments) {

		Double angleDeg = parseAngle(directionValue);

		if (angleDeg == null) return null;

		Angle angle = Angle.ofDegrees(angleDeg);

		List<Angle> segmentAngles = new ArrayList<>();
		for (LineSegmentXZ segment : segments) {
			Angle segmentAngle = Angle.ofRadians(segment.getDirection().angle());
			segmentAngles.add(segmentAngle);
			segmentAngles.add(segmentAngle.plus(Angle.ofRadians(0.5 * PI)));
			segmentAngles.add(segmentAngle.plus(Angle.ofRadians(PI)));
			segmentAngles.add(segmentAngle.plus(Angle.ofRadians(1.5 * PI)));
		}

		Angle closestSegmentAngle = Collections.min(segmentAngles, comparingDouble(a -> radiansBetween(a, angle)));

		double acceptableDifference;

		if (directionValue.matches("[NSEW]+")) {
			acceptableDifference = PI / 4;
		} else if (!directionValue.contains(".")) {
			acceptableDifference = PI / 18;
		} else {
			acceptableDifference = PI / 360;
		}

		if (radiansBetween(closestSegmentAngle, angle) <= acceptableDifference) {
			return closestSegmentAngle;
		} else {
			return angle;
		}

	}

}
