package ProcessModelGeneratorAndAnnotater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Association;
import org.camunda.bpm.model.bpmn.instance.BusinessRuleTask;
import org.camunda.bpm.model.bpmn.instance.DataInputAssociation;
import org.camunda.bpm.model.bpmn.instance.DataObject;
import org.camunda.bpm.model.bpmn.instance.DataObjectReference;
import org.camunda.bpm.model.bpmn.instance.DataOutputAssociation;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.ItemAwareElement;
import org.camunda.bpm.model.bpmn.instance.Property;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.camunda.bpm.model.bpmn.instance.Text;
import org.camunda.bpm.model.bpmn.instance.TextAnnotation;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnLabel;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.camunda.bpm.model.bpmn.instance.dc.Bounds;
import org.camunda.bpm.model.bpmn.instance.di.Plane;
import org.camunda.bpm.model.bpmn.instance.di.Waypoint;
import org.camunda.bpm.model.bpmn.instance.Lane;
import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import Mapping.BPMNBusinessRuleTask;
import Mapping.BPMNElement;
import Mapping.BPMNEndEvent;
import Mapping.BPMNExclusiveGateway;
import Mapping.BPMNGateway;
import Mapping.BPMNParallelGateway;
import Mapping.BPMNParticipant;
import Mapping.Combination;
import Mapping.ProcessInstanceWithVoters;
import Mapping.RequiredUpdate;
import Mapping.VoterForXorArc;

public class ProcessModellAnnotater {
	// class takes a process model and annotates it with dataObjects, readers,
	// writers, etc.
	private static BpmnModelInstance modelInstance;
	private static Collection<FlowNode> flowNodes;
	private static LinkedList<DataObjectReference> dataObjects = new LinkedList<DataObjectReference>();

	public static void annotateModel(String pathToFile, List<Integer> countDataObjects, List<String> defaultSpheres,
			int sphereProb, int readerProb, int writerProb) {
		File process = new File(pathToFile);
		modelInstance = Bpmn.readModelFromFile(process);

		// convert tasks that are right before XOR-Splits to be BusinessRuleTask
		// if there is no task right before xor-split -> insert one
		for (FlowNode f : modelInstance.getModelElementsByType(FlowNode.class)) {
			int id = 1;
			if (f instanceof ExclusiveGateway) {
				ExclusiveGateway xor = (ExclusiveGateway) f;
				if (xor.getOutgoing().size() >= 2) {
					FlowNode someNode = xor.getIncoming().iterator().next().getSource();

					if (someNode instanceof Task
							&& (!(xor.getIncoming().iterator().next().getSource() instanceof BusinessRuleTask))) {
						BusinessRuleTask bt = modelInstance.newInstance(BusinessRuleTask.class);
						bt.setId(someNode.getId());
						bt.setName(someNode.getName());
						someNode.getParentElement().replaceChildElement(someNode, bt);
					} else {
						// new businessRuletask needs to be inserted
						// delete old sequence flow

						SequenceFlow toBeDeleted = someNode.getOutgoing().iterator().next();
						String idOfSFlow = toBeDeleted.getId();

						BpmnEdge edgeToBeDeleted = getEdge(idOfSFlow);

						edgeToBeDeleted.getParentElement().removeChildElement(edgeToBeDeleted);
						someNode.getParentElement().removeChildElement(toBeDeleted);

						someNode.builder().businessRuleTask().name("Brt" + id).connectTo(xor.getId());
						id++;
					}
				}
			}
		}

		flowNodes = modelInstance.getModelElementsByType(FlowNode.class);

		// randomly generate dataObjects within the range from
		// countDataObjects[minCountDataObjects, maxCountDataObjects]
		int randomCountDataObjects = ThreadLocalRandom.current().nextInt(countDataObjects.get(0),
				countDataObjects.get(1) + 1);

		for (int i = 0; i < randomCountDataObjects; i++) {
			// create a new dataObject and add it to the model
			Object[] nodesAsArray = flowNodes.toArray();
			FlowNode someNode = (FlowNode) nodesAsArray[i];

			DataObject currDataObject = modelInstance.newInstance(DataObject.class);
			currDataObject.setId("DataObject" + i + 1);
			// need to add it to the process element in the xml file!!!
			flowNodes.iterator().next().getParentElement().addChildElement(currDataObject);

			DataObjectReference dataORef = modelInstance.newInstance(DataObjectReference.class);
			dataORef.setDataObject(currDataObject);
			dataORef.setName("[D" + (i + 1) + "]{someDataObject" + (i + 1) + "}");
			currDataObject.getParentElement().addChildElement(dataORef);
			dataObjects.add(dataORef);

			// add the default sphere to the xml file
			// randomly choose one of the given ones
			TextAnnotation defaultSphere = modelInstance.newInstance(TextAnnotation.class);
			int randomSphereCount = ThreadLocalRandom.current().nextInt(0, defaultSpheres.size());
			String textContent = "Default: [D" + (i + 1) + "]{" + defaultSpheres.get(randomSphereCount) + "}";

			defaultSphere.setId("TextAnnotation_defaultSphereForD" + (i + 1));
			defaultSphere.setTextFormat("text/plain");
			Text text = modelInstance.newInstance(Text.class);
			text.setTextContent(textContent);
			defaultSphere.setText(text);
			currDataObject.getParentElement().addChildElement(defaultSphere);

			// add the shape of the dataObject to the xml file
			generateDIElementsForDataObject(dataORef, someNode);

			// add the shape of the text annotation to the xml file
			generateShapeForTextAnnotation(defaultSphere, dataORef);

			// iterate through all tasks of the process and assign readers and writers
			// if task is a writer - add the sphere
			// task can only be either reader or writer to a specific dataObject
			for (FlowNode node : flowNodes) {
				if (node instanceof Task) {
					Task task = (Task) node;

					if (taskIsReaderOrWriter(writerProb)) {
						// task is a writer
						boolean toBeInserted = true;
						for(DataOutputAssociation dao: task.getDataOutputAssociations()) {
							if(dao.getTarget().getId().contentEquals(dataORef.getId())) {
								toBeInserted = false;
							}
						}
						for(DataInputAssociation dia: task.getDataInputAssociations()) {							
							for(ItemAwareElement iae: dia.getSources()) {
							if(iae.getId().contentEquals(dataORef.getId())) {
								toBeInserted = false;
							}
							}
						}
						if (toBeInserted) {
							DataOutputAssociation dao = modelInstance.newInstance(DataOutputAssociation.class);

							dao.setTarget(dataORef);
							task.addChildElement(dao);

							generateDIElementForWriter(dao, getShape(dataORef.getId()), getShape(task.getId()));
							// add sphere annotation for writer
							int randomCountSphere = ThreadLocalRandom.current().nextInt(0, 100 + 1);
							if (randomCountSphere <= sphereProb) {
								generateDIElementForSphereAnnotation(task, dataORef, defaultSpheres);

							}
						}
					}
					if (taskIsReaderOrWriter(readerProb)) {
						// task is a reader
						boolean toBeInserted = true;
						for(DataOutputAssociation dao: task.getDataOutputAssociations()) {
							if(dao.getTarget().getId().contentEquals(dataORef.getId())) {
								toBeInserted = false;
							}
						}
						for(DataInputAssociation dia: task.getDataInputAssociations()) {							
							for(ItemAwareElement iae: dia.getSources()) {
							if(iae.getId().contentEquals(dataORef.getId())) {
								toBeInserted = false;
							}
							}
						}
						
						if (toBeInserted) {
							DataInputAssociation dia = modelInstance.newInstance(DataInputAssociation.class);
							Property p1 = modelInstance.newInstance(Property.class);
							p1.setName("__targetRef_placeholder");
							task.addChildElement(p1);
							dia.setTarget(p1);
							task.getDataInputAssociations().add(dia);
							dia.getSources().add(dataORef);

							generateDIElementForReader(dia, getShape(dataORef.getId()), getShape(task.getId()));
						}
					}

				}

			}

		}

		// query again
		// go dfs through process until all branches inside split have been visited
		// if reader is found go backwards and check if on each path there is a writer
		// for the dataObject!
		// in a parallel split there must only be writers/readers in one branch for a
		// specific dataObject!

		StartEvent st = modelInstance.getModelElementsByType(StartEvent.class).iterator().next();
		EndEvent end = modelInstance.getModelElementsByType(EndEvent.class).iterator().next();

		goDFSAndRepairModel(st, end, new LinkedList<FlowNode>(), new LinkedList<FlowNode>(), new LinkedList<FlowNode>(),
				new LinkedList<FlowNode>(), new LinkedList<LinkedList<FlowNode>>());

		// check again if a brt followed by a xor-split has at least one data object
		// connected!!!
		// insert tuples for xor-gateways if not already done

		for (FlowNode f : modelInstance.getModelElementsByType(FlowNode.class)) {
			if (f instanceof BusinessRuleTask) {
				BusinessRuleTask brt = (BusinessRuleTask) f;
				if (brt.getOutgoing().iterator().next().getTarget() instanceof ExclusiveGateway) {
					ExclusiveGateway gtw = (ExclusiveGateway) brt.getOutgoing().iterator().next().getTarget();
					if (gtw.getOutgoing().size() >= 2) {
						// brt is followed by a xor-split
						if (brt.getDataInputAssociations().isEmpty()) {
							// when brt doesnt have a dataObject connected -> randomly connect one
							int randomCount = ThreadLocalRandom.current().nextInt(0, dataObjects.size());

							DataInputAssociation dia = modelInstance.newInstance(DataInputAssociation.class);
							Property p1 = modelInstance.newInstance(Property.class);
							p1.setName("__targetRef_placeholder");
							brt.addChildElement(p1);
							dia.setTarget(p1);
							brt.getDataInputAssociations().add(dia);
							dia.getSources().add(dataObjects.get(randomCount));
							generateDIElementForReader(dia, getShape(dataObjects.get(randomCount).getId()),
									getShape(brt.getId()));

						}

						// insert tuples for xor-gateways if there are non already
						// tuples are e.g. (3,2,5) -> 3 Voters needed, 2 have to decide the same, loop
						// goes 5 times until the final decider will take decision
						boolean insert = true;
						for (TextAnnotation tx : modelInstance.getModelElementsByType(TextAnnotation.class)) {
							for (Association assoc : modelInstance.getModelElementsByType(Association.class)) {
								if (assoc.getSource().equals(gtw) && assoc.getTarget().equals(tx)) {
									if (tx.getTextContent().startsWith("[Voters]")) {
										insert = false;
									}
								}

							}
						}

						if (insert) {
							Collection<Lane> lanes = modelInstance.getModelElementsByType(Lane.class);
							int randomCountVotersNeeded = ThreadLocalRandom.current().nextInt(0, lanes.size());
							int randomCountVotersSameDecision = ThreadLocalRandom.current().nextInt(0,
									randomCountVotersNeeded + 1);
							int randomCountIterations = ThreadLocalRandom.current().nextInt(0, 10 + 1);

							// generate TextAnnotations for xor-splits
							TextAnnotation votersAnnotation = modelInstance.newInstance(TextAnnotation.class);
							String textContent = "[Voters]{" + randomCountVotersNeeded + ","
									+ randomCountVotersSameDecision + "," + randomCountIterations + "}";

							votersAnnotation.setTextFormat("text/plain");
							Text text = modelInstance.newInstance(Text.class);
							text.setTextContent(textContent);
							votersAnnotation.setText(text);
							gtw.getParentElement().addChildElement(votersAnnotation);

							generateShapeForTextAnnotation(votersAnnotation, gtw);

							Association assoc = modelInstance.newInstance(Association.class);
							assoc.setSource(gtw);
							assoc.setTarget(votersAnnotation);
							gtw.getParentElement().addChildElement(assoc);
							// DI element for the edge
							BpmnEdge edge = modelInstance.newInstance(BpmnEdge.class);
							edge.setBpmnElement(assoc);
							Waypoint wp1 = modelInstance.newInstance(Waypoint.class);
							wp1.setX(gtw.getDiagramElement().getBounds().getX() + 50);
							wp1.setY(gtw.getDiagramElement().getBounds().getY());
							Waypoint wp2 = modelInstance.newInstance(Waypoint.class);
							wp2.setX(gtw.getDiagramElement().getBounds().getX() + 50);
							wp2.setY(gtw.getDiagramElement().getBounds().getY() - 50);

							edge.getWaypoints().add(wp1);
							edge.getWaypoints().add(wp2);
							modelInstance.getModelElementsByType(Plane.class).iterator().next().addChildElement(edge);

						}

					}

				}

			}

		}

		try {
			writeChangesToFile();
		} catch (IOException | ParserConfigurationException | SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void generateDIElementForSphereAnnotation(Task writerTask, DataObjectReference daoR,
			List<String> defaultSpheres) {
		TextAnnotation writerSphere = modelInstance.newInstance(TextAnnotation.class);
		int randomSphereCount = ThreadLocalRandom.current().nextInt(0, defaultSpheres.size());
		String newSphere = defaultSpheres.get(randomSphereCount);

		String textContent = daoR.getName().replaceAll("\\{.*?\\}", newSphere);

		writerSphere.setTextFormat("text/plain");
		Text text = modelInstance.newInstance(Text.class);
		text.setTextContent(textContent);
		writerSphere.setText(text);
		writerTask.getParentElement().addChildElement(writerSphere);

		Association assoc = modelInstance.newInstance(Association.class);
		assoc.setSource(writerTask);
		assoc.setTarget(writerSphere);
		writerTask.getParentElement().addChildElement(assoc);

		// add the DI Element
		BpmnShape shape = modelInstance.newInstance(BpmnShape.class);
		shape.setBpmnElement(writerSphere);
		Bounds bounds = modelInstance.newInstance(Bounds.class);
		bounds.setX(writerTask.getDiagramElement().getBounds().getX() + 50);
		bounds.setY(writerTask.getDiagramElement().getBounds().getY() - 50);
		bounds.setWidth(103);
		bounds.setHeight(43);
		shape.setBounds(bounds);
		modelInstance.getModelElementsByType(Plane.class).iterator().next().addChildElement(shape);

		// DI element for the edge
		BpmnEdge edge = modelInstance.newInstance(BpmnEdge.class);
		edge.setBpmnElement(assoc);
		Waypoint wp1 = modelInstance.newInstance(Waypoint.class);
		wp1.setX(writerTask.getDiagramElement().getBounds().getX() + 50);
		wp1.setY(writerTask.getDiagramElement().getBounds().getY());
		Waypoint wp2 = modelInstance.newInstance(Waypoint.class);
		wp2.setX(writerTask.getDiagramElement().getBounds().getX() + 50);
		wp2.setY(writerTask.getDiagramElement().getBounds().getY() - 50);

		edge.getWaypoints().add(wp1);
		edge.getWaypoints().add(wp2);
		modelInstance.getModelElementsByType(Plane.class).iterator().next().addChildElement(edge);

	}

	private static void generateShapeForTextAnnotation(TextAnnotation txt, DataObjectReference ref) {
		BpmnShape shapeForAnnotation = modelInstance.newInstance(BpmnShape.class);
		shapeForAnnotation.setBpmnElement(txt);
		Bounds bounds = modelInstance.newInstance(Bounds.class);
		bounds.setX(getShape(ref.getId()).getBounds().getX() - 200);
		bounds.setY(getShape(ref.getId()).getBounds().getY() - 40);
		bounds.setWidth(284);
		bounds.setHeight(30);
		shapeForAnnotation.setBounds(bounds);
		modelInstance.getModelElementsByType(Plane.class).iterator().next().addChildElement(shapeForAnnotation);

	}

	private static void generateShapeForTextAnnotation(TextAnnotation txt, FlowNode ref) {
		BpmnShape shapeForAnnotation = modelInstance.newInstance(BpmnShape.class);
		shapeForAnnotation.setBpmnElement(txt);
		Bounds bounds = modelInstance.newInstance(Bounds.class);
		bounds.setX(getShape(ref.getId()).getBounds().getX() - 200);
		bounds.setY(getShape(ref.getId()).getBounds().getY() - 40);
		bounds.setWidth(284);
		bounds.setHeight(30);
		shapeForAnnotation.setBounds(bounds);
		modelInstance.getModelElementsByType(Plane.class).iterator().next().addChildElement(shapeForAnnotation);

	}

	private static BpmnShape getShape(String id) {

		for (BpmnShape shape : modelInstance.getModelElementsByType(BpmnShape.class)) {
			if (shape.getBpmnElement().getId().equals(id)) {
				return shape;
			}
		}
		return null;

	}

	private static BpmnEdge getEdge(String id) {
		for (BpmnEdge edge : modelInstance.getModelElementsByType(BpmnEdge.class)) {
			if (edge.getBpmnElement().getId().equals(id)) {
				return edge;
			}
		}
		return null;

	}

	private static void generateDIElementForWriter(DataOutputAssociation dao, BpmnShape daoR,
			BpmnShape writerTaskShape) {
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

	private static void generateDIElementForReader(DataInputAssociation dia, BpmnShape daoR,
			BpmnShape readerTaskShape) {
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

	private static boolean taskIsReaderOrWriter(int probRange) {

		int randomCountDataObjects = ThreadLocalRandom.current().nextInt(0, 101);
		if (probRange >= randomCountDataObjects) {
			return true;
		}
		return false;

	}

	private static void generateDIElementsForDataObject(DataObjectReference dataORef, FlowNode someNode) {
		BpmnShape dataOShape = modelInstance.newInstance(BpmnShape.class);
		dataOShape.setBpmnElement(dataORef);

		// place the dataObject above someNode
		BpmnShape shapeOfNode = getShape(someNode.getId());

		Bounds dataOBounds = modelInstance.newInstance(Bounds.class);
		dataOBounds.setX(shapeOfNode.getBounds().getX());
		dataOBounds.setY(shapeOfNode.getBounds().getY() - 200);
		dataOBounds.setWidth(36);
		dataOBounds.setHeight(50);

		BpmnLabel dataOLabel = modelInstance.newInstance(BpmnLabel.class);
		dataOShape.setBpmnLabel(dataOLabel);

		dataOShape.setBounds(dataOBounds);

		modelInstance.getModelElementsByType(Plane.class).iterator().next().addChildElement(dataOShape);

	}

	private static void writeChangesToFile() throws IOException, ParserConfigurationException, SAXException {
		// validate and write model to file
		Bpmn.validateModel(modelInstance);
		File file = File.createTempFile("modelWithAnnotation", ".bpmn",
				new File("C:\\Users\\Micha\\OneDrive\\Desktop"));
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

	public static LinkedList<LinkedList<FlowNode>> goDFSAndRepairModel(FlowNode startNode, FlowNode endNode,
			LinkedList<FlowNode> queue, LinkedList<FlowNode> parallelGtwQueue, LinkedList<FlowNode> openXorStack,
			LinkedList<FlowNode> currentPath, LinkedList<LinkedList<FlowNode>> paths) {
		// go DFS inside the XOR till corresponding join is found
		queue.add(startNode);

		boolean reachedParallelEndGateway = false;

		while (!(queue.isEmpty())) {
			FlowNode element = queue.poll();

			// when a reader is found -> check if on each possible path to the reader there
			// is a writer to the dataObject
			if (element instanceof Task) {
				Task currTask = (Task) element;
				if (!currTask.getDataInputAssociations().isEmpty()) {
					for (DataInputAssociation dia : currTask.getDataInputAssociations()) {
						for (ItemAwareElement iae : dia.getSources()) {
							//reader has been found
							System.out.println("Reader: " + element.getName() + ", " + iae.getId());
							//check if there is a writer on currentPath to the reader
							//randomly change one task on the currentPath to a writer if there is no writer
							boolean writerOnTaskForDataO = false;
							for(FlowNode currentPathEl: currentPath) {
								if(currentPathEl instanceof Task) {
									Task currentPathTask = (Task)currentPathEl;
									for(DataOutputAssociation dao: currentPathTask.getDataOutputAssociations()) {
										if(dao.getTarget().equals(iae)) {
											writerOnTaskForDataO=true;
											break;
										}
												
									}
								}
							}
							
							
							if(writerOnTaskForDataO==false) {
								if(currentPath.size()==1&&currentPath.get(0)instanceof StartEvent) {
									//if the path only contains the startEvent - change the currTask from reader to writer for that dataO

								} else {
									//randomly select a Task on the path and make it a writer 
										
									
									
								}
							}
							
						}
					}

				}

				if (!currTask.getDataOutputAssociations().isEmpty()) {
					for (DataOutputAssociation dao : currTask.getDataOutputAssociations()) {
						ItemAwareElement iae = dao.getTarget();
						System.out.println("Writer: " + element.getName() + ", " + iae.getId());
					}
				}
			}

			currentPath.add(element);

			if (element.getId().equals(endNode.getId())) {

				paths.add(currentPath);

				if (endNode instanceof ExclusiveGateway && endNode.getId().contains("_join")) {

					ExclusiveGateway joinGtw = (ExclusiveGateway) element;

					// when a xor-join is found - poll the last opened xor gateway from the stack
					ExclusiveGateway lastOpenedXor = (ExclusiveGateway) openXorStack.pollLast();

					if (!openXorStack.isEmpty()) {
						if (!openXorStack.contains(lastOpenedXor)) {
							// when the openXorStack does not contain the lastOpenedXor anymore, all
							// branches to the joinGtw have been visited
							// go from joinGtw to the Join of the last opened xor-split in the stack
							FlowNode lastOpenedSplit = openXorStack.getLast();
							String correspondingJoinId = lastOpenedSplit.getName() + "_join";
							FlowNode correspondingJoin = modelInstance.getModelElementById(correspondingJoinId);

							goDFSAndRepairModel(joinGtw, correspondingJoin, queue, parallelGtwQueue, openXorStack,
									currentPath, paths);

						}
					} else if (openXorStack.isEmpty()) {
						// when there are no open Xor gtws
						// go from the successor of the element to bpmnEnd since the currentElement has
						// already been added to the path
						LinkedList<LinkedList<FlowNode>> newPaths = new LinkedList<LinkedList<FlowNode>>();

						for (LinkedList<FlowNode> path : paths) {
							if (path.getLast().equals(element)) {
								LinkedList<FlowNode> newPathAfterXorJoin = new LinkedList<FlowNode>();
								newPathAfterXorJoin.addAll(path);
								newPaths.add(newPathAfterXorJoin);
							}
						}

						for (LinkedList<FlowNode> newPath : newPaths) {
							goDFSAndRepairModel(element.getOutgoing().iterator().next().getTarget(),
									modelInstance.getModelElementsByType(EndEvent.class).iterator().next(), queue,
									parallelGtwQueue, openXorStack, newPath, paths);
						}

					}

				} else if (endNode instanceof BPMNEndEvent) {
					// when the endnode of the process is found

				}

				element = queue.poll();
				if (element == null && queue.isEmpty()) {
					int id = 1;
					for (LinkedList<FlowNode> path : paths) {
						System.out.println("Path with ID " + id);
						for (FlowNode el : path) {
							System.out.println(el.getId());
						}
						id++;
					}

					return paths;
				}

			}

			if (element instanceof ExclusiveGateway && element.getId().contains("_split")) {
				// add the xor split to the openXorStack 1 times for each outgoing paths
				int amountOfOutgoingPaths = element.getOutgoing().size();
				int i = 0;
				while (i < amountOfOutgoingPaths) {
					openXorStack.add((ExclusiveGateway) element);
					i++;
				}

			}

			if (element instanceof ParallelGateway && element.getId().contains("_split")) {
				for (SequenceFlow outgoingFlow : element.getOutgoing()) {
					parallelGtwQueue.add(outgoingFlow.getTarget());
				}
			}

			if (element instanceof ParallelGateway && element.getId().contains("_join")) {
				parallelGtwQueue.poll();
				if (!parallelGtwQueue.isEmpty()) {
					reachedParallelEndGateway = true;
				}
			}

			for (SequenceFlow outgoingFlow : element.getOutgoing()) {
				FlowNode successor = outgoingFlow.getTarget();
				if (element instanceof ExclusiveGateway && element.getId().equals("_split")) {
					// when a xor-split is found - go dfs till the corresponding join is found

					String correspondingJoinId = element.getName() + "_join";
					FlowNode correspondingJoinGtw = modelInstance.getModelElementById(correspondingJoinId);

					LinkedList<FlowNode> newPath = new LinkedList<FlowNode>();
					newPath.addAll(currentPath);

					goDFSAndRepairModel(successor, correspondingJoinGtw, queue, parallelGtwQueue, openXorStack, newPath,
							paths);
				} else {

					if (reachedParallelEndGateway == false) {
						queue.add(successor);
					}

				}

			}
			reachedParallelEndGateway = false;
		}

		return paths;

	}

}
