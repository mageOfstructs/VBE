package vbe.commands.fs;

import vbe.exceptions.NotFoundException;
import vbe.exceptions.fs.FsException;

public interface IFile extends IFsElement {
	public String[] getPublicContent();
	public IFile clone(String name, IDirectory parentDir);
	public void listContent();
	
	public void addLine(String line, boolean overrideExisting);
	public void setLine(int lineIndex, String newLine);
	public int find(String pattern);
	public String getLine(int lineIndex);
	public void appendToLine(int find, String string);
	public void listContent(int lines);
	public void execute() throws FsException;
	public void remLine(String pattern) throws NotFoundException;
	public int startsWith(String pattern);
	public int length();
}
