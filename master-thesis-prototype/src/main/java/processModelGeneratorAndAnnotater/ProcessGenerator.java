package processModelGeneratorAndAnnotater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

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
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Gateway;
import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import functionality.CommonFunctionality;
import processModelGeneratorAndAnnotater.ProcessGeneratorEnum.NodeType;

public class ProcessGenerator implements Callable<File> {

	private static int processGeneratorId = 1;
	private int processId;
	private static int run = 1;
	private BpmnModelInstance modelInstance;
	private String directoryToStore;
	private int taskId;
	private int xorGtwId;
	private int parallelGtwId;
	private FlowNode startEvent;
	private int amountParticipants;
	private LinkedList<String> participantNames;
	private LinkedList<String> participantsUsed;
	private int amountTasksToBeInserted;
	private int amountXorsToBeInserted;
	private int amountParallelsToBeInserted;
	private LinkedList<ProcessGeneratorEnum.NodeType> possibleNodeTypes;
	private LinkedList<LinkedList<FlowNode>> allPaths;
	private int probabilityForJoinGtw;
	private int nestingDepthFactor;
	private HashMap<NodeType, Integer[]> percentagesForNodesToBeDrawn;
	private boolean taskAsFirstElementAfterStart;
	private boolean allowNestedXors;

	public ProcessGenerator(String directoryToStore, int amountParticipants, int amountTasksToBeInserted,
			int amountXorsToBeInserted, int amountParallelsToBeInserted, int taskProb, int xorSplitProb,
			int parallelSplitProb, int probabilityForJoinGtw, int nestingDepthFactor,
			boolean taskAsFirstElementAfterStart, boolean allowNestedXors) throws Exception {
		// process model will have 1 StartEvent and 1 EndEvent
		if (taskProb + xorSplitProb + parallelSplitProb > 100) {
			throw new Exception("taskProb+xorSplitProb+parallelSplitProb!=100");
		}
		if (amountParticipants > amountTasksToBeInserted) {
			throw new Exception("amountParticipants can not be > amountTasks");
		}
		this.processId = processGeneratorId++;
		this.directoryToStore = directoryToStore;

		this.participantNames = new LinkedList<String>();
		for (int i = 0; i < amountParticipants; i++) {
			String participantName = "Participant_" + (i + 1);
			participantNames.add(participantName);
		}
		this.participantsUsed = new LinkedList<String>();
		this.amountParticipants = amountParticipants;
		this.amountTasksToBeInserted = amountTasksToBeInserted;
		this.amountXorsToBeInserted = amountXorsToBeInserted;
		this.amountParallelsToBeInserted = amountParallelsToBeInserted;
		this.taskId = 1;
		this.xorGtwId = 1;
		this.parallelGtwId = 1;
		this.modelInstance = Bpmn.createProcess("Process_" + run).startEvent("startEvent_1").done();
		this.startEvent = (FlowNode) modelInstance.getModelElementById("startEvent_1");
		this.possibleNodeTypes = new LinkedList<ProcessGeneratorEnum.NodeType>();
		this.probabilityForJoinGtw = probabilityForJoinGtw;
		this.nestingDepthFactor = nestingDepthFactor;

		// generator will insert a task directly after start event
		this.taskAsFirstElementAfterStart = taskAsFirstElementAfterStart;

		// will only add tasks or parallel branches containing tasks inside a xor-branch
		// if set to false -> there will not be a xor nested inside another xor
		this.allowNestedXors = allowNestedXors;

		if (amountTasksToBeInserted > 0) {
			this.possibleNodeTypes.add(NodeType.TASK);
		}
		if (amountXorsToBeInserted > 0) {
			this.possibleNodeTypes.add(NodeType.XOR_SPLIT);
		}
		if (amountParallelsToBeInserted > 0) {
			this.possibleNodeTypes.add(NodeType.PARALLEL_SPLIT);
		}

		this.allPaths = new LinkedList<LinkedList<FlowNode>>();
		// create starting path for generate process method
		LinkedList<FlowNode> startingPoint = new LinkedList<FlowNode>();
		startingPoint.add(this.startEvent);
		this.allPaths.add(startingPoint);

		this.percentagesForNodesToBeDrawn = new HashMap<NodeType, Integer[]>();
		Integer[] taskProbsArray = new Integer[2];
		taskProbsArray[0] = 0;
		taskProbsArray[1] = taskProb - 1;
		this.percentagesForNodesToBeDrawn.putIfAbsent(NodeType.TASK, taskProbsArray);

		Integer[] xorProbsArray = new Integer[2];
		xorProbsArray[0] = taskProb;
		xorProbsArray[1] = (taskProb + xorSplitProb - 1);
		this.percentagesForNodesToBeDrawn.putIfAbsent(NodeType.XOR_SPLIT, xorProbsArray);

		Integer[] parallelProbsArray = new Integer[2];
		parallelProbsArray[0] = taskProb + xorSplitProb;
		parallelProbsArray[1] = (taskProb + xorSplitProb + parallelSplitProb - 1);
		this.percentagesForNodesToBeDrawn.putIfAbsent(NodeType.PARALLEL_SPLIT, parallelProbsArray);
	}

	private void generateProcess(LinkedList<LinkedList<FlowNode>> allPaths, LinkedList<NodeType> possibleNodeTypes,
			int amountTasksToBeInsertedLeft, int amountXorsToBeInsertedLeft, int amountParallelsToBeInsertedLeft)
			throws InterruptedException {

		if (Thread.currentThread().isInterrupted()) {
			System.err.println("Interrupted! " + Thread.currentThread().getName());
			throw new InterruptedException();
		}
		boolean pathToBeAdded = true;
		LinkedList<FlowNode> openGatewayStack = new LinkedList<FlowNode>();
		// get a random path where EndEvent has not been added
		LinkedList<FlowNode> path = new LinkedList<FlowNode>();
		path.addAll(this.getRandomPathNotFullyBuilt(allPaths));
		allPaths.remove(path);
		if (path.isEmpty()) {
			return;
		}

		FlowNode currentEntryPoint = path.getLast();
		for (FlowNode f : path) {
			if (f instanceof Gateway) {
				if (f.getId().contains("_split")) {
					openGatewayStack.add(f);
				}
				if (f.getId().contains("_join")) {
					openGatewayStack.pollLast();
				}
			}
		}

		LinkedList<NodeType> nodeTypesToBeInserted = this.computeNodesToBeInserted(amountTasksToBeInsertedLeft,
				amountXorsToBeInsertedLeft, amountParallelsToBeInsertedLeft, currentEntryPoint, openGatewayStack,
				allPaths);

		int branchingFactor = this.probabilityForJoinGtw + (openGatewayStack.size() * this.nestingDepthFactor);
		if (branchingFactor > 100) {
			branchingFactor = 100;
		}

		boolean splitAdded = false;
		InsertionConstruct nextConstructToBeAdded = this.getNextNodeToAdd(amountTasksToBeInsertedLeft, branchingFactor,
				nodeTypesToBeInserted, openGatewayStack, path);

		FlowNode addedNode = null;
		// append the nextNodeToBeAdded to the currentNode
		if (nextConstructToBeAdded != null) {
			if (nextConstructToBeAdded.getType().contentEquals("Task")) {
				currentEntryPoint.builder().manualTask(nextConstructToBeAdded.getId())
						.name(nextConstructToBeAdded.getName());
				amountTasksToBeInsertedLeft--;
			} else if (nextConstructToBeAdded.getType().contains("ExclusiveGateway")) {
				ExclusiveGateway xorGtwToBeAdded = this.modelInstance
						.getModelElementById(nextConstructToBeAdded.getId());
				if (xorGtwToBeAdded == null) {
					currentEntryPoint.builder().exclusiveGateway(nextConstructToBeAdded.getId())
							.name(nextConstructToBeAdded.getName());
				}
				if (nextConstructToBeAdded.getType().contentEquals("ExclusiveGateway_split")) {
					amountXorsToBeInsertedLeft--;
					splitAdded = true;
				} else if (nextConstructToBeAdded.getType().contentEquals("ExclusiveGateway_join")) {
					if (xorGtwToBeAdded != null) {
						currentEntryPoint.builder().connectTo(xorGtwToBeAdded.getId());
						// do not add this path as entry point
						pathToBeAdded = false;
					}
				}

			} else if (nextConstructToBeAdded.getType().contains("ParallelGateway")) {
				ParallelGateway pGtwToBeAdded = this.modelInstance.getModelElementById(nextConstructToBeAdded.getId());
				if (pGtwToBeAdded == null) {
					currentEntryPoint.builder().parallelGateway(nextConstructToBeAdded.getId())
							.name(nextConstructToBeAdded.getName());
				}
				if (nextConstructToBeAdded.getType().contentEquals("ParallelGateway_split")) {
					amountParallelsToBeInsertedLeft--;
					splitAdded = true;
				} else if (nextConstructToBeAdded.getType().contentEquals("ParallelGateway_join")) {
					if (pGtwToBeAdded != null) {
						currentEntryPoint.builder().connectTo(pGtwToBeAdded.getId());
						// do not add this path as entry point
						// if there is already one with the same join
						pathToBeAdded = false;
					}
				}

			} else if (nextConstructToBeAdded.getType().contains("EndEvent")) {
				EndEvent endEvent = this.modelInstance.getModelElementById(nextConstructToBeAdded.getId());
				if (endEvent == null) {
					currentEntryPoint.builder().endEvent(nextConstructToBeAdded.getId())
							.name(nextConstructToBeAdded.getName());
				}
			}
			addedNode = this.modelInstance.getModelElementById(nextConstructToBeAdded.getId());
		}
		LinkedList<LinkedList<FlowNode>> newPaths = new LinkedList<LinkedList<FlowNode>>();
		newPaths.addAll(allPaths);
		if (addedNode != null) {
			path.add(addedNode);
			if (!(path.getLast() instanceof EndEvent)) {
				if (pathToBeAdded) {
					newPaths.add(path);
					if (splitAdded) {
						// new object of that path will be added again since there can be 2 branches
						LinkedList<FlowNode> samePathNewObject = new LinkedList<FlowNode>();
						samePathNewObject.addAll(path);
						newPaths.add(samePathNewObject);
					}
				}
			}
		}
		if (newPaths.isEmpty()) {
			return;
		}

		this.generateProcess(newPaths, nodeTypesToBeInserted, amountTasksToBeInsertedLeft, amountXorsToBeInsertedLeft,
				amountParallelsToBeInsertedLeft);

	}

	private LinkedList<FlowNode> getRandomPathNotFullyBuilt(LinkedList<LinkedList<FlowNode>> currentPaths) {
		LinkedList<LinkedList<FlowNode>> pathsToChooseFrom = new LinkedList<LinkedList<FlowNode>>();
		LinkedList<LinkedList<FlowNode>> pathsToPrefer = new LinkedList<LinkedList<FlowNode>>();

		for (LinkedList<FlowNode> currentPath : currentPaths) {
			FlowNode lastNode = currentPath.getLast();
			if (!(lastNode instanceof EndEvent)) {
				pathsToChooseFrom.add(currentPath);
				if (lastNode instanceof Gateway) {
					if (lastNode.getId().contains("_split")) {
						// lastNode is either a xor-split or a parallel-split
						if (lastNode instanceof ExclusiveGateway) {
							// if it is a xor-split -> add the current path if there is no outgoing branch
							if (lastNode.getOutgoing().size() == 0) {
								pathsToPrefer.add(currentPath);
							}
						} else if (lastNode instanceof ParallelGateway) {
							// if it is a parallel-split -> add the current path since it does not contain
							// an element on this branch yet
							pathsToPrefer.add(currentPath);
						}

					}
				}
			}
		}

		if (!pathsToPrefer.isEmpty()) {
			int randomNum = ThreadLocalRandom.current().nextInt(0, pathsToPrefer.size());
			return pathsToPrefer.get(randomNum);
		}

		if (pathsToChooseFrom.isEmpty()) {
			return new LinkedList<FlowNode>();
		}
		int randomNum = ThreadLocalRandom.current().nextInt(0, pathsToChooseFrom.size());
		return pathsToChooseFrom.get(randomNum);

	}

	private LinkedList<NodeType> computeNodesToBeInserted(int amountTasksLeft, int amountXorsLeft,
			int amountParallelsLeft, FlowNode currentEntryPoint, LinkedList<FlowNode> openGtwStack,
			LinkedList<LinkedList<FlowNode>> paths) {
		LinkedList<NodeType> nodeList = new LinkedList<NodeType>();

		if (currentEntryPoint instanceof StartEvent) {
			if (this.taskAsFirstElementAfterStart) {
				// task will be inserted
				if (amountTasksLeft > 0) {
					nodeList.add(NodeType.TASK);
					return nodeList;
				}
			}
		}

		if (amountXorsLeft > 0) {
			nodeList.add(NodeType.XOR_SPLIT);
		}

		if (amountParallelsLeft > 0) {
			nodeList.add(NodeType.PARALLEL_SPLIT);
		}

		if (amountTasksLeft <= 0) {
			// no tasks can be inserted anymore
			nodeList.remove(NodeType.PARALLEL_SPLIT);
			nodeList.remove(NodeType.XOR_SPLIT);
			nodeList.remove(NodeType.TASK);
		}

		if (amountTasksLeft > 0) {
			nodeList.add(NodeType.TASK);
		}

		if (!this.allowNestedXors) {
			for (FlowNode openGtw : openGtwStack) {
				if (openGtw instanceof ExclusiveGateway) {
					// if an open xor-split is on the path
					ExclusiveGateway currentGtw = (ExclusiveGateway) openGtw;
					String idOfCurrentGtw = currentGtw.getId();
					if (idOfCurrentGtw.contains("_split")) {
						// only tasks or parallels are possible inside
						nodeList.remove(NodeType.XOR_SPLIT);
						break;
					}
				}
			}
		}

		if (!openGtwStack.isEmpty()) {
			FlowNode lastOpenedSplit = openGtwStack.getLast();
			boolean addJoin = false;
			Collection<SequenceFlow> lastOpenedSplitOutgoing = lastOpenedSplit.getOutgoing();

			if (lastOpenedSplit instanceof ExclusiveGateway) {
				// there must be an element in at least 1 branch
				if (lastOpenedSplitOutgoing.size() >= 1) {
					addJoin = true;
				}

			} else if (lastOpenedSplit instanceof ParallelGateway) {
				// in each branch must be at least 1 element
				if (lastOpenedSplitOutgoing.size() == 2) {
					addJoin = true;
				}
			}

			if (lastOpenedSplit instanceof ExclusiveGateway && addJoin) {
				nodeList.add(NodeType.XOR_JOIN);
			} else if (lastOpenedSplit instanceof ParallelGateway && addJoin) {
				nodeList.add(NodeType.PARALLEL_JOIN);
			}
		}

		return nodeList;

	}

	private InsertionConstruct getNextNodeToAdd(int amountTasksToBeInsertedLeft, int branchingFactor,
			LinkedList<NodeType> possibleNodeTypes, LinkedList<FlowNode> lastOpenedGtws, LinkedList<FlowNode> path) {

		InsertionConstruct nextNode = null;
		if (possibleNodeTypes.isEmpty()) {
			nextNode = new InsertionConstruct("endEvent_1", "", "EndEvent");
			return nextNode;
		}
		NodeType nodeType = this.getRandomNode(branchingFactor, possibleNodeTypes);
		if (nodeType.equals(NodeType.PARALLEL_JOIN)) {
			ParallelGateway lastOpenedParallelSplit = (ParallelGateway) lastOpenedGtws.getLast();
			String joinName = lastOpenedParallelSplit.getName();
			String joinId = joinName + "_join";
			nextNode = new InsertionConstruct(joinId, joinName, "ParallelGateway_join");
		} else if (nodeType.equals(NodeType.XOR_JOIN)) {
			ExclusiveGateway lastOpenedXorSplit = (ExclusiveGateway) lastOpenedGtws.getLast();
			String joinName = lastOpenedXorSplit.getName();
			String joinId = joinName + "_join";
			nextNode = new InsertionConstruct(joinId, joinName, "ExclusiveGateway_join");
		} else if (nodeType.equals(NodeType.XOR_SPLIT)) {
			String name = "exclusiveGtw_" + this.xorGtwId;
			String uniqueXorGtwIdSplit = "exclusiveGtw_" + this.xorGtwId + "_split";
			nextNode = new InsertionConstruct(uniqueXorGtwIdSplit, name, "ExclusiveGateway_split");
			this.xorGtwId++;
		} else if (nodeType.equals(NodeType.PARALLEL_SPLIT)) {
			String name = "parallelGtw_" + parallelGtwId;
			String uniqueParallelGtwIdSplit = "parallelGtw_" + this.parallelGtwId + "_split";
			nextNode = new InsertionConstruct(uniqueParallelGtwIdSplit, name, "ParallelGateway_split");
			this.parallelGtwId++;
		} else if (nodeType.equals(NodeType.TASK)) {
			String uniqueTaskId = "task_" + this.taskId;
			String participantName = "";
			Random rand = new Random();
			int participantsNotInserted = this.participantNames.size() - this.participantsUsed.size();
			if (amountTasksToBeInsertedLeft <= participantsNotInserted) {
				LinkedList<String> availableNames = new LinkedList<String>();
				availableNames.addAll(this.participantNames);
				availableNames.removeAll(this.participantsUsed);
				participantName = availableNames.get(rand.nextInt(availableNames.size()));
			} else {
				participantName = this.participantNames.get(rand.nextInt(participantNames.size()));
			}
			String taskName = uniqueTaskId + " [" + participantName + "]";
			nextNode = new InsertionConstruct(uniqueTaskId, taskName, "Task");
			this.taskId++;
			if (!this.participantsUsed.contains(participantName)) {
				this.participantsUsed.add(participantName);
			}
		}

		return nextNode;
	}

	private File writeChangesToFile(boolean testForMinElementsInBranches) throws NullPointerException, Exception {
		// validate and write model to file
		// add the generated models to the given directory
		if (!CommonFunctionality.isModelBlockStructured(this.modelInstance)) {
			throw new Exception("Model not block structured!");
		}

		for (Task task : this.modelInstance.getModelElementsByType(Task.class)) {
			if (task.getIncoming().size() > 1) {
				throw new Exception("Task can not have > 1 incoming edges");
			}
		}

		if (testForMinElementsInBranches) {
			// check if parallel branches have at least 1 element
			// check if one of both xor branches has at least 1 element
			if (!CommonFunctionality.testGatewaysForElements(this.modelInstance)) {
				throw new Exception("Branches have not the minimum amount of elements before join!");
			}
		}

		Bpmn.validateModel(this.modelInstance);
		String pathOfProcessFile = this.directoryToStore;
		String fileName = "randomProcessModel" + this.processId + ".bpmn";

		File file = CommonFunctionality.createFileWithinDirectory(pathOfProcessFile, fileName);

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

	private NodeType getRandomNode(int branchingFactor, List<NodeType> possibleNodesToChooseFrom) {
		NodeType nodeToReturn = null;
		if (possibleNodesToChooseFrom.size() == 1) {
			return possibleNodesToChooseFrom.get(0);
		}

		// check with branchingFactor if join will be added
		for (NodeType nodeType : possibleNodesToChooseFrom) {
			if (nodeType.equals(NodeType.XOR_JOIN) || nodeType.equals(NodeType.PARALLEL_JOIN)) {
				int randomInt = ThreadLocalRandom.current().nextInt(0, 100);
				if (this.probabilityForJoinGtw >= randomInt) {
					// join will be added
					return nodeType;
				}
				break;
			}
		}

		while (nodeToReturn == null) {
			int randomInt = ThreadLocalRandom.current().nextInt(0, 100);
			for (Entry<NodeType, Integer[]> entry : this.percentagesForNodesToBeDrawn.entrySet()) {
				NodeType currNodeType = entry.getKey();
				Integer[] currProbs = entry.getValue();
				if (randomInt >= currProbs[0] && randomInt <= currProbs[1]
						&& possibleNodesToChooseFrom.contains(currNodeType)) {
					return currNodeType;
				}
			}

		}
		return null;
	}

	public static void decreaseProcessGeneratorId() {
		processGeneratorId--;
	}

	public int getProcessId() {
		return this.processId;
	}

	@Override
	public File call() throws Exception {
		// TODO Auto-generated method stub

		this.generateProcess(this.allPaths, this.possibleNodeTypes, this.amountTasksToBeInserted,
				this.amountXorsToBeInserted, this.amountParallelsToBeInserted);

		int xorSplits = CommonFunctionality.getAmountExclusiveGtwSplits(this.modelInstance);
		int parallelGtwsGenerated = CommonFunctionality.getAmountParallelGtwSplits(this.modelInstance);
		int amountTasks = this.modelInstance.getModelElementsByType(Task.class).size();
		int privateSphere = CommonFunctionality.getPrivateSphere(this.modelInstance, false);

		if (xorSplits == this.amountXorsToBeInserted && parallelGtwsGenerated == this.amountParallelsToBeInserted
				&& amountTasks == this.amountTasksToBeInserted && privateSphere == this.amountParticipants) {
			try {
				File f = writeChangesToFile(true);
				if (f != null) {
					System.out.println("File written: " + f.getAbsolutePath());
					return f;
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		} else {
			System.out.println("Model did not satisfy specified amounts!");
		}
		return null;

	}

}
