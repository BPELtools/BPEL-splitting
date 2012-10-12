package org.bpel4chor.splitprocess.dataflowanalysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.bpel4chor.splitprocess.pwdg.model.PWDGNode;
import org.bpel4chor.splitprocess.utils.ActivityUtil;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Variable;

/**
 * QueryWriterSet represents the tuple set "Q_s(a,x)", each tuple consists of a
 * set of queries on variable given and a set of writers that write on the
 * query.
 * 
 * <p>
 * 
 * <pre>
 * Q_s(a, x) = { ({x.m}, {w1, w2}), ({x.n}, {w3, w4}) } transform to tuples { ({.m}, {w1, w2}), ({.n}, {w3, w4}) }
 * if the whole variable x is queried, e.g. Q_s(a, x) = { ({x}, {w1, w2}), ({x.n}, {w3, w4}) }, 
 * after transform it looks like { ({}, {w1, w2}), ({.n}, {w3, w4}) }
 * </pre>
 * <p>
 * To get a QueryWriterSet, use
 * {@link AnalysisResultParser#parse(Activity, Variable, de.uni_stuttgart.iaas.bpel_d.algorithm.analysis.AnalysisResult)}
 * 
 * @since Feb 26, 2012
 * @author Daojun Cui
 */
public class QueryWriterSet {

	/** The query on variable to writers set map */
	protected Map<Set<String>, Set<Activity>> query2WriterMap = null;

	/** The reader */
	protected Activity act = null;

	/** The variable that is read */
	protected Variable var = null;
	
	protected Logger logger = Logger.getLogger(QueryWriterSet.class);

	/**
	 * This constructor is set to 'protected', so that user is supposed to use
	 * AnalysisResultParser.parse(act, var, analysis) to create a
	 * QueryWriterSet.
	 * 
	 * @param act
	 *            Reader Activity
	 * @param var
	 *            Variable that reader depends on
	 * @param query2WriterMap
	 *            QuerySet to WriterSet map
	 */
	protected QueryWriterSet(Activity act, Variable var, Map<Set<String>, Set<Activity>> query2WriterMap) {
		if (act == null || var == null || query2WriterMap == null)
			throw new NullPointerException();

		this.act = act;
		this.var = var;
		this.query2WriterMap = query2WriterMap;

	}

	/**
	 * Get the query sets, which are keySet of the map
	 * 
	 * @return
	 */
	public Set<Set<String>> querySets() {
		return query2WriterMap.keySet();
	}
	
	/**
	 * Get the writer sets, which are valueSet of the map
	 * @return
	 */
	public Collection<Set<Activity>> writerSets() {
		return query2WriterMap.values();
	}

	/**
	 * Get the writer set associated to the querySet given
	 * 
	 * @param querySet
	 *            Set of query string
	 * @return Set of writer
	 */
	public Set<Activity> get(Set<String> querySet) {
		return query2WriterMap.get(querySet);
	}

	/**
	 * Get the activity union of all writers in the query-writer-map
	 * 
	 * @return The writer activities
	 */
	public Set<Activity> getAllWriters() {

		Set<Activity> writers = new HashSet<Activity>();

		for (Set<Activity> wrtSet : query2WriterMap.values()) {
			writers.addAll(wrtSet);
		}

		return writers;

	}

	/**
	 * Return a QueryWriterSet that represents the Q_s(n, a, x), the writer in
	 * the writer sets must also present in the PWDG node n, the query set stay
	 * the same.
	 * <p>
	 * In other words, the writers in the writer set now must satisfy two
	 * conditions:
	 * <ol>
	 * <li>they are writers from Q_s(a, x)
	 * <li>they must present in the PWDG node n
	 * </ol>
	 * 
	 * @param node
	 *            PWDG node
	 * @return The QueryWriterSet that get filtered by the pwdg node. If there
	 *         none of the writers in the original queryWriterSet present in the
	 *         pwdg node, the returned QueryWriterSet is <b>empty</b>.
	 */
	public QueryWriterSet getQueryWriterSetFor(PWDGNode node) {

		Map<Set<String>, Set<Activity>> newQuery2WriterMap = new HashMap<Set<String>, Set<Activity>>();

		for (Set<String> querySet : query2WriterMap.keySet()) {
			Set<Activity> writerSet = query2WriterMap.get(querySet);
			Set<Activity> newWriterSet = new HashSet<Activity>();
			for (Activity act : writerSet) {
				if (node.getActivities().contains(act))
					newWriterSet.add(act);
			}
			if (newWriterSet.size() != 0) {
				// collect the tuple (query set : writer set) , whose writer
				// also present in the pwdg node
				newQuery2WriterMap.put(querySet, newWriterSet);
			}
		}

		mergeQuerySet(newQuery2WriterMap);

		QueryWriterSet newQWS = new QueryWriterSet(act, var, newQuery2WriterMap);
		return newQWS;
	}

	/**
	 * Merge the query sets in the Query2WriterSet
	 * <p>
	 * After the PWDG node filtering, it is possible now the following scenario
	 * happens.
	 * <p>
	 * Now the QueryWriterMap can look like:<br>
	 * 
	 * <pre>
	 * {
	 *   {.actNum}:{B},
	 *   {.amt}:{B}
	 * }
	 * </pre>
	 * 
	 * In this case the query sets should be merged together, as following.
	 * 
	 * <pre>
	 * {
	 *   {.actNum, .amt}:{B},
	 * }
	 * </pre>
	 * 
	 * @param query2WriterMap
	 *            The query set to writer set map
	 */
	protected void mergeQuerySet(Map<Set<String>, Set<Activity>> query2WriterMap) {

		if (query2WriterMap.size() == 0)
			// if the map is empty, don't bother
			return;

		Map<Set<String>, Set<Activity>> mergedQuery2WriterMap = new HashMap<Set<String>, Set<Activity>>();
		List<Set<String>> qsArray = new ArrayList<Set<String>>();
		qsArray.addAll(query2WriterMap.keySet());

		// boolean array to store whether the i-th query-writer entry is already
		// assessed.
		boolean[] visited = new boolean[qsArray.size()];
		for (int i = 0; i < visited.length; i++)
			visited[i] = false;

		// find all the query sets that have the same writer set
		// and merge them together as one query set, and save the query set to
		// writer set entry into the new map.
		for (int i = 0; i < qsArray.size(); i++) {
			if (visited[i] == false) {

				Set<String> mergedQS = new HashSet<String>();
				Set<String> qs = qsArray.get(i);
				Set<Activity> ws = query2WriterMap.get(qs);
				// collect query string
				mergedQS.addAll(qs);

				for (int j = i + 1; j < qsArray.size(); j++) {
					Set<String> nextQs = qsArray.get(j);
					Set<Activity> nextWs = query2WriterMap.get(nextQs);

					if (isEqual(ws, nextWs)) {
						// collect another query string
						mergedQS.addAll(nextQs);
						visited[j] = true;
					}
				}
				mergedQuery2WriterMap.put(mergedQS, ws);
				visited[i] = true;

			}
		}

		// double check
		for (int i = 0; i < visited.length; i++) {
			if (visited[i] == false)
				throw new IllegalStateException("Some entry in the qsArray is not assessed.");
		}

		query2WriterMap.clear();
		query2WriterMap.putAll(mergedQuery2WriterMap);
	}

	protected boolean isEqual(Set<Activity> actSet1, Set<Activity> actSet2) {
		return ActivityUtil.isEqual(actSet1, actSet2);
	}

	/**
	 * How many query set to writer set entries are there?
	 * 
	 * @return
	 */
	public int size() {
		return this.query2WriterMap.size();
	}

	public Activity getActivity() {
		return act;
	}

	public Variable getVariable() {
		return var;
	}
	
	public boolean isEmpty() {
		return this.query2WriterMap.isEmpty();
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("activity: ");
		sb.append(act.getName());
		sb.append(", variable: ");
		sb.append(var.getName());
		sb.append(", querySet2WriterSet:[");
		for(Set<String> qs : query2WriterMap.keySet()) {
			String qsString = qs.toString();
			StringBuffer wsSb = new StringBuffer();
			Set<Activity> ws = query2WriterMap.get(qs);
			wsSb.append("[");
			for(Activity w : ws) {
				wsSb.append(w.getName());
				wsSb.append(" ");
			}
			wsSb.append("]");
			sb.append(qsString);
			sb.append(":");
			sb.append(wsSb.toString());
			sb.append(" ");
		}
		sb.append("]");
		return sb.toString();
	}
	
}
