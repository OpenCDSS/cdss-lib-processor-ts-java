// RunCommands_JDialog - editor for RunCommands command

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2023 Colorado Department of Natural Resources

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
import java.util.ArrayList;
import java.util.List;

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

@SuppressWarnings("serial")
public class RunCommands_JDialog extends JDialog
implements ActionListener, KeyListener, WindowListener
{

private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __browse_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SimpleJButton __path_JButton = null;
private RunCommands_Command __command = null;
private JTextArea __command_JTextArea=null;
private String __working_dir = null;
private JTextField __InputFile_JTextField = null;
private SimpleJComboBox __ExpectedStatus_JComboBox = null;
//private SimpleJComboBox __ShareProperties_JComboBox = null;
private SimpleJComboBox __ShareDataStores_JComboBox = null;
private SimpleJComboBox __AppendOutputFiles_JComboBox = null;
private JTextField __WarningCountProperty_JTextField = null;
private JTextField __FailureCountProperty_JTextField = null;
private boolean __error_wait = false; // Is there an error waiting to be cleared up.
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK was pressed when closing the dialog.

/**
Command editor dialog constructor.
@param parent Frame class instantiating this class.
@param command Command to edit.
*/
public RunCommands_JDialog ( JFrame parent, RunCommands_Command command ) {
	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event ) {
	Object o = event.getSource();

	if ( o == __browse_JButton ) {
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
		}
		else {
            fc = JFileChooserFactory.createJFileChooser( __working_dir );
		}
		fc.setDialogTitle( "Select command file to run");
		List<String> extensionList = new ArrayList<>();
		extensionList.add("tstool");
		extensionList.add("TSTool");
		SimpleFileFilter sff = new SimpleFileFilter(extensionList, "TSTool Command File");
	    fc.addChoosableFileFilter(sff);
	    fc.setFileFilter(sff);

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
					__InputFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1,"RunCommands_JDialog", "Error converting file to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory(directory);
				refresh();
			}
		}
	}
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "RunCommands");
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
			__InputFile_JTextField.setText ( IOUtil.toAbsolutePath(__working_dir,__InputFile_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals( __RemoveWorkingDirectory) ) {
			try {
                __InputFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,	__InputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,"RunCommands_JDialog",	"Error converting file to relative path." );
			}
		}
		refresh ();
	}
	else {
		// Parameter choices.
		refresh ();
	}
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag to true.
This should be called before response() is allowed to complete.
*/
private void checkInput () {
	// Create a list of parameters to check.
	PropList props = new PropList ( "" );
	String InputFile = __InputFile_JTextField.getText().trim();
    String ExpectedStatus = __ExpectedStatus_JComboBox.getSelected();
    //String ShareProperties = __ShareProperties_JComboBox.getSelected();
    String ShareDataStores = __ShareDataStores_JComboBox.getSelected();
    String AppendOutputFiles = __AppendOutputFiles_JComboBox.getSelected();
	String WarningCountProperty = __WarningCountProperty_JTextField.getText().trim();
	String FailureCountProperty = __FailureCountProperty_JTextField.getText().trim();
	__error_wait = false;
	if ( InputFile.length() > 0 ) {
		props.set ( "InputFile", InputFile );
	}
    if ( ExpectedStatus.length() > 0 ) {
        props.set ( "ExpectedStatus", ExpectedStatus );
    }
    //if ( ShareProperties.length() > 0 ) {
    //    props.set ( "ShareProperties", ShareProperties );
    //}
    if ( ShareDataStores.length() > 0 ) {
        props.set ( "ShareDataStores", ShareDataStores );
    }
    if ( AppendOutputFiles.length() > 0 ) {
        props.set ( "AppendOutputFiles", AppendOutputFiles );
    }
    if ( WarningCountProperty.length() > 0 ) {
        props.set ( "WarningCountProperty", WarningCountProperty );
    }
    if ( FailureCountProperty.length() > 0 ) {
        props.set ( "FailureCountProperty", FailureCountProperty );
    }
	try {
		// This will warn the user.
		__command.checkCommandParameters ( props, null, 1 );
	}
	catch ( Exception e ) {
		// The warning would have been printed in the check code.
		__error_wait = true;
	}
}

/**
Commit the edits to the command.
In this case the command parameters have already been checked and no errors were detected.
*/
private void commitEdits () {
	String InputFile = __InputFile_JTextField.getText().trim();
    String ExpectedStatus = __ExpectedStatus_JComboBox.getSelected();
    //String ShareProperties = __ShareProperties_JComboBox.getSelected();
    String ShareDataStores = __ShareDataStores_JComboBox.getSelected();
    String AppendOutputFiles = __AppendOutputFiles_JComboBox.getSelected();
	String WarningCountProperty = __WarningCountProperty_JTextField.getText().trim();
	String FailureCountProperty = __FailureCountProperty_JTextField.getText().trim();
	__command.setCommandParameter ( "InputFile", InputFile );
    __command.setCommandParameter ( "ExpectedStatus", ExpectedStatus );
    //__command.setCommandParameter ( "ShareProperties", ShareProperties );
    __command.setCommandParameter ( "ShareDataStores", ShareDataStores );
    __command.setCommandParameter ( "AppendOutputFiles", AppendOutputFiles );
    __command.setCommandParameter ( "WarningCountProperty", WarningCountProperty );
    __command.setCommandParameter ( "FailureCountProperty", FailureCountProperty );
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, RunCommands_Command command ) {
	__command = command;
	CommandProcessor processor =__command.getCommandProcessor();

	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel.

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Read a command file and run the commands using a separate command processor." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Parent command processor datastores can be shared with the processor for the command file." ),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Results are cleared before processing each command file." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The (success/warning/failure) status from the command file is used for the RunCommands() command status." ),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify a full or relative path (relative to working directory)." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel ( "The working directory is: " + __working_dir ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command file to run:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputFile_JTextField = new JTextField ( 50 );
	__InputFile_JTextField.setToolTipText("Specify the file to remove or use ${Property} notation");
	__InputFile_JTextField.addKeyListener ( this );
    // Input file layout fights back with other rows so put in its own panel.
	JPanel InputFile_JPanel = new JPanel();
	InputFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(InputFile_JPanel, __InputFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ( "...", this );
	__browse_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(InputFile_JPanel, __browse_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__path_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(InputFile_JPanel, __path_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(main_JPanel, InputFile_JPanel,
		1, y, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Expected status:"),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ExpectedStatus_JComboBox = new SimpleJComboBox ( false );
    List<String> statusChoices = new ArrayList<>();
    statusChoices.add ( "" );   // Default.
    statusChoices.add ( __command._Unknown );
    statusChoices.add ( __command._Success );
    statusChoices.add ( __command._Warning );
    statusChoices.add ( __command._Failure );
    __ExpectedStatus_JComboBox.setData(statusChoices);
    __ExpectedStatus_JComboBox.select ( 0 );
    __ExpectedStatus_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __ExpectedStatus_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - use for testing (default=" + __command._Success + ")."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    /*
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Share parent workflow properties?:"),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ShareProperties_JComboBox = new SimpleJComboBox ( false );
    __ShareProperties_JComboBox.addItem ( "" );   // Default
    __ShareProperties_JComboBox.addItem ( __command._Copy );
    __ShareProperties_JComboBox.addItem ( __command._DoNotShare );
    __ShareProperties_JComboBox.addItem ( __command._Share );
    __ShareProperties_JComboBox.select ( 0 );
    __ShareProperties_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __ShareProperties_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - share properties (default=" + __command._DoNotShare + ")."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        */

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Share parent datastores?:"),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ShareDataStores_JComboBox = new SimpleJComboBox ( false );
    List<String> shareChoices = new ArrayList<>();
    shareChoices.add ( "" ); // Default.
    //shareChoices.add ( __command._Copy ); // Too difficult?
    shareChoices.add ( __command._DoNotShare );
    shareChoices.add ( __command._Share );
    __ShareDataStores_JComboBox.setData(shareChoices);
    __ShareDataStores_JComboBox.select ( 0 );
    __ShareDataStores_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __ShareDataStores_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - share datastores (default=" + __command._Share + ")."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Append output files?:"),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AppendOutputFiles_JComboBox = new SimpleJComboBox ( false );
    __AppendOutputFiles_JComboBox.setToolTipText("Should output files from the command file be added to the full output file list?");
    List<String> appendFilesChoices = new ArrayList<>();
    appendFilesChoices.add ( "" ); // Default.
    appendFilesChoices.add ( __command._False );
    appendFilesChoices.add ( __command._True );
    __AppendOutputFiles_JComboBox.setData(appendFilesChoices);
    __AppendOutputFiles_JComboBox.select ( 0 );
    __AppendOutputFiles_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __AppendOutputFiles_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - append output files (default=" + __command._False + ")."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel("Warning count property:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __WarningCountProperty_JTextField = new JTextField ( "", 20 );
    __WarningCountProperty_JTextField.setToolTipText("Specify the property name for warning count, can use ${Property} notation");
    __WarningCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __WarningCountProperty_JTextField,
        1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - processor property to set as warning count." ),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel("Failure count property:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __FailureCountProperty_JTextField = new JTextField ( "", 20 );
    __FailureCountProperty_JTextField.setToolTipText("Specify the property name for failure count, can use ${Property} notation");
    __FailureCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __FailureCountProperty_JTextField,
        1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - processor property to set as failure count." ),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

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
	refresh();	// Sets the __path_JButton status.
	// Dialogs do not need to be resizable.
	setResizable ( false );
    super.setVisible( true );
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event ) {
	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh ();
		checkInput ();
		if ( !__error_wait ) {
			response ( false );
		}
	}
}

public void keyReleased ( KeyEvent event ) {
	refresh();
}

public void keyTyped ( KeyEvent event ) {
	refresh();
}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user canceled.
*/
public boolean ok () {
	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh () {
	String routine = getClass().getSimpleName() + ".refresh";
    String InputFile = "";
    String ExpectedStatus = "";
    //String ShareProperties = "";
    String ShareDataStores = "";
    String AppendOutputFiles = "";
    String WarningCountProperty = "";
    String FailureCountProperty = "";
	PropList props = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		props = __command.getCommandParameters();
		InputFile = props.getValue ( "InputFile" );
        ExpectedStatus = props.getValue ( "ExpectedStatus" );
        //ShareProperties = props.getValue ( "ShareProperties" );
        ShareDataStores = props.getValue ( "ShareDataStores" );
        AppendOutputFiles = props.getValue ( "AppendOutputFiles" );
        WarningCountProperty = props.getValue ( "WarningCountProperty" );
        FailureCountProperty = props.getValue ( "FailureCountProperty" );
		if ( InputFile != null ) {
			__InputFile_JTextField.setText ( InputFile );
		}
        if ( JGUIUtil.isSimpleJComboBoxItem(__ExpectedStatus_JComboBox, ExpectedStatus,JGUIUtil.NONE, null, null ) ) {
            __ExpectedStatus_JComboBox.select ( ExpectedStatus );
        }
        else {
            if ( (ExpectedStatus == null) || ExpectedStatus.equals("") ) {
                // New command...select the default.
                __ExpectedStatus_JComboBox.select ( 0 );
            }
            else {
            	// Bad user command.
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\n"+
                "ExpectedStatus parameter \"" +
                ExpectedStatus +
                "\".  Select a\nMissing value or Cancel." );
            }
        }
        /*
        if ( JGUIUtil.isSimpleJComboBoxItem(__ShareProperties_JComboBox, ShareProperties,JGUIUtil.NONE, null, null ) ) {
            __ShareProperties_JComboBox.select ( ShareProperties );
        }
        else {
            if ( (ShareProperties == null) || ShareProperties.equals("") ) {
                // New command...select the default...
                __ShareProperties_JComboBox.select ( 0 );
            }
            else {  // Bad user command...
                Message.printWarning ( 1, routine,
                "Existing command references an invalid ShareProperties parameter \"" +
                ShareProperties + "\".  Correct or Cancel." );
            }
        }
        */
        if ( JGUIUtil.isSimpleJComboBoxItem(__ShareDataStores_JComboBox, ShareDataStores,JGUIUtil.NONE, null, null ) ) {
            __ShareDataStores_JComboBox.select ( ShareDataStores );
        }
        else {
            if ( (ShareDataStores == null) || ShareDataStores.equals("") ) {
                // New command...select the default.
                __ShareDataStores_JComboBox.select ( 0 );
            }
            else {
            	// Bad user command.
                Message.printWarning ( 1, routine,
                "Existing command references an invalid ShareDataStores parameter \"" +
                ShareDataStores + "\".  Correct or Cancel." );
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(__AppendOutputFiles_JComboBox, AppendOutputFiles,JGUIUtil.NONE, null, null ) ) {
            __AppendOutputFiles_JComboBox.select ( AppendOutputFiles );
        }
        else {
            if ( (AppendOutputFiles == null) || AppendOutputFiles.equals("") ) {
                // New command...select the default.
                __AppendOutputFiles_JComboBox.select ( 0 );
            }
            else {
            	// Bad user command.
                Message.printWarning ( 1, routine,
                "Existing command references an invalid AppendOutputFiles parameter \"" +
                AppendOutputFiles + "\".  Correct or Cancel." );
            }
        }
		if ( WarningCountProperty != null ) {
			__WarningCountProperty_JTextField.setText ( WarningCountProperty );
		}
		if ( FailureCountProperty != null ) {
			__FailureCountProperty_JTextField.setText ( FailureCountProperty );
		}
	}
	// Regardless, reset the command from the fields.
	InputFile = __InputFile_JTextField.getText().trim();
    ExpectedStatus = __ExpectedStatus_JComboBox.getSelected();
    //ShareProperties = __ShareProperties_JComboBox.getSelected();
    ShareDataStores = __ShareDataStores_JComboBox.getSelected();
    AppendOutputFiles = __AppendOutputFiles_JComboBox.getSelected();
    WarningCountProperty = __WarningCountProperty_JTextField.getText().trim();
    FailureCountProperty = __FailureCountProperty_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "InputFile=" + InputFile );
    props.add ( "ExpectedStatus=" + ExpectedStatus );
    //props.add ( "ShareProperties=" + ShareProperties );
    props.add ( "ShareDataStores=" + ShareDataStores );
    props.add ( "AppendOutputFiles=" + AppendOutputFiles );
    props.add ( "WarningCountProperty=" + WarningCountProperty );
    props.add ( "FailureCountProperty=" + FailureCountProperty );
	__command_JTextArea.setText( __command.toString ( props ).trim() );
	// Check the path and determine what the label on the path button should be.
	if ( __path_JButton != null ) {
		if ( (InputFile != null) && !InputFile.isEmpty() ) {
			__path_JButton.setEnabled ( true );
			File f = new File ( InputFile );
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
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
*/
private void response ( boolean ok ) {
	__ok = ok;	// Save to be returned by ok().
	if ( ok ) {
		// Commit the changes.
		commitEdits ();
		if ( __error_wait ) {
			// Not ready to close out.
			return;
		}
	}
	// Now close out.
	setVisible( false );
	dispose();
}

/**
Responds to WindowEvents.
@param event WindowEvent object
*/
public void windowClosing( WindowEvent event ) {
	response ( false );
}

public void windowActivated( WindowEvent evt ) {
}

public void windowClosed( WindowEvent evt ) {
}

public void windowDeactivated( WindowEvent evt ) {
}

public void windowDeiconified( WindowEvent evt ) {
}

public void windowIconified( WindowEvent evt ) {
}

public void windowOpened( WindowEvent evt ) {
}

}