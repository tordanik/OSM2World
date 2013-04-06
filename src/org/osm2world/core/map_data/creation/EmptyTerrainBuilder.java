package org.osm2world.core.map_data.creation;

import static java.lang.Math.ceil;
import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.plugins.graphview.core.data.EmptyTagGroup;
import org.openstreetmap.josm.plugins.graphview.core.data.MapBasedTagGroup;
import org.openstreetmap.josm.plugins.graphview.core.data.Tag;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.osm.data.OSMNode;
import org.osm2world.core.osm.data.OSMWay;

/**
 * utility class for building geometry representing empty terrain.
 */
public class EmptyTerrainBuilder {

	/** prevents instantiation */
	private EmptyTerrainBuilder() { }
	
	/** tag to be internally used on faked ways around "empty terrain" */
	public static final Tag EMPTY_SURFACE_TAG =
			new Tag("surface", "osm2world:empty_terrain");

	/** faked outline node for the terrain areas */
	private static final OSMNode EMPTY_SURFACE_NODE = new OSMNode(
			Double.NaN, Double.NaN, EmptyTagGroup.EMPTY_TAG_GROUP, 0);
	
	/** faked outline way for the terrain areas */
	private static final OSMWay EMPTY_SURFACE_WAY = new OSMWay(
			new MapBasedTagGroup(EMPTY_SURFACE_TAG), 0,
			Collections.<OSMNode>emptyList());
	
	/** intended length of the sides of a terrain patch */
	private static final double PATCH_SIZE = 30;
	
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
		
		int numPatchesX = (int) ceil(dataBounds.sizeX() / PATCH_SIZE) + 1;
		int numPatchesZ = (int) ceil(dataBounds.sizeZ() / PATCH_SIZE) + 1;
		
		double terrainMinX = dataBounds.minX - PATCH_SIZE / 2;
		double terrainMinZ = dataBounds.minZ - PATCH_SIZE / 2;
		
		/* create a grid of nodes */
		
		MapNode[][] nodeGrid = new MapNode[numPatchesX + 1][numPatchesZ + 1];
		
		for (int x = 0; x < numPatchesX + 1; x++) {
			for (int z = 0; z < numPatchesZ + 1; z++) {
				
				VectorXZ pos = new VectorXZ(
						terrainMinX + x * PATCH_SIZE,
						terrainMinZ + z * PATCH_SIZE);
				
				MapNode mapNode = new MapNode(pos, EMPTY_SURFACE_NODE);
				
				nodeGrid[x][z] = mapNode;
				mapNodes.add(mapNode);

			}
		}
		
		/* create a grid of areas based on the nodes */
		
		for (int x = 0; x < numPatchesX; x++) {
			for (int z = 0; z < numPatchesZ; z++) {
				
				MapArea mapArea = new MapArea(EMPTY_SURFACE_WAY, asList(
						nodeGrid[x][z],
						nodeGrid[x+1][z],
						nodeGrid[x+1][z+1],
						nodeGrid[x][z+1],
						nodeGrid[x][z]));
				
				mapAreas.add(mapArea);

			}
		}
				
	}
		
}
