package org.osm2world.viewer.view.debug;

import static java.lang.Math.min;
import static org.osm2world.math.Vector3D.distance;
import static org.osm2world.math.VectorXZ.listXYZ;

import org.osm2world.map_data.data.MapArea;
import org.osm2world.map_data.data.MapElement;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.MapWaySegment;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.datastructures.MapQuadtree;
import org.osm2world.math.datastructures.MapQuadtree.QuadLeaf;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.scene.Scene;
import org.osm2world.scene.color.Color;

public class QuadtreeDebugView extends StaticDebugView {

	private static final boolean ARROWS_ENABLED = true;
	private static final Color LEAF_BORDER_COLOR = Color.WHITE;
	private static final Color NODE_ARROW_COLOR = Color.YELLOW;
	private static final Color LINE_ARROW_COLOR = Color.LIGHT_GRAY;
	private static final Color AREA_ARROW_COLOR = new Color(0.8f, 0.8f, 1);

	private MapQuadtree mapQuadtree;

	public QuadtreeDebugView() {
		super("Quadtree debug view", "shows the Quadtree data structure");
	}

	@Override
	public void setConversionResults(Scene conversionResults) {
		super.setConversionResults(conversionResults);
		this.mapQuadtree = null;
	}

	@Override
	public void fillOutput(JOGLOutput output) {

		if (mapQuadtree == null) {
			mapQuadtree = new MapQuadtree(scene.getMapData().getDataBoundary());
			for (MapElement e : scene.getMapData().getMapElements()) {
				mapQuadtree.insert(e);
			}
		}

		for (QuadLeaf leaf : mapQuadtree.getLeaves()) {

			/* draw leaf boundary */

			output.drawLineStrip(LEAF_BORDER_COLOR, 1, listXYZ(leaf.bounds.vertices(), 0));

			if (ARROWS_ENABLED) {

				/* draw arrows from leaf center to elements */

				VectorXZ leafCenter = leaf.bounds.getCentroid();

				for (MapElement e : leaf) {

					if (e instanceof MapNode node) {

						VectorXZ nodePos = node.getPos();

						drawArrow(output, NODE_ARROW_COLOR,
								(float) min(1,
										distance(leafCenter, nodePos) * 0.3),
								leafCenter.xyz(0), nodePos.xyz(0));

					} else if (e instanceof MapWaySegment segment) {

						VectorXZ lineStart = segment.getStartNode().getPos();
						VectorXZ lineEnd = segment.getEndNode().getPos();

						VectorXZ lineCenter = lineStart.add(lineEnd).mult(0.5f);

						float headLength = (float)
								min(1, distance(leafCenter, lineCenter) * 0.3);
						drawArrow(output,
								LINE_ARROW_COLOR, headLength,
								leafCenter.xyz(0), lineCenter.xyz(0));

					} else if (e instanceof MapArea) {

						VectorXZ areaCenter =
							((MapArea)e).getOuterPolygon().getCenter();

						float headLength = (float)
								min(1, distance(leafCenter, areaCenter) * 0.3);
						drawArrow(output,
								AREA_ARROW_COLOR, headLength,
								leafCenter.xyz(0), areaCenter.xyz(0));

					}
				}

			}

		}

	}

}
