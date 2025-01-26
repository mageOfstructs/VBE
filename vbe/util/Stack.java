package vbe.util;

public class Stack<E> {
	private List<E> stack;
	
	public Stack() {
		stack = new List<E>();
	}

	public E top() { return stack.get(0); }
	public void push(E value) { stack.insert(0, value); }
	public E pop() {
		E ret = stack.get(0);
		stack.remove(0);
		return ret;
	}
} 
