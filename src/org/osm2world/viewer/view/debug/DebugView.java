package org.osm2world.viewer.view.debug;

import java.awt.Color;

import javax.media.opengl.GL2;

import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.heightmap.data.CellularTerrainElevation;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.target.jogl.RenderableToJOGL;
import org.osm2world.core.target.primitivebuffer.PrimitiveBuffer;
import org.osm2world.core.terrain.data.Terrain;

/**
 * contains some common methods for debug views
 */
public abstract class DebugView implements RenderableToJOGL {

	protected MapData map;
	protected Terrain terrain;
	protected CellularTerrainElevation eleData;
	
	protected PrimitiveBuffer mapDataPrimitiveBuffer;
	protected PrimitiveBuffer terrainPrimitiveBuffer;
	
	public void setConversionResults(Results conversionResults) {
		this.map = conversionResults.getMapData();
		this.terrain = conversionResults.getTerrain();
		this.eleData = conversionResults.getEleData();
	}
	
	public void setPrimitiveBuffers(PrimitiveBuffer mapDataPrimitiveBuffer,
			PrimitiveBuffer terrainPrimitiveBuffer) {
		this.mapDataPrimitiveBuffer = mapDataPrimitiveBuffer;
		this.terrainPrimitiveBuffer = terrainPrimitiveBuffer;
	}
	
	/**
	 * returns true if this DebugView can currently be used for rendering.
	 * By default, this checks whether all the setters have been used with
	 * non-null values, but subclasses can overwrite it with their own checks.
	 */
	public boolean canBeUsed() {
		return map != null
			&& terrain != null
			&& eleData != null
			&& mapDataPrimitiveBuffer != null
			&& terrainPrimitiveBuffer != null;
	}
	
	/**
	 * returns a description of the debug view
	 */
	public String getDescription() {
		return "";
	}
	
	@Override
	public void renderTo(GL2 gl, Camera camera) {
		if (canBeUsed()) {
			renderToImpl(gl, camera);
		}
	}
	
	/**
	 * implementation for the renderTo method, provided by subclasses.
	 * Will only be called if the DebugView {@link #canBeUsed()}.
	 */
	protected abstract void renderToImpl(GL2 gl, Camera camera);
	
	
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
		target.drawLineStrip(color, v1, v2, v3, v4, v1);
	}
	
}
