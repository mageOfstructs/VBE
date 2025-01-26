package newworld.fs;

import java.io.Serializable;
import java.util.Iterator;

import vbe.CommonFunctions;
import vbe.GlobalVars;
import vbe.exceptions.fs.FsException;
import vbe.usersystem.IGroup;
import vbe.usersystem.IUser;
import vbe.usersystem.UserSystem;

abstract class FsElement implements IModifiableFsElement, Serializable {
	private static final long serialVersionUID = 3049488980822189767L;

	protected static final char[] PERMS_PATTERN = {'r', 'w', 'x', 'r', 'w', 'x', 'r', 'w', 'x'};
	
	private String name, dateCreatedAt;
	private IUser user;
	private IGroup group;
	protected int size;
	private int links;
	private boolean[] perms;
	private Directory parentDir;
	private boolean isSystemLocation;
	
	protected void updateSize() {
		size = name.length()*2+69;
	}
	
	public FsElement(String name, String dateCreatedAt, IUser user, IGroup group, boolean[] perms, Directory parentDir) {
		this.name = name;
		this.dateCreatedAt = dateCreatedAt;
		this.user = user;
		this.group = group;
		this.perms = perms.clone();
		this.parentDir = parentDir;
		this.isSystemLocation = false;
		
		links = 2;
	}
	
	public boolean checkPerms(int op) {
		try {
			if (op < 0 || op > 2)
				throw new FsException(getPath(), "Invalid File Modifier Bit!");
			return UserSystem.isRoot() || perms[6+op] || (GlobalVars.comFuncString.indexOf(group.getName(), UserSystem.curUser.getGroupNames()) != -1 && perms[3+op]) || (UserSystem.curUser.equals(user) && perms[op]);
		} catch (FsException e) { // whatever happened, we won't allow permission
			return false;
		}
	}
	
	public String getPath() {
		return FileSystem.getOnlyInstance().getPath(this);
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the dateCreatedAt
	 */
	public String getDateCreatedAt() {
		return dateCreatedAt;
	}

	/**
	 * @return the user
	 */
	public IUser getUser() {
		return user;
	}
	/**
	 * @param user the user to set
	 */
	public void setUser(IUser user) {
		this.user = user;
	}
	/**
	 * @return the group
	 */
	public IGroup getGroup() {
		return group;
	}
	/**
	 * @param group the group to set
	 */
	public void setGroup(IGroup group) {
		this.group = group;
	}
	/**
	 * @return the size
	 */
	public int getSize() {
		return size;
	}

	/**
	 * @return the links
	 */
	public int getLinks() {
		return links;
	}

	/**
	 * @return the perms
	 */
	public boolean[] getPerms() {
		return perms;
	}
	/**
	 * @param perms the perms to set
	 */
	public void setPerms(boolean[] perms) {
		this.perms = perms;
	}
	/**
	 * @return the parentDir
	 */
	public Directory getParentDir() {
		return parentDir;
	}
	/**
	 * @param parentDir the parentDir to set
	 */
	public void setParentDir(Directory parentDir) {
		this.parentDir = parentDir;
	}
	
	public abstract Iterator iterator();
	
	public abstract int length();
	
	public abstract void remove(int index);
	
	public abstract FsElement clone();
	
	public boolean isSystemLocation() { return isSystemLocation; }
	public void setSystemLocation() { isSystemLocation = true; }

	public String permsToString() {
		char[] ret = PERMS_PATTERN.clone();
		for (int i = 0; i < perms.length; i++)
			if (!perms[i]) ret[i] = '-';
		return new String(ret);
	}
	
	/**
	 * compares the object's name to the parameter. Wildcards also are handled.
	 * @param name
	 * @return
	 */
	public boolean equals(String name) {
		if (FileSystem.hasWildCards(name) == 0) return this.name.equals(name);
		int i = 0, nameIndex = 0, endIndex, endIndexQ, QWildcardsCount;
		String ownName = this.name;
		boolean equals = true;
		while (i < name.length() && nameIndex < ownName.length() && equals) {
			if (name.charAt(i) == '*') {
				do i++; while (i < name.length() && name.charAt(i) == '*');
				endIndex = name.indexOf('*', i);
				endIndexQ = name.indexOf('?', i);
				if (endIndex == -1 || endIndex > endIndexQ) endIndex = endIndexQ;
				if (endIndex == -1) endIndex = name.length();
				String nextMatch = name.substring(i, endIndex);
				i = ownName.indexOf(nextMatch, i)+nextMatch.length();
				if (i == nextMatch.length()-1) equals = false;
				continue;
			} else if (name.charAt(i) == '?') {
				QWildcardsCount = 0;
				do {
					i++;
					QWildcardsCount++;
				} while (i < name.length() && name.charAt(i) == '?');
				equals = (nameIndex += QWildcardsCount) <= ownName.length();
				continue;
			} else equals = name.charAt(i) == ownName.charAt(nameIndex);
			i++;
			nameIndex++;
		}	
		return equals;
	}
	
	public String toString(int depth) {
		return name + ": FsElement\n" + 
		CommonFunctions.duplicateChar('\t', depth) + "dateCreatedAt: " + dateCreatedAt + "\n" + 
		CommonFunctions.duplicateChar('\t', depth) + "Owner: " + user + "\n" + 
		CommonFunctions.duplicateChar('\t', depth) + "Group: " + group + "\n" + 
		CommonFunctions.duplicateChar('\t', depth) + "Permissions: " + permsToString() + "\n" + 
		CommonFunctions.duplicateChar('\t', depth) + "isSystemLocation: " + isSystemLocation + "\n\n";
	}
	
	public String toString() {
		return getPath();
	}
}
