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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BellmanFordSP {
	private Logger log;
	private double[] distTo;               // distTo[v] = distance  of shortest s->v path
	private DirectedEdge[] edgeTo;         // edgeTo[v] = last edge on shortest s->v path
	private boolean[] onQueue;             // onQueue[v] = is v currently on the queue?
	private Queue<Integer> queue;          // queue of vertices to relax
	private int cost;                      // number of calls to relax()
	private Iterable<DirectedEdge> cycle;  // negative cycle (or null if no such cycle)

	public BellmanFordSP(EdgeWeightedDigraph G, int s) {
		log = LoggerFactory.getLogger(BellmanFordSP.class);
		distTo  = new double[G.V()];
		edgeTo  = new DirectedEdge[G.V()];
		onQueue = new boolean[G.V()];
		for (int v = 0; v < G.V(); v++)
		distTo[v] = Double.POSITIVE_INFINITY;
		distTo[s] = 0.0;

		// Bellman-Ford algorithm
		queue = new Queue<Integer>();
		queue.enqueue(s);
		onQueue[s] = true;
		while (!queue.isEmpty() && !hasNegativeCycle()) {
			int v = queue.dequeue();
			onQueue[v] = false;
			relax(G, v);
		}

		assert check(G, s);
	}

	// relax vertex v and put other endpoints on queue if changed
	private void relax(EdgeWeightedDigraph G, int v) {
		for (DirectedEdge e : G.adj(v)) {
			int w = e.to();
			if (distTo[w] > distTo[v] + e.weight()) {
				distTo[w] = distTo[v] + e.weight();
				edgeTo[w] = e;
				if (!onQueue[w]) {
					queue.enqueue(w);
					onQueue[w] = true;
				}
			}
			if (cost++ % G.V() == 0)
			findNegativeCycle();
		}
	}

	// is there a negative cycle reachable from s?
	public boolean hasNegativeCycle() {
		return cycle != null;
	}

	// return a negative cycle; null if no such cycle
	public Iterable<DirectedEdge> negativeCycle() {
		return cycle;
	}

	// by finding a cycle in predecessor graph
	private void findNegativeCycle() {
		int V = edgeTo.length;
		EdgeWeightedDigraph spt = new EdgeWeightedDigraph(V);
		for (int v = 0; v < V; v++)
		if (edgeTo[v] != null)
		spt.addEdge(edgeTo[v]);

		EdgeWeightedDirectedCycle finder = new EdgeWeightedDirectedCycle(spt);
		cycle = finder.cycle();
	}

	// check optimality conditions: either 
	// (i) there exists a negative cycle reacheable from s
	//     or 
	// (ii)  for all edges e = v->w:            distTo[w] <= distTo[v] + e.weight()
	// (ii') for all edges e = v->w on the SPT: distTo[w] == distTo[v] + e.weight()
	private boolean check(EdgeWeightedDigraph G, int s) {

		// has a negative cycle
		if (hasNegativeCycle()) {
			double weight = 0.0;
			for (DirectedEdge e : negativeCycle()) {
				weight += e.weight();
			}
			if (weight >= 0.0) {
				log.error("Weight of negative cycle = " + weight);
				return false;
			}
		}

		// no negative cycle reachable from source
		else {

			// check that distTo[v] and edgeTo[v] are consistent
			if (distTo[s] != 0.0 || edgeTo[s] != null) {
				log.error("distanceTo[s] and edgeTo[s] inconsistent");
				return false;
			}
			for (int v = 0; v < G.V(); v++) {
				if (v == s) continue;
				if (edgeTo[v] == null && distTo[v] != Double.POSITIVE_INFINITY) {
					log.error("distTo[] and edgeTo[] inconsistent");
					return false;
				}
			}

			// check that all edges e = v->w satisfy distTo[w] <= distTo[v] + e.weight()
			for (int v = 0; v < G.V(); v++) {
				for (DirectedEdge e : G.adj(v)) {
					int w = e.to();
					if (distTo[v] + e.weight() < distTo[w]) {
						log.error("Edge " + e + " not relaxed");
						return false;
					}
				}
			}

			// check that all edges e = v->w on SPT satisfy distTo[w] == distTo[v] + e.weight()
			for (int w = 0; w < G.V(); w++) {
				if (edgeTo[w] == null) continue;
				DirectedEdge e = edgeTo[w];
				int v = e.from();
				if (w != e.to()) return false;
				if (distTo[v] + e.weight() != distTo[w]) {
					log.error("Edge " + e + " on shortest path not tight");
					return false;
				}
			}
		}
		return true;
	}
}