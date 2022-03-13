package ProcessModelGeneratorAndAnnotater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Gateway;
import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import functionality.CommonFunctionality;

public class ProcessGenerator implements Callable {
	// generates a new Process Model using the camunda fluent API
	// the model can than be annotated using the ProcessModelAnnotater

	private static int processGeneratorId = 1;
	private int processId;
	private static int run = 1;
	private BpmnModelInstance modelInstance;
	private LinkedList<String> participantNames;

	private static int taskId;
	private static int xorGtwId;
	private static int parallelGtwId;
	private int[] taskProbArray;
	private int[] xorProbArray;
	private int[] parallelProbArray;
	private int nestingDepthFactor;
	private FlowNode startEvent;
	private int amountTasksLeft;
	private int amountXorsLeft;
	private int amountParallelsLeft;
	private int minXorSplits;

	private LinkedList<String> possibleNodeTypes;
	private LinkedList<InsertionConstruct> insertionConstructs;
	private int probJoinGtw;
	private String directoryToStore;

	public ProcessGenerator(String directoryToStore, int amountParticipants, int amountTasksLeft, int amountXorsLeft,
			int amountParallelsLeft, int probTask, int probXorGtw, int probParallelGtw, int probJoinGtw,
			int nestingDepthFactor) throws Exception {
		// process model will have 1 StartEvent and 1 EndEvent
		this.participantNames = new LinkedList<String>();
		for (int i = 0; i < amountParticipants; i++) {
			String participantName = "Participant_" + (i + 1);
			participantNames.add(participantName);
		}

		this.probJoinGtw = probJoinGtw;
		this.amountParallelsLeft = amountParallelsLeft;
		this.amountTasksLeft = amountTasksLeft;
		this.amountXorsLeft = amountXorsLeft;
		this.minXorSplits = 0;

		this.insertionConstructs = new LinkedList<InsertionConstruct>();

		this.nestingDepthFactor = nestingDepthFactor;

		this.processId = processGeneratorId++;

		if (probTask == 0) {
			throw new Exception("Process can not be created without tasks!");
		}

		this.directoryToStore = directoryToStore;
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

		this.modelInstance = Bpmn.createProcess("Process_" + run).startEvent("startEvent_1").done();
		this.startEvent = (FlowNode) modelInstance.getModelElementById("startEvent_1");

		this.possibleNodeTypes = new LinkedList<String>();
		this.possibleNodeTypes.add("Task");
		this.possibleNodeTypes.add("ExclusiveGateway");
		this.possibleNodeTypes.add("ParallelGateway");

	}

	public ProcessGenerator(String directoryToStore, int amountParticipants, int amountTasksLeft, int amountXorsLeft,
			int amountParallelsLeft, int probTask, int probXorGtw, int probParallelGtw, int probJoinGtw,
			int nestingDepthFactor, int minXorSplits) throws Exception {
		// process model will have 1 StartEvent and 1 EndEvent
		this.participantNames = new LinkedList<String>();
		for (int i = 0; i < amountParticipants; i++) {
			String participantName = "Participant_" + (i + 1);
			participantNames.add(participantName);
		}

		this.probJoinGtw = probJoinGtw;
		this.amountParallelsLeft = amountParallelsLeft;
		this.amountTasksLeft = amountTasksLeft;
		this.amountXorsLeft = amountXorsLeft;
		this.minXorSplits = minXorSplits;

		this.insertionConstructs = new LinkedList<InsertionConstruct>();

		this.nestingDepthFactor = nestingDepthFactor;

		this.processId = processGeneratorId++;

		if (probTask == 0) {
			throw new Exception("Process can not be created without tasks!");
		}

		this.directoryToStore = directoryToStore;
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

		this.modelInstance = Bpmn.createProcess("Process_" + run).startEvent("startEvent_1").done();
		this.startEvent = (FlowNode) modelInstance.getModelElementById("startEvent_1");

		this.possibleNodeTypes = new LinkedList<String>();
		this.possibleNodeTypes.add("Task");
		this.possibleNodeTypes.add("ExclusiveGateway");
		this.possibleNodeTypes.add("ParallelGateway");

	}

	private void goDfs(FlowNode startNode, FlowNode endNode, int amountTasksLeft, int amountXorsLeft,
			int amountParallelsLeft, LinkedList<String> possibleNodeTypesToBeInserted, LinkedList<FlowNode> queue,
			LinkedList<FlowNode> openGatewayStack, int nestingDepthFactor) {

		queue.add(startNode);
		while (!(queue.isEmpty())) {
			FlowNode currentNode = queue.pollLast();
			List<FlowNode> listDistinct = openGatewayStack.stream().distinct().collect(Collectors.toList());

			int branchingFactor = this.probJoinGtw + (listDistinct.size() * nestingDepthFactor);

			LinkedList<String> nodeTypesToBeInserted = this.computeNodesToBeInserted(possibleNodeTypesToBeInserted,
					amountTasksLeft, amountXorsLeft, amountParallelsLeft);

			InsertionConstruct nextConstructToBeAdded = this.getNextNodeToAdd(currentNode, nodeTypesToBeInserted,
					amountTasksLeft, amountXorsLeft, amountParallelsLeft, branchingFactor, openGatewayStack, queue);

			FlowNode addedNode = null;
			// append the nextNodeToBeAdded to the currentNode
			if (nextConstructToBeAdded != null) {

				if (nextConstructToBeAdded.getType().contentEquals("Task")) {
					currentNode.builder().manualTask(nextConstructToBeAdded.getId())
							.name(nextConstructToBeAdded.getName());
					amountTasksLeft--;
				} else if (nextConstructToBeAdded.getType().contentEquals("ExclusiveGateway")) {
					if (this.modelInstance.getModelElementById(nextConstructToBeAdded.getId()) == null) {
						currentNode.builder().exclusiveGateway(nextConstructToBeAdded.getId())
								.name(nextConstructToBeAdded.getName());
						if (nextConstructToBeAdded.getId().contains("_split")) {
							amountXorsLeft--;
						}
					} else {
						// check if the currentNode has already been connected to the nextNode
						boolean alreadyConnected = false;
						for (SequenceFlow seq : currentNode.getOutgoing()) {
							if (seq.getTarget()
									.equals(modelInstance.getModelElementById(nextConstructToBeAdded.getId()))) {
								alreadyConnected = true;
							}
						}

						if (alreadyConnected == false) {
							currentNode.builder().connectTo(nextConstructToBeAdded.getId());
						}
					}

				} else if (nextConstructToBeAdded.getType().contentEquals("ParallelGateway")) {
					if (this.modelInstance.getModelElementById(nextConstructToBeAdded.getId()) == null) {

						currentNode.builder().parallelGateway(nextConstructToBeAdded.getId())
								.name(nextConstructToBeAdded.getName());
						if (nextConstructToBeAdded.getId().contains("_split")) {
							amountParallelsLeft--;
						}
					} else {
						// check if the currentNode has already been connected to the nextNode
						boolean alreadyConnected = false;
						for (SequenceFlow seq : currentNode.getOutgoing()) {
							if (seq.getTarget()
									.equals(this.modelInstance.getModelElementById(nextConstructToBeAdded.getId()))) {
								alreadyConnected = true;
							}
						}
						if (alreadyConnected == false) {
							if (currentNode instanceof ParallelGateway) {
								// if the currentNode is a parallelsplit - append a task between it and the join
								Random rand = new Random();
								String uniqueTaskId = "task_" + taskId;
								String participantName = this.participantNames
										.get(rand.nextInt(participantNames.size()));
								String taskName = uniqueTaskId + " [" + participantName + "]";
								InsertionConstruct ic = new InsertionConstruct(uniqueTaskId, taskName, "Task");
								this.insertionConstructs.add(ic);
								taskId++;
								amountTasksLeft--;
								currentNode.builder().manualTask(ic.getId()).name(ic.getName())
										.connectTo(nextConstructToBeAdded.getId());

							} else {
								currentNode.builder().connectTo(nextConstructToBeAdded.getId());
							}
						}
					}
				}
				addedNode = this.modelInstance.getModelElementById(nextConstructToBeAdded.getId());

				if (addedNode instanceof Gateway && addedNode.getId().contains("_split")) {
					queue.add(addedNode);
					openGatewayStack.add(addedNode);
					openGatewayStack.add(addedNode);
				}

				if (addedNode instanceof Gateway && addedNode.getId().contains("_join")) {

					// when a join is found - poll the last opened Gateway from the stack
					Gateway lastOpenedSplit = (Gateway) openGatewayStack.pollLast();

					if (!openGatewayStack.contains(lastOpenedSplit)) {
						// when the openGatewayStack does not contain the lastOpenedSplit anymore, all
						// branches to the joinGtw have been visited
						// add the join gtw to the queue

						String id = lastOpenedSplit.getName();
						id += "_join";
						queue.add(this.modelInstance.getModelElementById(id));
					}

				} else {

					// add the nextNode on the stack
					queue.add(addedNode);
				}
			} else {
				// return of nextNode function is null
				// append endEvent
				if (currentNode instanceof Gateway && currentNode.getId().contains("_split")) {
					String idOfJoin = currentNode.getName() + "_join";
					if (this.modelInstance.getModelElementById(idOfJoin) != null) {
						currentNode.builder().connectTo(idOfJoin).endEvent().id("endEvent_1");
					} else {
						currentNode.builder().exclusiveGateway(idOfJoin).endEvent().id("endEvent_1");
					}
				} else {
					currentNode.builder().endEvent().id("endEvent_1").name("endEvent_1");
				}
				return;

			}

		}

	}

	private LinkedList<String> computeNodesToBeInserted(LinkedList<String> nodeTypes, int amountTasksLeft,
			int amountXorsLeft, int amountParallelsLeft) {
		// check if nodeTypes can still be inserted e.g. a xor-split can only be
		// inserted if there is still at least a flowNode to be inserted
		LinkedList<String> nodeList = new LinkedList<String>();
		nodeList.addAll(nodeTypes);
		if (amountTasksLeft <= 0) {
			// no tasks can be inserted anymore
			// no parallel and xor splits can be inserted anymore too
			nodeList.remove("ParallelGateway");
			nodeList.remove("ExclusiveGateway");
			nodeList.remove("Task");
		}

		if (amountXorsLeft <= 0) {
			nodeList.remove("ExclusiveGateway");
		}

		if (amountParallelsLeft <= 0) {
			nodeList.remove("ParallelGateway");
		}

		return nodeList;

	}

	private InsertionConstruct getNextNodeToAdd(FlowNode currentNode, LinkedList<String> possibleNodeTypesToBeInserted,
			int amountTasksLeft, int amountXorsLeft, int amountParallelsLeft, int branchingFactor,
			LinkedList<FlowNode> openSplitStack, LinkedList<FlowNode> queue) {
		// randomly choose next flowNode to be inserted into process out of the possible
		// nodeTypes
		// branchingFactor may lead to adding a join node - and closing the
		// currentBranch

		if (openSplitStack.isEmpty() && queue.isEmpty() && possibleNodeTypesToBeInserted.isEmpty()) {
			return null;
		}

		InsertionConstruct nextNode = null;

		// if we are inside a parallel branch
		// both branches should have a node before adding them to the join node
		// if we are inside a xor branch
		// check if at least in one branch is a node before adding the join node
		if (!openSplitStack.isEmpty()) {

			boolean callFinishCurrentBranch = false;
			FlowNode lastOpenedSplit = openSplitStack.getLast();

			if (lastOpenedSplit.getId().contains("_split") && lastOpenedSplit.getOutgoing().size() >= 1) {
				// when there is an open branch already after a xor-split
				// call the function to choose whether to close it or not
				callFinishCurrentBranch = true;
				if (lastOpenedSplit instanceof ParallelGateway) {
					// call the function only if there is on each outgoing branch an element
					if (currentNode.equals(lastOpenedSplit)) {
						if (currentNode.getOutgoing().size() <= 1) {
							callFinishCurrentBranch = false;
						}
					}
				}
			}

			if (possibleNodeTypesToBeInserted.isEmpty()) {
				// openSplitStack is not empty && there are no NodeTypes left to be inserted
				// -> open split needs to be closed
				callFinishCurrentBranch = true;
			}

			if (callFinishCurrentBranch) {
				boolean addJoin = finishCurrentBranch(possibleNodeTypesToBeInserted, branchingFactor, openSplitStack);

				if (addJoin) {
					// add a join to the last opened split
					String joinName = lastOpenedSplit.getName();
					String joinId = joinName + "_join";
					if (this.modelInstance.getModelElementById(joinId) == null) {
						if (lastOpenedSplit instanceof ExclusiveGateway) {
							nextNode = new InsertionConstruct(joinId, joinName, "ExclusiveGateway");
						} else if (lastOpenedSplit instanceof ParallelGateway) {
							nextNode = new InsertionConstruct(joinId, joinName, "ParallelGateway");
						}
						this.insertionConstructs.add(nextNode);

					} else {
						// if the join has already been added to the model
						nextNode = this.getInsertionConstruct(joinId);

					}

				}
				if (nextNode != null) {
					return nextNode;
				}
			}
		}

		int randomInt = 0;
		boolean insert = false;
		while (insert == false) {
			randomInt = this.getRandomInt(0, 99);
			if (randomInt >= taskProbArray[0] && randomInt <= taskProbArray[1]
					&& possibleNodeTypesToBeInserted.contains("Task")) {
				insert = true;
			} else if (randomInt >= xorProbArray[0] && randomInt <= xorProbArray[1]
					&& possibleNodeTypesToBeInserted.contains("ExclusiveGateway")) {
				insert = true;
			} else if (randomInt >= parallelProbArray[0] && randomInt <= parallelProbArray[1]
					&& possibleNodeTypesToBeInserted.contains("ParallelGateway")) {
				insert = true;
			}

		}

		if (randomInt >= taskProbArray[0] && randomInt <= taskProbArray[1]) {
			// create a task
			// randomly add a participant to the name of the task
			if (possibleNodeTypesToBeInserted.contains("Task")) {
				Random rand = new Random();
				String uniqueTaskId = "task_" + taskId;
				String participantName = this.participantNames.get(rand.nextInt(participantNames.size()));
				String taskName = uniqueTaskId + " [" + participantName + "]";
				nextNode = new InsertionConstruct(uniqueTaskId, taskName, "Task");
				this.insertionConstructs.add(nextNode);
				taskId++;
			}
		} else if (randomInt >= xorProbArray[0] && randomInt <= xorProbArray[1]) {
			if (amountXorsLeft > 0 && amountTasksLeft >= 1
					&& possibleNodeTypesToBeInserted.contains("ExclusiveGateway")) {
				// create new xor-split if there is at least a task left to be inserted

				String name = "exclusiveGateway_" + xorGtwId;
				String uniqueXorGtwIdSplit = "exclusiveGateway_" + xorGtwId + "_split";

				nextNode = new InsertionConstruct(uniqueXorGtwIdSplit, name, "ExclusiveGateway");
				this.insertionConstructs.add(nextNode);
				xorGtwId++;
			}
		} else if (randomInt >= parallelProbArray[0] && randomInt <= parallelProbArray[1]) {
			// create new parallel-split
			if (amountParallelsLeft > 0 && amountTasksLeft >= 2
					&& possibleNodeTypesToBeInserted.contains("ParallelGateway")) {
				String name = "parallelGateway_" + parallelGtwId;
				String uniqueParallelGtwIdSplit = "parallelGateway_" + parallelGtwId + "_split";
				nextNode = new InsertionConstruct(uniqueParallelGtwIdSplit, name, "ParallelGateway");
				this.insertionConstructs.add(nextNode);
				parallelGtwId++;
			}
		}

		return nextNode;
	}

	private InsertionConstruct getInsertionConstruct(String id) {
		for (InsertionConstruct ic : this.insertionConstructs) {
			if (ic.getId().contentEquals(id)) {
				return ic;
			}
		}
		return null;
	}

	private boolean finishCurrentBranch(LinkedList<String> possibleNodeTypesToBeInserted, int branchingFactor,
			LinkedList<FlowNode> openXorStack) {
		// possibility to finish current branch is increased with nesting depth
		// nesting depth is the amount of openXors on the stack

		if (possibleNodeTypesToBeInserted.isEmpty()) {
			return true;
		}

		int randomInt = this.getRandomInt(0, 99);

		if (branchingFactor >= randomInt) {
			return true;
		}

		return false;

	}


	private int getRandomInt(int min, int max) {
		int randomInt = ThreadLocalRandom.current().nextInt(min, max + 1);
		return randomInt;
	}

	private File writeChangesToFile() throws NullPointerException, Exception {
		// validate and write model to file
		// add the generated models to the given directory

		if (!CommonFunctionality.isModelBlockStructured(this.modelInstance)) {
			throw new Exception("Model not block structured!");
		}

		Bpmn.validateModel(this.modelInstance);
		String pathOfProcessFile = this.directoryToStore;
		String fileName = "randomProcessModel" + this.processId + ".bpmn";

		File file = CommonFunctionality.createFileWithinDirectory(pathOfProcessFile, fileName);

		System.out.println("FileCreated: " + file.createNewFile());

		Bpmn.writeModelToFile(file, this.modelInstance);

		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		docBuilderFactory.setNamespaceAware(true);
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document document = docBuilder.parse(file);

		// to change the manual Tasks into normal tasks
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

			FileOutputStream outputStream = new FileOutputStream(file, false);
			PrintWriter printWriter = new PrintWriter(outputStream);
			StreamResult result = new StreamResult(printWriter);
			DOMSource source = new DOMSource(document);
			transformer.transform(source, result);
			outputStream.close();
			printWriter.close();

		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return file;
	}

	@Override
	public File call() throws Exception {
		// TODO Auto-generated method stub
		while (!Thread.currentThread().isInterrupted()) {
			this.goDfs(this.startEvent, null, amountTasksLeft, amountXorsLeft, amountParallelsLeft,
					this.possibleNodeTypes, new LinkedList<FlowNode>(), new LinkedList<FlowNode>(), nestingDepthFactor);
			double exclusiveGatewaysGenerated = this.modelInstance.getModelElementsByType(ExclusiveGateway.class)
					.size();
			if (exclusiveGatewaysGenerated / 2 >= this.minXorSplits) {
				try {
					File f = writeChangesToFile();
					if (f != null) {
						return f;
					}
				} catch (Exception e) {
					System.out.println(e.getMessage());
					System.out.println("Generated Model not valid! Trying again!");

				}

			}
			// if no valid model has been generated -> try generating a new one
			this.clearCurrentModelInstance();
		}

		return null;
	}

	public void clearCurrentModelInstance() {
		this.modelInstance = Bpmn.createProcess("Process_" + run).startEvent("startEvent_1").done();
		this.startEvent = (FlowNode) modelInstance.getModelElementById("startEvent_1");
		this.insertionConstructs = new LinkedList<InsertionConstruct>();
	}

	public static void decreaseProcessGeneratorId() {
		processGeneratorId--;
	}
	

}
