package org.osm2world.core.target.jogl;

import static java.util.Arrays.asList;
import static javax.media.opengl.GL.*;
import static javax.media.opengl.GL2.*;
import static org.osm2world.core.target.common.material.Material.multiplyColor;
import static org.osm2world.core.target.common.material.Material.Transparency.*;
import static org.osm2world.core.target.jogl.NonAreaPrimitive.Type.*;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.osm2world.core.math.Vector3D;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.Primitive;
import org.osm2world.core.target.common.Primitive.Type;
import org.osm2world.core.target.common.PrimitiveTarget;
import org.osm2world.core.target.common.TextureData;
import org.osm2world.core.target.common.TextureData.Wrap;
import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Lighting;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;

import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.texture.Texture;

public final class JOGLTarget extends PrimitiveTarget<RenderableToJOGL> {
	
	/** maximum number of texture layers any material can use */
	public static final int MAX_TEXTURE_LAYERS = 4;
	
	/** globally controls anisotropic filtering for all textures */
	private static final boolean ANISOTROPIC_FILTERING = true;
	
	private final GL2 gl;
	
	private final JOGLTextureManager textureManager;
	
	private List<NonAreaPrimitive> nonAreaPrimitives;
	private PrimitiveBuffer primitiveBuffer;
	private JOGLRenderer renderer;

	private JOGLRenderingParameters renderingParameters;
	private GlobalLightingParameters globalLightingParameters;
	
	private Configuration config = new BaseConfiguration();
	
	/**
	 * creates a new JOGLTarget for a given {@link GL2} interface. It is
	 * possible to have multiple targets that render to the same gl object.
	 * 
	 * @param renderingParameters  global parameters for rendering;
	 *   see {@link #setRenderingParameters(JOGLRenderingParameters)}
	 * @param globalLightingParameters  global parameters for lighting;
	 *   see {@link #setGlobalLightingParameters(GlobalLightingParameters)}
	 */
	public JOGLTarget(GL2 gl, JOGLRenderingParameters renderingParameters,
			GlobalLightingParameters globalLightingParameters) {
		
		this.gl = gl;
		this.renderingParameters = renderingParameters;
		this.globalLightingParameters = globalLightingParameters;
		
		this.textureManager = new JOGLTextureManager(gl);
		
		reset();
		
	}
	
	@Override
	public Class<RenderableToJOGL> getRenderableType() {
		return RenderableToJOGL.class;
	}
		
	/**
	 * discards all accumulated draw calls
	 */
	public void reset() {
		
		this.nonAreaPrimitives = new ArrayList<NonAreaPrimitive>();
		
		this.primitiveBuffer = new PrimitiveBuffer();
		
		if (renderer != null) {
			renderer.freeResources();
			renderer = null;
		}
		
	}
	
	@Override
	public void render(RenderableToJOGL renderable) {
		renderable.renderTo(this);
	}

	@Override
	protected void drawPrimitive(Primitive.Type type, Material material,
			List<VectorXYZ> vertices, List<VectorXYZ> normals,
			List<List<VectorXZ>> texCoordLists) {
		
		primitiveBuffer.drawPrimitive(type, material, vertices, normals, texCoordLists);
		
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
	
	private void drawNonAreaPrimitive(NonAreaPrimitive.Type type,
			Color color, int width, List<VectorXYZ> vs) {
		
		nonAreaPrimitives.add(new NonAreaPrimitive(
				type, color, width, vs));
        
	}
	
	public void drawPoints(Color color, VectorXYZ... vs) {
		drawNonAreaPrimitive(POINTS, color, 1, asList(vs));
	}
	
	public void drawLineStrip(Color color, int width, VectorXYZ... vs) {
		drawNonAreaPrimitive(LINE_STRIP, color, width, asList(vs));
	}
	
	public void drawLineStrip(Color color, int width, List<VectorXYZ> vs) {
		drawNonAreaPrimitive(LINE_STRIP, color, width, vs);
	}
	
	public void drawLineLoop(Color color, int width, List<VectorXYZ> vs) {
		drawNonAreaPrimitive(LINE_LOOP, color, width, vs);
	}
	
	/**
	 * set global lighting parameters. Using this method affects all primitives
	 * (even those from previous draw calls).
	 * 
	 * @param parameters  parameter object; null disables lighting
	 */
	public void setGlobalLightingParameters(
			GlobalLightingParameters parameters) {
		
		this.globalLightingParameters = parameters;
		
	}
	
	/**
	 * set global rendering parameters. Using this method affects all primitives
	 * (even those from previous draw calls).
	 */
	public void setRenderingParameters(
			JOGLRenderingParameters renderingParameters) {
		
		this.renderingParameters = renderingParameters;
		
	}
	
	public void setConfiguration(Configuration config) {
		this.config = config;
	}
	
	/**
	 * prepares a scene, based on the accumulated draw calls, for rendering.
	 */
	@Override
	public void finish() {
		
		if (isFinished()) return;
		
		if ("DisplayList".equals(config.getString("joglImplementation"))) {
			renderer = new JOGLRendererDisplayList(
					gl, textureManager, primitiveBuffer);
		} else {
			renderer = new JOGLRendererVBO(
					gl, textureManager, primitiveBuffer);
		}
		
	}
	
	public boolean isFinished() {
		return renderer != null;
	}
	
	public void render(Camera camera, Projection projection) {
		renderPart(camera, projection, 0, 1, 0, 1);
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
		applyLightingParameters(gl, globalLightingParameters);
		
		/* render primitives */
		
		renderer.render(camera, projection);
		
		for (NonAreaPrimitive nonAreaPrimitive : nonAreaPrimitives) {
			
			gl.glLineWidth(nonAreaPrimitive.width);
			
			Color c = nonAreaPrimitive.color;
			gl.glColor3f(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f);
			
			gl.glBegin(getGLConstant(nonAreaPrimitive.type));
	        
			for (VectorXYZ v : nonAreaPrimitive.vs) {
		        gl.glVertex3d(v.getX(), v.getY(), -v.getZ());
			}
			
	        gl.glEnd();
			
		}
		
	}
	
	public void freeResources() {
		
		textureManager.releaseAll();
		
		reset();
		
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
		
		/* backface culling */
		
		if (parameters.frontFace == null) {
			gl.glDisable(GL_CULL_FACE);
		} else {
			gl.glFrontFace(GL_CCW);
			gl.glCullFace(GL_BACK);
			gl.glEnable (GL_CULL_FACE);
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
			GlobalLightingParameters lighting) {
		
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
		if (material.getTextureDataList() != null) {
			numTexLayers = material.getTextureDataList().size();
		}
		
		/* set lighting */
		
		if (material.getLighting() == Lighting.SMOOTH) {
			gl.glShadeModel(GL_SMOOTH);
		} else {
			gl.glShadeModel(GL_FLAT);
		}
		
		/* set color */
		
		if (numTexLayers == 0 || material.getTextureDataList().get(0).colorable) {
			
			//TODO: glMaterialfv could be redundant if color was used for ambient and diffuse
			Color c = material.getColor();
			gl.glColor3f(c.getRed()/255f, c.getGreen()/255f, c.getBlue());
			
			gl.glMaterialfv(GL_FRONT, GL_AMBIENT,
					getFloatBuffer(material.ambientColor()));
			gl.glMaterialfv(GL_FRONT, GL_DIFFUSE,
					getFloatBuffer(material.diffuseColor()));
			
		} else {
			
			gl.glColor3f(1, 1, 1);
			
			gl.glMaterialfv(GL_FRONT, GL_AMBIENT, getFloatBuffer(
					multiplyColor(Color.WHITE, material.getAmbientFactor())));
			gl.glMaterialfv(GL_FRONT, GL_DIFFUSE, getFloatBuffer(
					multiplyColor(Color.WHITE, material.getDiffuseFactor())));
			
		}
		
		/* set textures and associated parameters */
		
		for (int i = 0; i < MAX_TEXTURE_LAYERS; i++) {
			
			gl.glActiveTexture(getGLTextureConstant(i));
						
			if (i >= numTexLayers) {
				
				gl.glDisable(GL_TEXTURE_2D);
								
			} else {
				
				gl.glEnable(GL_TEXTURE_2D);
				
				if (material.getTransparency() == TRUE) {
					gl.glEnable(GL.GL_BLEND);
					gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
				} else if (material.getTransparency() == BINARY) {
					gl.glAlphaFunc(GL_GREATER, 0.5f);
					gl.glEnable(GL_ALPHA_TEST);
				} else {
					gl.glDisable(GL.GL_BLEND);
					gl.glDisable(GL_ALPHA_TEST);
				}
				
				TextureData textureData = material.getTextureDataList().get(i);
				
				Texture texture = textureManager.getTextureForFile(textureData.file);
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
				case CLAMP: wrap = GL_CLAMP; break;
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
				
		        /* combination of texture layers */
		        
		        if (i == 0) {
		        	gl.glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
		        } else {
		        	
		        	gl.glTexEnvi( GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_COMBINE );
		        	gl.glTexEnvi( GL_TEXTURE_ENV, GL_COMBINE_RGB, GL_INTERPOLATE );

		        	gl.glTexEnvi( GL_TEXTURE_ENV, GL_SOURCE0_RGB, GL_TEXTURE );
		        	gl.glTexEnvi( GL_TEXTURE_ENV, GL_OPERAND0_RGB, GL_SRC_COLOR );

		        	gl.glTexEnvi( GL_TEXTURE_ENV, GL_SOURCE1_RGB, GL_PREVIOUS );
		        	gl.glTexEnvi( GL_TEXTURE_ENV, GL_OPERAND1_RGB, GL_SRC_COLOR );

		        	gl.glTexEnvi( GL_TEXTURE_ENV, GL_SOURCE2_RGB, GL_TEXTURE );
		        	gl.glTexEnvi( GL_TEXTURE_ENV, GL_OPERAND2_RGB, GL_SRC_ALPHA );

		        }
		        
			}
			
		}
		
	}

	static final FloatBuffer getFloatBuffer(Color color) {
		float colorArray[] = {0, 0, 0, color.getAlpha() / 255f};
		color.getRGBColorComponents(colorArray);
		return FloatBuffer.wrap(colorArray);
	}
	
	static final int getGLConstant(Type type) {
		switch (type) {
		case TRIANGLE_STRIP: return GL_TRIANGLE_STRIP;
		case TRIANGLE_FAN: return GL_TRIANGLE_FAN;
		case TRIANGLES: return GL_TRIANGLES;
		case CONVEX_POLYGON: return GL_POLYGON;
		default: throw new Error("programming error: unhandled primitive type");
		}
	}

	static final int getGLConstant(NonAreaPrimitive.Type type) {
		switch (type) {
		case POINTS: return GL_POINTS;
		case LINES: return GL_LINES;
		case LINE_STRIP: return GL_LINE_STRIP;
		case LINE_LOOP: return GL_LINE_LOOP;
		default: throw new Error("programming error: unhandled primitive type");
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
	
	/**
	 * clears the rendering surface and the z buffer
	 * 
	 * @param clearColor  background color before rendering any primitives;
	 *                     null uses a previously defined clear color
	 */
	public static final void clearGL(GL2 gl, Color clearColor) {
		
		if (clearColor != null) {
			float[] c = {0f, 0f, 0f};
			clearColor.getColorComponents(c);
			gl.glClearColor(c[0], c[1], c[2], 1.0f);
		}
		
		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
	}
	
	private static final TextRenderer textRenderer = new TextRenderer(
			new Font("SansSerif", Font.PLAIN, 12), true, false);
	//needs quite a bit of memory, so it must not create an instance for each use!
	
	public static final void drawText(String string, Vector3D pos, Color color) {
		textRenderer.setColor(color);
		textRenderer.begin3DRendering();
		textRenderer.draw3D(string,
				(float)pos.getX(), (float)pos.getY(), -(float)pos.getZ(),
				0.05f);
	}

	public static final void drawText(String string, int x, int y,
			int screenWidth, int screenHeight, Color color) {
		textRenderer.beginRendering(screenWidth, screenHeight);
		textRenderer.setColor(color);
		textRenderer.draw(string, x, y);
		textRenderer.endRendering();
	}
	
	public static final void drawBackgoundImage(GL2 gl, File backgroundImage,
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
		
		Texture backgroundTexture =
				textureManager.getTextureForFile(backgroundImage);

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
		
	}
	
}
