// StartRegressionTestResultsReport_JDialog - editor for StartRegressionTestResultsReport command

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2019 Colorado Department of Natural Resources

CDSS Time Series Processor Java Library is free software:  you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    CDSS Time Series Processor Java Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with CDSS Time Series Processor Java Library.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

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
import java.io.File;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

@SuppressWarnings("serial")
public class StartRegressionTestResultsReport_JDialog extends JDialog
implements ActionListener, KeyListener, WindowListener
{
    
private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __browse_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SimpleJButton __path_JButton = null;
private JTextField __OutputFile_JTextField = null;
private JTextField __TestResultsTableID_JTextField = null;
private JTextArea __command_JTextArea = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private StartRegressionTestResultsReport_Command __command = null;	// Command to edit
private boolean __ok = false; // Whether the user has pressed OK to close the dialog.
private String __working_dir = null;

/**
Editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public StartRegressionTestResultsReport_JDialog ( JFrame parent, StartRegressionTestResultsReport_Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();

	if ( o == __browse_JButton ) {
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
		}
		else {	fc = JFileChooserFactory.createJFileChooser( __working_dir );
		}
		fc.setDialogTitle("Select Regression Test Results Report File");
		SimpleFileFilter sff = new SimpleFileFilter("txt","Report File");
		fc.addChoosableFileFilter(sff);
		
		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			String directory = fc.getSelectedFile().getParent();
			String filename = fc.getSelectedFile().getName(); 
			String path = fc.getSelectedFile().getPath(); 
	
			if (filename == null || filename.equals("")) {
				return;
			}
	
			if (path != null) {
				// Convert path to relative path by default.
				try {
					__OutputFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1,"StartRegressionTestResultsReport_JDialog", "Error converting file to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory(directory );
				refresh();
			}
		}
	}
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "StartRegressionTestResultsReport");
	}
	else if (o == __ok_JButton) {
		refresh();
		checkInput();
		if (!__error_wait) {
			response(true);
		}
	}
	else if ( o == __path_JButton ) {
		if ( __path_JButton.getText().equals(__AddWorkingDirectory) ) {
			__OutputFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir,
			__OutputFile_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
				__OutputFile_JTextField.setText (IOUtil.toRelativePath ( __working_dir,
				__OutputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,
				"StartRegressionTestResultsReport_JDialog",
				"Error converting file to relative path." );
			}
		}
		refresh ();
	}
	else {	// Combo box...
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
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String TestResultsTableID = __TestResultsTableID_JTextField.getText().trim();
	if ( OutputFile.length() > 0 ) {
		props.set ( "OutputFile", OutputFile );
	}
	if ( TestResultsTableID.length() > 0 ) {
		props.set ( "TestResultsTableID", TestResultsTableID );
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
Commit the edits to the command.  In this case the command should be reparsed
to check its low-level values.
*/
private void commitEdits ()
{	String OutputFile = __OutputFile_JTextField.getText().trim();
	String TestResultsTableID = __TestResultsTableID_JTextField.getText().trim();
	__command.setCommandParameter ( "OutputFile", OutputFile );
	__command.setCommandParameter ( "TestResultsTableID", TestResultsTableID );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, StartRegressionTestResultsReport_Command command )
{	__command = command;
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)__command.getCommandProcessor(), __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"(Re)start the regression test results report file, written to by the RunCommands() command to summarize automated test results." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    /*
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"A blank log file name will restart the current file."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    */
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The report file can be specified using a full or relative path (relative to the working directory)."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel ( "The working directory is:" ), 
        	0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel ( "  " + __working_dir ), 
        	0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The output file includes numbers for each test."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"An output file with 'nonum' in the name is automatically created and is formatted to allow comparison with previous runs."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++y, 8, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Report (output) file:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputFile_JTextField = new JTextField ( 50 );
	__OutputFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel
	JPanel OutputFile_JPanel = new JPanel();
	OutputFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(OutputFile_JPanel, __OutputFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
	__browse_JButton = new SimpleJButton ( "...", this );
	__browse_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(OutputFile_JPanel, __browse_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__path_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(OutputFile_JPanel, __path_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
	JGUIUtil.addComponent(main_JPanel, OutputFile_JPanel,
		1, y, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Test results table ID:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TestResultsTableID_JTextField = new JTextField (10);
    __TestResultsTableID_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __TestResultsTableID_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - identifier for table containing results."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 60 );
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.addKeyListener ( this );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	button_JPanel.add ( __ok_JButton = new SimpleJButton("OK", this) );
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add(__cancel_JButton = new SimpleJButton("Cancel", this));
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + " command" );
	
	// Refresh the contents...
    refresh ();

    pack();
    JGUIUtil.center( this );
	// Dialogs do not need to be resizable...
	setResizable ( false );
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
{	String OutputFile = "";
    String TestResultsTableID = "";
    PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
        parameters = __command.getCommandParameters();
		OutputFile = parameters.getValue ( "OutputFile" );
		TestResultsTableID = parameters.getValue ( "TestResultsTableID" );
		if ( OutputFile != null ) {
			__OutputFile_JTextField.setText ( OutputFile );
		}
		if ( TestResultsTableID != null ) {
			__TestResultsTableID_JTextField.setText ( TestResultsTableID );
		}
	}
	// Regardless, reset the command from the fields.  This is only visible
	// information that has not been committed in the command.
	OutputFile = __OutputFile_JTextField.getText().trim();
	TestResultsTableID = __TestResultsTableID_JTextField.getText().trim();
	parameters = new PropList ( __command.getCommandName() );
	parameters.add ( "OutputFile=" + OutputFile );
	parameters.add ( "TestResultsTableID=" + TestResultsTableID );
	__command_JTextArea.setText( __command.toString(parameters).trim() );
	// Check the path and determine what the label on the path button should be...
	if ( (OutputFile == null) || (OutputFile.length() == 0) ) {
		if ( __path_JButton != null ) {
			__path_JButton.setEnabled ( false );
		}
	}
	if ( __path_JButton != null ) {
		if ( (OutputFile != null) && !OutputFile.isEmpty() ) {
			__path_JButton.setEnabled ( true );
			File f = new File ( OutputFile );
			if ( f.isAbsolute() ) {
				__path_JButton.setText ( __RemoveWorkingDirectory );
				__path_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__path_JButton.setText ( __AddWorkingDirectory );
            	__path_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__path_JButton.setEnabled(false);
		}
	}
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
