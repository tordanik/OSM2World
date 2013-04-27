package org.osm2world.viewer.view.debug;

import static java.awt.Color.*;

import java.awt.Color;
import java.util.List;

import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.world.data.WorldObject;

/**
 * shows elevation constraints
 */
public class EleConstraintDebugView extends DebugView {
	
	private static final Color SAME_ELE_COLOR = WHITE;
	private static final Color MIN_VDIST_COLOR = PINK;
	private static final Color SMOOTHNESS = GREEN;
	
	@Override
	public String getDescription() {
		return "shows elevation constraints";
	}
	
	@Override
	public boolean canBeUsed() {
		return map != null;
	}
	
	@Override
	protected void fillTarget(JOGLTarget target) {
		
		ConstraintSink sink = new ConstraintSink(target);
		
		for (WorldObject worldObject : map.getWorldObjects()) {
			worldObject.defineEleConstraints(sink);
		}
		
	}
	
	private static class ConstraintSink implements EleConstraintEnforcer {

		private final JOGLTarget target;
				
		private ConstraintSink(JOGLTarget target) {
			this.target = target;
		}

		@Override
		public void addConnectors(Iterable<EleConnector> connectors) {
			
		}

		@Override
		public void requireSameEle(EleConnector c1, EleConnector c2) {
			if (!c1.getPosXYZ().equals(c2.getPosXYZ())) {
				target.drawLineStrip(SAME_ELE_COLOR, 2, c1.getPosXYZ(), c2.getPosXYZ());
			}
		}

		@Override
		public void requireSameEle(Iterable<EleConnector> cs) {
			
		}

		@Override
		public void requireVerticalDistance(ConstraintType type, double distance,
				EleConnector upper, EleConnector lower) {
			
			target.drawLineStrip(MIN_VDIST_COLOR, 2,
					lower.getPosXYZ(),
					upper.getPosXYZ());
			
			drawArrow(target, MIN_VDIST_COLOR,
					(float) (distance / 2),
					lower.getPosXYZ(),
					lower.getPosXYZ().addY(distance));
			
		}

		@Override
		public void requireIncline(ConstraintType type, double incline,
				List<EleConnector> cs) {
			
		}

		@Override
		public void requireSmoothness(
				EleConnector from, EleConnector via, EleConnector to) {
			
			VectorXYZ v = via.getPosXYZ();
			
			VectorXYZ vToFrom = from.getPosXYZ().subtract(v).normalize();
			VectorXYZ vToTo = to.getPosXYZ().subtract(v).normalize();
			
			target.drawLineStrip(SMOOTHNESS, 3,
					v.add(vToFrom.mult(2)),
					v,
					v.add(vToTo.mult(2)));
			
		}

		@Override
		public void enforceConstraints() {
			
		}
		
	}
	
}
