package newworld.fs;

import java.util.ArrayList;
import java.util.Iterator;

import vbe.CommonFunctions;
import vbe.exceptions.fs.FsException;
import vbe.usersystem.IGroup;
import vbe.usersystem.IUser;
import vbe.usersystem.UserSystem;

class File extends FsElement implements IFile, Iterable<String> {
	private static final long serialVersionUID = -8490284838021875752L;
	private ArrayList<String> lines;
	
	@Override
	public File clone() {
		File ret = new File(getName(), getDateCreatedAt(), getUser(), getGroup(), getPerms(), getParentDir());
		for (String line : lines)
			ret.lines.add(line);
		return ret;
	}
	
	public File(String name, String dateCreatedAt, IUser user, IGroup group, boolean[] perms, Directory parentDir) {
		super(name, dateCreatedAt, user, group, perms, parentDir);
		lines = new ArrayList<String>();
		updateSize();
	}

	public File(String name, Directory parentDir) {
		this(name, FileSystem.getDate(), UserSystem.curUser, UserSystem.curUser.getPrimaryGroup(), FileSystem.DEFAULT_PERMS, parentDir);
	}

	@Override
	public Iterator<String> iterator() {
		return new Iterator<String>() {
			private int i = 0;
			public boolean hasNext() { return i < length(); }
			public String next() { return lines.get(i++); }
		};
	}
	
	@Override
	public void updateSize() {
		super.updateSize();
		for (String line : lines)
			size += line.length()*2;
	}

	@Override
	public int length() { return lines.size(); }

	@Override
	public void add(String line) { lines.add(line); }

	@Override
	public void add(String line, int index) { lines.add(index, line); }

	@Override
	public void remove(String s) { 
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).equals(s)) lines.remove(i);
		}
	}
	@Override
	public void remove(int index) { lines.remove(index); }

	public int get(String pattern) {
		for (int i = 0; i < lines.size(); i++)
			if (lines.get(i).indexOf(pattern) != -1) return i;
		return -1;
	}
	
	public void clear() { lines.clear(); }

	public String get(int index) {
		return lines.get(index);
	}

	public void set(int index, String string) {
		lines.set(index, string);
	}

	@Override
	public String[] getChildPaths() {
		return null;
	}
	
	public void execute() throws FsException {
		if (!isExecutable())
			throw new FsException(getPath(), getName() + " is not an executable script! (scripts are defined through their .sh extension");
		for (String com : this)
			UserSystem.performOpAsCurUser(com);
	}

	public boolean isExecutable() {
		return getName().endsWith(".sh");
	}

	public String toString(int depth) {
		String ret = super.toString(depth);
		for (String line : this)
			ret += CommonFunctions.duplicateChar('\t', depth) +  line + "\n";
		return ret;
	}
}
