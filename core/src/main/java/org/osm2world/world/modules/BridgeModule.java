package org.osm2world.world.modules;

import static java.lang.Math.*;
import static org.osm2world.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.math.algorithms.GeometryUtil.*;
import static org.osm2world.math.algorithms.TriangulationUtil.triangulateXYZ;
import static org.osm2world.math.shapes.SimplePolygonXZ.asSimplePolygon;
import static org.osm2world.scene.color.ColorNameDefinitions.CSS_COLORS;
import static org.osm2world.scene.material.DefaultMaterials.BRIDGE_DEFAULT;
import static org.osm2world.scene.material.DefaultMaterials.BRIDGE_PILLAR_DEFAULT;
import static org.osm2world.scene.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.scene.texcoord.NamedTexCoordFunction.STRIP_WALL;
import static org.osm2world.util.ListUtil.getFirst;
import static org.osm2world.util.ListUtil.getLast;
import static org.osm2world.util.ValueParseUtil.*;
import static org.osm2world.world.data.ProceduralWorldObject.Target;
import static org.osm2world.world.modules.common.WorldModuleGeometryUtil.createTriangleStripBetween;
import static org.osm2world.world.modules.common.WorldModuleGeometryUtil.filterWorldObjectCollisions;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
import org.osm2world.scene.material.Material;
import org.osm2world.scene.material.Material.Interpolation;
import org.osm2world.scene.mesh.TriangleGeometry;
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

	public abstract class Bridge<E extends MapElement> implements ProceduralWorldObject {

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

		public List<PolylineXZ> getEdges() {
			initializeBridgeGeometry();
			return edges;
		}

		public List<PolylineXZ> getCaps() {
			initializeBridgeGeometry();
			return caps;
		}

		public @Nullable PolylineShapeXZ getCenterline() {
			initializeBridgeGeometry();
			return centerline;
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

				// don't use the original edges, those don't have the extra intersection points for curved bridges
				Set<LineSegmentXYZ> triangulationEdges = new TriangleGeometry(undersideTrianglesXYZ,
						Interpolation.FLAT, List.of(), null)
						.edges();

				VectorXZ outlineCenter = polygon.getOuter().getCentroid();

				for (LineSegmentXYZ edge : triangulationEdges) {

					boolean bridgeIsRight = isRightOf(outlineCenter, edge.p1.xz(), edge.p2.xz());

					List<VectorXYZ> triangleStrip = createTriangleStripBetween(
							edge.shift(new VectorXYZ(0, bridgeIsRight ? 0 : BRIDGE_UNDERSIDE_HEIGHT, 0)).vertices(),
							edge.shift(new VectorXYZ(0, bridgeIsRight ? BRIDGE_UNDERSIDE_HEIGHT : 0, 0)).vertices());
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

					if (caps.get(0).distanceTo(start) > caps.get(1).distanceTo(start)) {
						// swap the caps so that the "start" cap is at index 0
						Collections.swap(caps, 0, 1);
					}

					this.centerline = calculateCenterlineBetween(edges.get(0), edges.get(1));

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

		static @Nullable PolylineXZ calculateCenterlineBetween(PolylineShapeXZ edge0, PolylineShapeXZ edge1) {

			record Cut(VectorXZ p0, VectorXZ p1) {
				public VectorXZ center() {
					return p0.add(p1).mult(0.5);
				}
			}

			List<Cut> cuts = new ArrayList<>();

			Cut firstCut = new Cut(getFirst(edge0.vertices()), getFirst(edge1.vertices()));
			cuts.add(firstCut);

			for (VectorXZ p0 : edge0.vertices().subList(1, edge0.vertices().size() - 1)) {
				VectorXZ p1 = edge1.closestPoint(p0);
				cuts.add(new Cut(p0, p1));
			}

			for (VectorXZ p1 : edge1.vertices().subList(1, edge1.vertices().size() - 1)) {
				VectorXZ p0 = edge0.closestPoint(p1);
				cuts.add(new Cut(p0, p1));
			}

			Cut lastCut = new Cut(getLast(edge0.vertices()), getLast(edge1.vertices()));
			cuts.add(lastCut);

			cuts.sort(Comparator.comparingDouble((Cut c) -> edge0.offsetOf(c.p0))
					.thenComparingDouble(c -> edge1.offsetOf(c.p1)));

			// remove cuts where the order along the two edges is contradictory
			double previousOffset = 0;
			Iterator<Cut> iter = cuts.iterator();
			while (iter.hasNext()) {
				double offset = edge1.offsetOf(iter.next().p1);
				if (offset < previousOffset) {
					iter.remove();
				} else {
					previousOffset = offset;
				}
			}

			List<VectorXZ> centerlinePoints = new ArrayList<>();
			for (Cut cut : cuts) {
				VectorXZ p = cut.center();
				if (centerlinePoints.isEmpty() || getLast(centerlinePoints).distanceTo(p) > 0.01) {
					centerlinePoints.add(p);
				}
			}

			if (centerlinePoints.size() < 2) {
				return null;
			}

			PolylineXZ centerline = new PolylineXZ(centerlinePoints);

			if (getFirst(cuts) != firstCut || getLast(cuts) != lastCut) {
				return null;
			} else if (!centerline.getSegments().stream().allMatch(s ->
					edge0.intersections(s).isEmpty() && edge1.intersections(s).isEmpty())) {
				return null;
			} else {
				return centerline;
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

		private List<TriangleXYZ> getDeckTriangles(boolean subtractAttachedObjects) {

			PolygonShapeXZ polygon = getOutlinePolygonXZ();

			/* find attached features to subtract from the deck surface */

			List<PolygonShapeXZ> subtractPolys = new ArrayList<>();

			if (subtractAttachedObjects && attachmentSurface != null) {
				for (AttachmentConnector connector : attachmentSurface.getAttachedConnectors()) {
					if (connector.object != null) {
						subtractPolys.addAll(connector.object.getRawGroundFootprint());
					}
				}
			}

			subtractPolys.addAll(polygon.getHoles());

			/* define some lines at regular distances to allow the bridge to be (vertically) curved */

			List<ShapeXZ> extraLines = List.of();

			if (centerline != null && getCurvatureHeightPerLength() != 0) {

				double dist = min(5, centerline.getLength() / 4);
				List<VectorXZ> splitPoints = equallyDistributePointsAlong(dist, true, centerline);

				extraLines = new ArrayList<>(splitPoints.size());

				for (int i = 1; i < splitPoints.size() - 1; i++) { // skip start and end point
					VectorXZ p = splitPoints.get(i);
					double halfWidth = 100.0; // TODO use the actual bridge width
					VectorXZ normal = centerline.closestSegment(p).getDirection().rightNormal();
					extraLines.add(new LineSegmentXZ(p.add(normal.mult(-halfWidth)), p.add(normal.mult(halfWidth))));
				}

			}

			/* triangulate the (remaining) polygon */

			List<SimplePolygonXZ> holes = subtractPolys.stream().map(p -> asSimplePolygon(p.getOuter())).toList();

			Collection<PolygonWithHolesXZ> faces = FaceDecompositionUtil.splitPolygonIntoFaces(
					polygon.getOuter(),
					holes,
					extraLines
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
								: null;

						supports.add(createPier(pos, shape, element.getTags()));

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
					supports.add(createPier(pos, null, TagSet.of()));
				}

			}

		}

		private BridgeSupportData createPier(VectorXZ pos, @Nullable SimplePolygonShapeXZ shape, TagSet tags) {

			/* build shape */

			if (shape == null) {
				if (centerline != null) {
					double width = parseMeasure(tags.getValue("width"), getBridgeWidthAt(pos) * 0.7);
					double length = parseMeasure(tags.getValue("length"), width * 0.5);
					double angle = centerline.closestSegment(pos).getDirection().angle();
					shape = new AxisAlignedRectangleXZ(NULL_VECTOR, width, length);
					shape = shape.rotatedCW(angle);
				} else {
					double defaultDiameter = parseMeasure(tags.getValue("width"), min(4.0, polygon.getDiameter()));
					shape = asSimplePolygon(new CircleXZ(NULL_VECTOR, defaultDiameter / 2));
				}
			}

			/* build material */

			Material material = null;
			if (tags.containsKey("material")) {
				material = config.mapStyle().resolveMaterial(element.getTags().getValue("material"));
			}
			if (material == null) {
				material = BRIDGE_PILLAR_DEFAULT.get(config);
			}
			material = material.withColor(parseColor(tags.getValue("colour"), CSS_COLORS));

			/* build the result */

			return new BridgeSupportData(pos, shape, material, element.getTags());

		}

		/**
		 * Returns the height-per-length ratio of the bridge's vertical curvature.
		 * 0 if the bridge has no vertical curvature (may still have an incline if the ends are at different ele).
		 * Negative values can be used for suspension-type bridges which are lower in the center than at the ends.
		 */
		private double getCurvatureHeightPerLength() {
			if (element.getTags().contains("bridge:structure", "humpback")) {
				return 0.2;
			} else if (element.getTags().contains("bridge:structure", "simple-suspension")) {
				return -0.25;
			} else {
				return 0;
			}
		}

		public double getBridgeEleAt(VectorXZ pos) {

			if (capConnectors == null) throw new IllegalStateException("connectors not initialized");

			double ele;

			if (centerline != null) {

				double startEle = maxEle(capConnectors.get(caps.get(0)));
				double endEle = maxEle(capConnectors.get(caps.get(1)));
				double offset = centerline.offsetOf(centerline.closestPoint(pos)) / centerline.getLength();
				ele = offset * endEle + (1 - offset) * startEle;

				if (getCurvatureHeightPerLength() != 0) {
					double extraHeight = centerline.getLength() * getCurvatureHeightPerLength();
					ele += circularArcHeightAt(extraHeight, offset, 0.6);
				}

			} else {
				// complex bridge, assume it's totally flat
				ele = maxEle(capConnectors.values()) + 0.1;
			}

			return ele + 0.1;

		}

		public double getBridgeWidthAt(VectorXZ pos) {
			initializeBridgeGeometry();
			return edges.stream().mapToDouble(edge -> edge.distanceTo(pos)).min().orElse(0) * 2;
		}

		private static double maxEle(Collection<EleConnector> connectors) {
			return connectors.stream().mapToDouble(c -> c.getPosXYZ().y).max().orElse(0);
		}

		/**
		 * Calculates the height of a bridge's arc at a given relative position along its length.
		 *
		 * @param height   height difference between the top of the arc and the ends of the arc
		 * @param offset   between 0 (inclusive, beginning of the arc) and 1 (inclusive, end of the arc)
		 * @param section  between 0 (exclusive) and 1 (inclusive). 1 produces a full half circle.
		 */
		private static double circularArcHeightAt(double height, double offset, double section) {
			double dist = abs(offset - 0.5) * section;
			return height * (sqrt(0.25 - dist * dist) - sqrt(0.25 - (0.5 * section) * (0.5 * section)));
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
	protected record BridgeSupportData(VectorXZ pos, SimplePolygonShapeXZ shape, Material material, TagSet tags) {

		private void renderTo(Target target, double baseEle, Function<VectorXZ, Double> bridgeEleAt) {

			double finalBaseEle = baseEle - 2.0; // sink into ground a bit

			var outlineXZ = new PolylineXZ(shape.shift(pos).makeCounterclockwise().vertices());
			PolylineXYZ lowerOutline = outlineXZ.xyz(finalBaseEle);
			PolylineXYZ upperOutline = outlineXZ.xyz(p -> bridgeEleAt.apply(p) - 0.9 * BRIDGE_UNDERSIDE_HEIGHT);

			if (upperOutline.getVertices().stream().allMatch(v -> v.y >= finalBaseEle)) {

				List<VectorXYZ> stripVs = createTriangleStripBetween(upperOutline.getVertices(), lowerOutline.getVertices());
				target.drawTriangleStrip(material, stripVs, TexCoordUtil.texCoordLists(stripVs, material, STRIP_WALL));

				var triangles = triangulateXYZ(new PolygonXYZ(upperOutline.getVertices()));
				target.drawTriangles(material, triangles, TexCoordUtil.triangleTexCoordLists(triangles, material, GLOBAL_X_Z));

			}

		}

	}

}
