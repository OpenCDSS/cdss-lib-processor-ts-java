// CreateTimeSeriesEventTable_JDialog - editor dialog for CreateTimeSeriesEventTable command

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2022 Colorado Department of Natural Resources

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

import javax.swing.BorderFactory;
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

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

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

import java.util.List;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.DictionaryJDialog;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

@SuppressWarnings("serial")
public class CreateTimeSeriesEventTable_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{

private boolean __error_wait = false;
private boolean __first_time = true;
private JTextArea __command_JTextArea = null;
private JTabbedPane __main_JTabbedPane = null;
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __NewTableID_JTextField = null;
private JTextArea __TimeSeriesLocations_JTextArea = null;
private JTextField __IncludeColumns_JTextField = null;
private JTextField __InputTableEventIDColumn_JTextField = null;
private JTextField __InputTableEventTypeColumn_JTextField = null;
private JTextField __IncludeInputTableEventTypes_JTextField = null;
private JTextField __InputTableEventStartColumn_JTextField = null;
private JTextField __InputTableEventEndColumn_JTextField = null;
private JTextArea __InputTableEventLocationColumns_JTextArea = null;
private JTextField __InputTableEventLabelColumn_JTextField = null;
private JTextField __InputTableEventDescriptionColumn_JTextField = null;
private JTextField __OutputTableTSIDColumn_JTextField = null;
private TSFormatSpecifiersJPanel __OutputTableTSIDFormat_JTextField = null; // Format for time series identifiers.
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private CreateTimeSeriesEventTable_Command __command = null;
private boolean __ok = false;
private JFrame __parent = null;

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of table identifiers to provide as choices
*/
public CreateTimeSeriesEventTable_JDialog ( JFrame parent, CreateTimeSeriesEventTable_Command command, List<String> tableIDChoices )
{	super(parent, true);
	initialize ( parent, command, tableIDChoices );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed(ActionEvent event)
{	Object o = event.getSource();

    if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "CreateTimeSeriesEventTable");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput ();
		if ( !__error_wait ) {
			// Command has been edited...
			response ( true );
		}
	}
    else if ( event.getActionCommand().equalsIgnoreCase("EditInputTableEventLocationColumns") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String InputTableEventLocationColumns = __InputTableEventLocationColumns_JTextArea.getText().trim();
        String [] notes = { "Specify the event table columns for:",
                "Location Type - location type (e.g., \"County\" or \"Basin\")",
                "Location Column Name - name of column containing location identifiers (e.g., county name) for the above."};
        String dict = (new DictionaryJDialog ( __parent, true, InputTableEventLocationColumns,
            "Edit InputTableEventLocationColumns Parameter", notes, "Location Type", "Location ID Column Name",10)).response();
        if ( dict != null ) {
            __InputTableEventLocationColumns_JTextArea.setText ( dict );
            refresh();
        }
    }
    else if ( event.getActionCommand().equalsIgnoreCase("EditTimeSeriesLocations") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String TimeSeriesLocations = __TimeSeriesLocations_JTextArea.getText().trim();
        String [] notes = { "Specify the time series data lookup for:",
            "Time Series Location Type - location type (e.g., \"County\" or \"Basin\")",
            "Location ID Format Specifier - for example %L for time series identifer location ID, ${TS:property} for time series property.",
            "The time series location type and identifier are matched against the event table data to associate events with time series."};
        String dict = (new DictionaryJDialog ( __parent, true, TimeSeriesLocations,
            "Edit TimeSeriesLocations Parameter", notes, "Time Series Location Type", "Location ID Format Specifier",10)).response();
        if ( dict != null ) {
            __TimeSeriesLocations_JTextArea.setText ( dict );
            refresh();
        }
    }
	else {
        checkGUIState();
        refresh();
	}
}

/**
Check the GUI state to make sure that appropriate components are enabled/disabled.
*/
private void checkGUIState () {
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
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check.
	PropList props = new PropList ( "" );
	String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String TimeSeriesLocations = __TimeSeriesLocations_JTextArea.getText().trim().replace("\n"," ");
	String TableID = __TableID_JComboBox.getSelected();
	String IncludeColumns = __IncludeColumns_JTextField.getText().trim();
	String InputTableEventIDColumn = __InputTableEventIDColumn_JTextField.getText().trim();
	String InputTableEventTypeColumn = __InputTableEventTypeColumn_JTextField.getText().trim();
	String IncludeInputTableEventTypes = __IncludeInputTableEventTypes_JTextField.getText().trim();
	String InputTableEventStartColumn = __InputTableEventStartColumn_JTextField.getText().trim();
	String InputTableEventEndColumn = __InputTableEventEndColumn_JTextField.getText().trim();
	String InputTableEventLocationColumns = __InputTableEventLocationColumns_JTextArea.getText().trim().replace("\n"," ");
	String InputTableEventLabelColumn = __InputTableEventLabelColumn_JTextField.getText().trim();
	String InputTableEventDescriptionColumn = __InputTableEventDescriptionColumn_JTextField.getText().trim();
    String NewTableID = __NewTableID_JTextField.getText().trim();
	String OutputTableTSIDColumn = __OutputTableTSIDColumn_JTextField.getText().trim();
	String OutputTableTSIDFormat = __OutputTableTSIDFormat_JTextField.getText().trim();
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
    if ( TimeSeriesLocations.length() > 0 ) {
        props.set ( "TimeSeriesLocations", TimeSeriesLocations );
    }
    if ( TableID.length() > 0 ) {
        props.set ( "TableID", TableID );
    }
	if ( IncludeColumns.length() > 0 ) {
		props.set ( "IncludeColumns", IncludeColumns );
	}
    if ( InputTableEventIDColumn.length() > 0 ) {
        props.set ( "InputTableEventIDColumn", InputTableEventIDColumn );
    }
    if ( InputTableEventTypeColumn.length() > 0 ) {
        props.set ( "InputTableEventTypeColumn", InputTableEventTypeColumn );
    }
    if ( IncludeInputTableEventTypes.length() > 0 ) {
        props.set ( "IncludeInputTableEventTypes", IncludeInputTableEventTypes );
    }
    if ( InputTableEventStartColumn.length() > 0 ) {
        props.set ( "InputTableEventStartColumn", InputTableEventStartColumn );
    }
    if ( InputTableEventEndColumn.length() > 0 ) {
        props.set ( "InputTableEventEndColumn", InputTableEventEndColumn );
    }
    if ( InputTableEventLocationColumns.length() > 0 ) {
        props.set ( "InputTableEventLocationColumns", InputTableEventLocationColumns );
    }
    if ( InputTableEventLabelColumn.length() > 0 ) {
        props.set ( "InputTableEventLabelColumn", InputTableEventLabelColumn );
    }
    if ( InputTableEventDescriptionColumn.length() > 0 ) {
        props.set ( "InputTableEventDescriptionColumn", InputTableEventDescriptionColumn );
    }
    if ( NewTableID.length() > 0 ) {
        props.set ( "NewTableID", NewTableID );
    }
    if ( OutputTableTSIDColumn.length() > 0 ) {
        props.set ( "OutputTableTSIDColumn", OutputTableTSIDColumn );
    }
    if ( OutputTableTSIDFormat.length() > 0 ) {
        props.set ( "OutputTableTSIDFormat", OutputTableTSIDFormat );
    }
	try {
	    // This will warn the user.
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
{	String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String TimeSeriesLocations = __TimeSeriesLocations_JTextArea.getText().trim().replace("\n"," ");
    String TableID = __TableID_JComboBox.getSelected();
    String IncludeColumns = __IncludeColumns_JTextField.getText().trim();
    String InputTableEventIDColumn = __InputTableEventIDColumn_JTextField.getText().trim();
    String InputTableEventTypeColumn = __InputTableEventTypeColumn_JTextField.getText().trim();
    String IncludeInputTableEventTypes = __IncludeInputTableEventTypes_JTextField.getText().trim();
    String InputTableEventStartColumn = __InputTableEventStartColumn_JTextField.getText().trim();
    String InputTableEventEndColumn = __InputTableEventEndColumn_JTextField.getText().trim();
    String InputTableEventLocationColumns = __InputTableEventLocationColumns_JTextArea.getText().trim().replace("\n"," ");
    String InputTableEventLabelColumn = __InputTableEventLabelColumn_JTextField.getText().trim();
    String InputTableEventDescriptionColumn = __InputTableEventDescriptionColumn_JTextField.getText().trim();
    String NewTableID = __NewTableID_JTextField.getText().trim();
    String OutputTableTSIDColumn = __OutputTableTSIDColumn_JTextField.getText().trim();
    String OutputTableTSIDFormat = __OutputTableTSIDFormat_JTextField.getText().trim();
    __command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "TimeSeriesLocations", TimeSeriesLocations );
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "IncludeColumns", IncludeColumns );
	__command.setCommandParameter ( "InputTableEventIDColumn", InputTableEventIDColumn );
	__command.setCommandParameter ( "InputTableEventTypeColumn", InputTableEventTypeColumn );
	__command.setCommandParameter ( "IncludeInputTableEventTypes", IncludeInputTableEventTypes );
	__command.setCommandParameter ( "InputTableEventStartColumn", InputTableEventStartColumn );
	__command.setCommandParameter ( "InputTableEventEndColumn", InputTableEventEndColumn );
	__command.setCommandParameter ( "InputTableEventLocationColumns", InputTableEventLocationColumns );
	__command.setCommandParameter ( "InputTableEventLabelColumn", InputTableEventLabelColumn );
	__command.setCommandParameter ( "InputTableEventDescriptionColumn", InputTableEventDescriptionColumn );
    __command.setCommandParameter ( "NewTableID", NewTableID );
	__command.setCommandParameter ( "OutputTableTSIDColumn", OutputTableTSIDColumn );
    __command.setCommandParameter ( "OutputTableTSIDFormat", OutputTableTSIDFormat );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit and possibly run.
*/
private void initialize ( JFrame parent, CreateTimeSeriesEventTable_Command command, List<String> tableIDChoices )
{	__command = command;
    __parent = parent;

	addWindowListener(this);

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel.

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout(new GridBagLayout());
	getContentPane().add ("North", main_JPanel);
	int y = -1;

	JPanel paragraph = new JPanel();
	paragraph.setLayout(new GridBagLayout());
	int yy = 0;
    
   	JGUIUtil.addComponent(paragraph, new JLabel (
        "This command creates a new time series event table, which contains time series events " +
        "that have temporal and spatial properties."),
        0, yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "Events may be based on analysis of the time series or historical event data (such as regional floods and droughts)."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	JGUIUtil.addComponent(main_JPanel, paragraph,
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
    __TSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel ( this, main_JPanel, __TSList_JComboBox, y );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits.
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, y );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits.
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
        this, this, main_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );
	
    __main_JTabbedPane = new JTabbedPane ();
    __main_JTabbedPane.setBorder(
        BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
        "Specify how to create the time series event table" ));
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for creating the event table by analyzing time series.
    int yAnalysis = -1;
    JPanel analysis_JPanel = new JPanel();
    analysis_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Analyze time series", analysis_JPanel );

    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
        "Analyze the time series data to detect events, by evaluating data trends, peaks, and valleys."),
        0, ++yAnalysis, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
        "<html>This is experimental functionality.</html>"),
        0, ++yAnalysis, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
        "Currently, only peaks are analyzed (high value with lower value on each side)."),
        0, ++yAnalysis, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
        "The following table columns are automatically added:"),
        0, ++yAnalysis, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
        "  EventStartDateTime, EventStartValue"),
        0, ++yAnalysis, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
        "  EventExtremeDateTime, EventExtremeValue"),
        0, ++yAnalysis, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
        "  EventEndDateTime, EventEndValue"),
        0, ++yAnalysis, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	JGUIUtil.addComponent(analysis_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yAnalysis, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel for creating the event table from an input table.
    int yTable = -1;
    JPanel table_JPanel = new JPanel();
    table_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Create events from existing table", table_JPanel );

    JGUIUtil.addComponent(table_JPanel, new JLabel (
        "Generate events by associating input table events with time series, for example by matching time series location."),
        0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	JGUIUtil.addComponent(table_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Time series location type and ID:"),
        0, ++yTable, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TimeSeriesLocations_JTextArea = new JTextArea (4,35);
    __TimeSeriesLocations_JTextArea.setLineWrap ( true );
    __TimeSeriesLocations_JTextArea.setWrapStyleWord ( true );
    __TimeSeriesLocations_JTextArea.setToolTipText("LocationType1:TimeSeriesFormatSpecifier1,LocationType2:TimeSeriesFormatSpecifier2");
    __TimeSeriesLocations_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(table_JPanel, new JScrollPane(__TimeSeriesLocations_JTextArea),
        1, yTable, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Required - time series location type and ID."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(table_JPanel, new SimpleJButton ("Edit","EditTimeSeriesLocations",this),
        3, ++yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table ID:" ), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit.
    tableIDChoices.add(0,""); // Add blank to ignore table.
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(table_JPanel, __TableID_JComboBox,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel( "Required - input event table."), 
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Column names to copy:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IncludeColumns_JTextField = new JTextField (10);
    __IncludeColumns_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __IncludeColumns_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Optional - names of columns to copy (default=copy all)."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Event ID column:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputTableEventIDColumn_JTextField = new JTextField (10);
    __InputTableEventIDColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __InputTableEventIDColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Required - input table column name for event ID."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Event type column:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputTableEventTypeColumn_JTextField = new JTextField (10);
    __InputTableEventTypeColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __InputTableEventTypeColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Required - input table column name for event type."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Event types to include:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IncludeInputTableEventTypes_JTextField = new JTextField (10);
    __IncludeInputTableEventTypes_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __IncludeInputTableEventTypes_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Optional - input table event types to include (default=all)."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Event start column:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputTableEventStartColumn_JTextField = new JTextField (10);
    __InputTableEventStartColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __InputTableEventStartColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Required - input table column name for event start."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Event end column:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputTableEventEndColumn_JTextField = new JTextField (10);
    __InputTableEventEndColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __InputTableEventEndColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Required - input table column name for event end."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Event location type and ID columns:"),
        0, ++yTable, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputTableEventLocationColumns_JTextArea = new JTextArea (4,35);
    __InputTableEventLocationColumns_JTextArea.setLineWrap ( true );
    __InputTableEventLocationColumns_JTextArea.setWrapStyleWord ( true );
    __InputTableEventLocationColumns_JTextArea.setToolTipText("OriginalColumn1:NewColumn1,OriginalColumn2:NewColumn2");
    __InputTableEventLocationColumns_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(table_JPanel, new JScrollPane(__InputTableEventLocationColumns_JTextArea),
        1, yTable, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Required - input table column names for location type and ID."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(table_JPanel, new SimpleJButton ("Edit","EditInputTableEventLocationColumns",this),
        3, ++yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Event label column:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputTableEventLabelColumn_JTextField = new JTextField (10);
    __InputTableEventLabelColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __InputTableEventLabelColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Required - input table column name for event label."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Event description column:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputTableEventDescriptionColumn_JTextField = new JTextField (10);
    __InputTableEventDescriptionColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __InputTableEventDescriptionColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Required - input table column name for event description."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("New table ID:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NewTableID_JTextField = new JTextField (10);
    __NewTableID_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __NewTableID_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - unique identifier for the new table."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output table TSID column:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputTableTSIDColumn_JTextField = new JTextField ( 10 );
    __OutputTableTSIDColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __OutputTableTSIDColumn_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required if using table - column name for TSID."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("Output table format for TSID:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputTableTSIDFormat_JTextField = new TSFormatSpecifiersJPanel(10);
    __OutputTableTSIDFormat_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
    __OutputTableTSIDFormat_JTextField.addKeyListener ( this );
    __OutputTableTSIDFormat_JTextField.setToolTipText("%L for location, %T for data type.");
    JGUIUtil.addComponent(main_JPanel, __OutputTableTSIDFormat_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - use %L for location, etc. (default=alias or TSID)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Command:"), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (4,40);
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable (false);
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents.
    checkGUIState();
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
	setResizable (false);
    super.setVisible(true);
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged (ItemEvent e) {
    checkGUIState();
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

public void keyTyped (KeyEvent event) {
	
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
{	String routine = getClass().getName() + ".refresh";
    String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String TimeSeriesLocations = "";
    String TableID = "";
    String IncludeColumns = "";
    String InputTableEventIDColumn = "";
    String InputTableEventTypeColumn = "";
    String IncludeInputTableEventTypes = "";
    String InputTableEventStartColumn = "";
    String InputTableEventEndColumn = "";
    String InputTableEventLocationColumns = "";
    String InputTableEventLabelColumn = "";
    String InputTableEventDescriptionColumn = "";
    String NewTableID = "";
    String OutputTableTSIDColumn = "";
    String OutputTableTSIDFormat = "";
	PropList props = __command.getCommandParameters();
	if (__first_time) {
		__first_time = false;
        TSList = props.getValue ( "TSList" );
        TSID = props.getValue ( "TSID" );
        EnsembleID = props.getValue ( "EnsembleID" );
        TimeSeriesLocations = props.getValue ( "TimeSeriesLocations" );
        TableID = props.getValue ( "TableID" );
        IncludeColumns = props.getValue ( "IncludeColumns" );
        InputTableEventIDColumn = props.getValue ( "InputTableEventIDColumn" );
        InputTableEventTypeColumn = props.getValue ( "InputTableEventTypeColumn" );
        IncludeInputTableEventTypes = props.getValue ( "IncludeInputTableEventTypes" );
        InputTableEventStartColumn = props.getValue ( "InputTableEventStartColumn" );
        InputTableEventEndColumn = props.getValue ( "InputTableEventEndColumn" );
        InputTableEventLocationColumns = props.getValue ( "InputTableEventLocationColumns" );
        InputTableEventLabelColumn = props.getValue ( "InputTableEventLabelColumn" );
        InputTableEventDescriptionColumn = props.getValue ( "InputTableEventDescriptionColumn" );
        NewTableID = props.getValue ( "NewTableID" );
        OutputTableTSIDColumn = props.getValue ( "OutputTableTSIDColumn" );
        OutputTableTSIDFormat = props.getValue ( "OutputTableTSIDFormat" );
        if ( TSList == null ) {
            // Select default.
            __TSList_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TSList_JComboBox,TSList, JGUIUtil.NONE, null, null ) ) {
                __TSList_JComboBox.select ( TSList );
            }
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid\nTSList value \"" + TSList +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID, JGUIUtil.NONE, null, null ) ) {
            __TSID_JComboBox.select ( TSID );
        }
        else {
            // Automatically add to the list after the blank.
            if ( (TSID != null) && (TSID.length() > 0) ) {
                __TSID_JComboBox.insertItemAt ( TSID, 1 );
                // Select.
                __TSID_JComboBox.select ( TSID );
            }
            else {
                // Select the blank.
                __TSID_JComboBox.select ( 0 );
            }
        }
        if ( EnsembleID == null ) {
            // Select default.
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
        if ( TimeSeriesLocations != null ) {
            __TimeSeriesLocations_JTextArea.setText ( TimeSeriesLocations );
        }
        if ( TableID == null ) {
            // Select default.
            __TableID_JComboBox.select ( 0 );
            __main_JTabbedPane.setSelectedIndex(0);
        }
        else {
            __main_JTabbedPane.setSelectedIndex(1);
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
		if ( IncludeColumns != null ) {
			__IncludeColumns_JTextField.setText ( IncludeColumns );
		}
        if ( InputTableEventIDColumn != null ) {
            __InputTableEventIDColumn_JTextField.setText ( InputTableEventIDColumn );
        }
        if ( InputTableEventTypeColumn != null ) {
            __InputTableEventTypeColumn_JTextField.setText ( InputTableEventTypeColumn );
        }
        if ( IncludeInputTableEventTypes != null ) {
            __IncludeInputTableEventTypes_JTextField.setText ( IncludeInputTableEventTypes );
        }
        if ( InputTableEventStartColumn != null ) {
            __InputTableEventStartColumn_JTextField.setText ( InputTableEventStartColumn );
        }
        if ( InputTableEventEndColumn != null ) {
            __InputTableEventEndColumn_JTextField.setText ( InputTableEventEndColumn );
        }
        if ( InputTableEventLocationColumns != null ) {
            __InputTableEventLocationColumns_JTextArea.setText ( InputTableEventLocationColumns );
        }
        if ( InputTableEventLabelColumn != null ) {
            __InputTableEventLabelColumn_JTextField.setText ( InputTableEventLabelColumn );
        }
        if ( InputTableEventDescriptionColumn != null ) {
            __InputTableEventDescriptionColumn_JTextField.setText ( InputTableEventDescriptionColumn );
        }
        if ( NewTableID != null ) {
            __NewTableID_JTextField.setText ( NewTableID );
        }
        if ( OutputTableTSIDColumn != null ) {
            __OutputTableTSIDColumn_JTextField.setText ( OutputTableTSIDColumn );
        }
        if (OutputTableTSIDFormat != null ) {
            __OutputTableTSIDFormat_JTextField.setText(OutputTableTSIDFormat.trim());
        }
	}
	// Regardless, reset the command from the fields.
	checkGUIState();
	TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    TimeSeriesLocations = __TimeSeriesLocations_JTextArea.getText().trim();
	TableID = __TableID_JComboBox.getSelected();
	IncludeColumns = __IncludeColumns_JTextField.getText().trim();
	InputTableEventIDColumn = __InputTableEventIDColumn_JTextField.getText().trim();
	InputTableEventTypeColumn = __InputTableEventTypeColumn_JTextField.getText().trim();
	IncludeInputTableEventTypes = __IncludeInputTableEventTypes_JTextField.getText().trim();
	InputTableEventStartColumn = __InputTableEventStartColumn_JTextField.getText().trim();
	InputTableEventEndColumn = __InputTableEventEndColumn_JTextField.getText().trim();
	InputTableEventLocationColumns = __InputTableEventLocationColumns_JTextArea.getText().trim();
	InputTableEventLabelColumn = __InputTableEventLabelColumn_JTextField.getText().trim();
	InputTableEventDescriptionColumn = __InputTableEventDescriptionColumn_JTextField.getText().trim();
    NewTableID = __NewTableID_JTextField.getText().trim();
    OutputTableTSIDColumn = __OutputTableTSIDColumn_JTextField.getText().trim();
    OutputTableTSIDFormat = __OutputTableTSIDFormat_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
    props.add ( "TSList=" + TSList );
    props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
    props.add ( "TimeSeriesLocations=" + TimeSeriesLocations );
    props.add ( "TableID=" + TableID );
    props.add ( "IncludeColumns=" + IncludeColumns );
	props.add ( "InputTableEventIDColumn=" + InputTableEventIDColumn );
	props.add ( "InputTableEventTypeColumn=" + InputTableEventTypeColumn );
	props.add ( "IncludeInputTableEventTypes=" + IncludeInputTableEventTypes );
	props.add ( "InputTableEventStartColumn=" + InputTableEventStartColumn );
	props.add ( "InputTableEventEndColumn=" + InputTableEventEndColumn );
	props.add ( "InputTableEventLocationColumns=" + InputTableEventLocationColumns );
	props.add ( "InputTableEventLabelColumn=" + InputTableEventLabelColumn );
	props.add ( "InputTableEventDescriptionColumn=" + InputTableEventDescriptionColumn );
    props.add ( "NewTableID=" + NewTableID );
    props.add ( "OutputTableTSIDColumn=" + OutputTableTSIDColumn );
    props.add ( "OutputTableTSIDFormat=" + OutputTableTSIDFormat );
	__command_JTextArea.setText( __command.toString ( props ).trim() );
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
*/
private void response ( boolean ok )
{	__ok = ok;	// Save to be returned by ok().
	if ( ok ) {
		// Commit the changes.
		commitEdits ();
		if ( __error_wait ) {
			// Not ready to close out!
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
public void windowClosing(WindowEvent event) {
	response ( false );
}

// The following methods are all necessary because this class implements WindowListener.
public void windowActivated(WindowEvent evt)	{}
public void windowClosed(WindowEvent evt)	{}
public void windowDeactivated(WindowEvent evt)	{}
public void windowDeiconified(WindowEvent evt)	{}
public void windowIconified(WindowEvent evt)	{}
public void windowOpened(WindowEvent evt)	{}

}