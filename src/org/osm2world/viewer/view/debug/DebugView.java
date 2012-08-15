package org.osm2world.viewer.view.debug;

import static java.util.Arrays.asList;

import java.awt.Color;

import javax.media.opengl.GL2;

import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.heightmap.data.CellularTerrainElevation;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.material.ImmutableMaterial;
import org.osm2world.core.target.common.material.Material.Lighting;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.target.jogl.JOGLRenderingParameters;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.terrain.data.Terrain;

/**
 * contains some common methods for debug views
 */
public abstract class DebugView {
	
	protected MapData map;
	protected Terrain terrain;
	protected CellularTerrainElevation eleData;
	
	protected Camera camera;
	protected Projection projection;
	
	private JOGLTarget target = null;
	
	public void setConversionResults(Results conversionResults) {
	
		this.map = conversionResults.getMapData();
		this.terrain = conversionResults.getTerrain();
		this.eleData = conversionResults.getEleData();
		
		if (target != null) {
			target.reset();
		}
		
	}
	
	/**
	 * returns true if this DebugView can currently be used for rendering.
	 * By default, this checks whether all the setters have been used with
	 * non-null values, but subclasses can overwrite it with their own checks.
	 */
	public boolean canBeUsed() {
		return map != null
			&& terrain != null
			&& eleData != null;
	}
	
	/**
	 * returns a description of the debug view
	 */
	public String getDescription() {
		return "";
	}
	
	/**
	 * renders the content added by {@link #fillTarget(JOGLTarget)}.
	 * Only has an effect if {@link #canBeUsed()} is true.
	 */
	public void renderTo(GL2 gl, Camera camera, Projection projection) {
		
		if (canBeUsed() && camera != null && projection != null) {
					
			if (target == null) {
				target = new JOGLTarget(gl, new JOGLRenderingParameters(
						null, false, true), null);
				//TODO: what if gl has changed? Should be set in DebugView constructor.
			}
			
			boolean viewChanged = !camera.equals(this.camera)
					|| !projection.equals(this.projection);
			
			this.camera = camera;
			this.projection = projection;
			
			if (!target.isFinished()) {
				
				fillTarget(target);
				
				target.finish();
				
			} else {
				
				updateTarget(target, viewChanged);
				
				target.finish();
				
			}
			
			target.render(camera, projection);
			
		}
	}
		
	/**
	 * lets the subclass add all content and settings for rendering.
	 * Will only be called if {@link #canBeUsed()} is true.
	 */
	protected abstract void fillTarget(JOGLTarget target);
	
	/**
	 * lets the subclass update the target after the initial
	 * {@link #fillTarget(JOGLTarget)}.
	 * 
	 * @param viewChanged  true if camera or projection have changed
	 */
	protected void updateTarget(JOGLTarget target, boolean viewChanged) {};
	
	protected static final void drawBoxAround(JOGLTarget target,
			VectorXZ center, Color color, float halfWidth) {
		drawBoxAround(target, center.xyz(0), color, halfWidth);
	}

	protected static final void drawBoxAround(JOGLTarget target,
			VectorXYZ center, Color color, float halfWidth) {
		drawBox(target, color,
			new VectorXYZ(center.x - halfWidth,
				center.y,
				center.z - halfWidth),
			new VectorXYZ(center.x - halfWidth,
				center.y,
				center.z + halfWidth),
			new VectorXYZ(center.x + halfWidth,
				center.y,
				center.z + halfWidth),
			new VectorXYZ(center.x + halfWidth,
				center.y,
				center.z - halfWidth));
	}
	
	protected static final void drawBox(JOGLTarget target, Color color,
			VectorXYZ v1, VectorXYZ v2, VectorXYZ v3, VectorXYZ v4) {
		target.drawLineLoop(color, 1, asList(v1, v2, v3, v4));
	}
	
	protected static final void drawArrow(JOGLTarget target, Color color,
			float headLength, VectorXYZ... vs) {
		
		target.drawLineStrip(color, 1, vs);
		
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
		
		
		ImmutableMaterial colorMaterial =
				new ImmutableMaterial(Lighting.FLAT, color);
		
		target.drawTriangleStrip(colorMaterial, asList(
				lastV,
				headStart.subtract(endDirXZ.mult(headLength/2)),
				headStart.add(endDirXZ.mult(headLength/2))),
				null);
		
		target.drawTriangleStrip(colorMaterial, asList(
				lastV,
				headStart.subtract(endNormalXZ.mult(headLength/2)),
				headStart.add(endNormalXZ.mult(headLength/2))),
				null);
		
	}
	
}
