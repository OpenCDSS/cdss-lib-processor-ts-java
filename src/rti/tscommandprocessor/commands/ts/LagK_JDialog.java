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
import RTi.Util.IO.Command;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

public class LagK_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{

private SimpleJButton	__cancel_JButton = null,// Cancel button
			__ok_JButton = null;	// Ok button
private LagK_Command __command = null;	// Command to edit.
private JTextArea __command_JTextArea=null;// Command as JTextField
private JTextField __Alias_JTextField = null;// Field for time series alias
private SimpleJComboBox __TSID_JComboBox = null;// Time series available to operate on.
private SimpleJComboBox __ObsTSID_JComboBox = null;// Observed time series
private SimpleJComboBox	__FillNearest_JComboBox = null;
private JTextField	__DefaultFlow_JTextField = null;
private JTextField	__Lag_JTextField = null;
private JTextField	__K_JTextField = null;
private JTextArea   __InflowStates_JTextArea = null;
private JTextArea   __OutflowStates_JTextArea = null;

private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Whether OK has been pressed.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public LagK_JDialog ( JFrame parent, Command command )
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
	String ObsTSID = __ObsTSID_JComboBox.getSelected();
	String FillNearest = __FillNearest_JComboBox.getSelected();
	String DefaultFlow = __DefaultFlow_JTextField.getText().trim();
	String Lag = __Lag_JTextField.getText().trim();
	String K = __K_JTextField.getText().trim();
	String InflowStates = __InflowStates_JTextArea.getText().trim();
	String OutflowStates = __OutflowStates_JTextArea.getText().trim();
	__error_wait = false;

	if ( Alias.length() > 0 ) {
		props.set ( "Alias", Alias );
	}
	if ( (TSID != null) && (TSID.length() > 0) ) {
		props.set ( "TSID", TSID );
	}
	if ( (ObsTSID != null) && (ObsTSID.length() > 0) ) {
		props.set ( "ObsTSID", ObsTSID );
	}
	if ( (FillNearest != null) && (FillNearest.length() > 0) ) {
		props.set ( "FillNearest", FillNearest );
	}
	if ( (DefaultFlow != null) && (DefaultFlow.length() > 0) ) {
		props.set ( "DefaultFlow", DefaultFlow );
	}
	if ( (Lag != null) && (Lag.length() > 0) ) {
		props.set ( "Lag", Lag );
	}
	if ( (K != null) && (K.length() > 0) ) {
		props.set ( "K", K );
	}
	if ( (InflowStates != null) && (InflowStates.length() > 0) ) {
		props.set ( "InflowStates", InflowStates );
	}
	if ( (OutflowStates != null) && (OutflowStates.length() > 0) ) {
	    props.set ( "OutflowStates", OutflowStates );
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
    String ObsTSID = __ObsTSID_JComboBox.getSelected();
    String FillNearest = __FillNearest_JComboBox.getSelected();
    String DefaultFlow = __DefaultFlow_JTextField.getText().trim();
    String Lag = __Lag_JTextField.getText().trim();
    String K = __K_JTextField.getText().trim();
    String InflowStates = __InflowStates_JTextArea.getText().trim();
    String OutflowStates = __OutflowStates_JTextArea.getText().trim();
	__command.setCommandParameter ( "Alias", Alias );
	__command.setCommandParameter ( "TSID", TSID );
	__command.setCommandParameter ( "ObsTSID", ObsTSID );
	__command.setCommandParameter ( "FillNearest", FillNearest );
	__command.setCommandParameter ( "DefaultFlow", DefaultFlow );
	__command.setCommandParameter ( "Lag", Lag);
	__command.setCommandParameter ( "K", K );
	__command.setCommandParameter ( "InflowStates", InflowStates );
	__command.setCommandParameter ( "OutflowStates", OutflowStates );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__Alias_JTextField = null;
	__TSID_JComboBox = null;
	__InflowStates_JTextArea = null;
	__FillNearest_JComboBox = null;
	__DefaultFlow_JTextField = null;
	__Lag_JTextField = null;
	__K_JTextField = null;
	__cancel_JButton = null;
	__command_JTextArea = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param title Dialog title.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Command command )
{	__command = (LagK_Command)command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Lag and attenuate a time series, creating a new time series." ),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
       "The time series to be routed cannot contain missing values." ),
       0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
       "The observed time series is used for filling, then FillNearest, and finally DefaultFlow." ),
       0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"See the documentation for a complete description of the algorithm." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Time series alias:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Alias_JTextField = new JTextField ( "" );
	__Alias_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Often the location from the TSID, or a short string."), 
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel( "Time series to lag (TSID):"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TSID_JComboBox = new SimpleJComboBox ( true );	// Allow edit
	Vector tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
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
    
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Observed time series for filling:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ObsTSID_JComboBox = new SimpleJComboBox ( true );    // Allow edit
    Vector tsids2 = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    if ( tsids2 == null ) {
        // User will not be able to select anything.
        tsids2 = new Vector();
    }
    if ( tsids.size() == 0 ) {
        tsids2.add("");
    }
    else {
        // Add a blank at the start
        tsids2.insertElementAt("", 0);
    }
    __ObsTSID_JComboBox.setData ( tsids2 );
    __ObsTSID_JComboBox.addItemListener ( this );
    __ObsTSID_JComboBox.getJTextComponent().addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __ObsTSID_JComboBox,
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Fill nearest?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__FillNearest_JComboBox = new SimpleJComboBox(false);
	__FillNearest_JComboBox.add ( "" );
	__FillNearest_JComboBox.add ( __command._False );
	__FillNearest_JComboBox.add ( __command._True );
	__FillNearest_JComboBox.select ( 0 );
	__FillNearest_JComboBox.addActionListener (this);
	JGUIUtil.addComponent(main_JPanel, __FillNearest_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - fill missing with nearest data from TSID? (default=False)."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Default flow:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DefaultFlow_JTextField = new JTextField (10);
	__DefaultFlow_JTextField.addKeyListener (this);
	JGUIUtil.addComponent(main_JPanel, __DefaultFlow_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - use if no other filling works (default=0)."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Lag:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Lag_JTextField = new JTextField (10);
    __Lag_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __Lag_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Required - lag in time series base interval time units."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("K:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __K_JTextField = new JTextField (10);
    __K_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __K_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Required - attenuation in time series base interval time units."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Inflow states:" ),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InflowStates_JTextArea = new JTextArea ( 3, 25 );
    __InflowStates_JTextArea.setLineWrap ( true );
    __InflowStates_JTextArea.setWrapStyleWord ( true );
    __InflowStates_JTextArea.addKeyListener ( this );
    // Make 3-high
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__InflowStates_JTextArea),
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - separate values by commas (default=0 for all)."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Outflow states:" ),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutflowStates_JTextArea = new JTextArea ( 3, 25 );
    __OutflowStates_JTextArea.setLineWrap ( true );
    __OutflowStates_JTextArea.setWrapStyleWord ( true );
    __OutflowStates_JTextArea.addKeyListener ( this );
    // Make 3-high
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__OutflowStates_JTextArea),
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - separate values by commas (default=0 for all)."), 
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

	setTitle ( "Edit TS Alias = " + __command.getCommandName() + "() Command" );

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
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = "LagK_JDialog.refresh";
	String Alias = "";
	String TSID = "";
	String ObsTSID = "";
	String FillNearest = "";
	String DefaultFlow = "";
	String Lag = "";
	String K = "";
	String InflowStates = "";
	String OutflowStates = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		Alias = props.getValue ( "Alias" );
		TSID = props.getValue ( "TSID" );
		ObsTSID = props.getValue ( "ObsTSID" );
		FillNearest = props.getValue ( "FillNearest" );
		DefaultFlow = props.getValue ( "DefaultFlow" );
		Lag = props.getValue ( "Lag" );
		K = props.getValue ( "K" );
	    InflowStates = props.getValue ( "InflowStates" );
	    OutflowStates = props.getValue ( "OutflowStates" );
		if ( Alias != null ) {
			__Alias_JTextField.setText ( Alias );
		}
		// Now select the item in the list.  If not a match, print a warning.
		if (	JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID,
				JGUIUtil.NONE, null, null ) ) {
				__TSID_JComboBox.select ( TSID );
		}
		else {	// Automatically add to the list after the blank...
			if ( (TSID != null) && (TSID.length() > 0) ) {
				__TSID_JComboBox.insertItemAt ( TSID, 1 );
				// Select...
				__TSID_JComboBox.select ( TSID );
			}
			else {	// Do not select anything...
			}
		}
       if (  JGUIUtil.isSimpleJComboBoxItem( __ObsTSID_JComboBox, ObsTSID,
                JGUIUtil.NONE, null, null ) ) {
                __ObsTSID_JComboBox.select ( ObsTSID );
        }
        else {
            // Automatically add to the list after the blank...
            if ( (ObsTSID != null) && (ObsTSID.length() > 0) ) {
                __ObsTSID_JComboBox.insertItemAt ( ObsTSID, 1 );
                // Select...
                __ObsTSID_JComboBox.select ( ObsTSID );
            }
            else {
                // Do not select anything...
            }
        }
		if ( FillNearest == null ) {
			// Select default...
			__FillNearest_JComboBox.select ( 0 );
		}
		else {
		    if ( JGUIUtil.isSimpleJComboBoxItem(
				__FillNearest_JComboBox, FillNearest, JGUIUtil.NONE, null, null ) ) {
				__FillNearest_JComboBox.select ( FillNearest );
			}
			else {	Message.printWarning ( 1, routine,
				"Existing command references an invalid\nFillNearest value \"" +
				FillNearest +
				"\".  Select a different value or Cancel.");
				__error_wait = true;
			}
		}
		if ( DefaultFlow != null ) {
			__DefaultFlow_JTextField.setText ( DefaultFlow );
		}
		if ( Lag != null ) {
			__Lag_JTextField.setText( Lag );
		}
		if ( K != null ) {
			__K_JTextField.setText ( K );
		}
        if ( InflowStates != null ) {
            __InflowStates_JTextArea.setText ( InflowStates );
        }
        if ( OutflowStates != null ) {
            __OutflowStates_JTextArea.setText ( OutflowStates );
        }
	}
	// Regardless, reset the command from the fields...
	Alias = __Alias_JTextField.getText().trim();
	TSID = __TSID_JComboBox.getSelected();
	ObsTSID = __ObsTSID_JComboBox.getSelected();
	FillNearest = __FillNearest_JComboBox.getSelected();
	DefaultFlow = __DefaultFlow_JTextField.getText();
	Lag = __Lag_JTextField.getText().trim();
	K = __K_JTextField.getText().trim();
    InflowStates = __InflowStates_JTextArea.getText().trim();
    OutflowStates = __OutflowStates_JTextArea.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "Alias=" + Alias );
	props.add ( "TSID=" + TSID );
	props.add ( "ObsTSID=" + ObsTSID );
	props.add ( "FillNearest=" + FillNearest );
	props.add ( "DefaultFlow=" + DefaultFlow );
	props.add ( "Lag=" + Lag );
	props.add ( "K=" + K );
	props.add ( "InflowStates=" + InflowStates );
	props.add ( "OutflowStates=" + OutflowStates );
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