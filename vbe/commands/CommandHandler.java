package vbe.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import newworld.fs.FileSystem;
import vbe.GlobalVars;
import vbe.exceptions.CommandSyntaxException;
import vbe.exceptions.GeneralException;
import vbe.exceptions.ParameterIndexOutOfRangeException;
import vbe.exceptions.fs.FsElementNotFoundException;
import vbe.exceptions.usersystem.AccessDeniedException;
import vbe.usersystem.UserSystem;

public class CommandHandler {
	private static int flagsSize = 0;
	public static final String[] AVAILABLE_COMMANDS = {"help - prints this message",
			"pwd - print working directory", 
			"\n[Simple navigation tools]",
			"ls [path] - lists Directories and Files of the given path; If left empty, it'll list the contents of the current direcotry", 
			"cd [path] - change the working directory to the given path; If left empty, it'll change to the current working directory (and do nothing)", 
			"\n[Simple file system manipulation]",
			"mkdir [pathWithDirectoryName] - create the directory specified in the argument; If left empty, it'll use Element+[increasingNumber] as name and create it in the current direcotry",
			"rmdir <path> - removes the directory at the given path if it's empty, otherwise fails", 
			"touch [pathWithFileName] - creates an empty file at the given path (or in the current directory if only a name was given); Uses the same default name as mkdir", 
			"rm [-r] <path> removes the file at the given path, if -r is specified a directory together with its content will be removed", 
			"\n[Advanced file system manipulation]",
			"cp <source> <destination> - copies the element at source to destination (name can be changed)", 
			"mv <source> <destination> - moves the element at source to destination (name can be changed, therefore it can be used as a rename command)", 
			"cat <pathToFile> - lists the contents of the file at the given path", 
			"head [-n lines] [-numeric] <pathToFile> - prints the first n lines of a file",
			"echo <text> >|>> <path> - overrides(>)/appends to(>>) the contents of the file at path with/the parameter text", 
			"\n[User system manipulation]",
			"su [user] - switch user to given user; If left empty, it'll use root", 
			"useradd [-m] <name> - creates a user with the given name; If -m is specified, useradd also creates a folder in /home", 
			"usermod [options] <user> - modifies the user",
			"userdel <name> - deletes a user",
			"groupadd [-U userlist] <name> - creates a group and puts the users in userlist in it",
			"passwd [name] - changes the password of the given user; If left empty, it'll use the currently logged in user", 
			"id [name] - returns the uid, gid and the names and ids of all the user's groups. Uses the current User if name is left empty",
			"groups [name] - returns the names of the groups of the user. Uses the current User if name is left empty",
			"chown <owner>[:group] <path> - changes the owner (and possibly group) of the element at path",
			"chmod <numeric|[ugoa][+=-][rwx]> <path> - changes the permissions of the element at path. The numeric value will be converted to a nine-digit binary",
			"\n[Other]",
			"history [index] - returns a list of the previously executed commands, if an index within the boundaries of this list was given, it executes that command",
			"sudo <command> - If the current user is part of the 'sudo' group, this will execute the given command as root",
			"saveState - manually saves the root directory to a file",
			"report [message] - creates a bug report and saves it to a file named VBASHE_report.txt",
			"exit - exits the program"};
	
	public static final Map<String, Flag[]> POSSIBLE_FLAGS = new HashMap<>();
	
	private static String command = "";
	private static String[] tokens;
	
	public static class Flag implements Comparable<Flag> {
		public String flag;
		public int flagIndex, tokenIndex;
		public boolean needsParam;
		public Flag(String flag, int flagIndex, int tokenIndex, boolean needsParam) {
			this.flag = flag;
			this.flagIndex = flagIndex;
			this.tokenIndex = tokenIndex;
			this.needsParam = needsParam;
		}
		public Flag(String flag, int flagIndex, int tokenIndex) {
			this(flag, flagIndex, tokenIndex, false);
		}
		public Flag(String flag, boolean needsParam) {
			this(flag, -1, -1, needsParam);
		}
		public Flag(String flag) {
			this(flag, false);
		}
		
		public int compareTo(Flag flag) {
			if (flagIndex < ((Flag)flag).flagIndex) return -1;
			else if (flagIndex > ((Flag)flag).flagIndex) return 1;
			else return 0;
		}
		
		public String toString() { return flag; }
		public static Flag[] makeClone(Flag[] flags) {
			if (flags == null) return null;
			Flag[] ret = new Flag[flags.length];
			for (int i = 0; i < ret.length; i++)
				ret[i] = new Flag(flags[i].flag, flags[i].flagIndex, flags[i].tokenIndex, flags[i].needsParam);
			return ret;
		}
	}
	
	private static int getComPrefix() {
		return tokens[0].length() + 2;
	}
	
	public static void initPosFlags() {
		POSSIBLE_FLAGS.put("help", Commands.EMPTY_FLAGARR);
		POSSIBLE_FLAGS.put("pwd", Commands.EMPTY_FLAGARR);
		POSSIBLE_FLAGS.put("ls", new Flag[]{new Flag("l"), new Flag("a"), new Flag("h"), new Flag("all"), new Flag("human-readable")});
		POSSIBLE_FLAGS.put("cd", Commands.EMPTY_FLAGARR); // would only be needed if links get implemented
		POSSIBLE_FLAGS.put("mkdir", Commands.EMPTY_FLAGARR);
		POSSIBLE_FLAGS.put("touch", Commands.EMPTY_FLAGARR);
		POSSIBLE_FLAGS.put("rmdir", Commands.EMPTY_FLAGARR);
		POSSIBLE_FLAGS.put("rm", new Flag[]{new Flag("r"), new Flag("R"), new Flag("recursive")});
		POSSIBLE_FLAGS.put("cp", Commands.EMPTY_FLAGARR);
		POSSIBLE_FLAGS.put("mv", Commands.EMPTY_FLAGARR);
		POSSIBLE_FLAGS.put("cat", Commands.EMPTY_FLAGARR);
		POSSIBLE_FLAGS.put("head", new Flag[]{new Flag("n", true), new Flag("lines", true)}); // remember to add the =-char stuff <= I can only have one or both...
		POSSIBLE_FLAGS.put("echo", Commands.EMPTY_FLAGARR);
		POSSIBLE_FLAGS.put("su", Commands.EMPTY_FLAGARR);
		POSSIBLE_FLAGS.put("useradd", new Flag[]{new Flag("m"), new Flag("g", true), new Flag("G", true), new Flag("c", true), new Flag("create-home"), new Flag("gid", true), new Flag("groups", true), new Flag("comment")}); 
		POSSIBLE_FLAGS.put("groupadd", new Flag[]{new Flag("U", true), new Flag("users", true)});
		POSSIBLE_FLAGS.put("passwd", Commands.EMPTY_FLAGARR);
		POSSIBLE_FLAGS.put("chown", Commands.EMPTY_FLAGARR);
		POSSIBLE_FLAGS.put("chmod", new Flag[]{new Flag("R"), new Flag("recursive")});
		POSSIBLE_FLAGS.put("usermod", new Flag[] {new Flag("a"), new Flag("d", true), new Flag("g", true), new Flag("G", true), new Flag("l", true), new Flag("L"), new Flag("m"), new Flag("r"), new Flag("c", true), new Flag("u"), new Flag("append"), new Flag("home"), new Flag("gid", true), new Flag("groups", true), new Flag("login", true), new Flag("lock"), new Flag("move-home"), new Flag("remove"), new Flag("comment", true), new Flag("unlock")});
	}
	
	public static void handleCommand(String command) throws IOException {
		CommandHandler.command = command;
		handleCommand(handleTokens(command));
	}
	
	/** 
	 * processes commands and redirects to command method, providing them with detected flags and parameters 
	 * @author Jason Puschnig
	 * @throws IOException 
	**/
	public static void handleCommand(String[] tokens) throws IOException {
		try {
			CommandHandler.tokens = tokens; // set class variables
			if (command.isEmpty())
				for (String s : tokens)
					command += s + " ";
			
			if (UserSystem.curUser.getHome() != null) { // update .bash_history
				try {
					try {
						UserSystem.curUser.getHome().getSingle(".bash_history");
					} catch (FsElementNotFoundException e) {
						FileSystem.getOnlyInstance().add(UserSystem.curUser.getHomePath() + "/.bash_history", false);
					}
					if (Commands.addToHistory()) FileSystem.getOnlyInstance().addSingleLine(UserSystem.curUser.getHomePath() + "/.bash_history", false, command);
				} catch (FsElementNotFoundException e) {
					System.out.println("bash: Could not update .bash_history");
				}
			}
			
			Flag[] flags = Flag.makeClone(POSSIBLE_FLAGS.get(tokens[0]));
			String[] params = null;
			
			if (!tokens[0].equals("sudo")) {
				handleFlags(tokens, flags);
				params = handleFlaglessParams(tokens, flags);
				flagsSize = 0;
			}

			switch (tokens[0]) {
			case "sv":
				GlobalVars.verboseErrMsg = !GlobalVars.verboseErrMsg;
				System.out.println("Switched to " + (GlobalVars.verboseErrMsg ? "verbose" : "normal") + " mode!");
				break;
			case "help":
				Commands.help();
				break;
			case "pwd":
				Commands.pwd();
				break;
			case "ls":
				Commands.ls(flags, params[0]);
				break;
			case "cd":
				Commands.cd(flags, params[0]);
				break;
			case "mkdir":
				Commands.mk(flags, params[0], true);
				break;
			case "rmdir":
				Commands.rm(flags, params[0], true);
				break;
			case "touch":
				Commands.mk(flags, params[0], false);
				break;
			case "rm":
				Commands.rm(flags, params[0]);
				break;
			case "cp":
				checkParams(command, params, 2);
				Commands.cp(flags, params[0], params[1]);
				break;
			case "mv":
				checkParams(command, params, 2);
				Commands.mv(flags, params[0], params[1]);
				break;
			case "cat":
				checkParams(command, params, 1);
				Commands.cat(flags, params[0]);
				break;
			case "head":
				checkParams(command, params, 1);
				Commands.head(flags, tokens);
				break;
			case "echo":
				params = checkParamsAndFill(command, params, 3);
				Commands.echo(flags, params[0], params[1], params[2]);
				break;
			case "su":
				Commands.su(flags, params[0]);
				break;
			case "useradd":
				checkParams(command, params, 1);
				Commands.useradd(flags, tokens);
				break;
			case "passwd":
				Commands.passwd(flags, params[0]);
				break;
			case "chown":
				checkParams(command, params, 2);
				Commands.chown(flags, params[0], params[1]);
				break;
			case "chmod":
				checkParams(command, params, 2);
				Commands.chmod(flags, params[0], params[1]);
				break;
			case "groupadd":
				Commands.groupadd(flags, params[0]);
				break;
			case "userdel":
				Commands.userdel(flags, params[0]);
				break;
			case "id":
				Commands.id(flags, params[0]);
				break;
			case "groups":
				Commands.groups(flags, params[0]);
				break;
			case "usermod":
				Commands.usermod(flags, params[0]);
				break;
			case "history":
				Commands.history(params[0]);
				break;
			case "sudo":
				Commands.sudo(tokens);
				break;
			case "saveState":
				Commands.write();
				break;
			case "report":
				checkParams(command, params, 1);
				Commands.report(params[0]);
				break;
			case "exit":
				Commands.exit();
				break;
			default:
				if (FileSystem.isPath(tokens[0])) Commands.exec(tokens[0]);
				else Commands.notFound();
			}
		} catch (AccessDeniedException e) {
			System.out.println(tokens[0] + ": " + e.getMessage(GlobalVars.verboseErrMsg));
		} catch (GeneralException e) {
			System.out.println(tokens[0] + ": " + e.getMessage(GlobalVars.verboseErrMsg));
		} catch (StackOverflowError e) { 
			System.out.println("Maximum recursion depth exceeded.");
		} catch (Exception e) {
			System.out.print(GlobalVars.ERR_PREFIX);
			System.out.println(FileSystem.getOnlyInstance().dump());
			e.printStackTrace();
		} finally {
			command = "";
			CommandHandler.tokens = null;
		}
	}
	
	/**
	 * first function to be called;
	 * reads from the input stream until an End Of Line Symbol (here only the beginning of said symbol => \r)
	 * @return raw command typed by the user
	 * @throws IOException
	 */
	public static String handleInput() throws IOException {
		String ret = "";
		int curByte = System.in.read();
		while (curByte != 10) {
			ret += (char) curByte;
			curByte = System.in.read();
		}
		return ret;
	}
	
	public static String[] handleFlaglessParams(String[] tokens, Flag... flagsWithParamsIndexes) throws CommandSyntaxException {
		if (tokens.length - flagsSize - 1 < 1) return new String[] { null };
		String[] ret = new String[tokens.length - flagsSize - 1];
		int retIndex = 0, j = 0;
		for (int i = 1; i < tokens.length; i++) {
			if (flagsWithParamsIndexes == null && tokens[i].charAt(0) == '-') {
				throw new CommandSyntaxException(command, command.indexOf(tokens[i]), "Invalid option -- '" + tokens[i].substring(1) + "'");
			} else if (flagsWithParamsIndexes == null || tokens[i].charAt(0) != '-') {
				while (flagsWithParamsIndexes != null && j < flagsWithParamsIndexes.length && i != flagsWithParamsIndexes[j].tokenIndex+1) j++;
				boolean needsParam = flagsWithParamsIndexes != null && j < flagsWithParamsIndexes.length ? flagsWithParamsIndexes[j].needsParam : false;
				if (flagsWithParamsIndexes != null && j < flagsWithParamsIndexes.length && tokens[i-1].charAt(1) != '-' &&
						tokens[i-1].length() > 2) { // check if there are multiple flags
					needsParam = flagsWithParamsIndexes[j].needsParam;
					int k = 2;
					while (k < tokens[i-1].length() && !needsParam) {
						j = 0;
						while (flagsWithParamsIndexes != null && j < flagsWithParamsIndexes.length && tokens[i-1].charAt(k) != flagsWithParamsIndexes[j].flag.charAt(0)) j++;
						if (j < flagsWithParamsIndexes.length) needsParam = flagsWithParamsIndexes[j].needsParam;
						k++;
					}
				}
				if (!needsParam) ret[retIndex++] = tokens[i];
				j = 0;
			}
		}
		return ret;
	}
	
	/**
	 * splits the raw command into text tokens, with the ' ' whitespace acting as a "End of Token" symbol
	 * @param command
	 * @return String array with the tokens
	 */
	public static String[] handleTokens(String command) {
		ArrayList<String> params = new ArrayList<String>();
		int i = 0;
		while (i < command.length()) {
			String tmp = "";
			if (command.charAt(i) == '\"') {
				while (++i < command.length() && ((i != 0 && command.charAt(i-1) == '\\') || command.charAt(i) != '\"') && command.charAt(i) != '\r')
					if (command.charAt(i) != '\\') tmp += command.charAt(i);
				i++;
			} else
				while (i < command.length() && command.charAt(i) != ' ' && command.charAt(i) != '\r')
					tmp += command.charAt(i++);
			params.add(tmp);
			i++;
		}
		String[] ret = new String[params.size()];
		for (int j = 0; j < params.size(); j++) // copy elements of ArrayList into regular Array
			ret[j] = params.get(j);
		return ret;
	}
	
	/**
	 * extracts all flags from the given token array
	 * @param tokens
	 * @return String array with the flags
	 * @throws CommandSyntaxException 
	 */
	private static void handleFlags(String[] tokens, Flag[] flags) throws CommandSyntaxException {
		if (flags == null) return;
		int i = 0, j = 1, k = 1, comIndex = tokens[0].length()+1;
		i = 1;
		while (i < tokens.length) {
			if (tokens[i].startsWith("--")) {
				String flag = tokens[i].substring(2);
				k = 0;
				while (k < flags.length && !flags[k].flag.equals(flag)) { k++; }
				if (k == flags.length) throw new CommandSyntaxException(command, comIndex+getComPrefix(), "Invalid option -- '" + flag + "'");
				flags[k].tokenIndex = i;
				flags[k].flagIndex = k;
				flagsSize++;
			}
			else if (tokens[i].startsWith("-")) {
				for (j = 1; j < tokens[i].length(); j++) {
					String flag = tokens[i].substring(j, j+1);
					k = 0;
					while (k < flags.length && flags[k].flag.charAt(0) != flag.charAt(0)) { k++; }
					if (k == flags.length) throw new CommandSyntaxException(command, comIndex+getComPrefix(), "Invalid option -- '" + flag + "'");
					flags[k].tokenIndex = i;
					flags[k].flagIndex = k;
				}
				flagsSize++;
			}
			comIndex += tokens[i].length()+1;
			i++;
		}
	}

	@Deprecated
	public static int getFlag(String flag, Flag[] flags) throws CommandSyntaxException {
		if (flags[0].flag.equals(flag)) return 0;
		else throw new CommandSyntaxException(command, command.indexOf(flags[0].flag));
	}
	
	public static int[] getFlags(String[] possibleFlags, Flag[] flags) throws CommandSyntaxException { // old world, I think...
		int[] ret = new int[possibleFlags.length]; // flags that aren't found get set to 0 since no flag can be at that position (command name is always at index 0)
		int j;
		for (int i = 0; i < flags.length; i++) {
			j = 0;
			while (j < possibleFlags.length && !possibleFlags[j].equals(flags[i].flag)) {
				j++;
			}
			if (j < possibleFlags.length) {
				ret[j] = flags[i].tokenIndex;
			}
			else throw new CommandSyntaxException(command, command.indexOf(flags[i].flag));
		}
		return ret;
	}
	
	private static void checkParams(String rawCommand, String[] params, int minLength) throws ParameterIndexOutOfRangeException {
		if (!(params.length >= minLength))
			throw new ParameterIndexOutOfRangeException(minLength);
	}
	
	private static String[] checkParamsAndFill(String rawCommand, String[] params, int minLength) throws ParameterIndexOutOfRangeException {
		if (params.length < minLength) {
			String[] ret = new String[minLength];
			for (int i = 0; i < params.length; i++)
				ret[i] = params[i];
			return ret;
		}
		return params;
	}
	
	/**
	 * @return the tokens
	 */
	public static String[] getTokens() {
		return tokens;
	}

	public static void main(String[] args) throws IOException {
		//FileSystem.init(args);
//		String command = "";
//		for (int i = 0; i < 10000; i++) {
//			command += i;
//			System.out.println(i);
//		}
//		handleCommand(command);
//		for (Flag s : handleFlags(new String[]{"useradd", "-m", "-g", "users", "-G", "wheel", "jason"}, POSSIBLE_FLAGS.get("useradd"))) {
//			System.out.println(s);
//		}
	}

	public static String getCommand() {
		return command;
	}
	
	public static String getComName() {
		return tokens[0];
	}
}
