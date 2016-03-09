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

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

/**
Editor for CreateRegressionTestCommandFile command.
*/
public class CreateRegressionTestCommandFile_JDialog extends JDialog
implements ActionListener, KeyListener, WindowListener
{

private final String __AddWorkingDirectorySearchFolder = "Add Working Directory (Search Folder)";
private final String __RemoveWorkingDirectorySearchFolder = "Remove Working Directory (Search Folder)";

private final String __AddWorkingDirectoryOutputFile = "Add Working Directory (Output File)";
private final String __RemoveWorkingDirectoryOutputFile = "Remove Working Directory (Output File)";

private final String __AddWorkingDirectorySetupCommandFile = "Add Working Directory (Setup File)";
private final String __RemoveWorkingDirectorySetupCommandFile = "Remove Working Directory (Setup File)";

private final String __AddWorkingDirectoryEndCommandFile = "Add Working Directory (End File)";
private final String __RemoveWorkingDirectoryEndCommandFile = "Remove Working Directory (End File)";

private SimpleJButton __browseSearchFolder_JButton = null;
private SimpleJButton __browseOutputFile_JButton = null;
private SimpleJButton __browseSetupCommandFile_JButton = null;
private SimpleJButton __browseEndCommandFile_JButton = null;
private SimpleJButton __pathSearchFolder_JButton = null;
private SimpleJButton __pathOutputFile_JButton = null;
private SimpleJButton __pathSetupCommandFile_JButton = null;
private SimpleJButton __pathEndCommandFile_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private JTextField __SearchFolder_JTextField = null;	// Top folder to start search
private JTextField __FilenamePattern_JTextField = null;	// Pattern for file names
private JTextField __OutputFile_JTextField = null;	// Resulting command file
private JTextField __SetupCommandFile_JTextField = null; // Setup command file
private JTextField __EndCommandFile_JTextField = null;
private SimpleJComboBox __Append_JComboBox = null;
private JTextField __IncludeTestSuite_JTextField = null;
private JTextField __IncludeOS_JTextField = null;
private JTextField __TestResultsTableID_JTextField = null;
private JTextArea __command_JTextArea = null;
private String __working_dir = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private CreateRegressionTestCommandFile_Command __command = null;
private boolean __ok = false; // Indicates whether the user has pressed OK to close the dialog.

/**
Dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public CreateRegressionTestCommandFile_JDialog ( JFrame parent, CreateRegressionTestCommandFile_Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();
	String routine = getClass().getName() + ".actionPerformed";

	if ( o == __browseSearchFolder_JButton ) {
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
		}
		else {
		    fc = JFileChooserFactory.createJFileChooser(__working_dir );
		}
		fc.setFileSelectionMode (JFileChooser.DIRECTORIES_ONLY );
		fc.setDialogTitle( "Select Folder to Search For Command Files");
		
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			String filename = fc.getSelectedFile().getName(); 
			String path = fc.getSelectedFile().getPath(); 
	
			if (filename == null || filename.equals("")) {
				return;
			}
	
			if (path != null) {
				__SearchFolder_JTextField.setText(path );
				JGUIUtil.setLastFileDialogDirectory(path);
				refresh();
			}
		}
	}
	else if ( o == __browseSetupCommandFile_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser(__working_dir );
        }
        fc.setDialogTitle( "Select Setup Command File to Include at Start");
        SimpleFileFilter sff = new SimpleFileFilter("TSTool","TSTool Command File");
        fc.addChoosableFileFilter(sff);
        
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String directory = fc.getSelectedFile().getParent();
            String filename = fc.getSelectedFile().getName(); 
            String path = fc.getSelectedFile().getPath(); 
    
            if (filename == null || filename.equals("")) {
                return;
            }
    
            if (path != null) {
                __SetupCommandFile_JTextField.setText(path );
                JGUIUtil.setLastFileDialogDirectory(directory);
                refresh();
            }
        }
    }
	else if ( o == __browseEndCommandFile_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser(__working_dir );
        }
        fc.setDialogTitle( "Select Command File to Include at End");
        SimpleFileFilter sff = new SimpleFileFilter("TSTool","TSTool Command File");
        fc.addChoosableFileFilter(sff);
        
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String directory = fc.getSelectedFile().getParent();
            String filename = fc.getSelectedFile().getName(); 
            String path = fc.getSelectedFile().getPath(); 
    
            if (filename == null || filename.equals("")) {
                return;
            }
    
            if (path != null) {
                __EndCommandFile_JTextField.setText(path );
                JGUIUtil.setLastFileDialogDirectory(directory);
                refresh();
            }
        }
    }
	else if ( o == __browseOutputFile_JButton ) {
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
		}
		else {
		    fc = JFileChooserFactory.createJFileChooser( __working_dir );
		}
		fc.setDialogTitle( "Select Command File to Create");
        SimpleFileFilter sff = new SimpleFileFilter("TSTool","TSTool Command File");
        fc.addChoosableFileFilter(sff);
		
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			String directory = fc.getSelectedFile().getParent();
			String filename = fc.getSelectedFile().getName(); 
			String path = fc.getSelectedFile().getPath(); 
	
			if (filename == null || filename.equals("")) {
				return;
			}
	
			if (path != null) {
				__OutputFile_JTextField.setText(path );
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
	else if ( o == __pathSearchFolder_JButton ) {
		if ( __pathSearchFolder_JButton.getText().equals( __AddWorkingDirectorySearchFolder) ) {
			__SearchFolder_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir, __SearchFolder_JTextField.getText() ) );
		}
		else if ( __pathSearchFolder_JButton.getText().equals( __RemoveWorkingDirectorySearchFolder) ) {
			try {
			    __SearchFolder_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir, __SearchFolder_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,routine,
				"Error converting folder name to relative path." );
			}
		}
		refresh ();
	}
	else if ( o == __pathOutputFile_JButton ) {
		if ( __pathOutputFile_JButton.getText().equals( __AddWorkingDirectoryOutputFile) ) {
			__OutputFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir,__OutputFile_JTextField.getText() ) );
		}
		else if ( __pathOutputFile_JButton.getText().equals( __RemoveWorkingDirectoryOutputFile) ) {
			try {
			    __OutputFile_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir,__OutputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,routine,"Error converting output file name to relative path." );
			}
		}
		refresh ();
	}
    else if ( o == __pathSetupCommandFile_JButton ) {
        if ( __pathSetupCommandFile_JButton.getText().equals( __AddWorkingDirectorySetupCommandFile) ) {
            __SetupCommandFile_JTextField.setText (
            IOUtil.toAbsolutePath(__working_dir,__SetupCommandFile_JTextField.getText() ) );
        }
        else if ( __pathSetupCommandFile_JButton.getText().equals( __RemoveWorkingDirectorySetupCommandFile) ) {
            try {
                __SetupCommandFile_JTextField.setText (
                IOUtil.toRelativePath ( __working_dir,__SetupCommandFile_JTextField.getText() ) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1,routine,"Error converting setup command file name to relative path." );
            }
        }
        refresh ();
    }
    else if ( o == __pathEndCommandFile_JButton ) {
        if ( __pathEndCommandFile_JButton.getText().equals( __AddWorkingDirectoryEndCommandFile) ) {
            __EndCommandFile_JTextField.setText (
            IOUtil.toAbsolutePath(__working_dir,__EndCommandFile_JTextField.getText() ) );
        }
        else if ( __pathEndCommandFile_JButton.getText().equals( __RemoveWorkingDirectoryEndCommandFile) ) {
            try {
                __EndCommandFile_JTextField.setText (
                IOUtil.toRelativePath ( __working_dir,__EndCommandFile_JTextField.getText() ) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1,routine,"Error converting end command file name to relative path." );
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
	String SearchFolder = __SearchFolder_JTextField.getText().trim();
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String SetupCommandFile = __SetupCommandFile_JTextField.getText().trim();
	String EndCommandFile = __EndCommandFile_JTextField.getText().trim();
	String FilenamePattern = __FilenamePattern_JTextField.getText().trim();
	String Append = __Append_JComboBox.getSelected();
	String IncludeTestSuite = __IncludeTestSuite_JTextField.getText().trim();
	String IncludeOS = __IncludeOS_JTextField.getText().trim();
	String TestResultsTableID = __TestResultsTableID_JTextField.getText().trim();

	if ( SearchFolder.length() > 0 ) {
		props.set ( "SearchFolder", SearchFolder );
	}
	if ( OutputFile.length() > 0 ) {
		props.set ( "OutputFile", OutputFile );
	}
    if ( SetupCommandFile.length() > 0 ) {
        props.set ( "SetupCommandFile", SetupCommandFile );
    }
    if ( EndCommandFile.length() > 0 ) {
        props.set ( "EndCommandFile", EndCommandFile );
    }
    if ( FilenamePattern.length() > 0 ) {
        props.set ( "FilenamePattern", FilenamePattern );
    }
	if ( Append.length() > 0 ) {
		props.set ( "Append", Append );
	}
    if ( IncludeTestSuite.length() > 0 ) {
        props.set ( "IncludeTestSuite", IncludeTestSuite );
    }
    if ( IncludeOS.length() > 0 ) {
        props.set ( "IncludeOS", IncludeOS );
    }
	if ( TestResultsTableID.length() > 0 ) {
		props.set ( "TestResultsTableID", TestResultsTableID );
	}
	try {
	    // This will warn the user...
		__command.checkCommandParameters ( props, null, 1 );
		__error_wait = false;
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
{	String SearchFolder = __SearchFolder_JTextField.getText().trim();
	String OutputFile = __OutputFile_JTextField.getText();
	String SetupCommandFile = __SetupCommandFile_JTextField.getText().trim();
	String EndCommandFile = __EndCommandFile_JTextField.getText().trim();
	String FilenamePattern = __FilenamePattern_JTextField.getText().trim();
	String Append = __Append_JComboBox.getSelected();
	String IncludeTestSuite = __IncludeTestSuite_JTextField.getText().trim();
	String IncludeOS = __IncludeOS_JTextField.getText().trim();
	String TestResultsTableID = __TestResultsTableID_JTextField.getText().trim();
	__command.setCommandParameter ( "SearchFolder", SearchFolder );
    __command.setCommandParameter ( "OutputFile", OutputFile );
    __command.setCommandParameter ( "SetupCommandFile", SetupCommandFile );
    __command.setCommandParameter ( "EndCommandFile", EndCommandFile );
	__command.setCommandParameter ( "FilenamePattern", FilenamePattern );
	__command.setCommandParameter ( "Append", Append );
	__command.setCommandParameter ( "IncludeTestSuite", IncludeTestSuite );
	__command.setCommandParameter ( "IncludeOS", IncludeOS );
	__command.setCommandParameter ( "TestResultsTableID", TestResultsTableID );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, CreateRegressionTestCommandFile_Command command )
{	__command = command;
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
		"This command creates a regression test command file, for use in software testing." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Test command files should follow documented standards." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"A top-level folder is specified and will be searched for command files matching the specified pattern."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"The resulting output command file will include RunCommands() commands for each matched file," +
    	" and can be independently loaded and run."),
    	0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "A \"setup\" command file can be inserted at the top of the generated command file, for example to initialize " +
        "database connections."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "An \"end\" command file can be inserted at the end of the generated command file, for example to process the summary table."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    if ( __working_dir != null ) {
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"It is recommended that file names are relative to the working directory, which is:"),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"    " + __working_dir),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Folder to search for TSTool command files:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__SearchFolder_JTextField = new JTextField ( 50 );
	__SearchFolder_JTextField.setToolTipText("Specify top-level folder to search for TSTool command files, can use ${Property} notation");
	__SearchFolder_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __SearchFolder_JTextField,
		1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseSearchFolder_JButton = new SimpleJButton ( "Browse", this );
    JGUIUtil.addComponent(main_JPanel, __browseSearchFolder_JButton,
		6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command file to create:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputFile_JTextField = new JTextField ( 50 );
	__OutputFile_JTextField.setToolTipText("Specify command file to create, can use ${Property} notation");
	__OutputFile_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __OutputFile_JTextField,
		1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseOutputFile_JButton = new SimpleJButton ( "Browse", this );
        JGUIUtil.addComponent(main_JPanel, __browseOutputFile_JButton,
		6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Setup command file:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SetupCommandFile_JTextField = new JTextField ( 50 );
    __SetupCommandFile_JTextField.setToolTipText("Specify the setup command file to prepend, can use ${Property} notation");
    __SetupCommandFile_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __SetupCommandFile_JTextField,
        1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    __browseSetupCommandFile_JButton = new SimpleJButton ( "Browse", this );
        JGUIUtil.addComponent(main_JPanel, __browseSetupCommandFile_JButton,
        6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "End command file:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EndCommandFile_JTextField = new JTextField ( 50 );
    __EndCommandFile_JTextField.setToolTipText("Specify the end command file to append, can use ${Property} notation");
    __EndCommandFile_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __EndCommandFile_JTextField,
        1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    __browseEndCommandFile_JButton = new SimpleJButton ( "Browse", this );
        JGUIUtil.addComponent(main_JPanel, __browseEndCommandFile_JButton,
        6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command file name pattern:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __FilenamePattern_JTextField = new JTextField ( 30 );
    __FilenamePattern_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __FilenamePattern_JTextField,
    1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Optional - file pattern to match (default is \"Test_*.TSTool\")."), 
            3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Append to output?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Append_JComboBox = new SimpleJComboBox ( false );
	__Append_JComboBox.addItem ( "" );	// Default
	__Append_JComboBox.addItem ( __command._False );
	__Append_JComboBox.addItem ( __command._True );
	__Append_JComboBox.select ( 0 );
	__Append_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Append_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - append to command file? (default=" + __command._True + ")."), 
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Test suites to include:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IncludeTestSuite_JTextField = new JTextField ( "", 30 );
    __IncludeTestSuite_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __IncludeTestSuite_JTextField,
    1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel,
        new JLabel( "Optional - check \"#@testSuite ABC\" comments for tests to include (default=*)."), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Include tests for OS:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IncludeOS_JTextField = new JTextField ( "", 30 );
    __IncludeOS_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __IncludeOS_JTextField,
    1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel,
        new JLabel( "Optional - check \"#@os Windows|UNIX\" comments for tests to include (default=*)."), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
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
		__pathSearchFolder_JButton = new SimpleJButton( __RemoveWorkingDirectorySearchFolder,this);
		button_JPanel.add ( __pathSearchFolder_JButton );
		__pathOutputFile_JButton = new SimpleJButton( __RemoveWorkingDirectoryOutputFile,this);
		button_JPanel.add ( __pathOutputFile_JButton );
        __pathSetupCommandFile_JButton = new SimpleJButton( __RemoveWorkingDirectorySetupCommandFile,this);
        button_JPanel.add ( __pathSetupCommandFile_JButton );
        __pathEndCommandFile_JButton = new SimpleJButton( __RemoveWorkingDirectoryEndCommandFile,this);
        button_JPanel.add ( __pathEndCommandFile_JButton );
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
{	String routine = getClass().getSimpleName() + ".refresh";
	String SearchFolder = "";
	String SetupCommandFile = "";
	String EndCommandFile = "";
	String OutputFile = "";
	String FilenamePattern = "";
	String Append = "";
	String IncludeTestSuite = "*";
	String IncludeOS = "*";
	String TestResultsTableID = "";
	PropList props = null;
	if ( __first_time ) {
		__first_time = false;
		props = __command.getCommandParameters();
		SearchFolder = props.getValue ( "SearchFolder" );
		OutputFile = props.getValue ( "OutputFile" );
		SetupCommandFile = props.getValue ( "SetupCommandFile" );
		EndCommandFile = props.getValue ( "EndCommandFile" );
	    FilenamePattern = props.getValue ( "FilenamePattern" );
		Append = props.getValue ( "Append" );
		IncludeTestSuite = props.getValue ( "IncludeTestSuite" );
		IncludeOS = props.getValue ( "IncludeOS" );
		TestResultsTableID = props.getValue ( "TestResultsTableID" );
		if ( SearchFolder != null ) {
			__SearchFolder_JTextField.setText ( SearchFolder );
		}
		if ( OutputFile != null ) {
			__OutputFile_JTextField.setText ( OutputFile );
		}
        if ( SetupCommandFile != null ) {
            __SetupCommandFile_JTextField.setText ( SetupCommandFile );
        }
        if ( EndCommandFile != null ) {
            __EndCommandFile_JTextField.setText ( EndCommandFile );
        }
        if ( FilenamePattern != null ) {
            __FilenamePattern_JTextField.setText ( FilenamePattern );
        }
		if ( JGUIUtil.isSimpleJComboBoxItem( __Append_JComboBox, Append, JGUIUtil.NONE, null, null ) ) {
			__Append_JComboBox.select ( Append );
		}
		else {
		    if ( (Append == null) || Append.equals("") ) {
				// New command...select the default...
				__Append_JComboBox.select ( 0 );
			}
			else {
			    // Bad user command...
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\nAppend parameter \"" +
				Append + "\".  Select a\ndifferent value or Cancel." );
			}
		}
        if ( IncludeTestSuite != null ) {
            __IncludeTestSuite_JTextField.setText ( IncludeTestSuite );
        }
        if ( IncludeOS != null ) {
            __IncludeOS_JTextField.setText ( IncludeOS );
        }
		if ( TestResultsTableID != null ) {
			__TestResultsTableID_JTextField.setText ( TestResultsTableID );
		}
	}
	// Regardless, reset the command from the fields.  This is only  visible
	// information that has not been committed in the command.
	SearchFolder = __SearchFolder_JTextField.getText().trim();
	OutputFile = __OutputFile_JTextField.getText().trim();
	SetupCommandFile = __SetupCommandFile_JTextField.getText().trim();
	EndCommandFile = __EndCommandFile_JTextField.getText().trim();
	FilenamePattern = __FilenamePattern_JTextField.getText().trim();
	Append = __Append_JComboBox.getSelected();
	IncludeTestSuite = __IncludeTestSuite_JTextField.getText().trim();
	IncludeOS = __IncludeOS_JTextField.getText().trim();
	TestResultsTableID = __TestResultsTableID_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "SearchFolder=" + SearchFolder );
	props.add ( "OutputFile=" + OutputFile );
	props.add ( "SetupCommandFile=" + SetupCommandFile );
	props.add ( "EndCommandFile=" + EndCommandFile );
	props.add ( "FilenamePattern=" + FilenamePattern );
	props.add ( "Append=" + Append );
	props.add ( "IncludeTestSuite=" + IncludeTestSuite );
	props.add ( "IncludeOS=" + IncludeOS );
	props.add ( "TestResultsTableID=" + TestResultsTableID );
	__command_JTextArea.setText( __command.toString(props) );
	// Check the path and determine what the label on the path button should be...
	if ( __pathSearchFolder_JButton != null ) {
		__pathSearchFolder_JButton.setEnabled ( true );
		File f = new File ( SearchFolder );
		if ( f.isAbsolute() ) {
			__pathSearchFolder_JButton.setText (__RemoveWorkingDirectorySearchFolder);
		}
		else {
		    __pathSearchFolder_JButton.setText (__AddWorkingDirectorySearchFolder );
		}
	}
	if ( __pathOutputFile_JButton != null ) {
		__pathOutputFile_JButton.setEnabled ( true );
		File f = new File ( OutputFile );
		if ( f.isAbsolute() ) {
			__pathOutputFile_JButton.setText (__RemoveWorkingDirectoryOutputFile);
		}
		else {
		    __pathOutputFile_JButton.setText (__AddWorkingDirectoryOutputFile );
		}
	}
    if ( __pathSetupCommandFile_JButton != null ) {
        __pathSetupCommandFile_JButton.setEnabled ( true );
        File f = new File ( SetupCommandFile );
        if ( f.isAbsolute() ) {
            __pathSetupCommandFile_JButton.setText (__RemoveWorkingDirectorySetupCommandFile);
        }
        else {
            __pathSetupCommandFile_JButton.setText (__AddWorkingDirectorySetupCommandFile );
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