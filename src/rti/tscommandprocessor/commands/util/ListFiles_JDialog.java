// ListFiles_JDialog - editor for ListFiles command

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

package rti.tscommandprocessor.commands.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
public class ListFiles_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __browse_JButton = null;
private SimpleJButton __path_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private JTextField __Folder_JTextField = null;
private SimpleJComboBox __ListScope_JComboBox = null;
private SimpleJComboBox __ListFiles_JComboBox = null;
private SimpleJComboBox __ListFolders_JComboBox = null;
private JTextField __IncludeNames_JTextField = null;
private JTextField __ExcludeNames_JTextField = null;
private SimpleJComboBox __TableID_JComboBox = null;
private SimpleJComboBox __Append_JComboBox = null;
private JTextField __CountProperty_JTextField = null;
private JTextArea __command_JTextArea = null;
private String __working_dir = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private ListFiles_Command __command = null;
private boolean __ok = false; // Whether the user has pressed OK to close the dialog.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of tables to choose from, used if appending
*/
public ListFiles_JDialog ( JFrame parent, ListFiles_Command command, List<String> tableIDChoices ) {
	super(parent, true);
	initialize ( parent, command, tableIDChoices );
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
			fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
		}
		else {
		    fc = JFileChooserFactory.createJFileChooser(__working_dir );
		}
		fc.setDialogTitle( "Select Folder Containing Files to List");
		fc.setFileSelectionMode (JFileChooser.DIRECTORIES_ONLY );

		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			String filename = fc.getSelectedFile().getName();
			String path = fc.getSelectedFile().getPath();

			if (filename == null || filename.equals("")) {
				return;
			}

			if (path != null) {
				// Convert path to relative path by default.
				try {
					__Folder_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1,"FTPGet_JDialog", "Error converting file to relative path." );
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
		HelpViewer.getInstance().showHelp("command", "ListFiles");
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
			__Folder_JTextField.setText (IOUtil.toAbsolutePath(__working_dir,__Folder_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
                __Folder_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,
                        __Folder_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, "ListFiles_JDialog", "Error converting input file name to relative path." );
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
	String Folder = __Folder_JTextField.getText().trim();
	String ListScope = __ListScope_JComboBox.getSelected();
	String ListFiles = __ListFiles_JComboBox.getSelected();
	String ListFolders = __ListFolders_JComboBox.getSelected();
	String IncludeNames = __IncludeNames_JTextField.getText().trim();
	String ExcludeNames = __ExcludeNames_JTextField.getText().trim();
	String TableID = __TableID_JComboBox.getSelected();
	String Append = __Append_JComboBox.getSelected();
	String CountProperty = __CountProperty_JTextField.getText().trim();
	__error_wait = false;
	if ( Folder.length() > 0 ) {
		props.set ( "Folder", Folder );
	}
	if ( ListScope.length() > 0 ) {
		props.set ( "ListScope", ListScope );
	}
	if ( ListFiles.length() > 0 ) {
		props.set ( "ListFiles", ListFiles );
	}
	if ( ListFolders.length() > 0 ) {
		props.set ( "ListFolders", ListFolders );
	}
    if ( IncludeNames.length() > 0 ) {
        props.set ( "IncludeNames", IncludeNames );
    }
    if ( ExcludeNames.length() > 0 ) {
        props.set ( "ExcludeNames", ExcludeNames );
    }
    if ( TableID.length() > 0 ) {
        props.set ( "TableID", TableID );
    }
	if ( Append.length() > 0 ) {
		props.set ( "Append", Append );
	}
	if ( CountProperty.length() > 0 ) {
		props.set ( "CountProperty", CountProperty );
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
	String Folder = __Folder_JTextField.getText().trim();
	String ListScope = __ListScope_JComboBox.getSelected();
	String ListFiles = __ListFiles_JComboBox.getSelected();
	String ListFolders = __ListFolders_JComboBox.getSelected();
    String IncludeNames = __IncludeNames_JTextField.getText().trim();
    String ExcludeNames = __ExcludeNames_JTextField.getText().trim();
    String TableID = __TableID_JComboBox.getSelected();
    String Append = __Append_JComboBox.getSelected();
	String CountProperty = __CountProperty_JTextField.getText().trim();
    __command.setCommandParameter ( "Folder", Folder );
    __command.setCommandParameter ( "ListScope", ListScope );
    __command.setCommandParameter ( "ListFiles", ListFiles );
    __command.setCommandParameter ( "ListFolders", ListFolders );
	__command.setCommandParameter ( "IncludeNames", IncludeNames );
	__command.setCommandParameter ( "ExcludeNames", ExcludeNames );
	__command.setCommandParameter ( "TableID", TableID );
	__command.setCommandParameter ( "Append", Append );
	__command.setCommandParameter ( "CountProperty", CountProperty );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of tables to choose from, used if appending
*/
private void initialize ( JFrame parent, ListFiles_Command command, List<String> tableIDChoices ) {
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

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Create a list of files and/or folders given a starting folder and filtering parameters." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The output can be saved to the table and/or a property with the count." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The include and exclude name patterns can use * and will match the name only." ),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    if ( __working_dir != null ) {
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"It is recommended that the folder name be relative to the working directory, which is:"),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"    " + __working_dir),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Folder:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Folder_JTextField = new JTextField ( 50 );
	__Folder_JTextField.setToolTipText("Specify the folder from which to list files.");
	__Folder_JTextField.addKeyListener ( this );
    // Input file layout fights back with other rows so put in its own panel.
	JPanel Folder_JPanel = new JPanel();
	Folder_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(Folder_JPanel, __Folder_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ( "...", this );
	__browse_JButton.setToolTipText("Browse for folder");
    JGUIUtil.addComponent(Folder_JPanel, __browse_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__path_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(Folder_JPanel, __path_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(main_JPanel, Folder_JPanel,
		1, y, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "List scope:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ListScope_JComboBox = new SimpleJComboBox ( false );
	List<String> scopeChoices = new ArrayList<>();
	scopeChoices.add ( "" );	// Default.
	scopeChoices.add ( __command._All );
	scopeChoices.add ( __command._Folder );
	__ListScope_JComboBox.setData(scopeChoices);
	__ListScope_JComboBox.select ( 0 );
	__ListScope_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __ListScope_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - scope of listing (default=" + __command._Folder + ")."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "List files?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ListFiles_JComboBox = new SimpleJComboBox ( false );
	List<String> listFilesChoices = new ArrayList<>();
	listFilesChoices.add ( "" );	// Default.
	listFilesChoices.add ( __command._False );
	listFilesChoices.add ( __command._True );
	__ListFiles_JComboBox.setData(listFilesChoices);
	__ListFiles_JComboBox.select ( 0 );
	__ListFiles_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __ListFiles_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - whether to list files (default=" + __command._True + ")."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "List folders?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ListFolders_JComboBox = new SimpleJComboBox ( false );
	List<String> listFoldersChoices = new ArrayList<>();
	listFoldersChoices.add ( "" );	// Default.
	listFoldersChoices.add ( __command._False );
	listFoldersChoices.add ( __command._True );
	__ListFolders_JComboBox.setData(listFoldersChoices);
	__ListFolders_JComboBox.select ( 0 );
	__ListFolders_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(main_JPanel, __ListFolders_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - whether to list folders (default=" + __command._False + ")."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Names(s) to include:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IncludeNames_JTextField = new JTextField ( 40 );
    __IncludeNames_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __IncludeNames_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - names to include (default=include all)."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Names(s) to exclude:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ExcludeNames_JTextField = new JTextField ( 40 );
    __ExcludeNames_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __ExcludeNames_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - names to exclude (default=exclude none)."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Table ID:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit.
    tableIDChoices.add(0,""); // Add blank to ignore table.
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    __TableID_JComboBox.getJTextComponent().addKeyListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(main_JPanel, __TableID_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Optional - table containing the output list."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

   JGUIUtil.addComponent(main_JPanel, new JLabel ( "Append?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Append_JComboBox = new SimpleJComboBox ( false );
	List<String> appendChoices = new ArrayList<>();
	appendChoices.add ( "" );	// Default.
	appendChoices.add ( __command._False );
	appendChoices.add ( __command._True );
	__Append_JComboBox.setData(appendChoices);
	__Append_JComboBox.select ( 0 );
	__Append_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(main_JPanel, __Append_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - whether to append to table (default=" + __command._False + ")."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel("Count property:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CountProperty_JTextField = new JTextField ( "", 20 );
    __CountProperty_JTextField.setToolTipText("Specify the property name for the output count, can use ${Property} notation");
    __CountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __CountProperty_JTextField,
        1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - processor property to set as the output count." ),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

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
	setResizable ( true );
    super.setVisible( true );
}

/**
Handle item state changed events.
@param e ItemEvent for item state change
*/
public void itemStateChanged ( ItemEvent e ) {
     refresh();
}

/**
Handle key press events.
@param event KeyEvent for key pressed
*/
public void keyPressed ( KeyEvent event ) {
	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh ();
	}
}

/**
Handle key release events.
@param event KeyEvent for key release
*/
public void keyReleased ( KeyEvent event ) {
	refresh();
}

/**
Handle key typed events.
@param event KeyEvent for key typed
*/
public void keyTyped ( KeyEvent event ) {
}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true of OK was pressed
*/
public boolean ok () {
	return this.__ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh () {
	String routine = getClass().getSimpleName() + ".refresh";
	String Folder = "";
	String ListScope = "";
	String ListFiles = "";
	String ListFolders = "";
	String IncludeNames = "";
	String ExcludeNames = "";
	String TableID = "";
	String Append = "";
    String CountProperty = "";
    PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
        parameters = __command.getCommandParameters();
		Folder = parameters.getValue ( "Folder" );
		ListScope = parameters.getValue ( "ListScope" );
		ListFiles = parameters.getValue ( "ListFiles" );
		ListFolders = parameters.getValue ( "ListFolders" );
		IncludeNames = parameters.getValue ( "IncludeNames" );
		ExcludeNames = parameters.getValue ( "ExcludeNames" );
		TableID = parameters.getValue ( "TableID" );
		Append = parameters.getValue ( "Append" );
		CountProperty = parameters.getValue ( "CountProperty" );
		if ( Folder != null ) {
			__Folder_JTextField.setText ( Folder );
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__ListScope_JComboBox, ListScope,JGUIUtil.NONE, null, null ) ) {
			__ListScope_JComboBox.select ( ListScope );
		}
		else {
            if ( (ListScope == null) ||	ListScope.equals("") ) {
				// New command...select the default.
				__ListScope_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"ListScope parameter \"" + ListScope +
				"\".  Select a\n value or Cancel." );
			}
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__ListFiles_JComboBox, ListFiles,JGUIUtil.NONE, null, null ) ) {
			__ListFiles_JComboBox.select ( ListFiles );
		}
		else {
            if ( (ListFiles == null) ||	ListFiles.equals("") ) {
				// New command...select the default.
				__ListFiles_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"ListFiles parameter \"" + ListFiles +
				"\".  Select a\n value or Cancel." );
			}
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__ListFolders_JComboBox, ListFolders,JGUIUtil.NONE, null, null ) ) {
			__ListFolders_JComboBox.select ( ListFolders );
		}
		else {
            if ( (ListFolders == null) ||	ListFolders.equals("") ) {
				// New command...select the default.
				__ListFolders_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"ListFolders parameter \"" + ListFolders +
				"\".  Select a\n value or Cancel." );
			}
		}
        if ( IncludeNames != null ) {
            __IncludeNames_JTextField.setText ( IncludeNames );
        }
        if ( ExcludeNames != null ) {
            __ExcludeNames_JTextField.setText ( ExcludeNames );
        }
        if ( TableID == null ) {
            // Select default.
            __TableID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TableID_JComboBox,TableID, JGUIUtil.NONE, null, null ) ) {
                __TableID_JComboBox.select ( TableID );
            }
            else {
                // Creating new table so add in the first position.
                if ( __TableID_JComboBox.getItemCount() == 0 ) {
                    __TableID_JComboBox.add(TableID);
                }
                else {
                    __TableID_JComboBox.insert(TableID, 0);
                }
                __TableID_JComboBox.select(0);
            }
        }
		if ( JGUIUtil.isSimpleJComboBoxItem(__Append_JComboBox, Append,JGUIUtil.NONE, null, null ) ) {
			__Append_JComboBox.select ( Append );
		}
		else {
            if ( (Append == null) ||	Append.equals("") ) {
				// New command...select the default.
				__Append_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"Append parameter \"" + Append +
				"\".  Select a\n value or Cancel." );
			}
		}
        if ( CountProperty != null ) {
            __CountProperty_JTextField.setText ( CountProperty );
        }
	}
	// Regardless, reset the command from the fields.
	// This is only  visible information that has not been committed in the command.
	Folder = __Folder_JTextField.getText().trim();
	ListScope = __ListScope_JComboBox.getSelected();
	ListFiles = __ListFiles_JComboBox.getSelected();
	ListFolders = __ListFolders_JComboBox.getSelected();
	IncludeNames = __IncludeNames_JTextField.getText().trim();
	ExcludeNames = __ExcludeNames_JTextField.getText().trim();
	TableID = __TableID_JComboBox.getSelected();
	Append = __Append_JComboBox.getSelected();
	CountProperty = __CountProperty_JTextField.getText().trim();
	PropList props = new PropList ( __command.getCommandName() );
	props.add ( "Folder=" + Folder );
	props.add ( "ListScope=" + ListScope );
	props.add ( "ListFiles=" + ListFiles );
	props.add ( "ListFolders=" + ListFolders );
	props.add ( "IncludeNames=" + IncludeNames );
	props.add ( "ExcludeNames=" + ExcludeNames );
	props.add ( "TableID=" + TableID );
	props.add ( "Append=" + Append );
	props.add ( "CountProperty=" + CountProperty );
	__command_JTextArea.setText( __command.toString(props).trim() );
	// Check the path and determine what the label on the path button should be.
	if ( __path_JButton != null ) {
		if ( (Folder != null) && !Folder.isEmpty() ) {
			__path_JButton.setEnabled ( true );
			File f = new File ( Folder );
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