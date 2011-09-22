package org.osm2world.core.world.modules;

import static java.lang.Math.*;
import static java.util.Collections.*;
import static org.openstreetmap.josm.plugins.graphview.core.util.ValueStringParser.*;
import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.parseHeight;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
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
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.target.common.material.Material.Lighting;
import org.osm2world.core.terrain.creation.CAGUtil;
import org.osm2world.core.world.data.AreaWorldObject;
import org.osm2world.core.world.data.NodeWorldObject;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.modules.common.ConfigurableWorldModule;
import org.osm2world.core.world.modules.common.WorldModuleParseUtil;

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
	
	private static class Building implements AreaWorldObject,
			RenderableToAllTargets {

		private final MapArea area;
		
		private double height;
		private Material materialWall;
		private Material materialRoof;
		private RoofData roofData;
		
		public Building(MapArea area) {

			this.area = area;

			setAttributes();
			
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
			return height;
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
				
				renderWalls(target, area.getPolygon(), false, floorEle, roofData);
				
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
			
			double defaultHeight = 10;
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
			
			height = parseHeight(area.getTags(), (float)fallbackHeight);
			
			/* determine materials */
			
			materialWall = defaultMaterialWall;
			materialRoof = defaultMaterialRoof;
						
		}
		
		private static final double DEFAULT_RIDGE_HEIGHT = 5;
		
		private static interface RoofData {
						
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
				return area.getElevationProfile().getMinEle() + height;
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
				return height;
			}
			
			@Override
			public double getMaxRoofEle() {
				return area.getElevationProfile().getMinEle() + height;
			}
			
		}
		
		private class PyramidalRoofData extends TaggedRoofData {
			
			private final VectorXZ apex;
			private final List<LineSegmentXZ> innerSegments;
			
			public PyramidalRoofData() {
			
				super();
				
				PolygonXZ polygon = area.getPolygon().getOuter();
				
				if (polygon.isSimple()) {
					apex = polygon.asSimplePolygon().getCentroid();
				} else {
					apex = polygon.getCenter();
				}
				
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
				} else if (area.getPolygon().getOuter().getVertices().contains(pos)) {
					return getMaxRoofEle() - roofHeight;
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
							roofHeightMap.put(node.getPos(), height - roofHeight);
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
						roofHeightMap.put(segment.p1, height - roofHeight);
					}
					if (!roofHeightMap.containsKey(segment.p2)) {
						roofHeightMap.put(segment.p2, height - roofHeight);
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
						+ height + roofHeightMap.get(pos);
				} else {
					return null;
				}
			}
			
			@Override
			public double getMaxRoofEle() {
				return area.getElevationProfile().getMinEle()
					+ height + roofHeight;
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