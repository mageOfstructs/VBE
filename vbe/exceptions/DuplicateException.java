package vbe.exceptions;

public class DuplicateException extends GeneralException {
	private static final long serialVersionUID = -8072598805692511012L;

	public DuplicateException(String duplicateName, String type) {
		super(type + " '" + duplicateName + "' already exists");
	}
	
	public DuplicateException(String msg) {
		super(msg);
	}
}
