package org.bpel4chor.splitprocess.pwdg.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bpel4chor.splitprocess.exceptions.PWDGException;
import org.bpel4chor.splitprocess.exceptions.PartitionSpecificationException;
import org.bpel4chor.splitprocess.partition.model.Participant;
import org.bpel4chor.splitprocess.partition.model.PartitionSpecification;
import org.bpel4chor.splitprocess.pwdg.model.PWDG;
import org.bpel4chor.splitprocess.pwdg.model.PWDGNode;
import org.bpel4chor.splitprocess.pwdg.model.WDG;
import org.bpel4chor.splitprocess.pwdg.model.WDGNode;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.Process;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;
import org.jgrapht.graph.DefaultEdge;

/**
 * PWDGFactory constructs PWDG upon an activity and a variable that it reads.
 * 
 * @since Feb 23, 2012
 * @author Daojun Cui
 */
public class PWDGFactory {

	/**
	 * Construct PWDG based on the WDG and partition.
	 * 
	 * @param wdg
	 *            The WDG graph
	 * @param process
	 *            The non-split process
	 * @param partitionSpec
	 *            The partition specification
	 * @return PWDG
	 * @throws PWDGException 
	 */
	public static PWDG createPWDG(WDG wdg, Process process, PartitionSpecification partitionSpec)
			throws PWDGException {

		try {
			if (wdg == null || partitionSpec == null)
				throw new NullPointerException();

			// participant to WDG node map
			Map<Participant, Set<WDGNode>> part2WDGNodeMap = new HashMap<Participant, Set<WDGNode>>();

			// temporary root nodes
			Map<Participant, WDGNode> part2RootMap = new HashMap<Participant, WDGNode>();

			// 1. place temporary root in each partition of the WDG, cache the data
			// into part2RootMap, adn part2WDGNodeMap
			insertTempRootNode(wdg, part2RootMap, part2WDGNodeMap, process, partitionSpec);

			// 2. form largest weakly connected subgraphs i.e. PWDG Nodes
			Set<PWDGNode> pwdgNodes = formPWDGNodes(wdg, part2RootMap, part2WDGNodeMap);

			// 3. remove temporary root
			removeTempRootNode(wdg, part2RootMap, pwdgNodes);

			// new pwdg
			PWDG pwdg = new PWDG();
			for (PWDGNode v : pwdgNodes)
				pwdg.addVertex(v);

			// create edges
			createPwdgEdge(pwdg, wdg);

			return pwdg;
		} catch (CycleFoundException e) {
			throw new PWDGException("Cycle found in PWDG.", e);
		} catch (PartitionSpecificationException e) {
			throw new PWDGException("Something wrong in PartitionSpecification.", e);
		} 
		
	}

	/**
	 * Create edge for pwdg
	 * <p>
	 * If any wdgNode inside a pwdgNode has edge to other wdgNode in the other
	 * pwdgNode, then between the two pwdgNodes there is an pwdgEdge.
	 * 
	 * @param pwdg
	 * @param wdg
	 * @throws CycleFoundException
	 */
	protected static void createPwdgEdge(PWDG pwdg, WDG wdg) throws CycleFoundException {
		PWDGNode[] pwdgNodes = pwdg.vertexSet().toArray(new PWDGNode[0]);
		for (int i = 0; i < pwdgNodes.length - 1; i++) {
			for (int j = i + 1; j < pwdgNodes.length; j++) {

				PWDGNode pwNode1 = pwdgNodes[i];
				PWDGNode pwNode2 = pwdgNodes[j];

				if (hasWdgEdgeBetween(pwNode1, pwNode2, wdg)) {
					pwdg.addDagEdge(pwNode1, pwNode2);
				} else if (hasWdgEdgeBetween(pwNode2, pwNode1, wdg)) {
					pwdg.addDagEdge(pwNode2, pwNode1);
				}
			}
		}
	}

	/**
	 * Test if there is wdg edge from n1 to n2
	 * 
	 * @param n1
	 * @param n2
	 * @param wdg
	 * @return
	 */
	protected static boolean hasWdgEdgeBetween(PWDGNode n1, PWDGNode n2, WDG wdg) {
		for (WDGNode wnode1 : n1.getWdgNodes()) {
			for (WDGNode wnode2 : n2.getWdgNodes()) {
				if (wdg.getEdge(wnode1, wnode2) != null)
					return true;
			}
		}
		return false;
	}

	/**
	 * Place a temporary root node in each participant where there are wdg
	 * nodes.
	 * 
	 * @param wdg
	 *            WDG graph
	 * @param part2RootMap
	 *            participant to temporary root map
	 * @param part2wdgNodeMap
	 *            Participant to WDG Nodes Set Map
	 * @param process
	 *            The non-split process
	 * @param partitionSpec
	 *            PartitionSpecification
	 * @throws CycleFoundException
	 * @throws PartitionSpecificationException
	 */
	protected static void insertTempRootNode(WDG wdg, Map<Participant, WDGNode> part2RootMap,
			Map<Participant, Set<WDGNode>> part2wdgNodeMap, Process process, PartitionSpecification partitionSpec)
			throws CycleFoundException, PartitionSpecificationException {

		if (wdg == null || part2RootMap == null || part2wdgNodeMap == null || process == null || partitionSpec == null)
			throw new NullPointerException();

		for (Participant participant : partitionSpec.getParticipants()) {

			// wdg nodes in participant
			Set<WDGNode> wdgNodesInPart = new HashSet<WDGNode>();

			// get all basic activities in this participant
			Set<Activity> actsInParticipant = participant.getActivities();

			// get all wdg nodes
			Set<WDGNode> allWdgNodes = wdg.vertexSet();

			// collect the wdg nodes that reside in this participant
			for (WDGNode node : allWdgNodes) {
				for (Activity act : actsInParticipant)
					if (node.activity().equals(act))
						wdgNodesInPart.add(node);
			}

			// if there are no nodes in this participant, move on
			if (wdgNodesInPart.size() == 0)
				continue;

			//
			// if there are wdgNodes in this participant,
			// try combine the tempRoot to the nodes that do not have
			// incoming link from the same partition
			//

			// create a temp root, add to the part2rootmap
			WDGNode tempRoot = new WDGNode(BPELFactory.eINSTANCE.createActivity());
			tempRoot.activity().setName(participant.getName().concat("RootNode"));
			part2RootMap.put(participant, tempRoot);

			// add to wdg
			wdg.addVertex(tempRoot);

			// combine tempRoot to node
			for (WDGNode node : wdgNodesInPart) {
				if (hasIncomingEdgeFromSamePartition(node, wdgNodesInPart, wdg) == false) {
					wdg.addDagEdge(tempRoot, node);
				}
			}

			// now collect tempRoot in wdgNodes of Participant too
			wdgNodesInPart.add(tempRoot);

			// save the pair participant to wdgNodeInPart in map
			part2wdgNodeMap.put(participant, wdgNodesInPart);
		}
	}

	/**
	 * Test whether the nodeInPart has incoming edge from the same partition.
	 * 
	 * @param nodeInPart
	 *            The node in the current participant
	 * @param nodesCurrPart
	 *            The wdg nodes in current participant
	 * @param wdg
	 *            The WDG graph
	 * @return true if it has incoming edge from same partition, otherwise
	 *         false.
	 */
	protected static boolean hasIncomingEdgeFromSamePartition(WDGNode nodeInPart, Set<WDGNode> nodesCurrPart, WDG wdg) {

		Set<DefaultEdge> incomingEdges = wdg.incomingEdgesOf(nodeInPart);

		for (DefaultEdge inEdge : incomingEdges) {
			WDGNode sourceNode = wdg.getEdgeSource(inEdge);
			if (nodesCurrPart.contains(sourceNode)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Form pwdg nodes
	 * 
	 * @param wdg
	 * @param part2RootMap
	 * @param part2wdgNodeMap
	 * @return
	 * @throws PartitionSpecificationException
	 */
	protected static Set<PWDGNode> formPWDGNodes(WDG wdg, Map<Participant, WDGNode> part2RootMap,
			Map<Participant, Set<WDGNode>> part2wdgNodeMap) throws PartitionSpecificationException {

		if (wdg == null || part2RootMap == null || part2wdgNodeMap == null)
			throw new NullPointerException();

		PWDGNodeConstructor helper = new PWDGNodeConstructor(wdg, part2RootMap, part2wdgNodeMap);

		Set<PWDGNode> pwdgNodes = helper.formNodes();

		return pwdgNodes;

	}

	/**
	 * Remove the temporary root node, not only from PWDG, also from WDG
	 * @param wdg 
	 * @param part2RootMap
	 * @param pwdgNodes
	 */
	protected static void removeTempRootNode(WDG wdg, Map<Participant, WDGNode> part2RootMap, Set<PWDGNode> pwdgNodes) {
		// delete from pwdg
		for (WDGNode root : part2RootMap.values()) {
			for (PWDGNode pwnode : pwdgNodes) {
				pwnode.remove(root);
			}
		}
		// delete from wdg
		for(WDGNode root : part2RootMap.values()) {
			wdg.removeVertex(root);
		}
	}
}
