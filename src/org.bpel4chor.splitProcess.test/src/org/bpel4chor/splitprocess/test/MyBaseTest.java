package org.bpel4chor.splitprocess.test;

import javax.xml.namespace.QName;

import junit.framework.Assert;

import org.eclipse.bpel.model.messageproperties.MessagepropertiesFactory;
import org.eclipse.bpel.model.messageproperties.Property;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class MyBaseTest extends TestUtil{

	@BeforeClass
	public static void setUpBeforeClass() {
		
	}
	
	@Before
	public void setUp() {
		
	}
	
	@Test
	public void TestEquality() {
		QName qName1 = new QName("www.example.com", "example1", "ex");
		QName qName2 = new QName("www.example.com", "example1", "ex");
		Assert.assertEquals(qName1, qName2);
		
		Property prop1 =  MessagepropertiesFactory.eINSTANCE.createProperty();
		Property prop2 =  MessagepropertiesFactory.eINSTANCE.createProperty();

		prop1.setName("property1");
		prop2.setName("property1");
		
		prop1.setType("type1");
		prop2.setType("type1");
		
		prop1.setQName(qName1);
		prop2.setQName(qName2);
		
		Assert.assertEquals(true, prop2.equals(prop1));
	}
}
