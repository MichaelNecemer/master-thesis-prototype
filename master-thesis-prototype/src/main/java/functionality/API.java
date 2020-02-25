package functionality;

import java.io.File;
import java.io.IOException;
import java.security.Provider.Service;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Stack;

import org.camunda.bpm.model.bpmn.builder.*;
import org.camunda.bpm.model.bpmn.builder.ProcessBuilder;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.Query;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
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
	private ArrayList<BPMNTask> processTasks = new ArrayList<BPMNTask>();
	private ArrayList<BPMNParticipant> globalSphere = new ArrayList<BPMNParticipant>();
	private ArrayList<ArrayList<BPMNParticipant>> staticSphere = new ArrayList<ArrayList<BPMNParticipant>>();

	ArrayList<Label> labelList = new ArrayList<Label>();

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
		for(BPMNElement element: this.processElements) {
			if(element instanceof BPMNBusinessRuleTask) {
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
				// Attach the voters to the Gateway
				this.mapSphereAnnotations((BPMNExclusiveGateway) mappedNode);
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
			this.dataObjects.add(new BPMNDataObject(d.getAttributeValue("dataObjectRef"), d.getName()));
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
						if(element.getSphereAnnotation().isEmpty()) {
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
					// The number of voters is between []
					String s = str.substring(str.indexOf('[') + 1, str.indexOf(']'));
					gtw.setVoters(Integer.parseInt(s));
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

	public void addVotingTasksToProcess(BPMNBusinessRuleTask brt, ArrayList<BPMNParticipant> voters)
			throws IOException {

		// UserTask t = (UserTask)
		// this.getFlowNodeByBPMNNodeId(brt.getPredecessors().iterator().next().getId());

		BusinessRuleTask businessRt = (BusinessRuleTask) this.getFlowNodeByBPMNNodeId(brt.getId());
		// SequenceFlow outgoingSequenceFlow = t.getOutgoing().iterator().next();
		SequenceFlow incomingSequenceFlow = businessRt.getIncoming().iterator().next();
		// BpmnEdge flowDi = outgoingSequenceFlow.getDiagramElement();
		BpmnEdge flowDi = incomingSequenceFlow.getDiagramElement();

		// BPMNDi elements are need to be removed too
		for (BpmnEdge e : modelInstance.getModelElementsByType(BpmnEdge.class)) {
			if (e.getId().equals(flowDi.getId())) {
				e.getParentElement().removeChildElement(e);
			}
		}
		// modelInstance.getDefinitions().removeChildElement(outgoingSequenceFlow);
		modelInstance.getDefinitions().removeChildElement(incomingSequenceFlow);

		this.addTasksToVotingSystem(1, businessRt,
				businessRt.builder().moveToNode(brt.getPredecessors().iterator().next().getId()), "P2split", voters,
				"P2join");

		// Add the new tasks generated via fluent builder API to the corresponding lanes
		// in the xml model
		// Cant be done with the fluent model builder directly!

		for (Lane l : modelInstance.getModelElementsByType(Lane.class)) {
			for (Task task : modelInstance.getModelElementsByType(Task.class)) {
				if (l.getName()
						.equals(task.getName().substring(task.getName().indexOf(" ") + 1, task.getName().length()))) {
					task.getParentElement().addChildElement(task);
				}
			}

		}
		/*
		 * ServiceTask st = modelInstance.newInstance(ServiceTask.class);
		 * st.setName("Perform decision"); st.setId(businessRt.getId());
		 * 
		 * 
		 * 
		 * 
		 * //st.getDiagramElement().setBpmnElement(businessRt);
		 * //st.getDataInputAssociations().addAll(businessRt.getDataInputAssociations())
		 * ;
		 * 
		 * 
		 * businessRt.getParentElement().replaceChildElement(businessRt, st);
		 * for(BpmnEdge dia : modelInstance.getModelElementsByType(BpmnEdge.class)) {
		 * if(dia.getId().equals("DataInputAssociation_0e8o986_di")) {
		 * 
		 * }
		 * 
		 * }
		 */

		// validate and write model to file
		Bpmn.validateModel(modelInstance);
		
		// File file = File.createTempFile("bpmn-model-with-voting", ".bpmn", new
		// File("C:\\Users\\Micha\\OneDrive\\Desktop")); Bpmn.writeModelToFile(file,
		 //modelInstance);
		 
	}

	private void addTasksToVotingSystem(int i, BusinessRuleTask brt, AbstractFlowNodeBuilder builder,
			String parallelSplit, ArrayList<BPMNParticipant> voters, String parallelJoin) {
		if(voters.isEmpty()) {
			System.err.println("No voters selected");
			return;
		}
		boolean isSet = false;
		Iterator<BPMNParticipant> iter = voters.iterator();

		voters.forEach(v -> {
			v.printParticipant();
		});
		if(voters.size()==1) {
			builder.userTask("votingTask"+i).name("VotingTask "+voters.iterator().next().getName())
			.connectTo(brt.getId());
		} else {
		
		builder.parallelGateway(parallelSplit).name("P2");

		while (iter.hasNext()) {
			builder.moveToNode(parallelSplit).userTask("votingTask" + i).name("VotingTask " + iter.next().getName());
			if (isSet == false) {
				builder.moveToNode("votingTask" + i).parallelGateway(parallelJoin).name("P2");
				isSet = true;
			} else {
				builder.moveToNode("votingTask" + i).connectTo(parallelJoin);
			}

			if (!iter.hasNext()) {
				builder.moveToNode(parallelJoin).connectTo(brt.getId());
			} else {
				builder.moveToNode(parallelSplit);
			}
			i++;

		}	
		}
	}

	public void addVotingSystem(BPMNElement element) {
		// Check how many participants are needed for the voting!
		// Check the corresponding Data Objects for the decision
		// Get the latest Task(s) that wrote data to these data objects
		// Check the Spheres that Readers have to be in!
		// Compute the spheres and propose participants for voting to the user
		ArrayList<BPMNParticipant> listOfParticipants = new ArrayList<BPMNParticipant>();
		ArrayList<BPMNTask> lastWriters = new ArrayList<BPMNTask>();

		if (this.checkProcessModel() == true) {
			if (element instanceof BPMNBusinessRuleTask) {
				BPMNBusinessRuleTask brt = (BPMNBusinessRuleTask) element;
				String value = "";
				for (BPMNDataObject data : brt.getDataObjects()) {

					BPMNTask lastWriter = this.getLastWriterForDataObject(brt, data, null);
					lastWriters.add(lastWriter);
					// if lastWriter is within an xor-branch we need to find the lastWriter for the
					// other branch too
					if (lastWriter.hasLabel()) {
						BPMNTask lastWriterOtherBranch = this.getLastWriterForDataObject(brt, data, lastWriter);
						lastWriters.add(lastWriterOtherBranch);
					}

					for (BPMNTask lWriter : lastWriters) {
						lWriter.printElement();
						for (BPMNDataObject sphere : lWriter.getSphereAnnotation().keySet()) {
							value = lWriter.getSphereAnnotation().get(sphere);
							System.out.println("Sphere" + sphere.getName() + value);

							for (BPMNElement reader : sphere.getReaders()) {
								LinkedList<LinkedList<BPMNElement>> paths = this.allPathsBetweenNodes(lWriter, reader,
										new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(),
										new LinkedList<BPMNElement>(), new LinkedList<LinkedList<BPMNElement>>());
								// Check whether there is a path between the lastWriter and some Reader
								// if not, then the reader is a task that comes before the lastWriter
								if (!paths.isEmpty()) {
									// When there is another writer w2 for the data object on each path
									// between the lWriter and some reader, then the reader is in the static sphere
									boolean anotherWriterOnPath = false;
									int count = 0;
									
									for (BPMNElement otherWriter : data.getOtherWriters(lWriter)) {
										
										for (LinkedList<BPMNElement> path : paths) {						
											if (path.contains(otherWriter)) {
												count++;
											}
										}
										if(count%2==1) {
											anotherWriterOnPath = true;
										}
									}
									System.out.println(anotherWriterOnPath);
									// within an XOR-branch
									if (anotherWriterOnPath == false) {
										this.addReaderToSphere(sphere,lWriter, (BPMNTask)reader, paths);
									}
								}
							}

						}

					}
				}


				for(BPMNTask t: lastWriters) {
					for(Entry<BPMNDataObject, ArrayList<BPMNTask>> t2: t.getSDHashMap().entrySet()){
						for(BPMNTask task: t2.getValue()) {
							task.printElement();
						if(!listOfParticipants.contains(task.getParticipant())) {							
							listOfParticipants.add(task.getParticipant());
						}
					}
					}
				}
				
				
				
				try {

					this.addVotingTasksToProcess(brt, listOfParticipants);

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

	}
	
	private void addReaderToSphere(BPMNDataObject bpmndo, BPMNTask writer, BPMNTask reader, LinkedList<LinkedList<BPMNElement>>paths) {
		ArrayList<Label> writerLabels = writer.getLabels();
		ArrayList<Label> readerLabels = reader.getLabels();
		
		//When the labels are the same, the reader is in the SD Sphere of the writer
		//Therefore also in the WD Sphere
		if(writerLabels.equals(readerLabels)) {
			writer.addTaskToSDHashMap(bpmndo, reader);
			writer.addTaskToWDHashMap(bpmndo, reader);
		} else if (writerLabels.size()>readerLabels.size()) {
			if(readerLabels.size()==0) {
				writer.addTaskToSDHashMap(bpmndo, reader);
			} else {
				writer.addTaskToWDHashMap(bpmndo, reader);
			}
			
		} else if(writerLabels.size()<readerLabels.size()) {
			writer.addTaskToWDHashMap(bpmndo, reader);
			
			
		}
	
		
			
		
		
		
		
		
	}
	
	
	

	public boolean checkProcessModel() {
		// Check how many participants are needed for the voting!
		// Check whether there are enough participants available in the global Sphere
		for (BPMNElement bpmnElement : this.processElements) {
			if (bpmnElement instanceof BPMNExclusiveGateway) {
				if (((BPMNExclusiveGateway) bpmnElement).getType().equals("split")) {
					if (((BPMNExclusiveGateway) bpmnElement).getVoters() > this.globalSphere.size()) {
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

}
