package org.bpel4chor.splitprocess.test.cli;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import javax.wsdl.WSDLException;

import org.bpel4chor.splitprocess.cli.SplitProcessMainCmd;
import org.junit.Before;
import org.junit.Test;

import de.uni_stuttgart.iaas.bpel.model.utilities.MyWSDLUtil;

public class SplitProcessMainCmdTest {

	String bpelFilePath;
	String partitionFilePath;
	String wsdlURI;

	@Before
	public void setUp() {
		File currentDir = new File("");
		bpelFilePath = currentDir.getAbsolutePath()
				+ "\\files\\OrderInfo\\bpelContent\\OrderingProcess.bpel";
		wsdlURI = currentDir.getAbsolutePath()
				+ "\\files\\OrderInfo\\bpelContent\\OrderingProcess.wsdl";
		partitionFilePath = currentDir.getAbsolutePath()
				+ "\\files\\OrderInfo\\bpelContent\\Partition1.xml";
	}

	@Test
	public void testRunArgs() {

		String[] args = { "-bpel", bpelFilePath, "-partition", partitionFilePath, "-output",
				org.bpel4chor.utils.BPEL4ChorConstants.BPEL4CHOR_DEFAULT_WRITE_DIR + "\\OrderInfo" + Calendar.getInstance().getTimeInMillis() };

		SplitProcessMainCmd cmd = new SplitProcessMainCmd();
		cmd.run(args);

	}

	@Test
	public void testReadWSDL() throws WSDLException, IOException {
		System.out.println(wsdlURI);
		MyWSDLUtil.readWSDL(wsdlURI);

	}
}
