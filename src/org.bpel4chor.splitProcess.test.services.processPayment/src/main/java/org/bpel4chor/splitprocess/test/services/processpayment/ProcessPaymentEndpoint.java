package org.bpel4chor.splitprocess.test.services.processpayment;

import javax.xml.ws.Endpoint;

public class ProcessPaymentEndpoint {

	public static void main(String[] args) {
		// Source: http://stackoverflow.com/a/16338394/873282 - works on JDK8
		System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
		System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
		System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
		System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");

		String endpointLocation = "http://localhost:1236/ProcessPayment";
		Endpoint.publish(endpointLocation, new ProcessPayment());
		System.out.println("published at " + endpointLocation);
		System.out.println("Get the WSDL at " + endpointLocation + "?wsdl");
	}

}
