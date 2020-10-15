package Mapping;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RequiredUpdate {
	
	//ArcWithCost contain a list of required updates for the participants that may vote
	
	private static int id;
	private BPMNTask lastWriter;
	private BPMNDataObject dataO;
	private BPMNBusinessRuleTask currentBrt;
	private HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosen;
	private BPMNParticipant currentParticipant;
	private String update;
	private int updateId;
	//on how many paths the lastWriter writes the Data
	private double weightingOfLastWriterToWriteDataForBrt;
	
	
	public RequiredUpdate(BPMNTask lastWriter, BPMNDataObject dataO, BPMNBusinessRuleTask currentBrt, HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosenVoters, BPMNParticipant currentParticipant, String update, double weightingOfLastWriterToWriteDataForBrt){
	this.lastWriter=lastWriter;
	this.dataO=dataO;
	this.currentBrt=currentBrt;
	this.alreadyChosen=alreadyChosenVoters;
	this.currentParticipant=currentParticipant;		
	this.update=update;
	this.weightingOfLastWriterToWriteDataForBrt=weightingOfLastWriterToWriteDataForBrt;
	this.updateId=++id;
	}
	
	public void printUpdate() {
		System.out.println("UPDATE "+this.updateId+": "+currentParticipant.getName()+" needs an "+this.update+" update when "+lastWriter.getName()+" writes to "+dataO.getName()+" for "+currentBrt.getName() +" with weighting "+this.weightingOfLastWriterToWriteDataForBrt);
	}

	public double getWeightingOfLastWriterToWriteDataForBrt() {
		return weightingOfLastWriterToWriteDataForBrt;
	}

	public void setWeightingOfLastWriterToWriteDataForBrt(double weightingOfLastWriterToWriteDataForBrt) {
		this.weightingOfLastWriterToWriteDataForBrt = weightingOfLastWriterToWriteDataForBrt;
	}
	
	
	
	

}
