package functionality2;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
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
import functionality.CommonFunctionality;
import functionality.Constraint;
import functionality.ExcludeParticipantConstraint;
import functionality.MandatoryParticipantConstraint;
import functionality2.Enums.AlgorithmToPerform;
import functionality2.Enums.ClusterCondition;

public class API2 {

	private BpmnModelInstance modelInstance;
	private File processModelFile;
	private HashSet<BPMNParticipant> privateSphere;
	private HashSet<BPMNParticipant> publicSphere;
	private String algorithmToPerform;
	private String clusterCondition;
	private String amountPossibleCombinationsOfParticipants;
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
	private HashMap<BPMNDataObject, HashSet<BPMNBusinessRuleTask>> relatedBrts;

	public API2(String pathToBpmnCamundaFile, LinkedList<Double> weightingParameters) throws Exception {
		if (weightingParameters.size() != 3) {
			throw new Exception("Not exactly 3 weighting cost parameters in the list!");
		}
		this.weightingParameters = weightingParameters;
		this.processModelFile = new File(pathToBpmnCamundaFile);
		// preprocess the model, i.e. remove parallel branches
		BpmnModelInstance modelInst = Bpmn.readModelFromFile(this.processModelFile);
		this.modelInstance = CommonFunctionality.doPreprocessing(modelInst);
		System.out.println("API for: " + this.processModelFile.getName());
		boolean correctModel = true;
		try {
			CommonFunctionality.isCorrectModel(this.modelInstance);
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
		this.privateSphere = new HashSet<BPMNParticipant>();
		this.publicSphere = new HashSet<BPMNParticipant>();
		// set the default algorithm to perform
		this.setAlgorithmToPerform(Enums.AlgorithmToPerform.EXHAUSTIVE, 0);
		// set the default cluster condition
		this.setClusterCondition(Enums.ClusterCondition.ALLORIGINSTHESAME);
		this.amountPossibleCombinationsOfParticipants = "0";
		this.mandatoryParticipantConstraints = new LinkedList<MandatoryParticipantConstraint>();
		this.excludeParticipantConstraints = new LinkedList<ExcludeParticipantConstraint>();
		this.modelWithLanes = false;
		this.businessRuleTasks = new LinkedList<BPMNBusinessRuleTask>();
		this.privateSphereKey = "Private";
		this.staticSphereKey = "Static";
		this.weakDynamicSphereKey = "Weak-Dynamic";
		this.strongDynamicSphereKey = "Strong-Dynamic";
		this.relatedBrts = new HashMap<BPMNDataObject, HashSet<BPMNBusinessRuleTask>>();
		// map all the elements from camunda
		this.mapCamundaElements();
		this.pathsThroughProcess = CommonFunctionality.getAllPathsBetweenNodes(this.modelInstance,
				this.startEvent.getId(), this.endEvent.getId());
	}

	@SuppressWarnings("unchecked")
	public LinkedList<PModelWithAdditionalActors> exhaustiveSearch() throws Exception {
		long startTime = System.currentTimeMillis();

		HashMap<BPMNDataObject, LinkedList<HashSet<?>>> staticSpherePerDataObject = this
				.computeStaticSpherePerDataObject();

		// compute wd sphere for origins without additional actors
		// only for origins of data objects (may be the same for different brts!)
		HashMap<BPMNDataObject, LinkedList<LinkedList<HashSet<?>>>> wdSpherePerDataObject = this.computeWdSphere();

		LinkedList<PModelWithAdditionalActors> additionalActorsCombs = null;
		// when staticSpherePerDataObject is empty -> all dataObjects are private
		if (!staticSpherePerDataObject.isEmpty()) {
			// generate all possible combinations of additional readers
			additionalActorsCombs = this.generatePossibleCombinationsOfAdditionalActorsWithBound(0);

			for (PModelWithAdditionalActors pModelWithAdditionalActors : additionalActorsCombs) {
				pModelWithAdditionalActors.setPrivateSphere(this.privateSphere);

				for (Entry<BPMNDataObject, LinkedList<HashSet<?>>> staticSpherePerDataO : staticSpherePerDataObject
						.entrySet()) {
					BPMNDataObject currDataO = staticSpherePerDataO.getKey();
					HashSet<BPMNParticipant> staticSphereParticipants = (HashSet<BPMNParticipant>) staticSpherePerDataO
							.getValue().get(0);
					HashSet<BPMNTask> readerBrtsOfDataO = (HashSet<BPMNTask>) staticSpherePerDataO.getValue().get(1);
					Static_SphereEntry currStaticEntry = new Static_SphereEntry(currDataO, staticSphereParticipants,
							readerBrtsOfDataO);
					pModelWithAdditionalActors.getStaticSphereEntries().put(currDataO, currStaticEntry);

					// compute static sphere with additional actors
					HashSet<BPMNParticipant> staticSphereWithAdditionalActors = new HashSet<BPMNParticipant>();
					// add all actors of static sphere
					staticSphereWithAdditionalActors.addAll(currStaticEntry.getStaticSphere());

					for (AdditionalActors additionalActors : pModelWithAdditionalActors.getAdditionalActorsList()) {
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

				// compute beta measure
				// i.e. query brts in any order and get origins for each data object
				// weight(w) is the depth of the origin
				LinkedList<AdditionalActors> currAddActorsList = pModelWithAdditionalActors.getAdditionalActorsList();
				for (Entry<BPMNDataObject, LinkedList<LinkedList<HashSet<?>>>> wdSpherePerDataObjectAndOrigin : wdSpherePerDataObject
						.entrySet()) {
					BPMNDataObject currDataO = wdSpherePerDataObjectAndOrigin.getKey();
					LinkedList<LinkedList<HashSet<?>>> wdSphereList = wdSpherePerDataObjectAndOrigin.getValue();
					for (int i = 0; i < wdSphereList.size(); i++) {
						BPMNTask origin = (BPMNTask) wdSphereList.get(i).get(0).iterator().next();
						HashSet<BPMNTask> wdReaderBrts = (HashSet<BPMNTask>) wdSphereList.get(i).get(1);
						HashSet<BPMNParticipant> wdSphere = (HashSet<BPMNParticipant>) wdSphereList.get(i).get(2);
						WD_SphereEntry currWDSphereEntry = new WD_SphereEntry(currDataO, origin, wdReaderBrts,
								wdSphere);
						pModelWithAdditionalActors.getWdSphereEntries()
								.computeIfAbsent(currDataO, k -> new LinkedList<WD_SphereEntry>())
								.add(currWDSphereEntry);

						// compute wd' sphere with additional actors
						// add the additional actors to the brts reachable from an origin
						HashSet<BPMNParticipant> wdWithAddActorsParticipants = new HashSet<BPMNParticipant>();
						// add all wd actors of that wdEntry
						wdWithAddActorsParticipants.addAll(currWDSphereEntry.getWdSphere());
						HashSet<BPMNTask> reachableBrts = currWDSphereEntry.getWdReaderBrts();

						for (AdditionalActors addActors : currAddActorsList) {
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

				// compute gamma measure
				// first: compute sd sphere with additional actors for all origins of brts
				// second: compute sd* sphere for all origins of brts without additional actors
				// of current brt

				// compute sd sphere for each combination (DataObject,Origin,BusinessRuleTask)
				HashMap<BPMNDataObject, LinkedList<SD_SphereEntry>> sdSphereEntries = new HashMap<BPMNDataObject, LinkedList<SD_SphereEntry>>();
				for (AdditionalActors addActors : pModelWithAdditionalActors.getAdditionalActorsList()) {
					BPMNBusinessRuleTask currBrt = addActors.getCurrBrt();
					for (Entry<BPMNDataObject, ArrayList<BPMNTask>> lastWriterEntry : currBrt.getLastWriterList()
							.entrySet()) {
						String sphereOfDataObject = lastWriterEntry.getKey().getDefaultSphere();
						boolean computeGammaMeasure = this.computeCurrentMeasure(sphereOfDataObject,
								this.strongDynamicSphereKey);

						if (computeGammaMeasure) {
							for (BPMNTask origin : lastWriterEntry.getValue()) {
								LinkedList<LinkedList<BPMNElement>> pathsFromOriginOverCurrBrtToEnd = this.goDfs(origin,
										currBrt, this.endEvent, new LinkedList<BPMNElement>(),
										new LinkedList<BPMNElement>(), new LinkedList<LinkedList<BPMNElement>>());
								BPMNDataObject dataO = lastWriterEntry.getKey();

								SD_SphereEntry sdSphereEntry = new SD_SphereEntry(dataO, origin, currBrt);

								// get the additional actors of all brts except currBrt
								LinkedList<AdditionalActors> addActorsWithoutCurrBrt = new LinkedList<AdditionalActors>();
								addActorsWithoutCurrBrt.addAll(pModelWithAdditionalActors.getAdditionalActorsList());
								Iterator<AdditionalActors> addActIter = addActorsWithoutCurrBrt.iterator();
								boolean removed = false;
								while (addActIter.hasNext() && !removed) {
									AdditionalActors currAddActors = addActIter.next();
									if (currAddActors.getCurrBrt().equals(currBrt)) {
										addActIter.remove();
										removed = true;
									}
								}

								System.out.println(dataO.getName()+" "+origin.getName()+" "+currBrt.getName());
								LinkedList<HashSet<BPMNParticipant>> sdSphereList = this
										.getSdActorsAndSetWeightingOfOrigin(dataO, origin, currBrt,
												pModelWithAdditionalActors.getAdditionalActorsList(),
												addActorsWithoutCurrBrt, pathsFromOriginOverCurrBrtToEnd,
												sdSphereEntry);

								// get the sd sphere with additional actors for all brts
								HashSet<BPMNParticipant> sdSphereWithAdditionalActorsForAllBrts = sdSphereList.get(0);
								sdSphereEntry.setSdSphereWithAdditionalActors(sdSphereWithAdditionalActorsForAllBrts);

								// get the sd* sphere with additional actors for all brts except currBrt
								HashSet<BPMNParticipant> sdSphereWithAdditionalActorsExceptCurrBrt = sdSphereList
										.get(1);
								sdSphereEntry
										.setSdSphereWithoutAddActorOfCurrBrt(sdSphereWithAdditionalActorsExceptCurrBrt);

								sdSphereEntries.computeIfAbsent(dataO, k -> new LinkedList<SD_SphereEntry>())
										.add(sdSphereEntry);

								// compute lambda and set score
								HashSet<BPMNParticipant> lambdaActors = this.computeLambdaActors(
										sdSphereEntry.getSdSphereWithAdditionalActors(),
										sdSphereEntry.getSdSphereWithoutAddActorOfCurrBrt());

								sdSphereEntry.setLambdaSdSphere(lambdaActors);
								double lambda = lambdaActors.size();

								// weight(w,r,d) is the amount of instance types in which r reads d from w
								double depthOfCurrBrt = Math.pow(2, -currBrt.getLabels().size());
								double secondWeight = sdSphereEntry.getWeightingOfOriginForCurrBrt() * depthOfCurrBrt;
								sdSphereEntry.setWeightingOfOriginForCurrBrt(secondWeight);

								double score = lambda * sdSphereEntry.getWeightingOfOrigin() * secondWeight;
								sdSphereEntry.setScore(score);

								// add to sum gamma measure
								double currGammaScoreSum = pModelWithAdditionalActors.getGammaMeasureSum();
								double newGammaScoreSum = currGammaScoreSum += score;
								pModelWithAdditionalActors.setGammaMeasureSum(newGammaScoreSum);
							}
						}

					}

				}
				pModelWithAdditionalActors.setSdSphereEntries(sdSphereEntries);

				// compute measure for pModelWithAdditionalActors
				// i.e. compute alpha, beta, gamma measure and weight it with given parameters
				double sum = pModelWithAdditionalActors.getAlphaMeasureSum()
						+ pModelWithAdditionalActors.getBetaMeasureSum()
						+ pModelWithAdditionalActors.getGammaMeasureSum();
				pModelWithAdditionalActors.setSumMeasure(sum);
			}
		} else {
			// all data objects are private
			// i.e. each combination of participants that satisfies the constraints is a
			// cheapest one
			// generate combinations of the size == bound that satisfy the constraints
			additionalActorsCombs = this.generatePossibleCombinationsOfAdditionalActorsWithBound(1);
		}
		long endTime = System.currentTimeMillis();
		System.out.println("Run time in milsec: " + (endTime - startTime));
		System.out.println("Combs with bruteForce: " + additionalActorsCombs.size());
		return additionalActorsCombs;
	}

	@SuppressWarnings("unchecked")
	public LinkedList<PModelWithAdditionalActors> newMeasureHeuristic(int boundForCheapestSolutions,
			ClusterCondition clusterCondition) throws Exception {

		long startTime = System.currentTimeMillis();

		// build a cluster of related brts
		this.buildCluster(clusterCondition);

		// compute static sphere per data object without add actors
		HashMap<BPMNDataObject, LinkedList<HashSet<?>>> staticSpherePerDataObject = this
				.computeStaticSpherePerDataObject();
		LinkedList<PModelWithAdditionalActors> pModelAddActors = null;
		if (!staticSpherePerDataObject.isEmpty()) {
			// only generate those combinations of participants that are preferred (e.g. in
			// cluster or locally cheap)
			pModelAddActors = this.generatePModelsWithAddActorsForIntersectionInCluster();

			LinkedList<PModelWithAdditionalActors> cheapestPModels = new LinkedList<PModelWithAdditionalActors>();
			// iterate over all combinations and only keep the cheapest ones
			for (PModelWithAdditionalActors pModelWithAdditionalActors : pModelAddActors) {
				pModelWithAdditionalActors.setPrivateSphere(this.privateSphere);

				for (Entry<BPMNDataObject, LinkedList<HashSet<?>>> staticSpherePerDataO : staticSpherePerDataObject
						.entrySet()) {
					BPMNDataObject currDataO = staticSpherePerDataO.getKey();
					HashSet<BPMNParticipant> staticSphereParticipants = (HashSet<BPMNParticipant>) staticSpherePerDataO
							.getValue().get(0);
					HashSet<BPMNTask> readerBrtsOfDataO = (HashSet<BPMNTask>) staticSpherePerDataO.getValue().get(1);
					Static_SphereEntry currStaticEntry = new Static_SphereEntry(currDataO, staticSphereParticipants,
							readerBrtsOfDataO);
					pModelWithAdditionalActors.getStaticSphereEntries().put(currDataO, currStaticEntry);

					HashSet<BPMNParticipant> staticSphereWithAdditionalActors = new HashSet<BPMNParticipant>();
					// add all actors of static sphere
					staticSphereWithAdditionalActors.addAll(currStaticEntry.getStaticSphere());

					for (AdditionalActors additionalActors : pModelWithAdditionalActors.getAdditionalActorsList()) {
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
					pModelWithAdditionalActors.setSumMeasure(newAlphaScoreSum);

					if (cheapestPModels.isEmpty()) {
						cheapestPModels.add(pModelWithAdditionalActors);
					} else {
						if (newAlphaScoreSum < cheapestPModels.get(0).getAlphaMeasureSum()) {
							// new cheapest model found
							cheapestPModels.clear();
							cheapestPModels.add(pModelWithAdditionalActors);
						} else if (newAlphaScoreSum == cheapestPModels.get(0).getAlphaMeasureSum()) {
							// check for bound
							// if bound <= 0 -> unbounded
							if (boundForCheapestSolutions <= 0) {
								cheapestPModels.add(pModelWithAdditionalActors);
							} else {
								if (cheapestPModels.size() < boundForCheapestSolutions) {
									cheapestPModels.add(pModelWithAdditionalActors);
								}
							}

						}
					}

				}
			}
			pModelAddActors = cheapestPModels;
		} else {
			pModelAddActors = this.generatePossibleCombinationsOfAdditionalActorsWithBound(1);
		}

		/*
		 * System.out.println("Combs with heuristics: "+pModelAddActors.size()); for
		 * (PModelWithAdditionalActors pModel : pModelAddActors) {
		 * pModel.printMeasure(); }
		 */
		long endTime = System.currentTimeMillis();
		System.out.println("Combs with heuristic: " + pModelAddActors.size());
		System.out.println("Run time heuristic in milsec: " + (endTime - startTime));

		return pModelAddActors;

	}

	public LinkedList<PModelWithAdditionalActors> generatePossibleCombinationsOfAdditionalActorsWithBound(int bound)
			throws Exception {
		LinkedList<LinkedList<Object>> combinationsPerBrt = new LinkedList<LinkedList<Object>>();

		// if bound <= 0 -> unbounded

		for (BPMNBusinessRuleTask brt : this.businessRuleTasks) {
			LinkedList<LinkedList<BPMNParticipant>> potentialAddActors = this.getPotentialAddActorsForBrt(brt);
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
			LinkedList<LinkedList<Object>> combinationsPerBrt) throws InterruptedException {
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

	private synchronized LinkedList<AdditionalActors> generateAdditionalActorsForBrtsWithConstraintsAndBound(
			BPMNBusinessRuleTask currBrt, LinkedList<BPMNParticipant> allPossibleActors,
			LinkedList<BPMNParticipant> allMandatoryActors, LinkedList<BPMNParticipant> preferredParticipants,
			int bound) throws Exception {

		BPMNExclusiveGateway bpmnEx = (BPMNExclusiveGateway) currBrt.getSuccessors().iterator().next();
		LinkedList<AdditionalActors> additionalActorsForBrt = new LinkedList<AdditionalActors>();

		int boundCombsForNotMandatoryParticipants = bpmnEx.getAmountVoters() - allMandatoryActors.size();
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
				list = Combination.getPermutations(preferredParticipants, boundCombsForNotMandatoryParticipants);
			} else {
				list.add(new LinkedList<BPMNParticipant>());
			}

		} else {
			// there are less preferred participants ( + mandatory participants) than
			// verifiers still needed
			// take all preferred participants
			list.add(preferredParticipants);

			// compute the remaining combs of participants without mandatory ones
			remaining = Combination.getPermutations(notMands,
					Math.min(notMands.size(), Math.abs(boundCombsForPreferredPartList)));
		}

		// bound??
		for (int i = 0; i < list.size(); i++) {
			LinkedList<BPMNParticipant> currList = list.get(i);
			// add the mandatory additional actors
			currList.addAll(0, allMandatoryActors);
			if (remaining.size() > 0) {
				for (int j = 0; j < remaining.size(); j++) {
					LinkedList<BPMNParticipant> currListClone = (LinkedList<BPMNParticipant>) currList.clone();
					currListClone.addAll(remaining.get(j));
					if (currListClone.size() != bpmnEx.getAmountVoters()) {
						throw new Exception(bpmnEx.getAmountVoters() + " verifiers needed for " + bpmnEx.getId()
								+ " but " + currListClone.size() + " found!");
					}
					AdditionalActors addActors = new AdditionalActors(currBrt, currListClone);
					additionalActorsForBrt.add(addActors);
				}
			} else {
				if (currList.size() != bpmnEx.getAmountVoters()) {
					throw new Exception(bpmnEx.getAmountVoters() + " verifiers needed for " + bpmnEx.getId()
							+ " but only " + currList.size() + " found!");
				}
				AdditionalActors addActors = new AdditionalActors(currBrt, currList);
				additionalActorsForBrt.add(addActors);
			}

		}

		if (list.isEmpty()) {
			throw new Exception("No possible combination of verifiers for " + currBrt.getId());
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
			throw new Exception("Error in mapAndCompute() method!" + e.getStackTrace() + e.getMessage());
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
							node.getName().trim().substring(0, node.getName().indexOf("[")));
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
							node.getName().trim().substring(0, node.getName().indexOf("[")));
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
				this.publicSphere.add(lanePart);
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

	public void computePrivateSphere() {
		for (BPMNElement e : this.processElements) {
			if (e instanceof BPMNTask) {
				if (!(this.privateSphere.contains(((BPMNTask) e).getParticipant()))) {
					this.privateSphere.add(((BPMNTask) e).getParticipant());
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

		boolean decisionTakerPartOfVerifier = true;
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

			if (partsInMandatoryConstraintsForGtw.size() > xorGtw.getAmountVoters()) {
				throw new Exception("Mandatory participants > amount voters needed at " + xorGtw.getId() + " ("
						+ partsInMandatoryConstraintsForGtw.size() + " > " + xorGtw.getAmountVoters() + ")");
			}

		}

		for (BusinessRuleTask brt : this.modelInstance.getModelElementsByType(BusinessRuleTask.class)) {
			this.mapDecisions((BPMNBusinessRuleTask) this.getBPMNNodeById(brt.getId()));
		}
	}

	private void mapAnnotations(BPMNExclusiveGateway gtw) throws Exception {
		// map the annotated amount of needed voters to the BPMNExclusiveGateways
		// map the sphere connected to the gateway

		if (gtw.getAmountVoters() > this.privateSphere.size()) {
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
								BPMNParticipant participant = this.getBPMNParticipantFromSet(this.publicSphere,
										participantName);
								if (participant != null) {
									partAlreadyMapped = true;
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
								BPMNParticipant participant = this.getBPMNParticipantFromSet(this.publicSphere,
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
										BPMNElement predecessor = gtw.getPredecessors().iterator().next();
										if (predecessor instanceof BPMNBusinessRuleTask) {
											BPMNBusinessRuleTask brt = (BPMNBusinessRuleTask) predecessor;
											if (brt.getParticipant().equals(participant)) {
												throw new Exception(
														"Mandatory participant can not be the additional actor of the brt!");
											}

										}
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

	public BPMNParticipant getBPMNParticipantFromList(LinkedList<BPMNParticipant> list, String name) {
		for (BPMNParticipant p : list) {
			if (p.getNameWithoutBrackets().contentEquals(name)) {
				return p;
			}
		}
		return null;
	}

	public BPMNParticipant getBPMNParticipantFromSet(HashSet<BPMNParticipant> set, String name) {
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

	public HashSet<BPMNParticipant> getSdActorsWithAdditionalActorsAndSetWeightingOfOrigin(BPMNDataObject dataO,
			BPMNTask origin, BPMNBusinessRuleTask currPositionBrt, LinkedList<AdditionalActors> addActors,
			LinkedList<LinkedList<BPMNElement>> paths, SD_SphereEntry sdEntry) {

		HashSet<BPMNParticipant> sdList = new HashSet<BPMNParticipant>();
		sdList.addAll(this.privateSphere);

		HashSet<BPMNParticipant> readersToBeFound = new HashSet<BPMNParticipant>();
		readersToBeFound.addAll(sdList);
		double amountPathsWhereOriginWritesForDataO = 0;

		for (LinkedList<BPMNElement> path : paths) {
			HashSet<BPMNParticipant> foundOnPath = new HashSet<BPMNParticipant>();
			for (int i = 0; i < path.size(); i++) {
				BPMNElement el = path.get(i);
				if (el instanceof BPMNTask) {
					BPMNTask currTask = (BPMNTask) el;
					if (dataO.getReaders().contains(currTask)) {
						// currTask is a reader of the dataO
						foundOnPath.add(currTask.getParticipant());
						if (currTask instanceof BPMNBusinessRuleTask) {

							for (AdditionalActors addActor : addActors) {
								BPMNBusinessRuleTask brt = addActor.getCurrBrt();
								if (brt.equals(currTask) && dataO.getReaders().contains(currTask)) {
									// currTask is a brt that reads the data object
									// add additional readers
									foundOnPath.addAll(addActor.getAdditionalActors());
								}
							}
						}
					}

					if (dataO.getWriters().contains(currTask)) {
						// currTask is a writer
						// check if is the origin
						if (currTask.equals(origin)) {
							// origin is always in SD
							sdList.add(currTask.getParticipant());
							foundOnPath.add(currTask.getParticipant());
							amountPathsWhereOriginWritesForDataO++;
						} else {
							// another writer found on some path
							// stop here
							// actors after that writer will not read dataO from origin

							// check if other writer has another actor?

							i = path.size();
						}

					}

				}

			}

			HashSet<BPMNParticipant> notFoundOnPath = new HashSet<BPMNParticipant>();
			notFoundOnPath.addAll(readersToBeFound);
			notFoundOnPath.removeAll(foundOnPath);
			sdList.removeAll(notFoundOnPath);
		}
		// set amount of paths where origin writes for dataO
		if (sdEntry != null && sdEntry.getAmountPathsWhereOriginWritesForCurrBrt() == 0) {
			amountPathsWhereOriginWritesForDataO = amountPathsWhereOriginWritesForDataO / paths.size();
			sdEntry.setWeightingOfOriginForCurrBrt(amountPathsWhereOriginWritesForDataO);
		}

		return sdList;
	}

	public LinkedList<HashSet<BPMNParticipant>> getSdActorsAndSetWeightingOfOrigin(BPMNDataObject dataO,
			BPMNTask origin, BPMNBusinessRuleTask currPositionBrt, LinkedList<AdditionalActors> addActors,
			LinkedList<AdditionalActors> addActorsWithoutCurrPositionBrt, LinkedList<LinkedList<BPMNElement>> paths,
			SD_SphereEntry sdEntry) {

		// sdActorsList(0) will be the SD() -> sd sphere with additional actors for all
		// brts
		// sdActorsList(1) will be the SD* -> sd sphere with additional actors for all
		// brts except the currentBrt
		LinkedList<HashSet<BPMNParticipant>> sdActorsList = new LinkedList<HashSet<BPMNParticipant>>();

		HashSet<BPMNParticipant> sdListAddActors = new HashSet<BPMNParticipant>();
		sdListAddActors.addAll(this.privateSphere);
		HashSet<BPMNParticipant> sdListWithoutCurrBrtAddActors = new HashSet<BPMNParticipant>();
		sdListWithoutCurrBrtAddActors.addAll(this.privateSphere);

		HashSet<BPMNParticipant> readersToBeFound = new HashSet<BPMNParticipant>();
		readersToBeFound.addAll(this.privateSphere);

		double amountPathsWhereOriginWritesForDataO = 0;

		for (LinkedList<BPMNElement> path : paths) {
			HashSet<BPMNParticipant> foundOnPathWithAddActors = new HashSet<BPMNParticipant>();
			HashSet<BPMNParticipant> foundOnPathWithoutCurrBrtAddActors = new HashSet<BPMNParticipant>();
			boolean pastCurrBrt = false;
			for (int i = 0; i < path.size(); i++) {
				BPMNElement el = path.get(i);
				if (el instanceof BPMNTask) {
					BPMNTask currTask = (BPMNTask) el;
					if (dataO.getReaders().contains(currTask)) {
						// currTask is a reader of the dataO
						foundOnPathWithAddActors.add(currTask.getParticipant());
						foundOnPathWithoutCurrBrtAddActors.add(currTask.getParticipant());
						if (currTask instanceof BPMNBusinessRuleTask) {

							for (AdditionalActors addActor : addActors) {
								BPMNBusinessRuleTask brt = addActor.getCurrBrt();
								if (brt.equals(currTask) && dataO.getReaders().contains(currTask)) {
									// currTask is a brt that reads the data object
									// add additional readers
									foundOnPathWithAddActors.addAll(addActor.getAdditionalActors());
								}
							}

							for (AdditionalActors addActorWithoutCurrBrtAddActors : addActorsWithoutCurrPositionBrt) {
								BPMNBusinessRuleTask brt = addActorWithoutCurrBrtAddActors.getCurrBrt();
								if (brt.equals(currTask) && dataO.getReaders().contains(currTask)) {
									foundOnPathWithoutCurrBrtAddActors
											.addAll(addActorWithoutCurrBrtAddActors.getAdditionalActors());
								}
							}
							
							if(currPositionBrt.equals(currTask)) {
								pastCurrBrt = true;
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
							sdListWithoutCurrBrtAddActors.add(currTask.getParticipant());
							foundOnPathWithoutCurrBrtAddActors.add(currTask.getParticipant());
							amountPathsWhereOriginWritesForDataO++;
							
						} else {
							if(!pastCurrBrt) {
								amountPathsWhereOriginWritesForDataO--;
							}
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

			HashSet<BPMNParticipant> notFoundOnPathWithoutCurrBrtAddActors = new HashSet<BPMNParticipant>();
			notFoundOnPathWithoutCurrBrtAddActors.addAll(readersToBeFound);
			notFoundOnPathWithoutCurrBrtAddActors.removeAll(foundOnPathWithoutCurrBrtAddActors);
			sdListWithoutCurrBrtAddActors.removeAll(notFoundOnPathWithoutCurrBrtAddActors);
		}
		// set amount of paths where origin writes for dataO
		if (sdEntry != null && sdEntry.getAmountPathsWhereOriginWritesForCurrBrt() == 0) {
			amountPathsWhereOriginWritesForDataO = amountPathsWhereOriginWritesForDataO / paths.size();
			sdEntry.setWeightingOfOriginForCurrBrt(amountPathsWhereOriginWritesForDataO);
		}

		sdActorsList.add(sdListAddActors);
		sdActorsList.add(sdListWithoutCurrBrtAddActors);

		return sdActorsList;
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

	public LinkedList<LinkedList<BPMNElement>> goDfs(BPMNElement currentNode, BPMNElement targetNode1,
			BPMNElement targetNode2, LinkedList<BPMNElement> stack, LinkedList<BPMNElement> path,
			LinkedList<LinkedList<BPMNElement>> paths) throws Exception {
		// route depth first through the process
		// with the help of labels we only get those paths that contain both targetNodes
		// first go dfs till targetNode1 is found
		// then go dfs till targetNode2 is found

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

	private boolean computeCurrentMeasure(String requiredSphereOfDataObject, String requiredSphereOfMeasure) {
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

	public void buildCluster(ClusterCondition clusterCondition) {
		// build a cluster of related brts
		// e.g. brt1 and brt2 are related, if they (partially) have the same origins

		for (int i = 0; i < this.businessRuleTasks.size(); i++) {
			BPMNBusinessRuleTask brt = this.businessRuleTasks.get(i);

			for (int j = this.businessRuleTasks.size() - 1; j >= 0; j--) {
				BPMNBusinessRuleTask brt2 = this.businessRuleTasks.get(j);
				if (!brt.equals(brt2)) {
					for (Entry<BPMNDataObject, ArrayList<BPMNTask>> entry : brt.getLastWriterList().entrySet()) {
						for (Entry<BPMNDataObject, ArrayList<BPMNTask>> entry2 : brt2.getLastWriterList().entrySet()) {
							if (entry2.getKey().equals(entry.getKey())) {
								if (entry.getValue().containsAll(entry2.getValue())
										&& entry2.getValue().containsAll(entry.getValue())) {
									// all origins for the data object are the same
									if (clusterCondition.equals(ClusterCondition.ONEORIGINTHESAME)) {
										this.relatedBrts.computeIfAbsent(entry.getKey(),
												k -> new HashSet<BPMNBusinessRuleTask>()).add(brt);
										this.relatedBrts.get(entry.getKey()).add(brt2);
									}
									if (clusterCondition.equals(ClusterCondition.ALLORIGINSTHESAME)) {
										this.relatedBrts.computeIfAbsent(entry.getKey(),
												k -> new HashSet<BPMNBusinessRuleTask>()).add(brt);
										this.relatedBrts.get(entry.getKey()).add(brt2);
									}
								} else {
									// check if at least one origin for the data object is the same
									ArrayList<BPMNTask> intersect = new ArrayList<BPMNTask>();
									intersect.addAll(entry.getValue());
									intersect.retainAll(entry2.getValue());
									if (!intersect.isEmpty()) {
										// at least one origin for the data object is the same
										if (clusterCondition.equals(ClusterCondition.ONEORIGINTHESAME)) {
											this.relatedBrts.computeIfAbsent(entry.getKey(),
													k -> new HashSet<BPMNBusinessRuleTask>()).add(brt);
											this.relatedBrts.get(entry.getKey()).add(brt2);
										}
									}

								}

							}
						}

					}

				}

			}

		}

	}

	public LinkedList<PModelWithAdditionalActors> generatePModelsWithAddActorsForIntersectionInCluster()
			throws Exception {

		HashMap<BPMNDataObject, HashSet<BPMNParticipant>> addActorsIntersectPerDataO = new HashMap<BPMNDataObject, HashSet<BPMNParticipant>>();
		HashSet<BPMNParticipant> intersectionOverDataObjects = new HashSet<BPMNParticipant>();
		HashMap<BPMNBusinessRuleTask, LinkedList<LinkedList<BPMNParticipant>>> possiblePartMap = new HashMap<BPMNBusinessRuleTask, LinkedList<LinkedList<BPMNParticipant>>>();

		// get the intersection of additional actors of related brts per data object
		boolean initializedIntersectOverDataO = false;
		for (Entry<BPMNDataObject, HashSet<BPMNBusinessRuleTask>> relatedBrtEntry : this.relatedBrts.entrySet()) {
			HashSet<BPMNParticipant> additionalActorsIntersectForDataO = new HashSet<BPMNParticipant>();
			boolean initializedParticipantMap = false;
			for (BPMNBusinessRuleTask relatedBrt : relatedBrtEntry.getValue()) {
				LinkedList<LinkedList<BPMNParticipant>> participantsForBrt = this
						.getPotentialAddActorsForBrt(relatedBrt);
				LinkedList<BPMNParticipant> participantsPossibleForBrt = participantsForBrt.get(0);
				HashSet<BPMNParticipant> participantsWithAccessToData = new HashSet<BPMNParticipant>(
						participantsPossibleForBrt);
				// also add the actor of the brt itself
				// since it may be a good choice for other brts
				participantsWithAccessToData.add(relatedBrt.getParticipant());

				possiblePartMap.putIfAbsent(relatedBrt, participantsForBrt);

				if (!initializedParticipantMap) {
					additionalActorsIntersectForDataO.addAll(participantsWithAccessToData);
					initializedParticipantMap = true;
				}

				additionalActorsIntersectForDataO.retainAll(participantsWithAccessToData);
			}

			// add the intersected possible additional actors to the data object
			addActorsIntersectPerDataO.putIfAbsent(relatedBrtEntry.getKey(), additionalActorsIntersectForDataO);

			if (!initializedIntersectOverDataO) {
				intersectionOverDataObjects.addAll(additionalActorsIntersectForDataO);
				initializedIntersectOverDataO = true;
			}
			intersectionOverDataObjects.retainAll(additionalActorsIntersectForDataO);
		}

		LinkedList<LinkedList<Object>> combinationsPerBrt = new LinkedList<LinkedList<Object>>();
		for (BPMNBusinessRuleTask brt : this.businessRuleTasks) {
			LinkedList<LinkedList<BPMNParticipant>> addActorsList = possiblePartMap.get(brt);
			if (addActorsList == null) {
				// brt is not in cluster
				addActorsList = this.getPotentialAddActorsForBrt(brt);
			}

			LinkedList<BPMNParticipant> intersected = new LinkedList<BPMNParticipant>(intersectionOverDataObjects);

			// remove the actor of the brt itself from the intersected ones
			// he can not be an additional actor too
			intersected.remove(brt.getParticipant());

			// if intersected is empty
			// compute the local cheapest add actors
			// else compute the cheapest within intersection
			
			

			LinkedList<AdditionalActors> addActorsForCurrBrt = this
					.generateAdditionalActorsForBrtsWithConstraintsAndBound(brt, addActorsList.get(0),
							addActorsList.get(1), intersected, 0);

			LinkedList<Object> combsForCurrBrt = new LinkedList<Object>(addActorsForCurrBrt);
			combinationsPerBrt.add(combsForCurrBrt);

		}

		return this.generateAllPossiblePModelWithAdditionalActors(combinationsPerBrt);

	}

	public LinkedList<LinkedList<BPMNParticipant>> getPotentialAddActorsForBrt(BPMNBusinessRuleTask brt) {

		LinkedList<LinkedList<BPMNParticipant>> setOfParticipants = new LinkedList<LinkedList<BPMNParticipant>>();
		// on index 0 -> all participantsToCombineAsAdditionalActors
		// on index 1 -> all mandatoryParticipants

		BPMNParticipant partOfBrt = brt.getParticipant();
		BPMNExclusiveGateway bpmnEx = (BPMNExclusiveGateway) brt.getSuccessors().iterator().next();
		// get all the possible combinations of participants for the brt
		// consider the constraints e.g. participants may be excluded or mandatory
		LinkedList<BPMNParticipant> participantsToCombineAsAdditionalActors = new LinkedList<BPMNParticipant>();

		// add all participants except the actor of the brt itself
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

		setOfParticipants.add(participantsToCombineAsAdditionalActors);
		setOfParticipants.add(mandatoryParticipantsAsAdditionalActors);

		return setOfParticipants;
	}

	public HashMap<BPMNDataObject, LinkedList<HashSet<?>>> computeStaticSpherePerDataObject() {
		// compute static sphere per data object without additional actors
		HashMap<BPMNDataObject, LinkedList<HashSet<?>>> mapToReturn = new HashMap<BPMNDataObject, LinkedList<HashSet<?>>>();

		for (BPMNDataObject dataO : this.dataObjects) {
			LinkedList<HashSet<?>> list = new LinkedList<HashSet<?>>();
			// list on index 0 -> set of participants in static sphere
			// list on index 1 -> set of brts that read dataO
			String sphereOfDataO = dataO.getDefaultSphere();
			boolean computeAlphaMeasure = this.computeCurrentMeasure(sphereOfDataO, this.staticSphereKey);

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
				boolean computeBetaMeasure = this.computeCurrentMeasure(currDataObjectSphere,
						this.weakDynamicSphereKey);
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

								// on index 0 -> HashSet only containing the origin
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

	public HashMap<BPMNDataObject, HashSet<BPMNBusinessRuleTask>> getRelatedBrts() {
		return relatedBrts;
	}

	public void setRelatedBrts(HashMap<BPMNDataObject, HashSet<BPMNBusinessRuleTask>> relatedBrts) {
		this.relatedBrts = relatedBrts;
	}

	public synchronized HashMap<String, LinkedList<PModelWithAdditionalActors>> call()
			throws NullPointerException, InterruptedException, Exception {
		// TODO Auto-generated method stub
		System.out.println("Call algorithm for: " + this.processModelFile.getAbsolutePath());

		HashMap<String, LinkedList<PModelWithAdditionalActors>> mapToReturn = new HashMap<String, LinkedList<PModelWithAdditionalActors>>();
		if (this.algorithmToPerform.equals(Enums.AlgorithmToPerform.EXHAUSTIVE.name())) {
			LinkedList<PModelWithAdditionalActors> pInstBruteForce = this.exhaustiveSearch();
			mapToReturn.putIfAbsent(this.algorithmToPerform, pInstBruteForce);

		} else if (this.algorithmToPerform.equals(Enums.AlgorithmToPerform.HEURISTIC.name())) {
			LinkedList<PModelWithAdditionalActors> pInstHeuristic = this.newMeasureHeuristic(0,
					Enums.ClusterCondition.valueOf(this.clusterCondition));
			mapToReturn.putIfAbsent(this.algorithmToPerform, pInstHeuristic);

		} else if (this.algorithmToPerform.contains(Enums.AlgorithmToPerform.HEURISTICWITHBOUND.name())) {
			String limit = Enums.AlgorithmToPerform.HEURISTICWITHBOUND.name();
			int limitPerIteration = Integer.parseInt(limit.replaceAll("\\D+", "").trim());
			LinkedList<PModelWithAdditionalActors> pInstHeuristicWithBound = this.newMeasureHeuristic(limitPerIteration,
					Enums.ClusterCondition.valueOf(this.clusterCondition));
			mapToReturn.putIfAbsent(this.algorithmToPerform, pInstHeuristicWithBound);
		}

		return mapToReturn;

	}

	public void setAlgorithmToPerform(Enums.AlgorithmToPerform algToPerform, int bound) {
		String alg = algToPerform.name();
		if (algToPerform.equals(Enums.AlgorithmToPerform.HEURISTICWITHBOUND)) {
			alg += bound;
		}
		this.algorithmToPerform = alg;
	}

	public void setClusterCondition(Enums.ClusterCondition clusterCond) {
		this.clusterCondition = clusterCond.name();
	}

}
