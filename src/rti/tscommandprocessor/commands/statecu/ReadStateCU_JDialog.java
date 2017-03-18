// ----------------------------------------------------------------------------
// readStateCU_JDialog - editor for readStateCUB()
// ----------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
// ----------------------------------------------------------------------------
// History: 
//
// 2004-07-11	Steven A. Malers, RTi	Initial version (copy and modify
//					readStateModB_JDialog).
// 2005-06-09	SAM, RTi		Update to read CDS and IPY files.
// 2007-02-26	SAM, RTi		Clean up code based on Eclipse feedback.
// 2007-04-09	SAM, RTi		Add AutoAdjust to help automatically handle
//					new crop names that may not work with TSIDs.
//					Change the command from a text field to text area.
// 2007-06-21	SAM, RTi		Move to command design.
// ----------------------------------------------------------------------------

package rti.tscommandprocessor.commands.statecu;

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

import javax.swing.JFileChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

@SuppressWarnings("serial")
public class ReadStateCU_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
	
private SimpleJButton	__browse_JButton = null,// File browse button
			__cancel_JButton = null,// Cancel Button
			__ok_JButton = null,	// Ok Button
			__path_JButton = null;	// Convert between relative and
						// absolute paths.
private ReadStateCU_Command		__command = null;// Command to edit
private JTextArea	__command_JTextArea=null;// Command as TextArea
private String		__working_dir = null;	// Working directory.
private JTextField	__InputFile_JTextField = null;// Field for input file.
private JTextField	__InputStart_JTextField = null;
private JTextField	__InputEnd_JTextField = null;
private JTextField	__TSID_JTextField = null;// Field for time series
						// identifier
private JTextField	__NewScenario_JTextField = null;// Field for new scenario
private SimpleJComboBox	__AutoAdjust_JComboBox = null;  // For development to
						// deal with non-standard issues in data (e.g., crop
						// names that include "."
private SimpleJComboBox	__CheckData_JComboBox = null;  // Check data?
private boolean		__error_wait = false;	// Is there an error that we
						// are waiting to be cleared up
						// or Cancel?
private boolean		__first_time = true;
private boolean		__ok = false;	// Indicates whether OK was pressed to close

/**
readStateCU_JDialog constructor.
@param parent Frame class instantiating this class.
@param command Time series command to edit.
*/
public ReadStateCU_JDialog ( JFrame parent, ReadStateCU_Command command )
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
		String last_directory_selected =
			JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser(
				last_directory_selected );
		}
		else {	fc = JFileChooserFactory.createJFileChooser(
				__working_dir );
		}
		fc.setDialogTitle( "Select StateMod Time Series File");
		// REVISIT - maybe need to list all recognized StateCU file
		// extensions for data sets.
		SimpleFileFilter cds_sff = 
			new SimpleFileFilter("cds",
			"Crop Pattern Time Series (Yearly)");
		fc.addChoosableFileFilter(cds_sff);
		SimpleFileFilter ipy_sff = 
			new SimpleFileFilter("ipy",
			"Irrigation Practice Time Series (Yearly)");
		fc.addChoosableFileFilter(ipy_sff);
		SimpleFileFilter iwr_sff = 
			new SimpleFileFilter("iwr",
			"Irrigation Water Requirement - StateCU report " +
			"(Monthly, Yearly)");
		fc.addChoosableFileFilter(iwr_sff);
		SimpleFileFilter wsl_sff = 
			new SimpleFileFilter("wsl",
			"Water Supply Limited CU - StateCU report " +
			"(Monthly, Yearly)");
		fc.addChoosableFileFilter(wsl_sff);
		fc.setFileFilter ( cds_sff );
		
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
				Message.printWarning ( 1,
				"readStateCU_JDialog",
				"Error converting file to relative path." );
			}
		}
		refresh ();
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
Check the input.  If errors exist, warn the user and set the __errorWait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	PropList props = new PropList ( "" );
	String InputFile = __InputFile_JTextField.getText().trim();
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
	String TSID = __TSID_JTextField.getText().trim();
	String NewScenario = __NewScenario_JTextField.getText().trim();
	String AutoAdjust = __AutoAdjust_JComboBox.getSelected();
	String CheckData = __CheckData_JComboBox.getSelected();
	__error_wait = false;
	if ( InputFile.length() > 0 ) {
		props.set ( "InputFile", InputFile );
	}
	if (InputStart.length() > 0) {
		props.set("InputStart", InputStart);
	}
	if (InputEnd.length() > 0) {
		props.set("InputEnd", InputEnd);
	}
	if (TSID.length() > 0) {
		props.set("TSID", TSID);
	}
	if (NewScenario.length() > 0) {
		props.set("NewScenario", NewScenario);
	}
	if (AutoAdjust.length() > 0) {
		props.set("AutoAdjust", AutoAdjust);
	}
	if (CheckData.length() > 0) {
		props.set("CheckData", CheckData);
	}
	try { // This will warn the user...
		__command.checkCommandParameters(props, null, 1);
	} catch (Exception e) {
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
	String TSID = __TSID_JTextField.getText().trim();
	String NewScenario = __NewScenario_JTextField.getText().trim();
	String AutoAdjust = __AutoAdjust_JComboBox.getSelected();
	String CheckData = __CheckData_JComboBox.getSelected();
	__command.setCommandParameter ( "InputFile", InputFile );
	__command.setCommandParameter ( "InputStart", InputStart );
	__command.setCommandParameter ( "InputEnd", InputEnd );
	__command.setCommandParameter ( "TSID", TSID );
	__command.setCommandParameter ( "NewScenario", NewScenario );
	__command.setCommandParameter ( "AutoAdjust", AutoAdjust );
	__command.setCommandParameter ( "CheckData", CheckData );
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
	__TSID_JTextField = null;
	__NewScenario_JTextField = null;
	__AutoAdjust_JComboBox = null;
	__CheckData_JComboBox = null;
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
TSEngine.getTSIdentifiersFromCommands().
*/
private void initialize ( JFrame parent, ReadStateCU_Command command )
{	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Read 1+ (or all) time series from one of the following " +
		"file types, using data in the file to assign the identifier:"),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"     Crop pattern time series file (StateCU input file)." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"     Irrigation practice time series file (input)." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"     StateCU IWR or WSL report file (output)." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify a full or relative path (relative to working directory)." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel ("The working directory is: " + __working_dir ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The time series identifier pattern, if specified, will" +
		" filter the read ONLY for IWR and WSL files;" ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"specify blank to read all or use * wildcards to match a time series identifier." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"For example, to read all monthly IWR time series for locations starting with ABC, specify:" ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "  ABC.*.IWR.Month" ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Location can be X, X*, or *.  Data type and interval can be * or combinations as follows:"),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "   CropArea-AllCrops (Year)" ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "   IWR or WSL (Month, Year)" ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "   IWR_Depth or WSL_Depth (Month, Year)" ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "StateCU file to read:" ),
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
    	"Optional - overrides the global input start (default=read all)."),
    	3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input end:"), 
    	0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputEnd_JTextField = new JTextField (20);
    __InputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputEnd_JTextField,
    	1, y, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Optional - overrides the global input end (default=read all)."),
    	3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );   
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Time series ID:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TSID_JTextField = new JTextField ( 10 );
	__TSID_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __TSID_JTextField,
		1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
	"Optional - Loc.Source.Type.Interval to filter (default=read all)."),
	3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "New scenario:" ), 
    		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NewScenario_JTextField = new JTextField ( 10 );
    __NewScenario_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __NewScenario_JTextField,
    		1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Optional - to help uniquely identify time series (default=none)."),
    	3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Automatically adjust?:" ), 
   		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
   	__AutoAdjust_JComboBox = new SimpleJComboBox ( false );
   	List<String> adjustChoices = new ArrayList<String>();
   	adjustChoices.add ( "" );
   	adjustChoices.add ( __command._False );
   	adjustChoices.add ( __command._True );
   	__AutoAdjust_JComboBox.addItemListener ( this );
   	__AutoAdjust_JComboBox.setData(adjustChoices);
    JGUIUtil.addComponent(main_JPanel, __AutoAdjust_JComboBox,
   		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
       	"Optional - convert data type with \".\" to \"-\" to work in TSID (default=" + __command._True + ")."),
       	3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Check data after read?:" ), 
       		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CheckData_JComboBox = new SimpleJComboBox ( false );
    List<String> checkChoices = new ArrayList<String>();
    checkChoices.add ( "" );
    checkChoices.add ( __command._False );
    checkChoices.add ( __command._True );
    __CheckData_JComboBox.setData(checkChoices);
    __CheckData_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __CheckData_JComboBox,
       		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
           	"Optional - check data integrity after read? (default=" + __command._True + ")."),
           	3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
       	
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 5, 55 );
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
		__path_JButton = new SimpleJButton(
					"Remove Working Directory",this);
		button_JPanel.add ( __path_JButton );
	}
	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );

	setTitle ( "Edit " + __command.getCommandName() + "() Command" );
	// Dialogs do not need to be resizable...
	setResizable ( true );
    pack();
    JGUIUtil.center( this );
	refresh();	// Sets the __path_JButton status
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent e )
{	Object o = e.getItemSelectable();

	if ( (o == __AutoAdjust_JComboBox) && (e.getStateChange() == ItemEvent.SELECTED) ) {
		refresh();
	}
    else if ( (o == __CheckData_JComboBox) && (e.getStateChange() == ItemEvent.SELECTED) ) {
        refresh();
    }
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
			response ( true );
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
{	String routine = "readStateCU_JDialog.refresh";
	__error_wait = false;
	String InputFile="";
	String InputStart="";
	String InputEnd="";
	String TSID="";
	String NewScenario="";
	String AutoAdjust = "";
	String CheckData = "";
	if ( __first_time ) {
		__first_time = false;
		PropList props = __command.getCommandParameters();
		InputFile = props.getValue ( "InputFile" );
		InputStart = props.getValue ( "InputStart" );
		InputEnd = props.getValue ( "InputEnd" );
		TSID = props.getValue ( "TSID" );
		NewScenario = props.getValue ( "NewScenario" );
		AutoAdjust = props.getValue ( "AutoAdjust" );
		CheckData = props.getValue ( "CheckData" );
		if ( InputFile != null ) {
			__InputFile_JTextField.setText (InputFile);
		}
		if ( InputStart != null ) {
			__InputStart_JTextField.setText ( InputStart );
		}
		if ( InputEnd != null ) {
			__InputEnd_JTextField.setText ( InputEnd );
		}
		if ( TSID != null ) {
			__TSID_JTextField.setText (TSID);
		}
		if ( NewScenario != null ) {
			__NewScenario_JTextField.setText (NewScenario);
		}
		if (	JGUIUtil.isSimpleJComboBoxItem(
				__AutoAdjust_JComboBox,
				AutoAdjust, JGUIUtil.NONE, null, null ) ) {
				__AutoAdjust_JComboBox.select ( AutoAdjust);
		}
		else {	if (	(AutoAdjust == null) ||
					AutoAdjust.equals("") ) {
					// New command...select the default...
					__AutoAdjust_JComboBox.select ( 0 );
				}
				else {	Message.printWarning ( 1,
						routine,
						"Existing " + __command.getCommandName() +
						"() references an " +
						"invalid\n"+ "AutoAdjust parameter \"" +
						AutoAdjust + "\".  Correct or Cancel." );
				}
		}
		if (	JGUIUtil.isSimpleJComboBoxItem(
				__CheckData_JComboBox,
				CheckData, JGUIUtil.NONE, null, null ) ) {
				__CheckData_JComboBox.select ( CheckData);
		}
		else {	if (	(CheckData == null) ||
					CheckData.equals("") ) {
					// New command...select the default...
					__CheckData_JComboBox.select ( 0 );
				}
				else {	Message.printWarning ( 1,
						routine,
						"Existing " + __command.getCommandName() +
						"() references an " +
						"invalid\n"+ "CheckData parameter \"" +
						CheckData + "\".  Correct or Cancel." );
				}
		}
	}
	// Regardless, reset the command from the fields...
	InputFile = __InputFile_JTextField.getText().trim();
	InputStart = __InputStart_JTextField.getText().trim();
	InputEnd = __InputEnd_JTextField.getText().trim();
	TSID = __TSID_JTextField.getText().trim();
	NewScenario = __NewScenario_JTextField.getText().trim();
	AutoAdjust = __AutoAdjust_JComboBox.getSelected();
	CheckData = __CheckData_JComboBox.getSelected();
	PropList props = new PropList ( __command.getCommandName() );
	props.add ( "InputFile=" + InputFile );
	props.add ( "InputStart=" + InputStart );
	props.add ( "InputEnd=" + InputEnd );
	props.add ( "TSID=" + TSID );
	props.add ( "NewScenario=" + NewScenario );
	props.add ( "AutoAdjust=" + AutoAdjust );
	props.add ( "CheckData=" + CheckData );
	__command_JTextArea.setText(__command.toString(props) );

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
@param ok if false, then the edit is canceled.  If true, the edit is committed
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

}