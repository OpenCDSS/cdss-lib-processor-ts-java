package rti.tscommandprocessor.commands.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

/**
Editor dialog for the testCommand() command.
*/
public class testCommand_JDialog extends JDialog
implements ActionListener, KeyListener, WindowListener
{

private SimpleJButton	__cancel_JButton = null,	// Cancel Button
			__ok_JButton = null;		// Ok Button
private SimpleJComboBox	__InitializeStatus_JComboBox = null;
private SimpleJComboBox	__DiscoveryStatus_JComboBox = null;
private SimpleJComboBox	__RunStatus_JComboBox = null;
private JTextArea	__command_JTextArea = null;	// Command as JTextArea
private boolean		__error_wait = false;
private boolean		__first_time = true;
private testCommand_Command __command = null;	// Command to edit
private boolean		__ok = false;		// Indicates whether the user
						// has pressed OK to close the
						// dialog.
/**
testCommand_JDialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public testCommand_JDialog ( JFrame parent, Command command )
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
	else {	// Choices...
		refresh();
	}
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	String InitializeStatus = __InitializeStatus_JComboBox.getSelected();
	String DiscoveryStatus = __DiscoveryStatus_JComboBox.getSelected();
	String RunStatus = __RunStatus_JComboBox.getSelected();
	__error_wait = false;
	if ( InitializeStatus.length() > 0 ) {
		props.set ( "InitializeStatus", InitializeStatus );
	}
	if ( DiscoveryStatus.length() > 0 ) {
		props.set ( "DiscoveryStatus", DiscoveryStatus );
	}
	if ( RunStatus.length() > 0 ) {
		props.set ( "RunStatus", RunStatus );
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
{	String routine = getClass().getName() + ".commitEdits";
	String InitializeStatus = __InitializeStatus_JComboBox.getSelected();
	String DiscoveryStatus = __DiscoveryStatus_JComboBox.getSelected();
	String RunStatus = __RunStatus_JComboBox.getSelected();
	Message.printStatus ( 2, routine, "Parameters before setCommandParameter(Initialize) = " +
			__command.getCommandParameters() );
	__command.setCommandParameter ( "InitializeStatus", InitializeStatus );
	Message.printStatus ( 2, routine, "Parameters before setCommandParameter(Discovery) = " +
			__command.getCommandParameters() );
	__command.setCommandParameter ( "DiscoveryStatus", DiscoveryStatus );
	Message.printStatus ( 2, routine, "Parameters before setCommandParameter(Run) = " +
			__command.getCommandParameters() );
	__command.setCommandParameter ( "RunStatus", RunStatus );
	Message.printStatus ( 2, routine, "Parameters after setCommandParameter(Run) = " +
			__command.getCommandParameters() );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__cancel_JButton = null;
	__command_JTextArea = null;
	__command = null;
	__InitializeStatus_JComboBox = null;
	__DiscoveryStatus_JComboBox = null;
	__RunStatus_JComboBox = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Command command )
{	__command = (testCommand_Command)command;
	CommandProcessor processor =__command.getCommandProcessor();

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command sets the status levels for the commands, in order to test TSTool's user interface." ),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Initialization status:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InitializeStatus_JComboBox = new SimpleJComboBox ( false );
	__InitializeStatus_JComboBox.addItem ( CommandStatusType.UNKNOWN.toString() );
	__InitializeStatus_JComboBox.addItem ( CommandStatusType.SUCCESS.toString() );
	__InitializeStatus_JComboBox.addItem ( CommandStatusType.WARNING.toString() );
	__InitializeStatus_JComboBox.addItem ( CommandStatusType.FAILURE.toString() );
	__InitializeStatus_JComboBox.select ( 0 );
	__InitializeStatus_JComboBox.addActionListener ( this );
        JGUIUtil.addComponent(main_JPanel, __InitializeStatus_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Specify the initialization status level to test."), 
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Discovery status:"),
        		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DiscoveryStatus_JComboBox = new SimpleJComboBox ( false );
	__DiscoveryStatus_JComboBox.addItem ( CommandStatusType.UNKNOWN.toString() );
	__DiscoveryStatus_JComboBox.addItem ( CommandStatusType.SUCCESS.toString() );
	__DiscoveryStatus_JComboBox.addItem ( CommandStatusType.WARNING.toString() );
	__DiscoveryStatus_JComboBox.addItem ( CommandStatusType.FAILURE.toString() );
	__DiscoveryStatus_JComboBox.select ( 0 );
	__DiscoveryStatus_JComboBox.addActionListener ( this );
        JGUIUtil.addComponent(main_JPanel, __DiscoveryStatus_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Specify the discovery status level to test."), 
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Run status:"),
    		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__RunStatus_JComboBox = new SimpleJComboBox ( false );
	__RunStatus_JComboBox.addItem ( CommandStatusType.UNKNOWN.toString() );
	__RunStatus_JComboBox.addItem ( CommandStatusType.SUCCESS.toString() );
	__RunStatus_JComboBox.addItem ( CommandStatusType.WARNING.toString() );
	__RunStatus_JComboBox.addItem ( CommandStatusType.FAILURE.toString() );
	__RunStatus_JComboBox.select ( 0 );
	__RunStatus_JComboBox.addActionListener ( this );
        JGUIUtil.addComponent(main_JPanel, __RunStatus_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Specify the run status level to test."), 
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 60 );
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.addKeyListener ( this );
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

	setTitle ( "Edit " + __command.getCommandName() + "() command" );

	// Dialogs do not need to be resizable...
	setResizable ( true );
        pack();
        JGUIUtil.center( this );
        super.setVisible( true );
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh ();
	}
}

public void keyReleased ( KeyEvent event )
{	refresh();
}

public void keyTyped ( KeyEvent event ) {;}

/**
Indicate if the user pressed OK (cancel otherwise).
*/
public boolean ok ()
{	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = "testCommand_JDialog.refresh";
	String InitializeStatus = "";
	String DiscoveryStatus = "";
	String RunStatus = "";
	if ( __first_time ) {
		__first_time = false;
		PropList parameters = __command.getCommandParameters();
		Message.printStatus ( 2, routine, "Parameters at startup = " +
				__command.getCommandParameters() );
		InitializeStatus = parameters.getValue ( "InitializeStatus" );
		DiscoveryStatus = parameters.getValue ( "DiscoveryStatus" );
		RunStatus = parameters.getValue ( "RunStatus" );
		if (	JGUIUtil.isSimpleJComboBoxItem(
			__InitializeStatus_JComboBox, InitializeStatus,
			JGUIUtil.NONE, null, null ) ) {
			__InitializeStatus_JComboBox.select ( InitializeStatus );
		}
		else {	if (	(InitializeStatus == null) ||
				InitializeStatus.equals("") ) {
				// New command...select the default...
				__InitializeStatus_JComboBox.select ( 0 );
			}
			else {	// Bad user command...
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"InitializStatus parameter \"" +
				InitializeStatus +
				"\".  Select a\ndifferent value or Cancel." );
			}
		}
		if (	JGUIUtil.isSimpleJComboBoxItem(
				__DiscoveryStatus_JComboBox, DiscoveryStatus,
				JGUIUtil.NONE, null, null ) ) {
				__DiscoveryStatus_JComboBox.select ( DiscoveryStatus );
		}
		else {	if (	(DiscoveryStatus == null) ||
					DiscoveryStatus.equals("") ) {
					// New command...select the default...
					__DiscoveryStatus_JComboBox.select ( 0 );
				}
				else {	// Bad user command...
					Message.printWarning ( 1, routine,
					"Existing command references an invalid\n"+
					"InitializStatus parameter \"" +
					DiscoveryStatus +
					"\".  Select a\ndifferent value or Cancel." );
				}
		}
		if (	JGUIUtil.isSimpleJComboBoxItem(
				__RunStatus_JComboBox, RunStatus,
				JGUIUtil.NONE, null, null ) ) {
				__RunStatus_JComboBox.select ( RunStatus );
			}
		else {	if (	(RunStatus == null) ||
					RunStatus.equals("") ) {
					// New command...select the default...
					__RunStatus_JComboBox.select ( 0 );
				}
				else {	// Bad user command...
					Message.printWarning ( 1, routine,
					"Existing command references an invalid\n"+
					"InitializStatus parameter \"" +
					RunStatus +
					"\".  Select a\ndifferent value or Cancel." );
				}
		}
	}
	// Regardless, reset the command from the fields.  This is only  visible
	// information that has not been committed in the command.
	InitializeStatus = __InitializeStatus_JComboBox.getSelected();
	DiscoveryStatus = __DiscoveryStatus_JComboBox.getSelected();
	RunStatus = __RunStatus_JComboBox.getSelected();
	PropList props = new PropList ( __command.getCommandName() );
	props.add ( "InitializeStatus=" + InitializeStatus );
	props.add ( "DiscoveryStatus=" + DiscoveryStatus );
	props.add ( "RunStatus=" + RunStatus );
	__command_JTextArea.setText( __command.toString(props) );
}

/**
React to the user response.
@param ok if false, then the edit is cancelled.  If true, the edit is committed
and the dialog is closed.
*/
public void response ( boolean ok )
{	__ok = ok;
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
