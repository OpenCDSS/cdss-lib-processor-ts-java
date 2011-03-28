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
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.Command;
import RTi.Util.IO.PropList;

public class NewEndOfMonthTSFromDayTS_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{

private SimpleJButton	__cancel_JButton = null,// Cancel Button
			__ok_JButton = null;	// Ok Button
private NewEndOfMonthTSFromDayTS_Command __command = null;
private JTextArea __command_JTextArea=null;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private SimpleJComboBox __DayTSID_JComboBox = null;
private JTextField __Bracket_JTextField = null;
private boolean __error_wait = false;	// Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false;       // Whether OK has been pressed.

/**
newEndOfMonthTSFromDayTS_JDialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public NewEndOfMonthTSFromDayTS_JDialog ( JFrame parent, Command command )
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
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__Alias_JTextField = null;
	__cancel_JButton = null;
	__command = null;
	__command = null;
	__DayTSID_JComboBox = null;
	__Bracket_JTextField = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{   // Put together a list of parameters to check...
    PropList props = new PropList ( "" );
    String Alias = __Alias_JTextField.getText().trim();
    String DayTSID = __DayTSID_JComboBox.getSelected();
    String Bracket = __Bracket_JTextField.getText().trim();
    __error_wait = false;

    if ( Alias.length() > 0 ) {
        props.set ( "Alias", Alias );
    }
    if ( (DayTSID != null) && (DayTSID.length() > 0) ) {
        props.set ( "DayTSID", DayTSID );
    }
    if ( (Bracket != null) && (Bracket.length() > 0) ) {
        props.set ( "Bracket", Bracket );
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
{   String Alias = __Alias_JTextField.getText().trim();
    String DayTSID = __DayTSID_JComboBox.getSelected();
    String Bracket = __Bracket_JTextField.getText().trim();
    __command.setCommandParameter ( "Alias", Alias );
    __command.setCommandParameter ( "DayTSID", DayTSID );
    __command.setCommandParameter ( "Bracket", Bracket );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Command command )
{	__command = (NewEndOfMonthTSFromDayTS_Command)command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Create a new end of month time series from a daily time series."),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Use the alias to reference the new time series." +
		"  Only the data interval is changed in output (units, etc. remain)."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The number of days to search in either direction must be non-zero."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specifying a number of days > 31 may result in a constant pattern in the output."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel("Daily time series:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DayTSID_JComboBox = new SimpleJComboBox ( true );    // Allow edit
    List tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    __DayTSID_JComboBox.setData ( tsids );
    __DayTSID_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DayTSID_JComboBox,
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(10);
    __Alias_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - use %L for location, etc."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Number of days to search:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Bracket_JTextField = new JTextField ( "15", 10 );
	JGUIUtil.addComponent(main_JPanel, __Bracket_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required - must be greater than zero."), 
    3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	__Bracket_JTextField.addKeyListener ( this );

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

	setTitle ( "Edit " + __command.getCommandName() + "() Command" );
	// Dialogs do not need to be resizable...
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
{	refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
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
{   return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{   String Alias = "";
    String DayTSID = "";
    String Bracket = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        Alias = props.getValue ( "Alias" );
        DayTSID = props.getValue ( "DayTSID" );
        Bracket = props.getValue ( "Bracket" );
        if ( Alias != null ) {
            __Alias_JTextField.setText ( Alias );
        }
        // Now select the item in the list.  If not a match, print a warning.
        if ( JGUIUtil.isSimpleJComboBoxItem( __DayTSID_JComboBox, DayTSID, JGUIUtil.NONE, null, null ) ) {
            __DayTSID_JComboBox.select ( DayTSID );
        }
        else {
            // Automatically add to the list...
            if ( (DayTSID != null) && (DayTSID.length() > 0) ) {
                __DayTSID_JComboBox.insertItemAt ( DayTSID, 0 );
                // Select...
                __DayTSID_JComboBox.select ( DayTSID );
            }
            else {
                // Select the first choice...
                if ( __DayTSID_JComboBox.getItemCount() > 0 ) {
                    __DayTSID_JComboBox.select ( 0 );
                }
            }
        }
        if ( Bracket != null ) {
            __Bracket_JTextField.setText ( Bracket );
        }
    }
    // Regardless, reset the command from the fields...
    Alias = __Alias_JTextField.getText().trim();
    DayTSID = __DayTSID_JComboBox.getSelected();
    Bracket = __Bracket_JTextField.getText().trim();
    props = new PropList ( __command.getCommandName() );
    props.add ( "Alias=" + Alias );
    props.add ( "DayTSID=" + DayTSID );
    props.add ( "Bracket=" + Bracket );
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