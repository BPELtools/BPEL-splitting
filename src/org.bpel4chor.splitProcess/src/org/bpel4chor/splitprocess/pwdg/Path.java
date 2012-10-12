package org.bpel4chor.splitprocess.pwdg;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic path in the graph.
 * 
 * @since Feb 27, 2012
 * @author Daojun Cui
 */
public class Path<V> {

	List<V> vertices = new ArrayList<V>();

	/**
	 * Append vertex at the tail of the path
	 */
	public void append(V v) {
		vertices.add(v);
	}

	public void removeTail() {
		int size = vertices.size();
		if (size > 0)
			vertices.remove(vertices.size() - 1);
	}

	public List<V> getVertices() {
		return this.vertices;
	}

	public int length() {
		return this.vertices.size();
	}

	/** Get the node in the given index */
	public V get(int index) {
		if (index < 0 || index >= this.vertices.size())
			throw new IndexOutOfBoundsException("index: " + index + " path length:" + this.vertices.size());
		return this.vertices.get(index);
	}

	/** Test whether tha path contains the vertex given */
	public boolean contains(V v) {
		if (v == null)
			return false;
		for (V vertex : this.vertices) {
			if (vertex.equals(v))
				return true;
		}
		return false;
	}

	/**
	 * Return a copy of the path
	 */
	public Path<V> clone() {
		Path<V> clone = new Path<V>();
		for (int i = 0; i < vertices.size(); i++)
			clone.append(vertices.get(i));
		return clone;
	}
}