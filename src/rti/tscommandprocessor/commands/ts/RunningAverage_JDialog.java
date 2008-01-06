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
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.Command;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Editor dialog for the RunningAverage() command.
*/
public class RunningAverage_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{

private final String __BRACKET_LABEL_CENTERED = "Number of intervals on each side:";
private final String __BRACKET_LABEL_NYEAR = "Number of years:";
private final String __BRACKET_LABEL_PREVIOUS = "Number of previous intervals:";
private final String __BRACKET_LABEL_FUTURE = "Number of future intervals:";

private SimpleJButton	__cancel_JButton = null,// Cancel Button
			__ok_JButton = null;	// Ok Button
private RunningAverage_Command __command = null;// Command to edit
private JTextArea __command_JTextArea = null;
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private SimpleJComboBox __AverageMethod_JComboBox = null;
private JTextField	__Bracket_JTextField = null;
private JLabel		__Bracket_JLabel = null;// Label for bracket
private boolean		__error_wait = false;	// Is there an error to be cleared up
private boolean		__first_time = true;
private boolean     __ok = false;       // Indicates whether OK button has been pressed.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public RunningAverage_JDialog ( JFrame parent, Command command )
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
    
    String AverageMethod = __AverageMethod_JComboBox.getSelected();
    if ( AverageMethod.equalsIgnoreCase( __command._Centered ) ) {
        __Bracket_JLabel.setText ( __BRACKET_LABEL_CENTERED );
    }
    else if ( AverageMethod.equalsIgnoreCase( __command._NYear ) ){
        __Bracket_JLabel.setText ( __BRACKET_LABEL_NYEAR );
    }
    else if ( AverageMethod.equalsIgnoreCase( __command._Previous) ||
            AverageMethod.equalsIgnoreCase( __command._PreviousInclusive) ) {
        __Bracket_JLabel.setText ( __BRACKET_LABEL_PREVIOUS );
    }
    else if ( AverageMethod.equalsIgnoreCase( __command._Future) ||
            AverageMethod.equalsIgnoreCase( __command._FutureInclusive) ) {
        __Bracket_JLabel.setText ( __BRACKET_LABEL_FUTURE );
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
    String AverageMethod = __AverageMethod_JComboBox.getSelected();
    String Bracket = __Bracket_JTextField.getText().trim();
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
    if ( AverageMethod.length() > 0 ) {
        parameters.set ( "AverageMethod", AverageMethod );
    }
    if ( Bracket.length() > 0 ) {
        parameters.set ( "Bracket", Bracket );
    }
    try {   // This will warn the user...
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
{   String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();  
    String AverageMethod = __AverageMethod_JComboBox.getSelected();
    String Bracket = __Bracket_JTextField.getText().trim();
    __command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "AverageMethod", AverageMethod );
    __command.setCommandParameter ( "Bracket", Bracket );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__TSID_JComboBox = null;
	__AverageMethod_JComboBox = null;
	__Bracket_JLabel = null;
	__Bracket_JTextField = null;
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
{   __command = (RunningAverage_Command)command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Convert a time series to a running average.  Units, data type, etc., are not changed."), 
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "A centered running average averages the values at a date/time and on either side."), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Previous and future running averages use points only on one side of the current point, and optionally" +
        " inclusive of the current point."), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "An N-year running average averages the values for the date/time and previous years (N years total)."), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __TSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel ( this, main_JPanel, __TSList_JComboBox, y );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
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

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Type of Average:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__AverageMethod_JComboBox = new SimpleJComboBox ( false );
	__AverageMethod_JComboBox.addItem ( __command._Centered );
    __AverageMethod_JComboBox.addItem ( __command._Future );
    __AverageMethod_JComboBox.addItem ( __command._FutureInclusive );
	__AverageMethod_JComboBox.addItem ( __command._NYear );
    __AverageMethod_JComboBox.addItem ( __command._Previous );
    __AverageMethod_JComboBox.addItem ( __command._PreviousInclusive );
	__AverageMethod_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __AverageMethod_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	__Bracket_JLabel = new JLabel ( __BRACKET_LABEL_CENTERED );
    JGUIUtil.addComponent(main_JPanel, __Bracket_JLabel,
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Bracket_JTextField = new JTextField ( 10 );
	__Bracket_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Bracket_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 4, 40 );
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
    checkGUIState();
	refresh ();

	// South Panel: North
	JPanel button_Panel = new JPanel();
	button_Panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_Panel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_Panel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_Panel.add ( __ok_JButton );

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
{	String routine = "Cumulate_JDialog.refresh";
    String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String AverageMethod = "";
    String Bracket = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        TSList = props.getValue ( "TSList" );
        TSID = props.getValue ( "TSID" );
        EnsembleID = props.getValue ( "EnsembleID" );
        AverageMethod = props.getValue ( "AverageMethod" );
        Bracket = props.getValue ( "Bracket" );
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
        if ( AverageMethod == null ) {
            // Select default...
            __AverageMethod_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __AverageMethod_JComboBox,AverageMethod, JGUIUtil.NONE, null, null ) ) {
                __AverageMethod_JComboBox.select ( AverageMethod );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nAverageMethod value \"" + AverageMethod +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( Bracket != null ) {
            __Bracket_JTextField.setText( Bracket );
        }
	}
	// Regardless, reset the command from the fields...
    TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    AverageMethod = __AverageMethod_JComboBox.getSelected();
    Bracket = __Bracket_JTextField.getText().trim();
    props = new PropList ( __command.getCommandName() );
    props.add ( "TSList=" + TSList );
    props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
    props.add ( "AverageMethod=" + AverageMethod );
    props.add ( "Bracket=" + Bracket );
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
