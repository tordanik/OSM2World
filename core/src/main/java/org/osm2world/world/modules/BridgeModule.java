package org.osm2world.world.modules;

import static java.lang.Math.min;
import static org.osm2world.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.math.algorithms.GeometryUtil.*;
import static org.osm2world.math.shapes.SimplePolygonXZ.asSimplePolygon;
import static org.osm2world.scene.color.ColorNameDefinitions.CSS_COLORS;
import static org.osm2world.scene.material.DefaultMaterials.BRIDGE_DEFAULT;
import static org.osm2world.scene.material.DefaultMaterials.BRIDGE_PILLAR_DEFAULT;
import static org.osm2world.scene.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.scene.texcoord.NamedTexCoordFunction.STRIP_WALL;
import static org.osm2world.util.ListUtil.getFirst;
import static org.osm2world.util.ListUtil.getLast;
import static org.osm2world.util.ValueParseUtil.parseColor;
import static org.osm2world.util.ValueParseUtil.parseInt;
import static org.osm2world.world.data.ProceduralWorldObject.Target;
import static org.osm2world.world.modules.common.WorldModuleGeometryUtil.createTriangleStripBetween;
import static org.osm2world.world.modules.common.WorldModuleGeometryUtil.filterWorldObjectCollisions;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.osm2world.conversion.ConversionLog;
import org.osm2world.map_data.data.*;
import org.osm2world.map_data.data.overlaps.MapOverlap;
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
import org.osm2world.world.modules.common.ConfigurableWorldModule;
import org.osm2world.world.network.AbstractNetworkWaySegmentWorldObject;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * Adds bridges to the world.
 *
 * <p>Needs to be applied <em>after</em> all the modules that generate
 * whatever runs over the bridge.
 */
public class BridgeModule extends ConfigurableWorldModule {

	public static boolean isBridge(TagSet tags) {
		return tags.containsKey("bridge") && !"no".equals(tags.getValue("bridge"));
	}

	public static boolean isBridge(MapWaySegment segment) {
		return isBridge(segment.getTags());
	}


	@Override
	public final void applyTo(MapData mapData) {

		for (MapNode node : mapData.getMapNodes()) {
			// TODO: create piers, including ones which don't have a bridge on them (anymore)
		}

		for (MapWaySegment waySegment : mapData.getMapWaySegments()) {
			if (isBridge(waySegment)
					&& waySegment.getPrimaryRepresentation() instanceof AbstractNetworkWaySegmentWorldObject) {

				// only create a linear bridge if there is no man_made=bridge area for the segment
				if (waySegment.getOverlaps().stream().noneMatch(o -> {
					TagSet segmentTags = waySegment.getTags();
					TagSet otherTags = o.getOther(waySegment).getTags();
					return otherTags.contains("man_made", "bridge")
								&& (!segmentTags.containsKey("layer")
								|| Objects.equals(otherTags.getValue("layer"), segmentTags.getValue("layer")));
				})) {
					waySegment.addRepresentation(new BridgeWaySegment(waySegment));
				}

			}
		}

		for (MapArea area : mapData.getMapAreas()) {
			if (area.getTags().contains("man_made", "bridge")) {
				area.addRepresentation(new BridgeArea(area));
			}
		}

	}

	public static final double BRIDGE_UNDERSIDE_HEIGHT = 0.2f;

	private abstract class Bridge<E extends MapElement> implements ProceduralWorldObject {

		protected final E element;
		protected final int layer;

		protected PolygonShapeXZ polygon;
		protected List<PolylineXZ> edges;
		protected List<PolylineXZ> caps;

		/** centerline between two edges; only exists for bridges with exactly 2 edges and a simple shape */
		protected PolylineXZ centerline;

		protected List<BridgeSupportData> supports;

		private EleConnectorGroup connectors = null;
		private Multimap<PolylineXZ, EleConnector> capConnectors;
		private Map<BridgeSupportData, EleConnector> supportConnectors;

		private AttachmentSurface attachmentSurface = null;

		/**
		 * @param element  a {@link MapArea} (for man_made=bridge polygons) or a {@link MapWay} for bridge=yes ways
		 */
		public Bridge(E element) {

			this.element = element;

			this.layer = parseInt(element.getTags().getValue("layer"),0);

		}

		@Override
		public String toString() {
			return this.getClass().getSimpleName() + "(" + element + ")";
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

				initializeBridgeGeometry();

				connectors = new EleConnectorGroup();

				capConnectors = HashMultimap.create();
				for (PolylineXZ cap : caps) {
					for (VectorXZ v : cap.vertices()) {
						var connector = new EleConnector(v, null, GroundState.ON);
						connectors.add(connector);
						capConnectors.put(cap, connector);
					}
				}

				supportConnectors = new HashMap<>();
				for (BridgeSupportData support : supports) {
					var connector = new EleConnector(support.pos(), null, GroundState.ON);
					connectors.add(connector);
					supportConnectors.put(support, connector);
				}

			}

			return connectors;

		}

		@Override
		public Collection<AttachmentSurface> getAttachmentSurfaces() {

			if (attachmentSurface == null) {
				initializeBridgeGeometry();
				attachmentSurface = new AttachmentSurface(List.of("bridge-layer" + layer, "bridge"), this,
						getDeckTriangles(false));
			}

			return List.of(attachmentSurface);

		}

		@Override
		public @Nonnull PolygonShapeXZ getOutlinePolygonXZ() {
			return polygon.makeCounterclockwise();
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			initializeBridgeGeometry();

			/* draw deck */

			if (element instanceof MapArea) { // bridges based on network segments should be completely covered

				List<TriangleXYZ> trianglesXYZ = getDeckTriangles(true);

				if (!trianglesXYZ.isEmpty()) {

					Material deckMaterial = BRIDGE_DEFAULT.get(config);

					target.drawTriangles(deckMaterial, trianglesXYZ,
							TexCoordUtil.triangleTexCoordLists(trianglesXYZ, deckMaterial, GLOBAL_X_Z));

				}

			}

			{ /* draw underside and edges */

				Material undersideMaterial = BRIDGE_DEFAULT.get(config);

				List<TriangleXYZ> undersideTrianglesXYZ = getDeckTriangles(false).stream()
						.map(t -> t.shift(new VectorXYZ(0, -BRIDGE_UNDERSIDE_HEIGHT, 0)).reverse())
						.toList();

				target.drawTriangles(undersideMaterial, undersideTrianglesXYZ,
						TexCoordUtil.triangleTexCoordLists(undersideTrianglesXYZ, undersideMaterial, GLOBAL_X_Z));

				VectorXZ outlineCenter = polygon.getOuter().getCentroid();

				for (PolylineXZ edge : edges) {

					boolean bridgeIsRight = isRightOf(outlineCenter,
							getFirst(edge.vertices()), getLast(edge.vertices()));

					List<VectorXYZ> triangleStrip = createTriangleStripBetween(
							edge.xyz(p -> getBridgeEleAt(p) - (bridgeIsRight ? BRIDGE_UNDERSIDE_HEIGHT : 0)).getVertices(),
							edge.xyz(p -> getBridgeEleAt(p) - (bridgeIsRight ? 0 : BRIDGE_UNDERSIDE_HEIGHT)).getVertices());
					target.drawTriangleStrip(undersideMaterial, triangleStrip,
							TexCoordUtil.texCoordLists(triangleStrip, undersideMaterial, STRIP_WALL));

				}

			}

			/* draw the supports */

			for (BridgeSupportData support : supports) {
				support.renderTo(target, supportConnectors.get(support).getPosXYZ().y, this::getBridgeEleAt);
			}

		}

		/** makes sure the {@link #polygon}, {@link #edges} and {@link #caps} fields are populated */
		protected void initializeBridgeGeometry() {

			if (polygon != null) {
				// already calculated
				return;
			}

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

				/* calculate the centerline */

				if (this.edges.size() == 2 && this.caps.size() == 2) {

					// flip the second edge to make both point in the same direction
					edges.set(1, edges.get(1).reverse());

					VectorXZ start = getFirst(edges.get(0).vertices()).add(getFirst(edges.get(1).vertices())).mult(0.5);
					VectorXZ end = getLast(edges.get(0).vertices()).add(getLast(edges.get(1).vertices())).mult(0.5);

					if (caps.get(0).distanceTo(start) > caps.get(1).distanceTo(start)) {
						// swap the caps so that the "start" cap is at index 0
						Collections.swap(caps, 0, 1);
					}

					LineSegmentXZ centerlineCandidate = new LineSegmentXZ(start, end);

					if (edges.get(0).intersections(centerlineCandidate).isEmpty()
							&& edges.get(1).intersections(centerlineCandidate).isEmpty()) {

						this.centerline = new PolylineXZ(centerlineCandidate.vertices());

						// TODO: check that it's mostly in the center (distance to the edges not too different)

					}

				}

			} else if (element instanceof MapWaySegment segment) {

				AbstractNetworkWaySegmentWorldObject primaryRep =
						(AbstractNetworkWaySegmentWorldObject) segment.getPrimaryRepresentation();

				List<VectorXZ> leftOutline = primaryRep.getOutlineXZ(false);
				List<VectorXZ> rightOutline = primaryRep.getOutlineXZ(true);

				this.edges = List.of(new PolylineXZ(leftOutline), new PolylineXZ(rightOutline));
				this.caps = List.of(
						new PolylineXZ(getFirst(leftOutline), getFirst(rightOutline)),
						new PolylineXZ(getLast(leftOutline), getLast(rightOutline)));

				List<VectorXZ> outline = new ArrayList<>(leftOutline);
				outline.addAll(Lists.reverse(rightOutline));
				this.polygon = new SimplePolygonXZ(closeLoop(outline));

				this.centerline = primaryRep.getCenterlineXZ();

			} else {
				throw new IllegalArgumentException("Unsupported element type for bridge: " + element);
			}

			/* initialize supports */

			initializeSupports();

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

			return TriangulationUtil.triangulationXZtoXYZ(trianglesXZ,
					t -> t.xyz(getBridgeEleAt(t)));

		}


		/**
		 * Places supports for the bridge. These can be explicitly mapped with <code>bridge:support=*</code>.
		 * If no explicitly mapped supports are found, this method may place some at equal distances
		 * depending on the bridge's tags.
		 */
		private void initializeSupports() {

			supports = new ArrayList<>();

			/* determine defaults */

			Material defaultMaterial = BRIDGE_PILLAR_DEFAULT.get(config);

			SimplePolygonShapeXZ defaultShape;

			if (centerline != null) {
				double defaultWidth = 10 * 0.7; // TODO getBridgeWidthAt (ideally with left/right separation)
				double defaultLength = defaultWidth * 0.5;
				double angle = getLast(centerline.vertices()).subtract(getFirst(centerline.vertices())).angle();
				defaultShape = new AxisAlignedRectangleXZ(NULL_VECTOR, defaultWidth, defaultLength);
				defaultShape = defaultShape.rotatedCW(angle);
			} else {
				double defaultRadius = min(2.0, polygon.getDiameter() / 2);
				defaultShape = asSimplePolygon(new CircleXZ(NULL_VECTOR, defaultRadius));
			}

			/* look for explicitly mapped supports among the way's nodes and overlapping features */

			Collection<MapElement> explicitlyMappedSupports = new ArrayList<>();

			//note: there is currently no de-duplication of bridge support nodes shared by two bridge segments

			if (element instanceof MapWaySegment segment) {
				for (MapNode node : segment.getStartEndNodes()) {
					if (node.getTags().containsKey("bridge:support")) {
						explicitlyMappedSupports.add(node);
					}
				}
			}

			element.getOverlaps().stream()
					.map(it -> it.getOther(element))
					.filter(it -> it.getTags().containsKey("bridge:support"))
					.filter(it -> it instanceof MapNode || it instanceof MapArea)
					.forEach(explicitlyMappedSupports::add);

			if (!explicitlyMappedSupports.isEmpty()) {

				/* create the piers */

				for (MapElement element : explicitlyMappedSupports) {

					String bridgeSupportValue = element.getTags().getValue("bridge:support");
					if (List.of("pier", "lift_pier", "pivot_pier", "pylon").contains(bridgeSupportValue)) {

						VectorXZ pos = (element instanceof MapArea area)
								? area.getOuterPolygon().getCenter()
								: ((MapNode)element).getPos();

						SimplePolygonShapeXZ shape = (element instanceof MapArea area)
								? asSimplePolygon(area.getOuterPolygon().shift(pos.invert())).makeCounterclockwise()
								: defaultShape;

						Material material = null;
						if (element.getTags().containsKey("material")) {
							material = config.mapStyle().resolveMaterial(element.getTags().getValue("material"));
						}
						if (material == null) {
							material = BRIDGE_PILLAR_DEFAULT.get(config);
						}
						material = material.withColor(parseColor(element.getTags().getValue("colour"), CSS_COLORS));

						supports.add(new BridgeSupportData(pos, shape, material, element.getTags()));

					}

				}

			} else if (centerline != null) {

				/* no explicitly mapped supports found, distribute some equally along the bridge's length */

				double distance = min(50.0, centerline.getLength());

				List<VectorXZ> pierPositions = new ArrayList<>(equallyDistributePointsAlong(
						distance, false, centerline));

				/* make sure that the piers don't pierce anything on the ground */

				Collection<WorldObject> avoidedObjects = new ArrayList<>();

				for (MapOverlap<?, ?> i : element.getOverlaps()) {
					for (WorldObject otherRep : i.getOther(element).getRepresentations()) {

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

				/* create the piers */

				for (VectorXZ pos : pierPositions) {
					supports.add(new BridgeSupportData(pos, defaultShape, defaultMaterial, TagSet.of()));
				}

			}

		}

		private double getBridgeEleAt(VectorXZ pos) {

			if (capConnectors == null) throw new IllegalStateException("connectors not initialized");

			double ele;

			if (centerline != null) {
				double startEle = maxEle(capConnectors.get(caps.get(0)));
				double endEle = maxEle(capConnectors.get(caps.get(1)));
				double offset = centerline.offsetOf(centerline.closestPoint(pos)) / centerline.getLength();
				ele = offset * endEle + (1 - offset) * startEle;
			} else {
				// complex bridge, assume it's totally flat
				ele = maxEle(capConnectors.values()) + 0.1;
			}

			return ele + 0.1;

		}

		private static double maxEle(Collection<EleConnector> connectors) {
			return connectors.stream().mapToDouble(c -> c.getPosXYZ().y).max().orElse(0);
		}

	}

	private class BridgeArea extends Bridge<MapArea> implements AreaWorldObject {

		public BridgeArea(MapArea area) {
			super(area);
		}

	}

	private class BridgeWaySegment extends Bridge<MapWaySegment> implements WaySegmentWorldObject {

		public BridgeWaySegment(MapWaySegment waySegment) {
			super(waySegment);
		}

		@Override
		public VectorXZ getStartPosition() {
			WaySegmentWorldObject primaryRep = this.element.getPrimaryRepresentation();
			if (primaryRep != null && primaryRep != this) {
				return primaryRep.getStartPosition();
			} else {
				return element.getStartNode().getPos();
			}
		}

		@Override
		public VectorXZ getEndPosition() {
			WaySegmentWorldObject primaryRep = this.element.getPrimaryRepresentation();
			if (primaryRep != null && primaryRep != this) {
				return primaryRep.getEndPosition();
			} else {
				return element.getEndNode().getPos();
			}
		}

	}

	/** Data describing a bridge pier or other support element */
	record BridgeSupportData(VectorXZ pos, SimplePolygonShapeXZ shape, Material material, TagSet tags) {

		private void renderTo(Target target, double baseEle, Function<VectorXZ, Double> bridgeEleAt) {

			VectorXYZ top = pos.xyz(bridgeEleAt.apply(pos) - 0.9 * BRIDGE_UNDERSIDE_HEIGHT);

			baseEle -= 2.0; // sink into ground a bit

			if (top.y > baseEle) {
				target.drawExtrudedShape(material, shape, List.of(top.y(baseEle), top),
						null, null, EnumSet.of(ExtrudeOption.END_CAP));
			}

		}

	}

}
