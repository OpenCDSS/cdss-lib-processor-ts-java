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

import java.util.Vector;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.Command;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Editor dialog for the ComputeErrorTimeSeries() command.
*/
public class ComputeErrorTimeSeries_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, ListSelectionListener, WindowListener
{

private SimpleJButton	__cancel_JButton = null,// Cancel Button
			__ok_JButton = null;	// Ok Button
private ComputeErrorTimeSeries_Command __command = null;// Command to edit
//private JTextField	__SetStart_JTextField,
//			__SetEnd_JTextField; // Text fields for set period.
private JTextArea   __command_JTextArea=null;// Command as JTextField
private SimpleJComboBox __ObservedTSList_JComboBox = null;
private JLabel __ObservedTSID_JLabel = null;
private SimpleJComboBox __ObservedTSID_JComboBox = null;
private JLabel __ObservedEnsembleID_JLabel = null;
private SimpleJComboBox __ObservedEnsembleID_JComboBox = null;
private SimpleJComboBox __SimulatedTSList_JComboBox = null;
private JLabel __SimulatedTSID_JLabel = null;
private SimpleJComboBox __SimulatedTSID_JComboBox = null;
private JLabel __SimulatedEnsembleID_JLabel = null;
private SimpleJComboBox __SimulatedEnsembleID_JComboBox = null;
private SimpleJComboBox __ErrorMeasure_JComboBox = null;
private JTextField __Alias_JTextField = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ComputeErrorTimeSeries_JDialog ( JFrame parent, Command command )
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
    // Observed...
    
    String ObservedTSList = __ObservedTSList_JComboBox.getSelected();
    if ( TSListType.ALL_MATCHING_TSID.equals(ObservedTSList) ) {
        __ObservedTSID_JComboBox.setEnabled(true);
        __ObservedTSID_JLabel.setEnabled ( true );
    }
    else {
        __ObservedTSID_JComboBox.setEnabled(false);
        __ObservedTSID_JLabel.setEnabled ( false );
    }
    if ( TSListType.ENSEMBLE_ID.equals(ObservedTSList)) {
        __ObservedEnsembleID_JComboBox.setEnabled(true);
        __ObservedEnsembleID_JLabel.setEnabled ( true );
    }
    else {
        __ObservedEnsembleID_JComboBox.setEnabled(false);
        __ObservedEnsembleID_JLabel.setEnabled ( false );
    }
    
    // Simulated...
    
    String SimulatedTSList = __SimulatedTSList_JComboBox.getSelected();
    if ( TSListType.ALL_MATCHING_TSID.equals(SimulatedTSList) ) {
        __SimulatedTSID_JComboBox.setEnabled(true);
        __SimulatedTSID_JLabel.setEnabled ( true );
    }
    else {
        __SimulatedTSID_JComboBox.setEnabled(false);
        __SimulatedTSID_JLabel.setEnabled ( false );
    }
    if ( TSListType.ENSEMBLE_ID.equals(SimulatedTSList)) {
        __SimulatedEnsembleID_JComboBox.setEnabled(true);
        __SimulatedEnsembleID_JLabel.setEnabled ( true );
    }
    else {
        __SimulatedEnsembleID_JComboBox.setEnabled(false);
        __SimulatedEnsembleID_JLabel.setEnabled ( false );
    }
}

/**
Check the user input for errors and set __error_wait accordingly.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
    PropList props = new PropList ( "" );
    String ObservedTSList = __ObservedTSList_JComboBox.getSelected();
    String ObservedTSID = __ObservedTSID_JComboBox.getSelected();
    String ObservedEnsembleID = __ObservedEnsembleID_JComboBox.getSelected();
    String SimulatedTSList = __SimulatedTSList_JComboBox.getSelected();
    String SimulatedTSID = __SimulatedTSID_JComboBox.getSelected();
    String SimulatedEnsembleID = __SimulatedEnsembleID_JComboBox.getSelected();
    //String SetStart = __SetStart_JTextField.getText().trim();
    //String SetEnd = __SetEnd_JTextField.getText().trim();
    String ErrorMeasure = __ErrorMeasure_JComboBox.getSelected();
    String Alias = __Alias_JTextField.getText().trim();
    __error_wait = false;

    if ( ObservedTSList.length() > 0 ) {
        props.set ( "ObservedTSList", ObservedTSList );
    }
    if ( ObservedTSID.length() > 0 ) {
        props.set ( "ObservedTSID", ObservedTSID );
    }
    if ( ObservedEnsembleID.length() > 0 ) {
        props.set ( "ObservedEnsembleID", ObservedEnsembleID );
    }
    if ( SimulatedTSList.length() > 0 ) {
        props.set ( "SimulatedTSList", SimulatedTSList );
    }
    if ( SimulatedTSID.length() > 0 ) {
        props.set ( "SimulatedTSID", SimulatedTSID );
    }
    if ( SimulatedEnsembleID.length() > 0 ) {
        props.set ( "SimulatedEnsembleID", SimulatedEnsembleID );
    }
    /*
    if ( SetStart.length() > 0 ) {
        props.set ( "SetStart", SetStart );
    }
    if ( SetEnd.length() > 0 ) {
        props.set ( "SetEnd", SetEnd );
    }
    */
    if ( ErrorMeasure.length() > 0 ) {
        props.set ( "ErrorMeasure", ErrorMeasure );
    }
    if (Alias.length() > 0) {
        props.set("Alias", Alias);
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
{   String ObservedTSList = __ObservedTSList_JComboBox.getSelected();
    String ObservedTSID = __ObservedTSID_JComboBox.getSelected();
    String ObservedEnsembleID = __ObservedEnsembleID_JComboBox.getSelected();
    String SimulatedTSList = __SimulatedTSList_JComboBox.getSelected();
    String SimulatedTSID = __SimulatedTSID_JComboBox.getSelected();
    String SimulatedEnsembleID = __SimulatedEnsembleID_JComboBox.getSelected(); 
    //String SetStart = __SetStart_JTextField.getText().trim();
    //String SetEnd = __SetEnd_JTextField.getText().trim();
    String ErrorMeasure = __ErrorMeasure_JComboBox.getSelected();
    String Alias = __Alias_JTextField.getText().trim();
    __command.setCommandParameter ( "ObservedTSList", ObservedTSList );
    __command.setCommandParameter ( "ObservedTSID", ObservedTSID );
    __command.setCommandParameter ( "ObservedEnsembleID", ObservedEnsembleID );
    __command.setCommandParameter ( "SimulatedTSList", SimulatedTSList );
    __command.setCommandParameter ( "SimulatedTSID", SimulatedTSID );
    __command.setCommandParameter ( "SimulatedEnsembleID", SimulatedEnsembleID );
    //__command.setCommandParameter ( "SetStart", SetStart );
    //__command.setCommandParameter ( "SetEnd", SetEnd );
    __command.setCommandParameter ( "ErrorMeasure", ErrorMeasure );
    __command.setCommandParameter("Alias", Alias);
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{   __ObservedTSID_JComboBox = null;
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
{   __command = (ComputeErrorTimeSeries_Command)command;

	addWindowListener( this );

	Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

	// Main contents...

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Compute the error between simulated time series and observed time series, " +
		"and generate time series of the specified error measure." ), 
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command is useful for calibrating models and evaluating predictions after observed data are available."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "If one observed time series is specified, it will be analyzed against all simulated time series."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "If multiple observed time series are specified (e.g., for ensembles)," +
        " the same number of simulated time series must be specified."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    /*
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify dates with precision appropriate for the data, " +
		"blank for all available data, OutputStart, or OutputEnd." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The set period is for the independent time series."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
		*/

    __ObservedTSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel (
            this, main_JPanel, new JLabel ("Observed TS list:"), __ObservedTSList_JComboBox, y );

    __ObservedTSID_JLabel = new JLabel ("Observed TSID (for ObservedTSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __ObservedTSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
    Vector tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __ObservedTSID_JLabel, __ObservedTSID_JComboBox, tsids, y );
    
    __ObservedEnsembleID_JLabel = new JLabel ("Observed ensembleID (for ObservedTSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __ObservedEnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    Vector EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
            this, this, main_JPanel, __ObservedEnsembleID_JLabel, __ObservedEnsembleID_JComboBox, EnsembleIDs, y );
    
    __SimulatedTSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel (
            this, main_JPanel, new JLabel ("Simulated TS List:"), __SimulatedTSList_JComboBox, y );

    __SimulatedTSID_JLabel = new JLabel (
            "Simulated TSID (for Simulated TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __SimulatedTSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __SimulatedTSID_JLabel, __SimulatedTSID_JComboBox, tsids, y );
    
    __SimulatedEnsembleID_JLabel = new JLabel (
            "Simulated EnsembleID (for Simulated TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __SimulatedEnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
            this, this, main_JPanel, __SimulatedEnsembleID_JLabel, __SimulatedEnsembleID_JComboBox, EnsembleIDs, y );

    /*
	JGUIUtil.addComponent(main_JPanel, new JLabel ( "Set period:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__SetStart_JTextField = new JTextField ( "", 15 );
	__SetStart_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel, __SetStart_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel ( "to" ), 
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	__SetEnd_JTextField = new JTextField ( "", 15 );
	__SetEnd_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel, __SetEnd_JTextField,
		5, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
		*/

	JGUIUtil.addComponent(main_JPanel, new JLabel ( "Error measure:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ErrorMeasure_JComboBox = new SimpleJComboBox ( false );
	__ErrorMeasure_JComboBox.addItem ( __command._PercentError );
	__ErrorMeasure_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __ErrorMeasure_JComboBox,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Alias to assign:" ), 
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new JTextField ( "", 20 );
    __Alias_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Default is no alias is assigned."), 
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
{	String routine = "ComputeErrorTimeSeries_JDialog.refresh";
    String ObservedTSList = "";
    String ObservedTSID = "";
    String ObservedEnsembleID = "";
    String SimulatedTSList = "";
    String SimulatedTSID = "";
    String SimulatedEnsembleID = "";
    String ErrorMeasure = "";
    //String SetStart = "";
    //String SetEnd = "";
    String Alias = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        ObservedTSList = props.getValue ( "ObservedTSList" );
        ObservedTSID = props.getValue ( "ObservedTSID" );
        ObservedEnsembleID = props.getValue ( "ObservedEnsembleID" );
        SimulatedTSList = props.getValue ( "SimulatedTSList" );
        SimulatedTSID = props.getValue ( "SimulatedTSID" );
        SimulatedEnsembleID = props.getValue ( "SimulatedEnsembleID" );
        //SetStart = props.getValue ( "SetStart" );
        //SetEnd = props.getValue ( "SetEnd" );
        ErrorMeasure = props.getValue ( "ErrorMeasure" );
        Alias = props.getValue("Alias");
        if ( ObservedTSList == null ) {
            // Select default...
            __ObservedTSList_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __ObservedTSList_JComboBox,ObservedTSList, JGUIUtil.NONE, null, null ) ) {
                __ObservedTSList_JComboBox.select ( ObservedTSList );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nObservedTSList value \"" + ObservedTSList +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if (    JGUIUtil.isSimpleJComboBoxItem( __ObservedTSID_JComboBox, ObservedTSID,
                JGUIUtil.NONE, null, null ) ) {
                __ObservedTSID_JComboBox.select ( ObservedTSID );
        }
        else {  // Automatically add to the list after the blank...
            if ( (ObservedTSID != null) && (ObservedTSID.length() > 0) ) {
                __ObservedTSID_JComboBox.insertItemAt ( ObservedTSID, 1 );
                // Select...
                __ObservedTSID_JComboBox.select ( ObservedTSID );
            }
            else {  // Select the blank...
                __ObservedTSID_JComboBox.select ( 0 );
            }
        }
        if ( ObservedEnsembleID == null ) {
            // Select default...
            __ObservedEnsembleID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __ObservedEnsembleID_JComboBox,ObservedEnsembleID, JGUIUtil.NONE, null, null ) ) {
                __ObservedEnsembleID_JComboBox.select ( ObservedEnsembleID );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nObservedEnsembleID value \"" + ObservedEnsembleID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( SimulatedTSList == null ) {
            // Select default...
            __SimulatedTSList_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __SimulatedTSList_JComboBox,SimulatedTSList, JGUIUtil.NONE, null, null ) ) {
                __SimulatedTSList_JComboBox.select ( SimulatedTSList );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nSimulatedTSList value \"" + SimulatedTSList +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if (    JGUIUtil.isSimpleJComboBoxItem( __SimulatedTSID_JComboBox, SimulatedTSID,
                JGUIUtil.NONE, null, null ) ) {
                __SimulatedTSID_JComboBox.select ( SimulatedTSID );
        }
        else {  // Automatically add to the list after the blank...
            if ( (SimulatedTSID != null) && (SimulatedTSID.length() > 0) ) {
                __SimulatedTSID_JComboBox.insertItemAt ( SimulatedTSID, 1 );
                // Select...
                __SimulatedTSID_JComboBox.select ( SimulatedTSID );
            }
            else {  // Select the blank...
                __SimulatedTSID_JComboBox.select ( 0 );
            }
        }
        if ( SimulatedEnsembleID == null ) {
            // Select default...
            __SimulatedEnsembleID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __SimulatedEnsembleID_JComboBox,SimulatedEnsembleID, JGUIUtil.NONE, null, null ) ) {
                __SimulatedEnsembleID_JComboBox.select ( SimulatedEnsembleID );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nSimulatedEnsembleID value \"" + SimulatedEnsembleID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        /*
        if ( SetStart != null ) {
			__SetStart_JTextField.setText (	SetStart );
        }
        if ( SetEnd != null ) {
			__SetEnd_JTextField.setText ( SetEnd );
		}
		*/
        if ( ErrorMeasure == null ) {
            // Select default...
            __ErrorMeasure_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __ErrorMeasure_JComboBox,ErrorMeasure, JGUIUtil.NONE, null, null ) ) {
                __ErrorMeasure_JComboBox.select ( ErrorMeasure );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nErrorMeasure value \"" + ErrorMeasure +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( Alias != null ) {
            __Alias_JTextField.setText(Alias.trim());
        }
	}
    // Regardless, reset the command from the fields...
    ObservedTSList = __ObservedTSList_JComboBox.getSelected();
    ObservedTSID = __ObservedTSID_JComboBox.getSelected();
    ObservedEnsembleID = __ObservedEnsembleID_JComboBox.getSelected();
    SimulatedTSList = __SimulatedTSList_JComboBox.getSelected();
    SimulatedTSID = __SimulatedTSID_JComboBox.getSelected();
    SimulatedEnsembleID = __ErrorMeasure_JComboBox.getSelected();
    //SetStart = __SetStart_JTextField.getText().trim();
    //SetEnd = __SetEnd_JTextField.getText().trim();
    ErrorMeasure = __ErrorMeasure_JComboBox.getSelected();
    Alias = __Alias_JTextField.getText().trim();
    props = new PropList ( __command.getCommandName() );
    props.add ( "ObservedTSList=" + ObservedTSList );
    props.add ( "ObservedTSID=" + ObservedTSID );
    props.add ( "ObservedEnsembleID=" + ObservedEnsembleID );
    props.add ( "SimulatedTSList=" + SimulatedTSList );
    props.add ( "SimulatedTSID=" + SimulatedTSID );
    props.add ( "SimulatedEnsembleID=" + SimulatedEnsembleID );
    //props.add ( "SetStart=" + SetStart );
    //props.add ( "SetEnd=" + SetEnd );
    props.add ( "ErrorMeasure=" + ErrorMeasure );
    props.add("Alias=" + Alias );
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

} // end setFromTS_JDialog
