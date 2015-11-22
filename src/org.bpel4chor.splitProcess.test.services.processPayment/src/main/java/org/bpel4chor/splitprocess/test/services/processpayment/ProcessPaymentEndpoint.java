package org.bpel4chor.splitprocess.test.services.processpayment;

import javax.xml.ws.Endpoint;

public class ProcessPaymentEndpoint {

	public static void main(String[] args) {
		String endpointLocation = "http://localhost:1236/ProcessPayment";
		Endpoint.publish(endpointLocation, new ProcessPayment());
		System.out.println("published at " + endpointLocation);
		System.out.println("Get the WSDL at " + endpointLocation + "?wsdl");
	}

}
