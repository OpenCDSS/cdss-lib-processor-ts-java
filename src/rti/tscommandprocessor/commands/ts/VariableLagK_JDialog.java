package rti.tscommandprocessor.commands.ts;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.TS.TSIdent;
import RTi.TS.TSIdent_JDialog;

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
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Time.TimeInterval;

/**
Editor for the VariableLagK command.
*/
public class VariableLagK_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private JFrame __parent_JFrame = null;
private VariableLagK_Command __command = null;
private JTabbedPane __main_JTabbedPane = null;
private JTextArea __command_JTextArea=null;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JTextArea __NewTSID_JTextArea = null;
private SimpleJButton __edit_JButton = null;
private SimpleJButton __clear_JButton = null;
private JTextArea __Lag_JTextArea = null;
private JTextArea __K_JTextArea = null;
private JTextField __OutputStart_JTextField = null;
private JTextField __OutputEnd_JTextField = null;
private JTextField __FlowUnits_JTextField = null;
private SimpleJComboBox __LagInterval_JComboBox = null;
private SimpleJComboBox __InitializeStatesFromTable_JComboBox = null;
private JTextField __InitialLaggedInflow_JTextField = null;
private JTextField __InitialOutflow_JTextField = null;
private JTextField __InitialStorage_JTextField = null;
private JTextArea __InitialQTLag_JTextArea = null;
private SimpleJComboBox __StateTableID_JComboBox = null;
private JTextField __StateTableObjectIDColumn_JTextField = null;
private JTextField __StateTableObjectID_JTextField = null;
private JTextField __StateTableDateTimeColumn_JTextField = null;
private JTextField __StateTableNameColumn_JTextField = null;
private JTextField __StateTableValueColumn_JTextField = null;
private JTextField __StateTableStateName_JTextField = null;
private JTextField __StateSaveDateTime_JTextField = null;
private SimpleJComboBox __StateSaveInterval_JComboBox = null;

private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Whether OK has been pressed.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param StateTableIDChoices choices for StateTableID value.
*/
public VariableLagK_JDialog ( JFrame parent, VariableLagK_Command command, List<String> StateTableIDChoices )
{	super(parent, true);
	initialize ( parent, command, StateTableIDChoices );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();
    String routine = getClass().getSimpleName() + ".actionPerformed";

	if ( o == __cancel_JButton ) {
		response ( false );
	}
    else if ( o == __clear_JButton ) {
		__NewTSID_JTextArea.setText ( "" );
        refresh();
	}
	else if ( o == __edit_JButton ) {
		// Edit the NewTSID in the dialog.  It is OK for the string to be blank.
		String NewTSID = __NewTSID_JTextArea.getText().trim();
		TSIdent tsident;
		try {	if ( NewTSID.length() == 0 ) {
				tsident = new TSIdent();
			}
			else {	tsident = new TSIdent ( NewTSID );
			}
			TSIdent tsident2=(new TSIdent_JDialog ( __parent_JFrame, true, tsident, null )).response();
			if ( tsident2 != null ) {
				__NewTSID_JTextArea.setText (tsident2.toString(true) );
				refresh();
			}
		}
		catch ( Exception e ) {
			Message.printWarning ( 1, routine, "Error creating time series identifier from \"" + NewTSID + "\"." );
			Message.printWarning ( 3, routine, e );
		}
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
Check the state of the dialog, disabling/enabling components as appropriate.
*/
private void checkGUIState()
{	// TODO SAM 2005-09-08 - Evaluate need
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	String Alias = __Alias_JTextField.getText().trim();
	String TSID = __TSID_JComboBox.getSelected();
    String NewTSID = __NewTSID_JTextArea.getText().trim();
	String Lag = __Lag_JTextArea.getText().trim();
	String K = __K_JTextArea.getText().trim();
	String OutputStart = __OutputStart_JTextField.getText().trim();
	String OutputEnd = __OutputEnd_JTextField.getText().trim();
    String FlowUnits = __FlowUnits_JTextField.getText().trim();
    String LagInterval = __LagInterval_JComboBox.getSelected().trim();
    String InitializeStatesFromTable = __InitializeStatesFromTable_JComboBox.getSelected().trim();
    String InitialLaggedInflow = __InitialLaggedInflow_JTextField.getText().trim();
	String InitialOutflow = __InitialOutflow_JTextField.getText().trim();
	String InitialStorage = __InitialStorage_JTextField.getText().trim();
	String InitialQTLag = __InitialQTLag_JTextArea.getText().trim();
	String StateTableID = __StateTableID_JComboBox.getSelected();
	String StateTableObjectIDColumn = __StateTableObjectIDColumn_JTextField.getText().trim();
	String StateTableObjectID = __StateTableObjectID_JTextField.getText().trim();
	String StateTableDateTimeColumn = __StateTableDateTimeColumn_JTextField.getText().trim();
	String StateTableNameColumn = __StateTableNameColumn_JTextField.getText().trim();
	String StateTableValueColumn = __StateTableValueColumn_JTextField.getText().trim();
	String StateTableStateName = __StateTableStateName_JTextField.getText().trim();
	String StateSaveDateTime = __StateSaveDateTime_JTextField.getText().trim();
	String StateSaveInterval = __StateSaveInterval_JComboBox.getSelected();
	__error_wait = false;

	if ( Alias.length() > 0 ) {
		props.set ( "Alias", Alias );
	}
	if ( (TSID != null) && (TSID.length() > 0) ) {
		props.set ( "TSID", TSID );
	}
	if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
		props.set ( "NewTSID", NewTSID );
	}
	if ( (Lag != null) && (Lag.length() > 0) ) {
		props.set ( "Lag", Lag );
	}
	if ( (K != null) && (K.length() > 0) ) {
		props.set ( "K", K );
	}
	if ( OutputStart.length() > 0 ) {
		props.set ( "OutputStart", OutputStart );
	}
	if ( OutputEnd.length() > 0 ) {
		props.set ( "OutputEnd", OutputEnd );
	}
	if ( (FlowUnits != null) && (FlowUnits.length() > 0) ) {
		props.set ( "FlowUnits", FlowUnits );
	}
	if ( (LagInterval != null) && (LagInterval.length() > 0) ) {
		props.set ( "LagInterval", LagInterval );
	}
	if ( (InitializeStatesFromTable != null) && (InitializeStatesFromTable.length() > 0) ) {
		props.set ( "InitializeStatesFromTable", InitializeStatesFromTable );
	}
	if ( (InitialLaggedInflow != null) && (InitialLaggedInflow.length() > 0) ) {
		props.set ( "InitialLaggedInflow", InitialLaggedInflow );
	}
	if ( (InitialOutflow != null) && (InitialOutflow.length() > 0) ) {
		props.set ( "InitialOutflow", InitialOutflow );
	}
	if ( (InitialStorage != null) && (InitialStorage.length() > 0) ) {
		props.set ( "InitialStorage", InitialStorage );
	}
	if ( (InitialQTLag != null) && (InitialQTLag.length() > 0) ) {
		props.set ( "InitialQTLag", InitialQTLag );
	}
    if ( StateTableID.length() > 0 ) {
    	props.set ( "StateTableID", StateTableID );
    }
    if ( StateTableObjectIDColumn.length() > 0 ) {
    	props.set ( "StateTableObjectIDColumn", StateTableObjectIDColumn );
    }
    if ( StateTableObjectID.length() > 0 ) {
    	props.set ( "StateTableObjectID", StateTableObjectID );
    }
    if ( StateTableDateTimeColumn.length() > 0 ) {
    	props.set ( "StateTableDateTimeColumn", StateTableDateTimeColumn );
    }
    if ( StateTableNameColumn.length() > 0 ) {
    	props.set ( "StateTableNameColumn", StateTableNameColumn );
    }
    if ( StateTableValueColumn.length() > 0 ) {
    	props.set ( "StateTableValueColumn", StateTableValueColumn );
    }
    if ( StateTableStateName.length() > 0 ) {
    	props.set ( "StateTableStateName", StateTableStateName );
    }
    if ( StateSaveDateTime.length() > 0 ) {
    	props.set ( "StateSaveDateTime", StateSaveDateTime );
    }
    if ( StateSaveInterval.length() > 0 ) {
    	props.set ( "StateSaveInterval", StateSaveInterval );
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
{	String Alias = __Alias_JTextField.getText().trim();
    String TSID = __TSID_JComboBox.getSelected();
    String NewTSID = __NewTSID_JTextArea.getText().trim();
    String Lag = __Lag_JTextArea.getText().trim();
    String K = __K_JTextArea.getText().trim();
	String OutputStart = __OutputStart_JTextField.getText().trim();
	String OutputEnd = __OutputEnd_JTextField.getText().trim();
    String FlowUnits = __FlowUnits_JTextField.getText().trim();
    String LagInterval = __LagInterval_JComboBox.getSelected().trim();
    String InitializeStatesFromTable = __InitializeStatesFromTable_JComboBox.getSelected().trim();
    String InitialLaggedInflow = __InitialLaggedInflow_JTextField.getText().trim();
	String InitialOutflow = __InitialOutflow_JTextField.getText().trim();
	String InitialStorage = __InitialStorage_JTextField.getText().trim();
    String InitialQTLag = __InitialQTLag_JTextArea.getText().trim();
	String StateTableID = __StateTableID_JComboBox.getSelected();
	String StateTableObjectIDColumn = __StateTableObjectIDColumn_JTextField.getText().trim();
	String StateTableObjectID = __StateTableObjectID_JTextField.getText().trim();
	String StateTableDateTimeColumn = __StateTableDateTimeColumn_JTextField.getText().trim();
	String StateTableNameColumn = __StateTableNameColumn_JTextField.getText().trim();
	String StateTableValueColumn = __StateTableValueColumn_JTextField.getText().trim();
	String StateTableStateName = __StateTableStateName_JTextField.getText().trim();
	String StateSaveDateTime = __StateSaveDateTime_JTextField.getText().trim();
	String StateSaveInterval = __StateSaveInterval_JComboBox.getSelected();
	__command.setCommandParameter ( "Alias", Alias );
	__command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "NewTSID", NewTSID );
	__command.setCommandParameter ( "Lag", Lag);
	__command.setCommandParameter ( "K", K );
	__command.setCommandParameter ( "OutputStart", OutputStart );
	__command.setCommandParameter ( "OutputEnd", OutputEnd );
	__command.setCommandParameter ( "FlowUnits", FlowUnits );
	__command.setCommandParameter ( "LagInterval", LagInterval );
	__command.setCommandParameter ( "InitializeStatesFromTable", InitializeStatesFromTable );
	__command.setCommandParameter ( "InitialLaggedInflow", InitialLaggedInflow );
	__command.setCommandParameter ( "InitialOutflow", InitialOutflow );
	__command.setCommandParameter ( "InitialStorage", InitialStorage );
	__command.setCommandParameter ( "InitialQTLag", InitialQTLag );
    __command.setCommandParameter ( "StateTableID", StateTableID );
    __command.setCommandParameter ( "StateTableObjectIDColumn", StateTableObjectIDColumn );
    __command.setCommandParameter ( "StateTableObjectID", StateTableObjectID );
    __command.setCommandParameter ( "StateTableDateTimeColumn", StateTableDateTimeColumn );
    __command.setCommandParameter ( "StateTableNameColumn", StateTableNameColumn );
    __command.setCommandParameter ( "StateTableValueColumn", StateTableValueColumn );
    __command.setCommandParameter ( "StateTableStateName", StateTableStateName );
    __command.setCommandParameter ( "StateSaveDateTime", StateSaveDateTime );
    __command.setCommandParameter ( "StateSaveInterval", StateSaveInterval );
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param title Dialog title.
@param command Command to edit.
@param StateTableIDChoices choices for StateTableID value.
*/
private void initialize ( JFrame parent, VariableLagK_Command command, List<String> StateTableIDChoices )
{	__parent_JFrame = parent;
    __command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Lag and attenuate a time series, creating a new output time series using variable Lag and K technique." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"See the documentation for a complete description of the algorithm." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
	       0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
     
    // Panel for input time series
    int yInputTS = -1;
    JPanel inputTS_JPanel = new JPanel();
    inputTS_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Input Time Series", inputTS_JPanel );

    JGUIUtil.addComponent(inputTS_JPanel, new JLabel (
        "Specify the time series to be routed (the input time series will not be modified - the output time series will contain the result)." ),
        0, ++yInputTS, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(inputTS_JPanel, new JLabel (
        "The time series to be routed cannot contain missing values.  Use fill commands prior to this command if necessary." ),
        0, ++yInputTS, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(inputTS_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++yInputTS, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(inputTS_JPanel, new JLabel( "Time series to lag (TSID):"),
		0, ++yInputTS, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TSID_JComboBox = new SimpleJComboBox ( true ); // Allow edit
	__TSID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
	List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
			(TSCommandProcessor)__command.getCommandProcessor(), __command );
	if ( tsids == null ) {
		// User will not be able to select anything.
		tsids = new ArrayList<String>();
	}
	__TSID_JComboBox.setData ( tsids );
	__TSID_JComboBox.addItemListener ( this );
	__TSID_JComboBox.getJTextComponent().addKeyListener ( this );
    JGUIUtil.addComponent(inputTS_JPanel, __TSID_JComboBox,
		1, yInputTS, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel for analysis parameters series
    int yAnalysis = -1;
    JPanel analysis_JPanel = new JPanel();
    analysis_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Analysis Parameters", analysis_JPanel );
    
    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
        "Specify the parameters to control the analysis.  The Lag (flow/lag) and attenuation K (flow/K) data can have different number of points." ),
        0, ++yAnalysis, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(analysis_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++yAnalysis, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(analysis_JPanel, new JLabel ("Flow units:"),
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __FlowUnits_JTextField = new JTextField (10);
    __FlowUnits_JTextField.setToolTipText("Specify flow units, can use ${Property}");
    __FlowUnits_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(analysis_JPanel, __FlowUnits_JTextField,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
        "Required - units of Lag and K flow values, compatible with time series."),
        3, yAnalysis, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(analysis_JPanel, new JLabel("Lag interval: "),
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LagInterval_JComboBox = new SimpleJComboBox(false);
    List<String> lagIntervalList = TimeInterval.getTimeIntervalBaseChoices(
        TimeInterval.MINUTE, TimeInterval.YEAR, 1, false);
    lagIntervalList.add ( 0, "" );
    __LagInterval_JComboBox.setData ( lagIntervalList );
    __LagInterval_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __LagInterval_JComboBox,
        1, yAnalysis, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel("Required - Lag and K interval units."),
        3, yAnalysis, 1, 1, 0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ("Lag:"),
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Lag_JTextArea = new JTextArea (3, 40);
    __Lag_JTextArea.setToolTipText("Specify lag as flow1,lag1;flow2;lag2;... can use ${Property}");
    __Lag_JTextArea.setLineWrap ( true );
    __Lag_JTextArea.setWrapStyleWord ( true );
    __Lag_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(analysis_JPanel, new JScrollPane(__Lag_JTextArea),
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
        "Optional - flow,Lag;flow,Lag pairs."),
        3, yAnalysis, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(analysis_JPanel, new JLabel ("K:"),
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __K_JTextArea = new JTextArea (3, 40);
    __K_JTextArea.setToolTipText("Specify K as flow1,k1;flow2;k2;... can use ${Property}");
    __K_JTextArea.setLineWrap ( true );
    __K_JTextArea.setWrapStyleWord ( true );
    __K_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(analysis_JPanel, new JScrollPane(__K_JTextArea),
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
        "Optional - flow,K;flow,K pairs."),
        3, yAnalysis, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Output start:" ),
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputStart_JTextField = new JTextField ( "", 20 );
    __OutputStart_JTextField.setToolTipText("Specify the output start using a date/time string or ${Property} notation");
    __OutputStart_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __OutputStart_JTextField,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
        "Optional - output start date/time (default=full time series period)."),
        3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Output end:" ), 
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputEnd_JTextField = new JTextField ( "", 20 );
    __OutputEnd_JTextField.setToolTipText("Specify the output end using a date/time string or ${Property} notation");
    __OutputEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __OutputEnd_JTextField,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
        "Optional - output end date/time (default=full time series period)."),
        3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Panel for input states
    int yStatesIn = -1;
    JPanel statesIn_JPanel = new JPanel();
    statesIn_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "States (Input)", statesIn_JPanel );

	JGUIUtil.addComponent(statesIn_JPanel, new JLabel (
        "Initial states for the VariabLagK algorithm can be specified to ensure continuity with previous processing runs." ),
        0, ++yStatesIn, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(statesIn_JPanel, new JLabel (
        "Initial states can be read from the state table, in which case OutputStart is used to look up the states." ),
        0, ++yStatesIn, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(statesIn_JPanel, new JLabel (
        "The initial value parameters will only be used if no state table, or states are not found in the table." ),
        0, ++yStatesIn, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(statesIn_JPanel, new JLabel (
        "Specify initial QT Lag values in order t-n,...,t0 where t0 corresponds to OutputStart and intervals proceed backward from t0." ),
        0, ++yStatesIn, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesIn_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yStatesIn, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(statesIn_JPanel, new JLabel("Initialize states from table?: "),
        0, ++yStatesIn, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InitializeStatesFromTable_JComboBox = new SimpleJComboBox(true); // Allow edit to use property
    __InitializeStatesFromTable_JComboBox.setToolTipText("Indicate whether to initialize states from table, can use ${Property}");
    List<String> choiceList = new ArrayList<String>();
    choiceList.add ( "" );
    choiceList.add ( __command._False );
    choiceList.add ( __command._True );
    __InitializeStatesFromTable_JComboBox.setData ( choiceList );
    __InitializeStatesFromTable_JComboBox.addActionListener ( this );
    //__InitializeStatesFromTable_JComboBox.getJTextComponent().getDocument().addDocumentListener ( this );
    __InitializeStatesFromTable_JComboBox.getJTextComponent().addKeyListener ( this );
    JGUIUtil.addComponent(statesIn_JPanel, __InitializeStatesFromTable_JComboBox,
        1, yStatesIn, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesIn_JPanel, new JLabel("Optional - use if states available (default=" + __command._False + ")."),
        3, yStatesIn, 1, 1, 0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(statesIn_JPanel, new JLabel ("Initial lagged inflow:"),
        0, ++yStatesIn, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InitialLaggedInflow_JTextField = new JTextField (10);
    __InitialLaggedInflow_JTextField.setToolTipText("Specify initial lagged inflow state.");
    __InitialLaggedInflow_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(statesIn_JPanel, __InitialLaggedInflow_JTextField,
        1, yStatesIn, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesIn_JPanel, new JLabel (
        "Optional - initial lagged inflow state (default=0)."),
        3, yStatesIn, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(statesIn_JPanel, new JLabel ("Initial outflow:"),
        0, ++yStatesIn, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InitialOutflow_JTextField = new JTextField (10);
    __InitialOutflow_JTextField.setToolTipText("Specify initial outflow state.");
    __InitialOutflow_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(statesIn_JPanel, __InitialOutflow_JTextField,
        1, yStatesIn, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesIn_JPanel, new JLabel (
        "Optional - initial outflow state (default=0)."),
        3, yStatesIn, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(statesIn_JPanel, new JLabel ("Initial storage:"),
        0, ++yStatesIn, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InitialStorage_JTextField = new JTextField (10);
    __InitialStorage_JTextField.setToolTipText("Specify initial storage state value.");
    __InitialStorage_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(statesIn_JPanel, __InitialStorage_JTextField,
        1, yStatesIn, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesIn_JPanel, new JLabel (
        "Optional - initial storage state (default=0)."),
        3, yStatesIn, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(statesIn_JPanel, new JLabel ( "Initial QT lag values:" ),
        0, ++yStatesIn, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InitialQTLag_JTextArea = new JTextArea ( 3, 40 );
    __InitialQTLag_JTextArea.setLineWrap ( true );
    __InitialQTLag_JTextArea.setWrapStyleWord ( true );
    __InitialQTLag_JTextArea.addKeyListener ( this );
    // Make 3-high
    JGUIUtil.addComponent(statesIn_JPanel, new JScrollPane(__InitialQTLag_JTextArea),
        1, yStatesIn, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesIn_JPanel, new JLabel(
        "Optional - separate values by commas (default=0 for all)."), 
        3, yStatesIn, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Panel for output states
    int yStatesOut = -1;
    JPanel statesOut_JPanel = new JPanel();
    statesOut_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "States (Output)", statesOut_JPanel );

	JGUIUtil.addComponent(statesOut_JPanel, new JLabel (
       "Output states can be written to the state table by this command to allow a future restart." ),
       0, ++yStatesOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(statesOut_JPanel, new JLabel (
       "Output states will be written at date/times as specified by the following parameters." ),
       0, ++yStatesOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(statesOut_JPanel, new JLabel (
       "Any matching date/time, when compared to the processing date/time, will trigger saving states to the state table." ),
       0, ++yStatesOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(statesOut_JPanel, new JLabel (
       "Saving states using an interval of day will save at midnight and interval of hour will save at minute=0." ),
       0, ++yStatesOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesOut_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yStatesOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(statesOut_JPanel, new JLabel ( "State save date/time:" ), 
        0, ++yStatesOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __StateSaveDateTime_JTextField = new JTextField ( 20 );
    __StateSaveDateTime_JTextField.setToolTipText("Specify the date/time when states should be saved, can use ${Property} notation");
    __StateSaveDateTime_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(statesOut_JPanel, __StateSaveDateTime_JTextField,
        1, yStatesOut, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesOut_JPanel, new JLabel( "Optional (default=no states saved)."), 
        3, yStatesOut, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(statesOut_JPanel, new JLabel ( "State save interval:" ), 
        0, ++yStatesOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __StateSaveInterval_JComboBox = new SimpleJComboBox ( true ); // Allow editing to allow property to be specified
    __StateSaveInterval_JComboBox.setToolTipText("Specify the interval to save states, can use ${Property}");
    // TODO SAM 2016-08-28 Evaluate whether to allow saving more frequently
    //boolean padZeroes = true;
    //boolean includeIrregular = false;
    //List<String> stateSaveIntervalList = TimeInterval.getTimeIntervalChoices(
    //    TimeInterval.MINUTE, TimeInterval.YEAR, padZeroes, 1, includeIrregular);
    // Add a blank
    List<String> stateSaveIntervalList = new ArrayList<String>();
    stateSaveIntervalList.add(TimeInterval.getName(TimeInterval.HOUR, 0));
    stateSaveIntervalList.add(TimeInterval.getName(TimeInterval.DAY, 0));
    stateSaveIntervalList.add(0,"");
    __StateSaveInterval_JComboBox.setData ( stateSaveIntervalList );
    __StateSaveInterval_JComboBox.addItemListener ( this );
    //__StateSaveInterval_JComboBox.getJTextComponent().getDocument().addDocumentListener ( this );
    __StateSaveInterval_JComboBox.getJTextComponent().addKeyListener ( this );
        JGUIUtil.addComponent(statesOut_JPanel, __StateSaveInterval_JComboBox,
        1, yStatesOut, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesOut_JPanel, new JLabel("Optional (default=save only on StateSaveDateTime)."),
        3, yStatesOut, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for state table
    int yStatesTable = -1;
    JPanel statesTable_JPanel = new JPanel();
    statesTable_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "States (Table)", statesTable_JPanel );
    
	JGUIUtil.addComponent(statesTable_JPanel, new JLabel (
        "States can be initialized using a table, and the same table can be written to when saving states." ),
        0, ++yStatesTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(statesTable_JPanel, new JLabel (
        "The state table should generally include columns for object ID, date/time, state name, and state value,"
        + " although the object ID and/or state name columns can be omitted if not needed for lookup." ),
        0, ++yStatesTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(statesTable_JPanel, new JLabel (
        "The object ID, date/time, and state name provide a unique key to look up the state values." ),
        0, ++yStatesTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(statesTable_JPanel, new JLabel (
        "The state table column for state values will contain a JSON string." ),
        0, ++yStatesTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesTable_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yStatesTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel ( "State table ID:" ), 
        0, ++yStatesTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __StateTableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit
    __StateTableID_JComboBox.setToolTipText("Specify the table ID for states or use ${Property} notation");
    StateTableIDChoices.add(0,""); // Add blank to ignore table
    __StateTableID_JComboBox.setData ( StateTableIDChoices );
    __StateTableID_JComboBox.addItemListener ( this );
    //__StateTableID_JComboBox.setMaximumRowCount(StateTableIDChoices.size());
    JGUIUtil.addComponent(statesTable_JPanel, __StateTableID_JComboBox,
        1, yStatesTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel(
        "Optional - use if are read from table."), 
        3, yStatesTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel ( "Object ID column:" ), 
        0, ++yStatesTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __StateTableObjectIDColumn_JTextField = new JTextField ( 20 );
    __StateTableObjectIDColumn_JTextField.setToolTipText("Specify the object ID column or use ${Property} notation");
    __StateTableObjectIDColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(statesTable_JPanel, __StateTableObjectIDColumn_JTextField,
        1, yStatesTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel( "Optional - column name for object ID (default=not used)."), 
        3, yStatesTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel ( "State table object ID:" ), 
        0, ++yStatesTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __StateTableObjectID_JTextField = new JTextField ( 20 );
    __StateTableObjectID_JTextField.setToolTipText("Specify the object ID column or use ${Property} notation");
    __StateTableObjectID_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(statesTable_JPanel, __StateTableObjectID_JTextField,
        1, yStatesTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel( "Required - if object ID column is used."), 
        3, yStatesTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel ( "Date/time column:" ),
        0, ++yStatesTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __StateTableDateTimeColumn_JTextField = new JTextField ( 20 );
    __StateTableDateTimeColumn_JTextField.setToolTipText("Specify the date/time column or use ${Property} notation");
    __StateTableDateTimeColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(statesTable_JPanel, __StateTableDateTimeColumn_JTextField,
        1, yStatesTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel(
        "Required if using table - column name for state date/time."), 
        3, yStatesTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel ( "State name column:" ), 
        0, ++yStatesTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __StateTableNameColumn_JTextField = new JTextField ( 20 );
    __StateTableNameColumn_JTextField.setToolTipText("Specify the state name column or use ${Property} notation");
    __StateTableNameColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(statesTable_JPanel, __StateTableNameColumn_JTextField,
        1, yStatesTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel(
    	"Optional - column name for state name (default=not used)."), 
        3, yStatesTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel ( "State value column:" ), 
        0, ++yStatesTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __StateTableValueColumn_JTextField = new JTextField ( 20 );
    __StateTableValueColumn_JTextField.setToolTipText("Specify the state value column or use ${Property} notation");
    __StateTableValueColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(statesTable_JPanel, __StateTableValueColumn_JTextField,
        1, yStatesTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel(
        "Required if using table - column name for state value."), 
        3, yStatesTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel ( "State table state name:" ), 
        0, ++yStatesTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __StateTableStateName_JTextField = new JTextField ( 20 );
    __StateTableStateName_JTextField.setToolTipText("Specify the name of the states as JSON, can use ${Property} notation");
    __StateTableStateName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(statesTable_JPanel, __StateTableStateName_JTextField,
        1, yStatesTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel(
        "Optional if using table - name of states as JSON (default=state name column not used)."), 
        3, yStatesTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Panel for output time series
    int yOutputTS = -1;
    JPanel outputTS_JPanel = new JPanel();
    outputTS_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output Time Series", outputTS_JPanel );
    
    JGUIUtil.addComponent(outputTS_JPanel, new JLabel (
        "Specify information to identify the new output time series." ),
        0, ++yOutputTS, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(outputTS_JPanel, new JLabel (
        "Unique time series identifier (NewTSID) and alias should be specified." ),
        0, ++yOutputTS, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(outputTS_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++yOutputTS, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(outputTS_JPanel, new JLabel ( "New time series ID:" ),
		0, ++yOutputTS, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    	__NewTSID_JTextArea = new JTextArea ( 3, 40 );
	__NewTSID_JTextArea.setToolTipText("Specify new time series ID, can include ${Property} notation for TSID parts");
    __NewTSID_JTextArea.setEditable(false);
	__NewTSID_JTextArea.setLineWrap ( true );
	__NewTSID_JTextArea.setWrapStyleWord ( true );
	__NewTSID_JTextArea.addKeyListener ( this );
	// Make 3-high to fit in the edit button...
    JGUIUtil.addComponent(outputTS_JPanel, new JScrollPane(__NewTSID_JTextArea),
		1, yOutputTS, 2, 3, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(outputTS_JPanel, new JLabel(
		"Specify to avoid confusion with TSID from original TS."),
		3, yOutputTS, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    yOutputTS += 2;
    JGUIUtil.addComponent(outputTS_JPanel, (__edit_JButton = new SimpleJButton ( "Edit", "Edit", this ) ),
		3, yOutputTS, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(outputTS_JPanel, (__clear_JButton = new SimpleJButton ( "Clear", "Clear", this ) ),
		4, yOutputTS, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(outputTS_JPanel, new JLabel("Alias to assign:"),
        0, ++yOutputTS, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(15);
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(outputTS_JPanel, __Alias_JTextField,
        1, yOutputTS, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(outputTS_JPanel, new JLabel ("Optional - use %L for location, etc."),
        3, yOutputTS, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (4,50);
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	// Refresh the contents...
	refresh();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	button_JPanel.add(__cancel_JButton = new SimpleJButton("Cancel", this));
	button_JPanel.add ( __ok_JButton = new SimpleJButton("OK", this) );

	setTitle ( "Edit " + __command.getCommandName() + "() Command" );

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
{	checkGUIState();
    refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else {	refresh();
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
Refresh the command from component contents.
*/
private void refresh ()
{	String routine = getClass().getSimpleName() + ".refresh";
	String Alias = "";
	String TSID = "";
    String NewTSID = "";
	String Lag = "";
	String K = "";
	String OutputStart = "";
	String OutputEnd = "";
	String FlowUnits = "";
	String LagInterval = "";
	String InitializeStatesFromTable = "";
	String InitialLaggedInflow = "";
	String InitialOutflow = "";
	String InitialStorage = "";
	String InitialQTLag = "";
	String StateTableID = "";
	String StateTableObjectIDColumn = "";
	String StateTableObjectID = "";
	String StateTableDateTimeColumn = "";
	String StateTableNameColumn = "";
	String StateTableValueColumn = "";
	String StateTableStateName = "";
	String StateSaveDateTime = "";
	String StateSaveInterval = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		Alias = props.getValue ( "Alias" );
        TSID = props.getValue ( "TSID" );
        NewTSID = props.getValue ( "NewTSID" );
		Lag = props.getValue ( "Lag" );
		K = props.getValue ( "K" );
		OutputStart = props.getValue ( "OutputStart" );
		OutputEnd = props.getValue ( "OutputEnd" );
		FlowUnits = props.getValue ( "FlowUnits" );
		LagInterval = props.getValue ( "LagInterval" );
		InitializeStatesFromTable = props.getValue ( "InitializeStatesFromTable" );
		InitialLaggedInflow = props.getValue ( "InitialLaggedInflow" );
	    InitialOutflow = props.getValue ( "InitialOutflow" );
	    InitialStorage = props.getValue ( "InitialStorage" );
	    InitialQTLag = props.getValue ( "InitialQTLag" );
		StateTableID = props.getValue ( "StateTableID" );
		StateTableObjectIDColumn = props.getValue ( "StateTableObjectIDColumn" );
		StateTableObjectID = props.getValue ( "StateTableObjectID" );
		StateTableDateTimeColumn = props.getValue ( "StateTableDateTimeColumn" );
		StateTableNameColumn = props.getValue ( "StateTableNameColumn" );
		StateTableValueColumn = props.getValue ( "StateTableValueColumn" );
		StateTableStateName = props.getValue ( "StateTableStateName" );;
		StateSaveDateTime = props.getValue ( "StateSaveDateTime" );
		StateSaveInterval = props.getValue ( "StateSaveInterval" );
		// Now select the item in the list.  If not a match, print a warning.
		if ( JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID, JGUIUtil.NONE, null, null ) ) {
			__TSID_JComboBox.select ( TSID );
		}
		else {
		    // Automatically add to the list
			if ( (TSID != null) && (TSID.length() > 0) ) {
				__TSID_JComboBox.insertItemAt ( TSID, 0 );
				// Select...
				__TSID_JComboBox.select ( TSID );
			}
			else {
			    // Do not select anything...
			}
		}
        if ( NewTSID != null ) {
			__NewTSID_JTextArea.setText ( NewTSID );
		}
        if ( Alias != null ) {
            __Alias_JTextField.setText ( Alias );
        }
        if ( FlowUnits != null ) {
            __FlowUnits_JTextField.setText ( FlowUnits );
        }
        if ( LagInterval == null ) {
            // Select default...
            __LagInterval_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __LagInterval_JComboBox, LagInterval, JGUIUtil.NONE, null, null )) {
                __LagInterval_JComboBox.select ( LagInterval );
            }
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid\n" +
                "LagInterval value \"" + LagInterval + "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( InitializeStatesFromTable == null ) {
            // Select default...
            __InitializeStatesFromTable_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __InitializeStatesFromTable_JComboBox, InitializeStatesFromTable, JGUIUtil.NONE, null, null )) {
                __InitializeStatesFromTable_JComboBox.select ( InitializeStatesFromTable );
            }
            else if ( InitializeStatesFromTable.indexOf("${") >= 0 ) {
        		// OK to add as first position and select
        		__InitializeStatesFromTable_JComboBox.insertItemAt(InitializeStatesFromTable, 1);
        		__InitializeStatesFromTable_JComboBox.select(1);
        	}
        	else {
                Message.printWarning ( 1, routine, "Existing command references an invalid\n" +
                "InitializeStatesFromTable value \"" + InitializeStatesFromTable + "\".  Select a different value or Cancel.");
                __error_wait = true;
        	}
        }
		if ( Lag != null ) {
			__Lag_JTextArea.setText( Lag );
		}
		if ( K != null ) {
			__K_JTextArea.setText ( K );
		}
		if ( OutputStart != null ) {
			__OutputStart_JTextField.setText( OutputStart );
		}
		if ( OutputEnd != null ) {
			__OutputEnd_JTextField.setText ( OutputEnd );
		}
        if ( InitialLaggedInflow != null ) {
            __InitialLaggedInflow_JTextField.setText ( InitialLaggedInflow );
        }
        if ( InitialOutflow != null ) {
            __InitialOutflow_JTextField.setText ( InitialOutflow );
        }
        if ( InitialStorage != null ) {
            __InitialStorage_JTextField.setText ( InitialStorage );
        }
        if ( InitialQTLag != null ) {
            __InitialQTLag_JTextArea.setText ( InitialQTLag );
        }
        if ( StateTableID == null ) {
            // Select default...
            __StateTableID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __StateTableID_JComboBox,StateTableID, JGUIUtil.NONE, null, null ) ) {
                __StateTableID_JComboBox.select ( StateTableID );
            }
            else {
                // Creating new table so add in the first position
                if ( __StateTableID_JComboBox.getItemCount() == 0 ) {
                    __StateTableID_JComboBox.add(StateTableID);
                }
                else {
                    __StateTableID_JComboBox.insert(StateTableID, 0);
                }
                __StateTableID_JComboBox.select(0);
            }
        }
        if ( StateTableObjectIDColumn != null ) {
            __StateTableObjectIDColumn_JTextField.setText ( StateTableObjectIDColumn );
        }
        if ( StateTableObjectID != null ) {
            __StateTableObjectID_JTextField.setText ( StateTableObjectID );
        }
        if ( StateTableDateTimeColumn != null ) {
            __StateTableDateTimeColumn_JTextField.setText ( StateTableDateTimeColumn );
        }
        if ( StateTableNameColumn != null ) {
            __StateTableNameColumn_JTextField.setText ( StateTableNameColumn );
        }
        if ( StateTableValueColumn != null ) {
            __StateTableValueColumn_JTextField.setText ( StateTableValueColumn );
        }
        if ( StateTableStateName != null ) {
            __StateTableStateName_JTextField.setText ( StateTableStateName );
        }
        if ( StateSaveDateTime != null ) {
            __StateSaveDateTime_JTextField.setText ( StateSaveDateTime );
        }
        if ( StateSaveInterval == null ) {
            // Select default...
            __StateSaveInterval_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __StateSaveInterval_JComboBox, StateSaveInterval, JGUIUtil.NONE, null, null )) {
                __StateSaveInterval_JComboBox.select ( StateSaveInterval );
            }
            else if ( StateSaveInterval.indexOf("${") >= 0 ) {
        		// OK to add as first position and select
        		__StateSaveInterval_JComboBox.insertItemAt(StateSaveInterval, 1);
        		__StateSaveInterval_JComboBox.select(1);
        	}
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid\n" +
                "StateSaveInterval value \"" + StateSaveInterval + "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
	}
	// Regardless, reset the command from the fields...
	TSID = __TSID_JComboBox.getSelected();
    NewTSID = __NewTSID_JTextArea.getText().trim();
    FlowUnits = __FlowUnits_JTextField.getText().trim();
    LagInterval = __LagInterval_JComboBox.getSelected();
	Lag = __Lag_JTextArea.getText().trim();
	K = __K_JTextArea.getText().trim();
	OutputStart = __OutputStart_JTextField.getText().trim();
	OutputEnd = __OutputEnd_JTextField.getText().trim();
    InitializeStatesFromTable = __InitializeStatesFromTable_JComboBox.getSelected();
    InitialLaggedInflow = __InitialLaggedInflow_JTextField.getText().trim();
    InitialOutflow = __InitialOutflow_JTextField.getText().trim();
    InitialStorage = __InitialStorage_JTextField.getText().trim();
    InitialQTLag = __InitialQTLag_JTextArea.getText().trim();
	StateTableID = __StateTableID_JComboBox.getSelected();
    StateTableObjectIDColumn = __StateTableObjectIDColumn_JTextField.getText().trim();
    StateTableObjectID = __StateTableObjectID_JTextField.getText().trim();
    StateTableDateTimeColumn = __StateTableDateTimeColumn_JTextField.getText().trim();
    StateTableNameColumn = __StateTableNameColumn_JTextField.getText().trim();
    StateTableValueColumn = __StateTableValueColumn_JTextField.getText().trim();
    StateTableStateName = __StateTableStateName_JTextField.getText().trim();
    StateSaveDateTime = __StateSaveDateTime_JTextField.getText().trim();
    StateSaveInterval = __StateSaveInterval_JComboBox.getSelected();
    Alias = __Alias_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "TSID=" + TSID );
    props.add ( "FlowUnits=" + FlowUnits );
    props.add ( "LagInterval=" + LagInterval );
	props.add ( "Lag=" + Lag );
	props.add ( "K=" + K );
	props.add ( "OutputStart=" + OutputStart );
	props.add ( "OutputEnd=" + OutputEnd );
    props.add ( "InitializeStatesFromTable=" + InitializeStatesFromTable );
    props.add ( "InitialLaggedInflow=" + InitialLaggedInflow );
    props.add ( "InitialOutflow=" + InitialOutflow );
    props.add ( "InitialStorage=" + InitialStorage );
	props.add ( "InitialQTLag=" + InitialQTLag );
    props.add ( "StateSaveDateTime=" + StateSaveDateTime );
    props.add ( "StateSaveInterval=" + StateSaveInterval );
    props.add ( "StateTableID=" + StateTableID );
    props.add ( "StateTableObjectIDColumn=" + StateTableObjectIDColumn );
    props.add ( "StateTableObjectID=" + StateTableObjectID );
    props.add ( "StateTableDateTimeColumn=" + StateTableDateTimeColumn );
    props.add ( "StateTableNameColumn=" + StateTableNameColumn );
    props.add ( "StateTableValueColumn=" + StateTableValueColumn );
    props.add ( "StateTableStateName=" + StateTableStateName );
    props.add ( "NewTSID=" + NewTSID );
	props.add ( "Alias=" + Alias );
	__command_JTextArea.setText( __command.toString ( props ) );
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed
and the dialog is closed.
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