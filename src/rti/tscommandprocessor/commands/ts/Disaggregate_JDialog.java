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

public class Disaggregate_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{

private SimpleJButton	__cancel_JButton = null,// Cancel Button
			__ok_JButton = null;	// Ok Button
private Disaggregate_Command __command = null;
private JTextField __Alias_JTextField = null;// Alias for new time series
private SimpleJComboBox __TSID_JComboBox = null;// Time series available to operate on.
private JTextArea __command_JTextArea=null;
private SimpleJComboBox	__Method_JComboBox = null;// Disaggregation method.
private JTextField __NewInterval_JTextField = null; // New interval.
private JTextField __NewDataType_JTextField = null; // New data type.
private JTextField __NewUnits_JTextField = null;// New data units.
private boolean __error_wait = false;
private boolean __first_time = true;
private boolean __ok = false;       // Whether OK has been pressed.

/**
Command editor dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public Disaggregate_JDialog ( JFrame parent, Command command )
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
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{   // Put together a list of parameters to check...
    PropList props = new PropList ( "" );
    String Alias = __Alias_JTextField.getText().trim();
    String TSID = __TSID_JComboBox.getSelected();
    String Method = __Method_JComboBox.getSelected();
    String NewInterval = __NewInterval_JTextField.getText().trim();
    String NewDataType = __NewDataType_JTextField.getText().trim();
    String NewUnits = __NewUnits_JTextField.getText().trim();
    __error_wait = false;

    if ( Alias.length() > 0 ) {
        props.set ( "Alias", Alias );
    }
    if ( (TSID != null) && (TSID.length() > 0) ) {
        props.set ( "TSID", TSID );
    }
    if ( (Method != null) && (Method.length() > 0) ) {
        props.set ( "Method", Method );
    }
    if ( (NewInterval != null) && (NewInterval.length() > 0) ) {
        props.set ( "NewInterval", NewInterval );
    }
    if ( (NewDataType != null) && (NewDataType.length() > 0) ) {
        props.set ( "NewDataType", NewDataType );
    }
    if ( (NewUnits != null) && (NewUnits.length() > 0) ) {
        props.set ( "NewUnits", NewUnits );
    }
    try {   // This will warn the user...
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
{   String Alias = __Alias_JTextField.getText().trim();
    String TSID = __TSID_JComboBox.getSelected();
    String Method = __Method_JComboBox.getSelected();
    String NewInterval = __NewInterval_JTextField.getText().trim();
    String NewDataType = __NewDataType_JTextField.getText().trim();
    String NewUnits = __NewUnits_JTextField.getText().trim();
    __command.setCommandParameter ( "Alias", Alias );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "Method", Method );
    __command.setCommandParameter ( "NewInterval", NewInterval );
    __command.setCommandParameter ( "NewDataType", NewDataType );
    __command.setCommandParameter ( "NewUnits", NewUnits );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__cancel_JButton = null;
	__Alias_JTextField = null;
	__command_JTextArea = null;
	__command = null;
	__NewInterval_JTextField = null;
	__NewDataType_JTextField = null;
	__NewUnits_JTextField = null;
	__Method_JComboBox = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Command command )
{	__command = (Disaggregate_Command)command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Disaggregation converts a longer-interval time series to a shorter interval."),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify the new interal as Nbase, where base is Minute, Hour, Day (ommitting N assumes a value of 1)."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The new interval must be evenly divisible into the old interval."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The Ormsbee method is only enabled to disaggregate 1Day to 6Hour time series."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The SameValue method can disaggregate Year->Month, Month->Day, Day->NHour, Hour->NMinute."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Time series alias:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Alias_JTextField = new JTextField ( "" );
	__Alias_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Original time series:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TSID_JComboBox = new SimpleJComboBox ( true );    // Allow edit
    Vector tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    __TSID_JComboBox.setData ( tsids );
    __TSID_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __TSID_JComboBox,
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Method:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Method_JComboBox = new SimpleJComboBox ( false );
	__Method_JComboBox.addItem ( __command._Ormsbee );
	__Method_JComboBox.addItem ( __command._SameValue );
	__Method_JComboBox.select ( 1 );
	__Method_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __Method_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "New interval:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewInterval_JTextField = new JTextField ( "", 20);
	__NewInterval_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __NewInterval_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required - e.g., Day, 6Hour."), 
    3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "New data type:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewDataType_JTextField = new JTextField ( "", 20);
	__NewDataType_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __NewDataType_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Optional - default is same as original."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "New data units:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewUnits_JTextField = new JTextField ( "", 20);
	__NewUnits_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __NewUnits_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Optional - default is same as original."), 
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

	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );

	// Visualize it...

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
{	// Any change needs to refresh the command...
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
{   return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{   String routine = "Disaggregate_JDialog.refresh";
    String Alias = "";
    String TSID = "";
    String Method = "";
    String NewInterval = "";
    String NewDataType = "";
    String NewUnits = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        Alias = props.getValue ( "Alias" );
        TSID = props.getValue ( "TSID" );
        Method = props.getValue ( "Method" );
        NewInterval = props.getValue ( "NewInterval" );
        NewDataType = props.getValue ( "NewDataType" );
        NewUnits = props.getValue ( "NewUnits" );
        if ( Alias != null ) {
            __Alias_JTextField.setText ( Alias );
        }
        // Now select the item in the list.  If not a match, print a warning.
        if ( JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID, JGUIUtil.NONE, null, null ) ) {
            __TSID_JComboBox.select ( TSID );
        }
        else {
            // Automatically add to the list...
            if ( (TSID != null) && (TSID.length() > 0) ) {
                __TSID_JComboBox.insertItemAt ( TSID, 0 );
                // Select...
                __TSID_JComboBox.select ( TSID );
            }
            else {
                // Select the first choice...
                if ( __TSID_JComboBox.getItemCount() > 0 ) {
                    __TSID_JComboBox.select ( 0 );
                }
            }
        }
        if ( Method == null ) {
            // Select default...
            __Method_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __Method_JComboBox, Method, JGUIUtil.NONE, null, null ) ) {
                __Method_JComboBox.select ( Method );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nMethod value \"" +
                Method + "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( NewInterval != null ) {
            __NewInterval_JTextField.setText ( NewInterval );
        }
        if ( NewDataType != null ) {
            __NewDataType_JTextField.setText ( NewDataType );
        }
        if ( NewUnits != null ) {
            __NewUnits_JTextField.setText ( NewUnits );
        }
    }
    // Regardless, reset the command from the fields...
    Alias = __Alias_JTextField.getText().trim();
    TSID = __TSID_JComboBox.getSelected();
    Method = __Method_JComboBox.getSelected();
    NewInterval = __NewInterval_JTextField.getText().trim();
    NewDataType = __NewDataType_JTextField.getText().trim();
    NewUnits = __NewUnits_JTextField.getText().trim();
    props = new PropList ( __command.getCommandName() );
    props.add ( "Alias=" + Alias );
    props.add ( "TSID=" + TSID );
    props.add ( "Method=" + Method );
    props.add ( "NewInterval=" + NewInterval );
    props.add ( "NewDataType=" + NewDataType );
    props.add ( "NewUnits=" + NewUnits );
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
