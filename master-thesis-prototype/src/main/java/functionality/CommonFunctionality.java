package functionality;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.impl.instance.FlowNodeRef;
import org.camunda.bpm.model.bpmn.instance.Association;
import org.camunda.bpm.model.bpmn.instance.BusinessRuleTask;
import org.camunda.bpm.model.bpmn.instance.Collaboration;
import org.camunda.bpm.model.bpmn.instance.DataInputAssociation;
import org.camunda.bpm.model.bpmn.instance.DataObject;
import org.camunda.bpm.model.bpmn.instance.DataObjectReference;
import org.camunda.bpm.model.bpmn.instance.DataOutputAssociation;
import org.camunda.bpm.model.bpmn.instance.Documentation;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Gateway;
import org.camunda.bpm.model.bpmn.instance.ItemAwareElement;
import org.camunda.bpm.model.bpmn.instance.Lane;
import org.camunda.bpm.model.bpmn.instance.LaneSet;
import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.Property;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.camunda.bpm.model.bpmn.instance.Text;
import org.camunda.bpm.model.bpmn.instance.TextAnnotation;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnPlane;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.camunda.bpm.model.bpmn.instance.dc.Bounds;
import org.camunda.bpm.model.bpmn.instance.di.Plane;
import org.camunda.bpm.model.bpmn.instance.di.Shape;
import org.camunda.bpm.model.bpmn.instance.di.Waypoint;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rits.cloning.Cloner;

import Mapping.BPMNBusinessRuleTask;
import Mapping.BPMNDataObject;
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
					LinkedList<LinkedList<FlowNode>> pathsBetweenWriterAndReader = CommonFunctionality
							.allPathsBetweenNodesWithCamundaNodes(writer, reader, new LinkedList<FlowNode>(),
									new LinkedList<FlowNode>(), new LinkedList<FlowNode>(),
									new LinkedList<LinkedList<FlowNode>>());
					LinkedList<LinkedList<FlowNode>> pathsBetweenReaderAndWriter = CommonFunctionality
							.allPathsBetweenNodesWithCamundaNodes(reader, writer, new LinkedList<FlowNode>(),
									new LinkedList<FlowNode>(), new LinkedList<FlowNode>(),
									new LinkedList<LinkedList<FlowNode>>());
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
		
		for(DataObjectReference daoR:modelInstance.getModelElementsByType(DataObjectReference.class)) {
			if(daoR.getId().contentEquals(iae.getId())) {
				return daoR;
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
		FlowNode pJoin = null;
		try {
			pJoin = CommonFunctionality.getCorrespondingGtw(modelInstance, pSplit);
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
		
		
		Iterator<LinkedList<FlowNode>> subPathsIter = subPaths.iterator();
		while(subPathsIter.hasNext()) {
			LinkedList<FlowNode>subPath = subPathsIter.next();
			if((!subPath.getLast().equals(endNode))&&(!subPath.contains(endNode))) {				
				subPathsIter.remove();				
			} 
			
			else if((!subPath.getLast().equals(endNode))&&subPath.contains(endNode)) {
				Iterator<FlowNode>subPathNodeIter = subPath.iterator();
				boolean remove = false;
				while(subPathNodeIter.hasNext()) {
					FlowNode currNode = subPathNodeIter.next();
					if(remove) {
						subPathNodeIter.remove();
						
					}
					
					if(currNode.equals(endNode)) {
						remove = true;
					}
					
					
				}
				
			}
			
		}
		
		
		Set<LinkedList<FlowNode>>set = new HashSet<LinkedList<FlowNode>>(subPaths);
		LinkedList<LinkedList<FlowNode>> noDups = new LinkedList<LinkedList<FlowNode>>(set);

		
		return noDups;
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
						FlowNode correspondingSplit = CommonFunctionality.getCorrespondingGtw(modelInstance, joinGtw);
						
						LinkedList<LinkedList<FlowNode>> pathsToCombine = new LinkedList<LinkedList<FlowNode>>();
						LinkedList<LinkedList<FlowNode>> pathsNotFullyBuilt = new LinkedList<LinkedList<FlowNode>>();

						for (LinkedList<FlowNode> path : paths) {
							if (path.contains(correspondingSplit) && path.getLast().equals(joinGtw)) {
								pathsToCombine.add(path);
							} else if (path.contains(correspondingSplit) && !path.getLast().equals(joinGtw)) {
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
	
	
	
	public static LinkedList<String> getGlobalSphereList(BpmnModelInstance modelInstance, boolean modelWithLanes) {	
		
		LinkedList<String>participants = new LinkedList<String>();

		if(!modelWithLanes) {
			//if the model is without lanes -> the participants must be extracted from the task name 
		for (Task task : modelInstance.getModelElementsByType(Task.class)) {
				
				
			String participantName = task.getName().substring(task.getName().indexOf("[")+1, task.getName().indexOf("]"));
			if(!participants.contains(participantName)) {
				participants.add(participantName);
			}
				
			}
		} else {
			// model is with lanes 
			for(Lane l: modelInstance.getModelElementsByType(Lane.class)) {
				participants.add(l.getName());
			}
			
			
			
		}
		return participants;

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


public static void increaseSpherePerDataObject(BpmnModelInstance modelInstance, String sphereToSet) throws IOException, ParserConfigurationException, SAXException {
	//sets the sphere of each data object in the model to the sphereToSet

	
			for (TextAnnotation tx : modelInstance.getModelElementsByType(TextAnnotation.class)) {
				
						if (tx.getTextContent().startsWith("Default")) {
			
						
									StringBuilder sb = new StringBuilder();
									sb.append(tx.getTextContent().substring(0, tx.getTextContent().indexOf('{')+1));
									
									
									sb.append(sphereToSet+'}');
									Text txt = modelInstance.newInstance(Text.class);
									txt.setTextContent(sb.toString());
									tx.setText(txt);
							
						
					
				
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

	
	public static void increaseVotersPerDataObject(BpmnModelInstance modelInstance, int currValue) throws IOException, ParserConfigurationException, SAXException {
		
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
									
								}
								
							}
						}

					}
				}
				
			}
			
		}
		
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
		if(fileName.contains(".bpmn")) {
			fileName = fileName.substring(0, fileName.indexOf(".bpmn"));
		}
		if(!suffixName.startsWith("_")) {
			suffixName = "_"+suffixName;
		}
		
		File dir = new File(directoryToStoreAnnotatedModel);		
		  File[] directoryListing = dir.listFiles();
		  boolean increaseFileNumber = false;
		  if (directoryListing != null) {
		    for (File child : directoryListing) {	
		    	if(child.getName().equals(fileName)&&child.getName().contains(".bpmn")) {
		    	String nameOfFileInsideDirectory = child.getName().substring(0, child.getName().indexOf(".bpmn"));	
		    	if(nameOfFileInsideDirectory.contains("_annotated")) {
		    	Pattern p = Pattern.compile("[0-9]+");
		    	Matcher m = p.matcher(nameOfFileInsideDirectory);
		    	while (m.find()) {
		    	    int num = Integer.parseInt(m.group());
		    	    if(num>fileNumber) {
		    	    	fileNumber = num;
		    	    	increaseFileNumber = true;
		    	    }
		    	}
		    }
		    }
		    }
		  }
		  String annotatedFileName = fileName+suffixName+".bpmn";
		  if(increaseFileNumber) {
				 fileNumber++; 
			 annotatedFileName = fileName+fileNumber+suffixName+".bpmn";

		  }
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
	public static List<LinkedList<Integer>> computeRepartitionNumberWithResultBound(int maxAmount, int subParts, int threshold_number, int amountResults) throws Exception {
	    
		 List<LinkedList<Integer>> resultRec = new LinkedList<LinkedList<Integer>>();
	        
		 if(resultRec.size()==amountResults) {
			 return resultRec;
		 }
	        
	        if (subParts == 1) {
	            List<LinkedList<Integer>> resultEnd = new LinkedList<LinkedList<Integer>>();
	            LinkedList<Integer> unitary = new LinkedList<>();
	            resultEnd.add(unitary);
	            unitary.add(maxAmount);
	            return resultEnd;
	        }

	        for (int i = threshold_number; i <= maxAmount-threshold_number; i++) {
	            int remain = maxAmount - i;
	            List<LinkedList<Integer>> partialRec = computeRepartitionNumberWithResultBound(remain, subParts - 1, threshold_number, amountResults);
	            for(List<Integer> subList : partialRec){
	                subList.add(i);             
	            }
	            resultRec.addAll(partialRec);
	        }
	        return resultRec;

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
								if(!values[0].contentEquals("Public")) {
								int currAmountVoters = Integer.parseInt(values[0]);
								sumAmountVoters+=currAmountVoters;
								}
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
		if(amountGtws==0) {
			return 0;
		}
		return ((double)sumAmountVoters/amountGtws);
	}
	
	
	
public static double getSphereSumOfModel(BpmnModelInstance modelInstance) {
	//a higher value will be a model with more strict decisions
		double sumSpheres = 0; 
		double amountBrts = 0; 
			HashMap<DataObjectReference, LinkedList<FlowNode>> readers = CommonFunctionality.getReadersForDataObjects(modelInstance);
				for(DataObjectReference dataO: readers.keySet()) {
					for(FlowNode f: readers.get(dataO)) {
						if(f instanceof BusinessRuleTask) {
							BusinessRuleTask brt = (BusinessRuleTask)f;
							for (TextAnnotation tx : modelInstance.getModelElementsByType(TextAnnotation.class)) {
								for(DataInputAssociation dia: brt.getDataInputAssociations()) {
									for(ItemAwareElement iae: dia.getSources()){
										if(iae.getId().contentEquals(dataO.getId())) {
											if (tx.getTextContent().startsWith("Default")) {
												
												amountBrts++;
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
											}
					}
							
					}
				if(amountBrts==0) {
					amountBrts=1;
				}
		return sumSpheres/amountBrts;
	}


public static double getAverageCostForAllModelInstances(LinkedList<ProcessInstanceWithVoters>pInstances) {
	double cumulatedCost = 0; 
	for(ProcessInstanceWithVoters pInst: pInstances) {
		cumulatedCost += pInst.getCostForModelInstance();
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


public static int calculatePossibleCombinationsForProcess(BpmnModelInstance modelInstance, boolean modelWithLanes) {
	int combinations = 1;
	if(modelInstance.getModelElementsByType(ExclusiveGateway.class).size()==0) {
		return 0;
	}
	
	
	int amountParticipants = CommonFunctionality.getGlobalSphere(modelInstance, modelWithLanes);
	
	for(BusinessRuleTask brt: modelInstance.getModelElementsByType(BusinessRuleTask.class)) {
		if(brt.getOutgoing().iterator().next().getTarget() instanceof ExclusiveGateway) {
			ExclusiveGateway gtw = (ExclusiveGateway) brt.getOutgoing().iterator().next().getTarget();
			if(gtw.getOutgoing().size()>=2) {
				for (TextAnnotation tx : modelInstance.getModelElementsByType(TextAnnotation.class)) {
					for (Association assoc : modelInstance.getModelElementsByType(Association.class)) {
						if (assoc.getSource().equals(gtw) && assoc.getTarget().equals(tx)) {
							if (tx.getTextContent().startsWith("[Voters]")) {
								
								String subString = tx.getTextContent().substring(tx.getTextContent().indexOf('{')+1, tx.getTextContent().indexOf('}'));
								String[]values = subString.split(",");
								
								int amountVoters = Integer.parseInt(values[0]);
								combinations *= binom(amountParticipants, amountVoters);
								
								
							}
						}
					}
				}
								
			}
			
		}
		
	}
	
	return combinations;
	
}

public static List<Integer> maxVoterCombsPerGatewayFromProcessDimension(double processDimensionSize, int amountDecisions, int globalSphere, int minVotersPerDecision, boolean equallyDistributed ) throws Exception {
	//calculate the amount of maximum voter combinations per gateway to fit the processDimensionSize
	//e.g. processDimensonSize 10000 with 4 gtws -> if equally distributed each decision has 10 voterCombinations which is e.g. binomial coefficient of 5 over 3
	//decisions should have 2 voters at least
	
	
	if(globalSphere<2) {
		throw new Exception("Model shoud have >= 2 participants!");
	}
	if(minVotersPerDecision<2) {
		throw new Exception("Model should have >= 2 voters per decision");
	}
	if(minVotersPerDecision>globalSphere) {
		throw new Exception("Minimum voters per decision can not be > than global sphere!");

	}
	
	LinkedList<Integer>votersPerGtw = new LinkedList<Integer>();
	if(equallyDistributed) {
		//each decision has the same size of voter combinations
		double equallyDistributedVoterCombs = Math.floor(processDimensionSize / amountDecisions);
			//n is the global sphere
			// k is the amount of voters for the gtw
			//find the k such that the binomial coefficient n over k is <= the equallyDistributedVoterCombs
		int prevK = 0; 
			for(int k = minVotersPerDecision; k <= globalSphere; k++) {				
				double combs = CommonFunctionality.binom(globalSphere, k);
				if(combs<=equallyDistributedVoterCombs) {
					prevK = k;
				} else {
					if(prevK==0) {
						throw new Exception("Voter will be 0!");
					}
					
					for(int size = 0; size <= globalSphere; size++) {
						votersPerGtw.add(prevK);
					}
					break;
				}
				
			}
		
		
	
	} else {
		//each gtw can have a different size of voters for the decision
		double minVoterCombsPerDecision = Math.floor(processDimensionSize / minVotersPerDecision);
		List<LinkedList<Integer>> voterCombsPerGtwLists = CommonFunctionality.computeRepartitionNumberWithResultBound((int)processDimensionSize, amountDecisions, (int)minVoterCombsPerDecision, 5);
		LinkedList<Integer> voterCombsPerGtwList = CommonFunctionality.getRandomItem(voterCombsPerGtwLists);
		
		for(int i = 0; i < voterCombsPerGtwList.size(); i++) {
			int currVotersComb = voterCombsPerGtwList.get(i);
			int prevK = 0; 
			for(int k = minVotersPerDecision; k <= globalSphere; k++) {				
				double combs = CommonFunctionality.binom(globalSphere, k);
				if(combs<=currVotersComb) {
					prevK = k;
				} else {
					if(prevK==0) {
						throw new Exception("Voter will be 0!");
					}
					votersPerGtw.add(prevK);
					break;
				}
				
			}			
		}
			
	}
	
	if(votersPerGtw.size()!=amountDecisions) {
		throw new Exception("Failed: VotersPerGtw!=amountDecisions!");
	}
	
		return votersPerGtw;


	
	
}



public static int binom(int n, int k) {
	if(n==k || k==0) {
		return 1;
	}
	
	return binom(n-1, k) + binom (n-1, k-1);
	
}




public static Object deepCopy(Object object) {
	try {
		Cloner cloner = new Cloner();
		Object clone = cloner.deepClone(object);
		return clone;
		
	} catch (Exception e) {
		e.printStackTrace();
		return null;
	}
	
	
}


public static FlowNode getFlowNodeByBPMNNodeId(BpmnModelInstance modelInstance, String id) {
	for (FlowNode flowNode : modelInstance.getModelElementsByType(FlowNode.class)) {
		if (flowNode.getId().equals(id)) {
			return flowNode;
		}
	}
	return null;
}


public static void removeBusinessRuleTask(BusinessRuleTask brt, BpmnModelInstance modelInstance) {
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
	for (TextAnnotation text : modelInstance.getModelElementsByType(TextAnnotation.class)) {
		for (Association a : modelInstance.getModelElementsByType(Association.class)) {

			if (text.getTextContent().startsWith("[Decision]") && a.getSource().equals(brt)
					&& text.getId().equals(a.getTarget().getId())) {
				for (BpmnEdge edge : modelInstance.getModelElementsByType(BpmnEdge.class)) {
					if (edge.getBpmnElement().equals(a)) {
						edge.getParentElement().removeChildElement(edge);
						break;
					}
				}
				a.getParentElement().removeChildElement(a);
				text.getParentElement().removeChildElement(text);
			}
		}
	}

	brt.getParentElement().removeChildElement(brt);

	for (BpmnShape bpmnS : modelInstance.getModelElementsByType(BpmnShape.class)) {
		if (bpmnS.getBpmnElement() == null) {
			bpmnS.getParentElement().removeChildElement(bpmnS);
		}
	}

}



public static void generateNewModelsWithAnnotatedChosenParticipants(API api, LinkedList<ProcessInstanceWithVoters> pInstances, int upperBoundNewModels, String directoryToStoreAnnotatedModels) {
	// call this method after the localMinimumAlgorithm has found the best
	// solution(s)
	// for each solution -> generate a new bpmn file
	// annotate the participants to the xor-splits
	// if amount participants generated for voting equals the participants in the
	// global sphere: mark the xor-split as private
	// if amount participants needed for voting > participants in the global sphere
	// or if it is marked as public already:
	// mark the xor-split as public
	
	String fileName = api.getProcessFile().getName();
	if(directoryToStoreAnnotatedModels == null || directoryToStoreAnnotatedModels.contentEquals("")) {
		//directory will be the same directory as the process file from the api
		directoryToStoreAnnotatedModels = api.getProcessFile().getParent();	
	}
	
	if(upperBoundNewModels<=0) {
		//all instances will be written to files
		upperBoundNewModels = pInstances.size();
	} 
	if(upperBoundNewModels>pInstances.size()) {
		upperBoundNewModels = pInstances.size();
	}
	
	int i = 1;

	for (int bound = 0; bound < upperBoundNewModels; bound++) {
		ProcessInstanceWithVoters pInst = pInstances.get(bound);
		//make a deep copy of the modelInstance to prevent changing the model itself
		BpmnModelInstance modelInstance = (BpmnModelInstance) CommonFunctionality.deepCopy(api.getModelInstance());

		for (Entry<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> entry : pInst.getVotersMap().entrySet()) {
			
			ObjectMapper mapper = new ObjectMapper();
			// Convert object to JSON string
			StringBuilder sbDecision = new StringBuilder();
			String jsonInString;
			try {
				jsonInString = mapper.writeValueAsString(entry.getKey().getDecisionEvaluation());
				sbDecision.append(jsonInString);
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Documentation doc = modelInstance.newInstance(Documentation.class);
			doc.setTextContent(sbDecision.toString());
			BusinessRuleTask brt = (BusinessRuleTask) CommonFunctionality.getFlowNodeByBPMNNodeId( modelInstance,entry.getKey().getId());
			brt.getDocumentations().add(doc);

			BPMNExclusiveGateway xorSplit = (BPMNExclusiveGateway) entry.getKey().getSuccessors().iterator().next();
			FlowNode gtw = CommonFunctionality.getFlowNodeByBPMNNodeId( modelInstance,xorSplit.getId());
			TextAnnotation sphere = null;

			if (xorSplit.getAmountVoters() == api.getGlobalSphereList().size()) {
				// annotate "private" to the xor-split
				sphere = modelInstance.newInstance(TextAnnotation.class);
				String textContent = "[Voters] {Private}";

				sphere.setTextFormat("text/plain");
				Text text = modelInstance.newInstance(Text.class);
				text.setTextContent(textContent);
				sphere.setText(text);
				gtw.getParentElement().addChildElement(sphere);

				// add the shape of the text annotation to the xml file
				CommonFunctionality.generateShapeForTextAnnotation(modelInstance, sphere, gtw);

			} else if (xorSplit.getAmountVoters() > api.getGlobalSphereList().size()) {
				// annotate "public" to the xor-split
				sphere = modelInstance.newInstance(TextAnnotation.class);
				String textContent = "[Voters] {Public}";

				sphere.setTextFormat("text/plain");
				Text text = modelInstance.newInstance(Text.class);
				text.setTextContent(textContent);
				sphere.setText(text);
				gtw.getParentElement().addChildElement(sphere);

				// add the shape of the text annotation to the xml file
				CommonFunctionality.generateShapeForTextAnnotation(modelInstance, sphere, gtw);

			} else {
				// annotate the chosen Participants to the xor-split that will be the voters
				sphere = modelInstance.newInstance(TextAnnotation.class);
				sphere.setTextFormat("text/plain");

				Text text = modelInstance.newInstance(Text.class);

				StringBuilder sb = new StringBuilder();
				sb.append("[Voters]{");
				for (BPMNParticipant participant : entry.getValue()) {
					sb.append(participant.getName() + ", ");
				}
				sb.deleteCharAt(sb.length() - 1);
				sb.deleteCharAt(sb.length() - 1);
				sb.append("}");
				text.setTextContent(sb.toString());

				sphere.setText(text);
				gtw.getParentElement().addChildElement(sphere);

				// add the shape of the text annotation to the xml file
				CommonFunctionality.generateShapeForTextAnnotation(modelInstance, sphere, gtw);

			}

			if (sphere != null) {
				Association assoc = modelInstance.newInstance(Association.class);
				assoc.setSource(gtw);
				assoc.setTarget(sphere);
				gtw.getParentElement().addChildElement(assoc);
				// DI element for the edge
				BpmnEdge edge = modelInstance.newInstance(BpmnEdge.class);
				edge.setBpmnElement(assoc);
				Waypoint wp1 = modelInstance.newInstance(Waypoint.class);
				wp1.setX(((Shape) gtw.getDiagramElement()).getBounds().getX() + 10);
				wp1.setY(((Shape) gtw.getDiagramElement()).getBounds().getY() + 10);
				Waypoint wp2 = modelInstance.newInstance(Waypoint.class);
				wp2.setX(((Shape) gtw.getDiagramElement()).getBounds().getX() - 50);
				wp2.setY(((Shape) gtw.getDiagramElement()).getBounds().getY() - 50);

				edge.getWaypoints().add(wp1);
				edge.getWaypoints().add(wp2);
				modelInstance.getModelElementsByType(Plane.class).iterator().next().addChildElement(edge);
			}

		}

		for (BusinessRuleTask brt : modelInstance.getModelElementsByType(BusinessRuleTask.class)) {
			Iterator<Association> assocIter = modelInstance.getModelElementsByType(Association.class).iterator();
			while (assocIter.hasNext()) {
				Association a = assocIter.next();
				for (TextAnnotation txt : modelInstance.getModelElementsByType(TextAnnotation.class)) {
					if (txt.getTextContent().startsWith("[Decision]") && a.getSource().equals(brt)
							&& txt.getId().equals(a.getTarget().getId())) {
						for (BpmnEdge edge : modelInstance.getModelElementsByType(BpmnEdge.class)) {
							if (edge.getBpmnElement().equals(a)) {
								edge.getParentElement().removeChildElement(edge);
							}
						}
						a.getParentElement().removeChildElement(a);
						txt.getParentElement().removeChildElement(txt);
						for (BpmnShape shape : modelInstance.getModelElementsByType(BpmnShape.class)) {
							if (shape.getBpmnElement() == null
									|| shape.getBpmnElement().getId().contentEquals(a.getId())) {
								shape.getParentElement().removeChildElement(shape);
							}
						}
					}

				}
			}

		}

		try {
			String suffix = "votingAsAnnotation-solution" + i;
			CommonFunctionality.writeChangesToFile(modelInstance, fileName, directoryToStoreAnnotatedModels, suffix);
			
		} catch (IOException | ParserConfigurationException | SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		i++;
	}

}


public static void generateShapeForTextAnnotation(BpmnModelInstance modelInstance, TextAnnotation txt, FlowNode ref) {
	BpmnShape shapeForAnnotation = modelInstance.newInstance(BpmnShape.class);
	shapeForAnnotation.setBpmnElement(txt);
	Bounds bounds = modelInstance.newInstance(Bounds.class);
	bounds.setX(getShape(modelInstance, ref.getId()).getBounds().getX() - 200);
	bounds.setY(getShape(modelInstance, ref.getId()).getBounds().getY() - 40);
	bounds.setWidth(284);
	bounds.setHeight(30);
	shapeForAnnotation.setBounds(bounds);
	modelInstance.getModelElementsByType(Plane.class).iterator().next().addChildElement(shapeForAnnotation);

}



public static void generateNewModelsWithVotersAsBpmnConstruct(API api, LinkedList<ProcessInstanceWithVoters> pInstances, int upperBoundNewModels,
		String directoryToStoreModels, boolean mapModelBtn) throws IOException {
	
	String fileName = api.getProcessFile().getName();
	if(directoryToStoreModels == null || directoryToStoreModels.contentEquals("")) {
		//directory will be the same directory as the process file from the api
		directoryToStoreModels = api.getProcessFile().getParent();	
	}
	
	if(upperBoundNewModels<=0) {
		//all instances will be written to files
		upperBoundNewModels = pInstances.size();
	} 
	if(upperBoundNewModels>pInstances.size()) {
		upperBoundNewModels = pInstances.size();
	}
	
	int i = 1;
	
	
	// when there is no default troubleshooter annotated - take a random participant
	BPMNParticipant troubleShooter = api.getTroubleShooter();
	if (troubleShooter == null) {
		troubleShooter = CommonFunctionality.getRandomItem(api.getGlobalSphereList());
	}

	int j = 1;
	for (int bound = 0; bound < upperBoundNewModels; bound++) {
		//make a deep copy of the modelInstance to prevent changing the model itself
	BpmnModelInstance modelInstance = (BpmnModelInstance) CommonFunctionality.deepCopy(api.getModelInstance());
	Bpmn.validateModel(modelInstance);

	ProcessInstanceWithVoters pInstance = pInstances.get(bound);
	Iterator<BPMNBusinessRuleTask> bpmnBusinessRtIterator = pInstance.getVotersMap().keySet().iterator();

	while (bpmnBusinessRtIterator.hasNext()) {
		BPMNBusinessRuleTask bpmnBusinessRt = bpmnBusinessRtIterator.next();
		BusinessRuleTask businessRt = (BusinessRuleTask) CommonFunctionality.getFlowNodeByBPMNNodeId(modelInstance, bpmnBusinessRt.getId());
		LinkedList<BPMNParticipant> votersForBrt = pInstance.getVotersMap().get(bpmnBusinessRt);
		BPMNExclusiveGateway bpmnEx = (BPMNExclusiveGateway) bpmnBusinessRt.getSuccessors().iterator().next();

		// builder doesn't work on task (need to be subclasses of task!)
		// convert the task to a user task to prevent error
		BPMNElement predecessorOfBpmnBrt = bpmnBusinessRt.getPredecessors().iterator().next();
		if (predecessorOfBpmnBrt.getClass() == Mapping.BPMNTask.class) {
			Task predec = (Task) CommonFunctionality.getFlowNodeByBPMNNodeId(modelInstance,predecessorOfBpmnBrt.getId());
			Bpmn.validateModel(modelInstance);
			UserTask userTask = modelInstance.newInstance(UserTask.class);
			userTask.setId(predec.getId().trim());
			userTask.setName(predec.getName().trim());
			userTask.getDataInputAssociations().addAll(predec.getDataInputAssociations());
			userTask.getDataOutputAssociations().addAll(predec.getDataOutputAssociations());
			userTask.getProperties().addAll(predec.getProperties());
			predec.replaceWithElement(userTask);
		}

		// check if there is only one participant selected for each data object of the
		// voting
		boolean onlyOneTask = false;
		if (votersForBrt.size() == 1) {
			onlyOneTask = true;
		}

		// Voting system inside of a subprocess
		if (!mapModelBtn) {

			if (!(onlyOneTask)) {
				BPMNParallelGateway.increaseVotingTaskCount();
				CommonFunctionality.addTasksToVotingSystem(api,modelInstance, i, businessRt, bpmnEx,
						CommonFunctionality.getFlowNodeByBPMNNodeId(modelInstance, bpmnBusinessRt.getPredecessors().iterator().next().getId())
								.builder().subProcess().embeddedSubProcess().startEvent(),
						"PV" + BPMNParallelGateway.getVotingTaskCount(),
						pInstance.getVotersMap().get(bpmnBusinessRt), troubleShooter,
						"PV" + BPMNParallelGateway.getVotingTaskCount(),
						BPMNExclusiveGateway.increaseExclusiveGtwCount(), mapModelBtn, onlyOneTask);
			} else {
				CommonFunctionality.addTasksToVotingSystem(api, modelInstance, i, businessRt, bpmnEx,
						businessRt.builder().moveToNode(bpmnBusinessRt.getPredecessors().iterator().next().getId()),
						"PV" + BPMNParallelGateway.getVotingTaskCount(),
						pInstance.getVotersMap().get(bpmnBusinessRt), troubleShooter,
						"PV" + BPMNParallelGateway.getVotingTaskCount(), 0, mapModelBtn, onlyOneTask);
			}
		}

		else {
			// Voting without having a subprocess
			if (!onlyOneTask) {
				BPMNParallelGateway.increaseVotingTaskCount();
				BPMNExclusiveGateway.increaseExclusiveGtwCount();
			}
			CommonFunctionality.addTasksToVotingSystem(api, modelInstance, i, businessRt, bpmnEx,
					businessRt.builder().moveToNode(bpmnBusinessRt.getPredecessors().iterator().next().getId()),
					"PV" + BPMNParallelGateway.getVotingTaskCount(), pInstance.getVotersMap().get(bpmnBusinessRt),
					troubleShooter, "PV" + BPMNParallelGateway.getVotingTaskCount(),
					BPMNExclusiveGateway.getExclusiveGtwCount(), mapModelBtn, onlyOneTask);

		}

		// Add the new tasks generated via fluent builder API to the corresponding lanes
		// in the xml model
		// Cant be done with the fluent model builder directly!

		for (Lane l : modelInstance.getModelElementsByType(Lane.class)) {
			for (Task task : modelInstance.getModelElementsByType(UserTask.class)) {
				if (task.getName().startsWith("VotingTask") && l.getName().equals(
						task.getName().substring(task.getName().indexOf(" ") + 1, task.getName().length()))) {
					// Add necessary information to the voting tasks

					if (mapModelBtn && task.getDocumentations().isEmpty()) {
						Documentation doc = modelInstance.newInstance(Documentation.class);
						StringBuilder sb = new StringBuilder();

						// add the decision of the businessruletask to the element documentation of the
						// voting tasks
						// use the Jackson converter to convert java object into json format

						ObjectMapper mapper = new ObjectMapper();
						// Convert object to JSON string
						String jsonInString = mapper.writeValueAsString(bpmnBusinessRt.getDecisionEvaluation());
						sb.append(jsonInString);
						// add a false rate for the voting tasks
						if (task.getName().startsWith("VotingTask")) {
							sb.deleteCharAt(sb.length() - 1);
							sb.append(",\"falseRate\":\"" + bpmnBusinessRt.getFalseRate() + "\"}");

						}
						doc.setTextContent(sb.toString());
						task.getDocumentations().add(doc);

					}

					// Put the voting tasks to the corresponding lanes in the xml model
					FlowNodeRef ref = modelInstance.newInstance(FlowNodeRef.class);
					ref.setTextContent(task.getId());
					FlowNode n = CommonFunctionality.getFlowNodeByBPMNNodeId(modelInstance, task.getId());
					if (!l.getFlowNodeRefs().contains(n)) {
						l.addChildElement(ref);
					}

				}
			}

		}		
		i++;

	}
		
	Iterator<BPMNBusinessRuleTask>chosenPInstIter = pInstance.getVotersMap().keySet().iterator();
	while(chosenPInstIter.hasNext()) {
		BPMNBusinessRuleTask brt = chosenPInstIter.next();
			BusinessRuleTask b = (BusinessRuleTask) CommonFunctionality.getFlowNodeByBPMNNodeId(modelInstance, brt.getId());
			if(b!=null) {
			CommonFunctionality.removeBusinessRuleTask(b, modelInstance);
			}			
	}
	try {
		if (mapModelBtn) {
			CommonFunctionality.mapModel(modelInstance);
		}
		String suffix= "votingAsConstruct-solution" + bound;
		CommonFunctionality.writeChangesToFile(modelInstance, fileName,directoryToStoreModels, suffix);
	} catch (ParserConfigurationException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (SAXException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	j++;
	}

	
	

}

private static void addTasksToVotingSystem(API api, BpmnModelInstance modelInstance, int i, BusinessRuleTask brt, BPMNExclusiveGateway bpmnEx,
		AbstractFlowNodeBuilder builder, String parallelSplit, LinkedList<BPMNParticipant> voters,
		BPMNParticipant troubleShooter, String parallelJoin, int exclusiveGtwCount, boolean mapModelBtn,
		boolean onlyOneTask) {
	if (voters.isEmpty()) {
		System.err.println("No voters selected");

	}

	// SequenceFlow connecting businessruletask and xor gtw
	SequenceFlow s = brt.getOutgoing().iterator().next();
	FlowNode targetGtw = s.getTarget();

	String exclusiveGatewayDeciderSplitId = "";
	String exclusiveGatewayDeciderJoinId = "";
	String exclusiveGatewayDeciderName = "";

	Iterator<BPMNParticipant> votersIter = voters.iterator();
	ArrayList<Task> alreadyModelled = new ArrayList<Task>();
	Set<BPMNDataObject> allBPMNDataObjects = new HashSet<BPMNDataObject>();

	allBPMNDataObjects.addAll(((BPMNBusinessRuleTask) CommonFunctionality.getNodeById(api.getProcessElements(),brt.getId())).getDataObjects());
	String parallelSplitId = parallelSplit + "_split";
	String parallelJoinId = parallelJoin + "_join";
	boolean isSet = false;

	// if there is only one user, than simply add one voting task without parallel
	// and xor splits
	if (onlyOneTask) {
		int votingTaskId = BPMNTask.increaseVotingTaskId();

		BPMNParticipant nextParticipant = votersIter.next();
		String serviceTaskId = "serviceTask_CollectVotes" + i;
		builder.userTask("Task_votingTask" + votingTaskId).name("VotingTask " + nextParticipant.getName())
				.serviceTask(serviceTaskId).name("Collect Votes").connectTo(targetGtw.getId());

		ServiceTask st = (ServiceTask) CommonFunctionality.getFlowNodeByBPMNNodeId(modelInstance, serviceTaskId);
		st.getOutgoing().iterator().next().getId();
		CommonFunctionality.addInformationToServiceTasks(modelInstance, st, (BPMNExclusiveGateway) CommonFunctionality.getNodeById(api.getProcessElements(),targetGtw.getId()), false);

		for (BPMNDataObject dao : allBPMNDataObjects) {
			CommonFunctionality.addDataInputReferencesToVotingTasks(modelInstance,
					(Task) CommonFunctionality.getFlowNodeByBPMNNodeId(modelInstance,"Task_votingTask" + votingTaskId), dao);
		}

	} else {

		String exclusiveGatewaySplitId = "EV" + exclusiveGtwCount + "_split";
		String exclusiveGatewayJoinId = "EV" + exclusiveGtwCount + "_join";
		String exclusiveGatewayName = "EV" + exclusiveGtwCount + "_loop";
		builder.exclusiveGateway(exclusiveGatewayJoinId).name(exclusiveGatewayName).parallelGateway(parallelSplitId)
				.name(parallelSplit);

		while (votersIter.hasNext()) {

			BPMNParticipant nextParticipant = votersIter.next();

			boolean skip = false;

			for (Task t : alreadyModelled) {
				if (t.getName().equals("VotingTask " + nextParticipant.getName())) {
					for (BPMNDataObject dao : allBPMNDataObjects) {
						CommonFunctionality.addDataInputReferencesToVotingTasks(modelInstance, t, dao);
					}
					skip = true;
				}
			}
			if (skip == false) {
				int votingTaskId = BPMNTask.increaseVotingTaskId();

				builder.moveToNode(parallelSplitId).userTask("Task_votingTask" + votingTaskId)
						.name("VotingTask " + nextParticipant.getName());
				alreadyModelled.add((Task) CommonFunctionality.getFlowNodeByBPMNNodeId(modelInstance, "Task_votingTask" + votingTaskId));
				for (BPMNDataObject dao : allBPMNDataObjects) {
					CommonFunctionality.addDataInputReferencesToVotingTasks(modelInstance,
							(Task) CommonFunctionality.getFlowNodeByBPMNNodeId(modelInstance, "Task_votingTask" + votingTaskId), dao);
				}

				if (isSet == false) {
					builder.moveToNode("Task_votingTask" + votingTaskId).parallelGateway(parallelJoinId)
							.name(parallelJoin);
					isSet = true;
				} else {
					builder.moveToNode("Task_votingTask" + votingTaskId).connectTo(parallelJoinId);
				}

			}
			if (!votersIter.hasNext()) {

				String serviceTaskId = "serviceTask_CollectVotes" + i;
				builder.moveToNode(parallelJoinId).serviceTask(serviceTaskId).name("Collect Votes")
						.exclusiveGateway(exclusiveGatewaySplitId).name(exclusiveGatewayName);

				builder.moveToNode(exclusiveGatewaySplitId).connectTo(exclusiveGatewayJoinId);

				FlowNode flowN = modelInstance.getModelElementById(exclusiveGatewaySplitId);
				for (SequenceFlow outgoing : flowN.getOutgoing()) {
					if (outgoing.getTarget().equals(modelInstance.getModelElementById(exclusiveGatewayJoinId))) {
						// to avoid overlapping of sequenceflow in diagram
						outgoing.setName("yes");
						CommonFunctionality.changeWayPoints(modelInstance, outgoing);
					}
				}

				CommonFunctionality.addInformationToServiceTasks(modelInstance, (ServiceTask) CommonFunctionality.getFlowNodeByBPMNNodeId(modelInstance, serviceTaskId),
						(BPMNExclusiveGateway) CommonFunctionality.getNodeById(api.getProcessElements(),targetGtw.getId()), true);

				// add the gateway for the troubleShooter

				String votingTaskName = "TroubleShooter " + troubleShooter.getName();

				BPMNExclusiveGateway.increaseExclusiveGtwCount();
				exclusiveGatewayDeciderSplitId = "EV" + BPMNExclusiveGateway.getExclusiveGtwCount() + "split";
				exclusiveGatewayDeciderJoinId = "EV" + BPMNExclusiveGateway.getExclusiveGtwCount() + "join";
				exclusiveGatewayDeciderName = "EV" + BPMNExclusiveGateway.getExclusiveGtwCount();
				String serviceTaskDeciderId = "serviceTask_CollectVotesDecider" + i;
				builder.moveToNode(exclusiveGatewaySplitId).exclusiveGateway(exclusiveGatewayDeciderSplitId)
						.name(exclusiveGatewayDeciderName)
						.userTask("Task_votingTask" + BPMNTask.increaseVotingTaskId()).name(votingTaskName)
						.serviceTask(serviceTaskDeciderId).name("Collect Votes")
						.exclusiveGateway(exclusiveGatewayDeciderJoinId).name(exclusiveGatewayDeciderName);
				CommonFunctionality.addInformationToServiceTasks(modelInstance, (ServiceTask) CommonFunctionality.getFlowNodeByBPMNNodeId(modelInstance, serviceTaskDeciderId),
						(BPMNExclusiveGateway) CommonFunctionality.getNodeById(api.getProcessElements(),targetGtw.getId()), false);

				for (BPMNDataObject dao : allBPMNDataObjects) {
					CommonFunctionality.addDataInputReferencesToVotingTasks(modelInstance, 
							(Task) CommonFunctionality.getFlowNodeByBPMNNodeId(modelInstance, "Task_votingTask" + BPMNTask.getVotingTaskId()),
							dao);
				}

			} else {
				if (votersIter.hasNext()) {
					builder.moveToNode(parallelSplitId);
				}
			}

			skip = false;

		}

		if (mapModelBtn) {
			builder.moveToNode(exclusiveGatewayDeciderJoinId).connectTo(targetGtw.getId());
			builder.moveToNode(exclusiveGatewayDeciderSplitId).connectTo(exclusiveGatewayDeciderJoinId);

		}

		else if (!mapModelBtn) {

			builder.moveToNode(exclusiveGatewayDeciderJoinId).endEvent("endEventWithInSubProcess" + i).name("")
					.subProcessDone().connectTo(targetGtw.getId());
			builder.moveToNode(exclusiveGatewayDeciderSplitId).connectTo(exclusiveGatewayDeciderJoinId);
		}

		FlowNode flowNo = modelInstance.getModelElementById(exclusiveGatewayDeciderSplitId);
		SequenceFlow incomingSeq = flowNo.getIncoming().iterator().next();
		incomingSeq.setName("no");
		for (SequenceFlow outgoingSeq : flowNo.getOutgoing()) {
			if (outgoingSeq.getTarget() instanceof UserTask) {
				outgoingSeq.setName("yes");
			} else {
				outgoingSeq.setName("no");
				CommonFunctionality.changeWayPoints(modelInstance, outgoingSeq);
			}
		}

	}

}

public static BPMNElement getNodeById(List<BPMNElement> processElements, String id) {
	for (BPMNElement e : processElements) {
		if (e.getId().equals(id)) {
			return e;
		}
	}
	return null;
}

public static void mapModel(BpmnModelInstance modelInstance) {

	Iterator<Lane> laneIter = modelInstance.getModelElementsByType(Lane.class).iterator();
	while (laneIter.hasNext()) {
		Lane nextLane = laneIter.next();
		for (FlowNode flowNode : nextLane.getFlowNodeRefs()) {
			for (TextAnnotation txt : modelInstance.getModelElementsByType(TextAnnotation.class)) {
				for (Association a : modelInstance.getModelElementsByType(Association.class)) {
					if (flowNode instanceof ExclusiveGateway) {
						// remove XOR-Annotations for the amount of participants needed
						// remove Decision-Annotations for BusinessRuleTasks

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
					} else if (flowNode.getName().startsWith("TroubleShooter")) {
						flowNode.setAttributeValue("name", "TroubleShooter [" + nextLane.getName() + "]");
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
			lane.setBpmnElement(modelInstance.getModelElementsByType(Process.class).iterator().next());
		}
	}

}

public static void addInformationToServiceTasks(BpmnModelInstance modelInstance, ServiceTask st, BPMNExclusiveGateway xor, boolean votingTask) {
	st.setCamundaType("external");
	st.setCamundaTopic("voting");
	Documentation dataObjectDocu = modelInstance.newInstance(Documentation.class);
	StringBuilder sb = new StringBuilder();
	if (votingTask) {
		sb.append("{\"gateway\":\"" + st.getOutgoing().iterator().next().getTarget().getName() + "\"");
		sb.append(",\"sameDecision\":" + "\"" + xor.getVotersSameChoice() + "\"" + ",\"loops\":" + "\""
				+ xor.getAmountLoops() + "\"");
	} else {
		sb.append("{\"gateway\":\"" + xor.getName() + "\"");
	}
	sb.append("}");
	dataObjectDocu.setTextContent(sb.toString());
	st.getDocumentations().add(dataObjectDocu);
}



private static void addDataInputReferencesToVotingTasks(BpmnModelInstance modelInstance, Task task, BPMNDataObject dataObject) {
	boolean alreadyModelled = false;
	// check whether there is already a DataInputAssociation between the task and
	// the dataObject
	for (DataInputAssociation di : task.getDataInputAssociations()) {
		for (ItemAwareElement item : di.getSources()) {
			if (item.getAttributeValue("dataObjectRef").equals(dataObject.getId())) {
				alreadyModelled = true;
			}
		}
	}

	if (task.getDataInputAssociations().isEmpty() || alreadyModelled == false) {
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

public static void changeWayPoints(BpmnModelInstance modelInstance, SequenceFlow seqFlow) {
	BpmnEdge edge = seqFlow.getDiagramElement();
	Object[] wpArray = edge.getWaypoints().toArray();
	Waypoint wp1 = (Waypoint) wpArray[0];
	Waypoint wp2 = (Waypoint) wpArray[1];
	Collection<Waypoint> collWp = new ArrayList<Waypoint>();
	Waypoint wpToBeInserted1 = modelInstance.newInstance(Waypoint.class);
	Waypoint wpToBeInserted2 = modelInstance.newInstance(Waypoint.class);
	Waypoint wpToBeInserted3 = modelInstance.newInstance(Waypoint.class);
	Waypoint wpToBeInserted4 = modelInstance.newInstance(Waypoint.class);

	if (wp1.getX() > wp2.getX()) {
		double wpX = (wp1.getX() - 25.0);
		double wpY = (wp1.getY() - 25.0);
		wpToBeInserted1.setX(wpX);
		wpToBeInserted1.setY((wpY));
		wpToBeInserted2.setX(wpX);
		wpToBeInserted2.setY((wpY - 61.0));
		double wp2X = (wp2.getX() + 25.0);
		double wp2Y = (wp2.getY() - 25.0);
		wpToBeInserted4.setX(wp2X);
		wpToBeInserted4.setY((wp2Y));
		wpToBeInserted3.setX(wp2X);
		wpToBeInserted3.setY((wp2Y - 61.0));
	} else {
		Waypoint wp3 = (Waypoint) wpArray[2];
		wpToBeInserted1.setX(wp1.getX());
		wpToBeInserted1.setY((wp1.getY()));
		wpToBeInserted2.setX(wp1.getX());
		wpToBeInserted2.setY((wp1.getY() + 61.0));
		wpToBeInserted3.setX((wp3.getX() + 25.0));
		wpToBeInserted3.setY((wp1.getY() + 61.0));
		wpToBeInserted4.setX(wp3.getX() + 25.0);
		wpToBeInserted4.setY(wp1.getY());
	}
	collWp.add(wpToBeInserted1);
	collWp.add(wpToBeInserted2);
	collWp.add(wpToBeInserted3);
	collWp.add(wpToBeInserted4);
	edge.getWaypoints().removeAll(edge.getWaypoints());
	edge.getWaypoints().addAll(collWp);

}

public static String compareResultsOfAlgorithmsForDifferentAPIs(LinkedList<ProcessInstanceWithVoters> localMinInstances,
		LinkedList<ProcessInstanceWithVoters> bruteForceInstances, int boundForComparisons) {
	
	try {
	if(localMinInstances == null || bruteForceInstances == null) {
		return "null";
	}
	if(localMinInstances.isEmpty() || bruteForceInstances.isEmpty()) {
		return "null";
	}
		
	int countCheapestSolutionFoundInBruteForceSolutions = 0;
	LinkedList<ProcessInstanceWithVoters> cheapestBruteForceSolutions = CommonFunctionality
			.getCheapestProcessInstancesWithVoters(bruteForceInstances);
	
	//to avoid very long taking comparisons -> only check if boundForComparisons is reached
	if((localMinInstances.size()*cheapestBruteForceSolutions.size())>boundForComparisons) {
		
		if(cheapestBruteForceSolutions.get(0).getCostForModelInstance()==localMinInstances.get(0).getCostForModelInstance()) {
			return "true";
		
		} else {
			return "false";
		}
		
		
	}
	

		for (ProcessInstanceWithVoters cheapestInstBruteForce : cheapestBruteForceSolutions) {
			for (ProcessInstanceWithVoters cheapestInstLocalMin : localMinInstances) {
			if (cheapestInstBruteForce
					.getCostForModelInstance() == (cheapestInstLocalMin.getCostForModelInstance())) {
				boolean count = true;
				for(Entry<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>bruteForceEntry: cheapestInstBruteForce.getVotersMap().entrySet()) {
					for(Entry<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>localMinEntry: cheapestInstLocalMin.getVotersMap().entrySet()) {
						if(bruteForceEntry.getKey().getId().contentEquals(localMinEntry.getKey().getId())) {
							int size = bruteForceEntry.getValue().size();
							int entryEqual = 0;
							for(BPMNParticipant bruteForcePart: bruteForceEntry.getValue()) {
								for(BPMNParticipant localMinPart: localMinEntry.getValue()) {
									if(bruteForcePart.getId().contentEquals(localMinPart.getId())) {
										entryEqual++;
									}
									
								}								
							}
							if(entryEqual!=size) {
								count=false;
							}
							
						}
					
					
				
				}
				
				
			}
				if(count) {
					countCheapestSolutionFoundInBruteForceSolutions++;
					if(countCheapestSolutionFoundInBruteForceSolutions==localMinInstances.size()) {
						return "true";						
					}
				}

		}

	}
		
	}
	
	if (countCheapestSolutionFoundInBruteForceSolutions == localMinInstances.size()) {
		return "true";
	}  else {
		System.out.println("CountCheapestSolutionFoundInBruteForce"+countCheapestSolutionFoundInBruteForceSolutions+", "+localMinInstances.size());
		return "false";
	}
	} catch(NullPointerException ex) {
		return "null";
	}

}



public static LinkedList<ProcessInstanceWithVoters> getCheapestProcessInstancesWithVoters(
		LinkedList<ProcessInstanceWithVoters> pInstances) {
	if(pInstances==null) {
		return null;
	}
	List<ProcessInstanceWithVoters> allInstSortedByCheapest = pInstances.parallelStream()
			.sorted((Comparator.comparingDouble(ProcessInstanceWithVoters::getCostForModelInstance)))
			.collect(Collectors.toList());
	// allInstSortedByCheapest contains all solutions sorted by cheapest ones

	LinkedList<ProcessInstanceWithVoters> allCheapestInst = new LinkedList<ProcessInstanceWithVoters>();
	allCheapestInst.add(allInstSortedByCheapest.get(0));

	for (int i = 1; i < allInstSortedByCheapest.size(); i++) {
		ProcessInstanceWithVoters currInst = allInstSortedByCheapest.get(i);
		if (allCheapestInst.getFirst().getCostForModelInstance() == currInst.getCostForModelInstance()) {
			allCheapestInst.add(currInst);
		} else {
			return allCheapestInst;
		}
	}

	return allCheapestInst;
}

public static void generateNewModelsUntilGlobalSphereReached(File model,int globalSphereLowerBound,  int amountNewProcessesToCreatePerIteration,  String directoryToStore) {
	
	String fileNameWithoutExtension = model.getName().replace(".bpmn", "").trim();
	BpmnModelInstance modelInstance = Bpmn.readModelFromFile(model);
	int globalSphereUpperBound = modelInstance.getModelElementsByType(Task.class).size();
	
	
	
	while(globalSphereLowerBound<=globalSphereUpperBound) {
		//globalSphereLowerBound = amount of different participants for this model
		//globalSphereLowerBound e.g. 3 -> create amountNewProcessesToCreatePerIteration new Models where all tasks of the model have one of the 3 participants connected
		
		for(int iteration = 0; iteration < amountNewProcessesToCreatePerIteration; iteration++) {
			String suffix = "lb"+globalSphereLowerBound+"ub"+globalSphereUpperBound+"iter"+iteration;	
			LinkedList<String>participantNames = new LinkedList<String>();
			for(int i = 1; i <=globalSphereLowerBound; i++) {
				String participantName = "Participant_"+i;
				participantNames.add(participantName);
			}
					
			BpmnModelInstance cloneModel = (BpmnModelInstance) CommonFunctionality.deepCopy(modelInstance);
			LinkedList<Task>tasksToChooseFrom = new LinkedList<Task>();
					tasksToChooseFrom.addAll(cloneModel.getModelElementsByType(Task.class));
			Collections.shuffle(tasksToChooseFrom);
					
					
			Iterator<Task>taskIter = tasksToChooseFrom.iterator();
			LinkedList<String>participantsNeededToBeChosen = new LinkedList<String>();
			participantsNeededToBeChosen.addAll(participantNames);
			
			
			while(taskIter.hasNext()) {
				Task currTask = taskIter.next();
				String currTaskName = currTask.getName();

				Iterator<String>participantIter =participantsNeededToBeChosen.iterator();
				if(participantIter.hasNext()) {
					//first assign the participant once to a task to match the globalSphereLowerBound
				String newParticipant = participantIter.next();					
				 String newTaskName = currTaskName.replaceAll("(?<=\\[).*?(?=\\])", newParticipant);
				 currTask.setName(newTaskName);
				 
				 participantIter.remove();
				 
				} else {
					//choose a random participant
					String newParticipant = CommonFunctionality.getRandomItem(participantNames);
					String newTaskName = currTaskName.replaceAll("(?<=\\[).*?(?=\\])", newParticipant);
					currTask.setName(newTaskName);
					
				}
							
			}
					
			try {
				CommonFunctionality.writeChangesToFile(cloneModel, fileNameWithoutExtension, directoryToStore, suffix);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		globalSphereLowerBound++;
		
	}
	
	
	
}




public static void substituteOneDataObjectPerIterationAndWriteNewModels(BpmnModelInstance modelInstance, DataObjectReference substitute,String fileName, String directoryToStore){
	//choose a dataObject from a decision and substitute it
	//if decision already has the substitute -> remove the random dataObject
	//generate new model on each iteration	
	
	Collection<BusinessRuleTask>brtList = modelInstance.getModelElementsByType(BusinessRuleTask.class);
	
	Iterator<BusinessRuleTask>brtIter = brtList.iterator();
	
	
	while(brtIter.hasNext()) {
		BusinessRuleTask brt = brtIter.next();
		boolean substituteAnnotatedAlready = false;
		LinkedList<DataObjectReference> toRemove = new LinkedList<DataObjectReference>();
		for(DataInputAssociation dia: brt.getDataInputAssociations()) {
			for(ItemAwareElement iae: dia.getSources()) {
				DataObjectReference daoR = CommonFunctionality.getDataObjectReferenceForItemAwareElement(modelInstance, iae);
				if(daoR.getId().equals(substitute.getId())) {
					substituteAnnotatedAlready=true;
				} else {
					if(!toRemove.contains(daoR)) {
					toRemove.add(daoR);
					}
				}
							
			}		
		}
		
		int iteration = 0;

			while(!toRemove.isEmpty()) {
		
			//choose random dataObject to remove
			DataObjectReference daoRToBeRemoved = CommonFunctionality.getRandomItem(toRemove);
			Iterator<DataInputAssociation>diaIter = brt.getDataInputAssociations().iterator();
			while(diaIter.hasNext()) {
				DataInputAssociation dia = diaIter.next();
				for(ItemAwareElement iae: dia.getSources()) {
					DataObjectReference daoR = CommonFunctionality.getDataObjectReferenceForItemAwareElement(modelInstance, iae);
					if(daoR.getId().equals(daoRToBeRemoved.getId())) {
						brt.getDataInputAssociations().remove(dia);
						for (BpmnEdge bpmnE : modelInstance.getModelElementsByType(BpmnEdge.class)) {
							if (bpmnE.getBpmnElement() == null) {
								bpmnE.getParentElement().removeChildElement(bpmnE);
							}
						}

						
						//iterate through all tasks that have the dataObject connected
						//remove the dataObjectReference
						LinkedList<Task>taskWithInputAssocRemoved = new LinkedList<Task>();
						LinkedList<Task>taskWithOutputAssocRemoved = new LinkedList<Task>();
						for(Task task: modelInstance.getModelElementsByType(Task.class)) {
							for(DataInputAssociation taskDia: task.getDataInputAssociations()) {
								for(ItemAwareElement taskIae: taskDia.getSources()) {
									if(taskIae.getId().contentEquals(daoRToBeRemoved.getId())) {
										if(CommonFunctionality.removeTaskAsReaderFromDataObject(modelInstance, task, taskIae)) {
											taskWithInputAssocRemoved.add(task);
										}
										
									}
								}
							}
							for(DataOutputAssociation taskDao: task.getDataOutputAssociations()) {
								ItemAwareElement taskIae = taskDao.getTarget();
								if(taskIae.getId().contentEquals(daoRToBeRemoved.getId())) {
									if(CommonFunctionality.removeTaskAsWriterForDataObject(modelInstance, task, taskIae)) {
										taskWithOutputAssocRemoved.add(task);
									}
								}
							}
						}
						
						
						if(substituteAnnotatedAlready==false) {
							//connect the substitute 
							DataInputAssociation diaToConnect = modelInstance.newInstance(DataInputAssociation.class);
							Property p1 = modelInstance.newInstance(Property.class);
							p1.setName("__targetRef_placeholder");
							brt.addChildElement(p1);
							diaToConnect.setTarget(p1);
							brt.getDataInputAssociations().add(diaToConnect);
							diaToConnect.getSources().add(substitute);
							CommonFunctionality.generateDIElementForReader(modelInstance, diaToConnect, CommonFunctionality.getShape(modelInstance, substitute.getId()), CommonFunctionality.getShape(modelInstance, brt.getId()));
							
							for(Task t: taskWithInputAssocRemoved) {
								DataInputAssociation toConnect = modelInstance.newInstance(DataInputAssociation.class);
								Property p2 = modelInstance.newInstance(Property.class);
								p2.setName("__targetRef_placeholder");
								t.addChildElement(p2);
								toConnect.setTarget(p2);
								t.getDataInputAssociations().add(toConnect);
								toConnect.getSources().add(substitute);
								CommonFunctionality.generateDIElementForReader(modelInstance, toConnect, CommonFunctionality.getShape(modelInstance, substitute.getId()), CommonFunctionality.getShape(modelInstance, t.getId()));

							
							}
						
						}
						
						
						BpmnShape shape =CommonFunctionality.getShape(modelInstance, daoRToBeRemoved.getId());
						shape.getParentElement().removeChildElement(shape);		
						//daoRToBeRemoved.getParentElement().removeChildElement(daoRToBeRemoved);
						iteration++;

						try {
							CommonFunctionality.writeChangesToFile(modelInstance, fileName, directoryToStore, iteration+"");
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (ParserConfigurationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (SAXException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}
				}
			}
			
		
						}
			
	
	}
		
}


private static void generateDIElementForReader(BpmnModelInstance modelInstance, DataInputAssociation dia, BpmnShape daoR, BpmnShape readerTaskShape) {
	BpmnEdge e = modelInstance.newInstance(BpmnEdge.class);
	e.setBpmnElement(dia);
	Waypoint wp = modelInstance.newInstance(Waypoint.class);
	// Waypoints for the source -> the Data Object
	wp.setX(daoR.getBounds().getX());
	wp.setY(daoR.getBounds().getY());
	e.addChildElement(wp);

	// Waypoint for the target -> the Task that has the Data Input
	Waypoint wp2 = modelInstance.newInstance(Waypoint.class);

	wp2.setX(readerTaskShape.getBounds().getX());
	wp2.setY(readerTaskShape.getBounds().getY());
	e.addChildElement(wp2);

	modelInstance.getModelElementsByType(Plane.class).iterator().next().addChildElement(e);
}




public static void addExcludeParticipantConstraintsOnModel(BpmnModelInstance modelInstance, String modelName, int probabilityForGatewayToHaveConstraint,
		int lowerBoundAmountParticipantsToExclude,  boolean alwaysMaxConstrained,  boolean modelWithLanes, String directoryToStore) throws Exception {
	// upperBoundAmountParticipantsToExclude is the difference between the amount of
	// needed voters and the global Sphere
	// e.g. global sphere = 5, 3 people needed -> 2 is the max amount of
	// participants to exclude
	// the decision taker himself can not be excluded
	
	boolean constraintInserted = false;
	if(lowerBoundAmountParticipantsToExclude < 1) {
		lowerBoundAmountParticipantsToExclude = 1;
	}

	for (ExclusiveGateway gtw :modelInstance.getModelElementsByType(ExclusiveGateway.class)) {
		if(gtw.getIncoming().size()==1) {
		int randomInt = ThreadLocalRandom.current().nextInt(1, 101);
		if (probabilityForGatewayToHaveConstraint >= randomInt) {
			BusinessRuleTask brtBeforeGtw = (BusinessRuleTask) gtw.getIncoming().iterator().next().getSource();
			String decisionTakerName = brtBeforeGtw.getName();
			LinkedList<String> participantsToChooseFrom = CommonFunctionality.getGlobalSphereList(modelInstance, modelWithLanes);
			int globalSphereSize = participantsToChooseFrom.size();
			
			// remove the decision taker from the list
			for (String part : participantsToChooseFrom) {
				if (decisionTakerName.contains(part)) {
					participantsToChooseFrom.remove(part);
					break;
				}

			}

			if (!participantsToChooseFrom.isEmpty()) {

				for (TextAnnotation tx : modelInstance.getModelElementsByType(TextAnnotation.class)) {
					for (Association assoc : modelInstance.getModelElementsByType(Association.class)) {
						if (assoc.getSource().equals(gtw) && assoc.getTarget().equals(tx)) {
							if (tx.getTextContent().startsWith("[Voters]")) {
								String subStr = tx.getTextContent().substring(tx.getTextContent().indexOf('{') + 1, tx.getTextContent().indexOf('}'));

								String[] data = subStr.split(",");
								int amountVotersNeeded = Integer.parseInt(data[0]);
								int randomAmountConstraintsForGtw = 0;
								
										
								if(alwaysMaxConstrained) {
									randomAmountConstraintsForGtw = globalSphereSize - amountVotersNeeded;									
								} else {
									int maxConstraint = globalSphereSize - amountVotersNeeded;	
									randomAmountConstraintsForGtw = ThreadLocalRandom.current().nextInt(
											lowerBoundAmountParticipantsToExclude,
											maxConstraint + 1);
								}
								
															
								Collections.shuffle(participantsToChooseFrom);
								Iterator<String> partIter = participantsToChooseFrom.iterator();

								for(int i = 0; i < randomAmountConstraintsForGtw; i++) {
									if(partIter.hasNext()) {
									String currPart = partIter.next();
									
									String excludeParticipantConstraint = "[ExcludeParticipantConstraint] {"+currPart+"}";
									TextAnnotation constraintAnnotation = modelInstance.newInstance(TextAnnotation.class);
								
									constraintAnnotation.setTextFormat("text/plain");
									Text text = modelInstance.newInstance(Text.class);
									text.setTextContent(excludeParticipantConstraint);
									constraintAnnotation.setText(text);
									gtw.getParentElement().addChildElement(constraintAnnotation);
				
									CommonFunctionality.generateShapeForTextAnnotation(modelInstance, constraintAnnotation, brtBeforeGtw);
									
									Association association = modelInstance.newInstance(Association.class);
									association.setSource(gtw);
									association.setTarget(constraintAnnotation);
									gtw.getParentElement().addChildElement(association);
									
									
									BpmnEdge edge = modelInstance.newInstance(BpmnEdge.class);
									edge.setBpmnElement(association);
									Waypoint wp1 = modelInstance.newInstance(Waypoint.class);
									wp1.setX(gtw.getDiagramElement().getBounds().getX() + 50);
									wp1.setY(gtw.getDiagramElement().getBounds().getY());
									Waypoint wp2 = modelInstance.newInstance(Waypoint.class);
									wp2.setX(gtw.getDiagramElement().getBounds().getX() + 50);
									wp2.setY(gtw.getDiagramElement().getBounds().getY() - 50);

									edge.getWaypoints().add(wp1);
									edge.getWaypoints().add(wp2);
									modelInstance.getModelElementsByType(Plane.class).iterator().next().addChildElement(edge);
									constraintInserted = true;
									partIter.remove();
									
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
	if(constraintInserted) {
		String suffix = "";
	if(alwaysMaxConstrained) {
		suffix = "alwMaxConstrained";
	} else if(!alwaysMaxConstrained){
		suffix = "constrained";
	} 
	CommonFunctionality.writeChangesToFile(modelInstance, modelName, directoryToStore, suffix);
	}
}


}
