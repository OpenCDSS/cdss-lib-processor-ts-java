package rti.tscommandprocessor.commands.ts;

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
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.DictionaryJDialog;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Time.TimeInterval;

/**
Editor for the ReadTimeSeriesList() command.
*/
@SuppressWarnings("serial")
public class ReadTimeSeriesList_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private ReadTimeSeriesList_Command __command = null;
private JTextArea __command_JTextArea = null;
private SimpleJComboBox __TableID_JComboBox = null;
private SimpleJComboBox __ReadData_JComboBox = null;
private JTabbedPane __main_JTabbedPane = null;
private JTextField __LocationTypeColumn_JTextField = null;
private JTextField __LocationType_JTextField = null;
private JTextField __LocationColumn_JTextField = null;
private JTextField __DataSourceColumn_JTextField = null;
private JTextField __DataSource_JTextField = null;
private JTextField __DataTypeColumn_JTextField = null;
private JTextField __DataType_JTextField = null;
private SimpleJComboBox __Interval_JComboBox = null;
private JTextField __Scenario_JTextField = null;
private JTextField __DataStoreColumn_JTextField = null;
private JTextField __DataStore_JTextField = null;
private JTextField __InputName_JTextField = null;
private JTextArea __ColumnProperties_JTextArea = null;
private JTextArea __Properties_JTextArea = null;
private SimpleJComboBox	__IfNotFound_JComboBox = null;
private JTextField __DefaultUnits_JTextField = null;
private JTextField __DefaultOutputStart_JTextField = null;
private JTextField __DefaultOutputEnd_JTextField = null;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private JTextField __TimeSeriesCountProperty_JTextField = null;
private JTextField __TimeSeriesReadCountProperty_JTextField = null;
private JTextField __TimeSeriesDefaultCountProperty_JTextField = null;
private JTextField __TimeSeriesIndex1Property_JTextField = null;
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.
private JFrame __parent = null;

/**
Editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadTimeSeriesList_JDialog (	JFrame parent, ReadTimeSeriesList_Command command, List<String> tableIDChoices )
{	super ( parent, true );
	initialize ( parent, command, tableIDChoices );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();

    if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "ReadTimeSeriesList");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
    else if ( event.getActionCommand().equalsIgnoreCase("EditColumnProperties") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String ColumnWidths = __ColumnProperties_JTextArea.getText().trim();
        String [] notes = {
            "Table column values can be set as properties in the time series.",
            "Column Name - column name in the table or * to set all",
            "Property Name - property name to set in the time series, or * to use column name"
        };
        String columnProperties = (new DictionaryJDialog ( __parent, true, ColumnWidths, "Edit ColumnProperties Parameter",
            notes, "Column Name", "Property Name",10)).response();
        if ( columnProperties != null ) {
            __ColumnProperties_JTextArea.setText ( columnProperties );
            refresh();
        }
    }
    else if ( event.getActionCommand().equalsIgnoreCase("EditProperties") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String Properties = __Properties_JTextArea.getText().trim();
        String [] notes = {
            "Time series properties will be assigned after reading.",
            "Use % specifiers to assign properties from internal time series data.",
            "Use ${Property} to assign to a processor property."
        };
        String properties = (new DictionaryJDialog ( __parent, true, Properties, "Edit Properties Parameter",
            notes, "Property", "Property Value",10)).response();
        if ( properties != null ) {
            __Properties_JTextArea.setText ( properties );
            refresh();
        }
    }
}

//Start event handlers for DocumentListener...

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
private void checkInput ()
{   // Put together a list of parameters to check...
    PropList parameters = new PropList ( "" );
    String TableID = __TableID_JComboBox.getSelected();
    String ReadData = __ReadData_JComboBox.getSelected();
    String LocationTypeColumn = __LocationTypeColumn_JTextField.getText().trim();
    String LocationType = __LocationType_JTextField.getText().trim();
    String LocationColumn = __LocationColumn_JTextField.getText().trim();
    String DataSourceColumn = __DataSourceColumn_JTextField.getText().trim();
    String DataSource = __DataSource_JTextField.getText().trim();
    String DataTypeColumn = __DataTypeColumn_JTextField.getText().trim();
    String DataType = __DataType_JTextField.getText().trim();
    String Interval = __Interval_JComboBox.getSelected();
    String Scenario = __Scenario_JTextField.getText().trim();
    String DataStoreColumn = __DataStoreColumn_JTextField.getText().trim();
    String DataStore = __DataStore_JTextField.getText().trim();
    String InputName = __InputName_JTextField.getText().trim();
    String IfNotFound = __IfNotFound_JComboBox.getSelected();
    String DefaultUnits = __DefaultUnits_JTextField.getText().trim();
    String DefaultOutputStart = __DefaultOutputStart_JTextField.getText().trim();
    String DefaultOutputEnd = __DefaultOutputEnd_JTextField.getText().trim();
    String Alias = __Alias_JTextField.getText().trim();
    String ColumnProperties = __ColumnProperties_JTextArea.getText().trim().replace("\n"," ");
    String Properties = __Properties_JTextArea.getText().trim().replace("\n"," ");
    String TimeSeriesCountProperty = __TimeSeriesCountProperty_JTextField.getText().trim();
    String TimeSeriesReadCountProperty = __TimeSeriesReadCountProperty_JTextField.getText().trim();
    String TimeSeriesDefaultCountProperty = __TimeSeriesDefaultCountProperty_JTextField.getText().trim();
    String TimeSeriesIndex1Property = __TimeSeriesIndex1Property_JTextField.getText().trim();
    
    __error_wait = false;

    if ( TableID.length() > 0 ) {
        parameters.set ( "TableID", TableID );
    }
    if ( ReadData.length() > 0 ) {
    	parameters.set ( "ReadData", ReadData );
    }
    if ( LocationTypeColumn.length() > 0 ) {
        parameters.set ( "LocationTypeColumn", LocationTypeColumn );
    }
    if ( LocationType.length() > 0 ) {
        parameters.set ( "LocationType", LocationType );
    }
    if ( LocationColumn.length() > 0 ) {
        parameters.set ( "LocationColumn", LocationColumn );
    }
    if ( DataSourceColumn.length() > 0 ) {
        parameters.set ( "DataSourceColumn", DataSourceColumn );
    }
    if ( DataSource.length() > 0 ) {
        parameters.set ( "DataSource", DataSource );
    }
    if ( DataTypeColumn.length() > 0 ) {
        parameters.set ( "DataTypeColumn", DataTypeColumn );
    }
    if ( DataType.length() > 0 ) {
        parameters.set ( "DataType", DataType );
    }
    if ( Interval.length() > 0 ) {
        parameters.set ( "Interval", Interval );
    }
    if ( Scenario.length() > 0 ) {
        parameters.set ( "Scenario", Scenario );
    }
    if ( DataStoreColumn.length() > 0 ) {
        parameters.set ( "DataStoreColumn", DataStoreColumn );
    }
    if ( DataStore.length() > 0 ) {
        parameters.set ( "DataStore", DataStore );
    }
    if ( InputName.length() > 0 ) {
        parameters.set ( "InputName", InputName );
    }
    if ( IfNotFound.length() > 0 ) {
        parameters.set ( "IfNotFound", IfNotFound );
    }
    if ( DefaultUnits.length() > 0 ) {
        parameters.set ( "DefaultUnits", DefaultUnits );
    }
    if ( DefaultOutputStart.length() > 0 ) {
        parameters.set ( "DefaultOutputStart", DefaultOutputStart );
    }
    if ( DefaultOutputEnd.length() > 0 ) {
        parameters.set ( "DefaultOutputEnd", DefaultOutputEnd );
    }
    if ( Alias.length() > 0 ) {
        parameters.set ( "Alias", Alias );
    }
    if ( ColumnProperties.length() > 0 ) {
        parameters.set ( "ColumnProperties", ColumnProperties );
    }
    if ( Properties.length() > 0 ) {
        parameters.set ( "Properties", Properties );
    }
    if ( TimeSeriesCountProperty.length() > 0 ) {
        parameters.set ( "TimeSeriesCountProperty", TimeSeriesCountProperty );
    }
    if ( TimeSeriesReadCountProperty.length() > 0 ) {
        parameters.set ( "TimeSeriesReadCountProperty", TimeSeriesReadCountProperty );
    }
    if ( TimeSeriesDefaultCountProperty.length() > 0 ) {
        parameters.set ( "TimeSeriesDefaultCountProperty", TimeSeriesDefaultCountProperty );
    }
    if ( TimeSeriesIndex1Property.length() > 0 ) {
        parameters.set ( "TimeSeriesIndex1Property", TimeSeriesIndex1Property );
    }
    try {
        // This will warn the user...
        __command.checkCommandParameters ( parameters, null, 1 );
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
{   String TableID = __TableID_JComboBox.getSelected();
	String ReadData = __ReadData_JComboBox.getSelected();
    String LocationTypeColumn = __LocationTypeColumn_JTextField.getText().trim();
    String LocationType = __LocationType_JTextField.getText().trim();
    String LocationColumn = __LocationColumn_JTextField.getText().trim();
    String DataSourceColumn = __DataSourceColumn_JTextField.getText().trim();
    String DataSource = __DataSource_JTextField.getText().trim();
    String DataTypeColumn = __DataTypeColumn_JTextField.getText().trim();
    String DataType = __DataType_JTextField.getText().trim();
    String Interval = __Interval_JComboBox.getSelected();
    String Scenario = __Scenario_JTextField.getText().trim();
    String DataStoreColumn = __DataStoreColumn_JTextField.getText().trim();
    String DataStore = __DataStore_JTextField.getText().trim();
    String InputName = __InputName_JTextField.getText().trim();
    String IfNotFound = __IfNotFound_JComboBox.getSelected();
    String DefaultUnits = __DefaultUnits_JTextField.getText().trim();
    String DefaultOutputStart = __DefaultOutputStart_JTextField.getText().trim();
    String DefaultOutputEnd = __DefaultOutputEnd_JTextField.getText().trim();
    String Alias = __Alias_JTextField.getText().trim();
    String ColumnProperties = __ColumnProperties_JTextArea.getText().trim().replace("\n"," ");
    String Properties = __Properties_JTextArea.getText().trim().replace("\n"," ");
    String TimeSeriesCountProperty = __TimeSeriesCountProperty_JTextField.getText().trim();
    String TimeSeriesReadCountProperty = __TimeSeriesReadCountProperty_JTextField.getText().trim();
    String TimeSeriesDefaultCountProperty = __TimeSeriesDefaultCountProperty_JTextField.getText().trim();
    String TimeSeriesIndex1Property = __TimeSeriesIndex1Property_JTextField.getText().trim();
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "ReadData", ReadData );
    __command.setCommandParameter ( "DataTypeColumn", DataTypeColumn );
    __command.setCommandParameter ( "DataType", DataType );
    __command.setCommandParameter ( "LocationTypeColumn", LocationTypeColumn );
    __command.setCommandParameter ( "LocationType", LocationType );
    __command.setCommandParameter ( "LocationColumn", LocationColumn );
    __command.setCommandParameter ( "DataSourceColumn", DataSourceColumn );
    __command.setCommandParameter ( "DataSource", DataSource );
    __command.setCommandParameter ( "DataTypeColumn", DataTypeColumn );
    __command.setCommandParameter ( "DataType", DataType );
    __command.setCommandParameter ( "Interval", Interval );
    __command.setCommandParameter ( "Scenario", Scenario );
    __command.setCommandParameter ( "DataStoreColumn", DataStoreColumn );
    __command.setCommandParameter ( "DataStore", DataStore );
    __command.setCommandParameter ( "InputName", InputName );
    __command.setCommandParameter ( "IfNotFound", IfNotFound );
    __command.setCommandParameter ( "DefaultUnits", DefaultUnits );
    __command.setCommandParameter ( "DefaultOutputStart", DefaultOutputStart );
    __command.setCommandParameter ( "DefaultOutputEnd", DefaultOutputEnd );
    __command.setCommandParameter ( "Alias", Alias );
    __command.setCommandParameter ( "ColumnProperties", ColumnProperties );
    __command.setCommandParameter ( "Properties", Properties );
    __command.setCommandParameter ( "TimeSeriesCountProperty", TimeSeriesCountProperty );
    __command.setCommandParameter ( "TimeSeriesReadCountProperty", TimeSeriesReadCountProperty );
    __command.setCommandParameter ( "TimeSeriesDefaultCountProperty", TimeSeriesDefaultCountProperty );
    __command.setCommandParameter ( "TimeSeriesIndex1Property", TimeSeriesIndex1Property );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, ReadTimeSeriesList_Command command, List<String> tableIDChoices )
{	__command = command;
    __parent = parent;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Read a list of time series using location identifiers in a table." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Use the SetInputPeriod() command to specify the period to read.  " +
        "Use specific Read*() commands to test reading time series and troubleshoot problems."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Table ID:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit
    __TableID_JComboBox.setToolTipText("Specify the table that is providing the list of time series or use ${Property} notation");
    tableIDChoices.add(0,""); // Add blank to ignore table
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(main_JPanel, __TableID_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required - table containing list of location IDs."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel for General
    int yGen = -1;
    JPanel gen_JPanel = new JPanel();
    gen_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "General", gen_JPanel );
    
    JGUIUtil.addComponent(gen_JPanel, new JLabel (
        "The default is to read time series data."),
        0, ++yGen, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(gen_JPanel, new JLabel (
        "However, it can be useful to not read data, such as when checking time series existence and properties."),
        0, ++yGen, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(gen_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yGen, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(gen_JPanel,new JLabel("Read data?:"),
        0, ++yGen, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ReadData_JComboBox = new SimpleJComboBox ( false );
    __ReadData_JComboBox.setToolTipText("Read data (true) or only read properties (false).");
    List<String> readDataChoices = new ArrayList<String>();
    readDataChoices.add ( "" );
    readDataChoices.add ( __command._False );
    readDataChoices.add ( __command._True );
    __ReadData_JComboBox.setData(readDataChoices);
    __ReadData_JComboBox.select ( __command._Warn );
    __ReadData_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(gen_JPanel, __ReadData_JComboBox,
        1, yGen, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(gen_JPanel, new JLabel (
        "Optional - read data (default="+ __command._True + ")."),
        3, yGen, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
     
    // Panel for TSID
    int yTsid = -1;
    JPanel tsid_JPanel = new JPanel();
    tsid_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "TSID Parts", tsid_JPanel );
    
    JGUIUtil.addComponent(tsid_JPanel, new JLabel (
        "The information specified below is used to create time series identifiers, which are then used to read the time series."),
        0, ++yTsid, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(tsid_JPanel, new JLabel (
        "The time series identifiers (TSIDs) are of the following form, where identifier parts may or may not be required depending on the datastore:"),
        0, ++yTsid, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(tsid_JPanel, new JLabel (
        "  LocationType:LocationID.DataSource.DataType.Interval.Scenario~DataStore~InputName"),
        0, ++yTsid, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(tsid_JPanel, new JLabel (
        "The \"DataStore\" parameter is used generically to mean a database, web service, or file supplying time series data " +
        "(also called \"Input Type\" elsewhere)."),
        0, ++yTsid, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(tsid_JPanel, new JLabel (
        "The TSID parts generally can be specified as a constant or be read from a table column."),
        0, ++yTsid, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(tsid_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yTsid, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(tsid_JPanel, new JLabel ( "Location type column:" ), 
        0, ++yTsid, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LocationTypeColumn_JTextField = new JTextField ( "", 20 );
    __LocationTypeColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(tsid_JPanel, __LocationTypeColumn_JTextField,
        1, yTsid, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(tsid_JPanel, new JLabel ( "Optional or required depending on datastore."),
        3, yTsid, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(tsid_JPanel, new JLabel ( "OR location type:" ), 
        0, ++yTsid, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LocationType_JTextField = new JTextField ( "", 20 );
    __LocationType_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(tsid_JPanel, __LocationType_JTextField,
        1, yTsid, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(tsid_JPanel, new JLabel ( "Optional or required depending on datastore."),
        3, yTsid, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(tsid_JPanel, new JLabel ("Location ID column:"), 
        0, ++yTsid, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LocationColumn_JTextField = new JTextField (10);
    __LocationColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(tsid_JPanel, __LocationColumn_JTextField,
        1, yTsid, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(tsid_JPanel, new JLabel ("Required - name of column containing location IDs."),
        3, yTsid, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(tsid_JPanel, new JLabel ( "Data source column:" ), 
        0, ++yTsid, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataSourceColumn_JTextField = new JTextField ( "", 20 );
    __DataSourceColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(tsid_JPanel, __DataSourceColumn_JTextField,
        1, yTsid, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(tsid_JPanel, new JLabel ( "Optional or required depending on datastore."),
        3, yTsid, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(tsid_JPanel, new JLabel ( "OR data source:" ), 
		0, ++yTsid, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DataSource_JTextField = new JTextField ( "", 20 );
	__DataSource_JTextField.setToolTipText("Specify more than one data source separated by columns to try multiple data sources.");
	__DataSource_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(tsid_JPanel, __DataSource_JTextField,
		1, yTsid, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(tsid_JPanel, new JLabel ( "Optional or required depending on datastore."),
		3, yTsid, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(tsid_JPanel, new JLabel ( "Data type column:" ), 
        0, ++yTsid, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataTypeColumn_JTextField = new JTextField ( "", 20 );
    __DataTypeColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(tsid_JPanel, __DataTypeColumn_JTextField,
        1, yTsid, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(tsid_JPanel, new JLabel ( "Optional or required depending on datastore."),
        3, yTsid, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(tsid_JPanel, new JLabel ( "OR data type:" ), 
		0, ++yTsid, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DataType_JTextField = new JTextField ( "", 20 );
	__DataType_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(tsid_JPanel, __DataType_JTextField,
		1, yTsid, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(tsid_JPanel, new JLabel ( "Optional or required depending on datastore."),
		3, yTsid, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(tsid_JPanel, new JLabel ( "Data interval:"),
        0, ++yTsid, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Interval_JComboBox = new SimpleJComboBox ( false );
    List<String> intervalChoices = TimeInterval.getTimeIntervalChoices(TimeInterval.MINUTE, TimeInterval.YEAR, false, -1, true);
    __Interval_JComboBox.setData(intervalChoices);
    __Interval_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(tsid_JPanel, __Interval_JComboBox,
        1, yTsid, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(tsid_JPanel, new JLabel("Required - data interval (time step) for time series."), 
        3, yTsid, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(tsid_JPanel, new JLabel ( "Scenario:" ), 
		0, ++yTsid, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Scenario_JTextField = new JTextField ( "", 20 );
	__Scenario_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(tsid_JPanel, __Scenario_JTextField,
		1, yTsid, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(tsid_JPanel, new JLabel ( "Optional."),
		3, yTsid, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(tsid_JPanel, new JLabel ( "Datastore column:" ), 
		0, ++yTsid, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DataStoreColumn_JTextField = new JTextField ( "", 20 );
	__DataStoreColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(tsid_JPanel, __DataStoreColumn_JTextField,
		1, yTsid, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(tsid_JPanel, new JLabel ( "Required - needed to identify input database, file, etc."),
		3, yTsid, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(tsid_JPanel, new JLabel ( "OR datastore:" ), 
        0, ++yTsid, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStore_JTextField = new JTextField ( "", 20 );
    __DataStore_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(tsid_JPanel, __DataStore_JTextField,
        1, yTsid, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(tsid_JPanel, new JLabel ( "Required - needed to identify input database, file, etc."),
        3, yTsid, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(tsid_JPanel, new JLabel ( "Input name:" ), 
		0, ++yTsid, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputName_JTextField = new JTextField ( "", 20 );
	__InputName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(tsid_JPanel, __InputName_JTextField,
		1, yTsid, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(tsid_JPanel, new JLabel (
		"Optional - file name if required for datastore."),
		3, yTsid, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(tsid_JPanel, new JLabel("Alias to assign:"),
        0, ++yTsid, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(15);
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(tsid_JPanel, __Alias_JTextField,
        1, yTsid, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(tsid_JPanel, new JLabel ("Required - use %L for location, etc."),
        3, yTsid, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for properties
    int yProp = -1;
    JPanel prop_JPanel = new JPanel();
    prop_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Time Series Properties", prop_JPanel );
    
    JGUIUtil.addComponent(prop_JPanel, new JLabel (
        "Properties can be assigned to the time series from the list table or with data below to facilitate later processing steps and for output."),
        0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(prop_JPanel, new JLabel ("Column properties:"),
        0, ++yProp, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ColumnProperties_JTextArea = new JTextArea (3,35);
    __ColumnProperties_JTextArea.setLineWrap ( true );
    __ColumnProperties_JTextArea.setWrapStyleWord ( true );
    __ColumnProperties_JTextArea.setToolTipText("ColumnName1:TimeSeriesProperty1,ColumnName2:FilterPattern2");
    __ColumnProperties_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(prop_JPanel, new JScrollPane(__ColumnProperties_JTextArea),
        1, yProp, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel ("Optional - set time series properties from table columns."),
        3, yProp, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(prop_JPanel, new SimpleJButton ("Edit","EditColumnProperties",this),
        3, ++yProp, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(prop_JPanel, new JLabel ("Properties:"),
        0, ++yProp, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Properties_JTextArea = new JTextArea (3,35);
    __Properties_JTextArea.setToolTipText("Specify time series properties that should be set, can use ${Property} notation");
    __Properties_JTextArea.setLineWrap ( true );
    __Properties_JTextArea.setWrapStyleWord ( true );
    __Properties_JTextArea.setToolTipText("PropertyName1:PropertyValue1,PropertyName2:PropertyValue2");
    __Properties_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(prop_JPanel, new JScrollPane(__Properties_JTextArea),
        1, yProp, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel ("Optional - string properties to assign to time series."),
        3, yProp, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(prop_JPanel, new SimpleJButton ("Edit","EditProperties",this),
        3, ++yProp, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for if not found
    int yIfNotFound = -1;
    JPanel ifNotFound_JPanel = new JPanel();
    ifNotFound_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "If Time Series is Not Found?", ifNotFound_JPanel );
    
    JGUIUtil.addComponent(ifNotFound_JPanel, new JLabel (
        "The following command parameters control behavior if the time series matching a TSID is not found."),
        0, ++yIfNotFound, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ifNotFound_JPanel, new JLabel (
        "For example, it may be OK to default to an empty time series with all missing values."),
        0, ++yIfNotFound, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ifNotFound_JPanel, new JLabel (
        "Some data sources will NOT return a time series if the requested period is outside of available data."),
        0, ++yIfNotFound, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ifNotFound_JPanel, new JLabel (
        "Specify the default output period to ensure that the default time series will span a period."),
        0, ++yIfNotFound, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ifNotFound_JPanel, new JLabel (
        "The default output period date/times can be specified using processor ${property} notation. The following values are also recognized:"),
        0, ++yIfNotFound, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ifNotFound_JPanel, new JLabel (
        "    CurrentToYear, CurrentToMonth, CurrentToDay, CurrentToHour, CurrentToMinute"),
        0, ++yIfNotFound, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ifNotFound_JPanel, new JLabel (
        "    Simple math:  CurrentToDay - 7Day"),
        0, ++yIfNotFound, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ifNotFound_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yIfNotFound, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(ifNotFound_JPanel,new JLabel("If time series not found?:"),
		0, ++yIfNotFound, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IfNotFound_JComboBox = new SimpleJComboBox ( false );
	List<String> notFoundChoices = new ArrayList<String>();
	notFoundChoices.add ( __command._Default );
	notFoundChoices.add ( __command._Ignore );
	notFoundChoices.add ( __command._Warn );
	__IfNotFound_JComboBox.setData(notFoundChoices);
	__IfNotFound_JComboBox.select ( __command._Warn );
	__IfNotFound_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(ifNotFound_JPanel, __IfNotFound_JComboBox,
		1, yIfNotFound, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ifNotFound_JPanel, new JLabel (
		"Required - how to handle time series that are not found."),
		3, yIfNotFound, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ifNotFound_JPanel, new JLabel ( "Default units:" ), 
        0, ++yIfNotFound, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DefaultUnits_JTextField = new JTextField ( "", 20 );
    __DefaultUnits_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ifNotFound_JPanel, __DefaultUnits_JTextField,
    	1, yIfNotFound, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ifNotFound_JPanel, new JLabel (
	    "Optional - units when IfNotFound=" + __command._Default + "."),
	    3, yIfNotFound, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ifNotFound_JPanel, new JLabel ( "Default output start:" ), 
        0, ++yIfNotFound, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DefaultOutputStart_JTextField = new JTextField ( "", 20 );
    __DefaultOutputStart_JTextField.setToolTipText("Specify the default output start using a date/time string or ${Property} notation");
    __DefaultOutputStart_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ifNotFound_JPanel, __DefaultOutputStart_JTextField,
    	1, yIfNotFound, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ifNotFound_JPanel, new JLabel (
	    "Optional - period start when IfNotFound=" + __command._Default + " to initialize time series."),
	    3, yIfNotFound, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ifNotFound_JPanel, new JLabel ( "Default output end:" ), 
        0, ++yIfNotFound, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DefaultOutputEnd_JTextField = new JTextField ( "", 20 );
    __DefaultOutputEnd_JTextField.setToolTipText("Specify the default output end using a date/time string or ${Property} notation");
    __DefaultOutputEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ifNotFound_JPanel, __DefaultOutputEnd_JTextField,
    	1, yIfNotFound, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ifNotFound_JPanel, new JLabel (
	    "Optional - period end when IfNotFound=" + __command._Default + " to initialize time series."),
	    3, yIfNotFound, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for count properties
    int yCount = -1;
    JPanel count_JPanel = new JPanel();
    count_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Count Properties", count_JPanel );
    
    JGUIUtil.addComponent(count_JPanel, new JLabel (
        "Counts of output time series can be assigned to properties for data checks and output."),
        0, ++yCount, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(count_JPanel, new JLabel (
        "The total count is the sum of time series that are read and defaulted."),
        0, ++yCount, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(count_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yCount, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(count_JPanel, new JLabel ( "Time series count property:" ), 
        0, ++yCount, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TimeSeriesCountProperty_JTextField = new JTextField ( "", 20 );
    __TimeSeriesCountProperty_JTextField.setToolTipText("Specify time series count property to set, can use ${Property} notation");
    __TimeSeriesCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(count_JPanel, __TimeSeriesCountProperty_JTextField,
        1, yCount, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(count_JPanel, new JLabel (
        "Optional - name of property to set to time series total count."),
        3, yCount, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(count_JPanel, new JLabel ( "Time series read count property:" ), 
        0, ++yCount, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TimeSeriesReadCountProperty_JTextField = new JTextField ( "", 20 );
    __TimeSeriesReadCountProperty_JTextField.setToolTipText("Specify time series read count property to set, can use ${Property} notation");
    __TimeSeriesReadCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(count_JPanel, __TimeSeriesReadCountProperty_JTextField,
        1, yCount, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(count_JPanel, new JLabel (
        "Optional - name of property to set to time series read count."),
        3, yCount, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(count_JPanel, new JLabel ( "Time series default count property:" ), 
        0, ++yCount, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TimeSeriesDefaultCountProperty_JTextField = new JTextField ( "", 20 );
    __TimeSeriesDefaultCountProperty_JTextField.setToolTipText("Specify time series default count property to set, can use ${Property} notation");
    __TimeSeriesDefaultCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(count_JPanel, __TimeSeriesDefaultCountProperty_JTextField,
        1, yCount, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(count_JPanel, new JLabel (
        "Optional - name of property to set to time series default count."),
        3, yCount, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(count_JPanel, new JLabel (
        "The index (1+) of time series being read can be set to a property."),
        0, ++yCount, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(count_JPanel, new JLabel (
        "This is useful for outputting time series sequentially, for example to a table column with the number."),
        0, ++yCount, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(count_JPanel, new JLabel ( "Time series index property:" ), 
        0, ++yCount, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TimeSeriesIndex1Property_JTextField = new JTextField ( "", 20 );
    __TimeSeriesIndex1Property_JTextField.setToolTipText("Specify time series index property to set, can use ${Property} notation");
    __TimeSeriesIndex1Property_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(count_JPanel, __TimeSeriesIndex1Property_JTextField,
        1, yCount, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(count_JPanel, new JLabel (
        "Optional - name of property to set for time series read index (1+)."),
        3, yCount, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 4, 55 );
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

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

	setTitle ( "Edit " + __command.getCommandName() + " Command" );
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
{	refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

    refresh();
	if ( code == KeyEvent.VK_ENTER ) {
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
}

public void keyReleased ( KeyEvent event )
{	refresh();
}

public void keyTyped ( KeyEvent event ) {;}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user canceled.
*/
public boolean ok ()
{   return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{   String routine = getClass().getSimpleName() + ".refresh";
    String TableID = "";
    String ReadData = "";
    String LocationTypeColumn = "";
    String LocationType = "";
    String LocationColumn = "";
    String DataSourceColumn = "";
    String DataSource = "";
    String DataTypeColumn = "";
    String DataType = "";
    String Interval = "";
    String Scenario = "";
    String DataStoreColumn = "";
    String DataStore = "";
    String InputName = "";
    String Alias = "";
    String ColumnProperties = "";
    String Properties = "";
    String IfNotFound = "";
    String DefaultUnits = "";
    String DefaultOutputStart = "";
    String DefaultOutputEnd = "";
    String TimeSeriesCountProperty = "";
    String TimeSeriesReadCountProperty = "";
    String TimeSeriesDefaultCountProperty = "";
    String TimeSeriesIndex1Property = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        TableID = props.getValue ( "TableID" );
        LocationTypeColumn = props.getValue ( "LocationTypeColumn" );
        LocationType = props.getValue ( "LocationType" );
        LocationColumn = props.getValue ( "LocationColumn" );
        DataSourceColumn = props.getValue ( "DataSourceColumn" );
        DataSource = props.getValue ( "DataSource" );
        DataTypeColumn = props.getValue ( "DataTypeColumn" );
        DataType = props.getValue ( "DataType" );
        Interval = props.getValue ( "Interval" );
        Scenario = props.getValue ( "Scenario" );
        DataStoreColumn = props.getValue ( "DataStoreColumn" );
        DataStore = props.getValue ( "DataStore" );
        InputName = props.getValue ( "InputName" );
        Alias = props.getValue ( "Alias" );
        ColumnProperties = props.getValue ( "ColumnProperties" );
        Properties = props.getValue ( "Properties" );
        IfNotFound = props.getValue ( "IfNotFound" );
        DefaultUnits = props.getValue ( "DefaultUnits" );
        DefaultOutputStart = props.getValue ( "DefaultOutputStart" );
        DefaultOutputEnd = props.getValue ( "DefaultOutputEnd" );
        TimeSeriesCountProperty = props.getValue ( "TimeSeriesCountProperty" );
        TimeSeriesReadCountProperty = props.getValue ( "TimeSeriesReadCountProperty" );
        TimeSeriesDefaultCountProperty = props.getValue ( "TimeSeriesDefaultCountProperty" );
        TimeSeriesIndex1Property = props.getValue ( "TimeSeriesIndex1Property" );
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
        if ( __ReadData_JComboBox != null ) {
            if ( ReadData == null ) {
                // Select default...
                __ReadData_JComboBox.select ( __command._Warn );
            }
            else {  if (    JGUIUtil.isSimpleJComboBoxItem(
                    __ReadData_JComboBox,
                    ReadData, JGUIUtil.NONE, null, null ) ) {
                    __ReadData_JComboBox.select ( ReadData );
                }
                else {  Message.printWarning ( 1, routine,
                    "Existing command references an invalid\n"+
                    "ReadData \"" + ReadData + "\".  Select a different value or Cancel." );
                }
            }
        }
        if ( LocationTypeColumn != null ) {
            __LocationTypeColumn_JTextField.setText ( LocationTypeColumn );
        }
        if ( LocationType != null ) {
            __LocationType_JTextField.setText ( LocationType );
        }
        if ( LocationColumn != null ) {
            __LocationColumn_JTextField.setText ( LocationColumn );
        }
        if ( DataSourceColumn != null ) {
            __DataSourceColumn_JTextField.setText ( DataSourceColumn );
        }
        if ( DataSource != null ) {
            __DataSource_JTextField.setText ( DataSource );
        }
        if ( DataTypeColumn != null ) {
            __DataTypeColumn_JTextField.setText ( DataTypeColumn );
        }
        if ( DataType != null ) {
            __DataType_JTextField.setText ( DataType );
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(__Interval_JComboBox, Interval, JGUIUtil.NONE, null, null ) ) {
            __Interval_JComboBox.select ( Interval );
        }
        else {
            if ( (Interval == null) || Interval.equals("") ) {
                // New command...select the default...
                __Interval_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "Interval parameter \"" + Interval + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        if ( Scenario != null ) {
            __Scenario_JTextField.setText ( Scenario );
        }
        if ( DataStoreColumn != null ) {
            __DataStoreColumn_JTextField.setText ( DataStoreColumn );
        }
        if ( DataStore != null ) {
            __DataStore_JTextField.setText ( DataStore );
        }
        if ( InputName != null ) {
            __InputName_JTextField.setText ( InputName );
        }
        if ( Alias != null ) {
            __Alias_JTextField.setText ( Alias );
        }
        if ( ColumnProperties != null ) {
            __ColumnProperties_JTextArea.setText ( ColumnProperties );
        }
        if ( Properties != null ) {
            __Properties_JTextArea.setText ( Properties );
        }
        if ( __IfNotFound_JComboBox != null ) {
            if ( IfNotFound == null ) {
                // Select default...
                __IfNotFound_JComboBox.select ( __command._Warn );
            }
            else {  if (    JGUIUtil.isSimpleJComboBoxItem(
                    __IfNotFound_JComboBox,
                    IfNotFound, JGUIUtil.NONE, null, null ) ) {
                    __IfNotFound_JComboBox.select ( IfNotFound );
                }
                else {  Message.printWarning ( 1, routine,
                    "Existing command references an invalid\n"+
                    "IfNotFound \"" + IfNotFound + "\".  Select a\ndifferent value or Cancel." );
                }
            }
        }
        if ( DefaultUnits != null ) {
            __DefaultUnits_JTextField.setText ( DefaultUnits );
        }
        if ( DefaultOutputStart != null ) {
            __DefaultOutputStart_JTextField.setText ( DefaultOutputStart );
        }
        if ( DefaultOutputEnd != null ) {
            __DefaultOutputEnd_JTextField.setText ( DefaultOutputEnd );
        }
        if ( TimeSeriesCountProperty != null ) {
            __TimeSeriesCountProperty_JTextField.setText ( TimeSeriesCountProperty );
        }
        if ( TimeSeriesReadCountProperty != null ) {
            __TimeSeriesReadCountProperty_JTextField.setText ( TimeSeriesReadCountProperty );
        }
        if ( TimeSeriesDefaultCountProperty != null ) {
            __TimeSeriesDefaultCountProperty_JTextField.setText ( TimeSeriesDefaultCountProperty );
        }
        if ( TimeSeriesIndex1Property != null ) {
            __TimeSeriesIndex1Property_JTextField.setText ( TimeSeriesIndex1Property );
        }
    }
    // Regardless, reset the command from the fields...
    TableID = __TableID_JComboBox.getSelected();
    ReadData = __ReadData_JComboBox.getSelected();
    LocationTypeColumn = __LocationTypeColumn_JTextField.getText().trim();
    LocationType = __LocationType_JTextField.getText().trim();
    LocationColumn = __LocationColumn_JTextField.getText().trim();
    DataSourceColumn = __DataSourceColumn_JTextField.getText().trim();
    DataSource = __DataSource_JTextField.getText().trim();
    DataTypeColumn = __DataTypeColumn_JTextField.getText().trim();
    DataType = __DataType_JTextField.getText().trim();
    Interval = __Interval_JComboBox.getSelected();
    Scenario = __Scenario_JTextField.getText().trim();
    DataStoreColumn = __DataStoreColumn_JTextField.getText().trim();
    DataStore = __DataStore_JTextField.getText().trim();
    InputName = __InputName_JTextField.getText().trim();
    Alias = __Alias_JTextField.getText().trim();
    ColumnProperties = __ColumnProperties_JTextArea.getText().trim().replace("\n"," ");
    Properties = __Properties_JTextArea.getText().trim().replace("\n"," ");
    IfNotFound = __IfNotFound_JComboBox.getSelected();
    DefaultUnits = __DefaultUnits_JTextField.getText().trim();
    DefaultOutputStart = __DefaultOutputStart_JTextField.getText().trim();
    DefaultOutputEnd = __DefaultOutputEnd_JTextField.getText().trim();
    TimeSeriesCountProperty = __TimeSeriesCountProperty_JTextField.getText().trim();
    TimeSeriesReadCountProperty = __TimeSeriesReadCountProperty_JTextField.getText().trim();
    TimeSeriesDefaultCountProperty = __TimeSeriesDefaultCountProperty_JTextField.getText().trim();
    TimeSeriesIndex1Property = __TimeSeriesIndex1Property_JTextField.getText().trim();
    props = new PropList ( __command.getCommandName() );
    props.add ( "TableID=" + TableID );
    props.add ( "ReadData=" + ReadData );
    props.add ( "LocationTypeColumn=" + LocationTypeColumn );
    props.add ( "LocationType=" + LocationType );
    props.add ( "LocationColumn=" + LocationColumn );
    props.add ( "DataSourceColumn=" + DataSourceColumn );
    props.add ( "DataSource=" + DataSource );
    props.add ( "DataTypeColumn=" + DataTypeColumn );
    props.add ( "DataType=" + DataType );
    props.add ( "Interval=" + Interval );
    props.add ( "Scenario=" + Scenario );
    props.add ( "DataStoreColumn=" + DataStoreColumn );
    props.add ( "DataStore=" + DataStore );
    props.add ( "InputName=" + InputName );
    props.add ( "Alias=" + Alias );
    props.add ( "ColumnProperties=" + ColumnProperties );
    props.add ( "Properties=" + Properties );
    props.add ( "IfNotFound=" + IfNotFound );
    props.add ( "DefaultUnits=" + DefaultUnits );
    props.add ( "DefaultOutputStart=" + DefaultOutputStart );
    props.add ( "DefaultOutputEnd=" + DefaultOutputEnd );
    props.add ( "TimeSeriesCountProperty=" + TimeSeriesCountProperty );
    props.add ( "TimeSeriesReadCountProperty=" + TimeSeriesReadCountProperty );
    props.add ( "TimeSeriesDefaultCountProperty=" + TimeSeriesDefaultCountProperty );
    props.add ( "TimeSeriesIndex1Property=" + TimeSeriesIndex1Property );
    __command_JTextArea.setText( __command.toString ( props ) );
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
*/
private void response ( boolean ok )
{   __ok = ok;  // Save to be returned by ok()
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