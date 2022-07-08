package functionality2;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;

import Mapping.BPMNBusinessRuleTask;
import Mapping.BPMNDataObject;
import Mapping.BPMNParticipant;
import Mapping.BPMNTask;

public class PModelWithAdditionalActors {
	//contains the current allocation of additional actors for business rule tasks
	private LinkedList<AdditionalActors> additionalActors;
	private HashSet<BPMNParticipant> privateSphere;
	private HashMap<BPMNDataObject, Static_SphereEntry> staticSphereEntries;	
	private HashMap<BPMNDataObject, LinkedList<WD_SphereEntry>> wdSphereEntries;
	private HashMap<BPMNDataObject, LinkedList<SD_SphereEntry>> sdSphereEntries;
	private double alphaMeasureWeightingParameter;
	private double betaMeasureWeightingParameter;
	private double gammaMeasureWeightingParameter;
	private double alphaMeasureSum;
	private double betaMeasureSum;
	private double gammaMeasureSum;
	private double sumMeasure;

	
	public PModelWithAdditionalActors(LinkedList<AdditionalActors>additionalActors, LinkedList<Double>weightingParameters) {
		this.additionalActors = additionalActors;
		this.staticSphereEntries = new HashMap<BPMNDataObject, Static_SphereEntry>();
		this.wdSphereEntries = new HashMap<BPMNDataObject, LinkedList<WD_SphereEntry>>();
		this.sdSphereEntries = new HashMap<BPMNDataObject, LinkedList<SD_SphereEntry>>();
		this.alphaMeasureWeightingParameter = weightingParameters.get(0);
		this.betaMeasureWeightingParameter = weightingParameters.get(1);
		this.gammaMeasureWeightingParameter = weightingParameters.get(2);		
		this.alphaMeasureSum = 0;
		this.betaMeasureSum = 0;
		this.gammaMeasureSum = 0;
		this.sumMeasure = 0;
	}	
	

	
	
	public HashSet<BPMNParticipant> getPrivateSphere() {
		return privateSphere;
	}

	public void setPrivateSphere(HashSet<BPMNParticipant> privateSphere) {
		this.privateSphere = privateSphere;
	}

	
	
	
	public LinkedList<AdditionalActors> getAdditionalActorsList(){
		return this.additionalActors;
	}
	
	
	
	public HashMap<BPMNDataObject, Static_SphereEntry> getStaticSphereEntries() {
		return staticSphereEntries;
	}




	public void setStaticSphereEntries(HashMap<BPMNDataObject, Static_SphereEntry> staticSphereEntries) {
		this.staticSphereEntries = staticSphereEntries;
	}




	public HashMap<BPMNDataObject, LinkedList<WD_SphereEntry>> getWdSphere() {
		return wdSphereEntries;
	}

	public void setWdSphere(HashMap<BPMNDataObject, LinkedList<WD_SphereEntry>> wdSphere) {
		this.wdSphereEntries = wdSphere;
	}


	


	public HashMap<BPMNDataObject, LinkedList<WD_SphereEntry>> getWdSphereEntries() {
		return wdSphereEntries;
	}




	public void setWdSphereEntries(HashMap<BPMNDataObject, LinkedList<WD_SphereEntry>> wdSphereEntries) {
		this.wdSphereEntries = wdSphereEntries;
	}




	public HashMap<BPMNDataObject, LinkedList<SD_SphereEntry>> getSdSphereEntries() {
		return sdSphereEntries;
	}




	public void setSdSphereEntries(HashMap<BPMNDataObject, LinkedList<SD_SphereEntry>> sdSphereEntries) {
		this.sdSphereEntries = sdSphereEntries;
	}




	public double getAlphaMeasureSum() {
		return alphaMeasureSum;
	}




	public void setAlphaMeasureSum(double alphaMeasureSum) {
		this.alphaMeasureSum = alphaMeasureSum;
	}




	public double getBetaMeasureSum() {
		return betaMeasureSum;
	}




	public void setBetaMeasureSum(double betaMeasureSum) {
		this.betaMeasureSum = betaMeasureSum;
	}
	
	
	

	public double getGammaMeasureSum() {
		return gammaMeasureSum;
	}




	public void setGammaMeasureSum(double gammaMeasureSum) {
		this.gammaMeasureSum = gammaMeasureSum;
	}




	public void printAdditionalActors() {
		StringBuilder sb = new StringBuilder();
		sb.append("ADDITIONAL ACTORS:" );
		sb.append(System.lineSeparator());	
		for(AdditionalActors addActors: this.additionalActors) {
			sb.append(addActors.getCurrBrt().getName()+": ");
			Iterator<BPMNParticipant> addActorIter = addActors.getAdditionalActors().iterator();
			while(addActorIter.hasNext()) {
				BPMNParticipant addActor = addActorIter.next();
				sb.append(addActor.getName());
				if(addActorIter.hasNext()) {
					sb.append(", ");
				}
				
			}
			sb.append(System.lineSeparator());	
		
		}
		System.out.println(sb.toString());
	}
	
	public void printAlphaMeasure() {
		StringBuilder sb = new StringBuilder();
		sb.append("ALPHA MEASURE:");
		sb.append(System.lineSeparator());	
		for(Entry<BPMNDataObject, Static_SphereEntry> staticSphereEntry: this.staticSphereEntries.entrySet()) {
			HashSet<BPMNParticipant>sEntry = staticSphereEntry.getValue().getStaticSphere();
			HashSet<BPMNParticipant>sEntryAddActors = staticSphereEntry.getValue().getStaticSphereWithAdditionalActors();
			sb.append("Static sphere for "+staticSphereEntry.getKey().getName()+": {");
			Iterator<BPMNParticipant>partIter = sEntry.iterator();
			while(partIter.hasNext()) {
				BPMNParticipant nextPart = partIter.next();
				sb.append(nextPart.getName());
				if(partIter.hasNext()) {
				sb.append(", ");
				}
			}
			sb.append("}");	
			sb.append(System.lineSeparator());	
	
			sb.append("with additional actors:  {");
			Iterator<BPMNParticipant>partIter2 = sEntryAddActors.iterator();
			while(partIter2.hasNext()) {
				BPMNParticipant nextPart = partIter2.next();
				sb.append(nextPart.getName());
				if(partIter2.hasNext()) {
				sb.append(", ");
				}
			}
			
			sb.append("}");	
			sb.append(System.lineSeparator());
		}
		
		sb.append("Delta static sphere: ");
		sb.append(System.lineSeparator());
		for(Entry<BPMNDataObject, Static_SphereEntry> staticSphereEntry: this.staticSphereEntries.entrySet()) {
			HashSet<BPMNParticipant>deltaStaticSphereEntry = staticSphereEntry.getValue().getLambdaStaticSphere();
			sb.append(staticSphereEntry.getKey().getName()+": {");
			Iterator<BPMNParticipant>partIter = deltaStaticSphereEntry.iterator();
			while(partIter.hasNext()) {
				BPMNParticipant nextPart = partIter.next();
				sb.append(nextPart.getName());
				if(partIter.hasNext()) {
				sb.append(", ");
				}
			}
			sb.append("}");	
			sb.append(System.lineSeparator());
		}
		sb.append("Cost for alpha measure without weighting: "+this.alphaMeasureSum);
		sb.append(System.lineSeparator());
		System.out.println(sb.toString());
	}
	
	public void printBetaMeasure() {
		StringBuilder sb = new StringBuilder();
		sb.append("BETA MEASURE:");
		sb.append(System.lineSeparator());	
		for(Entry<BPMNDataObject, LinkedList<WD_SphereEntry>> betaSphereEntry: this.wdSphereEntries.entrySet()) {
			LinkedList<WD_SphereEntry> wdList = betaSphereEntry.getValue();
			for(WD_SphereEntry wdEntry: wdList) {
				BPMNTask origin = wdEntry.getOrigin();
				sb.append("WD sphere for "+betaSphereEntry.getKey().getName() +" with origin "+origin.getName()+": {");
				HashSet<BPMNParticipant>wdEntryParticipants = wdEntry.getWdSphere();
				Iterator<BPMNParticipant>partIter = wdEntryParticipants.iterator();
				while(partIter.hasNext()) {
					BPMNParticipant nextPart = partIter.next();
					sb.append(nextPart.getName());
					if(partIter.hasNext()) {
					sb.append(", ");
					}
				}
				sb.append("}");	
				sb.append(System.lineSeparator());	
				
				sb.append("with additional actors:  {");
				HashSet<BPMNParticipant>wdEntryParticipantsAddActors = wdEntry.getWdSphereWithAdditionalActors();
				Iterator<BPMNParticipant>partIter2 = wdEntryParticipantsAddActors.iterator();
				while(partIter2.hasNext()) {
					BPMNParticipant nextPart = partIter2.next();
					sb.append(nextPart.getName());
					if(partIter2.hasNext()) {
					sb.append(", ");
					}
				}				
				sb.append("}");	
				sb.append(System.lineSeparator());				
				
				sb.append("delta : {");
				HashSet<BPMNParticipant>deltaParticipants = wdEntry.getLambdaWdSphere();
				Iterator<BPMNParticipant>partIter3 = deltaParticipants.iterator();
				while(partIter3.hasNext()) {
					BPMNParticipant nextPart = partIter3.next();
					sb.append(nextPart.getName());
					if(partIter3.hasNext()) {
					sb.append(", ");
					}
				}				
				sb.append("}");	
				sb.append(System.lineSeparator());	
				sb.append("weight(w): "+wdEntry.getWeightingOfOrigin()+", score: "+wdEntry.getScore());
				sb.append(System.lineSeparator());							
			}		
		}		
		sb.append("Cost for beta measure without weighting: "+this.betaMeasureSum);
		sb.append(System.lineSeparator());
		System.out.println(sb.toString());
	}
	
	public void printGammaMeasure() {
		StringBuilder sb = new StringBuilder();
		sb.append("GAMMA MEASURE:");
		sb.append(System.lineSeparator());	
		for(Entry<BPMNDataObject, LinkedList<SD_SphereEntry>> gammaSphereEntry: this.sdSphereEntries.entrySet()) {
			LinkedList<SD_SphereEntry> sdList = gammaSphereEntry.getValue();
			for(SD_SphereEntry sdEntry: sdList) {
				BPMNTask origin = sdEntry.getOrigin();
				sb.append("SD sphere for "+gammaSphereEntry.getKey().getName() +" with origin "+origin.getName()+ "at pos. of "+sdEntry.getCurrBrt().getName()+" : {");
				HashSet<BPMNParticipant>sdEntryParticipants = sdEntry.getSdSphereWithAdditionalActors();
				Iterator<BPMNParticipant>partIter = sdEntryParticipants.iterator();
				while(partIter.hasNext()) {
					BPMNParticipant nextPart = partIter.next();
					sb.append(nextPart.getName());
					if(partIter.hasNext()) {
					sb.append(", ");
					}
				}
				sb.append("}");	
				sb.append(System.lineSeparator());	
				
				sb.append("without additional actors of "+sdEntry.getCurrBrt().getName()+" :  {");
				HashSet<BPMNParticipant>sdEntryParticipantsAddActors = sdEntry.getSdSphereWithoutAddActorOfCurrBrt();
				Iterator<BPMNParticipant>partIter2 = sdEntryParticipantsAddActors.iterator();
				while(partIter2.hasNext()) {
					BPMNParticipant nextPart = partIter2.next();
					sb.append(nextPart.getName());
					if(partIter2.hasNext()) {
					sb.append(", ");
					}
				}				
				sb.append("}");	
				sb.append(System.lineSeparator());				
				
				sb.append("delta : {");
				HashSet<BPMNParticipant>deltaParticipants = sdEntry.getLambdaSdSphere();
				Iterator<BPMNParticipant>partIter3 = deltaParticipants.iterator();
				while(partIter3.hasNext()) {
					BPMNParticipant nextPart = partIter3.next();
					sb.append(nextPart.getName());
					if(partIter3.hasNext()) {
					sb.append(", ");
					}
				}				
				sb.append("}");	
				sb.append(System.lineSeparator());	
				sb.append("weight(w): "+sdEntry.getWeightingOfOrigin()+", weight(w,r,d): "+sdEntry.getWeightingOfOriginForCurrBrt()+", score: "+sdEntry.getScore());
				sb.append(System.lineSeparator());							
			}		
		}
		
		sb.append("Cost for gamma measure without weighting: "+this.gammaMeasureSum);
		sb.append(System.lineSeparator());
		System.out.println(sb.toString());
	}
	
	public void printMeasure() {
		this.printAdditionalActors();
		this.printAlphaMeasure();
		this.printBetaMeasure();
		this.printGammaMeasure();		
		System.out.println("Measure: "+this.getSumMeasure());
		System.out.println("***************************************************");
	}
	
	public double getWeightedCostAlphaMeasure() {
		return this.alphaMeasureSum * this.alphaMeasureWeightingParameter;
	}
	
	public double getWeightedCostBetaMeasure() {
		return this.betaMeasureSum * this.betaMeasureWeightingParameter;
	}
	
	public double getWeightedCostGammaMeasure() {
		return this.gammaMeasureSum * this.gammaMeasureWeightingParameter;
	}
	

	public double getSumMeasure() {
		return sumMeasure;
	}




	public void setSumMeasure(double sumMeasure) {
		this.sumMeasure = sumMeasure;
	}
	
	

}
