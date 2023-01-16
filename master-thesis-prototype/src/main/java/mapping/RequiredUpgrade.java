package mapping;

import java.util.HashMap;
import java.util.LinkedList;

public class RequiredUpgrade {
	

	private static int id;
	private BPMNTask lastWriter;
	private BPMNDataObject dataO;
	private BPMNBusinessRuleTask currentBrt;
	private HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosenVoters;
	private BPMNParticipant currentParticipant;
	private String sphereOfCurrPart;
	private String upgrade;
	private int upgradeId;
	//on how many paths the lastWriter writes the Data
	private double weightingOfLastWriterToWriteDataForBrt;
	private double weightingOfCurrentBrt;
	private double costForUpgrade;
	private double weightedCostForUpgrade;
	
	
	public RequiredUpgrade(BPMNTask lastWriter, BPMNDataObject dataO, BPMNBusinessRuleTask currentBrt, HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosenVoters, BPMNParticipant currentParticipant, String sphereOfCurrPart, String upgrade,double weightingOfLastWriterToWriteDataForBrt,double weightingOfCurrentBrt, double costForUpgrade){
	this.lastWriter=lastWriter;
	this.dataO=dataO;
	this.currentBrt=currentBrt;
	this.alreadyChosenVoters=alreadyChosenVoters;
	this.currentParticipant=currentParticipant;	
	this.sphereOfCurrPart=sphereOfCurrPart;
	this.upgrade=upgrade;
	this.weightingOfLastWriterToWriteDataForBrt=weightingOfLastWriterToWriteDataForBrt;
	this.upgradeId=++id;
	this.costForUpgrade=costForUpgrade;
	this.weightingOfCurrentBrt = weightingOfCurrentBrt;
	this.weightedCostForUpgrade = costForUpgrade*weightingOfCurrentBrt*weightingOfLastWriterToWriteDataForBrt;
	}	
	
	public void printUpgrade() {		
		System.out.println("UPDATE "+this.upgradeId+": "+currentParticipant.getName()+" needs an "+this.upgrade+" update when "+lastWriter.getName()+" ("+lastWriter.getParticipant().getName()+", weighting: "+this.weightingOfLastWriterToWriteDataForBrt+") writes to "+dataO.getName()+" for "+currentBrt.getName() +"(weighting: "+this.weightingOfCurrentBrt+")");
		System.out.println("COST: "+this.weightedCostForUpgrade);
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
		return alreadyChosenVoters;
	}
	public void setAlreadyChosen(HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosen) {
		this.alreadyChosenVoters = alreadyChosen;
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
	
	public double getWeightingOfCurrentBrt() {
		return this.weightingOfCurrentBrt;
	}
	
	public String getSphereOfCurrPart() {
		return this.sphereOfCurrPart;
	}
	
	
}	
	

