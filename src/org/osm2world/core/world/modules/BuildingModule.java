package org.osm2world.core.world.modules;

import static com.google.common.base.Preconditions.checkArgument;
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
import org.osm2world.core.math.PolygonXYZ;
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
import org.osm2world.core.world.data.WaySegmentWorldObject;
import org.osm2world.core.world.data.WorldObjectWithOutline;
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
				
				Building buildingPart = new Building(area);
				area.addRepresentation(buildingPart);
								
			}
			
		}
		
	}
	
	public static class Building implements AreaWorldObject,
		WorldObjectWithOutline, RenderableToAllTargets {
		
		private final MapArea area;
		private final List<BuildingPart> parts =
				new ArrayList<BuildingModule.BuildingPart>();
		
		public Building(MapArea area) {
			
			this.area = area;
			
			for (MapOverlap<?,?> overlap : area.getOverlaps()) {
				MapElement other = overlap.getOther(area);
				if (other instanceof MapArea
						&& other.getTags().containsKey("building:part")) {
					
					MapArea otherArea = (MapArea)other;
					
					if (area.getOuterPolygon().contains(
							otherArea.getOuterPolygon().getCenter())) {
						parts.add(new BuildingPart(this, otherArea,
								otherArea.getPolygon()));
					}
					
				}
			}
			
			/* add part(s) for area not covered by building:part polygons */
			
			if (parts.isEmpty()) {
				parts.add(new BuildingPart(this, area, area.getPolygon()));
			} else {
				
				List<SimplePolygonXZ> subtractPolygons = new ArrayList<SimplePolygonXZ>();
				
				for (BuildingPart part : parts) {
					subtractPolygons.add(part.getPolygon().getOuter());
				}
				subtractPolygons.addAll(area.getPolygon().getHoles());
				
				Collection<PolygonWithHolesXZ> remainingPolys =
					CAGUtil.subtractPolygons(
							area.getPolygon().getOuter(),
							subtractPolygons);
				
				for (PolygonWithHolesXZ remainingPoly : remainingPolys) {
					parts.add(new BuildingPart(this, area, remainingPoly));
				}
				
			}
			
		}

		public MapArea getArea() {
			return area;
		}
		
		public List<BuildingPart> getParts() {
			return parts;
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
			double maxClearingAbove = 0;
			for (BuildingPart part : parts) {
				double clearing = part.getClearingAbove(pos);
				maxClearingAbove = max(clearing, maxClearingAbove);
			}
			return maxClearingAbove;
		}

		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}

		@Override
		public PolygonXYZ getOutlinePolygon() {
			return area.getPolygon().getOuter().xyz(
					area.getElevationProfile().getMinEle());
		}
		
		@Override
		public void renderTo(Target<?> target) {
			for (BuildingPart part : parts) {
				part.renderTo(target);
			}
		}
		
	}
	
	public static class BuildingPart implements RenderableToAllTargets {

		private final Building building;
		private final MapArea area;
		private final PolygonWithHolesXZ polygon;
		
		private double heightWithoutRoof;
		private Material materialWall;
		private Material materialRoof;
		private Roof roof;
		
		public BuildingPart(Building building,
				MapArea area, PolygonWithHolesXZ polygon) {

			this.building = building;
			this.area = area;
			this.polygon = polygon;

			setAttributes();
			
			for (MapNode node : area.getBoundaryNodes()) {
				if ((node.getTags().contains("building", "entrance")
						|| node.getTags().containsKey("entrance"))
						&& node.getRepresentations().isEmpty()) {
					node.addRepresentation(
							new BuildingEntrance(this, node));
				}
			}
			
		}
		
		public PolygonWithHolesXZ getPolygon() {
			return polygon;
		}
				
		public Roof getRoof() {
			return roof;
		}

		public double getClearingAbove(VectorXZ pos) {
			return heightWithoutRoof + roof.getRoofHeight();
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			renderWalls(target, roof);
			
			roof.renderTo(target);
			
		}

		private void renderFloor(Target<?> target, double floorEle) {
			
			Collection<TriangleXZ> triangles =
				TriangulationUtil.triangulate(polygon);

			List<TriangleXYZ> trianglesXYZ =
				new ArrayList<TriangleXYZ>(triangles.size());
				
			for (TriangleXZ triangle : triangles) {
				trianglesXYZ.add(triangle.makeClockwise().xyz(floorEle));
			}
				
			target.drawTriangles(materialWall, trianglesXYZ);
			
		}

		private void renderWalls(Target<?> target, Roof roof) {
			
			double floorEle = calculateFloorEle(roof);
			boolean renderFloor = (floorEle >
				building.getArea().getElevationProfile().getMinEle());
			
			if (area.getOverlaps().isEmpty()) {
				
				renderWalls(target, roof.getPolygon(), false, floorEle, roof);
				
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
					
					SimplePolygonXZ subtractPoly =
							o.getOutlinePolygon().getSimpleXZPolygon();
					
					subtractPolygons.add(subtractPoly);
					
					if (o instanceof WaySegmentWorldObject) {
						
						// extend the subtract polygon for segments that end
						// at a common node with this building part's outline.
						// (otherwise, the subtract polygon will probably
						// not exactly line up with the polygon boundary)
						
						WaySegmentWorldObject waySegmentWO = (WaySegmentWorldObject)o;
						VectorXZ start = waySegmentWO.getStartPosition();
						VectorXZ end = waySegmentWO.getEndPosition();
						
						boolean startCommonNode = false;
						boolean endCommonNode = false;
						
						for (SimplePolygonXZ p : polygon.getPolygons()) {
							startCommonNode |= p.getVertexCollection().contains(start);
							endCommonNode |= p.getVertexCollection().contains(end);
						}
						
						VectorXZ direction = end.subtract(start).normalize();
						
						if (startCommonNode) {
							subtractPolygons.add(subtractPoly.shift(direction));
						}
						
						if (endCommonNode) {
							subtractPolygons.add(subtractPoly.shift(direction.invert()));
						}
						
					}
					
				}
				
				subtractPolygons.addAll(roof.getPolygon().getHoles());
				
				Collection<PolygonWithHolesXZ> buildingPartPolys =
					CAGUtil.subtractPolygons(
							roof.getPolygon().getOuter(),
							subtractPolygons);
				
				for (PolygonWithHolesXZ p : buildingPartPolys) {
					renderWalls(target, p, false, floorEle, roof);
					if (renderFloor) {
						renderFloor(target, floorEle);
					}
				}
				
				/* render building parts above the terrain boundaries */
				
				for (TerrainBoundaryWorldObject o : tbWorldObjects) {

					Collection<PolygonWithHolesXZ> raisedBuildingPartPolys;
					
					Collection<PolygonWithHolesXZ> polysAboveTBWOs =
						CAGUtil.intersectPolygons(Arrays.asList(
								polygon.getOuter(), o.getOutlinePolygon().getSimpleXZPolygon()));
					
					
					if (polygon.getHoles().isEmpty()) {
						raisedBuildingPartPolys = polysAboveTBWOs;
					} else {
						raisedBuildingPartPolys = new ArrayList<PolygonWithHolesXZ>();
						for (PolygonWithHolesXZ p : polysAboveTBWOs) {
							List<SimplePolygonXZ> subPolys = new ArrayList<SimplePolygonXZ>();
							subPolys.addAll(polygon.getHoles());
							subPolys.addAll(p.getHoles());
							raisedBuildingPartPolys.addAll(
									CAGUtil.subtractPolygons(p.getOuter(), subPolys));
						}
					}
					
					for (PolygonWithHolesXZ p : raisedBuildingPartPolys) {
						double clearing = o.getClearingAbove(o.getOutlinePolygon().getSimpleXZPolygon().getCenter());
						double newFloorEle = building.getArea().getElevationProfile().getMinEle() + clearing;
						if (newFloorEle < floorEle) {
							newFloorEle = floorEle;
						}
						renderWalls(target, p, false, newFloorEle, roof);
						renderFloor(target, newFloorEle);
					}
					
				}
				
			}
				
		}

		private double calculateFloorEle(Roof roof) {
			double floorEle = building.getArea().getElevationProfile().getMinEle();
			
			if (area.getTags().containsKey("min_height")) {
				
				Float minEle = parseMeasure(
						area.getTags().getValue("min_height"));
				if (minEle != null) {
					floorEle += minEle;
				}
				
			} else if (area.getTags().containsKey("building:min_level")
					&& area.getTags().containsKey("building:levels")) {
				
				Float minLevel = parseOsmDecimal(
						area.getTags().getValue("building:min_level"), true);
				Float levels = parseOsmDecimal(
						area.getTags().getValue("building:levels"), false);
				if (minLevel != null && levels != null) {
					double totalHeight = heightWithoutRoof + roof.getRoofHeight();
					floorEle += (totalHeight / levels) * minLevel;
				}
			}
			return floorEle;
		}

		private void renderWalls(Target<?> target, PolygonWithHolesXZ p,
				boolean renderFloor, double floorEle, Roof roof) {
			
			drawWallOnPolygon(target, floorEle, roof, p.getOuter().makeCounterclockwise());
			
			for (SimplePolygonXZ polygon : p.getHoles()) {
				drawWallOnPolygon(target, floorEle, roof, polygon.makeClockwise());
			}
			
		}

		private void drawWallOnPolygon(Target<?> target, double floorEle,
				Roof roof, SimplePolygonXZ polygon) {
			
			List<VectorXZ> vertices = polygon.getVertexLoop();
			
			VectorXYZ[] wallVectors = new VectorXYZ[vertices.size() * 2];

			for (int i = 0; i < vertices.size(); i++) {
				final VectorXZ coord = vertices.get(i);
				wallVectors[i*2] = coord.xyz(roof.getRoofEleAt(coord));
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
				roof = new ComplexRoof();
			} else {
				
				String roofShape = area.getTags().getValue("roof:shape");
				if (roofShape == null) { roofShape = defaultRoofShape; }
				
				if ("pyramidal".equals(roofShape)) {
					roof = new PyramidalRoof();
				} else if ("onion".equals(roofShape)) {
					roof = new OnionRoof();
				} else if ("gabled".equals(roofShape)) {
					roof = new GabledRoof();
				} else if ("hipped".equals(roofShape)) {
					roof = new HippedRoof();
				} else if ("half-hipped".equals(roofShape)) {
					roof = new HalfHippedRoof();
				} else if ("gambrel".equals(roofShape)) {
					roof = new GambrelRoof();
				} else if ("mansard".equals(roofShape)) {
					roof = new MansardRoof();
				} else {
					roof = new FlatRoof();
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
			
			fallbackHeight += roof.getRoofHeight();
			
			double height = parseHeight(area.getTags(), (float)fallbackHeight);
		    heightWithoutRoof = height - roof.getRoofHeight();
			
			/* determine materials */
			
			materialWall = defaultMaterialWall;
			materialRoof = defaultMaterialRoof;
						
		}
		
		private static final float DEFAULT_RIDGE_HEIGHT = 5;
		
		public static interface Roof extends RenderableToAllTargets {
						
			/**
			 * returns the outline (with holes) of the roof.
			 * The shape will be generally identical to that of the
			 * building itself, but additional vertices might have
			 * been inserted into segments.
			 */
			PolygonWithHolesXZ getPolygon();

			/**
			 * returns roof elevation at a position.
			 */
			double getRoofEleAt(VectorXZ coord);
			
			/**
			 * returns maximum roof height
			 */
			double getRoofHeight();

			/**
			 * returns maximum roof elevation
			 */
			double getMaxRoofEle();
			
		}

		/**
		 * superclass for roofs based on roof:type tags.
		 * Contains common functionality, such as roof height parsing.
		 */
		abstract private class TaggedRoof implements Roof {
		
			protected final double roofHeight;
			
			TaggedRoof() {
				
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
				return building.getArea().getElevationProfile().getMinEle() +
						heightWithoutRoof + roofHeight;
			}
			
		}
		
		private class OnionRoof extends TaggedRoof {

			@Override
			public PolygonWithHolesXZ getPolygon() {
				return polygon;
			}

			@Override
			public double getRoofEleAt(VectorXZ pos) {
				return getMaxRoofEle() - getRoofHeight();
			}
			
			@Override
			public void renderTo(Target<?> target) {
				
				double roofY = getMaxRoofEle() - getRoofHeight();
				
				renderSpindle(target, materialRoof,
						polygon.getOuter(),
						asList(roofY,
								roofY + 0.15 * roofHeight,
								roofY + 0.52 * roofHeight,
								roofY + 0.72 * roofHeight,
								roofY + 0.82 * roofHeight,
								roofY + 1.0 * roofHeight),
						asList(1.0, 0.8, 1.0, 0.7, 0.15, 0.0));
				
			}

			private void renderSpindle(
					Target<?> target, Material material,
					SimplePolygonXZ polygon,
					List<Double> heights, List<Double> scaleFactors) {
				
				checkArgument(heights.size() == scaleFactors.size(),
						"heights and scaleFactors must have same size");

				int numRings = heights.size();
				VectorXZ center = polygon.getCenter();
							
				@SuppressWarnings("unchecked")
				List<VectorXYZ>[] rings = new List[numRings];
				
				for (int i = 0; i < numRings; i++) {
					
					double y = heights.get(i);
					double scale = scaleFactors.get(i);
					
					if (scale == 0) {
						
						rings[i] = singletonList(center.xyz(y));
						
					} else {
						
						rings[i] = new ArrayList<VectorXYZ>();
						for (VectorXZ v : polygon.getVertexLoop()) {
							rings[i].add(interpolateBetween(center, v, scale).xyz(y));
						}
											
					}
					
				}
				
				for (int i = 0; i+1 < numRings; i++) {
					
					if (rings[i].size() > 1 && rings[i+1].size() > 1) {

						List<VectorXYZ> vs = new ArrayList<VectorXYZ>();
						
						for (int v = 0; v < rings[i].size(); v ++) {
							vs.add(rings[i].get(v));
							vs.add(rings[i+1].get(v));
						}
						
						target.drawTriangleStrip(material, vs);
						
					} else if (rings[i].size() == 1 && rings[i+1].size() > 1) {
						
						List<VectorXYZ> vs = new ArrayList<VectorXYZ>();
						vs.add(rings[i].get(0));
						vs.addAll(rings[i+1]);
						target.drawTriangleFan(material, vs);
						
					} else if (rings[i].size() > 1 && rings[i+1].size() == 1) {
						
						List<VectorXYZ> vs = new ArrayList<VectorXYZ>();
						vs.addAll(rings[i]);
						vs.add(rings[i+1].get(0));
						reverse(vs);
						target.drawTriangleFan(material, vs);
						
					}
					
					
				}
				
			}
			
		}
		
		/**
		 * superclass for roofs that have exactly one height value
		 * for each point within their XZ polygon
		 */
		public abstract class HeightfieldRoof extends TaggedRoof {

			/**
			 * returns segments within the roof polygon
			 * that define ridges or edges of the roof
			 */
			public abstract Collection<LineSegmentXZ> getInnerSegments();

			/**
			 * returns segments within the roof polygon
			 * that define apex nodes of the roof
			 */
			public abstract Collection<VectorXZ> getInnerPoints();
			
			/**
			 * returns roof elevation at a position.
			 * Only required to work for positions that are part of the
			 * polygon, segments or points for the roof.
			 * 
			 * @return  elevation, null if unknown
			 */
			protected abstract Double getRoofEleAt_noInterpolation(VectorXZ pos);
			
			@Override
			public double getRoofEleAt(VectorXZ v) {
				
				Double ele = getRoofEleAt_noInterpolation(v);
				
				if (ele != null) {
					return ele;
				} else {
					
					// get all segments from the roof
					
					//TODO (performance): avoid doing this for every node
					
					Collection<LineSegmentXZ> segments =
						new ArrayList<LineSegmentXZ>();
					
					segments.addAll(this.getInnerSegments());
					segments.addAll(this.getPolygon().getOuter().getSegments());
					for (SimplePolygonXZ hole : this.getPolygon().getHoles()) {
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
					
					return interpolateValue(v,
							closestSegment.p1,
							getRoofEleAt_noInterpolation(closestSegment.p1),
							closestSegment.p2,
							getRoofEleAt_noInterpolation(closestSegment.p2));
					
				}
			}
			
			@Override
			public void renderTo(Target<?> target) {
				
				Collection<TriangleXZ> triangles =
						JTSTriangulationUtil.triangulate(
								getPolygon().getOuter(),
								getPolygon().getHoles(),
								getInnerSegments(),
								getInnerPoints());
				
				List<TriangleXYZ> trianglesXYZ =
						new ArrayList<TriangleXYZ>(triangles.size());
				
				for (TriangleXZ triangle : triangles) {
					TriangleXZ tCCW = triangle.makeCounterclockwise();
					trianglesXYZ.add(new TriangleXYZ(
							withRoofEle(tCCW.v1),
							withRoofEle(tCCW.v2),
							withRoofEle(tCCW.v3)));
					//TODO: avoid duplicate objects for points in more than one triangle
				}
				
				target.drawTriangles(materialRoof, trianglesXYZ);
					
			}
			
			private VectorXYZ withRoofEle(VectorXZ v) {
				return v.xyz(getRoofEleAt(v));
			}
			
		}
		
		private class FlatRoof extends HeightfieldRoof {
				
			@Override
			public PolygonWithHolesXZ getPolygon() {
				return polygon;
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
			public Double getRoofEleAt_noInterpolation(VectorXZ pos) {
				return getMaxRoofEle();
			}
			
			@Override
			public double getMaxRoofEle() {
				return building.getArea().getElevationProfile().getMinEle()
						+ heightWithoutRoof;
			}
			
		}
		
		private class PyramidalRoof extends HeightfieldRoof {
			
			private final VectorXZ apex;
			private final List<LineSegmentXZ> innerSegments;
			
			public PyramidalRoof() {
			
				super();
				
				SimplePolygonXZ outerPoly = polygon.getOuter();
				
				apex = outerPoly.getCentroid();
				
				innerSegments = new ArrayList<LineSegmentXZ>();
				for (VectorXZ v : outerPoly.getVertices()) {
					innerSegments.add(new LineSegmentXZ(v, apex));
				}
				
			}
			
			@Override
			public PolygonWithHolesXZ getPolygon() {
				return polygon;
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
			public Double getRoofEleAt_noInterpolation(VectorXZ pos) {
				if (apex.equals(pos)) {
					return getMaxRoofEle();
				} else if (polygon.getOuter().getVertices().contains(pos)) {
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
		abstract private class RoofWithRidge extends HeightfieldRoof {
			
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
			public RoofWithRidge(double relativeRoofOffset) {
			
				super();
				
				SimplePolygonXZ outerPoly = polygon.getOuter();
				
				SimplePolygonXZ simplifiedPolygon =
					outerPoly.getSimplifiedPolygon();
				
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
				
				VectorXZ p1 = outerPoly.getCentroid();
				
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
				
				for (VectorXZ v : outerPoly.getVertices()) {
					maxDistance = max (maxDistance,
							distanceFromLineSegment(v, ridge));
				}
				
				maxDistanceToRidge = maxDistance;
				
			}
			
		}
		
		private class GabledRoof extends RoofWithRidge {

			public GabledRoof() {
				super(0);
			}
			
			@Override
			public PolygonWithHolesXZ getPolygon() {
				
				PolygonXZ newOuter = polygon.getOuter();
				
				newOuter = insertIntoPolygon(newOuter, ridge.p1, 0.2);
				newOuter = insertIntoPolygon(newOuter, ridge.p2, 0.2);
				
				return new PolygonWithHolesXZ(
						newOuter.asSimplePolygon(),
						polygon.getHoles());
				
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
			public Double getRoofEleAt_noInterpolation(VectorXZ pos) {
				double distRidge = distanceFromLineSegment(pos, ridge);
				double relativePlacement = distRidge / maxDistanceToRidge;
				return getMaxRoofEle() - roofHeight * relativePlacement;
			}
			
		}
		
		private class HippedRoof extends RoofWithRidge {

			public HippedRoof() {
				super(1/3.0);
			}
			
			@Override
			public PolygonWithHolesXZ getPolygon() {
				return polygon;
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
			public Double getRoofEleAt_noInterpolation(VectorXZ pos) {
				if (ridge.p1.equals(pos) || ridge.p2.equals(pos)) {
					return getMaxRoofEle();
				} else if (getPolygon().getOuter().getVertexLoop().contains(pos)) {
					return getMaxRoofEle() - roofHeight;
				} else {
					return null;
				}
			}
			
		}
		
		private class HalfHippedRoof extends RoofWithRidge {

			private final LineSegmentXZ cap1part, cap2part;
			
			public HalfHippedRoof() {
				
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
				
				PolygonXZ newOuter = polygon.getOuter();
				
				newOuter = insertIntoPolygon(newOuter, cap1part.p1, 0.2);
				newOuter = insertIntoPolygon(newOuter, cap1part.p2, 0.2);
				newOuter = insertIntoPolygon(newOuter, cap2part.p1, 0.2);
				newOuter = insertIntoPolygon(newOuter, cap2part.p2, 0.2);
				
				return new PolygonWithHolesXZ(
						newOuter.asSimplePolygon(),
						polygon.getHoles());
				
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
			public Double getRoofEleAt_noInterpolation(VectorXZ pos) {
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
		
		private class GambrelRoof extends RoofWithRidge {

			private final LineSegmentXZ cap1part, cap2part;
			
			public GambrelRoof() {
				
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
				
				PolygonXZ newOuter = polygon.getOuter();

				newOuter = insertIntoPolygon(newOuter, ridge.p1, 0.2);
				newOuter = insertIntoPolygon(newOuter, ridge.p2, 0.2);
				newOuter = insertIntoPolygon(newOuter, cap1part.p1, 0.2);
				newOuter = insertIntoPolygon(newOuter, cap1part.p2, 0.2);
				newOuter = insertIntoPolygon(newOuter, cap2part.p1, 0.2);
				newOuter = insertIntoPolygon(newOuter, cap2part.p2, 0.2);
				
				//TODO: add intersections of additional edges with outline?
				
				return new PolygonWithHolesXZ(
						newOuter.asSimplePolygon(),
						polygon.getHoles());
				
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
			public Double getRoofEleAt_noInterpolation(VectorXZ pos) {

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
		
		private class MansardRoof extends RoofWithRidge {

			private final LineSegmentXZ mansardEdge1, mansardEdge2;
			
			public MansardRoof() {
				
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
				return polygon;
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
			public Double getRoofEleAt_noInterpolation(VectorXZ pos) {

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
		private class ComplexRoof extends HeightfieldRoof {
						
			private double roofHeight = 0;
			private final Map<VectorXZ, Double> roofHeightMap;
			
			private final Collection<LineSegmentXZ> ridgeAndEdgeSegments;
			
			public ComplexRoof() {

				/* find ridge and/or edges
				 * (apex nodes don't need to be handled separately
				 *  as they should always be part of an edge segment) */
				
				roofHeightMap = new HashMap<VectorXZ, Double>();
				
				ridgeAndEdgeSegments = new ArrayList<LineSegmentXZ>();
				
				for (MapOverlap<?,?> overlap : area.getOverlaps()) {
					
					if (overlap instanceof MapOverlapWA) {
						
						MapWaySegment waySegment = ((MapOverlapWA)overlap).e1;
						
						if (!polygon.contains(waySegment.getCenter())) {
							continue;
						}
						
						boolean isRidge = waySegment.getTags().contains("roof:ridge", "yes");
						boolean isEdge = waySegment.getTags().contains("roof:edge", "yes");
												
						if (isRidge || isEdge) {
							
							ridgeAndEdgeSegments.add(waySegment.getLineSegment());
							
							for (MapNode node : waySegment.getStartEndNodes()) {
								
								// height of node (above roof base)
								Float nodeHeight = null;
								
								if (node.getTags().containsKey("roof:height")) {
									nodeHeight = parseMeasure(
											node.getTags().getValue("roof:height"));
								} else if (waySegment.getTags().containsKey("roof:height")) {
									nodeHeight = parseMeasure(
											waySegment.getTags().getValue("roof:height"));
								} else if (node.getTags().contains("roof:apex", "yes")) {
									nodeHeight = DEFAULT_RIDGE_HEIGHT;
								} else if (isRidge) {
									nodeHeight = DEFAULT_RIDGE_HEIGHT;
								}
								
								if (nodeHeight != null) {
									
									roofHeightMap.put(node.getPos(), (double)nodeHeight);
									
									roofHeight = max(roofHeight, nodeHeight);
									
								}
								
							}
							
						}
						
					}
					
				}
				
				/* add heights for outline nodes that don't have one yet */
				
				for (VectorXZ v : polygon.getOuter().getVertices()) {
					if (!roofHeightMap.containsKey(v)) {
						roofHeightMap.put(v, 0.0);
					}
				}
				
				for (SimplePolygonXZ hole : polygon.getHoles()) {
					for (VectorXZ v : hole.getVertices()) {
						if (!roofHeightMap.containsKey(v)) {
							roofHeightMap.put(v, 0.0);
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
				return polygon;
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
			public Double getRoofEleAt_noInterpolation(VectorXZ pos) {
				if (roofHeightMap.containsKey(pos)) {
					return building.getArea().getElevationProfile().getMinEle()
						+ heightWithoutRoof + roofHeightMap.get(pos);
				} else {
					return null;
				}
			}
			
			@Override
			public double getMaxRoofEle() {
				return building.getArea().getElevationProfile().getMinEle()
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
		
		private final BuildingPart buildingPart;
		private final MapNode node;
		
		public BuildingEntrance(BuildingPart buildingPart, MapNode node) {
			this.buildingPart = buildingPart;
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
				buildingPart.polygon.getPolygons()) {
				
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