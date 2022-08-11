// ReadPropertiesFromFile_JDialog - Command editor dialog for the ReadPropertiesFromFile() command.

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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.IO.PropertyFileFormatType;
import RTi.Util.Message.Message;

/**
Command editor dialog for the ReadPropertiesFromFile() command.
*/
@SuppressWarnings("serial")
public class ReadPropertiesFromFile_JDialog extends JDialog
implements ActionListener, KeyListener, ItemListener, WindowListener
{

private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __browse_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __path_JButton = null;
private ReadPropertiesFromFile_Command __command = null;
private String __working_dir = null;
private JTextArea __command_JTextArea=null;
private JTextField __InputFile_JTextField = null;
// TODO SAM 2012-07-27 Convert the following from a text field to a property selector/formatter,
// similar to TSFormatSpecifiersJPanel.
private SimpleJComboBox __FileFormat_JComboBox = null;
private JTextField __IncludeProperty_JTextField = null;
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether the user has pressed OK to close the dialog.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadPropertiesFromFile_JDialog (	JFrame parent, ReadPropertiesFromFile_Command command )
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
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
		}
		else {
			fc = JFileChooserFactory.createJFileChooser(__working_dir );
		}
		fc.setDialogTitle("Select Property File to Read");
		SimpleFileFilter sff = new SimpleFileFilter("txt", "Property File");
		fc.addChoosableFileFilter(sff);
		
		if (fc.showDialog(this,"Select") == JFileChooser.APPROVE_OPTION) {
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
					Message.printWarning ( 1, __command.getCommandName() + "_JDialog", "Error converting file to relative path." );
				}
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
	else if ( o == __path_JButton ) {
		if ( __path_JButton.getText().equals(__AddWorkingDirectory) ) {
			__InputFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir,__InputFile_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
			    __InputFile_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir,__InputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, "ReadPropertiesFromFile_JDialog",
				"Error converting file to relative path." );
			}
		}
		refresh ();
	}
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag to true.
This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check.
	PropList parameters = new PropList ( "" );
	String InputFile = __InputFile_JTextField.getText().trim();
	String FileFormat = __FileFormat_JComboBox.getSelected();
	String IncludeProperty = __IncludeProperty_JTextField.getText().trim();

	__error_wait = false;
	
	if ( InputFile.length() > 0 ) {
		parameters.set ( "InputFile", InputFile );
	}
	if ( IncludeProperty.length() > 0 ) {
		parameters.set ( "IncludeProperty", IncludeProperty );
	}
    if ( FileFormat.length() > 0 ) {
        parameters.set ( "FileFormat", FileFormat );
    }
	try {
	    // This will warn the user.
		__command.checkCommandParameters ( parameters, null, 1 );
	}
	catch ( Exception e ) {
		// The warning would have been printed in the check code.
		Message.printWarning(2,"",e);
		__error_wait = true;
	}
}

/**
Commit the edits to the command.  In this case the command parameters have
already been checked and no errors were detected.
*/
private void commitEdits ()
{	String InputFile = __InputFile_JTextField.getText().trim();
    String IncludeProperty = __IncludeProperty_JTextField.getText().trim();
    String FileFormat = __FileFormat_JComboBox.getSelected();
    __command.setCommandParameter ( "InputFile", InputFile );
	__command.setCommandParameter ( "IncludeProperty", IncludeProperty );
	__command.setCommandParameter ( "FileFormat", FileFormat );
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, ReadPropertiesFromFile_Command command )
{	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(1,2,1,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Read one or more properties from a file.  The properties will apply globally to subsequent commands." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"It is recommended that the input file location is relative to the current working directory." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The working directory is: " + __working_dir ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Property file to read:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputFile_JTextField = new JTextField ( 50 );
	__InputFile_JTextField.setToolTipText("Property file name, can use ${Property} notation.");
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
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("File format:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __FileFormat_JComboBox = new SimpleJComboBox(false);
    List<PropertyFileFormatType> fileFormatTypes = __command.getFileFormatChoices();
    List<String> fileFormatChoices = new ArrayList<>();
    fileFormatChoices.add ( "" );
    for ( PropertyFileFormatType c : fileFormatTypes ) {
        fileFormatChoices.add ( "" + c );
    }
    __FileFormat_JComboBox.setData (fileFormatChoices);
    __FileFormat_JComboBox.select(0);
    __FileFormat_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(main_JPanel, __FileFormat_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - property file format (default=" + PropertyFileFormatType.NAME_TYPE_VALUE + ")."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Property to read:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IncludeProperty_JTextField = new JTextField(20);
	__IncludeProperty_JTextField.setToolTipText("List of properties to read separated by commas, can use ${Property} notation.");
	__IncludeProperty_JTextField.addKeyListener (this);
	JGUIUtil.addComponent(main_JPanel, __IncludeProperty_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - properties to read, separated by commas (default=read all)."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
    		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 4, 50 );
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
    		1, y, 6, 1, 1, 1, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__cancel_JButton = new SimpleJButton("Cancel", "Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", "OK", this);
	button_JPanel.add ( __ok_JButton );

	setTitle ( "Edit " + __command.getCommandName() + "() Command" );
	setResizable ( false );
    pack();
    JGUIUtil.center( this );
	refresh();	// Sets the __path_JButton status.
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged (ItemEvent e) {
	refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
}

public void keyReleased ( KeyEvent event )
{	refresh();
}

public void keyTyped ( KeyEvent event )
{	// Covered by the other events.
}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user canceled.
*/
public boolean ok ()
{	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = getClass().getSimpleName() + ".refresh";
    String InputFile = "";
	String IncludeProperty = "";
	String FileFormat = "";
	__error_wait = false;
	PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command.
		parameters = __command.getCommandParameters();
		InputFile = parameters.getValue ( "InputFile" );
		IncludeProperty = parameters.getValue ( "IncludeProperty" );
		FileFormat = parameters.getValue ( "FileFormat" );
		if ( InputFile != null ) {
			__InputFile_JTextField.setText (InputFile);
		}
        if ( FileFormat == null ) {
            // Select default.
            __FileFormat_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem(__FileFormat_JComboBox,
                FileFormat, JGUIUtil.NONE, null, null ) ) {
                __FileFormat_JComboBox.select ( FileFormat );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nFileFormat value \"" +
                FileFormat + "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
		if ( IncludeProperty != null ) {
            __IncludeProperty_JTextField.setText (IncludeProperty);
        }
	}
	// Regardless, reset the command from the fields.
	InputFile = __InputFile_JTextField.getText().trim();
	FileFormat = __FileFormat_JComboBox.getSelected();
	IncludeProperty = __IncludeProperty_JTextField.getText().trim();
	parameters = new PropList ( __command.getCommandName() );
	parameters.add ( "InputFile=" + InputFile );
	parameters.add ( "FileFormat=" + FileFormat );
	parameters.add ( "IncludeProperty=" + IncludeProperty );
	__command_JTextArea.setText( __command.toString ( parameters ) );
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
@param ok if false, then the edit is canceled.  If true, the edit is committed
and the dialog is closed.
*/
private void response ( boolean ok )
{	__ok = ok;	// Save to be returned by ok()
	if ( ok ) {
		// Commit the changes.
		commitEdits ();
		if ( __error_wait ) {
			// Not ready to close out!
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