package org.bpel4chor.splitprocess.partition.representation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.bpel4chor.splitprocess.exceptions.ActivityNotFoundException;
import org.bpel4chor.splitprocess.exceptions.PartitionSpecificationException;
import org.bpel4chor.splitprocess.partition.model.Participant;
import org.bpel4chor.splitprocess.partition.model.PartitionSpecification;
import org.bpel4chor.splitprocess.partition.util.BPELNamespaceContext;
import org.bpel4chor.splitprocess.partition.util.PartitionVerificator;
import org.bpel4chor.splitprocess.utils.ActivityFinder;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Process;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * PartitionSpecReader parses partition.xml and looks up the activities in BPEL
 * process, then save them into corresponding participant.
 * 
 * @since Dec 3, 2011
 * @author Daojun Cui
 */
public class PartitionSpecReader {

	/**
	 * Read in partition file and return the partition specification.
	 * 
	 * @param partitionFilePath
	 * @param process
	 * @return
	 * @throws FileNotFoundException
	 * @throws PartitionSpecificationException
	 * @see #readSpecification(InputStream, Process)
	 */
	public PartitionSpecification readSpecification(String partitionFilePath, Process process)
			throws FileNotFoundException, PartitionSpecificationException {
		File workingDir = new File("");
		FileInputStream inputStream = new FileInputStream(new File(partitionFilePath));
		return readSpecification(inputStream, process);
	}

	/**
	 * Read in partition file and return the partition specification.
	 * <p>
	 * It parses xml file into DOM and looks up the activities, then
	 * saves them into corresponding participant, result will be a
	 * partitionSpecification
	 * 
	 * @param inputStream
	 * @return the partitionSpecification, or <tt>null</tt> in case of error.
	 * @throws PartitionSpecificationException
	 */
	public PartitionSpecification readSpecification(InputStream inputStream, Process process)
			throws PartitionSpecificationException {

		PartitionSpecification partitionSpec = new PartitionSpecification();

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(inputStream);
			Element rootElement = document.getDocumentElement();

			// participants
			NodeList pNodeList = rootElement.getChildNodes();
			for (int i = 0; i < pNodeList.getLength(); i++) {
				if (pNodeList.item(i).getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				Element participantElement = (Element) pNodeList.item(i);
				Participant participant = new Participant();

				if (participantElement.getTagName().equals("participant")) {
					participant.setName(participantElement.getAttribute("name"));

					// activities in the partition
					NodeList actNodeList = participantElement.getChildNodes();
					for (int j = 0; j < actNodeList.getLength(); j++) {
						if (actNodeList.item(j).getNodeType() != Node.ELEMENT_NODE) {
							continue;
						}
						Element actElement = (Element) actNodeList.item(j);
						String path = actElement.getAttribute("path");

						if (path == null || path.isEmpty())
							throw new PartitionSpecificationException(
									"activity path in participant '" + participant.getName()
											+ "' is either null or empty");

						Activity act = lookupActivity(path, process);
						participant.add(act);
					}
				} else {
					throw new PartitionSpecificationException(
							"Local name of participant node is not correct : "
									+ participantElement.getTagName());
				}
				partitionSpec.add(participant);
			}

			// validation for the partition specification
			PartitionVerificator.check(partitionSpec, process);

			return partitionSpec;

		} catch (XPathExpressionException e) {
			throw new PartitionSpecificationException(e);
		} catch (ActivityNotFoundException e) {
			throw new PartitionSpecificationException(e);
		} catch (ParserConfigurationException e) {
			throw new PartitionSpecificationException(e);
		} catch (SAXException e) {
			throw new PartitionSpecificationException(e);
		} catch (IOException e) {
			throw new PartitionSpecificationException(e);
		}

	}

	/**
	 * Look up in the process given for the activity with the xpath given
	 * 
	 * @param actXPathStr
	 * @param process
	 * @return Found activity, otherwise null
	 * @throws XPathExpressionException
	 * @throws ActivityNotFoundException
	 * @throws PartitionSpecificationException
	 */
	protected Activity lookupActivity(String actXPathStr, Process process)
			throws XPathExpressionException, ActivityNotFoundException,
			PartitionSpecificationException {

		// create xpath
		XPath actXPath = XPathFactory.newInstance().newXPath();
		// set the namespaceContex
		actXPath.setNamespaceContext(new BPELNamespaceContext(process));
		// look up the element with xpath
		org.w3c.dom.Node res = (org.w3c.dom.Node) actXPath.evaluate(actXPathStr,
				process.getElement(), XPathConstants.NODE);

		if (res == null)
			throw new PartitionSpecificationException("check out whether " + actXPathStr
					+ " is correct in the partition specification.");

		// look up the activity with element
		ActivityFinder finder = new ActivityFinder(process);
		Activity activity = finder.find((Element) res);

		if (activity == null)
			throw new ActivityNotFoundException("Can not find activity with the xpath: "
					+ actXPathStr);

		return activity;
	}
}
