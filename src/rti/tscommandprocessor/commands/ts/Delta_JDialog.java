// Delta_JDialog - editor dialog for Delta command

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
import RTi.TS.ResetType;
import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.TS.TrendType;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

@SuppressWarnings("serial")
public class Delta_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private Delta_Command __command = null;
private JTabbedPane __main_JTabbedPane = null;
private JTextArea __command_JTextArea=null;
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox	__TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;

// General
private SimpleJComboBox __EnsembleID_JComboBox = null;
private SimpleJComboBox __ExpectedTrend_JComboBox = null;
private SimpleJComboBox __ResetType_JComboBox = null;
private JTextField __AnalysisStart_JTextField = null;
private JTextField __AnalysisEnd_JTextField = null;
private JTextField __Flag_JTextField = null; // Flag to label filled data.
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private SimpleJComboBox __CopyDataFlags_JComboBox = null;

// Delta Limit.
private JTextField __DeltaLimit_JTextField = null;
private SimpleJComboBox __DeltaLimitAction_JComboBox = null;
private JTextField __DeltaLimitFlag_JTextField = null;
private JTextField __IntervalLimit_JTextField = null;
private SimpleJComboBox __IntervalLimitAction_JComboBox = null;
private JTextField __IntervalLimitFlag_JTextField = null;

// Reset - Auto
private JTextField __AutoResetDatum_JTextField = null;
private JTextField __AutoResetFlag_JTextField = null;

// Reset - Rollover
private JTextField __ResetMin_JTextField = null;
private JTextField __ResetMax_JTextField = null;
private JTextField __RolloverDeltaLimit_JTextField = null;
private JTextField __RolloverFlag_JTextField = null;
private JTextField __ManualResetFlag_JTextField = null;
private JTextField __ResetProximityLimit_JTextField = null;
private JTextField __ResetProximityLimitInterval_JTextField = null;
private JTextField __ResetProximityLimitFlag_JTextField = null;

// Output Table.
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __TableTSIDColumn_JTextField = null;
// Format for time series identifiers, to match table TSID column.
private TSFormatSpecifiersJPanel __TableTSIDFormat_JTextField = null;
private JTextField __TableDateTimeColumn_JTextField = null;
private JTextField __TableValuePreviousColumn_JTextField = null;
private JTextField __TableValueColumn_JTextField = null;
private JTextField __TableDeltaCalculatedColumn_JTextField = null;
private JTextField __TableActionColumn_JTextField = null;
private JTextField __TableDeltaColumn_JTextField = null;
private JTextField __TableFlagColumn_JTextField = null;
private JTextField __TableProblemColumn_JTextField = null;

// Output Properties.
private JTextField __ProblemCountProperty_JTextField = null;
private JTextField __ProblemCountTimeSeriesProperty_JTextField = null;

private boolean __error_wait = false; // Is there an error to be cleared up or Cancel?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices choices for TableID value.
*/
public Delta_JDialog ( JFrame parent, Delta_Command command, List<String> tableIDChoices ) {
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
		HelpViewer.getInstance().showHelp("command", "Delta");
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
}

/**
Check the input. If errors exist, warn the user and set the __error_wait flag to true.
This should be called before response() is allowed to complete.
*/
private void checkInput () {
	// Put together a list of parameters to check.
	PropList parameters = new PropList ( "" );
    String TSList = __TSList_JComboBox.getSelected();
	String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    // General.
    String ResetType = __ResetType_JComboBox.getSelected();
    String ExpectedTrend = __ExpectedTrend_JComboBox.getSelected();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
    String Flag = __Flag_JTextField.getText().trim();
	String Alias = __Alias_JTextField.getText().trim();
    String CopyDataFlags = __CopyDataFlags_JComboBox.getSelected();
	// Delta Limit.
	String DeltaLimit = __DeltaLimit_JTextField.getText().trim();
    String DeltaLimitAction = __DeltaLimitAction_JComboBox.getSelected();
	String DeltaLimitFlag = __DeltaLimitFlag_JTextField.getText().trim();
	String IntervalLimit = __IntervalLimit_JTextField.getText().trim();
    String IntervalLimitAction = __IntervalLimitAction_JComboBox.getSelected();
	String IntervalLimitFlag = __IntervalLimitFlag_JTextField.getText().trim();
	// Reset=Auto.
	String AutoResetDatum = __AutoResetDatum_JTextField.getText().trim();
	String AutoResetFlag = __AutoResetFlag_JTextField.getText().trim();
	// Reset=Rollover.
	String ResetMin = __ResetMin_JTextField.getText().trim();
	String ResetMax = __ResetMax_JTextField.getText().trim();
	String RolloverDeltaLimit = __RolloverDeltaLimit_JTextField.getText().trim();
	String RolloverFlag = __RolloverFlag_JTextField.getText().trim();
	String ManualResetFlag = __ManualResetFlag_JTextField.getText().trim();
	String ResetProximityLimit = __ResetProximityLimit_JTextField.getText().trim();
	String ResetProximityLimitInterval = __ResetProximityLimitInterval_JTextField.getText().trim();
	String ResetProximityLimitFlag = __ResetProximityLimitFlag_JTextField.getText().trim();
	// Output table.
    String TableID = __TableID_JComboBox.getSelected();
    String TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
    String TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
    String TableDateTimeColumn = __TableDateTimeColumn_JTextField.getText().trim();
    String TableValuePreviousColumn = __TableValuePreviousColumn_JTextField.getText().trim();
    String TableValueColumn = __TableValueColumn_JTextField.getText().trim();
    String TableDeltaCalculatedColumn = __TableDeltaCalculatedColumn_JTextField.getText().trim();
    String TableActionColumn = __TableActionColumn_JTextField.getText().trim();
    String TableDeltaColumn = __TableDeltaColumn_JTextField.getText().trim();
    String TableFlagColumn = __TableFlagColumn_JTextField.getText().trim();
    String TableProblemColumn = __TableProblemColumn_JTextField.getText().trim();
    // Output properties.
    String ProblemCountProperty = __ProblemCountProperty_JTextField.getText().trim();
    String ProblemCountTimeSeriesProperty = __ProblemCountTimeSeriesProperty_JTextField.getText().trim();
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
    // General.
    if ( ResetType.length() > 0 ) {
        parameters.set ( "ResetType", ResetType );
    }
    if ( ExpectedTrend.length() > 0 ) {
        parameters.set ( "ExpectedTrend", ExpectedTrend );
    }
	if ( AnalysisStart.length() > 0 ) {
		parameters.set ( "AnalysisStart", AnalysisStart );
	}
	if ( AnalysisEnd.length() > 0 ) {
		parameters.set ( "AnalysisEnd", AnalysisEnd );
	}
    if ( Flag.length() > 0 ) {
        parameters.set ( "Flag", Flag );
    }
    if (Alias.length() > 0) {
        parameters.set("Alias", Alias);
    }
    if (CopyDataFlags.length() > 0) {
        parameters.set("CopyDataFlags", CopyDataFlags);
    }
    // Delta limit.
	if ( DeltaLimit.length() > 0 ) {
		parameters.set ( "DeltaLimit", DeltaLimit );
	}
	if ( DeltaLimitAction.length() > 0 ) {
		parameters.set ( "DeltaLimitAction", DeltaLimitAction );
	}
	if ( DeltaLimitFlag.length() > 0 ) {
		parameters.set ( "DeltaLimitFlag", DeltaLimitFlag );
	}
	if ( IntervalLimit.length() > 0 ) {
		parameters.set ( "IntervalLimit", IntervalLimit );
	}
	if ( IntervalLimitAction.length() > 0 ) {
		parameters.set ( "IntervalLimitAction", IntervalLimitAction );
	}
	if ( IntervalLimitFlag.length() > 0 ) {
		parameters.set ( "IntervalLimitFlag", IntervalLimitFlag );
	}
    // ResetType=Auto.
	if ( AutoResetDatum.length() > 0 ) {
		parameters.set ( "AutoResetDatum", AutoResetDatum );
	}
	if ( AutoResetFlag.length() > 0 ) {
		parameters.set ( "AutoResetFlag", AutoResetFlag );
	}
    // ResetType=Rollover.
	if ( ResetMin.length() > 0 ) {
		parameters.set ( "ResetMin", ResetMin );
	}
	if ( ResetMax.length() > 0 ) {
        parameters.set ( "ResetMax", ResetMax );
    }
	if ( RolloverDeltaLimit.length() > 0 ) {
        parameters.set ( "RolloverDeltaLimit", RolloverDeltaLimit );
    }
	if ( RolloverFlag.length() > 0 ) {
        parameters.set ( "RolloverFlag", RolloverFlag );
    }
	if ( ManualResetFlag.length() > 0 ) {
        parameters.set ( "ManualResetFlag", ManualResetFlag );
    }
	if ( ResetProximityLimit.length() > 0 ) {
		parameters.set ( "ResetProximityLimit", ResetProximityLimit );
	}
	if ( ResetProximityLimitInterval.length() > 0 ) {
		parameters.set ( "ResetProximityLimitInterval", ResetProximityLimitInterval );
	}
	if ( ResetProximityLimitFlag.length() > 0 ) {
		parameters.set ( "ResetProximityLimitFlag", ResetProximityLimitFlag );
	}
    // Output table.
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
    if ( TableValuePreviousColumn.length() > 0 ) {
    	parameters.set ( "TableValuePreviousColumn", TableValuePreviousColumn );
    }
    if ( TableValueColumn.length() > 0 ) {
    	parameters.set ( "TableValueColumn", TableValueColumn );
    }
    if ( TableDeltaCalculatedColumn.length() > 0 ) {
    	parameters.set ( "TableDeltaCalculatedColumn", TableDeltaCalculatedColumn );
    }
    if ( TableActionColumn.length() > 0 ) {
    	parameters.set ( "TableActionColumn", TableActionColumn );
    }
    if ( TableDeltaColumn.length() > 0 ) {
    	parameters.set ( "TableDeltaColumn", TableDeltaColumn );
    }
    if ( TableFlagColumn.length() > 0 ) {
    	parameters.set ( "TableFlagColumn", TableFlagColumn );
    }
    if ( TableProblemColumn.length() > 0 ) {
    	parameters.set ( "TableProblemColumn", TableProblemColumn );
    }
    // Output properties.
    if ( ProblemCountProperty.length() > 0 ) {
        parameters.set ( "ProblemCountProperty", ProblemCountProperty );
    }
    if ( ProblemCountTimeSeriesProperty.length() > 0 ) {
        parameters.set ( "ProblemCountTimeSeriesProperty", ProblemCountTimeSeriesProperty );
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
    // General.
    String ExpectedTrend = __ExpectedTrend_JComboBox.getSelected();
    String ResetType = __ResetType_JComboBox.getSelected();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	String Flag = __Flag_JTextField.getText().trim();
	String Alias = __Alias_JTextField.getText().trim();
    String CopyDataFlags = __CopyDataFlags_JComboBox.getSelected();
	// Delta Limit.
	String DeltaLimit = __DeltaLimit_JTextField.getText().trim();
    String DeltaLimitAction = __DeltaLimitAction_JComboBox.getSelected();
	String DeltaLimitFlag = __DeltaLimitFlag_JTextField.getText().trim();
	String IntervalLimit = __IntervalLimit_JTextField.getText().trim();
    String IntervalLimitAction = __IntervalLimitAction_JComboBox.getSelected();
	String IntervalLimitFlag = __IntervalLimitFlag_JTextField.getText().trim();
	// ResetType=Auto
	String AutoResetDatum = __AutoResetDatum_JTextField.getText().trim();
	String AutoResetFlag = __AutoResetFlag_JTextField.getText().trim();
	// ResetType=Rollover
	String ResetMin = __ResetMin_JTextField.getText().trim();
	String ResetMax = __ResetMax_JTextField.getText().trim();
	String RolloverDeltaLimit = __RolloverDeltaLimit_JTextField.getText().trim();
	String RolloverFlag = __RolloverFlag_JTextField.getText().trim();
	String ManualResetFlag = __ManualResetFlag_JTextField.getText().trim();
	String ResetProximityLimit = __ResetProximityLimit_JTextField.getText().trim();
	String ResetProximityLimitInterval = __ResetProximityLimitInterval_JTextField.getText().trim();
	String ResetProximityLimitFlag = __ResetProximityLimitFlag_JTextField.getText().trim();
	// Output table.
    String TableID = __TableID_JComboBox.getSelected();
    String TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
    String TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
    String TableDateTimeColumn = __TableDateTimeColumn_JTextField.getText().trim();
    String TableValuePreviousColumn = __TableValuePreviousColumn_JTextField.getText().trim();
    String TableValueColumn = __TableValueColumn_JTextField.getText().trim();
    String TableDeltaCalculatedColumn = __TableDeltaCalculatedColumn_JTextField.getText().trim();
    String TableActionColumn = __TableActionColumn_JTextField.getText().trim();
    String TableDeltaColumn = __TableDeltaColumn_JTextField.getText().trim();
    String TableFlagColumn = __TableFlagColumn_JTextField.getText().trim();
    String TableProblemColumn = __TableProblemColumn_JTextField.getText().trim();
    // Output properties.
    String ProblemCountProperty = __ProblemCountProperty_JTextField.getText().trim();
    String ProblemCountTimeSeriesProperty = __ProblemCountTimeSeriesProperty_JTextField.getText().trim();

    // General.
    __command.setCommandParameter ( "TSList", TSList );
	__command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "ExpectedTrend", ExpectedTrend );
	__command.setCommandParameter ( "ResetType", ResetType );
	__command.setCommandParameter ( "AnalysisStart", AnalysisStart );
	__command.setCommandParameter ( "AnalysisEnd", AnalysisEnd );
	__command.setCommandParameter ( "Flag", Flag );
	__command.setCommandParameter ( "Alias", Alias );
	__command.setCommandParameter ( "CopyDataFlags", CopyDataFlags );
	// Delta limit.
	__command.setCommandParameter ( "DeltaLimit", DeltaLimit );
	__command.setCommandParameter ( "DeltaLimitAction", DeltaLimitAction );
	__command.setCommandParameter ( "DeltaLimitFlag", DeltaLimitFlag );
	__command.setCommandParameter ( "IntervalLimit", IntervalLimit );
	__command.setCommandParameter ( "IntervalLimitAction", IntervalLimitAction );
	__command.setCommandParameter ( "IntervalLimitFlag", IntervalLimitFlag );
	// ResetType=Auto
	__command.setCommandParameter ( "AutoResetDatum", AutoResetDatum );
	__command.setCommandParameter ( "AutoResetFlag", AutoResetFlag );
	// ResetType=Rollover
	__command.setCommandParameter ( "ResetMin", ResetMin );
	__command.setCommandParameter ( "ResetMax", ResetMax );
	__command.setCommandParameter ( "RolloverDeltaLimit", RolloverDeltaLimit );
	__command.setCommandParameter ( "RolloverFlag", RolloverFlag );
	__command.setCommandParameter ( "ManualResetFlag", ManualResetFlag );
	__command.setCommandParameter ( "ResetProximityLimit", ResetProximityLimit );
	__command.setCommandParameter ( "ResetProximityLimitInterval", ResetProximityLimitInterval );
	__command.setCommandParameter ( "ResetProximityLimitFlag", ResetProximityLimitFlag );
	// Output table.
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "TableTSIDColumn", TableTSIDColumn );
    __command.setCommandParameter ( "TableTSIDFormat", TableTSIDFormat );
    __command.setCommandParameter ( "TableDateTimeColumn", TableDateTimeColumn );
    __command.setCommandParameter ( "TableValuePreviousColumn", TableValuePreviousColumn );
    __command.setCommandParameter ( "TableValueColumn", TableValueColumn );
    __command.setCommandParameter ( "TableDeltaCalculatedColumn", TableDeltaCalculatedColumn );
    __command.setCommandParameter ( "TableActionColumn", TableActionColumn );
    __command.setCommandParameter ( "TableDeltaColumn", TableDeltaColumn );
    __command.setCommandParameter ( "TableFlagColumn", TableFlagColumn );
    __command.setCommandParameter ( "TableProblemColumn", TableProblemColumn );
    // Output properties.
	__command.setCommandParameter ( "ProblemCountProperty", ProblemCountProperty );
	__command.setCommandParameter ( "ProblemCountTimeSeriesProperty", ProblemCountTimeSeriesProperty );
}

/**
Instantiates the UI components.
@param parent JFrame class instantiating this class.
@param title Dialog title.
@param command The command to edit.
@param tableIDChoices choices for TableID value.
*/
private void initialize ( JFrame parent, Delta_Command command, List<String> tableIDChoices ) {
	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Create new time series as a delta (difference) between the current value and the previous value." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "If ResetType=" + RTi.TS.ResetType.AUTO + ", also specify ExpectedTrend and resets will automatically reset relative to zero."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "If ResetType=" + RTi.TS.ResetType.ROLLOVER +
        ", use the ResetMax and ResetMin parameters for cumulative time series that periodically roll over to a " +
        "new starting value (will compute differences across resets)." ),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "If ResetType=" + RTi.TS.ResetType.UNKNOWN + ", a simple delta is computed as current minus previous value."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify dates with precision appropriate for the data or use blank for all available data."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "<html><b>The new time series identifier is defaulted to the old, with \"-Delta\" appended to the data type" +
        " (may allow specifying as a parameter in the future).</b></html>."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    __TSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel ( this, main_JPanel, __TSList_JComboBox, y );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true ); // Allow edits.
    __TSID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, this, main_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, y );

    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits.
    __EnsembleID_JComboBox.setToolTipText("Select an ensemble identifier from the list or specify with ${Property} notation");
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
            this, this, main_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );

    __main_JTabbedPane = new JTabbedPane ();
    //__main_JTabbedPane.addChangeListener(this);
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for general parameters.
    int yGeneral = -1;
    JPanel general_JPanel = new JPanel();
    general_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "General", general_JPanel );

    JGUIUtil.addComponent(general_JPanel, new JLabel (
        "The following are general parameters."),
        0, ++yGeneral, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(general_JPanel, new JLabel (
        "See the Reset* tabs corresponding to the reset type (if appropriate)."),
        0, ++yGeneral, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(general_JPanel, new JLabel (
        "See the Output* tabs to save problems."),
        0, ++yGeneral, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(general_JPanel, new JLabel (
        "It is recommended that the flag be specified as Auto and also specify other flags in other tabs."),
        0, ++yGeneral, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(general_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yGeneral, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(general_JPanel, new JLabel ( "Expected trend:" ),
        0, ++yGeneral, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ExpectedTrend_JComboBox = new SimpleJComboBox ();
    List<String> expectedChoices = new ArrayList<>();
    expectedChoices.add ( "" );
    expectedChoices.add ( "" + TrendType.DECREASING );
    expectedChoices.add ( "" + TrendType.INCREASING );
    expectedChoices.add ( "" + TrendType.VARIABLE );
    __ExpectedTrend_JComboBox.setData(expectedChoices);
    __ExpectedTrend_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(general_JPanel, __ExpectedTrend_JComboBox,
        1, yGeneral, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(general_JPanel, new JLabel( "Optional - specify with reset limits (default=" +
        TrendType.VARIABLE + ")."),
        3, yGeneral, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(general_JPanel, new JLabel ( "Reset type:" ),
        0, ++yGeneral, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ResetType_JComboBox = new SimpleJComboBox ();
    List<String> resetTypeChoices = new ArrayList<>();
    resetTypeChoices.add ( "" );
    resetTypeChoices.add ( "" + ResetType.AUTO );
    resetTypeChoices.add ( "" + ResetType.ROLLOVER );
    resetTypeChoices.add ( "" + ResetType.UNKNOWN );
    __ResetType_JComboBox.setData(resetTypeChoices);
    __ResetType_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(general_JPanel, __ResetType_JComboBox,
        1, yGeneral, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(general_JPanel, new JLabel( "Optional - reset type (default=" + ResetType.UNKNOWN + ")."),
        3, yGeneral, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(general_JPanel, new JLabel ( "Analysis start:" ),
        0, ++yGeneral, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AnalysisStart_JTextField = new JTextField ( "", 20 );
    __AnalysisStart_JTextField.setToolTipText("Specify the analysis start using a date/time string or processor ${Property}");
    __AnalysisStart_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(general_JPanel, __AnalysisStart_JTextField,
        1, yGeneral, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(general_JPanel, new JLabel(
        "Optional - analysis start date/time (default=full time series period)."),
        3, yGeneral, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(general_JPanel, new JLabel ( "Analysis end:" ),
        0, ++yGeneral, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AnalysisEnd_JTextField = new JTextField ( "", 20 );
    __AnalysisEnd_JTextField.setToolTipText("Specify the analysis end using a date/time string or processor ${Property}");
    __AnalysisEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(general_JPanel, __AnalysisEnd_JTextField,
        1, yGeneral, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(general_JPanel, new JLabel(
        "Optional - analysis end date/time (default=full time series period)."),
        3, yGeneral, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(general_JPanel,new JLabel( "Flag:"),
        0, ++yGeneral, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Flag_JTextField = new JTextField ( "", 10 );
    __Flag_JTextField.setToolTipText("Specify the flag to mark problem values, can use ${Property}");
    __Flag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(general_JPanel, __Flag_JTextField,
        1, yGeneral, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(general_JPanel, new JLabel ( "Optional - flag to mark problem values (use Auto for defaults)."),
        3, yGeneral, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(general_JPanel, new JLabel("Alias to assign:"),
        0, ++yGeneral, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(10);
    __Alias_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener ( this );
    __Alias_JTextField.setToolTipText("%L for location, %T for data type.");
    JGUIUtil.addComponent(general_JPanel, __Alias_JTextField,
        1, yGeneral, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(general_JPanel, new JLabel ("Optional - use %L for location, etc. (default=no alias)."),
        3, yGeneral, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(general_JPanel, new JLabel ( "Copy data flags?:" ),
        0, ++yGeneral, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CopyDataFlags_JComboBox = new SimpleJComboBox ();
    List<String> copyFlagsChoices = new ArrayList<>();
    copyFlagsChoices.add ( "" );
    copyFlagsChoices.add ( "" + this.__command._False );
    copyFlagsChoices.add ( "" + this.__command._True );
    __CopyDataFlags_JComboBox.setData(copyFlagsChoices);
    __CopyDataFlags_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(general_JPanel, __CopyDataFlags_JComboBox,
        1, yGeneral, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(general_JPanel, new JLabel( "Optional - copy data flags from input (default=" + this.__command._False + ")."),
        3, yGeneral, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for delta limit parameters.
    int yLimit = -1;
    JPanel limit_JPanel = new JPanel();
    limit_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Delta Limit", limit_JPanel );

    JGUIUtil.addComponent(limit_JPanel, new JLabel (
        "The following parameters control handling of delta values that are larger than expected."),
        0, ++yLimit, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(limit_JPanel, new JLabel (
        "The time interval between input values can also be checked, for example to check for gaps between seasons."),
        0, ++yLimit, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(limit_JPanel, new JLabel (
        "An out of range absolute value delta may indicate bad input data."),
        0, ++yLimit, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(limit_JPanel, new JLabel (
        "The interval check is often omitted for ResetType=" + ResetType.ROLLOVER +
        " because a rollover often indicates sensor maintenance."),
        0, ++yLimit, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(limit_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yLimit, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(limit_JPanel, new JLabel ( "Delta limit:" ),
		0, ++yLimit, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DeltaLimit_JTextField = new JTextField ( 10 );
	__DeltaLimit_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(limit_JPanel, __DeltaLimit_JTextField,
		1, yLimit, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(limit_JPanel, new JLabel(
		"Optional - maximum allowed delta value."),
		3, yLimit, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(limit_JPanel, new JLabel ( "Delta limit action:" ),
        0, ++yLimit, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DeltaLimitAction_JComboBox = new SimpleJComboBox ( 12, false );    // Do not allow edit.
    List<String> deltaLimitActionChoices = new ArrayList<>();
    deltaLimitActionChoices.add("");
    deltaLimitActionChoices.add(__command._Keep);
    deltaLimitActionChoices.add(__command._SetMissing);
    __DeltaLimitAction_JComboBox.setData ( deltaLimitActionChoices );
    __DeltaLimitAction_JComboBox.select(0);
    __DeltaLimitAction_JComboBox.addItemListener ( this );
    __DeltaLimitAction_JComboBox.setMaximumRowCount(deltaLimitActionChoices.size());
    JGUIUtil.addComponent(limit_JPanel, __DeltaLimitAction_JComboBox,
        1, yLimit, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(limit_JPanel, new JLabel("Optional - action for values > limit (default=" + __command._Keep + ")."),
        3, yLimit, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(limit_JPanel, new JLabel ( "Delta limit flag:" ),
		0, ++yLimit, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DeltaLimitFlag_JTextField = new JTextField ( 10 );
	__DeltaLimitFlag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(limit_JPanel, __DeltaLimitFlag_JTextField,
		1, yLimit, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(limit_JPanel, new JLabel(
		"Optional - data flag for values with data > limit."),
		3, yLimit, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(limit_JPanel, new JLabel ( "Interval limit:" ),
		0, ++yLimit, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IntervalLimit_JTextField = new JTextField ( 10 );
	__IntervalLimit_JTextField.setToolTipText("Maximum interval between values (e.g., 1Day, 1Month).");
	__IntervalLimit_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(limit_JPanel, __IntervalLimit_JTextField,
		1, yLimit, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(limit_JPanel, new JLabel(
		"Optional - maximum allowed interval between values."),
		3, yLimit, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(limit_JPanel, new JLabel ( "Interval limit action:" ),
        0, ++yLimit, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IntervalLimitAction_JComboBox = new SimpleJComboBox ( 12, false );    // Do not allow edit.
    List<String> intervalLimitActionChoices = new ArrayList<>();
    intervalLimitActionChoices.add("");
    intervalLimitActionChoices.add(__command._Keep);
    intervalLimitActionChoices.add(__command._SetMissing);
    __IntervalLimitAction_JComboBox.setData ( intervalLimitActionChoices );
    __IntervalLimitAction_JComboBox.select(0);
    __IntervalLimitAction_JComboBox.addItemListener ( this );
    __IntervalLimitAction_JComboBox.setMaximumRowCount(intervalLimitActionChoices.size());
    JGUIUtil.addComponent(limit_JPanel, __IntervalLimitAction_JComboBox,
        1, yLimit, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(limit_JPanel, new JLabel("Optional - action for interval > limit (default=" + __command._Keep + ")."),
        3, yLimit, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(limit_JPanel, new JLabel ( "Interval limit flag:" ),
		0, ++yLimit, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IntervalLimitFlag_JTextField = new JTextField ( 10 );
	__IntervalLimitFlag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(limit_JPanel, __IntervalLimitFlag_JTextField,
		1, yLimit, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(limit_JPanel, new JLabel(
		"Optional - data flag for values with interval > limit."),
		3, yLimit, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for ResetType=Auto parameters.
    int yResetAuto = -1;
    JPanel resetAuto_JPanel = new JPanel();
    resetAuto_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Reset - Auto", resetAuto_JPanel );

    JGUIUtil.addComponent(resetAuto_JPanel, new JLabel (
        "The following parameters are used when ResetType=" + ResetType.AUTO + "."),
        0, ++yResetAuto, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(resetAuto_JPanel, new JLabel (
        "Use the 'Data Limit' tab parameters to set a limit on the allowed delta value."),
        0, ++yResetAuto, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(resetAuto_JPanel, new JLabel (
        "The AutoResetDatum currently always defaults to zero."),
        0, ++yResetAuto, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(resetAuto_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yResetAuto, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(resetAuto_JPanel,new JLabel( "Auto reset datum:"),
        0, ++yResetAuto, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AutoResetDatum_JTextField = new JTextField ( "", 10 );
    __AutoResetDatum_JTextField.setEnabled(false); // Future feature.
    __AutoResetDatum_JTextField.setToolTipText("Future feature.  The datum for ResetType=Auto, 'Auto' or a number, can use ${Property}");
    __AutoResetDatum_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(resetAuto_JPanel, __AutoResetDatum_JTextField,
        1, yResetAuto, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(resetAuto_JPanel, new JLabel ( "Optional - datum for auto reset (default=0)."),
        3, yResetAuto, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(resetAuto_JPanel,new JLabel( "Auto reset flag:"),
        0, ++yResetAuto, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AutoResetFlag_JTextField = new JTextField ( "", 10 );
    __AutoResetFlag_JTextField.setToolTipText("Flag to set for auto reset values. `+A` will be used if `Auto` is specified. Can use ${Property}.");
    __AutoResetFlag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(resetAuto_JPanel, __AutoResetFlag_JTextField,
        1, yResetAuto, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(resetAuto_JPanel, new JLabel ( "Optional - flag for auto reset (default=none, can use Auto)."),
        3, yResetAuto, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    // Panel for ResetType=Rollover parameters.
    int yResetRollover = -1;
    JPanel resetRollover_JPanel = new JPanel();
    resetRollover_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Reset - Rollover", resetRollover_JPanel );

    JGUIUtil.addComponent(resetRollover_JPanel, new JLabel (
        "The following parameters are used when ResetType=" + ResetType.ROLLOVER + "."),
        0, ++yResetRollover, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(resetRollover_JPanel, new JLabel (
        "For example, a sensor may have an allowed range ResetValueMin=0 to ResteValueMax=2048."),
        0, ++yResetRollover, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    /*
    JGUIUtil.addComponent(resetRollover_JPanel, new JLabel (
        "The reset proximity parameters can be set to check for unexpected values near a non-rollover reset."),
        0, ++yResetRollover, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(resetRollover_JPanel, new JLabel (
        "For example, precipitation sensor maintenance usually occurs at a time when precipitation is not occurring"),
        0, ++yResetRollover, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(resetRollover_JPanel, new JLabel (
        "and non-zero sum in the proximity interval may indicate invalid data due to sensor calibration."),
        0, ++yResetRollover, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        */
    JGUIUtil.addComponent(resetRollover_JPanel, new JLabel (
        "If the RolloverDeltaLimit is specified, a delta of (ResetMax - previous value) that"
        + " is larger than the limit will be treated as a manual reset."),
        0, ++yResetRollover, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(resetRollover_JPanel, new JLabel (
        "and the delta up to ResetMax will not be counted."),
        0, ++yResetRollover, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(resetRollover_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yResetRollover, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(resetRollover_JPanel, new JLabel ( "Reset value (minimum):" ),
		0, ++yResetRollover, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ResetMin_JTextField = new JTextField ( 10 );
	__ResetMin_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(resetRollover_JPanel, __ResetMin_JTextField,
		1, yResetRollover, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(resetRollover_JPanel, new JLabel(
		"Optional - minimum value for a rollover reset."),
		3, yResetRollover, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(resetRollover_JPanel, new JLabel ( "Reset value (maximum):" ),
        0, ++yResetRollover, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ResetMax_JTextField = new JTextField ( 10 );
    __ResetMax_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(resetRollover_JPanel, __ResetMax_JTextField,
        1, yResetRollover, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(resetRollover_JPanel, new JLabel(
        "Optional - maximum value for a rollover reset."),
        3, yResetRollover, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(resetRollover_JPanel, new JLabel ( "Rollover delta limit:" ),
        0, ++yResetRollover, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __RolloverDeltaLimit_JTextField = new JTextField ( 10 );
    __RolloverDeltaLimit_JTextField.setToolTipText("Maximum delta (previous value) for rollover, else treat as manual reset, can use ${Property}.");
    __RolloverDeltaLimit_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(resetRollover_JPanel, __RolloverDeltaLimit_JTextField,
        1, yResetRollover, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(resetRollover_JPanel, new JLabel(
        "Optional - maximum delta (previous) for rollover (default=no limit)."),
        3, yResetRollover, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(resetRollover_JPanel, new JLabel ( "Rollover flag:" ),
        0, ++yResetRollover, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __RolloverFlag_JTextField = new JTextField ( 10 );
    __RolloverFlag_JTextField.setToolTipText("Flag to set for rollover. +R will be used if specified as Auto.  Can use ${Property}.");
    __RolloverFlag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(resetRollover_JPanel, __RolloverFlag_JTextField,
        1, yResetRollover, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(resetRollover_JPanel, new JLabel(
        "Optional - flag for rollover (default=none, can use Auto)."),
        3, yResetRollover, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(resetRollover_JPanel, new JLabel ( "Manual reset flag:" ),
        0, ++yResetRollover, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ManualResetFlag_JTextField = new JTextField ( 10 );
    __ManualResetFlag_JTextField.setToolTipText("Flag to set for manual reset. +r will be used if specified as Auto.  Can use ${Property}.");
    __ManualResetFlag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(resetRollover_JPanel, __ManualResetFlag_JTextField,
        1, yResetRollover, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(resetRollover_JPanel, new JLabel(
        "Optional - flag for manual reset (default=none, can use Auto)."),
        3, yResetRollover, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(resetRollover_JPanel, new JLabel ( "Reset proximity limit:" ),
		0, ++yResetRollover, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ResetProximityLimit_JTextField = new JTextField ( 10 );
	__ResetProximityLimit_JTextField.setToolTipText("Future feature.  Allowed delta immediately prior to or after a reset, to check for bad data.");
	__ResetProximityLimit_JTextField.setEnabled(false); // Future feature.
	__ResetProximityLimit_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(resetRollover_JPanel, __ResetProximityLimit_JTextField,
		1, yResetRollover, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(resetRollover_JPanel, new JLabel(
		"Future feature - maximum allowed sum near the reset (default=not checked)."),
		3, yResetRollover, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(resetRollover_JPanel, new JLabel ( "Reset proximity limit interval:" ),
		0, ++yResetRollover, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ResetProximityLimitInterval_JTextField = new JTextField ( 10 );
	__ResetProximityLimitInterval_JTextField.setToolTipText("Future feature.  Limit used to calculate RestProximityLimit.");
	__ResetProximityLimitInterval_JTextField.setEnabled(false); // Future feature.
	__ResetProximityLimitInterval_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(resetRollover_JPanel, __ResetProximityLimitInterval_JTextField,
		1, yResetRollover, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(resetRollover_JPanel, new JLabel(
		"Future feature - interval for ResetProximityLimit (default=not checked)."),
		3, yResetRollover, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(resetRollover_JPanel, new JLabel ( "Reset proximity limit flag:" ),
		0, ++yResetRollover, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ResetProximityLimitFlag_JTextField = new JTextField ( 10 );
	__ResetProximityLimitFlag_JTextField.setToolTipText("Future feature.  Flag for values that exceed the ResetProximityLimit.");
	__ResetProximityLimitFlag_JTextField.setEnabled(false); // Future feature.
	__ResetProximityLimitFlag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(resetRollover_JPanel, __ResetProximityLimitFlag_JTextField,
		1, yResetRollover, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(resetRollover_JPanel, new JLabel(
		"Future feature - data flag for ResetProximityLimit."),
		3, yResetRollover, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for output table parameters.
    int yTable = -1;
    JPanel table_JPanel = new JPanel();
    table_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output Table", table_JPanel );

    JGUIUtil.addComponent(table_JPanel, new JLabel (
        "The following parameters are used to control the output table, which includes problems from the analysis." ),
        0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(table_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
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
    JGUIUtil.addComponent(table_JPanel, new JLabel( "Optional - table for output."),
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
    JGUIUtil.addComponent(table_JPanel, new JLabel( "Optional - column name for date/time (default=DateTime)."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table previous value column:" ),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableValuePreviousColumn_JTextField = new JTextField ( 10 );
    __TableValuePreviousColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __TableValuePreviousColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel( "Optional - column name for time series previous value (default=ValuePrevious)."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table value column:" ),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableValueColumn_JTextField = new JTextField ( 10 );
    __TableValueColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __TableValueColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel( "Optional - column name for time series value (default=Value)."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table delta (calculated) column:" ),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableDeltaCalculatedColumn_JTextField = new JTextField ( 10 );
    __TableDeltaCalculatedColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __TableDeltaCalculatedColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel( "Optional - column name for delta (calculated) (default=DeltaCalculated)."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table action column:" ),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableActionColumn_JTextField = new JTextField ( 10 );
    __TableActionColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __TableActionColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel( "Optional - column name for action (default=Action)."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table delta column:" ),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableDeltaColumn_JTextField = new JTextField ( 10 );
    __TableDeltaColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __TableDeltaColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel( "Optional - column name for delta value (default=Delta)."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table flag column:" ),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableFlagColumn_JTextField = new JTextField ( 10 );
    __TableFlagColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __TableFlagColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel( "Optional - column name for time series flag (default=Flag)."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table problem column:" ),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableProblemColumn_JTextField = new JTextField ( 10 );
    __TableProblemColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __TableProblemColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel( "Optional - column name for problem (default=Problem)."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for output properties.
    int yProp = -1;
    JPanel prop_JPanel = new JPanel();
    prop_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output Properties", prop_JPanel );

    JGUIUtil.addComponent(prop_JPanel, new JLabel (
		"Specify a time series and/or processor property to set to the count of problems."),
		0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel (
		"The processor property is visible globally whereas the time series property is associated with the specific time series."),
		0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(prop_JPanel, new JLabel ( "Problem count processor property:" ),
        0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ProblemCountProperty_JTextField = new JTextField ( 20 );
    __ProblemCountProperty_JTextField.setToolTipText("Specify processor property to set or use ${Property}, ${ts:Property}, %-specifier notation");
    __ProblemCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(prop_JPanel, __ProblemCountProperty_JTextField,
        1, yProp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel(
        "Optional - name of processor property for problem count."),
        3, yProp, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(prop_JPanel, new JLabel ( "Problem count time series property:" ),
        0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ProblemCountTimeSeriesProperty_JTextField = new JTextField ( 20 );
    __ProblemCountTimeSeriesProperty_JTextField.setToolTipText("Specify time series property to set or use ${Property}, ${ts:Property}, %-specifier notation");
    __ProblemCountTimeSeriesProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(prop_JPanel, __ProblemCountTimeSeriesProperty_JTextField,
        1, yProp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel(
        "Optional - name of time series property for problem count."),
        3, yProp, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Command text area.
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

	// Add a button panel.
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
    // General.
    String ExpectedTrend = "";
	String ResetType = "";
	String AnalysisStart = "";
	String AnalysisEnd = "";
	String Flag = "";
    String Alias = "";
    String CopyDataFlags = "";
    // Delta limit.
	String DeltaLimit = "";
	String DeltaLimitAction = "";
	String DeltaLimitFlag = "";
	String IntervalLimit = "";
	String IntervalLimitAction = "";
	String IntervalLimitFlag = "";
    // ResetType=Auto.
	String AutoResetDatum = "";
	String AutoResetFlag = "";
    // ResetType=Rollover.
	String ResetMin = "";
	String ResetMax = "";
	String RolloverDeltaLimit = "";
	String RolloverFlag = "";
	String ManualResetFlag = "";
	String ResetProximityLimit = "";
	String ResetProximityLimitInterval = "";
	String ResetProximityLimitFlag = "";
    // Output table.
	String TableID = "";
	String TableTSIDColumn = "";
	String TableTSIDFormat = "";
	String TableDateTimeColumn = "";
	String TableValuePreviousColumn = "";
	String TableValueColumn = "";
	String TableDeltaCalculatedColumn = "";
	String TableActionColumn = "";
	String TableDeltaColumn = "";
	String TableFlagColumn = "";
	String TableProblemColumn = "";
	// Output properties.
	String ProblemCountProperty = "";
	String ProblemCountTimeSeriesProperty = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command.
        TSList = props.getValue ( "TSList" );
		TSID = props.getValue ( "TSID" );
        EnsembleID = props.getValue ( "EnsembleID" );
        // General.
        ExpectedTrend = props.getValue ( "ExpectedTrend" );
		ResetType = props.getValue ( "ResetType" );
		AnalysisStart = props.getValue ( "AnalysisStart" );
		AnalysisEnd = props.getValue ( "AnalysisEnd" );
	    Flag = props.getValue ( "Flag" );
        Alias = props.getValue("Alias");
        CopyDataFlags = props.getValue("CopyDataFlags");
        // Delta limit.
        DeltaLimit = props.getValue("DeltaLimit");
        DeltaLimitAction = props.getValue("DeltaLimitAction");
        DeltaLimitFlag = props.getValue("DeltaLimitFlag");
        IntervalLimit = props.getValue("IntervalLimit");
        IntervalLimitAction = props.getValue("IntervalLimitAction");
        IntervalLimitFlag = props.getValue("IntervalLimitFlag");
        // ResetType=Auto.
		AutoResetDatum = props.getValue ( "AutoResetDatum" );
		AutoResetFlag = props.getValue ( "AutoResetFlag" );
        // ResetType=Rollover.
		ResetMin = props.getValue ( "ResetMin" );
		ResetMax = props.getValue ( "ResetMax" );
		RolloverDeltaLimit = props.getValue ( "RolloverDeltaLimit" );
		RolloverFlag = props.getValue ( "RolloverFlag" );
		ManualResetFlag = props.getValue ( "ManualResetFlag" );
        ResetProximityLimit = props.getValue("ResetProximityLimit");
        ResetProximityLimitInterval = props.getValue("ResetProximityLimitInterval");
        ResetProximityLimitFlag = props.getValue("ResetProximityLimitFlag");
        // Output table.
        TableID = props.getValue ( "TableID" );
        TableTSIDColumn = props.getValue ( "TableTSIDColumn" );
        TableTSIDFormat = props.getValue ( "TableTSIDFormat" );
        TableDateTimeColumn = props.getValue ( "TableDateTimeColumn" );
        TableValuePreviousColumn = props.getValue ( "TableValueColumn" );
        TableValueColumn = props.getValue ( "TableValueColumn" );
        TableDeltaCalculatedColumn = props.getValue ( "TableDeltaCalculatedColumn" );
        TableActionColumn = props.getValue ( "TableActionColumn" );
        TableDeltaColumn = props.getValue ( "TableDeltaColumn" );
        TableFlagColumn = props.getValue ( "TableFlagColumn" );
        TableProblemColumn = props.getValue ( "TableProblemColumn" );
        // Output properties.
		ProblemCountProperty = props.getValue ( "ProblemCountProperty" );
		ProblemCountTimeSeriesProperty = props.getValue ( "ProblemCountTimeSeriesProperty" );
        if ( TSList == null ) {
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
        // General.
		if ( JGUIUtil.isSimpleJComboBoxItem(__ExpectedTrend_JComboBox, ExpectedTrend,JGUIUtil.NONE, null, null ) ) {
			__ExpectedTrend_JComboBox.select ( ExpectedTrend );
		}
		else {
            if ( (ExpectedTrend == null) ||	ExpectedTrend.equals("") ) {
				// New command...select the default.
				__ExpectedTrend_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"ExpectedTrend parameter \"" +	ExpectedTrend +
				"\".  Select a\n value or Cancel." );
			}
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__ResetType_JComboBox, ResetType,JGUIUtil.NONE, null, null ) ) {
			__ResetType_JComboBox.select ( ResetType );
		}
		else {
            if ( (ResetType == null) ||	ResetType.equals("") ) {
				// New command...select the default.
				__ResetType_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"ResetType parameter \"" +	ResetType +
				"\".  Select a\n value or Cancel." );
			}
		}
		if ( AnalysisStart != null ) {
			__AnalysisStart_JTextField.setText( AnalysisStart );
		}
		if ( AnalysisEnd != null ) {
			__AnalysisEnd_JTextField.setText ( AnalysisEnd );
		}
        if ( Flag != null ) {
            __Flag_JTextField.setText ( Flag );
        }
        if (Alias != null ) {
            __Alias_JTextField.setText(Alias.trim());
        }
		if ( JGUIUtil.isSimpleJComboBoxItem(__CopyDataFlags_JComboBox, CopyDataFlags,JGUIUtil.NONE, null, null ) ) {
			__CopyDataFlags_JComboBox.select ( CopyDataFlags );
		}
		else {
            if ( (CopyDataFlags == null) ||	CopyDataFlags.equals("") ) {
				// New command...select the default.
				__CopyDataFlags_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"CopyDataFlags parameter \"" +	CopyDataFlags +
				"\".  Select a\n value or Cancel." );
			}
		}
        // Delta limit.
		if ( DeltaLimit != null ) {
			__DeltaLimit_JTextField.setText ( DeltaLimit );
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__DeltaLimitAction_JComboBox, DeltaLimitAction,JGUIUtil.NONE, null, null ) ) {
			__DeltaLimitAction_JComboBox.select ( DeltaLimitAction );
		}
		else {
            if ( (DeltaLimitAction == null) ||	DeltaLimitAction.equals("") ) {
				// New command...select the default.
				__DeltaLimitAction_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"DeltaLimitAction parameter \"" +	DeltaLimitAction +
				"\".  Select a\n value or Cancel." );
			}
		}
		if ( DeltaLimitFlag != null ) {
			__DeltaLimitFlag_JTextField.setText ( DeltaLimitFlag );
		}
		if ( IntervalLimit != null ) {
			__IntervalLimit_JTextField.setText ( IntervalLimit );
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__IntervalLimitAction_JComboBox, IntervalLimitAction,JGUIUtil.NONE, null, null ) ) {
			__IntervalLimitAction_JComboBox.select ( IntervalLimitAction );
		}
		else {
            if ( (IntervalLimitAction == null) ||	IntervalLimitAction.equals("") ) {
				// New command...select the default.
				__IntervalLimitAction_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"IntervalLimitAction parameter \"" +	IntervalLimitAction +
				"\".  Select a\n value or Cancel." );
			}
		}
		if ( IntervalLimitFlag != null ) {
			__IntervalLimitFlag_JTextField.setText ( IntervalLimitFlag );
		}
        // ResetType=Auto.
		if ( AutoResetDatum != null ) {
			__AutoResetDatum_JTextField.setText ( AutoResetDatum );
		}
		if ( AutoResetFlag != null ) {
			__AutoResetFlag_JTextField.setText ( AutoResetFlag );
		}
        // ResetType=Rollover.
		if ( ResetMin != null ) {
			__ResetMin_JTextField.setText ( ResetMin );
		}
        if ( ResetMax != null ) {
            __ResetMax_JTextField.setText ( ResetMax );
        }
        if ( RolloverDeltaLimit != null ) {
            __RolloverDeltaLimit_JTextField.setText ( RolloverDeltaLimit );
        }
        if ( RolloverFlag != null ) {
            __RolloverFlag_JTextField.setText ( RolloverFlag );
        }
        if ( ManualResetFlag != null ) {
            __ManualResetFlag_JTextField.setText ( ManualResetFlag );
        }
        if ( ResetProximityLimit != null ) {
            __ResetProximityLimit_JTextField.setText ( ResetProximityLimit );
        }
        if ( ResetProximityLimitInterval != null ) {
            __ResetProximityLimitInterval_JTextField.setText ( ResetProximityLimitInterval );
        }
        if ( ResetProximityLimitFlag != null ) {
            __ResetProximityLimitFlag_JTextField.setText ( ResetProximityLimitFlag );
        }
        // Output table.
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
        if ( TableValuePreviousColumn != null ) {
            __TableValuePreviousColumn_JTextField.setText ( TableValuePreviousColumn );
        }
        if ( TableValueColumn != null ) {
            __TableValueColumn_JTextField.setText ( TableValueColumn );
        }
        if ( TableDeltaCalculatedColumn != null ) {
            __TableDeltaCalculatedColumn_JTextField.setText ( TableDeltaCalculatedColumn );
        }
        if ( TableActionColumn != null ) {
            __TableActionColumn_JTextField.setText ( TableActionColumn );
        }
        if ( TableDeltaColumn != null ) {
            __TableDeltaColumn_JTextField.setText ( TableDeltaColumn );
        }
        if ( TableFlagColumn != null ) {
            __TableFlagColumn_JTextField.setText ( TableFlagColumn );
        }
        if ( TableProblemColumn != null ) {
            __TableProblemColumn_JTextField.setText ( TableProblemColumn );
        }
        if ( ProblemCountProperty != null ) {
            __ProblemCountProperty_JTextField.setText ( ProblemCountProperty );
        }
        if ( ProblemCountTimeSeriesProperty != null ) {
            __ProblemCountTimeSeriesProperty_JTextField.setText ( ProblemCountTimeSeriesProperty );
        }
	}
	// Regardless, reset the command from the fields.
    TSList = __TSList_JComboBox.getSelected();
	TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    // General.
    ExpectedTrend = __ExpectedTrend_JComboBox.getSelected();
    ResetType = __ResetType_JComboBox.getSelected();
	AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	Flag = __Flag_JTextField.getText().trim();
	Alias = __Alias_JTextField.getText().trim();
    CopyDataFlags = __CopyDataFlags_JComboBox.getSelected();
	// Delta limit.
	DeltaLimit = __DeltaLimit_JTextField.getText().trim();
    DeltaLimitAction = __DeltaLimitAction_JComboBox.getSelected();
	DeltaLimitFlag = __DeltaLimitFlag_JTextField.getText().trim();
	IntervalLimit = __IntervalLimit_JTextField.getText().trim();
    IntervalLimitAction = __IntervalLimitAction_JComboBox.getSelected();
	IntervalLimitFlag = __IntervalLimitFlag_JTextField.getText().trim();
	// ResetType=Auto.
	AutoResetDatum = __AutoResetDatum_JTextField.getText().trim();
	AutoResetFlag = __AutoResetFlag_JTextField.getText().trim();
	// ResetType=Rollover.
	ResetMax = __ResetMax_JTextField.getText().trim();
	ResetMin = __ResetMin_JTextField.getText().trim();
	RolloverDeltaLimit = __RolloverDeltaLimit_JTextField.getText().trim();
	RolloverFlag = __RolloverFlag_JTextField.getText().trim();
	ManualResetFlag = __ManualResetFlag_JTextField.getText().trim();
	ResetProximityLimit = __ResetProximityLimit_JTextField.getText().trim();
	ResetProximityLimitInterval = __ResetProximityLimitInterval_JTextField.getText().trim();
	ResetProximityLimitFlag = __ResetProximityLimitFlag_JTextField.getText().trim();
	// Output table.
    TableID = __TableID_JComboBox.getSelected();
    TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
    TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
    TableDateTimeColumn = __TableDateTimeColumn_JTextField.getText().trim();
    TableValuePreviousColumn = __TableValuePreviousColumn_JTextField.getText().trim();
    TableValueColumn = __TableValueColumn_JTextField.getText().trim();
    TableDeltaCalculatedColumn = __TableDeltaCalculatedColumn_JTextField.getText().trim();
    TableActionColumn = __TableActionColumn_JTextField.getText().trim();
    TableDeltaColumn = __TableDeltaColumn_JTextField.getText().trim();
    TableFlagColumn = __TableFlagColumn_JTextField.getText().trim();
    TableProblemColumn = __TableProblemColumn_JTextField.getText().trim();
    // Output properties.
	ProblemCountProperty = __ProblemCountProperty_JTextField.getText().trim();
	ProblemCountTimeSeriesProperty = __ProblemCountTimeSeriesProperty_JTextField.getText().trim();

	props = new PropList ( __command.getCommandName() );
    props.add ( "TSList=" + TSList );
	props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
    // General.
    props.add ( "ExpectedTrend=" + ExpectedTrend );
    props.add ( "ResetType=" + ResetType );
	props.add ( "AnalysisStart=" + AnalysisStart );
	props.add ( "AnalysisEnd=" + AnalysisEnd );
	props.add ( "Flag=" + Flag );
	props.add ( "Alias=" + Alias );
	props.add ( "CopyDataFlags=" + CopyDataFlags );
	// Delta limit.
	props.add ( "DeltaLimit=" + DeltaLimit );
	props.add ( "DeltaLimitAction=" + DeltaLimitAction );
	props.add ( "DeltaLimitFlag=" + DeltaLimitFlag );
	props.add ( "IntervalLimit=" + IntervalLimit );
	props.add ( "IntervalLimitAction=" + IntervalLimitAction );
	props.add ( "IntervalLimitFlag=" + IntervalLimitFlag );
	// ResetType=Auto.
	props.add ( "AutoResetDatum=" + AutoResetDatum );
	props.add ( "AutoResetFlag=" + AutoResetFlag );
	// ResetType=Rollover.
	props.add ( "ResetMin=" + ResetMin );
	props.add ( "ResetMax=" + ResetMax );
	props.add ( "RolloverDeltaLimit=" + RolloverDeltaLimit );
	props.add ( "RolloverFlag=" + RolloverFlag );
	props.add ( "ManualResetFlag=" + ManualResetFlag );
	props.add ( "ResetProximityLimit=" + ResetProximityLimit );
	props.add ( "ResetProximityLimitInterval=" + ResetProximityLimitInterval );
	props.add ( "ResetProximityLimitFlag=" + ResetProximityLimitFlag );
	// Output table.
    props.add ( "TableID=" + TableID );
    props.add ( "TableTSIDColumn=" + TableTSIDColumn );
    props.add ( "TableTSIDFormat=" + TableTSIDFormat );
    props.add ( "TableDateTimeColumn=" + TableDateTimeColumn );
    props.add ( "TableValuePreviousColumn=" + TableValuePreviousColumn );
    props.add ( "TableValueColumn=" + TableValueColumn );
    props.add ( "TableDeltaCalculatedColumn=" + TableDeltaCalculatedColumn );
    props.add ( "TableActionColumn=" + TableActionColumn );
    props.add ( "TableDeltaColumn=" + TableDeltaColumn );
    props.add ( "TableFlagColumn=" + TableFlagColumn );
    props.add ( "TableProblemColumn=" + TableProblemColumn );
    // Output properties.
	props.add ( "ProblemCountProperty=" + ProblemCountProperty );
	props.add ( "ProblemCountTimeSeriesProperty=" + ProblemCountTimeSeriesProperty );
	__command_JTextArea.setText( __command.toString ( props ).trim() );
	Message.printStatus(2,routine,"In refresh, TableID=" + TableID);
}

/**
React to the user response.
@param ok if false, then the edit is canceled.
If true, the edit is committed and the dialog is closed.
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