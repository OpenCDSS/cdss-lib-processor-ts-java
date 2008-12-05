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

import java.util.List;
import java.util.Vector;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.Command;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Command editor dialog for the CreateEnsemble() command.
*/
public class CreateEnsemble_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, ListSelectionListener, WindowListener
{

private SimpleJButton	__cancel_JButton = null,// Cancel Button
			__ok_JButton = null;	// Ok Button
private CreateEnsemble_Command __command = null;// Command to edit
private JTextArea	__command_JTextArea=null;// Command as JTextField
private SimpleJComboBox __TSID_JComboBox = null;// Field for time series IDs
private JTextField  __InputStart_JTextField;// Text fields for query period, both versions.
private JTextField  __InputEnd_JTextField;
private JTextField  __EnsembleID_JTextField;
private JTextField  __EnsembleName_JTextField;
private SimpleJComboBox	__ShiftDataHow_JComboBox = null;// Indicates how to handle shift.
private JTextField	__ReferenceDate_JTextField = null; // Reference date.
private JTextField	__TraceLength_JTextField=null; // Total period length.

private boolean		__error_wait = false;
private boolean		__first_time = true;
private boolean     __ok = false;  // Has the users pressed OK to close the dialog.

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to parse.
*/
public CreateEnsemble_JDialog ( JFrame parent, Command command )
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
    PropList parameters = new PropList ( "" );
    String TSID = __TSID_JComboBox.getSelected();
    String InputStart = __InputStart_JTextField.getText().trim();
    String InputEnd = __InputEnd_JTextField.getText().trim();
    String EnsembleID = __EnsembleID_JTextField.getText().trim();
    String EnsembleName = __EnsembleName_JTextField.getText().trim();
    String TraceLength = __TraceLength_JTextField.getText().trim();
    String ReferenceDate = __ReferenceDate_JTextField.getText().trim();
    String ShiftDataHow = __ShiftDataHow_JComboBox.getSelected();

    __error_wait = false;
    
    if ( TSID.length() > 0 ) {
        parameters.set ( "TSID", TSID );
    }
    if ( InputStart.length() > 0 ) {
        parameters.set ( "InputStart", InputStart );
    }
    if ( InputEnd.length() > 0 ) {
        parameters.set ( "InputEnd", InputEnd );
    }
    if ( EnsembleID.length() > 0 ) {
        parameters.set ( "EnsembleID", EnsembleID );
    }
    if ( EnsembleName.length() > 0 ) {
        parameters.set ( "EnsembleName", EnsembleName );
    }
    if ( TraceLength.length() > 0 ) {
        parameters.set ( "TraceLength", TraceLength );
    }
    if ( ReferenceDate.length() > 0 ) {
        parameters.set ( "ReferenceDate", ReferenceDate );
    }
    if ( ShiftDataHow.length() > 0 ) {
        parameters.set ( "ShiftDataHow", ShiftDataHow );
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
    String TraceLength = __TraceLength_JTextField.getText().trim();
    String InputStart = __InputStart_JTextField.getText().trim();
    String InputEnd = __InputEnd_JTextField.getText().trim();
    String EnsembleID = __EnsembleID_JTextField.getText().trim();
    String EnsembleName = __EnsembleName_JTextField.getText().trim();
    String ReferenceDate = __ReferenceDate_JTextField.getText().trim();
    String ShiftDataHow = __ShiftDataHow_JComboBox.getSelected();
    
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "InputStart", InputStart );
    __command.setCommandParameter ( "InputEnd", InputEnd );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "EnsembleName", EnsembleName );
    __command.setCommandParameter ( "TraceLength", TraceLength );
    __command.setCommandParameter ( "ReferenceDate", ReferenceDate );
    __command.setCommandParameter ( "ShiftDataHow", ShiftDataHow );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__cancel_JButton = null;
	__command_JTextArea = null;
	__ReferenceDate_JTextField = null;
	__TraceLength_JTextField = null;
	__command = null;
	__TSID_JComboBox = null;
	__ShiftDataHow_JComboBox = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Command command )
{	__command = (CreateEnsemble_Command)command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Create an ensemble of time series traces from a time series."),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Each trace will start on the reference date and will be as long as specified."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Each trace will have the properties of the original time series with unique sequence numbers."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Specify the period to limit the number of traces generated from the original time series."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify the reference date using standard date formats to a precision appropriate for the data."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"If shifted, each trace will start on the reference date (use to align time series for display and analysis)."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "If NOT shifted, each trace will start on the reference date, but year will vary with the data."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel,
		new JLabel ( "Time series from which to create traces:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    List tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    if ( tsids == null ) {
        // User will not be able to select anything.
        tsids = new Vector();
    }
    __TSID_JComboBox = new SimpleJComboBox ( true );    // Allow edit
    __TSID_JComboBox.setData ( tsids );
    __TSID_JComboBox.addKeyListener ( this );
    __TSID_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __TSID_JComboBox,
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Period to read:" ), 
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputStart_JTextField = new JTextField ( 15 );
    __InputStart_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __InputStart_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "to" ), 
        3, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
    __InputEnd_JTextField = new JTextField ( 15 );
    __InputEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __InputEnd_JTextField,
        4, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel,
        new JLabel ( "Ensemble ID:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EnsembleID_JTextField = new JTextField ( "", 20 );
    __EnsembleID_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __EnsembleID_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required identifier for ensemble."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Ensemble name:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EnsembleName_JTextField = new JTextField ( "", 30 );
    __EnsembleName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __EnsembleName_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Optional name for output."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Trace length:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TraceLength_JTextField = new JTextField ( "1Year", 10 );
	__TraceLength_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel, __TraceLength_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
            "Default=1Year."), 
            3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel (	"Reference date:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ReferenceDate_JTextField = new JTextField ( 10 );
	__ReferenceDate_JTextField.addKeyListener(this);
	JGUIUtil.addComponent(main_JPanel, __ReferenceDate_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Default=Jan 1 of first year."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Shift data how?:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ShiftDataHow_JComboBox = new SimpleJComboBox ( false );
	__ShiftDataHow_JComboBox.addItem ( __command._NoShift );
	__ShiftDataHow_JComboBox.addItem ( __command._ShiftToReference );
	__ShiftDataHow_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __ShiftDataHow_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Default=" + __command._NoShift), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 5, 65 );
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

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

    setTitle ( "Edit " + __command.getCommandName() + "() command" );
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
{	String TSID = "";
    String InputStart = "";
    String InputEnd = "";
    String EnsembleID = "";
    String EnsembleName = "";
	String TraceLength = "";
	String ReferenceDate = "";
	String ShiftDataHow = "";
    PropList parameters = null;      // Parameters as PropList.
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        parameters = __command.getCommandParameters ();
		TSID = parameters.getValue("TSID");
        InputStart = parameters.getValue ( "InputStart" );
        InputEnd = parameters.getValue ( "InputEnd" );
        EnsembleID = parameters.getValue ( "EnsembleID" );
        EnsembleName = parameters.getValue ( "EnsembleName" );
        TraceLength = parameters.getValue("TraceLength");
        ReferenceDate = parameters.getValue("ReferenceDate");
        ShiftDataHow = parameters.getValue("ShiftDataHow");

        // Now select the item in the list.  If not a match, print a warning.
        if ( JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID, JGUIUtil.NONE, null, null ) ) {
                __TSID_JComboBox.select ( TSID );
        }
        else {  // Automatically add to the list after the blank...
            if ( (TSID != null) && (TSID.length() > 0) ) {
                __TSID_JComboBox.insertItemAt ( TSID, 1 );
                // Select...
                __TSID_JComboBox.select ( TSID );
            }
            else {  // Do not select anything...
            }
        }
        if ( InputStart != null ) {
            __InputStart_JTextField.setText ( InputStart );
        }
        if ( InputEnd != null ) {
            __InputEnd_JTextField.setText ( InputEnd );
        }
        if ( EnsembleID != null ) {
            __EnsembleID_JTextField.setText ( EnsembleID );
        }
        if ( EnsembleName != null ) {
            __EnsembleName_JTextField.setText ( EnsembleName );
        }
        if ( TraceLength != null ) {
            __TraceLength_JTextField.setText ( TraceLength );
        }
        if ( ReferenceDate != null ) {
            __ReferenceDate_JTextField.setText ( ReferenceDate );
        }
        if ( JGUIUtil.isSimpleJComboBoxItem( __ShiftDataHow_JComboBox, ShiftDataHow, JGUIUtil.NONE, null, null ) ) {
            __ShiftDataHow_JComboBox.select ( ShiftDataHow );
        }
        else {
            if ( (ShiftDataHow == null) || ShiftDataHow.equals("") ) {
                // Set default...
                __ShiftDataHow_JComboBox.select ( 0 );
            }
            else {
                Message.printWarning ( 1,
                "CreateEnsemble_JDialog.refresh", "Existing command references an invalid\n"+
                "ShiftDataHow \"" + ShiftDataHow + "\".  Select a\ndifferent value or Cancel." );
            }
        }
	}
	// Regardless, reset the command from the fields...
    TSID = __TSID_JComboBox.getSelected();
    TraceLength = __TraceLength_JTextField.getText().trim();
    ReferenceDate = __ReferenceDate_JTextField.getText().trim();
    ShiftDataHow = __ShiftDataHow_JComboBox.getSelected();
    InputStart = __InputStart_JTextField.getText().trim();
    InputEnd = __InputEnd_JTextField.getText().trim();
    EnsembleID = __EnsembleID_JTextField.getText().trim();
    EnsembleName = __EnsembleName_JTextField.getText().trim();
    parameters = new PropList ( __command.getCommandName() );
    parameters.add ( "TSID=" + TSID );
    parameters.add ( "InputStart=" + InputStart );
    parameters.add ( "InputEnd=" + InputEnd );
    parameters.add ( "EnsembleID=" + EnsembleID );
    parameters.add ( "EnsembleName=" + EnsembleName );
    parameters.add ( "TraceLength=" + TraceLength );
    parameters.add ( "ReferenceDate=" + ReferenceDate );
    parameters.add ( "ShiftDataHow=" + ShiftDataHow );
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

public void windowActivated( WindowEvent evt ){;}
public void windowClosed( WindowEvent evt ){;}
public void windowDeactivated( WindowEvent evt ){;}
public void windowDeiconified( WindowEvent evt ){;}
public void windowIconified( WindowEvent evt ){;}
public void windowOpened( WindowEvent evt ){;}

}
