package org.bpel4chor.splitprocess.test;

import java.util.List;

import javax.xml.namespace.QName;

import org.eclipse.bpel.model.CorrelationSet;
import org.eclipse.bpel.model.CorrelationSets;
import org.eclipse.bpel.model.PartnerLink;
import org.eclipse.bpel.model.Variable;
import org.eclipse.bpel.model.messageproperties.Property;
import org.eclipse.bpel.model.partnerlinktype.PartnerLinkType;
import org.eclipse.bpel.model.partnerlinktype.Role;
import org.eclipse.wst.wsdl.Input;
import org.eclipse.wst.wsdl.Message;
import org.eclipse.wst.wsdl.Operation;
import org.eclipse.wst.wsdl.Output;
import org.eclipse.wst.wsdl.PortType;

/**
 * Test tool-box for all
 * 
 * <p>
 * It provides some basic help methods.
 * 
 * @since Feb 2, 2012
 * @author Daojun Cui
 */
public class TestUtil {

	public static final boolean EQUAL = true;
	public static final boolean NOT_EQUAL = false;
	public static final boolean CONTAIN = true;
	public static final boolean NOT_CONTAIN = false;

	public static boolean isEqual(QName expect, QName actual) {

		if (expect != null && actual != null) {
			if (!isEqual(expect.getPrefix(), actual.getPrefix()))
				return NOT_EQUAL;
			if (!isEqual(expect.getLocalPart(), actual.getLocalPart()))
				return NOT_EQUAL;
			if (!isEqual(expect.getNamespaceURI(), actual.getNamespaceURI()))
				return NOT_EQUAL;
			return EQUAL;
		} else if (expect == null && actual == null) {
			return EQUAL;
		} else {
			return NOT_EQUAL;
		}
	}

	public static boolean isEqual(String strName1, String strName2) {
		return (strName1 == null || strName2 == null) ? false : strName1.equals(strName2);
	}

	public static boolean isEqualPropertyList(List<Property> propList1, List<Property> propList2) {

		if (propList1 != null && propList2 != null) {
			for (Property expect : propList1) {
				if (!contains(propList2, expect)) 
					return NOT_EQUAL; 
			}
			for(Property expect : propList2) {
				if (!contains(propList1, expect)) 
					return NOT_EQUAL;
			}
			return EQUAL;
		} else if (propList1 == null && propList2 == null) {
			return EQUAL;
		} else {
			return NOT_EQUAL;
		}

	}

	public static boolean isEqual(PartnerLinkType plt1, PartnerLinkType plt2) {

		if (plt1 != null && plt2 != null) {
			if (!plt1.getName().equals(plt2.getName()))
				return NOT_EQUAL;
			if (!isEqualRoleList(plt1.getRole(), plt2.getRole()))
				return NOT_EQUAL;
			return EQUAL;
		} else if (plt1 == null && plt2 == null) {
			return EQUAL;
		} else {
			return NOT_EQUAL;
		}
	}

	public static boolean isEqualRoleList(List<Role> roleList1, List<Role> roleList2) {

		if (roleList1 != null && roleList2 != null) {
			for (Role role1 : roleList1) {
				if (!contains(roleList2, role1)) {
					return NOT_EQUAL;
				}
			}
			return EQUAL;
		} else if (roleList1 == null && roleList2 == null) {
			return EQUAL;
		} else {
			return NOT_EQUAL;
		}
	}

	/** Whether role is contained in the list of role */
	public static boolean contains(List<Role> roleList, Role role) {

		if (roleList == null || role == null)
			return NOT_CONTAIN;

		for (Role r : roleList) {
			if (isEqual(r, role))
				return CONTAIN;
		}
		return NOT_CONTAIN;
	}

	public static boolean isEqual(Role r1, Role r2) {

		if (r1 != null && r2 != null) {
			if (!isEqual(r1.getName(), r2.getName())) {
				return NOT_EQUAL;
			}
			if (!isEqual((PortType) r1.getPortType(), (PortType) r2.getPortType())) {
				return NOT_EQUAL;
			}
			return EQUAL;

		} else if (r1 == null && r2 == null) {
			return EQUAL;
		} else {
			return NOT_EQUAL;
		}

	}

	public static boolean isEqual(PortType pt1, PortType pt2) {

		if (pt1 != null && pt2 != null) {
			if (!isEqual(pt1.getQName(), pt2.getQName())) {
				return NOT_EQUAL;
			}
			if (!isEqualOps(pt1.getEOperations(), pt2.getEOperations())) {
				return NOT_EQUAL;
			}
			return EQUAL;
		} else if (pt1 == null && pt2 == null) {
			return EQUAL;
		} else {
			return NOT_EQUAL;
		}

	}

	public static boolean isEqualOps(List<Operation> ops1, List<Operation> ops2) {

		if (ops1 != null && ops2 != null) {
			for (Operation op : ops1) {
				if (!contains(ops2, op))
					return NOT_EQUAL;
			}
			return EQUAL;
		} else if (ops1 == null && ops2 == null) {
			return EQUAL;
		} else {
			return NOT_EQUAL;
		}

	}

	/** Test whether the opration is contained in the list of operations */
	public static boolean contains(List<Operation> ops, Operation op) {

		if (ops == null || op == null)
			return NOT_CONTAIN;

		for (Operation op1 : ops) {
			if (isEqual(op1, op))
				return CONTAIN;
		}
		return NOT_CONTAIN;

	}

	public static boolean isEqual(Operation op1, Operation op2) {

		if (op1 != null && op2 != null) {

			if (!isEqual(op1.getName(), op2.getName()))
				return NOT_EQUAL;
			if (!isEqual(op1.getEInput(), op2.getEInput()))
				return NOT_EQUAL;
			if (!isEqual(op1.getEOutput(), op2.getEOutput()))
				return NOT_EQUAL;
			return EQUAL;

		} else if (op1 == null && op2 == null) {
			return EQUAL;
		} else {
			return NOT_EQUAL;
		}

	}

	public static boolean isEqual(Input input1, Input input2) {
		if (input1 != null && input2 != null) {
			if (input1.equals(input2))
				return EQUAL;
			else
				return NOT_EQUAL;
		} else if (input1 == null && input2 == null) {
			return EQUAL;
		} else {
			return NOT_EQUAL;
		}
	}

	public static boolean isEqual(Output output1, Output output2) {
		if (output1 != null && output2 != null) {
			if (output1.equals(output2))
				return EQUAL;
			else
				return NOT_EQUAL;
		} else if (output1 == null && output2 == null) {
			return EQUAL;
		} else {
			return NOT_EQUAL;
		}
	}

	public static boolean isEqual(Property p1, Property p2) {

		if (p1 != null && p2 != null) {
			if (!isEqual(p1.getName(), p2.getName())) {
				return NOT_EQUAL;
			}
			if (p1.getType() != null) {
				if (!p1.getType().equals(p2.getType()))
					return NOT_EQUAL;
			}
//			if (!isEqual(p1.getQName(), p2.getQName())) {
//				return NOT_EQUAL;
//			}
			return EQUAL;
		} else if (p1 == null && p2 == null) {
			return EQUAL;
		} else {
			return NOT_EQUAL;
		}
	}

	/** Whether the property is contained in the properties list */
	public static boolean contains(List<Property> properties, Property expect) {

		for (Property property : properties) {
			if (isEqual(expect, property)) {
				return CONTAIN;
			}
		}

		return NOT_CONTAIN;
	}

	/** Whether the partnerlink is contained in the list */
	public static boolean contains(List<PartnerLink> partnerLinks, PartnerLink pl) {

		for (PartnerLink partnerLink : partnerLinks) {
			if (isEqual(pl, partnerLink)) {
				return CONTAIN;
			}
		}
		return NOT_CONTAIN;
	}

	public static boolean isEqual(PartnerLink pl1, PartnerLink pl2) {

		if (pl1 != null && pl2 != null) {
			if (!isEqual(pl1.getName(), (pl2.getName())))
				return NOT_EQUAL;
			if (!isEqual(pl1.getPartnerRole(), pl2.getPartnerRole()))
				return NOT_EQUAL;
			if (!isEqual(pl1.getMyRole(), pl2.getMyRole()))
				return NOT_EQUAL;
			if (!isEqual(pl1.getPartnerLinkType(), pl2.getPartnerLinkType()))
				return NOT_EQUAL;
			return EQUAL;
		} else if (pl1 == null && pl2 == null) {
			return EQUAL;
		} else {
			return NOT_EQUAL;
		}

	}

	public static boolean isEqual(Variable var1, Variable var2) {
		if (var1 != null && var2 != null) {
			if (!isEqual(var1.getName(), var2.getName()))
				return NOT_EQUAL;
			if (!isEqual(var1.getMessageType(), var2.getMessageType()))
				return NOT_EQUAL;
			// TODO
			return EQUAL;
		} else if (var1 == null && var2 == null) {
			return EQUAL;
		} else {
			return NOT_EQUAL;
		}
	}

	public static boolean isEqual(Message msg1, Message msg2) {
		if (msg1 != null && msg2 != null) {
			return msg1.equals(msg2);
		} else if (msg1 == null && msg2 == null) {
			return EQUAL;
		} else {
			return NOT_EQUAL;
		}
	}

	public static boolean isEqual(CorrelationSets cls1, CorrelationSets cls2) {
		if (cls1 != null && cls2 != null) {
			return isEqual(cls1.getChildren(), cls2.getChildren());
		} else if (cls1 == null && cls2 == null) {
			return EQUAL;
		} else {
			return NOT_EQUAL;
		}
	}

	public static boolean isEqual(List<CorrelationSet> cs1, List<CorrelationSet> cs2) {
		if (cs1 != null && cs2 != null) {
			for (CorrelationSet cs : cs1) {
				if (!contains(cs2, cs))
					return NOT_EQUAL;
			}
			return EQUAL;
		} else if (cs1 == null && cs2 == null) {
			return EQUAL;
		} else {
			return NOT_EQUAL;
		}
	}

	public static boolean contains(List<CorrelationSet> csList, CorrelationSet cs) {
		if (csList == null || cs == null)
			return NOT_CONTAIN;
		for (CorrelationSet next : csList) {
			if (isEqual(next, cs))
				return CONTAIN;
		}
		return NOT_CONTAIN;
	}

	public static boolean isEqual(CorrelationSet cs1, CorrelationSet cs2) {
		if (cs1 != null && cs2 != null) {
			if (!isEqual(cs1.getName(), cs2.getName()))
				return NOT_EQUAL;
			
			if (!isEqualPropertyList(cs1.getProperties(), cs2.getProperties()))
				return NOT_EQUAL;
			return EQUAL;

		} else if (cs1 == null && cs2 == null) {
			return EQUAL;
		} else {
			return NOT_EQUAL;
		}
	}
	
}

