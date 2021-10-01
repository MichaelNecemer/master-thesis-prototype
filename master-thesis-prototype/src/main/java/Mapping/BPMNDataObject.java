package Mapping;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

//In the xml file the data object are mapped via a dataObject Reference and dataInputAssociation or dataOutpuAssociation to the corresponding tasks

public class BPMNDataObject {
	
	private String id;
	private String name;
	private String nameId;
	private String nameIdWithoutBrackets;
	private String defaultSphere;
	private String dataObjectReference;
	
	
	private LinkedList<BPMNElement> readers;
	private LinkedList<BPMNElement> writers;
	private LinkedList<BPMNParticipant> staticSphere;
	private LinkedList<BPMNParticipant> readerParticipants;
	private LinkedList<BPMNParticipant> writerParticipants;
	
	public BPMNDataObject(String id, String name, String dataObjectReference) {
		this.id = id;
		this.name = name;
		this.nameId=name.substring(name.indexOf("["), name.indexOf("]")+1);
		this.nameIdWithoutBrackets=name.substring(name.indexOf("[")+1, name.indexOf("]"));
		this.readers = new LinkedList<BPMNElement>();
		this.writers = new LinkedList<BPMNElement>();
		this.defaultSphere = "";
		this.dataObjectReference=dataObjectReference;
		this.staticSphere=new LinkedList<BPMNParticipant>();
		this.readerParticipants=new LinkedList<BPMNParticipant>();
		this.writerParticipants=new LinkedList<BPMNParticipant>();
	}



	public List<BPMNParticipant> getReaderParticipants() {
		return readerParticipants;
	}



	public void setReaderParticipants(LinkedList<BPMNParticipant> readerParticipants) {
		this.readerParticipants = readerParticipants;
	}



	public List<BPMNParticipant> getWriterParticipants() {
		return writerParticipants;
	}



	public void setWriterParticipants(LinkedList<BPMNParticipant> writerParticipants) {
		this.writerParticipants = writerParticipants;
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
		if(!this.readers.contains(e)) {
			this.readers.add(e);
			if(!this.readerParticipants.contains(e.getParticipant())) {
				this.readerParticipants.add(e.getParticipant());
			}
		}
		
	}
	
	public void addWriterToDataObject(BPMNTask e) {
		if(!this.writers.contains(e)) {
		this.writers.add(e);
		if(!this.writerParticipants.contains(e.getParticipant())) {
			this.writerParticipants.add(e.getParticipant());
		}
		}
	}
	
	public void printReadersOfDataObject() {
		for(BPMNElement bpmnt: this.readers) {
			if(bpmnt instanceof BPMNTask) {
			System.out.println("Readers of DataObject "+this.name+": "+(((BPMNTask)bpmnt).getName()+", "+((BPMNTask)bpmnt).getParticipant().getName()));
			}
		}
	
	}
	
	public void printWritersOfDataObject() {
		for(BPMNElement bpmnt: this.writers) {
			if(bpmnt instanceof BPMNTask) {
			System.out.println("Writers to DataObject "+this.name+": "+((BPMNTask)bpmnt).getName()+", "+((BPMNTask)bpmnt).getParticipant().getName());
			}
		}
	}



	public List<BPMNElement> getReaders() {
		return readers;
	}



	public void setReaders(LinkedList<BPMNElement> readers) {
		this.readers = readers;
	}



	public List<BPMNElement> getWriters() {
		return writers;
	}



	public void setWriters(LinkedList<BPMNElement> writers) {
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
	
	public String getNameId() {
		return this.nameId;
	}



	public String getDataObjectReference() {
		return dataObjectReference;
	}



	public void setDataObjectReference(String dataObjectReference) {
		this.dataObjectReference = dataObjectReference;
	}
	public String getNameIdWithoutBrackets() {
		return this.nameIdWithoutBrackets;
	}



	public LinkedList <BPMNParticipant> getStaticSphere() {
		return staticSphere;
	}

	public LinkedList<BPMNElement> getStaticSphereElements(){
		LinkedList<BPMNElement> staticSphere = new LinkedList<BPMNElement>();
		staticSphere.addAll(this.readers);
		staticSphere.addAll(this.writers);
		return staticSphere;
	}



	public void setStaticSphere(LinkedList<BPMNParticipant> staticSphere) {
		this.staticSphere = staticSphere;
	}
	
	public void addParticipantToStaticSphere(BPMNParticipant participant) {
		if(!this.staticSphere.contains(participant)) {
			this.staticSphere.add(participant);
		}
	}
	
	
}
