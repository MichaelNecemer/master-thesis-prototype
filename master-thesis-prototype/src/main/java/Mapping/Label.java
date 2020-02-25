package Mapping;

public class Label {
	
	private String label;
	private String outcome;
	private String name;
	
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
