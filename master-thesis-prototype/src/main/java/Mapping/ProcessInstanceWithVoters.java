package Mapping;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;


public class ProcessInstanceWithVoters {
	// class holds information about the process model with a possible combination
	// of voters for each brt as well as the required s
	// will update the global and static sphere if necessary

	private static int processID;
	private int processInstanceID;
	private HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> votersMap;
	private HashMap<BPMNBusinessRuleTask, LinkedList<RequiredUpgrade>> requiredUpgrades;
	private double costForModelInstance;
	private LinkedList<VoterForXorArc> listOfArcs;
	private LinkedList<BPMNParticipant> globalSphere;
	private HashMap<BPMNDataObject, LinkedList<BPMNParticipant>> staticSphere;
	private HashMap<BPMNDataObject, LinkedList<BPMNElement>> readersOfDataObjects;
	private HashMap<BPMNDataObject, LinkedList<BPMNElement>> writersOfDataObjects;

	public ProcessInstanceWithVoters() {
		this.processInstanceID = ++processID;
		this.votersMap = new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>();
		this.requiredUpgrades = new HashMap<BPMNBusinessRuleTask, LinkedList<RequiredUpgrade>>();
		this.listOfArcs = new LinkedList<VoterForXorArc>();
		this.costForModelInstance = 0;
		this.globalSphere = new LinkedList<BPMNParticipant>();
		this.staticSphere = new HashMap<BPMNDataObject, LinkedList<BPMNParticipant>>();
		this.readersOfDataObjects = new HashMap<BPMNDataObject, LinkedList<BPMNElement>>();
		this.writersOfDataObjects = new HashMap<BPMNDataObject, LinkedList<BPMNElement>>();
	}

	public HashMap<BPMNDataObject, LinkedList<BPMNElement>> getReadersOfDataObjects() {
		return readersOfDataObjects;
	}

	public LinkedList<BPMNParticipant> getReadersForDataObject(BPMNDataObject dataO) {
		LinkedList<BPMNParticipant> readerParticipants = new LinkedList<BPMNParticipant>();
		for (BPMNElement el : this.readersOfDataObjects.get(dataO)) {
			if (el instanceof BPMNTask) {
				BPMNTask task = (BPMNTask) el;
				if (!readerParticipants.contains(task.getParticipant())) {
					readerParticipants.add(task.getParticipant());
				}
			}
		}
		return readerParticipants;

	}
	

	public void setReadersOfDataObjects(HashMap<BPMNDataObject, LinkedList<BPMNElement>> readersOfDataObjects) {
		this.readersOfDataObjects = readersOfDataObjects;
	}

	public HashMap<BPMNDataObject, LinkedList<BPMNElement>> getWritersOfDataObjects() {
		return writersOfDataObjects;
	}

	public void setWritersOfDataObjects(HashMap<BPMNDataObject, LinkedList<BPMNElement>> writersOfDataObjects) {
		this.writersOfDataObjects = writersOfDataObjects;
	}

	public void setStaticSphere(HashMap<BPMNDataObject, LinkedList<BPMNParticipant>> staticSphereToSet) {
		this.staticSphere = staticSphereToSet;
	}

	public HashMap<BPMNDataObject, LinkedList<BPMNParticipant>> getStaticSphere() {
		return this.staticSphere;
	}

	public void setGlobalSphere(LinkedList<BPMNParticipant> globalSphereToSet) {
		this.globalSphere = globalSphereToSet;
	}

	public LinkedList<BPMNParticipant> getGlobalSphere() {
		return this.globalSphere;
	}

	public void updateSpheres(BPMNBusinessRuleTask brtToBeAdded, LinkedList<BPMNElement> globalSphereTasks)
			throws InterruptedException {
		// add the participants that have already been chosen as voters to the static
		// sphere of the dataObjects 
		// check if participants of substituted brts need to be removed from static
		// and/or global sphere
		
		//add participants that have been upgraded from global to static, wd or sd into static sphere
		for(Entry<BPMNBusinessRuleTask,LinkedList<BPMNParticipant>>entr:this.votersMap.entrySet()) {
			for(BPMNDataObject dataO: entr.getKey().getDataObjects()) {
				
			for(BPMNParticipant part: entr.getValue()) {	
				if (Thread.currentThread().isInterrupted()) {
					System.err.println("Interrupted! " + Thread.currentThread().getName());
					throw new InterruptedException();
				}

				BPMNBusinessRuleTask currBrt = entr.getKey();
				if (brtToBeAdded.getLabels().containsAll(currBrt.getLabels()) || brtToBeAdded.getLabels().isEmpty()
						|| currBrt.getLabels().isEmpty()) {
							
					// add the participants that have already been chosen as voters to the static sphere
					// if they are not already part of it
					if (this.staticSphere.get(dataO) != null) {
						if (!this.staticSphere.get(dataO).contains(part)) {
							this.staticSphere.computeIfAbsent(dataO, value -> new LinkedList<BPMNParticipant>())
									.add(part);
						}
					}

					// check whether participants of brts have to be removed from static sphere
					// because brts got substituted with voters already
					LinkedList<BPMNElement> staticSphereElements = dataO.getStaticSphereElements();
					BPMNParticipant partOfCurrBrt = currBrt.getParticipant();
					staticSphereElements.remove(currBrt);
					boolean partStillInStaticSphere = false;
					for (BPMNElement el : staticSphereElements) {
						if (el instanceof BPMNTask) {
							BPMNTask task = (BPMNTask) el;
							if (task.getParticipant().equals(partOfCurrBrt)) {
								// same part found -> part is still in static sphere
								partStillInStaticSphere = true;
								break;
							}

						}
					}
					
					

					if (!partStillInStaticSphere) {
						// remove part from static sphere
						this.staticSphere.get(dataO).remove(partOfCurrBrt);
						// check whether part needs to be removed from global sphere too
						LinkedList<BPMNElement> globalSphereElements = new LinkedList<BPMNElement>();
						globalSphereElements.addAll(globalSphereTasks);
						globalSphereElements.remove(currBrt);
						boolean partStillInGlobalSphere = false;
						for (BPMNElement el : globalSphereElements) {
							if (el instanceof BPMNTask) {
								BPMNTask task = (BPMNTask) el;
								if (task.getParticipant().equals(partOfCurrBrt)) {
									// same part found in global sphere-> part is still in global sphere
									partStillInGlobalSphere = true;
									break;
								}

							}
						}
						if (!partStillInGlobalSphere) {
							this.globalSphere.remove(partOfCurrBrt);
						}

					}
					
					
					

				}

			}
		}
		}
	}

	public void addVoterArcList(List<VoterForXorArc> voters) {
		for (VoterForXorArc arc : voters) {
			if (!listOfArcs.contains(arc)) {
				listOfArcs.add(arc);
				this.getVotersMap().putIfAbsent(arc.getBrt(), arc.getChosenCombinationOfParticipants());
			}
		}
	}

	public void addVoterArc(VoterForXorArc voters) {
		if (!listOfArcs.contains(voters)) {
			listOfArcs.add(voters);
			this.getVotersMap().putIfAbsent(voters.getBrt(), voters.getChosenCombinationOfParticipants());

		}
	}

	public HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> getVotersMap() {
		return votersMap;
	}

	public void setVotersMap(HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> votersMap) {
		this.votersMap = votersMap;
	}

	public double getCostForModelInstance() {
		return costForModelInstance;
	}

	public void setCostForModelInstance(double costForModelInstance) {
		this.costForModelInstance = costForModelInstance;
	}

	public HashMap<BPMNBusinessRuleTask, LinkedList<RequiredUpgrade>> getRequiredUpgrades() {
		return requiredUpgrades;
	}

	public void setRequiredUpgrades(HashMap<BPMNBusinessRuleTask, LinkedList<RequiredUpgrade>> requiredUpgrades) {
		this.requiredUpgrades = requiredUpgrades;
	}

	public void addVotersToBrt(BPMNBusinessRuleTask brt, LinkedList<BPMNParticipant> voters) {
		this.votersMap.putIfAbsent(brt, voters);
	}

	public void printProcessInstance() {
		System.out.println("********************************************");
		System.out.println("ProcessInstanceID: " + this.processInstanceID);
		System.out.println("Cost for ProcessInstance: " + this.costForModelInstance);
		for (VoterForXorArc arc : this.getListOfArcs()) {
			arc.printArc();
			for(Entry<BPMNBusinessRuleTask,LinkedList<RequiredUpgrade>>entry:this.requiredUpgrades.entrySet()) {
				if(entry.getKey().equals(arc.getBrt())) {
					for (RequiredUpgrade upgrade : entry.getValue()) {						
						upgrade.printUpgrade();					
				}
			
			}
		}
		}

	}

	public LinkedList<VoterForXorArc> getListOfArcs() {
		return listOfArcs;
	}

	public void setListOfArcs(LinkedList<VoterForXorArc> listOfArcs) {
		this.listOfArcs = listOfArcs;
	}

	public int getProcessInstanceID() {
		return processInstanceID;
	}

	public void setProcessInstanceID(int processInstanceID) {
		this.processInstanceID = processInstanceID;
	}

	public static int getProcessID() {
		return processID;
	}

	public static void setProcessID(int processID) {
		ProcessInstanceWithVoters.processID = processID;
	}

}
