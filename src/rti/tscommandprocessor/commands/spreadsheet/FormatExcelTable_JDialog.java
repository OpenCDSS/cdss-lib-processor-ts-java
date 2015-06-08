package rti.tscommandprocessor.commands.spreadsheet;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

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
Editor for the FormatExcelTable command.
*/
public class FormatExcelTable_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{

// Used for button labels...

private final String __AddWorkingDirectoryToFile = "Add Working Directory To File";
private final String __RemoveWorkingDirectoryFromFile = "Remove Working Directory From File";

private boolean __error_wait = false; // To track errors
private boolean __first_time = true;
private JTextArea __command_JTextArea = null;
private JTabbedPane __main_JTabbedPane = null;
private JTextField __OutputFile_JTextField = null;
private JTextField __Worksheet_JTextField = null;
private JTabbedPane __excelSpace_JTabbedPane = null;
private JTextField __ExcelAddress_JTextField = null;
private JTextField __ExcelNamedRange_JTextField = null;
private JTextField __ExcelTableName_JTextField = null;
private SimpleJComboBox __ExcelColumnNames_JComboBox = null;
private SimpleJComboBox __KeepOpen_JComboBox = null;
private JTextField __IncludeColumns_JTextField = null;
private JTextField __ExcludeColumns_JTextField = null;
private JTextArea __ColumnIncludeFilters_JTextArea = null;
private JTextArea __ColumnExcludeFilters_JTextArea = null;
private SimpleJComboBox __StyleTableID_JComboBox = null;
private JTextField __Style_JTextField = null;
private JTextField __Formula_JTextField = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;	
private SimpleJButton __browse_JButton = null;
private SimpleJButton __path_JButton = null;
private String __working_dir = null;	
private FormatExcelTable_Command __command = null;
private boolean __ok = false;
private JFrame __parent = null;

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of table identifiers to provide as choices
*/
public FormatExcelTable_JDialog ( JFrame parent, FormatExcelTable_Command command, List<String> tableIDChoices )
{	super(parent, true);
	initialize ( parent, command, tableIDChoices );
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
    else if ( event.getActionCommand().equalsIgnoreCase("EditColumnIncludeFilters") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String ColumnIncludeFilters = __ColumnIncludeFilters_JTextArea.getText().trim();
        String [] notes = {
            "Format (include) rows from the Excel worksheet by specifying a pattern to match for a column.",
            "Only string columns can be specified."
        };
        String dict = (new DictionaryJDialog ( __parent, true, ColumnIncludeFilters,
            "Edit ColumnIncludeFilters Parameter", notes, "Table Column", "Pattern to include rows (* allowed)",10)).response();
        if ( dict != null ) {
            __ColumnIncludeFilters_JTextArea.setText ( dict );
            refresh();
        }
    }
    else if ( event.getActionCommand().equalsIgnoreCase("EditColumnExcludeFilters") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String ColumnExcludeFilters = __ColumnExcludeFilters_JTextArea.getText().trim();
        String [] notes = {
            "Do not format (exclude) rows from the Excel worksheet by specifying a pattern to match for a column.",
            "Only string columns can be specified."
        };
        String dict = (new DictionaryJDialog ( __parent, true, ColumnExcludeFilters,
            "Edit ColumnExcludeFilters Parameter", notes, "Table Column", "Pattern to exclude rows (* allowed)",10)).response();
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
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String Worksheet = __Worksheet_JTextField.getText().trim();
	String ExcelAddress = __ExcelAddress_JTextField.getText().trim();
	String ExcelNamedRange = __ExcelNamedRange_JTextField.getText().trim();
	String ExcelTableName = __ExcelTableName_JTextField.getText().trim();
	String ExcelColumnNames  = __ExcelColumnNames_JComboBox.getSelected();
	String KeepOpen = __KeepOpen_JComboBox.getSelected();
	String IncludeColumns = __IncludeColumns_JTextField.getText().trim();
	String ExcludeColumns = __ExcludeColumns_JTextField.getText().trim();
	String ColumnIncludeFilters  = __ColumnIncludeFilters_JTextArea.getText().trim();
	String ColumnExcludeFilters  = __ColumnExcludeFilters_JTextArea.getText().trim();
	String StyleTableID = __StyleTableID_JComboBox.getSelected();
	String Style  = __Style_JTextField.getText().trim();
	String Formula  = __Formula_JTextField.getText().trim();
	__error_wait = false;

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
    if ( KeepOpen.length() > 0 ) {
        props.set ( "KeepOpen", KeepOpen );
    }
    if ( IncludeColumns.length() > 0 ) {
        props.set ( "IncludeColumns", IncludeColumns );
    }
    if ( ExcludeColumns.length() > 0 ) {
        props.set ( "ExcludeColumns", ExcludeColumns );
    }
    if ( ColumnIncludeFilters.length() > 0 ) {
        props.set ( "ColumnIncludeFilters", ColumnIncludeFilters );
    }
    if ( ColumnExcludeFilters.length() > 0 ) {
        props.set ( "ColumnExcludeFilters", ColumnExcludeFilters );
    }
    if ( StyleTableID.length() > 0 ) {
        props.set ( "StyleTableID", StyleTableID );
    }
    if ( Style.length() > 0 ) {
        props.set ( "Style", Style );
    }
    if ( Formula.length() > 0 ) {
        props.set ( "Formula", Formula );
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
{	String OutputFile = __OutputFile_JTextField.getText().trim();
    String Worksheet = __Worksheet_JTextField.getText().trim();
	String ExcelAddress = __ExcelAddress_JTextField.getText().trim();
	String ExcelNamedRange = __ExcelNamedRange_JTextField.getText().trim();
	String ExcelTableName = __ExcelTableName_JTextField.getText().trim();
	String ExcelColumnNames  = __ExcelColumnNames_JComboBox.getSelected();
	String KeepOpen  = __KeepOpen_JComboBox.getSelected();
    String IncludeColumns = __IncludeColumns_JTextField.getText().trim();
    String ExcludeColumns = __ExcludeColumns_JTextField.getText().trim();
	String ColumnIncludeFilters  = __ColumnIncludeFilters_JTextArea.getText().trim();
	String ColumnExcludeFilters  = __ColumnExcludeFilters_JTextArea.getText().trim();
	String StyleTableID = __StyleTableID_JComboBox.getSelected();
	String Style  = __Style_JTextField.getText().trim();
	String Formula  = __Formula_JTextField.getText().trim();
	__command.setCommandParameter ( "OutputFile", OutputFile );
	__command.setCommandParameter ( "Worksheet", Worksheet );
	__command.setCommandParameter ( "ExcelAddress", ExcelAddress );
	__command.setCommandParameter ( "ExcelNamedRange", ExcelNamedRange );
	__command.setCommandParameter ( "ExcelTableName", ExcelTableName );
	__command.setCommandParameter ( "ExcelColumnNames", ExcelColumnNames );
	__command.setCommandParameter ( "KeepOpen", KeepOpen );
    __command.setCommandParameter ( "IncludeColumns", IncludeColumns );
    __command.setCommandParameter ( "ExcludeColumns", ExcludeColumns );
	__command.setCommandParameter ( "ColumnIncludeFilters", ColumnIncludeFilters );
	__command.setCommandParameter ( "ColumnExcludeFilters", ColumnExcludeFilters );
    __command.setCommandParameter ( "StyleTableID", StyleTableID );
    __command.setCommandParameter ( "Style", Style );
    __command.setCommandParameter ( "Formula", Formula );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit and possibly run.
*/
private void initialize ( JFrame parent, FormatExcelTable_Command command, List<String> tableIDChoices )
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
    	"<html><b>This command is under development - do not use in production work</b></html>.  " ),
    	0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);	
   	JGUIUtil.addComponent(paragraph, new JLabel (
    	"This command formats cells in an Excel table, where the table is defined as an area in a worksheet with column names.  " ),
    	0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);		

	JGUIUtil.addComponent(main_JPanel, paragraph,
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for Excel table parameters
    int yExcelTable = -1;
    JPanel excelOutput_JPanel = new JPanel();
    excelOutput_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Excel Table Area", excelOutput_JPanel );
    
    JGUIUtil.addComponent(excelOutput_JPanel, new JLabel (
        "The Excel file must exist and have been opened with NewExcelWorkbook() or commands that write to Excel workbooks."),
        0, ++yExcelTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(excelOutput_JPanel, new JLabel (
        "It is recommended that the location of the Excel file be specified using a path relative to the working directory."),
        0, ++yExcelTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    if (__working_dir != null) {
        JGUIUtil.addComponent(excelOutput_JPanel, new JLabel (
        "The working directory is: " + __working_dir), 
        0, ++yExcelTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    JGUIUtil.addComponent(excelOutput_JPanel, new JLabel (
        "A contiguous block of Excel cells must be specified using one of the address methods below."),
        0, ++yExcelTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(excelOutput_JPanel, new JLabel (
        "The row containing column names must be specified because column names are used to match cells for formatting."),
        0, ++yExcelTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(excelOutput_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yExcelTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(excelOutput_JPanel, new JLabel ("Output (workbook) file:"),
		0, ++yExcelTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputFile_JTextField = new JTextField (45);
	__OutputFile_JTextField.addKeyListener (this);
        JGUIUtil.addComponent(excelOutput_JPanel, __OutputFile_JTextField,
		1, yExcelTable, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ("Browse", this);
        JGUIUtil.addComponent(excelOutput_JPanel, __browse_JButton,
		6, yExcelTable, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
        
    JGUIUtil.addComponent(excelOutput_JPanel, new JLabel ("Worksheet:"),
        0, ++yExcelTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Worksheet_JTextField = new JTextField (30);
    __Worksheet_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(excelOutput_JPanel, __Worksheet_JTextField,
        1, yExcelTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(excelOutput_JPanel,
        new JLabel ("Required (if not in address) - worksheet name (default=first sheet)."),
        3, yExcelTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    __excelSpace_JTabbedPane = new JTabbedPane ();
    __excelSpace_JTabbedPane.setBorder(
        BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
        "Specify the address for a contiguous block of cells the in Excel worksheet (upper left is start)" ));
    JGUIUtil.addComponent(excelOutput_JPanel, __excelSpace_JTabbedPane,
        0, ++yExcelTable, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
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
    JGUIUtil.addComponent(address_JPanel, new JLabel ("Excel cell block address in format A1 or A1:B2."),
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
    
    JPanel tableAddress_JPanel = new JPanel();
    tableAddress_JPanel.setLayout(new GridBagLayout());
    __excelSpace_JTabbedPane.addTab ( "by Excel Table Name", tableAddress_JPanel );
    int yTableAddress = -1;
    
    JGUIUtil.addComponent(tableAddress_JPanel, new JLabel ("Excel table name:"),
        0, ++yTableAddress, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ExcelTableName_JTextField = new JTextField (10);
    __ExcelTableName_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(tableAddress_JPanel, __ExcelTableName_JTextField,
        1, yTableAddress, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(tableAddress_JPanel, new JLabel ("Excel table name."),
        3, yTableAddress, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(excelOutput_JPanel, new JLabel( "Excel column names:"),
        0, ++yExcelTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ExcelColumnNames_JComboBox = new SimpleJComboBox ( false );
    __ExcelColumnNames_JComboBox.add("");
    //__ExcelColumnNames_JComboBox.add(__command._ColumnN);
    __ExcelColumnNames_JComboBox.add(__command._FirstRowInRange);
    __ExcelColumnNames_JComboBox.add(__command._RowBeforeRange);
    __ExcelColumnNames_JComboBox.select ( 0 );
    __ExcelColumnNames_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(excelOutput_JPanel, __ExcelColumnNames_JComboBox,
        1, yExcelTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(excelOutput_JPanel, new JLabel ( "Optional - how to define Excel column names (default=" +
        __command._RowBeforeRange + ")."),
        3, yExcelTable, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(excelOutput_JPanel, new JLabel( "Keep file open?:"),
        0, ++yExcelTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __KeepOpen_JComboBox = new SimpleJComboBox ( false );
    __KeepOpen_JComboBox.add("");
    __KeepOpen_JComboBox.add(__command._False);
    __KeepOpen_JComboBox.add(__command._True);
    __KeepOpen_JComboBox.select ( 0 );
    __KeepOpen_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(excelOutput_JPanel, __KeepOpen_JComboBox,
        1, yExcelTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(excelOutput_JPanel, new JLabel ( "Optional - keep Excel file open? (default=" + __command._False + ")."),
        3, yExcelTable, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for table parameters
    int yMatch = -1;
    JPanel match_JPanel = new JPanel();
    match_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Columns/Rows to Match", match_JPanel );
    
    JGUIUtil.addComponent(match_JPanel, new JLabel (
        "Table columns and rows are matched to determine the cells to format."),
        0, ++yMatch, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(match_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yMatch, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(match_JPanel, new JLabel ("Columns to include:"), 
        0, ++yMatch, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IncludeColumns_JTextField = new JTextField (10);
    __IncludeColumns_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(match_JPanel, __IncludeColumns_JTextField,
        1, yMatch, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(match_JPanel, new JLabel ("Optional - names of columns to format (default=format all)."),
        3, yMatch, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(match_JPanel, new JLabel ("Columns to exclude:"), 
        0, ++yMatch, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ExcludeColumns_JTextField = new JTextField (10);
    __ExcludeColumns_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(match_JPanel, __ExcludeColumns_JTextField,
        1, yMatch, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(match_JPanel, new JLabel ("Optional - names of columns to NOT format (default=format all)."),
        3, yMatch, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
 
    JGUIUtil.addComponent(match_JPanel, new JLabel ("Column filters to include rows:"),
        0, ++yMatch, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ColumnIncludeFilters_JTextArea = new JTextArea (3,35);
    __ColumnIncludeFilters_JTextArea.setLineWrap ( true );
    __ColumnIncludeFilters_JTextArea.setWrapStyleWord ( true );
    __ColumnIncludeFilters_JTextArea.setToolTipText("TableColumn:DatastoreColumn,TableColumn:DataStoreColumn");
    __ColumnIncludeFilters_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(match_JPanel, new JScrollPane(__ColumnIncludeFilters_JTextArea),
        1, yMatch, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(match_JPanel, new JLabel ("Optional - column patterns to include rows (default=include all)."),
        3, yMatch, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(match_JPanel, new SimpleJButton ("Edit","EditColumnIncludeFilters",this),
        3, ++yMatch, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(match_JPanel, new JLabel ("Column filters to exclude rows:"),
        0, ++yMatch, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ColumnExcludeFilters_JTextArea = new JTextArea (3,35);
    __ColumnExcludeFilters_JTextArea.setLineWrap ( true );
    __ColumnExcludeFilters_JTextArea.setWrapStyleWord ( true );
    __ColumnExcludeFilters_JTextArea.setToolTipText("TableColumn:DatastoreColumn,TableColumn:DataStoreColumn");
    __ColumnExcludeFilters_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(match_JPanel, new JScrollPane(__ColumnExcludeFilters_JTextArea),
        1, yMatch, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(match_JPanel, new JLabel ("Optional - column patterns to exclude rows (default=include all)."),
        3, yMatch, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(match_JPanel, new SimpleJButton ("Edit","EditColumnExcludeFilters",this),
        3, ++yMatch, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for Excel formatting 
    int yExcelFormat = -1;
    JPanel excelFormat_JPanel = new JPanel();
    excelFormat_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Excel Formatting", excelFormat_JPanel );

    JGUIUtil.addComponent(excelFormat_JPanel, new JLabel (
        "Cells are formatted by specifying a formula to check and a matching style to use if the formula is true."),
        0, ++yExcelFormat, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(excelFormat_JPanel, new JLabel (
        "A style table must be specified with column \"StyleID\" and optional columns specifying cell style properties (see documentation for details):"),
        0, ++yExcelFormat, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(excelFormat_JPanel, new JLabel (
        "    FillColor - specify as named color (e.g., red), RGB triplets (e.g., 255,0,255), or hexadecimal RGB (e.g., 0xFF0088)"),
        0, ++yExcelFormat, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(excelFormat_JPanel, new JLabel (
        "    More formatting properties will be added in the future."),
        0, ++yExcelFormat, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(excelFormat_JPanel, new JLabel (
        "The formula is evaluated and if true the specified style is used.  Use syntax:"),
        0, ++yExcelFormat, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(excelFormat_JPanel, new JLabel (
        "    X operator Y"),
        0, ++yExcelFormat, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(excelFormat_JPanel, new JLabel (
        "where X is ${cell:value} or ${cell:comment}; operator is >, >=, ==, !=, <, <=, contains; and Y is a value to compare against or \"null\"."),
        0, ++yExcelFormat, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(excelFormat_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yExcelFormat, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(excelFormat_JPanel, new JLabel ( "Style table ID:" ), 
        0, ++yExcelFormat, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __StyleTableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit since table may not be available
    tableIDChoices.add(0,""); // Add blank to give user a blank entry to type over
    __StyleTableID_JComboBox.setData ( tableIDChoices );
    __StyleTableID_JComboBox.addItemListener ( this );
    __StyleTableID_JComboBox.addKeyListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(excelFormat_JPanel, __StyleTableID_JComboBox,
        1, yExcelFormat, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(excelFormat_JPanel, new JLabel( "Required - table with cell style properties."), 
        3, yExcelFormat, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(excelFormat_JPanel, new JLabel ("Style:"),
        0, ++yExcelFormat, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Style_JTextField = new JTextField (10);
    __Style_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(excelFormat_JPanel, __Style_JTextField,
        1, yExcelFormat, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(excelFormat_JPanel,
        new JLabel ("Required - style from the style table."),
        3, yExcelFormat, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(excelFormat_JPanel, new JLabel ("Formula:"),
        0, ++yExcelFormat, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Formula_JTextField = new JTextField (30);
    __Formula_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(excelFormat_JPanel, __Formula_JTextField,
        1, yExcelFormat, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(excelFormat_JPanel,
        new JLabel ("Required - formula to evaluate."),
        3, yExcelFormat, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
   
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
{	String routine = getClass().getSimpleName() + ".refresh";
    String OutputFile = "";
    String Worksheet = "";
	String ExcelAddress = "";
	String ExcelNamedRange = "";
	String ExcelTableName = "";
	String ExcelColumnNames = "";
	String KeepOpen = "";
    String IncludeColumns = "";
    String ExcludeColumns = "";
	String ColumnIncludeFilters = "";
	String ColumnExcludeFilters = "";
	//String ExcelIntegerColumns = "";
	//String ExcelDateTimeColumns = "";
    String StyleTableID = "";
    String Formula = "";
    String Style = "";
	PropList props = __command.getCommandParameters();
	if (__first_time) {
		__first_time = false;
		OutputFile = props.getValue ( "OutputFile" );
		Worksheet = props.getValue ( "Worksheet" );
		ExcelAddress = props.getValue ( "ExcelAddress" );
		ExcelNamedRange = props.getValue ( "ExcelNamedRange" );
		ExcelTableName = props.getValue ( "ExcelTableName" );
		ExcelColumnNames = props.getValue ( "ExcelColumnNames" );
		KeepOpen = props.getValue ( "KeepOpen" );
        IncludeColumns = props.getValue ( "IncludeColumns" );
        ExcludeColumns = props.getValue ( "ExcludeColumns" );
		ColumnIncludeFilters = props.getValue ( "ColumnIncludeFilters" );
		ColumnExcludeFilters = props.getValue ( "ColumnExcludeFilters" );
		//ExcelIntegerColumns = props.getValue ( "ExcelIntegerColumns" );
		//ExcelDateTimeColumns = props.getValue ( "ExcelDateTimeColumns" );
        StyleTableID = props.getValue ( "StyleTableID" );
        Formula = props.getValue ( "Formula" );
        Style = props.getValue ( "Style" );
		if ( OutputFile != null ) {
			__OutputFile_JTextField.setText ( OutputFile );
		}
        if ( Worksheet != null ) {
            __Worksheet_JTextField.setText ( Worksheet );
        }
		if ( (ExcelAddress != null) && !ExcelAddress.equals("") ) {
			__ExcelAddress_JTextField.setText ( ExcelAddress );
			// Also select the tab to be visible
			__excelSpace_JTabbedPane.setSelectedIndex(0);
		}
		if ( (ExcelNamedRange != null) && !ExcelNamedRange.equals("") ) {
			__ExcelNamedRange_JTextField.setText ( ExcelNamedRange );
			__excelSpace_JTabbedPane.setSelectedIndex(1);
		}
        if ( (ExcelTableName != null) && !ExcelTableName.equals("") ) {
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
        if ( KeepOpen == null || KeepOpen.equals("") ) {
            // Select a default...
            __KeepOpen_JComboBox.select ( 0 );
        } 
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __KeepOpen_JComboBox, KeepOpen, JGUIUtil.NONE, null, null ) ) {
                __KeepOpen_JComboBox.select ( KeepOpen );
            }
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid\nKeepOpen \"" +
                    KeepOpen + "\".  Select a different choice or Cancel." );
            }
        }
        if ( IncludeColumns != null ) {
            __IncludeColumns_JTextField.setText ( IncludeColumns );
        }
        if ( ExcludeColumns != null ) {
            __ExcludeColumns_JTextField.setText ( ExcludeColumns );
        }
        if ( ColumnIncludeFilters != null ) {
            __ColumnIncludeFilters_JTextArea.setText ( ColumnIncludeFilters );
        }
        if ( ColumnExcludeFilters != null ) {
            __ColumnExcludeFilters_JTextArea.setText ( ColumnExcludeFilters );
        }
        if ( StyleTableID == null ) {
            // Select default...
            __StyleTableID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __StyleTableID_JComboBox,StyleTableID, JGUIUtil.NONE, null, null ) ) {
                __StyleTableID_JComboBox.select ( StyleTableID );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nStyleTableID value \"" + StyleTableID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( Formula != null ) {
            __Formula_JTextField.setText ( Formula );
        }
        if ( Style != null ) {
            __Style_JTextField.setText ( Style );
        }
	}
	// Regardless, reset the command from the fields...
	OutputFile = __OutputFile_JTextField.getText().trim();
	Worksheet = __Worksheet_JTextField.getText().trim();
	ExcelAddress = __ExcelAddress_JTextField.getText().trim();
	ExcelNamedRange = __ExcelNamedRange_JTextField.getText().trim();
	ExcelTableName = __ExcelTableName_JTextField.getText().trim();
	ExcelColumnNames = __ExcelColumnNames_JComboBox.getSelected();
	KeepOpen = __KeepOpen_JComboBox.getSelected();
	IncludeColumns = __IncludeColumns_JTextField.getText().trim();
	ExcludeColumns = __ExcludeColumns_JTextField.getText().trim();
	ColumnIncludeFilters = __ColumnIncludeFilters_JTextArea.getText().trim();
	ColumnExcludeFilters = __ColumnExcludeFilters_JTextArea.getText().trim();
	StyleTableID = __StyleTableID_JComboBox.getSelected();
	Formula = __Formula_JTextField.getText().trim();
	Style = __Style_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "OutputFile=" + OutputFile );
	props.add ( "Worksheet=" + Worksheet );
	props.add ( "ExcelAddress=" + ExcelAddress );
	props.add ( "ExcelNamedRange=" + ExcelNamedRange );
	props.add ( "ExcelTableName=" + ExcelTableName );
	props.add ( "ExcelColumnNames=" + ExcelColumnNames );
	props.add ( "KeepOpen=" + KeepOpen );
    props.add ( "IncludeColumns=" + IncludeColumns );
    props.add ( "ExcludeColumns=" + ExcludeColumns );
	props.add ( "ColumnIncludeFilters=" + ColumnIncludeFilters );
	props.add ( "ColumnExcludeFilters=" + ColumnExcludeFilters );
	//props.add ( "ExcelIntegerColumns=" + ExcelIntegerColumns );
	//props.add ( "ExcelDateTimeColumns=" + ExcelDateTimeColumns );
    props.add ( "StyleTableID=" + StyleTableID );
    props.add ( "Formula=" + Formula );
    props.add ( "Style=" + Style );
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