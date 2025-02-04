// For_JDialog - Editor dialog for the For() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2024 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.util;

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

import RTi.Util.GUI.DictionaryJDialog;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Time.TimeInterval;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

/**
Editor dialog for the For() command.
*/
@SuppressWarnings("serial")
public class For_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private For_Command __command = null;
// General (top).
private JTextField __Name_JTextField = null;
private JTextField __IteratorProperty_JTextField = null;
private JTextField __IndexProperty_JTextField = null;
private SimpleJComboBox __ShowProgress_JComboBox;
private JTabbedPane __main_JTabbedPane = null;
// List.
private JTextField __IteratorValueProperty_JTextField = null;
private JTextArea __List_JTextArea = null;
// Sequence.
private JTextField __SequenceStart_JTextField = null;
private JTextField __SequenceEnd_JTextField = null;
private JTextField __SequenceIncrement_JTextField = null;
// Table.
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __TableColumn_JTextField = null;
private JTextArea __TablePropertyMap_JTextArea = null;
// Time period.
private JTextField __PeriodStart_JTextField = null;
private JTextField __PeriodEnd_JTextField = null;
private SimpleJComboBox __PeriodIncrement_JComboBox = null;
// Time series list.
private SimpleJComboBox	__TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private JTextArea __TimeSeriesPropertyMap_JTextArea = null;
private JTextArea __command_JTextArea = null;
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether user pressed OK to close the dialog.
private JFrame __parent = null;

/**
Command dialog editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public For_JDialog ( JFrame parent, For_Command command, List<String> tableIDChoices ) {
 	super(parent, true);
	initialize ( parent, command, tableIDChoices );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event ) {
	Object o = event.getSource();

	if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "For");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
    else if ( event.getActionCommand().equalsIgnoreCase("EditTablePropertyMap") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String TablePropertyMap = __TablePropertyMap_JTextArea.getText().trim();
        String [] notes = {
            "Column names in the table can be mapped to processor property names, to set table values as properties.",
            "The property will be of the same object type as in the table (e.g., integer column -> integer property)."
        };
        String dict = (new DictionaryJDialog ( __parent, true, TablePropertyMap,
            "Edit TablePropertyMap Parameter", notes, "Column Name", "Property Name",10)).response();
        if ( dict != null ) {
        	__TablePropertyMap_JTextArea.setText ( dict );
            refresh();
        }
    }
    else if ( event.getActionCommand().equalsIgnoreCase("EditTimeSeriesPropertyMap") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String TimeSeriesPropertyMap = __TimeSeriesPropertyMap_JTextArea.getText().trim();
        String [] notes = {
            "Time series properties can be mapped to processor property names, to set time series properties as properties.",
            "The property will be of the same object type as in the time series."
        };
        String dict = (new DictionaryJDialog ( __parent, true, TimeSeriesPropertyMap,
            "Edit TimeSeriesPropertyMap Parameter", notes, "Time Series Property Name", "Property Name",10)).response();
        if ( dict != null ) {
        	__TimeSeriesPropertyMap_JTextArea.setText ( dict );
            refresh();
        }
    }
    else {
    	// No special action.
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

    /* TODO smalers 2021-06-06 don't need as long as parameter is only used with time series.
    if ( (TSList == null) || TSList.isEmpty() ) {
    	__IteratorValueProperty_JTextField.setEnabled(false);
    }
    else {
    	__IteratorValueProperty_JTextField.setEnabled(true);
    }
    */
}

/**
Check the input. If errors exist, warn the user and set the __error_wait flag to true.
This should be called before response() is allowed to complete.
*/
private void checkInput () {
    // Put together a list of parameters to check.
    PropList props = new PropList ( "" );
    String Name = __Name_JTextField.getText().trim();
    String IteratorProperty = __IteratorProperty_JTextField.getText().trim();
    String IndexProperty = __IndexProperty_JTextField.getText().trim();
	String ShowProgress = __ShowProgress_JComboBox.getSelected();
	// List.
    String IteratorValueProperty = __IteratorValueProperty_JTextField.getText().trim();
    String List = __List_JTextArea.getText().trim().replace('\n', ' ').replace('\t', ' ');
    // Sequence.
    String SequenceStart = __SequenceStart_JTextField.getText().trim();
    String SequenceEnd = __SequenceEnd_JTextField.getText().trim();
    String SequenceIncrement = __SequenceIncrement_JTextField.getText().trim();
    // Table.
    String TableID = __TableID_JComboBox.getSelected();
    String TableColumn = __TableColumn_JTextField.getText().trim();
	String TablePropertyMap = __TablePropertyMap_JTextArea.getText().trim().replace("\n"," ");
	// Time period.
    String PeriodStart = __PeriodStart_JTextField.getText().trim();
    String PeriodEnd = __PeriodEnd_JTextField.getText().trim();
    String PeriodIncrement = __PeriodIncrement_JComboBox.getSelected();
    // Time Series.
	String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
	String TimeSeriesPropertyMap = __TimeSeriesPropertyMap_JTextArea.getText().trim().replace("\n"," ");
	// General.
    if ( Name.length() > 0 ) {
        props.set ( "Name", Name );
    }
    if ( !IteratorProperty.isEmpty() ) {
        props.set ( "IteratorProperty", IteratorProperty );
    }
    if ( !IndexProperty.isEmpty() ) {
        props.set ( "IndexProperty", IndexProperty );
    }
	if ( !ShowProgress.isEmpty() ) {
		props.set ( "ShowProgress", ShowProgress );
	}
	// List.
    if ( IteratorValueProperty.length() > 0 ) {
        props.set ( "IteratorValueProperty", IteratorValueProperty );
    }
    if ( List.length() > 0 ) {
        props.set ( "List", List );
    }
    // Sequence.
    if ( SequenceStart.length() > 0 ) {
        props.set ( "SequenceStart", SequenceStart );
    }
    if ( SequenceEnd.length() > 0 ) {
        props.set ( "SequenceEnd", SequenceEnd );
    }
    if ( SequenceIncrement.length() > 0 ) {
        props.set ( "SequenceIncrement", SequenceIncrement );
    }
    // Table.
    if ( TableID.length() > 0 ) {
        props.set ( "TableID", TableID );
    }
    if ( TableColumn.length() > 0 ) {
        props.set ( "TableColumn", TableColumn );
    }
    if ( TablePropertyMap.length() > 0 ) {
        props.set ( "TablePropertyMap", TablePropertyMap );
    }
    // Time period.
    if ( PeriodStart.length() > 0 ) {
        props.set ( "PeriodStart", PeriodStart );
    }
    if ( PeriodEnd.length() > 0 ) {
        props.set ( "PeriodEnd", PeriodEnd );
    }
    if ( PeriodIncrement.length() > 0 ) {
        props.set ( "PeriodIncrement", PeriodIncrement );
    }
    // Time Series.
	if ( TSList.length() > 0 ) {
		props.set ( "TSList", TSList );
	}
    if ( TSID.length() > 0 ) {
        props.set ( "TSID", TSID );
    }
    if ( EnsembleID.length() > 0 ) {
        props.set ( "EnsembleID", EnsembleID );
    }
    if ( TimeSeriesPropertyMap.length() > 0 ) {
        props.set ( "TimeSeriesPropertyMap", TimeSeriesPropertyMap );
    }
    try {
        // This will warn the user.
        __command.checkCommandParameters ( props, null, 1 );
    }
    catch ( Exception e ) {
        // The warning would have been printed in the check code.
        __error_wait = true;
    }
}

/**
Commit the edits to the command.
*/
private void commitEdits () {
	// General.
    String Name = __Name_JTextField.getText().trim();
    String IteratorProperty = __IteratorProperty_JTextField.getText().trim();
    String IndexProperty = __IndexProperty_JTextField.getText().trim();
    String ShowProgress = __ShowProgress_JComboBox.getSelected();
	// List.
    String IteratorValueProperty = __IteratorValueProperty_JTextField.getText().trim();
    String List = __List_JTextArea.getText().trim().replace('\n', ' ').replace('\t', ' ');
    // Sequence.
    String SequenceStart = __SequenceStart_JTextField.getText().trim();
    String SequenceEnd = __SequenceEnd_JTextField.getText().trim();
    String SequenceIncrement = __SequenceIncrement_JTextField.getText().trim();
    // Table.
    String TableID = __TableID_JComboBox.getSelected();
    String TableColumn = __TableColumn_JTextField.getText().trim();
    String TablePropertyMap = __TablePropertyMap_JTextArea.getText().trim().replace("\n"," ");
    // Time period.
    String PeriodStart = __PeriodStart_JTextField.getText().trim();
    String PeriodEnd = __PeriodEnd_JTextField.getText().trim();
    String PeriodIncrement = __PeriodIncrement_JComboBox.getSelected();
    // Time Series.
	String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String TimeSeriesPropertyMap = __TimeSeriesPropertyMap_JTextArea.getText().trim().replace("\n"," ");
    // General.
    __command.setCommandParameter ( "Name", Name );
    __command.setCommandParameter ( "IteratorProperty", IteratorProperty );
    __command.setCommandParameter ( "IndexProperty", IndexProperty );
    __command.setCommandParameter ( "ShowProgress", ShowProgress );
    // List.
    __command.setCommandParameter ( "IteratorValueProperty", IteratorValueProperty );
    __command.setCommandParameter ( "List", List );
    // Sequence.
    __command.setCommandParameter ( "SequenceStart", SequenceStart );
    __command.setCommandParameter ( "SequenceEnd", SequenceEnd );
    __command.setCommandParameter ( "SequenceIncrement", SequenceIncrement );
    // Table.
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "TableColumn", TableColumn );
    __command.setCommandParameter ( "TablePropertyMap", TablePropertyMap );
    // Time period.
    __command.setCommandParameter ( "PeriodStart", PeriodStart );
    __command.setCommandParameter ( "PeriodEnd", PeriodEnd );
    __command.setCommandParameter ( "PeriodIncrement", PeriodIncrement );
    // Time series.
	__command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "TimeSeriesPropertyMap", TimeSeriesPropertyMap );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIdChoices a list of table identifiers from the processor
*/
private void initialize ( JFrame parent, For_Command command, List<String> tableIDChoices ) {
    __command = command;
	__parent = parent;

	addWindowListener( this );

    Insets insetsNONE = new Insets(1,1,1,1);
    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel.

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "This command starts a \"for loop\", which repeatedly executes a block of commands while changing the " +
        "value of an iterator variable for each iteration."),
        0, ++y, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The iterator value is set as a processor property that can be accessed by other commands using the ${Property} notation."),
        0, ++y, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Use an EndFor() command with matching name to indicate the end of the for loop."),
        0, ++y, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++y, 7, 1, 0, 0, insetsNONE, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "For loop name:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Name_JTextField = new JTextField ( 20 );
    __Name_JTextField.setToolTipText("Name for the 'For' loop, must be unique for all 'For' commands and match and 'EndFor' command.");
    __Name_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Name_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Required - the name will be matched against an EndFor() command name."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "For loop iterator property:" ),
        0, ++y, 1, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IteratorProperty_JTextField = new JTextField (20);
    __IteratorProperty_JTextField.setToolTipText("Property name for property that will contain iterator value in each iteration.");
    __IteratorProperty_JTextField.addKeyListener(this);
    JGUIUtil.addComponent(main_JPanel, __IteratorProperty_JTextField,
        1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Optional - name of iterator property for iteration (default=for loop name)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "For loop index property:" ),
        0, ++y, 1, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IndexProperty_JTextField = new JTextField (20);
    __IndexProperty_JTextField.setToolTipText("Property name for property that will contain the iterator index (1+).");
    __IndexProperty_JTextField.addKeyListener(this);
    JGUIUtil.addComponent(main_JPanel, __IndexProperty_JTextField,
        1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Optional - name of index property for iteration (default=none)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Show progress?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    List<String> ShowProgress_List = new ArrayList<>( 3 );
	ShowProgress_List.add ( "" );
	ShowProgress_List.add ( __command._False );
	ShowProgress_List.add ( __command._True );
	__ShowProgress_JComboBox = new SimpleJComboBox ( false );
	__ShowProgress_JComboBox.setToolTipText("Show command progress in the TSTool UI progress bar?.");
	__ShowProgress_JComboBox.setData ( ShowProgress_List);
	__ShowProgress_JComboBox.select ( 0 );
	__ShowProgress_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __ShowProgress_JComboBox,
		1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - show progress in TSTool (default=" + __command._False + ")."),
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Tabbed panel for For input types.

    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for list iteration.
    int yList = -1;
    JPanel list_JPanel = new JPanel();
    list_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "List", list_JPanel );

    JGUIUtil.addComponent(list_JPanel, new JLabel (
        "The for loop can iterate over a list of values, separated by commas.  Currently the values are treated as strings."),
        0, ++yList, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(list_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yList, 7, 1, 0, 0, insetsNONE, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(list_JPanel, new JLabel ( "List:" ),
        0, ++yList, 1, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __List_JTextArea = new JTextArea ( 4, 60 );
    __List_JTextArea.setLineWrap ( true );
    __List_JTextArea.setWrapStyleWord ( true );
    __List_JTextArea.addKeyListener(this);
    JGUIUtil.addComponent(list_JPanel, new JScrollPane(__List_JTextArea),
        1, yList, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(list_JPanel, new JLabel("Required - list of values for iterator."),
        3, yList, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel to use integer or double sequence for iteration.
    int ySeq = -1;
    JPanel seq_JPanel = new JPanel();
    seq_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Sequence", seq_JPanel );

    JGUIUtil.addComponent(seq_JPanel, new JLabel (
        "The for loop can iterate using a sequence of values, given a start, end, and increment."),
        0, ++ySeq, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(seq_JPanel, new JLabel (
        "The type of property is detected from the start as either integer or double-precision (has decimal point)."),
        0, ++ySeq, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(seq_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++ySeq, 7, 1, 0, 0, insetsNONE, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(seq_JPanel, new JLabel ( "Sequence start:" ),
        0, ++ySeq, 1, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SequenceStart_JTextField = new JTextField (20);
    __SequenceStart_JTextField.addKeyListener(this);
    JGUIUtil.addComponent(seq_JPanel, __SequenceStart_JTextField,
        1, ySeq, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(seq_JPanel, new JLabel("Required - sequence start."),
        3, ySeq, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(seq_JPanel, new JLabel ( "Sequence end:" ),
        0, ++ySeq, 1, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SequenceEnd_JTextField = new JTextField (20);
    __SequenceEnd_JTextField.addKeyListener(this);
    JGUIUtil.addComponent(seq_JPanel, __SequenceEnd_JTextField,
        1, ySeq, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(seq_JPanel, new JLabel("Required - sequence end."),
        3, ySeq, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(seq_JPanel, new JLabel ( "Sequence increment:" ),
        0, ++ySeq, 1, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SequenceIncrement_JTextField = new JTextField (20);
    __SequenceIncrement_JTextField.addKeyListener(this);
    JGUIUtil.addComponent(seq_JPanel, __SequenceIncrement_JTextField,
        1, ySeq, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(seq_JPanel, new JLabel("Optional - sequence increment (default=1)."),
        3, ySeq, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel to use table column for iteration.
    int yTable = -1;
    JPanel table_JPanel = new JPanel();
    table_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Table", table_JPanel );

    JGUIUtil.addComponent(table_JPanel, new JLabel (
        "The for loop can iterate using the values from a table column."),
        0, ++yTable, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel (
        "If necessary, copy a subset of values from a table using CopyTable() and other table commands."),
        0, ++yTable, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel (
        "Optionally, also set properties from other columns during iteration."),
        0, ++yTable, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel (
        "The table being iterated cannot have rows added to it during iteration."),
        0, ++yTable, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yTable, 7, 1, 0, 0, insetsNONE, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table ID:" ),
        0, ++yTable, 1, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( true ); // Allow edit.
    __TableID_JComboBox.setToolTipText("Specify the table ID or use ${Property} notation");
    tableIDChoices.add(0,""); // Add blank to ignore table.
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    __TableID_JComboBox.getJTextComponent().addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __TableID_JComboBox,
        1, yTable, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel("Required - identifier for table."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table column:" ),
        0, ++yTable, 1, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableColumn_JTextField = new JTextField (20);
    __TableColumn_JTextField.setToolTipText("Table column to use for iterator values.");
    __TableColumn_JTextField.addKeyListener(this);
    JGUIUtil.addComponent(table_JPanel, __TableColumn_JTextField,
        1, yTable, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel("Required - name of table column for iterator values."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(table_JPanel, new JLabel ("Table property map:"),
        0, ++yTable, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TablePropertyMap_JTextArea = new JTextArea (6,35);
    __TablePropertyMap_JTextArea.setLineWrap ( true );
    __TablePropertyMap_JTextArea.setWrapStyleWord ( true );
    __TablePropertyMap_JTextArea.setToolTipText("ColumnName1:PropertyName1,ColumnName2:PropertyName2");
    __TablePropertyMap_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(table_JPanel, new JScrollPane(__TablePropertyMap_JTextArea),
        1, yTable, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Optional - to set properties from table (default=none set)."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(table_JPanel, new SimpleJButton ("Edit","EditTablePropertyMap",this),
        3, ++yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    // Panel to use period for iteration.
    int yPeriod = -1;
    JPanel period_JPanel = new JPanel();
    period_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Time Period", period_JPanel );

    JGUIUtil.addComponent(period_JPanel, new JLabel (
        "The for loop can iterate over a time period, given a start, end, and increment."),
        0, ++yPeriod, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(period_JPanel, new JLabel (
        "The start and end must have the same time precision and the increment must be smaller than the start and end precision."),
        0, ++yPeriod, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(period_JPanel, new JLabel (
        "The increment should be specified as a time interval like Day, 3Hour, etc."),
        0, ++yPeriod, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(period_JPanel, new JLabel (
        "Specify the start before the end to indicate that the increment should be negative."),
        0, ++yPeriod, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(period_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yPeriod, 7, 1, 0, 0, insetsNONE, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(period_JPanel, new JLabel ( "Period start:" ),
        0, ++yPeriod, 1, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PeriodStart_JTextField = new JTextField (20);
    __PeriodStart_JTextField.setToolTipText("The period start as date/time, can specify with ${Property}.");
    __PeriodStart_JTextField.addKeyListener(this);
    JGUIUtil.addComponent(period_JPanel, __PeriodStart_JTextField,
        1, yPeriod, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(period_JPanel, new JLabel("Required - period start."),
        3, yPeriod, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(period_JPanel, new JLabel ( "Period end:" ),
        0, ++yPeriod, 1, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PeriodEnd_JTextField = new JTextField (20);
    __PeriodEnd_JTextField.setToolTipText("The period end as date/time, can specify with ${Property}.");
    __PeriodEnd_JTextField.addKeyListener(this);
    JGUIUtil.addComponent(period_JPanel, __PeriodEnd_JTextField,
        1, yPeriod, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(period_JPanel, new JLabel("Required - period end."),
        3, yPeriod, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(period_JPanel, new JLabel ( "Period increment:" ),
        0, ++yPeriod, 1, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PeriodIncrement_JComboBox = new SimpleJComboBox(false); // Do not allow editing text.
    __PeriodIncrement_JComboBox.setToolTipText("The period increment as an interval (Day, -Day, 3Hour, etc.), can specify with ${Property}.");
	List<String> intervalChoices = TimeInterval.getTimeIntervalChoices(
		TimeInterval.MINUTE, TimeInterval.YEAR, false, 1, false);
	// Add a blank for default.
	intervalChoices.add(0,"");
	__PeriodIncrement_JComboBox.setData ( intervalChoices );
	__PeriodIncrement_JComboBox.addActionListener ( this );
	JGUIUtil.addComponent(period_JPanel, __PeriodIncrement_JComboBox,
		1, yPeriod, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(period_JPanel, new JLabel("Optional - period increment (default=Day)."),
        3, yPeriod, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel to use time series list for iteration.
    int yTs = -1;
    JPanel ts_JPanel = new JPanel();
    ts_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "TS List", ts_JPanel );

    JGUIUtil.addComponent(ts_JPanel, new JLabel (
        "The for loop can iterate over a list of time series, similar to other commands."),
        0, ++yTs, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
        "This may be useful when iterating using a table of time series identifiers is not sufficient."),
        0, ++yTs, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
        "Specify the 'For loop Iterator value' to indicate the iterator property value."),
        0, ++yTs, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
        "  For example, specify ${ts:alias} to use the time series built-in 'alias' property as the iteration property value."),
        0, ++yTs, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yTs, 7, 1, 0, 0, insetsNONE, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "For loop iterator value:" ),
        0, ++yTs, 1, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IteratorValueProperty_JTextField = new JTextField (20);
    __IteratorValueProperty_JTextField.setToolTipText("Optional - iterator value property, "
   		+ "Use syntax ${ts:PropertyName} for built-in time series properties (default is alias or TSID).");
    __IteratorValueProperty_JTextField.addKeyListener(this);
    JGUIUtil.addComponent(ts_JPanel, __IteratorValueProperty_JTextField,
        1, yTs, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel("Optional - iteration property value from time series (default=alias or TSID)."),
        3, yTs, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __TSList_JComboBox = new SimpleJComboBox(false);
    yTs = CommandEditorUtil.addTSListToEditorDialogPanel ( this, ts_JPanel, new JLabel("TS list"),
    	__TSList_JComboBox, yTs, new JLabel("Required for time series - indicates the time series to process.") );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true ); // Allow edits.
    __TSID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yTs = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, ts_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, yTs );

    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits.
    __EnsembleID_JComboBox.setToolTipText("Select an ensemble identifier from the list or specify with ${Property} notation");
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yTs = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
        this, this, ts_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, yTs );

    JGUIUtil.addComponent(ts_JPanel, new JLabel ("Time series property map:"),
        0, ++yTs, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TimeSeriesPropertyMap_JTextArea = new JTextArea (6,35);
    __TimeSeriesPropertyMap_JTextArea.setLineWrap ( true );
    __TimeSeriesPropertyMap_JTextArea.setWrapStyleWord ( true );
    __TimeSeriesPropertyMap_JTextArea.setToolTipText("TSProperty1:PropertyName1,TSProperty2:PropertyName2");
    __TimeSeriesPropertyMap_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(ts_JPanel, new JScrollPane(__TimeSeriesPropertyMap_JTextArea),
        1, yTs, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel ("Optional - to set processor properties from time series (default=none set)."),
        3, yTs, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(ts_JPanel, new SimpleJButton ("Edit","EditTimeSeriesPropertyMap",this),
        3, ++yTs, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
		0, ++y, 1, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __command_JTextArea = new JTextArea ( 4, 60 );
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsNONE, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	// Refresh the contents.
    checkGUIState();
	refresh ();

	// Panel for buttons.
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

	setTitle ( "Edit " + __command.getCommandName() + " command" );
    pack();
    JGUIUtil.center( this );
	setResizable ( true );
    super.setVisible( true );
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
public void keyPressed ( KeyEvent event ) {
	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( false );
		}
	}
}

public void keyReleased ( KeyEvent event ) {
	refresh();
}

public void keyTyped ( KeyEvent event ) {
}

/**
Indicate if the user pressed OK (cancel otherwise).
*/
public boolean ok () {
    return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh () {
	String routine = getClass().getSimpleName() + ".refresh";
	// General.
    String Name = "";
    String IteratorProperty = "";
    String IndexProperty = "";
	String ShowProgress = "";
	// List.
    String IteratorValueProperty = "";
    String List = "";
    // Sequence.
    String SequenceStart = "";
    String SequenceEnd = "";
    String SequenceIncrement = "";
    // Table.
	String TableID = "";
	String TableColumn = "";
	String TablePropertyMap = "";
	// Time period.
    String PeriodStart = "";
    String PeriodEnd = "";
    String PeriodIncrement = "";
    // Time series.
    String TSList = "";
    String TSID = "";
	String EnsembleID = "";
	String TimeSeriesPropertyMap = "";
	__error_wait = false;
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// General.
		Name = props.getValue( "Name" );
	    IteratorProperty = props.getValue( "IteratorProperty" );
	    IndexProperty = props.getValue( "IndexProperty" );
		ShowProgress = props.getValue ( "ShowProgress" );
		// List.
	    IteratorValueProperty = props.getValue( "IteratorValueProperty" );
	    List = props.getValue( "List" );
	    // Sequence.
	    SequenceStart = props.getValue( "SequenceStart" );
	    SequenceEnd = props.getValue( "SequenceEnd" );
	    SequenceIncrement = props.getValue( "SequenceIncrement" );
	    // Table.
		TableID = props.getValue( "TableID" );
		TableColumn = props.getValue( "TableColumn" );
		TablePropertyMap = props.getValue ( "TablePropertyMap" );
		// Time period.
	    PeriodStart = props.getValue( "PeriodStart" );
	    PeriodEnd = props.getValue( "PeriodEnd" );
	    PeriodIncrement = props.getValue( "PeriodIncrement" );
	    // Time series.
		TSList = props.getValue ( "TSList" );
        TSID = props.getValue ( "TSID" );
        EnsembleID = props.getValue ( "EnsembleID" );
		TimeSeriesPropertyMap = props.getValue ( "TimeSeriesPropertyMap" );
		// General.
		if ( Name != null ) {
		    __Name_JTextField.setText( Name );
		}
        if ( IteratorProperty != null ) {
            __IteratorProperty_JTextField.setText( IteratorProperty );
        }
        if ( IndexProperty != null ) {
            __IndexProperty_JTextField.setText( IndexProperty );
        }
		if ( (ShowProgress == null) || ShowProgress.isEmpty() ) {
			// Select default.
			__ShowProgress_JComboBox.select ( 0 );
		}
		else {
		    if ( JGUIUtil.isSimpleJComboBoxItem( __ShowProgress_JComboBox,
				ShowProgress, JGUIUtil.NONE, null, null ) ) {
				__ShowProgress_JComboBox.select ( ShowProgress );
			}
			else {
			    Message.printWarning ( 1, routine,
				"Existing command references an invalid ShowProgress value \"" +
				ShowProgress + "\".  Select a different value or Cancel.");
				__error_wait = true;
			}
		}
		// List.
        if ( IteratorValueProperty != null ) {
            __IteratorValueProperty_JTextField.setText( IteratorValueProperty );
        }
        if ( (List != null) && !List.isEmpty() ) {
            __List_JTextArea.setText( List );
            __main_JTabbedPane.setSelectedIndex(0);
        }
        // Sequence.
		if ( (SequenceStart != null) && !SequenceStart.isEmpty() ) {
		    __SequenceStart_JTextField.setText( SequenceStart );
		    __main_JTabbedPane.setSelectedIndex(1);
		}
		if ( SequenceEnd != null ) {
		    __SequenceEnd_JTextField.setText( SequenceEnd );
		}
		if ( SequenceIncrement != null ) {
		    __SequenceIncrement_JTextField.setText( SequenceIncrement );
		}
		// Table.
        if ( (TableID == null) || TableID.isEmpty() ) {
            // Select default.
            __TableID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TableID_JComboBox,TableID, JGUIUtil.NONE, null, null ) ) {
                __TableID_JComboBox.select ( TableID );
                __main_JTabbedPane.setSelectedIndex(2);
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTableID value \"" + TableID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( TableColumn != null ) {
            __TableColumn_JTextField.setText( TableColumn );
        }
        if ( TablePropertyMap != null ) {
            __TablePropertyMap_JTextArea.setText ( TablePropertyMap );
        }
        // Time period.
		if ( (PeriodStart != null) && !PeriodStart.isEmpty() ) {
		    __PeriodStart_JTextField.setText( PeriodStart );
		    __main_JTabbedPane.setSelectedIndex(3);
		}
		if ( (PeriodEnd != null) && !PeriodEnd.isEmpty() ) {
		    __PeriodEnd_JTextField.setText( PeriodEnd );
		}
        if ( (PeriodIncrement == null) || PeriodIncrement.isEmpty() ) {
            // Select default.
            __PeriodIncrement_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __PeriodIncrement_JComboBox,PeriodIncrement, JGUIUtil.NONE, null, null ) ) {
                __PeriodIncrement_JComboBox.select ( PeriodIncrement );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nPeriodIncrement value \"" + PeriodIncrement +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        // Time series.
        if ( (TSList == null) || TSList.isEmpty() ) {
            // Select default.
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
            // Also select tab.
		    __main_JTabbedPane.setSelectedIndex(4);
        }
        if ( JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID, JGUIUtil.NONE, null, null ) ) {
                __TSID_JComboBox.select ( TSID );
        }
        else {
            // Automatically add to the list after the blank.
            if ( (TSID != null) && (TSID.length() > 0) ) {
                __TSID_JComboBox.insertItemAt ( TSID, 1 );
                // Select...
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
        if ( TimeSeriesPropertyMap != null ) {
            __TimeSeriesPropertyMap_JTextArea.setText ( TimeSeriesPropertyMap );
        }
	}
	// Regardless, reset the command from the fields.
	// General.
	Name = __Name_JTextField.getText().trim();
    IteratorProperty = __IteratorProperty_JTextField.getText().trim();
    IndexProperty = __IndexProperty_JTextField.getText().trim();
    ShowProgress = __ShowProgress_JComboBox.getSelected();
    // List.
    IteratorValueProperty = __IteratorValueProperty_JTextField.getText().trim();
    List = __List_JTextArea.getText().trim().replace('\n', ' ').replace('\t', ' ');
    // Sequence.
    SequenceStart = __SequenceStart_JTextField.getText().trim();
    SequenceEnd = __SequenceEnd_JTextField.getText().trim();
    SequenceIncrement = __SequenceIncrement_JTextField.getText().trim();
    // Table.
    TableID = __TableID_JComboBox.getSelected();
    TableColumn = __TableColumn_JTextField.getText().trim();
	TablePropertyMap = __TablePropertyMap_JTextArea.getText().trim().replace("\n"," ");
	// Time period.
    PeriodStart = __PeriodStart_JTextField.getText().trim();
    PeriodEnd = __PeriodEnd_JTextField.getText().trim();
    PeriodIncrement = __PeriodIncrement_JComboBox.getSelected();
    // Time series.
	TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
	TimeSeriesPropertyMap = __TimeSeriesPropertyMap_JTextArea.getText().trim().replace("\n"," ");
    props = new PropList ( __command.getCommandName() );
    // General.
    props.add ( "Name=" + Name );
    props.add ( "IteratorProperty=" + IteratorProperty );
    props.add ( "IndexProperty=" + IndexProperty );
    props.add ( "ShowProgress=" + ShowProgress );
    // List.
    props.add ( "IteratorValueProperty=" + IteratorValueProperty );
    props.add ( "List=" + List );
    // Sequence.
    props.add ( "SequenceStart=" + SequenceStart );
    props.add ( "SequenceEnd=" + SequenceEnd );
    props.add ( "SequenceIncrement=" + SequenceIncrement );
    // Table.
    props.set ( "TableID", TableID ); // May contain = so handle differently.
    props.add ( "TableColumn=" + TableColumn );
    props.add ( "TablePropertyMap=" + TablePropertyMap );
    // Time period.
    props.add ( "PeriodStart=" + PeriodStart );
    props.add ( "PeriodEnd=" + PeriodEnd );
    props.add ( "PeriodIncrement=" + PeriodIncrement );
    // Time series.
	props.add ( "TSList=" + TSList );
    props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
    props.add ( "TimeSeriesPropertyMap=" + TimeSeriesPropertyMap );
    __command_JTextArea.setText( __command.toString(props).trim() );
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
*/
public void response ( boolean ok ) {
    __ok = ok;
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
public void windowClosing( WindowEvent event ) {
	response ( false );
}

public void windowActivated( WindowEvent evt ) {
}

public void windowClosed( WindowEvent evt ) {
}

public void windowDeactivated( WindowEvent evt ) {
}

public void windowDeiconified( WindowEvent evt ) {
}

public void windowIconified( WindowEvent evt ) {
}

public void windowOpened( WindowEvent evt ) {
}

}