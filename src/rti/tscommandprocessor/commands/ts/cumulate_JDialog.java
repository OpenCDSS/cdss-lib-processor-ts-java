// ----------------------------------------------------------------------------
// cumulate_JDialog - editor for cumulate()
// ----------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
// ----------------------------------------------------------------------------
// History: 
//
// 04 Sep 2001	Steven A. Malers, RTi	Initial version (copy and modify
//					runningAverage_Dialog).
// 2002-04-23	SAM, RTi		Clean up the dialog.
// 2003-12-15	SAM, RTi		Update to Swing.
// 2005-09-29	SAM, RTi		Update to use a command class and
//					named parameters.
//					Add the Reset parameter.
// 2007-02-16	SAM, RTi		Use new CommandProcessor interface.
//					Clean up code based on Eclipse feedback.
// 2007-05-08	SAM, RTi		Cleanup code based on Eclipse feedback.
// ----------------------------------------------------------------------------

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
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.Command;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;

public class cumulate_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton	__cancel_JButton = null,// Cancel Button
			__ok_JButton = null;	// Ok Button
private cumulate_Command __command = null;	// Command to edit
private JTextArea	__command_JTextArea=null;// Command as JTextField
private SimpleJComboBox	__TSID_JComboBox = null;// Field for time series alias
private SimpleJComboBox	__HandleMissingHow_JComboBox=null;
						// Field for handling missing
private SimpleJComboBox	__Reset_JComboBox=null; // Field for reset date.
private boolean		__error_wait = false;	// Is there an error that we
						// are waiting to be cleared up
						// or Cancel?
private boolean		__first_time = true;
private boolean		__ok = false;		// Indicates whether OK button
						// has been pressed.

/**
cumulate_JDialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public cumulate_JDialog ( JFrame parent, Command command )
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
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	String TSID = __TSID_JComboBox.getSelected();
	String HandleMissingHow = __HandleMissingHow_JComboBox.getSelected();
	String Reset = __Reset_JComboBox.getSelected();
	__error_wait = false;

	if ( TSID.length() > 0 ) {
		props.set ( "TSID", TSID );
	}
	if ( HandleMissingHow.length() > 0 ) {
		props.set ( "HandleMissingHow", HandleMissingHow );
	}
	if ( Reset.length() > 0 ) {
		props.set ( "Reset", Reset );
	}
	try {	// This will warn the user...
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
{	String TSID = __TSID_JComboBox.getSelected();
	String HandleMissingHow = __HandleMissingHow_JComboBox.getSelected();
	String Reset = __Reset_JComboBox.getSelected();
	if ( (Reset != null) && (Reset.length() > 0) ) {
		// Use the first token...
		Reset = StringUtil.getToken(Reset,"(",0,0).trim();
	}
	__command.setCommandParameter ( "TSID", TSID );
	__command.setCommandParameter ( "HandleMissingHow", HandleMissingHow );
	__command.setCommandParameter ( "Reset", Reset );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__TSID_JComboBox = null;
	__HandleMissingHow_JComboBox = null;
	__Reset_JComboBox = null;
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
{	__command = (cumulate_Command)command;

	addWindowListener( this );

        Insets insetsTLBR = new Insets(0,2,0,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The selected time series will be converted to cumulative" +
		" values over the period." ), 
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The units remain the original." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Time series to cumulate:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	
    Vector tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
    			(TSCommandProcessor)__command.getCommandProcessor(), __command );
	
	// Always allow a "*" to let all time series be filled...
	tsids.addElement ( "*" );
	__TSID_JComboBox = new SimpleJComboBox ( false );
	__TSID_JComboBox.setData ( tsids );
	__TSID_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __TSID_JComboBox,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Handle missing data how?:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__HandleMissingHow_JComboBox = new SimpleJComboBox ();
	__HandleMissingHow_JComboBox.addItem ( "" );
	__HandleMissingHow_JComboBox.addItem (
		__command._CarryForwardIfMissing );
	__HandleMissingHow_JComboBox.addItem (
		__command._SetMissingIfMissing );
	__HandleMissingHow_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __HandleMissingHow_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Default (blank) is to set missing."), 
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Reset date/time:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Reset_JComboBox = new SimpleJComboBox ();
	__Reset_JComboBox.addItem ( "" );
	for ( int i = 1; i <= 31; i++ ) {
		__Reset_JComboBox.addItem ( "Day " + i +
			" (for <= day interval)" );
	}
	for ( int i = 1; i <= 12; i++ ) {
		__Reset_JComboBox.addItem ( "Date " + i +
			"-1 (MM-DD for month interval)" );
	}
	for ( int i = 1; i <= 12; i++ ) {
		__Reset_JComboBox.addItem ( "Month " + i +
			" (for month interval)" );
	}
	__Reset_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __Reset_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Date/day/time on which to reset to zero."), 
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 40 );
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

	button_JPanel.add(__cancel_JButton = new SimpleJButton("Cancel", this));
	button_JPanel.add ( __ok_JButton = new SimpleJButton("OK", this) );

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
{	refresh();
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
	else {	// Combo box...
		refresh();
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
Refresh the command from the other text field contents.  The command is
of the form:
<pre>
cumulate(TSID="X",HandleMissingHow=X,Reset="X")
</pre>
*/
private void refresh ()
{	String TSID = "";
	String HandleMissingHow = "";
	String Reset = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		TSID = props.getValue ( "TSID" );
		HandleMissingHow = props.getValue ( "HandleMissingHow" );
		Reset = props.getValue ( "Reset" );
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
			else {	// Select the blank...
				__TSID_JComboBox.select ( 0 );
			}
		}
		if (	JGUIUtil.isSimpleJComboBoxItem(
				__HandleMissingHow_JComboBox, HandleMissingHow,
				JGUIUtil.NONE, null, null ) ) {
				__HandleMissingHow_JComboBox.select (
				HandleMissingHow );
		}
		else {	// Automatically add to the list after the blank...
			if (	(HandleMissingHow != null) &&
				(HandleMissingHow.length() > 0) ) {
				__HandleMissingHow_JComboBox.insertItemAt (
				HandleMissingHow, 1 );
				// Select...
				__HandleMissingHow_JComboBox.select (
				HandleMissingHow );
			}
			else {	// Select the blank...
				__HandleMissingHow_JComboBox.select ( 0 );
			}
		}
		try {	JGUIUtil.selectTokenMatches (	__Reset_JComboBox, true,
							"(", 0, 0, Reset, "",
							true );
		}
		catch ( Exception e ) {
			// Automatically add to the list after the blank...
			if (	(Reset != null) &&
				(Reset.length() > 0) ) {
				__Reset_JComboBox.insertItemAt ( Reset, 1 );
				// Select...
				__Reset_JComboBox.select ( Reset );
			}
			else {	// Select the blank...
				__Reset_JComboBox.select ( 0 );
			}
		}
	}
	// Regardless, reset the command from the fields...
	TSID = __TSID_JComboBox.getSelected();
	HandleMissingHow = __HandleMissingHow_JComboBox.getSelected();
	Reset = __Reset_JComboBox.getSelected();
	if ( (Reset != null) && (Reset.length() > 0) ) {
		// Use the first token...
		Reset = StringUtil.getToken(Reset,"(",0,0).trim();
	}
	props = new PropList ( __command.getCommandName() );
	props.add ( "TSID=" + TSID );
	props.add ( "HandleMissingHow=" + HandleMissingHow );
	props.add ( "Reset=" + Reset );
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
{	response ( true );
}

public void windowActivated( WindowEvent evt ){;}
public void windowClosed( WindowEvent evt ){;}
public void windowDeactivated( WindowEvent evt ){;}
public void windowDeiconified( WindowEvent evt ){;}
public void windowIconified( WindowEvent evt ){;}
public void windowOpened( WindowEvent evt ){;}

} // end cumulate_JDialog
