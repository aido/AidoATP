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

public class Stack<Item> implements Iterable<Item> {
    private int N;          // size of the stack
    private Node first;     // top of stack

    // helper linked list class
    private class Node {
        private Item item;
        private Node next;
    }

   /**
     * Create an empty stack.
     */
    public Stack() {
        first = null;
        N = 0;
        assert check();
    }

   /**
     * Add the item to the stack.
     */
    public void push(Item item) {
        Node oldfirst = first;
        first = new Node();
        first.item = item;
        first.next = oldfirst;
        N++;
        assert check();
    }

    // check internal invariants
    private boolean check() {
        if (N == 0) {
            if (first != null) return false;
        }
        else if (N == 1) {
            if (first == null)      return false;
            if (first.next != null) return false;
        }
        else {
            if (first.next == null) return false;
        }

        // check internal consistency of instance variable N
        int numberOfNodes = 0;
        for (Node x = first; x != null; x = x.next) {
            numberOfNodes++;
        }
        if (numberOfNodes != N) return false;

        return true;
    }


   /**
     * Return an iterator to the stack that iterates through the items in LIFO order.
     */
	public Iterator<Item> iterator() {
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

