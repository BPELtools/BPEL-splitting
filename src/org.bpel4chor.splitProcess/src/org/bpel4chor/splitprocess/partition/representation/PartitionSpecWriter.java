package org.bpel4chor.splitprocess.partition.representation;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.bpel4chor.splitprocess.partition.model.Participant;
import org.bpel4chor.splitprocess.partition.model.PartitionSpecification;
import org.eclipse.bpel.model.Activity;
import org.eclipse.emf.ecore.EObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Writer for Partition Specification
 * 
 * <p>
 * <b>changeLog date user remark</b> <br>
 * 
 * @001 2011-12-03 DC initial version <br>
 * 
 * @since Dec 3, 2011
 * @author Daojun Cui
 */
public class PartitionSpecWriter {

	/**
	 * Write partitionSpecification to xml file with DOM
	 * 
	 * @param partitionSpec
	 * @param outputStream
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public void writeSpecification(PartitionSpecification partitionSpec, OutputStream outputStream) throws IOException,
			ParserConfigurationException {

		//DOMImplementationRegistry registry;

		// build the document
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.newDocument();

		// node partitionSpecification
		Element root = document.createElement("partitionSpecification");

		// node participant
		for (Participant participant : partitionSpec.getParticipants()) {
			Element pElem = document.createElement("participant");
			pElem.setAttribute("name", participant.getName());
			for (Activity act : participant.getActivities()) {
				// node pActivity
				Element actElem = document.createElement("activity");
				actElem.setAttribute("path", getXPath(act));
				pElem.appendChild(actElem);
			}
			root.appendChild(pElem);
		}

		document.appendChild(root);

		// serialize DOM tree
		OutputFormat fmt = new OutputFormat(document);
		fmt.setIndenting(true);
		fmt.setIndent(4);

		XMLSerializer serializer = new XMLSerializer(outputStream, fmt);
		serializer.serialize(document);

	}

	/**
	 * Create xpath of the activity given
	 * 
	 * @param act
	 *            Activity
	 * @return XPath String of the activity
	 */
	protected String getXPath(Activity act) {
		StringBuffer path = new StringBuffer();
		path.insert(0, "/bpel:" + act.getElement().getLocalName() + "[@name='" + act.getName() + "']");

		EObject container = act.eContainer();
		while (container.eContainer() != null) {
			
			if (container instanceof Activity) {
				Activity containActivity = (Activity) container;
				path.insert(0,"/bpel:" + containActivity.getElement().getLocalName() + "[@name='" + containActivity.getName()+ "']");
			}
			container = container.eContainer();
		}
		path.insert(0, "/bpel:process");		
		return path.toString();
	}
}
