package rti.tscommandprocessor.commands.nrcs.awdb;

import java.awt.Color;
import java.awt.Desktop;
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
import java.net.URI;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.commands.rccacis.FIPSCounty;
import rti.tscommandprocessor.core.TSCommandProcessor;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.ChoiceFormatterJPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Editor for he ReadNrcsAwdb() command.
*/
public class ReadNrcsAwdb_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __dataStoreDocumentation_JButton = null;
private SimpleJButton __dataStoreOnline_JButton = null;
private ReadNrcsAwdb_Command __command = null;
private SimpleJComboBox __DataStore_JComboBox = null;
private SimpleJComboBox __Interval_JComboBox = null;
private JTextField __Stations_JTextField;
private JTextField __States_JTextField;
private ChoiceFormatterJPanel __Networks_JTextField;
private JTextField __HUCs_JTextField;
private JTextField __BoundingBox_JTextField;
private ChoiceFormatterJPanel __Counties_JTextField;
private ChoiceFormatterJPanel __Elements_JTextField;
private JTextField __ElevationMax_JTextField;
private JTextField __ElevationMin_JTextField;
private JTextField __InputStart_JTextField;
private JTextField __InputEnd_JTextField;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
			
private JTextArea __command_JTextArea = null; // Command as JTextArea
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK was pressed when closing the dialog.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadNrcsAwdb_JDialog ( JFrame parent, ReadNrcsAwdb_Command command )
{	super(parent, true);

	initialize ( parent, command );
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
    else if ( o == __dataStoreDocumentation_JButton ) {
        try {
            Desktop desktop = Desktop.getDesktop();
            desktop.browse ( new URI(__dataStoreDocumentation_JButton.getActionCommand()) );
        }
        catch ( Exception e ) {
            Message.printWarning(1, null, "Unable to display NRCS AWDB web service documentation using \"" +
                __dataStoreDocumentation_JButton.getActionCommand() + "\"" );
        }
    }
    else if ( o == __dataStoreOnline_JButton ) {
        try {
            Desktop desktop = Desktop.getDesktop();
            desktop.browse ( new URI(__dataStoreOnline_JButton.getActionCommand()) );
        }
        catch ( Exception e ) {
            Message.printWarning(1, null, "Unable to display NRCS AWDB web service online using \"" +
                __dataStoreOnline_JButton.getActionCommand() + "\"");
        }
    }
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput ();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else {
		refresh();
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
Check the state of the dialog, disabling/enabling components as appropriate.
*/
private void checkGUIState()
{
    // If data store is selected and has property for help, enable the button
    NrcsAwdbDataStore dataStore = getSelectedDataStore();
    if ( dataStore != null ) {
        String urlString = dataStore.getProperty ( "ServiceAPIDocumentationURI" );
        if ( urlString == null ) {
            __dataStoreDocumentation_JButton.setEnabled(false);
        }
        else {
            __dataStoreDocumentation_JButton.setActionCommand(urlString);
            __dataStoreDocumentation_JButton.setEnabled(true);
        }
        urlString = dataStore.getProperty ( "ServiceOnlineURI" );
        if ( urlString == null ) {
            __dataStoreOnline_JButton.setEnabled(false);
        }
        else {
            __dataStoreOnline_JButton.setActionCommand(urlString);
            __dataStoreOnline_JButton.setEnabled(true);
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
	__error_wait = false;
	String DataStore = __DataStore_JComboBox.getSelected();
	if ( (DataStore != null) && (DataStore.length() > 0) ) {
		props.set ( "DataStore", DataStore );
	}
    String Interval = __Interval_JComboBox.getSelected();
    if ( Interval.length() > 0 ) {
        props.set ( "Interval", Interval );
    }
    String Stations = __Stations_JTextField.getText().trim();
    if ( Stations.length() > 0 ) {
        props.set ( "Stations", Stations );
    }
    String States = __States_JTextField.getText().trim();
    if ( States.length() > 0 ) {
        props.set ( "States", States );
    }
    String Networks = __Networks_JTextField.getText().trim();
    if ( Networks.length() > 0 ) {
        props.set ( "Networks", Networks );
    }
    String HUCs = __HUCs_JTextField.getText().trim();
    if ( HUCs.length() > 0 ) {
        props.set ( "HUCs", HUCs );
    }
    String BoundingBox = __BoundingBox_JTextField.getText().trim();
    if ( BoundingBox.length() > 0 ) {
        props.set ( "BoundingBox", BoundingBox );
    }
    String Counties = __Counties_JTextField.getText().trim();
    if ( Counties.length() > 0 ) {
        props.set ( "Counties", Counties );
    }
    String Elements = __Elements_JTextField.getText().trim();
    if ( Elements.length() > 0 ) {
        props.set ( "Elements", Elements );
    }
    String ElevationMax = __ElevationMax_JTextField.getText().trim();
    if ( ElevationMax.length() > 0 ) {
        props.set ( "ElevationMax", ElevationMax );
    }
    String ElevationMin = __ElevationMin_JTextField.getText().trim();
    if ( ElevationMin.length() > 0 ) {
        props.set ( "ElevationMin", ElevationMin );
    }
	String InputStart = __InputStart_JTextField.getText().trim();
	if ( InputStart.length() > 0 ) {
		props.set ( "InputStart", InputStart );
	}
	String InputEnd = __InputEnd_JTextField.getText().trim();
	if ( InputEnd.length() > 0 ) {
		props.set ( "InputEnd", InputEnd );
	}
    String Alias = __Alias_JTextField.getText().trim();
    if ( Alias.length() > 0 ) {
        props.set ( "Alias", Alias );
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
private void commitEdits ()
{	String DataStore = __DataStore_JComboBox.getSelected();
    String Interval = __Interval_JComboBox.getSelected();
    String Stations = __Stations_JTextField.getText().trim();
    String States = __States_JTextField.getText().trim();
    String Networks = __Networks_JTextField.getText().trim();
    String HUCs = __HUCs_JTextField.getText().trim();
    String BoundingBox = __BoundingBox_JTextField.getText().trim();
    String Counties = __Counties_JTextField.getText().trim();
    String Elements = __Elements_JTextField.getText().trim();
    String ElevationMax = __ElevationMax_JTextField.getText().trim();
    String ElevationMin = __ElevationMin_JTextField.getText().trim();
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
	String Alias = __Alias_JTextField.getText().trim();
	__command.setCommandParameter ( "DataStore", DataStore );
    __command.setCommandParameter ( "Interval", Interval );
	__command.setCommandParameter ( "Stations", Stations );
	__command.setCommandParameter ( "States", States );
	__command.setCommandParameter ( "Networks", Networks );
	__command.setCommandParameter ( "HUCs", HUCs );
	__command.setCommandParameter ( "BoundingBox", BoundingBox );
	__command.setCommandParameter ( "Counties", Counties );
	__command.setCommandParameter ( "Elements", Elements );
	__command.setCommandParameter ( "ElevationMax", ElevationMax );
	__command.setCommandParameter ( "ElevationMin", ElevationMin );
    __command.setCommandParameter ( "InputStart", InputStart );
    __command.setCommandParameter ( "InputEnd", InputEnd );
    __command.setCommandParameter ( "Alias", Alias );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__Alias_JTextField = null;
	__InputStart_JTextField = null;
	__InputEnd_JTextField = null;
	__cancel_JButton = null;
	__command_JTextArea = null;
	__command = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Get the selected data store.
*/
private NrcsAwdbDataStore getSelectedDataStore ()
{   String routine = getClass().getName() + ".getSelectedDataStore";
    String DataStore = __DataStore_JComboBox.getSelected();
    NrcsAwdbDataStore dataStore = (NrcsAwdbDataStore)((TSCommandProcessor)
        __command.getCommandProcessor()).getDataStoreForName(
        DataStore, NrcsAwdbDataStore.class );
    if ( dataStore == null ) {
        Message.printStatus(2, routine, "Selected data store is \"" + DataStore + "\"." );
    }
    return dataStore;
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, ReadNrcsAwdb_Command command )
{	//String routine = "ReadNrcsAwdbDaily_JDialog.initialize";
	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int yMain = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Read one or more time series from the National Resource Conservation Service (NRCS) " +
    	"Air and Water Database (AWDB) web service."),
    	0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "<html><b>WARNING - This command can be slow.  Constrain the query to improve performance.</b></html>"),
        0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "<html><b>Common choices are provided for convenience but may not apply (additional enhancements " +
        "to web services may improve intelligent choices in the future).</b></html>"),
        0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Refer to the NRCS AWDB datastore documentation for more information." ), 
        0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"<html>Constrain the query by specifying time series metadata to match.  " +
    	"<b>A location constraint must be specified.</b></html>" ), 
    	0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"If not specified, the input period defaults to the input period from SetInputPeriod()."),
		0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __dataStoreDocumentation_JButton = new SimpleJButton ("NRCS AWDB Documentation",this);
    JGUIUtil.addComponent(main_JPanel, __dataStoreDocumentation_JButton, 
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __dataStoreDocumentation_JButton.setEnabled(false);
    __dataStoreDocumentation_JButton.setToolTipText("Show the NRCS AWDB web service documentation in a browser - " +
        "useful for explaining query parameters.");
    __dataStoreOnline_JButton = new SimpleJButton ("NRCS AWDB Online",this);
    JGUIUtil.addComponent(main_JPanel, __dataStoreOnline_JButton, 
        1, yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __dataStoreOnline_JButton.setEnabled(false);
    __dataStoreOnline_JButton.setToolTipText("Show the NRCS AWDB web service web page in a browser - " +
        "useful for testing queries.");
    JGUIUtil.addComponent(main_JPanel, new JSeparator(), 
        0, ++yMain, 7, 1, 1, 1, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
   	
   	// List available data stores of the correct type
   	
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data store:"),
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStore_JComboBox = new SimpleJComboBox ( false );
    TSCommandProcessor tsProcessor = (TSCommandProcessor)processor;
    List<DataStore> dataStoreList = tsProcessor.getDataStoresByType( NrcsAwdbDataStore.class );
    for ( DataStore dataStore: dataStoreList ) {
        __DataStore_JComboBox.addItem ( dataStore.getName() );
    }
    if ( dataStoreList.size() > 0 ) {
        __DataStore_JComboBox.select ( 0 );
    }
    __DataStore_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataStore_JComboBox,
        1, yMain, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - data store containing data."), 
        3, yMain, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Interval:"),
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Interval_JComboBox = new SimpleJComboBox ( false );
    __Interval_JComboBox.add("Day");
    __Interval_JComboBox.add("Month");
    __Interval_JComboBox.add("Irregular");
    // Select a default...
    __Interval_JComboBox.select ( 0 );
    __Interval_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Interval_JComboBox,
        1, yMain, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required - data interval for time series."),
        3, yMain, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Panel for location
    int yLoc = -1;
    JPanel loc_JPanel = new JPanel();
    loc_JPanel.setLayout( new GridBagLayout() );
    loc_JPanel.setBorder( BorderFactory.createTitledBorder (
        BorderFactory.createLineBorder(Color.black),
        "Location constraints (specify one or more)" ));
    JGUIUtil.addComponent( main_JPanel, loc_JPanel,
        0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("Station ID(s):"), 
        0, ++yLoc, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Stations_JTextField = new JTextField (20);
    __Stations_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(loc_JPanel, __Stations_JTextField,
        1, yLoc, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("List of 1+ station IDs separated by commas."),
        3, yLoc, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("State(s):"), 
        0, ++yLoc, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __States_JTextField = new JTextField (20);
    __States_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(loc_JPanel, __States_JTextField,
        1, yLoc, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("List of 1+ state abbreviations separated by commas."),
        3, yLoc, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("Networks(s):"), 
        0, ++yLoc, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Networks_JTextField = new ChoiceFormatterJPanel ( getSelectedDataStore().getNetworkStrings(true),
        "-", "Select a network code to insert in the text field at right.", "-- Select Network --", ",", 20, true );
    __Networks_JTextField.addKeyListener (this);
    __Networks_JTextField.addDocumentListener (this);
    JGUIUtil.addComponent(loc_JPanel, __Networks_JTextField,
        1, yLoc, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("List of 1+ network codes separated by commas (default=all)."),
        3, yLoc, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("HUC(s):"), 
        0, ++yLoc, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __HUCs_JTextField = new JTextField (20);
    __HUCs_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(loc_JPanel, __HUCs_JTextField,
        1, yLoc, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("List of 1+ (8-digit) HUCs separated by commas."),
        3, yLoc, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("Bounding box:"), 
        0, ++yLoc, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __BoundingBox_JTextField = new JTextField (20);
    __BoundingBox_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(loc_JPanel, __BoundingBox_JTextField,
        1, yLoc, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("Bounding box: WestLon,SouthLat,EastLon,NorthLat"),
        3, yLoc, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("FIPS counties:"), 
        0, ++yLoc, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    // Get the global county FIPS data
    List<FIPSCounty> counties = FIPSCounty.getData();
    List<String> countyList = new Vector();
    for ( FIPSCounty fips : counties ) {
        countyList.add(fips.getCode() + " - " + fips.getName() + ", " + fips.getStateAbbreviation());
    }
    __Counties_JTextField = new ChoiceFormatterJPanel ( countyList, "-",
        "Select a FIPS county to insert in the text field at right.", "-- Select County --", ",",  20, true );
    __Counties_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(loc_JPanel, __Counties_JTextField,
        1, yLoc, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("List of 1+ counties separated by commas."),
        3, yLoc, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Element(s):"), 
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Elements_JTextField = new ChoiceFormatterJPanel ( getSelectedDataStore().getElementStrings(true),
        "-", "Select an element to insert in the text field at right.", "-- Select Element --", ",", 20, true );
    __Elements_JTextField.addKeyListener (this);
    __Elements_JTextField.addDocumentListener (this);
    JGUIUtil.addComponent(main_JPanel, __Elements_JTextField,
        1, yMain, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - list of element codes separated by commas (default=all)."),
        3, yMain, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Elevation, minimum:"), 
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ElevationMin_JTextField = new JTextField (20);
    __ElevationMin_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __ElevationMin_JTextField,
        1, yMain, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - minimum elevation, feet (default=all)."),
        3, yMain, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Elevation, maximum:"), 
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ElevationMax_JTextField = new JTextField (20);
    __ElevationMax_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __ElevationMax_JTextField,
        1, yMain, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - maximum elevation, feet (default=all)."),
        3, yMain, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Input start:"), 
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputStart_JTextField = new JTextField (20);
    __InputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputStart_JTextField,
        1, yMain, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - YYYY-MM-DD, override the global input start."),
        3, yMain, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input end:"), 
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputEnd_JTextField = new JTextField (20);
    __InputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputEnd_JTextField,
        1, yMain, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - YYYY-MM-DD, override the global input end."),
        3, yMain, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(10);
    __Alias_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener(this);
    __Alias_JTextField.setToolTipText("%L for location, %T for data type.");
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, yMain, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - use %L for location, etc. (default=no alias)."),
        3, yMain, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
		0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (4,50);
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, yMain, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	// Refresh the contents...
	refresh ();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++yMain, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__cancel_JButton = new SimpleJButton( "Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );

	setTitle ( "Edit " + __command.getCommandName() + " Command" );

	// Dialogs do not need to be resizable...
	setResizable ( true );
    pack();
    JGUIUtil.center( this );
	refresh();	// Sets the __path_JButton status
    super.setVisible( true );
}

/**
Handle ItemListener events.
*/
public void itemStateChanged ( ItemEvent event )
{
    // If a new data store has been selected, update the data type, interval, list and the input filter
    if ( event.getSource() == __DataStore_JComboBox ) {
        //setDataTypeChoices();
    }
    refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	refresh();
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
public boolean ok ()
{	return __ok;
}

/**
Refresh the command string from the dialog contents.
*/
private void refresh ()
{	String routine = getClass().getName() + ".refresh";
	__error_wait = false;
	String DataStore = "";
    String Interval = "";
	String Stations = "";
    String States = "";
    String Networks = "";
    String HUCs = "";
    String BoundingBox = "";
    String Counties = "";
    String Elements = "";
    String ElevationMin = "";
    String ElevationMax = "";
	String InputStart = "";
	String InputEnd = "";
	String Alias = "";
	PropList props = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		props = __command.getCommandParameters();
		DataStore = props.getValue ( "DataStore" );
		Interval = props.getValue ( "Interval" );
		Stations = props.getValue ( "Stations" );
		States = props.getValue ( "States" );
		Networks = props.getValue ( "Networks" );
		HUCs = props.getValue ( "HUCs" );
		BoundingBox = props.getValue ( "BoundingBox" );
		Counties = props.getValue ( "Counties" );
		Elements = props.getValue ( "Elements" );
		ElevationMin = props.getValue ( "ElevationMin" );
		ElevationMax = props.getValue ( "ElevationMax" );
		InputStart = props.getValue ( "InputStart" );
		InputEnd = props.getValue ( "InputEnd" );
		Alias = props.getValue ( "Alias" );
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataStore_JComboBox, DataStore, JGUIUtil.NONE, null, null ) ) {
            __DataStore_JComboBox.select ( DataStore );
        }
        else {
            if ( (DataStore == null) || DataStore.equals("") ) {
                // New command...select the default...
                if ( __DataStore_JComboBox.getItemCount() > 0 ) {
                    __DataStore_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataStore parameter \"" + DataStore + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(__Interval_JComboBox, Interval, JGUIUtil.NONE, null, null ) ) {
            __Interval_JComboBox.select ( Interval );
        }
        else {
            if ( (Interval == null) || Interval.equals("") ) {
                // New command...select the default...
                if ( __Interval_JComboBox.getItemCount() > 0 ) {
                    __Interval_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "Interval parameter \"" + Interval + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        if ( Stations != null ) {
            __Stations_JTextField.setText ( Stations );
        }
        if ( States != null ) {
            __States_JTextField.setText ( States );
        }
        if ( Networks != null ) {
            __Networks_JTextField.setText ( Networks );
        }
        if ( HUCs != null ) {
            __HUCs_JTextField.setText ( HUCs );
        }
        if ( BoundingBox != null ) {
            __BoundingBox_JTextField.setText ( BoundingBox );
        }
        if ( Counties != null ) {
            __Counties_JTextField.setText ( Counties );
        }
        if ( Elements != null ) {
            __Elements_JTextField.setText ( Elements );
        }
        if ( ElevationMin != null ) {
            __ElevationMin_JTextField.setText ( ElevationMin );
        }
        if ( ElevationMax != null ) {
            __ElevationMax_JTextField.setText ( ElevationMax );
        }
		if ( InputStart != null ) {
			__InputStart_JTextField.setText ( InputStart );
		}
		if ( InputEnd != null ) {
			__InputEnd_JTextField.setText ( InputEnd );
		}
        if ( Alias != null ) {
            __Alias_JTextField.setText ( Alias );
        }
	}
	// Regardless, reset the command from the fields...
	Alias = __Alias_JTextField.getText().trim();
	// Regardless, reset the command from the fields...
	props = new PropList ( __command.getCommandName() );
	DataStore = __DataStore_JComboBox.getSelected().trim();
    Interval = __Interval_JComboBox.getSelected();
    Stations = __Stations_JTextField.getText().trim();
    States = __States_JTextField.getText().trim();
    Networks = __Networks_JTextField.getText().trim();
    HUCs = __HUCs_JTextField.getText().trim();
    BoundingBox = __BoundingBox_JTextField.getText().trim();
    Counties = __Counties_JTextField.getText().trim();
    Elements = __Elements_JTextField.getText().trim();
    ElevationMin = __ElevationMin_JTextField.getText().trim();
    ElevationMax = __ElevationMax_JTextField.getText().trim();
    InputEnd = __InputEnd_JTextField.getText().trim();
    InputStart = __InputStart_JTextField.getText().trim();
    props.add ( "DataStore=" + DataStore );
    props.add ( "Interval=" + Interval );
    props.add ( "Stations=" + Stations );
    props.add ( "States=" + States );
    props.add ( "Networks=" + Networks );
    props.add ( "HUCs=" + HUCs );
    props.add ( "BoundingBox=" + BoundingBox );
    props.add ( "Counties=" + Counties );
    props.add ( "Elements=" + Elements );
    props.add ( "ElevationMin=" + ElevationMin );
    props.add ( "ElevationMax=" + ElevationMax );
	props.add ( "InputStart=" + InputStart );
	props.add ( "InputEnd=" + InputEnd );
	props.add ( "Alias=" + Alias );
	__command_JTextArea.setText( __command.toString ( props ) );

	// Check the GUI state to determine whether some controls should be disabled.

	checkGUIState();
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed
and the dialog is closed.
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