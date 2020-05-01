package org.osm2world.core.world.modules.building.roof;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.max;
import static java.lang.Math.round;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.osm2world.core.math.VectorXYZ.Z_UNIT;

import java.util.ArrayList;
import java.util.List;

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

	protected void renderSpindle(Target target, Material material, SimplePolygonXZ polygon,
			List<Double> heights, List<Double> scaleFactors) {

		checkArgument(heights.size() == scaleFactors.size(), "heights and scaleFactors must have same size");

		VectorXZ center = polygon.getCenter();

		/* calculate the polygon relative to the center */

		List<VectorXZ> vertexLoop = new ArrayList<>();

		for (VectorXZ v : polygon.makeCounterclockwise().getVertexList()) {
			vertexLoop.add(v.subtract(center));
		}

		ShapeXZ spindleShape = new SimplePolygonXZ(vertexLoop);

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

	protected List<List<VectorXZ>> spindleTexCoordLists(List<VectorXYZ> path, int shapeVertexCount,
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

	protected List<VectorXZ> spindleTexCoordList(List<VectorXYZ> path, int shapeVertexCount,
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