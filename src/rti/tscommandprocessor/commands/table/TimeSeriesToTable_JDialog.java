// TimeSeriesToTable_JDialog - Editor for TimeSeriesToTable command.

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

package rti.tscommandprocessor.commands.table;

import java.awt.Color;
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

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
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
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

import java.util.ArrayList;
import java.util.List;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.DateTimeWindow;
import RTi.Util.Time.DateTime_JPanel;
import RTi.Util.Time.TimeInterval;

/**
Editor for TimeSeriesToTable command.
*/
@SuppressWarnings("serial")
public class TimeSeriesToTable_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private TimeSeriesToTable_Command __command = null;
private JTextArea __command_JTextArea=null;
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __DateTimeColumn_JTextField = null;
private JTextField __TableTSIDColumn_JTextField = null;
private TSFormatSpecifiersJPanel __TableTSIDFormat_JTextField = null; // Format for TSID column output
private SimpleJComboBox __IncludeMissingValues_JComboBox = null;
private TSFormatSpecifiersJPanel __ValueColumn_JTextField = null;
private JTextField __OutputPrecision_JTextField = null;
private TSFormatSpecifiersJPanel __FlagColumn_JTextField = null;
private JTextField __OutputStart_JTextField = null;
private JTextField __OutputEnd_JTextField = null;
private JCheckBox __OutputWindow_JCheckBox = null;
private DateTime_JPanel __OutputWindowStart_JPanel = null; // Fields for output window within a year
private DateTime_JPanel __OutputWindowEnd_JPanel = null;
private JTextField __OutputWindowStart_JTextField = null; // Used for properties
private JTextField __OutputWindowEnd_JTextField = null;
private SimpleJComboBox __IfTableNotFound_JComboBox = null;
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public TimeSeriesToTable_JDialog ( JFrame parent, TimeSeriesToTable_Command command )
{   super(parent, true);
    initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();

	if ( o == __cancel_JButton ) {
		__command = null;
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "TimeSeriesToTable");
	}
	else if ( o == __ok_JButton ) {
		refresh();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
    else {
        checkGUIState();
        refresh ();
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
{
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
        __EnsembleID_JComboBox.setEnabled(true);
        __EnsembleID_JLabel.setEnabled ( true );
    }
    else {
        __EnsembleID_JComboBox.setEnabled(false);
        __EnsembleID_JLabel.setEnabled ( false );
    }
    
    if ( __OutputWindow_JCheckBox.isSelected() ) {
        // Checked so enable the date panels
        __OutputWindowStart_JPanel.setEnabled ( true );
        __OutputWindowEnd_JPanel.setEnabled ( true );
        __OutputWindowStart_JTextField.setEnabled ( true );
        __OutputWindowEnd_JTextField.setEnabled ( true );
    }
    else {
        __OutputWindowStart_JPanel.setEnabled ( false );
        __OutputWindowEnd_JPanel.setEnabled ( false );
        __OutputWindowStart_JTextField.setEnabled ( false );
        __OutputWindowEnd_JTextField.setEnabled ( false );
    }
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{   // Put together a list of parameters to check...
    PropList props = new PropList ( "" );
    String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String TableID = __TableID_JComboBox.getSelected();
    String DateTimeColumn = __DateTimeColumn_JTextField.getText().trim();
    String TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
    String TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
    String IncludeMissingValues = __IncludeMissingValues_JComboBox.getSelected();
    String ValueColumn = __ValueColumn_JTextField.getText().trim();
    String OutputPrecision = __OutputPrecision_JTextField.getText().trim();
    String FlagColumn = __FlagColumn_JTextField.getText().trim();
    String OutputStart = __OutputStart_JTextField.getText().trim();
    String OutputEnd = __OutputEnd_JTextField.getText().trim();
    String IfTableNotFound = __IfTableNotFound_JComboBox.getSelected();
    __error_wait = false;

    if ( TSList.length() > 0 ) {
        props.set ( "TSList", TSList );
    }
    if ( TSID.length() > 0 ) {
        props.set ( "TSID", TSID );
    }
    if ( EnsembleID.length() > 0 ) {
        props.set ( "EnsembleID", EnsembleID );
    }
    if ( TableID.length() > 0 ) {
        props.set ( "TableID", TableID );
    }
    if ( DateTimeColumn.length() > 0 ) {
        props.set ( "DateTimeColumn", DateTimeColumn );
    }
    if ( TableTSIDColumn.length() > 0 ) {
        props.set ( "TableTSIDColumn", TableTSIDColumn );
    }
    if ( TableTSIDFormat.length() > 0 ) {
        props.set ( "TableTSIDFormat", TableTSIDFormat );
    }
    if ( IncludeMissingValues.length() > 0 ) {
        props.set ( "IncludeMissingValues", IncludeMissingValues );
    }
    if ( ValueColumn.length() > 0 ) {
        props.set ( "ValueColumn", ValueColumn );
    }
    if ( OutputPrecision.length() > 0 ) {
        props.set ( "OutputPrecision", OutputPrecision );
    }
    if ( FlagColumn.length() > 0 ) {
        props.set ( "FlagColumn", FlagColumn );
    }
    if ( OutputStart.length() > 0 ) {
        props.set ( "OutputStart", OutputStart );
    }
    if ( OutputEnd.length() > 0 ) {
        props.set ( "OutputEnd", OutputEnd );
    }
    if ( IfTableNotFound.length() > 0 ) {
        props.set ( "IfTableNotFound", IfTableNotFound );
    }
    if ( __OutputWindow_JCheckBox.isSelected() ){
        String OutputWindowStart = __OutputWindowStart_JPanel.toString(false,true).trim();
        String OutputWindowEnd = __OutputWindowEnd_JPanel.toString(false,true).trim();
        // 99 is used for month if not specified - don't want that in the parameters
        if ( OutputWindowStart.startsWith("99") ) {
            OutputWindowStart = "";
        }
        if ( OutputWindowEnd.startsWith("99") ) {
            OutputWindowEnd = "";
        }
        // This will override the above
        String OutputWindowStart2 = __OutputWindowStart_JTextField.getText().trim();
        String OutputWindowEnd2 = __OutputWindowEnd_JTextField.getText().trim();
        if ( !OutputWindowStart2.isEmpty() ) {
            OutputWindowStart = OutputWindowStart2;
        }
        if ( !OutputWindowEnd2.isEmpty() ) {
            OutputWindowEnd = OutputWindowEnd2;
        }
        if ( !OutputWindowStart.isEmpty() ) {
            props.set ( "OutputWindowStart", OutputWindowStart );
        }
        if ( OutputWindowEnd.isEmpty() ) {
            props.set ( "OutputWindowEnd", OutputWindowEnd );
        }
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
{   String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String TableID = __TableID_JComboBox.getSelected();
    String DateTimeColumn = __DateTimeColumn_JTextField.getText().trim();
    String TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
    String TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
    String IncludeMissingValues = __IncludeMissingValues_JComboBox.getSelected();
    String ValueColumn = __ValueColumn_JTextField.getText().trim();
    String OutputPrecision = __OutputPrecision_JTextField.getText().trim();
    String FlagColumn = __FlagColumn_JTextField.getText().trim();
    String OutputStart = __OutputStart_JTextField.getText().trim();
    String OutputEnd = __OutputEnd_JTextField.getText().trim();
    String IfTableNotFound = __IfTableNotFound_JComboBox.getSelected();
    __command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "DateTimeColumn", DateTimeColumn );
    __command.setCommandParameter ( "TableTSIDColumn", TableTSIDColumn );
    __command.setCommandParameter ( "TableTSIDFormat", TableTSIDFormat );
    __command.setCommandParameter ( "IncludeMissingValues", IncludeMissingValues );
    __command.setCommandParameter ( "ValueColumn", ValueColumn );
    __command.setCommandParameter ( "OutputPrecision", OutputPrecision );
    __command.setCommandParameter ( "FlagColumn", FlagColumn );
    __command.setCommandParameter ( "OutputStart", OutputStart );
    __command.setCommandParameter ( "OutputEnd", OutputEnd );
    __command.setCommandParameter ( "IfTableNotFound", IfTableNotFound );
    if ( __OutputWindow_JCheckBox.isSelected() ){
        String OutputWindowStart = __OutputWindowStart_JPanel.toString(false,true).trim();
        String OutputWindowEnd = __OutputWindowEnd_JPanel.toString(false,true).trim();
        if ( OutputWindowStart.startsWith("99") ) {
            OutputWindowStart = "";
        }
        if ( OutputWindowEnd.startsWith("99") ) {
            OutputWindowEnd = "";
        }
        String OutputWindowStart2 = __OutputWindowStart_JTextField.getText().trim();
        String OutputWindowEnd2 = __OutputWindowEnd_JTextField.getText().trim();
        if ( !OutputWindowStart2.isEmpty() ) {
        	OutputWindowStart = OutputWindowStart2;
        }
        if ( !OutputWindowEnd2.isEmpty() ) {
        	OutputWindowEnd = OutputWindowEnd2;
        }
        __command.setCommandParameter ( "OutputWindowStart", OutputWindowStart );
        __command.setCommandParameter ( "OutputWindowEnd", OutputWindowEnd );
    }
    else {
    	// Clear the properties because they may have been set during editing but should not be propagated
    	__command.getCommandParameters().unSet ( "OutputWindowStart" );
    	__command.getCommandParameters().unSet ( "OutputWindowEnd" );
    }
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, TimeSeriesToTable_Command command )
{	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Copy time series date/time and value pairs to column(s) in a new table.  " +
		"If the table TSID column is specified, output will be to a single column." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The time series must have the same data interval if a multi-column table is created." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "If the output window is specified, use a date/time precision consistent with data." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    __TSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel ( this, main_JPanel, __TSList_JComboBox, y );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
    __TSID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, y );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __EnsembleID_JComboBox.setToolTipText("Select an ensemble identifier from the list or specify with ${Property} notation");
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
        this, this, main_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Table ID:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 10, true ); // Allow edits
    __TableID_JComboBox.setToolTipText("Specify the table ID for output or use ${Property} notation");
    List<String> TableIDs = TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    // If the TableID has been specified and is not in the list, then a new table is being added -
    // add the table ID at the start of the list
    String TableID = __command.getCommandParameters().getValue("TableID");
    if ( (TableID != null) && !TableID.equals("") ) {
        if ( StringUtil.indexOfIgnoreCase(TableIDs, TableID) < 0 ) {
            TableIDs.add(0,TableID);
        }
    }
    __TableID_JComboBox.setData(TableIDs);
    __TableID_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __TableID_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - table identifier."),
    3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel,new JLabel( "Date/time column in table:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DateTimeColumn_JTextField = new JTextField ( "", 20 );
    __DateTimeColumn_JTextField.setToolTipText("Specify the table column containing date/time or use ${Property} notation");
    __DateTimeColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DateTimeColumn_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Required - column name for date/times."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for single-column output
    int ySingleColumn = -1;
    JPanel singleColumn_JPanel = new JPanel();
    singleColumn_JPanel.setLayout( new GridBagLayout() );
    singleColumn_JPanel.setBorder( BorderFactory.createTitledBorder (
        BorderFactory.createLineBorder(Color.black),
        "Single-column output parameters" ));
    JGUIUtil.addComponent( main_JPanel, singleColumn_JPanel,
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(singleColumn_JPanel, new JLabel ( "Table TSID column:" ), 
        0, ++ySingleColumn, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableTSIDColumn_JTextField = new JTextField ( 20 );
    __TableTSIDColumn_JTextField.setToolTipText("Specify the table column containing TSID or use ${Property} notation");
    __TableTSIDColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(singleColumn_JPanel, __TableTSIDColumn_JTextField,
        1, ySingleColumn, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleColumn_JPanel, new JLabel( "Optional - column name for TSID (if values in single column)."), 
        3, ySingleColumn, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(singleColumn_JPanel, new JLabel("Format of TSID:"),
        0, ++ySingleColumn, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableTSIDFormat_JTextField = new TSFormatSpecifiersJPanel(20);
    __TableTSIDFormat_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
    __TableTSIDFormat_JTextField.addKeyListener ( this );
    __TableTSIDFormat_JTextField.getDocument().addDocumentListener(this);
    __TableTSIDFormat_JTextField.setToolTipText("%L for location, %T for data type.");
    JGUIUtil.addComponent(singleColumn_JPanel, __TableTSIDFormat_JTextField,
        1, ySingleColumn, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleColumn_JPanel, new JLabel ("Optional - can use if TableTSIDColumn is specified (default=alias or TSID)."),
        3, ySingleColumn, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(singleColumn_JPanel, new JLabel ( "Include missing values?:"), 
        0, ++ySingleColumn, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IncludeMissingValues_JComboBox = new SimpleJComboBox ( false );
    List<String> missingChoices = new ArrayList<String>();
    missingChoices.add ( "" );
    missingChoices.add ( __command._False );
    missingChoices.add ( __command._True );
    __IncludeMissingValues_JComboBox.setData(missingChoices);
    __IncludeMissingValues_JComboBox.select(0);
    __IncludeMissingValues_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(singleColumn_JPanel, __IncludeMissingValues_JComboBox,
        1, ySingleColumn, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleColumn_JPanel, new JLabel ( "Optional - include missing values (default=" + __command._True + ")."),
        3, ySingleColumn, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel,new JLabel( "Data value column(s) in table:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ValueColumn_JTextField = new TSFormatSpecifiersJPanel(10);
    __ValueColumn_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval, ${ts:Property}, ${Property}.");
    __ValueColumn_JTextField.addKeyListener ( this );
    __ValueColumn_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(main_JPanel, __ValueColumn_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required - value column name(s) for 1+ time series."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel,new JLabel( "Output precision:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputPrecision_JTextField = new JTextField ( "", 10 );
    __OutputPrecision_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __OutputPrecision_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - precision for value column(s) in table (default=2)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel,new JLabel( "Flag column(s) in table:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __FlagColumn_JTextField = new TSFormatSpecifiersJPanel(10);
    __FlagColumn_JTextField.setToolTipText("If specified, the number of columns much match the data columns, " +
        "but column names can be blank to ignore, can use ${Property}.");
    __FlagColumn_JTextField.addKeyListener ( this );
    __FlagColumn_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(main_JPanel, __FlagColumn_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - flag column name(s) for 1+ time series."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel,new JLabel( "Output start date/time:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputStart_JTextField = new JTextField ( "", 20 );
    __OutputStart_JTextField.setToolTipText("Specify the output start using a date/time string or ${Property} notation");
    __OutputStart_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __OutputStart_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional (default=copy all)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel,new JLabel("Output end date/time:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputEnd_JTextField = new JTextField ( "", 20 );
    __OutputEnd_JTextField.setToolTipText("Specify the output end using a date/time string or ${Property} notation");
    __OutputEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __OutputEnd_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional (default=copy all)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    // Always request month to minute because don't know for sure what time series interval is specified
    __OutputWindow_JCheckBox = new JCheckBox ( "Output window:", false );
    __OutputWindow_JCheckBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __OutputWindow_JCheckBox, 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    JPanel OutputWindow_JPanel = new JPanel();
    OutputWindow_JPanel.setLayout(new GridBagLayout());
    __OutputWindowStart_JPanel = new DateTime_JPanel ( "Start", TimeInterval.MONTH, TimeInterval.MINUTE, null );
    __OutputWindowStart_JPanel.addActionListener(this);
    __OutputWindowStart_JPanel.addKeyListener ( this );
    JGUIUtil.addComponent(OutputWindow_JPanel, __OutputWindowStart_JPanel,
        1, 0, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    // TODO SAM 2008-01-23 Figure out how to display the correct limits given the time series interval
    __OutputWindowEnd_JPanel = new DateTime_JPanel ( "End", TimeInterval.MONTH, TimeInterval.MINUTE, null );
    __OutputWindowEnd_JPanel.addActionListener(this);
    __OutputWindowEnd_JPanel.addKeyListener ( this );
    JGUIUtil.addComponent(OutputWindow_JPanel, __OutputWindowEnd_JPanel,
        4, 0, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, OutputWindow_JPanel,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - output window within each year (default=full year)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel,new JLabel( "Output window start as ${Property}:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputWindowStart_JTextField = new JTextField ( "", 20 );
    __OutputWindowStart_JTextField.setToolTipText("Specify the output window start ${Property} - will override the above.");
    __OutputWindowStart_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __OutputWindowStart_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional (default=full year)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel,new JLabel("Output window end as ${Property}:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputWindowEnd_JTextField = new JTextField ( "", 20 );
    __OutputWindowEnd_JTextField.setToolTipText("Specify the output window end ${Property} - will override the above.");
    __OutputWindowEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __OutputWindowEnd_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional (default=full year)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Action if table not found:"), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IfTableNotFound_JComboBox = new SimpleJComboBox ( false );
	List<String> notFoundChoices = new ArrayList<String>();
	notFoundChoices.add ( "" );
	notFoundChoices.add ( __command._Create );
	notFoundChoices.add ( __command._Warn );
	__IfTableNotFound_JComboBox.setData(notFoundChoices);
	__IfTableNotFound_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __IfTableNotFound_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - action if table not found (default=" + __command._Warn + ")."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 4, 50 );
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
    checkGUIState();
	refresh ();
	checkGUIState(); // To make sure output window is set up

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
	setResizable ( false );
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent e )
{	checkGUIState();
    refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

    refresh ();
	if ( code == KeyEvent.VK_ENTER ) {
		checkInput();
		if ( !__error_wait ) {
			response ( false );
		}
	}
}

public void keyReleased ( KeyEvent event )
{    refresh();
}

public void keyTyped ( KeyEvent event ) {;}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user canceled.
*/
public boolean ok ()
{   return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = getClass().getSimpleName() + ".refresh";
    String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String TableID = "";
    String DateTimeColumn = "";
    String TableTSIDColumn = "";
    String TableTSIDFormat = "";
    String IncludeMissingValues = "";
    String ValueColumn = "";
    String OutputPrecision = "";
    String FlagColumn = "";
    String OutputStart = "";
    String OutputEnd = "";
    String OutputWindowStart = "";
    String OutputWindowEnd = "";
    String IfTableNotFound = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        TSList = props.getValue ( "TSList" );
        TSID = props.getValue ( "TSID" );
        EnsembleID = props.getValue ( "EnsembleID" );
        TableID = props.getValue ( "TableID" );
        OutputStart = props.getValue ( "OutputStart" );
        OutputEnd = props.getValue ( "OutputEnd" );
        OutputWindowStart = props.getValue ( "OutputWindowStart" );
        OutputWindowEnd = props.getValue ( "OutputWindowEnd" );
        DateTimeColumn = props.getValue ( "DateTimeColumn" );
        TableTSIDColumn = props.getValue ( "TableTSIDColumn" );
        TableTSIDFormat = props.getValue ( "TableTSIDFormat" );
        IncludeMissingValues = props.getValue ( "IncludeMissingValues" );
        ValueColumn = props.getValue ( "ValueColumn" );
        OutputPrecision = props.getValue ( "OutputPrecision" );
        FlagColumn = props.getValue ( "FlagColumn" );
        IfTableNotFound = props.getValue ( "IfTableNotFound" );
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
        if ( TableID == null ) {
            // Select default...
            if ( __TableID_JComboBox.getItemCount() > 0 ) {
                __TableID_JComboBox.select ( 0 );
            }
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TableID_JComboBox,TableID, JGUIUtil.NONE, null, null ) ) {
                __TableID_JComboBox.select ( TableID );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTableID value \"" + TableID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( DateTimeColumn != null ) {
            __DateTimeColumn_JTextField.setText ( DateTimeColumn );
        }
        if ( TableTSIDColumn != null ) {
            __TableTSIDColumn_JTextField.setText ( TableTSIDColumn );
        }
        if (TableTSIDFormat != null ) {
            __TableTSIDFormat_JTextField.setText(TableTSIDFormat.trim());
        }
        if ( IncludeMissingValues == null ) {
            // Select default...
            __IncludeMissingValues_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem(__IncludeMissingValues_JComboBox,
                IncludeMissingValues, JGUIUtil.NONE, null, null )) {
                __IncludeMissingValues_JComboBox.select ( IncludeMissingValues );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\n" +
                "IncludeMissingValues value \"" + IncludeMissingValues +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( ValueColumn != null ) {
            __ValueColumn_JTextField.setText ( ValueColumn );
        }
        if ( OutputPrecision != null ) {
            __OutputPrecision_JTextField.setText ( OutputPrecision );
        }
        if ( FlagColumn != null ) {
            __FlagColumn_JTextField.setText ( FlagColumn );
        }
        if ( OutputStart != null ) {
            __OutputStart_JTextField.setText ( OutputStart );
        }
        if ( OutputEnd != null ) {
            __OutputEnd_JTextField.setText ( OutputEnd );
        }
        if ( (OutputWindowStart != null) && !OutputWindowStart.isEmpty() ) {
        	if ( OutputWindowStart.indexOf("${") >= 0 ) {
        		__OutputWindowStart_JTextField.setText ( OutputWindowStart );
        	}
        	else {
	            try {
	                // Add year because it is not part of the parameter value...
	                DateTime OutputWindowStart_DateTime = DateTime.parse (
	                    "" + DateTimeWindow.WINDOW_YEAR + "-" + OutputWindowStart );
	                Message.printStatus(2, routine, "Setting window start to " + OutputWindowStart_DateTime );
	                __OutputWindowStart_JPanel.setDateTime ( OutputWindowStart_DateTime );
	            }
	            catch ( Exception e ) {
	                Message.printWarning( 1, routine, "OutputWindowStart (" + OutputWindowStart +
	                    ") prepended with " + DateTimeWindow.WINDOW_YEAR + " is not a valid date/time." );
	            }
        	}
        }
        if ( (OutputWindowEnd != null) && !OutputWindowEnd.isEmpty() ) {
        	if ( OutputWindowEnd.indexOf("${") >= 0 ) {
        		__OutputWindowEnd_JTextField.setText ( OutputWindowEnd );
        	}
        	else {
	            try {
	                // Add year because it is not part of the parameter value...
	                DateTime OutputWindowEnd_DateTime = DateTime.parse (
	                    "" + DateTimeWindow.WINDOW_YEAR + "-" + OutputWindowEnd );
	                Message.printStatus(2, routine, "Setting window end to " + OutputWindowEnd_DateTime );
	                __OutputWindowEnd_JPanel.setDateTime ( OutputWindowEnd_DateTime );
	            }
	            catch ( Exception e ) {
	                Message.printWarning( 1, routine, "OutputWindowEnd (" + OutputWindowEnd +
	                    ") prepended with " + DateTimeWindow.WINDOW_YEAR + " is not a valid date/time." );
	            }
        	}
        }
        if ( ((OutputWindowStart != null) && !OutputWindowStart.isEmpty() ) ||
            ((OutputWindowEnd != null) && !OutputWindowEnd.isEmpty()) ) {
            __OutputWindow_JCheckBox.setSelected ( true );
        }
        else {
            __OutputWindow_JCheckBox.setSelected ( false );
        }
        if ( IfTableNotFound == null ) {
            // Select default...
            __IfTableNotFound_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem(__IfTableNotFound_JComboBox,
                IfTableNotFound, JGUIUtil.NONE, null, null )) {
                __IfTableNotFound_JComboBox.select ( IfTableNotFound );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\n" +
                "IfTableNotFound value \"" + IfTableNotFound +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
	}
    // Regardless, reset the command from the fields...
    checkGUIState();
    TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    TableID = __TableID_JComboBox.getSelected();
    DateTimeColumn = __DateTimeColumn_JTextField.getText().trim();
    TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
    TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
    IncludeMissingValues = __IncludeMissingValues_JComboBox.getSelected();
    ValueColumn = __ValueColumn_JTextField.getText().trim();
    OutputPrecision = __OutputPrecision_JTextField.getText().trim();
    FlagColumn = __FlagColumn_JTextField.getText().trim();
    OutputStart = __OutputStart_JTextField.getText().trim();
    OutputEnd = __OutputEnd_JTextField.getText().trim();
    IfTableNotFound = __IfTableNotFound_JComboBox.getSelected();
    props = new PropList ( __command.getCommandName() );
    props.add ( "TSList=" + TSList );
    props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
    props.add ( "TableID=" + TableID );
    props.add ( "DateTimeColumn=" + DateTimeColumn );
    props.add ( "TableTSIDColumn=" + TableTSIDColumn );
    props.add ( "TableTSIDFormat=" + TableTSIDFormat );
    props.add ( "IncludeMissingValues=" + IncludeMissingValues );
    props.add ( "ValueColumn=" + ValueColumn );
    props.add ( "OutputPrecision=" + OutputPrecision );
    props.add ( "FlagColumn=" + FlagColumn );
    props.add ( "Transformation=" + IfTableNotFound );
    props.add ( "OutputStart=" + OutputStart );
    props.add ( "OutputEnd=" + OutputEnd );
    props.add ( "IfTableNotFound=" + IfTableNotFound );
    if ( __OutputWindow_JCheckBox.isSelected() ) {
        OutputWindowStart = __OutputWindowStart_JPanel.toString(false,true).trim();
        if ( OutputWindowStart.startsWith("99") ) {
        	// 99 is used as placeholder when month is not set... artifact of setting choices to blank during editing
        	OutputWindowStart = "";
        }
        OutputWindowEnd = __OutputWindowEnd_JPanel.toString(false,true).trim();
        if ( OutputWindowEnd.startsWith("99") ) {
        	// 99 is used as placeholder when month is not set... artifact of setting choices to blank during editing
        	OutputWindowEnd = "";
        }
        String OutputWindowStart2 = __OutputWindowStart_JTextField.getText().trim();
        String OutputWindowEnd2 = __OutputWindowEnd_JTextField.getText().trim();
        if ( (OutputWindowStart2 != null) && !OutputWindowStart2.isEmpty() ) {
        	OutputWindowStart = OutputWindowStart2;
        }
        if ( (OutputWindowEnd2 != null) && !OutputWindowEnd2.isEmpty() ) {
        	OutputWindowEnd = OutputWindowEnd2;
        }
        props.add ( "OutputWindowStart=" + OutputWindowStart );
        props.add ( "OutputWindowEnd=" + OutputWindowEnd );
    }
    __command_JTextArea.setText( __command.toString ( props ).trim() );
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
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

public void windowActivated( WindowEvent evt ){;}
public void windowClosed( WindowEvent evt ){;}
public void windowDeactivated( WindowEvent evt ){;}
public void windowDeiconified( WindowEvent evt ){;}
public void windowIconified( WindowEvent evt ){;}
public void windowOpened( WindowEvent evt ){;}

}
