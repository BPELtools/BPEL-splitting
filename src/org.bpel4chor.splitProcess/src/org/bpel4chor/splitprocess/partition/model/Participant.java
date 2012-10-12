package org.bpel4chor.splitprocess.partition.model;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.bpel.model.Activity;
/**
 * Implementation of participant
 * <P>
 * Note: this class is different from org.bpel4chor.model.topology.Participant.
 * 
 * @since Dec 7, 2011
 * @author Daojun Cui
 */
public class Participant {

	/**
	 * Name of the participant
	 */
	private String name;
	
	/**
	 * Activity Set
	 */
	private Set<Activity> activities;

	public Participant(){
		name = "";
		activities = new HashSet<Activity>();
	}
	
	public Participant(String name, Set<Activity> activities)
	{
		this.name = name;
		this.activities = activities;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}

	public Set<Activity> getActivities() {
		return this.activities;
	}
	
	public void setActivities(Set<Activity> activities){
		this.activities = activities;
	}
	

	public void add(Activity act) {
		this.activities.add(act);
	}

	public void remove(Activity act) {
		this.activities.remove(act);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("name:");
		sb.append(name);
		sb.append(" activity[");
		for(Activity act : this.activities){
			sb.append(act.getName()+",");
		}
		sb.replace(sb.length()-1, sb.length(), "");
		sb.append("]");
		return sb.toString(); 
	}

}
