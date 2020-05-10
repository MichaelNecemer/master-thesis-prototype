package Mapping;

import java.util.ArrayList;

public class BPMNBusinessRuleTask extends BPMNTask {
	
	private ArrayList<BPMNTask> lastWriters;
	private Decision decision;

	public BPMNBusinessRuleTask(String id, String name) {
		super(id, name);
		// TODO Auto-generated constructor stub
		this.lastWriters = new ArrayList<BPMNTask>();
	}

	public ArrayList<BPMNTask> getLastWriterList() {
		return this.lastWriters;
	}
	
	public void setDecision(Decision decision) {
		this.decision=decision;
	}
	
	public Decision getDecision() {
		return this.decision;
	}
}
