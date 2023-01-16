package mapping;

import java.util.LinkedList;

import functionality.Constraint;

//Exclusive Gateways are binary
public class BPMNExclusiveGateway extends BPMNGateway{
	
	
	private int amountVerifiers;
	private int amountVerifiersSameChoice;
	private int amountLoops;
	private String sphere;
	private static int exclusiveGtwCount = 0;
	private LinkedList<Constraint> constraints; 

		
	public BPMNExclusiveGateway(String id, String name, String type) {
		super(id, name, type);
		this.amountVerifiers=0;	
		this.amountVerifiersSameChoice=0;
		this.amountLoops=0;
		//by default the sphere is private
		this.sphere="Private";
		//by default there are no constraints 
		this.constraints=new LinkedList<Constraint>();
	}


	public void printElement() {
		super.printElement();
		System.out.println(", "+this.getType());
		this.printLabels();
	}


	

	

	public int getAmountVerifiers() {
		return amountVerifiers;
	}






	public void setAmountVerifiers(int amountVerifiers) {
		this.amountVerifiers = amountVerifiers;
	}






	public int getAmountVerifiersSameChoice() {
		return amountVerifiersSameChoice;
	}






	public void setAmountVerifiersSameChoice(int amountVerifiersSameChoice) {
		this.amountVerifiersSameChoice = amountVerifiersSameChoice;
	}






	public int getAmountLoops() {
		return amountLoops;
	}


	public void setAmountLoops(int amountLoops) {
		this.amountLoops = amountLoops;
	}


	public static int getExclusiveGtwCount() {
		return exclusiveGtwCount;
	}

	public static int increaseExclusiveGtwCount() {
		return ++exclusiveGtwCount;
	}
	
	public void setSphere(String sphere) {
		this.sphere=sphere;
	}
	public String getSphere() {
		return this.sphere;
	}
	
	public LinkedList<Constraint> getConstraints(){
		return this.constraints;
	}
	
}
