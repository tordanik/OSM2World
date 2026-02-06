package org.osm2world.world.modules;

import static java.lang.Math.min;
import static org.osm2world.scene.material.DefaultMaterials.*;
import static org.osm2world.scene.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.scene.texcoord.TexCoordUtil.texCoordLists;
import static org.osm2world.scene.texcoord.TexCoordUtil.triangleTexCoordLists;
import static org.osm2world.world.modules.common.WorldModuleGeometryUtil.createLineBetween;
import static org.osm2world.world.modules.common.WorldModuleParseUtil.parseWidth;
import static org.osm2world.world.network.NetworkUtil.getConnectedNetworkSegments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.osm2world.map_data.data.MapArea;
import org.osm2world.map_data.data.MapData;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.MapWaySegment;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.PolygonShapeXZ;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.material.MaterialOrRef;
import org.osm2world.scene.material.TextureDataDimensions;
import org.osm2world.scene.texcoord.GlobalXZTexCoordFunction;
import org.osm2world.scene.texcoord.TexCoordFunction;
import org.osm2world.world.data.AbstractAreaWorldObject;
import org.osm2world.world.data.ProceduralWorldObject;
import org.osm2world.world.modules.common.ConfigurableWorldModule;
import org.osm2world.world.modules.common.WorldModuleGeometryUtil;
import org.osm2world.world.network.AbstractNetworkWaySegmentWorldObject;
import org.osm2world.world.network.JunctionNodeWorldObject;
import org.osm2world.world.network.NetworkAreaWorldObject;
import org.osm2world.world.network.VisibleConnectorNodeWorldObject;

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
	private static MaterialOrRef getSurfaceForNode(MapNode node) {
		if (node.getTags().containsKey("surface")) {
			return getSurfaceMaterial(node.getTags().getValue("surface"), null);
		} else {
			return getConnectedNetworkSegments(node, AerowaySegment.class, null).get(0).getSurface();
		}
	}

	public class Helipad extends AbstractAreaWorldObject
			implements ProceduralWorldObject {

		protected Helipad(MapArea area) {
			super(area);
		}

		@Override
		public Collection<PolygonShapeXZ> getRawGroundFootprint() {
			return List.of(getOutlinePolygonXZ());
		}

		@Override
		public void buildMeshesAndModels(Target target) {

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

			Material baseMaterial = getSurfaceMaterial(area.getTags().getValue("surface"), ASPHALT, config);
			List<List<VectorXZ>> baseTexCoords = triangleTexCoordLists(triangles, baseMaterial, GLOBAL_X_Z);

			Material fullMaterial = baseMaterial.withAddedLayers(HELIPAD_MARKING.get(config).textureLayers());
			List<List<VectorXZ>> texCoords = new ArrayList<>(baseTexCoords);
			texCoords.addAll(triangleTexCoordLists(triangles, HELIPAD_MARKING.get(config), localXZTexCoordFunction));

			target.drawTriangles(fullMaterial, triangles, texCoords);

		}

	}

	public class Apron extends NetworkAreaWorldObject
			implements ProceduralWorldObject {

		public Apron(MapArea area) {
			super(area);
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			Material material = getSurfaceMaterial(area.getTags().getValue("surface"), ASPHALT, config);

			List<TriangleXYZ> triangles = getTriangulation();
			target.drawTriangles(material, triangles,
					triangleTexCoordLists(triangles, material, GLOBAL_X_Z));

		}

	}

	/** some linear "road" on an airport, e.g. a runway or taxiway */
	public abstract class AerowaySegment extends AbstractNetworkWaySegmentWorldObject
		implements ProceduralWorldObject {

		final float centerlineWidthMeters;

		protected AerowaySegment(MapWaySegment segment, float centerlineWidthMeters) {
			super(segment);
			this.centerlineWidthMeters = centerlineWidthMeters;
		}

		@Override
		public void buildMeshesAndModels(Target target) {

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
			return getSurfaceMaterial(segment.getTags().getValue("surface"), ASPHALT, config);
		}

		abstract Material getCenterlineSurface();

	}

	public class Runway extends AerowaySegment {

		protected Runway(MapWaySegment segment) {
			super(segment, 0.9f);
		}

		@Override
		public double getWidth() {
			return parseWidth(segment.getTags(), 20.0);
		}

		@Override
		Material getCenterlineSurface() {
			Material material = getSurface();
			if (List.of("ASPHALT", "CONCRETE").contains(config.mapStyle().getMaterialName(material))) {
				return getSurface().withAddedLayers(RUNWAY_CENTER_MARKING.get(config).textureLayers());
			} else {
				return getSurface();
			}
		}

	}

	public class Taxiway extends AerowaySegment {

		protected Taxiway(MapWaySegment segment) {
			super(segment, 0.15f);
		}

		@Override
		public double getWidth() {
			return parseWidth(segment.getTags(), 5.0);
		}

		@Override
		Material getCenterlineSurface() {
			Material material = getSurface();
			if (List.of("ASPHALT", "CONCRETE").contains(config.mapStyle().getMaterialName(material))) {
				return TAXIWAY_CENTER_MARKING.get(config);
			} else {
				return getSurface();
			}
		}

	}

	public class AerowayJunction extends JunctionNodeWorldObject<AerowaySegment>
		implements ProceduralWorldObject {

		public AerowayJunction(MapNode node) {
			super(node, AerowaySegment.class);
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			Material material = getSurfaceForNode(node).get(config);
			List<TriangleXYZ> triangles = super.getTriangulation();

			target.drawTriangles(material, triangles,
					triangleTexCoordLists(triangles, material, GLOBAL_X_Z));

		}

	}

	public class AerowayConnector extends VisibleConnectorNodeWorldObject<AerowaySegment>
		implements ProceduralWorldObject {

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
		public void buildMeshesAndModels(Target target) {

			Material material = getSurfaceForNode(node).get(config);

			List<TriangleXYZ> trianglesXYZ = getTriangulation();

			target.drawTriangles(material, trianglesXYZ,
					triangleTexCoordLists(trianglesXYZ, material, GLOBAL_X_Z));

		}

	}

}
