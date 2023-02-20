// RunDSSUTL_JDialog - Editor for the RunDSSUTL() command.

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

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFileChooser;
import javax.swing.JDialog;
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

import java.io.File;

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Editor for the RunDSSUTL() command.
*/
@SuppressWarnings("serial")
public class RunDSSUTL_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
    
private final String __AddWorkingDirectoryDssFile = "Abs";
private final String __RemoveWorkingDirectoryDssFile = "Rel";

private final String __AddWorkingDirectoryInputFile = "Abs";
private final String __RemoveWorkingDirectoryInputFile = "Rel";

private final String __AddWorkingDirectoryOutputFile = "Abs";
private final String __RemoveWorkingDirectoryOutputFile = "Rel";

private final String __AddWorkingDirectoryDssutlProgram = "Abs";
private final String __RemoveWorkingDirectoryDssutlProgram = "Rel";

private SimpleJButton __browseDssFile_JButton = null;
private SimpleJButton __browseInputFile_JButton = null;
private SimpleJButton __browseOutputFile_JButton = null;
private SimpleJButton __browseDssutlProgram_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SimpleJButton __pathDssFile_JButton = null;
private SimpleJButton __pathInputFile_JButton = null;
private SimpleJButton __pathOutputFile_JButton = null;
private SimpleJButton __pathDssutlProgram_JButton = null;
private RunDSSUTL_Command __command = null;	// Command to edit
private String __working_dir = null;	// Working directory.
private JTextField __DssFile_JTextField = null;
private JTextField __InputFile_JTextField = null;
private JTextField __OutputFile_JTextField = null;
private JTextField __DssutlProgram_JTextField = null;
//private JTextArea __Arguments_JTextArea=null;
private JTextArea __command_JTextArea=null;// Command as TextArea
private boolean __error_wait = false;	// Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK was pressed when closing the dialog.

/**
Command editor constructor.
@param parent Frame class instantiating this class.
@param command Command to edit.
*/
public RunDSSUTL_JDialog ( JFrame parent, RunDSSUTL_Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();
    //String routine = getClass().getName() + ".actionPerformed";

	if ( o == __browseDssFile_JButton ) {
	    actionPerformedBrowse ( __DssFile_JTextField, "Select HEC-DSS File", "dss", "HEC-DSS File" );
	}
	else if ( o == __browseInputFile_JButton ) {
        actionPerformedBrowse ( __InputFile_JTextField, "Select DSSUTL Input File", "mco", "DSSUTL Input File" );
    }
    else if ( o == __browseOutputFile_JButton ) {
        actionPerformedBrowse ( __OutputFile_JTextField, "Select DSSUTL Output File", "txt", "DSSUTL Output File" );
    }
    else if ( o == __browseDssutlProgram_JButton ) {
        actionPerformedBrowse ( __DssutlProgram_JTextField, "Select DSSUTL Program", "exe",
            "DSSUTL Executable Program File" );
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
	else if ( o == __pathDssFile_JButton ) {
		actionPerformedPath ( __pathDssFile_JButton, __DssFile_JTextField,
		    __AddWorkingDirectoryDssFile, __RemoveWorkingDirectoryDssFile );
	}
    else if ( o == __pathInputFile_JButton ) {
        actionPerformedPath ( __pathInputFile_JButton, __InputFile_JTextField,
            __AddWorkingDirectoryInputFile, __RemoveWorkingDirectoryInputFile );
    }
    else if ( o == __pathOutputFile_JButton ) {
        actionPerformedPath ( __pathOutputFile_JButton, __OutputFile_JTextField,
            __AddWorkingDirectoryOutputFile, __RemoveWorkingDirectoryOutputFile );
    }
    else if ( o == __pathDssutlProgram_JButton ) {
        actionPerformedPath ( __pathDssutlProgram_JButton, __DssutlProgram_JTextField,
            __AddWorkingDirectoryDssutlProgram, __RemoveWorkingDirectoryDssutlProgram );
    }
}

/**
Handle the actionPerformed() call for browse buttons.
*/
private void actionPerformedBrowse( JTextField tf, String title, String ext, String extDescription )
{
    String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
    JFileChooser fc = null;
    if ( last_directory_selected != null ) {
        fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
    }
    else {
        fc = JFileChooserFactory.createJFileChooser( __working_dir );
    }
    fc.setDialogTitle( title );
    SimpleFileFilter sff = new SimpleFileFilter(ext, extDescription);
    fc.addChoosableFileFilter(sff);
    
    if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        String directory = fc.getSelectedFile().getParent();
        String filename = fc.getSelectedFile().getName(); 
        String path = fc.getSelectedFile().getPath(); 

        if (filename == null || filename.equals("")) {
            return;
        }

        if (path != null) {
			// Convert path to relative path by default.
			try {
				tf.setText(IOUtil.toRelativePath(__working_dir, path));
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,"RunDSSUTL_JDialog", "Error converting file to relative path." );
			}
            JGUIUtil.setLastFileDialogDirectory(directory);
            refresh();
        }
    }
}

private void actionPerformedPath ( SimpleJButton button, JTextField tf, String add, String remove )
{   String routine = getClass().getName() + ".actionPerformedPath";
    if ( button.getText().equals( add ) ) {
        tf.setText ( IOUtil.toAbsolutePath(__working_dir, tf.getText() ) );
    }
    else if ( button.getText().equals( remove ) ) {
        try {
            tf.setText ( IOUtil.toRelativePath ( __working_dir, tf.getText() ) );
        }
        catch ( Exception e ) {
            Message.printWarning ( 1, routine, "Error converting file to relative path." );
        }
    }
    refresh ();
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	String DssFile = __DssFile_JTextField.getText().trim();
	String InputFile = __InputFile_JTextField.getText().trim();
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String DssutlProgram = __DssutlProgram_JTextField.getText().trim();
	//String Arguments = __Arguments_JTextArea.getText().trim();
	__error_wait = false;
	if ( DssFile.length() > 0 ) {
		props.set ( "DssFile", DssFile );
	}
    if ( InputFile.length() > 0 ) {
        props.set ( "InputFile", InputFile );
    }
    if ( OutputFile.length() > 0 ) {
        props.set ( "OutputFile", OutputFile );
    }
    if ( DssutlProgram.length() > 0 ) {
        props.set ( "DssutlProgram", DssutlProgram );
    }
	/*
    if ( Arguments.length() > 0 ) {
        props.set ( "Arguments", Arguments );
    }
    */
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
{	String DssFile = __DssFile_JTextField.getText().trim();
    String InputFile = __InputFile_JTextField.getText().trim();
    String OutputFile = __OutputFile_JTextField.getText().trim();
    String DssutlProgram = __DssutlProgram_JTextField.getText().trim();
    //String Arguments = __Arguments_JTextArea.getText().trim();
	__command.setCommandParameter ( "DssFile", DssFile );
	__command.setCommandParameter ( "InputFile", InputFile );
	__command.setCommandParameter ( "OutputFile", OutputFile );
	__command.setCommandParameter ( "DssutlProgram", DssutlProgram );
	//__command.setCommandParameter ( "Arguments", Arguments );
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, RunDSSUTL_Command command )
{	__command = (RunDSSUTL_Command)command;
	CommandProcessor processor =__command.getCommandProcessor();
	
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command runs the DSSUTL and other HEC-DSS utility programs, " +
		"which have been developed by the US Army Corps of Engineers." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "DSSUTL processes data stored in a HEC-DSS binary database file, using DSSUTL commands in the input file." ),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The utility program location must be in the PATH environment variable, specified using an absolute" +
        "path, or specified using ${WorkingDir}, which is the location of the command file." ),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The HEC-DSS file must exist because otherwise the DSSUTL software prompts for the filename, and interaction " +
        "with utility programs from TSTool is not enabled." ),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The program will be run in TSTool's working directory (command file location)." ),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify a full or relative path to HEC-DSS, input, and output files (relative to working directory)." ), 
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel("The working directory is: " + __working_dir ), 
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("HEC-DSS file:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DssFile_JTextField = new JTextField ( 50 );
	__DssFile_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __DssFile_JTextField,
		1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseDssFile_JButton = new SimpleJButton ( "...", this );
	__browseDssFile_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(main_JPanel, __browseDssFile_JButton,
		6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
    __DssFile_JTextField.setToolTipText("Required for DSSUTL, DSPLAY.");
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__pathDssFile_JButton = new SimpleJButton(__RemoveWorkingDirectoryDssFile,this);
	    JGUIUtil.addComponent(main_JPanel, __pathDssFile_JButton,
	    	7, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Input file:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputFile_JTextField = new JTextField ( 50 );
    __InputFile_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __InputFile_JTextField,
        1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    __browseInputFile_JButton = new SimpleJButton ( "...", this );
	__browseInputFile_JButton.setToolTipText("Browse for file");
        JGUIUtil.addComponent(main_JPanel, __browseInputFile_JButton,
        6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
    __InputFile_JTextField.setToolTipText("Required for all programs.");
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__pathInputFile_JButton = new SimpleJButton(__RemoveWorkingDirectoryInputFile,this);
	    JGUIUtil.addComponent(main_JPanel, __pathInputFile_JButton,
	    	7, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Output file:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputFile_JTextField = new JTextField ( 50 );
    __OutputFile_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __OutputFile_JTextField,
        1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    __browseOutputFile_JButton = new SimpleJButton ( "...", this );
	__browseOutputFile_JButton.setToolTipText("Browse for file");
        JGUIUtil.addComponent(main_JPanel, __browseOutputFile_JButton,
        6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
    __OutputFile_JTextField.setToolTipText("Optional for all programs.");
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__pathOutputFile_JButton = new SimpleJButton(__RemoveWorkingDirectoryOutputFile,this);
	    JGUIUtil.addComponent(main_JPanel, __pathOutputFile_JButton,
	    	7, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("DSSUTL program:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DssutlProgram_JTextField = new JTextField ( 50 );
    __DssutlProgram_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __DssutlProgram_JTextField,
        1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    __browseDssutlProgram_JButton = new SimpleJButton ( "...", this );
	__browseDssutlProgram_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(main_JPanel, __browseDssutlProgram_JButton,
        6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__pathDssutlProgram_JButton = new SimpleJButton(__RemoveWorkingDirectoryDssutlProgram,this);
	    JGUIUtil.addComponent(main_JPanel, __pathDssutlProgram_JButton,
	    	7, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}

        /* TODO SAM 2009-04-09 Possibly enable later if canned arguments are not enough
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Arguments:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Arguments_JTextArea = new JTextArea ( 4, 55 );
    __Arguments_JTextArea.setLineWrap ( true );
    __Arguments_JTextArea.setWrapStyleWord ( true );
    __Arguments_JTextArea.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__Arguments_JTextArea),
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        */

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 55 );
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
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
	button_JPanel.add (__cancel_JButton = new SimpleJButton("Cancel",this));
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + " Command" );
    pack();
    JGUIUtil.center( this );
	refresh();	// Sets the __path_JButton status
	// Dialogs do not need to be resizable...
	setResizable ( true );
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent e )
{
    refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

    refresh ();
	if ( code == KeyEvent.VK_ENTER ) {
		checkInput ();
		if ( !__error_wait ) {
			response ( false );
		}
	}
}

public void keyReleased ( KeyEvent event )
{	refresh();
}

public void keyTyped ( KeyEvent event )
{	refresh();
}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user cancelled.
*/
public boolean ok ()
{	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	//String routine = __command.getCommandName() + "_JDialog.refresh";
    String DssFile = "";
    String InputFile = "";
    String OutputFile = "";
    String DssutlProgram = "";
    //String Arguments = "";
    String Interpreter = "";
	PropList props = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		props = __command.getCommandParameters();
		DssFile = props.getValue ( "DssFile" );
		InputFile = props.getValue ( "InputFile" );
		OutputFile = props.getValue ( "OutputFile" );
		DssutlProgram = props.getValue ( "DssutlProgram" );
		//Arguments = props.getValue ( "Arguments" );
		Interpreter = props.getValue ( "Interpreter" );
		if ( DssFile != null ) {
			__DssFile_JTextField.setText ( DssFile );
		}
        if ( InputFile != null ) {
            __InputFile_JTextField.setText ( InputFile );
        }
        if ( OutputFile != null ) {
            __OutputFile_JTextField.setText ( OutputFile );
        }
        if ( DssutlProgram != null ) {
            __DssutlProgram_JTextField.setText ( DssutlProgram );
        }
		/*
        if ( Arguments != null ) {
            __Arguments_JTextArea.setText ( Arguments );
        }
        */
	}
	// Regardless, reset the command from the fields...
	DssFile = __DssFile_JTextField.getText().trim();
	InputFile = __InputFile_JTextField.getText().trim();
	OutputFile = __OutputFile_JTextField.getText().trim();
	DssutlProgram = __DssutlProgram_JTextField.getText().trim();
	//Arguments = __Arguments_JTextArea.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "DssFile=" + DssFile );
	props.add ( "InputFile=" + InputFile );
	props.add ( "OutputFile=" + OutputFile );
	props.add ( "DssutlProgram=" + DssutlProgram );
	//props.add ( "Arguments=" + Arguments );
	props.add ( "Interpreter=" + Interpreter );
	__command_JTextArea.setText( __command.toString ( props ).trim() );
	// Check the path and determine what the label on the path buttons should be...
	if ( __pathDssFile_JButton != null ) {
		__pathDssFile_JButton.setEnabled ( true );
		File f = new File ( DssFile );
		if ( f.isAbsolute() ) {
			__pathDssFile_JButton.setText ( __RemoveWorkingDirectoryDssFile );
			__pathDssFile_JButton.setToolTipText("Change path to relative to command file");
		}
		else {
            __pathDssFile_JButton.setText ( __AddWorkingDirectoryDssFile );
            __pathDssFile_JButton.setToolTipText("Change path to absolute");
		}
	}
    if ( __pathInputFile_JButton != null ) {
        __pathInputFile_JButton.setEnabled ( true );
        File f = new File ( InputFile );
        if ( f.isAbsolute() ) {
            __pathInputFile_JButton.setText ( __RemoveWorkingDirectoryInputFile );
            __pathInputFile_JButton.setToolTipText("Change path to relative to command file");
        }
        else {
            __pathInputFile_JButton.setText ( __AddWorkingDirectoryInputFile );
            __pathInputFile_JButton.setToolTipText("Change path to absolute");
        }
    }
    if ( __pathOutputFile_JButton != null ) {
        __pathOutputFile_JButton.setEnabled ( true );
        File f = new File ( OutputFile );
        if ( f.isAbsolute() ) {
            __pathOutputFile_JButton.setText ( __RemoveWorkingDirectoryOutputFile );
            __pathOutputFile_JButton.setToolTipText("Change path to relative to command file");
        }
        else {
            __pathOutputFile_JButton.setText ( __AddWorkingDirectoryOutputFile );
            __pathOutputFile_JButton.setToolTipText("Change path to absolute");
        }
    }
    if ( __pathDssutlProgram_JButton != null ) {
        __pathDssutlProgram_JButton.setEnabled ( true );
        File f = new File ( DssutlProgram );
        if ( f.isAbsolute() ) {
            __pathDssutlProgram_JButton.setText ( __RemoveWorkingDirectoryDssutlProgram );
            __pathDssutlProgram_JButton.setToolTipText("Change path to relative to command file");
        }
        else {
            __pathDssutlProgram_JButton.setText ( __AddWorkingDirectoryDssutlProgram );
            __pathDssutlProgram_JButton.setToolTipText("Change path to absolute");
        }
    }
}

/**
React to the user response.
@param ok if false, then the edit is cancelled.  If true, the edit is committed
and the dialog is closed.
*/
private void response ( boolean ok )
{	__ok = ok;	// Save to be returned by ok()
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
