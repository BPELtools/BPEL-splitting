
package org.bpel4chor.splitprocess.test.services.processorder;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;


@WebService(name = "ProcessOrder", targetNamespace = "http://www.bpel4chor.org/splitProcess/test/services/ProcessOrder")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@XmlSeeAlso({
    ObjectFactory.class
})
public class ProcessOrder {


    @WebMethod(action = "http://www.bpel4chor.org/splitProcess/test/services/ProcessOrder/processOrder")
    @Oneway
    public void processOrder(
        @WebParam(name = "OrderInfo", targetNamespace = "http://www.bpel4chor.org/splitProcess/test/services/ProcessOrder", partName = "payload")
        OrderInfo payload) {
    	System.out.println("Retreived OrderInfo with status " + payload.oderStatus);
    }

}
