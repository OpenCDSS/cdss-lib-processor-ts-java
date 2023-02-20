// CreateFromList_JDialog - Editor for the CreateFromList() command.

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

package rti.tscommandprocessor.commands.ts;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.Command;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Editor for the CreateFromList() command.
*/
@SuppressWarnings("serial")
public class CreateFromList_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
	
	private final String __AddWorkingDirectory = "Abs";
	private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __browse_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SimpleJButton __path_JButton = null;	// Convert between relative and absolute paths
private CreateFromList_Command __command = null;
private String __working_dir = null;	// Working directory.
private JTextArea __command_JTextArea=null;
private JTextField __ListFile_JTextField = null;
private JTextField __ID_JTextField = null;	// IDs to process file
private SimpleJComboBox	__IDCol_JComboBox = null; // ID column in file
private JTextField __DataSource_JTextField = null; // Field for time series data source.
private JTextField __DataType_JTextField = null; // Field for time series data type.
private JTextField __Interval_JTextField = null; // Data Interval.
private JTextField __Scenario_JTextField = null; // Scenario.
private JTextField __InputType_JTextField = null; // Input type.
private JTextField __InputName_JTextField = null; // Input name.
private JTextField __Delim_JTextField = null; // Delimiter character(s).
private SimpleJComboBox	__IfNotFound_JComboBox = null;
private JTextField __DefaultUnits_JTextField = null; // Default units when blank time series is created.
private boolean __error_wait = false;	// Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public CreateFromList_JDialog (	JFrame parent, Command command )
{	super ( parent, true );
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
		fc.setDialogTitle( "Select List File");
		SimpleFileFilter sff = new SimpleFileFilter("txt", "List File");
		fc.addChoosableFileFilter(sff);
		sff = new SimpleFileFilter("csv", "List File");
		fc.addChoosableFileFilter(sff);
		
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
					__ListFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1,"CreateFromList_JDialog", "Error converting file to relative path." );
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
		HelpViewer.getInstance().showHelp("command", "CreateFromList");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else if ( o == __path_JButton ) {
		if (__path_JButton.getText().equals(__AddWorkingDirectory) ) {
			__ListFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir,__ListFile_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
				__ListFile_JTextField.setText (IOUtil.toRelativePath ( __working_dir,__ListFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,"createFromList_JDialog","Error converting file to relative path." );
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
{   // Put together a list of parameters to check...
    PropList parameters = new PropList ( "" );
    String ListFile = __ListFile_JTextField.getText().trim();
    String IDCol = __IDCol_JComboBox.getSelected();
    String Delim = __Delim_JTextField.getText().trim();
    String ID = __ID_JTextField.getText().trim();
    String DataSource = __DataSource_JTextField.getText().trim();
    String DataType = __DataType_JTextField.getText().trim();
    String Interval = __Interval_JTextField.getText().trim();
    String Scenario = __Scenario_JTextField.getText().trim();
    String InputType = __InputType_JTextField.getText().trim();
    String InputName = __InputName_JTextField.getText().trim();
    String IfNotFound = __IfNotFound_JComboBox.getSelected();
    String DefaultUnits = __DefaultUnits_JTextField.getText().trim();
    
    __error_wait = false;

    if ( ListFile.length() > 0 ) {
        parameters.set ( "ListFile", ListFile );
    }
    if ( IDCol.length() > 0 ) {
        parameters.set ( "IDCol", IDCol );
    }
    if ( Delim.length() > 0 ) {
        parameters.set ( "Delim", Delim );
    }
    if ( ID.length() > 0 ) {
        parameters.set ( "ID", ID );
    }
    if ( DataSource.length() > 0 ) {
        parameters.set ( "DataSource", DataSource );
    }
    if ( DataType.length() > 0 ) {
        parameters.set ( "DataType", DataType );
    }
    if ( Interval.length() > 0 ) {
        parameters.set ( "Interval", Interval );
    }
    if ( Scenario.length() > 0 ) {
        parameters.set ( "Scenario", Scenario );
    }
    if ( InputType.length() > 0 ) {
        parameters.set ( "InputType", InputType );
    }
    if ( InputName.length() > 0 ) {
        parameters.set ( "InputName", InputName );
    }
    if ( IfNotFound.length() > 0 ) {
        parameters.set ( "IfNotFound", IfNotFound );
    }
    if ( DefaultUnits.length() > 0 ) {
        parameters.set ( "DefaultUnits", DefaultUnits );
    }
    try {   // This will warn the user...
        __command.checkCommandParameters ( parameters, null, 1 );
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
{   String ListFile = __ListFile_JTextField.getText().trim();
    String IDCol = __IDCol_JComboBox.getSelected();
    String Delim = __Delim_JTextField.getText().trim();
    String ID = __ID_JTextField.getText().trim();
    String DataSource = __DataSource_JTextField.getText().trim();
    String DataType = __DataType_JTextField.getText().trim();
    String Interval = __Interval_JTextField.getText().trim();
    String Scenario = __Scenario_JTextField.getText().trim();
    String InputType = __InputType_JTextField.getText().trim();
    String InputName = __InputName_JTextField.getText().trim();
    String IfNotFound = __IfNotFound_JComboBox.getSelected();
    String DefaultUnits = __DefaultUnits_JTextField.getText().trim();
    __command.setCommandParameter ( "ListFile", ListFile );
    __command.setCommandParameter ( "IDCol", IDCol );
    __command.setCommandParameter ( "Delim", Delim );
    __command.setCommandParameter ( "ID", ID );
    __command.setCommandParameter ( "DataSource", DataSource );
    __command.setCommandParameter ( "DataType", DataType );
    __command.setCommandParameter ( "Interval", Interval );
    __command.setCommandParameter ( "Scenario", Scenario );
    __command.setCommandParameter ( "InputType", InputType );
    __command.setCommandParameter ( "InputName", InputName );
    __command.setCommandParameter ( "IfNotFound", IfNotFound );
    __command.setCommandParameter ( "DefaultUnits", DefaultUnits );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Command command )
{	__command = (CreateFromList_Command)command;
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)__command.getCommandProcessor(), __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Create a list of time series from a list of location identifiers in a file." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The information specified below is used with the identifiers" +
		" to create time series identifiers,"),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"which are then used to read the time series.  The identifiers are of the form:"),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"  ID.DataSource.DataType.Interval.Scenario~InputType~InputName"),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command is useful for automating time series creation " +
		"where lists of identifiers are being processed."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The list file can contain comment lines starting with #." ), 
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"It is recommended that the path to the file be specified using a relative path."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The working directory is: " + __working_dir ), 
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
          0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("List file to read:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ListFile_JTextField = new JTextField ( 50 );
	__ListFile_JTextField.addKeyListener ( this );
    // Input file layout fights back with other rows so put in its own panel
	JPanel ListFile_JPanel = new JPanel();
	ListFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(ListFile_JPanel, __ListFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ( "...", this );
	__browse_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(ListFile_JPanel, __browse_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__path_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(ListFile_JPanel, __path_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(main_JPanel, ListFile_JPanel,
		1, y, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "ID column:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IDCol_JComboBox = new SimpleJComboBox ( false );
	List<String> IDCol_Vector = new Vector<String> ( 100 );
	for ( int i = 1; i < 101; i++ ) {
		IDCol_Vector.add ( "" + i );
	}
	__IDCol_JComboBox.setData ( IDCol_Vector );
	__IDCol_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __IDCol_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Required - the ID column in the list file (1+)."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Delimiter:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Delim_JTextField = new JTextField ( "", 20 );
	__Delim_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __Delim_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - delimiter(s) between data columns (default is \" ,\")."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "ID filter pattern:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ID_JTextField = new JTextField ( "", 20 );
	__ID_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __ID_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - IDs to use from list file (default is all). For example, use X*."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data source:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DataSource_JTextField = new JTextField ( "", 20 );
	__DataSource_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataSource_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional or required depending on input type."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data type:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DataType_JTextField = new JTextField ( "", 20 );
	__DataType_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataType_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional or required depending on input type."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data interval:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Interval_JTextField = new JTextField ( "", 20 );
	__Interval_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Interval_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Required - for example, 5Minute, 6Hour, Day, Month, Year, or Irregular."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Scenario:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Scenario_JTextField = new JTextField ( "", 20 );
	__Scenario_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __Scenario_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input type:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputType_JTextField = new JTextField ( "", 20 );
	__InputType_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __InputType_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required - needed to identify format of input (e.g., HydroBase)."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input name:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputName_JTextField = new JTextField ( "", 20 );
	__InputName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __InputName_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional (e.g., use for file name for input type)."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel,new JLabel("If time series not found?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IfNotFound_JComboBox = new SimpleJComboBox ( false );
	List<String> notFoundChoices = new ArrayList<String>();
	notFoundChoices.add ( __command._Default );
	notFoundChoices.add ( __command._Ignore );
	notFoundChoices.add ( __command._Warn );
	notFoundChoices.add ( __command._Fail );
	__IfNotFound_JComboBox.setData(notFoundChoices);
	__IfNotFound_JComboBox.select ( __command._Warn );
	__IfNotFound_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __IfNotFound_JComboBox,
		1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Required - how to handle time series that are not found."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Default units:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DefaultUnits_JTextField = new JTextField ( "", 20 );
    __DefaultUnits_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DefaultUnits_JTextField,
    1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
    "Optional - units when IfNotFound=" + __command._Default + "."),
    3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 4, 55 );
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__ok_JButton = new SimpleJButton("OK", this);
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add ( __ok_JButton );
	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + " Command" );
    pack();
    JGUIUtil.center( this );
	refresh();	// Sets the __path_JButton status
	setResizable ( false );
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent e )
{	refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

    refresh();
	if ( code == KeyEvent.VK_ENTER ) {
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
}

public void keyReleased ( KeyEvent event )
{	refresh();
}

public void keyTyped ( KeyEvent event ) {;}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user cancelled.
*/
public boolean ok ()
{   return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{   String routine = "CreateFromList_JDialog.refresh";
    String ListFile = "";
    String IDCol = "";
    String Delim = "";
    String ID = "";
    String DataSource = "";
    String DataType = "";
    String Interval = "";
    String Scenario = "";
    String InputType = "";
    String InputName = "";
    String IfNotFound = "";
    String DefaultUnits = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        ListFile = props.getValue ( "ListFile" );
        IDCol = props.getValue ( "IDCol" );
        Delim = props.getValue ( "Delim" );
        ID = props.getValue ( "ID" );
        DataSource = props.getValue ( "DataSource" );
        DataType = props.getValue ( "DataType" );
        Interval = props.getValue ( "Interval" );
        Scenario = props.getValue ( "Scenario" );
        InputType = props.getValue ( "InputType" );
        InputName = props.getValue ( "InputName" );
        IfNotFound = props.getValue ( "IfNotFound" );
        DefaultUnits = props.getValue ( "DefaultUnits" );
        if ( ListFile != null ) {
            __ListFile_JTextField.setText ( ListFile );
        }
        if ( __IDCol_JComboBox != null ) {
            if ( IDCol == null ) {
                // Select default...
                __IDCol_JComboBox.select ( 0 );
            }
            else {  if (    JGUIUtil.isSimpleJComboBoxItem(
                    __IDCol_JComboBox, IDCol,
                    JGUIUtil.NONE, null, null ) ){
                    __IDCol_JComboBox.select ( IDCol );
                }
                else {  Message.printWarning ( 1, routine,
                    "Existing command references an invalid\n"+
                    "IDCol \"" + IDCol +
                    "\".  Select a\ndifferent value or " +
                    "Cancel." );
                }
            }
        }
        if ( ID != null ) {
            __ID_JTextField.setText ( ID );
        }
        if ( DataSource != null ) {
            __DataSource_JTextField.setText ( DataSource );
        }
        if ( DataType != null ) {
            __DataType_JTextField.setText ( DataType );
        }
        if ( Interval != null ) {
            __Interval_JTextField.setText ( Interval );
        }
        if ( Scenario != null ) {
            __Scenario_JTextField.setText ( Scenario );
        }
        if ( InputType != null ) {
            __InputType_JTextField.setText ( InputType );
        }
        if ( InputName != null ) {
            __InputName_JTextField.setText ( InputName );
        }
        if ( __IfNotFound_JComboBox != null ) {
            if ( IfNotFound == null ) {
                // Select default...
                __IfNotFound_JComboBox.select ( __command._Warn );
            }
            else {  if (    JGUIUtil.isSimpleJComboBoxItem(
                    __IfNotFound_JComboBox,
                    IfNotFound, JGUIUtil.NONE, null, null ) ) {
                    __IfNotFound_JComboBox.select ( IfNotFound );
                }
                else {  Message.printWarning ( 1, routine,
                    "Existing command references an invalid\n"+
                    "IfNotFound \"" + IfNotFound + "\".  Select a\ndifferent value or Cancel." );
                }
            }
        }
        if ( DefaultUnits != null ) {
            __DefaultUnits_JTextField.setText ( DefaultUnits );
        }
    }
    // Regardless, reset the command from the fields...
    ListFile = __ListFile_JTextField.getText().trim();
    IDCol = __IDCol_JComboBox.getSelected();
    Delim = __Delim_JTextField.getText().trim();
    ID = __ID_JTextField.getText().trim();
    DataSource = __DataSource_JTextField.getText().trim();
    DataType = __DataType_JTextField.getText().trim();
    Interval = __Interval_JTextField.getText().trim();
    Scenario = __Scenario_JTextField.getText().trim();
    InputType = __InputType_JTextField.getText().trim();
    InputName = __InputName_JTextField.getText().trim();
    IfNotFound = __IfNotFound_JComboBox.getSelected();
    DefaultUnits = __DefaultUnits_JTextField.getText().trim();
    props = new PropList ( __command.getCommandName() );
    props.add ( "ListFile=" + ListFile );
    props.add ( "IDCol=" + IDCol );
    props.add ( "Delim=" + Delim );
    props.add ( "ID=" + ID );
    props.add ( "DataSource=" + DataSource );
    props.add ( "DataType=" + DataType );
    props.add ( "Interval=" + Interval );
    props.add ( "Scenario=" + Scenario );
    props.add ( "InputType=" + InputType );
    props.add ( "InputName=" + InputName );
    props.add ( "IfNotFound=" + IfNotFound );
    props.add ( "DefaultUnits=" + DefaultUnits );
    __command_JTextArea.setText( __command.toString ( props ).trim() );
	if ( __path_JButton != null ) {
		if ( (ListFile != null) && !ListFile.isEmpty() ) {
			__path_JButton.setEnabled ( true );
			File f = new File ( ListFile );
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
@param ok if false, then the edit is cancelled.  If true, the edit is committed
and the dialog is closed.
*/
private void response ( boolean ok )
{   __ok = ok;  // Save to be returned by ok()
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
public void windowClosing( WindowEvent event )
{	response ( false );
}

public void windowActivated( WindowEvent evt )
{
}

public void windowClosed( WindowEvent evt )
{
}

public void windowDeactivated( WindowEvent evt )
{
}

public void windowDeiconified( WindowEvent evt )
{
}

public void windowIconified( WindowEvent evt )
{
}

public void windowOpened( WindowEvent evt )
{
}

} // end createFromList_JDialog
