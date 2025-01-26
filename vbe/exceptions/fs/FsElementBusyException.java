package vbe.exceptions.fs;

public class FsElementBusyException extends FsException {

	private static final long serialVersionUID = -601722545527068307L;

	public FsElementBusyException(String path) {
		super(path, "Directory or File Busy!");
	}	
	
	public String getMessage() {
		return "FsElementBusyException : " + super.getMessage();
	}
}
