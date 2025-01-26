package vbe.exceptions;

import vbe.commands.CommandHandler;

public class CommandSyntaxException extends GeneralException {
	private static final long serialVersionUID = -7323343036095458663L;
	private int indexOfError;
	private String rawCommand;

	private static String calcSpaces(int indexOfError) {
		String ret = "";
		for (int i = 0; i < indexOfError+35+CommandHandler.getComName().length()+(indexOfError/10); i++) {
			ret += " ";
		}
		return ret + "^";
	}
	
	public CommandSyntaxException(String rawCommand, int indexOfError) {
		this(rawCommand, indexOfError, "Invalid Syntax at position " + indexOfError + " in: " + rawCommand + "\n" + calcSpaces(indexOfError));
	}
	
	public CommandSyntaxException(String rawCommand, int indexOfError, String msg) {
		super(msg);
		this.indexOfError = indexOfError;
		this.rawCommand = rawCommand;
	} 
	
	public String getMessage() {
		return "Invalid Syntax at position " + indexOfError + " in: " + rawCommand + "; " + super.getMessage(false) + "\n" + calcSpaces(indexOfError) + "\n: " + super.getMessage();
	}
}
