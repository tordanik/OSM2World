package org.osm2world.core.world.modules;

import static java.lang.Math.*;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.openstreetmap.josm.plugins.graphview.core.util.ValueStringParser.*;
import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.parseHeight;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_data.data.overlaps.MapOverlapWA;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.PolygonXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.algorithms.JTSTriangulationUtil;
import org.osm2world.core.math.algorithms.TriangulationUtil;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.ImmutableMaterial;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Lighting;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.terrain.creation.CAGUtil;
import org.osm2world.core.util.MinMaxUtil;
import org.osm2world.core.world.data.AreaWorldObject;
import org.osm2world.core.world.data.NodeWorldObject;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.modules.common.ConfigurableWorldModule;
import org.osm2world.core.world.modules.common.WorldModuleParseUtil;

import com.google.common.base.Function;

/**
 * adds buildings to the world
 */
public class BuildingModule extends ConfigurableWorldModule {
	
	@Override
	public void applyTo(MapData mapData) {
		
		for (MapArea area : mapData.getMapAreas()) {
			
			if (!area.getRepresentations().isEmpty()) continue;
			
			String buildingValue = area.getTags().getValue("building");
			
			if (buildingValue != null && !buildingValue.equals("no")) {
				
				Building building = new Building(area);
				area.addRepresentation(building);
				
				for (MapNode node : area.getBoundaryNodes()) {
					if (node.getTags().contains("building", "entrance")
							&& node.getRepresentations().isEmpty()) {
						node.addRepresentation(
								new BuildingEntrance(building, node));
					}
				}
				
			}
			
		}
		
	}
	
	public static class Building implements AreaWorldObject,
			RenderableToAllTargets {

		private final MapArea area;
		
		private double heightWithoutRoof;
		private Material materialWall;
		private Material materialRoof;
		private RoofData roofData;
		
		public Building(MapArea area) {

			this.area = area;

			setAttributes();
			
		}
				
		public RoofData getRoofData() {
			return roofData;
		}

		@Override
		public MapElement getPrimaryMapElement() {
			return area;
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public double getClearingAbove(VectorXZ pos) {
			return heightWithoutRoof + roofData.getRoofHeight();
		}

		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}

		@Override
		public void renderTo(Target<?> target) {
			
			renderWalls(target, roofData);
			
			renderRoof(target, roofData);
			
		}

		private void renderFloor(Target<?> target, double floorEle) {
			
			Collection<TriangleXZ> triangles =
				TriangulationUtil.triangulate(area.getPolygon());

			List<TriangleXYZ> trianglesXYZ =
				new ArrayList<TriangleXYZ>(triangles.size());
				
			for (TriangleXZ triangle : triangles) {
				trianglesXYZ.add(triangle.makeClockwise().xyz(floorEle));
			}
				
			target.drawTriangles(materialWall, trianglesXYZ);
			
		}
		
		private void renderRoof(Target<?> target, RoofData roofData) {
						
			Collection<TriangleXZ> triangles =
				JTSTriangulationUtil.triangulate(
						roofData.getPolygon().getOuter(),
						roofData.getPolygon().getHoles(),
						roofData.getInnerSegments(),
						roofData.getInnerPoints());

			List<TriangleXYZ> trianglesXYZ =
				new ArrayList<TriangleXYZ>(triangles.size());
				
			for (TriangleXZ triangle : triangles) {
				TriangleXZ tCCW = triangle.makeCounterclockwise();
				trianglesXYZ.add(new TriangleXYZ(
						getWithRoofEle(tCCW.v1, roofData),
						getWithRoofEle(tCCW.v2, roofData),
						getWithRoofEle(tCCW.v3, roofData)));
			}
			
			target.drawTriangles(materialRoof, trianglesXYZ);
			
		}

		private VectorXYZ getWithRoofEle(VectorXZ v, RoofData roofData) {
			
			Double ele = roofData.getRoofEleAt(v);
			
			if (ele != null) {
				return v.xyz(ele);
			} else {
				
				// get all segments from the roof
				
				//TODO (performance): avoid doing this for every node
				
				Collection<LineSegmentXZ> segments =
					new ArrayList<LineSegmentXZ>();
				
				segments.addAll(roofData.getInnerSegments());
				segments.addAll(roofData.getPolygon().getOuter().getSegments());
				for (SimplePolygonXZ hole : roofData.getPolygon().getHoles()) {
					segments.addAll(hole.getSegments());
				}
				
				// find the segment with the closest distance to the node
				
				LineSegmentXZ closestSegment = null;
				double closestSegmentDistance = Double.MAX_VALUE;
				
				for (LineSegmentXZ segment : segments) {
					double segmentDistance = distanceFromLineSegment(v, segment);
					if (segmentDistance < closestSegmentDistance) {
						closestSegment = segment;
						closestSegmentDistance = segmentDistance;
					}
				}
				
				// use that segment for height interpolation
				
				return v.xyz(interpolateValue(v,
						closestSegment.p1,
						roofData.getRoofEleAt(closestSegment.p1),
						closestSegment.p2,
						roofData.getRoofEleAt(closestSegment.p2)));
				
			}
		}

		private void renderWalls(Target<?> target, RoofData roofData) {
			
			double floorEle = area.getElevationProfile().getMinEle();
			boolean renderFloor = false;
			
			if (area.getTags().containsKey("min_height")) {
				
				Float minEle = parseMeasure(
						area.getTags().getValue("min_height"));
				if (minEle != null) {
					floorEle += minEle;
					renderFloor = true;
				}
				
			} else if (area.getTags().containsKey("building:min_level")
					&& area.getTags().containsKey("building:levels")) {
				
				Float minLevel = parseOsmDecimal(
						area.getTags().getValue("building:min_level"), true);
				Float levels = parseOsmDecimal(
						area.getTags().getValue("building:levels"), false);
				if (minLevel != null && levels != null) {
					double totalHeight = roofData.getMaxRoofEle() - floorEle;
					floorEle += (totalHeight / levels) * minLevel;
					renderFloor = true;
				}
			}
			
			if (area.getOverlaps().isEmpty()) {
				
				renderWalls(target, roofData.getPolygon(), false, floorEle, roofData);
				
			} else {
							
				/* find terrain boundaries on the ground
				 * that overlap with the building */
				
				List<TerrainBoundaryWorldObject> tbWorldObjects = new ArrayList<TerrainBoundaryWorldObject>();
								
				for (MapOverlap<?,?> overlap : area.getOverlaps()) {
					MapElement other = overlap.getOther(area);
					if (other.getPrimaryRepresentation() instanceof TerrainBoundaryWorldObject
							&& other.getPrimaryRepresentation().getGroundState() == GroundState.ON) {
						tbWorldObjects.add((TerrainBoundaryWorldObject)
								other.getPrimaryRepresentation());
					}
				}
				
				/* render building parts where the building polygon does not overlap with terrain boundaries */
				
				List<SimplePolygonXZ> subtractPolygons = new ArrayList<SimplePolygonXZ>();
				for (TerrainBoundaryWorldObject o : tbWorldObjects) {
					subtractPolygons.add(o.getOutlinePolygon().getSimpleXZPolygon());
				}
				subtractPolygons.addAll(roofData.getPolygon().getHoles());
				
				Collection<PolygonWithHolesXZ> buildingPartPolys =
					CAGUtil.subtractPolygons(
							roofData.getPolygon().getOuter(),
							subtractPolygons);
				
				for (PolygonWithHolesXZ p : buildingPartPolys) {
					renderWalls(target, p, false, floorEle, roofData);
					if (renderFloor) {
						renderFloor(target, floorEle);
					}
				}
				
				/* render building parts above the terrain boundaries */
				
				for (TerrainBoundaryWorldObject o : tbWorldObjects) {

					Collection<PolygonWithHolesXZ> raisedBuildingPartPolys;
					
					Collection<PolygonWithHolesXZ> polysAboveTBWOs =
						CAGUtil.intersectPolygons(Arrays.asList(
								area.getOuterPolygon(), o.getOutlinePolygon().getSimpleXZPolygon()));
					
					
					if (area.getHoles().isEmpty()) {
						raisedBuildingPartPolys = polysAboveTBWOs;
					} else {
						raisedBuildingPartPolys = new ArrayList<PolygonWithHolesXZ>();
						for (PolygonWithHolesXZ p : polysAboveTBWOs) {
							List<SimplePolygonXZ> subPolys = new ArrayList<SimplePolygonXZ>();
							subPolys.addAll(area.getPolygon().getHoles());
							subPolys.addAll(p.getHoles());
							raisedBuildingPartPolys.addAll(
									CAGUtil.subtractPolygons(p.getOuter(), subPolys));
						}
					}
					
					for (PolygonWithHolesXZ p : raisedBuildingPartPolys) {
						double clearing = o.getClearingAbove(o.getOutlinePolygon().getSimpleXZPolygon().getCenter());
						double newFloorEle = area.getElevationProfile().getMinEle() + clearing;
						if (newFloorEle < floorEle) {
							newFloorEle = floorEle;
						}
						renderWalls(target, p, false, newFloorEle, roofData);
						renderFloor(target, newFloorEle);
					}
					
				}
				
			}
				
		}

		private void renderWalls(Target<?> target, PolygonWithHolesXZ p,
				boolean renderFloor, double floorEle, RoofData roofData) {
			
			drawWallOnPolygon(target, floorEle, roofData, p.getOuter().makeCounterclockwise());
			
			for (SimplePolygonXZ polygon : p.getHoles()) {
				drawWallOnPolygon(target, floorEle, roofData, polygon.makeClockwise());
			}
			
		}

		private void drawWallOnPolygon(Target<?> target, double floorEle,
				RoofData roofData, SimplePolygonXZ polygon) {
			
			List<VectorXZ> vertices = polygon.getVertexLoop();
			
			VectorXYZ[] wallVectors = new VectorXYZ[vertices.size() * 2];

			for (int i = 0; i < vertices.size(); i++) {
				final VectorXZ coord = vertices.get(i);
				wallVectors[i*2] = getWithRoofEle(coord, roofData);
				double upperEle = wallVectors[i*2].y;
				wallVectors[i*2 + 1] = new VectorXYZ(coord.x,
						min(floorEle, upperEle), coord.z);
			}

			target.drawTriangleStrip(materialWall, wallVectors);
			
		}
		
		/**
		 * sets the building attributes (height, colors) depending on
		 * the building's tags. If available, explicitly tagged data
		 * is used. Otherwise, the values depend on indirect assumptions
		 * (level height) or ultimately the building class as determined
		 * by the "building" key.
		 */
		private void setAttributes() {
			
			String buildingValue = area.getTags().getValue("building");
			
			/* determine defaults for building type */
			
			double defaultHeight = 7.5;
			double defaultHeightPerLevel = 2.5;
			Material defaultMaterialWall = Materials.BUILDING_DEFAULT;
			Material defaultMaterialRoof = Materials.ROOF_DEFAULT;
			String defaultRoofShape = "flat";
			
			if ("greenhouse".equals(buildingValue)) {
				defaultHeight = 2.5;
				defaultMaterialWall = new ImmutableMaterial(Lighting.FLAT,
						new Color(0.9f, 0.9f, 0.9f));
				defaultMaterialRoof = defaultMaterialWall;
			}
			
			/* determine roof shape */
						
			if (hasComplexRoof(area)) {
				roofData = new ComplexRoofData();
			} else {
				
				String roofShape = area.getTags().getValue("roof:shape");
				if (roofShape == null) { roofShape = defaultRoofShape; }
				
				if ("pyramidal".equals(roofShape)) {
					roofData = new PyramidalRoofData();
				} else if ("gabled".equals(roofShape)) {
					roofData = new GabledRoofData();
				} else if ("hipped".equals(roofShape)) {
					roofData = new HippedRoofData();
				} else if ("half-hipped".equals(roofShape)) {
					roofData = new HalfHippedRoofData();
				} else if ("gambrel".equals(roofShape)) {
					roofData = new GambrelRoofData();
				} else if ("mansard".equals(roofShape)) {
					roofData = new MansardRoofData();
				} else {
					roofData = new FlatRoofData();
				}
				
			}
			
			/* determine height */
			
			Float levels = null;
			if (area.getTags().containsKey("building:levels")) {
				levels = parseOsmDecimal(area.getTags().getValue("building:levels"), false);
			}
			
			double fallbackHeight = (levels == null)
				?  defaultHeight
				: (levels * defaultHeightPerLevel);
			
			fallbackHeight += roofData.getRoofHeight();
			
			double height = parseHeight(area.getTags(), (float)fallbackHeight);
		    heightWithoutRoof = height - roofData.getRoofHeight();
			
			/* determine materials */
			
			materialWall = defaultMaterialWall;
			materialRoof = defaultMaterialRoof;
						
		}
		
		private static final double DEFAULT_RIDGE_HEIGHT = 5;
		
		public static interface RoofData {
						
			/**
			 * returns the outline (with holes) of the roof.
			 * The shape will be generally identical to that of the
			 * building itself, but additional vertices might have
			 * been inserted into segments.
			 */
			PolygonWithHolesXZ getPolygon();

			/**
			 * returns segments within the roof polygon
			 * that define ridges or edges of the roof
			 */
			Collection<LineSegmentXZ> getInnerSegments();

			/**
			 * returns segments within the roof polygon
			 * that define apex nodes of the roof
			 */
			Collection<VectorXZ> getInnerPoints();
			
			/**
			 * returns maximum roof height
			 */
			double getRoofHeight();

			/**
			 * returns roof elevation at a position.
			 * Only required to work for positions that are part of the
			 * polygon, segments or points for the roof.
			 * 
			 * @return  elevation, null if unknown
			 */
			Double getRoofEleAt(VectorXZ pos);

			/**
			 * returns maximum roof elevation
			 */
			double getMaxRoofEle();
			
		}
		
		private class FlatRoofData implements RoofData {
				
			@Override
			public PolygonWithHolesXZ getPolygon() {
				return area.getPolygon();
			}

			@Override
			public Collection<VectorXZ> getInnerPoints() {
				return emptyList();
			}

			@Override
			public Collection<LineSegmentXZ> getInnerSegments() {
				return emptyList();
			}

			@Override
			public double getRoofHeight() {
				return 0;
			}

			@Override
			public Double getRoofEleAt(VectorXZ pos) {
				return getMaxRoofEle();
			}
			
			@Override
			public double getMaxRoofEle() {
				return area.getElevationProfile().getMinEle() + heightWithoutRoof;
			}
			
		}

		/**
		 * superclass for roofs based on roof:type tags.
		 * Contains common functionality, such as roof height parsing.
		 */
		abstract private class TaggedRoofData implements RoofData {
		
			protected final double roofHeight;
			
			TaggedRoofData() {
				
				Float taggedHeight = null;
				
				if (area.getTags().containsKey("roof:height")) {
					String valueString = area.getTags().getValue("roof:height");
					taggedHeight = parseMeasure(valueString);
				}
				
				roofHeight =
					taggedHeight != null ? taggedHeight : DEFAULT_RIDGE_HEIGHT;
				
			}
			
			@Override
			public double getRoofHeight() {
				return roofHeight;
			}
			
			@Override
			public double getMaxRoofEle() {
				return area.getElevationProfile().getMinEle() +
						heightWithoutRoof + roofHeight;
			}
			
		}
		
		private class PyramidalRoofData extends TaggedRoofData {
			
			private final VectorXZ apex;
			private final List<LineSegmentXZ> innerSegments;
			
			public PyramidalRoofData() {
			
				super();
				
				SimplePolygonXZ polygon = area.getOuterPolygon();
				
				apex = polygon.getCentroid();
				
				innerSegments = new ArrayList<LineSegmentXZ>();
				for (VectorXZ v : polygon.getVertices()) {
					innerSegments.add(new LineSegmentXZ(v, apex));
				}
				
			}
			
			@Override
			public PolygonWithHolesXZ getPolygon() {
				return area.getPolygon();
			}

			@Override
			public Collection<VectorXZ> getInnerPoints() {
				return singletonList(apex);
			}

			@Override
			public Collection<LineSegmentXZ> getInnerSegments() {
				return innerSegments;
			}

			@Override
			public Double getRoofEleAt(VectorXZ pos) {
				if (apex.equals(pos)) {
					return getMaxRoofEle();
				} else if (area.getOuterPolygon().getVertices().contains(pos)) {
					return getMaxRoofEle() - roofHeight;
				} else {
					return null;
				}
			}
			
		}
				
		/**
		 * tagged roof with a ridge.
		 * Deals with ridge calculation for various subclasses.
		 */
		abstract private class RoofDataWithRidge extends TaggedRoofData {
			
			/** absolute distance of ridge to outline */
			protected final double ridgeOffset;
			
			protected final LineSegmentXZ ridge;
			
			/** the roof cap that is closer to the first vertex of the ridge */
			protected final LineSegmentXZ cap1;
			/** the roof cap that is closer to the second vertex of the ridge */
			protected final LineSegmentXZ cap2;
			
			/** maximum distance of any outline vertex to the ridge */
			protected final double maxDistanceToRidge;
			
			/**
			 * creates an instance and calculates the final fields
			 * 
			 * @param relativeRoofOffset  distance of ridge to outline
			 *    relative to length of roof cap; 0 if ridge ends at outline
			 */
			public RoofDataWithRidge(double relativeRoofOffset) {
			
				super();
				
				SimplePolygonXZ polygon = area.getOuterPolygon();
				
				SimplePolygonXZ simplifiedPolygon =
					polygon.getSimplifiedPolygon();
				
				/* determine ridge direction based on tag if it exists,
				 * otherwise choose direction of longest polygon segment */
				
				VectorXZ ridgeDirection = null;
				
				if (area.getTags().containsKey("roof:ridge:direction")) {
					Float angle = parseAngle(
							area.getTags().getValue("roof:ridge:direction"));
					if (angle != null) {
						ridgeDirection = VectorXZ.fromAngle(toRadians(angle));
					}
				}
				
				if (ridgeDirection == null) {
					
					LineSegmentXZ longestSeg = MinMaxUtil.max(
							simplifiedPolygon.getSegments(),
							new Function<LineSegmentXZ, Double>() {
								public Double apply(LineSegmentXZ s) {
									return s.getLength();
								};
							});
							
					ridgeDirection =
						longestSeg.p2.subtract(longestSeg.p1).normalize();
						
				}
				
				/* calculate the two outermost intersections of the
				 * quasi-infinite ridge line with segments of the polygon */
				
				VectorXZ p1 = polygon.getCentroid();
				
				Collection<LineSegmentXZ> intersections =
					simplifiedPolygon.intersectionSegments(new LineSegmentXZ(
							p1.add(ridgeDirection.mult(-1000)),
							p1.add(ridgeDirection.mult(1000))
					));

				//TODO choose outermost instead of any pair of intersections
				Iterator<LineSegmentXZ> it = intersections.iterator();
				cap1 = it.next();
				cap2 = it.next();
				
				/* base ridge on the centers of the intersected segments
				 * (the intersections itself are not used because the
				 * tagged ridge direction is likely not precise)       */

				VectorXZ c1 = cap1.getCenter();
				VectorXZ c2 = cap2.getCenter();
				
				ridgeOffset = min(
						cap1.getLength() * relativeRoofOffset,
						0.4 * c1.distanceTo(c2));
				
				if (relativeRoofOffset == 0) {
					
					ridge = new LineSegmentXZ(c1, c2);
					
				} else {
					
					ridge = new LineSegmentXZ(
							c1.add( p1.subtract(c1).normalize().mult(ridgeOffset) ),
							c2.add( p1.subtract(c2).normalize().mult(ridgeOffset) ));
					
				}
				
				/* calculate maxDistanceToRidge */
				
				double maxDistance = 0;
				
				for (VectorXZ v : polygon.getVertices()) {
					maxDistance = max (maxDistance,
							distanceFromLineSegment(v, ridge));
				}
				
				maxDistanceToRidge = maxDistance;
				
			}
			
		}
		
		private class GabledRoofData extends RoofDataWithRidge {

			public GabledRoofData() {
				super(0);
			}
			
			@Override
			public PolygonWithHolesXZ getPolygon() {
				
				PolygonXZ newOuter = area.getOuterPolygon();
				
				newOuter = insertIntoPolygon(newOuter, ridge.p1, 0.2);
				newOuter = insertIntoPolygon(newOuter, ridge.p2, 0.2);
				
				return new PolygonWithHolesXZ(
						newOuter.asSimplePolygon(),
						area.getPolygon().getHoles());
				
			}
			
			@Override
			public Collection<VectorXZ> getInnerPoints() {
				return emptyList();
			}
			
			@Override
			public Collection<LineSegmentXZ> getInnerSegments() {
				return singleton(ridge);
			}

			@Override
			public Double getRoofEleAt(VectorXZ pos) {
				double distRidge = distanceFromLineSegment(pos, ridge);
				double relativePlacement = distRidge / maxDistanceToRidge;
				return getMaxRoofEle() - roofHeight * relativePlacement;
			}
			
		}
		
		private class HippedRoofData extends RoofDataWithRidge {

			public HippedRoofData() {
				super(1/3.0);
			}
			
			@Override
			public PolygonWithHolesXZ getPolygon() {
				return area.getPolygon();
			}
			
			@Override
			public Collection<VectorXZ> getInnerPoints() {
				return emptyList();
			}
			
			@Override
			public Collection<LineSegmentXZ> getInnerSegments() {
				return asList(
						ridge,
						new LineSegmentXZ(ridge.p1, cap1.p1),
						new LineSegmentXZ(ridge.p1, cap1.p2),
						new LineSegmentXZ(ridge.p2, cap2.p1),
						new LineSegmentXZ(ridge.p2, cap2.p2));
			}

			@Override
			public Double getRoofEleAt(VectorXZ pos) {
				if (ridge.p1.equals(pos) || ridge.p2.equals(pos)) {
					return getMaxRoofEle();
				} else if (getPolygon().getOuter().getVertexLoop().contains(pos)) {
					return getMaxRoofEle() - roofHeight;
				} else {
					return null;
				}
			}
			
		}
		
		private class HalfHippedRoofData extends RoofDataWithRidge {

			private final LineSegmentXZ cap1part, cap2part;
			
			public HalfHippedRoofData() {
				
				super(1/6.0);
				
				cap1part = new LineSegmentXZ(
						interpolateBetween(cap1.p1, cap1.p2,
								0.5 - ridgeOffset / cap1.getLength()),
						interpolateBetween(cap1.p1, cap1.p2,
								0.5 + ridgeOffset / cap1.getLength()));
				
				cap2part = new LineSegmentXZ(
						interpolateBetween(cap2.p1, cap2.p2,
								0.5 - ridgeOffset / cap1.getLength()),
						interpolateBetween(cap2.p1, cap2.p2,
								0.5 + ridgeOffset / cap1.getLength()));
				
			}
			
			@Override
			public PolygonWithHolesXZ getPolygon() {
				
				PolygonXZ newOuter = area.getOuterPolygon();
				
				newOuter = insertIntoPolygon(newOuter, cap1part.p1, 0.2);
				newOuter = insertIntoPolygon(newOuter, cap1part.p2, 0.2);
				newOuter = insertIntoPolygon(newOuter, cap2part.p1, 0.2);
				newOuter = insertIntoPolygon(newOuter, cap2part.p2, 0.2);
				
				return new PolygonWithHolesXZ(
						newOuter.asSimplePolygon(),
						area.getPolygon().getHoles());
				
			}
			
			@Override
			public Collection<VectorXZ> getInnerPoints() {
				return emptyList();
			}
			
			@Override
			public Collection<LineSegmentXZ> getInnerSegments() {
				return asList(ridge,
						new LineSegmentXZ(ridge.p1, cap1part.p1),
						new LineSegmentXZ(ridge.p1, cap1part.p2),
						new LineSegmentXZ(ridge.p2, cap2part.p1),
						new LineSegmentXZ(ridge.p2, cap2part.p2));
			}

			@Override
			public Double getRoofEleAt(VectorXZ pos) {
				if (ridge.p1.equals(pos) || ridge.p2.equals(pos)) {
					return getMaxRoofEle();
				} else if (getPolygon().getOuter().getVertexLoop().contains(pos)) {
					if (distanceFromLineSegment(pos, cap1part) < 0.05) {
						return getMaxRoofEle()
							- roofHeight * ridgeOffset / (cap1.getLength()/2);
					} else if (distanceFromLineSegment(pos, cap2part) < 0.05) {
						return getMaxRoofEle()
							- roofHeight * ridgeOffset / (cap2.getLength()/2);
					} else {
						return getMaxRoofEle() - roofHeight;
					}
				} else {
					return null;
				}
			}
			
		}
		
		private class GambrelRoofData extends RoofDataWithRidge {

			private final LineSegmentXZ cap1part, cap2part;
			
			public GambrelRoofData() {
				
				super(0);
				
				cap1part = new LineSegmentXZ(
						interpolateBetween(cap1.p1, cap1.p2, 1/6.0),
						interpolateBetween(cap1.p1, cap1.p2, 5/6.0));

				cap2part = new LineSegmentXZ(
						interpolateBetween(cap2.p1, cap2.p2, 1/6.0),
						interpolateBetween(cap2.p1, cap2.p2, 5/6.0));
				
			}
			
			@Override
			public PolygonWithHolesXZ getPolygon() {
				
				PolygonXZ newOuter = area.getOuterPolygon();

				newOuter = insertIntoPolygon(newOuter, ridge.p1, 0.2);
				newOuter = insertIntoPolygon(newOuter, ridge.p2, 0.2);
				newOuter = insertIntoPolygon(newOuter, cap1part.p1, 0.2);
				newOuter = insertIntoPolygon(newOuter, cap1part.p2, 0.2);
				newOuter = insertIntoPolygon(newOuter, cap2part.p1, 0.2);
				newOuter = insertIntoPolygon(newOuter, cap2part.p2, 0.2);
				
				//TODO: add intersections of additional edges with outline?
				
				return new PolygonWithHolesXZ(
						newOuter.asSimplePolygon(),
						area.getPolygon().getHoles());
				
			}
			
			@Override
			public Collection<VectorXZ> getInnerPoints() {
				return emptyList();
			}
			
			@Override
			public Collection<LineSegmentXZ> getInnerSegments() {
				return asList(ridge,
						new LineSegmentXZ(cap1part.p1, cap2part.p2),
						new LineSegmentXZ(cap1part.p2, cap2part.p1));
			}

			@Override
			public Double getRoofEleAt(VectorXZ pos) {

				double distRidge = distanceFromLineSegment(pos, ridge);
				double relativePlacement = distRidge / maxDistanceToRidge;
				
				if (relativePlacement < 2/3.0) {
					return getMaxRoofEle()
						- 1/2.0 * roofHeight * relativePlacement;
				} else {
					return getMaxRoofEle() - 1/3.0 * roofHeight
						- 2 * roofHeight * (relativePlacement - 2/3.0);
				}
				
			}
			
		}
		
		private class MansardRoofData extends RoofDataWithRidge {

			private final LineSegmentXZ mansardEdge1, mansardEdge2;
			
			public MansardRoofData() {
				
				super(1/3.0);
				
				mansardEdge1 = new LineSegmentXZ(
						interpolateBetween(cap1.p1, ridge.p1, 1/3.0),
						interpolateBetween(cap2.p2, ridge.p2, 1/3.0));

				mansardEdge2 = new LineSegmentXZ(
						interpolateBetween(cap1.p2, ridge.p1, 1/3.0),
						interpolateBetween(cap2.p1, ridge.p2, 1/3.0));
				
			}
			
			@Override
			public PolygonWithHolesXZ getPolygon() {
				return area.getPolygon();
			}
			
			@Override
			public Collection<VectorXZ> getInnerPoints() {
				return emptyList();
			}
			
			@Override
			public Collection<LineSegmentXZ> getInnerSegments() {
				return asList(ridge,
						mansardEdge1,
						mansardEdge2,
						new LineSegmentXZ(ridge.p1, mansardEdge1.p1),
						new LineSegmentXZ(ridge.p1, mansardEdge2.p1),
						new LineSegmentXZ(ridge.p2, mansardEdge1.p2),
						new LineSegmentXZ(ridge.p2, mansardEdge2.p2),
						new LineSegmentXZ(cap1.p1, mansardEdge1.p1),
						new LineSegmentXZ(cap2.p2, mansardEdge1.p2),
						new LineSegmentXZ(cap1.p2, mansardEdge2.p1),
						new LineSegmentXZ(cap2.p1, mansardEdge2.p2),
						new LineSegmentXZ(mansardEdge1.p1, mansardEdge2.p1),
						new LineSegmentXZ(mansardEdge1.p2, mansardEdge2.p2));
			}

			@Override
			public Double getRoofEleAt(VectorXZ pos) {

				if (ridge.p1.equals(pos) || ridge.p2.equals(pos)) {
					return getMaxRoofEle();
				} else if (getPolygon().getOuter().getVertexLoop().contains(pos)) {
					return getMaxRoofEle() - roofHeight;
				} else if (mansardEdge1.p1.equals(pos)
						|| mansardEdge1.p2.equals(pos)
						|| mansardEdge2.p1.equals(pos)
						|| mansardEdge2.p2.equals(pos)) {
					return getMaxRoofEle() - 1/3.0 * roofHeight;
				} else {
					return null;
				}
				
			}
			
		}
		
		/**
		 * roof that has been mapped with explicit roof edge/ridge/apex elements
		 */
		private class ComplexRoofData implements RoofData {
						
			private double roofHeight = 0;
			private final Map<VectorXZ, Double> roofHeightMap;
			
			private final Collection<LineSegmentXZ> ridgeAndEdgeSegments;
			
			public ComplexRoofData() {

				/* find ridge and/or edges
				 * (apex nodes don't need to be handled separately
				 *  as they should always be part of an edge segment) */
				
				roofHeightMap = new HashMap<VectorXZ, Double>();
				
				ridgeAndEdgeSegments = new ArrayList<LineSegmentXZ>();
				
				for (MapOverlap<?,?> overlap : area.getOverlaps()) {
					
					if (overlap instanceof MapOverlapWA) {
						
						MapWaySegment waySegment = ((MapOverlapWA)overlap).e1;
						
						boolean isRidge = waySegment.getTags().contains("roof:ridge", "yes");
						boolean isEdge = waySegment.getTags().contains("roof:edge", "yes");
												
						if (isRidge || isEdge) {
							
							ridgeAndEdgeSegments.add(waySegment.getLineSegment());
							
							for (MapNode node : waySegment.getStartEndNodes()) {
								
								// height of node (above roof base)
								double nodeHeight = Double.NaN;
								
								if (node.getTags().containsKey("height")) {
									nodeHeight =
										parseHeight(node.getTags(), Float.NaN);
								} else if (waySegment.getTags().containsKey("height")) {
									nodeHeight =
										parseHeight(waySegment.getTags(), Float.NaN);
								} else if (node.getTags().contains("roof:apex", "yes")) {
									nodeHeight = DEFAULT_RIDGE_HEIGHT;
								} else if (isRidge) {
									nodeHeight = DEFAULT_RIDGE_HEIGHT;
								}
								
								if (!Double.isNaN(nodeHeight)) {
									
									roofHeightMap.put(node.getPos(), nodeHeight);
									
									roofHeight = max(roofHeight, nodeHeight);
									
								}
								
							}
							
						}
						
					}
					
				}
				
				/* add heights for outline nodes that don't have one yet */
				
				for (MapNode node : area.getBoundaryNodes()) {
					if (!roofHeightMap.containsKey(node.getPos())) {
						roofHeightMap.put(node.getPos(), 0.0);
					}
				}
				
				for (List<MapNode> hole : area.getHoles()) {
					for (MapNode node : hole) {
						if (!roofHeightMap.containsKey(node.getPos())) {
							roofHeightMap.put(node.getPos(), 0.0);
						}
					}
				}

				/* add heights for edge nodes that are not also
				 * ridge/outline/apex nodes. This will just use base height
				 * for them instead of trying to interpolate heights along
				 * chains of edge segments. Results are therefore wrong,
				 * but there's no reason to map them like that anyway. */
				
				for (LineSegmentXZ segment : ridgeAndEdgeSegments) {
					if (!roofHeightMap.containsKey(segment.p1)) {
						roofHeightMap.put(segment.p1, 0.0);
					}
					if (!roofHeightMap.containsKey(segment.p2)) {
						roofHeightMap.put(segment.p2, 0.0);
					}
				}
				
			}
			
			@Override
			public PolygonWithHolesXZ getPolygon() {
				return area.getPolygon();
			}

			@Override
			public Collection<VectorXZ> getInnerPoints() {
				return emptyList();
			}

			@Override
			public Collection<LineSegmentXZ> getInnerSegments() {
				return ridgeAndEdgeSegments;
			}
			
			@Override
			public double getRoofHeight() {
				return roofHeight;
			}

			@Override
			public Double getRoofEleAt(VectorXZ pos) {
				if (roofHeightMap.containsKey(pos)) {
					return area.getElevationProfile().getMinEle()
						+ heightWithoutRoof + roofHeightMap.get(pos);
				} else {
					return null;
				}
			}
			
			@Override
			public double getMaxRoofEle() {
				return area.getElevationProfile().getMinEle()
					+ heightWithoutRoof + roofHeight;
			}
			
		}
		
		public static boolean hasComplexRoof(MapArea area) {
			for (MapOverlap<?,?> overlap : area.getOverlaps()) {
				if (overlap instanceof MapOverlapWA) {
					TagGroup tags = overlap.e1.getTags();
					if (tags.contains("roof:ridge", "yes")
							|| tags.contains("roof:edge", "yes")) {
						return true;
					}
				}
			}
			return false;
		}
		
	}
	
	private static class BuildingEntrance implements NodeWorldObject,
		RenderableToAllTargets {
		
		private final Building building;
		private final MapNode node;
		
		public BuildingEntrance(Building building, MapNode node) {
			this.building = building;
			this.node = node;
		}
		
		@Override
		public MapElement getPrimaryMapElement() {
			return node;
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			/* calculate a vector that points into the building */
			
			VectorXZ intoBuilding = VectorXZ.Z_UNIT;
			
			for (SimplePolygonXZ polygon :
				building.area.getPolygon().getPolygons()) {
				
				final List<VectorXZ> vs = polygon.getVertexLoop();
				int entranceI = vs.indexOf(node.getPos());
				
				if (entranceI != -1) {
					
					VectorXZ posBefore = vs.get((vs.size() + entranceI - 1) % vs.size());
					VectorXZ posAfter = vs.get((vs.size() + entranceI + 1) % vs.size());
					
					intoBuilding = posAfter.subtract(posBefore).rightNormal();
					if (polygon.isClockwise()) {
						intoBuilding = intoBuilding.invert();
					}
					
					break;
					
				}
				
			}
			
			/* use height and width */
			
			float height = WorldModuleParseUtil.parseHeight(node.getTags(), 2);
			float width = WorldModuleParseUtil.parseWidth(node.getTags(), 1);
			
			VectorXYZ right = intoBuilding.rightNormal().mult(width).xyz(0);
			VectorXYZ up = VectorXYZ.Y_UNIT.mult(height);
			
			/* draw the entrance as a box protruding from the building */
			
			VectorXYZ center = node.getElevationProfile().getPointWithEle();
			
			target.drawBox(Materials.ENTRANCE_DEFAULT,
					center.subtract(right.mult(0.5)),
					right, up, intoBuilding.xyz(0).mult(0.1));
			
		}
		
	}

}