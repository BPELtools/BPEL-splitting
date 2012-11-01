package org.bpel4chor.splitprocess.utils;

import java.util.ArrayList;
import java.util.List;

import org.bpel4chor.splitprocess.exceptions.LinkNotFoundException;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Target;
import org.eclipse.bpel.model.Targets;

import de.uni_stuttgart.iaas.bpel.model.utilities.ActivityIterator;
import de.uni_stuttgart.iaas.bpel.model.utilities.exceptions.AmbiguousPropertyForLinkException;

/**
 * LinkFinder looks up the link in the process with given criterion.
 * 
 * @since Feb 12, 2012
 * @author Daojun Cui
 * @deprecated this class's function is shifted to {@link MyBPELUtils}
 */
public class LinkFinder {

	protected Process process = null;

	public LinkFinder(Process process) {
		if (process == null)
			throw new NullPointerException("arguemt is null");
		this.process = process;
	}

	/**
	 * Look up the link that has the given name and resides in activity's
	 * target.
	 * 
	 * <p>
	 * This method embraces the principle that we find the perfect match link,
	 * or we throw error to the caller. Errors include
	 * {@link LinkNotFoundException} and
	 * {@link AmbiguousPropertyForLinkException}.
	 * 
	 * @param linkName
	 *            The name to find link with
	 * @return The found link
	 * @throws LinkNotFoundException
	 *             if no link is found
	 * @throws AmbiguousPropertyForLinkException
	 *             if multiple links that fit the description are found
	 */
	public Link findLinkInActivityTarget(String linkName) throws LinkNotFoundException,
			AmbiguousPropertyForLinkException {

		if (linkName == null)
			throw new NullPointerException("argument is null");
		if (linkName.isEmpty())
			throw new IllegalArgumentException("argument is empty");

		ActivityIterator actIterator = new ActivityIterator(this.process);
		List<Link> found = new ArrayList<Link>();

		// iterate through all the activities
		while (actIterator.hasNext()) {
			Activity activity = actIterator.next();
			Targets targets = activity.getTargets();
			if (targets != null) {
				for (Target target : targets.getChildren()) {
					Link linkInTarget = target.getLink();
					if (linkInTarget != null && linkInTarget.getName().equals(linkName)) {
						found.add(linkInTarget);
					}
				}
			}
		}

		// nothing found
		if (found.size() == 0) {
			throw new LinkNotFoundException("Can not find link any target of activity with the name : " + linkName);
		}
		// found too many
		if (found.size() > 1) {
			throw new AmbiguousPropertyForLinkException("Ambiguous link name:" + linkName
					+ ", multiple instances are found.");
		}

		// perfect match, single one
		return found.get(0);

	}

}
