package ProcessModelGeneratorAndAnnotater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
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
import org.camunda.bpm.model.bpmn.instance.Task;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import functionality.CommonFunctionality;

public class ProcessGenerator implements Callable {

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
	private int amountTasksToBeInserted;
	private int amountXorsToBeInserted;
	private int amountParallelsToBeInserted;
	private LinkedList<String> possibleNodeTypes;
	private LinkedList<LinkedList<FlowNode>> allPaths;
	private int probabilityForJoinGtw;
	private int nestingDepthFactor;
	private HashMap<String, Integer[]> percentagesForNodesToBeDrawn;

	public ProcessGenerator(String directoryToStore, int amountParticipants, int amountTasksToBeInserted,
			int amountXorsToBeInserted, int amountParallelsToBeInserted, int taskProb, int xorSplitProb,
			int parallelSplitProb, int probabilityForJoinGtw, int nestingDepthFactor) throws Exception {
		// process model will have 1 StartEvent and 1 EndEvent
		if (taskProb + xorSplitProb + parallelSplitProb > 100) {
			throw new Exception("taskProb+xorSplitProb+parallelSplitProb!=100");
		}
		this.processId = processGeneratorId++;
		this.directoryToStore = directoryToStore;

		this.participantNames = new LinkedList<String>();
		for (int i = 0; i < amountParticipants; i++) {
			String participantName = "Participant_" + (i + 1);
			participantNames.add(participantName);
		}

		this.amountParticipants = amountParticipants;
		this.amountTasksToBeInserted = amountTasksToBeInserted;
		this.amountXorsToBeInserted = amountXorsToBeInserted;
		this.amountParallelsToBeInserted = amountParallelsToBeInserted;
		this.taskId = 1;
		this.xorGtwId = 1;
		this.parallelGtwId = 1;
		this.modelInstance = Bpmn.createProcess("Process_" + run).startEvent("startEvent_1").done();
		this.startEvent = (FlowNode) modelInstance.getModelElementById("startEvent_1");
		this.possibleNodeTypes = new LinkedList<String>();
		this.probabilityForJoinGtw = probabilityForJoinGtw;
		this.nestingDepthFactor = nestingDepthFactor;
		if (amountTasksToBeInserted > 0) {
			this.possibleNodeTypes.add("Task");
		}
		if (amountXorsToBeInserted > 0) {
			this.possibleNodeTypes.add("ExclusiveGateway_split");
		}
		if (amountParallelsToBeInserted > 0) {
			this.possibleNodeTypes.add("ParallelGateway_split");
		}

		this.allPaths = new LinkedList<LinkedList<FlowNode>>();
		// create starting path for generate process method
		LinkedList<FlowNode> startingPoint = new LinkedList<FlowNode>();
		startingPoint.add(this.startEvent);
		this.allPaths.add(startingPoint);

		this.percentagesForNodesToBeDrawn = new HashMap<String, Integer[]>();
		Integer[] taskProbsArray = new Integer[2];
		taskProbsArray[0] = 0;
		taskProbsArray[1] = taskProb - 1;
		this.percentagesForNodesToBeDrawn.putIfAbsent("Task", taskProbsArray);

		Integer[] xorProbsArray = new Integer[2];
		xorProbsArray[0] = taskProb;
		xorProbsArray[1] = (taskProb + xorSplitProb - 1);
		this.percentagesForNodesToBeDrawn.putIfAbsent("ExclusiveGateway_split", xorProbsArray);

		Integer[] parallelProbsArray = new Integer[2];
		parallelProbsArray[0] = taskProb + xorSplitProb;
		parallelProbsArray[1] = (taskProb + xorSplitProb + parallelSplitProb - 1);
		this.percentagesForNodesToBeDrawn.putIfAbsent("ParallelGateway_split", parallelProbsArray);
	}

	private void generateProcess(LinkedList<LinkedList<FlowNode>> allPaths, LinkedList<String> possibleNodeTypes,
			int amountTasksToBeInsertedLeft, int amountXorsToBeInsertedLeft, int amountParallelsToBeInsertedLeft) {

		boolean pathToBeAdded = true;
		LinkedList<FlowNode> openGatewayStack = new LinkedList<FlowNode>();
		LinkedList<FlowNode> openXorSplitStack = new LinkedList<FlowNode>();
		// get a random path where EndEvent has not been added)
		int in = 1;

		System.out.println("TEST");
		for (LinkedList<FlowNode> p : allPaths) {
			System.out.println("***********************");
			System.out.println("path " + in);
			for (FlowNode f : p) {
				System.out.println(f.getName());
			}
			in++;
		}

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
					openXorSplitStack.add(f);
				}
				if (f.getId().contains("_join")) {
					openGatewayStack.pollLast();
					if (f instanceof ExclusiveGateway) {
						openXorSplitStack.pollLast();
					}
				}
			}
		}

		LinkedList<String> nodeTypesToBeInserted = this.computeNodesToBeInserted(amountTasksToBeInsertedLeft,
				amountXorsToBeInsertedLeft, amountParallelsToBeInsertedLeft, currentEntryPoint, openGatewayStack);

		int branchingFactor = this.probabilityForJoinGtw + (openGatewayStack.size() * this.nestingDepthFactor);

		boolean splitAdded = false;
		InsertionConstruct nextConstructToBeAdded = this.getNextNodeToAdd(branchingFactor, nodeTypesToBeInserted,
				openGatewayStack, path);

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
						pathToBeAdded = false;
					}
				}

			} else if (nextConstructToBeAdded.getType().contains("EndEvent")) {
				EndEvent endEvent = this.modelInstance.getModelElementById(nextConstructToBeAdded.getId());
				if (endEvent == null) {
					currentEntryPoint.builder().endEvent(nextConstructToBeAdded.getId())
							.name(nextConstructToBeAdded.getName());
				} else {
					currentEntryPoint.builder().connectTo(endEvent.getId());
				}
			}
			addedNode = this.modelInstance.getModelElementById(nextConstructToBeAdded.getId());
		}

		LinkedList<LinkedList<FlowNode>> newPaths = new LinkedList<LinkedList<FlowNode>>();
		newPaths.addAll(allPaths);
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
		if (newPaths.isEmpty()) {
			return;
		}

		this.generateProcess(newPaths, nodeTypesToBeInserted, amountTasksToBeInsertedLeft, amountXorsToBeInsertedLeft,
				amountParallelsToBeInsertedLeft);

	}

	private LinkedList<FlowNode> getRandomPathNotFullyBuilt(LinkedList<LinkedList<FlowNode>> currentPaths) {
		LinkedList<LinkedList<FlowNode>> pathsToChooseFrom = new LinkedList<LinkedList<FlowNode>>();
		for (LinkedList<FlowNode> currentPath : currentPaths) {
			if (!(currentPath.getLast() instanceof EndEvent)) {
				pathsToChooseFrom.add(currentPath);
			}
		}
		if (pathsToChooseFrom.isEmpty()) {
			return new LinkedList<FlowNode>();
		}
		int randomNum = ThreadLocalRandom.current().nextInt(0, pathsToChooseFrom.size());
		return pathsToChooseFrom.get(randomNum);
	}

	private LinkedList<String> computeNodesToBeInserted(int amountTasksLeft, int amountXorsLeft,
			int amountParallelsLeft, FlowNode currentEntryPoint, LinkedList<FlowNode> openGtwStack) {
		// check if nodeTypes can still be inserted e.g. a xor-split can only be
		// inserted if there is still at least a flowNode to be inserted
		LinkedList<String> nodeList = new LinkedList<String>();

		if (!openGtwStack.isEmpty()) {
			if (currentEntryPoint.equals(openGtwStack.getLast())) {
				// currentEntryPoint is a split
				if (currentEntryPoint instanceof ParallelGateway) {
					// there must be an element after parallel split in both branches
					// in order for the join to be added
				} else if (currentEntryPoint instanceof ExclusiveGateway) {
					// there must be an element on one of both xor-splits branches before adding the
					// join
					if (currentEntryPoint.getOutgoing().size() >= 1) {
						nodeList.add("ExclusiveGateway_join");
					}
				}

			} else {
				// currentEntryPoint is in branch
				FlowNode lastOpenedSplit = openGtwStack.getLast();
				if (lastOpenedSplit instanceof ParallelGateway) {
					nodeList.add("ParallelGateway_join");
				} else if (lastOpenedSplit instanceof ExclusiveGateway) {
					nodeList.add("ExclusiveGateway_join");
				}
			}
		}

		if (amountXorsLeft > 0) {
			nodeList.add("ExclusiveGateway_split");
		}

		if (amountParallelsLeft > 0) {
			nodeList.add("ParallelGateway_split");
		}

		if (amountTasksLeft <= 0) {
			// no tasks can be inserted anymore
			// no parallel and xor splits can be inserted anymore too
			nodeList.remove("ParallelGateway_split");
			nodeList.remove("ExclusiveGateway_split");
			nodeList.remove("Task");
		}

		if (amountTasksLeft > 0) {
			nodeList.add("Task");
		}
		return nodeList;

	}

	private InsertionConstruct getNextNodeToAdd(int branchingFactor, LinkedList<String> possibleNodeTypes,
			LinkedList<FlowNode> lastOpenedGtws, LinkedList<FlowNode> path) {

		InsertionConstruct nextNode = null;
		if (possibleNodeTypes.isEmpty()) {
			nextNode = new InsertionConstruct("endEvent_1", "", "EndEvent");
			return nextNode;
		}

		if(this.modelInstance.getModelElementById("endEvent_1")==null) {
			if(possibleNodeTypes.size()==1&&possibleNodeTypes.get(0).contentEquals("Task")) {
				//if there are only tasks available for this path
				//and endEvent has not been visited
				//randomly add task or the endEvent
				int randomNum = ThreadLocalRandom.current().nextInt(0, 99);
				Integer[]taskProbs = this.percentagesForNodesToBeDrawn.get("Task");
				if(randomNum>=taskProbs[0]&&randomNum<=taskProbs[1]) {
					//task chosen
					Random rand = new Random();
					String uniqueTaskId = "task_" + this.taskId;
					String participantName = this.participantNames.get(rand.nextInt(participantNames.size()));
					String taskName = uniqueTaskId + " [" + participantName + "]";
					nextNode = new InsertionConstruct(uniqueTaskId, taskName, "Task");
					this.taskId++;
				} else {
					//endEvent chosen
					nextNode = new InsertionConstruct("endEvent_1", "", "EndEvent");
					return nextNode;
				}				
			}
			
		}
		
		// String nodeType = this.getRandomItem(possibleNodeTypes);
		String nodeType = this.getRandomNode(branchingFactor, possibleNodeTypes);
		if (nodeType.contentEquals("ParallelGateway_join")) {
			ParallelGateway lastOpenedParallelSplit = (ParallelGateway) lastOpenedGtws.getLast();
			String joinName = lastOpenedParallelSplit.getName();
			String joinId = joinName + "_join";
			nextNode = new InsertionConstruct(joinId, joinName, "ParallelGateway_join");
		} else if (nodeType.contentEquals("ExclusiveGateway_join")) {
			ExclusiveGateway lastOpenedXorSplit = (ExclusiveGateway) lastOpenedGtws.getLast();
			String joinName = lastOpenedXorSplit.getName();
			String joinId = joinName + "_join";
			nextNode = new InsertionConstruct(joinId, joinName, "ExclusiveGateway_join");
		} else if (nodeType.contentEquals("ExclusiveGateway_split")) {
			String name = "exclusiveGateway_" + this.xorGtwId;
			String uniqueXorGtwIdSplit = "exclusiveGateway_" + this.xorGtwId + "_split";
			nextNode = new InsertionConstruct(uniqueXorGtwIdSplit, name, "ExclusiveGateway_split");
			this.xorGtwId++;
		} else if (nodeType.contentEquals("ParallelGateway_split")) {
			String name = "parallelGateway_" + parallelGtwId;
			String uniqueParallelGtwIdSplit = "parallelGateway_" + this.parallelGtwId + "_split";
			nextNode = new InsertionConstruct(uniqueParallelGtwIdSplit, name, "ParallelGateway_split");
			this.parallelGtwId++;
		} else if (nodeType.contentEquals("Task")) {
			Random rand = new Random();
			String uniqueTaskId = "task_" + this.taskId;
			String participantName = this.participantNames.get(rand.nextInt(participantNames.size()));
			String taskName = uniqueTaskId + " [" + participantName + "]";
			nextNode = new InsertionConstruct(uniqueTaskId, taskName, "Task");
			this.taskId++;
		}

		return nextNode;
	}

	public void clearCurrentModelInstance() {
		this.modelInstance = Bpmn.createProcess("Process_" + run).startEvent("startEvent_1").done();
		this.startEvent = (FlowNode) modelInstance.getModelElementById("startEvent_1");
		LinkedList<FlowNode> startingPoint = new LinkedList<FlowNode>();
		startingPoint.add(startEvent);
		this.allPaths.clear();
		this.allPaths.add(startingPoint);
		this.taskId = 1;
		this.xorGtwId = 1;
		this.parallelGtwId = 1;
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

	private <T> T getRandomItem(List<T> list) {
		int randomIndex = ThreadLocalRandom.current().nextInt(0, list.size());
		return list.get(randomIndex);
	}

	private String getRandomNode(int branchingFactor, List<String> possibleNodesToChooseFrom) {
		String nodeToReturn = null;
		if (possibleNodesToChooseFrom.size() == 1) {
			return possibleNodesToChooseFrom.get(0);
		}

		if (possibleNodesToChooseFrom.contains("ExclusiveGateway_join")||possibleNodesToChooseFrom.contains("ParallelGateway_join")) {
				for (String str2 : possibleNodesToChooseFrom) {
						if (str2.contains("_join")) {
							int randomInt = ThreadLocalRandom.current().nextInt(0, 99);
							if (branchingFactor >= randomInt) {
								// join will be added
								nodeToReturn = str2;
								return nodeToReturn;
							}
						}			
			}
		}

		while (nodeToReturn == null) {
			int randomInt = ThreadLocalRandom.current().nextInt(0, 99);
			for (Entry<String, Integer[]> entry : this.percentagesForNodesToBeDrawn.entrySet()) {
				Integer[] currProbs = entry.getValue();
				String nodeType = entry.getKey();
				if (randomInt >= currProbs[0] && randomInt <= currProbs[1]
						&& possibleNodesToChooseFrom.contains(nodeType)) {
					nodeToReturn = entry.getKey();
					return nodeToReturn;
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
		boolean modelIsValid = false;
		while (!Thread.currentThread().isInterrupted() && !modelIsValid) {
			this.generateProcess(this.allPaths, this.possibleNodeTypes, this.amountTasksToBeInserted,
					this.amountXorsToBeInserted, this.amountParallelsToBeInserted);
			int xorSplits = CommonFunctionality.getAmountExclusiveGtwSplits(this.modelInstance);
			int parallelGtwsGenerated = CommonFunctionality.getAmountParallelGtwSplits(this.modelInstance);
			int amountTasks = this.modelInstance.getModelElementsByType(Task.class).size();
			int globalSphere = CommonFunctionality.getGlobalSphere(this.modelInstance, false);

			if (xorSplits == this.amountXorsToBeInserted && parallelGtwsGenerated == this.amountParallelsToBeInserted
					&& amountTasks == this.amountTasksToBeInserted && globalSphere == this.amountParticipants) {
				try {
					File f = writeChangesToFile();
					if (f != null) {
						System.out.println("File written: " + f.getAbsolutePath());
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

}
