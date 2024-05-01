package org.osm2world.core.world.modules.building.indoor;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.osm2world.core.target.common.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.core.target.common.texcoord.TexCoordUtil.triangleTexCoordLists;
import static org.osm2world.core.util.ValueParseUtil.parseLevels;

import java.util.*;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.*;
import org.osm2world.core.math.algorithms.TriangulationUtil;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.world.attachment.AttachmentConnector;
import org.osm2world.core.world.data.AbstractAreaWorldObject;
import org.osm2world.core.world.data.LegacyWorldObject;
import org.osm2world.core.world.modules.building.Door;
import org.osm2world.core.world.modules.building.DoorParameters;
import org.osm2world.core.world.modules.building.WallSurface;
import org.osm2world.core.world.modules.common.AbstractModule;

public class IndoorModule extends AbstractModule {

	@Override
	protected void applyToArea(MapArea area) {
		if (area.getTags().contains("highway", "elevator")) {
			area.addRepresentation(new Elevator(area));
		}
	}

	private static class Elevator extends AbstractAreaWorldObject implements LegacyWorldObject {

		private final double carHeight = 2.2;

		// value between 0 and 1
		// determines cars position in shaft
		private double carElevationInShaft = 0.5;
		private final int noLevels;
		private final VectorXZ centroid;


		private final AttachmentConnector topConnector;
		private final AttachmentConnector bottomConnector;

		public Elevator(MapArea area){
			super(area);

			centroid = area.getOuterPolygon().getCentroid();

			if (area.getTags().containsKey("level")) {

				List<Integer> levels = parseLevels(area.getTags().getValue("level"));

				if (levels != null) {
					topConnector = new AttachmentConnector(asList("ceiling" + levels.get(levels.size() - 1).toString()),
							centroid.xyz(0), this, 0, false);

					bottomConnector = new AttachmentConnector(asList("floor" + levels.get(0).toString()),
							centroid.xyz(0), this, 0, false);

					noLevels = levels.get(levels.size() - 1) - levels.get(0) + 1;
				} else {
					topConnector = null;
					bottomConnector = null;

					noLevels = 3;
				}

			} else {
				topConnector = null;
				bottomConnector = null;

				noLevels = 3;
			}

		}

		@Override
		public GroundState getGroundState() { return null; }

		@Override
		public Iterable<AttachmentConnector> getAttachmentConnectors() {
			if (topConnector == null) {
				return emptyList();
			} else {
				return asList(topConnector, bottomConnector);
			}
		}

		@Override
		public void renderTo(Target target) {

			List<LineSegmentXZ> outerLineSegments = area.getOuterPolygon().makeClockwise().getSegments();
			List<MapNode> doorNodes = area.getBoundaryNodes().stream().filter(n -> n.getTags().containsKey("door")).collect(toList());
			Map<Integer, MapNode> lineSegIndexToDoorNode = new HashMap<>();

			/* determine car polygon */

			List<LineSegmentXZ> innerLineSegments = new ArrayList<>();

			for (LineSegmentXZ lineSegment : outerLineSegments) {

				VectorXZ rightNormal = lineSegment.getDirection().rightNormal().mult(0.3);

				innerLineSegments.add(new LineSegmentXZ(lineSegment.p1.add(rightNormal), lineSegment.p2.add(rightNormal)));

			}

			List<VectorXZ> carOuterPoints = new ArrayList<>();

			int noSegments = -1;
			for (int i = 0; i < innerLineSegments.size(); i++) {

				LineSegmentXZ first = innerLineSegments.get(i);
				LineSegmentXZ second = innerLineSegments.get((i + 1) % innerLineSegments.size());

				VectorXZ p2Intersection = GeometryUtil.getLineSegmentIntersection(first.p1, first.p2, second.p1, second.p2);

				// effectively straightens out wall surface
				if (p2Intersection != null && first.getDirection().dot(second.getDirection()) < 0.05) {
					carOuterPoints.add(p2Intersection);
					noSegments += 1;
				} else {
					final int j = i;
					if (doorNodes.stream().anyMatch(n -> n.getPos().equals(outerLineSegments.get(j).p2))) {
						// one door per segment
						lineSegIndexToDoorNode.put(noSegments, doorNodes.stream().filter(n -> n.getPos().equals(outerLineSegments.get(j).p2)).collect(toList()).get(0));
					}
				}

			}

			carOuterPoints.add(carOuterPoints.get(0));

			if (lineSegIndexToDoorNode.get(-1) != null) {
				lineSegIndexToDoorNode.put(noSegments, lineSegIndexToDoorNode.get(-1));
			}


			/* render elevator */

			if (carOuterPoints.size() > 3) {

				/* determine top and bottom of shaft */

				final double shaftBaseEle;
				Double tempBaseEle = null;

				if (bottomConnector != null) {
					if (bottomConnector.isAttached()) {
						tempBaseEle = bottomConnector.getAttachedPos().getY() + 0.001;
					}
				}
				if (tempBaseEle == null) {
					tempBaseEle = 0.001;
				}

				shaftBaseEle = tempBaseEle;


				final double cableTopEle;
				Double tempTopEle = null;

				if (topConnector != null) {
					if (topConnector.isAttached()) {
						tempTopEle = topConnector.getAttachedPos().getY();
					}
				}
				if (tempTopEle == null){
					// estimate of top based on the number of levels
					tempTopEle = shaftBaseEle + noLevels * 2.5;
				}

				cableTopEle = tempTopEle;


				final double carBaseEle = shaftBaseEle + (cableTopEle - shaftBaseEle - carHeight - 0.25) * carElevationInShaft;

				/* draw car walls */

				for (int i = 0; i < carOuterPoints.size(); i++) {

					List<VectorXYZ> bottomPoints = new ArrayList<>(asList(carOuterPoints.get((i + 1) % carOuterPoints.size()), carOuterPoints.get(i)))
							.stream().map(v -> v.xyz(carBaseEle)).collect(toList());

					List<VectorXYZ> topPoints = new ArrayList<>(bottomPoints).stream().map(v -> v.addY(carHeight)).collect(toList());

					WallSurface frontSurface = new WallSurface(Materials.STEEL, bottomPoints, topPoints);

					List<VectorXYZ> bottomPointsReverse = new ArrayList<>(bottomPoints);
					List<VectorXYZ> topPointsReverse = new ArrayList<>(topPoints);

					Collections.reverse(bottomPointsReverse);
					Collections.reverse(topPointsReverse);

					WallSurface backSurface = new WallSurface(Materials.STEEL, bottomPointsReverse, topPointsReverse);

					LineSegmentXZ carSegment = new LineSegmentXZ(carOuterPoints.get((i + 1) % carOuterPoints.size()), carOuterPoints.get(i));
					MapNode doorNode = lineSegIndexToDoorNode.get(i);

					if (doorNode != null) {
						if (doorNode.getTags().containsKey("door")) {

							DoorParameters params = DoorParameters.fromTags(doorNode.getTags(), TagSet.of());

							VectorXZ v = new VectorXZ(carSegment.offsetOf(carSegment.closestPoint(doorNode.getPos())),0);
							frontSurface.addElementIfSpaceFree(new Door(v, params));
						}
					}

					frontSurface.renderTo(target, new VectorXZ(0, carBaseEle), false, null, true);
					backSurface.renderTo(target, new VectorXZ(0, carBaseEle), false, null, true);

				}


				/* draw floor */

				SimplePolygonXZ carPolygon = new SimplePolygonXZ(carOuterPoints);

				List<TriangleXYZ> trianglesBottomDown = TriangulationUtil.triangulate(carPolygon)
						.stream().map(t -> t.makeClockwise().xyz(carBaseEle - 0.0001)).toList();

				List<TriangleXYZ> trianglesBottomUp = TriangulationUtil.triangulate(carPolygon)
						.stream().map(t -> t.makeCounterclockwise().xyz(carBaseEle - 0.0001)).toList();

				target.drawTriangles(Materials.STEEL, trianglesBottomDown,
						triangleTexCoordLists(trianglesBottomDown, Materials.STEEL, GLOBAL_X_Z));
				target.drawTriangles(Materials.STEEL, trianglesBottomUp,
						triangleTexCoordLists(trianglesBottomUp, Materials.STEEL, GLOBAL_X_Z));


				/* draw ceiling */

				List<TriangleXYZ> trianglesTopDown = TriangulationUtil.triangulate(carPolygon)
						.stream().map(t -> t.makeClockwise().xyz(carBaseEle + carHeight)).toList();

				List<TriangleXYZ> trianglesTopUp = TriangulationUtil.triangulate(carPolygon)
						.stream().map(t -> t.makeCounterclockwise().xyz(carBaseEle + carHeight)).toList();

				target.drawTriangles(Materials.STEEL, trianglesTopDown,
						triangleTexCoordLists(trianglesTopDown, Materials.STEEL, GLOBAL_X_Z));
				target.drawTriangles(Materials.STEEL, trianglesTopUp,
						triangleTexCoordLists(trianglesTopUp, Materials.STEEL, GLOBAL_X_Z));


				/* draw cable */

				//TODO align to be parallel to door walls?

				VectorXZ sideDirection = carPolygon.getSegments().get(0).getDirection();

				target.drawBox(Materials.STEEL, centroid.xyz(cableTopEle - 0.25), sideDirection.normalize(), 0.25, 0.25, 0.4);

				for (int i = 1; i < 5; i++) {
					target.drawBox(Materials.STEEL,
							centroid.xyz(cableTopEle - 0.25).add(sideDirection.mult(-0.2)).add(sideDirection.mult(0.08 * i)),
							sideDirection.normalize(),
							- (cableTopEle - carBaseEle - carHeight - 0.25),
							0.01,
							0.01);
				}

				target.drawBox(Materials.STEEL, centroid.xyz(carBaseEle + carHeight), sideDirection.normalize(), 0.25, 0.25, 0.4);

			}
		}
	}









}
