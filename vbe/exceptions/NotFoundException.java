package vbe.exceptions;

public class NotFoundException extends GeneralException {
	private static final long serialVersionUID = -7553765461928079623L;
	private String name;
	public NotFoundException(String name, String msg) {
		super(msg);
		this.name = name;
	}

	public NotFoundException(String msg) {
		this("", msg);
	}

	public String getMessage() {
		return "NotFoundException; name: " + name + "; : " + super.getMessage();
	}
}
