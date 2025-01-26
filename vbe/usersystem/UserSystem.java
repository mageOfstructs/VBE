package vbe.usersystem;

import java.io.IOException;
import java.util.ArrayList;

import newworld.fs.FileSystem;
import newworld.fs.FileSystem.FS_CONFIGS;
import vbe.GlobalVars;
import vbe.commands.CommandHandler;
import vbe.exceptions.fs.LineNotFoundException;
import vbe.exceptions.usersystem.AccessDeniedException;
import vbe.exceptions.usersystem.UserElementNotFoundException;

public class UserSystem extends GlobalVars {
	public static ArrayList<IUser> users = new ArrayList<>();
	public static ArrayList<IGroup> groups = new ArrayList<>();
	
	public static final IGroup ROOTGROUP = new Group("root", false); // no clue how it got the right id here
	public static final IGroup SUDO = new Group("sudo", false);
	public static final IUser ROOT = new User("root", "123", null, "", UserSystem.ROOTGROUP, UserSystem.SUDO);
	
	public static IUser curUser = ROOT;
	
	private static IUser tmpUser; // a temp variable to hold the current user while the system is performing the action as root
	
	private static int userId = 1000;
	private static int groupId = 1000;
	
	public static IUser getUserByName(String username) throws UserElementNotFoundException {
		if (username == null) return users.get(0);
			for (IUser user : UserSystem.users)
				if (user.getName().equals(username)) { return user; }
		throw new UserElementNotFoundException(username, true);
	}
	
	public static IGroup getGroupByName(String groupname) throws UserElementNotFoundException {
		if (groupname == null) groups.get(0);
		for (IGroup group : groups)
			if (groupname.equals(group.getName())) { return group; }
		throw new UserElementNotFoundException(groupname, false);
	}
	
	public static void performOpAsRoot(String command) {
		performOpAsRoot(CommandHandler.handleTokens(command));
	}

	public static void performOpAsRoot(String[] tokens) {
		tmpUser = curUser; // save current user
		curUser = ROOT;
		
		performOpAsCurUser(tokens); // will be executed as root
		
		curUser = tmpUser;
	}
	
	private static void performOpAsCurUser(String[] tokens) {
		try {
			CommandHandler.handleCommand(tokens);
		} catch (IOException e) {
			System.out.println(GlobalVars.ERR_PREFIX + "Error while executing a read/write operation");
			e.printStackTrace();
		}
	}

	public static void performOpAsCurUser(String com) {
		performOpAsCurUser(CommandHandler.handleTokens(com));
	}
	
	public static boolean isRoot() { return curUser.equals(ROOT); }

	public static int getUserId() { return userId++; }

	public static int getGroupId() { return groupId++; }

	public static boolean doesGroupExist(String name) {
		for (IGroup group : groups)
			if (group.getName().equals(name)) return true;
		return false;
	}

	public static void delUser(IUser user) throws LineNotFoundException {
		FileSystem.getOnlyInstance().remLine(FS_CONFIGS.FS_PASSWD, FileSystem.getOnlyInstance().find(FS_CONFIGS.FS_PASSWD, user.getName()));
		users.remove(user);
	}

	public static boolean performPasswdCheck() throws AccessDeniedException {
		return curUser.performPasswdCheck();
	}

	public static IGroup getGroupById(int gid) {
		int low = 0, high = groups.size(), index = -1, mid = 0;
		while (low <= high && index == -1) {
			mid = (low + high) / 2;
			if (groups.get(mid).getId() == gid) index = mid;
			else if (groups.get(mid).getId() > gid) low = mid+1;
			else high = mid-1;
		}
		if (index == -1) return null;
		else return groups.get(index);
	}
}
