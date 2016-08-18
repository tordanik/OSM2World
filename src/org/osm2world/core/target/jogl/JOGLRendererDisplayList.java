package org.osm2world.core.target.jogl;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static javax.media.opengl.GL2.GL_COMPILE;
import static org.osm2world.core.target.common.rendering.OrthoTilesUtil.CardinalDirection.closestCardinal;
import static org.osm2world.core.target.jogl.JOGLTargetFixedFunction.drawPrimitive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.media.opengl.GL2;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.common.Primitive;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Transparency;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.OrthoTilesUtil.CardinalDirection;
import org.osm2world.core.target.common.rendering.Projection;

/**
 * renders the contents of a {@link PrimitiveBuffer} using JOGL.
 * Uses display lists to speed up the process.
 * 
 * If you don't need the renderer anymore, it's recommended to manually call
 * {@link #freeResources()} to delete the display lists. Otherwise, this will
 * not be done before a destructor call.
 */
class JOGLRendererDisplayList extends JOGLRenderer {
	
	protected GL2 gl;
	
	/** pointer to the display list with static, non-transparent geometry */
	private Integer displayListPointer;
	
	/** transparent primitives, need to be sorted by distance from camera */
	private List<PrimitiveWithMaterial> transparentPrimitives =
			new ArrayList<PrimitiveWithMaterial>();
	
	/**
	 * the camera direction that was the basis for the previous sorting
	 * of {@link #transparentPrimitives}.
	 */
	private CardinalDirection currentPrimitiveSortDirection = null;
	
	private static final class PrimitiveWithMaterial {
		
		public final Primitive primitive;
		public final Material material;
		
		private PrimitiveWithMaterial(Primitive primitive, Material material) {
			this.primitive = primitive;
			this.material = material;
		}
		
	}
	
	public JOGLRendererDisplayList(GL2 gl, JOGLTextureManager textureManager,
			PrimitiveBuffer primitiveBuffer) {
		
		super(textureManager);
		this.gl = gl;
		
		displayListPointer = gl.glGenLists(1);
		
		gl.glNewList(displayListPointer, GL_COMPILE);

		for (JOGLMaterial joglMaterial : primitiveBuffer.getMaterials()) {
			Material material = joglMaterial.getBaseMaterial();
			
			if (material.getTransparency() == Transparency.TRUE) {
				
				for (Primitive primitive : primitiveBuffer.getPrimitives(joglMaterial)) {
					transparentPrimitives.add(
							new PrimitiveWithMaterial(primitive, material));
				}
				
			} else {
				
				JOGLTargetFixedFunction.setMaterial(gl, material, textureManager);
	
				for (Primitive primitive : primitiveBuffer.getPrimitives(joglMaterial)) {
					drawPrimitive(gl, AbstractJOGLTarget.getGLConstant(primitive.type),
							primitive.vertices, primitive.normals,
							primitive.texCoordLists);
				}
				
			}
			
		}

		gl.glEndList();
		
	}

	@Override
	public void render(final Camera camera, final Projection projection) {
		
		/* render static geometry */
		
		if (displayListPointer == null)
			throw new IllegalStateException("display list has been deleted");
		
		gl.glCallList(displayListPointer);
		
		/* render transparent primitives back-to-front */

		sortPrimitivesBackToFront(camera, projection);
		
		Material previousMaterial = null;
		
		for (PrimitiveWithMaterial p : transparentPrimitives) {
			
			if (!p.material.equals(previousMaterial)) {
				JOGLTargetFixedFunction.setMaterial(gl, p.material, textureManager);
				previousMaterial = p.material;
			}
			
			drawPrimitive(gl, AbstractJOGLTarget.getGLConstant(p.primitive.type),
					p.primitive.vertices, p.primitive.normals,
					p.primitive.texCoordLists);
			
		}
		
	}

	private void sortPrimitivesBackToFront(final Camera camera,
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
		
		if (displayListPointer != null) {
			gl.glDeleteLists(displayListPointer, 1);
			displayListPointer = null;
		}
		gl = null;

		super.freeResources();
		
	}
	
}
