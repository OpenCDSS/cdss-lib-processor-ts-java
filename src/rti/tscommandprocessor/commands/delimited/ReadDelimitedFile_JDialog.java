package rti.tscommandprocessor.commands.delimited;

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
import java.io.File;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;

import RTi.Util.IO.Command;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;

import RTi.Util.Message.Message;

/**
Editor for the TS Alias = readDateValue() and non-TS Alias ReadDateValue() commands.
*/
public class ReadDelimitedFile_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton	__browse_JButton = null,// File browse button
			__path_JButton = null,	// Convert between relative and absolute path.
			__cancel_JButton = null,// Cancel Button
			__ok_JButton = null;	// Ok Button
private ReadDelimitedFile_Command __command = null;   // Command to edit
private String __working_dir = null;	// Working directory.
private JTextField __Alias_JTextField = null;
//            __TSID_JTextField = null,
private JTextField __InputFile_JTextField = null;
private JTextField __Delimiter_JTextField = null;
private SimpleJComboBox __TreatConsecutiveDelimitersAsOne_JComboBox = null;
private JTextField __ColumnNames_JTextField = null;
private JTextField __DateColumn_JTextField = null;
private JTextField __TimeColumn_JTextField = null;
private JTextField __DateTimeColumn_JTextField = null;
private JTextField __ValueColumn_JTextField = null;
private JTextField __LocationID_JTextField = null;
private JTextField __DataProviderID_JTextField = null;
private JTextField __DataType_JTextField = null;
private JTextField __Interval_JTextField = null;
private JTextField __Scenario_JTextField = null;
private JTextField __Units_JTextField = null;
private JTextField __MissingValue_JTextField = null;
private JTextField __SkipRows_JTextField = null;
//private JTextField __InputStart_JTextField = null;
//private JTextField __InputEnd_JTextField = null;
private JTextArea __Command_JTextArea = null;
private boolean __error_wait = false;	// Is there an error to be cleared up or Cancel?
private boolean __first_time = true;

//private boolean __isAliasVersion = false;	
			// Whether this dialog is being opened for the version
			// of the command that returns an alias or not
            // TODO SAM 2008-01-31 Evaluate whether alias version is appropriate - for now don't implement
private boolean		__ok = false;			
private final String __RemoveWorkingDirectory = "Remove Working Directory",
	__AddWorkingDirectory = "Add Working Directory";

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadDelimitedFile_JDialog ( JFrame parent, Command command )
{
	super(parent, true);

	/*
	PropList props = command.getCommandParameters();
	String alias = props.getValue("Alias");
	Message.printStatus(1, "", "Props: " + props.toString("\n"));
	if (alias == null || alias.trim().equalsIgnoreCase("")) {
        if (((ReadDelimitedFile_Command)command).getCommandString().trim().toUpperCase().startsWith("TS ")) {
            __isAliasVersion = true;
		}
		else {
			__isAliasVersion = false;
		}
	}
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
        fc.setDialogTitle( "Select Delimited Time Series File");
        SimpleFileFilter sff = new SimpleFileFilter("txt","Delimited Time Series File");
        fc.addChoosableFileFilter(sff);
        sff = new SimpleFileFilter("csv","Delimited (Comma Separated Value) Time Series File");
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
				__InputFile_JTextField.setText(path );
				JGUIUtil.setLastFileDialogDirectory( directory);
				refresh();
			}
		}
	}
	else if ( o == __cancel_JButton ) {
		response(false);
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput ();
		if (!__error_wait) {
			response(true);
		}
	}
	else if ( o == __path_JButton ) {
		if ( __path_JButton.getText().equals( __AddWorkingDirectory) ) {
			__InputFile_JTextField.setText ( IOUtil.toAbsolutePath(__working_dir,__InputFile_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals( __RemoveWorkingDirectory) ) {
			try {
                __InputFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,	__InputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, "ReadDelimitedFile_JDialog", "Error converting file to relative path." );
			}
		}
		refresh ();
	}
}

/**
Check the GUI state to make sure that appropriate components are enabled/disabled.
*/
private void checkGUIState ()
{
    // Add to this as more functionality is added
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput () {
	
	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	String InputFile = __InputFile_JTextField.getText().trim();
	String SkipRows = __SkipRows_JTextField.getText().trim();
	String Delimiter = __Delimiter_JTextField.getText().trim();
	String TreatConsecutiveDelimitersAsOne = __TreatConsecutiveDelimitersAsOne_JComboBox.getSelected();
	String ColumnNames = __ColumnNames_JTextField.getText().trim();
	String DateColumn = __DateColumn_JTextField.getText().trim();
	String TimeColumn = __TimeColumn_JTextField.getText().trim();
	String DateTimeColumn = __DateTimeColumn_JTextField.getText().trim();
	String ValueColumn = __ValueColumn_JTextField.getText().trim();
	String LocationID = __LocationID_JTextField.getText().trim();
	String DataProviderID = __DataProviderID_JTextField.getText().trim();
	String DataType = __DataType_JTextField.getText().trim();
	String Interval = __Interval_JTextField.getText().trim();
	String Scenario = __Scenario_JTextField.getText().trim();
	String Units = __Units_JTextField.getText().trim();
	String MissingValue = __MissingValue_JTextField.getText().trim();
	String Alias = __Alias_JTextField.getText().trim();

	//String InputStart = __InputStart_JTextField.getText().trim();
	//String InputEnd = __InputEnd_JTextField.getText().trim();
	/*
	String Alias = null;
    String TSID = null;
	if (__isAliasVersion) { 
		Alias = __Alias_JTextField.getText().trim();
        TSID = __TSID_JTextField.getText().trim();
        if (Alias != null && Alias.length() > 0) {
            props.set("Alias", Alias);
        }
        if (TSID != null && TSID.length() > 0) {
            props.set("TSID", TSID);
        }
	}
	*/
	
	__error_wait = false;
	
	if (InputFile.length() > 0) {
		props.set("InputFile", InputFile);
	}
    if (SkipRows.length() > 0) {
        props.set("SkipRows", SkipRows);
    }
    if (Delimiter.length() > 0) {
        props.set("Delimiter", Delimiter);
    }
    if (TreatConsecutiveDelimitersAsOne.length() > 0) {
        props.set("TreatConsecutiveDelimitersAsOne", TreatConsecutiveDelimitersAsOne);
    }
    if (ColumnNames.length() > 0) {
        props.set("ColumnNames", ColumnNames);
    }
    if (DateColumn.length() > 0) {
        props.set("DateColumn", DateColumn);
    }
    if (TimeColumn.length() > 0) {
        props.set("TimeColumn", TimeColumn);
    }
    if (DateTimeColumn.length() > 0) {
        props.set("DateTimeColumn", DateTimeColumn);
    }
    if (ValueColumn.length() > 0) {
        props.set("ValueColumn", ValueColumn);
    }
    if (LocationID.length() > 0) {
        props.set("LocationID", LocationID);
    }
    if (DataProviderID.length() > 0) {
        props.set("DataProviderID", DataProviderID);
    }
    if (DataType.length() > 0) {
        props.set("DataType", DataType);
    }
    if (Interval.length() > 0) {
        props.set("Interval", Interval);
    }
    if (Scenario.length() > 0) {
        props.set("Scenario", Scenario);
    }
    if (Units.length() > 0) {
        props.set("Units", Units);
    }
    if (MissingValue.length() > 0) {
        props.set("MissingValue", MissingValue);
    }
    if (Alias.length() > 0) {
        props.set("Alias", Alias);
    }

    /*
	if (InputStart.length() > 0 ) {
		props.set("InputStart", InputStart);
	}
	if (InputEnd.length() > 0 ) {
		props.set("InputEnd", InputEnd);
	}
	*/

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
private void commitEdits() {
	String InputFile = __InputFile_JTextField.getText().trim();
	String SkipRows = __SkipRows_JTextField.getText().trim();
    String Delimiter = __Delimiter_JTextField.getText().trim();
    String TreatConsecutiveDelimitersAsOne = __TreatConsecutiveDelimitersAsOne_JComboBox.getSelected();
    String ColumnNames = __ColumnNames_JTextField.getText().trim();
    String DateColumn = __DateColumn_JTextField.getText().trim();
    String TimeColumn = __TimeColumn_JTextField.getText().trim();
    String DateTimeColumn = __DateTimeColumn_JTextField.getText().trim();
    String ValueColumn = __ValueColumn_JTextField.getText().trim();
    String LocationID = __LocationID_JTextField.getText().trim();
    String DataProviderID = __DataProviderID_JTextField.getText().trim();
    String DataType = __DataType_JTextField.getText().trim();
    String Interval = __Interval_JTextField.getText().trim();
    String Scenario = __Scenario_JTextField.getText().trim();
    String Units = __Units_JTextField.getText().trim();
    String MissingValue = __MissingValue_JTextField.getText().trim();
    String Alias = __Alias_JTextField.getText().trim();
    
	//String InputStart = __InputStart_JTextField.getText().trim();
	//String InputEnd = __InputEnd_JTextField.getText().trim();

	__command.setCommandParameter("InputFile", InputFile);
	__command.setCommandParameter("SkipRows", SkipRows);
	__command.setCommandParameter("Delimiter", Delimiter);
	__command.setCommandParameter("TreatConsecutiveDelimitersAsOne", TreatConsecutiveDelimitersAsOne);
	__command.setCommandParameter("ColumnNames", ColumnNames);
	__command.setCommandParameter("DateColumn", DateColumn);
	__command.setCommandParameter("TimeColumn", TimeColumn);
	__command.setCommandParameter("DateTimeColumn", DateTimeColumn);
	__command.setCommandParameter("ValueColumn", ValueColumn);
	__command.setCommandParameter("LocationID", LocationID);
	__command.setCommandParameter("DataProviderID", DataProviderID );
	__command.setCommandParameter("DataType", DataType);
	__command.setCommandParameter("Interval", Interval);
	__command.setCommandParameter("Scenario", Scenario);
	__command.setCommandParameter("Units", Units);
	__command.setCommandParameter("MissingValue", MissingValue);
	__command.setCommandParameter("Alias", Alias);
	//__command.setCommandParameter("InputStart", InputStart);
	//__command.setCommandParameter("InputEnd", InputEnd);
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable {
	__browse_JButton = null;
	__path_JButton = null;
	__cancel_JButton = null;
	__ok_JButton = null;
	__command = null;
	__working_dir = null;
	__Alias_JTextField = null;
	//__InputStart_JTextField = null;
	//__InputEnd_JTextField = null;
	__InputFile_JTextField = null;
	__Command_JTextArea = null;

	super.finalize();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param app_PropList Properties from application.
@param command Command to edit.
*/
private void initialize(JFrame parent, Command command) {
	__command = (ReadDelimitedFile_Command)command;
	CommandProcessor processor = __command.getCommandProcessor();
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;
	
	JGUIUtil.addComponent(main_JPanel, new JLabel (
            "<HTML><B>This command is under development.</B></HTML>."),
            0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	/*
	if (__isAliasVersion) {
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Read one time series from a delimited format file that is column-oriented."),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	else {
	*/
        JGUIUtil.addComponent(main_JPanel, new JLabel (
            "Read all the time series from a column-oriented delimited file, using " +
            "provided information to assign the time series metadata."),
            0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	//}
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify a full path or relative path (relative to working " +
		"directory) for a delimited file to read." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The working directory is: " + __working_dir ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}

	/*
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specifying the input period will limit data that are " +
		"available for fill commands but can increase performance." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
		*/

    /*
	if (__isAliasVersion) {
	    JGUIUtil.addComponent(main_JPanel, 
			new JLabel("Time series alias:"),
			0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
		__Alias_JTextField = new JTextField ( 30 );
		__Alias_JTextField.addKeyListener ( this );
		JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
			1, y, 3, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	}
	*/

    JGUIUtil.addComponent(main_JPanel, new JLabel (	"Delimited file to read:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputFile_JTextField = new JTextField ( 50 );
	__InputFile_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __InputFile_JTextField,
		1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ( "Browse", this );
        JGUIUtil.addComponent(main_JPanel, __browse_JButton,
		6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Rows to skip:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SkipRows_JTextField = new JTextField (10);
    __SkipRows_JTextField.addKeyListener (this);
        JGUIUtil.addComponent(main_JPanel, __SkipRows_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Specify as comma-separated numbers or ranges."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Delimiter:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Delimiter_JTextField = new JTextField (10);
    __Delimiter_JTextField.addKeyListener (this);
        JGUIUtil.addComponent(main_JPanel, __Delimiter_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Default is \",\", use \\t for tab."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Treat consecutive delimiters as one?:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TreatConsecutiveDelimitersAsOne_JComboBox = new SimpleJComboBox ( false );
    __TreatConsecutiveDelimitersAsOne_JComboBox.addItem ( "" );
    __TreatConsecutiveDelimitersAsOne_JComboBox.addItem ( __command._False );
    __TreatConsecutiveDelimitersAsOne_JComboBox.addItem ( __command._True );
    __TreatConsecutiveDelimitersAsOne_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __TreatConsecutiveDelimitersAsOne_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Default=False."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Column names:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ColumnNames_JTextField = new JTextField (10);
    __ColumnNames_JTextField.addKeyListener (this);
        JGUIUtil.addComponent(main_JPanel, __ColumnNames_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Used by other parameters."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Date/time column:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DateTimeColumn_JTextField = new JTextField (10);
    __DateTimeColumn_JTextField.addKeyListener (this);
        JGUIUtil.addComponent(main_JPanel, __DateTimeColumn_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Specify if date and time are in the same column."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JTextField __DateTimeFormat_JTextField;
    JLabel DateTimeFormat_JLabel = new JLabel ("Date/time format:");
    DateTimeFormat_JLabel.setEnabled( false );
    JGUIUtil.addComponent(main_JPanel, DateTimeFormat_JLabel,
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DateTimeFormat_JTextField = new JTextField (10);
    __DateTimeFormat_JTextField.setEnabled( false );
    __DateTimeFormat_JTextField.addKeyListener (this);
        JGUIUtil.addComponent(main_JPanel, __DateTimeFormat_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Indiate date/time format MM/DD/YYYY, etc. - under development."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JLabel DateColumn_JLabel = new JLabel ("Date column:");
    DateColumn_JLabel.setEnabled( false );
    JGUIUtil.addComponent(main_JPanel, DateColumn_JLabel,
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DateColumn_JTextField = new JTextField (10);
    __DateColumn_JTextField.setEnabled( false );
    __DateColumn_JTextField.addKeyListener (this);
        JGUIUtil.addComponent(main_JPanel, __DateColumn_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Specify if date and time are in separate columns."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JLabel TimeColumn_JLabel = new JLabel ("Time column:");
    TimeColumn_JLabel.setEnabled( false );
    JGUIUtil.addComponent(main_JPanel, TimeColumn_JLabel,
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TimeColumn_JTextField = new JTextField (10);
    __TimeColumn_JTextField.setEnabled( false);
    __TimeColumn_JTextField.addKeyListener (this);
        JGUIUtil.addComponent(main_JPanel, __TimeColumn_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Specify if date and time are in separate columns."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Value column(s):"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ValueColumn_JTextField = new JTextField (10);
    __ValueColumn_JTextField.addKeyListener (this);
        JGUIUtil.addComponent(main_JPanel, __ValueColumn_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Specify column names separated by commas."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Location ID(s):"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LocationID_JTextField = new JTextField (10);
    __LocationID_JTextField.addKeyListener (this);
        JGUIUtil.addComponent(main_JPanel, __LocationID_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Specify location ID for each value column, separated by commas."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Data provider ID:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataProviderID_JTextField = new JTextField (10);
    __DataProviderID_JTextField.addKeyListener (this);
        JGUIUtil.addComponent(main_JPanel, __DataProviderID_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Specify data provider ID for the data."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Data type(s):"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataType_JTextField = new JTextField (10);
    __DataType_JTextField.addKeyListener (this);
        JGUIUtil.addComponent(main_JPanel, __DataType_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Data type for each value column, separated by commas."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Data interval:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Interval_JTextField = new JTextField (10);
    __Interval_JTextField.addKeyListener (this);
        JGUIUtil.addComponent(main_JPanel, __Interval_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Data interval for data (e.g., 6Hour, Day)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Scenario:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Scenario_JTextField = new JTextField (10);
    __Scenario_JTextField.addKeyListener (this);
        JGUIUtil.addComponent(main_JPanel, __Scenario_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Scenario for the time series (specify none or one)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("Units of data:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Units_JTextField = new JTextField ( "", 10 );
	__Units_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel, __Units_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Specify for each time series, separated by commas."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Missing value:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MissingValue_JTextField = new JTextField (10);
    __MissingValue_JTextField.addKeyListener (this);
        JGUIUtil.addComponent(main_JPanel, __MissingValue_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Missing value indicator (if missing values are in file)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Alias to assign:" ), 
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new JTextField ( "", 20 );
    __Alias_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Default is no alias is assigned."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	/* FIXME SAM 2008-01-31 Enable when functionality of low-level code is confirmed
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
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
	refresh ();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__path_JButton = new SimpleJButton(	__RemoveWorkingDirectory, this);
		button_JPanel.add ( __path_JButton );
	}
	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );

	/*
	if (__isAliasVersion) {
		setTitle("Edit TS Alias = ReadDateValue() Command");
	}
	else {*/
		setTitle("Edit ReadDelimitedFile() Command");
	//}

	setResizable ( true );
    pack();
    JGUIUtil.center( this );
//	refresh();	// Sets the __path_JButton status
	refreshPathControl();	// Sets the __path_JButton status
    super.setVisible( true );
}


/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent e )
{   checkGUIState();
    refresh();
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
@return true if the edits were committed, false if the user cancelled.
*/
public boolean ok() {
	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh()
{   String routine = "ReadDelimitedFile_JDialog.refresh";
    String InputFile = "";
    String SkipRows = "";
    String Delimiter = "";
    String TreatConsecutiveDelimitersAsOne = "";
    String ColumnNames = "";
    String DateColumn = "";
    String TimeColumn = "";
    String DateTimeColumn = "";
    String ValueColumn = "";
    String LocationID = "";
    String DataProviderID = "";
    String DataType = "";
    String Interval = "";
    String Scenario = "";
    String Units = "";
    String MissingValue = "";
    String Alias = "";
    String InputStart = "";
    String InputEnd = "";

	PropList props = null;

	if (__first_time) {
		__first_time = false;

		// Get the properties from the command
		props = __command.getCommandParameters();
		/*
        if ( __isAliasVersion ) {
            Alias = props.getValue("Alias");
            TSID = props.getValue("TSID");
        }
        */
		InputFile = props.getValue("InputFile");
		SkipRows = props.getValue("SkipRows");
	    Delimiter = props.getValue("Delimiter");
	    TreatConsecutiveDelimitersAsOne = props.getValue("TreatConsecutiveDelimitersAsOne");
	    ColumnNames = props.getValue("ColumnNames");
	    DateColumn = props.getValue("DateColumn");
	    TimeColumn = props.getValue("TimeColumn");
	    DateTimeColumn = props.getValue("DateTimeColumn");
	    ValueColumn = props.getValue("ValueColumn");
	    LocationID = props.getValue("LocationID");
	    DataProviderID = props.getValue("DataProviderID");
	    DataType = props.getValue("DataType");
	    Interval = props.getValue("Interval");
	    Scenario = props.getValue("Scenario");
	    Units = props.getValue("Units");
	    MissingValue = props.getValue("MissingValue");
	    Alias = props.getValue("Alias");
		InputStart = props.getValue("InputStart");
		InputEnd = props.getValue("InputEnd");
		// Set the control fields
		/*
        if (TSID != null && __isAliasVersion) {
            __TSID_JTextField.setText(TSID.trim());
        }
        */
		if (InputFile != null) {
			__InputFile_JTextField.setText(InputFile);
		}
        if ( SkipRows != null) {
            __SkipRows_JTextField.setText(SkipRows);
        }
	    if (Delimiter != null) {
	         __Delimiter_JTextField.setText(Delimiter);
	    }
        if ( TreatConsecutiveDelimitersAsOne == null ) {
            // Select default...
            __TreatConsecutiveDelimitersAsOne_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem(
                __TreatConsecutiveDelimitersAsOne_JComboBox,
                TreatConsecutiveDelimitersAsOne, JGUIUtil.NONE, null, null ) ) {
                __TreatConsecutiveDelimitersAsOne_JComboBox.select ( TreatConsecutiveDelimitersAsOne );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTreatConsecutiveDelimitersAsOne value \"" +
                TreatConsecutiveDelimitersAsOne + "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if (ColumnNames != null) {
            __ColumnNames_JTextField.setText(ColumnNames);
        }
        if (DateColumn != null) {
            __DateColumn_JTextField.setText(DateColumn);
        }
        if (TimeColumn != null) {
            __TimeColumn_JTextField.setText(TimeColumn);
        }
        if (DateTimeColumn != null) {
            __DateTimeColumn_JTextField.setText(DateTimeColumn);
        }
        if (ValueColumn != null) {
            __ValueColumn_JTextField.setText(ValueColumn);
        }
        if (LocationID != null) {
            __LocationID_JTextField.setText(LocationID);
        }
        if (DataProviderID != null) {
            __DataProviderID_JTextField.setText(DataProviderID);
        }
        if (DataType != null) {
            __DataType_JTextField.setText(DataType);
        }
        if (Interval != null) {
            __Interval_JTextField.setText(Interval);
        }
        if (Scenario != null) {
            __Scenario_JTextField.setText(Scenario);
        }
        if (Units != null) {
            __Units_JTextField.setText(Units);
        }
        if (MissingValue != null) {
            __MissingValue_JTextField.setText(MissingValue);
        }
        if ( Alias != null ) {
            __Alias_JTextField.setText(Alias.trim());
        }
        /*
		if (InputStart != null) {
			__InputStart_JTextField.setText(InputStart);
		}
		if (InputEnd != null) {
			__InputEnd_JTextField.setText(InputEnd);
		}
		*/
	}

	// Regardless, reset the command from the fields.  This is only  visible
	// information that has not been committed in the command.
	InputFile = __InputFile_JTextField.getText().trim();
    SkipRows = __SkipRows_JTextField.getText().trim();
    Delimiter = __Delimiter_JTextField.getText().trim();
    TreatConsecutiveDelimitersAsOne = __TreatConsecutiveDelimitersAsOne_JComboBox.getSelected();
    ColumnNames = __ColumnNames_JTextField.getText().trim();
    DateColumn = __DateColumn_JTextField.getText().trim();
    TimeColumn = __TimeColumn_JTextField.getText().trim();
    DateTimeColumn = __DateTimeColumn_JTextField.getText().trim();
    ValueColumn = __ValueColumn_JTextField.getText().trim();
    LocationID = __LocationID_JTextField.getText().trim();
    DataProviderID = __DataProviderID_JTextField.getText().trim();
    DataType = __DataType_JTextField.getText().trim();
    Interval = __Interval_JTextField.getText().trim();
    Scenario = __Scenario_JTextField.getText().trim();
    Units = __Units_JTextField.getText().trim();
    MissingValue = __MissingValue_JTextField.getText().trim();
    Alias = __Alias_JTextField.getText().trim();
	//InputStart = __InputStart_JTextField.getText().trim();
	//InputEnd = __InputEnd_JTextField.getText().trim();
	/*
	if (__isAliasVersion) {
		Alias = __Alias_JTextField.getText().trim();
        TSID = __TSID_JTextField.getText().trim();
	}
	*/

	props = new PropList(__command.getCommandName());
	props.add("InputFile=" + InputFile);
	props.add("SkipRows=" + SkipRows );
    props.add("Delimiter=" + Delimiter );
    props.add("TreatConsecutiveDelimitersAsOne=" + TreatConsecutiveDelimitersAsOne );
    props.add("ColumnNames=" + ColumnNames );
    props.add("DateColumn=" + DateColumn );
    props.add("TimeColumn=" + TimeColumn );
    props.add("DateTimeColumn=" + DateTimeColumn );
    props.add("ValueColumn=" + ValueColumn );
    props.add("LocationID=" + LocationID );
    props.add("DataProviderID=" + DataProviderID );
    props.add("DataType=" + DataType );
    props.add("Interval=" + Interval );
    props.add("Scenario=" + Scenario );
    props.add("Units=" + Units );
    props.add("MissingValue=" + MissingValue );
    props.add("Alias=" + Alias );
	props.add("InputStart=" + InputStart);
	props.add("InputEnd=" + InputEnd);
	/*
    if (TSID != null) {
        props.add("TSID=" + TSID);
    }
    */
	
	__Command_JTextArea.setText( __command.toString(props) );

	// Refresh the Path Control text.
	refreshPathControl();
}

/**
Refresh the PathControl text based on the contents of the input text field
contents.
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

	// Check the path and determine what the label on the path button should
	// be...
	if ( __path_JButton != null ) {
		__path_JButton.setEnabled ( true );
		File f = new File ( InputFile );
		if ( f.isAbsolute() ) {
			__path_JButton.setText(	__RemoveWorkingDirectory );
		}
		else {
            __path_JButton.setText(	__AddWorkingDirectory );
		}
	}
}

/**
React to the user response.
@param ok if false, then the edit is cancelled.  If true, the edit is committed
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
