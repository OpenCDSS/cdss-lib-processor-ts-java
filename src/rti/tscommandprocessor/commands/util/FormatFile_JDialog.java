// FormatFile_JDialog - editor for FormatFile command

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
import RTi.Util.IO.ProcessManager;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

@SuppressWarnings("serial")
public class FormatFile_JDialog extends JDialog
implements ActionListener, KeyListener, WindowListener
{
private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __browseInputFile_JButton = null;
private SimpleJButton __browsePrependFile_JButton = null;
private SimpleJButton __browseAppendFile_JButton = null;
private SimpleJButton __browseOutputFile_JButton = null;
private SimpleJButton __pathInputFile_JButton = null;
private SimpleJButton __pathPrependFile_JButton = null;
private SimpleJButton __pathAppendFile_JButton = null;
private SimpleJButton __pathOutputFile_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SimpleJButton __visualDiff_JButton = null;
private JTextField __InputFile_JTextField = null;
private JTextField __PrependFile_JTextField = null;
private JTextField __AppendFile_JTextField = null;
private SimpleJComboBox __ContentType_JComboBox = null;
private SimpleJComboBox __AutoFormat_JComboBox = null;
private SimpleJComboBox __OutputType_JComboBox = null;
private JTextField __OutputFile_JTextField = null;
private JTextArea __command_JTextArea = null;
private String __working_dir = null;
private String __diffProgram = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private FormatFile_Command __command = null; // Command to edit
private boolean __ok = false; // Indicates whether the user pressed OK to close the dialog.

private final String __VisualDiff = "Visual Diff";
private final String visualDiffLabel = "Run program to visually compare input and output files (see TSTool DiffProgram configuration property).";

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param diffProgram visual difference program
*/
public FormatFile_JDialog ( JFrame parent, FormatFile_Command command, String diffProgram )
{	super(parent, true);
	initialize ( parent, command, diffProgram );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();
	String routine = "FormatFile_JDialog";

	if ( o == __browseInputFile_JButton ) {
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
		}
		else {
		    fc = JFileChooserFactory.createJFileChooser(__working_dir );
		}
		fc.setDialogTitle( "Select Input File to Format");
		
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
					Message.printWarning ( 1, routine, "Error converting file to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory(directory);
				refresh();
			}
		}
	}
	else if ( o == __browsePrependFile_JButton ) {
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
		}
		else {
		    fc = JFileChooserFactory.createJFileChooser(__working_dir );
		}
		fc.setDialogTitle( "Select File to Prepend");
		
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
					__PrependFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1, routine, "Error converting file to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory(directory);
				refresh();
			}
		}
	}
	else if ( o == __browseAppendFile_JButton ) {
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
		}
		else {
		    fc = JFileChooserFactory.createJFileChooser(__working_dir );
		}
		fc.setDialogTitle( "Select File to Append");
		
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
					__AppendFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1, routine, "Error converting file to relative path." );
				}
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
		    fc = JFileChooserFactory.createJFileChooser(__working_dir );
		}
		fc.setDialogTitle( "Select File to Output");
		
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
					__OutputFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1, routine, "Error converting file to relative path." );
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
		HelpViewer.getInstance().showHelp("command", "FormatFile");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else if ( o == __pathInputFile_JButton ) {
		if ( __pathInputFile_JButton.getText().equals(__AddWorkingDirectory) ) {
			__InputFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir,__InputFile_JTextField.getText() ) );
		}
		else if ( __pathInputFile_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
			    __InputFile_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir, __InputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, routine,
				"Error converting input file name to relative path." );
			}
		}
		refresh ();
	}
	else if ( o == __pathPrependFile_JButton ) {
		if ( __pathPrependFile_JButton.getText().equals(__AddWorkingDirectory) ) {
			__PrependFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir,__PrependFile_JTextField.getText() ) );
		}
		else if ( __pathPrependFile_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
			    __PrependFile_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir, __PrependFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, routine,
				"Error converting prepend file name to relative path." );
			}
		}
		refresh ();
	}
	else if ( o == __pathAppendFile_JButton ) {
		if ( __pathAppendFile_JButton.getText().equals(__AddWorkingDirectory) ) {
			__AppendFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir,__AppendFile_JTextField.getText() ) );
		}
		else if ( __pathAppendFile_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
			    __AppendFile_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir, __AppendFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, routine,
				"Error converting append file name to relative path." );
			}
		}
		refresh ();
	}
	else if ( o == __pathOutputFile_JButton ) {
		if ( __pathOutputFile_JButton.getText().equals(__AddWorkingDirectory) ) {
			__OutputFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir,__OutputFile_JTextField.getText() ) );
		}
		else if ( __pathOutputFile_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
			    __OutputFile_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir, __OutputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, routine,
				"Error converting output file name to relative path." );
			}
		}
		refresh ();
	}
	else if ( o == __visualDiff_JButton ) {
		// Run the diff program on the input and output files
		// (they should have existed because the button will have been disabled if not)
		TSCommandProcessor processor = (TSCommandProcessor)__command.getCommandProcessor();
		String file1Path = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(__working_dir,
                TSCommandProcessorUtil.expandParameterValue(processor, __command, __InputFile_JTextField.getText()) ) );
		String file2Path = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(__working_dir,
                TSCommandProcessorUtil.expandParameterValue(processor, __command, __OutputFile_JTextField.getText()) ) );
		String [] programAndArgsList = { __diffProgram, file1Path, file2Path };
		try {
			ProcessManager pm = new ProcessManager ( programAndArgsList,
					0, // No timeout
	                null, // Exit status indicator
	                false, // Use command shell
	                new File((String)__command.getCommandProcessor().getPropContents("WorkingDir")));
			Thread t = new Thread ( pm );
            t.start();
		}
		catch ( Exception e ) {
			Message.printWarning(1, "", "Unable to run program (" + e + ")" );
		}
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
	String InputFile = __InputFile_JTextField.getText().trim();
	String PrependFile = __PrependFile_JTextField.getText().trim();
	String AppendFile = __AppendFile_JTextField.getText().trim();
	String ContentType = __ContentType_JComboBox.getSelected();
	String AutoFormat = __AutoFormat_JComboBox.getSelected();
	String OutputType = __OutputType_JComboBox.getSelected();
	String OutputFile = __OutputFile_JTextField.getText().trim();
	__error_wait = false;
	if ( InputFile.length() > 0 ) {
		props.set ( "InputFile", InputFile );
	}
	if ( PrependFile.length() > 0 ) {
		props.set ( "PrependFile", PrependFile );
	}
	if ( AppendFile.length() > 0 ) {
		props.set ( "AppendFile", AppendFile );
	}
    if ( ContentType.length() > 0 ) {
        props.set ( "ContentType", ContentType );
    }
    if ( AutoFormat.length() > 0 ) {
        props.set ( "AutoFormat", AutoFormat );
    }
    if ( OutputType.length() > 0 ) {
        props.set ( "OutputType", OutputType );
    }
	if ( OutputFile.length() > 0 ) {
		props.set ( "OutputFile", OutputFile );
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
{	String InputFile = __InputFile_JTextField.getText().trim();
	String PrependFile = __PrependFile_JTextField.getText().trim();
	String AppendFile = __AppendFile_JTextField.getText().trim();
	String ContentType = __ContentType_JComboBox.getSelected();
	String AutoFormat = __AutoFormat_JComboBox.getSelected();
	String OutputType = __OutputType_JComboBox.getSelected();
	String OutputFile = __OutputFile_JTextField.getText().trim();
	__command.setCommandParameter ( "InputFile", InputFile );
	__command.setCommandParameter ( "PrependFile", PrependFile );
	__command.setCommandParameter ( "AppendFile", AppendFile );
	__command.setCommandParameter ( "ContentType", ContentType );
	__command.setCommandParameter ( "AutoFormat", AutoFormat );
	__command.setCommandParameter ( "OutputType", OutputType );
	__command.setCommandParameter ( "OutputFile", OutputFile );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param diffProgram visual diff program
*/
private void initialize ( JFrame parent, FormatFile_Command command, String diffProgram )
{	__command = command;
	CommandProcessor processor =__command.getCommandProcessor();
	__diffProgram = diffProgram;
	
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command formats a file, for example to add header/footer, or to wrap the file with content needed as a web page." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Formatting can be done explicitly by providing prepend/append files, or auto-format based on the input and output."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    if ( __working_dir != null ) {
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"It is recommended that file names are specified relative to the working directory, which is:"),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"    " + __working_dir),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input file to format:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputFile_JTextField = new JTextField ( 50 );
	__InputFile_JTextField.setToolTipText("Specify the filename for the input file, can use ${Property} notation");
	__InputFile_JTextField.addKeyListener ( this );
    // Input file layout fights back with other rows so put in its own panel
	JPanel InputFile_JPanel = new JPanel();
	InputFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(InputFile_JPanel, __InputFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseInputFile_JButton = new SimpleJButton ( "...", this );
	__browseInputFile_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(InputFile_JPanel, __browseInputFile_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__pathInputFile_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(InputFile_JPanel, __pathInputFile_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(main_JPanel, InputFile_JPanel,
		1, y, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "File to prepend:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__PrependFile_JTextField = new JTextField ( 50 );
	__PrependFile_JTextField.setToolTipText("Specify the filename for file to prepend, can use ${Property} notation");
	__PrependFile_JTextField.addKeyListener ( this );
    // Input file layout fights back with other rows so put in its own panel
	JPanel PrependFile_JPanel = new JPanel();
	PrependFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(PrependFile_JPanel, __PrependFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browsePrependFile_JButton = new SimpleJButton ( "...", this );
	__browsePrependFile_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(PrependFile_JPanel, __browsePrependFile_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__pathPrependFile_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(PrependFile_JPanel, __pathPrependFile_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(main_JPanel, PrependFile_JPanel,
		1, y, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "File to append:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__AppendFile_JTextField = new JTextField ( 50 );
	__AppendFile_JTextField.setToolTipText("Specify the filename for file to append, can use ${Property} notation");
	__AppendFile_JTextField.addKeyListener ( this );
    // Input file layout fights back with other rows so put in its own panel
	JPanel AppendFile_JPanel = new JPanel();
	AppendFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(AppendFile_JPanel, __AppendFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseAppendFile_JButton = new SimpleJButton ( "...", this );
	__browseAppendFile_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(AppendFile_JPanel, __browseAppendFile_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__pathAppendFile_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(AppendFile_JPanel, __pathAppendFile_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(main_JPanel, AppendFile_JPanel,
		1, y, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Content type:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ContentType_JComboBox = new SimpleJComboBox ( false );
	__ContentType_JComboBox.setToolTipText("General content type, used when auto-formatting.");
	List<String> ignoreChoices = new ArrayList<>();
	ignoreChoices.add ( "" );	// Default
	ignoreChoices.add ( __command._Csv );
	ignoreChoices.add ( __command._Image );
	ignoreChoices.add ( __command._Json );
	ignoreChoices.add ( __command._Text );
	__ContentType_JComboBox.setData(ignoreChoices);
	__ContentType_JComboBox.select ( 0 );
	__ContentType_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __ContentType_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - general content type (default=" + __command._Text + ")"), 
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Auto-format?:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AutoFormat_JComboBox = new SimpleJComboBox ( false );
	__AutoFormat_JComboBox.setToolTipText("Whether to auto-format, ignores prepend and append files.");
    List<String> formatChoices = new ArrayList<>();
    formatChoices.add ( "" );
    formatChoices.add ( __command._False );
    formatChoices.add ( __command._True );
    __AutoFormat_JComboBox.setData(formatChoices);
    __AutoFormat_JComboBox.select ( 0 );
    __AutoFormat_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __AutoFormat_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - autoformat input file? (default=" + __command._False + ")"), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output type:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputType_JComboBox = new SimpleJComboBox ( false );
	__OutputType_JComboBox.setToolTipText("Output type to format.");
    List<String> typeChoices = new ArrayList<>();
    typeChoices.add ( "" );
    typeChoices.add ( __command._Cgi );
    typeChoices.add ( __command._Html );
    typeChoices.add ( __command._Text );
    __OutputType_JComboBox.setData(typeChoices);
    __OutputType_JComboBox.select ( 0 );
    __OutputType_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __OutputType_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - output type (default=" + __command._Text + ")"), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output file:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputFile_JTextField = new JTextField ( 50 );
	__OutputFile_JTextField.setToolTipText("Specify the output filename, can be the same as the input file, can use ${Property} notation");
	__OutputFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel
	JPanel OutputFile_JPanel = new JPanel();
	OutputFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(OutputFile_JPanel, __OutputFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
	__browseOutputFile_JButton = new SimpleJButton ( "...", this );
	__browseOutputFile_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(OutputFile_JPanel, __browseOutputFile_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__pathOutputFile_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(OutputFile_JPanel, __pathOutputFile_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
	JGUIUtil.addComponent(main_JPanel, OutputFile_JPanel,
		1, y, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

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
	button_JPanel.add(__visualDiff_JButton = new SimpleJButton(__VisualDiff, this));
	__visualDiff_JButton.setToolTipText(this.visualDiffLabel);
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");
	
	// Refresh the contents (put after buttons because want to enable/disable...
	refresh ();

	setTitle ( "Edit " + __command.getCommandName() + " command" );

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
{	String routine = "FormatFile_JDialog.refresh";
	String InputFile = "";
	String PrependFile = "";
	String AppendFile = "";
	String ContentType = "";
	String AutoFormat = "";
	String OutputType = "";
	String OutputFile = "";
    PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
		parameters = __command.getCommandParameters();
		InputFile = parameters.getValue ( "InputFile" );
		PrependFile = parameters.getValue ( "PrependFile" );
		AppendFile = parameters.getValue ( "AppendFile" );
		ContentType = parameters.getValue ( "ContentType" );
		AutoFormat = parameters.getValue ( "AutoFormat" );
		OutputType = parameters.getValue ( "OutputType" );
		OutputFile = parameters.getValue ( "OutputFile" );
		if ( InputFile != null ) {
			__InputFile_JTextField.setText ( InputFile );
		}
		if ( PrependFile != null ) {
			__PrependFile_JTextField.setText ( PrependFile );
		}
		if ( AppendFile != null ) {
			__AppendFile_JTextField.setText ( AppendFile );
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__ContentType_JComboBox, ContentType, JGUIUtil.NONE, null, null ) ) {
			__ContentType_JComboBox.select ( ContentType );
		}
		else {
		    if ( (ContentType == null) || ContentType.equals("") ) {
				// New command...select the default...
				__ContentType_JComboBox.select ( 0 );
			}
			else {
			    // Bad user command...
				Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
				"ContentType parameter \"" + ContentType + "\".  Select a\ndifferent value or Cancel." );
			}
		}
        if ( JGUIUtil.isSimpleJComboBoxItem(__AutoFormat_JComboBox, AutoFormat, JGUIUtil.NONE, null, null ) ) {
            __AutoFormat_JComboBox.select ( AutoFormat );
        }
        else {
            if ( (AutoFormat == null) || AutoFormat.equals("") ) {
                // New command...select the default...
                __AutoFormat_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                "AutoFormat parameter \"" + AutoFormat + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(__OutputType_JComboBox, OutputType, JGUIUtil.NONE, null, null ) ) {
            __OutputType_JComboBox.select ( OutputType );
        }
        else {
            if ( (OutputType == null) || OutputType.equals("") ) {
                // New command...select the default...
                __OutputType_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                "OutputType parameter \"" + OutputType + "\".  Select a\ndifferent value or Cancel." );
            }
        }
		if ( OutputFile != null ) {
			__OutputFile_JTextField.setText ( OutputFile );
		}
	}
	// Regardless, reset the command from the fields.  This is only  visible
	// information that has not been committed in the command.
	InputFile = __InputFile_JTextField.getText().trim();
	PrependFile = __PrependFile_JTextField.getText().trim();
	AppendFile = __AppendFile_JTextField.getText().trim();
	ContentType = __ContentType_JComboBox.getSelected();
	AutoFormat = __AutoFormat_JComboBox.getSelected();
	OutputType = __OutputType_JComboBox.getSelected();
	OutputFile = __OutputFile_JTextField.getText().trim();
	PropList props = new PropList ( __command.getCommandName() );
	props.add ( "InputFile=" + InputFile );
	props.add ( "PrependFile=" + PrependFile );
	props.add ( "AppendFile=" + AppendFile );
	props.add ( "ContentType=" + ContentType );
	props.add ( "AutoFormat=" + AutoFormat );
	props.add ( "OutputType=" + OutputType );
	props.add ( "OutputFile=" + OutputFile );
	__command_JTextArea.setText( __command.toString(props) );
	// Check the path and determine what the label on the path button should be...
	if ( __pathInputFile_JButton != null ) {
		if ( (InputFile != null) && !InputFile.isEmpty() ) {
			__pathInputFile_JButton.setEnabled ( true );
			File f = new File ( InputFile );
			if ( f.isAbsolute() ) {
				__pathInputFile_JButton.setText ( __RemoveWorkingDirectory );
				__pathInputFile_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
		    	__pathInputFile_JButton.setText ( __AddWorkingDirectory );
		    	__pathInputFile_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathInputFile_JButton.setEnabled(false);
		}
	}
	// Check the path and determine what the label on the path button should be...
	if ( __pathPrependFile_JButton != null ) {
		if ( (PrependFile != null) && !PrependFile.isEmpty() ) {
			__pathPrependFile_JButton.setEnabled ( true );
			File f = new File ( PrependFile );
			if ( f.isAbsolute() ) {
				__pathPrependFile_JButton.setText ( __RemoveWorkingDirectory );
				__pathPrependFile_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
		    	__pathPrependFile_JButton.setText ( __AddWorkingDirectory );
		    	__pathPrependFile_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathPrependFile_JButton.setEnabled(false);
		}
	}
	// Check the path and determine what the label on the path button should be...
	if ( __pathAppendFile_JButton != null ) {
		if ( (AppendFile != null) && !AppendFile.isEmpty() ) {
			__pathAppendFile_JButton.setEnabled ( true );
			File f = new File ( AppendFile );
			if ( f.isAbsolute() ) {
				__pathAppendFile_JButton.setText ( __RemoveWorkingDirectory );
				__pathAppendFile_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
		    	__pathAppendFile_JButton.setText ( __AddWorkingDirectory );
		    	__pathAppendFile_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathAppendFile_JButton.setEnabled(false);
		}
	}
	// Check the path and determine what the label on the path button should be...
	if ( __pathOutputFile_JButton != null ) {
		if ( (OutputFile != null) && !OutputFile.isEmpty() ) {
			__pathOutputFile_JButton.setEnabled ( true );
			File f = new File ( OutputFile );
			if ( f.isAbsolute() ) {
				__pathOutputFile_JButton.setText ( __RemoveWorkingDirectory );
				__pathOutputFile_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__pathOutputFile_JButton.setText ( __AddWorkingDirectory );
            	__pathOutputFile_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathOutputFile_JButton.setEnabled(false);
		}
	}
	// Disable the Visual Diff button if the program file does not exist or
	// either of the files to compare do not exist
	if ( __visualDiff_JButton != null ) {
		TSCommandProcessor processor = (TSCommandProcessor)__command.getCommandProcessor();
		String file1Path = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(__working_dir,
                TSCommandProcessorUtil.expandParameterValue(processor, __command, __InputFile_JTextField.getText()) ) );
		String file2Path = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(__working_dir,
                TSCommandProcessorUtil.expandParameterValue(processor, __command, __OutputFile_JTextField.getText()) ) );
		if ( IOUtil.fileExists(__diffProgram) && IOUtil.fileExists(file1Path) && IOUtil.fileExists(file2Path) ) {
			__visualDiff_JButton.setEnabled(true);
			__visualDiff_JButton.setToolTipText(this.visualDiffLabel);
		}
		else if ( !IOUtil.fileExists(__diffProgram)) {
			__visualDiff_JButton.setEnabled(false);
			__visualDiff_JButton.setToolTipText(this.visualDiffLabel + " - disabled because diff program not configured.");
		}
		else if ( !IOUtil.fileExists(file1Path) ) {
			__visualDiff_JButton.setEnabled(false);
			__visualDiff_JButton.setToolTipText(this.visualDiffLabel + " - disabled because input file does not exist.");
		}
		else if ( !IOUtil.fileExists(file2Path) ) {
			__visualDiff_JButton.setEnabled(false);
			__visualDiff_JButton.setToolTipText(this.visualDiffLabel + " - disabled because output file does not exist.");
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
