package org.osm2world.viewer.view.debug;

import java.awt.Color;

import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.map_data.creation.index.Map2dTree;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.jogl.JOGLTarget;

public class Map2dTreeDebugView extends DebugView {
	
	private RenderableMap2dTree map2dTree;
	
	@Override
	public void setConversionResults(Results conversionResults) {
		super.setConversionResults(conversionResults);
		this.map2dTree = null;
	}
	
	@Override
	public boolean canBeUsed() {
		return map != null;
	}
	
	@Override
	public void fillTarget(JOGLTarget target) {
		
		if (map2dTree == null) {
			map2dTree = new RenderableMap2dTree();
			for (MapElement e : map.getMapElements()) {
				map2dTree.insert(e);
			}
		}
		
		map2dTree.renderTo(target);
		
	}
	
	private class RenderableMap2dTree extends Map2dTree {

		public RenderableMap2dTree() {
			super(map.getDataBoundary());
		}

		public void renderTo(JOGLTarget target) {
			
			renderNodeTo(this.root, target,
					map.getDataBoundary().minX - 10,
					map.getDataBoundary().maxX + 10,
					map.getDataBoundary().minZ - 10,
					map.getDataBoundary().maxZ + 10,
					1);
			
		}

		private void renderNodeTo(Node node, JOGLTarget target,
				double minX, double maxX,
				double minZ, double maxZ,
				int depth) {
			
			if (node instanceof InnerNode) {
				
				InnerNode innerNode = (InnerNode)node;
				
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
