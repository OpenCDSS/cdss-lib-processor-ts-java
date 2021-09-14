// CompareFiles_JDialog - editor for CompareFiles commands

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
import javax.swing.JTabbedPane;
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
public class CompareFiles_JDialog extends JDialog
implements ActionListener, KeyListener, WindowListener
{
private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private final String __VisualDiff = "Visual Diff";
private final String visualDiffLabel = "Run program to visually compare output files (see TSTool 'DiffProgram' configuration property).";

private SimpleJButton __browse1_JButton = null;
private SimpleJButton __browse2_JButton = null;
private SimpleJButton __path1_JButton = null;
private SimpleJButton __path2_JButton = null;
private SimpleJButton __visualDiff_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private JTabbedPane __main_JTabbedPane = null;
private JTextField __InputFile1_JTextField = null;
private JTextField __InputFile2_JTextField = null;
private JTextField __CommentLineChar_JTextField = null;
private SimpleJComboBox __MatchCase_JComboBox = null;
private SimpleJComboBox __IgnoreWhitespace_JComboBox = null;
private JTextField __ExcludeText_JTextField = null;
private JTextField __AllowedDiff_JTextField = null;
private SimpleJComboBox __IfDifferent_JComboBox = null;
private SimpleJComboBox __IfSame_JComboBox = null;

private SimpleJComboBox __FileProperty_JComboBox = null;
private SimpleJComboBox __FilePropertyOperator_JComboBox = null;
private SimpleJComboBox __FilePropertyAction_JComboBox =null;

private JTextArea __command_JTextArea = null;
private String __working_dir = null;
private String __diffProgram = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private CompareFiles_Command __command = null; // Command to edit
private boolean __ok = false; // Indicates whether the user pressed OK to close the dialog.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param diffProgram visual difference program
*/
public CompareFiles_JDialog ( JFrame parent, CompareFiles_Command command, String diffProgram )
{	super(parent, true);
	initialize ( parent, command, diffProgram );
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
				// Convert path to relative path by default.
				try {
					__InputFile1_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1,"CompareFiles_JDialog", "Error converting file to relative path." );
				}
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
				// Convert path to relative path by default.
				try {
					__InputFile2_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1,"CompareFiles_JDialog", "Error converting file to relative path." );
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
		HelpViewer.getInstance().showHelp("command", "CompareFiles");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else if ( o == __path1_JButton ) {
		if ( __path1_JButton.getText().equals(__AddWorkingDirectory) ) {
			__InputFile1_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir,__InputFile1_JTextField.getText() ) );
		}
		else if ( __path1_JButton.getText().equals(__RemoveWorkingDirectory) ) {
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
		if ( __path2_JButton.getText().equals( __AddWorkingDirectory) ) {
			__InputFile2_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir, __InputFile2_JTextField.getText() ) );
		}
		else if ( __path2_JButton.getText().equals(__RemoveWorkingDirectory) ) {
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
	else if ( o == __visualDiff_JButton ) {
		// Run the diff program on the input and output files
		// (they should have existed because the button will have been disabled if not)
		TSCommandProcessor processor = (TSCommandProcessor)__command.getCommandProcessor();
		String file1Path = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(__working_dir,
                TSCommandProcessorUtil.expandParameterValue(processor, __command, __InputFile1_JTextField.getText()) ) );
		String file2Path = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(__working_dir,
                TSCommandProcessorUtil.expandParameterValue(processor, __command, __InputFile2_JTextField.getText()) ) );
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
	    // Choices.
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
	String MatchCase = __MatchCase_JComboBox.getSelected();
	String IgnoreWhitespace = __IgnoreWhitespace_JComboBox.getSelected();
	String ExcludeText = __ExcludeText_JTextField.getText().trim();
	String AllowedDiff = __AllowedDiff_JTextField.getText().trim();
	String IfDifferent = __IfDifferent_JComboBox.getSelected();
	String IfSame = __IfSame_JComboBox.getSelected();
	String FileProperty = __FileProperty_JComboBox.getSelected();
	String FilePropertyOperator = __FilePropertyOperator_JComboBox.getSelected();
	String FilePropertyAction = __FilePropertyAction_JComboBox.getSelected();
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
    if ( MatchCase.length() > 0 ) {
        props.set ( "MatchCase", MatchCase );
    }
    if ( IgnoreWhitespace.length() > 0 ) {
        props.set ( "IgnoreWhitespace", IgnoreWhitespace );
    }
    if ( ExcludeText.length() > 0 ) {
        props.set ( "ExcludeText", ExcludeText );
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
	if ( FileProperty.length() > 0 ) {
		props.set ( "FileProperty", FileProperty );
	}
	if ( FilePropertyOperator.length() > 0 ) {
		props.set ( "FilePropertyOperator", FilePropertyOperator );
	}
	if ( FilePropertyAction.length() > 0 ) {
		props.set ( "FilePropertyAction", FilePropertyAction );
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
	String MatchCase = __MatchCase_JComboBox.getSelected();
	String IgnoreWhitespace = __IgnoreWhitespace_JComboBox.getSelected();
    String ExcludeText = __ExcludeText_JTextField.getText().trim();
	String AllowedDiff = __AllowedDiff_JTextField.getText().trim();
	String IfDifferent = __IfDifferent_JComboBox.getSelected();
	String IfSame = __IfSame_JComboBox.getSelected();
	String FileProperty = __FileProperty_JComboBox.getSelected();
	String FilePropertyOperator = __FilePropertyOperator_JComboBox.getSelected();
	String FilePropertyAction = __FilePropertyAction_JComboBox.getSelected();
	__command.setCommandParameter ( "InputFile1", InputFile1 );
	__command.setCommandParameter ( "InputFile2", InputFile2 );
	__command.setCommandParameter ( "CommentLineChar", CommentLineChar );
	__command.setCommandParameter ( "MatchCase", MatchCase );
	__command.setCommandParameter ( "IgnoreWhitespace", IgnoreWhitespace );
	__command.setCommandParameter ( "ExcludeText", ExcludeText );
	__command.setCommandParameter ( "AllowedDiff", AllowedDiff );
	__command.setCommandParameter ( "IfDifferent", IfDifferent );
	__command.setCommandParameter ( "IfSame", IfSame );
	__command.setCommandParameter ( "FileProperty", FileProperty );
	__command.setCommandParameter ( "FilePropertyOperator", FilePropertyOperator );
	__command.setCommandParameter ( "FilePropertyAction", FilePropertyAction );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param diffProgram visual diff program
*/
private void initialize ( JFrame parent, CompareFiles_Command command, String diffProgram )
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
		"This command compares files and is used for automated testing." ),
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

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "First file to compare:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputFile1_JTextField = new JTextField ( 50 );
	__InputFile1_JTextField.setToolTipText("Name of the first file, can use ${Property} notation");
	__InputFile1_JTextField.addKeyListener ( this );
    // Input file layout fights back with other rows so put in its own panel
	JPanel InputFile1_JPanel = new JPanel();
	InputFile1_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(InputFile1_JPanel, __InputFile1_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse1_JButton = new SimpleJButton ( "...", this );
	__browse1_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(InputFile1_JPanel, __browse1_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__path1_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(InputFile1_JPanel, __path1_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(main_JPanel, InputFile1_JPanel,
		1, y, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Second file to compare:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputFile2_JTextField = new JTextField ( 50 );
	__InputFile2_JTextField.setToolTipText("Name of the second file, can use ${Property} notation");
	__InputFile2_JTextField.addKeyListener ( this );
    // Input file layout fights back with other rows so put in its own panel
	JPanel InputFile2_JPanel = new JPanel();
	InputFile2_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(InputFile2_JPanel, __InputFile2_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse2_JButton = new SimpleJButton ( "...", this );
	__browse2_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(InputFile2_JPanel, __browse2_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__path2_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(InputFile2_JPanel, __path2_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(main_JPanel, InputFile2_JPanel,
		1, y, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for file contents comparison
    int yContent = -1;
    JPanel content_JPanel = new JPanel();
    content_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Compare File Contents", content_JPanel );

    JGUIUtil.addComponent(content_JPanel, new JLabel (
		"Text files are compared line by line and character by character within lines.  Comment lines are ignored." ),
		0, ++yContent, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(content_JPanel, new JLabel ( "A line by line comparison is made, alternating between files and skipping comments."),
		0, ++yContent, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(content_JPanel, new JLabel ( "The comparison does not look forward or back to match lines."),
		0, ++yContent, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(content_JPanel, new JLabel ( "Use the visual difference program to review differences."),
		0, ++yContent, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(content_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yContent, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(content_JPanel, new JLabel ( "Comment line character:"),
        0, ++yContent, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CommentLineChar_JTextField = new JTextField ( 20 );
    __CommentLineChar_JTextField.setToolTipText("For example: #");
    __CommentLineChar_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(content_JPanel, __CommentLineChar_JTextField,
        1, yContent, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(content_JPanel, new JLabel( "Optional - must be first char on line (default=#)"), 
        3, yContent, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(content_JPanel, new JLabel ( "Match case:"),
        0, ++yContent, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MatchCase_JComboBox = new SimpleJComboBox ( false );
    List<String> matchChoices = new ArrayList<>();
    matchChoices.add ( "" );
    matchChoices.add ( __command._False );
    matchChoices.add ( __command._True );
    __MatchCase_JComboBox.setData(matchChoices);
    __MatchCase_JComboBox.select ( 0 );
    __MatchCase_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(content_JPanel, __MatchCase_JComboBox,
        1, yContent, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(content_JPanel, new JLabel(
        "Optional - match case (default=" + __command._True + ")"), 
        3, yContent, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(content_JPanel, new JLabel ( "Ignore whitespace:"),
		0, ++yContent, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IgnoreWhitespace_JComboBox = new SimpleJComboBox ( false );
	List<String> ignoreChoices = new ArrayList<>();
	ignoreChoices.add ( "" );	// Default
	ignoreChoices.add ( __command._False );
	ignoreChoices.add ( __command._True );
	__IgnoreWhitespace_JComboBox.setData(ignoreChoices);
	__IgnoreWhitespace_JComboBox.select ( 0 );
	__IgnoreWhitespace_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(content_JPanel, __IgnoreWhitespace_JComboBox,
		1, yContent, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(content_JPanel, new JLabel(
		"Optional - ignore whitespace at ends of lines (default=" + __command._False + ")"), 
		3, yContent, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(content_JPanel, new JLabel ( "Exclude text:"),
        0, ++yContent, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ExcludeText_JTextField = new JTextField ( 20 );
    __ExcludeText_JTextField.setToolTipText("Pattern(s) to exclude lines separated by commas, use * for wildcard and | for multiple 'or' in one patterns.");
    __ExcludeText_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(content_JPanel, __ExcludeText_JTextField,
        1, yContent, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(content_JPanel, new JLabel(
        "Optional - exclude lines matching regular expression (default=include all)"), 
        3, yContent, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(content_JPanel, new JLabel ( "Allowed # of different lines:"),
        0, ++yContent, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AllowedDiff_JTextField = new JTextField ( 5 );
    __AllowedDiff_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(content_JPanel, __AllowedDiff_JTextField,
        1, yContent, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(content_JPanel, new JLabel( "Optional - when checking for differences (default=0)"), 
        3, yContent, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(content_JPanel, new JLabel ( "Action if different:"),
		0, ++yContent, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IfDifferent_JComboBox = new SimpleJComboBox ( false );
	List<String> diffChoices = new ArrayList<>();
	diffChoices.add ( "" );	// Default
	diffChoices.add ( __command._Ignore );
	diffChoices.add ( __command._Warn );
	diffChoices.add ( __command._Fail );
	__IfDifferent_JComboBox.setData(diffChoices);
	__IfDifferent_JComboBox.select ( 0 );
	__IfDifferent_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(content_JPanel, __IfDifferent_JComboBox,
		1, yContent, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(content_JPanel, new JLabel(
		"Optional - action if files are different (default=" + __command._Ignore + ")"), 
		3, yContent, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(content_JPanel, new JLabel ( "Action if same:"),
		0, ++yContent, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IfSame_JComboBox = new SimpleJComboBox ( false );
	List<String> sameChoices = new ArrayList<>();
	sameChoices.add ( "" );	// Default
	sameChoices.add ( __command._Ignore );
	sameChoices.add ( __command._Warn );
	sameChoices.add ( __command._Fail );
	__IfSame_JComboBox.setData(sameChoices);
	__IfSame_JComboBox.select ( 0 );
	__IfSame_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(content_JPanel, __IfSame_JComboBox,
		1, yContent, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(content_JPanel, new JLabel(
		"Optional - action if files are the same (default=" + __command._Ignore + ")"), 
		3, yContent, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for file property comparison
    int yProp = -1;
    JPanel prop_JPanel = new JPanel();
    prop_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Compare File Properties", prop_JPanel );

    JGUIUtil.addComponent(prop_JPanel, new JLabel (
		"File properties can be compared." ),
		0, ++yProp, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel (
		"For example, this can be used to ensure that a file is an expected size or one file is newer than another file." ),
		0, ++yProp, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel (
		"The comparison is evaluated as follows:"),
		0, ++yProp, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel (
		"    If.... File1Property Operator File2Property ... then action"),
		0, ++yProp, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel (
		"For example:"),
		0, ++yProp, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel (
		"    If.... File1ModificationTime > File2ModificationTime ... then Warn"),
		0, ++yProp, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(prop_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yProp, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(prop_JPanel, new JLabel ( "File property:"),
		0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__FileProperty_JComboBox = new SimpleJComboBox ( false );
	List<String> propChoices = new ArrayList<>();
	propChoices.add ( "" );	// Default
	propChoices.add ( __command._ModificationTime );
	propChoices.add ( __command._Size );
	__FileProperty_JComboBox.setData(propChoices);
	__FileProperty_JComboBox.select ( 0 );
	__FileProperty_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(prop_JPanel, __FileProperty_JComboBox,
		1, yProp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel(
		"Required (for property comparison) - property"), 
		3, yProp, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(prop_JPanel, new JLabel ( "Property comparison operator:"),
		0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__FilePropertyOperator_JComboBox = new SimpleJComboBox ( false );
	List<String> opChoices = new ArrayList<>();
	opChoices.add ( "" ); // Default
	opChoices.add ( "<" );
	opChoices.add ( "<=" );
	opChoices.add ( "=" );
	opChoices.add ( ">" );
	opChoices.add ( ">=" );
	opChoices.add ( "!=" );
	__FilePropertyOperator_JComboBox.setData(opChoices);
	__FilePropertyOperator_JComboBox.select ( 0 );
	__FilePropertyOperator_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(prop_JPanel, __FilePropertyOperator_JComboBox,
		1, yProp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel(
		"Required (for property comparison) - how to compare"), 
		3, yProp, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(prop_JPanel, new JLabel ( "Action if met:"),
		0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__FilePropertyAction_JComboBox = new SimpleJComboBox ( false );
	List<String> actionChoices = new ArrayList<>();
	actionChoices.add ( "" );	// Default
	actionChoices.add ( __command._Warn );
	actionChoices.add ( __command._Fail );
	__FilePropertyAction_JComboBox.setData(actionChoices);
	__FilePropertyAction_JComboBox.select ( 0 );
	__FilePropertyAction_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(prop_JPanel, __FilePropertyAction_JComboBox,
		1, yProp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel(
		"Optional - action if condition is met (default=" + __command._Warn + ")"), 
		3, yProp, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

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
{	String routine = "CompareFiles_JDialog.refresh";
	String InputFile1 = "";
	String InputFile2 = "";
	String CommentLineChar = "";
	String MatchCase = "";
	String IgnoreWhitespace = "";
	String ExcludeText = "";
	String AllowedDiff = "";
	String IfDifferent = "";
	String IfSame = "";
	String FileProperty = "";
	String FilePropertyOperator = "";
	String FilePropertyAction = "";
    PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
		parameters = __command.getCommandParameters();
		InputFile1 = parameters.getValue ( "InputFile1" );
		InputFile2 = parameters.getValue ( "InputFile2" );
		CommentLineChar = parameters.getValue ( "CommentLineChar" );
		MatchCase = parameters.getValue ( "MatchCase" );
		IgnoreWhitespace = parameters.getValue ( "IgnoreWhitespace" );
		ExcludeText = parameters.getValue ( "ExcludeText" );
		AllowedDiff = parameters.getValue ( "AllowedDiff" );
		IfDifferent = parameters.getValue ( "IfDifferent" );
		IfSame = parameters.getValue ( "IfSame" );
		FileProperty = parameters.getValue ( "FileProperty" );
		FilePropertyOperator = parameters.getValue ( "FilePropertyOperator" );
		FilePropertyAction = parameters.getValue ( "FilePropertyAction" );
		if ( InputFile1 != null ) {
			__InputFile1_JTextField.setText ( InputFile1 );
		}
		if ( InputFile2 != null ) {
			__InputFile2_JTextField.setText ( InputFile2 );
		}
        if ( CommentLineChar != null ) {
            __CommentLineChar_JTextField.setText ( CommentLineChar );
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(__MatchCase_JComboBox, MatchCase, JGUIUtil.NONE, null, null ) ) {
            __MatchCase_JComboBox.select ( MatchCase );
        }
        else {
            if ( (MatchCase == null) || MatchCase.equals("") ) {
                // New command...select the default...
                __MatchCase_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                "MatchCase parameter \"" + MatchCase + "\".  Select a\ndifferent value or Cancel." );
            }
        }
		if ( JGUIUtil.isSimpleJComboBoxItem(__IgnoreWhitespace_JComboBox, IgnoreWhitespace, JGUIUtil.NONE, null, null ) ) {
			__IgnoreWhitespace_JComboBox.select ( IgnoreWhitespace );
		}
		else {
		    if ( (IgnoreWhitespace == null) || IgnoreWhitespace.equals("") ) {
				// New command...select the default...
				__IgnoreWhitespace_JComboBox.select ( 0 );
			}
			else {
			    // Bad user command...
				Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
				"IgnoreWhitespace parameter \"" + IgnoreWhitespace + "\".  Select a\ndifferent value or Cancel." );
			}
		}
        if ( ExcludeText != null ) {
            __ExcludeText_JTextField.setText ( ExcludeText );
        }
        if ( AllowedDiff != null ) {
            __AllowedDiff_JTextField.setText ( AllowedDiff );
        }
		if ( JGUIUtil.isSimpleJComboBoxItem(__IfDifferent_JComboBox, IfDifferent, JGUIUtil.NONE, null, null ) ) {
			__IfDifferent_JComboBox.select ( IfDifferent );
			__main_JTabbedPane.setSelectedIndex(0);
		}
		else {
		    if ( (IfDifferent == null) || IfDifferent.equals("") ) {
				// New command...select the default...
				__IfDifferent_JComboBox.select ( 0 );
			}
			else {
			    // Bad user command...
				Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
				"IfDifferent parameter \"" + IfDifferent + "\".  Select a\ndifferent value or Cancel." );
			}
		}
		if ( JGUIUtil.isSimpleJComboBoxItem( __IfSame_JComboBox, IfSame, JGUIUtil.NONE, null, null ) ) {
			__IfSame_JComboBox.select ( IfSame );
			__main_JTabbedPane.setSelectedIndex(0);
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
		if ( JGUIUtil.isSimpleJComboBoxItem( __FileProperty_JComboBox, FileProperty, JGUIUtil.NONE, null, null ) ) {
			__FileProperty_JComboBox.select ( FileProperty );
			if ( (FileProperty != null) && !FileProperty.isEmpty() ) {
				__main_JTabbedPane.setSelectedIndex(1);
			}
		}
		else {
			if ( (FileProperty == null) || FileProperty.equals("") ) {
				// New command...select the default...
				__FileProperty_JComboBox.select ( 0 );
			}
			else {
				// Bad user command...
				Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
				"FileProperty parameter \"" + FileProperty + "\".  Select a\ndifferent value or Cancel." );
			}
		}
		if ( JGUIUtil.isSimpleJComboBoxItem( __FilePropertyOperator_JComboBox, FilePropertyOperator, JGUIUtil.NONE, null, null ) ) {
			__FilePropertyOperator_JComboBox.select ( FilePropertyOperator );
		}
		else {
			if ( (FilePropertyOperator == null) || FilePropertyOperator.equals("") ) {
				// New command...select the default...
				__FilePropertyOperator_JComboBox.select ( 0 );
			}
			else {
				// Bad user command...
				Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
				"FilePropertyOperator parameter \"" + FilePropertyOperator + "\".  Select a\ndifferent value or Cancel." );
			}
		}
		if ( JGUIUtil.isSimpleJComboBoxItem( __FilePropertyAction_JComboBox, FilePropertyAction, JGUIUtil.NONE, null, null ) ) {
			__FilePropertyAction_JComboBox.select ( FilePropertyAction );
		}
		else {
			if ( (FilePropertyAction == null) || FilePropertyAction.equals("") ) {
				// New command...select the default...
				__FilePropertyAction_JComboBox.select ( 0 );
			}
			else {
				// Bad user command...
				Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
				"FilePropertyAction parameter \"" + FilePropertyAction + "\".  Select a\ndifferent value or Cancel." );
			}
		}
	}
	// Regardless, reset the command from the fields.  This is only  visible
	// information that has not been committed in the command.
	InputFile1 = __InputFile1_JTextField.getText().trim();
	InputFile2 = __InputFile2_JTextField.getText().trim();
	CommentLineChar = __CommentLineChar_JTextField.getText().trim();
	MatchCase = __MatchCase_JComboBox.getSelected();
	IgnoreWhitespace = __IgnoreWhitespace_JComboBox.getSelected();
	ExcludeText = __ExcludeText_JTextField.getText().trim();
	AllowedDiff = __AllowedDiff_JTextField.getText().trim();
	IfDifferent = __IfDifferent_JComboBox.getSelected();
	IfSame = __IfSame_JComboBox.getSelected();
	FileProperty = __FileProperty_JComboBox.getSelected();
	FilePropertyOperator = __FilePropertyOperator_JComboBox.getSelected();
	FilePropertyAction = __FilePropertyAction_JComboBox.getSelected();
	PropList props = new PropList ( __command.getCommandName() );
	props.add ( "InputFile1=" + InputFile1 );
	props.add ( "InputFile2=" + InputFile2 );
	props.add ( "CommentLineChar=" + CommentLineChar );
	props.add ( "MatchCase=" + MatchCase );
	props.add ( "IgnoreWhitespace=" + IgnoreWhitespace );
	props.add ( "ExcludeText=" + ExcludeText );
	props.add ( "AllowedDiff=" + AllowedDiff );
	props.add ( "IfDifferent=" + IfDifferent );
	props.add ( "IfSame=" + IfSame );
	props.add ( "FileProperty=" + FileProperty );
	// Use the following to handle '=' in parameters.
	props.set ( "FilePropertyOperator" , FilePropertyOperator );
	props.add ( "FilePropertyAction=" + FilePropertyAction );
	__command_JTextArea.setText( __command.toString(props) );
	// Check the path and determine what the label on the path button should be...
	if ( __path1_JButton != null ) {
		if ( (InputFile1 != null) && !InputFile1.isEmpty() ) {
			__path1_JButton.setEnabled ( true );
			File f = new File ( InputFile1 );
			if ( f.isAbsolute() ) {
				__path1_JButton.setText ( __RemoveWorkingDirectory );
				__path1_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
		    	__path1_JButton.setText ( __AddWorkingDirectory );
		    	__path1_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__path1_JButton.setEnabled(false);
		}
	}
	if ( __path2_JButton != null ) {
		if ( (InputFile2 != null) && !InputFile2.isEmpty() ) {
			__path2_JButton.setEnabled ( true );
			File f = new File ( InputFile2 );
			if ( f.isAbsolute() ) {
				__path2_JButton.setText ( __RemoveWorkingDirectory );
				__path2_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
		    	__path2_JButton.setText ( __AddWorkingDirectory );
		    	__path2_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__path2_JButton.setEnabled(false);
		}
	}
	// Disable the Visual Diff button if the program file does not exist or
	// either of the files to compare do not exist
	if ( __visualDiff_JButton != null ) {
		TSCommandProcessor processor = (TSCommandProcessor)__command.getCommandProcessor();
		String file1Path = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(__working_dir,
                TSCommandProcessorUtil.expandParameterValue(processor, __command, __InputFile1_JTextField.getText()) ) );
		String file2Path = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(__working_dir,
                TSCommandProcessorUtil.expandParameterValue(processor, __command, __InputFile2_JTextField.getText()) ) );
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
			__visualDiff_JButton.setToolTipText(this.visualDiffLabel + " - disabled because first file does not exist.");
		}
		else if ( !IOUtil.fileExists(file2Path) ) {
			__visualDiff_JButton.setEnabled(false);
			__visualDiff_JButton.setToolTipText(this.visualDiffLabel + " - disabled because second file does not exist.");
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
