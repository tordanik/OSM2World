package org.osm2world.viewer.view.debug;

import static java.lang.Math.min;
import static org.osm2world.math.Vector3D.distance;
import static org.osm2world.math.VectorXZ.listXYZ;

import java.awt.*;

import org.osm2world.map_data.data.MapArea;
import org.osm2world.map_data.data.MapElement;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.MapWaySegment;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.datastructures.MapQuadtree;
import org.osm2world.math.datastructures.MapQuadtree.QuadLeaf;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.scene.Scene;

public class QuadtreeDebugView extends DebugView {

	private static final Color LEAF_BORDER_COLOR = Color.WHITE;
	private static final Color NODE_ARROW_COLOR = Color.YELLOW;
	private static final Color LINE_ARROW_COLOR = Color.LIGHT_GRAY;
	private static final Color AREA_ARROW_COLOR = new Color(0.8f, 0.8f, 1);

	private MapQuadtree mapQuadtree;
	private boolean arrowsEnabled = true;

	public QuadtreeDebugView() {
		super("Quadtree debug view", "shows the Quadtree data structure");
	}

	@Override
	public void setConversionResults(Scene conversionResults) {
		super.setConversionResults(conversionResults);
		this.mapQuadtree = null;
	}

	public void setArrowsEnabled(boolean arrowsEnabled) {
		this.arrowsEnabled = arrowsEnabled;
	}

	@Override
	public void fillTarget(JOGLOutput target) {

		if (mapQuadtree == null) {
			mapQuadtree = new MapQuadtree(scene.getMapData().getDataBoundary());
			for (MapElement e : scene.getMapData().getMapElements()) {
				mapQuadtree.insert(e);
			}
		}

		for (QuadLeaf leaf : mapQuadtree.getLeaves()) {

			/* draw leaf boundary */

			target.drawLineStrip(LEAF_BORDER_COLOR, 1, listXYZ(leaf.bounds.vertices(), 0));

			if (arrowsEnabled) {

				/* draw arrows from leaf center to elements */

				VectorXZ leafCenter = leaf.bounds.getCentroid();

				for (MapElement e : leaf) {

					if (e instanceof MapNode node) {

						VectorXZ nodePos = node.getPos();

						drawArrow(target, NODE_ARROW_COLOR,
								(float) min(1,
										distance(leafCenter, nodePos) * 0.3),
								leafCenter.xyz(0), nodePos.xyz(0));

					} else if (e instanceof MapWaySegment segment) {

						VectorXZ lineStart = segment.getStartNode().getPos();
						VectorXZ lineEnd = segment.getEndNode().getPos();

						VectorXZ lineCenter = lineStart.add(lineEnd).mult(0.5f);

						float headLength = (float)
								min(1, distance(leafCenter, lineCenter) * 0.3);
						drawArrow(target,
								LINE_ARROW_COLOR, headLength,
								leafCenter.xyz(0), lineCenter.xyz(0));

					} else if (e instanceof MapArea) {

						VectorXZ areaCenter =
							((MapArea)e).getOuterPolygon().getCenter();

						float headLength = (float)
								min(1, distance(leafCenter, areaCenter) * 0.3);
						drawArrow(target,
								AREA_ARROW_COLOR, headLength,
								leafCenter.xyz(0), areaCenter.xyz(0));

					}
				}

			}

		}

	}

}
