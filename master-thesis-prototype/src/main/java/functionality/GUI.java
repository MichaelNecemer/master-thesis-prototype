package functionality;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.NumberFormatter;

import functionality.Enums.AlgorithmToPerform;
import mapping.BPMNElement;
import mapping.BPMNExclusiveGateway;

public class GUI {
	// small GUI where users can import a camunda bpmn model and generate the
	// optimal solutions
	static API api = null;
	static BPMNExclusiveGateway bpmnEx = null;
	static int sumVotes = 0;
	static JCheckBox compareResultsAgainstExhaustiveSearch;
	static JCheckBox jrbExhaustiveSearch;
	static JCheckBox jrbLocalMinimum;
	static JLabel openingModelsLabel;
	static JLabel saveModelsLabel;
	static JLabel boundLabel;
	static JFormattedTextField jFormattedTextFieldCost;
	static JFormattedTextField jFormattedTextField1;
	static JFormattedTextField jFormattedTextField2;
	static JFormattedTextField jFormattedTextField3;
	static JFormattedTextField jFormattedTextField4;
	static Dimension dimension = new Dimension(Integer.MAX_VALUE, 20);
	static JButton runButton;
	static JCheckBox votingAsConstructBox;
	static JCheckBox subProcessBox;
	static JCheckBox mapModelBox;
	static JCheckBox votingAsAnnotationBox;
	static JCheckBox boundBox;
	static JButton saveButton;
	static Dimension verticalSpacingBetweenComponents = new Dimension(0, 10);
	static JMenuBar mb = new JMenuBar();
	static JMenuItem openFileItem = new JMenuItem("Open .bpmn file");
	static JMenuItem saveFileItem = new JMenuItem("Save files in directory");
	static JMenuItem editDefaultSettingsItem = new JMenuItem("Settings");
	static JMenu menu = new JMenu("Menu");
	static JFrame frame;
	static JFileChooser openFileChooser;
	static JFileChooser saveFileChooser;
	static String pathToInputFile = "";
	static FileNameExtensionFilter filter = new FileNameExtensionFilter("bpmn files", "bpmn");
	static JPanel leftPanel = new JPanel();
	static JPanel rightPanel = new JPanel();
	static String defaultDirectoryForOpeningModels;
	static String defaultDirectoryForStoringModels;
	static File configFile = new File("src/main/resources/configGUI.properties");
	static String defaultCostMeasureParameters = "1,1,1";
	static Properties props = new Properties();
	static FileReader reader;
	static LinkedList<Double> costMeasureWeightingParameters;

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		menu.add(openFileItem);
		menu.add(editDefaultSettingsItem);
		mb.add(menu);
		frame = new JFrame("GUI");
		frame.setLayout(new GridLayout(1, 0, 40, 30));
		frame.setVisible(true);

		frame.setJMenuBar(mb);
		frame.pack();

		if(!configFile.exists()) {
			configFile.createNewFile();
		}		
	
		try {
			reader = new FileReader(configFile);
			props.load(reader);
			reader.close();
		
			boolean storeConfigFile = false;
			
			if(!props.containsKey("costMeasureWeightingParameters")) {
				props.setProperty("costMeasureWeightingParameters", defaultCostMeasureParameters);
				storeConfigFile = true;
			}
			if(!props.containsKey("defaultDirectoryForOpeningModels")) {
				props.setProperty("defaultDirectoryForOpeningModels", "");
				storeConfigFile = true;
			}
			if(!props.containsKey("defaultDirectoryForStoringModels")) {
				props.setProperty("defaultDirectoryForStoringModels", "");
				storeConfigFile = true;
			}
			
			
			if(storeConfigFile) {
				props.store(new FileOutputStream(configFile), null);
				frame.pack();
			}

		} catch (FileNotFoundException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}

		editDefaultSettingsItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				// read the config file with default values in src/main/resources
				// to change the default values - edit the file directly or open the config in
				// GUI application
				try {
					frame.getContentPane().remove(leftPanel);
					frame.getContentPane().remove(rightPanel);

					leftPanel = new JPanel();
					leftPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
					leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.PAGE_AXIS));

					// display the default settings
					leftPanel.add(new JLabel("Cost for upgrading spheres: "));
					jFormattedTextFieldCost = new JFormattedTextField();
					String costMeasureWeightingParametersAsString = props.getProperty("costMeasureWeightingParameters");
					boolean askUserForValues = false;
					if (!costMeasureWeightingParametersAsString.isEmpty()) {
						String[] spheresArr = costMeasureWeightingParametersAsString.split(",");
						LinkedList<Double> spheresAsDouble = new LinkedList<Double>();

						for (String str : spheresArr) {
							spheresAsDouble.add(Double.parseDouble(str));
						}
						if (spheresAsDouble.size() != 3) {
							askUserForValues = true;
						} else {
							costMeasureWeightingParameters = spheresAsDouble;
						}

					}

					if (askUserForValues) {
						// ask user for default values
						ArrayList<Double> cost;
						String defaultCost;
						do {
							cost = new ArrayList<Double>();
							defaultCost = JOptionPane.showInputDialog(
									"Add 3 comma separated numbers as parameters for the cost measure (Alpha, Beta, Gamma)!");
							defaultCost.trim();
							String[] defaultCostArr = defaultCost.split(",");
							for (String str : defaultCostArr) {
								cost.add((Double.parseDouble(str)));
							}
						} while (cost.size() != 3);

						if (cost.size() == 3) {
							props.setProperty("costMeasureWeightingParameters", defaultCost);
							costMeasureWeightingParametersAsString = defaultCost;
						}

						try {
							props.store(new FileOutputStream(configFile), null);
							reader = new FileReader(configFile);
							props.load(reader);
							reader.close();
						} catch (FileNotFoundException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					jFormattedTextFieldCost.setText(costMeasureWeightingParametersAsString);
					leftPanel.add(jFormattedTextFieldCost);
					leftPanel.add(Box.createRigidArea(verticalSpacingBetweenComponents));

					defaultDirectoryForOpeningModels = props.getProperty("defaultDirectoryForOpeningModels");
					openingModelsLabel = new JLabel(
							"Default directory for opening models: " + defaultDirectoryForOpeningModels);
					leftPanel.add(openingModelsLabel);
					File defaultDirectoryForOpeningModelsFile = new File(defaultDirectoryForOpeningModels);
					JButton searchFolderButton = new JButton("Search folder...");
					searchFolderButton.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent arg0) {
							// TODO Auto-generated method stub
							openFileChooser = new JFileChooser();
							// TODO Auto-generated method stub
							openFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
							if (defaultDirectoryForOpeningModelsFile != null) {
								openFileChooser.setCurrentDirectory(defaultDirectoryForOpeningModelsFile);
							}

							if (openFileChooser.showSaveDialog(openFileChooser) == JFileChooser.APPROVE_OPTION) {
								defaultDirectoryForOpeningModels = openFileChooser.getSelectedFile().getAbsolutePath();
								openingModelsLabel.setText(
										"Default directory for opening models: " + defaultDirectoryForOpeningModels);
								frame.pack();
							}
						}

					});
					leftPanel.add(searchFolderButton);
					leftPanel.add(Box.createRigidArea(verticalSpacingBetweenComponents));

					defaultDirectoryForStoringModels = props.getProperty("defaultDirectoryForStoringModels");
					saveModelsLabel = new JLabel(
							"Default directory for storing models: " + defaultDirectoryForStoringModels.trim());
					leftPanel.add(saveModelsLabel);
					File defaultDirectoryForStoringModelsFile = new File(defaultDirectoryForStoringModels);
					JButton searchFolderButton2 = new JButton("Search folder...");
					leftPanel.add(searchFolderButton2);
					searchFolderButton2.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent arg0) {
							// TODO Auto-generated method stub
							saveFileChooser = new JFileChooser();
							// TODO Auto-generated method stub
							saveFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
							if (defaultDirectoryForStoringModelsFile != null) {
								saveFileChooser.setCurrentDirectory(defaultDirectoryForStoringModelsFile);
							}

							if (saveFileChooser.showSaveDialog(saveFileChooser) == JFileChooser.APPROVE_OPTION) {
								defaultDirectoryForStoringModels = saveFileChooser.getSelectedFile().getAbsolutePath();
								saveModelsLabel.setText(
										"Default directory for saving models: " + defaultDirectoryForStoringModels);
								frame.pack();
							}
						}

					});
					leftPanel.add(Box.createRigidArea(verticalSpacingBetweenComponents));

					JButton saveSettingsButton = new JButton("SAVE SETTINGS");
					saveSettingsButton.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							// TODO Auto-generated method stub
							props.setProperty("costMeasureWeightingParameters", jFormattedTextFieldCost.getText());
							props.setProperty("defaultDirectoryForOpeningModels", defaultDirectoryForOpeningModels);
							props.setProperty("defaultDirectoryForStoringModels", defaultDirectoryForStoringModels);
							try {
								props.store(new FileOutputStream(configFile), null);
								reader = new FileReader(configFile);
								props.load(reader);
								reader.close();
								openingModelsLabel.setText("Default directory for opening models: "
										+ props.getProperty("defaultDirectoryForOpeningModels"));
								saveModelsLabel.setText("Default directory for storing models: "
										+ props.getProperty("defaultDirectoryForStoringModels").trim());
								String costForUpgradeString = props.getProperty("costMeasureWeightingParameters");
								String[] costForUpgradeArr = costForUpgradeString.split(",");
								LinkedList<Double> costForUpgrade = new LinkedList<Double>();
								for (String str : costForUpgradeArr) {
									costForUpgrade.add(Double.parseDouble(str));
								}
								costMeasureWeightingParameters = costForUpgrade;
								defaultDirectoryForOpeningModels = props
										.getProperty("defaultDirectoryForOpeningModels");
								defaultDirectoryForStoringModels = props
										.getProperty("defaultDirectoryForStoringModels");
								frame.pack();
							} catch (FileNotFoundException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}

					});
					leftPanel.add(saveSettingsButton);
					frame.add(leftPanel);
					frame.pack();

					reader.close();
				} catch (FileNotFoundException ex) {
					// file does not exist
				} catch (IOException ex) {
					// I/O error

				}

			}

		});

		openFileItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub

				openFileChooser = new JFileChooser();
				File defaultDirectoryForOpeningModelsFile = null;
				boolean insertDefaultValues = false;

				try {
					defaultDirectoryForOpeningModelsFile = new File(
							props.getProperty("defaultDirectoryForOpeningModels"));
					if (!defaultDirectoryForOpeningModelsFile.exists()) {
						insertDefaultValues = true;
					}

					String cost = props.getProperty("costMeasureWeightingParameters");
					boolean askUserForValues = false;
					LinkedList<Double> costList = new LinkedList<Double>();
					cost.trim();
					String[] defaultCostArr = cost.split(",");
					for (String str : defaultCostArr) {
						costList.add((Double.parseDouble(str)));
					}
					if (cost.isEmpty() || defaultCostArr.length != 3) {
						askUserForValues = true;
					}
					if (askUserForValues) {

						while (costList.size() != 3) {
							cost = JOptionPane.showInputDialog(
									"Add 3 comma separated numbers as parameters for the cost measure (Alpha, Beta, Gamma)!");

							costList = new LinkedList<Double>();
							cost.trim();
							defaultCostArr = cost.split(",");
							for (String str : defaultCostArr) {
								costList.add((Double.parseDouble(str)));
							}

						}
					}
					if (costList.size() == 3) {
						props.setProperty("costMeasureWeightingParameters", cost);
						costMeasureWeightingParameters = costList;
					}
				} catch (Exception ex) {
					leftPanel.add(new JLabel("Default directory for file can not be null"));
				}

				if (defaultDirectoryForOpeningModelsFile != null) {
					openFileChooser.setCurrentDirectory(defaultDirectoryForOpeningModelsFile);

				}
				openFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				openFileChooser.setFileFilter(filter);
				int check = openFileChooser.showOpenDialog(null);
				if (check == JFileChooser.APPROVE_OPTION) {
					pathToInputFile = openFileChooser.getSelectedFile().getAbsolutePath();
					String fileName = openFileChooser.getSelectedFile().getName();
					frame.setTitle("Model loaded: " + fileName);
					if (insertDefaultValues) {
						// set default properties
						String directoryOfInputFile = openFileChooser.getCurrentDirectory().getAbsolutePath();
						props.setProperty("defaultDirectoryForOpeningModels", directoryOfInputFile);
						props.setProperty("defaultDirectoryForStoringModels", directoryOfInputFile);
					}

					try {
						props.store(new FileOutputStream(configFile), null);
						reader = new FileReader(configFile);
						props.load(reader);
						reader.close();
					} catch (FileNotFoundException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					frame.pack();
					if (!pathToInputFile.isEmpty()) {
						frame.getContentPane().remove(leftPanel);
						leftPanel = new JPanel();
						leftPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
						leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.PAGE_AXIS));

						try {
							api = new API(pathToInputFile, costMeasureWeightingParameters);
							leftPanel.add(new JLabel("Troubleshooter: " + api.getTroubleShooter().getName()));
							leftPanel.add(
									new JLabel("Paths through process: " + api.getAllPathsThroughProcess().size()));
							int sumConstraints = 0;
							for (BPMNElement element : api.getProcessElements()) {
								if (element instanceof BPMNExclusiveGateway) {
									sumConstraints += ((BPMNExclusiveGateway) element).getConstraints().size();
								}
							}
							leftPanel.add(new JLabel("Amount of constraints: " + sumConstraints));
							leftPanel.add(new JLabel("Private sphere size: " + api.getPrivateSphere().size()));
							leftPanel.add(Box.createRigidArea(verticalSpacingBetweenComponents));

							// create buttons for choosing the algorithm to perform
							leftPanel.add(new JLabel("Algorithms: "));
							compareResultsAgainstExhaustiveSearch = new JCheckBox("compare results against Exhaustive Search");
							compareResultsAgainstExhaustiveSearch.setEnabled(false);
							jrbExhaustiveSearch = new JCheckBox("Exhaustive Search", true);
							jrbExhaustiveSearch.addItemListener(new ItemListener() {

								@Override
								public void itemStateChanged(ItemEvent e) {
									// TODO Auto-generated method stub
									if (e.getStateChange() == ItemEvent.SELECTED) {
										compareResultsAgainstExhaustiveSearch.setSelected(false);
										compareResultsAgainstExhaustiveSearch.setEnabled(false);
									}

								}

							});

							jrbLocalMinimum = new JCheckBox("Heuristic search", false);

							jrbLocalMinimum.addItemListener(new ItemListener() {

								@Override
								public void itemStateChanged(ItemEvent e) {
									// TODO Auto-generated method stub
									if (e.getStateChange() == ItemEvent.SELECTED) {
										compareResultsAgainstExhaustiveSearch.setEnabled(true);
										compareResultsAgainstExhaustiveSearch.setSelected(true);
										boundLabel.setVisible(true);
										jFormattedTextField1.setVisible(true);
										jFormattedTextField1.setEnabled(true);
										frame.pack();
									} else if (e.getStateChange() == ItemEvent.DESELECTED) {
										compareResultsAgainstExhaustiveSearch.setSelected(false);
										boundLabel.setVisible(false);
										jFormattedTextField1.setEnabled(false);
										jFormattedTextField1.setVisible(false);
										frame.pack();
									}

								}

							});


							NumberFormat format = NumberFormat.getIntegerInstance();
							format.setGroupingUsed(false);

							// minimum 1 is allowed for the field
							NumberFormatter numberFormatter1 = new NumberFormatter(format);
							numberFormatter1.setValueClass(Integer.class);
							numberFormatter1.setAllowsInvalid(false);
							numberFormatter1.setMinimum(1);
							numberFormatter1.setCommitsOnValidEdit(true);

							NumberFormat format2 = NumberFormat.getIntegerInstance();
							format.setGroupingUsed(false);

							// minimum 0 is allowed for the field
							NumberFormatter numberFormatter2 = new NumberFormatter(format2);
							numberFormatter2.setValueClass(Integer.class);
							numberFormatter2.setAllowsInvalid(false);
							numberFormatter2.setMinimum(0);
							numberFormatter2.setCommitsOnValidEdit(true);

							jFormattedTextField1 = new JFormattedTextField(numberFormatter1);
							jFormattedTextField1.setMaximumSize(dimension);
							jFormattedTextField1.setEnabled(false);

							compareResultsAgainstExhaustiveSearch.addItemListener(new ItemListener() {

								@Override
								public void itemStateChanged(ItemEvent e) {
									// TODO Auto-generated method stub
									if (e.getStateChange() == ItemEvent.SELECTED) {
										if (jrbExhaustiveSearch.isSelected()) {
											compareResultsAgainstExhaustiveSearch.setSelected(false);
										}
									}

								}

							});

							ButtonGroup jCheckBoxGroup = new ButtonGroup();
							jCheckBoxGroup.add(jrbExhaustiveSearch);
							jCheckBoxGroup.add(jrbLocalMinimum);

							leftPanel.add(jrbExhaustiveSearch);
							leftPanel.add(jrbLocalMinimum);
							boundLabel = new JLabel("Enter bound (0... unbounded): ");
							leftPanel.add(boundLabel);
							boundLabel.setVisible(false);

							leftPanel.add(jFormattedTextField1);
							jFormattedTextField1.setVisible(false);

							frame.add(leftPanel);

							frame.getContentPane().remove(rightPanel);
							rightPanel = new JPanel();
							rightPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
							rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.PAGE_AXIS));
							rightPanel.add(new JLabel("Enter max amount new models to be stored (0... all models):"));

							jFormattedTextField2 = new JFormattedTextField(numberFormatter2);
							jFormattedTextField2.setText("0");
							jFormattedTextField2.setMaximumSize(dimension);
							rightPanel.add(jFormattedTextField2);

							votingAsConstructBox = new JCheckBox("VotingAsConstruct", false);
							votingAsConstructBox.addActionListener(new ActionListener() {

								@Override
								public void actionPerformed(ActionEvent e) {
									// TODO Auto-generated method stub
									subProcessBox.setSelected(false);
									subProcessBox.setEnabled(true);
									mapModelBox.setSelected(false);
									mapModelBox.setEnabled(true);
								}

							});
							votingAsAnnotationBox = new JCheckBox("VotingAsAnnotation", true);
							votingAsAnnotationBox.addActionListener(new ActionListener() {

								@Override
								public void actionPerformed(ActionEvent arg0) {
									// TODO Auto-generated method stub
									if (votingAsAnnotationBox.isSelected()) {
										subProcessBox.setSelected(false);
										subProcessBox.setEnabled(false);
										mapModelBox.setSelected(false);
										mapModelBox.setEnabled(false);
									}
								}

							});
							subProcessBox = new JCheckBox("VotingAsSubProcess", false);
							subProcessBox.setBorder(new EmptyBorder(0, 20, 0, 0));
							mapModelBox = new JCheckBox("MapModel", false);
							rightPanel.add(Box.createRigidArea(verticalSpacingBetweenComponents));
							mapModelBox.setBorder(new EmptyBorder(0, 20, 0, 0));

							ButtonGroup jCheckBoxGroupSettings = new ButtonGroup();
							jCheckBoxGroupSettings.add(votingAsAnnotationBox);
							jCheckBoxGroupSettings.add(votingAsConstructBox);

							rightPanel.add(compareResultsAgainstExhaustiveSearch);
							rightPanel.add(votingAsConstructBox);

							rightPanel.add(subProcessBox);
							rightPanel.add(mapModelBox);

							rightPanel.add(votingAsAnnotationBox);

							rightPanel.add(new JLabel("Enter timeout for algorithms in seconds:"));

							jFormattedTextField3 = new JFormattedTextField(numberFormatter1);
							jFormattedTextField3.setText("30");
							jFormattedTextField3.setMaximumSize(dimension);

							rightPanel.add(jFormattedTextField3);

							rightPanel.add(new JLabel("Enter amount of threads used for computing solutions:"));

							jFormattedTextField4 = new JFormattedTextField(numberFormatter1);
							jFormattedTextField4.setText("1");
							jFormattedTextField4.setMaximumSize(dimension);

							rightPanel.add(jFormattedTextField4);

							rightPanel.add(Box.createRigidArea(verticalSpacingBetweenComponents));

							runButton = new JButton("RUN");
							runButton.addActionListener(new ActionListener() {

								@Override
								public void actionPerformed(ActionEvent ae) {
									// TODO Auto-generated method stub
									// check if results need to be compared against exhaustiveSearch algorithm
									

									// read amount of models to create
									int amountModelsToCreate = Integer.parseInt(jFormattedTextField2.getText());

									// read timeout value
									int timeout = Integer.parseInt(jFormattedTextField3.getText());

									// read number of threads value (must not be <= 0)
									int threads = Integer.parseInt(jFormattedTextField4.getText());
									ExecutorService executor = Executors.newFixedThreadPool(threads);

									AlgorithmToPerform selectedAlgorithm = Enums.AlgorithmToPerform.EXHAUSTIVE;
									int bound = 0;
									HashMap<Enums.AlgorithmToPerform, LinkedList<PModelWithAdditionalActors>> pModelSolutions = null;

									Future<HashMap<Enums.AlgorithmToPerform, LinkedList<PModelWithAdditionalActors>>> future = executor
											.submit(api);
									Exception exception = null;

									if (jrbExhaustiveSearch.isSelected()) {
										selectedAlgorithm = Enums.AlgorithmToPerform.EXHAUSTIVE;

									} else if (jrbLocalMinimum.isSelected()) {
										selectedAlgorithm = Enums.AlgorithmToPerform.HEURISTIC;
										// read the bound value
										bound = Integer.parseInt(jFormattedTextField1.getText());
									} else {
										leftPanel.add(new JLabel("Error: " + "no algorithm selected!"));
										frame.add(leftPanel);
									}

									try {
										api.setAlgorithmToPerform(selectedAlgorithm, bound);
										pModelSolutions = future.get(timeout, TimeUnit.SECONDS);
										future.cancel(true);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										exception = (InterruptedException) e;
										future.cancel(true);
									} catch (ExecutionException e) {
										// TODO Auto-generated catch block
										exception = (ExecutionException) e;
										future.cancel(true);
									} catch (TimeoutException e) {
										// TODO Auto-generated catch block
										exception = (TimeoutException) e;
										future.cancel(true);
									} catch(Exception e) {
										exception = e;
										future.cancel(true);
									}
									
									if(exception == null) {

										if (pModelSolutions.get(selectedAlgorithm) != null) {
											leftPanel.add(new JLabel("RESULTS: "));
											LinkedList<PModelWithAdditionalActors> cheapestInst = null;
											if (selectedAlgorithm.equals(Enums.AlgorithmToPerform.EXHAUSTIVE)) {
												cheapestInst = CommonFunctionality.getCheapestPModelsWithAdditionalActors(pModelSolutions.get(selectedAlgorithm));
												leftPanel.add(new JLabel("Exhaustive search sum amount solution(s): "
														+ pModelSolutions.get(selectedAlgorithm).size()));
											} else {
												cheapestInst = pModelSolutions.get(selectedAlgorithm);
											}
											if (cheapestInst != null) {
												leftPanel.add(new JLabel(selectedAlgorithm.name() + ": " + cheapestInst.size()
														+ " cheapest solutions"));
												leftPanel.add(new JLabel("Cost of cheapest solution(s): "
														+ cheapestInst.getFirst().getSumMeasure()));
											} else {
												leftPanel.add(new JLabel("No solutions found!!!"));
											}
										}
										
										if (selectedAlgorithm.equals(Enums.AlgorithmToPerform.HEURISTIC)
												) {
											if (compareResultsAgainstExhaustiveSearch.isSelected() && api != null) {
												
												HashMap<Enums.AlgorithmToPerform, LinkedList<PModelWithAdditionalActors>> pInstancesExhaustive = null;
												Future<HashMap<Enums.AlgorithmToPerform, LinkedList<PModelWithAdditionalActors>>> futureexhaustiveSearch = executor
														.submit(api);
												Exception exceptionexhaustiveSearch = null;

												try {
													api.setAlgorithmToPerform(Enums.AlgorithmToPerform.EXHAUSTIVE, bound);
													pInstancesExhaustive = futureexhaustiveSearch.get(timeout, TimeUnit.SECONDS);
													futureexhaustiveSearch.cancel(true);
												} catch (InterruptedException e) {
													// TODO Auto-generated catch block
													exceptionexhaustiveSearch = (InterruptedException) e;
													futureexhaustiveSearch.cancel(true);
												} catch (ExecutionException e) {
													// TODO Auto-generated catch block
													exceptionexhaustiveSearch = (ExecutionException) e;
													futureexhaustiveSearch.cancel(true);
												} catch (TimeoutException e) {
													// TODO Auto-generated catch block
													exceptionexhaustiveSearch = (TimeoutException) e;
													futureexhaustiveSearch.cancel(true);
												}

												if (exceptionexhaustiveSearch != null) {
													leftPanel.add(
															new JLabel("Exception: " + exceptionexhaustiveSearch.getMessage()));
													leftPanel.add(new JLabel("Comparison against Exhaustive Search not possible!"));
												} else {
													LinkedList<PModelWithAdditionalActors> exhaustiveSearchSolutions = pInstancesExhaustive.get(Enums.AlgorithmToPerform.EXHAUSTIVE);
													LinkedList<PModelWithAdditionalActors> heuristicSearchSolutions = pModelSolutions.get(selectedAlgorithm);

													String comparison = CommonFunctionality.compareResultsOfAlgorithmsForDifferentAPIs(heuristicSearchSolutions, exhaustiveSearchSolutions, 100000);
										
													leftPanel.add(new JLabel("Exhaustive Search: "
															+ pInstancesExhaustive.get(Enums.AlgorithmToPerform.EXHAUSTIVE).size() + " solution(s)"));
													LinkedList<PModelWithAdditionalActors> cheapestExhaustiveSearchSolutions = CommonFunctionality.getCheapestPModelsWithAdditionalActors(exhaustiveSearchSolutions);
															
													leftPanel.add(new JLabel(
															"Cost of cheapest solution(s): " + cheapestExhaustiveSearchSolutions
																	.getFirst().getSumMeasure()));
													leftPanel.add(new JLabel("Cheapest solution(s) found:  " + comparison));
												
												
												}

											}
											executor.shutdownNow();
										}

										
										leftPanel.add(new JLabel("Model(s) stored in: "
												+ props.getProperty("defaultDirectoryForStoringModels")));

										// check whether voting as construct or as annotation is selected
										if (votingAsConstructBox.isSelected()) {
											try {
												// check whether or not voting should be inside subprocess and whether or
												// not it should be mapped
												CommonFunctionality.generateNewModelsWithAddActorsAsBpmnConstruct(api,
														pModelSolutions.get(selectedAlgorithm), amountModelsToCreate,
														props.getProperty("defaultDirectoryForStoringModels"),
														subProcessBox.isSelected(), mapModelBox.isSelected());
											} catch (IOException e) {
												// TODO Auto-generated catch block
												rightPanel.add(new JLabel("IO Exception: " + e.getMessage()));
											} catch (Exception e) {
												leftPanel.add(new JLabel("Exception: " + e.getMessage()));

											}
										} else if (votingAsAnnotationBox.isSelected()) {
											CommonFunctionality.generateNewModelsWithAnnotatedChosenParticipants(api,
													pModelSolutions.get(selectedAlgorithm), amountModelsToCreate,
													props.getProperty("defaultDirectoryForStoringModels"));
										}

									}
									else  {
										leftPanel.add(new JLabel("Exception: " + exception.getClass().getName()));
										frame.pack();
									}

									frame.pack();
									runButton.setText("FINISHED");
									runButton.setEnabled(false);
								}

							});

							rightPanel.add(runButton);
							rightPanel.add(Box.createRigidArea(verticalSpacingBetweenComponents));
							frame.add(rightPanel);

						} catch (Exception e2) {
							// TODO Auto-generated catch block
							frame.getContentPane().remove(leftPanel);
							frame.getContentPane().remove(rightPanel);
							leftPanel = new JPanel();
							leftPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
							leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.PAGE_AXIS));
							leftPanel.add(new JLabel("Error: " + e2.getMessage()));
							frame.add(leftPanel);

						} finally {
							frame.pack();

						}
					}
				}

			}

		});

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	}
}
