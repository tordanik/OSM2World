package org.osm2world.core.world.modules.building.roof;

import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.world.attachment.AttachmentSurface;
import org.osm2world.core.world.modules.building.BuildingPart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

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

	protected Collection<AttachmentSurface> attachmentSurfaces;


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

	/** returns the attachment surfaces for the roof */
	public Collection<AttachmentSurface> getAttachmentSurfaces(double baseEle, int level){
		return Collections.emptyList();
	}

	/**
	 * renders the roof. The same as {@link Renderable#renderTo(Target)},
	 * but it also needs the lower elevation of the roof (which is not yet known at construction time).
	 */
	public abstract void renderTo(Target target, double baseEle);

	/**
	 * creates the correct roof for the given roof:shape value
	 */
	public static final Roof createRoofForShape(String roofShape, PolygonWithHolesXZ originalPolygon,
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
		default: return new FlatRoof(originalPolygon, tags, material);
		}

	}
}
