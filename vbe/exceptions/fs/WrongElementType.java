package vbe.exceptions.fs;

public class WrongElementType extends FsException {
	private static final long serialVersionUID = 8339875705247207177L;

	public WrongElementType(String path, boolean falseTypeFile) {
		super(path, "Not a " + (falseTypeFile ? "Directory" : "File") + "!");
	}
}
