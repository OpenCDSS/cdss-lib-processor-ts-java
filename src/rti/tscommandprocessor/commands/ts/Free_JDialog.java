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

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.Vector;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.Command;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

public class Free_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton   __cancel_JButton = null,// Cancel Button
            __ok_JButton = null;    // Ok Button
private Free_Command      __command = null;
private JTextArea  __command_JTextArea=null;
private SimpleJComboBox __TSID_JComboBox = null;
private boolean     __first_time = true;
private boolean     __error_wait = false;
private boolean     __ok = false; // Indicates whether user pressed OK to close the dialog.

/**
Free_JDialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public Free_JDialog ( JFrame parent, Command command )
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
private void checkInput()
{   // Put together a list of parameters to check...
    PropList parameters = new PropList ( "" );
    String TSID = __TSID_JComboBox.getSelected();
    //String warning = "";

    __error_wait = false;
    
    if ( TSID.length() > 0 ) {
        parameters.set ( "TSID", TSID );
    }
    try {   // This will warn the user...
        __command.checkCommandParameters ( parameters, null, 1 );
    }
    catch ( Exception e ) {
        // The warning would have been printed in the check code.
        Message.printWarning(2,"",e);
        __error_wait = true;
    }
}

/**
Commit the edits to the command.  In this case the command parameters have
already been checked and no errors were detected.
*/
private void commitEdits ()
{   String TSID = __TSID_JComboBox.getSelected();
    __command.setCommandParameter ( "TSID", TSID );
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
{   __command = (Free_Command)command;

    addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

    JPanel main_JPanel = new JPanel();
    main_JPanel.setLayout( new GridBagLayout() );
    getContentPane().add ( "North", main_JPanel );
    int y = 0;

     JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Free time series.  This is useful because some commands operate on all available time series,"),
        0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
     JGUIUtil.addComponent(main_JPanel, new JLabel (
        "and therefore unneeded time series may need to be removed."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
     JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Select the time series to free from the list or enter " +
        "a time series identifier or alias."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
     JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Identifiers follow the pattern:"),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
     JGUIUtil.addComponent(main_JPanel, new JLabel (
        "  Location.Source.DataType.Interval.Scenario"),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
     JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Examples of wildcard use are shown below:"),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
     JGUIUtil.addComponent(main_JPanel, new JLabel (
        "* - matches all time series"),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
     JGUIUtil.addComponent(main_JPanel, new JLabel (
        "ABC* - matches locations starting with ABC"),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
     JGUIUtil.addComponent(main_JPanel, new JLabel (
        "ABC*.*.TYPE.MONTH - matches locations starting with ABC, with"+
        " data type TYPE and interval MONTH."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
     JGUIUtil.addComponent(main_JPanel, new JLabel (
        "See also the selectTimeSeries() and deselectTimeSeries() commands."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
     JGUIUtil.addComponent(main_JPanel, new JLabel (
         "Time series that are in an ensemble will be removed from the ensemble."),
         0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

     JGUIUtil.addComponent(main_JPanel, new JLabel ( "Time series to free:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    // Allow edits so that wildcards can be entered.
    __TSID_JComboBox = new SimpleJComboBox ( true );
    Vector tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    __TSID_JComboBox.setData ( tsids );
    // Always allow a "*" to let all time series be freed...
    __TSID_JComboBox.add( "*" );
    __TSID_JComboBox.addItemListener ( this );
    __TSID_JComboBox.addKeyListener ( this );
     JGUIUtil.addComponent(main_JPanel, __TSID_JComboBox,
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

     JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
     __command_JTextArea = new JTextArea ( 4, 50 );
     __command_JTextArea.setLineWrap ( true );
     __command_JTextArea.setWrapStyleWord ( true );
     __command_JTextArea.setEditable ( false );
     JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
            1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Refresh the contents...
    refresh ();

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
@param event ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent event )
{   Object o = event.getItemSelectable();

    if ( o.equals(__TSID_JComboBox) ) {
        if ( event.getStateChange() != ItemEvent.SELECTED ) {
            return;
        }
        refresh();
    }
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{   int code = event.getKeyCode();

    refresh ();
    if ( code == KeyEvent.VK_ENTER ) {
        checkInput();
        if ( !__error_wait ) {
            response ( true );
        }
    }
}

public void keyReleased ( KeyEvent event )
{   // Nothing to do    
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
Refresh the command from the other text field contents:
<pre>
free(TSID="X")
</pre>
*/
private void refresh ()
{   String TSID = "";
    String routine = "free_JDialog.refresh";
    PropList parameters = null;
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        parameters = __command.getCommandParameters();
        TSID = parameters.getValue ( "TSID" );
        if ( __TSID_JComboBox != null ) {
        if ( TSID == null ) {
            // Select default...
            __TSID_JComboBox.select ( 0 );
        }
        else {  if (    JGUIUtil.isSimpleJComboBoxItem(
                __TSID_JComboBox, TSID,
                JGUIUtil.NONE, null, null) ) {
                __TSID_JComboBox.select ( TSID );
            }
            else {  Message.printWarning ( 2, routine,
                "Existing free() references an unrecognized\n" +
                "TSID value \"" + TSID + "\".\nAllowing the " +
                " value because it contains wildcards or " +
                "may be the result from a bulk read.");
                __TSID_JComboBox.setText ( TSID );
                /* TODO SAM 2004-07-11 - see how the above
                works
                else {  // print a warning...
                    Message.printWarning ( 1, routine,
                    "Existing free() references a non-existent\n"+
                    "time series \"" + TSID + "\".  Select a\n" +
                    "different time series or Cancel." );
                }
                */
            }
        }
        }
    }
    // Regardless, reset the command from the fields...
    if ( __TSID_JComboBox != null ) {
        TSID = __TSID_JComboBox.getSelected();
    }
    parameters = new PropList ( __command.getCommandName() );
    parameters.add ( "TSID=" + TSID );
    __command_JTextArea.setText( __command.toString ( parameters ) );
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
{   response ( false );
}

public void windowActivated( WindowEvent evt ){;}
public void windowClosed( WindowEvent evt ){;}
public void windowDeactivated( WindowEvent evt ){;}
public void windowDeiconified( WindowEvent evt ){;}
public void windowIconified( WindowEvent evt ){;}
public void windowOpened( WindowEvent evt ){;}

} // end Free_JDialog
