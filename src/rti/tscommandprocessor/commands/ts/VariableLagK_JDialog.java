package rti.tscommandprocessor.commands.ts;

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
implements ActionListener, ItemListener, KeyListener, WindowListener
{

private SimpleJButton	__cancel_JButton = null,// Cancel button
			__ok_JButton = null;	// Ok button
private JFrame __parent_JFrame = null;	// parent JFrame
private VariableLagK_Command __command = null;	// Command to edit.
private JTextArea __command_JTextArea=null;// Command as JTextField
private JTextField __Alias_JTextField = null;// Field for time series alias
private SimpleJComboBox __TSID_JComboBox = null;// Time series available to operate on.
private JTextArea __NewTSID_JTextArea = null; // New TSID.
private SimpleJButton __edit_JButton = null;	// Edit button
private SimpleJButton __clear_JButton = null;	// Clear NewTSID button
private JTextArea __Lag_JTextArea = null;
private JTextArea __K_JTextArea = null;
private JTextField __DataUnits_JTextField = null;
private SimpleJComboBox __LagInterval_JComboBox = null;
private JTextArea __InputStates_JTextArea = null;
private JTextArea __OutputStates_JTextArea = null;

private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Whether OK has been pressed.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public VariableLagK_JDialog ( JFrame parent, VariableLagK_Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();
    String routine = "VariableLagK_JDialog.actionPerformed";

	if ( o == __cancel_JButton ) {
		response ( false );
	}
    else if ( o == __clear_JButton ) {
		__NewTSID_JTextArea.setText ( "" );
        refresh();
	}
	else if ( o == __edit_JButton ) {
		// Edit the NewTSID in the dialog.  It is OK for the string to
		// be blank.
		String NewTSID = __NewTSID_JTextArea.getText().trim();
		TSIdent tsident;
		try {	if ( NewTSID.length() == 0 ) {
				tsident = new TSIdent();
			}
			else {	tsident = new TSIdent ( NewTSID );
			}
			TSIdent tsident2=(new TSIdent_JDialog ( __parent_JFrame,
				true, tsident, null )).response();
			if ( tsident2 != null ) {
				__NewTSID_JTextArea.setText (
					tsident2.toString(true) );
				refresh();
			}
		}
		catch ( Exception e ) {
			Message.printWarning ( 1, routine,
			"Error creating time series identifier from \"" +
			NewTSID + "\"." );
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
    String DataUnits = __DataUnits_JTextField.getText().trim();
    String LagInterval = __LagInterval_JComboBox.getSelected().trim();
	String InputStates = __InputStates_JTextArea.getText().trim();
	String OutputStates = __OutputStates_JTextArea.getText().trim();
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
	if ( (DataUnits != null) && (DataUnits.length() > 0) ) {
		props.set ( "DataUnits", DataUnits );
	}
	if ( (LagInterval != null) && (LagInterval.length() > 0) ) {
		props.set ( "LagInterval", LagInterval );
	}
	if ( (InputStates != null) && (InputStates.length() > 0) ) {
		props.set ( "InputStates", InputStates );
	}
	if ( (OutputStates != null) && (OutputStates.length() > 0) ) {
	    props.set ( "OutputStates", OutputStates );
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
    String DataUnits = __DataUnits_JTextField.getText().trim();
    String LagInterval = __LagInterval_JComboBox.getSelected().trim();
    String InputStates = __InputStates_JTextArea.getText().trim();
    String OutputStates = __OutputStates_JTextArea.getText().trim();
	__command.setCommandParameter ( "Alias", Alias );
	__command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "NewTSID", NewTSID );
	__command.setCommandParameter ( "Lag", Lag);
	__command.setCommandParameter ( "K", K );
	__command.setCommandParameter ( "DataUnits", DataUnits );
	__command.setCommandParameter ( "LagInterval", LagInterval );
	__command.setCommandParameter ( "InputStates", InputStates );
	__command.setCommandParameter ( "OutputStates", OutputStates );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__Alias_JTextField = null;
	__TSID_JComboBox = null;
    __NewTSID_JTextArea = null;
	__InputStates_JTextArea = null;
	__Lag_JTextArea = null;
	__K_JTextArea = null;
	__DataUnits_JTextField = null;
    __LagInterval_JComboBox = null;
	__cancel_JButton = null;
	__command_JTextArea = null;
	__ok_JButton = null;
    __parent_JFrame = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param title Dialog title.
@param command Command to edit.
*/
private void initialize ( JFrame parent, VariableLagK_Command command )
{	__parent_JFrame = parent;
    __command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Lag and attenuate a time series, creating a new time series using variable Lag and K technique." ),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
       "The time series to be routed cannot contain missing values." ),
       0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"See the documentation for a complete description of the algorithm." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
       "<HTML><B>The input and output state parameters are currently ignored - states default to zero.</B></HTML>." ),
       0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel( "Time series to lag (TSID):"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TSID_JComboBox = new SimpleJComboBox ( true );	// Allow edit
	List tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
			(TSCommandProcessor)__command.getCommandProcessor(), __command );
	if ( tsids == null ) {
		// User will not be able to select anything.
		tsids = new Vector();
	}
	__TSID_JComboBox.setData ( tsids );
	__TSID_JComboBox.addItemListener ( this );
	__TSID_JComboBox.getJTextComponent().addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __TSID_JComboBox,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "New time series ID:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewTSID_JTextArea = new JTextArea ( 3, 25 );
    __NewTSID_JTextArea.setEditable(false);
	__NewTSID_JTextArea.setLineWrap ( true );
	__NewTSID_JTextArea.setWrapStyleWord ( true );
	__NewTSID_JTextArea.addKeyListener ( this );
	// Make 3-high to fit in the edit button...
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__NewTSID_JTextArea),
		1, y, 2, 3, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Specify to avoid confusion with TSID from original TS."),
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	y += 2;
    JGUIUtil.addComponent(main_JPanel, (__edit_JButton = new SimpleJButton ( "Edit", "Edit", this ) ),
		3, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(main_JPanel, (__clear_JButton = new SimpleJButton ( "Clear", "Clear", this ) ),
		4, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Data units:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataUnits_JTextField = new JTextField (10);
    __DataUnits_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __DataUnits_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Required - units of Lag and K data values, compatible with time series."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel("Lag interval: "),
        0, ++y, 1, 1, 0, 0, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LagInterval_JComboBox = new SimpleJComboBox(false);
    List interval_Vector = TimeInterval.getTimeIntervalBaseChoices(
        TimeInterval.MINUTE, TimeInterval.YEAR, 1, false);
    interval_Vector.add ( 0, "" );
    __LagInterval_JComboBox.setData ( interval_Vector );
    __LagInterval_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __LagInterval_JComboBox,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - Lag and K interval units."),
        3, y, 1, 1, 0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Lag:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Lag_JTextArea = new JTextArea (3, 25);
    __Lag_JTextArea.setLineWrap ( true );
    __Lag_JTextArea.setWrapStyleWord ( true );
    __Lag_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__Lag_JTextArea),
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - value,Lag;value,Lag pairs."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("K:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __K_JTextArea = new JTextArea (3, 25);
    __K_JTextArea.setLineWrap ( true );
    __K_JTextArea.setWrapStyleWord ( true );
    __K_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__K_JTextArea),
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - value,K;value,K pairs."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input time series states:" ),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputStates_JTextArea = new JTextArea ( 3, 25 );
    __InputStates_JTextArea.setLineWrap ( true );
    __InputStates_JTextArea.setWrapStyleWord ( true );
    __InputStates_JTextArea.addKeyListener ( this );
    // Make 3-high
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__InputStates_JTextArea),
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - separate values by commas (default=0 for all)."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output time series states:" ),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputStates_JTextArea = new JTextArea ( 3, 25 );
    __OutputStates_JTextArea.setLineWrap ( true );
    __OutputStates_JTextArea.setWrapStyleWord ( true );
    __OutputStates_JTextArea.addKeyListener ( this );
    // Make 3-high
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__OutputStates_JTextArea),
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - separate values by commas (default=0 for all)."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Time series alias:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new JTextField ( "" );
    __Alias_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
    1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
    "Optional - alternate ID, (e.g., location from the TSID)."),
    3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

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
@return true if the edits were committed, false if the user cancelled.
*/
public boolean ok ()
{	return __ok;
}

/**
Refresh the command from component contents.
*/
private void refresh ()
{	String routine = "LagK_JDialog.refresh";
	String Alias = "";
	String TSID = "";
    String NewTSID = "";
	String Lag = "";
	String K = "";
	String DataUnits = "";
	String LagInterval = "";
	String InputStates = "";
	String OutputStates = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		Alias = props.getValue ( "Alias" );
        TSID = props.getValue ( "TSID" );
        NewTSID = props.getValue ( "NewTSID" );
		Lag = props.getValue ( "Lag" );
		K = props.getValue ( "K" );
		DataUnits = props.getValue ( "DataUnits" );
		LagInterval = props.getValue ( "LagInterval" );
	    InputStates = props.getValue ( "InputStates" );
	    OutputStates = props.getValue ( "OutputStates" );
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
        if ( DataUnits != null ) {
            __DataUnits_JTextField.setText ( DataUnits );
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
        if ( InputStates != null ) {
            __InputStates_JTextArea.setText ( InputStates );
        }
        if ( OutputStates != null ) {
            __OutputStates_JTextArea.setText ( OutputStates );
        }
	}
	// Regardless, reset the command from the fields...
	TSID = __TSID_JComboBox.getSelected();
    NewTSID = __NewTSID_JTextArea.getText().trim();
    DataUnits = __DataUnits_JTextField.getText().trim();
    LagInterval = __LagInterval_JComboBox.getSelected();
	Lag = __Lag_JTextArea.getText().trim();
	K = __K_JTextArea.getText().trim();
    InputStates = __InputStates_JTextArea.getText().trim();
    OutputStates = __OutputStates_JTextArea.getText().trim();
    Alias = __Alias_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "TSID=" + TSID );
    props.add ( "NewTSID=" + NewTSID );
    props.add ( "DataUnits=" + DataUnits );
    props.add ( "LagInterval=" + LagInterval );
	props.add ( "Lag=" + Lag );
	props.add ( "K=" + K );
	props.add ( "InputStates=" + InputStates );
	props.add ( "OutputStates=" + OutputStates );
	props.add ( "Alias=" + Alias );
	__command_JTextArea.setText( __command.toString ( props ) );
}

/**
React to the user response.
@param ok if false, then the edit is cancelled.  If true, the edit is committed
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