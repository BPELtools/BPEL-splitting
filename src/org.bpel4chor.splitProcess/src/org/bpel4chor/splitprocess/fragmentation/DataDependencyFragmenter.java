package org.bpel4chor.splitprocess.fragmentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.bpel4chor.model.grounding.impl.Grounding;
import org.bpel4chor.model.topology.impl.Topology;
import org.bpel4chor.splitprocess.RuntimeData;
import org.bpel4chor.splitprocess.dataflowanalysis.AnalysisResultParser;
import org.bpel4chor.splitprocess.dataflowanalysis.DataFlowAnalyzer;
import org.bpel4chor.splitprocess.dataflowanalysis.QueryWriterSet;
import org.bpel4chor.splitprocess.exceptions.ActivityNotFoundException;
import org.bpel4chor.splitprocess.exceptions.SplitDataDependencyException;
import org.bpel4chor.splitprocess.partition.model.Participant;
import org.bpel4chor.splitprocess.partition.model.PartitionSpecification;
import org.bpel4chor.splitprocess.pwdg.model.PWDG;
import org.bpel4chor.splitprocess.pwdg.model.PWDGNode;
import org.bpel4chor.splitprocess.pwdg.model.WDG;
import org.bpel4chor.splitprocess.pwdg.util.PWDGFactory;
import org.bpel4chor.splitprocess.pwdg.util.WDGFactory;
import org.bpel4chor.splitprocess.utils.ActivityFinder;
import org.bpel4chor.splitprocess.utils.NameGenerator;
import org.bpel4chor.splitprocess.utils.VariableResolver;
import org.bpel4chor.utils.BPEL4ChorFactory;
import org.bpel4chor.utils.BPEL4ChorModelConstants;
import org.bpel4chor.utils.BPEL4ChorUtil;
import org.bpel4chor.utils.RandomIdGenerator;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.Catch;
import org.eclipse.bpel.model.Condition;
import org.eclipse.bpel.model.Copy;
import org.eclipse.bpel.model.Correlation;
import org.eclipse.bpel.model.CorrelationSet;
import org.eclipse.bpel.model.Correlations;
import org.eclipse.bpel.model.Expression;
import org.eclipse.bpel.model.FaultHandler;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.From;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Links;
import org.eclipse.bpel.model.PartnerLink;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Query;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.Source;
import org.eclipse.bpel.model.Targets;
import org.eclipse.bpel.model.To;
import org.eclipse.bpel.model.Variable;
import org.eclipse.bpel.model.While;
import org.eclipse.bpel.model.util.BPELConstants;
import org.eclipse.wst.wsdl.Definition;
import org.eclipse.wst.wsdl.Message;
import org.eclipse.wst.wsdl.Operation;
import org.eclipse.wst.wsdl.Part;
import org.eclipse.wst.wsdl.PortType;
import org.jgrapht.graph.DefaultEdge;

import de.uni_stuttgart.iaas.bpel.model.utilities.ActivityIterator;
import de.uni_stuttgart.iaas.bpel.model.utilities.MyBPELUtils;
import de.uni_stuttgart.iaas.bpel.model.utilities.MyWSDLUtil;
import de.uni_stuttgart.iaas.bpel_d.algorithm.analysis.AnalysisResult;

/**
 * DataDependencyFragmenter fragments the data dependency between fragment
 * processes.
 * <p>
 * The data dependency will be detected by data flow analysis. The results will
 * serve as input of the PWDGConstructor, which generates a PWDG graph for
 * creating LocalResolver and Receiving Flow.
 * 
 * 
 * @since Feb 24, 2012
 * @author Daojun Cui
 */
public class DataDependencyFragmenter {

	/** Data Flow Analysis of BPEL process */
	protected AnalysisResult analysisRes = null;

	/** The non-split BPEL process */
	protected Process nonSplitProcess = null;

	/** The Partition Specification */
	protected PartitionSpecification partitionSpec = null;

	/** Participant to fragment process map */
	protected Map<String, Process> participant2FragProc = null;

	/** Participant to fragment WSDL map */
	protected Map<String, Definition> participant2WSDL = null;

	/** Map associating PWDGNode to unique name */
	protected Map<PWDGNode, String> node2NameMap = null;

	/** Map associating Query Set to unique name */
	protected Map<Set<String>, String> querySet2NameMap = null;

	/** The Topology */
	protected Topology topology = null;

	/** The Grounding */
	protected Grounding grounding = null;

	public DataDependencyFragmenter(RuntimeData data) {

		if (data == null)
			throw new NullPointerException();

		this.nonSplitProcess = data.getNonSplitProcess();
		this.partitionSpec = data.getPartitionSpec();
		this.participant2FragProc = data.getParticipant2FragProcMap();
		this.participant2WSDL = data.getParticipant2WSDLMap();
		this.node2NameMap = new HashMap<PWDGNode, String>();
		this.querySet2NameMap = new HashMap<Set<String>, String>();
		this.topology = data.getTopology();
		this.grounding = data.getGrounding();

	}

	/**
	 * Split data dependency
	 * <p>
	 * We iterate though all pairs of (a, x) - a for activity, x for variable -
	 * in the non-split process. In each iteration we do following things:
	 * <ol>
	 * <li>create Q_s(a, x) from data flow analysis result
	 * <li>create WDG(a,x) from Q_s(a,x)
	 * <li>create PWDG(a,x) from WDG(a,x)
	 * <li>create prerequisites for LR and RF
	 * <li>for each PWDG node n do CREATE-LOCAL-RESOLVER
	 * <li>CREATE-RECEIVING-FLOW in a's process
	 * </ol>
	 * 
	 * @throws SplitDataDependencyException
	 */
	public void splitDataDependency() throws SplitDataDependencyException {
		try {
			// analyze data flow of the process
			analysisRes = DataFlowAnalyzer.analyze(this.nonSplitProcess);

			// collect all basic activities from the participants
			List<Activity> activities = new ArrayList<Activity>();
			for (Participant p : this.partitionSpec.getParticipants()) {
				activities.addAll(p.getActivities());
			}
			sortActivities(activities);

			//
			// create local resolver and receiving flow
			//

			// iterate based on each activity and variable pair
			Activity act = null;
			Variable var = null;
			for (int i = 0; i < activities.size(); i++) {

				act = activities.get(i);

				// get the read variables and sort them.
				VariableResolver resolver = new VariableResolver(nonSplitProcess);
				List<Variable> variables = resolver.resolveReadVariable(act);
				sortVariables(variables);

				for (int j = 0; j < variables.size(); j++) {

					var = variables.get(j);
					QueryWriterSet qwSet = AnalysisResultParser.parse(act, var, this.analysisRes);
					WDG wdg = WDGFactory.createWDG(qwSet.getAllWriters());
					PWDG pwdg = PWDGFactory.createPWDG(wdg, nonSplitProcess, partitionSpec);

					if (isSingleWriterGraph(pwdg) && isReaderAndPWDGInSamePartition(pwdg, act))
						continue;

					// initialize the id maps for the given Q_s(act, var)
					initQuerySet2nameMap(var.getName(), qwSet.querySets());
					initNode2NameMap(pwdg);

					// create prerequisite: message, portType, partnerLinkType,
					// partnerLink
					DataDependencyHelper helper = new DataDependencyHelper(participant2FragProc,
							participant2WSDL, partitionSpec, act, var);
					helper.createPrerequisites(pwdg, qwSet, querySet2NameMap, node2NameMap);

					// local resolver
					for (PWDGNode node : pwdg.vertexSet()) {

						// get the Q_s(n, a, x)
						QueryWriterSet qwSet4Node = qwSet.getQueryWriterSetFor(node);

						// due to the tuples of query set to writer set being
						// filtered by the pwdg node given, some query sets
						// might be merged together because of the same writer
						// set, then some not-in-id-map-registered query set
						// will be generated, so the queryset2nameMap must be
						// updated now. More for merging of the query set see
						// also the page 50 in Diplomarbeit Nr. 3255 -
						// "Splitting BPEL processes".
						// updateQuerySet2NameMap(qwSet4Node);

						createLocalResolver(node, act, var, qwSet4Node);
					}

					// receiving flow
					createReceivingFlow(act, var, pwdg, qwSet);
				}
			}

			// last treatment for correlation set, the initial <receive>
			// activity in the main process must be connected to an <assign>
			// activity, which copy the correlation value to the global
			// variable, then both of the <receive> and the <assign> must be
			// wrapped into one <sequence> activity. The links of the
			// <receive> activity will be rerouted to the <sequence> activity.
			wrapInitialReceive();

		} catch (Exception e) {
			throw new SplitDataDependencyException(e);
		}

	}

	/**
	 * Test whether only one writer is in a node of a PWDG
	 * 
	 * @param pwdg
	 * @return
	 */
	private boolean isSingleWriterGraph(PWDG pwdg) {
		if (pwdg.vertexSet().size() == 1) {
			PWDGNode node = pwdg.vertexSet().iterator().next();
			if (node.getActivities().size() == 1)
				return true;
		}
		return false;
	}

	/**
	 * Test whether the reader and the single-writer-pwdg is in a same partition
	 * 
	 * @param singleWriterPWDG
	 * @param act
	 * @return
	 */
	private boolean isReaderAndPWDGInSamePartition(PWDG singleWriterPWDG, Activity act) {
		if (singleWriterPWDG == null || act == null)
			throw new NullPointerException();

		if (isSingleWriterGraph(singleWriterPWDG) == false)
			throw new IllegalArgumentException();

		PWDGNode node = singleWriterPWDG.vertexSet().iterator().next();
		Activity writer = node.getActivities().iterator().next();

		Participant writerPartition = partitionSpec.getParticipant(writer);
		Participant readerPartition = partitionSpec.getParticipant(act);
		boolean isSamepartition = readerPartition.equals(writerPartition);
		return isSamepartition;
	}

	/**
	 * Create Local Resolver in the process where the given pwdg node presents
	 * 
	 * <p>
	 * The Local Resolver handles 4 cases:
	 * <ol>
	 * <li>Query Set = 1, Node Participant == Reader Participant.<br>
	 * In this case, only empty activity and a new variable are created.
	 * Variable is inserted into process, and the activity is inserted into the
	 * top level flow activity of the node process.
	 * 
	 * <li>Query Set = 1, Node Participant != Reader Participant.<br>
	 * In This case, the sending block is created and inserted into the top
	 * level flow activity of the node process. Additionally, there are two
	 * variables (one for sending true and data, the other for sending false)
	 * created and inserted into the invoking process.
	 * 
	 * <li>Query Set > 1, Node Participant == Reader Participant<br>
	 * In this case, a new variable to collect the write statuses is created. A
	 * assign scope is created for each query set. And an empty activity is
	 * created and all the assign scopes are connected to this empty activity.
	 * 
	 * <li>Query Set > 1, Node Participant != Reader Participant<br>
	 * In this case, a new variable to collect the write statuses and the data
	 * is created. A assign scope is created for each query set. And a sequence
	 * activity, which contains an assign activity and an invoke activity, is
	 * created and all the assign scopes are connected to this sequence
	 * activity.
	 * </ol>
	 * 
	 * It helps to understand this procedure by recognizing which case the
	 * pre-given conditions are suit to.
	 * 
	 * @param node
	 *            PWDG Node
	 * @param act
	 *            Reader Activity
	 * @param var
	 *            Variable the reader depends on
	 * @param qwSet4Node
	 *            The QueryWriterSet that represents Q_s(n, a, x), n for pwdg
	 *            node, a for activity, x for variable
	 * @throws SplitDataDependencyException
	 * 
	 * @see The procedure CREATE-LOCAL-RESOLVER-MULTIPLE-WRITERS from Khalaf,
	 *      R.; Kopp, O. & Leymann, F. Maintaining Data Dependencies Across BPEL
	 *      Process Fragments International Journal of Cooperative Information
	 *      Systems (IJCIS), 2008, 17, 259-282
	 * @see The details about the concepts implemented in this procedure see
	 *      also section 5.8.10 in the development manual.
	 */
	protected void createLocalResolver(PWDGNode node, Activity act, Variable var,
			QueryWriterSet qwSet4Node) throws SplitDataDependencyException {

		if (node == null || act == null || var == null || qwSet4Node == null)
			throw new NullPointerException();

		// the QueryWriterSet filtered by pwdg node, Q_s(n, a, x)
		// QueryWriterSet nodeQWS = qsAX.getQueryWriterSetFor(node);

		if (qwSet4Node.isEmpty()) // nothing to do
			return;

		Activity bAct = null;
		Variable inputVar = null;
		String tmpName = null;

		String participantOfNode = node.getParticipant();
		String participantOfReader = participantOf(act).getName();

		Process nodeProcess = participant2FragProc.get(participantOfNode);
		Process readerProcess = participant2FragProc.get(participantOfReader);

		boolean isSamePartition = participantOfNode.equals(participantOfReader);

		// begin
		if (qwSet4Node.size() == 1) {

			if (isSamePartition) {// single tuple, same partition

				// same partition, only create an empty activity and an
				// tmp-variable
				bAct = BPELFactory.eINSTANCE.createEmpty();
				inputVar = BPELFactory.eINSTANCE.createVariable();

				// activity name
				bAct.setName(id(node) + "Empty");

				// inputVar name using idn(node)
				inputVar.setName(id(node));
				tmpName = inputVar.getName();

				// add the variable and activity into process
				// nodeProcess.getVariables().getChildren().add(inputVar);
				Flow flow = (Flow) nodeProcess.getActivity();
				flow.getActivities().add(bAct);

			} else { // single tuple, different partitions

				// create sending block
				bAct = createSendingBlock(nodeProcess, readerProcess, var, node);

				// add the sending block activity into process
				// Note that: the corresponding variables already are added into
				// the invoking process (in the createSendingBlock)
				Flow flow = (Flow) nodeProcess.getActivity();
				flow.getActivities().add(bAct);

			}

			// either the node and the reader are in the same participant or
			// not, create the links that combine the writers of the query set
			// and the 'b' (the empty activity or sending block).
			Set<String> querySet = qwSet4Node.querySets().iterator().next();
			for (Activity writer : qwSet4Node.get(querySet)) {

				// recall that the writer in the query writer set is in the main
				// process, we need the corresponding one in the fragment
				// process.
				Activity writerInFragProc = MyBPELUtils.resolveActivity(writer.getName(),
						nodeProcess);
				if (writerInFragProc == null)
					throw new NullPointerException();

				if (writerInFragProc instanceof While) {
					throw new RuntimeException("Implementation for <while> is not implemented yet.");
				} else {
					// new link (writer,'bAct', true())
					boolean transitionCondition = true;
					Link link = FragmentFactory.createLinkBetween(writerInFragProc, bAct,
							transitionCondition);

					// add link to flow
					Flow flow = (Flow) nodeProcess.getActivity();
					flow.getLinks().getChildren().add(link);
				}
			}

		} else if (qwSet4Node.size() > 1) { // more than one query sets

			// variable must be set with the messageType that contains more than
			// one status-parts
			inputVar = BPELFactory.eINSTANCE.createVariable();

			// The message name for multiple query sets agrees with the naming
			// convention
			String msg4MQName = nodeProcess.getName() + readerProcess.getName() + var.getName()
					+ id(node) + "MQMessage";

			// retrieve the message from the wsdl definition
			Message msg4MQ = getMQMessage(msg4MQName, readerProcess);

			// setup the variable messageType and name
			inputVar.setMessageType(msg4MQ);

			// inputVar name using idn(node)
			inputVar.setName(id(node));
			tmpName = inputVar.getName();

			// add the variable into the invoking (node) process
			nodeProcess.getVariables().getChildren().add(inputVar);

			if (isSamePartition) {// more than one query set and same partition

				// create only an empty activity and empty variable
				bAct = BPELFactory.eINSTANCE.createEmpty();

				// set name = idn(n) + 'Empty'
				bAct.setName(id(node) + "Empty");

				// add the activity 'b' into node process
				Flow flow = (Flow) nodeProcess.getActivity();
				flow.getActivities().add(bAct);

			} else { // more than one query set, different partitions

				// Create a sequence that contains an assign activity and an
				// invoke activity. The assign activity initializes the data
				// part of the inputVariable for the invoke activity. The invoke
				// activity sends the data and the status of 'x'.

				// Assigning the variable 'x' namely the 'var' to the data part
				// of the input variable is done by the newly created assign
				// activity, additionally the sequence activity is created to
				// accommodate the assign and the invoke activity.

				// NOTE: there are two variable 'x', one is in the main process,
				// one is in the local fragment process. The variable 'x', which
				// is assigned to the data part of the inputVariable, must
				// be the one in the local fragment process, not the one in the
				// main process.
				bAct = createSequence4MQAndDiffPartition(nodeProcess, readerProcess, var, inputVar,
						node);

				// add the activity 'b' into node process
				Flow flow = (Flow) nodeProcess.getActivity();
				flow.getActivities().add((Sequence) bAct);

			}

			// either the node and the reader are in the same participant or
			// not, for each tuple (query set: writer set) create assign scope
			// for setting the status of the query set.
			for (Set<String> querySet : qwSet4Node.querySets()) {

				// create the scope for assigning status parts
				Set<Activity> writerSet = qwSet4Node.get(querySet);
				Scope assignScope = createAssignScope(nodeProcess, var.getName(),
						inputVar.getName(), node, querySet, writerSet);

				// add the scope 's' into node process
				Flow flow = (Flow) nodeProcess.getActivity();
				flow.getActivities().add(assignScope);

				// add link 'l' = ( s, b, true())
				boolean transitionCondition = true;
				Link linkScope2Sequence = FragmentFactory.createLinkBetween(assignScope, bAct,
						transitionCondition);

				// add the link into process
				flow.getLinks().getChildren().add(linkScope2Sequence);
			}
		}
	}

	/**
	 * Get the Message Name that agrees with the naming convention for single
	 * Query Set in Q_s(a,x)
	 * 
	 * @param invokingProcessName
	 * @param invokedProcessName
	 * @param varName
	 * @return
	 * @see naming convention for message see also in Development Manual Chapter
	 *      5.8.7
	 */
	private String getSQMessageName(String invokingProcessName, String invokedProcessName,
			String varName) {

		if (invokedProcessName == null || invokedProcessName == null || varName == null)
			throw new NullPointerException();

		StringBuffer sb = new StringBuffer();
		sb.append(invokingProcessName);
		sb.append(invokedProcessName);
		sb.append(varName);
		sb.append("SQMessage");
		return sb.toString();
	}

	/**
	 * Get the Message Name that agrees with the naming convention for Multiple
	 * Query Set in Q_s(a,x)
	 * 
	 * @param invokingProcessName
	 * @param invokedProcessName
	 * @param var
	 * @return
	 * @see naming convention for message see also Section 5.8.7 in Development
	 *      Manual
	 */
	private String getMQMessageName(String invokingProcessName, String invokedProcessName,
			String var, PWDGNode node) {

		if (invokingProcessName == null || invokedProcessName == null || var == null
				|| node == null)
			throw new NullPointerException();

		StringBuffer sb = new StringBuffer();
		sb.append(invokingProcessName);
		sb.append(invokedProcessName);
		sb.append(var);
		sb.append(id(node));
		sb.append("MQMessage");
		return sb.toString();
	}

	/**
	 * Get the message that contains multiple status-parts
	 * 
	 * @param messageName
	 * @param process
	 * @return
	 * @throws SplitDataDependencyException
	 */
	private Message getMQMessage(String messageName, Process process)
			throws SplitDataDependencyException {
		Definition defn = participant2WSDL.get(process.getName());
		QName msgQName = new QName(defn.getTargetNamespace(), messageName);
		Message message = MyWSDLUtil.resolveMessage(defn, msgQName);
		if (message == null)
			throw new SplitDataDependencyException("Message: " + messageName
					+ " does not exist in Definition of " + process.getName());
		return message;
	}

	/**
	 * Create sending block for local resolver
	 * <p>
	 * Artifacts to create:
	 * <ol>
	 * <li>A new Scope
	 * <li>A new Variable for sending true, a new Variable for sending false
	 * <li>A new 'Sequence' for sending true, it contains an 'Assign' activity
	 * and an 'Invoke' activity.
	 * <li>A new 'Sequence' for sending false, it contains an 'Assign' activity
	 * and an 'Invoke' activity.
	 * </ol>
	 * 
	 * Configurations needed:
	 * <ol>
	 * <li>Setup the messageType of the variables
	 * <ul>
	 * <li>For the message sending true and data, the data part is set to the
	 * variable in local process that has the same name of the x.
	 * <li>For the message sending false, only the status part is needed to be
	 * set.
	 * </ul>
	 * <li>Setup the operation, portType, partnerLink of the invoke activities.
	 * <li>Set 'suppressJoinFailure=false' in the 'Invoke' activity sending true
	 * </ol>
	 * 
	 * @param nodeProcess
	 *            The process that the node presents
	 * @param readerProcess
	 *            The process that the reader activity presents
	 * @param var
	 *            The variable that contains the data
	 * @param node
	 *            The PWDG node
	 * @return A scope for sending true and the data
	 * @throws SplitDataDependencyException
	 */
	protected Scope createSendingBlock(Process nodeProcess, Process readerProcess, Variable var,
			PWDGNode node) throws SplitDataDependencyException {

		if (nodeProcess == null || readerProcess == null || var == null || node == null)
			throw new NullPointerException();

		// invoke for sending true and data, that sets the suppressJoinFailure
		// as 'false', the inputVariable will be created and added into the
		// invoking process.
		Invoke invoke4TrueAndData = createInvokeSendTrueAndData4SendingBlock(nodeProcess,
				readerProcess, node, var);

		// assign for assigning the inputVariable that contains true and data x
		Assign assign4TrueAndData = createAssignVarTrueAndData4SendingBlock(invoke4TrueAndData,
				var, node, nodeProcess);

		// sequence that accommodate the invoke and assign activities
		Sequence sequence4TrueAndData = createSequenceTrueAndData4SendingBlock(invoke4TrueAndData,
				assign4TrueAndData, var, node);

		// invoke for sending false, the inputVariable is created and added into
		// the invoking process.
		Invoke invoke4False = createInvokeSendFalse4SendingBlock(nodeProcess, readerProcess, node,
				var);

		// assign for assigning the variable that contains the false
		Assign assign4False = createAssignVarFalse4SendingBlock(invoke4False, var, node,
				nodeProcess);

		// sequence that holds the invoke and assign for FaultHandler
		Sequence sequenceFh = createSequenceFH4SendingBlock(invoke4False, assign4False, var, node);
		sequenceFh.getActivities().add(assign4False);
		sequenceFh.getActivities().add(invoke4False);

		// catch for fault
		Catch fhCatch = BPELFactory.eINSTANCE.createCatch();

		fhCatch.setFaultName(new QName(nonSplitProcess.getTargetNamespace(), "joinFailure",
				BPELConstants.PREFIX));
		fhCatch.setActivity(sequenceFh);

		// faultHandler
		FaultHandler fh = BPELFactory.eINSTANCE.createFaultHandler();
		fh.getCatch().add(fhCatch);

		// scope
		String scopeName = "LR" + var.getName() + id(node) + "Scope";
		Scope scope = BPELFactory.eINSTANCE.createScope();
		scope.setName(scopeName);
		scope.setActivity(sequence4TrueAndData);
		scope.setFaultHandlers(fh);

		return scope;
	}

	/**
	 * Create a scope and a assign as the scope's child to initialise the status
	 * true, and another assign as the scope's faultHandler to initialise the
	 * status false.
	 * <p>
	 * Note: The status part, which the assign activities will set 'true' or
	 * 'false' to, has the following naming convention: 'status' + id(querySet).
	 * <p>
	 * The naming convention for message that has more than one status parts is
	 * defined in section 5.8.7 Message Specification for Invoke in LR and
	 * Receive in RF (Development Manual).
	 * <p>
	 * See also
	 * {@link DataDependencyHelper#createPrerequisiteMQMessage(Participant, Participant, QueryWriterSet, Map, String)}
	 * for details of the Message.
	 * <p>
	 * See also procedure CREATE-ASSIGN-SCOPE in Khalaf, R.; Kopp, O. & Leymann,
	 * F. Maintaining Data Dependencies Across BPEL Process Fragments
	 * International Journal of Cooperative Information Systems (IJCIS), 2008,
	 * 17, 259-282
	 * <p>
	 * Note: There is an error in the function CREATE-ASSIGN-SCOPE's input
	 * parameter. The second input parameter should be the tuple of query set
	 * and writer set, instead of only the query set.
	 * 
	 * @param invokingProcess
	 *            The node process in which the assignScope is added.
	 * @param varName
	 *            The name of the variable that is being handled
	 * @param inputVarName
	 *            The name of the inputVariable for sending status and data.
	 * @param node
	 *            The PWDG node
	 * @param querySet
	 *            The Query Set of the current tuple in Q_s(n, a, x)
	 * @param writerSet
	 *            The Writer Set of the current tuple in Q_s(n, a, x)
	 * @return
	 */
	protected Scope createAssignScope(Process invokingProcess, String varName, String inputVarName,
			PWDGNode node, Set<String> querySet, Set<Activity> writerSet) {

		if (invokingProcess == null || inputVarName == null || querySet == null
				|| writerSet == null)
			throw new NullPointerException();

		// the scope for assign status part
		Scope scope4Assign = BPELFactory.eINSTANCE.createScope();
		String scopeName = "AssignScope" + varName + querySet2NameMap.get(querySet);
		scope4Assign.setName(scopeName);

		// assign 'false' to the status part, in the faultHandler
		Assign assign4False = BPELFactory.eINSTANCE.createAssign();
		String assign4FalseName = "Assign" + varName + querySet2NameMap.get(querySet) + "FH";
		assign4False.setName(assign4FalseName);

		// create copy - from - to for the case of 'false'
		Copy copy4False = BPELFactory.eINSTANCE.createCopy();
		From from4False = BPELFactory.eINSTANCE.createFrom();
		To to4False = BPELFactory.eINSTANCE.createTo();

		// from spec - false()
		Expression exprFalse = BPELFactory.eINSTANCE.createExpression();
		exprFalse.setBody("false()");
		from4False.setExpression(exprFalse);

		// to spec - inputVariable.status+id(querySet)
		Variable inputVar = MyBPELUtils.resolveVariable(inputVarName, invokingProcess);
		if (inputVar == null)
			throw new NullPointerException();
		to4False.setVariable(inputVar);
		String statusPartName = "status" + querySet2NameMap.get(querySet);
		Part statusPart = (Part) inputVar.getMessageType().getPart(statusPartName);
		to4False.setPart(statusPart);

		copy4False.setFrom(from4False);
		copy4False.setTo(to4False);

		// setup the assign for 'false'
		assign4False.getCopy().add(copy4False);

		// create fault catcher that contains the assign for false
		Catch fhCatch = BPELFactory.eINSTANCE.createCatch();
		fhCatch.setFaultName(new QName(nonSplitProcess.getTargetNamespace(), "joinFailure",
				BPELConstants.PREFIX));
		fhCatch.setActivity(assign4False);

		// faultHandler
		FaultHandler fh = BPELFactory.eINSTANCE.createFaultHandler();
		fh.getCatch().add(fhCatch);

		// add the faultHandler to scope
		scope4Assign.setFaultHandlers(fh);

		// assign 'true' to status part
		Assign assign4True = BPELFactory.eINSTANCE.createAssign();
		String assign4TrueName = "Assign" + varName + querySet2NameMap.get(querySet);
		assign4True.setName(assign4TrueName);

		// create copy - from - to for the case of 'true'
		Copy copy4True = BPELFactory.eINSTANCE.createCopy();
		From from4True = BPELFactory.eINSTANCE.createFrom();
		To to4True = BPELFactory.eINSTANCE.createTo();

		// from spec - true()
		Expression exprTrue = BPELFactory.eINSTANCE.createExpression();
		exprTrue.setBody("true()");
		from4True.setExpression(exprTrue);

		// to spec - inputVariable.status+id(querySet)
		to4True.setVariable(inputVar);
		to4True.setPart(statusPart);

		copy4True.setFrom(from4True);
		copy4True.setTo(to4True);

		// setup the assign for 'true'
		assign4True.getCopy().add(copy4True);

		// set the activity of the scope as the assign for true
		scope4Assign.setActivity(assign4True);

		return scope4Assign;
	}

	/**
	 * Create the Invoke for true and data and set the
	 * 'suppressJoinFailure=false', the inputVariable is also created and added
	 * into invoking process. Besides the information for BPEL4Chor topology and
	 * grounding is also collected.
	 * <p>
	 * Naming convention for invoke-activity: Name= 'Send' + varName +
	 * id_n(node)
	 * 
	 * @param invokingProcess
	 * @param invokedProcess
	 * @param node
	 *            The PWDG node
	 * @param dataVar
	 *            The variable that contains the data
	 * @return
	 * @throws SplitDataDependencyException
	 * @see section 5.8.6, 5.8.7, 5.8.10, 5.8.11 in Development manual for
	 *      naming convention
	 */
	private Invoke createInvokeSendTrueAndData4SendingBlock(Process invokingProcess,
			Process invokedProcess, PWDGNode node, Variable dataVar)
			throws SplitDataDependencyException {

		if (invokingProcess == null || invokedProcess == null || node == null || dataVar == null)
			throw new NullPointerException();

		Definition invokedDefn = participant2WSDL.get(invokedProcess.getName());

		// invoke name agrees with the naming convention
		String invokeName = "Send" + dataVar.getName() + id(node);

		// partnerLink name agrees with the naming convention
		String partnerLinkName = invokingProcess.getName() + invokedProcess.getName() + "PL";
		PartnerLink pl = MyBPELUtils.getPartnerLink(invokingProcess, partnerLinkName);
		if (pl == null)
			throw new SplitDataDependencyException("partnerlink: " + pl + " does not exist.");

		// portType name agrees with the naming convention
		String portTypeName = invokingProcess.getName() + invokedProcess.getName() + "PT";
		PortType pt = MyWSDLUtil.findPortType(invokedDefn, portTypeName);
		if (pt == null)
			throw new SplitDataDependencyException("portType: " + portTypeName + " does not exist.");

		// operation name agrees with the naming convention
		String operationName = invokingProcess.getName() + invokedProcess.getName()
				+ dataVar.getName() + id(node) + "OP";
		Operation op = MyWSDLUtil.findOperation(pt, operationName);
		if (op == null)
			throw new SplitDataDependencyException("operation: " + operationName
					+ " does not exist in portType: " + portTypeName);

		// inputVariable Name agrees with the naming convention
		String inputVarName = id(node);

		// messsageType of the inputVariable
		String msgName4SQ = getSQMessageName(invokingProcess.getName(), invokedProcess.getName(),
				dataVar.getName());
		QName msgQName = new QName(invokedDefn.getTargetNamespace(), msgName4SQ);

		// message for single query set, containing status part and data part
		Message msg4SQ = MyWSDLUtil.resolveMessage(invokedDefn, msgQName);
		if (msg4SQ == null)
			throw new SplitDataDependencyException("mesage:" + msgName4SQ + " does not exist.");

		// create inputVariable for true and data, then set the messageType
		Variable inputVar4StatusAndData = BPELFactory.eINSTANCE.createVariable();
		inputVar4StatusAndData.setName(inputVarName);
		inputVar4StatusAndData.setMessageType(msg4SQ);

		// add inputVariable into the invoking process
		invokingProcess.getVariables().getChildren().add(inputVar4StatusAndData);

		// create invoke activity
		Invoke invoke = BPELFactory.eINSTANCE.createInvoke();
		invoke.setName(invokeName);
		invoke.setPartnerLink(pl);
		invoke.setPortType(pt);
		invoke.setOperation(op);
		invoke.setInputVariable(inputVar4StatusAndData);
		invoke.setSuppressJoinFailure(false);

		// create a topology message link for the invoke activity
		String msgName = msg4SQ.getQName().getLocalPart();
		NameGenerator nameGen = new NameGenerator(topology);

		org.bpel4chor.model.topology.impl.MessageLink topologyMsgLink = BPEL4ChorFactory
				.createTopologyMessageLink();
		String topoMsgLinkName = nameGen.getUniqueTopoMsgLinkName(msgName + "Link");
		String senderName = invokingProcess.getName();
		String sendActName = invoke.getName();

		topologyMsgLink.setName(topoMsgLinkName);
		topologyMsgLink.setSender(senderName);
		topologyMsgLink.setSendActivity(sendActName);
		topologyMsgLink.setMessageName(msgName);

		this.topology.add(topologyMsgLink);

		// create the corresponding grounding message link
		org.bpel4chor.model.grounding.impl.MessageLink groundingMsgLink = BPEL4ChorFactory
				.createGroundingMessageLink(topologyMsgLink, pt, op);
		this.grounding.add(groundingMsgLink);

		return invoke;

	}

	/**
	 * Create the assign activity to initialize the inputVariable
	 * <p>
	 * The copy operations include (1) copy 'true' to the status-part of the
	 * inputVariable, (2) copy the global variable for correlation to the
	 * correlation-part of the inputVariable, and (3) copy the variable
	 * currently being read to the data-part of the inputVariable.
	 * 
	 * @param invoke4TrueAndData
	 * @param dataVar
	 * @param node
	 * @param invokingProcess
	 * @return
	 */
	private Assign createAssignVarTrueAndData4SendingBlock(Invoke invoke4TrueAndData,
			Variable dataVar, PWDGNode node, Process invokingProcess) {

		// inputVariable
		Variable inputVar = invoke4TrueAndData.getInputVariable();

		// assign
		Assign assign = BPELFactory.eINSTANCE.createAssign();
		String assignName = "Assign" + dataVar.getName() + id(node);
		assign.setName(assignName);

		// copy the status into the status part in message of the inputVariable
		boolean status = true;
		Copy copy4Status = FragmentFactory.createCopy4Status(inputVar, status);
		assign.getCopy().add(copy4Status);

		// copy the global variable for correlation to the correlation part
		// in the message of inputVariable
		Copy copy4Correl = FragmentFactory.createCopy4Correlatoin(invokingProcess, inputVar);
		assign.getCopy().add(copy4Correl);

		// copy the data that is currently being read to the message part 'data'
		// of the inputVariable for invoke
		Copy copy4Data = BPELFactory.eINSTANCE.createCopy();
		From fromData = BPELFactory.eINSTANCE.createFrom();
		To toData = BPELFactory.eINSTANCE.createTo();

		Part dataPart = (Part) inputVar.getMessageType().getPart("data");

		// from the variable in the local fragment process
		Variable varInFragmentProcess = MyBPELUtils.resolveVariable(dataVar.getName(),
				invokingProcess);
		if (varInFragmentProcess == null)
			throw new NullPointerException();
		fromData.setVariable(dataVar);

		// to the data part of the input variable
		toData.setVariable(inputVar);
		toData.setPart(dataPart);

		copy4Data.setFrom(fromData);
		copy4Data.setTo(toData);

		assign.getCopy().add(copy4Data);

		return assign;
	}

	/**
	 * Create the sequence activity to accommodate the invoke and assign
	 * activities
	 * 
	 * @param invoke4TrueAndData
	 * @param assign4TrueAndData
	 * @param dataVar
	 * @param node
	 * @return
	 */
	private Sequence createSequenceTrueAndData4SendingBlock(Invoke invoke4TrueAndData,
			Assign assign4TrueAndData, Variable dataVar, PWDGNode node) {

		Sequence sequence = BPELFactory.eINSTANCE.createSequence();
		String sequenceName = "LR" + dataVar.getName() + id(node);
		sequence.setName(sequenceName);
		sequence.getActivities().add(assign4TrueAndData);
		sequence.getActivities().add(invoke4TrueAndData);
		return sequence;

	}

	/**
	 * Create the invoke activity for sending 'false', and collect the BPEL4Chor
	 * information.
	 * <p>
	 * The difference between activities sending 'false' and 'true' is that the
	 * activity for sending 'false' only needs to send the status-part 'false',
	 * the data-part is not needed.
	 * 
	 * @param invokingProcess
	 * @param invokedProcess
	 * @param node
	 * @param dataVar
	 * @return
	 * @throws SplitDataDependencyException
	 */
	private Invoke createInvokeSendFalse4SendingBlock(Process invokingProcess,
			Process invokedProcess, PWDGNode node, Variable dataVar)
			throws SplitDataDependencyException {

		if (invokingProcess == null || invokedProcess == null || node == null || dataVar == null)
			throw new NullPointerException();

		Definition invokedDefn = participant2WSDL.get(invokedProcess.getName());

		// partnerLink name agrees with the naming convention
		String partnerLinkName = invokingProcess.getName() + invokedProcess.getName() + "PL";
		PartnerLink pl = MyBPELUtils.getPartnerLink(invokingProcess, partnerLinkName);
		if (pl == null)
			throw new SplitDataDependencyException("partnerlink: " + pl + " does not exist.");

		// portType name agrees with the naming convention
		String portTypeName = invokingProcess.getName() + invokedProcess.getName() + "PT";
		PortType pt = MyWSDLUtil.findPortType(invokedDefn, portTypeName);
		if (pt == null)
			throw new SplitDataDependencyException("portType: " + portTypeName + " does not exist.");

		// operation name agrees with the naming convention
		String operationName = invokingProcess.getName() + invokedProcess.getName()
				+ dataVar.getName() + id(node) + "OP";
		Operation op = MyWSDLUtil.findOperation(pt, operationName);
		if (op == null)
			throw new SplitDataDependencyException("operation: " + operationName
					+ " does not exist.");

		// inputVariable Name agrees with the naming convention
		String inputVarName = id(node) + "FH";

		// messsageType of the inputVariable
		String msgName4SQ = getSQMessageName(invokingProcess.getName(), invokedProcess.getName(),
				dataVar.getName());
		QName msgQName = new QName(invokedDefn.getTargetNamespace(), msgName4SQ);

		// message for single query set, containing status part and data part
		Message msg4SQ = MyWSDLUtil.resolveMessage(invokedDefn, msgQName);
		if (msg4SQ == null)
			throw new SplitDataDependencyException("mesage:" + msgName4SQ + " does not exist.");

		// create the inputVariable for false, then set the messageType
		Variable inputVar4StatusFalse = BPELFactory.eINSTANCE.createVariable();
		inputVar4StatusFalse.setName(inputVarName);
		inputVar4StatusFalse.setMessageType(msg4SQ);

		// add the inputVariable into the invoking process
		invokingProcess.getVariables().getChildren().add(inputVar4StatusFalse);

		// invoke for sending false
		Invoke invoke = BPELFactory.eINSTANCE.createInvoke();
		String invoke4FalseName = "Send" + dataVar.getName() + id(node) + "FH";
		invoke.setName(invoke4FalseName);
		invoke.setInputVariable(inputVar4StatusFalse);
		invoke.setPartnerLink(pl);
		invoke.setPortType(pt);
		invoke.setOperation(op);

		// create a message link in topology for the invoke false
		org.bpel4chor.model.topology.impl.MessageLink topologyMsgLink = BPEL4ChorFactory
				.createTopologyMessageLink();
		NameGenerator nameGen = new NameGenerator(topology);
		String topoMsgLinkFalseName = nameGen.getUniqueTopoMsgLinkName(msgName4SQ + "Link");
		String senderName = invokingProcess.getName();
		String sendActName = invoke.getName();

		topologyMsgLink.setName(topoMsgLinkFalseName);
		topologyMsgLink.setSender(senderName);
		topologyMsgLink.setSendActivity(sendActName);
		topologyMsgLink.setMessageName(msgName4SQ);

		topology.add(topologyMsgLink);

		// the corresponding message in grounding
		org.bpel4chor.model.grounding.impl.MessageLink groundingMsgLink = BPEL4ChorFactory
				.createGroundingMessageLink(topologyMsgLink, pt, op);
		grounding.add(groundingMsgLink);

		return invoke;
	}

	/**
	 * Create assign activity for initializing the inputVariable for sending
	 * 'false'.
	 * 
	 * @param invoke4False
	 * @param dataVar
	 * @param node
	 * @param invokingProcess
	 * @return
	 */
	private Assign createAssignVarFalse4SendingBlock(Invoke invoke4False, Variable dataVar,
			PWDGNode node, Process invokingProcess) {

		if (invoke4False == null || dataVar == null || node == null)
			throw new NullPointerException();

		// inputVariable
		Variable inputVar = invoke4False.getInputVariable();

		// assign
		Assign assign = BPELFactory.eINSTANCE.createAssign();
		String assignName = "Assign" + dataVar.getName() + id(node);
		assign.setName(assignName);

		// copy for the status part
		boolean status = false;
		Copy copy4Status = FragmentFactory.createCopy4Status(inputVar, status);
		assign.getCopy().add(copy4Status);

		// copy for the correlation part
		Copy copy4Correl = FragmentFactory.createCopy4Correlatoin(invokingProcess, inputVar);
		assign.getCopy().add(copy4Correl);

		return assign;
	}

	/**
	 * Create Sequence to accommodate the invoke and assign activities in
	 * FaultHandler
	 * 
	 * @param invoke4False
	 * @param assign4False
	 * @param dataVar
	 * @param node
	 * @return
	 */
	private Sequence createSequenceFH4SendingBlock(Invoke invoke4False, Assign assign4False,
			Variable dataVar, PWDGNode node) {

		Sequence sequence = BPELFactory.eINSTANCE.createSequence();
		String sequenceName = "LR" + dataVar.getName() + id(node) + "FH";
		sequence.setName(sequenceName);
		sequence.getActivities().add(assign4False);
		sequence.getActivities().add(invoke4False);
		return sequence;
	}

	/**
	 * Create the sequence to accommodate a new created assign activity and the
	 * invoke activity for sending data and status.
	 * <p>
	 * Note: the parameter 'var' is from the main process, NOT from the local
	 * fragment process, therefore it can not directly be assigned into the data
	 * part of the 'inputVar', the corresponding variable that has the same name
	 * as 'var' in the local fragment process (invoking process) must be found
	 * and used in this case.
	 * 
	 * @param invokingProcess
	 * @param invokedProcess
	 * @param var
	 *            The variable that is currently being accessed by the reader
	 * @param inputVar
	 *            The inputVariable for the invoke activity
	 * @param node
	 *            The PWDG node
	 * 
	 * @return The sequence that contains the assign activity and the invoke
	 *         activity
	 */
	private Sequence createSequence4MQAndDiffPartition(Process invokingProcess,
			Process invokedProcess, Variable var, Variable inputVar, PWDGNode node) {

		if (invokingProcess == null || invokedProcess == null || var == null || inputVar == null
				|| node == null)
			throw new NullPointerException();

		Definition invokedDefn = participant2WSDL.get(invokedProcess.getName());

		//
		// Invoke for sending data and status
		//

		// create the invoke activity for sending data and status
		Invoke invoke4SendDataAndStatus = BPELFactory.eINSTANCE.createInvoke();
		invoke4SendDataAndStatus.setName("Send" + var.getName() + id(node));
		invoke4SendDataAndStatus.setTargets(BPELFactory.eINSTANCE.createTargets());

		// set inputVariable
		invoke4SendDataAndStatus.setInputVariable(inputVar);

		// set joinCondition=true
		Condition joinCond = BPELFactory.eINSTANCE.createCondition();
		joinCond.setBody("true()");
		invoke4SendDataAndStatus.getTargets().setJoinCondition(joinCond);

		// partnerLink name agrees with the naming convention
		String partnerLinkName = invokingProcess.getName() + invokedProcess.getName() + "PL";
		PartnerLink pl = MyBPELUtils.getPartnerLink(invokingProcess, partnerLinkName);
		if (pl == null)
			throw new NullPointerException("partnerlink: " + pl + " does not exist.");

		// portType name agrees with the naming convention
		String portTypeName = invokingProcess.getName() + invokedProcess.getName() + "PT";
		PortType pt = MyWSDLUtil.findPortType(invokedDefn, portTypeName);
		if (pt == null)
			throw new NullPointerException("portType: " + portTypeName + " does not exist.");

		// operation name agrees with the naming convention
		String operationName = invokingProcess.getName() + invokedProcess.getName() + var.getName()
				+ id(node) + "OP";
		Operation op = MyWSDLUtil.findOperation(pt, operationName);
		if (op == null)
			throw new NullPointerException("operation: " + operationName + " does not exist.");

		//
		// assign for initialization of the inputVariable
		//

		// create assign for initializing inputVariable
		Assign assign4MQ = BPELFactory.eINSTANCE.createAssign();

		// name for assign
		String assignName = "Assign" + inputVar.getName();
		assign4MQ.setName(assignName);

		// copy to data part of the inputVariable
		Copy copyData = BPELFactory.eINSTANCE.createCopy();
		From fromData = BPELFactory.eINSTANCE.createFrom();
		To toData = BPELFactory.eINSTANCE.createTo();

		// from the variable 'x' in the local fragment process
		Variable varInFragmentProcess = MyBPELUtils.resolveVariable(var.getName(), invokingProcess);
		if (varInFragmentProcess == null)
			throw new NullPointerException();
		fromData.setVariable(varInFragmentProcess);

		// to the data-part of the inputVariable
		Part dataPart = (Part) inputVar.getMessageType().getPart("data");
		toData.setVariable(inputVar);
		toData.setPart(dataPart);

		copyData.setFrom(fromData);
		copyData.setTo(toData);

		assign4MQ.getCopy().add(copyData);

		// copy global variable to the correlation part of the inputVariable
		Copy copy4Correl = FragmentFactory.createCopy4Correlatoin(invokingProcess, inputVar);
		assign4MQ.getCopy().add(copy4Correl);

		// create the sequence activity
		Sequence sequence4MQ = BPELFactory.eINSTANCE.createSequence();
		String seqenceName = inputVar.getName() + "Sequence";
		sequence4MQ.setName(seqenceName);

		// add the assign and the invoke into sequence
		sequence4MQ.getActivities().add(assign4MQ);
		sequence4MQ.getActivities().add(invoke4SendDataAndStatus);

		//
		// collect bpel4chor artifacts
		//

		Message msg4MQ = inputVar.getMessageType();

		// create a topology message link for the invoke activity
		String msgName = msg4MQ.getQName().getLocalPart();
		NameGenerator nameGen = new NameGenerator(topology);

		org.bpel4chor.model.topology.impl.MessageLink topologyMsgLink = BPEL4ChorFactory
				.createTopologyMessageLink();
		String topoMsgLinkName = nameGen.getUniqueTopoMsgLinkName(msgName + "Link");
		String senderName = invokingProcess.getName();
		String sendActName = invoke4SendDataAndStatus.getName();

		topologyMsgLink.setName(topoMsgLinkName);
		topologyMsgLink.setSender(senderName);
		topologyMsgLink.setSendActivity(sendActName);
		topologyMsgLink.setMessageName(msgName);

		this.topology.add(topologyMsgLink);

		// create the corresponding grounding message link
		org.bpel4chor.model.grounding.impl.MessageLink groundingMsgLink = BPEL4ChorFactory
				.createGroundingMessageLink(topologyMsgLink, pt, op);
		this.grounding.add(groundingMsgLink);
		return sequence4MQ;
	}

	/**
	 * Create the Receiving Flow based on the given PWDG and the tuple set of
	 * QuerySet and WriterSet
	 * 
	 * <p>
	 * Note: the given parameter 'act' is the reader activity in the main
	 * process, to create the receiving flow, the corresponding reader activity
	 * (aka the 'act') in the local fragment process must be found and used. The
	 * same principle goes to the parameter 'var'.
	 * 
	 * @param act
	 *            The reader activity from the main process
	 * @param var
	 *            The variable that is from the main process and is read by the
	 *            reader activity
	 * @param pwdg
	 *            PWDG graph
	 * @param qwSet
	 *            The tuples of querySet and writerSet, aka. Q_s(a, x)
	 */
	protected void createReceivingFlow(Activity act, Variable var, PWDG pwdg, QueryWriterSet qwSet) {

		if (act == null || var == null || pwdg == null || qwSet == null)
			throw new NullPointerException();

		// participant of the reader activity
		Participant participant = participantOf(act);

		// reader activity's process
		Process readerProcess = participant2FragProc.get(participant.getName());

		// get the corresponding activity for 'act' in the local fragment
		// process
		Activity readerAct = MyBPELUtils.resolveActivity(act.getName(), readerProcess);
		if (readerAct == null)
			throw new NullPointerException();

		// get the corresponding variable for 'var' in the local fragment
		// process
		Variable readVar = MyBPELUtils.resolveVariable(var.getName(), readerProcess);
		if (readVar == null)
			throw new NullPointerException();

		// The Temporary Variable for assembling incoming data
		Variable tmpVar = BPELFactory.eINSTANCE.createVariable();
		String tmpVarName = "tmp" + readVar.getName() + "4" + act.getName();
		tmpVar.setName(tmpVarName);
		tmpVar.setMessageType(readVar.getMessageType());
		readerProcess.getVariables().getChildren().add(tmpVar);

		// get top level flow in the reader process
		Flow topLevelFlow = (Flow) readerProcess.getActivity();

		// create a flow in the process to assemble the incoming information
		Flow assemblingFlow = BPELFactory.eINSTANCE.createFlow();
		String flowName = var.getName() + "RFFlow";
		Links links4Flow = BPELFactory.eINSTANCE.createLinks();
		assemblingFlow.setName(flowName);
		assemblingFlow.setLinks(links4Flow);

		// add the flow into the reader process
		topLevelFlow.getActivities().add(assemblingFlow);

		// Map from the pwdg node to the begin activity
		Map<PWDGNode, Activity> node2ba = new HashMap<PWDGNode, Activity>();

		// Map from the pwdg node to the end activity list
		Map<PWDGNode, List<Activity>> node2ea = new HashMap<PWDGNode, List<Activity>>();

		// process the PWDG node
		for (PWDGNode node : pwdg.vertexSet()) {
			boolean isNodeAndReaderInSameParticipant = (node.getParticipant().equals(readerProcess
					.getName()));
			processNode(node, node2ba, node2ea, assemblingFlow, qwSet,
					isNodeAndReaderInSameParticipant, var, tmpVar, readerProcess);
		}

		for (DefaultEdge e : pwdg.edgeSet()) {

			PWDGNode sourceNode = pwdg.getEdgeSource(e);

			PWDGNode targetNode = pwdg.getEdgeTarget(e);
			// the begin activity in the target node
			Activity ba = node2ba.get(targetNode);

			// the end activities in the source node
			List<Activity> eaList = node2ea.get(sourceNode);

			for (Activity d : eaList) {
				// add a link l = (d, ba, true())
				Link link = FragmentFactory.createLinkBetween(d, ba, true);
				assemblingFlow.getLinks().getChildren().add(link);
			}
		}

		// add assign activity to copy the assembled data from the tmpVar into
		// the variable 'x', then add a link to connect the flow and this
		// assign, and a link to connect this assign and the reader activity.
		Assign assignTmp = BPELFactory.eINSTANCE.createAssign();
		String assignTmpName = "AssignTmp" + readVar.getName() + "4" + readerAct.getName();
		assignTmp.setName(assignTmpName);

		// copy-from-to
		Copy copy4AssignTmp = BPELFactory.eINSTANCE.createCopy();

		From from4AssignTmp = BPELFactory.eINSTANCE.createFrom();
		from4AssignTmp.setVariable(tmpVar);

		To to4AssignTmp = BPELFactory.eINSTANCE.createTo();
		to4AssignTmp.setVariable(readVar);

		copy4AssignTmp.setFrom(from4AssignTmp);
		copy4AssignTmp.setTo(to4AssignTmp);

		assignTmp.getCopy().add(copy4AssignTmp);

		// add assignTmp into flow
		topLevelFlow.getActivities().add(assignTmp);

		// create the link between the flow and the assignTmp
		boolean transCond = true;
		Link linkFlow2AssignTmp = FragmentFactory.createLinkBetween(assemblingFlow, assignTmp,
				transCond);
		topLevelFlow.getLinks().getChildren().add(linkFlow2AssignTmp);

		// create the link between the assignTemp and the reader activity
		Link linkTmp2Reader = FragmentFactory.createLinkBetween(assignTmp, readerAct, transCond);
		topLevelFlow.getLinks().getChildren().add(linkTmp2Reader);

	}

	/**
	 * Processing of the PWDG node given based on the pre-given conditions.
	 * <p>
	 * There are two major conditions and therefore four situations when the two
	 * conditions are combined. Condition 1 is whether PWDG node and the reader
	 * activity is in the same participant. Condition 2 is whether the query set
	 * in the tuple is single.
	 * 
	 * <ol>
	 * <li>
	 * <p>
	 * In case that the PWDG node is in the same participant as the reader
	 * activity
	 * 
	 * <p>
	 * a) If the tuple set of query set and writer set has only single query
	 * set. We create one assign activity in the receiving flow to copy the read
	 * variable to the temporary variable.
	 * 
	 * <p>
	 * b) If the tuple set of query set and writer set has more than one query
	 * set. We create one empty activity in the receiving flow and for each
	 * query set one assign activity. All the newly created assign activity will
	 * be connected with the empty activity.
	 * 
	 * <li>
	 * <p>
	 * In case that the PWDG node is in the different participant as the reader
	 * activity
	 * 
	 * <p>
	 * a) If the tuple set of query set and writer set has only single query
	 * set. We create one receive activity to accept the sent message and it
	 * links to one assign activity that copies the data from the message to the
	 * temporary variable.
	 * 
	 * <p>
	 * b) If the tuple set of query set and writer set has more than one query
	 * set. We create one receive activity in the receiving flow to accept the
	 * sent message, and for each of the query set one assign activity is
	 * created for copy the queried data part from the message into the
	 * temporary variable. The assign activities are collected to the receive
	 * activity by links. </ul>
	 * </ol>
	 * 
	 * @param node2ba
	 *            The map for the node and the begin activity
	 * @param node2ea
	 *            The map for the node and the end activities
	 * @param node
	 *            The current PWDG node
	 * @param assemblingFlow
	 *            The flow for assembling the incoming information
	 * @param qwSet4Node
	 *            The Tuples for Query Set and Writer Set, aka. Q_s(a, x)
	 * @param isInSameParticipant
	 *            Whether the node and the reader are in the same participant
	 * @param var
	 *            The variable that is from main process and is accessed by the
	 *            reader activity
	 * @param tmpVar
	 *            The temporary variable for assembling the written information
	 * @param readerProcess
	 *            The process in which the reader activity presents
	 */
	protected void processNode(PWDGNode node, Map<PWDGNode, Activity> node2ba,
			Map<PWDGNode, List<Activity>> node2ea, Flow assemblingFlow, QueryWriterSet qwSet,
			boolean isInSameParticipant, Variable var, Variable tmpVar, Process readerProcess) {

		if (node2ba == null || node2ea == null || node == null || assemblingFlow == null
				|| qwSet == null)
			throw new NullPointerException();

		// get Q_s(n, a, x)
		QueryWriterSet qws4Node = qwSet.getQueryWriterSetFor(node);

		// if the set of the query sets is empty, return.
		if (qws4Node.querySets().isEmpty())
			return;

		if (isInSameParticipant)
			// participant of node == participant of reader
			processNodeSameParticipant(node, node2ba, node2ea, qws4Node, assemblingFlow, var,
					tmpVar);
		else
			// participant of node != participant of reader
			processNodeDiffParticipant(node, node2ba, node2ea, qws4Node, assemblingFlow, var,
					tmpVar, readerProcess);

	}

	/**
	 * Process the node that is in the same participant as the reader
	 * 
	 * @param node
	 *            The PWDG node
	 * @param node2ba
	 *            The node to begin activity map
	 * @param node2ea
	 *            The node to end activities set map
	 * @param qws4Node
	 *            The tuple set of query set and writer set in the node
	 * @param assemblingFlow
	 *            The flow for assembling the incoming information
	 * @param var
	 *            The variable from the main process and has corresponding
	 *            variable in the local fragment process
	 * @param tmpVar
	 *            The temporary variable for assembling the written information
	 * @see The Procedure PROCESS-NODE in case p==p_r in the paper 'Maintaining
	 *      Data Dependencies Across BPEL Process Fragments'
	 */
	private void processNodeSameParticipant(PWDGNode node, Map<PWDGNode, Activity> node2ba,
			Map<PWDGNode, List<Activity>> node2ea, QueryWriterSet qws4Node, Flow assemblingFlow,
			Variable var, Variable tmpVar) {

		// create a new empty end activities list
		List<Activity> eaList = new ArrayList<Activity>();

		// begin activity
		Activity ba = null;

		// the set of the query sets
		Set<Set<String>> qsSet = qws4Node.querySets();

		// The writer activities in the given PWDG node,
		// Note that the writer activities are in the main process, if
		// some one is needed, the corresponding activity in the local fragment
		// process, must be found at first.
		Set<Activity> writersInNode = node.getActivities();

		// the readerProcess
		Process readerProcess = participant2FragProc.get(node.getParticipant());

		if (qsSet.size() == 1) {

			// node and reader in same participant; single query set

			Assign baAssign = BPELFactory.eINSTANCE.createAssign();
			String baName = "Assign" + id(node);
			baAssign.setName(baName);

			Set<String> qs = qsSet.iterator().next();
			for (String q : qs) {
				// Add copy for each query in the query set.
				// Recall that, a tmpVar is created to assemble incoming
				// information in the assembling flow, the written information
				// is copied from the queried parts of the variable 'var' into
				// the same parts of the variable 'tmpVar'
				Variable varInFragmentProc = MyBPELUtils.resolveVariable(var.getName(),
						readerProcess);
				if (varInFragmentProc == null)
					throw new NullPointerException();
				Copy copy = createCopy4QuerySamePartition(varInFragmentProc, tmpVar, q);

				// add into baAssign
				baAssign.getCopy().add(copy);
			}

			ba = baAssign;
			assemblingFlow.getActivities().add(ba);

			if (writersInNode.size() == 1) {
				// Add link l = (b, ba, true()).
				// Note that the activity in the writer set is in the main
				// process, we need the corresponding one in the local fragment
				// process.
				Activity bInMainProcess = writersInNode.iterator().next();
				Activity b = MyBPELUtils.resolveActivity(bInMainProcess.getName(), readerProcess);
				boolean transitionCondition = true;
				Link linkb2ba = FragmentFactory.createLinkBetween(b, ba, transitionCondition);
				assemblingFlow.getLinks().getChildren().add(linkb2ba);
			}

			eaList.add(ba);

		} else {

			// node and reader in same participant; more than one query set

			ba = BPELFactory.eINSTANCE.createEmpty();
			String baName = "RF" + id(node) + "Empty";
			ba.setName(baName);
			
			assemblingFlow.getActivities().add(ba);

			for (Set<String> qs : qsSet) {
				//
				// Recall that, in case of same partition and multiple
				// query sets, the name of the status part for the query set
				// agrees with the naming convention: status+id(qs), and the
				// variable that contains the multiple status parts is named
				// with 'idn(node)'
				String conditionBody = "$" + getQStatusStr(id(node), qs);
				//
				// Create the QAssign for each query set and the link
				// l=(ba, QAssign, QS-Status).
				// Recall that in the local resolver, in the case of same
				// participant and more than one query set, a variable that
				// contains multiple status parts is created.
				// The naming convention of this variable is 'idn(n)'. The
				// transition condition for the link can be set as the
				// corresponding part in the variable 'idn(n)' namely the
				// 'qsStatus'.
				Variable varInFragmentProc = MyBPELUtils.resolveVariable(var.getName(),
						readerProcess);
				boolean isSamePartition = true;
				boolean isSingleQuerySet = false;
				createQAssign(node, qs, eaList, ba, varInFragmentProc, tmpVar, conditionBody,
						assemblingFlow, isSamePartition, isSingleQuerySet);
			}

		}
		// put the key value pair into the maps
		node2ba.put(node, ba);
		node2ea.put(node, eaList);

		if (writersInNode.size() > 1) {
			// Add link l=(em, ba, true()), em is the emtpy from LR
			// Recall that in the case same partition and multiple query sets in
			// the local resolver, an empty activity is created with the name
			// idn(n)+'Empty'. In this case, the 'ba' is also an empty activity,
			// with the name 'RF' + idn(n) + 'Empty'.
			Activity em = MyBPELUtils.resolveActivity(id(node) + "Empty", readerProcess);
			boolean transitionCondition = true;
			Link linkem2ba = FragmentFactory.createLinkBetween(em, ba, transitionCondition);
			assemblingFlow.getLinks().getChildren().add(linkem2ba);
		}

	}

	/**
	 * Get the status part string for the given query set
	 * 
	 * @param varName
	 *            The name of the variable that contains multiple status parts
	 * @param qs
	 *            The query set
	 * @return The concatenation of varName + ".status" + id(qs)
	 */
	private String getQStatusStr(String varName, Set<String> qs) {

		if (id(qs) == null)
			throw new IllegalStateException();

		StringBuffer sb = new StringBuffer();
		sb.append(varName);
		sb.append(".");
		sb.append("status");
		sb.append(id(qs));
		return sb.toString();
	}

	/**
	 * Create an assign activity for copying the queried part in the given query
	 * set 'qs' from 'fromVar' to 'toVar'. Add the assign activity into the end
	 * activities list 'eaList'. Then create a link from 'ba' activity to the
	 * assign activity with the given 'condBody' as transition condition body.
	 * 
	 * @param node
	 *            The PWDG node
	 * @param qs
	 *            The query set
	 * @param eaList
	 *            The end activities list
	 * @param ba
	 *            The begin activity
	 * @param fromVar
	 *            The variable the assign copies from
	 * @param toVar
	 *            The variable the assign copies to
	 * @param condBody
	 *            The condition body for the transition condition of the link
	 * @param assemblingFlow
	 *            The assembling flow
	 * @param isSamePartition
	 *            Whether the node and the reader activity is in the same
	 *            partition
	 * @param isSingleQuerySet
	 *            'true' if the tuple set of the query set contains single query
	 *            set, otherwise 'false'
	 */
	private void createQAssign(PWDGNode node, Set<String> qs, List<Activity> eaList, Activity ba,
			Variable fromVar, Variable toVar, String condBody, Flow assemblingFlow,
			boolean isSamePartition, boolean isSingleQuerySet) {

		if (qs == null || eaList == null || ba == null || fromVar == null || toVar == null
				|| assemblingFlow == null)
			throw new NullPointerException();

		// create a new assign
		Assign act = BPELFactory.eINSTANCE.createAssign();

		String actName = null;

		// For single query set and multiple query set has different name for
		// the assign
		if (isSingleQuerySet)
			actName = "Assign" + id(node);
		else
			actName = "Assign" + id(node) + id(qs);

		act.setName(actName);
		assemblingFlow.getActivities().add(act);

		// add the new assign into the end activities list
		eaList.add(act);

		// Add link l=(ba, act, statusQS)
		Link linkba2act = FragmentFactory.createLinkBetween(ba, act, condBody);
		assemblingFlow.getLinks().getChildren().add(linkba2act);

		// for each query in the 'qs', create a copy from the 'fromVar' to the
		// 'toVar'
		for (String q : qs) {
			if (isSamePartition) {
				Copy copy = createCopy4QuerySamePartition(fromVar, toVar, q);
				act.getCopy().add(copy);
			} else {
				Copy copy = createCopy4QueryDiffPartition(fromVar, toVar, q);
				act.getCopy().add(copy);
			}

		}
	}

	/**
	 * Create the Copy from the locations in the 'fromVar' to the same locations
	 * in 'toVar'
	 * 
	 * @param fromVar
	 *            The source variable
	 * @param toVar
	 *            The target variable
	 * @param query
	 *            The query over the variable
	 * @return The created Copy
	 */
	private Copy createCopy4QuerySamePartition(Variable fromVar, Variable toVar, String query) {

		if (fromVar == null | toVar == null | query == null)
			throw new NullPointerException();

		Copy copy = BPELFactory.eINSTANCE.createCopy();
		From fromQuery = BPELFactory.eINSTANCE.createFrom();
		To toQuery = BPELFactory.eINSTANCE.createTo();

		// from-spec: 'fromVar', part,
		int beginIndx = 1;
		String partStr = query.startsWith(".") ? query.substring(beginIndx) : query;
		Part queriedPart = (Part) fromVar.getMessageType().getPart(partStr);
		fromQuery.setVariable(fromVar);
		fromQuery.setPart(queriedPart);

		// to-spec: 'toVar', part
		Part sameQueriedPart = (Part) toVar.getMessageType().getPart(partStr);
		toQuery.setVariable(toVar);
		toQuery.setPart(sameQueriedPart);

		// set from and to
		copy.setFrom(fromQuery);
		copy.setTo(toQuery);

		return copy;
	}

	/**
	 * Create the Copy from the locations in the 'data' part of the 'fromVar' to
	 * the same locations in 'toVar'
	 * 
	 * @param fromVar
	 * @param toVar
	 * @param queryStr
	 * @return
	 */
	private Copy createCopy4QueryDiffPartition(Variable fromVar, Variable toVar, String queryStr) {

		if (fromVar == null | toVar == null | queryStr == null)
			throw new NullPointerException();

		Copy copy = BPELFactory.eINSTANCE.createCopy();
		From fromQuery = BPELFactory.eINSTANCE.createFrom();
		To toQuery = BPELFactory.eINSTANCE.createTo();

		// from-spec: 'fromVar', part,
		String queriedPartStr = queryStr.startsWith(".") ? queryStr : "." + queryStr;
		Query query = BPELFactory.eINSTANCE.createQuery();
		query.setValue("/data" + queriedPartStr.replace(".", "/"));
		fromQuery.setVariable(fromVar);
		fromQuery.setQuery(query);

		// to-spec: 'toVar', part
		int beginIndex = 1;
		String partStr = queryStr.startsWith(".") ? queryStr.substring(beginIndex) : queryStr;
		Part sameQueriedPart = (Part) toVar.getMessageType().getPart(partStr);
		toQuery.setVariable(toVar);
		toQuery.setPart(sameQueriedPart);

		// set from and to
		copy.setFrom(fromQuery);
		copy.setTo(toQuery);

		return copy;
	}

	/**
	 * Process the node that is in the different participant as the reader
	 * <p>
	 * We create a receive activity to accept the sent message from local
	 * resolver, and depending on the number of the query set, we create for
	 * each query set one assign activity that copies the data from the received
	 * message into the temporary variable in the receiving flow.
	 * 
	 * @param node
	 *            The PWDG node
	 * @param node2ba
	 *            The map for node and the begin activity
	 * @param node2ea
	 *            The map for node and the end activity list
	 * @param qws4Node
	 *            The tuple set of the query set and writer set that only suit
	 *            to the node given
	 * @param assemblingFlow
	 *            The Flow assembling information aka. the receiving flow
	 * @param var
	 *            The variable that is being read by the reader
	 * @param tmpVar
	 *            The variable for assembling information in the assembling flow
	 * @param readerProcess
	 *            The process where the reader presents
	 * @see The Procedure PROCESS-NODE in case p!=p_r in the paper 'Maintaining
	 *      Data Dependencies Across BPEL Process Fragments'
	 */
	private void processNodeDiffParticipant(PWDGNode node, Map<PWDGNode, Activity> node2ba,
			Map<PWDGNode, List<Activity>> node2ea, QueryWriterSet qws4Node, Flow assemblingFlow,
			Variable var, Variable tmpVar, Process readerProcess) {
		// the end activities list
		List<Activity> eaList = new ArrayList<Activity>();

		// the begin activity as receive activity
		Activity ba = null;

		// inputVariable for the receive follows the naming convention
		Variable inputVar = BPELFactory.eINSTANCE.createVariable();
		String inputVarName = id(node);
		inputVar.setName(inputVarName);

		// create receive activity to accept the message from the node process
		Receive rrb = createReceive4ReceivingFlow(node, inputVar, readerProcess);

		// create assign activity for copying correlation into global variable
		Assign assign4GlobalVar = FragmentFactory.createAssign4GlobalVar(readerProcess,
				rrb.getVariable());

		// create sequence to accommodate the receive and the assign activity
		String seqName = "RFSequence";
		Sequence sequence = FragmentFactory.createSequence4GlobalVar(readerProcess, seqName);

		sequence.getActivities().add(rrb);
		sequence.getActivities().add(assign4GlobalVar);

		ba = sequence;

		assemblingFlow.getActivities().add(ba);

		// The process where the node presents
		Process nodeProcess = participant2FragProc.get(node.getParticipant());

		// WSDL Definition of the reader process
		Definition readerDefn = participant2WSDL.get(readerProcess.getName());

		Set<Set<String>> qsSet = qws4Node.querySets();

		if (qsSet.size() == 1) {

			// Different partitions, single query set

			// message name in the case that the Q_s has only single query set
			// has the naming convention Name = invokingPartitionName
			// +invokedPartitionName + varName + 'SQMessage'
			StringBuffer sb = new StringBuffer();
			sb.append(nodeProcess.getName());
			sb.append(readerProcess.getName());
			sb.append(var.getName());
			sb.append("SQMessage");
			String msgName = sb.toString();
			QName msgQName = new QName(readerDefn.getTargetNamespace(), msgName);
			Message message = MyWSDLUtil.resolveMessage(readerDefn, msgQName);
			if (message == null)
				throw new NullPointerException();
			inputVar.setMessageType(message);

			Set<String> qs = qsSet.iterator().next();

			String condBody = "$" + inputVar.getName() + ".status";

			boolean isSamePartition = false;
			boolean isSingleQuerySet = true;
			createQAssign(node, qs, eaList, ba, inputVar, tmpVar, condBody, assemblingFlow,
					isSamePartition, isSingleQuerySet);

		} else {

			// Different partitions, more than one query set

			// message name in the case that the Q_s has more than one query set
			// has the naming convention Name = invokingPartitionName
			// +invokedPartitionName + varName + idn(node) + 'MQMessage'
			StringBuffer sb = new StringBuffer();
			sb.append(nodeProcess.getName());
			sb.append(readerProcess.getName());
			sb.append(var.getName());
			sb.append(id(node));
			sb.append("MQMessage");
			String msgName = sb.toString();
			QName msgQName = new QName(readerDefn.getTargetNamespace(), msgName);
			Message message = MyWSDLUtil.resolveMessage(readerDefn, msgQName);
			if (message == null)
				throw new NullPointerException();
			inputVar.setMessageType(message);

			for (Set<String> qs : qsSet) {
				String condBody = "$" + getQStatusStr(inputVar.getName(), qs);
				boolean isSamePartition = false;
				boolean isSingleQuerySet = false;
				createQAssign(node, qs, eaList, ba, inputVar, tmpVar, condBody, assemblingFlow,
						isSamePartition, isSingleQuerySet);
			}
		}

		// put the key value pair into the maps
		node2ba.put(node, ba);
		node2ea.put(node, eaList);

		//
		// resolve the corresponding topology message link for sending True
		//

		// naming convention for the invoke (true) in local resolver
		String sendActTrue = "Send" + var.getName() + id(node);
		org.bpel4chor.model.topology.impl.MessageLink topoMsgLinkTrue = BPEL4ChorUtil
				.resolveTopologyMessageLinkBySendAct(topology, sendActTrue);

		// update the attribute 'receiver' and 'receiveActivity'
		topoMsgLinkTrue.setReceiver(readerProcess.getName());
		topoMsgLinkTrue.setReceiveActivity(rrb.getName());

		//
		// resolve the corresponding topology message link for sending False
		//

		if (qsSet.size() == 1) {
			// naming convention for the invoke (false) in local resolver
			String sendActFalse = "Send" + var.getName() + id(node) + "FH";
			org.bpel4chor.model.topology.impl.MessageLink topoMsgLinkFalse = BPEL4ChorUtil
					.resolveTopologyMessageLinkBySendAct(topology, sendActFalse);

			// update the attribute 'receiver' and 'receiveActivity'
			topoMsgLinkFalse.setReceiver(readerProcess.getName());
			topoMsgLinkFalse.setReceiveActivity(rrb.getName());
		}

		// besides, update the attribute 'selects' in the participant of sender
		org.bpel4chor.model.topology.impl.Participant topoParticipant = BPEL4ChorUtil
				.resolveParticipant(topology, topoMsgLinkTrue.getSender());

		if (topoParticipant.getSelects().contains(readerProcess.getName()) == false)
			topoParticipant.getSelects().add(readerProcess.getName());

	}

	/**
	 * Create a receive activity for accepting message from the PWDG node.
	 * 
	 * @param node
	 *            The pwdg node
	 * @param inputVar
	 * @param readerProcess
	 *            The process where the reader is present
	 * @return
	 */
	private Receive createReceive4ReceivingFlow(PWDGNode node, Variable inputVar,
			Process readerProcess) {

		Receive rrb = BPELFactory.eINSTANCE.createReceive();

		String rrbName = "Receive" + id(node);
		rrb.setName(rrbName);

		rrb.setVariable(inputVar);

		Condition jc = BPELFactory.eINSTANCE.createCondition();
		jc.setBody("true()");

		Targets targets = BPELFactory.eINSTANCE.createTargets();
		targets.setJoinCondition(jc);
		rrb.setTargets(targets);

		// add correlation set
		Correlation correlation = BPELFactory.eINSTANCE.createCorrelation();
		CorrelationSet correlSet = MyBPELUtils.resolveCorrelationSet(readerProcess,
				BPEL4ChorModelConstants.CORRELATION_SET_NAME);
		correlation.setSet(correlSet);
		correlation.setInitiate("join");

		Correlations correlations = BPELFactory.eINSTANCE.createCorrelations();
		correlations.getChildren().add(correlation);

		rrb.setCorrelations(correlations);

		return rrb;

	}

	/**
	 * Create one 'assign' activity to copy the correlation value to the global
	 * variable and create one 'sequence' activity to wrap both of the initial
	 * 'receive' activity and the 'assign' activity. The links of the 'receive'
	 * activity will be rerouted to the newly created 'sequence' activity.
	 * <p>
	 * Note that the initial 'receive' activity means the one that was the
	 * starting activity in the original process and now is copied in one of the
	 * fragment processes.
	 * 
	 * @throws ActivityNotFoundException
	 */
	protected void wrapInitialReceive() throws ActivityNotFoundException {

		ActivityIterator actIt = new ActivityIterator(nonSplitProcess);
		List<Receive> receiveList = new ArrayList<Receive>();
		while (actIt.hasNext()) {
			Activity act = actIt.next();
			if (act instanceof Receive) {
				Receive receive = (Receive) act;
				if (receive.getCorrelations() == null || receive.getCorrelations().getChildren() == null || receive.getCorrelations().getChildren().size() == 0) {
					throw new IllegalStateException("Main process contains no correlations.");
				}
				if (receive.getCorrelations().getChildren().size() == 1) {
					receiveList.add(receive);
				} else {
					throw new IllegalStateException(
							"Main process contains more than one correlation.");
				}
			}
		}

		// find the participant where the <receive> activity presents, then find
		// the fragment process that relates to this participant. We create the
		// <assign> activity and the <sequence> to wrap them together, at the
		// end reroute the links of the <receive> activity.
		for (Receive origReceive : receiveList) {

			Participant participant = partitionSpec.getParticipant(origReceive);
			Process fragProcess = participant2FragProc.get(participant.getName());

			Receive receive = (Receive) MyBPELUtils.resolveActivity(origReceive.getName(),
					fragProcess);

			Assign assign = FragmentFactory.createAssign4GlobalVar(fragProcess,
					receive.getVariable());

			String sugguestSeqName = "InitialReceiveSequence";
			Sequence sequence = FragmentFactory.createSequence4GlobalVar(fragProcess,
					sugguestSeqName);

			rerouteLinks(receive, sequence);

			// Find parent of the receive
			ActivityFinder finder = new ActivityFinder(fragProcess);
			Activity parent = (Activity) finder.findParent(receive.getName());

			// BE AWARE! after the .add(receive) operation, the 'receive' will
			// automatically be removed from its original parent container
			// (flow). It is done by the EMF mechanism, recall that the BPEL
			// model is based on EMF. So we do not need to remove it from the
			// activities list of the parent 'flow'.
			sequence.getActivities().add(receive);
			sequence.getActivities().add(assign);

			if (parent instanceof Flow) {
				Flow parentFlow = (Flow) parent;
				parentFlow.getActivities().add(0, sequence);
			} else {
				throw new IllegalStateException("Not expected parent activity, child:"
						+ receive.getName() + " parent:" + parent.getName());
			}

		}
	}

	/**
	 * Reroute the links, of which the original activity is the source activity,
	 * to the new activity. So that the new activity will be the source activity
	 * of the links.
	 * 
	 * @param origAct
	 * @param newAct
	 */
	private void rerouteLinks(Activity origAct, Activity newAct) {

		// Solution 1: Iterator Concurrent Modification Exception
		// iterate all the source, set the activity of the
		// source to the new activity.
		// List<Source> origSourceList = origAct.getSources().getChildren();
		// for (Source origSource : origSourceList) {
		// origSource.setActivity(newAct);
		// }

		// Solution 2: Iterator Concurrent Modification Exception
		// Iterator<Source> sourceIterator =
		// origAct.getSources().getChildren().iterator();
		// while(sourceIterator.hasNext()) {
		// Source origSource = sourceIterator.next();
		// origSource.setActivity(newAct);
		// }

		// Solution 3: This one works
		Source[] sourceList = origAct.getSources().getChildren().toArray(new Source[0]);
		for (int i = 0; i < sourceList.length; i++) {
			sourceList[i].setActivity(newAct);
		}
	}

	/**
	 * Get a system wide unique name string associated to the querySet
	 * 
	 * @param querySet
	 * @return
	 */
	protected String id(Set<String> querySet) {
		if (querySet == null)
			throw new NullPointerException();

		return querySet2NameMap.get(querySet);
	}

	/**
	 * Get a system wide unique name string associated to the pwdg node
	 * 
	 * @param node
	 * @return
	 */
	protected String id(PWDGNode node) {
		if (node == null)
			throw new NullPointerException();

		return node2NameMap.get(node);
	}

	/**
	 * Init the querySet2NameMap, for query set {.a, .b, .c} the id format looks
	 * like 'abc-'+'xxxx'(x can be number and character).
	 * 
	 * @param varName
	 * @param keySet
	 */
	protected void initQuerySet2nameMap(String varName, Set<Set<String>> keySet) {

		if (varName == null || keySet == null)
			throw new NullPointerException();

		querySet2NameMap = new HashMap<Set<String>, String>();

		for (Set<String> key : keySet) {
			querySet2NameMap.put(key, DataDependencyHelper.createId4QuerySet(varName, key));
		}

	}

	/**
	 * Update the querySet2Namemap with the newly generated QueryWriterSet
	 * filtered by the PWDG node.
	 * 
	 * @param qwSet4Node
	 */
	protected void updateQuerySet2NameMap(QueryWriterSet qwSet4Node) {
		String varName = qwSet4Node.getVariable().getName();
		for (Set<String> querySet : qwSet4Node.querySets()) {
			if (id(querySet) == null) {
				querySet2NameMap.put(querySet,
						DataDependencyHelper.createId4QuerySet(varName, querySet));
			}
		}
	}

	/**
	 * Initialize the node2NameMap, for node {A, B, C} the id format looks like
	 * 'ABC-'+'xxxx'(x can be number and character).
	 * 
	 * @param pwdg
	 */
	protected void initNode2NameMap(PWDG pwdg) {
		node2NameMap = new HashMap<PWDGNode, String>();

		for (PWDGNode node : pwdg.vertexSet()) {
			StringBuffer sb = new StringBuffer();
			for (Activity act : node.getActivities()) {
				sb.append(act.getName());
			}
			sb.append("-");
			sb.append(RandomIdGenerator.getId());
			node2NameMap.put(node, sb.toString());
		}
	}

	/**
	 * Get activity list sorted by name from the partitionSpecification
	 * 
	 * @return
	 */
	protected void sortActivities(List<Activity> activities) {
		Collections.sort(activities, new Comparator<Activity>() {
			@Override
			public int compare(Activity o1, Activity o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
	}

	/**
	 * Get variable list sorted by name from the non-split process
	 * 
	 * @return
	 */
	protected void sortVariables(List<Variable> variables) {
		Collections.sort(variables, new Comparator<Variable>() {
			@Override
			public int compare(Variable o1, Variable o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
	}

	/**
	 * Figure out the participant of activity given
	 * 
	 * @param act
	 * @return
	 */
	protected Participant participantOf(Activity act) {
		return partitionSpec.getParticipant(act);
	}

}
