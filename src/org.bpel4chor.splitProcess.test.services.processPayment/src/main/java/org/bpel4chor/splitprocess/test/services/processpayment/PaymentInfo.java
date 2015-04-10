
package org.bpel4chor.splitprocess.test.services.processpayment;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="amt" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="actNum" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "amt",
    "actNum"
})
@XmlRootElement(name = "PaymentInfo")
public class PaymentInfo {

    protected int amt;
    @XmlElement(required = true)
    protected String actNum;

    /**
     * Gets the value of the amt property.
     * 
     */
    public int getAmt() {
        return amt;
    }

    /**
     * Sets the value of the amt property.
     * 
     */
    public void setAmt(int value) {
        this.amt = value;
    }

    /**
     * Gets the value of the actNum property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getActNum() {
        return actNum;
    }

    /**
     * Sets the value of the actNum property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setActNum(String value) {
        this.actNum = value;
    }

}
