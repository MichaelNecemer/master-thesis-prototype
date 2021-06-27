package functionality;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
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
import Mapping.ArcWithCost;
import Mapping.Combination;
import Mapping.DecisionEvaluation;
import Mapping.InfixToPostfix;
import Mapping.Label;
import Mapping.ProcessInstanceWithVoters;
import Mapping.RequiredUpdate;
import Mapping.VoterForXorArc;

//Class that uses the camunda model API to interact with the process model directly without parsing the XML first to e.g. DOM Object
//Note that only processes with exactly 1 Start Event are possible
public class API implements Callable<HashMap<String, LinkedList<ProcessInstanceWithVoters>>> {

	static int value = 0;
	private Collection<StartEvent> startEvent;
	private Collection<EndEvent> endEvent;
	private File process;
	private BpmnModelInstance modelInstance;
	private int amountPossibleCombinationsOfParticipants;
	private boolean modelWithLanes;
	private HashMap<String, Double> executionTimeMap;
	private BPMNParticipant troubleShooter;
	private String algorithmToPerform;

	private BPMNStartEvent bpmnStart;
	private BPMNEndEvent bpmnEnd;
	private ArrayList<BPMNDataObject> dataObjects = new ArrayList<BPMNDataObject>();
	private ArrayList<BPMNElement> processElements = new ArrayList<BPMNElement>();
	private LinkedList<BPMNParticipant> globalSphere = new LinkedList<BPMNParticipant>();

	private ArrayList<BPMNBusinessRuleTask> businessRuleTaskList = new ArrayList<BPMNBusinessRuleTask>();
	private ArrayList<Label> labelList = new ArrayList<Label>();
	private LinkedList<LinkedList<FlowNode>> pathsThroughProcess = new LinkedList<LinkedList<FlowNode>>();
	private double costForAddingReaderAfterBrt;
	private double costForAddingToGlobalSphere;
	private double costForLiftingFromGlobalToStatic;
	private double costForLiftingFromStaticToWeakDynamic;
	private double costForLiftingFromWeakDynamicToStrongDynamic;
	private LinkedList<LinkedList<BPMNBusinessRuleTask>> possibleBrtCombinationsTillEnd;
	private LinkedList<ProcessInstanceWithVoters> processInstancesWithVoters;
	private HashMap<DataObjectReference, LinkedList<FlowNode>> readersMap;
	private HashMap<DataObjectReference, LinkedList<FlowNode>> writersMap;

	public API(String pathToFile, ArrayList<Double> cost, double costForAddingReaderAfterBrt) throws Exception {
		if (cost.size() != 4) {
			throw new Exception("Not exactly 4 cost parameters in the list!");
		}
		this.process = new File(pathToFile);
		this.modelInstance = Bpmn.readModelFromFile(process);
		this.startEvent = modelInstance.getModelElementsByType(StartEvent.class);
		this.endEvent = modelInstance.getModelElementsByType(EndEvent.class);

		this.costForAddingReaderAfterBrt = costForAddingReaderAfterBrt;
		this.costForAddingToGlobalSphere = cost.get(0);
		this.costForLiftingFromGlobalToStatic = cost.get(1);
		this.costForLiftingFromStaticToWeakDynamic = cost.get(2);
		this.costForLiftingFromWeakDynamicToStrongDynamic = cost.get(3);
		this.algorithmToPerform = "bruteForce";
		this.executionTimeMap = new HashMap<String, Double>();

		System.out.println("API for: " + process.getName());
		CommonFunctionality.isCorrectModel(modelInstance);
		this.readersMap = CommonFunctionality.getReadersForDataObjects(modelInstance);
		this.writersMap = CommonFunctionality.getWritersForDataObjects(modelInstance);

		this.mapAndCompute();
		
		this.setDependentBrts();
		this.pathsThroughProcess = CommonFunctionality.getAllPathsBetweenNodes(modelInstance,
				modelInstance.getModelElementsByType(StartEvent.class).iterator().next(),
				modelInstance.getModelElementsByType(EndEvent.class).iterator().next(), new LinkedList<FlowNode>(),
				new LinkedList<FlowNode>(), new LinkedList<FlowNode>(), new LinkedList<LinkedList<FlowNode>>(),
				new LinkedList<LinkedList<FlowNode>>());
	}

	private void mapAndCompute() {
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

		if (modelWithLanes) {
			this.storeLanePerTask();
		} else {
			this.addParticipantToTask();
		}

		this.mapDataObjects();

		this.computeGlobalSphere();
		this.createDataObjectAsssociatons();
		this.mapDefaultTroubleShooter();
		this.setAllEffectivePathsForWriters();

		// set last writer lists for the brts
		for (BPMNBusinessRuleTask brt : this.businessRuleTaskList) {
			this.mapDecisions(brt);
			for (BPMNDataObject dataO : brt.getDataObjects()) {
				ArrayList<BPMNTask> lastWritersForDataO = this.getLastWriterListForDataObject(brt, dataO,
						new ArrayList<BPMNTask>(), new LinkedList<BPMNElement>());
				for(BPMNTask t: lastWritersForDataO) {
				}
				brt.getLastWriterList().putIfAbsent(dataO, lastWritersForDataO);
			}
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
							// for(BPMNTask lastWriter2: entry2.getValue()) {
							if (entry2.getValue().contains(lastWriter)) {

								for (LinkedList<BPMNElement> l : lastWriter.getEffectivePaths().get(true)) {
									for (BPMNElement el : l) {
										if (el.equals(lastWriter)) {
											if ((!brt.getPotentiallyDependentBrts().contains(brt2))
													&& (!brt2.getPotentiallyDependentBrts().contains(brt))) {
												brt.getPotentiallyDependentBrts().add(brt2);
												brt2.getPotentiallyDependentBrts().add(brt);
											}
										}
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

	private void setAllEffectivePathsForWriters() {
		for (BPMNDataObject dataO : this.dataObjects) {
			for (BPMNElement writerTask : dataO.getWriters()) {
				HashMap<Boolean, LinkedList<LinkedList<BPMNElement>>> allEffectivePathsForWriter = this
						.allEffectivePathsForWriters(dataO, writerTask, writerTask, this.bpmnEnd,
								new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(),
								new LinkedList<BPMNElement>(), new LinkedList<LinkedList<BPMNElement>>());
				BPMNTask currentWriterTask = (BPMNTask) writerTask;
				currentWriterTask.setEffectivePaths(allEffectivePathsForWriter);

				// loop through the effective Paths and add all readers to the effective readers
				// of the writer
				LinkedList<LinkedList<BPMNElement>> effectivePaths = ((BPMNTask) writerTask).getEffectivePaths()
						.get(true);
				for (LinkedList<BPMNElement> effectivePathList : effectivePaths) {
					for (BPMNElement currEl : effectivePathList) {
						if (currEl instanceof BPMNTask) {
							BPMNTask currReaderTask = (BPMNTask) currEl;
							if (dataO.getReaders().contains(currReaderTask)
									&& currReaderTask.getDataObjects().contains(dataO) && (!(currReaderTask
											.getParticipant().equals(currentWriterTask.getParticipant())))) {
								if (!currentWriterTask.getEffectiveReaders().contains(currReaderTask)) {
									currentWriterTask.getEffectiveReaders().add(currReaderTask);
								}
							}
						}
					}
				}

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

	private void setRequiredUpdatesForArc(VoterForXorArc arcToBeAdded, ProcessInstanceWithVoters currInst ,
			LinkedList<LinkedList<BPMNElement>> paths) throws NullPointerException, InterruptedException, Exception {

		// get all the participants of the current process instance and check which
		// updates
		// would be necessary for arcToBeAdded
		HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosenVoters = currInst.getVotersMap();
		BPMNBusinessRuleTask brtToBeAdded = arcToBeAdded.getBrt();
		HashMap<BPMNDataObject, LinkedList<BPMNParticipant>> addToStaticSphereMap = new HashMap<BPMNDataObject, LinkedList<BPMNParticipant>>();
			for(RequiredUpdate reqUpdate: currInst.getListOfRequiredUpdates()) {
				if(Thread.currentThread().isInterrupted()) {
					System.err.println("Interrupted! " +Thread.currentThread().getName());
					throw new InterruptedException();
				}
				
				//add the participants that have already been chosen as voters to the static sphere of the dataObjects if they are on the path of voterForXorArc
				//if they are not already part of it
				BPMNBusinessRuleTask currBrt = reqUpdate.getCurrentBrt();
				if(brtToBeAdded.getLabels().containsAll(currBrt.getLabels())||brtToBeAdded.getLabels().isEmpty()||currBrt.getLabels().isEmpty()){
					BPMNDataObject dataObject = reqUpdate.getDataO();				
					BPMNParticipant part =  reqUpdate.getCurrentParticipant();
					if(!dataObject.getStaticSphere().contains(part)) {
						dataObject.getStaticSphere().add(part);		
						addToStaticSphereMap.computeIfAbsent(dataObject,value->new LinkedList<BPMNParticipant>()).add(part);
					}
				}
				
			
			}
			
			
		
		
		
		if (arcToBeAdded.getBrt() instanceof BPMNBusinessRuleTask) {
			BPMNBusinessRuleTask currentBrt = (BPMNBusinessRuleTask) arcToBeAdded.getBrt();

			HashMap<BPMNDataObject, ArrayList<BPMNTask>> lastWriters = currentBrt.getLastWriterList();
			for (Entry<BPMNDataObject, ArrayList<BPMNTask>> entry : lastWriters.entrySet()) {
				for (BPMNTask lastWriter : entry.getValue()) {
					BPMNDataObject dataO = entry.getKey();
					String requiredSphere = lastWriter.getSphereAnnotation().get(dataO);
					HashMap<BPMNParticipant, LinkedList<String>> sphereMap = new HashMap<BPMNParticipant, LinkedList<String>>();
					// set the WD and SD sphere and consider the preceeding already chosen voters
					// substitute the respective brts participant with the voters
					ArrayList<BPMNParticipant> wdList = new ArrayList<BPMNParticipant>();
					ArrayList<BPMNParticipant> sdList = new ArrayList<BPMNParticipant>();

					for (BPMNParticipant readerParticipant : arcToBeAdded.getChosenCombinationOfParticipants()) {
						// List will contain 1 or 2 entries, 1. sphereForReaderBeforeBrt 2.(if possible)
						// - sphereForReaderAfterBrt
						LinkedList<String> sphereList = new LinkedList<String>();

						// get sphere for reader between lastWriter and currentBrt
						String sphereForReaderBeforeBrt = this
								.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(currentBrt, lastWriter,
										dataO, readerParticipant, alreadyChosenVoters);
						sphereList.add(sphereForReaderBeforeBrt);
						String sphereForReader = sphereForReaderBeforeBrt;
						// if found sphere for reader does not match the required sphere
						// e.g. if writer demands WD or SD and the sphereForReader is static
						// search can be extended
						// since the reader reads the data somewhere in the process (may be after the
						// xor-split and potentially WD or SD)
						String requiredSphereOfWriter = ((BPMNTask) lastWriter).getSphereAnnotation().get(dataO);

						if ((requiredSphereOfWriter.equals("Strong-Dynamic")
								|| requiredSphereOfWriter.equals("Weak-Dynamic"))
								&& sphereForReaderBeforeBrt.contentEquals("Static")) {
							if (this.atLeastInSphere(sphereForReaderBeforeBrt, requiredSphereOfWriter) == false) {
								String sphereForReaderAfterBrt = this
										.getSphereForParticipantOnEffectivePathsAfterCurrentBrtWithAlreadyChosenVoters(
												currentBrt, lastWriter, dataO, readerParticipant, alreadyChosenVoters,
												sphereForReaderBeforeBrt, paths);
								// check if the sphere for reader after brt has been "upgraded" e.g. from static
								// to WD
								if (this.atLeastInSphere(sphereForReaderAfterBrt, sphereForReaderBeforeBrt)) {
									if (!sphereForReaderAfterBrt.contentEquals(sphereForReaderBeforeBrt)) {
										// take the upgraded sphere as a required Update
										// mark that a reader fulfills required sphere only after the brt and not on the
										// path from lastWriter to it
										sphereList.add(sphereForReaderAfterBrt);
										sphereForReader = sphereForReaderAfterBrt;
									}
								}
							}
						}

						if (sphereForReader.contentEquals("Strong-Dynamic")) {
							if (!sdList.contains(readerParticipant)) {
								sdList.add(readerParticipant);
							}
						} else if (sphereForReader.contentEquals("Weak-Dynamic")) {
							if (!wdList.contains(readerParticipant)) {
								wdList.add(readerParticipant);
							}
						}

						sphereMap.putIfAbsent(readerParticipant, sphereList);
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

						String update = "";
						double cost = 0;
						double weight = 1;
						// to calculate the weight we have to check on how many paths the lastWriter
						// writes data that is read by the brt

						if (requiredSphere.contentEquals("Strong-Dynamic")) {

							if (lastWriter.getStrongDynamicHashMap().containsKey(dataO)) {
								if (lastWriter.getStrongDynamicHashMap().get(dataO).contains(reader)) {
									// reader is on each effective path from lastwriter to brt and also in sd sphere
									// of the process at the point of the lastwriter
									// no update needed

								} else if (lastWriter.getWeakDynamicHashMap().get(dataO).contains(reader)) {
									// update from WD to SD
									update = "wdToSD";
									cost = this.costForLiftingFromWeakDynamicToStrongDynamic;

								} else if (dataO.getStaticSphere().contains(reader)) {
									// update from static to SD
									update = "staticToSD";
									cost = this.costForLiftingFromStaticToWeakDynamic
											+ this.costForLiftingFromWeakDynamicToStrongDynamic;

								} else if (this.globalSphere.contains(reader)) {
									// update from global to SD
									update = "globalToSD";
									cost = this.costForLiftingFromGlobalToStatic
											+ this.costForLiftingFromStaticToWeakDynamic
											+ this.costForLiftingFromWeakDynamicToStrongDynamic;
								}

							}
						} else if (requiredSphere.contentEquals("Weak-Dynamic")) {
							if (lastWriter.getWeakDynamicHashMap().containsKey(dataO)) {

								if (lastWriter.getStrongDynamicHashMap().get(dataO).contains(reader)
										|| lastWriter.getWeakDynamicHashMap().get(dataO).contains(reader)) {
									// no update needed

								} else if (dataO.getStaticSphere().contains(reader)) {
									// update from static to WD
									update = "staticToWD";
									cost = this.costForLiftingFromStaticToWeakDynamic;

								} else if (this.globalSphere.contains(reader)) {
									update = "globalToWD";
									cost = this.costForLiftingFromGlobalToStatic
											+ this.costForLiftingFromStaticToWeakDynamic;
								}

							}
						} else if (requiredSphere.contentEquals("Static")) {
							if (lastWriter.getStrongDynamicHashMap().get(dataO).contains(reader)
									|| lastWriter.getWeakDynamicHashMap().get(dataO).contains(reader)
									|| dataO.getStaticSphere().contains(reader)) {
								// no update needed

							} else if (this.globalSphere.contains(reader)) {
								update = "globalToStatic";
								cost = this.costForLiftingFromGlobalToStatic;
							}

						} else if (requiredSphere.contentEquals("Global")) {
							if (lastWriter.getStrongDynamicHashMap().get(dataO).contains(reader)
									|| lastWriter.getWeakDynamicHashMap().get(dataO).contains(reader)
									|| dataO.getStaticSphere().contains(reader) || this.globalSphere.contains(reader)) {
								// no update needed
							}

							// update from public to global!
						}

						if (update != "") {
							LinkedList<String> spheres = sphereMap.get(reader);

							RequiredUpdate reqUpdate = new RequiredUpdate(lastWriter, dataO, currentBrt,
									alreadyChosenVoters, reader, spheres, update, weight, cost);
							double weighting = this
									.calculateWeightingForLastWriter(lastWriter, this.bpmnStart, dataO, currentBrt);
							reqUpdate.setWeightingOfLastWriterToWriteDataForBrt(weighting);
							//reqUpdate.setWeightingOfLastWriterToWriteDataForBrt(1);
							currInst.getListOfRequiredUpdates().add(reqUpdate);

							// add additional cost for adding a reader as voter who reads data after the brt

							if (spheres.size() == 2) {
								if (spheres.get(1) != null) {
									cost += this.costForAddingReaderAfterBrt;
								}
							}

							double currInstCost = currInst.getCostForModelInstance();
							// cost will be multiplied with the weighting of the last writer and the
							// weighting of the brt
							double weightingOfCurrentBrt = 1;
							if (!currentBrt.getLabels().isEmpty()) {
								// since process is block structured -> there are 2 paths for each xor-split
								weightingOfCurrentBrt = weightingOfCurrentBrt / (currentBrt.getLabels().size() * 2);
							}

							currInstCost += (cost * reqUpdate.getWeightingOfLastWriterToWriteDataForBrt()
									* weightingOfCurrentBrt);
							currInst.setCostForModelInstance(currInstCost);

						}
					}
						
					if(Thread.currentThread().isInterrupted()) {
						System.err.println("Interrupted! " +Thread.currentThread().getName());
						throw new InterruptedException();
					}
					
				}

			}
		}
		
		
		//remove the added participants from the static spheres again
		for(Entry<BPMNDataObject, LinkedList<BPMNParticipant>>entry: addToStaticSphereMap.entrySet()) {
			BPMNDataObject currDataO = entry.getKey();
			Iterator<BPMNParticipant>partIter = entry.getValue().iterator();
			while(partIter.hasNext()) {
			BPMNParticipant part = partIter.next();
				currDataO.getStaticSphere().remove(part);
			}
			
			
		}
		
		
		

	}
	
	public synchronized LinkedList<ProcessInstanceWithVoters> localMinimumAlgorithmWithLimit(int upperBoundSolutionsPerIteration)
			throws NullPointerException, InterruptedException, Exception {
		// at each businessRuleTask in front of a xor-split:
		// generate all possible combinations of voters - calculate cost and only take
		// cheapest one(s)
		
		if (!this.executionTimeMap.isEmpty()) {
			// delete all arcs for brts
			for (BPMNBusinessRuleTask brt : this.businessRuleTaskList) {
				brt.getCombinations().clear();
				brt.getVoterArcs().clear();
			}

		}
		System.out.println("Local Minimum Algorithm with Limit "
				+ upperBoundSolutionsPerIteration+" generating all cheapest process instances: ");
		long startTime = System.nanoTime();
		LinkedList<ProcessInstanceWithVoters> cheapestCombinationsWithLimit = this.goDFSthroughProcessBuildArcsAndGetVoters(true, upperBoundSolutionsPerIteration,
				this.bpmnStart, this.bpmnEnd, null, new LinkedList<ProcessInstanceWithVoters>(),
				new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>(),
				new HashMap<BPMNBusinessRuleTask, LinkedList<RequiredUpdate>>(), new LinkedList<VoterForXorArc>(),
				new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(),
				new LinkedList<BPMNElement>(), new LinkedList<LinkedList<BPMNElement>>());
		long stopTime = System.nanoTime();
		long executionTime = stopTime - startTime;
		double executionTimelocalMinAlgorithmWithLimitInSeconds = (double) executionTime / 1000000000;
		String key = "localMinWithLimit"+upperBoundSolutionsPerIteration;
		this.executionTimeMap.put(key, executionTimelocalMinAlgorithmWithLimitInSeconds);

		
		return cheapestCombinationsWithLimit;
		
	}
	
	

	public synchronized LinkedList<ProcessInstanceWithVoters> localMinimumAlgorithm()
			throws NullPointerException, InterruptedException, Exception {
		// at each businessRuleTask in front of a xor-split:
		// generate all possible combinations of voters - calculate cost and only take
		// cheapest one(s)
		
		if (!this.executionTimeMap.isEmpty()) {
			// delete all arcs for brts
			for (BPMNBusinessRuleTask brt : this.businessRuleTaskList) {
				brt.getCombinations().clear();
				brt.getVoterArcs().clear();
			}

		}
		System.out.println("Local Minimum Algorithm generating all cheapest process instances: ");
		long startTime = System.nanoTime();
		LinkedList<ProcessInstanceWithVoters> cheapestCombinations = this.goDFSthroughProcessBuildArcsAndGetVoters(true,0,
				this.bpmnStart, this.bpmnEnd, null, new LinkedList<ProcessInstanceWithVoters>(),
				new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>(),
				new HashMap<BPMNBusinessRuleTask, LinkedList<RequiredUpdate>>(), new LinkedList<VoterForXorArc>(),
				new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(),
				new LinkedList<BPMNElement>(), new LinkedList<LinkedList<BPMNElement>>());
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
		System.out.println("Brute Force generating all possible process instances: ");
		if (!this.executionTimeMap.isEmpty()) {
			// delete all arcs for brts
			for (BPMNBusinessRuleTask brt : this.businessRuleTaskList) {
				brt.getCombinations().clear();
				brt.getVoterArcs().clear();
			}

		}

		long startTime = System.nanoTime();
		LinkedList<ProcessInstanceWithVoters> pInstances = this.goDFSthroughProcessBuildArcsAndGetVoters(false,0,
				this.bpmnStart, this.bpmnEnd, null, new LinkedList<ProcessInstanceWithVoters>(),
				new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>(),
				new HashMap<BPMNBusinessRuleTask, LinkedList<RequiredUpdate>>(), new LinkedList<VoterForXorArc>(),
				new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(),
				new LinkedList<BPMNElement>(), new LinkedList<LinkedList<BPMNElement>>());
		long stopTime = System.nanoTime();
		long executionTime = stopTime - startTime;
		double executionTimeBruteForceAlgorithmInSeconds = (double) executionTime / 1000000000;
		this.executionTimeMap.put("bruteForce", executionTimeBruteForceAlgorithmInSeconds);
		return pInstances;
		
	}

	private double calculateWeightingForLastWriter(BPMNTask currentLastWriter, BPMNElement start, BPMNDataObject dataO,
			BPMNBusinessRuleTask brt) throws NullPointerException, Exception {

		double foundCurrentLastWriterCount = 0;
		LinkedList<LinkedList<BPMNElement>> paths = this.getPathsWithMappedNodesFromCamundaNodes(CommonFunctionality
				.getAllPathsBetweenNodes(this.modelInstance, this.modelInstance.getModelElementById(start.getId()),
						this.modelInstance.getModelElementById(brt.getId()), new LinkedList<FlowNode>(),
						new LinkedList<FlowNode>(), new LinkedList<FlowNode>(), new LinkedList<LinkedList<FlowNode>>(),
						new LinkedList<LinkedList<FlowNode>>()));

		
		
		for (LinkedList<BPMNElement> pathList : paths) {

			for (int i = pathList.size() - 1; i > 0; i--) {
				if (pathList.get(i).equals(currentLastWriter)) {
					// go backwards through the list of paths between start and brt
					// if lastWriter is found in the list increase the count
					foundCurrentLastWriterCount++;
					i = 0;
				}

				if (pathList.get(i) instanceof BPMNTask) {
					BPMNTask currTask = (BPMNTask) pathList.get(i);
					if (dataO.getWriters().contains(currTask) && !currTask.equals(currentLastWriter)) {
						i = 0;
					}
				}
			}

		}
		
		
		double amount = (double) Math.round(foundCurrentLastWriterCount / paths.size() * 100) / 100;

		return amount;

	}

	private VoterForXorArc arcAlreadyGenerated(BPMNBusinessRuleTask brt, VoterForXorArc arc) {
		for (VoterForXorArc a : brt.getVoterArcs()) {
			if (a.getBrt().equals(arc.getBrt())) {
				if (a.getXorSplit().equals(arc.getXorSplit())) {

					if (a.getChosenCombinationOfParticipants().equals(arc.getChosenCombinationOfParticipants())) {
						return arc;

					}
				}
			}

		}
		return null;
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

	public void addCostsForSqhereRequirements(BPMNBusinessRuleTask bpmnBrt, LinkedList<BPMNParticipant> participants) {

		// Search for lastWriters of the connected data objects
		for (BPMNDataObject dataO : bpmnBrt.getDataObjects()) {
			ArrayList<BPMNTask> lastWriterList = this.getLastWriterListForDataObject(bpmnBrt, dataO,
					new ArrayList<BPMNTask>(), new LinkedList<BPMNElement>());
			// now check if participants are in the required sphere of the reader at the
			// position of the brt
			for (BPMNTask writerTask : lastWriterList) {
				LinkedList<LinkedList<BPMNElement>> paths = CommonFunctionality.allPathsBetweenNodesWithMappedNodes(
						writerTask, bpmnBrt, new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(),
						new LinkedList<BPMNElement>(), new LinkedList<LinkedList<BPMNElement>>());
				String sphere = writerTask.getSphereAnnotation().get(dataO);
				for (BPMNParticipant participant : participants) {

					this.checkSphereRequirement(writerTask, dataO, sphere, participant, paths);

					/*
					 * for(LinkedList<BPMNElement>path: paths) { this.readerIsOnPath(reader,
					 * writerTask, dataO, path); }
					 */

				}

			}

		}

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
			for (Lane l : modelInstance.getModelElementsByType(Lane.class)) {
				BPMNParticipant lanePart = new BPMNParticipant(l.getId(), l.getName().trim());
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
			for (Task task : modelInstance.getModelElementsByType(Task.class)) {
				for (BPMNElement t : this.processElements) {
					if (t instanceof BPMNTask && task.getId().equals(t.getId())) {
						String participantName = task.getName().substring(task.getName().indexOf("["),
								task.getName().indexOf("]") + 1);
						BPMNParticipant participant = new BPMNParticipant(participantName, participantName);
						((BPMNTask) t).setParticipant(participant);
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
		for (TextAnnotation text : modelInstance.getModelElementsByType(TextAnnotation.class)) {

			String str = text.getTextContent();
			String troubleshooter = "";

			if (str.toLowerCase().contains(("Default").toLowerCase())
					&& str.toLowerCase().contains(("[Troubleshooter]").toLowerCase())) {
				troubleshooter = str.substring(str.indexOf('{') + 1, str.indexOf('}')).trim();
				if (globalSphere.isEmpty()) {
					this.computeGlobalSphere();
				}
				for (BPMNParticipant part : this.globalSphere) {
					if (part.getName().contentEquals(troubleshooter)) {
						this.troubleShooter = part;
						break;
					}
				}

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
					if (bpmndo.getName().substring(1, 3).equals(dataObject) && bpmndo.getWriters().contains(element)) {
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
						if (bpmndo.getName().substring(1, 3).equals(s)) {
							element.getSphereAnnotation().put(bpmndo, s2);
						}

					}
				}
			}

		}

	}

	// map the annotated amount of needed voters to the BPMNExclusiveGateways
	// map the sphere connected to the gateway
	private void mapAnnotations(BPMNExclusiveGateway gtw) {
		for (TextAnnotation text : modelInstance.getModelElementsByType(TextAnnotation.class)) {
			for (Association a : modelInstance.getModelElementsByType(Association.class)) {
				if (a.getAttributeValue("sourceRef").equals(gtw.getId())
						&& a.getAttributeValue("targetRef").equals(text.getId())) {
					String str = text.getTextContent();
					if (str.contains("[Voters]")) {
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
					} else if (str.contains("[Sphere]")) {
						String subStr = str.substring(str.indexOf('{') + 1, str.indexOf('}'));
						gtw.setSphere(subStr);
					} else if (str.contains("[ExcludeParticipantConstraint]")) {
							String partName = str.substring(str.indexOf('{') + 1, str.indexOf('}'));
							
							for(BPMNParticipant p: this.globalSphere) {
								if(p.getNameWithoutBrackets().contentEquals(partName)) {
									ExcludeParticipantConstraint epc = new ExcludeParticipantConstraint(p);
									gtw.getConstraints().add(epc);
									break;
								}
								
								
							}
							
							

						

					}

				}
			}
		}

	}

	// DataObjects can be attached to Tasks, BusinessRuleTasks and UserTasks in
	// Camunda
	public void createDataObjectAsssociatons() {
		for (BPMNDataObject bpmno : this.dataObjects) {
			for (Task t : modelInstance.getModelElementsByType(Task.class)) {
				for (BPMNElement e : this.processElements) {
					for (DataOutputAssociation doa : t.getDataOutputAssociations()) {
						if (doa.getTarget().getAttributeValue("dataObjectRef").equals(bpmno.getId())) {
							if (e instanceof BPMNTask && e.getId().equals(t.getId())) {
								bpmno.addWriterToDataObject((BPMNTask) e);
								// if a participant writes to a dataObject he is also added as a reader
								bpmno.addReaderToDataObject((BPMNTask) e);

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

			for (ExclusiveGateway xor : this.modelInstance.getModelElementsByType(ExclusiveGateway.class)) {
				this.mapAnnotations((BPMNExclusiveGateway) this.getNodeById(xor.getId()));
			}

			for (BusinessRuleTask brt : this.modelInstance.getModelElementsByType(BusinessRuleTask.class)) {
				this.mapDecisions((BPMNBusinessRuleTask) this.getNodeById(brt.getId()));
			}
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

	// Uses Breadth first search to go through the predecessors of a node to find
	// the last Writer to that dataObject
	public ArrayList<BPMNTask> getLastWriterListForDataObject(BPMNBusinessRuleTask brt, BPMNDataObject data,
			ArrayList<BPMNTask> lastWriterList, LinkedList<BPMNElement> queue) {
		queue.addAll(brt.getPredecessorsSorted());
		while (!(queue.isEmpty())) {
			BPMNElement element = queue.poll();
			
			// if the element is a writer who has been already found, than skip it
			if (lastWriterList.contains(element)) {
				element = queue.poll();
				return lastWriterList;
			}
			if (element instanceof BPMNStartEvent && queue.isEmpty()) {
				return lastWriterList;
			}

			if (element instanceof BPMNTask) {
				// Check if the element is a Writer to the dataObject given as a parameter
				BPMNTask currentLastWriterCandidate = (BPMNTask) element;

				for (BPMNDataObject dataO : brt.getDataObjects()) {
					if (dataO.equals(data) && dataO.getWriters().contains(currentLastWriterCandidate)) {
						// if a writer is found who is already in the list - return the list
						
						if(!lastWriterList.contains(currentLastWriterCandidate)) {
						lastWriterList.add(currentLastWriterCandidate);						
						} 
						
						if(!currentLastWriterCandidate.hasLabel()) {
							return lastWriterList;
						} else {
							// check if last added writer is not on the same branch as the
							// currentLastWriterCandidate
							// if so, add it and return the list
							if (!lastWriterList.isEmpty()) {
								BPMNTask lastAddedWriterTask = lastWriterList.get(lastWriterList.size() - 1);
								if (this.sameDepthOtherBranch(lastAddedWriterTask, currentLastWriterCandidate)) {
									lastWriterList.add(currentLastWriterCandidate);
									return lastWriterList;
								}
							}


							// when the found writers labels don't match with the labels of the brt
							// check other paths for possible last writers too
							if (!currentLastWriterCandidate.getLabels().equals(brt.getLabels())) {
								if (currentLastWriterCandidate.hasLabel()) {
									this.getLastWriterListForDataObject(brt, data, lastWriterList, queue);
								}

							} else {
								// when they match the lastWriter is on the same path as the brt and no XOR is
								// in between
								return lastWriterList;
							}

						}

					}

				}
			}

			for (BPMNElement predecessor : element.getPredecessorsSorted()) {
				queue.add(predecessor);

			}
		}
		return lastWriterList;

	}

	private boolean sameDepthOtherBranch(BPMNElement firstEl, BPMNElement secondEl) {
		// check if firstEl and secondEl are in same nesting depth but other branch
		if (firstEl.getLabels().size() != secondEl.getLabels().size()) {
			return false;
		}
		if(firstEl.getLabels().isEmpty()&&secondEl.getLabels().isEmpty()) {
			return false;
		}
		
		ArrayList<Label>firstElLabelsWithoutLast = new ArrayList<Label>();
		for(int i = 0; i < firstEl.getLabels().size()-1; i++) {
			firstElLabelsWithoutLast.add(firstEl.getLabels().get(i));
		}
		
		ArrayList<Label>secondElLabelsWithoutLast = new ArrayList<Label>();
		for(int i = 0; i < secondEl.getLabels().size()-1; i++) {
			secondElLabelsWithoutLast.add(secondEl.getLabels().get(i));
		}

		if(firstElLabelsWithoutLast.equals(secondElLabelsWithoutLast)) {
			//last label must be in other branch
			Label firstElLastLabel = firstEl.getLabels().get(firstEl.getLabels().size()-1);
			Label secondElLastLabel  = secondEl.getLabels().get(secondEl.getLabels().size()-1);
			if(firstElLastLabel.getName().contentEquals(secondElLastLabel.getName())) {
				if(firstElLastLabel.getOutcome().contentEquals(secondElLastLabel.getOutcome())) {
					//same branch
					return false;
				} else {
					return true;
				}
				
			}
			
		}
		
		return true;
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

	public synchronized LinkedList<VoterForXorArc> generateArcsForXorSplitWithConstraint(BPMNBusinessRuleTask currBrt) {
		LinkedList<VoterForXorArc> brtCombs = new LinkedList<VoterForXorArc>();

		if (currBrt.getSuccessors().iterator().next() instanceof BPMNExclusiveGateway) {
			BPMNExclusiveGateway bpmnEx = (BPMNExclusiveGateway) currBrt.getSuccessors().iterator().next();
			// get all the possible combinations of participants for the brt
			// consider the constraints e.g. participants may be excluded
			LinkedList<BPMNParticipant>participantsToCombine = new LinkedList<BPMNParticipant>();
			participantsToCombine.addAll(this.globalSphere);
			
			//exclude the participants due to constraints
			for(Constraint constraint: bpmnEx.getConstraints()) {
				if(constraint instanceof ExcludeParticipantConstraint) {
					BPMNParticipant partToRemove = ((ExcludeParticipantConstraint)constraint).getParticipantToExclude();
					participantsToCombine.remove(partToRemove);					
				}
				
			}
			
			
			
			if (currBrt.getCombinations().isEmpty()) {
				LinkedList<LinkedList<BPMNParticipant>> list = Combination.getPermutations(participantsToCombine,
						bpmnEx.getAmountVoters());
				currBrt.getCombinations().putIfAbsent(currBrt, list);
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

	public LinkedList<Object> generateArcsForXorSplitReturnAsObjectList(BPMNBusinessRuleTask currBrt) {
		LinkedList<Object> brtCombs = new LinkedList<Object>();

		if (currBrt.getSuccessors().iterator().next() instanceof BPMNExclusiveGateway) {
			BPMNExclusiveGateway bpmnEx = (BPMNExclusiveGateway) currBrt.getSuccessors().iterator().next();
			// get all the possible combinations of participants for the brt
			if (currBrt.getCombinations().isEmpty()) {
				LinkedList<LinkedList<BPMNParticipant>> list = Combination.getPermutations(this.globalSphere,
						bpmnEx.getAmountVoters());
				currBrt.getCombinations().putIfAbsent(currBrt, list);
			}

			for (LinkedList<BPMNParticipant> partList : currBrt.getCombinations().get(currBrt)) {

				VoterForXorArc arc = new VoterForXorArc(currBrt, bpmnEx, partList);

				// check if arc already exists
				VoterForXorArc arcAlreadyGenerated = this.arcAlreadyGenerated(currBrt, arc);
				if (arcAlreadyGenerated == null) {
					brtCombs.add(arc);
				} else {

					brtCombs.add(arcAlreadyGenerated);
					ArcWithCost.id--;
				}

			}

		}

		return brtCombs;

	}
	

	public LinkedList<ProcessInstanceWithVoters> goDFSthroughProcessBuildArcsAndGetVoters(boolean localMin, int bound, 
			BPMNElement startNode, BPMNElement endNode, BPMNBusinessRuleTask lastFoundBrt,
			LinkedList<ProcessInstanceWithVoters> processInstancesWithVoters,
			HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> votersMap,
			HashMap<BPMNBusinessRuleTask, LinkedList<RequiredUpdate>> requiredUpdates,
			LinkedList<VoterForXorArc> alreadyTakenVoters, LinkedList<BPMNElement> queue,
			LinkedList<BPMNElement> parallelGtwQueue, LinkedList<BPMNElement> openSplitStack,
			LinkedList<BPMNElement> currentPath, LinkedList<LinkedList<BPMNElement>> paths)
			throws NullPointerException, InterruptedException, Exception {
		// go DFS inside the XOR till corresponding join is found
		
		try {
		queue.add(startNode);
		
		while (!(queue.isEmpty())) {
			if(Thread.currentThread().isInterrupted()) {
				System.err.println("Interrupted! " +Thread.currentThread().getName());
				throw new InterruptedException();
			}
			
			
			if (lastFoundBrt == null) {
				for (int i = currentPath.size() - 1; i >= 0; i--) {
					BPMNElement el = currentPath.get(i);
					if (el instanceof BPMNBusinessRuleTask) {
						lastFoundBrt = (BPMNBusinessRuleTask) el;
					}
				}

			}

			BPMNElement element = queue.poll();
			currentPath.add(element);

			if (element.getId().equals(endNode.getId())) {

				paths.add(currentPath);

				if (endNode instanceof BPMNGateway && ((BPMNGateway) element).getPredecessors().size() >= 2) {

					BPMNGateway joinGtw = (BPMNGateway) element;

					// when a xor-join is found - poll the last opened xor gateway from the stack
					BPMNGateway lastOpenedSplit = (BPMNGateway) openSplitStack.pollLast();

					if (!openSplitStack.isEmpty()) {
						if (!openSplitStack.contains(lastOpenedSplit)) {
							// when the openSplitStack does not contain the lastOpenedSplit anymore, all
							// branches to the joinGtw have been visited
							// go from joinGtw to the Join of the last opened split in the stack
							this.goDFSthroughProcessBuildArcsAndGetVoters(localMin, bound, joinGtw,
									this.getCorrespondingGtw((BPMNGateway) openSplitStack.getLast()), lastFoundBrt,
									processInstancesWithVoters, votersMap, requiredUpdates, alreadyTakenVoters, queue,
									parallelGtwQueue, openSplitStack, currentPath, paths);

						}
					} else if (openSplitStack.isEmpty()) {
						// when there are no open Xor gtws
						// go from the successor of the element to bpmnEnd since the currentElement has
						// already been added to the path
						LinkedList<LinkedList<BPMNElement>> newPaths = new LinkedList<LinkedList<BPMNElement>>();

						for (LinkedList<BPMNElement> path : paths) {
							if (path.getLast().equals(element)) {
								LinkedList<BPMNElement> newPathAfterXorJoin = new LinkedList<BPMNElement>();
								newPathAfterXorJoin.addAll(path);
								newPaths.add(newPathAfterXorJoin);
							}
						}

						for (LinkedList<BPMNElement> newPath : newPaths) {
							this.goDFSthroughProcessBuildArcsAndGetVoters(localMin, bound,
									element.getSuccessors().iterator().next(), this.bpmnEnd, lastFoundBrt,
									processInstancesWithVoters, votersMap, requiredUpdates, alreadyTakenVoters, queue,
									parallelGtwQueue, openSplitStack, newPath, paths);
						}

					}

				}
				element = queue.poll();
				if (element == null && queue.isEmpty()) {
					return processInstancesWithVoters;
				}

			}

			if (element instanceof BPMNBusinessRuleTask) {
				BPMNBusinessRuleTask currBrt = (BPMNBusinessRuleTask) element;
				LinkedList<VoterForXorArc> arcsForCurrBrt = null;

				if (currBrt.getVoterArcs().isEmpty()) {
					// when brt is found and arcs havent been generated
					arcsForCurrBrt = generateArcsForXorSplitWithConstraint(currBrt);
					currBrt.setVoterArcs(arcsForCurrBrt);

					// check if there has been already a brt before
					if (lastFoundBrt == null) {
						for (VoterForXorArc voters : arcsForCurrBrt) {
							// generate a new possible processInstance
							ProcessInstanceWithVoters pInstance = new ProcessInstanceWithVoters();
							this.setRequiredUpdatesForArc(voters, pInstance, paths);

							pInstance.addVoterArc(voters);

							if (localMin) {
								//when bound == 0 -> get all cheapest instances
								
									this.insertIfCheapestWithBound(processInstancesWithVoters, pInstance, bound);
								
								
							} else {
								processInstancesWithVoters.add(pInstance);
							}
						}

					} else {
						// if there has already been a brt before the currBrt
						LinkedList<LinkedList<Object>> toCombine = new LinkedList<LinkedList<Object>>();
						LinkedList<ProcessInstanceWithVoters> newInstances = new LinkedList<ProcessInstanceWithVoters>();

						// need to combine currBrtArcs with each existing possible process instance
						LinkedList<Object> aK = new LinkedList<Object>();
						for (ProcessInstanceWithVoters existingInstance : processInstancesWithVoters) {
							aK.add(existingInstance);
						}

						toCombine.add(aK);

						LinkedList<Object> aL = new LinkedList<Object>();
						for (VoterForXorArc ar : arcsForCurrBrt) {
							aL.add(ar);
						}
						toCombine.add(aL);

						// list of all possible combinations of Voters for currBrt combined with all
						// existing process instances
						Collection<List<Object>> combs = Combination.permutations(toCombine);
						ProcessInstanceWithVoters.setProcessID(0);
												
						for (List list : combs) {
							if(Thread.currentThread().isInterrupted()) {
								System.err.println("Interrupted" +Thread.currentThread().getName());
								throw new InterruptedException();							}
							
							ProcessInstanceWithVoters newInstance = new ProcessInstanceWithVoters();
							ProcessInstanceWithVoters currInst = (ProcessInstanceWithVoters) list.get(0);
							VoterForXorArc currBrtCombArc = (VoterForXorArc) list.get(1);
							for (VoterForXorArc curr : currInst.getListOfArcs()) {
								VoterForXorArc newInstanceArc = new VoterForXorArc(curr.getBrt(), curr.getXorSplit(),
										curr.getChosenCombinationOfParticipants());
								newInstance.addVoterArc(newInstanceArc);
							}
							newInstance.getListOfRequiredUpdates().addAll(currInst.getListOfRequiredUpdates());
							newInstance.setCostForModelInstance(currInst.getCostForModelInstance());
							this.setRequiredUpdatesForArc(currBrtCombArc, newInstance, paths);

							newInstance.addVoterArc(currBrtCombArc);
							if (localMin) {
								//when bound == 0 -> get all cheapest instances
								//else e.g. bound == 3 -> get the first 3 cheapest instances
															
							this.insertIfCheapestWithBound(newInstances, newInstance, bound);
									
								
							} else {
								newInstances.add(newInstance);
							}

						}

						processInstancesWithVoters.clear();
						processInstancesWithVoters.addAll(newInstances);

					}

					lastFoundBrt = currBrt;
				}

			}

			if (element instanceof BPMNGateway && ((BPMNGateway) element).getSuccessors().size() >= 2) {
				// add the xor split to the openXorStack 1 times for each outgoing paths
				int amountOfOutgoingPaths = element.getSuccessors().size();
				int i = 0;
				while (i < amountOfOutgoingPaths) {
					openSplitStack.add((BPMNGateway) element);
					i++;
				}

			}

			for (BPMNElement successor : element.getSuccessors()) {

				if (element instanceof BPMNGateway && ((BPMNGateway) element).getSuccessors().size() >= 2) {
					// when a split is found - go dfs till the corresponding join is found

					BPMNGateway correspondingJoinGtw = this.getCorrespondingGtw((BPMNGateway) element);

					LinkedList<BPMNElement> newPath = new LinkedList<BPMNElement>();
					newPath.addAll(currentPath);

					this.goDFSthroughProcessBuildArcsAndGetVoters(localMin, bound, successor, correspondingJoinGtw,
							lastFoundBrt, processInstancesWithVoters, votersMap, requiredUpdates, alreadyTakenVoters,
							queue, parallelGtwQueue, openSplitStack, newPath, paths);
				} else {

					queue.add(successor);

				}

			}

		}
		
		return processInstancesWithVoters;
		} catch(Exception e) {
			throw new Exception("Exception in goDFS!");
		}

	}

	private void insertIfCheapestWithBound(LinkedList<ProcessInstanceWithVoters> processInstancesWithVoters,
			ProcessInstanceWithVoters currInstance, int bound) throws InterruptedException {
		if(Thread.currentThread().isInterrupted()) {
			System.err.println("Interrupted" +Thread.currentThread().getName());
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
				//only insert if bound not reached!
				if(bound==0) {
					processInstancesWithVoters.add(currInstance);
				} else {
				if(processInstancesWithVoters.size()<bound) {
				processInstancesWithVoters.add(currInstance);
				}
				}
			}
		}

	}

	public HashMap<Boolean, LinkedList<LinkedList<BPMNElement>>> getEffectivePathsBetweenWriterAndTargetElement(
			BPMNDataObject dataO, BPMNElement writerTask, BPMNElement targetElement, LinkedList<BPMNElement> stack,
			LinkedList<BPMNElement> gtwStack, LinkedList<BPMNElement> currentPath,
			LinkedList<LinkedList<BPMNElement>> paths) {
		// returns a hashmap with the keys true and false
		// where key = true: contains all effective Paths from writerTask to currentBrt
		// where key = false: contains all paths where another writer writes to same
		// dataO between the writerTask and the currentBrt

		LinkedList<LinkedList<BPMNElement>> allPathsBetweenWriterAndTargetElement = CommonFunctionality
				.allPathsBetweenNodesWithMappedNodes(writerTask, targetElement, stack, gtwStack, currentPath, paths);
		HashMap<Boolean, LinkedList<LinkedList<BPMNElement>>> pathMap = new HashMap<Boolean, LinkedList<LinkedList<BPMNElement>>>();
		LinkedList<LinkedList<BPMNElement>> effectivePaths = new LinkedList<LinkedList<BPMNElement>>();
		LinkedList<LinkedList<BPMNElement>> nonEffectivePaths = new LinkedList<LinkedList<BPMNElement>>();

		for (LinkedList<BPMNElement> pathInstance : allPathsBetweenWriterAndTargetElement) {
			boolean effective = true;
			for (BPMNElement el : pathInstance) {
				if (el instanceof BPMNTask) {
					BPMNTask currentTask = (BPMNTask) el;
					if (dataO.getWriters().contains(currentTask)
							&& (!(currentTask.getParticipant().equals(((BPMNTask) writerTask).getParticipant())))) {
						// another writer for dataO has been found on the path
						effective = false;
					}

				}

			}
			if (effective) {
				effectivePaths.add(pathInstance);
			} else if (effective == false) {
				nonEffectivePaths.add(pathInstance);
			}

		}

		pathMap.putIfAbsent(true, effectivePaths);
		pathMap.putIfAbsent(false, nonEffectivePaths);
		return pathMap;

	}

	
	
	public HashMap<Boolean, LinkedList<LinkedList<BPMNElement>>> allEffectivePathsForWriters(BPMNDataObject dataO,
			BPMNElement writerTask, BPMNElement startNode, BPMNElement endNode, LinkedList<BPMNElement> stack,
			LinkedList<BPMNElement> gtwStack, LinkedList<BPMNElement> currentPath,
			LinkedList<LinkedList<BPMNElement>> paths) {
		// returns a hashmap with the keys true and false
		// where key = true: contains all effective Paths from writer to endNode
		// where key = false: contains all paths where another writer writes to same
		// dataO
		// the participants on the path are set to be equal to the chosencombination
		// given as parameter
		LinkedList<LinkedList<BPMNElement>> allPathsBetweenWriterAndEndEvent = CommonFunctionality
				.allPathsBetweenNodesWithMappedNodes(startNode, endNode, stack, gtwStack, currentPath, paths);
		HashMap<Boolean, LinkedList<LinkedList<BPMNElement>>> pathMap = new HashMap<Boolean, LinkedList<LinkedList<BPMNElement>>>();
		LinkedList<LinkedList<BPMNElement>> effectivePaths = new LinkedList<LinkedList<BPMNElement>>();
		LinkedList<LinkedList<BPMNElement>> nonEffectivePaths = new LinkedList<LinkedList<BPMNElement>>();

		for (LinkedList<BPMNElement> pathInstance : allPathsBetweenWriterAndEndEvent) {
			boolean effective = true;
			for (BPMNElement el : pathInstance) {
				if (el instanceof BPMNTask) {
					BPMNTask currentTask = (BPMNTask) el;
					if (dataO.getWriters().contains(currentTask)
							&& (!(currentTask.getParticipant().equals(((BPMNTask) writerTask).getParticipant())))) {
						// another writer for dataO has been found on the path
						effective = false;
					}

				}

			}
			if (effective) {
				effectivePaths.add(pathInstance);
			} else if (effective == false) {
				nonEffectivePaths.add(pathInstance);
			}

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

	public void printElementPredecessorAndSuccessor() {
		this.processElements.forEach(f -> {
			f.printPredecessors();
		});
		this.processElements.forEach(f -> {
			f.printSuccessors();
		});

	}

	/*
	 * public void addVotingTasksToProcess( HashMap<BPMNBusinessRuleTask,
	 * HashMap<BPMNDataObject, ArrayList<BPMNTask>>> votersMap,
	 * HashMap<BPMNBusinessRuleTask, BPMNParticipant> finalDeciderMap, boolean
	 * mapModelBtn) throws IOException {
	 * 
	 * int i = 1; Iterator<BPMNBusinessRuleTask> bpmnBusinessRtIterator =
	 * votersMap.keySet().iterator();
	 * 
	 * while (bpmnBusinessRtIterator.hasNext()) { BPMNBusinessRuleTask
	 * bpmnBusinessRt = bpmnBusinessRtIterator.next(); BusinessRuleTask businessRt =
	 * (BusinessRuleTask) this.getFlowNodeByBPMNNodeId(bpmnBusinessRt.getId());
	 * HashMap<BPMNDataObject, ArrayList<BPMNTask>> votersMapInner =
	 * votersMap.get(bpmnBusinessRt); BPMNExclusiveGateway bpmnEx =
	 * (BPMNExclusiveGateway) bpmnBusinessRt.getSuccessors().iterator().next();
	 * 
	 * // builder doesn't work on task (need to be subclasses of task!) // convert
	 * the task to a user task to prevent error BPMNElement predecessorOfBpmnBrt =
	 * bpmnBusinessRt.getPredecessors().iterator().next(); if
	 * (predecessorOfBpmnBrt.getClass() == Mapping.BPMNTask.class) { Task predec =
	 * (Task) this.getFlowNodeByBPMNNodeId(predecessorOfBpmnBrt.getId()); UserTask
	 * userTask = modelInstance.newInstance(UserTask.class);
	 * userTask.setId(predec.getId()); userTask.setName(predec.getName());
	 * userTask.getDataInputAssociations().addAll(predec.getDataInputAssociations())
	 * ;
	 * userTask.getDataOutputAssociations().addAll(predec.getDataOutputAssociations(
	 * )); predec.replaceWithElement(userTask); }
	 * 
	 * // check if there is only one participant selected for each data object of
	 * the // voting boolean onlyOneTask = true; BPMNTask currentTask =
	 * votersMapInner.values().iterator().next().iterator().next(); for
	 * (ArrayList<BPMNTask> taskList : votersMapInner.values()) { if
	 * (!(taskList.size() == 1 && taskList.contains(currentTask))) { onlyOneTask =
	 * false; } }
	 * 
	 * // Voting system inside of a subprocess if (!mapModelBtn) {
	 * 
	 * if (!(onlyOneTask)) { BPMNParallelGateway.increaseVotingTaskCount();
	 * this.addTasksToVotingSystem(i, businessRt, bpmnEx,
	 * this.getFlowNodeByBPMNNodeId(bpmnBusinessRt.getPredecessors().iterator().next
	 * ().getId()) .builder().subProcess().embeddedSubProcess().startEvent(), "PV" +
	 * BPMNParallelGateway.getVotingTaskCount(), votersMapInner, finalDeciderMap,
	 * "PV" + BPMNParallelGateway.getVotingTaskCount(),
	 * BPMNExclusiveGateway.increaseExclusiveGtwCount(), mapModelBtn, onlyOneTask);
	 * } else { this.addTasksToVotingSystem(i, businessRt, bpmnEx,
	 * businessRt.builder().moveToNode(bpmnBusinessRt.getPredecessors().iterator().
	 * next().getId()), "PV" + BPMNParallelGateway.getVotingTaskCount(),
	 * votersMapInner, finalDeciderMap, "PV" +
	 * BPMNParallelGateway.getVotingTaskCount(), 0, mapModelBtn, onlyOneTask); } }
	 * 
	 * else { // Voting without having a subprocess if (!onlyOneTask) {
	 * BPMNParallelGateway.increaseVotingTaskCount();
	 * BPMNExclusiveGateway.increaseExclusiveGtwCount(); }
	 * this.addTasksToVotingSystem(i, businessRt, bpmnEx,
	 * businessRt.builder().moveToNode(bpmnBusinessRt.getPredecessors().iterator().
	 * next().getId()), "PV" + BPMNParallelGateway.getVotingTaskCount(),
	 * votersMapInner, finalDeciderMap, "PV" +
	 * BPMNParallelGateway.getVotingTaskCount(),
	 * BPMNExclusiveGateway.getExclusiveGtwCount(), mapModelBtn, onlyOneTask);
	 * 
	 * }
	 * 
	 * // Add the new tasks generated via fluent builder API to the corresponding
	 * lanes // in the xml model // Cant be done with the fluent model builder
	 * directly! for (Lane l : modelInstance.getModelElementsByType(Lane.class)) {
	 * for (Task task : modelInstance.getModelElementsByType(UserTask.class)) { if
	 * (l.getName().equals( task.getName().substring(task.getName().indexOf(" ") +
	 * 1, task.getName().length()))) { // Add necessary information to the voting
	 * tasks
	 * 
	 * if (mapModelBtn && task.getDocumentations().isEmpty()) { Documentation doc =
	 * modelInstance.newInstance(Documentation.class); StringBuilder sb = new
	 * StringBuilder();
	 * 
	 * // add the decision of the businessruletask to the element documentation of
	 * the // voting tasks // use the Jackson converter to convert java object into
	 * json format
	 * 
	 * ObjectMapper mapper = new ObjectMapper(); // Convert object to JSON string
	 * String jsonInString =
	 * mapper.writeValueAsString(bpmnBusinessRt.getDecisionEvaluation());
	 * sb.append(jsonInString); // add a false rate for the voting tasks if
	 * (task.getName().startsWith("VotingTask")) { sb.deleteCharAt(sb.length() - 1);
	 * sb.append(",\"falseRate\":\"" + bpmnBusinessRt.getFalseRate() + "\"}");
	 * 
	 * } doc.setTextContent(sb.toString()); task.getDocumentations().add(doc);
	 * 
	 * }
	 * 
	 * // Put the voting tasks to the corresponding lanes in the xml model
	 * FlowNodeRef ref = modelInstance.newInstance(FlowNodeRef.class);
	 * ref.setTextContent(task.getId()); FlowNode n =
	 * this.getFlowNodeByBPMNNodeId(task.getId()); if
	 * (!l.getFlowNodeRefs().contains(n)) { l.addChildElement(ref); }
	 * 
	 * } }
	 * 
	 * } i++;
	 * 
	 * }
	 * 
	 * for (BPMNBusinessRuleTask brt : votersMap.keySet()) { BusinessRuleTask b =
	 * (BusinessRuleTask) this.getFlowNodeByBPMNNodeId(brt.getId());
	 * this.removeBusinessRuleTask(b); } try { if (mapModelBtn) { this.mapModel(); }
	 * this.writeChangesToFile("votingAsBpmnElements"); } catch
	 * (ParserConfigurationException e) { // TODO Auto-generated catch block
	 * e.printStackTrace(); } catch (SAXException e) { // TODO Auto-generated catch
	 * block e.printStackTrace(); }
	 * 
	 * }
	 */

	/*
	 * private void addTasksToVotingSystem(int i, BusinessRuleTask brt,
	 * BPMNExclusiveGateway bpmnEx, AbstractFlowNodeBuilder builder, String
	 * parallelSplit, HashMap<BPMNDataObject, ArrayList<BPMNTask>> votersMap,
	 * HashMap<BPMNBusinessRuleTask, BPMNParticipant> finalDeciderMap, String
	 * parallelJoin, int exclusiveGtwCount, boolean mapModelBtn, boolean
	 * onlyOneTask) { if (votersMap.isEmpty()) {
	 * System.err.println("No voters selected");
	 * 
	 * }
	 * 
	 * // SequenceFlow connecting businessruletask and xor gtw SequenceFlow s =
	 * brt.getOutgoing().iterator().next(); FlowNode targetGtw = s.getTarget();
	 * 
	 * String exclusiveGatewayDeciderSplitId = ""; String
	 * exclusiveGatewayDeciderJoinId = ""; String exclusiveGatewayDeciderName = "";
	 * 
	 * Iterator<Entry<BPMNDataObject, ArrayList<BPMNTask>>> iter =
	 * votersMap.entrySet().iterator(); ArrayList<Task> alreadyModelled = new
	 * ArrayList<Task>(); Set<BPMNDataObject> allBPMNDataObjects = new
	 * HashSet<BPMNDataObject>();
	 * 
	 * allBPMNDataObjects.addAll(((BPMNBusinessRuleTask)
	 * this.getNodeById(brt.getId())).getDataObjects()); String parallelSplitId =
	 * parallelSplit + "split"; String parallelJoinId = parallelJoin + "join";
	 * boolean isSet = false;
	 * 
	 * // if there is only one user, than simply add one voting task without
	 * parallel // and xor splits if (onlyOneTask) { int votingTaskId =
	 * BPMNTask.increaseVotingTaskId(); BPMNDataObject key = iter.next().getKey();
	 * ArrayList<BPMNTask> nextList = votersMap.get(key); Iterator<BPMNTask>
	 * nextListIter = nextList.iterator(); BPMNParticipant nextParticipant =
	 * nextListIter.next().getParticipant();
	 * 
	 * String serviceTaskId = "serviceTask_CollectVotes" + i;
	 * builder.userTask("Task_votingTask" + votingTaskId).name("VotingTask " +
	 * nextParticipant.getName())
	 * .serviceTask(serviceTaskId).name("Collect Votes").connectTo(targetGtw.getId()
	 * );
	 * 
	 * ServiceTask st = (ServiceTask) this.getFlowNodeByBPMNNodeId(serviceTaskId);
	 * st.getOutgoing().iterator().next().getId();
	 * this.addInformationToServiceTasks(st, (BPMNExclusiveGateway)
	 * this.getNodeById(targetGtw.getId()), false);
	 * 
	 * for (BPMNDataObject dao : allBPMNDataObjects) {
	 * this.addDataInputReferencesToVotingTasks( (Task)
	 * this.getFlowNodeByBPMNNodeId("Task_votingTask" + votingTaskId), dao); }
	 * 
	 * } else {
	 * 
	 * String exclusiveGatewaySplitId = "EV" + exclusiveGtwCount + "split"; String
	 * exclusiveGatewayJoinId = "EV" + exclusiveGtwCount + "join"; String
	 * exclusiveGatewayName = "EV" + exclusiveGtwCount + "_Loop";
	 * builder.exclusiveGateway(exclusiveGatewayJoinId).name(exclusiveGatewayName).
	 * parallelGateway(parallelSplitId) .name(parallelSplit);
	 * 
	 * while (iter.hasNext()) {
	 * 
	 * BPMNDataObject key = iter.next().getKey();
	 * 
	 * ArrayList<BPMNTask> nextList = votersMap.get(key); Iterator<BPMNTask>
	 * nextListIter = nextList.iterator(); boolean skip = false; while
	 * (nextListIter.hasNext()) { BPMNParticipant nextParticipant =
	 * nextListIter.next().getParticipant();
	 * 
	 * for (Task t : alreadyModelled) { if (t.getName().equals("VotingTask " +
	 * nextParticipant.getName())) { for (BPMNDataObject dao : allBPMNDataObjects) {
	 * this.addDataInputReferencesToVotingTasks(t, dao); } skip = true; } } if (skip
	 * == false) { int votingTaskId = BPMNTask.increaseVotingTaskId();
	 * 
	 * builder.moveToNode(parallelSplitId).userTask("Task_votingTask" +
	 * votingTaskId) .name("VotingTask " + nextParticipant.getName());
	 * alreadyModelled.add((Task) this.getFlowNodeByBPMNNodeId("Task_votingTask" +
	 * votingTaskId)); for (BPMNDataObject dao : allBPMNDataObjects) {
	 * this.addDataInputReferencesToVotingTasks( (Task)
	 * this.getFlowNodeByBPMNNodeId("Task_votingTask" + votingTaskId), dao); }
	 * 
	 * if (isSet == false) { builder.moveToNode("Task_votingTask" +
	 * votingTaskId).parallelGateway(parallelJoinId) .name(parallelJoin); isSet =
	 * true; } else { builder.moveToNode("Task_votingTask" +
	 * votingTaskId).connectTo(parallelJoinId); }
	 * 
	 * } if (!iter.hasNext() && !nextListIter.hasNext()) {
	 * 
	 * String serviceTaskId = "serviceTask_CollectVotes" + i;
	 * builder.moveToNode(parallelJoinId).serviceTask(serviceTaskId).
	 * name("Collect Votes")
	 * .exclusiveGateway(exclusiveGatewaySplitId).name(exclusiveGatewayName);
	 * 
	 * builder.moveToNode(exclusiveGatewaySplitId).connectTo(exclusiveGatewayJoinId)
	 * ;
	 * 
	 * FlowNode flowN = modelInstance.getModelElementById(exclusiveGatewaySplitId);
	 * for (SequenceFlow outgoing : flowN.getOutgoing()) { if (outgoing.getTarget()
	 * .equals(modelInstance.getModelElementById(exclusiveGatewayJoinId))) { // to
	 * avoid overlapping of sequenceflow in diagram outgoing.setName("yes");
	 * this.changeWayPoints(outgoing); } }
	 * 
	 * this.addInformationToServiceTasks((ServiceTask)
	 * this.getFlowNodeByBPMNNodeId(serviceTaskId), (BPMNExclusiveGateway)
	 * this.getNodeById(targetGtw.getId()), true);
	 * 
	 * // add the gateway for the final decider String votingTaskName = ""; for
	 * (Entry<BPMNBusinessRuleTask, BPMNParticipant> entry :
	 * finalDeciderMap.entrySet()) { if (entry.getKey().getId().equals(brt.getId()))
	 * { votingTaskName = "TroubleShooter " + entry.getValue().getName(); } }
	 * 
	 * BPMNExclusiveGateway.increaseExclusiveGtwCount();
	 * exclusiveGatewayDeciderSplitId = "EV" +
	 * BPMNExclusiveGateway.getExclusiveGtwCount() + "split";
	 * exclusiveGatewayDeciderJoinId = "EV" +
	 * BPMNExclusiveGateway.getExclusiveGtwCount() + "join";
	 * exclusiveGatewayDeciderName = "EV" +
	 * BPMNExclusiveGateway.getExclusiveGtwCount(); String serviceTaskDeciderId =
	 * "serviceTask_CollectVotesDecider" + i;
	 * builder.moveToNode(exclusiveGatewaySplitId).exclusiveGateway(
	 * exclusiveGatewayDeciderSplitId) .name(exclusiveGatewayDeciderName)
	 * .userTask("Task_votingTask" +
	 * BPMNTask.increaseVotingTaskId()).name(votingTaskName)
	 * .serviceTask(serviceTaskDeciderId).name("Collect Votes")
	 * .exclusiveGateway(exclusiveGatewayDeciderJoinId).name(
	 * exclusiveGatewayDeciderName); this.addInformationToServiceTasks(
	 * (ServiceTask) this.getFlowNodeByBPMNNodeId(serviceTaskDeciderId),
	 * (BPMNExclusiveGateway) this.getNodeById(targetGtw.getId()), false);
	 * 
	 * for (BPMNDataObject dao : allBPMNDataObjects) {
	 * this.addDataInputReferencesToVotingTasks( (Task)
	 * this.getFlowNodeByBPMNNodeId("Task_votingTask" + BPMNTask.getVotingTaskId()),
	 * dao); }
	 * 
	 * } else { if (nextListIter.hasNext()) { builder.moveToNode(parallelSplitId); }
	 * }
	 * 
	 * skip = false; } }
	 * 
	 * if (mapModelBtn) {
	 * builder.moveToNode(exclusiveGatewayDeciderJoinId).connectTo(targetGtw.getId()
	 * ); builder.moveToNode(exclusiveGatewayDeciderSplitId).connectTo(
	 * exclusiveGatewayDeciderJoinId);
	 * 
	 * }
	 * 
	 * else if (!mapModelBtn) {
	 * 
	 * builder.moveToNode(exclusiveGatewayDeciderJoinId).endEvent(
	 * "endEventWithInSubProcess" + i).name("")
	 * .subProcessDone().connectTo(targetGtw.getId());
	 * builder.moveToNode(exclusiveGatewayDeciderSplitId).connectTo(
	 * exclusiveGatewayDeciderJoinId); }
	 * 
	 * FlowNode flowNo =
	 * modelInstance.getModelElementById(exclusiveGatewayDeciderSplitId);
	 * SequenceFlow incomingSeq = flowNo.getIncoming().iterator().next();
	 * incomingSeq.setName("no"); for (SequenceFlow outgoingSeq :
	 * flowNo.getOutgoing()) { if (outgoingSeq.getTarget() instanceof UserTask) {
	 * outgoingSeq.setName("yes"); } else { outgoingSeq.setName("no");
	 * this.changeWayPoints(outgoingSeq); } }
	 * 
	 * }
	 * 
	 * }
	 */

	private boolean checkSphereRequirement(BPMNTask lastWriterTask, BPMNDataObject dataO, String sphere,
			BPMNParticipant reader, LinkedList<LinkedList<BPMNElement>> pathsBetweenLastWriterAndBrt) {
		// check if the reader is at least in the sphere of the lastWriterTask
		String sphereOfReader = "";

		if (pathsBetweenLastWriterAndBrt.isEmpty()) {
			return false;
		}

		int count = 0;
		int staticCount = 0;
		for (LinkedList<BPMNElement> path : pathsBetweenLastWriterAndBrt) {
			// increase if the reader is found on the path
			int pathCountReader = 0;
			BPMNTask lastWriterOnPath = lastWriterTask;

			// HashMap<Boolean, BPMNElement> readerOnPath = this.readerIsOnPath(reader,
			// lastWriterTask, dataO, path);

			/*
			 * for(Entry<Boolean, BPMNElement> entry: readerOnPath.entrySet()) {
			 * if(entry.getKey().equals(true)) { if(entry.getValue().equals(null)) {
			 * //reader is on the path and no other writer in between
			 * 
			 * } else { //another writer is on the path } } }
			 */

			for (BPMNElement element : path) {

				if (element instanceof BPMNTask) {
					BPMNTask currentTask = (BPMNTask) element;

					if (dataO.getWriters().contains(currentTask)
							&& (!currentTask.getParticipant().equals(lastWriterTask.getParticipant()))) {
						// another writer to the dataObject has been found on the path
						lastWriterOnPath = currentTask;
					}

					if (currentTask.getParticipant().equals(reader) && dataO.getReaders().contains(currentTask)) {
						// the reader has been found on the path
						if (!(lastWriterOnPath.getParticipant().equals(lastWriterTask.getParticipant()))) {
							// if there is another writer on the path between lastWriterTask and currentTask
							// the currentTask is in the static sphere of the lastWriterTask for this path
							staticCount++;

						} else {
							pathCountReader++;
						}

					}

				}
			}
			if (pathCountReader > 0 && staticCount == 0) {
				// reader reads in at least one path without another writer in between
				count++;
			}

		}

		if (pathsBetweenLastWriterAndBrt.size() > count) {
			// reader is not in every path
			if (count == staticCount) {
				// another Writer writes to dataO on each path where reader reads the data after
				// lastWriterTask!
				sphereOfReader = "Static";
			} else {
				sphereOfReader = "Weak-Dynamic";
			}

		} else if (pathsBetweenLastWriterAndBrt.size() == count) {
			// every path contains the participant
			if (count == staticCount) {
				sphereOfReader = "Static";

			} else {
				sphereOfReader = "Strong-Dynamic";
			}

		}

		return this.atLeastInSphere(sphereOfReader, sphere);

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

	public String getSphereForParticipantOnEffectivePathsAfterCurrentBrtWithAlreadyChosenVoters(
			BPMNBusinessRuleTask currentBrt, BPMNElement writerTask, BPMNDataObject dataO, BPMNParticipant reader,
			HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosenVoters, String sphereForReader,
			LinkedList<LinkedList<BPMNElement>> paths) {

		// second step - search from currentBrt to ProcessEnd
		// if the data that was written by the writerTask will be read by some task
		// after the currentBrt
		// WE ARE STILL AT THE POSITION OF THE CURRENTBRT!!!

		// get the effective readers for the writer
		// only those that have not been visited yet

		String sphereForReaderAfterCurrentBrt = sphereForReader;
		
		
		if(((BPMNTask)writerTask).getEffectivePaths().get(true).isEmpty()){
			//need to check if there may be subpaths with the reader on it and no writer in between
			for(LinkedList<BPMNElement> path: ((BPMNTask)writerTask).getEffectivePaths().get(false)) {
				BPMNTask otherReader = null;
				for(BPMNElement el: path) {
					if(el instanceof BPMNTask) {
						BPMNTask task = (BPMNTask)el;
						if(!task.getParticipant().equals(reader)) {
							if(dataO.getWriters().contains(task)) {
								if(!task.equals(writerTask)) {
									break;
								}
							}
							
						}
						
						
						
						if(task.getParticipant().equals(reader)&&dataO.getReaders().contains(task)&&(!task.equals(writerTask))) {
							//another reader to the dataObject found
							otherReader = task;
							break;
						}
					}
					
				}
				if(otherReader!=null) {
					sphereForReaderAfterCurrentBrt = this
							.getSphereForEffectiveReaderAfterBrt((BPMNTask) otherReader, currentBrt);
				}
				
				
			}
			
			
		} else {

		for (BPMNElement effectiveReader : ((BPMNTask) writerTask).getEffectiveReaders()) {
			if (reader.equals(((BPMNTask) effectiveReader).getParticipant())) {
				boolean alreadyVisited = false;
				for (LinkedList<BPMNElement> path : paths) {
					for (BPMNElement pathEl : path) {
						if (pathEl.equals(effectiveReader)) {
							alreadyVisited = true;
							break;
						}
					}
				}
				if (alreadyVisited == false) {
					sphereForReaderAfterCurrentBrt = this
							.getSphereForEffectiveReaderAfterBrt((BPMNTask) effectiveReader, currentBrt);
				}
			}

		}
		}
		return sphereForReaderAfterCurrentBrt;

	}

	public String getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(BPMNBusinessRuleTask currentBrt,
			BPMNElement writerTask, BPMNDataObject dataO, BPMNParticipant reader,
			HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosenVoters) {

		if (writerTask == null || currentBrt == null || dataO == null) {
			return "not existent";
		}
		// calculate sphere for the reader at the position of the currentBrt, therefore:
		// first check if the reader is on each effective path going from writerTask to
		// currentBrt
		// -> reader is in SD at the position of the currentBrt
		// if there is a non-effective path going from writerTask to currentBrt
		// -> reader is in WD if he is on effective paths but there exist non-effective
		// ones

		// if the first check does not evaluate to WD or SD and the lastWriter demands
		// it
		// we can extend the search by checking the sphere for the reader from
		// currentBrt to processEnd
		// this means we possibly suggest participants for voting at the position of the
		// currentBrt even if they will be upgraded
		// to the required sphere of the lastWriter after the currentBrt
		// in other words: they will eventually get the data in the process after the
		// currentBrt

		// first step - get the effective paths between writerTask and currentBrt
		HashMap<Boolean, LinkedList<LinkedList<BPMNElement>>> effectivePathsBetweenWriterTaskAndCurrentBrt = this
				.getEffectivePathsBetweenWriterAndTargetElement(dataO, writerTask, currentBrt,
						new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(),
						new LinkedList<LinkedList<BPMNElement>>());
		String sphereForReader = this.getSphereOnPathBeforeCurrentBrt(currentBrt, writerTask, dataO, reader,
				effectivePathsBetweenWriterTaskAndCurrentBrt, alreadyChosenVoters);

		return sphereForReader;

	}

	public String getSphereForEffectiveReaderAfterBrt(BPMNTask effectiveReader, BPMNBusinessRuleTask currentBrt) {
		String sphereForEffectiveReader = "";
		ArrayList<Label> efReaderLabels = effectiveReader.getLabels();
		ArrayList<Label> brtLabels = currentBrt.getLabels();
		if (efReaderLabels.equals(brtLabels)) {
			sphereForEffectiveReader = "Strong-Dynamic";
		} else if (efReaderLabels.containsAll(brtLabels) && efReaderLabels.size() > brtLabels.size()) {
			sphereForEffectiveReader = "Weak-Dynamic";
		} else {
			// effective Reader is in other xor-branch than the currentBrt
			// other branch will not be considered
			// do not change the sphere that has been generated before
		}

		return sphereForEffectiveReader;
	}

	private String getSphereOnPathBeforeCurrentBrt(BPMNBusinessRuleTask currentBrt, BPMNElement writerTask,
			BPMNDataObject dataO, BPMNParticipant reader,
			HashMap<Boolean, LinkedList<LinkedList<BPMNElement>>> effectivePaths,
			HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosenVoters) {
		int strongDynamicCountEffectivePaths = 0;
		int countReaderOnNonEffectivePath = 0;
		if (writerTask == null || currentBrt == null) {
			return "not existent";
		}
		// the participant of the writerTask is always in SD looking from writerTask
		// onwards
		if (((BPMNTask) writerTask).getParticipant().equals(reader)) {
			return "Strong-Dynamic";
		}

		for (Entry<Boolean, LinkedList<LinkedList<BPMNElement>>> entry : effectivePaths.entrySet()) {
			for (LinkedList<BPMNElement> path : entry.getValue()) {

				boolean readerFound = false;
				for (BPMNElement pathEl : path) {

					if (pathEl instanceof BPMNTask) {
						BPMNTask task = (BPMNTask) pathEl;
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
								if (dataO.getReaders().contains(currentBrtOnPath)
										&& task.getParticipant().equals(reader)
										&& this.isParticipantInList(dataO.getReaders(), reader)) {
									// reader found on the path
									readerFound = true;
								}

							}
						} else {
							if (dataO.getReaders().contains(task) && task.getParticipant().equals(reader)
									&& this.isParticipantInList(dataO.getReaders(), reader)) {
								// reader found on the path
								readerFound = true;
							}

						}
					}

				}
				if (readerFound) {
					if (entry.getKey() == true) {
						// reader has been found on an effective path
						strongDynamicCountEffectivePaths++;
					} else if (entry.getKey() == false) {
						countReaderOnNonEffectivePath++;
					}
				}

			}

		}


		if (!effectivePaths.get(true).isEmpty()) {
			if (strongDynamicCountEffectivePaths == effectivePaths.get(true).size()
					&& effectivePaths.get(false).isEmpty()) {
				return "Strong-Dynamic";
			} else if (strongDynamicCountEffectivePaths == effectivePaths.get(true).size()
					&& !effectivePaths.get(false).isEmpty()) {
				// reader reads data on each effective path, but there are also non effective
				// ones
				return "Weak-Dynamic";
			} else if (strongDynamicCountEffectivePaths > 0
					&& (strongDynamicCountEffectivePaths < effectivePaths.get(true).size())) {
				// reader reads data on some effective path, but there are also non effective
				// ones
				return "Weak-Dynamic";
			} else if (strongDynamicCountEffectivePaths == 0) {
				if (dataO.getStaticSphere().contains(reader)) {
					return "Static";
				} else if (this.globalSphere.contains(reader)) {
					return "Global";
				}

			}
		} else {
			// there are no effective paths
			if (dataO.getStaticSphere().contains(reader)) {
				return "Static";
			} else if (this.globalSphere.contains(reader)) {
				return "Global";
			}

		}

		return "";

	}

	private String getSphereOnPathAfterCurrentBrt(BPMNBusinessRuleTask currentBrt, BPMNElement writerTask,
			BPMNDataObject dataO, BPMNParticipant reader,
			HashMap<Boolean, LinkedList<LinkedList<BPMNElement>>> effectivePaths,
			HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosenVoters) {
		int strongDynamicCountEffectivePaths = 0;
		int countReaderOnNonEffectivePath = 0;

		for (Entry<Boolean, LinkedList<LinkedList<BPMNElement>>> entry : effectivePaths.entrySet()) {
			for (LinkedList<BPMNElement> path : entry.getValue()) {

				boolean readerFound = false;
				for (BPMNElement pathEl : path) {

					if (pathEl instanceof BPMNTask) {
						BPMNTask task = (BPMNTask) pathEl;
						if (task instanceof BPMNBusinessRuleTask) {
							// the participant of the brt gets substituted with the already chosen voters
							// for it
							BPMNBusinessRuleTask currentBrtOnPath = (BPMNBusinessRuleTask) task;
							if (alreadyChosenVoters.get(currentBrtOnPath) != null) {
								// check if the reader given as an argument is one of the already chosen voters
								// for the brt
								if (alreadyChosenVoters.get(currentBrtOnPath).contains(reader)
										&& this.isParticipantInList(dataO.getReaders(), reader)
										&& dataO.getReaders().contains(currentBrtOnPath)) {
									readerFound = true;
								}

							} else {
								if (dataO.getReaders().contains(currentBrtOnPath)
										&& task.getParticipant().equals(reader)
										&& this.isParticipantInList(dataO.getReaders(), reader)) {
									// reader found on the path
									readerFound = true;
								}

							}
						} else {
							if (dataO.getReaders().contains(task) && task.getParticipant().equals(reader)
									&& this.isParticipantInList(dataO.getReaders(), reader)) {
								// reader found on the path
								readerFound = true;
							}

						}
					}

				}
				if (readerFound) {
					if (entry.getKey() == true) {
						// reader has been found on an effective path
						strongDynamicCountEffectivePaths++;
					} else if (entry.getKey() == false) {
						countReaderOnNonEffectivePath++;
					}
				}

			}

		}

		if (!effectivePaths.get(true).isEmpty()) {
			if (strongDynamicCountEffectivePaths == effectivePaths.get(true).size()
					&& effectivePaths.get(false).isEmpty()) {
				return "Strong-Dynamic";
			} else if (strongDynamicCountEffectivePaths == effectivePaths.get(true).size()
					&& !effectivePaths.get(false).isEmpty()) {
				// reader reads data on each effective path, but there are also non effective
				// ones
				return "Weak-Dynamic";
			} else if (strongDynamicCountEffectivePaths == 0) {
				if (dataO.getStaticSphere().contains(reader)) {
					return "Static";
				} else if (this.globalSphere.contains(reader)) {
					return "Global";
				}

			} else if (strongDynamicCountEffectivePaths > 0
					&& strongDynamicCountEffectivePaths < effectivePaths.get(true).size()) {
				return "Weak-Dynamic";
			}

		} else {
			// there are no effective paths
			if (dataO.getStaticSphere().contains(reader)) {
				return "Static";
			} else if (this.globalSphere.contains(reader)) {
				return "Global";
			}

		}

		return "";

	}

	public int getAmountPossibleCombinationsOfParticipants() {
		return amountPossibleCombinationsOfParticipants;
	}

	public void setAmountPossibleCombinationsOfParticipants(int amountPossibleCombinationsOfParticipants) {
		this.amountPossibleCombinationsOfParticipants = amountPossibleCombinationsOfParticipants;
	}

	public LinkedList<LinkedList<FlowNode>> getAllPathsThroughProcess() {
		return this.pathsThroughProcess;
	}

	public ArrayList<BPMNDataObject> getDataObjects() {
		return this.dataObjects;
	}

	public BPMNEndEvent getBpmnEnd() {
		return bpmnEnd;
	}

	public void setBpmnEnd(BPMNEndEvent bpmnEnd) {
		this.bpmnEnd = bpmnEnd;
	}

	public LinkedList<ProcessInstanceWithVoters> getProcessInstancesWithVoters() {
		return processInstancesWithVoters;
	}

	public void setProcessInstancesWithVoters(LinkedList<ProcessInstanceWithVoters> processInstancesWithVoters) {
		this.processInstancesWithVoters = processInstancesWithVoters;
	}

	private BpmnShape getShape(String id) {

		for (BpmnShape shape : modelInstance.getModelElementsByType(BpmnShape.class)) {
			if (shape.getBpmnElement().getId().equals(id)) {
				return shape;
			}
		}
		return null;

	}
	
	public double getExecutionTimeLocalMinimumAlgorithmWithLimit(int limitPerIteration) {
		String key = "localMinWithLimit"+limitPerIteration;
		return this.executionTimeMap.get(key);		
	}

	public double getExecutionTimeLocalMinimumAlgorithmWithLimit() {
		if(this.getAlgorithmToPerform().contains("localMinWithLimit")) {
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
		this.algorithmToPerform=algorithmToPerform;
	}

	public  String getAlgorithmToPerform() {
		return this.algorithmToPerform;
	}

	public List<BPMNElement> getProcessElements() {
		return this.processElements;
	}

	@Override
	public synchronized HashMap<String, LinkedList<ProcessInstanceWithVoters>> call() throws NullPointerException, InterruptedException, Exception {
		// TODO Auto-generated method stub
		System.out.println("Call algorithm for: "+this.process.getAbsolutePath());
		HashMap<String, LinkedList<ProcessInstanceWithVoters>> mapToReturn = new HashMap<String, LinkedList<ProcessInstanceWithVoters>>();
			if (this.algorithmToPerform.contentEquals("bruteForce")) {
				LinkedList<ProcessInstanceWithVoters> pInstBruteForce = this.bruteForceAlgorithm();
				mapToReturn.putIfAbsent("bruteForce", pInstBruteForce);
				return mapToReturn;

			} else if (this.algorithmToPerform.contentEquals("localMin")) {
				LinkedList<ProcessInstanceWithVoters> pInstLocalMin = this.localMinimumAlgorithm();
				mapToReturn.putIfAbsent("localMin", pInstLocalMin);
				return mapToReturn;

			} else if(this.algorithmToPerform.contains("localMinWithLimit")) {
				String limit = this.algorithmToPerform;
				int limitPerIteration = Integer.parseInt(limit.replaceAll("\\D+","").trim());
				LinkedList<ProcessInstanceWithVoters> pInstLocalMinWithLimit = this.localMinimumAlgorithmWithLimit(limitPerIteration);
				mapToReturn.putIfAbsent(this.algorithmToPerform, pInstLocalMinWithLimit);
				return mapToReturn;				
			}

		
		return null;
	}
	
}
