package org.bpel4chor.splitprocess.pwdg.model;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.bpel.model.Activity;

/**
 * PWDGNode contains a participant name and a set of wdgNodes, and represents a
 * largest weakly-connected subgraph in the participant.
 * 
 * @since Feb 23, 2012
 * @author Daojun Cui
 */
public class PWDGNode {

	String participant = null;

	Set<WDGNode> wdgNodes = null;

	public PWDGNode() {
		this.participant = "";
		this.wdgNodes = new HashSet<WDGNode>();
	}

	public PWDGNode(String participant, Set<WDGNode> nodes) {
		if (participant == null || nodes == null)
			throw new NullPointerException();
		this.participant = participant;
		this.wdgNodes = nodes;
	}

	public PWDGNode(String participant) {
		if (participant == null)
			throw new NullPointerException();
		this.participant = participant;
		this.wdgNodes = new HashSet<WDGNode>();
	}

	public void add(WDGNode node) {
		if (node == null)
			throw new NullPointerException();
		this.wdgNodes.add(node);
	}

	/**
	 * Remove the wdgNode given, when it exists.
	 * 
	 * @param wdgNode
	 * @return
	 */
	public boolean remove(WDGNode wdgNode) {
		if (wdgNode == null)
			throw new NullPointerException();
		for (WDGNode node : this.wdgNodes) {
			if (node.equals(wdgNode))
				return this.wdgNodes.remove(node);
		}
		return false;
	}

	public String getParticipant() {
		return participant;
	}

	public void setParticipant(String participant) {
		if (participant == null)
			throw new NullPointerException();
		this.participant = participant;
	}

	/**
	 * Get the activities that are contained in the PWDG node
	 * @return
	 */
	public Set<Activity> getActivities() {
		Set<Activity> acts = new HashSet<Activity>();
		for (WDGNode node : wdgNodes) {
			acts.add(node.activity());
		}
		return acts;
	}

	/**
	 * Get the activity in the PWDG node by the name given
	 * 
	 * @param actName
	 * @return
	 */
	public Activity getActivity(String actName) {
		Activity act = null;
		for (WDGNode node : wdgNodes) {
			act = node.getActivity();
			if (actName.equals(act.getName())) {
				return act;
			}
		}
		return null;
	}

	public Set<WDGNode> getWdgNodes() {
		return this.wdgNodes;
	}

	public void setWdgNodes(Set<WDGNode> nodes) {
		if (nodes == null)
			throw new NullPointerException();
		this.wdgNodes = nodes;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		for (WDGNode node : wdgNodes) {
			sb.append(node.toString());
			sb.append(" ");
		}
		sb.append("]");
		return sb.toString();
	}
}
