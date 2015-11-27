package org.bpel4chor.splitprocess.test.services.processorder;

import javax.xml.ws.Endpoint;

public class ProcessOrderEndpoint {

	public static void main(String[] args) {
		// Source: http://stackoverflow.com/a/16338394/873282
		System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
		System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
		System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
		System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");

		String endpointLocation = "http://localhost:1235/ProcessOrder";
		Endpoint.publish(endpointLocation, new ProcessOrder());
		System.out.println("published at " + endpointLocation);
		System.out.println("Get the WSDL at " + endpointLocation + "?wsdl");
	}

}
