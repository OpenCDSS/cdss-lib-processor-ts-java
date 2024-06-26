// SetWorkingDir_JDialog - Editor for SetWorkingDir() command.

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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

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

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Editor for SetWorkingDir() command.
*/
@SuppressWarnings("serial")
public class SetWorkingDir_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
	private final String __AddWorkingDirectory = "Abs";
	private final String __RemoveWorkingDirectory = "Rel";
	
private SimpleJButton __browse_JButton = null;
private SimpleJButton __path_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SetWorkingDir_Command __command = null;
private String __working_dir = null; // Working directory.
private JTextArea __command_JTextArea = null;
private SimpleJComboBox	__RunMode_JComboBox = null;
private SimpleJComboBox __RunOnOS_JComboBox = null;
private JTextField __WorkingDir_JTextField = null;
private boolean __error_wait = false; // Is there an error waiting to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Whether the user has pressed OK to close the dialog.

/**
Command editor dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public SetWorkingDir_JDialog (	JFrame parent, SetWorkingDir_Command command )
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
		// Browse for the file to read...
		JFileChooser fc = JFileChooserFactory.createJFileChooser(
			JGUIUtil.getLastFileDialogDirectory() );
		fc.setDialogTitle("Select the Working Directory" );
		fc.setFileSelectionMode ( JFileChooser.DIRECTORIES_ONLY );
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			String path = fc.getSelectedFile().getPath();

			if (path != null) {
				// Convert path to relative path by default.
				try {
					__WorkingDir_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1, __command.getCommandName() + "_JDialog", "Error converting file to relative path." );
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
		HelpViewer.getInstance().showHelp("command", "SetWorkingDir");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else if ( o == __path_JButton ) {
		if ( __path_JButton.getText().equals(__AddWorkingDirectory) ) {
			__WorkingDir_JTextField.setText ( IOUtil.toAbsolutePath(__working_dir, __WorkingDir_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
			    __WorkingDir_JTextField.setText ( IOUtil.toRelativePath ( __working_dir, __WorkingDir_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, __command.getCommandName() + "_JDialog", "Error converting file to relative path." );
			}
		}
		refresh ();
	}
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{   // Put together a list of parameters to check...
    PropList props = new PropList ( "" );
    String WorkingDir = __WorkingDir_JTextField.getText().trim();
    String RunMode = __RunMode_JComboBox.getSelected();
    String RunOnOS = __RunOnOS_JComboBox.getSelected();
    __error_wait = false;
    if ( WorkingDir.length() > 0 ) {
        props.set ( "WorkingDir", WorkingDir );
    }
    if ( RunMode.length() > 0 ) {
        props.set ( "RunMode", RunMode );
    }
    if ( RunOnOS.length() > 0 ) {
        props.set ( "RunOnOS", RunOnOS );
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
{   String WorkingDir = __WorkingDir_JTextField.getText().trim();
    String RunMode = __RunMode_JComboBox.getSelected();
    String RunOnOS = __RunOnOS_JComboBox.getSelected();
    __command.setCommandParameter ( "WorkingDir", WorkingDir );
    __command.setCommandParameter ( "RunMode", RunMode );
    __command.setCommandParameter ( "RunOnOS", RunOnOS );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, SetWorkingDir_Command command )
{	__command = command;
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( __command.getCommandProcessor(), __command );

	addWindowListener( this );

   Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "<html><b>Use of this command is discouraged - commands may generate " +
        "warnings after editing, but will run correctly.</b></html>" ),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The working directory is automatically set to the location of the command file "
        + "and generally all paths should be relative to the command file folder." ),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command (re)sets the working directory, which will be prepended to relative paths in commands."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"If browsing for files while editing other commands, paths typically default to be relative to the working directory." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The working directory is currently: " + __working_dir ), 
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Working directory:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__WorkingDir_JTextField = new JTextField ( 55 );
	__WorkingDir_JTextField.setToolTipText("Specify the absolute path to the working directory, can use ${Property}");
	__WorkingDir_JTextField.addKeyListener ( this );
    // Working directory layout fights back with other rows so put in its own panel
	JPanel WorkingDir_JPanel = new JPanel();
	WorkingDir_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(WorkingDir_JPanel, __WorkingDir_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ( "...", this );
	__browse_JButton.setToolTipText("Browse for folder");
    JGUIUtil.addComponent(WorkingDir_JPanel, __browse_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__path_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(WorkingDir_JPanel, __path_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(main_JPanel, WorkingDir_JPanel,
		1, y, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Run mode:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__RunMode_JComboBox = new SimpleJComboBox ();
	__RunMode_JComboBox.add ( "" );
	__RunMode_JComboBox.add ( __command._BatchOnly );
	__RunMode_JComboBox.add ( __command._GUIAndBatch );
	__RunMode_JComboBox.add ( __command._GUIOnly );
	__RunMode_JComboBox.select ( 0 );
	__RunMode_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __RunMode_JComboBox,
		1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - does command run in GUI and/or batch? (default=" + __command._GUIAndBatch + ")"), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Run on operating system:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __RunOnOS_JComboBox = new SimpleJComboBox ();
    __RunOnOS_JComboBox.add ( "" );
    __RunOnOS_JComboBox.add ( __command._All );
    __RunOnOS_JComboBox.add ( __command._UNIX );
    __RunOnOS_JComboBox.add ( __command._Windows );
    __RunOnOS_JComboBox.select ( 0 );
    __RunOnOS_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __RunOnOS_JComboBox,
        1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - operating system to run on (default=" + __command._All + ")"), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea (4,60);
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
        1, y, 8, 1, 1.0, 1.0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST );

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

	if ( (code == KeyEvent.VK_ENTER) || (code == KeyEvent.VK_TAB) ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( false );
		}
	}
	else {	refresh();
	}
}

public void keyReleased ( KeyEvent event )
{	
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
Refresh the command from the other text field contents
*/
private void refresh ()
{	String routine = __command.getCommandName() + "_JDialog.refresh";
    String WorkingDir = "";
	String RunMode = "";
	String RunOnOS = "";
    PropList props = null;
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        props = __command.getCommandParameters();
        WorkingDir = props.getValue ( "WorkingDir" );
        RunMode = props.getValue ( "RunMode" );
        RunOnOS = props.getValue ( "RunOnOS" );
        if ( WorkingDir != null ) {
            __WorkingDir_JTextField.setText( WorkingDir );
        }
        // Now select the item in the list.  If not a match, print a warning.
        if ( (RunMode == null) || (RunMode.length() == 0) ) {
            // Select default...
            __RunMode_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem(__RunMode_JComboBox,
                RunMode, JGUIUtil.NONE, null, null ) ) {
                __RunMode_JComboBox.select ( RunMode );
            }
            else {
                Message.printWarning ( 1,
                routine, "Existing "+
                "command references an invalid\n"+
                "run mode flag \"" + RunMode +
                "\".  Select a\ndifferent value or Cancel." );
            }
        }
        if ( (RunOnOS == null) || (RunOnOS.length() == 0) ) {
            // Select default...
            __RunOnOS_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem(__RunOnOS_JComboBox,
                RunOnOS, JGUIUtil.NONE, null, null ) ) {
                __RunOnOS_JComboBox.select ( RunMode );
            }
            else {
                Message.printWarning ( 1,
                routine, "Existing "+
                "command references an invalid\n"+
                "RunOnOS \"" + RunOnOS +
                "\".  Select a\ndifferent value or Cancel." );
            }
        }
	}
    // Regardless, reset the command from the fields...
    WorkingDir = __WorkingDir_JTextField.getText().trim();
    RunMode = __RunMode_JComboBox.getSelected();
    RunOnOS = __RunOnOS_JComboBox.getSelected();
    props = new PropList ( __command.getCommandName() );
    props.add ( "WorkingDir=" + WorkingDir );
    props.add ( "RunMode=" + RunMode );
    props.add ( "RunOnOS=" + RunOnOS );
    __command_JTextArea.setText(__command.toString(props).trim() );
	// Check the path and determine what the label on the path button should be...
	if ( __path_JButton != null ) {
		if ( (WorkingDir != null) && !WorkingDir.isEmpty() ) {
			__path_JButton.setEnabled ( true );
			File f = new File ( WorkingDir );
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

}
