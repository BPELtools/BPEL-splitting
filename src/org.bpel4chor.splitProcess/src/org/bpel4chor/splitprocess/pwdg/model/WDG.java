package org.bpel4chor.splitprocess.pwdg.model;

import java.util.Set;

import org.eclipse.bpel.model.Activity;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.graph.DefaultEdge;

/**
 * Writer Dependency Graph uses {@link WDGNode} as vertex and
 * {@link DefaultEdge} as edge.
 * 
 * @since Feb 23, 2012
 * @author Daojun Cui
 */
public class WDG extends DirectedAcyclicGraph<WDGNode, DefaultEdge> {

	private static final long serialVersionUID = -2245556128630600859L;

	public WDG() {
		super(DefaultEdge.class);
	}

	/**
	 * Get the edge with the given source activity and target activity
	 * 
	 * @param sourceAct
	 * @param targetAct
	 * @return The edge or null if none matched
	 */
	public DefaultEdge getEdge(Activity sourceAct, Activity targetAct) {
		Set<WDGNode> nodes = vertexSet();
		WDGNode source = null;
		WDGNode target = null;
		for (WDGNode node : nodes) {
			if (node.activity().equals(sourceAct)) {
				source = node;
				break;
			}
		}
		for (WDGNode node : nodes) {
			if (node.activity().equals(targetAct)) {
				target = node;
				break;
			}
		}
		return getEdge(source, target);
	}

	/**
	 * Return the WDG node that contains the activity given, null if none
	 * matched.
	 * 
	 * @param act
	 * @return
	 */
	public WDGNode getVertex(Activity act) {
		Set<WDGNode> nodes = vertexSet();
		for (WDGNode node : nodes) {
			if (node.activity().equals(act)) {
				return node;
			}
		}
		return null;
	}
}
