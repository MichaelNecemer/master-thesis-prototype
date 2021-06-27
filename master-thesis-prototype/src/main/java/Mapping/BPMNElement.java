package Mapping;

import java.util.ArrayList;

//All the classes in the Mapping package are simply to better interact with the API provided by camunda
//BPMNElement is the Base Element for the mapping
//All Elements in Camunda have at least a unique id
//The sequenceFlow is mapped to having predecessors and successors for each BPMNElement

public class BPMNElement {
	

	protected String id;
	protected ArrayList<BPMNElement> predecessors;
	protected ArrayList<BPMNElement> successors;
	protected ArrayList<Label> labels;
	private boolean labelHasBeenSet;
	
	public BPMNElement(String id) {
		this.id = id;
		this.predecessors = new ArrayList<BPMNElement>();
		this.successors = new ArrayList<BPMNElement>();
		this.labels = new ArrayList<Label>();
		this.labelHasBeenSet=false;
	}
	
	public String getId() {
		return this.id;
	}
	
	public void setPredecessors(ArrayList<BPMNElement> predecessors) {
		this.predecessors = predecessors;
	}
	
	public void setSuccessors(ArrayList<BPMNElement> successors) {
		this.successors = successors;
	}
	public void addSuccessor(BPMNElement successor) {
		this.successors.add(successor);
	}
	public void addPredecessor(BPMNElement predecessor) {
		this.predecessors.add(predecessor);
	}
	
	public void setSuccessors(BPMNElement successor) {
		this.successors.add(successor);
	}	
	
	public void setPredecessors(BPMNElement predecessor) {
		this.predecessors.add(predecessor);
	}
	
	
	
	public ArrayList<BPMNElement> getPredecessors() {
		return predecessors;
	}

	public ArrayList<BPMNElement> getSuccessors() {
		return successors;
	}

	public void printPredecessors() {
		if(this instanceof BPMNTask ) {
			System.out.println("Predecessor of "+((BPMNTask)this).getName()+": ");
		} else {
			System.out.println("Predecessor of "+this.getId()+": ");
		}
		
		for(BPMNElement b: this.predecessors) {			
			b.printElement();
		}
	}
	
	public void printSuccessors() {
		if(this instanceof BPMNTask ) {
			System.out.print("Successor of "+((BPMNTask)this).getName()+": ");
		} else {
			System.out.print("Successor of "+this.getId()+": ");
		}
		
		for(BPMNElement b: this.successors) {	
		b.printElement();
		}
	}
	
	public void printElement() {
		System.out.println(this.id);
	}

	public void addLabel(Label label) {
		this.labels.add(label);
	}
	
	
	public void addLabels(ArrayList<Label>labels) {
		this.labels.addAll(labels);
		
	}
	public ArrayList<Label> getLabels(){
		return this.labels;
	}
	
	public void printLabels() {
		for(Label label: this.labels) {
			System.out.println(label.getLabel());
		}
	}
	
	public boolean hasLabel() {
		return !(this.labels.isEmpty());
	}

	public void addLabelFirst(Label label) {
		this.labels.add(0, label);
	}
	
	public void addLabelFirst(ArrayList<Label>labels) {
		this.labels.addAll(0, labels);
	}
	
	public void deleteLastLabel() {
		this.labels.remove(labels.size()-1);
	}

	public boolean getLabelHasBeenSet() {
		return labelHasBeenSet;
	}

	public void setLabelHasBeenSet(boolean labelHasBeenSet) {
		this.labelHasBeenSet = labelHasBeenSet;
	}
	
	
	
	public ArrayList<BPMNElement> getPredecessorsSorted() {
		ArrayList<BPMNElement>predecessorsSorted = new ArrayList<BPMNElement>();
		for(BPMNElement e: this.predecessors) {
			if(e instanceof BPMNGateway) {
				predecessorsSorted.add(0,e);
			} else {
				predecessorsSorted.add(e);
			}
		}
		
		return predecessorsSorted;
	}
	
	public String getLabelsAsString() {
		StringBuilder sb = new StringBuilder();
		for(Label l: this.labels) {
			sb.append(l.getLabel());
		}
		return sb.toString();
	}
	
	
	
	
}
