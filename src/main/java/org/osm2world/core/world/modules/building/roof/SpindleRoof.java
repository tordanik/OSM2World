package org.osm2world.core.world.modules.building.roof;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.max;
import static java.lang.Math.round;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.osm2world.core.math.VectorXYZ.Z_UNIT;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.TextureData;
import org.osm2world.core.target.common.material.Material;

abstract public class SpindleRoof extends Roof {

	public SpindleRoof(PolygonWithHolesXZ originalPolygon, TagSet tags, double height, Material material) {
		super(originalPolygon, tags, height, material);
	}

	@Override
	public PolygonWithHolesXZ getPolygon() {
		return originalPolygon;
	}

	@Override
	public double getRoofHeightAt(VectorXZ pos) {
		return 0;
	}

	@Override
	public void renderTo(Target target, double baseEle) {

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

	private void renderSpindle(Target target, Material material, SimplePolygonXZ polygon,
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
				nCopies(path.size(), Z_UNIT), scaleFactors,
				spindleTexCoordLists(path, spindleShape.getVertexList().size(),
						polygon.getOutlineLength(), material),
				null);

	}

	private List<List<VectorXZ>> spindleTexCoordLists(List<VectorXYZ> path, int shapeVertexCount,
			double polygonLength, Material material) {

		List<TextureData> textureDataList = material.getTextureDataList();

		switch (textureDataList.size()) {

		case 0: return emptyList();

		case 1: return singletonList(spindleTexCoordList(path,
				shapeVertexCount, polygonLength, textureDataList.get(0)));

		default:

			List<List<VectorXZ>> result = new ArrayList<>();

			for (TextureData textureData : textureDataList) {
				result.add(spindleTexCoordList(path,
						shapeVertexCount, polygonLength, textureData));
			}

			return result;

		}

	}

	private List<VectorXZ> spindleTexCoordList(List<VectorXYZ> path, int shapeVertexCount,
			double polygonLength, TextureData textureData) {

		List<VectorXZ> result = new ArrayList<>();

		double accumulatedTexHeight = 0;

		for (int i = 0; i < path.size(); i++) {

			if (i > 0) {

				accumulatedTexHeight += path.get(i - 1).distanceTo(path.get(i));

				//TODO use the distance on the extruded surface instead of on the path,
				//e.g. += rings[i-1].get(0).distanceTo(rings[i].get(0));
			}

			result.addAll(spindleTexCoordListForRing(shapeVertexCount,
					polygonLength, accumulatedTexHeight, textureData));

		}

		return result;

	}

	private List<VectorXZ> spindleTexCoordListForRing(int shapeVertexCount, double polygonLength,
			double accumulatedTexHeight, TextureData textureData) {

		double textureRepeats = max(1, round(polygonLength / textureData.width));

		double texWidthSteps = textureRepeats / (shapeVertexCount - 1);

		double texZ = accumulatedTexHeight / textureData.height;

		VectorXZ[] texCoords = new VectorXZ[shapeVertexCount];

		for (int i = 0; i < shapeVertexCount; i++) {
			texCoords[i] = new VectorXZ(i*texWidthSteps, texZ);
		}

		return asList(texCoords);

	}

}