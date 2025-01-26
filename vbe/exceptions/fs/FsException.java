package vbe.exceptions.fs;

import vbe.exceptions.GeneralException;

public class FsException extends GeneralException {
	private static final long serialVersionUID = 8995284790289472227L;
	private String path;
	public FsException(String path, String message) {
		super(message);
		this.path = path;
	}
	
	public String getMessage() {
		return "FileSystemException; Path: " + path + " : " + super.getMessage();
	}
}