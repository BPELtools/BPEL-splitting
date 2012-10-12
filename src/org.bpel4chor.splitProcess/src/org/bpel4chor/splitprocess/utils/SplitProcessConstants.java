package org.bpel4chor.splitprocess.utils;


/**
 * Constants for all
 * 
 * @since Feb 10, 2012
 * @author Daojun Cui
 */
public class SplitProcessConstants {

	public static final boolean DEBUG = false;

	/** C:\tmp\BPEL4Chor\ **/
	public static final String DEFAULT_SPLITTING_OUTPUT_DIR = "C:\\tmp\\BPEL4Chor\\";

	public static final String DEFAULT_PREFIX = "tns";

	public static final String PREFIX_WSDL = "wsdl";

	public static final String PREFIX_SOAP = "soap";

	public static final String PREFIX_SCHEMA = "xsd";

	public static final String PREFIX_VPROP = "vprop";
	
	public static final String PREFIX_PLNK = "plnk";

	/** http://schemas.xmlsoap.org/wsdl/ */
	public static final String NAMESPACE_WSDL = "http://schemas.xmlsoap.org/wsdl/";

	/** http://schemas.xmlsoap.org/wsdl/soap */
	public static final String NAMESPACE_SOAP = "http://schemas.xmlsoap.org/wsdl/soap";

	/** http://www.w3.org/2001/XMLSchema */
	public static final String NAMESPACE_SCHEMA = "http://www.w3.org/2001/XMLSchema";

	/** http://docs.oasis-open.org/wsbpel/2.0/varprop */
	public static final String NAMESPACE_VPROP = "http://docs.oasis-open.org/wsbpel/2.0/varprop";
	
	/** http://docs.oasis-open.org/wsbpel/2.0/plnktype */
	public static final String NAMESPACE_PLNK = "http://docs.oasis-open.org/wsbpel/2.0/plnktype";

	/** The property name for correlation in the WSDL */
	public static final String CORRELATION_PROPERTY_NAME = "correlProperty";
	
	/** The part name in the messages for correlation */
	public static final String CORRELATION_PART_NAME = "correlation";
	
	/** The correlation set name defined in the BPEL process */
	public static final String CORRELATION_SET_NAME = "CorrelationSet";
	
	public static final String VARIABLE_FOR_CORRELATION_NAME = "globalCorrel";
	
	public static final String VARIABLE_FOR_CORRELATION_TYPE = "string";

	/** General name for the control link message */
	public static final String CONTROL_LINK_MESSAGE_NAME = "controlLinkMessage";

	/** {http://www.bpel4chor.org/fragments}controlLinkMessage  */
//	public static final QName CTRL_LINK_MSG_QNAME = new QName(FRAGMENT_NAMESPACE, CONTROL_LINK_MESSAGE_NAME,
//			FRAGMENT_PREFIX);

}
