package org.osm2world.world.modules;

import static java.lang.Math.*;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Collections.*;
import static java.util.Comparator.comparingDouble;
import static java.util.Objects.requireNonNullElse;
import static org.osm2world.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.math.algorithms.GeometryUtil.*;
import static org.osm2world.math.algorithms.TriangulationUtil.triangulateXYZ;
import static org.osm2world.math.shapes.SimplePolygonXZ.asSimplePolygon;
import static org.osm2world.scene.color.ColorNameDefinitions.CSS_COLORS;
import static org.osm2world.scene.material.DefaultMaterials.*;
import static org.osm2world.scene.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.scene.texcoord.NamedTexCoordFunction.STRIP_WALL;
import static org.osm2world.util.ListUtil.getFirst;
import static org.osm2world.util.ListUtil.getLast;
import static org.osm2world.util.ValueParseUtil.*;
import static org.osm2world.world.data.ProceduralWorldObject.Target;
import static org.osm2world.world.modules.common.WorldModuleGeometryUtil.createTriangleStripBetween;
import static org.osm2world.world.modules.common.WorldModuleGeometryUtil.filterWorldObjectCollisions;
import static org.osm2world.world.modules.common.WorldModuleParseUtil.parseHeight;
import static org.osm2world.world.network.NetworkUtil.getConnectedNetworkSegments;

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
import org.osm2world.math.Angle;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.FaceDecompositionUtil;
import org.osm2world.math.algorithms.TriangulationUtil;
import org.osm2world.math.shapes.*;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.material.Material.Interpolation;
import org.osm2world.scene.material.MaterialOrRef;
import org.osm2world.scene.mesh.TriangleGeometry;
import org.osm2world.scene.texcoord.GlobalXZTexCoordFunction;
import org.osm2world.scene.texcoord.TexCoordUtil;
import org.osm2world.util.exception.InvalidGeometryException;
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
import org.osm2world.world.network.NetworkWaySegmentWorldObject;

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

		/* find bridge way segments, excluding those which are covered by a man_made=bridge area */

		Set<MapWaySegment> bridgeSegments = new LinkedHashSet<>();

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
					bridgeSegments.add(waySegment);
				}

			}
		}

		/* combine chains of connected segments into a single bridge each */

		Set<MapWaySegment> handledSegments = new HashSet<>();

		for (MapWaySegment waySegment : bridgeSegments) {

			if (!handledSegments.add(waySegment)) continue;

			List<MapWaySegment> segments = new ArrayList<>(List.of(waySegment));
			List<MapNode> nodes = new ArrayList<>(waySegment.getStartEndNodes());

			// extend the chain in both directions, keeping the direction of the initial segment
			extendChain(segments, nodes, bridgeSegments, handledSegments);
			reverse(segments);
			reverse(nodes);
			extendChain(segments, nodes, bridgeSegments, handledSegments);
			reverse(segments);
			reverse(nodes);

			BridgeWay bridge = new BridgeWay(segments, nodes);
			segments.forEach(s -> s.addRepresentation(bridge));

		}

		for (MapArea area : mapData.getMapAreas()) {
			if (area.getTags().contains("man_made", "bridge")) {
				area.addRepresentation(new BridgeArea(area));
			}
		}

	}

	/**
	 * extends a chain of bridge segments beyond its last node for as long as there is exactly one
	 * suitable continuation
	 *
	 * @param segments  the segments of the chain, will be modified
	 * @param nodes  the nodes of the chain, will be modified
	 * @param bridgeSegments  all segments which may become part of a chain
	 * @param handledSegments  segments which are already part of a chain, will be modified
	 */
	private static void extendChain(List<MapWaySegment> segments, List<MapNode> nodes,
			Set<MapWaySegment> bridgeSegments, Set<MapWaySegment> handledSegments) {

		while (true) {

			MapWaySegment lastSegment = getLast(segments);
			MapNode lastNode = getLast(nodes);

			/* the chain ends at nodes where other network segments branch off */

			List<NetworkWaySegmentWorldObject> connectedSegments =
					getConnectedNetworkSegments(lastNode, NetworkWaySegmentWorldObject.class, null);

			if (connectedSegments.size() != 2
					|| connectedSegments.get(0).getClass() != connectedSegments.get(1).getClass()) {
				return;
			}

			MapWaySegment nextSegment = connectedSegments.stream()
					.map(NetworkWaySegmentWorldObject::getPrimaryMapElement)
					.filter(s -> s != lastSegment)
					.findAny().orElse(null);

			if (nextSegment == null
					|| !bridgeSegments.contains(nextSegment)
					|| !isSameBridge(lastSegment, nextSegment)
					|| !handledSegments.add(nextSegment)) {
				return;
			}

			segments.add(nextSegment);
			nodes.add(nextSegment.getOtherNode(lastNode));

		}

	}

	/**
	 * checks whether two connected bridge segments should be represented by the same {@link Bridge}
	 */
	static boolean isSameBridge(MapWaySegment s1, MapWaySegment s2) {

		if (s1.getWay() == s2.getWay()) return true;

		TagSet tags1 = s1.getTags();
		TagSet tags2 = s2.getTags();

		return Objects.equals(tags1.getValue("bridge"), tags2.getValue("bridge"))
				&& Objects.equals(tags1.getValue("bridge:structure"), tags2.getValue("bridge:structure"))
				&& parseInt(tags1.getValue("layer"), 0) == parseInt(tags2.getValue("layer"), 0)
				&& getBridgeRelations(s1.getWay()).equals(getBridgeRelations(s2.getWay()));

	}

	private static Set<MapRelation> getBridgeRelations(MapWay way) {
		return way.getMemberships().stream()
				.map(MapRelation.Membership::getRelation)
				.filter(r -> r.getTags().contains("type", "bridge"))
				.collect(Collectors.toSet());
	}

	public static final double BRIDGE_UNDERSIDE_HEIGHT = 0.2f;

	/** sag of a suspension bridge's main cable in the longest span, relative to that span's length */
	private static final double SUSPENSION_SAG_RATIO = 0.1;
	/** minimum vertical distance between a suspension bridge's main cable and the deck at mid-span */
	private static final double SUSPENSION_CABLE_CLEARANCE = 1.0;
	/** approximate distance between a suspension bridge's hangers */
	private static final double SUSPENSION_HANGER_SPACING = 10.0;

	public abstract class Bridge<E extends MapElement> implements ProceduralWorldObject {

		protected final E element;
		protected final @Nullable MapRelation relation;
		protected final int layer;

		protected final BridgeDefaults defaults;

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

			this.defaults = BridgeDefaults.forTags(element.getTags());

			/* find the relation associated with this bridge (if any) */

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

			Material material = config.mapStyle().resolveMaterial(
					element.getTags().getValue("material"), requireNonNullElse(defaults.material, BRIDGE_DEFAULT));

			Angle textureAngle = centerline == null ? null :
					Angle.ofRadians(centerline.getSegments().get(0).getDirection().angle());

			/* draw deck */

			if (element instanceof MapArea) { // bridges based on network segments should be completely covered

				List<TriangleXYZ> trianglesXYZ = getDeckTriangles(true);

				if (!trianglesXYZ.isEmpty()) {

					target.drawTriangles(material, trianglesXYZ,
							TexCoordUtil.triangleTexCoordLists(trianglesXYZ, material,
									td -> new GlobalXZTexCoordFunction(td, textureAngle)));

				}

			}

			{ /* draw underside and edges */

				List<TriangleXYZ> undersideTrianglesXYZ = getDeckTriangles(false).stream()
						.map(t -> t.shift(new VectorXYZ(0, -BRIDGE_UNDERSIDE_HEIGHT, 0)).reverse())
						.toList();

				target.drawTriangles(material, undersideTrianglesXYZ,
						TexCoordUtil.triangleTexCoordLists(undersideTrianglesXYZ, material,
								td -> new GlobalXZTexCoordFunction(td, textureAngle)));

				// don't use the original edges, those don't have the extra intersection points for curved bridges
				Set<LineSegmentXYZ> triangulationEdges = new TriangleGeometry(
						undersideTrianglesXYZ,
						Interpolation.FLAT, List.of(), null)
						.outerEdges();

				VectorXZ outlineCenter = polygon.getOuter().getCentroid();

				for (LineSegmentXYZ edge : triangulationEdges) {

					boolean bridgeIsRight = isRightOf(outlineCenter, edge.p1.xz(), edge.p2.xz());

					List<VectorXYZ> triangleStrip = createTriangleStripBetween(
							edge.shift(new VectorXYZ(0, bridgeIsRight ? 0 : BRIDGE_UNDERSIDE_HEIGHT, 0)).vertices(),
							edge.shift(new VectorXYZ(0, bridgeIsRight ? BRIDGE_UNDERSIDE_HEIGHT : 0, 0)).vertices());
					target.drawTriangleStrip(material, triangleStrip,
							TexCoordUtil.texCoordLists(triangleStrip, material, STRIP_WALL));

				}

			}

			/* draw the supports */

			for (BridgeSupportData support : supports) {
				support.renderTo(target, supportConnectors.get(support).getPosXYZ().y, this);
			}

			/* draw arches between supports */

			if (defaults.hasArches) {
				renderArches(target, material, textureAngle);
			}

			/* draw cables */

			if (defaults.cables != null) {
				renderCables(target);
			}

		}

		/**
		 * renders an arch between each pair of successive supports
		 */
		private void renderArches(Target target, Material material, Angle textureAngle) {

			if (centerline == null || edges.size() != 2) {
				return;
			}

			/* collect the necessary data about support */

			List<VectorXZ> supportPositions = new ArrayList<>(supports.size());
			List<Double> supportBaseElevations = new ArrayList<>(supports.size());
			List<Double> supportWidths = new ArrayList<>(supports.size());

			for (BridgeSupportData support : supports) {
				supportPositions.add(support.pos);
				supportBaseElevations.add(supportConnectors.get(support).getPosXYZ().y);
				// TODO: improve angle calculation for curved centerlines
				double centerlineAngle = getLast(centerline.vertices()).subtract(centerline.vertices().get(0)).angle();
				supportWidths.add(support.getWidth(Angle.ofRadians(centerlineAngle)));
			}

			for (int support = 0; support + 1 < supportPositions.size(); support++) {

				/* render arch between this support and the next */

				VectorXZ supportAPos = supportPositions.get(support);
				VectorXZ supportBPos = supportPositions.get(support + 1);
				double supportAEle = supportBaseElevations.get(support);
				double supportBEle = supportBaseElevations.get(support + 1);
				double supportAHeight = getBridgeEleAt(supportAPos) - supportAEle;
				double supportBHeight = getBridgeEleAt(supportBPos) - supportBEle;
				double supportAWidth = supportWidths.get(support);
				double supportBWidth = supportWidths.get(support + 1);

				int archPoints = 11;

				List<VectorXZ> leftArchEdge = new ArrayList<>(archPoints);
				List<VectorXZ> rightArchEdge = new ArrayList<>(archPoints);

				double relativeStartWidth = (0.95 * min(1, supportAWidth / getBridgeWidthAt(supportAPos)));
				double relativeEndWidth = (0.95 * min(1, supportBWidth / getBridgeWidthAt(supportBPos)));

				double startOffset = centerline.offsetOfClosestPoint(supportAPos);
				double endOffset = centerline.offsetOfClosestPoint(supportBPos);
				double offsetStep = (endOffset - startOffset) / (archPoints - 1);

				for (int i = 0; i < archPoints; i++) {
					VectorXZ center = centerline.pointAtOffset(startOffset + i * offsetStep);
					double relativeWidth = interpolateValue(i / (archPoints - 1.0), relativeStartWidth, relativeEndWidth);
					leftArchEdge.add(center.add(edges.get(0).closestPoint(center).subtract(center).mult(relativeWidth)));
					rightArchEdge.add(center.add(edges.get(1).closestPoint(center).subtract(center).mult(relativeWidth)));
				}

				PolylineXZ leftXZ = new PolylineXZ(leftArchEdge);
				PolylineXZ rightXZ = new PolylineXZ(rightArchEdge);

				PolylineXYZ leftTop = leftXZ.xyz(p -> getBridgeEleAt(p) - BRIDGE_UNDERSIDE_HEIGHT);
				PolylineXYZ rightTop = rightXZ.xyz(p -> getBridgeEleAt(p) - BRIDGE_UNDERSIDE_HEIGHT);

				double spanLength = supportAPos.distanceTo(supportBPos);
				double bridgeEleAtArcPeak = getBridgeEleAt(supportAPos.add(supportBPos).mult(0.5));
				double extraHeight = bridgeEleAtArcPeak - min(supportAEle + supportAHeight, supportBEle + supportBHeight);

				Function<PolylineXZ, PolylineXYZ> calculateBottom = lineXZ -> {
					var firstPassResult = lineXZ.xyz(p -> {
						double offset = min(lineXZ.offsetOf(p) / lineXZ.getLength(), 1);
						double archHeight = min(min(supportAHeight, supportBHeight) + extraHeight, spanLength / 2);
						return bridgeEleAtArcPeak - BRIDGE_UNDERSIDE_HEIGHT - archHeight
								+ circularArcHeightAt(archHeight * 0.95, offset, 0.9);
					});
					double maxExcessHeight = firstPassResult.getVertices().stream()
							.mapToDouble(p -> p.y - (getBridgeEleAt(p.xz()) - BRIDGE_UNDERSIDE_HEIGHT))
							.max().orElse(0);
					if (maxExcessHeight <= 0) {
						return firstPassResult;
					} else {
						// scale the arc down (flattening it) to be below the deck
						double arcBottomY = firstPassResult.getVertices().get(0).y;
						double scale = 1 - (maxExcessHeight / (firstPassResult.getVertices().get(archPoints / 2).y - arcBottomY));
						return new PolylineXYZ(firstPassResult.getVertices().stream()
								.map(v -> v.xz().xyz(arcBottomY + (v.y - arcBottomY) * scale))
								.toList());
					}
				};

				PolylineXYZ leftBottom = calculateBottom.apply(leftXZ);
				PolylineXYZ rightBottom = calculateBottom.apply(rightXZ);

				List<TriangleXYZ> undersideTrianglesXYZ = getDeckTriangles(false).stream()
						.map(t -> t.shift(new VectorXYZ(0, -BRIDGE_UNDERSIDE_HEIGHT, 0)).reverse())
						.toList();

				target.drawTriangles(material, undersideTrianglesXYZ,
						TexCoordUtil.triangleTexCoordLists(undersideTrianglesXYZ, material,
								td -> new GlobalXZTexCoordFunction(td, textureAngle)));

				List<List<VectorXYZ>> triangleStrips = List.of(
						createTriangleStripBetween(leftBottom.getVertices(), leftTop.getVertices()),
						createTriangleStripBetween(rightBottom.getVertices(), leftBottom.getVertices()),
						createTriangleStripBetween(rightTop.getVertices(), rightBottom.getVertices())
				);

				for (var triangleStrip : triangleStrips) {
					try {
						target.drawTriangleStrip(material, triangleStrip,
								TexCoordUtil.texCoordLists(triangleStrip, material, STRIP_WALL));
					} catch (InvalidGeometryException e) {
						ConversionLog.warn("Broken geometry in bridge arches", e, element.getElementWithId());
					}
				}

			}

		}

		private void renderCables(Target target) {

			if (centerline == null) return;

			List<VectorXYZ> cableAnchors = new ArrayList<>();
			List<TagSet> cableAnchorTags = new ArrayList<>();

			for (BridgeSupportData support : supports) {
				if (support.type() == BridgeSupportType.PYLON) {
					double baseEle = supportConnectors.get(support).getPosXYZ().y;
					double height = support.getHeight(baseEle, this) - 0.5;
					cableAnchors.add(support.pos.xyz(baseEle + height));
					cableAnchorTags.add(support.tags);
				}
			}

			if (cableAnchors.isEmpty()) return;

			if (defaults.cables == BridgeDefaults.CableType.SUSPENSION) {

				/* build a path for the cables which extends beyond the deck to any anchors there */

				VectorXZ deckStart = getFirst(centerline.vertices());
				VectorXZ deckEnd = getLast(centerline.vertices());
				VectorXZ outwardStartDirection = centerline.getSegments().get(0).getDirection().invert();
				VectorXZ outwardEndDirection = getLast(centerline.getSegments()).getDirection();

				List<VectorXZ> anchorsBeyondStart = cableAnchors.stream()
						.map(VectorXYZ::xz)
						.filter(a -> a.subtract(deckStart).dot(outwardStartDirection) > 1.0)
						.sorted(comparingDouble(a -> -a.distanceTo(deckStart)))
						.toList();
				List<VectorXZ> anchorsBeyondEnd = cableAnchors.stream()
						.map(VectorXYZ::xz)
						.filter(a -> a.subtract(deckEnd).dot(outwardEndDirection) > 1.0)
						.sorted(comparingDouble(a -> a.distanceTo(deckEnd)))
						.toList();

				List<VectorXZ> cablePathVertices = new ArrayList<>(anchorsBeyondStart);
				cablePathVertices.addAll(centerline.vertices());
				cablePathVertices.addAll(anchorsBeyondEnd);
				PolylineXZ cablePath = new PolylineXZ(cablePathVertices);

				double deckStartOffset = anchorsBeyondStart.isEmpty() ? 0
						: new PolylineXZ(cablePathVertices.subList(0, anchorsBeyondStart.size() + 1)).getLength();
				double deckEndOffset = deckStartOffset + centerline.getLength();

				/* sort anchors along the cable path, add anchors at the deck ends if necessary */

				List<VectorXYZ> suspensionAnchors = new ArrayList<>(cableAnchors);
				suspensionAnchors.sort(comparingDouble(cablePath::offsetOfClosestPoint));

				if (anchorsBeyondStart.isEmpty()
						&& cablePath.offsetOfClosestPoint(suspensionAnchors.get(0)) - deckStartOffset > 3) {
					suspensionAnchors.add(0, deckStart.xyz(getBridgeEleAt(deckStart)));
				}
				if (anchorsBeyondEnd.isEmpty()
						&& deckEndOffset - cablePath.offsetOfClosestPoint(getLast(suspensionAnchors)) > 3) {
					suspensionAnchors.add(deckEnd.xyz(getBridgeEleAt(deckEnd)));
				}

				double[] anchorOffsets = suspensionAnchors.stream()
						.mapToDouble(cablePath::offsetOfClosestPoint)
						.toArray();

				double mainSpanLength = 0;
				for (int i = 0; i + 1 < anchorOffsets.length; i++) {
					mainSpanLength = max(mainSpanLength, anchorOffsets[i + 1] - anchorOffsets[i]);
				}

				for (int i = 0; i + 1 < suspensionAnchors.size(); i++) {

					double offsetA = anchorOffsets[i];
					double offsetB = anchorOffsets[i + 1];
					double eleA = suspensionAnchors.get(i).y;
					double eleB = suspensionAnchors.get(i + 1).y;
					double spanLength = offsetB - offsetA;

					if (spanLength <= 0) continue;

					int intervals = max(2, (int) ceil(spanLength / SUSPENSION_HANGER_SPACING));

					double[] sampleOffsets = new double[intervals + 1];
					for (int k = 0; k <= intervals; k++) {
						sampleOffsets[k] = min(offsetA + k * spanLength / intervals, cablePath.getLength());
					}

					/* choose the sag: same curvature in all spans, but keep the cable above the deck */

					double sag = SUSPENSION_SAG_RATIO * spanLength * spanLength / mainSpanLength;

					for (int k = 1; k < intervals; k++) {
						if (sampleOffsets[k] < deckStartOffset || sampleOffsets[k] > deckEndOffset) continue;
						double t = k / (double) intervals;
						double chordEle = eleA + t * (eleB - eleA);
						double deckEle = getBridgeEleAt(cablePath.pointAtOffset(sampleOffsets[k]));
						sag = min(sag, (chordEle - deckEle) / (4 * t * (1 - t)) - SUSPENSION_CABLE_CLEARANCE);
					}

					sag = max(0, sag);

					/* render the main cable and hangers along each edge */

					for (var edge : edges) {

						VectorXZ startLateralOffset = edge.closestPoint(deckStart).subtract(deckStart);
						VectorXZ endLateralOffset = edge.closestPoint(deckEnd).subtract(deckEnd);

						List<VectorXYZ> pointsXYZ = new ArrayList<>(intervals + 1);

						for (int k = 0; k <= intervals; k++) {
							VectorXZ center = cablePath.pointAtOffset(sampleOffsets[k]);
							VectorXZ pointXZ;
							if (sampleOffsets[k] < deckStartOffset) {
								pointXZ = center.add(startLateralOffset);
							} else if (sampleOffsets[k] > deckEndOffset) {
								pointXZ = center.add(endLateralOffset);
							} else {
								pointXZ = edge.closestPoint(center);
							}
							pointsXYZ.add(pointXZ.xyz(parabolicCableHeightAt(eleA, eleB, sag, k / (double) intervals)));
						}

						renderCable(target, pointsXYZ, false);

						for (int k = 1; k < intervals; k++) {
							if (sampleOffsets[k] < deckStartOffset || sampleOffsets[k] > deckEndOffset) continue;
							VectorXYZ point = pointsXYZ.get(k);
							List<VectorXYZ> path = List.of(point, point.xz().xyz(this::getBridgeEleAt));
							if (path.get(0).y - path.get(1).y < 0.1)
								continue;
							renderCable(target, path, true);
						}

					}

				}

			} else if (defaults.cables == BridgeDefaults.CableType.CABLE_STAYED_FAN) {

				double[] anchorOffsets = cableAnchors.stream()
						.mapToDouble(anchor -> centerline.offsetOfClosestPoint(anchor))
						.toArray();

				for (int i = 0; i < cableAnchors.size(); i++) {

					VectorXYZ anchor = cableAnchors.get(i);

					double offsetStart = (i > 0) ? (anchorOffsets[i - 1] + anchorOffsets[i]) / 2 : 0;
					double offsetEnd = (i < cableAnchors.size() - 1)
							? (anchorOffsets[i + 1] + anchorOffsets[i]) / 2
							: centerline.getLength();

					int cables = parseUInt(cableAnchorTags.get(i).getValue("cables"), 4);
					double offsetStep = (offsetEnd - offsetStart) / cables;

					for (int c = 0; c < cables; c++) {
						double connectionOffset = offsetStart + (c + 0.5) * offsetStep;
						VectorXZ connectionCenter = centerline.pointAtOffset(connectionOffset);
						VectorXZ connectionLeft = edges.get(0).closestPoint(connectionCenter);
						VectorXZ connectionRight = edges.get(1).closestPoint(connectionCenter);
						for (VectorXZ connectionXZ : List.of(connectionLeft, connectionRight)) {
							renderCable(target, List.of(anchor, connectionXZ.xyz(this::getBridgeEleAt)), false);
						}
					}

				}

			}

		}

		private void renderCable(Target target, List<VectorXYZ> vs, boolean vertical) {
			var path = new PolylineXYZ(vs);
			double diameter = max(0.1, min(path.length() / 250, 1.0));
			Material material = PLASTIC.get(config.mapStyle());
			List<VectorXYZ> upVectors = vertical ? null : Collections.nCopies(path.size(), VectorXYZ.Z_UNIT);
			CircleXZ shape = new CircleXZ(new VectorXZ(0, 0), diameter / 2);
			target.drawExtrudedShape(material, shape, path.getVertices(), upVectors, null, null);
		}

		/** makes sure the {@link #polygon}, {@link #edges}, {@link #caps} and {@link #supports} fields are populated */
		protected void initializeBridgeGeometry() {

			if (polygon != null) {
				// already calculated
				return;
			}

			initializeOutlineGeometry();

			initializeSupports();

		}

		/** populates the {@link #polygon}, {@link #edges} and {@link #caps} fields, and {@link #centerline} if possible */
		protected abstract void initializeOutlineGeometry();

		/** returns all {@link MapElement}s which are represented by this bridge */
		protected abstract Collection<? extends MapElement> getMapElements();

		/** returns nodes which are part of this bridge's {@link #getMapElements()} and may be explicitly mapped supports */
		protected abstract Collection<MapNode> getSupportCandidateNodes();

		/** returns all elements overlapping any of this bridge's {@link #getMapElements()}, without duplicates */
		protected Collection<MapElement> getOverlappingElements() {
			Collection<? extends MapElement> ownElements = getMapElements();
			Set<MapElement> result = new LinkedHashSet<>();
			for (MapElement e : ownElements) {
				for (MapOverlap<?, ?> overlap : e.getOverlaps()) {
					MapElement other = overlap.getOther(e);
					if (!ownElements.contains(other)) {
						result.add(other);
					}
				}
			}
			return result;
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

			cuts.sort(comparingDouble((Cut c) -> edge0.offsetOf(c.p0))
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

			if (centerline != null && defaults.curvatureHeightPerLength != 0) {

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

			/* look for explicitly mapped supports among way nodes, overlapping features and relation members */

			Set<MapElement> supportCandidates = new LinkedHashSet<>(getSupportCandidateNodes());

			supportCandidates.addAll(getOverlappingElements());

			if (relation != null) {
				relation.getMembers().stream()
						.filter(m -> "support".equals(m.getRole()) || m.getRole().isBlank())
						.filter(m -> m.getElement() instanceof MapElement)
						.forEach(m -> supportCandidates.add((MapElement)m.getElement()));
			}

			List<MapElement> explicitlyMappedSupports = supportCandidates.stream()
					.filter(it -> it instanceof MapNode || it instanceof MapArea)
					.filter(it -> BridgeSupportType.forTags(it.getTags()) != null)
					.toList();

			if (!explicitlyMappedSupports.isEmpty()) {

				/* create the supports */

				for (MapElement element : explicitlyMappedSupports) {

					VectorXZ pos = (element instanceof MapArea area)
							? area.getOuterPolygon().getCenter()
							: ((MapNode)element).getPos();

					SimplePolygonShapeXZ shape = (element instanceof MapArea area)
							? asSimplePolygon(area.getOuterPolygon().shift(pos.invert())).makeCounterclockwise()
							: null;

					supports.add(createSupport(pos, shape, element.getTags()));

				}

				if (centerline != null) {
					// sort along the centerline
					supports.sort(comparingDouble(it -> centerline.offsetOfClosestPoint(it.pos)));
				}

			} else if (centerline != null && defaults.supportType != null && Double.isFinite(defaults.pierDistance)) {

				/* no explicitly mapped supports found, distribute some equally along the bridge's length */

				double distance = defaults.supportType == BridgeSupportType.PYLON
						? centerline.getLength() / 1.9
						: min(defaults.pierDistance, centerline.getLength());

				List<VectorXZ> supportPositions = new ArrayList<>(equallyDistributePointsAlong(
						distance, false, centerline));

				/* make sure that the piers don't pierce anything on the ground */

				Collection<WorldObject> avoidedObjects = new ArrayList<>();

				for (MapElement overlappingElement : getOverlappingElements()) {
					for (WorldObject otherRep : overlappingElement.getRepresentations()) {

						if (otherRep.getGroundState() == GroundState.ON
								&& !(otherRep instanceof Water
								|| otherRep instanceof Waterway
								|| otherRep instanceof SurfaceArea) //TODO: choose better criteria!
						) {
							avoidedObjects.add(otherRep);
						}

					}
				}

				filterWorldObjectCollisions(supportPositions, avoidedObjects);

				/* create the piers */

				for (VectorXZ pos : supportPositions) {
					supports.add(createSupport(pos, null, TagSet.of("bridge:support",
							defaults.supportType.toString().toLowerCase(Locale.ROOT))));
				}

			}

		}

		private BridgeSupportData createSupport(VectorXZ pos, @Nullable SimplePolygonShapeXZ shape, TagSet tags) {

			/* build shape */

			if (shape == null) {
				if (centerline != null && !(BridgeSupportType.forTags(tags) == BridgeSupportType.PYLON
						&& defaults.cables != BridgeDefaults.CableType.SUSPENSION)) {
					double width = parseMeasure(tags.getValue("width"),
							(defaults.cables == BridgeDefaults.CableType.SUSPENSION ? 1.2 : 0.7) * getBridgeWidthAt(pos));
					double length = parseMeasure(tags.getValue("length"), min(width / 2, defaults.pierDistance / 4));
					double angle = centerline.closestSegment(pos).getDirection().angle();
					shape = new AxisAlignedRectangleXZ(NULL_VECTOR, width, length);
					shape = shape.rotatedCW(angle);
				} else {
					double diameter = parseMeasure(tags.getValue("width"), min(4.0, getBridgeWidthAt(pos) * 0.7));
					shape = asSimplePolygon(new CircleXZ(NULL_VECTOR, diameter / 2));
				}
			}

			/* build material */

			Material material = config.mapStyle().resolveMaterial(tags.getValue("material"),
					config.mapStyle().resolveMaterial(element.getTags().getValue("material"),
					Objects.requireNonNullElse(defaults.material, BRIDGE_PILLAR_DEFAULT)));

			material = material.withColor(parseColor(tags.getValue("colour"), CSS_COLORS));

			/* build the result */

			return new BridgeSupportData(pos, shape, material, tags);

		}

		public double getBridgeEleAt(VectorXZ pos) {

			if (capConnectors == null) throw new IllegalStateException("connectors not initialized");

			double ele;

			if (centerline != null) {

				double startEle = maxEle(capConnectors.get(caps.get(0)));
				double endEle = maxEle(capConnectors.get(caps.get(1)));
				double offset = centerline.offsetOfClosestPoint(pos) / centerline.getLength();
				ele = interpolateValue(offset, startEle, endEle);

				if (defaults.curvatureHeightPerLength != 0) {
					double extraHeight = centerline.getLength() * defaults.curvatureHeightPerLength;
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

		private double maxSpanLength() {

			if (centerline != null) {

				List<Double> offsets = new ArrayList<>();
				supports.forEach(s -> offsets.add(centerline.offsetOfClosestPoint(s.pos)));
				offsets.add(0, 0.0);
				offsets.add(centerline.getLength());

				double max = 0;
				for (int i = 0; i + 1 < offsets.size(); i++) {
					max = Math.max(max, offsets.get(i + 1) - offsets.get(i));
				}
				return max;

			} else {
				return polygon.getDiameter();
			}
		}

		/**
		 * Calculates the height of a bridge's arc at a given relative position along its length.
		 *
		 * @param height   height difference between the top of the arc and the ends of the arc
		 * @param offset   between 0 (inclusive, beginning of the arc) and 1 (inclusive, end of the arc)
		 * @param section  between 0 (exclusive) and 1 (inclusive). 1 produces a full half circle.
		 */
		static double circularArcHeightAt(double height, double offset, double section) {
			double dist = abs(offset - 0.5) * section;
			double endHeight = sqrt(0.25 - (0.5 * section) * (0.5 * section));
			return height * (sqrt(0.25 - dist * dist) - endHeight) / (0.5 - endHeight);
		}

		/**
		 * Calculates the height of a suspension bridge's main cable, approximated as a parabola.
		 *
		 * @param eleA    elevation of the anchor at the start of the cable
		 * @param eleB    elevation of the anchor at the end of the cable
		 * @param sag     vertical distance between the cable and the straight line between the anchors at mid-span
		 * @param offset  between 0 (inclusive, anchor A) and 1 (inclusive, anchor B)
		 */
		static double parabolicCableHeightAt(double eleA, double eleB, double sag, double offset) {
			return eleA + offset * (eleB - eleA) - 4 * sag * offset * (1 - offset);
		}

	}

	private class BridgeArea extends Bridge<MapArea> implements AreaWorldObject {

		public BridgeArea(MapArea area) {
			super(area);
		}

		@Override
		protected Collection<MapArea> getMapElements() {
			return List.of(element);
		}

		@Override
		protected Collection<MapNode> getSupportCandidateNodes() {
			return List.of();
		}

		@Override
		protected void initializeOutlineGeometry() {

			/* get edge members from the bridge relation associated with this bridge (if any) */

			Set<MapWaySegment> edgeMemberSegments = Set.of();

			if (relation != null) {
				edgeMemberSegments = relation.getMembers().stream()
						.filter(m -> "edge".equals(m.getRole()))
						.filter(m -> m.getElement() instanceof MapWay)
						.map(m -> (MapWay) m.getElement())
						.flatMap(m -> m.getWaySegments().stream())
						.collect(Collectors.toSet());
			}

			MapArea area = element;

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

				// make sure that the left edge is at index 0.
				// The first edge follows the direction of the outline, so the other edge is on its right
				// if the bridge area is on the right of the outline.
				if (!area.getAreaSegmentsOuter().get(0).isAreaRight()) {
					swap(edges, 0, 1);
				}

				VectorXZ start = getFirst(edges.get(0).vertices()).add(getFirst(edges.get(1).vertices())).mult(0.5);

				if (caps.get(0).distanceTo(start) > caps.get(1).distanceTo(start)) {
					// swap the caps so that the "start" cap is at index 0
					swap(caps, 0, 1);
				}

				this.centerline = calculateCenterlineBetween(edges.get(0), edges.get(1));

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

	}

	/**
	 * A bridge carrying one or more connected, non-branching {@link MapWaySegment}s
	 * which belong to the same bridge according to {@link #isSameBridge(MapWaySegment, MapWaySegment)}.
	 */
	private class BridgeWay extends Bridge<MapWaySegment> implements WaySegmentWorldObject {

		/** the segments of this bridge, ordered from the start to the end of the bridge */
		private final List<MapWaySegment> segments;

		/**
		 * the nodes connecting the {@link #segments}, including the start and end node of the bridge.
		 * Segment <code>i</code> runs from node <code>i</code> to node <code>i+1</code>,
		 * which may be opposite to the segment's own direction.
		 */
		private final List<MapNode> nodes;

		public BridgeWay(List<MapWaySegment> segments, List<MapNode> nodes) {
			super(segments.get(0));
			if (nodes.size() != segments.size() + 1) throw new IllegalArgumentException("inconsistent chain");
			this.segments = List.copyOf(segments);
			this.nodes = List.copyOf(nodes);
		}

		@Override
		protected Collection<MapWaySegment> getMapElements() {
			return segments;
		}

		@Override
		protected Collection<MapNode> getSupportCandidateNodes() {
			return nodes;
		}

		@Override
		protected void initializeOutlineGeometry() {

			/* join the outlines and centerlines of all segments */

			List<VectorXZ> leftOutline = new ArrayList<>();
			List<VectorXZ> rightOutline = new ArrayList<>();
			List<VectorXZ> centerline = new ArrayList<>();

			for (int i = 0; i < segments.size(); i++) {

				MapWaySegment segment = segments.get(i);
				boolean reversed = segment.getStartNode() != nodes.get(i);

				AbstractNetworkWaySegmentWorldObject primaryRep =
						(AbstractNetworkWaySegmentWorldObject) segment.getPrimaryRepresentation();

				List<VectorXZ> segmentCenterline = primaryRep.getCenterlineXZ().vertices();

				if (!reversed) {
					appendToLine(leftOutline, primaryRep.getOutlineXZ(false));
					appendToLine(rightOutline, primaryRep.getOutlineXZ(true));
					appendToLine(centerline, segmentCenterline);
				} else {
					appendToLine(leftOutline, Lists.reverse(primaryRep.getOutlineXZ(true)));
					appendToLine(rightOutline, Lists.reverse(primaryRep.getOutlineXZ(false)));
					appendToLine(centerline, Lists.reverse(segmentCenterline));
				}

			}

			/* build the bridge geometry from the joined lines */

			this.edges = List.of(new PolylineXZ(leftOutline), new PolylineXZ(rightOutline));
			this.caps = List.of(
					new PolylineXZ(getFirst(leftOutline), getFirst(rightOutline)),
					new PolylineXZ(getLast(leftOutline), getLast(rightOutline)));

			List<VectorXZ> outline = new ArrayList<>(leftOutline);
			outline.addAll(Lists.reverse(rightOutline));
			this.polygon = new SimplePolygonXZ(closeLoop(outline));

			this.centerline = new PolylineXZ(centerline);

		}

		/**
		 * appends vertices to a line, skipping the first vertex if it is (almost) identical
		 * to the current last vertex of the line
		 */
		private static void appendToLine(List<VectorXZ> line, List<VectorXZ> vertices) {
			if (!line.isEmpty() && getLast(line).distanceTo(vertices.get(0)) < 0.01) {
				vertices = vertices.subList(1, vertices.size());
			}
			line.addAll(vertices);
		}

		@Override
		public VectorXZ getStartPosition() {
			MapWaySegment firstSegment = segments.get(0);
			WaySegmentWorldObject primaryRep = firstSegment.getPrimaryRepresentation();
			boolean reversed = firstSegment.getStartNode() != nodes.get(0);
			if (primaryRep != null && primaryRep != this) {
				return reversed ? primaryRep.getEndPosition() : primaryRep.getStartPosition();
			} else {
				return nodes.get(0).getPos();
			}
		}

		@Override
		public VectorXZ getEndPosition() {
			MapWaySegment lastSegment = getLast(segments);
			WaySegmentWorldObject primaryRep = lastSegment.getPrimaryRepresentation();
			boolean reversed = lastSegment.getStartNode() != nodes.get(nodes.size() - 2);
			if (primaryRep != null && primaryRep != this) {
				return reversed ? primaryRep.getStartPosition() : primaryRep.getEndPosition();
			} else {
				return getLast(nodes).getPos();
			}
		}

	}

	/**
	 * Default values for various bridge dimensions and properties.
	 *
	 * @param material  default material for if different from the overall defaults for bridges
	 * @param pierDistance  distance between the centers of piers (if not explicitly mapped)
	 * @param curvatureHeightPerLength  height-per-length ratio of the bridge's vertical curvature.
	 * 0 if the bridge has no vertical curvature (may still have an incline if the ends are at different ele).
	 * Negative values can be used for suspension-type bridges which are lower in the center than at the ends.
	 */
	protected record BridgeDefaults (
			@Nullable MaterialOrRef material,
			@Nullable CableType cables,
			@Nullable BridgeSupportType supportType,
			boolean hasArches,
			double pierDistance,
			double curvatureHeightPerLength
		) {

		public enum CableType { SUSPENSION, CABLE_STAYED_FAN }

		public static BridgeDefaults forTags(TagSet tags) {

			String type = requireNonNullElse(tags.getValue("bridge"), "");
			String structure = requireNonNullElse(tags.getValue("bridge:structure"), "beam");

			@Nullable MaterialOrRef material = switch (structure) {
				case "clapper" -> ROCK;
				case "simple-suspension" -> WOOD;
				default -> ("boardwalk".equals(type)) ? WOOD : null;
			};

			@Nullable CableType cables = switch (structure) {
				case "suspension" -> CableType.SUSPENSION;
				case "cable-stayed" -> CableType.CABLE_STAYED_FAN;
				default -> null;
			};

			@Nullable BridgeSupportType supportType = switch (structure) {
				case "suspension", "cable-stayed" -> BridgeSupportType.PYLON;
				case "simple-suspension", "floating" -> null;
				default -> BridgeSupportType.PIER;
			};

			boolean hasArches = List.of("arch", "humpback").contains(structure);

			double pierDistance = switch (structure) {
				case "simple-suspension", "floating" -> Double.POSITIVE_INFINITY;
				case "clapper" -> 3.0;
				default -> "viaduct".equals(type) ? 20.0 : 50.0;
			};

			double curvatureHeightPerLength = switch (structure) {
				case "humpback" -> 0.2;
				case "simple-suspension" -> -0.25;
				default -> 0;
			};

			return new BridgeDefaults(material, cables, supportType, hasArches, pierDistance, curvatureHeightPerLength);

		}

	}

	/** type of bridge support */
	protected enum BridgeSupportType {

		PIER, ABUTMENT, LIFT_PIER, PIVOT_PIER, PYLON;

		public static @Nullable BridgeSupportType forTags(TagSet tags) {
			return switch (requireNonNullElse(tags.getValue("bridge:support"), "no")) {
				case "abutment" -> ABUTMENT;
				case "lift_pier" -> LIFT_PIER;
				case "pivot_pier" -> PIVOT_PIER;
				case "pylon" -> PYLON;
				case "no" -> null;
				default -> PIER;
			};
		}

	}

	/** Data describing a bridge pier or other support element */
	protected record BridgeSupportData(VectorXZ pos, SimplePolygonShapeXZ shape, Material material, TagSet tags) {

		private void renderTo(Target target, double baseEle, Bridge<?> bridge) {

			double finalBaseEle = baseEle - 2.0; // sink into ground a bit

			var outlineXZ = new PolylineXZ(shape.shift(pos).makeCounterclockwise().vertices());
			PolylineXYZ lowerOutline = outlineXZ.xyz(finalBaseEle);
			PolylineXYZ upperOutline;

			if (type() != BridgeSupportType.PYLON) {
				upperOutline = outlineXZ.xyz(p -> bridge.getBridgeEleAt(p) - 0.9 * BRIDGE_UNDERSIDE_HEIGHT);
			} else {
				double pylonHeight = getHeight(baseEle, bridge);
				if (bridge.defaults.cables() != BridgeDefaults.CableType.SUSPENSION) {
					upperOutline = outlineXZ.xyz(baseEle + pylonHeight);
				} else {
					// two-part suspension bridge pylon
					double splitEle = bridge.getBridgeEleAt(pos) - 1;
					upperOutline = outlineXZ.xyz(splitEle);
					renderSuspensionPylonTopTo(target, splitEle, pylonHeight - (splitEle - baseEle), bridge);
				}
			}

			if (upperOutline.getVertices().stream().allMatch(v -> v.y >= finalBaseEle)) {

				List<VectorXYZ> stripVs = createTriangleStripBetween(upperOutline.getVertices(), lowerOutline.getVertices());
				target.drawTriangleStrip(material, stripVs, TexCoordUtil.texCoordLists(stripVs, material, STRIP_WALL));

				var triangles = triangulateXYZ(new PolygonXYZ(upperOutline.getVertices()));
				target.drawTriangles(material, triangles, TexCoordUtil.triangleTexCoordLists(triangles, material, GLOBAL_X_Z));

			}

		}

		private void renderSuspensionPylonTopTo(Target target, double bottomEle, double topPartHeight,
				Bridge<?> bridge) {

			VectorXZ pA = bridge.getEdges().get(0).closestPoint(pos);
			VectorXZ pB = bridge.getEdges().get(1).closestPoint(pos);

			ShapeXZ shape = new AxisAlignedRectangleXZ(NULL_VECTOR, 0.4, 0.2);

			List<VectorXYZ> path = List.of(
					pA.xyz(bottomEle), pA.xyz(bottomEle + topPartHeight),
					pB.xyz(bottomEle + topPartHeight), pB.xyz(bottomEle));
			List<VectorXYZ> upVectors = nCopies(path.size(), pB.subtract(pA).rightNormal().xyz(0));

			target.drawExtrudedShape(material, shape, path, upVectors, null, null);

		}

		public BridgeSupportType type() {
			return BridgeSupportType.forTags(tags);
		}

		public double getHeight(double baseEle, Bridge<?> bridge) {
			if (type() == BridgeSupportType.PYLON) {
				Double pylonHeight = parseHeight(tags);
				if (pylonHeight == null || baseEle + pylonHeight < bridge.getBridgeEleAt(pos) + 1.0) {
					if (bridge.defaults.cables == BridgeDefaults.CableType.SUSPENSION) {
						pylonHeight = bridge.getBridgeEleAt(pos) + max(5, bridge.maxSpanLength() / 8) - baseEle;
					} else {
						pylonHeight = bridge.getBridgeEleAt(pos) + max(8, bridge.maxSpanLength() / 3) - baseEle;
					}
				}
				return pylonHeight;
			} else {
				return bridge.getBridgeEleAt(pos) - baseEle;
			}
		}

		public double getWidth(Angle centerlineDirection) {

			if (shape instanceof AxisAlignedRectangleXZ rect) {
				return rect.sizeX();
			} else {
				var rotatedShape = shape.rotatedCW(-centerlineDirection.radians);
				return rotatedShape.boundingBox().sizeX();
			}

		}

	}

}
