// ReadMODSIM_JDialog - Command editor dialog for the ReadMODSIM().

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

package rti.tscommandprocessor.commands.modsim;

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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import RTi.TS.ModsimTS;
import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.TS.TSIdent;

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

/**
Command editor dialog for the ReadMODSIM().
*/
@SuppressWarnings("serial")
public class ReadMODSIM_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
private final String
    __RemoveWorkingDirectory = "Rel",
    __AddWorkingDirectory = "Abs";    
    
private SimpleJButton __browse_JButton = null;
private SimpleJButton __path_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private ReadMODSIM_Command __command = null;
private String __working_dir = null;	// Working directory.
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private JTextField __NodeName_JTextField;	// Node name to read.
private JTextField __TSID_JTextField;
private JTextField __InputStart_JTextField;
private JTextField __InputEnd_JTextField;
private JTextField __InputFile_JTextField = null;
			//__NewUnits_JTextField = null  // not in the file
			// TODO SAM 2008-09-09 Need to check whether units are in version 8 files.
JTextArea __command_JTextArea = null;

private SimpleJComboBox	__DataType_JComboBox; // Node data type choice.
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Whether OK was pressed to close dialog

/**
Command editor dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadMODSIM_JDialog (	JFrame parent, ReadMODSIM_Command command )
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
		    fc = JFileChooserFactory.createJFileChooser( __working_dir );
		}
		fc.setDialogTitle( "Select MODSIM Output Time Series File");
		SimpleFileFilter sff = new SimpleFileFilter("FLO", "MODSIM flow link output");
		fc.addChoosableFileFilter(sff);
		sff = new SimpleFileFilter("RES", "MODSIM reservoir output");
		fc.addChoosableFileFilter(sff);
		sff = new SimpleFileFilter("DEM", "MODSIM demand output");
		fc.addChoosableFileFilter(sff);
		sff = new SimpleFileFilter("GW", "MODSIM groundwater output");
		fc.addChoosableFileFilter(sff);
		
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
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
					Message.printWarning ( 1,"ReadMODSIM_JDialog", "Error converting file to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory( last_directory_selected );
	            updateDataTypeChoices();
				refresh();
			}
		}
	}
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "ReadMODSIM");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else if ( o == __path_JButton ) {
		if ( __path_JButton.getText().equals( __AddWorkingDirectory) ) {
			__InputFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir, __InputFile_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals( __RemoveWorkingDirectory) ) {
			try {
			    __InputFile_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir, __InputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,
				"ReadMODSIM_JDialog", "Error converting file to relative path." );
			}
		}
		refresh ();
	}
}

// Start event handlers for DocumentListener...

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void changedUpdate ( DocumentEvent e )
{
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void insertUpdate ( DocumentEvent e )
{
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void removeUpdate ( DocumentEvent e )
{
    refresh();
}

// ...End event handlers for DocumentListener

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput () {
    
    // Put together a list of parameters to check...
    PropList props = new PropList ( "" );
    String InputFile = __InputFile_JTextField.getText().trim();
    String InputStart = __InputStart_JTextField.getText().trim();
    String InputEnd = __InputEnd_JTextField.getText().trim();
    //String NewUnits = __NewUnits_JTextField.getText().trim();
    String Alias = __Alias_JTextField.getText().trim();
    String TSID = __TSID_JTextField.getText().trim();
    
    __error_wait = false;
    
    if (InputFile.length() > 0) {
        props.set("InputFile", InputFile);
    }
    if (InputStart.length() > 0 && !InputStart.equals("*")) {
        props.set("InputStart", InputStart);
    }
    if (InputEnd.length() > 0 && !InputEnd.equals("*")) {
        props.set("InputEnd", InputEnd);
    }
    /*
    if (NewUnits.length() > 0 && !NewUnits.equals("*")) {
        props.set("NewUnits", NewUnits);
    }*/
    if (Alias != null && Alias.length() > 0) {
        props.set("Alias", Alias);
    }
    if (TSID != null && TSID.length() > 0) {
        props.set("TSID", TSID);
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
private void commitEdits()
{
    String InputFile = __InputFile_JTextField.getText().trim();
    String InputStart = __InputStart_JTextField.getText().trim();
    String InputEnd = __InputEnd_JTextField.getText().trim();
    //String NewUnits = __NewUnits_JTextField.getText().trim();
    String Alias = __Alias_JTextField.getText().trim();
    String TSID = __TSID_JTextField.getText().trim();

    __command.setCommandParameter("InputFile", InputFile);
    __command.setCommandParameter("InputStart", InputStart);
    __command.setCommandParameter("InputEnd", InputEnd);
    //__command.setCommandParameter("NewUnits", NewUnits);
    __command.setCommandParameter("Alias", Alias);
    __command.setCommandParameter("TSID", TSID);
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, ReadMODSIM_Command command )
{	__command = command;
    __working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)__command.getCommandProcessor(), __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Read 1+ time series from a MODSIM format file."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Specify the node/link name and data type to read one time series or leave blank to read all time series."),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The node/link name must be consistent with information in the MODSIM file."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Data types are determined from the file extension but others can be specified."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify a full path or relative path (relative to working directory) for a MODSIM file to read." ), 
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specifying the period will limit data that are available " +
		"for fill commands but can increase performance." ), 
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    //JGUIUtil.addComponent(main_JPanel, new JLabel (
		//"Specifying units causes conversion during the read (currently under development)."),
		//0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"If not specified, the period defaults to the query period."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
	if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel ( "The working directory is: " + __working_dir ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}

    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "MODSIM file to read:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputFile_JTextField = new JTextField ( 50 );
	__InputFile_JTextField.addKeyListener ( this );
    // Input file layout fights back with other rows so put in its own panel
	JPanel InputFile_JPanel = new JPanel();
	InputFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(InputFile_JPanel, __InputFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ( "...", this );
	__browse_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(InputFile_JPanel, __browse_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__path_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(InputFile_JPanel, __path_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(main_JPanel, InputFile_JPanel,
		1, y, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel,new JLabel("Node/link name to read:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
        __NodeName_JTextField = new JTextField ( "", 30 );
        __NodeName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __NodeName_JTextField,
		1, y, 3, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - default is to read all."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel("Data type to read:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
        __DataType_JComboBox = new SimpleJComboBox ( false );
        List<String> typeChoices = new ArrayList<String>();
        typeChoices.add ( "Unavailable" );
        __DataType_JComboBox.setData(typeChoices);
        __DataType_JComboBox.addItemListener ( this );
	JGUIUtil.addComponent(main_JPanel, __DataType_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - default is to read all."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	    
    // Show full TSID, formed from above data (but not directly editable)
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "TSID (full):"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TSID_JTextField = new JTextField ( "" );
    __TSID_JTextField.setToolTipText(
        "Time series identifier to read, including only location and data type.");
    __TSID_JTextField.setEditable ( false );
    __TSID_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __TSID_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Created from node/link name and data type."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(10);
    __Alias_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener(this);
    __Alias_JTextField.setToolTipText("%L for location, %T for data type.");
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - use %L for location, etc. (default=no alias)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    /*
    JGUIUtil.addComponent(main_JPanel, new JLabel("Units to convert to:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewUnits_JTextField = new JTextField ( "", 10 );
	__NewUnits_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel, __NewUnits_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Optional.  Default is units in file."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        */

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Input start:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputStart_JTextField = new JTextField (20);
    __InputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputStart_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - overrides the global input start."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input end:"), 
        0, ++y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputEnd_JTextField = new JTextField (20);
    __InputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputEnd_JTextField,
        1, y, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - overrides the global input end."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea(4, 55);
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

	__ok_JButton = new SimpleJButton("OK", this);
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add ( __ok_JButton );
	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

    setTitle("Edit " + __command.getCommandName() + " Command");
    pack();
    JGUIUtil.center( this );
	refresh();	// Sets the __path_JButton status
	setResizable ( false );
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent e )
{	// Only one choice...
	refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

    if ((event.getSource() == __InputFile_JTextField) &&
		((code == KeyEvent.VK_ENTER) || (code == KeyEvent.VK_TAB)) ) {
		updateDataTypeChoices();
	}
    refresh();
}

/**
Need this to properly capture key events, especially deletes.
*/
public void keyReleased ( KeyEvent event )
{	refresh();
}

public void keyTyped ( KeyEvent event )
{
}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user canceled.
*/
public boolean ok() {
    return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh() {
    String InputFile = "";
    String TSID = "";
    String InputStart = "";
    String InputEnd = "";
    String NewUnits = "";
    String Alias = "";

    PropList props = null;

    if (__first_time) {
        __first_time = false;

        // Get the properties from the command
        props = __command.getCommandParameters();
        InputFile = props.getValue("InputFile");
        InputStart = props.getValue("InputStart");
        InputEnd = props.getValue("InputEnd");
        NewUnits = props.getValue("NewUnits");
        Alias = props.getValue("Alias");
        TSID = props.getValue("TSID");

        if (InputFile != null) {
            // This needs to be before the TSID code below.
            __InputFile_JTextField.setText(InputFile);
            // Also initialize the data type choices
            updateDataTypeChoices();
        }
        if ( TSID != null ) {
            // Parse and set the parts, since that is what the user can edit.
            try {
                TSIdent tsident = new TSIdent ( TSID );
                __NodeName_JTextField.setText(tsident.getLocation());
                String DataType = tsident.getType();
                if (    JGUIUtil.isSimpleJComboBoxItem( __DataType_JComboBox, DataType,
                        JGUIUtil.NONE, null, null ) ) {
                        __DataType_JComboBox.select ( DataType );
                }
                else {
                    // Automatically add to the list after the blank...
                    if ( (DataType != null) && (DataType.length() > 0) ) {
                        __DataType_JComboBox.insertItemAt ( DataType, 1 );
                        // Select...
                        __DataType_JComboBox.select ( DataType );
                    }
                    else {
                        // Select the blank...
                        __DataType_JComboBox.select ( 0 );
                    }
                }
            }
            catch ( Exception e ) {
                // Unlikely but let the user fill in the parts to reform the TSID
            }
        }
        if ( Alias != null ) {
            __Alias_JTextField.setText(Alias.trim());
        }
        if (InputStart != null) {
            __InputStart_JTextField.setText(InputStart);
        }
        if (InputEnd != null) {
            __InputEnd_JTextField.setText(InputEnd);
        }
        /*
        if (NewUnits != null) {
            __NewUnits_JTextField.setText(NewUnits);
        }
        */
    }

    // Regardless, reset the command from the fields.  This is only visible
    // information that has not been committed in the command.
    InputFile = __InputFile_JTextField.getText().trim();
    InputStart = __InputStart_JTextField.getText().trim();
    InputEnd = __InputEnd_JTextField.getText().trim();
    //NewUnits = __NewUnits_JTextField.getText().trim();
    Alias = __Alias_JTextField.getText().trim();
    TSID = __NodeName_JTextField.getText().trim() + ".." + __DataType_JComboBox.getSelected() + "..";
    if ( (TSID.indexOf("Unavailable") > 0) || TSID.startsWith("....") ) {
        // Leave TSID blank in final command string since no useful information for runtime
        TSID = "";
    }
    __TSID_JTextField.setText ( TSID );

    props = new PropList(__command.getCommandName());
    props.add("InputFile=" + InputFile);
    props.add("InputStart=" + InputStart);
    props.add("InputEnd=" + InputEnd);
    props.add("NewUnits=" + NewUnits);
    props.add("Alias=" + Alias);
    props.add("TSID=" + TSID);
    
    __command_JTextArea.setText( __command.toString(props).trim() );

	// Check the path and determine what the label on the path button should be...
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
public void response ( boolean ok ) {
    __ok = ok;
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
Update the data type choices based on the file extension.
*/
private void updateDataTypeChoices()
{	String routine = getClass().getName() + ".updateDataTypeChoices";
    __DataType_JComboBox.removeAll();
    try {
        // Shouldn't need entire path since only using extension but put in code for future support.
    	String [] availableDataTypes = ModsimTS.getAvailableDataTypes(
    	    IOUtil.verifyPathForOS(IOUtil.adjustPath (__working_dir,
                TSCommandProcessorUtil.expandParameterValue(
                    __command.getCommandProcessor(),__command,__InputFile_JTextField.getText().trim()))), false );
    	if ( availableDataTypes == null ) {
    		__DataType_JComboBox.add ( "Unavailable" );
    	}
    	else {
    	    List<String> availableDataTypesList = StringUtil.toList(availableDataTypes);
    	    availableDataTypesList.add(0,"");
    	    __DataType_JComboBox.setData ( availableDataTypesList );
    	}
    }
    catch ( Exception e ) {
        Message.printWarning ( 3, routine, e );
        __DataType_JComboBox.add ( "Unavailable" );
    }
}

/**
Responds to WindowEvents.
@param event WindowEvent object
*/
public void windowClosing( WindowEvent event )
{	response ( false );
}

public void windowActivated( WindowEvent evt )
{
}

public void windowClosed( WindowEvent evt )
{
}

public void windowDeactivated( WindowEvent evt )
{
}

public void windowDeiconified( WindowEvent evt )
{
}

public void windowIconified( WindowEvent evt )
{
}

public void windowOpened( WindowEvent evt )
{
}

}
