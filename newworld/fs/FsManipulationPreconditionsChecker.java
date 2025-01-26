package newworld.fs;

import vbe.exceptions.DuplicateException;

class FsManipulationPreconditionsChecker {

	public static void checkPredefinedNames(String name) throws DuplicateException {
		if (name.equals(".") || name.equals("..")) throw new DuplicateException(name, "Directory");
	}

}
