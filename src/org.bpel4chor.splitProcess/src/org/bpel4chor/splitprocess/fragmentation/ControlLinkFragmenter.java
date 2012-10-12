package org.bpel4chor.splitprocess.fragmentation;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bpel4chor.model.grounding.impl.Grounding;
import org.bpel4chor.model.topology.impl.Topology;
import org.bpel4chor.splitprocess.RuntimeData;
import org.bpel4chor.splitprocess.exceptions.SplitControlLinkException;
import org.bpel4chor.splitprocess.partition.model.PartitionSpecification;
import org.bpel4chor.utils.ActivityIterator;
import org.bpel4chor.utils.MyBPELUtils;
import org.bpel4chor.utils.exceptions.AmbiguousPropertyForLinkException;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Source;
import org.eclipse.bpel.model.Target;
import org.eclipse.wst.wsdl.Definition;

/**
 * ControlLinkFragmenter split the control link.
 * 
 * <p>
 * It goes through all the participants and check the activity whether their
 * control links cross over the participants, if yes, then splits the explicit
 * control dependencies and create sending- and receiving block in the source-
 * and target process, if no, then add them into the fragment process regarding
 * the participant.
 * 
 * @since Feb 11, 2012
 * @author Daojun Cui
 */
public class ControlLinkFragmenter {

	/** The non-split process */
	protected Process nonSplitProcess = null;

	/** The WSDL definition of the non-split process */
	protected Definition nonSplitProcessDfn = null;

	/** The partition specification */
	protected PartitionSpecification partitionSpec = null;

	/** participant to the fragment process map */
	protected Map<String, Process> participant2FragProc = null;

	/** participant to wsdl map */
	protected Map<String, Definition> participant2WSDL = null;

	/** The BPEL4Chor Topology */
	protected Topology topology = null;

	/** The BPEL4Chor Grounding */
	protected Grounding grounding = null;

	protected Logger logger = Logger.getLogger(ControlLinkFragmenter.class);

	public ControlLinkFragmenter(RuntimeData data) {

		if (data == null)
			throw new NullPointerException("argument is null");

		this.nonSplitProcess = data.getNonSplitProcess();
		this.nonSplitProcessDfn = data.getNonSplitProcessDfn();
		this.partitionSpec = data.getPartitionSpec();
		this.participant2FragProc = data.getParticipant2FragProcMap();
		this.participant2WSDL = data.getParticipant2WSDLMap();
		this.topology = data.getTopology();
		this.grounding = data.getGrounding();
	}

	/**
	 * Split the control link if it crosses the participants, combine the source
	 * part and target part of the link if it does not crosses the participants.
	 * 
	 * <p>
	 * The iteration sequence is : Fragment process -> Activity -> Source ->
	 * Link.
	 * <p>
	 * Upon each link, it will be tested whether the link crosses the processes.
	 * For the record the link was created by only copying the name in the
	 * ProcessFragmentater, so, the original link has two copies, one find
	 * itself in the source of one activity, the other one resides in the target
	 * of another activity. The given link is from the source of an activity, we
	 * find out the activity at the link's target end, meanwhile, the
	 * targetFragmentProcess is also found. Then we compare the
	 * targetFragmentProcess with the sourceFragmentProcess. If they are NOT
	 * equal, then link crosses the processes. Then it is going to be split.
	 * 
	 * <p>
	 * If the link does not crosses the processes, it will be added into the
	 * current fragment process.
	 * 
	 * @throws SplitControlLinkException
	 *             if anything goes wrong in this part
	 */
	public void splitControlLink() throws SplitControlLinkException {
		try {

			for (Process sourceProcess : this.participant2FragProc.values()) {

				ActivityIterator actIterator = new ActivityIterator(
						sourceProcess);
				while (actIterator.hasNext()) {

					Activity currentAct = actIterator.next();

					// in this runtime, the fragment process could contain more
					// artifacts than the original process provided, so we must
					// leave the non-original artifacts out.
					if (existInOriginalProcess(currentAct)
							&& currentAct.getSources() != null) {

						List<Source> currentActSources = currentAct
								.getSources().getChildren();
						for (Source currentSource : currentActSources) {

							// link in source, where it is found.
							Link linkInSourceProcess = currentSource.getLink();

							Process targetProcess = getTargetFragmentProcess(linkInSourceProcess);
							boolean linkCrossProcesses = (targetProcess
									.equals(sourceProcess) ? false : true);
							if (linkCrossProcesses) {// then split the link

								Definition targetWSDL = participant2WSDL
										.get(targetProcess.getName());

								ControlLinkBlockBuilder blockBuilder = new ControlLinkBlockBuilder(
										targetProcess, sourceProcess,
										nonSplitProcess, targetWSDL,
										linkInSourceProcess, topology,
										grounding);

								// create prerequisite
								blockBuilder.createPrerequisites();

								// sending block will be built in source process
								blockBuilder.createSendingBlock();

								// receiving block will be built in target
								// process
								blockBuilder.createReceivingBlock();

							} else {
								// link does not cross processes
								combineUnsplitControlLink(sourceProcess,
										linkInSourceProcess);
							}
						}
					}
				}
			}

		} catch (Exception e) {
			throw new SplitControlLinkException(
					"Exception while trying to split control link.", e);
		}

	}

	/**
	 * Whether the activity given is in the the original process
	 * 
	 * @param currentAct
	 * @param sourceProcess
	 * @return
	 */
	private boolean existInOriginalProcess(Activity currentAct) {
		String actName = currentAct.getName();
		Activity originalAct = MyBPELUtils.resolveActivity(actName,
				nonSplitProcess);
		return (originalAct != null);
	}

	/**
	 * Add link into the fragment process.
	 * 
	 * @param sourceFragment
	 * @param link
	 * @throws AmbiguousPropertyForLinkException
	 */
	protected void combineUnsplitControlLink(Process sourceFragment, Link link)
			throws AmbiguousPropertyForLinkException {
		if (sourceFragment == null || link == null)
			throw new NullPointerException();

		Link linkInTarget = MyBPELUtils.findLinkInActivityTarget(
				link.getName(), sourceFragment);
		Target target = linkInTarget.getTargets().get(0);
		target.setLink(link);
	}

	/**
	 * Get fragment process, that the target of the link resides.
	 * 
	 * @param linkInSourceProcess
	 *            Control Link
	 * @return The target process that the link points to
	 */
	protected Process getTargetFragmentProcess(Link linkInSourceProcess) {

		if (linkInSourceProcess == null)
			throw new NullPointerException("argument is null.");
		if (linkInSourceProcess.getSources().size() != 1)
			throw new IllegalStateException("link sources size != 1,  size="
					+ linkInSourceProcess.getSources().size());

		for (Process fragmentProcess : participant2FragProc.values()) {
			try {
				String linkName = linkInSourceProcess.getName();
				Link linkInTarget = MyBPELUtils.findLinkInActivityTarget(
						linkName, fragmentProcess);
				if (linkInTarget != null) {
					// if the equivalent link find itself in current fragment
					// process, then the current fragment process is the
					// target fragment process.
					return fragmentProcess;
				}
			} catch (AmbiguousPropertyForLinkException e) {
				logger.warn("Ambiguous link: " + linkInSourceProcess.getName()
						+ " , found multiple links in process: "
						+ fragmentProcess.getName());
				return fragmentProcess;
			}
		}

		throw new RuntimeException(
				"can not find target fragment process with the given link:"
						+ linkInSourceProcess.getName());
	}

}
