package vbe.exceptions.usersystem;

import vbe.usersystem.UserSystem;

public class AccessDeniedException extends UserException {
	private static final long serialVersionUID = -3322989785647581504L;
	private static final String[] OPERATIONS = { "read", "write to", "execute" };
	private String path;
	private int operation;
	public AccessDeniedException(int operation, String path) {
		super(UserSystem.curUser, "Permission denied! Unable to " + OPERATIONS[operation] + " " + path);
		this.path = path;
		this.operation = operation;
	}
	
	public AccessDeniedException(String msg) {
		super(UserSystem.curUser, msg);
	}
	
	public AccessDeniedException() {
		this("Permission denied.");
	}
	
	public String getMessage() {
		return "AccessDeniedException" + "; operation: " + OPERATIONS[operation] + " " + path + " : " + super.getMessage();
	}

	public String getPath() { return path; }
}
