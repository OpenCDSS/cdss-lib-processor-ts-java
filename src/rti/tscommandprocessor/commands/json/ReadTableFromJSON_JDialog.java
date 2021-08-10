// ReadTableFromJSON_JDialog - editor dialog for ReadTableFromJSON command

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2019 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.json;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

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

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

@SuppressWarnings("serial")
public class ReadTableFromJSON_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{

// Used for button labels...

private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private boolean __error_wait = false;
private boolean __first_time = true;
private JTextArea __command_JTextArea=null;
private JTextField __TableID_JTextField = null;
private JTextField __InputFile_JTextField = null;
private JTextField __ArrayName_JTextField = null;
private JTextField __ExcludeNames_JTextField = null;
private JTextField __ArrayColumns_JTextField = null;
private JTextField __BooleanColumns_JTextField = null;
private JTextField __DateTimeColumns_JTextField = null;
private JTextField __DoubleColumns_JTextField = null;
private JTextField __IntegerColumns_JTextField = null;
private JTextField __TextColumns_JTextField = null;
private JTextField __Top_JTextField = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SimpleJButton __browse_JButton = null;
private SimpleJButton __path_JButton = null;
private String __working_dir = null;	
private ReadTableFromJSON_Command __command = null;
private boolean __ok = false;

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadTableFromJSON_JDialog ( JFrame parent, ReadTableFromJSON_Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed(ActionEvent event)
{	Object o = event.getSource();

	if ( o == __browse_JButton ) {
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
		}
		else {
            fc = JFileChooserFactory.createJFileChooser( __working_dir );
		}
		fc.setDialogTitle("Select Delimited Table File");
        SimpleFileFilter sff = new SimpleFileFilter("json", "JSON File");
		fc.addChoosableFileFilter(sff);
		fc.addChoosableFileFilter( new SimpleFileFilter("txt", "JSON File") );
		fc.setFileFilter(sff);

		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			String directory = fc.getSelectedFile().getParent();
			String path = fc.getSelectedFile().getPath();
			// Convert path to relative path by default.
			try {
				__InputFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,"ReadTableFromJSON_JDialog", "Error converting file to relative path." );
			}
			JGUIUtil.setLastFileDialogDirectory(directory);
			refresh ();
		}
	}
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "ReadTableFromJSON");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput ();
		if ( !__error_wait ) {
			// Command has been edited...
			response ( true );
		}
	}
	else if ( o == __path_JButton ) {
		if (__path_JButton.getText().equals( __AddWorkingDirectory)) {
			__InputFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir,__InputFile_JTextField.getText()));
		}
		else if (__path_JButton.getText().equals( __RemoveWorkingDirectory)) {
			try {
                __InputFile_JTextField.setText ( IOUtil.toRelativePath (__working_dir,
                        __InputFile_JTextField.getText()));
			}
			catch (Exception e) {
				Message.printWarning (1, 
				__command.getCommandName() + "_JDialog", "Error converting file to relative path.");
			}
		}
		refresh ();
	}
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
    String TableID = __TableID_JTextField.getText().trim();
	String InputFile = __InputFile_JTextField.getText().trim();
	String ArrayName = __ArrayName_JTextField.getText().trim();
	String ExcludeNames  = __ExcludeNames_JTextField.getText().trim();
	String ArrayColumns  = __ArrayColumns_JTextField.getText().trim();
	String BooleanColumns  = __BooleanColumns_JTextField.getText().trim();
	String DateTimeColumns  = __DateTimeColumns_JTextField.getText().trim();
	String DoubleColumns  = __DoubleColumns_JTextField.getText().trim();
	String IntegerColumns  = __IntegerColumns_JTextField.getText().trim();
	String TextColumns  = __TextColumns_JTextField.getText().trim();
	String Top  = __Top_JTextField.getText().trim();
	__error_wait = false;

    if ( TableID.length() > 0 ) {
        props.set ( "TableID", TableID );
    }
	if ( InputFile.length() > 0 ) {
		props.set ( "InputFile", InputFile );
	}
    if ( ArrayName.length() > 0 ) {
        props.set ( "ArrayName", ArrayName );
    }
    if ( ExcludeNames.length() > 0 ) {
        props.set ( "ExcludeNames", ExcludeNames );
    }
    if ( ArrayColumns.length() > 0 ) {
        props.set ( "ArrayColumns", ArrayColumns );
    }
    if ( BooleanColumns.length() > 0 ) {
        props.set ( "BooleanColumns", BooleanColumns );
    }
    if ( DateTimeColumns.length() > 0 ) {
        props.set ( "DateTimeColumns", DateTimeColumns );
    }
    if ( DoubleColumns.length() > 0 ) {
        props.set ( "DoubleColumns", DoubleColumns );
    }
    if ( IntegerColumns.length() > 0 ) {
        props.set ( "IntegerColumns", IntegerColumns );
    }
    if ( TextColumns.length() > 0 ) {
        props.set ( "TextColumns", TextColumns );
    }
    if ( Top.length() > 0 ) {
        props.set ( "Top", Top );
    }
	try {
	    // This will warn the user...
		__command.checkCommandParameters ( props, null, 1 );
	}
	catch ( Exception e ) {
        Message.printWarning(2,"", e);
		// The warning would have been printed in the check code.
		__error_wait = true;
	}
}

/**
Commit the edits to the command.  In this case the command parameters have
already been checked and no errors were detected.
*/
private void commitEdits ()
{	String TableID = __TableID_JTextField.getText().trim();
    String InputFile = __InputFile_JTextField.getText().trim();
	String ArrayName = __ArrayName_JTextField.getText().trim();
    String ExcludeNames  = __ExcludeNames_JTextField.getText().trim();
	String ArrayColumns  = __ArrayColumns_JTextField.getText().trim();
	String BooleanColumns  = __BooleanColumns_JTextField.getText().trim();
	String DateTimeColumns  = __DateTimeColumns_JTextField.getText().trim();
	String DoubleColumns  = __DoubleColumns_JTextField.getText().trim();
	String IntegerColumns  = __IntegerColumns_JTextField.getText().trim();
	String TextColumns  = __TextColumns_JTextField.getText().trim();
	String Top  = __Top_JTextField.getText().trim();
    __command.setCommandParameter ( "TableID", TableID );
	__command.setCommandParameter ( "InputFile", InputFile );
	__command.setCommandParameter ( "ArrayName", ArrayName );
	__command.setCommandParameter ( "ExcludeNames", ExcludeNames );
	__command.setCommandParameter ( "ArrayColumns", ArrayColumns );
	__command.setCommandParameter ( "BooleanColumns", BooleanColumns );
	__command.setCommandParameter ( "DateTimeColumns", DateTimeColumns );
	__command.setCommandParameter ( "DoubleColumns", DoubleColumns );
	__command.setCommandParameter ( "IntegerColumns", IntegerColumns );
	__command.setCommandParameter ( "TextColumns", TextColumns );
	__command.setCommandParameter ( "Top", Top );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit and possibly run.
*/
private void initialize ( JFrame parent, ReadTableFromJSON_Command command )
{	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)processor, __command );

	addWindowListener(this);

    Insets insetsTLBR = new Insets(1,2,1,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout(new GridBagLayout());
	getContentPane().add ("North", main_JPanel);
	int y = -1;

	JPanel paragraph = new JPanel();
	paragraph.setLayout(new GridBagLayout());
	int yy = -1;
    
   	JGUIUtil.addComponent(paragraph, new JLabel (
		"This command reads a table from a JavaScript Object Notation (JSON) file by processing an array of objects."),
		0, ++yy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(paragraph, new JLabel (
		"See the following JSON reference:  http://www.json.org/"),
		0, ++yy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
		"The JSON array name indicates how to locate the data in the JSON hierarchy."), 
		0, ++yy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "If the array name is not specified, an unnamed array is expected to be found in the input file."),
        0, ++yy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "An example JSON file is shown below with a list of ojects within the main array, and name/value pairs for each object:"),
        0, ++yy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "<html><pre>\n" +
        "[{\"CountyName\":\"Adams\",\"Huc8\":\"\",\"Huc12\":\"\",\"MonitoringLocationIdentifier\":\"NFE\",\"Latitude\":\"39.812806\",\"Longitude\":\"-104.954333\"},\n" +
        "{\"CountyName\":\"Adams\",\"Huc8\":\"\",\"Huc12\":\"\",\"MonitoringLocationIdentifier\":\"SFE\",\"Latitude\":\"39.812772\",\"Longitude\":\"-104.95444\"}]</pre></html>"),
        0, ++yy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "The object list must be contained in an array and each object must contain simple name/value pairs."),
        0, ++yy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "Each name/value within a JSON object will be converted to a table column."),
        0, ++yy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "The value type will automatically be determined based on standard JSON conventions and also can be specified using command parameters."),
        0, ++yy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
		"It is recommended that the location of the files be " +
		"specified using a path relative to the working directory."),
		0, ++yy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);			
	if (__working_dir != null) {
    	JGUIUtil.addComponent(paragraph, new JLabel (
		"The working directory is: " + __working_dir), 
		0, ++yy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}

	JGUIUtil.addComponent(main_JPanel, paragraph,
		0, ++y, 8, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++y, 8, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Table ID:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JTextField = new JTextField (20);
    __TableID_JTextField.setToolTipText("Specify the table ID or use ${Property} notation");
    __TableID_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __TableID_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - unique identifier for the table."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Input file:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputFile_JTextField = new JTextField (35);
	__InputFile_JTextField.setToolTipText("Specify the path to the file to read or use ${Property} notation");
	__InputFile_JTextField.addKeyListener (this);
    // Input file layout fights back with other rows so put in its own panel
	JPanel InputFile_JPanel = new JPanel();
	InputFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(InputFile_JPanel, __InputFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ( "...", this );
	__browse_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(InputFile_JPanel, __browse_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__path_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(InputFile_JPanel, __path_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(main_JPanel, InputFile_JPanel,
		1, y, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("JSON array name:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ArrayName_JTextField = new JTextField (20);
    __ArrayName_JTextField.setToolTipText("Array name to read, for example from:  \"arrayName\" : [ { objects ... } ]");
    __ArrayName_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __ArrayName_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel,
        new JLabel ("Optional - JSON array name containing objects to read."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Exclude names:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ExcludeNames_JTextField = new JTextField (20);
    __ExcludeNames_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __ExcludeNames_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel,
        new JLabel ("Optional - JSON names to exclude, separated by commas."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
   	
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Array columns:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ArrayColumns_JTextField = new JTextField (20);
    __ArrayColumns_JTextField.setToolTipText("Simple array data (not arrays of objects) can be saved as a table column.");
    __ArrayColumns_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __ArrayColumns_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel,
        new JLabel ("Optional - columns that contain simple array data, separated by commas."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Boolean columns:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __BooleanColumns_JTextField = new JTextField (20);
    __BooleanColumns_JTextField.setToolTipText("Text data can be converted to boolean values.");
    __BooleanColumns_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __BooleanColumns_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel,
        new JLabel ("Optional - columns that contain boolean values, separated by commas."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Date/time columns:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DateTimeColumns_JTextField = new JTextField (20);
    __DateTimeColumns_JTextField.setToolTipText("Text data can be converted to date/time values.");
    __DateTimeColumns_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __DateTimeColumns_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel,
        new JLabel ("Optional - columns that contain date/times, separated by commas."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Double columns:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DoubleColumns_JTextField = new JTextField (20);
    __DoubleColumns_JTextField.setToolTipText("Text data can be converted to double values.");
    __DoubleColumns_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __DoubleColumns_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel,
        new JLabel ("Optional - columns that contain double (floating point) values, separated by commas."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Integer columns:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IntegerColumns_JTextField = new JTextField (20);
    __IntegerColumns_JTextField.setToolTipText("Text data can be converted to integer values.");
    __IntegerColumns_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __IntegerColumns_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel,
        new JLabel ("Optional - columns that contain integer values, separated by commas."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Text columns:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TextColumns_JTextField = new JTextField (20);
    __TextColumns_JTextField.setToolTipText("Non-text data can be converted to text (string) values.");
    __TextColumns_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __TextColumns_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel,
        new JLabel ("Optional - columns that contain text, separated by commas."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Top N objects:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Top_JTextField = new JTextField (5);
    __Top_JTextField.setToolTipText("Read top N rows - all rows will be checked to determine default column types.");
    __Top_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __Top_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel,
        new JLabel ("Optional - only process top N objects."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Command:"), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (4,40);
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable (false);
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
	refresh ();

	// South JPanel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__ok_JButton = new SimpleJButton("OK", this);
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add (__ok_JButton);
	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add (__cancel_JButton);
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + " Command");
    pack();
    JGUIUtil.center(this);
	refresh();	// Sets the __path_JButton status
	setResizable (false);
    super.setVisible(true);
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged (ItemEvent e) {
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
		if (!__error_wait) {
			response ( true );
		}
	}
}

public void keyReleased (KeyEvent event) {
	refresh();
}

public void keyTyped (KeyEvent event) {}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user canceled.
*/
public boolean ok ()
{	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String TableID = "";
    String InputFile = "";
    String ArrayName = "";
    String ExcludeNames = "";
	String ArrayColumns = "";
	String BooleanColumns = "";
	String DateTimeColumns = "";
	String DoubleColumns = "";
	String IntegerColumns = "";
	String TextColumns = "";
	String Top = "";
	PropList props = __command.getCommandParameters();
	if (__first_time) {
		__first_time = false;
        TableID = props.getValue ( "TableID" );
		InputFile = props.getValue ( "InputFile" );
		ArrayName = props.getValue ( "ArrayName" );
		ExcludeNames = props.getValue ( "ExcludeNames" );
		ArrayColumns = props.getValue ( "ArrayColumns" );
		BooleanColumns = props.getValue ( "BooleanColumns" );
		DateTimeColumns = props.getValue ( "DateTimeColumns" );
		DoubleColumns = props.getValue ( "DoubleColumns" );
		IntegerColumns = props.getValue ( "IntegerColumns" );
		TextColumns = props.getValue ( "TextColumns" );
		Top = props.getValue ( "Top" );
        if ( TableID != null ) {
            __TableID_JTextField.setText ( TableID );
        }
		if ( InputFile != null ) {
			__InputFile_JTextField.setText ( InputFile );
		}
		if ( ArrayName != null ) {
			__ArrayName_JTextField.setText ( ArrayName );
		}
        if ( ExcludeNames != null ) {
            __ExcludeNames_JTextField.setText ( ExcludeNames );
        }
        if ( ArrayColumns != null ) {
            __ArrayColumns_JTextField.setText ( ArrayColumns );
        }
        if ( BooleanColumns != null ) {
            __BooleanColumns_JTextField.setText ( BooleanColumns );
        }
        if ( DateTimeColumns != null ) {
            __DateTimeColumns_JTextField.setText ( DateTimeColumns );
        }
        if ( DoubleColumns != null ) {
            __DoubleColumns_JTextField.setText ( DoubleColumns );
        }
        if ( IntegerColumns != null ) {
            __IntegerColumns_JTextField.setText ( IntegerColumns );
        }
        if ( TextColumns != null ) {
            __TextColumns_JTextField.setText ( TextColumns );
        }
        if ( Top != null ) {
            __Top_JTextField.setText ( Top );
        }
	}
	// Regardless, reset the command from the fields...
    TableID = __TableID_JTextField.getText().trim();
	InputFile = __InputFile_JTextField.getText().trim();
	ArrayName = __ArrayName_JTextField.getText().trim();
	ExcludeNames = __ExcludeNames_JTextField.getText().trim();
	ArrayColumns = __ArrayColumns_JTextField.getText().trim();
	BooleanColumns = __BooleanColumns_JTextField.getText().trim();
	DateTimeColumns = __DateTimeColumns_JTextField.getText().trim();
	DoubleColumns = __DoubleColumns_JTextField.getText().trim();
	IntegerColumns = __IntegerColumns_JTextField.getText().trim();
	TextColumns = __TextColumns_JTextField.getText().trim();
	Top = __Top_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
    props.add ( "TableID=" + TableID );
	props.add ( "InputFile=" + InputFile );
	props.add ( "ArrayName=" + ArrayName );
	props.add ( "ExcludeNames=" + ExcludeNames );
	props.add ( "ArrayColumns=" + ArrayColumns );
	props.add ( "BooleanColumns=" + BooleanColumns );
	props.add ( "DateTimeColumns=" + DateTimeColumns );
	props.add ( "DoubleColumns=" + DoubleColumns );
	props.add ( "IntegerColumns=" + IntegerColumns );
	props.add ( "TextColumns=" + TextColumns );
	props.add ( "Top=" + Top );
	__command_JTextArea.setText( __command.toString ( props ) );
	// Check the path and determine what the label on the path button should be...
	if ( __path_JButton != null ) {
		if ( (InputFile != null) && !InputFile.isEmpty() ) {
			__path_JButton.setEnabled ( true );
			File f = new File ( InputFile );
			if ( f.isAbsolute() ) {
				__path_JButton.setText ( __RemoveWorkingDirectory );
				__path_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
		    	__path_JButton.setText ( __AddWorkingDirectory );
		    	__path_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__path_JButton.setEnabled(false);
		}
	}
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
*/
private void response ( boolean ok )
{	__ok = ok;	// Save to be returned by ok()
	if ( ok ) {
		// Commit the changes...
		commitEdits ();
		if ( __error_wait ) {
			// Not ready to close out!
			return;
		}
	}
	// Now close out...
	setVisible( false );
	dispose();
}

/**
Responds to WindowEvents.
@param event WindowEvent object 
*/
public void windowClosing(WindowEvent event) {
	response ( false );
}

// The following methods are all necessary because this class implements WindowListener
public void windowActivated(WindowEvent evt)	{}
public void windowClosed(WindowEvent evt)	{}
public void windowDeactivated(WindowEvent evt)	{}
public void windowDeiconified(WindowEvent evt)	{}
public void windowIconified(WindowEvent evt)	{}
public void windowOpened(WindowEvent evt)	{}

}
