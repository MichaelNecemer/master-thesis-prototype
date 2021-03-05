package ProcessModelGeneratorAndAnnotater;

import java.util.LinkedList;

import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.ItemAwareElement;
import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

public class LockedBranch {
	//when a reader is found inside a parallelBranch - check for writers
	//may be inside the branch or in front of the split

	//lockedBranch means, that there is a reader/writer of a dataObject inside of it
	//this results in removing readers/writers in the other branch 
	
	
	
	private ParallelGateway pSplit;
	private SequenceFlow sFlowIntoBranchToRemoveReadersAndWriters;
	private FlowNode successorOfBranchToRemoveReadersAndWriters;
	private SequenceFlow sFlowIntoLockedBranch; 
	private FlowNode successorOfLockedBranch;
	private ParallelGateway pJoin;
	
	private ItemAwareElement iae;
	
	
	public LockedBranch(ParallelGateway pSplit, SequenceFlow sFlowIntoLockedBranch, FlowNode successorOfLockedBranch,ItemAwareElement iae,SequenceFlow sFlowIntoBranchToRemoveReadersAndWriters, FlowNode successorOfBranchToRemoveReadersAndWriters, ParallelGateway pJoin) {
		this.pSplit=pSplit;
		this.sFlowIntoLockedBranch=sFlowIntoLockedBranch;
		this.successorOfLockedBranch=successorOfLockedBranch;
		this.iae=iae;
		this.sFlowIntoBranchToRemoveReadersAndWriters=sFlowIntoBranchToRemoveReadersAndWriters;
		this.successorOfBranchToRemoveReadersAndWriters=successorOfBranchToRemoveReadersAndWriters;
		this.pJoin=pJoin;	
	}
	
	public LockedBranch() {
		
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
