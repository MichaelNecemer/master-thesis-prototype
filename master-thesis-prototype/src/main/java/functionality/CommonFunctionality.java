package functionality;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.DataInputAssociation;
import org.camunda.bpm.model.bpmn.instance.DataObjectReference;
import org.camunda.bpm.model.bpmn.instance.DataOutputAssociation;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Gateway;
import org.camunda.bpm.model.bpmn.instance.ItemAwareElement;
import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.camunda.bpm.model.bpmn.instance.di.Plane;
import org.camunda.bpm.model.bpmn.instance.di.Waypoint;

import Mapping.BPMNElement;
import Mapping.BPMNExclusiveGateway;
import Mapping.BPMNParallelGateway;
import Mapping.Combination;
import ProcessModelGeneratorAndAnnotater.LockedBranch;
import ProcessModelGeneratorAndAnnotater.ProcessModellAnnotater;

public class CommonFunctionality {

	public static boolean isCorrectModel(BpmnModelInstance modelInstance) {
		// does correctness checking of the process model
		// e.g. 1 Start and 1 End Event
		boolean correctModel = true;
		if (CommonFunctionality.checkIfOnlyOneStartEventAndEventEvent(modelInstance)) {

		} else {
			System.err.println("There must be 1 Start and 1 End Event!");
			correctModel = false;
		}

		/*
		 * if(!CommonFunctionality.checkCorrectnessOfBranches(modelInstance)) {
		 * correctModel = false; }
		 */
		LinkedList<LinkedList<FlowNode>> paths = CommonFunctionality.getAllPaths(modelInstance,
				modelInstance.getModelElementsByType(StartEvent.class).iterator().next(),
				modelInstance.getModelElementsByType(EndEvent.class).iterator().next(), new LinkedList<FlowNode>(),
				new LinkedList<Gateway>(), new LinkedList<FlowNode>(), new LinkedList<LinkedList<FlowNode>>(),
				new LinkedList<LinkedList<FlowNode>>());
		int i = 1;
		for (LinkedList<FlowNode> path : paths) {
			System.out.println("Path: " + i);
			for (FlowNode f : path) {
				System.out.println(f.getId());
			}
			i++;
		}

		System.out.println("Amount paths " + paths.size());
		System.out.println("has duplicates: "+CommonFunctionality.hasduplicateList(paths));

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
		System.out.println(pSplit.getId());

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
			for(LinkedList<FlowNode> f: entry.getValue()) {
				System.out.println("size: "+f.size());
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

	public static LinkedList<LinkedList<FlowNode>> getAllPaths(BpmnModelInstance modelInstance, FlowNode startNode,
			FlowNode endNode, LinkedList<FlowNode> queue, LinkedList<Gateway> openSplits,
			LinkedList<FlowNode> currentPath, LinkedList<LinkedList<FlowNode>> paths,
			LinkedList<LinkedList<FlowNode>> parallelBranches) {
		// go DFS inside all branch till corresponding join is found
		queue.add(startNode);

		while (!(queue.isEmpty())) {
			FlowNode element = queue.poll();
			System.out.println("Element: " + element.getName() + ", " + element.getId());

			currentPath.add(element);
			boolean toBeCombined = false;

			if (element.getId().equals(endNode.getId())) {

				for (Gateway gtw : openSplits) {
					if (gtw instanceof ParallelGateway) {
						toBeCombined = true;
						break;
					}
				}

				if (toBeCombined == true) {
					//currentPath is part of a parallel branch
					parallelBranches.add(currentPath);
					

				} else {

					paths.add(currentPath);
				}

				if (endNode instanceof Gateway && endNode.getIncoming().size() == 2) {

					Gateway joinGtw = (Gateway) element;

					// when a join is found - poll the last opened gateway from the stack
					Gateway lastOpenedSplitGtw = openSplits.pollLast();

					if (!openSplits.contains(lastOpenedSplitGtw)) {
						// when the openSplitStack does not contain the lastOpenedSplit anymore, all
						// branches to the joinGtw have been visited
						// go from joinGtw to the Join of the last opened split in the stack, if there
						// is any

						// if lastOpenedSplit was a parallelGtw -> get possible combinations of paths
						// within the branches
						if (lastOpenedSplitGtw instanceof ParallelGateway) {
							// -> check if each path containing the corresponding split of the parallelJoinGtw has the joinGtw as last element!
							// else there are paths inside the branch that are not a fully built yet
							
							String idOfSplit = joinGtw.getName()+"_split";
							FlowNode splitGtw = modelInstance.getModelElementById(idOfSplit);
							
							
							LinkedList<LinkedList<FlowNode>> pathsToCombine = new LinkedList<LinkedList<FlowNode>>();
							LinkedList<LinkedList<FlowNode>> pathsNotFullyBuilt = new LinkedList<LinkedList<FlowNode>>();

							for (LinkedList<FlowNode> path : parallelBranches) {
								if (path.contains(splitGtw)&&path.getLast().equals(joinGtw)) {
									pathsToCombine.add(path);
								} else {
									pathsNotFullyBuilt.add(path);
								}
							}
							for (LinkedList<FlowNode> p : pathsToCombine) {
								System.out.println("PathsToCombine: "+p.size());
							}
							
							if(pathsNotFullyBuilt.isEmpty()) {
							LinkedList<LinkedList<FlowNode>> combinedPaths = CommonFunctionality
									.combineParallelBranches(modelInstance, pathsToCombine,
											(ParallelGateway) lastOpenedSplitGtw);

							Iterator<LinkedList<FlowNode>> pathsIter = paths.iterator();
							
							while(pathsIter.hasNext()) {
								LinkedList<FlowNode> currPath = pathsIter.next();
								boolean isSubList = CommonFunctionality.isSubListOfEachList(currPath, combinedPaths);
								if (isSubList) {
									pathsIter.remove();
								}

							}
							paths.addAll(combinedPaths);
							
							Iterator<LinkedList<FlowNode>> pBranchIter = parallelBranches.iterator();
							
							while(pBranchIter.hasNext()) {
								LinkedList<FlowNode>pBranch = pBranchIter.next();
								boolean isSubList = CommonFunctionality.isSubListOfEachList(pBranch, combinedPaths);
								if (isSubList) {
									pBranchIter.remove();
								}
							}							

							if (!parallelBranches.isEmpty()) {
								parallelBranches.addAll(combinedPaths);
							}
							} else {
								//there are branches that need to be visited
								System.out.println("Branches need to be visited! "+pathsNotFullyBuilt.size()+" ,"+pathsToCombine.size());
								
								
								
								
								
							}
						}

						if (!openSplits.isEmpty()) {
							//still inside a branch
							Gateway lastOpenedSplit = openSplits.getLast();
							String correspondingJoinId = lastOpenedSplit.getName() + "_join";
							FlowNode correspondingJoin = modelInstance.getModelElementById(correspondingJoinId);

							getAllPaths(modelInstance, joinGtw.getOutgoing().iterator().next().getTarget(),
									correspondingJoin, queue, openSplits, currentPath, paths, parallelBranches);
						} else {
							// when there are no open splits gtws but there are still elements in the queue
							// go from the successor of the element to bpmnEnd since the currentElement has
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
										modelInstance.getModelElementsByType(EndEvent.class).iterator().next(), queue,
										openSplits, newPath, paths, parallelBranches);
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
					FlowNode correspondingJoinGtw = null;
					String idCorrespondingJoinGtw = element.getName() + "_join";

					for (Gateway gtw : modelInstance.getModelElementsByType(Gateway.class)) {
						if (gtw.getId().contentEquals(idCorrespondingJoinGtw) && gtw.getIncoming().size() == 2) {
							correspondingJoinGtw = gtw;
							break;
						}

					}
					if (correspondingJoinGtw == null) {
						System.err.println(
								"no corresponding join for " + element.getId() + ", " + element.getOutgoing().size());

					}

					LinkedList<FlowNode> newPath = new LinkedList<FlowNode>();
					newPath.addAll(currentPath);

					getAllPaths(modelInstance, successor, correspondingJoinGtw, queue, openSplits, newPath, paths,
							parallelBranches);
				} else {

					queue.add(successor);

				}

			}

		}

		return paths;

	}

	public static <T> boolean isSubListOfEachList(LinkedList<T> list1,
			LinkedList<LinkedList<FlowNode>> listOfLists) {
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

	public static <T> boolean isSubListOfList2(LinkedList<FlowNode> list1, LinkedList<FlowNode> list2) {
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
		  return 
		    lists.stream()  // create a <Stream<List<T>>
		         .map(HashSet::new) // transform Stream<List<T>> to Stream<HashSet<T>> 
		         .distinct() // keep only distinct Sets
		         .count() < lists.size();
		}
}
