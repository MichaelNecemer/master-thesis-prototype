package Mapping;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RequiredUpgrade {
	
	//ArcWithCost contain a list of required updates for the participants that may vote
	
	private static int id;
	private BPMNTask lastWriter;
	private BPMNDataObject dataO;
	private BPMNBusinessRuleTask currentBrt;
	private HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosen;
	private BPMNParticipant currentParticipant;
	private LinkedList<String> spheresOfCurrPart;
	private String update;
	private int updateId;
	//on how many paths the lastWriter writes the Data
	private double weightingOfLastWriterToWriteDataForBrt;
	private double costForUpdate;
	
	
	public RequiredUpgrade(BPMNTask lastWriter, BPMNDataObject dataO, BPMNBusinessRuleTask currentBrt, HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosenVoters, BPMNParticipant currentParticipant, LinkedList<String> spheresOfCurrPart, String update,double weightingOfLastWriterToWriteDataForBrt, double costForUpdate){
	this.lastWriter=lastWriter;
	this.dataO=dataO;
	this.currentBrt=currentBrt;
	this.alreadyChosen=alreadyChosenVoters;
	this.currentParticipant=currentParticipant;	
	this.spheresOfCurrPart=spheresOfCurrPart;
	this.update=update;
	this.weightingOfLastWriterToWriteDataForBrt=weightingOfLastWriterToWriteDataForBrt;
	this.updateId=++id;
	this.costForUpdate=costForUpdate;
	}	
	
	public void printUpgrade() {		
		System.out.println("UPDATE "+this.updateId+": "+currentParticipant.getName()+" needs an "+this.update+" update when "+lastWriter.getName()+" ("+lastWriter.getParticipant().getName()+") "+"writes to "+dataO.getName()+" for "+currentBrt.getName() +" with weighting "+this.weightingOfLastWriterToWriteDataForBrt);
		if(spheresOfCurrPart.size()==2) {
			System.out.println("Search successfully extended! Search to "+this.currentBrt.getName()+" -> ("+this.spheresOfCurrPart.get(0)+"); Search After "+this.currentBrt.getName()+" -> ("+this.spheresOfCurrPart.get(1)+")!");
		}
		System.out.println("COST: "+this.costForUpdate);
	}

	public double getWeightingOfLastWriterToWriteDataForBrt() {
		return weightingOfLastWriterToWriteDataForBrt;
	}

	public void setWeightingOfLastWriterToWriteDataForBrt(double weightingOfLastWriterToWriteDataForBrt) {
		this.weightingOfLastWriterToWriteDataForBrt = weightingOfLastWriterToWriteDataForBrt;
	}

	public double getCostForUpdate() {
		return costForUpdate;
	}

	public void setCostForUpdate(double costForUpdate) {
		this.costForUpdate = costForUpdate;
	}
	public BPMNParticipant getCurrentParticipant() {
		return currentParticipant;
	}
	public void setCurrentParticipant(BPMNParticipant currentParticipant) {
		this.currentParticipant = currentParticipant;
	}
	public BPMNTask getLastWriter() {
		return lastWriter;
	}
	public void setLastWriter(BPMNTask lastWriter) {
		this.lastWriter = lastWriter;
	}
	public BPMNDataObject getDataO() {
		return dataO;
	}
	public void setDataO(BPMNDataObject dataO) {
		this.dataO = dataO;
	}
	public BPMNBusinessRuleTask getCurrentBrt() {
		return currentBrt;
	}
	public void setCurrentBrt(BPMNBusinessRuleTask currentBrt) {
		this.currentBrt = currentBrt;
	}
	public HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> getAlreadyChosen() {
		return alreadyChosen;
	}
	public void setAlreadyChosen(HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosen) {
		this.alreadyChosen = alreadyChosen;
	}
	public String getUpdate() {
		return update;
	}
	public void setUpdate(String update) {
		this.update = update;
	}
	public int getUpdateId() {
		return updateId;
	}
	public void setUpdateId(int updateId) {
		this.updateId = updateId;
	}
	
	
	
	

}
