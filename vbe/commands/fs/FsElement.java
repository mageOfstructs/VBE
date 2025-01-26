package vbe.commands.fs;

import java.io.Serializable;
import java.util.ArrayList;

import vbe.CommonFunctions;
import vbe.SystemElement;
import vbe.exceptions.fs.FsException;
import vbe.usersystem.IGroup;
import vbe.usersystem.IUser;
import vbe.usersystem.UserSystem;

public abstract class FsElement extends SystemElement implements IFsElement, Serializable {
	private static final long serialVersionUID = -7637537056490534149L;
	private static final int SIZE_OF_COMPONENTS = 55;

	protected static final char[] PERMS_PATTERN = {'r', 'w', 'x', 'r', 'w', 'x', 'r', 'w', 'x'};
	
	protected String dateCreatedAt = FileSystem.curDate;
	protected IUser owner;
	protected IGroup group;
	protected boolean[] perms = FileSystem.DEFAULT_PERMS.clone();
	protected boolean isDirectory = false;
	protected IDirectory parentDir = null;
	protected boolean isSystemLocation = false;
	protected int cachedSize = super.getSize() + SIZE_OF_COMPONENTS;

	
	/**
	 * computes the size of the object in bytes
	 * @return size of object in bytes
	 */
	public int getSize() {		
//		dateSize = 20; // date has a specified format
//		owner = 8; // technically it's a reference (and we assume we are on a 64-bit JVM)
//		group = 8; // technically it's a reference
//		perms = 9; // 9 * boolean (boolean = 1byte)
//		isDirectory = 1;
//		parentDir = 8; // technically it's a reference
//		isSystemLocation = 1; => 55 bytes
		return cachedSize;
	}
	
	public void updateSize() {
		cachedSize = super.getSize() + SIZE_OF_COMPONENTS;
	}
	
	public FsElement(String name, String dateCreatedAt, IUser owner, IGroup group, boolean[] perms, IDirectory parentDir, boolean isDirectory) {
		super(name);
		if (dateCreatedAt != null) // only specify what you need, everything else will be filled with defaults (except owner and group)
			this.dateCreatedAt = dateCreatedAt;
		this.owner = owner;
		this.group = group;
		if (perms != null)
			this.perms = perms.clone();
		this.isDirectory = isDirectory;
		this.parentDir = parentDir;
	}
	
	public String getDateCreatedAt() { return dateCreatedAt; }
	
	public IUser getOwner() { return owner; }
	public void setOwner(IUser newOwner) { owner = newOwner; }
	
	public IGroup getGroup() { return group; }
	public void setGroup(IGroup newGroup) { group = newGroup; }
	
	public boolean[] getPerms() { return perms; }
	public void setPerms(boolean[] newPerms) { perms = newPerms.clone(); }
	public void setPerms(boolean[] newPerms, int subsection) {
		if (newPerms.length < 3) throw new IllegalArgumentException("Not enough permission entries given!");
		perms[subsection*3] = newPerms[0];
		perms[subsection*3+1] = newPerms[1];
		perms[subsection*3+2] = newPerms[2];
	}
	
	public boolean[] computePerms(int mod, int specifier, boolean[] fileBits) {
		boolean[] tmp = fileBits.clone();
		switch (mod) {
		case 4:
			for (int j = 0; j < tmp.length; j++)
				if (!tmp[j]) tmp[j] = perms[specifier*3+j];
			break;
		case 5:
			for (int j = 0; j < tmp.length; j++)
				if (!tmp[j]) tmp[j] = perms[specifier*3+j];
				else tmp[j] = false;
		}
		return tmp;
	}
	
	public IDirectory getParentDir() { return parentDir; }
	
	public void setParentDir(IDirectory dirToMoveTo) { parentDir = dirToMoveTo; }
	
	public boolean isDirectory() { return isDirectory; }
	
	public String getPath() { return FileSystem.getPath(this); }
	
	public boolean isSystemLocation() { return isSystemLocation; }
	public void setSystemLocation() { isSystemLocation = true; }
	
	public String permsToString() {
		char[] ret = PERMS_PATTERN.clone();
		for (int i = 0; i < perms.length; i++)
			if (!perms[i]) ret[i] = '-';
		return new String(ret);
	}
	
	public boolean checkPerms(int rwx) {
		try {
			if (rwx < 0 || rwx > 2)
				throw new FsException(getPath(), "Invalid File Modifier Bit!");
			return UserSystem.isRoot() || perms[6+rwx] || (FileSystem.comFuncString.indexOf(group.getName(), UserSystem.curUser.getGroupNames()) != -1 && perms[3+rwx]) || (UserSystem.curUser.equals(owner) && perms[rwx]);
		} catch (FsException e) { // whatever happened, we won't allow permission
			return false;
		}
	}
	
	protected abstract ArrayList<?> getContent();
	public abstract Object[] getPublicContent();
	public abstract void listContent();
	
	public boolean equals(FsElement obj) {
		return getPath().equals(obj.getPath());
	}
	
	public String toString(int depth) {
		return name + ": " + (isDirectory ? "Directory" : "File") + "\n" + 
		CommonFunctions.duplicateChar('\t', depth) + "dateCreatedAt: " + dateCreatedAt + "\n" + 
		CommonFunctions.duplicateChar('\t', depth) + "Owner: " + owner + "\n" + 
		CommonFunctions.duplicateChar('\t', depth) + "Group: " + group + "\n" + 
		CommonFunctions.duplicateChar('\t', depth) + "Permissions: " + permsToString() + "\n" + 
		CommonFunctions.duplicateChar('\t', depth) + "isSystemLocation: " + isSystemLocation + "\n\n";
	}

	public abstract int getLinks();
}