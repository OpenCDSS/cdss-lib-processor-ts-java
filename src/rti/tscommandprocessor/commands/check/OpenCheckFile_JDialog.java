package rti.tscommandprocessor.commands.check;

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
import javax.swing.JTextArea;
import javax.swing.JTextField;

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
 * Editor for OpenCheckFile() command.
 * @author sam
 *
 */
public class OpenCheckFile_JDialog extends JDialog
implements ActionListener, KeyListener, WindowListener
{
private final String __AddWorkingDirectory = "Add Working Directory";
private final String __RemoveWorkingDirectory = "Remove Working Directory";

private SimpleJButton	__cancel_JButton = null,	// Cancel Button
			__browse_JButton = null,	// Browse Button
			__ok_JButton = null,		// Ok Button
			__path_JButton = null;		// Button to add/remove
							// path
private JTextField	__CheckFile_JTextField = null;	// Field for CheckFile
private SimpleJComboBox	__Suffix_JComboBox = null;	// Choice for file suffix
private JTextArea	__command_JTextArea = null;	// Command as JTextField
private boolean		__error_wait = false;
private boolean		__first_time = true;
private OpenCheckFile_Command __command = null;	// Command to edit
private boolean		__ok = false;		// Indicates whether user pressed OK to close the dialog.
private String		__working_dir = null;	// The working directory.

/**
Command editor dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public OpenCheckFile_JDialog ( JFrame parent, Command command )
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
			fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
		}
		else {
		    fc = JFileChooserFactory.createJFileChooser( __working_dir );
		}
		fc.setDialogTitle("Select Log File");
		SimpleFileFilter sff = new SimpleFileFilter("log", "Log File");
		fc.addChoosableFileFilter(sff);
		
		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			String directory = fc.getSelectedFile().getParent();
			String filename = fc.getSelectedFile().getName(); 
			String path = fc.getSelectedFile().getPath(); 
	
			if (filename == null || filename.equals("")) {
				return;
			}
	
			if (path != null) {
				__CheckFile_JTextField.setText(path );
				JGUIUtil.setLastFileDialogDirectory(directory );
				refresh();
			}
		}
	}
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if (o == __ok_JButton) {
		// Enforce the ".html" extension if a filename has been entered
		String CheckFile = __CheckFile_JTextField.getText().trim();
		if (!CheckFile.equals("")) {
			CheckFile = IOUtil.enforceFileExtension(CheckFile, "html");
			__CheckFile_JTextField.setText(CheckFile);
		}
		//try {
		refresh();
		checkInput();
		if (!__error_wait) {
			response(true);
		}
		//}
		//catch ( Exception e ) {
		//    Message.printWarning(2, "", e);
		//}
	}
	else if ( o == __path_JButton ) {
		if ( __path_JButton.getText().equals( __AddWorkingDirectory) ) {
			__CheckFile_JTextField.setText (
			        IOUtil.toAbsolutePath(__working_dir, __CheckFile_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals( __RemoveWorkingDirectory) ) {
			try {
			    __CheckFile_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir, __CheckFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,
				"OpenCheckFile_JDialog", "Error converting file to relative path." );
			}
		}
		refresh ();
	}
	else {
	    // Combo box...
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
	String CheckFile = __CheckFile_JTextField.getText().trim();
	String Suffix = __Suffix_JComboBox.getSelected();
	if ( CheckFile.length() > 0 ) {
		props.set ( "CheckFile", CheckFile );
	}
	if ( Suffix.length() > 0 ) {
		props.set ( "Suffix", Suffix );
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
{	String CheckFile = __CheckFile_JTextField.getText().trim();
	String Suffix = __Suffix_JComboBox.getSelected();
	__command.setCommandParameter ( "CheckFile", CheckFile );
	__command.setCommandParameter ( "Suffix", Suffix );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__cancel_JButton = null;
	__browse_JButton = null;
	__path_JButton = null;
	__command_JTextArea = null;
	__CheckFile_JTextField = null;
	__Suffix_JComboBox = null;
	__ok_JButton = null;
	super.finalize ();
}
/**
Get the working directory for a command (e.g., for editing).
@param processor the TSCommandProcessor to use to get data.
@param command Command for which to get the working directory.
@return The working directory in effect for a command.
*/
private String getWorkingDirForCommand ( CommandProcessor processor, Command command )
{	String routine = getClass().getName() + ".getWorkingDirForCommand";
	PropList request_params = new PropList ( "" );
	request_params.setUsingObject ( "Command", command );
	CommandProcessorRequestResultsBean bean = null;
	try { bean =
		processor.processRequest( "GetWorkingDirForCommand", request_params );
		return bean.getResultsPropList().getValue("WorkingDir");
	}
	catch ( Exception e ) {
		String message = "Error requesting GetWorkingDirForCommand(Command=\"" + command +
		"\" from processor).";
		Message.printWarning(3, routine, e);
		Message.printWarning(3, routine, message );
	}
	return null;
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Command command )
{	__command = (OpenCheckFile_Command)command;
	__working_dir = null;
	// Because this command is shared by StateDMI_Processor, do it the generic way, NOT as commented -
	// basically paste in the code from the method indicated below.
	//__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)__command.getCommandProcessor(), __command );
	__working_dir = getWorkingDirForCommand ( __command.getCommandProcessor(), __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Open the HTML check file to receive a summary of processing problems and results." ),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The check file can be specified using a full or " +
		"relative path (relative to the working directory)."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
       JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The working directory is: " + __working_dir ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The Browse button can be used to select an existing file to overwrite."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specifying a suffix for the file will insert the suffix " +
		"before the \"html\" file extension."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Check file:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__CheckFile_JTextField = new JTextField ( 50 );
	__CheckFile_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __CheckFile_JTextField,
		1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ( "Browse", this );
    JGUIUtil.addComponent(main_JPanel, __browse_JButton,
		6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Suffix:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Suffix_JComboBox = new SimpleJComboBox ( false );
	__Suffix_JComboBox.addItem ( "" );	// Default
	__Suffix_JComboBox.addItem ( __command._Date );
	__Suffix_JComboBox.addItem ( __command._DateTime );
	__Suffix_JComboBox.select ( 0 );
	__Suffix_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Suffix_JComboBox,
	    1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Suffix for log file (blank=none)."), 
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

   JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 60 );
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		button_JPanel.add ( __path_JButton = new SimpleJButton(
			__RemoveWorkingDirectory, __RemoveWorkingDirectory, this) );
	}
	button_JPanel.add(__cancel_JButton = new SimpleJButton("Cancel", this));
	button_JPanel.add ( __ok_JButton = new SimpleJButton("OK", this) );

	setTitle ( "Edit " + __command.getCommandName() + "() command" );
	
	// Refresh the contents...
    refresh ();

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
startLog(CheckFile="X",Suffix="X")
</pre>
*/
private void refresh ()
{	String routine = "startLog_JDialog.refresh";
	String CheckFile = "", Suffix = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		Suffix = props.getValue ( "Suffix" );
		CheckFile = props.getValue ( "CheckFile" );
		if ( CheckFile != null ) {
			__CheckFile_JTextField.setText ( CheckFile );
		}
		if (	JGUIUtil.isSimpleJComboBoxItem(
			__Suffix_JComboBox, Suffix, JGUIUtil.NONE, null, null)){
			__Suffix_JComboBox.select ( Suffix );
		}
		else {	if ( (Suffix == null) || Suffix.equals("") ) {
				// New command...select the default...
				__Suffix_JComboBox.select ( 0 );
			}
			else {	// Bad user command...
				Message.printWarning ( 1, routine, "Existing " +
				"command references an invalid\n"+
				"suffix \"" + Suffix +
				"\".  Select a\ndifferent value or Cancel." );
			}
		}
	}
	// Regardless, reset the command from the fields.  This is only visible
	// information that has not been committed in the command.
	CheckFile = __CheckFile_JTextField.getText().trim();
	Suffix = __Suffix_JComboBox.getSelected();
	props = new PropList ( __command.getCommandName() );
	props.add ( "CheckFile=" + CheckFile );
	props.add ( "Suffix=" + Suffix );
	__command_JTextArea.setText( __command.toString(props) );
	// Check the path and determine what the label on the path button should be...
	if ( (CheckFile == null) || (CheckFile.length() == 0) ) {
		if ( __path_JButton != null ) {
			__path_JButton.setEnabled ( false );
		}
	}
	if ( __path_JButton != null ) {
		__path_JButton.setEnabled ( true );
		File f = new File ( CheckFile );
		if ( f.isAbsolute() ) {
			__path_JButton.setText ( __RemoveWorkingDirectory );
		}
		else {
		    __path_JButton.setText ( __AddWorkingDirectory );
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
