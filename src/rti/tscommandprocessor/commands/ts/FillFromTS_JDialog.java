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

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.Command;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Command editor dialog for the FillFromTS() command.
*/
public class FillFromTS_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, ListSelectionListener, WindowListener
{

private SimpleJButton	__cancel_JButton = null,// Cancel Button
			__ok_JButton = null;	// Ok Button
private FillFromTS_Command __command = null; // Command to edit
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
private JTextField __FillStart_JTextField;
private JTextField __FillEnd_JTextField;
private SimpleJComboBox __RecalcLimits_JComboBox = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public FillFromTS_JDialog ( JFrame parent, Command command )
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
		__command = null;
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
{   // Put together a list of parameters to check...
    PropList props = new PropList ( "" );
    String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String IndependentTSList = __IndependentTSList_JComboBox.getSelected();
    String IndependentTSID = __IndependentTSID_JComboBox.getSelected();
    String IndependentEnsembleID = __IndependentEnsembleID_JComboBox.getSelected();
    String FillStart = __FillStart_JTextField.getText().trim();
    String FillEnd = __FillEnd_JTextField.getText().trim();
    String RecalcLimits = __RecalcLimits_JComboBox.getSelected();
    //String TransferHow = __TransferHow_JComboBox.getSelected();
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
    if ( FillStart.length() > 0 ) {
        props.set ( "FillStart", FillStart );
    }
    if ( FillEnd.length() > 0 ) {
        props.set ( "FillEnd", FillEnd );
    }
    /*
    if ( TransferHow.length() > 0 ) {
        props.set ( "TransferHow", TransferHow );
    }
    */
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
    String FillStart = __FillStart_JTextField.getText().trim();
    String FillEnd = __FillEnd_JTextField.getText().trim();
    String RecalcLimits = __RecalcLimits_JComboBox.getSelected();
    //String TransferHow = __TransferHow_JComboBox.getSelected();
    __command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "IndependentTSList", IndependentTSList );
    __command.setCommandParameter ( "IndependentTSID", IndependentTSID );
    __command.setCommandParameter ( "IndependentEnsembleID", IndependentEnsembleID );
    __command.setCommandParameter ( "FillStart", FillStart );
    __command.setCommandParameter ( "FillEnd", FillEnd );
    //__command.setCommandParameter ( "TransferHow", TransferHow );
    __command.setCommandParameter ( "RecalcLimits", RecalcLimits );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__TSID_JComboBox = null;
	__cancel_JButton = null;
	__command_JTextArea = null;
	__command = null;
	__ok_JButton = null;
	__RecalcLimits_JComboBox = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Command command )
{   __command = (FillFromTS_Command)command;

	addWindowListener( this );

	Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

	// Main contents...

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Copy data values from the independent time series to replace missing values in the " +
		"dependent time series.  Only data in the fill period will be checked."),
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
		"use blank for all available data, OutputStart, or OutputEnd." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

     __TSList_JComboBox = new SimpleJComboBox(false);
     y = CommandEditorUtil.addTSListToEditorDialogPanel (
             this, main_JPanel, new JLabel ("Dependent TS List:"), __TSList_JComboBox, y );

     __TSID_JLabel = new JLabel ("TSID (for TSList=*MatchingTSID):");
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
             "Independent TSID (for Independent TSList=*MatchingTSID):");
     __IndependentTSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
     List<String> tsids2 = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
         (TSCommandProcessor)__command.getCommandProcessor(), __command );
     y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __IndependentTSID_JLabel, __IndependentTSID_JComboBox, tsids2, y );
     
     __IndependentEnsembleID_JLabel = new JLabel (
             "Independent EnsembleID (for Independent TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
     __IndependentEnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
     y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
             this, this, main_JPanel, __IndependentEnsembleID_JLabel, __IndependentEnsembleID_JComboBox, EnsembleIDs, y );

     JGUIUtil.addComponent(main_JPanel,new JLabel( "Fill start date/time:"),
         0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
     __FillStart_JTextField = new JTextField ( "", 10 );
     __FillStart_JTextField.addKeyListener ( this );
         JGUIUtil.addComponent(main_JPanel, __FillStart_JTextField,
         1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
     JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - start of period to fill (default=fill all)."),
         3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

     JGUIUtil.addComponent(main_JPanel,new JLabel("Fill end date/time:"),
         0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
     __FillEnd_JTextField = new JTextField ( "", 10 );
     __FillEnd_JTextField.addKeyListener ( this );
     JGUIUtil.addComponent(main_JPanel, __FillEnd_JTextField,
         1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
     JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - end of period to fill (default=fill all)."),
         3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
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
        new JLabel( "Optional - recalculate original data limits after fill (default=" + __command._False + ")."), 
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
{   String routine = "FillFromTS_JDialog.refresh";
    String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String IndependentTSList = "";
    String IndependentTSID = "";
    String IndependentEnsembleID = "";
    //String TransferHow = "";
    String FillStart = "";
    String FillEnd = "";
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
        FillStart = props.getValue ( "FillStart" );
        FillEnd = props.getValue ( "FillEnd" );
        //TransferHow = props.getValue ( "TransferHow" );
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
        if (    JGUIUtil.isSimpleJComboBoxItem( __IndependentTSID_JComboBox, IndependentTSID,
                JGUIUtil.NONE, null, null ) ) {
                __IndependentTSID_JComboBox.select ( IndependentTSID );
        }
        else {  // Automatically add to the list after the blank...
            if ( (IndependentTSID != null) && (IndependentTSID.length() > 0) ) {
                __IndependentTSID_JComboBox.insertItemAt ( IndependentTSID, 1 );
                // Select...
                __IndependentTSID_JComboBox.select ( IndependentTSID );
            }
            else {  // Select the blank...
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
        if ( FillStart != null ) {
            __FillStart_JTextField.setText ( FillStart );
        }
        if ( FillEnd != null ) {
            __FillEnd_JTextField.setText ( FillEnd );
        }
        /*
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
        */
        if ( RecalcLimits != null &&
            JGUIUtil.isSimpleJComboBoxItem( __RecalcLimits_JComboBox, 
            RecalcLimits, JGUIUtil.NONE, null, null ) ) {
            __RecalcLimits_JComboBox.select ( RecalcLimits );
        }
    }
    // Regardless, reset the command from the fields...
    TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    IndependentTSList = __IndependentTSList_JComboBox.getSelected();
    IndependentTSID = __IndependentTSID_JComboBox.getSelected();
    IndependentEnsembleID = __IndependentEnsembleID_JComboBox.getSelected();
    FillStart = __FillStart_JTextField.getText().trim();
    FillEnd = __FillEnd_JTextField.getText().trim();
    RecalcLimits = __RecalcLimits_JComboBox.getSelected();
    //TransferHow = __TransferHow_JComboBox.getSelected();
    props = new PropList ( __command.getCommandName() );
    props.add ( "TSList=" + TSList );
    props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
    props.add ( "IndependentTSList=" + IndependentTSList );
    props.add ( "IndependentTSID=" + IndependentTSID );
    props.add ( "IndependentEnsembleID=" + IndependentEnsembleID );
    props.add ( "FillStart=" + FillStart );
    props.add ( "FillEnd=" + FillEnd );
    //props.add ( "TransferHow=" + TransferHow );
    props.add ( "RecalcLimits=" + RecalcLimits);
    __command_JTextArea.setText( __command.toString ( props ) );
}

/**
React to the user response.
@param ok if false, then the edit is cancelled.  If true, the edit is committed
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

} // end fillFromTS_JDialog
