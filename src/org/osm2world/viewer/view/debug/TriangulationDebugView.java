package org.osm2world.viewer.view.debug;

import java.awt.Color;

import javax.media.opengl.GL;

import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.terrain.data.TerrainPatch;

/**
 * shows the effect of integrating the holes and outer polygons
 * of {@link TerrainPatch}es
 * //TODO: does this *do* anything useful?
 */
public class TriangulationDebugView extends DebugView {

	private static final Color LINE_COLOR = Color.PINK;
	private static final Color ROT_COLOR = Color.WHITE;
	
	@Override
	public void renderToImpl(GL gl, Camera camera, Projection projection) {
	
		JOGLTarget target = new JOGLTarget(gl, camera);
//
//		for (TerrainPatch patch : terrain.getPatches()) {
//
//			if (patch.getHoles().isEmpty()) continue;
//
//			List<VectorXZ> polygonOutline = new LinkedList<VectorXZ>(
//					patch.getOuterPolygon().getVertexLoop());
//
//			TriangulationUtil.insertHolesInPolygonOutline(
//					polygonOutline, patch.getHoles());
//
//			target.drawLineLoop(LINE_COLOR, polygonOutline);
//
//			/* draw direction indicators near nodes */
//			for (int i=0; i+1 < polygonOutline.size(); i++) {
//
//				VectorXZ nodePos = polygonOutline.get(i);
//				VectorXZ afterPos = polygonOutline.get(i+1);
//
//				VectorXZ offsetTargetNode = VectorXZ.NULL_VECTOR;
//
//				if (i != 0) {
//					VectorXZ beforePos = polygonOutline.get(i-1);
//					offsetTargetNode = beforePos.subtract(nodePos).normalize().mult(2);
//				}
//
//				VectorXZ arrowStart = nodePos.add(offsetTargetNode);
//				VectorXZ arrowEnd = nodePos.add(
//					afterPos.subtract(nodePos).normalize().mult(3))
//					.add(offsetTargetNode);
//
//				target.drawArrow(ROT_COLOR, 1, arrowStart, arrowEnd);
//
//				target.drawText(Integer.toString(i), arrowStart);
//
//			}
//
//			/* draw numbers on triangle centers */
//
//			int i=0;
//			for (TriangleXYZ triangle : patch.triangulation) {
//				target.drawText(Integer.toString(i),
//						new SimplePolygonXZ(Arrays.asList(triangle.v1.xz(), triangle.v2.xz(), triangle.v3.xz(), triangle.v1.xz())).getCenter());
//				i++;
//			}
//
//		}
		
	}
	
}
