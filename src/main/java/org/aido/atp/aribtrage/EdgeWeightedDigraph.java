/**
* Copyright (c) 2013 Aido
* 
* This file is part of Aido ATP.
* 
* Aido ATP is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* Aido ATP is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with Aido ATP.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.aido.atp;

public class EdgeWeightedDigraph {
	private final int V;
	private int E;
	private Bag<DirectedEdge>[] adj;
	
	/**
	* Create an empty edge-weighted digraph with V vertices.
	*/
	public EdgeWeightedDigraph(int V) {
		if (V < 0) throw new IllegalArgumentException("Number of vertices in a Digraph must be nonnegative");
		this.V = V;
		this.E = 0;
		adj = (Bag<DirectedEdge>[]) new Bag[V];
		for (int v = 0; v < V; v++)
		adj[v] = new Bag<DirectedEdge>();
	}

	/**
	* Copy constructor.
	*/
	public EdgeWeightedDigraph(EdgeWeightedDigraph G) {
		this(G.V());
		this.E = G.E();
		for (int v = 0; v < G.V(); v++) {
			// reverse so that adjacency list is in same order as original
			Stack<DirectedEdge> reverse = new Stack<DirectedEdge>();
			for (DirectedEdge e : G.adj[v]) {
				reverse.push(e);
			}
			for (DirectedEdge e : reverse) {
				adj[v].add(e);
			}
		}
	}

	/**
	* Return the number of vertices in this digraph.
	*/
	public int V() {
		return V;
	}

	/**
	* Return the number of edges in this digraph.
	*/
	public int E() {
		return E;
	}

	/**
	* Add the directed edge e to this digraph.
	*/
	public void addEdge(DirectedEdge e) {
		int v = e.from();
		adj[v].add(e);
		E++;
	}

	/**
	* Return the edges incident from vertex v as an Iterable.
	* To iterate over the edges incident from vertex v in digraph G, use foreach notation:
	* <tt>for (DirectedEdge e : G.adj(v))</tt>.
	*/
	public Iterable<DirectedEdge> adj(int v) {
		return adj[v];
	}
}