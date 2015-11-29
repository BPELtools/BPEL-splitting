
package org.bpel4chor.splitprocess.test.services.processpayment;

import javax.jws.Oneway;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlElement;

@WebService(name = "ProcessPayment", targetNamespace = "http://www.bpel4chor.org/splitProcess/test/services/ProcessPayment")
public class ProcessPayment {

	@Oneway
	public void processPayment(@XmlElement(required = true) @WebParam(name = "actNum", targetNamespace = "http://www.bpel4chor.org/splitProcess/test/services/ProcessPayment") String actNum,
			@WebParam(name = "amt", targetNamespace = "http://www.bpel4chor.org/splitProcess/test/services/ProcessPayment") double amt) {
		System.out.println(String.format("Received PaymentInfo with account number %s and amount %f", actNum, amt));
	}

}
