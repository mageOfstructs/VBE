package vbe.exceptions.fs;

import vbe.exceptions.NotFoundException;

public class LineNotFoundException extends NotFoundException {
	private static final long serialVersionUID = -7119707289871895450L;
	private String path, pattern;
	
	public LineNotFoundException(String path, String pattern) {
		super(pattern, path + " does not contain a line with the pattern '" + pattern + "'");
	}
	
	public String getMessage() {
		return "LineNotFoundException; path: " + path + "; pattern: " + pattern + " : " + super.getMessage();
	}
}
