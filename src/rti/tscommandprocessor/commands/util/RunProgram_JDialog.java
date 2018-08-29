package rti.tscommandprocessor.commands.util;

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
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

@SuppressWarnings("serial")
public class RunProgram_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private RunProgram_Command __command = null;
private JTabbedPane __main_JTabbedPane = null;
private JTextArea __command_JTextArea = null;
private JTextArea __CommandLine_JTextArea = null;
private JTextField __Program_JTextField= null;
private JTextField [] __ProgramArg_JTextField = null;
private SimpleJComboBox __UseCommandShell_JComboBox = null;
private JTextField __CommandShell_JTextField= null;
private JTextField __Timeout_JTextField = null;
private JTextField __ExitStatusIndicator_JTextField = null;
private JTextField __ExitCodeProperty_JTextField = null;
private SimpleJComboBox __OutputCheckTableID_JComboBox = null;
private JTextField __OutputCheckWarningCountProperty_JTextField = null;
private JTextField __OutputCheckFailureCountProperty_JTextField = null;
private boolean __error_wait = false; // Is there an error waiting to be cleared up
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether the user has pressed OK to close the dialog.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of table identifiers to provide as choices
*/
public RunProgram_JDialog ( JFrame parent, RunProgram_Command command, List<String> tableIDChoices )
{   super(parent, true);
    initialize ( parent, command, tableIDChoices );
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
		HelpViewer.getInstance().showHelp("command", "RunProgram");
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
    String UseCommandShell =__UseCommandShell_JComboBox.getSelected();
    String CommandShell = __CommandShell_JTextField.getText().trim();
    String Timeout = __Timeout_JTextField.getText().trim();
    String ExitStatusIndicator = __ExitStatusIndicator_JTextField.getText().trim();
    String ExitCodeProperty = __ExitCodeProperty_JTextField.getText().trim();
    String OutputCheckTableID =__OutputCheckTableID_JComboBox.getSelected();
    String OutputCheckWarningCountProperty = __OutputCheckWarningCountProperty_JTextField.getText().trim();
    String OutputCheckFailureCountProperty = __OutputCheckFailureCountProperty_JTextField.getText().trim();
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
    if ( UseCommandShell.length() > 0 ) {
        props.set ( "UseCommandShell", UseCommandShell );
    }
    if ( CommandShell.length() > 0 ) {
        props.set ( "CommandShell", CommandShell );
    }
    if ( Timeout.length() > 0 ) {
        props.set ( "Timeout", Timeout );
    }
    if ( ExitStatusIndicator.length() > 0 ) {
        props.set ( "ExitStatusIndicator", ExitStatusIndicator );
    }
    if ( ExitCodeProperty.length() > 0 ) {
        props.set ( "ExitCodeProperty", ExitCodeProperty );
    }
    if ( OutputCheckWarningCountProperty.length() > 0 ) {
        props.set ( "OutputCheckWarningCountProperty", OutputCheckWarningCountProperty );
    }
    if ( OutputCheckFailureCountProperty.length() > 0 ) {
        props.set ( "OutputCheckFailureCountProperty", OutputCheckFailureCountProperty );
    }
    if ( OutputCheckTableID.length() > 0 ) {
        props.set ( "OutputCheckTableID", OutputCheckTableID );
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
    String UseCommandShell =__UseCommandShell_JComboBox.getSelected();
    String CommandShell = __CommandShell_JTextField.getText().trim();
    String Timeout = __Timeout_JTextField.getText().trim();
    String ExitStatusIndicator = __ExitStatusIndicator_JTextField.getText().trim();
    String ExitCodeProperty = __ExitCodeProperty_JTextField.getText().trim();
    String OutputCheckTableID =__OutputCheckTableID_JComboBox.getSelected();
    String OutputCheckWarningCountProperty = __OutputCheckWarningCountProperty_JTextField.getText().trim();
    String OutputCheckFailureCountProperty = __OutputCheckFailureCountProperty_JTextField.getText().trim();
    __command.setCommandParameter ( "CommandLine", CommandLine );
    __command.setCommandParameter ( "Program", Program );
    for ( int i = 0; i < __ProgramArg_JTextField.length; i++ ) {
        __command.setCommandParameter ( "ProgramArg" + (i + 1), ProgramArg[i] );
    }
    __command.setCommandParameter ( "UseCommandShell", UseCommandShell );
    __command.setCommandParameter ( "CommandShell", CommandShell );
    __command.setCommandParameter ( "Timeout", Timeout );
    __command.setCommandParameter ( "ExitStatusIndicator", ExitStatusIndicator );
    __command.setCommandParameter ( "ExitCodeProperty", ExitCodeProperty );
    __command.setCommandParameter ( "OutputCheckTableID", OutputCheckTableID );
    __command.setCommandParameter ( "OutputCheckWarningCountProperty", OutputCheckWarningCountProperty );
    __command.setCommandParameter ( "OutputCheckFailureCountProperty", OutputCheckFailureCountProperty );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, RunProgram_Command command, List<String> tableIDChoices )
{	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command runs another program."),
		0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"In order for TSTool to locate input and output files, one of the following approaches should be used:"),
		0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"     1) Start TSTool from the folder (directory) where files exist in order to use relative paths."),
		0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "     2) Start TSTool anywhere and use ${WorkingDir} in the command line to specify files relative to the working directory (folder)."),
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Specify the program to run using the command line OR separate arguments - the latter makes it simpler " +
        "to know how to treat whitespace in command line arguments."),
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
     
    // Panel for full command
    int yCommand = -1;
    JPanel command_JPanel = new JPanel();
    command_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Command (full)", command_JPanel );

    JGUIUtil.addComponent(command_JPanel, new JLabel (
	    "Simple commands can be specified as a single string."),
        0, ++yCommand, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(command_JPanel, new JLabel (
	    "Use \\\" to indicate double quotes if needed to surround program name or program command-line parameters - " +
	    "this may be needed if there are spaces in paths."),
        0, ++yCommand, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(command_JPanel, new JLabel (
	    "Specifying the command with a single string allows for redirection of output, for example:"),
        0, ++yCommand, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(command_JPanel, new JLabel (
	    "echo Hello > ${WorkingDir}/echo_out.txt"),
        0, ++yCommand, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(command_JPanel, new JLabel (
	    "<html><b>In the future a parameter may be added to read the command from a file.</b></html>"),
        0, ++yCommand, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(command_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yCommand, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(command_JPanel, new JLabel ( "Command to run (with arguments):" ), 
		0, ++yCommand, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__CommandLine_JTextArea = new JTextArea ( 4, 50 );
	__CommandLine_JTextArea.setToolTipText("Specify the full command line string, can use ${Property} notation.");
    __CommandLine_JTextArea.setLineWrap ( true );
    __CommandLine_JTextArea.setWrapStyleWord ( true );
	__CommandLine_JTextArea.setText( "");
	__CommandLine_JTextArea.addKeyListener ( this );
        JGUIUtil.addComponent(command_JPanel, new JScrollPane(__CommandLine_JTextArea),
		1, yCommand, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
    // Panel for command specified with parts
    int yParts = -1;
    JPanel parts_JPanel = new JPanel();
    parts_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Command (parts)", parts_JPanel );

    JGUIUtil.addComponent(parts_JPanel, new JLabel (
	    "Sometimes it is necessary to specify the command in parts so that separation between command line parts is explicit."),
        0, ++yParts, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(parts_JPanel, new JLabel (
	    "Use the following parameters to provide the command as parts."),
        0, ++yParts, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(parts_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yParts, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(parts_JPanel, new JLabel ( "Program to run:" ), 
        0, ++yParts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Program_JTextField = new JTextField ( "", 40 );
    __Program_JTextField.setToolTipText("Specify the program to run, can use ${Property} notation.");
    __Program_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(parts_JPanel, __Program_JTextField,
        1, yParts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(parts_JPanel, new JLabel(
        "Required - if full command line is not specified."), 
        3, yParts, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    __ProgramArg_JTextField = new JTextField[__command._ProgramArg_SIZE];
    for ( int i = 0; i < __ProgramArg_JTextField.length; i++ ) {
        JGUIUtil.addComponent(parts_JPanel, new JLabel ( "Program argument " + (i + 1) + ":" ), 
            0, ++yParts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
        __ProgramArg_JTextField[i] = new JTextField ( "", 40 );
        __ProgramArg_JTextField[i].setToolTipText("Specify the program command-line parameters, can use ${Property} notation.");
        __ProgramArg_JTextField[i].addKeyListener ( this );
        JGUIUtil.addComponent(parts_JPanel, __ProgramArg_JTextField[i],
            1, yParts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(parts_JPanel, new JLabel("Optional - as needed if Program is specified."), 
            3, yParts, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    
    // Panel for command shell
    int yShell = -1;
    JPanel shell_JPanel = new JPanel();
    shell_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Command shell", shell_JPanel );

    JGUIUtil.addComponent(shell_JPanel, new JLabel (
        "The program by default will be run with a command shell (e.g., cmd.exe on Windows)."),
        0, ++yShell, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(shell_JPanel, new JLabel (
        "This is often helpful because it allows the shell to initialize the environment for the program."),
        0, ++yShell, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(shell_JPanel, new JLabel (
        "A shell is necessary if the program to run is a batch file (Windows) or shell script (Linux) that is parsed and run by the shell."),
        0, ++yShell, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(shell_JPanel, new JLabel (
        "Indicate NOT to use a command shell if it is known that the program is an executable (not a shell command or script)."),
        0, ++yShell, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(shell_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yShell, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(shell_JPanel, new JLabel ( "Use command shell:" ), 
        0, ++yShell, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __UseCommandShell_JComboBox = new SimpleJComboBox ( false );
    __UseCommandShell_JComboBox.add ( "" );
    __UseCommandShell_JComboBox.add ( __command._False );
    __UseCommandShell_JComboBox.add ( __command._True );
    __UseCommandShell_JComboBox.select ( 0 );
    __UseCommandShell_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(shell_JPanel, __UseCommandShell_JComboBox,
        1, yShell, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(shell_JPanel, new JLabel(
        "Optional - use command shell (default="+ __command._True + ")."), 
        3, yShell, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(shell_JPanel, new JLabel ( "Command shell:" ), 
		0, ++yShell, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__CommandShell_JTextField = new JTextField ( "", 10 );
	__CommandShell_JTextField.setToolTipText("Specify the program command shell program, can use ${Property} notation.");
	__CommandShell_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(shell_JPanel, __CommandShell_JTextField,
		1, yShell, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(shell_JPanel, new JLabel( "Optional - shell program to run (default=depends on system)."), 
        3, yShell, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Panel for command timeout
    int yTimeout = -1;
    JPanel timeout_JPanel = new JPanel();
    timeout_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Timeout", timeout_JPanel );

    JGUIUtil.addComponent(timeout_JPanel, new JLabel (
		"TSTool will for the called program to complete before continuing."),
		0, ++yTimeout, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(timeout_JPanel, new JLabel (
		"This allows TSTool to check for errors and process output from the program."),
		0, ++yTimeout, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(timeout_JPanel, new JLabel (
		"Specify the timeout to ensure that TSTool does not wait indefinitely for the program to finish."),
		0, ++yTimeout, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(timeout_JPanel, new JLabel (
		"<html><b>May add parameters to control whether timeout is treated as an error.</b></html>"),
		0, ++yTimeout, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(timeout_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yTimeout, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(timeout_JPanel, new JLabel ( "Timeout (seconds):" ), 
		0, ++yTimeout, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Timeout_JTextField = new JTextField ( "", 10 );
	__Timeout_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(timeout_JPanel, __Timeout_JTextField,
		1, yTimeout, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(timeout_JPanel, new JLabel( "Optional - default is no timeout."), 
        3, yTimeout, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Panel for exit code
    int yExit = -1;
    JPanel exit_JPanel = new JPanel();
    exit_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Exit Code Check", exit_JPanel );

    JGUIUtil.addComponent(exit_JPanel, new JLabel (
        "Specify the exit status indicator if program output messages must be used to determine the program " +
        "exit status (e.g., \"Status:\")."),
        0, ++yExit, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(exit_JPanel, new JLabel (
        "<html><b>The ExitStatusIndicator parameter functionality is being reviewed and may be changed.</b></html>"),
        0, ++yExit, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(exit_JPanel, new JLabel (
        "<html><b>In the future a table may be added to control how exit code is handled, similar to output checks.</b></html>"),
        0, ++yExit, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(exit_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yExit, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(exit_JPanel, new JLabel ( "Exit status indicator:" ), 
        0, ++yExit, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ExitStatusIndicator_JTextField = new JTextField ( "", 10 );
    __ExitStatusIndicator_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(exit_JPanel, __ExitStatusIndicator_JTextField,
        1, yExit, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(exit_JPanel, new JLabel(
        "Optional - output string to indicate status (default=use process exit status)."), 
        3, yExit, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(exit_JPanel, new JLabel("Exit code property:"),
        0, ++yExit, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ExitCodeProperty_JTextField = new JTextField ( "", 20 );
    __ExitCodeProperty_JTextField.setToolTipText("Processor property to set to program exit code, can use ${Property} notation");
    __ExitCodeProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(exit_JPanel, __ExitCodeProperty_JTextField,
        1, yExit, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(exit_JPanel, new JLabel ( "Optional - processor property to set as program exit code." ),
        3, yExit, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Panel for output checks
    int yCheck = -1;
    JPanel check_JPanel = new JPanel();
    check_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output Checks", check_JPanel );

    JGUIUtil.addComponent(check_JPanel, new JLabel (
		"The program that is called may generate one or more output files that contain information indicating success, warnings, and errors."),
		0, ++yCheck, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel (
		"Use the table to specify files to search for output, and how to interpret text in files as warnings and errors."),
		0, ++yCheck, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel (
		"Table columns should be:"),
		0, ++yCheck, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel (
		"  File - name of file to search, use \"stdout\" for console output, can contain ${Property}"),
		0, ++yCheck, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel (
		"  Pattern - pattern to match in file, using literal text and *, can contain ${Property}"),
		0, ++yCheck, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel (
		"  Level - for match, level of message, one of:  Status, Warning, Failure"),
		0, ++yCheck, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel (
		"  Message - message for TSTool command status, use ${file.line:text} to include output line text, can contain ${Property}"),
		0, ++yCheck, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel (
		"  Recommendation - recommendation for TSTool command status, can contain ${Property}"),
		0, ++yCheck, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yCheck, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(check_JPanel, new JLabel ( "Output check table ID:" ), 
        0, ++yCheck, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputCheckTableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit
    __OutputCheckTableID_JComboBox.setToolTipText("Specify the table ID or use ${Property} notation");
    tableIDChoices.add(0,""); // Add blank to ignore table
    __OutputCheckTableID_JComboBox.setData ( tableIDChoices );
    __OutputCheckTableID_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(check_JPanel, __OutputCheckTableID_JComboBox,
        1, yCheck, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel( "Optional - table with output check information."), 
        3, yCheck, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(check_JPanel, new JLabel("Output check warning count property:"),
        0, ++yCheck, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputCheckWarningCountProperty_JTextField = new JTextField ( "", 20 );
    __OutputCheckWarningCountProperty_JTextField.setToolTipText("Processor property to set to output check warning count, can use ${Property} notation");
    __OutputCheckWarningCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(check_JPanel, __OutputCheckWarningCountProperty_JTextField,
        1, yCheck, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel ( "Optional - processor property to set as warning count." ),
        3, yCheck, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(check_JPanel, new JLabel("Output check failure count property:"),
        0, ++yCheck, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputCheckFailureCountProperty_JTextField = new JTextField ( "", 20 );
    __OutputCheckFailureCountProperty_JTextField.setToolTipText("Processor property to set to output check failure count, can use ${Property} notation");
    __OutputCheckFailureCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(check_JPanel, __OutputCheckFailureCountProperty_JTextField,
        1, yCheck, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel ( "Optional - processor property to set as failure count." ),
        3, yCheck, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

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

	__ok_JButton = new SimpleJButton("OK", this);
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add ( __ok_JButton );
	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

    setTitle ( "Edit " + __command.getCommandName() + " command" );
    pack();
    JGUIUtil.center( this );
	setResizable ( false );
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged(ItemEvent event)
{
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
{	String routine = getClass().getSimpleName() + ".refresh";
	String CommandLine = "";
    String Program = "";
    String [] ProgramArg = new String[__ProgramArg_JTextField.length];
    String UseCommandShell = "";
    String CommandShell = "";
	String Timeout = "";
	String ExitStatusIndicator = "";
	String ExitCodeProperty = "";
	String OutputCheckTableID = "";
	String OutputCheckWarningCountProperty = "";
	String OutputCheckFailureCountProperty = "";
    PropList parameters = null;
    if ( __first_time ) {
        __first_time = false;
        parameters = __command.getCommandParameters();
        CommandLine = parameters.getValue ( "CommandLine" );
        Program = parameters.getValue ( "Program" );
        for ( int i = 0; i < ProgramArg.length; i++ ) {
            ProgramArg[i] = parameters.getValue ( "ProgramArg" + (i + 1) );
        }
        UseCommandShell = parameters.getValue ( "UseCommandShell" );
        CommandShell = parameters.getValue ( "CommandShell" );
        Timeout = parameters.getValue ( "Timeout" );
        ExitStatusIndicator = parameters.getValue ( "ExitStatusIndicator" );
        ExitCodeProperty = parameters.getValue ( "ExitCodeProperty" );
        OutputCheckTableID = parameters.getValue ( "OutputCheckTableID" );
        OutputCheckWarningCountProperty = parameters.getValue ( "OutputCheckWarningCountProperty" );
        OutputCheckFailureCountProperty = parameters.getValue ( "OutputCheckFailureCountProperty" );
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
        if ( UseCommandShell == null ) {
            // Select default...
            __UseCommandShell_JComboBox.select ( 0 );
        }
        else {  
            if ( JGUIUtil.isSimpleJComboBoxItem( __UseCommandShell_JComboBox,
                UseCommandShell, JGUIUtil.NONE, null,null)){
                __UseCommandShell_JComboBox.select ( UseCommandShell );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid UseCommandShell parameter \""
                + UseCommandShell + "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( CommandShell != null ) {
            __CommandShell_JTextField.setText ( CommandShell );
        }
        if ( Timeout != null ) {
            __Timeout_JTextField.setText ( Timeout );
        }
        if ( ExitStatusIndicator != null ) {
            __ExitStatusIndicator_JTextField.setText ( ExitStatusIndicator );
        }
        if ( ExitCodeProperty != null ) {
            __ExitCodeProperty_JTextField.setText ( ExitCodeProperty );
        }
        if ( OutputCheckTableID == null ) {
            // Select default...
            __OutputCheckTableID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __OutputCheckTableID_JComboBox,OutputCheckTableID, JGUIUtil.NONE, null, null ) ) {
                __OutputCheckTableID_JComboBox.select ( OutputCheckTableID );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nOutputCheckTableID value \"" + OutputCheckTableID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( OutputCheckWarningCountProperty != null ) {
            __OutputCheckWarningCountProperty_JTextField.setText ( OutputCheckWarningCountProperty );
        }
        if ( OutputCheckFailureCountProperty != null ) {
            __OutputCheckFailureCountProperty_JTextField.setText ( OutputCheckFailureCountProperty );
        }
	}
	// Regardless, reset the command from the fields...
    CommandLine = __CommandLine_JTextArea.getText();
    Program = __Program_JTextField.getText();
    for ( int i = 0; i < ProgramArg.length; i++ ) {
        ProgramArg[i] = __ProgramArg_JTextField[i].getText();
    }
    UseCommandShell =__UseCommandShell_JComboBox.getSelected();
    CommandShell = __CommandShell_JTextField.getText();
    Timeout = __Timeout_JTextField.getText();
    ExitStatusIndicator = __ExitStatusIndicator_JTextField.getText();
    ExitCodeProperty = __ExitCodeProperty_JTextField.getText();
    OutputCheckTableID =__OutputCheckTableID_JComboBox.getSelected();
    OutputCheckWarningCountProperty = __OutputCheckWarningCountProperty_JTextField.getText();
    OutputCheckFailureCountProperty = __OutputCheckFailureCountProperty_JTextField.getText();
    PropList props = new PropList ( __command.getCommandName() );
    props.add ( "CommandLine=" + CommandLine );
    props.add ( "Program=" + Program );
    for ( int i = 0; i < ProgramArg.length; i++ ) {
        props.add ( "ProgramArg" + (i + 1) + "=" + ProgramArg[i] );
    }
    props.add ( "UseCommandShell=" + UseCommandShell );
    props.add ( "CommandShell=" + CommandShell );
    props.add ( "Timeout=" + Timeout );
    props.add ( "ExitStatusIndicator=" + ExitStatusIndicator );
    props.add ( "ExitCodeProperty=" + ExitCodeProperty );
    props.add ( "OutputCheckTableID=" + OutputCheckTableID );
    props.add ( "OutputCheckWarningCountProperty=" + OutputCheckWarningCountProperty );
    props.add ( "OutputCheckFailureCountProperty=" + OutputCheckFailureCountProperty );
    __command_JTextArea.setText( __command.toString(props) );
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
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