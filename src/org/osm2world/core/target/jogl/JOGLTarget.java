package org.osm2world.core.target.jogl;

import static javax.media.opengl.GL.*;
import static javax.media.opengl.GL2.*;
import static javax.media.opengl.GL2ES1.*;
import static javax.media.opengl.fixedfunc.GLLightingFunc.*;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.*;

import java.awt.Color;
import java.awt.Font;
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
		
		assert vertices.size() == normals.size();
		
		setMaterial(gl, material, textureManager);
		
		gl.glBegin(getGLConstant(type));
        		
		for (int i = 0; i < vertices.size(); i++) {
			gl.glNormal3d(normals.get(i).x, normals.get(i).y, -normals.get(i).z);
	        gl.glVertex3d(vertices.get(i).x, vertices.get(i).y, -vertices.get(i).z);
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
	
//	//TODO: own class for Texture, so Target classes can offer load texture
//	public void drawBillboard(VectorXYZ center, float halfWidth, float halfHeight,
//			Texture texture, Camera camera) {
//
//		VectorXYZ right = camera.getRight();
//		double rightXScaled = halfWidth*right.getX();
//		double rightZScaled = halfWidth*right.getZ();
//
//		TextureCoords tc = texture.getImageTexCoords();
//
//    	gl.glColor3f(1, 1, 1);
//
//		gl.glEnable(GL_TEXTURE_2D);
//        gl.glEnable(GL_BLEND);
//        gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//        gl.glEnable(GL_ALPHA_TEST);
//        gl.glAlphaFunc(GL_GREATER, 0);
//        //TODO: disable calls?
//
//        gl.glBegin(GL_QUADS);
//
//		texture.bind();
//
//		gl.glTexCoord2f(tc.left(), tc.bottom());
//        gl.glVertex3d(
//        		center.getX() - rightXScaled,
//        		center.getY() - halfHeight,
//        		-(center.getZ() - rightZScaled));
//
//		gl.glTexCoord2f(tc.right(), tc.bottom());
//        gl.glVertex3d(
//        		center.getX() + rightXScaled,
//        		center.getY() - halfHeight,
//        		-(center.getZ() + rightZScaled));
//
//		gl.glTexCoord2f(tc.right(), tc.top());
//        gl.glVertex3d(
//        		center.getX() + rightXScaled,
//        		center.getY() + halfHeight,
//        		-(center.getZ() + rightZScaled));
//
//		gl.glTexCoord2f(tc.left(), tc.top());
//        gl.glVertex3d(
//        		center.getX() - rightXScaled,
//        		center.getY() + halfHeight,
//        		-(center.getZ() - rightZScaled));
//
//        gl.glDisable(GL_TEXTURE_2D);
//
//	}
//
//	public static Texture loadTexture(String fileName) throws GLException, IOException
//	{
//	  File file = new File("resources" + File.separator + fileName);
//	  Texture result = null;
//
//	  result = TextureIO.newTexture(file, false);
//	  result.setTexParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR); //TODO (performance): GL_NEAREST for performance?
//	  result.setTexParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR); //TODO (performance): GL_NEAREST for performance?
//
//	  return result;
//	}

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

	public static final void setCameraMatrices(GL2 gl, Camera camera) {
		VectorXYZ pos = camera.getPos();
		VectorXYZ lookAt = camera.getLookAt();
//		VectorXYZ dir = lookAt.subtract(pos);
//		VectorXYZ right = dir.cross(VectorXYZ.Y_UNIT).normalize();
//		VectorXYZ up = right.cross(dir);
		new GLU().gluLookAt(
				pos.x, pos.y, -pos.z,
				lookAt.x, lookAt.y, -lookAt.z,
				0, 1f, 0f);
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

	public static final void setMaterial(GL2 gl, Material material,
			JOGLTextureManager textureManager) {
		
		if (material.getLighting() == Lighting.SMOOTH) {
			gl.glShadeModel(GL_SMOOTH);
		} else {
			gl.glShadeModel(GL_FLAT);
		}

		boolean textured = material.getTextureDataList().size() > 0;

		if (!textured || material.getTextureDataList().get(0).colorable) {
			setFrontMaterialColor(gl, GL_AMBIENT, material.ambientColor());
			setFrontMaterialColor(gl, GL_DIFFUSE, material.diffuseColor());
		} else {
			setFrontMaterialColor(gl, GL_AMBIENT, Material.multiplyColor(
					Color.WHITE, material.getAmbientFactor()));
			setFrontMaterialColor(gl, GL_DIFFUSE, Material.multiplyColor(
					Color.WHITE, material.getDiffuseFactor()));
		}
		
		if (!textured) {
			gl.glDisable(GL_TEXTURE_2D);
		} else {
			
			TextureData textureData = material.getTextureDataList().get(0);
						
			gl.glEnable(GL_TEXTURE_2D);
	        
			//gl.glEnable(GL_BLEND);
			//gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			
	        Texture texture = textureManager.getTextureForFile(textureData.file);
	        texture.enable(gl); //TODO: should this be called every time?
	        texture.bind(gl);

			int wrap = (textureData.wrap == Wrap.CLAMP) ?
					GL_CLAMP : GL_REPEAT;
			gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrap);
	        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrap);

	        gl.glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
		    
		}
		
	}

	public static final void setFrontMaterialColor(GL2 gl, int pname, Color color) {
		float ambientColor[] = {0, 0, 0, 1};
		color.getRGBColorComponents(ambientColor);
		gl.glMaterialfv(GL_FRONT, pname, FloatBuffer.wrap(ambientColor));
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
	
}
