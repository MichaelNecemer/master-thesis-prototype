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

public class PModelWithAdditionalActors {
	//contains the current allocation of additional actors for business rule tasks
	private LinkedList<AdditionalActors> additionalActors;
	private HashSet<BPMNParticipant> privateSphere;
	private HashMap<BPMNDataObject, HashSet<BPMNParticipant>> staticSphere;
	private HashMap<BPMNDataObject, HashSet<BPMNParticipant>> staticSphereWithAdditionalActors;
	private HashMap<BPMNDataObject, HashSet<BPMNParticipant>> deltaStaticSphere;
	private HashMap<BPMNDataObject, LinkedList<WD_SphereEntry>> wdSphere;
	private double alphaMeasureParameter;
	private double alphaMeasurePenalty;	
	
	public PModelWithAdditionalActors(LinkedList<AdditionalActors> additionalActors, HashSet<BPMNParticipant>privateSphere, HashMap<BPMNDataObject, HashSet<BPMNParticipant>>staticSphere, HashMap<BPMNDataObject, HashSet<BPMNParticipant>> staticSphereWithAdditionalActors) {
		this.additionalActors = additionalActors;
		this.staticSphere = staticSphere;
		this.staticSphereWithAdditionalActors = staticSphereWithAdditionalActors;
		this.deltaStaticSphere = new HashMap<BPMNDataObject, HashSet<BPMNParticipant>>();
		this.computeAlphaMeasure();
	}
	
	public PModelWithAdditionalActors(LinkedList<AdditionalActors>additionalActors) {
		this.additionalActors = additionalActors;
		this.staticSphere = new HashMap<BPMNDataObject, HashSet<BPMNParticipant>>();
		this.staticSphereWithAdditionalActors = new HashMap<BPMNDataObject, HashSet<BPMNParticipant>>();
		this.deltaStaticSphere = new HashMap<BPMNDataObject, HashSet<BPMNParticipant>>();
		this.wdSphere = new HashMap<BPMNDataObject, LinkedList<WD_SphereEntry>>();
	}	
	
	public void computeStaticSphereWithAdditionalActors() {
		// additional actors per brt are readers of connected data objects
		for(AdditionalActors additionalActors: this.additionalActors) {
			BPMNBusinessRuleTask brt = additionalActors.getCurrBrt();
			LinkedList<BPMNParticipant> addActors = additionalActors.getAdditionalActors();
			for(BPMNDataObject dataO: brt.getDataObjects()) {
				Set<BPMNParticipant>staticSphereForDataO = this.staticSphere.get(dataO);
				this.staticSphereWithAdditionalActors.computeIfAbsent(dataO, k->new HashSet<BPMNParticipant>()).addAll(staticSphereForDataO);
				for(BPMNParticipant part: addActors) {
					this.staticSphereWithAdditionalActors.computeIfAbsent(dataO, k->new HashSet<BPMNParticipant>()).add(part);
				}
				
			}
			
		}
	
	}
	
	
	
	
	public HashSet<BPMNParticipant> getPrivateSphere() {
		return privateSphere;
	}

	public void setPrivateSphere(HashSet<BPMNParticipant> privateSphere) {
		this.privateSphere = privateSphere;
	}

	public HashMap<BPMNDataObject, HashSet<BPMNParticipant>> getStaticSphere() {
		return staticSphere;
	}

	public void setStaticSphere(HashMap<BPMNDataObject, HashSet<BPMNParticipant>> staticSphere) {
		this.staticSphere = staticSphere;
	}

	public void computeAlphaMeasure() {
		for(Entry<BPMNDataObject, HashSet<BPMNParticipant>>staticSphereEntry: this.staticSphere.entrySet()) {
			BPMNDataObject currDataObject = staticSphereEntry.getKey();
			HashSet<BPMNParticipant>staticSphereWithAdditionalActorsList = this.staticSphereWithAdditionalActors.get(currDataObject);
			HashSet<BPMNParticipant>deltaStaticSphereList = new HashSet<BPMNParticipant>();
			deltaStaticSphereList.addAll(staticSphereWithAdditionalActorsList);
			deltaStaticSphereList.removeAll(staticSphereEntry.getValue());
			this.deltaStaticSphere.putIfAbsent(currDataObject, deltaStaticSphereList);
		}		
	}

	public double getAlphaMeasurePenalty() {
		return alphaMeasurePenalty;
	}

	public void setAlphaMeasurePenalty(double alphaMeasurePenalty) {
		this.alphaMeasurePenalty = alphaMeasurePenalty;
	}
	
	public LinkedList<AdditionalActors> getAdditionalActorsList(){
		return this.additionalActors;
	}
	
	
	
	public HashMap<BPMNDataObject, LinkedList<WD_SphereEntry>> getWdSphere() {
		return wdSphere;
	}

	public void setWdSphere(HashMap<BPMNDataObject, LinkedList<WD_SphereEntry>> wdSphere) {
		this.wdSphere = wdSphere;
	}

	public void printAlphaMeasure() {
		StringBuilder sb = new StringBuilder();
		sb.append("Delta static sphere: ");
		sb.append(System.lineSeparator());
		for(Entry<BPMNDataObject, HashSet<BPMNParticipant>>deltaStaticSphereEntry: this.deltaStaticSphere.entrySet()) {
			sb.append(deltaStaticSphereEntry.getKey().getName()+": {");
			Iterator<BPMNParticipant>partIter = deltaStaticSphereEntry.getValue().iterator();
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
		sb.append("Penalty: "+this.alphaMeasurePenalty);
		System.out.println(sb.toString());
	}
	
}
