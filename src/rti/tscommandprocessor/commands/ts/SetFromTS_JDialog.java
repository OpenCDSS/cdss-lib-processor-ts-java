// SetFromTS_JDialog - Editor dialog for the SetFromTS() command.

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

import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
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

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import RTi.TS.TSUtil;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.DateTime_JPanel;
import RTi.Util.Time.TimeInterval;

/**
Editor dialog for the SetFromTS() command.
*/
@SuppressWarnings("serial")
public class SetFromTS_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, ListSelectionListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SetFromTS_Command __command = null;
private JTabbedPane __main_JTabbedPane = null;
private JTextArea __command_JTextArea=null;
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private SimpleJComboBox __IndependentTSList_JComboBox = null;
private JLabel __IndependentTSID_JLabel = null;
private SimpleJComboBox __IndependentTSID_JComboBox = null;
private JLabel __IndependentEnsembleID_JLabel = null;
private SimpleJComboBox __IndependentEnsembleID_JComboBox = null;
private JTextField __SetStart_JTextField;
private JTextField __SetEnd_JTextField;
private JCheckBox __SetWindow_JCheckBox = null;
private DateTime_JPanel __SetWindowStart_JPanel = null;
private DateTime_JPanel __SetWindowEnd_JPanel = null;
private JTextField __SetWindowStart_JTextField = null; // Used for properties
private JTextField __SetWindowEnd_JTextField = null;
private SimpleJComboBox	__TransferHow_JComboBox =null;
private SimpleJComboBox __HandleMissingHow_JComboBox = null;
private SimpleJComboBox __SetDataFlags_JComboBox = null;
private JTextField __SetFlag_JTextField = null;
private JTextField __SetFlagDesc_JTextField = null;
private SimpleJComboBox __RecalcLimits_JComboBox = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public SetFromTS_JDialog ( JFrame parent, SetFromTS_Command command )
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
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "SetFromTS");
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

/**
Check the GUI state to make sure that appropriate components are enabled/disabled.
*/
private void checkGUIState ()
{
    String TSList = __TSList_JComboBox.getSelected();
    if ( TSListType.ALL_MATCHING_TSID.equals(TSList) ||
            TSListType.FIRST_MATCHING_TSID.equals(TSList) ||
            TSListType.LAST_MATCHING_TSID.equals(TSList)) {
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
    
    // Independent...
    
    String IndependentTSList = __IndependentTSList_JComboBox.getSelected();
    if ( TSListType.ALL_MATCHING_TSID.equals(IndependentTSList) ) {
        __IndependentTSID_JComboBox.setEnabled(true);
        __IndependentTSID_JLabel.setEnabled ( true );
    }
    else {
        __IndependentTSID_JComboBox.setEnabled(false);
        __IndependentTSID_JLabel.setEnabled ( false );
    }
    if ( TSListType.ENSEMBLE_ID.equals(IndependentTSList)) {
        __IndependentEnsembleID_JComboBox.setEnabled(true);
        __IndependentEnsembleID_JLabel.setEnabled ( true );
    }
    else {
        __IndependentEnsembleID_JComboBox.setEnabled(false);
        __IndependentEnsembleID_JLabel.setEnabled ( false );
    }
    if ( __SetWindow_JCheckBox.isSelected() ) {
        // Checked so enable the date panels
        __SetWindowStart_JPanel.setEnabled ( true );
        __SetWindowEnd_JPanel.setEnabled ( true );
        __SetWindowStart_JTextField.setEnabled ( true );
        __SetWindowEnd_JTextField.setEnabled ( true );
    }
    else {
        __SetWindowStart_JPanel.setEnabled ( false );
        __SetWindowEnd_JPanel.setEnabled ( false );
        __SetWindowStart_JTextField.setEnabled ( false );
        __SetWindowEnd_JTextField.setEnabled ( false );
    }
}

/**
Check the user input for errors and set __error_wait accordingly.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
    PropList props = new PropList ( "" );
    String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String IndependentTSList = __IndependentTSList_JComboBox.getSelected();
    String IndependentTSID = __IndependentTSID_JComboBox.getSelected();
    String IndependentEnsembleID = __IndependentEnsembleID_JComboBox.getSelected();
    String SetStart = __SetStart_JTextField.getText().trim();
    String SetEnd = __SetEnd_JTextField.getText().trim();
    String TransferHow = __TransferHow_JComboBox.getSelected();
    String HandleMissingHow = __HandleMissingHow_JComboBox.getSelected();
    String SetDataFlags = __SetDataFlags_JComboBox.getSelected();
	String SetFlag = __SetFlag_JTextField.getText().trim();
    String SetFlagDesc = __SetFlagDesc_JTextField.getText().trim();
    String RecalcLimits = __RecalcLimits_JComboBox.getSelected();
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
    if ( IndependentTSList.length() > 0 ) {
        props.set ( "IndependentTSList", IndependentTSList );
    }
    if ( IndependentTSID.length() > 0 ) {
        props.set ( "IndependentTSID", IndependentTSID );
    }
    if ( IndependentEnsembleID.length() > 0 ) {
        props.set ( "IndependentEnsembleID", IndependentEnsembleID );
    }
    if ( SetStart.length() > 0 ) {
        props.set ( "SetStart", SetStart );
    }
    if ( SetEnd.length() > 0 ) {
        props.set ( "SetEnd", SetEnd );
    }
    if ( TransferHow.length() > 0 ) {
        props.set ( "TransferHow", TransferHow );
    }
    if ( HandleMissingHow.length() > 0 ) {
        props.set ( "HandleMissingHow", HandleMissingHow );
    }
    if ( (SetDataFlags != null) && (SetDataFlags.length() > 0) ) {
        props.set ( "SetDataFlags", SetDataFlags );
    }
	if ( SetFlag.length() > 0 ) {
		props.set ( "SetFlag", SetFlag );
	}
    if ( SetFlagDesc.length() > 0 ) {
        props.set ( "SetFlagDesc", SetFlagDesc );
    }
    if ( RecalcLimits.length() > 0 ) {
        props.set( "RecalcLimits", RecalcLimits );
    }
    if ( __SetWindow_JCheckBox.isSelected() ){
        String SetWindowStart = __SetWindowStart_JPanel.toString(false,true).trim();
        String SetWindowEnd = __SetWindowEnd_JPanel.toString(false,true).trim();
        // 99 is used for month if not specified - don't want that in the parameters
        if ( SetWindowStart.startsWith("99") ) {
            SetWindowStart = "";
        }
        if ( SetWindowEnd.startsWith("99") ) {
            SetWindowEnd = "";
        }
        // This will override the above
        String SetWindowStart2 = __SetWindowStart_JTextField.getText().trim();
        String SetWindowEnd2 = __SetWindowEnd_JTextField.getText().trim();
        if ( !SetWindowStart2.isEmpty() ) {
            SetWindowStart = SetWindowStart2;
        }
        if ( !SetWindowEnd2.isEmpty() ) {
            SetWindowEnd = SetWindowEnd2;
        }
        if ( !SetWindowStart.isEmpty() ) {
            props.set ( "SetWindowStart", SetWindowStart );
        }
        if ( SetWindowEnd.isEmpty() ) {
            props.set ( "SetWindowEnd", SetWindowEnd );
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
    String IndependentTSList = __IndependentTSList_JComboBox.getSelected();
    String IndependentTSID = __IndependentTSID_JComboBox.getSelected();
    String IndependentEnsembleID = __IndependentEnsembleID_JComboBox.getSelected(); 
    String SetStart = __SetStart_JTextField.getText().trim();
    String SetEnd = __SetEnd_JTextField.getText().trim();
    String TransferHow = __TransferHow_JComboBox.getSelected();
    String HandleMissingHow = __HandleMissingHow_JComboBox.getSelected();
    String SetDataFlags = __SetDataFlags_JComboBox.getSelected();
	String SetFlag = __SetFlag_JTextField.getText().trim();
    String SetFlagDesc = __SetFlagDesc_JTextField.getText().trim();
    String RecalcLimits = __RecalcLimits_JComboBox.getSelected();
    __command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "IndependentTSList", IndependentTSList );
    __command.setCommandParameter ( "IndependentTSID", IndependentTSID );
    __command.setCommandParameter ( "IndependentEnsembleID", IndependentEnsembleID );
    __command.setCommandParameter ( "SetStart", SetStart );
    __command.setCommandParameter ( "SetEnd", SetEnd );
    __command.setCommandParameter ( "TransferHow", TransferHow );
    __command.setCommandParameter ( "HandleMissingHow", HandleMissingHow );
    __command.setCommandParameter ( "SetDataFlags", SetDataFlags );
    __command.setCommandParameter ( "SetFlag", SetFlag );
    __command.setCommandParameter ( "SetFlagDesc", SetFlagDesc );
    __command.setCommandParameter ( "RecalcLimits", RecalcLimits );
    if ( __SetWindow_JCheckBox.isSelected() ){
        String SetWindowStart = __SetWindowStart_JPanel.toString(false,true).trim();
        String SetWindowEnd = __SetWindowEnd_JPanel.toString(false,true).trim();
        if ( SetWindowStart.startsWith("99") ) {
            SetWindowStart = "";
        }
        if ( SetWindowEnd.startsWith("99") ) {
            SetWindowEnd = "";
        }
        String SetWindowStart2 = __SetWindowStart_JTextField.getText().trim();
        String SetWindowEnd2 = __SetWindowEnd_JTextField.getText().trim();
        if ( !SetWindowStart2.isEmpty() ) {
        	SetWindowStart = SetWindowStart2;
        }
        if ( !SetWindowEnd2.isEmpty() ) {
        	SetWindowEnd = SetWindowEnd2;
        }
        __command.setCommandParameter ( "SetWindowStart", SetWindowStart );
        __command.setCommandParameter ( "SetWindowEnd", SetWindowEnd );
    }
    else {
    	// Clear the properties because they may have been set during editing but should not be propagated
    	__command.getCommandParameters().unSet ( "SetWindowStart" );
    	__command.getCommandParameters().unSet ( "SetWindowEnd" );
    }
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, SetFromTS_Command command )
{   __command = command;

	addWindowListener( this );

	Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

	// Main contents...

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Copy data values from the independent time series to replace values in the dependent time series." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"All data values (by default including missing data) in the set period will be copied."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "If one independent time series is specified, it will be used for all dependent time series."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "If multiple independent time series are specified (e.g., for ensembles)," +
        " the same number of dependent time series must be specified."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel for input time series
    int yts = -1;
    JPanel ts_JPanel = new JPanel();
    ts_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Time series", ts_JPanel );

    __TSList_JComboBox = new SimpleJComboBox(false);
    yts = CommandEditorUtil.addTSListToEditorDialogPanel (
        this, ts_JPanel, new JLabel ("Dependent TS List:"), __TSList_JComboBox, yts );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __TSID_JComboBox.setToolTipText("Select a dependent time series TSID/alias from the list or specify with ${Property} notation");
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yts = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, ts_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, yts );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __EnsembleID_JComboBox.setToolTipText("Select a dependent ensemble identifier from the list or specify with ${Property} notation");
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yts = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
            this, this, ts_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, yts );
    
    __IndependentTSList_JComboBox = new SimpleJComboBox(false);
    yts = CommandEditorUtil.addTSListToEditorDialogPanel (
            this, ts_JPanel, new JLabel ("Independent TS List:"), __IndependentTSList_JComboBox, yts );

    __IndependentTSID_JLabel = new JLabel (
            "Independent TSID (for Independent TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __IndependentTSID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __IndependentTSID_JComboBox.setToolTipText("Select an independent time series TSID/alias from the list or specify with ${Property} notation");
    List<String> tsids2 = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yts = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, ts_JPanel, __IndependentTSID_JLabel, __IndependentTSID_JComboBox, tsids2, yts );
    
    __IndependentEnsembleID_JLabel = new JLabel (
            "Independent EnsembleID (for Independent TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __IndependentEnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __IndependentEnsembleID_JComboBox.setToolTipText("Select an independent ensemble identifier from the list or specify with ${Property} notation");
    yts = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
            this, this, ts_JPanel, __IndependentEnsembleID_JLabel, __IndependentEnsembleID_JComboBox, EnsembleIDs, yts );

    // Panel for period and window
    int yTime = -1;
    JPanel time_JPanel = new JPanel();
    time_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Period and Window", time_JPanel );
    
    JGUIUtil.addComponent(time_JPanel, new JLabel (
		"Specify date/times with precision appropriate for the data." ),
		0, ++yTime, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(time_JPanel, new JLabel (
		"The set period is for the independent time series."),
		0, ++yTime, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(time_JPanel, new JLabel (
		"The set window can be specified using the provided choices or with processor ${Property}."),
		0, ++yTime, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(time_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
    	0, ++yTime, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(time_JPanel, new JLabel ("Set start:"), 
        0, ++yTime, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SetStart_JTextField = new JTextField (20);
    __SetStart_JTextField.setToolTipText("Specify the set start using a date/time string or ${Property} notation");
    __SetStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(time_JPanel, __SetStart_JTextField,
        1, yTime, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(time_JPanel, new JLabel ( "Optional - set start, can use ${Property} (default is full period)."),
        3, yTime, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(time_JPanel, new JLabel ( "Set End:"), 
        0, ++yTime, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SetEnd_JTextField = new JTextField (20);
    __SetEnd_JTextField.setToolTipText("Specify the set end using a date/time string or ${Property} notation");
    __SetEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(time_JPanel, __SetEnd_JTextField,
        1, yTime, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(time_JPanel, new JLabel ( "Optional - set end, can use ${Property} (default is full period)."),
        3, yTime, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    __SetWindow_JCheckBox = new JCheckBox ( "Set window:", false );
    __SetWindow_JCheckBox.addActionListener ( this );
    JGUIUtil.addComponent(time_JPanel, __SetWindow_JCheckBox, 
        0, ++yTime, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    JPanel setWindow_JPanel = new JPanel();
    setWindow_JPanel.setLayout(new GridBagLayout());
    __SetWindowStart_JPanel = new DateTime_JPanel ( "Start", TimeInterval.MONTH, TimeInterval.MINUTE, null );
    __SetWindowStart_JPanel.addActionListener(this);
    __SetWindowStart_JPanel.addKeyListener ( this );
    JGUIUtil.addComponent(setWindow_JPanel, __SetWindowStart_JPanel,
        1, 0, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    // TODO SAM 2008-01-23 Figure out how to display the correct limits given the time series interval
    __SetWindowEnd_JPanel = new DateTime_JPanel ( "End", TimeInterval.MONTH, TimeInterval.MINUTE, null );
    __SetWindowEnd_JPanel.addActionListener(this);
    __SetWindowEnd_JPanel.addKeyListener ( this );
    JGUIUtil.addComponent(setWindow_JPanel, __SetWindowEnd_JPanel,
        4, 0, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(time_JPanel, setWindow_JPanel,
        1, yTime, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(time_JPanel, new JLabel(
        "Optional - window within output year to set data (default=full year)."),
        3, yTime, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(time_JPanel,new JLabel( "Set window start ${Property}:"),
        0, ++yTime, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SetWindowStart_JTextField = new JTextField ( "", 20 );
    __SetWindowStart_JTextField.setToolTipText("Specify the output window start ${Property} - will override the above.");
    __SetWindowStart_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(time_JPanel, __SetWindowStart_JTextField,
        1, yTime, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(time_JPanel, new JLabel ( "Optional (default=full year)."),
        3, yTime, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(time_JPanel,new JLabel("Set window end ${Property}:"),
        0, ++yTime, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SetWindowEnd_JTextField = new JTextField ( "", 20 );
    __SetWindowEnd_JTextField.setToolTipText("Specify the set window end ${Property} - will override the above.");
    __SetWindowEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(time_JPanel, __SetWindowEnd_JTextField,
        1, yTime, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(time_JPanel, new JLabel ( "Optional (default=full year)."),
        3, yTime, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    // Panel for set data
    int ySet = -1;
    JPanel set_JPanel = new JPanel();
    set_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Set Control", set_JPanel );
    
    JGUIUtil.addComponent(set_JPanel, new JLabel (
		"The following parameters indicate how to set the data."),
		0, ++ySet, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(set_JPanel, new JLabel (
		"If the data should be treated as original data, the time series limits can be recalculated (can be used for filling later)."),
		0, ++ySet, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(set_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
    	0, ++ySet, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
	JGUIUtil.addComponent(set_JPanel, new JLabel ( "Transfer data how:" ), 
		0, ++ySet, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TransferHow_JComboBox = new SimpleJComboBox ( false );
	List<String> transferChoices = new ArrayList<String>();
	transferChoices.add ( TSUtil.TRANSFER_BYDATETIME );
	transferChoices.add ( TSUtil.TRANSFER_SEQUENTIALLY );
	__TransferHow_JComboBox.setData(transferChoices);
	__TransferHow_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(set_JPanel, __TransferHow_JComboBox,
		1, ySet, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(set_JPanel, new JLabel(
        "Required - how are data values transferred?"), 
        3, ySet, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(set_JPanel, new JLabel ( "Handle missing data how?:" ), 
        0, ++ySet, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __HandleMissingHow_JComboBox = new SimpleJComboBox ( false );
	List<String> missingChoices = new ArrayList<String>();
	missingChoices.add ( "" );
	missingChoices.add ( __command._IgnoreMissing );
	missingChoices.add ( __command._SetMissing );
	missingChoices.add ( __command._SetOnlyMissingValues );
	__HandleMissingHow_JComboBox.setData(missingChoices);
    __HandleMissingHow_JComboBox.select ( 0 );
    __HandleMissingHow_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(set_JPanel, __HandleMissingHow_JComboBox,
        1, ySet, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(set_JPanel, new JLabel( "Optional - missing in independent handled how? (default=" +
        __command._SetMissing + ")."), 
        3, ySet, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(set_JPanel, new JLabel("Set data flags?:"),
        0, ++ySet, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SetDataFlags_JComboBox = new SimpleJComboBox ( false );
    List<String> choices = new Vector<String>();
    choices.add ("");
    choices.add ( __command._False );
    choices.add ( __command._True );
    __SetDataFlags_JComboBox.setData ( choices );
    __SetDataFlags_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(set_JPanel, __SetDataFlags_JComboBox,
        1, ySet, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(set_JPanel, new JLabel(
        "Optional - should data flags be copied (default=" + __command._True + ")."), 
        3, ySet, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(set_JPanel, new JLabel ( "Set flag:" ), 
		0, ++ySet, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__SetFlag_JTextField = new JTextField ( 10 );
	__SetFlag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(set_JPanel, __SetFlag_JTextField,
		1, ySet, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(set_JPanel, new JLabel(
		"Optional - string to flag set values."), 
		3, ySet, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(set_JPanel, new JLabel ( "Set flag description:" ), 
        0, ++ySet, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SetFlagDesc_JTextField = new JTextField ( 15 );
    __SetFlagDesc_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(set_JPanel, __SetFlagDesc_JTextField,
        1, ySet, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(set_JPanel, new JLabel( "Optional - description for set flag."), 
        3, ySet, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(set_JPanel, new JLabel ( "Recalculate limits:"), 
        0, ++ySet, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __RecalcLimits_JComboBox = new SimpleJComboBox ( false );
    List<String>recalcChoices = new ArrayList<String>();
    recalcChoices.add ( "" );
    recalcChoices.add ( __command._False );
    recalcChoices.add ( __command._True );
    __RecalcLimits_JComboBox.setData(recalcChoices);
    __RecalcLimits_JComboBox.select ( 0 );
    __RecalcLimits_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(set_JPanel, __RecalcLimits_JComboBox,
        1, ySet, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(set_JPanel,
        new JLabel( "Optional - recalculate original data limits after set (default=" + __command._False + ")."), 
        3, ySet, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

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
	refresh();
	checkGUIState(); // To make sure SetWindow components are ok

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

	if ( code == KeyEvent.VK_ENTER ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( false );
		}
	}
}

public void keyReleased ( KeyEvent event )
{	refresh();
}

public void keyTyped ( KeyEvent event )
{
}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user cancelled.
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
    String IndependentTSList = "";
    String IndependentTSID = "";
    String IndependentEnsembleID = "";
    String TransferHow = "";
    String SetStart = "";
    String SetEnd = "";
    String SetWindowStart = "";
    String SetWindowEnd = "";
    String HandleMissingHow = "";
    String SetDataFlags = "";
    String SetFlag = "";
    String SetFlagDesc = "";
    String RecalcLimits = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        TSList = props.getValue ( "TSList" );
        TSID = props.getValue ( "TSID" );
        EnsembleID = props.getValue ( "EnsembleID" );
        IndependentTSList = props.getValue ( "IndependentTSList" );
        IndependentTSID = props.getValue ( "IndependentTSID" );
        IndependentEnsembleID = props.getValue ( "IndependentEnsembleID" );
        SetStart = props.getValue ( "SetStart" );
        SetEnd = props.getValue ( "SetEnd" );
        SetWindowStart = props.getValue ( "SetWindowStart" );
        SetWindowEnd = props.getValue ( "SetWindowEnd" );
        TransferHow = props.getValue ( "TransferHow" );
        HandleMissingHow = props.getValue ( "HandleMissingHow" );
        SetDataFlags = props.getValue ( "SetDataFlags" );
        SetFlag = props.getValue ( "SetFlag" );
        SetFlagDesc = props.getValue ( "SetFlagDesc" );
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
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nEnsembleID value \"" + EnsembleID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( IndependentTSList == null ) {
            // Select default...
            __IndependentTSList_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __IndependentTSList_JComboBox,IndependentTSList, JGUIUtil.NONE, null, null ) ) {
                __IndependentTSList_JComboBox.select ( IndependentTSList );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nIndependentTSList value \"" + IndependentTSList +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem( __IndependentTSID_JComboBox, IndependentTSID,
            JGUIUtil.NONE, null, null ) ) {
            __IndependentTSID_JComboBox.select ( IndependentTSID );
        }
        else {
            // Automatically add to the list after the blank...
            if ( (IndependentTSID != null) && (IndependentTSID.length() > 0) ) {
                __IndependentTSID_JComboBox.insertItemAt ( IndependentTSID, 1 );
                // Select...
                __IndependentTSID_JComboBox.select ( IndependentTSID );
            }
            else {
                // Select the blank...
                __IndependentTSID_JComboBox.select ( 0 );
            }
        }
        if ( IndependentEnsembleID == null ) {
            // Select default...
            __IndependentEnsembleID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __IndependentEnsembleID_JComboBox,IndependentEnsembleID, JGUIUtil.NONE, null, null ) ) {
                __IndependentEnsembleID_JComboBox.select ( IndependentEnsembleID );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nIndependentEnsembleID value \"" + IndependentEnsembleID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( SetStart != null ) {
			__SetStart_JTextField.setText (	SetStart );
        }
        if ( SetEnd != null ) {
			__SetEnd_JTextField.setText ( SetEnd );
		}
        if ( (SetWindowStart != null) && (SetWindowStart.length() > 0) ) {
        	if ( SetWindowStart.indexOf("${") >= 0 ) {
        		__SetWindowStart_JTextField.setText ( SetWindowStart );
        	}
        	else {
	            try {
	                // Add year because it is not part of the parameter value...
	                DateTime SetWindowStart_DateTime = DateTime.parse ( "0000-" + SetWindowStart );
	                Message.printStatus(2, routine, "Setting window start to " + SetWindowStart_DateTime );
	                __SetWindowStart_JPanel.setDateTime ( SetWindowStart_DateTime );
	            }
	            catch ( Exception e ) {
	                Message.printWarning( 1, routine, "SetWindowStart (" + SetWindowStart + ") is not a valid date/time." );
	            }
        	}
        }
        if ( (SetWindowEnd != null) && (SetWindowEnd.length() > 0) ) {
        	if ( SetWindowEnd.indexOf("${") >= 0 ) {
        		__SetWindowEnd_JTextField.setText ( SetWindowEnd );
        	}
        	else {
	            try {
	                // Add year because it is not part of the parameter value...
	                DateTime SetWindowEnd_DateTime = DateTime.parse ( "0000-" + SetWindowEnd );
	                Message.printStatus(2, routine, "Setting window end to " + SetWindowEnd_DateTime );
	                __SetWindowEnd_JPanel.setDateTime ( SetWindowEnd_DateTime );
	            }
	            catch ( Exception e ) {
	                Message.printWarning( 1, routine, "SetWindowEnd (" + SetWindowEnd + ") is not a valid date/time." );
	            }
        	}
        }
        if ( (SetWindowStart != null) && !SetWindowStart.isEmpty() &&
            (SetWindowEnd != null) && !SetWindowEnd.isEmpty() ) {
            __SetWindow_JCheckBox.setSelected ( true );
        }
        else {
            __SetWindow_JCheckBox.setSelected ( false );
        }
        if ( TransferHow == null ) {
            // Select default...
            __TransferHow_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TransferHow_JComboBox,TransferHow, JGUIUtil.NONE, null, null ) ) {
                __TransferHow_JComboBox.select ( TransferHow );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTransferHow value \"" + TransferHow +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( HandleMissingHow == null ) {
            // Select default...
            __HandleMissingHow_JComboBox.select ( 0 );
        }
        else {
        	if ( JGUIUtil.isSimpleJComboBoxItem(
                __HandleMissingHow_JComboBox, HandleMissingHow, JGUIUtil.NONE, null, null )) {
                __HandleMissingHow_JComboBox.select ( HandleMissingHow );
            }
            else {
            	Message.printWarning ( 1, routine,
                "Existing command references an invalid\n" +
                "HandleMissingHow value \"" + HandleMissingHow +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( SetDataFlags == null ) {
            // Select default...
            __SetDataFlags_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __SetDataFlags_JComboBox, SetDataFlags, JGUIUtil.NONE, null, null ) ) {
                __SetDataFlags_JComboBox.select ( SetDataFlags );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nSetDataFlags value \"" +
                SetDataFlags + "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( SetFlag != null ) {
			__SetFlag_JTextField.setText ( SetFlag );
        }
        if ( SetFlagDesc != null ) {
			__SetFlagDesc_JTextField.setText ( SetFlagDesc );
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
            	Message.printWarning ( 1, routine,
                "Existing command references an invalid\n" +
                "RecalcLimits value \"" + RecalcLimits +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
	}
    // Regardless, reset the command from the fields...
    TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    IndependentTSList = __IndependentTSList_JComboBox.getSelected();
    IndependentTSID = __IndependentTSID_JComboBox.getSelected();
    IndependentEnsembleID = __IndependentEnsembleID_JComboBox.getSelected();
    SetStart = __SetStart_JTextField.getText().trim();
    SetEnd = __SetEnd_JTextField.getText().trim();
    TransferHow = __TransferHow_JComboBox.getSelected();
    HandleMissingHow = __HandleMissingHow_JComboBox.getSelected();
    SetDataFlags = __SetDataFlags_JComboBox.getSelected();
    SetFlag = __SetFlag_JTextField.getText().trim();
    SetFlagDesc = __SetFlagDesc_JTextField.getText().trim();
    RecalcLimits = __RecalcLimits_JComboBox.getSelected();
    props = new PropList ( __command.getCommandName() );
    props.add ( "TSList=" + TSList );
    props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
    props.add ( "IndependentTSList=" + IndependentTSList );
    props.add ( "IndependentTSID=" + IndependentTSID );
    props.add ( "IndependentEnsembleID=" + IndependentEnsembleID );
    props.add ( "SetStart=" + SetStart );
    props.add ( "SetEnd=" + SetEnd );
    props.add ( "TransferHow=" + TransferHow );
    props.add ( "HandleMissingHow=" + HandleMissingHow );
    props.add ( "SetDataFlags=" + SetDataFlags );
    props.add ( "SetFlag=" + SetFlag );
    props.add ( "SetFlagDesc=" + SetFlagDesc );
    props.add ( "RecalcLimits=" + RecalcLimits);
    if ( __SetWindow_JCheckBox.isSelected() ) {
        SetWindowStart = __SetWindowStart_JPanel.toString(false,true).trim();
        if ( SetWindowStart.startsWith("99") ) {
        	// 99 is used as placeholder when month is not set... artifact of setting choices to blank during editing
        	SetWindowStart = "";
        }
        SetWindowEnd = __SetWindowEnd_JPanel.toString(false,true).trim();
        if ( SetWindowEnd.startsWith("99") ) {
        	// 99 is used as placeholder when month is not set... artifact of setting choices to blank during editing
        	SetWindowEnd = "";
        }
        String SetWindowStart2 = __SetWindowStart_JTextField.getText().trim();
        String SetWindowEnd2 = __SetWindowEnd_JTextField.getText().trim();
        if ( (SetWindowStart2 != null) && !SetWindowStart2.isEmpty() ) {
        	SetWindowStart = SetWindowStart2;
        }
        if ( (SetWindowEnd2 != null) && !SetWindowEnd2.isEmpty() ) {
        	SetWindowEnd = SetWindowEnd2;
        }
        props.add ( "SetWindowStart=" + SetWindowStart );
        props.add ( "SetWindowEnd=" + SetWindowEnd );
    }
    __command_JTextArea.setText( __command.toString ( props ) );
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
Handle ListSelectionListener events.
*/
public void valueChanged ( ListSelectionEvent e )
{	refresh ();
}

/**
Responds to WindowEvents.
@param event WindowEvent object
*/
public void windowClosing( WindowEvent event )
{	response ( false );
}

public void windowActivated( WindowEvent evt )
{
}

public void windowClosed( WindowEvent evt )
{
}

public void windowDeactivated( WindowEvent evt )
{
}

public void windowDeiconified( WindowEvent evt )
{
}

public void windowIconified( WindowEvent evt )
{
}

public void windowOpened( WindowEvent evt )
{
}

}
