// ----------------------------------------------------------------------------
// changeInterval_JDialog - editor for TS X = changeInterval()
//
// TODO SAM 2005-02-12
//		In the future may also support changeInterval() to operate on
//		multiple time series.
// ----------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
// ----------------------------------------------------------------------------
// History: 
//
// 2005-02-16	Steven A. Malers, RTi	Initial version, initialized from
//					normalize_JDialog().
// 2005-02-18	SAM, RTi		Comment out AllowMissingPercent - it
//					is causing problems in some of the
//					computations so re-evaluate later.
// 2005-03-14	SAM, RTi		Add OutputFillMethod and
//					HandleMissingInputHow parameters.
// 2005-05-24	Luiz Teixeira, RTi	Copied the original class 
//					changeInterval_JDialog() from TSTool and
//					split the code into the new
//					changeInterval_JDialog() and
//					changeInterval_Command().
// 2005-05-26	Luiz Teixeira, RTi	Cleanup and documentation.
// 2007-02-16	SAM, RTi		Use new CommandProcessor interface.
//					Clean up code based on Eclipse feedback.
// 2007-05-08	SAM, RTi		Cleanup code based on Eclipse feedback.
// ----------------------------------------------------------------------------
package rti.tscommandprocessor.commands.ts;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.MeasTimeScale;
import RTi.Util.IO.PropList;
import RTi.Util.IO.Command;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.TimeInterval;

public class ChangeInterval_JDialog extends JDialog
	implements ActionListener,
		   ItemListener, 
		   KeyListener, 
		   WindowListener
{
// Controls are defined in logical order -- The order they appear in the dialog
// box and documentation.
private ChangeInterval_Command __command = null;// Command object.

private JTextField	__Alias_JTextField = null;
						// Field for time series alias
private SimpleJComboBox	__TSID_JComboBox = null;
						// Time series available to
						// operate on.
private SimpleJComboBox	__NewInterval_JComboBox = null;
						// New time interval for result.
private SimpleJComboBox	__OldTimeScale_JComboBox = null;
						// Old time scale for time
						// series.
private SimpleJComboBox	__NewTimeScale_JComboBox = null;
private JTextField	__NewDataType_JTextField = null;
private JTextField  __NewUnits_JTextField = null;// Field for new units
private JTextField	__Tolerance_JTextField = null;
private SimpleJComboBox	__HandleEndpointsHow_JComboBox = null;
private JTextField	__AllowMissingCount_JTextField = null;
						// Number of missing to allow
						// in input when converting.
/* TODO SAM 2005-02-18 may enable later
private JTextField	__AllowMissingPercent_JTextField = null;
						// Percent of missing to allow
						// in input when converting.
*/
private SimpleJComboBox	__OutputFillMethod_JComboBox = null;
						// Fill method when going from
						// large to small interval.
private SimpleJComboBox	__HandleMissingInputHow_JComboBox = null;
						// How to handle missing data
						// in input time series.
private JTextArea	__Command_JTextArea   = null;
private JScrollPane	__Command_JScrollPane = null;
						// Command JTextArea and
						// related controls
private SimpleJButton	__cancel_JButton = null;// Cancel Button
private String __cancel_String  = "Cancel";
private String __cancel_Tip = "Close the window, without returning the command.";
private SimpleJButton	__ok_JButton 	 = null;// Ok Button
private String __ok_String  = "OK";
private String __ok_Tip = "Close the window, returning the command.";

private JTextField	__statusJTextField = null; // Status bar					
private boolean		__error_wait = false;	// Is there an error to be cleared up?
private boolean		__first_time = true;
private boolean		__ok         = false;						

/**
changeInterval_JDialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to parse.
*/
public ChangeInterval_JDialog ( JFrame parent, Command command )
{	
	super(parent, true);
	
	// Initialize the dialog.
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	
	Object o = event.getSource();

	if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __ok_JButton ) {		
		refresh ();
		checkInput();	
		if ( !__error_wait ) {
			response ( true );
		}
	}
}

/**
Check the user input for errors and set __error_wait accordingly.
*/
private void checkInput ()
{	// Get the values from the interface.
	String Alias = __Alias_JTextField.getText().trim();
	String TSID = __TSID_JComboBox.getSelected();
	String NewInterval  = __NewInterval_JComboBox.getSelected();
	String OldTimeScale = StringUtil.getToken( __OldTimeScale_JComboBox.getSelected(), " ", 0, 0 );
	String NewTimeScale  = StringUtil.getToken( __NewTimeScale_JComboBox.getSelected(), " ", 0, 0 );
	String NewDataType = __NewDataType_JTextField.getText().trim();
	String NewUnits = __NewUnits_JTextField.getText().trim();
	String Tolerance = __Tolerance_JTextField.getText().trim();
    String HandleEndpointsHow = __HandleEndpointsHow_JComboBox.getSelected();
	String AllowMissingCount = __AllowMissingCount_JTextField.getText().trim();
	/* TODO LT 2005-05-24 may enable later		
	String AllowMissingPercent = __AllowMissingPercent_JTextField.getText().trim(); */
	String OutputFillMethod = __OutputFillMethod_JComboBox.getSelected();
	String HandleMissingInputHow = __HandleMissingInputHow_JComboBox.getSelected();
	
	// Put together the list of parameters to check...
	PropList props = new PropList ( "" );
	// Alias
	if ( Alias != null && Alias.length() > 0 ) {
		props.set( "Alias", Alias );
	}
	// TSID
	if ( TSID != null && TSID.length() > 0 ) {
		props.set( "TSID", TSID );
	}
	// NewInterval
	if ( NewInterval != null && NewInterval.length() > 0 ) {
		props.set( "NewInterval", NewInterval );
	}
	// OldTimeScale
	if ( OldTimeScale != null && OldTimeScale.length() > 0 ) {
		props.set( "OldTimeScale", OldTimeScale );
	}
	// NewTimeScale
	if ( NewTimeScale != null && NewTimeScale.length() > 0 ) {
		props.set( "NewTimeScale", NewTimeScale );
	}
	// NewDataType
	if ( NewDataType != null && NewDataType.length() > 0 ) {
		props.set( "NewDataType", NewDataType );
	}
	if ( NewUnits != null && NewUnits.length() > 0 ) {
	     props.set( "NewUnits", NewUnits );
	}
    // Tolerance
    if ( Tolerance != null && Tolerance.length() > 0 ) {
	     props.set( "Tolerance", Tolerance );
	}
    // HandleEndpointsHow
	if ( HandleEndpointsHow != null &&
	     HandleEndpointsHow.length() > 0 ) {
		props.set( "HandleEndpointsHow", HandleEndpointsHow );
	}
	// AllowMissingCount
	if ( AllowMissingCount != null && AllowMissingCount.length() > 0 ) {
		props.set( "AllowMissingCount", AllowMissingCount );
	}
	// AllowMissingPercent
	/* TODO LT 2005-05-24 may enable later
	if ( AllowMissingPercent != null && AllowMissingPercent.length() > 0 ) {
		props.set( "AllowMissingPercent", AllowMissingPercent );
	} */
	// OutputFillMethod
	if ( OutputFillMethod != null && OutputFillMethod.length() > 0 ) {
		props.set( "OutputFillMethod", OutputFillMethod );
	}
	// HandleMissingInputHow
	if ( HandleMissingInputHow != null &&
	     HandleMissingInputHow.length() > 0 ) {
		props.set( "HandleMissingInputHow", HandleMissingInputHow );
	}
	
	// Check the list of Command Parameters.
	try {	// This will warn the user...
		__command.checkCommandParameters ( props, null, 1 );
		__error_wait = false;
	} catch ( Exception e ) {
		// The warning would have been printed in the check code.		
		__error_wait = true;
	}
}

/**
Commit the edits to the command.  In this case the command parameters have
already been checked and no errors were detected.
*/
private void commitEdits ()
{
	// Get the values from the interface.
	String Alias = __Alias_JTextField.getText().trim();
	String TSID = __TSID_JComboBox.getSelected();
	String NewInterval = __NewInterval_JComboBox.getSelected();
	String OldTimeScale = StringUtil.getToken( __OldTimeScale_JComboBox.getSelected(), " ", 0, 0 );
	String NewTimeScale = StringUtil.getToken( __NewTimeScale_JComboBox.getSelected(), " ", 0, 0 );
	String NewDataType = __NewDataType_JTextField.getText().trim();
	String NewUnits = __NewUnits_JTextField.getText().trim();
	String Tolerance = __Tolerance_JTextField.getText().trim();
    String HandleEndpointsHow = __HandleEndpointsHow_JComboBox.getSelected();
	String AllowMissingCount = __AllowMissingCount_JTextField.getText().trim();
	/* TODO LT 2005-05-24 may enable later		
	String AllowMissingPercent = __AllowMissingPercent_JTextField.getText().trim(); */
	String OutputFillMethod = __OutputFillMethod_JComboBox.getSelected();
	String HandleMissingInputHow = __HandleMissingInputHow_JComboBox.getSelected();

	// Commit the values to the command object.
	__command.setCommandParameter ( "Alias", Alias );
	__command.setCommandParameter ( "TSID", TSID );
	__command.setCommandParameter ( "NewInterval", NewInterval );
	__command.setCommandParameter ( "OldTimeScale", OldTimeScale );
	__command.setCommandParameter ( "NewTimeScale", NewTimeScale );
	__command.setCommandParameter ( "NewDataType", NewDataType );
	__command.setCommandParameter ( "NewUnits", NewUnits );
	__command.setCommandParameter ( "Tolerance", Tolerance );
	__command.setCommandParameter ( "HandleEndpointsHow", HandleEndpointsHow );
	__command.setCommandParameter ( "AllowMissingCount", AllowMissingCount );
	/* TODO LT 2005-05-24 may enable later
	__command.setCommandParameter ( "AllowMissingPercent", AllowMissingPercent ); */
	__command.setCommandParameter ( "OutputFillMethod", OutputFillMethod );
	__command.setCommandParameter ( "HandleMissingInputHow", HandleMissingInputHow );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	
	__command       = null;
	
	__Alias_JTextField		  = null;
	__TSID_JComboBox		  = null;
	__NewInterval_JComboBox 	  = null;
	__OldTimeScale_JComboBox 	  = null;
	__AllowMissingCount_JTextField    = null;
	/* TODO SAM 2005-02-18 may enable later
	__AllowMissingPercent_JTextField  = null; */
	__NewTimeScale_JComboBox 	  = null;
	__NewDataType_JTextField 	  = null;
	__Tolerance_JTextField 	  = null;
	__HandleEndpointsHow_JComboBox = null;
	__OutputFillMethod_JComboBox 	  = null;
	__HandleMissingInputHow_JComboBox = null;
	__Command_JTextArea		  = null;
	__Command_JScrollPane		  = null;
	
	// Command Buttons
	__cancel_JButton  = null;
	__ok_JButton      = null;
	
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Vector of String containing the command.
*/
private void initialize ( JFrame parent, Command command )
{		
	__command = (ChangeInterval_Command) command;
	
	// GUI Title
	String title = "Edit " + __command.getCommandName() + "() Command";
	
	addWindowListener( this );
	
    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Create a new time series by changing the data interval of an existing time series."),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Use the alias to reference the new time series.  Data units are not changed unless specified."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The conversion process depends on whether the original and "
		+ "new time series contain accumulated, mean, or instantaneous data."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The time scales must be specified (they are not automatically determined from the data type)."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Other time series information will be copied from the original."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// Time series alias
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Time series alias:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Alias_JTextField = new JTextField ( 20 );
	__Alias_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	// List of time series available for conversion
    JGUIUtil.addComponent(main_JPanel, new JLabel("Time series to convert:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	
    List tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
    			(TSCommandProcessor)__command.getCommandProcessor(), __command );
    if ( tsids == null ) {
        tsids = new Vector();
    }
	__TSID_JComboBox = new SimpleJComboBox ( false );
	__TSID_JComboBox.setData ( tsids );
	__TSID_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __TSID_JComboBox,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// New interval
    JGUIUtil.addComponent(main_JPanel, new JLabel( "New interval:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewInterval_JComboBox = new SimpleJComboBox ( false );
	__NewInterval_JComboBox.setData (
		TimeInterval.getTimeIntervalChoices(TimeInterval.MINUTE,
			TimeInterval.YEAR,false,-1));
	// Select a default...
	__NewInterval_JComboBox.select ( 0 );
	__NewInterval_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __NewInterval_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (	"Required - Data interval for result."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	// Old time scale
    JGUIUtil.addComponent(main_JPanel, new JLabel("Old time scale:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OldTimeScale_JComboBox = new SimpleJComboBox ( false );
	List scale_Vector = MeasTimeScale.getTimeScaleChoices(true);
	//scale_Vector.insertElementAt("",0);	// Blank to not select.
	__OldTimeScale_JComboBox.setData ( scale_Vector );
	__OldTimeScale_JComboBox.select ( 0 );	// Default
	__OldTimeScale_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __OldTimeScale_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	// New time scale
    JGUIUtil.addComponent(main_JPanel, new JLabel("New time scale:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewTimeScale_JComboBox = new SimpleJComboBox ( false );
	__NewTimeScale_JComboBox.setData ( MeasTimeScale.getTimeScaleChoices(true) );
	__NewTimeScale_JComboBox.select ( 0 );	// Default
	__NewTimeScale_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __NewTimeScale_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	// New data type
    JGUIUtil.addComponent(main_JPanel,new JLabel ("New data type:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewDataType_JTextField = new JTextField ( "", 10 );
	JGUIUtil.addComponent(main_JPanel, __NewDataType_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	__NewDataType_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - Data type (default = original time series data type)."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // New units
    JGUIUtil.addComponent(main_JPanel,new JLabel ("New units:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NewUnits_JTextField = new JTextField ( "", 10 );
    JGUIUtil.addComponent(main_JPanel, __NewUnits_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __NewUnits_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - Data units (default = original time series units)."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Tolerance
    JGUIUtil.addComponent(main_JPanel,new JLabel ("Tolerance:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Tolerance_JTextField = new JTextField ( "", 10 );
    JGUIUtil.addComponent(main_JPanel, __Tolerance_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __Tolerance_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - Tolerance (default = 1% (0.01))."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Handle endpoints how?
    JGUIUtil.addComponent(main_JPanel, new JLabel("Handle endpoints how?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__HandleEndpointsHow_JComboBox = new SimpleJComboBox ( false );
	List endpoints_Vector = new Vector(4);
	endpoints_Vector.add ( "" );	// Blank is default
	endpoints_Vector.add ( __command._IncFirstOnly );
	endpoints_Vector.add ( __command._AvgEndpoints );
	__HandleEndpointsHow_JComboBox.setData ( endpoints_Vector );
	__HandleEndpointsHow_JComboBox.select ( 0 );	// Default
	__HandleEndpointsHow_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __HandleEndpointsHow_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - Indicate how to handle endpoints values in input (default=" + __command._IncFirstOnly + ")."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// Allow missing count
    JGUIUtil.addComponent(main_JPanel, new JLabel("Allow missing count:"), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__AllowMissingCount_JTextField = new JTextField ( "", 10 );
	JGUIUtil.addComponent(main_JPanel, __AllowMissingCount_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	__AllowMissingCount_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - Number of missing values allowed in original " +
		"processing interval (default=0)."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	// Allow missing percent
	/* TODO SAM 2005-02-18 may enable later
        JGUIUtil.addComponent(main_JPanel, new JLabel("Allow missing percent:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__AllowMissingPercent_JTextField = new JTextField ( "", 10 );
	JGUIUtil.addComponent(main_JPanel, __AllowMissingPercent_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	__AllowMissingPercent_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Percent (0 - 100) of missing values allowed in original " +
		"processing interval."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	*/
	// Output fill method
        JGUIUtil.addComponent(main_JPanel, new JLabel( "Output fill method:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputFillMethod_JComboBox = new SimpleJComboBox ( false );
	List fill_Vector = new Vector(3);
	fill_Vector.add ( "" );	// Blank is default
	fill_Vector.add ( __command._Interpolate );
	fill_Vector.add ( __command._Repeat );
	__OutputFillMethod_JComboBox.setData ( fill_Vector );
	__OutputFillMethod_JComboBox.select ( 0 );	// Default
	__OutputFillMethod_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __OutputFillMethod_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - Use to fill output when converting from large to small " +
		"interval (default=" + __command._Repeat + ")."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	// Handle missing input how?
    JGUIUtil.addComponent(main_JPanel, new JLabel("Handle missing input how?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__HandleMissingInputHow_JComboBox = new SimpleJComboBox ( false );
	List missing_Vector = new Vector(4);
	missing_Vector.add ( "" );	// Blank is default
	missing_Vector.add ( __command._KeepMissing );
	missing_Vector.add ( __command._Repeat );
	missing_Vector.add ( __command._SetToZero );
	__HandleMissingInputHow_JComboBox.setData ( missing_Vector );
	__HandleMissingInputHow_JComboBox.select ( 0 );	// Default
	__HandleMissingInputHow_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __HandleMissingInputHow_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - Indicate how to handle missing values in input (default=" + __command._KeepMissing + ")."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	// Command
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Command_JTextArea = new JTextArea (3,55);
	__Command_JTextArea.setLineWrap ( true );
	__Command_JTextArea.setWrapStyleWord ( true );
	__Command_JTextArea.setEditable ( false );
	__Command_JScrollPane = new JScrollPane( __Command_JTextArea );
	JGUIUtil.addComponent(main_JPanel, __Command_JScrollPane,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
	refresh();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	// Cancel button:
	__cancel_JButton = new SimpleJButton( __cancel_String, this);
	__cancel_JButton.setToolTipText( __cancel_Tip );
	button_JPanel.add ( __cancel_JButton );

	// OK button:
	__ok_JButton = new SimpleJButton(__ok_String, this);
	__ok_JButton .setToolTipText( __ok_Tip );
	button_JPanel.add ( __ok_JButton );

	// Set up the status bar.
	__statusJTextField = new JTextField();
	__statusJTextField.setEditable(false);
	JPanel statusJPanel = new JPanel();
	statusJPanel.setLayout(new GridBagLayout());
	JGUIUtil.addComponent(statusJPanel, __statusJTextField,
		0, 0, 1, 1, 1, 1,
		GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);
	getContentPane().add ( "South", statusJPanel);
	
	// Visualize it...
	if ( title != null ) {
		setTitle ( title );
	}
	
	setResizable ( true );
        pack();
        JGUIUtil.center( this );
        super.setVisible( true );
        
        __statusJTextField.setText ( " Ready" );      
}

/** Done
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent e )
{	
	refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	
	int code = event.getKeyCode();

	refresh();
	if ( code == KeyEvent.VK_ENTER ) {
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
}

/**
*/
public void keyReleased ( KeyEvent event )
{	
	refresh();
}

/**
*/
public void keyTyped ( KeyEvent event ) {
}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user cancelled.
*/
public boolean ok ()
{
	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	
	String mthd = "changeInterval_JDialog.refresh", mssg;
	
	String Alias = "";
	String TSID = "";
	String NewInterval = "";
	String NewDataType = "";
	String NewUnits = "";
	String Tolerance = "";
	String HandleEndpointsHow = "";
	String OldTimeScale = "";
	String NewTimeScale = "";
	String AllowMissingCount = "";
	/* TODO SAM 2005-02-18 may enable later
	String AllowMissingPercent = ""; */
	String OutputFillMethod	 = "";
	String HandleMissingInputHow = "";
	
	__error_wait = false;
	
	PropList props 	= null;
	
	if ( __first_time ) {
		
		__first_time = false;
		
		// Get the properties from the command
		props = __command.getCommandParameters(); 
		Alias = props.getValue( "Alias" );
		TSID = props.getValue( "TSID" );
		NewInterval = props.getValue( "NewInterval" );
		NewDataType = props.getValue( "NewDataType" );
		NewUnits = props.getValue( "NewUnits" );
		Tolerance = props.getValue( "Tolerance" );
		HandleEndpointsHow = props.getValue( "HandleEndpointsHow" );
		OldTimeScale = props.getValue( "OldTimeScale" );
		NewTimeScale = props.getValue( "NewTimeScale" );
		AllowMissingCount = props.getValue( "AllowMissingCount" );
		/* TODO SAM 2005-02-18 may enable later
		AllowMissingPercent = props.getValue( "AllowMissingPercent"  );
		*/
		OutputFillMethod = props.getValue( "OutputFillMethod" );
		HandleMissingInputHow = props.getValue( "HandleMissingInputHow");
		
		// Update Alias text field
		if ( Alias == null ) {
			__Alias_JTextField.setText ( "" );
		} else {
			__Alias_JTextField.setText ( Alias );
		}
		
		// Update TSID text field	
		// Select the item in the list.  If not a match, print a warning.
		if ( TSID == null || TSID.equals("") ) {
			// Select a default...
            if ( __TSID_JComboBox.getItemCount() > 0 ) {
                __TSID_JComboBox.select ( 0 );
            }
		} 
		else { 
			if ( JGUIUtil.isSimpleJComboBoxItem(	__TSID_JComboBox, TSID, JGUIUtil.NONE, null, null ) ) {
				__TSID_JComboBox.select ( TSID );
			}
            else {	
				mssg = "Existing command references a "
					+ "non-existent\n" + "time series \""
					+ TSID + "\".  Select a\n"
					+ "different time series or Cancel.";
				Message.printWarning ( 1, mthd, mssg );
			}
		}
		
		// Update NewInterval text field
		// Select the item in the list.  If not a match, print a warning.
		if ( NewInterval == null || NewInterval.equals("") ) {
			// Select a default...
			__NewInterval_JComboBox.select ( 0 );
		} 
		else {
			if (	JGUIUtil.isSimpleJComboBoxItem(
				__NewInterval_JComboBox, NewInterval,
				JGUIUtil.NONE, null, null ) ) {
				__NewInterval_JComboBox.select ( NewInterval );
			} else {
				mssg = "Existing " + __command.getCommandName()
					+ "() references an "
					+ "invalid\n"
					+ "NewInterval \""
					+ NewInterval + "\".  "
					+"Select a different choice or Cancel.";
				Message.printWarning ( 1, mthd, mssg );
			}
		}
		
		// Update OldTimeScale text field
		// Select the item in the list.  If not a match, print a warning.
		if ( OldTimeScale == null || OldTimeScale.equals("") ) {
			// Select default...
			__OldTimeScale_JComboBox.select ( 0 );
		} else {
			try {	
				JGUIUtil.selectTokenMatches (
					__OldTimeScale_JComboBox,
					true, " ", 0, 0,
					OldTimeScale, null );
			}
			catch ( Exception e ) {
				mssg = "Existing " + __command.getCommandName()
					+ "() references an unrecognized\n"
					+ "OldTimeScale value \""
					+ OldTimeScale
					+ "\".  Using the user value.";
				Message.printWarning ( 2, mthd, mssg );
				__OldTimeScale_JComboBox.setText (OldTimeScale);
			}
		}
		
		// Update NewTimeScale text field
		// Select the item in the list.  If not a match, print a warning.
		if ( NewTimeScale == null || NewTimeScale.equals("") ) {
			// Select default...
			__NewTimeScale_JComboBox.select ( 0 );
		} else {
			try {	
				JGUIUtil.selectTokenMatches (
					__NewTimeScale_JComboBox,
					true, " ", 0, 0,
					NewTimeScale, null );
			}
			catch ( Exception e ) {
				mssg = "Existing " + __command.getCommandName()
					+ "() references an unrecognized\n"
					+ "NewTimeScale value \""
					+ NewTimeScale
					+ "\".  Using the user value.";
				Message.printWarning ( 2, mthd, mssg );
				__NewTimeScale_JComboBox.setText (NewTimeScale);
			}
		}
		
		// Update NewDataType text field
		if ( NewDataType != null ) {
			__NewDataType_JTextField.setText ( NewDataType);
		}

        // Update new units text field
        if ( NewUnits != null ) {
            __NewUnits_JTextField.setText ( NewUnits );
        }

        // Update tolerance text field
        if ( Tolerance != null ) {
            __Tolerance_JTextField.setText ( Tolerance );
        }

        // Update HandleEndpointsHow text field
		// Select the item in the list. If not a match, print a warning.
		if ( HandleEndpointsHow == null ) {
			// Select default...
			__HandleEndpointsHow_JComboBox.select ( 0 );
		}
		else {	try {	JGUIUtil.selectTokenMatches (
				__HandleEndpointsHow_JComboBox,
					true, " ", 0, 0,
					HandleEndpointsHow, null );
			}
			catch ( Exception e ) {
				mssg = "Existing " + __command.getCommandName()
					+ "() references an unrecognized\n"
					+ "HandleEndpointsHow value \""
					+ HandleEndpointsHow
					+ "\".  Using the user value.";
				Message.printWarning ( 2, mthd, mssg );
				__HandleEndpointsHow_JComboBox.
					setText ( HandleEndpointsHow );
			}
		}

		// Update AllowMissingCount text field
		if ( AllowMissingCount != null ) {
			__AllowMissingCount_JTextField.setText (
				AllowMissingCount );
		}
		
		// Update AllowMissingPercent text field
		/* TODO SAM 2005-02-18 may enable later
		if ( AllowMissingPercent != null ) {
			__AllowMissingPercent_JTextField.setText (
				AllowMissingPercent );
		}
		*/
		// Update OutputFillMethod text field
		// Select the item in the list. If not a match, print a warning.
		if ( OutputFillMethod == null ) {
			// Select default...
			__OutputFillMethod_JComboBox.select ( 0 );
		} else {
			try {	
				JGUIUtil.selectTokenMatches (
					__OutputFillMethod_JComboBox,
					true, " ", 0, 0,
					OutputFillMethod, null );
			}
			catch ( Exception e ) {
				mssg = "Existing " + __command.getCommandName()
					+  "() references an unrecognized\n"
					+ "OutputFillMethod value \""
					+ OutputFillMethod
					+ "\".  Using the user value.";
				Message.printWarning ( 2, mthd, mssg );
				__OutputFillMethod_JComboBox.setText (
					OutputFillMethod );
			}
		}
		
		// Update HandleMissingInputHow text field
		// Select the item in the list. If not a match, print a warning.
		if ( HandleMissingInputHow == null ) {
			// Select default...
			__HandleMissingInputHow_JComboBox.select ( 0 );
		}
		else {	try {	JGUIUtil.selectTokenMatches (
				__HandleMissingInputHow_JComboBox,
					true, " ", 0, 0,
					HandleMissingInputHow, null );
			}
			catch ( Exception e ) {
				mssg = "Existing " + __command.getCommandName()
					+ "() references an unrecognized\n"
					+ "HandleMissingInputHow value \""
					+ HandleMissingInputHow
					+ "\".  Using the user value.";
				Message.printWarning ( 2, mthd, mssg );
				__HandleMissingInputHow_JComboBox.
					setText ( HandleMissingInputHow );
			}
		}
	}
	
	// Regardless, reset the command from the fields.  This is only  visible
	// information that has not been committed in the command.
	Alias = __Alias_JTextField.getText().trim();
	TSID = __TSID_JComboBox.getSelected();
	NewInterval = __NewInterval_JComboBox.getSelected();
	OldTimeScale = StringUtil.getToken(	__OldTimeScale_JComboBox.getSelected(), " ", 0, 0 );
	NewTimeScale = StringUtil.getToken( __NewTimeScale_JComboBox.getSelected(), " ", 0, 0 );
	NewDataType = __NewDataType_JTextField.getText().trim();
	NewUnits = __NewUnits_JTextField.getText().trim();
	Tolerance = __Tolerance_JTextField.getText().trim();
    HandleEndpointsHow = __HandleEndpointsHow_JComboBox.getSelected();
	AllowMissingCount = __AllowMissingCount_JTextField.getText().trim();
	/* TODO LT 2005-05-24 may enable later		
	AllowMissingPercent = __AllowMissingPercent_JTextField.getText().trim(); */
	OutputFillMethod = __OutputFillMethod_JComboBox.getSelected();
	HandleMissingInputHow =	__HandleMissingInputHow_JComboBox.getSelected();
	
	// And set the command properties.
	props = new PropList ( __command.getCommandName() );
	props.add ( "Alias=" + Alias );
	props.add ( "TSID=" + TSID );
	props.add ( "NewInterval=" + NewInterval );
	props.add ( "OldTimeScale=" + OldTimeScale );
	props.add ( "NewTimeScale=" + NewTimeScale );
	props.add ( "NewDataType=" + NewDataType );
	props.add ( "NewUnits=" + NewUnits );
	props.add ( "Tolerance=" + Tolerance );
	props.add ( "HandleEndpointsHow=" + HandleEndpointsHow );
	props.add ( "AllowMissingCount=" + AllowMissingCount );
	/* TODO LT 2005-05-24 may enable later	
	props.add ( "AllowMissingPercent=" + AllowMissingPercent   ); */
	props.add ( "OutputFillMethod=" + OutputFillMethod );
	props.add ( "HandleMissingInputHow=" + HandleMissingInputHow );
	
	__Command_JTextArea.setText( __command.toString(props) );
}

/**
React to the user response.
@param ok if false, then the edit is cancelled.  If true, the edit is committed
and the dialog is closed.
*/
public void response ( boolean ok )
{	
	__ok = ok;
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
{	
	response ( false );
}

public void windowActivated	( WindowEvent evt ){;}
public void windowClosed	( WindowEvent evt ){;}
public void windowDeactivated	( WindowEvent evt ){;}
public void windowDeiconified	( WindowEvent evt ){;}
public void windowIconified	( WindowEvent evt ){;}
public void windowOpened	( WindowEvent evt ){;}

} // end changeInterval_JDialog
