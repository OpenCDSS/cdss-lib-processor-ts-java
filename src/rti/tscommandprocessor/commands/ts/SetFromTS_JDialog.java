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

import java.util.List;
import java.util.Vector;

import RTi.TS.TSUtil;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.Command;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Editor dialog for the SetFromTS() command.
*/
public class SetFromTS_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, ListSelectionListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;// Cancel Button
private SimpleJButton __ok_JButton = null;	// Ok Button
private SetFromTS_Command __command = null;// Command to edit
private JTextField __SetStart_JTextField;
private JTextField __SetEnd_JTextField; // Text fields for set period.
private JTextArea __command_JTextArea=null;// Command as JTextField
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
private SimpleJComboBox	__TransferHow_JComboBox =null;	// Indicates how to transfer data.
private SimpleJComboBox __HandleMissingHow_JComboBox = null; // Indicates how to handle missing data.
private SimpleJComboBox __SetDataFlags_JComboBox = null;
private SimpleJComboBox __RecalcLimits_JComboBox = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public SetFromTS_JDialog ( JFrame parent, Command command )
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
	else if ( o == __ok_JButton ) {
		refresh();
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
    if ( RecalcLimits.length() > 0 ) {
        props.set( "RecalcLimits", RecalcLimits );
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
    __command.setCommandParameter ( "RecalcLimits", RecalcLimits );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{   __TSID_JComboBox = null;
    __cancel_JButton = null;
    __command_JTextArea = null;
    __command = null;
    __ok_JButton = null;
    __HandleMissingHow_JComboBox = null;
    __RecalcLimits_JComboBox = null;
    super.finalize ();
}


/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Command command )
{   __command = (SetFromTS_Command)command;

	addWindowListener( this );

	Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

	// Main contents...

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Copy data values from the independent time" +
		" series to replace values in the dependent time series." ), 
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
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
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Use a SetOutputPeriod() command if the dependent time series period will be extended." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify dates with precision appropriate for the data, " +
		"blank for all available data, OutputStart, or OutputEnd." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The set period is for the independent time series."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __TSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel (
            this, main_JPanel, new JLabel ("Dependent TS List:"), __TSList_JComboBox, y );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, y );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
            this, this, main_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );
    
    __IndependentTSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel (
            this, main_JPanel, new JLabel ("Independent TS List:"), __IndependentTSList_JComboBox, y );

    __IndependentTSID_JLabel = new JLabel (
            "Independent TSID (for Independent TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __IndependentTSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
    List<String> tsids2 = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __IndependentTSID_JLabel, __IndependentTSID_JComboBox, tsids2, y );
    
    __IndependentEnsembleID_JLabel = new JLabel (
            "Independent EnsembleID (for Independent TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __IndependentEnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
            this, this, main_JPanel, __IndependentEnsembleID_JLabel, __IndependentEnsembleID_JComboBox, EnsembleIDs, y );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Set start:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SetStart_JTextField = new JTextField (20);
    __SetStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __SetStart_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - set start (default is full period)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Set End:"), 
        0, ++y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SetEnd_JTextField = new JTextField (20);
    __SetEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __SetEnd_JTextField,
        1, y, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - set end (default is full period)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

	JGUIUtil.addComponent(main_JPanel, new JLabel ( "Transfer data how:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TransferHow_JComboBox = new SimpleJComboBox ( false );
	__TransferHow_JComboBox.addItem ( TSUtil.TRANSFER_BYDATETIME );
	__TransferHow_JComboBox.addItem ( TSUtil.TRANSFER_SEQUENTIALLY );
	__TransferHow_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __TransferHow_JComboBox,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Required - how are data values transferred?"), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Handle missing data how?:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __HandleMissingHow_JComboBox = new SimpleJComboBox ( false );
    __HandleMissingHow_JComboBox.addItem ( "" );
    __HandleMissingHow_JComboBox.addItem ( __command._IgnoreMissing );
    __HandleMissingHow_JComboBox.addItem ( __command._SetMissing );
    __HandleMissingHow_JComboBox.addItem ( __command._SetOnlyMissingValues );
    __HandleMissingHow_JComboBox.select ( 0 );
    __HandleMissingHow_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __HandleMissingHow_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Optional - missing in independent handled how? (default=" +
            __command._SetMissing + ")."), 
            3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("Set data flags?:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SetDataFlags_JComboBox = new SimpleJComboBox ( false );
    List<String> choices = new Vector();
    choices.add ("");
    choices.add ( __command._False );
    choices.add ( __command._True );
    __SetDataFlags_JComboBox.setData ( choices );
    __SetDataFlags_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __SetDataFlags_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - should data flags be copied (default=" + __command._True + ")."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Recalculate limits:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __RecalcLimits_JComboBox = new SimpleJComboBox ( false );
    __RecalcLimits_JComboBox.addItem ( "" );
    __RecalcLimits_JComboBox.addItem ( __command._False );
    __RecalcLimits_JComboBox.addItem ( __command._True );
    __RecalcLimits_JComboBox.select ( 0 );
    __RecalcLimits_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __RecalcLimits_JComboBox,
    1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel,
        new JLabel( "Optional - recalculate original data limits after set (default=" + __command._False + ")."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

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

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );

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
{	String routine = "SetFromTS_JDialog.refresh";
    String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String IndependentTSList = "";
    String IndependentTSID = "";
    String IndependentEnsembleID = "";
    String TransferHow = "";
    String SetStart = "";
    String SetEnd = "";
    String HandleMissingHow = "";
    String SetDataFlags = "";
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
        TransferHow = props.getValue ( "TransferHow" );
        HandleMissingHow = props.getValue ( "HandleMissingHow" );
        SetDataFlags = props.getValue ( "SetDataFlags" );
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
    props.add ( "RecalcLimits=" + RecalcLimits);
    __command_JTextArea.setText( __command.toString ( props ) );
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed
and the dialog is closed.
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