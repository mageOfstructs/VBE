package vbe.commands.fs;

import vbe.exceptions.DuplicateException;
import vbe.exceptions.fs.FsElementNotFoundException;
import vbe.exceptions.fs.WrongElementType;
import vbe.exceptions.usersystem.AccessDeniedException;

public interface IDirectory extends IFsElement {	
	public void add(Object params, boolean createDir) throws AccessDeniedException, DuplicateException;
	
	public void rem(String name, boolean remDir, boolean removeRecursively) throws AccessDeniedException, FsElementNotFoundException;
	
	public boolean isEmpty();
	public FsElement getElementFromContentByName(String dirName, boolean isDir) throws FsElementNotFoundException, WrongElementType;
	public FsElement getElementFromContentByName(String dirName) throws FsElementNotFoundException;
	public void listContent(boolean verbose, boolean showHidden) throws AccessDeniedException;
	public IDirectory clone(String name, IDirectory parentDir);

	public String getPath();

	public void setParentDir(IDirectory root);

	FsElement[] getPublicContent();
	
	String toString(int depth);

	public void listContent(boolean verbose, boolean showHidden, boolean humanReadableUnits) throws AccessDeniedException;

	public IDirectory addDir(IDirectory rootHome);

	public IFile addFile(String filename);

	public void updateSize();
}
