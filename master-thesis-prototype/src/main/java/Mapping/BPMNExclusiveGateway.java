package Mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import functionality.Constraint;

//Exclusive Gateways are binary
public class BPMNExclusiveGateway extends BPMNGateway{
	
	
	private int amountVoters;
	private int votersSameChoice;
	private int amountLoops;
	private String sphere;
	private static int exclusiveGtwCount = 0;
	private LinkedList<Constraint> constraints; 
	//contains all incoming ArcWithCost from all branches into the xor-split
	private LinkedList<LinkedList<ArcWithCost>> incomingArcsWithCostAllBranches;
		
	public BPMNExclusiveGateway(String id, String name, String type) {
		super(id, name, type);
		//by default a gateway needs 2 voters and has 2 loops
		this.amountVoters=2;	
		this.votersSameChoice=2;
		this.amountLoops=2;
		//by default the sphere is global
		this.sphere="Global";
		//by default there are no constraints 
		this.constraints=new LinkedList<Constraint>();
		this.incomingArcsWithCostAllBranches=new LinkedList<LinkedList<ArcWithCost>>();
		
	}

	
	
	


	public LinkedList<LinkedList<ArcWithCost>> getIncomingArcsWithCostAllBranches() {
		return incomingArcsWithCostAllBranches;
	}






	public void setIncomingArcsWithCostAllBranches(LinkedList<LinkedList<ArcWithCost>> incomingArcsWithCostAllBranches) {
		this.incomingArcsWithCostAllBranches = incomingArcsWithCostAllBranches;
	}






	public void printElement() {
		super.printElement();
		System.out.println(", "+this.getType());
		this.printLabels();
	}


	
	public int getAmountVoters() {
		return this.amountVoters;
	}
	
	public void setAmountVoters(int amountVoters) {
		this.amountVoters=amountVoters;
	}


	public int getVotersSameChoice() {
		return votersSameChoice;
	}


	public void setVotersSameChoice(int votersSameChoice) {
		this.votersSameChoice = votersSameChoice;
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
