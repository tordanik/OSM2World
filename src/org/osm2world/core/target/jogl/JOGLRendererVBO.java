package org.osm2world.core.target.jogl;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static org.osm2world.core.target.common.rendering.OrthoTilesUtil.CardinalDirection.closestCardinal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.common.Primitive;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Transparency;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.OrthoTilesUtil.CardinalDirection;
import org.osm2world.core.target.common.rendering.Projection;

/**
 * Base class for renderer that use vertex buffer objects (VBO) to speed up the process.
 * 
 * If you don't need the renderer anymore, it's recommended to manually call
 * {@link #freeResources()} to delete the VBOs and other resources.
 */
public abstract class JOGLRendererVBO extends JOGLRenderer {

	protected static final boolean DOUBLE_PRECISION_RENDERING = false;

	/** VBOs with static, non-alphablended geometry for each material */
	protected List<VBOData<?>> vbos = new ArrayList<VBOData<?>>();
	
	/** alphablended primitives, need to be sorted by distance from camera */
	protected List<PrimitiveWithMaterial> transparentPrimitives =
			new ArrayList<PrimitiveWithMaterial>();
	
	/**
	 * the camera direction that was the basis for the previous sorting
	 * of {@link #transparentPrimitives}.
	 */
	private CardinalDirection currentPrimitiveSortDirection = null;

	protected static final class PrimitiveWithMaterial {
		
		public final Primitive primitive;
		public final Material material;
		public final VBOData<?> vbo;
		
		private PrimitiveWithMaterial(Primitive primitive, Material material, VBOData<?>vbo) {
			this.primitive = primitive;
			this.material = material;
			this.vbo = vbo;
		}
		
	}

	/**
	 * returns the number of values for each vertex
	 * in the vertex buffer layout appropriate for a given material.
	 */
	public static int getValuesPerVertex(Material material) {
		
		int numValues = 6; // vertex coordinates and normals
		
		if (material.getTextureDataList() != null) {
			numValues += 2 * material.getTextureDataList().size();
		}
		if (material.hasBumpMap()) {
			numValues += 4; // tangent vectors are 4D
		}
		
		return numValues;
		
	}
	
	JOGLRendererVBO(JOGLTextureManager textureManager) {
		super(textureManager);
	}
	
	/**
	 * Create the VBOs from a {@link PrimitiveBuffer}.
	 * @param primitiveBuffer the source for the VBOs
	 */
	protected void init(PrimitiveBuffer primitiveBuffer) {
		
		for (JOGLMaterial joglMaterial : primitiveBuffer.getMaterials()) {
			Material material = joglMaterial.getBaseMaterial();
			
			if (material.getTransparency() == Transparency.TRUE) {
				
				for (Primitive primitive : primitiveBuffer.getPrimitives(joglMaterial)) {
					transparentPrimitives.add(new PrimitiveWithMaterial(
							primitive, material, this.createVBOData(
									textureManager, joglMaterial,
									Arrays.asList(primitive))));
				}
				
			} else {
				
				Collection<Primitive> primitives = primitiveBuffer.getPrimitives(joglMaterial);
				vbos.add(this.createVBOData(textureManager, joglMaterial, primitives));
				
			}
			
		}
		
	}
	
	/**
	 * Sort all transparent primitives back to front relative to the camera.
	 * The projection can be used to speed up sorting if it is orthographic.
	 */
	protected void sortPrimitivesBackToFront(final Camera camera,
			final Projection projection) {
		
		if (projection.isOrthographic() &&
				abs(camera.getViewDirection().xz().angle() % (PI/2)) < 0.01 ) {
			
			/* faster sorting for cardinal directions */
			
			CardinalDirection closestCardinal = closestCardinal(camera.getViewDirection().xz().angle());
			
			if (closestCardinal.isOppositeOf(currentPrimitiveSortDirection)) {
				
				Collections.reverse(transparentPrimitives);
				
			} else if (closestCardinal != currentPrimitiveSortDirection) {
				
				Comparator<PrimitiveWithMaterial> comparator = null;
				
				switch(closestCardinal) {
				
				case N:
					comparator = new Comparator<PrimitiveWithMaterial>() {
						@Override
						public int compare(PrimitiveWithMaterial p1, PrimitiveWithMaterial p2) {
							return Double.compare(primitivePos(p2).z, primitivePos(p1).z);
						}
					};
					break;
					
				case E:
					comparator = new Comparator<PrimitiveWithMaterial>() {
						@Override
						public int compare(PrimitiveWithMaterial p1, PrimitiveWithMaterial p2) {
							return Double.compare(primitivePos(p2).x, primitivePos(p1).x);
						}
					};
					break;
					
				case S:
					comparator = new Comparator<PrimitiveWithMaterial>() {
						@Override
						public int compare(PrimitiveWithMaterial p1, PrimitiveWithMaterial p2) {
							return Double.compare(primitivePos(p1).z, primitivePos(p2).z);
						}
					};
					break;
					
				case W:
					comparator = new Comparator<PrimitiveWithMaterial>() {
						@Override
						public int compare(PrimitiveWithMaterial p1, PrimitiveWithMaterial p2) {
							return Double.compare(primitivePos(p1).x, primitivePos(p2).x);
						}
					};
					break;
					
				}
				
				Collections.sort(transparentPrimitives, comparator);
				
			}
			
			currentPrimitiveSortDirection = closestCardinal;
			
		} else {
			
			/* sort based on distance to camera */
			
			Collections.sort(transparentPrimitives, new Comparator<PrimitiveWithMaterial>() {
				@Override
				public int compare(PrimitiveWithMaterial p1, PrimitiveWithMaterial p2) {
					return Double.compare(
							distanceToCameraSq(camera, p2),
							distanceToCameraSq(camera, p1));
				}
			});
			
			currentPrimitiveSortDirection = null;
			
		}
		
	}
	
	private double distanceToCameraSq(Camera camera, PrimitiveWithMaterial p) {
		return primitivePos(p).distanceToSquared(camera.getPos());
	}
	
	private VectorXYZ primitivePos(PrimitiveWithMaterial p) {
		
		double sumX = 0, sumY = 0, sumZ = 0;
		
		for (VectorXYZ v : p.primitive.vertices) {
			sumX += v.x;
			sumY += v.y;
			sumZ += v.z;
		}
		
		return new VectorXYZ(sumX / p.primitive.vertices.size(),
				sumY / p.primitive.vertices.size(),
				sumZ / p.primitive.vertices.size());
		
	}
	
	@Override
	public void freeResources() {
		
		if (vbos != null) {
			for (VBOData<?> vbo : vbos) {
				vbo.delete();
			}
			vbos = null;
		}
		
		super.freeResources();
		
	}
	
	/**
	 * Create a new vertex buffer object for a bunch of primitives with the same material.
	 * @param textureManager the texture manager used if the material contains texture layers.
	 * @param material the material that applies to all primitives
	 * @param primitives the primitives to create the VBO for
	 * @return a vertex buffer object matching the primitives
	 */
	VBOData<?> createVBOData(JOGLTextureManager textureManager, Material material, Collection<Primitive> primitives) { System.err.println("This method should be overriden"); return null; }

	VBOData<?> createVBOData(JOGLTextureManager textureManager, JOGLMaterial material, Collection<Primitive> primitives) { System.err.println("This method should be overriden"); return null; }
}
