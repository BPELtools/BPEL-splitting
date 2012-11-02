package org.bpel4chor.splitprocess.cli;

import java.io.File;
import java.io.PrintStream;

import org.apache.log4j.Logger;
import org.bpel4chor.splitprocess.ProcessSplitter;
import org.bpel4chor.utils.BPEL4ChorModelConstants;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

/**
 * Main command-line program that parses the input arguments and runs the
 * ProcessSplitter based on the arguments given.
 * 
 * The output see also {@link ProcessSplitter}
 * 
 * @since Nov 29, 2011
 * @author Daojun Cui
 */
public class SplitProcessMainCmd {

	/**
	 * command line input, it contains the information for (1) bpel file (2)
	 * partition file (3) output dir
	 */
	private CmdInput cmdInput = new CmdInput();

	private Logger logger = Logger.getLogger(SplitProcessMainCmd.class);

	/**
	 * Main function
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		SplitProcessMainCmd splitProcess = new SplitProcessMainCmd();
		splitProcess.run(args);
	}

	/**
	 * run the program
	 * 
	 * @param args
	 */
	public void run(String[] args) {
		CmdLineParser parser = new CmdLineParser(cmdInput);
		parser.setUsageWidth(80);

		try {
			parser.parseArgument(args);
			if (cmdInput.getOutputDir() == null || cmdInput.getOutputDir().isEmpty()) {
				cmdInput.setOutputDir(BPEL4ChorModelConstants.DEFAULT_SPLITTING_OUTPUT_DIR);
				File outputDir = new File(cmdInput.getOutputDir());
				if (outputDir.exists() == false) {
					outputDir.mkdirs();
				}
			}

			// initialize the process splitter
			ProcessSplitter splitter = new ProcessSplitter(cmdInput.getBpelFilePath(),
					cmdInput.getPartitionFilePath(), cmdInput.getOutputDir());

			// split process
			splitter.split();

		} catch (CmdLineException e) {
			logger.error(e.getMessage());
			printUsage(parser, System.err);
			System.exit(1);
		} catch (Exception e) {
			logger.error(e.toString(), e);
			System.exit(1);
		}
	}

	/**
	 * Print usage of the program
	 * 
	 * @param parser
	 * @param out
	 */
	protected void printUsage(CmdLineParser parser, PrintStream out) {
		out.println("Usage: java org.bpel4chor.splitprocess.cli.Cmd -bpel files" + File.separator
				+ " -partition files" + File.separator + " -output path" + File.separator + "to"
				+ File.separator + "outputdir -debug");
		parser.printUsage(System.out);
		out.println();
	}
}
