// CreateFolder_JDialog - editor for CreateFolder command

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2020 Colorado Department of Natural Resources

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
import java.util.ArrayList;
import java.util.List;

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

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

@SuppressWarnings("serial")
public class CreateFolder_JDialog extends JDialog
implements ActionListener, KeyListener, WindowListener
{
private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __browseFolder_JButton = null;
private SimpleJButton __pathFolder_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private JTextField __Folder_JTextField = null;
private SimpleJComboBox __CreateParentFolders_JComboBox =null;
private SimpleJComboBox __IfFolderExists_JComboBox =null;
private JTextArea __command_JTextArea = null;
private String __working_dir = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private CreateFolder_Command __command = null;
private boolean __ok = false; // Whether the user has pressed OK to close the dialog.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public CreateFolder_JDialog ( JFrame parent, CreateFolder_Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();

	if ( o == __browseFolder_JButton ) {
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
		}
		else {
		    fc = JFileChooserFactory.createJFileChooser(__working_dir );
		}
		fc.setDialogTitle( "Select a folder to create");
		fc.setFileSelectionMode (JFileChooser.DIRECTORIES_ONLY );
		
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			String folder = fc.getSelectedFile().getName(); 
			String path = fc.getSelectedFile().getPath(); 
	
			if (folder == null || folder.equals("")) {
				return;
			}
	
			if (path != null) {
				// Convert path to relative path by default.
				try {
					__Folder_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1,"CreateFolder_JDialog", "Error converting folder to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory(path);
				refresh();
			}
		}
	}
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "CreateFolder");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else if ( o == __pathFolder_JButton ) {
		if ( __pathFolder_JButton.getText().equals(__AddWorkingDirectory) ) {
			__Folder_JTextField.setText (IOUtil.toAbsolutePath(__working_dir,__Folder_JTextField.getText() ) );
		}
		else if ( __pathFolder_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
                __Folder_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,
                        __Folder_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,"CopyFile_JDialog",
				"Error converting input file name to relative path." );
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
	String Folder = __Folder_JTextField.getText().trim();
	String CreateParentFolders = __CreateParentFolders_JComboBox.getSelected();
	String IfFolderExists = __IfFolderExists_JComboBox.getSelected();
	__error_wait = false;
	if ( Folder.length() > 0 ) {
		props.set ( "Folder", Folder );
	}
	if ( CreateParentFolders.length() > 0 ) {
		props.set ( "CreateParentFolders", CreateParentFolders );
	}
	if ( IfFolderExists.length() > 0 ) {
		props.set ( "IfFolderExists", IfFolderExists );
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
{	String Folder = __Folder_JTextField.getText().trim();
	String IfFolderExists = __IfFolderExists_JComboBox.getSelected();
	String CreateParentFolders = __CreateParentFolders_JComboBox.getSelected();
	__command.setCommandParameter ( "Folder", Folder );
	__command.setCommandParameter ( "CreateParentFolders", CreateParentFolders );
	__command.setCommandParameter ( "IfFolderExists", IfFolderExists );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, CreateFolder_Command command )
{	__command = command;
	CommandProcessor processor =__command.getCommandProcessor();
	
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Create a new folder." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Folder names can use the notation ${Property} to use processor properties." ),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    if ( __working_dir != null ) {
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"It is recommended that the folder name is relative to the working directory, which is:"),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	JGUIUtil.addComponent(main_JPanel, new JLabel ("    " + __working_dir),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Folder:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Folder_JTextField = new JTextField ( 50 );
	__Folder_JTextField.setToolTipText("Specify the folder to create, can use ${Property} notation.");
	__Folder_JTextField.addKeyListener ( this );
    // Folder layout fights back with other rows so put in its own panel
	JPanel folder_JPanel = new JPanel();
	folder_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(folder_JPanel, __Folder_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseFolder_JButton = new SimpleJButton ( "...", this );
	__browseFolder_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(folder_JPanel, __browseFolder_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__pathFolder_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(folder_JPanel, __pathFolder_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(main_JPanel, folder_JPanel,
		1, y, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

   JGUIUtil.addComponent(main_JPanel, new JLabel ( "Create parent folders?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__CreateParentFolders_JComboBox = new SimpleJComboBox ( false );
	List<String> parentChoices = new ArrayList<String>();
	parentChoices.add ( "" );	// Default
	parentChoices.add ( __command._False );
	parentChoices.add ( __command._True );
	__CreateParentFolders_JComboBox.setData(parentChoices);
	__CreateParentFolders_JComboBox.select ( 0 );
	__CreateParentFolders_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(main_JPanel, __CreateParentFolders_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - create parent folders if necessary (default=" + __command._False + ")."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
   JGUIUtil.addComponent(main_JPanel, new JLabel ( "If folder exists?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IfFolderExists_JComboBox = new SimpleJComboBox ( false );
	List<String> notFoundChoices = new ArrayList<String>();
	notFoundChoices.add ( "" );	// Default
	notFoundChoices.add ( __command._Ignore );
	notFoundChoices.add ( __command._Warn );
	notFoundChoices.add ( __command._Fail );
	__IfFolderExists_JComboBox.setData(notFoundChoices);
	__IfFolderExists_JComboBox.select ( 0 );
	__IfFolderExists_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(main_JPanel, __IfFolderExists_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - action if the folder exists (default=" + __command._Warn + ")."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

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
{	String routine = getClass().getSimpleName() + ".refresh";
	String Folder = "";
	String CreateParentFolders = "";
	String IfFolderExists = "";
    PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
        parameters = __command.getCommandParameters();
		Folder = parameters.getValue ( "Folder" );
		CreateParentFolders = parameters.getValue ( "CreateParentFolders" );
		IfFolderExists = parameters.getValue ( "IfFolderExists" );
		if ( Folder != null ) {
			__Folder_JTextField.setText ( Folder );
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__CreateParentFolders_JComboBox, CreateParentFolders,JGUIUtil.NONE, null, null ) ) {
			__CreateParentFolders_JComboBox.select ( CreateParentFolders );
		}
		else {
            if ( (CreateParentFolders == null) ||	CreateParentFolders.equals("") ) {
				// New command...select the default...
				__CreateParentFolders_JComboBox.select ( 0 );
			}
			else {	// Bad user command...
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"CreateParentFolders parameter \"" +	CreateParentFolders +
				"\".  Select a\n value or Cancel." );
			}
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__IfFolderExists_JComboBox, IfFolderExists,JGUIUtil.NONE, null, null ) ) {
			__IfFolderExists_JComboBox.select ( IfFolderExists );
		}
		else {
            if ( (IfFolderExists == null) ||	IfFolderExists.equals("") ) {
				// New command...select the default...
				__IfFolderExists_JComboBox.select ( 0 );
			}
			else {	// Bad user command...
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"IfFolderExists parameter \"" +	IfFolderExists +
				"\".  Select a\n value or Cancel." );
			}
		}
	}
	// Regardless, reset the command from the fields.  This is only  visible
	// information that has not been committed in the command.
	Folder = __Folder_JTextField.getText().trim();
	CreateParentFolders = __CreateParentFolders_JComboBox.getSelected();
	IfFolderExists = __IfFolderExists_JComboBox.getSelected();
	PropList props = new PropList ( __command.getCommandName() );
	props.add ( "Folder=" + Folder );
	props.add ( "CreateParentFolders=" + CreateParentFolders );
	props.add ( "IfFolderExists=" + IfFolderExists );
	__command_JTextArea.setText( __command.toString(props) );
	// Check the path and determine what the label on the path button should be...
	if ( __pathFolder_JButton != null ) {
		if ( (Folder != null) && !Folder.isEmpty() ) {
			__pathFolder_JButton.setEnabled ( true );
			File f = new File ( Folder );
			if ( f.isAbsolute() ) {
				__pathFolder_JButton.setText ( __RemoveWorkingDirectory );
				__pathFolder_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
		    	__pathFolder_JButton.setText ( __AddWorkingDirectory );
		    	__pathFolder_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathFolder_JButton.setEnabled(false);
		}
	}
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed
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
