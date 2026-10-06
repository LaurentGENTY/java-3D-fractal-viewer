package exceptions;

public class NombreNegatifException extends Exception{

	private String message;
	
	public NombreNegatifException(String message)
	{
		this.setMessage(message);
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}