package org.osm2world.core.target.jogl;

import static javax.media.opengl.GL.*;
import static javax.media.opengl.GL2.*;
import static javax.media.opengl.GL2ES1.*;
import static javax.media.opengl.fixedfunc.GLLightingFunc.*;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.*;
import static org.osm2world.core.target.common.material.Material.multiplyColor;
import static org.osm2world.core.target.common.material.Material.Transparency.*;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

import org.osm2world.core.math.TriangleXYZ;
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

public class JOGLTarget extends PrimitiveTarget<RenderableToJOGL> {
		
	private final GL2 gl;
	private final Camera camera;

	private final JOGLTextureManager textureManager;
	
	public JOGLTarget(GL2 gl, Camera camera) {
		this.gl = gl;
		this.camera = camera;
		this.textureManager = new JOGLTextureManager(gl);
	}
	
	@Override
	public Class<RenderableToJOGL> getRenderableType() {
		return RenderableToJOGL.class;
	}
	
	@Override
	public void render(RenderableToJOGL renderable) {
		renderable.renderTo(gl, camera);
	}

	@Override
	protected void drawPrimitive(Primitive.Type type, Material material,
			List<VectorXYZ> vertices, List<VectorXYZ> normals,
			List<List<VectorXZ>> texCoordLists) {
		
		setMaterial(gl, material, textureManager);
		
		drawPrimitive(gl, getGLConstant(type), vertices, normals, texCoordLists);
		
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
	
	private void drawPrimitive(int primitive, Color color,
			List<? extends VectorXYZ> vs) {
		
		gl.glColor3f(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f);
		
		gl.glBegin(primitive);
        
		for (VectorXYZ v : vs) {
	        gl.glVertex3d(v.getX(), v.getY(), -v.getZ());
		}
		
        gl.glEnd();
        
	}
	
	public void drawPoints(Color color, VectorXYZ... vs) {
		drawPrimitive(GL_POINTS, color, Arrays.asList(vs));
	}

	public void drawLineStrip(Color color, VectorXYZ... vs) {
		drawLineStrip(color, Arrays.asList(vs));
	}
	
	public void drawLineStrip(Color color, List<VectorXYZ> vs) {
		drawPrimitive(GL_LINE_STRIP, color, vs);
	}
	
	public void drawLineStrip(Color color, int width, VectorXYZ... vs) {
		gl.glLineWidth(width);
		drawLineStrip(color, vs);
		gl.glLineWidth(1);
	}

	public void drawLineLoop(Color color, List<? extends VectorXYZ> vs) {
		drawPrimitive(GL_LINE_LOOP, color, vs);
	}

	public void drawArrow(Color color, float headLength, VectorXYZ... vs) {
		
		drawLineStrip(color, vs);
		
		/* draw head */
		
		VectorXYZ lastV = VectorXYZ.xyz(vs[vs.length-1]);
		VectorXYZ slastV = VectorXYZ.xyz(vs[vs.length-2]);
		
		VectorXYZ endDir = lastV.subtract(slastV).normalize();
		VectorXYZ headStart = lastV.subtract(endDir.mult(headLength));
		
		VectorXZ endDirXZ = endDir.xz();
		if (endDirXZ.lengthSquared() < 0.01) { //(almost) vertical vector
			endDirXZ = VectorXZ.X_UNIT;
		} else {
			endDirXZ = endDirXZ.normalize();
		}
		VectorXZ endNormalXZ = endDirXZ.rightNormal();
				
		drawTriangleStrip(color,
				lastV,
				headStart.add(endDirXZ.mult(headLength/2)),
				headStart.subtract(endDirXZ.mult(headLength/2)));
        		
		drawTriangleStrip(color,
				lastV,
				headStart.add(endNormalXZ.mult(headLength/2)),
				headStart.subtract(endNormalXZ.mult(headLength/2)));
		
	}
	
	public void drawTriangleStrip(Color color, VectorXYZ... vs) {
		drawPrimitive(GL_TRIANGLE_STRIP, color, Arrays.asList(vs));
	}
	
	public void drawTriangles(Color color, Collection<TriangleXYZ> triangles) {
		
		gl.glColor3f(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f);
		gl.glBegin(GL_TRIANGLES);
        
		for (TriangleXYZ triangle : triangles) {
	        gl.glVertex3d(triangle.v1.x, triangle.v1.y, -triangle.v1.z);
	        gl.glVertex3d(triangle.v2.x, triangle.v2.y, -triangle.v2.z);
	        gl.glVertex3d(triangle.v3.x, triangle.v3.y, -triangle.v3.z);
		}

        gl.glEnd();
        
	}

	public void drawPolygon(Color color, VectorXYZ... vs) {
		drawPrimitive(GL_POLYGON, color, Arrays.asList(vs));
	}
	
	private static final TextRenderer textRenderer = new TextRenderer(
			new Font("SansSerif", Font.PLAIN, 12), true, false);
	//needs quite a bit of memory, so it must not be created for each instance!
	
	public void drawText(String string, Vector3D pos, Color color) {
		textRenderer.setColor(color);
		textRenderer.begin3DRendering();
		textRenderer.draw3D(string, (float)pos.getX(), (float)pos.getY(), -(float)pos.getZ(), 0.05f);
	}

	public void drawText(String string, int x, int y,
			int screenWidth, int screenHeight, Color color) {
		textRenderer.beginRendering(screenWidth, screenHeight);
		textRenderer.setColor(color);
		textRenderer.draw(string, x, y);
		textRenderer.endRendering();
	}
	
	@Override
	public void finish() {
		textureManager.releaseAll();
	}

	public static final int MAX_TEXTURE_LAYERS = 4;
	
	public static final void setCameraMatrices(GL2 gl, Camera camera) {
		VectorXYZ pos = camera.getPos();
		VectorXYZ lookAt = camera.getLookAt();
		VectorXYZ up = camera.getUp();
		new GLU().gluLookAt(
				pos.x, pos.y, -pos.z,
				lookAt.x, lookAt.y, -lookAt.z,
				up.x, up.y, -up.z);
	}
	
	public static final void setProjectionMatrices(GL2 gl, Projection projection) {
		setProjectionMatricesForPart(gl, projection, 0, 1, 0, 1);
	}

	/**
	 * similar to {@link #setProjectionMatrices(GL, Projection)},
	 * but allows rendering only a part of the "normal" image.
	 * For example, with xStart=0, xEnd=0.5, yStart=0 and yEnd=1,
	 * only the left half of the full image will be rendered,
	 * but it will be stretched to cover the available space.
	 * 
	 * Only supported for orthographic projections!
	 */
	public static final void setProjectionMatricesForPart(GL2 gl, Projection projection,
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

	public static final void setLightingParameters(GL2 gl,
			GlobalLightingParameters lighting) {

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
	
	public static final void setMaterial(GL2 gl, Material material,
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
			gl.glMaterialfv(GL_FRONT, GL_AMBIENT,
					getFloatBuffer(material.ambientColor()));
			gl.glMaterialfv(GL_FRONT, GL_DIFFUSE,
					getFloatBuffer(material.diffuseColor()));
		} else {
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

				int wrap = (textureData.wrap == Wrap.CLAMP) ?
						GL_CLAMP : GL_REPEAT;
				gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrap);
		        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrap);

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

	public static final FloatBuffer getFloatBuffer(Color color) {
		float colorArray[] = {0, 0, 0, 1};
		color.getRGBColorComponents(colorArray);
		return FloatBuffer.wrap(colorArray);
	}

	public static final int getGLConstant(Type type) {
		switch (type) {
		case TRIANGLE_STRIP: return GL_TRIANGLE_STRIP;
		case TRIANGLE_FAN: return GL_TRIANGLE_FAN;
		case TRIANGLES: return GL_TRIANGLES;
		case CONVEX_POLYGON: return GL_POLYGON;
		default: throw new Error("programming error: unhandled primitive type");
		}
	}

	public static final int getGLTextureConstant(int textureNumber) {
		switch (textureNumber) {
		case 0: return GL_TEXTURE0;
		case 1: return GL_TEXTURE1;
		case 2: return GL_TEXTURE2;
		case 3: return GL_TEXTURE3;
		default: throw new Error("programming error: unhandled texture number");
		}
	}

	public static void drawBackgoundImage(GL2 gl, File backgroundImage,
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
