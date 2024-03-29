package functionality;

import mapping.BPMNParticipant;

public class ExcludeParticipantConstraint extends Constraint{
	
	private BPMNParticipant participantToExclude;
	
	public ExcludeParticipantConstraint(BPMNParticipant participantToExclude) {
		this.participantToExclude = participantToExclude;
	}
	
	
	public BPMNParticipant getParticipantToExclude() {
		return this.participantToExclude;
	}
	

}
