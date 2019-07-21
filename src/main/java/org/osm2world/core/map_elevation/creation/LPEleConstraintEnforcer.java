package org.osm2world.core.map_elevation.creation;

import static org.osm2world.core.map_elevation.data.GroundState.ON;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.LPVariablePair;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.util.FaultTolerantIterationUtil;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.sf.javailp.Linear;
import net.sf.javailp.OptType;
import net.sf.javailp.Problem;
import net.sf.javailp.Result;
import net.sf.javailp.Solver;
import net.sf.javailp.SolverFactory;
import net.sf.javailp.SolverFactoryLpSolve;

/**
 * enforces constraints using linear programming
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

		//TODO: calling this multiple times does not work yet; connectors from different invocations would not be joined

		for (LPVariablePair v : createVariables(connectors)) {

			variables.add(v);

			for (EleConnector c : v.getConnectors()) {
				variableMap.put(c, v);
			}

		}

	}

	/**
	 * creates variables for connectors. Joins connected connectors by
	 * representing their elevation with the same {@link LPVariablePair}.
	 */
	private static Collection<LPVariablePair> createVariables(
			Iterable<EleConnector> connectors) {

		Multimap<VectorXZ, LPVariablePair> variablePositionMap =
				HashMultimap.create();

		for (EleConnector c : connectors) {

			Collection<LPVariablePair> existingVariables =
					variablePositionMap.get(c.pos);

			List<LPVariablePair> matchingVariables =
					new ArrayList<LPVariablePair>();

			if (existingVariables != null) {
				for (LPVariablePair v : existingVariables) {
					if (v.connectsTo(c)) {
						matchingVariables.add(v);
					}
				}
			}

			if (matchingVariables.isEmpty()) {

				/* create a new variable because no existing one fits */

				variablePositionMap.put(c.pos, new LPVariablePair(c));

			} else {

				/* add connector to one of the matching variables */

				LPVariablePair vHead = matchingVariables.get(0);
				vHead.add(c);

				/* merge other matching variables into that one */

				for (int i = 1; i < matchingVariables.size(); i++) {

					LPVariablePair v = matchingVariables.get(i);

					vHead.addAll(v);
					variablePositionMap.remove(c.pos, v);

				}

			}

		}

		return variablePositionMap.values();

	}

	@Override
	public void requireSameEle(EleConnector c1, EleConnector c2) {

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
	public void requireSameEle(Iterable<EleConnector> cs) {

		Iterator<EleConnector> csIterator = cs.iterator();

		if (csIterator.hasNext()) {

			EleConnector c = csIterator.next();

			while (csIterator.hasNext()) {
				requireSameEle(c, csIterator.next());
			}

		}

	}

	@Override
	public void requireVerticalDistance(ConstraintType type, double distance,
			EleConnector upper, EleConnector base1, EleConnector base2) {

		double dist1 = base1.pos.distanceTo(upper.pos);
		double dist2 = base2.pos.distanceTo(upper.pos);

		addConstraint(
				 1, upper,
				-(dist2 / (dist1 + dist2)), base1,
				-(dist1 / (dist1 + dist2)), base2,
				getOperator(type),
				distance);

	}

	@Override
	public void requireVerticalDistance(ConstraintType type, double distance,
			EleConnector upper, EleConnector lower) {

		addConstraint(
				 1, upper,
				-1, lower,
				getOperator(type),
				distance);

	}

	@Override
	public void requireIncline(ConstraintType type, double incline,
			List<EleConnector> cs) {

		for (int i = 0; i+1 < cs.size(); i++) {

			addConstraint(
					 1, cs.get(i+1),
					-1, cs.get(i),
					getOperator(type),
					incline * cs.get(i).pos.distanceTo(cs.get(i+1).pos));

		}

	}

	@Override
	public void requireSmoothness(EleConnector from,
			EleConnector via, EleConnector to) {

		/* TODO restore

		double maxInclineDiffPerMeter = 0.5 / 100;

		double dist12 = from.pos.distanceTo(via.pos);
		double dist23 = via.pos.distanceTo(to.pos);

		double maxInclineDiff = maxInclineDiffPerMeter * (dist12 + dist23);

		System.out.println(maxInclineDiff);

		//| - 1/dist12 * from + (1/dist12 + 1/dist23) * via - 1/dist23 * to |
		//   <= maxInclineDiff

		addConstraint(
				-1/dist12, from,
				1/dist12 + 1/dist23, via,
				-1/dist23, to,
				"<=", maxInclineDiff);

		addConstraint(
				-1/dist12, from,
				1/dist12 + 1/dist23, via,
				-1/dist23, to,
				">=", -maxInclineDiff);

		*/

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

	private static String getOperator(ConstraintType constraintType) {

		switch (constraintType) {
		case MIN: return ">=";
		case MAX: return "<=";
		case EXACT: return "=";
		default: throw new Error("unhandled constraint type");
		}

	}

	@Override
	public void enforceConstraints() {

		SolverFactory factory = new SolverFactoryLpSolve();
		factory.setParameter(Solver.VERBOSE, 0);

		problem.setObjective(constructObjective(), OptType.MIN);

		//TODO Relaxations relax = new Relaxations();

		final Solver solver = factory.get();
		final Result result = solver.solve(problem);

		if (result == null) {
			System.out.println("[ERROR]: cannot enforce constraints, no result for LP");
		} else {

			/* apply elevation values */

			FaultTolerantIterationUtil.iterate(variables, (LPVariablePair v) -> {

				VectorXYZ posXYZ = v.getPosXYZ().addY(
						+ result.get(v.posVar()).doubleValue()
						- result.get(v.negVar()).doubleValue());

				v.setPosXYZ(posXYZ);

			});

		}

	}

	private Linear constructObjective() {

		Linear objectiveLinear = new Linear();

		for (LPVariablePair v : variables) {

			if (v.getConnectors().get(0).groundState == ON) {

				objectiveLinear.add(1, v.posVar());
				objectiveLinear.add(1, v.negVar());

			}

		}

		return objectiveLinear;

	}

}
