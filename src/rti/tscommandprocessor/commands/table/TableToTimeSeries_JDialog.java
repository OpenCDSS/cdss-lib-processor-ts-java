package rti.tscommandprocessor.commands.table;

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
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;

import RTi.Util.IO.PropList;

import RTi.Util.Message.Message;
import RTi.Util.Time.DateTimeFormatterSpecifiersJPanel;
import RTi.Util.Time.DateTimeFormatterType;
import RTi.Util.Time.TimeInterval;

/**
Editor for the TableToTimeSeries() command.
*/
public class TableToTimeSeries_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private TableToTimeSeries_Command __command = null;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __DateColumn_JTextField = null;
private JTextField __TimeColumn_JTextField = null;
private JTextField __DateTimeColumn_JTextField = null;
private DateTimeFormatterSpecifiersJPanel __DateTimeFormat_JPanel = null;
private JTabbedPane __location_JTabbedPane = null;
private JTextField __ValueColumn_JTextField = null;
private JTextField __FlagColumn_JTextField = null;
//private JTextField __SkipRows_JTextField = null;
private JTextField __LocationID_JTextField = null;
private JTextField __LocationTypeColumn_JTextField = null;
private JTextField __LocationColumn_JTextField = null;
private JTextField __DataSourceColumn_JTextField = null;
private JTextField __DataTypeColumn_JTextField = null;
private JTextField __ScenarioColumn_JTextField = null;
private JTextField __UnitsColumn_JTextField = null;
private JTextField __DataSource_JTextField = null;
private JTextField __DataType_JTextField = null;
private SimpleJComboBox __Interval_JComboBox = null;
private JTextField __Scenario_JTextField = null;
private JTextField __Units_JTextField = null;
private JTextField __MissingValue_JTextField = null;
private JTextField __InputStart_JTextField = null;
private JTextField __InputEnd_JTextField = null;
private JTextArea __Command_JTextArea = null;
private boolean __error_wait = false; // Is there an error to be cleared up or Cancel?
private boolean __first_time = true;
private boolean __ok = false;

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of table identifiers to provide as choices
*/
public TableToTimeSeries_JDialog ( JFrame parent, TableToTimeSeries_Command command, List<String> tableIDChoices )
{
	super(parent, true);
	initialize ( parent, command, tableIDChoices );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();
try{
    if ( o == __cancel_JButton ) {
		response(false);
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput ();
		if (!__error_wait) {
			response(true);
		}
	}
}catch ( Exception e ) {
    Message.printWarning(2, "Action performed", e );
}
}

// Start event handlers for DocumentListener...

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void changedUpdate ( DocumentEvent e )
{   checkGUIState();
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void insertUpdate ( DocumentEvent e )
{   checkGUIState();
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void removeUpdate ( DocumentEvent e )
{   checkGUIState();
    refresh();
}

// ...End event handlers for DocumentListener

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
	String TableID = __TableID_JComboBox.getSelected();
//	String SkipRows = __SkipRows_JTextField.getText().trim();
	String DateColumn = __DateColumn_JTextField.getText().trim();
	String TimeColumn = __TimeColumn_JTextField.getText().trim();
	String DateTimeColumn = __DateTimeColumn_JTextField.getText().trim();
	String DateTimeFormat =__DateTimeFormat_JPanel.getText(true,true).trim();
	String LocationTypeColumn = __LocationTypeColumn_JTextField.getText().trim();
	String LocationColumn = __LocationColumn_JTextField.getText().trim();
	String DataSourceColumn = __DataSourceColumn_JTextField.getText().trim();
	String DataTypeColumn = __DataTypeColumn_JTextField.getText().trim();
	String ScenarioColumn = __ScenarioColumn_JTextField.getText().trim();
	String UnitsColumn = __UnitsColumn_JTextField.getText().trim();
	String LocationID = __LocationID_JTextField.getText().trim();
	String ValueColumn = __ValueColumn_JTextField.getText().trim();
	String FlagColumn = __FlagColumn_JTextField.getText().trim();
	String DataSource = __DataSource_JTextField.getText().trim();
	String DataType = __DataType_JTextField.getText().trim();
	String Interval = __Interval_JComboBox.getSelected();
	String Scenario = __Scenario_JTextField.getText().trim();
	String Units = __Units_JTextField.getText().trim();
	String MissingValue = __MissingValue_JTextField.getText().trim();
	String Alias = __Alias_JTextField.getText().trim();
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
	
	__error_wait = false;
	
	if (TableID.length() > 0) {
		props.set("TableID", TableID);
	}
//    if (SkipRows.length() > 0) {
//        props.set("SkipRows", SkipRows);
//    }
    if (DateColumn.length() > 0) {
        props.set("DateColumn", DateColumn);
    }
    if (TimeColumn.length() > 0) {
        props.set("TimeColumn", TimeColumn);
    }
    if (DateTimeColumn.length() > 0) {
        props.set("DateTimeColumn", DateTimeColumn);
    }
    if (DateTimeFormat.length() > 0) {
        props.set("DateTimeFormat", DateTimeFormat);
    }
    if (LocationTypeColumn.length() > 0) {
        props.set("LocationTypeColumn", LocationTypeColumn);
    }
    if (LocationColumn.length() > 0) {
        props.set("LocationColumn", LocationColumn);
    }
    if (DataSourceColumn.length() > 0) {
        props.set("DataSourceColumn", DataSourceColumn);
    }
    if (DataTypeColumn.length() > 0) {
        props.set("DataTypeColumn", DataTypeColumn);
    }
    if (ScenarioColumn.length() > 0) {
        props.set("ScenarioColumn", ScenarioColumn);
    }
    if (UnitsColumn.length() > 0) {
        props.set("UnitsColumn", UnitsColumn);
    }
    if (LocationID.length() > 0) {
        props.set("LocationID", LocationID);
    }
    if (ValueColumn.length() > 0) {
        props.set("ValueColumn", ValueColumn);
    }
    if (FlagColumn.length() > 0) {
        props.set("FlagColumn", FlagColumn);
    }
    if (DataSource.length() > 0) {
        props.set("DataSource", DataSource);
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
	if (InputStart.length() > 0 ) {
		props.set("InputStart", InputStart);
	}
	if (InputEnd.length() > 0 ) {
		props.set("InputEnd", InputEnd);
	}
	try {
	    // This will warn the user...
		__command.checkCommandParameters ( props, null, 1 );
	} 
	catch ( Exception e ) {
		// The warning would have been printed in the check code.
	    Message.printWarning(3, "CheckInput", e);
		__error_wait = true;
	}
}

/**
Commit the edits to the command.  In this case the command parameters have
already been checked and no errors were detected.
*/
private void commitEdits() {
    String TableID = __TableID_JComboBox.getSelected();
//	String SkipRows = __SkipRows_JTextField.getText().trim();
    String DateColumn = __DateColumn_JTextField.getText().trim();
    String DateTimeFormat =__DateTimeFormat_JPanel.getText(true,true).trim();
    String TimeColumn = __TimeColumn_JTextField.getText().trim();
    String DateTimeColumn = __DateTimeColumn_JTextField.getText().trim();
    String ValueColumn = __ValueColumn_JTextField.getText().trim();
    String FlagColumn = __FlagColumn_JTextField.getText().trim();
    String LocationTypeColumn = __LocationTypeColumn_JTextField.getText().trim();
    String LocationColumn = __LocationColumn_JTextField.getText().trim();
    String DataSourceColumn = __DataSourceColumn_JTextField.getText().trim();
    String DataTypeColumn = __DataTypeColumn_JTextField.getText().trim();
    String ScenarioColumn = __ScenarioColumn_JTextField.getText().trim();
    String UnitsColumn = __UnitsColumn_JTextField.getText().trim();
    String LocationID = __LocationID_JTextField.getText().trim();
    String DataSource = __DataSource_JTextField.getText().trim();
    String DataType = __DataType_JTextField.getText().trim();
    String Interval = __Interval_JComboBox.getSelected();
    String Scenario = __Scenario_JTextField.getText().trim();
    String Units = __Units_JTextField.getText().trim();
    String MissingValue = __MissingValue_JTextField.getText().trim();
    String Alias = __Alias_JTextField.getText().trim();
    String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();

	__command.setCommandParameter("TableID", TableID);
//	__command.setCommandParameter("SkipRows", SkipRows);
	__command.setCommandParameter("DateColumn", DateColumn);
	__command.setCommandParameter("TimeColumn", TimeColumn);
	__command.setCommandParameter("DateTimeColumn", DateTimeColumn);
	__command.setCommandParameter("DateTimeFormat", DateTimeFormat);
	__command.setCommandParameter("LocationTypeColumn", LocationTypeColumn);
    __command.setCommandParameter("LocationColumn", LocationColumn);
    __command.setCommandParameter("DataSourceColumn", DataSourceColumn);
    __command.setCommandParameter("DataTypeColumn", DataTypeColumn);
    __command.setCommandParameter("ScenarioColumn", ScenarioColumn);
    __command.setCommandParameter("UnitsColumn", UnitsColumn);
    __command.setCommandParameter("LocationID", LocationID);
	__command.setCommandParameter("ValueColumn", ValueColumn);
	__command.setCommandParameter("FlagColumn", FlagColumn);
	__command.setCommandParameter("DataSource", DataSource );
	__command.setCommandParameter("DataType", DataType);
	__command.setCommandParameter("Interval", Interval);
	__command.setCommandParameter("Scenario", Scenario);
	__command.setCommandParameter("Units", Units);
	__command.setCommandParameter("MissingValue", MissingValue);
	__command.setCommandParameter("Alias", Alias);
	__command.setCommandParameter("InputStart", InputStart);
	__command.setCommandParameter("InputEnd", InputEnd);
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable {
	__cancel_JButton = null;
	__ok_JButton = null;
	__command = null;
	__Alias_JTextField = null;
	//__InputStart_JTextField = null;
	//__InputEnd_JTextField = null;
	__Command_JTextArea = null;

	super.finalize();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize(JFrame parent, TableToTimeSeries_Command command, List<String> tableIDChoices) {
	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(1,2,1,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;
	
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Create 1+ time series from a table." +
        "  The table can contain one column per time series, or a single column for all time series." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The column name(s), date/time column, value column(s), and Location ID(s) columns can use the notation " +
        "TC[start:stop] to use column names." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "For example, \"Date,TC[2:]\" defines the first column as \"Date\" and column names " +
        "2+ will be taken from the table." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "If used, specify input start and end to a precision appropriate for the data." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Table ID:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true );    // Allow edit
    tableIDChoices.add(0,""); // Add blank to ignore table
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(main_JPanel, __TableID_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required - table to process."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    /*
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Rows to skip (by row number):"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SkipRows_JTextField = new JTextField (10);
    __SkipRows_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __SkipRows_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - comma-separated numbers (1+) and ranges (e.g., 1,3-7) (default=none)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        */
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Date/time column:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DateTimeColumn_JTextField = new JTextField (10);
    __DateTimeColumn_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __DateTimeColumn_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Required - if date and time are in the same column (can use \"TC[N]\")."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JLabel DateTimeFormat_JLabel = new JLabel ("Date/time format:");
    JGUIUtil.addComponent(main_JPanel, DateTimeFormat_JLabel,
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DateTimeFormat_JPanel = new DateTimeFormatterSpecifiersJPanel(20,true,true,null);
    __DateTimeFormat_JPanel.addKeyListener (this);
    __DateTimeFormat_JPanel.addFormatterTypeItemListener (this); // Respond to changes in formatter choice
    __DateTimeFormat_JPanel.getDocument().addDocumentListener(this); // Respond to changes in text field contents
    JGUIUtil.addComponent(main_JPanel, __DateTimeFormat_JPanel,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - date/time format MM/DD/YYYY, etc. (default=auto-detect)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Date column:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DateColumn_JTextField = new JTextField (10);
    __DateColumn_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __DateColumn_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Required - if date and time are in separate columns (can use \"TC[N:N]\")."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Time column:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TimeColumn_JTextField = new JTextField (10);
    __TimeColumn_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __TimeColumn_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Required - if date and time are in separate columns (can use \"TC[N:N]\")."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    __location_JTabbedPane = new JTabbedPane ();
    __location_JTabbedPane.setBorder(
        BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
        "Indicate how to assign location identifier" ));
    JGUIUtil.addComponent(main_JPanel, __location_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JPanel multTS_JPanel = new JPanel();
    multTS_JPanel.setLayout(new GridBagLayout());
    __location_JTabbedPane.addTab ( "Multiple Data Value (Number) Columns", multTS_JPanel );
    int yMult = -1;
    
    JGUIUtil.addComponent(multTS_JPanel, new JLabel ("Location ID(s):"),
        0, ++yMult, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LocationID_JTextField = new JTextField (10);
    __LocationID_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(multTS_JPanel, __LocationID_JTextField,
        1, yMult, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(multTS_JPanel, new JLabel (
        "Required - location ID for each value column, separated by commas (can use \"TC[N:N]\")."),
        3, yMult, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JPanel singleTS_JPanel = new JPanel();
    singleTS_JPanel.setLayout(new GridBagLayout());
    __location_JTabbedPane.addTab ( "Single Data Value (Number) Column", singleTS_JPanel );
    int ySingle = -1;
    
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ("Location type column:"),
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LocationTypeColumn_JTextField = new JTextField (20);
    __LocationTypeColumn_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(singleTS_JPanel, __LocationTypeColumn_JTextField,
        1, ySingle, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel (
        "Optional - column name for location type"),
        3, ySingle, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ("Location column:"),
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LocationColumn_JTextField = new JTextField (20);
    __LocationColumn_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(singleTS_JPanel, __LocationColumn_JTextField,
        1, ySingle, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel (
        "Required - column name for location identifier."),
        3, ySingle, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ("Data source column:"),
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataSourceColumn_JTextField = new JTextField (20);
    __DataSourceColumn_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(singleTS_JPanel, __DataSourceColumn_JTextField,
        1, ySingle, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel (
        "Optional - column name for data source if not provided with DataSource."),
        3, ySingle, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ("Data type column:"),
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataTypeColumn_JTextField = new JTextField (20);
    __DataTypeColumn_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(singleTS_JPanel, __DataTypeColumn_JTextField,
        1, ySingle, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel (
        "Optional - column name for data type if not provided with DataType."),
        3, ySingle, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ("Scenario column:"),
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ScenarioColumn_JTextField = new JTextField (20);
    __ScenarioColumn_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(singleTS_JPanel, __ScenarioColumn_JTextField,
        1, ySingle, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel (
        "Optional - column name for scenario if not provided as Scenario."),
        3, ySingle, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ("Data units column:"),
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __UnitsColumn_JTextField = new JTextField (20);
    __UnitsColumn_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(singleTS_JPanel, __UnitsColumn_JTextField,
        1, ySingle, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel (
        "Optional - column name for units, if not provided as Units."),
        3, ySingle, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Value column(s):"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ValueColumn_JTextField = new JTextField (20);
    __ValueColumn_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __ValueColumn_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Required - specify column names for time series values, separated by commas (can use \"TC[N:N]\")."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Flag column(s):"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __FlagColumn_JTextField = new JTextField (20);
    __FlagColumn_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __FlagColumn_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - specify column names for time series flags, separated by commas (can use \"TC[N:N]\")."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Data source:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataSource_JTextField = new JTextField (10);
    __DataSource_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __DataSource_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - data source (provider) for the data (default=blank)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Data type(s):"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataType_JTextField = new JTextField (10);
    __DataType_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __DataType_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - data type for each value column, separated by commas (default=value column name(s))."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Data interval:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Interval_JComboBox = new SimpleJComboBox ( false );
    List<String> intervals = TimeInterval.getTimeIntervalChoices(TimeInterval.MINUTE, TimeInterval.YEAR,false,-1);
    TimeInterval irreg = new TimeInterval ( TimeInterval.IRREGULAR, 0 );
    intervals.add("" + irreg);
    __Interval_JComboBox.setData ( intervals );
    // Select a default...
    __Interval_JComboBox.select ( 0 );
    __Interval_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Interval_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required - data interval for time series."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Scenario:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Scenario_JTextField = new JTextField (10);
    __Scenario_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __Scenario_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - scenario for the time series (comma-separated, default=blank)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("Units of data:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Units_JTextField = new JTextField ( "", 10 );
	__Units_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel, __Units_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - separate by commas (default=blank)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Missing value(s):"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MissingValue_JTextField = new JTextField (10);
    __MissingValue_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __MissingValue_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - missing value indicator(s) for table data (default=blank values)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
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
	__Command_JTextArea = new JTextArea(7, 55);
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

	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );

	setTitle("Edit " + __command.getCommandName() + " Command");

	setResizable ( true );
    pack();
    JGUIUtil.center( this );
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
@return true if the edits were committed, false if the user canceled.
*/
public boolean ok() {
	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh()
{   String routine = getClass().getName() + ".refresh", message;
    String TableID = "";
    //String SkipRows = "";
    String DateColumn = "";
    String TimeColumn = "";
    String DateTimeColumn = "";
    String DateTimeFormat = "";
    String LocationTypeColumn = "";
    String LocationColumn = "";
    String DataSourceColumn = "";
    String DataTypeColumn = "";
    String ScenarioColumn = "";
    String UnitsColumn = "";
    String LocationID = "";
    String ValueColumn = "";
    String FlagColumn = "";
    String DataSource = "";
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
		TableID = props.getValue ( "TableID" );
//		SkipRows = props.getValue("SkipRows");
	    DateColumn = props.getValue("DateColumn");
	    TimeColumn = props.getValue("TimeColumn");
	    DateTimeColumn = props.getValue("DateTimeColumn");
	    DateTimeFormat = props.getValue("DateTimeFormat");
	    LocationTypeColumn = props.getValue("LocationTypeColumn");
        LocationColumn = props.getValue("LocationColumn");
        DataSourceColumn = props.getValue("DataSourceColumn");
        DataTypeColumn = props.getValue("DataTypeColumn");
        ScenarioColumn = props.getValue("ScenarioColumn");
        UnitsColumn = props.getValue("UnitsColumn");
        LocationID = props.getValue("LocationID");
	    ValueColumn = props.getValue("ValueColumn");
	    FlagColumn = props.getValue("FlagColumn");
	    DataSource = props.getValue("DataSource");
	    DataType = props.getValue("DataType");
	    Interval = props.getValue("Interval");
	    Scenario = props.getValue("Scenario");
	    Units = props.getValue("Units");
	    MissingValue = props.getValue("MissingValue");
	    Alias = props.getValue("Alias");
		InputStart = props.getValue("InputStart");
		InputEnd = props.getValue("InputEnd");
		// Set the control fields
        if ( TableID == null ) {
            // Select default...
            __TableID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TableID_JComboBox,TableID, JGUIUtil.NONE, null, null ) ) {
                __TableID_JComboBox.select ( TableID );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTableID value \"" + TableID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
//        if ( SkipRows != null) {
//            __SkipRows_JTextField.setText(SkipRows);
//        }
        if (DateColumn != null) {
            __DateColumn_JTextField.setText(DateColumn);
        }
        if (TimeColumn != null) {
            __TimeColumn_JTextField.setText(TimeColumn);
        }
        if (DateTimeColumn != null) {
            __DateTimeColumn_JTextField.setText(DateTimeColumn);
        }
        if (DateTimeFormat != null) {
            // The front part of the string may match a formatter type (e.g., "C:") in which case
            // only the latter part of the string should be displayed.
            int pos = DateTimeFormat.indexOf(":");
            if ( pos > 0 ) {
                try {
                    __DateTimeFormat_JPanel.selectFormatterType(
                        DateTimeFormatterType.valueOfIgnoreCase(DateTimeFormat.substring(0,pos)));
                    Message.printStatus(2, routine, "Selecting format \"" + DateTimeFormat.substring(0,pos) + "\"");
                    if ( DateTimeFormat.length() > pos ) {
                        __DateTimeFormat_JPanel.setText(DateTimeFormat.substring(pos + 1));
                    }
                }
                catch ( IllegalArgumentException e ) {
                    __DateTimeFormat_JPanel.setText(DateTimeFormat);
                }
            }
            else {
                __DateTimeFormat_JPanel.setText(DateTimeFormat);
            }
        }
        if (LocationTypeColumn != null) {
            __LocationTypeColumn_JTextField.setText(LocationTypeColumn);
        }
        if (LocationColumn != null) {
            __LocationColumn_JTextField.setText(LocationColumn);
            if ( !LocationColumn.equals("") ) {
                __location_JTabbedPane.setSelectedIndex(1);
            }
        }
        if (DataSourceColumn != null) {
            __DataSourceColumn_JTextField.setText(DataSourceColumn);
        }
        if (DataTypeColumn != null) {
            __DataTypeColumn_JTextField.setText(DataTypeColumn);
        }
        if (ScenarioColumn != null) {
            __ScenarioColumn_JTextField.setText(ScenarioColumn);
        }
        if (UnitsColumn != null) {
            __UnitsColumn_JTextField.setText(UnitsColumn);
        }
        if (LocationID != null) {
            __LocationID_JTextField.setText(LocationID);
            if ( !LocationID.equals("") ) {
                __location_JTabbedPane.setSelectedIndex(0);
            }
        }
        if (ValueColumn != null) {
            __ValueColumn_JTextField.setText(ValueColumn);
        }
        if (FlagColumn != null) {
            __FlagColumn_JTextField.setText(FlagColumn);
        }
        if (DataSource != null) {
            __DataSource_JTextField.setText(DataSource);
        }
        if (DataType != null) {
            __DataType_JTextField.setText(DataType);
        }
        if ( Interval == null || Interval.equals("") ) {
            // Select a default...
            __Interval_JComboBox.select ( 0 );
        } 
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __Interval_JComboBox, Interval, JGUIUtil.NONE, null, null ) ) {
                __Interval_JComboBox.select ( Interval );
            }
            else {
                message = "Existing command references an invalid\nInterval \"" + Interval + "\".  "
                    +"Select a different choice or Cancel.";
                Message.printWarning ( 1, routine, message );
            }
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
		if (InputStart != null) {
			__InputStart_JTextField.setText(InputStart);
		}
		if (InputEnd != null) {
			__InputEnd_JTextField.setText(InputEnd);
		}
	}

	// Regardless, reset the command from the fields.  This is only  visible
	// information that has not been committed in the command.
	TableID = __TableID_JComboBox.getSelected();
//    SkipRows = __SkipRows_JTextField.getText().trim();
    DateColumn = __DateColumn_JTextField.getText().trim();
    TimeColumn = __TimeColumn_JTextField.getText().trim();
    DateTimeColumn = __DateTimeColumn_JTextField.getText().trim();
    DateTimeFormat = __DateTimeFormat_JPanel.getText(true,true).trim();
    LocationTypeColumn = __LocationTypeColumn_JTextField.getText().trim();
    LocationColumn = __LocationColumn_JTextField.getText().trim();
    DataSourceColumn = __DataSourceColumn_JTextField.getText().trim();
    DataTypeColumn = __DataTypeColumn_JTextField.getText().trim();
    ScenarioColumn = __ScenarioColumn_JTextField.getText().trim();
    UnitsColumn = __UnitsColumn_JTextField.getText().trim();
    LocationID = __LocationID_JTextField.getText().trim();
    ValueColumn = __ValueColumn_JTextField.getText().trim();
    FlagColumn = __FlagColumn_JTextField.getText().trim();
    DataSource = __DataSource_JTextField.getText().trim();
    DataType = __DataType_JTextField.getText().trim();
    Interval = __Interval_JComboBox.getSelected();
    Scenario = __Scenario_JTextField.getText().trim();
    Units = __Units_JTextField.getText().trim();
    MissingValue = __MissingValue_JTextField.getText().trim();
    Alias = __Alias_JTextField.getText().trim();
	InputStart = __InputStart_JTextField.getText().trim();
	InputEnd = __InputEnd_JTextField.getText().trim();

	props = new PropList(__command.getCommandName());
	props.add("TableID=" + TableID);
//	props.add("SkipRows=" + SkipRows );
    props.add("DateColumn=" + DateColumn );
    props.add("TimeColumn=" + TimeColumn );
    props.add("DateTimeColumn=" + DateTimeColumn );
    props.add("DateTimeFormat=" + DateTimeFormat );
    props.add("LocationTypeColumn=" + LocationTypeColumn );
    props.add("LocationColumn=" + LocationColumn );
    props.add("DataSourceColumn=" + DataSourceColumn );
    props.add("DataTypeColumn=" + DataTypeColumn );
    props.add("ScenarioColumn=" + ScenarioColumn );
    props.add("UnitsColumn=" + UnitsColumn );
    props.add("LocationID=" + LocationID );
    props.add("ValueColumn=" + ValueColumn );
    props.add("FlagColumn=" + FlagColumn );
    props.add("DataSource=" + DataSource );
    props.add("DataType=" + DataType );
    props.add("Interval=" + Interval );
    props.add("Scenario=" + Scenario );
    props.add("Units=" + Units );
    props.add("MissingValue=" + MissingValue );
    props.add("Alias=" + Alias );
	props.add("InputStart=" + InputStart);
	props.add("InputEnd=" + InputEnd);
	
	__Command_JTextArea.setText( __command.toString(props) );
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