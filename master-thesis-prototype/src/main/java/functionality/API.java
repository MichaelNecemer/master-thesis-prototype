package functionality;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
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
import org.camunda.bpm.model.bpmn.instance.Gateway;
import org.camunda.bpm.model.bpmn.instance.Lane;
import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.camunda.bpm.model.bpmn.instance.TextAnnotation;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;

import mapping.BPMNBusinessRuleTask;
import mapping.BPMNDataObject;
import mapping.BPMNElement;
import mapping.BPMNEndEvent;
import mapping.BPMNExclusiveGateway;
import mapping.BPMNGateway;
import mapping.BPMNParallelGateway;
import mapping.BPMNParticipant;
import mapping.BPMNStartEvent;
import mapping.BPMNTask;
import mapping.Combination;
import mapping.DecisionEvaluation;
import mapping.InfixToPostfix;
import mapping.Label;

public class API {

	private BpmnModelInstance modelInstance;
	private BpmnModelInstance modelInstanceWithParallels;
	private File processModelFile;
	private LinkedList<BPMNParticipant> privateSphere;
	private LinkedList<MandatoryParticipantConstraint> mandatoryParticipantConstraints;
	private LinkedList<ExcludeParticipantConstraint> excludeParticipantConstraints;
	private boolean modelWithLanes;
	private LinkedList<BPMNBusinessRuleTask> businessRuleTasks;
	private LinkedList<BPMNElement> processElements;
	private BPMNStartEvent startEvent;
	private BPMNEndEvent endEvent;
	private ArrayList<Label> labelList;
	private LinkedList<BPMNDataObject> dataObjects;
	private BPMNParticipant troubleShooter;
	private LinkedList<LinkedList<FlowNode>> pathsThroughProcess;
	private String privateSphereKey;
	private String staticSphereKey;
	private String weakDynamicSphereKey;
	private String strongDynamicSphereKey;
	private LinkedList<Double> weightingParameters;
	private HashMap<Enums.AlgorithmToPerform, LinkedList<String>> executionTimeMap;
	private int bound;
	private int amountParallelsBeforePreprocessing;
	private static final int NANOSEC_TO_SEC = 1_000_000_000;

	public API(String pathToBpmnCamundaFile, LinkedList<Double> weightingParameters, boolean testIfModelValid,
			boolean calculateAmountOfPaths) throws Exception {
		if (weightingParameters.size() != 3) {
			throw new Exception("Not exactly 3 weighting cost parameters (Alpha, Beta, Gamma) in the list!");
		}
		this.weightingParameters = weightingParameters;
		this.processModelFile = new File(pathToBpmnCamundaFile);
		// preprocess the model, i.e. remove parallel branches
		BpmnModelInstance model = Bpmn.readModelFromFile(this.processModelFile);
		this.modelInstanceWithParallels = model;
		this.amountParallelsBeforePreprocessing = CommonFunctionality.getAmountParallelGtwSplits(model);
		this.modelInstance = CommonFunctionality.doPreprocessing(model);
		int amountParallelSplitsAfterPreprocessing = CommonFunctionality.getAmountParallelGtwSplits(this.modelInstance);
		if (amountParallelSplitsAfterPreprocessing > 0) {
			throw new Exception("Still parallel splits in the model after preprocessing!");
		}
		System.out.println("API for: " + this.processModelFile.getName());
		boolean correctModel = true;
		try {
			CommonFunctionality.isCorrectModel(this.modelInstance, testIfModelValid);
		} catch (Exception e) {
			correctModel = false;
			System.out.println("Model correct: " + correctModel);
			throw new Exception(e.getMessage());
		}
		System.out.println("Model correct: " + correctModel);
		this.processElements = new LinkedList<BPMNElement>();
		this.businessRuleTasks = new LinkedList<BPMNBusinessRuleTask>();
		this.dataObjects = new LinkedList<BPMNDataObject>();
		this.labelList = new ArrayList<Label>();
		this.privateSphere = new LinkedList<BPMNParticipant>();
		// set the default cluster condition
		this.executionTimeMap = new HashMap<Enums.AlgorithmToPerform, LinkedList<String>>();
		this.mandatoryParticipantConstraints = new LinkedList<MandatoryParticipantConstraint>();
		this.excludeParticipantConstraints = new LinkedList<ExcludeParticipantConstraint>();
		this.modelWithLanes = false;
		this.businessRuleTasks = new LinkedList<BPMNBusinessRuleTask>();
		this.privateSphereKey = "Private";
		this.staticSphereKey = "Static";
		this.weakDynamicSphereKey = "Weak-Dynamic";
		this.strongDynamicSphereKey = "Strong-Dynamic";
		this.bound = 0;
		// map all the elements from camunda
		this.mapCamundaElements();
		this.pathsThroughProcess = new LinkedList<LinkedList<FlowNode>>();
		// expensive calculations when a lot of xors in process
		if (calculateAmountOfPaths) {
			this.pathsThroughProcess = CommonFunctionality.getAllPathsBetweenNodes(this.modelInstance,
					this.startEvent.getId(), this.endEvent.getId());
		}
		System.out.println("Paths: " + this.pathsThroughProcess.size());
	}

	public synchronized LinkedList<PModelWithAdditionalActors> exhaustiveSearch(int bound) throws Exception {

		if (bound != 0) {
			bound = 0;
		}
		long startTime = System.nanoTime();
		LinkedList<String> timeList = new LinkedList<String>();
		timeList.add("null");
		timeList.add("null");
		timeList.add("null");
		this.executionTimeMap.put(Enums.AlgorithmToPerform.EXHAUSTIVE, timeList);

		try {
			// compute static sphere per data object without add actors
			HashMap<BPMNDataObject, LinkedList<HashSet<?>>> staticSpherePerDataObject = this
					.computeStaticSpherePerDataObject();

			// compute wd sphere for origins without additional actors
			// only for origins of data objects (may be the same for different brts!)
			HashMap<BPMNDataObject, LinkedList<LinkedList<HashSet<?>>>> wdSpherePerDataObject = this.computeWdSphere();

			LinkedList<PModelWithAdditionalActors> additionalActorsCombs = new LinkedList<PModelWithAdditionalActors>();

			// generate all possible combinations of additional readers
			additionalActorsCombs = this.generatePossibleCombinationsOfAdditionalActorsWithBound(this.bound);
			long endTimeGeneratingCombs = System.nanoTime();

			long timeForGeneratingCombs = endTimeGeneratingCombs - startTime;
			timeList.set(0, (double) timeForGeneratingCombs / NANOSEC_TO_SEC + "");

			// calculate the cost measure for all possible additional actors combinations
			this.calculateMeasure(additionalActorsCombs, staticSpherePerDataObject, wdSpherePerDataObject,
					new HashMap<BPMNTask, LinkedList<LinkedList<BPMNElement>>>(),
					new HashMap<BPMNTask, HashMap<BPMNBusinessRuleTask, LinkedList<LinkedList<BPMNElement>>>>());

			long endTimeExhaustiveSearch = System.nanoTime();

			long timeForCalculatingMeasure = endTimeExhaustiveSearch - endTimeGeneratingCombs;
			timeList.set(1, (double) timeForCalculatingMeasure / NANOSEC_TO_SEC + "");

			long executionTimeExhaustive = endTimeExhaustiveSearch - startTime;
			timeList.set(2, (double) executionTimeExhaustive / NANOSEC_TO_SEC + "");

			return additionalActorsCombs;

		} catch (Exception ex) {
			throw ex;
		} finally {
			this.executionTimeMap.put(Enums.AlgorithmToPerform.EXHAUSTIVE, timeList);
			System.out.println("Exhaustive search execution time in sec: " + timeList.get(2));
		}
	}

	public synchronized LinkedList<PModelWithAdditionalActors> baseHeuristicSearch(int bound)
			throws NullPointerException, InterruptedException, NotEnoughAddActorsException, Exception {
		long startTime = System.nanoTime();
		LinkedList<String> timeList = new LinkedList<String>();
		timeList.add("null");
		timeList.add("null");
		timeList.add("null");
		this.executionTimeMap.put(Enums.AlgorithmToPerform.BASEHEURISTIC, timeList);

		try {
			// compute static sphere per data object without add actors
			HashMap<BPMNDataObject, LinkedList<HashSet<?>>> staticSpherePerDataObject = this
					.computeStaticSpherePerDataObject();

			// compute wd sphere for origins without additional actors
			// only for origins of data objects (may be the same for different brts!)
			HashMap<BPMNDataObject, LinkedList<LinkedList<HashSet<?>>>> wdSpherePerDataObject = this.computeWdSphere();

			HashMap<BPMNBusinessRuleTask, LinkedList<AdditionalActors>> alreadyChosenAdditionalActors = new HashMap<BPMNBusinessRuleTask, LinkedList<AdditionalActors>>();
			HashMap<BPMNTask, LinkedList<LinkedList<BPMNElement>>> pathsFromOriginToEndMap = new HashMap<BPMNTask, LinkedList<LinkedList<BPMNElement>>>();
			LinkedList<LinkedList<AdditionalActors>> allCheapestAddActorsLists = new LinkedList<LinkedList<AdditionalActors>>();

			// query brts in any order
			// do not consider already generated additional actors
			// shuffle brts first, else they may be in same order as incremental heuristic
			// search already
			Collections.shuffle(this.businessRuleTasks);
			for (BPMNBusinessRuleTask brt : this.businessRuleTasks) {
				LinkedList<AdditionalActors> addActorsForBrt = this.getCheapestAdditionalActorsForBrtHeuristic(false,
						brt, pathsFromOriginToEndMap, staticSpherePerDataObject, wdSpherePerDataObject, null, bound);
				alreadyChosenAdditionalActors.putIfAbsent(brt, addActorsForBrt);
				LinkedList<LinkedList<AdditionalActors>> currLists = this.computeAllAddActorsLists(addActorsForBrt,
						allCheapestAddActorsLists);
				allCheapestAddActorsLists.clear();
				allCheapestAddActorsLists.addAll(currLists);
			}

			LinkedList<PModelWithAdditionalActors> pModels = new LinkedList<PModelWithAdditionalActors>();
			for (LinkedList<AdditionalActors> additionalActorsList : allCheapestAddActorsLists) {
				PModelWithAdditionalActors newModel = new PModelWithAdditionalActors(additionalActorsList,
						this.weightingParameters);
				pModels.add(newModel);
			}

			long endTimeGeneratingCombs = System.nanoTime();
			long timeForGeneratingCombs = endTimeGeneratingCombs - startTime;
			timeList.set(0, (double) timeForGeneratingCombs / NANOSEC_TO_SEC + "");

			// calculate the cost measure for all possible additional actors combinations
			this.calculateMeasure(pModels, staticSpherePerDataObject, wdSpherePerDataObject, pathsFromOriginToEndMap,
					new HashMap<BPMNTask, HashMap<BPMNBusinessRuleTask, LinkedList<LinkedList<BPMNElement>>>>());

			long endTimeBaseHeuristicSearch = System.nanoTime();

			long timeForCalculatingMeasure = endTimeBaseHeuristicSearch - endTimeGeneratingCombs;
			timeList.set(1, (double) timeForCalculatingMeasure / NANOSEC_TO_SEC + "");

			long executionTimeBaseHeuristic = endTimeBaseHeuristicSearch - startTime;
			timeList.set(2, (double) executionTimeBaseHeuristic / NANOSEC_TO_SEC + "");

			System.out.println("Heuristic base search generated solutions: " + pModels.size());

			if (!this.testIfAdditionalActorsValid(pModels.get(0))) {
				throw new Exception("Not valid assigned additional actors!");
			}

			return pModels;

		} catch (Exception ex) {
			throw ex;
		} finally {
			this.executionTimeMap.put(Enums.AlgorithmToPerform.BASEHEURISTIC, timeList);
			System.out.println("Heuristic base search combsGen time in sec: " + timeList.get(0));
			System.out.println("Heuristic base search calcMeasure time in sec: " + timeList.get(1));
			System.out.println("Heuristic base search execution time in sec: " + timeList.get(2));
		}

	}

	public synchronized LinkedList<PModelWithAdditionalActors> incrementalHeuristicSearch(int bound)
			throws NullPointerException, InterruptedException, NotEnoughAddActorsException, Exception {
		long startTime = System.nanoTime();
		LinkedList<String> timeList = new LinkedList<String>();
		timeList.add("null");
		timeList.add("null");
		timeList.add("null");
		this.executionTimeMap.put(Enums.AlgorithmToPerform.INCREMENTALHEURISTIC, timeList);

		try {
			// compute static sphere per data object without add actors
			HashMap<BPMNDataObject, LinkedList<HashSet<?>>> staticSpherePerDataObject = this
					.computeStaticSpherePerDataObject();

			// compute wd sphere for origins without additional actors
			// only for origins of data objects (may be the same for different brts!)
			HashMap<BPMNDataObject, LinkedList<LinkedList<HashSet<?>>>> wdSpherePerDataObject = this.computeWdSphere();

			HashMap<BPMNBusinessRuleTask, LinkedList<AdditionalActors>> alreadyChosenAdditionalActors = new HashMap<BPMNBusinessRuleTask, LinkedList<AdditionalActors>>();
			HashMap<BPMNTask, LinkedList<LinkedList<BPMNElement>>> pathsFromOriginToEndMap = new HashMap<BPMNTask, LinkedList<LinkedList<BPMNElement>>>();
			LinkedList<LinkedList<AdditionalActors>> allCheapestAddActorsLists = new LinkedList<LinkedList<AdditionalActors>>();

			// query brts in topological order
			// consider already generated additional actors
			allCheapestAddActorsLists = queryBrtsInTopologicalOrderAndAssignAdditionalActorsForIncrementalHeuristic(
					this.startEvent, this.endEvent, new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(),
					this.endEvent, pathsFromOriginToEndMap, alreadyChosenAdditionalActors, staticSpherePerDataObject,
					wdSpherePerDataObject, allCheapestAddActorsLists, bound);

			LinkedList<PModelWithAdditionalActors> pModels = new LinkedList<PModelWithAdditionalActors>();
			for (LinkedList<AdditionalActors> additionalActorsList : allCheapestAddActorsLists) {
				PModelWithAdditionalActors newModel = new PModelWithAdditionalActors(additionalActorsList,
						this.weightingParameters);
				pModels.add(newModel);
			}

			long endTimeGeneratingCombs = System.nanoTime();
			long timeForGeneratingCombs = endTimeGeneratingCombs - startTime;
			timeList.set(0, (double) timeForGeneratingCombs / NANOSEC_TO_SEC + "");

			// calculate the cost measure for all possible additional actors combinations
			this.calculateMeasure(pModels, staticSpherePerDataObject, wdSpherePerDataObject, pathsFromOriginToEndMap,
					new HashMap<BPMNTask, HashMap<BPMNBusinessRuleTask, LinkedList<LinkedList<BPMNElement>>>>());

			long endTimeIncrementalHeuristicSearch = System.nanoTime();

			long timeForCalculatingMeasure = endTimeIncrementalHeuristicSearch - endTimeGeneratingCombs;
			timeList.set(1, (double) timeForCalculatingMeasure / NANOSEC_TO_SEC + "");

			long executionTimeIncrementalHeuristic = endTimeIncrementalHeuristicSearch - startTime;
			timeList.set(2, (double) executionTimeIncrementalHeuristic / NANOSEC_TO_SEC + "");

			System.out.println("Incremental heuristic search generated solutions: " + pModels.size());

			if (!this.testIfAdditionalActorsValid(pModels.get(0))) {
				throw new Exception("Not valid assigned additional actors!");
			}

			return pModels;

		} catch (Exception ex) {
			throw ex;
		} finally {
			this.executionTimeMap.put(Enums.AlgorithmToPerform.INCREMENTALHEURISTIC, timeList);
			System.out.println("Incremental heuristic search combsGen time in sec: " + timeList.get(0));
			System.out.println("Incremental heuristic search calcMeasure time in sec: " + timeList.get(1));
			System.out.println("Incremental heuristic search execution time in sec: " + timeList.get(2));
		}

	}

	public synchronized LinkedList<PModelWithAdditionalActors> advancedHeuristicSearch(int bound) throws Exception {

		long startTime = System.nanoTime();
		LinkedList<String> timeList = new LinkedList<String>();
		timeList.add("null");
		timeList.add("null");
		timeList.add("null");
		this.executionTimeMap.put(Enums.AlgorithmToPerform.ADVANCEDHEURISTIC, timeList);

		try {
			// compute static sphere per data object without add actors
			HashMap<BPMNDataObject, LinkedList<HashSet<?>>> staticSpherePerDataObject = this
					.computeStaticSpherePerDataObject();

			// compute the static sphere tiebreaker map 
	
			HashMap<BPMNDataObject, HashMap<BPMNParticipant, Double>> staticSphereTiebreakerMap = this
					.computeStaticSphereTiebreakerHashMap();

			LinkedList<LinkedList<AdditionalActors>> allCheapestAddActorsLists = new LinkedList<LinkedList<AdditionalActors>>();
			LinkedList<PModelWithAdditionalActors> pModelAddActors = new LinkedList<PModelWithAdditionalActors>();

			HashMap<BPMNDataObject, LinkedList<LinkedList<HashSet<?>>>> wdSpherePerDataObject = this.computeWdSphere();

			// the following maps will contain paths from origins ongoing to avoid
			// redundant calculations
			HashMap<BPMNTask, LinkedList<LinkedList<BPMNElement>>> pathsFromOriginToEndMap = new HashMap<BPMNTask, LinkedList<LinkedList<BPMNElement>>>();
			HashMap<BPMNTask, HashMap<BPMNBusinessRuleTask, LinkedList<LinkedList<BPMNElement>>>> pathFromOriginOverCurrBrtToEndMap = new HashMap<BPMNTask, HashMap<BPMNBusinessRuleTask, LinkedList<LinkedList<BPMNElement>>>>();

			HashMap<BPMNBusinessRuleTask, LinkedList<AdditionalActors>> alreadyChosenAdditionalActors = new HashMap<BPMNBusinessRuleTask, LinkedList<AdditionalActors>>();
			allCheapestAddActorsLists = this.queryBrtsTopolgicalAdvancedHeuristicSearch(this.startEvent, this.endEvent,
					new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(), this.endEvent,
					pathsFromOriginToEndMap, alreadyChosenAdditionalActors, staticSpherePerDataObject,
					wdSpherePerDataObject, new LinkedList<LinkedList<AdditionalActors>>(), staticSphereTiebreakerMap,
					bound);

			for (LinkedList<AdditionalActors> additionalActorsList : allCheapestAddActorsLists) {
				PModelWithAdditionalActors newModel = new PModelWithAdditionalActors(additionalActorsList,
						this.weightingParameters);
				pModelAddActors.add(newModel);
			}

			long endTimeGeneratingCombs = System.nanoTime();
			long timeForGeneratingCombs = endTimeGeneratingCombs - startTime;
			timeList.set(0, (double) timeForGeneratingCombs / NANOSEC_TO_SEC + "");

			// calculate the cost measure for the all additional actors combinations found
			// with the advanced heuristic search
			this.calculateMeasure(pModelAddActors, staticSpherePerDataObject, wdSpherePerDataObject,
					pathsFromOriginToEndMap, pathFromOriginOverCurrBrtToEndMap);

			long endTimeAdvancedHeuristicSearch = System.nanoTime();

			long timeForCalculatingMeasure = endTimeAdvancedHeuristicSearch - endTimeGeneratingCombs;
			timeList.set(1, (double) timeForCalculatingMeasure / NANOSEC_TO_SEC + "");

			long executionTimeAdvancedHeuristic = endTimeAdvancedHeuristicSearch - startTime;
			timeList.set(2, (double) executionTimeAdvancedHeuristic / NANOSEC_TO_SEC + "");

			System.out.println("Advanced heuristic search generated solutions: " + pModelAddActors.size());
			if (!this.testIfAdditionalActorsValid(pModelAddActors.get(0))) {
				throw new Exception("Not valid assigned additional actors!");
			}

			return pModelAddActors;

		} catch (

		Exception ex) {
			throw ex;
		} finally {
			this.executionTimeMap.put(Enums.AlgorithmToPerform.ADVANCEDHEURISTIC, timeList);
			System.out.println("Advanced heuristic search combsGen time in sec: " + timeList.get(0));
			System.out.println("Advanced heuristic search calcMeasure time in sec: " + timeList.get(1));
			System.out.println("Advanced heuristic search execution time in sec: " + timeList.get(2));
		}

	}

	public LinkedList<PModelWithAdditionalActors> generatePossibleCombinationsOfAdditionalActorsWithBound(int bound)
			throws InterruptedException, NotEnoughAddActorsException {
		LinkedList<LinkedList<Object>> combinationsPerBrt = new LinkedList<LinkedList<Object>>();

		// if bound <= 0 -> unbounded

		for (BPMNBusinessRuleTask brt : this.businessRuleTasks) {
			if (Thread.currentThread().isInterrupted()) {
				System.err.println("Interrupted! " + Thread.currentThread().getName());
				throw new InterruptedException();
			}
			LinkedList<LinkedList<BPMNParticipant>> potentialAddActors = this.getPotentialAddActorsListsForBrt(brt);
			LinkedList<AdditionalActors> addActorsCombinationsForBrt = this
					.generateAdditionalActorsForBrtsWithConstraintsAndBound(brt, potentialAddActors.get(0),
							potentialAddActors.get(1), null, bound);
			LinkedList<Object> addActors = new LinkedList<Object>();
			addActors.addAll(addActorsCombinationsForBrt);
			combinationsPerBrt.add(addActors);
		}

		return this.generateAllPossiblePModelWithAdditionalActors(combinationsPerBrt);
	}

	private LinkedList<PModelWithAdditionalActors> generateAllPossiblePModelWithAdditionalActors(
			LinkedList<LinkedList<Object>> combinationsPerBrt)
			throws InterruptedException, NotEnoughAddActorsException {
		// generate all possible combinations of additional actors for brts

		LinkedList<PModelWithAdditionalActors> pModelWithAddActorsList = new LinkedList<PModelWithAdditionalActors>();
		Collection<List<Object>> combs = Combination.permutations(combinationsPerBrt);
		for (List list : combs) {
			if (Thread.currentThread().isInterrupted()) {
				System.err.println("Interrupted" + Thread.currentThread().getName());
				throw new InterruptedException();
			}
			LinkedList<AdditionalActors> additionalActorsList = new LinkedList<AdditionalActors>(list);
			PModelWithAdditionalActors newModel = new PModelWithAdditionalActors(additionalActorsList,
					this.weightingParameters);
			pModelWithAddActorsList.add(newModel);
		}

		return pModelWithAddActorsList;
	}

	private LinkedList<AdditionalActors> generateAdditionalActorsForBrtsWithConstraintsAndBound(
			BPMNBusinessRuleTask currBrt, LinkedList<BPMNParticipant> allPossibleActors,
			LinkedList<BPMNParticipant> allMandatoryActors, LinkedList<BPMNParticipant> preferredParticipants,
			int boundForAddActors) throws InterruptedException, NotEnoughAddActorsException {

		BPMNExclusiveGateway bpmnEx = (BPMNExclusiveGateway) currBrt.getSuccessors().iterator().next();
		LinkedList<AdditionalActors> additionalActorsForBrt = new LinkedList<AdditionalActors>();

		int boundCombsForNotMandatoryParticipants = bpmnEx.getAmountAddActors() - allMandatoryActors.size();
		LinkedList<BPMNParticipant> notMands = new LinkedList<BPMNParticipant>(allPossibleActors);
		notMands.removeAll(allMandatoryActors);
		if (preferredParticipants != null) {
			notMands.removeAll(preferredParticipants);
			preferredParticipants.removeAll(allMandatoryActors);
		} else {
			preferredParticipants = new LinkedList<BPMNParticipant>();
		}

		int boundCombsForPreferredPartList = boundCombsForNotMandatoryParticipants - preferredParticipants.size();

		LinkedList<LinkedList<BPMNParticipant>> list = new LinkedList<LinkedList<BPMNParticipant>>();
		LinkedList<LinkedList<BPMNParticipant>> remaining = new LinkedList<LinkedList<BPMNParticipant>>();

		if (boundCombsForPreferredPartList == 0) {
			// there are exactly as many preferred participants ( + mandatory participants)
			// as verifiers needed
			list.add(preferredParticipants);
		} else if (boundCombsForPreferredPartList < 0) {
			// there are more preferred participants ( + mandatory participants) than
			// verifiers still needed
			if (boundCombsForNotMandatoryParticipants > 0) {
				// compute the combinations of the verifiers still needed (= verifiers needed
				// without the mandatory ones) from the preferred participants
				list = Combination.getPermutationsWithBound(preferredParticipants,
						boundCombsForNotMandatoryParticipants, boundForAddActors);
			} else {
				list.add(new LinkedList<BPMNParticipant>());
			}

		} else {
			// there are less preferred participants ( + mandatory participants) than
			// verifiers still needed
			// take all preferred participants
			list.add(preferredParticipants);

			// compute the remaining combs of participants without mandatory ones
			remaining = Combination.getPermutationsWithBound(notMands,
					Math.min(notMands.size(), Math.abs(boundCombsForPreferredPartList)), boundForAddActors);
		}

		// bound??
		for (int i = 0; i < list.size(); i++) {
			LinkedList<BPMNParticipant> currList = list.get(i);
			// add the mandatory additional actors
			currList.addAll(0, allMandatoryActors);
			if (remaining.size() > 0) {
				for (int j = 0; j < remaining.size(); j++) {
					if (boundForAddActors > 0) {
						if (j == boundForAddActors) {
							break;
						}
					}
					LinkedList<BPMNParticipant> currListClone = (LinkedList<BPMNParticipant>) currList.clone();
					currListClone.addAll(remaining.get(j));
					if (currListClone.size() != bpmnEx.getAmountAddActors()) {
						throw new NotEnoughAddActorsException(bpmnEx.getAmountAddActors() + " verifiers needed for "
								+ bpmnEx.getId() + " but only " + currListClone.size() + " found!");
					}

					AdditionalActors addActors = new AdditionalActors(currBrt, currListClone);
					additionalActorsForBrt.add(addActors);

				}
			} else {
				if (currList.size() != bpmnEx.getAmountAddActors()) {
					throw new NotEnoughAddActorsException(bpmnEx.getAmountAddActors() + " verifiers needed for "
							+ bpmnEx.getId() + " but only " + currList.size() + " found!");
				}
				AdditionalActors addActors = new AdditionalActors(currBrt, currList);
				additionalActorsForBrt.add(addActors);
			}

		}

		if (additionalActorsForBrt.isEmpty()) {
			throw new NotEnoughAddActorsException("No possible combination of verifiers for " + currBrt.getId());
		}

		return additionalActorsForBrt;

	}

	private void mapCamundaElements() throws Exception {

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
			StartEvent startEvent = this.modelInstance.getModelElementsByType(StartEvent.class).iterator().next();
			EndEvent endEvent = this.modelInstance.getModelElementsByType(EndEvent.class).iterator().next();
			this.mapSuccessorsAndPredecessors(startEvent, endEvent, new LinkedList<SequenceFlow>(),
					new ArrayList<Label>());

			if (this.modelWithLanes) {
				this.storeLanePerTask();
			} else {
				this.addParticipantToTask();
			}
			this.computePrivateSphere();
			this.mapDataObjects();
			this.mapAssociationsAndAnotations();
			this.mapDefaultTroubleShooter();

			// set last writer lists for the brts
			for (BPMNBusinessRuleTask brt : this.businessRuleTasks) {
				this.mapDecisions(brt);
				for (BPMNDataObject dataO : brt.getDataObjects()) {
					ArrayList<BPMNTask> lastWritersForDataO = this.getLastWriterListForDataObject(brt, dataO);
					brt.getLastWriterList().putIfAbsent(dataO, lastWritersForDataO);
				}
			}
		} catch (Exception e) {
			throw new Exception("Error in mapAndCompute() method!" + e.getMessage());
		}

	}

	private void mapProcessElements() throws Exception {
		Collection<FlowNode> processNodes = this.modelInstance.getModelElementsByType(FlowNode.class);
		for (FlowNode node : processNodes) {
			this.mapCamundaFlowNodes(node);
		}
	}

	private void mapSuccessorsAndPredecessors(FlowNode currentNode, FlowNode endNode, LinkedList<SequenceFlow> stack,
			ArrayList<Label> currentLabels) {
		// route depth first through the process and add the successors and predecessors
		// to the nodes

		stack.addAll(currentNode.getOutgoing());

		if (stack.isEmpty()) {
			return;
		}

		while (!stack.isEmpty()) {
			SequenceFlow currentSeqFlow = stack.pop();

			FlowNode targetFlowNode = currentSeqFlow.getTarget();

			BPMNElement targetBPMNElement = this.getBPMNNodeById(targetFlowNode.getId());
			BPMNElement currentBPMNElement = this.getBPMNNodeById(currentNode.getId());

			if ((!currentLabels.isEmpty()) && currentBPMNElement.getLabelHasBeenSet() == false) {
				boolean setLabel = true;
				if (currentBPMNElement instanceof BPMNExclusiveGateway) {
					BPMNExclusiveGateway gtw = (BPMNExclusiveGateway) currentBPMNElement;
					if (gtw.getType().contentEquals("join")) {
						setLabel = false;
					}
				}
				if (setLabel) {
					currentBPMNElement.addLabels(currentLabels);
					currentBPMNElement.setLabelHasBeenSet(true);
				}
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
					Label label = new Label(bpmnEx.getName().trim(), currentSeqFlow.getName().trim());
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
					if ((!currentLabels.isEmpty()) && currentBPMNElement.getLabelHasBeenSet() == false) {
						currentBPMNElement.addLabels(currentLabels);
						currentBPMNElement.setLabelHasBeenSet(true);
					}
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

	public BPMNElement mapCamundaFlowNodes(FlowNode node) throws Exception {
		BPMNElement mappedNode = null;

		// Check if the FlowNode has already been mapped
		for (BPMNElement e : this.processElements) {
			if (node.getId().equals(e.getId())) {
				return e;
			}
		}

		if (node instanceof Task) {
			if (node instanceof BusinessRuleTask) {
				if (this.modelWithLanes) {
					mappedNode = new BPMNBusinessRuleTask(node.getId().trim(), node.getName().trim());
				} else {
					mappedNode = new BPMNBusinessRuleTask(node.getId().trim(),
							node.getName().trim().substring(0, node.getName().indexOf("[")).trim());
				}
				FlowNode nextNode = node.getOutgoing().iterator().next().getTarget();
				if (nextNode instanceof ExclusiveGateway && nextNode.getOutgoing().size() >= 2) {
					this.businessRuleTasks.add((BPMNBusinessRuleTask) mappedNode);
				}

			} else {
				if (this.modelWithLanes) {
					mappedNode = new BPMNTask(node.getId().trim(), node.getName().trim());
				} else {
					mappedNode = new BPMNTask(node.getId().trim(),
							node.getName().trim().substring(0, node.getName().indexOf("[")).trim());
				}
			}

		} else if (node instanceof ParallelGateway) {
			if (node.getIncoming().size() >= 2 && node.getOutgoing().size() == 1) {
				mappedNode = new BPMNParallelGateway(node.getId().trim(), node.getName().trim(), "join");
			} else if (node.getIncoming().size() == 1 && node.getOutgoing().size() >= 2) {
				mappedNode = new BPMNParallelGateway(node.getId().trim(), node.getName().trim(), "split");

			}
		} else if (node instanceof ExclusiveGateway) {
			if (node.getIncoming().size() >= 2 && node.getOutgoing().size() == 1) {
				mappedNode = new BPMNExclusiveGateway(node.getId().trim(), node.getName().trim(), "join");
			} else if (node.getIncoming().size() == 1 && node.getOutgoing().size() >= 2) {
				mappedNode = new BPMNExclusiveGateway(node.getId().trim(), node.getName().trim(), "split");
			}
		} else if (node instanceof StartEvent) {
			mappedNode = new BPMNStartEvent(node.getId().trim());
			this.startEvent = (BPMNStartEvent) mappedNode;
		} else if (node instanceof EndEvent) {
			mappedNode = new BPMNEndEvent(node.getId().trim());
			this.endEvent = (BPMNEndEvent) mappedNode;
		}
		this.processElements.add(mappedNode);
		return mappedNode;

	}

	public BPMNElement getBPMNNodeById(String id) {
		for (BPMNElement e : this.processElements) {
			if (e.getId().equals(id)) {
				return e;
			}
		}
		return null;
	}

	public void storeLanePerTask() {
		// go through each lane of the process and store the lane as a participant to
		// the tasks
		if (this.modelWithLanes) {
			for (Lane l : this.modelInstance.getModelElementsByType(Lane.class)) {
				BPMNParticipant lanePart = new BPMNParticipant(l.getId().trim(), l.getName().trim());
				if (!privateSphere.contains(lanePart)) {
					this.privateSphere.add(lanePart);
				}
				for (FlowNode flowNode : l.getFlowNodeRefs()) {
					for (BPMNElement t : this.processElements) {
						if (t instanceof BPMNTask && flowNode.getId().trim().equals(t.getId().trim())) {
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
					if (t instanceof BPMNTask && task.getId().trim().equals(t.getId().trim())) {
						String taskName = task.getName().trim();
						String participantName = taskName.substring(taskName.indexOf("["), taskName.indexOf("]") + 1);
						boolean partAlreadyMapped = false;
						for (BPMNParticipant part : this.privateSphere) {
							if (part.getName().contentEquals(participantName)) {
								((BPMNTask) t).setParticipant(part);
								partAlreadyMapped = true;
								break;
							}
						}
						if (!partAlreadyMapped) {
							BPMNParticipant participant = new BPMNParticipant(participantName);
							((BPMNTask) t).setParticipant(participant);
							this.privateSphere.add(participant);
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
		if (this.privateSphere.isEmpty()) {
			this.computePrivateSphere();
		}

		for (TextAnnotation text : modelInstance.getModelElementsByType(TextAnnotation.class)) {

			String str = text.getTextContent();
			String troubleshooter = "";

			if (str.toLowerCase().contains(("Default").toLowerCase())
					&& str.toLowerCase().contains(("[Troubleshooter]").toLowerCase())) {
				troubleshooter = str.substring(str.indexOf('{') + 1, str.indexOf('}')).trim();
				boolean troubleshooterExists = false;
				for (BPMNParticipant part : this.privateSphere) {
					if (part.getName().contentEquals(troubleshooter)) {
						this.troubleShooter = part;
						troubleshooterExists = true;
						break;
					}
				}
				if (!troubleshooterExists) {
					BPMNParticipant troubleShooter = new BPMNParticipant(troubleshooter);
					this.troubleShooter = troubleShooter;
				}
			}

		}

		// when there is no default troubleShooter -> randomly choose one from the
		// private sphere
		if (this.troubleShooter == null) {
			if (!this.privateSphere.isEmpty()) {
				this.troubleShooter = CommonFunctionality.getRandomItem(this.privateSphere);
			} else {
				// create a new troubleshooter
				BPMNParticipant defaultTroubleShooter = new BPMNParticipant("someTrustedThirdParty");
				this.troubleShooter = defaultTroubleShooter;
			}
		}

	}

	public void computePrivateSphere() {
		for (BPMNElement e : this.processElements) {
			if (e instanceof BPMNTask) {
				BPMNParticipant currParticipant = ((BPMNTask) e).getParticipant();
				if (!privateSphere.contains(currParticipant)) {
					this.privateSphere.add(currParticipant);
				}
			}
		}
	}

	public void mapAssociationsAndAnotations() throws Exception {
		for (BPMNDataObject bpmno : this.dataObjects) {
			for (Task t : this.modelInstance.getModelElementsByType(Task.class)) {
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
					this.mapAnnotations((BPMNExclusiveGateway) this.getBPMNNodeById(xor.getId()));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					throw e;
				}
			}
		}

		for (ExclusiveGateway xor : this.modelInstance.getModelElementsByType(ExclusiveGateway.class)) {
			BPMNExclusiveGateway xorGtw = (BPMNExclusiveGateway) this.getBPMNNodeById(xor.getId());
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

			if (partsInMandatoryConstraintsForGtw.size() > xorGtw.getAmountAddActors()) {
				throw new Exception("Mandatory participants > amount verifiers needed at " + xorGtw.getId() + " ("
						+ partsInMandatoryConstraintsForGtw.size() + " > " + xorGtw.getAmountAddActors() + ")");
			}

		}

		for (BusinessRuleTask brt : this.modelInstance.getModelElementsByType(BusinessRuleTask.class)) {
			this.mapDecisions((BPMNBusinessRuleTask) this.getBPMNNodeById(brt.getId()));
		}
	}

	private void mapAnnotations(BPMNExclusiveGateway gtw) throws Exception {
		// map the annotated amount of needed voters to the BPMNExclusiveGateways
		// map the sphere connected to the gateway

		if (gtw.getAmountAddActors() > this.privateSphere.size()) {
			throw new Exception("Amount of verifiers for " + gtw.getId() + " can not be > private sphere!");
		}

		for (TextAnnotation text : modelInstance.getModelElementsByType(TextAnnotation.class)) {
			for (Association a : modelInstance.getModelElementsByType(Association.class)) {
				if (a.getAttributeValue("sourceRef").equals(gtw.getId())
						&& a.getAttributeValue("targetRef").equals(text.getId())) {
					String str = text.getTextContent().trim();
					if (!(str.contains("[") && str.contains("]") && str.contains("{") && str.contains("}"))) {
						throw new Exception("Annotation: " + a.getId() + " not modeled in correct form [...]{...}");
					}
					if (CommonFunctionality.containsIgnoreCase(str, "[AdditionalActors]")
							|| CommonFunctionality.containsIgnoreCase(str, "[Verifiers]")) {
						String subStr = str.substring(str.indexOf('{') + 1, str.indexOf('}'));
						String[] split = subStr.split(",");

						if (split.length == 3) {
							// there is a tuple of the form
							// (amountAdditionalActors,additionalActorsSameChoice,amountLoops)
							try {
								gtw.setAmountAddActors(Integer.parseInt(split[0]));
								gtw.setAmountAddActorsSameChoice(Integer.parseInt(split[1]));
								gtw.setAmountLoops(Integer.parseInt(split[2]));
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else if (split.length == 2) {
							// there is a tuple of the form (amountAdditionalActors,
							// additionalActorsSameChoice)
							try {
								gtw.setAmountAddActors(Integer.parseInt(split[0]));
								gtw.setAmountAddActorsSameChoice(Integer.parseInt(split[1]));
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else if (split.length == 1) {
							try {
								gtw.setAmountAddActors(Integer.parseInt(split[0]));
							} catch (Exception e) {
								e.printStackTrace();
							}
						}

					} else if (CommonFunctionality.containsIgnoreCase(str, "[Sphere]")) {
						String subStr = str.substring(str.indexOf('{') + 1, str.indexOf('}'));
						gtw.setSphere(subStr);
					} else if (CommonFunctionality.containsIgnoreCase(str, "[ExcludeParticipantConstraint]")) {
						String partName = str.substring(str.indexOf('{') + 1, str.indexOf('}')).trim();

						BPMNParticipant participant = this.getBPMNParticipantFromList(this.privateSphere, partName);

						if (participant != null) {
							boolean constrExists = false;
							for (ExcludeParticipantConstraint exclConst : this.excludeParticipantConstraints) {
								// check if constraint already exists
								if (exclConst.getParticipantToExclude().equals(participant)) {
									// constraint already exists
									gtw.getConstraints().add(exclConst);
									if (!this.excludeParticipantConstraints.contains(exclConst)) {
										this.excludeParticipantConstraints.add(exclConst);
									}
									constrExists = true;
								}
							}
							if (constrExists == false) {

								for (Constraint constr : gtw.getConstraints()) {
									if (constr instanceof MandatoryParticipantConstraint) {
										if (((MandatoryParticipantConstraint) constr).getMandatoryParticipant()
												.equals(participant)) {
											throw new Exception("Participant: " + partName
													+ " can not be excluded and mandatory at the same time for "
													+ gtw.getName());
										}
									}
								}

								if (!this.privateSphere.contains(participant)) {
									throw new Exception(
											"Constraints to exclude participants must be on participants from the private sphere! ");
								}
								ExcludeParticipantConstraint epc = new ExcludeParticipantConstraint(participant);
								gtw.getConstraints().add(epc);
								if (!this.excludeParticipantConstraints.contains(epc)) {
									this.excludeParticipantConstraints.add(epc);
								}
							}

						} else {
							throw new Exception(partName
									+ " is not part of the private sphere! Constraints to exclude participants must be on participants from the private sphere!");
						}

					} else if (CommonFunctionality.containsIgnoreCase(str, "[MandatoryParticipantConstraint]")) {
						String partName = str.substring(str.indexOf('{') + 1, str.indexOf('}')).trim();

						BPMNParticipant participant = this.getBPMNParticipantFromList(this.privateSphere, partName);

						if (participant != null) {
							boolean constrExists = false;
							for (MandatoryParticipantConstraint mandConst : this.mandatoryParticipantConstraints) {
								// check if constraint already exists
								if (mandConst.getMandatoryParticipant().equals(participant)) {
									// constraint already exists
									gtw.getConstraints().add(mandConst);
									if (!this.mandatoryParticipantConstraints.contains(mandConst)) {
										this.mandatoryParticipantConstraints.add(mandConst);
									}
									constrExists = true;
								}
							}
							if (constrExists == false) {
								BPMNElement predecessor = gtw.getPredecessors().iterator().next();
								if (predecessor instanceof BPMNBusinessRuleTask) {
									BPMNBusinessRuleTask brt = (BPMNBusinessRuleTask) predecessor;
									if (brt.getParticipant().equals(participant)) {
										throw new Exception(
												"Mandatory participant can not be the additional actor of the brt!");
									}
								}

								for (Constraint constr : gtw.getConstraints()) {
									if (constr instanceof ExcludeParticipantConstraint) {
										if (((ExcludeParticipantConstraint) constr).getParticipantToExclude()
												.equals(participant)) {
											throw new Exception("Participant: " + partName
													+ " can not be mandatory and excluded at the same time for "
													+ gtw.getName());
										}
									}
								}

								if (!this.privateSphere.contains(participant)) {
									throw new Exception(
											"Constraints to make participants mandatory must be on participants from the private sphere! ");
								}
								MandatoryParticipantConstraint mpc = new MandatoryParticipantConstraint(participant);
								gtw.getConstraints().add(mpc);
								if (!this.mandatoryParticipantConstraints.contains(mpc)) {
									this.mandatoryParticipantConstraints.add(mpc);
								}
							}

						} else {
							throw new Exception(partName
									+ " is not part of the private sphere! Constraints to make participants mandatory must be on participants from the private sphere!");
						}
					}

				}
			}
		}

	}

	public BPMNParticipant getBPMNParticipantFromList(List<BPMNParticipant> list, String nameWithoutBrackets) {
		for (BPMNParticipant p : list) {
			if (p.getNameWithoutBrackets().contentEquals(nameWithoutBrackets)) {
				return p;
			}
		}
		return null;
	}

	public BPMNParticipant getBPMNParticipantFromSet(Set<BPMNParticipant> set, String name) {
		for (BPMNParticipant p : set) {
			if (p.getNameWithoutBrackets().contentEquals(name)) {
				return p;
			}
		}
		return null;
	}

	private void mapSphereAnnotations(Task task) throws Exception {
		BPMNTask element = (BPMNTask) this.getBPMNNodeById(task.getId());
		String dataObject = "";
		String defaultSphere = "";
		for (TextAnnotation text : modelInstance.getModelElementsByType(TextAnnotation.class)) {
			// If at a writing task no corresponding sphere is annotated, take the default
			// one
			if (text.getTextContent().startsWith("Default:")) {
				String str = text.getTextContent().trim();
				dataObject = str.substring(str.indexOf('[') + 1, str.indexOf(']'));
				defaultSphere = str.substring(str.indexOf('{') + 1, str.indexOf('}'));
			}
			for (Association a : modelInstance.getModelElementsByType(Association.class)) {
				for (BPMNDataObject bpmndo : element.getDataObjects()) {
					// Map the default Spheres for the data objects to the corresponding writing
					// tasks
					if (bpmndo.getNameIdWithoutBrackets().equals(dataObject) && bpmndo.getWriters().contains(element)) {
						// check if the sphere is valid
						if (!(defaultSphere.contentEquals(this.privateSphereKey)
								|| defaultSphere.contentEquals(this.staticSphereKey)
								|| defaultSphere.contentEquals(this.weakDynamicSphereKey)
								|| defaultSphere.contentEquals(this.strongDynamicSphereKey))) {
							throw new Exception("Sphere: " + defaultSphere + " is not a valid sphere!");
						}

						// attach the defaultSphere to the dataObject
						if (bpmndo.getDefaultSphere().isEmpty()) {
							bpmndo.setDefaultSphere(defaultSphere);
						}
						element.getSphereAnnotation().putIfAbsent(bpmndo, defaultSphere);
					}

					if (a.getAttributeValue("sourceRef").equals(element.getId())
							&& a.getAttributeValue("targetRef").equals(text.getId())) {
						String str = text.getTextContent().trim();
						// characters within square brackets specify the Data object e.g. [D1]
						String s = str.substring(str.indexOf('[') + 1, str.indexOf(']'));
						// The Sphere for the data object is between curly brackets
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

	public ArrayList<BPMNTask> getLastWriterListForDataObject(BPMNBusinessRuleTask brt, BPMNDataObject data)
			throws NullPointerException, InterruptedException, Exception {
		LinkedList<LinkedList<FlowNode>> list = CommonFunctionality.getAllPathsBetweenNodes(this.modelInstance,
				this.startEvent.getId(), brt.getId());
		ArrayList<BPMNTask> lastWriterList = new ArrayList<BPMNTask>();
		for (LinkedList<FlowNode> path : list) {
			for (int i = path.size() - 1; i >= 0; i--) {
				FlowNode f = path.get(i);
				if (f instanceof Task) {
					BPMNTask task = (BPMNTask) this.getBPMNNodeById(f.getId());
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

	public boolean isParticipantInSDForOriginWithExcludedTasks(BPMNParticipant participant, BPMNDataObject dataO,
			BPMNTask origin, BPMNBusinessRuleTask currPositionBrt, LinkedList<AdditionalActors> addActors,
			LinkedList<BPMNBusinessRuleTask> brtsToExcludeAddActors,
			LinkedList<LinkedList<BPMNElement>> pathFromOriginOverCurrPositionBrtToEnd) {

		boolean participantIsSd = true;

		// only look at those paths where origin writes the data for the currPositionBrt

		for (LinkedList<BPMNElement> path : pathFromOriginOverCurrPositionBrtToEnd) {
			HashSet<BPMNParticipant> foundOnPathWithAddActors = new HashSet<BPMNParticipant>();
			boolean pastCurrPositionBrt = false;
			for (int i = 0; i < path.size(); i++) {
				BPMNElement el = path.get(i);
				if (el instanceof BPMNTask) {
					BPMNTask currTask = (BPMNTask) el;
					if (dataO.getReaders().contains(currTask)) {
						// currTask is a reader of the dataO
						foundOnPathWithAddActors.add(currTask.getParticipant());
						if (currTask instanceof BPMNBusinessRuleTask) {

							if (currTask.equals(currPositionBrt)) {
								pastCurrPositionBrt = true;
							}

							if (brtsToExcludeAddActors != null && !brtsToExcludeAddActors.isEmpty()) {
								if (!brtsToExcludeAddActors.contains(currTask)) {
									for (AdditionalActors addActor : addActors) {
										BPMNBusinessRuleTask brt = addActor.getCurrBrt();
										if (brt.equals(currTask) && dataO.getReaders().contains(currTask)) {
											// currTask is a brt that reads the data object
											// add additional readers
											foundOnPathWithAddActors.addAll(addActor.getAdditionalActors());
										}
									}
								}
							} else {
								// no brts excluded
								for (AdditionalActors addActor : addActors) {
									BPMNBusinessRuleTask brt = addActor.getCurrBrt();
									if (brt.equals(currTask) && dataO.getReaders().contains(currTask)) {
										// currTask is a brt that reads the data object
										// add additional readers
										foundOnPathWithAddActors.addAll(addActor.getAdditionalActors());
									}
								}
							}

						}
					}

					if (dataO.getWriters().contains(currTask)) {
						// currTask is a writer
						// check if is the origin
						if (currTask.equals(origin)) {
							// origin is always in SD
							foundOnPathWithAddActors.add(currTask.getParticipant());
						} else {
							// another writer found on path
							// stop here
							// actors after that writer will not read dataO from inspected origin
							i = path.size();
						}

					}

				}

			}

			if (pastCurrPositionBrt) {
				// make sure to only inspect paths that contain the currPositionBrt and no other
				// origin writes the data object in between
				if (!foundOnPathWithAddActors.contains(participant)) {
					// participant is not sd for the path!
					return false;
				}

			}

		}

		return participantIsSd;
	}

	public HashSet<BPMNParticipant> getSdActors(BPMNDataObject dataO, BPMNTask origin,
			BPMNBusinessRuleTask currPositionBrt,
			HashMap<BPMNBusinessRuleTask, LinkedList<AdditionalActors>> addActorsMap,
			LinkedList<LinkedList<BPMNElement>> paths) throws InterruptedException {

		HashSet<BPMNParticipant> sdListAddActors = new HashSet<BPMNParticipant>();
		sdListAddActors.addAll(this.privateSphere);

		HashSet<BPMNParticipant> readersToBeFound = new HashSet<BPMNParticipant>();
		readersToBeFound.addAll(this.privateSphere);

		for (LinkedList<BPMNElement> path : paths) {
			if (Thread.currentThread().isInterrupted()) {
				System.err.println("Interrupted! " + Thread.currentThread().getName());
				throw new InterruptedException();
			}
			HashSet<BPMNParticipant> foundOnPathWithAddActors = new HashSet<BPMNParticipant>();
			for (int i = 0; i < path.size(); i++) {
				BPMNElement el = path.get(i);
				if (el instanceof BPMNTask) {
					BPMNTask currTask = (BPMNTask) el;
					if (dataO.getReaders().contains(currTask)) {
						// currTask is a reader of the dataO
						foundOnPathWithAddActors.add(currTask.getParticipant());
						if (currTask instanceof BPMNBusinessRuleTask) {
							BPMNBusinessRuleTask currBrt = (BPMNBusinessRuleTask) currTask;
							if (addActorsMap != null) {
								for (Entry<BPMNBusinessRuleTask, LinkedList<AdditionalActors>> addActorsEntry : addActorsMap
										.entrySet()) {
									BPMNBusinessRuleTask brt = addActorsEntry.getKey();
									if (brt.equals(currTask) && dataO.getReaders().contains(currTask)) {
										// currTask is a brt that reads the data object
										// add additional readers
										for (AdditionalActors addActors : addActorsEntry.getValue()) {
											foundOnPathWithAddActors.addAll(addActors.getAdditionalActors());
										}
									}
								}
							}

							// add participants that are mandatory
							BPMNExclusiveGateway exclGtw = (BPMNExclusiveGateway) currBrt.getSuccessors().iterator()
									.next();
							LinkedList<Constraint> constraints = exclGtw.getConstraints();
							for (Constraint constraint : constraints) {
								if (constraint instanceof MandatoryParticipantConstraint) {
									foundOnPathWithAddActors.add(
											((MandatoryParticipantConstraint) constraint).getMandatoryParticipant());
								}
							}
						}
					}

					if (dataO.getWriters().contains(currTask)) {
						// currTask is a writer
						// check if is the origin
						if (currTask.equals(origin)) {
							// origin is always in SD
							sdListAddActors.add(currTask.getParticipant());
							foundOnPathWithAddActors.add(currTask.getParticipant());
						} else {
							// another writer found on some path
							// stop here
							// actors after that writer will not read dataO from origin
							i = path.size();
						}

					}

				}

			}

			HashSet<BPMNParticipant> notFoundOnPathWithAddActors = new HashSet<BPMNParticipant>();
			notFoundOnPathWithAddActors.addAll(readersToBeFound);
			notFoundOnPathWithAddActors.removeAll(foundOnPathWithAddActors);
			sdListAddActors.removeAll(notFoundOnPathWithAddActors);

		}
		return sdListAddActors;
	}

	public HashSet<BPMNTask> getWdTasks(BPMNDataObject dataO, BPMNTask origin,
			LinkedList<LinkedList<BPMNElement>> paths) {

		HashSet<BPMNTask> wdList = new HashSet<BPMNTask>();

		for (LinkedList<BPMNElement> path : paths) {
			for (int i = 0; i < path.size(); i++) {
				BPMNElement el = path.get(i);
				if (el instanceof BPMNTask) {
					BPMNTask currTask = (BPMNTask) el;
					if (dataO.getReaders().contains(currTask)) {
						// currTask is a reader of the dataO
						wdList.add(currTask);
					}

					if (dataO.getWriters().contains(currTask)) {
						// currTask is a writer
						// check if is the origin
						if (currTask.equals(origin)) {
							// origin is always in SD
							wdList.add(currTask);
						} else {
							// another writer found on path
							// stop here
							i = path.size();
						}

					}

				}

			}

		}

		return wdList;
	}

	public synchronized LinkedList<LinkedList<BPMNElement>> goDfs(BPMNElement currentNode, BPMNElement targetNode1,
			BPMNElement targetNode2, LinkedList<BPMNElement> stack, LinkedList<BPMNElement> path,
			LinkedList<LinkedList<BPMNElement>> paths) throws Exception {
		// route depth first through the process
		// with the help of labels we only get those paths that contain both targetNodes
		// first go dfs till targetNode1 is found
		// then go dfs till targetNode2 is found

		if (Thread.currentThread().isInterrupted()) {
			System.err.println("Interrupted! " + Thread.currentThread().getName());
			throw new InterruptedException();
		}
		if (targetNode1 == null) {
			throw new Exception("TargetNode1 can not be null!");
		}
		if (targetNode2 == null) {
			throw new Exception("TargetNode2 can not be null!");
		}

		if (path.isEmpty()) {
			path.add(currentNode);
		}

		if (!currentNode.getId().contentEquals(targetNode2.getId())) {
			if (currentNode instanceof BPMNExclusiveGateway) {
				BPMNExclusiveGateway gtw = (BPMNExclusiveGateway) currentNode;
				if (gtw.getType().contentEquals("split")) {
					// only add those successors, that lead to the targetNode1
					// and go over the currPosition
					ArrayList<Label> targetNodeLabels = targetNode1.getLabels();

					for (BPMNElement successor : gtw.getSuccessors()) {
						ArrayList<Label> successorLabels = successor.getLabels();
						boolean allDifferent = true;
						for (Label label : targetNodeLabels) {
							if (label.getName().trim().contentEquals(gtw.getName())) {
								allDifferent = false;
								for (Label succLabel : successorLabels) {
									if (succLabel.getLabel().contentEquals(label.getLabel())) {
										stack.add(successor);
									}
								}

							}

						}

						if (allDifferent) {
							stack.add(successor);
						}

					}

				} else {
					stack.addAll(currentNode.getSuccessors());
				}

			} else {
				stack.addAll(currentNode.getSuccessors());
			}

		}

		if (stack.isEmpty()) {
			return paths;
		}

		while (!stack.isEmpty()) {
			LinkedList<BPMNElement> newPath = new LinkedList<BPMNElement>();
			newPath.addAll(path);
			BPMNElement nextNode = stack.poll();
			newPath.add(nextNode);
			if (nextNode.getId().contentEquals(targetNode1.getId())
					&& !nextNode.getId().contentEquals(targetNode2.getId())) {
				// go dfs till endNode is found
				// i.e. set targetNode1 to be targetNode2
				goDfs(nextNode, targetNode2, targetNode2, new LinkedList<BPMNElement>(), newPath, paths);
			} else if (nextNode.getId().contentEquals(targetNode2.getId())) {
				paths.add(newPath);
			} else {
				goDfs(nextNode, targetNode1, targetNode2, new LinkedList<BPMNElement>(), newPath, paths);
			}
		}

		return paths;

	}

	public HashSet<BPMNParticipant> computeLambdaActors(HashSet<BPMNParticipant> set1, HashSet<BPMNParticipant> set2) {
		HashSet<BPMNParticipant> setWithDifferentParticipants = new HashSet<BPMNParticipant>();
		if (set1.size() >= set2.size()) {
			setWithDifferentParticipants.addAll(set1);
			setWithDifferentParticipants.removeAll(set2);
		} else {
			setWithDifferentParticipants.addAll(set2);
			setWithDifferentParticipants.removeAll(set1);
		}
		return setWithDifferentParticipants;
	}

	public HashSet<BPMNParticipant> getParticipantSetFromElementSet(HashSet<BPMNTask> tasks) {
		HashSet<BPMNParticipant> participantSet = new HashSet<BPMNParticipant>();
		for (BPMNTask task : tasks) {
			participantSet.add(task.getParticipant());
		}
		return participantSet;
	}

	public HashSet<BPMNTask> filterBrts(HashSet<BPMNTask> tasks) {
		HashSet<BPMNTask> brtSet = new HashSet<BPMNTask>();
		for (BPMNTask task : tasks) {
			if (this.businessRuleTasks.contains(task)) {
				brtSet.add(task);
			}
		}
		return brtSet;
	}

	private boolean computeMeasure(String requiredSphereOfDataObject, String requiredSphereOfMeasure) {
		// check if the measure should be computed
		// i.e. if requiredSphereOfDataObject is weak-dynamic -> alpha and beta measure
		// should be computed but not gamma

		if (requiredSphereOfDataObject.contentEquals(this.privateSphereKey)) {
			return false;
		}

		// basic comparison -> if the spheres are the same, return true
		if (requiredSphereOfDataObject.contentEquals(requiredSphereOfMeasure)) {
			return true;
		} else {
			if (requiredSphereOfDataObject.contentEquals(this.strongDynamicSphereKey)) {
				return true;
			}

			else if (requiredSphereOfDataObject.contentEquals(this.weakDynamicSphereKey)) {
				if (requiredSphereOfMeasure.contentEquals(this.staticSphereKey)) {
					return true;
				}
			}
		}
		return false;

	}

	private boolean requiredSphereIsAtLeast(String requiredSphereOfDataObject, String requiredSphereOfMeasure) {

		// basic comparison -> if the spheres are the same, return true
		if (requiredSphereOfDataObject.contentEquals(requiredSphereOfMeasure)) {
			return true;
		} else {
			if (requiredSphereOfDataObject.contentEquals(this.strongDynamicSphereKey)) {
				return true;
			}

			else if (requiredSphereOfDataObject.contentEquals(this.weakDynamicSphereKey)) {
				if (requiredSphereOfMeasure.contentEquals(this.staticSphereKey)) {
					return true;
				}
				if (requiredSphereOfMeasure.contentEquals(this.privateSphereKey)) {
					return true;
				}
			} else if (requiredSphereOfDataObject.contentEquals(this.staticSphereKey)) {
				if (requiredSphereOfMeasure.contentEquals(this.privateSphereKey)) {
					return true;
				}
			}

		}
		return false;

	}

	public LinkedList<PModelWithAdditionalActors> getCheapestCombinationsOfAdditionalActors(
			LinkedList<PModelWithAdditionalActors> pModels) {

		LinkedList<PModelWithAdditionalActors> currCheapestPModels = new LinkedList<PModelWithAdditionalActors>();
		currCheapestPModels.add(pModels.get(0));
		for (int i = 1; i < pModels.size(); i++) {
			PModelWithAdditionalActors currPModel = pModels.get(i);
			if (currPModel.getSumMeasure() < currCheapestPModels.get(0).getSumMeasure()) {
				currCheapestPModels.clear();
				currCheapestPModels.add(currPModel);
			} else if (currPModel.getSumMeasure() == currCheapestPModels.get(0).getSumMeasure()) {
				currCheapestPModels.add(currPModel);
			}
		}

		return currCheapestPModels;

	}

	public LinkedList<LinkedList<BPMNParticipant>> getPotentialAddActorsListsForBrt(BPMNBusinessRuleTask brt) {

		LinkedList<LinkedList<BPMNParticipant>> listOfParticipants = new LinkedList<LinkedList<BPMNParticipant>>();
		// on index 0 -> all participantsToCombineAsAdditionalActors
		// on index 1 -> all mandatoryParticipants

		BPMNParticipant partOfBrt = brt.getParticipant();
		BPMNExclusiveGateway bpmnEx = (BPMNExclusiveGateway) brt.getSuccessors().iterator().next();
		// get all the possible combinations of participants for the brt
		// consider the constraints e.g. participants may be excluded or mandatory
		LinkedList<BPMNParticipant> participantsToCombineAsAdditionalActors = new LinkedList<BPMNParticipant>();

		// add all participants of the private sphere except the actor of the brt itself
		participantsToCombineAsAdditionalActors.addAll(this.privateSphere);
		participantsToCombineAsAdditionalActors.remove(partOfBrt);

		LinkedList<BPMNParticipant> mandatoryParticipantsAsAdditionalActors = new LinkedList<BPMNParticipant>();

		for (Constraint constraint : bpmnEx.getConstraints()) {
			if (constraint instanceof ExcludeParticipantConstraint) {
				// exclude the participants due to constraints
				BPMNParticipant partToRemove = ((ExcludeParticipantConstraint) constraint).getParticipantToExclude();
				participantsToCombineAsAdditionalActors.remove(partToRemove);
			} else if (constraint instanceof MandatoryParticipantConstraint) {
				BPMNParticipant mandatoryPart = ((MandatoryParticipantConstraint) constraint).getMandatoryParticipant();
				// add the mandatory participant to the head of the list
				participantsToCombineAsAdditionalActors.remove(mandatoryPart);
				participantsToCombineAsAdditionalActors.addFirst(mandatoryPart);

				if (!mandatoryParticipantsAsAdditionalActors.contains(mandatoryPart)) {
					mandatoryParticipantsAsAdditionalActors.add(mandatoryPart);
				}
			}
		}

		listOfParticipants.add(participantsToCombineAsAdditionalActors);
		listOfParticipants.add(mandatoryParticipantsAsAdditionalActors);

		return listOfParticipants;
	}

	public HashMap<BPMNDataObject, LinkedList<HashSet<?>>> computeStaticSpherePerDataObject() {
		// compute static sphere per data object without additional actors
		HashMap<BPMNDataObject, LinkedList<HashSet<?>>> mapToReturn = new HashMap<BPMNDataObject, LinkedList<HashSet<?>>>();

		for (BPMNDataObject dataO : this.dataObjects) {
			LinkedList<HashSet<?>> list = new LinkedList<HashSet<?>>();
			// list on index 0 -> set of participants in static sphere
			// list on index 1 -> set of brts that read dataO
			String sphereOfDataO = dataO.getDefaultSphere();
			boolean computeAlphaMeasure = this.computeMeasure(sphereOfDataO, this.staticSphereKey);

			if (computeAlphaMeasure) {
				HashSet<BPMNParticipant> staticSphereSet = new HashSet<BPMNParticipant>(dataO.getStaticSphere());
				HashSet<BPMNTask> readerBrts = new HashSet<BPMNTask>();
				for (BPMNElement reader : dataO.getReaders()) {
					if (this.businessRuleTasks.contains(reader)) {
						readerBrts.add((BPMNTask) reader);
					}
				}

				list.addFirst(staticSphereSet);
				list.addLast(readerBrts);
				mapToReturn.putIfAbsent(dataO, list);
			}
		}

		return mapToReturn;
	}

	public HashMap<BPMNDataObject, LinkedList<LinkedList<HashSet<?>>>> computeWdSphere() {
		HashMap<BPMNDataObject, LinkedList<LinkedList<HashSet<?>>>> mapToReturn = new HashMap<BPMNDataObject, LinkedList<LinkedList<HashSet<?>>>>();
		// on index 0 -> HashSet only containing the origin
		// on index 1 -> HashSet with readerBrts
		// on index 2 -> HashSet with participants in wd sphere

		for (BPMNBusinessRuleTask brt : this.businessRuleTasks) {
			for (Entry<BPMNDataObject, ArrayList<BPMNTask>> originsPerDataObject : brt.getLastWriterList().entrySet()) {
				BPMNDataObject currDataObject = originsPerDataObject.getKey();
				String currDataObjectSphere = currDataObject.getDefaultSphere();
				boolean computeBetaMeasure = this.computeMeasure(currDataObjectSphere, this.weakDynamicSphereKey);
				if (computeBetaMeasure) {
					for (BPMNTask origin : originsPerDataObject.getValue()) {
						boolean wdAlreadyComputed = false;
						// check if wd for origin and data object has been computed already
						if (mapToReturn.containsKey(currDataObject)) {
							LinkedList<LinkedList<HashSet<?>>> wdSphereList = mapToReturn.get(currDataObject);

							for (int i = 0; i < wdSphereList.size() && !wdAlreadyComputed; i++) {
								BPMNTask alreadyVisitedOrigin = (BPMNTask) wdSphereList.get(i).get(0).iterator().next();
								if (alreadyVisitedOrigin.equals(origin)) {
									wdAlreadyComputed = true;
								}
							}
						}

						if (!wdAlreadyComputed) {
							// compute wd sphere for origin
							// get all participants in the wd sphere without additional actors at position
							// of the origin
							LinkedList<LinkedList<BPMNElement>> pathsBetweenOriginAndEnd;
							try {
								pathsBetweenOriginAndEnd = this.goDfs(origin, origin, this.endEvent,
										new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(),
										new LinkedList<LinkedList<BPMNElement>>());
								// get wd reader tasks
								HashSet<BPMNTask> wdTasks = this.getWdTasks(currDataObject, origin,
										pathsBetweenOriginAndEnd);
								// get wd reader participants
								HashSet<BPMNParticipant> wdReaderParticipants = this
										.getParticipantSetFromElementSet(wdTasks);
								// get wd brts that read from origin
								// to compute wd' later on by adding the additional actors of the brts
								HashSet<BPMNTask> readerBrts = this.filterBrts(wdTasks);

								// on index 0 -> HashSet of size 1 -> only containing the origin
								// on index 1 -> HashSet with readerBrts
								// on index 2 -> HashSet with participants in wd sphere
								LinkedList<HashSet<?>> listToReturn = new LinkedList<HashSet<?>>();
								HashSet<BPMNTask> originSet = new HashSet<BPMNTask>();
								originSet.add(origin);
								listToReturn.add(originSet);
								listToReturn.add(readerBrts);
								listToReturn.add(wdReaderParticipants);

								mapToReturn
										.computeIfAbsent(currDataObject, k -> new LinkedList<LinkedList<HashSet<?>>>())
										.add(listToReturn);

							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						}

					}
				}
			}
		}
		return mapToReturn;
	}

	public HashMap<Enums.AlgorithmToPerform, LinkedList<String>> getExecutionTimeMap() {
		return executionTimeMap;
	}

	public void setExecutionTimeMap(HashMap<Enums.AlgorithmToPerform, LinkedList<String>> executionTimeMap) {
		this.executionTimeMap = executionTimeMap;
	}

	public int getBound() {
		return this.bound;
	}

	public void setBoundForAddActorsInCluster(int bound) {
		this.bound = bound;
	}

	public HashMap<Double, LinkedList<BPMNParticipant>> getCheapestParticipantListStaticOrWD(BPMNBusinessRuleTask brt,
			LinkedList<BPMNParticipant> potentialAddActors,
			HashMap<BPMNDataObject, LinkedList<HashSet<?>>> staticSpherePerDataObject,
			HashMap<BPMNDataObject, LinkedList<LinkedList<HashSet<?>>>> wdSpherePerDataObject) {

		HashMap<Double, LinkedList<BPMNParticipant>> cheapestOnesWithCost = new HashMap<Double, LinkedList<BPMNParticipant>>();
		// how to compute the cheapest ones?
		// check if potential additional actor is in static or wd sphere (without
		// additional readers)
		// currently: prefer participant that is in highest sphere for all data objects
		// in sum

		// what about different parameters for alpha and beta?
		// what about the amount of readers/writers for data object?

		for (BPMNParticipant potentialAddActor : potentialAddActors) {
			double sphereSum = 0;

			for (BPMNDataObject dataO : brt.getDataObjects()) {
				LinkedList<HashSet<?>> staticSphere = staticSpherePerDataObject.get(dataO);
				if (staticSphere != null) {
					// when staticSphere==null -> dataObject is private -> every participant in
					// private sphere can be assigned without cost
					HashSet<BPMNParticipant> staticSphereParticipants = (HashSet<BPMNParticipant>) staticSphere.get(0);

					if (!staticSphereParticipants.contains(potentialAddActor)) {
						// potential add actor is in private sphere
						sphereSum++;
					}

					LinkedList<LinkedList<HashSet<?>>> wdSphereList = wdSpherePerDataObject.get(dataO);
					if (wdSphereList != null) {
						// wd sphere: check for each origin if potential add actor is wd
						for (int i = 0; i < wdSphereList.size(); i++) {
							HashSet<BPMNParticipant> wdSphere = (HashSet<BPMNParticipant>) wdSphereList.get(i).get(2);
							if (!wdSphere.contains(potentialAddActor)) {
								sphereSum++;
							}

						}
					}
				}
			}

			cheapestOnesWithCost.computeIfAbsent(sphereSum, k -> new LinkedList<BPMNParticipant>())
					.add(potentialAddActor);

		}
		return cheapestOnesWithCost;
	}

	public void addPModelIfCheapest(int boundForCheapestSolutions,
			LinkedList<PModelWithAdditionalActors> cheapestPModelsAlphaMeasure,
			PModelWithAdditionalActors pModelWithAdditionalActors) {
		if (cheapestPModelsAlphaMeasure.isEmpty()) {
			cheapestPModelsAlphaMeasure.add(pModelWithAdditionalActors);
		} else {
			if (pModelWithAdditionalActors.getSumMeasure() < cheapestPModelsAlphaMeasure.get(0).getSumMeasure()) {
				// new cheapest model found
				cheapestPModelsAlphaMeasure.clear();
				cheapestPModelsAlphaMeasure.add(pModelWithAdditionalActors);
			} else if (pModelWithAdditionalActors.getSumMeasure() == cheapestPModelsAlphaMeasure.get(0)
					.getSumMeasure()) {
				// check for bound
				// if bound <= 0 -> unbounded
				if (boundForCheapestSolutions <= 0) {
					cheapestPModelsAlphaMeasure.add(pModelWithAdditionalActors);
				} else {
					if (cheapestPModelsAlphaMeasure.size() < boundForCheapestSolutions) {
						cheapestPModelsAlphaMeasure.add(pModelWithAdditionalActors);
					}
				}

			}
		}

	}

	private void computeAlphaMeasure(PModelWithAdditionalActors pModelWithAdditionalActors,
			HashMap<BPMNDataObject, LinkedList<HashSet<?>>> staticSpherePerDataObject) throws InterruptedException {
		pModelWithAdditionalActors.setPrivateSphere(this.privateSphere);

		for (Entry<BPMNDataObject, LinkedList<HashSet<?>>> staticSpherePerDataO : staticSpherePerDataObject
				.entrySet()) {

			BPMNDataObject dataO = staticSpherePerDataO.getKey();
			Static_SphereEntry currStaticEntry = this.generateStaticSphereEntry(pModelWithAdditionalActors, dataO,
					staticSpherePerDataO);

			// compute lambda and set score
			HashSet<BPMNParticipant> differentActors = this.computeLambdaActors(
					currStaticEntry.getStaticSphereWithAdditionalActors(), currStaticEntry.getStaticSphere());
			currStaticEntry.setLambdaStaticSphere(differentActors);

			double lambda = differentActors.size();
			double score = lambda;

			// add to sum alpha measure
			double currAlphaScoreSum = pModelWithAdditionalActors.getAlphaMeasureSum();
			double newAlphaScoreSum = currAlphaScoreSum += score;
			pModelWithAdditionalActors.setAlphaMeasureSum(newAlphaScoreSum);
		}

	}

	public HashSet<BPMNBusinessRuleTask> getDependentBrts(BPMNParticipant participant, BPMNTask writer,
			BPMNDataObject dataO, LinkedList<LinkedList<BPMNElement>> pathsFromOriginToEnd,
			PModelWithAdditionalActors pModel) {
		// dependentBrts contains all brts, where there exists at least one instance
		// type where writer writes dataO and participant is an additional actor of the
		// brt
		HashSet<BPMNBusinessRuleTask> dependentBrts = new HashSet<BPMNBusinessRuleTask>();

		for (int i = 0; i < pathsFromOriginToEnd.size(); i++) {
			LinkedList<BPMNElement> path = pathsFromOriginToEnd.get(i);
			for (int j = 0; j < path.size(); j++) {
				BPMNElement pathEl = path.get(j);
				if (pathEl instanceof BPMNTask) {
					BPMNTask nextWriter = (BPMNTask) pathEl;
					if (dataO.getWriters().contains(nextWriter) && !nextWriter.equals(writer)) {
						// other writer found
						break;
					}
				}

				if (pathEl instanceof BPMNBusinessRuleTask) {
					BPMNBusinessRuleTask currBrt = (BPMNBusinessRuleTask) pathEl;
					LinkedList<AdditionalActors> addActorsList = pModel.getAdditionalActorsList();
					for (int addActorsIndex = 0; addActorsIndex < addActorsList.size(); addActorsIndex++) {
						AdditionalActors addActors = addActorsList.get(addActorsIndex);
						if (addActors.getCurrBrt().equals(currBrt)
								&& addActors.getCurrBrt().getDataObjects().contains(dataO)) {
							// check if the participant is an additional actor
							if (addActors.getAdditionalActors().contains(participant)) {
								dependentBrts.add(currBrt);
								addActorsIndex = addActorsList.size();
							}
						}
					}

				}

			}

		}

		return dependentBrts;

	}

	public HashSet<BPMNBusinessRuleTask> getDependentBrtsOfWriter(BPMNTask writer, BPMNDataObject dataO,
			LinkedList<LinkedList<BPMNElement>> pathsFromOriginToEnd) {

		HashSet<BPMNBusinessRuleTask> dependentBrts = new HashSet<BPMNBusinessRuleTask>();

		for (int i = 0; i < pathsFromOriginToEnd.size(); i++) {
			LinkedList<BPMNElement> path = pathsFromOriginToEnd.get(i);
			for (int j = 0; j < path.size(); j++) {
				BPMNElement pathEl = path.get(j);

				if (pathEl instanceof BPMNTask) {
					BPMNTask task = (BPMNTask) pathEl;
					if (dataO.getWriters().contains(task) && !task.equals(writer)) {
						// other writer found
						j = path.size();
					}
				}

				if (pathEl instanceof BPMNBusinessRuleTask) {
					BPMNBusinessRuleTask currBrt = (BPMNBusinessRuleTask) pathEl;
					dependentBrts.add(currBrt);
				}

			}

		}

		return dependentBrts;

	}

	public LinkedList<LinkedList<BPMNBusinessRuleTask>> getMinimalSubSetsWithSameSpheres(BPMNParticipant participant,
			HashSet<BPMNBusinessRuleTask> depT, HashMap<BPMNBusinessRuleTask, HashSet<BPMNParticipant>> spheres1,
			BPMNDataObject dataO, BPMNTask origin, BPMNBusinessRuleTask currPositionBrt,
			LinkedList<AdditionalActors> addActors,
			HashMap<BPMNTask, HashMap<BPMNBusinessRuleTask, LinkedList<LinkedList<BPMNElement>>>> pathsFromOriginOverCurrBrtToEndMap)
			throws Exception {
		// create minimal subsets
		// start with sets of brts of size 1
		LinkedList<LinkedList<BPMNBusinessRuleTask>> cheapestSubSets = new LinkedList<LinkedList<BPMNBusinessRuleTask>>();
		LinkedList<BPMNBusinessRuleTask> toRemove = new LinkedList<BPMNBusinessRuleTask>();

		for (int i = 1; i <= depT.size(); i++) {
			if (Thread.currentThread().isInterrupted()) {
				System.err.println("Interrupted! " + Thread.currentThread().getName());
				throw new InterruptedException();
			}
			LinkedList<BPMNBusinessRuleTask> currAvailable = new LinkedList<BPMNBusinessRuleTask>(depT);
			currAvailable.removeAll(toRemove);

			LinkedList<LinkedList<BPMNBusinessRuleTask>> brtSubLists = Combination.getPermutations(currAvailable, i);

			int skipped = 0;
			for (LinkedList<BPMNBusinessRuleTask> brtSubList : brtSubLists) {
				boolean skip = false;
				// skip combinations where proper subsets exist
				for (LinkedList<BPMNBusinessRuleTask> cheapestSubSet : cheapestSubSets) {
					if (brtSubList.containsAll(cheapestSubSet)) {
						skip = true;
					}
				}
				if (!skip) {
					// check if sd*, TE = depT \ brtSubList, leads to same spheres
					LinkedList<BPMNBusinessRuleTask> excludingTasks = new LinkedList<BPMNBusinessRuleTask>(
							currAvailable);
					excludingTasks.removeAll(brtSubList);

					HashMap<BPMNBusinessRuleTask, HashSet<BPMNParticipant>> spheres2 = new HashMap<BPMNBusinessRuleTask, HashSet<BPMNParticipant>>();

					boolean sameSpheres = true;
					for (BPMNBusinessRuleTask dependentBrt : depT) {
						// compute sd*, TE = depT \ brtSubList
						LinkedList<LinkedList<BPMNElement>> pathsFromOriginOverDependentBrtToEnd = pathsFromOriginOverCurrBrtToEndMap
								.get(origin).get(dependentBrt);

						boolean participantIsSd = isParticipantInSDForOriginWithExcludedTasks(participant, dataO,
								origin, dependentBrt, addActors, excludingTasks, pathsFromOriginOverDependentBrtToEnd);

						HashSet<BPMNParticipant> sdSet = new HashSet<BPMNParticipant>();
						if (participantIsSd) {
							sdSet.add(participant);
						}

						spheres2.putIfAbsent(dependentBrt, sdSet);
						if (!spheres1.get(dependentBrt).equals(spheres2.get(dependentBrt))) {
							// spheres for dependentBrt are not the same!
							sameSpheres = false;
							break;
						}

					}

					// check if the impact on the spheres are the same
					if (sameSpheres) {
						cheapestSubSets.add(brtSubList);
						if (brtSubList.size() == 1) {
							// if the list contains exactly 1 brt: this brt will be ignored for generating
							// new combinations containing that brt
							toRemove.addAll(brtSubList);
						}
					}

				} else {
					skipped++;
				}
			}

			if (skipped == brtSubLists.size()) {
				// each sub list has a proper subset already
				// no need to generate new sets
				i = depT.size();
			}

		}
		return cheapestSubSets;
	}

	public void computeBetaMeasure(PModelWithAdditionalActors pModelWithAdditionalActors,
			HashMap<BPMNDataObject, LinkedList<LinkedList<HashSet<?>>>> wdSpherePerDataObject)
			throws InterruptedException {
		// compute beta measure
		// i.e. query brts in any order and get origins for each data object
		// weight(w) is the depth of the origin
		LinkedList<AdditionalActors> currAddActorsList = pModelWithAdditionalActors.getAdditionalActorsList();
		for (Entry<BPMNDataObject, LinkedList<LinkedList<HashSet<?>>>> wdSpherePerDataObjectAndOrigin : wdSpherePerDataObject
				.entrySet()) {
			BPMNDataObject currDataO = wdSpherePerDataObjectAndOrigin.getKey();
			LinkedList<LinkedList<HashSet<?>>> wdSphereList = wdSpherePerDataObjectAndOrigin.getValue();
			for (int i = 0; i < wdSphereList.size(); i++) {
				if (Thread.currentThread().isInterrupted()) {
					System.err.println("Interrupted! " + Thread.currentThread().getName());
					throw new InterruptedException();
				}
				BPMNTask origin = (BPMNTask) wdSphereList.get(i).get(0).iterator().next();
				HashSet<BPMNTask> wdReaderBrts = (HashSet<BPMNTask>) wdSphereList.get(i).get(1);
				HashSet<BPMNParticipant> wdSphere = (HashSet<BPMNParticipant>) wdSphereList.get(i).get(2);
				WD_SphereEntry currWDSphereEntry = new WD_SphereEntry(currDataO, origin, wdReaderBrts, wdSphere);
				pModelWithAdditionalActors.getWdSphereEntries()
						.computeIfAbsent(currDataO, k -> new LinkedList<WD_SphereEntry>()).add(currWDSphereEntry);

				// compute wd' sphere with additional actors
				// add the additional actors to the brts reachable from an origin
				HashSet<BPMNParticipant> wdWithAddActorsParticipants = new HashSet<BPMNParticipant>();
				// add all wd actors of that wdEntry
				wdWithAddActorsParticipants.addAll(currWDSphereEntry.getWdSphere());
				HashSet<BPMNTask> reachableBrts = currWDSphereEntry.getWdReaderBrts();

				for (AdditionalActors addActors : currAddActorsList) {
					if (Thread.currentThread().isInterrupted()) {
						System.err.println("Interrupted! " + Thread.currentThread().getName());
						throw new InterruptedException();
					}
					// add all additional actors of brts reachable from origin
					BPMNBusinessRuleTask currBrt = addActors.getCurrBrt();
					for (BPMNTask reachableBrt : reachableBrts) {
						if (reachableBrt.getId().contentEquals(currBrt.getId())) {
							wdWithAddActorsParticipants.addAll(addActors.getAdditionalActors());
						}
					}
				}
				currWDSphereEntry.setWdSphereWithAdditionalActors(wdWithAddActorsParticipants);

				// compute lambda and set score
				HashSet<BPMNParticipant> lambdaActors = this.computeLambdaActors(
						currWDSphereEntry.getWdSphereWithAdditionalActors(), currWDSphereEntry.getWdSphere());
				currWDSphereEntry.setLambdaWdSphere(lambdaActors);

				double lambda = lambdaActors.size();

				double score = lambda * currWDSphereEntry.getWeightingOfOrigin();
				currWDSphereEntry.setScore(score);

				// add to sum beta measure
				double currBetaScoreSum = pModelWithAdditionalActors.getBetaMeasureSum();
				double newBetaScoreSum = currBetaScoreSum += score;
				pModelWithAdditionalActors.setBetaMeasureSum(newBetaScoreSum);
			}

		}
	}

	public void computeGammaMeasure(PModelWithAdditionalActors pModelWithAdditionalActors,
			HashMap<BPMNTask, LinkedList<LinkedList<BPMNElement>>> pathsFromOriginToEndMap,
			HashMap<BPMNTask, HashMap<BPMNBusinessRuleTask, LinkedList<LinkedList<BPMNElement>>>> pathFromOriginOverCurrBrtToEndMap)
			throws Exception {
		// compute sd sphere for each combination (DataObject,Origin,BusinessRuleTask)
		HashMap<BPMNParticipant, HashMap<BPMNDataObject, LinkedList<SD_SphereEntry>>> sdSphereEntries = new HashMap<BPMNParticipant, HashMap<BPMNDataObject, LinkedList<SD_SphereEntry>>>();
		HashMap<BPMNBusinessRuleTask, LinkedList<WeightWRD>> weightMap = new HashMap<BPMNBusinessRuleTask, LinkedList<WeightWRD>>();

		for (AdditionalActors addActors : pModelWithAdditionalActors.getAdditionalActorsList()) {
			BPMNBusinessRuleTask currBrt = addActors.getCurrBrt();
			for (Entry<BPMNDataObject, ArrayList<BPMNTask>> lastWriterEntry : currBrt.getLastWriterList().entrySet()) {
				if (Thread.currentThread().isInterrupted()) {
					System.err.println("Interrupted! " + Thread.currentThread().getName());
					throw new InterruptedException();
				}
				String sphereOfDataObject = lastWriterEntry.getKey().getDefaultSphere();
				boolean computeGammaMeasure = this.computeMeasure(sphereOfDataObject, this.strongDynamicSphereKey);

				if (computeGammaMeasure) {
					for (BPMNTask origin : lastWriterEntry.getValue()) {
						BPMNDataObject dataO = lastWriterEntry.getKey();

						LinkedList<LinkedList<BPMNElement>> pathsFromOriginToEnd = this
								.getOrComputePathsFromOriginToEnd(pathsFromOriginToEndMap, origin);

						for (BPMNParticipant participant : addActors.getAdditionalActors()) {

							if (Thread.currentThread().isInterrupted()) {
								System.err.println("Interrupted! " + Thread.currentThread().getName());
								throw new InterruptedException();
							}
							sdSphereEntries.computeIfAbsent(participant,
									k -> new HashMap<BPMNDataObject, LinkedList<SD_SphereEntry>>());
							sdSphereEntries.get(participant).computeIfAbsent(dataO,
									k -> new LinkedList<SD_SphereEntry>());

							boolean skipCombination = false;
							SD_SphereEntry entryAlreadyComputed = new SD_SphereEntry(dataO, origin, currBrt,
									participant);
							if (sdSphereEntries.get(participant).get(dataO).contains(entryAlreadyComputed)) {
								skipCombination = true;
							}

							if (!skipCombination) {
								// get the dependent brts for the configuration of origin, dataO and participant
								HashSet<BPMNBusinessRuleTask> depT = this.getDependentBrts(participant, origin, dataO,
										pathsFromOriginToEnd, pModelWithAdditionalActors);

								HashMap<BPMNBusinessRuleTask, HashSet<BPMNParticipant>> spheres1 = new HashMap<BPMNBusinessRuleTask, HashSet<BPMNParticipant>>();

								for (BPMNBusinessRuleTask dependentBrt : depT) {
									if (Thread.currentThread().isInterrupted()) {
										System.err.println("Interrupted! " + Thread.currentThread().getName());
										throw new InterruptedException();
									}
									SD_SphereEntry sdSphereEntryForDependentBrt = new SD_SphereEntry(dataO, origin,
											dependentBrt, participant);

									LinkedList<SD_SphereEntry> sdSphereEntriesForDataO = sdSphereEntries
											.get(participant).get(dataO);

									if (!sdSphereEntriesForDataO.contains(sdSphereEntryForDependentBrt)) {
										sdSphereEntriesForDataO.add(sdSphereEntryForDependentBrt);
									}

									// calculate sd* sphere for the participant (true or false, if the
									// participant is in sd)
									// TE = {} i.e. no additional actors of brts are excluded
									LinkedList<LinkedList<BPMNElement>> pathsFromOriginOverDependentBrtToEnd = this
											.getOrComputePathsFromOriginOverCurrBrtToEnd(
													pathFromOriginOverCurrBrtToEndMap, origin, dependentBrt);

									boolean participantIsSdWithoutExcludedAddActors = isParticipantInSDForOriginWithExcludedTasks(
											participant, dataO, origin, dependentBrt,
											pModelWithAdditionalActors.getAdditionalActorsList(), null,
											pathsFromOriginOverDependentBrtToEnd);

									HashSet<BPMNParticipant> sdSetWithoutExcludedAddActors = new HashSet<BPMNParticipant>();
									if (participantIsSdWithoutExcludedAddActors) {
										sdSetWithoutExcludedAddActors.add(participant);
									}
									spheres1.putIfAbsent(dependentBrt, sdSetWithoutExcludedAddActors);

									// is the participant strong-dynamic for the current configuration where TE = {}
									// i.e. no additional actors are excluded
									sdSphereEntryForDependentBrt
											.setSdSphereWithoutExcludedAddActors(sdSetWithoutExcludedAddActors);

									// exclude all brts with additional actors
									boolean participantIsSdWithExcludedAddActors = isParticipantInSDForOriginWithExcludedTasks(
											participant, dataO, origin, dependentBrt,
											pModelWithAdditionalActors.getAdditionalActorsList(),
											this.businessRuleTasks, pathsFromOriginOverDependentBrtToEnd);

									HashSet<BPMNParticipant> sdSetWithExcludedAddActors = new HashSet<BPMNParticipant>();
									if (participantIsSdWithExcludedAddActors) {
										sdSetWithExcludedAddActors.add(participant);
									}

									// is the participant strong-dynamic for the current configuration where TE =
									// {all business-rule-tasks}
									// i.e. all additional actors are excluded
									sdSphereEntryForDependentBrt
											.setSdSphereWithExcludedAddActors(sdSetWithExcludedAddActors);

								}

								// calculate depTMin
								LinkedList<LinkedList<BPMNBusinessRuleTask>> depTMin = new LinkedList<LinkedList<BPMNBusinessRuleTask>>();
								if (depT.size() == 1) {
									// no need to calculate subsets
									// spheres will be the same, since there cannot be a subset
									LinkedList<BPMNBusinessRuleTask> depTMinList = new LinkedList<BPMNBusinessRuleTask>();
									depTMinList.addAll(depT);
									depTMin.add(depTMinList);
								} else {
									depTMin = this.getMinimalSubSetsWithSameSpheres(participant, depT, spheres1, dataO,
											origin, null, pModelWithAdditionalActors.getAdditionalActorsList(),
											pathFromOriginOverCurrBrtToEndMap);
								}

								// compute gamma min for each list in depTMin
								LinkedList<LinkedList<BPMNBusinessRuleTask>> gammaMinLists = new LinkedList<LinkedList<BPMNBusinessRuleTask>>();
								double gammaMinScore = 0;
								for (LinkedList<BPMNBusinessRuleTask> cheapestSubList : depTMin) {
									double scorePerBrtList = 0;
									for (BPMNBusinessRuleTask brt : cheapestSubList) {
										if (Thread.currentThread().isInterrupted()) {
											System.err.println("Interrupted! " + Thread.currentThread().getName());
											throw new InterruptedException();
										}
										double currScore = 0;
										SD_SphereEntry sdSphereEntryForDependentBrt = this
												.getSD_SphereEntryWithParameters(sdSphereEntries, participant, dataO,
														origin, brt);

										LinkedList<WeightWRD> weightList = weightMap.computeIfAbsent(brt,
												k -> new LinkedList<WeightWRD>());
										WeightWRD currWeight = this.getWeightWRDWithParameters(weightMap, origin, brt,
												dataO);

										// calculate weight(w,r,d)
										double secondWeight = 0;
										if (currWeight == null) {
											LinkedList<LinkedList<BPMNElement>> pathsFromOriginOverBrtToEnd = pathFromOriginOverCurrBrtToEndMap
													.get(origin).get(brt);
											secondWeight = this.getAmountPathsWhereOriginWritesDataOForReaderBrt(dataO,
													origin, brt, pathsFromOriginOverBrtToEnd);
											WeightWRD currWeightWrd = new WeightWRD(origin, brt, dataO);
											weightList.add(currWeightWrd);
											currWeightWrd.setWeightWRD(secondWeight);
											currWeight = currWeightWrd;
										} else {
											secondWeight = currWeight.getWeightWRD();
										}

										// compute lambda and set score
										HashSet<BPMNParticipant> lambdaActors = this.computeLambdaActors(
												sdSphereEntryForDependentBrt.getSdSphereWithoutExcludedAddActors(),
												sdSphereEntryForDependentBrt.getSdSphereWithExcludedAddActors());

										sdSphereEntryForDependentBrt.setLambdaSdSphere(lambdaActors);
										double lambda = lambdaActors.size();

										sdSphereEntryForDependentBrt.setWeightOfOriginForCurrBrt(secondWeight);

										currScore = lambda * sdSphereEntryForDependentBrt.getWeightOfOrigin()
												* secondWeight;
										sdSphereEntryForDependentBrt.setScore(currScore);

										scorePerBrtList += currScore;
									}

									if (gammaMinLists.isEmpty()) {
										gammaMinLists.add(cheapestSubList);
										gammaMinScore = scorePerBrtList;
									} else {
										if (scorePerBrtList == gammaMinScore) {
											gammaMinLists.add(cheapestSubList);
										} else if (scorePerBrtList < gammaMinScore) {
											gammaMinLists.clear();
											gammaMinLists.add(cheapestSubList);
											gammaMinScore = scorePerBrtList;
										}
									}

								}

								// if there are more than 1 cheapest solutions, choose one
								int random = ThreadLocalRandom.current().nextInt(0, gammaMinLists.size());
								LinkedList<BPMNBusinessRuleTask> gammaMinList = gammaMinLists.get(random);

								// go through each brt in depT and check if it is contributing to gamma min
								for (BPMNBusinessRuleTask dependentBrt : depT) {
									if (Thread.currentThread().isInterrupted()) {
										System.err.println("Interrupted! " + Thread.currentThread().getName());
										throw new InterruptedException();
									}
									boolean isInCheapest = false;
									if (gammaMinList.contains(dependentBrt)) {
										isInCheapest = true;
									}

									SD_SphereEntry sdSphereEntryForDependentBrt = this.getSD_SphereEntryWithParameters(
											sdSphereEntries, participant, dataO, origin, dependentBrt);

									if (!isInCheapest) {
										// not contributing to gamma min
										sdSphereEntryForDependentBrt.setContributingToGammaMin(false);
										sdSphereEntryForDependentBrt.setScore(0);
									}
									// add to sum gamma measure
									double currGammaScoreSum = pModelWithAdditionalActors.getGammaMeasureSum();
									double newGammaScoreSum = currGammaScoreSum += sdSphereEntryForDependentBrt
											.getScore();
									pModelWithAdditionalActors.setGammaMeasureSum(newGammaScoreSum);

								}
							}
						}

					}
				}

			}

		}
		pModelWithAdditionalActors.setSdSphereEntries(sdSphereEntries);

	}

	public double getAmountPathsWhereOriginWritesDataOForReaderBrt(BPMNDataObject dataO, BPMNTask origin,
			BPMNBusinessRuleTask currPositionBrt, LinkedList<LinkedList<BPMNElement>> pathsFromOriginToEnd,
			LinkedList<LinkedList<BPMNElement>> pathsFromOriginOverBrtToEnd) {

		double amountPathsWhereOriginWritesForDataOForCurrPositionBrt = 0;

		for (LinkedList<BPMNElement> path : pathsFromOriginOverBrtToEnd) {
			boolean pastCurrBrt = false;
			for (int i = 0; i < path.size(); i++) {
				BPMNElement el = path.get(i);
				if (el instanceof BPMNTask) {
					BPMNTask currTask = (BPMNTask) el;
					if (dataO.getReaders().contains(currTask)) {
						// currTask is a reader of the dataO
						if (currTask instanceof BPMNBusinessRuleTask) {
							if (currPositionBrt.equals(currTask)) {
								pastCurrBrt = true;
							}
						}
					}

					if (dataO.getWriters().contains(currTask)) {
						// currTask is a writer
						// check if is the origin
						if (currTask.equals(origin)) {
							amountPathsWhereOriginWritesForDataOForCurrPositionBrt++;
						} else {
							// another writer found on some path
							// actors after that writer will not read dataO from origin
							if (!pastCurrBrt) {
								amountPathsWhereOriginWritesForDataOForCurrPositionBrt--;
							}
							i = path.size();
						}

					}

				}

			}

		}

		return amountPathsWhereOriginWritesForDataOForCurrPositionBrt / pathsFromOriginToEnd.size();

	}

	public double getAmountPathsWhereOriginWritesDataOForReaderBrt(BPMNDataObject dataO, BPMNTask origin,
			BPMNBusinessRuleTask currPositionBrt, LinkedList<LinkedList<BPMNElement>> pathsFromOriginOverBrtToEnd) {

		double amountPathsWhereOriginWritesForDataOForCurrPositionBrt = 0;

		for (LinkedList<BPMNElement> path : pathsFromOriginOverBrtToEnd) {
			boolean pastCurrBrt = false;
			for (int i = 0; i < path.size(); i++) {
				BPMNElement el = path.get(i);
				if (el instanceof BPMNTask) {
					BPMNTask currTask = (BPMNTask) el;
					if (dataO.getReaders().contains(currTask)) {
						// currTask is a reader of the dataO
						if (currTask instanceof BPMNBusinessRuleTask) {
							if (currPositionBrt.equals(currTask)) {
								pastCurrBrt = true;
							}
						}
					}

					if (dataO.getWriters().contains(currTask)) {
						// currTask is a writer
						// check if is the origin
						if (currTask.equals(origin)) {
							amountPathsWhereOriginWritesForDataOForCurrPositionBrt++;
						} else {
							// another writer found on some path
							// actors after that writer will not read dataO from origin
							if (!pastCurrBrt) {
								amountPathsWhereOriginWritesForDataOForCurrPositionBrt--;
							}
							i = path.size();
						}

					}

				}

			}

		}
		double fraction = amountPathsWhereOriginWritesForDataOForCurrPositionBrt / pathsFromOriginOverBrtToEnd.size();

		return fraction * Math.pow(2, -currPositionBrt.getLabels().size());

	}

	public SD_SphereEntry getSD_SphereEntryWithParameters(
			HashMap<BPMNParticipant, HashMap<BPMNDataObject, LinkedList<SD_SphereEntry>>> sdSphereEntries,
			BPMNParticipant participant, BPMNDataObject dataO, BPMNTask origin, BPMNTask brt) throws Exception {

		try {
			LinkedList<SD_SphereEntry> sdEntries = sdSphereEntries.get(participant).get(dataO);
			for (SD_SphereEntry sdEntry : sdEntries) {
				if (sdEntry.getCurrBrt().equals(brt) && sdEntry.getDataObject().equals(dataO)
						&& sdEntry.getOrigin().equals(origin)) {
					return sdEntry;
				}
			}

		} catch (Exception ex) {
			throw new Exception("No SD_SphereEntry found!");
		}

		return null;

	}

	public WeightWRD getWeightWRDWithParameters(HashMap<BPMNBusinessRuleTask, LinkedList<WeightWRD>> weightEntries,
			BPMNTask origin, BPMNBusinessRuleTask brt, BPMNDataObject dataO) throws Exception {

		try {
			LinkedList<WeightWRD> weightList = weightEntries.get(brt);

			for (WeightWRD weightEntry : weightList) {
				if (weightEntry.getOrigin().equals(origin) && weightEntry.getReaderBrt().equals(brt)
						&& weightEntry.getDataO().equals(dataO)) {
					return weightEntry;
				}
			}

		} catch (Exception ex) {
			throw new Exception("No WeightEntry found!");
		}

		return null;

	}

	public LinkedList<BPMNParticipant> getPrivateSphere() {
		return privateSphere;
	}

	public void setPrivateSphere(LinkedList<BPMNParticipant> privateSphere) {
		this.privateSphere = privateSphere;
	}

	public void calculateMeasure(LinkedList<PModelWithAdditionalActors> additionalActorsCombs,
			HashMap<BPMNDataObject, LinkedList<HashSet<?>>> staticSpherePerDataObject,
			HashMap<BPMNDataObject, LinkedList<LinkedList<HashSet<?>>>> wdSpherePerDataObject,
			HashMap<BPMNTask, LinkedList<LinkedList<BPMNElement>>> pathsFromOriginToEndMap,
			HashMap<BPMNTask, HashMap<BPMNBusinessRuleTask, LinkedList<LinkedList<BPMNElement>>>> pathFromOriginOverCurrBrtToEndMap)
			throws Exception {

		for (PModelWithAdditionalActors pModelWithAdditionalActors : additionalActorsCombs) {
			// compute alpha measure
			this.computeAlphaMeasure(pModelWithAdditionalActors, staticSpherePerDataObject);

			// compute beta measure
			this.computeBetaMeasure(pModelWithAdditionalActors, wdSpherePerDataObject);

			// compute gamma measure
			this.computeGammaMeasure(pModelWithAdditionalActors, pathsFromOriginToEndMap,
					pathFromOriginOverCurrBrtToEndMap);

			// compute sum measure for current pModelWithAdditionalActors
			// i.e. compute alpha, beta, gamma measure and weight it with given parameters
			double sum = pModelWithAdditionalActors.getWeightedCostAlphaMeasure()
					+ pModelWithAdditionalActors.getWeightedCostBetaMeasure()
					+ pModelWithAdditionalActors.getWeightedCostGammaMeasure();
			pModelWithAdditionalActors.setSumMeasure(sum);
		}
	}

	public LinkedList<LinkedList<BPMNElement>> getOrComputePathsFromOriginToEnd(
			HashMap<BPMNTask, LinkedList<LinkedList<BPMNElement>>> pathsFromOriginToEndMap, BPMNTask origin)
			throws Exception {
		LinkedList<LinkedList<BPMNElement>> pathsFromOriginToEnd = pathsFromOriginToEndMap.get(origin);
		if (pathsFromOriginToEnd == null || pathsFromOriginToEnd.isEmpty()) {
			pathsFromOriginToEnd = this.goDfs(origin, this.endEvent, this.endEvent, new LinkedList<BPMNElement>(),
					new LinkedList<BPMNElement>(), new LinkedList<LinkedList<BPMNElement>>());
			pathsFromOriginToEndMap.put(origin, pathsFromOriginToEnd);
		}

		return pathsFromOriginToEnd;

	}

	public LinkedList<LinkedList<BPMNElement>> getOrComputePathsFromOriginOverCurrBrtToEnd(
			HashMap<BPMNTask, HashMap<BPMNBusinessRuleTask, LinkedList<LinkedList<BPMNElement>>>> pathFromOriginOverCurrBrtToEndMap,
			BPMNTask origin, BPMNBusinessRuleTask currBrt) throws Exception {
		HashMap<BPMNBusinessRuleTask, LinkedList<LinkedList<BPMNElement>>> pathsFromOriginOverCurrBrtToEndInnerMap = pathFromOriginOverCurrBrtToEndMap
				.get(origin);
		LinkedList<LinkedList<BPMNElement>> pathsFromOriginOverCurrBrtToEnd = new LinkedList<LinkedList<BPMNElement>>();

		if (pathsFromOriginOverCurrBrtToEndInnerMap == null) {
			// no paths for any brt for the origin exists yet
			pathsFromOriginOverCurrBrtToEnd = this.goDfs(origin, currBrt, this.endEvent, new LinkedList<BPMNElement>(),
					new LinkedList<BPMNElement>(), new LinkedList<LinkedList<BPMNElement>>());
			HashMap<BPMNBusinessRuleTask, LinkedList<LinkedList<BPMNElement>>> currMap = new HashMap<BPMNBusinessRuleTask, LinkedList<LinkedList<BPMNElement>>>();
			currMap.put(currBrt, pathsFromOriginOverCurrBrtToEnd);
			pathFromOriginOverCurrBrtToEndMap.putIfAbsent(origin, currMap);
		} else {
			pathsFromOriginOverCurrBrtToEnd = pathsFromOriginOverCurrBrtToEndInnerMap.get(currBrt);
			// check if paths for the origin and the currBrt exist
			if (pathsFromOriginOverCurrBrtToEnd == null) {
				pathsFromOriginOverCurrBrtToEnd = this.goDfs(origin, currBrt, this.endEvent,
						new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(),
						new LinkedList<LinkedList<BPMNElement>>());
				pathsFromOriginOverCurrBrtToEndInnerMap.putIfAbsent(currBrt, pathsFromOriginOverCurrBrtToEnd);
				pathFromOriginOverCurrBrtToEndMap.putIfAbsent(origin, pathsFromOriginOverCurrBrtToEndInnerMap);
			}
		}

		return pathsFromOriginOverCurrBrtToEnd;

	}

	public LinkedList<LinkedList<AdditionalActors>> computeListsWithMinIncreaseOfAddActors(
			LinkedList<LinkedList<AdditionalActors>> allCheapestAddActorsLists,
			LinkedList<AdditionalActors> additionalActorsForBrt) {

		int overallSmallestIncrease = Integer.MAX_VALUE;
		LinkedList<LinkedList<AdditionalActors>> newCheapest = new LinkedList<LinkedList<AdditionalActors>>();

		for (LinkedList<AdditionalActors> currCheapestAddActorsList : allCheapestAddActorsLists) {
			HashSet<BPMNParticipant> currentCheapestAddActorsSet = new HashSet<BPMNParticipant>();
			for (AdditionalActors currCheapestAddActors : currCheapestAddActorsList) {
				currentCheapestAddActorsSet.addAll(currCheapestAddActors.getAdditionalActors());
			}
			int size = currentCheapestAddActorsSet.size();

			for (AdditionalActors addActorsCurrBrt : additionalActorsForBrt) {
				// check the increase
				HashSet<BPMNParticipant> test = new HashSet<BPMNParticipant>(currentCheapestAddActorsSet);
				test.addAll(addActorsCurrBrt.getAdditionalActors());
				int increase = test.size() - size;
				if (increase < overallSmallestIncrease) {
					newCheapest = new LinkedList<LinkedList<AdditionalActors>>();
					LinkedList<AdditionalActors> newCheapestList = new LinkedList<AdditionalActors>();
					newCheapestList.addAll(currCheapestAddActorsList);
					newCheapestList.add(addActorsCurrBrt);
					newCheapest.add(newCheapestList);
					overallSmallestIncrease = increase;
				} else if (increase == overallSmallestIncrease) {
					LinkedList<AdditionalActors> newCheapestList = new LinkedList<AdditionalActors>();
					newCheapestList.addAll(currCheapestAddActorsList);
					newCheapestList.add(addActorsCurrBrt);
					newCheapest.add(newCheapestList);
				}
			}

		}

		return newCheapest;

	}

	public BPMNParticipant getTroubleShooter() {
		return this.troubleShooter;
	}

	public LinkedList<LinkedList<FlowNode>> getAllPathsThroughProcess() {
		return this.pathsThroughProcess;
	}

	public LinkedList<BPMNElement> getProcessElements() {
		return this.processElements;
	}

	public File getProcessModelFile() {
		return this.processModelFile;
	}

	public BpmnModelInstance getModelInstance() {
		return this.modelInstance;
	}

	public LinkedList<BPMNBusinessRuleTask> getBusinessRuleTasks() {
		return this.businessRuleTasks;
	}

	public LinkedList<LinkedList<AdditionalActors>> queryBrtsInTopologicalOrderAndAssignAdditionalActorsForIncrementalHeuristic(
			BPMNElement startNode, BPMNElement endNode, LinkedList<BPMNElement> queue,
			LinkedList<BPMNElement> openSplits, BPMNElement endPointOfSearch,
			HashMap<BPMNTask, LinkedList<LinkedList<BPMNElement>>> pathsFromOriginToEndMap,
			HashMap<BPMNBusinessRuleTask, LinkedList<AdditionalActors>> alreadyChosenAdditionalActors,
			HashMap<BPMNDataObject, LinkedList<HashSet<?>>> staticSpherePerDataObject,
			HashMap<BPMNDataObject, LinkedList<LinkedList<HashSet<?>>>> wdSpherePerDataObject,
			LinkedList<LinkedList<AdditionalActors>> allCheapestAddActorsLists, int bound)
			throws NullPointerException, InterruptedException, Exception {
		// go DFS inside all branch till corresponding join is found
		queue.add(startNode);

		while (!(queue.isEmpty())) {
			BPMNElement element = queue.poll();
			if (Thread.currentThread().isInterrupted()) {
				System.err.println("Interrupted! " + Thread.currentThread().getName());
				throw new InterruptedException();
			}

			if (element.getId().equals(endNode.getId())) {

				if (endNode instanceof BPMNGateway && endNode.getPredecessors().size() == 2) {
					// when a join is found - poll the last opened gateway from the stack
					BPMNGateway lastOpenedSplitGtw = (BPMNGateway) openSplits.pollLast();

					if (!openSplits.contains(lastOpenedSplitGtw)) {
						// when the openSplitStack does not contain the lastOpenedSplit anymore, all
						// branches to the joinGtw have been visited
						// go from joinGtw to the Join of the last opened split in the stack, if there
						// is any

						if (!openSplits.isEmpty()) {
							// still inside a branch
							BPMNGateway lastOpenedSplit = (BPMNGateway) openSplits.getLast();
							BPMNGateway correspondingJoin = this.getCorrespondingGtw(lastOpenedSplit);
							// need to add the lastOpenedSplit, since one path has gone dfs till the join
							// already
							queryBrtsInTopologicalOrderAndAssignAdditionalActorsForIncrementalHeuristic(
									element.getSuccessors().iterator().next(), correspondingJoin, queue, openSplits,
									endPointOfSearch, pathsFromOriginToEndMap, alreadyChosenAdditionalActors,
									staticSpherePerDataObject, wdSpherePerDataObject, allCheapestAddActorsLists, bound);
						} else {
							// when there are no open splits gtws
							// go from the successor of the element to endPointOfSearch since the
							// currentElement has
							// already been added to the path
							queryBrtsInTopologicalOrderAndAssignAdditionalActorsForIncrementalHeuristic(
									element.getSuccessors().iterator().next(), endPointOfSearch, queue, openSplits,
									endPointOfSearch, pathsFromOriginToEndMap, alreadyChosenAdditionalActors,
									staticSpherePerDataObject, wdSpherePerDataObject, allCheapestAddActorsLists, bound);
						}
					}

				}

				element = queue.poll();
				if (element == null && queue.isEmpty()) {
					return allCheapestAddActorsLists;
				}

			}

			if (element instanceof BPMNBusinessRuleTask) {
				BPMNBusinessRuleTask currentBrt = (BPMNBusinessRuleTask) element;
				if (this.businessRuleTasks.contains(currentBrt)) {
					LinkedList<AdditionalActors> addActorsForBrt = this.getCheapestAdditionalActorsForBrtHeuristic(true,
							currentBrt, pathsFromOriginToEndMap, staticSpherePerDataObject, wdSpherePerDataObject,
							alreadyChosenAdditionalActors, bound);
					alreadyChosenAdditionalActors.putIfAbsent(currentBrt, addActorsForBrt);
					LinkedList<LinkedList<AdditionalActors>> currCh = this.computeAllAddActorsLists(addActorsForBrt,
							allCheapestAddActorsLists);
					allCheapestAddActorsLists.clear();
					allCheapestAddActorsLists.addAll(currCh);
				}
			}

			if (element instanceof BPMNGateway && element.getSuccessors().size() == 2) {
				// add the split to the openSplitStack 1 times for each outgoing paths
				int i = 0;
				while (i < element.getSuccessors().size()) {
					openSplits.add((BPMNGateway) element);
					i++;
				}

			}

			for (BPMNElement successor : element.getSuccessors()) {
				if (element instanceof BPMNGateway && element.getSuccessors().size() == 2) {
					// when a split is found - go dfs till the corresponding join is found
					BPMNElement correspondingJoinGtw = null;
					try {
						correspondingJoinGtw = this.getCorrespondingGtw((BPMNGateway) element);
					} catch (Exception ex) {
						throw ex;
					}
					queryBrtsInTopologicalOrderAndAssignAdditionalActorsForIncrementalHeuristic(successor,
							correspondingJoinGtw, queue, openSplits, endPointOfSearch, pathsFromOriginToEndMap,
							alreadyChosenAdditionalActors, staticSpherePerDataObject, wdSpherePerDataObject,
							allCheapestAddActorsLists, bound);
				} else {
					queue.add(successor);
				}

			}

		}

		return allCheapestAddActorsLists;

	}

	public LinkedList<LinkedList<AdditionalActors>> queryBrtsTopolgicalAdvancedHeuristicSearch(BPMNElement startNode,
			BPMNElement endNode, LinkedList<BPMNElement> queue, LinkedList<BPMNElement> openSplits,
			BPMNElement endPointOfSearch,
			HashMap<BPMNTask, LinkedList<LinkedList<BPMNElement>>> pathsFromOriginToEndMap,
			HashMap<BPMNBusinessRuleTask, LinkedList<AdditionalActors>> alreadyChosenAdditionalActors,
			HashMap<BPMNDataObject, LinkedList<HashSet<?>>> staticSpherePerDataObject,
			HashMap<BPMNDataObject, LinkedList<LinkedList<HashSet<?>>>> wdSpherePerDataObject,
			LinkedList<LinkedList<AdditionalActors>> allCheapestAddActorsLists,
			HashMap<BPMNDataObject, HashMap<BPMNParticipant, Double>> staticSphereTiebreakerMap, int bound)
			throws NullPointerException, InterruptedException, Exception {
		// go DFS inside all branch till corresponding join is found
		queue.add(startNode);

		while (!(queue.isEmpty())) {
			BPMNElement element = queue.poll();
			if (Thread.currentThread().isInterrupted()) {
				System.err.println("Interrupted! " + Thread.currentThread().getName());
				throw new InterruptedException();
			}

			if (element.getId().equals(endNode.getId())) {

				if (endNode instanceof BPMNGateway && endNode.getPredecessors().size() == 2) {
					// when a join is found - poll the last opened gateway from the stack
					BPMNGateway lastOpenedSplitGtw = (BPMNGateway) openSplits.pollLast();

					if (!openSplits.contains(lastOpenedSplitGtw)) {
						// when the openSplitStack does not contain the lastOpenedSplit anymore, all
						// branches to the joinGtw have been visited
						// go from joinGtw to the Join of the last opened split in the stack, if there
						// is any

						if (!openSplits.isEmpty()) {
							// still inside a branch
							BPMNGateway lastOpenedSplit = (BPMNGateway) openSplits.getLast();
							BPMNGateway correspondingJoin = this.getCorrespondingGtw(lastOpenedSplit);
							// need to add the lastOpenedSplit, since one path has gone dfs till the join
							// already
							queryBrtsTopolgicalAdvancedHeuristicSearch(element.getSuccessors().iterator().next(),
									correspondingJoin, queue, openSplits, endPointOfSearch, pathsFromOriginToEndMap,
									alreadyChosenAdditionalActors, staticSpherePerDataObject, wdSpherePerDataObject,
									allCheapestAddActorsLists, staticSphereTiebreakerMap, bound);
						} else {
							// when there are no open splits gtws
							// go from the successor of the element to endPointOfSearch since the
							// currentElement has
							// already been added to the path
							queryBrtsTopolgicalAdvancedHeuristicSearch(element.getSuccessors().iterator().next(),
									endPointOfSearch, queue, openSplits, endPointOfSearch, pathsFromOriginToEndMap,
									alreadyChosenAdditionalActors, staticSpherePerDataObject, wdSpherePerDataObject,
									allCheapestAddActorsLists, staticSphereTiebreakerMap, bound);
						}
					}

				}

				element = queue.poll();
				if (element == null && queue.isEmpty()) {
					return allCheapestAddActorsLists;
				}

			}

			if (element instanceof BPMNBusinessRuleTask) {
				BPMNBusinessRuleTask currentBrt = (BPMNBusinessRuleTask) element;
				if (this.businessRuleTasks.contains(currentBrt)) {

					BPMNGateway gtw = (BPMNGateway) currentBrt.getSuccessors().iterator().next();
					BPMNExclusiveGateway exclGtw = (BPMNExclusiveGateway) gtw;
					int amountAddActors = exclGtw.getAmountAddActors();

					// get all potential additional actors of the current brt
					LinkedList<LinkedList<BPMNParticipant>> potentialAddActorsLists = this
							.getPotentialAddActorsListsForBrt(currentBrt);

					HashSet<BPMNParticipant> cheapestAddActors = new HashSet<BPMNParticipant>();
					// add all mandatory participants
					cheapestAddActors.addAll(potentialAddActorsLists.get(1));
					int addActorsToFind = amountAddActors - cheapestAddActors.size();

					LinkedList<AdditionalActors> addActorsForBrt = new LinkedList<AdditionalActors>();

					// get the local cheapest potential add actors
					// take into account the mandatory participants of other brts		
					TreeMap<Double, LinkedList<BPMNParticipant>> localCheapestPotentialAddActors = this
							.getCheapestPotentialAdditionalActorsForBrt(true, currentBrt, pathsFromOriginToEndMap,
									staticSpherePerDataObject, wdSpherePerDataObject, alreadyChosenAdditionalActors);

					// compute the cluster
					HashSet<BPMNTask> clusterSet = this.computeCluster(currentBrt);

					if (clusterSet.size() >= 1) {
						// there are other brts that share an origin with the currentBrt
						// compute the spheres for the brts in that cluster only for the shared origins
						HashMap<BPMNParticipant, Double> bestCandidatesForCluster = new HashMap<BPMNParticipant, Double>();

						for (BPMNTask otherBrt : clusterSet) {

							if (!otherBrt.equals(currentBrt)) {

								TreeMap<Double, LinkedList<BPMNParticipant>> cheapestPotentialAddActorsForOtherBrt = this
										.getCheapestPotentialAdditionalActorsForBrt(true,
												(BPMNBusinessRuleTask) otherBrt, pathsFromOriginToEndMap,
												staticSpherePerDataObject, wdSpherePerDataObject,
												alreadyChosenAdditionalActors);

								for (Entry<Double, LinkedList<BPMNParticipant>> entry : cheapestPotentialAddActorsForOtherBrt
										.entrySet()) {

									for (BPMNParticipant participant : entry.getValue()) {
										double currValue = bestCandidatesForCluster.getOrDefault(participant, 0.0);
										double newValue = currValue + entry.getKey();
										bestCandidatesForCluster.put(participant, newValue);
									}
								}
							}

						}
						TreeMap<Double, LinkedList<BPMNParticipant>> bestParticipantsMap = CommonFunctionality
								.computeMapDescendingOrder(bestCandidatesForCluster);

						// best participants with tiebreaker rules for brt

						// generate the additional actors for the current brt using the best options
						addActorsForBrt = this.generatePossibleCombinationsOfAdditionalActorsWithBoundForBrt(exclGtw,
								addActorsToFind, potentialAddActorsLists.get(1), potentialAddActorsLists.get(0),
								currentBrt, localCheapestPotentialAddActors, bestCandidatesForCluster,
								staticSphereTiebreakerMap, bound);

					} else {
						// generate the additional actors using the local cheapest combinations of the
						// current brt
						addActorsForBrt = this.generatePossibleCombinationsOfAdditionalActorsWithBoundForBrt(exclGtw,
								addActorsToFind, potentialAddActorsLists.get(1), potentialAddActorsLists.get(0),
								currentBrt, localCheapestPotentialAddActors, null, staticSphereTiebreakerMap, bound);

					}

					alreadyChosenAdditionalActors.putIfAbsent(currentBrt, addActorsForBrt);

					LinkedList<LinkedList<AdditionalActors>> currLists = this.computeAllAddActorsLists(addActorsForBrt,
							allCheapestAddActorsLists);
					allCheapestAddActorsLists.clear();
					allCheapestAddActorsLists.addAll(currLists);

				}
			}

			if (element instanceof BPMNGateway && element.getSuccessors().size() == 2) {
				// add the split to the openSplitStack 1 times for each outgoing paths
				int i = 0;
				while (i < element.getSuccessors().size()) {
					openSplits.add((BPMNGateway) element);
					i++;
				}

			}

			for (BPMNElement successor : element.getSuccessors()) {
				if (element instanceof BPMNGateway && element.getSuccessors().size() == 2) {
					// when a split is found - go dfs till the corresponding join is found
					BPMNElement correspondingJoinGtw = null;
					try {
						correspondingJoinGtw = this.getCorrespondingGtw((BPMNGateway) element);
					} catch (Exception ex) {
						throw ex;
					}
					queryBrtsTopolgicalAdvancedHeuristicSearch(successor, correspondingJoinGtw, queue, openSplits,
							endPointOfSearch, pathsFromOriginToEndMap, alreadyChosenAdditionalActors,
							staticSpherePerDataObject, wdSpherePerDataObject, allCheapestAddActorsLists,
							staticSphereTiebreakerMap, bound);
				} else {
					queue.add(successor);
				}

			}

		}

		return allCheapestAddActorsLists;

	}

	public BPMNGateway getCorrespondingGtw(BPMNGateway gtw) throws NullPointerException, Exception {

		if (gtw.getName().isEmpty() || gtw.getName().equals(null)) {
			throw new Exception("Corresponding gtws must be named accordingly!");
		}
		BPMNGateway correspondingGtw = null;
		if (gtw.getPredecessors().size() >= 2 && gtw.getSuccessors().size() == 1) {
			// gtw is a join
			StringBuilder splitGtwId = new StringBuilder();

			if (gtw.getId().contains("_join")) {
				splitGtwId.append(gtw.getName().trim() + "_split");
			} else {
				// it must have the same name but >=2 outgoing and ==1 incoming
				for (Gateway gateway : this.modelInstance.getModelElementsByType(Gateway.class)) {
					if (gateway.getName().trim().contentEquals(gtw.getName().trim())) {
						if (gateway.getIncoming().size() == 1 && gateway.getOutgoing().size() >= 2) {
							splitGtwId.append(gateway.getId());
							break;
						}
					}
				}

			}
			BPMNGateway splitGtw = (BPMNGateway) this.getBPMNNodeById(splitGtwId.toString().trim());
			if (splitGtw == null) {
				throw new Exception("Names of corresponding split and join gtws have to be equal!");
			}
			if (splitGtw.getPredecessors().size() == 1 && splitGtw.getSuccessors().size() == 2) {
				correspondingGtw = splitGtw;
			}
		} else if (gtw.getPredecessors().size() == 1 && gtw.getSuccessors().size() == 2) {
			// gtw is a split
			StringBuilder joinGtwId = new StringBuilder();
			if (gtw.getId().contains("_split")) {
				joinGtwId.append(gtw.getName().trim() + "_join");
			} else {
				// it must have the same name but ==1 outgoing and >=2 incoming
				for (Gateway gateway : this.modelInstance.getModelElementsByType(Gateway.class)) {
					if (gateway.getName().trim().contentEquals(gtw.getName().trim())) {
						if (gateway.getIncoming().size() == 2 && gateway.getOutgoing().size() == 1) {
							joinGtwId.append(gateway.getId());
							break;
						}
					}
				}

			}
			BPMNGateway joinGtw = (BPMNGateway) this.getBPMNNodeById(joinGtwId.toString().trim());

			if (joinGtw == null) {
				throw new Exception("Names of corresponding split and join gtws have to be equal!");
			}

			if (joinGtw.getPredecessors().size() == 2 && joinGtw.getSuccessors().size() == 1) {
				correspondingGtw = joinGtw;
			}
		}
		return correspondingGtw;

	}

	public HashMap<BPMNParticipant, Double> getCheapestPotentialAddActorsForBrtForDataObject(boolean incremental,
			BPMNBusinessRuleTask currentBrt, BPMNDataObject dataO,
			HashMap<BPMNTask, LinkedList<LinkedList<BPMNElement>>> pathsFromOriginToEndMap,
			HashMap<BPMNDataObject, LinkedList<HashSet<?>>> staticSpherePerDataObject,
			HashMap<BPMNDataObject, LinkedList<LinkedList<HashSet<?>>>> wdSpherePerDataObject,
			HashMap<BPMNBusinessRuleTask, LinkedList<AdditionalActors>> alreadyChosenAdditionalActorsPerBrt,
			HashSet<BPMNTask> clusterSet, HashSet<BPMNTask> sharedOrigins, boolean addToClusterSetAndSharedOrigins,
			boolean onlyTakeClusteredOrigins) throws Exception {

		HashMap<BPMNParticipant, Double> sphereSumOfParticipantForBrt = new HashMap<BPMNParticipant, Double>();

		String requiredSphere = dataO.getDefaultSphere();
		if (this.requiredSphereIsAtLeast(requiredSphere, this.privateSphereKey)) {
			for (BPMNParticipant participant : this.privateSphere) {
				this.increaseOccurenceOfParticipant(participant, sphereSumOfParticipantForBrt);
			}
		}

		if (this.requiredSphereIsAtLeast(requiredSphere, this.staticSphereKey)) {
			HashSet<BPMNParticipant> staticSphereForDataObject = new HashSet<BPMNParticipant>(
					this.getParticipantsInStaticSphereForDataObject(dataO, staticSpherePerDataObject));

			HashSet<BPMNTask> readerBrts = this.getReaderBrtsFromStaticSphereForDataObject(dataO,
					staticSpherePerDataObject);
			if (incremental) {
				if (alreadyChosenAdditionalActorsPerBrt != null) {
					// add already chosen additional actors
					for (Entry<BPMNBusinessRuleTask, LinkedList<AdditionalActors>> alreadyChosenAddActorsEntry : alreadyChosenAdditionalActorsPerBrt
							.entrySet()) {
						BPMNBusinessRuleTask brtWithAddActors = alreadyChosenAddActorsEntry.getKey();
						if (readerBrts.contains(brtWithAddActors)) {
							for (AdditionalActors addActors : alreadyChosenAddActorsEntry.getValue()) {
								staticSphereForDataObject.addAll(addActors.getAdditionalActors());
							}
						}
					}
				}
			}
			// add participants that are mandatory
			for (BPMNTask readerBrt : readerBrts) {
				BPMNExclusiveGateway exclGtw = (BPMNExclusiveGateway) readerBrt.getSuccessors().iterator().next();
				LinkedList<Constraint> constraints = exclGtw.getConstraints();
				for (Constraint constraint : constraints) {
					if (constraint instanceof MandatoryParticipantConstraint) {
						staticSphereForDataObject
								.add(((MandatoryParticipantConstraint) constraint).getMandatoryParticipant());
					}
				}
			}

			for (BPMNParticipant participant : staticSphereForDataObject) {
				this.increaseOccurenceOfParticipant(participant, sphereSumOfParticipantForBrt);
			}
		}

		if (this.requiredSphereIsAtLeast(requiredSphere, this.weakDynamicSphereKey)) {
			for (BPMNElement writer : this.getLastWriterListForDataObject(currentBrt, dataO)) {
				BPMNTask origin = (BPMNTask) writer;
				LinkedList<LinkedList<HashSet<?>>> wdSphere = wdSpherePerDataObject.get(dataO);
				for (LinkedList<HashSet<?>> wdEntry : wdSphere) {
					if (Thread.currentThread().isInterrupted()) {
						System.err.println("Interrupted! " + Thread.currentThread().getName());
						throw new InterruptedException();
					}
					BPMNTask currOrigin = (BPMNTask) wdEntry.get(0).iterator().next();
					if (origin.equals(currOrigin)) {
						HashSet<BPMNParticipant> wdParticipants = new HashSet<BPMNParticipant>(
								(HashSet<BPMNParticipant>) wdEntry.get(2));

						HashSet<BPMNTask> wdBrts = (HashSet<BPMNTask>) wdEntry.get(1);

						if (incremental) {
							if (alreadyChosenAdditionalActorsPerBrt != null) {
								// add the additional actors of already queried brts
								for (Entry<BPMNBusinessRuleTask, LinkedList<AdditionalActors>> alreadyChosenAddActorsEntry : alreadyChosenAdditionalActorsPerBrt
										.entrySet()) {
									BPMNBusinessRuleTask brtWithAddActors = alreadyChosenAddActorsEntry.getKey();
									if (wdBrts.contains(brtWithAddActors)) {
										for (AdditionalActors addActors : alreadyChosenAddActorsEntry.getValue()) {
											wdParticipants.addAll(addActors.getAdditionalActors());
										}
									}
								}

							}
						}
						// add participants that are mandatory
						for (BPMNTask readerBrt : wdBrts) {
							BPMNExclusiveGateway exclGtw = (BPMNExclusiveGateway) readerBrt.getSuccessors().iterator()
									.next();
							LinkedList<Constraint> constraints = exclGtw.getConstraints();
							for (Constraint constraint : constraints) {
								if (constraint instanceof MandatoryParticipantConstraint) {
									wdParticipants.add(
											((MandatoryParticipantConstraint) constraint).getMandatoryParticipant());
								}
							}
						}

						for (BPMNParticipant participant : wdParticipants) {
							this.increaseOccurenceOfParticipant(participant, sphereSumOfParticipantForBrt);
						}

					}
				}

				if (this.requiredSphereIsAtLeast(requiredSphere, this.strongDynamicSphereKey)) {
					// compute sd for (DataObject, Origin, currentBrt)
					LinkedList<LinkedList<BPMNElement>> pathsFromOriginToEnd = this
							.getOrComputePathsFromOriginToEnd(pathsFromOriginToEndMap, origin);
					HashSet<BPMNParticipant> sdParticipants = this.getSdActors(dataO, origin, currentBrt,
							alreadyChosenAdditionalActorsPerBrt, pathsFromOriginToEnd);

					for (BPMNParticipant participant : sdParticipants) {
						this.increaseOccurenceOfParticipant(participant, sphereSumOfParticipantForBrt);
					}
				}

			}
		}

		return sphereSumOfParticipantForBrt;

	}

	public TreeMap<Double, LinkedList<BPMNParticipant>> getCheapestPotentialAdditionalActorsForBrt(boolean incremental,
			BPMNBusinessRuleTask currentBrt,
			HashMap<BPMNTask, LinkedList<LinkedList<BPMNElement>>> pathsFromOriginToEndMap,
			HashMap<BPMNDataObject, LinkedList<HashSet<?>>> staticSpherePerDataObject,
			HashMap<BPMNDataObject, LinkedList<LinkedList<HashSet<?>>>> wdSpherePerDataObject,
			HashMap<BPMNBusinessRuleTask, LinkedList<AdditionalActors>> alreadyChosenAdditionalActorsPerBrt)
			throws Exception {

		HashMap<BPMNParticipant, Double> sphereSumOfParticipantForBrt = new HashMap<BPMNParticipant, Double>();

		for (BPMNDataObject dataO : currentBrt.getDataObjects()) {
			String requiredSphere = dataO.getDefaultSphere();
			if (this.requiredSphereIsAtLeast(requiredSphere, this.privateSphereKey)) {
				for (BPMNParticipant participant : this.privateSphere) {
					this.increaseOccurenceOfParticipant(participant, sphereSumOfParticipantForBrt);
				}
			}

			if (this.requiredSphereIsAtLeast(requiredSphere, this.staticSphereKey)) {
				HashSet<BPMNParticipant> staticSphereForDataObject = new HashSet<BPMNParticipant>(
						this.getParticipantsInStaticSphereForDataObject(dataO, staticSpherePerDataObject));

				HashSet<BPMNTask> readerBrts = this.getReaderBrtsFromStaticSphereForDataObject(dataO,
						staticSpherePerDataObject);
				if (incremental) {
					if (alreadyChosenAdditionalActorsPerBrt != null) {
						// add already chosen additional actors
						for (Entry<BPMNBusinessRuleTask, LinkedList<AdditionalActors>> alreadyChosenAddActorsEntry : alreadyChosenAdditionalActorsPerBrt
								.entrySet()) {
							BPMNBusinessRuleTask brtWithAddActors = alreadyChosenAddActorsEntry.getKey();
							if (readerBrts.contains(brtWithAddActors)) {
								for (AdditionalActors addActors : alreadyChosenAddActorsEntry.getValue()) {
									staticSphereForDataObject.addAll(addActors.getAdditionalActors());
								}
							}
						}
					}
				}
				// add participants that are mandatory
				for (BPMNTask readerBrt : readerBrts) {
					BPMNExclusiveGateway exclGtw = (BPMNExclusiveGateway) readerBrt.getSuccessors().iterator().next();
					LinkedList<Constraint> constraints = exclGtw.getConstraints();
					for (Constraint constraint : constraints) {
						if (constraint instanceof MandatoryParticipantConstraint) {
							staticSphereForDataObject
									.add(((MandatoryParticipantConstraint) constraint).getMandatoryParticipant());
						}
					}
				}

				for (BPMNParticipant participant : staticSphereForDataObject) {
					this.increaseOccurenceOfParticipant(participant, sphereSumOfParticipantForBrt);
				}
			}

			if (this.requiredSphereIsAtLeast(requiredSphere, this.weakDynamicSphereKey)) {

				for (BPMNElement writer : this.getLastWriterListForDataObject(currentBrt, dataO)) {
					BPMNTask origin = (BPMNTask) writer;
					LinkedList<LinkedList<HashSet<?>>> wdSphere = wdSpherePerDataObject.get(dataO);
					for (LinkedList<HashSet<?>> wdEntry : wdSphere) {
						if (Thread.currentThread().isInterrupted()) {
							System.err.println("Interrupted! " + Thread.currentThread().getName());
							throw new InterruptedException();
						}
						BPMNTask currOrigin = (BPMNTask) wdEntry.get(0).iterator().next();
						if (origin.equals(currOrigin)) {
							HashSet<BPMNParticipant> wdParticipants = new HashSet<BPMNParticipant>(
									(HashSet<BPMNParticipant>) wdEntry.get(2));

							HashSet<BPMNTask> wdBrts = (HashSet<BPMNTask>) wdEntry.get(1);

							if (incremental) {
								if (alreadyChosenAdditionalActorsPerBrt != null) {
									// add the additional actors of already queried brts
									for (Entry<BPMNBusinessRuleTask, LinkedList<AdditionalActors>> alreadyChosenAddActorsEntry : alreadyChosenAdditionalActorsPerBrt
											.entrySet()) {
										BPMNBusinessRuleTask brtWithAddActors = alreadyChosenAddActorsEntry.getKey();
										if (wdBrts.contains(brtWithAddActors)) {
											for (AdditionalActors addActors : alreadyChosenAddActorsEntry.getValue()) {
												wdParticipants.addAll(addActors.getAdditionalActors());
											}
										}
									}

								}
							}
							// add participants that are mandatory
							for (BPMNTask readerBrt : wdBrts) {
								BPMNExclusiveGateway exclGtw = (BPMNExclusiveGateway) readerBrt.getSuccessors()
										.iterator().next();
								LinkedList<Constraint> constraints = exclGtw.getConstraints();
								for (Constraint constraint : constraints) {
									if (constraint instanceof MandatoryParticipantConstraint) {
										wdParticipants.add(((MandatoryParticipantConstraint) constraint)
												.getMandatoryParticipant());
									}
								}
							}

							for (BPMNParticipant participant : wdParticipants) {
								this.increaseOccurenceOfParticipant(participant, sphereSumOfParticipantForBrt);
							}

						}
					}

					if (this.requiredSphereIsAtLeast(requiredSphere, this.strongDynamicSphereKey)) {
						// compute sd for (DataObject, Origin, currentBrt)
						LinkedList<LinkedList<BPMNElement>> pathsFromOriginToEnd = this
								.getOrComputePathsFromOriginToEnd(pathsFromOriginToEndMap, origin);
						HashSet<BPMNParticipant> sdParticipants = this.getSdActors(dataO, origin, currentBrt,
								alreadyChosenAdditionalActorsPerBrt, pathsFromOriginToEnd);

						for (BPMNParticipant participant : sdParticipants) {
							this.increaseOccurenceOfParticipant(participant, sphereSumOfParticipantForBrt);
						}
					}

				}
			}

		}
		// order the participant by the cheapest ones
		TreeMap<Double, LinkedList<BPMNParticipant>> cheapest = new TreeMap<Double, LinkedList<BPMNParticipant>>(
				Collections.reverseOrder());
		for (Entry<BPMNParticipant, Double> costPerParticipant : sphereSumOfParticipantForBrt.entrySet()) {
			cheapest.computeIfAbsent(costPerParticipant.getValue(), k -> new LinkedList<BPMNParticipant>())
					.add(costPerParticipant.getKey());
		}

		return cheapest;

	}

	public LinkedList<AdditionalActors> getCheapestAdditionalActorsForBrtHeuristic(boolean incremental,
			BPMNBusinessRuleTask currentBrt,
			HashMap<BPMNTask, LinkedList<LinkedList<BPMNElement>>> pathsFromOriginToEndMap,
			HashMap<BPMNDataObject, LinkedList<HashSet<?>>> staticSpherePerDataObject,
			HashMap<BPMNDataObject, LinkedList<LinkedList<HashSet<?>>>> wdSpherePerDataObject,
			HashMap<BPMNBusinessRuleTask, LinkedList<AdditionalActors>> alreadyChosenAdditionalActorsPerBrt, int bound)
			throws Exception {

		if (Thread.currentThread().isInterrupted()) {
			System.err.println("Interrupted! " + Thread.currentThread().getName());
			throw new InterruptedException();
		}
		LinkedList<AdditionalActors> addActorsForBrt = new LinkedList<AdditionalActors>();
		BPMNGateway gtw = (BPMNGateway) currentBrt.getSuccessors().iterator().next();
		BPMNExclusiveGateway exclGtw = (BPMNExclusiveGateway) gtw;
		int amountAddActors = exclGtw.getAmountAddActors();
		LinkedList<LinkedList<BPMNParticipant>> potentialAddActors = this.getPotentialAddActorsListsForBrt(currentBrt);

		HashSet<BPMNParticipant> cheapestAddActors = new HashSet<BPMNParticipant>();
		// add all mandatory participants
		cheapestAddActors.addAll(potentialAddActors.get(1));
		int addActorsToFind = amountAddActors - cheapestAddActors.size();

		if (addActorsToFind == 0) {
			// there are exactly as many participants mandatory as needed
			AdditionalActors addActors = new AdditionalActors(currentBrt, cheapestAddActors);
			addActorsForBrt.add(addActors);
		} else {
			TreeMap<Double, LinkedList<BPMNParticipant>> cheapest = this.getCheapestPotentialAdditionalActorsForBrt(
					incremental, currentBrt, pathsFromOriginToEndMap, staticSpherePerDataObject, wdSpherePerDataObject,
					alreadyChosenAdditionalActorsPerBrt);

			addActorsForBrt = this.generatePossibleCombinationsOfAdditionalActorsWithBoundForBrtOld(exclGtw,
					addActorsToFind, potentialAddActors.get(1), potentialAddActors.get(0), currentBrt, cheapest, null,
					null, bound);
		}

		return addActorsForBrt;

	}

	@SuppressWarnings("unchecked")
	public HashSet<BPMNTask> getReaderBrtsFromStaticSphereForDataObject(BPMNDataObject dataO,
			HashMap<BPMNDataObject, LinkedList<HashSet<?>>> staticSpherePerDataObject) throws Exception {
		HashSet<?> set = staticSpherePerDataObject.get(dataO).get(1);
		if (!set.isEmpty()) {
			if (set.iterator().next() instanceof BPMNTask) {
				return (HashSet<BPMNTask>) set;
			} else {
				throw new Exception(
						"There must be a HashSet<BPMNTask> with all brts reading the data object at index 1");
			}
		}
		return new HashSet<BPMNTask>();
	}

	@SuppressWarnings("unchecked")
	public HashSet<BPMNParticipant> getParticipantsInStaticSphereForDataObject(BPMNDataObject dataO,
			HashMap<BPMNDataObject, LinkedList<HashSet<?>>> staticSpherePerDataObject) throws Exception {
		HashSet<?> set = staticSpherePerDataObject.get(dataO).get(0);
		if (!set.isEmpty()) {
			if (set.iterator().next() instanceof BPMNParticipant) {
				return (HashSet<BPMNParticipant>) set;
			} else {
				throw new Exception(
						"There must be a HashSet<BPMNParticipant> with all participants in static sphere at index 0!");
			}
		}
		return new HashSet<BPMNParticipant>();
	}

	public void increaseOccurenceOfParticipant(BPMNParticipant participant,
			HashMap<BPMNParticipant, Double> sumCostForBrt) {
		double cost = sumCostForBrt.getOrDefault(participant, 0.0);
		double newCost = cost + 1;
		sumCostForBrt.put(participant, newCost);
	}

	/*public LinkedList<LinkedList<AdditionalActors>> computeAllCheapestAddActorsLists(
			LinkedList<AdditionalActors> additionalActorsForBrt,
			LinkedList<LinkedList<AdditionalActors>> allCheapestAddActorsLists) {
		LinkedList<LinkedList<AdditionalActors>> newCheapestLists = new LinkedList<LinkedList<AdditionalActors>>();
		if (allCheapestAddActorsLists.isEmpty()) {
			for (AdditionalActors addActorsCurrBrt : additionalActorsForBrt) {
				LinkedList<AdditionalActors> newCheapest = new LinkedList<AdditionalActors>();
				newCheapest.add(addActorsCurrBrt);
				newCheapestLists.add(newCheapest);
			}
		} else {
			// go through the list of current cheapest additionalActors of the cluster
			// add those additional actors, that lead to a minimal increase of distinct
			// additional actors in the current list
			newCheapestLists = this.computeListsWithMinIncreaseOfAddActors(allCheapestAddActorsLists,
					additionalActorsForBrt);
		}
		return newCheapestLists;
	}*/

	public LinkedList<LinkedList<AdditionalActors>> computeAllAddActorsLists(
			LinkedList<AdditionalActors> additionalActorsForBrt,
			LinkedList<LinkedList<AdditionalActors>> allCheapestAddActorsLists) {
		LinkedList<LinkedList<AdditionalActors>> newCheapestLists = new LinkedList<LinkedList<AdditionalActors>>();
		if (allCheapestAddActorsLists.isEmpty()) {
			for (AdditionalActors addActorsCurrBrt : additionalActorsForBrt) {
				LinkedList<AdditionalActors> newCheapest = new LinkedList<AdditionalActors>();
				newCheapest.add(addActorsCurrBrt);
				newCheapestLists.add(newCheapest);
			}
		} else {
			// get all new combinations
			for (LinkedList<AdditionalActors> cheapestList : allCheapestAddActorsLists) {
				for (AdditionalActors addActorsCurrBrt : additionalActorsForBrt) {
					LinkedList<AdditionalActors> newCheapest = new LinkedList<AdditionalActors>();
					newCheapest.addAll(cheapestList);
					newCheapest.add(addActorsCurrBrt);
					newCheapestLists.add(newCheapest);
				}
			}

		}
		return newCheapestLists;
	}

	public int getAmountParallelsBeforePreprocessing() {
		return this.amountParallelsBeforePreprocessing;
	}

	public LinkedList<MandatoryParticipantConstraint> getMandatoryParticipantConstraints() {
		return mandatoryParticipantConstraints;
	}

	public void setMandatoryParticipantConstraints(
			LinkedList<MandatoryParticipantConstraint> mandatoryParticipantConstraints) {
		this.mandatoryParticipantConstraints = mandatoryParticipantConstraints;
	}

	public LinkedList<ExcludeParticipantConstraint> getExcludeParticipantConstraints() {
		return excludeParticipantConstraints;
	}

	public void setExcludeParticipantConstraints(
			LinkedList<ExcludeParticipantConstraint> excludeParticipantConstraints) {
		this.excludeParticipantConstraints = excludeParticipantConstraints;
	}

	public HashSet<BPMNParticipant> getParticipantsInStaticSphere(boolean incremental, BPMNDataObject dataO,
			HashMap<BPMNDataObject, LinkedList<HashSet<?>>> staticSpherePerDataObject,
			HashMap<BPMNBusinessRuleTask, LinkedList<AdditionalActors>> alreadyChosenAdditionalActors)
			throws Exception {
		HashSet<BPMNParticipant> staticSphereForDataObject = new HashSet<BPMNParticipant>(
				this.getParticipantsInStaticSphereForDataObject(dataO, staticSpherePerDataObject));

		HashSet<BPMNTask> readerBrts = this.getReaderBrtsFromStaticSphereForDataObject(dataO,
				staticSpherePerDataObject);
		if (incremental) {
			if (alreadyChosenAdditionalActors != null) {
				// add already chosen additional actors
				for (Entry<BPMNBusinessRuleTask, LinkedList<AdditionalActors>> alreadyChosenAddActorsEntry : alreadyChosenAdditionalActors
						.entrySet()) {
					BPMNBusinessRuleTask brtWithAddActors = alreadyChosenAddActorsEntry.getKey();
					if (readerBrts.contains(brtWithAddActors)) {
						for (AdditionalActors addActors : alreadyChosenAddActorsEntry.getValue()) {
							staticSphereForDataObject.addAll(addActors.getAdditionalActors());
						}
					}
				}
			}
		}
		// add participants that are mandatory
		for (BPMNTask readerBrt : readerBrts) {
			BPMNExclusiveGateway exclGtw = (BPMNExclusiveGateway) readerBrt.getSuccessors().iterator().next();
			LinkedList<Constraint> constraints = exclGtw.getConstraints();
			for (Constraint constraint : constraints) {
				if (constraint instanceof MandatoryParticipantConstraint) {
					staticSphereForDataObject
							.add(((MandatoryParticipantConstraint) constraint).getMandatoryParticipant());
				}
			}
		}

		return staticSphereForDataObject;

	}

	public HashMap<BPMNParticipant, Double> generateSpheresMapForAllBrts(LinkedList<BPMNBusinessRuleTask> brtsToExclude,
			boolean incremental, HashMap<BPMNTask, LinkedList<LinkedList<BPMNElement>>> pathsFromOriginToEndMap,
			HashMap<BPMNDataObject, LinkedList<HashSet<?>>> staticSpherePerDataObject,
			HashMap<BPMNDataObject, LinkedList<LinkedList<HashSet<?>>>> wdSpherePerDataObject,
			HashMap<BPMNBusinessRuleTask, LinkedList<AdditionalActors>> alreadyFoundAdditionalActorsPerBrt)
			throws Exception {

		HashMap<BPMNParticipant, Double> overallBestCandidates = new HashMap<BPMNParticipant, Double>();
		for (BPMNBusinessRuleTask brt : this.businessRuleTasks) {
			if (!brtsToExclude.contains(brt)) {
				TreeMap<Double, LinkedList<BPMNParticipant>> localCheapestCandidates = this
						.getCheapestPotentialAdditionalActorsForBrt(incremental, brt, pathsFromOriginToEndMap,
								staticSpherePerDataObject, wdSpherePerDataObject, alreadyFoundAdditionalActorsPerBrt);

				for (Entry<Double, LinkedList<BPMNParticipant>> localCandidatesEntry : localCheapestCandidates
						.entrySet()) {
					double value = localCandidatesEntry.getKey();
					for (BPMNParticipant candidate : localCandidatesEntry.getValue()) {
						double currentValue = overallBestCandidates.getOrDefault(candidate, 0.0);
						double newValue = currentValue + value;
						overallBestCandidates.put(candidate, newValue);
					}
				}
			}
		}
		return overallBestCandidates;
	}

	public synchronized Future<LinkedList<PModelWithAdditionalActors>> executeCallable(ExecutorService executor,
			Enums.AlgorithmToPerform algToPerform, int bound) {
		Callable<LinkedList<PModelWithAdditionalActors>> callable = null;

		if (algToPerform.equals(Enums.AlgorithmToPerform.EXHAUSTIVE)) {
			callable = () -> this.exhaustiveSearch(bound);
		} else if (algToPerform.equals(Enums.AlgorithmToPerform.BASEHEURISTIC)) {
			callable = () -> this.baseHeuristicSearch(bound);
		} else if (algToPerform.equals(Enums.AlgorithmToPerform.INCREMENTALHEURISTIC)) {
			callable = () -> this.incrementalHeuristicSearch(bound);
		} else if (algToPerform.equals(Enums.AlgorithmToPerform.ADVANCEDHEURISTIC)) {
			callable = () -> this.advancedHeuristicSearch(bound);
		}

		return executor.submit(callable);
	}

	public BpmnModelInstance getModelInstanceWithParallels() {
		return modelInstanceWithParallels;
	}

	public void setModelInstanceWithParallels(BpmnModelInstance modelInstanceWithParallels) {
		this.modelInstanceWithParallels = modelInstanceWithParallels;
	}

	public LinkedList<AdditionalActors> generatePossibleCombinationsOfAdditionalActorsWithBoundForBrtOld(
			BPMNExclusiveGateway gtw, int verifiersToFind, LinkedList<BPMNParticipant> mandatoryParticipants,
			LinkedList<BPMNParticipant> possibleParticipantsAsAddActors, BPMNBusinessRuleTask brt,
			Map<Double, LinkedList<BPMNParticipant>> bestParticipantsMap,
			TreeMap<Double, LinkedList<BPMNParticipant>> tiebreakerMap1,
			TreeMap<Double, LinkedList<BPMNParticipant>> tiebreakerMap2, int bound)
			throws InterruptedException, NotEnoughAddActorsException {

		LinkedList<AdditionalActors> addActorsCombinationsForBrt = new LinkedList<AdditionalActors>();
		LinkedList<BPMNParticipant> participantsWithBestCost = new LinkedList<BPMNParticipant>();
		LinkedList<BPMNParticipant> remainingWithBestCost = new LinkedList<BPMNParticipant>();
		int sumVerifiersOfGtw = gtw.getAmountAddActors();

		if (verifiersToFind > 0) {
			for (Entry<Double, LinkedList<BPMNParticipant>> entry : bestParticipantsMap.entrySet()) {
				// take all participants of the current cost that are not excluded and not the
				// actor of the brt
				LinkedList<BPMNParticipant> participantsWithCurrentCost = new LinkedList<BPMNParticipant>();
				for (BPMNParticipant participant : entry.getValue()) {
					if (possibleParticipantsAsAddActors.contains(participant)
							&& !mandatoryParticipants.contains(participant)) {
						participantsWithCurrentCost.add(participant);
					}
				}

				// check if there are still verifiers needed
				verifiersToFind = verifiersToFind - participantsWithCurrentCost.size();

				if (verifiersToFind > 0) {
					// there are less participants on the current cost level than needed
					// take all of them
					participantsWithBestCost.addAll(participantsWithCurrentCost);
				} else if (verifiersToFind == 0) {
					// there are exactly as many participants on the current cost level as needed
					// take all of them and stop iterating
					participantsWithBestCost.addAll(participantsWithCurrentCost);
					break;
				} else {
					// there are more participants on the current cost level than needed
					// stop iterating
					remainingWithBestCost.addAll(participantsWithCurrentCost);
					break;
				}

			}
		}

		LinkedList<LinkedList<BPMNParticipant>> lists = new LinkedList<LinkedList<BPMNParticipant>>();
		int amountParticipantsToTakeFromRemaining = sumVerifiersOfGtw - mandatoryParticipants.size()
				- participantsWithBestCost.size();

		if (tiebreakerMap1 != null && amountParticipantsToTakeFromRemaining > 0) {
			// there are several participants remaining on a plateau (= same cost)
			// sort the remaining participants by their cost in the tiebreakerMap1

			LinkedList<BPMNParticipant> remainingSortedByTiebreaker1 = new LinkedList<BPMNParticipant>();
			LinkedList<BPMNParticipant> notFoundInTiebreaker1 = new LinkedList<BPMNParticipant>();
			notFoundInTiebreaker1.addAll(remainingWithBestCost);
			LinkedList<BPMNParticipant> remainingSortedByTiebreaker1WithFillUp = new LinkedList<BPMNParticipant>();

			for (Entry<Double, LinkedList<BPMNParticipant>> entry : tiebreakerMap1.entrySet()) {
				// preserve the order of the local cheapest list
				// to get same results as incremental heuristic when the tieBrakerMap has the same
				// elements
				// on the plateau
				// the elements from the remainingWithBestCost are on a plateau
				// the same elements may be on different levels in the tie breaker map!

				for (BPMNParticipant remainingPart : remainingWithBestCost) {
					if (entry.getValue().contains(remainingPart)) {
						remainingSortedByTiebreaker1.add(remainingPart);
						notFoundInTiebreaker1.remove(remainingPart);
					}
				}

			}

			if (tiebreakerMap2 != null) {
				// check which remaining elements have not changed their places due to tiebreaking
				LinkedList<BPMNParticipant> list = new LinkedList<BPMNParticipant>();
				for (int i = 0; i < remainingWithBestCost.size(); i++) {
					BPMNParticipant remainingPart = remainingWithBestCost.get(i);
					if (remainingSortedByTiebreaker1.get(i).equals(remainingPart)) {
						// still on same place after applying tiebreaker rules
						list.add(remainingPart);
					}
				}

				//sort the elements that have not changed their place
				LinkedList<BPMNParticipant> listAfterSort = new LinkedList<BPMNParticipant>();
				for (Entry<Double, LinkedList<BPMNParticipant>> entry : tiebreakerMap2.entrySet()) {
					for (BPMNParticipant p : list) {
						if (entry.getValue().contains(p)) {
							listAfterSort.add(p);
						}
					}

				}

				// sort the not found participants with tiebreakerMap2
				LinkedList<BPMNParticipant> notFoundSorted = new LinkedList<BPMNParticipant>();
				if (tiebreakerMap2 != null) {
					for (Entry<Double, LinkedList<BPMNParticipant>> entry : tiebreakerMap2.entrySet()) {
						for (BPMNParticipant part : notFoundInTiebreaker1) {
							if (entry.getValue().contains(part)) {
								notFoundSorted.add(part);
							}
						}

					}

				}

				remainingSortedByTiebreaker1WithFillUp.addAll(listAfterSort);
				remainingSortedByTiebreaker1WithFillUp.addAll(notFoundSorted);

			} else {
				remainingSortedByTiebreaker1WithFillUp.addAll(remainingSortedByTiebreaker1);
				remainingSortedByTiebreaker1WithFillUp.addAll(notFoundInTiebreaker1);

			}

			if (remainingSortedByTiebreaker1.size() >= amountParticipantsToTakeFromRemaining) {
				remainingWithBestCost = remainingSortedByTiebreaker1;

			} else if (remainingSortedByTiebreaker1.size() < amountParticipantsToTakeFromRemaining) {
				// fill up
				remainingWithBestCost = remainingSortedByTiebreaker1WithFillUp;
			}

		}

		if (!remainingWithBestCost.isEmpty() && amountParticipantsToTakeFromRemaining > 0) {
			lists = Combination.getPermutationsWithBound(remainingWithBestCost, amountParticipantsToTakeFromRemaining,
					bound);
		}

		if (lists.isEmpty()) {
			LinkedList<BPMNParticipant> listToAdd = new LinkedList<BPMNParticipant>();
			listToAdd.addAll(mandatoryParticipants);
			listToAdd.addAll(participantsWithBestCost);
			if (listToAdd.size() < sumVerifiersOfGtw) {
				throw new NotEnoughAddActorsException(sumVerifiersOfGtw + " verifiers needed for " + gtw.getId()
						+ " but only " + listToAdd.size() + " found!");
			}
			AdditionalActors addActors = new AdditionalActors(brt, listToAdd);
			addActorsCombinationsForBrt.add(addActors);

		} else {
			// iterate through all combinations
			for (int i = 0; i < lists.size(); i++) {
				LinkedList<BPMNParticipant> currList = lists.get(i);
				currList.addAll(0, participantsWithBestCost);
				currList.addAll(0, mandatoryParticipants);
				if (currList.size() < sumVerifiersOfGtw) {
					throw new NotEnoughAddActorsException(sumVerifiersOfGtw + " verifiers needed for " + gtw.getId()
							+ " but only " + currList.size() + " found!");
				}
				AdditionalActors addActors = new AdditionalActors(brt, currList);
				addActorsCombinationsForBrt.add(addActors);

			}

		}
		if (addActorsCombinationsForBrt.isEmpty()) {
			throw new NotEnoughAddActorsException("No possible combination of verifiers for " + brt.getId());
		}

		return addActorsCombinationsForBrt;

	}

	public LinkedList<AdditionalActors> generatePossibleCombinationsOfAdditionalActorsWithBoundForBrt(
			BPMNExclusiveGateway gtw, int verifiersToFind, LinkedList<BPMNParticipant> mandatoryParticipants,
			LinkedList<BPMNParticipant> possibleParticipantsAsAddActors, BPMNBusinessRuleTask brt,
			Map<Double, LinkedList<BPMNParticipant>> bestParticipantsMap,
			HashMap<BPMNParticipant, Double> tiebreakerMap1,
			HashMap<BPMNDataObject, HashMap<BPMNParticipant, Double>> staticSphereTiebreakerMap, int bound)
			throws InterruptedException, NotEnoughAddActorsException {

		LinkedList<AdditionalActors> addActorsCombinationsForBrt = new LinkedList<AdditionalActors>();
		LinkedList<BPMNParticipant> participantsWithBestCost = new LinkedList<BPMNParticipant>();
		LinkedList<BPMNParticipant> remainingWithBestCost = new LinkedList<BPMNParticipant>();
		int sumVerifiersOfGtw = gtw.getAmountAddActors();

		if (verifiersToFind > 0) {
			for (Entry<Double, LinkedList<BPMNParticipant>> entry : bestParticipantsMap.entrySet()) {
				// take all participants of the current cost that are not excluded and not the
				// actor of the brt
				LinkedList<BPMNParticipant> participantsWithCurrentCost = new LinkedList<BPMNParticipant>();
				for (BPMNParticipant participant : entry.getValue()) {
					if (possibleParticipantsAsAddActors.contains(participant)
							&& !mandatoryParticipants.contains(participant)) {
						participantsWithCurrentCost.add(participant);
					}
				}

				// check if there are still verifiers needed
				verifiersToFind = verifiersToFind - participantsWithCurrentCost.size();

				if (verifiersToFind > 0) {
					// there are less participants on the current cost level than needed
					// take all of them
					participantsWithBestCost.addAll(participantsWithCurrentCost);
				} else if (verifiersToFind == 0) {
					// there are exactly as many participants on the current cost level as needed
					// take all of them and stop iterating
					participantsWithBestCost.addAll(participantsWithCurrentCost);
					break;
				} else {
					// there are more participants on the current cost level than needed
					// stop iterating
					remainingWithBestCost.addAll(participantsWithCurrentCost);
					break;
				}

			}
		}

		LinkedList<LinkedList<BPMNParticipant>> lists = new LinkedList<LinkedList<BPMNParticipant>>();
		int amountParticipantsToTakeFromRemaining = sumVerifiersOfGtw - mandatoryParticipants.size()
				- participantsWithBestCost.size();

		TreeMap<Double, LinkedList<BPMNParticipant>> combinedTiebreakerMap = new TreeMap<Double, LinkedList<BPMNParticipant>>();
		TreeMap<Double, LinkedList<BPMNParticipant>> combinedTiebreakerMapForBrt = new TreeMap<Double, LinkedList<BPMNParticipant>>();

		if (tiebreakerMap1 != null || staticSphereTiebreakerMap != null) {
			// compute combined tiebreakerMap

			HashMap<BPMNParticipant, Double> combinedMap = new HashMap<BPMNParticipant, Double>();
			HashMap<BPMNParticipant, Double> combinedMapForBrt = new HashMap<BPMNParticipant, Double>();

			if (tiebreakerMap1 != null) {
				for (Entry<BPMNParticipant, Double> map1Entry : tiebreakerMap1.entrySet()) {
					double amount = map1Entry.getValue();
					double currAmount = combinedMap.getOrDefault(map1Entry.getKey(), 0.0);
					double nextAmount = currAmount + amount;
					combinedMap.put(map1Entry.getKey(), nextAmount);
				}
			}

			if (staticSphereTiebreakerMap != null) {
				for (Entry<BPMNDataObject, HashMap<BPMNParticipant, Double>> staticSphereMapEntry : staticSphereTiebreakerMap
						.entrySet()) {
					BPMNDataObject dataO = staticSphereMapEntry.getKey();
					for (Entry<BPMNParticipant, Double> innerMapEntry : staticSphereMapEntry.getValue().entrySet()) {
						double amount = innerMapEntry.getValue();
						double currAmount = combinedMap.getOrDefault(innerMapEntry.getKey(), 0.0);
						double nextAmount = currAmount + amount;
						combinedMap.put(innerMapEntry.getKey(), nextAmount);

						if (brt.getDataObjects().contains(dataO)) {
							combinedMapForBrt.put(innerMapEntry.getKey(), nextAmount);
						}
					}
				}
			}
			
			combinedTiebreakerMap = CommonFunctionality.computeMapDescendingOrder(combinedMap);
			combinedTiebreakerMapForBrt = CommonFunctionality.computeMapDescendingOrder(combinedMapForBrt);

		}

		if (!combinedTiebreakerMapForBrt.isEmpty()) {

			LinkedList<BPMNParticipant> combinedList = new LinkedList<BPMNParticipant>();
			for (LinkedList<BPMNParticipant> pList : combinedTiebreakerMapForBrt.values()) {
				// participant who are in static sphere of data objects connected to brt will be preferred				
				for (BPMNParticipant p : pList) {
					if (!participantsWithBestCost.contains(p) && !brt.getParticipant().equals(p)) {
						combinedList.add(p);
					}
				}
			}

			if (combinedList.size() < remainingWithBestCost.size()) {

				for (LinkedList<BPMNParticipant> pList : combinedTiebreakerMap.values()) {
					for (BPMNParticipant p : pList) {

						if (!combinedList.contains(p) && !participantsWithBestCost.contains(p)
								&& !brt.getParticipant().equals(p)) {
							combinedList.add(p);
						}
					}
				}
			}

			remainingWithBestCost = combinedList;
		}

		//		if (!combinedTiebreakerMap.isEmpty() && amountParticipantsToTakeFromRemaining > 0) {
		//			// there are several participants remaining on a plateau (= same cost)
		//			// sort the remaining participants by their cost in the combinedTiebreakerMap
		//			LinkedList<BPMNParticipant>sortedRemainingWithBestCost = new LinkedList<BPMNParticipant>();
		//			LinkedList<BPMNParticipant> notFound = new LinkedList<BPMNParticipant>();
		//			notFound.addAll(remainingWithBestCost);
		//				// preserve the order of the local cheapest list
		//				// to get same results as incremental heuristic when the tieBrakerMap has the same
		//				// elements
		//				// on the plateau
		//				// the elements from the remainingWithBestCost are on a plateau
		//				// the same elements may be on different levels in the tie breaker map!
		//
		//			for (BPMNParticipant remainingPart : remainingWithBestCost) {
		//					for (Entry<Double, LinkedList<BPMNParticipant>> entry : combinedTiebreakerMap.entrySet()) {
		//
		//					if (entry.getValue().contains(remainingPart)) {
		//						sortedRemainingWithBestCost.add(remainingPart);
		//						notFound.remove(remainingPart);						
		//					} 
		//				}
		//
		//			}
		//			
		//			if(!notFound.isEmpty()) {
		//				sortedRemainingWithBestCost.addAll(notFound);
		//			}
		//			
		//			remainingWithBestCost = sortedRemainingWithBestCost;
		//		}

		if (!remainingWithBestCost.isEmpty() && amountParticipantsToTakeFromRemaining > 0) {
			lists = Combination.getPermutationsWithBound(remainingWithBestCost, amountParticipantsToTakeFromRemaining,
					bound);
		}

		if (lists.isEmpty()) {
			LinkedList<BPMNParticipant> listToAdd = new LinkedList<BPMNParticipant>();
			listToAdd.addAll(mandatoryParticipants);
			listToAdd.addAll(participantsWithBestCost);
			if (listToAdd.size() < sumVerifiersOfGtw) {
				throw new NotEnoughAddActorsException(sumVerifiersOfGtw + " verifiers needed for " + gtw.getId()
						+ " but only " + listToAdd.size() + " found!");
			}
			AdditionalActors addActors = new AdditionalActors(brt, listToAdd);
			addActorsCombinationsForBrt.add(addActors);

		} else {
			// iterate through all combinations
			for (int i = 0; i < lists.size(); i++) {
				LinkedList<BPMNParticipant> currList = lists.get(i);
				currList.addAll(0, participantsWithBestCost);
				currList.addAll(0, mandatoryParticipants);
				if (currList.size() < sumVerifiersOfGtw) {
					throw new NotEnoughAddActorsException(sumVerifiersOfGtw + " verifiers needed for " + gtw.getId()
							+ " but only " + currList.size() + " found!");
				}
				AdditionalActors addActors = new AdditionalActors(brt, currList);
				addActorsCombinationsForBrt.add(addActors);

			}

		}
		if (addActorsCombinationsForBrt.isEmpty()) {
			throw new NotEnoughAddActorsException("No possible combination of verifiers for " + brt.getId());
		}

		return addActorsCombinationsForBrt;

	}

	private Static_SphereEntry generateStaticSphereEntry(PModelWithAdditionalActors pModelWithAdditionalActors,
			BPMNDataObject dataO, Entry<BPMNDataObject, LinkedList<HashSet<?>>> staticSpherePerDataObject)
			throws InterruptedException {
		HashSet<BPMNParticipant> staticSphereParticipants = (HashSet<BPMNParticipant>) staticSpherePerDataObject
				.getValue().get(0);
		HashSet<BPMNTask> readerBrtsOfDataO = (HashSet<BPMNTask>) staticSpherePerDataObject.getValue().get(1);
		Static_SphereEntry currStaticEntry = new Static_SphereEntry(dataO, staticSphereParticipants, readerBrtsOfDataO);
		pModelWithAdditionalActors.getStaticSphereEntries().put(dataO, currStaticEntry);

		HashSet<BPMNParticipant> staticSphereWithAdditionalActors = new HashSet<BPMNParticipant>();
		// add all actors of static sphere
		staticSphereWithAdditionalActors.addAll(currStaticEntry.getStaticSphere());

		for (AdditionalActors additionalActors : pModelWithAdditionalActors.getAdditionalActorsList()) {
			if (Thread.currentThread().isInterrupted()) {
				System.err.println("Interrupted! " + Thread.currentThread().getName());
				throw new InterruptedException();
			}
			BPMNBusinessRuleTask brt = additionalActors.getCurrBrt();
			LinkedList<BPMNParticipant> addActors = additionalActors.getAdditionalActors();

			// add additional actors of reading brts
			for (BPMNTask readerBrt : currStaticEntry.getReaderBrts()) {
				if (readerBrt.getId().contentEquals(brt.getId())) {
					staticSphereWithAdditionalActors.addAll(addActors);
				}
			}
		}
		// set static sphere'
		currStaticEntry.setStaticSphereWithAdditionalActors(staticSphereWithAdditionalActors);

		return currStaticEntry;
	}

	private boolean testIfAdditionalActorsValid(PModelWithAdditionalActors pModel) {
		for (AdditionalActors addAct : pModel.getAdditionalActorsList()) {
			LinkedList<BPMNParticipant> addActors = addAct.getAdditionalActors();
			BPMNBusinessRuleTask brt = addAct.getCurrBrt();
			BPMNExclusiveGateway gtw = (BPMNExclusiveGateway) brt.getSuccessors().iterator().next();
			if (addAct.getAdditionalActors().size() != gtw.getAmountAddActors()) {
				return false;
			}

			for (Constraint constr : gtw.getConstraints()) {
				if (constr instanceof MandatoryParticipantConstraint) {
					MandatoryParticipantConstraint mandC = (MandatoryParticipantConstraint) constr;

					if (!addActors.contains(mandC.getMandatoryParticipant())) {
						return false;
					}

				}
				if (constr instanceof ExcludeParticipantConstraint) {
					ExcludeParticipantConstraint exclC = (ExcludeParticipantConstraint) constr;

					if (addActors.contains(exclC.getParticipantToExclude())) {
						return false;
					}
				}

			}

		}
		return true;

	}

	public HashSet<BPMNTask> computeCluster(BPMNBusinessRuleTask currBrt) {
		HashSet<BPMNTask> clusterSet = new HashSet<BPMNTask>();

		for (BPMNBusinessRuleTask brt : this.businessRuleTasks) {
			if (!brt.equals(currBrt)) {
				innerloop: for (Entry<BPMNDataObject, ArrayList<BPMNTask>> originsEntry : brt.getLastWriterList()
						.entrySet()) {

					for (Entry<BPMNDataObject, ArrayList<BPMNTask>> originsEntryOfCurrBrt : currBrt.getLastWriterList()
							.entrySet()) {

						if (originsEntry.getKey().equals(originsEntryOfCurrBrt.getKey())) {
							for (BPMNTask origin : originsEntry.getValue()) {
								if (originsEntryOfCurrBrt.getValue().contains(origin)) {
									clusterSet.add(brt);
									break innerloop;
								}
							}

						}
					}

				}

			}

		}

		return clusterSet;
	}

	//	public HashMap<BPMNDataObject, HashMap<BPMNParticipant, Double>> computeStaticSphereTiebreakerHashMap() {
	//		HashMap<BPMNDataObject, HashMap<BPMNParticipant, Double>> mapToReturn = new HashMap<BPMNDataObject, HashMap<BPMNParticipant, Double>>();
	//		for (BPMNDataObject dataO : this.dataObjects) {
	//			HashMap<BPMNParticipant, Double> occurrencesMapForDataO = new HashMap<BPMNParticipant, Double>();
	//			for (BPMNElement el : dataO.getStaticSphereElements()) {
	//				if (el instanceof BPMNTask) {
	//					BPMNTask task = (BPMNTask) el;
	//					BPMNParticipant part = task.getParticipant();
	//					double curr = occurrencesMapForDataO.getOrDefault(part, 0.0);
	//					double next = ++curr;
	//					occurrencesMapForDataO.put(part, next);
	//				}
	//
	//			}
	//			mapToReturn.putIfAbsent(dataO, occurrencesMapForDataO);
	//			
	//		}
	//
	//		return mapToReturn;
	//
	//	}

	public HashMap<BPMNDataObject, HashMap<BPMNParticipant, Double>> computeStaticSphereTiebreakerHashMap() {
		HashMap<BPMNDataObject, HashMap<BPMNParticipant, Double>> mapToReturn = new HashMap<BPMNDataObject, HashMap<BPMNParticipant, Double>>();
		for (BPMNDataObject dataO : this.dataObjects) {
			HashMap<BPMNParticipant, Double> occurrencesMapForDataO = new HashMap<BPMNParticipant, Double>();
			for (BPMNParticipant part : dataO.getStaticSphere()) {

				double curr = occurrencesMapForDataO.getOrDefault(part, 0.0);
				double next = ++curr;
				occurrencesMapForDataO.put(part, next);

			}
			mapToReturn.putIfAbsent(dataO, occurrencesMapForDataO);

		}

		return mapToReturn;

	}

	//	public TreeMap<Double, LinkedList<BPMNParticipant>> computeStaticSphereTiebreakerMap() {
	//		HashMap<BPMNParticipant, Double> occurrences = new HashMap<BPMNParticipant, Double>();
	//		
	//		
	//		
	//		for (BPMNDataObject dataO : this.dataObjects) {
	//			for (BPMNParticipant part : dataO.getStaticSphere()) {
	//					double curr = occurrences.getOrDefault(part, 0.0);
	//					double next = ++curr;
	//					occurrences.put(part, next);
	//			}
	//		}
	//
	//		return CommonFunctionality.computeMapDescendingOrder(occurrences);
	//
	//	}

}
