// CheckTimeSeries_JDialog - editor for CheckTimeSeries command

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2023 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.check;

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

import javax.swing.JCheckBox;
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
import RTi.TS.TSUtil_CheckTimeSeries;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.DateTime_JPanel;
import RTi.Util.Time.TimeInterval;

@SuppressWarnings("serial")
public class CheckTimeSeries_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private JTabbedPane __main_JTabbedPane = null;
private CheckTimeSeries_Command __command = null;
private JTextArea __command_JTextArea=null;
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox	__TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private SimpleJComboBox __CheckCriteria_JComboBox = null;
private JTextField __Value1_JTextField = null;
private JTextField __Value2_JTextField = null;
private JTextField __ProblemType_JTextField = null;
private JTextField __MaxWarnings_JTextField = null;
private JTextField __Flag_JTextField = null;
private JTextField __FlagDesc_JTextField;
private SimpleJComboBox __Action_JComboBox = null;
private JTextField __AnalysisStart_JTextField = null;
private JTextField __AnalysisEnd_JTextField = null;
private JCheckBox __AnalysisWindow_JCheckBox = null;
private DateTime_JPanel __AnalysisWindowStart_JPanel = null; // Fields for analysis window within a year.
private DateTime_JPanel __AnalysisWindowEnd_JPanel = null;
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __TableTSIDColumn_JTextField = null;
private TSFormatSpecifiersJPanel __TableTSIDFormat_JTextField = null; // Format for time series identifiers, to match table TSID column.
private JTextField __TableDateTimeColumn_JTextField = null;
private JTextField __TableValueColumn_JTextField = null;
private JTextField __TableValuePrecision_JTextField = null;
private JTextField __TableFlagColumn_JTextField = null;
private JTextField __TableCheckTypeColumn_JTextField = null;
private JTextField __TableCheckMessageColumn_JTextField = null;
private JTextField __CheckCountProperty_JTextField = null;
private JTextField __CheckCountTimeSeriesProperty_JTextField = null;
private boolean __error_wait = false; // Is there an error to be cleared up or Cancel?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices choices for TableID value.
*/
public CheckTimeSeries_JDialog ( JFrame parent, CheckTimeSeries_Command command, List<String> tableIDChoices ) {
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
		HelpViewer.getInstance().showHelp("command", "CheckTimeSeries");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
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
public void changedUpdate ( DocumentEvent e ) {
    checkGUIState();
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void insertUpdate ( DocumentEvent e ) {
    checkGUIState();
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void removeUpdate ( DocumentEvent e ) {
    checkGUIState();
    refresh();
}

// ...End event handlers for DocumentListener

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

    if ( __AnalysisWindow_JCheckBox.isSelected() ) {
        // Checked so enable the date panels.
        __AnalysisWindowStart_JPanel.setEnabled ( true );
        __AnalysisWindowEnd_JPanel.setEnabled ( true );
    }
    else {
        __AnalysisWindowStart_JPanel.setEnabled ( false );
        __AnalysisWindowEnd_JPanel.setEnabled ( false );
    }
}

/**
Check the input.
If errors exist, warn the user and set the __error_wait flag to true.
This should be called before response() is allowed to complete.
*/
private void checkInput () {
	// Create a list of parameters to check.
	PropList parameters = new PropList ( "" );
    String TSList = __TSList_JComboBox.getSelected();
	String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String CheckCriteria = __CheckCriteria_JComboBox.getSelected();
	String Value1 = __Value1_JTextField.getText().trim();
	String Value2 = __Value2_JTextField.getText().trim();
	String ProblemType = __ProblemType_JTextField.getText().trim();
	String MaxWarnings = __MaxWarnings_JTextField.getText().trim();
    String Flag = __Flag_JTextField.getText().trim();
    String FlagDesc = __FlagDesc_JTextField.getText().trim();
    String Action = __Action_JComboBox.getSelected();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
    String TableID = __TableID_JComboBox.getSelected();
    String TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
    String TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
    String TableDateTimeColumn = __TableDateTimeColumn_JTextField.getText().trim();
    String TableValueColumn = __TableValueColumn_JTextField.getText().trim();
    String TableValuePrecision = __TableValuePrecision_JTextField.getText().trim();
    String TableFlagColumn = __TableFlagColumn_JTextField.getText().trim();
    String TableCheckTypeColumn = __TableCheckTypeColumn_JTextField.getText().trim();
    String TableCheckMessageColumn = __TableCheckMessageColumn_JTextField.getText().trim();
    String CheckCountProperty = __CheckCountProperty_JTextField.getText().trim();
    String CheckCountTimeSeriesProperty = __CheckCountTimeSeriesProperty_JTextField.getText().trim();
	__error_wait = false;

    if ( TSList.length() > 0 ) {
        parameters.set ( "TSList", TSList );
    }
	if ( TSID.length() > 0 ) {
		parameters.set ( "TSID", TSID );
	}
    if ( EnsembleID.length() > 0 ) {
        parameters.set ( "EnsembleID", EnsembleID );
    }
    if ( CheckCriteria.length() > 0 ) {
        parameters.set ( "CheckCriteria", CheckCriteria );
    }
	if ( Value1.length() > 0 ) {
		parameters.set ( "Value1", Value1 );
	}
    if ( Value2.length() > 0 ) {
        parameters.set ( "Value2", Value2 );
    }
	if ( ProblemType.length() > 0 ) {
		parameters.set ( "ProblemType", ProblemType );
	}
    if ( MaxWarnings.length() > 0 ) {
        parameters.set ( "MaxWarnings", MaxWarnings );
    }
    if ( Flag.length() > 0 ) {
        parameters.set ( "Flag", Flag );
    }
    if ( FlagDesc.length() > 0 ) {
        parameters.set ( "FlagDesc", FlagDesc );
    }
    if ( Action.length() > 0 ) {
        parameters.set ( "Action", Action );
    }
	if ( AnalysisStart.length() > 0 ) {
		parameters.set ( "AnalysisStart", AnalysisStart );
	}
	if ( AnalysisEnd.length() > 0 ) {
		parameters.set ( "AnalysisEnd", AnalysisEnd );
	}
    if ( __AnalysisWindow_JCheckBox.isSelected() ){
        String AnalysisWindowStart = __AnalysisWindowStart_JPanel.toString(false,true).trim();
        String AnalysisWindowEnd = __AnalysisWindowEnd_JPanel.toString(false,true).trim();
        if ( AnalysisWindowStart.length() > 0 ) {
            parameters.set ( "AnalysisWindowStart", AnalysisWindowStart );
        }
        if ( AnalysisWindowEnd.length() > 0 ) {
            parameters.set ( "AnalysisWindowEnd", AnalysisWindowEnd );
        }
    }
    if ( TableID.length() > 0 ) {
    	parameters.set ( "TableID", TableID );
    }
    if ( TableTSIDColumn.length() > 0 ) {
    	parameters.set ( "TableTSIDColumn", TableTSIDColumn );
    }
    if ( TableTSIDFormat.length() > 0 ) {
    	parameters.set ( "TableTSIDFormat", TableTSIDFormat );
    }
    if ( TableDateTimeColumn.length() > 0 ) {
    	parameters.set ( "TableDateTimeColumn", TableDateTimeColumn );
    }
    if ( TableValueColumn.length() > 0 ) {
    	parameters.set ( "TableValueColumn", TableValueColumn );
    }
    if ( !TableValuePrecision.isEmpty() ) {
    	parameters.set ( "TableValuePrecision", TableValuePrecision );
    }
    if ( TableFlagColumn.length() > 0 ) {
    	parameters.set ( "TableFlagColumn", TableFlagColumn );
    }
    if ( TableCheckTypeColumn.length() > 0 ) {
    	parameters.set ( "TableCheckTypeColumn", TableCheckTypeColumn );
    }
    if ( TableCheckMessageColumn.length() > 0 ) {
    	parameters.set ( "TableCheckMessageColumn", TableCheckMessageColumn );
    }
    if ( CheckCountProperty.length() > 0 ) {
        parameters.set ( "CheckCountProperty", CheckCountProperty );
    }
    if ( CheckCountTimeSeriesProperty.length() > 0 ) {
        parameters.set ( "CheckCountTimeSeriesProperty", CheckCountTimeSeriesProperty );
    }
	try {
	    // This will warn the user.
		__command.checkCommandParameters ( parameters, null, 1 );
	}
	catch ( Exception e ) {
		// The warning would have been printed in the check code.
		__error_wait = true;
	}
}

/**
Commit the edits to the command.
In this case the command parameters have already been checked and no errors were detected.
*/
private void commitEdits () {
	String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String CheckCriteria = __CheckCriteria_JComboBox.getSelected();
	String Value1 = __Value1_JTextField.getText().trim();
	String Value2 = __Value2_JTextField.getText().trim();
	String ProblemType = __ProblemType_JTextField.getText().trim();
	String MaxWarnings = __MaxWarnings_JTextField.getText().trim();
	String Flag = __Flag_JTextField.getText().trim();
	String FlagDesc = __FlagDesc_JTextField.getText().trim();
    String Action = __Action_JComboBox.getSelected();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
    String TableID = __TableID_JComboBox.getSelected();
    String TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
    String TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
    String TableDateTimeColumn = __TableDateTimeColumn_JTextField.getText().trim();
    String TableValueColumn = __TableValueColumn_JTextField.getText().trim();
    String TableValuePrecision = __TableValuePrecision_JTextField.getText().trim();
    String TableFlagColumn = __TableFlagColumn_JTextField.getText().trim();
    String TableCheckTypeColumn = __TableCheckTypeColumn_JTextField.getText().trim();
    String TableCheckMessageColumn = __TableCheckMessageColumn_JTextField.getText().trim();
    String CheckCountProperty = __CheckCountProperty_JTextField.getText().trim();
    String CheckCountTimeSeriesProperty = __CheckCountTimeSeriesProperty_JTextField.getText().trim();
    __command.setCommandParameter ( "TSList", TSList );
	__command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "CheckCriteria", CheckCriteria );
	__command.setCommandParameter ( "Value1", Value1 );
	__command.setCommandParameter ( "Value2", Value2 );
	__command.setCommandParameter ( "ProblemType", ProblemType );
	__command.setCommandParameter ( "MaxWarnings", MaxWarnings );
	__command.setCommandParameter ( "Flag", Flag );
	__command.setCommandParameter ( "FlagDesc", FlagDesc );
	__command.setCommandParameter ( "Action", Action );
	__command.setCommandParameter ( "AnalysisStart", AnalysisStart );
	__command.setCommandParameter ( "AnalysisEnd", AnalysisEnd );
    if ( __AnalysisWindow_JCheckBox.isSelected() ){
        String AnalysisWindowStart = __AnalysisWindowStart_JPanel.toString(false,true).trim();
        String AnalysisWindowEnd = __AnalysisWindowEnd_JPanel.toString(false,true).trim();
        __command.setCommandParameter ( "AnalysisWindowStart", AnalysisWindowStart );
        __command.setCommandParameter ( "AnalysisWindowEnd", AnalysisWindowEnd );
    }
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "TableTSIDColumn", TableTSIDColumn );
    __command.setCommandParameter ( "TableTSIDFormat", TableTSIDFormat );
    __command.setCommandParameter ( "TableDateTimeColumn", TableDateTimeColumn );
    __command.setCommandParameter ( "TableValueColumn", TableValueColumn );
    __command.setCommandParameter ( "TableValuePrecision", TableValuePrecision );
    __command.setCommandParameter ( "TableFlagColumn", TableFlagColumn );
    __command.setCommandParameter ( "TableCheckTypeColumn", TableCheckTypeColumn );
    __command.setCommandParameter ( "TableCheckMessageColumn", TableCheckMessageColumn );
	__command.setCommandParameter ( "CheckCountProperty", CheckCountProperty );
	__command.setCommandParameter ( "CheckCountTimeSeriesProperty", CheckCountTimeSeriesProperty );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command The command to edit.
@param tableIDChoices list of choices for table identifiers
*/
private void initialize ( JFrame parent, CheckTimeSeries_Command command, List<String> tableIDChoices ) {
	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Check time series data values for critical values (see also the CheckTimeSeriesStatistic() command)." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Check results can be saved to a table for output for further processing." ),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Or, use the WriteCheckFile() command to save the results of all checks from command status messages." ),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for time series.
    int yts = -1;
    JPanel ts_JPanel = new JPanel();
    ts_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Time Series", ts_JPanel );

    JGUIUtil.addComponent(ts_JPanel, new JLabel (
		"Indicate the time series to check."),
		0, ++yts, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++yts, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    __TSList_JComboBox = new SimpleJComboBox(false);
    yts = CommandEditorUtil.addTSListToEditorDialogPanel ( this, ts_JPanel, __TSList_JComboBox, yts );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true ); // Allow edits.
    __TSID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yts = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, ts_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, yts );

    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits.
    __EnsembleID_JComboBox.setToolTipText("Select an ensemble identifier from the list or specify with ${Property} notation");
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yts = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
        this, this, ts_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, yts );

    // Panel for check criteria.
    int yCheck = -1;
    JPanel check_JPanel = new JPanel();
    check_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Check Criteria and Actions", check_JPanel );

    JGUIUtil.addComponent(check_JPanel, new JLabel (
		"Specify criteria to check for and actions to be taken when check criteria are met."),
		0, ++yCheck, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel (
        "A warning will be generated for each case where a value matches the specified condition(s)." ),
        0, ++yCheck, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel (
		"Specify dates with precision appropriate for the data, " +
		"use blank for all available data, or ${Property} for processor property."),
		0, ++yCheck, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++yCheck, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(check_JPanel, new JLabel ( "Check criteria:" ),
        0, ++yCheck, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CheckCriteria_JComboBox = new SimpleJComboBox ( 12, false );    // Do not allow edit.
    List<String> checkCriteriaChoices = TSUtil_CheckTimeSeries.getCheckCriteriaChoicesAsStrings();
    __CheckCriteria_JComboBox.setData ( checkCriteriaChoices );
    __CheckCriteria_JComboBox.addItemListener ( this );
    __CheckCriteria_JComboBox.setMaximumRowCount(checkCriteriaChoices.size());
    JGUIUtil.addComponent(check_JPanel, __CheckCriteria_JComboBox,
        1, yCheck, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel("Required - may require other parameters."),
        3, yCheck, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(check_JPanel, new JLabel ( "Value1:" ),
		0, ++yCheck, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Value1_JTextField = new JTextField ( 10 );
	__Value1_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(check_JPanel, __Value1_JTextField,
		1, yCheck, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel(
		"Optional - minimum (or only) value to check."),
		3, yCheck, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(check_JPanel, new JLabel ( "Value2:" ),
        0, ++yCheck, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Value2_JTextField = new JTextField ( 10 );
    __Value2_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(check_JPanel, __Value2_JTextField,
        1, yCheck, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel(
        "Optional - maximum value in range, or other input to check."),
        3, yCheck, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(check_JPanel, new JLabel ( "Problem type:" ),
		0, ++yCheck, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ProblemType_JTextField = new JTextField ( 10 );
	__ProblemType_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(check_JPanel, __ProblemType_JTextField,
		1, yCheck, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel(
		"Optional - problem type to use in output (default=check criteria)."),
		3, yCheck, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(check_JPanel, new JLabel ( "Maximum warnings:" ),
        0, ++yCheck, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MaxWarnings_JTextField = new JTextField ( 10 );
    __MaxWarnings_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(check_JPanel, __MaxWarnings_JTextField,
        1, yCheck, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel(
        "Optional - maximum # of warnings/time series (default=no limit)."),
        3, yCheck, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(check_JPanel,new JLabel( "Flag:"),
        0, ++yCheck, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Flag_JTextField = new JTextField ( "", 10 );
    __Flag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(check_JPanel, __Flag_JTextField,
        1, yCheck, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel ( "Optional - flag to mark detected values."),
        3, yCheck, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(check_JPanel, new JLabel ( "Flag description:" ),
        0, ++yCheck, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __FlagDesc_JTextField = new JTextField ( 15 );
    __FlagDesc_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(check_JPanel, __FlagDesc_JTextField,
        1, yCheck, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel( "Optional - description for flag."),
        3, yCheck, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(check_JPanel, new JLabel ( "Action:" ),
        0, ++yCheck, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Action_JComboBox = new SimpleJComboBox ( 12, false );    // Do not allow edit.
    List<String> actionChoices = new ArrayList<>();
    actionChoices.add("");
    actionChoices.add(__command._Remove);
    actionChoices.add(__command._SetMissing);
    __Action_JComboBox.setData ( actionChoices );
    __Action_JComboBox.select(0);
    __Action_JComboBox.addItemListener ( this );
    __Action_JComboBox.setMaximumRowCount(actionChoices.size());
    JGUIUtil.addComponent(check_JPanel, __Action_JComboBox,
        1, yCheck, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel("Optional - action for matched values (default=no action)."),
        3, yCheck, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for analysis period and window table.
    int yTime = -1;
    JPanel time_JPanel = new JPanel();
    time_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Analysis Period and Window", time_JPanel );

    JGUIUtil.addComponent(time_JPanel, new JLabel (
		"Use the following parameters to constrain checks to a period and window within each year."),
		0, ++yTime, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(time_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++yTime, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(time_JPanel, new JLabel ( "Analysis start:" ),
        0, ++yTime, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AnalysisStart_JTextField = new JTextField ( "", 20 );
    __AnalysisStart_JTextField.setToolTipText("Specify the analysis start using a date/time string or ${Property} notation");
    __AnalysisStart_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(time_JPanel, __AnalysisStart_JTextField,
        1, yTime, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(time_JPanel, new JLabel(
        "Optional - analysis start date/time (default=full time series period)."),
        3, yTime, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(time_JPanel, new JLabel ( "Analysis end:" ),
        0, ++yTime, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AnalysisEnd_JTextField = new JTextField ( "", 20 );
    __AnalysisEnd_JTextField.setToolTipText("Specify the analysis end using a date/time string or ${Property} notation");
    __AnalysisEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(time_JPanel, __AnalysisEnd_JTextField,
        1, yTime, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(time_JPanel, new JLabel(
        "Optional - analysis end date/time (default=full time series period)."),
        3, yTime, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __AnalysisWindow_JCheckBox = new JCheckBox ( "Analysis window:", false );
    __AnalysisWindow_JCheckBox.addActionListener ( this );
    JGUIUtil.addComponent(time_JPanel, __AnalysisWindow_JCheckBox,
        0, ++yTime, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    JPanel analysisWindow_JPanel = new JPanel();
    analysisWindow_JPanel.setLayout(new GridBagLayout());
    __AnalysisWindowStart_JPanel = new DateTime_JPanel ( "Start", TimeInterval.MONTH, TimeInterval.HOUR, null );
    __AnalysisWindowStart_JPanel.addActionListener(this);
    __AnalysisWindowStart_JPanel.addKeyListener ( this );
    JGUIUtil.addComponent(analysisWindow_JPanel, __AnalysisWindowStart_JPanel,
        1, 0, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    // TODO SAM 2008-01-23 Figure out how to display the correct limits given the time series interval.
    __AnalysisWindowEnd_JPanel = new DateTime_JPanel ( "End", TimeInterval.MONTH, TimeInterval.HOUR, null );
    __AnalysisWindowEnd_JPanel.addActionListener(this);
    __AnalysisWindowEnd_JPanel.addKeyListener ( this );
    JGUIUtil.addComponent(analysisWindow_JPanel, __AnalysisWindowEnd_JPanel,
        4, 0, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(time_JPanel, analysisWindow_JPanel,
        1, yTime, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(time_JPanel, new JLabel(
        "Optional - analysis window within input year (default=full year)."),
        3, yTime, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(time_JPanel, new JLabel(""),
        3, yTime, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for output table.
    int yTable = -1;
    JPanel table_JPanel = new JPanel();
    table_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output Table", table_JPanel );

    JGUIUtil.addComponent(table_JPanel, new JLabel (
		"Check warnings can be written to an output table."),
		0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel (
		"The MaxWarnings parameter in the \"Check Criteria and Actions\" tab limits the output rows per time series."),
		0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table ID:" ),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit.
    __TableID_JComboBox.setToolTipText("Specify the table ID for statistic output or use ${Property} notation");
    tableIDChoices.add(0,""); // Add blank to ignore table.
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    __TableID_JComboBox.getJTextComponent().addKeyListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(table_JPanel, __TableID_JComboBox,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel( "Optional - table to receive output."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table TSID column:" ),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableTSIDColumn_JTextField = new JTextField ( 10 );
    __TableTSIDColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __TableTSIDColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel( "Required for table - column name to match time series TSID."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(table_JPanel, new JLabel("Format of TSID:"),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableTSIDFormat_JTextField = new TSFormatSpecifiersJPanel(10);
    __TableTSIDFormat_JTextField.setToolTipText("Use %L for location, %T for data type, ${ts:property} to match time series property.");
    __TableTSIDFormat_JTextField.addKeyListener ( this );
    __TableTSIDFormat_JTextField.getDocument().addDocumentListener(this);
    __TableTSIDFormat_JTextField.getTextField().setToolTipText("%L for location, %T for data type, ${ts:property} to match time series property.");
    JGUIUtil.addComponent(table_JPanel, __TableTSIDFormat_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Required for table - format time series TSID to match table TSID."),
        3, yTable, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table date/time column:" ),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableDateTimeColumn_JTextField = new JTextField ( 10 );
    __TableDateTimeColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __TableDateTimeColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel( "Optional - column name for date/time (default=not output)."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table value column:" ),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableValueColumn_JTextField = new JTextField ( 10 );
    __TableValueColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __TableValueColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel( "Optional - column name for time series value (default=not output)."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table value precision:" ),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableValuePrecision_JTextField = new JTextField ( 10 );
    __TableValuePrecision_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __TableValuePrecision_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel( "Optional - precision for value (default=4 digits)."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table flag column:" ),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableFlagColumn_JTextField = new JTextField ( 10 );
    __TableFlagColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __TableFlagColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel( "Optional - column name for time series flag (default=not output)."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table check type column:" ),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableCheckTypeColumn_JTextField = new JTextField ( 10 );
    __TableCheckTypeColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __TableCheckTypeColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel( "Optional - column name for check type (default=not output)."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table check message column:" ),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableCheckMessageColumn_JTextField = new JTextField ( 10 );
    __TableCheckMessageColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __TableCheckMessageColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel( "Optional - column name for check message (default=not output)."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for output properties.
    int yProp = -1;
    JPanel prop_JPanel = new JPanel();
    prop_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output Properties", prop_JPanel );

    JGUIUtil.addComponent(prop_JPanel, new JLabel (
		"Specify a time series and/or processor property to set to the count of values detected that meet the check criteria."),
		0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel (
		"The processor property is visible globally whereas the time series property is associated with the specific time series."),
		0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(prop_JPanel, new JLabel ( "Check count processor property:" ),
        0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CheckCountProperty_JTextField = new JTextField ( 20 );
    __CheckCountProperty_JTextField.setToolTipText("Specify processor property to set or use ${Property}, ${ts:Property}, %-specifier notation");
    __CheckCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(prop_JPanel, __CheckCountProperty_JTextField,
        1, yProp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel(
        "Optional - name of processor property for check count."),
        3, yProp, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(prop_JPanel, new JLabel ( "Check count time series property:" ),
        0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CheckCountTimeSeriesProperty_JTextField = new JTextField ( 20 );
    __CheckCountTimeSeriesProperty_JTextField.setToolTipText("Specify time series property to set or use ${Property}, ${ts:Property}, %-specifier notation");
    __CheckCountTimeSeriesProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(prop_JPanel, __CheckCountTimeSeriesProperty_JTextField,
        1, yProp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel(
        "Optional - name of time series property for check count."),
        3, yProp, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 55 );
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents.
    checkGUIState();
	refresh ();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel,
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

    button_JPanel.add ( __ok_JButton = new SimpleJButton("OK", this) );
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add(__cancel_JButton = new SimpleJButton("Cancel", this));
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
public void itemStateChanged ( ItemEvent e ) {
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
			response ( true );
		}
	}
	else {
	    // Combo box.
		refresh();
	}
}

public void keyReleased ( KeyEvent event ) {
	refresh();
}

public void keyTyped ( KeyEvent event ) {
}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user canceled.
*/
public boolean ok () {
	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh () {
	String routine = getClass().getSimpleName() + ".refresh";
    String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String CheckCriteria = "";
	String Value1 = "";
	String Value2 = "";
	String ProblemType = "";
	String MaxWarnings = "";
	String Flag = "";
	String FlagDesc = "";
	String Action = "";
	String AnalysisStart = "";
	String AnalysisEnd = "";
    String AnalysisWindowStart = "";
    String AnalysisWindowEnd = "";
	String TableID = "";
	String TableTSIDColumn = "";
	String TableTSIDFormat = "";
	String TableDateTimeColumn = "";
	String TableValueColumn = "";
	String TableValuePrecision = "";
	String TableFlagColumn = "";
	String TableCheckTypeColumn = "";
	String TableCheckMessageColumn = "";
	String CheckCountProperty = "";
	String CheckCountTimeSeriesProperty = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command.
        TSList = props.getValue ( "TSList" );
		TSID = props.getValue ( "TSID" );
        EnsembleID = props.getValue ( "EnsembleID" );
        CheckCriteria = props.getValue ( "CheckCriteria" );
		Value1 = props.getValue ( "Value1" );
		Value2 = props.getValue ( "Value2" );
		ProblemType = props.getValue ( "ProblemType" );
		MaxWarnings = props.getValue ( "MaxWarnings" );
		Flag = props.getValue ( "Flag" );
		FlagDesc = props.getValue ( "FlagDesc" );
		Action = props.getValue ( "Action" );
		AnalysisStart = props.getValue ( "AnalysisStart" );
		AnalysisEnd = props.getValue ( "AnalysisEnd" );
        AnalysisWindowStart = props.getValue ( "AnalysisWindowStart" );
        AnalysisWindowEnd = props.getValue ( "AnalysisWindowEnd" );
        TableID = props.getValue ( "TableID" );
        TableTSIDColumn = props.getValue ( "TableTSIDColumn" );
        TableTSIDFormat = props.getValue ( "TableTSIDFormat" );
        TableDateTimeColumn = props.getValue ( "TableDateTimeColumn" );
        TableValueColumn = props.getValue ( "TableValueColumn" );
        TableValuePrecision = props.getValue ( "TableValuePrecision" );
        TableFlagColumn = props.getValue ( "TableFlagColumn" );
        TableCheckTypeColumn = props.getValue ( "TableCheckTypeColumn" );
        TableCheckMessageColumn = props.getValue ( "TableCheckMessageColumn" );
		CheckCountProperty = props.getValue ( "CheckCountProperty" );
		CheckCountTimeSeriesProperty = props.getValue ( "CheckCountTimeSeriesProperty" );
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
        if ( CheckCriteria == null ) {
            // Select default.
            __CheckCriteria_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __CheckCriteria_JComboBox,CheckCriteria, JGUIUtil.NONE, null, null ) ) {
                __CheckCriteria_JComboBox.select ( CheckCriteria );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nCheckType value \"" + CheckCriteria +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
		if ( Value1 != null ) {
			__Value1_JTextField.setText ( Value1 );
		}
        if ( Value2 != null ) {
            __Value2_JTextField.setText ( Value2 );
        }
		if ( ProblemType != null ) {
			__ProblemType_JTextField.setText ( ProblemType );
		}
        if ( MaxWarnings != null ) {
            __MaxWarnings_JTextField.setText ( MaxWarnings );
        }
        if ( Flag != null ) {
            __Flag_JTextField.setText ( Flag );
        }
        if ( FlagDesc != null ) {
            __FlagDesc_JTextField.setText ( FlagDesc );
        }
        if ( Action == null ) {
            // Select default.
            __Action_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __Action_JComboBox,Action, JGUIUtil.NONE, null, null ) ) {
                __Action_JComboBox.select ( Action );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nAction value \"" + Action +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
		if ( AnalysisStart != null ) {
			__AnalysisStart_JTextField.setText( AnalysisStart );
		}
		if ( AnalysisEnd != null ) {
			__AnalysisEnd_JTextField.setText ( AnalysisEnd );
		}
        if ( (AnalysisWindowStart != null) && (AnalysisWindowStart.length() > 0) ) {
            try {
                // Add year because it is not part of the parameter value.
                DateTime AnalysisWindowStart_DateTime = DateTime.parse ( "0000-" + AnalysisWindowStart );
                Message.printStatus(2, routine, "Setting window start to " + AnalysisWindowStart_DateTime );
                __AnalysisWindowStart_JPanel.setDateTime ( AnalysisWindowStart_DateTime );
            }
            catch ( Exception e ) {
                Message.printWarning( 1, routine, "AnalysisWindowStart (" + AnalysisWindowStart +
                        ") is not a valid date/time." );
            }
        }
        if ( (AnalysisWindowEnd != null) && (AnalysisWindowEnd.length() > 0) ) {
            try {
                // Add year because it is not part of the parameter value.
                DateTime AnalysisWindowEnd_DateTime = DateTime.parse ( "0000-" + AnalysisWindowEnd );
                Message.printStatus(2, routine, "Setting window end to " + AnalysisWindowEnd_DateTime );
                __AnalysisWindowEnd_JPanel.setDateTime ( AnalysisWindowEnd_DateTime );
            }
            catch ( Exception e ) {
                Message.printWarning( 1, routine, "AnalysisWindowEnd (" + AnalysisWindowEnd +
                        ") is not a valid date/time." );
            }
        }
        if ( (AnalysisWindowStart != null) && (AnalysisWindowStart.length() != 0) &&
            (AnalysisWindowEnd != null) && (AnalysisWindowEnd.length() != 0)) {
            __AnalysisWindow_JCheckBox.setSelected ( true );
        }
        else {
            __AnalysisWindow_JCheckBox.setSelected ( false );
        }
        if ( TableID == null ) {
            // Select default.
            __TableID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TableID_JComboBox,TableID, JGUIUtil.NONE, null, null ) ) {
                __TableID_JComboBox.select ( TableID );
            }
            else {
            	// OK to add to list since does not need to exist.
            	__TableID_JComboBox.add(TableID);
            	__TableID_JComboBox.select(TableID);
                //Message.printWarning ( 1, routine,
                //"Existing command references an invalid\nTableID value \"" + TableID +
                //"\".  Select a different value or Cancel.");
                //__error_wait = true;
            }
        }
        if ( TableTSIDColumn != null ) {
            __TableTSIDColumn_JTextField.setText ( TableTSIDColumn );
        }
        if (TableTSIDFormat != null ) {
            __TableTSIDFormat_JTextField.setText(TableTSIDFormat.trim());
        }
        if ( TableDateTimeColumn != null ) {
            __TableDateTimeColumn_JTextField.setText ( TableDateTimeColumn );
        }
        if ( TableValueColumn != null ) {
            __TableValueColumn_JTextField.setText ( TableValueColumn );
        }
        if ( TableValuePrecision != null ) {
            __TableValuePrecision_JTextField.setText ( TableValuePrecision );
        }
        if ( TableFlagColumn != null ) {
            __TableFlagColumn_JTextField.setText ( TableFlagColumn );
        }
        if ( TableCheckTypeColumn != null ) {
            __TableCheckTypeColumn_JTextField.setText ( TableCheckTypeColumn );
        }
        if ( TableCheckMessageColumn != null ) {
            __TableCheckMessageColumn_JTextField.setText ( TableCheckMessageColumn );
        }
        if ( CheckCountProperty != null ) {
            __CheckCountProperty_JTextField.setText ( CheckCountProperty );
        }
        if ( CheckCountTimeSeriesProperty != null ) {
            __CheckCountTimeSeriesProperty_JTextField.setText ( CheckCountTimeSeriesProperty );
        }
	}
	// Regardless, reset the command from the fields.
	checkGUIState();
    TSList = __TSList_JComboBox.getSelected();
	TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    CheckCriteria = __CheckCriteria_JComboBox.getSelected();
    Value2 = __Value2_JTextField.getText().trim();
	Value1 = __Value1_JTextField.getText().trim();
	ProblemType = __ProblemType_JTextField.getText().trim();
	MaxWarnings = __MaxWarnings_JTextField.getText().trim();
	Flag = __Flag_JTextField.getText().trim();
	FlagDesc = __FlagDesc_JTextField.getText().trim();
	Action = __Action_JComboBox.getSelected();
	AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
    TableID = __TableID_JComboBox.getSelected();
    TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
    TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
    TableDateTimeColumn = __TableDateTimeColumn_JTextField.getText().trim();
    TableValueColumn = __TableValueColumn_JTextField.getText().trim();
    TableValuePrecision = __TableValuePrecision_JTextField.getText().trim();
    TableFlagColumn = __TableFlagColumn_JTextField.getText().trim();
    TableCheckTypeColumn = __TableCheckTypeColumn_JTextField.getText().trim();
    TableCheckMessageColumn = __TableCheckMessageColumn_JTextField.getText().trim();
	CheckCountProperty = __CheckCountProperty_JTextField.getText().trim();
	CheckCountTimeSeriesProperty = __CheckCountTimeSeriesProperty_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
    props.add ( "TSList=" + TSList );
	props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
    // Use set method so equal sign in criteria does not cause a problem.
    props.set ( "CheckCriteria", CheckCriteria );
    props.add ( "Value1=" + Value1 );
    props.add ( "Value2=" + Value2 );
	props.add ( "ProblemType=" + ProblemType );
	props.add ( "MaxWarnings=" + MaxWarnings );
	props.add ( "Flag=" + Flag );
	props.add ( "FlagDesc=" + FlagDesc );
	props.add ( "Action=" + Action );
	props.add ( "AnalysisStart=" + AnalysisStart );
	props.add ( "AnalysisEnd=" + AnalysisEnd );
    if ( __AnalysisWindow_JCheckBox.isSelected() ) {
        AnalysisWindowStart = __AnalysisWindowStart_JPanel.toString(false,true).trim();
        AnalysisWindowEnd = __AnalysisWindowEnd_JPanel.toString(false,true).trim();
        props.add ( "AnalysisWindowStart=" + AnalysisWindowStart );
        props.add ( "AnalysisWindowEnd=" + AnalysisWindowEnd );
    }
    props.add ( "TableID=" + TableID );
    props.add ( "TableTSIDColumn=" + TableTSIDColumn );
    props.add ( "TableTSIDFormat=" + TableTSIDFormat );
    props.add ( "TableDateTimeColumn=" + TableDateTimeColumn );
    props.add ( "TableValueColumn=" + TableValueColumn );
    props.add ( "TableValuePrecision=" + TableValuePrecision );
    props.add ( "TableFlagColumn=" + TableFlagColumn );
    props.add ( "TableCheckTypeColumn=" + TableCheckTypeColumn );
    props.add ( "TableCheckMessageColumn=" + TableCheckMessageColumn );
	props.add ( "CheckCountProperty=" + CheckCountProperty );
	props.add ( "CheckCountTimeSeriesProperty=" + CheckCountTimeSeriesProperty );
	__command_JTextArea.setText( __command.toString ( props ).trim() );
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
*/
private void response ( boolean ok ) {
	__ok = ok;	// Save to be returned by ok().
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
