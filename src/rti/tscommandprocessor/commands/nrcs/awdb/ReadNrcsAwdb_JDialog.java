// ReadNrcsAwdb_JDialog - Editor for he ReadNrcsAwdb() command.

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

package rti.tscommandprocessor.commands.nrcs.awdb;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

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

import riverside.datastore.DataStore;
import rti.tscommandprocessor.commands.rccacis.FIPSCounty;
import rti.tscommandprocessor.core.TSCommandProcessor;
import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.ChoiceFormatterJPanel;
import RTi.Util.GUI.DictionaryJDialog;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Editor for he ReadNrcsAwdb() command.
*/
@SuppressWarnings("serial")
public class ReadNrcsAwdb_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SimpleJButton __dataStoreDocumentation_JButton = null;
private SimpleJButton __dataStoreOnline_JButton = null;
private ReadNrcsAwdb_Command __command = null;
private SimpleJComboBox __DataStore_JComboBox = null;
private SimpleJComboBox __Interval_JComboBox = null;
private JTabbedPane __main_JTabbedPane = null;
private JTextField __Stations_JTextField;
private JTextField __States_JTextField;
private ChoiceFormatterJPanel __Networks_JTextField;
private JTextField __HUCs_JTextField;
private JTextField __BoundingBox_JTextField;
private ChoiceFormatterJPanel __Counties_JTextField;
private SimpleJComboBox __ReadForecast_JComboBox = null;
private SimpleJComboBox __ForecastPeriod_JComboBox = null;
private JTextField __ForecastTableID_JTextField = null;
private JTextField __ForecastPublicationDateStart_JTextField;
private JTextField __ForecastPublicationDateEnd_JTextField;
private JTextField __ForecastExceedanceProbabilities_JTextField;
private ChoiceFormatterJPanel __Elements_JTextField;
private JTextField __ElevationMax_JTextField;
private JTextField __ElevationMin_JTextField;
private JTextField __InputStart_JTextField;
private JTextField __InputEnd_JTextField;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private JTextField __TimeZoneMap_JTextField = null;
			
private JTextArea __command_JTextArea = null; // Command as JTextArea
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK was pressed when closing the dialog.
private JFrame __parent = null;

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
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "ReadNrcsAwdb");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput ();
		if ( !__error_wait ) {
			response ( true );
		}
	}
    else if ( event.getActionCommand().equalsIgnoreCase("EditTimeZoneMap") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String TimeZoneMap = __TimeZoneMap_JTextField.getText().trim();
        String [] notes = {
            "Time zones from the NRCS AWDB property stationDataTimeZone can translated in output.",
            "For example, convert -8.0 to PST, the latter of which is a more standard representation.",
            "The translated time zone may be needed to properly use in other commands, datastores, etc.",
            "The date/time numerical values will not be changed - only the time zone string is set.",
            "See the TSTool Date/time tool for time zone information."
        };
        String dict = (new DictionaryJDialog ( __parent, true, TimeZoneMap,
            "Edit TimeZone Parameter", notes, "Original NRCS AWDB TimeZone", "Time Zone to Use",10)).response();
        if ( dict != null ) {
        	__TimeZoneMap_JTextField.setText ( dict );
            refresh();
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
    String ReadForecast = __ReadForecast_JComboBox.getSelected();
    if ( ReadForecast.length() > 0 ) {
        props.set ( "ReadForecast", ReadForecast );
    }
    String ForecastTableID = __ForecastTableID_JTextField.getText().trim();
    if ( ForecastTableID.length() > 0 ) {
        props.set ( "ForecastTableID", ForecastTableID );
    }
    String ForecastPeriod = __ForecastPeriod_JComboBox.getSelected();
    if ( ForecastPeriod.length() > 0 ) {
        props.set ( "ForecastPeriod", ForecastPeriod );
    }
    String ForecastPublicationDateStart = __ForecastPublicationDateStart_JTextField.getText().trim();
    if ( ForecastPublicationDateStart.length() > 0 ) {
        props.set ( "ForecastPublicationDateStart", ForecastPublicationDateStart );
    }
    String ForecastPublicationDateEnd = __ForecastPublicationDateEnd_JTextField.getText().trim();
    if ( ForecastPublicationDateEnd.length() > 0 ) {
        props.set ( "ForecastPublicationDateEnd", ForecastPublicationDateEnd );
    }
    String ForecastExceedanceProbabilities = __ForecastExceedanceProbabilities_JTextField.getText().trim();
    if ( ForecastExceedanceProbabilities.length() > 0 ) {
        props.set ( "forecastExceedanceProbabilities", ForecastExceedanceProbabilities );
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
	String TimeZoneMap = __TimeZoneMap_JTextField.getText().trim();
    if ( TimeZoneMap.length() > 0 ) {
        props.set ( "TimeZoneMap", TimeZoneMap );
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
    String ReadForecast = __ReadForecast_JComboBox.getSelected();
    String ForecastTableID = __ForecastTableID_JTextField.getText().trim();
    String ForecastPeriod = __ForecastPeriod_JComboBox.getSelected();
    String ForecastPublicationDateStart = __ForecastPublicationDateStart_JTextField.getText().trim();
    String ForecastPublicationDateEnd = __ForecastPublicationDateEnd_JTextField.getText().trim();
    String ForecastExceedanceProbabilities = __ForecastExceedanceProbabilities_JTextField.getText().trim();
    String Elements = __Elements_JTextField.getText().trim();
    String ElevationMax = __ElevationMax_JTextField.getText().trim();
    String ElevationMin = __ElevationMin_JTextField.getText().trim();
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
	String TimeZoneMap = __TimeZoneMap_JTextField.getText().trim();
	String Alias = __Alias_JTextField.getText().trim();
	__command.setCommandParameter ( "DataStore", DataStore );
    __command.setCommandParameter ( "Interval", Interval );
	__command.setCommandParameter ( "Stations", Stations );
	__command.setCommandParameter ( "States", States );
	__command.setCommandParameter ( "Networks", Networks );
	__command.setCommandParameter ( "HUCs", HUCs );
	__command.setCommandParameter ( "BoundingBox", BoundingBox );
	__command.setCommandParameter ( "Counties", Counties );
	__command.setCommandParameter ( "ReadForecast", ReadForecast );
	__command.setCommandParameter ( "ForecastTableID", ForecastTableID );
	__command.setCommandParameter ( "ForecastPeriod", ForecastPeriod );
    __command.setCommandParameter ( "ForecastPublicationDateStart", ForecastPublicationDateStart );
    __command.setCommandParameter ( "ForecastPublicationDateEnd", ForecastPublicationDateEnd );
    __command.setCommandParameter ( "ForecastExceedanceProbabilities", ForecastExceedanceProbabilities );
	__command.setCommandParameter ( "Elements", Elements );
	__command.setCommandParameter ( "ElevationMax", ElevationMax );
	__command.setCommandParameter ( "ElevationMin", ElevationMin );
    __command.setCommandParameter ( "InputStart", InputStart );
    __command.setCommandParameter ( "InputEnd", InputEnd );
    __command.setCommandParameter ( "TimeZoneMap", TimeZoneMap );
    __command.setCommandParameter ( "Alias", Alias );
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
{
	__parent = parent;
	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int yMain = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Read one or more time series from the Natural Resources Conservation Service (NRCS) " +
    	"Air and Water Database (AWDB) web service OR read forecast data as a table."),
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
    	"Constrain the query by specifying time series metadata to match.  Station number, state, and " +
    	"network triplet is unique in NRCS AWDB system." ), 
    	0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"If not specified, the input period defaults to the input period from SetInputPeriod() (or read all data)."),
		0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

   	// Put the buttons in vertical slots that are less width than content below, to conserve space
    __dataStoreDocumentation_JButton = new SimpleJButton ("NRCS AWDB Documentation",this);
    JGUIUtil.addComponent(main_JPanel, __dataStoreDocumentation_JButton, 
        1, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __dataStoreDocumentation_JButton.setEnabled(false);
    __dataStoreDocumentation_JButton.setToolTipText("Show the NRCS AWDB web service documentation in a browser - " +
        "useful for explaining query parameters.");
    __dataStoreOnline_JButton = new SimpleJButton ("NRCS AWDB Online",this);
    JGUIUtil.addComponent(main_JPanel, __dataStoreOnline_JButton, 
        2, yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __dataStoreOnline_JButton.setEnabled(false);
    __dataStoreOnline_JButton.setToolTipText("Show the NRCS AWDB web service web page in a browser - " +
        "useful for testing queries.");
   	
   	JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
   	// List available data stores of the correct type
   	
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data store:"),
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStore_JComboBox = new SimpleJComboBox ( false );
    TSCommandProcessor tsProcessor = (TSCommandProcessor)processor;
    List<DataStore> dataStoreList = tsProcessor.getDataStoresByType( NrcsAwdbDataStore.class );
    List<String> datastoreChoices = new ArrayList<String>();
    for ( DataStore dataStore: dataStoreList ) {
    	datastoreChoices.add ( dataStore.getName() );
    }
    if ( dataStoreList.size() > 0 ) {
    	__DataStore_JComboBox.setData(datastoreChoices);
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
    __Interval_JComboBox.add("Hour");
    __Interval_JComboBox.add("Day");
    __Interval_JComboBox.add("Month");
    __Interval_JComboBox.add("Year");
    __Interval_JComboBox.add("Irregular");
    __Interval_JComboBox.setToolTipText("Irregular = instantaneous, NRCS has indicated the method is deprecated so use Hour if possible.");
    // Select a default...
    __Interval_JComboBox.select ( 0 );
    __Interval_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Interval_JComboBox,
        1, yMain, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required - data interval for time series."),
        3, yMain, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    __main_JTabbedPane = new JTabbedPane ();
    //__main_JTabbedPane.setBorder(
    //    BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
    //    "Specify SQL" ));
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++yMain, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
     
    // Panel for location filter parameters
    int yLoc = -1;
    JPanel loc_JPanel = new JPanel();
    loc_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Location Constraints", loc_JPanel );
    
    JGUIUtil.addComponent(loc_JPanel, new JLabel (
        "Specify one or more location constraints to filter the query.  Unconstrained queries can be VERY SLOW."),
        0, ++yLoc, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(loc_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yLoc, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
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
    
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("Network(s):"), 
        0, ++yLoc, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    List<String> networkStrings = getSelectedDataStore().getNetworkStrings(true);
    __Networks_JTextField = new ChoiceFormatterJPanel ( networkStrings,
        "-", "Select a network code to insert in the text field at right.", "-- Select Network --", ",", 20, true );
    __Networks_JTextField.getSimpleJComboBox().setMaximumRowCount(networkStrings.size());
    __Networks_JTextField.addKeyListener (this);
    __Networks_JTextField.addDocumentListener (this);
    JGUIUtil.addComponent(loc_JPanel, __Networks_JTextField,
        1, yLoc, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("List of 1+ network codes separated by commas (default=all)."),
        3, yLoc, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("HUC(s):"), 
        0, ++yLoc, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __HUCs_JTextField = new JTextField (20);
    __HUCs_JTextField.setToolTipText("Specify 8-12 digits, with * at end if matching a HUC pattern.");
    __HUCs_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(loc_JPanel, __HUCs_JTextField,
        1, yLoc, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("List of 1+ (12-digit) HUCs separated by commas."),
        3, yLoc, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("Bounding box:"), 
        0, ++yLoc, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __BoundingBox_JTextField = new JTextField (40);
    __BoundingBox_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(loc_JPanel, __BoundingBox_JTextField,
        1, yLoc, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("Bounding box: WestLon,SouthLat,EastLon,NorthLat"),
        3, yLoc, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("FIPS counties:"), 
        0, ++yLoc, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    // Get the global county FIPS data
    List<FIPSCounty> counties = FIPSCounty.getData();
    List<String> countyList = new Vector<String>();
    for ( FIPSCounty fips : counties ) {
        countyList.add(fips.getName() + ", " + fips.getStateAbbreviation() + " (" + fips.getCode() + ")" );
    }
    __Counties_JTextField = new ChoiceFormatterJPanel ( countyList, ",",
        "Select a FIPS county to insert in the text field at right.", "-- Select County --", ",",  20, true );
    __Counties_JTextField.addKeyListener (this);
    __Counties_JTextField.addDocumentListener (this);
    JGUIUtil.addComponent(loc_JPanel, __Counties_JTextField,
        1, yLoc, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("List of 1+ counties separated by commas."),
        3, yLoc, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for reservoir parameters
    int yRes = -1;
    JPanel res_JPanel = new JPanel();
    res_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Reservoirs", res_JPanel );
    
    JGUIUtil.addComponent(res_JPanel, new JLabel (
        "Reservoirs are associated with the \"BOR\" network in the location constraints."),
        0, ++yRes, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(res_JPanel, new JLabel (
        "Specify appropriate reservoir data element codes such as REST for reservoir stage and RESC for reservoir volume."),
        0, ++yRes, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(res_JPanel, new JLabel (
        "Reservoir daily data can be converted to end of month values using the NewEndOfMonthTSFromDayTS() command."),
        0, ++yRes, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(res_JPanel, new JLabel (
        "Time series identifiers with \"BOR\" network also will result in reservoir metadata being set as time series properties."),
        0, ++yRes, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Panel for forecasts
    int yFc = -1;
    JPanel fc_JPanel = new JPanel();
    fc_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Forecasts", fc_JPanel );
    
    JGUIUtil.addComponent(fc_JPanel, new JLabel (
        "Specify ReadForecast=True to read forecasts.  The location constraints will filter the stations."),
        0, ++yFc, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(fc_JPanel, new JLabel (
        "The following element types have forecasts:  SRVO"),
        0, ++yFc, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(fc_JPanel, new JLabel (
        "<html>Forecasts are a list of values and corresponding exceedance probabilities for the forecast period.  " +
        "Consequently, <b>output is a table rather than a time series</b>.</html>"),
        0, ++yFc, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(fc_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yFc, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(fc_JPanel, new JLabel( "Read forecast?:"),
        0, ++yFc, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ReadForecast_JComboBox = new SimpleJComboBox ( false );
    __ReadForecast_JComboBox.add("");
    __ReadForecast_JComboBox.add(__command._False);
    __ReadForecast_JComboBox.add(__command._True);
    // Select a default...
    __ReadForecast_JComboBox.select ( 0 );
    __ReadForecast_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(fc_JPanel, __ReadForecast_JComboBox,
        1, yFc, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(fc_JPanel, new JLabel (
        "Optional - read forecast table? (default=" + __command._False + ")."),
        3, yFc, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(fc_JPanel, new JLabel ("Forecast table ID:"), 
        0, ++yFc, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ForecastTableID_JTextField = new JTextField (20);
    __ForecastTableID_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(fc_JPanel, __ForecastTableID_JTextField,
        1, yFc, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(fc_JPanel, new JLabel ("Optional - ID for output forecast table (default=\"NRCS_Forecasts\")."),
        3, yFc, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(fc_JPanel, new JLabel( "Forecast period:"),
        0, ++yFc, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ForecastPeriod_JComboBox = new SimpleJComboBox ( true ); // Editable because property may be entered
    List<String> periodList = new ArrayList<String>();
    try {
    	periodList = getSelectedDataStore().getForecastPeriodStrings();
    }
    catch ( Exception e ) {
    	periodList.add("Error getting forecast periods");
    }
    periodList.add(0,"");
    __ForecastPeriod_JComboBox.setData(periodList);
    // Select a default...
    __ForecastPeriod_JComboBox.select ( 0 );
    __ForecastPeriod_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(fc_JPanel, __ForecastPeriod_JComboBox,
        1, yFc, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(fc_JPanel, new JLabel (
        "Required if forecast read - forecast period."),
        3, yFc, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(fc_JPanel, new JLabel ("Forecast publication date start:"), 
        0, ++yFc, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ForecastPublicationDateStart_JTextField = new JTextField (20);
    __ForecastPublicationDateStart_JTextField.setToolTipText("Typically the first of the month YYYY-MM-DD");
    __ForecastPublicationDateStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(fc_JPanel, __ForecastPublicationDateStart_JTextField,
        1, yFc, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(fc_JPanel, new JLabel ("Optional - YYYY-MM-DD (default=all published forecasts)."),
        3, yFc, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(fc_JPanel, new JLabel ( "Forecast publication date end:"), 
        0, ++yFc, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ForecastPublicationDateEnd_JTextField = new JTextField (20);
    __ForecastPublicationDateEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(fc_JPanel, __ForecastPublicationDateEnd_JTextField,
        1, yFc, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(fc_JPanel, new JLabel ( "Optional - YYYY-MM-DD (default=all published forecasts)."),
        3, yFc, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(fc_JPanel, new JLabel ("Forecast exceedance probabilities:"), 
        0, ++yFc, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ForecastExceedanceProbabilities_JTextField = new JTextField (20);
    __ForecastExceedanceProbabilities_JTextField.setToolTipText("Specify probabilities to read as comma-separated integers 10,30,50,70,90");
    __ForecastExceedanceProbabilities_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(fc_JPanel, __ForecastExceedanceProbabilities_JTextField,
        1, yFc, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(fc_JPanel, new JLabel ("Optional - probabilities to return (default=all available)."),
        3, yFc, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Generic parameters
    
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
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Time zone map:"),
        0, ++yMain, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TimeZoneMap_JTextField = new JTextField(35);
    __TimeZoneMap_JTextField.setToolTipText("OriginalTimeZone1:AssignedTimeZone1,OriginalTimeZone2:AssignedTimeZone2");
    __TimeZoneMap_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __TimeZoneMap_JTextField,
        1, yMain, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - assign time zone ID (default=from data)."),
        3, yMain, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(main_JPanel, new SimpleJButton ("Edit","EditTimeZoneMap",this),
        3, ++yMain, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
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

	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );
	__ok_JButton.setToolTipText("Save changes to command");
	__cancel_JButton = new SimpleJButton( "Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + " Command" );

    pack();
    JGUIUtil.center( this );
	refresh();	// Sets the __path_JButton status
	// Dialogs do not need to be resizable...
	setResizable ( false );
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
    String ReadForecast = "";
    String ForecastTableID = "";
    String ForecastPeriod = "";
    String ForecastPublicationDateStart = "";
    String ForecastPublicationDateEnd = "";
    String ForecastExceedanceProbabilities = "";
    String Elements = "";
    String ElevationMin = "";
    String ElevationMax = "";
	String InputStart = "";
	String InputEnd = "";
	String TimeZoneMap = "";
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
		ReadForecast = props.getValue ( "ReadForecast" );
        ForecastTableID = props.getValue ( "ForecastTableID" );
        ForecastPeriod = props.getValue ( "ForecastPeriod" );
	    ForecastPublicationDateStart = props.getValue ( "ForecastPublicationDateStart" );
	    ForecastPublicationDateEnd = props.getValue ( "ForecastPublicationDateEnd" );
	    ForecastExceedanceProbabilities = props.getValue ( "ForecastExceedanceProbabilities" );
		Elements = props.getValue ( "Elements" );
		ElevationMin = props.getValue ( "ElevationMin" );
		ElevationMax = props.getValue ( "ElevationMax" );
		InputStart = props.getValue ( "InputStart" );
		InputEnd = props.getValue ( "InputEnd" );
		TimeZoneMap = props.getValue ( "TimeZoneMap" );
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
        if ( JGUIUtil.isSimpleJComboBoxItem(__ReadForecast_JComboBox, ReadForecast, JGUIUtil.NONE, null, null ) ) {
            __ReadForecast_JComboBox.select ( ReadForecast );
            if ( (ReadForecast != null) && ReadForecast.equalsIgnoreCase(__command._True) ) {
                __main_JTabbedPane.setSelectedIndex(2);
            }
        }
        else {
            if ( (ReadForecast == null) || ReadForecast.equals("") ) {
                // New command...select the default...
                if ( __ReadForecast_JComboBox.getItemCount() > 0 ) {
                    __ReadForecast_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "ReadForecast parameter \"" + ReadForecast + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        if ( ForecastTableID != null ) {
            __ForecastTableID_JTextField.setText ( ForecastTableID );
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(__ForecastPeriod_JComboBox, ForecastPeriod, JGUIUtil.NONE, null, null ) ) {
            __ForecastPeriod_JComboBox.select ( ForecastPeriod );
        }
        else {
            if ( (ForecastPeriod == null) || ForecastPeriod.equals("") ) {
                // New command...select the default...
                if ( __ForecastPeriod_JComboBox.getItemCount() > 0 ) {
                    __ForecastPeriod_JComboBox.select ( 0 );
                }
            }
            else {
                // Not found, add to list if has ${Property} notation
                if ( ForecastPeriod.startsWith("${") ) {
                    __ForecastPeriod_JComboBox.add(ForecastPeriod);
                    __ForecastPeriod_JComboBox.select(ForecastPeriod);
                }
                else {
                    // Bad user command...
                    Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                      "ForecastPeriod parameter \"" + ForecastPeriod + "\".  Select a\ndifferent value or Cancel." );
                }
            }
        }
        if ( ForecastPublicationDateStart != null ) {
            __ForecastPublicationDateStart_JTextField.setText ( ForecastPublicationDateStart );
        }
        if ( ForecastPublicationDateEnd != null ) {
            __ForecastPublicationDateEnd_JTextField.setText ( ForecastPublicationDateEnd );
        }
        if ( ForecastExceedanceProbabilities != null ) {
            __ForecastExceedanceProbabilities_JTextField.setText ( ForecastExceedanceProbabilities );
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
        if ( TimeZoneMap != null ) {
            __TimeZoneMap_JTextField.setText ( TimeZoneMap );
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
    ReadForecast = __ReadForecast_JComboBox.getSelected();
    ForecastTableID = __ForecastTableID_JTextField.getText().trim();
    ForecastPeriod = __ForecastPeriod_JComboBox.getSelected();
    ForecastPublicationDateStart = __ForecastPublicationDateStart_JTextField.getText().trim();
    ForecastPublicationDateEnd = __ForecastPublicationDateEnd_JTextField.getText().trim();
    ForecastExceedanceProbabilities = __ForecastExceedanceProbabilities_JTextField.getText().trim();
    Elements = __Elements_JTextField.getText().trim();
    ElevationMin = __ElevationMin_JTextField.getText().trim();
    ElevationMax = __ElevationMax_JTextField.getText().trim();
    InputStart = __InputStart_JTextField.getText().trim();
    InputEnd = __InputEnd_JTextField.getText().trim();
    TimeZoneMap = __TimeZoneMap_JTextField.getText().trim();
    props.add ( "DataStore=" + DataStore );
    props.add ( "Interval=" + Interval );
    props.add ( "Stations=" + Stations );
    props.add ( "States=" + States );
    props.add ( "Networks=" + Networks );
    props.add ( "HUCs=" + HUCs );
    props.add ( "BoundingBox=" + BoundingBox );
    props.add ( "Counties=" + Counties );
    props.add ( "ReadForecast=" + ReadForecast );
    props.add ( "ForecastTableID=" + ForecastTableID );
    props.add ( "ForecastPeriod=" + ForecastPeriod );
    props.add ( "ForecastPublicationDateStart=" + ForecastPublicationDateStart );
    props.add ( "ForecastPublicationDateEnd=" + ForecastPublicationDateEnd );
    props.add ( "ForecastExceedanceProbabilities=" + ForecastExceedanceProbabilities );
    props.add ( "Elements=" + Elements );
    props.add ( "ElevationMin=" + ElevationMin );
    props.add ( "ElevationMax=" + ElevationMax );
	props.add ( "InputStart=" + InputStart );
	props.add ( "InputEnd=" + InputEnd );
	props.add ( "TimeZoneMap=" + TimeZoneMap );
	props.add ( "Alias=" + Alias );
	__command_JTextArea.setText( __command.toString ( props ).trim() );

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
