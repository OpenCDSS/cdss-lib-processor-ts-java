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
import rti.tscommandprocessor.ui.CommandEditorUtil;

import RTi.TS.TSIdent;
import RTi.TS.TSIdent_JDialog;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.Command;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Dialog to edit CopyEnsemble() command.
 */
public class CopyEnsemble_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{

private SimpleJButton	__cancel_JButton = null,// Cancel button
			__ok_JButton = null;	// Ok button
private JFrame		__parent_JFrame = null;	// parent JFrame
private CopyEnsemble_Command	__command = null;	// Command to edit.
private JTextArea	__command_JTextArea=null;// Command as JTextField
private JTextField	__NewEnsembleID_JTextField = null;
private JTextField  __NewEnsembleName_JTextField;
private JTextField  __NewAlias_JTextField = null;
private SimpleJComboBox	__EnsembleID_JComboBox = null;
private JTextArea	__NewTSID_JTextArea = null;
private SimpleJButton	__edit_JButton = null;	// Edit button
private SimpleJButton	__clear_JButton = null;	// Clear NewTSID button
private boolean		__error_wait = false;	// Is there an error to be cleared up?
private boolean		__first_time = true;
private boolean		__ok = false;		// Whether OK has been pressed.

/**
Command editor dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public CopyEnsemble_JDialog ( JFrame parent, Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();
	String routine = "CopyEnsemble_JDialog.actionPerformed";

	if ( o == __cancel_JButton ) {
		response ( false );
	}
    else if ( o == __clear_JButton ) {
		__NewTSID_JTextArea.setText ( "" );
        refresh();
	}
	else if ( o == __edit_JButton ) {
		// Edit the NewTSID in the dialog.  It is OK for the string to be blank.
		String NewTSID = __NewTSID_JTextArea.getText().trim();
		TSIdent tsident;
		try {
            if ( NewTSID.length() == 0 ) {
				tsident = new TSIdent();
			}
			else {
                tsident = new TSIdent ( NewTSID );
			}
            PropList idprops = new PropList("NewTSIDProps" );
            idprops.set ( "EnableAll=False" );
            idprops.set ( "EnableLocation=True" );
            idprops.set ( "EnableSource=True" );
            idprops.set ( "EnableType=True" );
            idprops.set ( "EnableScenario=True" );
			TSIdent tsident2=(new TSIdent_JDialog ( __parent_JFrame, true, tsident, idprops )).response();
			if ( tsident2 != null ) {
				__NewTSID_JTextArea.setText ( tsident2.toString(true) );
				refresh();
			}
		}
		catch ( Exception e ) {
			Message.printWarning ( 1, routine, "Error creating time series identifier from \"" + NewTSID + "\"." );
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
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	String NewEnsembleID = __NewEnsembleID_JTextField.getText().trim();
    String NewEnsembleName = __NewEnsembleName_JTextField.getText().trim();
	String EnsembleID = __EnsembleID_JComboBox.getSelected();
	String NewAlias = __NewAlias_JTextField.getText().trim();
	String NewTSID = __NewTSID_JTextArea.getText().trim();
	__error_wait = false;

	if ( NewEnsembleID.length() > 0 ) {
		props.set ( "NewEnsembleID", NewEnsembleID );
	}
    if ( NewEnsembleName.length() > 0 ) {
        props.set ( "NewEnsembleName", NewEnsembleName );
    }
	if ( (EnsembleID != null) && (EnsembleID.length() > 0) ) {
		props.set ( "EnsembleID", EnsembleID );
	}
    if ( (NewAlias != null) && (NewAlias.length() > 0) ) {
        props.set ( "NewAlias", NewAlias );
    }
	if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
		props.set ( "NewTSID", NewTSID );
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
{	String NewEnsembleID = __NewEnsembleID_JTextField.getText().trim();
    String NewEnsembleName = __NewEnsembleName_JTextField.getText().trim();
	String EnsembleID = __EnsembleID_JComboBox.getSelected();
	String NewAlias = __NewAlias_JTextField.getText().trim();
	String NewTSID = __NewTSID_JTextArea.getText().trim();
	__command.setCommandParameter ( "NewEnsembleID", NewEnsembleID );
    __command.setCommandParameter ( "NewEnsembleName", NewEnsembleName );
	__command.setCommandParameter ( "EnsembleID", EnsembleID );
	__command.setCommandParameter ( "NewAlias", NewAlias );
	__command.setCommandParameter ( "NewTSID", NewTSID );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__NewEnsembleID_JTextField = null;
	__EnsembleID_JComboBox = null;
	__NewAlias_JTextField = null;
	__NewTSID_JTextArea = null;
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
private void initialize ( JFrame parent, Command command )
{	__parent_JFrame = parent;
	__command = (CopyEnsemble_Command)command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Make a copy of a time series ensemble, giving the copy a new identifier." ),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The copy is exactly the same and can be referenced by its identifier in other commands." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Because time series in the ensemble are copies of time series from the original ensemble," +
        " the Ensemble ID should be used for processing time series." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optionally, specify new time series alias and identifier (TSID)" +
		" information for the time series in the copy:  location, source, data type, and/or scenario." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This is highly recommended if there is any chance that the " +
		"copy will be mistaken for the original." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "New ensemble identifier:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewEnsembleID_JTextField = new JTextField ( "", 20 );
	__NewEnsembleID_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __NewEnsembleID_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "New ensemble name:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NewEnsembleName_JTextField = new JTextField ( "", 30 );
    __NewEnsembleName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __NewEnsembleName_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional name for copy."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JLabel EnsembleID_JLabel = new JLabel ("Ensemble to copy:");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    Vector EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
            this, this, main_JPanel, EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "New time series alias:" ), 
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NewAlias_JTextField = new JTextField ( "", 20 );
    __NewAlias_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __NewAlias_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    // TODO SAM 2008-07-23 Need editor for these format specifiers
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Can use % fields as per graph labels."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "New time series ID parts:" ),
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
    JGUIUtil.addComponent(main_JPanel, (__edit_JButton =
		new SimpleJButton ( "Edit", "Edit", this ) ),
		3, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(main_JPanel, (__clear_JButton =
		new SimpleJButton ( "Clear", "Clear", this ) ),
		4, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
   
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
{	refresh();
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
			response ( false );
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
{	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String NewEnsembleID = "";
    String NewEnsembleName = "";
	String EnsembleID = "";
	String NewAlias = "";
	String NewTSID = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
        NewEnsembleID = props.getValue ( "NewEnsembleID" );
        NewEnsembleName = props.getValue ( "NewEnsembleName" );
        EnsembleID = props.getValue ( "EnsembleID" );
        NewAlias = props.getValue ( "NewAlias" );
		NewTSID = props.getValue ( "NewTSID" );
		if ( NewEnsembleID != null ) {
			__NewEnsembleID_JTextField.setText ( NewEnsembleID );
		}
        if ( NewEnsembleName != null ) {
            __NewEnsembleName_JTextField.setText ( NewEnsembleName );
        }
		// Now select the item in the list.  If not a match, print awarning.
		if ( JGUIUtil.isSimpleJComboBoxItem( __EnsembleID_JComboBox, EnsembleID, JGUIUtil.NONE, null, null ) ) {
			__EnsembleID_JComboBox.select ( EnsembleID );
		}
		else {
            // Automatically add to the list after the blank...
			if ( (EnsembleID != null) && (EnsembleID.length() > 0) ) {
				__EnsembleID_JComboBox.insertItemAt ( EnsembleID, 1 );
				// Select...
				__EnsembleID_JComboBox.select ( EnsembleID );
			}
			else {	// Select the first choice...
				if ( __EnsembleID_JComboBox.getItemCount() > 0 ) {
					__EnsembleID_JComboBox.select ( 0 );
				}
			}
		}
        if ( NewAlias != null ) {
            __NewAlias_JTextField.setText ( NewAlias );
        }
		if ( NewTSID != null ) {
			__NewTSID_JTextArea.setText ( NewTSID );
		}
	}
	// Regardless, reset the command from the fields...
    NewEnsembleID = __NewEnsembleID_JTextField.getText().trim();
    NewEnsembleName = __NewEnsembleName_JTextField.getText().trim();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    NewAlias = __NewAlias_JTextField.getText().trim();
	NewTSID = __NewTSID_JTextArea.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "NewEnsembleID=" + NewEnsembleID );
    props.add ( "NewEnsembleName=" + NewEnsembleName );
	props.add ( "EnsembleID=" + EnsembleID );
	props.add ( "NewAlias=" + NewAlias );
	props.add ( "NewTSID=" + NewTSID );
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
