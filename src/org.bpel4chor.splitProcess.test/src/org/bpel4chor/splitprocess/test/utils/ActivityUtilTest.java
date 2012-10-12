package org.bpel4chor.splitprocess.test.utils;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.bpel4chor.splitprocess.utils.ActivityUtil;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.Else;
import org.eclipse.bpel.model.ElseIf;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.If;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.OnAlarm;
import org.eclipse.bpel.model.OnMessage;
import org.eclipse.bpel.model.Pick;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.RepeatUntil;
import org.eclipse.bpel.model.Reply;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.While;
import org.eclipse.bpel.model.impl.BPELFactoryImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test Case for ActivityUtil
 * 
 * <p>
 * <b>changeLog date user remark</b> <br>
 * 
 * @001 2012-01-28 DC initial version
 * 
 * @since Jan 28, 2012
 * @author Daojun Cui
 */
public class ActivityUtilTest {

	List<Activity> basicActivities = null;
	List<Activity> simpleActivities = null;

	/**
	 * Run only once before class begins
	 * @throws Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * Run each time when each Test begins
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		basicActivities = new ArrayList<Activity>();
		simpleActivities = new ArrayList<Activity>();
		
		basicActivities.add(BPELFactoryImpl.eINSTANCE.createEmpty());
		basicActivities.add(BPELFactoryImpl.eINSTANCE.createInvoke());
		basicActivities.add(BPELFactoryImpl.eINSTANCE.createAssign());
		basicActivities.add(BPELFactoryImpl.eINSTANCE.createReply());
		basicActivities.add(BPELFactoryImpl.eINSTANCE.createReceive());

		simpleActivities.addAll(basicActivities);
		simpleActivities.add(BPELFactoryImpl.eINSTANCE.createFlow());
	}

	@After
	public void tearDown() throws Exception {
		// insert code here to finalise the class
	}


	@Test
	public void testIsBasicActivity() {
		//
		// THE FULL PRIMITIVE ACTIVITY SET: Empty, Invoke, Assign, Reply,
		// Receive,
		// Wait, Throw, Exit, OpaqueActivity, Rethrow, Validate,
		// ExtensionActivity,
		// Compensate, CompensateScope
		//

		Flow flow = BPELFactoryImpl.eINSTANCE.createFlow();
		try{
			ActivityUtil.isBasicActivity(null);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals(true, e instanceof NullPointerException);
		}
		Assert.assertEquals(false, ActivityUtil.isBasicActivity(flow));
		
		for (Activity act : basicActivities) {
			Assert.assertEquals(true, ActivityUtil.isBasicActivity(act));
		}

	}

	@Test
	public void testIsSimpleActivity() {
		//
		// THE FULL SIMPLE ACTIVITY SET: Empty, Invoke, Assign, Reply, Receive,
		// Wait, Throw, Exit, OpaqueActivity, Rethrow, Validate,
		// ExtensionActivity,
		// Compensate, CompensateScope, *Flow*
		//
		Flow flow = BPELFactoryImpl.eINSTANCE.createFlow();
		try{
			ActivityUtil.isSimpleActivity(null);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals(true, e instanceof NullPointerException);
		}
		Assert.assertEquals(true, ActivityUtil.isSimpleActivity(flow));
		
		for (Activity act : simpleActivities) {
			Assert.assertEquals(true, ActivityUtil.isSimpleActivity(act));
		}
	}

	/**
	 * Test Object
	 * 
	 * <pre>
	 * flow
	 *  |
	 *  |-> receive1
	 *  |-> assgin1
	 *  |-> inovke1
	 *  |-> while
	 *  |    |->flow
	 *  |        |-> invoke2
	 *  |        |-> assign2
	 *  |        |-> if
	 *  |             |-> elseif
	 *  |             |     |-> invoke3
	 *  |             |-> else
	 *  |                   |-> assign3
	 *  |-> pick
	 *  |    |-> OnMessage
	 *  |    |      |-> scope
	 *  |    |            |-> sequence
	 *  |    |                   |-> receive2
	 *  |    |                   |-> assign4 
	 *  |    |-> OnAlarm
	 *  |           |-> repeatUntil
	 *  |                   |-> assign5
	 *  |
	 *  |-> reply
	 * </pre>
	 */
	@Test
	public void testGetAllDescBasicChildren() {
		// Get all descendant primitive activity children of the given activity

		// Expected Results: The recursively descendant primitive children of
		// the activity, if the activity is primitive one or is null,
		// return an empty array list.

		Activity testActivity = null;

		// activity is null
		Assert.assertEquals(true, ActivityUtil.getAllDescBasicChildren(testActivity).isEmpty());

		// activity is flow
		testActivity = createTestObject();

		String[] expectedNames = { "TestReceive1", "TestReceive2", "TestAssign1", "TestAssign2", "TestAssign3",
				"TestAssign4", "TestAssign5", "TestAssign6", "TestInvoke1", "TestInvoke2", "TestInvoke3", "TestReply" };
		List<Activity> actualActs = ActivityUtil.getAllDescBasicChildren(testActivity);
		// lengthe is the same
		Assert.assertEquals(expectedNames.length, actualActs.size());
		// names are the same
		for (Activity actualAct : actualActs) {
			boolean expected = true;
			boolean actual = false;
			for (String expectedName : expectedNames) {
				if (expectedName.equals(actualAct.getName())) {
					actual = true;
				}
			}
			Assert.assertEquals(expected, actual);
		}
	}

	/**
	 * Test Object
	 * 
	 * <pre>
	 * flow
	 *  |
	 *  |-> receive1
	 *  |-> assgin1
	 *  |-> inovke1
	 *  |-> while
	 *  |    |->flow
	 *  |        |-> invoke2
	 *  |        |-> assign2
	 *  |        |-> if
	 *  |             |->assign3
	 *  |             |-> elseif
	 *  |             |     |-> invoke3
	 *  |             |-> else
	 *  |                   |-> assign4
	 *  |-> pick
	 *  |    |-> OnMessage
	 *  |    |      |-> scope
	 *  |    |            |-> sequence
	 *  |    |                   |-> receive2
	 *  |    |                   |-> assign5 
	 *  |    |-> OnAlarm
	 *  |           |-> repeatUntil
	 *  |                   |-> assign6
	 *  |
	 *  |-> reply
	 * </pre>
	 */
	private Activity createTestObject() {

		Flow testAct = BPELFactoryImpl.eINSTANCE.createFlow();
		Receive receive1 = BPELFactoryImpl.eINSTANCE.createReceive();
		Receive receive2 = BPELFactoryImpl.eINSTANCE.createReceive();
		Assign assign1 = BPELFactoryImpl.eINSTANCE.createAssign();
		Assign assign2 = BPELFactoryImpl.eINSTANCE.createAssign();
		Assign assign3 = BPELFactoryImpl.eINSTANCE.createAssign();
		Assign assign4 = BPELFactoryImpl.eINSTANCE.createAssign();
		Assign assign5 = BPELFactoryImpl.eINSTANCE.createAssign();
		Assign assign6 = BPELFactoryImpl.eINSTANCE.createAssign();
		Invoke invoke1 = BPELFactoryImpl.eINSTANCE.createInvoke();
		Invoke invoke2 = BPELFactoryImpl.eINSTANCE.createInvoke();
		Invoke invoke3 = BPELFactoryImpl.eINSTANCE.createInvoke();
		If ifAct = BPELFactoryImpl.eINSTANCE.createIf();
		ElseIf elseIfAct = BPELFactoryImpl.eINSTANCE.createElseIf();
		Else elseAct = BPELFactoryImpl.eINSTANCE.createElse();
		While _while = BPELFactoryImpl.eINSTANCE.createWhile();
		Scope scope = BPELFactoryImpl.eINSTANCE.createScope();
		Sequence sequence = BPELFactoryImpl.eINSTANCE.createSequence();
		Pick pick = BPELFactoryImpl.eINSTANCE.createPick();
		RepeatUntil repeatUntil = BPELFactoryImpl.eINSTANCE.createRepeatUntil();
		Flow flow = BPELFactoryImpl.eINSTANCE.createFlow();
		Reply reply = BPELFactoryImpl.eINSTANCE.createReply();

		// set names to the primitive activities
		receive1.setName("TestReceive1");
		receive2.setName("TestReceive2");
		assign1.setName("TestAssign1");
		assign2.setName("TestAssign2");
		assign3.setName("TestAssign3");
		assign4.setName("TestAssign4");
		assign5.setName("TestAssign5");
		assign6.setName("TestAssign6");
		invoke1.setName("TestInvoke1");
		invoke2.setName("TestInvoke2");
		invoke3.setName("TestInvoke3");
		reply.setName("TestReply");

		// children
		// construct if
		ifAct.setActivity(assign3);
		elseIfAct.setActivity(invoke3);
		elseAct.setActivity(assign4);
		ifAct.getElseIf().add(elseIfAct);
		ifAct.setElse(elseAct);
		// construct flow
		flow.getActivities().add(invoke2);
		flow.getActivities().add(assign2);
		flow.getActivities().add(ifAct);
		_while.setActivity(flow);

		// construct sequence
		sequence.getActivities().add(receive2);
		sequence.getActivities().add(assign5);
		// scope
		scope.setActivity(sequence);
		// repeatUntil
		repeatUntil.setActivity(assign6);

		// construct pick
		OnMessage onMsg = BPELFactoryImpl.eINSTANCE.createOnMessage();
		onMsg.setActivity(scope);
		OnAlarm onAlarm = BPELFactoryImpl.eINSTANCE.createOnAlarm();
		onAlarm.setActivity(repeatUntil);

		// construct pick
		pick.getMessages().add(onMsg);
		pick.getAlarm().add(onAlarm);

		// assemble
		testAct.getActivities().add(receive1);
		testAct.getActivities().add(assign1);
		testAct.getActivities().add(invoke1);
		testAct.getActivities().add(_while);
		testAct.getActivities().add(pick);
		testAct.getActivities().add(reply);

		return testAct;
	}


	@Test
	public void testGetDirectChildrenActivity() {
		// Get the direct children of the given activity
		Activity act = null;
		Assert.assertEquals(0, ActivityUtil.getDirectChildren(act).size());
		
		Flow flow = (Flow) createTestObject();
		List<Activity> directChildren = ActivityUtil.getDirectChildren(flow);
		Assert.assertEquals(6, directChildren.size());
	}
	
	@Test
	public void testIsEqualActivitySet() {
		Set<Activity> actSet1 = new HashSet<Activity>();
		Set<Activity> actSet2 = new HashSet<Activity>();
		Set<Activity> actSet3 = new HashSet<Activity>();
		Activity A = BPELFactory.eINSTANCE.createActivity();
		Activity B = BPELFactory.eINSTANCE.createActivity();
		Activity C = BPELFactory.eINSTANCE.createActivity();
		actSet1.add(A);
		actSet1.add(B);
		actSet2.add(A);
		actSet2.add(B);
		actSet3.add(C);
		// set1==set2
		assertTrue(ActivityUtil.isEqual(actSet1, actSet2));
		// set1==set3
		assertFalse(ActivityUtil.isEqual(actSet1, actSet3));
	}

	@Test
	public void testIsEqualActivity() {
		Activity A = BPELFactory.eINSTANCE.createActivity();
		Activity anotherA = BPELFactory.eINSTANCE.createActivity();
		Activity C = BPELFactory.eINSTANCE.createActivity();
		A.setName("A");
		anotherA.setName("A");
		C.setName("C");
		assertTrue(ActivityUtil.isEqual(A, anotherA));
		assertFalse(ActivityUtil.isEqual(A, C));
	}

}
