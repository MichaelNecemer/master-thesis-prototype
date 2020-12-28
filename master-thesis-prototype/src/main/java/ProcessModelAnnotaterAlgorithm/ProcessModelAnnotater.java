package ProcessModelAnnotaterAlgorithm;

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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.ItemAwareElement;
import org.camunda.bpm.model.bpmn.instance.Lane;
import org.camunda.bpm.model.bpmn.instance.LaneSet;
import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.Property;
import org.camunda.bpm.model.bpmn.instance.SendTask;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.camunda.bpm.model.bpmn.instance.Text;
import org.camunda.bpm.model.bpmn.instance.TextAnnotation;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnLabel;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnPlane;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.camunda.bpm.model.bpmn.instance.dc.Bounds;
import org.camunda.bpm.model.bpmn.instance.di.Plane;
import org.camunda.bpm.model.bpmn.instance.di.Waypoint;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProcessModelAnnotater {
	// class takes a process model and annotates it with dataObjects, readers,
	// writers, etc.
	private static BpmnModelInstance modelInstance;
	private static Collection<FlowNode> flowNodes;
	
	public static void annotateModel(String pathToFile, List<Integer> countDataObjects, List<String>defaultSpheres, int readerProbRange,
			int writerProbRange) {
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
			
			
			//add the default sphere to the xml file
			//randomly choose one of the given ones
			TextAnnotation defaultSphere = modelInstance.newInstance(TextAnnotation.class);
			int randomSphereCount = ThreadLocalRandom.current().nextInt(0, defaultSpheres.size());
			String textContent = "Default: [D" + (i + 1) + "]{"+defaultSpheres.get(randomSphereCount)+"}";
	
			defaultSphere.setId("TextAnnotation_defaultSphereForD"+(i+1));
			defaultSphere.setTextFormat("text/plain");
			Text text = modelInstance.newInstance(Text.class);
			text.setTextContent(textContent);
			defaultSphere.setText(text);
			currDataObject.getParentElement().addChildElement(defaultSphere);

			// add the shape of the dataObject to the xml file
			generateDIElementsForDataObject(dataORef, someNode);

			
			//add the shape of the text annotation to the xml file
			generateShapeForTextAnnotation(defaultSphere, dataORef);
			
			// iterate through all tasks of the process and assign readers and writers
			for (FlowNode node : flowNodes) {
				if (node instanceof Task) {
					Task task = (Task) node;				
					
					if (taskIsReaderOrWriter(writerProbRange)) {
						DataOutputAssociation dao = modelInstance.newInstance(DataOutputAssociation.class);
						dao.setTarget(dataORef);
						task.addChildElement(dao);

						generateDIElementForWriter(dao, getShape(dataORef.getId()), getShape(task.getId()));
					}
					if (taskIsReaderOrWriter(readerProbRange)) {
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
		
		//check again if a brt followed by a xor-split has at least one data object connected!!!
		//insert tuples for xor-gateways if not already done
		
		
		
		
		
		try {
			writeChangesToFile();
		} catch (IOException | ParserConfigurationException | SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void generateShapeForTextAnnotation(TextAnnotation txt, DataObjectReference ref) {
		BpmnShape shapeForAnnotation = modelInstance.newInstance(BpmnShape.class);
		shapeForAnnotation.setBpmnElement(txt);
		Bounds bounds = modelInstance.newInstance(Bounds.class);
		bounds.setX(getShape(ref.getId()).getBounds().getX()-200);
		bounds.setY(getShape(ref.getId()).getBounds().getY()-40);
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

}
