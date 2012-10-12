package org.bpel4chor.splitprocess.pwdg.model;

import java.util.HashSet;
import java.util.Set;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.graph.DefaultEdge;

/**
 * Partitioned Writer Dependency Graph (PWDG) uses {@link PWDGNode} as vertex
 * and {@link DefaultEdge} as edge.
 * <p>
 * It can be regarded as a partitioned {@link WDG}.
 * 
 * @since Feb 23, 2012
 * @author Daojun Cui
 */
public class PWDG extends DirectedAcyclicGraph<PWDGNode, DefaultEdge> {

	private static final long serialVersionUID = 7482991946837553037L;

	public PWDG() {
		super(DefaultEdge.class);
	}

	/**
	 * Get nodes in the participant
	 * 
	 * @param participant
	 * @return
	 */
	public Set<PWDGNode> getNodeSet(String participant) {
		if (participant == null)
			throw new NullPointerException();

		Set<PWDGNode> nodes = new HashSet<PWDGNode>();
		for (PWDGNode node : vertexSet()) {
			if (node.getParticipant().equals(participant)) {
				nodes.add(node);
			}
		}

		return nodes;
	}

	/**
	 * Get the node with the given participant name and wdgNode, return null if
	 * no match found.
	 * 
	 * @param participant
	 * @param wdgNode
	 * @return
	 */
	public PWDGNode getNodeWith(String participant, WDGNode wdgNode) {
		if (participant == null || wdgNode == null)
			throw new NullPointerException();

		for (PWDGNode node : vertexSet()) {
			if (node.getParticipant().equals(participant) && node.getWdgNodes().contains(wdgNode))
				return node;
		}

		return null;
	}
}
