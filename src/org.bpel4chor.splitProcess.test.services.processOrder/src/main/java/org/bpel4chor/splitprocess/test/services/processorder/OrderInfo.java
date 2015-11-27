
package org.bpel4chor.splitprocess.test.services.processorder;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "accountNumber",
    "orderStatus",
    "orderTotalPrice",
    "status",
    "numDeliveries"
})
@XmlRootElement(name = "OrderInfo")
public class OrderInfo {

    @XmlElement(required = true)
    protected String accountNumber;
    @XmlElement(required = true)
    protected String orderStatus;
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
    public String getAccountNumber() {
        return accountNumber;
    }

    /**
     * Sets the value of the acountNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAccountNumber(String value) {
        this.accountNumber = value;
    }

    /**
     * Gets the value of the oderStatus property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOrderStatus() {
        return orderStatus;
    }

    /**
     * Sets the value of the oderStatus property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOrderStatus(String value) {
        this.orderStatus = value;
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
