package functionality2;
import java.util.HashSet;
import java.util.LinkedList;

import Mapping.BPMNBusinessRuleTask;
import Mapping.BPMNParticipant;

public class AdditionalActors {
	//list of additional actors for a business rule task
	private BPMNBusinessRuleTask currBrt;
	private LinkedList<BPMNParticipant> additionalActors;
		
	public AdditionalActors(BPMNBusinessRuleTask currBrt, LinkedList<BPMNParticipant>additionalActors) {
		this.currBrt = currBrt;
		this.additionalActors = additionalActors;		
	}
	
	public AdditionalActors (BPMNBusinessRuleTask currBrt, HashSet<BPMNParticipant>additionalActors) {
		this.currBrt = currBrt;
		this.additionalActors = new LinkedList<BPMNParticipant>();
		this.additionalActors.addAll(additionalActors);
	}

	public BPMNBusinessRuleTask getCurrBrt() {
		return currBrt;
	}


	public void setCurrBrt(BPMNBusinessRuleTask currBrt) {
		this.currBrt = currBrt;
	}


	public LinkedList<BPMNParticipant> getAdditionalActors() {
		return additionalActors;
	}


	public void setAdditionalActors(LinkedList<BPMNParticipant> additionalActors) {
		this.additionalActors = additionalActors;
	}
	
	

}
