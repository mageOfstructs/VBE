package vbe;

import java.util.Scanner;

public class GlobalVars {
	public static final CommonFunctions<String> comFuncString = new CommonFunctions<String>();
	public static final CommonFunctions<Integer> comFuncInteger = new CommonFunctions<>();
	public static final CommonFunctions<Character> comFuncChar = new CommonFunctions<>();
	
	static Scanner in = new Scanner(System.in);

	public static String curDate = "00/00/0000";
	public static boolean verboseErrMsg = false;
	private static int reportCount = 0;

	public static final String ERR_PREFIX = "|#####################|\n"
										+   "|=====FATAL=ERROR=====|\n"
										+   "|#####################|\n";
	
	public static Scanner getIn() { return in; }
	public static int getRepCount() {
		return reportCount++;
	}
}