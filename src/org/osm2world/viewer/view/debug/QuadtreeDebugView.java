package org.osm2world.viewer.view.debug;

import static java.lang.Math.min;
import static org.osm2world.core.math.VectorXZ.*;

import java.awt.Color;
import java.util.Arrays;

import javax.media.opengl.GL2;

import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.map_data.creation.index.MapQuadtree;
import org.osm2world.core.map_data.creation.index.MapQuadtree.QuadLeaf;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.jogl.JOGLTarget;

public class QuadtreeDebugView extends DebugView {
	
	private static final Color LEAF_BORDER_COLOR = Color.WHITE;
	private static final Color NODE_ARROW_COLOR = Color.YELLOW;
	private static final Color LINE_ARROW_COLOR = Color.LIGHT_GRAY;
	private static final Color AREA_ARROW_COLOR = new Color(0.8f, 0.8f, 1);
	
	private MapQuadtree mapQuadtree;
	private boolean arrowsEnabled = true;
	
	@Override
	public void setConversionResults(Results conversionResults) {
		super.setConversionResults(conversionResults);
		this.mapQuadtree = null;
	}
	
	public void setArrowsEnabled(boolean arrowsEnabled) {
		this.arrowsEnabled = arrowsEnabled;
	}
	
	@Override
	public boolean canBeUsed() {
		return map != null;
	}
	
	@Override
	public void renderToImpl(GL2 gl, Camera camera) {
		
		if (mapQuadtree == null) {
			mapQuadtree = new MapQuadtree(map);
		}
		
		JOGLTarget target = new JOGLTarget(gl, camera);
		
		for (QuadLeaf leaf : mapQuadtree.getLeaves()) {
			
			/* draw leaf boundary */
			
			VectorXZ[] vs = new VectorXZ[5];
			vs[0] = new VectorXZ(leaf.minX, leaf.minZ);
			vs[1] = new VectorXZ(leaf.maxX, leaf.minZ);
			vs[2] = new VectorXZ(leaf.maxX, leaf.maxZ);
			vs[3] = new VectorXZ(leaf.minX, leaf.maxZ);
			vs[4] = vs[0];
			target.drawLineStrip(LEAF_BORDER_COLOR,
					listXYZ(Arrays.asList(vs),0));
			
			if (arrowsEnabled) {
				
				/* draw arrows from leaf center to elements */
				
				VectorXZ leafCenter = new VectorXZ(
						(leaf.minX + leaf.maxX)/2,
						(leaf.minZ + leaf.maxZ)/2);
				
				for (MapElement e : leaf) {
					
					if (e instanceof MapNode) {
						
						VectorXZ nodePos = ((MapNode)e).getPos();
						
						target.drawArrow(NODE_ARROW_COLOR,
								(float) min(1,
										distance(leafCenter, nodePos) * 0.3),
								leafCenter.xyz(0), nodePos.xyz(0));
						
					} else if (e instanceof MapWaySegment) {
						
						VectorXZ lineStart =
							((MapWaySegment)e).getStartNode().getPos();
						VectorXZ lineEnd =
							((MapWaySegment)e).getEndNode().getPos();
						
						VectorXZ lineCenter = lineStart.add(lineEnd).mult(0.5f);
						
						float headLength = (float)
								min(1, distance(leafCenter, lineCenter) * 0.3);
						target.drawArrow(
								LINE_ARROW_COLOR, headLength,
								leafCenter.xyz(0), lineCenter.xyz(0));
						
					} else if (e instanceof MapArea) {
						
						VectorXZ areaCenter =
							((MapArea)e).getOuterPolygon().getCenter();
						
						float headLength = (float)
								min(1, distance(leafCenter, areaCenter) * 0.3);
						target.drawArrow(
								AREA_ARROW_COLOR, headLength,
								leafCenter.xyz(0), areaCenter.xyz(0));
					
					}
				}
				
			}
			
		}
		
	}
	
}
