package vbe.exceptions.fs;

import vbe.exceptions.NotFoundException;

public class FsElementNotFoundException extends NotFoundException {
	private static final long serialVersionUID = -4026042793843109643L;

	public FsElementNotFoundException(String path) {
		super(path, path + ": No such file or directory");
	}
	
	public String getMessage() {
		return "FsElementNotFoundException : " + super.getMessage();
	}
}
