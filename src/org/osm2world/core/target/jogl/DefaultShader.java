package org.osm2world.core.target.jogl;

import static javax.media.opengl.GL.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT;
import static javax.media.opengl.GL.GL_REPEAT;
import static javax.media.opengl.GL.GL_TEXTURE0;
import static javax.media.opengl.GL.GL_TEXTURE1;
import static javax.media.opengl.GL.GL_TEXTURE2;
import static javax.media.opengl.GL.GL_TEXTURE3;
import static javax.media.opengl.GL.GL_TEXTURE_2D;
import static javax.media.opengl.GL.GL_TEXTURE_MAX_ANISOTROPY_EXT;
import static javax.media.opengl.GL.GL_TEXTURE_WRAP_S;
import static javax.media.opengl.GL.GL_TEXTURE_WRAP_T;
import static javax.media.opengl.GL2GL3.GL_CLAMP_TO_BORDER;
import static javax.media.opengl.GL2GL3.GL_TEXTURE_BORDER_COLOR;
import static org.osm2world.core.target.common.material.Material.multiplyColor;
import static org.osm2world.core.target.common.material.Material.Transparency.BINARY;
import static org.osm2world.core.target.common.material.Material.Transparency.TRUE;
import static org.osm2world.core.target.jogl.AbstractJOGLTarget.getFloatBuffer;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;

import org.osm2world.core.target.common.TextureData;
import org.osm2world.core.target.common.TextureData.Wrap;
import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Transparency;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.texture.Texture;

public class DefaultShader extends AbstractShader {
	
	/** maximum number of texture layers any material can use */
	public static final int MAX_TEXTURE_LAYERS = 4;

	/** globally controls anisotropic filtering for all textures */
	private static final boolean ANISOTROPIC_FILTERING = true;
	
	private int projectionMatrixID;
	private int modelViewMatrixID;
	private int modelViewProjectionMatrixID;
	private int normalMatrixID;
	private int vertexPositionID;
	private int vertexNormalID;
	private int[] vertexTexCoordID = new int[MAX_TEXTURE_LAYERS];
	
	public DefaultShader(GL3 gl) {
		super(gl, "/shaders/default");
		
		// get indices of named attributes
		vertexPositionID = gl.glGetAttribLocation(shaderProgram, "VertexPosition");
		vertexNormalID = gl.glGetAttribLocation(shaderProgram, "VertexNormal");
		for (int i=0; i<MAX_TEXTURE_LAYERS; i++)
			vertexTexCoordID[i] = gl.glGetAttribLocation(shaderProgram, "VertexTexCoord"+i+"");
		
		// get indices of uniform variables
		projectionMatrixID = gl.glGetUniformLocation(shaderProgram, "ProjectionMatrix");
		modelViewMatrixID = gl.glGetUniformLocation(shaderProgram, "ModelViewMatrix");
		modelViewProjectionMatrixID = gl.glGetUniformLocation(shaderProgram, "ModelViewProjectionMatrix");
		normalMatrixID = gl.glGetUniformLocation(shaderProgram, "NormalMatrix");
	}
	
	@Override
	public void loadDefaults() {
		
		// set default material values
		gl.glUniform3f(gl.glGetUniformLocation(shaderProgram, "Material.Ka"), 0,0,0);
		gl.glUniform3f(gl.glGetUniformLocation(shaderProgram, "Material.Kd"), 0,0,0);
		gl.glUniform3f(gl.glGetUniformLocation(shaderProgram, "Material.Ks"), 0,0,0);
		gl.glUniform1f(gl.glGetUniformLocation(shaderProgram, "Material.Shininess"), 0);
	}
	
	/**
	 * Send uniform matrices "ProjectionMatrix, ModelViewMatrix and ModelViewProjectionMatrix" to vertex shader
	 * @param pmvMatrix
	 */
	public void setPMVMatrix(PMVMatrix pmvMatrix) {
		gl.glUniformMatrix4fv(this.getProjectionMatrixID(), 1, false, pmvMatrix.glGetPMatrixf());
		gl.glUniformMatrix4fv(this.getModelViewMatrixID(), 1, false, pmvMatrix.glGetMvMatrixf());
		FloatBuffer pmvMat = FloatBuffer.allocate(16);
		FloatUtil.multMatrixf(pmvMatrix.glGetPMatrixf(), pmvMatrix.glGetMvMatrixf(), pmvMat);
		gl.glUniformMatrix4fv(this.getModelViewProjectionMatrixID(), 1, false, pmvMat);
		gl.glUniformMatrix4fv(this.getNormalMatrixID(), 1, false, pmvMatrix.glGetMvitMatrixf());
	}
	
	public void setGlobalLighting(GlobalLightingParameters lighting) {
		
		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "useLighting"), lighting != null ? 1 : 0);
		if (lighting != null) {
			gl.glUniform4f(gl.glGetUniformLocation(shaderProgram, "Light.Position"), (float)lighting.lightFromDirection.getX(),
					(float)lighting.lightFromDirection.getY(), -(float)lighting.lightFromDirection.getZ(), 0f);
			gl.glUniform3fv(gl.glGetUniformLocation(shaderProgram, "Light.La"), 1, getFloatBuffer(lighting.globalAmbientColor));
			gl.glUniform3fv(gl.glGetUniformLocation(shaderProgram, "Light.Ld"), 1, getFloatBuffer(lighting.lightColorDiffuse));
			gl.glUniform3fv(gl.glGetUniformLocation(shaderProgram, "Light.Ls"), 1, getFloatBuffer(lighting.lightColorSpecular));			
		}
	}
	
	public void setMaterial(Material material, JOGLTextureManager textureManager) {

		int numTexLayers = 0;
		if (material.getTextureDataList() != null) {
			numTexLayers = material.getTextureDataList().size();
		}
		
		/* set color / lighting */
		
		if (numTexLayers == 0 || material.getTextureDataList().get(0).colorable) {
			
			gl.glUniform3fv(gl.glGetUniformLocation(shaderProgram, "Material.Ka"), 1, getFloatBuffer(material.ambientColor()));
			gl.glUniform3fv(gl.glGetUniformLocation(shaderProgram, "Material.Kd"), 1, getFloatBuffer(material.diffuseColor()));
			gl.glUniform3f(gl.glGetUniformLocation(shaderProgram, "Material.Ks"), 1f, 1f, 1f);
			gl.glUniform1f(gl.glGetUniformLocation(shaderProgram, "Material.Shininess"), 100f);
			
		} else {
			
			gl.glUniform3fv(gl.glGetUniformLocation(shaderProgram, "Material.Ka"), 1, getFloatBuffer(
					multiplyColor(Color.WHITE, material.getAmbientFactor())));
			gl.glUniform3fv(gl.glGetUniformLocation(shaderProgram, "Material.Kd"), 1, getFloatBuffer(
					multiplyColor(Color.WHITE, material.getDiffuseFactor())));
			gl.glUniform3f(gl.glGetUniformLocation(shaderProgram, "Material.Ks"), 1f,1f,1f);
			gl.glUniform1f(gl.glGetUniformLocation(shaderProgram, "Material.Shininess"), 100f);	
		}
		
		/* set textures and associated parameters */
		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "useAlphaTreshold"), material.getTransparency() == Transparency.BINARY ? 1 : 0);
		if (material.getTransparency() == Transparency.FALSE) {
			gl.glDisable(GL.GL_BLEND);
		} else {
			gl.glEnable(GL.GL_BLEND);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			if (material.getTransparency() == Transparency.BINARY) {
				gl.glUniform1f(gl.glGetUniformLocation(shaderProgram, "alphaTreshold"), 0.5f );
			}
		}
	    for (int i = 0; i < MAX_TEXTURE_LAYERS; i++) {
    	    gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "useTexture["+i+"]"), i < numTexLayers ? 1 : 0);
	    	if (i < numTexLayers) {
				
				gl.glActiveTexture(getGLTextureConstant(i));
				TextureData textureData = material.getTextureDataList().get(i);
				Texture texture = textureManager.getTextureForFile(textureData.file);

				texture.bind(gl);
				
				/* enable anisotropic filtering (note: this could be a
				 * per-texture setting, but currently isn't) */
				
		        if (gl.isExtensionAvailable("GL_EXT_texture_filter_anisotropic")) {
					
		        	if (ANISOTROPIC_FILTERING) {
						
						float max[] = new float[1];
						gl.glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, max, 0);
						
						gl.glTexParameterf(GL_TEXTURE_2D,
								GL_TEXTURE_MAX_ANISOTROPY_EXT,
								max[0]);
						
					} else {
						
						gl.glTexParameterf(GL_TEXTURE_2D,
								GL_TEXTURE_MAX_ANISOTROPY_EXT,
								1.0f);
						
					}
					
		        }
		        
				/* wrapping behavior */
		        
				int wrap = 0;
				
				switch (textureData.wrap) {
				case CLAMP: System.out.println("Warning: CLAMP is no longer supported. Using CLAMP_TO_BORDER instead."); wrap = GL_CLAMP_TO_BORDER; break;
				case REPEAT: wrap = GL_REPEAT; break;
				case CLAMP_TO_BORDER: wrap = GL_CLAMP_TO_BORDER; break;
				}
				
				gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrap);
		        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrap);
		        
		        
		        if (textureData.wrap == Wrap.CLAMP_TO_BORDER) {
		        	
		        	/* TODO: make the RGB configurable -  for some reason,
		        	 * it shows up in lowzoom even if fully transparent */
		        	gl.glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR,
		        			getFloatBuffer(new Color(1f, 1f, 1f, 0f)));
		        	
		        }

		        int loc = gl.glGetUniformLocation(shaderProgram, "Tex["+i+"]");
		        if (loc < 0) {
		        	throw new RuntimeException("Tex["+i+"] not found in shader program.");
		        }
		        gl.glUniform1i(loc, i);
	    	}
	    }
	   
	}
	
	static final int getGLTextureConstant(int textureNumber) {
		switch (textureNumber) {
		case 0: return GL_TEXTURE0;
		case 1: return GL_TEXTURE1;
		case 2: return GL_TEXTURE2;
		case 3: return GL_TEXTURE3;
		default: throw new Error("programming error: unhandled texture number");
		}
	}
	
	public int getVertexPositionID() {
		return vertexPositionID;
	}
	
	public int getVertexNormalID() {
		return vertexNormalID;
	}
	
	public int getVertexTexCoordID(int i) {
		return vertexTexCoordID[i];
	}
	
	public int getProjectionMatrixID() {
		return projectionMatrixID;
	}
	
	public int getModelViewMatrixID() {
		return modelViewMatrixID;
	}
	
	public int getModelViewProjectionMatrixID() {
		return modelViewProjectionMatrixID;
	}
	
	public int getNormalMatrixID() {
		return normalMatrixID;
	}
}