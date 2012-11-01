package org.bpel4chor.splitprocess.partition.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.bpel4chor.splitprocess.exceptions.PartitionSpecificationException;
import org.bpel4chor.splitprocess.partition.model.Participant;
import org.bpel4chor.splitprocess.partition.model.PartitionSpecification;
import org.bpel4chor.splitprocess.utils.ActivityUtil;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.Reply;
import org.eclipse.wst.wsdl.Operation;

import de.uni_stuttgart.iaas.bpel.model.utilities.ActivityIterator;

/**
 * PartitionVerificator verifies whether the partition specification violates
 * any restriction.
 * 
 * <p>
 * <b>Restrictions</b>:
 * <ol>
 * <li>Only basic activity is assigned
 * <li>A participant must have at least one activity
 * <li>No two participants share a basic activity
 * <li>Participant name must be unique
 * <li>Each basic activity of the process is assigned to a participant
 * <li>Combined "receive" and "reply" activities must be assigned to the same
 * participant
 * </ol>
 * 
 * @since Dec 20, 2011
 * @author Daojun Cui
 */
public class PartitionVerificator {

	static Logger logger = Logger.getLogger(PartitionVerificator.class);

	/**
	 * Verify whether the partition specification complies to the restrictions.
	 * 
	 * @param partitionSpec
	 *            partition specification
	 * @param process
	 *            BPEL process
	 * @throws PartitionSpecificationException 
	 */
	public static void check(PartitionSpecification partitionSpec, Process process) throws PartitionSpecificationException {

		if (partitionSpec == null || process == null)
			throw new NullPointerException();

		if (partitionSpec.getParticipants().size() == 0)
			throw new IllegalArgumentException("no participant found");

		// 1. A participant must have at least one activity
		for (Participant p : partitionSpec.getParticipants()) {
			assertParticipantHaveAtLeastOneActivity(p);
		}

		// 2. Only basic activity is assigned
		for (Participant p : partitionSpec.getParticipants()) {
			assertOnlyBasicActivityIsAssigned(p);
		}

		// 3. No two participants share a basic activity
		List<Participant> participants = new ArrayList<Participant>();
		participants.addAll(partitionSpec.getParticipants());
		Participant p1 = participants.remove(0);
		while (!participants.isEmpty()) {
			for (Participant p2 : participants) {
				assertNoTwoParticipantsShareABasicActivity(p1, p2);
			}
			p1 = participants.remove(0);
		}

		// 4. Participant name must be unique
		assertParticipantNameIsUnique(partitionSpec.getParticipants());

		// 5. Each basic activity of the process is assigned to a participant
		ActivityIterator actIt = new ActivityIterator(process);
		Set<Participant> participantSet = partitionSpec.getParticipants();
		while (actIt.hasNext()) {
			Activity act = actIt.next();
			if (ActivityUtil.isBasicActivity(act))
				assertActivityIsAssignedToAParticipant(act, participantSet);
		}

		// 6. The combined <receive> and <reply> activity must be in the same
		// participant
		actIt.reset();
		while (actIt.hasNext()) {
			Activity act1 = actIt.next();
			if (act1 instanceof Receive) {

				// the operation of receive
				Operation op1 = ((Receive) act1).getOperation();

				ActivityIterator actIt2 = new ActivityIterator(process);

				while (actIt2.hasNext()) {
					Activity act2 = actIt2.next();

					if (act2 instanceof Reply) {

						Operation op2 = ((Reply) act2).getOperation();

						if (op1.equals(op2)) {
							assertCombinedActivitiesMustBeAssignedToSameParticipant(act1, act2,
									participantSet);
						}
					}
				}
			}
		}
	}

	private static void assertCombinedActivitiesMustBeAssignedToSameParticipant(Activity act1,
			Activity act2, Set<Participant> participantSet) throws PartitionSpecificationException {

		for (Participant p : participantSet) {
			Set<Activity> activities = p.getActivities();
			if (activities.contains(act1) && activities.contains(act2)) {
				return;
			}
		}

		throw new PartitionSpecificationException(
				"Combined <receive> and <reply> must be in the same participant. Activity "
						+ act1.getName() + " and " + act2.getName()
						+ " are not in the same participant.");

	}

	private static void assertActivityIsAssignedToAParticipant(Activity next,
			Set<Participant> participantSet) throws PartitionSpecificationException {
		for (Participant p : participantSet) {
			for (Activity act : p.getActivities()) {
				if (act.equals(next))
					return;
			}
		}
		throw new PartitionSpecificationException(
				"Every basic acitivity must be assigned to a participant. Activity "
						+ next.getName() + " is not assigned");
	}

	private static void assertNoTwoParticipantsShareABasicActivity(Participant p1, Participant p2) throws PartitionSpecificationException {
		for (Activity act : p1.getActivities()) {
			if (p2.getActivities().contains(act))
				throw new PartitionSpecificationException(
						"Two participants do NOT share a basic activity. Activity " + act.getName()
								+ " is assigned in two partiticipants.");
		}

	}

	private static void assertParticipantNameIsUnique(Set<Participant> participants) throws PartitionSpecificationException {

		Set<String> names = new HashSet<String>();
		for (Participant p : participants) {
			if (names.contains(p.getName()))
				throw new PartitionSpecificationException("Participant name must be unique. The name "
						+ p.getName() + " is duplicated.");
			else
				names.add(p.getName());
		}
	}

	private static void assertOnlyBasicActivityIsAssigned(Participant p) throws PartitionSpecificationException {
		for (Activity act : p.getActivities()) {
			if (ActivityUtil.isBasicActivity(act) == false)
				throw new PartitionSpecificationException(
						"Only basic Activity can be assigned. Activity " + act.getName()
								+ " is of type " + act.getClass().getName());
		}

	}

	private static void assertParticipantHaveAtLeastOneActivity(Participant p) throws PartitionSpecificationException {
		if (p.getActivities().size() < 1)
			throw new PartitionSpecificationException(
					"Every participant holds at least one activity. Size of participant " + p.getName() + " is "
							+ p.getActivities().size());

	}
}
