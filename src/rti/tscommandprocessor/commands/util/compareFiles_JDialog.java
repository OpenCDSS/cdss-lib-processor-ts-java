// ----------------------------------------------------------------------------
// compareFiles_JDialog - editor for compareFiles()
// ----------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
// ----------------------------------------------------------------------------
// History: 
//
// 2006-05-01	Steven A. Malers, RTi	Initial version (copy and modify
//					compareTimeSeries_JDialog).
// 2006-05-03	SAM, RTi		Add WarnIfSame parameter.
// 2007-02-16	SAM, RTi		Update for new CommandProcessor interface.
//					Clean up code based on Eclipse feedback.
// 2007-05-08	SAM, RTi		Cleanup code based on Eclipse feedback.
// ----------------------------------------------------------------------------

package rti.tscommandprocessor.commands.util;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

public class compareFiles_JDialog extends JDialog
implements ActionListener, KeyListener, WindowListener
{
private final String	__AddWorkingDirectoryFile1 =
				"Add Working Directory (File 1)";
private final String	__AddWorkingDirectoryFile2 =
				"Add Working Directory (File 2)";
private final String	__RemoveWorkingDirectoryFile1 =
				"Remove Working Directory (File 1)";
private final String	__RemoveWorkingDirectoryFile2 =
				"Remove Working Directory (File 2)";


private SimpleJButton	__browse1_JButton = null,
			__browse2_JButton = null,
			__path1_JButton = null,
			__path2_JButton = null,
			__cancel_JButton = null,	// Cancel Button
			__ok_JButton = null;		// Ok Button
private JTextField	__InputFile1_JTextField = null;	// First file
private JTextField	__InputFile2_JTextField = null;	// Second file
private SimpleJComboBox	__WarnIfDifferent_JComboBox =null;
private SimpleJComboBox	__WarnIfSame_JComboBox =null;
private JTextArea	__command_JTextArea = null;	// Command as JTextField
private String		__working_dir = null;	// Working directory.
private boolean		__error_wait = false;
private boolean		__first_time = true;
private compareFiles_Command __command = null;	// Command to edit
private boolean		__ok = false;		// Indicates whether the user
						// has pressed OK to close the
						// dialog.
/**
compareFiles_JDialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public compareFiles_JDialog ( JFrame parent, Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();

	if ( o == __browse1_JButton ) {
		String last_directory_selected =
			JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser(
				last_directory_selected );
		}
		else {	fc = JFileChooserFactory.createJFileChooser(
				__working_dir );
		}
		fc.setDialogTitle( "Select First File to Compare");
		
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			String directory = fc.getSelectedFile().getParent();
			String filename = fc.getSelectedFile().getName(); 
			String path = fc.getSelectedFile().getPath(); 
	
			if (filename == null || filename.equals("")) {
				return;
			}
	
			if (path != null) {
				__InputFile1_JTextField.setText(path );
				JGUIUtil.setLastFileDialogDirectory(directory);
				refresh();
			}
		}
	}
	else if ( o == __browse2_JButton ) {
		String last_directory_selected =
			JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser(
				last_directory_selected );
		}
		else {	fc = JFileChooserFactory.createJFileChooser(
				__working_dir );
		}
		fc.setDialogTitle( "Select Second File to Compare");
		
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			String directory = fc.getSelectedFile().getParent();
			String filename = fc.getSelectedFile().getName(); 
			String path = fc.getSelectedFile().getPath(); 
	
			if (filename == null || filename.equals("")) {
				return;
			}
	
			if (path != null) {
				__InputFile2_JTextField.setText(path );
				JGUIUtil.setLastFileDialogDirectory(directory);
				refresh();
			}
		}
	}
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else if ( o == __path1_JButton ) {
		if (	__path1_JButton.getText().equals(
			__AddWorkingDirectoryFile1) ) {
			__InputFile1_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir,
			__InputFile1_JTextField.getText() ) );
		}
		else if ( __path1_JButton.getText().equals(
			__RemoveWorkingDirectoryFile1) ) {
			try {	__InputFile1_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir,
				__InputFile1_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,"compareFiles_JDialog",
				"Error converting first file name to " +
				"relative path." );
			}
		}
		refresh ();
	}
	else if ( o == __path2_JButton ) {
		if (	__path2_JButton.getText().equals(
			__AddWorkingDirectoryFile2) ) {
			__InputFile2_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir,
			__InputFile2_JTextField.getText() ) );
		}
		else if ( __path2_JButton.getText().equals(
			__RemoveWorkingDirectoryFile2) ) {
			try {	__InputFile2_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir,
				__InputFile2_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,"compareFiles_JDialog",
				"Error converting first file name to " +
				"relative path." );
			}
		}
		refresh ();
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
	String InputFile1 = __InputFile1_JTextField.getText().trim();
	String InputFile2 = __InputFile2_JTextField.getText().trim();
	String WarnIfDifferent = __WarnIfDifferent_JComboBox.getSelected();
	String WarnIfSame = __WarnIfSame_JComboBox.getSelected();
	__error_wait = false;
	if ( InputFile1.length() > 0 ) {
		props.set ( "InputFile1", InputFile1 );
	}
	if ( InputFile2.length() > 0 ) {
		props.set ( "InputFile2", InputFile2 );
	}
	if ( WarnIfDifferent.length() > 0 ) {
		props.set ( "WarnIfDifferent", WarnIfDifferent );
	}
	if ( WarnIfSame.length() > 0 ) {
		props.set ( "WarnIfSame", WarnIfSame );
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
{	String InputFile1 = __InputFile1_JTextField.getText().trim();
	String InputFile2 = __InputFile2_JTextField.getText().trim();
	String WarnIfDifferent = __WarnIfDifferent_JComboBox.getSelected();
	String WarnIfSame = __WarnIfSame_JComboBox.getSelected();
	__command.setCommandParameter ( "InputFile1", InputFile1 );
	__command.setCommandParameter ( "InputFile2", InputFile2 );
	__command.setCommandParameter ( "WarnIfDifferent", WarnIfDifferent );
	__command.setCommandParameter ( "WarnIfSame", WarnIfSame );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__cancel_JButton = null;
	__command_JTextArea = null;
	__command = null;
	__InputFile1_JTextField = null;
	__InputFile2_JTextField = null;
	__WarnIfDifferent_JComboBox = null;
	__WarnIfSame_JComboBox = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Command command )
{	__command = (compareFiles_Command)command;
	CommandProcessor processor =__command.getCommandProcessor();
	
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command compares text files.  Comment lines starting "+
		"with # are ignored." ),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"A line by line comparison is made."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    if ( __working_dir != null ) {
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"It is recommended that file names be relative to the working directory, which is:"),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"    " + __working_dir),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"First file to compare:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputFile1_JTextField = new JTextField ( 50 );
	__InputFile1_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __InputFile1_JTextField,
		1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse1_JButton = new SimpleJButton ( "Browse", this );
    JGUIUtil.addComponent(main_JPanel, __browse1_JButton,
		6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Second file to compare:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputFile2_JTextField = new JTextField ( 50 );
	__InputFile2_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __InputFile2_JTextField,
		1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse2_JButton = new SimpleJButton ( "Browse", this );
        JGUIUtil.addComponent(main_JPanel, __browse2_JButton,
		6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Warn if different?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__WarnIfDifferent_JComboBox = new SimpleJComboBox ( false );
	__WarnIfDifferent_JComboBox.addItem ( "" );	// Default
	__WarnIfDifferent_JComboBox.addItem ( __command._False );
	__WarnIfDifferent_JComboBox.addItem ( __command._True );
	__WarnIfDifferent_JComboBox.select ( 0 );
	__WarnIfDifferent_JComboBox.addActionListener ( this );
        JGUIUtil.addComponent(main_JPanel, __WarnIfDifferent_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Generate a warning if different? (default=false)"), 
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Warn if same?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__WarnIfSame_JComboBox = new SimpleJComboBox ( false );
	__WarnIfSame_JComboBox.addItem ( "" );	// Default
	__WarnIfSame_JComboBox.addItem ( __command._False );
	__WarnIfSame_JComboBox.addItem ( __command._True );
	__WarnIfSame_JComboBox.select ( 0 );
	__WarnIfSame_JComboBox.addActionListener ( this );
        JGUIUtil.addComponent(main_JPanel, __WarnIfSame_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Generate a warning if same? (default=false)"), 
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

	if ( __working_dir != null ) {
		// Add the buttons to allow conversion to/from relative path...
		__path1_JButton = new SimpleJButton(
			__RemoveWorkingDirectoryFile1,this);
		button_JPanel.add ( __path1_JButton );
		__path2_JButton = new SimpleJButton(
			__RemoveWorkingDirectoryFile2,this);
		button_JPanel.add ( __path2_JButton );
	}
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
Refresh the command from the other text field contents.  The command is
of the form:
<pre>
compareFiles(InputFile1="X",InputFile2="X",WarnIfDifferent=X,WarnIfSame=X)
</pre>
*/
private void refresh ()
{	String routine = "compareFiles_JDialog.refresh";
	String InputFile1 = "";
	String InputFile2 = "";
	String WarnIfDifferent = "";
	String WarnIfSame = "";
	if ( __first_time ) {
		__first_time = false;
		Vector v = StringUtil.breakStringList (
			__command.toString(),"()",
			StringUtil.DELIM_SKIP_BLANKS );
		PropList props = null;
		if (	(v != null) && (v.size() > 1) &&
			(((String)v.elementAt(1)).indexOf("=") > 0) ) {
			props = PropList.parse (
				(String)v.elementAt(1), routine, "," );
		}
		if ( props == null ) {
			props = new PropList ( __command.getCommandName() );
		}
		InputFile1 = props.getValue ( "InputFile1" );
		InputFile2 = props.getValue ( "InputFile2" );
		WarnIfDifferent = props.getValue ( "WarnIfDifferent" );
		WarnIfSame = props.getValue ( "WarnIfSame" );
		if ( InputFile1 != null ) {
			__InputFile1_JTextField.setText ( InputFile1 );
		}
		if ( InputFile2 != null ) {
			__InputFile2_JTextField.setText ( InputFile2 );
		}
		if (	JGUIUtil.isSimpleJComboBoxItem(
			__WarnIfDifferent_JComboBox, WarnIfDifferent,
			JGUIUtil.NONE, null, null ) ) {
			__WarnIfDifferent_JComboBox.select ( WarnIfDifferent );
		}
		else {	if (	(WarnIfDifferent == null) ||
				WarnIfDifferent.equals("") ) {
				// New command...select the default...
				__WarnIfDifferent_JComboBox.select ( 0 );
			}
			else {	// Bad user command...
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"WarnIfDifferent parameter \"" +
				WarnIfDifferent +
				"\".  Select a\ndifferent value or Cancel." );
			}
		}
		if (	JGUIUtil.isSimpleJComboBoxItem(
			__WarnIfSame_JComboBox, WarnIfSame,
			JGUIUtil.NONE, null, null ) ) {
			__WarnIfSame_JComboBox.select ( WarnIfSame );
		}
		else {	if (	(WarnIfSame == null) ||
				WarnIfSame.equals("") ) {
				// New command...select the default...
				__WarnIfSame_JComboBox.select ( 0 );
			}
			else {	// Bad user command...
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"WarnIfSame parameter \"" +
				WarnIfSame +
				"\".  Select a\ndifferent value or Cancel." );
			}
		}
	}
	// Regardless, reset the command from the fields.  This is only  visible
	// information that has not been committed in the command.
	InputFile1 = __InputFile1_JTextField.getText().trim();
	InputFile2 = __InputFile2_JTextField.getText().trim();
	WarnIfDifferent = __WarnIfDifferent_JComboBox.getSelected();
	WarnIfSame = __WarnIfSame_JComboBox.getSelected();
	PropList props = new PropList ( __command.getCommandName() );
	props.add ( "InputFile1=" + InputFile1 );
	props.add ( "InputFile2=" + InputFile2 );
	props.add ( "WarnIfDifferent=" + WarnIfDifferent );
	props.add ( "WarnIfSame=" + WarnIfSame );
	__command_JTextArea.setText( __command.toString(props) );
	// Check the path and determine what the label on the path button should
	// be...
	if ( __path1_JButton != null ) {
		__path1_JButton.setEnabled ( true );
		File f = new File ( InputFile1 );
		if ( f.isAbsolute() ) {
			__path1_JButton.setText (__RemoveWorkingDirectoryFile1);
		}
		else {	__path1_JButton.setText (__AddWorkingDirectoryFile1 );
		}
	}
	if ( __path2_JButton != null ) {
		__path2_JButton.setEnabled ( true );
		File f = new File ( InputFile2 );
		if ( f.isAbsolute() ) {
			__path2_JButton.setText (__RemoveWorkingDirectoryFile2);
		}
		else {	__path2_JButton.setText (__AddWorkingDirectoryFile2 );
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

} // end compareFiles_JDialog
