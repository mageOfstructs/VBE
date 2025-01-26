package vbe.exceptions.usersystem;

import vbe.exceptions.GeneralException;
import vbe.usersystem.IUser;

public class UserException extends GeneralException {
	private static final long serialVersionUID = -5250064026858064133L;
	private IUser user;
	
	public UserException(IUser curUser, String msg) {
		super(msg);
		this.user = curUser;
	}
	
	public String getMessage() {
		return "UserException; user: " + user + "; : " + super.getMessage();
	}
}
