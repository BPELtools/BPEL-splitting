package org.bpel4chor.splitprocess.pwdg.util;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.bpel4chor.splitprocess.exceptions.PartitionSpecificationException;
import org.bpel4chor.splitprocess.partition.model.Participant;
import org.bpel4chor.splitprocess.pwdg.Path;
import org.bpel4chor.splitprocess.pwdg.model.PWDGNode;
import org.bpel4chor.splitprocess.pwdg.model.WDG;
import org.bpel4chor.splitprocess.pwdg.model.WDGNode;
import org.eclipse.bpel.model.Activity;
import org.jgrapht.graph.DefaultEdge;

/**
 * PWDGNodeConstructor forms the pwdgNodes with the given WDG, the temporary
 * root nodes, and the wdgNodes in each participant.
 * <p>
 * <b>To notice</b>: All the operations on the PWDG is on WDG, not on BPEL
 * process. E.g. getting a wdgNode(activity)'s adjacent nodes, we must do it the
 * WDG way, NOT via get targets form its BPEL link.
 * 
 * @since Feb 29, 2012
 * @author Daojun Cui
 */
public class PWDGNodeConstructor {

	protected WDG wdg = null;

	/** The participant to wdgNodes map */
	protected Map<Participant, Set<WDGNode>> part2wdgNodeMap = null;

	/** The participant to temporary root node map */
	protected Map<Participant, WDGNode> part2rootMap = null;

	/** The wdgNodes to check in the participant */
	private Set<WDGNode> toCheck = null;

	/** Queue for iteration putting nodes in pwdgNode */
	private Queue<WDGNode> queue = null;

	/** The start point for putting nodes in pwdgNode */
	private WDGNode root = null;

	/**
	 * 
	 * @param wdg
	 *            WDG
	 * @param part2rootMap
	 *            The participant to temporary root node map
	 * @param part2wdgNodeMap
	 *            The
	 */
	public PWDGNodeConstructor(WDG wdg, Map<Participant, WDGNode> part2rootMap,
			Map<Participant, Set<WDGNode>> part2wdgNodeMap) {

		if (wdg == null || part2rootMap == null || part2wdgNodeMap == null)
			throw new NullPointerException();

		this.wdg = wdg;
		this.part2rootMap = part2rootMap;
		this.part2wdgNodeMap = part2wdgNodeMap;
		this.toCheck = new HashSet<WDGNode>();
		this.queue = new LinkedList<WDGNode>();

	}

	/**
	 * Form the pwdg nodes by applying the greedy principle.
	 * <p>
	 * A queue is used to save the start point of each pwdg node, the wdgNodes
	 * from the current participant are held in toCheck, and they are removed
	 * from the toCheck one by one, and are added to the current pwdg node
	 * recursively, as long as the path constraint is not violated. If there is
	 * path via other participant from the parent wdgNode to the current one,
	 * then the current wdgNode will be added to the queue, and be handled with
	 * as start point for the next new pwdg node.
	 * 
	 * @return
	 * @throws PartitionSpecificationException
	 */
	public Set<PWDGNode> formNodes() throws PartitionSpecificationException {

		Set<PWDGNode> pwdgNodes = new HashSet<PWDGNode>();

		Set<Participant> participants = part2wdgNodeMap.keySet();
		for (Participant participant : participants) {

			// get the temporary root node
			root = part2rootMap.get(participant);
			if (root == null)
				continue;

			// prepare the toCheck and queue in the participant
			toCheck = new HashSet<WDGNode>();
			toCheck.addAll(part2wdgNodeMap.get(participant));
			queue = new LinkedList<WDGNode>();

			// start with root
			queue.add(root);

			// put wdg nodes into newPwdgNode
			while (!queue.isEmpty()) {
				WDGNode top = queue.remove();
				PWDGNode pwdgNode = new PWDGNode(participant.getName());

				// add the first node
				pwdgNode.add(top);

				// add his children recursively until path constraint violated
				Set<WDGNode> adjacents = getTargets(top);
				for (WDGNode current : adjacents)
					addRecursivelyToPWDGNode(top, current, pwdgNode, participant);

				pwdgNodes.add(pwdgNode);
			}
		}

		return pwdgNodes;
	}

	/**
	 * Recursively add current node to pwdgNode.
	 * <p>
	 * If the current node is already outside of the participant, simply stop.
	 * <p>
	 * If path constraint is violated, put it to the queue, for next pwdgNode.
	 * 
	 * @param parent
	 *            parent node
	 * @param current
	 *            The node that the parent node targets to.
	 * @param pwdgNode
	 *            The current pwdgNode
	 * @param participant
	 *            The current participant the pwdgNode resides
	 * @throws PartitionSpecificationException
	 */
	protected void addRecursivelyToPWDGNode(WDGNode parent, WDGNode current, PWDGNode pwdgNode, Participant participant)
			throws PartitionSpecificationException {

		if (parent == null || current == null || participant == null)
			throw new NullPointerException();

		// current already placed in other partition
		if (toCheck.contains(current) == false)
			return;

		// current still in the participant
		if (pathViaOtherParticipant(parent, current, participant)) {
			queue.add(current);
			return;
		}
		// add current to pwdgNode, removed from toCheck
		pwdgNode.add(current);
		toCheck.remove(current);

		Set<WDGNode> children = getTargets(current);
		for (WDGNode child : children) {
			addRecursivelyToPWDGNode(current, child, pwdgNode, participant);
		}
	}

	/**
	 * Get adjacent nodes in the outgoing direction of the given node.
	 * 
	 * @param wdg
	 *            WDG graph with activity as node
	 * @param source
	 *            The source node
	 * @return
	 */
	protected Set<WDGNode> getTargets(WDGNode source) {

		if (source == null)
			throw new NullPointerException();

		Set<DefaultEdge> outgoingEdges = wdg.outgoingEdgesOf(source);
		Set<WDGNode> targets = new HashSet<WDGNode>();
		for (DefaultEdge edge : outgoingEdges) {
			WDGNode v = wdg.getEdgeTarget(edge);
			targets.add(v);
		}
		return targets;
	}

	/**
	 * Test whether there is a path (parent->current) crosses over to the other
	 * participant
	 * 
	 * @param start
	 *            The start node
	 * @param end
	 *            The end node
	 * @param participant
	 *            The participant the nodes are associated to.
	 * @return true if path via other participant found, otherwise false.
	 */
	protected boolean pathViaOtherParticipant(WDGNode start, WDGNode end, Participant participant) {

		if (start == null || end == null || participant == null)
			throw new NullPointerException();

		Path<WDGNode> visited = new Path<WDGNode>();
		return findPathViaOtherParticipant(visited, start, end, participant);

	}

	/**
	 * Recursively find all paths from current node to end node, once a path is
	 * found, that crosses over to other path, return true, otherwise go
	 * further, at the end return false.
	 * <p>
	 * <b>Note</b>: The navigation on the path relays on the wdg edges, NOT bpel
	 * links.
	 * 
	 * @param visited
	 *            The visited path
	 * @param current
	 *            The current node
	 * @param end
	 *            The node in the current participant
	 * @param participant
	 * @return
	 * @see WDGFactory#findAllPath(Path, Activity, Activity, java.util.List)
	 */
	protected boolean findPathViaOtherParticipant(Path<WDGNode> visited, WDGNode current, WDGNode end,
			Participant participant) {

		if (visited == null || current == null || end == null || participant == null)
			throw new NullPointerException();

		if (current.equals(end))
			return false;

		visited.append(current);

		Set<WDGNode> targets = getTargets(current);
		// stop by no more children
		if (targets.isEmpty()) {
			visited.removeTail();
			return false;
		}

		// examine all the adjacent target nodes
		for (WDGNode target : targets) {

			if (target.equals(end)) {
				// a path found, test it
				//Path found = visited.clone();
				int startIdx = 0;
				int endIdx = visited.length();
				for (int i = endIdx - 1; i > startIdx; i--) {
					if (!isInParticipant(visited.get(i), participant))
						return true;
				}

			} else if (visited.contains(target) == false) {
				boolean pathViaOtherParticipant = findPathViaOtherParticipant(visited, target, end, participant);
				if (pathViaOtherParticipant)
					return true;

			}
		}

		visited.removeTail();
		return false;
	}

	/**
	 * Test whether the node given is in the participant
	 * 
	 * @param node
	 * @param participant
	 * @return
	 */
	protected boolean isInParticipant(WDGNode node, Participant participant) {
		for (WDGNode wdgNode : part2wdgNodeMap.get(participant)) {
			if (wdgNode.equals(node))
				return true;
		}
		return false;
	}

	protected Set<WDGNode> getToCheck() {
		if (toCheck == null)
			toCheck = new HashSet<WDGNode>();
		return toCheck;
	}

	protected void setToCheck(Set<WDGNode> toCheck) {
		this.toCheck = toCheck;
	}

	protected Queue<WDGNode> getQueue() {
		if (this.queue == null)
			this.queue = new LinkedList<WDGNode>();
		return this.queue;
	}

	protected void setQueue(Queue<WDGNode> queue) {
		this.queue = queue;
	}

	protected WDGNode getRoot() {
		return root;
	}

	protected void setRoot(WDGNode root) {
		this.root = root;
	}

}
