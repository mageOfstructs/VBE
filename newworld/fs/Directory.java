package newworld.fs;

import java.util.ArrayList;
import java.util.Iterator;

import vbe.CommonFunctions;
import vbe.exceptions.fs.FsElementNotFoundException;
import vbe.exceptions.usersystem.AccessDeniedException;
import vbe.usersystem.IGroup;
import vbe.usersystem.IUser;
import vbe.usersystem.UserSystem;

class Directory extends FsElement implements IDirectory, IDirForCommercialUse, Iterable<FsElement> {
	private static final long serialVersionUID = -7566118432643411099L;
	private final char[] UNITS = { ' ', 'K', 'M', 'G', 'T' };
	private ArrayList<Directory> dirs;
	private ArrayList<File> files;
	
	public Directory(String name, String dateCreatedAt, IUser user, IGroup group, boolean[] perms,
			Directory parentDir) {
		super(name, dateCreatedAt, user, group, perms, parentDir);
		dirs = new ArrayList<>();
		files = new ArrayList<>();
	}

	public Directory(String name, Directory parentDir) {
		this(name, FileSystem.getDate(), UserSystem.curUser, UserSystem.curUser.getPrimaryGroup(), FileSystem.DEFAULT_PERMS, parentDir);
	}

	public Directory clone() {
		Directory ret = new Directory(getName(), getDateCreatedAt(), getUser(), getGroup(), getPerms(), getParentDir());
		for (Directory dir : dirs)
			ret.dirs.add(dir);
		for (File file : files)
			ret.files.add(file);
		return ret;
	}
	
	public void updateSize() {
		super.updateSize();
		Iterator<FsElement> it = iterator();
		while (it.hasNext()) {
			FsElement el = (FsElement) it.next();
			el.updateSize();
			size += el.getSize();
		}
	}
	
	@Override
	public Iterator<FsElement> iterator() {
		return new Iterator<FsElement>() {
			private int i = 0;
			public boolean hasNext() {
				return i < length();
			}
			public FsElement next() {
				return i < dirs.size() ? dirs.get(i++) : files.get(i++-dirs.size());
			}
		};
	}

	@Override
	public int length() {
		if (dirs == null) dirs = new ArrayList<>();
		if (files == null) files = new ArrayList<>();
		return dirs.size() + files.size();
	}

	@Override
	public void remove(int index) {
		if (index < dirs.size())
			dirs.remove(index);
		else
			files.remove(index-dirs.size());
	}

	@Override
	public void add(IDirectory dirToAdd) {
		dirs.add((Directory) dirToAdd);
	}

	@Override
	public void add(IFile fileToAdd) {
		files.add((File) fileToAdd);
	}
	
	@Override
	public void remove(IDirectory dirToRemove) {
		dirs.remove(dirToRemove);
	}

	@Override
	public void remove(IFile fileToRemove) {
		files.remove(fileToRemove);
	}
	
	public ArrayList<Directory> getDir(String dir) {
		ArrayList<Directory> ret = new ArrayList<>();
		for (Directory d : dirs) {
			if (d.equals(dir)) ret.add(d);
		}
		return ret;
	}

	public ArrayList<File> getFile(String filename) {
		ArrayList<File> ret = new ArrayList<>();
		String[] options = filename.split("\\|");
		for (File d : files) {
			for (String option : options) {
				if (d.equals(option)) ret.add(d);
			}
		}
		return ret;
	}

	public ArrayList<FsElement> get(String name) throws FsElementNotFoundException {
		ArrayList<Directory> dirs = getDir(name);
		ArrayList<File> files = getFile(name);
		if (dirs.isEmpty() && files.isEmpty()) throw new FsElementNotFoundException(name);
		ArrayList<FsElement> ret = new ArrayList<>();
		for (Directory d : dirs) ret.add(d);
		for (File f : files) ret.add(f);
		return ret;
	}

	public FsElement get(int i) {
		if (i < 0 || i >= length()) throw new IllegalArgumentException();
		return i < dirs.size() ? dirs.get(i) : files.get(i-dirs.size());
	}

	@Override
	public void listContents(boolean verbose, boolean showHidden, boolean humanReadableUnits) throws AccessDeniedException {
		if (!checkPerms(0))
			throw new AccessDeniedException(0, getPath());
		if (!verbose) {
			for (FsElement el : this)
				if (showHidden || !el.getName().startsWith("."))
					System.out.print(((FsElement) el).getName() + " ");
			if (length() > 0) System.out.println();
		} else {
			for (FsElement el : this) {
				int size = el.getSize();
				int exp = 0;
				if (humanReadableUnits) {
					while (size >= Math.pow(1000, exp)) { exp++; }
					size = size / (int) Math.pow(1000, --exp);
				}
				
				if (showHidden || !el.getName().startsWith(".")) {
					System.out.printf((el instanceof Directory ? "d" : "-") + el.permsToString() + " " + el.getLinks() + " " + el.getUser().getName() + " " + el.getGroup().getName() + " %1$03d" + (exp < 5 ? UNITS[exp] : "OVERFLOW!") + el.getDateCreatedAt() + " " + el.getName() + "%n", size);
				}
			}
		}
	}

	public String[] getChildPaths() {
		String[] ret = new String[length()];
		int retIndex = 0;
		for (FsElement el : this)
			ret[retIndex++] = el.getPath();
		return ret;
	}
	
	public String toString(int depth) {
		String ret = super.toString(depth);
		for (FsElement el : this)
			ret += CommonFunctions.duplicateChar('\t', depth) + el.toString(depth+1) + "\n";
		return ret;
	}
	
	public Directory getSingleDir(String name) {
		for (Directory d : dirs) if (d.getName().equals(name)) return d;
		return null;
	}
	public File getSingleFile(String name) {
		for (File f : files) if (f.getName().equals(name)) return f;
		return null;
	}

	public FsElement getSingle(String name) throws FsElementNotFoundException {
		FsElement ret = getSingleDir(name);
		if (ret == null) ret = getSingleFile(name);
		if (ret == null) throw new FsElementNotFoundException(getPath() + "/" + name);
		return ret;
	}
}
