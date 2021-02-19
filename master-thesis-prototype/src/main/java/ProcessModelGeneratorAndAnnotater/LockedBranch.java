package ProcessModelGeneratorAndAnnotater;

import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.ItemAwareElement;
import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

public class LockedBranch {
	//when a reader is found inside a parallelBranch - check for writers
	//may be inside the branch or in front of the split

	//lock the other branch for this specific dataObject 
	// -> when algorithm queries a locked other branch - it will remove readers and writers of that dataObject inside it!
	
	private ParallelGateway pSplit;
	private SequenceFlow sFlowIntoLockedBranch; 
	private FlowNode successorOfLockedBranch;
	private ItemAwareElement iae;
	
	
	public LockedBranch(ParallelGateway pSplit, SequenceFlow sFlowIntoLockedBranch, FlowNode successorOfLockedBranch,ItemAwareElement iae) {
		this.pSplit=pSplit;
		this.sFlowIntoLockedBranch=sFlowIntoLockedBranch;
		this.successorOfLockedBranch=successorOfLockedBranch;
		this.iae=iae;
		
	}


	public ParallelGateway getpSplit() {
		return pSplit;
	}


	public void setpSplit(ParallelGateway pSplit) {
		this.pSplit = pSplit;
	}


	public SequenceFlow getsFlowIntoLockedBranch() {
		return sFlowIntoLockedBranch;
	}


	public void setsFlowIntoLockedBranch(SequenceFlow sFlowIntoLockedBranch) {
		this.sFlowIntoLockedBranch = sFlowIntoLockedBranch;
	}


	public FlowNode getSuccessorOfLockedBranch() {
		return successorOfLockedBranch;
	}


	public void setSuccessorOfLockedBranch(FlowNode successorOfLockedBranch) {
		this.successorOfLockedBranch = successorOfLockedBranch;
	}



	public ItemAwareElement getIae() {
		return iae;
	}


	public void setIae(ItemAwareElement iae) {
		this.iae = iae;
	}
	
	
	

}
