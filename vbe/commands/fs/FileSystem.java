package vbe.commands.fs;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import newworld.fs.IDirForCommercialUse;
import vbe.GlobalVars;
import vbe.ISystemElement;
import vbe.commands.CommandHandler;
import vbe.commands.Commands;
import vbe.exceptions.GeneralException;
import vbe.exceptions.fs.FsElementNotFoundException;
import vbe.exceptions.fs.WrongElementType;
import vbe.exceptions.usersystem.AccessDeniedException;
import vbe.usersystem.UserSystem;

public class FileSystem extends GlobalVars {
	public static final boolean[] DEFAULT_PERMS = { true, true, true, true, false, true, true, false, true }, 
			NO_OTHERS = { true, true, true, true, false, true, false, false, false };
	public static final String NAME_PREFIX = "Element0";
	public static IDirectory root;
	
	/* pointers to important places inside the root directory */
	private static IDirectory curDir;
	private static IDirectory home;
	private static IDirectory rootHome;
	private static IDirectory etc;
	private static IFile hostname;
	
	private static String curPath;
	private static int elNum = 0;
	private static ISystemElement lastCreationPointer;
	
	public static IDirectory resolvePath(String path) throws FsElementNotFoundException, WrongElementType, AccessDeniedException {
		if (path.isEmpty() || path.equals("/"))
			return root;
		IDirectory ret = (path.indexOf('/') != -1) ? FileSystem.root : FileSystem.curDir;
		ArrayList<String> dirs = new ArrayList<String>();
		int start = (path.charAt(0) != '/') ? 0 : 1, end = (path.indexOf('/', start) != -1) ? path.indexOf('/', start) : path.length();
		dirs.add(path.substring(start, end)); // get first directory name
		while (start < path.length() && end < path.length()) { // get other directory names
			start = end+1;
			end = (path.indexOf('/', start) != -1) ? path.indexOf('/', start) : path.length();
			dirs.add(path.substring(start, end));
		}
		
		for (int i = 0; i < dirs.size(); i++) {
			String dir = dirs.get(i);
			if (dir.equals("~") && i == 0)
				ret = (IDirectory) UserSystem.curUser.getHome();
			else if (dir.equals(".") && i == 0) // check hardcoded names
				ret = FileSystem.getCurDir();
			else if (dir.equals("..")) // check hardcoded names
				ret = ret.getParentDir();
			else // must be a directory inside the working one
				ret = (IDirectory) ret.getElementFromContentByName(dir, true);
		}
		if (!ret.checkPerms(0)) throw new AccessDeniedException(0, ret.getPath());
		return ret;
	}
	
	public static String getPath(IFsElement dir) {
		String ret = "/" + dir.getName();
		IFsElement tmp = dir.getParentDir();
		while (tmp != null && !tmp.equals(root)) {
			ret = "/" + tmp.getName() + ret;
			tmp = tmp.getParentDir();
		}
		return ret;
	}
	
	public static IDirectory getCurDir() { return curDir; }
	public static void setCurDir(IDirectory iDirectory) { curDir = iDirectory; }

	public static String getCurPath() { return curPath; }
	public static void setCurPath(String newPath) { curPath = newPath; }
	
	/**
	 * Creates an empty BASH environment
	 * MUST be executed before anything else! (the other classes are very dependent on some of these variables
	 * @param args
	 * @throws GeneralException 
	 * @throws AccessDeniedException 
	 * @throws  
	 */
	public static void init(String[] args) throws AccessDeniedException, GeneralException {
		CommandHandler.initPosFlags(); // very important
		
		System.out.println("Starting Linux...");
	
		System.out.print("Updating clock...");
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");
		LocalDateTime now = LocalDateTime.now();
	    GlobalVars.curDate = dtf.format(now);
	    System.out.println("done\n");
			
		System.out.println("Constructing file system...");
		
		System.out.print("Mounting root partition...");
		try {
			ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream("root.vbe")));
			FileSystem.root = (IDirectory) in.readObject();
			in.close();
			System.out.println("done");
			
			rootHome = (IDirectory) root.getElementFromContentByName("root", true);
			UserSystem.ROOT.setUserHome((IDirForCommercialUse) rootHome, false);
			
			System.out.print("Mounting home partition...");
			home = (IDirectory) root.getElementFromContentByName("home", true);
			System.out.println("done");
			
			System.out.print("Setting up system configurations...");
			etc = (IDirectory) root.getElementFromContentByName("etc", true);
			hostname = (IFile) etc.getElementFromContentByName("hostname", false);
			System.out.println("done");
		} catch (IOException | ClassNotFoundException e) { // create a new root
			System.out.println("Failed to read root.vbe: " + e.getMessage());
			System.out.print("Attempting to create one...");
			root = new Directory("", GlobalVars.curDate, UserSystem.ROOT, UserSystem.ROOTGROUP, DEFAULT_PERMS, null, new ArrayList<FsElement>());
			root.setParentDir(FileSystem.root);
			
			rootHome = new Directory("root", GlobalVars.curDate, UserSystem.ROOT, UserSystem.ROOTGROUP, NO_OTHERS, root, new ArrayList<FsElement>());
			root.addDir(rootHome);
			UserSystem.ROOT.setUserHome((IDirForCommercialUse) rootHome, false);
			System.out.println("done");
			
			System.out.print("Mounting home partition...");
			home = (IDirectory) root.addDir(new Directory("home", GlobalVars.curDate, UserSystem.ROOT, UserSystem.ROOTGROUP, DEFAULT_PERMS, root, new ArrayList<FsElement>()));
			System.out.println("done\n");
			
			System.out.print("Setting up system configurations...");
			etc = new Directory("etc", GlobalVars.curDate, UserSystem.ROOT, UserSystem.ROOTGROUP, DEFAULT_PERMS, root, new ArrayList<FsElement>());
			root.addDir(etc);
			etc.addFile("passwd");
			etc.addFile("group");
			etc.addFile("exit.conf").addLine("SaveOnExit=1", true);
			hostname = ((IFile) etc.addFile("hostname"));
			hostname.addLine("localhost", true);
			System.out.println("done");
			
			UserSystem.ROOT.updateConfigFile();
			UserSystem.ROOTGROUP.updateGroupFile();
			UserSystem.SUDO.updateGroupFile();
		}
		System.out.print("Logging into the default user...");
		UserSystem.SUDO.addUser(UserSystem.ROOT);
		UserSystem.ROOTGROUP.addUser(UserSystem.ROOT);
		
		UserSystem.users.add(UserSystem.ROOT);
		UserSystem.groups.add(UserSystem.ROOTGROUP);
		UserSystem.groups.add(UserSystem.SUDO);
		UserSystem.curUser = UserSystem.ROOT;
		Commands.cd(Commands.EMPTY_FLAGARR, UserSystem.curUser.getHomePath());
		System.out.println("done");
		
		lastCreationPointer = null; // clear temporary variable
		root.updateSize();
		if (GlobalVars.comFuncString.indexOf("--system-level", args) == -1) { // if user starts application in normal mode, prevent the deletion of important elements
			rootHome.setSystemLocation();
			home.setSystemLocation();
			etc.setSystemLocation();
		}
	}

	public static void setLastCreationPointer(ISystemElement ret) {
		lastCreationPointer = ret;
	}

	public static ISystemElement getLastCreationPointer() {
		ISystemElement ret = lastCreationPointer;
		lastCreationPointer = null;
		return ret;
	}

	public static IDirectory getRootHome() {
		return rootHome;
	}
	
	public static int getElNum() {
		return elNum;
	}

	public static void incElNum() {
		elNum++;
	}

	public static String dump() {
		return "curPath: " + curPath + "\n"
				+ root.toString(0);
	}
	
	public static String getHostname() { return hostname.getPublicContent()[0]; }

	public static IDirectory getEtc() {
		return etc;
	}
	
	public static boolean isPath(String path) {
		return path.indexOf('/') != -1;
	}
}
