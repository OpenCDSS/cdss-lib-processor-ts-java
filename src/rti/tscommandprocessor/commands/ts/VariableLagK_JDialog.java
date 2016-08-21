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
private JTextField __FlowUnits_JTextField = null;
private SimpleJComboBox __LagInterval_JComboBox = null;
private JTextArea __InflowStates_JTextArea = null;
private JTextArea __OutflowStates_JTextArea = null;
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __TableTSIDColumn_JTextField = null;
private TSFormatSpecifiersJPanel __TableTSIDFormat_JTextField = null; // Format for time series identifiers
private JTextField __TableStateDateTimeColumn_JTextField = null;
private JTextField __TableStateNameColumn_JTextField = null;
private JTextField __TableStateValueColumn_JTextField = null;
private JTextField __TableInflowStateName_JTextField = null;
private JTextField __TableOutflowStateName_JTextField = null;

private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Whether OK has been pressed.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices choices for TableID value.
*/
public VariableLagK_JDialog ( JFrame parent, VariableLagK_Command command, List<String> tableIDChoices )
{	super(parent, true);
	initialize ( parent, command, tableIDChoices );
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
    String FlowUnits = __FlowUnits_JTextField.getText().trim();
    String LagInterval = __LagInterval_JComboBox.getSelected().trim();
	String InflowStates = __InflowStates_JTextArea.getText().trim();
	String OutflowStates = __OutflowStates_JTextArea.getText().trim();
	String TableID = __TableID_JComboBox.getSelected();
	String TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
	String TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
	String TableStateDateTimeColumn = __TableStateDateTimeColumn_JTextField.getText().trim();
	String TableStateNameColumn = __TableStateNameColumn_JTextField.getText().trim();
	String TableStateValueColumn = __TableStateValueColumn_JTextField.getText().trim();
	String TableInflowStateName = __TableInflowStateName_JTextField.getText().trim();
	String TableOutflowStateName = __TableOutflowStateName_JTextField.getText().trim();
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
	if ( (FlowUnits != null) && (FlowUnits.length() > 0) ) {
		props.set ( "FlowUnits", FlowUnits );
	}
	if ( (LagInterval != null) && (LagInterval.length() > 0) ) {
		props.set ( "LagInterval", LagInterval );
	}
	if ( (InflowStates != null) && (InflowStates.length() > 0) ) {
		props.set ( "InflowStates", InflowStates );
	}
	if ( (OutflowStates != null) && (OutflowStates.length() > 0) ) {
	    props.set ( "OutflowStates", OutflowStates );
	}
    if ( TableID.length() > 0 ) {
    	props.set ( "TableID", TableID );
    }
    if ( TableTSIDColumn.length() > 0 ) {
    	props.set ( "TableTSIDColumn", TableTSIDColumn );
    }
    if ( TableTSIDFormat.length() > 0 ) {
    	props.set ( "TableTSIDFormat", TableTSIDFormat );
    }
    if ( TableStateDateTimeColumn.length() > 0 ) {
    	props.set ( "TableStateDateTimeColumn", TableStateDateTimeColumn );
    }
    if ( TableStateNameColumn.length() > 0 ) {
    	props.set ( "TableStateNameColumn", TableStateNameColumn );
    }
    if ( TableStateValueColumn.length() > 0 ) {
    	props.set ( "TableStateValueColumn", TableStateValueColumn );
    }
    if ( TableInflowStateName.length() > 0 ) {
    	props.set ( "TableInflowStateName", TableInflowStateName );
    }
    if ( TableOutflowStateName.length() > 0 ) {
    	props.set ( "TableOutflowStateName", TableOutflowStateName );
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
    String FlowUnits = __FlowUnits_JTextField.getText().trim();
    String LagInterval = __LagInterval_JComboBox.getSelected().trim();
    String InflowStates = __InflowStates_JTextArea.getText().trim();
    String OutflowStates = __OutflowStates_JTextArea.getText().trim();
	String TableID = __TableID_JComboBox.getSelected();
	String TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
	String TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
	String TableStateDateTimeColumn = __TableStateDateTimeColumn_JTextField.getText().trim();
	String TableStateNameColumn = __TableStateNameColumn_JTextField.getText().trim();
	String TableStateValueColumn = __TableStateValueColumn_JTextField.getText().trim();
	String TableInflowStateName = __TableInflowStateName_JTextField.getText().trim();
	String TableOutflowStateName = __TableOutflowStateName_JTextField.getText().trim();
	__command.setCommandParameter ( "Alias", Alias );
	__command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "NewTSID", NewTSID );
	__command.setCommandParameter ( "Lag", Lag);
	__command.setCommandParameter ( "K", K );
	__command.setCommandParameter ( "FlowUnits", FlowUnits );
	__command.setCommandParameter ( "LagInterval", LagInterval );
	__command.setCommandParameter ( "InflowStates", InflowStates );
	__command.setCommandParameter ( "OutflowStates", OutflowStates );
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "TableTSIDColumn", TableTSIDColumn );
    __command.setCommandParameter ( "TableTSIDFormat", TableTSIDFormat );
    __command.setCommandParameter ( "TableStateDateTimeColumn", TableStateDateTimeColumn );
    __command.setCommandParameter ( "TableStateNameColumn", TableStateNameColumn );
    __command.setCommandParameter ( "TableStateValueColumn", TableStateValueColumn );
    __command.setCommandParameter ( "TableInflowStateName", TableInflowStateName );
    __command.setCommandParameter ( "TableOutflowStateName", TableOutflowStateName );
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param title Dialog title.
@param command Command to edit.
@param tableIDChoices choices for TableID value.
*/
private void initialize ( JFrame parent, VariableLagK_Command command, List<String> tableIDChoices )
{	__parent_JFrame = parent;
    __command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"<html><b>This command is being enhanced to allow specifying inflow and outflow states.</b></html>" ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Lag and attenuate a time series, creating a new time series using variable Lag and K technique." ),
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
        "Specify the time series to be routed." ),
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
        "Specify the parameters to control the analysis.  The Lag (flow/lag) and K (flow/K) data can have different number of points." ),
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
        0, ++yAnalysis, 1, 1, 0, 0, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LagInterval_JComboBox = new SimpleJComboBox(false);
    List<String> intervalList = TimeInterval.getTimeIntervalBaseChoices(
        TimeInterval.MINUTE, TimeInterval.YEAR, 1, false);
    intervalList.add ( 0, "" );
    __LagInterval_JComboBox.setData ( intervalList );
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
    
    // Panel for states
    int yStates = -1;
    JPanel states_JPanel = new JPanel();
    states_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "States", states_JPanel );

	JGUIUtil.addComponent(states_JPanel, new JLabel (
       "States for the input and output time series can be specified to ensure continuity with previous processing runs." ),
       0, ++yStates, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(states_JPanel, new JLabel (
       "The InflowStates and OutputStates parameters are currently ignored - states default to zero." ),
       0, ++yStates, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(states_JPanel, new JLabel (
       "The states can be specified using a table - see States (Table) tab." ),
       0, ++yStates, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(states_JPanel, new JLabel (
       "<html><b>Need to decide whether to include dates for StateStart/StateEnd or AnalysisStart/AnalysisEnd</b></html>" ),
       0, ++yStates, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(states_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yStates, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(states_JPanel, new JLabel ( "Input time series states:" ),
            0, ++yStates, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InflowStates_JTextArea = new JTextArea ( 3, 40 );
    __InflowStates_JTextArea.setLineWrap ( true );
    __InflowStates_JTextArea.setWrapStyleWord ( true );
    __InflowStates_JTextArea.addKeyListener ( this );
    // Make 3-high
    JGUIUtil.addComponent(states_JPanel, new JScrollPane(__InflowStates_JTextArea),
        1, yStates, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(states_JPanel, new JLabel(
        "Optional - separate values by commas (default=0 for all)."), 
        3, yStates, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(states_JPanel, new JLabel ( "Output time series states:" ),
            0, ++yStates, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutflowStates_JTextArea = new JTextArea ( 3, 40 );
    __OutflowStates_JTextArea.setLineWrap ( true );
    __OutflowStates_JTextArea.setWrapStyleWord ( true );
    __OutflowStates_JTextArea.addKeyListener ( this );
    // Make 3-high
    JGUIUtil.addComponent(states_JPanel, new JScrollPane(__OutflowStates_JTextArea),
        1, yStates, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(states_JPanel, new JLabel(
        "Optional - separate values by commas (default=0 for all)."), 
        3, yStates, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Panel for states table
    int yStatesTable = -1;
    JPanel statesTable_JPanel = new JPanel();
    statesTable_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "States (Table)", statesTable_JPanel );
    
	JGUIUtil.addComponent(statesTable_JPanel, new JLabel (
        "<html>States for inflow and outflow time series can be provided using a table (<b>not currently functional</b>)</html>." ),
        0, ++yStatesTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(statesTable_JPanel, new JLabel (
        "The table should include columns for TSID to match, date/time for states, state name, and state value." ),
        0, ++yStatesTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(statesTable_JPanel, new JLabel (
        "State value is specified in array form:  [ inflow1, inflow2, inflow3 ] for example [ 10.0, 11.5, 13.1 ]" ),
        0, ++yStatesTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesTable_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yStatesTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel ( "Table ID for states:" ), 
        0, ++yStatesTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit
    __TableID_JComboBox.setToolTipText("Specify the table ID for states or use ${Property} notation");
    tableIDChoices.add(0,""); // Add blank to ignore table
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(statesTable_JPanel, __TableID_JComboBox,
        1, yStatesTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel(
        "Optional - use if are read from table."), 
        3, yStatesTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel ( "Table TSID column:" ), 
        0, ++yStatesTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableTSIDColumn_JTextField = new JTextField ( 20 );
    __TableTSIDColumn_JTextField.setToolTipText("Specify the table TSID column or use ${Property} notation");
    __TableTSIDColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(statesTable_JPanel, __TableTSIDColumn_JTextField,
        1, yStatesTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel( "Required if using table - column name for TSID."), 
        3, yStatesTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel("Format of TSID:"),
        0, ++yStatesTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableTSIDFormat_JTextField = new TSFormatSpecifiersJPanel(10);
    __TableTSIDFormat_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
    __TableTSIDFormat_JTextField.addKeyListener ( this );
    __TableTSIDFormat_JTextField.setToolTipText("%L for location, %T for data type.");
    JGUIUtil.addComponent(statesTable_JPanel, __TableTSIDFormat_JTextField,
        1, yStatesTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel ("Optional - use %L for location, etc. (default=alias or TSID)."),
        3, yStatesTable, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel ( "Table state date/time column:" ), 
        0, ++yStatesTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableStateDateTimeColumn_JTextField = new JTextField ( 20 );
    __TableStateDateTimeColumn_JTextField.setToolTipText("Specify the table state date/time column or use ${Property} notation");
    __TableStateDateTimeColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(statesTable_JPanel, __TableStateDateTimeColumn_JTextField,
        1, yStatesTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel(
        "Required if using table - column name for state date/time."), 
        3, yStatesTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel ( "Table state name column:" ), 
        0, ++yStatesTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableStateNameColumn_JTextField = new JTextField ( 20 );
    __TableStateNameColumn_JTextField.setToolTipText("Specify the table statistic column or use ${Property} notation");
    __TableStateNameColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(statesTable_JPanel, __TableStateNameColumn_JTextField,
        1, yStatesTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel(
        "Required if using table - column name(s) for states(s)."), 
        3, yStatesTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel ( "Table state value column:" ), 
        0, ++yStatesTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableStateValueColumn_JTextField = new JTextField ( 20 );
    __TableStateValueColumn_JTextField.setToolTipText("Specify the table statistic value column or use ${Property} notation");
    __TableStateValueColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(statesTable_JPanel, __TableStateValueColumn_JTextField,
        1, yStatesTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel(
        "Required if using table - column name for state value."), 
        3, yStatesTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel ( "Inflow state name:" ), 
        0, ++yStatesTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableInflowStateName_JTextField = new JTextField ( 20 );
    __TableInflowStateName_JTextField.setToolTipText("Specify the name of the state for inflow time series values ${Property} notation");
    __TableInflowStateName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(statesTable_JPanel, __TableInflowStateName_JTextField,
        1, yStatesTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel(
        "Required if using table - name of input states."), 
        3, yStatesTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel ( "Outflow state name:" ), 
        0, ++yStatesTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableOutflowStateName_JTextField = new JTextField ( 20 );
    __TableOutflowStateName_JTextField.setToolTipText("Specify the name of the state for outflow time series values ${Property} notation");
    __TableOutflowStateName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(statesTable_JPanel, __TableOutflowStateName_JTextField,
        1, yStatesTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(statesTable_JPanel, new JLabel(
        "Required if using table - name of output states."), 
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
	String FlowUnits = "";
	String LagInterval = "";
	String InflowStates = "";
	String OutflowStates = "";
	String TableID = "";
	String TableTSIDColumn = "";
	String TableTSIDFormat = "";
	String TableStateDateTimeColumn = "";
	String TableStateNameColumn = "";
	String TableStateValueColumn = "";
	String TableInflowStateName = "";
	String TableOutflowStateName = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		Alias = props.getValue ( "Alias" );
        TSID = props.getValue ( "TSID" );
        NewTSID = props.getValue ( "NewTSID" );
		Lag = props.getValue ( "Lag" );
		K = props.getValue ( "K" );
		FlowUnits = props.getValue ( "FlowUnits" );
		LagInterval = props.getValue ( "LagInterval" );
	    InflowStates = props.getValue ( "InflowStates" );
	    OutflowStates = props.getValue ( "OutflowStates" );
		TableID = props.getValue ( "TableID" );
		TableTSIDColumn = props.getValue ( "TableTSIDColumn" );
		TableTSIDFormat = props.getValue ( "TableTSIDFormat" );
		TableStateDateTimeColumn = props.getValue ( "TableStateDateTimeColumn" );
		TableStateNameColumn = props.getValue ( "TableStateNameColumn" );
		TableStateValueColumn = props.getValue ( "TableStateValueColumn" );
		TableInflowStateName = props.getValue ( "TableInflowStateName" );
		TableOutflowStateName = props.getValue ( "TableOutflowStateName" );
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
		if ( Lag != null ) {
			__Lag_JTextArea.setText( Lag );
		}
		if ( K != null ) {
			__K_JTextArea.setText ( K );
		}
        if ( InflowStates != null ) {
            __InflowStates_JTextArea.setText ( InflowStates );
        }
        if ( OutflowStates != null ) {
            __OutflowStates_JTextArea.setText ( OutflowStates );
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
                // Creating new table so add in the first position
                if ( __TableID_JComboBox.getItemCount() == 0 ) {
                    __TableID_JComboBox.add(TableID);
                }
                else {
                    __TableID_JComboBox.insert(TableID, 0);
                }
                __TableID_JComboBox.select(0);
            }
        }
        if ( TableTSIDColumn != null ) {
            __TableTSIDColumn_JTextField.setText ( TableTSIDColumn );
        }
        if (TableTSIDFormat != null ) {
            __TableTSIDFormat_JTextField.setText(TableTSIDFormat.trim());
        }
        if ( TableStateDateTimeColumn != null ) {
            __TableStateDateTimeColumn_JTextField.setText ( TableStateDateTimeColumn );
        }
        if ( TableStateNameColumn != null ) {
            __TableStateNameColumn_JTextField.setText ( TableStateNameColumn );
        }
        if ( TableStateValueColumn != null ) {
            __TableStateValueColumn_JTextField.setText ( TableStateValueColumn );
        }
        if ( TableInflowStateName != null ) {
            __TableInflowStateName_JTextField.setText ( TableInflowStateName );
        }
        if ( TableOutflowStateName != null ) {
            __TableOutflowStateName_JTextField.setText ( TableOutflowStateName );
        }
	}
	// Regardless, reset the command from the fields...
	TSID = __TSID_JComboBox.getSelected();
    NewTSID = __NewTSID_JTextArea.getText().trim();
    FlowUnits = __FlowUnits_JTextField.getText().trim();
    LagInterval = __LagInterval_JComboBox.getSelected();
	Lag = __Lag_JTextArea.getText().trim();
	K = __K_JTextArea.getText().trim();
    InflowStates = __InflowStates_JTextArea.getText().trim();
    OutflowStates = __OutflowStates_JTextArea.getText().trim();
	TableID = __TableID_JComboBox.getSelected();
    TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
    TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
    TableStateDateTimeColumn = __TableStateDateTimeColumn_JTextField.getText().trim();
    TableStateNameColumn = __TableStateNameColumn_JTextField.getText().trim();
    TableStateValueColumn = __TableStateValueColumn_JTextField.getText().trim();
    TableInflowStateName = __TableInflowStateName_JTextField.getText().trim();
    TableOutflowStateName = __TableOutflowStateName_JTextField.getText().trim();
    Alias = __Alias_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "TSID=" + TSID );
    props.add ( "NewTSID=" + NewTSID );
    props.add ( "FlowUnits=" + FlowUnits );
    props.add ( "LagInterval=" + LagInterval );
	props.add ( "Lag=" + Lag );
	props.add ( "K=" + K );
	props.add ( "InflowStates=" + InflowStates );
	props.add ( "OutflowStates=" + OutflowStates );
    props.add ( "TableID=" + TableID );
    props.add ( "TableTSIDColumn=" + TableTSIDColumn );
    props.add ( "TableTSIDFormat=" + TableTSIDFormat );
    props.add ( "TableStateDateTimeColumn=" + TableStateDateTimeColumn );
    props.add ( "TableStateNameColumn=" + TableStateNameColumn );
    props.add ( "TableStateValueColumn=" + TableStateValueColumn );
    props.add ( "TableInflowStateName=" + TableInflowStateName );
    props.add ( "TableOutflowStateName=" + TableOutflowStateName );
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