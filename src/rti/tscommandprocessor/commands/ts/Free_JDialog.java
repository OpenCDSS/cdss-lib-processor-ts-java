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
import javax.swing.JTextField;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

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
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private SimpleJComboBox __FreeEnsembleIfEmpty_JComboBox = null;
private JLabel __TSPosition_JLabel = null;
private JTextField  __TSPosition_JTextField=null;       // Field for TS positions
private boolean     __first_time = true;
private boolean     __error_wait = false;
private boolean     __ok = false; // Indicates whether user pressed OK to close the dialog.

/**
Command editor dialog constructor.
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
Check the GUI state to make sure that appropriate components are enabled/disabled.
*/
private void checkGUIState ()
{
    String TSList = __TSList_JComboBox.getSelected();
    if ( TSListType.ALL_MATCHING_TSID.equals(TSList) ) {
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
    if ( TSListType.TSPOSITION.equals(TSList)) {
        __TSPosition_JTextField.setEnabled(true);
        __TSPosition_JLabel.setEnabled ( true );
    }
    else {
        __TSPosition_JTextField.setEnabled(false);
        __TSPosition_JLabel.setEnabled ( false );
    }
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput()
{   // Put together a list of parameters to check...
    PropList parameters = new PropList ( "" );
    String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();   
    String FreeEnsembleIfEmpty = __FreeEnsembleIfEmpty_JComboBox.getSelected();
    String TSPosition = __TSPosition_JTextField.getText().trim();

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
    if ( FreeEnsembleIfEmpty.length() > 0 ) {
        parameters.set ( "FreeEnsembleIfEmpty", FreeEnsembleIfEmpty );
    }
    if ( TSPosition.length() > 0 ) {
        parameters.set ( "TSPosition", TSPosition );
    }
    try {
        // This will warn the user...
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
{   String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String FreeEnsembleIfEmpty = __FreeEnsembleIfEmpty_JComboBox.getSelected();
    String TSPosition = __TSPosition_JTextField.getText().trim();
    __command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "FreeEnsembleIfEmpty", FreeEnsembleIfEmpty );
    __command.setCommandParameter ( "TSPosition", TSPosition );
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
        "This command frees (removes) time series, which is useful to remove unneeded or temporary time series."),
        0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
     JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The list of time series to be removed can be indicated in several ways."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
     JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Time series identifiers follow the pattern:"),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
     JGUIUtil.addComponent(main_JPanel, new JLabel (
        "  Location.Source.DataType.Interval.Scenario"),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
     JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Examples of wildcard use when TSList=AllMatchingTSID are shown below:"),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
     JGUIUtil.addComponent(main_JPanel, new JLabel (
        "* - matches all time series"),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
     JGUIUtil.addComponent(main_JPanel, new JLabel (
        "ABC* - matches locations starting with ABC"),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
     JGUIUtil.addComponent(main_JPanel, new JLabel (
        "ABC*.*.Type.Month - matches locations starting with ABC, with data type Type and interval Month."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
     JGUIUtil.addComponent(main_JPanel, new JLabel (
         "Time series that are in an ensemble will be removed from the ensemble."),
         0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

     __TSList_JComboBox = new SimpleJComboBox(false);
     y = CommandEditorUtil.addTSListToEditorDialogPanel ( this, main_JPanel, __TSList_JComboBox, y );
     // Add the non-standard choice
     __TSList_JComboBox.add( TSListType.TSPOSITION.toString());

     __TSID_JLabel = new JLabel ("TSID (for TSList=matching TSID):");
     __TSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
     Vector tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
             (TSCommandProcessor)__command.getCommandProcessor(), __command );
     y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, y );
     
     __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
     __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
     Vector EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
             (TSCommandProcessor)__command.getCommandProcessor(), __command );
     y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
             this, this, main_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );
     
      __TSPosition_JLabel = new JLabel ("Time series position(s) (for TSList=" + TSListType.TSPOSITION.toString() + "):");
     JGUIUtil.addComponent(main_JPanel, __TSPosition_JLabel,
         0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
     __TSPosition_JTextField = new JTextField ( "", 8 );
     __TSPosition_JTextField.addKeyListener ( this );
         JGUIUtil.addComponent(main_JPanel, __TSPosition_JTextField,
         1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
         JGUIUtil.addComponent(main_JPanel, new JLabel ( "For example, 1,2,7-8 (positions are 1+)." ),
         2, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
         
     JGUIUtil.addComponent(main_JPanel, new JLabel ( "Free ensemble if empty?" ), 
         0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
     __FreeEnsembleIfEmpty_JComboBox = new SimpleJComboBox ( false );
     __FreeEnsembleIfEmpty_JComboBox.addItem ( "" );
     __FreeEnsembleIfEmpty_JComboBox.addItem ( __command._False );
     __FreeEnsembleIfEmpty_JComboBox.addItem ( __command._True );
     __FreeEnsembleIfEmpty_JComboBox.select ( __command._True );
     __FreeEnsembleIfEmpty_JComboBox.addItemListener ( this );
     JGUIUtil.addComponent(main_JPanel, __FreeEnsembleIfEmpty_JComboBox,
         1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
     JGUIUtil.addComponent(main_JPanel, new JLabel( "Default (blank) = True."), 
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
{   if ( event.getStateChange() != ItemEvent.SELECTED ) {
        return;
    }
    checkGUIState();
    refresh();
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
{   refresh();  
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
{   String routine = "Free_JDialog.refresh";
    String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String FreeEnsembleIfEmpty = "";
    String TSPosition = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        TSList = props.getValue ( "TSList" );
        TSID = props.getValue ( "TSID" );
        EnsembleID = props.getValue ( "EnsembleID" );
        FreeEnsembleIfEmpty = props.getValue ( "FreeEnsembleIfEmpty" );
        TSPosition = props.getValue ( "TSPosition" );
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
        if (    JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID,
                JGUIUtil.NONE, null, null ) ) {
                __TSID_JComboBox.select ( TSID );
        }
        else {  // Automatically add to the list after the blank...
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
        if ( FreeEnsembleIfEmpty == null ) {
            // Select default...
            __FreeEnsembleIfEmpty_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __FreeEnsembleIfEmpty_JComboBox,FreeEnsembleIfEmpty, JGUIUtil.NONE, null, null ) ) {
                __FreeEnsembleIfEmpty_JComboBox.select ( FreeEnsembleIfEmpty );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nFreeEnsembleIfEmpty value \"" + FreeEnsembleIfEmpty +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( TSPosition != null ) {
            __TSPosition_JTextField.setText ( TSPosition );
        }
    }
    // Regardless, reset the command from the fields...
    TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected().trim();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    FreeEnsembleIfEmpty = __FreeEnsembleIfEmpty_JComboBox.getSelected();
    TSPosition = __TSPosition_JTextField.getText().trim();
    props = new PropList ( __command.getCommandName() );
    props.add ( "TSList=" + TSList );
    props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
    props.add ( "FreeEnsembleIfEmpty=" + FreeEnsembleIfEmpty );
    props.add ( "TSPosition=" + TSPosition );
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
{   response ( false );
}

public void windowActivated( WindowEvent evt ){;}
public void windowClosed( WindowEvent evt ){;}
public void windowDeactivated( WindowEvent evt ){;}
public void windowDeiconified( WindowEvent evt ){;}
public void windowIconified( WindowEvent evt ){;}
public void windowOpened( WindowEvent evt ){;}

}
