package org.osm2world.core.target.jogl;

import static javax.media.opengl.GL.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT;
import static javax.media.opengl.GL.GL_REPEAT;
import static javax.media.opengl.GL.GL_TEXTURE_2D;
import static javax.media.opengl.GL.GL_TEXTURE_MAX_ANISOTROPY_EXT;
import static javax.media.opengl.GL.GL_TEXTURE_WRAP_S;
import static javax.media.opengl.GL.GL_TEXTURE_WRAP_T;
import static javax.media.opengl.GL2GL3.GL_CLAMP_TO_BORDER;
import static javax.media.opengl.GL2GL3.GL_TEXTURE_BORDER_COLOR;
import static org.osm2world.core.target.jogl.AbstractJOGLTarget.getFloatBuffer;

import java.awt.Color;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.common.TextureData;
import org.osm2world.core.target.common.TextureData.Wrap;
import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Transparency;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.texture.Texture;

/**
 * Complex shader with support for complex materials and various graphic effects:
 * <ul>
 * <li> Shadow Volumes
 * <li> Shadow Maps
 * <li> Screen Space Ambient Occlusion
 * <li> Phong shading
 * <li> Bumpmaps / Normalmaps
 * </ul>
 */
public class DefaultShader extends AbstractPrimitiveShader {

	/** maximum number of texture layers any material can use */
	public static final int MAX_TEXTURE_LAYERS = 4;

	/** globally controls anisotropic filtering for all textures */
	public static final boolean ANISOTROPIC_FILTERING = true;

	/**
	 * Parameters for SSAO
	 */
	private int kernelSize = 16;
	private float[] kernel;
	private float ssaoRadius = 1;
	private int noiseTextureHandle;
	private static final int NOISE_TEXTURE_WIDTH=4;
	private static final int NOISE_TEXTURE_HEIGHT=4;

	/**
	 * Threshold for the dot product of a SSAO kernel sample and the vertex
	 * normal. this prevents samples from being too near to the vertex plane and
	 * so prevents self shadowing
	 */
	private static final double SSAO_SAMPLE_THRESHOLD = 0.15;

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

	public DefaultShader(GL3 gl) {
		super(gl, "/shaders/default");

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

		generateSamplingMatrix();
		generateNoiseTexture();

		this.prepareValidation();
		this.validateShader();
	}

	/**
	 * Executes necessary preparation steps for shader validation.
	 * Sets default texture units for samplers. This is needed if there are
	 * different sampler types, as they need to point to different texture units.
	 */
	private void prepareValidation() {

		this.useShader();
		// set default texture units
		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "ShadowMap"), 0);
        gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "DepthMap"), 1);
        gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "NoiseTex"), 2);
		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "BumpMap"), getGLTextureNumber(0));
		for (int i=0; i<MAX_TEXTURE_LAYERS; i++)
			gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "Tex["+i+"]"), getGLTextureNumber(i));
		this.disableShader();
	}

	@Override
	public void loadDefaults() {
		renderSemiTransparent = true;
		renderOnlySemiTransparent = false;

		// set default material values
		gl.glUniform3f(gl.glGetUniformLocation(shaderProgram, "Material.Color"), 0, 0, 0);
		gl.glUniform1f(gl.glGetUniformLocation(shaderProgram, "Material.Ka"), 0);
		gl.glUniform1f(gl.glGetUniformLocation(shaderProgram, "Material.Kd"), 0);
		gl.glUniform1f(gl.glGetUniformLocation(shaderProgram, "Material.Ks"), 0);
		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "Material.Shininess"), 0);
		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "isShadowed"), 0);

		// reset optional parts
		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "useShadowMap"), 0);
		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "useSSAO"), 0);
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

	/**
	 * Prepares the shader to do lighting.
	 * @param lighting the global lighting to apply. Can be <code>null</code> to disable lighting.
	 */
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

	/**
	 * Sets whether to Render everything as in shadow.
	 */
	public void setShadowed(boolean isShadowed) {
		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "isShadowed"), isShadowed ? 1 : 0);
	}

	@Override
	public boolean setMaterial(Material material, JOGLTextureManager textureManager) {
		if (!super.setMaterial(material, textureManager))
			return false;

		int numTexLayers = 0;
		if (material.getTextureDataList() != null) {
			numTexLayers = material.getTextureDataList().size();
		}

		/* set color / lighting */

		if (numTexLayers == 0 || material.getTextureDataList().get(0).colorable) {
			gl.glUniform3fv(gl.glGetUniformLocation(shaderProgram, "Material.Color"), 1, getFloatBuffer(material.getColor()));
		} else {
			gl.glUniform3fv(gl.glGetUniformLocation(shaderProgram, "Material.Color"), 1, getFloatBuffer(Color.WHITE));
		}
		gl.glUniform1f(gl.glGetUniformLocation(shaderProgram, "Material.Ka"), material.getAmbientFactor());
		gl.glUniform1f(gl.glGetUniformLocation(shaderProgram, "Material.Kd"), material.getDiffuseFactor());
		gl.glUniform1f(gl.glGetUniformLocation(shaderProgram, "Material.Ks"), material.getSpecularFactor());
		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "Material.Shininess"), material.getShininess());

		/* set textures and associated parameters */
		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "useBumpMaps"), material.hasBumpMap() ? 1 : 0);
		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "useAlphaTreshold"), material.getTransparency() == Transparency.BINARY ? 1 : 0);
		if (material.getTransparency() == Transparency.FALSE) {
			gl.glDisable(GL.GL_BLEND);
		} else {
			gl.glEnable(GL.GL_BLEND);
			/* GL.GL_SRC_ALPHA and GL.GL_ONE_MINUS_SRC_ALPHA for color blending produces correct results for color, while
			 * GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA produces correct alpha blended results: the blendfunction is in fact equal to 1-(1-SRC_APLHA)*(1-DST_APLHA)
			 */
			gl.glBlendFuncSeparate(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA, GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
			if (material.getTransparency() == Transparency.BINARY) {
				gl.glUniform1f(gl.glGetUniformLocation(shaderProgram, "alphaTreshold"), 0.5f );
			}
		}
	    for (int i = 0; i < MAX_TEXTURE_LAYERS; i++) {
	    	if (i < numTexLayers) {
				gl.glActiveTexture(getGLTextureConstant(i));
				TextureData textureData = material.getTextureDataList().get(i);
				
				Texture texture = textureManager.getTextureForFile(textureData.getFile());

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
	   return true;
	}

	static final int getGLTextureConstant(int textureNumber) {
		switch (getGLTextureNumber(textureNumber)) {
		//case 0: return GL.GL_TEXTURE0; // reserved for shadow map
		//case 1: return GL.GL_TEXTURE1; // reserved for ssao depth map
		//case 2: return GL.GL_TEXTURE2; // reserved for ssao noise texture
		case 3: return GL.GL_TEXTURE3;
		case 4: return GL.GL_TEXTURE4;
		case 5: return GL.GL_TEXTURE5;
		case 6: return GL.GL_TEXTURE6;
		default: throw new Error("programming error: unhandled texture number");
		}
	}

	static final int getGLTextureNumber(int textureNumber) {
		return textureNumber + 3; // texture id 0-2 are reserved for shadow map, ssao depth map and ssao noise texture
	}

	/**
	 * Binds the specified texture as shadow map and uses it for rendering shadows.
	 */
	public void bindShadowMap(int shadowMapHandle) {
		gl.glActiveTexture(GL.GL_TEXTURE0);
		gl.glBindTexture(GL.GL_TEXTURE_2D, shadowMapHandle);
        gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "ShadowMap"), 0);
        gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "useShadowMap"), 1);
	}

	/**
	 * Creates random sampling kernel data for SSAO.
	 */
	private void generateSamplingMatrix() {
        kernel = new float[kernelSize*3];
        for (int i = 0; i < kernelSize; ++i) {
        	float scale = (float)i / (float)kernelSize;
        	scale = lerp(0.2f, 1.0f, scale*scale);
        	VectorXYZ v = new VectorXYZ(
        			Math.random()*2-1,
        			Math.random()*2-1,
        			Math.random()
        			).normalize().mult(scale); //.mult(Math.random()*scale);
        	while (v.dot(new VectorXYZ(0, 0, 1)) < SSAO_SAMPLE_THRESHOLD) {
        		v = new VectorXYZ(
        				Math.random()*2-1,
        				Math.random()*2-1,
        				Math.random()
        				).normalize().mult(scale);
        	}

        	kernel[i*3] = (float)v.x;
        	kernel[i*3+1] = (float)v.y;
        	kernel[i*3+2] = (float)v.z;

        }
	}

	/**
	 * Generates and binds the noise texture used for SSAO.
	 */
	private void generateNoiseTexture() {
		float[] noise = new float[NOISE_TEXTURE_WIDTH*NOISE_TEXTURE_HEIGHT*3];
		for (int i = 0; i < NOISE_TEXTURE_WIDTH*NOISE_TEXTURE_HEIGHT; i++)
		{
			noise[i*3] = (float)Math.random()*2-1;
			noise[i*3+1] = (float)Math.random()*2-1;
			noise[i*3+2] = 0;
		}
		int[] noiseTexture = new int[1];
		gl.glGenTextures(1, noiseTexture, 0);
		noiseTextureHandle = noiseTexture[0];
		gl.glBindTexture(GL_TEXTURE_2D, noiseTextureHandle);
		gl.glTexImage2D(GL_TEXTURE_2D, 0, GL.GL_RGB16F, NOISE_TEXTURE_WIDTH, NOISE_TEXTURE_HEIGHT, 0, GL.GL_RGB, GL.GL_FLOAT, FloatBuffer.wrap(noise));
		gl.glTexParameteri(GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
		gl.glTexParameteri(GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
	}

	private float lerp(float a, float b, float f) {
		return a + f * (b - a);
	}

	/**
	 * Binds a texture as depth map and uses it for SSAO. Also sets up the rest of the SSAO settings.
	 */
	public void enableSSAOwithDepthMap(int depthMapHandle) {
		// bind depth map
		gl.glActiveTexture(GL.GL_TEXTURE1);
		gl.glBindTexture(GL.GL_TEXTURE_2D, depthMapHandle);
        gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "DepthMap"), 1);

        // send SSAO parameters
        gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "useSSAO"), 1);
        int[] viewport = new int[4];
		gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
		int width = viewport[2], height = viewport[3];
        gl.glUniform2f(gl.glGetUniformLocation(shaderProgram, "uNoiseScale"), (float)width/(float)NOISE_TEXTURE_WIDTH, (float)height/(float)NOISE_TEXTURE_HEIGHT);

        // TODO: may be enough to only send once when initializing?
        gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "uKernelSize"), kernelSize);
        gl.glUniform3fv(gl.glGetUniformLocation(shaderProgram, "uKernelOffsets"), kernelSize, kernel, 0);
        gl.glUniform1f(gl.glGetUniformLocation(shaderProgram, "uRadius"), ssaoRadius);
        gl.glActiveTexture(GL.GL_TEXTURE2);
		gl.glBindTexture(GL.GL_TEXTURE_2D, noiseTextureHandle);
        gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "NoiseTex"), 2);
	}

	/**
	 * Change the size of the SSAO kernel and recomputes it. If the size already
	 * matches nothing is done. This should be called before
	 * {@link #enableSSAOwithDepthMap(int)} to have any effect.
	 */
	public void setSSAOkernelSize(int kernelSize) {
		if (this.kernelSize != kernelSize) {
			this.kernelSize = kernelSize;
			generateSamplingMatrix();
		}
	}

	/**
	 * Change the radius used for SSAO. This should be called before
	 * {@link #enableSSAOwithDepthMap(int)} to have any effect.
	 */
	public void setSSAOradius(float radius) {
		this.ssaoRadius = radius;
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

	/**
	 * Sets the PMVMatrix that was used to render the shadow map set with {@link #bindShadowMap(int)}.
	 * This is needed to correctly compare the shadow map depth values with the fragment depth value.
	 * @param pmvMatrix
	 */
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
