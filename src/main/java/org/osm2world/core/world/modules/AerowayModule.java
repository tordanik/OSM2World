package org.osm2world.core.world.modules;

import static java.lang.Math.min;
import static org.osm2world.core.target.common.material.Materials.*;
import static org.osm2world.core.target.common.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.core.target.common.texcoord.TexCoordUtil.texCoordLists;
import static org.osm2world.core.target.common.texcoord.TexCoordUtil.triangleTexCoordLists;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.createLineBetween;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.parseWidth;
import static org.osm2world.core.world.network.NetworkUtil.getConnectedNetworkSegments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.PolygonShapeXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.TextureDataDimensions;
import org.osm2world.core.target.common.texcoord.GlobalXZTexCoordFunction;
import org.osm2world.core.target.common.texcoord.TexCoordFunction;
import org.osm2world.core.world.data.AbstractAreaWorldObject;
import org.osm2world.core.world.data.LegacyWorldObject;
import org.osm2world.core.world.modules.common.ConfigurableWorldModule;
import org.osm2world.core.world.modules.common.WorldModuleGeometryUtil;
import org.osm2world.core.world.network.AbstractNetworkWaySegmentWorldObject;
import org.osm2world.core.world.network.JunctionNodeWorldObject;
import org.osm2world.core.world.network.NetworkAreaWorldObject;
import org.osm2world.core.world.network.VisibleConnectorNodeWorldObject;

/**
 * places runways, helipads and various other features related to air travel
 */
public class AerowayModule extends ConfigurableWorldModule {

	@Override
	public void applyTo(MapData mapData) {

		for (MapWaySegment segment : mapData.getMapWaySegments()) {
			if (segment.getTags().contains("aeroway", "runway")) {
				segment.addRepresentation(new Runway(segment));
			} else if (segment.getTags().contains("aeroway", "taxiway")) {
				segment.addRepresentation(new Taxiway(segment));
			}
		}

		for (MapArea area : mapData.getMapAreas()) {
			if (area.getTags().contains("aeroway", "helipad")) {
				area.addRepresentation(new Helipad(area));
			} else if (area.getTags().contains("aeroway", "apron")) {
				area.addRepresentation(new Apron(area));
			}
		}

		for (MapNode node : mapData.getMapNodes()) {

			List<AerowaySegment> connectedSegments = getConnectedNetworkSegments(node, AerowaySegment.class, null);

			if (connectedSegments.size() > 2) {
				if (connectedSegments.stream().allMatch(s -> s instanceof Runway)) {
					//TODO: remove this condition once junction polygon calculation produces better results
					node.addRepresentation(new AerowayJunction(node));
				}
			} else if (connectedSegments.size() == 2) {

				AerowaySegment s1 = connectedSegments.get(0);
				AerowaySegment s2 = connectedSegments.get(1);

				if (s1.getWidth() != s2.getWidth()) {
					node.addRepresentation(new AerowayConnector(node));
				}

			}

		}

	}

	/**
	 * comparable to {@link RoadModule#getSurfaceForNode}
	 */
	private static Material getSurfaceForNode(MapNode node) {
		if (node.getTags().containsKey("surface")) {
			return getSurfaceMaterial(node.getTags().getValue("surface"), null);
		} else {
			return getConnectedNetworkSegments(node, AerowaySegment.class, null).get(0).getSurface();
		}
	}

	public static class Helipad extends AbstractAreaWorldObject
			implements LegacyWorldObject {

		protected Helipad(MapArea area) {
			super(area);
		}

		@Override
		public Collection<PolygonShapeXZ> getRawGroundFootprint() {
			return List.of(getOutlinePolygonXZ());
		}

		@Override
		public void renderTo(Target target) {

			Function<TextureDataDimensions, TexCoordFunction> localXZTexCoordFunction = (TextureDataDimensions textureDimensions) -> {
				return (List<VectorXYZ> vs) -> {
					var globalXZ = new GlobalXZTexCoordFunction(textureDimensions);
					VectorXZ center = area.getOuterPolygon().getCentroid();
					VectorXZ shift = center.add(-textureDimensions.width() / 2, -textureDimensions.height() / 2);
					List<VectorXYZ> localCoords = vs.stream().map(v -> v.subtract(shift)).toList();
					return globalXZ.apply(localCoords);
				};
			};

			List<TriangleXYZ> triangles = getTriangulation();

			Material baseMaterial = getSurfaceMaterial(area.getTags().getValue("surface"), ASPHALT);
			List<List<VectorXZ>> baseTexCoords = triangleTexCoordLists(triangles, baseMaterial, GLOBAL_X_Z);

			Material fullMaterial = baseMaterial.withAddedLayers(HELIPAD_MARKING.getTextureLayers());
			List<List<VectorXZ>> texCoords = new ArrayList<>(baseTexCoords);
			texCoords.addAll(triangleTexCoordLists(triangles, HELIPAD_MARKING, localXZTexCoordFunction));

			target.drawTriangles(fullMaterial, triangles, texCoords);

		}

	}

	public static class Apron extends NetworkAreaWorldObject
			implements LegacyWorldObject {

		public Apron(MapArea area) {
			super(area);
		}

		@Override
		public void renderTo(Target target) {

			Material material = getSurfaceMaterial(area.getTags().getValue("surface"), ASPHALT);

			List<TriangleXYZ> triangles = getTriangulation();
			target.drawTriangles(material, triangles,
					triangleTexCoordLists(triangles, material, GLOBAL_X_Z));

		}

	}

	/** some linear "road" on an airport, e.g. a runway or taxiway */
	public static abstract class AerowaySegment extends AbstractNetworkWaySegmentWorldObject
		implements LegacyWorldObject {

		final float centerlineWidthMeters;

		protected AerowaySegment(MapWaySegment segment, float centerlineWidthMeters) {
			super(segment);
			this.centerlineWidthMeters = centerlineWidthMeters;
		}

		@Override
		public void renderTo(Target target) {

			List<VectorXYZ> leftOuter = getOutline(false);
			List<VectorXYZ> rightOuter = getOutline(true);

			// create geometry for the central marking
			double relativeMarkingWidth = min(centerlineWidthMeters / getWidth(), 0.2f);
			List<VectorXYZ> leftInner = createLineBetween(leftOuter, rightOuter, 0.5f * (1 - relativeMarkingWidth));
			List<VectorXYZ> rightInner = createLineBetween(leftOuter, rightOuter, 0.5f * (1 + relativeMarkingWidth));

			Material material = getSurface();

			List<VectorXYZ> leftVs = WorldModuleGeometryUtil.createTriangleStripBetween(leftOuter, leftInner);
			target.drawTriangleStrip(material, leftVs, texCoordLists(leftVs, material, GLOBAL_X_Z));

			List<VectorXYZ> rightVs = WorldModuleGeometryUtil.createTriangleStripBetween(rightInner, rightOuter);
			target.drawTriangleStrip(material, rightVs, texCoordLists(rightVs, material, GLOBAL_X_Z));

			material = getCenterlineSurface();

			List<VectorXYZ> centerVs = WorldModuleGeometryUtil.createTriangleStripBetween(leftInner, rightInner);
			target.drawTriangleStrip(material, centerVs, texCoordLists(centerVs, material, GLOBAL_X_Z));


		}

		Material getSurface() {
			return getSurfaceMaterial(segment.getTags().getValue("surface"), ASPHALT);
		}

		abstract Material getCenterlineSurface();

	}

	public static class Runway extends AerowaySegment {

		protected Runway(MapWaySegment segment) {
			super(segment, 0.9f);
		}

		@Override
		public double getWidth() {
			return parseWidth(segment.getTags(), 20.0);
		}

		@Override
		Material getCenterlineSurface() {
			if (getSurface() == ASPHALT || getSurface() == CONCRETE) {
				return getSurface().withAddedLayers(RUNWAY_CENTER_MARKING.getTextureLayers());
			} else {
				return getSurface();
			}
		}

	}

	public static class Taxiway extends AerowaySegment {

		protected Taxiway(MapWaySegment segment) {
			super(segment, 0.15f);
		}

		@Override
		public double getWidth() {
			return parseWidth(segment.getTags(), 5.0);
		}

		@Override
		Material getCenterlineSurface() {
			if (getSurface() == ASPHALT || getSurface() == CONCRETE) {
				return TAXIWAY_CENTER_MARKING;
			} else {
				return getSurface();
			}
		}

	}

	public static class AerowayJunction extends JunctionNodeWorldObject<AerowaySegment>
		implements LegacyWorldObject {

		public AerowayJunction(MapNode node) {
			super(node, AerowaySegment.class);
		}

		@Override
		public void renderTo(Target target) {

			Material material = getSurfaceForNode(node);
			List<TriangleXYZ> triangles = super.getTriangulation();

			target.drawTriangles(material, triangles,
					triangleTexCoordLists(triangles, material, GLOBAL_X_Z));

		}

	}

	public static class AerowayConnector extends VisibleConnectorNodeWorldObject<AerowaySegment>
		implements LegacyWorldObject {

		public AerowayConnector(MapNode node) {
			super(node, AerowaySegment.class);
		}

		@Override
		public double getLength() {
			// length is at most a third of the shorter segment's length
			return min(
					getConnectedNetworkSegments().get(0).segment.getLineSegment().getLength() / 3,
					getConnectedNetworkSegments().get(1).segment.getLineSegment().getLength() / 3);
		}

		@Override
		public void renderTo(Target target) {

			Material material = getSurfaceForNode(node);

			List<TriangleXYZ> trianglesXYZ = getTriangulation();

			target.drawTriangles(material, trianglesXYZ,
					triangleTexCoordLists(trianglesXYZ, material, GLOBAL_X_Z));

		}

	}

}
