package functionality;

public class Constraint {

	private static int constraintId = 1;
	private int id; 
	
	public Constraint() {
		this.id=constraintId++;		
	}
	
	public int getId() {
		return this.id;
	}
	
}
