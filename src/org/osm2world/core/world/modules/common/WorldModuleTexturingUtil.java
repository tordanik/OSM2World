package org.osm2world.core.world.modules.common;

import static java.lang.Math.abs;
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
	 * 
	 * @param vertical  uses x,y coordinates instead of x,z coordinates
	 */
	public static final List<List<VectorXZ>> globalTexCoordLists(
			List<VectorXYZ> vs, Material material, boolean vertical) {
		
		List<TextureData> textureDataList = material.getTextureDataList();
		
		if (textureDataList.size() == 0) {
			
			return emptyList();
			
		} else if (textureDataList.size() == 1) {
			
			return singletonList(globalTexCoordList(
					vs, textureDataList.get(0), vertical));
			
		} else {
			
			List<List<VectorXZ>> result = new ArrayList<List<VectorXZ>>();
			
			for (TextureData textureData : textureDataList) {
				result.add(globalTexCoordList(vs, textureData, vertical));
			}
			
			return result;
			
		}
		
	}

	private static final List<VectorXZ> globalTexCoordList(
			List<VectorXYZ> vs, TextureData textureData, boolean vertical) {
		
		List<VectorXZ> textureCoords = new ArrayList<VectorXZ>(vs.size());
		
		for (VectorXYZ v : vs) {
			textureCoords.add(new VectorXZ(
					v.x / textureData.width,
					(vertical ? v.y : v.z) / textureData.height));
		}
		
		return textureCoords;
		
	}

	/**
	 * variant of {@link #globalTexCoordLists(List, Material, boolean)}
	 * based on a triangle collection
	 * @param vertical TODO
	 */
	public static final List<List<VectorXZ>> globalTexCoordLists(
			Collection<TriangleXYZ> triangles, Material material, boolean vertical) {
		
		List<VectorXYZ> vs = new ArrayList<VectorXYZ>(triangles.size() * 3);
		
		for (TriangleXYZ triangle : triangles) {
			vs.add(triangle.v1);
			vs.add(triangle.v2);
			vs.add(triangle.v3);
		}
		
		return globalTexCoordLists(vs, material, vertical);
		
	}
	
	/**
	 * creates texture coordinates for triangles that orient the texture
	 * based on each triangle's downward slope.
	 */
	public static final List<List<VectorXZ>> slopedFaceTexCoordLists(
			Collection<TriangleXYZ> triangles, Material material) {
		
		List<TextureData> textureDataList = material.getTextureDataList();
		
		if (textureDataList.size() == 0) {
			
			return emptyList();
			
		} else if (textureDataList.size() == 1) {
			
			return singletonList(slopedFaceTexCoordList(
					triangles, textureDataList.get(0)));
			
		} else {
			
			List<List<VectorXZ>> result = new ArrayList<List<VectorXZ>>();
			
			for (TextureData textureData : textureDataList) {
				result.add(slopedFaceTexCoordList(triangles, textureData));
			}
			
			return result;
			
		}
		
	}
		
	
	private static final List<VectorXZ> slopedFaceTexCoordList(
			Collection<TriangleXYZ> triangles, TextureData textureData) {
		
		List<VectorXZ> texCoords = new ArrayList<VectorXZ>(3 * triangles.size());
		
		List<Double> knownAngles = new ArrayList<Double>();
		
		for (TriangleXYZ triangle : triangles) {
		
			VectorXZ normalXZProjection = triangle.getNormal().xz();
			
			double downAngle = 0;
			
			if (normalXZProjection.x != 0 || normalXZProjection.z != 0) {
				
				downAngle = normalXZProjection.angle();
				
				//try to avoid differences between triangles of the same face
				
				Double similarKnownAngle = null;
				
				for (double knownAngle : knownAngles) {
					if (abs(downAngle - knownAngle) < 0.02) {
						similarKnownAngle = knownAngle;
						break;
					}
				}
				
				if (similarKnownAngle == null) {
					knownAngles.add(downAngle);
				} else {
					downAngle = similarKnownAngle;
				}
				
			}
			
			for (VectorXYZ v : triangle.getVertices()) {
				VectorXZ baseTexCoord = v.rotateY(-downAngle).xz();
				texCoords.add(new VectorXZ(
						baseTexCoord.x / textureData.width,
						baseTexCoord.z / textureData.height));
			}
			
		}
		
		return texCoords;
		
	}

	/**
	 * creates texture coordinates for a triangle strip,
	 * based on the length along a wall from the starting point,
	 * height of the vertex, and texture size.
	 * 
	 * @param vs  wall vertices, ordered along the wall, for a triangle strip
	 *            alternating between upper and lower vertex
	 */
	public static final List<List<VectorXZ>> wallTexCoordLists(
			List<VectorXYZ> vs, Material material) {
		
		List<TextureData> textureDataList = material.getTextureDataList();
		
		if (textureDataList.size() == 0) {
			
			return emptyList();
			
		} else if (textureDataList.size() == 1) {
			
			return singletonList(wallTexCoordList(
					vs, textureDataList.get(0)));
			
		} else {
			
			List<List<VectorXZ>> result = new ArrayList<List<VectorXZ>>();
			
			for (TextureData textureData : textureDataList) {
				result.add(wallTexCoordList(vs, textureData));
			}
			
			return result;
			
		}
		
	}

	private static final List<VectorXZ> wallTexCoordList(
			List<VectorXYZ> vs, TextureData textureData) {
		
		/* calculate length of the wall (if needed later) */
		
		double totalLength = 0;
		
		if (textureData.width == 0) {
			for (int i = 0; i+1 < vs.size(); i++) {
				totalLength += vs.get(i).distanceToXZ(vs.get(i+1));
			}
		}
		
		/* calculate texture coordinate list */
		
		List<VectorXZ> textureCoords = new ArrayList<VectorXZ>(vs.size());
				
		double accumulatedLength = 0;
		
		for (int i = 0; i < vs.size(); i++) {
			
			VectorXYZ v = vs.get(i);
			
			// increase accumulated length after every second vector
			
			if (i > 0 && i % 2 == 0) {
				accumulatedLength += v.xz().distanceTo(vs.get(i-2).xz());
			}
			
			// calculate texture coords.
			// height/width of 0 means: the texture should fit exactly onto the wall
			
			double s, t;
			
			if (textureData.width > 0) {
				s = accumulatedLength / textureData.width;
			} else {
				s = accumulatedLength / totalLength;
			}
			
			if (textureData.height > 0) {
				t = (i % 2 == 0) ? (v.distanceTo(vs.get(i+1))) / textureData.height : 0;
			} else {
				t = (i % 2 == 0) ? 1 : 0;
			}
			
			textureCoords.add(new VectorXZ(s, t));
			
		}
		
		return textureCoords;
		
	}

	
}
