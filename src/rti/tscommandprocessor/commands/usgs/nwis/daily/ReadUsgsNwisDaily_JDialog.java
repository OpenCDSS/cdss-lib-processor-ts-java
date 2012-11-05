package rti.tscommandprocessor.commands.usgs.nwis.daily;

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
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
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
import rti.tscommandprocessor.commands.usgs.nwis.daily.UsgsNwisFormatType;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.ChoiceFormatterJPanel;
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
Editor for he ReadUsgsNwisDaily() command.
*/
public class ReadUsgsNwisDaily_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{

private final String __AddWorkingDirectory = "Add Working Directory";
private final String __RemoveWorkingDirectory = "Remove Working Directory";
    
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __browse_JButton = null;
private SimpleJButton __path_JButton = null;
private SimpleJButton __dataStoreDocumentation_JButton = null;
private SimpleJButton __dataStoreOnline_JButton = null;
private ReadUsgsNwisDaily_Command __command = null;
private SimpleJComboBox __DataStore_JComboBox = null;
//private SimpleJComboBox __DataType_JComboBox = null;
private JTextField __Sites_JTextField;
private JTextField __States_JTextField;
private JTextField __HUCs_JTextField;
private JTextField __BoundingBox_JTextField;
private ChoiceFormatterJPanel __Counties_JTextField;
private ChoiceFormatterJPanel __Parameters_JTextField;
private ChoiceFormatterJPanel __Statistics_JTextField;
private JTextField __Agency_JTextField;
private SimpleJComboBox __SiteStatus_JComboBox = null;
private JTextField __SiteTypes_JTextField;
private JTextField __InputStart_JTextField;
private JTextField __InputEnd_JTextField;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private SimpleJComboBox __Format_JComboBox = null;
private JTextField __OutputFile_JTextField = null;
			
private JTextArea __command_JTextArea = null; // Command as JTextArea
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK was pressed when closing the dialog.
private String __working_dir = null; // Working directory.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadUsgsNwisDaily_JDialog ( JFrame parent, ReadUsgsNwisDaily_Command command )
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
            fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser( __working_dir );
        }
        fc.setDialogTitle("Select Time Series File to Write");
        SimpleFileFilter sff = new SimpleFileFilter("json", "USGS NWIS JSON Time Series File");
        fc.addChoosableFileFilter(sff);
        sff = new SimpleFileFilter("rdb", "USGS NWIS RDB Time Series File");
        fc.addChoosableFileFilter(sff);
        sff = new SimpleFileFilter("xml", "USGS NWIS WaterML Time Series File");
        fc.addChoosableFileFilter(sff);
        sff = new SimpleFileFilter("waterml", "USGS NWIS WaterML Time Series File");
        fc.addChoosableFileFilter(sff);
        
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String directory = fc.getSelectedFile().getParent();
            String filename = fc.getSelectedFile().getName(); 
            String path = fc.getSelectedFile().getPath(); 
    
            if (filename == null || filename.equals("")) {
                return;
            }
    
            if (path != null) {
                __OutputFile_JTextField.setText(path );
                JGUIUtil.setLastFileDialogDirectory(directory );
                refresh();
            }
        }
    }
    else if ( o == __cancel_JButton ) {
		response ( false );
	}
    else if ( o == __dataStoreDocumentation_JButton ) {
        try {
            Desktop desktop = Desktop.getDesktop();
            desktop.browse ( new URI(__dataStoreDocumentation_JButton.getActionCommand()) );
        }
        catch ( Exception e ) {
            Message.printWarning(1, null, "Unable to display USGS NWIS web service documentation using \"" +
                __dataStoreDocumentation_JButton.getActionCommand() + "\"" );
        }
    }
    else if ( o == __dataStoreOnline_JButton ) {
        try {
            Desktop desktop = Desktop.getDesktop();
            desktop.browse ( new URI(__dataStoreOnline_JButton.getActionCommand()) );
        }
        catch ( Exception e ) {
            Message.printWarning(1, null, "Unable to display USGS NWIS web service online using \"" +
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
    else if ( o == __path_JButton ) {
        if ( __path_JButton.getText().equals(__AddWorkingDirectory) ) {
            __OutputFile_JTextField.setText (
            IOUtil.toAbsolutePath(__working_dir, __OutputFile_JTextField.getText() ) );
        }
        else if ( __path_JButton.getText().equals(__RemoveWorkingDirectory) ) {
            try {
                __OutputFile_JTextField.setText (
                IOUtil.toRelativePath ( __working_dir, __OutputFile_JTextField.getText() ) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1, "ReadUsgsNwisDaily_JDialog", "Error converting file to relative path." );
            }
        }
        refresh ();
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
    UsgsNwisDailyDataStore dataStore = getSelectedDataStore();
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
    String Sites = __Sites_JTextField.getText().trim();
    if ( Sites.length() > 0 ) {
        props.set ( "Sites", Sites );
    }
    String States = __States_JTextField.getText().trim();
    if ( States.length() > 0 ) {
        props.set ( "States", States );
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
    String Parameters = __Parameters_JTextField.getText().trim();
    if ( Parameters.length() > 0 ) {
        props.set ( "Parameters", Parameters );
    }
    String Statistics = __Statistics_JTextField.getText().trim();
    if ( Statistics.length() > 0 ) {
        props.set ( "Statistics", Statistics );
    }
    String Agency = __Agency_JTextField.getText().trim();
    if ( Agency.length() > 0 ) {
        props.set ( "Agency", Agency );
    }
    String SiteStatus = __SiteStatus_JComboBox.getSelected();
    if ( (SiteStatus != null) && (SiteStatus.length() > 0) ) {
        props.set ( "SiteStatus", SiteStatus );
    }
    String SiteTypes = __SiteTypes_JTextField.getText().trim();
    if ( SiteTypes.length() > 0 ) {
        props.set ( "SiteTypes", SiteTypes );
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
    String Format = __Format_JComboBox.getSelected();
    if ( (Format != null) && (Format.length() > 0) ) {
        props.set ( "Format", Format );
    }
    String OutputFile = __OutputFile_JTextField.getText().trim();
    if ( OutputFile.length() > 0 ) {
        props.set ( "OutputFile", OutputFile );
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
    String Sites = __Sites_JTextField.getText().trim();
    String States = __States_JTextField.getText().trim();
    String HUCs = __HUCs_JTextField.getText().trim();
    String BoundingBox = __BoundingBox_JTextField.getText().trim();
    String Counties = __Counties_JTextField.getText().trim();
    String Parameters = __Parameters_JTextField.getText().trim();
    String Statistics = __Statistics_JTextField.getText().trim();
    String Agency = __Agency_JTextField.getText().trim();
    String SiteStatus = __SiteStatus_JComboBox.getSelected();
    String SiteTypes = __SiteTypes_JTextField.getText().trim();
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
	String Alias = __Alias_JTextField.getText().trim();
    String OutputFile = __OutputFile_JTextField.getText().trim();
    String Format = __Format_JComboBox.getSelected();
	__command.setCommandParameter ( "DataStore", DataStore );
	__command.setCommandParameter ( "Sites", Sites );
	__command.setCommandParameter ( "States", States );
	__command.setCommandParameter ( "HUCs", HUCs );
	__command.setCommandParameter ( "BoundingBox", BoundingBox );
	__command.setCommandParameter ( "Counties", Counties );
	__command.setCommandParameter ( "Parameters", Parameters );
	__command.setCommandParameter ( "Statistics", Statistics );
	__command.setCommandParameter ( "Agency", Agency );
	__command.setCommandParameter ( "SiteStatus", SiteStatus );
	__command.setCommandParameter ( "SiteTypes", SiteTypes );
    __command.setCommandParameter ( "InputStart", InputStart );
    __command.setCommandParameter ( "InputEnd", InputEnd );
    __command.setCommandParameter ( "Alias", Alias );
    __command.setCommandParameter ( "Format", Format );
    __command.setCommandParameter ( "OutputFile", OutputFile );
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
private UsgsNwisDailyDataStore getSelectedDataStore ()
{   String routine = getClass().getName() + ".getSelectedDataStore";
    String DataStore = __DataStore_JComboBox.getSelected();
    UsgsNwisDailyDataStore dataStore = (UsgsNwisDailyDataStore)((TSCommandProcessor)
        __command.getCommandProcessor()).getDataStoreForName(
        DataStore, UsgsNwisDailyDataStore.class );
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
private void initialize ( JFrame parent, ReadUsgsNwisDaily_Command command )
{	//String routine = "ReadUsgsNwisDaily_JDialog.initialize";
	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int yMain = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Read one or more time series from the USGS NWIS daily value web service."),
    	0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "<html><b>WARNING - This command can be slow.  Constrain the query to improve performance.</b></html>"),
        0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "<html><b>Common choices are provided for convenience but may not apply (additional enhancements " +
        "to web services may improve intelligent choices in the future).</b></html>"),
        0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Refer to the USGS NWIS Daily Data Store documentation for more information." ), 
        0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"<html>Constrain the query by specifying time series metadata to match.  " +
    	"<b>A location constraint must be specified.</b></html>" ), 
    	0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"If not specified, the input period defaults to the input period from SetInputPeriod()."),
		0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optionally, also write time series to a file," +
        " which can be specified using a full or relative path (relative to the working directory)." ),
        0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The working directory is: " + __working_dir ), 
        0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    __dataStoreDocumentation_JButton = new SimpleJButton ("USGS NWIS Documentation",this);
    JGUIUtil.addComponent(main_JPanel, __dataStoreDocumentation_JButton, 
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __dataStoreDocumentation_JButton.setEnabled(false);
    __dataStoreDocumentation_JButton.setToolTipText("Show the USGS NWIS web service documentation in a browser - " +
        "useful for explaining query parameters.");
    __dataStoreOnline_JButton = new SimpleJButton ("USGS NWIS Online",this);
    JGUIUtil.addComponent(main_JPanel, __dataStoreOnline_JButton, 
        1, yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __dataStoreOnline_JButton.setEnabled(false);
    __dataStoreOnline_JButton.setToolTipText("Show the USGS NWIS web service web page in a browser - " +
        "useful for testing queries.");
    JGUIUtil.addComponent(main_JPanel, new JSeparator(), 
        0, ++yMain, 7, 1, 1, 1, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
   	
   	// List available data stores of the correct type
   	
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data store:"),
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStore_JComboBox = new SimpleJComboBox ( false );
    TSCommandProcessor tsProcessor = (TSCommandProcessor)processor;
    List<DataStore> dataStoreList = tsProcessor.getDataStoresByType( UsgsNwisDailyDataStore.class );
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
    
    // Panel for location
    int yLoc = -1;
    JPanel loc_JPanel = new JPanel();
    loc_JPanel.setLayout( new GridBagLayout() );
    loc_JPanel.setBorder( BorderFactory.createTitledBorder (
        BorderFactory.createLineBorder(Color.black),
        "Location constraint (specify only one constraint)" ));
    JGUIUtil.addComponent( main_JPanel, loc_JPanel,
        0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("Site number(s):"), 
        0, ++yLoc, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Sites_JTextField = new JTextField (20);
    __Sites_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(loc_JPanel, __Sites_JTextField,
        1, yLoc, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("List of 1+ site numbers separated by commas."),
        3, yLoc, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("State(s):"), 
        0, ++yLoc, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __States_JTextField = new JTextField (20);
    __States_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(loc_JPanel, __States_JTextField,
        1, yLoc, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("List of 1+ state abbreviations separated by commas."),
        3, yLoc, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("HUC(s):"), 
        0, ++yLoc, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __HUCs_JTextField = new JTextField (20);
    __HUCs_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(loc_JPanel, __HUCs_JTextField,
        1, yLoc, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(loc_JPanel, new JLabel ("List of 1+ (1 2-digit and/or up to 10 8-digit) HUCs separated by commas."),
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
    
    // Parameters
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Parameter(s):"), 
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Parameters_JTextField = new ChoiceFormatterJPanel ( getSelectedDataStore().getParameterStrings(true),
        "-", "Select a parameter to insert in the text field at right.", "-- Select Parameter --", ",",  20, true );
    __Parameters_JTextField.addKeyListener (this);
    __Parameters_JTextField.addDocumentListener (this);
    JGUIUtil.addComponent(main_JPanel, __Parameters_JTextField,
        1, yMain, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - list of parameter codes separated by commas (default=all)."),
        3, yMain, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Statistics
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Statistic(s):"), 
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Statistics_JTextField = new ChoiceFormatterJPanel ( getSelectedDataStore().getStatisticStrings(true),
        "-", "Select a statistic to insert in the text field at right.", "-- Select Statistic --", ",",  20, true );
    __Statistics_JTextField.addKeyListener (this);
    __Statistics_JTextField.addDocumentListener (this);
    JGUIUtil.addComponent(main_JPanel, __Statistics_JTextField,
        1, yMain, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - list of statistic codes separated by commas (default=all)."),
        3, yMain, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Site status are hard-coded
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Site status:"),
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SiteStatus_JComboBox = new SimpleJComboBox ( false );
    __SiteStatus_JComboBox.add ( "" );
    __SiteStatus_JComboBox.add ( "" + UsgsNwisSiteStatusType.ALL );
    __SiteStatus_JComboBox.add ( "" + UsgsNwisSiteStatusType.ACTIVE );
    __SiteStatus_JComboBox.add ( "" + UsgsNwisSiteStatusType.INACTIVE );
    __SiteStatus_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __SiteStatus_JComboBox,
        1, yMain, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Optional - site status (default=" + UsgsNwisSiteStatusType.ALL + ")."), 
        3, yMain, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Site types
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Site types(s):"), 
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SiteTypes_JTextField = new JTextField (20);
    __SiteTypes_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __SiteTypes_JTextField,
        1, yMain, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - list of site types separated by commas (default=all)."),
        3, yMain, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Agency code
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Agency:"), 
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Agency_JTextField = new JTextField (20);
    __Agency_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __Agency_JTextField,
        1, yMain, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - agency code (default=all)."),
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
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Format:"),
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Format_JComboBox = new SimpleJComboBox ( false );
    __Format_JComboBox.add ( "" );
    __Format_JComboBox.add ( "" + UsgsNwisFormatType.JSON );
    __Format_JComboBox.add ( "" + UsgsNwisFormatType.RDB );
    __Format_JComboBox.add ( "" + UsgsNwisFormatType.WATERML );
    __Format_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Format_JComboBox,
        1, yMain, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Optional - data format (default=" + UsgsNwisFormatType.WATERML + ")."), 
        3, yMain, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output file to write:" ), 
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputFile_JTextField = new JTextField ( 50 );
    __OutputFile_JTextField.addKeyListener ( this );
    __OutputFile_JTextField.setToolTipText (
        "Optional output file to save time series data, which can be read by other commands");
    JGUIUtil.addComponent(main_JPanel, __OutputFile_JTextField,
        1, yMain, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    __browse_JButton = new SimpleJButton ( "Browse", this );
    JGUIUtil.addComponent(main_JPanel, __browse_JButton,
        6, yMain, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);

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

    if ( __working_dir != null ) {
        // Add the button to allow conversion to/from relative path...
        __path_JButton = new SimpleJButton( __RemoveWorkingDirectory, __RemoveWorkingDirectory, this);
        button_JPanel.add ( __path_JButton );
    }
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
	String Sites = "";
    String States = "";
    String HUCs = "";
    String BoundingBox = "";
    String Counties = "";
    String Parameters = "";
    String Statistics = "";
    String Agency = "";
    String SiteStatus = "";
    String SiteTypes = "";
	String InputStart = "";
	String InputEnd = "";
	String Alias = "";
	String Format = "";
	String OutputFile = "";
	PropList props = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		props = __command.getCommandParameters();
		DataStore = props.getValue ( "DataStore" );
		Sites = props.getValue ( "Sites" );
		States = props.getValue ( "States" );
		HUCs = props.getValue ( "HUCs" );
		BoundingBox = props.getValue ( "BoundingBox" );
		Counties = props.getValue ( "Counties" );
		Parameters = props.getValue ( "Parameters" );
		Statistics = props.getValue ( "Statistics" );
		Agency = props.getValue ( "Agency" );
		SiteStatus = props.getValue ( "SiteStatus" );
		SiteTypes = props.getValue ( "SiteTypes" );
		InputStart = props.getValue ( "InputStart" );
		InputEnd = props.getValue ( "InputEnd" );
		Alias = props.getValue ( "Alias" );
		Format = props.getValue ( "Format" );
		OutputFile = props.getValue ( "OutputFile" );
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
        if ( Sites != null ) {
            __Sites_JTextField.setText ( Sites );
        }
        if ( States != null ) {
            __States_JTextField.setText ( States );
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
        if ( Parameters != null ) {
            __Parameters_JTextField.setText ( Parameters );
        }
        if ( Statistics != null ) {
            __Statistics_JTextField.setText ( Statistics );
        }
        if ( SiteTypes != null ) {
            __SiteTypes_JTextField.setText ( SiteTypes );
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(__SiteStatus_JComboBox, SiteStatus, JGUIUtil.NONE, null, null ) ) {
            __SiteStatus_JComboBox.select ( SiteStatus );
        }
        else {
            if ( (SiteStatus == null) || SiteStatus.equals("") ) {
                // New command...select the default...
                __SiteStatus_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "SiteStatus parameter \"" + DataStore + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        if ( Agency != null ) {
            __Agency_JTextField.setText ( Agency );
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
        if ( JGUIUtil.isSimpleJComboBoxItem(__Format_JComboBox, Format, JGUIUtil.NONE, null, null ) ) {
            __Format_JComboBox.select ( Format );
        }
        else {
            if ( (Format == null) || Format.equals("") ) {
                // New command...select the default...
                __Format_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "Format parameter \"" + DataStore + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        if ( OutputFile != null ) {
            __OutputFile_JTextField.setText ( OutputFile );
        }
	}
	// Regardless, reset the command from the fields...
	Alias = __Alias_JTextField.getText().trim();
	// Regardless, reset the command from the fields...
	props = new PropList ( __command.getCommandName() );
	DataStore = __DataStore_JComboBox.getSelected().trim();
    Sites = __Sites_JTextField.getText().trim();
    States = __States_JTextField.getText().trim();
    HUCs = __HUCs_JTextField.getText().trim();
    BoundingBox = __BoundingBox_JTextField.getText().trim();
    Counties = __Counties_JTextField.getText().trim();
    Parameters = __Parameters_JTextField.getText().trim();
    Statistics = __Statistics_JTextField.getText().trim();
    SiteStatus = __SiteStatus_JComboBox.getSelected();
    SiteTypes = __SiteTypes_JTextField.getText().trim();
    Agency = __Agency_JTextField.getText().trim();
    InputEnd = __InputEnd_JTextField.getText().trim();
    InputStart = __InputStart_JTextField.getText().trim();
    Format = __Format_JComboBox.getSelected();
    OutputFile = __OutputFile_JTextField.getText().trim();
    props.add ( "DataStore=" + DataStore );
    props.add ( "Sites=" + Sites );
    props.add ( "States=" + States );
    props.add ( "HUCs=" + HUCs );
    props.add ( "BoundingBox=" + BoundingBox );
    props.add ( "Counties=" + Counties );
    props.add ( "Parameters=" + Parameters );
    props.add ( "Statistics=" + Statistics );
    props.add ( "Agency=" + Agency );
    props.add ( "SiteStatus=" + SiteStatus );
    props.add ( "SiteTypes=" + SiteTypes );
	props.add ( "InputStart=" + InputStart );
	props.add ( "InputEnd=" + InputEnd );
	props.add ( "Alias=" + Alias );
	props.add ( "Format=" + Format );
	props.add ( "OutputFile=" + OutputFile );
	__command_JTextArea.setText( __command.toString ( props ) );
    if ( (OutputFile == null) || (OutputFile.length() == 0) ) {
        if ( __path_JButton != null ) {
            __path_JButton.setEnabled ( false );
        }
    }
    if ( __path_JButton != null ) {
        __path_JButton.setEnabled ( true );
        File f = new File ( OutputFile );
        if ( f.isAbsolute() ) {
            __path_JButton.setText ( __RemoveWorkingDirectory );
        }
        else {
            __path_JButton.setText ( __AddWorkingDirectory );
        }
    }

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