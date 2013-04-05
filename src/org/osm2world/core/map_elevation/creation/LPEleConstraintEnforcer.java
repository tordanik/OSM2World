package org.osm2world.core.map_elevation.creation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.javailp.Linear;
import net.sf.javailp.OptType;
import net.sf.javailp.Problem;
import net.sf.javailp.Result;
import net.sf.javailp.Solver;
import net.sf.javailp.SolverFactory;
import net.sf.javailp.SolverFactoryLpSolve;

import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.JoinedEleConnectors;
import org.osm2world.core.math.VectorXYZ;

/**
 * enforces constraints using a linear programming
 */
public class LPEleConstraintEnforcer implements EleConstraintEnforcer {

	private final Problem problem;
	
	private final List<JoinedEleConnectors> joinedConnectors;
	private final Map<EleConnector, JoinedEleConnectors> connMap;

	
	public LPEleConstraintEnforcer() {
		
		problem = new Problem();
		
		joinedConnectors = new ArrayList<JoinedEleConnectors>();
		connMap = new HashMap<EleConnector, JoinedEleConnectors>();
		
	}
	
	@Override
	public void addConnectors(Iterable<JoinedEleConnectors> connectors) {
		
		for (JoinedEleConnectors jc : connectors) {
			
			joinedConnectors.add(jc);
			
			for (EleConnector c : jc.getConnectors()) {
				connMap.put(c, jc);
			}
			
		}
		
	}

	@Override
	public void addSameEleConstraint(EleConnector c1, EleConnector c2) {
		
		//TODO: merge joinedConnectors instead? Problem: different xz, and possibly terrain ele
		//TODO: but add appropriate weighting to the objective term!!
		
		addConstraint(
				 1, c1,
				-1, c2,
				"=", 0);
		
	}
	
	@Override
	public void addSameEleConstraint(Iterable<EleConnector> cs) {
		
		Iterator<EleConnector> csIterator = cs.iterator();
		
		if (csIterator.hasNext()) {
		
			EleConnector c = csIterator.next();
		
			while (csIterator.hasNext()) {
				addSameEleConstraint(c, csIterator.next());
			}
			
		}
		
	}
	
	public void addDistanceMinimumConstraint(EleConnector upper,
			EleConnector lower, double distance) {
		
		addConstraint(
				 1, upper,
				-1, lower,
				">=", distance);
		
	}
	
	
	private void addConstraint(
			double factor1, EleConnector var1,
			String op, double limit) {
		
		addConstraint(
				factor1, var1,
				0, null,
				0, null,
				op, limit);
		
	}
	
	private void addConstraint(
			double factor1, EleConnector var1,
			double factor2, EleConnector var2,
			String op, double limit) {
		
		addConstraint(
				factor1, var1,
				factor2, var2,
				0, null,
				op, limit);
		
	}
	
	private void addConstraint(
			double factor1, EleConnector var1,
			double factor2, EleConnector var2,
			double factor3, EleConnector var3,
			String op, double limit) {
		
		Linear linear = new Linear();
		
		double limitCorrection = 0;
		
		if (var1 != null) {
			JoinedEleConnectors c1 = connMap.get(var1);
			linear.add(factor1, posVar(c1));
			linear.add(-factor1, negVar(c1));
			limitCorrection += factor1 * var1.getPosXYZ().y;
		}
		
		if (var2 != null) {
			JoinedEleConnectors c2 = connMap.get(var2);
			linear.add(factor2, posVar(c2));
			linear.add(-factor2, negVar(c2));
			limitCorrection += factor2 * var2.getPosXYZ().y;
		}
		
		if (var3 != null) {
			JoinedEleConnectors c3 = connMap.get(var3);
			linear.add(factor3, posVar(c3));
			linear.add(-factor3, negVar(c3));
			limitCorrection += factor3 * var3.getPosXYZ().y;
		}
		
		problem.add(linear, op, limit - limitCorrection);
		
	}
	
	@Override
	public void enforceConstraints() {
		
		SolverFactory factory = new SolverFactoryLpSolve();
		factory.setParameter(Solver.VERBOSE, 0);
		
		problem.setObjective(constructObjective(), OptType.MIN);
		
		//TODO Relaxations relax = new Relaxations();
		
		Solver solver = factory.get();
		Result result = solver.solve(problem);
		
		if (result == null) {
			System.out.println("no result");
		}
		
		/* apply elevation values */
		
		for (JoinedEleConnectors c : joinedConnectors) {
			
			VectorXYZ posXYZ = c.getPosXYZ().addY(
					+ result.get(posVar(c)).doubleValue()
					- result.get(negVar(c)).doubleValue());
			
			c.setPosXYZ(posXYZ);
			
		}
		
	}
	
	private Linear constructObjective() {
	
		Linear objectiveLinear = new Linear();
		
		for (JoinedEleConnectors jc : joinedConnectors) {
			
			objectiveLinear.add(1, posVar(jc));
			objectiveLinear.add(1, negVar(jc));
			
		}
		
		return objectiveLinear;
		
	}
	
	/**
	 * TODO document
	 * @see #negVar(JoinedEleConnectors)
	 */
	private Object posVar(JoinedEleConnectors c) {
		return c;
	}
	
	/**
	 * TODO document
	 * @see #posVar(JoinedEleConnectors)
	 */
	private Object negVar(JoinedEleConnectors c) {
		return c.getConnectors();
	}
	
}
