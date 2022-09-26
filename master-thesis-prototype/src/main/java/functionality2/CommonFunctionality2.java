package functionality2;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
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
import org.camunda.bpm.model.bpmn.instance.ManualTask;
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
import Mapping.BPMNExclusiveGateway;
import Mapping.BPMNParallelGateway;
import Mapping.BPMNParticipant;
import Mapping.BPMNTask;
import Mapping.Combination;
import functionality.API;



public class CommonFunctionality2 {	


	public static synchronized void isCorrectModel(BpmnModelInstance modelInstance) throws Exception {
		// does correctness checking of the process model
		// e.g. 1 Start and 1 End Event

		if (!CommonFunctionality2.checkIfOnlyOneStartEventAndEventEvent(modelInstance)) {
			throw new Exception("Model must have exactly 1 Start and 1 End Event");
		}

		CommonFunctionality2.isModelValid(modelInstance);

		if (!CommonFunctionality2.isModelBlockStructured(modelInstance)) {
			throw new Exception("Model must be block structured!");
		}

		if (!CommonFunctionality2.isNamedBranches(modelInstance)) {
			throw new Exception("Name outgoing sequenceflows of splits in order to enable labeling!");
		}

	}

	public static boolean checkIfOnlyOneStartEventAndEventEvent(BpmnModelInstance modelInstance) {
		if (modelInstance.getModelElementsByType(StartEvent.class).size() == 1
				&& modelInstance.getModelElementsByType(EndEvent.class).size() == 1) {
			return true;
		} else {
			return false;
		}

	}

	public static boolean checkCorrectnessOfBranches(BpmnModelInstance modelInstance)
			throws NullPointerException, InterruptedException, Exception {
		// for each dataObject
		// get all readers and writers
		// check paths between readers and writers and also in the other direction:
		// writers and readers
		// also check paths between writers -> when there is no path -> they are in
		// different branches
		// if there is no path -> nodes in different branches of same depth

		for (DataObjectReference dao : modelInstance.getModelElementsByType(DataObjectReference.class)) {

			LinkedList<FlowNode> readersForDataO = CommonFunctionality2.getAllReadersForDataObject(modelInstance, dao);
			LinkedList<FlowNode> writersForDataO = CommonFunctionality2.getAllWritersForDataObject(modelInstance, dao);
			if (readersForDataO.isEmpty()) {
				System.err.println("No readers in the process");
				return false;
			}

			for (FlowNode reader : readersForDataO) {
				int amountPathsWithWriter = 0;

				for (int i = 0; i < writersForDataO.size() - 1; i++) {

					FlowNode writer = writersForDataO.get(i);
					LinkedList<LinkedList<FlowNode>> pathsBetweenWriterAndReader = CommonFunctionality2
							.getAllPathsBetweenNodes(modelInstance, writer.getId(), reader.getId());
					LinkedList<LinkedList<FlowNode>> pathsBetweenReaderAndWriter = CommonFunctionality2
							.getAllPathsBetweenNodes(modelInstance, reader.getId(), writer.getId());
					// there must be at least 1 path between the writer and the reader
					if (pathsBetweenWriterAndReader.size() > 0) {
						amountPathsWithWriter++;
					}

					for (int j = 1; j < writersForDataO.size(); j++) {
						// if there is no path between the writer i and writer j and writer j and writer
						// i -> they are in different branches of the same gtw
						FlowNode writer2 = writersForDataO.get(j);
						LinkedList<LinkedList<FlowNode>> pathsBetweenWriters = CommonFunctionality2
								.getAllPathsBetweenNodes(modelInstance, writer.getId(), writer2.getId());
						if (pathsBetweenWriters.isEmpty()) {
							pathsBetweenWriters = CommonFunctionality2.getAllPathsBetweenNodes(modelInstance,
									writer2.getId(), writer.getId());
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

		for (DataObjectReference daoR : modelInstance.getModelElementsByType(DataObjectReference.class)) {
			if (daoR.getId().contentEquals(iae.getId())) {
				return daoR;
			}
		}

		return null;

	}

	public static boolean removeTaskAsWriterForDataObject(BpmnModelInstance modelInstance, Task task,
			ItemAwareElement iae) {
		boolean removed = false;
		for (DataOutputAssociation dao : task.getDataOutputAssociations()) {
			ItemAwareElement currIae = dao.getTarget();
			if (currIae.getId().equals(iae.getId())) {
				BpmnEdge edgeToBeRemoved = getEdge(modelInstance, dao.getId());
				removed = task.getDataOutputAssociations().remove(dao);
				task.removeChildElement(dao);
				edgeToBeRemoved.getParentElement().removeChildElement(edgeToBeRemoved);
				return removed;
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
					return removed;
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
		CommonFunctionality2.generateDIElementForWriter(modelInstance, dao, getShape(modelInstance, iae.getId()),
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

	public static boolean isSubPath(LinkedList<FlowNode> currentPath, LinkedList<FlowNode> subPath) {
		boolean isSubPath = false;
		if (currentPath.containsAll(subPath)) {
			isSubPath = true;
			for (int i = 0; i < subPath.size(); i++) {
				if (!subPath.get(i).equals(currentPath.get(i))) {
					isSubPath = false;
					return isSubPath;
				}
			}
			return isSubPath;
		}
		return isSubPath;
	}

	public static boolean isSubPathBPMNElements(LinkedList<BPMNElement> currentPath, LinkedList<BPMNElement> subPath) {
		boolean isSubPath = false;
		if (currentPath.containsAll(subPath)) {
			isSubPath = true;
			for (int i = 0; i < subPath.size(); i++) {
				if (!subPath.get(i).equals(currentPath.get(i))) {
					isSubPath = false;
					return isSubPath;
				}
			}
			return isSubPath;
		}
		return isSubPath;
	}

	public static <T> T getRandomItem(List<T> list) {
		Random random = new Random();
		int listSize = list.size();
		int randomIndex = random.nextInt(listSize);
		return list.get(randomIndex);
	}
	
	public static <T> T getRandomItem(Set<T> set) {
		// convert set to list
		List<T> list = new LinkedList<T>();
		list.addAll(set);
		return getRandomItem(list);		
	}

	private static LinkedList<LinkedList<FlowNode>> combineParallelBranches(BpmnModelInstance modelInstance,
			LinkedList<LinkedList<FlowNode>> parallelBranches, ParallelGateway pSplit) {
		LinkedList<SequenceFlow> flowIntoBranch = new LinkedList<SequenceFlow>();
		flowIntoBranch.addAll(pSplit.getOutgoing());
		LinkedList<LinkedList<FlowNode>> combinedPaths = new LinkedList<LinkedList<FlowNode>>();
		HashMap<SequenceFlow, LinkedList<LinkedList<FlowNode>>> map = new HashMap<SequenceFlow, LinkedList<LinkedList<FlowNode>>>();
		FlowNode pJoin = null;
		try {
			pJoin = CommonFunctionality2.getCorrespondingGtw(modelInstance, pSplit);
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

		if (!map.isEmpty()) {
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
		}

		return combinedPaths;

	}

	public static LinkedList<LinkedList<FlowNode>> getAllPathsBetweenNodes(BpmnModelInstance modelInstance,
			String startNodeId, String endNodeId) throws NullPointerException, InterruptedException, Exception {

		LinkedList<FlowNode> queue = new LinkedList<FlowNode>();
		LinkedList<FlowNode> openSplits = new LinkedList<FlowNode>();
		LinkedList<FlowNode> currentPath = new LinkedList<FlowNode>();
		LinkedList<LinkedList<FlowNode>> paths = new LinkedList<LinkedList<FlowNode>>();
		LinkedList<LinkedList<FlowNode>> parallelBranches = new LinkedList<LinkedList<FlowNode>>();
		FlowNode startNode = modelInstance.getModelElementById(startNodeId);
		FlowNode endNode = modelInstance.getModelElementById(endNodeId);

		LinkedList<LinkedList<FlowNode>> subPaths = CommonFunctionality2.getAllPathsForCamundaElements(modelInstance,
				startNode, endNode, queue, openSplits, currentPath, paths, parallelBranches, endNode);
		// filter out subpaths that do not have the endNode as target

		Iterator<LinkedList<FlowNode>> subPathsIter = subPaths.iterator();
		while (subPathsIter.hasNext()) {
			LinkedList<FlowNode> subPath = subPathsIter.next();
			if ((!subPath.getLast().equals(endNode)) && (!subPath.contains(endNode))) {
				subPathsIter.remove();
			}

			else if ((!subPath.getLast().equals(endNode)) && subPath.contains(endNode)) {
				Iterator<FlowNode> subPathNodeIter = subPath.iterator();
				boolean remove = false;
				while (subPathNodeIter.hasNext()) {
					FlowNode currNode = subPathNodeIter.next();
					if (remove) {
						subPathNodeIter.remove();

					}

					if (currNode.equals(endNode)) {
						remove = true;
					}

				}

			}

		}

		Set<LinkedList<FlowNode>> set = new HashSet<LinkedList<FlowNode>>(subPaths);
		LinkedList<LinkedList<FlowNode>> noDups = new LinkedList<LinkedList<FlowNode>>(set);

		return noDups;
	}

	public static void isModelValid(BpmnModelInstance modelInstance) throws Exception {
		// model must have a writer on each path to a reader
		// model can not have a reader/writer in a parallel branch when there is a
		// writer in the other one
		// readers in both branches are allowed if there is no writer in any branch

		LinkedList<LinkedList<FlowNode>> allProcessPaths = CommonFunctionality2.getAllPathsBetweenNodes(modelInstance,
				modelInstance.getModelElementsByType(StartEvent.class).iterator().next().getId(),
				modelInstance.getModelElementsByType(EndEvent.class).iterator().next().getId());

		for (LinkedList<FlowNode> path : allProcessPaths) {
			for (int i = 0; i < path.size(); i++) {
				FlowNode f = path.get(i);

				if (f instanceof Task) {
					Task currTask = (Task) f;
					if(f.getIncoming().size()>1) {
						throw new Exception(f.getName()+" can not have >1 incoming edges!");
					}
					if (!currTask.getDataInputAssociations().isEmpty()) {
						// task is a reader
						for (DataInputAssociation dia : currTask.getDataInputAssociations()) {
							for (ItemAwareElement iae : dia.getSources()) {
								// check if there is a writer to the dataObject on the pathsToReader
								boolean writersOnPaths = true;
								LinkedList<LinkedList<FlowNode>> pathsToReader = CommonFunctionality2
										.getAllPathsBetweenNodes(modelInstance, modelInstance
												.getModelElementsByType(StartEvent.class).iterator().next().getId(),
												currTask.getId());

								for (LinkedList<FlowNode> pathToReader : pathsToReader) {
									boolean writerOnPath = false;
									for (FlowNode nodeOnPathToReader : pathToReader) {
										if (nodeOnPathToReader instanceof Task) {
											if (CommonFunctionality2.isWriterForDataObject((Task) nodeOnPathToReader,
													iae)) {
												writerOnPath = true;
											}
										}
									}
									if (!writerOnPath) {
										writersOnPaths = false;
									}
								}

								if (writersOnPaths == false) {
									throw new Exception("Not a writer before every reader!");
								}

							}

						}

					}
					if (!currTask.getDataOutputAssociations().isEmpty()) {
						// currTask is a writer
						// check if currTask is inside a parallel Branch
						// if yes, check if there is a reader/writer in the other branch
						LinkedList<LinkedList<FlowNode>> pathsToWriter = CommonFunctionality2.getAllPathsBetweenNodes(
								modelInstance,
								modelInstance.getModelElementsByType(StartEvent.class).iterator().next().getId(),
								currTask.getId());

						for (LinkedList<FlowNode> pathToWriter : pathsToWriter) {
							HashMap<ParallelGateway, FlowNode> branchOfCurrTask = new HashMap<ParallelGateway, FlowNode>();
							LinkedList<ParallelGateway> openParallelSplits = new LinkedList<ParallelGateway>();
							for (int index = 0; index < pathToWriter.size(); index++) {
								FlowNode nodeOnPathToWriter = pathToWriter.get(index);

								if (nodeOnPathToWriter instanceof ParallelGateway
										&& nodeOnPathToWriter.getOutgoing().size() >= 2) {
									// parallel Split found on path
									openParallelSplits.add((ParallelGateway) nodeOnPathToWriter);
									branchOfCurrTask.putIfAbsent((ParallelGateway) nodeOnPathToWriter,
											pathToWriter.get(index + 1));
								}
								if (nodeOnPathToWriter instanceof ParallelGateway
										&& nodeOnPathToWriter.getIncoming().size() >= 2) {
									// parallel Join found on path
									openParallelSplits.pollLast();
								}
							}

							if (!openParallelSplits.isEmpty()) {
								// writer is inside parallel branch
								// check other branches for readers/writers to same data object
								for (ParallelGateway openPSplit : openParallelSplits) {
									FlowNode successorOfOtherBranch = null;
									for (SequenceFlow outgoingSeq : openPSplit.getOutgoing()) {
										FlowNode successor = outgoingSeq.getTarget();
										for (Entry<ParallelGateway, FlowNode> entry : branchOfCurrTask.entrySet()) {
											if (entry.getKey().equals(openPSplit)) {
												if (!successor.equals(entry.getValue())) {
													// first node in other branch found
													successorOfOtherBranch = successor;
												}
											}

										}

									}

									LinkedList<LinkedList<FlowNode>> branchOfSuccessorPaths = CommonFunctionality2
											.getAllPathsBetweenNodes(modelInstance, successorOfOtherBranch.getId(),
													CommonFunctionality2.getCorrespondingGtw(modelInstance, openPSplit)
															.getId());
									for (LinkedList<FlowNode> branch : branchOfSuccessorPaths) {
										for (int nodeIndex = 0; nodeIndex < branch.size(); nodeIndex++) {
											FlowNode nodeInsideBranch = branch.get(nodeIndex);

											if (nodeInsideBranch instanceof Task) {
												for (DataOutputAssociation dao : currTask.getDataOutputAssociations()) {
													ItemAwareElement iae = dao.getTarget();
													if (CommonFunctionality2.isReaderForDataObject(
															(Task) nodeInsideBranch, iae) == true) {
														// subPathNode is reader in other parallel branch than
														// currTask
														throw new Exception(
																"Invalid reader: " + nodeInsideBranch.getName()
																		+ "and writer " + currTask.getName()
																		+ " dependencies due to parallel branching!");
													}
													if (CommonFunctionality2.isWriterForDataObject(
															(Task) nodeInsideBranch, iae) == true) {
														// subPathNode is writer in other parallel branch than
														// currTask
														throw new Exception(
																"Invalid writer: " + nodeInsideBranch.getName()
																		+ "and writer " + currTask.getName()
																		+ " dependencies due to parallel branching!");
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

			}
		}
	}

	public static LinkedList<LinkedList<FlowNode>> getAllPathsForCamundaElements(BpmnModelInstance modelInstance,
			FlowNode startNode, FlowNode endNode, LinkedList<FlowNode> queue, LinkedList<FlowNode> openSplits,
			LinkedList<FlowNode> currentPath, LinkedList<LinkedList<FlowNode>> paths,
			LinkedList<LinkedList<FlowNode>> parallelBranches, FlowNode endPointOfSearch)
			throws NullPointerException, InterruptedException, Exception {
		// go DFS inside all branch till corresponding join is found
		queue.add(startNode);

		while (!(queue.isEmpty())) {
			FlowNode element = queue.poll();

			if (Thread.currentThread().isInterrupted()) {
				System.err.println("Interrupted! " + Thread.currentThread().getName());
				throw new InterruptedException();
			}

			currentPath.add(element);

			if ((element.getId().contentEquals(endPointOfSearch.getId()) && endPointOfSearch instanceof Task)) {

				Iterator<LinkedList<FlowNode>> subPathsIter = paths.iterator();
				while (subPathsIter.hasNext()) {
					LinkedList<FlowNode> subPath = subPathsIter.next();
					boolean isSubPath = CommonFunctionality2.isSubPath(currentPath, subPath);

					if (isSubPath) {
						subPathsIter.remove();
					}

				}
				paths.add(currentPath);

				element = queue.poll();
				if (element == null && queue.isEmpty()) {

					return paths;
				}

			}

			if (element.getId().equals(endNode.getId())) {

				Iterator<LinkedList<FlowNode>> subPathIter = paths.iterator();
				while (subPathIter.hasNext()) {
					LinkedList<FlowNode> subPath = subPathIter.next();

					boolean isSubPath = CommonFunctionality2.isSubPath(currentPath, subPath);

					if (isSubPath) {
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
					Gateway lastOpenedSplitGtw = (Gateway) openSplits.pollLast();

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
							FlowNode correspondingSplit = CommonFunctionality2.getCorrespondingGtw(modelInstance,
									joinGtw);

							LinkedList<LinkedList<FlowNode>> pathsToCombine = new LinkedList<LinkedList<FlowNode>>();
							LinkedList<LinkedList<FlowNode>> pathsNotFullyBuilt = new LinkedList<LinkedList<FlowNode>>();

							for (LinkedList<FlowNode> path : paths) {
								if (path.contains(correspondingSplit) && path.getLast().equals(joinGtw)) {
									pathsToCombine.add(path);
								} else if (path.contains(correspondingSplit) && !path.getLast().equals(joinGtw)) {
									pathsNotFullyBuilt.add(path);
								}

							}

							if (pathsToCombine.size() >= 2) {
								// all paths to joinGtw are fully built
								LinkedList<LinkedList<FlowNode>> combinedPaths = CommonFunctionality2
										.combineParallelBranches(modelInstance, pathsToCombine,
												(ParallelGateway) lastOpenedSplitGtw);

								Iterator<LinkedList<FlowNode>> pathsIter = paths.iterator();

								while (pathsIter.hasNext()) {
									LinkedList<FlowNode> currPath = pathsIter.next();
									boolean isSubList = false;
									for (LinkedList<FlowNode> combPath : combinedPaths) {
										if (CommonFunctionality2.isSubListOfList2(currPath, combPath)) {
											isSubList = true;
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
									for (LinkedList<FlowNode> combPath : combinedPaths) {
										if (CommonFunctionality2.isSubListOfList2(pBranch, combPath)) {
											isSubList = true;
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
							Gateway lastOpenedSplit = (Gateway) openSplits.getLast();

							FlowNode correspondingJoin = CommonFunctionality2.getCorrespondingGtw(modelInstance,
									lastOpenedSplit);

							// go to next join node with all paths
							LinkedList<LinkedList<FlowNode>> newPaths = new LinkedList<LinkedList<FlowNode>>();

							Iterator<LinkedList<FlowNode>> pathsIter = paths.iterator();
							while (pathsIter.hasNext()) {
								LinkedList<FlowNode> path = pathsIter.next();
								if (path.getLast().equals(element)) {
									LinkedList<FlowNode> newPathAfterJoin = new LinkedList<FlowNode>();
									newPathAfterJoin.addAll(path);
									newPaths.add(newPathAfterJoin);
									// pathsIter.remove();
								}
							}

							// need to add the lastOpenedSplit, since one path has gone dfs till the join
							// already

							for (int i = 0; i < newPaths.size() - 1; i++) {
								openSplits.add(lastOpenedSplit);
							}

							Iterator<LinkedList<FlowNode>> newPathIter = newPaths.iterator();
							while (newPathIter.hasNext()) {
								LinkedList<FlowNode> newPath = newPathIter.next();
								if (!newPathIter.hasNext()) {

								}
								getAllPathsForCamundaElements(modelInstance,
										element.getOutgoing().iterator().next().getTarget(), correspondingJoin, queue,
										openSplits, newPath, paths, parallelBranches, endPointOfSearch);
							}

						} else {
							// when there are no open splits gtws
							// go from the successor of the element to endPointOfSearch since the
							// currentElement has
							// already been added to the path
							LinkedList<LinkedList<FlowNode>> newPaths = new LinkedList<LinkedList<FlowNode>>();

							Iterator<LinkedList<FlowNode>> pathsIter = paths.iterator();
							while (pathsIter.hasNext()) {
								LinkedList<FlowNode> path = pathsIter.next();
								if (path.getLast().equals(element)) {
									LinkedList<FlowNode> newPathAfterJoin = new LinkedList<FlowNode>();
									newPathAfterJoin.addAll(path);
									newPaths.add(newPathAfterJoin);
									// pathsIter.remove();
								}
							}

							for (LinkedList<FlowNode> newPath : newPaths) {
								getAllPathsForCamundaElements(modelInstance,
										element.getOutgoing().iterator().next().getTarget(), endPointOfSearch, queue,
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

			Iterator<SequenceFlow> seqFlowIter = element.getOutgoing().iterator();
			while (seqFlowIter.hasNext()) {
				FlowNode successor = seqFlowIter.next().getTarget();
				if (element instanceof Gateway && element.getOutgoing().size() == 2) {
					// when a split is found - go dfs till the corresponding join is found
					FlowNode correspondingJoinGtw = null;
					try {
						correspondingJoinGtw = CommonFunctionality2.getCorrespondingGtw(modelInstance,
								(Gateway) element);
					} catch (Exception ex) {
						throw ex;
					}

					LinkedList<FlowNode> newPath = new LinkedList<FlowNode>();
					newPath.addAll(currentPath);
					getAllPathsForCamundaElements(modelInstance, successor, correspondingJoinGtw, queue, openSplits,
							newPath, paths, parallelBranches, endPointOfSearch);
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

	public static Gateway getCorrespondingGtw(BpmnModelInstance modelInstance, Gateway gtw)
			throws NullPointerException, Exception {

		if (gtw.getName().isEmpty() || gtw.getName().equals(null)) {
			throw new Exception("Corresponding gtws must be named accordingly!");
		}
		Gateway correspondingGtw = null;
		if (gtw.getIncoming().size() >= 2 && gtw.getOutgoing().size() == 1) {
			// gtw is a join
			StringBuilder splitGtwId = new StringBuilder();

			if (gtw.getId().contains("_join")) {
				splitGtwId.append(gtw.getName().trim() + "_split");
			} else {
				// it must have the same name but >=2 outgoing and ==1 incoming
				for (Gateway gateway : modelInstance.getModelElementsByType(Gateway.class)) {
					if (gateway.getName().trim().contentEquals(gtw.getName().trim())) {
						if (gateway.getIncoming().size() == 1 && gateway.getOutgoing().size() >= 2) {
							splitGtwId.append(gateway.getId());
							break;
						}
					}
				}

			}
			Gateway splitGtw = modelInstance.getModelElementById(splitGtwId.toString().trim());
			if (splitGtw == null) {
				throw new Exception("Names of corresponding split and join gtws have to be equal!");
			}
			if (splitGtw.getIncoming().size() == 1 && splitGtw.getOutgoing().size() == 2) {
				correspondingGtw = splitGtw;
			}
		} else if (gtw.getIncoming().size() == 1 && gtw.getOutgoing().size() == 2) {
			// gtw is a split
			StringBuilder joinGtwId = new StringBuilder();
			if (gtw.getId().contains("_split")) {
				joinGtwId.append(gtw.getName().trim() + "_join");
			} else {
				// it must have the same name but ==1 outgoing and >=2 incoming
				for (Gateway gateway : modelInstance.getModelElementsByType(Gateway.class)) {
					if (gateway.getName().trim().contentEquals(gtw.getName().trim())) {
						if (gateway.getIncoming().size() == 2 && gateway.getOutgoing().size() == 1) {
							joinGtwId.append(gateway.getId());
							break;
						}
					}
				}

			}
			Gateway joinGtw = modelInstance.getModelElementById(joinGtwId.toString().trim());
			if (joinGtw == null) {
				throw new Exception("Names of corresponding split and join gtws have to be equal!");
			}
			
			if (joinGtw.getIncoming().size() == 2 && joinGtw.getOutgoing().size() == 1) {
				correspondingGtw = joinGtw;
			}
		}
		return correspondingGtw;

	}

	public static boolean isModelBlockStructured(BpmnModelInstance modelInstance)
			throws NullPointerException, Exception {
		if (modelInstance.getModelElementsByType(Gateway.class).size() % 2 != 0) {
			System.out.println("!!!");
			return false;
		}
		for (Gateway gtw : modelInstance.getModelElementsByType(Gateway.class)) {
			try {
				if (CommonFunctionality2.getCorrespondingGtw(modelInstance, gtw) == null) {
					return false;
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				return false;
			}
		}
		return true;
	}

	public static boolean testGatewaysForElements(BpmnModelInstance modelInstance)
			throws NullPointerException, Exception {
		boolean branchesCorrect = true;
		for (Gateway gtw : modelInstance.getModelElementsByType(Gateway.class)) {
			if (gtw instanceof ParallelGateway) {
				if (gtw.getOutgoing().size() == 2 && gtw.getIncoming().size() == 1) {
					Gateway parallelJoin = CommonFunctionality2.getCorrespondingGtw(modelInstance, gtw);
					int parallelJoinSuccessor = gtw.getOutgoing().size();
					for (SequenceFlow s : gtw.getOutgoing()) {
						if (!s.getTarget().equals(parallelJoin)) {
							parallelJoinSuccessor--;
						}
					}
					if (parallelJoinSuccessor > 0) {
						// one of the branches has the join as direct successor
						return false;
					}
				}

			} else if (gtw instanceof ExclusiveGateway) {
				if (gtw.getOutgoing().size() == 2 && gtw.getIncoming().size() == 1) {
					Gateway xorJoin = CommonFunctionality2.getCorrespondingGtw(modelInstance, gtw);
					boolean xorJoinSuccessor = true;
					for (SequenceFlow s : gtw.getOutgoing()) {
						if (!s.getTarget().equals(xorJoin)) {
							xorJoinSuccessor = false;
						}
					}
					if (xorJoinSuccessor) {
						// both branches have the xorJoin as direct successor
						return false;
					}
				}
			}

		}
		return branchesCorrect;
	}

	public static int getAmountExclusiveGtwSplits(BpmnModelInstance modelInstance) {
		int amount = 0;
		for (ExclusiveGateway gtw : modelInstance.getModelElementsByType(ExclusiveGateway.class)) {
			if (gtw.getIncoming().size() == 1 && gtw.getOutgoing().size() >= 2) {
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
		for (ParallelGateway gtw : modelInstance.getModelElementsByType(ParallelGateway.class)) {
			if (gtw.getIncoming().size() == 1 && gtw.getOutgoing().size() >= 2) {
				amount++;
			}
		}

		return amount;

	}

	public static HashMap<DataObjectReference, LinkedList<FlowNode>> getReadersForDataObjects(
			BpmnModelInstance modelInstance) {
		HashMap<DataObjectReference, LinkedList<FlowNode>> readersMap = new HashMap<DataObjectReference, LinkedList<FlowNode>>();
		for (DataObjectReference daoR : modelInstance.getModelElementsByType(DataObjectReference.class)) {
			readersMap.putIfAbsent(daoR, new LinkedList<FlowNode>());
		}

		for (Task task : modelInstance.getModelElementsByType(Task.class)) {
			for (DataInputAssociation dia : task.getDataInputAssociations()) {
				for (ItemAwareElement iae : dia.getSources()) {
					for (DataObjectReference daoRef : readersMap.keySet()) {
						if (iae.getId().contentEquals(daoRef.getId())) {
							readersMap.get(daoRef).add(task);
						}
					}
				}
			}
		}

		return readersMap;
	}

	public static HashMap<DataObjectReference, LinkedList<FlowNode>> getWritersForDataObjects(
			BpmnModelInstance modelInstance) {
		HashMap<DataObjectReference, LinkedList<FlowNode>> writersMap = new HashMap<DataObjectReference, LinkedList<FlowNode>>();
		for (DataObjectReference daoR : modelInstance.getModelElementsByType(DataObjectReference.class)) {
			writersMap.putIfAbsent(daoR, new LinkedList<FlowNode>());
		}

		for (Task task : modelInstance.getModelElementsByType(Task.class)) {
			for (DataOutputAssociation dao : task.getDataOutputAssociations()) {
				ItemAwareElement iae = dao.getTarget();
				for (DataObjectReference daoRef : writersMap.keySet()) {
					if (iae.getId().contentEquals(daoRef.getId())) {
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
		if (!newFile.exists()) {
			newFile.mkdirs();
		}
		return newFile;
	}

	public static String getNextStricterSphere(String sphere) {
		sphere.trim();
		if (sphere.contentEquals("Public")) {
			return "Global";
		} else if (sphere.contentEquals("Global")) {
			return "Static";
		} else if (sphere.contentEquals("Static")) {
			return "Weak-Dynamic";

		} else if (sphere.contentEquals("Weak-Dynamic")) {
			return "Strong-Dynamic";
		} else

			return "";

	}

	public static int getGlobalSphere(BpmnModelInstance modelInstance, boolean modelWithLanes) {

		LinkedList<String> participants = new LinkedList<String>();

		if (!modelWithLanes) {
			// if the model is without lanes -> the participants must be extracted from the
			// task name
			for (Task task : modelInstance.getModelElementsByType(Task.class)) {

				String participantName = task.getName().substring(task.getName().indexOf("[") + 1,
						task.getName().indexOf("]"));
				if (!participants.contains(participantName)) {
					participants.add(participantName);
				}

			}
		} else {
			// model is with lanes
			for (Lane l : modelInstance.getModelElementsByType(Lane.class)) {
				for (FlowNode nodeOnLane : l.getFlowNodeRefs()) {
					if (nodeOnLane instanceof Task && !participants.contains(l.getName())) {
						participants.add(l.getName());
					}
				}
			}
		}
		return participants.size();
	}

	public static LinkedList<String> getGlobalSphereList(BpmnModelInstance modelInstance, boolean modelWithLanes) {

		LinkedList<String> participants = new LinkedList<String>();

		if (!modelWithLanes) {
			// if the model is without lanes -> the participants must be extracted from the
			// task name
			for (Task task : modelInstance.getModelElementsByType(Task.class)) {

				String participantName = task.getName().substring(task.getName().indexOf("[") + 1,
						task.getName().indexOf("]"));
				if (!participants.contains(participantName)) {
					participants.add(participantName);
				}

			}
		} else {
			// model is with lanes
			// only add the lane to global sphere if there is a task on it
			for (Lane l : modelInstance.getModelElementsByType(Lane.class)) {
				for (FlowNode nodeOnLane : l.getFlowNodeRefs()) {
					if (nodeOnLane instanceof Task && !participants.contains(l.getName())) {
						participants.add(l.getName());
					}
				}
			}

		}
		return participants;

	}

	public static void generateNewModelAndIncreaseVotersForEachDataObject(String pathToFile, int increaseBy) {
		File file = new File(pathToFile);
		BpmnModelInstance modelInstance = Bpmn.readModelFromFile(file);

		boolean modelWithLanes = true;
		if (modelInstance.getModelElementsByType(Lane.class).isEmpty()) {
			modelWithLanes = false;
		}

		int maxParticipants = CommonFunctionality2.getGlobalSphere(modelInstance, modelWithLanes);

		for (ExclusiveGateway gtw : modelInstance.getModelElementsByType(ExclusiveGateway.class)) {
			if (gtw.getIncoming().size() == 1 && gtw.getOutgoing().size() >= 2) {
				for (TextAnnotation tx : modelInstance.getModelElementsByType(TextAnnotation.class)) {
					for (Association assoc : modelInstance.getModelElementsByType(Association.class)) {
						if (assoc.getSource().equals(gtw) && assoc.getTarget().equals(tx)) {
							if (tx.getTextContent().startsWith("[Voters]")) {
								String string = tx.getTextContent();
								String substr = string.substring(string.indexOf('{') + 1, string.indexOf(','));

								int currAmountVoters = Integer.parseInt(substr);
								if (currAmountVoters < maxParticipants) {
									int amountAfterIncreasing = currAmountVoters + increaseBy;
									string.replaceFirst(substr, amountAfterIncreasing + "");
									tx.setTextContent(string);
								}

							}
						}

					}
				}

			}

		}

	}

	public static void increaseSpherePerDataObject(BpmnModelInstance modelInstance, String sphereToSet)
			throws IOException, ParserConfigurationException, SAXException {
		// sets the sphere of each data object in the model to the sphereToSet

		for (TextAnnotation tx : modelInstance.getModelElementsByType(TextAnnotation.class)) {

			if (tx.getTextContent().startsWith("Default")) {

				StringBuilder sb = new StringBuilder();
				sb.append(tx.getTextContent().substring(0, tx.getTextContent().indexOf('{') + 1));

				sb.append(sphereToSet + '}');
				Text txt = modelInstance.newInstance(Text.class);
				txt.setTextContent(sb.toString());
				tx.setText(txt);

			}
		}

	}

	public static void increaseSpherePerDataObject(File file, String directoryToStore, String suffixName,
			String sphereToSet) throws IOException, ParserConfigurationException, SAXException {
		// sets the sphere of each data object in the model to the sphereToSet

		BpmnModelInstance modelInstance = Bpmn.readModelFromFile(file);
		String fileName = file.getName().substring(0, file.getName().indexOf(".bpmn"));
		String iterationName = "";

		for (TextAnnotation tx : modelInstance.getModelElementsByType(TextAnnotation.class)) {

			if (tx.getTextContent().startsWith("Default")) {

				String subString = tx.getTextContent().substring(tx.getTextContent().indexOf('{') + 1,
						tx.getTextContent().indexOf('}'));

				StringBuilder sb = new StringBuilder();
				sb.append(tx.getTextContent().substring(0, tx.getTextContent().indexOf('{')));

				sb.append(sphereToSet + '}');
				Text txt = modelInstance.newInstance(Text.class);
				txt.setTextContent(sb.toString());
				tx.setText(txt);

			}
		}
		String modelWithStricterSpheres = fileName + "_" + sphereToSet;
		CommonFunctionality2.writeChangesToFile(modelInstance, modelWithStricterSpheres, directoryToStore,
				iterationName);

	}

	public static void increaseVotersPerDataObject(BpmnModelInstance modelInstance, int currValue)
			throws IOException, ParserConfigurationException, SAXException {

		boolean modelWithLanes = true;
		if (modelInstance.getModelElementsByType(Lane.class).isEmpty()) {
			modelWithLanes = false;
		}

		int maxParticipants = CommonFunctionality2.getGlobalSphere(modelInstance, modelWithLanes);

		for (ExclusiveGateway gtw : modelInstance.getModelElementsByType(ExclusiveGateway.class)) {
			if (gtw.getIncoming().size() == 1 && gtw.getOutgoing().size() >= 2) {
				for (TextAnnotation tx : modelInstance.getModelElementsByType(TextAnnotation.class)) {
					for (Association assoc : modelInstance.getModelElementsByType(Association.class)) {
						if (assoc.getSource().equals(gtw) && assoc.getTarget().equals(tx)) {
							if (tx.getTextContent().startsWith("[Voters]")) {

								String subString = tx.getTextContent().substring(tx.getTextContent().indexOf('{') + 1,
										tx.getTextContent().indexOf('}'));
								String[] values = subString.split(",");

								int currAmountVoters = Integer.parseInt(values[0]);
								if (currAmountVoters <= maxParticipants && currValue <= maxParticipants) {
									int amountAfterIncreasing = currValue;

									int lowerBound = (int) Math.ceil((double) amountAfterIncreasing / 2) + 1;
									int randomCountVotersSameDecision = 0;
									if (lowerBound < currValue) {
										randomCountVotersSameDecision = ThreadLocalRandom.current().nextInt(lowerBound,
												amountAfterIncreasing + 1);
									} else {
										randomCountVotersSameDecision = currValue;
									}
									StringBuilder sb = new StringBuilder();
									sb.append("[Voters]{" + amountAfterIncreasing + "," + randomCountVotersSameDecision
											+ "," + values[2] + "}");

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

	public static void increaseVotersPerDataObject(File file, String directoryToStore, String suffixName, int currValue)
			throws IOException, ParserConfigurationException, SAXException {
		BpmnModelInstance modelInstance = Bpmn.readModelFromFile(file);
		String fileName = file.getName().substring(0, file.getName().indexOf(".bpmn"));
		String iterationName = "_amountVoters";

		boolean modelWithLanes = true;
		if (modelInstance.getModelElementsByType(Lane.class).isEmpty()) {
			modelWithLanes = false;
		}

		int maxParticipants = CommonFunctionality2.getGlobalSphere(modelInstance, modelWithLanes);

		for (ExclusiveGateway gtw : modelInstance.getModelElementsByType(ExclusiveGateway.class)) {
			if (gtw.getIncoming().size() == 1 && gtw.getOutgoing().size() >= 2) {
				for (TextAnnotation tx : modelInstance.getModelElementsByType(TextAnnotation.class)) {
					for (Association assoc : modelInstance.getModelElementsByType(Association.class)) {
						if (assoc.getSource().equals(gtw) && assoc.getTarget().equals(tx)) {
							if (tx.getTextContent().startsWith("[Voters]")) {

								String subString = tx.getTextContent().substring(tx.getTextContent().indexOf('{') + 1,
										tx.getTextContent().indexOf('}'));
								String[] values = subString.split(",");

								int currAmountVoters = Integer.parseInt(values[0]);
								if (currAmountVoters <= maxParticipants && currValue <= maxParticipants) {
									int amountAfterIncreasing = currValue;

									int lowerBound = (int) Math.ceil((double) amountAfterIncreasing / 2) + 1;
									int randomCountVotersSameDecision = 0;
									if (lowerBound < currValue) {
										randomCountVotersSameDecision = ThreadLocalRandom.current().nextInt(lowerBound,
												amountAfterIncreasing + 1);
									} else {
										randomCountVotersSameDecision = currValue;
									}
									StringBuilder sb = new StringBuilder();
									sb.append("[Voters]{" + amountAfterIncreasing + "," + randomCountVotersSameDecision
											+ "," + values[2] + "}");

									Text text = modelInstance.newInstance(Text.class);
									text.setTextContent(sb.toString());
									tx.setText(text);
									iterationName += "-" + amountAfterIncreasing;
								}

							}
						}

					}
				}

			}

		}
		if (!iterationName.isEmpty()) {
			String modelWithNewAmountVoters = fileName;
			CommonFunctionality2.writeChangesToFile(modelInstance, modelWithNewAmountVoters, directoryToStore,
					suffixName);
		}
	}

	public static int getAmountFromPercentageWithMinimum(int amountTasks, int percentage, int min) {
		double amountFromPercentage = (double) amountTasks * percentage / 100;
		int amountTasksToBeWriter = (int) Math.ceil(amountFromPercentage);
		if (amountTasksToBeWriter < min) {
			return min;
		}
		return amountTasksToBeWriter;

	}

	public static int getAmountFromPercentage(int amountTasks, int percentage) {
		double amountFromPercentage = (double) amountTasks * percentage / 100;
		return (int) Math.ceil(amountFromPercentage);

	}

	public static File writeChangesToFile(BpmnModelInstance modelInstance, String fileName,
			String directoryToStoreAnnotatedModel, String suffixName)
			throws IOException, ParserConfigurationException, SAXException {
		// validate and write model to file
		Bpmn.validateModel(modelInstance);

		int fileNumber = 0;
		if (fileName.contains(".bpmn")) {
			fileName = fileName.substring(0, fileName.indexOf(".bpmn"));
		}
		if (!suffixName.startsWith("_")) {
			suffixName = "_" + suffixName;
		}

		File dir = new File(directoryToStoreAnnotatedModel);
		File[] directoryListing = dir.listFiles();
		boolean increaseFileNumber = false;
		if (directoryListing != null) {
			for (File child : directoryListing) {
				if (child.getName().equals(fileName) && child.getName().contains(".bpmn")) {
					String nameOfFileInsideDirectory = child.getName().substring(0, child.getName().indexOf(".bpmn"));
					if (nameOfFileInsideDirectory.contains("_annotated")) {
						Pattern p = Pattern.compile("[0-9]+");
						Matcher m = p.matcher(nameOfFileInsideDirectory);
						while (m.find()) {
							int num = Integer.parseInt(m.group());
							if (num > fileNumber) {
								fileNumber = num;
								increaseFileNumber = true;
							}
						}
					}
				}
			}
		}
		String annotatedFileName = fileName + suffixName + ".bpmn";
		if (increaseFileNumber) {
			fileNumber++;
			annotatedFileName = fileName + fileNumber + suffixName + ".bpmn";

		}
		File file = CommonFunctionality2.createFileWithinDirectory(directoryToStoreAnnotatedModel, annotatedFileName);

		System.out.println("File path: " + file.getAbsolutePath());
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
		return file;
	}

	public static List<LinkedList<Integer>> computeRepartitionNumberWithResultBound(int maxAmount, int subParts,
			int threshold_number, int amountResults) throws Exception, InterruptedException {

		if (Thread.currentThread().isInterrupted()) {
			System.err.println("Interrupted! " + Thread.currentThread().getName());
			throw new InterruptedException();
		}

		List<LinkedList<Integer>> resultRec = new LinkedList<LinkedList<Integer>>();

		if (resultRec.size() == amountResults) {
			return resultRec;
		}

		if (subParts == 1) {
			List<LinkedList<Integer>> resultEnd = new LinkedList<LinkedList<Integer>>();
			LinkedList<Integer> unitary = new LinkedList<>();
			resultEnd.add(unitary);
			unitary.add(maxAmount);
			return resultEnd;
		}

		for (int i = threshold_number; i <= maxAmount - threshold_number; i++) {
			int remain = maxAmount - i;
			List<LinkedList<Integer>> partialRec = computeRepartitionNumberWithResultBound(remain, subParts - 1,
					threshold_number, amountResults);
			for (List<Integer> subList : partialRec) {
				subList.add(i);
			}
			resultRec.addAll(partialRec);
			if (resultRec.size() == amountResults) {
				return resultRec;
			}
		}
		return resultRec;

	}

	public static List<LinkedList<Integer>> computeRepartitionNumber(int maxAmount, int subParts, int threshold_number)
			throws Exception, InterruptedException {

		if (Thread.currentThread().isInterrupted()) {
			System.err.println("Interrupted! " + Thread.currentThread().getName());
			throw new InterruptedException();
		}

		List<LinkedList<Integer>> resultRec = new LinkedList<LinkedList<Integer>>();
		if (subParts == 1) {
			List<LinkedList<Integer>> resultEnd = new LinkedList<LinkedList<Integer>>();
			LinkedList<Integer> unitary = new LinkedList<>();
			resultEnd.add(unitary);
			unitary.add(maxAmount);
			return resultEnd;
		}

		for (int i = threshold_number; i <= maxAmount - threshold_number; i++) {
			int remain = maxAmount - i;
			List<LinkedList<Integer>> partialRec = computeRepartitionNumber(remain, subParts - 1, threshold_number);
			for (List<Integer> subList : partialRec) {
				subList.add(i);
			}
			resultRec.addAll(partialRec);
		}
		return resultRec;

	}

	public static int getSumAmountVotersOfModel(BpmnModelInstance modelInstance) {

		int sumAmountVoters = 0;
		for (ExclusiveGateway gtw : modelInstance.getModelElementsByType(ExclusiveGateway.class)) {
			if (gtw.getIncoming().size() == 1 && gtw.getOutgoing().size() >= 2) {
				for (TextAnnotation tx : modelInstance.getModelElementsByType(TextAnnotation.class)) {
					for (Association assoc : modelInstance.getModelElementsByType(Association.class)) {
						if (assoc.getSource().equals(gtw) && assoc.getTarget().equals(tx)) {
							if (tx.getTextContent().startsWith("[Voters]")) {

								String subString = tx.getTextContent().substring(tx.getTextContent().indexOf('{') + 1,
										tx.getTextContent().indexOf('}'));
								String[] values = subString.split(",");
								if (!values[0].contentEquals("Public")) {
									int currAmountVoters = Integer.parseInt(values[0]);
									sumAmountVoters += currAmountVoters;
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
		for (ExclusiveGateway gtw : modelInstance.getModelElementsByType(ExclusiveGateway.class)) {
			if (gtw.getIncoming().size() == 1 && gtw.getOutgoing().size() >= 2) {
				for (TextAnnotation tx : modelInstance.getModelElementsByType(TextAnnotation.class)) {
					for (Association assoc : modelInstance.getModelElementsByType(Association.class)) {
						if (assoc.getSource().equals(gtw) && assoc.getTarget().equals(tx)) {
							if (tx.getTextContent().startsWith("[Voters]")) {

								String subString = tx.getTextContent().substring(tx.getTextContent().indexOf('{') + 1,
										tx.getTextContent().indexOf('}'));
								String[] values = subString.split(",");

								double currAmountVoters = Double.parseDouble(values[0]);
								sumAmountVoters += currAmountVoters;
								amountGtws++;
							}
						}
					}
				}

			}
		}
		if (amountGtws == 0) {
			return 0;
		}
		return ((double) sumAmountVoters / amountGtws);
	}

	public static double getSphereSumOfModel(BpmnModelInstance modelInstance) {
		// a higher value will be a model with more strict decisions
		double sumSpheres = 0;
		double amountBrts = 0;
		HashMap<DataObjectReference, LinkedList<FlowNode>> readers = CommonFunctionality2
				.getReadersForDataObjects(modelInstance);
		for (DataObjectReference dataO : readers.keySet()) {
			for (FlowNode f : readers.get(dataO)) {
				if (f instanceof BusinessRuleTask) {
					BusinessRuleTask brt = (BusinessRuleTask) f;
					for (TextAnnotation tx : modelInstance.getModelElementsByType(TextAnnotation.class)) {
						for (DataInputAssociation dia : brt.getDataInputAssociations()) {
							for (ItemAwareElement iae : dia.getSources()) {
								if (iae.getId().contentEquals(dataO.getId())) {
									if (tx.getTextContent().startsWith("Default")) {

										amountBrts++;
										String subString = tx.getTextContent().substring(
												tx.getTextContent().indexOf('{') + 1, tx.getTextContent().indexOf('}'));

										if (subString.contentEquals("Strong-Dynamic")) {
											sumSpheres += 3;
										} else if (subString.contentEquals("Weak-Dynamic")) {
											sumSpheres += 2;
										} else if (subString.contentEquals("Static")) {
											sumSpheres += 1;
										} else if (subString.contentEquals("Global")) {
											sumSpheres += 0;

										}
									}
								}
							}
						}

					}
				}
			}

		}
		if (amountBrts == 0) {
			amountBrts = 1;
		}
		return sumSpheres / amountBrts;
	}

	

	public static int getAmountTasks(BpmnModelInstance modelInstance) {
		int amountTasks = 0;
		for (Task t : modelInstance.getModelElementsByType(Task.class)) {
			amountTasks++;
		}
		return amountTasks;
	}

	public static List<Integer> maxVoterCombsPerGatewayFromProcessDimension(double processDimensionSize,
			int amountDecisions, int globalSphere, int minVotersPerDecision, boolean equallyDistributed)
			throws Exception {
		// calculate the amount of maximum voter combinations per gateway to fit the
		// processDimensionSize
		// e.g. processDimensonSize 10000 with 4 gtws -> if equally distributed each
		// decision has 10 voterCombinations which is e.g. binomial coefficient of 5
		// over 3
		// decisions should have 2 voters at least

		if (globalSphere < 2) {
			throw new Exception("Model shoud have >= 2 participants!");
		}
		if (minVotersPerDecision < 2) {
			throw new Exception("Model should have >= 2 voters per decision");
		}
		if (minVotersPerDecision > globalSphere) {
			throw new Exception("Minimum voters per decision can not be > than global sphere!");

		}

		LinkedList<Integer> votersPerGtw = new LinkedList<Integer>();
		if (equallyDistributed) {
			// each decision has the same size of voter combinations
			double equallyDistributedVoterCombs = Math.floor(processDimensionSize / amountDecisions);
			// n is the global sphere
			// k is the amount of voters for the gtw
			// find the k such that the binomial coefficient n over k is <= the
			// equallyDistributedVoterCombs
			int prevK = 0;
			for (int k = minVotersPerDecision; k <= globalSphere; k++) {
				double combs = CommonFunctionality2.binom(globalSphere, k);
				if (combs <= equallyDistributedVoterCombs) {
					prevK = k;
				} else {
					if (prevK == 0) {
						throw new Exception("Voter will be 0!");
					}

					for (int size = 0; size <= globalSphere; size++) {
						votersPerGtw.add(prevK);
					}
					break;
				}

			}

		} else {
			// each gtw can have a different size of voters for the decision
			double minVoterCombsPerDecision = Math.floor(processDimensionSize / minVotersPerDecision);
			List<LinkedList<Integer>> voterCombsPerGtwLists = CommonFunctionality2
					.computeRepartitionNumberWithResultBound((int) processDimensionSize, amountDecisions,
							(int) minVoterCombsPerDecision, 5);
			LinkedList<Integer> voterCombsPerGtwList = CommonFunctionality2.getRandomItem(voterCombsPerGtwLists);

			for (int i = 0; i < voterCombsPerGtwList.size(); i++) {
				int currVotersComb = voterCombsPerGtwList.get(i);
				int prevK = 0;
				for (int k = minVotersPerDecision; k <= globalSphere; k++) {
					double combs = CommonFunctionality2.binom(globalSphere, k);
					if (combs <= currVotersComb) {
						prevK = k;
					} else {
						if (prevK == 0) {
							throw new Exception("Voter will be 0!");
						}
						votersPerGtw.add(prevK);
						break;
					}

				}
			}

		}

		if (votersPerGtw.size() != amountDecisions) {
			throw new Exception("Failed: VotersPerGtw!=amountDecisions!");
		}

		return votersPerGtw;

	}

	public static int binom(int n, int k) {
		if (n == k || k == 0) {
			return 1;
		}

		return binom(n - 1, k) + binom(n - 1, k - 1);

	}

	public static boolean containsIgnoreCase(String src, String what) {
		final int length = what.length();
		if (length == 0)
			return true; // Empty string is contained

		final char firstLo = Character.toLowerCase(what.charAt(0));
		final char firstUp = Character.toUpperCase(what.charAt(0));

		for (int i = src.length() - length; i >= 0; i--) {
			// Quick check before calling the more expensive regionMatches() method:
			final char ch = src.charAt(i);
			if (ch != firstLo && ch != firstUp)
				continue;

			if (src.regionMatches(true, i, what, 0, length))
				return true;
		}

		return false;
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

	public static void generateNewModelsWithAnnotatedChosenParticipants(API api,
			LinkedList<PModelWithAdditionalActors> pInstances, int upperBoundNewModels,
			String directoryToStoreAnnotatedModels) {
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
		if (directoryToStoreAnnotatedModels == null || directoryToStoreAnnotatedModels.contentEquals("")) {
			// directory will be the same directory as the process file from the api
			directoryToStoreAnnotatedModels = api.getProcessFile().getParent();
		}

		if (upperBoundNewModels <= 0) {
			// all instances will be written to files
			upperBoundNewModels = pInstances.size();
		}
		if (upperBoundNewModels > pInstances.size()) {
			upperBoundNewModels = pInstances.size();
		}

		int i = 1;

		for (int bound = 0; bound < upperBoundNewModels; bound++) {
			PModelWithAdditionalActors pInst = pInstances.get(bound);
			// make a deep copy of the modelInstance to prevent changing the model itself
			BpmnModelInstance modelInstance = (BpmnModelInstance) CommonFunctionality2.deepCopy(api.getModelInstance());
			

			for (AdditionalActors addActors : pInst.getAdditionalActorsList()) {
				BPMNBusinessRuleTask bpmnBrt = addActors.getCurrBrt();

				ObjectMapper mapper = new ObjectMapper();
				// Convert object to JSON string
				StringBuilder sbDecision = new StringBuilder();
				String jsonInString;
				try {
					jsonInString = mapper.writeValueAsString(bpmnBrt.getDecisionEvaluation());
					sbDecision.append(jsonInString);
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				Documentation doc = modelInstance.newInstance(Documentation.class);
				doc.setTextContent(sbDecision.toString());
				BusinessRuleTask brt = (BusinessRuleTask) CommonFunctionality2.getFlowNodeByBPMNNodeId(modelInstance,
						bpmnBrt.getId());
				brt.getDocumentations().add(doc);

				BPMNExclusiveGateway xorSplit = (BPMNExclusiveGateway) bpmnBrt.getSuccessors().iterator().next();
				FlowNode gtw = CommonFunctionality2.getFlowNodeByBPMNNodeId(modelInstance, xorSplit.getId());
				TextAnnotation sphere = null;

				if (xorSplit.getAmountVerifiers() == api.getGlobalSphereList().size()) {
					// annotate "private" to the xor-split
					sphere = modelInstance.newInstance(TextAnnotation.class);
					String textContent = "[Voters] {Private}";

					sphere.setTextFormat("text/plain");
					Text text = modelInstance.newInstance(Text.class);
					text.setTextContent(textContent);
					sphere.setText(text);
					gtw.getParentElement().addChildElement(sphere);

					// add the shape of the text annotation to the xml file
					CommonFunctionality2.generateShapeForTextAnnotation(modelInstance, sphere, gtw);

				} else if (xorSplit.getAmountVerifiers() > api.getGlobalSphereList().size()) {
					// annotate "public" to the xor-split
					sphere = modelInstance.newInstance(TextAnnotation.class);
					String textContent = "[Voters] {Public}";

					sphere.setTextFormat("text/plain");
					Text text = modelInstance.newInstance(Text.class);
					text.setTextContent(textContent);
					sphere.setText(text);
					gtw.getParentElement().addChildElement(sphere);

					// add the shape of the text annotation to the xml file
					CommonFunctionality2.generateShapeForTextAnnotation(modelInstance, sphere, gtw);

				} else {
					// annotate the chosen Participants to the xor-split that will be the voters
					sphere = modelInstance.newInstance(TextAnnotation.class);
					sphere.setTextFormat("text/plain");

					Text text = modelInstance.newInstance(Text.class);

					StringBuilder sb = new StringBuilder();
					sb.append("[Voters]{");
					for (BPMNParticipant participant : addActors.getAdditionalActors()) {
						sb.append(participant.getName() + ", ");
					}
					sb.deleteCharAt(sb.length() - 1);
					sb.deleteCharAt(sb.length() - 1);
					sb.append("}");
					text.setTextContent(sb.toString());

					sphere.setText(text);
					gtw.getParentElement().addChildElement(sphere);

					// add the shape of the text annotation to the xml file
					CommonFunctionality2.generateShapeForTextAnnotation(modelInstance, sphere, gtw);

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
				CommonFunctionality2.writeChangesToFile(modelInstance, fileName, directoryToStoreAnnotatedModels,
						suffix);

			} catch (IOException | ParserConfigurationException | SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			i++;
		}

	}

	public static void generateShapeForTextAnnotation(BpmnModelInstance modelInstance, TextAnnotation txt,
			FlowNode ref) {
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

	public static void generateNewModelsWithVotersAsBpmnConstruct(API api,
			LinkedList<PModelWithAdditionalActors> pInstances, int upperBoundNewModels, String directoryToStoreModels,
			boolean votingAsSubProcess, boolean mapModel) throws Exception, IOException {

		try {
			String fileName = api.getProcessFile().getName();

			if (directoryToStoreModels == null || directoryToStoreModels.contentEquals("")) {
				// directory will be the same directory as the process file from the api
				directoryToStoreModels = api.getProcessFile().getParent();
			}

			if (upperBoundNewModels <= 0) {
				// all instances will be written to files
				upperBoundNewModels = pInstances.size();
			}
			if (upperBoundNewModels > pInstances.size()) {
				upperBoundNewModels = pInstances.size();
			}

			int i = 1;

			BPMNParticipant troubleShooter = api.getTroubleShooter();

			int j = 1;
			for (int bound = 0; bound < upperBoundNewModels; bound++) {
				// make a deep copy of the modelInstance to prevent changing the model itself
				BpmnModelInstance modelInstance = (BpmnModelInstance) CommonFunctionality2
						.deepCopy(api.getModelInstance());
				Bpmn.validateModel(modelInstance);

				PModelWithAdditionalActors pInstance = pInstances.get(bound);
				Iterator<AdditionalActors> addActorsIter = pInstance.getAdditionalActorsList().iterator();

				while (addActorsIter.hasNext()) {
					AdditionalActors addActors = addActorsIter.next();
					BPMNBusinessRuleTask bpmnBusinessRt = addActors.getCurrBrt();
					BusinessRuleTask businessRt = (BusinessRuleTask) CommonFunctionality2
							.getFlowNodeByBPMNNodeId(modelInstance, bpmnBusinessRt.getId());
					LinkedList<BPMNParticipant> additionalActors = addActors.getAdditionalActors();
					BPMNExclusiveGateway bpmnEx = (BPMNExclusiveGateway) bpmnBusinessRt.getSuccessors().iterator()
							.next();

					// builder doesn't work on task (need to be subclasses of task!)
					// convert the task to a user task to prevent error
					BPMNElement predecessorOfBpmnBrt = bpmnBusinessRt.getPredecessors().iterator().next();
					if (predecessorOfBpmnBrt.getClass() == Mapping.BPMNTask.class) {
						Task predec = (Task) CommonFunctionality2.getFlowNodeByBPMNNodeId(modelInstance,
								predecessorOfBpmnBrt.getId());
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
					if (additionalActors.size() == 1) {
						onlyOneTask = true;
					}

					// Voting system inside of a subprocess
					if (votingAsSubProcess) {

						if (!(onlyOneTask)) {
							BPMNParallelGateway.increaseVotingTaskCount();
							CommonFunctionality2.addTasksToVotingSystem(api, modelInstance, i, businessRt, bpmnEx,
									CommonFunctionality2
											.getFlowNodeByBPMNNodeId(modelInstance,
													bpmnBusinessRt.getPredecessors().iterator().next().getId())
											.builder().subProcess().embeddedSubProcess().startEvent(),
									"PV" + BPMNParallelGateway.getVotingTaskCount(),
									addActors.getAdditionalActors(), troubleShooter,
									"PV" + BPMNParallelGateway.getVotingTaskCount(),
									BPMNExclusiveGateway.increaseExclusiveGtwCount(), votingAsSubProcess, onlyOneTask);
						} else {
							CommonFunctionality2.addTasksToVotingSystem(api, modelInstance, i, businessRt, bpmnEx,
									businessRt.builder()
											.moveToNode(bpmnBusinessRt.getPredecessors().iterator().next().getId()),
									"PV" + BPMNParallelGateway.getVotingTaskCount(),
									addActors.getAdditionalActors(), troubleShooter,
									"PV" + BPMNParallelGateway.getVotingTaskCount(), 0, votingAsSubProcess,
									onlyOneTask);
						}
					}

					else {
						// Voting without having a subprocess
						if (!onlyOneTask) {
							BPMNParallelGateway.increaseVotingTaskCount();
							BPMNExclusiveGateway.increaseExclusiveGtwCount();
						}
						CommonFunctionality2.addTasksToVotingSystem(api, modelInstance, i, businessRt, bpmnEx,
								businessRt.builder()
										.moveToNode(bpmnBusinessRt.getPredecessors().iterator().next().getId()),
								"PV" + BPMNParallelGateway.getVotingTaskCount(),
								addActors.getAdditionalActors(), troubleShooter,
								"PV" + BPMNParallelGateway.getVotingTaskCount(),
								BPMNExclusiveGateway.getExclusiveGtwCount(), votingAsSubProcess, onlyOneTask);

					}

					// Add the new tasks generated via fluent builder API to the corresponding lanes
					// in the xml model
					// Cant be done with the fluent model builder directly!

					for (Lane l : modelInstance.getModelElementsByType(Lane.class)) {
						for (Task task : modelInstance.getModelElementsByType(UserTask.class)) {
							if (task.getName().startsWith("VotingTask") && l.getName().equals(task.getName()
									.substring(task.getName().indexOf(" ") + 1, task.getName().length()))) {
								// Add necessary information to the voting tasks

								if (mapModel && task.getDocumentations().isEmpty()) {
									Documentation doc = modelInstance.newInstance(Documentation.class);
									StringBuilder sb = new StringBuilder();

									// add the decision of the businessruletask to the element documentation of the
									// voting tasks
									// use the Jackson converter to convert java object into json format

									ObjectMapper mapper = new ObjectMapper();
									// Convert object to JSON string
									String jsonInString = mapper
											.writeValueAsString(bpmnBusinessRt.getDecisionEvaluation());
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
								FlowNode n = CommonFunctionality2.getFlowNodeByBPMNNodeId(modelInstance, task.getId());
								if (!l.getFlowNodeRefs().contains(n)) {
									l.addChildElement(ref);
								}

							}
						}

					}
					i++;

				}

				Iterator<AdditionalActors> addActorsIter2 = pInstance.getAdditionalActorsList().iterator();
				while (addActorsIter2.hasNext()) {
					AdditionalActors addActors = addActorsIter2.next();
					BPMNBusinessRuleTask brt = addActors.getCurrBrt();
					BusinessRuleTask b = (BusinessRuleTask) CommonFunctionality2.getFlowNodeByBPMNNodeId(modelInstance,
							brt.getId());
					if (b != null) {
						CommonFunctionality2.removeBusinessRuleTask(b, modelInstance);
					}
				}
				try {
					if (mapModel) {
						// convert pools and lanes
						CommonFunctionality2.mapModel(modelInstance);
					}
					String suffix = "votingAsConstruct-solution" + j;
					CommonFunctionality2.writeChangesToFile(modelInstance, fileName, directoryToStoreModels, suffix);
				} catch (ParserConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				j++;
			}
		} catch (NullPointerException n) {
			throw new Exception("API was null!");
		} catch (Exception e) {
			throw e;
		}

	}

	private static void addTasksToVotingSystem(API api, BpmnModelInstance modelInstance, int i, BusinessRuleTask brt,
			BPMNExclusiveGateway bpmnEx, AbstractFlowNodeBuilder builder, String parallelSplit,
			LinkedList<BPMNParticipant> voters, BPMNParticipant troubleShooter, String parallelJoin,
			int exclusiveGtwCount, boolean votingAsSubProcess, boolean onlyOneTask) {
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

		allBPMNDataObjects
				.addAll(((BPMNBusinessRuleTask) CommonFunctionality2.getNodeById(api.getProcessElements(), brt.getId()))
						.getDataObjects());
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

			ServiceTask st = (ServiceTask) CommonFunctionality2.getFlowNodeByBPMNNodeId(modelInstance, serviceTaskId);
			st.getOutgoing().iterator().next().getId();
			CommonFunctionality2.addInformationToServiceTasks(modelInstance, st,
					(BPMNExclusiveGateway) CommonFunctionality2.getNodeById(api.getProcessElements(), targetGtw.getId()),
					false);

			for (BPMNDataObject dao : allBPMNDataObjects) {
				CommonFunctionality2.addDataInputReferencesToVotingTasks(modelInstance, (Task) CommonFunctionality2
						.getFlowNodeByBPMNNodeId(modelInstance, "Task_votingTask" + votingTaskId), dao);
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
							CommonFunctionality2.addDataInputReferencesToVotingTasks(modelInstance, t, dao);
						}
						skip = true;
					}
				}
				if (skip == false) {
					int votingTaskId = BPMNTask.increaseVotingTaskId();

					builder.moveToNode(parallelSplitId).userTask("Task_votingTask" + votingTaskId)
							.name("VotingTask " + nextParticipant.getName());
					alreadyModelled.add((Task) CommonFunctionality2.getFlowNodeByBPMNNodeId(modelInstance,
							"Task_votingTask" + votingTaskId));
					for (BPMNDataObject dao : allBPMNDataObjects) {
						CommonFunctionality2.addDataInputReferencesToVotingTasks(modelInstance,
								(Task) CommonFunctionality2.getFlowNodeByBPMNNodeId(modelInstance,
										"Task_votingTask" + votingTaskId),
								dao);
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
							CommonFunctionality2.changeWayPoints(modelInstance, outgoing);
						}
					}

					CommonFunctionality2.addInformationToServiceTasks(modelInstance,
							(ServiceTask) CommonFunctionality2.getFlowNodeByBPMNNodeId(modelInstance, serviceTaskId),
							(BPMNExclusiveGateway) CommonFunctionality2.getNodeById(api.getProcessElements(),
									targetGtw.getId()),
							true);

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
					CommonFunctionality2.addInformationToServiceTasks(modelInstance,
							(ServiceTask) CommonFunctionality2.getFlowNodeByBPMNNodeId(modelInstance,
									serviceTaskDeciderId),
							(BPMNExclusiveGateway) CommonFunctionality2.getNodeById(api.getProcessElements(),
									targetGtw.getId()),
							false);

					for (BPMNDataObject dao : allBPMNDataObjects) {
						CommonFunctionality2.addDataInputReferencesToVotingTasks(modelInstance,
								(Task) CommonFunctionality2.getFlowNodeByBPMNNodeId(modelInstance,
										"Task_votingTask" + BPMNTask.getVotingTaskId()),
								dao);
					}

				} else {
					if (votersIter.hasNext()) {
						builder.moveToNode(parallelSplitId);
					}
				}

				skip = false;

			}

			if (!votingAsSubProcess) {
				builder.moveToNode(exclusiveGatewayDeciderJoinId).connectTo(targetGtw.getId());
				builder.moveToNode(exclusiveGatewayDeciderSplitId).connectTo(exclusiveGatewayDeciderJoinId);

			}

			else if (votingAsSubProcess) {

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
					CommonFunctionality2.changeWayPoints(modelInstance, outgoingSeq);
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

	public static void addInformationToServiceTasks(BpmnModelInstance modelInstance, ServiceTask st,
			BPMNExclusiveGateway xor, boolean votingTask) {
		st.setCamundaType("external");
		st.setCamundaTopic("voting");
		Documentation dataObjectDocu = modelInstance.newInstance(Documentation.class);
		StringBuilder sb = new StringBuilder();
		if (votingTask) {
			sb.append("{\"gateway\":\"" + st.getOutgoing().iterator().next().getTarget().getName() + "\"");
			sb.append(",\"sameDecision\":" + "\"" + xor.getAmountVerifiersSameChoice() + "\"" + ",\"loops\":" + "\""
					+ xor.getAmountLoops() + "\"");
		} else {
			sb.append("{\"gateway\":\"" + xor.getName() + "\"");
		}
		sb.append("}");
		dataObjectDocu.setTextContent(sb.toString());
		st.getDocumentations().add(dataObjectDocu);
	}

	private static void addDataInputReferencesToVotingTasks(BpmnModelInstance modelInstance, Task task,
			BPMNDataObject dataObject) {
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

	public static String compareResultsOfAlgorithmsForDifferentAPIs(
			LinkedList<PModelWithAdditionalActors> modelsFoundWithHeuristicSearch,
			LinkedList<PModelWithAdditionalActors> modelsFoundWithExhaustiveSearch, int boundForComparisons) {

		try {
			if (modelsFoundWithHeuristicSearch == null || modelsFoundWithExhaustiveSearch == null) {
				return "null";
			}
			if (modelsFoundWithHeuristicSearch.isEmpty() || modelsFoundWithExhaustiveSearch.isEmpty()) {
				return "null";
			}

			int countCheapestSolutionFoundWithExhaustive = 0;
			LinkedList<PModelWithAdditionalActors> cheapestExhaustiveSolutions = CommonFunctionality2
					.getCheapestPModelsWithAdditionalActors(modelsFoundWithExhaustiveSearch);
			boolean compareAll = false;
			if (boundForComparisons <= 0) {
				// compare models one by one 
				compareAll = true;
			}

			if (compareAll == false) {
				// only check if the cheapest solutions have the same cost
				if (cheapestExhaustiveSolutions.get(0).getSumMeasure() == modelsFoundWithHeuristicSearch.get(0)
						.getSumMeasure()) {
					return "true";

				} else {
					return "false";
				}

			} else {
				// check if the solutions found with exhaustive search contain all solutions found with heuristic approach
				for (PModelWithAdditionalActors cheapestModelExhaustive : cheapestExhaustiveSolutions) {
					for (PModelWithAdditionalActors cheapestModelHeuristic : modelsFoundWithHeuristicSearch) {
						if (cheapestModelExhaustive
								.getSumMeasure() == (cheapestModelHeuristic.getSumMeasure())) {
							boolean count = true;
							for (AdditionalActors addActors : cheapestModelExhaustive
									.getAdditionalActorsList()) {
							
								for (AdditionalActors addActors2 : cheapestModelHeuristic.getAdditionalActorsList()) {
									if (addActors.getCurrBrt().getId().equals(addActors2.getCurrBrt().getId())) {
										
										int size = addActors.getAdditionalActors().size();
										int entryEqual = 0;
										
										for (BPMNParticipant participant1 : addActors.getAdditionalActors()) {
											for (BPMNParticipant participant2 : addActors2.getAdditionalActors()) {
												if (participant1.getId().contentEquals(participant2.getId())) {
													entryEqual++;
												}

											}
										}			
										if(entryEqual != size) {
											System.out.println(entryEqual + " , "+size);
											return "false";
										}
									}

								}
								
								

							}
							if (count) {
								countCheapestSolutionFoundWithExhaustive++;
								if (countCheapestSolutionFoundWithExhaustive == modelsFoundWithHeuristicSearch.size()) {
									return "true";
								}
							}

						}

					}

				}
			}
			if (countCheapestSolutionFoundWithExhaustive == modelsFoundWithHeuristicSearch.size()) {
				return "true";
			} else {
				return "false";
			}
		} catch (NullPointerException ex) {
			return "null";
		}

	}


	
	public static void generateNewModelsUntilGlobalSphereReached(File model, int globalSphereLowerBound,
			int amountNewProcessesToCreatePerIteration, String directoryToStore) {

		String fileNameWithoutExtension = model.getName().replace(".bpmn", "").trim();
		BpmnModelInstance modelInstance = Bpmn.readModelFromFile(model);
		int globalSphereUpperBound = modelInstance.getModelElementsByType(Task.class).size();

		while (globalSphereLowerBound <= globalSphereUpperBound) {
			// globalSphereLowerBound = amount of different participants for this model
			// globalSphereLowerBound e.g. 3 -> create
			// amountNewProcessesToCreatePerIteration new Models where all tasks of the
			// model have one of the 3 participants connected

			for (int iteration = 0; iteration < amountNewProcessesToCreatePerIteration; iteration++) {
				String suffix = "lb" + globalSphereLowerBound + "ub" + globalSphereUpperBound + "iter" + iteration;
				LinkedList<String> participantNames = new LinkedList<String>();
				for (int i = 1; i <= globalSphereLowerBound; i++) {
					String participantName = "Participant_" + i;
					participantNames.add(participantName);
				}

				BpmnModelInstance cloneModel = (BpmnModelInstance) CommonFunctionality2.deepCopy(modelInstance);
				LinkedList<Task> tasksToChooseFrom = new LinkedList<Task>();
				tasksToChooseFrom.addAll(cloneModel.getModelElementsByType(Task.class));
				Collections.shuffle(tasksToChooseFrom);

				Iterator<Task> taskIter = tasksToChooseFrom.iterator();
				LinkedList<String> participantsNeededToBeChosen = new LinkedList<String>();
				participantsNeededToBeChosen.addAll(participantNames);

				while (taskIter.hasNext()) {
					Task currTask = taskIter.next();
					String currTaskName = currTask.getName();

					Iterator<String> participantIter = participantsNeededToBeChosen.iterator();
					if (participantIter.hasNext()) {
						// first assign the participant once to a task to match the
						// globalSphereLowerBound
						String newParticipant = participantIter.next();
						String newTaskName = currTaskName.replaceAll("(?<=\\[).*?(?=\\])", newParticipant);
						currTask.setName(newTaskName);

						participantIter.remove();

					} else {
						// choose a random participant
						String newParticipant = CommonFunctionality2.getRandomItem(participantNames);
						String newTaskName = currTaskName.replaceAll("(?<=\\[).*?(?=\\])", newParticipant);
						currTask.setName(newTaskName);

					}

				}

				try {
					CommonFunctionality2.writeChangesToFile(cloneModel, fileNameWithoutExtension, directoryToStore,
							suffix);
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

	public static int getAmountDataObjects(BpmnModelInstance modelInstance) {
		return modelInstance.getModelElementsByType(DataObjectReference.class).size();
	}

	public static void removeDataObjectsWithoutConnectionsToDecisionsFromModel(BpmnModelInstance modelInstance) {
		LinkedList<DataObjectReference> daoRList = new LinkedList<DataObjectReference>();
		daoRList.addAll(modelInstance.getModelElementsByType(DataObjectReference.class));
		LinkedList<BusinessRuleTask> brtList = new LinkedList<BusinessRuleTask>();
		brtList.addAll(modelInstance.getModelElementsByType(BusinessRuleTask.class));
		LinkedList<DataObjectReference> daoRsFound = new LinkedList<DataObjectReference>();

		for (BusinessRuleTask brt : brtList) {
			Iterator<DataInputAssociation> diaIter = brt.getDataInputAssociations().iterator();
			while (diaIter.hasNext()) {
				DataInputAssociation dia = diaIter.next();
				for (ItemAwareElement iae : dia.getSources()) {
					DataObjectReference daoR = CommonFunctionality2
							.getDataObjectReferenceForItemAwareElement(modelInstance, iae);
					if (!daoRsFound.contains(daoR)) {
						daoRsFound.add(daoR);
					}

				}
			}

		}

		// remove all daoRs that have not been found
		Iterator<DataObjectReference> daoRIter = daoRList.iterator();
		while (daoRIter.hasNext()) {
			DataObjectReference dao = daoRIter.next();
			if (!daoRsFound.contains(dao)) {
				String daoName = dao.getName();
				String name = daoName.substring(daoName.indexOf('['), daoName.indexOf(']') + 1);
				BpmnShape shape = CommonFunctionality2.getShape(modelInstance, dao.getId());
				if (shape != null) {
					shape.getParentElement().removeChildElement(shape);
				}
				Iterator<TextAnnotation> txIter = modelInstance.getModelElementsByType(TextAnnotation.class).iterator();
				while (txIter.hasNext()) {
					TextAnnotation tx = txIter.next();
					if (tx.getTextContent().startsWith("Default: ")) {
						if (tx.getTextContent().contains(name)) {
							BpmnShape shape2 = CommonFunctionality2.getShape(modelInstance, tx.getId());
							if (shape2 != null) {
								shape2.getParentElement().removeChildElement(shape2);
							}
							tx.getParentElement().removeChildElement(tx);

						}
					}
				}
				dao.getParentElement().removeChildElement(dao);
			}
		}

	}

	public static void substituteOneDataObjectPerIterationAndWriteNewModels(BpmnModelInstance modelInstance,
			DataObjectReference substitute, String fileName, String directoryToStore) {
		// choose a dataObject from a decision and substitute it
		// if decision already has the substitute -> remove the random dataObject
		// generate new model on each iteration

		Collection<BusinessRuleTask> brtList = modelInstance.getModelElementsByType(BusinessRuleTask.class);
		Iterator<BusinessRuleTask> brtIter = brtList.iterator();

		int iteration = 0;

		while (brtIter.hasNext()) {
			BusinessRuleTask brt = brtIter.next();
			boolean substituteAnnotatedAlready = false;
			LinkedList<DataObjectReference> toRemove = new LinkedList<DataObjectReference>();
			for (DataInputAssociation dia : brt.getDataInputAssociations()) {
				for (ItemAwareElement iae : dia.getSources()) {
					DataObjectReference daoR = CommonFunctionality2
							.getDataObjectReferenceForItemAwareElement(modelInstance, iae);
					if (daoR.getId().equals(substitute.getId())) {
						substituteAnnotatedAlready = true;
					} else {
						if (!toRemove.contains(daoR)) {
							toRemove.add(daoR);
						}
					}

				}
			}

			while (!toRemove.isEmpty()) {
				// choose random dataObject to remove
				DataObjectReference daoRToBeRemoved = CommonFunctionality2.getRandomItem(toRemove);
				Iterator<DataInputAssociation> diaIter = brt.getDataInputAssociations().iterator();
				while (diaIter.hasNext()) {
					DataInputAssociation dia = diaIter.next();
					for (ItemAwareElement iae : dia.getSources()) {
						DataObjectReference daoR = CommonFunctionality2
								.getDataObjectReferenceForItemAwareElement(modelInstance, iae);
						if (daoR.getId().equals(daoRToBeRemoved.getId())) {
							brt.getDataInputAssociations().remove(dia);
							for (BpmnEdge bpmnE : modelInstance.getModelElementsByType(BpmnEdge.class)) {
								if (bpmnE.getBpmnElement() == null) {
									bpmnE.getParentElement().removeChildElement(bpmnE);
								}
							}

							// iterate through all tasks that have the dataObject connected
							// remove the dataObjectReference
							LinkedList<Task> taskWithInputAssocRemoved = new LinkedList<Task>();
							LinkedList<Task> taskWithOutputAssocRemoved = new LinkedList<Task>();
							for (Task task : modelInstance.getModelElementsByType(Task.class)) {
								for (DataInputAssociation taskDia : task.getDataInputAssociations()) {
									for (ItemAwareElement taskIae : taskDia.getSources()) {
										if (taskIae.getId().contentEquals(daoRToBeRemoved.getId())) {
											if (CommonFunctionality2.removeTaskAsReaderFromDataObject(modelInstance,
													task, taskIae)) {
												taskWithInputAssocRemoved.add(task);
											}

										}
									}
								}
								for (DataOutputAssociation taskDao : task.getDataOutputAssociations()) {
									ItemAwareElement taskIae = taskDao.getTarget();
									if (taskIae.getId().contentEquals(daoRToBeRemoved.getId())) {
										if (CommonFunctionality2.removeTaskAsWriterForDataObject(modelInstance, task,
												taskIae)) {
											taskWithOutputAssocRemoved.add(task);
										}
									}
								}
							}

							if (substituteAnnotatedAlready == false) {
								// connect the substitute
								DataInputAssociation diaToConnect = modelInstance
										.newInstance(DataInputAssociation.class);
								Property p1 = modelInstance.newInstance(Property.class);
								p1.setName("__targetRef_placeholder");
								brt.addChildElement(p1);
								diaToConnect.setTarget(p1);
								brt.getDataInputAssociations().add(diaToConnect);
								diaToConnect.getSources().add(substitute);
								CommonFunctionality2.generateDIElementForReader(modelInstance, diaToConnect,
										CommonFunctionality2.getShape(modelInstance, substitute.getId()),
										CommonFunctionality2.getShape(modelInstance, brt.getId()));

								for (Task t : taskWithInputAssocRemoved) {
									DataInputAssociation toConnect = modelInstance
											.newInstance(DataInputAssociation.class);
									Property p2 = modelInstance.newInstance(Property.class);
									p2.setName("__targetRef_placeholder");
									t.addChildElement(p2);
									toConnect.setTarget(p2);
									t.getDataInputAssociations().add(toConnect);
									toConnect.getSources().add(substitute);
									CommonFunctionality2.generateDIElementForReader(modelInstance, toConnect,
											CommonFunctionality2.getShape(modelInstance, substitute.getId()),
											CommonFunctionality2.getShape(modelInstance, t.getId()));

								}
								substituteAnnotatedAlready = true;
							}

							BpmnShape shape = CommonFunctionality2.getShape(modelInstance, daoRToBeRemoved.getId());
							shape.getParentElement().removeChildElement(shape);
							// daoRToBeRemoved.getParentElement().removeChildElement(daoRToBeRemoved);
							iteration++;
							try {
								CommonFunctionality2
										.removeDataObjectsWithoutConnectionsToDecisionsFromModel(modelInstance);
							} catch (Exception e) {
							}

							try {
								CommonFunctionality2.writeChangesToFile(modelInstance, fileName, directoryToStore,
										"substituteIter" + iteration);
								toRemove.remove(daoRToBeRemoved);
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

	private static void generateDIElementForReader(BpmnModelInstance modelInstance, DataInputAssociation dia,
			BpmnShape daoR, BpmnShape readerTaskShape) {
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

	public static void addExcludeParticipantConstraintsOnModel(BpmnModelInstance modelInstance, String modelName,
			int probabilityForGatewayToHaveConstraint, int lowerBoundAmountParticipantsToExcludePerGtw,
			boolean decisionTakerExcludeable, boolean alwaysMaxConstrained, boolean modelWithLanes,
			String directoryToStore) throws Exception {
		// upperBoundAmountParticipantsToExclude is the difference between the amount of
		// needed voters and the global Sphere
		// e.g. global sphere = 5, 3 people needed -> 2 is the max amount of
		// participants to exclude

		if (lowerBoundAmountParticipantsToExcludePerGtw < 0) {
			throw new Exception("LowerBoundAmountParticipantsToExclude must be >= 0!");
		}

		boolean constraintInserted = false;
		boolean maxConstrainedModel = true;

		for (ExclusiveGateway gtw : modelInstance.getModelElementsByType(ExclusiveGateway.class)) {
			if (gtw.getOutgoing().size() == 2
					&& gtw.getIncoming().iterator().next().getSource() instanceof BusinessRuleTask) {
				int randomInt = ThreadLocalRandom.current().nextInt(1, 101);
				if (probabilityForGatewayToHaveConstraint >= randomInt) {
					BusinessRuleTask brtBeforeGtw = (BusinessRuleTask) gtw.getIncoming().iterator().next().getSource();
					String decisionTakerName = brtBeforeGtw.getName();
					LinkedList<String> participantsToChooseFrom = CommonFunctionality2.getGlobalSphereList(modelInstance,
							modelWithLanes);
					int globalSphereSize = participantsToChooseFrom.size();

					// remove the decision taker from the list if necessary
					if (!decisionTakerExcludeable) {
						for (String part : participantsToChooseFrom) {
							if (decisionTakerName.contains(part)) {
								participantsToChooseFrom.remove(part);
								break;
							}

						}
					}

					if (!participantsToChooseFrom.isEmpty()) {

						for (TextAnnotation tx : modelInstance.getModelElementsByType(TextAnnotation.class)) {
							for (Association assoc : modelInstance.getModelElementsByType(Association.class)) {
								if (assoc.getSource().equals(gtw) && assoc.getTarget().equals(tx)) {
									if (tx.getTextContent().startsWith("[Voters]")) {
										String subStr = tx.getTextContent().substring(
												tx.getTextContent().indexOf('{') + 1, tx.getTextContent().indexOf('}'));

										String[] data = subStr.split(",");
										int amountVotersNeeded = Integer.parseInt(data[0]);
										int randomAmountConstraintsForGtw = 0;

										if (alwaysMaxConstrained) {
											randomAmountConstraintsForGtw = globalSphereSize - amountVotersNeeded;
										} else {
											int maxConstraint = globalSphereSize - amountVotersNeeded;

											if (maxConstraint <= 0) {
												// no constraints possible -> else model will not be valid
												randomAmountConstraintsForGtw = 0;
											} else {
												if (lowerBoundAmountParticipantsToExcludePerGtw < maxConstraint) {
													randomAmountConstraintsForGtw = ThreadLocalRandom.current().nextInt(
															lowerBoundAmountParticipantsToExcludePerGtw,
															maxConstraint + 1);
												} else {
													// lowerBoundAmountParticipantsToExcludePerGtw is bigger than
													// maxConstraint
													// generate a new random between 0 and maxConstraint
													randomAmountConstraintsForGtw = ThreadLocalRandom.current()
															.nextInt(0, maxConstraint + 1);
												}
											}

										}

										Collections.shuffle(participantsToChooseFrom);
										Iterator<String> partIter = participantsToChooseFrom.iterator();
										if (randomAmountConstraintsForGtw < (globalSphereSize - amountVotersNeeded)) {
											maxConstrainedModel = false;
										}

										for (int i = 0; i < randomAmountConstraintsForGtw; i++) {
											if (partIter.hasNext()) {
												String currPart = partIter.next();

												String excludeParticipantConstraint = "[ExcludeParticipantConstraint] {"
														+ currPart + "}";
												TextAnnotation constraintAnnotation = modelInstance
														.newInstance(TextAnnotation.class);

												constraintAnnotation.setTextFormat("text/plain");
												Text text = modelInstance.newInstance(Text.class);
												text.setTextContent(excludeParticipantConstraint);
												constraintAnnotation.setText(text);
												gtw.getParentElement().addChildElement(constraintAnnotation);

												CommonFunctionality2.generateShapeForTextAnnotation(modelInstance,
														constraintAnnotation, brtBeforeGtw);

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
												modelInstance.getModelElementsByType(Plane.class).iterator().next()
														.addChildElement(edge);
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
		String suffix = "";
		if (constraintInserted) {
			if (maxConstrainedModel) {
				suffix = "alwMaxExcl_constrained";
			} else {
				suffix = "excl_constrained";
			}
		} else {
			suffix = "noExclConstraints";
		}
		CommonFunctionality2.writeChangesToFile(modelInstance, modelName, directoryToStore, suffix);

	}

	public static void addMandatoryParticipantConstraintsOnModel(BpmnModelInstance modelInstance, String modelName,
			int probabilityForGatewayToHaveConstraint, int lowerBoundAmountParticipantsToBeMandatoryPerGtw,
			int upperBoundAmountParticipantsToBeMandatoryPerGtw, boolean decisionTakerMandatory,
			boolean alwaysMaxConstrained, boolean modelWithLanes, String directoryToStore) throws Exception {
		// if upperBoundAmountParticipantsToBeMandatoryPerGtw is < 0:
		// upperBoundAmountParticipantsToBeMandatoryPerGtw is the needed amount of
		// voters for the gateway
		// e.g. 3 people needed for voting -> 3 constraints for mandatory participants
		// for the
		// gtw
		// if decisionTakerMandatory = true -> the participant of the brt will be
		// mandatory
		// else the mandatory participants will be chosen randomly from global sphere

		boolean constraintInserted = false;
		boolean maxMandConstrained = true;

		if (lowerBoundAmountParticipantsToBeMandatoryPerGtw < 0) {
			throw new Exception("lowerBoundAmountParticipantsToBeMandatoryPerGtw must be >= 0");
		}

		for (ExclusiveGateway gtw : modelInstance.getModelElementsByType(ExclusiveGateway.class)) {
			if (gtw.getOutgoing().size() == 2
					&& gtw.getIncoming().iterator().next().getSource() instanceof BusinessRuleTask) {
				int randomInt = ThreadLocalRandom.current().nextInt(1, 101);
				if (probabilityForGatewayToHaveConstraint >= randomInt) {
					BusinessRuleTask brtBeforeGtw = (BusinessRuleTask) gtw.getIncoming().iterator().next().getSource();
					String decisionTakerName = brtBeforeGtw.getName();
					String decisionTakerParticipant = decisionTakerName.substring(decisionTakerName.indexOf('[') + 1,
							decisionTakerName.indexOf(']'));
					LinkedList<String> participantsToChooseFrom = CommonFunctionality2.getGlobalSphereList(modelInstance,
							modelWithLanes);

					if (!participantsToChooseFrom.isEmpty()) {

						for (TextAnnotation tx : modelInstance.getModelElementsByType(TextAnnotation.class)) {
							for (Association assoc : modelInstance.getModelElementsByType(Association.class)) {
								if (assoc.getSource().equals(gtw) && assoc.getTarget().equals(tx)) {
									if (tx.getTextContent().startsWith("[Voters]")) {
										String subStr = tx.getTextContent().substring(
												tx.getTextContent().indexOf('{') + 1, tx.getTextContent().indexOf('}'));

										String[] data = subStr.split(",");
										int amountVotersNeeded = Integer.parseInt(data[0]);
										int randomAmountConstraintsForGtw = 0;

										if (alwaysMaxConstrained) {
											randomAmountConstraintsForGtw = amountVotersNeeded;
										} else {
											if (upperBoundAmountParticipantsToBeMandatoryPerGtw < 0) {
												upperBoundAmountParticipantsToBeMandatoryPerGtw = amountVotersNeeded;
											}

											if (lowerBoundAmountParticipantsToBeMandatoryPerGtw < upperBoundAmountParticipantsToBeMandatoryPerGtw) {
												randomAmountConstraintsForGtw = ThreadLocalRandom.current().nextInt(
														lowerBoundAmountParticipantsToBeMandatoryPerGtw,
														upperBoundAmountParticipantsToBeMandatoryPerGtw + 1);
											} else if (lowerBoundAmountParticipantsToBeMandatoryPerGtw == upperBoundAmountParticipantsToBeMandatoryPerGtw) {
												randomAmountConstraintsForGtw = upperBoundAmountParticipantsToBeMandatoryPerGtw;
											}
										}

										if (randomAmountConstraintsForGtw != amountVotersNeeded) {
											// if one decision in the model is not constrained to maximum
											maxMandConstrained = false;
										}

										if (decisionTakerMandatory && randomAmountConstraintsForGtw > 0) {
											// first mandatory participant will be the decision taker
											String mandatoryParticipantConstraint = "[MandatoryParticipantConstraint] {"
													+ decisionTakerParticipant + "}";
											TextAnnotation constraintAnnotation = modelInstance
													.newInstance(TextAnnotation.class);

											constraintAnnotation.setTextFormat("text/plain");
											Text text = modelInstance.newInstance(Text.class);
											text.setTextContent(mandatoryParticipantConstraint);
											constraintAnnotation.setText(text);
											gtw.getParentElement().addChildElement(constraintAnnotation);

											CommonFunctionality2.generateShapeForTextAnnotation(modelInstance,
													constraintAnnotation, brtBeforeGtw);

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
											modelInstance.getModelElementsByType(Plane.class).iterator().next()
													.addChildElement(edge);
											constraintInserted = true;
											participantsToChooseFrom.remove(decisionTakerParticipant);
											randomAmountConstraintsForGtw--;
										}

										Collections.shuffle(participantsToChooseFrom);
										Iterator<String> partIter = participantsToChooseFrom.iterator();

										for (int i = 0; i < randomAmountConstraintsForGtw; i++) {
											if (partIter.hasNext()) {
												String currPart = partIter.next();

												String mandatoryParticipantConstraint = "[MandatoryParticipantConstraint] {"
														+ currPart + "}";
												TextAnnotation constraintAnnotation = modelInstance
														.newInstance(TextAnnotation.class);

												constraintAnnotation.setTextFormat("text/plain");
												Text text = modelInstance.newInstance(Text.class);
												text.setTextContent(mandatoryParticipantConstraint);
												constraintAnnotation.setText(text);
												gtw.getParentElement().addChildElement(constraintAnnotation);

												CommonFunctionality2.generateShapeForTextAnnotation(modelInstance,
														constraintAnnotation, brtBeforeGtw);

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
												modelInstance.getModelElementsByType(Plane.class).iterator().next()
														.addChildElement(edge);
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
		String suffix = "";
		if (constraintInserted) {
			if (maxMandConstrained) {
				suffix = "max-mand_constrained";
			} else {
				suffix = "mand_constrained";
			}
		} else {
			suffix = "noMandConstraints";
		}
		CommonFunctionality2.writeChangesToFile(modelInstance, modelName, directoryToStore, suffix);

	}

	public static String getNecessaryUpdate(String currentSphere, String requiredSphere) {
		String necessaryUpdate = "";
		if (!atLeastInSphere(currentSphere, requiredSphere)) {
			if (requiredSphere.contentEquals("Strong-Dynamic")) {
				if (currentSphere.contentEquals("Public")) {
					necessaryUpdate = "publicToSD";
				} else if (currentSphere.contentEquals("Global")) {
					necessaryUpdate = "globalToSD";
				} else if (currentSphere.contentEquals("Static")) {
					necessaryUpdate = "staticToSD";
				} else if (currentSphere.contentEquals("Weak-Dynamic")) {
					necessaryUpdate = "wdToSD";
				}
			}

			else if (requiredSphere.contentEquals("Weak-Dynamic")) {
				if (currentSphere.contentEquals("Public")) {
					necessaryUpdate = "publicToWD";
				} else if (currentSphere.contentEquals("Global")) {
					necessaryUpdate = "globalToWD";
				} else if (currentSphere.contentEquals("Static")) {
					necessaryUpdate = "staticToWD";
				}
			} else if (requiredSphere.contentEquals("Static")) {
				if (currentSphere.contentEquals("Public")) {
					necessaryUpdate = "publicToStatic";
				} else if (currentSphere.contentEquals("Global")) {
					necessaryUpdate = "globalToStatic";
				}
			}

			else if (requiredSphere.contentEquals("Global")) {
				if (currentSphere.contentEquals("Public")) {
					necessaryUpdate = "publicToGlobal";
				}
			}

		}

		return necessaryUpdate;
	}

	public static boolean atLeastInSphere(String currentSphere, String requiredSphere) {
		// return true if requiredSphere comprises the currentSphere
		// if currentSphere e.g. WD and requiredSphere SD return false

		// basic comparison -> if the spheres are the same, return true
		if (requiredSphere.contentEquals(currentSphere)) {
			return true;
		} else {
			if (requiredSphere.contentEquals("Strong-Dynamic")) {
				return false;
			}

			else if (requiredSphere.contentEquals("Weak-Dynamic")) {
				if (currentSphere.contentEquals("Strong-Dynamic")) {
					return true;
				}
			} else if (requiredSphere.contentEquals("Static")) {
				if (currentSphere.contentEquals("Weak-Dynamic") || currentSphere.contentEquals("Strong-Dynamic")) {
					return true;
				}

			} else if (requiredSphere.contentEquals("Global")) {
				if (currentSphere.contentEquals("Static") || currentSphere.contentEquals("Weak-Dynamic")
						|| currentSphere.contentEquals("Strong-Dynamic")) {
					return true;

				}
			}
		}
		return false;

	}

	public static boolean isNamedBranches(BpmnModelInstance modelInstance) {
		for (Gateway gtw : modelInstance.getModelElementsByType(Gateway.class)) {
			Collection<SequenceFlow> outgoing = gtw.getOutgoing();
			if (outgoing.size() >= 2 && gtw instanceof ExclusiveGateway) {
				for (SequenceFlow out : outgoing) {
					if (out.getName() == null) {
						return false;
					} 
					if(out.getName().isEmpty()) {
						return false;
					}
				}

			}
		}
		return true;
	}

	public static BpmnModelInstance preprocessModel(BpmnModelInstance modelInstance) {
		BpmnModelInstance preprocessedModel = null;
		try {
			preprocessedModel = doPreprocessing(modelInstance);
			// doPreprocessing(modelInstance, startNode, endNode, endPointOfSearch,false,new
			// LinkedList<FlowNode>(), new LinkedList<Gateway>(), new
			// LinkedList<LinkedList<FlowNode>>(), new LinkedList<FlowNode>(), new
			// LinkedList<LinkedList<FlowNode>>(), preprocessedModel);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return preprocessedModel;
	}

	public static BpmnModelInstance doPreprocessing(BpmnModelInstance modelInstance)
			throws NullPointerException, Exception {
		String id = modelInstance.getModelElementsByType(Process.class).iterator().next().getId().trim();
		String preprocessedModelId = id + "_preprocessed";
		BpmnModelInstance preprocessedModel = (BpmnModelInstance) CommonFunctionality2.deepCopy(modelInstance);
		preprocessedModel.getModelElementsByType(Process.class).iterator().next().setId(preprocessedModelId);

		// set of elements in parallel branches A,B are converted to a sequence <A,B>
		// change sequenceflows of parallel branches
		// one will be after the other
		// delete parallel split and join nodes
		Iterator<ParallelGateway> pGtwIter = preprocessedModel.getModelElementsByType(ParallelGateway.class).iterator();
		while (pGtwIter.hasNext()) {
			ParallelGateway pGtw = pGtwIter.next();
			if (pGtw.getOutgoing().size() >= 2) {
				// split found
				// get corresponding join
				ParallelGateway pSplit = pGtw;
				ParallelGateway pJoin = (ParallelGateway) CommonFunctionality2.getCorrespondingGtw(preprocessedModel,
						pSplit);

				// get nodes after pSplit
				// remove incoming seq flows of these nodes
				LinkedList<FlowNode> pBranchesFirstElements = new LinkedList<FlowNode>();
				Iterator<SequenceFlow> seqIter = pSplit.getOutgoing().iterator();
				while (seqIter.hasNext()) {
					SequenceFlow curr = seqIter.next();
					pBranchesFirstElements.add(curr.getTarget());
					curr.getParentElement().removeChildElement(curr);
				}

				// get nodeBeforePSplit
				// delete incoming seq of pSplit into first branch
				// seq may be from a xor-split -> preserve the name
				SequenceFlow incomingToPSplit = pSplit.getIncoming().iterator().next();
				String nameOfIncomingToPSplit = incomingToPSplit.getName();
				FlowNode nodeBeforePSplit = incomingToPSplit.getSource();
				incomingToPSplit.getParentElement().removeChildElement(incomingToPSplit);

				// connect nodeBeforePSplit to element in first branch
				FlowNode firstBranchFirstElement = pBranchesFirstElements.get(0);
				convertToUseBuilderAndConnectToNode(preprocessedModel, nodeBeforePSplit, nameOfIncomingToPSplit, firstBranchFirstElement);
				// get last element of first branch before join and remove outgoing seqFlow
				LinkedList<LinkedList<FlowNode>> pathsBetweenElements = goDfs(preprocessedModel,
						firstBranchFirstElement, pJoin, new LinkedList<SequenceFlow>(), new LinkedList<FlowNode>(),
						new LinkedList<LinkedList<FlowNode>>());
				LinkedList<FlowNode> pathBetweenElements = pathsBetweenElements.get(0);
				
				SequenceFlow seqFlowOfLastElementInBranchToPJoin = null;
				if(pathBetweenElements.size()==1) {
					//only join is after firstBranchFirstElement
					//get outgoing sequenceFlow of firstBranchFirstElement
					seqFlowOfLastElementInBranchToPJoin = firstBranchFirstElement.getOutgoing().iterator().next();					
				} else {
					int index = pathBetweenElements.size() - 2;
					seqFlowOfLastElementInBranchToPJoin = pathBetweenElements.get(index).getOutgoing()
					.iterator().next();
				}
								
				FlowNode lastElementInBranchBeforePJoin = seqFlowOfLastElementInBranchToPJoin.getSource();
				seqFlowOfLastElementInBranchToPJoin.getParentElement()
						.removeChildElement(seqFlowOfLastElementInBranchToPJoin);

				// connect to first element of second branch
				FlowNode secondBranchFirstElement = pBranchesFirstElements.get(1);
				convertToUseBuilderAndConnectToNode(preprocessedModel, lastElementInBranchBeforePJoin, null,
						secondBranchFirstElement);

				// get last element of second branch before join and remove outgoing seqFlow
				LinkedList<LinkedList<FlowNode>> pathsBetweenElements2 = goDfs(preprocessedModel,
						secondBranchFirstElement, pJoin, new LinkedList<SequenceFlow>(), new LinkedList<FlowNode>(),
						new LinkedList<LinkedList<FlowNode>>());
				LinkedList<FlowNode> pathBetweenElements2 = pathsBetweenElements2.get(0);
				
				SequenceFlow seqFlowOfLastElementInBranch2ToPJoin = null;
				if(pathBetweenElements2.size()==1) {
					//only join is after secondBranchFirstElement
					//get outgoing sequenceFlow of firstBranchFirstElement
					seqFlowOfLastElementInBranch2ToPJoin = secondBranchFirstElement.getOutgoing().iterator().next();					
				} else {
					int index2 = pathBetweenElements2.size() - 2;
					seqFlowOfLastElementInBranch2ToPJoin = pathBetweenElements2.get(index2).getOutgoing()
					.iterator().next();
				}
				
				FlowNode lastElementInBranch2BeforePJoin = seqFlowOfLastElementInBranch2ToPJoin.getSource();
				seqFlowOfLastElementInBranch2ToPJoin.getParentElement()
						.removeChildElement(seqFlowOfLastElementInBranch2ToPJoin);

				// connect last element of second branch to node after pJoin
				FlowNode nodeAfterPJoin = pJoin.getOutgoing().iterator().next().getTarget();
				convertToUseBuilderAndConnectToNode(preprocessedModel, lastElementInBranch2BeforePJoin, null, nodeAfterPJoin);
				
			}

		}

		for(ParallelGateway gtw: preprocessedModel.getModelElementsByType(ParallelGateway.class)) {

			// remove pSplit and pJoin
			gtw.getParentElement().removeChildElement(gtw);

		}
		
		for (SequenceFlow f : preprocessedModel.getModelElementsByType(SequenceFlow.class)) {
			if (f.getTarget() == null) {
				f.getParentElement().removeChildElement(f);
			}
			if (f.getSource() == null) {
				f.getParentElement().removeChildElement(f);
			}
		}

		for (Association assoc: preprocessedModel.getModelElementsByType(Association.class)) {
			if(assoc.getTarget() == null) {
				assoc.getParentElement().removeChildElement(assoc);
			}
			if(assoc.getSource() == null) {
				assoc.getParentElement().removeChildElement(assoc);
			}
			
		}
		
		for (BpmnEdge edge : preprocessedModel.getModelElementsByType(BpmnEdge.class)) {
			if (edge.getBpmnElement() == null) {
				edge.getParentElement().removeChildElement(edge);
			}
		}

		for (BpmnShape shape : preprocessedModel.getModelElementsByType(BpmnShape.class)) {
			if (shape.getBpmnElement() == null) {
				shape.getParentElement().removeChildElement(shape);
			}
		}
		
		return preprocessedModel;

	}

	public static BpmnModelInstance doPreprocessing(BpmnModelInstance modelInstance, FlowNode currNode,
			FlowNode endNode, FlowNode endPointOfSearch, boolean insidePBranch, LinkedList<FlowNode> queue,
			LinkedList<Gateway> openGtws, LinkedList<LinkedList<FlowNode>> parallelBranches,
			LinkedList<FlowNode> currentPath, LinkedList<LinkedList<FlowNode>> paths,
			BpmnModelInstance preprocessedModel) throws Exception {
		queue.add(currNode);

		while (!(queue.isEmpty())) {
			if (Thread.currentThread().isInterrupted()) {
				System.err.println("Interrupted! " + Thread.currentThread().getName());
				throw new InterruptedException();
			}

			FlowNode element = queue.pollLast();
			currentPath.add(element);
			System.out.println(element.getName() + ", " + element.getId());

			if (element.getId().contentEquals(endPointOfSearch.getId())) {
				// endEvent found
			}
			if (element.getId().equals(endNode.getId()) && element.getIncoming().size() >= 2) {
				// join found
				Gateway joinGtw = openGtws.pollLast();
				if (joinGtw instanceof ParallelGateway) {
					// check if all branches have been built
					parallelBranches.add(currentPath);
				}
				paths.add(currentPath);
			}

			// element is a split
			if (element instanceof Gateway && element.getOutgoing().size() >= 2) {
				for (SequenceFlow f : element.getOutgoing()) {
					openGtws.add((Gateway) element);
				}
			}

			Iterator<SequenceFlow> seqFlowIter = element.getOutgoing().iterator();
			while (seqFlowIter.hasNext()) {
				FlowNode successor = seqFlowIter.next().getTarget();
				if (element instanceof Gateway && element.getOutgoing().size() == 2) {
					// when a split is found - go dfs till the corresponding join is found
					FlowNode correspondingJoinGtw = null;
					try {
						correspondingJoinGtw = CommonFunctionality2.getCorrespondingGtw(modelInstance,
								(Gateway) element);
					} catch (Exception ex) {
						throw ex;
					}

					LinkedList<FlowNode> newPath = new LinkedList<FlowNode>();
					if (correspondingJoinGtw instanceof ParallelGateway) {
						insidePBranch = true;
						// fully build this branch
						// then append the other branch after this one
						for (LinkedList<FlowNode> pBranch : parallelBranches) {
							if (pBranch.getLast().getId().contentEquals(correspondingJoinGtw.getId())) {
								// one branch has already been built
								// append the current one after that
								newPath.addAll(pBranch);
								break;
							}
						}

					}
					if (newPath.isEmpty()) {
						newPath.addAll(currentPath);
					}
					doPreprocessing(modelInstance, successor, correspondingJoinGtw, endPointOfSearch, insidePBranch,
							queue, openGtws, parallelBranches, newPath, paths, preprocessedModel);
				} else {
					queue.add(successor);
				}

			}

		}

		return preprocessedModel;

	}

	public static LinkedList<LinkedList<FlowNode>> goDfs(BpmnModelInstance modelInstance, FlowNode currentNode,
			FlowNode endNode, LinkedList<SequenceFlow> stack, LinkedList<FlowNode> path,
			LinkedList<LinkedList<FlowNode>> paths) throws Exception {
		// route depth first through the process and add the successors and predecessors
		// to the nodes

		if (!currentNode.getId().contentEquals(endNode.getId())) {
			stack.addAll(currentNode.getOutgoing());
		}

		if (stack.isEmpty()) {
			return paths;
		}

		while (!stack.isEmpty()) {
			LinkedList<FlowNode> newPath = new LinkedList<FlowNode>();
			newPath.addAll(path);
			SequenceFlow currentSeqFlow = stack.poll();
			FlowNode targetFlowNode = currentSeqFlow.getTarget();
			newPath.add(targetFlowNode);
			if (targetFlowNode.getId().contentEquals(endNode.getId())) {
				paths.add(newPath);
			} else {
				goDfs(modelInstance, targetFlowNode, endNode, new LinkedList<SequenceFlow>(), newPath, paths);
			}
		}

		return paths;

	}
	
	

	public static void convertToUseBuilderAndConnectToNode(BpmnModelInstance modelInstance, FlowNode node, String nameOfNode,
			FlowNode nodeToConnectTo) throws Exception {
		try {
			FlowNode source = node;
			FlowNode target = nodeToConnectTo;

			if (source instanceof Task && !(source instanceof BusinessRuleTask)) {
				ManualTask manualTask = convertToManualTaskIfNecessary(modelInstance, (Task) source);
				source.replaceWithElement(manualTask);
				source = manualTask;
			}

			if (target instanceof Task && !(target instanceof BusinessRuleTask)) {
				ManualTask manualTask = convertToManualTaskIfNecessary(modelInstance, (Task) target);
				target.replaceWithElement(manualTask);
				target = manualTask;
			}
			
			source.builder().connectTo(target.getId());
			if(nameOfNode!=null && !nameOfNode.isEmpty()) {
				SequenceFlow generatedSeqFlow = target.getIncoming().iterator().next();
				generatedSeqFlow.setName(nameOfNode);
			}
			
			if (source instanceof ManualTask) {
				Task task = convertFromManualTaskToTaskIfNecessary(modelInstance, (ManualTask) source);
				source.replaceWithElement(task);
			}
			if (target instanceof ManualTask) {
				Task task = convertFromManualTaskToTaskIfNecessary(modelInstance, (ManualTask) target);
				target.replaceWithElement(task);
			}

		} catch (Exception e) {
			throw new Exception(e);
		}
	}

	public static ManualTask convertToManualTaskIfNecessary(BpmnModelInstance modelInstance, FlowNode node) {
		if (node instanceof Task) {
			Task taskNode = (Task) node;
			ManualTask changeToTask = modelInstance.newInstance(ManualTask.class);
			changeToTask.setName(taskNode.getName());
			changeToTask.setId(taskNode.getId());
			changeToTask.getIncoming().addAll(taskNode.getIncoming());
			changeToTask.getOutgoing().addAll(taskNode.getOutgoing());
			changeToTask.getDataOutputAssociations().addAll(taskNode.getDataOutputAssociations());
			changeToTask.getDataInputAssociations().addAll(taskNode.getDataInputAssociations());
			changeToTask.getProperties().addAll(taskNode.getProperties());
			return changeToTask;
		}
		return null;
	}

	public static Task convertFromManualTaskToTaskIfNecessary(BpmnModelInstance modelInstance, FlowNode node) {
		if (node instanceof ManualTask) {
			ManualTask taskNode = (ManualTask) node;
			Task changeBackToTask = modelInstance.newInstance(Task.class);
			changeBackToTask.setName(taskNode.getName());
			changeBackToTask.setId(taskNode.getId());
			changeBackToTask.getIncoming().addAll(taskNode.getIncoming());
			changeBackToTask.getOutgoing().addAll(taskNode.getOutgoing());
			changeBackToTask.getDataOutputAssociations().addAll(taskNode.getDataOutputAssociations());
			changeBackToTask.getDataInputAssociations().addAll(taskNode.getDataInputAssociations());
			changeBackToTask.getProperties().addAll(taskNode.getProperties());
			return changeBackToTask;
		}
		return null;
	}
	
	
	


	
	
	public static LinkedList<PModelWithAdditionalActors> getCheapestPModelsWithAdditionalActors(
			LinkedList<PModelWithAdditionalActors> pModels) {
		if (pModels == null) {
			return null;
		}
		List<PModelWithAdditionalActors> allModelsSortedByCheapest = pModels.parallelStream()
				.sorted((Comparator.comparingDouble(PModelWithAdditionalActors::getSumMeasure)))
				.collect(Collectors.toList());
		// allInstSortedByCheapest contains all solutions sorted by cheapest ones

		LinkedList<PModelWithAdditionalActors> allCheapestModels = new LinkedList<PModelWithAdditionalActors>();
		allCheapestModels.add(allModelsSortedByCheapest.get(0));

		for (int i = 1; i < allModelsSortedByCheapest.size(); i++) {
			PModelWithAdditionalActors currInst = allModelsSortedByCheapest.get(i);
			if (allCheapestModels.getFirst().getSumMeasure() == currInst.getSumMeasure()) {
				allCheapestModels.add(currInst);
			} else {
				return allCheapestModels;
			}
		}

		return allCheapestModels;
	}


	
	
}
