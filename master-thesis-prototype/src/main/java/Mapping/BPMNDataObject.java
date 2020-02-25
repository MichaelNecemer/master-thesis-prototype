package Mapping;

import java.util.ArrayList;

//In the xml file the data object are mapped via a dataObject Reference and dataInputAssociation or dataOutpuAssociation to the corresponding tasks

public class BPMNDataObject {
	
	private String id;
	private String name;
	private String defaultSphere;
	
	private ArrayList<BPMNElement> readers;
	private ArrayList<BPMNElement> writers;
	
	public BPMNDataObject(String id, String name) {
		this.id = id;
		this.name = name;
		this.readers = new ArrayList<BPMNElement>();
		this.writers = new ArrayList<BPMNElement>();
		this.defaultSphere = "";
	}



	public String getId() {
		return id;
	}



	public void setId(String id) {
		this.id = id;
	}



	public String getName() {
		return name;
	}



	public void setName(String name) {
		this.name = name;
	}
	
	
	public void addReaderToDataObject(BPMNTask e) {
		this.readers.add(e);
	}
	
	public void addWriterToDataObject(BPMNTask e) {
		this.writers.add(e);
	}
	
	public void printReadersOfDataObject() {
		for(BPMNElement bpmnt: this.readers) {
			if(bpmnt instanceof BPMNTask) {
			System.out.println("Readers of DataObject "+this.name+": "+(((BPMNTask)bpmnt).getName()+", "+((BPMNTask)bpmnt).getParticipant().getName()));
			//bpmne.printElement();
			}
		}
	
	}
	
	public void printWritersOfDataObject() {
		for(BPMNElement bpmnt: this.writers) {
			if(bpmnt instanceof BPMNTask) {
			System.out.println("Writers to DataObject "+this.name+": "+((BPMNTask)bpmnt).getName()+", "+((BPMNTask)bpmnt).getParticipant().getName());
			//bpmne.printElement();
			}
		}
	}



	public ArrayList<BPMNElement> getReaders() {
		return readers;
	}



	public void setReaders(ArrayList<BPMNElement> readers) {
		this.readers = readers;
	}



	public ArrayList<BPMNElement> getWriters() {
		return writers;
	}



	public void setWriters(ArrayList<BPMNElement> writers) {
		this.writers = writers;
	}



	public String getDefaultSphere() {
		return defaultSphere;
	}



	public void setDefaultSphere(String defaultSphere) {
		this.defaultSphere = defaultSphere;
	}
	
	public ArrayList<BPMNElement> getOtherWriters(BPMNElement someWriter){
		ArrayList<BPMNElement> otherWriters = new ArrayList<BPMNElement>();
		for(BPMNElement writer: this.writers) {
			if(!writer.equals(someWriter)) {
				otherWriters.add(writer);
			}
			
		}
		return otherWriters;
	}
	

	
}
