package org.bpel4chor.splitprocess.utils;

import java.util.List;

import org.eclipse.bpel.model.Variable;

public class VariableUtil {
	/**
	 * Test if the variable already exists in the variables list
	 * 
	 * @param var
	 *            The given variable
	 * @param variables
	 *            The given variables
	 * @return <tt>true</tt> if the variable is already existed in the given
	 *         variable list. otherwise <tt>false</tt>
	 */
	public static boolean isExistedVariable(Variable var, List<Variable> variables) {

		if (var == null || variables == null)
			throw new NullPointerException("argument is null, var == null:" + (var == null) + " variables == null:"
					+ (variables == null));

		for (Variable next : variables) {
			if (next.equals(var) || next.getName().equals(var.getName()))
				return true;
		}
		return false;
	}
}
