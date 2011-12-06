package org.osm2world.core.terrain.creation;

import static org.osm2world.core.math.AxisAlignedBoundingBoxXZ.union;
import static org.osm2world.core.util.FaultTolerantIterationUtil.iterate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osm2world.core.heightmap.data.CellularTerrainElevation;
import org.osm2world.core.heightmap.data.TerrainElevationCell;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_elevation.data.ElevationProfile;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.InvalidGeometryException;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.datastructures.IntersectionGrid;
import org.osm2world.core.math.datastructures.IntersectionTestObject;
import org.osm2world.core.terrain.data.EmptyCellTerrainPatch;
import org.osm2world.core.terrain.data.GenericTerrainPatch;
import org.osm2world.core.terrain.data.Terrain;
import org.osm2world.core.terrain.data.TerrainPatch;
import org.osm2world.core.util.FaultTolerantIterationUtil.Operation;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.data.WorldObject;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.vividsolutions.jts.geom.TopologyException;

/**
 * creates {@link Terrain} based on a {@link MapData} with elevation information
 * and {@link CellularTerrainElevation}.
 */
public class TerrainCreator {

	public Terrain createTerrain(MapData grid, CellularTerrainElevation eleData) {

		/* find the terrain boundaries and ele info for each cell and
		 * those cells that are completely within a terrain boundary
		 * (and will not be drawn) */
		
		Multimap<TerrainElevationCell, TerrainBoundaryWorldObject>
			terrainBoundaryMap = HashMultimap.create();
		
		Multimap<TerrainElevationCell, VectorXYZ>
			unconnectedEleMap = HashMultimap.create();

		Set<TerrainElevationCell> ignoredCells
			= new HashSet<TerrainElevationCell>();
		
		// perform intersection tests for pairs of a cell and a boundary
		// that are in the same cell of the intersection grid

		IntersectionGrid speedupGrid = prepareSpeedupGrid(grid, eleData);

		for (Collection<IntersectionTestObject> intersectionCell : speedupGrid.getCells()) {

			Iterable<TerrainElevationCell> terrainCells =
				Iterables.filter(intersectionCell, TerrainElevationCell.class);
			Iterable<WorldObject> worldObjects =
				Iterables.filter(intersectionCell, WorldObject.class);
			
			for (TerrainElevationCell terrainCell : terrainCells) {
			for (WorldObject worldObject : worldObjects) {
				
				if (worldObject instanceof TerrainBoundaryWorldObject) {
					
					TerrainBoundaryWorldObject terrainBoundary =
						(TerrainBoundaryWorldObject)worldObject;
					
					if (terrainBoundaryMap.containsEntry(
							terrainCell, terrainBoundary)) {
						continue;
					} // elements can be in more than 1 cell together,
					  // so the intersection might have been handled before
					
					SimplePolygonXZ cellPolyXZ = terrainCell.getPolygonXZ();
					PolygonXYZ outlinePolygon = terrainBoundary.getOutlinePolygon();
					
					if (outlinePolygon != null
							&& outlinePolygon.getXZPolygon().isSimple()) {
						
						SimplePolygonXZ outlinePolygonXZ = outlinePolygon.getSimpleXZPolygon();
						
						if (cellPolyXZ.contains(outlinePolygonXZ)
								|| cellPolyXZ.intersects(outlinePolygonXZ)) {
							
							terrainBoundaryMap.put(terrainCell, terrainBoundary);
							
						} else if (outlinePolygonXZ.contains(cellPolyXZ)) {
							
							ignoredCells.add(terrainCell);
							
						}
						
					}
					
				} else {
					
					// no boundary, but ele data can be relevant
					
					ElevationProfile eleProfile = worldObject
							.getPrimaryMapElement().getElevationProfile();
					
					unconnectedEleMap.putAll(terrainCell,
							eleProfile.getPointsWithEle());
					
				}
				
			}
			}
			
		}
						
		/* create terrain patches and terrain */
		
		Collection<TerrainPatch> patches = generateTerrainPatches(
				eleData.getCells(), terrainBoundaryMap,
				unconnectedEleMap, ignoredCells);
				
		finishTerrainPatches(patches);
				
		return new Terrain(patches);
		
	}

	/**
	 * creates an IntersectionGrid with all terrain cells and boundaries
	 */
	private IntersectionGrid prepareSpeedupGrid(
			MapData mapData, CellularTerrainElevation eleData) {
		
		AxisAlignedBoundingBoxXZ speedupGridBounds = union(
				mapData.getDataBoundary(),
				new AxisAlignedBoundingBoxXZ(
						eleData.getBoundaryPolygonXZ().getVertexCollection()));
				
		final IntersectionGrid speedupGrid = new IntersectionGrid(
				speedupGridBounds.pad(20),
				50, 50); //TODO (performance): choose appropriate cell size params
		
		for (TerrainElevationCell cell : eleData.getCells()) {
			speedupGrid.insert(cell);
		}
		
		/* add all IntersectionTestObjects to the speedupGrid */
		
		iterate(mapData.getWorldObjects(IntersectionTestObject.class),
			new Operation<IntersectionTestObject>() {
				@Override public void perform(IntersectionTestObject object) {
					
					if (((WorldObject)object).getGroundState()
							== GroundState.ON) {
						
						speedupGrid.insert(object);
						
					}
					
				}
			});
		
		return speedupGrid;
	}

	private static Collection<TerrainPatch> generateTerrainPatches(
			Iterable<? extends TerrainElevationCell> terrainCells,
			Multimap<TerrainElevationCell, TerrainBoundaryWorldObject> terrainBoundaryMap,
			Multimap<TerrainElevationCell, VectorXYZ> unconnectedEleMap,
			Set<TerrainElevationCell> ignoredCells) {
		
		Collection<TerrainPatch> patches = new ArrayList<TerrainPatch>();
		
		for (TerrainElevationCell cell : terrainCells) {
			
			if (ignoredCells.contains(cell)) continue; //TODO: only correct until inner polys of mulitpolys etc. are used!
			
			Collection<TerrainBoundaryWorldObject> terrainBoundarys =
				terrainBoundaryMap.get(cell);
			
			Collection<VectorXYZ> unconnectedEles =
				unconnectedEleMap.get(cell);
						
			// handle the common "empty cell" special case
			// in a more efficient way
			
			if (terrainBoundarys.isEmpty() && unconnectedEles.isEmpty()) {
				
				patches.add(new EmptyCellTerrainPatch(cell));
				
			} else {
				
				addPatchesForCell(patches, cell,
						terrainBoundarys, unconnectedEles);
			
			}
		
		}
		
		return patches;
	}

	/**
	 * creates the necessary {@link TerrainPatch}es for a terrain cell
	 * and adds them to a collection of {@link TerrainPatch}es
	 * 
	 * @param patches  collection to add the new patches to
	 */
	private static void addPatchesForCell(Collection<TerrainPatch> patches,
			TerrainElevationCell cell,
			Collection<TerrainBoundaryWorldObject> terrainBoundarys,
			Collection<VectorXYZ> unconnectedEles) {
		
		PolygonXYZ cellPoly = cell.getPolygonXYZ();
		SimplePolygonXZ cellPolyXZ = cell.getPolygonXZ();
						
		/* prepare elevation information and subtractPolysXZ list */
		
		TemporaryElevationStorage eleStorage = new TemporaryElevationStorage();
		List<SimplePolygonXZ> subtractPolysXZ = new ArrayList<SimplePolygonXZ>(terrainBoundarys.size());
		
		for (TerrainBoundaryWorldObject tb : terrainBoundarys) {
							
			PolygonXYZ tbPolygon = tb.getOutlinePolygon();
			SimplePolygonXZ tbPolygonXZ = tbPolygon.getSimpleXZPolygon();
			
			subtractPolysXZ.add(tbPolygonXZ);
			
			eleStorage.addPolygon(tbPolygonXZ, tbPolygon);
								
		}
		
		Collection<VectorXZ> elePoints = new ArrayList<VectorXZ>();
		
		for (VectorXYZ v : unconnectedEles) {
			VectorXZ vXZ = v.xz();
			elePoints.add(vXZ);
			eleStorage.addVector(vXZ, v);
		}
		
		eleStorage.addPolygon(cellPolyXZ, cellPoly);
									
		/* subtract terrain borders from the cell polygon */

		try {
			
			Collection<PolygonWithHolesXZ> remainingPolygons =
				CAGUtil.subtractPolygons(
						cellPoly.getSimpleXZPolygon(),
						subtractPolysXZ);
			
			/* create terrain patches */
			
			for (PolygonWithHolesXZ remainingPolygon : remainingPolygons) {
								
				Collection<VectorXZ> points = new ArrayList<VectorXZ>();
				
				for (VectorXZ elePoint : elePoints) {
					if (remainingPolygon.contains(elePoint)) {
						points.add(elePoint);
					}
				}
				
				patches.add(new GenericTerrainPatch(
					remainingPolygon, points, eleStorage));
				
			}
			
		} catch (InvalidGeometryException e) {
			//TODO (error handling)
			e.printStackTrace();
		} catch (TopologyException e) {
			//TODO (error handling)
			e.printStackTrace();
		}
		
	}
	
	private static void finishTerrainPatches(Collection<TerrainPatch> patches) {
		
		iterate(patches, new Operation<TerrainPatch>() {
			@Override public void perform(TerrainPatch patch) {
				patch.build();
			}
		});
		
	}
		
}
