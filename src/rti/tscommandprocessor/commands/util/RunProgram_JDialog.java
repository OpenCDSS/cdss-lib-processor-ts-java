// ----------------------------------------------------------------------------
// runProgram_JDialog - editor for runProgram()
// ----------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
// ----------------------------------------------------------------------------
// History: 
//
// 22 Aug 2001	Steven A. Malers, RTi	Initial version (copy and modify
//					setMissingDataValue_Dialog).
//					Might need to add pause also to let
//					OS finish with files.
// 2002-04-08	SAM, RTi		Clean up dialog.
// 2003-12-03	SAM, RTi		Update to Swing.
// 2007-02-26	SAM, RTi		Clean up code based on Eclipse feedback.
// ----------------------------------------------------------------------------

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

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.Command;
import RTi.Util.IO.PropList;

public class RunProgram_JDialog extends JDialog
implements ActionListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null,// Cancel Button
			__ok_JButton = null;	// Ok Button
private RunProgram_Command __command = null;    // Command to edit
private JTextArea __command_JTextArea=null;
private JTextArea __CommandLine_JTextArea = null;
private JTextField __Program_JTextField= null;
private JTextField [] __ProgramArg_JTextField = null;
private JTextField __Timeout_JTextField = null;
private JTextField __ExitStatusIndicator_JTextField = null;
private boolean __error_wait = false;	// Is there an error waiting to be cleared up
private boolean __first_time = true;
private boolean __ok = false;       // Indicates whether the user has pressed OK to close the dialog.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public RunProgram_JDialog ( JFrame parent, Command command )
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
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{   // Put together a list of parameters to check...
    PropList props = new PropList ( "" );
    String CommandLine = __CommandLine_JTextArea.getText().trim();
    String Program = __Program_JTextField.getText().trim();
    String [] ProgramArg = new String[__ProgramArg_JTextField.length];
    for ( int i = 0; i < __ProgramArg_JTextField.length; i++ ) {
        ProgramArg[i] = __ProgramArg_JTextField[i].getText().trim();
    }
    String Timeout = __Timeout_JTextField.getText().trim();
    String ExitStatusIndicator = __ExitStatusIndicator_JTextField.getText().trim();
    __error_wait = false;
    if ( CommandLine.length() > 0 ) {
        props.set ( "CommandLine", CommandLine );
    }
    if ( Program.length() > 0 ) {
        props.set ( "Program", Program );
    }
    for ( int i = 0; i < __ProgramArg_JTextField.length; i++ ) {
        if ( Program.length() > 0 ) {
            ProgramArg[i] = __ProgramArg_JTextField[i].getText().trim();
        }
    }
    if ( Timeout.length() > 0 ) {
        props.set ( "Timeout", Timeout );
    }
    if ( ExitStatusIndicator.length() > 0 ) {
        props.set ( "ExitStatusIndicator", ExitStatusIndicator );
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
{   String CommandLine = __CommandLine_JTextArea.getText().trim();
    String Program = __Program_JTextField.getText().trim();
    String [] ProgramArg = new String[__ProgramArg_JTextField.length];
    for ( int i = 0; i < __ProgramArg_JTextField.length; i++ ) {
        ProgramArg[i] = __ProgramArg_JTextField[i].getText().trim();
    }
    String Timeout = __Timeout_JTextField.getText().trim();
    String ExitStatusIndicator = __ExitStatusIndicator_JTextField.getText().trim();
    __command.setCommandParameter ( "CommandLine", CommandLine );
    __command.setCommandParameter ( "Program", Program );
    for ( int i = 0; i < __ProgramArg_JTextField.length; i++ ) {
        __command.setCommandParameter ( "ProgramArg" + (i + 1), ProgramArg[i] );
    }
    __command.setCommandParameter ( "Timeout", Timeout );
    __command.setCommandParameter ( "ExitStatusIndicator", ExitStatusIndicator );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__cancel_JButton = null;
	__CommandLine_JTextArea = null;
	__Timeout_JTextField = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Command command )
{	__command = (RunProgram_Command)command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);
    Insets insetsMin = new Insets(0,2,0,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command runs another program, and TSTool waits for it to complete before continuing."),
		0, y, 7, 1, 1, 0, insetsMin, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Commands must use a full path to files, TSTool must be started from the directory where files exist " +
		"to use relative paths, or "),
		0, ++y, 7, 1, 1, 0, insetsMin, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "use ${WorkingDir} in the command line to specify files relative to the working directory."),
        0, ++y, 7, 1, 1, 0, insetsMin, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
    "Use \\\" to indicate double quotes if needed to surround program name or program command-line parameters - " +
    "this may be needed if there are spaces in paths."),
    0, ++y, 7, 1, 1, 0, insetsMin, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Specify the exit status indicator if program output messages must be used to determine the program " +
        "exit status (e.g., \"Status:\")."),
        0, ++y, 7, 1, 1, 0, insetsMin, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
            "Specify the program to run using the command line OR separate arguments - the latter makes it simpler " +
            "to know how to treat whitespace in command line arguments."),
            0, ++y, 7, 1, 1, 0, insetsMin, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command to run (with arguments):" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__CommandLine_JTextArea = new JTextArea ( 4, 50 );
    __CommandLine_JTextArea.setLineWrap ( true );
    __CommandLine_JTextArea.setWrapStyleWord ( true );
	__CommandLine_JTextArea.setText( "");
	__CommandLine_JTextArea.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, new JScrollPane(__CommandLine_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Program to run:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Program_JTextField = new JTextField ( "", 40 );
    __Program_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __Program_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Required if command line is not specified."), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    __ProgramArg_JTextField = new JTextField[__command._ProgramArg_SIZE];
    for ( int i = 0; i < __ProgramArg_JTextField.length; i++ ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Program argument " + (i + 1) + ":" ), 
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
        __ProgramArg_JTextField[i] = new JTextField ( "", 40 );
        __ProgramArg_JTextField[i].addKeyListener ( this );
            JGUIUtil.addComponent(main_JPanel, __ProgramArg_JTextField[i],
            1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
            "As needed if Program is specified."), 
            3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Timeout (seconds):" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Timeout_JTextField = new JTextField ( "", 10 );
	__Timeout_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __Timeout_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - default is no timeout."), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Exit status indicator:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ExitStatusIndicator_JTextField = new JTextField ( "", 10 );
    __ExitStatusIndicator_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __ExitStatusIndicator_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - output string to indicate status (default=use process exit status)."), 
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

	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );

    setTitle ( "Edit " + __command.getCommandName() + "() command" );
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
		checkInput();
		if ( !__error_wait ) {
			response ( false );
		}
	}
}

public void keyReleased ( KeyEvent event )
{
    refresh();
}

public void keyTyped ( KeyEvent event ) {;}

/**
Indicate if the user pressed OK (cancel otherwise).
*/
public boolean ok ()
{   return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String CommandLine = "";
    String Program = "";
    String [] ProgramArg = new String[__ProgramArg_JTextField.length];
	String Timeout = "";
	String ExitStatusIndicator = "";
    PropList parameters = null;
    if ( __first_time ) {
        __first_time = false;
        parameters = __command.getCommandParameters();
        CommandLine = parameters.getValue ( "CommandLine" );
        Program = parameters.getValue ( "Program" );
        for ( int i = 0; i < ProgramArg.length; i++ ) {
            ProgramArg[i] = parameters.getValue ( "ProgramArg" + (i + 1) );
        }
        Timeout = parameters.getValue ( "Timeout" );
        ExitStatusIndicator = parameters.getValue ( "ExitStatusIndicator" );
        if ( CommandLine != null ) {
            __CommandLine_JTextArea.setText ( CommandLine );
        }
        if ( Program != null ) {
            __Program_JTextField.setText ( Program );
        }
        for ( int i = 0; i < ProgramArg.length; i++ ) {
            if ( ProgramArg[i] != null ) {
                __ProgramArg_JTextField[i].setText ( ProgramArg[i] );
            }  
        }
        if ( Timeout != null ) {
            __Timeout_JTextField.setText ( Timeout );
        }
	}
	// Regardless, reset the command from the fields...
    CommandLine = __CommandLine_JTextArea.getText();
    Program = __Program_JTextField.getText();
    for ( int i = 0; i < ProgramArg.length; i++ ) {
        ProgramArg[i] = __ProgramArg_JTextField[i].getText();
    }
    Timeout = __Timeout_JTextField.getText();
    ExitStatusIndicator = __ExitStatusIndicator_JTextField.getText();
    PropList props = new PropList ( __command.getCommandName() );
    props.add ( "CommandLine=" + CommandLine );
    props.add ( "Program=" + Program );
    for ( int i = 0; i < ProgramArg.length; i++ ) {
        props.add ( "ProgramArg" + (i + 1) + "=" + ProgramArg[i] );
    }
    props.add ( "Timeout=" + Timeout );
    props.add ( "ExitStatusIndicator=" + ExitStatusIndicator );
    __command_JTextArea.setText( __command.toString(props) );
}

/**
React to the user response.
@param ok if false, then the edit is cancelled.  If true, the edit is committed
and the dialog is closed.
*/
public void response ( boolean ok )
{   __ok = ok;
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