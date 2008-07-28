// ----------------------------------------------------------------------------
// readStateMod_JDialog - editor for readStateMod()
// ----------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
// ----------------------------------------------------------------------------
// History: 
//
// 28 Feb 2001	Steven A. Malers, RTi	Initial version (copy and modify
//					writeDateValue).
// 16 Apr 2001	SAM, RTi		Enable the Browse button.
// 2002-04-23	SAM, RTi		Clean up the dialog and add the ability
//					to add/remove the working directory.
// 2003-10-29	SAM, RTi		Rename class from readStateMod_Dialog to
//					readStateMod_JDialog and update to
//					Swing.
// 2004-02-17	SAM, RTi		Fix bug where directory from file
//					selection was not getting set as the
//					last dialog directory in JGUIUtil.
// 2005-09-02	SAM, RTi		Move from TSTool to StateMod package and
//					update to the new command class design.
// 2007-02-16	SAM, RTi		Use new CommandProcessor interface.
//					Clean up code based on Eclipse feedback.
// 2007-03-01	SAM, RTi		Clean up code based on Eclipse feedback.
// ----------------------------------------------------------------------------

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
import javax.swing.JTextArea;
import javax.swing.JTextField;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.File;
import java.util.Vector;

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

public class readStateMod_JDialog extends JDialog
implements ActionListener, KeyListener, WindowListener
{
private SimpleJButton	__browse_JButton = null,// File browse button
			__cancel_JButton = null,// Cancel Button
			__ok_JButton = null,	// Ok Button
			__path_JButton = null;	// Convert between relative and
						// absolute paths.
private readStateMod_Command __command = null;	// Command to edit
private JTextArea	__command_JTextArea=null;// Command as TextField
private String		__working_dir = null;	// Working directory.
private JTextField	__InputFile_JTextField = null;// Field for time series
						// identifier
private JTextField	__InputStart_JTextField = null;
						// Start of period for input
private JTextField	__InputEnd_JTextField = null;
						// End of period for input
private SimpleJComboBox	__Interval_JComboBox = null;
						// Interval for water rights output.
private SimpleJComboBox	__SpatialAggregation_JComboBox = null;
						// aggregation for water rights output.
private JTextField	__ParcelYear_JTextField = null;
						// Parcel year for parcel total water rights
private boolean		__error_wait = false;	// Is there an error that we
						// are waiting to be cleared up
						// or Cancel?
private boolean		__first_time = true;
// TODO SAM 2007-02-18 Evaluate whether to support alias
//private boolean		__use_alias = false;	// If true, then the syntax is
						// TS Alias = readStateMod().
						// If false, it is:
						// readStateMod().
private boolean		__ok = false;		// Indicates whether OK was
						// pressed when closing the
						// dialog.

/**
readStateMod_JDialog constructor.
@param parent Frame class instantiating this class.
@param command Command to edit.
*/
public readStateMod_JDialog ( JFrame parent, Command command )
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
			fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
		}
		else {
            fc = JFileChooserFactory.createJFileChooser(__working_dir );
		}
		fc.setDialogTitle( "Select StateMod Time Series File");
		// TODO - maybe need to list all recognized StateMod file extensions for data sets.
		SimpleFileFilter sff = new SimpleFileFilter("stm", "StateMod Time Series File");
		fc.addChoosableFileFilter(sff);
		
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			String directory = fc.getSelectedFile().getParent();
			String filename = fc.getSelectedFile().getName(); 
			String path = fc.getSelectedFile().getPath(); 
	
			if (filename == null || filename.equals("")) {
				return;
			}
	
			if (path != null) {
				__InputFile_JTextField.setText(path );
				JGUIUtil.setLastFileDialogDirectory(directory);
				refresh();
			}
		}
	}
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else if ( o == __path_JButton ) {
		if (	__path_JButton.getText().equals(
			"Add Working Directory") ) {
			__InputFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir,
			__InputFile_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals(
			"Remove Working Directory") ) {
			try {	__InputFile_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir,
				__InputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,"readStateMod_JDialog",
				"Error converting file to relative path." );
			}
		}
		refresh ();
	}
	else if ( (__Interval_JComboBox != null) && (o == __Interval_JComboBox) ) {
		refresh ();
	}
	else if ( (__SpatialAggregation_JComboBox != null) && (o == __SpatialAggregation_JComboBox) ) {
		refresh ();
	}
	else {	// Other combo boxes, etc...
		refresh();
	}
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	String InputFile = __InputFile_JTextField.getText().trim();
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
	String Interval = __Interval_JComboBox.getSelected();
	String SpatialAggregation = __SpatialAggregation_JComboBox.getSelected();
	String ParcelYear = __ParcelYear_JTextField.getText().trim();
	__error_wait = false;
	if ( InputFile.length() > 0 ) {
		props.set ( "InputFile", InputFile );
	}
	if ( InputStart.length() > 0 ) {
		props.set ( "InputStart", InputStart );
	}
	if ( InputEnd.length() > 0 ) {
		props.set ( "InputEnd", InputEnd );
	}
	if ( Interval.length() > 0 ) {
		props.set ( "Interval", Interval );
	}
	if ( SpatialAggregation.length() > 0 ) {
		props.set ( "SpatialAggregation", SpatialAggregation );
	}
	if ( ParcelYear.length() > 0 ) {
		props.set ( "ParcelYear", ParcelYear );
	}
	try {	// This will warn the user...
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
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
	String Interval = __Interval_JComboBox.getSelected();
	String SpatialAggregation = __SpatialAggregation_JComboBox.getSelected();
	String ParcelYear = __ParcelYear_JTextField.getText().trim();
	__command.setCommandParameter ( "InputFile", InputFile );
	__command.setCommandParameter ( "InputStart", InputStart );
	__command.setCommandParameter ( "InputEnd", InputEnd );
	__command.setCommandParameter ( "Interval", Interval );
	__command.setCommandParameter ( "SpatialAggregation", SpatialAggregation );
	__command.setCommandParameter ( "ParcelYear", ParcelYear );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__browse_JButton = null;
	__cancel_JButton = null;
	__command_JTextArea = null;
	__InputFile_JTextField = null;
	__InputStart_JTextField = null;
	__InputEnd_JTextField = null;
	__Interval_JComboBox = null;
	__SpatialAggregation_JComboBox = null;
	__ParcelYear_JTextField = null;
	__command = null;
	__ok_JButton = null;
	__path_JButton = null;
	__working_dir = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Command command )
{	__command = (readStateMod_Command)command;
	CommandProcessor processor = __command.getCommandProcessor();
	// TODO SAM 2007-02-18 Evaluate whether to support
	//__use_alias = false;

	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)processor, __command );
	
	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Read all the time series from a StateMod time series or water right file, using " +
		"information in the file to assign the identifier."),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The data source and data type will be blank in the resulting "+
		"time series identifier (TSID)."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Specify the interval and parcel year only for well rights, based on how data will be used."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify a full or relative path (relative to working directory)." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
        	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The working directory is: " + __working_dir ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"StateMod file to read:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputFile_JTextField = new JTextField ( 50 );
	__InputFile_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __InputFile_JTextField,
		1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ( "Browse", this );
        JGUIUtil.addComponent(main_JPanel, __browse_JButton,
		6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

        JGUIUtil.addComponent(main_JPanel, new JLabel ("Input start:"), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputStart_JTextField = new JTextField (20);
	__InputStart_JTextField.addKeyListener (this);
        JGUIUtil.addComponent(main_JPanel, __InputStart_JTextField,
		1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Overrides the global input start."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input end:"), 
		0, ++y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputEnd_JTextField = new JTextField (20);
	__InputEnd_JTextField.addKeyListener (this);
        JGUIUtil.addComponent(main_JPanel, __InputEnd_JTextField,
		1, y, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Overrides the global input end."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
     JGUIUtil.addComponent(main_JPanel, new JLabel ("Interval:"),
      	0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
       	Vector Interval_Vector = new Vector();
       	Interval_Vector.addElement ( "" );
       	Interval_Vector.addElement ( __command._Day );
       	Interval_Vector.addElement ( __command._Month );
       	Interval_Vector.addElement ( __command._Year );
    	// TODO SAM 2007-05-16 Evaluate whether needed
       	//Interval_Vector.addElement ( __command._Irregular );
       	__Interval_JComboBox = new SimpleJComboBox(false);
       	__Interval_JComboBox.setData ( Interval_Vector );
       	__Interval_JComboBox.select ( 0 );
       	__Interval_JComboBox.addActionListener (this);
    JGUIUtil.addComponent(main_JPanel, __Interval_JComboBox,
    	1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Interval for resulting time series when reading rights."),
    	3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
        JGUIUtil.addComponent(main_JPanel, new JLabel ("Spatial aggregation:"),
              	0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
               	Vector SpatialAggregation_Vector = new Vector();
               	SpatialAggregation_Vector.addElement ( "" );
               	SpatialAggregation_Vector.addElement ( __command._Location );
               	SpatialAggregation_Vector.addElement ( __command._Parcel );
               	SpatialAggregation_Vector.addElement ( __command._None );
               	__SpatialAggregation_JComboBox = new SimpleJComboBox(false);
               	__SpatialAggregation_JComboBox.setData ( SpatialAggregation_Vector );
               	__SpatialAggregation_JComboBox.select ( 0 );
               	__SpatialAggregation_JComboBox.addActionListener (this);
            JGUIUtil.addComponent(main_JPanel, __SpatialAggregation_JComboBox,
            	1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
                JGUIUtil.addComponent(main_JPanel, new JLabel (
            	"Spatial aggregation when reading rights (default=Location)."),
            	3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

       JGUIUtil.addComponent(main_JPanel, new JLabel ( "Parcel year:"), 
       		0, ++y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
       	__ParcelYear_JTextField = new JTextField (20);
       	__ParcelYear_JTextField.addKeyListener (this);
               JGUIUtil.addComponent(main_JPanel, __ParcelYear_JTextField,
       		1, y, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
               JGUIUtil.addComponent(main_JPanel, new JLabel (
       		"Use to only read a single irrigated lands year from a well right file."),
       		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
       
        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
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

	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative
		// path...
		__path_JButton = new SimpleJButton(	"Remove Working Directory",this);
		button_JPanel.add ( __path_JButton );
	}
	button_JPanel.add (__cancel_JButton = new SimpleJButton("Cancel",this));
	button_JPanel.add ( __ok_JButton = new SimpleJButton("OK", this) );

	setTitle ( "Edit " + __command.getCommandName() + "() Command" );
	// Dialogs do not need to be resizable...
	setResizable ( true );
    pack();
    JGUIUtil.center( this );
	refresh();	// Sets the __path_JButton status
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
@return true if the edits were committed, false if the user cancelled.
*/
public boolean ok ()
{	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = "readStateMod_JDialog.refresh";
	String InputFile = "";
	String InputStart = "";
	String InputEnd = "";
	String Interval = "";
	String SpatialAggregation = "";
	String ParcelYear = "";
	PropList props = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		props = __command.getCommandParameters();
		InputFile = props.getValue ( "InputFile" );
		InputStart = props.getValue ( "InputStart" );
		InputEnd = props.getValue ( "InputEnd" );
		Interval = props.getValue ( "Interval" );
		SpatialAggregation = props.getValue ( "SpatialAggregation" );
		ParcelYear = props.getValue ( "ParcelYear" );
		if ( InputFile != null ) {
			__InputFile_JTextField.setText ( InputFile );
		}
		if ( InputStart != null ) {
			__InputStart_JTextField.setText ( InputStart );
		}
		if ( InputEnd != null ) {
			__InputEnd_JTextField.setText ( InputEnd );
		}
		if ( Interval == null ) {
			// Select the first item
			__Interval_JComboBox.select ( 0 );
		}
		else {	if (	JGUIUtil.isSimpleJComboBoxItem(
				__Interval_JComboBox,
				Interval, JGUIUtil.NONE, null, null ) ) {
				__Interval_JComboBox.select ( Interval );
			}
			else {	Message.printWarning ( 1, routine,
					"Existing command " +
					"references an invalid\nInterval value \"" +
					Interval +
				"\".  Select a different value or Cancel.");
				__error_wait = true;
			}
		}
		if ( SpatialAggregation == null ) {
			// Select the first item
			__SpatialAggregation_JComboBox.select ( 0 );
		}
		else {	if (	JGUIUtil.isSimpleJComboBoxItem(
				__SpatialAggregation_JComboBox,
				SpatialAggregation, JGUIUtil.NONE, null, null ) ) {
				__SpatialAggregation_JComboBox.select ( SpatialAggregation );
			}
			else {	Message.printWarning ( 1, routine,
					"Existing command " +
					"references an invalid\nSpatialAggregation value \"" +
					SpatialAggregation +
				"\".  Select a different value or Cancel.");
				__error_wait = true;
			}
		}
		if ( ParcelYear != null ) {
			__ParcelYear_JTextField.setText ( ParcelYear );
		}
	}
	// Regardless, reset the command from the fields...
	InputFile = __InputFile_JTextField.getText().trim();
	InputStart = __InputStart_JTextField.getText().trim();
	InputEnd = __InputEnd_JTextField.getText().trim();
	Interval = __Interval_JComboBox.getSelected();
	SpatialAggregation = __SpatialAggregation_JComboBox.getSelected();
	ParcelYear = __ParcelYear_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "InputFile=" + InputFile );
	props.add ( "InputStart=" + InputStart );
	props.add ( "InputEnd=" + InputEnd );
	props.add ( "Interval=" + Interval );
	props.add ( "SpatialAggregation=" + SpatialAggregation );
	props.add ( "ParcelYear=" + ParcelYear );
	__command_JTextArea.setText( __command.toString ( props ) );
	// Check the path and determine what the label on the path button should
	// be...
	if ( __path_JButton != null ) {
		__path_JButton.setEnabled ( true );
		File f = new File ( InputFile );
		if ( f.isAbsolute() ) {
			__path_JButton.setText ( "Remove Working Directory" );
		}
		else {	__path_JButton.setText ( "Add Working Directory" );
		}
	}
}

/**
React to the user response.
@param ok if false, then the edit is cancelled.  If true, the edit is committed
and the dialog is closed.
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
public void windowClosing( WindowEvent event )
{	response ( false );
}

public void windowActivated( WindowEvent evt ){;}
public void windowClosed( WindowEvent evt ){;}
public void windowDeactivated( WindowEvent evt ){;}
public void windowDeiconified( WindowEvent evt ){;}
public void windowIconified( WindowEvent evt ){;}
public void windowOpened( WindowEvent evt ){;}

} // end readStateMod_JDialog
