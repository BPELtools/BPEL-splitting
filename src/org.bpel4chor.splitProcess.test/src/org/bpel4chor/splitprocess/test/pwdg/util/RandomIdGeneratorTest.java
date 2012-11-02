package org.bpel4chor.splitprocess.test.pwdg.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.apache.log4j.Logger;
import org.bpel4chor.utils.RandomIdGenerator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class RandomIdGeneratorTest {

	Logger logger = Logger.getLogger(RandomIdGeneratorTest.class);
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetIdInt() {
		String id1 = RandomIdGenerator.getId();
		String id2 = RandomIdGenerator.getId();
		assertNotNull(id1);
		assertNotNull(id2);
		assertEquals(RandomIdGenerator.idLength(), id1.length());
		assertEquals(RandomIdGenerator.idLength(), id2.length());
		assertFalse(id1.equals(id2));
		String id3 = RandomIdGenerator.getId();
		assertNotNull(id3);
		assertFalse(id1.equals(id3));
	}

	// this test make takes a while
	@Test
	public void testLimitOfTheIdGenerator() throws Exception {
		logger.info("Begin Stress Test for limit of RandomIdGenerator, this may take a while");
		int usedidSize = RandomIdGenerator.usedSize();
		for (int i = 0; i < RandomIdGenerator.maxSize() - usedidSize; i++) {
			String id = RandomIdGenerator.getId();
			assertNotNull(id);
			assertEquals(RandomIdGenerator.idLength(), id.length());
		}

		try {
			RandomIdGenerator.getId();
			fail();
		} catch (Exception e) {
			if(e instanceof RuntimeException)
				logger.info("Expected Stress Test Result: "+e.toString());
			else
				throw new Exception(e);
		}

	}
}
