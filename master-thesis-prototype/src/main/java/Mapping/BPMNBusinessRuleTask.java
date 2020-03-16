package Mapping;

import java.util.ArrayList;

public class BPMNBusinessRuleTask extends BPMNTask {
	
	private ArrayList<BPMNTask> lastWriters;

	public BPMNBusinessRuleTask(String id, String name) {
		super(id, name);
		// TODO Auto-generated constructor stub
		this.lastWriters = new ArrayList<BPMNTask>();
	}

	public ArrayList<BPMNTask> getLastWriterList() {
		return this.lastWriters;
	}
}
