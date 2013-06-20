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

public class EdgeWeightedDirectedCycle {
	private boolean[] marked;             // marked[v] = has vertex v been marked?
	private DirectedEdge[] edgeTo;        // edgeTo[v] = previous edge on path to v
	private boolean[] onStack;            // onStack[v] = is vertex on the stack?
	private Stack<DirectedEdge> cycle;    // directed cycle (or null if no such cycle)

	public EdgeWeightedDirectedCycle(EdgeWeightedDigraph G) {
		marked  = new boolean[G.V()];
		onStack = new boolean[G.V()];
		edgeTo  = new DirectedEdge[G.V()];
		for (int v = 0; v < G.V(); v++)
		if (!marked[v]) dfs(G, v);

		// check that digraph has a cycle
		assert check(G);
	}


	// check that algorithm computes either the topological order or finds a directed cycle
	private void dfs(EdgeWeightedDigraph G, int v) {
		onStack[v] = true;
		marked[v] = true;
		for (DirectedEdge e : G.adj(v)) {
			int w = e.to();

			// short circuit if directed cycle found
			if (cycle != null) return;

			//found new vertex, so recur
			else if (!marked[w]) {
				edgeTo[w] = e;
				dfs(G, w);
			}


			// trace back directed cycle
			else if (onStack[w]) {
				cycle = new Stack<DirectedEdge>();
				while (e.from() != w) {
					cycle.push(e);
					e = edgeTo[e.from()];
				}
				cycle.push(e);
			}
		}

		onStack[v] = false;
	}

	public boolean hasCycle() {
		return cycle != null;
	}

	public Iterable<DirectedEdge> cycle() {
		return cycle;
	}

	// certify that digraph is either acyclic or has a directed cycle
	private boolean check(EdgeWeightedDigraph G) {

		// edge-weighted digraph is cyclic
		if (hasCycle()) {
			// verify cycle
			DirectedEdge first = null, last = null;
			for (DirectedEdge e : cycle()) {
				if (first == null) first = e;
				if (last != null) {
					if (last.to() != e.from()) {
						System.err.printf("cycle edges %s and %s not incident\n", last, e);
						return false;
					}
				}
				last = e;
			}

			if (last.to() != first.from()) {
				System.err.printf("cycle edges %s and %s not incident\n", last, first);
				return false;
			}
		}
		return true;
	}
}
