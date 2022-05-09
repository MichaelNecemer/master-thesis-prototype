package functionality;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Association;
import org.camunda.bpm.model.bpmn.instance.BusinessRuleTask;
import org.camunda.bpm.model.bpmn.instance.DataInputAssociation;
import org.camunda.bpm.model.bpmn.instance.DataObject;
import org.camunda.bpm.model.bpmn.instance.DataObjectReference;
import org.camunda.bpm.model.bpmn.instance.DataOutputAssociation;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Lane;
import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
import org.camunda.bpm.model.bpmn.instance.SendTask;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.camunda.bpm.model.bpmn.instance.TextAnnotation;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;

import Mapping.ArcWithCost;
import Mapping.BPMNBusinessRuleTask;
import Mapping.BPMNDataObject;
import Mapping.BPMNElement;
import Mapping.BPMNEndEvent;
import Mapping.BPMNExclusiveGateway;
import Mapping.BPMNGateway;
import Mapping.BPMNParallelGateway;
import Mapping.BPMNParticipant;
import Mapping.BPMNStartEvent;
import Mapping.BPMNTask;
import Mapping.Combination;
import Mapping.DecisionEvaluation;
import Mapping.InfixToPostfix;
import Mapping.Label;
import Mapping.ProcessInstanceWithVoters;
import Mapping.RequiredUpgrade;
import Mapping.VoterForXorArc;

//Class that uses the camunda model API to interact with the process model directly without parsing the XML first to e.g. DOM Object
//Note that only processes with exactly 1 Start Event are possible
public class API implements Callable<HashMap<String, LinkedList<ProcessInstanceWithVoters>>> {

	static int value = 0;
	private Collection<StartEvent> startEvent;
	private Collection<EndEvent> endEvent;
	private File process;
	private BpmnModelInstance modelInstance;
	private String amountPossibleCombinationsOfParticipants;
	private boolean modelWithLanes;
	private HashMap<String, Double> executionTimeMap;
	private BPMNParticipant troubleShooter;
	private String algorithmToPerform;

	private BPMNStartEvent bpmnStart;
	private BPMNEndEvent bpmnEnd;
	private ArrayList<BPMNDataObject> dataObjects = new ArrayList<BPMNDataObject>();
	private ArrayList<BPMNElement> processElements = new ArrayList<BPMNElement>();
	private LinkedList<BPMNParticipant> publicSphere = new LinkedList<BPMNParticipant>();
	private LinkedList<BPMNParticipant> globalSphere = new LinkedList<BPMNParticipant>();
	private LinkedList<BPMNElement> globalSphereTasks;
	private ArrayList<BPMNBusinessRuleTask> businessRuleTaskList = new ArrayList<BPMNBusinessRuleTask>();
	private ArrayList<Label> labelList = new ArrayList<Label>();
	private LinkedList<LinkedList<FlowNode>> pathsThroughProcess = new LinkedList<LinkedList<FlowNode>>();
	private double costForLiftingFromPublicToGlobal;
	private double costForLiftingFromGlobalToStatic;
	private double costForLiftingFromStaticToWeakDynamic;
	private double costForLiftingFromWeakDynamicToStrongDynamic;
	private LinkedList<ProcessInstanceWithVoters> processInstancesWithVoters;
	private HashMap<DataObjectReference, LinkedList<FlowNode>> readersMap;
	private HashMap<DataObjectReference, LinkedList<FlowNode>> writersMap;
	private String deciderOrVerifier;
	private LinkedList<MandatoryParticipantConstraint> mandatoryParticipantConstraints;
	private LinkedList<ExcludeParticipantConstraint> excludeParticipantConstraints;

	public API(String pathToFile, ArrayList<Double> cost) throws Exception {
		if (cost.size() != 4) {
			throw new Exception("Not exactly 4 cost parameters in the list!");
		}
		synchronized (this) {
			this.process = new File(pathToFile);
			this.modelInstance = Bpmn.readModelFromFile(process);
			System.out.println("API for: " + process.getName());
			boolean correctModel = true;
			try {
				CommonFunctionality.isCorrectModel(this.modelInstance);
			} catch (Exception e) {
				correctModel = false;
				System.out.println("Model correct: " + correctModel);
				throw new Exception(e.getMessage());
			}
			System.out.println("Model correct: " + correctModel);
			this.startEvent = modelInstance.getModelElementsByType(StartEvent.class);
			this.endEvent = modelInstance.getModelElementsByType(EndEvent.class);
			this.globalSphereTasks = new LinkedList<BPMNElement>();
			this.costForLiftingFromPublicToGlobal = cost.get(0);
			this.costForLiftingFromGlobalToStatic = cost.get(1);
			this.costForLiftingFromStaticToWeakDynamic = cost.get(2);
			this.costForLiftingFromWeakDynamicToStrongDynamic = cost.get(3);
			this.algorithmToPerform = "bruteForce";
			this.executionTimeMap = new HashMap<String, Double>();
			this.amountPossibleCombinationsOfParticipants = "0";
			this.mandatoryParticipantConstraints = new LinkedList<MandatoryParticipantConstraint>();
			this.excludeParticipantConstraints = new LinkedList<ExcludeParticipantConstraint>();

			this.readersMap = CommonFunctionality.getReadersForDataObjects(this.modelInstance);
			this.writersMap = CommonFunctionality.getWritersForDataObjects(this.modelInstance);

			this.mapAndCompute();
			this.setDependentBrts();
			this.pathsThroughProcess = CommonFunctionality.getAllPathsBetweenNodes(this.modelInstance,
					modelInstance.getModelElementsByType(StartEvent.class).iterator().next().getId(),
					modelInstance.getModelElementsByType(EndEvent.class).iterator().next().getId());

		}
	}

	private void mapAndCompute() throws Exception {
		try {
			// check if it is a model with lanes
			if (modelInstance.getModelElementsByType(Lane.class).isEmpty()) {
				this.modelWithLanes = false;
			} else {
				this.modelWithLanes = true;
			}

			// maps all the Camunda FlowNodes to BPMNElements
			this.mapProcessElements();
			// maps the successors and predecessors of the elements directly to the
			// elements, sets the labels
			this.mapSuccessorsAndPredecessors(startEvent.iterator().next(), endEvent.iterator().next(),
					new LinkedList<SequenceFlow>(), new ArrayList<Label>());

			if (this.modelWithLanes) {
				this.storeLanePerTask();
			} else {
				this.addParticipantToTask();
			}

			this.mapDataObjects();

			this.computeGlobalSphere();
			this.mapAssociationsAndAnotations();

			this.mapDefaultTroubleShooter();

			// set last writer lists for the brts
			for (BPMNBusinessRuleTask brt : this.businessRuleTaskList) {
				this.mapDecisions(brt);
				for (BPMNDataObject dataO : brt.getDataObjects()) {
					ArrayList<BPMNTask> lastWritersForDataO = this.getLastWriterListForDataObject(brt, dataO);
					brt.getLastWriterList().putIfAbsent(dataO, lastWritersForDataO);
				}
			}
		} catch (Exception e) {
			throw new Exception("Error in mapAndCompute() method!" + e.getCause());
		}
	}

	private void setDependentBrts() {
		for (int i = 0; i < this.businessRuleTaskList.size(); i++) {
			int j = businessRuleTaskList.size() - 1;
			BPMNBusinessRuleTask brt = this.businessRuleTaskList.get(i);
			while (i < j) {
				BPMNBusinessRuleTask brt2 = this.businessRuleTaskList.get(j);
				for (Entry<BPMNDataObject, ArrayList<BPMNTask>> entry : brt.getLastWriterList().entrySet()) {
					for (BPMNTask lastWriter : entry.getValue()) {
						for (Entry<BPMNDataObject, ArrayList<BPMNTask>> entry2 : brt2.getLastWriterList().entrySet()) {
							if (entry2.getKey().equals(entry.getKey())) {
								// check if both brts have (partially) the same lastwriters
								if (entry2.getValue().contains(lastWriter)) {
									if ((!brt2.getPotentiallyDependentBrts().contains(brt))) {
										brt2.getPotentiallyDependentBrts().add(brt);
									}
								}
							}
						}
					}

				}

				j--;
			}

		}

	}

	public BPMNExclusiveGateway getLastXorJoinInProcess() {

		LinkedList<BPMNElement> stack = new LinkedList<BPMNElement>();
		stack.add(this.bpmnEnd);

		while (!stack.isEmpty()) {
			BPMNElement currentElement = stack.poll();
			if (currentElement instanceof BPMNExclusiveGateway) {
				BPMNExclusiveGateway lastXor = (BPMNExclusiveGateway) currentElement;
				if (lastXor.getType().contentEquals("join")) {
					return lastXor;
				}
			}

			for (BPMNElement predecessor : currentElement.getPredecessors()) {
				stack.push(predecessor);
			}

		}

		return null;

	}

	public void setRequiredUpgradeForArc(VoterForXorArc arcToBeAdded, ProcessInstanceWithVoters currInst,
			LinkedList<LinkedList<BPMNElement>> paths) throws NullPointerException, InterruptedException, Exception {
		// get all the participants of the current process instance and check which
		// updates would be necessary for arcToBeAdded
		HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosenVoters = currInst.getVotersMap();
		BPMNBusinessRuleTask brtToBeAdded = arcToBeAdded.getBrt();
		if (currInst.getRequiredUpgrades().values().isEmpty()) {
			currInst.setGlobalSphere(this.globalSphere);
			HashMap<BPMNDataObject, LinkedList<BPMNParticipant>> staticSphereToSet = new HashMap<BPMNDataObject, LinkedList<BPMNParticipant>>();
			HashMap<BPMNDataObject, LinkedList<BPMNElement>> readersPerDataObject = new HashMap<BPMNDataObject, LinkedList<BPMNElement>>();
			HashMap<BPMNDataObject, LinkedList<BPMNElement>> writersPerDataObject = new HashMap<BPMNDataObject, LinkedList<BPMNElement>>();

			for (BPMNDataObject dataO : this.dataObjects) {
				staticSphereToSet.putIfAbsent(dataO, dataO.getStaticSphere());
				readersPerDataObject.putIfAbsent(dataO, (LinkedList<BPMNElement>) dataO.getReaders());
				writersPerDataObject.putIfAbsent(dataO, (LinkedList<BPMNElement>) dataO.getWriters());
			}
			currInst.setStaticSphere(staticSphereToSet);
			currInst.setWritersOfDataObjects(writersPerDataObject);
			currInst.setReadersOfDataObjects(readersPerDataObject);
		}
		currInst.updateSpheres(brtToBeAdded, this.globalSphereTasks);

		HashMap<BPMNDataObject, ArrayList<BPMNTask>> lastWriters = brtToBeAdded.getLastWriterList();
		for (Entry<BPMNDataObject, ArrayList<BPMNTask>> entry : lastWriters.entrySet()) {
			for (BPMNTask lastWriter : entry.getValue()) {
				BPMNDataObject dataO = entry.getKey();
				String requiredSphere = lastWriter.getSphereAnnotation().get(dataO);

				HashMap<BPMNParticipant, String> sphereMap = new HashMap<BPMNParticipant, String>();
				// set the WD and SD sphere and consider the preceding already chosen voters
				// substitute the respective brts participant with the voters
				ArrayList<BPMNParticipant> wdList = new ArrayList<BPMNParticipant>();
				ArrayList<BPMNParticipant> sdList = new ArrayList<BPMNParticipant>();

				for (BPMNParticipant readerParticipant : arcToBeAdded.getChosenCombinationOfParticipants()) {
					LinkedList<LinkedList<BPMNElement>> pathsBetweenWriterTaskAndEnd = this
							.getPathsWithMappedNodesFromCamundaNodes(CommonFunctionality.getAllPathsBetweenNodes(
									this.modelInstance, lastWriter.getId(), this.bpmnEnd.getId()));

					String sphereForReader = this.getSphereForParticipantOnPathsWithAlreadyChosenVoters(currInst,
							brtToBeAdded, lastWriter, dataO, readerParticipant, alreadyChosenVoters,
							pathsBetweenWriterTaskAndEnd);

					if (sphereForReader.contentEquals("Strong-Dynamic")) {
						if (!sdList.contains(readerParticipant)) {
							sdList.add(readerParticipant);
						}
					} else if (sphereForReader.contentEquals("Weak-Dynamic")) {
						if (!wdList.contains(readerParticipant)) {
							wdList.add(readerParticipant);
						}
					}

					sphereMap.putIfAbsent(readerParticipant, sphereForReader);
				}

				if (lastWriter.getWeakDynamicHashMap().get(dataO) == null) {
					lastWriter.getWeakDynamicHashMap().put(dataO, wdList);
				} else {
					lastWriter.getWeakDynamicHashMap().remove(dataO);
					lastWriter.getWeakDynamicHashMap().put(dataO, wdList);
				}

				if (lastWriter.getStrongDynamicHashMap().get(dataO) == null) {
					lastWriter.getStrongDynamicHashMap().put(dataO, sdList);
				} else {
					lastWriter.getStrongDynamicHashMap().remove(dataO);
					lastWriter.getStrongDynamicHashMap().put(dataO, sdList);
				}

				LinkedList<BPMNParticipant> chosenPartForArc = arcToBeAdded.getChosenCombinationOfParticipants();

				for (BPMNParticipant reader : chosenPartForArc) {

					String upgrade = "";
					double cost = 0;
					
					if (requiredSphere.contentEquals("Strong-Dynamic")) {

						if (lastWriter.getStrongDynamicHashMap().containsKey(dataO)) {
							if (lastWriter.getStrongDynamicHashMap().get(dataO).contains(reader)) {							
								// no update needed
							} else if (lastWriter.getWeakDynamicHashMap().get(dataO).contains(reader)) {
								// update from WD to SD
								upgrade = "wdToSD";
								cost = this.costForLiftingFromWeakDynamicToStrongDynamic;

							} else if (currInst.getStaticSphere().get(dataO).contains(reader)) {
								// update from static to SD
								upgrade = "staticToSD";
								cost = this.costForLiftingFromStaticToWeakDynamic
										+ this.costForLiftingFromWeakDynamicToStrongDynamic;

							} else if (currInst.getGlobalSphere().contains(reader)) {
								// update from global to SD
								upgrade = "globalToSD";
								cost = this.costForLiftingFromGlobalToStatic
										+ this.costForLiftingFromStaticToWeakDynamic
										+ this.costForLiftingFromWeakDynamicToStrongDynamic;
							} else {
								// update from public to SD
								upgrade = "publicToSD";
								cost = this.costForLiftingFromPublicToGlobal + this.costForLiftingFromGlobalToStatic
										+ this.costForLiftingFromStaticToWeakDynamic
										+ this.costForLiftingFromWeakDynamicToStrongDynamic;
							}

						}
					} else if (requiredSphere.contentEquals("Weak-Dynamic")) {
						if (lastWriter.getWeakDynamicHashMap().containsKey(dataO)) {

							if (lastWriter.getStrongDynamicHashMap().get(dataO).contains(reader)
									|| lastWriter.getWeakDynamicHashMap().get(dataO).contains(reader)) {
								// no update needed
							} else if (currInst.getStaticSphere().get(dataO).contains(reader)) {
								// update from static to WD
								upgrade = "staticToWD";
								cost = this.costForLiftingFromStaticToWeakDynamic;

							} else if (currInst.getGlobalSphere().contains(reader)) {
								// update from global to WD
								upgrade = "globalToWD";
								cost = this.costForLiftingFromGlobalToStatic
										+ this.costForLiftingFromStaticToWeakDynamic;
							} else {
								// update from public to WD
								upgrade = "publicToWD";
								cost = this.costForLiftingFromPublicToGlobal + this.costForLiftingFromGlobalToStatic
										+ this.costForLiftingFromStaticToWeakDynamic;
							}

						}
					} else if (requiredSphere.contentEquals("Static")) {
						if (lastWriter.getStrongDynamicHashMap().get(dataO).contains(reader)
								|| lastWriter.getWeakDynamicHashMap().get(dataO).contains(reader)
								|| currInst.getStaticSphere().get(dataO).contains(reader)) {
							// no update needed

						} else if (currInst.getGlobalSphere().contains(reader)) {
							upgrade = "globalToStatic";
							cost = this.costForLiftingFromGlobalToStatic;
						} else {
							upgrade = "publicToStatic";
							cost = this.costForLiftingFromPublicToGlobal + this.costForLiftingFromGlobalToStatic;
						}

					} else if (requiredSphere.contentEquals("Global")) {
						if (lastWriter.getStrongDynamicHashMap().get(dataO).contains(reader)
								|| lastWriter.getWeakDynamicHashMap().get(dataO).contains(reader)
								|| currInst.getStaticSphere().get(dataO).contains(reader)
								|| currInst.getGlobalSphere().contains(reader)) {
							// no update needed
						} else {
							// update from public to global
							upgrade = "publicToGlobal";
							cost = this.costForLiftingFromPublicToGlobal;
						}
					}

					if (upgrade != "") {
						String sphere = sphereMap.get(reader);
						double weighting = this.calculateWeightingForLastWriter(lastWriter, this.bpmnStart, dataO,
								brtToBeAdded);
						double weightingOfCurrentBrt = 1;
						
						if (!brtToBeAdded.getLabels().isEmpty()) {
							// since process is block structured -> there are 2 paths for each xor-split
							weightingOfCurrentBrt = weightingOfCurrentBrt / (brtToBeAdded.getLabels().size() * 2);
						}
						
						RequiredUpgrade reqUpgrade = new RequiredUpgrade(lastWriter, dataO, brtToBeAdded,
								alreadyChosenVoters, reader, sphere, upgrade, weighting, weightingOfCurrentBrt, cost);
						
						currInst.getRequiredUpgrades()
								.computeIfAbsent(brtToBeAdded, k -> new LinkedList<RequiredUpgrade>()).add(reqUpgrade);

						double currInstCost = currInst.getCostForModelInstance();
						// cost will be multiplied with the weighting of the last writer and the
						// weighting of the brt			
						double costToAdd = cost * reqUpgrade.getWeightingOfLastWriterToWriteDataForBrt()
								* reqUpgrade.getWeightingOfCurrentBrt();
						currInstCost += costToAdd;
						currInst.setCostForModelInstance(currInstCost);
					}

				}

				if (Thread.currentThread().isInterrupted()) {
					System.err.println("Interrupted! " + Thread.currentThread().getName());
					throw new InterruptedException();
				}

			}

		}

	}

	public synchronized LinkedList<ProcessInstanceWithVoters> localMinimumAlgorithmWithLimit(
			int upperBoundSolutionsPerIteration) throws NullPointerException, InterruptedException, Exception {
		// at each businessRuleTask in front of a xor-split:
		// generate all possible combinations of voters - calculate cost and only take
		// cheapest one(s)
		
		if (!this.executionTimeMap.isEmpty()) {
			throw new Exception("Make a deep copy of API first or create a new API object since there has already run an algorithm on this object!");
		}

		System.out.println("Local Minimum Algorithm with Limit " + upperBoundSolutionsPerIteration
				+ " generating all cheapest process instances: ");
		long startTime = System.nanoTime();
		LinkedList<ProcessInstanceWithVoters> cheapestCombinationsWithLimit = new LinkedList<ProcessInstanceWithVoters>();
		CommonFunctionality.getAllPathsForCamundaElementsBuildArcsAndGetVoters(this, true,
				upperBoundSolutionsPerIteration, new LinkedList<BPMNBusinessRuleTask>(), cheapestCombinationsWithLimit,
				modelInstance.getModelElementById(startEvent.iterator().next().getId()),
				modelInstance.getModelElementById(endEvent.iterator().next().getId()), new LinkedList<FlowNode>(),
				new LinkedList<FlowNode>(), new LinkedList<FlowNode>(), new LinkedList<LinkedList<FlowNode>>(),
				new LinkedList<LinkedList<FlowNode>>(),
				modelInstance.getModelElementById(endEvent.iterator().next().getId()));
		long stopTime = System.nanoTime();
		long executionTime = stopTime - startTime;
		double executionTimelocalMinAlgorithmWithLimitInSeconds = (double) executionTime / 1000000000;
		String key = "localMinWithBound" + upperBoundSolutionsPerIteration;
		this.executionTimeMap.put(key, executionTimelocalMinAlgorithmWithLimitInSeconds);

		return cheapestCombinationsWithLimit;

	}

	public synchronized LinkedList<ProcessInstanceWithVoters> localMinimumAlgorithm()
			throws NullPointerException, InterruptedException, Exception {
		// at each businessRuleTask in front of a xor-split:
		// generate all possible combinations of voters - calculate cost and only take
		// cheapest one(s)

		if (!this.executionTimeMap.isEmpty()) {
			throw new Exception("Make a deep copy of API first or create a new API object since there has already run an algorithm on this object!");
		}
		System.out.println("Local Minimum Algorithm generating all cheapest process instances: ");
		long startTime = System.nanoTime();
		LinkedList<ProcessInstanceWithVoters> cheapestCombinations = new LinkedList<ProcessInstanceWithVoters>();
		CommonFunctionality.getAllPathsForCamundaElementsBuildArcsAndGetVoters(this, true, 0,
				new LinkedList<BPMNBusinessRuleTask>(), cheapestCombinations,
				modelInstance.getModelElementById(startEvent.iterator().next().getId()),
				modelInstance.getModelElementById(endEvent.iterator().next().getId()), new LinkedList<FlowNode>(),
				new LinkedList<FlowNode>(), new LinkedList<FlowNode>(), new LinkedList<LinkedList<FlowNode>>(),
				new LinkedList<LinkedList<FlowNode>>(),
				modelInstance.getModelElementById(endEvent.iterator().next().getId()));
		long stopTime = System.nanoTime();
		long executionTime = stopTime - startTime;
		double executionTimeLocalMinAlgorithmInSeconds = (double) executionTime / 1000000000;
		this.executionTimeMap.put("localMin", executionTimeLocalMinAlgorithmInSeconds);

		return cheapestCombinations;

	}

	public synchronized LinkedList<ProcessInstanceWithVoters> bruteForceAlgorithm()
			throws NullPointerException, InterruptedException, Exception {
		// generate all possible combinations of voters for all brts of the process
		// calculate the cost for each one and return all of them
		if (!this.executionTimeMap.isEmpty()) {
			throw new Exception("Make a deep copy of API first or create a new API object since there has already run an algorithm on this object!");
		}
		System.out.println("Brute Force generating all possible process instances: ");
		long startTime = System.nanoTime();
		LinkedList<ProcessInstanceWithVoters> allCombinations = new LinkedList<ProcessInstanceWithVoters>();
		CommonFunctionality.getAllPathsForCamundaElementsBuildArcsAndGetVoters(this, false, 0,
				new LinkedList<BPMNBusinessRuleTask>(), allCombinations,
				modelInstance.getModelElementById(startEvent.iterator().next().getId()),
				modelInstance.getModelElementById(endEvent.iterator().next().getId()), new LinkedList<FlowNode>(),
				new LinkedList<FlowNode>(), new LinkedList<FlowNode>(), new LinkedList<LinkedList<FlowNode>>(),
				new LinkedList<LinkedList<FlowNode>>(),
				modelInstance.getModelElementById(endEvent.iterator().next().getId()));
		long stopTime = System.nanoTime();
		long executionTime = stopTime - startTime;
		double executionTimeBruteForceAlgorithmInSeconds = (double) executionTime / 1000000000;
		this.executionTimeMap.put("bruteForce", executionTimeBruteForceAlgorithmInSeconds);

		return allCombinations;

	}

	private double calculateWeightingForLastWriter(BPMNTask currentLastWriter, BPMNElement start, BPMNDataObject dataO,
			BPMNBusinessRuleTask brt) throws NullPointerException, Exception {

		double foundCurrentLastWriterCount = 0;
		LinkedList<LinkedList<FlowNode>> paths = CommonFunctionality.getAllPathsBetweenNodes(this.modelInstance,
				start.getId(), brt.getId());

		for (LinkedList<FlowNode> pathList : paths) {

			for (int i = pathList.size() - 1; i > 0; i--) {
				if (pathList.get(i).getId().equals(currentLastWriter.getId())) {
					// go backwards through the list of paths between start and brt
					// if lastWriter is found in the list increase the count
					foundCurrentLastWriterCount++;
					i = 0;
				}

				if (pathList.get(i) instanceof Task) {
					Task currTask = (Task) pathList.get(i);
					for (BPMNElement writer : dataO.getWriters()) {
						if (writer.getId().contentEquals(currTask.getId())
								&& (!currTask.getId().contentEquals(currentLastWriter.getId()))) {
							i = 0;
						}
					}

				}
			}

		}

		double amount = (double) Math.round(foundCurrentLastWriterCount / paths.size() * 100) / 100;

		return amount;

	}

	private boolean arcAlreadyGeneratedBool(BPMNBusinessRuleTask brt, VoterForXorArc arc) {
		for (VoterForXorArc a : brt.getVoterArcs()) {
			if (a.getBrt().equals(arc.getBrt())) {
				if (a.getXorSplit().equals(arc.getXorSplit())) {

					if (a.getChosenCombinationOfParticipants().equals(arc.getChosenCombinationOfParticipants())) {
						return true;

					}
				}
			}

		}
		return false;
	}

	public boolean readerIsOnPath(BPMNParticipant reader, BPMNTask lastWriter, BPMNDataObject dataO,
			LinkedList<BPMNElement> path) {
		for (BPMNElement pathEl : path) {
			if (pathEl instanceof BPMNTask) {
				BPMNTask currTask = (BPMNTask) pathEl;
				if (currTask.getParticipant().equals(reader) && dataO.getReaderParticipants().contains(reader)) {
					return true;
				}
			}
		}

		return false;
	}

	private boolean isParticipantInList(List<BPMNElement> el, BPMNParticipant p) {
		for (BPMNElement e : el) {
			if (e instanceof BPMNTask) {
				BPMNTask currentTask = (BPMNTask) e;
				if (currentTask.getParticipant().equals(p)) {
					return true;
				}
			}
		}
		return false;
	}

	private void mapProcessElements() {
		Collection<FlowNode> processNodes = modelInstance.getModelElementsByType(FlowNode.class);
		for (FlowNode node : processNodes) {
			this.mapCamundaFlowNodes(node);
		}
	}

	private void mapSuccessorsAndPredecessors(FlowNode currentNode, FlowNode endNode, LinkedList<SequenceFlow> stack,
			ArrayList<Label> currentLabels) {
		// route depth first through the process and add the successors and predecessors
		// to the
		// nodes

		stack.addAll(currentNode.getOutgoing());

		if (stack.isEmpty()) {
			return;
		}

		while (!stack.isEmpty()) {
			SequenceFlow currentSeqFlow = stack.pop();

			FlowNode targetFlowNode = currentSeqFlow.getTarget();

			BPMNElement targetBPMNElement = this.getNodeById(targetFlowNode.getId());
			BPMNElement currentBPMNElement = this.getNodeById(currentNode.getId());

			if ((!currentLabels.isEmpty()) && currentBPMNElement.getLabelHasBeenSet() == false) {
				currentBPMNElement.addLabels(currentLabels);
				currentBPMNElement.setLabelHasBeenSet(true);
			}

			// add the targetFlowNode as a successor to the currentNode
			if (!currentBPMNElement.getSuccessors().contains(targetBPMNElement)) {
				currentBPMNElement.addSuccessor(targetBPMNElement);
			}

			// add the currentNode as a predecessor to the targetFlowNode
			if (!targetBPMNElement.getPredecessors().contains(currentBPMNElement)) {
				targetBPMNElement.addPredecessor(currentBPMNElement);
			}

			// add the labels to the elements
			if (currentBPMNElement instanceof BPMNExclusiveGateway) {
				BPMNExclusiveGateway bpmnEx = (BPMNExclusiveGateway) currentBPMNElement;
				if (bpmnEx.getType().equals("split")) {
					Label label = new Label(bpmnEx.getName(), currentSeqFlow.getName());
					// check if label exists
					for (Label l : labelList) {
						if (l.getLabel().equals(label.getLabel())) {
							label = l;
						}
					}

					currentLabels = new ArrayList<Label>();
					currentLabels.addAll(currentBPMNElement.getPredecessors().iterator().next().getLabels());
					currentLabels.add(label);

				} else if (bpmnEx.getType().equals("join") && (!currentLabels.isEmpty())) {

					currentLabels.remove(currentLabels.size() - 1);
				}

			}

			this.mapSuccessorsAndPredecessors(targetFlowNode, endNode, new LinkedList<SequenceFlow>(), currentLabels);

		}

	}

	public Collection<FlowNode> getSucceedingFlowNodes(FlowNode node) {
		Collection<FlowNode> followingFlowNodes = new ArrayList<FlowNode>();
		for (SequenceFlow sequenceFlow : node.getOutgoing()) {
			followingFlowNodes.add(sequenceFlow.getTarget());
		}
		return followingFlowNodes;
	}

	public Collection<FlowNode> getPreceedingFlowNodes(FlowNode node) {
		Collection<FlowNode> preceedingFlowNodes = new ArrayList<FlowNode>();
		for (SequenceFlow sequenceFlow : node.getIncoming()) {
			preceedingFlowNodes.add(sequenceFlow.getSource());
		}
		return preceedingFlowNodes;
	}

	public BPMNElement mapCamundaFlowNodes(FlowNode node) {
		BPMNElement mappedNode = null;

		// Check if the FlowNode has already been mapped
		for (BPMNElement e : this.processElements) {
			if (node.getId().equals(e.getId())) {
				return e;
			}
		}

		if (node instanceof Task || node instanceof UserTask || node instanceof BusinessRuleTask
				|| node instanceof SendTask) {
			if (node instanceof BusinessRuleTask) {
				if (this.modelWithLanes) {
					mappedNode = new BPMNBusinessRuleTask(node.getId(), node.getName());
				} else {
					mappedNode = new BPMNBusinessRuleTask(node.getId(),
							node.getName().substring(0, node.getName().indexOf("[")));
				}
				this.businessRuleTaskList.add((BPMNBusinessRuleTask) mappedNode);

			} else {
				if (this.modelWithLanes) {
					mappedNode = new BPMNTask(node.getId(), node.getName());
				} else {
					mappedNode = new BPMNTask(node.getId(), node.getName().substring(0, node.getName().indexOf("[")));

				}
			}

		} else if (node instanceof ParallelGateway) {
			if (node.getIncoming().size() >= 2 && node.getOutgoing().size() == 1) {
				mappedNode = new BPMNParallelGateway(node.getId(), node.getName(), "join");
			} else if (node.getIncoming().size() == 1 && node.getOutgoing().size() >= 2) {
				mappedNode = new BPMNParallelGateway(node.getId(), node.getName(), "split");

			}
		} else if (node instanceof ExclusiveGateway) {
			if (node.getIncoming().size() >= 2 && node.getOutgoing().size() == 1) {
				mappedNode = new BPMNExclusiveGateway(node.getId(), node.getName(), "join");
			} else if (node.getIncoming().size() == 1 && node.getOutgoing().size() >= 2) {
				mappedNode = new BPMNExclusiveGateway(node.getId(), node.getName(), "split");

			}
		} else if (node instanceof StartEvent) {
			mappedNode = new BPMNStartEvent(node.getId());
			this.bpmnStart = (BPMNStartEvent) mappedNode;
		} else if (node instanceof EndEvent) {
			mappedNode = new BPMNEndEvent(node.getId());
			this.bpmnEnd = (BPMNEndEvent) mappedNode;
		}

		this.processElements.add(mappedNode);
		return mappedNode;

	}

	// Query CAMUNDA FlowNodes and map them to BPMNDataElements
	// Set the Predecessors for each BPMNDataElement

	public void printProcessElements() {
		for (BPMNElement element : this.processElements) {
			element.printElement();
		}
	}

	public Collection<StartEvent> getStartEvent() {
		return this.startEvent;
	}

	// go through each lane of the process and store the lane as a participant to
	// the tasks
	public void storeLanePerTask() {
		if (this.modelWithLanes) {
			for (Lane l : this.modelInstance.getModelElementsByType(Lane.class)) {
				BPMNParticipant lanePart = new BPMNParticipant(l.getId(), l.getName().trim());
				this.publicSphere.add(lanePart);
				for (FlowNode flowNode : l.getFlowNodeRefs()) {
					for (BPMNElement t : this.processElements) {
						if (t instanceof BPMNTask && flowNode.getId().equals(t.getId())) {
							((BPMNTask) t).setParticipant(lanePart);
						}
					}
				}
			}
		}
	}

	public void addParticipantToTask() {
		if (this.modelWithLanes == false) {
			for (Task task : this.modelInstance.getModelElementsByType(Task.class)) {
				for (BPMNElement t : this.processElements) {
					if (t instanceof BPMNTask && task.getId().equals(t.getId())) {
						String participantName = task.getName().substring(task.getName().indexOf("["),
								task.getName().indexOf("]") + 1);
						boolean partAlreadyMapped = false;
						for (BPMNParticipant part : this.publicSphere) {
							if (part.getName().contentEquals(participantName)) {
								((BPMNTask) t).setParticipant(part);
								partAlreadyMapped = true;
								break;
							}
						}
						if (!partAlreadyMapped) {
							BPMNParticipant participant = new BPMNParticipant(participantName, participantName);
							((BPMNTask) t).setParticipant(participant);
							this.publicSphere.add(participant);
						}

					}
				}
			}
		}

	}

	// Map the Camunda Data Objects to BPMNDataObjects
	public void mapDataObjects() {
		for (DataObjectReference d : modelInstance.getModelElementsByType(DataObjectReference.class)) {
			this.dataObjects
					.add(new BPMNDataObject(d.getAttributeValue("dataObjectRef"), d.getName().trim(), d.getId()));
		}
	}

	private void mapDecisions(BPMNBusinessRuleTask bpmnBrt) {
		for (TextAnnotation text : modelInstance.getModelElementsByType(TextAnnotation.class)) {
			for (Association a : modelInstance.getModelElementsByType(Association.class)) {
				if (a.getAttributeValue("sourceRef").equals(bpmnBrt.getId())
						&& a.getAttributeValue("targetRef").equals(text.getId())) {
					if (text.getTextContent().startsWith("[Decision]") && bpmnBrt.getDecisionEvaluation() == null) {
						String dec = text.getTextContent();
						String str = dec.substring(dec.indexOf('{') + 1, dec.indexOf('}')).replaceAll("==", "=")
								.replaceAll("&&", "&").replace("||", "|");
						Pattern pattern2 = Pattern.compile(
								"(D\\d*)\\.(\\w*)([\\+\\-\\*\\/\\=|\\>|\\<]|[\\&|\\|])(true|false|\"[a-zA-Z0-9]*\"|[0-9]*)");

						Matcher matcher = pattern2.matcher(str);

						// check if fields needed for decision making are in the element documentation
						// of the data object!
						// e.g. D1.someVar means that there needs to be a variable called someVar in the
						// DataObject D1
						// if not, than insert these fields into the DataObject

						while (matcher.find()) {
							int i = 0;
							while (i <= matcher.groupCount()) {
								i++;
							}

							for (BPMNDataObject dataO : this.dataObjects) {
								if (dataO.getNameIdWithoutBrackets().equals(matcher.group(1))) {
									DataObject dao = modelInstance.getModelElementById(dataO.getId());
									for (DataObjectReference daoR : modelInstance
											.getModelElementsByType(DataObjectReference.class)) {
										if (daoR.getDataObject().equals(dao)) {
											ExtensionElements extensionElements = daoR.getExtensionElements();
											if (extensionElements == null) {
												extensionElements = modelInstance.newInstance(ExtensionElements.class);
												daoR.setExtensionElements(extensionElements);
											}
											CamundaProperty camundaProperty = null;
											if (extensionElements.getElements().isEmpty()) {
												CamundaProperties camundaProperties = extensionElements
														.addExtensionElement(CamundaProperties.class);

												String match = InfixToPostfix.getLastGroupMatches(matcher);
												if (match != null) {
													camundaProperty = modelInstance.newInstance(CamundaProperty.class);
													camundaProperty.setCamundaName(matcher.group(2));

													if (match.equals("true") || match.equals("false")) {
														camundaProperty.setCamundaValue("boolean");
													} else if (match.matches("\\d*")) {
														camundaProperty.setCamundaValue("double");
													} else if (match.matches("\"[a-zA-Z0-9]*\"")) {
														camundaProperty.setCamundaValue("String");
													} else if (match.matches("[\\/|\\+|\\-|\\*]")) {
														camundaProperty.setCamundaValue("double");
													} else {
														camundaProperty.setCamundaValue("double");
													}
													camundaProperties.addChildElement(camundaProperty);
												}
											} else {
												CamundaProperties cmd = extensionElements.getElementsQuery()
														.filterByType(CamundaProperties.class).singleResult();
												String match = InfixToPostfix.getLastGroupMatches(matcher);
												if (match != null) {
													camundaProperty = modelInstance.newInstance(CamundaProperty.class);
													camundaProperty.setCamundaName(matcher.group(2));

													if (match.equals("true") || match.equals("false")) {
														camundaProperty.setCamundaValue("boolean");
													} else if (match.matches("\\d*")) {
														camundaProperty.setCamundaValue("double");
													} else if (match.matches("\"[a-zA-Z0-9]*\"")) {
														camundaProperty.setCamundaValue("String");
													} else if (match.matches("[\\/|\\+|\\-|\\*]")) {
														camundaProperty.setCamundaValue("double");
													} else {
														camundaProperty.setCamundaValue("double");
													}
													boolean insert = true;
													for (CamundaProperty cp : cmd.getCamundaProperties()) {
														if (camundaProperty.getCamundaName()
																.equals(cp.getCamundaName())) {
															insert = false;
														}
													}
													if (insert == true) {
														cmd.addChildElement(camundaProperty);
													}
												}
											}

										}
									}

								}

							}

						}

						DecisionEvaluation decEval = new DecisionEvaluation();
						// We get back the mapped Expression String
						String mappedExpression = InfixToPostfix.mapDecision(str, decEval);
						// Now we convert the Expression to Postfix format
						String postfix = InfixToPostfix.convertInfixToPostfix(mappedExpression);
						decEval.setDecisionExpressionPostfix(postfix);
						bpmnBrt.setDecisionEvaluation(decEval);

					}

				}
			}
		}

	}

	private void mapDefaultTroubleShooter() {
		if (globalSphere.isEmpty()) {
			this.computeGlobalSphere();
		}

		for (TextAnnotation text : modelInstance.getModelElementsByType(TextAnnotation.class)) {

			String str = text.getTextContent();
			String troubleshooter = "";

			if (str.toLowerCase().contains(("Default").toLowerCase())
					&& str.toLowerCase().contains(("[Troubleshooter]").toLowerCase())) {
				troubleshooter = str.substring(str.indexOf('{') + 1, str.indexOf('}')).trim();
				boolean troubleshooterExists = false;
				for (BPMNParticipant part : this.publicSphere) {
					if (part.getName().contentEquals(troubleshooter)) {
						this.troubleShooter = part;
						troubleshooterExists = true;
						break;
					}
				}
				if (!troubleshooterExists) {
					BPMNParticipant troubleShooter = new BPMNParticipant(troubleshooter, troubleshooter);
					this.publicSphere.add(troubleShooter);
					this.troubleShooter = troubleShooter;
				}
			}

		}

		// when there is no default troubleShooter -> randomly choose one from the
		// public sphere
		if (this.troubleShooter == null) {
			if (!publicSphere.isEmpty()) {
				// each participant has been excluded for some decision
				// no optimal default troubleshooter for all decisions
				this.troubleShooter = CommonFunctionality.getRandomItem(publicSphere);
			} else {
				// create a new troubleshooter
				BPMNParticipant defaultTroubleShooter = new BPMNParticipant("defaultTroubleShooterId_1",
						"someTrustedThirdParty");
				this.troubleShooter = defaultTroubleShooter;
			}
		}

	}

	private void mapSphereAnnotations(Task task) {
		BPMNTask element = (BPMNTask) this.getNodeById(task.getId());
		String dataObject = "";
		String defaultSphere = "";
		for (TextAnnotation text : modelInstance.getModelElementsByType(TextAnnotation.class)) {
			// If at a writing task no corresponding sphere is annotated, take the default
			// one
			if (text.getTextContent().startsWith("Default:")) {
				String str = text.getTextContent();
				dataObject = str.substring(str.indexOf('[') + 1, str.indexOf(']'));
				defaultSphere = str.substring(str.indexOf('{') + 1, str.indexOf('}'));
			}
			for (Association a : modelInstance.getModelElementsByType(Association.class)) {
				for (BPMNDataObject bpmndo : element.getDataObjects()) {
					// Map the default Spheres for the data objects to the corresponding writing
					// tasks
					if (bpmndo.getNameIdWithoutBrackets().equals(dataObject) && bpmndo.getWriters().contains(element)) {
						// attach the defaultSphere to the dataObject
						if (bpmndo.getDefaultSphere().isEmpty()) {
							bpmndo.setDefaultSphere(defaultSphere);
						}
						element.getSphereAnnotation().putIfAbsent(bpmndo, defaultSphere);
					}

					if (a.getAttributeValue("sourceRef").equals(element.getId())
							&& a.getAttributeValue("targetRef").equals(text.getId())) {
						String str = text.getTextContent();
						// First 4 characters specify the Data object e.g. [D1]
						String s = str.substring(str.indexOf('[') + 1, str.indexOf(']'));
						// The Sphere for the data object is between {}
						String s2 = str.substring(str.indexOf('{') + 1, str.indexOf('}'));

						// Check if it is the right data object
						if (bpmndo.getNameIdWithoutBrackets().equals(s)) {
							element.getSphereAnnotation().put(bpmndo, s2);
						}

					}
				}
			}

		}

	}

	// map the annotated amount of needed voters to the BPMNExclusiveGateways
	// map the sphere connected to the gateway
	private void mapAnnotations(BPMNExclusiveGateway gtw) throws Exception {
		for (TextAnnotation text : modelInstance.getModelElementsByType(TextAnnotation.class)) {
			for (Association a : modelInstance.getModelElementsByType(Association.class)) {
				if (a.getAttributeValue("sourceRef").equals(gtw.getId())
						&& a.getAttributeValue("targetRef").equals(text.getId())) {
					String str = text.getTextContent().trim();
					if (!(str.contains("[") && str.contains("]") && str.contains("{") && str.contains("}"))) {
						throw new Exception("Annotation: " + a.getId() + " not modeled in correct form [...]{...}");
					}
					if (CommonFunctionality.containsIgnoreCase(str, "[Voters]")) {
						String subStr = str.substring(str.indexOf('{') + 1, str.indexOf('}'));
						String[] split = subStr.split(",");

						// there is a tuple of the form (amountvoters,votersSameChoice,amountLoops)
						try {
							gtw.setAmountVoters(Integer.parseInt(split[0]));
							gtw.setVotersSameChoice(Integer.parseInt(split[1]));
							gtw.setAmountLoops(Integer.parseInt(split[2]));
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else if (CommonFunctionality.containsIgnoreCase(str, "[Sphere]")) {
						String subStr = str.substring(str.indexOf('{') + 1, str.indexOf('}'));
						gtw.setSphere(subStr);
					} else if (CommonFunctionality.containsIgnoreCase(str, "[ExcludeParticipantConstraint]")) {
						String partName = str.substring(str.indexOf('{') + 1, str.indexOf('}'));
						boolean partAlreadyMapped = false;
						boolean exit = false;
						Iterator<BPMNParticipant> partIter = this.publicSphere.iterator();
						while (partIter.hasNext() && exit == false) {
							BPMNParticipant p = partIter.next();
							String participantName = p.getNameWithoutBrackets();
							if (participantName.contentEquals(partName)) {
								BPMNParticipant participant = this.getBPMNParticipantFromList(this.publicSphere,
										participantName);
								if (participant != null) {
									partAlreadyMapped = true;
									boolean constrExists = false;
									for (ExcludeParticipantConstraint exclConst : this.excludeParticipantConstraints) {
										// check if constraint already existst
										if (exclConst.getParticipantToExclude().equals(participant)) {
											// constraint already exists
											gtw.getConstraints().add(exclConst);
											if (!this.excludeParticipantConstraints.contains(exclConst)) {
												this.excludeParticipantConstraints.add(exclConst);
											}
											constrExists = true;
											exit = true;
											break;
										}
									}
									if (constrExists == false) {
										ExcludeParticipantConstraint epc = new ExcludeParticipantConstraint(
												participant);
										gtw.getConstraints().add(epc);
										if (!this.excludeParticipantConstraints.contains(epc)) {
											this.excludeParticipantConstraints.add(epc);
										}
										exit = true;
									}
								}
							}
						}

						if (!partAlreadyMapped) {
							BPMNParticipant partToBeMapped = new BPMNParticipant(partName, partName);
							this.publicSphere.add(partToBeMapped);
							ExcludeParticipantConstraint epc = new ExcludeParticipantConstraint(partToBeMapped);
							gtw.getConstraints().add(epc);
							if (!this.excludeParticipantConstraints.contains(epc)) {
								this.excludeParticipantConstraints.add(epc);
							}
						}

					} else if (CommonFunctionality.containsIgnoreCase(str, "[MandatoryParticipantConstraint]")) {
						String partName = str.substring(str.indexOf('{') + 1, str.indexOf('}'));
						boolean partAlreadyMapped = false;
						boolean exit = false;
						Iterator<BPMNParticipant> partIter = this.publicSphere.iterator();
						while (partIter.hasNext() && exit == false) {
							BPMNParticipant p = partIter.next();
							String participantName = p.getNameWithoutBrackets();
							if (participantName.contentEquals(partName)) {
								BPMNParticipant participant = this.getBPMNParticipantFromList(this.publicSphere,
										participantName);
								if (participant != null) {
									partAlreadyMapped = true;
									boolean constrExists = false;
									for (MandatoryParticipantConstraint mandConst : this.mandatoryParticipantConstraints) {
										// check if constraint already existst
										if (mandConst.getMandatoryParticipant().equals(participant)) {
											// constraint already exists
											gtw.getConstraints().add(mandConst);
											if (!this.mandatoryParticipantConstraints.contains(mandConst)) {
												this.mandatoryParticipantConstraints.add(mandConst);
											}
											constrExists = true;
											exit = true;
											break;
										}
									}
									if (constrExists == false) {
										MandatoryParticipantConstraint mpc = new MandatoryParticipantConstraint(
												participant);
										gtw.getConstraints().add(mpc);
										if (!this.mandatoryParticipantConstraints.contains(mpc)) {
											this.mandatoryParticipantConstraints.add(mpc);
										}
										exit = true;
									}
								}
							}
						}

						if (!partAlreadyMapped) {
							BPMNParticipant partToBeMapped = new BPMNParticipant(partName, partName);
							this.publicSphere.add(partToBeMapped);
							MandatoryParticipantConstraint mpc = new MandatoryParticipantConstraint(partToBeMapped);
							gtw.getConstraints().add(mpc);
							if (!this.mandatoryParticipantConstraints.contains(mpc)) {
								this.mandatoryParticipantConstraints.add(mpc);
							}
						}
					}

				}
			}
		}

	}

	// DataObjects can be attached to Tasks, BusinessRuleTasks and UserTasks in
	// Camunda
	public void mapAssociationsAndAnotations() throws Exception {
		for (BPMNDataObject bpmno : this.dataObjects) {
			for (Task t : modelInstance.getModelElementsByType(Task.class)) {
				for (BPMNElement e : this.processElements) {
					for (DataOutputAssociation doa : t.getDataOutputAssociations()) {
						if (doa.getTarget().getAttributeValue("dataObjectRef").equals(bpmno.getId())) {
							if (e instanceof BPMNTask && e.getId().equals(t.getId())) {
								bpmno.addWriterToDataObject((BPMNTask) e);
								bpmno.addParticipantToStaticSphere(((BPMNTask) e).getParticipant());
								((BPMNTask) e).addBPMNDataObject(bpmno);
								this.mapSphereAnnotations(t);
							}
						}
					}
					for (DataInputAssociation dia : t.getDataInputAssociations()) {
						if (dia.getSources().iterator().next().getAttributeValue("dataObjectRef")
								.equals(bpmno.getId())) {
							if (e instanceof BPMNTask && e.getId().equals(t.getId())) {
								bpmno.addReaderToDataObject((BPMNTask) e);
								bpmno.addParticipantToStaticSphere(((BPMNTask) e).getParticipant());
								((BPMNTask) e).addBPMNDataObject(bpmno);
							}
						}
					}
				}
			}
		}
		for (ExclusiveGateway xor : this.modelInstance.getModelElementsByType(ExclusiveGateway.class)) {
			if (xor.getOutgoing().size() >= 2) {
				try {
					this.mapAnnotations((BPMNExclusiveGateway) this.getNodeById(xor.getId()));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					throw e;
				}
			}
		}

		boolean decisionTakerPartOfVerifier = true;
		for (ExclusiveGateway xor : this.modelInstance.getModelElementsByType(ExclusiveGateway.class)) {
			BPMNExclusiveGateway xorGtw = (BPMNExclusiveGateway) this.getNodeById(xor.getId());
			LinkedList<BPMNParticipant> partsInExcludeConstraintsForGtw = new LinkedList<BPMNParticipant>();
			LinkedList<BPMNParticipant> partsInMandatoryConstraintsForGtw = new LinkedList<BPMNParticipant>();

			for (Constraint constraint : xorGtw.getConstraints()) {
				if (constraint instanceof ExcludeParticipantConstraint) {
					ExcludeParticipantConstraint exclConst = (ExcludeParticipantConstraint) constraint;
					if (!partsInExcludeConstraintsForGtw.contains(exclConst.getParticipantToExclude())) {
						partsInExcludeConstraintsForGtw.add(exclConst.getParticipantToExclude());
					}
				}
				if (constraint instanceof MandatoryParticipantConstraint) {
					MandatoryParticipantConstraint mandConst = (MandatoryParticipantConstraint) constraint;
					if (!partsInMandatoryConstraintsForGtw.contains(mandConst.getMandatoryParticipant())) {
						partsInMandatoryConstraintsForGtw.add(mandConst.getMandatoryParticipant());
					}
				}
			}

			// check if each decision taker is mandatory for the succeeding gtw due to
			// constraints
			for (BPMNElement predec : xorGtw.getPredecessors()) {
				if (predec instanceof BPMNBusinessRuleTask) {
					BPMNBusinessRuleTask predecBrt = (BPMNBusinessRuleTask) predec;
					BPMNParticipant decisionTaker = predecBrt.getParticipant();
					if (partsInMandatoryConstraintsForGtw.contains(decisionTaker)
							&& partsInExcludeConstraintsForGtw.contains(decisionTaker)) {
						decisionTakerPartOfVerifier = false;
						break;
					} else if (!partsInMandatoryConstraintsForGtw.contains(decisionTaker)) {
						decisionTakerPartOfVerifier = false;
						break;
					} else if (partsInExcludeConstraintsForGtw.contains(decisionTaker)) {
						decisionTakerPartOfVerifier = false;
						break;
					}
				}
			}

			if (decisionTakerPartOfVerifier) {
				this.deciderOrVerifier = "verifier";
			} else {
				this.deciderOrVerifier = "decider";
			}

			if (partsInMandatoryConstraintsForGtw.size() > xorGtw.getAmountVoters()) {
				throw new Exception("Mandatory participants > amount voters needed at " + xorGtw.getId() + " ("
						+ partsInMandatoryConstraintsForGtw.size() + " > " + xorGtw.getAmountVoters() + ")");
			}

		}

		for (BusinessRuleTask brt : this.modelInstance.getModelElementsByType(BusinessRuleTask.class)) {
			this.mapDecisions((BPMNBusinessRuleTask) this.getNodeById(brt.getId()));
		}
	}

	public void printDataObjects() {
		for (BPMNDataObject bpmnd : this.dataObjects) {
			bpmnd.printWritersOfDataObject();
			bpmnd.printReadersOfDataObject();

		}
	}

	public void computeGlobalSphere() {
		for (BPMNElement e : this.processElements) {
			if (e instanceof BPMNTask) {
				this.globalSphereTasks.add((BPMNTask) e);
				if (!(globalSphere.contains(((BPMNTask) e).getParticipant()))) {
					this.globalSphere.add(((BPMNTask) e).getParticipant());
				}
			}
		}
	}

	public void printGlobalSphere() {
		System.out.println("Global Sphere contains: ");
		for (BPMNParticipant bpmnp : this.globalSphere) {
			bpmnp.printParticipant();
		}
	}

	public LinkedList<BPMNParticipant> getGlobalSphereList() {
		return this.globalSphere;
	}

	// Uses Breadth first search to go through the predecessors of a node to find
	// the last Writer to that dataObject
	public BPMNTask getLastWriterForDataObject(BPMNElement currentNode, BPMNDataObject dataObject,
			ArrayList<BPMNElement> alreadyFound) {
		LinkedList<BPMNElement> queue = new LinkedList<BPMNElement>();

		queue.addAll(currentNode.getPredecessors());
		// queue.remove(alreadyFound);
		while (!(queue.isEmpty())) {
			BPMNElement element = queue.poll();
			// if the element is a writer who has been already found, than skip it

			if (alreadyFound.contains(element)) {
				element = queue.poll();
			}

			if (element instanceof BPMNTask) {
				// Check if the element is a Writer to the dataObject given as a parameter
				if (((BPMNTask) element).getDataObjects().contains(dataObject)) {
					for (BPMNDataObject data : ((BPMNTask) element).getDataObjects()) {
						if (data.getWriters().contains(element) && data.equals(dataObject)) {
							return (BPMNTask) element;
						}
					}

				}
			}

			for (BPMNElement predecessor : element.getPredecessors()) {
				queue.add(predecessor);
				// if a parallel split is visited - the other path needs to be checked for
				// writers too
				if (predecessor instanceof BPMNParallelGateway) {
					BPMNParallelGateway currentEl = (BPMNParallelGateway) predecessor;
					if (currentEl.getType().equals("split")) {
						queue.add(this.getCorrespondingGtw(currentEl));
					}
				}
			}
		}
		return null;

	}

	public ArrayList<BPMNTask> getLastWriterListForDataObject(BPMNBusinessRuleTask brt, BPMNDataObject data)
			throws NullPointerException, InterruptedException, Exception {
		LinkedList<LinkedList<FlowNode>> list = CommonFunctionality.getAllPathsBetweenNodes(modelInstance,
				this.bpmnStart.getId(), brt.getId());
		ArrayList<BPMNTask> lastWriterList = new ArrayList<BPMNTask>();
		for (LinkedList<FlowNode> path : list) {
			for (int i = path.size() - 1; i >= 0; i--) {
				FlowNode f = path.get(i);
				if (f instanceof Task) {
					BPMNTask task = (BPMNTask) this.getBPMNElementByFlowNodeId(f.getId());
					if (data.getWriters().contains(task)) {
						if (!lastWriterList.contains(task)) {
							lastWriterList.add(task);
						}
						break;
					}
				}
			}

		}
		return lastWriterList;

	}

	public BPMNElement getLastElementOnLaneBeforeCurrentNode(BPMNTask currentNode, BPMNParticipant participant) {
		LinkedList<BPMNElement> queue = new LinkedList<BPMNElement>();

		queue.addAll(currentNode.getPredecessors());
		BPMNElement element = null;
		while (!(queue.isEmpty())) {
			element = queue.poll();
			if (element instanceof BPMNTask) {
				BPMNTask task = (BPMNTask) element;

				if (task.getParticipant().equals(participant)) {
					return task;
				}
			}

			for (BPMNElement predecessor : element.getPredecessors()) {
				queue.add(predecessor);
			}
		}
		return element;

	}

	public LinkedList<LinkedList<BPMNElement>> allPathsBetweenNodesBFS(BPMNElement startNode, BPMNElement endNode,
			LinkedList<BPMNElement> queue, LinkedList<BPMNElement> gtwQueue, LinkedList<BPMNElement> currentPath,
			LinkedList<LinkedList<BPMNElement>> paths) {

		queue.add(startNode);
		boolean reachedEndGateway = false;

		while (!(queue.isEmpty())) {
			BPMNElement element = queue.poll();
			currentPath.add(element);

			if (element.getId().equals(endNode.getId())) {
				paths.add(currentPath);
				element = queue.poll();
				if (element == null) {
					return paths;
				}
			}

			if (element instanceof BPMNParallelGateway && ((BPMNParallelGateway) element).getType().equals("split")) {
				for (BPMNElement successor : element.getSuccessors()) {
					gtwQueue.add(successor);
				}
			}

			if (element instanceof BPMNParallelGateway && ((BPMNParallelGateway) element).getType().equals("join")) {
				gtwQueue.poll();
				if (!gtwQueue.isEmpty()) {
					reachedEndGateway = true;
				}
			}

			for (BPMNElement successor : element.getSuccessors()) {

				if (element instanceof BPMNExclusiveGateway
						&& ((BPMNExclusiveGateway) element).getType().equals("split")) {
					LinkedList<BPMNElement> newPath = new LinkedList<BPMNElement>();
					newPath.addAll(currentPath);

					this.allPathsBetweenNodesBFS(successor, endNode, queue, gtwQueue, newPath, paths);
				} else {
					if (reachedEndGateway == false) {
						queue.add(successor);
					}
				}

			}
			reachedEndGateway = false;
		}

		return paths;

	}

	public synchronized LinkedList<VoterForXorArc> generateArcsForXorSplitWithConstraints(BPMNBusinessRuleTask currBrt)
			throws Exception {
		LinkedList<VoterForXorArc> brtCombs = new LinkedList<VoterForXorArc>();

		if (currBrt.getSuccessors().iterator().next() instanceof BPMNExclusiveGateway) {
			BPMNExclusiveGateway bpmnEx = (BPMNExclusiveGateway) currBrt.getSuccessors().iterator().next();
			// get all the possible combinations of participants for the brt
			// consider the constraints e.g. participants may be excluded or mandatory
			LinkedList<BPMNParticipant> participantsToCombine = new LinkedList<BPMNParticipant>();
			participantsToCombine.addAll(this.globalSphere);
			LinkedList<BPMNParticipant> mandatoryParticipants = new LinkedList<BPMNParticipant>();

			for (Constraint constraint : bpmnEx.getConstraints()) {
				if (constraint instanceof ExcludeParticipantConstraint) {
					// exclude the participants due to constraints
					BPMNParticipant partToRemove = ((ExcludeParticipantConstraint) constraint)
							.getParticipantToExclude();
					participantsToCombine.remove(partToRemove);
				} else if (constraint instanceof MandatoryParticipantConstraint) {
					BPMNParticipant mandatoryPart = ((MandatoryParticipantConstraint) constraint)
							.getMandatoryParticipant();
					if (!mandatoryParticipants.contains(mandatoryPart)) {
						mandatoryParticipants.add(mandatoryPart);
					}
				}
			}

			if (currBrt.getCombinations().isEmpty()) {

				LinkedList<LinkedList<BPMNParticipant>> list = Combination.getPermutations(participantsToCombine,
						bpmnEx.getAmountVoters());
				Iterator<LinkedList<BPMNParticipant>> listIter = list.iterator();
				while (listIter.hasNext()) {
					LinkedList<BPMNParticipant> partList = listIter.next();
					if (!partList.containsAll(mandatoryParticipants)) {
						listIter.remove();
					}
				}
				if (list.isEmpty()) {
					throw new Exception("No possible combination of voters for " + currBrt.getId());
				}
				currBrt.getCombinations().putIfAbsent(currBrt, list);

				try {
					if (this.amountPossibleCombinationsOfParticipants.contentEquals("0")) {
						this.setAmountPossibleCombinationsOfParticipants("1");
					}

					if (!this.amountPossibleCombinationsOfParticipants.contentEquals("Overflow")) {
						int currAmountCombinationsOfParticipants = Math.multiplyExact(
								Integer.parseInt(this.amountPossibleCombinationsOfParticipants), list.size());
						this.amountPossibleCombinationsOfParticipants = currAmountCombinationsOfParticipants + "";
					}
				} catch (ArithmeticException e) {
					this.amountPossibleCombinationsOfParticipants = "Overflow";
				}

			}

			for (LinkedList<BPMNParticipant> partList : currBrt.getCombinations().get(currBrt)) {

				VoterForXorArc arc = new VoterForXorArc(currBrt, bpmnEx, partList);

				// check if arc already exists
				if (!this.arcAlreadyGeneratedBool(currBrt, arc)) {
					brtCombs.add(arc);
				} else {
					ArcWithCost.id--;
				}

			}

		}

		return brtCombs;

	}

	public void insertIfCheapestWithBound(LinkedList<ProcessInstanceWithVoters> processInstancesWithVoters,
			ProcessInstanceWithVoters currInstance, int bound) throws InterruptedException {
		if (Thread.currentThread().isInterrupted()) {
			System.err.println("Interrupted" + Thread.currentThread().getName());
			throw new InterruptedException();
		}

		if (processInstancesWithVoters.isEmpty()) {
			processInstancesWithVoters.add(currInstance);
		} else {
			// check if current Instance is as cheap or cheaper than the ones in the list
			if (currInstance.getCostForModelInstance() < processInstancesWithVoters.getFirst()
					.getCostForModelInstance()) {
				// new cheapest combination found
				processInstancesWithVoters.clear();
				processInstancesWithVoters.add(currInstance);
			} else if (currInstance.getCostForModelInstance() == processInstancesWithVoters.getFirst()
					.getCostForModelInstance()) {
				// only insert if bound not reached!
				if (bound == 0) {
					processInstancesWithVoters.add(currInstance);
				} else {
					if (processInstancesWithVoters.size() < bound) {
						processInstancesWithVoters.add(currInstance);
					}
				}
			}
		}

	}

	public HashMap<Boolean, LinkedList<LinkedList<BPMNElement>>> getEffectivePathsBetweenWriterAndTargetElement(
			BPMNDataObject dataO, BPMNElement writerTask, BPMNElement targetElement)
			throws NullPointerException, InterruptedException, Exception {
		// returns a hashmap with the keys true and false
		// where key = true: contains all effective Paths from writer to brt (no other
		// writer writes to same data object)
		// where key = false: contains all paths from writerTask to otherWriter (writer
		// writes to same data object)

		LinkedList<LinkedList<BPMNElement>> allPathsBetweenWriterAndTargetElement = this
				.getPathsWithMappedNodesFromCamundaNodes(CommonFunctionality.getAllPathsBetweenNodes(this.modelInstance,
						writerTask.getId(), targetElement.getId()));
		HashMap<Boolean, LinkedList<LinkedList<BPMNElement>>> pathMap = new HashMap<Boolean, LinkedList<LinkedList<BPMNElement>>>();
		LinkedList<LinkedList<BPMNElement>> effectivePaths = new LinkedList<LinkedList<BPMNElement>>();
		LinkedList<LinkedList<BPMNElement>> nonEffectivePaths = new LinkedList<LinkedList<BPMNElement>>();
		LinkedList<BPMNTask> otherWritersFound = new LinkedList<BPMNTask>();

		for (LinkedList<BPMNElement> pathInstance : allPathsBetweenWriterAndTargetElement) {
			boolean effective = true;
			for (BPMNElement el : pathInstance) {
				if (el instanceof BPMNTask) {
					BPMNTask currentTask = (BPMNTask) el;
					if (dataO.getWriters().contains(currentTask)
							&& !((BPMNTask) writerTask).getParticipant().equals(currentTask.getParticipant())) {
						// some other writer that writes to dataO has been found on path
						effective = false;
						if (!otherWritersFound.contains(currentTask)) {
							otherWritersFound.add(currentTask);
						}
						break;
					}
				}

			}
			if (effective) {
				effectivePaths.add(pathInstance);
			}
		}

		for (BPMNTask otherWriter : otherWritersFound) {
			LinkedList<LinkedList<BPMNElement>> pathsBetweenWriterAndOtherWriter = this
					.getPathsWithMappedNodesFromCamundaNodes(CommonFunctionality
							.getAllPathsBetweenNodes(this.modelInstance, writerTask.getId(), otherWriter.getId()));
			nonEffectivePaths.addAll(pathsBetweenWriterAndOtherWriter);
		}

		pathMap.putIfAbsent(true, effectivePaths);
		pathMap.putIfAbsent(false, nonEffectivePaths);
		return pathMap;

	}

	public BPMNElement getNodeById(String id) {
		for (BPMNElement e : this.processElements) {
			if (e.getId().equals(id)) {
				return e;
			}
		}
		return null;
	}

	public FlowNode getFlowNodeByBPMNNodeId(String id) {
		for (FlowNode flowNode : this.modelInstance.getModelElementsByType(FlowNode.class)) {
			if (flowNode.getId().equals(id)) {
				return flowNode;
			}
		}
		return null;
	}

	public BPMNElement getBPMNElementByFlowNodeId(String id) {
		for (BPMNElement element : this.processElements) {
			if (element.getId().equals(id)) {
				return element;
			}
		}
		return null;
	}

	public void printElementPredecessorAndSuccessor() {
		this.processElements.forEach(f -> {
			f.printPredecessors();
		});
		this.processElements.forEach(f -> {
			f.printSuccessors();
		});

	}

	private boolean atLeastInSphere(String currentSphere, String requiredSphere) {
		// return true if requiredSphere comprises the currentSphere
		// if currentSphere e.g. WD and requiredSphere SD return false

		// basic comparison -> if the spheres are the same, return true
		if (requiredSphere.contentEquals(currentSphere)) {
			return true;
		} else {
			if (requiredSphere.contentEquals("Strong-Dynamic")) {
				return false;
			}

			else if (requiredSphere.contentEquals("Weak-Dynamic")) {
				if (currentSphere.contentEquals("Strong-Dynamic")) {
					return true;
				}
			} else if (requiredSphere.contentEquals("Static")) {
				if (currentSphere.contentEquals("Weak-Dynamic") || currentSphere.contentEquals("Strong-Dynamic")) {
					return true;
				}

			} else if (requiredSphere.contentEquals("Global")) {
				if (currentSphere.contentEquals("Static") || currentSphere.contentEquals("Weak-Dynamic")
						|| currentSphere.contentEquals("Strong-Dynamic")) {
					return true;

				}
			}
		}
		return false;

	}

	/*
	 * private void addReaderToSphere(BPMNBusinessRuleTask brt, BPMNDataObject
	 * bpmndo, int count, BPMNTask writer, BPMNTask reader,
	 * LinkedList<LinkedList<BPMNElement>> paths) { ArrayList<Label> writerLabels =
	 * writer.getLabels(); ArrayList<Label> readerLabels = reader.getLabels(); int
	 * anotherWriterOnPath = paths.size() - count; // if anotherWriterOnPath == 0,
	 * then there is anotherWriter on each path between // reader and writer // if
	 * anotherWriterOnPath == paths.size, then there is no path containing //
	 * anotherWriter between reader and writer // if anotherWriterOnPath > 0 && <
	 * paths.size, then there is at least one path // containing another writer
	 * between reader and writer
	 * 
	 * if (writerLabels.equals(readerLabels)) { if (anotherWriterOnPath ==
	 * paths.size()) { writer.addTaskToSDHashMap(brt, bpmndo, reader); } else if
	 * (anotherWriterOnPath > 0 && anotherWriterOnPath < paths.size()) {
	 * writer.addTaskToWDHashMap(brt, bpmndo, reader); } //
	 * writer.addTaskToWDHashMap(bpmndo, reader); } else if (writerLabels.size() >
	 * readerLabels.size()) { if (readerLabels.size() == 0) { if
	 * (anotherWriterOnPath == paths.size()) { writer.addTaskToSDHashMap(brt,
	 * bpmndo, reader); } else if (anotherWriterOnPath > 0 && anotherWriterOnPath <
	 * paths.size()) { writer.addTaskToWDHashMap(brt, bpmndo, reader); } //
	 * writer.addTaskToWDHashMap(bpmndo, reader); } else { if
	 * (writerLabels.containsAll(readerLabels)) { if (anotherWriterOnPath ==
	 * paths.size()) { writer.addTaskToSDHashMap(brt, bpmndo, reader); } else if
	 * (anotherWriterOnPath > 0 && anotherWriterOnPath < paths.size()) {
	 * writer.addTaskToWDHashMap(brt, bpmndo, reader); } //
	 * writer.addTaskToWDHashMap(bpmndo, reader); } else { if (anotherWriterOnPath
	 * == paths.size()) { writer.addTaskToWDHashMap(brt, bpmndo, reader); } else if
	 * (anotherWriterOnPath > 0 && anotherWriterOnPath < paths.size()) {
	 * writer.addTaskToWDHashMap(brt, bpmndo, reader); } } }
	 * 
	 * } else if (writerLabels.size() < readerLabels.size()) { if
	 * (anotherWriterOnPath == paths.size()) { writer.addTaskToWDHashMap(brt,
	 * bpmndo, reader); } else if (anotherWriterOnPath > 0 && anotherWriterOnPath <
	 * paths.size()) { writer.addTaskToWDHashMap(brt, bpmndo, reader); } } else if
	 * (writerLabels.size() == readerLabels.size() &&
	 * !writerLabels.equals(readerLabels)) { if (anotherWriterOnPath ==
	 * paths.size()) { writer.addTaskToWDHashMap(brt, bpmndo, reader); } else if
	 * (anotherWriterOnPath > 0 && anotherWriterOnPath < paths.size()) {
	 * writer.addTaskToWDHashMap(brt, bpmndo, reader); } }
	 * 
	 * }
	 */

	public boolean checkProcessModel() {
		// Check how many participants are needed for the voting!
		// Check whether there are enough participants available in the global Sphere
		for (BPMNElement bpmnElement : this.processElements) {
			if (bpmnElement instanceof BPMNExclusiveGateway) {
				if (((BPMNExclusiveGateway) bpmnElement).getType().equals("split")) {
					if (((BPMNExclusiveGateway) bpmnElement).getAmountVoters() > this.globalSphere.size()) {
						return false;
					}
				}
			}
		}
		return true;

	}

	public BPMNGateway getCorrespondingGtw(BPMNGateway gtw) {
		for (BPMNElement el : this.processElements) {
			if (el instanceof BPMNGateway) {
				BPMNGateway gateway = (BPMNGateway) el;
				if (gtw.getType().contentEquals("split") && gateway.getType().contentEquals("join")
						&& gateway.getName().equals(gtw.getName())) {
					// return corresponding join gtw
					return gateway;
				} else if (gtw.getType().contentEquals("join") && gateway.getType().contentEquals("split")
						&& gateway.getName().equals(gtw.getName())) {
					return gateway;
				}
			}
		}
		return null;
	}

	public ArrayList<BPMNBusinessRuleTask> getBusinessRuleTasks() {
		return this.businessRuleTaskList;
	}

	/*
	 * public void moveNodesToCorrespondingLanesInDiagram(BPMNTask votingTask) { //
	 * put the inserted voting tasks with fluent builder to the correct lane in the
	 * // diagram! // can not be done with the fluent builder directly // the x
	 * value of the task is correct, but y needs to be set to equal the last //
	 * element in the corresponding lane BPMNElement lastElementInLane =
	 * this.getLastElementOnLaneBeforeCurrentNode(votingTask,
	 * votingTask.getParticipant()); System.out.println("LAstelement inlane " +
	 * lastElementInLane.getId());
	 * 
	 * double newY = 0; double newX = 0; double oldX = 0; BpmnShape votingTaskShape
	 * = null; for (BpmnShape shape :
	 * modelInstance.getModelElementsByType(BpmnShape.class)) { if
	 * (shape.getBpmnElement().getId().equals(votingTask.getId())) { votingTaskShape
	 * = shape; oldX = shape.getBounds().getX(); newX = shape.getBounds().getX() +
	 * 20; } if (shape.getBpmnElement().getId().equals(lastElementInLane.getId())) {
	 * newY = shape.getBounds().getY();
	 * 
	 * } } if (votingTaskShape != null) { votingTaskShape.getBounds().setX(newX);
	 * votingTaskShape.getBounds().setY(newY); }
	 * 
	 * // change the incoming edge SequenceFlow incomingEdge =
	 * this.getFlowNodeByBPMNNodeId(votingTask.getId()).getIncoming().iterator().
	 * next(); System.out.println("IncomingEdge: " + incomingEdge.getId()); for
	 * (BpmnEdge edge : modelInstance.getModelElementsByType(BpmnEdge.class)) { if
	 * (edge.getBpmnElement().equals(incomingEdge)) { //
	 * edge.getBpmnLabel().getBounds().setX(newX); //
	 * edge.getBpmnLabel().getBounds().setY(newY); Iterator<Waypoint> wpIter =
	 * edge.getWaypoints().iterator(); while (wpIter.hasNext()) { Waypoint currPoint
	 * = wpIter.next(); if (!wpIter.hasNext()) { currPoint.setX(newX);
	 * currPoint.setY(newY); } }
	 * 
	 * } }
	 * 
	 * }
	 */

	public BPMNElement searchReadersAfterBrt(BPMNTask lastWriter, BPMNDataObject dataO, BPMNBusinessRuleTask bpmnBrt,
			ArrayList<BPMNElement> readerList) {

		// if the lastWriter writes Strong-Dynamically the search can not be extended
		// beyond the brt, since there is a XOR-Split right after the brt
		BPMNElement nextPossibleReader = bpmnBrt;

		for (Entry<BPMNDataObject, String> sphereEntry : lastWriter.getSphereAnnotation().entrySet()) {
			if (sphereEntry.getKey().equals(dataO)) {

				LinkedList<BPMNElement> stack = new LinkedList<BPMNElement>();
				stack.addAll(bpmnBrt.getSuccessors());
				while (!stack.isEmpty()) {
					nextPossibleReader = stack.poll();
					// skip the already found readers
					if (readerList.contains(nextPossibleReader)) {
						nextPossibleReader = stack.poll();
					}
					if (dataO.getReaders().contains(nextPossibleReader) && (!readerList.contains(nextPossibleReader))) {
						readerList.add(nextPossibleReader);
						return nextPossibleReader;
					}

					stack.addAll(nextPossibleReader.getSuccessors());

				}
				// if no reader is found
				nextPossibleReader = bpmnBrt;

			}

		}

		return nextPossibleReader;

	}

	public String getSphereForParticipantOnPathsWithAlreadyChosenVoters(ProcessInstanceWithVoters currInst,
			BPMNBusinessRuleTask currentBrt, BPMNElement writerTask, BPMNDataObject dataO, BPMNParticipant reader,
			HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosenVoters,
			LinkedList<LinkedList<BPMNElement>> pathsBetweenWriterAndBrt)
			throws NullPointerException, InterruptedException, Exception {

		if (writerTask == null || currentBrt == null || dataO == null) {
			return "not existent";
		}
		// calculate sphere for the reader at the position of the currentBrt, therefore:
		// compute paths between lastWriter and end
		// then we only look at those, that contain the currentBrt (therefore a specific
		// decision branch)
		// if reader is on each path -> reader is in SD
		// if reader is on some path -> readers is in WD
		String sphereForReader = this.getSphere(currInst, currentBrt, writerTask, dataO, reader,
				pathsBetweenWriterAndBrt, alreadyChosenVoters);

		return sphereForReader;

	}

	private String getSphere(ProcessInstanceWithVoters currInst, BPMNBusinessRuleTask currentBrt,
			BPMNElement writerTask, BPMNDataObject dataO, BPMNParticipant reader,
			LinkedList<LinkedList<BPMNElement>> pathsBetweenWriterAndEnd,
			HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosenVoters) {
		if (writerTask == null || currentBrt == null) {
			return "not existent";
		}

		// the participant of the lastWriter is always in SD at position of the
		// currentBrt since he writes the data in that instance type
		if (((BPMNTask) writerTask).getParticipant().equals(reader)) {
			return "Strong-Dynamic";
		}

		int strongDynamicReaderCount = 0;
		int pathsToBeConsidered = 0;
		for (LinkedList<BPMNElement> path : pathsBetweenWriterAndEnd) {
			if (path.contains(currentBrt)) {
				pathsToBeConsidered++;
				boolean readerFound = false;
				boolean anotherWriterFound = false;
				for (int i = 0; i < path.size() && anotherWriterFound == false && readerFound == false; i++) {
					BPMNElement pathEl = path.get(i);
					if (pathEl instanceof BPMNTask) {
						BPMNTask task = (BPMNTask) pathEl;
						BPMNParticipant currTaskPart = task.getParticipant();
						if (task instanceof BPMNBusinessRuleTask) {
							// the participant of the brt gets substituted with the already chosen voters
							// for it
							BPMNBusinessRuleTask currentBrtOnPath = (BPMNBusinessRuleTask) task;
							if (alreadyChosenVoters.get(currentBrtOnPath) != null) {
								// check if the reader given as an argument is one of the already chosen voters
								// for the brt
								if (alreadyChosenVoters.get(currentBrtOnPath).contains(reader)) {
									readerFound = true;
								}
							} else {
								if (currInst.getReadersOfDataObjects().get(dataO).contains(currentBrtOnPath)
										&& currTaskPart.equals(reader) && this.isParticipantInList(
												currInst.getReadersOfDataObjects().get(dataO), reader)) {
									// reader found on the path
									readerFound = true;
								}

							}
						} else {
							if (currInst.getWritersOfDataObjects().get(dataO).contains(task)
									&& dataO.getWriters().contains(task)
									&& !currTaskPart.equals(((BPMNTask) writerTask).getParticipant())) {
								// another writer found to same dataO
								// further readers on that path will read data from that newly found writer
								anotherWriterFound = true;
							}
							if (currInst.getReadersOfDataObjects().get(dataO).contains(task)
									&& currTaskPart.equals(reader) && this.isParticipantInList(
											currInst.getReadersOfDataObjects().get(dataO), reader)) {
								// reader found on the path
								readerFound = true;
							}

						}
					}

				}
				if (readerFound) {
					strongDynamicReaderCount++;
				}
			}
		}
		if (strongDynamicReaderCount == pathsToBeConsidered) {
			return "Strong-Dynamic";
		} else if (strongDynamicReaderCount > 0) {
			return "Weak-Dynamic";
		} else if (currInst.getStaticSphere().get(dataO).contains(reader)) {
			return "Static";
		} else if (currInst.getGlobalSphere().contains(reader)) {
			return "Global";
		} else {
			return "Public";
		}

	}

	public String getAmountPossibleCombinationsOfParticipants() {
		return amountPossibleCombinationsOfParticipants;
	}

	public void setAmountPossibleCombinationsOfParticipants(String amountPossibleCombinationsOfParticipants) {
		this.amountPossibleCombinationsOfParticipants = amountPossibleCombinationsOfParticipants;
	}

	public LinkedList<LinkedList<FlowNode>> getAllPathsThroughProcess() {
		return this.pathsThroughProcess;
	}

	public ArrayList<BPMNDataObject> getDataObjects() {
		return this.dataObjects;
	}

	public BPMNEndEvent getBpmnEnd() {
		return this.bpmnEnd;
	}

	public void setBpmnEnd(BPMNEndEvent bpmnEnd) {
		this.bpmnEnd = bpmnEnd;
	}

	public BPMNStartEvent getBpmnStart() {
		return this.bpmnStart;
	}

	public LinkedList<ProcessInstanceWithVoters> getProcessInstancesWithVoters() {
		return processInstancesWithVoters;
	}

	public void setProcessInstancesWithVoters(LinkedList<ProcessInstanceWithVoters> processInstancesWithVoters) {
		this.processInstancesWithVoters = processInstancesWithVoters;
	}

	public double getExecutionTimeLocalMinimumAlgorithmWithLimit(int limitPerIteration) {
		String key = "localMinWithBound" + limitPerIteration;
		return this.executionTimeMap.get(key);
	}

	public double getExecutionTimeLocalMinimumAlgorithmWithLimit() {
		if (this.getAlgorithmToPerform().contains("localMinWithBound")) {
			return this.executionTimeMap.get(this.getAlgorithmToPerform());
		} else {
			return -1;
		}

	}

	public double getExecutionTimeLocalMinimumAlgorithm() {
		return this.executionTimeMap.get("localMin");
	}

	public double getExecutionTimeBruteForceAlgorithm() {
		return this.executionTimeMap.get("bruteForce");
	}

	public boolean compareResultsOfAlgorithms(LinkedList<ProcessInstanceWithVoters> localMinInstances,
			LinkedList<ProcessInstanceWithVoters> bruteForceInstances) {
		if (localMinInstances == null || bruteForceInstances == null) {
			return false;
		}
		int countCheapestSolutionFoundInBruteForceSolutions = 0;
		LinkedList<ProcessInstanceWithVoters> cheapestBruteForceSolutions = CommonFunctionality
				.getCheapestProcessInstancesWithVoters(bruteForceInstances);

		for (ProcessInstanceWithVoters cheapestInstBruteForce : cheapestBruteForceSolutions) {
			for (ProcessInstanceWithVoters cheapestInstLocalMin : localMinInstances) {
				if (cheapestInstBruteForce
						.getCostForModelInstance() == (cheapestInstLocalMin.getCostForModelInstance())) {
					if (cheapestInstBruteForce.getVotersMap().equals(cheapestInstLocalMin.getVotersMap())) {
						countCheapestSolutionFoundInBruteForceSolutions++;
					}

				}

			}

		}
		if (countCheapestSolutionFoundInBruteForceSolutions == localMinInstances.size()
				&& localMinInstances.size() == cheapestBruteForceSolutions.size()) {
			return true;
		}

		return false;
	}

	public LinkedList<LinkedList<BPMNElement>> getPathsWithMappedNodesFromCamundaNodes(
			LinkedList<LinkedList<FlowNode>> pathsWithCamundaNodes) {
		LinkedList<LinkedList<BPMNElement>> mappedPaths = new LinkedList<LinkedList<BPMNElement>>();

		for (LinkedList<FlowNode> path : pathsWithCamundaNodes) {
			LinkedList<BPMNElement> mappedPath = new LinkedList<BPMNElement>();
			for (FlowNode camundaNode : path) {
				mappedPath.add(this.getNodeById(camundaNode.getId()));
			}

			mappedPaths.add(mappedPath);
		}
		return mappedPaths;
	}

	public File getProcessFile() {
		return this.process;
	}

	public BpmnModelInstance getModelInstance() {
		return modelInstance;
	}

	public HashMap<DataObjectReference, LinkedList<FlowNode>> getReadersMap() {
		return readersMap;
	}

	public HashMap<DataObjectReference, LinkedList<FlowNode>> getWritersMap() {
		return writersMap;
	}

	public BPMNParticipant getTroubleShooter() {
		return this.troubleShooter;
	}

	public boolean modelWithLanes() {
		return this.modelWithLanes;
	}

	public void setAlgorithmToPerform(String algorithmToPerform) {
		this.algorithmToPerform = algorithmToPerform;
	}

	public String getAlgorithmToPerform() {
		return this.algorithmToPerform;
	}

	public List<BPMNElement> getProcessElements() {
		return this.processElements;
	}

	public boolean addParticipantToPublicShpere(BPMNParticipant part) {
		if (!publicSphere.contains(part)) {
			publicSphere.add(part);
			return true;
		}
		return false;
	}

	public LinkedList<BPMNParticipant> getPublicSphere() {
		return this.publicSphere;
	}

	public void setPublicSphere(LinkedList<BPMNParticipant> publicSphere) {
		this.publicSphere = publicSphere;
	}

	@Override
	public synchronized HashMap<String, LinkedList<ProcessInstanceWithVoters>> call()
			throws NullPointerException, InterruptedException, Exception {
		// TODO Auto-generated method stub

		System.out.println("Call algorithm for: " + this.process.getAbsolutePath());
		HashMap<String, LinkedList<ProcessInstanceWithVoters>> mapToReturn = new HashMap<String, LinkedList<ProcessInstanceWithVoters>>();
		if (this.algorithmToPerform.contentEquals("bruteForce")) {
			LinkedList<ProcessInstanceWithVoters> pInstBruteForce = this.bruteForceAlgorithm();
			mapToReturn.putIfAbsent("bruteForce", pInstBruteForce);

		} else if (this.algorithmToPerform.contentEquals("localMin")) {
			LinkedList<ProcessInstanceWithVoters> pInstLocalMin = this.localMinimumAlgorithm();
			mapToReturn.putIfAbsent("localMin", pInstLocalMin);

		} else if (this.algorithmToPerform.contains("localMinWithBound")) {
			String limit = this.algorithmToPerform;
			int limitPerIteration = Integer.parseInt(limit.replaceAll("\\D+", "").trim());
			LinkedList<ProcessInstanceWithVoters> pInstLocalMinWithBound = this
					.localMinimumAlgorithmWithLimit(limitPerIteration);
			mapToReturn.putIfAbsent(this.algorithmToPerform, pInstLocalMinWithBound);
		}

		return mapToReturn;

	}

	public String getDeciderOrVerifier() {
		return this.deciderOrVerifier;
	}

	public BPMNParticipant getBPMNParticipantFromList(LinkedList<BPMNParticipant> list, String name) {
		for (BPMNParticipant p : list) {
			if (p.getNameWithoutBrackets().contentEquals(name)) {
				return p;
			}
		}
		return null;
	}

}
