package functionality;

import mapping.BPMNParticipant;

public class MandatoryParticipantConstraint extends Constraint {
private BPMNParticipant mandatoryParticipant;

public MandatoryParticipantConstraint(BPMNParticipant mandatoryParticipant) {
	this.mandatoryParticipant=mandatoryParticipant;
}

public BPMNParticipant getMandatoryParticipant() {
	return mandatoryParticipant;
}



}
