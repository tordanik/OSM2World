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
import java.util.Arrays;

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

public class BumpMapShader extends AbstractPrimitiveShader {
	
	/** maximum number of texture layers any material can use */
	public static final int MAX_TEXTURE_LAYERS = 4;

	/** globally controls anisotropic filtering for all textures */
	private static final boolean ANISOTROPIC_FILTERING = true;
	
	private int projectionMatrixID;
	private int modelViewMatrixID;
	private int modelViewProjectionMatrixID;
	private int normalMatrixID;
	private int shadowMatrixID;
	private int vertexPositionID;
	private int vertexNormalID;
	private int[] vertexTexCoordID = new int[MAX_TEXTURE_LAYERS];
	private int vertexBumpMapCoordID;
	private int vertexTangentID;
	
	public BumpMapShader(GL3 gl) {
		super(gl, "/shaders/bumpmap");
		
		// get indices of named attributes
		vertexPositionID = gl.glGetAttribLocation(shaderProgram, "VertexPosition");
		vertexNormalID = gl.glGetAttribLocation(shaderProgram, "VertexNormal");
		for (int i=0; i<MAX_TEXTURE_LAYERS; i++)
			vertexTexCoordID[i] = gl.glGetAttribLocation(shaderProgram, "VertexTexCoord"+i+"");
		vertexBumpMapCoordID = gl.glGetAttribLocation(shaderProgram, "VertexBumpMapCoord");
		vertexTangentID = gl.glGetAttribLocation(shaderProgram, "VertexTangent");
		
		// get indices of uniform variables
		projectionMatrixID = gl.glGetUniformLocation(shaderProgram, "ProjectionMatrix");
		modelViewMatrixID = gl.glGetUniformLocation(shaderProgram, "ModelViewMatrix");
		modelViewProjectionMatrixID = gl.glGetUniformLocation(shaderProgram, "ModelViewProjectionMatrix");
		normalMatrixID = gl.glGetUniformLocation(shaderProgram, "NormalMatrix");
		shadowMatrixID = gl.glGetUniformLocation(shaderProgram, "ShadowMatrix");
		
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
	 * Pass 2: Render by using the generated shadow map
	 */
	public void prepareShadowRendering() {
		// set viewport, view and projection matrices to camera
		
		// bind default frame buffer
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
		
		// reset culling
		
	}
	
	@Override
	public void useShader() {
		super.useShader();
		prepareShadowRendering();
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
	
	@Override
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
		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "useBumpMaps"), material.hasBumpMap() ? 1 : 0);
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
	    	if (i < numTexLayers) {
				gl.glActiveTexture(getGLTextureConstant(i));
				TextureData textureData = material.getTextureDataList().get(i);
				Texture texture = textureManager.getTextureForFile(textureData.file);

				texture.bind(gl);
				
				if (textureData.isBumpMap) {
		    		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "useTexture["+i+"]"), 0);
				} else {
		    		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "useTexture["+i+"]"), 1);
				
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

		        int loc;
		        if (textureData.isBumpMap) {
		        	loc = gl.glGetUniformLocation(shaderProgram, "BumpMap");
			        if (loc < 0) {
			        	//throw new RuntimeException("BumpMap not found in shader program.");
			        }
				} else {
					loc = gl.glGetUniformLocation(shaderProgram, "Tex["+i+"]");
					if (loc < 0) {
						//throw new RuntimeException("Tex["+i+"] not found in shader program.");
					}
				}
		        gl.glUniform1i(loc, getGLTextureNumber(i));
	    	} else {
	    		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "useTexture["+i+"]"), 0);
	    	}
	    }
	   
	}
	
	static final int getGLTextureConstant(int textureNumber) {
		switch (getGLTextureNumber(textureNumber)) {
		//case 0: return GL.GL_TEXTURE0;
		case 1: return GL.GL_TEXTURE1;
		case 2: return GL.GL_TEXTURE2;
		case 3: return GL.GL_TEXTURE3;
		case 4: return GL.GL_TEXTURE4;
		default: throw new Error("programming error: unhandled texture number");
		}
	}
	
	static final int getGLTextureNumber(int textureNumber) {
		return textureNumber + 1;
	}
	
	public void bindShadowMap(int shadowMapHandle) {
		gl.glActiveTexture(GL.GL_TEXTURE0);
		gl.glBindTexture(GL.GL_TEXTURE_2D, shadowMapHandle);
        gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "ShadowMap"), 0);
        gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "useShadowMap"), 1);
	}
	
	@Override
	public int getVertexPositionID() {
		return vertexPositionID;
	}

	@Override
	public int getVertexNormalID() {
		return vertexNormalID;
	}

	@Override
	public int getVertexTexCoordID(int i) {
		return vertexTexCoordID[i];
	}

	@Override
	public int getVertexBumpMapCoordID() {
		return vertexBumpMapCoordID;
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
	
	public int getShadowMatrixID() {
		return shadowMatrixID;
	}

	@Override
	public int getVertexTangentID() {
		return vertexTangentID;
	}

	public void setShadowMatrix(PMVMatrix pmvMatrix) {
		// S = B*MPV
		
		// bias matrix
		float[] b = {0.5f, 0,    0,    0,
					 0,    0.5f, 0,    0,
					 0,    0,    0.5f, 0f,
					 0.5f, 0.5f, 0.5f, 1.0f};
		FloatBuffer bb = FloatBuffer.wrap(b);
		
		// PMV of light source
		FloatBuffer pmvMat = FloatBuffer.allocate(16);
		FloatUtil.multMatrixf(pmvMatrix.glGetPMatrixf(), pmvMatrix.glGetMvMatrixf(), pmvMat);
		
		FloatBuffer shadowMat = FloatBuffer.allocate(16);
		FloatUtil.multMatrixf(bb, pmvMat, shadowMat);
		
		gl.glUniformMatrix4fv(this.getShadowMatrixID(), 1, false, shadowMat);
	}
}