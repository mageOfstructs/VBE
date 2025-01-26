package vbe.usersystem;

import java.io.Serializable;
import java.util.ArrayList;

import newworld.fs.FileSystem;
import newworld.fs.FileSystem.FS_CONFIGS;
import vbe.SystemElement;
import vbe.exceptions.fs.FsElementNotFoundException;
import vbe.exceptions.fs.WrongElementType;

public class Group extends SystemElement implements IGroup, Serializable {	
	private static final long serialVersionUID = -5688129401295099009L;
	private int id, lineIndex = -1;
	private ArrayList<IUser> userlist;
	
	public Group(String name) {
		this(name, new ArrayList<IUser>(), true);
	}
	
	public Group(String name, boolean updateGroup) {
		this(name, new ArrayList<IUser>(), updateGroup);
	}
	
	public Group(String name, ArrayList<IUser> userlist, boolean updateGroup) {
		super(name);
		this.id = UserSystem.getGroupId();
		this.userlist = userlist;
		try {
			if (updateGroup) updateGroupFile();
		} catch (FsElementNotFoundException | WrongElementType e) {
			System.out.println(e);
		}
	}

	public Group(String name, int gid, ArrayList<IUser> users) {
		super(name);
		this.id = gid;
		this.userlist = users;
	}

	public void updateGroupFile() throws FsElementNotFoundException, WrongElementType {
		String line = name + ":x:" + id + ":" + getUsernames();
		
		if (lineIndex == -1) {
			FileSystem.getOnlyInstance().addLine(FS_CONFIGS.FS_GROUP, false, line);
			lineIndex = FileSystem.getOnlyInstance().length(FS_CONFIGS.FS_GROUP)-1;
		}
		else FileSystem.getOnlyInstance().setLine(FS_CONFIGS.FS_GROUP, lineIndex, line);
	}
	
	public void addUser(IUser user) throws FsElementNotFoundException, WrongElementType {
		if (!hasUser(user)) {
			userlist.add(user);
			updateGroupFile();
		}
	}
	
	@Override
	public void remUser(IUser user) throws FsElementNotFoundException, WrongElementType {
		if (hasUser(user)) {
			userlist.remove(user);
			updateGroupFile();
		}
	}
	
	public String toString() {
		return "[Group] " + getName();
	}

	public int getId() {
		return id;
	}
	
	public String getUsernames() {
		String ret = "";
		for (int i = 0; i < userlist.size()-1; i++) ret += userlist.get(i).getName() + ",";
		return userlist.size() > 0 ? ret + userlist.get(userlist.size()-1).getName() : "";
	}

	@Override
	public boolean hasUser(IUser user) {
		for (IUser u : userlist) {
			if (u.getName().equals(user.getName())) return true;
		}
		return false;
	}
	
	public void setLineIndex(int i) { lineIndex = i; }
}
