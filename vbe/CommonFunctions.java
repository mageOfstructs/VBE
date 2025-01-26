package vbe;

public class CommonFunctions<E> {	
	public int indexOf(E el, E[] arr) {
		int i = 0;
		while (i < arr.length && !arr[i].equals(el)) {
			i++;
		}
		return i < arr.length ? i : -1;
	}
	
	/**
	 * converts a number into a binary representation in a boolean array
	 * @param num
	 * @param maxDigits if the binary number has less digits the function will fill it up with 0s, if the binary number has more digits the function the closest number it can represent
	 * @return
	 */
	public static boolean[] intToBin(int num, int maxDigits) {
		boolean[] ret = new boolean[maxDigits];
		long pow2 = (long) Math.pow(2, maxDigits-1);
		int i = maxDigits-1; // start position MSB
		while (i > -1 && num != 0) {
			if (num >= pow2) {
				num -= pow2;
				ret[i] = true;
			}
			pow2 /= 2; i--;
		}
		return ret;
	}

	public static String duplicateChar(char c, int count) {
		String ret = "";
		for (int i = 0; i < count; i++)
			ret += c;
		return ret;
	}
}
