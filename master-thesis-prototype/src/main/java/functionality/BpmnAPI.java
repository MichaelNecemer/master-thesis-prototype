package functionality;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.Query;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.camunda.bpm.model.bpmn.instance.Lane;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.type.ModelElementType;

//Class that uses the camunda model API to interact with the process model directly without parsing the XML first to e.g. DOM Object
//Note that only processes with exactly 1 Start Event are possible
public class BpmnAPI {

	private Collection<StartEvent> startEvent;
	private File process;
	private BpmnModelInstance modelInstance;
	private Collection<Task> taskNodes;
	private Collection<Lane> globalSphere;
	private Collection<Lane> staticSphere;
	private Collection<FlowNode> flowNodes;
	private Collection<FlowNode> test = new ArrayList<FlowNode>();

	private Collection<Task> taskNodesWritingToDataObjects;
	private Collection<Task> taskNodesReadingFromDataObjects;
	public Collection<DataObject> dataObjects;

	BpmnAPI(String pathToFile) {
		process = new File(pathToFile);
		modelInstance = Bpmn.readModelFromFile(process);
		findElementByType();
	}

	public Collection<FlowNode> getAllFlowNodesAfterNode(FlowNode node) {
		Collection<FlowNode> followingFlowNodes = new ArrayList<FlowNode>();
		// Bei Binären Gateways werden 2 Nodes in Liste gespeichert!
		// 2 sequence flows
		for (SequenceFlow sequenceFlow : node.getOutgoing()) {
						
			followingFlowNodes.add(sequenceFlow.getTarget());
			test.add(sequenceFlow.getTarget());
			followingFlowNodes.forEach(n -> {
				if (n.getName() == null) {
					System.out.println(n.getId());
				} else {
					System.out.println(n.getName());
				}

			});
		}

		if (followingFlowNodes.iterator().hasNext()) {
			return getAllFlowNodesAfterNode(followingFlowNodes.iterator().next());
		} else
			return test;

	}

	public Collection<FlowNode> getAllFlowNodesAfterNode(Collection<FlowNode> nList) {
		Collection<FlowNode> followingFlowNodes = new ArrayList<FlowNode>();
		for (SequenceFlow sequenceFlow : nList.iterator().next().getOutgoing()) {
			followingFlowNodes.add(sequenceFlow.getTarget());
			followingFlowNodes.forEach(n -> {
				if (n.getName() == null) {
					System.out.println(n.getId());
				} else {
					System.out.println(n.getName());
				}

			});
		}

		return getAllFlowNodesAfterNode(followingFlowNodes);
	}

	private Collection<FlowNode> getFlowingFlowNodes(FlowNode node) {
		Collection<FlowNode> followingFlowNodes = new ArrayList<FlowNode>();
		for (SequenceFlow sequenceFlow : node.getOutgoing()) {
			followingFlowNodes.add(sequenceFlow.getTarget());
		}
		return followingFlowNodes;
	}

	public void printGlobalSphere() {
		System.out.println("Global Sphere of the process contains: ");
		for (ModelElementInstance g : globalSphere) {
			System.out.println(g.getAttributeValue("name").trim());
		}
	}

	public Collection<Lane> computeStaticSphere() {
		// A participant is in the static sphere of some Data Object if he interacts
		// (reads or writes) with it at some point in the process

		// Static Sphere has to be computed for every Data Object in the process

		Collection<Lane> staticSphere = new HashSet<Lane>();

		for (Task t : this.taskNodesReadingFromDataObjects) {
			this.getDataObjectsForNode(t);
			this.getLaneForNode(t);
		}

		return staticSphere;
	}

	public void findElementByType() {
		// All stored within collections!!!
		startEvent = modelInstance.getModelElementsByType(StartEvent.class);
		taskNodes = modelInstance.getModelElementsByType(Task.class);
		flowNodes = modelInstance.getModelElementsByType(FlowNode.class);
		globalSphere = modelInstance.getModelElementsByType(Lane.class);
		dataObjects = modelInstance.getModelElementsByType(DataObject.class);

		// Use the helper methods to get the Tasks that interact with Data Objects (read
		// and write data)

		taskNodesWritingToDataObjects = this.computeTasksWithDataOutputAssociation();
		taskNodesReadingFromDataObjects = this.computeTasksWithDataInputAssociation();

	}

	public Collection<StartEvent> getStartEvent() {
		return this.startEvent;
	}

	public Collection<FlowNode> getAllFlowNodes() {
		return this.flowNodes;
	}

	// Prints out the Data Objects connected to a specific task (the task either
	// reads or writes to the data objects)
	public void printCorrespondingDataObject(Task t) {
		t.getDataOutputAssociations().forEach(f -> {
			System.out.println(t.getName() + " -> " + f.getTarget().getAttributeValue("name"));
		});
		t.getDataInputAssociations().forEach(g -> {
			System.out.println(g.getSources().iterator().next().getAttributeValue("name") + " -> " + t.getName());
		});
	}

	// Helper method to compute a Collection of Tasks that Write to data objects
	private Collection<Task> computeTasksWithDataOutputAssociation() {
		Collection<Task> writingTasks = new ArrayList<Task>();

		for (Task t : this.taskNodes) {
			if (!(t.getDataOutputAssociations().isEmpty())) {
				writingTasks.add(t);
			}

		}
		return writingTasks;
	}

	// Helper method to compute a Collection of Tasks that Read data from data
	// objects
	private Collection<Task> computeTasksWithDataInputAssociation() {
		Collection<Task> readingTasks = new ArrayList<Task>();
		for (Task t : this.taskNodes) {
			if (!(t.getDataInputAssociations().isEmpty())) {
				readingTasks.add(t);
			}

		}
		return readingTasks;
	}

	// ???
	private Collection<Task> computeTasksWithDataInputAssociationForDataObject(DataObject d) {
		Collection<Task> readingTasks = new ArrayList<Task>();
		for (Task t : this.taskNodes) {
			if (!(t.getDataInputAssociations().isEmpty())) {
				for (DataInputAssociation data : t.getDataInputAssociations()) {
					if (data.getTarget().getAttributeValue("dataObjectRef").equals(d.getId())) {
						readingTasks.add(t);
					}
				}

			}

		}
		return readingTasks;
	}

	// Helper method to get the Lane a certain node corresponds to
	public Lane getLaneForNode(FlowNode n) {
		for (Lane l : globalSphere) {
			for (FlowNode flowNode : l.getFlowNodeRefs()) {
				if (n.getId().equals(flowNode.getId())) {
					return l;
				}
			}
		}
		return null;
	}

	// Helper method to get all Nodes connected to a Data Object
	/*
	 * public Collection<FlowNode> getNodesForDataObjecs(DataObject d){
	 * Collection<DataObjectReference> ref =
	 * modelInstance.getModelElementsByType(DataObjectReference.class); }
	 */

	// Get all Data Objects the node (in this implementation a task) writes to
	public Collection<DataObject> getDataObjectsTheTaskWritesTo(FlowNode n) {
		Collection<DataObject> dataObjects = new ArrayList<DataObject>();

		// Currently DataObjects can only be attached to Tasks!
		if (n instanceof Task) {
			Task t = (Task) n;
			Collection<DataOutputAssociation> output = t.getDataOutputAssociations();

			if (!(output.isEmpty())) {
				for (DataOutputAssociation d : output) {
					for (DataObject data : this.dataObjects) {
						if (d.getTarget().getAttributeValue("dataObjectRef").equals(data.getId())) {
							dataObjects.add(data);

						}
					}

				}

			}
		}
		return dataObjects;
	}

	// Helper method to get the Data Object(s) connected to a Node
	public Collection<DataObject> getDataObjectsForNode(FlowNode n) {
		Collection<DataObject> dataObjects = new ArrayList<DataObject>();

		// Currently DataObjects can only be attached to Tasks!
		if (n instanceof Task) {
			Task t = (Task) n;
			Collection<DataOutputAssociation> output = t.getDataOutputAssociations();
			Collection<DataInputAssociation> input = t.getDataInputAssociations();

			if (!(output.isEmpty())) {
				for (DataOutputAssociation d : output) {
					for (DataObject data : this.dataObjects) {
						if (d.getTarget().getAttributeValue("dataObjectRef").equals(data.getId())) {
							dataObjects.add(data);
						}
					}

				}

			}

			if (!(input.isEmpty())) {
				for (DataInputAssociation di : input) {
					for (DataObject data : this.dataObjects) {
						// Wenn mehrere DataInputAssociations vorhanden sind muss hier noch angepasst
						// werden!!!
						if (di.getSources().iterator().next().getAttributeValue("dataObjectRef").equals(data.getId())) {
							dataObjects.add(data);
						}
					}

				}

			}

		}
		return dataObjects;

	}

	public FlowNode getNodeById(String id) {
		for (FlowNode n : this.flowNodes) {
			if (n.getId().equals(id)) {
				return n;
			}
		}
		return null;
	}

	public void printAllElements() {
		flowNodes.forEach(s -> {
			System.out.println(s.getId());
		});
	}

	public void printAllFlowNodes() {
		flowNodes.forEach(f -> {
			System.out.println(f.getAttributeValue("id"));
		});
	}

	public void getAllFollowers(FlowNode node) {
		node.getSucceedingNodes().list().forEach(s -> {
			System.out.println(s.getName());
		});
	}

	public void printFlowingFlowNodes(FlowNode node) {
		getFlowingFlowNodes(node).forEach(s -> {
			System.out.println(s.getId());
		});
	}

	public void printAllFlowingFlowNodesAfterNode(FlowNode node) {
		getAllFlowNodesAfterNode(node).forEach(n -> {
			System.out.println(n.getId());
		});
	}

	public void printTasks() {
		for (ModelElementInstance t : taskNodes) {
			System.out.println(t.getAttributeValue("id") + " " + t.getAttributeValue("name"));

		}
	}

	public void getNameOfDataObject(DataObject d) {
		// The name is not stored inside the DataObject Class itself but in the
		// DataObjectReference Class
		Collection<DataObjectReference> dor = modelInstance.getModelElementsByType(DataObjectReference.class);
		for (DataObjectReference ref : dor) {
			if (ref.getAttributeValue("dataObjectRef").equals(d.getId())) {
				System.out.println(ref.getName());
			}
		}

	}


	// To be done
	public Collection<FlowNode> getAllNodesConnectedToDataObject(DataObject d) {
		Collection<FlowNode> retNodes = new ArrayList<FlowNode>();
		Collection<DataObjectReference> dor = modelInstance.getModelElementsByType(DataObjectReference.class);
		// Collection<DataInputAssociation> input =
		// modelInstance.getModelElementsByType(DataInputAssociation.class);
		for (Task t : this.taskNodes) {
			for (DataOutputAssociation outp : t.getDataOutputAssociations()) {
				for (DataObjectReference ref : dor) {
				}
			}

		}
		return retNodes;
	}

}
