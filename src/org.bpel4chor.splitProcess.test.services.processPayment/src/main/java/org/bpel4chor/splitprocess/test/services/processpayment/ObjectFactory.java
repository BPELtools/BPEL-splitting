
package org.bpel4chor.splitprocess.test.services.processpayment;

import java.math.BigInteger;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.bpel4chor.splitprocess.test.services.processpayment package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Delivered_QNAME = new QName("http://www.bpel4chor.org/splitProcess/test/services/ProcessPayment", "Delivered");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.bpel4chor.splitprocess.test.services.processpayment
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link PaymentInfo }
     * 
     */
    public PaymentInfo createPaymentInfo() {
        return new PaymentInfo();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.bpel4chor.org/splitProcess/test/services/ProcessPayment", name = "Delivered")
    public JAXBElement<BigInteger> createDelivered(BigInteger value) {
        return new JAXBElement<BigInteger>(_Delivered_QNAME, BigInteger.class, null, value);
    }

}
