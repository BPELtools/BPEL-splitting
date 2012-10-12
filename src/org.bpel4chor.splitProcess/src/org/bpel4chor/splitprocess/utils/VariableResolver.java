package org.bpel4chor.splitprocess.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.Compensate;
import org.eclipse.bpel.model.CompensateScope;
import org.eclipse.bpel.model.Condition;
import org.eclipse.bpel.model.Copy;
import org.eclipse.bpel.model.ElseIf;
import org.eclipse.bpel.model.Empty;
import org.eclipse.bpel.model.Exit;
import org.eclipse.bpel.model.Expression;
import org.eclipse.bpel.model.ExtensionActivity;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.ForEach;
import org.eclipse.bpel.model.From;
import org.eclipse.bpel.model.If;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.OpaqueActivity;
import org.eclipse.bpel.model.Pick;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.RepeatUntil;
import org.eclipse.bpel.model.Reply;
import org.eclipse.bpel.model.Rethrow;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.Source;
import org.eclipse.bpel.model.Throw;
import org.eclipse.bpel.model.To;
import org.eclipse.bpel.model.Validate;
import org.eclipse.bpel.model.Variable;
import org.eclipse.bpel.model.Wait;
import org.eclipse.bpel.model.While;

/**
 * VariableResolver collects the both the read and written variables in the
 * activity.
 * 
 * 
 * @since Feb 13, 2012
 * @author Daojun Cui
 */
public class VariableResolver {

	protected Process process = null;

	public VariableResolver(Process process) {
		this.process = process;
	}

	/**
	 * Get the variables that are only read by the given activity
	 * <p>
	 * <b>Note</b>: This method for now supports only the minimal set: invoke,
	 * reply, receive, assign.
	 * 
	 * @param act
	 * @return
	 */
	public List<Variable> resolveReadVariable(Activity act) {
		VariableCollector collector = new VariableCollector();
		if (act instanceof Invoke) {
			Variable inputVariable = ((Invoke) act).getInputVariable();
			if (inputVariable != null)
				collector.add(inputVariable);
		} else if (act instanceof Reply) {
			Variable replyVariable = ((Reply) act).getVariable();
			if (replyVariable != null)
				collector.add(replyVariable);
		} else if (act instanceof Assign) {
			Assign assign = (Assign) act;
			collector.addAll(this.getReadVariablesInAssign(assign));
		}
		// TODO get variable used by the incoming joinCondition
		
		// get variables used by the outgoing transmitConditions of the activity
		if (act.getSources() != null) {
			List<Source> actSources = act.getSources().getChildren();
			List<Variable> varsInCondition = new ArrayList<Variable>();
			for (Source source : actSources) {
				Condition condition = source.getTransitionCondition();
				varsInCondition.addAll(this.getVariablesInCondition(condition));
			}
			collector.addAll(varsInCondition);
		}
		return collector.getVariables();
	}

	/**
	 * get only variables that are read in the assign
	 * @param assign
	 * @return
	 */
	public Collection<Variable> getReadVariablesInAssign(Assign assign) {
		List<Variable> usedVars = new ArrayList<Variable>();

		if (assign == null)
			return usedVars;

		List<Copy> copyList = assign.getCopy();
		for (Copy copy : copyList) {
			From from = copy.getFrom();
			if (from.getVariable() != null) {
				usedVars.add(from.getVariable());
			} else if (from.getExpression() != null) {
				usedVars.addAll(this.getVariableInExpression(from.getExpression()));
			} else if (from.getLiteral() != null) {
				usedVars.addAll(this.getVariableInLiteral(from.getLiteral()));
			}
		}

		return usedVars;
	}

	/**
	 * Get variables that are read or written in the given activity
	 * 
	 * @param act
	 *            The activity
	 * @return The used variables that are used in the activity
	 */
	public List<Variable> resolveVariable(Activity act) {

		VariableCollector collector = new VariableCollector();

		if (act == null) {
			throw new NullPointerException("Null argument for getVariablesInActivity(), act == null:" + (act == null));
		}

		// get variables the activity directly uses
		if (act instanceof Invoke) {
			Variable inputVariable = ((Invoke) act).getInputVariable();
			Variable outputVariable = ((Invoke) act).getOutputVariable();
			if (inputVariable != null)
				collector.add(inputVariable);
			if (outputVariable != null)
				collector.add(outputVariable);

		} else if (act instanceof Reply) {
			Variable replyVariable = ((Reply) act).getVariable();
			if (replyVariable != null)
				collector.add(replyVariable);

		} else if (act instanceof Receive) {
			Variable receiveVariable = ((Receive) act).getVariable();
			if (receiveVariable != null)
				collector.add(receiveVariable);

		} else if (act instanceof Assign) {
			Assign assign = (Assign) act;
			collector.addAll(this.getVariablesInAssign(assign));

		} else if (act instanceof Throw) {
			Variable faultVariable = ((Throw) act).getFaultVariable();
			if (faultVariable != null)
				collector.add(faultVariable);
		} else if (act instanceof If) {
			// if-condition
			Condition ifCondition = ((If) act).getCondition();
			List<Variable> varsIfCond = this.getVariablesInCondition(ifCondition);
			collector.addAll(varsIfCond);

			// elseif-condition
			List<ElseIf> elseIfList = ((If) act).getElseIf();
			for (ElseIf elseIf : elseIfList) {
				Condition elseIfCondition = elseIf.getCondition();
				List<Variable> varsElseIfCond = this.getVariablesInCondition(elseIfCondition);
				collector.addAll(varsElseIfCond);
			}

			// else-branch does not have condition

		} else if (act instanceof While) {
			// while condition
			While whileAct = (While) act;
			Condition whileCond = whileAct.getCondition();
			if (whileAct.getCondition() != null) {
				collector.addAll(this.getVariablesInCondition(whileCond));
			}
		} 
//		else if (act instanceof Empty) {
//		} else if (act instanceof Wait) {
//		} else if (act instanceof Exit) {
//		} else if (act instanceof Flow) {
//		} else if (act instanceof Sequence) {
//		} else if (act instanceof Pick) {
//		} else if (act instanceof Scope) {
//		} else if (act instanceof Compensate) {
//		} else if (act instanceof CompensateScope) {
//		} else if (act instanceof Rethrow) {
//		} else if (act instanceof OpaqueActivity) {
//		} else if (act instanceof ForEach) {
//		} else if (act instanceof RepeatUntil) {
//		} else if (act instanceof Validate) {
//		} else if (act instanceof ExtensionActivity) {
//		}

		// get variables used by the outgoing transmitConditions of the activity
		if (act.getSources() != null) {
			List<Source> actSources = act.getSources().getChildren();
			List<Variable> varsInCondition = new ArrayList<Variable>();
			for (Source source : actSources) {
				Condition condition = source.getTransitionCondition();
				varsInCondition.addAll(this.getVariablesInCondition(condition));
			}
			collector.addAll(varsInCondition);
		}

		return collector.getVariables();
	}

	/**
	 * Get the used variables in the assign activity
	 * 
	 * @param assign
	 *            The activity Assign
	 * @return The used variables in assign
	 * 
	 */
	public List<Variable> getVariablesInAssign(Assign assign) {
		List<Variable> usedVars = new ArrayList<Variable>();

		if (assign == null)
			return usedVars;

		List<Copy> copyList = assign.getCopy();
		for (Copy copy : copyList) {
			From from = copy.getFrom();
			To to = copy.getTo();
			if (from.getVariable() != null) {
				usedVars.add(from.getVariable());
			} else if (from.getExpression() != null) {
				usedVars.addAll(this.getVariableInExpression(from.getExpression()));
			} else if (from.getLiteral() != null) {
				usedVars.addAll(this.getVariableInLiteral(from.getLiteral()));
			}
			if (to.getVariable() != null) {
				usedVars.add(to.getVariable());
			}
		}

		return usedVars;
	}

	/**
	 * Test whether the first list of activities is contained in the second one.
	 * 
	 * @param actLsit1
	 *            The activity list 1
	 * @param actList2
	 *            The activity list 2
	 * @return <tt>true</tt> if the first list is contained in the second list,
	 *         otherwise <tt>false</tt>
	 */
	public boolean containedIn(List<Activity> actLsit1, List<Activity> actList2) {
		throw new IllegalStateException("containedIn is not yet implemented");
	}

	/**
	 * Get the variables that reside in the condition body
	 * 
	 * <p>
	 * <b>Note</b>: It is assumed that only global variables are used.
	 * 
	 * @param condition
	 * @return The variables that are used in the condition body
	 */
	public List<Variable> getVariablesInCondition(Condition condition) {

		if (condition == null)
			return new ArrayList<Variable>();

		String condString = condition.getBody().toString();
		return getVariableInLiteral(condString);
	}

	/**
	 * Get used variables in the expression
	 * 
	 * @param expression
	 *            The expression
	 * @return The used variables
	 */
	public List<Variable> getVariableInExpression(Expression expression) {
		if (expression == null)
			throw new NullPointerException("argument is null");

		String expString = expression.getBody().toString();
		return getVariableInLiteral(expString);
	}

	/**
	 * Get used variables in the literal string
	 * 
	 * @param literal
	 *            The literal string
	 * @return The used variables
	 */
	public List<Variable> getVariableInLiteral(String literal) {
		if (literal == null || literal.isEmpty())
			return new ArrayList<Variable>();

		List<Variable> results = new ArrayList<Variable>();
		List<String> varStrList = new ArrayList<String>();

		// extract the variable strings from condition expression body
		Pattern pattern = Pattern.compile("\\$[0-9a-zA-Z_.]+");
		Matcher matcher = pattern.matcher(literal);
		while (matcher.find()) {
			String var = literal.substring(matcher.start() + 1, matcher.end());
			// the variable string might also contain the sub-variable part.
			// e.g. "orderInfo.payload", "payload" isn't wanted , only
			// "orderInfo" is desired.
			if (var.contains("."))
				varStrList.add(var.substring(0, var.indexOf('.')));
			else
				varStrList.add(var);
		}

		List<Variable> processVariables = this.process.getVariables().getChildren();
		for (String varStr : varStrList) {
			for (Variable var : processVariables) {
				if (var.getName().equals(varStr) && (!VariableUtil.isExistedVariable(var, results))) {
					results.add(var);
				}
			}
		}

		return results;
	}
}
