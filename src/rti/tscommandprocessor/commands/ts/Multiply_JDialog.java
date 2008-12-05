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

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.List;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.Command;
import RTi.Util.IO.PropList;

public class Multiply_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, ListSelectionListener, WindowListener
{

private SimpleJButton   __cancel_JButton = null,// Cancel Button
            __ok_JButton = null;    // Ok Button
private Multiply_Command __command = null;
private JTextArea __command_JTextArea=null;
private SimpleJComboBox __TSID_JComboBox = null;
private SimpleJComboBox __MultiplierTSID_JComboBox = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private boolean __ok = false; // Whether OK has been pressed.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public Multiply_JDialog ( JFrame parent, Command command )
{   super(parent, true);
    initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{   Object o = event.getSource();

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
    String TSID = __TSID_JComboBox.getSelected();
    String MultiplierTSID = __MultiplierTSID_JComboBox.getSelected();
    __error_wait = false;

    if ( (TSID != null) && (TSID.length() > 0) ) {
        props.set ( "TSID", TSID );
    }
    if ( (MultiplierTSID != null) && (MultiplierTSID.length() > 0) ) {
        props.set ( "MultiplierTSID", MultiplierTSID );
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
{   String TSID = __TSID_JComboBox.getSelected();
    String MultiplierTSID = __MultiplierTSID_JComboBox.getSelected();
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "MultiplierTSID", MultiplierTSID );
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
    super.finalize ();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Command command )
{   __command = (Multiply_Command)command;

    addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

    JPanel main_JPanel = new JPanel();
    main_JPanel.setLayout( new GridBagLayout() );
    getContentPane().add ( "North", main_JPanel );
    int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Multiply one time series by another (the first time series is modified)"),
        0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Missing data in either time series sets the result to missing."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Time series to modify:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TSID_JComboBox = new SimpleJComboBox ( true );    // Allow edit
    List tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    __TSID_JComboBox.setData ( tsids );
    __TSID_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __TSID_JComboBox,
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Time series to multiply by:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MultiplierTSID_JComboBox = new SimpleJComboBox ( true );    // Allow edit
    __MultiplierTSID_JComboBox.setData ( tsids );
    __MultiplierTSID_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __MultiplierTSID_JComboBox,
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

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
{   refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{   int code = event.getKeyCode();

    if ( code == KeyEvent.VK_ENTER ) {
        refresh ();
        checkInput();
        if ( !__error_wait ) {
            response ( true );
        }
    }
}

public void keyReleased ( KeyEvent event )
{   refresh();
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
private void refresh()
{   String TSID = "";
    String MultiplierTSID = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        TSID = props.getValue ( "TSID" );
        MultiplierTSID = props.getValue ( "MultiplierTSID" );
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
        if ( JGUIUtil.isSimpleJComboBoxItem( __MultiplierTSID_JComboBox, MultiplierTSID, JGUIUtil.NONE, null, null ) ) {
            __MultiplierTSID_JComboBox.select ( MultiplierTSID );
        }
        else {
            // Automatically add to the list...
            if ( (MultiplierTSID != null) && (MultiplierTSID.length() > 0) ) {
                __MultiplierTSID_JComboBox.insertItemAt ( MultiplierTSID, 0 );
                // Select...
                __MultiplierTSID_JComboBox.select ( MultiplierTSID );
            }
            else {
                // Select the first choice...
                if ( __MultiplierTSID_JComboBox.getItemCount() > 0 ) {
                    __MultiplierTSID_JComboBox.select ( 0 );
                }
            }
        }
    }
    // Regardless, reset the command from the fields...
    TSID = __TSID_JComboBox.getSelected();
    MultiplierTSID = __MultiplierTSID_JComboBox.getSelected();
    props = new PropList ( __command.getCommandName() );
    props.add ( "TSID=" + TSID );
    props.add ( "MultiplierTSID=" + MultiplierTSID );
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
{   refresh ();
}

/**
Responds to WindowEvents.
@param event WindowEvent object
*/
public void windowClosing( WindowEvent event )
{   response ( false );
}

public void windowActivated( WindowEvent evt ){;}
public void windowClosed( WindowEvent evt ){;}
public void windowDeactivated( WindowEvent evt ){;}
public void windowDeiconified( WindowEvent evt ){;}
public void windowIconified( WindowEvent evt ){;}
public void windowOpened( WindowEvent evt ){;}

}