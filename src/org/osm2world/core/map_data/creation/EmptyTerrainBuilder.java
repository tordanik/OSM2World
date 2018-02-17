package org.osm2world.core.map_data.creation;

import static java.lang.Math.min;
import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.VectorGridXZ;
import org.osm2world.core.math.VectorXZ;

import com.slimjars.dist.gnu.trove.list.array.TLongArrayList;

import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmTag;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.impl.Node;
import de.topobyte.osm4j.core.model.impl.Tag;
import de.topobyte.osm4j.core.model.impl.Way;

/**
 * utility class for building geometry representing empty terrain.
 */
public class EmptyTerrainBuilder {

	/** prevents instantiation */
	private EmptyTerrainBuilder() { }
	
	/** tag to be internally used on faked ways around "empty terrain" */
	public static final OsmTag EMPTY_SURFACE_TAG =
			new Tag("surface", "osm2world:empty_terrain");

	/** faked outline node for the terrain areas */
	private static final OsmNode EMPTY_SURFACE_NODE = new Node(
			0, Double.NaN, Double.NaN);
	
	/** faked outline way for the terrain areas */
	private static final OsmWay EMPTY_SURFACE_WAY = new Way(
			0, new TLongArrayList(), singletonList(EMPTY_SURFACE_TAG));
	
	public static final double POINT_GRID_DIST = 30;
	public static final int PATCH_SIZE_POINTS = 10;
	
	/**
	 * creates a grid of square {@link MapArea}s to represent empty terrain.
	 * The areas are connected with each other, but do not overlap,
	 * and cover the entire data bounds.
	 * 
	 * These areas do not come from OSM data, but they are treated the same
	 * as mapped areas later on to avoid unnecessary special case handling.
	 */
	static void createAreasForEmptyTerrain(List<MapNode> mapNodes,
			List<MapArea> mapAreas, AxisAlignedBoundingBoxXZ dataBounds) {
		
		VectorGridXZ posGrid = new VectorGridXZ(
				dataBounds.pad(POINT_GRID_DIST), POINT_GRID_DIST);
		
		/* create a grid of nodes (leaving points within the future patches blank) */
		
		MapNode[][] nodeGrid = new MapNode[posGrid.sizeX()][posGrid.sizeZ()];
		
		for (int x = 0; x < posGrid.sizeX(); x++) {
			for (int z = 0; z < posGrid.sizeZ(); z++) {
				
				if (x % PATCH_SIZE_POINTS == 0 || x == posGrid.sizeX() - 1
						|| z % PATCH_SIZE_POINTS == 0 || z == posGrid.sizeZ() - 1) {
					
					VectorXZ pos = posGrid.get(x, z);
					
					MapNode mapNode = new MapNode(pos, EMPTY_SURFACE_NODE);
					
					nodeGrid[x][z] = mapNode;
					mapNodes.add(mapNode);
					
				}
				
			}
		}
		
		/* create a grid of areas based on the nodes */
			
		// calculate the number of patches, but always round up
		int numPatchesX = (nodeGrid.length + PATCH_SIZE_POINTS - 2) / PATCH_SIZE_POINTS;
		int numPatchesZ = (nodeGrid[0].length + PATCH_SIZE_POINTS - 2) / PATCH_SIZE_POINTS;

		for (int patchX = 0; patchX < numPatchesX; patchX++) {
			for (int z = 0; z < numPatchesZ; z++) {
				
				mapAreas.add(createAreaForPatch(nodeGrid,
						patchX * PATCH_SIZE_POINTS,
						z * PATCH_SIZE_POINTS));
				
			}
		}
		
	}
	
	private static MapArea createAreaForPatch(MapNode[][] nodeGrid,
			int startX, int startZ) {
		
		int endX = min(startX + PATCH_SIZE_POINTS + 1, nodeGrid.length);
		int endZ = min(startZ + PATCH_SIZE_POINTS + 1, nodeGrid[0].length);
		
		List<MapNode> nodes = new ArrayList<MapNode>();
		
		// first row
		for (int x = startX; x < endX; x++) {
			nodes.add(nodeGrid[x][startZ]);
		}
		
		// last column
		for (int z = startZ + 1; z < endZ - 1; z++) {
			nodes.add(nodeGrid[endX - 1][z]);
		}
		
		// last row
		for (int x = endX - 1; x >= startX; x--) {
			nodes.add(nodeGrid[x][endZ - 1]);
		}
		
		// first column
		for (int z = endZ - 2; z >= startZ /* start will be added again */; z--) {
			nodes.add(nodeGrid[startX][z]);
		}
		
		return new MapArea(EMPTY_SURFACE_WAY, nodes);
		
	}
	
}
