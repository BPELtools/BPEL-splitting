package org.bpel4chor.splitprocess.test.services.processpayment;

import javax.xml.ws.Endpoint;

public class ProcessPaymentEndpoint {

	public static void main(String[] args) {
		Endpoint.publish("http://localhost:1236/ProcessPayment", new ProcessPayment());
	}

}
