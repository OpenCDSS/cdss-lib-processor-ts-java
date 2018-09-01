package rti.tscommandprocessor.commands.time;

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

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.Command;
import RTi.Util.IO.PropList;

@SuppressWarnings("serial")
public class SetAveragePeriod_JDialog extends JDialog
implements ActionListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SetAveragePeriod_Command __command = null;
private JTextField __AverageStart_JTextField = null;
private JTextField __AverageEnd_JTextField = null;
private JTextArea __command_JTextArea = null; // Command as JTextField
private boolean __error_wait = false;	// Is there an error that we
						// are waiting to be cleared up
						// or Cancel?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command editor dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public SetAveragePeriod_JDialog ( JFrame parent, Command command )
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
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "SetAveragePeriod");
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
{   // Put together a list of parameters to check...
    PropList props = new PropList ( "" );
    String AverageStart = __AverageStart_JTextField.getText();
    String AverageEnd = __AverageEnd_JTextField.getText();
    __error_wait = false;

    if ( AverageStart.length() > 0 ) {
        props.set ( "AverageStart", AverageStart );
    }
    if ( AverageEnd.length() > 0 ) {
        props.set ( "AverageEnd", AverageEnd );
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
{   String AverageStart = __AverageStart_JTextField.getText();
    String AverageEnd = __AverageEnd_JTextField.getText();
    __command.setCommandParameter ( "AverageStart", AverageStart );
    __command.setCommandParameter ( "AverageEnd", AverageEnd );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__cancel_JButton = null;
	__AverageStart_JTextField = null;
	__AverageEnd_JTextField = null;
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
{	__command = (SetAveragePeriod_Command)command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

	// Main contents...
	
    JGUIUtil.addComponent(main_JPanel, new JLabel (
    "Use SetAveragePeriod() to limit the period used to compute historical averages immediately after data are read."),
    0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Calculating historical averages for data filling is only" +
		" supported for monthly and yearly time series."), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Averages are by default computed for the available period."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Enter dates as MM/YYYY or YYYY-MM (or YYYY for yearly).  Enter * to use all available data."), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
	JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Average period start:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__AverageStart_JTextField = new JTextField ( 24 );
	__AverageStart_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __AverageStart_JTextField,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Average period end:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__AverageEnd_JTextField = new JTextField ( 24 );
	__AverageEnd_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __AverageEnd_JTextField,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 4, 50 );
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	// Refresh the contents...
	refresh ();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__ok_JButton = new SimpleJButton("OK", this);
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add ( __ok_JButton );    
	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");
	
    setTitle ( "Edit " + __command.getCommandName() + " Command" );

    pack();
    JGUIUtil.center( this );
	setResizable ( false );
    super.setVisible( true );
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{
}

public void keyReleased ( KeyEvent event )
{	refresh ();
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
{   String AverageStart = "";
    String AverageEnd = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        AverageStart = props.getValue ( "AverageStart" );
        AverageEnd = props.getValue ( "AverageEnd" );
        if ( AverageStart != null ) {
            __AverageStart_JTextField.setText( AverageStart );
        }
        if ( AverageEnd != null ) {
            __AverageEnd_JTextField.setText( AverageEnd );
        }
    }
    // Regardless, reset the command from the fields...
    AverageStart = __AverageStart_JTextField.getText().trim();
    AverageEnd = __AverageEnd_JTextField.getText().trim();
    props = new PropList ( __command.getCommandName() );
    props.add ( "AverageStart=" + AverageStart );
    props.add ( "AverageEnd=" + AverageEnd );
    __command_JTextArea.setText(__command.toString(props) );
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

} // end setAveragePeriod_JDialog
