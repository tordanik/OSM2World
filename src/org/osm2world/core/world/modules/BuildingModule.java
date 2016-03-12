package org.osm2world.core.world.modules;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.lang.Math.*;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.openstreetmap.josm.plugins.graphview.core.util.ValueStringParser.*;
import static org.osm2world.core.map_elevation.creation.EleConstraintEnforcer.ConstraintType.*;
import static org.osm2world.core.map_elevation.data.GroundState.*;
import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.*;
import static org.osm2world.core.target.common.material.TexCoordUtil.*;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_data.data.overlaps.MapOverlapWA;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.EleConnectorGroup;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.InvalidGeometryException;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.Poly2TriUtil;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.PolygonXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.algorithms.CAGUtil;
import org.osm2world.core.math.algorithms.JTSTriangulationUtil;
import org.osm2world.core.math.algorithms.TriangulationUtil;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.TextureData;
import org.osm2world.core.target.common.material.ImmutableMaterial;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.util.MinMaxUtil;
import org.osm2world.core.util.exception.TriangulationException;
import org.osm2world.core.world.data.AreaWorldObject;
import org.osm2world.core.world.data.NodeWorldObject;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.data.WaySegmentWorldObject;
import org.osm2world.core.world.data.WorldObjectWithOutline;
import org.osm2world.core.world.modules.common.ConfigurableWorldModule;
import org.osm2world.core.world.network.AbstractNetworkWaySegmentWorldObject;

import com.google.common.base.Function;

/**
 * adds buildings to the world
 */
public class BuildingModule extends ConfigurableWorldModule {
	
	@Override
	public void applyTo(MapData mapData) {
		
		boolean useBuildingColors = config.getBoolean("useBuildingColors", true);
		boolean drawBuildingWindows = config.getBoolean("drawBuildingWindows", true);
		
		for (MapArea area : mapData.getMapAreas()) {
			
			if (!area.getRepresentations().isEmpty()) continue;
			
			String buildingValue = area.getTags().getValue("building");
			
			if (buildingValue != null && !buildingValue.equals("no")) {
				
				Building building = new Building(area,
						useBuildingColors, drawBuildingWindows);
				area.addRepresentation(building);
								
			}
			
		}
		
	}
	
	public static class Building implements AreaWorldObject,
		WorldObjectWithOutline, RenderableToAllTargets {
		
		private final MapArea area;
		private final List<BuildingPart> parts =
				new ArrayList<BuildingModule.BuildingPart>();
		
		private final EleConnectorGroup outlineConnectors;
		
		public Building(MapArea area, boolean useBuildingColors,
				boolean drawBuildingWindows) {
			
			this.area = area;
			
			for (MapOverlap<?,?> overlap : area.getOverlaps()) {
				MapElement other = overlap.getOther(area);
				if (other instanceof MapArea
						&& other.getTags().containsKey("building:part")) {
					
					MapArea otherArea = (MapArea)other;
					
					//TODO: check whether the building contains the part (instead of just touching it)
					if (area.getPolygon().contains(
							otherArea.getPolygon().getOuter())) {
						parts.add(new BuildingPart(this, otherArea,
							otherArea.getPolygon(), useBuildingColors,
							drawBuildingWindows));
					}
					
				}
			}
			
			/* add part(s) for area not covered by building:part polygons */
			boolean isBuildingPart = false;
			if (area.getTags().containsKey("building:part"))
				isBuildingPart = !("no".equals(area.getTags().getValue("building:part")));
			
			if (parts.isEmpty() || isBuildingPart) {
				parts.add(new BuildingPart(this, area,
						area.getPolygon(), useBuildingColors, drawBuildingWindows));
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
					parts.add(new BuildingPart(this, area, remainingPoly,
							useBuildingColors, drawBuildingWindows));
				}
				
			}

			/* create connectors along the outline.
			 * Because the ground around buildings is not necessarily plane,
			 * they aren't directly used for ele, but instead their minimum.
			 */
			
			outlineConnectors = new EleConnectorGroup();
			outlineConnectors.addConnectorsFor(area.getPolygon(), null, ON);
			
		}

		public MapArea getArea() { //TODO: redundant because of getPrimaryMapElement
			return area;
		}
		
		public List<BuildingPart> getParts() {
			return parts;
		}

		@Override
		public MapArea getPrimaryMapElement() {
			return area;
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public EleConnectorGroup getEleConnectors() {
			return outlineConnectors;
		}

		@Override
		public void defineEleConstraints(EleConstraintEnforcer enforcer) {
			
			List<EleConnector> groundLevelEntrances = new ArrayList<EleConnector>();
			
			/* add constraints between entrances with different levels */
			
			for (BuildingPart part : parts) {
				
				// add vertical distances
				
				for (int i = 0; i < part.entrances.size(); i++) {
					
					BuildingEntrance e1 = part.entrances.get(i);
					
					for (int j = i+1; j < part.entrances.size(); j++) {
						
						BuildingEntrance e2 = part.entrances.get(j);
						
						double heightPerLevel = part.heightWithoutRoof / part.buildingLevels;
						
						if (e1.getLevel() > e2.getLevel()) {
							
							enforcer.requireVerticalDistance(EXACT,
									heightPerLevel * (e1.getLevel() - e2.getLevel()),
									e1.connector, e2.connector);
							
						} else if (e1.getLevel() < e2.getLevel()) {

							enforcer.requireVerticalDistance(EXACT,
									heightPerLevel * (e2.getLevel() - e1.getLevel()),
									e2.connector, e1.connector);
							
						}
																		
					}
					
					// collect entrances for next step
					
					if (e1.getLevel() == 0 && e1.getGroundState() == ON) {
						groundLevelEntrances.add(e1.connector);
					}
					
				}
			
			}
			
			/* make sure that a level=0 ground entrance is the building's lowest point */
			
			for (EleConnector outlineConnector : outlineConnectors) {
				for (EleConnector entranceConnector : groundLevelEntrances) {
					enforcer.requireVerticalDistance(
							MIN, 0, outlineConnector, entranceConnector);
				}
			}
			
		}
		
		//TODO
//		@Override
//		public double getClearingAbove(VectorXZ pos) {
//			double maxClearingAbove = 0;
//			for (BuildingPart part : parts) {
//				double clearing = part.getClearingAbove(pos);
//				maxClearingAbove = max(clearing, maxClearingAbove);
//			}
//			return maxClearingAbove;
//		}

		@Override
		public SimplePolygonXZ getOutlinePolygonXZ() {
			return area.getPolygon().getOuter().makeCounterclockwise();
		}

		public double getGroundLevelEle() {
			
			double minEle = POSITIVE_INFINITY;
			
			for (EleConnector c : outlineConnectors) {
				if (c.getPosXYZ().y < minEle) {
					minEle = c.getPosXYZ().y;
				}
			}
			
			return minEle;
			
		}

		@Override
		public PolygonXYZ getOutlinePolygon() {
			return getOutlinePolygonXZ().xyz(getGroundLevelEle());
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
		
		private int buildingLevels;
		private int minLevel;
		
		private double heightWithoutRoof;
		
		private Material materialWall;
		private Material materialWallWithWindows;
		private Material materialRoof;
		
		private List<BuildingEntrance> entrances = new ArrayList<BuildingEntrance>();
		
		private Roof roof;
		
		public BuildingPart(Building building,
				MapArea area, PolygonWithHolesXZ polygon,
				boolean useBuildingColors, boolean drawBuildingWindows) {

			this.building = building;
			this.area = area;
			this.polygon = polygon;

			setAttributes(useBuildingColors, drawBuildingWindows);
			
			for (MapNode node : area.getBoundaryNodes()) {
				if ((node.getTags().contains("building", "entrance")
						|| node.getTags().containsKey("entrance"))
						&& node.getRepresentations().isEmpty()) {
					
					BuildingEntrance entrance = new BuildingEntrance(this, node);
					entrances.add(entrance);
					node.addRepresentation(entrance);
					
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
				
			target.drawTriangles(materialWall, trianglesXYZ,
					triangleTexCoordLists(trianglesXYZ, materialWall, GLOBAL_X_Z));
			
		}

		private void renderWalls(Target<?> target, Roof roof) {
			
			double baseEle = building.getGroundLevelEle();
			
			double floorHeight = calculateFloorHeight(roof);
			boolean renderFloor = (floorHeight > 0);
			
			if (area.getOverlaps().isEmpty()) {
				
				renderWalls(target, roof.getPolygon(), false,
						baseEle, floorHeight, roof);
				
			} else {
							
				/* find terrain boundaries on the ground
				 * that overlap with the building */
				
				List<TerrainBoundaryWorldObject> tbWorldObjects = new ArrayList<TerrainBoundaryWorldObject>();
								
				for (MapOverlap<?,?> overlap : area.getOverlaps()) {
					MapElement other = overlap.getOther(area);
					if (other.getPrimaryRepresentation() instanceof TerrainBoundaryWorldObject
							&& other.getPrimaryRepresentation().getGroundState() == GroundState.ON
							&& (other.getTags().contains("tunnel", "passage")
									|| other.getTags().contains("tunnel", "building_passage"))) {
						tbWorldObjects.add((TerrainBoundaryWorldObject)
								other.getPrimaryRepresentation());
					}
				}
				
				/* render building parts where the building polygon does not overlap with terrain boundaries */
				
				List<SimplePolygonXZ> subtractPolygons = new ArrayList<SimplePolygonXZ>();
				
				for (TerrainBoundaryWorldObject o : tbWorldObjects) {
					
					SimplePolygonXZ subtractPoly = o.getOutlinePolygonXZ();
					
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
					renderWalls(target, p, false, baseEle, floorHeight, roof);
					if (renderFloor) {
						renderFloor(target, baseEle + floorHeight);
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
						double newFloorHeight = 3;
							//TODO restore clearing - o.getClearingAbove(o.getOutlinePolygon().getSimpleXZPolygon().getCenter());
						if (newFloorHeight < floorHeight) {
							newFloorHeight = floorHeight;
						}
						renderWalls(target, p, false, baseEle, newFloorHeight, roof);
						renderFloor(target, baseEle);
					}
					
				}
				
			}
				
		}

		private double calculateFloorHeight(Roof roof) {
			
			if (getValue("min_height") != null) {
				
				Float minHeight = parseMeasure(getValue("min_height"));
				if (minHeight != null) {
					return minHeight;
				}
				
			}
			
			if (minLevel > 0) {
				
				return (heightWithoutRoof / buildingLevels) * minLevel;
				
			}
			
			if (area.getTags().contains("building", "roof")
					|| area.getTags().contains("building:part", "roof")) {
				
				return heightWithoutRoof - 0.3;
				
			}
			
			return 0;
						
		}

		private void renderWalls(Target<?> target, PolygonWithHolesXZ p,
				boolean renderFloor, double baseEle, double floorHeight,
				Roof roof) {
			
			drawWallOnPolygon(target, baseEle, floorHeight,
					roof, p.getOuter().makeCounterclockwise());
			
			for (SimplePolygonXZ polygon : p.getHoles()) {
				drawWallOnPolygon(target, baseEle, floorHeight,
						roof, polygon.makeClockwise());
			}
			
		}

		private void drawWallOnPolygon(Target<?> target, double baseEle,
				double floorHeight, Roof roof, SimplePolygonXZ polygon) {
			
			double floorEle = baseEle + floorHeight;
			
			List<TextureData> textureDataList = materialWallWithWindows.getTextureDataList();
			List<VectorXZ> vertices = polygon.getVertexLoop();
			
			List<VectorXYZ> mainWallVectors = new ArrayList<VectorXYZ>(vertices.size() * 2);
			List<VectorXYZ> roofWallVectors = new ArrayList<VectorXYZ>(vertices.size() * 2);
			
			List<List<VectorXZ>> mainWallTexCoordLists = new ArrayList<List<VectorXZ>>(
					textureDataList.size());
			
			for (int texLayer = 0; texLayer < textureDataList.size(); texLayer ++) {
				mainWallTexCoordLists.add(new ArrayList<VectorXZ>());
			}
			
			double accumulatedLength = 0;
			double[] previousS = new double[textureDataList.size()];
			
			for (int i = 0; i < vertices.size(); i++) {
				
				final VectorXZ coord = vertices.get(i);
				
				/* update accumulated wall length */
				
				if (i > 0) {
					accumulatedLength += coord.distanceTo(vertices.get(i-1));
				}
				
				/* add wall vectors */
				
				final VectorXYZ upperVector = coord.xyz(roof.getRoofEleAt(coord));
				final VectorXYZ middleVector = coord.xyz(baseEle + heightWithoutRoof);
				
				double upperEle = upperVector.y;
				double middleEle = middleVector.y;
				
				mainWallVectors.add(middleVector);
				mainWallVectors.add(new VectorXYZ(coord.x,
						min(floorEle, middleEle), coord.z));
				
				roofWallVectors.add(upperVector);
				roofWallVectors.add(new VectorXYZ(coord.x,
						min(middleEle, upperEle), coord.z));
				
				
				/* add texture coordinates */
				
				for (int texLayer = 0; texLayer < textureDataList.size(); texLayer ++) {
					
					TextureData textureData = textureDataList.get(texLayer);
					List<VectorXZ> texCoordList = mainWallTexCoordLists.get(texLayer);
					
					double s, lowerT, middleT;
					
					// determine s (width dimension) coordinate
					
					if (textureData.height > 0) {
						s = accumulatedLength / textureData.width;
					} else {
						if (i == 0) {
							s = 0;
						} else {
							s = previousS[texLayer] + round(vertices.get(i-1)
									.distanceTo(coord) / textureData.width);
						}
					}
					
					previousS[texLayer] = s;
					
					// determine t (height dimension) coordinates
					
					if (textureData.height > 0) {
						
						lowerT = (floorEle - baseEle) / textureData.height;
						middleT = (middleEle - baseEle) / textureData.height;
						
					} else {
						
						lowerT = buildingLevels *
							(floorEle - baseEle) / (middleEle - baseEle);
						middleT = buildingLevels;
						
					}
					
					// set texture coordinates
					
					texCoordList.add(new VectorXZ(s, middleT));
					texCoordList.add(new VectorXZ(s, lowerT));
					
				}
								
			}

			target.drawTriangleStrip(materialWallWithWindows, mainWallVectors,
					mainWallTexCoordLists);
			target.drawTriangleStrip(materialWall, roofWallVectors,
					texCoordLists(roofWallVectors, materialWall, STRIP_WALL));
			
		}
		
		/**
		 * sets the building part attributes (height, colors) depending on
		 * the building's and building part's tags.
		 * If available, explicitly tagged data is used,
		 * with tags of the building part overriding building tags.
		 * Otherwise, the values depend on indirect assumptions
		 * (level height) or ultimately the building class as determined
		 * by the "building" key.
		 */
		private void setAttributes(boolean useBuildingColors,
				boolean drawBuildingWindows) {
			
			TagGroup tags = area.getTags();
			TagGroup buildingTags = building.area.getTags();
			
			/* determine defaults for building type */
			
			int defaultLevels = 3;
			double defaultHeightPerLevel = 2.5;
			Material defaultMaterialWall = Materials.BUILDING_DEFAULT;
			Material defaultMaterialRoof = Materials.ROOF_DEFAULT;
			Material defaultMaterialWindows = Materials.BUILDING_WINDOWS;
			String defaultRoofShape = "flat";
			
			String buildingValue = getValue("building");
			
			if ("greenhouse".equals(buildingValue)) {
				defaultLevels = 1;
				defaultMaterialWall = Materials.GLASS;
				defaultMaterialRoof = Materials.GLASS_ROOF;
				defaultMaterialWindows = null;
			} else if ("garage".equals(buildingValue)
					|| "garages".equals(buildingValue)) {
				defaultLevels = 1;
				defaultMaterialWall = Materials.CONCRETE;
				defaultMaterialRoof = Materials.CONCRETE;
				defaultMaterialWindows = Materials.GARAGE_DOORS;
			} else if ("hut".equals(buildingValue)
					|| "shed".equals(buildingValue)) {
				defaultLevels = 1;
			} else if ("cabin".equals(buildingValue)) {
				defaultLevels = 1;
				defaultMaterialWall = Materials.WOOD_WALL;
				defaultMaterialRoof = Materials.WOOD;
			} else if ("roof".equals(buildingValue)) {
				defaultLevels = 1;
				defaultMaterialWindows = null;
			} else if ("church".equals(buildingValue)
					|| "hangar".equals(buildingValue)
					|| "industrial".equals(buildingValue)) {
				defaultMaterialWindows = null;
			} else {
				if (getValue("building:levels") == null) {
					defaultMaterialWindows = null;
				}
			}
                        
			if ("multi-storey".equals(getValue("parking"))) {
				defaultLevels = 5;
				defaultMaterialWindows = null;
			}
			
			/* determine levels */
			
			buildingLevels = defaultLevels;
			
			Float parsedLevels = null;
			
			if (getValue("building:levels") != null) {
				parsedLevels = parseOsmDecimal(
						getValue("building:levels"), false);
			}
			
			if (parsedLevels != null) {
				buildingLevels = (int)(float)parsedLevels;
			} else if (parseHeight(tags, parseHeight(buildingTags, -1)) > 0) {
				buildingLevels = max(1, (int)(parseHeight(tags, parseHeight(
						buildingTags, -1)) / defaultHeightPerLevel));
			}
			
			minLevel = 0;
			
			if (getValue("building:min_level") != null) {
				Float parsedMinLevel = parseOsmDecimal(
						getValue("building:min_level"), false);
				if (parsedMinLevel != null) {
					minLevel = (int)(float)parsedMinLevel;
				}
			}
			
			/* determine roof shape */
			
			boolean explicitRoofTagging = true;
			
			if (!("no".equals(area.getTags().getValue("roof:lines"))) && hasComplexRoof(area)) {
				roof = new ComplexRoof();
			} else {
				
				String roofShape = getValue("roof:shape");
				if (roofShape == null) { roofShape = getValue("building:roof:shape"); }
				
				if (roofShape == null) {
					roofShape = defaultRoofShape;
					explicitRoofTagging = false;
				}
				
				try {
					
					if ("pyramidal".equals(roofShape)) {
						roof = new PyramidalRoof();
					} else if ("onion".equals(roofShape)) {
						roof = new OnionRoof();
					} else if ("skillion".equals(roofShape)) {
						roof = new SkillionRoof();
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
					} else if ("dome".equals(roofShape)) {
						roof = new DomeRoof();
					} else if ("round".equals(roofShape)) {
						roof = new RoundRoof();
					} else {
						roof = new FlatRoof();
					}
					
				} catch (InvalidGeometryException e) {
					System.err.println("falling back to FlatRoof: " + e);
					roof = new FlatRoof();
					explicitRoofTagging = false;
				}
				
			}
			
			/* determine height */
			
			double fallbackHeight = buildingLevels * defaultHeightPerLevel;
			fallbackHeight += roof.getRoofHeight();
			
			fallbackHeight = parseHeight(buildingTags, (float)fallbackHeight);
			
			double height = parseHeight(tags, (float)fallbackHeight);

			// Make sure buildings have at least some height
			height = Math.max(height, 0.001);
                        
			heightWithoutRoof = height - roof.getRoofHeight();
			
			/* determine materials */
		    
		    if (defaultMaterialRoof == Materials.ROOF_DEFAULT
		    		&& explicitRoofTagging && roof instanceof FlatRoof) {
		    	defaultMaterialRoof = Materials.CONCRETE;
		    }
		    
		    if (useBuildingColors) {
		    	
		    	materialWall = buildMaterial(
		    			getValue("building:material"),
		    			getValue("building:colour"),
		    			defaultMaterialWall, false);
		    	materialRoof = buildMaterial(
		    			getValue("roof:material"),
		    			getValue("roof:colour"),
		    			defaultMaterialRoof, true);
		    	
		    } else {
		    	
		    	materialWall = defaultMaterialWall;
		    	materialRoof = defaultMaterialRoof;
		    	
		    }
		    
		    if (materialWall == Materials.GLASS) {
				// avoid placing windows into a glass front
				// TODO: the == currently only works if GLASS is not colorable
				defaultMaterialWindows = null;
		    }
		    
		    materialWallWithWindows = materialWall;
		    
		    if (drawBuildingWindows) {

		    	Material materialWindows = defaultMaterialWindows;
		    	
		    	if (materialWindows != null) {
		    		
		    		materialWallWithWindows = materialWallWithWindows.
		    				withAddedLayers(materialWindows.getTextureDataList());
			    	
		    	}
		    	
		    }
		    
		}
		
		private Material buildMaterial(String materialString,
				String colorString, Material defaultMaterial,
				boolean roof) {
			
			Material material = defaultMaterial;
			
			if (materialString != null) {
				if ("brick".equals(materialString)) {
					material = Materials.BRICK;
				} else if ("glass".equals(materialString)) {
					material = roof ? Materials.GLASS_ROOF : Materials.GLASS;
				} else if ("wood".equals(materialString)) {
					material = Materials.WOOD_WALL;
				} else if (Materials.getSurfaceMaterial(materialString) != null) {
					material = Materials.getSurfaceMaterial(materialString);
				}
			}
			
			boolean colorable = material.getNumTextureLayers() == 0
					|| material.getTextureDataList().get(0).colorable;
			
			if (colorString != null && colorable) {
				
				Color color;
				
				if ("white".equals(colorString)) {
					color = new Color(240, 240, 240);
				} else if ("black".equals(colorString)) {
					color = new Color(76, 76, 76);
				} else if ("grey".equals(colorString) || "gray".equals(colorString)) {
					color = new Color(100, 100, 100);
				} else if ("red".equals(colorString)) {
					if (roof) {
						color = new Color(204, 0, 0);
					} else {
						color = new Color(255, 190, 190);
					}
				} else if ("green".equals(colorString)) {
					if (roof) {
						color = new Color(150, 200, 130);
					} else {
						color = new Color(190, 255, 190);
					}
				} else if ("blue".equals(colorString)) {
					if (roof) {
						color = new Color(100, 50, 200);
					} else {
						color = new Color(190, 190, 255);
					}
				} else if ("yellow".equals(colorString)) {
					color = new Color(255, 255, 175);
				} else if ("pink".equals(colorString)) {
					color = new Color(225, 175, 225);
				} else if ("orange".equals(colorString)) {
					color = new Color(255, 225, 150);
				} else if ("brown".equals(colorString)) {
					if (roof) {
						color = new Color(120, 110, 110);
					} else {
						color = new Color(170, 130, 80);
					}
				} else {
					color = parseColor(colorString);
				}
				
				if (color != null) {
					material = new ImmutableMaterial(
							material.getInterpolation(), color,
							material.getAmbientFactor(),
							material.getDiffuseFactor(),
							material.getSpecularFactor(),
							material.getShininess(),
							material.getTransparency(),
							material.getShadow(),
							material.getAmbientOcclusion(),
							material.getTextureDataList());
				}
				
			}
			
			return material;
			
		}
		
		/**
		 * returns the value for a key from the building part's tags or the
		 * building's tags (if the part doesn't have a tag with this key)
		 */
		private String getValue(String key) {

			if (area.getTags().containsKey(key)) {
				return area.getTags().getValue(key);
			} else {
				return building.area.getTags().getValue(key);
			}
			
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
			
			/*
			 * default roof height if no value is tagged explicitly.
			 * Can optionally be overwritten by subclasses.
			 */
			protected float getDefaultRoofHeight() {
				if (buildingLevels == 1) {
					return 1;
				} else {
					return DEFAULT_RIDGE_HEIGHT;
				}
			}
			
			TaggedRoof() {
				
				Float taggedHeight = null;
				
				if (area.getTags().containsKey("roof:height")) {
					String valueString = getValue("roof:height");
					taggedHeight = parseMeasure(valueString);
				} else if (getValue("roof:levels") != null) {
					try {
						taggedHeight = 2.5f * Integer.parseInt(getValue("roof:levels"));
					} catch (NumberFormatException e) {}
				}
				
				roofHeight =
					taggedHeight != null ? taggedHeight : getDefaultRoofHeight();
				
			}

			@Override
			public double getRoofHeight() {
				return roofHeight;
			}
			
			@Override
			public double getMaxRoofEle() {
				return building.getGroundLevelEle() +
						heightWithoutRoof + roofHeight;
			}
			
		}
		
		private abstract class SpindleRoof extends TaggedRoof {

			@Override
			public PolygonWithHolesXZ getPolygon() {
				return polygon;
			}

			@Override
			public double getRoofEleAt(VectorXZ pos) {
				return getMaxRoofEle() - getRoofHeight();
			}
			
			protected void renderSpindle(
					Target<?> target, Material material,
					SimplePolygonXZ polygon,
					List<Double> heights, List<Double> scaleFactors) {
				
				checkArgument(heights.size() == scaleFactors.size(),
						"heights and scaleFactors must have same size");
				
				int numRings = heights.size();
				VectorXZ center = polygon.getCenter();
				
				/* calculate the vertex rings */
				
				@SuppressWarnings("unchecked")
				List<VectorXYZ>[] rings = new List[numRings];

				for (int i = 0; i < numRings; i++) {
					
					double y = heights.get(i);
					double scale = scaleFactors.get(i);
					
					if (scale == 0) {
						
						rings[i] = nCopies(polygon.size() + 1, center.xyz(y));
						
					} else {
						
						rings[i] = new ArrayList<VectorXYZ>();
						for (VectorXZ v : polygon.getVertexLoop()) {
							rings[i].add(interpolateBetween(center, v, scale).xyz(y));
						}
											
					}
					
				}
					
				/* draw the triangle strips (or fans) between the rings */
				
				List<List<VectorXZ>> texCoordData[] = spindleTexCoordLists(
						rings, polygon.getOutlineLength(), material);
				
				for (int i = 0; i+1 < numRings; i++) {
					
					List<VectorXYZ> vs = new ArrayList<VectorXYZ>();
										
					for (int v = 0; v < rings[i].size(); v ++) {
						vs.add(rings[i].get(v));
						vs.add(rings[i+1].get(v));
					}
												
					target.drawTriangleStrip(material, vs, texCoordData[i]);
					
				}
				
			}
			
			protected List<List<VectorXZ>>[] spindleTexCoordLists(
					List<VectorXYZ>[] rings, double polygonLength,
					Material material) {
				
				@SuppressWarnings("unchecked")
				List<List<VectorXZ>>[] result = new List[rings.length - 1];
				
				double accumulatedTexHeight = 0;
				
				for (int i = 0; i+1 < rings.length; i++) {
					
					double texHeight =
						rings[i].get(0).distanceTo(rings[i+1].get(0));
					
					List<TextureData> textureDataList =
						material.getTextureDataList();
					
					if (textureDataList.size() == 0) {
						
						result[i] = emptyList();
						
					} else if (textureDataList.size() == 1) {
						
						result[i] = singletonList(spindleTexCoordList(
								rings[i], rings[i+1], polygonLength,
								accumulatedTexHeight, textureDataList.get(0)));
						
					} else {
						
						result[i] = new ArrayList<List<VectorXZ>>();
						
						for (TextureData textureData : textureDataList) {
							result[i].add(spindleTexCoordList(
									rings[i], rings[i+1], polygonLength,
									accumulatedTexHeight, textureData));
						}
						
					}
					
					accumulatedTexHeight += texHeight;
					
				}
				
				return result;
				
			}

			private List<VectorXZ> spindleTexCoordList(
					List<VectorXYZ> lowerRing, List<VectorXYZ> upperRing,
					double polygonLength, double accumulatedTexHeight,
					TextureData textureData) {
				
				double textureRepeats = max(1,
						round(polygonLength / textureData.width));
				
				double texWidthSteps = textureRepeats / (lowerRing.size() - 1);
				double texHeight = lowerRing.get(0).distanceTo(upperRing.get(0));
				
				double texZ1 = accumulatedTexHeight / textureData.height;
				double texZ2 = (accumulatedTexHeight + texHeight) / textureData.height;
				
				VectorXZ[] texCoords = new VectorXZ[2 * lowerRing.size()];
				
				for (int i = 0; i < lowerRing.size(); i++) {
					texCoords[2*i] = new VectorXZ(i*texWidthSteps, -texZ1);
					texCoords[2*i+1] = new VectorXZ(i*texWidthSteps, -texZ2);
				}
				
				return asList(texCoords);
				
			}

			@Override
			protected float getDefaultRoofHeight() {
				return (float)polygon.getOuter().getDiameter() / 2;
			}
			
		}
		
		private class OnionRoof extends SpindleRoof {
			
			@Override
			public void renderTo(Target<?> target) {
				
				double roofY = getMaxRoofEle() - getRoofHeight();
				
				renderSpindle(target, materialRoof,
						polygon.getOuter().makeClockwise(),
						asList(roofY,
								roofY + 0.15 * roofHeight,
								roofY + 0.52 * roofHeight,
								roofY + 0.72 * roofHeight,
								roofY + 0.82 * roofHeight,
								roofY + 1.0 * roofHeight),
						asList(1.0, 0.8, 1.0, 0.7, 0.15, 0.0));
				
			}
			
			@Override
			protected float getDefaultRoofHeight() {
				return (float)polygon.getOuter().getDiameter();
			}
			
		}
		
		private class DomeRoof extends SpindleRoof {

			/**
			 * number of height rings to approximate the round dome shape
			 */
			private static final int HEIGHT_RINGS = 10;
						
			@Override
			public void renderTo(Target<?> target) {
				
				double roofY = getMaxRoofEle() - getRoofHeight();

				List<Double> heights = new ArrayList<Double>();
				List<Double> scales = new ArrayList<Double>();
				
				for (int ring = 0; ring < HEIGHT_RINGS; ++ring) {
					double relativeHeight = (double)ring / (HEIGHT_RINGS - 1);
					heights.add(roofY + relativeHeight * roofHeight);
					scales.add(sqrt(1.0 - relativeHeight * relativeHeight));
				}
				
				renderSpindle(target, materialRoof,
						polygon.getOuter().makeClockwise(),
						heights, scales);
				
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
				
				/* create the triangulation of the roof */
				
				Collection<TriangleXZ> triangles;
				
				try {
					
					triangles = Poly2TriUtil.triangulate(
							getPolygon().getOuter(),
						    getPolygon().getHoles(),
						    getInnerSegments(),
						    getInnerPoints());
				
				} catch (TriangulationException e) {
				
						triangles = JTSTriangulationUtil.triangulate(
								getPolygon().getOuter(),
								getPolygon().getHoles(),
								getInnerSegments(),
								getInnerPoints());
				
				}
				
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
								
				/* draw triangles */
				
				target.drawTriangles(materialRoof, trianglesXYZ,
						triangleTexCoordLists(trianglesXYZ,
								materialRoof, SLOPED_TRIANGLES));
				
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
				return building.getGroundLevelEle() + heightWithoutRoof;
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
		
		private class SkillionRoof extends HeightfieldRoof {
			
			private final LineSegmentXZ ridge;
			private final double roofLength;
			
			public SkillionRoof() {
				
				/* parse slope direction */
				
				VectorXZ slopeDirection = null;
				
				if (getValue("roof:direction") != null) {
					Float angle = parseAngle(
							getValue("roof:direction"));
					if (angle != null) {
						slopeDirection = VectorXZ.fromAngle(toRadians(angle));
					}
				}
				
				// fallback from roof:direction to roof:slope:direction
				if (slopeDirection == null
						&& getValue("roof:slope:direction") != null) {
					Float angle = parseAngle(
							getValue("roof:slope:direction"));
					if (angle != null) {
						slopeDirection = VectorXZ.fromAngle(toRadians(angle));
					}
				}
				
				if (slopeDirection != null) {
					
					SimplePolygonXZ simplifiedOuter =
							polygon.getOuter().getSimplifiedPolygon();
					
					/* find ridge by calculating the outermost intersections of
					 * the quasi-infinite slope "line" towards the centroid vector
					 * with segments of the polygon */
					
					VectorXZ center = simplifiedOuter.getCentroid();
					
					Collection<LineSegmentXZ> intersections =
							simplifiedOuter.intersectionSegments(new LineSegmentXZ(
								center.add(slopeDirection.mult(-1000)), center));
					
					LineSegmentXZ outermostIntersection = null;
					double distanceOutermostIntersection = -1;
					
					for (LineSegmentXZ i : intersections) {
						double distance = distanceFromLineSegment(center, i);
						if (distance > distanceOutermostIntersection) {
							outermostIntersection = i;
							distanceOutermostIntersection = distance;
						}
					}
					
					ridge = outermostIntersection;
					
					/* calculate maximum distance from ridge */
					
					double maxDistance = 0.1;
					
					for (VectorXZ v : polygon.getOuter().getVertexLoop()) {
						double distance = distanceFromLine(v, ridge.p1, ridge.p2);
						if (distance > maxDistance) {
							maxDistance = distance;
						}
					}
					
					roofLength = maxDistance;
					
				} else {
					
					ridge = null;
					roofLength = Double.NaN;
					
				}
				
			}
			
			@Override
			public PolygonWithHolesXZ getPolygon() {
				return polygon;
			}

			@Override
			public Collection<LineSegmentXZ> getInnerSegments() {
				return emptyList();
			}

			@Override
			public Collection<VectorXZ> getInnerPoints() {
				return emptyList();
			}

			@Override
			protected Double getRoofEleAt_noInterpolation(VectorXZ pos) {
				if (ridge == null) {
					return getMaxRoofEle();
				} else {
					double distance = distanceFromLineSegment(pos, ridge);
					double relativeDistance = distance / roofLength;
					return getMaxRoofEle() - relativeDistance * roofHeight;
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

				if (getValue("roof:direction") != null) {
					Float angle = parseAngle(
							getValue("roof:direction"));
					if (angle != null) {
						ridgeDirection = VectorXZ.fromAngle(toRadians(angle)).rightNormal();
					}
				}
				
				if (ridgeDirection == null && getValue("roof:ridge:direction") != null) {
					Float angle = parseAngle(
							getValue("roof:ridge:direction"));
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
					
					if (area.getTags().contains("roof:orientation", "across")) {
						ridgeDirection = ridgeDirection.rightNormal();
					}
					
				}
				
				/* calculate the two outermost intersections of the
				 * quasi-infinite ridge line with segments of the polygon */
				
				VectorXZ p1 = outerPoly.getCentroid();
				
				Collection<LineSegmentXZ> intersections =
					simplifiedPolygon.intersectionSegments(new LineSegmentXZ(
							p1.add(ridgeDirection.mult(-1000)),
							p1.add(ridgeDirection.mult(1000))
					));

				if (intersections.size() < 2) {
					throw new InvalidGeometryException(
							"cannot handle roof geometry for id "
									+ area.getOsmObject().id);
				}
				
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
		
		private class RoundRoof extends RoofWithRidge {
			private final static double ROOF_SUBDIVISION_METER = 2.5;
			
			private final List<LineSegmentXZ> capParts;
			private final int rings;
			private final double radius;
			
			public RoundRoof() {

				super(0);

				if (roofHeight < maxDistanceToRidge) {
					double squaredHeight = roofHeight * roofHeight;
					double squaredDist = maxDistanceToRidge * maxDistanceToRidge;
					double centerY =  (squaredDist - squaredHeight) / (2 * roofHeight);
					radius = sqrt(squaredDist + centerY * centerY);
				} else {
					radius = 0;
				}
				
				rings = (int)Math.max(3, roofHeight/ROOF_SUBDIVISION_METER);
				capParts = new ArrayList<LineSegmentXZ>(rings*2);
				// TODO: would be good to vary step size with slope
				float step = 0.5f / (rings + 1);
				for (int i = 1; i <= rings; i++) {
					capParts.add(new LineSegmentXZ(
							interpolateBetween(cap1.p1, cap1.p2, i * step),
							interpolateBetween(cap1.p1, cap1.p2, 1 - i * step)));

					capParts.add(new LineSegmentXZ(
							interpolateBetween(cap2.p1, cap2.p2, i * step),
							interpolateBetween(cap2.p1, cap2.p2, 1 - i * step)));
				}
			}

			@Override
			public PolygonWithHolesXZ getPolygon() {

				PolygonXZ newOuter = polygon.getOuter();

				newOuter = insertIntoPolygon(newOuter, ridge.p1, 0.2);
				newOuter = insertIntoPolygon(newOuter, ridge.p2, 0.2);

				for (LineSegmentXZ capPart : capParts){
					newOuter = insertIntoPolygon(newOuter, capPart.p1, 0.2);
					newOuter = insertIntoPolygon(newOuter, capPart.p2, 0.2);
				}

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

				List<LineSegmentXZ> innerSegments = new ArrayList<LineSegmentXZ>(rings * 2 + 1);
				innerSegments.add(ridge);
				for (int i = 0; i < rings * 2; i += 2) {
					LineSegmentXZ cap1part = capParts.get(i);
					LineSegmentXZ cap2part = capParts.get(i+1);
					innerSegments.add(new LineSegmentXZ(cap1part.p1, cap2part.p2));
					innerSegments.add(new LineSegmentXZ(cap1part.p2, cap2part.p1));
				}

				return innerSegments;
			}

			@Override
			public Double getRoofEleAt_noInterpolation(VectorXZ pos) {
				double distRidge = distanceFromLineSegment(pos, ridge);
				double ele;
				
				if (radius > 0) {
					double relativePlacement = distRidge / radius;
					ele = getMaxRoofEle() - radius
						+ sqrt(1.0 - relativePlacement * relativePlacement) * radius;
				} else {
					// This could be any interpolator
					double relativePlacement = distRidge / maxDistanceToRidge;
					ele = getMaxRoofEle() - roofHeight +
					(1 - (Math.pow(relativePlacement, 2.5))) * roofHeight;
				}
				
				return Math.max(ele, getMaxRoofEle() - roofHeight);
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
			private PolygonWithHolesXZ simplePolygon;
			private final Collection<LineSegmentXZ> ridgeAndEdgeSegments;
			
			public ComplexRoof() {

				/* find ridge and/or edges
				 * (apex nodes don't need to be handled separately
				 *  as they should always be part of an edge segment) */
				
				roofHeightMap = new HashMap<VectorXZ, Double>();
				Set<VectorXZ> nodeSet = new HashSet<VectorXZ>();
				
				ridgeAndEdgeSegments = new ArrayList<LineSegmentXZ>();
				
				List<MapNode> nodes = area.getBoundaryNodes();
				boolean usePartRoofHeight = false;
				
				if (area.getTags().containsKey("roof:height")){
					roofHeight = parseMeasure(area.getTags().getValue("roof:height"));
					usePartRoofHeight = true;
				} else
					roofHeight = DEFAULT_RIDGE_HEIGHT;
				
				List<MapWaySegment> edges = new ArrayList<MapWaySegment>();
				List<MapWaySegment> ridges = new ArrayList<MapWaySegment>();
				
				for (MapOverlap<?,?> overlap : area.getOverlaps()) {
					
					if (overlap instanceof MapOverlapWA) {
						
						MapWaySegment waySegment = ((MapOverlapWA)overlap).e1;
						
						boolean isRidge = waySegment.getTags().contains("roof:ridge", "yes");
						boolean isEdge = waySegment.getTags().contains("roof:edge", "yes");
						
						if (!(isRidge || isEdge))
							continue;
						
						boolean inside = polygon.contains(waySegment.getCenter());

						// check also endpoints as pnpoly algo is not reliable when
						// segment lies on the polygon edge
						boolean containsStart = nodes.contains(waySegment.getStartNode());
						boolean containsEnd = nodes.contains(waySegment.getEndNode());

						if (!inside && !(containsStart && containsEnd))
							continue;

						if (isEdge)
							edges.add(waySegment);
						else
							ridges.add(waySegment);
						
						ridgeAndEdgeSegments.add(waySegment.getLineSegment());
					}
				}

				for (MapWaySegment waySegment : edges){
					for (MapNode node : waySegment.getStartEndNodes()) {

						// height of node (above roof base)
						Float nodeHeight = null;

						if (node.getTags().containsKey("roof:height")) {
							nodeHeight = parseMeasure(node.getTags()
									.getValue("roof:height"));
						// hmm, shouldnt edges be interpolated? some seem to think they dont
						} else if (waySegment.getTags().containsKey("roof:height")) {
							nodeHeight = parseMeasure(waySegment.getTags()
							        .getValue("roof:height"));
						} else if (node.getTags().contains("roof:apex",	"yes")) {
							nodeHeight = (float)roofHeight;
						}

						if (nodeHeight == null) {
							nodeSet.add(node.getPos());
							continue;
						}
						
						roofHeightMap.put(node.getPos(), (double) nodeHeight);
						
						if (usePartRoofHeight)
							roofHeight = max(roofHeight, nodeHeight);
					}
				}
								
				for (MapWaySegment waySegment : ridges){
					// height of node (above roof base)
					Float nodeHeight = null;

					if (waySegment.getTags().containsKey("roof:height")) {
						nodeHeight = parseMeasure(waySegment.getTags()
								.getValue("roof:height"));
					} else {
						nodeHeight = (float) roofHeight;
					}

					if (usePartRoofHeight)
						roofHeight = max(roofHeight, nodeHeight);

					for (MapNode node : waySegment.getStartEndNodes())
							roofHeightMap.put(node.getPos(), (double) nodeHeight);
				}
				
				/* join colinear segments, but not the nodes that are connected to ridge/edges
				 * often there are nodes that are only added to join one building to another
				 * but these interfere with proper triangulation.
				 * TODO: do the same for holes */
				List<VectorXZ> vertices = polygon.getOuter().getVertexLoop();
				List<VectorXZ> simplified = new ArrayList<VectorXZ>();
				VectorXZ vPrev = vertices.get(vertices.size() - 2);
				
				for (int i = 0, size = vertices.size() - 1; i < size; i++ ){
					VectorXZ v = vertices.get(i);
					
					if (i == 0 || roofHeightMap.containsKey(v) || nodeSet.contains(v)) {
						simplified.add(v);
						vPrev = v;
						continue;
					}
					VectorXZ vNext = vertices.get(i + 1);
					LineSegmentXZ l = new LineSegmentXZ(vPrev, vNext);
					
					// TODO define as static somewhere: 10 cm tolerance
					if (distanceFromLineSegment(v, l) < 0.01){
						continue;
					}
					
					roofHeightMap.put(v, 0.0);
					simplified.add(v);
					vPrev = v;
				}
				
				if (simplified.size() > 2) {
					try{
						simplified.add(simplified.get(0));
						simplePolygon = new PolygonWithHolesXZ(new SimplePolygonXZ(simplified),
								polygon.getHoles());
					} catch (InvalidGeometryException e) {
						System.err.print(e.getMessage());
						simplePolygon = polygon;
					}
				} else
					simplePolygon = polygon;

				/* add heights for outline nodes that don't have one yet */

				for (VectorXZ v : simplePolygon.getOuter().getVertices()) {
					if (!roofHeightMap.containsKey(v)) {
						roofHeightMap.put(v, 0.0);
					}
				}
				
				for (SimplePolygonXZ hole : simplePolygon.getHoles()) {
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
				return simplePolygon;
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
					return building.getGroundLevelEle()
						+ heightWithoutRoof + roofHeightMap.get(pos);
				} else {
					return null;
				}
			}
			
			@Override
			public double getMaxRoofEle() {
				return building.getGroundLevelEle()
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
		
		private final EleConnector connector;
		
		public BuildingEntrance(BuildingPart buildingPart, MapNode node) {
			this.buildingPart = buildingPart;
			this.node = node;
			this.connector = new EleConnector(node.getPos(), node, getGroundState());
		}
		
		@Override
		public MapNode getPrimaryMapElement() {
			return node;
		}
		
		@Override
		public Iterable<EleConnector> getEleConnectors() {
			return singleton(connector);
		}

		@Override
		public void defineEleConstraints(EleConstraintEnforcer enforcer) {
			
			/* TODO for level != null and ABOVE/BELO, add vertical distance to ground */
						
		}
		
		@Override
		public GroundState getGroundState() {
			
			boolean onlyOn = true;
			boolean onlyAbove = true;
			boolean onlyBelow = true;
			
			for (MapWaySegment waySegment : node.getConnectedWaySegments()) {
				
				if (waySegment.getPrimaryRepresentation() instanceof
						AbstractNetworkWaySegmentWorldObject) {
					
					switch (waySegment.getPrimaryRepresentation().getGroundState()) {
					case ABOVE: onlyOn = false; onlyBelow = false; break;
					case BELOW: onlyOn = false; onlyAbove = false; break;
					case ON: onlyBelow = false; onlyAbove = false; break;
					}
					
				}
				
			}
			
			if (onlyOn) {
				return ON;
			} else if (onlyAbove) {
				return ABOVE;
			} else if (onlyBelow) {
				return BELOW;
			} else {
				return ON;
			}
			
		}
		
		public int getLevel() {

			try {
				return Integer.parseInt(node.getTags().getValue("level"));
			} catch (NumberFormatException e) {
				return 0;
			}
			
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			/* calculate a vector that points out of the building */
			
			VectorXZ outOfBuilding = VectorXZ.Z_UNIT;
			
			for (SimplePolygonXZ polygon :
				buildingPart.polygon.getPolygons()) {
				
				final List<VectorXZ> vs = polygon.getVertexLoop();
				int entranceI = vs.indexOf(node.getPos());
				
				if (entranceI != -1) {
					
					VectorXZ posBefore = vs.get((vs.size() + entranceI - 1) % vs.size());
					VectorXZ posAfter = vs.get((vs.size() + entranceI + 1) % vs.size());
					
					outOfBuilding = posBefore.subtract(posAfter).rightNormal();
					if (!polygon.isClockwise()) {
						outOfBuilding = outOfBuilding.invert();
					}
					
					break;
					
				}
				
			}
			
			/* draw the entrance as a box protruding from the building */
			
			VectorXYZ center = connector.getPosXYZ();
			
			float height = parseHeight(node.getTags(), 2);
			float width = parseWidth(node.getTags(), 1);
			
			target.drawBox(Materials.ENTRANCE_DEFAULT,
					center, outOfBuilding, height, width, 0.1);
			
		}
		
	}

}
