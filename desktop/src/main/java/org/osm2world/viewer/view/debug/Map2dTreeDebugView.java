package org.osm2world.viewer.view.debug;

import org.osm2world.map_data.data.MapElement;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.datastructures.Map2dTree;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.scene.Scene;
import org.osm2world.scene.color.Color;

public class Map2dTreeDebugView extends StaticDebugView {

	private RenderableMap2dTree map2dTree;

	public Map2dTreeDebugView() {
		super("Map2dTree debug view", "shows the Map2dTree data structure");
	}

	@Override
	public void setConversionResults(Scene conversionResults) {
		super.setConversionResults(conversionResults);
		this.map2dTree = null;
	}

	@Override
	public void fillOutput(JOGLOutput output) {

		if (map2dTree == null) {
			map2dTree = new RenderableMap2dTree();
			for (MapElement e : scene.getMapData().getMapElements()) {
				map2dTree.insert(e);
			}
		}

		map2dTree.renderTo(output);

	}

	private class RenderableMap2dTree extends Map2dTree {

		public RenderableMap2dTree() {
			super(scene.getMapData().getDataBoundary());
		}

		public void renderTo(JOGLOutput target) {

			renderNodeTo(this.root, target,
					scene.getMapData().getDataBoundary().minX - 10,
					scene.getMapData().getDataBoundary().maxX + 10,
					scene.getMapData().getDataBoundary().minZ - 10,
					scene.getMapData().getDataBoundary().maxZ + 10,
					1);

		}

		private void renderNodeTo(Node node, JOGLOutput target,
				double minX, double maxX,
				double minZ, double maxZ,
				int depth) {

			if (node instanceof InnerNode innerNode) {

				Color lineColor = new Color(1.0f,
						Math.min(1.0f, 2.0f / depth), 0.0f);

				if (innerNode.splitAlongX) {

					target.drawLineStrip(lineColor, 1,
							new VectorXYZ(innerNode.splitValue, 0, minZ),
							new VectorXYZ(innerNode.splitValue, 0, maxZ));

					renderNodeTo(innerNode.lowerChild, target,
							minX, innerNode.splitValue,
							minZ, maxZ,
							depth + 1);

					renderNodeTo(innerNode.upperChild, target,
							innerNode.splitValue, maxX,
							minZ, maxZ,
							depth + 1);

				} else {

					target.drawLineStrip(lineColor, 1,
							new VectorXYZ(minX, 0, innerNode.splitValue),
							new VectorXYZ(maxX, 0, innerNode.splitValue));

					renderNodeTo(innerNode.lowerChild, target,
							minX, maxX,
							minZ, innerNode.splitValue,
							depth + 1);

					renderNodeTo(innerNode.upperChild, target,
							minX, maxX,
							innerNode.splitValue, maxZ,
							depth + 1);

				}

			}

		}

	}

}
