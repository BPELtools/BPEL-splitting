package org.bpel4chor.splitprocess.cli;

import org.kohsuke.args4j.Option;

/**
 * Command-line Input for splitting process
 * 
 * <p>
 * It accepts <code>-bpel</code>,<code>-partition</code>,<code>-out</code>,
 * which means respectively
 * <ul>
 * <li>path to bpel file, mandatory
 * <li>path to partition file, mandatory
 * <li>path of output directory, optional
 * </ul>
 * 
 * <p>
 * 
 * <pre>
 * <b>changeLog date user remark</b> <br>
 * @001 2011-11-29 DC initial version <br>
 * </pre>
 * 
 * @since Nov 29, 2011
 * @author Daojun Cui
 */
public class CmdInput {

	@Option(name = "-bpel", required = true, usage = "path to BPEL file")
	private String bpelFilePath;

	@Option(name = "-partition", required = true, usage = "path to partition file")
	private String partitionFilePath;

	@Option(name = "-output", usage = "output directory")
	private String outputDir;

	public String getBpelFilePath() {
		return bpelFilePath;
	}

	public void setBpelFilePath(String bpelFilePath) {
		this.bpelFilePath = bpelFilePath;
	}

	public String getPartitionFilePath() {
		return partitionFilePath;
	}

	public void setPartitionFilePath(String partitionFilePath) {
		this.partitionFilePath = partitionFilePath;
	}

	public String getOutputDir() {
		return outputDir;
	}

	public void setOutputDir(String outputDir) {
		this.outputDir = outputDir;
	}


}
