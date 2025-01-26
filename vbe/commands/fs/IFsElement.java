package vbe.commands.fs;

import vbe.ISystemElement;
import vbe.usersystem.IGroup;
import vbe.usersystem.IUser;

public interface IFsElement extends ISystemElement {	
	String getName();
	IDirectory getParentDir();
	
	String toString();
	boolean checkPerms(int rwx);
	boolean[] getPerms();
	void setPerms(boolean[] intToBin);
	
	IUser getOwner();
	IGroup getGroup();
	void setOwner(IUser newOwner);
	void setGroup(IGroup newGroup);
	
	boolean isSystemLocation();
	void setSystemLocation();
	boolean isDirectory();
	void setPerms(boolean[] newPerms, int subsection);
	boolean[] computePerms(int mod, int specifier, boolean[] fileBits);
	String getPath();
	int getSize();	
	String permsToString();
	int getLinks();
}