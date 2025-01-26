package vbe.commands.fs;

import java.util.ArrayList;

import vbe.CommonFunctions;
import vbe.commands.CommandHandler;
import vbe.exceptions.DuplicateException;
import vbe.exceptions.fs.ElementIsRequiredBySystemException;
import vbe.exceptions.fs.FsElementBusyException;
import vbe.exceptions.fs.FsElementNotFoundException;
import vbe.exceptions.fs.FsException;
import vbe.exceptions.fs.WrongElementType;
import vbe.exceptions.usersystem.AccessDeniedException;
import vbe.usersystem.IGroup;
import vbe.usersystem.IUser;
import vbe.usersystem.UserSystem;

public class Directory extends FsElement implements IDirectory {
	private static final long serialVersionUID = -4459735020172033813L;
	private ArrayList<FsElement> content = new ArrayList<FsElement>();
	private final char[] UNITS = { ' ', 'K', 'M', 'G', 'T' };
	
	@SuppressWarnings("unchecked")
	public Directory(String name, String dateCreatedAt, IUser owner, IGroup group, boolean[] perms, IDirectory parentDir, ArrayList<FsElement> content) {
		super(name, dateCreatedAt, owner, group, perms, parentDir, true);
		this.content = (ArrayList<FsElement>) content.clone();
		for (FsElement el : content) {
			cachedSize += el.getSize();
		}
	}
	
	public FsElement getElementFromContentByName(String elName, boolean isDir) throws FsElementNotFoundException, WrongElementType {
		for (FsElement dir : content)
			if (dir.isDirectory() == isDir && dir.getName().equals(elName))
				return dir;
			else if (dir.getName().equals(elName))
				throw new WrongElementType(this.getPath()+elName, isDir);
		throw new FsElementNotFoundException(this.getPath() + elName);
	}
	
	public FsElement getElementFromContentByName(String elName) throws FsElementNotFoundException {
		for (FsElement el : content)
			if (el.getName().equals(elName))
				return el;
		throw new FsElementNotFoundException(this.getPath() + elName);
	}
	
	/**
	 * does the exception handling and calls the correct add method for adding a Directory/File
	 * @param params either a String or the IFile/IDirectory object to add
	 * @param createDir
	 * @throws AccessDeniedException 
	 * @throws DuplicateException 
	 */
	public void add(Object params, boolean createDir) throws AccessDeniedException, DuplicateException {
		IFsElement ret = null;
		try {
			String name = params instanceof String ? (String) params : ((FsElement) params).getName();
			getElementFromContentByName(name);
			throw new DuplicateException(CommandHandler.getComName(), "cannot create " + (createDir ? "directory" : "file") + " '" + name + "': File exists");
		} catch (FsElementNotFoundException e) {
			if (!checkPerms(1))
				throw new AccessDeniedException(1, getPath());
			if (createDir && params instanceof String)
				ret = addDir((String) params);
			else if (params instanceof String)
				ret = addFile((String) params);
			else if (createDir)
				ret = addDir((IDirectory) params);
			else
				ret = addFile((IFile) params);
		}
		FileSystem.setLastCreationPointer(ret);
		cachedSize += ret.getSize();
	}
	
	public IDirectory addDir(String name) {
		content.add(new Directory(name, newworld.fs.FileSystem.getDate(), UserSystem.curUser, UserSystem.curUser.getPrimaryGroup(), FileSystem.DEFAULT_PERMS, this, new ArrayList<FsElement>()));
		return (IDirectory) content.get(content.size()-1);
	}

	public IDirectory addDir(IDirectory dir) {
		content.add((Directory) dir);
		return (IDirectory) content.get(content.size()-1);
	}
	
	public IFile addFile(String name) { 
		content.add(new File(name, newworld.fs.FileSystem.getDate(), UserSystem.curUser, UserSystem.curUser.getPrimaryGroup(), FileSystem.DEFAULT_PERMS, this, new ArrayList<String>()));
		return (IFile) content.get(content.size()-1);
	}
	
	public IFile addFile(IFile file) { 
		content.add((File) file);
		return (IFile) content.get(content.size()-1);
	}
	
	public void rem(String name, boolean remDir, boolean removeRec) throws AccessDeniedException, FsElementNotFoundException {
		try {
			if (!checkPerms((byte) 1))
				throw new AccessDeniedException(1, getPath());
			if (getElementFromContentByName(name).isSystemLocation)
				throw new ElementIsRequiredBySystemException("rem", getElementFromContentByName(name).getPath());
			if (remDir || removeRec)
				remDir(name, removeRec);
			else
				remFile(name);
		} catch (FsException e) {
			System.out.println(FileSystem.ERR_PREFIX + e.getMessage(FileSystem.verboseErrMsg));
		}

	}
	
	@SuppressWarnings("unlikely-arg-type")
	private void remDir(String name, boolean removeRecursively) throws FsException, FsElementNotFoundException {
		IDirectory directoryToDelete = (IDirectory) getElementFromContentByName(name, true); // gets the directory
		if (FileSystem.getCurPath().startsWith(directoryToDelete.getPath()))
			throw new FsElementBusyException(directoryToDelete.getPath());
			// way to still do it (not in the real BASH though):
			//UserSystem.performOpAsRoot("cd " + directoryToDelete.getParentDir().getPath()); // if you are currently in the directory to delete, this gets you out of there, so you don't keep writing to a zombie directory
		if (!directoryToDelete.isEmpty() && !removeRecursively)
			throw new FsException(getPath()+"/"+name, "Directory must be empty! (even though my implementation would allow it, you still have to use the right command.");
		content.remove(directoryToDelete);
		cachedSize -= directoryToDelete.getSize();
	}
	
	@SuppressWarnings("unlikely-arg-type")
	private void remFile(String name) throws FsElementNotFoundException, WrongElementType {
		IFile fileToDelete = (IFile) getElementFromContentByName(name, false);
		content.remove(fileToDelete);
		cachedSize -= fileToDelete.getSize();
	}
	
	public ArrayList<FsElement> getContent() { return content; }
	
	public boolean isEmpty() {
		return content.isEmpty();
	}

	@Override
	public void listContent() {
		listContent(false);
	}
	
	public void listContent(boolean showHidden) {
		for (FsElement el : content)
			if (showHidden || !el.getName().startsWith("."))
				System.out.print(((FsElement) el).getName() + " ");
		System.out.println();
	}
	
	public void listContent(boolean verbose, boolean showHidden) throws AccessDeniedException {
		listContent(verbose, showHidden, false);
	}
	
	@Override
	public void listContent(boolean verbose, boolean showHidden, boolean humanReadableUnits) throws AccessDeniedException {
		if (!checkPerms(0))
			throw new AccessDeniedException(0, getPath());
		if (!verbose)
			listContent(showHidden);
		else
			for (FsElement el : content) {
				int size = el.getSize();
				int exp = 0;
				if (humanReadableUnits) {
					while (size >= Math.pow(1000, exp)) { exp++; }
					size = size / (int) Math.pow(1000, --exp);
				}
				
				if (showHidden || !el.getName().startsWith(".")) {
					System.out.printf((el.isDirectory() ? "d" : "-") + el.permsToString() + " " + el.getLinks() + " " + el.getOwner().getName() + " " + el.getGroup().getName() + " %1$03d" + (exp < 5 ? UNITS[exp] : "OVERFLOW!") + dateCreatedAt + " " + el.getName() + "%n", size);
				}
			}
	}
	
	public FsElement[] getPublicContent() {
		FsElement[] ret = new FsElement[content.size()];
		for (int i = 0; i < ret.length; i++)
			ret[i] = content.get(i);
		return ret;
	}
	
	public IDirectory clone(String name, IDirectory parentDir) {
		return new Directory(name, getDateCreatedAt(), getOwner(), getGroup(), getPerms(), parentDir, getContent());
	}
	
	public String toString(int depth) {
		String ret = super.toString(depth);
		for (FsElement el : content)
			ret += CommonFunctions.duplicateChar('\t', depth) + el.toString(depth+1) + "\n";
		return ret;
	}

	@Override
	public int getLinks() {
		int links = 2;
		for (FsElement el : content) {
			links += el.getLinks();
		}
		return links;
	}
	
	@Override
	public void updateSize() {
		super.updateSize();
		for (FsElement el : content) {
			el.updateSize();
			cachedSize += el.getSize();
		}
	}
}
