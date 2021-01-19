package ProcessModelGeneratorAndAnnotater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.Node;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Gateway;
import org.camunda.bpm.model.bpmn.instance.ManualTask;
import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.di.Waypoint;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import Mapping.BPMNElement;
import Mapping.BPMNEndEvent;
import Mapping.BPMNExclusiveGateway;
import Mapping.BPMNGateway;

public class ProcessGenerator {
	// generates a new Process Model using the camunda fluent API
	// model can than be annotated using the ProcessModelAnnotater

	private static int run = 0;
	private static BpmnModelInstance modelInstance;
	private LinkedList<String> participantNames;
	private int amountTasksLeft;
	private int amountXorsLeft;
	private int amountParallelsLeft;
	
	private int probTask;
	private int probXorGtw;
	private int probParallelGtw;
	private int nestingDepthFactor;
	private LinkedList<FlowNode> queue;
	private LinkedList<FlowNode> entryPointsStack;

	private static int taskId;
	private static int xorGtwId;
	private static int parallelGtwId;
	private int[] taskProbArray;
	private int[] xorProbArray;
	private int[] parallelProbArray;
	private FlowNode lastNode = null;
	private LinkedList<Gateway> openSplits;
	private LinkedList<String>possibleNodeTypes;

	public ProcessGenerator(int amountParticipants, int amountTasksLeft, int amountXorsLeft,
			int amountParallelsLeft, int probTask, int probXorGtw, int probParallelGtw, int nestingDepthFactor) {
		// process model will have 1 StartEvent and 1 EndEvent
		this.participantNames = new LinkedList<String>();
		for (int i = 0; i < amountParticipants; i++) {
			String participantName = "Participant_" + (i + 1);
			participantNames.add(participantName);
		}
		this.amountTasksLeft=amountTasksLeft;
		this.amountXorsLeft=amountXorsLeft;
		this.amountParallelsLeft=amountParallelsLeft;
		
		this.probTask = probTask;
		this.probXorGtw = probXorGtw;
		this.probParallelGtw = probParallelGtw;
		this.nestingDepthFactor = nestingDepthFactor;
		this.entryPointsStack = new LinkedList<FlowNode>();
		this.openSplits = new LinkedList<Gateway>();

		taskId = 1;
		xorGtwId = 1;
		parallelGtwId = 1;

		taskProbArray = new int[2];
		taskProbArray[0] = 0;
		taskProbArray[1] = probTask - 1;

		xorProbArray = new int[2];
		xorProbArray[0] = probTask;
		xorProbArray[1] = (probTask + probXorGtw - 1);

		parallelProbArray = new int[2];
		parallelProbArray[0] = (probTask + probXorGtw);
		parallelProbArray[1] = (probTask + probXorGtw + probParallelGtw - 1);

		modelInstance = Bpmn.createProcess("Process"+run).startEvent("startEvent_1").done();
		FlowNode startEvent = (FlowNode) modelInstance.getModelElementById("startEvent_1");
		

		entryPointsStack.add(startEvent);

		this.possibleNodeTypes = new LinkedList<String>();
		this.possibleNodeTypes.add("Task");
		this.possibleNodeTypes.add("ExclusiveGateway");
		this.possibleNodeTypes.add("ParallelGateway");
		
		// go dfs
		this.goDfs(startEvent, null, amountTasksLeft, amountXorsLeft, amountParallelsLeft, this.possibleNodeTypes, new LinkedList<FlowNode>(), new LinkedList<FlowNode>(), this.nestingDepthFactor);
		
		
	
		try {
			writeChangesToFile();
		} catch (IOException | ParserConfigurationException | SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private void goDfs(FlowNode startNode, FlowNode endNode, int amountTasksLeft, int amountXorsLeft, int amountParallelsLeft, LinkedList<String> possibleNodeTypesToBeInserted, LinkedList<FlowNode>queue, LinkedList<FlowNode>openXorStack, int branchingFactor){
		
		queue.add(startNode);
		while (!(queue.isEmpty())) {
		FlowNode currentNode = queue.poll();
		
		
		LinkedList<String> nodeTypesToBeInserted = this.computeNodesToBeInserted(possibleNodeTypesToBeInserted, amountTasksLeft, amountXorsLeft, amountParallelsLeft);
		if(nodeTypesToBeInserted.isEmpty()) {		
			//no constructs can be inserted anymore
			//elements in the queue need to be connected to join nodes 
			//last element needs to be connected to end event
			
				//check if there are open xor-splits that need to be closed before appending the end event
				
				for(int i = 0; i<queue.size(); i++) {
					FlowNode lastNode = queue.pollFirst();					
					
					if(queue.isEmpty()) {
						lastNode.builder().endEvent().id("endEvent_1").name("endEvent_1");
						return;
					} else {
						if(lastNode.getOutgoing().isEmpty()) {
							
							//find last xor-split
							FlowNode lastOpenedXor = lastNode.builder().findLastGateway();
							if(lastOpenedXor!=null) {
								if(lastOpenedXor.getId().contains("_split")) {
									
								} else {
									
								}
							} 
							
							
							
							
							String name = lastOpenedXor.getName();
							String joinId = lastOpenedXor.getName()+"_join";
							if(modelInstance.getModelElementById(joinId)!=null) {
								lastNode.builder().connectTo(joinId);
							} else {
								lastNode.builder().exclusiveGateway(joinId).name(name);
							}
							
							
							
							
						}
						
					}
					
				}
				
			
				//append the endEvent
				currentNode.builder().endEvent().id("endEvent_1").name("endEvent_1");
				run++;
				System.out.println("RUN"+run);
				for(SequenceFlow f: modelInstance.getModelElementsByType(SequenceFlow.class)) {
					System.out.println("Sflow: "+f.getDiagramElement().getId()+", "+f.getSource().getId()+", "+f.getTarget().getId());
					
				}
				return;
			
		}
		
		InsertionConstruct nextConstructToBeAdded = this.getNextNodeToAdd(possibleNodeTypesToBeInserted,amountTasksLeft, amountXorsLeft, amountParallelsLeft, branchingFactor, openXorStack);	
		FlowNode addedNode = null;
		//append the nextNodeToBeAdded to the currentNode	
		if(nextConstructToBeAdded!=null) {		
			
			if(nextConstructToBeAdded.getType().contentEquals("Task")) {
				currentNode.builder().manualTask(nextConstructToBeAdded.getId()).name(nextConstructToBeAdded.getName());
				amountTasksLeft--;
			} else if(nextConstructToBeAdded.getType().contentEquals("ExclusiveGateway")) {
				currentNode.builder().exclusiveGateway(nextConstructToBeAdded.getId()).name(nextConstructToBeAdded.getName());
				if(nextConstructToBeAdded.getId().contains("_split")) {
				amountXorsLeft--;
				}
			} else if(nextConstructToBeAdded.getType().contentEquals("ParallelGateway")){
				currentNode.builder().parallelGateway(nextConstructToBeAdded.getId()).name(nextConstructToBeAdded.getName());
				if(nextConstructToBeAdded.getId().contains("_split")) {
					amountParallelsLeft--;
				}
			}
			addedNode = modelInstance.getModelElementById(nextConstructToBeAdded.getId());

		
		run++;
		System.out.println("RUN"+run);
		for(SequenceFlow f: modelInstance.getModelElementsByType(SequenceFlow.class)) {
			System.out.println("Sflow: "+f.getId()+", "+f.getSource().getId()+", "+f.getTarget().getId());
	
		}
		
		
		if(addedNode instanceof ExclusiveGateway && addedNode.getId().contains("_split")) {
			queue.add(addedNode);
			openXorStack.add(addedNode);
			openXorStack.add(addedNode);
		}
		//add the nextNode on the stack
		queue.add(addedNode);
				
		}		
		
		
		if (currentNode instanceof ExclusiveGateway
				&& currentNode.getId().contains("_join")) {
			
			ExclusiveGateway joinGtw = (ExclusiveGateway)currentNode;
			
			// when a xor-join is found - poll the last opened xor gateway from the stack
			ExclusiveGateway lastOpenedXor = (ExclusiveGateway) openXorStack.pollLast();

			if (!openXorStack.isEmpty()) {
				if (!openXorStack.contains(lastOpenedXor)) {
					// when the openXorStack does not contain the lastOpenedXor anymore, all
					// branches to the joinGtw have been visited
					// go from joinGtw to the Join of the last opened xor-split in the stack
					
					String id = openXorStack.getLast().getName();
					id+="_join";
					
					this.goDfs(joinGtw.getOutgoing().iterator().next().getTarget(), modelInstance.getModelElementById(id), amountTasksLeft, amountXorsLeft, amountParallelsLeft, nodeTypesToBeInserted,queue, openXorStack, branchingFactor);
				}
			} else if(openXorStack.isEmpty()) {
				// when there are no open Xor gtws 
				// go from the successor of the element to end since the currentElement has already been added to the path
					
				this.goDfs(currentNode.getOutgoing().iterator().next().getTarget(), endNode,amountTasksLeft, amountXorsLeft, amountParallelsLeft, nodeTypesToBeInserted, queue, openXorStack, branchingFactor);
			}

		} 

		}
		
	}

	private LinkedList<String> computeNodesToBeInserted(LinkedList<String>nodeTypes, int amountTasksLeft, int amountXorsLeft, int amountParallelsLeft){
		//check if nodeTypes can still be inserted e.g. a xor-split can only be inserted if there is still at least a flowNode to be inserted
		LinkedList<String> nodeList = new LinkedList<String>();
		nodeList.addAll(nodeTypes);
		if(amountTasksLeft==0) {
			//no tasks can be inserted anymore
			//no parallel and xor splits can be inserted anymore too
			nodeList.remove("ParallelGateway");
			nodeList.remove("ExclusiveGateway");
			nodeList.remove("Task");			
		}
		
		if(amountXorsLeft==0) {
			nodeList.remove("ExclusiveGateway");			
		}
		
		
		if(amountParallelsLeft==0) {
			nodeList.remove("ParallelGateway");
		}
		
		return nodeList;
		
	}
	
	private InsertionConstruct getNextNodeToAdd(LinkedList<String>possibleNodeTypesToBeInserted,int amountTasksLeft,int amountXorsLeft, int amountParallelsLeft, int branchingFactor, LinkedList<FlowNode>openXorStack) {
		// randomly choose next flowNode to be inserted into process out of the possible nodeTypes
		// branchingFactor may lead to adding a join node - and closing the currentBranch
		InsertionConstruct nextNode = null;

		if(!openXorStack.isEmpty()) {
		boolean addJoin = finishCurrentBranch(branchingFactor, openXorStack);
		
		if(addJoin) {
			//add a xor-join to the last opened xor-split
			FlowNode lastOpenedXor = openXorStack.getLast();
			nextNode = new InsertionConstruct(lastOpenedXor.getName()+"_join", lastOpenedXor.getName(), "ExclusiveGateway");
			
		}
		}
		if(possibleNodeTypesToBeInserted.isEmpty()) {
			return null;
		}
		
		int randomInt = 0;
		boolean insert = false;
		while(insert==false) {
			randomInt = this.getRandomInt(0, 100);
			if(randomInt >=taskProbArray[0]&&randomInt<=taskProbArray[1]&&possibleNodeTypesToBeInserted.contains("Task")) {
				insert = true;
			} else if(randomInt >= xorProbArray[0] && randomInt <= xorProbArray[1]&&possibleNodeTypesToBeInserted.contains("ExclusiveGateway")) {
				insert = true;
			} else if(amountParallelsLeft >0 && amountTasksLeft >= 2 &&possibleNodeTypesToBeInserted.contains("ParallelGateway")) {
				insert = true;
			}
			
		}
		
		

		if (randomInt >= taskProbArray[0] && randomInt <= taskProbArray[1]) {
			//create a task
			// randomly add a participant to the name of the task
			if(possibleNodeTypesToBeInserted.contains("Task")) {
			Random rand = new Random();
			String uniqueTaskId = "task_" + taskId;
			String participantName = this.participantNames.get(rand.nextInt(participantNames.size()));
			String taskName = uniqueTaskId + " [" + participantName + "]";
			nextNode = new InsertionConstruct(uniqueTaskId, taskName, "Task");		
			taskId++;
			}
		} else if (randomInt >= xorProbArray[0] && randomInt <= xorProbArray[1]) {			
			if (amountXorsLeft > 0 && amountTasksLeft >= 1 && possibleNodeTypesToBeInserted.contains("ExclusiveGateway")) {
				// create new xor-split if there is at least a task left to be inserted
				
				String name = "exclusiveGateway_" + xorGtwId;
				String uniqueXorGtwIdSplit = "exclusiveGateway_" + xorGtwId + "_split";

				nextNode = new InsertionConstruct(uniqueXorGtwIdSplit, name, "ExclusiveGateway");
				xorGtwId++;
			} 
		} else if (randomInt >= parallelProbArray[0] && randomInt <= parallelProbArray[1]) {
			// create new parallel-split
			if (amountParallelsLeft > 0 && amountTasksLeft >= 2 &&possibleNodeTypesToBeInserted.contains("ParallelGateway")) {
				String name = "parallelGateway_" + parallelGtwId;
				String uniqueParallelGtwIdSplit = "parallelGateway_" + parallelGtwId + "_split";
				nextNode = new InsertionConstruct(uniqueParallelGtwIdSplit, name, "ParallelGateway");
				parallelGtwId++;
			} 
		} 

		return nextNode;
	}
	
	private boolean finishCurrentBranch(int branchingFactor, LinkedList<FlowNode>openXorStack) {
		//possibility to finish current branch should be increased with nesting depth
		//nesting depth is the amount of openXors on the stack
	
		List<FlowNode> listDistinct = openXorStack.stream().distinct().collect(Collectors.toList());
		int randomInt = this.getRandomInt(0, 100);
		if(randomInt<50) {
			return true;
		}
		
		return false;

	}
	
	

	private void connectBranchesToJoin(FlowNode insideBranch) {

		Gateway split = insideBranch.builder().findLastGateway();
		String joinId = "";

		if (split.getId().contains("_split") && split instanceof ExclusiveGateway) {
			joinId = split.getName() + "_join";
			if (modelInstance.getModelElementById("joinId") == null) {
				insideBranch.builder().exclusiveGateway(joinId);
			}
		} else if (split.getId().contains("_split") && split instanceof ParallelGateway) {
			joinId = split.getName() + "_join";
			if (modelInstance.getModelElementById("joinId") == null) {
				insideBranch.builder().parallelGateway(joinId);
			}
		}

		// get the sequenceFlow going from split to first node of the branch of the
		// insideBranch Node
		SequenceFlow oneFlowFromSplitToNode = null;
		LinkedList<SequenceFlow> sFlows = new LinkedList<SequenceFlow>();
		sFlows.addAll(insideBranch.getIncoming());
		while (!sFlows.isEmpty()) {
			SequenceFlow currFlow = sFlows.pollLast();
			if (currFlow.getSource().equals(split)) {
				oneFlowFromSplitToNode = currFlow;
				break;
			} else {
				sFlows.addAll(currFlow.getSource().getIncoming());
			}
		}

		// connect the other path(s) to the join too
		Collection<SequenceFlow> otherPaths = split.getOutgoing();
		otherPaths.remove(oneFlowFromSplitToNode);

		if (otherPaths.isEmpty()) {
			// no other paths from split onwards
			// connect the xor-split to the xor-join
			split.builder().connectTo(joinId);
		}

		for (SequenceFlow otherPathFlow : otherPaths) {
			// get last element on this branch
			LinkedList<FlowNode> queue = new LinkedList<FlowNode>();
			queue.add(otherPathFlow.getTarget());
			while (!queue.isEmpty()) {
				FlowNode currentNode = queue.pollLast();
				if (currentNode.getOutgoing().isEmpty()) {
					currentNode.builder().connectTo(joinId);
					this.entryPointsStack.remove(currentNode);
				} else {
					for (SequenceFlow seq : currentNode.getOutgoing()) {
						queue.add(seq.getTarget());
					}
				}
			}
		}

		this.entryPointsStack.remove(insideBranch);
		this.entryPointsStack.remove(split);

	}

	
	private int getRandomInt(int min, int max) {
		int randomInt = ThreadLocalRandom.current().nextInt(min, max + 1);
		return randomInt;
	}

	
	
	private static void writeChangesToFile() throws IOException, ParserConfigurationException, SAXException {
		// validate and write model to file
		Bpmn.validateModel(modelInstance);
		File file = File.createTempFile("processModel", ".bpmn", new File("C:\\Users\\Micha\\OneDrive\\Desktop"));
		Bpmn.writeModelToFile(file, modelInstance);

		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		docBuilderFactory.setNamespaceAware(true);
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document document = docBuilder.parse(file);
		
		//to change the manual Tasks into normal tasks
		NodeList nodes1 = document.getElementsByTagName("manualTask");
		for (int i = 0; i < nodes1.getLength(); i++) {
			org.w3c.dom.Node eachNode = nodes1.item(i);
		  document.renameNode(eachNode, eachNode.getNamespaceURI(), "task");
		}
		

		NodeList nodeList = document.getElementsByTagName("*");
		for (int i = 0; i < nodeList.getLength(); i++) {
			org.w3c.dom.Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE && !(node.getNamespaceURI() == null)
					&& !(node.getNodeName().contains(":"))) {
				String nodeName = node.getNodeName();
				if (nodeName.equals("property")) {
					node.setPrefix("camunda");

				} else {
					node.setPrefix("bpmn");
				}

				if (((Element) node).hasAttribute("xmlns")) {
					((Element) node).removeAttribute("xmlns");
				}

			}

		}

		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

			StreamResult result = new StreamResult(new PrintWriter(new FileOutputStream(file, false)));
			DOMSource source = new DOMSource(document);
			transformer.transform(source, result);

		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	

}
