package ProcessModelGeneratorAndAnnotater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

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
import org.camunda.bpm.model.bpmn.instance.DataObject;
import org.camunda.bpm.model.bpmn.instance.DataObjectReference;
import org.camunda.bpm.model.bpmn.instance.DataOutputAssociation;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Gateway;
import org.camunda.bpm.model.bpmn.instance.ItemAwareElement;
import org.camunda.bpm.model.bpmn.instance.Lane;
import org.camunda.bpm.model.bpmn.instance.ManualTask;
import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import functionality.CommonFunctionality;

public class ProcessModellAnnotater {
	// class takes a process model and annotates it with dataObjects, readers,
	// writers, etc.
	private static BpmnModelInstance modelInstance;
	private static Collection<FlowNode> flowNodes;
	private static LinkedList<DataObjectReference> dataObjects;
	private static boolean modelWithLanes;
	private static LinkedList<String> differentParticipants;
	private static int idForTask;
	private static int idForBrt;
	
	
	public static void annotateModel(String pathToFile, List<Integer> countDataObjects, List<String> defaultSpheres,
			int dynamicWriter, int readerProb, int writerProb, int probPublicDecision) {
		File process = new File(pathToFile);
		modelInstance = Bpmn.readModelFromFile(process);
		
		idForTask = 1;
		idForBrt = 1;
		dataObjects = new LinkedList<DataObjectReference>();
		differentParticipants = new LinkedList<String>();
		
	
		// dynamicWriter is the probability to annotate a random required sphere for a
		// writer
		
		if (modelInstance.getModelElementsByType(Lane.class).isEmpty()) {
			modelWithLanes = false;
			for (FlowNode task : modelInstance.getModelElementsByType(Task.class)) {
				String taskName = task.getName();
				String participantName = taskName.substring(taskName.indexOf('['), taskName.indexOf(']') + 1);
				if (!differentParticipants.contains(participantName)) {
					differentParticipants.add(participantName);
				}
			}
		} else {
			modelWithLanes = true;
			for (Lane lane : modelInstance.getModelElementsByType(Lane.class)) {
				String laneName = "[" + lane.getName() + "]";
				if (!differentParticipants.contains(laneName)) {
					differentParticipants.add(laneName);
				}
			}

		}
		
		

		for (FlowNode f : modelInstance.getModelElementsByType(FlowNode.class)) {

			if (f instanceof StartEvent) {
				FlowNode successor = f.getOutgoing().iterator().next().getTarget();
				if (successor instanceof BusinessRuleTask) {
					System.out.println("First node after StartEvent can not be a brt - insert a task in between!");
					ProcessModellAnnotater.insertTaskAfterNode(f, successor, modelWithLanes);
				} else if (successor instanceof ParallelGateway && successor.getOutgoing().size()>=2) {
					//either: insert a task in before the parallel split 
					//or: check if on each branch there is a task before the brt
					System.out.println("First Node after Start Event is a Parallel-Split - insert a task in front of it!");
					ProcessModellAnnotater.insertTaskAfterNode(f, successor, modelWithLanes);
					
					
				}
			}
			// if there is no brt right before xor-split -> insert one
			if (f instanceof ExclusiveGateway) {
				ExclusiveGateway xor = (ExclusiveGateway) f;
				if (xor.getOutgoing().size() >= 2) {
					FlowNode nodeBeforeXorSplit = xor.getIncoming().iterator().next().getSource();

					if (!(nodeBeforeXorSplit instanceof BusinessRuleTask)) {
						// new businessRuletask needs to be inserted
						// delete old sequence flow
						System.out.println("Add new Brt before " + xor.getId());
						if (nodeBeforeXorSplit instanceof Task) {
							// the fluent builder doesn't work on tasks
							// change the task to a manual task and after using the fluent api change it
							// back
							ManualTask mt = modelInstance.newInstance(ManualTask.class);
							mt.setName(nodeBeforeXorSplit.getName());
							mt.setId(nodeBeforeXorSplit.getId());
							mt.getIncoming().addAll(nodeBeforeXorSplit.getIncoming());
							mt.getOutgoing().addAll(nodeBeforeXorSplit.getOutgoing());
							SequenceFlow toBeDeleted = xor.getIncoming().iterator().next();
							String idOfSFlow = toBeDeleted.getId();

							BpmnEdge edgeToBeDeleted = getEdge(idOfSFlow);

							edgeToBeDeleted.getParentElement().removeChildElement(edgeToBeDeleted);

							nodeBeforeXorSplit.replaceWithElement(mt);
							mt.getParentElement().removeChildElement(toBeDeleted);

							String nameForBrt = "InsertedBrt" + idForBrt;
							if (!modelWithLanes) {
								// add a random participantName to the nameForBr
								nameForBrt += " " + CommonFunctionality.getRandomItem(differentParticipants);
							}

							mt.builder().businessRuleTask().name(nameForBrt).connectTo(xor.getId());

							Task changeBackToTask = modelInstance.newInstance(Task.class);
							changeBackToTask.setName(mt.getName());
							changeBackToTask.setId(mt.getId());
							changeBackToTask.getIncoming().addAll(mt.getIncoming());
							changeBackToTask.getOutgoing().addAll(mt.getOutgoing());
							mt.replaceWithElement(changeBackToTask);
							idForBrt++;

						} else {
							
							SequenceFlow toBeDeleted = xor.getIncoming().iterator().next();
							
							String idOfSFlow = toBeDeleted.getId();

							BpmnEdge edgeToBeDeleted = getEdge(idOfSFlow);

							edgeToBeDeleted.getParentElement().removeChildElement(edgeToBeDeleted);
							nodeBeforeXorSplit.getParentElement().removeChildElement(toBeDeleted);

							String nameForBrt = "InsertedBrt" + idForBrt;
							if (!modelWithLanes) {
								// add a random participantName to the nameForBr
								nameForBrt += CommonFunctionality.getRandomItem(differentParticipants);
							}

							nodeBeforeXorSplit.builder().businessRuleTask().name(nameForBrt).connectTo(xor.getId());
							idForBrt++;
						}
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

					if (taskIsReaderOrWriter(writerProb) && !taskIsBrtFollowedByXorSplit(task)) {
						// task is a writer
						// businessRuleTasks right before xor-splits can not be writers
						boolean toBeInserted = true;
						for (DataOutputAssociation dao : task.getDataOutputAssociations()) {
							if (dao.getTarget().getId().contentEquals(dataORef.getId())) {
								toBeInserted = false;
							}
						}
						for (DataInputAssociation dia : task.getDataInputAssociations()) {
							for (ItemAwareElement iae : dia.getSources()) {
								if (iae.getId().contentEquals(dataORef.getId())) {
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
							if (randomCountSphere <= dynamicWriter) {
								generateDIElementForSphereAnnotation(task, dataORef, defaultSpheres);
							}
						}
					}
					if (taskIsReaderOrWriter(readerProb)) {
						// task is a reader
						boolean toBeInserted = true;
						for (DataOutputAssociation dao : task.getDataOutputAssociations()) {
							if (dao.getTarget().getId().contentEquals(dataORef.getId())) {
								toBeInserted = false;
							}
						}
						for (DataInputAssociation dia : task.getDataInputAssociations()) {
							for (ItemAwareElement iae : dia.getSources()) {
								if (iae.getId().contentEquals(dataORef.getId())) {
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

		
		// check again if a brt followed by a xor-split has at least one data object
		// connected!!!
		// insert tuples for xor-gateways if not already done

		for (Task task : modelInstance.getModelElementsByType(Task.class)) {
			if (taskIsBrtFollowedByXorSplit(task)) {
				BusinessRuleTask brt = (BusinessRuleTask) task;
				ExclusiveGateway gtw = (ExclusiveGateway) brt.getOutgoing().iterator().next().getTarget();
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
				// tuple can also be only: {Public}
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

					//check if decision will be public
					int decideIfPublic = ThreadLocalRandom.current().nextInt(0,
							101);
					
					TextAnnotation votersAnnotation = modelInstance.newInstance(TextAnnotation.class);
					StringBuilder textContentBuilder = new StringBuilder();
					textContentBuilder.append("[Voters]{");
					
					if(probPublicDecision>=decideIfPublic) {
						//xor will be annotated with {Public}
						
						textContentBuilder.append("Public");
						
					} else {
					
						
						
					int randomCountVotersNeeded = ThreadLocalRandom.current().nextInt(1, differentParticipants.size());
					
					
					//second argument -> voters that need to decide the same
					//must be bigger than the voters needed divided by 2 and rounded up to next int
					// e.g. if 3 voters are needed -> 2 or 3 must decide the same
					// e.g. if 5 voters are needed -> 3,4 or 5 must decide the same
					int lowerBound = (int) Math.ceil((double)randomCountVotersNeeded / 2);				
					
					
					int randomCountVotersSameDecision = ThreadLocalRandom.current().nextInt(lowerBound,
							randomCountVotersNeeded + 1);
					int randomCountIterations = ThreadLocalRandom.current().nextInt(1, 10 + 1);

					// generate TextAnnotations for xor-splits
					textContentBuilder.append(randomCountVotersNeeded + "," + randomCountVotersSameDecision
							+ "," + randomCountIterations);
				
					}
					textContentBuilder.append("}");
					
					votersAnnotation.setTextFormat("text/plain");
					Text text = modelInstance.newInstance(Text.class);
					text.setTextContent(textContentBuilder.toString());
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
			// query again
			// go dfs through process until all branches inside split have been visited
			// if reader is found check if on currentPath there is a writer
			// for the dataObject!
			//if writer is found inside a parallel split -> readers of same dataObject can not be in the other branch!
			//mark dataObject - remove readers in other branch when it is queried
		
			StartEvent st = modelInstance.getModelElementsByType(StartEvent.class).iterator().next();
			EndEvent end = modelInstance.getModelElementsByType(EndEvent.class).iterator().next();

			/*
			LinkedList<LinkedList<FlowNode>> paths = goDFSAndRepairModel(st, end, new LinkedList<FlowNode>(),
					new LinkedList<FlowNode>(), new LinkedList<Gateway>(), new LinkedList<FlowNode>(),
					new LinkedList<LinkedList<FlowNode>>(), new HashMap<ItemAwareElement, LinkedList<LockedBranch>>());
			*/

		

		try {			
			CommonFunctionality.isCorrectModel(modelInstance);
			writeChangesToFile(process);
			
		} catch (IOException | ParserConfigurationException | SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.err.println(e.getMessage());
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

	private static void writeChangesToFile(File process) throws IOException, ParserConfigurationException, SAXException {
		// validate and write model to file
		Bpmn.validateModel(modelInstance);
		
		String pathOfProcessFile = process.getParent();
		String fileName = process.getAbsolutePath().substring(process.getAbsolutePath().lastIndexOf("\\"), process.getAbsolutePath().indexOf(".bpmn"));
		String annotatedFileName = fileName+"_annotated.bpmn";
		File file = new File(pathOfProcessFile, annotatedFileName);

		file.getParentFile().mkdirs(); 
		System.out.println("FileCreated: "+file.createNewFile());
		
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

	private static boolean taskIsBrtFollowedByXorSplit(FlowNode node) {
		FlowNode directSuccessor = node.getOutgoing().iterator().next().getTarget();
		if (node instanceof BusinessRuleTask && directSuccessor instanceof ExclusiveGateway
				&& directSuccessor.getOutgoing().size() == 2) {
			return true;
		}
		return false;
	}

	public static LinkedList<LinkedList<FlowNode>> goDFSAndRepairModel(FlowNode startNode, FlowNode endNode,
			LinkedList<FlowNode> queue, LinkedList<FlowNode> parallelGtwQueue, LinkedList<Gateway> openSplits,
			LinkedList<FlowNode> currentPath, LinkedList<LinkedList<FlowNode>> paths, HashMap<ItemAwareElement, LinkedList<LockedBranch>>lockedBranches) {
		// go DFS inside the XOR till corresponding join is found
		queue.add(startNode);

		while (!(queue.isEmpty())) {
			FlowNode element = queue.poll();
			System.out.println("Element: "+element.getName()+", "+element.getId());
			
			// when a reader is found -> check if on the currentPath to the reader there
			// is a writer to the dataObject
			if (element instanceof Task) {
				Task currTask = (Task) element;
				if (!currTask.getDataInputAssociations().isEmpty()) {
					for (DataInputAssociation dia : currTask.getDataInputAssociations()) {
						for (ItemAwareElement iae : dia.getSources()) {
							// reader has been found
							// check if there is a writer on the currentPath to the reader
							// randomly change one available task on the currentPath to a writer if there is no writer
							boolean writerOnPath = false;	
							LinkedList<ParallelGateway> insideParallelSplit = new LinkedList<ParallelGateway>();
							
							boolean removeReadersAndWriters = false;
							

							// available will contain the tasks that one of will be randomly chosen to be a writer
							// if no writer on the path to the reader is found
							//remove reading and writing tasks in a parallel branch if there is a writer in the other one!
							
							HashMap<ParallelGateway, FlowNode> pSplitAndFollowerNode = new HashMap<ParallelGateway, FlowNode>();
							LinkedList<FlowNode>availableTasksToChangeToWriters = new LinkedList<FlowNode>();
														
							for (int i = 0; i < currentPath.size()-1; i++) {
								FlowNode currentPathEl = currentPath.get(i);
								
								if(currentPathEl instanceof ParallelGateway ) {
									if(currentPathEl.getId().contains("_split")) {
										insideParallelSplit.add((ParallelGateway)currentPathEl);
										FlowNode directSuccessor = currentPath.get(i+1);
										pSplitAndFollowerNode.putIfAbsent((ParallelGateway)currentPathEl, directSuccessor);
					
										
										
									} else if (currentPathEl.getId().contains("_join")) {
										ParallelGateway closedPSplit = insideParallelSplit.pollLast();
										pSplitAndFollowerNode.remove(closedPSplit);
										
										
									}
							
								}
								
								if (currentPathEl instanceof Task) {	
									Task currentPathTask = (Task) currentPathEl;
									boolean locked = false;
										if(insideParallelSplit.size()>0) {
											//currentPathTask is inside parallel branch
											//check if it is a locked one
											for(ParallelGateway pSplit: insideParallelSplit) {
												if(lockedBranches.get(iae)!=null) {
													for(LockedBranch lBranch: lockedBranches.get(iae)) {
														if(lBranch.getpSplit().equals(pSplit)) {
															//branch is locked for the dataObject
															locked = true;
														}
													}
													
												}
											}
											
										}
										
									
									if(CommonFunctionality.isWriterForDataObject(currentPathTask, iae)) {									
											//currentPathTask is a writer for the dataObject
											writerOnPath = true;
											
											if(!insideParallelSplit.isEmpty()) {
												//currentPathTask is a writer inside a parallel Branch
												ParallelGateway pSplit = insideParallelSplit.getLast();
												String pJoinId = pSplit.getName()+"_join";
												ParallelGateway pJoin = modelInstance.getModelElementById(pJoinId);
												
												//remove readers and writers of that dataObject in the other branch
												//get the first node after pSplit in the other branch(es)
												
												ParallelGateway lastOpenedPSplit = insideParallelSplit.getLast();
												FlowNode successorOfBranchWithWriter = pSplitAndFollowerNode.get(lastOpenedPSplit);
												LinkedList<FlowNode> successorOtherBranches = new LinkedList<FlowNode>();
												
												for(SequenceFlow directSuccSeqFlow: lastOpenedPSplit.getOutgoing()) {
													FlowNode directSucc = directSuccSeqFlow.getTarget();
													if(!directSucc.equals(successorOfBranchWithWriter)) {
														successorOtherBranches.add(directSucc);
													}
													
												}
												
												//remover readers and writers for the dataObject in the other branch(es)
												for(FlowNode successorOtherBranch:successorOtherBranches ) {
												LinkedList<LinkedList<FlowNode>>branchesToLock = CommonFunctionality.removeReadersAndWritersInBranch(modelInstance, successorOtherBranch, pJoin, iae);
												
												for(LinkedList<FlowNode>branchToLock: branchesToLock) {
													LockedBranch lBranch = new LockedBranch(pSplit, iae, pJoin, branchToLock);
													if(!lockedBranches.get(iae).contains(lBranch)) {
														lockedBranches.get(iae).add(lBranch);
													}
												}
												
												}
												
											}
											
											
										} else {
											//currentPathTask is not a writer
											if(!locked) {
												availableTasksToChangeToWriters.add(currentPathTask);
											}
											
											
										}

									
									
									
								}

							}

							if (!writerOnPath) {
								// no writer on the currentPath

								if (currentPath.size() == 1 && currentPath.get(0) instanceof StartEvent) {
									// if the path only contains the startEvent - change the currTask from reader to
									// writer for that dataO if the reader is not a brt
									if (taskIsBrtFollowedByXorSplit(currTask)) {
										System.err.println("Not possible!" +currTask.getId()+" can not be changed to a writer!");										

									} else {
										
										CommonFunctionality.removeTaskAsReaderFromDataObject(modelInstance, currTask, iae);
										
										DataOutputAssociation dao = modelInstance
												.newInstance(DataOutputAssociation.class);
										dao.setTarget(iae);
										currTask.addChildElement(dao);
										DataObjectReference daoR = modelInstance.getModelElementById(iae.getId());

										ProcessModellAnnotater.generateDIElementForWriter(dao,
												ProcessModellAnnotater.getShape(iae.getId()),
												ProcessModellAnnotater.getShape(currTask.getId()));
										System.out.println("Change made: " + currTask.getName()
												+ " has been modified from reader to writer for" + daoR.getName());
									}
								} else {
									// randomly select a Task on the path and make it a writer to the dataObject
									// BusinessRuleTasks with a xor-split right after as well as tasks inside a lockedBranch are not part of the selection

									// if the randomly selected Task is inside a Parallel Branch -> lock the other
									// branch
									// readers and writers for a specific dataObject can only be in a specific
									// parallel branch
									// readers and writers for a specific dataObject need to be removed from the
									// locked path

									if(availableTasksToChangeToWriters.size()>0) {
										int randomInt = ThreadLocalRandom.current().nextInt(0, availableTasksToChangeToWriters.size());
										FlowNode taskToBeWriter = availableTasksToChangeToWriters.get(randomInt);
										DataOutputAssociation dao = modelInstance.newInstance(DataOutputAssociation.class);
										dao.setTarget(iae);
										taskToBeWriter.addChildElement(dao);
										DataObjectReference daoR = modelInstance.getModelElementById(iae.getId());

										ProcessModellAnnotater.generateDIElementForWriter(dao,
												ProcessModellAnnotater.getShape(iae.getId()),
												ProcessModellAnnotater.getShape(taskToBeWriter.getId()));

										System.out.println("Change made: " + taskToBeWriter.getName()
												+ " has been added as a writer for " + daoR.getName());
									} else {
										System.err.println("No task found for random selection");
									}
										
									
									
								}

							}

						}
					}

				}

			}

			currentPath.add(element);

			if (element.getId().equals(endNode.getId())) {

				paths.add(currentPath);

				if (endNode instanceof Gateway && endNode.getIncoming().size() == 2) {

					Gateway joinGtw = (Gateway) element;

					// when a xor-join is found - poll the last opened xor gateway from the stack
					Gateway lastOpenedSplitGtw = openSplits.pollLast();

					if (!openSplits.isEmpty()) {
						if (!openSplits.contains(lastOpenedSplitGtw)) {
							// when the openXorStack does not contain the lastOpenedXor anymore, all
							// branches to the joinGtw have been visited
							// go from joinGtw to the Join of the last opened split in the stack
							Gateway lastOpenedSplit = openSplits.getLast();
							String correspondingJoinId = lastOpenedSplit.getName() + "_join";
							FlowNode correspondingJoin = modelInstance.getModelElementById(correspondingJoinId);

							goDFSAndRepairModel(joinGtw.getOutgoing().iterator().next().getTarget(), correspondingJoin,
									queue, parallelGtwQueue, openSplits, currentPath, paths, lockedBranches);

						}
					} else if (openSplits.isEmpty()&&!queue.isEmpty()) {
						// when there are no open splits gtws but there are still elements in the queue
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
									parallelGtwQueue, openSplits, newPath, paths, lockedBranches);
						}

					}

				}
				element = queue.poll();
				if (element == null && queue.isEmpty()) {

					return paths;
				}
				
			}

			if (element instanceof Gateway && element.getOutgoing().size() == 2) {
				// add the xor split to the openXorStack 1 times for each outgoing paths
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
					String idCorrespondingJoinGtw = element.getName()+"_join";

					for (Gateway gtw : modelInstance.getModelElementsByType(Gateway.class)) {
						if (gtw.getId().contentEquals(idCorrespondingJoinGtw) && gtw.getIncoming().size()==2) {
							correspondingJoinGtw = gtw;
							break;
						}

					}
					if(correspondingJoinGtw == null) {
						System.out.println("no corresponding join for "+element.getId()+", "+element.getOutgoing().size());
						
					}

					LinkedList<FlowNode> newPath = new LinkedList<FlowNode>();
					newPath.addAll(currentPath);
					
					
					System.out.println("LockedBranches:"+lockedBranches.size());
				
					goDFSAndRepairModel(successor, correspondingJoinGtw, queue, parallelGtwQueue, openSplits, newPath,
							paths, lockedBranches);
				} else {

					queue.add(successor);

				}

			}

		}

		return paths;

	}


	
	private static FlowNode insertTaskAfterNode(FlowNode node, FlowNode successor, boolean modelWithLanes) {
		SequenceFlow toBeDeleted = node.getOutgoing().iterator().next();
		String idOfSFlow = toBeDeleted.getId();

		BpmnEdge edgeToBeDeleted = getEdge(idOfSFlow);

		edgeToBeDeleted.getParentElement().removeChildElement(edgeToBeDeleted);
		node.getParentElement().removeChildElement(toBeDeleted);

		String taskName = "InsertedTask" + idForTask;
		if (!modelWithLanes) {
			taskName += " " + CommonFunctionality.getRandomItem(differentParticipants);
		}
		idForTask++;

		node.builder().manualTask().name(taskName).connectTo(successor.getId());
		FlowNode insertedTask = node.getOutgoing().iterator().next().getTarget();

		Task changeBackToTask = modelInstance.newInstance(Task.class);
		changeBackToTask.setName(insertedTask.getName());
		changeBackToTask.setId(insertedTask.getId());
		changeBackToTask.getIncoming().addAll(insertedTask.getIncoming());
		changeBackToTask.getOutgoing().addAll(insertedTask.getOutgoing());
		insertedTask.replaceWithElement(changeBackToTask);
		return changeBackToTask;
		
	}

}
