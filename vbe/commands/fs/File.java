package vbe.commands.fs;

import java.util.ArrayList;

import vbe.CommonFunctions;
import vbe.exceptions.NotFoundException;
import vbe.exceptions.fs.FsException;
import vbe.usersystem.IGroup;
import vbe.usersystem.IUser;
import vbe.usersystem.UserSystem;

public class File extends FsElement implements IFile {
	private static final long serialVersionUID = -6008263471670529177L;
	private ArrayList<String> content = new ArrayList<String>();
	
	@SuppressWarnings("unchecked")
	public File(String name, String dateCreatedAt, IUser owner, IGroup group, boolean[] perms, IDirectory parentDir, ArrayList<String> content) {
		super(name, dateCreatedAt, owner, group, perms, parentDir, false);
		this.content = (ArrayList<String>) content.clone();
		for (String s : content)
			cachedSize += s.length()*2;
	}
	
	protected ArrayList<String> getContent() { return content; }
	
	public String[] getPublicContent() {
		String[] ret = new String[content.size()];
		for (int i = 0; i < ret.length; i++)
			ret[i] = content.get(i);
		return ret;
	}
	
	public void listContent() {
		for (String line : content)
			System.out.println(line);
	}
	
	@Override
	public void listContent(int lines) {
		if (lines > content.size()) throw new IllegalArgumentException("File too small!");
		for (int i = 0; i < lines; i++)
			System.out.println(content.get(i));
	}
	
	public void setLine(int lineIndex, String newLine) {
		cachedSize += newLine.length()*2 - content.get(lineIndex).length()*2;
		content.set(lineIndex, newLine);
	}
	
	public void addLine(String line, boolean overrideExisting) {
		if (overrideExisting) {
			content.clear();
			cachedSize = super.getSize();
		}
		content.add(line);
		cachedSize += line.length()*2;
	}
	
	public String getLine(int lineIndex) {
		return content.get(lineIndex);
	}
	
	public int startsWith(String pattern) {
		int i = 0;
		while (i < content.size() && !content.get(i).startsWith(pattern)) i++;
		return i < content.size() ? i : -1;
	}
	
	public int find(String pattern) {
		int i = 0;
		while (i < content.size() && content.get(i).indexOf(pattern) == -1) i++;
		return i < content.size() ? i : -1;
	}
	
	public IFile clone(String name, IDirectory parentDir) {
		return new File(name, dateCreatedAt, owner, group, perms, parentDir, content);
	}
	
	public String toString(int depth) {
		String ret = super.toString(depth);
		for (String line : content)
			ret += CommonFunctions.duplicateChar('\t', depth) +  line + "\n";
		return ret;
	}

	@Override
	public void appendToLine(int lineIndex, String string) {
		content.set(lineIndex, content.get(lineIndex) + string);
		cachedSize += string.length()*2;
	}
	
	private boolean isExecutable() { return name.endsWith(".sh"); }

	@Override
	public void execute() throws FsException {
		if (!isExecutable())
			throw new FsException(getPath(), getName() + " is not an executable script! (scripts are defined through their .sh extension");
		for (String com : content)
			UserSystem.performOpAsCurUser(com);
	}

	@Override
	public void remLine(String pattern) throws NotFoundException {
		int index = find(pattern);
		if (index != -1)
			content.remove(index);
		else throw new NotFoundException(pattern, "No line with the pattern '" + pattern + "' found");
	}

	@Override
	public int getLinks() {
		return 1;
	}
	
	public int length() { return content.size(); }
	
	@Override
	public void updateSize() {
		super.updateSize();
		for (String line : content)
			cachedSize += line.length()*2;
	}
}
