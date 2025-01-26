package vbe.exceptions.usersystem;

import vbe.exceptions.NotFoundException;

public class UserElementNotFoundException extends NotFoundException {
	private static final long serialVersionUID = 125309811827791148L;

	public UserElementNotFoundException(String name, boolean isUser) {
		super(name, (isUser ? "User" : "Group") + " '" + name + "' does not exist!");
	}
	public UserElementNotFoundException(String msg) {
		super(msg);
	}

	public String getMessage() {
		return "UserElementNotFoundException; : " + super.getMessage();
	}
}
