package org.bpel4chor.splitprocess.partition.model;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.bpel.model.Activity;

/**
 * Implementation of partition specification, it consists of a set of
 * participants.
 * 
 * @since Dec 3, 2011
 * @author Daojun Cui
 */
public class PartitionSpecification {

	/**
	 * Participant Set
	 */
	private Set<Participant> participants;

	public PartitionSpecification() {
		this.participants = new HashSet<Participant>();
	}

	public PartitionSpecification(Set<Participant> participants) {
		if (participants == null)
			throw new NullPointerException();
		this.participants = participants;
	}

	public Set<Participant> getParticipants() {
		return this.participants;
	}

	public void setParticipants(Set<Participant> participants) {
		if (participants == null)
			throw new NullPointerException();
		this.participants = participants;
	}

	/**
	 * Get participant with the name given
	 * 
	 * @param name
	 * @return
	 */
	public Participant getParticipant(String name) {
		for (Participant p : this.participants) {
			if (p.getName().equals(name))
				return p;
		}
		return null;
	}

	/**
	 * Get the participant where the activity given presents
	 * 
	 * @param memberOfParticipant
	 *            The activity that gets assigned in one participant
	 * @return The participant or null
	 */
	public Participant getParticipant(Activity memberOfParticipant) {
		if (memberOfParticipant == null)
			throw new NullPointerException();

		for (Participant p : this.participants) {
			for (Activity act : p.getActivities()) {
				if (act.equals(memberOfParticipant))
					return p;
			}
		}
		return null;
	}

	public void add(Participant participant) {
		if (participant == null)
			throw new NullPointerException();
		this.participants.add(participant);
	}

	public void remove(Participant participant) {
		this.participants.remove(participant);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("partitionSpecification: ");
		for (Participant p : this.participants) {
			sb.append(p.getName() + "={");
			for (Activity act : p.getActivities()) {
				sb.append(act.getName() + " ");
			}
			sb.replace(sb.length()-1, sb.length(), "");
			sb.append("} ");
		}
		return sb.toString();
	}
}
