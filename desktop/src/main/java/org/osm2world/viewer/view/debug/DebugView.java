package org.osm2world.viewer.view.debug;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.awt.*;
import java.util.List;

import org.osm2world.conversion.ConversionLog;
import org.osm2world.conversion.O2WConfig;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.output.common.rendering.Camera;
import org.osm2world.output.common.rendering.Projection;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.output.jogl.JOGLOutputFixedFunction;
import org.osm2world.output.jogl.JOGLOutputShader;
import org.osm2world.output.jogl.JOGLRenderingParameters;
import org.osm2world.scene.Scene;
import org.osm2world.scene.material.ImmutableMaterial;
import org.osm2world.scene.material.Material.Interpolation;

import com.jogamp.opengl.GL;

/**
 * contains some common methods for debug views
 */
public abstract class DebugView {

	public final String label;
	public final String description;

	protected O2WConfig config;

	protected Scene scene;

	protected Camera camera;
	protected Projection projection;

	// camera attributes that are used to detect changes
	private VectorXYZ cameraPos;
	private VectorXYZ cameraUp;
	private VectorXYZ cameraLookAt;

	private JOGLOutput target = null;
	private boolean targetNeedsReset;

	protected DebugView(String label, String description) {
		this.label = label;
		this.description = description;
	}

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

	public void setConversionResults(Scene conversionResults) {

		this.scene = conversionResults;

		targetNeedsReset = true;
	}

	/**
	 * returns true if this DebugView can currently be used for rendering.
	 * By default, this checks whether the #scene is available (not null),
	 * but subclasses can overwrite it with their own checks.
	 */
	public boolean canBeUsed() {
		return scene != null;
	}

	/**
	 * renders the content added by {@link #fillTarget(JOGLOutput)}.
	 * Only has an effect if {@link #canBeUsed()} is true.
	 *
	 * @param gl  needs to be the same gl as in previous calls
	 */
	public void renderTo(GL gl, Camera camera, Projection projection) {

		if (canBeUsed() && camera != null && projection != null) {

			if (target == null) {
				if ("shader".equals(config.getString("joglImplementation"))) {
					target = new JOGLOutputShader(gl.getGL3(), new JOGLRenderingParameters(), null);
				} else {
					target = new JOGLOutputFixedFunction(gl.getGL2(), new JOGLRenderingParameters(), null);
				}
				target.setConfiguration(config);
			} else if (targetNeedsReset){
				target.reset();
			}
			targetNeedsReset = false;

			boolean viewChanged = !camera.pos().equals(this.cameraPos)
					|| !camera.up().equals(this.cameraUp)
					|| !camera.lookAt().equals(this.cameraLookAt)
					|| !projection.equals(this.projection);

			this.camera = camera;
			this.cameraPos = camera.pos();
			this.cameraUp = camera.up();
			this.cameraLookAt = camera.lookAt();
			this.projection = projection;

			if (!target.isFinished()) {

				fillTarget(target);

				target.finish();

			} else {

				updateTarget(target, viewChanged);

				target.finish();

			}

			target.render(camera, projection);

			// flush log messages
			ConversionLog.getLog();

		}
	}

	/**
	 * lets the subclass add all content and settings for rendering.
	 * Will only be called if {@link #canBeUsed()} is true.
	 */
	protected abstract void fillTarget(JOGLOutput target);

	/**
	 * lets the subclass update the target after the initial
	 * {@link #fillTarget(JOGLOutput)}.
	 *
	 * @param viewChanged  true if camera or projection have changed
	 */
	protected void updateTarget(JOGLOutput target, boolean viewChanged) {}

	protected static final void drawBoxAround(JOGLOutput target,
			VectorXZ center, Color color, float halfWidth) {
		drawBoxAround(target, center.xyz(0), color, halfWidth);
	}

	protected static final void drawBoxAround(JOGLOutput target,
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

	protected static final void drawBox(JOGLOutput target, Color color,
			VectorXYZ v1, VectorXYZ v2, VectorXYZ v3, VectorXYZ v4) {
		target.drawLineLoop(color, 1, asList(v1, v2, v3, v4));
	}

	protected static final void drawArrow(JOGLOutput target, Color color,
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
