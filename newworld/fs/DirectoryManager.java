package newworld.fs;

import java.util.ArrayList;

import vbe.GlobalVars;
import vbe.exceptions.DuplicateException;
import vbe.exceptions.fs.FsElementNotFoundException;
import vbe.exceptions.fs.FsException;

class DirectoryManager {
	
	private static void checkDuplicate(Directory parentDir, FsElement dirToAdd) throws DuplicateException {
		try {
			parentDir.getSingle(dirToAdd.getName());
			throw new DuplicateException(dirToAdd.getPath(), "element");
		} catch (FsElementNotFoundException e) {}
	}
	
	public static void add(Directory parentDir, IDirectory dirToAdd) throws DuplicateException { 
		checkDuplicate(parentDir, (FsElement) dirToAdd);
		parentDir.add(dirToAdd); 
	}
	public static void removeDirs(IDirectory parentDir, ArrayList<Directory> dirsToRemove) { removeDirs(parentDir, dirsToRemove, false); }
	
	static void removeDirs(IDirectory parentDir, ArrayList<Directory> dirsToRemove, boolean removeRecursively) {
		for (Directory dirToRemove : dirsToRemove) {
			try {
				if (!removeRecursively && dirToRemove.length() != 0) throw new FsException(dirToRemove.getPath(), "Directory must be empty!");
				parentDir.remove(dirToRemove);
			} catch (FsException e) { System.out.println(dirToRemove.getPath() + ": " + e.getMessage(GlobalVars.verboseErrMsg)); }
		}
	}
	
	public static void add(Directory parentDir, IFile fileToAdd) throws DuplicateException { 
		checkDuplicate(parentDir, (FsElement) fileToAdd);
		parentDir.add(fileToAdd);
	}
	public static void removeFiles(IDirectory parentDir, ArrayList<File> filesToRemove) { for (File fileToRemove : filesToRemove) parentDir.remove(fileToRemove); }

	public static ArrayList<FsElement> get(Directory parentDir, String name) throws FsElementNotFoundException {
		return parentDir.get(name);
	}
}
