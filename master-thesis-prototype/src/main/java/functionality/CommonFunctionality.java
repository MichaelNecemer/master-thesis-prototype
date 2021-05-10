package functionality;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Association;
import org.camunda.bpm.model.bpmn.instance.BusinessRuleTask;
import org.camunda.bpm.model.bpmn.instance.DataInputAssociation;
import org.camunda.bpm.model.bpmn.instance.DataObjectReference;
import org.camunda.bpm.model.bpmn.instance.DataOutputAssociation;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Gateway;
import org.camunda.bpm.model.bpmn.instance.ItemAwareElement;
import org.camunda.bpm.model.bpmn.instance.Lane;
import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.camunda.bpm.model.bpmn.instance.Text;
import org.camunda.bpm.model.bpmn.instance.TextAnnotation;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.camunda.bpm.model.bpmn.instance.di.Plane;
import org.camunda.bpm.model.bpmn.instance.di.Waypoint;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import Mapping.BPMNBusinessRuleTask;
import Mapping.BPMNElement;
import Mapping.BPMNEndEvent;
import Mapping.BPMNExclusiveGateway;
import Mapping.BPMNGateway;
import Mapping.BPMNParallelGateway;
import Mapping.BPMNParticipant;
import Mapping.BPMNTask;
import Mapping.Combination;
import Mapping.ProcessInstanceWithVoters;
import Mapping.RequiredUpdate;
import Mapping.VoterForXorArc;
import ProcessModelGeneratorAndAnnotater.LockedBranch;
import ProcessModelGeneratorAndAnnotater.ProcessModelAnnotater;

public class CommonFunctionality {

	public static boolean isCorrectModel(BpmnModelInstance modelInstance) throws Exception {
		// does correctness checking of the process model
		// e.g. 1 Start and 1 End Event
		boolean correctModel = true;
		if (!CommonFunctionality.checkIfOnlyOneStartEventAndEventEvent(modelInstance)) {
			throw new Exception("Model must have exactly 1 Start and 1 End Event");
		} 
		if(!CommonFunctionality.isModelValid(modelInstance)) {
			throw new Exception("Model is not valid!");
		}
		if(!CommonFunctionality.isModelBlockStructured(modelInstance)) {
			throw new Exception("Model must be block structured!");
		}
		
		
		return correctModel;

	}

	public void writeChangesToFile() {

	}

	public static boolean checkIfOnlyOneStartEventAndEventEvent(BpmnModelInstance modelInstance) {
		if (modelInstance.getModelElementsByType(StartEvent.class).size() == 1
				&& modelInstance.getModelElementsByType(EndEvent.class).size() == 1) {
			return true;
		} else {
			return false;
		}

	}

	public static boolean checkCorrectnessOfBranches(BpmnModelInstance modelInstance) {
		// for each dataObject
		// get all readers and writers
		// check paths between readers and writers and also in the other direction:
		// writers and readers
		// also check paths between writers -> when there is no path -> they are in
		// different branches
		// if there is no path -> nodes in different branches of same depth

		for (DataObjectReference dao : modelInstance.getModelElementsByType(DataObjectReference.class)) {

			LinkedList<FlowNode> readersForDataO = CommonFunctionality.getAllReadersForDataObject(modelInstance, dao);
			LinkedList<FlowNode> writersForDataO = CommonFunctionality.getAllWritersForDataObject(modelInstance, dao);
			if (readersForDataO.isEmpty()) {
				System.err.println("No readers in the process");
				return false;
			}

			for (FlowNode reader : readersForDataO) {
				int amountPathsWithWriter = 0;

				for (int i = 0; i < writersForDataO.size() - 1; i++) {

					FlowNode writer = writersForDataO.get(i);
					System.out.println("Writer: " + writer.getName() + ", Reader: " + reader.getName());
					LinkedList<LinkedList<FlowNode>> pathsBetweenWriterAndReader = CommonFunctionality
							.allPathsBetweenNodesWithCamundaNodes(writer, reader, new LinkedList<FlowNode>(),
									new LinkedList<FlowNode>(), new LinkedList<FlowNode>(),
									new LinkedList<LinkedList<FlowNode>>());
					LinkedList<LinkedList<FlowNode>> pathsBetweenReaderAndWriter = CommonFunctionality
							.allPathsBetweenNodesWithCamundaNodes(reader, writer, new LinkedList<FlowNode>(),
									new LinkedList<FlowNode>(), new LinkedList<FlowNode>(),
									new LinkedList<LinkedList<FlowNode>>());
					System.out.println("Paths between: " + pathsBetweenWriterAndReader.size());
					// there must be at least 1 path between the writer and the reader
					if (pathsBetweenWriterAndReader.size() > 0) {
						amountPathsWithWriter++;
					}

					for (int j = 1; j < writersForDataO.size(); j++) {
						// if there is no path between the writer i and writer j and writer j and writer
						// i -> they are in different branches of the same gtw
						FlowNode writer2 = writersForDataO.get(j);
						LinkedList<LinkedList<FlowNode>> pathsBetweenWriters = CommonFunctionality
								.allPathsBetweenNodesWithCamundaNodes(writer, writer2, new LinkedList<FlowNode>(),
										new LinkedList<FlowNode>(), new LinkedList<FlowNode>(),
										new LinkedList<LinkedList<FlowNode>>());
						if (pathsBetweenWriters.isEmpty()) {
							pathsBetweenWriters = CommonFunctionality.allPathsBetweenNodesWithCamundaNodes(writer2,
									writer, new LinkedList<FlowNode>(), new LinkedList<FlowNode>(),
									new LinkedList<FlowNode>(), new LinkedList<LinkedList<FlowNode>>());
						}

						if (pathsBetweenWriters.size() == 0) {
							System.out.println("No path from " + writer.getName() + " to " + writer2.getName());
							return false;
						}

					}

				}
				if (amountPathsWithWriter == 0) {
					// no writer for a reader found on any path in front of it
					System.err.println("No writer for " + dao.getName() + " in front of: " + reader.getName());

				}

			}

		}

		/*
		 * LinkedList<LinkedList<FlowNode>>paths =
		 * CommonFunctionality.allPathsBetweenNodesWithCamundaNodes(modelInstance.
		 * getModelElementsByType(StartEvent.class).iterator().next(),
		 * modelInstance.getModelElementsByType(EndEvent.class).iterator().next(), new
		 * LinkedList<FlowNode>(), new LinkedList<FlowNode>(), new
		 * LinkedList<FlowNode>(), new LinkedList<LinkedList<FlowNode>>());
		 * System.out.println("Paths size: "+paths.size());
		 * 
		 * 
		 * 
		 * 
		 * for(LinkedList<FlowNode>path: paths) { HashMap<ItemAwareElement,
		 * LinkedList<FlowNode>> writerOnPath = new HashMap<ItemAwareElement,
		 * LinkedList<FlowNode>>(); LinkedList<ParallelGateway>openSplit = new
		 * LinkedList<ParallelGateway>(); LinkedList<FlowNode>subPathToReader = new
		 * LinkedList<FlowNode>(); for(FlowNode node: path) { subPathToReader.add(node);
		 * 
		 * if(node instanceof ParallelGateway) { ParallelGateway pGtw =
		 * (ParallelGateway)node; if(node.getId().contains("split")) {
		 * openSplit.add(pGtw); } else if(node.getId().contains("join")) {
		 * openSplit.pollLast(); }
		 * 
		 * 
		 * }
		 * 
		 * if(node instanceof Task) { Task currTask = (Task)node;
		 * if(!currTask.getDataInputAssociations().isEmpty()) { //currTask is a reader
		 * for(DataInputAssociation dia: currTask.getDataInputAssociations()) {
		 * for(ItemAwareElement iae: dia.getSources()) { //check if there is a writer on
		 * the path to the currTask //for the data Object
		 * 
		 * LinkedList<FlowNode>writersOnPath = writerOnPath.getOrDefault(iae, new
		 * LinkedList<FlowNode>()); if(writersOnPath.isEmpty()) {
		 * System.err.println("There must be a writer on each path to "+currTask.getName
		 * ()+ " for DataObject "+CommonFunctionality.
		 * getDataObjectReferenceForItemAwareElement(modelInstance, iae).getName());
		 * return false; } else { for(FlowNode writer: writersOnPath) {
		 * if((!subPathToReader.contains(writer))) {
		 * System.err.println("There must be a writer on each path to "+currTask.getName
		 * ()+ " for DataObject "+CommonFunctionality.
		 * getDataObjectReferenceForItemAwareElement(modelInstance, iae).getName());
		 * return false; } } }
		 * 
		 * 
		 * 
		 * }
		 * 
		 * 
		 * 
		 * }
		 * 
		 * 
		 * }
		 * 
		 * if(!currTask.getDataOutputAssociations().isEmpty()) { //currTask is a writer
		 * for(DataOutputAssociation dao: currTask.getDataOutputAssociations()) {
		 * ItemAwareElement iae = dao.getTarget(); writerOnPath.computeIfAbsent(iae, k
		 * -> new LinkedList<FlowNode>()).add(currTask);
		 * 
		 * }
		 * 
		 * }
		 * 
		 * }
		 * 
		 * } }
		 */

		return true;

	}

	public static LinkedList<FlowNode> getAllReadersForDataObject(BpmnModelInstance modelInstance,
			DataObjectReference dataORef) {
		LinkedList<FlowNode> readers = new LinkedList<FlowNode>();
		for (Task task : modelInstance.getModelElementsByType(Task.class)) {
			for (DataInputAssociation dia : task.getDataInputAssociations()) {
				for (ItemAwareElement iae : dia.getSources()) {
					if (iae.getId().contentEquals(dataORef.getId())) {
						readers.add(task);
					}
				}
			}
		}
		return readers;
	}

	public static LinkedList<FlowNode> getAllWritersForDataObject(BpmnModelInstance modelInstance,
			DataObjectReference dataORef) {
		LinkedList<FlowNode> writers = new LinkedList<FlowNode>();
		for (Task task : modelInstance.getModelElementsByType(Task.class)) {
			for (DataOutputAssociation dao : task.getDataOutputAssociations()) {
				if (dao.getTarget().getId().contentEquals(dataORef.getId())) {
					writers.add(task);
				}
			}
		}
		return writers;
	}

	public static DataObjectReference getDataObjectReferenceForItemAwareElement(BpmnModelInstance modelInstance,
			ItemAwareElement iae) {
		for (Task task : modelInstance.getModelElementsByType(Task.class)) {
			for (DataInputAssociation dia : task.getDataInputAssociations()) {
				if (dia.getTarget().equals(iae)) {
					for (DataObjectReference daoR : modelInstance.getModelElementsByType(DataObjectReference.class)) {
						for (ItemAwareElement currIae : dia.getSources()) {
							if (currIae.getId().contentEquals(daoR.getId())) {
								return daoR;
							}
						}

					}
				}
			}
			for (DataOutputAssociation dao : task.getDataOutputAssociations()) {
				for (DataObjectReference daoR : modelInstance.getModelElementsByType(DataObjectReference.class)) {
					if (dao.getTarget().getId().contentEquals(daoR.getId())) {
						return daoR;
					}
				}
			}

		}

		return null;

	}

	public static LinkedList<LinkedList<BPMNElement>> allPathsBetweenNodesWithMappedNodes(BPMNElement startNode,
			BPMNElement endNode, LinkedList<BPMNElement> stack, LinkedList<BPMNElement> gtwStack,
			LinkedList<BPMNElement> currentPath, LinkedList<LinkedList<BPMNElement>> paths) {

		stack.add(startNode);
		boolean reachedEndGateway = false;

		while (!(stack.isEmpty())) {
			BPMNElement element = stack.pollLast();
			currentPath.add(element);

			if (element.getId().equals(endNode.getId())) {
				paths.add(currentPath);
				element = stack.pollLast();
				if (element == null && stack.isEmpty()) {
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

				if (element instanceof BPMNExclusiveGateway
						&& ((BPMNExclusiveGateway) element).getType().equals("split")) {
					LinkedList<BPMNElement> newPath = new LinkedList<BPMNElement>();
					newPath.addAll(currentPath);
					allPathsBetweenNodesWithMappedNodes(successor, endNode, stack, gtwStack, newPath, paths);
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

	public static LinkedList<LinkedList<FlowNode>> allPathsBetweenNodesWithCamundaNodes(FlowNode startNode,
			FlowNode endNode, LinkedList<FlowNode> stack, LinkedList<FlowNode> gtwStack,
			LinkedList<FlowNode> currentPath, LinkedList<LinkedList<FlowNode>> paths) {

		stack.add(startNode);
		boolean reachedEndGateway = false;

		while (!(stack.isEmpty())) {
			FlowNode element = stack.pollLast();
			currentPath.add(element);

			if (element.getId().equals(endNode.getId())) {
				paths.add(currentPath);
				element = stack.pollLast();
				if (element == null && stack.isEmpty()) {
					return paths;
				}
			}

			if (element instanceof ParallelGateway && ((ParallelGateway) element).getId().contains("split")) {
				for (SequenceFlow succeedingFlow : element.getOutgoing()) {
					gtwStack.add(element);
				}
			}

			if (element instanceof ParallelGateway && ((ParallelGateway) element).getId().contains("join")) {
				gtwStack.pollLast();
				if (!gtwStack.isEmpty()) {
					reachedEndGateway = true;
				}
			}

			for (SequenceFlow succeedingFlow : element.getOutgoing()) {
				FlowNode successor = succeedingFlow.getTarget();
				if (element instanceof ExclusiveGateway && ((ExclusiveGateway) element).getId().contains("split")) {
					LinkedList<FlowNode> newPath = new LinkedList<FlowNode>();
					newPath.addAll(currentPath);
					allPathsBetweenNodesWithCamundaNodes(successor, endNode, stack, gtwStack, newPath, paths);
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

	public static boolean removeTaskAsWriterForDataObject(BpmnModelInstance modelInstance, Task task,
			ItemAwareElement iae) {
		boolean removed = false;
		for (DataOutputAssociation dao : task.getDataOutputAssociations()) {
			for (ItemAwareElement currIae : dao.getSources()) {
				if (currIae.getId().equals(iae.getId())) {
					BpmnEdge edgeToBeRemoved = getEdge(modelInstance, dao.getId());
					removed = task.getDataOutputAssociations().remove(dao);
					task.removeChildElement(dao);
					edgeToBeRemoved.getParentElement().removeChildElement(edgeToBeRemoved);
				}
			}

		}

		return removed;

	}

	public static boolean removeTaskAsReaderFromDataObject(BpmnModelInstance modelInstance, Task task,
			ItemAwareElement iae) {
		boolean removed = false;
		for (DataInputAssociation dia : task.getDataInputAssociations()) {
			for (ItemAwareElement currIae : dia.getSources()) {
				if (currIae.getId().equals(iae.getId())) {
					BpmnEdge edgeToBeRemoved = getEdge(modelInstance, dia.getId());
					removed = task.getDataInputAssociations().remove(dia);
					task.removeChildElement(dia);
					edgeToBeRemoved.getParentElement().removeChildElement(edgeToBeRemoved);
				}
			}

		}

		return removed;

	}

	public static void addTaskAsWriterForDataObject(BpmnModelInstance modelInstance, Task task, ItemAwareElement iae) {
		DataOutputAssociation dao = modelInstance.newInstance(DataOutputAssociation.class);
		dao.setTarget(iae);
		task.addChildElement(dao);
		DataObjectReference daoR = modelInstance.getModelElementById(iae.getId());
		CommonFunctionality.generateDIElementForWriter(modelInstance, dao, getShape(modelInstance, iae.getId()),
				getShape(modelInstance, daoR.getId()));

	}

	public static boolean isReaderForDataObject(Task task, ItemAwareElement iae) {
		for (DataInputAssociation dia : task.getDataInputAssociations()) {
			if (dia.getSources().contains(iae)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isWriterForDataObject(Task task, ItemAwareElement iae) {
		for (DataOutputAssociation dao : task.getDataOutputAssociations()) {
			if (dao.getTarget().equals(iae)) {
				return true;
			}
		}
		return false;
	}

	public static BpmnEdge getEdge(BpmnModelInstance modelInstance, String id) {
		for (BpmnEdge edge : modelInstance.getModelElementsByType(BpmnEdge.class)) {
			if (edge.getBpmnElement().getId().equals(id)) {
				return edge;
			}
		}
		return null;

	}

	public static void generateDIElementForWriter(BpmnModelInstance modelInstance, DataOutputAssociation dao,
			BpmnShape daoR, BpmnShape writerTaskShape) {
		BpmnEdge e = modelInstance.newInstance(BpmnEdge.class);
		e.setBpmnElement(dao);
		Waypoint wp = modelInstance.newInstance(Waypoint.class);
		// Waypoints for the source -> the Writer Task
		wp.setX(writerTaskShape.getBounds().getX());
		wp.setY(writerTaskShape.getBounds().getY());
		e.addChildElement(wp);

		// Waypoint for the target -> the Data Object
		Waypoint wp2 = modelInstance.newInstance(Waypoint.class);

		wp2.setX(daoR.getBounds().getX());
		wp2.setY(daoR.getBounds().getY());
		e.addChildElement(wp2);

		modelInstance.getModelElementsByType(Plane.class).iterator().next().addChildElement(e);
	}

	public static BpmnShape getShape(BpmnModelInstance modelInstance, String id) {

		for (BpmnShape shape : modelInstance.getModelElementsByType(BpmnShape.class)) {
			if (shape.getBpmnElement().getId().equals(id)) {
				return shape;
			}
		}
		return null;

	}

	public static LinkedList<LinkedList<FlowNode>> removeReadersAndWritersInBranch(BpmnModelInstance modelInstance,
			FlowNode firstNodeInOtherBranch, FlowNode parallelJoin, ItemAwareElement iae) {
		// call this method when a writer is found inside a parallel branch
		// query the other branch till the corresponding parallel join is found
		// remove readers and writers on the other branch for the dataObject
		// all paths in the other branch will be excluded from the selection of a new
		// writer for that dataObject
		LinkedList<LinkedList<FlowNode>> pathsInsideBranch = CommonFunctionality.allPathsBetweenNodesWithCamundaNodes(
				firstNodeInOtherBranch, parallelJoin, new LinkedList<FlowNode>(), new LinkedList<FlowNode>(),
				new LinkedList<FlowNode>(), new LinkedList<LinkedList<FlowNode>>());
		for (LinkedList<FlowNode> path : pathsInsideBranch) {
			for (FlowNode node : path) {
				// if the node is a reader or writer to the dataObject -> remove
				// dataInputAssociation or dataOutputAssociation
				if (node instanceof Task) {
					Task currTask = (Task) node;
					if (CommonFunctionality.isReaderForDataObject(currTask, iae)) {
						// task is a reader
						CommonFunctionality.removeTaskAsReaderFromDataObject(modelInstance, currTask, iae);
						System.out.println("CHANGED: " + currTask.getId() + " from reader to task");
					} else if (CommonFunctionality.isWriterForDataObject(currTask, iae)) {
						// task is a writer
						CommonFunctionality.removeTaskAsWriterForDataObject(modelInstance, currTask, iae);
						System.out.println("CHANGED: " + currTask.getId() + " from writer to task");

					}
				}

			}

		}

		return pathsInsideBranch;
	}

	public static <T> T getRandomItem(List<T> list) {
		Random random = new Random();
		int listSize = list.size();
		int randomIndex = random.nextInt(listSize);
		return list.get(randomIndex);
	}

	private static LinkedList<LinkedList<FlowNode>> combineParallelBranches(BpmnModelInstance modelInstance,
			LinkedList<LinkedList<FlowNode>> parallelBranches, ParallelGateway pSplit) {
		LinkedList<SequenceFlow> flowIntoBranch = new LinkedList<SequenceFlow>();
		flowIntoBranch.addAll(pSplit.getOutgoing());

		LinkedList<LinkedList<FlowNode>> combinedPaths = new LinkedList<LinkedList<FlowNode>>();
		HashMap<SequenceFlow, LinkedList<LinkedList<FlowNode>>> map = new HashMap<SequenceFlow, LinkedList<LinkedList<FlowNode>>>();
		String pJoinId = pSplit.getName() + "_join";
		FlowNode pJoin = modelInstance.getModelElementById(pJoinId);

		for (LinkedList<FlowNode> path : parallelBranches) {
			for (int i = 0; i < path.size(); i++) {
				FlowNode node = path.get(i);
				if (node.equals(pSplit)) {
					FlowNode target = path.get(i + 1);
					SequenceFlow sFlow = null;
					for (SequenceFlow flowToTarget : node.getOutgoing()) {
						if (flowToTarget.getTarget().equals(target)) {
							sFlow = flowToTarget;
						}
					}
					if (map.get(sFlow) == null) {
						LinkedList<LinkedList<FlowNode>> paths = new LinkedList<LinkedList<FlowNode>>();
						paths.add(path);
						map.put(sFlow, paths);
					} else {
						map.get(sFlow).add(path);

					}
					break;
				}
			}

		}

		for (Entry<SequenceFlow, LinkedList<LinkedList<FlowNode>>> entry : map.entrySet()) {
			System.out.println("entry: " + entry.getKey().getId() + ", " + entry.getValue().size());
			for (LinkedList<FlowNode> f : entry.getValue()) {
				System.out.println("size: " + f.size());
			}
		}

		System.out.println(flowIntoBranch.size() + " flowintoBranch");
		SequenceFlow key1 = map.keySet().iterator().next();

		for (SequenceFlow key2 : map.keySet()) {
			for (LinkedList<FlowNode> path : map.get(key1)) {
				if (!key1.equals(key2)) {
					for (LinkedList<FlowNode> otherBranchPath : map.get(key2)) {
						LinkedList<FlowNode> pathToCombine = new LinkedList<FlowNode>();
						pathToCombine.addAll(path);
						for (FlowNode node : otherBranchPath) {
							if (node.equals(pSplit) || node.equals(pJoin) || !pathToCombine.contains(node)) {
								pathToCombine.add(node);
							}
						}

						combinedPaths.add(pathToCombine);

					}

				}
			}

		}

		return combinedPaths;

	}
	
	
	
	
	public static LinkedList<LinkedList<FlowNode>> getAllPathsBetweenNodes(BpmnModelInstance modelInstance, FlowNode startNode,
			FlowNode endNode, LinkedList<FlowNode> queue, LinkedList<FlowNode> openSplits,
			LinkedList<FlowNode> currentPath, LinkedList<LinkedList<FlowNode>> paths,
			LinkedList<LinkedList<FlowNode>> parallelBranches) throws NullPointerException, Exception {
			
		//filter out subpaths that do not have the endNode as target
		LinkedList<LinkedList<FlowNode>> subPaths = CommonFunctionality.getAllPaths(modelInstance, startNode, endNode, queue, openSplits, currentPath, paths, parallelBranches, endNode);
		System.out.println("Subpaths: "+subPaths.size());
		
		
		Iterator<LinkedList<FlowNode>> subPathsIter = subPaths.iterator();
		while(subPathsIter.hasNext()) {
			LinkedList<FlowNode>subPath = subPathsIter.next();
			if(!subPath.getLast().equals(endNode)&&!(subPath.contains(endNode))) {				
				subPathsIter.remove();				
			}
			
		}
		return subPaths;
	}
	
	
public static boolean isModelValid(BpmnModelInstance modelInstance) throws NullPointerException, Exception {
	//model must have a writer on each path to a reader
	//model can not have a reader/writer in a parallel branch when there is a writer in the other one
	//readers in both branches are allowed if there is no writer in any branch
	boolean isModelValid = true;
	
	LinkedList<LinkedList<FlowNode>> allProcessPaths =
			 CommonFunctionality.getAllPathsBetweenNodes(modelInstance,
			  modelInstance.getModelElementsByType(StartEvent.class).iterator().next(),
			  modelInstance.getModelElementsByType(EndEvent.class).iterator().next(), new
			  LinkedList<FlowNode>(), new LinkedList<FlowNode>(), new
			 LinkedList<FlowNode>(), new LinkedList<LinkedList<FlowNode>>(), new
			  LinkedList<LinkedList<FlowNode>>());
	
	for(LinkedList<FlowNode>path: allProcessPaths) {
		LinkedList<FlowNode>openParallelSplits = new LinkedList<FlowNode>();
		LinkedList<FlowNode>pathToCurrTask = new LinkedList<FlowNode>();
		for(int i = 0; i < path.size(); i++) {
			FlowNode f = path.get(i);
			pathToCurrTask.add(f);
			if(f instanceof ParallelGateway && f.getOutgoing().size()>=2) {
				//parallel Split found on path				
				openParallelSplits.add(f);
				
			}
			if (f instanceof ParallelGateway && f.getIncoming().size()>=2) {
				//parallel Join found on path
				openParallelSplits.pollLast();
			}
			if(f instanceof Task) {
				Task currTask = (Task)f;
				if(!currTask.getDataInputAssociations().isEmpty()) {
					//task is a reader
				
				for (DataInputAssociation dia : currTask.getDataInputAssociations()) {
					for (ItemAwareElement iae : dia.getSources()) {
						//check if there is a writer to the dataObject on the pathToReader
						boolean writerOnPath = false;
						for(FlowNode pathToReaderNode: pathToCurrTask) {							
							if(pathToReaderNode instanceof Task) {							
								if(CommonFunctionality.isWriterForDataObject((Task)pathToReaderNode, iae)) {
									writerOnPath = true;
								}
							
							}
						}
						if(writerOnPath==false) {
							System.err.println("Not a writer before every reader!");
							return false; 
						}
			
					}
			
			
				}
		
			
			
		}
				
				if(!currTask.getDataOutputAssociations().isEmpty()) {
					//currTask is a writer
					//check if currTask is inside a parallel Branch
					//if yes, check if there is a reader/writer in the other branch
					
					if(!openParallelSplits.isEmpty()) {
						//currTask is in a parallel Branch
						//for the lastOpened parallelSplit:						
						//check if there are readers/writers in the other branch
						//if so - model is not valid
						
						LinkedList<FlowNode>currOpenPSplits = new LinkedList<FlowNode>();
						currOpenPSplits.addAll(openParallelSplits);
									boolean insidePBranch = false;		
							LinkedList<FlowNode>currPBranchPath = new LinkedList<FlowNode>();
							ListIterator<FlowNode>currOpenPSplitsIter = currOpenPSplits.listIterator(currOpenPSplits.size());
							while(currOpenPSplitsIter.hasPrevious()) {						
							FlowNode currentLastPSplit = currOpenPSplitsIter.previous();
								
							for(int j = 0; j<path.size();j++) {
								FlowNode otherNode = path.get(j);	
								if(insidePBranch) {
									currPBranchPath.add(otherNode);									
								}
								
								if(otherNode instanceof ParallelGateway && otherNode.equals(currentLastPSplit)) {
									currPBranchPath.add(otherNode);
									insidePBranch=true;									
								} else if(otherNode instanceof ParallelGateway && CommonFunctionality.getCorrespondingGtw(modelInstance, (Gateway) currentLastPSplit)==otherNode) {
									if(!currPBranchPath.contains(currTask)) {
									//end of parallelSplit reached on other branch than currTask is - which is a writer							
									//check if this branch contains a reader or writer to the same dataObject
										
										Iterator<FlowNode>subPathNodeIter = currPBranchPath.iterator();
											while(subPathNodeIter.hasNext()) {
												FlowNode subPathNode = subPathNodeIter.next();
												if(subPathNode instanceof Task) {
													for (DataOutputAssociation dao : currTask.getDataOutputAssociations()) {
														ItemAwareElement iae = dao.getTarget();
														if(CommonFunctionality.isReaderForDataObject((Task)subPathNode, iae)==true) {
															//subPathNode is reader in other parallel branch than currTask
															return false;															
														}
														if(CommonFunctionality.isWriterForDataObject((Task)subPathNode, iae)==true) {
															//subPathNode is writer in other parallel branch than currTask
															return false;															
														}
													
													}
												
											}
										
								
										
									
										
									}
											//other branch has been checked for writers/readers
											currOpenPSplitsIter.remove();
								}
									insidePBranch = false;									
									currPBranchPath=new LinkedList<FlowNode>();
																															
							}
							
							
							
						
					}
							}
					
					
										
					
				}
		
			}
		}
		
	}
	}
	return isModelValid;
}
	

	


public static LinkedList<LinkedList<FlowNode>> getAllPaths(BpmnModelInstance modelInstance, FlowNode startNode,
		FlowNode endNode, LinkedList<FlowNode> queue, LinkedList<FlowNode> openSplits,
		LinkedList<FlowNode> currentPath, LinkedList<LinkedList<FlowNode>> paths,
		LinkedList<LinkedList<FlowNode>> parallelBranches, FlowNode endPointOfSearch) throws NullPointerException, Exception {
	// go DFS inside all branch till corresponding join is found
	queue.add(startNode);		
	
	while (!(queue.isEmpty())) {
		FlowNode element = queue.poll();
		
		if((endPointOfSearch instanceof EndEvent || endPointOfSearch instanceof Gateway)) {
		Iterator<LinkedList<FlowNode>>subPaths = paths.iterator();
		while(subPaths.hasNext()) {
			LinkedList<FlowNode>subPath = subPaths.next();
			if(currentPath.containsAll(subPath)&&currentPath.size()==subPath.size()+1) {
				subPaths.remove();
			}
			
		}
		}
		currentPath.add(element);
		
		if (element.getId().equals(endNode.getId())) {
			
			Iterator<LinkedList<FlowNode>>subPathIter = paths.iterator();
			while(subPathIter.hasNext()) {
				LinkedList<FlowNode>subPath = subPathIter.next();
				if(currentPath.containsAll(subPath)&&currentPath.size()==subPath.size()+1) {
					subPathIter.remove();
				}
				
			}
			paths.add(currentPath);
			
			
			
			
			if (endNode instanceof ParallelGateway) {
				// currentPath is part of a parallel branch
				parallelBranches.add(currentPath);
			}

			if (endNode instanceof Gateway && endNode.getIncoming().size() == 2) {

				Gateway joinGtw = (Gateway) element;

				// when a join is found - poll the last opened gateway from the stack
				Gateway lastOpenedSplitGtw = (Gateway)openSplits.pollLast();

				if (!openSplits.contains(lastOpenedSplitGtw)) {
					// when the openSplitStack does not contain the lastOpenedSplit anymore, all
					// branches to the joinGtw have been visited
					// go from joinGtw to the Join of the last opened split in the stack, if there
					// is any

					// if lastOpenedSplit was a parallelGtw -> get possible combinations of paths
					// within the branches
					if (lastOpenedSplitGtw instanceof ParallelGateway) {
						// -> check if each path containing the corresponding split of the
						// parallelJoinGtw has the joinGtw as last element!
						// else there are paths inside the branch that are not a fully built yet

						String idOfSplit = joinGtw.getName() + "_split";
						FlowNode splitGtw = modelInstance.getModelElementById(idOfSplit);

						LinkedList<LinkedList<FlowNode>> pathsToCombine = new LinkedList<LinkedList<FlowNode>>();
						LinkedList<LinkedList<FlowNode>> pathsNotFullyBuilt = new LinkedList<LinkedList<FlowNode>>();

						for (LinkedList<FlowNode> path : paths) {
							if (path.contains(splitGtw) && path.getLast().equals(joinGtw)) {
								pathsToCombine.add(path);
							} else if (path.contains(splitGtw) && !path.getLast().equals(joinGtw)) {
								pathsNotFullyBuilt.add(path);
							}

						}

						if (pathsToCombine.size()>=2) {
							// all paths to joinGtw are fully built
							LinkedList<LinkedList<FlowNode>> combinedPaths = CommonFunctionality
									.combineParallelBranches(modelInstance, pathsToCombine,
											(ParallelGateway) lastOpenedSplitGtw);

							Iterator<LinkedList<FlowNode>> pathsIter = paths.iterator();

							while (pathsIter.hasNext()) {
								LinkedList<FlowNode> currPath = pathsIter.next();
								boolean isSubList = false;
								for(LinkedList<FlowNode>combPath: combinedPaths) {
									if(CommonFunctionality.isSubListOfList2(currPath, combPath)) {
										isSubList=true;	
										break;
									}
								}
								if (isSubList) {
									pathsIter.remove();
								}

							}
							paths.addAll(combinedPaths);

							Iterator<LinkedList<FlowNode>> pBranchIter = parallelBranches.iterator();

							while (pBranchIter.hasNext()) {
								LinkedList<FlowNode> pBranch = pBranchIter.next();
								boolean isSubList = false;
								for(LinkedList<FlowNode>combPath: combinedPaths) {
									if(CommonFunctionality.isSubListOfList2(pBranch, combPath)) {
										isSubList=true;
										break;
									}
								}
								if (isSubList) {
									pBranchIter.remove();
								}
							}

							if (!parallelBranches.isEmpty()) {
								parallelBranches.addAll(combinedPaths);
							}
						
						}
					}

					if (!openSplits.isEmpty()) {
						// still inside a branch
						Gateway lastOpenedSplit = (Gateway)openSplits.getLast();
						
						FlowNode correspondingJoin = 	CommonFunctionality.getCorrespondingGtw(modelInstance, lastOpenedSplit);
						
						
						// go to next join node with all paths
						LinkedList<LinkedList<FlowNode>> newPaths = new LinkedList<LinkedList<FlowNode>>();

						for (LinkedList<FlowNode> path : paths) {
							if (path.getLast().equals(element)) {
								LinkedList<FlowNode> newPathAfterJoin = new LinkedList<FlowNode>();
								newPathAfterJoin.addAll(path);
								newPaths.add(newPathAfterJoin);
							}
						}
						
						//need to add the lastOpenedSplit, since one path has gone dfs till the join already
						
						for(int i = 0; i < newPaths.size()-1;i++) {
							openSplits.add(lastOpenedSplit);
						}

						for (LinkedList<FlowNode> newPath : newPaths) {
							getAllPaths(modelInstance, element.getOutgoing().iterator().next().getTarget(),
									correspondingJoin, queue, openSplits, newPath, paths, parallelBranches, endPointOfSearch);
						}

						
					} else {
						// when there are no open splits gtws 
						// go from the successor of the element to end since the currentElement has
						// already been added to the path
						LinkedList<LinkedList<FlowNode>> newPaths = new LinkedList<LinkedList<FlowNode>>();

						for (LinkedList<FlowNode> path : paths) {
							if (path.getLast().equals(element)) {
								LinkedList<FlowNode> newPathAfterJoin = new LinkedList<FlowNode>();
								newPathAfterJoin.addAll(path);
								newPaths.add(newPathAfterJoin);
							}
						}

						for (LinkedList<FlowNode> newPath : newPaths) {
							getAllPaths(modelInstance, element.getOutgoing().iterator().next().getTarget(),
									endPointOfSearch, queue,
									openSplits, newPath, paths, parallelBranches, endPointOfSearch);
						}

					}
				}

			}
			
			element = queue.poll();
			if (element == null && queue.isEmpty()) {

				return paths;
			}

		}

		if (element instanceof Gateway && element.getOutgoing().size() == 2) {
			// add the split to the openSplitStack 1 times for each outgoing paths
			int amountOfOutgoingPaths = element.getOutgoing().size();
			int i = 0;
			while (i < amountOfOutgoingPaths) {
				openSplits.add((Gateway) element);
				i++;
			}

		}

		for (SequenceFlow outgoingFlow : element.getOutgoing()) {
			FlowNode successor = outgoingFlow.getTarget();
			if (element instanceof Gateway && element.getOutgoing().size() == 2) {
				// when a split is found - go dfs till the corresponding join is found
				FlowNode correspondingJoinGtw = CommonFunctionality.getCorrespondingGtw(modelInstance, (Gateway)element);
				if (correspondingJoinGtw == null) {
					System.err.println(
							"no corresponding join for " + element.getId() + ", " + element.getOutgoing().size());

				}

				LinkedList<FlowNode> newPath = new LinkedList<FlowNode>();
				newPath.addAll(currentPath);

				getAllPaths(modelInstance, successor, correspondingJoinGtw, queue, openSplits, newPath, paths,
						parallelBranches, endPointOfSearch);
			} else {

				queue.add(successor);

			}

		}

	}
	
	return paths;

}











	public static <T> boolean isSubListOfEachList(LinkedList<T> list1, LinkedList<LinkedList<FlowNode>> listOfLists) {
		boolean isSubListOfEachList = true;
		for (LinkedList<FlowNode> list2 : listOfLists) {

			if (!list1.equals(list2)) {
				if (!list2.containsAll(list1)) {
					isSubListOfEachList = false;
					return isSubListOfEachList;
				}
			}
		}
		return isSubListOfEachList;
	}

	public static <T> boolean isSubListOfList2(LinkedList<T> list1, LinkedList<T> list2) {
		boolean isSubListOfList = true;

		if (!list1.equals(list2)) {
			if (!list2.containsAll(list1)) {
				isSubListOfList = false;
				return isSubListOfList;
			}
		}

		return isSubListOfList;
	}

	public static <T> boolean hasduplicateList(LinkedList<LinkedList<T>> lists) {
		return lists.stream() // create a <Stream<List<T>>
				.map(HashSet::new) // transform Stream<List<T>> to Stream<HashSet<T>>
				.distinct() // keep only distinct Sets
				.count() < lists.size();
	}

	public static Gateway getCorrespondingGtw(BpmnModelInstance modelInstance, Gateway gtw) throws NullPointerException, Exception{
		
		Gateway correspondingGtw = null;
		if(gtw.getIncoming().size()>=2&&gtw.getOutgoing().size()==1) {
			//gtw is a join
			StringBuilder splitGtwId = new StringBuilder();
			
			if(gtw.getId().contains("_join")) {
				splitGtwId.append(gtw.getName()+"_split");
			} else {
				//it must have the same name but >=2 outgoing and ==1 incoming
				for(Gateway gateway: modelInstance.getModelElementsByType(Gateway.class)) {
					if(gateway.getName().contentEquals(gtw.getName())) {
						if(gateway.getIncoming().size()==1&&gateway.getOutgoing().size()>=2) {
							splitGtwId.append(gateway.getId());
							break;
						}
					}
				}
								
			}			
			Gateway splitGtw = modelInstance.getModelElementById(splitGtwId.toString().trim());
			if(splitGtw==null) {
				throw new Exception("Names of corresponding split and join gtws have to be equal!");
			}
			if(splitGtw.getIncoming().size()==1&&splitGtw.getOutgoing().size()>=2) {
				correspondingGtw =  splitGtw;
			} 
		} else if(gtw.getIncoming().size()==1&&gtw.getOutgoing().size()>=2) {
			//gtw is a split
			StringBuilder joinGtwId = new StringBuilder();
			if(gtw.getId().contains("_split")) {
				joinGtwId.append(gtw.getName()+"_join");
			} else {
				//it must have the same name but ==1 outgoing and >=2 incoming				
				for(Gateway gateway: modelInstance.getModelElementsByType(Gateway.class)) {
					if(gateway.getName().contentEquals(gtw.getName())) {
						if(gateway.getIncoming().size()>=2&&gateway.getOutgoing().size()==1) {
							joinGtwId.append(gateway.getId());
							break;
						}
					}
				}
								
			}			
			Gateway joinGtw = modelInstance.getModelElementById(joinGtwId.toString().trim());
			if(joinGtw==null) {
				throw new Exception("Names of corresponding split and join gtws have to be equal!");
			}
			if(joinGtw.getIncoming().size()>=2&&joinGtw.getOutgoing().size()==1) {
				correspondingGtw = joinGtw;
			} 
		}
		
		return correspondingGtw;
		
	}
	
	
	
	
	
	public static boolean isModelBlockStructured (BpmnModelInstance modelInstance) throws NullPointerException, Exception {
		if(modelInstance.getModelElementsByType(Gateway.class).size()%2!=0) {
			return false;
		}
		for(Gateway gtw: modelInstance.getModelElementsByType(Gateway.class)) {
			if(CommonFunctionality.getCorrespondingGtw(modelInstance, gtw)==null) {
				return false;
			}
		}
		return true;
	}
	
	public static int getAmountExclusiveGtwSplits(BpmnModelInstance modelInstance) {
		int amount = 0;
		for(ExclusiveGateway gtw: modelInstance.getModelElementsByType(ExclusiveGateway.class)) {
			if(gtw.getIncoming().size()==1&&gtw.getOutgoing().size()>=2) {
				amount++;
			}
		}
		
		return amount;
		
		
	}
	
	
	
	
	public static int getAmountElements(BpmnModelInstance modelInstance) {
		
		return modelInstance.getModelElementsByType(FlowNode.class).size();
		
		
	}
	
	

	public static int getAmountParallelGtwSplits(BpmnModelInstance modelInstance) {
		int amount = 0;
		for(ParallelGateway gtw: modelInstance.getModelElementsByType(ParallelGateway.class)) {
			if(gtw.getIncoming().size()==1&&gtw.getOutgoing().size()>=2) {
				amount++;
			}
		}
		
		return amount;
		
		
	}
	
	
	public static HashMap<DataObjectReference, LinkedList<FlowNode>> getReadersForDataObjects(BpmnModelInstance modelInstance){
		HashMap<DataObjectReference, LinkedList<FlowNode>> readersMap = new HashMap<DataObjectReference, LinkedList<FlowNode>>();
		for(DataObjectReference daoR: modelInstance.getModelElementsByType(DataObjectReference.class)) {
			readersMap.putIfAbsent(daoR, new LinkedList<FlowNode>());
		}
		
		for(Task task: modelInstance.getModelElementsByType(Task.class)) {
			for(DataInputAssociation dia: task.getDataInputAssociations()) {
				for(ItemAwareElement iae: dia.getSources()) {
					for(DataObjectReference daoRef: readersMap.keySet()) {
					if(iae.getId().contentEquals(daoRef.getId())) {
						readersMap.get(daoRef).add(task);
					}
					}
				}
			}
		}
		
		return readersMap;
	}
	
	
	public static HashMap<DataObjectReference, LinkedList<FlowNode>> getWritersForDataObjects(BpmnModelInstance modelInstance){
		HashMap<DataObjectReference, LinkedList<FlowNode>> writersMap = new HashMap<DataObjectReference, LinkedList<FlowNode>>();
		for(DataObjectReference daoR: modelInstance.getModelElementsByType(DataObjectReference.class)) {
			writersMap.putIfAbsent(daoR, new LinkedList<FlowNode>());
		}
		
		for(Task task: modelInstance.getModelElementsByType(Task.class)) {
			for(DataOutputAssociation dao: task.getDataOutputAssociations()) {
				ItemAwareElement iae = dao.getTarget();
					for(DataObjectReference daoRef: writersMap.keySet()) {
					if(iae.getId().contentEquals(daoRef.getId())) {
						writersMap.get(daoRef).add(task);
					}
					}
				}
			}
		
		
		return writersMap;
	}
	
	
	public static File createFileWithinDirectory(String directory, String filename) {
		 File dir = new File(directory);
		    if (!dir.exists()) {
		    	dir.mkdirs();	    
		    }
		    
		    File newFile = new File(directory + File.separatorChar + filename);
	
		    return newFile;
		
		
	}
	
	
	public static File fileWithDirectoryAssurance(String directory, String filename) {
	    File dir = new File(directory);
	    if (!dir.exists()) {
	    	dir.mkdirs();	    
	    }
	    
	    File newFile = new File(directory + File.separatorChar + filename);
	    if(!newFile.exists()) {
	    	newFile.mkdirs();
	    }
	    return newFile;
	}
	
	

	
	public static void setTimeout(Runnable runnable, int delay){
	    new Thread(() -> {
	        try {
	            Thread.sleep(delay);
	            runnable.run();
	        }
	        catch (Exception e){
	            System.err.println(e);
	        }
	    }).start();
	}
	
	public static String getNextStricterSphere(String sphere) {
		sphere.trim();
		if(sphere.contentEquals("Public")) {
			return "Global";
		} else if(sphere.contentEquals("Global")) {
			return "Static";
		}else if(sphere.contentEquals("Static")) {
				return "Weak-Dynamic";
			
		} else if(sphere.contentEquals("Weak-Dynamic")) {
			return "Strong-Dynamic";
		} else 
		
			return"";
	
		
	}
	
	
	public static int getGlobalSphere(BpmnModelInstance modelInstance, boolean modelWithLanes) {	
		
		
		if(!modelWithLanes) {
			//if the model is without lanes -> the participants must be extracted from the task name 
			LinkedList<String>participants = new LinkedList<String>();
		for (Task task : modelInstance.getModelElementsByType(Task.class)) {
				
				
			String participantName = task.getName().substring(task.getName().indexOf("[")+1, task.getName().indexOf("]"));
			if(!participants.contains(participantName)) {
				participants.add(participantName);
			}
				
			}
			return participants.size();
		} else {
			// model is with lanes 
			return modelInstance.getModelElementsByType(Lane.class).size();			
		}
		
	}
	
public static void generateNewModelAndIncreaseVotersForEachDataObject(String pathToFile, int increaseBy) {
	File file = new File(pathToFile);	
	BpmnModelInstance modelInstance = Bpmn.readModelFromFile(file);
		
		boolean modelWithLanes = true;
		if(modelInstance.getModelElementsByType(Lane.class).isEmpty()) {
			modelWithLanes=false;
		}
		
		int maxParticipants = CommonFunctionality.getGlobalSphere(modelInstance, modelWithLanes);
		
		
		for(ExclusiveGateway gtw: modelInstance.getModelElementsByType(ExclusiveGateway.class)) {
			if(gtw.getIncoming().size()==1&&gtw.getOutgoing().size()>=2) {
				for (TextAnnotation tx : modelInstance.getModelElementsByType(TextAnnotation.class)) {
					for (Association assoc : modelInstance.getModelElementsByType(Association.class)) {
						if (assoc.getSource().equals(gtw) && assoc.getTarget().equals(tx)) {
							if (tx.getTextContent().startsWith("[Voters]")) {
								String string = tx.getTextContent();
								String substr = string.substring(string.indexOf('{') + 1, string.indexOf(','));
								
								int currAmountVoters = Integer.parseInt(substr);
								if(currAmountVoters<maxParticipants) {
									int amountAfterIncreasing = currAmountVoters+increaseBy;
									string.replaceFirst(substr, amountAfterIncreasing+"");
									tx.setTextContent(string);
								}
								
							}
						}

					}
				}
				
			}
			
		}
		
	}




	
	public static void increaseSpherePerDataObject(File file, String directoryToStore, String suffixName, String sphereToSet) throws IOException, ParserConfigurationException, SAXException {
		//sets the sphere of each data object in the model to the sphereToSet
		
		BpmnModelInstance modelInstance = Bpmn.readModelFromFile(file);
		String fileName = file.getName().substring(0, file.getName().indexOf(".bpmn"));
		String iterationName = "";
		
				for (TextAnnotation tx : modelInstance.getModelElementsByType(TextAnnotation.class)) {
					
							if (tx.getTextContent().startsWith("Default")) {
				
							String subString = tx.getTextContent().substring(tx.getTextContent().indexOf('{')+1, tx.getTextContent().indexOf('}'));
								
										StringBuilder sb = new StringBuilder();
										sb.append(tx.getTextContent().substring(0, tx.getTextContent().indexOf('{')));
										
										
										sb.append(sphereToSet+'}');
										Text txt = modelInstance.newInstance(Text.class);
										txt.setTextContent(sb.toString());
										tx.setText(txt);
										
										
							
							
						
					
				}
				}	
				String modelWithStricterSpheres = fileName+"_"+sphereToSet;
				CommonFunctionality.writeChangesToFile(modelInstance, modelWithStricterSpheres,  directoryToStore, iterationName);
		
		
		
	}

	
	
	
	
	
	public static void increaseVotersPerDataObject(File file, String directoryToStore, String suffixName, int currValue) throws IOException, ParserConfigurationException, SAXException {
		BpmnModelInstance modelInstance = Bpmn.readModelFromFile(file);
		String fileName = file.getName().substring(0, file.getName().indexOf(".bpmn"));
		String iterationName = "_amountVoters";
		
		boolean modelWithLanes = true;
		if(modelInstance.getModelElementsByType(Lane.class).isEmpty()) {
			modelWithLanes=false;
		}
		
		int maxParticipants = CommonFunctionality.getGlobalSphere(modelInstance, modelWithLanes);
		
		
		for(ExclusiveGateway gtw: modelInstance.getModelElementsByType(ExclusiveGateway.class)) {
			if(gtw.getIncoming().size()==1&&gtw.getOutgoing().size()>=2) {
				for (TextAnnotation tx : modelInstance.getModelElementsByType(TextAnnotation.class)) {
					for (Association assoc : modelInstance.getModelElementsByType(Association.class)) {
						if (assoc.getSource().equals(gtw) && assoc.getTarget().equals(tx)) {
							if (tx.getTextContent().startsWith("[Voters]")) {
								
								String subString = tx.getTextContent().substring(tx.getTextContent().indexOf('{')+1, tx.getTextContent().indexOf('}'));
								String[]values = subString.split(",");
								
								int currAmountVoters = Integer.parseInt(values[0]);
								if(currAmountVoters<=maxParticipants&&currValue<=maxParticipants) {
									int amountAfterIncreasing = currValue;								
									
									int lowerBound = (int) Math.ceil((double)amountAfterIncreasing / 2)+1;								
									int randomCountVotersSameDecision = 0;
									if(lowerBound<currValue) {
									randomCountVotersSameDecision = ThreadLocalRandom.current().nextInt(lowerBound,
											amountAfterIncreasing + 1);
									} else {
										randomCountVotersSameDecision = currValue;
									}
									StringBuilder sb = new StringBuilder();
									sb.append("[Voters]{"+amountAfterIncreasing+","+randomCountVotersSameDecision+","+values[2]+"}");
									
									Text text = modelInstance.newInstance(Text.class);
									text.setTextContent(sb.toString());				
									tx.setText(text);									
									iterationName += "-"+amountAfterIncreasing;
								}
								
							}
						}

					}
				}
				
			}
			
		}
		if(!iterationName.isEmpty()) {
		String modelWithNewAmountVoters = fileName;
		CommonFunctionality.writeChangesToFile(modelInstance, modelWithNewAmountVoters,  directoryToStore, suffixName);
		}
	}
	
	public static int getAmountFromPercentageWithMinimum(int amountTasks, int percentage, int min) {
		double amountFromPercentage = (double)amountTasks*percentage/100;
		int amountTasksToBeWriter =  (int) Math.ceil(amountFromPercentage);		
		if(amountTasksToBeWriter<min) {
			return min;
		} 
			return amountTasksToBeWriter;
		
	}
	
	public static int getAmountFromPercentage(int amountTasks, int percentage) {
		double amountFromPercentage = (double)amountTasks*percentage/100;
		return (int) Math.ceil(amountFromPercentage);		
		
	}
	
	public static void writeChangesToFile(BpmnModelInstance modelInstance, String fileName,  String directoryToStoreAnnotatedModel, String suffixName) throws IOException, ParserConfigurationException, SAXException {
		// validate and write model to file
		Bpmn.validateModel(modelInstance);		
		
		int fileNumber = 0;
		
		File dir = new File(directoryToStoreAnnotatedModel);		
		  File[] directoryListing = dir.listFiles();
		  if (directoryListing != null) {
		    for (File child : directoryListing) {	
		    	if(child.getName().contains(fileName)&&child.getName().contains(".bpmn")) {
		    	String nameOfFileInsideDirectory = child.getName().substring(0, child.getName().indexOf(".bpmn"));	
		    	if(nameOfFileInsideDirectory.contains("_annotated")) {
		    	Pattern p = Pattern.compile("[0-9]+");
		    	Matcher m = p.matcher(nameOfFileInsideDirectory);
		    	while (m.find()) {
		    	    int num = Integer.parseInt(m.group());
		    	    if(num>fileNumber) {
		    	    	fileNumber = num;
		    	    }
		    	}
		    }
		    }
		    }
		  }
		 fileNumber++; 
		String annotatedFileName = fileName+fileNumber+suffixName+".bpmn";
		File file = CommonFunctionality.createFileWithinDirectory(directoryToStoreAnnotatedModel, annotatedFileName);
			
		System.out.println("File path: "+file.getAbsolutePath());
		Bpmn.writeModelToFile(file, modelInstance);
		

		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		docBuilderFactory.setNamespaceAware(true);
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document document = docBuilder.parse(file);
		

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
			/*
			 * DOMSource source = new DOMSource(document); StreamResult filex = new
			 * StreamResult(new File("C:\\Users\\Micha\\OneDrive\\Desktop\\test.xml"));
			 * 
			 * transformer.transform(source, filex);
			 */
			
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	 public static List<LinkedList<Integer>> computeRepartitionNumber(int maxAmount, int subParts, int threshold_number) throws Exception {
	    
		 List<LinkedList<Integer>> resultRec = new LinkedList<LinkedList<Integer>>();
	        
	        
	        if (subParts == 1) {
	            List<LinkedList<Integer>> resultEnd = new LinkedList<LinkedList<Integer>>();
	            LinkedList<Integer> unitary = new LinkedList<>();
	            resultEnd.add(unitary);
	            unitary.add(maxAmount);
	            return resultEnd;
	        }

	        for (int i = threshold_number; i <= maxAmount-threshold_number; i++) {
	            int remain = maxAmount - i;
	            List<LinkedList<Integer>> partialRec = computeRepartitionNumber(remain, subParts - 1, threshold_number);
	            for(List<Integer> subList : partialRec){
	                subList.add(i);             
	            }
	            resultRec.addAll(partialRec);
	        }
	        return resultRec;

	    }

	public static int getSumAmountVotersOfModel(BpmnModelInstance modelInstance) {
		
		int sumAmountVoters = 0; 
		for(ExclusiveGateway gtw: modelInstance.getModelElementsByType(ExclusiveGateway.class)) {
			if(gtw.getIncoming().size()==1&&gtw.getOutgoing().size()>=2) {
				for (TextAnnotation tx : modelInstance.getModelElementsByType(TextAnnotation.class)) {
					for (Association assoc : modelInstance.getModelElementsByType(Association.class)) {
						if (assoc.getSource().equals(gtw) && assoc.getTarget().equals(tx)) {
							if (tx.getTextContent().startsWith("[Voters]")) {
								
								String subString = tx.getTextContent().substring(tx.getTextContent().indexOf('{')+1, tx.getTextContent().indexOf('}'));
								String[]values = subString.split(",");
								
								int currAmountVoters = Integer.parseInt(values[0]);
								sumAmountVoters+=currAmountVoters;
							}
						}
					}
				}
		
			}
	}
		return sumAmountVoters;
	}
	
	
public static double getAverageAmountVotersOfModel(BpmnModelInstance modelInstance) {
		int amountGtws = 0; 
		double sumAmountVoters = 0; 
		for(ExclusiveGateway gtw: modelInstance.getModelElementsByType(ExclusiveGateway.class)) {
			if(gtw.getIncoming().size()==1&&gtw.getOutgoing().size()>=2) {
				for (TextAnnotation tx : modelInstance.getModelElementsByType(TextAnnotation.class)) {
					for (Association assoc : modelInstance.getModelElementsByType(Association.class)) {
						if (assoc.getSource().equals(gtw) && assoc.getTarget().equals(tx)) {
							if (tx.getTextContent().startsWith("[Voters]")) {
								
								String subString = tx.getTextContent().substring(tx.getTextContent().indexOf('{')+1, tx.getTextContent().indexOf('}'));
								String[]values = subString.split(",");
								
								double currAmountVoters = Double.parseDouble(values[0]);
								sumAmountVoters+=currAmountVoters;
								amountGtws++;
							}
						}
					}
				}
		
			}
	}
		return ((double)sumAmountVoters/amountGtws);
	}
	
	
	
public static int getSphereSumOfModel(BpmnModelInstance modelInstance) {
		//global = 0, ....,  Strong-Dynamic = 3
	//a higher sum will be a model with more strict decisions
		int sumSpheres = 0; 
		
			HashMap<DataObjectReference, LinkedList<FlowNode>> readers = CommonFunctionality.getReadersForDataObjects(modelInstance);
				for(DataObjectReference dataO: readers.keySet()) {
					for(FlowNode f: readers.get(dataO)) {
						if(f instanceof BusinessRuleTask) {

							for (TextAnnotation tx : modelInstance.getModelElementsByType(TextAnnotation.class)) {
								
										if (tx.getTextContent().startsWith("Default ")) {
							
										String subString = tx.getTextContent().substring(tx.getTextContent().indexOf('{')+1, tx.getTextContent().indexOf('}'));
									
													if(subString.contentEquals("Strong-Dynamic")) {
														sumSpheres+=3;
													}
													else if(subString.contentEquals("Weak-Dynamic")) {
														sumSpheres+=2;
													} 
													else if(subString.contentEquals("Static")) {
														sumSpheres+=1;
													} 
														else if(subString.contentEquals("Global")) {
														sumSpheres+=0;
													} 
													
												}
												}
											}
					}
							
					}
		return sumSpheres;
	}


public static double getAverageCostForAllModelInstances(LinkedList<ProcessInstanceWithVoters>pInstances) {
	double cumulatedCost = 0; 
	for(ProcessInstanceWithVoters pInst: pInstances) {
		cumulatedCost = pInst.getCostForModelInstance();
	}
	
	return cumulatedCost/pInstances.size();
	
	
}

public static int getAmountTasks(BpmnModelInstance modelInstance) {
	int amountTasks = 0;
	for(Task t: modelInstance.getModelElementsByType(Task.class)) {
		amountTasks++;
	}
	return amountTasks;
}




}
