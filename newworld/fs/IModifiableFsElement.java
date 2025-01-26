package newworld.fs;

import vbe.usersystem.IGroup;
import vbe.usersystem.IUser;

/**
 * special interface to only modify an elemnent's attribute
 */
public interface IModifiableFsElement {
	String getName();
	void setName(String name);
	
	String getDateCreatedAt();

	IUser getUser();
	void setUser(IUser user);
	
	IGroup getGroup();
	void setGroup(IGroup group);
	
	int getSize();
	
	int getLinks();
	
	boolean[] getPerms();
	void setPerms(boolean[] perms);
	
	Directory getParentDir();
	void setParentDir(Directory parentDir);
	
	boolean isSystemLocation();
	int length();
	boolean checkPerms(int i);
	String[] getChildPaths();
	default boolean isDir() { return this instanceof Directory; }
}
