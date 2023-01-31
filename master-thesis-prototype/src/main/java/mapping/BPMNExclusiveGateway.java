package mapping;

import java.util.LinkedList;

import functionality.Constraint;

//Exclusive Gateways are binary
public class BPMNExclusiveGateway extends BPMNGateway {

	private int amountAddActors;
	private int amountAddActorsSameChoice;
	private int amountLoops;
	private String sphere;
	private static int exclusiveGtwCount = 0;
	private LinkedList<Constraint> constraints;

	public BPMNExclusiveGateway(String id, String name, String type) {
		super(id, name, type);
		this.amountAddActors = 0;
		this.amountAddActorsSameChoice = 0;
		this.amountLoops = 0;
		// by default the sphere is private
		this.sphere = "Private";
		// by default there are no constraints
		this.constraints = new LinkedList<Constraint>();
	}

	public void printElement() {
		super.printElement();
		System.out.println(", " + this.getType());
		this.printLabels();
	}

	public int getAmountAddActors() {
		return amountAddActors;
	}

	public void setAmountAddActors(int amountAddActors) {
		this.amountAddActors = amountAddActors;
	}

	public int getAmountAddActorsSameChoice() {
		return amountAddActorsSameChoice;
	}

	public void setAmountAddActorsSameChoice(int amountAddActors) {
		this.amountAddActorsSameChoice = amountAddActors;
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
		this.sphere = sphere;
	}

	public String getSphere() {
		return this.sphere;
	}

	public LinkedList<Constraint> getConstraints() {
		return this.constraints;
	}

}
