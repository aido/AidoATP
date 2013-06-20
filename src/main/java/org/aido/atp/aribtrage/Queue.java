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

import java.util.Iterator;
import java.util.NoSuchElementException;

public class Queue<Item> implements Iterable<Item> {
	private int N;         // number of elements on queue
	private Node first;    // beginning of queue
	private Node last;     // end of queue

	// helper linked list class
	private class Node {
		private Item item;
		private Node next;
	}

	/**
	* Create an empty queue.
	*/
	public Queue() {
		first = null;
		last  = null;
		N = 0;
		assert check();
	}

	/**
	* Is the queue empty?
	*/
   public boolean isEmpty() {
		return first == null;
	}

	/**
	* Add the item to the queue.
	*/
    public void enqueue(Item item) {
		Node oldlast = last;
		last = new Node();
		last.item = item;
		last.next = null;
		if (isEmpty()) first = last;
		else           oldlast.next = last;
		N++;
		assert check();
	}

	/**
	* Remove and return the item on the queue least recently added.
	* @throws java.util.NoSuchElementException if queue is empty.
	*/
    public Item dequeue() {
		if (isEmpty()) throw new NoSuchElementException("Queue underflow");
		Item item = first.item;
		first = first.next;
		N--;
		if (isEmpty()) last = null;   // to avoid loitering
		assert check();
		return item;
	}

	// check internal invariants
    private boolean check() {
		if (N == 0) {
			if (first != null) return false;
			if (last  != null) return false;
		}
		else if (N == 1) {
			if (first == null || last == null) return false;
			if (first != last)                 return false;
			if (first.next != null)            return false;
		}
		else {
			if (first == last)      return false;
			if (first.next == null) return false;
			if (last.next  != null) return false;

			// check internal consistency of instance variable N
			int numberOfNodes = 0;
			for (Node x = first; x != null; x = x.next) {
			numberOfNodes++;
			}
			if (numberOfNodes != N) return false;

			// check internal consistency of instance variable last
			Node lastNode = first;
			while (lastNode.next != null) {
			lastNode = lastNode.next;
			}
			if (last != lastNode) return false;
		}

		return true;
	}


	/**
	* Return an iterator that iterates over the items on the queue in FIFO order.
	*/
	public Iterator<Item> iterator()  {
		return new ListIterator();  
	}

	// an iterator, doesn't implement remove() since it's optional
	private class ListIterator implements Iterator<Item> {
		private Node current = first;

		public boolean hasNext()  { return current != null;                     }
		public void remove()      { throw new UnsupportedOperationException();  }

		public Item next() {
			if (!hasNext()) throw new NoSuchElementException();
			Item item = current.item;
			current = current.next; 
			return item;
		}
	}
}
