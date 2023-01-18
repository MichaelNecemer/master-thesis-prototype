package functionality;

public class NotEnoughVerifiersException extends Exception{
	
	public NotEnoughVerifiersException(String message) {
		super(message);
	}
	
	public NotEnoughVerifiersException(Throwable cause) {
		super(cause);
	}
	
	public NotEnoughVerifiersException(String message, Throwable cause) {
		super(message, cause);
	}

}
