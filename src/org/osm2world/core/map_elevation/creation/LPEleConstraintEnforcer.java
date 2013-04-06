package org.osm2world.core.map_elevation.creation;

import java.util.ArrayList;
import java.util.Collection;
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
import org.osm2world.core.map_elevation.data.LPVariablePair;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * enforces constraints using a linear programming
 */
public class LPEleConstraintEnforcer implements EleConstraintEnforcer {

	private final Problem problem;
	
	private final List<LPVariablePair> variables;
	private final Map<EleConnector, LPVariablePair> variableMap;

	
	public LPEleConstraintEnforcer() {
		
		problem = new Problem();
		
		variables = new ArrayList<LPVariablePair>();
		variableMap = new HashMap<EleConnector, LPVariablePair>();
		
	}
	
	@Override
	public void addConnectors(Iterable<EleConnector> connectors) {
		
		for (LPVariablePair jc : joinConnectors(connectors)) {
			
			variables.add(jc);
			
			for (EleConnector c : jc.getConnectors()) {
				variableMap.put(c, jc);
			}
			
		}
		
	}
	
	/**
	 * join connected connectors by constraining them to the same elevation
	 */
	private Collection<LPVariablePair> joinConnectors(
			Iterable<EleConnector> connectors) {
		
		Multimap<VectorXZ, LPVariablePair> connectorJoinMap =
				HashMultimap.create();
		
		connectors:
		for (EleConnector c : connectors) {
			
			Collection<LPVariablePair> existingJoinedConns =
					connectorJoinMap.get(c.pos);
			
			if (existingJoinedConns != null){
				for (LPVariablePair cs : existingJoinedConns) {
					if (cs.connectsTo(c)) {
						cs.add(c);
						continue connectors;
					}
				}
			}
			
			// add new entry if no existing set of joined connectors matched c
			connectorJoinMap.put(c.pos, new LPVariablePair(c));
			
		}
		
		return connectorJoinMap.values();
		
	}
	
	@Override
	public void addSameEleConstraint(EleConnector c1, EleConnector c2) {
		
		//FIXME: this doesn't work because of the different interpolated elevations
		// - the pos/neg variables are different!
		
		/* instead of adding an actual constraint, the variables are merged.
		 * This reduces the total number of variables and constraints. */
		/*
		LPVariablePair v1 = variableMap.get(c1);
		LPVariablePair v2 = variableMap.get(c2);
		
		if (v1 == v2) { return; }
		
		// move all connectors from v2 onto v1
		
		for (EleConnector connector : v2.getConnectors()) {
			variableMap.put(connector, v1);
		}
		
		v1.addAll(v2);
		variables.remove(v1);
		
		// replace previous occurrences of v2
		
		for (Constraint constraint : problem.getConstraints()) {
			
			List<Object> variables = constraint.getLhs().getVariables();
			
			for (int i = 0; i < variables.size(); i++) {
				if (variables.get(i) == v2.posVar()) {
					variables.set(i, v1.posVar());
				}
				if (variables.get(i) == v2.negVar()) {
					variables.set(i, v1.negVar());
				}
			}
			
		}
		*/
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
	
	@Override
	public void addMinVerticalDistanceConstraint(
			EleConnector upper, EleConnector lower, double distance) {
		
		addConstraint(
				 1, upper,
				-1, lower,
				">=", distance);
		
	}
	
	@Override
	public void addMinInclineConstraint(List<EleConnector> cs, double minIncline) {
		
		EleConnector first = cs.get(0);
		EleConnector last = cs.get(cs.size() - 1);
		
		addConstraint(
				 1, last,
				-1, first,
				">=", minIncline * first.pos.distanceTo(last.pos));
		
	}
	
	@Override
	public void addMaxInclineConstraint(List<EleConnector> cs, double maxIncline) {
		
		EleConnector first = cs.get(0);
		EleConnector last = cs.get(cs.size() - 1);
		
		addConstraint(
				 1, last,
				-1, first,
				"<=", maxIncline * first.pos.distanceTo(last.pos));
		
	}
	
	@Override
	public void addSmoothnessConstraint(EleConnector c2,
			EleConnector c1, EleConnector c3) {
		
		double smoothFactor = 0.1;
		
		double dist12 = c1.pos.distanceTo(c2.pos);
		double dist23 = c2.pos.distanceTo(c3.pos);
		
		//| - dist23 * c1 + (dist12 + dist23) * c2 + dist12 * c3 |
		//   <= x% * dist12 * dist23
		
		//FIXME rethink this smoothie stuff
		
		addConstraint(
				-dist23, c1,
				dist12 + dist23, c2,
				-dist12, c3,
				"<=", smoothFactor * dist12 * dist23);
		
		addConstraint(
				dist23, c1,
				-(dist12 + dist23), c2,
				dist12, c3,
				">=", -smoothFactor * dist12 * dist23);
		
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
			LPVariablePair c1 = variableMap.get(var1);
			linear.add(factor1, c1.posVar());
			linear.add(-factor1, c1.negVar());
			limitCorrection += factor1 * var1.getPosXYZ().y;
		}
		
		if (var2 != null) {
			LPVariablePair c2 = variableMap.get(var2);
			linear.add(factor2, c2.posVar());
			linear.add(-factor2, c2.negVar());
			limitCorrection += factor2 * var2.getPosXYZ().y;
		}
		
		if (var3 != null) {
			LPVariablePair c3 = variableMap.get(var3);
			linear.add(factor3, c3.posVar());
			linear.add(-factor3, c3.negVar());
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
		
		for (LPVariablePair c : variables) {
			
			VectorXYZ posXYZ = c.getPosXYZ().addY(
					+ result.get(c.posVar()).doubleValue()
					- result.get(c.negVar()).doubleValue());
			
			c.setPosXYZ(posXYZ);
			
		}
		
	}
	
	private Linear constructObjective() {
	
		Linear objectiveLinear = new Linear();
		
		for (LPVariablePair v : variables) {
			
			if (v.getConnectors().get(0).terrain) {
			
				objectiveLinear.add(1, v.posVar());
				objectiveLinear.add(1, v.negVar());
				
			}
			
		}
		
		return objectiveLinear;
		
	}
	
}
