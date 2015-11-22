package org.bpel4chor.splitprocess.test.services.processorder;

import javax.xml.ws.Endpoint;

public class ProcessOrderEndpoint {

	public static void main(String[] args) {
		String endpointLocation = "http://localhost:1235/ProcessOrder";
		Endpoint.publish(endpointLocation, new ProcessOrder());
		System.out.println("published at " + endpointLocation);
		System.out.println("Get the WSDL at " + endpointLocation + "?wsdl");
	}

}
