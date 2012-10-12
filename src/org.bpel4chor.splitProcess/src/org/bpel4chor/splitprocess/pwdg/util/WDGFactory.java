package org.bpel4chor.splitprocess.pwdg.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bpel4chor.splitprocess.exceptions.WDGException;
import org.bpel4chor.splitprocess.pwdg.Path;
import org.bpel4chor.splitprocess.pwdg.model.WDG;
import org.bpel4chor.splitprocess.pwdg.model.WDGNode;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Source;
import org.eclipse.bpel.model.Sources;
import org.eclipse.bpel.model.Target;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;

/**
 * WDGFactory creates WDG graph with the nodes given.
 * 
 * @since Feb 24, 2012
 * @author Daojun Cui
 */
public class WDGFactory {

	/**
	 * Create WDG with the given WDGNodes
	 * 
	 * @param wdgNodes
	 * @return Created WDG graph
	 * @throws WDGException
	 */
	public static WDG createWDG(Set<WDGNode> wdgNodes) throws WDGException {
		if (wdgNodes == null)
			throw new NullPointerException();

		try {
			WDG wdg = new WDG();
			if (wdgNodes.isEmpty())
				return wdg;

			for (WDGNode v : wdgNodes)
				wdg.addVertex(v);

			WDGNode[] nodes = wdg.vertexSet().toArray(new WDGNode[0]);

			// try creating edge with each pair of the nodes
			for (int i = 0; i < nodes.length - 1; i++) {

				WDGNode v1 = nodes[i];
				for (int j = i + 1; j < nodes.length; j++) {
					
					WDGNode v2 = nodes[j];
					createEdge(v1, v2, wdg);
					
				}

			}
			return wdg;

		} catch (CycleFoundException e) {
			throw new WDGException("There is cycle in the WDG graph.", e);
		}
	}

	/**
	 * Create WDG with the given activities that are supposed to be contents of
	 * WDG node.
	 * 
	 * @param actsForWdgNode
	 * @return WDG graph, if empty list is given, then empty WDG will be
	 *         created.
	 * @throws WDGException
	 */
	public static WDG createWDG(Collection<Activity> actsForWdgNode) throws WDGException {
		if (actsForWdgNode == null)
			throw new NullPointerException();

		// if empty list is given, then return empty WDG
		if (actsForWdgNode.isEmpty())
			return new WDG();

		Set<WDGNode> wdgNodes = new HashSet<WDGNode>();
		for (Activity act : actsForWdgNode) {
			WDGNode node = new WDGNode(act);
			wdgNodes.add(node);
		}
		return createWDG(wdgNodes);
	}

	/**
	 * Create an edge with node1 and node2 in the WDG given, if there is a path
	 * between the two nodes, and between them there is no other node from WDG.
	 * 
	 * @param node1
	 *            WDGNode
	 * @param node2
	 *            WDGNode
	 * @param wdg
	 *            The underlying WDG graph
	 * @throws CycleFoundException
	 */
	protected static void createEdge(WDGNode node1, WDGNode node2, WDG wdg) throws CycleFoundException {

		if (node1 == null || node2 == null || wdg == null)
			throw new NullPointerException();

		//
		// 1. recursively search all paths in the BPEL process from node1 to
		// node2,
		// 2. if we find one path that has no intermediate activity, which also
		// belongs to the WDG nodes, then create edge with the both nodes.
		//

		// node1 -> node2
		List<Path<Activity>> paths = new ArrayList<Path<Activity>>();
		Path<Activity> currentPath = new Path<Activity>();
		findAllPath(currentPath, node1.activity(), node2.activity(), paths);

		if (paths.size() != 0) {
			for (Path<Activity> path : paths) {
				if (pathContainIntermediateWdgNode(path, wdg.vertexSet()) == false) {
					wdg.addDagEdge(node1, node2);
					return;
				}
			}
		}

		// node2 -> node1
		paths = new ArrayList<Path<Activity>>();
		currentPath = new Path<Activity>();
		findAllPath(currentPath, node2.activity(), node1.activity(), paths);

		if (paths.size() != 0) {
			for (Path<Activity> path : paths) {
				if (pathContainIntermediateWdgNode(path, wdg.vertexSet()) == false) {
					wdg.addDagEdge(node2, node1);
					return;
				}
			}
		}

	}

	/**
	 * Recursively find all paths from start activity to end activity, and save
	 * them into found list.
	 * 
	 * @param visitedPath
	 *            visited activities
	 * @param current
	 *            The current activity
	 * @param end
	 *            The destination activity, NOT equals to the current activity.
	 * @param foundList
	 *            The list of found paths
	 */
	protected static void findAllPath(Path<Activity> visitedPath, Activity current, Activity end,
			List<Path<Activity>> foundList) {

		if (visitedPath == null || current == null || end == null || foundList == null)
			throw new NullPointerException();

		if (current.equals(end))
			return;

		visitedPath.append(current);

		// stop by no more children
		Sources sources = current.getSources();
		if (sources == null) {
			visitedPath.removeTail();
			return;
		}

		// examine adjacent nodes
		for (Source source : sources.getChildren()) {
			Link link = source.getLink();
			Target target = link.getTargets().get(0);
			Activity act = target.getActivity();

			if (act.equals(end)) {
				// found a path , save a copy
				Path<Activity> found = visitedPath.clone();
				found.append(end);
				foundList.add(found);
			} else if (visitedPath.contains(act) == false) {
				// recursively further
				findAllPath(visitedPath, act, end, foundList);
			}
		}
		visitedPath.removeTail();
	}

	/**
	 * Test whether one of the intermediates between the start- and end-node is
	 * also WDG node .
	 * 
	 * @param path
	 * @param wdgNodes
	 * @return
	 */
	protected static boolean pathContainIntermediateWdgNode(Path<Activity> path, Set<WDGNode> wdgNodes) {
		if (path == null || wdgNodes == null)
			throw new NullPointerException();
		if (path.length() > 2) {
			// test only the intermediates, start and end points are ignored.
			for (int i = 1; i < path.length() - 1; i++) {
				Activity act = path.get(i);
				// if wdgNodes contains this activity, return true
				for (WDGNode node : wdgNodes)
					if (node.activity().equals(act))
						return true;
			}
		}
		return false;
	}

	/**
	 * Test whether both activities are equal.
	 * 
	 * @param act1
	 *            Activity from process
	 * @param act2
	 *            Activity from process
	 * @return true if they equal, otherwise false
	 */
	protected static boolean isEquals(Activity act1, Activity act2) {
		if (act1 == null && act2 == null) {
			return true;

		} else if (act1 != null && act2 != null) {
			return act1.equals(act2);

		} else {
			return false;
		}
	}

}
