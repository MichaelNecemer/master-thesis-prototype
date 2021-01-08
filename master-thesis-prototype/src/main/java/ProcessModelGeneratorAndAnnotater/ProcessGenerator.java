package ProcessModelGeneratorAndAnnotater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Random;
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
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ProcessGenerator {
	//generates a new Process Model using the camunda fluent API
	//model can than be annotated using the ProcessModelAnnotater
	
	private static BpmnModelInstance modelInstance;
	private int maxAmountTasks;
	private int maxAmountXorGtws;
	private int maxAmountParallelGtws;
	private int probTask;
	private int probXorGtw;
	private int probParallelGtw;
	private int nestingDepthFactor;
	private LinkedList<FlowNode>queue;
	private LinkedList<FlowNode>entryPoints;
	
	private static int taskId;
	private static int xorGtwId;
	private static int parallelGtwId;
	private int[] taskProbArray;
	private int[] xorProbArray;
	private int[] parallelProbArray;
	private FlowNode lastNode = null;
	
	
	public ProcessGenerator(int maxAmountTasks, int maxAmountXorGtws,int maxAmountParallelGtws, int probTask, int probXorGtw, int probParallelGtw, int nestingDepthFactor) {
		//process model will have 1 StartEvent and 1 EndEvent
		
		this.maxAmountTasks=maxAmountTasks;
		this.maxAmountXorGtws=maxAmountXorGtws;
		this.maxAmountParallelGtws=maxAmountParallelGtws;
		this.probTask=probTask;
		this.probXorGtw=probXorGtw;
		this.probParallelGtw=probParallelGtw;
		this.nestingDepthFactor=nestingDepthFactor;
		this.entryPoints=new LinkedList<FlowNode>();
		
		taskId = 1;
		xorGtwId = 1;
		parallelGtwId = 1;
		
		taskProbArray = new int[2];
		taskProbArray[0]=0;
		taskProbArray[1]=probTask-1;
		
		xorProbArray = new int[2];
		xorProbArray[0]=probTask;
		xorProbArray[1]=(probTask+probXorGtw-1);
		
		parallelProbArray = new int[2];
		parallelProbArray[0] = (probTask+probXorGtw);
		parallelProbArray[1] = (probTask+probXorGtw+probParallelGtw-1);
		
		modelInstance = Bpmn.createProcess().startEvent("startEvent_1").manualTask().done();
		FlowNode startEvent = (FlowNode)modelInstance.getModelElementById("startEvent_1");
		entryPoints.add(startEvent);
		
		/*
		while(this.getMaxAmountTasks()>0) {
			//consider nesting depth factor to jump out of xor-branches
			
			//chose a node to add a random flowNode after it
			 Random rand = new Random();
			 FlowNode currNodeToAddNextOne = entryPoints.get(rand.nextInt(entryPoints.size()));
			if(currNodeToAddNextOne!=null) {
				lastNode = this.addNextFlowNode(currNodeToAddNextOne);
				entryPoints.add(lastNode);
				entryPoints.remove(currNodeToAddNextOne);
				
			} 
		}
		
		lastNode.builder().endEvent("endEvent_1").done();
		*/
		
	
	try {
		writeChangesToFile();
	} catch (IOException | ParserConfigurationException | SAXException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	}	
	
	private FlowNode addNextFlowNode(FlowNode currentNode) {
		//randomly choose next FlowNode to be inserted into process
		int randomInt = this.getRandomInt(0, 100);
		FlowNode flowNodeAdded = null;
		//if(randomInt>=taskProbArray[0]&&randomInt<=taskProbArray[1]) {
			//create new Task
			String uniqueTaskId = "manualTask_"+taskId++;
			currentNode.builder().manualTask(uniqueTaskId);
			this.setMaxAmountTasks(--this.maxAmountTasks);
			flowNodeAdded = modelInstance.getModelElementById(uniqueTaskId);
			
		/*} else if(randomInt>=xorProbArray[0]&&randomInt<=xorProbArray[1]) {
			if(maxAmountXorGtws!=0) {
			//create new xor-split
			String uniqueXorGtwId = "exclusiveGateway_"+xorGtwId++;
			currentNode.builder().exclusiveGateway(uniqueXorGtwId);
			this.setMaxAmountXorGtws(--maxAmountXorGtws);
			flowNodeAdded = modelInstance.getModelElementById(uniqueXorGtwId);
			}
			
		} else if(randomInt>=parallelProbArray[0]&&randomInt<=parallelProbArray[1]) {
			//create new parallel-split
			if(maxAmountParallelGtws!=0) {
			String uniqueParallelGtwId = "parallelGateway_"+parallelGtwId++;
			currentNode.builder().parallelGateway(uniqueParallelGtwId);
			this.setMaxAmountParallelGtws(--maxAmountParallelGtws);
			flowNodeAdded = modelInstance.getModelElementById(uniqueParallelGtwId);
			}
		}*/
		
		return flowNodeAdded;
		
	}
	
	private int getRandomInt(int min, int max) {
		int randomInt = ThreadLocalRandom.current().nextInt(min,max+1);
		return randomInt;
	}
	
	private int getMaxAmountTasks() {
		return this.maxAmountTasks;
	}
	
	private void setMaxAmountTasks(int amountTasks) {
		this.maxAmountTasks=amountTasks;
	}
	
	private void setMaxAmountParallelGtws(int amountParallelGtws) {
		this.maxAmountParallelGtws=amountParallelGtws;
	}
	
	private void setMaxAmountXorGtws(int amountXorGtws) {
		this.maxAmountXorGtws=amountXorGtws;
	}
	
	private static void writeChangesToFile() throws IOException, ParserConfigurationException, SAXException {
		// validate and write model to file
		Bpmn.validateModel(modelInstance);
		File file = File.createTempFile("processModel", ".bpmn",
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
		

		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
