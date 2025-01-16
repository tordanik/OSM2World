package org.osm2world.core.target.jogl;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2.*;
import static com.jogamp.opengl.GL2ES3.GL_QUADS;
import static java.awt.Color.WHITE;
import static org.osm2world.core.target.common.material.Material.Transparency.BINARY;
import static org.osm2world.core.target.common.material.Material.Transparency.TRUE;
import static org.osm2world.core.target.common.material.Material.multiplyColor;

import java.awt.*;
import java.io.File;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.common.material.ImageFileTexture;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.TextureData;
import org.osm2world.core.target.common.material.TextureData.Wrap;
import org.osm2world.core.target.common.material.TextureDataDimensions;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.target.common.texcoord.NamedTexCoordFunction;
import org.osm2world.core.target.jogl.JOGLRenderingParameters.Winding;
import org.osm2world.core.util.color.LColor;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.texture.Texture;

/**
 * JOGL target using the old fixed function OpenGL pipeline.
 */
public final class JOGLTargetFixedFunction extends AbstractJOGLTarget implements JOGLTarget {

	/** maximum number of texture layers any material can use */
	public static final int MAX_TEXTURE_LAYERS = 4;

	/** globally controls anisotropic filtering for all textures */
	private static final boolean ANISOTROPIC_FILTERING = true;

	private final GL2 gl;

	private Configuration config = new BaseConfiguration();

	private static @Nullable Winding frontFaceWinding = Winding.CCW;

	/**
	 * creates a new JOGLTarget for a given {@link GL2} interface. It is
	 * possible to have multiple targets that render to the same gl object.
	 *
	 * @param renderingParameters  global parameters for rendering;
	 *   see {@link #setRenderingParameters(JOGLRenderingParameters)}
	 * @param globalLightingParameters  global parameters for lighting;
	 *   see {@link #setGlobalLightingParameters(GlobalLightingParameters)}
	 */
	public JOGLTargetFixedFunction(GL2 gl, JOGLRenderingParameters renderingParameters,
			GlobalLightingParameters globalLightingParameters) {

		super(gl, renderingParameters, globalLightingParameters);
		this.gl = gl;

		reset();

	}

	public static void drawPrimitive(GL2 gl, int glPrimitiveType,
			List<VectorXYZ> vertices, List<VectorXYZ> normals,
			List<List<VectorXZ>> texCoordLists) {

		assert vertices.size() == normals.size();

		gl.glBegin(glPrimitiveType);

		for (int i = 0; i < vertices.size(); i++) {

			if (texCoordLists != null) {
				for (int texLayer = 0; texLayer < texCoordLists.size(); texLayer++) {
					VectorXZ textureCoord =	texCoordLists.get(texLayer).get(i);
					gl.glMultiTexCoord2d(getGLTextureConstant(texLayer),
							textureCoord.x, textureCoord.z);
				}
			}

			VectorXYZ n = normals.get(i);
			gl.glNormal3d(n.x, n.y,	-n.z);

			VectorXYZ v = vertices.get(i);
			gl.glVertex3d(v.x, v.y, -v.z);

		}

		gl.glEnd();

	}

	/**
	 * prepares a scene, based on the accumulated draw calls, for rendering.
	 */
	@Override
	public void finish() {

		if (isFinished()) return;

		renderer = new JOGLRendererVBOFixedFunction(gl, textureManager, primitiveBuffer);

	}

	/**
	 * similar to {@link #render(Camera, Projection)},
	 * but allows rendering only a part of the "normal" image.
	 * For example, with xStart=0, xEnd=0.5, yStart=0 and yEnd=1,
	 * only the left half of the full image will be rendered,
	 * but it will be stretched to cover the available space.
	 *
	 * Only supported for orthographic projections!
	 */
	@Override
	public void renderPart(Camera camera, Projection projection,
			double xStart, double xEnd, double yStart, double yEnd) {

		if (renderer == null) {
			throw new IllegalStateException("finish must be called first");
		}

		/* apply camera and projection information */

		applyProjectionMatricesForPart(gl, projection,
				xStart, xEnd, yStart, yEnd);

		applyCameraMatrices(gl, camera);

		/* apply global rendering parameters */

		applyRenderingParameters(gl, renderingParameters);
		applyLightingParameters(gl, globalLightingParameters, projection.isOrthographic());

		/* render primitives */

		renderer.render(camera, projection);

		for (NonAreaPrimitive nonAreaPrimitive : nonAreaPrimitives) {

			gl.glLineWidth(nonAreaPrimitive.width);

			Color c = nonAreaPrimitive.color;
			gl.glColor3f(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f);

			gl.glBegin(AbstractJOGLTarget.getGLConstant(nonAreaPrimitive.type));

			for (VectorXYZ v : nonAreaPrimitive.vs) {
		        gl.glVertex3d(v.getX(), v.getY(), -v.getZ());
			}

	        gl.glEnd();

		}

		finishRendering(gl);

	}

	/**
	 * Disables lighting and textures.
	 */
	static final void finishRendering(GL gl) {
		gl.glDisable(GL_LIGHT0);
		gl.glDisable(GL_LIGHTING);
		for (int i = 0; i < MAX_TEXTURE_LAYERS; i++) {
			gl.glActiveTexture(getGLTextureConstant(i));
			gl.glDisable(GL_TEXTURE_2D);
		}
	}

	static final void applyCameraMatrices(GL2 gl, Camera camera) {

    	gl.glLoadIdentity();

		VectorXYZ pos = camera.getPos();
		VectorXYZ lookAt = camera.getLookAt();
		VectorXYZ up = camera.getUp();
		new GLU().gluLookAt(
				pos.x, pos.y, -pos.z,
				lookAt.x, lookAt.y, -lookAt.z,
				up.x, up.y, -up.z);

	}

	static final void applyProjectionMatrices(GL2 gl, Projection projection) {
		applyProjectionMatricesForPart(gl, projection, 0, 1, 0, 1);
	}

	/**
	 * similar to {@link #applyProjectionMatrices(GL2, Projection)},
	 * but allows rendering only a part of the "normal" image.
	 */
	static final void applyProjectionMatricesForPart(GL2 gl, Projection projection,
			double xStart, double xEnd, double yStart, double yEnd) {

		if ((xStart != 0 || xEnd != 1 || yStart != 0 || yEnd != 1)
				&& !projection.isOrthographic()) {
			throw new IllegalArgumentException("section rendering only supported "
					+ "for orthographic projections");
		}

		gl.glMatrixMode(GL_PROJECTION);
		gl.glLoadIdentity();

		if (projection.isOrthographic()) {

			double volumeWidth = projection.getAspectRatio() * projection.getVolumeHeight();

			gl.glOrtho(
					(-0.5 + xStart) * volumeWidth,
					(-0.5 + xEnd  ) * volumeWidth,
					(-0.5 + yStart) * projection.getVolumeHeight(),
					(-0.5 + yEnd  ) * projection.getVolumeHeight(),
					projection.getNearClippingDistance(),
					projection.getFarClippingDistance());

		} else { //perspective

			new GLU().gluPerspective(
					projection.getVertAngle(),
					projection.getAspectRatio(),
					projection.getNearClippingDistance(),
					projection.getFarClippingDistance());

		}

		gl.glMatrixMode(GL_MODELVIEW);

	}

	static final void applyRenderingParameters(GL2 gl,
			JOGLRenderingParameters parameters) {

		/* back-face culling */

		frontFaceWinding = parameters.frontFace;

		if (frontFaceWinding == null) {
			gl.glDisable(GL_CULL_FACE);
		} else {
			gl.glFrontFace(frontFaceWinding.glConstant);
			gl.glCullFace(GL_BACK);
			gl.glEnable(GL_CULL_FACE);
		}

		/* wireframe mode */

		if (parameters.wireframe) {
    		gl.glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
    	} else {
    		gl.glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    	}

		/* z buffer */

		if (parameters.useZBuffer) {
			gl.glEnable(GL_DEPTH_TEST);
		} else {
			gl.glDisable(GL_DEPTH_TEST);
		}

	}

	static final void applyLightingParameters(GL2 gl,
			GlobalLightingParameters lighting, boolean orthographicProjection) {

		if (lighting == null) {

			gl.glDisable(GL_LIGHT0);
			gl.glDisable(GL_LIGHTING);

		} else {

			gl.glLightModelfv(GL_LIGHT_MODEL_AMBIENT,
					getFloatBuffer(lighting.globalAmbientColor));

			gl.glLightfv(GL_LIGHT0, GL_AMBIENT,
					getFloatBuffer(Color.BLACK));
			gl.glLightfv(GL_LIGHT0, GL_DIFFUSE,
					getFloatBuffer(lighting.lightColorDiffuse));
			gl.glLightfv(GL_LIGHT0, GL_SPECULAR,
					getFloatBuffer(lighting.lightColorSpecular));

			// make specular lighting computation more realistic
			gl.glLightModeli(GL2.GL_LIGHT_MODEL_LOCAL_VIEWER, orthographicProjection ? GL.GL_FALSE : GL.GL_TRUE);
			gl.glLightModeli(GL2.GL_LIGHT_MODEL_COLOR_CONTROL, GL2.GL_SEPARATE_SPECULAR_COLOR);

			gl.glLightfv(GL_LIGHT0, GL_POSITION, new float[] {
						(float)lighting.lightFromDirection.x,
						(float)lighting.lightFromDirection.y,
						-(float)lighting.lightFromDirection.z,
						0.0f}, 0);

			gl.glEnable(GL_LIGHT0);
			gl.glEnable(GL_LIGHTING);

		}

	}

	static final void setMaterial(GL2 gl, Material material,
			JOGLTextureManager textureManager) {

		int numTexLayers = 0;
		if (material.getTextureLayers() != null) {
			numTexLayers = material.getTextureLayers().size();
		}

		/* handle back-face culling and double-sided materials */

		if (frontFaceWinding == null || material.isDoubleSided()) {
			gl.glDisable(GL_CULL_FACE);
		} else {
			gl.glFrontFace(frontFaceWinding.glConstant);
			gl.glCullFace(GL_BACK);
			gl.glEnable(GL_CULL_FACE);
		}

		if (material.isDoubleSided()) {
			gl.glLightModeli(GL_LIGHT_MODEL_TWO_SIDE, GL_TRUE);
		} else {
			gl.glLightModeli(GL_LIGHT_MODEL_TWO_SIDE, GL_FALSE);
		}

		/* set lighting */
		// wrong as interpolation is meant for normals? light shading should always be smooth
		/*
		if (material.getInterpolation() == Interpolation.SMOOTH) {
			gl.glShadeModel(GL_SMOOTH);
		} else {
			gl.glShadeModel(GL_FLAT);
		}
		*/

		/* set color */

		Color c = WHITE;

		if (numTexLayers == 0) {
			c = material.getColor();
		} else if (material.getTextureLayers().get(0).colorable) {
			c = material.getTextureLayers().get(0).clampedBaseColorFactor(LColor.fromAWT(material.getColor())).toAWT();
		}

		//TODO: glMaterialfv could be redundant if color was used for ambient and diffuse
		gl.glColor3f(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f);

		gl.glMaterialfv(GL_FRONT, GL_AMBIENT,
				getFloatBuffer(multiplyColor(c, AMBIENT_FACTOR)));
		gl.glMaterialfv(GL_FRONT, GL_DIFFUSE,
				getFloatBuffer(multiplyColor(c, 1 - AMBIENT_FACTOR)));

		if (material.isDoubleSided()) {
			gl.glMaterialfv(GL_BACK, GL_AMBIENT,
					getFloatBuffer(multiplyColor(c, AMBIENT_FACTOR)));
			gl.glMaterialfv(GL_BACK, GL_DIFFUSE,
					getFloatBuffer(multiplyColor(c, 1 - AMBIENT_FACTOR)));
		}

		// specular lighting
		gl.glMaterialfv(GL.GL_FRONT, GL2.GL_SPECULAR, getFloatBuffer(
				multiplyColor(Color.WHITE, SPECULAR_FACTOR)));
		gl.glMateriali(GL.GL_FRONT, GL2.GL_SHININESS, SHININESS);

		/* set textures and associated parameters */

		for (int i = 0; i < MAX_TEXTURE_LAYERS; i++) {

			gl.glActiveTexture(getGLTextureConstant(i));

			if (i >= numTexLayers) {

				gl.glDisable(GL_TEXTURE_2D);

			} else {

				gl.glEnable(GL_TEXTURE_2D);

				if (material.getTransparency() == TRUE) {
					gl.glEnable(GL.GL_BLEND);
					/* GL.GL_SRC_ALPHA and GL.GL_ONE_MINUS_SRC_ALPHA for color blending produces correct results for color, while
					 * GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA produces correct alpha blended results: the blendfunction is in fact equal to 1-(1-SRC_APLHA)*(1-DST_APLHA)
					 */
					gl.glBlendFuncSeparate(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA, GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
				} else if (material.getTransparency() == BINARY) {
					gl.glAlphaFunc(GL_GREATER, 0.5f);
					gl.glEnable(GL_ALPHA_TEST);
				} else {
					gl.glDisable(GL.GL_BLEND);
					gl.glDisable(GL_ALPHA_TEST);
				}

				TextureData textureData = material.getTextureLayers().get(i).baseColorTexture;

				Texture texture = textureManager.getTextureForTextureData(textureData);
				texture.enable(gl); //TODO: should this be called every time?
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
				case CLAMP: wrap = GL_CLAMP_TO_EDGE; break;
				case REPEAT: wrap = GL_REPEAT; break;
				}

				gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrap);
		        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrap);


		        if (textureData.wrap == Wrap.CLAMP) {

		        	/* TODO: make the RGB configurable -  for some reason,
		        	 * it shows up in lowzoom even if fully transparent */
		        	gl.glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR,
		        			getFloatBuffer(new Color(1f, 1f, 1f, 0f)));

		        }

		        /* combination of texture layers */
	        	if (i == 0) {
		        	/* Cv = Cp × Cs (Cp = ?)
		        	 * Av = Ap × As (Ap = 1?)
		        	 */
		        	gl.glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
		        } else {

		        	/* Cv = Arg0×Arg2 + Arg1×(1-Arg2)
		        	 * Cv = Cs × As + Cp × (1-As)
		        	 * Arg0 = Cs
		        	 * Arg1 = Cp
		        	 * Arg2 = As
		        	 */
		        	gl.glTexEnvi( GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_COMBINE );
		        	gl.glTexEnvi( GL_TEXTURE_ENV, GL_COMBINE_RGB, GL_INTERPOLATE );

		        	gl.glTexEnvi( GL_TEXTURE_ENV, GL_SOURCE0_RGB, GL_TEXTURE );
		        	gl.glTexEnvi( GL_TEXTURE_ENV, GL_OPERAND0_RGB, GL_SRC_COLOR );

		        	gl.glTexEnvi( GL_TEXTURE_ENV, GL_SOURCE1_RGB, GL_PREVIOUS );
		        	gl.glTexEnvi( GL_TEXTURE_ENV, GL_OPERAND1_RGB, GL_SRC_COLOR );

		        	gl.glTexEnvi( GL_TEXTURE_ENV, GL_SOURCE2_RGB, GL_TEXTURE );
		        	gl.glTexEnvi( GL_TEXTURE_ENV, GL_OPERAND2_RGB, GL_SRC_ALPHA );

		        	/* Av = Arg0×Arg2 + Arg1×(1-Arg2)
		        	 * Av = 1 × As + Ad × (1-As)
		        	 * Arg0 = 1
		        	 * Arg1 = Ad
		        	 * Arg2 = As
		        	 */
		        	gl.glTexEnvi( GL_TEXTURE_ENV, GL2.GL_COMBINE_ALPHA, GL_INTERPOLATE );

		        	gl.glTexEnvi( GL_TEXTURE_ENV, GL2.GL_SOURCE0_ALPHA, GL2.GL_CONSTANT );
		        	gl.glTexEnvi( GL_TEXTURE_ENV, GL2.GL_OPERAND0_ALPHA, GL_SRC_ALPHA );

		        	float[] mycolor = {0.0f, 0.0f, 0.0f, 1.0f}; //RGB doesn't matter since its not used
		        	gl.glTexEnvfv(GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_COLOR, mycolor, 0);

		        	gl.glTexEnvi( GL_TEXTURE_ENV, GL2.GL_SOURCE1_ALPHA, GL_PREVIOUS );
		        	gl.glTexEnvi( GL_TEXTURE_ENV, GL2.GL_OPERAND1_ALPHA, GL_SRC_ALPHA );

		        	gl.glTexEnvi( GL_TEXTURE_ENV, GL2.GL_SOURCE2_ALPHA, GL_TEXTURE );
		        	gl.glTexEnvi( GL_TEXTURE_ENV, GL2.GL_OPERAND2_ALPHA, GL_SRC_ALPHA );
		        }

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

	@Override
	public final void drawBackgoundImage(File backgroundImage,
			int startPixelX, int startPixelY,
			int pixelWidth, int pixelHeight,
			JOGLTextureManager textureManager) {

		gl.glMatrixMode(GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glOrtho(0, 1, 0, 1, 0, 1);

		gl.glMatrixMode(GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();

		gl.glDepthMask( false );

		/* texture binding */

		gl.glEnable(GL_TEXTURE_2D);
		gl.glActiveTexture(GL_TEXTURE0);

		gl.glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);

		Texture backgroundTexture = textureManager.getTextureForTextureData(ImageFileTexture.create(
				backgroundImage, new TextureDataDimensions(1, 1), Wrap.REPEAT, NamedTexCoordFunction.GLOBAL_X_Z));

		backgroundTexture.enable(gl);
		backgroundTexture.bind(gl);

		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

		int texWidth = backgroundTexture.getImageWidth();
		int texHeight = backgroundTexture.getImageHeight();

		/* draw quad */

		gl.glBegin( GL_QUADS ); {
			gl.glTexCoord2f(
					(float) startPixelX / texWidth,
					(float) startPixelY / texHeight );
			gl.glVertex2f( 0, 0 );
			gl.glTexCoord2f(
					(float) (startPixelX + pixelWidth) / texWidth,
					(float) startPixelY / texHeight );
			gl.glVertex2f( 1f, 0 );
			gl.glTexCoord2f(
					(float) (startPixelX + pixelWidth) / texWidth,
					(float) (startPixelY + pixelHeight) / texHeight );
			gl.glVertex2f( 1f, 1f );
			gl.glTexCoord2f(
					(float) startPixelX / texWidth,
					(float) (startPixelY + pixelHeight) / texHeight );
			gl.glVertex2f( 0, 1f );
		} gl.glEnd();

		/* restore some settings */

		gl.glDepthMask( true );

		gl.glPopMatrix();
		gl.glMatrixMode(GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GL_MODELVIEW);
		gl.glDisable(GL_TEXTURE_2D);

	}

	@Override
	public void setXZBoundary(AxisAlignedRectangleXZ boundary) {}

}

