package org.bpel4chor.splitprocess.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.wsdl.Operation;
import javax.wsdl.PortType;

import org.bpel4chor.model.topology.impl.Topology;
import org.bpel4chor.utils.ActivityIterator;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Variable;
import org.eclipse.bpel.model.partnerlinktype.PartnerLinkType;
import org.eclipse.bpel.model.partnerlinktype.Role;
import org.eclipse.wst.wsdl.Definition;

/**
 * NameGenerator creates unique names inside the given BPEL process, WSDL
 * definition, and Topology.
 * 
 * @since Feb 13, 2012
 * @author Daojun Cui
 */
public class NameGenerator {

	protected Process process = null;

	protected Definition definition = null;

	protected Topology topology = null;

	/** name collection of the global variables */
	protected Set<String> existedVariableNames = new HashSet<String>();

	protected Set<String> existedPartnerLinkTypeNames = new HashSet<String>();

	protected Set<String> existedRoleNames = new HashSet<String>();

	protected Set<String> existedPortTypeNames = new HashSet<String>();

	protected Set<String> existedOperationNames = new HashSet<String>();

	protected Set<String> existedActivityNames = new HashSet<String>();

	protected Set<String> existedMessageLInkNames = new HashSet<String>();

	protected String format = "%03d";// 3 digits, with leading zeroes as
										// necessary

	public NameGenerator(Process process) {
		if (process == null) {
			throw new NullPointerException("argument is null");
		}
		this.process = process;
		this.initExistedVariableNames();
		this.initExistedActivityNames();
	}

	public NameGenerator(Definition definition) {
		if (definition == null)
			throw new NullPointerException("argument is null");

		this.definition = definition;
		this.initExistedOperationNames();
		this.initExistedPortTypeNames();
		this.initRoleExistedNames();
	}

	public NameGenerator(Topology topology) {
		if (topology == null)
			throw new NullPointerException();

		this.topology = topology;
		this.initMessageLinkNames();
	}

	protected void initMessageLinkNames() {

		List<org.bpel4chor.model.topology.impl.MessageLink> msgLinks = this.topology
				.getMessageLinks();

		for (org.bpel4chor.model.topology.impl.MessageLink msgLink : msgLinks) {
			if (existedMessageLInkNames.contains(msgLink.getName()) == false) {
				existedMessageLInkNames.add(msgLink.getName());
			}
		}
	}

	protected void initExistedVariableNames() {
		if (this.process.getVariables() == null)
			return;

		List<Variable> variables = this.process.getVariables().getChildren();

		for (Variable var : variables) {
			if (existedVariableNames.contains(var.getName()) == false) {
				existedVariableNames.add(var.getName());
			}
		}
	}

	protected void initExistedActivityNames() {

		if (this.process.getActivity() == null)
			return;

		ActivityIterator iterator = new ActivityIterator(this.process);

		while (iterator.hasNext()) {
			Activity act = iterator.next();
			existedActivityNames.add(act.getName());
		}
	}

	protected void initExistedPortTypeNames() {

		List<PortType> portTypeList = this.definition.getEPortTypes();

		for (PortType portType : portTypeList) {
			if (existedPortTypeNames.contains(portType.getQName().getLocalPart()) == false)
				existedPortTypeNames.add(portType.getQName().getLocalPart());
		}
	}

	protected void initExistedOperationNames() {

		List<PortType> portTypeList = this.definition.getEPortTypes();

		for (PortType portType : portTypeList) {
			List<Operation> operations = portType.getOperations();
			for (Operation op : operations) {
				if (existedOperationNames.contains(op.getName()) == false)
					existedOperationNames.add(op.getName());
			}
		}
	}

	protected void initRoleExistedNames() {
		List extElements = this.definition.getExtensibilityElements();
		for (Object e : extElements) {
			if (e instanceof PartnerLinkType) {
				PartnerLinkType plt = (PartnerLinkType) e;
				List<Role> roleList = plt.getRole();
				for (Role role : roleList) {
					if (existedRoleNames.contains(role.getName()) == false)
						existedRoleNames.add(role.getName());
				}
			}
		}
	}

	/**
	 * Create a variable name that is unique in the given process.
	 * 
	 * @param sugguestVarName
	 * @return
	 */
	public String getUniqueVariableName(String sugguestVarName) {

		if (sugguestVarName == null || sugguestVarName.isEmpty())
			throw new IllegalArgumentException("illegal argument.");

		if (!existedVariableNames.contains(sugguestVarName)) {
			existedVariableNames.add(sugguestVarName);
			return sugguestVarName;
		}

		String uniqueName = null;

		int i = 1;
		do {
			// uniqueName = uniqueName + String.format(format, i++);
			uniqueName = sugguestVarName + i;
			i++;
		} while (existedVariableNames.contains(uniqueName));
		existedVariableNames.add(uniqueName);
		return uniqueName;

	}

	/**
	 * Create an activity name that is unique in the given process
	 * 
	 * @param sugguestActName
	 * @return
	 */
	public String getUniqueActivityName(String sugguestActName) {

		if (sugguestActName == null || sugguestActName.isEmpty())
			throw new IllegalArgumentException("illegal argument");

		if (!existedActivityNames.contains(sugguestActName)) {
			existedActivityNames.add(sugguestActName);
			return sugguestActName;
		}

		String uniqueName = null;

		int i = 1;
		do {
			// uniqueName = uniqueName + String.format(format, i++);
			uniqueName = sugguestActName + i;
			i++;
		} while (existedActivityNames.contains(uniqueName));
		existedActivityNames.add(uniqueName);
		return uniqueName;
	}

	public String getUniquePortTypeName(String sugguestPTName) {
		if (sugguestPTName == null || sugguestPTName.isEmpty())
			throw new IllegalArgumentException("illegal argument");

		if (!existedPortTypeNames.contains(sugguestPTName)) {
			existedPortTypeNames.add(sugguestPTName);
			return sugguestPTName;
		}

		String uniqueName = null;

		int i = 1;
		do {
			// uniqueName = uniqueName + String.format(format, i++);
			uniqueName = sugguestPTName + i;
			i++;
		} while (existedPortTypeNames.contains(uniqueName));
		existedVariableNames.add(uniqueName);
		return uniqueName;
	}

	/**
	 * Create a operation name that is unique in the given process
	 * 
	 * @param sugguestOpName
	 * @return
	 */
	public String getUniqueOperationName(String sugguestOpName) {
		if (sugguestOpName == null || sugguestOpName.isEmpty())
			throw new IllegalArgumentException("illegal argument.");

		if (!existedOperationNames.contains(sugguestOpName)) {
			existedOperationNames.add(sugguestOpName);
			return sugguestOpName;
		}

		String uniqueName = null;
		
		int i = 1;
		do {
			// uniqueName = uniqueName + String.format(format, i++);
			uniqueName = sugguestOpName + i;
			i++;
		} while (existedOperationNames.contains(uniqueName));
		existedOperationNames.add(uniqueName);

		return uniqueName;
	}

	public String getUniqueRoleName(String sugguestRoleName) {
		if (sugguestRoleName == null || sugguestRoleName.isEmpty())
			throw new IllegalArgumentException("illegal argument.");

		if (existedRoleNames.contains(sugguestRoleName) == false) {
			existedRoleNames.add(sugguestRoleName);
			return sugguestRoleName;
		}

		String uniqueName = null;
		
		int i = 1;
		do {
			// uniqueName = uniqueName + String.format(format, i++);
			uniqueName = sugguestRoleName + i;
			i++;
		} while (existedRoleNames.contains(uniqueName));
		existedRoleNames.add(uniqueName);

		return uniqueName;
	}

	public String getUniqueTopoMsgLinkName(String sugguestMsgLinkName) {
		if (sugguestMsgLinkName == null || sugguestMsgLinkName.isEmpty())
			throw new IllegalArgumentException("illegal argument.");

		if (existedMessageLInkNames.contains(sugguestMsgLinkName) == false) {
			existedMessageLInkNames.add(sugguestMsgLinkName);
			return sugguestMsgLinkName;
		}

		String uniqueName = null;
				
		int i = 1;
		do {
			// uniqueName = uniqueName + String.format(format, i++);
			uniqueName = sugguestMsgLinkName + i;
			i++;
		} while (existedMessageLInkNames.contains(uniqueName));
		existedMessageLInkNames.add(uniqueName);

		return uniqueName;
	}
}
