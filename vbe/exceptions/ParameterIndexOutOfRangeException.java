package vbe.exceptions;

public class ParameterIndexOutOfRangeException extends GeneralException {
	private static final long serialVersionUID = -6237386495628300997L;
	private int minParamCount = 0;
	public ParameterIndexOutOfRangeException(int minParamCount) {
		super("Not enough parameters given!");
		this.minParamCount = minParamCount;
	}
	
	public String getMessage() {
		return "ParameterIndexOutOfRangeException; Minimum: " + minParamCount + " : " + super.getMessage();
	}
}
