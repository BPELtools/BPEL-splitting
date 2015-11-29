package org.bpel4chor.splitprocess.test.services.processorder;

import javax.jws.Oneway;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlElement;

@WebService(name = "ProcessOrder", targetNamespace = "http://www.bpel4chor.org/splitProcess/test/services/ProcessOrder")
public class ProcessOrder {

	@Oneway
	public void processOrder(
			@WebParam(name = "accountNumber", targetNamespace = "http://www.bpel4chor.org/splitProcess/test/services/ProcessOrder") @XmlElement(required = true) String accountNumber,
			@WebParam(name = "totalPrice", targetNamespace = "http://www.bpel4chor.org/splitProcess/test/services/ProcessOrder") double totalPrice,
			@WebParam(name = "status", targetNamespace = "http://www.bpel4chor.org/splitProcess/test/services/ProcessOrder") @XmlElement(required = true) String status,
			@WebParam(name = "numDeliveries", targetNamespace = "http://www.bpel4chor.org/splitProcess/test/services/ProcessOrder") int numDeliveries) {
		System.out.println(String.format("Retreived OrderInfo with status %s and total price %f", status, totalPrice));
	}

}
