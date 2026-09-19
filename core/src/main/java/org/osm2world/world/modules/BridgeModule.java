package org.osm2world.world.modules;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static org.osm2world.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.math.algorithms.GeometryUtil.*;
import static org.osm2world.math.shapes.SimplePolygonXZ.asSimplePolygon;
import static org.osm2world.scene.color.ColorNameDefinitions.CSS_COLORS;
import static org.osm2world.scene.material.DefaultMaterials.BRIDGE_DEFAULT;
import static org.osm2world.scene.material.DefaultMaterials.BRIDGE_PILLAR_DEFAULT;
import static org.osm2world.scene.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.scene.texcoord.NamedTexCoordFunction.STRIP_WALL;
import static org.osm2world.scene.texcoord.TexCoordUtil.texCoordLists;
import static org.osm2world.util.ListUtil.getFirst;
import static org.osm2world.util.ListUtil.getLast;
import static org.osm2world.util.ValueParseUtil.parseColor;
import static org.osm2world.util.ValueParseUtil.parseInt;
import static org.osm2world.world.modules.common.WorldModuleGeometryUtil.createTriangleStripBetween;
import static org.osm2world.world.modules.common.WorldModuleGeometryUtil.filterWorldObjectCollisions;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.osm2world.conversion.ConversionLog;
import org.osm2world.map_data.data.*;
import org.osm2world.map_data.data.overlaps.MapIntersectionWW;
import org.osm2world.map_data.data.overlaps.MapOverlapWA;
import org.osm2world.map_elevation.data.EleConnector;
import org.osm2world.map_elevation.data.EleConnectorGroup;
import org.osm2world.map_elevation.data.GroundState;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.FaceDecompositionUtil;
import org.osm2world.math.algorithms.TriangulationUtil;
import org.osm2world.math.shapes.*;
import org.osm2world.output.common.ExtrudeOption;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.texcoord.TexCoordUtil;
import org.osm2world.world.attachment.AttachmentConnector;
import org.osm2world.world.attachment.AttachmentSurface;
import org.osm2world.world.data.AreaWorldObject;
import org.osm2world.world.data.ProceduralWorldObject;
import org.osm2world.world.data.WaySegmentWorldObject;
import org.osm2world.world.data.WorldObject;
import org.osm2world.world.modules.SurfaceAreaModule.SurfaceArea;
import org.osm2world.world.modules.WaterModule.Water;
import org.osm2world.world.modules.WaterModule.Waterway;
import org.osm2world.world.modules.common.BridgeOrTunnel;
import org.osm2world.world.modules.common.ConfigurableWorldModule;
import org.osm2world.world.network.AbstractNetworkWaySegmentWorldObject;

/**
 * Adds bridges to the world.
 *
 * <p>Needs to be applied <em>after</em> all the modules that generate
 * whatever runs over the bridge.
 */
public class BridgeModule extends ConfigurableWorldModule {

	public static final boolean isBridge(TagSet tags) {
		return tags.containsKey("bridge")
			&& !"no".equals(tags.getValue("bridge"));
	}

	public static final boolean isBridge(MapWaySegment segment) {
		return isBridge(segment.getTags());
	}


	@Override
	public final void applyTo(MapData mapData) {

		for (MapNode node : mapData.getMapNodes()) {
			// TODO: create piers
		}

		for (MapWaySegment waySegment : mapData.getMapWaySegments()) {
			if (waySegment.getRepresentations().isEmpty()) {
				applyToWaySegment(waySegment);
			}
		}

		for (MapArea area : mapData.getMapAreas()) {
			if (area.getTags().contains("man_made", "bridge")) {
				area.addRepresentation(new BridgeArea(area));
			}
		}

	}

	protected void applyToWaySegment(MapWaySegment segment) {

		WaySegmentWorldObject primaryRepresentation =
			segment.getPrimaryRepresentation();

		if (primaryRepresentation instanceof AbstractNetworkWaySegmentWorldObject
				&& isBridge(segment)) {

			segment.addRepresentation(new OldBridgeWay(segment,
					(AbstractNetworkWaySegmentWorldObject) primaryRepresentation));

		}

	}

	public static final double BRIDGE_UNDERSIDE_HEIGHT = 0.2f;

	private class OldBridgeWay implements BridgeOrTunnel {

		protected final MapWaySegment segment;
		protected final AbstractNetworkWaySegmentWorldObject primaryRep;

		public OldBridgeWay(MapWaySegment segment,
				AbstractNetworkWaySegmentWorldObject primaryWO) {
			this.segment = segment;
			this.primaryRep = primaryWO;
		}

		@Override
		public String toString() {
			return this.getClass().getSimpleName() + "(" + segment + ")";
		}

		@Override
		public MapWaySegment getPrimaryMapElement() {
			return segment;
		}

		@Override
		public AbstractNetworkWaySegmentWorldObject getPrimaryRep() {
			return primaryRep;
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ABOVE;
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			drawBridgeUnderside(target);

			drawBridgePiers(target);

		}

		private void drawBridgeUnderside(Target target) {

			List<VectorXYZ> leftOutline = primaryRep.getOutline(false);
			List<VectorXYZ> rightOutline = primaryRep.getOutline(true);

			List<VectorXYZ> belowLeftOutline = sequenceAbove(leftOutline, -BRIDGE_UNDERSIDE_HEIGHT);
			List<VectorXYZ> belowRightOutline = sequenceAbove(rightOutline, -BRIDGE_UNDERSIDE_HEIGHT);

			List<VectorXYZ> strip1 = createTriangleStripBetween(
					belowLeftOutline, leftOutline);
			List<VectorXYZ> strip2 = createTriangleStripBetween(
					belowRightOutline, belowLeftOutline);
			List<VectorXYZ> strip3 = createTriangleStripBetween(
					rightOutline, belowRightOutline);

			target.drawTriangleStrip(BRIDGE_DEFAULT.get(config), strip1,
					texCoordLists(strip1, BRIDGE_DEFAULT.get(config), STRIP_WALL));
			target.drawTriangleStrip(BRIDGE_DEFAULT.get(config), strip2,
					texCoordLists(strip2, BRIDGE_DEFAULT.get(config), STRIP_WALL));
			target.drawTriangleStrip(BRIDGE_DEFAULT.get(config), strip3,
					texCoordLists(strip3, BRIDGE_DEFAULT.get(config), STRIP_WALL));

		}

		/**
		 * draws supports for the bridge. These can be explicitly mapped with bridge:support=*.
		 * If no explicitly mapped supports are found, this method places some at equal distances.
		 */
		private void drawBridgePiers(Target target) {

			/* determine defaults */

			double defaultWidth = primaryRep.getWidth() * 0.7;
			double defaultLength = defaultWidth * 0.5;
			SimplePolygonShapeXZ defaultShape = new AxisAlignedRectangleXZ(NULL_VECTOR, defaultWidth, defaultLength);
			defaultShape = defaultShape.rotatedCW(primaryRep.getPrimaryMapElement().getDirection().angle());

			Material defaultMaterial = BRIDGE_PILLAR_DEFAULT.get(config);

			/* look for explicitly mapped supports among the way's nodes and overlapping features */

			Collection<MapElement> explicitlyMappedSupports = new ArrayList<>();

			//note: there is currently no de-duplication of bridge support nodes shared by two bridge segments

			for (MapNode node : primaryRep.segment.getStartEndNodes()) {
				if (node.getTags().containsKey("bridge:support")) {
					explicitlyMappedSupports.add(node);
				}
			}

			segment.getOverlaps().stream()
					.filter(overlap -> overlap instanceof MapOverlapWA)
					.map(overlap -> ((MapOverlapWA)overlap).e2)
					.filter(area -> area.getTags().containsKey("bridge:support"))
					.forEach(explicitlyMappedSupports::add);

			if (!explicitlyMappedSupports.isEmpty()) {

				/* draw the piers */

				for (MapElement element : explicitlyMappedSupports) {

					String bridgeSupportValue = element.getTags().getValue("bridge:support");
					if (asList("pier", "lift_pier", "pivot_pier", "pylon").contains(bridgeSupportValue)) {

						VectorXZ pos = (element instanceof MapArea)
								? ((MapArea)element).getOuterPolygon().getCenter()
								: ((MapNode)element).getPos();

						SimplePolygonShapeXZ shape = (element instanceof MapArea)
								? ((MapArea)element).getOuterPolygon()
								: defaultShape;

						Material material = null;
						if (element.getTags().containsKey("material")) {
							material = config.mapStyle().resolveMaterial(element.getTags().getValue("material"));
						}
						if (material == null) {
							material = BRIDGE_PILLAR_DEFAULT.get(config);
						}
						material = material.withColor(parseColor(element.getTags().getValue("colour"), CSS_COLORS));

						drawBridgePierAt(target, pos, shape, material);

					}

				}

			} else {

				/* no explicitly mapped supports found, distribute some equally along the bridge's length */

				double distance = min(50.0, primaryRep.getPrimaryMapElement().getLineSegment().getLength() / 2);

				List<VectorXZ> pierPositions = equallyDistributePointsAlong((float)distance, false,
						primaryRep.getStartPosition(), primaryRep.getEndPosition());

				/* make sure that the piers don't pierce anything on the ground */

				Collection<WorldObject> avoidedObjects = new ArrayList<>();

				for (MapIntersectionWW i : segment.getIntersectionsWW()) {
					for (WorldObject otherRep : i.getOther(segment).getRepresentations()) {

						if (otherRep.getGroundState() == GroundState.ON
								&& !(otherRep instanceof Water
										|| otherRep instanceof Waterway
										|| otherRep instanceof SurfaceArea) //TODO: choose better criteria!
						) {
							avoidedObjects.add(otherRep);
						}

					}
				}

				filterWorldObjectCollisions(pierPositions, avoidedObjects);

				/* draw the piers */

				for (VectorXZ pos : pierPositions) {
					drawBridgePierAt(target, pos, defaultShape, defaultMaterial);
				}

			}

		}

		private void drawBridgePierAt(Target target, VectorXZ pos, ClosedShapeXZ crossSection, Material material) {

			/* determine the bridge elevation at that point */

			PolylineXZ centerlineXZ = primaryRep.getCenterlineXZ();
			double offset = centerlineXZ.offsetOf(centerlineXZ.closestPoint(pos));
			double ratio = offset / centerlineXZ.getLength();

			//note: there's a small inaccuracy because the length along the 3D line is different than in 2D
			VectorXYZ top = interpolateOn(primaryRep.getCenterline(), ratio);
			top = top.addY(-0.5 * BRIDGE_UNDERSIDE_HEIGHT);

			/* draw the pillar */

			// TODO: start pillar at ground instead of just x meters below the bridge
			VectorXYZ base = top.y(max(top.y-20, -3));
			target.drawExtrudedShape(material, crossSection,
					asList(base, top), null, null, EnumSet.of(ExtrudeOption.END_CAP));

		}

	}
	private abstract class Bridge<E extends MapElement> implements ProceduralWorldObject {

		private final E element;
		private final @Nullable MapRelation relation;
		private final int layer;

		private EleConnectorGroup connectors = null;
		private AttachmentSurface attachmentSurface = null;

		private final PolygonShapeXZ polygon;
		private final List<PolylineXZ> edges;
		private final List<PolylineXZ> caps;

		/**
		 * @param element  a {@link MapArea} (for man_made=bridge polygons) or a {@link MapWay} for bridge=yes ways
		 */
		public Bridge(E element) {

			this.element = element;

			this.layer = parseInt(element.getTags().getValue("layer"),0);

			/* find the bridge relation associated with this bridge (if any) */

			MapRelation relation = null;

			for (MapRelation.Membership membership : element.getElementWithId().getMemberships()) {
				if (membership.getRelation().getTags().contains("type", "bridge")
						&& "outline".equals(membership.getRole())) {
					if (relation == null) {
						relation = membership.getRelation();
					} else {
						ConversionLog.warn("More than one bridge relation for bridge outline", element.getElementWithId());
					}
				}
			}

			this.relation = relation;

			Set<MapWaySegment> edgeMemberSegments = Set.of();

			if (relation != null) {
				edgeMemberSegments = relation.getMembers().stream()
						.filter(m -> "edge".equals(m.getRole()))
						.filter(m -> m.getElement() instanceof MapWay)
						.map(m -> (MapWay) m.getElement())
						.flatMap(m -> m.getWaySegments().stream())
						.collect(Collectors.toSet());
			}

			if (element instanceof MapArea area) {

				this.polygon = area.getPolygon();

				/* split the bridge outline polygon into:
				 * - edges (sides which are in the air)
				 * - caps (sides where the bridge ends at the ground) */

				List<PolylineXZ> edges = new ArrayList<>(3);
				List<PolylineXZ> caps = new ArrayList<>(3);

				SimplePolygonShapeXZ outline = polygon.getOuter();

				List<LineSegmentXZ> currentPolyline = new ArrayList<>();
				boolean currentIsCap = false;

				for (MapAreaSegment segment : area.getAreaSegmentsOuter()) {

					boolean isCap;

					if (!edgeMemberSegments.isEmpty()) {
						// rely on explicit edge member mapping
						isCap = edgeMemberSegments.stream().noneMatch(s ->
								(s.getStartNode().equals(segment.getStartNode())
										&& s.getEndNode().equals(segment.getEndNode()))
										|| (s.getEndNode().equals(segment.getStartNode()) &&
										s.getStartNode().equals(segment.getEndNode())));
					} else {
						isCap = isConnectedToGround(segment.getStartNode(), outline)
								|| isConnectedToGround(segment.getEndNode(), outline);
					}

					if (isCap ^ currentIsCap) {
						if (!currentPolyline.isEmpty()) {
							if (currentIsCap) {
								caps.add(PolylineXZ.join(currentPolyline));
							} else {
								edges.add(PolylineXZ.join(currentPolyline));
							}
						}
						currentPolyline.clear();
						currentIsCap = isCap;
					}

					currentPolyline.add(segment.getLineSegment());

				}

				PolylineXZ finalPolyline = PolylineXZ.join(currentPolyline);

				if (currentIsCap) {
					if (!caps.isEmpty() && getFirst(caps.get(0).vertices()).equals(getLast(finalPolyline.vertices()))) {
						caps.set(0, PolylineXZ.join(List.of(finalPolyline, caps.get(0))));
					} else {
						caps.add(finalPolyline);
					}
				} else {
					if (!edges.isEmpty() && getFirst(edges.get(0).vertices()).equals(getLast(finalPolyline.vertices()))) {
						edges.set(0, PolylineXZ.join(List.of(finalPolyline, edges.get(0))));
					} else {
						edges.add(finalPolyline);
					}
				}

				this.edges = edges;
				this.caps = caps;

			} else {
				throw new IllegalArgumentException("Unsupported element type for bridge: " + element);
			}

		}

		/**
		 * Applies several heuristics to find out if a point of the bridge outline is likely connected to the ground
		 * (or some other solid end of the bridge, such as a building).
		 */
		private boolean isConnectedToGround(MapNode node, SimplePolygonShapeXZ outline) {

			/* building entrances */

			if (node.getTags().containsKey("entrance")
			 	&& node.getAdjacentAreas().stream().anyMatch(a -> a.getTags().containsKey("building"))) {
				return true;
			}

			/* nodes connected to (non-bridge) highways, railways or waterways entering the outline */

			boolean hasGroundWaysOutside = false;
			boolean hasBridgeWayInside = false;

			for (MapWaySegment segment : node.getConnectedWaySegments()) {
				WaySegmentWorldObject rep = segment.getPrimaryRepresentation();
				if (rep instanceof RoadModule.Road || rep instanceof RailwayModule.Rail
						|| rep instanceof Waterway) {
					boolean isInside = outline.contains(segment.getCenter());
					boolean isBridgeWay = segment.getTags().containsKey("bridge")
							&& !"no".equals(segment.getTags().getValue("bridge"));
					if (isInside && isBridgeWay) {
							hasBridgeWayInside = true;
					} else if (!isInside && !isBridgeWay) {
						hasGroundWaysOutside = true;
					}
				}
			}

			return hasBridgeWayInside && hasGroundWaysOutside;

		}

		@Override
		public E getPrimaryMapElement() {
			return element;
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ABOVE;
		}

		@Override
		public Iterable<EleConnector> getEleConnectors() {

			if (connectors == null) {

				connectors = new EleConnectorGroup();

				for (PolylineXZ cap : caps) {
					for (VectorXZ v : cap.vertices()) {
						connectors.add(new EleConnector(v, null, GroundState.ON));
					}
				}

			}

			return connectors;

		}

		@Override
		public Collection<AttachmentSurface> getAttachmentSurfaces() {

			if (attachmentSurface == null) {
				attachmentSurface = new AttachmentSurface(List.of("bridge-layer" + layer, "bridge"), this,
						getDeckTriangles(false));
			}

			return List.of(attachmentSurface);

		}

		@Override
		public void buildMeshesAndModels(Target target) {

			/* draw deck */

			List<TriangleXYZ> trianglesXYZ = getDeckTriangles(true);

			Material deckMaterial = BRIDGE_DEFAULT.get(config);

			target.drawTriangles(deckMaterial, trianglesXYZ,
					TexCoordUtil.triangleTexCoordLists(trianglesXYZ, deckMaterial, GLOBAL_X_Z));

			{ /* draw underside and edges */

				Material undersideMaterial = BRIDGE_DEFAULT.get(config);

				List<TriangleXYZ> undersideTrianglesXYZ = getDeckTriangles(false).stream()
						.map(t -> t.shift(new VectorXYZ(0, -BRIDGE_UNDERSIDE_HEIGHT, 0)).reverse())
						.toList();

				target.drawTriangles(undersideMaterial, undersideTrianglesXYZ,
						TexCoordUtil.triangleTexCoordLists(undersideTrianglesXYZ, undersideMaterial, GLOBAL_X_Z));

				VectorXZ outlineCenter = polygon.getOuter().getCentroid();

				for (PolylineXZ edge : edges) {

					// TODO: determine which should be "left"

					boolean bridgeIsRight = isRightOf(outlineCenter,
							getFirst(edge.vertices()), getLast(edge.vertices()));

					List<VectorXYZ> triangleStrip = createTriangleStripBetween(
							edge.xyz(getBridgeEle() - (bridgeIsRight ? BRIDGE_UNDERSIDE_HEIGHT : 0)).getVertices(),
							edge.xyz(getBridgeEle() - (bridgeIsRight ? 0 : BRIDGE_UNDERSIDE_HEIGHT)).getVertices());
					target.drawTriangleStrip(undersideMaterial, triangleStrip,
							TexCoordUtil.texCoordLists(triangleStrip, undersideMaterial, STRIP_WALL));

				}

			}

		}


		private List<TriangleXYZ> getDeckTriangles(boolean subtractAttachedObjects) {

			PolygonShapeXZ polygon = getOutlinePolygonXZ();

			/* subtract attached features from the deck surface */

			List<PolygonShapeXZ> subtractPolys = new ArrayList<>();

			if (subtractAttachedObjects && attachmentSurface != null) {
				for (AttachmentConnector connector : attachmentSurface.getAttachedConnectors()) {
					if (connector.object != null) {
						subtractPolys.addAll(connector.object.getRawGroundFootprint());
					}
				}
			}

			subtractPolys.addAll(polygon.getHoles());

			/* triangulate the (remaining) polygon */

			List<SimplePolygonXZ> holes = subtractPolys.stream().map(p -> asSimplePolygon(p.getOuter())).toList();

			Collection<PolygonWithHolesXZ> faces = FaceDecompositionUtil.splitPolygonIntoFaces(
					polygon.getOuter(),
					holes,
					List.of() // TODO: in the future, inner segments may be useful to achieve a curved shape
			);

			List<TriangleXZ> trianglesXZ = new ArrayList<>();
			faces.forEach(f -> trianglesXZ.addAll(f.getTriangulation()));

			/* assign elevations to the triangulation */

			double bridgeEle = getBridgeEle();
			return TriangulationUtil.triangulationXZtoXYZ(trianglesXZ,
					t -> t.xyz(bridgeEle));

		}

		private double getBridgeEle() {
			// TODO replace with proper ele calculation and support non-flat bridges
			if (connectors == null) throw new IllegalStateException("connectors not initialized");
			OptionalDouble maxEle = connectors.eleConnectors.stream().mapToDouble(c -> c.getPosXYZ().y).max();
			return maxEle.orElse(0) + 0.1;
		}

	}

	private class BridgeArea extends Bridge<MapArea> implements AreaWorldObject {

		public BridgeArea(MapArea area) {
			super(area);
		}

	}

}
