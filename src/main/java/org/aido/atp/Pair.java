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

/**
* HashMap compatible Pair class class.
*
* @author Aido
*/

public class Pair<F, S> {
	private F first;
	private S second;

	public Pair(F first, S second) {
		super();
		this.first = first;
		this.second = second;
	}

	public int hashCode() {
		int hashFirst = first != null ? first.hashCode() : 0;
		int hashSecond = second != null ? second.hashCode() : 0;

		return (hashFirst + hashSecond) * hashSecond + hashFirst;
	}

	public boolean equals(Object other) {
		if (other instanceof Pair) {
			Pair otherPair = (Pair) other;
			return 
			((  this.first == otherPair.first ||
			( this.first != null && otherPair.first != null &&
			this.first.equals(otherPair.first))) &&
			(      this.second == otherPair.second ||
			( this.second != null && otherPair.second != null &&
			this.second.equals(otherPair.second))) );
		}

		return false;
	}

	public String toString()
	{ 
		return "(" + first + ", " + second + ")"; 
	}

	public F getFirst() {
		return first;
	}

	public void setFirst(F first) {
		this.first = first;
	}

	public S getSecond() {
		return second;
	}

	public void setSecond(S second) {
		this.second = second;
	}
}