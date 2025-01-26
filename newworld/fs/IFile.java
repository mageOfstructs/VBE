package newworld.fs;

interface IFile {
	void add(String line);
	void add(String line, int index);
	
	void remove(String line);
	void remove(int index);
}
