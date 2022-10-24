// CopyFile_JDialog - editor for CopyFile command

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2022 Colorado Department of Natural Resources

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
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

@SuppressWarnings("serial")
public class CopyFile_JDialog extends JDialog
implements ActionListener, KeyListener, WindowListener
{
private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __browseInput_JButton = null;
private SimpleJButton __pathInput_JButton = null;
private SimpleJButton __browseOutput_JButton = null;
private SimpleJButton __pathOutput_JButton = null;
private SimpleJButton __browseTempFolder_JButton = null;
private SimpleJButton __pathTempFolder_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private JTabbedPane __main_JTabbedPane = null;
private JTextField __InputFile_JTextField = null;
private JTextField __OutputFile_JTextField = null;
private JTextField __TempFolder_JTextField = null;
private JTextField __TempFilePrefix_JTextField = null;
private JTextField __TempFileSuffix_JTextField = null;
private JTextField __TempFileProperty_JTextField = null;
private SimpleJComboBox __IfInputNotFound_JComboBox =null;
private JTextArea __command_JTextArea = null;
private String __working_dir = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private CopyFile_Command __command = null;
private boolean __ok = false; // Whether the user has pressed OK to close the dialog.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public CopyFile_JDialog ( JFrame parent, CopyFile_Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();
	String routine = "Copy_JDialog";

	if ( o == __browseInput_JButton ) {
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
    else if ( o == __browseTempFolder_JButton ) {
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
		}
		else {
		    fc = JFileChooserFactory.createJFileChooser(__working_dir );
		}
		fc.setFileSelectionMode (JFileChooser.DIRECTORIES_ONLY );
		fc.setDialogTitle( "Select Temporary File Folder");
		
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			String folder = fc.getSelectedFile().getName(); 
			String path = fc.getSelectedFile().getPath(); 
	
			if (folder == null || folder.equals("")) {
				return;
			}
	
			if (path != null) {
				// Convert path to relative path by default.
				try {
					if ( __TempFolder_JTextField.getText().trim().length() == 0 ) {
						// Set the value.
						__TempFolder_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
					}
					else {
						// Append to the existing folder list with comma delimiter.
						__TempFolder_JTextField.setText(__TempFolder_JTextField.getText() + "," + IOUtil.toRelativePath(__working_dir, path));
					}
				}
				catch ( Exception e ) {
					Message.printWarning ( 1, routine, "Error converting file to relative path." );
				}
				File pathFolder = new File(path);
				if ( pathFolder.exists() ) {
					JGUIUtil.setLastFileDialogDirectory(path);
				}
				refresh();
			}
		}
	}
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "CopyFile");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else if ( o == __pathInput_JButton ) {
		if ( __pathInput_JButton.getText().equals(__AddWorkingDirectory) ) {
			__InputFile_JTextField.setText (IOUtil.toAbsolutePath(__working_dir,__InputFile_JTextField.getText() ) );
		}
		else if ( __pathInput_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
                __InputFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,
                        __InputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,"CopyFile_JDialog",
				"Error converting input file name to relative path." );
			}
		}
		refresh ();
	}
    else if ( o == __pathOutput_JButton ) {
        if ( __pathOutput_JButton.getText().equals(__AddWorkingDirectory) ) {
            __OutputFile_JTextField.setText (IOUtil.toAbsolutePath(__working_dir,__OutputFile_JTextField.getText() ) );
        }
        else if ( __pathOutput_JButton.getText().equals(__RemoveWorkingDirectory) ) {
            try {
                __OutputFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,
                        __OutputFile_JTextField.getText() ) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1,"CopyFile_JDialog",
                "Error converting output file name to relative path." );
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
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check.
	PropList props = new PropList ( "" );
	String InputFile = __InputFile_JTextField.getText().trim();
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String TempFolder = __TempFolder_JTextField.getText().trim();
	String TempFilePrefix = __TempFilePrefix_JTextField.getText().trim();
	String TempFileSuffix = __TempFileSuffix_JTextField.getText().trim();
	String TempFileProperty = __TempFileProperty_JTextField.getText().trim();
	String IfInputNotFound = __IfInputNotFound_JComboBox.getSelected();
	__error_wait = false;
	if ( InputFile.length() > 0 ) {
		props.set ( "InputFile", InputFile );
	}
    if ( OutputFile.length() > 0 ) {
        props.set ( "OutputFile", OutputFile );
    }
    if ( TempFolder.length() > 0 ) {
        props.set ( "TempFolder", TempFolder );
    }
    if ( TempFilePrefix.length() > 0 ) {
        props.set ( "TempFilePrefix", TempFilePrefix );
    }
    if ( TempFileSuffix.length() > 0 ) {
        props.set ( "TempFileSuffix", TempFileSuffix );
    }
    if ( TempFileProperty.length() > 0 ) {
        props.set ( "TempFileProperty", TempFileProperty );
    }
	if ( IfInputNotFound.length() > 0 ) {
		props.set ( "IfInputNotFound", IfInputNotFound );
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
Commit the edits to the command.  In this case the command parameters have
already been checked and no errors were detected.
*/
private void commitEdits ()
{	String InputFile = __InputFile_JTextField.getText().trim();
    String OutputFile = __OutputFile_JTextField.getText().trim();
	String TempFolder = __TempFolder_JTextField.getText().trim();
	String TempFilePrefix = __TempFilePrefix_JTextField.getText().trim();
	String TempFileSuffix = __TempFileSuffix_JTextField.getText().trim();
	String TempFileProperty = __TempFileProperty_JTextField.getText().trim();
	String IfInputNotFound = __IfInputNotFound_JComboBox.getSelected();
	__command.setCommandParameter ( "InputFile", InputFile );
	__command.setCommandParameter ( "OutputFile", OutputFile );
	__command.setCommandParameter ( "TempFolder", TempFolder );
	__command.setCommandParameter ( "TempFilePrefix", TempFilePrefix );
	__command.setCommandParameter ( "TempFileSuffix", TempFileSuffix );
	__command.setCommandParameter ( "TempFileProperty", TempFileProperty );
	__command.setCommandParameter ( "IfInputNotFound", IfInputNotFound );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, CopyFile_Command command )
{	__command = command;
	CommandProcessor processor =__command.getCommandProcessor();
	
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel.

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Copy one file to another file.  Currently only a single file can be copied." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Specify an output file name or an auto-generated temporary file." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Filenames can use the notation ${Property} to use global processor properties." ),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    if ( __working_dir != null ) {
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"It is recommended that the file names are relative to the working directory, which is:"),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	JGUIUtil.addComponent(main_JPanel, new JLabel ("    " + __working_dir),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Input file:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputFile_JTextField = new JTextField ( 50 );
	__InputFile_JTextField.setToolTipText("Specify the input file to copy, can use ${Property} notation.");
	__InputFile_JTextField.addKeyListener ( this );
    // Input file layout fights back with other rows so put in its own panel.
	JPanel InputFile_JPanel = new JPanel();
	InputFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(InputFile_JPanel, __InputFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseInput_JButton = new SimpleJButton ( "...", this );
	__browseInput_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(InputFile_JPanel, __browseInput_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__pathInput_JButton = new SimpleJButton( __RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(InputFile_JPanel, __pathInput_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(main_JPanel, InputFile_JPanel,
		1, y, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

   JGUIUtil.addComponent(main_JPanel, new JLabel ( "If input not found?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IfInputNotFound_JComboBox = new SimpleJComboBox ( false );
	List<String> notFoundChoices = new ArrayList<>();
	notFoundChoices.add ( "" );	// Default.
	notFoundChoices.add ( __command._Ignore );
	notFoundChoices.add ( __command._Warn );
	notFoundChoices.add ( __command._Fail );
	__IfInputNotFound_JComboBox.setData(notFoundChoices);
	__IfInputNotFound_JComboBox.select ( 0 );
	__IfInputNotFound_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(main_JPanel, __IfInputNotFound_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - action if input file is not found (default=" + __command._Warn + ")."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for general parameters.
    int yGeneral = -1;
    JPanel general_JPanel = new JPanel();
    general_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "General", general_JPanel );

    JGUIUtil.addComponent(general_JPanel, new JLabel ("Output file:" ),
        0, ++yGeneral, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputFile_JTextField = new JTextField ( 50 );
    __OutputFile_JTextField.setToolTipText("Specify the output file to copy, can use ${Property} notation.");
    __OutputFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel.
	JPanel OutputFile_JPanel = new JPanel();
	OutputFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(OutputFile_JPanel, __OutputFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseOutput_JButton = new SimpleJButton ( "...", this );
	__browseOutput_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(OutputFile_JPanel, __browseOutput_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__pathOutput_JButton = new SimpleJButton( __RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(OutputFile_JPanel, __pathOutput_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(general_JPanel, OutputFile_JPanel,
		1, yGeneral, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for temporary file parameters.
    int yTemp = -1;
    JPanel temp_JPanel = new JPanel();
    temp_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Temporary File", temp_JPanel );

    JGUIUtil.addComponent(temp_JPanel, new JLabel (
        "A temporary file can be created in a specified folder or if not specified the default temporary folder:"),
        0, ++yTemp, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(temp_JPanel, new JLabel (
        "  " + System.getProperty("java.io.tmpdir")),
        0, ++yTemp, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(temp_JPanel, new JLabel (
        "At least one of the following parameters must be specified to cause a temporary output file to be used."),
        0, ++yTemp, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(temp_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yTemp, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(temp_JPanel, new JLabel ( "Temporary folder:" ), 
		0, ++yTemp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TempFolder_JTextField = new JTextField ( 50 );
	__TempFolder_JTextField.setToolTipText("Specify the folder for temporary files, can use ${Property} notation");
	__TempFolder_JTextField.addKeyListener ( this );
    // Folder layout fights back with other rows so put in its own panel.
	JPanel TempFolder_JPanel = new JPanel();
	TempFolder_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(TempFolder_JPanel, __TempFolder_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseTempFolder_JButton = new SimpleJButton ( "...", this );
	__browseTempFolder_JButton.setToolTipText("Browse for folder");
    JGUIUtil.addComponent(TempFolder_JPanel, __browseTempFolder_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__pathTempFolder_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(TempFolder_JPanel, __pathTempFolder_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(temp_JPanel, TempFolder_JPanel,
		1, yTemp, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(temp_JPanel, new JLabel ( "Temporary file prefix:"),
        0, ++yTemp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TempFilePrefix_JTextField = new JTextField ( 20 );
    __TempFilePrefix_JTextField.setToolTipText("For example: #");
    __TempFilePrefix_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(temp_JPanel, __TempFilePrefix_JTextField,
        1, yTemp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(temp_JPanel, new JLabel( "Optional - temporary filename prefix."),
        3, yTemp, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(temp_JPanel, new JLabel ( "Temporary file suffix:"),
        0, ++yTemp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TempFileSuffix_JTextField = new JTextField ( 20 );
    __TempFileSuffix_JTextField.setToolTipText("Suffix (extension) for temporary file, must include period (default=.tmp)");
    __TempFileSuffix_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(temp_JPanel, __TempFileSuffix_JTextField,
        1, yTemp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(temp_JPanel, new JLabel( "Optional - temporary filename suffix (default=.tmp)."),
        3, yTemp, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(temp_JPanel, new JLabel ( "Temporary file property:"),
        0, ++yTemp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TempFileProperty_JTextField = new JTextField ( 20 );
    __TempFileProperty_JTextField.setToolTipText("Property to set for temporary file name.");
    __TempFileProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(temp_JPanel, __TempFileProperty_JTextField,
        1, yTemp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(temp_JPanel, new JLabel( "Optional - property to set to temporary filename."),
        3, yTemp, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

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

	setTitle ( "Edit " + __command.getCommandName() + "() command" );
	
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
{	String routine = getClass().getName() + ".refresh";
	String InputFile = "";
	String OutputFile = "";
	String TempFolder = "";
	String TempFilePrefix = "";
	String TempFileSuffix = "";
	String TempFileProperty = "";
	String IfInputNotFound = "";
    PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
        parameters = __command.getCommandParameters();
		InputFile = parameters.getValue ( "InputFile" );
		OutputFile = parameters.getValue ( "OutputFile" );
		TempFolder = parameters.getValue ( "TempFolder" );
		TempFilePrefix = parameters.getValue ( "TempFilePrefix" );
		TempFileSuffix = parameters.getValue ( "TempFileSuffix" );
		TempFileProperty = parameters.getValue ( "TempFileProperty" );
		IfInputNotFound = parameters.getValue ( "IfInputNotFound" );
		if ( InputFile != null ) {
			__InputFile_JTextField.setText ( InputFile );
		}
        if ( OutputFile != null ) {
            __OutputFile_JTextField.setText ( OutputFile );
            __main_JTabbedPane.setSelectedIndex(0);
        }
		if ( TempFolder != null ) {
			__TempFolder_JTextField.setText ( TempFolder );
            __main_JTabbedPane.setSelectedIndex(1);
		}
		if ( TempFilePrefix != null ) {
			__TempFilePrefix_JTextField.setText ( TempFilePrefix );
            __main_JTabbedPane.setSelectedIndex(1);
		}
		if ( TempFileSuffix != null ) {
			__TempFileSuffix_JTextField.setText ( TempFileSuffix );
            __main_JTabbedPane.setSelectedIndex(1);
		}
		if ( TempFileProperty != null ) {
			__TempFileProperty_JTextField.setText ( TempFileProperty );
            __main_JTabbedPane.setSelectedIndex(1);
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__IfInputNotFound_JComboBox, IfInputNotFound,JGUIUtil.NONE, null, null ) ) {
			__IfInputNotFound_JComboBox.select ( IfInputNotFound );
		}
		else {
            if ( (IfInputNotFound == null) ||	IfInputNotFound.equals("") ) {
				// New command...select the default.
				__IfInputNotFound_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"IfInputNotFound parameter \"" +	IfInputNotFound +
				"\".  Select a\n value or Cancel." );
			}
		}
	}
	// Regardless, reset the command from the fields.
	// This is only  visible information that has not been committed in the command.
	InputFile = __InputFile_JTextField.getText().trim();
	OutputFile = __OutputFile_JTextField.getText().trim();
	TempFolder = __TempFolder_JTextField.getText().trim();
	TempFilePrefix = __TempFilePrefix_JTextField.getText().trim();
	TempFileSuffix = __TempFileSuffix_JTextField.getText().trim();
	TempFileProperty = __TempFileProperty_JTextField.getText().trim();
	IfInputNotFound = __IfInputNotFound_JComboBox.getSelected();
	PropList props = new PropList ( __command.getCommandName() );
	props.add ( "InputFile=" + InputFile );
	props.add ( "OutputFile=" + OutputFile );
	props.add ( "TempFolder=" + TempFolder );
	props.add ( "TempFilePrefix=" + TempFilePrefix );
	props.add ( "TempFileSuffix=" + TempFileSuffix );
	props.add ( "TempFileProperty=" + TempFileProperty );
	props.add ( "IfInputNotFound=" + IfInputNotFound );
	__command_JTextArea.setText( __command.toString(props) );
	// Check the path and determine what the label on the path button should be.
	if ( __pathInput_JButton != null ) {
		if ( (InputFile != null) && !InputFile.isEmpty() ) {
			__pathInput_JButton.setEnabled ( true );
			File f = new File ( InputFile );
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
		if ( (OutputFile != null) && !OutputFile.isEmpty() ) {
			__pathOutput_JButton.setEnabled ( true );
			File f = new File ( OutputFile );
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
	if ( __pathTempFolder_JButton != null ) {
		if ( (TempFolder != null) && !TempFolder.isEmpty() && (TempFolder.indexOf(",") < 0) ) {
			__pathTempFolder_JButton.setEnabled ( true );
			File f = new File ( TempFolder );
			if ( f.isAbsolute() ) {
				__pathTempFolder_JButton.setText ( __RemoveWorkingDirectory );
				__pathTempFolder_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__pathTempFolder_JButton.setText ( __AddWorkingDirectory );
            	__pathTempFolder_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathTempFolder_JButton.setEnabled(false);
		}
	}
}

/**
React to the user response.
@param ok if false, then the edit is canceled.
If true, the edit is committed and the dialog is closed.
*/
public void response ( boolean ok )
{	__ok = ok;
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