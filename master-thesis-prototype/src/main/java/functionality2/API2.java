package functionality2;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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

public class API2 {

	private BpmnModelInstance modelInstance;
	private File processModelFile;
	private HashSet<BPMNParticipant> privateSphere;
	private HashSet<BPMNParticipant> publicSphere;
	private String algorithmToPerform;
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
	private LinkedList<LinkedList<FlowNode>>pathsThroughProcess;

	public API2(String pathToBpmnCamundaFile, LinkedList<Double> cost) throws Exception {
		if (cost.size() != 4) {
			throw new Exception("Not exactly 4 cost parameters in the list!");
		}
		this.processModelFile = new File(pathToBpmnCamundaFile);
		//preprocess the model, i.e. remove parallel branches
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
		this.algorithmToPerform = "bruteForce";
		this.amountPossibleCombinationsOfParticipants = "0";
		this.mandatoryParticipantConstraints = new LinkedList<MandatoryParticipantConstraint>();
		this.excludeParticipantConstraints = new LinkedList<ExcludeParticipantConstraint>();
		this.modelWithLanes = false;
		this.businessRuleTasks = new LinkedList<BPMNBusinessRuleTask>();
		//map all the elements from camunda
		this.mapCamundaElements();
		this.pathsThroughProcess = CommonFunctionality.getAllPathsBetweenNodes(this.modelInstance,
				this.startEvent.getId(), this.endEvent.getId());
	}

	public void newMeasureAlgorithm() throws InterruptedException {
		// generate all possible combinations of additional readers
		LinkedList<PModelWithAdditionalActors> additionalActorsCombs = this.generateAllPossibleCombinationsOfAdditionalActors();
		System.out.println(additionalActorsCombs.size());
		
		// compute alpha measure with and without additional actors
		// compute static sphere of process (without additional actors)
		HashMap<BPMNDataObject, HashSet<BPMNParticipant>>staticSpherePerDataObject = new HashMap<BPMNDataObject, HashSet<BPMNParticipant>>();
		for(BPMNDataObject dataO: this.dataObjects) {
			staticSpherePerDataObject.putIfAbsent(dataO, new HashSet<BPMNParticipant>(dataO.getStaticSphere()));			
		}
		
		for(PModelWithAdditionalActors pModelWithAdditionalActors: additionalActorsCombs) {
			pModelWithAdditionalActors.setPrivateSphere(this.privateSphere);
			pModelWithAdditionalActors.setStaticSphere(staticSpherePerDataObject);
			pModelWithAdditionalActors.computeStaticSphereWithAdditionalActors();
			pModelWithAdditionalActors.computeAlphaMeasure();
			System.out.println(pModelWithAdditionalActors.getAlphaMeasurePenalty());
			pModelWithAdditionalActors.printAlphaMeasure();
		}
		
		BPMNElement currentNode = this.getBPMNNodeById("Task_0zwm6xe");
		BPMNElement targetNode = this.getBPMNNodeById("Task_1fefoqy");
		try {
			LinkedList<LinkedList<BPMNElement>>paths = this.goDfs(currentNode, targetNode, new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(), new LinkedList<LinkedList<BPMNElement>>());
			System.out.println(paths.size());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// compute beta measure
		// i.e. query brts in any order and get origins for each data object
		// compute WD sphere at the position of the origin without additional actors
		// weight(w) is the depth of the origin
		
		
		// for each PModelWithAdditionalActors
			// for each origin of each data object of each brt 
				// compute SD sphere and SD* sphere at the position of the current brt
				// SD is the strong-dynamic sphere with additional actors 
				// SD* is the strong-dynamic sphere without additional actors of the current brt
				// weight(w) is the nesting depth of the origin
				// weight(w,r,d) is the fraction of instance types in which r reads data object d from origin w
				
				
	}


	public LinkedList<PModelWithAdditionalActors> generateAllPossibleCombinationsOfAdditionalActors()
			throws InterruptedException {
		LinkedList<LinkedList<Object>> combinationsPerBrt = new LinkedList<LinkedList<Object>>();
		LinkedList<PModelWithAdditionalActors> pModelWithAddActorsList = new LinkedList<PModelWithAdditionalActors>();

		for (BPMNBusinessRuleTask brt : this.businessRuleTasks) {
			try {
				LinkedList<AdditionalActors> addActorsCombinationsForBrt = this.generateAdditionalActorsForBrtsWithConstraints(brt);
				LinkedList<Object> addActors = new LinkedList<Object>();
				addActors.addAll(addActorsCombinationsForBrt);
				combinationsPerBrt.add(addActors);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		// generate all possible combinations of additional actors for brts
		Collection<List<Object>> combs = Combination.permutations(combinationsPerBrt);
		// ProcessInstanceWithVoters.setProcessID(0);
		for (List list : combs) {
			if (Thread.currentThread().isInterrupted()) {
				System.err.println("Interrupted" + Thread.currentThread().getName());
				throw new InterruptedException();
			}
			LinkedList<AdditionalActors> additionalActorsList = new LinkedList<AdditionalActors>(list);
			PModelWithAdditionalActors newModel = new PModelWithAdditionalActors(additionalActorsList);
			pModelWithAddActorsList.add(newModel);
		}
		return pModelWithAddActorsList;
	}

	private synchronized LinkedList<AdditionalActors> generateAdditionalActorsForBrtsWithConstraints(
			BPMNBusinessRuleTask currBrt) throws Exception {
		LinkedList<AdditionalActors> additionalActorsForBrt = new LinkedList<AdditionalActors>();

		BPMNExclusiveGateway bpmnEx = (BPMNExclusiveGateway) currBrt.getSuccessors().iterator().next();
		// get all the possible combinations of participants for the brt
		// consider the constraints e.g. participants may be excluded or mandatory
		LinkedList<BPMNParticipant> participantsToCombineAsAdditionalActors = new LinkedList<BPMNParticipant>();

		// add all participants except the actor of the brt itself
		participantsToCombineAsAdditionalActors.addAll(this.privateSphere);
		participantsToCombineAsAdditionalActors.remove(currBrt.getParticipant());

		LinkedList<BPMNParticipant> mandatoryParticipantsAsAdditionalActors = new LinkedList<BPMNParticipant>();

		for (Constraint constraint : bpmnEx.getConstraints()) {
			if (constraint instanceof ExcludeParticipantConstraint) {
				// exclude the participants due to constraints
				BPMNParticipant partToRemove = ((ExcludeParticipantConstraint) constraint).getParticipantToExclude();
				participantsToCombineAsAdditionalActors.remove(partToRemove);
			} else if (constraint instanceof MandatoryParticipantConstraint) {
				BPMNParticipant mandatoryPart = ((MandatoryParticipantConstraint) constraint).getMandatoryParticipant();
				if (!mandatoryParticipantsAsAdditionalActors.contains(mandatoryPart)) {
					mandatoryParticipantsAsAdditionalActors.add(mandatoryPart);
				}
			}
		}

		LinkedList<LinkedList<BPMNParticipant>> list = Combination
				.getPermutations(participantsToCombineAsAdditionalActors, bpmnEx.getAmountVoters());
		Iterator<LinkedList<BPMNParticipant>> listIter = list.iterator();
		while (listIter.hasNext()) {
			LinkedList<BPMNParticipant> partList = listIter.next();
			if (!partList.containsAll(mandatoryParticipantsAsAdditionalActors)) {
				listIter.remove();
			} else {
				AdditionalActors addActors = new AdditionalActors(currBrt, partList);
				additionalActorsForBrt.add(addActors);
			}
		}
		if (list.isEmpty()) {
			throw new Exception("No possible combination of voters for " + currBrt.getId());
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

			this.mapDataObjects();
			this.computePrivateSphere();
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
			throw new Exception("Error in mapAndCompute() method!" + e.getStackTrace() + e.getCause());
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
		// to the
		// nodes

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
	
	
	public LinkedList<LinkedList<BPMNElement>> goDfs(BPMNElement currentNode,
			BPMNElement targetNode, LinkedList<BPMNElement> stack, LinkedList<BPMNElement> path,
			LinkedList<LinkedList<BPMNElement>> paths) throws Exception {
		// route depth first through the process 
		// with the help of labels we only go in those paths that contain the targetNode
		if(path.isEmpty()) {
			path.add(currentNode);
		}
		
		if (!currentNode.getId().contentEquals(targetNode.getId())) {			
			if(currentNode instanceof BPMNExclusiveGateway) {
				BPMNExclusiveGateway gtw = (BPMNExclusiveGateway)currentNode;
				if(gtw.getType().contentEquals("split")) {
					// only add those successors, that lead to the targetNode
					ArrayList<Label>targetNodeLabels = targetNode.getLabels();
					for(BPMNElement successor: gtw.getSuccessors()) {
						ArrayList<Label>successorLabels = successor.getLabels();
						if(targetNodeLabels.isEmpty() || targetNodeLabels.containsAll(successorLabels)){
							stack.add(successor);							
						} else {
							// check if they do not contain a common label
							boolean different = true;
							Iterator<Label>labelIter = targetNodeLabels.iterator();
							while(labelIter.hasNext() && different) {
								Label targetNodeLabel = labelIter.next();
								if(successorLabels.contains(targetNodeLabel)) {
									different = false;
								}
							}
							if(different) {
								stack.add(successor);
							}
							
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
			if (nextNode.getId().contentEquals(targetNode.getId())) {
				paths.add(newPath);
			} else {
				goDfs(nextNode, targetNode, new LinkedList<BPMNElement>(), newPath, paths);
			}
		}

		return paths;

	}
	
	


}
