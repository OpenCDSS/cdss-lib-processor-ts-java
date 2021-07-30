// CheckTimeSeriesStatistic_JDialog - editor for CheckTimeSeriesStatistic command

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

package rti.tscommandprocessor.commands.check;

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

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.TS.TSStatisticType;
import RTi.TS.TSUtil_CalculateTimeSeriesStatistic;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

@SuppressWarnings("serial")
public class CheckTimeSeriesStatistic_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private CheckTimeSeriesStatistic_Command __command = null;
private JTextArea __command_JTextArea=null;
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox	__TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private JTabbedPane __main_JTabbedPane = null;
private SimpleJComboBox __Statistic_JComboBox = null;
private JTextField __StatisticValue1_JTextField = null;
private JTextField __StatisticValue2_JTextField = null;
private JTextField __StatisticValue3_JTextField = null;
private JTextField __AnalysisStart_JTextField = null;
private JTextField __AnalysisEnd_JTextField = null;
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __TableTSIDColumn_JTextField = null;
private TSFormatSpecifiersJPanel __TableTSIDFormat_JTextField = null; // Format for time series identifiers
private JTextField __TableStatisticColumn_JTextField = null;
private SimpleJComboBox __CheckCriteria_JComboBox = null;
private JTextField __CheckValue1_JTextField = null;
private JTextField __CheckValue2_JTextField = null;
private SimpleJComboBox __IfCriteriaMet_JComboBox = null;
private JTextField __ProblemType_JTextField = null; // Field for problem type
private JTextField __PropertyName_JTextField = null;
private JTextField __PropertyValue_JTextField = null;
//private SimpleJComboBox __Action_JComboBox = null;
private boolean __error_wait = false; // Is there an error to be cleared up or Cancel?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public CheckTimeSeriesStatistic_JDialog ( JFrame parent, CheckTimeSeriesStatistic_Command command,
    List<String> tableIDChoices )
{	super(parent, true);
	initialize ( parent, command, tableIDChoices );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();

	if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "CheckTimeSeriesStatistic");
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
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList parameters = new PropList ( "" );
    String TSList = __TSList_JComboBox.getSelected();
	String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String Statistic = __Statistic_JComboBox.getSelected();
    String StatisticValue1 = __StatisticValue1_JTextField.getText().trim();
    String StatisticValue2 = __StatisticValue2_JTextField.getText().trim();
    String StatisticValue3 = __StatisticValue3_JTextField.getText().trim();
    String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
    String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
    String TableID = __TableID_JComboBox.getSelected();
    String TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
    String TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
    String TableStatisticColumn = __TableStatisticColumn_JTextField.getText().trim();
    String CheckCriteria = __CheckCriteria_JComboBox.getSelected();
	String CheckValue1 = __CheckValue1_JTextField.getText().trim();
	String CheckValue2 = __CheckValue2_JTextField.getText().trim();
	String IfCriteriaMet = __IfCriteriaMet_JComboBox.getSelected();
	String ProblemType = __ProblemType_JTextField.getText().trim();
    String PropertyName = __PropertyName_JTextField.getText().trim();
    String PropertyValue = __PropertyValue_JTextField.getText().trim();
    //String Action = __Action_JComboBox.getSelected();
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
    if ( Statistic.length() > 0 ) {
        parameters.set ( "Statistic", Statistic );
    }
    if ( StatisticValue1.length() > 0 ) {
        parameters.set ( "StatisticValue1", StatisticValue1 );
    }
    if ( StatisticValue2.length() > 0 ) {
        parameters.set ( "StatisticValue2", StatisticValue2 );
    }
    if ( StatisticValue3.length() > 0 ) {
        parameters.set ( "StatisticValue3", StatisticValue3 );
    }
    if ( AnalysisStart.length() > 0 ) {
        parameters.set ( "AnalysisStart", AnalysisStart );
    }
    if ( AnalysisEnd.length() > 0 ) {
        parameters.set ( "AnalysisEnd", AnalysisEnd );
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
    if ( TableStatisticColumn.length() > 0 ) {
        parameters.set ( "TableStatisticColumn", TableStatisticColumn );
    }
    if ( CheckCriteria.length() > 0 ) {
        parameters.set ( "CheckCriteria", CheckCriteria );
    }
	if ( CheckValue1.length() > 0 ) {
		parameters.set ( "CheckValue1", CheckValue1 );
	}
    if ( CheckValue2.length() > 0 ) {
        parameters.set ( "CheckValue2", CheckValue2 );
    }
    if ( IfCriteriaMet.length() > 0 ) {
        parameters.set ( "IfCriteriaMet", IfCriteriaMet );
    }
	if ( ProblemType.length() > 0 ) {
		parameters.set ( "ProblemType", ProblemType );
	}
    if ( PropertyName.length() > 0 ) {
        parameters.set ( "PropertyName", PropertyName );
    }
    if ( PropertyValue.length() > 0 ) {
        parameters.set ( "PropertyValue", PropertyValue );
    }
    //if ( Action.length() > 0 ) {
    //    parameters.set ( "Action", Action );
    //}
	try {
	    // This will warn the user...
		__command.checkCommandParameters ( parameters, null, 1 );
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
{	String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String Statistic = __Statistic_JComboBox.getSelected();
    String StatisticValue1 = __StatisticValue1_JTextField.getText().trim();
    String StatisticValue2 = __StatisticValue2_JTextField.getText().trim();
    String StatisticValue3 = __StatisticValue3_JTextField.getText().trim();
    String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
    String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
    String TableID = __TableID_JComboBox.getSelected();
    String TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
    String TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
    String TableStatisticColumn = __TableStatisticColumn_JTextField.getText().trim();
    String CheckCriteria = __CheckCriteria_JComboBox.getSelected();
	String CheckValue1 = __CheckValue1_JTextField.getText().trim();
	String CheckValue2 = __CheckValue2_JTextField.getText().trim();
	String IfCriteriaMet = __IfCriteriaMet_JComboBox.getSelected();
	String ProblemType = __ProblemType_JTextField.getText().trim();
    String PropertyName = __PropertyName_JTextField.getText().trim();
	String PropertyValue = __PropertyValue_JTextField.getText().trim();
    //String Action = __Action_JComboBox.getSelected();
    __command.setCommandParameter ( "TSList", TSList );
	__command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "Statistic", Statistic );
    __command.setCommandParameter ( "StatisticValue1", StatisticValue1 );
    __command.setCommandParameter ( "StatisticValue2", StatisticValue2 );
    __command.setCommandParameter ( "StatisticValue3", StatisticValue3 );
    __command.setCommandParameter ( "AnalysisStart", AnalysisStart );
    __command.setCommandParameter ( "AnalysisEnd", AnalysisEnd );
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "TableTSIDColumn", TableTSIDColumn );
    __command.setCommandParameter ( "TableTSIDFormat", TableTSIDFormat );
    __command.setCommandParameter ( "TableStatisticColumn", TableStatisticColumn );
    __command.setCommandParameter ( "CheckCriteria", CheckCriteria );
	__command.setCommandParameter ( "CheckValue1", CheckValue1 );
	__command.setCommandParameter ( "CheckValue2", CheckValue2 );
	__command.setCommandParameter ( "IfCriteriaMet", IfCriteriaMet );
	__command.setCommandParameter ( "ProblemType", ProblemType );
    __command.setCommandParameter ( "PropertyName", PropertyName );
	__command.setCommandParameter ( "PropertyValue", PropertyValue );
	//__command.setCommandParameter ( "Action", Action );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param title Dialog title.
@param command The command to edit.
*/
private void initialize ( JFrame parent, CheckTimeSeriesStatistic_Command command, List<String> tableIDChoices )
{	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Check time series statistic for against criteria (see also the CheckTimeSeries() command, " +
		"which checks data values)." ), 
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The sample is taken from the entire time series." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "A warning will be generated if the statistic matches the specified condition(s)." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Use the WriteCheckFile() command to save the results of all checks." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify dates with precision appropriate for the data, " +
		"use blank for all available data, OutputStart, or OutputEnd."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JPanel tslist_JPanel = new JPanel();
    tslist_JPanel.setBorder(BorderFactory.createTitledBorder (
        BorderFactory.createLineBorder(Color.black),"Time series to process"));
    tslist_JPanel.setLayout( new GridBagLayout() );
    
    int yTslist = 0;
    __TSList_JComboBox = new SimpleJComboBox(false);
    yTslist = CommandEditorUtil.addTSListToEditorDialogPanel ( this, tslist_JPanel, __TSList_JComboBox, yTslist );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yTslist = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, tslist_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, yTslist );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yTslist = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
        this, this, tslist_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, yTslist );
    
    JGUIUtil.addComponent(main_JPanel, tslist_JPanel,
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);
    
    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
     
    // Panel for statistic
    int yStat = -1;
    JPanel statistic_JPanel = new JPanel();
    statistic_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Statistic", statistic_JPanel );
    
    JGUIUtil.addComponent(statistic_JPanel, new JLabel (
        "The following parameters define how to compute the statistic."),
        0, ++yStat, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statistic_JPanel, new JLabel (
        "Currently minimum sample size and number of missing allowed cannot be specified."),
        0, ++yStat, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(statistic_JPanel, new JLabel ( "Statistic to calculate:" ), 
        0, ++yStat, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Statistic_JComboBox = new SimpleJComboBox ( 12, false ); // Do not allow edit
    __Statistic_JComboBox.setData ( TSUtil_CalculateTimeSeriesStatistic.getStatisticChoicesAsStrings() );
    __Statistic_JComboBox.addItemListener ( this );
    //__Statistic_JComboBox.setMaximumRowCount(statisticChoices.size());
    JGUIUtil.addComponent(statistic_JPanel, __Statistic_JComboBox,
        1, yStat, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statistic_JPanel, new JLabel(
        "Required - may require other parameters."), 
        3, yStat, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(statistic_JPanel, new JLabel ( "Statistic value1:" ), 
        0, ++yStat, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __StatisticValue1_JTextField = new JTextField ( 10 );
    __StatisticValue1_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(statistic_JPanel, __StatisticValue1_JTextField,
        1, yStat, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statistic_JPanel, new JLabel(
        "Optional - may be needed as input to calculate statistic."), 
        3, yStat, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(statistic_JPanel, new JLabel ( "Statistic value2:" ), 
        0, ++yStat, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __StatisticValue2_JTextField = new JTextField ( 10 );
    __StatisticValue2_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(statistic_JPanel, __StatisticValue2_JTextField,
        1, yStat, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statistic_JPanel, new JLabel(
        "Optional - may be needed as input to calculate statistic."), 
        3, yStat, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(statistic_JPanel, new JLabel ( "Statistic value3:" ), 
        0, ++yStat, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __StatisticValue3_JTextField = new JTextField ( 10 );
    __StatisticValue3_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(statistic_JPanel, __StatisticValue3_JTextField,
        1, yStat, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statistic_JPanel, new JLabel(
        "Optional - may be needed as input to calculate statistic."), 
        3, yStat, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(statistic_JPanel, new JLabel ( "Analysis start:" ),
        0, ++yStat, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AnalysisStart_JTextField = new JTextField ( "", 20 );
    __AnalysisStart_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(statistic_JPanel, __AnalysisStart_JTextField,
        1, yStat, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statistic_JPanel, new JLabel(
        "Optional - analysis start date/time (default=full time series period)."),
        3, yStat, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(statistic_JPanel, new JLabel ( "Analysis end:" ), 
        0, ++yStat, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AnalysisEnd_JTextField = new JTextField ( "", 20 );
    __AnalysisEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(statistic_JPanel, __AnalysisEnd_JTextField,
        1, yStat, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statistic_JPanel, new JLabel(
        "Optional - analysis end date/time (default=full time series period)."),
        3, yStat, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Panel for table output
    int yOut = -1;
    JPanel out_JPanel = new JPanel();
    out_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output", out_JPanel );
    
    JGUIUtil.addComponent(out_JPanel, new JLabel (
        "The statistic that is calculated can be saved in a table containing columns for the TSID and statistic value."),
        0, ++yOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel (
        "Currently secondary values such as date/time for " + TSStatisticType.MAX + " are not output."),
        0, ++yOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(out_JPanel, new JLabel ( "Table ID for output:" ), 
        0, ++yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true );    // Allow edit
    tableIDChoices.add(0,""); // Add blank to ignore table
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(out_JPanel, __TableID_JComboBox,
        1, yOut, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel(
        "Optional - table to save the statistic."), 
        3, yOut, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(out_JPanel, new JLabel ( "Table TSID column:" ), 
        0, ++yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableTSIDColumn_JTextField = new JTextField ( 10 );
    __TableTSIDColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(out_JPanel, __TableTSIDColumn_JTextField,
        1, yOut, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel( "Required if using table - column name for TSID."), 
        3, yOut, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(out_JPanel, new JLabel("Format of TSID:"),
        0, ++yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableTSIDFormat_JTextField = new TSFormatSpecifiersJPanel(10);
    __TableTSIDFormat_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
    __TableTSIDFormat_JTextField.addKeyListener ( this );
    __TableTSIDFormat_JTextField.setToolTipText("%L for location, %T for data type.");
    JGUIUtil.addComponent(out_JPanel, __TableTSIDFormat_JTextField,
        1, yOut, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel ("Optional - use %L for location, etc. (default=alias or TSID)."),
        3, yOut, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(out_JPanel, new JLabel ( "Table statistic column:" ), 
        0, ++yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableStatisticColumn_JTextField = new JTextField ( 10 );
    __TableStatisticColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(out_JPanel, __TableStatisticColumn_JTextField,
        1, yOut, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel(
        "Required if using table - column name for statistic."), 
        3, yOut, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for check and actions
    int yCheck = -1;
    JPanel check_JPanel = new JPanel();
    check_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Check Criteria and Actions", check_JPanel );
    
    JGUIUtil.addComponent(check_JPanel, new JLabel (
        "The following parameters are used to check the statistic value against a criteria."),
        0, ++yCheck, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel (
        "If the statistic value matches the criteria, then an action can be taken and a property can be set."),
        0, ++yCheck, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel ( "Check criteria:" ), 
        0, ++yCheck, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CheckCriteria_JComboBox = new SimpleJComboBox ( 12, false );    // Do not allow edit
    List<String> checkCriteriaChoices = __command.getCheckCriteriaChoicesAsStrings();
    __CheckCriteria_JComboBox.setData ( checkCriteriaChoices );
    __CheckCriteria_JComboBox.addItemListener ( this );
    __CheckCriteria_JComboBox.setMaximumRowCount(checkCriteriaChoices.size());
    JGUIUtil.addComponent(check_JPanel, __CheckCriteria_JComboBox,
        1, yCheck, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel("Required - may require other parameters."), 
        3, yCheck, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(check_JPanel, new JLabel ( "Check value1:" ), 
		0, ++yCheck, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__CheckValue1_JTextField = new JTextField ( 10 );
	__CheckValue1_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(check_JPanel, __CheckValue1_JTextField,
		1, yCheck, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel(
		"Optional - minimum (or only) statistic value to check."), 
		3, yCheck, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(check_JPanel, new JLabel ( "Check value2:" ), 
        0, ++yCheck, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CheckValue2_JTextField = new JTextField ( 10 );
    __CheckValue2_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(check_JPanel, __CheckValue2_JTextField,
        1, yCheck, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel(
        "Optional - maximum value in range, or other statistic value to check."), 
        3, yCheck, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(check_JPanel, new JLabel ( "Problem type:" ), 
		0, ++yCheck, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ProblemType_JTextField = new JTextField ( 10 );
	__ProblemType_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(check_JPanel, __ProblemType_JTextField,
		1, yCheck, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel(
		"Optional - problem type to use in output (default=Statistic-CheckCriteria)."), 
		3, yCheck, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(check_JPanel,new JLabel("If criteria met?:"),
        0, ++yCheck, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IfCriteriaMet_JComboBox = new SimpleJComboBox ( false );
    List<String> criteriaChoices = new ArrayList<String>();
    criteriaChoices.add ( "" );
    criteriaChoices.add ( __command._Ignore );
    criteriaChoices.add ( __command._Warn );
    criteriaChoices.add ( __command._Fail );
    __IfCriteriaMet_JComboBox.setData(criteriaChoices);
    __IfCriteriaMet_JComboBox.select ( 0 );
    __IfCriteriaMet_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(check_JPanel, __IfCriteriaMet_JComboBox,
        1, yCheck, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel (
        "Optional - should warning/failure be generated (default=" + __command._Warn + ")."),
        3, yCheck, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(check_JPanel, new JLabel ( "Property name:" ), 
        0, ++yCheck, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyName_JTextField = new JTextField ( 20 );
    __PropertyName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(check_JPanel, __PropertyName_JTextField,
        1, yCheck, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel(
        "Optional - name of time series property to set when criteria are met."), 
        3, yCheck, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(check_JPanel, new JLabel ( "Property value:" ), 
        0, ++yCheck, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyValue_JTextField = new JTextField ( 20 );
    __PropertyValue_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(check_JPanel, __PropertyValue_JTextField,
        1, yCheck, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel(
        "Optional - value of time series property to set when criteria are met."), 
        3, yCheck, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    /*
    JGUIUtil.addComponent(check_JPanel, new JLabel ( "Action:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Action_JComboBox = new SimpleJComboBox ( 12, false );    // Do not allow edit
    List<String> actionChoices = new Vector();
    actionChoices.add("");
    actionChoices.add(__command._Remove);
    actionChoices.add(__command._SetMissing);
    __Action_JComboBox.setData ( actionChoices );
    __Action_JComboBox.select(0);
    __Action_JComboBox.addItemListener ( this );
    __Action_JComboBox.setMaximumRowCount(actionChoices.size());
    JGUIUtil.addComponent(check_JPanel, __Action_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel("Optional - action for matched values (default=no action)."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        */

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 55 );
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
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
public void itemStateChanged ( ItemEvent e )
{	checkGUIState();
    refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else {
	    // Combo box...
		refresh();
	}
}

public void keyReleased ( KeyEvent event )
{	refresh();
}

public void keyTyped ( KeyEvent event ) {;}

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
{	String routine = "CheckTimeSeriesStatistic_JDialog.refresh";
    String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String Statistic = "";
    String StatisticValue1 = "";
    String StatisticValue2 = "";
    String StatisticValue3 = "";
    String AnalysisStart = "";
    String AnalysisEnd = "";
    String TableID = "";
    String TableTSIDColumn = "";
    String TableTSIDFormat = "";
    String TableStatisticColumn = "";
    String CheckCriteria = "";
	String CheckValue1 = "";
	String CheckValue2 = "";
	String IfCriteriaMet = "";
	String ProblemType = "";
    String PropertyName = "";
    String PropertyValue = "";
	//String Action = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
        TSList = props.getValue ( "TSList" );
		TSID = props.getValue ( "TSID" );
        EnsembleID = props.getValue ( "EnsembleID" );
        Statistic = props.getValue ( "Statistic" );
        StatisticValue1 = props.getValue ( "StatisticValue1" );
        StatisticValue2 = props.getValue ( "StatisticValue2" );
        StatisticValue3 = props.getValue ( "StatisticValue3" );
        AnalysisStart = props.getValue ( "AnalysisStart" );
        AnalysisEnd = props.getValue ( "AnalysisEnd" );
        TableID = props.getValue ( "TableID" );
        TableTSIDColumn = props.getValue ( "TableTSIDColumn" );
        TableTSIDFormat = props.getValue ( "TableTSIDFormat" );
        TableStatisticColumn = props.getValue ( "TableStatisticColumn" );
        CheckCriteria = props.getValue ( "CheckCriteria" );
		CheckValue1 = props.getValue ( "CheckValue1" );
		CheckValue2 = props.getValue ( "CheckValue2" );
		IfCriteriaMet = props.getValue ( "IfCriteriaMet" );
		ProblemType = props.getValue ( "ProblemType" );
		PropertyName = props.getValue ( "PropertyName" );
		PropertyValue = props.getValue ( "PropertyValue" );
		//Action = props.getValue ( "Action" );
        if ( TSList == null ) {
            // Select default...
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
        if ( StatisticValue1 != null ) {
            __StatisticValue1_JTextField.setText ( StatisticValue1 );
        }
        if ( StatisticValue2 != null ) {
            __StatisticValue2_JTextField.setText ( StatisticValue2 );
        }
        if ( StatisticValue3 != null ) {
            __StatisticValue3_JTextField.setText ( StatisticValue3 );
        }
        if ( AnalysisStart != null ) {
            __AnalysisStart_JTextField.setText( AnalysisStart );
        }
        if ( AnalysisEnd != null ) {
            __AnalysisEnd_JTextField.setText ( AnalysisEnd );
        }
        if ( TableID == null ) {
            // Select default...
            __TableID_JComboBox.select ( 0 );
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
        if ( TableTSIDColumn != null ) {
            __TableTSIDColumn_JTextField.setText ( TableTSIDColumn );
        }
        if (TableTSIDFormat != null ) {
            __TableTSIDFormat_JTextField.setText(TableTSIDFormat.trim());
        }
        if ( TableStatisticColumn != null ) {
            __TableStatisticColumn_JTextField.setText ( TableStatisticColumn );
        }
        if ( CheckCriteria == null ) {
            // Select default...
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
		if ( CheckValue1 != null ) {
			__CheckValue1_JTextField.setText ( CheckValue1 );
		}
        if ( CheckValue2 != null ) {
            __CheckValue2_JTextField.setText ( CheckValue2 );
        }
        if ( __IfCriteriaMet_JComboBox != null ) {
            if ( IfCriteriaMet == null ) {
                // Select default...
                __IfCriteriaMet_JComboBox.select ( "" );
            }
            else {  if (    JGUIUtil.isSimpleJComboBoxItem(
                    __IfCriteriaMet_JComboBox,
                    IfCriteriaMet, JGUIUtil.NONE, null, null ) ) {
                    __IfCriteriaMet_JComboBox.select ( IfCriteriaMet );
                }
                else {  Message.printWarning ( 1, routine,
                    "Existing command references an invalid\n"+
                    "IfCriteriaMet \"" + IfCriteriaMet + "\".  Select a\ndifferent value or Cancel." );
                }
            }
        }
		if ( ProblemType != null ) {
			__ProblemType_JTextField.setText ( ProblemType );
		}
        if ( PropertyName != null ) {
            __PropertyName_JTextField.setText ( PropertyName );
        }
        if ( PropertyValue != null ) {
            __PropertyValue_JTextField.setText ( PropertyValue );
        }
        /*
        if ( Action == null ) {
            // Select default...
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
        }*/
	}
	// Regardless, reset the command from the fields...
    TSList = __TSList_JComboBox.getSelected();
	TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    Statistic = __Statistic_JComboBox.getSelected();
    StatisticValue1 = __StatisticValue1_JTextField.getText().trim();
    StatisticValue2 = __StatisticValue2_JTextField.getText().trim();
    StatisticValue3 = __StatisticValue3_JTextField.getText().trim();
    AnalysisStart = __AnalysisStart_JTextField.getText().trim();
    AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
    TableID = __TableID_JComboBox.getSelected();
    TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
    TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
    TableStatisticColumn = __TableStatisticColumn_JTextField.getText().trim();
    CheckCriteria = __CheckCriteria_JComboBox.getSelected();
    CheckValue2 = __CheckValue2_JTextField.getText().trim();
	CheckValue1 = __CheckValue1_JTextField.getText().trim();
    IfCriteriaMet = __IfCriteriaMet_JComboBox.getSelected();
	ProblemType = __ProblemType_JTextField.getText().trim();
	PropertyName = __PropertyName_JTextField.getText().trim();
    PropertyValue = __PropertyValue_JTextField.getText().trim();
	//Action = __Action_JComboBox.getSelected();
	props = new PropList ( __command.getCommandName() );
    props.add ( "TSList=" + TSList );
	props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
    props.add ( "Statistic=" + Statistic );
    props.add ( "StatisticValue1=" + StatisticValue1 );
    props.add ( "StatisticValue2=" + StatisticValue2 );
    props.add ( "StatisticValue3=" + StatisticValue3 );
    props.add ( "AnalysisStart=" + AnalysisStart );
    props.add ( "AnalysisEnd=" + AnalysisEnd );
    props.add ( "TableID=" + TableID );
    props.add ( "TableTSIDColumn=" + TableTSIDColumn );
    props.add ( "TableTSIDFormat=" + TableTSIDFormat );
    props.add ( "TableStatisticColumn=" + TableStatisticColumn );
    // Have to set in such a way that = at start of CheckCriteria does not foul up the method
    props.set ( "CheckCriteria", CheckCriteria );
    props.add ( "CheckValue1=" + CheckValue1 );
    props.add ( "CheckValue2=" + CheckValue2 );
    props.add ( "IfCriteriaMet=" + IfCriteriaMet );
	props.add ( "ProblemType=" + ProblemType );
	props.add ( "PropertyName=" + PropertyName );
	props.add ( "PropertyValue=" + PropertyValue );
	//props.add ( "Action=" + Action );
	__command_JTextArea.setText( __command.toString ( props ) );
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
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
