Test files for org.bpel4chor.splitProcess
------------------------------------------

1. OrderInfo 
full example of ordering process without loop

2. OrderInfoSimple1
simplified version of ordering process with 2 activities and the base flow
with partition specification: Partition.xml

3. OrderInfoSimple2
simplified version of ordering process with 3 activities and the base flow
with partition specification: Partition.xml

4. OrderInfoSimple3
simplified version of ordering process with 6 activities and the base flow
with partition specification: Partition.xml

5. OrderInfoSimple4
for testing DataFlowAnalysis
with partition specification: Partition.xml

5. ProcessOrder
[replaced by service implementation - see org.bpel4chor.splitProcess.test.services.processOrder]

6. ProcessPayment
[replaced by service implementation - see org.bpel4chor.splitProcess.test.services.processPayment]

7. DeliverProcess
Invoked process

8. OrderInfoWithLoop
full example of ordering process with loop
with partition specification: Partition.xml

9. ProcessFoo
Invoked synchronous Process
In-bound XSD Schema
Flow(Receive->Assign->Reply)


10. ProcessBla
Asynchronous Process, invoke ProcessFoo, contain CorrelationSet "content"
In-bound XSD Schema
Flow(ReceiveInput->Assign->InvokeFoo->Assign1->CallBackClient)

11. ProcessOctopus
Asynchronous Process, for multiple links target an activity(callbackClient)
In-bound XSD Schema
Flow(ReceiveInput->{Assign1, Assign2}->CallBackClient)

12. PWDGProcess
Asynchronous process, it is only for testing PWDGFactory

13. OrderInfo4DDTestCase1
The ordering process with the test scenario 1 for data dependency fragment
The test scenario 1 consists of activities A, B, C, D, G, together they stimulate the scenario Top: paymntInfo in Figure 5 in the paper "Maintaining Data Dependencies Across BPEL Process Fragments"
The partition specification is described in partition.xml.


14. OrderInfo4DDTestCase2
The ordering process with the test scenario 2 for data dependency fragment
The test scenario 2 consists of activities A, B, C, D, H.
The A is receive activity and the H is reply activity. They are bound to form the synchronous bpel process. So A and H must be in the same partition.
The partition specification is described in partition.xml.