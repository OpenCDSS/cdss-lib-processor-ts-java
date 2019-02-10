// ChangeIntervalIrregularToRegular_JDialog - editor dialog for ChangeIntervalIrregularToRegular command

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
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
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
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.TS.TSUtil_ChangeInterval;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.TimeScaleType;
import RTi.Util.Time.YearType;

@SuppressWarnings("serial")
public class ChangeIntervalIrregularToRegular_JDialog extends JDialog
	implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
// Controls are defined in logical order -- The order they appear in the dialog
// box and documentation.
private ChangeIntervalIrregularToRegular_Command __command = null;// Command object.

private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private JLabel __NewEnsembleID_JLabel = null;
private JTextField __NewEnsembleID_JTextField;
private JLabel __NewEnsembleName_JLabel = null;
private JTextField __NewEnsembleName_JTextField;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private SimpleJComboBox	__NewInterval_JComboBox = null;
private SimpleJComboBox	__OldTimeScale_JComboBox = null;
private SimpleJComboBox	__NewTimeScale_JComboBox = null;
private JLabel __Statistic_JLabel = null;
private SimpleJComboBox __Statistic_JComboBox = null;
private JLabel  __OutputYearType_JLabel = null;
private SimpleJComboBox __OutputYearType_JComboBox = null;
private JTextField __NewDataType_JTextField = null;
private JTextField __NewUnits_JTextField = null;
private JLabel __Tolerance_JLabel = null;
private JTextField __Tolerance_JTextField = null;
private JLabel __HandleEndpointsHow_JLabel = null;
private SimpleJComboBox __HandleEndpointsHow_JComboBox = null;
private JLabel __AllowMissingCount_JLabel = null;
private JTextField __AllowMissingCount_JTextField = null;
private JLabel __AllowMissingConsecutive_JLabel = null;
private JTextField __AllowMissingConsecutive_JTextField = null;
/* TODO SAM 2005-02-18 may enable later
private JTextField	__AllowMissingPercent_JTextField = null; // % missing to allow in input when converting.
*/
private JLabel __OutputFillMethod_JLabel = null;
private SimpleJComboBox	__OutputFillMethod_JComboBox = null; // Fill method when going from large to small interval.
private JLabel __HandleMissingInputHow_JLabel = null;
private SimpleJComboBox	__HandleMissingInputHow_JComboBox = null;
						// How to handle missing data in input time series.
private SimpleJComboBox __RecalcLimits_JComboBox = null;
private JTextArea __Command_JTextArea   = null;
private JScrollPane	__Command_JScrollPane = null;
private SimpleJButton __cancel_JButton = null;
private String __cancel_String  = "Cancel";
private String __cancel_Tip = "Close the window, without returning the command.";
private SimpleJButton __ok_JButton = null;
private String __ok_String  = "OK";
private String __ok_Tip = "Close the window, returning the command.";
				
private boolean __error_wait = false;	// Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false;						

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to parse.
*/
public ChangeIntervalIrregularToRegular_JDialog ( JFrame parent, ChangeIntervalIrregularToRegular_Command command )
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

// Start event handlers for DocumentListener...

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void changedUpdate ( DocumentEvent e )
{   checkGUIState();
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void insertUpdate ( DocumentEvent e )
{   checkGUIState();
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void removeUpdate ( DocumentEvent e )
{   checkGUIState();
    refresh();
}

// ...End event handlers for DocumentListener

/**
Check the GUI state to make sure that appropriate components are enabled/disabled.
*/
private void checkGUIState ()
{   String routine = "ChangeInterval_JDialog.checkGUIState";

    // Handle TSList-related parameter editing...

    String TSList = __TSList_JComboBox.getSelected();
    if ( TSListType.ALL_MATCHING_TSID.equals(TSList) ||
        TSListType.FIRST_MATCHING_TSID.equals(TSList) ||
        TSListType.LAST_MATCHING_TSID.equals(TSList) ) {
        __TSID_JComboBox.setEnabled(true);
        __TSID_JLabel.setEnabled ( true );
    }
    else {
        __TSID_JComboBox.setEnabled(false);
        __TSID_JLabel.setEnabled ( false );
    }
    if ( TSListType.ENSEMBLE_ID.equals(TSList)) {
        __EnsembleID_JLabel.setEnabled ( true );
        __EnsembleID_JComboBox.setEnabled(true);
        __NewEnsembleID_JLabel.setEnabled ( true );
        __NewEnsembleID_JTextField.setEnabled(true);
        __NewEnsembleName_JLabel.setEnabled ( true );
        __NewEnsembleName_JTextField.setEnabled(true);
    }
    else {
        __EnsembleID_JLabel.setEnabled ( false );
        __EnsembleID_JComboBox.setEnabled(false);
        __NewEnsembleID_JLabel.setEnabled ( false );
        __NewEnsembleID_JTextField.setEnabled(false);
        __NewEnsembleName_JLabel.setEnabled ( false );
        __NewEnsembleName_JTextField.setEnabled(false);
    }

    // initially set the following to gray and only enable based on input and output scale.

    __OutputYearType_JLabel.setEnabled ( false );
    __OutputYearType_JComboBox.setEnabled ( false );
    __Tolerance_JLabel.setEnabled ( false );
    __Tolerance_JTextField.setEnabled ( false );
    __HandleEndpointsHow_JLabel.setEnabled ( false );
    __HandleEndpointsHow_JComboBox.setEnabled ( false );
    __AllowMissingCount_JLabel.setEnabled ( false );
    __AllowMissingCount_JTextField.setEnabled ( false );
    __AllowMissingConsecutive_JLabel.setEnabled ( false );
    __AllowMissingConsecutive_JTextField.setEnabled ( false );
    __OutputFillMethod_JLabel.setEnabled ( false );
    __OutputFillMethod_JComboBox.setEnabled ( false );
    __HandleMissingInputHow_JLabel.setEnabled ( false );
    __HandleMissingInputHow_JComboBox.setEnabled ( false );

    String newInterval = __NewInterval_JComboBox.getSelected();
    TimeInterval newIntervalObj = null;
    try {
        newIntervalObj = TimeInterval.parseInterval(newInterval);
    }
    catch ( Exception e ) {
        // Should not happen because choices are valid
        Message.printWarning ( 3, routine, e );
    }
    String oldTimeScale = __OldTimeScale_JComboBox.getSelected();
    TimeScaleType oldTimeScaleType = TimeScaleType.valueOfIgnoreCase(oldTimeScale);
    String newTimeScale = __NewTimeScale_JComboBox.getSelected();
    TimeScaleType newTimeScaleType = TimeScaleType.valueOfIgnoreCase(newTimeScale);
    String statistic = __Statistic_JComboBox.getSelected().trim();
    String outputYearType0 = __OutputYearType_JComboBox.getSelected();
    YearType outputYearType = null;
    if ( (outputYearType0 != null) && !outputYearType0.equals("") ) {
        outputYearType = YearType.valueOfIgnoreCase(outputYearType0);
    }

    if ( (oldTimeScaleType == TimeScaleType.MEAN) && (newTimeScaleType == TimeScaleType.INST) ) {
        __Tolerance_JLabel.setEnabled( true );
        __Tolerance_JTextField.setEnabled( true );
        __HandleMissingInputHow_JLabel.setEnabled ( true );
        __HandleMissingInputHow_JComboBox.setEnabled ( true );
    }
    else if (( (oldTimeScaleType == TimeScaleType.MEAN) || (oldTimeScaleType == TimeScaleType.ACCM)) &&
        (newTimeScaleType == TimeScaleType.MEAN) || (newTimeScaleType == TimeScaleType.ACCM)) {
        __AllowMissingCount_JLabel.setEnabled ( true );
        __AllowMissingCount_JTextField.setEnabled ( true );
        __AllowMissingConsecutive_JLabel.setEnabled ( true );
        __AllowMissingConsecutive_JTextField.setEnabled ( true );
        __HandleMissingInputHow_JLabel.setEnabled ( true );
        __HandleMissingInputHow_JComboBox.setEnabled ( true );
        __OutputFillMethod_JLabel.setEnabled ( true );
        __OutputFillMethod_JComboBox.setEnabled ( true );
        __HandleEndpointsHow_JLabel.setEnabled ( true );
        __HandleEndpointsHow_JComboBox.setEnabled ( true );
    }
    else if ( (oldTimeScaleType == TimeScaleType.INST) && (newTimeScaleType == TimeScaleType.INST) ) {
        __HandleMissingInputHow_JLabel.setEnabled ( true );
        __HandleMissingInputHow_JComboBox.setEnabled ( true );
    }
    else if ( (oldTimeScaleType == TimeScaleType.INST) && (newTimeScaleType == TimeScaleType.MEAN) ) {
        __AllowMissingCount_JLabel.setEnabled ( true );
        __AllowMissingCount_JTextField.setEnabled ( true );
        __AllowMissingConsecutive_JLabel.setEnabled ( true );
        __AllowMissingConsecutive_JTextField.setEnabled ( true );
        __HandleMissingInputHow_JLabel.setEnabled ( true );
        __HandleMissingInputHow_JComboBox.setEnabled ( true );
        __OutputFillMethod_JLabel.setEnabled ( true );
        __OutputFillMethod_JComboBox.setEnabled ( true );
        __HandleEndpointsHow_JLabel.setEnabled ( true );
        __HandleEndpointsHow_JComboBox.setEnabled ( true );
    }
    
    // Converting to yearly time series has some special handling
    if ( newInterval.equalsIgnoreCase("Year") ) {
        __OutputYearType_JLabel.setEnabled ( true );
        __OutputYearType_JComboBox.setEnabled ( true );
        if ( (outputYearType != null) && (outputYearType != YearType.CALENDAR) ) {
            // Going through special code so don't use the advanced options
            __Tolerance_JLabel.setEnabled ( false );
            __Tolerance_JTextField.setEnabled ( false );
            __HandleMissingInputHow_JLabel.setEnabled ( false );
            __HandleMissingInputHow_JComboBox.setEnabled ( false );
            __OutputFillMethod_JLabel.setEnabled ( false );
            __OutputFillMethod_JComboBox.setEnabled ( false );
            __HandleEndpointsHow_JLabel.setEnabled ( false );
            __HandleEndpointsHow_JComboBox.setEnabled ( false );
            
            __AllowMissingCount_JLabel.setEnabled ( true );
            __AllowMissingCount_JTextField.setEnabled ( true );
            __AllowMissingConsecutive_JLabel.setEnabled ( true );
            __AllowMissingConsecutive_JTextField.setEnabled ( true );
        }
    }
    
    // More specific handling of the HandleEndPointsHow parameter...
    
    if ( (oldTimeScaleType == TimeScaleType.INST) && (newTimeScaleType == TimeScaleType.MEAN) &&
        (newIntervalObj.getBase() <= TimeInterval.DAY)  ) {
        // Would also like to check the following but it is not available until runtime...
        // && (oldIntervalBase < TimeInterval.DAY)
        __HandleEndpointsHow_JLabel.setEnabled ( true );
        __HandleEndpointsHow_JComboBox.setEnabled ( true );
    }
    else {
        // Need to disable
        __HandleEndpointsHow_JLabel.setEnabled ( false );
        __HandleEndpointsHow_JComboBox.setEnabled ( false );
    }
    
    // More specific handling of the OutputFillMethod parameter...
    
    if ( (oldTimeScaleType == TimeScaleType.INST) && (newTimeScaleType == TimeScaleType.MEAN) ) {
        // Would also like to check the following but it is not available until runtime...
        // && long to short
        __OutputFillMethod_JLabel.setEnabled ( true );
        __OutputFillMethod_JComboBox.setEnabled ( true );
    }
    else {
        // Need to disable
        __OutputFillMethod_JLabel.setEnabled ( false );
        __OutputFillMethod_JComboBox.setEnabled ( false );
    }
    
    // Statistic is only implemented for instantaneous to instantaneous time scale and impacts some parameters.
    if ( (oldTimeScaleType == TimeScaleType.INST) && (newTimeScaleType == TimeScaleType.INST) ) {
        __Statistic_JLabel.setEnabled ( true );
        __Statistic_JComboBox.setEnabled ( true );
    }
    else {
        __Statistic_JLabel.setEnabled ( false );
        __Statistic_JComboBox.setEnabled ( false );
    }
    
    // If the statistic is enabled and has a value set, also allow the number of missing allowed to be set
    if ( __Statistic_JComboBox.isEnabled() ) {
        if ( (statistic != null) && (statistic.length() > 0) ) {
            __AllowMissingCount_JLabel.setEnabled ( true );
            __AllowMissingCount_JTextField.setEnabled ( true );
            __AllowMissingConsecutive_JLabel.setEnabled ( true );
            __AllowMissingConsecutive_JTextField.setEnabled ( true );
        }
    }
}

/**
Check the user input for errors and set __error_wait accordingly.
*/
private void checkInput ()
{	// Get the values from the interface.
    String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String NewEnsembleID = __NewEnsembleID_JTextField.getText().trim();
    String NewEnsembleName = __NewEnsembleName_JTextField.getText().trim();
	String Alias = __Alias_JTextField.getText().trim();
	String NewInterval  = __NewInterval_JComboBox.getSelected();
	String OldTimeScale = StringUtil.getToken( __OldTimeScale_JComboBox.getSelected(), " ", 0, 0 );
	String NewTimeScale  = StringUtil.getToken( __NewTimeScale_JComboBox.getSelected(), " ", 0, 0 );
    String Statistic = __Statistic_JComboBox.getSelected();
    String OutputYearType = __OutputYearType_JComboBox.getSelected();
	String NewDataType = __NewDataType_JTextField.getText().trim();
	String NewUnits = __NewUnits_JTextField.getText().trim();
	String Tolerance = __Tolerance_JTextField.getText().trim();
    String HandleEndpointsHow = __HandleEndpointsHow_JComboBox.getSelected();
	String AllowMissingCount = __AllowMissingCount_JTextField.getText().trim();
	/* TODO LT 2005-05-24 may enable later		
	String AllowMissingPercent = __AllowMissingPercent_JTextField.getText().trim(); */
	String AllowMissingConsecutive = __AllowMissingConsecutive_JTextField.getText().trim();
	String OutputFillMethod = __OutputFillMethod_JComboBox.getSelected();
	String HandleMissingInputHow = __HandleMissingInputHow_JComboBox.getSelected();
	String RecalcLimits = __RecalcLimits_JComboBox.getSelected();
	
	// Put together the list of parameters to check...
	PropList props = new PropList ( "" );
    if ( TSList.length() > 0 ) {
        props.set ( "TSList", TSList );
    }
    if ( TSID.length() > 0 ) {
        props.set ( "TSID", TSID );
    }
    if ( EnsembleID.length() > 0 ) {
        props.set ( "EnsembleID", EnsembleID );
    }
    if ( NewEnsembleID.length() > 0 ) {
        props.set ( "NewEnsembleID", NewEnsembleID );
    }
    if ( NewEnsembleName.length() > 0 ) {
        props.set ( "NewEnsembleName", NewEnsembleName );
    }
	if ( Alias != null && Alias.length() > 0 ) {
		props.set( "Alias", Alias );
	}
	if ( NewInterval != null && NewInterval.length() > 0 ) {
		props.set( "NewInterval", NewInterval );
	}
	if ( OldTimeScale != null && OldTimeScale.length() > 0 ) {
		props.set( "OldTimeScale", OldTimeScale );
	}
	if ( NewTimeScale != null && NewTimeScale.length() > 0 ) {
		props.set( "NewTimeScale", NewTimeScale );
	}
    if ( Statistic.length() > 0 ) {
        props.set ( "Statistic", Statistic );
    }
    if ( OutputYearType.length() > 0 ) {
        props.set ( "OutputYearType", OutputYearType );
    }
	if ( NewDataType != null && NewDataType.length() > 0 ) {
		props.set( "NewDataType", NewDataType );
	}
	if ( NewUnits != null && NewUnits.length() > 0 ) {
	     props.set( "NewUnits", NewUnits );
	}
    if ( Tolerance != null && Tolerance.length() > 0 ) {
	     props.set( "Tolerance", Tolerance );
	}
	if ( HandleEndpointsHow != null &&
	     HandleEndpointsHow.length() > 0 ) {
		props.set( "HandleEndpointsHow", HandleEndpointsHow );
	}
	if ( AllowMissingCount != null && AllowMissingCount.length() > 0 ) {
		props.set( "AllowMissingCount", AllowMissingCount );
	}
	// AllowMissingPercent
	/* TODO LT 2005-05-24 may enable later
	if ( AllowMissingPercent != null && AllowMissingPercent.length() > 0 ) {
		props.set( "AllowMissingPercent", AllowMissingPercent );
	} */
    if ( AllowMissingConsecutive != null && AllowMissingConsecutive.length() > 0 ) {
        props.set( "AllowMissingConsecutive", AllowMissingConsecutive );
    }
	if ( OutputFillMethod != null && OutputFillMethod.length() > 0 ) {
		props.set( "OutputFillMethod", OutputFillMethod );
	}
	if ( HandleMissingInputHow != null &&
	    HandleMissingInputHow.length() > 0 ) {
		props.set( "HandleMissingInputHow", HandleMissingInputHow );
	}
    if ( RecalcLimits.length() > 0 ) {
        props.set( "RecalcLimits", RecalcLimits );
    }
	
	// Check the list of Command Parameters.
	try {
	    // This will warn the user...
		__command.checkCommandParameters ( props, null, 1 );
		__error_wait = false;
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
{
	// Get the values from the interface.
    String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String NewEnsembleID = __NewEnsembleID_JTextField.getText().trim();
    String NewEnsembleName = __NewEnsembleName_JTextField.getText().trim();
	String Alias = __Alias_JTextField.getText().trim();
	String NewInterval = __NewInterval_JComboBox.getSelected();
	String OldTimeScale = StringUtil.getToken( __OldTimeScale_JComboBox.getSelected(), " ", 0, 0 );
	String NewTimeScale = StringUtil.getToken( __NewTimeScale_JComboBox.getSelected(), " ", 0, 0 );
    String Statistic = __Statistic_JComboBox.getSelected();
	String OutputYearType = __OutputYearType_JComboBox.getSelected();
	String NewDataType = __NewDataType_JTextField.getText().trim();
	String NewUnits = __NewUnits_JTextField.getText().trim();
	String Tolerance = __Tolerance_JTextField.getText().trim();
    String HandleEndpointsHow = __HandleEndpointsHow_JComboBox.getSelected();
	String AllowMissingCount = __AllowMissingCount_JTextField.getText().trim();
	/* TODO LT 2005-05-24 may enable later		
	String AllowMissingPercent = __AllowMissingPercent_JTextField.getText().trim(); */
	String AllowMissingConsecutive = __AllowMissingConsecutive_JTextField.getText().trim();
	String OutputFillMethod = __OutputFillMethod_JComboBox.getSelected();
	String HandleMissingInputHow = __HandleMissingInputHow_JComboBox.getSelected();
	String RecalcLimits = __RecalcLimits_JComboBox.getSelected();

	// Commit the values to the command object.
    __command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "NewEnsembleID", NewEnsembleID );
    __command.setCommandParameter ( "NewEnsembleName", NewEnsembleName );
	__command.setCommandParameter ( "Alias", Alias );
	__command.setCommandParameter ( "NewInterval", NewInterval );
	__command.setCommandParameter ( "OldTimeScale", OldTimeScale );
	__command.setCommandParameter ( "NewTimeScale", NewTimeScale );
    __command.setCommandParameter ( "Statistic", Statistic );
	__command.setCommandParameter ( "OutputYearType", OutputYearType );
	__command.setCommandParameter ( "NewDataType", NewDataType );
	__command.setCommandParameter ( "NewUnits", NewUnits );
	__command.setCommandParameter ( "Tolerance", Tolerance );
	__command.setCommandParameter ( "HandleEndpointsHow", HandleEndpointsHow );
	__command.setCommandParameter ( "AllowMissingCount", AllowMissingCount );
	/* TODO LT 2005-05-24 may enable later
	__command.setCommandParameter ( "AllowMissingPercent", AllowMissingPercent ); */
	__command.setCommandParameter ( "AllowMissingConsecutive", AllowMissingConsecutive );
	__command.setCommandParameter ( "OutputFillMethod", OutputFillMethod );
	__command.setCommandParameter ( "HandleMissingInputHow", HandleMissingInputHow );
	__command.setCommandParameter ( "RecalcLimits", RecalcLimits );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Vector of String containing the command.
*/
private void initialize ( JFrame parent, ChangeIntervalIrregularToRegular_Command command )
{		
	__command = command;
	
	// GUI Title
	String title = "Edit " + __command.getCommandName() + "() Command";
	
	addWindowListener( this );
	
    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"<html><b>This command is under development</b></html>"),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Create new time series by changing the data interval of each input time series from irregular to regular interval."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The output time series will have an identifier that is the same as the input, but with the new interval."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"If the input is an ensemble, then a new ensemble can be created for output."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JPanel tslist_JPanel = new JPanel();
    tslist_JPanel.setBorder(BorderFactory.createTitledBorder (
        BorderFactory.createLineBorder(Color.black),"Time series to process"));
    tslist_JPanel.setLayout( new GridBagLayout() );
    
    int yTslist = 0;
    __TSList_JComboBox = new SimpleJComboBox(false);
    yTslist = CommandEditorUtil.addTSListToEditorDialogPanel ( this, tslist_JPanel, __TSList_JComboBox, yTslist );

    __TSID_JLabel = new JLabel ("TSID (for TSList=*TSID:");
    __TSID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __TSID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yTslist = CommandEditorUtil.addTSIDToEditorDialogPanel (
        this, this, tslist_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, yTslist );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __EnsembleID_JComboBox.setToolTipText("Select an ensemble identifier from the list or specify with ${Property} notation");
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yTslist = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
        this, this, tslist_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, yTslist );
    
    JGUIUtil.addComponent(main_JPanel, tslist_JPanel,
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);
    
    __NewEnsembleID_JLabel = new JLabel ( "New ensemble ID:" );
    JGUIUtil.addComponent(main_JPanel, __NewEnsembleID_JLabel, 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NewEnsembleID_JTextField = new JTextField ( "", 20 );
    __NewEnsembleID_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __NewEnsembleID_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Optional - to create ensemble when input is an ensemble."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    __NewEnsembleName_JLabel = new JLabel ( "New ensemble name:" );
    JGUIUtil.addComponent(main_JPanel, __NewEnsembleName_JLabel,
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NewEnsembleName_JTextField = new JTextField ( "", 30 );
    __NewEnsembleName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __NewEnsembleName_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Optional - name for new ensemble."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
	// Time series alias
    JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(15);
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener( this );
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - use %L for location, etc."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	// New interval
    JGUIUtil.addComponent(main_JPanel, new JLabel( "New interval:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewInterval_JComboBox = new SimpleJComboBox ( false );
	__NewInterval_JComboBox.setData (
		TimeInterval.getTimeIntervalChoices(TimeInterval.MINUTE, TimeInterval.YEAR,false,-1));
	// Select a default...
	__NewInterval_JComboBox.select ( 0 );
	__NewInterval_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __NewInterval_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (	"Required - data interval for result."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	// Old time scale
    JGUIUtil.addComponent(main_JPanel, new JLabel("Old time scale:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OldTimeScale_JComboBox = new SimpleJComboBox ( false );
	__OldTimeScale_JComboBox.setData ( TimeScaleType.getTimeScaleChoicesAsStrings(true) );
	__OldTimeScale_JComboBox.select ( 0 );	// Default
	__OldTimeScale_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __OldTimeScale_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required - indicates how to process data."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	// New time scale
    JGUIUtil.addComponent(main_JPanel, new JLabel("New time scale:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewTimeScale_JComboBox = new SimpleJComboBox ( false );
	__NewTimeScale_JComboBox.setData ( TimeScaleType.getTimeScaleChoicesAsStrings(true) );
	__NewTimeScale_JComboBox.select ( 0 );	// Default
	__NewTimeScale_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __NewTimeScale_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required - indicates how to process data."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    __Statistic_JLabel = new JLabel ( "Statistic to calculate:" );
    JGUIUtil.addComponent(main_JPanel, __Statistic_JLabel, 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Statistic_JComboBox = new SimpleJComboBox ( false );    // Do not allow edit
    List<String> statisticList = TSUtil_ChangeInterval.getStatisticChoicesAsStrings();
    statisticList.add ( 0, "" ); // Blank as default
    __Statistic_JComboBox.setData ( statisticList );
    __Statistic_JComboBox.addItemListener ( this );
    //__Statistic_JComboBox.setMaximumRowCount(statisticChoices.size());
    JGUIUtil.addComponent(main_JPanel, __Statistic_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - limited support for " + TimeScaleType.INST + " to " + TimeScaleType.INST +
        " (default statistic is from old/new time scale)."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    __OutputYearType_JLabel = new JLabel ( "Output year type:" );
    JGUIUtil.addComponent(main_JPanel, __OutputYearType_JLabel, 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputYearType_JComboBox = new SimpleJComboBox ( false );
    __OutputYearType_JComboBox.add ( "" );
    __OutputYearType_JComboBox.add ( "" + YearType.CALENDAR );
    __OutputYearType_JComboBox.add ( "" + YearType.NOV_TO_OCT );
    __OutputYearType_JComboBox.add ( "" + YearType.WATER );
    __OutputYearType_JComboBox.add ( "" + YearType.YEAR_MAY_TO_APR );
    __OutputYearType_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __OutputYearType_JComboBox,
        1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - use only when old interval is day or month and new interval is Year (default=" +
        YearType.CALENDAR + ")."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	// New data type
    JGUIUtil.addComponent(main_JPanel,new JLabel ("New data type:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewDataType_JTextField = new JTextField ( "", 10 );
	JGUIUtil.addComponent(main_JPanel, __NewDataType_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	__NewDataType_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - data type (default = original time series data type)."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // New units
    JGUIUtil.addComponent(main_JPanel,new JLabel ("New units:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NewUnits_JTextField = new JTextField ( "", 10 );
    JGUIUtil.addComponent(main_JPanel, __NewUnits_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __NewUnits_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - data units (default = original time series units)."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Tolerance
    __Tolerance_JLabel = new JLabel ("Tolerance:" );
    JGUIUtil.addComponent(main_JPanel, __Tolerance_JLabel,
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Tolerance_JTextField = new JTextField ( "", 10 );
    JGUIUtil.addComponent(main_JPanel, __Tolerance_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __Tolerance_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - convergence tolerance (default = 0.01)."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Handle endpoints how?
    __HandleEndpointsHow_JLabel = new JLabel("Handle endpoints how?:");
    JGUIUtil.addComponent(main_JPanel, __HandleEndpointsHow_JLabel,
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__HandleEndpointsHow_JComboBox = new SimpleJComboBox ( false );
	List<String> endpoints_Vector = new Vector<String>(4);
	endpoints_Vector.add ( "" );	// Blank is default
	endpoints_Vector.add ( __command._AverageEndpoints );
	endpoints_Vector.add ( __command._IncludeFirstOnly );
	__HandleEndpointsHow_JComboBox.setData ( endpoints_Vector );
	__HandleEndpointsHow_JComboBox.select ( 0 );	// Default
	__HandleEndpointsHow_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __HandleEndpointsHow_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - for INST to MEAN, small to large, new interval=day or less (default=" +
		__command._AverageEndpoints + ")."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// Allow missing count
    __AllowMissingCount_JLabel = new JLabel("Allow missing count:");
    JGUIUtil.addComponent(main_JPanel, __AllowMissingCount_JLabel,
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__AllowMissingCount_JTextField = new JTextField ( "", 10 );
	JGUIUtil.addComponent(main_JPanel, __AllowMissingCount_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	__AllowMissingCount_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - number of missing values allowed in input interval (default=0)."),
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
    
    // Allow missing consecutive
    __AllowMissingConsecutive_JLabel = new JLabel("Allow consecutive missing:");
    JGUIUtil.addComponent(main_JPanel, __AllowMissingConsecutive_JLabel,
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AllowMissingConsecutive_JTextField = new JTextField ( "", 10 );
    JGUIUtil.addComponent(main_JPanel, __AllowMissingConsecutive_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __AllowMissingConsecutive_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - number of consecutive missing values allowed in input interval (default=AllowMissingCount)."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// Output fill method
    __OutputFillMethod_JLabel = new JLabel( "Output fill method:");
    JGUIUtil.addComponent(main_JPanel, __OutputFillMethod_JLabel,
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputFillMethod_JComboBox = new SimpleJComboBox ( false );
	List<String> fill_Vector = new Vector<String>(3);
	fill_Vector.add ( "" );	// Blank is default
	fill_Vector.add ( __command._Interpolate );
	fill_Vector.add ( __command._Repeat );
	__OutputFillMethod_JComboBox.setData ( fill_Vector );
	__OutputFillMethod_JComboBox.select ( 0 );	// Default
	__OutputFillMethod_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __OutputFillMethod_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - use when converting from INST to MEAN, large to small interval (default=" + __command._Repeat + ")."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	// Handle missing input how?
    __HandleMissingInputHow_JLabel = new JLabel("Handle missing input how?:");
    JGUIUtil.addComponent(main_JPanel, __HandleMissingInputHow_JLabel,
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__HandleMissingInputHow_JComboBox = new SimpleJComboBox ( false );
	List<String> missing_Vector = new Vector<String>(4);
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
		"Optional - how to handle missing values in input (default=" + __command._KeepMissing + ")."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Recalculate limits:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __RecalcLimits_JComboBox = new SimpleJComboBox ( false );
    List<String> recalcChoices = new ArrayList<String>();
    recalcChoices.add ( "" );
    recalcChoices.add ( __command._False );
    recalcChoices.add ( __command._True );
    __RecalcLimits_JComboBox.setData(recalcChoices);
    __RecalcLimits_JComboBox.select ( 0 );
    __RecalcLimits_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __RecalcLimits_JComboBox,
    1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel,
        new JLabel( "Optional - recalculate original data limits after set (default=" + __command._False + ")."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	// Command
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Command_JTextArea = new JTextArea (5,55);
	__Command_JTextArea.setLineWrap ( true );
	__Command_JTextArea.setWrapStyleWord ( true );
	__Command_JTextArea.setEditable ( false );
	__Command_JScrollPane = new JScrollPane( __Command_JTextArea );
	JGUIUtil.addComponent(main_JPanel, __Command_JScrollPane,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
    checkGUIState();
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
	
	// Visualize it...
	if ( title != null ) {
		setTitle ( title );
	}
	
	setResizable ( true );
    pack();
    JGUIUtil.center( this );
    super.setVisible( true );   
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent e )
{
    checkGUIState();
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
	String mthd = "ChangeInterval_JDialog.refresh", mssg;
    String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String NewEnsembleID = "";
    String NewEnsembleName = "";
	String Alias = "";
	String NewInterval = "";
	String OldTimeScale = "";
	String NewTimeScale = "";
    String Statistic = "";
	String OutputYearType = "";
	String NewDataType = "";
	String NewUnits = "";
	String Tolerance = "";
	String HandleEndpointsHow = "";
	String AllowMissingCount = "";
	String AllowMissingConsecutive = "";
	/* TODO SAM 2005-02-18 may enable later
	String AllowMissingPercent = ""; */
	String OutputFillMethod	 = "";
	String HandleMissingInputHow = "";
	String RecalcLimits = "";
	
	__error_wait = false;
	PropList props 	= null;
	if ( __first_time ) {
		__first_time = false;
		// Get the properties from the command
		props = __command.getCommandParameters();
        TSList = props.getValue ( "TSList" );
        TSID = props.getValue ( "TSID" );
        EnsembleID = props.getValue ( "EnsembleID" );
        NewEnsembleID = props.getValue ( "NewEnsembleID" );
        NewEnsembleName = props.getValue ( "NewEnsembleName" );
		Alias = props.getValue( "Alias" );
		NewInterval = props.getValue( "NewInterval" );
	    OldTimeScale = props.getValue( "OldTimeScale" );
	    NewTimeScale = props.getValue( "NewTimeScale" );
	    Statistic = props.getValue ( "Statistic" );
		OutputYearType = props.getValue ( "OutputYearType" );
		NewDataType = props.getValue( "NewDataType" );
		NewUnits = props.getValue( "NewUnits" );
		Tolerance = props.getValue( "Tolerance" );
		HandleEndpointsHow = props.getValue( "HandleEndpointsHow" );
		AllowMissingCount = props.getValue( "AllowMissingCount" );
		/* TODO SAM 2005-02-18 may enable later
		AllowMissingPercent = props.getValue( "AllowMissingPercent"  );
		*/
		AllowMissingConsecutive = props.getValue( "AllowMissingConsecutive" );
		OutputFillMethod = props.getValue( "OutputFillMethod" );
		HandleMissingInputHow = props.getValue( "HandleMissingInputHow");
		RecalcLimits = props.getValue ( "RecalcLimits" );

        if ( TSList == null ) {
            // Select default...
            __TSList_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TSList_JComboBox,TSList, JGUIUtil.NONE, null, null ) ) {
                __TSList_JComboBox.select ( TSList );
            }
            else {
                Message.printWarning ( 1, mthd,
                "Existing command references an invalid\nTSList value \"" + TSList +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if (    JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID,
                JGUIUtil.NONE, null, null ) ) {
                __TSID_JComboBox.select ( TSID );
        }
        else {  // Automatically add to the list after the blank...
            if ( (TSID != null) && (TSID.length() > 0) ) {
                __TSID_JComboBox.insertItemAt ( TSID, 1 );
                // Select...
                __TSID_JComboBox.select ( TSID );
            }
            else {  // Select the blank...
                __TSID_JComboBox.select ( 0 );
            }
        }
        if ( EnsembleID == null ) {
            // Select default...
            __EnsembleID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __EnsembleID_JComboBox,EnsembleID, JGUIUtil.NONE, null, null ) ) {
                __EnsembleID_JComboBox.select ( EnsembleID );
            }
            else {
                Message.printWarning ( 1, mthd,
                "Existing command references an invalid\nEnsembleID value \"" + EnsembleID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( NewEnsembleID != null ) {
            __NewEnsembleID_JTextField.setText ( NewEnsembleID );
        }
        if ( NewEnsembleName != null ) {
            __NewEnsembleName_JTextField.setText ( NewEnsembleName );
        }
		if ( Alias != null ) {
			__Alias_JTextField.setText ( Alias );
		}
		// Update NewInterval text field
		// Select the item in the list.  If not a match, print a warning.
		if ( NewInterval == null || NewInterval.equals("") ) {
			// Select a default...
			__NewInterval_JComboBox.select ( 0 );
		} 
		else {
			if ( JGUIUtil.isSimpleJComboBoxItem( __NewInterval_JComboBox, NewInterval, JGUIUtil.NONE, null, null ) ) {
				__NewInterval_JComboBox.select ( NewInterval );
			}
			else {
				mssg = "Existing command references an invalid\nNewInterval \"" + NewInterval + "\".  "
					+"Select a different choice or Cancel.";
				Message.printWarning ( 1, mthd, mssg );
			}
		}
		
		// Update OldTimeScale text field
		// Select the item in the list.  If not a match, print a warning.
		if ( OldTimeScale == null || OldTimeScale.equals("") ) {
			// Select default...
			__OldTimeScale_JComboBox.select ( 0 );
		}
		else {
			try {	
				JGUIUtil.selectTokenMatches ( __OldTimeScale_JComboBox, true, " ", 0, 0, OldTimeScale, null );
			}
			catch ( Exception e ) {
				mssg = "Existing command references an unrecognized\nOldTimeScale value \""
					+ OldTimeScale + "\".  Using the user value.";
				Message.printWarning ( 2, mthd, mssg );
				__OldTimeScale_JComboBox.setText (OldTimeScale);
			}
		}
		
		// Update NewTimeScale text field
		// Select the item in the list.  If not a match, print a warning.
		if ( NewTimeScale == null || NewTimeScale.equals("") ) {
			// Select default...
			__NewTimeScale_JComboBox.select ( 0 );
		}
		else {
			try {	
				JGUIUtil.selectTokenMatches ( __NewTimeScale_JComboBox, true, " ", 0, 0, NewTimeScale, null );
			}
			catch ( Exception e ) {
				mssg = "Existing command references an unrecognized\nNewTimeScale value \""
					+ NewTimeScale + "\".  Using the user value.";
				Message.printWarning ( 2, mthd, mssg );
				__NewTimeScale_JComboBox.setText (NewTimeScale);
			}
		}
        if ( Statistic == null ) {
            // Select default...
            __Statistic_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __Statistic_JComboBox,Statistic, JGUIUtil.NONE, null, null ) ) {
                __Statistic_JComboBox.select ( Statistic );
            }
            else {
                Message.printWarning ( 1, mthd,
                "Existing command references an invalid\nStatistic value \"" + Statistic +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( OutputYearType == null ) {
            // Select default...
            __OutputYearType_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __OutputYearType_JComboBox,OutputYearType, JGUIUtil.NONE, null, null ) ) {
                __OutputYearType_JComboBox.select ( OutputYearType );
            }
            else {
                Message.printWarning ( 1, mthd,
                "Existing command references an invalid\nOutputYearType value \"" + OutputYearType +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
		if ( NewDataType != null ) {
			__NewDataType_JTextField.setText ( NewDataType);
		}
        if ( NewUnits != null ) {
            __NewUnits_JTextField.setText ( NewUnits );
        }
        if ( Tolerance != null ) {
            __Tolerance_JTextField.setText ( Tolerance );
        }

        // Update HandleEndpointsHow text field
		// Select the item in the list. If not a match, print a warning.
		if ( HandleEndpointsHow == null ) {
			// Select default...
			__HandleEndpointsHow_JComboBox.select ( 0 );
		}
		else {
		    try {
		        JGUIUtil.selectTokenMatches ( __HandleEndpointsHow_JComboBox, true, " ", 0, 0, HandleEndpointsHow, null );
			}
			catch ( Exception e ) {
			    // Only an issue when going from INST to MEAN
			    if ( OldTimeScale.equalsIgnoreCase("" + TimeScaleType.INST) &&
			        NewTimeScale.equalsIgnoreCase("" + TimeScaleType.MEAN) ) {
    				mssg = "Existing command references an unrecognized HandleEndpointsHow value \""
    					+ HandleEndpointsHow + "\".  Using the user value.";
    				Message.printWarning ( 2, mthd, mssg );
			    }
				__HandleEndpointsHow_JComboBox.setText ( HandleEndpointsHow );
			}
		}

		if ( AllowMissingCount != null ) {
			__AllowMissingCount_JTextField.setText ( AllowMissingCount );
		}
		
		// Update AllowMissingPercent text field
		/* TODO SAM 2005-02-18 may enable later
		if ( AllowMissingPercent != null ) {
			__AllowMissingPercent_JTextField.setText (
				AllowMissingPercent );
		}
		*/
       if ( AllowMissingConsecutive != null ) {
            __AllowMissingConsecutive_JTextField.setText ( AllowMissingConsecutive );
        }
		// Update OutputFillMethod text field
		// Select the item in the list. If not a match, print a warning.
		if ( OutputFillMethod == null ) {
			// Select default...
			__OutputFillMethod_JComboBox.select ( 0 );
		}
		else {
			try {	
				JGUIUtil.selectTokenMatches ( __OutputFillMethod_JComboBox, true, " ", 0, 0, OutputFillMethod, null );
			}
			catch ( Exception e ) {
				mssg = "Existing command references an unrecognized\nOutputFillMethod value \""
					+ OutputFillMethod + "\".  Using the user value.";
				Message.printWarning ( 2, mthd, mssg );
				__OutputFillMethod_JComboBox.setText ( OutputFillMethod );
			}
		}
		
		// Update HandleMissingInputHow text field
		// Select the item in the list. If not a match, print a warning.
		if ( HandleMissingInputHow == null ) {
			// Select default...
			__HandleMissingInputHow_JComboBox.select ( 0 );
		}
		else {
		    try {
		        JGUIUtil.selectTokenMatches (
				__HandleMissingInputHow_JComboBox, true, " ", 0, 0, HandleMissingInputHow, null );
			}
			catch ( Exception e ) {
				mssg = "Existing command references an unrecognized\nHandleMissingInputHow value \""
					+ HandleMissingInputHow + "\".  Using the user value.";
				Message.printWarning ( 2, mthd, mssg );
				__HandleMissingInputHow_JComboBox.setText ( HandleMissingInputHow );
			}
		}
        if ( RecalcLimits == null ) {
            // Select default...
            __RecalcLimits_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem(
                __RecalcLimits_JComboBox, RecalcLimits, JGUIUtil.NONE, null, null )) {
                __RecalcLimits_JComboBox.select ( RecalcLimits );
            }
            else {
                Message.printWarning ( 1, mthd,
                "Existing command references an invalid\n" +
                "RecalcLimits value \"" + RecalcLimits +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
	}
	
	// Regardless, reset the command from the fields.  This is only  visible
	// information that has not been committed in the command.
    TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    NewEnsembleID = __NewEnsembleID_JTextField.getText().trim();
    NewEnsembleName = __NewEnsembleName_JTextField.getText().trim();
	Alias = __Alias_JTextField.getText().trim();
	NewInterval = __NewInterval_JComboBox.getSelected();
	OldTimeScale = StringUtil.getToken(	__OldTimeScale_JComboBox.getSelected(), " ", 0, 0 );
	NewTimeScale = StringUtil.getToken( __NewTimeScale_JComboBox.getSelected(), " ", 0, 0 );
    Statistic = __Statistic_JComboBox.getSelected();
	OutputYearType = __OutputYearType_JComboBox.getSelected();
	NewDataType = __NewDataType_JTextField.getText().trim();
	NewUnits = __NewUnits_JTextField.getText().trim();
	Tolerance = __Tolerance_JTextField.getText().trim();
    HandleEndpointsHow = __HandleEndpointsHow_JComboBox.getSelected();
	AllowMissingCount = __AllowMissingCount_JTextField.getText().trim();
	/* TODO LT 2005-05-24 may enable later		
	AllowMissingPercent = __AllowMissingPercent_JTextField.getText().trim(); */
	AllowMissingConsecutive = __AllowMissingConsecutive_JTextField.getText().trim();
	OutputFillMethod = __OutputFillMethod_JComboBox.getSelected();
	HandleMissingInputHow =	__HandleMissingInputHow_JComboBox.getSelected();
	RecalcLimits = __RecalcLimits_JComboBox.getSelected();
	
	// And set the command properties.
	props = new PropList ( __command.getCommandName() );
    props.add ( "TSList=" + TSList );
    props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
    props.add ( "NewEnsembleID=" + NewEnsembleID );
    props.add ( "NewEnsembleName=" + NewEnsembleName );
	props.add ( "Alias=" + Alias );
	props.add ( "NewInterval=" + NewInterval );
	props.add ( "OldTimeScale=" + OldTimeScale );
	props.add ( "OutputYearType=" + OutputYearType );
	props.add ( "NewTimeScale=" + NewTimeScale );
	if ( __Statistic_JComboBox.isEnabled() ) {
	    props.add ( "Statistic=" + Statistic );
	}
	props.add ( "NewDataType=" + NewDataType );
	props.add ( "NewUnits=" + NewUnits );
	props.add ( "Tolerance=" + Tolerance );
	if ( __HandleEndpointsHow_JComboBox.isEnabled() ) {
	    props.add ( "HandleEndpointsHow=" + HandleEndpointsHow );
	}
	props.add ( "AllowMissingCount=" + AllowMissingCount );
	/* TODO LT 2005-05-24 may enable later	
	props.add ( "AllowMissingPercent=" + AllowMissingPercent   ); */
	props.add ( "AllowMissingConsecutive=" + AllowMissingConsecutive );
	if ( __OutputFillMethod_JComboBox.isEnabled() ) {
	    props.add ( "OutputFillMethod=" + OutputFillMethod );
	}
	props.add ( "HandleMissingInputHow=" + HandleMissingInputHow );
	props.add ( "RecalcLimits=" + RecalcLimits);
	
	__Command_JTextArea.setText( __command.toString(props) );
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
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
public void windowClosed ( WindowEvent evt ){;}
public void windowDeactivated ( WindowEvent evt ){;}
public void windowDeiconified ( WindowEvent evt ){;}
public void windowIconified	( WindowEvent evt ){;}
public void windowOpened ( WindowEvent evt ){;}

}
