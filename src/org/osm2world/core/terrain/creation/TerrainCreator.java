package org.osm2world.core.terrain.creation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import static org.osm2world.core.util.FaultTolerantIterationUtil.*;

import org.osm2world.core.heightmap.data.CellularTerrainElevation;
import org.osm2world.core.heightmap.data.TerrainElevationCell;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.InvalidGeometryException;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.datastructures.IntersectionGrid;
import org.osm2world.core.math.datastructures.IntersectionTestObject;
import org.osm2world.core.terrain.data.EmptyCellTerrainPatch;
import org.osm2world.core.terrain.data.GenericTerrainPatch;
import org.osm2world.core.terrain.data.Terrain;
import org.osm2world.core.terrain.data.TerrainPatch;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;

import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.TopologyException;

/**
 * creates {@link Terrain} based on a {@link MapData} with elevation information
 * and {@link CellularTerrainElevation}.
 */
public class TerrainCreator {

	public Terrain createTerrain(MapData grid, CellularTerrainElevation eleData) {

		/* find the terrain boundaries for each cell and
		 * those cells that are completely within a terrain boundary
		 * (and will not be drawn) */
		
		Map<TerrainElevationCell, List<TerrainBoundaryWorldObject>> terrainBoundaryMap
			= new HashMap<TerrainElevationCell, List<TerrainBoundaryWorldObject>>();

		Set<TerrainElevationCell> ignoredCells
			= new HashSet<TerrainElevationCell>();
		
		for (TerrainElevationCell cell : eleData.getCells()) {
			terrainBoundaryMap.put(cell, new ArrayList<TerrainBoundaryWorldObject>(0));
		}

		// perform intersection tests for pairs of a cell and a boundary
		// that are in the same cell of the intersection grid

		IntersectionGrid speedupGrid = prepareSpeedupGrid(grid, eleData);

		for (Collection<IntersectionTestObject> intersectionCell : speedupGrid.getCells()) {

			Iterable<TerrainElevationCell> terrainCells = 
				Iterables.filter(intersectionCell, TerrainElevationCell.class);
			Iterable<TerrainBoundaryWorldObject> boundaries =
				Iterables.filter(intersectionCell, TerrainBoundaryWorldObject.class);

			for (TerrainElevationCell terrainCell : terrainCells) {
			for (TerrainBoundaryWorldObject terrainBoundary : boundaries) {

				if (terrainBoundaryMap.get(terrainCell).contains(terrainBoundary)) {
					continue;
				} // elements can be in more than 1 cell together,
				  // so the intersection might have been handled before

				SimplePolygonXZ cellPolyXZ = terrainCell.getPolygonXZ();
				PolygonXYZ outlinePolygon = terrainBoundary.getOutlinePolygon();

				if (outlinePolygon.getXZPolygon().isSimple()) {

					SimplePolygonXZ outlinePolygonXZ = outlinePolygon.getSimpleXZPolygon();

					if (cellPolyXZ.contains(outlinePolygonXZ)
							|| cellPolyXZ.intersects(outlinePolygonXZ)) {

						terrainBoundaryMap.get(terrainCell).add(terrainBoundary);

					} else if (outlinePolygonXZ.contains(cellPolyXZ)) {

						ignoredCells.add(terrainCell);

					}

				}

			}
			}

		}
						
		/* create terrain patches and terrain */
		
		Collection<TerrainPatch> patches = generateTerrainPatches(
				eleData.getCells(), terrainBoundaryMap, ignoredCells);
				
		finishTerrainPatches(patches);
				
		return new Terrain(patches);
		
	}

	/** 
	 * creates an IntersectionGrid with all terrain cells and boundaries
	 */
	private IntersectionGrid prepareSpeedupGrid(
			MapData grid, CellularTerrainElevation eleData) {
		
		final IntersectionGrid speedupGrid = new IntersectionGrid(
				grid.getBoundary().pad(20),
				50, 50); //TODO (performance): choose appropriate cell size params

		for (TerrainElevationCell cell : eleData.getCells()) {
			speedupGrid.insert(cell);
		}
		
		/* add all TerrainBoundaryWorldObjects to the speedupGrid */
		
		iterate(grid.getWorldObjects(TerrainBoundaryWorldObject.class),
			new Operation<TerrainBoundaryWorldObject>() {
				@Override public void perform(TerrainBoundaryWorldObject boundary) {
					
					if (boundary.getGroundState() == GroundState.ON
							&& boundary.getOutlinePolygon() != null) {
						speedupGrid.insert(boundary);
					}
					
				}
			});
				
		return speedupGrid;
	}

	private static Collection<TerrainPatch> generateTerrainPatches(
			Iterable<? extends TerrainElevationCell> terrainCells,
			Map<TerrainElevationCell, List<TerrainBoundaryWorldObject>> terrainBoundaryMap,
			Set<TerrainElevationCell> ignoredCells) {
		
		Collection<TerrainPatch> patches = new ArrayList<TerrainPatch>();
		
		for (TerrainElevationCell cell : terrainCells) {
			
			if (ignoredCells.contains(cell)) continue; //TODO: only correct until inner polys of mulitpolys etc. are used!
			
			List<TerrainBoundaryWorldObject> terrainBoundarys =
				terrainBoundaryMap.get(cell);
			
			// handle the common "empty cell" special case
			// in a more efficient way
			
			if (terrainBoundarys.isEmpty()) {
				
				patches.add(new EmptyCellTerrainPatch(cell));
				
			} else {
				
				addPatchesForCell(patches, cell, terrainBoundarys);
			
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
			List<TerrainBoundaryWorldObject> terrainBoundarys) {
		
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
		
		eleStorage.addPolygon(cellPolyXZ, cellPoly);
									
		/* subtract terrain borders from the cell polygon */

		try {
			
			Collection<PolygonWithHolesXZ> remainingPolygons = 
				CAGUtil.subtractPolygons(
						cellPoly.getSimpleXZPolygon(),
						subtractPolysXZ);
					
			/* create terrain patches */
			
			for (PolygonWithHolesXZ remainingPolygon : remainingPolygons) {
							patches.add(new GenericTerrainPatch(
						remainingPolygon.getOuter(),
						remainingPolygon.getHoles(),
						eleStorage));
							
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
