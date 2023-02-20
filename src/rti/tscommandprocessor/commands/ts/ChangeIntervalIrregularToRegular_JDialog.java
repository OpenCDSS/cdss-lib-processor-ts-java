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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.TS.TSUtil_ChangeIntervalIrregularToRegular;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.YearType;

@SuppressWarnings("serial")
public class ChangeIntervalIrregularToRegular_JDialog extends JDialog
	implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
// Controls are defined in logical order -- The order they appear in the dialog
// box and documentation.
private ChangeIntervalIrregularToRegular_Command __command = null;// Command object.

private JTabbedPane __main_JTabbedPane = null;
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
private SimpleJComboBox __Statistic_JComboBox = null;
private JTextField __Flag_JTextField = null;
private JTextField __FlagDescription_JTextField = null;
private JTextField __PersistInterval_JTextField = null;
private JTextField __PersistValue_JTextField = null;
private JTextField __PersistFlag_JTextField = null;
private JTextField __PersistFlagDescription_JTextField = null;
private JLabel  __OutputYearType_JLabel = null;
private SimpleJComboBox __OutputYearType_JComboBox = null;
private JTextField __NewDataType_JTextField = null;
private JTextField __NewUnits_JTextField = null;
private JTextField __ScaleValue_JTextField = null;
//private SimpleJComboBox __RecalcLimits_JComboBox = null;
private JTextArea __Command_JTextArea   = null;
private JScrollPane	__Command_JScrollPane = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
				
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
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", __command.getCommandName());
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
{   //String routine = "ChangeInterval_JDialog.checkGUIState";

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

    // Converting to yearly time series has some special handling
    String newInterval = __NewInterval_JComboBox.getSelected();
    if ( newInterval.equalsIgnoreCase("Year") ) {
        __OutputYearType_JLabel.setEnabled ( true );
        __OutputYearType_JComboBox.setEnabled ( true );
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
    String Statistic = __Statistic_JComboBox.getSelected();
	String Flag = __Flag_JTextField.getText().trim();
	String FlagDescription = __FlagDescription_JTextField.getText().trim();
	String PersistInterval = __PersistInterval_JTextField.getText().trim();
	String PersistValue = __PersistValue_JTextField.getText().trim();
	String PersistFlag = __PersistFlag_JTextField.getText().trim();
	String PersistFlagDescription = __PersistFlagDescription_JTextField.getText().trim();
    String OutputYearType = __OutputYearType_JComboBox.getSelected();
	String NewDataType = __NewDataType_JTextField.getText().trim();
	String NewUnits = __NewUnits_JTextField.getText().trim();
	String ScaleValue = __ScaleValue_JTextField.getText().trim();
	//String RecalcLimits = __RecalcLimits_JComboBox.getSelected();
	
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
    if ( Statistic.length() > 0 ) {
        props.set ( "Statistic", Statistic );
    }
    if ( Flag.length() > 0 ) {
        props.set ( "Flag", Flag );
    }
    if ( FlagDescription.length() > 0 ) {
        props.set ( "FlagDescription", FlagDescription );
    }
    if ( PersistInterval.length() > 0 ) {
        props.set ( "PersistInterval", PersistInterval );
    }
    if ( PersistValue.length() > 0 ) {
        props.set ( "PersistValue", PersistValue );
    }
    if ( PersistFlag.length() > 0 ) {
        props.set ( "PersistFlag", PersistFlag );
    }
    if ( PersistFlagDescription.length() > 0 ) {
        props.set ( "PersistFlagDescription", PersistFlagDescription );
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
	if ( ScaleValue != null && ScaleValue.length() > 0 ) {
	     props.set( "ScaleValue", ScaleValue );
	}
	/*
    if ( RecalcLimits.length() > 0 ) {
        props.set( "RecalcLimits", RecalcLimits );
    }
    */
	
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
    String Statistic = __Statistic_JComboBox.getSelected();
	String Flag = __Flag_JTextField.getText().trim();
	String FlagDescription = __FlagDescription_JTextField.getText().trim();
	String PersistInterval = __PersistInterval_JTextField.getText().trim();
	String PersistValue = __PersistValue_JTextField.getText().trim();
	String PersistFlag = __PersistFlag_JTextField.getText().trim();
	String PersistFlagDescription = __PersistFlagDescription_JTextField.getText().trim();
	String OutputYearType = __OutputYearType_JComboBox.getSelected();
	String NewDataType = __NewDataType_JTextField.getText().trim();
	String NewUnits = __NewUnits_JTextField.getText().trim();
	String ScaleValue = __ScaleValue_JTextField.getText().trim();
	//String RecalcLimits = __RecalcLimits_JComboBox.getSelected();

	// Commit the values to the command object.
    __command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "NewEnsembleID", NewEnsembleID );
    __command.setCommandParameter ( "NewEnsembleName", NewEnsembleName );
	__command.setCommandParameter ( "Alias", Alias );
	__command.setCommandParameter ( "NewInterval", NewInterval );
    __command.setCommandParameter ( "Statistic", Statistic );
    __command.setCommandParameter ( "Flag", Flag );
    __command.setCommandParameter ( "FlagDescription", FlagDescription );
    __command.setCommandParameter ( "PersistInterval", PersistInterval );
    __command.setCommandParameter ( "PersistValue", PersistValue );
    __command.setCommandParameter ( "PersistFlag", PersistFlag );
    __command.setCommandParameter ( "PersistFlagDescription", PersistFlagDescription );
	__command.setCommandParameter ( "OutputYearType", OutputYearType );
	__command.setCommandParameter ( "NewDataType", NewDataType );
	__command.setCommandParameter ( "NewUnits", NewUnits );
	__command.setCommandParameter ( "ScaleValue", ScaleValue );
	//__command.setCommandParameter ( "RecalcLimits", RecalcLimits );
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
	String title = "Edit " + __command.getCommandName() + " Command";
	
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
		"Create a new regular interval time series (or ensemble) from irrregular interval time series (or ensemble)."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The output time series will by default have an identifier that is the same as the input, but with the new interval."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"If the input is an ensemble, then a new ensemble will be created."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
     
    // Panel for input

    int yInput = -1;
    JPanel input_JPanel = new JPanel();
    input_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Input", input_JPanel );

    __TSList_JComboBox = new SimpleJComboBox(false);
    yInput = CommandEditorUtil.addTSListToEditorDialogPanel ( this, input_JPanel, __TSList_JComboBox, yInput );

    __TSID_JLabel = new JLabel ("TSID (for TSList=*TSID:");
    __TSID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __TSID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yInput = CommandEditorUtil.addTSIDToEditorDialogPanel (
        this, this, input_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, yInput );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __EnsembleID_JComboBox.setToolTipText("Select an ensemble identifier from the list or specify with ${Property} notation");
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yInput = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
        this, this, input_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, yInput );
    
    // Panel for analysis (to Larger)

    int yLarger = -1;
    JPanel larger_JPanel = new JPanel();
    larger_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Analysis (to Larger)", larger_JPanel );

    JGUIUtil.addComponent(larger_JPanel, new JLabel (
		"<html>Irregular interval values are converted <b>to a larger interval</b> by determining a sample of 0+ input values in the output interval.</html>"),
		0, ++yLarger, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(larger_JPanel, new JLabel (
		"The sample is then used to compute the output value as a statistic."),
		0, ++yLarger, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(larger_JPanel, new JLabel (
		"For example, input values may have a date/time precision of minute or smaller (real-time data) and output interval is hour or larger."),
		0, ++yLarger, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(larger_JPanel, new JLabel (
		"Output intervals with no sample will be set to missing OR the most recent input value if within the persist interval."),
		0, ++yLarger, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(larger_JPanel, new JLabel (
		"The output value can also be scaled, for example to convert to new units (see Output parameters)."),
		0, ++yLarger, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	JGUIUtil.addComponent(larger_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++yLarger, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(larger_JPanel, new JLabel("Statistic:"), 
        0, ++yLarger, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Statistic_JComboBox = new SimpleJComboBox ( false );    // Do not allow edit
    List<String> statisticChoices = TSUtil_ChangeIntervalIrregularToRegular.getStatisticChoicesAsStrings();
    __Statistic_JComboBox.setData ( statisticChoices );
    __Statistic_JComboBox.addItemListener ( this );
    __Statistic_JComboBox.setMaximumRowCount(statisticChoices.size());
    JGUIUtil.addComponent(larger_JPanel, __Statistic_JComboBox,
        1, yLarger, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(larger_JPanel, new JLabel("Required - statistic for sample for interval."), 
        3, yLarger, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Flag 
    JGUIUtil.addComponent(larger_JPanel,new JLabel ("Flag:" ),
        0, ++yLarger, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Flag_JTextField = new JTextField ( "", 10 );
    __Flag_JTextField.setToolTipText("Flag for calculated values.");
    JGUIUtil.addComponent(larger_JPanel, __Flag_JTextField,
        1, yLarger, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __Flag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(larger_JPanel, new JLabel ( "Optional - flag for calculated values (default=no flag)."),
        3, yLarger, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Flag Description
    JGUIUtil.addComponent(larger_JPanel,new JLabel ("Flag description:" ),
        0, ++yLarger, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __FlagDescription_JTextField = new JTextField ( "", 10 );
    __FlagDescription_JTextField.setToolTipText("Flag desecription.");
    JGUIUtil.addComponent(larger_JPanel, __FlagDescription_JTextField,
        1, yLarger, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __FlagDescription_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(larger_JPanel, new JLabel ( "Optional - flag description (default=no description)."),
        3, yLarger, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // PersistInterval 
    JGUIUtil.addComponent(larger_JPanel,new JLabel ("Persist interval:" ),
        0, ++yLarger, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PersistInterval_JTextField = new JTextField ( "", 10 );
    __PersistInterval_JTextField.setToolTipText("Use interval similar to the new interval, such as 15Minute, 1Hour, Day, Month, Year.");
    JGUIUtil.addComponent(larger_JPanel, __PersistInterval_JTextField,
        1, yLarger, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __PersistInterval_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(larger_JPanel, new JLabel ( "Optional - interval that last value persists (default=no persistence)."),
        3, yLarger, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // PersistValue 
    JGUIUtil.addComponent(larger_JPanel,new JLabel ("Persist value:" ),
        0, ++yLarger, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PersistValue_JTextField = new JTextField ( "", 10 );
    __PersistValue_JTextField.setToolTipText("Value to persist in persist interval, if different than last value (e.g., 0 for precipitation).");
    JGUIUtil.addComponent(larger_JPanel, __PersistValue_JTextField,
        1, yLarger, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __PersistValue_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(larger_JPanel, new JLabel ( "Optional - value to use in persist interval (default=last input value)."),
        3, yLarger, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // PersistFlag 
    JGUIUtil.addComponent(larger_JPanel,new JLabel ("Persist flag:" ),
        0, ++yLarger, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PersistFlag_JTextField = new JTextField ( "", 10 );
    __PersistFlag_JTextField.setToolTipText("Flag for persisted values.");
    JGUIUtil.addComponent(larger_JPanel, __PersistFlag_JTextField,
        1, yLarger, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __PersistFlag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(larger_JPanel, new JLabel ( "Optional - flag for persisted values (default=no flag)."),
        3, yLarger, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // PersistFlagDescription
    JGUIUtil.addComponent(larger_JPanel,new JLabel ("Persist flag description:" ),
        0, ++yLarger, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PersistFlagDescription_JTextField = new JTextField ( "", 10 );
    __PersistFlagDescription_JTextField.setToolTipText("Persist flag desecription.");
    JGUIUtil.addComponent(larger_JPanel, __PersistFlagDescription_JTextField,
        1, yLarger, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __PersistFlagDescription_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(larger_JPanel, new JLabel ( "Optional - persist flag description (default=no description)."),
        3, yLarger, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for analysis (to Smaller)

    int ySmaller = -1;
    JPanel smaller_JPanel = new JPanel();
    smaller_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Analysis (to Smaller)", smaller_JPanel );

    JGUIUtil.addComponent(smaller_JPanel, new JLabel (
		"<html>Irregular interval values are converted <b>to a smaller interval</b> by back-calculating from the input values in the output interval.</html>"),
		0, ++ySmaller, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(smaller_JPanel, new JLabel (
		"For example, input values may have a date/time precision of hour or larger (infrequent measurement or total) and output interval is minute."),
		0, ++ySmaller, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(smaller_JPanel, new JLabel (
		"<html><b>This functionality is being evaluated but is currently not supported.</b></html>"),
		0, ++ySmaller, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(smaller_JPanel, new JLabel (
		"<html><b>An alternative is to convert the irregular interval data to a larger interval (e.g., day) and then convert day to smaller interval.</b></html>"),
		0, ++ySmaller, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	//JGUIUtil.addComponent(small_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		//0, ++ySmaller, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Maybe use SampleMethod for large to small?

    // Panel for time series general output
    int yOutput = -1;
    JPanel output_JPanel = new JPanel();
    output_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output", output_JPanel );

	// Time series alias
    JGUIUtil.addComponent(output_JPanel, new JLabel("Alias to assign:"),
        0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(15);
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener( this );
    JGUIUtil.addComponent(output_JPanel, __Alias_JTextField,
        1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel ("Required - use %L for location, etc."),
        3, yOutput, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

	// New interval
    JGUIUtil.addComponent(output_JPanel, new JLabel( "New interval:"),
		0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewInterval_JComboBox = new SimpleJComboBox ( false );
	__NewInterval_JComboBox.setData (
		TimeInterval.getTimeIntervalChoices(TimeInterval.MINUTE, TimeInterval.YEAR,false,-1));
	// Select a default...
	__NewInterval_JComboBox.select ( 0 );
	__NewInterval_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(output_JPanel, __NewInterval_JComboBox,
		1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel (	"Required - data interval for result."),
		3, yOutput, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __OutputYearType_JLabel = new JLabel ( "Output year type:" );
    JGUIUtil.addComponent(output_JPanel, __OutputYearType_JLabel, 
        0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputYearType_JComboBox = new SimpleJComboBox ( false );
    __OutputYearType_JComboBox.add ( "" );
    __OutputYearType_JComboBox.add ( "" + YearType.CALENDAR );
    __OutputYearType_JComboBox.add ( "" + YearType.NOV_TO_OCT );
    __OutputYearType_JComboBox.add ( "" + YearType.WATER );
    __OutputYearType_JComboBox.add ( "" + YearType.YEAR_MAY_TO_APR );
    __OutputYearType_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(output_JPanel, __OutputYearType_JComboBox,
        1, yOutput, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel (
        "Optional - use only when new interval is Year (default=" + YearType.CALENDAR + ")."),
        3, yOutput, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	// New data type
    JGUIUtil.addComponent(output_JPanel,new JLabel ("New data type:" ),
		0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewDataType_JTextField = new JTextField ( "", 10 );
	JGUIUtil.addComponent(output_JPanel, __NewDataType_JTextField,
		1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	__NewDataType_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(output_JPanel, new JLabel ( "Optional - data type (default=original time series data type)."),
		3, yOutput, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // New units
    JGUIUtil.addComponent(output_JPanel,new JLabel ("New units:" ),
        0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NewUnits_JTextField = new JTextField ( "", 10 );
    JGUIUtil.addComponent(output_JPanel, __NewUnits_JTextField,
        1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __NewUnits_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(output_JPanel, new JLabel ( "Optional - data units (default=original time series units)."),
        3, yOutput, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Scale value 
    JGUIUtil.addComponent(output_JPanel,new JLabel ("Scale value:" ),
        0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ScaleValue_JTextField = new JTextField ( "", 10 );
    JGUIUtil.addComponent(output_JPanel, __ScaleValue_JTextField,
        1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __ScaleValue_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(output_JPanel, new JLabel ( "Optional - value to scale output (default=1.0)."),
        3, yOutput, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    /*
    JGUIUtil.addComponent(output_JPanel, new JLabel ( "Recalculate limits:"), 
        0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __RecalcLimits_JComboBox = new SimpleJComboBox ( false );
    List<String> recalcChoices = new ArrayList<String>();
    recalcChoices.add ( "" );
    recalcChoices.add ( __command._False );
    recalcChoices.add ( __command._True );
    __RecalcLimits_JComboBox.setData(recalcChoices);
    __RecalcLimits_JComboBox.select ( 0 );
    __RecalcLimits_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(output_JPanel, __RecalcLimits_JComboBox,
    1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel,
        new JLabel( "Optional - recalculate original data limits after set (default=" + __command._False + ")."), 
        3, yOutput, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        */

    // Panel for ensemble output
    int yEnsOutput = -1;
    JPanel ensOutput_JPanel = new JPanel();
    ensOutput_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output (Ensemble)", ensOutput_JPanel );
    
    __NewEnsembleID_JLabel = new JLabel ( "New ensemble ID:" );
    JGUIUtil.addComponent(ensOutput_JPanel, __NewEnsembleID_JLabel, 
        0, ++yEnsOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NewEnsembleID_JTextField = new JTextField ( "", 20 );
    __NewEnsembleID_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ensOutput_JPanel, __NewEnsembleID_JTextField,
        1, yEnsOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensOutput_JPanel, new JLabel( "Optional - to create ensemble when input is an ensemble."), 
        3, yEnsOutput, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __NewEnsembleName_JLabel = new JLabel ( "New ensemble name:" );
    JGUIUtil.addComponent(ensOutput_JPanel, __NewEnsembleName_JLabel,
        0, ++yEnsOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NewEnsembleName_JTextField = new JTextField ( "", 30 );
    __NewEnsembleName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ensOutput_JPanel, __NewEnsembleName_JTextField,
        1, yEnsOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensOutput_JPanel, new JLabel( "Optional - name for new ensemble."), 
        3, yEnsOutput, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
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

	// OK button:
	__ok_JButton = new SimpleJButton("OK", this);
	__ok_JButton .setToolTipText( "Close the window, without saving the command edits.");
	button_JPanel.add ( __ok_JButton );

	// Cancel button:
	__cancel_JButton = new SimpleJButton( "Cancel", this);
	__cancel_JButton.setToolTipText( "Close the window, without saving the command edits.");
	button_JPanel.add ( __cancel_JButton );

	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");
	
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
	String routine = getClass().getSimpleName() + ".refresh", message;
    String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String NewEnsembleID = "";
    String NewEnsembleName = "";
	String Alias = "";
	String NewInterval = "";
    String Statistic = "";
    String Flag = "";
    String FlagDescription = "";
    String PersistInterval = "";
    String PersistValue = "";
    String PersistFlag = "";
    String PersistFlagDescription = "";
	String OutputYearType = "";
	String NewDataType = "";
	String NewUnits = "";
	String ScaleValue = "";
	//String RecalcLimits = "";
	
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
	    Statistic = props.getValue ( "Statistic" );
	    Flag = props.getValue ( "Flag" );
	    FlagDescription = props.getValue ( "FlagDescription" );
		PersistInterval = props.getValue( "PersistInterval" );
		PersistValue = props.getValue( "PersistValue" );
	    PersistFlag = props.getValue ( "PersistFlag" );
	    PersistFlagDescription = props.getValue ( "PersistFlagDescription" );
		OutputYearType = props.getValue ( "OutputYearType" );
		NewDataType = props.getValue( "NewDataType" );
		NewUnits = props.getValue( "NewUnits" );
		ScaleValue = props.getValue( "ScaleValue" );
		//RecalcLimits = props.getValue ( "RecalcLimits" );

        if ( TSList == null ) {
            // Select default...
            __TSList_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TSList_JComboBox,TSList, JGUIUtil.NONE, null, null ) ) {
                __TSList_JComboBox.select ( TSList );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTSList value \"" + TSList +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID, JGUIUtil.NONE, null, null ) ) {
            __TSID_JComboBox.select ( TSID );
        }
        else {
        	// Automatically add to the list after the blank...
            if ( (TSID != null) && (TSID.length() > 0) ) {
                __TSID_JComboBox.insertItemAt ( TSID, 1 );
                // Select...
                __TSID_JComboBox.select ( TSID );
            }
            else {
            	// Select the blank...
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
                Message.printWarning ( 1, routine,
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
				message = "Existing command references an invalid\nNewInterval \"" + NewInterval + "\".  "
					+ "Select a different choice or Cancel.";
				Message.printWarning ( 1, routine, message );
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
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nStatistic value \"" + Statistic +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( Flag != null ) {
            __Flag_JTextField.setText ( Flag );
        }
        if ( FlagDescription != null ) {
            __FlagDescription_JTextField.setText ( FlagDescription );
        }
        if ( PersistInterval != null ) {
            __PersistInterval_JTextField.setText ( PersistInterval );
        }
        if ( PersistValue != null ) {
            __PersistValue_JTextField.setText ( PersistValue );
        }
        if ( PersistFlag != null ) {
            __PersistFlag_JTextField.setText ( PersistFlag );
        }
        if ( PersistFlagDescription != null ) {
            __PersistFlagDescription_JTextField.setText ( PersistFlagDescription );
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
                Message.printWarning ( 1, routine,
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
        if ( ScaleValue != null ) {
            __ScaleValue_JTextField.setText ( ScaleValue );
        }
        /*
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
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\n" +
                "RecalcLimits value \"" + RecalcLimits +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        */
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
    Statistic = __Statistic_JComboBox.getSelected();
	Flag = __Flag_JTextField.getText().trim();
	FlagDescription = __FlagDescription_JTextField.getText().trim();
	PersistInterval = __PersistInterval_JTextField.getText().trim();
	PersistValue = __PersistValue_JTextField.getText().trim();
	PersistFlag = __PersistFlag_JTextField.getText().trim();
	PersistFlagDescription = __PersistFlagDescription_JTextField.getText().trim();
	OutputYearType = __OutputYearType_JComboBox.getSelected();
	NewDataType = __NewDataType_JTextField.getText().trim();
	NewUnits = __NewUnits_JTextField.getText().trim();
	ScaleValue = __ScaleValue_JTextField.getText().trim();
	//RecalcLimits = __RecalcLimits_JComboBox.getSelected();
	
	// And set the command properties.
	props = new PropList ( __command.getCommandName() );
    props.add ( "TSList=" + TSList );
    props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
    props.add ( "NewEnsembleID=" + NewEnsembleID );
    props.add ( "NewEnsembleName=" + NewEnsembleName );
	props.add ( "Alias=" + Alias );
	props.add ( "NewInterval=" + NewInterval );
	props.add ( "OutputYearType=" + OutputYearType );
	props.add ( "Statistic=" + Statistic );
	props.add ( "Flag=" + Flag );
	props.add ( "FlagDescription=" + FlagDescription );
	props.add ( "PersistInterval=" + PersistInterval );
	props.add ( "PersistValue=" + PersistValue );
	props.add ( "PersistFlag=" + PersistFlag );
	props.add ( "PersistFlagDescription=" + PersistFlagDescription );
	props.add ( "NewDataType=" + NewDataType );
	props.add ( "NewUnits=" + NewUnits );
	props.add ( "ScaleValue=" + ScaleValue );
	//props.add ( "RecalcLimits=" + RecalcLimits);
	
	__Command_JTextArea.setText( __command.toString(props).trim() );
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