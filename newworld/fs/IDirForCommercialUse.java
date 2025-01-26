package newworld.fs;

import java.util.ArrayList;

import vbe.exceptions.DuplicateException;
import vbe.exceptions.fs.FsElementNotFoundException;
import vbe.exceptions.usersystem.AccessDeniedException;

public interface IDirForCommercialUse {
	String getPath();
	
	ArrayList<FsElement> get(String name) throws FsElementNotFoundException;
	ArrayList<File> getFile(String name);

	String getName();

	default void addD(FsElement el) throws DuplicateException {
		if (el instanceof Directory) DirectoryManager.add((Directory) this, (IDirectory) el);
		else DirectoryManager.add((Directory) this, (IFile) el);
	}

	boolean checkPerms(int i);

	void listContents(boolean verbose, boolean showHidden, boolean humanReadableUnits) throws AccessDeniedException;

	FsElement getSingle(String name) throws FsElementNotFoundException;

	IDirForCommercialUse getParentDir();
}