package processModelGeneratorAndAnnotater;

import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

public class LabelForAnnotater {
	
	// class for generating labels for splits 
	// also parallel branches will be labeled to ease the checking of reader/writer dependencies
	
	
	private FlowNode splitNode;
	private SequenceFlow outgoingFlow; 
	
	public LabelForAnnotater(FlowNode splitNode, SequenceFlow outgoingFlow) {
		this.splitNode=splitNode;
		this.outgoingFlow=outgoingFlow;
	}

	public FlowNode getSplitNode() {
		return splitNode;
	}

	public void setSplitNode(FlowNode splitNode) {
		this.splitNode = splitNode;
	}

	public SequenceFlow getOutgoingFlow() {
		return outgoingFlow;
	}

	public void setOutgoingFlow(SequenceFlow outgoingFlow) {
		this.outgoingFlow = outgoingFlow;
	}

	
	
}
