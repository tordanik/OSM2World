package org.osm2world.world.modules.building.roof;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.max;
import static java.lang.Math.round;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

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
import org.osm2world.scene.material.TextureData;
import org.osm2world.scene.material.TextureLayer;

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
				null, scaleFactors,
				spindleTexCoordLists(path, spindleShape.vertices().size(),
						polygon.getOutlineLength(), material),
				EnumSet.of(ExtrudeOption.SMOOTH_SIDES));

	}

	private List<List<VectorXZ>> spindleTexCoordLists(List<VectorXYZ> path, int shapeVertexCount,
			double polygonLength, Material material) {

		List<TextureLayer> textureLayers = material.getTextureLayers();

		switch (textureLayers.size()) {

		case 0: return emptyList();

		case 1: return singletonList(spindleTexCoordList(path,
				shapeVertexCount, polygonLength, textureLayers.get(0)));

		default:

			List<List<VectorXZ>> result = new ArrayList<>();

			for (TextureLayer textureLayer : textureLayers) {
				result.add(spindleTexCoordList(path,
						shapeVertexCount, polygonLength, textureLayer));
			}

			return result;

		}

	}

	private List<VectorXZ> spindleTexCoordList(List<VectorXYZ> path, int shapeVertexCount,
			double polygonLength, TextureLayer textureLayer) {

		List<VectorXZ> result = new ArrayList<>();

		double accumulatedTexHeight = 0;

		for (int i = 0; i < path.size(); i++) {

			if (i > 0) {

				accumulatedTexHeight += path.get(i - 1).distanceTo(path.get(i));

				//TODO use the distance on the extruded surface instead of on the path,
				//e.g. += rings[i-1].get(0).distanceTo(rings[i].get(0));
			}

			result.addAll(spindleTexCoordListForRing(shapeVertexCount,
					polygonLength, accumulatedTexHeight, textureLayer.baseColorTexture));

		}

		return result;

	}

	private List<VectorXZ> spindleTexCoordListForRing(int shapeVertexCount, double polygonLength,
			double accumulatedTexHeight, TextureData textureData) {

		double textureRepeats = max(1, round(polygonLength / textureData.dimensions().width()));

		double texWidthSteps = textureRepeats / (shapeVertexCount - 1);

		double texZ = accumulatedTexHeight / textureData.dimensions.height();

		VectorXZ[] texCoords = new VectorXZ[shapeVertexCount];

		for (int i = 0; i < shapeVertexCount; i++) {
			texCoords[i] = new VectorXZ(i*texWidthSteps, texZ);
		}

		return asList(texCoords);

	}

}
