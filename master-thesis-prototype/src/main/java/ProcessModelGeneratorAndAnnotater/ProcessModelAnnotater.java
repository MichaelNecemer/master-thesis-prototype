package ProcessModelGeneratorAndAnnotater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.Callable;
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
import org.camunda.bpm.model.bpmn.instance.DataObject;
import org.camunda.bpm.model.bpmn.instance.DataObjectReference;
import org.camunda.bpm.model.bpmn.instance.DataOutputAssociation;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
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

public class ProcessModelAnnotater implements Callable<File> {
	// class takes a process model and annotates it with dataObjects, readers,
	// writers, etc.

	private File process;
	private BpmnModelInstance modelInstance;
	private Collection<FlowNode> flowNodes;
	private LinkedList<DataObjectReference> dataObjects;
	private boolean modelWithLanes;
	private LinkedList<String> differentParticipants;
	private int idForTask;
	private int idForBrt;
	private String pathToFile;
	private String pathWhereToCreateAnnotatedFile;
	private String fileNameSuffix;
	private boolean dataObjectsConnectedToBrts;
	private String fileNameForNewFile;
	private String directoryForNewFile;
	private LinkedHashMap<String, Object[]> methodsToRunWithinCall;

	public ProcessModelAnnotater(String pathToFile, String pathWhereToCreateAnnotatedFile, String fileNameSuffix)
			throws Exception {
		this.process = new File(pathToFile);
		this.modelInstance = Bpmn.readModelFromFile(process);
		this.pathToFile = pathToFile;
		this.pathWhereToCreateAnnotatedFile = pathWhereToCreateAnnotatedFile;
		this.fileNameSuffix = fileNameSuffix;

		this.idForTask = 1;
		this.idForBrt = 1;
		this.dataObjects = new LinkedList<DataObjectReference>();
		this.dataObjects.addAll(this.modelInstance.getModelElementsByType(DataObjectReference.class));
		this.differentParticipants = new LinkedList<String>();
		this.dataObjectsConnectedToBrts = false;
		this.fileNameForNewFile = this.generateFileNameForNewFile(process, pathWhereToCreateAnnotatedFile,
				fileNameSuffix);
		this.directoryForNewFile = pathWhereToCreateAnnotatedFile + File.separatorChar + fileNameForNewFile;
		this.methodsToRunWithinCall = new LinkedHashMap<String, Object[]>();
		this.setDifferentParticipants();
		this.addFlowNodesIfNecessary();
	}

	public void setMethodsToRunWithinCall(LinkedHashMap<String, Object[]> methodsToRunWithinCall) {
		// key -> method to call
		// value -> arguments for that methods
		this.methodsToRunWithinCall = methodsToRunWithinCall;
	}

	public void connectDataObjectsToBrtsAndTuplesForXorSplits(int minDataObjectsPerDecision,
			int maxDataObjectsPerDecision, int amountParticipantsPerDecisionLowerBound,
			int amountParticipantsPerDecisionUpperBound, int probPublicDecision, boolean allDataObjectsUniquePerGtw) {
		if (!this.dataObjects.isEmpty()) {
			LinkedList<DataObjectReference> dataObjectsToChoseFrom = new LinkedList<DataObjectReference>();
			dataObjectsToChoseFrom.addAll(this.dataObjects);
			for (Task task : this.modelInstance.getModelElementsByType(Task.class)) {
				if (taskIsBrtFollowedByXorSplit(task)) {
					if (maxDataObjectsPerDecision <= 0) {
						int amountDataObjects = modelInstance.getModelElementsByType(DataObjectReference.class).size();
						if (amountDataObjects == minDataObjectsPerDecision) {
							maxDataObjectsPerDecision = amountDataObjects;
						} else if (amountDataObjects > minDataObjectsPerDecision) {
							maxDataObjectsPerDecision = ThreadLocalRandom.current().nextInt(minDataObjectsPerDecision,
									amountDataObjects + 1);

						}
					}

					int upperBoundAnnotatedDataObjects = maxDataObjectsPerDecision;
					if (maxDataObjectsPerDecision > this.dataObjects.size()) {
						upperBoundAnnotatedDataObjects = this.dataObjects.size();
					}
					int boundedValue = 0;
					if (upperBoundAnnotatedDataObjects == minDataObjectsPerDecision) {
						boundedValue = upperBoundAnnotatedDataObjects;
					} else {
						boundedValue = ThreadLocalRandom.current().nextInt(minDataObjectsPerDecision,
								(upperBoundAnnotatedDataObjects + 1));

					}

					if (allDataObjectsUniquePerGtw) {
						dataObjectsToChoseFrom = this.addRandomUniqueDataObjectsForBrt(task, dataObjectsToChoseFrom,
								boundedValue);
					} else {
						this.addRandomDataObjectsForBrt(task, boundedValue);
					}
					this.addTuplesForXorSplits(task, probPublicDecision, amountParticipantsPerDecisionLowerBound,
							amountParticipantsPerDecisionUpperBound);

				}

			}
		}
		this.dataObjectsConnectedToBrts = true;

	}

	public File checkCorrectnessAndWriteChangesToFile() throws Exception {
		File newFile = null;
		CommonFunctionality.isCorrectModel(this.modelInstance);
		newFile = this.writeChangesToFile();
		if (newFile == null) {
			throw new Exception("Model not valid - try another readers/writers assertion!");
		}
		return newFile;

	}

	public File writeChangesToFileWithoutCorrectnessCheck()
			throws IOException, ParserConfigurationException, SAXException {
		return this.writeChangesToFile();
	}

	public void generateDataObjects(int amountDataObjectsToCreate, LinkedList<String> defaultSpheres) {
		for (int i = 0; i < amountDataObjectsToCreate; i++) {
			// create a new dataObject and add it to the model
			Object[] nodesAsArray = flowNodes.toArray();
			FlowNode someNode = (FlowNode) nodesAsArray[i];

			DataObject currDataObject = modelInstance.newInstance(DataObject.class);
			currDataObject.setId("DataObject" + (i + 1));
			// need to add it to the process element in the xml file!!!
			flowNodes.iterator().next().getParentElement().addChildElement(currDataObject);

			DataObjectReference dataORef = this.modelInstance.newInstance(DataObjectReference.class);
			dataORef.setDataObject(currDataObject);
			dataORef.setName("[D" + (i + 1) + "]{someDataObject" + (i + 1) + "}");
			currDataObject.getParentElement().addChildElement(dataORef);
			this.dataObjects.add(dataORef);

			// add the default sphere to the xml file
			// randomly choose one of the given ones
			TextAnnotation defaultSphere = this.modelInstance.newInstance(TextAnnotation.class);
			int randomSphereCount = ThreadLocalRandom.current().nextInt(0, defaultSpheres.size());
			String textContent = "Default: [D" + (i + 1) + "]{" + defaultSpheres.get(randomSphereCount) + "}";

			defaultSphere.setId("TextAnnotation_defaultSphereForD" + (i + 1));
			defaultSphere.setTextFormat("text/plain");
			Text text = this.modelInstance.newInstance(Text.class);
			text.setTextContent(textContent);
			defaultSphere.setText(text);
			currDataObject.getParentElement().addChildElement(defaultSphere);

			// add the shape of the dataObject to the xml file
			generateDIElementsForDataObject(dataORef, someNode);

			// add the shape of the text annotation to the xml file
			generateShapeForTextAnnotation(defaultSphere, dataORef);

		}

	}

	public void annotateModelWithFixedAmountOfReadersAndWriters(int amountWriters, int amountReaders, int dynamicWriter,
			LinkedList<String> defaultSpheresForDynamicWriter) throws InterruptedException, Exception {
		// amountWriters and amountReaders is for whole process
		// randomly generate subAmounts for each dataObject
		// get each dataObject -> insert a writer for it
		// if it is needed for a decision -> writer must be on path to the brt!
		// amountWriters must be >= dataObjects, since every dataObject will need to be
		// written before it can get read

		if (!this.dataObjects.isEmpty()) {
			if (amountWriters < this.dataObjects.size()) {
				throw new Exception("Amount of writers must be >= amount of DataObjects!");
			}

			if (!this.dataObjectsConnectedToBrts) {
				throw new Exception(
						"Method: connectDataObjectsToBrtsAndTuplesForXorSplits() needs to be called before annotating the model with readers and writers!");
			}

			// check how many readers there are already in the model
			int sumExistingReaders = modelInstance.getModelElementsByType(DataInputAssociation.class).size();
			int sumExistingWriters = modelInstance.getModelElementsByType(DataOutputAssociation.class).size();

			if (amountWriters >= sumExistingWriters) {
				amountWriters = amountWriters - sumExistingWriters;
			}

			if (amountReaders >= sumExistingReaders) {
				amountReaders = amountReaders - sumExistingReaders;
			}
			System.out.println("Writers to be inserted: " + amountWriters);
			System.out.println("Readers to be inserted: " + amountReaders);
			List<LinkedList<Integer>> subAmountWritersLists = CommonFunctionality
					.computeRepartitionNumber(amountWriters, this.dataObjects.size(), 1);
			int randomNum = ThreadLocalRandom.current().nextInt(0, subAmountWritersLists.size());
			LinkedList<Integer> subAmountWriters = subAmountWritersLists.get(randomNum);

			List<LinkedList<Integer>> subAmountReadersLists = CommonFunctionality
					.computeRepartitionNumber(amountReaders, this.dataObjects.size(), 0);

			int randomNum2 = ThreadLocalRandom.current().nextInt(0, subAmountReadersLists.size());
			LinkedList<Integer> subAmountReaders = subAmountReadersLists.get(randomNum2);

			HashMap<BusinessRuleTask, LinkedList<Task>> possibleWritersBeforeBrt = new HashMap<BusinessRuleTask, LinkedList<Task>>();
			List<Task> taskList = new LinkedList<Task>();
			for (Task task : this.modelInstance.getModelElementsByType(Task.class)) {
				taskList.add(task);
				if (ProcessModelAnnotater.taskIsBrtFollowedByXorSplit(task)) {
					LinkedList<Task> tasksBeforeBrt = new LinkedList<Task>();
					LinkedList<LinkedList<FlowNode>> pathsBetweenStartAndBrt = CommonFunctionality
							.getAllPathsBetweenNodes(this.modelInstance, this.modelInstance
									.getModelElementsByType(StartEvent.class).iterator().next().getId(), task.getId());
					for (LinkedList<FlowNode> subPath : pathsBetweenStartAndBrt) {
						LinkedList<ExclusiveGateway> exclGtwStack = new LinkedList<ExclusiveGateway>();
						LinkedList<ParallelGateway> paraGtwStack = new LinkedList<ParallelGateway>();

						for (FlowNode f : subPath) {
							if (f instanceof ExclusiveGateway) {
								ExclusiveGateway ex = (ExclusiveGateway) f;
								if (f.getOutgoing().size() == 2) {
									exclGtwStack.push(ex);
								} else if (f.getIncoming().size() == 2) {
									exclGtwStack.pollLast();
								}
							}

							if (f instanceof ParallelGateway) {
								ParallelGateway pa = (ParallelGateway) f;
								if (f.getOutgoing().size() == 2) {
									paraGtwStack.push(pa);
								} else if (f.getIncoming().size() == 2) {
									paraGtwStack.pollLast();
								}
							}

							if (f instanceof Task && !ProcessModelAnnotater.taskIsBrtFollowedByXorSplit(f)) {
								// possible lastWriters must be unconditional and in front of the brt
								// because they may get read in the other branch of the brt too
								// if brt is inside a parallel branch -> last writer can be in the same branch
								// before the brt
								// since there can not be a reader/writer to same data object in the other
								// branch (brt is a reader!)

								boolean insert = true;
								if (exclGtwStack.isEmpty() && paraGtwStack.isEmpty()) {
									// f is unconditional and on path to brt
									// insert = true;
								} else if (!exclGtwStack.isEmpty()) {
									// f is conditional -> can not be a lastWriter
									insert = false;
								} else if (exclGtwStack.isEmpty() && !paraGtwStack.isEmpty()) {
									// task has to be in same branch as brt!
									LinkedList<LinkedList<FlowNode>> pathsBetweenTaskAndBrt = CommonFunctionality
											.getAllPathsBetweenNodes(this.modelInstance, f.getId(), task.getId());
									for (LinkedList<FlowNode> subPathFromTaskToBrt : pathsBetweenTaskAndBrt) {
										for (FlowNode fNode : subPathFromTaskToBrt) {
											if (fNode instanceof ParallelGateway) {
												insert = false;
												break;
											}
										}
									}

								}

								if (insert && !tasksBeforeBrt.contains(f)) {
									tasksBeforeBrt.add((Task) f);
								}
							}

						}
					}
					possibleWritersBeforeBrt.putIfAbsent((BusinessRuleTask) task, tasksBeforeBrt);
				}
			}

			if (!possibleWritersBeforeBrt.isEmpty()) {

				int index = 0;
				LinkedList<DataObjectReference> alreadyMapped = new LinkedList<DataObjectReference>();
				for (Entry<BusinessRuleTask, LinkedList<Task>> entry : possibleWritersBeforeBrt.entrySet()) {
					// get the dataObjects connected to the brt
					BusinessRuleTask currBrt = entry.getKey();
					for (DataInputAssociation dia : currBrt.getDataInputAssociations()) {
						for (ItemAwareElement iae : dia.getSources()) {
							DataObjectReference daoR = CommonFunctionality
									.getDataObjectReferenceForItemAwareElement(modelInstance, iae);
							if (daoR != null && !(alreadyMapped.contains(daoR))) {
								Task writerBeforeDecision = CommonFunctionality.getRandomItem(entry.getValue());
								this.addReadersAndWritersForDataObjectWithFixedAmounts(subAmountWriters.get(index),
										subAmountReaders.get(index), dynamicWriter, daoR,
										defaultSpheresForDynamicWriter, writerBeforeDecision);
								index++;
								alreadyMapped.add(daoR);
							}
						}

					}

				}
			} else {
				// there is no brt inserted
				// choose a random task
				for (int i = 0; i < dataObjects.size(); i++) {
					Task writerBeforeDecision = CommonFunctionality.getRandomItem(taskList);
					this.addReadersAndWritersForDataObjectWithFixedAmounts(subAmountWriters.get(i),
							subAmountReaders.get(i), dynamicWriter, dataObjects.get(i), defaultSpheresForDynamicWriter,
							writerBeforeDecision);
				}
			}

		}

	}

	private void generateDIElementForSphereAnnotation(Task writerTask, DataObjectReference daoR,
			List<String> defaultSpheres) {
		TextAnnotation writerSphere = modelInstance.newInstance(TextAnnotation.class);
		int randomSphereCount = ThreadLocalRandom.current().nextInt(0, defaultSpheres.size());
		String newSphere = defaultSpheres.get(randomSphereCount);

		String textContent = daoR.getName().replaceAll("(?<=\\{).*?(?=\\})", newSphere);

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

	private void generateShapeForTextAnnotation(TextAnnotation txt, DataObjectReference ref) {
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

	private void generateShapeForTextAnnotation(TextAnnotation txt, FlowNode ref) {
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

	private BpmnShape getShape(String id) {

		for (BpmnShape shape : modelInstance.getModelElementsByType(BpmnShape.class)) {
			if (shape.getBpmnElement().getId().equals(id)) {
				return shape;
			}
		}
		return null;

	}

	private BpmnEdge getEdge(String id) {
		for (BpmnEdge edge : modelInstance.getModelElementsByType(BpmnEdge.class)) {
			if (edge.getBpmnElement().getId().equals(id)) {
				return edge;
			}
		}
		return null;

	}

	private void generateDIElementForWriter(DataOutputAssociation dao, BpmnShape daoR, BpmnShape writerTaskShape) {
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

	private void generateDIElementForReader(DataInputAssociation dia, BpmnShape daoR, BpmnShape readerTaskShape) {
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

	private void generateDIElementsForDataObject(DataObjectReference dataORef, FlowNode someNode) {
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

	private String generateFileNameForNewFile(File process, String directoryToStoreAnnotatedModel,
			String suffixFileName) {
		String fileName = process.getName().substring(0, process.getName().indexOf(".bpmn"));
		int fileNumber = 0;

		if (directoryToStoreAnnotatedModel.isEmpty() || directoryToStoreAnnotatedModel == null) {
			// write it to same directory as process
			directoryToStoreAnnotatedModel = process.getParent();
		}

		File dir = new File(directoryToStoreAnnotatedModel);
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			for (File child : directoryListing) {
				if (child.getName().contains(fileName) && child.getName().contains(".bpmn")) {
					String nameOfFileInsideDirectory = child.getName().substring(0, child.getName().indexOf(".bpmn"));
					if (nameOfFileInsideDirectory.contains("_annotated")) {
						Pattern p = Pattern.compile("[0-9]+");
						Matcher m = p.matcher(nameOfFileInsideDirectory);
						while (m.find()) {
							int num = Integer.parseInt(m.group());
							if (num > fileNumber) {
								fileNumber = num;
							}
						}
					}
				}
			}
		}
		fileNumber++;
		StringBuilder annotatedFileNameBuilder = new StringBuilder();

		annotatedFileNameBuilder.append(fileName + "_annotated" + fileNumber);
		if (!suffixFileName.contentEquals("_annotated")) {
			annotatedFileNameBuilder.append(suffixFileName);
		}
		annotatedFileNameBuilder.append(".bpmn");
		return annotatedFileNameBuilder.toString();
	}

	private File writeChangesToFile() throws IOException, ParserConfigurationException, SAXException {
		// validate and write model to file
		Bpmn.validateModel(this.modelInstance);
		String fileName = this.fileNameForNewFile;

		File file = CommonFunctionality.createFileWithinDirectory(this.pathWhereToCreateAnnotatedFile, fileName);

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

	private static boolean taskIsBrtFollowedByXorSplit(FlowNode node) {
		FlowNode directSuccessor = node.getOutgoing().iterator().next().getTarget();
		if (node instanceof BusinessRuleTask && directSuccessor instanceof ExclusiveGateway
				&& directSuccessor.getOutgoing().size() == 2) {
			return true;
		}
		return false;
	}

	private FlowNode insertTaskAfterNode(FlowNode node, FlowNode successor, boolean modelWithLanes) {
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

	private LinkedList<DataObjectReference> addRandomUniqueDataObjectsForBrt(Task brtTask,
			LinkedList<DataObjectReference> dataObjectsToChoseFrom, int dataObjectsPerDecision) {
		LinkedList<DataObjectReference> daoR = new LinkedList<DataObjectReference>();
		daoR.addAll(dataObjectsToChoseFrom);
		if (taskIsBrtFollowedByXorSplit(brtTask)) {
			BusinessRuleTask brt = (BusinessRuleTask) brtTask;

			if (brt.getDataInputAssociations().isEmpty()) {
				// randomly connect dataObjects till dataObjectsPerDecision is reached
				int i = 0;
				while (i < dataObjectsPerDecision && !dataObjectsToChoseFrom.isEmpty()) {
					int randomCount = ThreadLocalRandom.current().nextInt(0, daoR.size());
					DataInputAssociation dia = modelInstance.newInstance(DataInputAssociation.class);
					Property p1 = modelInstance.newInstance(Property.class);
					p1.setName("__targetRef_placeholder");
					brt.addChildElement(p1);
					dia.setTarget(p1);
					brt.getDataInputAssociations().add(dia);
					dia.getSources().add(daoR.get(randomCount));
					generateDIElementForReader(dia, getShape(daoR.get(randomCount).getId()), getShape(brt.getId()));
					daoR.remove(daoR.get(randomCount));
					i++;
				}
			}
		}

		return daoR;

	}

	private void addRandomDataObjectsForBrt(Task brtTask, int dataObjectsPerDecision) {

		if (taskIsBrtFollowedByXorSplit(brtTask)) {
			BusinessRuleTask brt = (BusinessRuleTask) brtTask;

			if (brt.getDataInputAssociations().isEmpty()) {
				// randomly connect dataObjects till dataObjectsPerDecision is reached
				int i = 0;
				LinkedList<DataObjectReference> doRefs = new LinkedList<DataObjectReference>();
				doRefs.addAll(dataObjects);
				while (i < dataObjectsPerDecision && !doRefs.isEmpty()) {
					int randomCount = ThreadLocalRandom.current().nextInt(0, doRefs.size());
					DataInputAssociation dia = modelInstance.newInstance(DataInputAssociation.class);
					Property p1 = modelInstance.newInstance(Property.class);
					p1.setName("__targetRef_placeholder");
					brt.addChildElement(p1);
					dia.setTarget(p1);
					brt.getDataInputAssociations().add(dia);
					dia.getSources().add(doRefs.get(randomCount));
					generateDIElementForReader(dia, getShape(doRefs.get(randomCount).getId()), getShape(brt.getId()));
					doRefs.remove(doRefs.get(randomCount));
					i++;
				}
			}
		}

	}

	private void addTuplesForXorSplits(Task brtTask, int probPublicDecision,
			int lowerBoundAmountParticipantsPerDecision, int upperBoundAmountParticipantsPerDecision) {

		if (taskIsBrtFollowedByXorSplit(brtTask)) {
			BusinessRuleTask brt = (BusinessRuleTask) brtTask;
			ExclusiveGateway gtw = (ExclusiveGateway) brt.getOutgoing().iterator().next().getTarget();

			// insert tuples for xor-gateways
			// tuples are e.g. (3,2,5) -> 3 Voters needed, 2 have to decide the same, loop
			// goes 5 times until the troubleshooter will take decision
			// tuple can also be only: {Public}
			boolean insert = true;
			for (TextAnnotation tx : this.modelInstance.getModelElementsByType(TextAnnotation.class)) {
				for (Association assoc : this.modelInstance.getModelElementsByType(Association.class)) {
					if (assoc.getSource().equals(gtw) && assoc.getTarget().equals(tx)) {
						if (tx.getTextContent().startsWith("[Voters]")) {
							insert = false;
						}
					}

				}
			}

			if (insert) {
				// check if decision will be public
				int decideIfPublic = ThreadLocalRandom.current().nextInt(1, 101);

				TextAnnotation votersAnnotation = modelInstance.newInstance(TextAnnotation.class);
				StringBuilder textContentBuilder = new StringBuilder();
				textContentBuilder.append("[Voters]{");

				if (probPublicDecision >= decideIfPublic) {
					// xor will be annotated with {Public}

					textContentBuilder.append("Public");

				} else {
					int randomCountVotersNeeded = ThreadLocalRandom.current().nextInt(
							lowerBoundAmountParticipantsPerDecision, upperBoundAmountParticipantsPerDecision + 1);

					// second argument -> voters that need to decide the same
					// must be bigger than the voters needed divided by 2 and rounded up to next int
					// e.g. if 3 voters are needed -> 2 or 3 must decide the same
					// e.g. if 5 voters are needed -> 3,4 or 5 must decide the same
					int lowerBound = (int) Math.ceil((double) randomCountVotersNeeded / 2);

					int randomCountVotersSameDecision = ThreadLocalRandom.current().nextInt(lowerBound,
							randomCountVotersNeeded + 1);
					int randomCountIterations = ThreadLocalRandom.current().nextInt(1, 10 + 1);

					// generate TextAnnotations for xor-splits
					textContentBuilder.append(randomCountVotersNeeded + "," + randomCountVotersSameDecision + ","
							+ randomCountIterations);

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

	private void setDifferentParticipants() {
		if (this.modelInstance.getModelElementsByType(Lane.class).isEmpty()) {
			this.modelWithLanes = false;
			for (FlowNode task : modelInstance.getModelElementsByType(Task.class)) {
				String taskName = task.getName();
				String participantName = taskName.substring(taskName.indexOf('['), taskName.indexOf(']') + 1);
				if (!differentParticipants.contains(participantName)) {
					differentParticipants.add(participantName);
				}
			}
		} else {
			this.modelWithLanes = true;
			for (Lane lane : modelInstance.getModelElementsByType(Lane.class)) {
				String laneName = "[" + lane.getName() + "]";
				if (!differentParticipants.contains(laneName)) {
					differentParticipants.add(laneName);
				}
			}

		}
	}

	private void addFlowNodesIfNecessary() {
		for (FlowNode f : this.modelInstance.getModelElementsByType(FlowNode.class)) {

			if (f instanceof StartEvent) {
				FlowNode successor = f.getOutgoing().iterator().next().getTarget();
				if (successor instanceof BusinessRuleTask) {
					System.out.println("First node after StartEvent can not be a brt - insert a task in between!");
					this.insertTaskAfterNode(f, successor, modelWithLanes);
				} else if (successor instanceof ParallelGateway && successor.getOutgoing().size() >= 2) {
					// either: insert a task in before the parallel split
					// or: check if on each branch there is a task before the brt
					// in this implementation: insert a task
					System.out.println(
							"First Node after Start Event is a Parallel-Split - insert a task in front of it!");
					this.insertTaskAfterNode(f, successor, modelWithLanes);

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
								// add a random participantName to the nameForBrt
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
	}

	private void generateDataObjectsWithDefaultSpheres(int amountDataObjectsToCreate, List<String> defaultSpheres) {
		for (int i = 0; i < amountDataObjectsToCreate; i++) {
			// create a new dataObject and add it to the model
			Object[] nodesAsArray = flowNodes.toArray();
			FlowNode someNode = (FlowNode) nodesAsArray[i];

			DataObject currDataObject = modelInstance.newInstance(DataObject.class);
			currDataObject.setId("DataObject" + (i + 1));
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

		}
	}

	private void addReadersAndWritersForDataObjectWithProbabilities(int writerProb, int readerProb, int dynamicWriter,
			DataObjectReference dataORef, List<String> defaultSpheres) {
		// iterate through all tasks of the process and assign readers and writers
		// if task is a writer - add the dynamic writer sphere if necessary
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
						// add sphere annotation for writer if necessary
						int randomCountSphere = ThreadLocalRandom.current().nextInt(1, 100 + 1);
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

	public void addRandomDecisionsForBrts(int probToTakeAlreadyMappedVariableForDecision) {
		LinkedList<String> operators = new LinkedList<String>();
		operators.add("+");
		operators.add("-");
		operators.add("*");
		operators.add("/");

		String comparisonOperator = "==";
		// random decisons for testing will always be of form e.g. D1.randomChar
		// operator D2.randomChar == randomInteger
		HashMap<DataObjectReference, LinkedList<Character>> alreadyMappedVariables = new HashMap<DataObjectReference, LinkedList<Character>>();
		for (FlowNode f : this.flowNodes) {
			if (f instanceof ExclusiveGateway) {
				ExclusiveGateway xor = (ExclusiveGateway) f;
				if (xor.getOutgoing().size() >= 2) {
					FlowNode nodeBeforeXorSplit = xor.getIncoming().iterator().next().getSource();

					if ((nodeBeforeXorSplit instanceof BusinessRuleTask)) {
						BusinessRuleTask currBrt = (BusinessRuleTask) nodeBeforeXorSplit;
						StringBuilder decisionBuilder = new StringBuilder();
						decisionBuilder.append("[Decision]{");

						int dataObjectsPerDecisionLeft = currBrt.getDataOutputAssociations().size();

						for (DataOutputAssociation doa : currBrt.getDataOutputAssociations()) {
							ItemAwareElement iae = doa.getTarget();
							for (DataObjectReference daoR : this.dataObjects) {
								if (daoR.getId().contentEquals(iae.getId())) {
									String daoRName = daoR.getName();
									decisionBuilder.append(
											daoR.getName().substring(daoRName.indexOf('[') + 1, daoRName.indexOf(']')));
									decisionBuilder.append('.');
									if (!alreadyMappedVariables.get(daoR).isEmpty()) {
										int prob = ThreadLocalRandom.current().nextInt(0, 100);
										if (probToTakeAlreadyMappedVariableForDecision >= prob) {
											LinkedList<Character> alreadyMapped = alreadyMappedVariables.get(daoR);
											char randomCharAlreadyMapped = CommonFunctionality
													.getRandomItem(alreadyMapped);
											decisionBuilder.append(randomCharAlreadyMapped);
										}

									} else {
										// get a random letter from alphabet
										char randomChar = Character.MIN_VALUE;
										do {
											Random r = new Random();
											randomChar = (char) (r.nextInt(26) + 'a');
										} while (alreadyMappedVariables.get(daoR).contains(randomChar));
										decisionBuilder.append(randomChar);
										alreadyMappedVariables.computeIfAbsent(daoR, v -> new LinkedList<Character>())
												.add(randomChar);
									}

									dataObjectsPerDecisionLeft--;

									if (dataObjectsPerDecisionLeft > 0) {
										// get a random operator from operators
										String randomOperator = CommonFunctionality.getRandomItem(operators);
										decisionBuilder.append(randomOperator);
									}

								}
							}

						}
						decisionBuilder.append(comparisonOperator);
						double randomValue = ThreadLocalRandom.current().nextDouble(0, 101);
						decisionBuilder.append(randomValue);

					}
				}
			}
		}
	}

	private void addReadersAndWritersForDataObjectWithFixedAmounts(int amountWriters, int amountReaders,
			int dynamicWriterProb, DataObjectReference dataORef, List<String> defaultSpheresForDynamicWriter,
			Task writerBeforeDecision) throws InterruptedException {
		// iterate through all tasks of the process and assign readers and writers
		// if task is a writer - add the dynamic writer sphere if necessary
		// task can only be either reader or writer to a specific dataObject
		// if amountWriters == 0 -> need to connect the writerBeforeDecision if it is
		// not null

		int i = 0;
		int j = 0;
		LinkedList<Task> allAvailableTasks = new LinkedList<Task>();
		allAvailableTasks.addAll(modelInstance.getModelElementsByType(Task.class));
		boolean writerTaskInFrontOfDecisionChosen = false;

		do {
			if (Thread.currentThread().isInterrupted()) {
				System.err.println("Interrupted! " + Thread.currentThread().getName());
				throw new InterruptedException();
			}
			// get a random flowNode and try making it a writer
			Task task = null;
			boolean inFrontOfDecision = false;
			if (writerTaskInFrontOfDecisionChosen == false && writerBeforeDecision != null) {
				task = writerBeforeDecision;
				inFrontOfDecision = true;
			} else {
				task = CommonFunctionality.getRandomItem(allAvailableTasks);
			}
			if (!taskIsBrtFollowedByXorSplit(task)) {
				// task will be a writer to the dataObject if it is not a brt followed by a xor
				// split or a reader/writer to the dataObject already
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
					// add sphere annotation for writer if necessary
					int randomCountSphere = ThreadLocalRandom.current().nextInt(1, 100 + 1);
					if (randomCountSphere <= dynamicWriterProb) {
						generateDIElementForSphereAnnotation(task, dataORef, defaultSpheresForDynamicWriter);
					}
					if (inFrontOfDecision == true) {
						// writer has been inserted in front of decision
						writerTaskInFrontOfDecisionChosen = true;
					}
				}
				allAvailableTasks.remove(task);
				i++;
			}

		} while (i < amountWriters);

		while (j < amountReaders) {
			// task will be a reader if it is not a reader or writer to the dataObject
			// already
			// brts followed by a xor-split will always be readers to some dataObjects
			if (Thread.currentThread().isInterrupted()) {
				System.err.println("Interrupted! " + Thread.currentThread().getName());
				throw new InterruptedException();
			}
			Task task = CommonFunctionality.getRandomItem(allAvailableTasks);

			if (!taskIsBrtFollowedByXorSplit(task)) {
				boolean toBeReader = true;
				for (DataOutputAssociation dao : task.getDataOutputAssociations()) {
					if (dao.getTarget().getId().contentEquals(dataORef.getId())) {
						toBeReader = false;
					}
				}
				for (DataInputAssociation dia : task.getDataInputAssociations()) {
					for (ItemAwareElement iae : dia.getSources()) {
						if (iae.getId().contentEquals(dataORef.getId())) {
							toBeReader = false;
						}
					}
				}

				if (toBeReader) {
					DataInputAssociation dia = modelInstance.newInstance(DataInputAssociation.class);
					Property p1 = modelInstance.newInstance(Property.class);
					p1.setName("__targetRef_placeholder");
					task.addChildElement(p1);
					dia.setTarget(p1);
					task.getDataInputAssociations().add(dia);
					dia.getSources().add(dataORef);

					generateDIElementForReader(dia, getShape(dataORef.getId()), getShape(task.getId()));
				}

				allAvailableTasks.remove(task);
				j++;
			}
		}

	}

	public void addExcludeParticipantConstraintsOnModel(int probabilityForGatewayToHaveConstraint,
			int lowerBoundAmountParticipantsToExcludePerGtw, int upperBoundAmountParticipantsToExcludePerGtw,
			boolean decisionTakerExcludeable, boolean alwaysMaxConstrained) throws Exception {
		// upperBoundAmountParticipantsToExclude is the difference between the amount of
		// needed voters and the global Sphere
		// e.g. global sphere = 5, 3 people needed -> 2 is the max amount of
		// participants to exclude
		if (probabilityForGatewayToHaveConstraint <= 0) {
			return;
		}

		if (lowerBoundAmountParticipantsToExcludePerGtw < 1) {
			lowerBoundAmountParticipantsToExcludePerGtw = 1;
		}

		for (ExclusiveGateway gtw : modelInstance.getModelElementsByType(ExclusiveGateway.class)) {
			if (gtw.getOutgoing().size() == 2
					&& gtw.getIncoming().iterator().next().getSource() instanceof BusinessRuleTask) {
				int randomInt = ThreadLocalRandom.current().nextInt(1, 101);
				if (probabilityForGatewayToHaveConstraint >= randomInt) {
					BusinessRuleTask brtBeforeGtw = (BusinessRuleTask) gtw.getIncoming().iterator().next().getSource();
					String decisionTakerName = brtBeforeGtw.getName();
					LinkedList<String> participantsToChooseFrom = CommonFunctionality.getGlobalSphereList(modelInstance,
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

												CommonFunctionality.generateShapeForTextAnnotation(modelInstance,
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
	}

	public void addMandatoryParticipantConstraintsOnModel(int probabilityForGatewayToHaveConstraint,
			int lowerBoundAmountParticipantsToBeMandatoryPerGtw, int upperBoundAmountParticipantsToBeMandatoryPerGtw,
			boolean decisionTakerMandatory, boolean alwaysMaxConstrained) throws Exception {
		// upperBoundAmountParticipantsToBeMandatory is the amount of
		// needed voters 
		// e.g. 3 people needed -> max 3 constraints for mandatory participants for the
		// gtw
		// if decisionTakerMandatory = true -> the participant of the brt will be
		// mandatory
		// else the mandatory participants will be chosen randomly from global sphere
		if (probabilityForGatewayToHaveConstraint <= 0) {
			return;
		}

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
					LinkedList<String> participantsToChooseFrom = CommonFunctionality.getGlobalSphereList(modelInstance,
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
											
											if (upperBoundAmountParticipantsToBeMandatoryPerGtw > amountVotersNeeded) {
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

										if (decisionTakerMandatory && randomAmountConstraintsForGtw > 0) {
											// first mandatory participant will be the decision taker
											String mandatoryParticipantConstraint = "[MandatoryParticipantConstraint] {"
													+ decisionTakerName + "}";
											TextAnnotation constraintAnnotation = modelInstance
													.newInstance(TextAnnotation.class);

											constraintAnnotation.setTextFormat("text/plain");
											Text text = modelInstance.newInstance(Text.class);
											text.setTextContent(mandatoryParticipantConstraint);
											constraintAnnotation.setText(text);
											gtw.getParentElement().addChildElement(constraintAnnotation);

											CommonFunctionality.generateShapeForTextAnnotation(modelInstance,
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
											participantsToChooseFrom.remove(decisionTakerName);
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

												CommonFunctionality.generateShapeForTextAnnotation(modelInstance,
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
	}

	public void addNamesForOutgoingFlowsOfXorSplits(LinkedList<String> namesForOutgoingSeqFlowsOfXorSplits) {
		for (ExclusiveGateway gtw : modelInstance.getModelElementsByType(ExclusiveGateway.class)) {
			if (gtw.getOutgoing().size() == 2 && gtw.getIncoming().size() == 1) {
				if (namesForOutgoingSeqFlowsOfXorSplits != null && namesForOutgoingSeqFlowsOfXorSplits.size() != 0) {
					LinkedList<String> names = new LinkedList<String>();
					names.addAll(namesForOutgoingSeqFlowsOfXorSplits);
					for (SequenceFlow seq : gtw.getOutgoing()) {
						String randomName = CommonFunctionality.getRandomItem(names);
						seq.setName(randomName);
						names.remove(randomName);
					}

				}

			}

		}

	}

	public BpmnModelInstance getModelInstance() {
		return this.modelInstance;
	}

	public String getFileNameForNewFile() {
		return this.fileNameForNewFile;
	}

	public String getDirectoryForNewFile() {
		return this.directoryForNewFile;
	}

	public boolean isDataObjectsConnectedToBrts() {
		return dataObjectsConnectedToBrts;
	}

	public void setDataObjectsConnectedToBrts(boolean dataObjectsConnectedToBrts) {
		this.dataObjectsConnectedToBrts = dataObjectsConnectedToBrts;
	}

	@Override
	public File call() throws Exception {
		// TODO Auto-generated method stub

		if (!Thread.currentThread().isInterrupted()) {
			try {
				if (methodsToRunWithinCall.isEmpty()) {
					System.out.println("No methods specifided to run within call!");
				}

				for (Entry<String, Object[]> methodEntry : this.methodsToRunWithinCall.entrySet()) {
					// methodEntry is in order of execution
					// key -> method name
					// value -> arguments
					String methodName = methodEntry.getKey();
					Object[] methodArguments = methodEntry.getValue();
					Class[] methodParameters = new Class[methodArguments.length];

					for (int i = 0; i < methodArguments.length; i++) {
						Object argument = methodArguments[i];
						if (argument instanceof Byte) {
							methodParameters[i] = Byte.TYPE;
						} else if (argument instanceof Short) {
							methodParameters[i] = Short.TYPE;
						} else if (argument instanceof Integer) {
							methodParameters[i] = Integer.TYPE;
						} else if (argument instanceof Long) {
							methodParameters[i] = Long.TYPE;
						} else if (argument instanceof Float) {
							methodParameters[i] = Float.TYPE;
						} else if (argument instanceof Double) {
							methodParameters[i] = Double.TYPE;
						} else if (argument instanceof Boolean) {
							methodParameters[i] = Boolean.TYPE;
						} else {
							methodParameters[i] = argument.getClass();
						}

					}

					try {
						Method method = this.getClass().getMethod(methodName, methodParameters);
						method.invoke(this, methodArguments);
					} catch (Exception ex) {
						ex.printStackTrace();
						throw ex;
					}

				}
				File f = this.checkCorrectnessAndWriteChangesToFile();
				return f;
			} catch (Exception ex) {
				throw ex;
			}
		}

		return null;

	}

}
