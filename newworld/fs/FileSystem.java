package newworld.fs;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;

import vbe.GlobalVars;
import vbe.commands.CommandHandler;
import vbe.exceptions.DuplicateException;
import vbe.exceptions.GeneralException;
import vbe.exceptions.fs.FsElementNotFoundException;
import vbe.exceptions.fs.FsException;
import vbe.exceptions.fs.InvalidSymbolException;
import vbe.exceptions.fs.LineNotFoundException;
import vbe.exceptions.fs.WrongElementType;
import vbe.exceptions.usersystem.AccessDeniedException;
import vbe.usersystem.Group;
import vbe.usersystem.IUser;
import vbe.usersystem.User;
import vbe.usersystem.UserSystem;

public class FileSystem {
	public static final boolean[] DEFAULT_PERMS = { true, true, true, true, false, true, true, false, true }, 
			NO_OTHERS = { true, true, true, true, false, true, false, false, false };
	public static final String NAME_PREFIX = "Element";
	private static FileSystem onlyInstance;
	private static String date;
	private static float div = 97f/400;
	
	public enum FS_POINTERS {
		FS_ROOT, FS_ROOTHOME, FS_HOME, FS_ETC, FS_CUR
	};
	public enum FS_CONFIGS {
		FS_PASSWD, FS_GROUP, FS_SHADOW, FS_HOST, FS_EXIT
	}
	// private static Directory root, bin, boot, dev, etc, home, lib, lib64, mnt, opt, proc, rootHome, run, sBin, srv, sys, tmp, usr, var
	private static Directory root, rootHome, home, etc, boot, dev, tmp;
	private static File passwd, group, shadow, hostname, exit;
	private static IDirForCommercialUse curDir = root;

	public static FileSystem getOnlyInstance() {
		return onlyInstance;
	}
	
	public static char hasWildCards(String dir) {
		if (dir.indexOf('*') != -1) return '*';
		if (dir.indexOf('|') != -1) return '|';
		if (dir.indexOf('?') != -1) return '?';
		return 0;
	}
	
	private String elName;
	public ArrayList<Directory> splitPathAndName(String path) throws AccessDeniedException, FsElementNotFoundException, WrongElementType {
		ArrayList<Directory> workDir;
		if (path == null || path.indexOf('/') == -1) {
			elName = (path == null) ? FileSystem.generateFileName() : path;
			workDir = new ArrayList<>(1);
			workDir.add((Directory) getCurDir());
			return workDir;
		}
		workDir = FileSystem.getOnlyInstance().resolveDir(path.substring(0, path.lastIndexOf('/')));
		elName = path.substring(path.lastIndexOf('/')+1);
		return workDir;
	}
	
	public String getPath(IModifiableFsElement el) {
		String ret = "/" + el.getName();
		IModifiableFsElement tmp = el.getParentDir();
		while (tmp != null && !tmp.equals(root)) {
			ret = "/" + tmp.getName() + ret;
			tmp = tmp.getParentDir();
		}
		return ret;
	}
	
	public static File resolveConfig(FS_CONFIGS c) {
		switch (c) {
		case FS_PASSWD:
			return passwd;
		case FS_GROUP:
			return group;
		case FS_SHADOW:
			return shadow;
		case FS_HOST:
			return hostname;
		default:
			return exit;
		}
	}
	
	private ArrayList<ArrayList<String>> splitPath(String paths) {
		ArrayList<ArrayList<String>> ret = new ArrayList<>();
		for (String path : paths.split("\\|")) {
			ArrayList<String> dirs = new ArrayList<String>();
			int start = (path.charAt(0) != '/') ? 0 : 1, end = (path.indexOf('/', start) != -1) ? path.indexOf('/', start) : path.length();
			dirs.add(path.substring(start, end)); // get first directory name
			while (start < path.length() && end < path.length()) { // get other directory names
				start = end+1;
				end = (path.indexOf('/', start) != -1) ? path.indexOf('/', start) : path.length();
				dirs.add(path.substring(start, end));
			}
			ret.add(dirs);
		}
		return ret;
	}
	
//	public boolean isDir(String path) throws AccessDeniedException, FsElementNotFoundException, WrongElementType {
//		ArrayList<Directory> parentDir = splitPathAndName(path);
//		return parentDir.get(elName) instanceof Directory;
//	}
	
	private void followPathDir(ArrayList<String> dirs, int i, Directory curDir, ArrayList<Directory> ret, String path) {
		String dir;
		ArrayList<Directory> tmpList;
		for (; i < dirs.size(); i++) {
			try {
				dir = dirs.get(i);
				tmp = curDir;
				if (dir.equals("~") && i == 0)
					curDir = (Directory) UserSystem.curUser.getHome();
				if (dir.equals(".") && i == 0) // check hardcoded names
					curDir = (Directory) curDir;
				else if (dir.equals("..")) // check hardcoded names
					curDir = curDir.getParentDir();
				else { // must be a directory inside the working one
					tmpList = curDir.getDir(dir);
					if (tmpList != null && tmpList.isEmpty() && !tmp.getFile(dir).isEmpty()) throw new WrongElementType(path, true);
					if (tmpList == null || tmpList.isEmpty()) throw new FsElementNotFoundException(path);
					if (i == dirs.size()-1) 
						for (Directory d : tmpList) {
							try {
								if (!d.checkPerms(0)) throw new AccessDeniedException(0, d.getPath());
								ret.add(d);
							} catch (AccessDeniedException e) {
								System.out.println(d.getPath() + ": Access Denied");
							}
						}
					else
						for (Directory d : tmpList)
							followPathDir(dirs, i+1, d, ret, path);
				}
			} catch (WrongElementType | FsElementNotFoundException e) {
				System.out.println(CommandHandler.getComName() + ": " + e.getMessage(GlobalVars.verboseErrMsg));
			}
		}
		if (ret.size() == 0) ret.add(curDir);
		}
	
	public ArrayList<Directory> resolveDir(String path) throws AccessDeniedException, FsElementNotFoundException {
		Directory curDir = (path.indexOf('/') != -1) ? root : (Directory) FileSystem.curDir;
		ArrayList<Directory> ret = new ArrayList<Directory>();
		if (path.isEmpty() || path.equals("/")) {
			ret.add(root);
			return ret;
		}
		ArrayList<ArrayList<String>> dirlist = splitPath(path);
		
		for (ArrayList<String> dirs : dirlist)
			followPathDir(dirs, 0, curDir, ret, path);
		if (ret.isEmpty()) throw new FsElementNotFoundException(path);
		return ret;
	}
	
	public Directory resolveSingleDir(String path) throws WrongElementType, FsElementNotFoundException, AccessDeniedException {
		Directory curDir = (path.indexOf('/') != -1) ? root : (Directory) FileSystem.curDir;
		String dir;
		if (path.isEmpty() || path.equals("/")) {
			return root;
		}
		ArrayList<ArrayList<String>> dirlist = splitPath(path);
		ArrayList<String> dirs = dirlist.get(0);
		
		for (int i = 0; i < dirs.size(); i++) {
			dir = dirs.get(i);
			tmp = curDir;
			if (dir.equals("~") && i == 0)
				curDir = (Directory) UserSystem.curUser.getHome();
			if (dir.equals(".") && i == 0) // check hardcoded names
				curDir = (Directory) curDir;
			else if (dir.equals("..")) // check hardcoded names
				curDir = curDir.getParentDir();
			else // must be a directory inside the working one
				curDir = curDir.getSingleDir(dir);
			if (curDir == null && tmp.getSingleFile(dir) != null) throw new WrongElementType(path, true);
			if (curDir == null) throw new FsElementNotFoundException(path);
			if (!curDir.checkPerms(0)) throw new AccessDeniedException(0, curDir.getPath());
		}
		return curDir;
	}
	
	private ArrayList<File> resolveFile(String path) throws AccessDeniedException, FsElementNotFoundException, WrongElementType {
		ArrayList<Directory> parents = splitPathAndName(path);
		ArrayList<File> ret = new ArrayList<>(), tmpList;
		for (Directory d : parents) {
			tmpList = d.getFile(elName);
			for (File f : tmpList) ret.add(f);
		}
		//if (ret == null && parents.getDir(elName) != null) throw new WrongElementType(path, false);
		if (ret.isEmpty()) throw new FsElementNotFoundException(path);
		elName = null;
		return ret;
	}
	
	private File resolveSingleFile(String path) throws AccessDeniedException, FsElementNotFoundException, WrongElementType {
		Directory parentDir = splitPathAndName(path).get(0);
		File ret = parentDir.getSingleFile(elName);
		if (ret == null && parentDir.getSingleDir(elName) != null) throw new WrongElementType(path, false);
		if (ret == null) throw new FsElementNotFoundException(path);
		elName = null;
		return ret;
	}
	
	public Directory resolvePointer(FS_POINTERS p) {
		switch (p) {
		case FS_ROOT:
			return root;
		case FS_ROOTHOME:
			return rootHome;
		case FS_HOME:
			return home;
		case FS_ETC:
			return etc;
		default:
			return (Directory) curDir;
		}
	}
	
	public void add(FS_POINTERS p, String name, boolean createDir) throws DuplicateException, InvalidSymbolException {
		FsManipulationPreconditionsChecker.checkPredefinedNames(name);
		char wildcard = FileSystem.hasWildCards(name); 
		if (wildcard != 0) throw new InvalidSymbolException(name, wildcard);
		if (createDir) DirectoryManager.add(resolvePointer(p), new Directory(name, date, UserSystem.curUser, UserSystem.curUser.getPrimaryGroup(), DEFAULT_PERMS, resolvePointer(p)));
		else DirectoryManager.add(resolvePointer(p), new File(name, date, UserSystem.curUser, UserSystem.curUser.getPrimaryGroup(), DEFAULT_PERMS, resolvePointer(p)));
	}
	public void add(String path, boolean createDir) throws AccessDeniedException, FsElementNotFoundException, WrongElementType, DuplicateException, InvalidSymbolException {
		ArrayList<Directory> parentDirs = splitPathAndName(path);
		FsManipulationPreconditionsChecker.checkPredefinedNames(elName);
		char wildcard = FileSystem.hasWildCards(elName); 
		if (wildcard != 0) throw new InvalidSymbolException(elName, wildcard);
		if (createDir) {
			for (Directory parentDir : parentDirs) DirectoryManager.add(parentDir, new Directory(elName, date, UserSystem.curUser, UserSystem.curUser.getPrimaryGroup(), DEFAULT_PERMS, parentDir));
		} else for (Directory parentDir : parentDirs) DirectoryManager.add(parentDir, new File(elName, parentDir));
		elName = null;
	}
	
	public void remove(FS_POINTERS p, String name, boolean createDir) {
		if (createDir) DirectoryManager.removeDirs(resolvePointer(p), resolvePointer(p).getDir(name));
		else DirectoryManager.removeFiles(resolvePointer(p), resolvePointer(p).getFile(name));
	}
	public void remove(String path, boolean remDir) throws AccessDeniedException, FsElementNotFoundException, WrongElementType {
		if (remDir) removeDir(path, false);
		else {
			ArrayList<Directory> parentDirs = splitPathAndName(path);
			for (Directory parentDir : parentDirs)
				DirectoryManager.removeFiles(parentDir, parentDir.getFile(elName));
			elName = null;
		}
	}
	
	public void remove(String path) throws AccessDeniedException, FsElementNotFoundException, WrongElementType {
		remove(path, true);
		remove(path, false);
	}

	public void removeDir(String path, boolean removeRecursively) throws AccessDeniedException, FsElementNotFoundException, WrongElementType {
		ArrayList<Directory> parentDirs = splitPathAndName(path);
		for (Directory parentDir : parentDirs) {
			DirectoryManager.removeDirs(parentDir, parentDir.getDir(elName), removeRecursively);
		}
		elName = null;
	}
	
	public ArrayList<IModifiableFsElement> getEl(String path) throws AccessDeniedException, FsElementNotFoundException, WrongElementType {
		ArrayList<Directory> parentDirs = splitPathAndName(path);
		ArrayList<IModifiableFsElement> ret = new ArrayList<>();
		ArrayList<FsElement> tmpList;
		
		for (Directory parentDir : parentDirs) {
			tmpList = parentDir.get(elName);
			for (FsElement d : tmpList) ret.add(d);
		}
		
		elName = null;
		return ret;
	}
	public ArrayList<IModifiableFsElement> getEl(FS_POINTERS p, String name) throws FsElementNotFoundException {
		ArrayList<IModifiableFsElement> ret = new ArrayList<>();
		for (FsElement el : resolvePointer(p).get(name)) ret.add(el);
		return ret;
	}
	
	public static int getDateInDays() {
		int ret = Integer.parseInt(date.substring(8, date.length()))-1;
		int months = Integer.parseInt(date.substring(5, 7))-1;
		int years = Integer.parseInt(date.substring(0, 4)) - 1970;
		ret += (months / 2) * 30;
		ret += (months - (months / 2)) * 31;
		int syears = (int) (years * div)+1;
		ret += syears * 366;
		ret += (years - syears) * 365;
		
		return ret;
	}
	
	public static void main(String[] args) {
		date = "2019/04/23";
		System.out.println(getDateInDays());
	}
	
	public FileSystem(String[] args) {
		try {
			onlyInstance = this;
			int count = 0;
			CommandHandler.initPosFlags(); // very important
			
			System.out.println("Starting Linux...");
		
			System.out.print("Updating clock...");
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");
			LocalDateTime now = LocalDateTime.now();
		    date = dtf.format(now);
		    GlobalVars.curDate = date;
		    System.out.println("done\n");
				
			System.out.print("Searching for existing file system...");
			try {
				ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream("root.vbe")));
				System.out.println("found: " + ++count);
				System.out.print("Mounting root partition...");
				root = (Directory) in.readObject();
				in.close();
				System.out.println("done");
				
				boot = (Directory) root.getSingleDir("boot");
				dev = (Directory) root.getSingleDir("dev");
				
				System.out.print("Setting up system configurations...");
				etc = root.getSingleDir("etc");
				passwd = etc.getSingleFile("passwd");
				group = etc.getSingleFile("group");
				shadow = etc.getSingleFile("shadow");
				exit = etc.getSingleFile("exit");
				hostname = etc.getSingleFile("hostname");
				System.out.println("done");

				System.out.print("Mounting home partition...");
				home = root.getSingleDir("home");
				System.out.println("done");
				
				rootHome = (Directory) root.getSingleDir("root");
				UserSystem.ROOT.setUserHome(rootHome, false);
				
				for (int i = 0; i < group.length(); i++) 
					if (group.get(i).startsWith("sudo")) UserSystem.SUDO.setLineIndex(i);
					else if (group.get(i).startsWith("root")) UserSystem.ROOTGROUP.setLineIndex(i);
			} catch (IOException | ClassNotFoundException e) { // create a new root
				System.out.println("found: " + count);
				System.out.println("Constructing new file system...");
				System.out.print("Mounting root partition...");
				root = new Directory("", date, UserSystem.ROOT, UserSystem.ROOTGROUP, DEFAULT_PERMS, null);
				root.setParentDir(root);
				System.out.println("done");
				
				boot = new Directory("boot", root);
				root.add(boot);
				
				dev = new Directory("dev", root);
				root.add(dev);

				
				System.out.print("Setting up system configurations...");
				etc = new Directory("etc", date, UserSystem.ROOT, UserSystem.ROOTGROUP, DEFAULT_PERMS, root);
				root.add(etc);
				
				passwd = new File("passwd", etc);
				etc.add(passwd);
				
				group = new File("group", etc);
				etc.add(group);
				
				shadow = new File("shadow", etc);
				etc.add(shadow);

				exit = new File("exit", etc);
				etc.add(exit);
				FileManager.add(exit, "SaveOnExit=1");
				
				hostname = new File("hostname", etc);
				etc.add(hostname);
				FileManager.add(hostname, "localhost");
				
				System.out.println("done");

				
				System.out.print("Mounting home partition...");
				home = new Directory("home", date, UserSystem.ROOT, UserSystem.ROOTGROUP, DEFAULT_PERMS, root);
				root.add(home);
				System.out.println("done\n");
				
				rootHome = new Directory("root", date, UserSystem.ROOT, UserSystem.ROOTGROUP, NO_OTHERS, root);
				root.add(rootHome);
				UserSystem.ROOT.setUserHome(rootHome, false);
				
				passwd.add(UserSystem.ROOT.getName() + ":x:" + UserSystem.ROOT.getId() + ":" + UserSystem.ROOT.getGroups()[0].getId() + ":" + UserSystem.ROOT.getComment() + ":" + UserSystem.ROOT.getHomePath() + ":/bin/bash");
				UserSystem.ROOT.setLineIndex(passwd.length()-1);
				
				group.add(UserSystem.ROOTGROUP.getName() + ":x:" + UserSystem.ROOTGROUP.getId() + ":" + UserSystem.ROOTGROUP.getUsernames());
				UserSystem.ROOTGROUP.setLineIndex(group.length()-1);

				group.add(UserSystem.SUDO.getName() + ":x:" + UserSystem.SUDO.getId() + ":" + UserSystem.SUDO.getUsernames());
				UserSystem.SUDO.setLineIndex(group.length()-1);
			
				shadow.add(UserSystem.ROOT.getName() + ":" + UserSystem.ROOT.getPassword() + ":" + UserSystem.ROOT.getDaysSinceLastPasChange() + ":" + UserSystem.ROOT.getMinPasAge()
				+ ":" + UserSystem.ROOT.getMaxPasAge() + ":" + UserSystem.ROOT.getWarnPeriod() + ":" + UserSystem.ROOT.getInactivePeriod() + ":" + UserSystem.ROOT.getExpirationDate());
				
				root.add(new Directory("opt", root));
			}
			
			System.out.print("Logging into the default user...");
			
			UserSystem.SUDO.addUser(UserSystem.ROOT);
			UserSystem.ROOTGROUP.addUser(UserSystem.ROOT);
			
			/* setup userlist */
			UserSystem.users.add(UserSystem.ROOT);
			int firstColon, endUID, endGID, endComment, endHomePath, shadowIndex, uid, gid, endName;
			String line, name, shadowLine, password, comment, homePath, userlist;
			ArrayList<IUser> users;
			for (int i = 1; i < passwd.length(); i++) {
				line = passwd.get(i);
				firstColon = line.indexOf(':');
				endUID = line.indexOf(':', firstColon+3);
				endGID = line.indexOf(':', endUID+1);
				endComment = line.indexOf(':', endGID+1);
				endHomePath = line.indexOf(':', endComment+1);
				
				name = line.substring(0, firstColon);
				shadowIndex = shadow.get(name);
				shadowLine = shadow.get(shadowIndex);
				password = shadowLine.substring(name.length()+1, shadowLine.indexOf(':', name.length()+1));
				uid = Integer.parseInt(line.substring(firstColon+3, endUID));
				comment = "";
				if (endComment > endGID+1) comment = line.substring(endGID+1, endComment);
				homePath = line.substring(endComment+1, endHomePath);
				UserSystem.users.add(new User(name, password, uid, comment, resolveSingleDir(homePath), i, shadowIndex));
			}

			/* setup grouplist */
			UserSystem.groups.add(UserSystem.ROOTGROUP);
			UserSystem.groups.add(UserSystem.SUDO);
			for (int i = 2; i < group.length(); i++) {
				line = group.get(i);
				endName = line.indexOf(':');
				endGID = line.indexOf(':', endName+3);
				
				name = line.substring(0, endName);
				gid = Integer.parseInt(line.substring(endName+3, endGID));
				userlist = line.substring(endGID+1);
				users = new ArrayList<>();
				for (String username : userlist.split(","))
					if (!username.isEmpty()) users.add(UserSystem.getUserByName(username));
				UserSystem.groups.add(new Group(name, gid, users));
			}

			/* assign users to groups */
			for (int i = 1; i < UserSystem.users.size(); i++) {
				IUser user = UserSystem.users.get(i);
				for (int j = 0; j < UserSystem.groups.size(); j++)
					if (UserSystem.groups.get(j).hasUser(user)) user.addGroup(UserSystem.groups.get(j));
			}
			
			UserSystem.curUser = UserSystem.ROOT;
			setCurDir(UserSystem.curUser.getHome());
			System.out.println("done");
			
			root.updateSize();
			if (GlobalVars.comFuncString.indexOf("--system-level", args) == -1) { // if user starts application in normal mode, prevent the deletion of important elements
				rootHome.setSystemLocation();
				home.setSystemLocation();
				etc.setSystemLocation();
			}
		} catch (GeneralException e) {
			System.out.println(GlobalVars.ERR_PREFIX + "The system experienced a major failure during startup. This may be due to an incorrect configuration in the FileSystem constructor. Try to restart or contact me if the error persists..");
		}
	}

	public static String getDate() {
		return date;
	}

	public IDirForCommercialUse getCurDir() {
		return curDir;
	}

	public String getCurPath() {
		return curDir.getPath();
	}

	public static void setCurDir(IDirForCommercialUse iDirForCommercialUse) {
		curDir = iDirForCommercialUse;
	}
	
	public static void setCurDir(String path) {
		UserSystem.performOpAsRoot("cd " + path);
	}
	
	public static String generateFileName() {
		int i = 0;
		try {
			while (true)
				DirectoryManager.get((Directory) curDir, NAME_PREFIX + i++);
		} catch (FsElementNotFoundException e) {}
		return NAME_PREFIX + i;
	}

	public void addSingleLine(String pathToFile, boolean override, String text) throws AccessDeniedException, FsElementNotFoundException, WrongElementType {
		File file = resolveSingleFile(pathToFile);
		if (override) file.clear();
		FileManager.add(file, text);
	}
	
	public void addLines(String pathToFile, boolean override, String text) throws AccessDeniedException, FsElementNotFoundException, WrongElementType {
		ArrayList<File> files = resolveFile(pathToFile);
		if (override) for (File file : files) file.clear();
		for (File file : files) FileManager.add(file, text);
	}

	public void addLine(FS_CONFIGS c, boolean override, String line) {
		File file = resolveConfig(c);
		if (override) file.clear();
		FileManager.add(file, line);
	}
	
	private String[] readLines(File file, int low, int max) {
		low = low > -1 && low < file.length() ? low : 0;
		max = max < file.length() && max > low ? max : file.length();
		String[] ret = new String[max];
		int retIndex = low;
		Iterator<String> it = file.iterator();
		while (it.hasNext() && retIndex < max)
			ret[retIndex++] = it.next();
		return ret;
	}
	
	public String[] readLines(String path, int low, int max) throws AccessDeniedException, FsElementNotFoundException, WrongElementType {
		File file = resolveSingleFile(path);
		return readLines(file, low, max);
	}
	
	public String[] readLines(String path, int max) throws AccessDeniedException, FsElementNotFoundException, WrongElementType {
		return readLines(path, 0, max);
	}
	
	public String[] readLines(FS_CONFIGS c, int low, int max) {
		return readLines(resolveConfig(c), low, max);
	}

	public String[][] readLinesOfMultipleFiles(String pathToFile, int max) {
		ArrayList<File> files;
		String[][] ret = null;
		try {
			files = resolveFile(pathToFile);
			ret = new String[files.size()][];
			String[] tmpArr;
			for (int i = 0; i < ret.length; i++) {
				tmpArr = readLines(files.get(i), 0, max);
				ret[i] = new String[tmpArr.length+1];
				ret[i][0] = files.get(i).getName() + ":";
				for (int j = 1; j < ret[i].length; j++) ret[i][j] = tmpArr[j-1];
			}
		} catch (WrongElementType | AccessDeniedException | FsElementNotFoundException e) {
			ret = new String[0][];
			System.out.println(e.getMessage(GlobalVars.verboseErrMsg));
		}
		
		return ret;
	}
	
	public int find(FS_CONFIGS c, String pattern) throws LineNotFoundException {
		int ret = resolveConfig(c).get(pattern);
		if (ret == -1) throw new LineNotFoundException(resolveConfig(c).getPath(), pattern);
		return ret;
	}

	public void appendToLine(FS_CONFIGS c, int index, String text) {
		if (index < 0 || index >= resolveConfig(c).length()) throw new IllegalArgumentException("Invalid index: " + index);
		resolveConfig(c).set(index, resolveConfig(c).get(index) + text);
	}

	public int length(FS_CONFIGS c) {
		return resolveConfig(c).length();
	}
	
	public void remLine(FS_CONFIGS c, int index) {
		resolveConfig(c).remove(index);
	}

	public void setLine(FS_CONFIGS c, int lineIndex, String line) {
		FileManager.set(resolveConfig(c), lineIndex, line);
	}
	
	public IDirForCommercialUse getDir(FS_POINTERS p, String name) {
		return resolvePointer(p).getSingleDir(name);
	}

	public String dump() {
		return "curPath: " + curDir.getPath() + "\n"
				+ root.toString(0);
	}

	public void execute(String filename) throws AccessDeniedException, FsElementNotFoundException, FsException {
		File script = resolveSingleFile(filename);
		if (!script.checkPerms(2))
			throw new AccessDeniedException(2, script.getPath());
		if (script.isExecutable()) script.execute();
	}

	public static boolean isPath(String path) {
		return path.indexOf('/') != -1;
	}

	public String getHostname() {
		return hostname.get(0);
	}

	public static void init(String[] args) {
		new FileSystem(args);
	}

	public void copy(FsElement src, IDirForCommercialUse trg, String newName) throws DuplicateException {
		FsElement newSrc = src.clone();
		newSrc.setName(newName);
		newSrc.setParentDir((Directory) trg);
		trg.addD(newSrc);
	}
	public void copy(ArrayList<FsElement> src, ArrayList<IDirForCommercialUse> trgArr, String newName) throws DuplicateException {
		for (IDirForCommercialUse trg : trgArr) {
			for (FsElement newSrc : src) {
				newSrc.setName(newName);
				newSrc.setParentDir((Directory) trg);
				trg.addD(newSrc);
			}
		}
	}
}
