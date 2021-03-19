package ProcessModelGeneratorAndAnnotater;

import java.util.LinkedList;

import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.ItemAwareElement;
import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

public class LockedBranch {
	//when a writer is found inside a parallel branch -> generate a new Object of this class
	//this will mark the elements of the other branch 
	//when ProcessModellAnnotater will query a LockedBranch -> readers and writers of the dataObject will be removed
	//also, elements within locked Branches will be excluded from random selection process if a writer is needed on a path
	
	private ParallelGateway pSplit;
	private SequenceFlow sFlowIntoBranchToRemoveReadersAndWriters;
	private FlowNode successorOfBranchToRemoveReadersAndWriters;
	private ParallelGateway pJoin;
	
	
	private ItemAwareElement iae;
	
	
	public LockedBranch(ParallelGateway pSplit, ItemAwareElement iae,SequenceFlow sFlowIntoBranchToRemoveReadersAndWriters, FlowNode successorOfBranchToRemoveReadersAndWriters, ParallelGateway pJoin) {
		this.pSplit=pSplit;
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


	public ItemAwareElement getIae() {
		return iae;
	}


	public void setIae(ItemAwareElement iae) {
		this.iae = iae;
	}

	public SequenceFlow getsFlowIntoBranchToRemoveReadersAndWriters() {
		return sFlowIntoBranchToRemoveReadersAndWriters;
	}

	public void setsFlowIntoBranchToRemoveReadersAndWriters(SequenceFlow sFlowIntoBranchToRemoveReadersAndWriters) {
		this.sFlowIntoBranchToRemoveReadersAndWriters = sFlowIntoBranchToRemoveReadersAndWriters;
	}

	public FlowNode getSuccessorOfBranchToRemoveReadersAndWriters() {
		return successorOfBranchToRemoveReadersAndWriters;
	}

	public void setSuccessorOfBranchToRemoveReadersAndWriters(FlowNode successorOfBranchToRemoveReadersAndWriters) {
		this.successorOfBranchToRemoveReadersAndWriters = successorOfBranchToRemoveReadersAndWriters;
	}

	public ParallelGateway getpJoin() {
		return pJoin;
	}

	public void setpJoin(ParallelGateway pJoin) {
		this.pJoin = pJoin;
	}
	
	
	

}
