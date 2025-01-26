package vbe.usersystem;

import vbe.ISystemElement;
import vbe.exceptions.fs.FsElementNotFoundException;
import vbe.exceptions.fs.WrongElementType;

public interface IGroup extends ISystemElement {

	int getId();

	void addUser(IUser user) throws FsElementNotFoundException, WrongElementType;
	
	void remUser(IUser user) throws FsElementNotFoundException, WrongElementType;
	
	void updateGroupFile() throws FsElementNotFoundException, WrongElementType;

	boolean hasUser(IUser user);

	String getUsernames();

	void setLineIndex(int i);
}
