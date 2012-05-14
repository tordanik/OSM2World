package org.osm2world.core.target.primitivebuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.media.opengl.GL;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.Primitive;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.target.jogl.JOGLTextureManager;

/**
 * renders the contents of a {@link PrimitiveBuffer} using JOGL.
 * Uses display lists to speed up the process.
 * 
 * If you don't need the renderer anymore, it's recommended to manually call
 * {@link #freeResources()} to delete the display lists. Otherwise, this will
 * not be done before a destructor call.
 */
public class JOGLPrimitiveBufferRenderer {
	
	private GL gl;
	
	private JOGLTextureManager textureManager;
	private PrimitiveBuffer primitiveBuffer; //keeping this referenced is only necessary because of indexed vertices
		
	/** pointer to the display list with static, non-transparent geometry */
	private Integer displayListPointer;
	
	/** transparent primitives, need to be sorted by distance from camera */
	private List<PrimitiveWithMaterial> transparentPrimitives =
			new ArrayList<PrimitiveWithMaterial>();
	
	private final class PrimitiveWithMaterial {
		
		public final Primitive primitive;
		public final Material material;
		
		private PrimitiveWithMaterial(Primitive primitive, Material material) {
			this.primitive = primitive;
			this.material = material;
		}
		
	}
	
	public JOGLPrimitiveBufferRenderer(GL gl, PrimitiveBuffer primitiveBuffer) {
		
		this.gl = gl;
		this.textureManager = new JOGLTextureManager(gl);
		this.primitiveBuffer = primitiveBuffer;
		
		primitiveBuffer.optimize();
		
		displayListPointer = gl.glGenLists(1);
		
		gl.glNewList(displayListPointer, GL.GL_COMPILE);

		for (Material material : primitiveBuffer.getMaterials()) {
			
			if (material.getUseAlpha()) {
				
				for (Primitive primitive : primitiveBuffer.getPrimitives(material)) {
					transparentPrimitives.add(
							new PrimitiveWithMaterial(primitive, material));
				}
				
			} else {
				
				JOGLTarget.setMaterial(gl, material, textureManager);
	
				for (Primitive primitive : primitiveBuffer.getPrimitives(material)) {
					renderPrimitive(gl, primitiveBuffer, primitive);
				}
				
			}
			
		}

		gl.glEndList();
		
	}

	private void renderPrimitive(GL gl, PrimitiveBuffer primitiveBuffer,
			Primitive primitive) {
		
		gl.glBegin(JOGLTarget.getGLConstant(primitive.type));
		
		int i = 0;
		for (int index : primitive.indices) {
			
			if (primitive.texCoordLists != null
					&& !primitive.texCoordLists.isEmpty()) {
				VectorXZ textureCoord =
						primitive.texCoordLists.get(0).get(i);
				gl.glTexCoord2d(textureCoord.x, textureCoord.z);
			}
			
			gl.glNormal3d(primitive.normals.get(i).x,
					primitive.normals.get(i).y,
					-primitive.normals.get(i).z);
			
			VectorXYZ v = primitiveBuffer.getVertex(index);
			gl.glVertex3d(v.x, v.y, -v.z);
				
			++ i;
			
		}
		
		gl.glEnd();
		
	}
	
	public void render(final Camera camera) {
		
		/* render static geometry */
		
		if (displayListPointer == null)
			throw new IllegalStateException("display list has been deleted");
		
		gl.glCallList(displayListPointer);
		
		/* render transparent primitives back-to-front */
		
		Material previousMaterial = null;
		
		Collections.sort(transparentPrimitives, new Comparator<PrimitiveWithMaterial>() {
			@Override
			public int compare(PrimitiveWithMaterial p1, PrimitiveWithMaterial p2) {
				return Double.compare(
						distanceToCamera(camera, p2),
						distanceToCamera(camera, p1));
			}
		});
		
		for (PrimitiveWithMaterial p : transparentPrimitives) {
			
			if (!p.material.equals(previousMaterial)) {
				JOGLTarget.setMaterial(gl, p.material, textureManager);
				previousMaterial = p.material;
			}
			
			renderPrimitive(gl, primitiveBuffer, p.primitive);
			
		}
		
	}

	private double distanceToCamera(Camera camera, PrimitiveWithMaterial p) {
		return primitiveBuffer.getVertex(p.primitive.indices[0])
				.distanceTo(camera.getPos());
	}
	
	/**
	 * frees all OpenGL resources associated with this object.
	 * Rendering will no longer be possible afterwards!
	 */
	public void freeResources() {
		
		textureManager.releaseAll();
		
		if (displayListPointer != null) {
			gl.glDeleteLists(displayListPointer, 1);
			displayListPointer = null;
			gl = null;
		}
		
	}
	
	@Override
	protected void finalize() {
		freeResources();
	}
	
}
