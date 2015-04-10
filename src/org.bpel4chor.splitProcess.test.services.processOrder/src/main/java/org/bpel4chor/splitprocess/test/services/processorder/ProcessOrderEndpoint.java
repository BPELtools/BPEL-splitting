package org.bpel4chor.splitprocess.test.services.processorder;

import javax.xml.ws.Endpoint;

public class ProcessOrderEndpoint {

	public static void main(String[] args) {
		Endpoint.publish("http://localhost:1235/ProcessOrder", new ProcessOrder());
	}

}
