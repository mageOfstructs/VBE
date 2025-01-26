package vbe.commands;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Stack;

import newworld.fs.FileSystem;
import newworld.fs.FileSystem.FS_CONFIGS;
import newworld.fs.FileSystem.FS_POINTERS;
import newworld.fs.IDirForCommercialUse;
import newworld.fs.IModifiableFsElement;
import vbe.CommonFunctions;
import vbe.GlobalVars;
import vbe.commands.CommandHandler.Flag;
import vbe.exceptions.CommandSyntaxException;
import vbe.exceptions.DuplicateException;
import vbe.exceptions.GeneralException;
import vbe.exceptions.fs.FsElementNotFoundException;
import vbe.exceptions.fs.FsException;
import vbe.exceptions.fs.InvalidSymbolException;
import vbe.exceptions.fs.LineNotFoundException;
import vbe.exceptions.fs.WrongElementType;
import vbe.exceptions.usersystem.AccessDeniedException;
import vbe.exceptions.usersystem.UserElementNotFoundException;
import vbe.usersystem.Group;
import vbe.usersystem.IGroup;
import vbe.usersystem.IUser;
import vbe.usersystem.User;
import vbe.usersystem.UserSystem;

public class Commands implements IAdminCommands {
	public final static Flag[] EMPTY_FLAGARR = {}; // keep it EMPTY_FLAGARR!
	
	private static String elName;
	
	/**
	 * splits the given path into the directory the element is in and the element's name
	 * @param path
	 * @return
	 * @throws FsElementNotFoundException 
	 * @throws WrongElementType 
	 * @throws AccessDeniedException 
	 */
	private static IDirForCommercialUse splitPath(String path) throws FsElementNotFoundException, WrongElementType, AccessDeniedException {
		if (path == null || path.indexOf('/') == -1) {
			elName = (path == null) ? FileSystem.generateFileName() : path;
			return FileSystem.getOnlyInstance().getCurDir();
		}
		IDirForCommercialUse workDir = FileSystem.getOnlyInstance().resolveSingleDir(path.substring(0, path.lastIndexOf('/')));
		elName = path.substring(path.lastIndexOf('/')+1);
		return workDir;
	}
	
	/**
	 * a simple help function, that prints all currently available commands
	 */
	public static void help() {
		for (int i = 0; i < CommandHandler.AVAILABLE_COMMANDS.length; i++)
			System.out.println(CommandHandler.AVAILABLE_COMMANDS[i]);
	}
	
	/**
	 * another simple function, that prints the current working directory
	 */
	public static void pwd() {
		System.out.println(FileSystem.getOnlyInstance().getCurPath());
	}
	
	/**
	 * prints the contents of the given directory to the terminal
	 * flagArray: l, a, h
	 * @param flags
	 * @param pathToListContents
	 * @throws AccessDeniedException 
	 * @throws CommandSyntaxException 
	 * @throws FsElementNotFoundException 
	 * @throws WrongElementType 
	 */
	public static void ls(Flag[] flags, String pathToListContents) throws AccessDeniedException, CommandSyntaxException, FsElementNotFoundException, WrongElementType {
		boolean verbose = flags[0].flagIndex != -1;
		boolean showHidden = flags[1].flagIndex != -1;
		boolean humanReadableUnits = flags[2].flagIndex != -1;

		ArrayList<IDirForCommercialUse> dirs = new ArrayList<>();
		if (pathToListContents == null) {
			dirs.add(FileSystem.getOnlyInstance().getCurDir());
		} else
			for (IDirForCommercialUse d : FileSystem.getOnlyInstance().resolveDir(pathToListContents)) dirs.add(d);
		if (dirs.size() > 1)
			for (IDirForCommercialUse dir : dirs) {
				System.out.println(dir.getPath() + ":");
				dir.listContents(verbose, showHidden, humanReadableUnits);
			}
		else if (dirs.size() > 0) dirs.get(0).listContents(verbose, showHidden, humanReadableUnits);
		else throw new FsElementNotFoundException(pathToListContents);
	}
	
	public static void cd(Flag[] flags, String pathToChangeTo) throws AccessDeniedException, WrongElementType, GeneralException {
		if (pathToChangeTo != null) {
			FileSystem.setCurDir(FileSystem.getOnlyInstance().resolveSingleDir(pathToChangeTo));
		} else if (UserSystem.curUser.getHome() != null) {
			FileSystem.setCurDir(UserSystem.curUser.getHome());
		} else throw new GeneralException("You're homeless!");
	}
	
	public static void mk (Flag[] flags, String pathToNewDir, boolean createDir) throws AccessDeniedException, FsElementNotFoundException, WrongElementType, DuplicateException, InvalidSymbolException {
		FileSystem.getOnlyInstance().add(pathToNewDir, createDir);
	}
	
	/**
	 * removes a directory or file (depends on removeDir)
	 * use flag "r" to remove a directory and all of its content
	 * flagArray: r
	 * @param flags
	 * @param pathToDirToRemove
	 * @param removeDir
	 * @throws AccessDeniedException
	 * @throws CommandSyntaxException
	 * @throws FsElementNotFoundException 
	 * @throws FsException 
	 */
	public static void rm (Flag[] flags, String pathToDirToRemove, boolean removeDir) throws AccessDeniedException, CommandSyntaxException, FsElementNotFoundException, FsException {
		boolean removeRecursively = flags.length > 0 && flags[0].flagIndex != -1; // removes the directory along with its subdirectories
		if (removeDir) FileSystem.getOnlyInstance().removeDir(pathToDirToRemove, removeRecursively);
		else FileSystem.getOnlyInstance().remove(pathToDirToRemove, false);
	}
	
	public static void rm(Flag[] flags, String pathToDirToRemove) throws AccessDeniedException, FsElementNotFoundException, WrongElementType {
		boolean removeRecursively = flags.length > 0 && flags[0].flagIndex != -1; // removes the directory along with its subdirectories
		try {
			FileSystem.getOnlyInstance().removeDir(pathToDirToRemove, removeRecursively);
		} catch (FsElementNotFoundException e) {
			FileSystem.getOnlyInstance().remove(pathToDirToRemove, false);
		}
		if (FileSystem.getOnlyInstance().getCurPath().startsWith(pathToDirToRemove)) {
			IDirForCommercialUse newCurDir = FileSystem.getOnlyInstance().getCurDir().getParentDir();
			while (newCurDir.getPath().startsWith(pathToDirToRemove)) newCurDir = newCurDir.getParentDir();
			FileSystem.setCurDir(newCurDir);
		}
	}
	
	public static void cp (Flag[] flags, String pathToElement, String pathToCopyTo) throws AccessDeniedException, FsElementNotFoundException, WrongElementType, DuplicateException {
		IDirForCommercialUse parentDir = splitPath(pathToElement);
		ArrayList<IDirForCommercialUse> dirToCopyTo = new ArrayList<>();
		String srcName = elName;
		try {
			for (IDirForCommercialUse d : FileSystem.getOnlyInstance().resolveDir(pathToCopyTo))
				dirToCopyTo.add(d); // try and see if a new name was given in the path
		} catch (FsElementNotFoundException e) {
			for (IDirForCommercialUse d : FileSystem.getOnlyInstance().splitPathAndName(pathToElement))
				dirToCopyTo.add(d);
		}
		FileSystem.getOnlyInstance().copy(parentDir.get(srcName), dirToCopyTo, elName);
	}
	
	public static void mv (Flag[] flags, String pathToElement, String pathToCopyTo) throws AccessDeniedException, FsElementNotFoundException, WrongElementType, DuplicateException {
		cp(flags, pathToElement, pathToCopyTo);
		FileSystem.getOnlyInstance().remove(pathToElement);
	}
	
	public static void cat (Flag[] flags, String pathToFile) throws AccessDeniedException, FsElementNotFoundException, WrongElementType {		
		if (FileSystem.hasWildCards(pathToFile) == 0) for (String line : FileSystem.getOnlyInstance().readLines(pathToFile, -1)) System.out.println(line);
		else for (String[] lines : FileSystem.getOnlyInstance().readLinesOfMultipleFiles(pathToFile, -1)) {
			System.out.println(lines[0]);
			for (int j = 1; j < lines.length; j++) System.out.println("\t" + lines[j]);
		}
	}
	
	/**
	 * prints the first x lines of a file (use -n x to specify number)
	 * If no number is given, it'll default to 10
	 * flagArray: n (technically -1, -2,... should be here, but that's handled in the method directly)
	 * @param flags
	 * @param tokens
	 * @throws FsElementNotFoundException
	 * @throws WrongElementType
	 * @throws AccessDeniedException
	 * @throws CommandSyntaxException
	 */
	public static void head(Flag[] flags, String...tokens) throws FsElementNotFoundException, WrongElementType, AccessDeniedException, CommandSyntaxException {
		int linesIndex = 0;
		int lines = 10;
		String file = "";
		int i = 1;
		
		try {
			lines = Integer.parseInt(flags[0].flag);
		} catch (NumberFormatException e) {
			final String[] possibleFlags = { "n" };
			int[] flagIndexes = CommandHandler.getFlags(possibleFlags, flags);
			linesIndex = flagIndexes[0];
			if (linesIndex != 0)
				lines = Integer.parseInt(tokens[linesIndex+1]);
		} catch (ArrayIndexOutOfBoundsException e) {}
		
		while (i < tokens.length) {
			if (tokens[i].charAt(0) != '-' && (linesIndex == 0 || i != linesIndex+1)) {
				file = tokens[i];
				break;
			}
			i++;
		}
		String[] linesArr = FileSystem.getOnlyInstance().readLines(file, lines);
		for (String line : linesArr)
			System.out.println(line);
	}
	
	public static void echo (Flag[] flags, String text, String writeMode, String pathToFile) throws CommandSyntaxException, AccessDeniedException, FsElementNotFoundException, WrongElementType {
		if (pathToFile == null) { // if no second and third parameter was given system uses output console
			System.out.println(text);
		} else {
		if (writeMode.equals(">>"))
			FileSystem.getOnlyInstance().addSingleLine(pathToFile, false, text); // remember to add the addLine() method later
		else if (writeMode.equals(">"))
			FileSystem.getOnlyInstance().addSingleLine(pathToFile, true, text);
		else
			throw new CommandSyntaxException(CommandHandler.getCommand(), 6+text.length());
		}
	}
	
	public static void exit() throws IOException, AccessDeniedException, WrongElementType, GeneralException {
		if (!userstack.isEmpty() && !UserSystem.curUser.equals(userstack.get(0))) {
			UserSystem.curUser = userstack.pop();
			cd(EMPTY_FLAGARR, pathstack.pop());
		} else {
			if (FileSystem.getOnlyInstance().readLines(FileSystem.FS_CONFIGS.FS_EXIT, 0, 1)[0].endsWith("1"))
				write();
			exit = true;
		}
	}
	
	public static void notFound() {
		System.out.println("bash: " + CommandHandler.getTokens()[0] + ": Command not found.");
	}

	private static Stack<IUser> userstack = new Stack<IUser>();
	private static Stack<String> pathstack = new Stack<String>();
	private static boolean exit = false;
	
	public static boolean canExit() { return exit; }
	
	public static void su(Flag[] flags, String username) throws AccessDeniedException, UserElementNotFoundException {
		IUser userToSwitchTo = UserSystem.getUserByName(username);
		if (UserSystem.isRoot() || !userToSwitchTo.getPassword().equals("!*") && !userToSwitchTo.isLocked() && userToSwitchTo.performPasswdCheck()) {
			userstack.push(UserSystem.curUser);
			pathstack.push(FileSystem.getOnlyInstance().getCurPath());
			UserSystem.curUser = userToSwitchTo;
		} else System.out.println("su: Authentication failure");
	}
	
	/**
	 * adds a user
	 * use -m to create a home directory
	 * use -g to specify an existing primary group for the user
	 * use -G to add additional groups to the user
	 * use -c to specify GECOS comment
	 * flagArray: m, g, G, c
	 * @param flags
	 * @param tokens
	 * @throws AccessDeniedException
	 * @throws DuplicateException
	 * @throws CommandSyntaxException
	 * @throws WrongElementType 
	 * @throws FsElementNotFoundException 
	 * @throws LineNotFoundException 
	 * @throws UserElementNotFoundException 
	 * @throws InvalidSymbolException 
	 */
	public static void useradd(Flag[] flags, String... tokens) throws AccessDeniedException, DuplicateException, CommandSyntaxException, FsElementNotFoundException, WrongElementType, LineNotFoundException, UserElementNotFoundException, InvalidSymbolException {
		if (!UserSystem.isRoot()) throw new AccessDeniedException();
		
		IDirForCommercialUse home = null;
		IGroup primGroup = null;

		String username = null;
		String comment = "";
		boolean createHome = flags.length > 0 && flags[0].flagIndex != -1;
		boolean gecos = flags.length > 0 && flags[3].flagIndex != -1 || flags[7].flagIndex != -1;
		int primGroupFlagIndex = flags.length > 0 ? flags[1].flagIndex : -1, primGroupTokenIndex = flags.length > 0 ? flags[1].tokenIndex : -1;
		
		if (gecos) {
			int tokenIndex = flags[3].flagIndex != -1 ? flags[3].tokenIndex : flags[7].tokenIndex;
			comment = tokens[tokenIndex+1];
		}
		
		if (primGroupFlagIndex != -1) {
			primGroup = UserSystem.getGroupByName(tokens[primGroupTokenIndex]);

			int lineOfGroupIndex = FileSystem.getOnlyInstance().find(FS_CONFIGS.FS_GROUP, primGroup.getName());
			FileSystem.getOnlyInstance().appendToLine(FS_CONFIGS.FS_GROUP, lineOfGroupIndex, "," + primGroup.getName());

		}
		
		for (int i = 1; i < tokens.length; i++) { // determine the username
			if (tokens[i].charAt(0) != '-' && i != primGroupTokenIndex) username = tokens[i];
		}
		
		try {
			UserSystem.getUserByName(username);
			throw new DuplicateException(username, "user");
		} catch (UserElementNotFoundException e) {}
		
		if (primGroupFlagIndex == -1) {
			if (UserSystem.doesGroupExist(username))
				throw new DuplicateException(username, "group");
			UserSystem.groups.add(new Group(username));
			primGroup = UserSystem.groups.get(UserSystem.groups.size()-1);
		}
		
		if (username == null) {
			String command = "";
			for (String token : tokens) command += token + " ";
			throw new CommandSyntaxException(command, command.length()-2, "Missing argument 'username'!");
		}
		
		if (createHome) {
			FileSystem.getOnlyInstance().add(FS_POINTERS.FS_HOME, username, true);
			home = FileSystem.getOnlyInstance().getDir(FS_POINTERS.FS_HOME, username);
			if (home == null) throw new DuplicateException(username, "users");
			FileSystem.getOnlyInstance().add("/home/" + username + "/.bash_history", false);
		}
		
		IUser newUser = new User(username, "!*", home, comment, primGroup);
		UserSystem.users.add(newUser);
		
		if (createHome)
			UserSystem.performOpAsRoot("chown " + username + " " + home.getPath());
		
		newUser.updateConfigFile();
		primGroup.addUser(newUser);
	}
	
	/**
	 * modifies the given user (or the current user, if the param was omitted
	 * flagArray: a, d, g, G, l, L, m, r, c, u
	 * @param flags
	 * @param params
	 * @throws WrongElementType 
	 * @throws FsElementNotFoundException 
	 * @throws AccessDeniedException 
	 * @throws DuplicateException 
	 */
	public static void usermod(Flag[] flags, String username) throws FsElementNotFoundException, WrongElementType, AccessDeniedException, UserElementNotFoundException, DuplicateException {
		if (!UserSystem.isRoot()) throw new AccessDeniedException();
		
		String[] tokens = CommandHandler.getTokens();
		IUser user = null;
		if (username != null) user = UserSystem.getUserByName(username);
		else user = UserSystem.curUser;
		
		boolean append = flags[0].flagIndex != -1;
		boolean remove = flags[7].flagIndex != -1;
		boolean changeHomeDir = flags[1].flagIndex != -1;
		boolean changePrimGroup = flags[2].flagIndex != -1;
		boolean hasGrouplist = flags[3].flagIndex != -1;
		boolean changeName = flags[4].flagIndex != -1;
		boolean lockUser = flags[5].flagIndex != -1;
		boolean unlockUser = flags[9].flagIndex != -1;
		boolean moveHome = flags[6].flagIndex != -1;
		boolean gecos = flags[8].flagIndex != -1;
		
		if (gecos) {
			user.setComment(tokens[flags[8].tokenIndex+1]);
		}
		
		if (changeHomeDir) { // change the home directory
			String prevHomePath = user.getHomePath();
			user.setUserHome(FileSystem.getOnlyInstance().resolveSingleDir(tokens[flags[1].tokenIndex+1]));
			if (moveHome && prevHomePath != null)
				Commands.mv(EMPTY_FLAGARR, prevHomePath, user.getHomePath());
		}
		if (changePrimGroup) {
			String group = tokens[flags[2].tokenIndex+1];
			try {
				int gid = Integer.parseInt(group);
				user.setPrimGroup(UserSystem.getGroupById(gid));
			} catch (NumberFormatException e) {
				user.setPrimGroup(UserSystem.getGroupByName(group));
			}
		}
		
		if (changeName)
			user.setName(tokens[flags[4].tokenIndex+1]);
		
		if (lockUser) user.setLocked(true);
		else if (unlockUser) user.setLocked(false);
	
		IGroup[] grouplist = null;
		if (hasGrouplist) {
			String[] groupnames = tokens[flags[3].tokenIndex+1].split(",");
			grouplist = new IGroup[groupnames.length];
			for (int i = 0; i < grouplist.length; i++)
				grouplist[i] = UserSystem.getGroupByName(groupnames[i]);
		}
		
		if (append && hasGrouplist) {
			for (int i = 0; i < grouplist.length; i++)
				user.addGroup(grouplist[i]);
		} else if (remove && hasGrouplist)
			for (int i = 0; i < grouplist.length; i++)
				user.remGroup(grouplist[i]);
		else if (hasGrouplist) {
			user.clearGroups();
			for (int i = 0; i < grouplist.length; i++)
				user.addGroup(grouplist[i]);
		}
		user.updateConfigFile();
	}
	
	public static void userdel(Flag[] flags, String username) throws UserElementNotFoundException, LineNotFoundException {
		IUser user = UserSystem.getUserByName(username);
		UserSystem.delUser(user);
	}
	
	/**
	 * adds a group
	 * use -U to specify a list of users, that will be in the newly created group
	 * flagArray: U
	 * @param flags
	 * @param tokens
	 * @throws CommandSyntaxException
	 * @throws AccessDeniedException 
	 * @throws UserElementNotFoundException 
	 * @throws DuplicateException 
	 * @throws WrongElementType 
	 * @throws FsElementNotFoundException 
	 */
	public static void groupadd(Flag[] flags, String groupname) throws CommandSyntaxException, AccessDeniedException, UserElementNotFoundException, DuplicateException, FsElementNotFoundException, WrongElementType {
		if (!UserSystem.isRoot()) throw new AccessDeniedException();
		
		boolean gotUserlist = flags[0].flagIndex != -1;
		ArrayList<IUser> userlist = new ArrayList<>();
		String[] users = null;
		
		if (gotUserlist) {
			users = CommandHandler.getTokens()[flags[0].tokenIndex+1].split(",");
			for (String user : users) userlist.add(UserSystem.getUserByName(user));
		}
		
		if (UserSystem.doesGroupExist(groupname))
			throw new DuplicateException(groupname, "group");
		
		IGroup newGroup = new Group(groupname, userlist, true);
		UserSystem.groups.add(newGroup);
		
		if (gotUserlist) {
			for (String user : users) UserSystem.getUserByName(user).addGroup(newGroup);
		}
	}
	
	public static void id(Flag[] flags, String username) throws UserElementNotFoundException {
		IUser user = UserSystem.curUser;
		if (username != null) user = UserSystem.getUserByName(username);
		System.out.print("uid=" + user.getId() + "(" + user.getName() + ") ");
		System.out.print("gid=" + user.getPrimaryGroup().getId() + "(" + user.getPrimaryGroup().getName() + ") ");
		System.out.print("groups=");
		IGroup[] groups = user.getGroups();
		for (int i = 0; i < groups.length-1; i++) {
			IGroup g = groups[i];
			System.out.print(g.getId() + "(" + g.getName() + "),");
		}
		System.out.println(groups[groups.length-1].getId() + "(" + groups[groups.length-1].getName() + ")");
	}
	
	public static void groups(Flag[] flags, String username) throws UserElementNotFoundException {
		IUser user = UserSystem.curUser;
		if (username != null) user = UserSystem.getUserByName(username);
		IGroup[] groups = user.getGroups();
		for (IGroup group : groups)
			System.out.print(group.getName() + " ");
		System.out.println();
	}
	
	public static void passwd(Flag[] flags, String username) throws UserElementNotFoundException, AccessDeniedException {
		IUser user = UserSystem.curUser;
		if (username != null) user = UserSystem.getUserByName(username);
		
		if (user.getName().equals(UserSystem.curUser.getName()) || UserSystem.isRoot()) {
			System.out.print("New password: ");
			String passwd = GlobalVars.getIn().next();
			System.out.print("Retype password: ");
			if (passwd.equals(GlobalVars.getIn().next())) {
				user.setPassword(passwd);
				System.out.println("Password successfully updated");
			} else
				System.out.println(GlobalVars.ERR_PREFIX + "Passwords do not match!");
		} else throw new AccessDeniedException("You may not access or modify the password of " + username);
	}
	
	public static void chown(Flag[] flags, String OwnerGroupPair, String path) throws AccessDeniedException, FsElementNotFoundException, WrongElementType, UserElementNotFoundException {
		ArrayList<IModifiableFsElement> elementsToChange = FileSystem.getOnlyInstance().getEl(path);
		for (IModifiableFsElement elementToChange : elementsToChange) {
			if (!elementToChange.checkPerms(1))
				throw new AccessDeniedException(1, path);
			IUser newOwner = UserSystem.getUserByName(OwnerGroupPair.substring(0, OwnerGroupPair.indexOf(':') != -1 ? OwnerGroupPair.indexOf(':') : OwnerGroupPair.length()));
			IGroup newGroup = OwnerGroupPair.indexOf(':') != -1 ? UserSystem.getGroupByName(OwnerGroupPair.substring(OwnerGroupPair.indexOf(':')+1)) : newOwner.getPrimaryGroup();
			elementToChange.setUser(newOwner);
			elementToChange.setGroup(newGroup);
		}
	}
	
	private static final Character[] allowedSymbols = { 'u', 'g', 'o', 'a', '+', '-', '=', 'r', 'w', 'x' };
	private static boolean[] fullFileBits = null;
	private static int mod = -1;
	private static boolean[] fileBits = null;
	private static int[] specifiers = null;
	private static int indexOfLastSpecifier = -1;
	
	/**
	 * changes the permissions of files
	 * file mode bit format can either be numerical (will be converted to binary) or in the ugoa format
	 * ugoa format [u=user][g=group][o=other][a=all][+=add][-=take away][= =set][r=read][w=write][x=execute]
	 * -R to apply it to a directory and its content
	 * flagArray: R
	 * @param flags
	 * @param fileModeBitsModifier
	 * @param path
	 * @throws AccessDeniedException
	 * @throws CommandSyntaxException
	 * @throws WrongElementType 
	 * @throws FsElementNotFoundException 
	 */
	public static void chmod(Flag[] flags, String fileModeBitsModifier, String path) throws AccessDeniedException, CommandSyntaxException, FsElementNotFoundException, WrongElementType {
		boolean setRecursive = flags[0].flagIndex != -1;
		try {
			for (IModifiableFsElement elementToChange : FileSystem.getOnlyInstance().getEl(path)) {
				if (!elementToChange.checkPerms(1)) throw new AccessDeniedException(1, path);
				try {
					int newFileModeBits = Integer.parseInt(fileModeBitsModifier);
					if (fullFileBits == null) fullFileBits = CommonFunctions.intToBin(newFileModeBits, 9);
					elementToChange.setPerms(fullFileBits);
				} catch (NumberFormatException e) { 
					// Modifier is in the ugoa format ([uoga][+=-][rwx])
					fileModeBitsModifier = fileModeBitsModifier.toLowerCase();
					int i = 0; 
					if (specifiers == null) {
						specifiers = new int[8]; // expression should be <= 8
						for (int j = 0; j < specifiers.length; j++) specifiers[j] = -1;
						// syntax check
						while (i < fileModeBitsModifier.length() && (specifiers[i] = GlobalVars.comFuncChar.indexOf(fileModeBitsModifier.charAt(i), allowedSymbols)) != -1) {					
							i++;
						}
						if (i < fileModeBitsModifier.length()) throw new CommandSyntaxException("chmod " + fileModeBitsModifier + " " + path, 6, "Unknown file mode modifier format!");
					}
					i = fileModeBitsModifier.length();
					// get file bits
					if (fileBits == null) {
						fileBits = new boolean[3];
						while (specifiers[i] == -1 || specifiers[i] > 6) {
							if (specifiers[i] > 6) fileBits[specifiers[i]-7] = true;
							i--;
						}
					}
					
					if (mod == -1) mod = specifiers[i--]; // get modifier
					if (indexOfLastSpecifier != -1) i = indexOfLastSpecifier;
					else indexOfLastSpecifier = i;
					boolean[] newPerms = elementToChange.getPerms();
					while (i > -1) { // get specifier
						if (specifiers[i] >= 4) throw new CommandSyntaxException("chmod " + fileModeBitsModifier + " " + path, 6, "A specifier must first be before modifier or file bits");
						if (specifiers[i] < 3) {
							if (mod == 4) {
								if (fileBits[0]) newPerms[specifiers[i]*3] = true;
								if (fileBits[1]) newPerms[specifiers[i]*3+1] = true;
								if (fileBits[2]) newPerms[specifiers[i]*3+2] = true;
							} else if (mod == 5) {
								if (fileBits[0]) newPerms[specifiers[i]*3] = false;
								if (fileBits[1]) newPerms[specifiers[i]*3+1] = false;
								if (fileBits[2]) newPerms[specifiers[i]*3+2] = false;
							} else if (mod == 6) {
								newPerms[specifiers[i]*3] = fileBits[0];
								newPerms[specifiers[i]*3+1] = fileBits[1];
								newPerms[specifiers[i]*3+2] = fileBits[2];
							} else throw new CommandSyntaxException(CommandHandler.getComName(), 6, "Something wrong with the modifier.");
						}
						else {
							for (int j = 0; j < 3; j++) {
								if (mod == 4) {
									if (fileBits[0]) newPerms[j*3] = true;
									if (fileBits[1]) newPerms[j*3+1] = true;
									if (fileBits[2]) newPerms[j*3+2] = true;
								} else if (mod == 5) {
									if (fileBits[0]) newPerms[j*3] = false;
									if (fileBits[1]) newPerms[j*3+1] = false;
									if (fileBits[2]) newPerms[j*3+2] = false;
								} else if (mod == 6) {
									newPerms[j*3] = fileBits[0];
									newPerms[j*3+1] = fileBits[1];
									newPerms[j*3+2] = fileBits[2];
								} else throw new CommandSyntaxException(CommandHandler.getComName(), 6, "Something wrong with the modifier.");
							}
						}
						i--;
					}
				} finally {
					if (setRecursive && elementToChange.isDir())
						for (String elPath : elementToChange.getChildPaths())
							chmod(flags, fileModeBitsModifier, elPath);
				}
			}
		} finally {
			fullFileBits = null;
			mod = -1;
			fileBits = null;
			specifiers = null;
			indexOfLastSpecifier = -1;
		}
	}
	
	public static void history(String index) throws GeneralException {
		IDirForCommercialUse userHome = UserSystem.curUser.getHome();
		String[] comList = null;
		if (userHome != null) {
			try {
				comList = FileSystem.getOnlyInstance().readLines(UserSystem.curUser.getHomePath() + "/.bash_history", -1);
			} catch (FsElementNotFoundException e) {
				FileSystem.getOnlyInstance().add(userHome.getPath() + "/.bash_history", false);
			}
		}
		
		if (index != null) {
			try {
				int i = Integer.parseInt(index);
				if (i < 0 || i >= comList.length) throw new GeneralException("Invalid history index!");
				addToHistory = false;
				UserSystem.performOpAsCurUser(comList[i]);
				addToHistory = true;
			} catch (NumberFormatException e) { throw new GeneralException("Not a number!"); }
		} else 
			for (int i = 0; i < comList.length; i++)
				System.out.println(i + ": " + comList[i]);
	}
	
	private static boolean addToHistory = true;
	
	public static boolean addToHistory() {
		return addToHistory;
	}
	
	public static void sudo(String... tokens) throws GeneralException {
		if (tokens.length == 1) throw new GeneralException("Sorry, I couldn't hear you. Could you say that again?");
		String[] command = new String[tokens.length-1];
		for (int i = 1; i < tokens.length; i++)
			command[i-1] = tokens[i];
		if (UserSystem.isRoot() || (UserSystem.performPasswdCheck() && UserSystem.SUDO.hasUser(UserSystem.curUser)))
			UserSystem.performOpAsRoot(command);
		else throw new AccessDeniedException(UserSystem.curUser.getName() + " is not in the sudoers file!");
	}
	
	public static void write() throws IOException {
		ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("root.vbe")));
		out.writeObject(FileSystem.getOnlyInstance().resolvePointer(FS_POINTERS.FS_ROOT));
		out.close();
	}
	
	public static void report(String msg) throws IOException {
		StringBuilder fileContent = new StringBuilder();
		fileContent.append("+------------------------------------------+\n");
		fileContent.append("| ERROR report of the application BASH.jar |\n");
		fileContent.append("+------------------------------------------+\n\n");
		
		fileContent.append("Please send this file to the creator, (Github: https://github.com/mageOfstructs), as it contains important and invaluable debugging information.\n");
		fileContent.append("Report:\n" + msg + "\n\n");
		fileContent.append("FileSystem dump:\n");
		fileContent.append(FileSystem.getOnlyInstance().dump());
		
		StringBuilder filename = new StringBuilder("VBASHE_report");
		int count = GlobalVars.getRepCount();
		if (count != 0) filename.append(count);
		filename.append(".txt");
		
		BufferedWriter outB = new BufferedWriter(new FileWriter(filename.toString()));
		outB.write(fileContent.toString());
		outB.close();
	}
	
	public static void main(String[] args) {
//		char[] specifiers = { 'u', 'g', 'o', 'a'};
//		char[] ops = { '+', '-', '=' };
//		char[] bits = { 'r', 'w', 'x' };
		
		FileSystem.getOnlyInstance();
		UserSystem.performOpAsRoot("ls -l /");
	}

	public static void exec(String filename) throws AccessDeniedException, FsElementNotFoundException, FsException {
		addToHistory = false;
		FileSystem.getOnlyInstance().execute(filename);
		addToHistory = true;
	}
}
