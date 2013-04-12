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
		public void addSameEleConstraint(EleConnector c1, EleConnector c2) {
			if (!c1.getPosXYZ().equals(c2.getPosXYZ())) {
				target.drawLineStrip(SAME_ELE_COLOR, 2, c1.getPosXYZ(), c2.getPosXYZ());
			}
		}

		@Override
		public void addSameEleConstraint(Iterable<EleConnector> cs) {
			
		}

		@Override
		public void addMinVerticalDistanceConstraint(EleConnector upper, EleConnector lower, double distance) {
			
			target.drawLineStrip(MIN_VDIST_COLOR, 2,
					lower.getPosXYZ(),
					upper.getPosXYZ());
			
			drawArrow(target, MIN_VDIST_COLOR,
					(float) (distance / 2),
					lower.getPosXYZ(),
					lower.getPosXYZ().addY(distance));
			
		}

		@Override
		public void addMinInclineConstraint(List<EleConnector> cs, double minIncline) {
			
		}

		@Override
		public void addMaxInclineConstraint(List<EleConnector> cs, double maxIncline) {
			
		}

		@Override
		public void addSmoothnessConstraint(EleConnector c2, EleConnector c1, EleConnector c3) {
			
			VectorXYZ via = c2.getPosXYZ();
			
			VectorXYZ to1 = c1.getPosXYZ().subtract(via).normalize();
			VectorXYZ to3 = c3.getPosXYZ().subtract(via).normalize();
			
			target.drawLineStrip(SMOOTHNESS, 3,
					via.add(to1.mult(2)),
					via,
					via.add(to3.mult(2)));
			
		}

		@Override
		public void enforceConstraints() {
			
		}
		
	}
	
}
