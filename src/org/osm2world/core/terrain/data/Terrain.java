package org.osm2world.core.terrain.data;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;

import org.osm2world.core.heightmap.data.TerrainElevation;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.algorithms.NormalCalculationUtil;
import org.osm2world.core.target.Material;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.Material.Lighting;
import org.osm2world.core.target.povray.POVRayTarget;
import org.osm2world.core.target.povray.RenderableToPOVRay;

/**
 * Terrain data created from a {@link MapData} and {@link TerrainElevation} data. 
 * 
 * Terrain consists of {@link TerrainPatch}es that fill the gaps between
 * representations of elements providing terrain boundary information.
 */
public class Terrain implements RenderableToAllTargets , RenderableToPOVRay {
	
	final Collection<TerrainPatch> patches;
		
	public Terrain(Collection<TerrainPatch> patches) {
		this.patches = patches;
	}
	
	/** 
	 * returns the patches making up the entire terrain.
	 * Needn't be used directly for rendering
	 * (rendering the Terrain renders all the patches),
	 * but is required e.g. for debugging.
	 */
	public Collection<TerrainPatch> getPatches() {
		return patches;
	}

	public Collection<TriangleXYZ> getTriangulation() {
		
		Collection<TriangleXYZ> triangles = new ArrayList<TriangleXYZ>();
		
		for (TerrainPatch patch : patches) {
			triangles.addAll(patch.triangulation);
		}
		
		return triangles;
		
	}
		
	@Override
	public void renderTo(Target target) {
		
		Collection<TriangleXYZ> triangles = getTriangulation();
		
		target.drawTrianglesWithNormals(
				new Material(Lighting.SMOOTH, Color.GREEN), 
				NormalCalculationUtil.calculateTrianglesWithNormals(triangles));
		
	}
	
	@Override
	public void renderTo(POVRayTarget target) {
		
		Collection<TriangleXYZ> triangles = getTriangulation();
		
		target.drawTrianglesWithNormals(
				new Material(Lighting.SMOOTH, Color.GREEN), 
				NormalCalculationUtil.calculateTrianglesWithNormals(triangles),
				true);
		
	}

}
