package functionality;

public class NotEnoughAddActorsException extends Exception{
	
	public NotEnoughAddActorsException(String message) {
		super(message);
	}
	
	public NotEnoughAddActorsException(Throwable cause) {
		super(cause);
	}
	
	public NotEnoughAddActorsException(String message, Throwable cause) {
		super(message, cause);
	}

}
