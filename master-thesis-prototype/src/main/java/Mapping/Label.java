package Mapping;

public class Label {
	
	private String label; //e.g. x1=false
	private String outcome; //e.g. false
	private String name; // e.g. x1
	
	public Label(String name, String outcome) {
		this.name = name;
		this.outcome = outcome;
		this.label = name+"="+outcome;
		
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
	
	public void printLabel() {
		System.out.println("Label: "+this.label);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOutcome() {
		return outcome;
	}

	public void setOutcome(String outcome) {
		this.outcome = outcome;
	}
	
	
}