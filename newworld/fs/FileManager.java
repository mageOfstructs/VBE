package newworld.fs;

class FileManager {
	public static void add(IFile file, String line) { file.add(line); }
	public static void remove(IFile file, String line) { file.remove(line); }
	public static void set(File file, int lineIndex, String line) {
		file.set(lineIndex, line);
	}
}
