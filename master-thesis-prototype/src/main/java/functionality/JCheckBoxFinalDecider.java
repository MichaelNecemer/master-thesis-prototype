package functionality;

import javax.swing.JCheckBox;

import Mapping.BPMNBusinessRuleTask;
import Mapping.BPMNExclusiveGateway;
import Mapping.BPMNParticipant;

public class JCheckBoxFinalDecider extends JCheckBox {
	public BPMNParticipant participant;
	public BPMNExclusiveGateway bpmnEx;
	public BPMNBusinessRuleTask bpmnBrt;
	
	public JCheckBoxFinalDecider(BPMNParticipant participant, BPMNExclusiveGateway bpmnEx, BPMNBusinessRuleTask bpmnBrt) {
		this.participant=participant;
		this.bpmnEx=bpmnEx;
		this.bpmnBrt=bpmnBrt;
	}

}
