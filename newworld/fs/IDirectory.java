package newworld.fs;

interface IDirectory {
	void add(IDirectory dirToAdd);
	void add(IFile fileToAdd);
	
	void remove(IDirectory dirToRemove);
	void remove(IFile fileToRemove);
	void remove(int index);
}
