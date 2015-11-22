
package org.bpel4chor.splitprocess.test.services.processpayment;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;


@WebService(name = "ProcessPayment", targetNamespace = "http://www.bpel4chor.org/splitProcess/test/services/ProcessPayment")
@XmlSeeAlso({
    ObjectFactory.class
})
public class ProcessPayment {


    @WebMethod(action = "http://www.iaas.uni-stuttgart.de/ProcessPayment/NewOperation")
    @Oneway
    public void processPayment(
        @WebParam(name = "PaymentInfo", targetNamespace = "http://www.bpel4chor.org/splitProcess/test/services/ProcessPayment", partName = "payload")
        PaymentInfo payload) {
    	System.out.println("Received PaymentInfo with account number " + payload.getActNum());
    }

}
