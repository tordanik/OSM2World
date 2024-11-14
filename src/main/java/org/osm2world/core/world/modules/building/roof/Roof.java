package org.osm2world.core.world.modules.building.roof;

import static java.lang.Math.PI;
import static java.util.Comparator.comparingDouble;
import static org.osm2world.core.math.Angle.radiansBetween;
import static org.osm2world.core.util.ValueParseUtil.parseAngle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.Angle;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.CommonTarget;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.world.attachment.AttachmentSurface;
import org.osm2world.core.world.modules.building.BuildingPart;

/** the roof of a {@link BuildingPart} */
abstract public class Roof {

	/**
	 * the polygon of the {@link BuildingPart}.
	 * The roof code may modify it by inserting additional points, see {@link #getPolygon()}
	 */
	protected final PolygonWithHolesXZ originalPolygon;

	protected final TagSet tags;
	protected final double roofHeight;
	protected final Material material;

	public Roof(PolygonWithHolesXZ originalPolygon, TagSet tags, double height, Material material) {
		this.originalPolygon = originalPolygon;
		this.tags = tags;
		this.roofHeight = height;
		this.material = material;
	}

	/**
	 * returns the outline (with holes) of the roof.
	 * The shape will be generally identical to that of the building part (as passed to the constructor),
	 * but additional vertices might have been inserted into segments.
	 */
	abstract public PolygonWithHolesXZ getPolygon();

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
			TagSet tags, double height, Material material) {

		switch (roofShape) {
		case "pyramidal": return new PyramidalRoof(originalPolygon, tags, height, material);
		case "onion": return new OnionRoof(originalPolygon, tags, height,  material);
		case "skillion": return new SkillionRoof(originalPolygon, tags, height, material);
		case "gabled": return new GabledRoof(originalPolygon, tags, height, material);
		case "hipped": return new HippedRoof(originalPolygon, tags, height,  material);
		case "half-hipped": return new HalfHippedRoof(originalPolygon, tags, height, material);
		case "gambrel": return new GambrelRoof(originalPolygon, tags, height, material);
		case "mansard": return new MansardRoof(originalPolygon, tags, height, material);
		case "dome": return new DomeRoof(originalPolygon, tags, height, material);
		case "round": return new RoundRoof(originalPolygon, tags, height, material);
		case "chimney": return new ChimneyRoof(originalPolygon, tags, material);
		case "complex": return new ComplexRoof(area, originalPolygon, tags, height, material);
		default: return new FlatRoof(originalPolygon, tags, material);
		}

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
