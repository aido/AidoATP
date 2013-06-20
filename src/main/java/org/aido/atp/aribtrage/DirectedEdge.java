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

public class DirectedEdge { 
    private final int v;
    private final int w;
    private final double weight;

   /**
     * Create a directed edge from v to w with given weight.
     */
    public DirectedEdge(int v, int w, double weight) {
        this.v = v;
        this.w = w;
        this.weight = weight;
    }

   /**
     * Return the vertex where this edge begins.
     */
    public int from() {
        return v;
    }

   /**
     * Return the vertex where this edge ends.
     */
    public int to() {
        return w;
    }

   /**
     * Return the weight of this edge.
     */
    public double weight() {
		return weight;
	}
}
