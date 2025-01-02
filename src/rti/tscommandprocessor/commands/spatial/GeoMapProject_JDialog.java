// GeoMapProject_JDialog - editor for GeoMapProject command

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2024 Colorado Department of Natural Resources

CDSS Time Series Processor Java Library is free software:  you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

CDSS Time Series Processor Java Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

You should have received a copy of the GNU General Public License
    along with CDSS Time Series Processor Java Library.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

package rti.tscommandprocessor.commands.spatial;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import RTi.Util.GUI.DictionaryJDialog;
import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

@SuppressWarnings("serial")
public class GeoMapProject_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{

	private final String AddWorkingDirectory = "Abs";
	private final String RemoveWorkingDirectory = "Rel";

	// Tab positions.
	private final int newTabIndex = 0;
	private final int copyTabIndex = 1;
	private final int readTabIndex = 2;
	private final int deleteTabIndex = 3;
	private final int outputTabIndex = 4;

	private boolean errorWait = false; // To track errors.
	private boolean firstTime = true; // Indicate first time display.
	private JTextArea command_JTextArea = null;
	private JTabbedPane main_JTabbedPane = null;
	// General.
	private SimpleJComboBox ProjectCommand_JComboBox = null;
	private JTextField GeoMapProjectID_JTextField = null;
	// New.
	private JTextField NewGeoMapProjectID_JTextField = null;
	private JTextField Name_JTextField = null;
	private JTextField Description_JTextField = null;
	private JTextArea Properties_JTextArea = null;
	// Read.
	private SimpleJButton browseInput_JButton = null;
	private SimpleJButton pathInput_JButton = null;
	private JTextField InputFile_JTextField = null;
	// Write.
	private SimpleJButton browseOutput_JButton = null;
	private SimpleJButton pathOutput_JButton = null;
	private JTextField OutputFile_JTextField = null;
	private SimpleJComboBox JsonFormat_JComboBox = null;
	private String workingDir = null;

	private SimpleJButton cancel_JButton = null;
	private SimpleJButton ok_JButton = null;
	private SimpleJButton help_JButton = null;
	private GeoMapProject_Command command = null;
	private JFrame parent = null;
	private boolean ok = false;

	/**
	Command dialog constructor.
	@param parent JFrame class instantiating this class.
	@param command Command to edit.
	@param tableIDChoices list of table identifiers to provide as choices
	*/
	public GeoMapProject_JDialog ( JFrame parent, GeoMapProject_Command command, List<String> tableIDChoices ) {
		super(parent, true);
		initialize ( parent, command, tableIDChoices );
	}

	/**
	Responds to ActionEvents.
	@param event ActionEvent object
	*/
	public void actionPerformed(ActionEvent event) {
		Object o = event.getSource();
	
    	if ( o == this.ProjectCommand_JComboBox ) {
    		setTabForProjectCommand();
    		refresh();
    	}
    	else if ( o == this.browseInput_JButton ) {
			// Browse for the file to read.
			JFileChooser fc = new JFileChooser();
        	fc.setDialogTitle( "Select GeoMap Project JSON File");
        	SimpleFileFilter sff = new SimpleFileFilter("json","GeoMap Project JSON File");
        	fc.addChoosableFileFilter(sff);
        	sff = new SimpleFileFilter("gmpjson","GeoMap Project GeoJSON File");
        	fc.addChoosableFileFilter(sff);
		
			String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
			if ( last_directory_selected != null ) {
				fc.setCurrentDirectory(	new File(last_directory_selected));
			}
			else {
            	fc.setCurrentDirectory(new File(this.workingDir));
			}
			if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				String directory = fc.getSelectedFile().getParent();
				String filename = fc.getSelectedFile().getName(); 
				String path = fc.getSelectedFile().getPath(); 
	
				if (filename == null || filename.equals("")) {
					return;
				}
	
				if (path != null) {
					// Convert path to relative path by default.
					try {
						this.InputFile_JTextField.setText(IOUtil.toRelativePath(this.workingDir, path));
					}
					catch ( Exception e ) {
						Message.printWarning ( 1,"NewObject_JDialog", "Error converting file to relative path." );
					}
					JGUIUtil.setLastFileDialogDirectory( directory);
					refresh();
				}
			}
		}
		else if ( o == this.browseOutput_JButton ) {
			String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
			JFileChooser fc = null;
			if ( last_directory_selected != null ) {
				fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
			}
			else {
				fc = JFileChooserFactory.createJFileChooser( this.workingDir );
			}
			fc.setDialogTitle("Select JSON File to Write");
			SimpleFileFilter sff = new SimpleFileFilter("json", "JSON GeoMap project file");
			fc.addChoosableFileFilter(sff);

			if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				String directory = fc.getSelectedFile().getParent();
				String filename = fc.getSelectedFile().getName();
				String path = fc.getSelectedFile().getPath();
	
				if (filename == null || filename.equals("")) {
					return;
				}

				if (path != null) {
					// Convert path to relative path by default.
					try {
						this.OutputFile_JTextField.setText(IOUtil.toRelativePath(this.workingDir, path));
					}
					catch ( Exception e ) {
						Message.printWarning ( 1,"GeoMapProject_JDialog", "Error converting file to relative path." );
					}
					JGUIUtil.setLastFileDialogDirectory(directory );
					refresh();
				}
			}
		}
		else if ( o == this.cancel_JButton ) {
			response ( false );
		}
    	else if ( event.getActionCommand().equalsIgnoreCase("EditProperties") ) {
        	// Edit the dictionary in the dialog.  It is OK for the string to be blank.
        	String Properties = this.Properties_JTextArea.getText().trim();
        	String [] notes = {
            	"GeoMap project properties provide project configuration data."
        	};
        	String dict = (new DictionaryJDialog ( this.parent, true, Properties,
            	"Edit Properties Parameter", notes, "Property Name", "Property Value",10)).response();
        	if ( dict != null ) {
            	this.Properties_JTextArea.setText ( dict );
            	refresh();
        	}
    	}
		else if ( o == this.help_JButton ) {
			HelpViewer.getInstance().showHelp("command", "GeoMapProject");
		}
		else if ( o == this.ok_JButton ) {
			refresh ();
			checkInput ();
			if ( !this.errorWait ) {
				// Command has been edited.
				response ( true );
			}
		}
		else if ( o == this.pathInput_JButton ) {
			if ( this.pathInput_JButton.getText().equals( this.AddWorkingDirectory) ) {
				this.InputFile_JTextField.setText ( IOUtil.toAbsolutePath(this.workingDir, this.InputFile_JTextField.getText() ) );
			}
			else if ( this.pathInput_JButton.getText().equals( this.RemoveWorkingDirectory) ) {
				try {
                	this.InputFile_JTextField.setText ( IOUtil.toRelativePath ( this.workingDir, this.InputFile_JTextField.getText() ) );
				}
				catch ( Exception e ) {
					Message.printWarning ( 1, "GeoMapProject_JDialog", "Error converting file to relative path." );
				}
			}
			refresh ();
		}
		else if ( o == this.pathOutput_JButton ) {
			if ( this.pathOutput_JButton.getText().equals(this.AddWorkingDirectory) ) {
				this.OutputFile_JTextField.setText (
				IOUtil.toAbsolutePath(this.workingDir,this.OutputFile_JTextField.getText() ) );
			}
			else if ( this.pathOutput_JButton.getText().equals(this.RemoveWorkingDirectory) ) {
				try {
			    	this.OutputFile_JTextField.setText (
					IOUtil.toRelativePath ( this.workingDir,this.OutputFile_JTextField.getText() ) );
				}
				catch ( Exception e ) {
					Message.printWarning ( 1, "WriteObjectToJSON_JDialog",
					"Error converting file to relative path." );
				}
			}
			refresh ();
		}
		else {
			refresh();
		}
	}

	/**
	Check the input.  If errors exist, warn the user and set the this.errorWait flag to true.
	This should be called before response() is allowed to complete.
	*/
	private void checkInput () {
		// Create a list of parameters to check.
		PropList props = new PropList ( "" );
		// General.
		String ProjectCommand = this.ProjectCommand_JComboBox.getSelected();
		String GeoMapProjectID = this.GeoMapProjectID_JTextField.getText().trim();
		// New.
		String NewGeoMapProjectID = this.NewGeoMapProjectID_JTextField.getText().trim();
		String Name = this.Name_JTextField.getText().trim();
		String Description = this.Description_JTextField.getText().trim();
		String Properties = this.Properties_JTextArea.getText().trim().replace("\n"," ");
		// Read.
		String InputFile = this.InputFile_JTextField.getText().trim();
		// Write.
		String OutputFile = this.OutputFile_JTextField.getText().trim();
		String JsonFormat = this.JsonFormat_JComboBox.getSelected();
		this.errorWait = false;

		if ( (ProjectCommand != null) && !ProjectCommand.isEmpty() ) {
			props.set ( "ProjectCommand", ProjectCommand );
		}
		if ( !GeoMapProjectID.isEmpty() ) {
        	props.set ( "GeoMapProjectID", GeoMapProjectID );
    	}
    	// New.
    	if ( !NewGeoMapProjectID.isEmpty() ) {
        	props.set ( "NewGeoMapProjectID", NewGeoMapProjectID );
    	}
    	if ( !Name.isEmpty() ) {
        	props.set ( "Name", Name );
    	}
    	if ( !Description.isEmpty() ) {
        	props.set ( "Description", Description );
    	}
    	if ( !Properties.isEmpty() ) {
        	props.set ( "Properties", Properties );
    	}
    	// Read.
		if ( InputFile.length() > 0 ) {
			props.set ( "InputFile", InputFile );
		}
    	// Write.
    	if ( !OutputFile.isEmpty() ) {
        	props.set ( "OutputFile", OutputFile );
    	}
    	if ( !JsonFormat.isEmpty() ) {
        	props.set ( "JsonFormat", JsonFormat );
    	}
		try {
	    	// This will warn the user.
			this.command.checkCommandParameters ( props, null, 1 );
		}
		catch ( Exception e ) {
        	Message.printWarning(2,"", e);
			// The warning would have been printed in the check code.
			this.errorWait = true;
		}
	}

	/**
	Commit the edits to the command.
	In this case the command parameters have already been checked and no errors were detected.
	*/
	private void commitEdits () {
		// General.
		String ProjectCommand = this.ProjectCommand_JComboBox.getSelected();
		String GeoMapProjectID = this.GeoMapProjectID_JTextField.getText().trim();
		// New.
		String NewGeoMapProjectID = this.NewGeoMapProjectID_JTextField.getText().trim();
		String Name = this.Name_JTextField.getText().trim();
		String Description = this.Description_JTextField.getText().trim();
		String Properties = this.Properties_JTextArea.getText().trim().replace("\n"," ");
		// Read.
		String InputFile = this.InputFile_JTextField.getText().trim();
		// Write.
		String OutputFile = this.OutputFile_JTextField.getText().trim();
		String JsonFormat = this.JsonFormat_JComboBox.getSelected();
	
		// General.
	    this.command.setCommandParameter ( "ProjectCommand", ProjectCommand );
	    this.command.setCommandParameter ( "GeoMapProjectID", GeoMapProjectID );
	    // New.
	    this.command.setCommandParameter ( "NewGeoMapProjectID", NewGeoMapProjectID );
	    this.command.setCommandParameter ( "Name", Name );
	    this.command.setCommandParameter ( "Description", Description );
	    this.command.setCommandParameter ( "Properties", Properties );
	    // Read.
	    this.command.setCommandParameter ( "InputFile", InputFile );
	    // Write.
	    this.command.setCommandParameter ( "OutputFile", OutputFile );
	    this.command.setCommandParameter ( "JsonFormat", JsonFormat );
	}

	/**
	Return the selected command type enumeration.
	@return the selected command type
	*/
	private GeoMapProjectCommandType getSelectedProjectCommandType () {
	    // The combo box is not null so can get the value.
	    String projectCommand = ProjectCommand_JComboBox.getSelected();
	   	int pos = projectCommand.indexOf(" -");
	   	if ( pos > 0 ) {
	   		// Have a description.
	   		projectCommand = projectCommand.substring(0,pos).trim();
	   	}
	   	else {
	   		// No description.
	       	projectCommand = projectCommand.trim();
	   	}
	   	return GeoMapProjectCommandType.valueOfIgnoreCase(projectCommand);
	}
	
	/**
	Instantiates the GUI components.
	@param parent JFrame class instantiating this class.
	@param command Command to edit and possibly run.
	*/
	private void initialize ( JFrame parent, GeoMapProject_Command command, List<String> tableIDChoices ) {
		this.parent = parent;
		this.command = command;
		CommandProcessor processor = this.command.getCommandProcessor();
		this.workingDir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, this.command );
	
		addWindowListener(this);
	
	    Insets insetsTLBR = new Insets(2,2,2,2);
	
		// Main panel.
	
		JPanel main_JPanel = new JPanel();
		main_JPanel.setLayout(new GridBagLayout());
		getContentPane().add ("North", main_JPanel);
		int y = -1;
	
		JPanel paragraph = new JPanel();
		paragraph.setLayout(new GridBagLayout());
		int yy = -1;
	
	   	JGUIUtil.addComponent(paragraph, new JLabel (
	        "<html><b>This command is under development.</b></html>"),
	        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	   	JGUIUtil.addComponent(paragraph, new JLabel (
	        "This command creates a GeoMap project, which defines a map project configuration, organized as shown below."),
	        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	   	JGUIUtil.addComponent(paragraph, new JLabel (
	        "Currently only the single map project type is implemented."),
	        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	   	JGUIUtil.addComponent(paragraph, new JLabel (
	        "The 'Project command' controls which command parameters are used for processing."),
	        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	   	JGUIUtil.addComponent(paragraph, new JLabel (""),
	        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	   	JGUIUtil.addComponent(paragraph, new JLabel ("* GeoMapProject"),
	        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	   	JGUIUtil.addComponent(paragraph, new JLabel ("     GeoMap[ ] - each project includes 1+ maps"),
	        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	   	JGUIUtil.addComponent(paragraph, new JLabel ("         GeoLayer[ ] - each map includes 1+ layers"),
	        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	   	JGUIUtil.addComponent(paragraph, new JLabel ("         GeoLayerViewGroup[ ] - each map includes 1+ layer view groups"),
	        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	   	JGUIUtil.addComponent(paragraph, new JLabel ("             GeoLayerView[ ] - each layer view group includes 1+ layer views"),
	        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	   	JGUIUtil.addComponent(paragraph, new JLabel ("                 GeoLayer + GeoLayerSymbol (each layer view includes a layer and symbology)"),
	        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	
		JGUIUtil.addComponent(main_JPanel, paragraph,
			0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.NONE, GridBagConstraints.WEST);
		JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
			0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
	    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Project command:"),
			0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
		this.ProjectCommand_JComboBox = new SimpleJComboBox ( false );
		this.ProjectCommand_JComboBox.setToolTipText("Project command to execute.");
		boolean alphabetical = false;
		boolean includeNotes = false;
		List<String> commandChoices = GeoMapProjectCommandType.getChoicesAsStrings ( alphabetical, includeNotes );
		this.ProjectCommand_JComboBox.setData(commandChoices);
		this.ProjectCommand_JComboBox.select ( 0 );
		this.ProjectCommand_JComboBox.addActionListener ( this );
	    JGUIUtil.addComponent(main_JPanel, this.ProjectCommand_JComboBox,
			1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - project command to run (see tabs below)."),
			3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	    JGUIUtil.addComponent(main_JPanel, new JLabel ("GeoMap Project ID:"),
	        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoMapProjectID_JTextField = new JTextField (15);
	    this.GeoMapProjectID_JTextField.setToolTipText("Unique identifier used when processing existing projects.");
	    this.GeoMapProjectID_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(main_JPanel, this.GeoMapProjectID_JTextField,
	        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required (except for New) - project identifier."),
	        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    this.main_JTabbedPane = new JTabbedPane ();
	    //this.main_JTabbedPane.addChangeListener(this);
	    JGUIUtil.addComponent(main_JPanel, this.main_JTabbedPane,
	        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
	    // Panel for new project.
	    int yNew = -1;
	    JPanel new_JPanel = new JPanel();
	    new_JPanel.setLayout( new GridBagLayout() );
	    this.main_JTabbedPane.addTab ( "New", new_JPanel );
	
	    JGUIUtil.addComponent(new_JPanel, new JLabel (
	        "Use these parameters to create a new GeoMap project."),
	        0, ++yNew, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(new_JPanel, new JLabel (
	        "The GeoMap project will be listed in the TSTool Results / Objects area."),
	        0, ++yNew, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
		JGUIUtil.addComponent(new_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
			0, ++yNew, 8, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
	    JGUIUtil.addComponent(new_JPanel, new JLabel ("New GeoMap Project ID:"),
	        0, ++yNew, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.NewGeoMapProjectID_JTextField = new JTextField (30);
	    this.NewGeoMapProjectID_JTextField.setToolTipText("Unique identifier used when creating a new project.");
	    this.NewGeoMapProjectID_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(new_JPanel, this.NewGeoMapProjectID_JTextField,
	        1, yNew, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(new_JPanel, new JLabel ("Required - new project identifier."),
	        3, yNew, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(new_JPanel, new JLabel ("Project name:"),
	        0, ++yNew, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.Name_JTextField = new JTextField (10);
	    this.Name_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(new_JPanel, this.Name_JTextField,
	        1, yNew, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(new_JPanel, new JLabel ("Required - project name."),
	        3, yNew, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(new_JPanel, new JLabel ("Project description:"),
	        0, ++yNew, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.Description_JTextField = new JTextField (10);
	    this.Description_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(new_JPanel, this.Description_JTextField,
	        1, yNew, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(new_JPanel, new JLabel ("Optional - project description."),
	        3, yNew, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(new_JPanel, new JLabel ("Properties:"),
	        0, ++yNew, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.Properties_JTextArea = new JTextArea (6,35);
	    this.Properties_JTextArea.setToolTipText("Properties for the map.  See the command documentation.");
	    this.Properties_JTextArea.setLineWrap ( true );
	    this.Properties_JTextArea.setWrapStyleWord ( true );
	    this.Properties_JTextArea.setToolTipText("PropertyName1:PropertyValue1,PropertyName2:PropertyValue2");
	    this.Properties_JTextArea.addKeyListener (this);
	    JGUIUtil.addComponent(new_JPanel, new JScrollPane(this.Properties_JTextArea),
	        1, yNew, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(new_JPanel, new JLabel ("Optional - map project properties."),
	        3, yNew, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	    JGUIUtil.addComponent(new_JPanel, new SimpleJButton ("Edit","EditProperties",this),
	        3, ++yNew, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    // Panel for copy project.
	    int yCopy = -1;
	    JPanel copy_JPanel = new JPanel();
	    copy_JPanel.setLayout( new GridBagLayout() );
	    this.main_JTabbedPane.addTab ( "Copy", copy_JPanel );
	
	    JGUIUtil.addComponent(copy_JPanel, new JLabel (
	        "Copy a GeoMap project by specifying the GeoMap Project identifier above and parameters in the 'New' tab."),
	        0, ++yCopy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(copy_JPanel, new JLabel (
	        "The GeoMap project will be listed in the TSTool Results / Objects area."),
	        0, ++yCopy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
		JGUIUtil.addComponent(copy_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
			0, ++yCopy, 8, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
	    // Panel for read.
	    int yInput = -1;
	    JPanel input_JPanel = new JPanel();
	    input_JPanel.setLayout( new GridBagLayout() );
	    this.main_JTabbedPane.addTab ( "Read", input_JPanel );
	
	    JGUIUtil.addComponent(input_JPanel, new JLabel (
	        "Use these parameters to read an existing GeoMap project JSON file."),
	        0, ++yInput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(input_JPanel, new JLabel (
	        "Use the 'New' tab to provide data to overwrite values read from the file."),
	        0, ++yInput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(input_JPanel, new JLabel (
	        "The GeoMap project will be listed in the TSTool Results / Objects area."),
	        0, ++yInput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
		JGUIUtil.addComponent(input_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
			0, ++yInput, 8, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
	    JGUIUtil.addComponent(input_JPanel, new JLabel ( "GeoMap project JSON input file to read:" ),
			0, ++yInput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
		this.InputFile_JTextField = new JTextField ( 50 );
		this.InputFile_JTextField.addKeyListener ( this );
	    // Input file layout fights back with other rows so put in its own panel.
		JPanel InputFile_JPanel = new JPanel();
		InputFile_JPanel.setLayout(new GridBagLayout());
	    JGUIUtil.addComponent(InputFile_JPanel, this.InputFile_JTextField,
			0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
		this.browseInput_JButton = new SimpleJButton ( "...", this );
		this.browseInput_JButton.setToolTipText("Browse for file");
	    JGUIUtil.addComponent(InputFile_JPanel, this.browseInput_JButton,
			1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
		if ( this.workingDir != null ) {
			// Add the button to allow conversion to/from relative path.
			this.pathInput_JButton = new SimpleJButton(	this.RemoveWorkingDirectory,this);
			JGUIUtil.addComponent(InputFile_JPanel, this.pathInput_JButton,
				2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
		}
		JGUIUtil.addComponent(input_JPanel, InputFile_JPanel,
			1, yInput, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
	    // Panel for delete project.
	    int yDelete = -1;
	    JPanel delete_JPanel = new JPanel();
	    delete_JPanel.setLayout( new GridBagLayout() );
	    this.main_JTabbedPane.addTab ( "Delete", delete_JPanel );
	
	    JGUIUtil.addComponent(delete_JPanel, new JLabel (
	        "Delete a GeoMap project by specifying its identifier above."),
	        0, ++yDelete, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
		JGUIUtil.addComponent(delete_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
			0, ++yDelete, 8, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
	    // Panel for output.
	    int yWrite = -1;
	    JPanel g = new JPanel();
	    g.setLayout( new GridBagLayout() );
	    this.main_JTabbedPane.addTab ( "Write", g );
	
	    JGUIUtil.addComponent(g, new JLabel (
	        "Use these parameters to output a GeoMap project JSON file."),
	        0, ++yWrite, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
		JGUIUtil.addComponent(g, new JSeparator(SwingConstants.HORIZONTAL),
			0, ++yWrite, 8, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
	    JGUIUtil.addComponent(g, new JLabel ( "Project output file to write:" ),
			0, ++yWrite, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
		this.OutputFile_JTextField = new JTextField ( 40 );
		this.OutputFile_JTextField.setToolTipText("Specify the file to output, can use ${Property} notation");
		this.OutputFile_JTextField.addKeyListener ( this );
	    // Output file layout fights back with other rows so put in its own panel.
		JPanel OutputFile_JPanel = new JPanel();
		OutputFile_JPanel.setLayout(new GridBagLayout());
	    JGUIUtil.addComponent(OutputFile_JPanel, this.OutputFile_JTextField,
			0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
		this.browseOutput_JButton = new SimpleJButton ( "...", this );
		this.browseOutput_JButton.setToolTipText("Browse for file");
	    JGUIUtil.addComponent(OutputFile_JPanel, this.browseOutput_JButton,
			1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
		if ( this.workingDir != null ) {
			// Add the button to allow conversion to/from relative path.
			this.pathOutput_JButton = new SimpleJButton( this.RemoveWorkingDirectory,this);
			JGUIUtil.addComponent(OutputFile_JPanel, this.pathOutput_JButton,
				2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
		}
		JGUIUtil.addComponent(g, OutputFile_JPanel,
			1, yWrite, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	    JGUIUtil.addComponent(g, new JLabel ( "JSON format:"),
			0, ++yWrite, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
		this.JsonFormat_JComboBox = new SimpleJComboBox ( false );
		this.JsonFormat_JComboBox.setToolTipText("JSON format, 'Named' has top-level element name.");
		List<String> jsonFormatChoices = new ArrayList<>();
		jsonFormatChoices.add("");
		jsonFormatChoices.add("Bare");
		jsonFormatChoices.add("Named");
		this.JsonFormat_JComboBox.setData(jsonFormatChoices);
		this.JsonFormat_JComboBox.select ( 0 );
		this.JsonFormat_JComboBox.addActionListener ( this );
	    JGUIUtil.addComponent(g, this.JsonFormat_JComboBox,
			1, yWrite, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(g, new JLabel("Optional - JSON format (default=Named)."),
			3, yWrite, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	    JGUIUtil.addComponent(main_JPanel, new JLabel ("Command:"),
			0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
		this.command_JTextArea = new JTextArea (7,60);
		this.command_JTextArea.setLineWrap ( true );
		this.command_JTextArea.setWrapStyleWord ( true );
		this.command_JTextArea.setEditable (false);
		JGUIUtil.addComponent(main_JPanel, new JScrollPane(this.command_JTextArea),
			1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
		// Refresh the contents.
		refresh ();
	
		// South JPanel: North
		JPanel button_JPanel = new JPanel();
		button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
	        JGUIUtil.addComponent(main_JPanel, button_JPanel,
			0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);
	
		this.ok_JButton = new SimpleJButton("OK", this);
		this.ok_JButton.setToolTipText("Save changes to command");
		button_JPanel.add (this.ok_JButton);
		this.cancel_JButton = new SimpleJButton("Cancel", this);
		button_JPanel.add (this.cancel_JButton);
		this.cancel_JButton.setToolTipText("Cancel without saving changes to command");
		button_JPanel.add ( this.help_JButton = new SimpleJButton("Help", this) );
		this.help_JButton.setToolTipText("Show command documentation in web browser");
	
		setTitle ( "Edit " + this.command.getCommandName() + " Command");
	    pack();
	    JGUIUtil.center(this);
		refresh();	// Sets the __path_JButton status.
		setResizable (false);
	    super.setVisible(true);
	}
	
	/**
	Handle ItemEvent events.
	@param e ItemEvent to handle.
	*/
	public void itemStateChanged (ItemEvent e) {
		Object o = e.getSource();
	    if ( o == this.ProjectCommand_JComboBox ) {
	    	setTabForProjectCommand();
	    }
		refresh();
	}
	
	/**
	Respond to KeyEvents.
	*/
	public void keyPressed (KeyEvent event) {
		int code = event.getKeyCode();
	
		if (code == KeyEvent.VK_ENTER) {
			refresh ();
			checkInput();
			if (!this.errorWait) {
				response ( true );
			}
		}
	}
	
	public void keyReleased (KeyEvent event) {
		refresh();
	}
	
	public void keyTyped (KeyEvent event) {
	}
	
	/**
	Indicate if the user pressed OK (cancel otherwise).
	@return true if the edits were committed, false if the user canceled.
	*/
	public boolean ok () {
		return this.ok;
	}
	
	/**
	Refresh the command from the other text field contents.
	*/
	private void refresh () {
		String routine = getClass().getSimpleName() + ".refresh";
		// General.
		String ProjectCommand = "";
		String GeoMapProjectID = "";
		// New.
		String NewGeoMapProjectID = "";
		String Name = "";
		String Description = "";
		String Properties = "";
		// Read.
		String InputFile = "";
		// Write.
		String OutputFile = "";
		String JsonFormat = "";
		PropList props = this.command.getCommandParameters();
		if (this.firstTime) {
			this.firstTime = false;
			// General.
			ProjectCommand = props.getValue ( "ProjectCommand" );
			GeoMapProjectID = props.getValue ( "GeoMapProjectID" );
			// New.
			NewGeoMapProjectID = props.getValue ( "NewGeoMapProjectID" );
			Name = props.getValue ( "Name" );
			Description = props.getValue ( "Description" );
			Properties = props.getValue ( "Properties" );
			// Read.
			InputFile = props.getValue ( "InputFile" );
			// Write.
			OutputFile = props.getValue ( "OutputFile" );
			JsonFormat = props.getValue ( "JsonFormat" );
	
			// General.
			if ( JGUIUtil.isSimpleJComboBoxItem(this.ProjectCommand_JComboBox, ProjectCommand,JGUIUtil.NONE, null, null ) ) {
				this.ProjectCommand_JComboBox.select ( ProjectCommand );
			}
			else {
	            if ( (ProjectCommand == null) ||	ProjectCommand.equals("") ) {
					// New command...select the default.
					this.ProjectCommand_JComboBox.select ( 0 );
				}
				else {
					// Bad user command.
					Message.printWarning ( 1, routine,
					"Existing command references an invalid\n"+
					"ProjectCommand parameter \"" + ProjectCommand + "\".  Select a value or Cancel." );
				}
			}
	        if ( GeoMapProjectID != null ) {
	            this.GeoMapProjectID_JTextField.setText ( GeoMapProjectID );
	        }
	        // New.
	        if ( NewGeoMapProjectID != null ) {
	            this.NewGeoMapProjectID_JTextField.setText ( NewGeoMapProjectID );
	            this.main_JTabbedPane.setSelectedIndex(0);
	        }
	        if ( Name != null ) {
	            this.Name_JTextField.setText ( Name );
	        }
	        if ( Description != null ) {
	            this.Description_JTextField.setText ( Description );
	        }
	        if ( Properties != null ) {
	            this.Properties_JTextArea.setText ( Properties );
	        }
	        // Read.
			if ( InputFile != null ) {
				this.InputFile_JTextField.setText (InputFile);
	            this.main_JTabbedPane.setSelectedIndex(1);
			}
	        // Write.
			if ( OutputFile != null ) {
				this.OutputFile_JTextField.setText (OutputFile);
	            this.main_JTabbedPane.setSelectedIndex(2);
			}
			if ( JGUIUtil.isSimpleJComboBoxItem(this.JsonFormat_JComboBox, JsonFormat,JGUIUtil.NONE, null, null ) ) {
				this.JsonFormat_JComboBox.select ( JsonFormat );
			}
			else {
	            if ( (JsonFormat == null) ||	JsonFormat.equals("") ) {
					// New command...select the default.
					this.JsonFormat_JComboBox.select ( 0 );
				}
				else {
					// Bad user command.
					Message.printWarning ( 1, routine,
					"Existing command references an invalid\n"+
					"JsonFormat parameter \"" + JsonFormat + "\".  Select a value or Cancel." );
				}
			}
			// Set the tab for selected project command.
			setTabForProjectCommand();
		}
	
		// Regardless, reset the command from the fields.
	
		// General.
		ProjectCommand = this.ProjectCommand_JComboBox.getSelected();
		GeoMapProjectID = this.GeoMapProjectID_JTextField.getText().trim();
		// New.
		NewGeoMapProjectID = this.NewGeoMapProjectID_JTextField.getText().trim();
		Name = this.Name_JTextField.getText().trim();
		Description = this.Description_JTextField.getText().trim();
		Properties = this.Properties_JTextArea.getText().trim();
		// Read.
		InputFile = this.InputFile_JTextField.getText().trim();
		// Write.
		OutputFile = this.OutputFile_JTextField.getText().trim();
		JsonFormat = this.JsonFormat_JComboBox.getSelected();
		props = new PropList ( this.command.getCommandName() );
	
		// General.
		props.add ( "ProjectCommand=" + ProjectCommand );
		props.add ( "GeoMapProjectID=" + GeoMapProjectID );
		// New.
		props.add ( "NewGeoMapProjectID=" + NewGeoMapProjectID );
		props.add ( "Name=" + Name );
		props.add ( "Description=" + Description );
		props.add ( "Properties=" + Properties );
		// Read.
		props.add ( "InputFile=" + InputFile );
		// Write.
		props.add ( "OutputFile=" + OutputFile );
		props.add ( "JsonFormat=" + JsonFormat );
		this.command_JTextArea.setText( this.command.toString ( props ).trim() );
		// Check the path and determine what the label on the path button should be.
		if ( this.pathInput_JButton != null ) {
			if ( (InputFile != null) && !InputFile.isEmpty() ) {
				this.pathInput_JButton.setEnabled ( true );
				File f = new File ( InputFile );
				if ( f.isAbsolute() ) {
					this.pathInput_JButton.setText ( this.RemoveWorkingDirectory );
					this.pathInput_JButton.setToolTipText("Change path to relative to command file");
				}
				else {
			    	this.pathInput_JButton.setText ( this.AddWorkingDirectory );
			    	this.pathInput_JButton.setToolTipText("Change path to absolute");
				}
			}
			else {
				this.pathInput_JButton.setEnabled(false);
			}
		}
		if ( this.pathOutput_JButton != null ) {
			if ( (OutputFile != null) && !OutputFile.isEmpty() ) {
				this.pathOutput_JButton.setEnabled ( true );
				File f = new File ( OutputFile );
				if ( f.isAbsolute() ) {
					this.pathOutput_JButton.setText ( this.RemoveWorkingDirectory );
					this.pathOutput_JButton.setToolTipText("Change path to relative to command file");
				}
				else {
			    	this.pathOutput_JButton.setText ( this.AddWorkingDirectory );
			    	this.pathOutput_JButton.setToolTipText("Change path to absolute");
				}
			}
			else {
				this.pathOutput_JButton.setEnabled(false);
			}
		}
	}
	
	/**
	React to the user response.
	@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
	*/
	private void response ( boolean ok ) {
		this.ok = ok;	// Save to be returned by ok().
		if ( ok ) {
			// Commit the changes.
			commitEdits ();
			if ( this.errorWait ) {
				// Not ready to close out.
				return;
			}
		}
		// Now close out.
		setVisible( false );
		dispose();
	}
	
	/**
	 * Set the parameter tab based on the selected command.
	 */
	private void setTabForProjectCommand() {
		GeoMapProjectCommandType commandType = getSelectedProjectCommandType();
		if ( commandType == GeoMapProjectCommandType.COPY ) {
			this.main_JTabbedPane.setSelectedIndex(this.copyTabIndex);
		}
		else if ( commandType == GeoMapProjectCommandType.DELETE ) {
			this.main_JTabbedPane.setSelectedIndex(this.deleteTabIndex);
		}
		else if ( commandType == GeoMapProjectCommandType.NEW_PROJECT ) {
			this.main_JTabbedPane.setSelectedIndex(this.newTabIndex);
		}
		else if ( commandType == GeoMapProjectCommandType.READ ) {
			this.main_JTabbedPane.setSelectedIndex(this.readTabIndex);
		}
		else if ( commandType == GeoMapProjectCommandType.WRITE ) {
			this.main_JTabbedPane.setSelectedIndex(this.outputTabIndex);
		}
	}
	
	/**
	Responds to WindowEvents.
	@param event WindowEvent object
	*/
	public void windowClosing(WindowEvent event) {
		response ( false );
	}
	
	// The following methods are all necessary because this class implements WindowListener.
	
	public void windowActivated(WindowEvent evt) {
	}
	
	public void windowClosed(WindowEvent evt) {
	}
	
	public void windowDeactivated(WindowEvent evt) {
	}
	
	public void windowDeiconified(WindowEvent evt) {
	}
	
	public void windowIconified(WindowEvent evt) {
	}
	
	public void windowOpened(WindowEvent evt) {
	}

}