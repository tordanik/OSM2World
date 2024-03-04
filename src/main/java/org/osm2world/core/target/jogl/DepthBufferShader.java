package org.osm2world.core.target.jogl;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES2.GL_TEXTURE_BORDER_COLOR;
import static org.osm2world.core.target.jogl.AbstractJOGLTarget.getFloatBuffer;

import java.awt.Color;
import java.nio.FloatBuffer;

import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Transparency;
import org.osm2world.core.target.common.material.TextureData;
import org.osm2world.core.target.common.material.TextureData.Wrap;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.texture.Texture;

/**
 * Shader that renders the depth buffer only. Supports transparent texture layers with {@link #USE_TRANSPARENCY}.
 */
public class DepthBufferShader extends AbstractPrimitiveShader {

	public static final boolean USE_TRANSPARENCY = true;

	private int modelViewProjectionMatrixID;
	private int vertexPositionID;
	private int[] vertexTexCoordID = new int[DefaultShader.MAX_TEXTURE_LAYERS];

	public DepthBufferShader(GL3 gl) {
		super(gl, "/shaders/shadowmap");

		// get indices of named attributes
		vertexPositionID = gl.glGetAttribLocation(shaderProgram, "VertexPosition");
		for (int i=0; i<DefaultShader.MAX_TEXTURE_LAYERS; i++)
			vertexTexCoordID[i] = gl.glGetAttribLocation(shaderProgram, "VertexTexCoord"+i+"");

		// get indices of uniform variables
		modelViewProjectionMatrixID = gl.glGetUniformLocation(shaderProgram, "ModelViewProjectionMatrix");

		this.validateShader();
	}

	/**
	 * Send uniform matrices "ProjectionMatrix, ModelViewMatrix and ModelViewProjectionMatrix" to vertex shader
	 */
	public void setPMVMatrix(PMVMatrix pmvMatrix) {
		FloatBuffer pmvMat = FloatBuffer.allocate(16);
		FloatUtil.multMatrix(pmvMatrix.glGetPMatrixf(), pmvMatrix.glGetMvMatrixf(), pmvMat.array());
		gl.glUniformMatrix4fv(this.getModelViewProjectionMatrixID(), 1, false, pmvMat);
	}

	@Override
	public int getVertexPositionID() {
		return vertexPositionID;
	}

	/**
	 * Returns the id to use to bind the ModelViewProjectionMatrix attribute.
	 */
	public int getModelViewProjectionMatrixID() {
		return modelViewProjectionMatrixID;
	}

	@Override
	public boolean setMaterial(Material material, JOGLTextureManager textureManager) {

		if (!USE_TRANSPARENCY) {
			return true;
		}

		/*
		 * only set textures (needed for transparency)
		 */
		int numTexLayers = 0;
		if (material.getTextureLayers() != null) {
			numTexLayers = material.getTextureLayers().size();
		}

		/* set textures and associated parameters */
		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "useAlphaTreshold"), material.getTransparency() == Transparency.BINARY ? 1 : 0);
		if (material.getTransparency() == Transparency.BINARY) {
			gl.glUniform1f(gl.glGetUniformLocation(shaderProgram, "alphaTreshold"), 0.5f );
		}

	    for (int i = 0; i < DefaultShader.MAX_TEXTURE_LAYERS; i++) {
	    	if (i < numTexLayers) {
				gl.glActiveTexture(getGLTextureConstant(i));
				TextureData textureData = material.getTextureLayers().get(i).baseColorTexture;
				if (false /*textureData.isBumpMap*/) {
		    		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "useTexture["+i+"]"), 0);
		    		continue;
				} else {
		    		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "useTexture["+i+"]"), 1);
				}

 				Texture texture = textureManager.getTextureForTextureData(textureData);

				texture.bind(gl);

				/* wrapping behavior */

				int wrap = 0;

				switch (textureData.wrap) {
				case REPEAT: wrap = GL_REPEAT; break;
				case CLAMP: wrap = GL_CLAMP_TO_EDGE; break;
				}

				gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrap);
		        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrap);


		        if (textureData.wrap == Wrap.CLAMP) {

		        	/* TODO: make the RGB configurable -  for some reason,
		        	 * it shows up in lowzoom even if fully transparent */
		        	gl.glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR,
		        			getFloatBuffer(new Color(1f, 1f, 1f, 0f)));

		        }

		        int loc = gl.glGetUniformLocation(shaderProgram, "Tex["+i+"]");
		        if (loc < 0) {
		        	//throw new RuntimeException("Tex["+i+"] not found in shader program.");
		        }
		        gl.glUniform1i(loc, getGLTextureNumber(i));
	    	} else {
	    		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "useTexture["+i+"]"), 0);
	    	}
	    }

	    return true;
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

	@Override
	public int getVertexNormalID() {
		return -1; // unused
	}

	@Override
	public int getVertexTexCoordID(int i) {
		return vertexTexCoordID[i];
	}

	@Override
	public int getVertexBumpMapCoordID() {
		return -1; // unused
	}

	@Override
	public int getVertexTangentID() {
		return -1; // unused
	}
}
