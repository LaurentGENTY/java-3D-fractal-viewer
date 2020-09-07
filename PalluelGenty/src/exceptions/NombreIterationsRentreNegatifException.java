package exceptions;

public class NombreIterationsRentreNegatifException extends Exception{

	private String message;
	
	public NombreIterationsRentreNegatifException(String message)
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