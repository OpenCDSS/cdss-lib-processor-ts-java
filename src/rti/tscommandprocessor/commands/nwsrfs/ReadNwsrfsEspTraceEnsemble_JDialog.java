// ReadNwsrfsEspTraceEnsemble_JDialog - The class edits the TS Alias = ReadNwsrfsEspTraceEnsemble() and non-TS Alias

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

package rti.tscommandprocessor.commands.nwsrfs;

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
import java.io.File;
import java.util.ArrayList;

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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;

import RTi.Util.Message.Message;

/**
The class edits the TS Alias = ReadNwsrfsEspTraceEnsemble() and non-TS Alias
ReadNwsrfsEspTraceEnsemble() commands.  Currently only the latter is implemented,
although the Alias parameter is used for both versions.
*/
@SuppressWarnings("serial")
public class ReadNwsrfsEspTraceEnsemble_JDialog extends JDialog
implements ActionListener, DocumentListener, KeyListener, WindowListener
{
private SimpleJButton __browse_JButton = null;
private SimpleJButton __path_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private ReadNwsrfsEspTraceEnsemble_Command __command = null;
private String __working_dir = null;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
			//__InputStart_JTextField,
			//__InputEnd_JTextField,
private JTextField __InputFile_JTextField = null;
			//__NewUnits_JTextField = null;
				// Units to convert to at read
private SimpleJComboBox	__Read24HourAsDay_JComboBox = null;
private JTextField __EnsembleID_JTextField = null;
private JTextField __EnsembleName_JTextField = null;
private JTextArea __Command_JTextArea = null;
private boolean __error_wait = false; // Is there an error to be cleared up
private boolean __first_time = true;

// TODO SAM 2011-05-23 Need to evaluate if needed since TSTool version 10 changes
private boolean __isAliasVersion = false;	
			// Whether this dialog is being opened for the version
			// of the command that returns an alias or not
private boolean __ok = false;			
private final String __RemoveWorkingDirectory = "Rel";
private final String __AddWorkingDirectory = "Abs";

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadNwsrfsEspTraceEnsemble_JDialog ( JFrame parent, ReadNwsrfsEspTraceEnsemble_Command command )
{
	super(parent, true);

	//PropList props = command.getCommandParameters();
	//String alias = props.getValue("Alias");
	//Message.printStatus(2, "", "Props: " + props.toString("\n"));
	//if (alias == null || alias.trim().equalsIgnoreCase("")) {
	//	if (((ReadNwsrfsEspTraceEnsemble_Command)command).getCommandString().trim().toUpperCase().startsWith("TS ")) {
	//	    __isAliasVersion = true;
	//	}
	//	else {
			__isAliasVersion = false;
	//	}
	//}
	/*
	else {
		__isAliasVersion = true;
	}
	*/
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();

	if ( o == __browse_JButton ) {
		// Browse for the file to read...
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle( "Select NWSRFS ESP Trace Ensemble File");
		SimpleFileFilter sff = new SimpleFileFilter("CS", "Conditional Simulation Trace File");
        fc.addChoosableFileFilter(sff);
        /* TODO SAM 2007-12-18 Evaluate enabling later when tested.
        sff = new SimpleFileFilter("HS","Historical Simulation Trace File");
        fc.addChoosableFileFilter(sff);
        */
		fc.addChoosableFileFilter(sff);
		
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		if ( last_directory_selected != null ) {
			fc.setCurrentDirectory(	new File(last_directory_selected));
		}
		else {
            fc.setCurrentDirectory(new File(__working_dir));
		}
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
					Message.printWarning ( 1,"CompareFiles_JDialog", "Error converting file to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory( directory);
				refresh();
			}
		}
	}
	else if ( o == __cancel_JButton ) {
		response(false);
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "ReadNwsrfsEspTraceEnsemble");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput ();
		if (!__error_wait) {
			response(true);
		}
	}
	else if ( o == __path_JButton ) {
		if ( __path_JButton.getText().equals(__AddWorkingDirectory) ) {
			__InputFile_JTextField.setText ( IOUtil.toAbsolutePath(__working_dir,__InputFile_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals( __RemoveWorkingDirectory) ) {
			try {
                __InputFile_JTextField.setText (IOUtil.toRelativePath ( __working_dir,__InputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, "ReadNwsrfsEspTraceEnsemble_JDialog",
				"Error converting file to relative path." );
			}
		}
		refresh ();
	}
    /*
	else if (o == __Read24HourAsDay_JComboBox) {
		refresh();
	}
    */
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
	String EnsembleID = __EnsembleID_JTextField.getText().trim();
	String EnsembleName = __EnsembleName_JTextField.getText().trim();
    /*
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
	String NewUnits = __NewUnits_JTextField.getText().trim();
	*/
	String Read24HourAsDay = __Read24HourAsDay_JComboBox.getSelected().trim();
	String Alias = null;
	if ( __Alias_JTextField != null ) {
		Alias = __Alias_JTextField.getText().trim();
	}
	
	__error_wait = false;
	
	if (InputFile.length() > 0) {
		props.set("InputFile", InputFile);
	}
    if ( EnsembleID.length() > 0 ) {
        props.set ( "EnsembleID", EnsembleID );
    }
    if ( EnsembleName.length() > 0 ) {
        props.set ( "EnsembleName", EnsembleName );
    }
    /*
	if (InputStart.length() > 0 && !InputStart.equals("*")) {
		props.set("InputStart", InputStart);
	}
	if (InputEnd.length() > 0 && !InputEnd.equals("*")) {
		props.set("InputEnd", InputEnd);
	}
	if (NewUnits.length() > 0 && !NewUnits.equals("*")) {
		props.set("NewUnits", NewUnits);
	}
    */
	if (Alias != null && Alias.length() > 0) {
		props.set("Alias", Alias);
	}
	if (Read24HourAsDay.trim().length() > 0) {
		props.set("Read24HourAsDay", Read24HourAsDay);
	}

	try {	// This will warn the user...
		__command.checkCommandParameters ( props, null, 1 );
	} 
	catch ( Exception e ) {
		// The warning would have been printed in the check 
		// code.
		__error_wait = true;
	}
}

/**
Commit the edits to the command.  In this case the command parameters have
already been checked and no errors were detected.
*/
private void commitEdits() {
	String InputFile = __InputFile_JTextField.getText().trim();
    /*
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
	String NewUnits = __NewUnits_JTextField.getText().trim();
	*/
	String Read24HourAsDay = __Read24HourAsDay_JComboBox.getSelected().trim();
    String EnsembleID = __EnsembleID_JTextField.getText().trim();
    String EnsembleName = __EnsembleName_JTextField.getText().trim();

	__command.setCommandParameter("InputFile", InputFile);
    /*
	__command.setCommandParameter("InputStart", InputStart);
	__command.setCommandParameter("InputEnd", InputEnd);
	__command.setCommandParameter("NewUnits", NewUnits);
	*/
	__command.setCommandParameter("Read24HourAsDay", Read24HourAsDay);
    __command.setCommandParameter("EnsembleID", EnsembleID);
    __command.setCommandParameter("EnsembleName", EnsembleName);
	
	if ( __Alias_JTextField != null ) {
		String Alias = __Alias_JTextField.getText().trim();
		__command.setCommandParameter("Alias", Alias);
	}
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param app_PropList Properties from application.
@param command Command to edit.
*/
private void initialize(JFrame parent, ReadNwsrfsEspTraceEnsemble_Command command)
{
	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

	if (__isAliasVersion) {
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Read a single time series from an NWSRFS ESP trace ensemble and assign an alias to the time series."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	else {
        JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Read all the time series from an ESP trace ensemble file, using information in the file to assign the identifier and alias."),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Specify a full or relative path (relative to working directory)." ), 
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The working directory is: " + __working_dir ), 
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    /*
       	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specifying units causes conversion during the read." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

       	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"If reading 24Hour data as Day and the input period is " +
		"specified, specify hour 24 of the day or hour 0 of the " +
		" following day."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

       	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specifying the input period will limit data that are " +
		"available for fill commands but can increase performance." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
       	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify date/times using an hour format (e.g.," +
		" YYYY-MM-DD HH or MM/DD/YYYY HH,"),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
       	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"where HH is evenly divisible by the interval)."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
       	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"If not specified, the period defaults to the global input "
		+ "period (or all data if not specified)."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        */

	if (__isAliasVersion) {
	    /*
	    JGUIUtil.addComponent(main_JPanel, new JLabel("Time series alias:"),
			0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
		__Alias_JTextField = new JTextField ( 30 );
		__Alias_JTextField.addKeyListener ( this );
		JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
			1, y, 3, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
			*/
	}

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Ensemble file to read:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputFile_JTextField = new JTextField ( 50 );
	__InputFile_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __InputFile_JTextField,
		1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ( "...", this );
	__browse_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(main_JPanel, __browse_JButton,
		6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__path_JButton = new SimpleJButton(__RemoveWorkingDirectory,this);
	    JGUIUtil.addComponent(main_JPanel, __path_JButton,
	    	7, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Ensemble ID:" ), 
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EnsembleID_JTextField = new JTextField ( "", 20 );
    __EnsembleID_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __EnsembleID_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required - identifier for ensemble."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Ensemble name:" ),
    0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EnsembleName_JTextField = new JTextField ( 20 );
    __EnsembleName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __EnsembleName_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - ensemble name."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    if ( !__isAliasVersion) {
        JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
        __Alias_JTextField = new TSFormatSpecifiersJPanel(15);
        __Alias_JTextField.addKeyListener ( this );
        __Alias_JTextField.getDocument().addDocumentListener(this);
        JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
            1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel( "Optional - default is Location_Trace_HistYear."), 
            3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }

        /*
        JGUIUtil.addComponent(main_JPanel, new JLabel("Units to convert to:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewUnits_JTextField = new JTextField ( "", 10 );
	__NewUnits_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel, __NewUnits_JTextField,
		1, y, 3, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
		*/

	JGUIUtil.addComponent(main_JPanel, new JLabel("Read 24 hour as day:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	ArrayList<String> v = new ArrayList<String>();
	v.add("");
	v.add(__command._FALSE);
	v.add(__command._TRUE);
	__Read24HourAsDay_JComboBox = new SimpleJComboBox(v);
	__Read24HourAsDay_JComboBox.select(0);
	__Read24HourAsDay_JComboBox.addActionListener(this);
	JGUIUtil.addComponent(main_JPanel, __Read24HourAsDay_JComboBox,
		1, y, 3, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Convert 24Hour interval to Day interval (default=False)."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

	/*
	JGUIUtil.addComponent(main_JPanel, new JLabel ( "Period to read:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputStart_JTextField = new JTextField ( "", 15 );
	__InputStart_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel, __InputStart_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel ( "to" ), 
		3, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	__InputEnd_JTextField = new JTextField ( "", 15 );
	__InputEnd_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel, __InputEnd_JTextField,
		4, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        */

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Command_JTextArea = new JTextArea(4, 55);
	__Command_JTextArea.setLineWrap ( true );
	__Command_JTextArea.setWrapStyleWord ( true );	
	__Command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__Command_JTextArea),
		1, y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
	refresh ();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	if (__isAliasVersion) {
		setTitle("Edit TS Alias = ReadNwsrfsEspTraceEnsemble() Command");
	}
	else {
		setTitle("Edit ReadNwsrfsEspTraceEnsemble() Command");
	}
	
	__ok_JButton = new SimpleJButton("OK", this);
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add ( __ok_JButton );
	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

    pack();
    JGUIUtil.center( this );
    //	refresh();	// Sets the __path_JButton status
	refreshPathControl();	// Sets the __path_JButton status
	setResizable ( false );
    super.setVisible( true );
}

/**
Respond to KeyEvents.
*/
public void keyPressed(KeyEvent event) {
	int code = event.getKeyCode();
	if (code == KeyEvent.VK_ENTER) {
		refresh();
		checkInput();
		if (!__error_wait) {
			response(true);
		}
	}
}

/**
Need this to properly capture key events, especially deletes.
*/
public void keyReleased(KeyEvent event) {
	refresh();
}

public void keyTyped(KeyEvent event) {
	refresh();
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
private void refresh()
{
	String InputFile = "";
	       //InputStart = "",
	       //InputEnd = "",
	       //NewUnits = "",
	String Alias = "";
	String Read24HourAsDay = "";
    String EnsembleID = "";
    String EnsembleName = "";

	PropList props = null;

	if (__first_time) {
		__first_time = false;

		// Get the properties from the command
		props = __command.getCommandParameters();
		InputFile = props.getValue("InputFile");
        EnsembleID = props.getValue ( "EnsembleID" );
        EnsembleName = props.getValue("EnsembleName");
		//InputStart = props.getValue("InputStart");
		//InputEnd = props.getValue("InputEnd");
		//NewUnits = props.getValue("NewUnits");
		Alias = props.getValue("Alias");
		Read24HourAsDay = props.getValue("Read24HourAsDay");

		// Set the control fields
		if (Alias != null && (__Alias_JTextField != null) ) {
			__Alias_JTextField.setText(Alias.trim());
		}
		if (InputFile != null) {
			__InputFile_JTextField.setText(InputFile);
		}
        if ( EnsembleID != null ) {
            __EnsembleID_JTextField.setText ( EnsembleID );
        }
        if (EnsembleName != null && !__isAliasVersion) {
            __EnsembleName_JTextField.setText(EnsembleName.trim());
        }
        /*
		if (InputStart != null) {
			__InputStart_JTextField.setText(InputStart);
		}
		if (InputEnd != null) {
			__InputEnd_JTextField.setText(InputEnd);
		}
		if (NewUnits != null) {
			__NewUnits_JTextField.setText(NewUnits);
		}*/
		if (Read24HourAsDay != null) {
			if (Read24HourAsDay.equalsIgnoreCase("true")) {
				__Read24HourAsDay_JComboBox.select(__command._TRUE);
			}
			else if (Read24HourAsDay.equalsIgnoreCase("false")) {
				__Read24HourAsDay_JComboBox.select(__command._FALSE);
			}
			else {
				__Read24HourAsDay_JComboBox.select(0);
			}
		}
	}

	// Regardless, reset the command from the fields.  This is only  visible
	// information that has not been committed in the command.
	InputFile = __InputFile_JTextField.getText().trim();
    EnsembleID = __EnsembleID_JTextField.getText().trim();
    EnsembleName = __EnsembleName_JTextField.getText().trim();
    /*
	InputStart = __InputStart_JTextField.getText().trim();
	InputEnd = __InputEnd_JTextField.getText().trim();
	NewUnits = __NewUnits_JTextField.getText().trim();
	*/
	Read24HourAsDay = __Read24HourAsDay_JComboBox.getSelected().trim();
	if ( __Alias_JTextField != null ) {
		Alias = __Alias_JTextField.getText().trim();
	}

	props = new PropList(__command.getCommandName());
	props.add ( "InputFile=" + InputFile);
    props.add ( "EnsembleID=" + EnsembleID );
    props.add("EnsembleName=" + EnsembleName);
    /*
	props.add("InputStart=" + InputStart);
	props.add("InputEnd=" + InputEnd);
	props.add("NewUnits=" + NewUnits);
	*/
	props.add("Read24HourAsDay=" + Read24HourAsDay);
	if (Alias != null) {
		props.add("Alias=" + Alias);
	}
	
	__Command_JTextArea.setText( __command.toString(props) );

	// Refresh the Path Control text.
	refreshPathControl();
}

/**
Refresh the PathControl text based on the contents of the input text field contents.
*/
private void refreshPathControl()
{
	String InputFile = __InputFile_JTextField.getText().trim();
	if ( (InputFile == null) || (InputFile.trim().length() == 0) ) {
		if ( __path_JButton != null ) {
			__path_JButton.setEnabled ( false );
		}
		return;
	}

	// Check the path and determine what the label on the path button should be...
	if ( __path_JButton != null ) {
		__path_JButton.setEnabled ( true );
		File f = new File ( InputFile );
		if ( f.isAbsolute() ) {
			__path_JButton.setText(	__RemoveWorkingDirectory );
			__path_JButton.setToolTipText("Change path to relative to command file");
		}
		else {
            __path_JButton.setText(	__AddWorkingDirectory );
			__path_JButton.setToolTipText("Change path to absolute");
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
Responds to WindowEvents.
@param event WindowEvent object
*/
public void windowClosing( WindowEvent event )
{	response(false);
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
