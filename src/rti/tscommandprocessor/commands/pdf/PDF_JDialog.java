// PDF_JDialog - editor for PDF command

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2024 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.pdf;

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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
public class PDF_JDialog extends JDialog
implements ActionListener, ChangeListener, KeyListener, WindowListener
{
private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __browseInput_JButton = null;
private SimpleJButton __pathInput_JButton = null;
private SimpleJButton __clearInput_JButton = null;
private SimpleJButton __browseOutput_JButton = null;
private SimpleJButton __pathOutput_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private JTabbedPane __main_JTabbedPane = null;
private SimpleJComboBox __PDFCommand_JComboBox = null;
private JTextField __MergeInputFiles_JTextField = null;
private JTextField __MergeOutputFile_JTextField = null;
private SimpleJComboBox __IfNotFound_JComboBox =null;
private JTextArea __command_JTextArea = null;
private String __working_dir = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private PDF_Command __command = null;
private boolean __ok = false; // Whether the user has pressed OK to close the dialog.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public PDF_JDialog ( JFrame parent, PDF_Command command ) {
	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event ) {
	Object o = event.getSource();

    if ( o == this.__PDFCommand_JComboBox ) {
    	setTabForPDFCommand();
    	refresh();
    }
    else if ( o == __browseInput_JButton ) {
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
		}
		else {
		    fc = JFileChooserFactory.createJFileChooser(__working_dir );
		}
		fc.setDialogTitle( "Select Input File");

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
					if ( __MergeInputFiles_JTextField.getText().isEmpty() ) {
						// Set.
						__MergeInputFiles_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
					}
					else {
						// Append.
						__MergeInputFiles_JTextField.setText(__MergeInputFiles_JTextField.getText() + "," + IOUtil.toRelativePath(__working_dir, path));
					}
				}
				catch ( Exception e ) {
					Message.printWarning ( 1, "PDF", "Error converting file to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory(directory);
				refresh();
			}
		}
	}
	else if ( o == __browseOutput_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser(__working_dir );
        }
        fc.setDialogTitle( "Select Output File");

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String directory = fc.getSelectedFile().getParent();
            String filename = fc.getSelectedFile().getName();
            String path = fc.getSelectedFile().getPath();

            if (filename == null || filename.equals("")) {
                return;
            }

            if (path != null) {
				// Convert path to relative path by default.
				try {
					__MergeOutputFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1, "PDF", "Error converting file to relative path." );
				}
                JGUIUtil.setLastFileDialogDirectory(directory);
                refresh();
            }
        }
    }
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __clearInput_JButton ) {
		__MergeInputFiles_JTextField.setText("");
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "PDF");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else if ( o == __pathInput_JButton ) {
		String inputFiles = __MergeInputFiles_JTextField.getText();
		if ( __pathInput_JButton.getText().equals(__AddWorkingDirectory) ) {
			if ( inputFiles.contains(",") ) {
				// Split.
				String [] parts = inputFiles.split(",");
				StringBuilder b = new StringBuilder();
				for ( String part : parts ) {
					if ( b.length() > 0 ) {
						b.append(",");
					}
					b.append (IOUtil.verifyPathForOS(IOUtil.toAbsolutePath(__working_dir,part ), true) );
				}
				__MergeInputFiles_JTextField.setText (b.toString());
			}
			else {
				// Single file.
				__MergeInputFiles_JTextField.setText (IOUtil.verifyPathForOS(
					IOUtil.toAbsolutePath(__working_dir,__MergeInputFiles_JTextField.getText() ), true) );
			}
		}
		else if ( __pathInput_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
				if ( inputFiles.contains(",") ) {
					// Split.
					String [] parts = inputFiles.split(",");
					StringBuilder b = new StringBuilder();
					for ( String part : parts ) {
						if ( b.length() > 0 ) {
							b.append(",");
						}
						b.append (IOUtil.verifyPathForOS(IOUtil.toRelativePath(__working_dir,part ), true) );
					}
					__MergeInputFiles_JTextField.setText (b.toString());
				}
				else {
					// Single file.
					__MergeInputFiles_JTextField.setText ( IOUtil.verifyPathForOS(
						IOUtil.toRelativePath ( __working_dir, __MergeInputFiles_JTextField.getText() ), true) );
				}
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,"PDF_JDialog", "Error converting input file name to relative path." );
			}
		}
		refresh ();
	}
    else if ( o == __pathOutput_JButton ) {
        if ( __pathOutput_JButton.getText().equals(__AddWorkingDirectory) ) {
            __MergeOutputFile_JTextField.setText (IOUtil.toAbsolutePath(__working_dir,__MergeOutputFile_JTextField.getText() ) );
        }
        else if ( __pathOutput_JButton.getText().equals(__RemoveWorkingDirectory) ) {
            try {
                __MergeOutputFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,
                        __MergeOutputFile_JTextField.getText() ) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1,"PDF_JDialog", "Error converting output file name to relative path." );
            }
        }
        refresh ();
    }
	else {
		// Choices.
		refresh();
	}
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag to true.
This should be called before response() is allowed to complete.
*/
private void checkInput () {
	// Put together a list of parameters to check.
	PropList props = new PropList ( "" );
	String PDFCommand = __PDFCommand_JComboBox.getSelected();
	String MergeInputFiles = __MergeInputFiles_JTextField.getText().trim();
	String MergeOutputFile = __MergeOutputFile_JTextField.getText().trim();
	String IfNotFound = __IfNotFound_JComboBox.getSelected();
	__error_wait = false;
	if ( (PDFCommand != null) && !PDFCommand.isEmpty() ) {
		props.set ( "PDFCommand", PDFCommand );
	}
	if ( MergeInputFiles.length() > 0 ) {
		props.set ( "MergeInputFiles", MergeInputFiles );
	}
    if ( MergeOutputFile.length() > 0 ) {
        props.set ( "MergeOutputFile", MergeOutputFile );
    }
	if ( IfNotFound.length() > 0 ) {
		props.set ( "IfNotFound", IfNotFound );
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
Commit the edits to the command.  In this case the command parameters have already been checked and no errors were detected.
*/
private void commitEdits () {
	String PDFCommand = __PDFCommand_JComboBox.getSelected();
	String MergeInputFiles = __MergeInputFiles_JTextField.getText().trim();
    String MergeOutputFile = __MergeOutputFile_JTextField.getText().trim();
	String IfNotFound = __IfNotFound_JComboBox.getSelected();
	__command.setCommandParameter ( "PDFCommand", PDFCommand );
	__command.setCommandParameter ( "MergeInputFiles", MergeInputFiles );
	__command.setCommandParameter ( "MergeOutputFile", MergeOutputFile );
	__command.setCommandParameter ( "IfNotFound", IfNotFound );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, PDF_Command command ) {
	__command = command;
	CommandProcessor processor =__command.getCommandProcessor();

	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel.

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

	// Seems to be working with Java 11 TSTool and PDFBox 3.0.6 library.
	/*
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"<html><b>This command is under development and may not be functional as documented.</b></html>"),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"<html><b>If the TSTool interface command editor does not work, try loading or editing a command other than PDF, and then use PDF command.</b></html>"),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
		*/
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command manipulates PDF files using the Apache Foundation's PDFBox software package." ),
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

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "PDF command:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__PDFCommand_JComboBox = new SimpleJComboBox ( false );
	__PDFCommand_JComboBox.setToolTipText("PDF command to execute.");
	List<String> commandChoices = PDFCommandType.getChoicesAsStrings(false);
	__PDFCommand_JComboBox.setData(commandChoices);
	__PDFCommand_JComboBox.select ( 0 );
	__PDFCommand_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __PDFCommand_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - PDF command to run (see tabs below)."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __main_JTabbedPane = new JTabbedPane ();
    __main_JTabbedPane.addChangeListener(this);
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for 'Merge' parameters.
    int yMergeFiles = -1;
    JPanel mergeFiles_JPanel = new JPanel();
    mergeFiles_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Merge", mergeFiles_JPanel );

    JGUIUtil.addComponent(mergeFiles_JPanel, new JLabel (
		"Merge the contents of multiple PDF files into an output PDF file." ),
		0, ++yMergeFiles, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(mergeFiles_JPanel, new JLabel (
        "The input file can be a single file, all files in a folder (*), all files matching an extension (*.pdf), or a list of file patterns separated by commas." ),
        0, ++yMergeFiles, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(mergeFiles_JPanel, new JLabel (
        "Files that do not end in 'pdf' will be removed from the merge list."),
        0, ++yMergeFiles, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(mergeFiles_JPanel, new JLabel (
        "The order of files will be as specified with 'MergeInputFiles', and wildcard results are sorted alphabetically."),
        0, ++yMergeFiles, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(mergeFiles_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yMergeFiles, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Input file(s):" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__MergeInputFiles_JTextField = new JTextField ( 50 );
	__MergeInputFiles_JTextField.setToolTipText("Specify the input file(s) using a single file, *, or *.pdf pattern, can use ${Property} notation");
	__MergeInputFiles_JTextField.addKeyListener ( this );
    // Input file layout fights back with other rows so put in its own panel.
	JPanel MergeInputFiles_JPanel = new JPanel();
	MergeInputFiles_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(MergeInputFiles_JPanel, __MergeInputFiles_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseInput_JButton = new SimpleJButton ( "...", this );
	__browseInput_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(MergeInputFiles_JPanel, __browseInput_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__pathInput_JButton = new SimpleJButton(__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(MergeInputFiles_JPanel, __pathInput_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	__clearInput_JButton = new SimpleJButton ( "Clear", this );
	__clearInput_JButton.setToolTipText("Clear input files");
    JGUIUtil.addComponent(MergeInputFiles_JPanel, __clearInput_JButton,
		3, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	JGUIUtil.addComponent(main_JPanel, MergeInputFiles_JPanel,
		1, y, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Output file:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MergeOutputFile_JTextField = new JTextField ( 50 );
	__MergeOutputFile_JTextField.setToolTipText("Specify the output file, can be the same as an input file, can use ${Property} notation");
    __MergeOutputFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel.
	JPanel MergeOutputFile_JPanel = new JPanel();
	MergeOutputFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(MergeOutputFile_JPanel, __MergeOutputFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseOutput_JButton = new SimpleJButton ( "...", this );
	__browseOutput_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(MergeOutputFile_JPanel, __browseOutput_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__pathOutput_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(MergeOutputFile_JPanel, __pathOutput_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(main_JPanel, MergeOutputFile_JPanel,
		1, y, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

   JGUIUtil.addComponent(main_JPanel, new JLabel ( "If not found?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IfNotFound_JComboBox = new SimpleJComboBox ( false );
	List<String> ifNotFoundChoices = new ArrayList<>();
	ifNotFoundChoices.add ( "" ); // Default.
	ifNotFoundChoices.add ( __command._Ignore );
	ifNotFoundChoices.add ( __command._Warn );
	ifNotFoundChoices.add ( __command._Fail );
	__IfNotFound_JComboBox.setData(ifNotFoundChoices);
	__IfNotFound_JComboBox.select ( 0 );
	__IfNotFound_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(main_JPanel, __IfNotFound_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - action if an input file is not found (default=" + __command._Warn + ")."),
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

	// Panel for buttons.
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

	// Refresh the contents.
    refresh ();

    pack();
    JGUIUtil.center( this );
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
	}
}

public void keyReleased ( KeyEvent event ) {
	refresh();
}

public void keyTyped ( KeyEvent event ) {
}

/**
Indicate if the user pressed OK (cancel otherwise).
*/
public boolean ok () {
	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh () {
	String routine = getClass().getSimpleName() + ".refresh";
	String PDFCommand = "";
	String MergeInputFiles = "";
	String MergeOutputFile = "";
	String IfNotFound = "";
    PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
        parameters = __command.getCommandParameters();
		PDFCommand = parameters.getValue ( "PDFCommand" );
		MergeInputFiles = parameters.getValue ( "MergeInputFiles" );
		MergeOutputFile = parameters.getValue ( "MergeOutputFile" );
		IfNotFound = parameters.getValue ( "IfNotFound" );
		if ( JGUIUtil.isSimpleJComboBoxItem(__PDFCommand_JComboBox, PDFCommand,JGUIUtil.NONE, null, null ) ) {
			__PDFCommand_JComboBox.select ( PDFCommand );
		}
		else {
            if ( (PDFCommand == null) || PDFCommand.equals("") ) {
				// New command...select the default.
				__PDFCommand_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"PDFCommand parameter \"" +	PDFCommand +
				"\".  Select a\n value or Cancel." );
			}
		}
		if ( MergeInputFiles != null ) {
			__MergeInputFiles_JTextField.setText ( MergeInputFiles );
		}
        if ( MergeOutputFile != null ) {
            __MergeOutputFile_JTextField.setText ( MergeOutputFile );
        }
		if ( JGUIUtil.isSimpleJComboBoxItem(__IfNotFound_JComboBox, IfNotFound,JGUIUtil.NONE, null, null ) ) {
			__IfNotFound_JComboBox.select ( IfNotFound );
		}
		else {
            if ( (IfNotFound == null) || IfNotFound.equals("") ) {
				// New command...select the default.
				__IfNotFound_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"IfNotFound parameter \"" +	IfNotFound +
				"\".  Select a\n value or Cancel." );
			}
		}
	}
	// Regardless, reset the command from the fields.
	// This is only  visible information that has not been committed in the command.
	PDFCommand = __PDFCommand_JComboBox.getSelected();
	MergeInputFiles = __MergeInputFiles_JTextField.getText().trim();
	MergeOutputFile = __MergeOutputFile_JTextField.getText().trim();
	IfNotFound = __IfNotFound_JComboBox.getSelected();
	PropList props = new PropList ( __command.getCommandName() );
	props.add ( "PDFCommand=" + PDFCommand );
	props.add ( "MergeInputFiles=" + MergeInputFiles );
	props.add ( "MergeOutputFile=" + MergeOutputFile );
	props.add ( "IfNotFound=" + IfNotFound );
	__command_JTextArea.setText( __command.toString(props).trim() );
	// Check the path and determine what the label on the path button should be.
	if ( __pathInput_JButton != null ) {
		if ( (MergeInputFiles != null) && !MergeInputFiles.isEmpty() ) {
			__pathInput_JButton.setEnabled ( true );
			File f = new File ( MergeInputFiles );
			if ( f.isAbsolute() ) {
				__pathInput_JButton.setText ( __RemoveWorkingDirectory );
				__pathInput_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
		    	__pathInput_JButton.setText ( __AddWorkingDirectory );
		    	__pathInput_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathInput_JButton.setEnabled(false);
		}
	}
    if ( __pathOutput_JButton != null ) {
		if ( (MergeOutputFile != null) && !MergeOutputFile.isEmpty() ) {
			__pathOutput_JButton.setEnabled ( true );
			File f = new File ( MergeOutputFile );
			if ( f.isAbsolute() ) {
				__pathOutput_JButton.setText ( __RemoveWorkingDirectory );
				__pathOutput_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__pathOutput_JButton.setText ( __AddWorkingDirectory );
            	__pathOutput_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathOutput_JButton.setEnabled(false);
		}
    }
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
*/
public void response ( boolean ok ) {
	__ok = ok;
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
 * Set the parameter tab based on the selected command.
 */
private void setTabForPDFCommand() {
	String command = __PDFCommand_JComboBox.getSelected();
	if ( command.equalsIgnoreCase("" + PDFCommandType.MERGE_FILES) ) {
		__main_JTabbedPane.setSelectedIndex(0);
	}
}

/**
 * Handle JTabbedPane changes.
 */
public void stateChanged ( ChangeEvent event ) {
	//JTabbedPane sourceTabbedPane = (JTabbedPane)event.getSource();
	//int index = sourceTabbedPane.getSelectedIndex();
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