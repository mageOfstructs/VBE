package vbe.exceptions;

import vbe.commands.CommandHandler;

public class GeneralException extends Exception {
	private static final long serialVersionUID = -8724335818582215699L;
	private String command;
	
	public GeneralException(String msg) {
		super(msg);
		this.command = CommandHandler.getComName();
	}
	
	public String getMessage() {
		return "GeneralException; Command: " + command + "; Message: " + super.getMessage();
	}
	
	public String getMessage(boolean verbose) {
		if (!verbose)
			return super.getMessage();
		return getMessage();
	}
}