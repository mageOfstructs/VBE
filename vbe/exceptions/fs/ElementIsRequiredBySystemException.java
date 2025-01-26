package vbe.exceptions.fs;

public class ElementIsRequiredBySystemException extends FsException {

	private static final long serialVersionUID = -2296492635127031951L;

	public ElementIsRequiredBySystemException(String command, String path) {
		super(path, path + " is required by the system to function properly. You can disable this by starting the application with the --system-level flag.");
	}

	public String getMessage() {
		return "ElementIsRequiredBySystemException : " + super.getMessage();
	}
}
