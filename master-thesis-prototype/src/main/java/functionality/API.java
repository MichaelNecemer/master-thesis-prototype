package functionality;

import java.io.File;
import java.io.IOException;
import java.security.Provider.Service;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;

import org.camunda.bpm.model.bpmn.builder.*;
import org.camunda.bpm.model.bpmn.builder.ProcessBuilder;
import org.camunda.bpm.model.bpmn.impl.instance.FlowNodeRef;
import org.apache.ibatis.javassist.compiler.ast.Variable;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.Query;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnPlane;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaFormData;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.camunda.bpm.model.bpmn.instance.dc.Bounds;
import org.camunda.bpm.model.bpmn.instance.di.Plane;
import org.camunda.bpm.model.bpmn.instance.di.Shape;
import org.camunda.bpm.model.bpmn.instance.di.Waypoint;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.type.ModelElementType;

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
import Mapping.DecisionEvaluation;
import Mapping.InfixToPostfix;
import Mapping.Label;

//Class that uses the camunda model API to interact with the process model directly without parsing the XML first to e.g. DOM Object
//Note that only processes with exactly 1 Start Event are possible
public class API {

	private Collection<StartEvent> startEvent;
	private File process;
	private BpmnModelInstance modelInstance;

	private BPMNStartEvent bpmnStart;
	private BPMNEndEvent bpmnEnd;
	private ArrayList<BPMNDataObject> dataObjects = new ArrayList<BPMNDataObject>();
	private ArrayList<BPMNElement> processElements = new ArrayList<BPMNElement>();
	private ArrayList<BPMNParticipant> globalSphere = new ArrayList<BPMNParticipant>();
	private ArrayList<ArrayList<BPMNParticipant>> staticSphere = new ArrayList<ArrayList<BPMNParticipant>>();

	private ArrayList<BPMNBusinessRuleTask> businessRuleTaskList = new ArrayList<BPMNBusinessRuleTask>();
	private ArrayList<Label> labelList = new ArrayList<Label>();

	API(String pathToFile) {
		process = new File(pathToFile);
		modelInstance = Bpmn.readModelFromFile(process);
		startEvent = modelInstance.getModelElementsByType(StartEvent.class);
		this.mapAndCompute();
	}

	private void mapAndCompute() {
		this.setSuccessors();
		this.setPredecessors();
		this.storeLanePerTask();
		this.mapDataObjects();
		this.createDataObjectAsssociatons();
		this.computeGlobalSphere();
		this.computeStaticSphere();
		this.getAllProcessPaths();
		for (BPMNElement element : this.processElements) {
			if (element instanceof BPMNBusinessRuleTask) {
				this.addVotingSystem(element);
			}
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
				mappedNode = new BPMNBusinessRuleTask(node.getId(), node.getName());
			} else {
				mappedNode = new BPMNTask(node.getId(), node.getName());
			}

		} else if (node instanceof ParallelGateway) {
			if (node.getOutgoing().size() == 1) {
				mappedNode = new BPMNParallelGateway(node.getId(), node.getName(), "join");
			} else {
				mappedNode = new BPMNParallelGateway(node.getId(), node.getName(), "split");

			}
		} else if (node instanceof ExclusiveGateway) {
			if (node.getOutgoing().size() == 1) {
				mappedNode = new BPMNExclusiveGateway(node.getId(), node.getName(), "join");
			} else {
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

	public void setSuccessors() {
		try {
			Collection<FlowNode> camundaFlowNodes = modelInstance.getModelElementsByType(FlowNode.class);

			BPMNElement currentNode = null;

			for (FlowNode flow : camundaFlowNodes) {
				currentNode = mapCamundaFlowNodes(flow);

				BPMNElement everyN = null;
				for (FlowNode everyNode : getSucceedingFlowNodes(flow)) {

					everyN = mapCamundaFlowNodes(everyNode);

					// Create labels for the outgoing flows of Exclusive Gateways and add them to
					// directly following Elements
					for (SequenceFlow seq : flow.getOutgoing()) {
						if (currentNode instanceof BPMNExclusiveGateway && flow.getOutgoing().size() >= 2) {
							Label label = new Label(((BPMNExclusiveGateway) currentNode).getName(), seq.getName());
							for (Label l : labelList) {
								if (l.getLabel().equals(label.getLabel())) {
									label = l;
								}
							}
							if (seq.getTarget().equals(everyNode)) {
								labelList.add(label);

								everyN.addLabel(label);

							}

						}
					}

					currentNode.setSuccessors(everyN);

				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Query CAMUNDA FlowNodes and map them to BPMNDataElements
	// Set the Predecessors for each BPMNDataElement

	public void setPredecessors() {
		Collection<FlowNode> camundaFlowNodes = modelInstance.getModelElementsByType(FlowNode.class);
		BPMNElement currentNode = null;

		// For each Camunda FlowNode check the type and create the matching BPMNElement
		for (FlowNode flow : camundaFlowNodes) {
			currentNode = mapCamundaFlowNodes(flow);

			BPMNElement everyN = null;
			// For each FlowNode get the preceeding Flow Nodes and create the matching
			// BPMNElements
			for (FlowNode everyNode : getPreceedingFlowNodes(flow)) {
				everyN = this.mapCamundaFlowNodes(everyNode);

				currentNode.setPredecessors(everyN);
			}

		}
	}

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

	// Map the Camunda Data Objects to BPMNDataObjects
	public void mapDataObjects() {
		for (DataObjectReference d : modelInstance.getModelElementsByType(DataObjectReference.class)) {
			this.dataObjects.add(new BPMNDataObject(d.getAttributeValue("dataObjectRef"), d.getName(), d.getId()));
		}
	}

	private void mapDecisions(BPMNBusinessRuleTask bpmnBrt) {		
		for (TextAnnotation text : modelInstance.getModelElementsByType(TextAnnotation.class)) {
			for (Association a : modelInstance.getModelElementsByType(Association.class)) {
				if (a.getAttributeValue("sourceRef").equals(bpmnBrt.getId())
						&& a.getAttributeValue("targetRef").equals(text.getId())) {
					if (text.getTextContent().startsWith("[Decision]") && bpmnBrt.getDecisionEvaluation() == null) {
						String dec = text.getTextContent();
						Pattern pattern = Pattern.compile("(D\\d*)\\.(\\w*)");						
						Matcher matcher = pattern.matcher(dec);							
						// check if fields needed for decision making are in the element documentation
						// of the data object!
						// e.g. D1.someVar means that there needs to be a variable called someVar in the
						// DataObject D1
						// if not, than insert these fields into the DataObject
						
						while(matcher.find()) {		
							for (BPMNDataObject dataO : this.dataObjects) {
								if (dataO.getNameIdWithoutBrackets().equals(matcher.group(1))) {
									DataObject dao = modelInstance.getModelElementById(dataO.getId());
									for (DataObjectReference daoR : modelInstance
											.getModelElementsByType(DataObjectReference.class)) {
										if (daoR.getDataObject().equals(dao)) {
											ExtensionElements extensionElements = daoR.getExtensionElements();
											if (extensionElements==null) {
											extensionElements = modelInstance.newInstance(ExtensionElements.class);											
											daoR.setExtensionElements(extensionElements);													
											} 
											CamundaProperty camundaProperty = null;
											if(extensionElements.getElements().isEmpty()) {
											CamundaProperties camundaProperties = extensionElements.addExtensionElement(CamundaProperties.class);											
											camundaProperty = modelInstance.newInstance(CamundaProperty.class);
											camundaProperty.setCamundaName(matcher.group(2));
											camundaProperty.setCamundaValue("12");
											camundaProperties.addChildElement(camundaProperty);
											} else {
												CamundaProperties cmd = extensionElements.getElementsQuery().filterByType(CamundaProperties.class).singleResult();
												camundaProperty = modelInstance.newInstance(CamundaProperty.class);
												camundaProperty.setCamundaName(matcher.group(2));
												camundaProperty.setCamundaValue("12");
												boolean insert = true;
												for(CamundaProperty cp: cmd.getCamundaProperties()) {
												if(camundaProperty.getCamundaName().equals(cp.getCamundaName())) {
													insert = false;
												}
												}
												if(insert==true) {
												cmd.addChildElement(camundaProperty);
												}
											}
											
									
										}
									}

								}
							
							
							
							
							
							
							
							
							
						}
						
						
						}

						//Whole decision is inside {} in the model
						String decisionExpression = dec.substring(dec.indexOf('{')+1, dec.indexOf('}'));
						
						DecisionEvaluation decEval = new DecisionEvaluation();
						//We get back the mapped Expression String
						String mappedExpression = InfixToPostfix.mapDecision(decisionExpression, decEval);
						//Now we convert the Expression to Postfix format
						String postfix = InfixToPostfix.convertInfixToPostfix(mappedExpression);
						decEval.setDecisionExpressionPostfix(postfix);
						bpmnBrt.setDecisionEvaluation(decEval);
						
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
				dataObject = str.substring(str.indexOf('['), str.indexOf(']') + 1);
				defaultSphere = str.substring(str.indexOf('{'), str.indexOf('}') + 1);
			}
			for (Association a : modelInstance.getModelElementsByType(Association.class)) {
				for (BPMNDataObject bpmndo : element.getDataObjects()) {
					// Map the default Spheres for the data objects to the corresponding writing
					// tasks
					if (bpmndo.getName().substring(0, 4).equals(dataObject) && bpmndo.getWriters().contains(element)) {
						// attach the defaultSphere to the dataObject
						if (bpmndo.getDefaultSphere().isEmpty()) {
							bpmndo.setDefaultSphere(defaultSphere);
						}
						if (element.getSphereAnnotation().isEmpty()) {
							element.getSphereAnnotation().put(bpmndo, defaultSphere);
						}
					}

					if (a.getAttributeValue("sourceRef").equals(element.getId())
							&& a.getAttributeValue("targetRef").equals(text.getId())) {
						String str = text.getTextContent();

						// First 4 characters specify the Data object e.g. [D1]
						String s = str.substring(str.indexOf('['), str.indexOf(']') + 1);
						// The Sphere for the data object is between {}
						String s2 = str.substring(str.indexOf('{'), str.indexOf('}') + 1);

						// Check if it is the right data object
						if (bpmndo.getName().substring(0, 4).equals(s)) {
							element.getSphereAnnotation().put(bpmndo, s2);
						}

					}
				}
			}

		}

	}

	// map the annotated amount of needed voters to the BPMNExclusiveGateways
	private void mapSphereAnnotations(BPMNExclusiveGateway gtw) {
		for (TextAnnotation text : modelInstance.getModelElementsByType(TextAnnotation.class)) {
			for (Association a : modelInstance.getModelElementsByType(Association.class)) {
				if (a.getAttributeValue("sourceRef").equals(gtw.getId())
						&& a.getAttributeValue("targetRef").equals(text.getId())) {
					String str = text.getTextContent();

					String amountVoters = str.substring(str.indexOf('{') + 1, str.indexOf('}'));
					gtw.setAmountVoters(Integer.parseInt(amountVoters));

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
								((BPMNTask) e).addBPMNDataObject(bpmno);
							}
						}
					}
				}
			}
			for (ExclusiveGateway xor : this.modelInstance.getModelElementsByType(ExclusiveGateway.class)) {
				this.mapSphereAnnotations((BPMNExclusiveGateway) this.getNodeById(xor.getId()));
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

	public ArrayList<BPMNParticipant> getGlobalSphereList() {
		return this.globalSphere;
	}

	public void computeStaticSphere() {
		for (BPMNDataObject d : this.dataObjects) {
			this.staticSphere.add(readersAndWritersForDataObject(d));
		}
	}

	private ArrayList<BPMNParticipant> readersAndWritersForDataObject(BPMNDataObject data) {
		ArrayList<BPMNParticipant> readersAndWriters = new ArrayList<BPMNParticipant>();
		for (BPMNElement t : data.getReaders()) {
			if (t instanceof BPMNTask) {
				if (!(readersAndWriters.contains(((BPMNTask) t).getParticipant()))) {
					readersAndWriters.add(((BPMNTask) t).getParticipant());
				}
			}
		}
		for (BPMNElement t : data.getWriters()) {
			if (t instanceof BPMNTask) {

				if (!(readersAndWriters.contains(((BPMNTask) t).getParticipant()))) {
					readersAndWriters.add(((BPMNTask) t).getParticipant());
				}

			}
		}
		return readersAndWriters;
	}

	public void printStaticSphere() {
		for (BPMNDataObject d : this.dataObjects) {
			System.out.println("Static sphere for " + d.getName() + ":");
			readersAndWritersForDataObject(d).forEach(r -> {
				System.out.println(r.getName());
			});
		}

	}

	// Uses Breadth first search to go through the predecessors of a node to find
	// the last Writer to that dataObject
	public BPMNTask getLastWriterForDataObject(BPMNElement currentNode, BPMNDataObject dataObject,
			BPMNElement alreadyFound) {
		LinkedList<BPMNElement> queue = new LinkedList<BPMNElement>();

		queue.addAll(currentNode.getPredecessors());
		queue.remove(alreadyFound);
		while (!(queue.isEmpty())) {
			BPMNElement element = queue.poll();
			if (element.equals(alreadyFound)) {
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
			}
		}
		return null;

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

	/*
	 * public LinkedList<LinkedList<BPMNElement>> allPathsBetweenNodes(BPMNElement
	 * startNode, BPMNElement endNode) { LinkedList<BPMNElement> stack = new
	 * LinkedList<BPMNElement>(); LinkedList<BPMNElement> gtwStack = new
	 * LinkedList<BPMNElement>(); LinkedList<BPMNElement> path = new
	 * LinkedList<BPMNElement>(); LinkedList<LinkedList<BPMNElement>>paths = new
	 * LinkedList<LinkedList<BPMNElement>>();
	 * 
	 * stack.add(startNode); boolean reachedEndGateway=false;
	 * 
	 * while(!(stack.isEmpty())) { BPMNElement element = stack.pollLast();
	 * path.add(element);
	 * 
	 * 
	 * if(element.getId().equals(endNode.getId())) { paths.add(path); element =
	 * stack.pollLast(); if(element == null) { return paths; } }
	 * 
	 * 
	 * if(element instanceof BPMNParallelGateway &&
	 * ((BPMNParallelGateway)element).getType().equals("split")) { for(BPMNElement
	 * successor: element.getSuccessors()) { gtwStack.add(successor); } }
	 * 
	 * if(element instanceof BPMNParallelGateway &&
	 * ((BPMNParallelGateway)element).getType().equals("join")) {
	 * gtwStack.pollLast(); if(!gtwStack.isEmpty()) { reachedEndGateway=true; } }
	 * 
	 * 
	 * for(BPMNElement successor: element.getSuccessors()) {
	 * 
	 * if(reachedEndGateway==false) { stack.add(successor); }
	 * 
	 * 
	 * if(successor.equals(endNode)) { //one path has been found paths.add(path); }
	 * 
	 * } reachedEndGateway = false; }
	 * 
	 * 
	 * return paths;
	 * 
	 * }
	 */
	public LinkedList<LinkedList<BPMNElement>> allPathsBetweenNodes(BPMNElement startNode, BPMNElement endNode,
			LinkedList<BPMNElement> stack, LinkedList<BPMNElement> gtwStack, LinkedList<BPMNElement> currentPath,
			LinkedList<LinkedList<BPMNElement>> paths) {

		stack.add(startNode);
		boolean reachedEndGateway = false;

		while (!(stack.isEmpty())) {
			BPMNElement element = stack.pollLast();
			currentPath.add(element);

			if (element.getId().equals(endNode.getId())) {
				paths.add(currentPath);
				element = stack.pollLast();
				if (element == null) {
					return paths;
				}
			}

			if (element instanceof BPMNParallelGateway && ((BPMNParallelGateway) element).getType().equals("split")) {
				for (BPMNElement successor : element.getSuccessors()) {
					gtwStack.add(successor);
				}
			}

			if (element instanceof BPMNParallelGateway && ((BPMNParallelGateway) element).getType().equals("join")) {
				gtwStack.pollLast();
				if (!gtwStack.isEmpty()) {
					reachedEndGateway = true;
				}
			}

			for (BPMNElement successor : element.getSuccessors()) {
				if (element.hasLabel() && successor.getLabelHasBeenSet() == false) {

					successor.addLabelFirst(element.getLabels());
					if (element instanceof BPMNExclusiveGateway
							&& ((BPMNExclusiveGateway) element).getType().equals("join")) {

						successor.deleteLastLabel();
						element.deleteLastLabel();
					}
					successor.setLabelHasBeenSet(true);
				}

				if (element instanceof BPMNExclusiveGateway
						&& ((BPMNExclusiveGateway) element).getType().equals("split")) {
					LinkedList<BPMNElement> newPath = new LinkedList<BPMNElement>();
					newPath.addAll(currentPath);

					this.allPathsBetweenNodes(successor, endNode, stack, gtwStack, newPath, paths);
				} else {
					if (reachedEndGateway == false) {
						stack.add(successor);
					}
				}

			}
			reachedEndGateway = false;
		}

		return paths;

	}

	// Test Method for pathing through the process
	public void getAllProcessPaths() {
		LinkedList<LinkedList<BPMNElement>> paths = this.allPathsBetweenNodes(this.bpmnStart, this.bpmnEnd,
				new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(),
				new LinkedList<LinkedList<BPMNElement>>());
		int i = 1;

		for (LinkedList<BPMNElement> pathElement : paths) {
			System.out.println("Liste: " + i);
			for (BPMNElement el : pathElement) {
				el.printElement();
				// pathElement.printLabels();
			}
			i++;
		}

		// this.getLastWriterForDataObject(this.getNodeById("Task_1imtmva"),
		// ((BPMNTask)this.getNodeById("Task_1imtmva")).getDataObjects().iterator().next()).printElement();

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

	public void addVotingTasksToProcess(
			HashMap<BPMNBusinessRuleTask, HashMap<BPMNDataObject, ArrayList<BPMNTask>>> votersMap, boolean mapModelBtn)
			throws IOException {

		for (BPMNBusinessRuleTask bpmnBrt : votersMap.keySet()) {
			//System.out.println("DECISION" + bpmnBrt.getDecisionEvaluation().getDecisionExpression());
			BusinessRuleTask businessRt = (BusinessRuleTask) this.getFlowNodeByBPMNNodeId(bpmnBrt.getId());
			HashMap<BPMNDataObject, ArrayList<BPMNTask>> votersMapInner = votersMap.get(bpmnBrt);

			// Voting system inside of a subprocess
			if (!mapModelBtn) {
				this.addTasksToVotingSystem(BPMNParallelGateway.increaseVotingTaskCount(), businessRt,
						this.getFlowNodeByBPMNNodeId(bpmnBrt.getPredecessors().iterator().next().getId()).builder()
								.subProcess().embeddedSubProcess().startEvent(),
						"PV" + BPMNParallelGateway.getVotingTaskCount(), votersMapInner,
						"PV" + BPMNParallelGateway.getVotingTaskCount(), mapModelBtn);
			} else {
				// Voting without having a subprocess
				this.addTasksToVotingSystem(BPMNParallelGateway.increaseVotingTaskCount(), businessRt,
						businessRt.builder().moveToNode(bpmnBrt.getPredecessors().iterator().next().getId()),
						"PV" + BPMNParallelGateway.getVotingTaskCount(), votersMapInner,
						"PV" + BPMNParallelGateway.getVotingTaskCount(), mapModelBtn);
			}

			// Add the new tasks generated via fluent builder API to the corresponding lanes
			// in the xml model
			// Cant be done with the fluent model builder directly!
			for (Lane l : modelInstance.getModelElementsByType(Lane.class)) {
				for (Task task : modelInstance.getModelElementsByType(Task.class)) {
					if (l.getName().equals(
							task.getName().substring(task.getName().indexOf(" ") + 1, task.getName().length()))) {
						// Add necessary information to the voting tasks 
						if (mapModelBtn && task.getDocumentations().isEmpty()) {
							Documentation doc = modelInstance.newInstance(Documentation.class);
							StringBuilder sb = new StringBuilder();
							sb.append("{incomingData:");
							for (BPMNDataObject dao : bpmnBrt.getDataObjects()) {
								sb.append(dao.getNameId() + ",");
							}
							sb.deleteCharAt(sb.length() - 1);
							sb.append("}");
							doc.setTextContent(sb.toString());
							task.getDocumentations().add(doc);
						}

						// Put the voting tasks to the corresponding lanes in the xml model
						FlowNodeRef ref = modelInstance.newInstance(FlowNodeRef.class);
						ref.setTextContent(task.getId());
						FlowNode n = this.getFlowNodeByBPMNNodeId(task.getId());
						if (!l.getFlowNodeRefs().contains(n)) {
							l.addChildElement(ref);
						}

					}
				}

			}
			this.changeBusinessRuleTaskToServiceTask(businessRt, mapModelBtn);
		}
		/*
		 * this.setSuccessors(); this.setPredecessors(); this.storeLanePerTask();
		 */
		if (mapModelBtn) {
			this.mapModel();
		}
		this.writeChangesToFile();

	}

	private void addTasksToVotingSystem(int i, BusinessRuleTask brt, AbstractFlowNodeBuilder builder,
			String parallelSplit, HashMap<BPMNDataObject, ArrayList<BPMNTask>> votersMap, String parallelJoin,
			boolean mapModelBtn) {
		// Functionality to not show parallel split and join when every task that user
		// chooses is from same participant
		// not implemented yet

		if (votersMap.isEmpty()) {
			System.err.println("No voters selected");

		}

		Iterator<Entry<BPMNDataObject, ArrayList<BPMNTask>>> iter = votersMap.entrySet().iterator();
		ArrayList<Task> alreadyModelled = new ArrayList<Task>();
		Set<BPMNDataObject> allBPMNDataObjects = new HashSet<BPMNDataObject>();

		allBPMNDataObjects.addAll(((BPMNBusinessRuleTask) this.getNodeById(brt.getId())).getDataObjects());
		String parallelSplitId = parallelSplit + "split";
		String parallelJoinId = parallelJoin + "join";
		boolean isSet = false;

		if (votersMap.entrySet().size() == 1 && votersMap.entrySet().iterator().next().getValue().size() == 1) {
			int votingTaskId = BPMNTask.increaseVotingTaskId();
			BPMNDataObject key = iter.next().getKey();
			ArrayList<BPMNTask> nextList = votersMap.get(key);
			Iterator<BPMNTask> nextListIter = nextList.iterator();
			BPMNParticipant nextParticipant = nextListIter.next().getParticipant();

			if (mapModelBtn) {
				builder.userTask("votingTask" + votingTaskId).name("VotingTask " + nextParticipant.getName())
						.connectTo(brt.getId());
			} else {
				builder.userTask("votingTask" + votingTaskId).name("VotingTask " + nextParticipant.getName()).endEvent()
						.subProcessDone().connectTo(brt.getId());
			}

			for (BPMNDataObject dao : allBPMNDataObjects) {
				this.addDataInputReferencesToVotingTasks(
						(Task) this.getFlowNodeByBPMNNodeId("votingTask" + votingTaskId), dao);
			}

		} else {

			builder.parallelGateway(parallelSplitId).name(parallelSplit);

			while (iter.hasNext()) {

				BPMNDataObject key = iter.next().getKey();

				ArrayList<BPMNTask> nextList = votersMap.get(key);
				Iterator<BPMNTask> nextListIter = nextList.iterator();
				boolean skip = false;
				while (nextListIter.hasNext()) {
					BPMNParticipant nextParticipant = nextListIter.next().getParticipant();

					for (Task t : alreadyModelled) {
						if (t.getName().equals("VotingTask " + nextParticipant.getName())) {
							for (BPMNDataObject dao : allBPMNDataObjects) {
								this.addDataInputReferencesToVotingTasks(t, dao);
							}
							skip = true;
						}
					}
					if (skip == false) {
						int votingTaskId = BPMNTask.increaseVotingTaskId();

						builder.moveToNode(parallelSplitId).userTask("votingTask" + votingTaskId)
								.name("VotingTask " + nextParticipant.getName());
						alreadyModelled.add((Task) this.getFlowNodeByBPMNNodeId("votingTask" + votingTaskId));
						for (BPMNDataObject dao : allBPMNDataObjects) {
							this.addDataInputReferencesToVotingTasks(
									(Task) this.getFlowNodeByBPMNNodeId("votingTask" + votingTaskId), dao);
						}

						if (isSet == false) {
							builder.moveToNode("votingTask" + votingTaskId).parallelGateway(parallelJoinId)
									.name(parallelJoin);
							isSet = true;
						} else {
							builder.moveToNode("votingTask" + votingTaskId).connectTo(parallelJoinId);
						}

					}
					if (!iter.hasNext() && !nextListIter.hasNext() && mapModelBtn) {
						builder.moveToNode(parallelJoinId).connectTo(brt.getId());
					} else {
						if (nextListIter.hasNext()) {
							builder.moveToNode(parallelSplitId);
						}
					}

					skip = false;
				}
			}

			if (!mapModelBtn) {
				builder.moveToNode(parallelJoinId).endEvent().subProcessDone().connectTo(brt.getId());
			}
		}

	}

	public void addVotingSystem(BPMNElement element) {
		// Check how many participants are needed for the voting!
		// Check the corresponding Data Objects for the decision
		// Get the latest Task(s) that wrote data to these data objects
		// Check the Spheres that Readers have to be in!
		// Compute the spheres and propose participants for voting to the user

		LinkedList<BPMNTask> lastWriters = new LinkedList<BPMNTask>();

		if (this.checkProcessModel() == true) {
			if (element instanceof BPMNBusinessRuleTask) {
				BPMNBusinessRuleTask brt = (BPMNBusinessRuleTask) element;
				this.businessRuleTaskList.add(brt);
				String minimumSphere = "";
				for (BPMNDataObject data : brt.getDataObjects()) {
					BPMNTask lastWriter = this.getLastWriterForDataObject(brt, data, null);
					lastWriters.add(lastWriter);
					brt.getLastWriterList().add(lastWriter);

					// if lastWriter is within an other xor branch than the brt we need to find the
					// lastWriter for the other branch too
					if (!lastWriter.getLabels().equals(brt.getLabels())) {
						BPMNTask lastWriterOtherBranch = this.getLastWriterForDataObject(brt, data, lastWriter);
						if (lastWriterOtherBranch != null) {
							lastWriters.add(lastWriterOtherBranch);
							brt.getLastWriterList().add(lastWriterOtherBranch);
						}
					}

					while (!lastWriters.isEmpty()) {
						BPMNTask lWriter = lastWriters.pollFirst();
						System.out.print("LASTWRITER: ");
						lWriter.printElement();
						for (BPMNDataObject sphere : lWriter.getSphereAnnotation().keySet()) {
							minimumSphere = lWriter.getSphereAnnotation().get(sphere);
							System.out.println("Sphere" + sphere.getName() + minimumSphere);

							for (BPMNElement reader : sphere.getReaders()) {
								System.out.println("READER");
								reader.printElement();
								LinkedList<LinkedList<BPMNElement>> paths = this.allPathsBetweenNodes(lWriter, reader,
										new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(),
										new LinkedList<BPMNElement>(), new LinkedList<LinkedList<BPMNElement>>());
								// Check whether there is a path between the lastWriter and some Reader
								// if not, then the reader is a task that comes before the lastWriter
								if (!paths.isEmpty()) {
									// When there is another writer w2 for the data object on each path
									// between the lWriter and some reader, then the reader is in the static sphere

									int count = 0;
									for (BPMNElement otherWriter : data.getOtherWriters(lWriter)) {
										for (LinkedList<BPMNElement> path : paths) {
											if (path.contains(otherWriter)) {
												count++;
											}
										}
									}
									this.addReaderToSphere(brt, sphere, count, lWriter, (BPMNTask) reader, paths);
									count = 0;
								}
							}

						}

					}

				}

			}

		}

	}

	private void addReaderToSphere(BPMNBusinessRuleTask brt, BPMNDataObject bpmndo, int count, BPMNTask writer,
			BPMNTask reader, LinkedList<LinkedList<BPMNElement>> paths) {
		ArrayList<Label> writerLabels = writer.getLabels();
		ArrayList<Label> readerLabels = reader.getLabels();
		int anotherWriterOnPath = paths.size() - count;
		// if anotherWriterOnPath == 0, then there is anotherWriter on each path between
		// reader and writer
		// if anotherWriterOnPath == paths.size, then there is no path containing
		// anotherWriter between reader and writer
		// if anotherWriterOnPath > 0 && < paths.size, then there is at least one path
		// containing another writer between reader and writer

		if (writerLabels.equals(readerLabels)) {
			if (anotherWriterOnPath == paths.size()) {
				writer.addTaskToSDHashMap(brt, bpmndo, reader);
			} else if (anotherWriterOnPath > 0 && anotherWriterOnPath < paths.size()) {
				writer.addTaskToWDHashMap(brt, bpmndo, reader);
			}
			// writer.addTaskToWDHashMap(bpmndo, reader);
		} else if (writerLabels.size() > readerLabels.size()) {
			if (readerLabels.size() == 0) {
				if (anotherWriterOnPath == paths.size()) {
					writer.addTaskToSDHashMap(brt, bpmndo, reader);
				} else if (anotherWriterOnPath > 0 && anotherWriterOnPath < paths.size()) {
					writer.addTaskToWDHashMap(brt, bpmndo, reader);
				}
				// writer.addTaskToWDHashMap(bpmndo, reader);
			} else {
				if (writerLabels.containsAll(readerLabels)) {
					if (anotherWriterOnPath == paths.size()) {
						writer.addTaskToSDHashMap(brt, bpmndo, reader);
					} else if (anotherWriterOnPath > 0 && anotherWriterOnPath < paths.size()) {
						writer.addTaskToWDHashMap(brt, bpmndo, reader);
					}
					// writer.addTaskToWDHashMap(bpmndo, reader);
				} else {
					if (anotherWriterOnPath == paths.size()) {
						writer.addTaskToWDHashMap(brt, bpmndo, reader);
					} else if (anotherWriterOnPath > 0 && anotherWriterOnPath < paths.size()) {
						writer.addTaskToWDHashMap(brt, bpmndo, reader);
					}
				}
			}

		} else if (writerLabels.size() < readerLabels.size()) {
			if (anotherWriterOnPath == paths.size()) {
				writer.addTaskToWDHashMap(brt, bpmndo, reader);
			} else if (anotherWriterOnPath > 0 && anotherWriterOnPath < paths.size()) {
				writer.addTaskToWDHashMap(brt, bpmndo, reader);
			}
		} else if (writerLabels.size() == readerLabels.size() && !writerLabels.equals(readerLabels)) {
			if (anotherWriterOnPath == paths.size()) {
				writer.addTaskToWDHashMap(brt, bpmndo, reader);
			} else if (anotherWriterOnPath > 0 && anotherWriterOnPath < paths.size()) {
				writer.addTaskToWDHashMap(brt, bpmndo, reader);
			}
		}

	}

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

	public BPMNGateway getCorrespondingJoinGtw(BPMNGateway split) {
		for (BPMNElement join : this.processElements) {
			if (join instanceof BPMNGateway) {
				if (((BPMNGateway) join).getType().equals("join")
						&& ((BPMNGateway) join).getName().equals(split.getName())) {
					return ((BPMNGateway) join);
				}
			}
		}
		return null;
	}

	
	private void addDataInputReferencesToVotingTasks(Task task, BPMNDataObject dataObject) {
		boolean alreadyModelled = false;
		//check whether there is already a DataInputAssociation between the task and the dataObject
		for(DataInputAssociation di: task.getDataInputAssociations()) {
			for(ItemAwareElement item : di.getSources()) {
			  if(item.getAttributeValue("dataObjectRef").equals(dataObject.getId())){
				   alreadyModelled = true;
			   }
			}
		}
					
			if (task.getDataInputAssociations().isEmpty()||alreadyModelled==false) {
				DataInputAssociation dia = modelInstance.newInstance(DataInputAssociation.class);

				Property p1 = modelInstance.newInstance(Property.class);
				p1.setName("__targetRef_placeholder");

				task.addChildElement(p1);
				dia.setTarget(p1);
				task.getDataInputAssociations().add(dia);

				double xDataObject = 0;
				double yDataObject = 0;
				double xTask = 0;
				double yTask = 0;
				for (DataObjectReference d1 : modelInstance.getModelElementsByType(DataObjectReference.class)) {
					if (dataObject.getId().equals(d1.getAttributeValue("dataObjectRef"))) {
						dia.getSources().add(d1);

						for (BpmnShape shape : modelInstance.getModelElementsByType(BpmnShape.class)) {
							if (shape.getBpmnElement().getId().equals(d1.getId())) {
								xDataObject = shape.getBounds().getX() + (shape.getBounds().getWidth() / 2);
								yDataObject = (shape.getBounds().getY() + shape.getBounds().getHeight());

							}
							if (shape.getBpmnElement().getId().equals(task.getId())) {
								xTask = shape.getBounds().getX();
								yTask = shape.getBounds().getY();
							}

						}
					}
				}

				BpmnEdge e = modelInstance.newInstance(BpmnEdge.class);
				e.setBpmnElement(dia);
				Waypoint wp = modelInstance.newInstance(Waypoint.class);
				// Waypoints for the source -> the Data Object
				wp.setX(xDataObject);
				wp.setY(yDataObject);
				e.addChildElement(wp);

				// Waypoint for the target -> the Task that has the Data Input
				Waypoint wp2 = modelInstance.newInstance(Waypoint.class);

				wp2.setX(xTask);
				wp2.setY(yTask);
				e.addChildElement(wp2);

				// e.getParentElement().addChildElement(e);
				modelInstance.getModelElementsByType(Plane.class).iterator().next().addChildElement(e);
			}
		
	}

	private void removeBusinessRuleTask(BusinessRuleTask brt) {
		SequenceFlow outgoingSeq = brt.getOutgoing().iterator().next();
		BpmnEdge flowDi = outgoingSeq.getDiagramElement();
		for (BpmnEdge e : modelInstance.getModelElementsByType(BpmnEdge.class)) {
			if (e.getId().equals(flowDi.getId())) {
				e.getParentElement().removeChildElement(e);
			}
		}

		outgoingSeq.getParentElement().removeChildElement(outgoingSeq);

		SequenceFlow incomingSeq = brt.getIncoming().iterator().next();
		BpmnEdge flowDi2 = incomingSeq.getDiagramElement();
		for (BpmnEdge e : modelInstance.getModelElementsByType(BpmnEdge.class)) {
			if (e.getId().equals(flowDi2.getId())) {
				e.getParentElement().removeChildElement(e);
			}
		}
		incomingSeq.getParentElement().removeChildElement(incomingSeq);

		brt.getDataInputAssociations().removeAll(brt.getDataInputAssociations());
		for (BpmnEdge bpmnE : modelInstance.getModelElementsByType(BpmnEdge.class)) {
			if (bpmnE.getBpmnElement() == null) {
				bpmnE.getParentElement().removeChildElement(bpmnE);
			}
		}

		brt.getParentElement().removeChildElement(brt);
		for (BpmnShape bpmnS : modelInstance.getModelElementsByType(BpmnShape.class)) {
			if (bpmnS.getBpmnElement() == null) {
				bpmnS.getParentElement().removeChildElement(bpmnS);
			}
		}

	}

	private void changeBusinessRuleTaskToServiceTask(BusinessRuleTask brt, boolean withSubProcess) {

		SequenceFlow incomingSequenceFlow = brt.getIncoming().iterator().next();
		BpmnEdge flowDi = incomingSequenceFlow.getDiagramElement();

		for (BpmnEdge e : modelInstance.getModelElementsByType(BpmnEdge.class)) {
			if (e.getId().equals(flowDi.getId())) {
				e.getParentElement().removeChildElement(e);
			}
		}
		incomingSequenceFlow.getParentElement().removeChildElement(incomingSequenceFlow);

		ServiceTask serviceTask = modelInstance.newInstance(ServiceTask.class);
		serviceTask.setId(brt.getId());
		serviceTask.setName("Collect Votes");
		// serviceTask.getDataInputAssociations().addAll(brt.getDataInputAssociations());
		// serviceTask.getProperties().addAll(brt.getProperties());

		if (withSubProcess) {
			serviceTask.setCamundaType("external");
			serviceTask.setCamundaTopic("voting");
			Documentation dataObjectDocu = modelInstance.newInstance(Documentation.class);
			StringBuilder sb = new StringBuilder();
			sb.append("{dataObjects:");
			for (BPMNDataObject dao : ((BPMNBusinessRuleTask) this.getNodeById(brt.getId())).getDataObjects()) {
				sb.append(dao.getNameId() + ",");
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append("}");

			dataObjectDocu.setTextContent(sb.toString());
			serviceTask.getDocumentations().add(dataObjectDocu);
		}

		brt.replaceWithElement(serviceTask);

		// remove the remaining bpmndi:dataInputAssociations
		for (BpmnEdge bpmnE : modelInstance.getModelElementsByType(BpmnEdge.class)) {
			if (bpmnE.getBpmnElement() == null) {
				bpmnE.getParentElement().removeChildElement(bpmnE);
			}
		}
	}

	public ArrayList<BPMNBusinessRuleTask> getBusinessRuleTasks() {
		return this.businessRuleTaskList;
	}

	public void writeChangesToFile() throws IOException {
		// validate and write model to file

		Bpmn.validateModel(modelInstance);

		File file = File.createTempFile("bpmn-model-with-voting", ".bpmn",
				new File("C:\\Users\\Micha\\OneDrive\\Desktop"));
		Bpmn.writeModelToFile(file, modelInstance);
	}

	public void moveNodesToCorrespondingLanesInDiagram(BPMNTask votingTask) {
		// put the inserted voting tasks with fluent builder to the correct lane in the
		// diagram!
		// can not be done with the fluent builder directly
		// the x value of the task is correct, but y needs to be set to equal the last
		// element in the corresponding lane
		BPMNElement lastElementInLane = this.getLastElementOnLaneBeforeCurrentNode(votingTask,
				votingTask.getParticipant());
		System.out.println("LAstelement inlane " + lastElementInLane.getId());

		double newY = 0;
		double newX = 0;
		double oldX = 0;
		BpmnShape votingTaskShape = null;
		for (BpmnShape shape : modelInstance.getModelElementsByType(BpmnShape.class)) {
			if (shape.getBpmnElement().getId().equals(votingTask.getId())) {
				votingTaskShape = shape;
				oldX = shape.getBounds().getX();
				newX = shape.getBounds().getX() + 20;
			}
			if (shape.getBpmnElement().getId().equals(lastElementInLane.getId())) {
				newY = shape.getBounds().getY();

			}
		}
		if (votingTaskShape != null) {
			votingTaskShape.getBounds().setX(newX);
			votingTaskShape.getBounds().setY(newY);
		}

		// change the incoming edge
		SequenceFlow incomingEdge = this.getFlowNodeByBPMNNodeId(votingTask.getId()).getIncoming().iterator().next();
		System.out.println("IncomingEdge: " + incomingEdge.getId());
		for (BpmnEdge edge : modelInstance.getModelElementsByType(BpmnEdge.class)) {
			if (edge.getBpmnElement().equals(incomingEdge)) {
				// edge.getBpmnLabel().getBounds().setX(newX);
				// edge.getBpmnLabel().getBounds().setY(newY);
				Iterator<Waypoint> wpIter = edge.getWaypoints().iterator();
				while (wpIter.hasNext()) {
					Waypoint currPoint = wpIter.next();
					if (!wpIter.hasNext()) {
						currPoint.setX(newX);
						currPoint.setY(newY);
					}
				}

			}
		}

	}

	public void mapModel() {

		Iterator<Lane> laneIter = modelInstance.getModelElementsByType(Lane.class).iterator();
		while (laneIter.hasNext()) {
			Lane nextLane = laneIter.next();
			for (FlowNode flowNode : nextLane.getFlowNodeRefs()) {
				for (TextAnnotation txt : modelInstance.getModelElementsByType(TextAnnotation.class)) {
					for (Association a : modelInstance.getModelElementsByType(Association.class)) {
						if (flowNode instanceof ExclusiveGateway || flowNode instanceof ServiceTask) {
							// remove XOR-Annotations for the amount of participants needed
							// remove Decision-Annotations for BusinessRuleTasks which have been changed to
							// ServiceTasks!

							if ((txt.getTextContent().startsWith("[Voters]")
									|| txt.getTextContent().startsWith("[Decision]")) && a.getSource().equals(flowNode)
									&& txt.getId().equals(a.getTarget().getId())) {
								for (BpmnEdge edge : modelInstance.getModelElementsByType(BpmnEdge.class)) {
									if (edge.getBpmnElement().equals(a)) {
										edge.getParentElement().removeChildElement(edge);
									}
								}
								a.getParentElement().removeChildElement(a);
								txt.getParentElement().removeChildElement(txt);

							}
						}
					}
				}
				if (flowNode instanceof Task) {
					if (!(flowNode instanceof ServiceTask && flowNode.getName().equals("Collect Votes"))) {
						if (flowNode.getName().startsWith("VotingTask")) {
							flowNode.setAttributeValue("name", "VotingTask [" + nextLane.getName() + "]");
						} else {
							flowNode.setAttributeValue("name", flowNode.getName() + " [" + nextLane.getName() + "]");
						}
					}
				}
			}
			nextLane.getParentElement().removeChildElement(nextLane);

		}

		// remove collaboration
		for (Collaboration cr : modelInstance.getModelElementsByType(Collaboration.class)) {
			cr.getParentElement().removeChildElement(cr);
		}

		// remove laneSet
		for (LaneSet ls : modelInstance.getModelElementsByType(LaneSet.class)) {
			ls.getParentElement().removeChildElement(ls);
		}

		// remove remaining bpmndi elements
		for (BpmnShape shape : modelInstance.getModelElementsByType(BpmnShape.class)) {
			if (shape.getBpmnElement() == null) {
				shape.getParentElement().removeChildElement(shape);
			}
		}

		for (BpmnPlane lane : modelInstance.getModelElementsByType(BpmnPlane.class)) {
			if (lane.getBpmnElement() == null) {
				lane.setBpmnElement(this.modelInstance.getModelElementsByType(Process.class).iterator().next());
			}
		}

	}

}
