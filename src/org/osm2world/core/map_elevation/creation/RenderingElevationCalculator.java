package org.osm2world.core.map_elevation.creation;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.osm2world.core.heightmap.data.CellularTerrainElevation;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.jogl.JOGLTarget;

/**
 * ElevationCalculator that allows to set the number of calculation steps
 * and displays the forces that would be in effect during the next step.
 */
public class RenderingElevationCalculator extends ElevationCalculator {

	private final int steps;
	private final Collection<RenderingElevationDeterminationScenario> scenarios
		= new ArrayList<RenderingElevationDeterminationScenario>();
		
	public RenderingElevationCalculator(int steps) {
		super();
		this.steps = steps;
	}
	
	public int getSteps() {
		return steps;
	}
	
	public void renderTo(JOGLTarget util) {
		for (RenderingElevationDeterminationScenario scenario : scenarios) {
			scenario.renderTo(util);
		}
	}
	
	@Override
	protected ElevationDeterminationScenario createScenario(CellularTerrainElevation eleData) {
		
		RenderingElevationDeterminationScenario scenario =
			new RenderingElevationDeterminationScenario(eleData, steps);
		
		scenarios.add(scenario);
		
		return scenario;
	}
	
	private static class RenderingElevationDeterminationScenario
		extends ElevationDeterminationScenario {

		private final int steps;
		
		private final Collection<ForceArrow> forceArrows = new ArrayList<ForceArrow>();
		
		public RenderingElevationDeterminationScenario(CellularTerrainElevation eleData, int steps) {
			super(eleData);
			this.steps = steps;
		}
		
		@Override
		public void calculate() {
			
			super.calculate();
			
			/* calculate force effects for next step (without writing results)
			 * and visualize differences */
			
			for (Force force : forces) {
				force.prepare(forceScaleForStep(steps));
			}

			forceArrows.addAll(arrowsForForces(Color.RED,
					forcesOfType(VerticalDistanceForce.class, forces)));
			
			forceArrows.addAll(arrowsForForces(Color.WHITE,
					forcesOfType(NodeElevationForce.class, forces)));
			
			forceArrows.addAll(arrowsForForces(Color.BLUE,
					forcesOfType(InclineConnectionForce.class, forces)));
		
			forceArrows.addAll(arrowsForForces(Color.GREEN,
					forcesOfType(DirectionConnectionForce.class, forces)));

			forceArrows.addAll(arrowsForForces(Color.YELLOW,
					forcesOfType(UnknownInclineConnectionForce.class, forces)));

			//TODO: remove (represented by connecting lines)
			forceArrows.addAll(arrowsForForces(Color.RED,
					forcesOfType(NeighborshipForce.class, forces)));
			
//			TODO
//			forceArrows.addAll(arrowsForForces(Color.PINK,
//					forcesOfType(AngleForce.class, forces)));

			
		}
		
		private static Collection<Force> forcesOfType(
				Class<? extends Force> forceType, Collection<Force> allForces) {
			
			Collection<Force> result = new ArrayList<Force>();
		
			for (Force force : allForces) {
				if (forceType.isInstance(force)) {
					result.add(force);
				}
			}
			
			return result;
			
		}
		
		public void renderTo(JOGLTarget util) {

			for (Force force : forces) {
				if (force instanceof NeighborshipForce) {
					NeighborshipForce nForce = (NeighborshipForce) force;
					renderNeighborshipTo(util, nForce.node1, nForce.node2);
					//TODO: brightness dependent on current force effect?
				}
			}
			
			for (ForceNode forceNode : forceNodes) {
				renderForceNodeTo(util, forceNode);
			}
						
			for (ForceArrow forceArrow : forceArrows) {
				forceArrow.renderTo(util);
			}
			
		}

		private void renderNeighborshipTo(JOGLTarget util,
				ForceNode node1, ForceNode node2) {

			util.drawLineStrip(Color.LIGHT_GRAY,
					node1.getPos().xyz(node1.getCurrentEle()),
					node2.getPos().xyz(node2.getCurrentEle()));
			
		}

		private static void renderForceNodeTo(JOGLTarget util,
				ForceNode forceNode) {
		
			Color color = Color.WHITE;
			float halfWidth = 0.25f;
			
			if (forceNode instanceof ForceNodeOnTerrainPoint) {
				color = Color.LIGHT_GRAY;
			} else if (forceNode instanceof ForceNodeOnNode) {
				color = Color.YELLOW;
				halfWidth = 0.5f;
			}
			
			// TODO: replace
//			DebugView.drawBoxAround(util,
//					forceNode.getPos().xyz(forceNode.getCurrentEle()),
//					color, halfWidth);
			
		}

		/**
		 * creates force arrows for a collection of forces
		 * by applying them and checking the results.
		 * The forces need to be prepared ({@link Force#prepare(float)}).
		 */
		private Collection<ForceArrow> arrowsForForces(
				Color color, Collection<Force> forces) {
			
			Collection<ForceArrow> results = new ArrayList<ForceArrow>();
			
			/* save start eles so the effect of force applications
			 * can be observed */
			
			Map<ForceNode, Float> startEles =
				new HashMap<ForceNode, Float>();
			
			for (ForceNode fNode : forceNodes) {
				startEles.put(fNode, fNode.getCurrentEle());
			}
			
			/* apply forces and create arrows for them */
			
			for (Force force : forces) {
				force.apply();
			}
			
			for (ForceNode fNode : forceNodes) {
				results.add(new ForceArrow(fNode.getPos(),
						startEles.get(fNode), fNode.getCurrentEle(), color));
			}
			
			/* restore start eles - the arrows for other
			 * force types should start at the original position */
			
			for (ForceNode fNode : forceNodes) {
				fNode.changeCurrentEle(
						- fNode.getCurrentEle() + startEles.get(fNode) );
			}
			
			/* return the arrows */
			
			return results;
			
		}
		
		@Override
		protected int getCalculationSteps() {
			return steps;
		}
		
		private static final class ForceArrow {
			
			private final VectorXZ pos;
			private final float fromEle;
			private final float toEle;
			private final Color color;
			
			public ForceArrow(VectorXZ pos, float fromEle, float toEle, Color color) {
				this.pos = pos;
				this.fromEle = fromEle;
				this.toEle = toEle;
				this.color = color;
			}
			
			public void renderTo(JOGLTarget util) {
				util.drawArrow(color, Math.min(1, 0.3f*Math.abs(fromEle-toEle)),
						pos.xyz(fromEle),
						pos.xyz(toEle));
			}
			
		}
		
	}
	
}
