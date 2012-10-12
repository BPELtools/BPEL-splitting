package org.bpel4chor.splitprocess.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.bpel.model.Variable;

/**
 * Variable Collector collects variables that has NOT been collected in the
 * list.
 * 
 * <p>
 * <b>changeLog date user remark</b> <br>
 * 
 * @001 2012-01-30 DC initial version
 * 
 * @since Jan 30, 2012
 * @author Daojun Cui
 */
public class VariableCollector {

	private List<Variable> variables = null;

	/**
	 * Constructor
	 * 
	 * @param references
	 *            The References List
	 */
	public VariableCollector() {
		this.variables = new ArrayList<Variable>();
	}

	public List<Variable> getVariables() {
		return this.variables;
	}

	public void add(Variable var) {
		if (!isExisted(var)) {
			this.variables.add(var);
		}
	}

	public void addAll(Collection<Variable> vars) {
		for (Variable var : vars) {
			if (!isExisted(var)) {
				this.variables.add(var);
			}
		}
	}

	/**
	 * Test if the variable already exists in the variables list
	 * 
	 * @param var
	 *            The variable to test
	 * @return Whether the variable has already existed in the list.
	 */
	public boolean isExisted(Variable var) {

		if (var == null)
			throw new NullPointerException("argument is null, var == null:" + (var == null));
		if (variables == null)
			throw new IllegalStateException("illegal state, variables == null:" + (variables == null));

		for (Variable next : variables) {
			if (next.equals(var) || next.getName().equals(var.getName()))
				return true;
		}
		return false;
	}

}