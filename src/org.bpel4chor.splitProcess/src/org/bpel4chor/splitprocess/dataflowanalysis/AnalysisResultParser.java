package org.bpel4chor.splitprocess.dataflowanalysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.bpel4chor.splitprocess.utils.ActivityUtil;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.BPELExtensibleElement;
import org.eclipse.bpel.model.Variable;

import de.uni_stuttgart.iaas.bpel_d.algorithm.analysis.AnalysisResult;
import de.uni_stuttgart.iaas.bpel_d.algorithm.analysis.Utility;
import de.uni_stuttgart.iaas.bpel_d.algorithm.infrastructure.InOut;
import de.uni_stuttgart.iaas.bpel_d.algorithm.infrastructure.Placement;
import de.uni_stuttgart.iaas.bpel_d.algorithm.infrastructure.Writes;

/**
 * AnalysisResultParser parses the data flow analysis and extracts the query to
 * writer map out of the result data.
 * <p>
 * Given an activity, a variable that it reads, and the analysis results,
 * AnalysisResultParser provides a query set to writer map that represents the
 * write state of the Variable at the Place IN of the reader.
 * 
 * @since Feb 27, 2012
 * @author Daojun Cui
 */
public class AnalysisResultParser {

	/** The result of data flow analysis on the underlying process */
	protected static AnalysisResult analysis = null;

	protected static Logger logger = Logger.getLogger(AnalysisResultParser.class);

	/**
	 * Parse the writers at Place IN out of the analysis result and provide a
	 * query-writer-set.
	 * 
	 * @param act
	 *            The reader
	 * @param var
	 *            The variable
	 * @param analysisResult
	 *            Data flow analysis
	 * @return
	 */
	public static QueryWriterSet parse(Activity act, Variable var, AnalysisResult analysisResult) {

		if (act == null || var == null || analysisResult == null)
			throw new NullPointerException();

		Map<String, Map<Placement, Writes>> resultData = analysisResult.getResultData();
		Map<String, Set<Activity>> singleQuery2WriterMap = new HashMap<String, Set<Activity>>();

		// extract writers via the variable given
		for (String fullQuery : resultData.keySet()) {
			if (fullQuery.equals(var.getName()) || fullQuery.startsWith(var.getName() + ".")) {

				Set<Activity> possibleWriters = new HashSet<Activity>();

				// Get placement to writes map of varQuery
				Map<Placement, Writes> placement2WritesMap = resultData.get(fullQuery);

				// Get writers by the place IN of act
				Placement actPlacementIN = new Placement(act, InOut.IN);
				Writes writesBeforeAct = placement2WritesMap.get(actPlacementIN);

				// Get only the 'possible' writers
				Set<BPELExtensibleElement> possWritersByAct = writesBeforeAct.getPoss();

				// Take only the basic activity
				for (BPELExtensibleElement possWriter : possWritersByAct) {
					if (isBasicActivity(possWriter)) {
						possibleWriters.add((Activity) possWriter);
					}
				}

				String queryForWholeVariable = "";
				String queryWithoutVarName = fullQuery.equals(var.getName()) ? queryForWholeVariable : fullQuery
						.substring(var.getName().length());
				singleQuery2WriterMap.put(queryWithoutVarName, possibleWriters);
			}
		}

		// try to merge queries with same writer set into a query set.
		Map<Set<String>, Set<Activity>> querySet2WriterSetMap = mergeQueries(singleQuery2WriterMap);

		return new QueryWriterSet(act, var, querySet2WriterSetMap);

	}

	/**
	 * Merge queries that have same writer set into a query set
	 * 
	 * @param query2WriterSetMap
	 * @return querySet2WriterSetMap
	 */
	protected static Map<Set<String>, Set<Activity>> mergeQueries(Map<String, Set<Activity>> query2WriterSetMap) {

		if (query2WriterSetMap == null)
			throw new NullPointerException();

		Map<Set<String>, Set<Activity>> querySet2WriterSet = new HashMap<Set<String>, Set<Activity>>();

		// each iteration create one query set - writer set entry
		while (!query2WriterSetMap.isEmpty()) {

			String query = query2WriterSetMap.keySet().iterator().next();
			Set<Activity> writerSet = query2WriterSetMap.remove(query);

			// collect query into query set
			Set<String> querySet = new HashSet<String>();
			querySet.add(query);

			// test the rest in the query2writerSetMap
			String[] keys = query2WriterSetMap.keySet().toArray(new String[0]);

			for (String key : keys) {
				Set<Activity> wrtSet = query2WriterSetMap.get(key);
				if (ActivityUtil.isEqual(writerSet, wrtSet)) {
					// merge into a query set
					querySet.add(key);
					// remove from map
					query2WriterSetMap.remove(key);
				}
			}

			// store into querySet2WriterSet
			querySet2WriterSet.put(querySet, writerSet);
		}

		return querySet2WriterSet;
	}

	/**
	 * Test whether the element given is a basic BPEL activity.
	 * 
	 * @param act
	 *            The BPEL element
	 * @return true if the element given is basic activity, or false.
	 */
	protected static boolean isBasicActivity(BPELExtensibleElement act) {
		if (act instanceof Activity) {
			return ActivityUtil.isBasicActivity((Activity) act);
		}
		return false;
	}

	/**
	 * Print out the analysis result in the manner of Q_s(a,x), i.e. Query, IN
	 * Writers, OUT Writers.
	 * 
	 * @param analysisRes
	 */
	public static void print(AnalysisResult analysisRes) {

		SortedSet<String> sortedVars = new TreeSet<String>();
		sortedVars.addAll(analysisRes.getAllVariableElementNames());
		Set<Activity> anonymousActivities = new HashSet<Activity>();

		HashMap<String, Activity> mapActivities = new HashMap<String, Activity>();
		for (Activity act : analysisRes.getAllActivities()) {
			String name = act.getName();
			if (name != null)
				mapActivities.put(act.getName(), act);
			else
				anonymousActivities.add(act);
		}
		SortedSet<String> sortedActs = new TreeSet<String>();
		sortedActs.addAll(mapActivities.keySet());

		for (String actName : sortedActs) {
			logger.info(String.format("%1$20s %2$s %3$" + (90 - actName.length()) + "s", " ", actName, " ").replace(
					" ", "="));
			StringBuffer sb = new StringBuffer();
			sb.append(String.format("%-15s %-40s %-30s %-30s", "Activity:", "Query:", "IN Writers:", "OUT Writers:"));
			logger.info(sb.toString());
			Activity act = mapActivities.get(actName);
			for (String var : sortedVars) {
				printActAdvance(var, act, analysisRes.getResultData());
			}
		}
		for (String var : sortedVars) {
			logger.info(String.format("%1$20s %2$s %3$" + (90 - var.length()) + "s", " ", var, " ").replace(" ", "="));
			StringBuffer sb = new StringBuffer();
			sb.append(String.format("%-30s %-20s %-33s %-30s", "Query:", "Activity:", "IN Writers:", "OUT Writers:"));
			logger.info(sb.toString());
			for (String actName : sortedActs) {
				Activity act = mapActivities.get(actName);
				printVarAdvance(var, act, analysisRes.getResultData());
			}
		}
	}

	
	public static void printActPreorder(AnalysisResult analysisRes) {
		SortedSet<String> sortedVars = new TreeSet<String>();
		sortedVars.addAll(analysisRes.getAllVariableElementNames());
		Set<Activity> anonymousActivities = new HashSet<Activity>();

		HashMap<String, Activity> mapActivities = new HashMap<String, Activity>();
		for (Activity act : analysisRes.getAllActivities()) {
			String name = act.getName();
			if (name != null)
				mapActivities.put(act.getName(), act);
			else
				anonymousActivities.add(act);
		}
		SortedSet<String> sortedActs = new TreeSet<String>();
		sortedActs.addAll(mapActivities.keySet());

		for (String actName : sortedActs) {
			logger.info(String.format("%1$20s %2$s %3$" + (90 - actName.length()) + "s", " ", actName, " ").replace(
					" ", "="));
			StringBuffer sb = new StringBuffer();
			sb.append(String.format("%-15s %-40s %-30s %-30s", "Activity:", "Query:", "IN Writers:", "OUT Writers:"));
			logger.info(sb.toString());
			Activity act = mapActivities.get(actName);
			for (String var : sortedVars) {
				printActAdvance(var, act, analysisRes.getResultData());
			}
		}
	}
	
	/**
	 * Print out the line with variable query, IN writers, OUT writers.
	 * 
	 * @param var
	 * @param act
	 * @param resultData
	 */
	protected static void printActAdvance(String var, Activity act, Map<String, Map<Placement, Writes>> resultData) {

		Map<Placement, Writes> writersForVar = resultData.get(var);
		Writes writesBeforeActivity = writersForVar.get(new Placement(act, InOut.IN));
		Writes writesAfterActivity = writersForVar.get(new Placement(act, InOut.OUT));

		if (writesBeforeActivity == null) {
			logger.info("!! not handled !!");
		} else {
			StringBuffer sb = new StringBuffer();
			sb = new StringBuffer();
			// Print Format:
			// 15 characters for arg 1, filled with tailing white spaces until
			// max. 15 length,
			// 40 characters for arg 2, filled with tailing white spaces until
			// max. 40 length,
			// plus IN:
			// 30 characters for arg 3, filled with tailing white spaces until
			// max. 30 length,
			// plus OUT:
			// 30 characters for arg 4, filled with tailing white spaces until
			// max. 30 length,
			sb.append(String.format("%-15s %-40s IN: %-30s OUT: %-30s", act.getName(), var,
					collectionToString(writesBeforeActivity.getPoss()),
					collectionToString(writesAfterActivity.getPoss())));

			logger.info(sb.toString());
		}
	}

	protected static void printVarAdvance(String var, Activity act, Map<String, Map<Placement, Writes>> resultData) {
		Map<Placement, Writes> writersForVar = resultData.get(var);
		Writes writesBeforeActivity = writersForVar.get(new Placement(act, InOut.IN));
		Writes writesAfterActivity = writersForVar.get(new Placement(act, InOut.OUT));

		if (writesBeforeActivity == null) {
			logger.info("!! not handled !!");
		} else {
			StringBuffer sb = new StringBuffer();
			sb = new StringBuffer();
			// Print Format:
			// 30 characters for arg 1, filled with tailing white spaces until
			// max. 30 length,
			// 20 characters for arg 2, filled with tailing white spaces until
			// max. 20 length,
			// plus IN:
			// 30 characters for arg 3, filled with tailing white spaces until
			// max. 20 length,
			// plus OUT:
			// 30 characters for arg 4, filled with tailing white spaces until
			// max. 30 length,
			sb.append(String.format("%-30s %-20s IN: %-30s OUT: %-30s", var, act.getName(),
					collectionToString(writesBeforeActivity.getPoss()),
					collectionToString(writesAfterActivity.getPoss())));

			logger.info(sb.toString());
		}
	}

	/**
	 * Translate collection to string
	 * 
	 * @param set
	 * @return
	 */
	protected static String collectionToString(Set<BPELExtensibleElement> set) {
		StringBuffer sb = new StringBuffer();
		if (set.size() == 0)
			return "[]";

		Iterator<BPELExtensibleElement> it = set.iterator();
		BPELExtensibleElement element = it.next();
		sb.append("[");
		if (element instanceof Activity)
			sb.append(((Activity) element).getName());
		else
			sb.append(Utility.dumpEE(element));

		while (it.hasNext()) {
			sb.append(", ");
			BPELExtensibleElement next = it.next();
			if (next instanceof Activity)
				sb.append(((Activity) next).getName());
			else
				sb.append(Utility.dumpEE(next));
		}
		sb.append("]");
		return sb.toString();
	}
}
