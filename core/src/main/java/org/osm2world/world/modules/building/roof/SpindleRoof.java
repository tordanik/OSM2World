package org.osm2world.world.modules.building.roof;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.math.shapes.ShapeXZ;
import org.osm2world.math.shapes.SimplePolygonXZ;
import org.osm2world.output.CommonTarget;
import org.osm2world.output.common.ExtrudeOption;
import org.osm2world.scene.material.Material;

abstract public class SpindleRoof extends Roof {

	public SpindleRoof(PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {
		super(originalPolygon, tags, material);
	}

	@Override
	public PolygonWithHolesXZ getPolygon() {
		return originalPolygon;
	}

	@Override
	public double getRoofHeightAt(VectorXZ pos) {
		return 0;
	}

	// TODO needs implementing
	@Override
	public Collection<LineSegmentXZ> getInnerSegments(){
		return emptyList();
	}

	@Override
	public void renderTo(CommonTarget target, double baseEle) {

		List<Double> heights = new ArrayList<>();
		List<Double> scaleFactors = new ArrayList<>();

		getSpindleSteps().forEach(pair -> {
			heights.add(baseEle + pair.getKey() * roofHeight);
			scaleFactors.add(pair.getValue());
		});

		renderSpindle(target, material, getPolygon().getOuter().makeClockwise(), heights, scaleFactors);

	}

	/**
	 * defines the spindle. Each step has a height (relative to the total roof height)
	 * and a scale factor for the polygon.
	 */
	abstract protected List<Pair<Double, Double>> getSpindleSteps();

	private void renderSpindle(CommonTarget target, Material material, SimplePolygonXZ polygon,
			List<Double> heights, List<Double> scaleFactors) {

		checkArgument(heights.size() == scaleFactors.size(), "heights and scaleFactors must have same size");

		/* calculate the polygon relative to the center */

		VectorXZ center = polygon.getCenter();
		ShapeXZ spindleShape = polygon.makeCounterclockwise().shift(center.invert());

		/* construct a path from the heights */

		List<VectorXYZ> path = new ArrayList<>();

		for (double height : heights) {
			path.add(center.xyz(height));
		}

		/* render the roof using shape extrusion */

		target.drawExtrudedShape(material, spindleShape, path,
				null, scaleFactors, null,
				EnumSet.of(ExtrudeOption.SMOOTH_SIDES));

	}

}
