package Mapping;

import java.util.ArrayList;
import java.util.HashMap;

//Exclusive Gateways are binary
public class BPMNExclusiveGateway extends BPMNGateway{
	
	
	private int amountVoters;
	private int votersSameChoice;
	private int amountLoops;
	private String sphere;
	private static int exclusiveGtwCount = 0;
	private String[] constraints; 
		
	public BPMNExclusiveGateway(String id, String name, String type) {
		super(id, name, type);
		//by default a gateway needs 2 voters and has no loops
		this.amountVoters=2;	
		this.votersSameChoice=2;
		this.amountLoops=0;
		//by default the sphere is global
		this.sphere="Global";
		//by default there are no constraints 
		
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
	public void setConstraints(String[]constraints) {
		this.constraints=constraints;
	}
	public String[] getConstraints(){
		return this.constraints;
	}
	
}
