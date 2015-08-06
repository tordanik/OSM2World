package org.osm2world.core.target.jogl;

import static javax.media.opengl.GL.GL_ARRAY_BUFFER;
import static javax.media.opengl.GL.GL_TRIANGLES;
import static javax.media.opengl.fixedfunc.GLPointerFunc.GL_TEXTURE_COORD_ARRAY;
import static org.osm2world.core.math.GeometryUtil.triangleNormalListFromTriangleStrip;
import static org.osm2world.core.math.GeometryUtil.triangleVertexListFromTriangleFan;
import static org.osm2world.core.math.GeometryUtil.triangleVertexListFromTriangleStrip;
import static org.osm2world.core.math.GeometryUtil.calculateTangentVectorsForTexLayer;

import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.management.RuntimeErrorException;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL3;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXYZW;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.Primitive;
import org.osm2world.core.target.common.Primitive.Type;
import org.osm2world.core.target.common.material.Material;

/**
 * class that keeps a VBO id along with associated information
 */
abstract class VBODataShader<BufferT extends Buffer> extends VBOData<BufferT> {
	
	protected GL3 gl;
	protected AbstractPrimitiveShader shader;
	
	public VBODataShader(GL3 gl, JOGLTextureManager textureManager, Material material, Collection<Primitive> primitives) {
		super(gl, textureManager, material, primitives);
		this.gl = gl;
	}
	
	public void setShader(AbstractPrimitiveShader shader) {
		this.shader = shader;
	}
	
	/**
	 * put the values for a primitive's vertices into the buffer
	 */
	@Override
	protected void addPrimitiveToValueBuffer(BufferT buffer,
			Primitive primitive) {
					
		/*
		 * rearrange the lists of vertices, normals and texture coordinates
		 * to turn triangle strips and triangle fans into separate triangles
		 */

		List<VectorXYZ> primVertices = primitive.vertices;
		List<VectorXYZ> primNormals = primitive.normals;
		List<List<VectorXZ>> primTexCoordLists = primitive.texCoordLists;
		
		if (primitive.type == Type.TRIANGLE_STRIP) {
			
			primVertices = triangleVertexListFromTriangleStrip(primVertices);
			primNormals = triangleNormalListFromTriangleStrip(primNormals);
			
			if (primTexCoordLists != null) {
				List<List<VectorXZ>> newPrimTexCoordLists = new ArrayList<List<VectorXZ>>();
				for (List<VectorXZ> primTexCoordList : primTexCoordLists) {
					newPrimTexCoordLists.add(triangleVertexListFromTriangleStrip(primTexCoordList));
				}
				primTexCoordLists = newPrimTexCoordLists;
			}
			
		} else if (primitive.type == Type.TRIANGLE_FAN) {
			
			primVertices = triangleVertexListFromTriangleFan(primVertices);
			primNormals = triangleVertexListFromTriangleFan(primNormals);
			
			if (primTexCoordLists != null) {
				List<List<VectorXZ>> newPrimTexCoordLists = new ArrayList<List<VectorXZ>>();
				for (List<VectorXZ> primTexCoordList : primTexCoordLists) {
					newPrimTexCoordLists.add(triangleVertexListFromTriangleFan(primTexCoordList));
				}
				primTexCoordLists = newPrimTexCoordLists;
			}
			
		}
		
		List<VectorXYZW> primTangents = null;
		if (material.hasBumpMap()) {
			primTangents = calculateTangentVectorsForTexLayer(primVertices, primNormals, primTexCoordLists.get(material.getBumpMapInd()));
		}
			
		/* put the values into the buffer, in the right order */
		
		for (int i = 0; i < primVertices.size(); i++) {
			
			int count = 0;
			assert (primTexCoordLists == null
					&& material.getNumTextureLayers() == 0)
				|| (primTexCoordLists != null
					&& primTexCoordLists.size() == material.getNumTextureLayers())
				: "WorldModules need to provide the correct number of tex coords";
			
			if (primTexCoordLists == null && material.getNumTextureLayers() > 0) {
				System.out.println(material);
			}
				
			for (int t = 0; t < material.getNumTextureLayers(); t++) {
				if (!material.hasBumpMap() || t != material.getBumpMapInd()) {
					VectorXZ textureCoord =	primTexCoordLists.get(t).get(i);
					put(buffer, textureCoord);
					//System.out.println("put tex coord");
					count += 2;
				}
			}
			
			put(buffer, primNormals.get(i));
			count += 3;
			if (material.hasBumpMap()) {
				put(buffer, primTangents.get(i));
				count += 4;
				put(buffer, primTexCoordLists.get(material.getBumpMapInd()).get(i));
				count += 2;
			}
			put(buffer, primVertices.get(i));
			count += 3;
			
			if (count != JOGLRendererVBO.getValuesPerVertex(material)) {
				throw new RuntimeException("put: "+count +" values:" + JOGLRendererVBO.getValuesPerVertex(material));
			}
		}
		
	}
	
	@Override
	public void render() {
		gl.glBindBuffer(GL_ARRAY_BUFFER, id[0]);

		setPointerLayout();
		if (shader.setMaterial(material, textureManager))
			gl.glDrawArrays(GL_TRIANGLES, 0, vertexCount);
		
		for (int i=1; i<DefaultShader.MAX_TEXTURE_LAYERS; i++) {
			shader.glDisableVertexAttribArray(shader.getVertexTexCoordID(i));
		}
		shader.glDisableVertexAttribArray(shader.getVertexBumpMapCoordID());
		shader.glDisableVertexAttribArray(shader.getVertexTangentID());
	}
	
	private void setPointerLayout() {
		
		int stride = valueTypeSize * JOGLRendererVBO.getValuesPerVertex(material);
		
		int offset = 0;
		
		for (int i = 0; i < material.getNumTextureLayers(); i++) {

			if (!material.hasBumpMap() || i != material.getBumpMapInd()) {
				shader.glEnableVertexAttribArray(shader.getVertexTexCoordID(i));
				shader.glVertexAttribPointer(shader.getVertexTexCoordID(i), 2, glValueType(), false, stride, offset);
				offset += 2 * valueTypeSize;
			}
			
		}
		
		shader.glVertexAttribPointer(shader.getVertexNormalID(), 3, glValueType(), false, stride, offset);
		offset += valueTypeSize() * 3;
		
		if (material.hasBumpMap()) {
			shader.glEnableVertexAttribArray(shader.getVertexTangentID());
			shader.glVertexAttribPointer(shader.getVertexTangentID(), 4, glValueType(), false, stride, offset);
			offset += valueTypeSize() * 4;
			shader.glEnableVertexAttribArray(shader.getVertexBumpMapCoordID());
			shader.glVertexAttribPointer(shader.getVertexBumpMapCoordID(), 2, glValueType(), false, stride, offset);
			offset += valueTypeSize() * 2;
		}
		if (offset != stride - 3*valueTypeSize()) {
			throw new RuntimeException("offset: "+offset + " stride:"+stride +" valueTypeSize:"+valueTypeSize());
		}
		shader.glVertexAttribPointer(shader.getVertexPositionID(), 3, glValueType(), false, stride, offset);
	}
	
	@Override
	protected int getValuesPerVertex(Material material) {
		return JOGLRendererVBO.getValuesPerVertex(material);
	}
	
	protected abstract void put(BufferT buffer, VectorXYZW t);
	
}
