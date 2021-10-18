// ReadStateModB_JDialog - editor dialog for ReadStateModB command

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

package rti.tscommandprocessor.commands.statemod;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFileChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import RTi.TS.TSFormatSpecifiersJPanel;
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

@SuppressWarnings("serial")
public class ReadStateModB_JDialog extends JDialog
implements ActionListener, DocumentListener, KeyListener, WindowListener
{
	
	private final String __AddWorkingDirectory = "Abs";
	private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __browse_JButton = null; // File browse button
private SimpleJButton __cancel_JButton = null; // Cancel Button
private SimpleJButton __ok_JButton = null; // Ok Button
private SimpleJButton __help_JButton = null;
private SimpleJButton __path_JButton = null; // Convert between relative and absolute paths.
private ReadStateModB_Command __command = null; // Command to edit
private JTextArea __command_JTextArea=null;// Command as TextField
private String __working_dir = null;	// Working directory.
private JTextField __InputFile_JTextField = null;// Field for input file. 
private JTextField __InputStart_JTextField = null;
private JTextField __InputEnd_JTextField = null;
private JTextField __TSID_JTextField = null;
private JTextField __IncludeDataTypes_JTextField = null;
private JTextField __ExcludeDataTypes_JTextField = null;
private JTextField __Version_JTextField = null;
private SimpleJComboBox __OutputVersion_JComboBox = null;
private TSFormatSpecifiersJPanel __Alias_JTextField = null; // Alias for time series.
private SimpleJComboBox	__IfFileNotFound_JComboBox =null;
private boolean __error_wait = false; // Is there an error waiting to be cleared up or Cancel?
private boolean __first_time = true;
// TODO SAM 2007-02-18 Evaluate whether to support alias
//private boolean		__use_alias = false;	// If true, then the syntax is
						// TS Alias = readStateMod().
						// If false, it is:
						// readStateMod().
private boolean __ok = false; // Indicates whether OK was pressed when closing the dialog.

/**
Command editor constructor.
@param parent Frame class instantiating this class.
@param command Command to edit.
*/
public ReadStateModB_JDialog ( JFrame parent, ReadStateModB_Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();

	if ( o == __browse_JButton ) {
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
		}
		else {
		    fc = JFileChooserFactory.createJFileChooser( __working_dir );
		}
		fc.setDialogTitle( "Select StateMod Time Series File");
		// TODO - maybe need to list all recognized StateMod file extensions for data sets.
		SimpleFileFilter sff = new SimpleFileFilter("b43", "Diversion, Stream, Instream stations (Monthly)");
		fc.addChoosableFileFilter( sff );
		fc.addChoosableFileFilter( new SimpleFileFilter("b49", "Diversion, Stream, Instream stations (Daily)") );
		fc.addChoosableFileFilter( new SimpleFileFilter("b44", "Reservoir stations (Monthly)") );
		fc.addChoosableFileFilter( new SimpleFileFilter("b50", "Reservoir stations (Daily)") );
		fc.addChoosableFileFilter( new SimpleFileFilter("b42", "Well stations (Monthly)") );
		fc.addChoosableFileFilter( new SimpleFileFilter("b65", "Well stations (Daily)") );
		fc.setFileFilter(sff);
		
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
					__InputFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1,"ReadStateMod_JDialog", "Error converting file to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory(directory);
				refresh();
			}
		}
	}
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "ReadStateModB");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else if ( o == __path_JButton ) {
		if ( __path_JButton.getText().equals( __AddWorkingDirectory) ) {
			__InputFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir, __InputFile_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals( __RemoveWorkingDirectory) ) {
			try {
			    __InputFile_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir, __InputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,
				"readStateModB_JDialog", "Error converting file to relative path." );
			}
		}
		refresh ();
	}
	else {
		// Choices...
		refresh();
	}
}

// Start event handlers for DocumentListener...

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void changedUpdate ( DocumentEvent e )
{
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void insertUpdate ( DocumentEvent e )
{
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void removeUpdate ( DocumentEvent e )
{
    refresh();
}

// ...End event handlers for DocumentListener

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	String InputFile = __InputFile_JTextField.getText().trim();
	String TSID = __TSID_JTextField.getText().trim();
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
    String IncludeDataTypes = __IncludeDataTypes_JTextField.getText().trim();
    String ExcludeDataTypes = __ExcludeDataTypes_JTextField.getText().trim();
	String Version = __Version_JTextField.getText().trim();
	String OutputVersion = __OutputVersion_JComboBox.getSelected();
	String Alias = __Alias_JTextField.getText().trim();
	String IfFileNotFound = __IfFileNotFound_JComboBox.getSelected();
	__error_wait = false;
	if ( InputFile.length() > 0 ) {
		props.set ( "InputFile", InputFile );
	}
	if ( TSID.length() > 0 ) {
		props.set ( "TSID", TSID );
	}
	if ( InputStart.length() > 0 ) {
		props.set ( "InputStart", InputStart );
	}
	if ( InputEnd.length() > 0 ) {
		props.set ( "InputEnd", InputEnd );
	}
    if ( IncludeDataTypes.length() > 0 ) {
        props.set ( "IncludeDataTypes", IncludeDataTypes );
    }
    if ( ExcludeDataTypes.length() > 0 ) {
        props.set ( "ExcludeDataTypes", ExcludeDataTypes );
    }
	if ( Version.length() > 0 ) {
		props.set ( "Version", Version );
	}
	if ( OutputVersion.length() > 0 ) {
		props.set ( "OutputVersion", OutputVersion );
	}
    if (Alias.length() > 0) {
        props.set("Alias", Alias);
    }
    if (IfFileNotFound.length() > 0) {
        props.set("IfFileNotFound", IfFileNotFound);
    }
	try {
	    // This will warn the user...
		__command.checkCommandParameters ( props, null, 1 );
	}
	catch ( Exception e ) {
		// The warning would have been printed in the check code.
		__error_wait = true;
	}
}

/**
Commit the edits to the command.  In this case the command parameters have
already been checked and no errors were detected.
*/
private void commitEdits ()
{	String InputFile = __InputFile_JTextField.getText().trim();
	String TSID = __TSID_JTextField.getText().trim();
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
    String IncludeDataTypes = __IncludeDataTypes_JTextField.getText().trim();
    String ExcludeDataTypes = __ExcludeDataTypes_JTextField.getText().trim();
	String Version = __Version_JTextField.getText().trim();
	String OutputVersion = __OutputVersion_JComboBox.getSelected();
	String Alias = __Alias_JTextField.getText().trim();
	String IfFileNotFound = __IfFileNotFound_JComboBox.getSelected();
	__command.setCommandParameter ( "InputFile", InputFile );
	__command.setCommandParameter ( "TSID", TSID );
	__command.setCommandParameter ( "InputStart", InputStart );
	__command.setCommandParameter ( "InputEnd", InputEnd );
	__command.setCommandParameter ( "IncludeDataTypes", IncludeDataTypes );
	__command.setCommandParameter ( "ExcludeDataTypes", ExcludeDataTypes );
	__command.setCommandParameter ( "Version", Version );
	__command.setCommandParameter ( "OutputVersion", OutputVersion );
	__command.setCommandParameter ( "Alias", Alias );
	__command.setCommandParameter ( "IfFileNotFound", IfFileNotFound );
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, ReadStateModB_Command command )
{	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel.

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Read time series from a StateMod binary output file, "+
		"using information in the file to assign the identifier."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    //JGUIUtil.addComponent(main_JPanel, new JLabel (
    //    "Due to the large number of time series in StateMod binary files, the list of time series " +
    //    "identifiers in the file will NOT be available in other command editors." ), 
    //    0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify a full or relative path (relative to working directory)." ), 
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel ( "The working directory is:"),
		    0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel ( "  " + __working_dir ),
		    0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The time series identifier pattern, if specified, will filter the read." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"  Use blank or * to read all time series." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"  Use A* to read all time series location starting with A." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"  Use *.*.XXXXX.*.* to read all time series with data type XXXXX." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"  Currently, data source, interval, and scenario are internally defaulted to *." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("StateMod binary file to read:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputFile_JTextField = new JTextField ( 20 );
	__InputFile_JTextField.addKeyListener ( this );
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

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Time series ID:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TSID_JTextField = new JTextField ( 10 );
	__TSID_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __TSID_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel("Optional - specify a TSID pattern to match (default is all)."), 
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Input start:"), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputStart_JTextField = new JTextField (20);
	__InputStart_JTextField.addKeyListener (this);
        JGUIUtil.addComponent(main_JPanel, __InputStart_JTextField,
		1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - override the global input start."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input end:"), 
		0, ++y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputEnd_JTextField = new JTextField (20);
	__InputEnd_JTextField.addKeyListener (this);
        JGUIUtil.addComponent(main_JPanel, __InputEnd_JTextField,
		1, y, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - override the global input end."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data types to include:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IncludeDataTypes_JTextField = new JTextField ( 20 );
    __IncludeDataTypes_JTextField.setToolTipText("Indicate StateMod data types (parameters) to include, separated by commas.");
    __IncludeDataTypes_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __IncludeDataTypes_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - data types to include (default=all)."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data types to exclude:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ExcludeDataTypes_JTextField = new JTextField ( 20 );
    __ExcludeDataTypes_JTextField.setToolTipText("Indicate StateMod data types (parameters) to exclude, separated by commas.");
    __ExcludeDataTypes_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __ExcludeDataTypes_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - data types to exclude (default=none)."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "StateMod version:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Version_JTextField = new JTextField ( 10 );
	__Version_JTextField.setToolTipText("Version of the input file, for StateMod version <= 10 because cannot determine the version from file contents." );
	__Version_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __Version_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - NN.NN, for files prior to StateMod version 11 (default is current version)."), 
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output version:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputVersion_JComboBox = new SimpleJComboBox ( false );
    __OutputVersion_JComboBox.setToolTipText("Version for output, used to translate to a specific version.");
    List<String> versionChoices = new ArrayList<>();
    versionChoices.add ( "" );
    versionChoices.add ( __command._Original );
    versionChoices.add ( __command._Latest );
    __OutputVersion_JComboBox.setData(versionChoices);
    __OutputVersion_JComboBox.select ( 0 );
    __OutputVersion_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __OutputVersion_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - output version (default=" + __command._Original + ")"), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(10);
    __Alias_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener(this);
    __Alias_JTextField.setToolTipText("%L for location, %T for data type.");
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - use %L for location, etc. (default=no alias)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

   JGUIUtil.addComponent(main_JPanel, new JLabel ( "If file not found?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IfFileNotFound_JComboBox = new SimpleJComboBox ( false );
	List<String> notFoundChoices = new ArrayList<>();
	notFoundChoices.add ( "" );	// Default
	notFoundChoices.add ( __command._Ignore );
	notFoundChoices.add ( __command._Warn );
	notFoundChoices.add ( __command._Fail );
	__IfFileNotFound_JComboBox.setData(notFoundChoices);
	__IfFileNotFound_JComboBox.select ( 0 );
	__IfFileNotFound_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(main_JPanel, __IfFileNotFound_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - action if file not found (default=" + __command._Warn + ")."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 55 );
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__ok_JButton = new SimpleJButton("OK", this);
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add ( __ok_JButton );
	__cancel_JButton = new SimpleJButton("Cancel", this);
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __cancel_JButton );
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + " Command" );
    pack();
    JGUIUtil.center( this );
	refresh();	// Sets the __path_JButton status
	// Dialogs do not need to be resizable...
	setResizable ( false );
    super.setVisible( true );
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh ();
		checkInput ();
		if ( !__error_wait ) {
			response ( false );
		}
	}
}

public void keyReleased ( KeyEvent event )
{	refresh();
}

public void keyTyped ( KeyEvent event )
{	refresh();
}

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
{	String routine = getClass().getSimpleName() + ".routine";
	String InputFile="";
	String TSID="";
	String InputStart = "";
	String InputEnd = "";
    String IncludeDataTypes = "";
    String ExcludeDataTypes = "";
	String Version="";
	String OutputVersion = "";
    String Alias = "";
    String IfFileNotFound = "";
	PropList props = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		props = __command.getCommandParameters();
		InputFile = props.getValue ( "InputFile" );
		TSID = props.getValue ( "TSID" );
		InputStart = props.getValue ( "InputStart" );
		InputEnd = props.getValue ( "InputEnd" );
		IncludeDataTypes = props.getValue ( "IncludeDataTypes" );
		ExcludeDataTypes = props.getValue ( "ExcludeDataTypes" );
		Version = props.getValue ( "Version" );
		OutputVersion = props.getValue ( "OutputVersion" );
        Alias = props.getValue("Alias");
        IfFileNotFound = props.getValue("IfFileNotFound");
		if ( InputFile != null ) {
			__InputFile_JTextField.setText (InputFile);
		}
		if ( TSID != null ) {
			__TSID_JTextField.setText (TSID);
		}
		if ( InputStart != null ) {
			__InputStart_JTextField.setText ( InputStart );
		}
		if ( InputEnd != null ) {
			__InputEnd_JTextField.setText ( InputEnd );
		}
		if ( IncludeDataTypes != null ) {
			__IncludeDataTypes_JTextField.setText (IncludeDataTypes);
		}
		if ( ExcludeDataTypes != null ) {
			__ExcludeDataTypes_JTextField.setText (ExcludeDataTypes);
		}
		if ( Version != null ) {
			__Version_JTextField.setText (Version);
		}
        if ( JGUIUtil.isSimpleJComboBoxItem(__OutputVersion_JComboBox, OutputVersion, JGUIUtil.NONE, null, null ) ) {
            __OutputVersion_JComboBox.select ( OutputVersion );
        }
        else {
            if ( (OutputVersion == null) || OutputVersion.equals("") ) {
                // New command...select the default...
                __OutputVersion_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                "OutputVersion parameter \"" + OutputVersion + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        if (Alias != null ) {
            __Alias_JTextField.setText(Alias.trim());
        }
		if ( JGUIUtil.isSimpleJComboBoxItem(__IfFileNotFound_JComboBox, IfFileNotFound,JGUIUtil.NONE, null, null ) ) {
			__IfFileNotFound_JComboBox.select ( IfFileNotFound );
		}
		else {
            if ( (IfFileNotFound == null) ||	IfFileNotFound.equals("") ) {
				// New command...select the default...
				__IfFileNotFound_JComboBox.select ( 0 );
			}
			else {	// Bad user command...
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"IfFileNotFound parameter \"" +	IfFileNotFound +
				"\".  Select a\n value or Cancel." );
			}
		}
	}
	// Regardless, reset the command from the fields...
	InputFile = __InputFile_JTextField.getText().trim();
	TSID = __TSID_JTextField.getText().trim();
	InputStart = __InputStart_JTextField.getText().trim();
	InputEnd = __InputEnd_JTextField.getText().trim();
	IncludeDataTypes = __IncludeDataTypes_JTextField.getText().trim();
	ExcludeDataTypes = __ExcludeDataTypes_JTextField.getText().trim();
	Version = __Version_JTextField.getText().trim();
	OutputVersion = __OutputVersion_JComboBox.getSelected();
	Alias = __Alias_JTextField.getText().trim();
	IfFileNotFound = __IfFileNotFound_JComboBox.getSelected();
	props = new PropList ( __command.getCommandName() );
	props.add ( "InputFile=" + InputFile );
	props.add ( "TSID=" + TSID );
	props.add ( "InputStart=" + InputStart );
	props.add ( "InputEnd=" + InputEnd );
	props.add ( "IncludeDataTypes=" + IncludeDataTypes );
	props.add ( "ExcludeDataTypes=" + ExcludeDataTypes );
	props.add ( "Version=" + Version );
	props.add ( "OutputVersion=" + OutputVersion );
	props.add ( "Alias=" + Alias );
	props.add ( "IfFileNotFound=" + IfFileNotFound );
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
@param ok if false, then the edit is canceled.  If true, the edit is committed
and the dialog is closed.
*/
private void response ( boolean ok )
{	__ok = ok;	// Save to be returned by ok().
	if ( ok ) {
		// Commit the changes.
		commitEdits ();
		if ( __error_wait ) {
			// Not ready to close out.
			return;
		}
	}
	// Now close out.
	setVisible( false );
	dispose();
}

/**
Responds to WindowEvents.
@param event WindowEvent object
*/
public void windowClosing( WindowEvent event )
{	response ( false );
}

public void windowActivated( WindowEvent evt ){;}
public void windowClosed( WindowEvent evt ){;}
public void windowDeactivated( WindowEvent evt ){;}
public void windowDeiconified( WindowEvent evt ){;}
public void windowIconified( WindowEvent evt ){;}
public void windowOpened( WindowEvent evt ){;}

}