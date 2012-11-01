package org.bpel4chor.splitprocess.fragmentation;

import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.bpel4chor.splitprocess.dataflowanalysis.QueryWriterSet;
import org.bpel4chor.splitprocess.partition.model.Participant;
import org.bpel4chor.splitprocess.partition.model.PartitionSpecification;
import org.bpel4chor.splitprocess.pwdg.model.PWDG;
import org.bpel4chor.splitprocess.pwdg.model.PWDGNode;
import org.bpel4chor.splitprocess.utils.RandomIdGenerator;
import org.bpel4chor.splitprocess.utils.SplitProcessConstants;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.PartnerLink;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Variable;
import org.eclipse.bpel.model.messageproperties.MessagepropertiesFactory;
import org.eclipse.bpel.model.messageproperties.Property;
import org.eclipse.bpel.model.messageproperties.PropertyAlias;
import org.eclipse.bpel.model.partnerlinktype.PartnerLinkType;
import org.eclipse.bpel.model.partnerlinktype.PartnerlinktypeFactory;
import org.eclipse.bpel.model.partnerlinktype.Role;
import org.eclipse.wst.wsdl.Definition;
import org.eclipse.wst.wsdl.Input;
import org.eclipse.wst.wsdl.Message;
import org.eclipse.wst.wsdl.Operation;
import org.eclipse.wst.wsdl.Part;
import org.eclipse.wst.wsdl.PortType;
import org.eclipse.wst.wsdl.WSDLFactory;
import org.eclipse.xsd.util.XSDConstants;

import de.uni_stuttgart.iaas.bpel.model.utilities.MyBPELUtils;
import de.uni_stuttgart.iaas.bpel.model.utilities.MyWSDLUtil;

/**
 * This DataDependencyHelper provides necessary stuffs before the local resolver
 * and receiving flow can be created, e.g. the message, portType,
 * partnerLinkType, partnerLink.
 * <p>
 * This helper gets all the information from the invoker for initialisation, it
 * includes the participant to fragment process map, the participant to wsdl
 * map, partitionSpecification, the current activity and variable.
 * 
 * @since Mar 13, 2012
 * @author Daojun Cui
 */
public class DataDependencyHelper {

	protected Map<String, Process> participant2fragProc = null;

	protected Map<String, Definition> participant2wsdl = null;

	protected PartitionSpecification partitionSpec = null;

	protected Activity act = null;

	protected Variable var = null;

	protected Logger logger = Logger.getLogger(DataDependencyHelper.class);

	public DataDependencyHelper(Map<String, Process> participant2fragProc,
			Map<String, Definition> participant2wsdl, PartitionSpecification partitionSpec,
			Activity act, Variable var) {

		if (participant2fragProc == null || participant2wsdl == null || partitionSpec == null
				|| act == null || var == null)
			throw new NullPointerException();

		this.participant2fragProc = participant2fragProc;
		this.participant2wsdl = participant2wsdl;
		this.partitionSpec = partitionSpec;
		this.act = act;
		this.var = var;
	}

	/**
	 * Create the prerequisites for fragmenting data dependency respecting
	 * current activity 'act' and current variable 'var'.
	 * 
	 * @param pwdg
	 *            PWDG graph for the current 'act' and 'var'
	 * @param qws
	 *            QueryWriterSet for the current 'act' and 'var'
	 * @param idMap
	 *            map that associates unique id string to the querySet
	 * @param idnMap
	 *            map that associates unique id string to the pwdg node
	 */
	public void createPrerequisites(PWDG pwdg, QueryWriterSet qws, Map<Set<String>, String> idMap,
			Map<PWDGNode, String> idnMap) {

		if (pwdg == null || qws == null || idMap == null || idnMap == null)
			throw new NullPointerException();

		for (PWDGNode n : pwdg.vertexSet()) {

			Participant pn = partitionSpec.getParticipant(n.getParticipant());
			Participant pr = partitionSpec.getParticipant(act);

			// empty query set
			QueryWriterSet qws4Node = qws.getQueryWriterSetFor(n);
			if (qws4Node.size() == 0)
				continue;

			// Due to the tuples of query set to writer set being
			// filtered by the pwdg node given, some query sets
			// might be merged together because of the same writer
			// set, then some not-in-id-map-registered query set
			// will be generated, so the queryset2nameMap must be
			// updated now. More for merging of the query set see
			// also the page 50 in Diplomarbeit Nr. 3255 -
			// "Splitting BPEL processes".
			updateQuerySet2NameMap(idMap, qws4Node);

			// single query set, same partition
			if (pn.getName().equals(pr.getName()) && qws4Node.size() == 1)
				continue;

			// multiple query sets, same partition
			if (pn.getName().equals(pr.getName())) {
				createPrerequisiteMessage(n, qws4Node, idMap, idnMap);
				continue;
			}

			// create prerequisite message from node n to reader r
			createPrerequisiteMessage(n, qws4Node, idMap, idnMap);

			// create prerequisite portType and operation
			createPrerequisitePortTypeOperation(pn, pr, n, qws4Node, idnMap);

			// create prerequisite partnerLinkType
			createPrerequisitePartnerLinkType(pn, pr);

			// create prerequisite partnerLink
			createPrerequisitePartnerLink(pn, pr);
		}
	}

	/**
	 * Update the querySet2NameMap
	 * 
	 * @param querySet2NameMap
	 * @param qws4Node
	 */
	private void updateQuerySet2NameMap(Map<Set<String>, String> querySet2NameMap,
			QueryWriterSet qws4Node) {
		String varName = qws4Node.getVariable().getName();
		for (Set<String> querySet : qws4Node.querySets()) {
			if (querySet2NameMap.get(querySet) == null) {
				querySet2NameMap.put(querySet, createId4QuerySet(varName, querySet));
			}
		}
	}

	/**
	 * Create one unique string based on the variable name given and the query
	 * set given.
	 * 
	 * @param varName
	 * @param querySet
	 * @return
	 */
	public static String createId4QuerySet(String varName, Set<String> querySet) {
		StringBuffer sb = new StringBuffer();
		sb.append(varName);

		for (String query : querySet) {
			if (query.startsWith("."))
				sb.append(query.substring(1));
			else
				sb.append(query);

		}
		sb.append("-");
		sb.append(RandomIdGenerator.getId());
		return sb.toString();
	}

	/**
	 * Test whether exists a single querySet message over variable var between
	 * participant n and participant r
	 * 
	 * @param pn
	 *            The participant of the current node
	 * @param pr
	 *            The participant of the current reader
	 * @return true if exists, false otherwise.
	 */
	protected boolean existSQMsgBetween(Participant pn, Participant pr) {
		if (pn == null || pr == null)
			throw new NullPointerException();

		// try to get the message from the wsdl of participant pr
		Definition defn = participant2wsdl.get(pr.getName());
		StringBuffer sqMsgNameSb = new StringBuffer();
		sqMsgNameSb.append(pn.getName());
		sqMsgNameSb.append(pr.getName());
		sqMsgNameSb.append(var.getName());
		sqMsgNameSb.append("SQMessage");
		QName msgQName = new QName(defn.getTargetNamespace(), sqMsgNameSb.toString());
		Message msg = MyWSDLUtil.resolveMessage(defn, msgQName);
		return (msg == null) ? false : true;
	}

	/**
	 * test whether a porttype exists between participant n and participant r
	 * 
	 * @param pn
	 *            The participant of the current node
	 * @param pr
	 *            The participant of the current reader
	 * @return true if exists, false otherwise.
	 */
	public boolean existPortTypeFor(Participant pn, Participant pr) {
		if (pn == null || pr == null)
			throw new NullPointerException();

		Definition defn = participant2wsdl.get(pr.getName());
		StringBuffer ptName = new StringBuffer();
		ptName.append(pn.getName());
		ptName.append(pr.getName());
		ptName.append("PT");
		QName ptQName = new QName(defn.getTargetNamespace(), ptName.toString());
		PortType pt = MyWSDLUtil.resolvePortType(defn, ptQName);
		return (pt == null) ? false : true;
	}

	/**
	 * test whether a partnerLinkType exists between participant n and
	 * participant r
	 * 
	 * @param pn
	 *            The participant of the current node
	 * @param pr
	 *            The participant of the current reader
	 * @return true if exists, false otherwise
	 */
	public boolean existPartnerLinkTypeFor(Participant pn, Participant pr) {
		if (pn == null || pr == null)
			throw new NullPointerException();

		Definition defn = participant2wsdl.get(pr.getName());
		StringBuffer pltName = new StringBuffer();
		pltName.append(pn.getName());
		pltName.append(pr.getName());
		pltName.append("PLT");
		PartnerLinkType plt = MyWSDLUtil.findPartnerLinkType(defn, pltName.toString());
		return (plt == null) ? false : true;
	}

	/**
	 * Test whether the partnerLink exists between the participant n and
	 * participant r
	 * 
	 * @param pn
	 *            The participant of the current node
	 * @param pr
	 *            The participant of the current reader
	 * @return true if exists, false otherwise
	 */
	public boolean existPartnerLinkBetween(Participant pn, Participant pr) {
		if (pn == null || pr == null)
			throw new NullPointerException();

		Process procN = participant2fragProc.get(pn.getName());
		Process procR = participant2fragProc.get(pr.getName());
		StringBuffer plname = new StringBuffer();
		plname.append(pn.getName());
		plname.append(pr.getName());
		plname.append("PL");
		PartnerLink pln = MyBPELUtils.getPartnerLink(procN, plname.toString());
		PartnerLink plr = MyBPELUtils.getPartnerLink(procR, plname.toString());

		boolean exist = (pln == null || plr == null) ? false : true;
		return exist;
	}

	/**
	 * Create the prerequisite Message for Single QuerySet and add it into the
	 * WSDL definition of the reader participant.
	 * <p>
	 * The message consists three parts: (1) status, (2) any, (3) correlation.
	 * 
	 * @param pn
	 *            Participant of the node
	 * @param pr
	 *            Participant of the reader
	 * @see The naming convention documented in Development Manual Section 5.8.7
	 */
	protected void createPrerequisiteSQMessageDiffPartition(Participant pn, Participant pr) {
		if (pn == null || pr == null)
			throw new NullPointerException();

		Definition dfn = participant2wsdl.get(pr.getName());
		StringBuffer sb = new StringBuffer();
		sb.append(pn.getName());
		sb.append(pr.getName());
		sb.append(var.getName());
		sb.append("SQMessage");

		QName qname = new QName(dfn.getTargetNamespace(), sb.toString());

		Message msg = WSDLFactory.eINSTANCE.createMessage();
		msg.setQName(qname);

		// part for status
		Part status = WSDLFactory.eINSTANCE.createPart();
		Part data = WSDLFactory.eINSTANCE.createPart();
		status.setName("status");
		status.setTypeName(new QName(XSDConstants.SCHEMA_FOR_SCHEMA_URI_2001, "boolean", "xsd"));
		msg.addPart(status);

		// part for data
		data.setName("data");
		data.setTypeName(new QName(XSDConstants.SCHEMA_FOR_SCHEMA_URI_2001, "any", "xsd"));
		msg.addPart(data);

		// part for correlation
		Part correlationPart = FragmentFactory.createMessagePart4Correlation();
		msg.addPart(correlationPart);

		dfn.addMessage(msg);

		// propertyAlias that points to the correlation property to the
		// correlation part in the message
		Property correlProperty = MyWSDLUtil.findProperty(dfn,
				SplitProcessConstants.CORRELATION_PROPERTY_NAME);

		PropertyAlias propertyAlias = MessagepropertiesFactory.eINSTANCE.createPropertyAlias();
		propertyAlias.setPropertyName(correlProperty);
		propertyAlias.setMessageType(msg);
		propertyAlias.setPart(SplitProcessConstants.CORRELATION_PART_NAME);
		dfn.addExtensibilityElement(propertyAlias);

	}

	/**
	 * Create message that has more than one status parts, because of the
	 * multiple querySets.
	 * <p>
	 * The message consists of at least three parts: (1) one or more status
	 * part, (2) one "any" part, (3) one "correlation" part.
	 * 
	 * @param pn
	 *            The participant of the current node
	 * @param pr
	 *            The participant of the current reader
	 * @param qws
	 *            The tuple set of Query set and Writer Set
	 * @param id
	 *            The map of query set to unique string
	 * @param idn
	 *            The map of PWDG node to unique string
	 * @see The naming convention documented in Development Manual Section 5.8.7
	 */
	protected void createPrerequisiteMQMessageDiffPartition(Participant pn, Participant pr,
			QueryWriterSet qws, Map<Set<String>, String> id, String idn) {

		if (pn == null || pr == null || qws == null || id == null || idn == null)
			throw new NullPointerException();

		Definition dfn = participant2wsdl.get(pr.getName());

		// message name
		StringBuffer sb = new StringBuffer();
		sb.append(pn.getName());
		sb.append(pr.getName());
		sb.append(var.getName());
		sb.append(idn);
		sb.append("MQMessage");

		QName qname = new QName(dfn.getTargetNamespace(), sb.toString());
		Message msg = WSDLFactory.eINSTANCE.createMessage();
		msg.setQName(qname);

		// parts for status
		for (Set<String> qs : qws.querySets()) {
			// each querySet one status part
			Part status = WSDLFactory.eINSTANCE.createPart();
			status.setTypeName(new QName(XSDConstants.SCHEMA_FOR_SCHEMA_URI_2001, "boolean", "xsd"));
			status.setName("status" + id.get(qs));
			msg.addPart(status);
		}

		// part for data
		Part data = WSDLFactory.eINSTANCE.createPart();
		data.setTypeName(new QName(XSDConstants.SCHEMA_FOR_SCHEMA_URI_2001, "any", "xsd"));
		data.setName("data");
		msg.addPart(data);

		// part for correlation
		Part correlationPart = FragmentFactory.createMessagePart4Correlation();
		msg.addPart(correlationPart);

		dfn.addMessage(msg);

		// propertyAlias that points to the correlation property to the
		// correlation part in the message
		Property correlProperty = MyWSDLUtil.findProperty(dfn,
				SplitProcessConstants.CORRELATION_PROPERTY_NAME);

		PropertyAlias propertyAlias = MessagepropertiesFactory.eINSTANCE.createPropertyAlias();
		propertyAlias.setPropertyName(correlProperty);
		propertyAlias.setMessageType(msg);
		propertyAlias.setPart(SplitProcessConstants.CORRELATION_PART_NAME);

		dfn.addExtensibilityElement(propertyAlias);
	}

	/**
	 * Create the prerequisite messages that are necessary for creating local
	 * resolver and receiving flow
	 * 
	 * @param n
	 *            The PWDG Node
	 * @param qws
	 *            The tuples of query set and writer set
	 * @param id
	 *            The map of query set to unique string
	 * @param idn
	 *            The map of PWDG node to unique string
	 */
	protected void createPrerequisiteMessage(PWDGNode n, QueryWriterSet qws,
			Map<Set<String>, String> id, Map<PWDGNode, String> idn) {

		if (n == null || qws == null || id == null || idn == null)
			throw new NullPointerException();

		Participant pn = partitionSpec.getParticipant(n.getParticipant());
		Participant pr = partitionSpec.getParticipant(this.act);

		if (pn.getName().equals(pr.getName())) {
			// same partition, multiple query sets
			createPrerequisiteMQSamePartitionMessage(pr, n, id, idn, qws);
		} else {
			// different partition
			if (qws.size() == 1) {
				if (!existSQMsgBetween(pn, pr)) {
					createPrerequisiteSQMessageDiffPartition(pn, pr);
				}
			} else if (qws.size() > 1) {
				createPrerequisiteMQMessageDiffPartition(pn, pr, qws, id, idn.get(n));
			}
		}
	}

	/**
	 * Create prerequisite message for the case same partition and multiple
	 * query sets, which contains one status part for each query set, and the
	 * data parts from the original variable.
	 * 
	 * @param pr
	 *            The reader participant
	 * @param n
	 *            The PWDG node
	 * @param idn
	 *            The node to name map
	 */
	protected void createPrerequisiteMQSamePartitionMessage(Participant pr, PWDGNode n,
			Map<Set<String>, String> id, Map<PWDGNode, String> idn, QueryWriterSet qws4Node) {

		Definition dfn = participant2wsdl.get(pr.getName());

		// message name
		StringBuffer sb = new StringBuffer();
		sb.append(pr.getName());
		sb.append(pr.getName());
		sb.append(var.getName());
		sb.append(idn.get(n));
		sb.append("MQMessage");

		QName qname = new QName(dfn.getTargetNamespace(), sb.toString());
		Message msg = WSDLFactory.eINSTANCE.createMessage();
		msg.setQName(qname);

		// parts for status
		for (Set<String> qs : qws4Node.querySets()) {
			// each querySet one status part
			Part status = WSDLFactory.eINSTANCE.createPart();
			status.setTypeName(new QName(XSDConstants.SCHEMA_FOR_SCHEMA_URI_2001, "boolean", "xsd"));
			status.setName("status" + id.get(qs));
			msg.addPart(status);
		}

//		// part for data
//		Message varMessage = var.getMessageType();
//		for (Object obj : varMessage.getParts().values()) {
//			Part origPart = (Part) obj;
//			Part data = FragmentDuplicator.copyPart(origPart);
//			msg.addPart(data);
//		}
		
		dfn.addMessage(msg);

	}

	/**
	 * Create prerequisite PortType
	 * 
	 * @param pn
	 *            Participant of the PWDG node
	 * @param pr
	 *            Participant of the reader
	 */
	protected void createPrerequisitePortType(Participant pn, Participant pr) {
		if (pn == null || pr == null)
			throw new NullPointerException();

		Definition dfn = participant2wsdl.get(pr.getName());

		StringBuffer sb = new StringBuffer();
		sb.append(pn.getName());
		sb.append(pr.getName());
		sb.append("PT");

		QName qname = new QName(dfn.getTargetNamespace(), sb.toString());

		PortType pt = WSDLFactory.eINSTANCE.createPortType();
		pt.setQName(qname);

		dfn.addPortType(pt);
	}

	/**
	 * Create prerequisite operation for the message
	 * 
	 * @param pn
	 *            Participant of the PWDG node
	 * @param pr
	 *            Participant of the reader
	 * @param msg
	 *            The message
	 * @param idn
	 *            The map of PWDG node to unique string
	 * @param pt
	 *            The portType
	 */
	protected void createPrerequisiteOperation(Participant pn, Participant pr, Message msg,
			String idn, PortType pt) {
		if (pn == null || pr == null || msg == null || idn == null || pt == null)
			throw new NullPointerException();

		StringBuffer sb = new StringBuffer();
		sb.append(pn.getName());
		sb.append(pr.getName());
		sb.append(var.getName());
		sb.append(idn);
		sb.append("OP");
		Operation op = WSDLFactory.eINSTANCE.createOperation();
		op.setName(sb.toString());

		Input input = WSDLFactory.eINSTANCE.createInput();
		StringBuffer sb2 = new StringBuffer();
		sb2.append(pn.getName());
		sb2.append(pr.getName());
		sb2.append(var.getName());
		sb2.append(idn);
		sb2.append("Input");
		input.setName(sb2.toString());
		input.setMessage(msg);
		op.setInput(input);

		pt.addOperation(op);

		logger.debug("Operation: " + op.getName() + " created");
	}

	/**
	 * Create prerequisites portType and operation, if the portType does not
	 * exist, create it. Each time there will be an operation get created.
	 * 
	 * @param pn
	 *            The participant of the current node
	 * @param pr
	 *            The participant of the current reader
	 * @param n
	 *            The current pwdg node from where the message is sent
	 * @param qws1
	 *            The querySet to WriterSet map
	 * @param idn
	 *            The pwdg node to unique id string map
	 */
	protected void createPrerequisitePortTypeOperation(Participant pn, Participant pr, PWDGNode n,
			QueryWriterSet qws1, Map<PWDGNode, String> idn) {

		if (pn == null || pr == null || n == null || qws1 == null || idn == null)
			throw new NullPointerException();

		if (!existPortTypeFor(pn, pr))
			createPrerequisitePortType(pn, pr);

		PortType pt = getPortTypeFor(pn, pr);
		boolean isSQMsg = (qws1.size() == 1);
		Message msg = getMessageFor(pn, pr, n, isSQMsg, idn);
		createPrerequisiteOperation(pn, pr, msg, idn.get(n), pt);
	}

	/**
	 * Create partnerLinkType for the two participant of current pwdg node and
	 * participant of reader.
	 * 
	 * @param pn
	 *            The participant of the current node
	 * @param pr
	 *            The participant of the current reader
	 */
	protected void createPrerequisitePartnerLinkType(Participant pn, Participant pr) {
		if (pn == null || pr == null)
			throw new NullPointerException();

		if (!existPartnerLinkTypeFor(pn, pr)) {
			Definition dfn = participant2wsdl.get(pr.getName());
			StringBuffer sb = new StringBuffer();
			sb.append(pn.getName());
			sb.append(pr.getName());
			sb.append("PLT");
			PartnerLinkType plt = PartnerlinktypeFactory.eINSTANCE.createPartnerLinkType();
			plt.setName(sb.toString());

			StringBuffer sb2 = new StringBuffer();
			sb2.append(pn.getName());
			sb2.append(pr.getName());
			sb2.append("ROLE");
			Role role = PartnerlinktypeFactory.eINSTANCE.createRole();
			role.setName(sb2.toString());

			PortType pt = getPortTypeFor(pn, pr);

			role.setPortType(pt);
			plt.getRole().add(role);

			dfn.addExtensibilityElement(plt);
		}
	}

	/**
	 * Create partnerLink between the two participants of current pwdg node and
	 * reader
	 * 
	 * @param pn
	 *            The participant of the current node
	 * @param pr
	 *            The participant of the current reader
	 */
	protected void createPrerequisitePartnerLink(Participant pn, Participant pr) {
		if (pn == null || pr == null)
			throw new NullPointerException();

		if (!existPartnerLinkBetween(pn, pr)) {
			Process procn = participant2fragProc.get(pn.getName());
			Process procr = participant2fragProc.get(pr.getName());

			PartnerLink pl4procNode = BPELFactory.eINSTANCE.createPartnerLink();
			PartnerLink pl4procReader = BPELFactory.eINSTANCE.createPartnerLink();

			StringBuffer sb = new StringBuffer();
			sb.append(pn.getName());
			sb.append(pr.getName());
			sb.append("PL");
			pl4procNode.setName(sb.toString());
			pl4procReader.setName(sb.toString());

			Definition defn = participant2wsdl.get(pr.getName());
			StringBuffer sb1 = new StringBuffer();
			sb1.append(pn.getName());
			sb1.append(pr.getName());
			sb1.append("PLT");
			QName qname = new QName(defn.getTargetNamespace(), sb1.toString());
			PartnerLinkType plt = MyWSDLUtil.resolveBPELPartnerLinkType(defn, qname);

			StringBuffer sb2 = new StringBuffer();
			sb2.append(pn.getName());
			sb2.append(pr.getName());
			sb2.append("ROLE");
			Role role = MyWSDLUtil.findRole(plt, sb2.toString());

			pl4procReader.setPartnerLinkType(plt);
			pl4procReader.setMyRole(role);
			procr.getPartnerLinks().getChildren().add(pl4procReader);

			pl4procNode.setPartnerLinkType(plt);
			pl4procNode.setPartnerRole(role);
			procn.getPartnerLinks().getChildren().add(pl4procNode);

		}
	}

	/**
	 * Get portType for the invoking participant and invoked participant
	 * 
	 * @param pn
	 *            The participant of the current node
	 * @param pr
	 *            The participant of the current reader
	 * @return
	 */
	protected PortType getPortTypeFor(Participant pn, Participant pr) {
		if (pn == null || pr == null)
			throw new NullPointerException();

		Definition defn = participant2wsdl.get(pr.getName());

		StringBuffer ptname = new StringBuffer();
		ptname.append(pn.getName());
		ptname.append(pr.getName());
		ptname.append("PT");

		QName qname = new QName(defn.getTargetNamespace(), ptname.toString());

		return MyWSDLUtil.resolvePortType(defn, qname);
	}

	/**
	 * Get message that get sent from invoking participant(pn) to invoked
	 * participant(pr) respecting the current pwdg node n and the current
	 * variable.
	 * 
	 * @param pn
	 *            The participant of the current node
	 * @param pr
	 *            The participant of the current reader
	 * @param n
	 *            The current pwdg node from where the message is sent
	 * @param isSQMsg
	 *            Whether the message is single querySet
	 * @param idn
	 *            The pwdg node to unique id string map
	 * @param dfn
	 *            The WSDL definition of the invoked participant(pr)
	 * @return
	 */
	protected Message getMessageFor(Participant pn, Participant pr, PWDGNode n, boolean isSQMsg,
			Map<PWDGNode, String> idn) {

		if (pn == null || pr == null || idn == null)
			throw new NullPointerException();

		Definition dfn = participant2wsdl.get(pr.getName());

		StringBuffer sb = new StringBuffer();
		Message msg = null;

		if (isSQMsg) {
			sb.append(pn.getName());
			sb.append(pr.getName());
			sb.append(var.getName());
			sb.append("SQMessage");
		} else {
			sb.append(pn.getName());
			sb.append(pr.getName());
			sb.append(var.getName());
			sb.append(idn.get(n));
			sb.append("MQMessage");
		}
		QName qname = new QName(dfn.getTargetNamespace(), sb.toString());
		msg = MyWSDLUtil.resolveMessage(dfn, qname);
		return msg;
	}
}
