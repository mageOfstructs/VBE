package vbe.util;

import java.util.Iterator;

public class List<E> implements Iterable<E> {

	private ListEl<E> start;
	
	private class ListEl<D> {
		public D value;
		public ListEl<D> next;
		
		public ListEl(D value, ListEl<D> next) {
			this.value = value;
			this.next = next;
		}
		
		public int size(int curSize) {
			if (next != null)
				return next.size(curSize+1);
			return curSize;
		}
	}
	
	public List(E value) {
		start = new ListEl<E>(value, null);
	}
	
	public List() {
		start = new ListEl<E>(null, null);
	}

	public void insert(int index, E value) {
		ListEl<E> el = start;
		int i = 0;
		while (i < index && el.next != null) {
			i++; el = el.next;
		}
		el.next = new ListEl<E>(value, el.next);
	}
	
	public void push(E value) {
		ListEl<E> lastEl = start;
		while (lastEl.next != null)
			lastEl = lastEl.next;
		lastEl.next = new ListEl<E>(value, null);
	}
	
	public void remove(int index) {
		int i = 0;
		ListEl<E> elToRemoveFrom = start;
		while (i < index-1 && elToRemoveFrom.next != null) {
			i++; elToRemoveFrom = elToRemoveFrom.next;
		}
		elToRemoveFrom.next = elToRemoveFrom.next.next; // make an orphan
	}
	
	public E get(int index) {
		int i = 0;
		ListEl<E> el = start;
		while (i < index && el.next != null) {
			i++; el = el.next;
		}
		return el.value;
	}
	
	public int size() {
		return start.size(1);
	}
	
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			private ListEl<E> cur = start;
			public E next() {
				E ret = cur.value;
				cur = cur.next;
				return ret;
			}
			public boolean hasNext() {
				return cur.next != null;
			}
		};
	}
}
