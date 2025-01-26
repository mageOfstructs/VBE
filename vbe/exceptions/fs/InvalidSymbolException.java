package vbe.exceptions.fs;

public class InvalidSymbolException extends FsException {

	private static final long serialVersionUID = 5189650060791904139L;

	public InvalidSymbolException(String path, char symbol) {
		super(path, "Invalid symbol '" + symbol + "'");
	}

}
