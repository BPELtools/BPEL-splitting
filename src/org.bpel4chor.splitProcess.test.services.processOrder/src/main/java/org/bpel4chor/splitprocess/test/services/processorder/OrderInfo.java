
package org.bpel4chor.splitprocess.test.services.processorder;

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
 *         &lt;element name="acountNumber" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="oderStatus" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="orderTotalPrice" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="status" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="numDeliveries" type="{http://www.w3.org/2001/XMLSchema}int"/>
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
    "acountNumber",
    "oderStatus",
    "orderTotalPrice",
    "status",
    "numDeliveries"
})
@XmlRootElement(name = "OrderInfo")
public class OrderInfo {

    @XmlElement(required = true)
    protected String acountNumber;
    @XmlElement(required = true)
    protected String oderStatus;
    protected int orderTotalPrice;
    @XmlElement(required = true)
    protected String status;
    protected int numDeliveries;

    /**
     * Gets the value of the acountNumber property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAcountNumber() {
        return acountNumber;
    }

    /**
     * Sets the value of the acountNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAcountNumber(String value) {
        this.acountNumber = value;
    }

    /**
     * Gets the value of the oderStatus property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOderStatus() {
        return oderStatus;
    }

    /**
     * Sets the value of the oderStatus property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOderStatus(String value) {
        this.oderStatus = value;
    }

    /**
     * Gets the value of the orderTotalPrice property.
     * 
     */
    public int getOrderTotalPrice() {
        return orderTotalPrice;
    }

    /**
     * Sets the value of the orderTotalPrice property.
     * 
     */
    public void setOrderTotalPrice(int value) {
        this.orderTotalPrice = value;
    }

    /**
     * Gets the value of the status property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the value of the status property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStatus(String value) {
        this.status = value;
    }

    /**
     * Gets the value of the numDeliveries property.
     * 
     */
    public int getNumDeliveries() {
        return numDeliveries;
    }

    /**
     * Sets the value of the numDeliveries property.
     * 
     */
    public void setNumDeliveries(int value) {
        this.numDeliveries = value;
    }

}
