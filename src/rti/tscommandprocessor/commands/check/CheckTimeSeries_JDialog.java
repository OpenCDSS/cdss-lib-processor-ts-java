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
import java.util.List;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

import RTi.TS.TSUtil_CheckTimeSeries;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

public class CheckTimeSeries_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;// Cancel Button
private SimpleJButton __ok_JButton = null;	// Ok Button
private CheckTimeSeries_Command __command = null;	// Command to edit
private JTextArea __command_JTextArea=null;
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox	__TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private SimpleJComboBox __ValueToCheck_JComboBox = null;
private SimpleJComboBox __CheckCriteria_JComboBox = null;
private JTextField __Value1_JTextField = null;
private JTextField __Value2_JTextField = null;
private JTextField __AnalysisStart_JTextField = null;
private JTextField __AnalysisEnd_JTextField = null;
private JTextField __ProblemType_JTextField = null;// Field for problem type
private JTextField __MaxWarnings_JTextField = null;
private JTextField __Flag_JTextField = null; // Flag to label filled data.
private JTextField __FlagDesc_JTextField;
private SimpleJComboBox __Action_JComboBox = null;
private boolean __error_wait = false; // Is there an error to be cleared up or Cancel?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public CheckTimeSeries_JDialog ( JFrame parent, CheckTimeSeries_Command command )
{	super(parent, true);
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
    String ValueToCheck = __ValueToCheck_JComboBox.getSelected();
    String CheckCriteria = __CheckCriteria_JComboBox.getSelected();
	String Value1 = __Value1_JTextField.getText().trim();
	String Value2 = __Value2_JTextField.getText().trim();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	String ProblemType = __ProblemType_JTextField.getText().trim();
	String MaxWarnings = __MaxWarnings_JTextField.getText().trim();
    String Flag = __Flag_JTextField.getText().trim();
    String FlagDesc = __FlagDesc_JTextField.getText().trim();
    String Action = __Action_JComboBox.getSelected();
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
    if ( ValueToCheck.length() > 0 ) {
        parameters.set ( "ValueToCheck", ValueToCheck );
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
	if ( AnalysisStart.length() > 0 ) {
		parameters.set ( "AnalysisStart", AnalysisStart );
	}
	if ( AnalysisEnd.length() > 0 ) {
		parameters.set ( "AnalysisEnd", AnalysisEnd );
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
    String ValueToCheck = __ValueToCheck_JComboBox.getSelected();
    String CheckCriteria = __CheckCriteria_JComboBox.getSelected();
	String Value1 = __Value1_JTextField.getText().trim();
	String Value2 = __Value2_JTextField.getText().trim();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	String ProblemType = __ProblemType_JTextField.getText().trim();
	String MaxWarnings = __MaxWarnings_JTextField.getText().trim();
	String Flag = __Flag_JTextField.getText().trim();
	String FlagDesc = __FlagDesc_JTextField.getText().trim();
    String Action = __Action_JComboBox.getSelected();
    __command.setCommandParameter ( "TSList", TSList );
	__command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "ValueToCheck", ValueToCheck );
    __command.setCommandParameter ( "CheckCriteria", CheckCriteria );
	__command.setCommandParameter ( "Value1", Value1 );
	__command.setCommandParameter ( "Value2", Value2 );
	__command.setCommandParameter ( "AnalysisStart", AnalysisStart );
	__command.setCommandParameter ( "AnalysisEnd", AnalysisEnd );
	__command.setCommandParameter ( "ProblemType", ProblemType );
	__command.setCommandParameter ( "MaxWarnings", MaxWarnings );
	__command.setCommandParameter ( "Flag", Flag );
	__command.setCommandParameter ( "FlagDesc", FlagDesc );
	__command.setCommandParameter ( "Action", Action );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__TSList_JComboBox = null;
    __TSID_JComboBox = null;
	__Value1_JTextField = null;
	__AnalysisStart_JTextField = null;
	__AnalysisEnd_JTextField = null;
	__ProblemType_JTextField = null;
	__cancel_JButton = null;
	__command_JTextArea = null;
	__command = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param title Dialog title.
@param command The command to edit.
*/
private void initialize ( JFrame parent, CheckTimeSeries_Command command )
{	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Check time series values and statistics for critical values." ), 
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "A warning will be generated for each case where a value matches the specified condition(s)." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Use the WriteCheckFile() command to save the results of all checks." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify dates with precision appropriate for the data, " +
		"use blank for all available data, OutputStart, or OutputEnd."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    __TSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel ( this, main_JPanel, __TSList_JComboBox, y );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
    List tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, y );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    List EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
        this, this, main_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Value to check:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ValueToCheck_JComboBox = new SimpleJComboBox ( 10, true );    // Allow edit
    __ValueToCheck_JComboBox.add ( "" );
    __ValueToCheck_JComboBox.add ( __command._DataValue );
    // TODO SAM 2009-04-23 Enable in the future.
    //__ValueToCheck_JComboBox.add ( __command._Statistic );
    __ValueToCheck_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __ValueToCheck_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "<html>Optional - check data values or statistic? (default=" + __command._DataValue +
        "). <b>Statistic is not enabled.</b></html>"), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Check criteria:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CheckCriteria_JComboBox = new SimpleJComboBox ( 12, false );    // Do not allow edit
    List checkCriteriaChoices = TSUtil_CheckTimeSeries.getCheckCriteriaChoices();
    __CheckCriteria_JComboBox.setData ( checkCriteriaChoices );
    __CheckCriteria_JComboBox.addItemListener ( this );
    __CheckCriteria_JComboBox.setMaximumRowCount(checkCriteriaChoices.size());
    JGUIUtil.addComponent(main_JPanel, __CheckCriteria_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - may require other parameters."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Value1:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Value1_JTextField = new JTextField ( 10 );
	__Value1_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Value1_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - minimum (or only) value to check."), 
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Value2:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Value2_JTextField = new JTextField ( 10 );
    __Value2_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Value2_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - maximum value in range, or other input to check."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Analysis start:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AnalysisStart_JTextField = new JTextField ( "", 20 );
    __AnalysisStart_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __AnalysisStart_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - analysis start date/time (default=full time series period)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Analysis end:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AnalysisEnd_JTextField = new JTextField ( "", 20 );
    __AnalysisEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __AnalysisEnd_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - analysis end date/time (default=full time series period)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Problem type:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ProblemType_JTextField = new JTextField ( 10 );
	__ProblemType_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __ProblemType_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - problem type to use in output (default=check criteria)."), 
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Maximum warnings:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MaxWarnings_JTextField = new JTextField ( 10 );
    __MaxWarnings_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __MaxWarnings_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - maximum # of warnings/time series (default=no limit)."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel,new JLabel( "Flag:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Flag_JTextField = new JTextField ( "", 10 );
    __Flag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Flag_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - flag to mark detected values."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Flag description:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __FlagDesc_JTextField = new JTextField ( 15 );
    __FlagDesc_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __FlagDesc_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Optional - description for flag."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Action:" ), 
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
    JGUIUtil.addComponent(main_JPanel, __Action_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Optional - action for matched values (default=no action)."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

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
{	String routine = "CheckTimeSeries_JDialog.refresh";
    String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String ValueToCheck = "";
    String CheckCriteria = "";
	String Value1 = "";
	String Value2 = "";
	String AnalysisStart = "";
	String AnalysisEnd = "";
	String ProblemType = "";
	String MaxWarnings = "";
	String Flag = "";
	String FlagDesc = "";
	String Action = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
        TSList = props.getValue ( "TSList" );
		TSID = props.getValue ( "TSID" );
        EnsembleID = props.getValue ( "EnsembleID" );
        ValueToCheck = props.getValue ( "ValueToCheck" );
        CheckCriteria = props.getValue ( "CheckCriteria" );
		Value1 = props.getValue ( "Value1" );
		Value2 = props.getValue ( "Value2" );
		AnalysisStart = props.getValue ( "AnalysisStart" );
		AnalysisEnd = props.getValue ( "AnalysisEnd" );
		ProblemType = props.getValue ( "ProblemType" );
		MaxWarnings = props.getValue ( "MaxWarnings" );
		Flag = props.getValue ( "Flag" );
		FlagDesc = props.getValue ( "FlagDesc" );
		Action = props.getValue ( "Action" );
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
        if ( ValueToCheck == null ) {
            // Select default...
            __ValueToCheck_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __ValueToCheck_JComboBox,ValueToCheck, JGUIUtil.NONE, null, null ) ) {
                __ValueToCheck_JComboBox.select ( ValueToCheck );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nValueToCheck value \"" + ValueToCheck +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
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
		if ( Value1 != null ) {
			__Value1_JTextField.setText ( Value1 );
		}
        if ( Value2 != null ) {
            __Value2_JTextField.setText ( Value2 );
        }
		if ( AnalysisStart != null ) {
			__AnalysisStart_JTextField.setText( AnalysisStart );
		}
		if ( AnalysisEnd != null ) {
			__AnalysisEnd_JTextField.setText ( AnalysisEnd );
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
        }
	}
	// Regardless, reset the command from the fields...
    TSList = __TSList_JComboBox.getSelected();
	TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    ValueToCheck = __ValueToCheck_JComboBox.getSelected();
    CheckCriteria = __CheckCriteria_JComboBox.getSelected();
    Value2 = __Value2_JTextField.getText().trim();
	Value1 = __Value1_JTextField.getText().trim();
	AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	ProblemType = __ProblemType_JTextField.getText().trim();
	MaxWarnings = __MaxWarnings_JTextField.getText().trim();
	Flag = __Flag_JTextField.getText().trim();
	FlagDesc = __FlagDesc_JTextField.getText().trim();
	Action = __Action_JComboBox.getSelected();
	props = new PropList ( __command.getCommandName() );
    props.add ( "TSList=" + TSList );
	props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
    props.add ( "ValueToCheck=" + ValueToCheck );
    props.add ( "CheckCriteria=" + CheckCriteria );
    props.add ( "Value1=" + Value1 );
    props.add ( "Value2=" + Value2 );
	props.add ( "AnalysisStart=" + AnalysisStart );
	props.add ( "AnalysisEnd=" + AnalysisEnd );
	props.add ( "ProblemType=" + ProblemType );
	props.add ( "MaxWarnings=" + MaxWarnings );
	props.add ( "Flag=" + Flag );
	props.add ( "FlagDesc=" + FlagDesc );
	props.add ( "Action=" + Action );
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