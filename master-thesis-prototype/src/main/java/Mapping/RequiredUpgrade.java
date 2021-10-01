package Mapping;

import java.util.HashMap;
import java.util.LinkedList;

public class RequiredUpgrade {
	

	private static int id;
	private BPMNTask lastWriter;
	private BPMNDataObject dataO;
	private BPMNBusinessRuleTask currentBrt;
	private HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosen;
	private BPMNParticipant currentParticipant;
	private LinkedList<String> spheresOfCurrPart;
	private String upgrade;
	private int upgradeId;
	//on how many paths the lastWriter writes the Data
	private double weightingOfLastWriterToWriteDataForBrt;
	private double costForUpgrade;
	
	
	public RequiredUpgrade(BPMNTask lastWriter, BPMNDataObject dataO, BPMNBusinessRuleTask currentBrt, HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosenVoters, BPMNParticipant currentParticipant, LinkedList<String> spheresOfCurrPart, String upgrade,double weightingOfLastWriterToWriteDataForBrt, double costForUpgrade){
	this.lastWriter=lastWriter;
	this.dataO=dataO;
	this.currentBrt=currentBrt;
	this.alreadyChosen=alreadyChosenVoters;
	this.currentParticipant=currentParticipant;	
	this.spheresOfCurrPart=spheresOfCurrPart;
	this.upgrade=upgrade;
	this.weightingOfLastWriterToWriteDataForBrt=weightingOfLastWriterToWriteDataForBrt;
	this.upgradeId=++id;
	this.costForUpgrade=costForUpgrade;
	}	
	
	public void printUpgrade() {		
		System.out.println("UPDATE "+this.upgradeId+": "+currentParticipant.getName()+" needs an "+this.upgrade+" update when "+lastWriter.getName()+" ("+lastWriter.getParticipant().getName()+") "+"writes to "+dataO.getName()+" for "+currentBrt.getName() +" with weighting "+this.weightingOfLastWriterToWriteDataForBrt);
		if(spheresOfCurrPart.size()==2) {
			System.out.println("Search successfully extended! Search to "+this.currentBrt.getName()+" -> ("+this.spheresOfCurrPart.get(0)+"); Search After "+this.currentBrt.getName()+" -> ("+this.spheresOfCurrPart.get(1)+")!");
		}
		System.out.println("COST: "+this.costForUpgrade);
	}

	public double getWeightingOfLastWriterToWriteDataForBrt() {
		return weightingOfLastWriterToWriteDataForBrt;
	}

	public void setWeightingOfLastWriterToWriteDataForBrt(double weightingOfLastWriterToWriteDataForBrt) {
		this.weightingOfLastWriterToWriteDataForBrt = weightingOfLastWriterToWriteDataForBrt;
	}

	public double getCostForUpdate() {
		return costForUpgrade;
	}

	public void setCostForUpdate(double costForUpdate) {
		this.costForUpgrade = costForUpdate;
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
		return upgrade;
	}
	public void setUpdate(String update) {
		this.upgrade = update;
	}
	public int getUpdateId() {
		return upgradeId;
	}
	public void setUpdateId(int updateId) {
		this.upgradeId = updateId;
	}
	
	
	
	

}
