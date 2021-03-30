package ProcessModelGeneratorAndAnnotater;

import java.util.LinkedList;

import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.ItemAwareElement;
import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

public class LockedBranch {
	//when a writer is found inside a parallel branch -> readers and writers inside the other branch for a specific dataObject will be removed
	//after that, the branch will be marked as locked
	//elements within locked Branches will be excluded from random selection process if a writer is needed on a path
	
	private ParallelGateway pSplit; 
	private ParallelGateway pJoin;
	private LinkedList<FlowNode>path;
	
	
	private ItemAwareElement iae;
	
	
	public LockedBranch(ParallelGateway pSplit, ItemAwareElement iae, ParallelGateway pJoin, LinkedList<FlowNode>path) {
		this.pSplit=pSplit;
		this.iae=iae;
		this.pJoin=pJoin;	
		this.path=path;
	}
	
	public LockedBranch() {
		
	}
	
	


	public LinkedList<FlowNode> getPath() {
		return path;
	}

	public void setPath(LinkedList<FlowNode> path) {
		this.path = path;
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

	

	public ParallelGateway getpJoin() {
		return pJoin;
	}

	public void setpJoin(ParallelGateway pJoin) {
		this.pJoin = pJoin;
	}
	
	
	

}
