package exceptions;

public class WrongFormatException extends Exception{
	
	private String message;
	
	public WrongFormatException(String message) {
		this.setMessage(message);
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}