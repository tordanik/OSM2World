package org.osm2world.viewer.view.debug;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Objects;

import org.osm2world.conversion.ConversionLog;
import org.osm2world.conversion.O2WConfig;
import org.osm2world.math.Vector3D;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.output.common.rendering.Camera;
import org.osm2world.output.common.rendering.ImmutableCamera;
import org.osm2world.output.common.rendering.Projection;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.output.jogl.JOGLOutputFixedFunction;
import org.osm2world.output.jogl.JOGLOutputShader;
import org.osm2world.output.jogl.JOGLRenderingParameters;
import org.osm2world.scene.Scene;
import org.osm2world.scene.color.Color;
import org.osm2world.scene.material.Material;
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

	// camera attributes that are used to detect changes
	private ImmutableCamera previousCamera;
	private Projection previousProjection;

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
	 * Returns true if this DebugView can currently be used for rendering.
	 * By default, this checks whether the #scene is available (not null),
	 * but subclasses can overwrite it with their own checks.
	 */
	public boolean canBeUsed() {
		return scene != null;
	}

	/**
	 * Renders the content added by {@link #updateOutput(JOGLOutput, boolean, Camera, Projection)}.
	 * Only has an effect if {@link #canBeUsed()} is true.
	 *
	 * @param gl  needs to be the same gl as in previous calls
	 */
	public void renderTo(GL gl, Camera camera, Projection projection) {

		if (canBeUsed() && camera != null && projection != null) {

			if (target == null) {
				if ("shader".equals(config.joglImplementation())) {
					target = new JOGLOutputShader(gl.getGL3(), new JOGLRenderingParameters(), null);
				} else {
					target = new JOGLOutputFixedFunction(gl.getGL2(), new JOGLRenderingParameters(), null);
				}
				target.setConfiguration(config);
			} else if (targetNeedsReset){
				target.reset();
			}
			targetNeedsReset = false;

			boolean viewChanged = !Objects.equals(camera.copy(), previousCamera)
					|| !Objects.equals(projection, previousProjection);

			this.previousCamera = camera.copy();
			this.previousProjection = projection;

			updateOutput(target, !target.isFinished() || viewChanged, camera, projection);

			target.finish();

			target.render(camera, projection);

			// flush log messages
			ConversionLog.getLog();

		}
	}

	/**
	 * Lets the subclass add all content and settings for rendering,
	 * either initially or after a relevant change.
	 * Will only be called if {@link #canBeUsed()} is true.
	 *
	 * @param viewChanged  true for the first call, and if camera or projection have changed
	 */
	protected abstract void updateOutput(JOGLOutput output, boolean viewChanged, Camera camera, Projection projection);

	protected static void drawBoxAround(JOGLOutput output,
			Vector3D center, Color color, float halfWidth) {
		VectorXYZ centerXYZ = center.xyz();
		output.drawLineLoop(color, 1, List.of(
				centerXYZ.add(- halfWidth, 0, - halfWidth),
				centerXYZ.add(- halfWidth, 0, halfWidth),
				centerXYZ.add(halfWidth, 0, halfWidth),
				centerXYZ.add(halfWidth, 0, - halfWidth)));
	}

	protected static void drawArrow(JOGLOutput output, Color color,
			float headLength, VectorXYZ... vs) {

		output.drawLineStrip(color, 1, vs);

		/* draw head */

		VectorXYZ lastV = vs[vs.length-1];
		VectorXYZ sLastV = vs[vs.length-2];

		VectorXYZ endDir = lastV.subtract(sLastV).normalize();
		VectorXYZ headStart = lastV.subtract(endDir.mult(headLength));

		VectorXZ endDirXZ = endDir.xz();
		if (endDirXZ.lengthSquared() < 0.01) { //(almost) vertical vector
			endDirXZ = VectorXZ.X_UNIT;
		} else {
			endDirXZ = endDirXZ.normalize();
		}
		VectorXYZ endNormal = endDirXZ.rightNormal().xyz(0);
		VectorXYZ endNormal2 = endDir.crossNormalized(endNormal);

		var colorMaterial = new Material(Interpolation.FLAT, color);

		output.drawTriangles(colorMaterial, List.of(
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
