package org.bpel4chor.splitprocess.fragmentation;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.bpel4chor.splitprocess.utils.NameGenerator;
import org.bpel4chor.splitprocess.utils.SplitProcessConstants;
import org.bpel4chor.utils.FragmentDuplicator;
import org.bpel4chor.utils.MyBPELUtils;
import org.bpel4chor.utils.MyWSDLUtil;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.Condition;
import org.eclipse.bpel.model.Copy;
import org.eclipse.bpel.model.Correlation;
import org.eclipse.bpel.model.CorrelationSet;
import org.eclipse.bpel.model.Correlations;
import org.eclipse.bpel.model.Expression;
import org.eclipse.bpel.model.FaultHandler;
import org.eclipse.bpel.model.From;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.PartnerLink;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.Source;
import org.eclipse.bpel.model.Target;
import org.eclipse.bpel.model.To;
import org.eclipse.bpel.model.Variable;
import org.eclipse.bpel.model.messageproperties.Property;
import org.eclipse.wst.wsdl.Definition;
import org.eclipse.wst.wsdl.Input;
import org.eclipse.wst.wsdl.Message;
import org.eclipse.wst.wsdl.Operation;
import org.eclipse.wst.wsdl.Part;
import org.eclipse.wst.wsdl.PortType;
import org.eclipse.wst.wsdl.WSDLFactory;
import org.eclipse.xsd.util.XSDConstants;

/**
 * FragmentFactory creates all kinds of stuffs while fragmenting process,
 * control link, and data dependency.
 * 
 * @since Feb 17, 2012
 * @author Daojun Cui
 */
public class FragmentFactory {

	public static Logger logger = Logger.getLogger(FragmentFactory.class);

	/**
	 * Create control link message
	 * 
	 * <p>
	 * If the message does not exist in wsdl, add into it. If the checking for
	 * propertyAlias also will be run, if it does not exist yet, create the
	 * propertyAlias and add into the wsdl.
	 * 
	 * @return The message for control link
	 */
	public static Message createControlLinkMessage(String namespace) {

		Message ctrlLinkMessage = null;

		// we always use the same name for the message that conveys control
		// information.
		QName ctrlMsgQName = new QName(namespace, SplitProcessConstants.CONTROL_LINK_MESSAGE_NAME);

		ctrlLinkMessage = WSDLFactory.eINSTANCE.createMessage();
		ctrlLinkMessage.setQName(ctrlMsgQName);

		// part for true or false
		Part status = WSDLFactory.eINSTANCE.createPart();
		status.setName("status");
		status.setTypeName(new QName(XSDConstants.SCHEMA_FOR_SCHEMA_URI_2001, "boolean", "xsd"));
		ctrlLinkMessage.addPart(status);

		// part for correlation
		Part correlationPart = WSDLFactory.eINSTANCE.createPart();
		correlationPart.setName(SplitProcessConstants.CORRELATION_PART_NAME);
		correlationPart.setTypeName(new QName(XSDConstants.SCHEMA_FOR_SCHEMA_URI_2001, "string",
				"xsd"));
		ctrlLinkMessage.addPart(correlationPart);

		return ctrlLinkMessage;
	}

	/**
	 * Create a message for transporting data, it consists of
	 * <ol>
	 * <li>.data - anyType(mandatory)
	 * <li>.status - boolean(query writer set size == 1)
	 * <li>.statusQs1 - boolean(query writer set size > 1)
	 * <li>....
	 * <li>.statusQsn - boolean(query writer set size > 1)
	 * <li>.correlation - string(mandatory)
	 * </ol>
	 * <p>
	 * The data part will be type of 'any'. It will be parsed correctly in the
	 * receiving flow.
	 * 
	 * @param msgQName
	 *            Message QName
	 * @param statusNameArr
	 *            The status name string array
	 * @return
	 */
	public static Message createDataDependencyMessage(QName msgQName, String[] statusNameArr) {

		Message dataMessage = null;

		dataMessage = WSDLFactory.eINSTANCE.createMessage();

		// use the namespace given
		dataMessage.setQName(msgQName);

		// part for status, it can be multiple status dependent on size of query
		// writer tuples size
		for (String statusName : statusNameArr) {
			Part statusPart = WSDLFactory.eINSTANCE.createPart();
			statusPart.setName(statusName);
			statusPart.setTypeName(new QName(XSDConstants.SCHEMA_FOR_SCHEMA_URI_2001, "boolean",
					"xsd"));
			dataMessage.addPart(statusPart);
		}

		// part for data
		Part dataPart = WSDLFactory.eINSTANCE.createPart();
		dataPart.setName("data");
		dataPart.setTypeName(new QName(XSDConstants.SCHEMA_FOR_SCHEMA_URI_2001, "any", "xsd"));

		// add part
		dataMessage.addPart(dataPart);

		// part for correlation
		Part correlationPart = WSDLFactory.eINSTANCE.createPart();
		correlationPart.setName(SplitProcessConstants.CORRELATION_PART_NAME);
		correlationPart.setTypeName(new QName(XSDConstants.SCHEMA_FOR_SCHEMA_URI_2001, "string",
				"xsd"));

		// add part
		dataMessage.addPart(correlationPart);

		return dataMessage;

	}

	/**
	 * Create an empty WSDL Definition
	 * 
	 * <p>
	 * A property and properties will be pre-stored into the definition for
	 * correlation.
	 * 
	 * @param nonSplitProcess
	 * @param nonSplitProcessDefn
	 * @param fragProcessName
	 *            The associated fragment process name
	 * @return The WSDL definition for the given process
	 * 
	 * @see #createControlLinkMessage()
	 */
	public static Definition createWSDLDefinition(Process nonSplitProcess,
			Definition nonSplitProcessDefn, String fragProcessName) {

		if (nonSplitProcess == null || nonSplitProcessDefn == null || fragProcessName == null)
			throw new NullPointerException();

		Definition defn = null;
		defn = WSDLFactory.eINSTANCE.createDefinition();
		defn.setTargetNamespace(nonSplitProcessDefn.getTargetNamespace());
		defn.addNamespace(SplitProcessConstants.DEFAULT_PREFIX,
				nonSplitProcessDefn.getTargetNamespace());
		defn.addNamespace(SplitProcessConstants.PREFIX_VPROP, SplitProcessConstants.NAMESPACE_VPROP);
		defn.addNamespace(SplitProcessConstants.PREFIX_WSDL, SplitProcessConstants.NAMESPACE_WSDL);
		defn.addNamespace(SplitProcessConstants.PREFIX_SOAP, SplitProcessConstants.NAMESPACE_SOAP);
		defn.addNamespace(SplitProcessConstants.PREFIX_SCHEMA,
				SplitProcessConstants.NAMESPACE_SCHEMA);
		defn.addNamespace(SplitProcessConstants.PREFIX_PLNK, SplitProcessConstants.NAMESPACE_PLNK);

		// set the qName of process
		QName qName = new QName(defn.getTargetNamespace(), fragProcessName);
		defn.setQName(qName);

		// add property, name="correlProperty" type=string
		Property correlProperty = MyWSDLUtil.findProperty(nonSplitProcessDefn,
				SplitProcessConstants.CORRELATION_PROPERTY_NAME);
		if (correlProperty != null) {
			Property newCorrelProperty = FragmentDuplicator.copyProperty(correlProperty);
			defn.addExtensibilityElement(newCorrelProperty);
		} else {
			throw new NullPointerException("Non correlation property is found in process "
					+ fragProcessName);
		}

		// // add propertyAlias, the propertyAlias points to the part
		// "correlation"
		// // in the control link message
		// Message controlLinkMessage =
		// FragmentFactory.createControlLinkMessage(nonSplitProcessDefn
		// .getTargetNamespace());
		//
		// String partName = "correlation";
		// PropertyAlias propertyAlias =
		// MessagepropertiesFactory.eINSTANCE.createPropertyAlias();
		// propertyAlias.setPropertyName(correlProperty);
		// propertyAlias.setMessageType(controlLinkMessage);
		// propertyAlias.setPart(partName);
		// defn.addExtensibilityElement(propertyAlias);

		return defn;
	}

	/**
	 * Create variable for sending block
	 * 
	 * <p>
	 * Note that this method takes advantage of the from-spec in the BPEL
	 * Specification to initialize the variable. But this feature is only
	 * supported by the commercial work-flow engine.
	 * 
	 * @param process
	 * @param sugguestVarName
	 * @param message
	 * @param status
	 *            true or false
	 * @return
	 */
	public static Variable createSendingBlockVariable(Process process, String sugguestVarName,
			Message message, boolean status) {

		if (process == null || sugguestVarName == null || message == null)
			throw new NullPointerException("argument is null");

		Variable variable = BPELFactory.eINSTANCE.createVariable();
		NameGenerator generator = new NameGenerator(process);
		String uniqueName = generator.getUniqueVariableName(sugguestVarName);
		variable.setName(uniqueName);
		variable.setMessageType(message);
		// in-line assign with from-spec
		From from = BPELFactory.eINSTANCE.createFrom();
		// Variants of from-spec:
		// ----------------------
		// 1.
		// <from variable="BPELVariableName" part="NCName"?>
		// <query queryLanguage="anyURI"?>?
		// queryContent
		// </query>
		// </from>
		// 2.
		// <from partnerLink="NCName" endpointReference="myRole|partnerRole" />
		// 3.
		// <from variable="BPELVariableName" property="QName" />
		// 4.
		// <from expressionLanguage="anyURI"?>expression</from>
		// 5.
		// <from><literal>literal value</literal></from>
		// 6.
		// <from/>
		// FIXME, we want to copy the "false()" to the variable's part "status",
		// NOT to the whole variable.
		// Only the 5. variant provides a possibility, but one must create a
		// literal structure and copy it to the variable.
		// Furthermore, there is an issue that the Apache ODE does not support
		// the inline from-spec feature.
		//
		Expression statusExpr = BPELFactory.eINSTANCE.createExpression();
		statusExpr.setBody(status + "()");
		from.setExpression(statusExpr);
		variable.setFrom(from);

		return variable;
	}

	/**
	 * Create variable for invoke activity in the sending block
	 * 
	 * @param process
	 * @param sugguestVarName
	 * @param message
	 * @return
	 */
	public static Variable createSendingBlockVariable(Process process, String sugguestVarName,
			Message message) {

		if (process == null || sugguestVarName == null || message == null)
			throw new NullPointerException("argument is null");

		Variable variable = BPELFactory.eINSTANCE.createVariable();
		NameGenerator generator = new NameGenerator(process);
		String uniqueName = generator.getUniqueVariableName(sugguestVarName);
		variable.setName(uniqueName);
		variable.setMessageType(message);

		return variable;
	}

	/**
	 * Create variable for receiving block
	 * 
	 * @param process
	 * @param sugguestVarName
	 * @param message
	 * @return
	 */
	public static Variable createReceivingBlockVariable(Process process, String sugguestVarName,
			Message message) {
		if (process == null || sugguestVarName == null || message == null)
			throw new NullPointerException("argument is null");

		Variable variable = BPELFactory.eINSTANCE.createVariable();
		NameGenerator generator = new NameGenerator(process);
		String uniqueName = generator.getUniqueVariableName(sugguestVarName);
		variable.setName(uniqueName);
		variable.setMessageType(message);
		return variable;
	}

	/**
	 * Create operation in the definition given
	 * 
	 * @param process
	 * @param definition
	 * @param message
	 * @return
	 */
	public static Operation createOperation(Definition definition, Message message,
			String sugguestOpName) {
		if (definition == null || message == null || sugguestOpName == null)
			throw new NullPointerException("argument is null.");
		if (sugguestOpName.isEmpty())
			throw new IllegalArgumentException("sugguestOpName is empty.");

		Input input = WSDLFactory.eINSTANCE.createInput();
		input.setMessage(message);

		Operation operation = WSDLFactory.eINSTANCE.createOperation();
		NameGenerator generator = new NameGenerator(definition);
		String uniqueOpName = generator.getUniqueOperationName(sugguestOpName);
		operation.setName(uniqueOpName);
		operation.setInput(input);

		logger.debug("Create operation " + uniqueOpName);

		return operation;
	}

	/**
	 * Create port type in the definition given
	 * 
	 * @param process
	 * @param definition
	 * @param operation
	 * @param sugguestPTName
	 * @return
	 */
	public static PortType createPortType(Definition definition, Operation operation,
			String sugguestPTName) {
		if (definition == null || operation == null || sugguestPTName == null)
			throw new NullPointerException("argument is null.");
		if (sugguestPTName.isEmpty())
			throw new IllegalArgumentException("sugguestPTName is empty.");

		NameGenerator generator = new NameGenerator(definition);
		String uniquePortTypeName = generator.getUniquePortTypeName(sugguestPTName);

		PortType portType = WSDLFactory.eINSTANCE.createPortType();
		QName portTypeQName = new QName(definition.getTargetNamespace(), uniquePortTypeName);
		portType.setQName(portTypeQName);
		portType.addOperation(operation);

		logger.debug("Create portType " + portTypeQName.getLocalPart());
		return portType;
	}

	/**
	 * Create invoke that sends message
	 * 
	 * @param name
	 * @param pl
	 *            PartnerLink
	 * @param pt
	 *            PortType
	 * @param op
	 *            Operation
	 * @param inputVariable
	 * @param suppressJoinFailure
	 * @return
	 */
	public static Invoke createSendingBlockInvoke(String name, PartnerLink pl, PortType pt,
			Operation op, Variable inputVariable, boolean suppressJoinFailure) {

		if (name == null || pl == null || pt == null || op == null || inputVariable == null)
			throw new NullPointerException();

		Invoke invoke = BPELFactory.eINSTANCE.createInvoke();
		invoke.setName(name);
		invoke.setPartnerLink(pl);
		invoke.setPortType(pt);
		invoke.setOperation(op);
		invoke.setInputVariable(inputVariable);
		invoke.setSuppressJoinFailure(suppressJoinFailure);
		return invoke;

	}

	/**
	 * Create Assign that assigns the 'status' and a correlation to the variable
	 * given.
	 * 
	 * @param process
	 *            The source process
	 * @param var4StatusAndCorrel
	 *            The variable for status and correlation
	 * @return
	 */
	public static Assign createSendingBlockAssign(Process process, Variable var4StatusAndCorrel,
			boolean status) {

		if (process == null || var4StatusAndCorrel == null)
			throw new NullPointerException();

		Assign assign = BPELFactory.eINSTANCE.createAssign();

		// get it a unique name
		NameGenerator generator = new NameGenerator(process);
		String sugguestActName = "assignSendingBlockVariable";
		String assignName = generator.getUniqueActivityName(sugguestActName);
		assign.setName(assignName);

		// assign status part inside var4StatusAndCorrel
		Copy copyStatusPart = createCopy4Status(var4StatusAndCorrel, status);
		assign.getCopy().add(copyStatusPart);

		// assign global variable, in which the global correlation is stored, to
		// the correlation part
		Copy copyCorrelationPart = createCopy4Correlatoin(process, var4StatusAndCorrel);
		assign.getCopy().add(copyCorrelationPart);

		return assign;
	}

	/**
	 * Create a "Copy" that copies from global variable in the given process to
	 * the correlation part in the given variable's message.
	 * 
	 * @param process
	 * @param var
	 * @return
	 */
	public static Copy createCopy4Correlatoin(Process process, Variable var) {
		Copy copy = BPELFactory.eINSTANCE.createCopy();
		From from = BPELFactory.eINSTANCE.createFrom();
		To to = BPELFactory.eINSTANCE.createTo();

		// from global correlation variable
		Variable globalVar4Correl = MyBPELUtils.resolveVariable(
				SplitProcessConstants.VARIABLE_FOR_CORRELATION_NAME, process);
		if (globalVar4Correl == null)
			throw new NullPointerException();
		from.setVariable(globalVar4Correl);

		// to message correlation part
		Message msg = var.getMessageType();
		Part correlPart = (Part) msg.getPart(SplitProcessConstants.CORRELATION_PART_NAME);
		to.setVariable(var);
		to.setPart(correlPart);

		// return
		copy.setFrom(from);
		copy.setTo(to);
		return copy;
	}

	/**
	 * Create a "Copy" that copies the status (true or false) to the the message
	 * 'status' of the given variable.
	 * 
	 * @param var
	 * @param status
	 * @return
	 */
	public static Copy createCopy4Status(Variable var, boolean status) {
		Copy copy = BPELFactory.eINSTANCE.createCopy();
		From from = BPELFactory.eINSTANCE.createFrom();
		To to = BPELFactory.eINSTANCE.createTo();

		Expression expr = BPELFactory.eINSTANCE.createExpression();
		expr.setBody(status == true ? "true()" : "false()");
		from.setExpression(expr);

		if (var.getMessageType() == null)
			throw new IllegalStateException("message in the variable :" + var.getName()
					+ " is null.");

		Part statusPart = (Part) var.getMessageType().getPart("status");
		if (statusPart == null)
			throw new NullPointerException();
		to.setVariable(var);
		to.setPart(statusPart);

		copy.setFrom(from);
		copy.setTo(to);
		return copy;
	}

	/**
	 * Create sequence for the sending block
	 * 
	 * @param process
	 *            The process where the sequence will be added in
	 * @param assign
	 * @param invoke
	 * @param sugguestName
	 * @return
	 */
	public static Sequence createSendingBlockSequence(Process process, Assign assign,
			Invoke invoke, String sugguestName) {

		if (process == null || assign == null || invoke == null || sugguestName == null)
			throw new NullPointerException();

		Sequence sequence = BPELFactory.eINSTANCE.createSequence();
		NameGenerator generator = new NameGenerator(process);
		String uniqueName = generator.getUniqueActivityName(sugguestName);
		sequence.setName(uniqueName);
		sequence.getActivities().add(assign);
		sequence.getActivities().add(invoke);
		return sequence;
	}

	/**
	 * Create scope for the sending block
	 * 
	 * @param process
	 *            The process where the scope will be added in
	 * @param activity
	 *            The activity of scope
	 * @param fh
	 *            The Fault handler
	 * @param sugguestName
	 * @return
	 */
	public static Scope createSendingBlockScope(Process process, Activity activity,
			FaultHandler fh, String sugguestName) {
		if (process == null || activity == null || fh == null || sugguestName == null)
			throw new NullPointerException();

		Scope scope = BPELFactory.eINSTANCE.createScope();
		NameGenerator generator = new NameGenerator(process);
		String uniqueName = generator.getUniqueActivityName(sugguestName);
		scope.setName(uniqueName);
		scope.setFaultHandlers(fh);
		scope.setActivity(activity);
		return scope;
	}

	/**
	 * Create the receive activity for receiving-block
	 * 
	 * @param fragProcess
	 *            The fragment process where the receive will be added in
	 * @param fragDefn
	 *            The fragment wsdl definition
	 * @param partnerLink
	 * @param portType
	 * @param operation
	 * @param inputVariable
	 * @param sugguestName
	 * @return
	 */
	public static Receive createReceivingBlockReceive(Process fragProcess, Definition fragDefn,
			PartnerLink partnerLink, PortType portType, Operation operation,
			Variable inputVariable, String sugguestName) {
		if (fragProcess == null || partnerLink == null || portType == null || operation == null
				|| inputVariable == null || sugguestName == null)
			throw new NullPointerException();

		Receive receive = BPELFactory.eINSTANCE.createReceive();
		NameGenerator generator = new NameGenerator(fragProcess);
		String uniqueName = generator.getUniqueActivityName(sugguestName);
		receive.setName(uniqueName);
		receive.setPartnerLink(partnerLink);
		receive.setPortType(portType);
		receive.setOperation(operation);
		receive.setVariable(inputVariable);
		receive.setCreateInstance(true);

		// add correlation set in the receive activity
		if (fragProcess.getCorrelationSets() != null) {

			Correlation correlation = BPELFactory.eINSTANCE.createCorrelation();
			CorrelationSet correlSet = MyBPELUtils.resolveCorrelationSet(fragProcess,
					SplitProcessConstants.CORRELATION_SET_NAME);
			correlation.setSet(correlSet);
			correlation.setInitiate("join");

			Correlations correlations = BPELFactory.eINSTANCE.createCorrelations();
			correlations.getChildren().add(correlation);

			receive.setCorrelations(correlations);

			// // if the corresponding propertyAlias does not exist yet,
			// // add propertyAlias that points the global property to the
			// received
			// // message's "correlation" part.
			// QName propertyQName = new QName(fragDefn.getTargetNamespace(),
			// SplitProcessConstants.CORRELATION_PROPERTY_NAME);
			// QName msgQName = inputVariable.getMessageType().getQName();
			// PropertyAlias alias = MyWSDLUtil.findPropertyAlias(fragDefn,
			// propertyQName, msgQName,
			// SplitProcessConstants.CORRELATION_PART_NAME);
			// if (alias == null) {
			// alias = MessagepropertiesFactory.eINSTANCE.createPropertyAlias();
			// Property correlProperty = MyWSDLUtil.findProperty(fragDefn,
			// SplitProcessConstants.CORRELATION_PROPERTY_NAME);
			// alias.setPropertyName(correlProperty);
			// alias.setMessageType(inputVariable.getMessageType());
			// alias.setPart(SplitProcessConstants.CORRELATION_PART_NAME);
			// fragDefn.addExtensibilityElement(alias);
			// }
		}

		return receive;

	}

	/**
	 * Create link with the given source activity, target activity, and
	 * transitionCondition
	 * 
	 * @param sourceAct
	 * @param targetAct
	 * @param transitionCondition
	 * @return
	 */
	public static Link createLinkBetween(Activity sourceAct, Activity targetAct,
			boolean transitionCondition) {

		if (sourceAct == null || targetAct == null)
			throw new NullPointerException();

		Link link = BPELFactory.eINSTANCE.createLink();
		link.setName(sourceAct.getName() + "2" + targetAct.getName());
		Source source = BPELFactory.eINSTANCE.createSource();

		source.setActivity(sourceAct);
		source.setLink(link);

		Condition condition = BPELFactory.eINSTANCE.createCondition();
		condition.setBody(transitionCondition == true ? "true()" : "false()");
		source.setTransitionCondition(condition);

		Target target = BPELFactory.eINSTANCE.createTarget();
		target.setActivity(targetAct);

		link.getSources().add(source);
		link.getTargets().add(target);

		return link;
	}

	public static Link createLinkBetween(Activity sourceAct, Assign targetAct,
			String transitionCondition) {

		if (sourceAct == null || targetAct == null)
			throw new NullPointerException();

		Link link = BPELFactory.eINSTANCE.createLink();
		link.setName(sourceAct.getName() + "2" + targetAct.getName());
		Source source = BPELFactory.eINSTANCE.createSource();

		source.setActivity(sourceAct);
		source.setLink(link);

		Condition condition = BPELFactory.eINSTANCE.createCondition();
		condition.setBody(transitionCondition);
		source.setTransitionCondition(condition);

		Target target = BPELFactory.eINSTANCE.createTarget();
		target.setActivity(targetAct);

		link.getSources().add(source);
		link.getTargets().add(target);

		return link;
	}

	/**
	 * Create an assign activity that copies the correlation in the process to
	 * the global variable.
	 * <p>
	 * Note that a global variable is created for storage of the correlation as
	 * the fragment process is initialized.
	 * 
	 * @param process
	 * @param receiveVar
	 *            The incoming variable from which the assign copies
	 * @return
	 */
	public static Assign createAssign4GlobalVar(Process process, Variable receiveVar) {

		if (process == null || receiveVar == null)
			throw new NullPointerException();

		Assign assign = BPELFactory.eINSTANCE.createAssign();
		NameGenerator nameGen = new NameGenerator(process);
		String assignName = nameGen.getUniqueActivityName("Assign4GlobalVar");
		assign.setName(assignName);

		Copy copy = BPELFactory.eINSTANCE.createCopy();

		From from = BPELFactory.eINSTANCE.createFrom();
		To to = BPELFactory.eINSTANCE.createTo();

		// copy from the inputVariable
		from.setVariable(receiveVar);

		// Copy from the property
		// we have only one correlation set and only one property
		CorrelationSet correlSet = process.getCorrelationSets().getChildren().get(0);
		Property correlProperty = correlSet.getProperties().get(0);

		if (correlProperty == null)
			throw new NullPointerException();

		from.setProperty(correlProperty);
		copy.setFrom(from);

		Variable globalVar = MyBPELUtils.resolveVariable(
				SplitProcessConstants.VARIABLE_FOR_CORRELATION_NAME, process);
		to.setVariable(globalVar);
		copy.setTo(to);

		assign.getCopy().add(copy);

		return assign;
	}

	/**
	 * Create a sequence for the receive and the assign that refers the global
	 * variable.
	 * 
	 * @param process
	 * @param seqName
	 * @return
	 */
	public static Sequence createSequence4GlobalVar(Process process, String seqName) {

		if (process == null || seqName == null)
			throw new NullPointerException();

		Sequence sequence = BPELFactory.eINSTANCE.createSequence();
		NameGenerator nameGen = new NameGenerator(process);
		String sequenceName = nameGen.getUniqueActivityName(seqName);
		sequence.setName(sequenceName);
		return sequence;
	}
	
	/**
	 * Create a correlation part for message
	 * @return
	 */
	public static Part createMessagePart4Correlation() {
		Part correlationPart = WSDLFactory.eINSTANCE.createPart();
		correlationPart.setName(SplitProcessConstants.CORRELATION_PART_NAME);
		correlationPart.setTypeName(new QName(XSDConstants.SCHEMA_FOR_SCHEMA_URI_2001, "string",
				"xsd"));
		return correlationPart;
	}

}
