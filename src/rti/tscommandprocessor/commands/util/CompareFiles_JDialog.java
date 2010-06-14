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
import javax.swing.JTextArea;
import javax.swing.JTextField;

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

public class CompareFiles_JDialog extends JDialog
implements ActionListener, KeyListener, WindowListener
{
private final String __AddWorkingDirectoryFile1 = "Add Working Directory (File 1)";
private final String __AddWorkingDirectoryFile2 = "Add Working Directory (File 2)";
private final String __RemoveWorkingDirectoryFile1 = "Remove Working Directory (File 1)";
private final String __RemoveWorkingDirectoryFile2 = "Remove Working Directory (File 2)";

private SimpleJButton __browse1_JButton = null;
private SimpleJButton __browse2_JButton = null;
private SimpleJButton __path1_JButton = null;
private SimpleJButton __path2_JButton = null;
private SimpleJButton __cancel_JButton = null; // Cancel Button
private SimpleJButton __ok_JButton = null; // Ok Button
private JTextField __InputFile1_JTextField = null; // First file
private JTextField __InputFile2_JTextField = null; // Second file
private JTextField __CommentLineChar_JTextField = null;
private JTextField __AllowedDiff_JTextField = null;
private SimpleJComboBox __IfDifferent_JComboBox =null;
private SimpleJComboBox __IfSame_JComboBox =null;
private JTextArea __command_JTextArea = null; // Command as JTextField
private String __working_dir = null; // Working directory.
private boolean __error_wait = false;
private boolean __first_time = true;
private CompareFiles_Command __command = null; // Command to edit
private boolean __ok = false; // Indicates whether the user pressed OK to close the dialog.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public CompareFiles_JDialog ( JFrame parent, Command command )
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
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
		}
		else {
		    fc = JFileChooserFactory.createJFileChooser(__working_dir );
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
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
		}
		else {
		    fc = JFileChooserFactory.createJFileChooser(__working_dir );
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
		if ( __path1_JButton.getText().equals(__AddWorkingDirectoryFile1) ) {
			__InputFile1_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir,__InputFile1_JTextField.getText() ) );
		}
		else if ( __path1_JButton.getText().equals(__RemoveWorkingDirectoryFile1) ) {
			try {
			    __InputFile1_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir, __InputFile1_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,"compareFiles_JDialog",
				"Error converting first file name to relative path." );
			}
		}
		refresh ();
	}
	else if ( o == __path2_JButton ) {
		if ( __path2_JButton.getText().equals( __AddWorkingDirectoryFile2) ) {
			__InputFile2_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir, __InputFile2_JTextField.getText() ) );
		}
		else if ( __path2_JButton.getText().equals(__RemoveWorkingDirectoryFile2) ) {
			try {
			    __InputFile2_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir, __InputFile2_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,"CompareFiles_JDialog",
				"Error converting first file name to relative path." );
			}
		}
		refresh ();
	}
	else {
	    // Choices...
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
	String CommentLineChar = __CommentLineChar_JTextField.getText().trim();
	String AllowedDiff = __AllowedDiff_JTextField.getText().trim();
	String IfDifferent = __IfDifferent_JComboBox.getSelected();
	String IfSame = __IfSame_JComboBox.getSelected();
	__error_wait = false;
	if ( InputFile1.length() > 0 ) {
		props.set ( "InputFile1", InputFile1 );
	}
	if ( InputFile2.length() > 0 ) {
		props.set ( "InputFile2", InputFile2 );
	}
    if ( CommentLineChar.length() > 0 ) {
        props.set ( "CommentLineChar", CommentLineChar );
    }
    if ( AllowedDiff.length() > 0 ) {
        props.set ( "AllowedDiff", AllowedDiff );
    }
	if ( IfDifferent.length() > 0 ) {
		props.set ( "IfDifferent", IfDifferent );
	}
	if ( IfSame.length() > 0 ) {
		props.set ( "IfSame", IfSame );
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
	String CommentLineChar = __CommentLineChar_JTextField.getText().trim();
	String AllowedDiff = __AllowedDiff_JTextField.getText().trim();
	String IfDifferent = __IfDifferent_JComboBox.getSelected();
	String IfSame = __IfSame_JComboBox.getSelected();
	__command.setCommandParameter ( "InputFile1", InputFile1 );
	__command.setCommandParameter ( "InputFile2", InputFile2 );
	__command.setCommandParameter ( "CommentLineChar", CommentLineChar );
	__command.setCommandParameter ( "AllowedDiff", AllowedDiff );
	__command.setCommandParameter ( "IfDifferent", IfDifferent );
	__command.setCommandParameter ( "IfSame", IfSame );
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
	__IfDifferent_JComboBox = null;
	__IfSame_JComboBox = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Command command )
{	__command = (CompareFiles_Command)command;
	CommandProcessor processor =__command.getCommandProcessor();
	
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command compares text files.  Comment lines starting with # are ignored." ),
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

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "First file to compare:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputFile1_JTextField = new JTextField ( 50 );
	__InputFile1_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __InputFile1_JTextField,
		1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse1_JButton = new SimpleJButton ( "Browse", this );
    JGUIUtil.addComponent(main_JPanel, __browse1_JButton,
		6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Second file to compare:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputFile2_JTextField = new JTextField ( 50 );
	__InputFile2_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __InputFile2_JTextField,
		1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse2_JButton = new SimpleJButton ( "Browse", this );
        JGUIUtil.addComponent(main_JPanel, __browse2_JButton,
		6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Comment line character:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CommentLineChar_JTextField = new JTextField ( 20 );
    __CommentLineChar_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __CommentLineChar_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Optional - must be first char on line (default=#)"), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Allowed # of different lines:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AllowedDiff_JTextField = new JTextField ( 5 );
    __AllowedDiff_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __AllowedDiff_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Optional - when checking for differences (default=0)"), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Action if different:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IfDifferent_JComboBox = new SimpleJComboBox ( false );
	__IfDifferent_JComboBox.addItem ( "" );	// Default
	__IfDifferent_JComboBox.addItem ( __command._Ignore );
	__IfDifferent_JComboBox.addItem ( __command._Warn );
	__IfDifferent_JComboBox.addItem ( __command._Fail );
	__IfDifferent_JComboBox.select ( 0 );
	__IfDifferent_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __IfDifferent_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - action if files are different (default=" + __command._Ignore + ")"), 
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Action if same:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IfSame_JComboBox = new SimpleJComboBox ( false );
	__IfSame_JComboBox.addItem ( "" );	// Default
	__IfSame_JComboBox.addItem ( __command._Ignore );
	__IfSame_JComboBox.addItem ( __command._Warn );
	__IfSame_JComboBox.addItem ( __command._Fail );
	__IfSame_JComboBox.select ( 0 );
	__IfSame_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __IfSame_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - action if files are the same (default=" + __command._Ignore + ")"), 
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
		__path1_JButton = new SimpleJButton(__RemoveWorkingDirectoryFile1,this);
		button_JPanel.add ( __path1_JButton );
		__path2_JButton = new SimpleJButton(__RemoveWorkingDirectoryFile2,this);
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
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = "CompareFiles_JDialog.refresh";
	String InputFile1 = "";
	String InputFile2 = "";
	String CommentLineChar = "";
	String AllowedDiff = "";
	String IfDifferent = "";
	String IfSame = "";
    PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
		parameters = __command.getCommandParameters();
		InputFile1 = parameters.getValue ( "InputFile1" );
		InputFile2 = parameters.getValue ( "InputFile2" );
		CommentLineChar = parameters.getValue ( "CommentLineChar" );
		AllowedDiff = parameters.getValue ( "AllowedDiff" );
		IfDifferent = parameters.getValue ( "IfDifferent" );
		IfSame = parameters.getValue ( "IfSame" );
		if ( InputFile1 != null ) {
			__InputFile1_JTextField.setText ( InputFile1 );
		}
		if ( InputFile2 != null ) {
			__InputFile2_JTextField.setText ( InputFile2 );
		}
        if ( CommentLineChar != null ) {
            __CommentLineChar_JTextField.setText ( CommentLineChar );
        }
        if ( AllowedDiff != null ) {
            __AllowedDiff_JTextField.setText ( AllowedDiff );
        }
		if ( JGUIUtil.isSimpleJComboBoxItem(__IfDifferent_JComboBox, IfDifferent, JGUIUtil.NONE, null, null ) ) {
			__IfDifferent_JComboBox.select ( IfDifferent );
		}
		else {
		    if ( (IfDifferent == null) || IfDifferent.equals("") ) {
				// New command...select the default...
				__IfDifferent_JComboBox.select ( 0 );
			}
			else {
			    // Bad user command...
				Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
				"fDifferent parameter \"" + IfDifferent + "\".  Select a\ndifferent value or Cancel." );
			}
		}
		if (	JGUIUtil.isSimpleJComboBoxItem(
			__IfSame_JComboBox, IfSame,
			JGUIUtil.NONE, null, null ) ) {
			__IfSame_JComboBox.select ( IfSame );
		}
		else {
			if ( (IfSame == null) || IfSame.equals("") ) {
				// New command...select the default...
				__IfSame_JComboBox.select ( 0 );
			}
			else {
				// Bad user command...
				Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
				"IfSame parameter \"" + IfSame + "\".  Select a\ndifferent value or Cancel." );
			}
		}
	}
	// Regardless, reset the command from the fields.  This is only  visible
	// information that has not been committed in the command.
	InputFile1 = __InputFile1_JTextField.getText().trim();
	InputFile2 = __InputFile2_JTextField.getText().trim();
	CommentLineChar = __CommentLineChar_JTextField.getText().trim();
	AllowedDiff = __AllowedDiff_JTextField.getText().trim();
	IfDifferent = __IfDifferent_JComboBox.getSelected();
	IfSame = __IfSame_JComboBox.getSelected();
	PropList props = new PropList ( __command.getCommandName() );
	props.add ( "InputFile1=" + InputFile1 );
	props.add ( "InputFile2=" + InputFile2 );
	props.add ( "CommentLineChar=" + CommentLineChar );
	props.add ( "AllowedDiff=" + AllowedDiff );
	props.add ( "IfDifferent=" + IfDifferent );
	props.add ( "IfSame=" + IfSame );
	__command_JTextArea.setText( __command.toString(props) );
	// Check the path and determine what the label on the path button should be...
	if ( __path1_JButton != null ) {
		__path1_JButton.setEnabled ( true );
		File f = new File ( InputFile1 );
		if ( f.isAbsolute() ) {
			__path1_JButton.setText (__RemoveWorkingDirectoryFile1);
		}
		else {
		    __path1_JButton.setText (__AddWorkingDirectoryFile1 );
		}
	}
	if ( __path2_JButton != null ) {
		__path2_JButton.setEnabled ( true );
		File f = new File ( InputFile2 );
		if ( f.isAbsolute() ) {
			__path2_JButton.setText (__RemoveWorkingDirectoryFile2);
		}
		else {
		    __path2_JButton.setText (__AddWorkingDirectoryFile2 );
		}
	}
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
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