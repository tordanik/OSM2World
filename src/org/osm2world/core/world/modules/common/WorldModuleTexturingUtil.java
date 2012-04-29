package org.osm2world.core.world.modules.common;

import static java.util.Collections.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.TextureData;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.world.creation.WorldModule;

/**
 * utility class that can be used by {@link WorldModule}s
 * to generate texture coordinates
 */
public class WorldModuleTexturingUtil {
	
	private WorldModuleTexturingUtil() { }

	/**
	 * creates texture coordinates based only on the vertex coordinates
	 * in the global coordinate system and the texture size
	 */
	public static final List<List<VectorXZ>> generateGlobalTextureCoordLists(
			VectorXYZ[] vs, Material material) {
		
		List<TextureData> textureDataList = material.getTextureDataList();
		
		if (textureDataList.size() == 0) {
			
			return emptyList();
			
		} else if (textureDataList.size() == 1) {
			
			return singletonList(generateGlobalTextureCoordList(
					vs, textureDataList.get(0)));
			
		} else {
			
			List<List<VectorXZ>> result = new ArrayList<List<VectorXZ>>();
			
			for (TextureData textureData : textureDataList) {
				result.add(generateGlobalTextureCoordList(vs, textureData));
			}
			
			return result;
			
		}
		
	}

	private static final List<VectorXZ> generateGlobalTextureCoordList(
			VectorXYZ[] vs, TextureData textureData) {
		
		List<VectorXZ> textureCoords = new ArrayList<VectorXZ>(vs.length);
		
		for (VectorXYZ v : vs) {
			textureCoords.add(new VectorXZ(
					v.x / textureData.width,
					v.z / textureData.height));
		}
		
		return textureCoords;
		
	}

	/**
	 * variant of
	 * {@link #generateGlobalTextureCoordList(VectorXYZ[], TextureData)}
	 * based on a triangle collection
	 */
	public static final List<List<VectorXZ>> generateGlobalTextureCoordLists(
			Collection<TriangleXYZ> triangles, Material material) {
		
		VectorXYZ[] vs = new VectorXYZ[triangles.size() * 3];
		
		int i = 0;
		for (TriangleXYZ triangle : triangles) {
			vs[i*3 + 0] = triangle.v1;
			vs[i*3 + 1] = triangle.v2;
			vs[i*3 + 2] = triangle.v3;
			i++;
		}
		
		return generateGlobalTextureCoordLists(vs, material);
		
	}

	/**
	 * creates texture coordinates for a triangle strip,
	 * based on the length along a wall from the starting point,
	 * height of the vertex, and texture size.
	 * 
	 * @param vs  wall vertices, ordered along the wall, for a triangle strip
	 *            alternating between upper and lower vertex
	 */
	public static final List<List<VectorXZ>> generateWallTextureCoordLists(
			List<VectorXYZ> vs, Material material) {
		
		List<TextureData> textureDataList = material.getTextureDataList();
		
		if (textureDataList.size() == 0) {
			
			return emptyList();
			
		} else if (textureDataList.size() == 1) {
			
			return singletonList(generateWallTextureCoordList(
					vs, textureDataList.get(0)));
			
		} else {
			
			List<List<VectorXZ>> result = new ArrayList<List<VectorXZ>>();
			
			for (TextureData textureData : textureDataList) {
				result.add(generateWallTextureCoordList(vs, textureData));
			}
			
			return result;
			
		}
		
	}

	private static final List<VectorXZ> generateWallTextureCoordList(
			List<VectorXYZ> vs, TextureData textureData) {
		
		List<VectorXZ> textureCoords = new ArrayList<VectorXZ>(vs.size());
				
		double accumulatedLength = 0;
		
		for (int i = 0; i < vs.size(); i++) {
			
			VectorXYZ v = vs.get(i);
			
			if (i > 0 && i % 2 == 0) {
				accumulatedLength += v.xz().distanceTo(vs.get(i-2).xz());
			}
			
			textureCoords.add(new VectorXZ(
					accumulatedLength / textureData.width,
					v.y / textureData.height));
			
		}
		
		return textureCoords;
		
	}

	
}
