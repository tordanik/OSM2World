package org.osm2world.viewer.view.debug;

import static java.lang.Math.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL2;

import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.terrain.data.TerrainPatch;

/**
 * view that shows points with a color depending on their elevation
 */
public class EleDebugView extends DebugView {

	@Override
	public String getDescription() {
		return "shows points with a color depending on their elevation";
	}
	
	@Override
	protected void renderToImpl(GL2 gl, Camera camera) {
		
		/* collect all points */
		
		List<VectorXYZ> points = new ArrayList<VectorXYZ>();
		
		for (TerrainPatch patch : terrain.getPatches()) {
			for (TriangleXYZ t : patch.getTriangulation()) {
				points.addAll(t.getVertices());
			}
		}
		
		for (MapElement element : map.getMapElements()) {
			if (element.getElevationProfile() != null) {
				points.addAll(element.getElevationProfile().getPointsWithEle());
			}
		}
		
		/* determine minimum and maximum elevations */
		
		double minEle = Double.POSITIVE_INFINITY;
		double maxEle = Double.NEGATIVE_INFINITY;
		
		for (VectorXYZ point : points) {
			minEle = min(minEle, point.y);
			maxEle = max(maxEle, point.y);
		}
		
		/* choose a range for distributing the colors */
		
		double eleRange = max (maxEle - minEle, 10);
		
		/* draw the points */
		
		for (VectorXYZ point : points) {
			
			double colorGradientValue = (point.y - minEle) / eleRange;
			
			Color color = interpolateGradientColor((float)colorGradientValue,
					Color.GREEN, Color.YELLOW, Color.RED);
			
			drawBoxAround(new JOGLTarget(gl, camera), point, color, 0.4f);
			
		}
		
	}

	/**
	 * chooses a color value from a sequence of linear color gradients
	 */
	private Color interpolateGradientColor(float value, Color... colors) {

		assert colors.length > 1;
		
		int sections = colors.length - 1;
		float sectionLength = 1.0f / sections;
		
		int sectionOfValue = min(max(
				(int)( value / sectionLength ), 0), sections-1);
				
		return interpolateGradientColor(
				 (value - sectionLength * sectionOfValue) / sectionLength,
				 colors[sectionOfValue], colors[sectionOfValue+1]);
				
	}
	
	/**
	 * chooses a color value from linear color gradient
	 */
	private Color interpolateGradientColor(float value, Color c0, Color c1) {
		
		return new Color(
				round(c0.getRed() * (1 - value) + c1.getRed() * value),
				round(c0.getGreen() * (1 - value) + c1.getGreen() * value),
				round(c0.getBlue() * (1 - value) + c1.getBlue() * value));
		
	}

	
}
