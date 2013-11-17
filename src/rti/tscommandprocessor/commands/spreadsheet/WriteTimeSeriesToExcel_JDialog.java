package rti.tscommandprocessor.commands.spreadsheet;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

import java.awt.Color;
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
import java.util.List;

import RTi.Util.GUI.DictionaryJDialog;
import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Editor for the WriteTimeSeriesToExcel command.
*/
public class WriteTimeSeriesToExcel_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{

// Used for button labels...

private final String __AddWorkingDirectoryToFile = "Add Working Directory To File";
private final String __RemoveWorkingDirectoryFromFile = "Remove Working Directory From File";

private boolean __error_wait = false; // To track errors
private boolean __first_time = true;
private JTextArea __command_JTextArea=null;
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private JTextField __OutputFile_JTextField = null;
private JTextField __Worksheet_JTextField = null;
private JTabbedPane __excelSpace_JTabbedPane = null;
private JTextField __ExcelAddress_JTextField = null;
private JTextField __ExcelNamedRange_JTextField = null;
private JTextField __ExcelTableName_JTextField = null;
private JTextField __Comment_JTextField = null;
private SimpleJComboBox __ExcelColumnNames_JComboBox = null;
private JTextArea __ColumnExcludeFilters_JTextArea = null;
private JTextField __ExcelIntegerColumns_JTextField = null;
private JTextField __ExcelDateTimeColumns_JTextField = null;
private JTextField __NumberPrecision_JTextField = null;
private SimpleJComboBox __WriteAllAsText_JComboBox = null;
private JTextField __MissingValue_JTextField = null;// Missing value for output
private JTextField __OutputStart_JTextField = null;
private JTextField __OutputEnd_JTextField = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;	
private SimpleJButton __browse_JButton = null;
private SimpleJButton __path_JButton = null;
private String __working_dir = null;	
private WriteTimeSeriesToExcel_Command __command = null;
private boolean __ok = false;
private JFrame __parent = null;

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of table identifiers to provide as choices
*/
public WriteTimeSeriesToExcel_JDialog ( JFrame parent, WriteTimeSeriesToExcel_Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed(ActionEvent event)
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
		fc.setDialogTitle("Select Excel File");
     	fc.addChoosableFileFilter(new SimpleFileFilter("xls", "Excel File"));
		SimpleFileFilter sff = new SimpleFileFilter("xlsx", "Excel File");
		fc.addChoosableFileFilter(sff);
		fc.setFileFilter(sff);

		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			String directory = fc.getSelectedFile().getParent();
			String path = fc.getSelectedFile().getPath();
			__OutputFile_JTextField.setText(path);
			JGUIUtil.setLastFileDialogDirectory(directory);
			refresh ();
		}
	}
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput ();
		if ( !__error_wait ) {
			// Command has been edited...
			response ( true );
		}
	}
	else if ( o == __path_JButton ) {
		if (__path_JButton.getText().equals( __AddWorkingDirectoryToFile)) {
			__OutputFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir,__OutputFile_JTextField.getText()));
		}
		else if (__path_JButton.getText().equals( __RemoveWorkingDirectoryFromFile)) {
			try {
                __OutputFile_JTextField.setText ( IOUtil.toRelativePath (__working_dir,
                        __OutputFile_JTextField.getText()));
			}
			catch (Exception e) {
				Message.printWarning (1, 
				__command.getCommandName() + "_JDialog", "Error converting file to relative path.");
			}
		}
		refresh ();
	}
    else if ( event.getActionCommand().equalsIgnoreCase("EditColumnExcludeFilters") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String ColumnExcludeFilters = __ColumnExcludeFilters_JTextArea.getText().trim();
        String dict = (new DictionaryJDialog ( __parent, true, ColumnExcludeFilters,
            "Edit ColumnExcludeFilters Parameter", null, "Table Column", "Pattern to exclude rows (* allowed)",10)).response();
        if ( dict != null ) {
            __ColumnExcludeFilters_JTextArea.setText ( dict );
            refresh();
        }
    }
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
    String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String Worksheet = __Worksheet_JTextField.getText().trim();
	String ExcelAddress = __ExcelAddress_JTextField.getText().trim();
	String ExcelNamedRange = __ExcelNamedRange_JTextField.getText().trim();
	String ExcelTableName = __ExcelTableName_JTextField.getText().trim();
	String Comment = __Comment_JTextField.getText().trim();
	String ExcelColumnNames  = __ExcelColumnNames_JComboBox.getSelected();
	String ColumnExcludeFilters  = __ColumnExcludeFilters_JTextArea.getText().trim();
	String ExcelIntegerColumns  = __ExcelIntegerColumns_JTextField.getText().trim();
	String ExcelDateTimeColumns  = __ExcelDateTimeColumns_JTextField.getText().trim();
	String NumberPrecision  = __NumberPrecision_JTextField.getText().trim();
	String ReadAllAsText  = __WriteAllAsText_JComboBox.getSelected();
	String MissingValue = __MissingValue_JTextField.getText().trim();
    String OutputStart = __OutputStart_JTextField.getText().trim();
    String OutputEnd = __OutputEnd_JTextField.getText().trim();
	__error_wait = false;

    if ( TSList.length() > 0 ) {
        props.set ( "TSList", TSList );
    }
    if ( TSID.length() > 0 ) {
        props.set ( "TSID", TSID );
    }
    if ( EnsembleID.length() > 0 ) {
        props.set ( "EnsembleID", EnsembleID );
    }
	if ( OutputFile.length() > 0 ) {
		props.set ( "OutputFile", OutputFile );
	}
    if ( Worksheet.length() > 0 ) {
        props.set ( "Worksheet", Worksheet );
    }
	if ( ExcelAddress.length() > 0 ) {
		props.set ( "ExcelAddress", ExcelAddress );
	}
	if ( ExcelNamedRange.length() > 0 ) {
		props.set ( "ExcelNamedRange", ExcelNamedRange );
	}
    if ( ExcelTableName.length() > 0 ) {
        props.set ( "ExcelTableName", ExcelTableName );
    }
    if ( ExcelColumnNames.length() > 0 ) {
        props.set ( "ExcelColumnNames", ExcelColumnNames );
    }
    if ( ColumnExcludeFilters.length() > 0 ) {
        props.set ( "ColumnExcludeFilters", ColumnExcludeFilters );
    }
    if (Comment.length() > 0) {
        props.set("Comment", Comment);
    }
    if ( ExcelIntegerColumns.length() > 0 ) {
        props.set ( "ExcelIntegerColumns", ExcelIntegerColumns );
    }
    if ( ExcelDateTimeColumns.length() > 0 ) {
        props.set ( "ExcelDateTimeColumns", ExcelDateTimeColumns );
    }
    if ( NumberPrecision.length() > 0 ) {
        props.set ( "NumberPrecision", NumberPrecision );
    }
    if ( ReadAllAsText.length() > 0 ) {
        props.set ( "ReadAllAsText", ReadAllAsText );
    }
    if ( MissingValue.length() > 0 ) {
        props.set ( "MissingValue", MissingValue );
    }
    if ( OutputStart.length() > 0 ) {
        props.set ( "OutputStart", OutputStart );
    }
    if ( OutputEnd.length() > 0 ) {
        props.set ( "OutputEnd", OutputEnd );
    }
	try {
	    // This will warn the user...
		__command.checkCommandParameters ( props, null, 1 );
	}
	catch ( Exception e ) {
        Message.printWarning(2,"", e);
		// The warning would have been printed in the check code.
		__error_wait = true;
	}
}

/**
Commit the edits to the command.  In this case the command parameters have
already been checked and no errors were detected.
*/
private void commitEdits ()
{	String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String OutputFile = __OutputFile_JTextField.getText().trim();
    String Worksheet = __Worksheet_JTextField.getText().trim();
	String ExcelAddress = __ExcelAddress_JTextField.getText().trim();
	String ExcelNamedRange = __ExcelNamedRange_JTextField.getText().trim();
	String ExcelTableName = __ExcelTableName_JTextField.getText().trim();
	String ExcelColumnNames  = __ExcelColumnNames_JComboBox.getSelected();
	String ColumnExcludeFilters  = __ColumnExcludeFilters_JTextArea.getText().trim();
	String Comment = __Comment_JTextField.getText().trim();
	String ExcelIntegerColumns  = __ExcelIntegerColumns_JTextField.getText().trim();
	String ExcelDateTimeColumns  = __ExcelDateTimeColumns_JTextField.getText().trim();
	String NumberPrecision  = __NumberPrecision_JTextField.getText().trim();
	String ReadAllAsText  = __WriteAllAsText_JComboBox.getSelected();
	String MissingValue = __MissingValue_JTextField.getText().trim();
    String OutputStart = __OutputStart_JTextField.getText().trim();
    String OutputEnd = __OutputEnd_JTextField.getText().trim();
	__command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
	__command.setCommandParameter ( "OutputFile", OutputFile );
	__command.setCommandParameter ( "Worksheet", Worksheet );
	__command.setCommandParameter ( "ExcelAddress", ExcelAddress );
	__command.setCommandParameter ( "ExcelNamedRange", ExcelNamedRange );
	__command.setCommandParameter ( "ExcelTableName", ExcelTableName );
	__command.setCommandParameter ( "ExcelColumnNames", ExcelColumnNames );
	__command.setCommandParameter ( "ColumnExcludeFilters", ColumnExcludeFilters );
	__command.setCommandParameter ( "Comment", Comment );
	__command.setCommandParameter ( "ExcelIntegerColumns", ExcelIntegerColumns );
	__command.setCommandParameter ( "ExcelDateTimeColumns", ExcelDateTimeColumns );
	__command.setCommandParameter ( "NumberPrecision", NumberPrecision );
	__command.setCommandParameter ( "ReadAllAsText", ReadAllAsText );
	__command.setCommandParameter ( "MissingValue", MissingValue );
    __command.setCommandParameter ( "OutputStart", OutputStart );
    __command.setCommandParameter ( "OutputEnd", OutputEnd );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit and possibly run.
*/
private void initialize ( JFrame parent, WriteTimeSeriesToExcel_Command command )
{	__command = command;
    __parent = parent;
	CommandProcessor processor = __command.getCommandProcessor();
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)processor, __command );

	addWindowListener(this);

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout(new GridBagLayout());
	getContentPane().add ("North", main_JPanel);
	int y = -1;

	JPanel paragraph = new JPanel();
	paragraph.setLayout(new GridBagLayout());
	int yy = -1;

    JGUIUtil.addComponent(paragraph, new JLabel (
        "<html><b>This command is in the early stages of devevelopment - DO NOT USE FOR PRODUCTION WORK.<b></html>"),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(paragraph, new JLabel (
    	"This command writes a list of time series to a worksheet in a Microsoft Excel workbook file (*.xls, *.xlsx).  " +
    	"Currently the Excel file must exist."),
    	0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
		"A contiguous block of cells must be specified using one of the address methods below."),
		0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
		"It is recommended that the location of the Excel file be " +
		"specified using a path relative to the working directory."),
		0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);		
    JGUIUtil.addComponent(paragraph, new JLabel ( ""),
		0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);		
	if (__working_dir != null) {
    	JGUIUtil.addComponent(paragraph, new JLabel (
		"The working directory is: " + __working_dir), 
		0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}

	JGUIUtil.addComponent(main_JPanel, paragraph,
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
    __TSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel ( this, main_JPanel, __TSList_JComboBox, y );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, y );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
        this, this, main_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Output (workbook) file:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputFile_JTextField = new JTextField (45);
	__OutputFile_JTextField.addKeyListener (this);
        JGUIUtil.addComponent(main_JPanel, __OutputFile_JTextField,
		1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ("Browse", this);
        JGUIUtil.addComponent(main_JPanel, __browse_JButton,
		6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Worksheet:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Worksheet_JTextField = new JTextField (30);
    __Worksheet_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __Worksheet_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel,
        new JLabel ("Required (if not in address) - worksheet name (default=first sheet)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    __excelSpace_JTabbedPane = new JTabbedPane ();
    __excelSpace_JTabbedPane.setBorder(
        BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
        "Specify the address for a contigous block of cells the in Excel worksheet" ));
    JGUIUtil.addComponent(main_JPanel, __excelSpace_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JPanel address_JPanel = new JPanel();
    address_JPanel.setLayout(new GridBagLayout());
    __excelSpace_JTabbedPane.addTab ( "by Excel Address", address_JPanel );
    int yAddress = -1;
        
    JGUIUtil.addComponent(address_JPanel, new JLabel ("Excel address:"),
        0, ++yAddress, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ExcelAddress_JTextField = new JTextField (10);
    __ExcelAddress_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(address_JPanel, __ExcelAddress_JTextField,
        1, yAddress, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(address_JPanel, new JLabel ("Excel cell block address in format A1:B2."),
        3, yAddress, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JPanel range_JPanel = new JPanel();
    range_JPanel.setLayout(new GridBagLayout());
    __excelSpace_JTabbedPane.addTab ( "by Named Range", range_JPanel );
    int yRange = -1;
    
    JGUIUtil.addComponent(range_JPanel, new JLabel ("Excel named range:"),
        0, ++yRange, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ExcelNamedRange_JTextField = new JTextField (10);
    __ExcelNamedRange_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(range_JPanel, __ExcelNamedRange_JTextField,
        1, yRange, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(range_JPanel, new JLabel ("Excel named range."),
        3, yRange, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JPanel table_JPanel = new JPanel();
    table_JPanel.setLayout(new GridBagLayout());
    __excelSpace_JTabbedPane.addTab ( "by Table Name", table_JPanel );
    int yTable = -1;
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Excel table name:"),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ExcelTableName_JTextField = new JTextField (10);
    __ExcelTableName_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(table_JPanel, __ExcelTableName_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Excel table name."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Excel column names:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ExcelColumnNames_JComboBox = new SimpleJComboBox ( false );
    __ExcelColumnNames_JComboBox.add("");
    __ExcelColumnNames_JComboBox.add(__command._ColumnN);
    __ExcelColumnNames_JComboBox.add(__command._FirstRowInRange);
    __ExcelColumnNames_JComboBox.add(__command._RowBeforeRange);
    __ExcelColumnNames_JComboBox.select ( 0 );
    __ExcelColumnNames_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __ExcelColumnNames_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - how to define column names (default=" +
        __command._ColumnN + ")."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Column filters to exclude rows:"),
        0, ++y, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ColumnExcludeFilters_JTextArea = new JTextArea (3,35);
    __ColumnExcludeFilters_JTextArea.setLineWrap ( true );
    __ColumnExcludeFilters_JTextArea.setWrapStyleWord ( true );
    __ColumnExcludeFilters_JTextArea.setToolTipText("TableColumn:DatastoreColumn,TableColumn:DataStoreColumn");
    __ColumnExcludeFilters_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__ColumnExcludeFilters_JTextArea),
        1, y, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - column patterns to exclude rows (default=include all)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(main_JPanel, new SimpleJButton ("Edit","EditColumnExcludeFilters",this),
        3, ++y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // TODO SAM 2013-11-03 Evaluate whether to enable or remove the following 3 parameters
    //JGUIUtil.addComponent(main_JPanel, new JLabel ("Comment character:"),
    //    0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Comment_JTextField = new JTextField (10);
    //__Comment_JTextField.addKeyListener (this);
    //JGUIUtil.addComponent(main_JPanel, __Comment_JTextField,
    //    1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    //JGUIUtil.addComponent(main_JPanel, new JLabel (
    //    "Optional - character that indicates comment lines (default=none)."),
    //    3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    __Comment_JTextField.setVisible(false);
    
    //JGUIUtil.addComponent(main_JPanel, new JLabel ("Excel integer columns:"),
    //    0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ExcelIntegerColumns_JTextField = new JTextField (20);
    //__ExcelIntegerColumns_JTextField.addKeyListener (this);
    //JGUIUtil.addComponent(main_JPanel, __ExcelIntegerColumns_JTextField,
    //    1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    //JGUIUtil.addComponent(main_JPanel,
    //    new JLabel ("Optional - columns that are integers, separated by commas."),
    //    3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    __ExcelIntegerColumns_JTextField.setVisible(false);
    
    //JGUIUtil.addComponent(main_JPanel, new JLabel ("Excel date/time columns:"),
    //    0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ExcelDateTimeColumns_JTextField = new JTextField (20);
    //__ExcelDateTimeColumns_JTextField.addKeyListener (this);
    //JGUIUtil.addComponent(main_JPanel, __ExcelDateTimeColumns_JTextField,
    //    1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    //JGUIUtil.addComponent(main_JPanel,
    //    new JLabel ("Optional - columns that are date/times, separated by commas."),
    //    3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    __ExcelDateTimeColumns_JTextField.setVisible(false);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Number precision:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NumberPrecision_JTextField = new JTextField (10);
    __NumberPrecision_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __NumberPrecision_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel,
        new JLabel ("Optional - precision for numbers (default=6)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Write all as text?:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __WriteAllAsText_JComboBox = new SimpleJComboBox ( false );
    __WriteAllAsText_JComboBox.add("");
    __WriteAllAsText_JComboBox.add(__command._False);
    __WriteAllAsText_JComboBox.add(__command._True);
    __WriteAllAsText_JComboBox.select ( 0 );
    __WriteAllAsText_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __WriteAllAsText_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - write all cells as text? (default=" + __command._False + ")."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Missing value:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MissingValue_JTextField = new JTextField ( "", 10 );
    __MissingValue_JTextField.setToolTipText("Specify Blank to output a blank.");
    __MissingValue_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __MissingValue_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - value to write for missing data (default=initial missing value)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Output start:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputStart_JTextField = new JTextField (20);
    __OutputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __OutputStart_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - override the global output start (default=write all data)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output end:"), 
        0, ++y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputEnd_JTextField = new JTextField (20);
    __OutputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __OutputEnd_JTextField,
        1, y, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - override the global output end (default=write all data)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Command:"), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (4,40);
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable (false);
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
	refresh ();

	// South JPanel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	if (__working_dir != null) {
		// Add the button to allow conversion to/from relative path...
		__path_JButton = new SimpleJButton(	__RemoveWorkingDirectoryFromFile, this);
		button_JPanel.add (__path_JButton);
	}
	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add (__cancel_JButton);
	__cancel_JButton.setToolTipText ( "Close window without saving changes." );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add (__ok_JButton);
	__ok_JButton.setToolTipText ( "Close window and save changes to command." );

	setTitle ( "Edit " + __command.getCommandName() + "() Command");
	setResizable (false);
    pack();
    JGUIUtil.center(this);
	refresh();	// Sets the __path_JButton status
    super.setVisible(true);
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
public void keyPressed (KeyEvent event) {
	int code = event.getKeyCode();

	if (code == KeyEvent.VK_ENTER) {
		refresh ();
		checkInput();
		if (!__error_wait) {
			response ( true );
		}
	}
}

public void keyReleased (KeyEvent event) {
	refresh();
}

public void keyTyped (KeyEvent event) {}

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
{	String routine = getClass().getName() + ".refresh";
    String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String OutputFile = "";
    String Worksheet = "";
	String ExcelAddress = "";
	String ExcelNamedRange = "";
	String ExcelTableName = "";
	String ExcelColumnNames = "";
	String ColumnExcludeFilters = "";
    String Comment = "";
	String ExcelIntegerColumns = "";
	String ExcelDateTimeColumns = "";
	String NumberPrecision = "";
	String ReadAllAsText = "";
    String MissingValue = "";
    String OutputStart = "";
    String OutputEnd = "";
	PropList props = __command.getCommandParameters();
	if (__first_time) {
		__first_time = false;
	    TSList = props.getValue ( "TSList" );
	    TSID = props.getValue ( "TSID" );
	    EnsembleID = props.getValue ( "EnsembleID" );
		OutputFile = props.getValue ( "OutputFile" );
		Worksheet = props.getValue ( "Worksheet" );
		ExcelAddress = props.getValue ( "ExcelAddress" );
		ExcelNamedRange = props.getValue ( "ExcelNamedRange" );
		ExcelTableName = props.getValue ( "ExcelTableName" );
		ExcelColumnNames = props.getValue ( "ExcelColumnNames" );
		ColumnExcludeFilters = props.getValue ( "ColumnExcludeFilters" );
		Comment = props.getValue ( "Comment" );
		ExcelIntegerColumns = props.getValue ( "ExcelIntegerColumns" );
		ExcelDateTimeColumns = props.getValue ( "ExcelDateTimeColumns" );
		NumberPrecision = props.getValue ( "NumberPrecision" );
		ReadAllAsText = props.getValue ( "ReadAllAsText" );
        MissingValue = props.getValue("MissingValue");
        OutputStart = props.getValue ( "OutputStart" );
        OutputEnd = props.getValue ( "OutputEnd" );
        if ( TSList == null ) {
            // Select default...
            __TSList_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TSList_JComboBox,TSList, JGUIUtil.NONE, null, null ) ) {
                __TSList_JComboBox.select ( TSList );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTSList value \"" + TSList +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID, JGUIUtil.NONE, null, null ) ) {
                __TSID_JComboBox.select ( TSID );
        }
        else {
            // Automatically add to the list after the blank...
            if ( (TSID != null) && (TSID.length() > 0) ) {
                __TSID_JComboBox.insertItemAt ( TSID, 1 );
                // Select...
                __TSID_JComboBox.select ( TSID );
            }
            else {
                // Select the blank...
                __TSID_JComboBox.select ( 0 );
            }
        }
        if ( EnsembleID == null ) {
            // Select default...
            __EnsembleID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __EnsembleID_JComboBox,EnsembleID, JGUIUtil.NONE, null, null ) ) {
                __EnsembleID_JComboBox.select ( EnsembleID );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nEnsembleID value \"" + EnsembleID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
		if ( OutputFile != null ) {
			__OutputFile_JTextField.setText ( OutputFile );
		}
        if ( Worksheet != null ) {
            __Worksheet_JTextField.setText ( Worksheet );
        }
		if ( ExcelAddress != null ) {
			__ExcelAddress_JTextField.setText ( ExcelAddress );
			// Also select the tab to be visible
			__excelSpace_JTabbedPane.setSelectedIndex(0);
		}
		if ( ExcelNamedRange != null ) {
			__ExcelNamedRange_JTextField.setText ( ExcelNamedRange );
			__excelSpace_JTabbedPane.setSelectedIndex(1);
		}
        if ( ExcelTableName != null ) {
            __ExcelTableName_JTextField.setText ( ExcelTableName );
            __excelSpace_JTabbedPane.setSelectedIndex(2);
        }
        if ( ExcelColumnNames == null || ExcelColumnNames.equals("") ) {
            // Select a default...
            __ExcelColumnNames_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __ExcelColumnNames_JComboBox, ExcelColumnNames, JGUIUtil.NONE, null, null ) ) {
                __ExcelColumnNames_JComboBox.select ( ExcelColumnNames );
            }
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid\nExcelColumnNames \"" +
                    ExcelColumnNames + "\".  Select a different choice or Cancel." );
            }
        }
        if ( ColumnExcludeFilters != null ) {
            __ColumnExcludeFilters_JTextArea.setText ( ColumnExcludeFilters );
        }
        if ( Comment != null) {
            __Comment_JTextField.setText(Comment);
        }
        if ( ExcelIntegerColumns != null ) {
            __ExcelIntegerColumns_JTextField.setText ( ExcelIntegerColumns );
        }
        if ( ExcelDateTimeColumns != null ) {
            __ExcelDateTimeColumns_JTextField.setText ( ExcelDateTimeColumns );
        }
        if ( NumberPrecision != null ) {
            __NumberPrecision_JTextField.setText ( NumberPrecision );
        }
        if ( ReadAllAsText == null || ReadAllAsText.equals("") ) {
            // Select a default...
            __WriteAllAsText_JComboBox.select ( 0 );
        } 
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __WriteAllAsText_JComboBox, ReadAllAsText, JGUIUtil.NONE, null, null ) ) {
                __WriteAllAsText_JComboBox.select ( ReadAllAsText );
            }
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid\nReadAllAsText \"" +
                    ReadAllAsText + "\".  Select a different choice or Cancel." );
            }
        }
        if ( MissingValue != null ) {
            __MissingValue_JTextField.setText ( MissingValue );
        }
        if ( OutputStart != null ) {
            __OutputStart_JTextField.setText (OutputStart);
        }
        if ( OutputEnd != null ) {
            __OutputEnd_JTextField.setText (OutputEnd);
        }
	}
	// Regardless, reset the command from the fields...
	TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
	OutputFile = __OutputFile_JTextField.getText().trim();
	Worksheet = __Worksheet_JTextField.getText().trim();
	ExcelAddress = __ExcelAddress_JTextField.getText().trim();
	ExcelNamedRange = __ExcelNamedRange_JTextField.getText().trim();
	ExcelTableName = __ExcelTableName_JTextField.getText().trim();
	ExcelColumnNames = __ExcelColumnNames_JComboBox.getSelected();
	ColumnExcludeFilters = __ColumnExcludeFilters_JTextArea.getText().trim();
	Comment = __Comment_JTextField.getText().trim();
	ExcelIntegerColumns = __ExcelIntegerColumns_JTextField.getText().trim();
	ExcelDateTimeColumns = __ExcelDateTimeColumns_JTextField.getText().trim();
	NumberPrecision = __NumberPrecision_JTextField.getText().trim();
	ReadAllAsText = __WriteAllAsText_JComboBox.getSelected();
    MissingValue = __MissingValue_JTextField.getText().trim();
    OutputStart = __OutputStart_JTextField.getText().trim();
    OutputEnd = __OutputEnd_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "TSList=" + TSList );
    props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
	props.add ( "OutputFile=" + OutputFile );
	props.add ( "Worksheet=" + Worksheet );
	props.add ( "ExcelAddress=" + ExcelAddress );
	props.add ( "ExcelNamedRange=" + ExcelNamedRange );
	props.add ( "ExcelTableName=" + ExcelTableName );
	props.add ( "ExcelColumnNames=" + ExcelColumnNames );
	props.add ( "ColumnExcludeFilters=" + ColumnExcludeFilters );
	props.add ( "Comment=" + Comment );
	props.add ( "ExcelIntegerColumns=" + ExcelIntegerColumns );
	props.add ( "ExcelDateTimeColumns=" + ExcelDateTimeColumns );
	props.add ( "NumberPrecision=" + NumberPrecision );
	props.add ( "ReadAllAsText=" + ReadAllAsText );
	props.add ( "MissingValue=" + MissingValue );
	props.add ( "OutputStart=" + OutputStart );
	props.add ( "OutputEnd=" + OutputEnd );
	__command_JTextArea.setText( __command.toString ( props ) );
	// Check the path and determine what the label on the path button should be...
	if (__path_JButton != null) {
		__path_JButton.setEnabled (true);
		File f = new File (OutputFile);
		if (f.isAbsolute()) {
			__path_JButton.setText (__RemoveWorkingDirectoryFromFile);
		}
		else {
            __path_JButton.setText (__AddWorkingDirectoryToFile);
		}
	}
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
*/
private void response ( boolean ok )
{	__ok = ok;	// Save to be returned by ok()
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
public void windowClosing(WindowEvent event) {
	response ( false );
}

// The following methods are all necessary because this class implements WindowListener
public void windowActivated(WindowEvent evt) {}
public void windowClosed(WindowEvent evt) {}
public void windowDeactivated(WindowEvent evt) {}
public void windowDeiconified(WindowEvent evt) {}
public void windowIconified(WindowEvent evt) {}
public void windowOpened(WindowEvent evt) {}

}