package functionality;

public class HeapSpaceException extends Exception {

	public HeapSpaceException(String message) {
		super(message);
	}
	
	public HeapSpaceException(Throwable cause) {
		super(cause);
	}
	
	public HeapSpaceException(String message, Throwable cause) {
		super(message, cause);
	}
}
