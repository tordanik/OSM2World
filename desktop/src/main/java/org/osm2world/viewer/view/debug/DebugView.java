package org.osm2world.viewer.view.debug;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.awt.*;
import java.util.List;

import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.conversion.O2WConfig;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_elevation.creation.TerrainElevationData;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.TriangleXYZ;
import org.osm2world.core.target.common.material.ImmutableMaterial;
import org.osm2world.core.target.common.material.Material.Interpolation;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.target.jogl.JOGLRenderingParameters;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.target.jogl.JOGLTargetFixedFunction;
import org.osm2world.core.target.jogl.JOGLTargetShader;

import com.jogamp.opengl.GL;

/**
 * contains some common methods for debug views
 */
public abstract class DebugView {

	protected O2WConfig config;

	protected MapData map;
	protected TerrainElevationData eleData;

	protected Camera camera;
	protected Projection projection;

	// camera attributes that are used to detect changes
	private VectorXYZ cameraPos;
	private VectorXYZ cameraUp;
	private VectorXYZ cameraLookAt;

	private JOGLTarget target = null;
	private boolean targetNeedsReset;

	public final void setConfiguration(O2WConfig config) {

		this.config = config;

		if (target != null) {
			target.setConfiguration(config);
			targetNeedsReset = true;
		}

	}

	public void reset() {
		if (target != null) {
			target.freeResources();
			target = null;
		}
	}

	public void setConversionResults(Results conversionResults) {

		this.map = conversionResults.getMapData();
		this.eleData = conversionResults.getEleData();

		targetNeedsReset = true;
	}

	/**
	 * returns true if this DebugView can currently be used for rendering.
	 * By default, this checks whether all the setters have been used with
	 * non-null values, but subclasses can overwrite it with their own checks.
	 */
	public boolean canBeUsed() {
		return map != null
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
	 *
	 * @param gl  needs to be the same gl as in previous calls
	 */
	public void renderTo(GL gl, Camera camera, Projection projection) {

		if (canBeUsed() && camera != null && projection != null) {

			if (target == null) {
				if ("shader".equals(config.getString("joglImplementation"))) {
					target = new JOGLTargetShader(gl.getGL3(), new JOGLRenderingParameters(), null);
				} else {
					target = new JOGLTargetFixedFunction(gl.getGL2(), new JOGLRenderingParameters(), null);
				}
				target.setConfiguration(config);
			} else if (targetNeedsReset){
				target.reset();
			}
			targetNeedsReset = false;

			boolean viewChanged = !camera.getPos().equals(this.cameraPos)
					|| !camera.getUp().equals(this.cameraUp)
					|| !camera.getLookAt().equals(this.cameraLookAt)
					|| !projection.equals(this.projection);

			this.camera = camera;
			this.cameraPos = camera.getPos();
			this.cameraUp = camera.getUp();
			this.cameraLookAt = camera.getLookAt();
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
	protected void updateTarget(JOGLTarget target, boolean viewChanged) {}

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

		VectorXYZ lastV = vs[vs.length-1];
		VectorXYZ slastV = vs[vs.length-2];

		VectorXYZ endDir = lastV.subtract(slastV).normalize();
		VectorXYZ headStart = lastV.subtract(endDir.mult(headLength));

		VectorXZ endDirXZ = endDir.xz();
		if (endDirXZ.lengthSquared() < 0.01) { //(almost) vertical vector
			endDirXZ = VectorXZ.X_UNIT;
		} else {
			endDirXZ = endDirXZ.normalize();
		}
		VectorXYZ endNormal = endDirXZ.rightNormal().xyz(0);
		VectorXYZ endNormal2 = endDir.crossNormalized(endNormal);

		var colorMaterial = new ImmutableMaterial(Interpolation.FLAT, color);

		target.drawTriangles(colorMaterial, List.of(
				new TriangleXYZ(
						lastV,
						headStart.subtract(endNormal.mult(headLength / 2)),
						headStart.add(endNormal.mult(headLength / 2))),
				new TriangleXYZ(
						lastV,
						headStart.subtract(endNormal2.mult(headLength / 2)),
						headStart.add(endNormal2.mult(headLength / 2)))
		), emptyList());

	}

}
