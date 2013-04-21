package rti.tscommandprocessor.commands.reclamationhdb;

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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Vector;

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

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;

import RTi.DMI.DatabaseDataStore;
import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
Editor for the ReadReclamationHDB() command.
*/
public class ReadReclamationHDB_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private ReadReclamationHDB_Command __command = null;
private SimpleJComboBox __DataStore_JComboBox = null;
private SimpleJComboBox __DataType_JComboBox = null;
private SimpleJComboBox __Interval_JComboBox = null;
private JTabbedPane __main_JTabbedPane = null;
private JTabbedPane __inner_JTabbedPane = null;
private SimpleJComboBox __SiteCommonName_JComboBox = null;
private SimpleJComboBox __DataTypeCommonName_JComboBox = null;
private JLabel __selectedSiteID_JLabel = null;
private JLabel __selectedSiteDataTypeID_JLabel = null;
private SimpleJComboBox __SiteDataTypeID_JComboBox = null;
private SimpleJComboBox __ModelName_JComboBox = null;
private SimpleJComboBox __ModelRunName_JComboBox = null;
private SimpleJComboBox __ModelRunDate_JComboBox = null;
private SimpleJComboBox __HydrologicIndicator_JComboBox = null;
private JLabel __selectedModelID_JLabel = null;
private JLabel __selectedModelRunID_JLabel = null;
private SimpleJComboBox __ModelRunID_JComboBox = null;
private SimpleJComboBox __EnsembleName_JComboBox = null;
//private TSFormatSpecifiersJPanel __EnsembleTraceID_JTextField = null;
private SimpleJComboBox __EnsembleModelName_JComboBox = null;
private SimpleJComboBox __EnsembleModelRunDate_JComboBox = null;
private JLabel __selectedEnsembleID_JLabel = null;
private JLabel __selectedEnsembleModelID_JLabel = null;
private JLabel __selectedEnsembleModelRunID_JLabel = null;
private JTextField __InputStart_JTextField;
private JTextField __InputEnd_JTextField;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
			
private JTextArea __command_JTextArea = null; // Command as JTextArea
private InputFilter_JPanel __inputFilter_JPanel =null;
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK was pressed when closing the dialog.

private boolean __ignoreEvents = false; // Used to ignore cascading events when initializing the components

private ReclamationHDBDataStore __dataStore = null; // selected ReclamationHDBDataStore
private ReclamationHDB_DMI __dmi = null; // ReclamationHDB_DMI to do queries.

private List<ReclamationHDB_Ensemble> __ensembleList = new Vector<ReclamationHDB_Ensemble>(); // Corresponds to displayed list (has ensemble_id)
private List<ReclamationHDB_SiteDataType> __siteDataTypeList = new Vector<ReclamationHDB_SiteDataType>(); // Corresponds to displayed list
private List<ReclamationHDB_Model> __modelList = new Vector<ReclamationHDB_Model>(); // Corresponds to displayed list (has model_id)
private List<ReclamationHDB_ModelRun> __modelRunList = new Vector<ReclamationHDB_ModelRun>(); // Corresponds to models matching model_id

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadReclamationHDB_JDialog ( JFrame parent, ReadReclamationHDB_Command command )
{	super(parent, true);

	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param e ActionEvent object
*/
public void actionPerformed( ActionEvent e )
{   if ( __ignoreEvents ) {
        return; // Startup.
    }
    Object o = e.getSource();
    if ( o == __cancel_JButton ) {
        response ( false );
    }
    else if ( o == __ok_JButton ) {
        refresh ();
        checkInput();
        if ( !__error_wait ) {
            response ( true );
        }
    }
}

/**
Refresh the site common name choices in response to the currently selected ReclamationHDB datastore.
*/
private void actionPerformedDataStoreSelected ( )
{
    if ( __DataStore_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    setDMIForSelectedDataStore();
    //Message.printStatus(2, "", "Selected datastore " + __dataStore + " __dmi=" + __dmi );
    // Now populate the data type choices corresponding to the datastore
    populateSiteCommonNameChoices ( __dmi );
    populateSiteDataTypeIDChoices ( __dmi );
    populateModelNameChoices ( __dmi );
    populateModelRunIDChoices ( __dmi );
    populateEnsembleNameChoices ( __dmi );
    populateEnsembleModelNameChoices ( __dmi );
}

/**
Refresh the query choices for the currently selected ReclamationHDB data type common name.
*/
private void actionPerformedDataTypeCommonNameSelected ( )
{
    if ( __DataTypeCommonName_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // No further action needed to populate choices but show selected site_datatype_id for those who
    // are familiar with the database internals
    updateSiteIDTextFields();
}

/**
Refresh the query choices for the currently selected ReclamationHDB ensemble name.
*/
private void actionPerformedEnsembleNameSelected ( )
{
    if ( __EnsembleName_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // No further action needed to populate choices but show selected ensemble_id those who
    // are familiar with the database internals
    updateEnsembleIDTextFields ();
    // Now populate the model run choices corresponding to the ensemble name, which will cascade to
    // populating the other choices
    populateEnsembleModelNameChoices ( __dmi );
}

/**
Refresh the query choices for the currently selected ReclamationHDB model name.
*/
private void actionPerformedEnsembleModelNameSelected ( )
{
    if ( __EnsembleModelName_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // No further action needed to populate choices but show selected model_id those who
    // are familiar with the database internals
    updateEnsembleIDTextFields ();
    // Now populate the model run choices corresponding to the model name, which will cascade to
    // populating the other choices
    // This is not a selectable item with ensembles - just key off of model run name
    //populateModelRunNameChoices ( __dmi );
}

/**
Refresh the query choices for the currently selected ReclamationHDB hydrologic indicator name.
*/
private void actionPerformedHydrologicIndicatorSelected ( )
{
    if ( __HydrologicIndicator_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // No further action needed to populate choices but show selected model_run_id for those who
    // are familiar with the database internals
    updateModelIDTextFields ();
}

/**
Refresh the query choices for the currently selected ReclamationHDB model name.
*/
private void actionPerformedModelNameSelected ( )
{
    if ( __ModelName_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // No further action needed to populate choices but show selected model_id those who
    // are familiar with the database internals
    updateModelIDTextFields ();
    // Now populate the model run choices corresponding to the model name, which will cascade to
    // populating the other choices
    populateModelRunNameChoices ( __dmi );
}

/**
Refresh the query choices for the currently selected ReclamationHDB model run date.
*/
private void actionPerformedModelRunDateSelected ( )
{
    if ( __ModelRunDate_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // No further action needed to populate choices but show selected model_run_id for those who
    // are familiar with the database internals
    updateModelIDTextFields ();
    // Now populate the hydrologic indicator choices corresponding to the model run date
    populateHydrologicIndicatorChoices ( __dmi );
}

/**
Refresh the query choices for the currently selected ReclamationHDB model run name.
*/
private void actionPerformedModelRunNameSelected ( )
{
    if ( __ModelRunName_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // No further action needed to populate choices but show selected model_run_id for those who
    // are familiar with the database internals
    updateModelIDTextFields ();
    // Now populate the model run choices corresponding to the model run name
    populateModelRunDateChoices ( __dmi );
}

/**
Refresh the query choices for the currently selected ReclamationHDB site common name.
*/
private void actionPerformedSiteCommonNameSelected ( )
{
    if ( __SiteCommonName_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // Now populate the data type choices corresponding to the site common name
    populateDataTypeCommonNameChoices ( __dmi );
    updateSiteIDTextFields();
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
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{   if ( __ignoreEvents ) {
        // Startup
        return;
    }
    // Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	__error_wait = false;
	String DataStore = __DataStore_JComboBox.getSelected();
	if ( DataStore.length() > 0 ) {
		props.set ( "DataStore", DataStore );
	}
    String DataType = __DataType_JComboBox.getSelected();
    if ( DataType.length() > 0 ) {
        props.set ( "DataType", DataType );
    }
    String Interval = __Interval_JComboBox.getSelected();
    if ( Interval.length() > 0 ) {
        props.set ( "Interval", Interval );
    }
	int numWhere = __inputFilter_JPanel.getNumFilterGroups();
	for ( int i = 1; i <= numWhere; i++ ) {
	    String where = getWhere ( i - 1 );
	    if ( where.length() > 0 ) {
	        props.set ( "Where" + i, where );
	    }
    }
    String SiteCommonName = __SiteCommonName_JComboBox.getSelected();
    if ( (SiteCommonName != null) && (SiteCommonName.length() > 0) ) {
        props.set ( "SiteCommonName", SiteCommonName );
    }
    String DataTypeCommonName = __DataTypeCommonName_JComboBox.getSelected();
    if ( (DataTypeCommonName != null) && (DataTypeCommonName.length() > 0) ) {
        props.set ( "DataTypeCommonName", DataTypeCommonName );
    }
    String SiteDataTypeID = __SiteDataTypeID_JComboBox.getSelected();
    if ( (SiteDataTypeID != null) && (SiteDataTypeID.length() > 0) ) {
        props.set ( "SiteDataTypeID", SiteDataTypeID );
    }
    String ModelName = __ModelName_JComboBox.getSelected();
    if ( (ModelName != null) && (ModelName.length() > 0) ) {
        props.set ( "ModelName", ModelName );
    }
    String ModelRunName = __ModelRunName_JComboBox.getSelected();
    if ( (ModelRunName != null) && (ModelRunName.length() > 0) ) {
        props.set ( "ModelRunName", ModelRunName );
    }
    String ModelRunDate = __ModelRunDate_JComboBox.getSelected();
    if ( (ModelRunDate != null) && (ModelRunDate.length() > 0) ) {
        props.set ( "ModelRunDate", ModelRunDate );
    }
    String HydrologicIndicator = __HydrologicIndicator_JComboBox.getSelected();
    if ( HydrologicIndicator.length() > 0 ) {
        props.set ( "HydrologicIndicator", HydrologicIndicator );
    }
    String ModelRunID = __ModelRunID_JComboBox.getSelected();
    if ( (ModelRunID != null) && (ModelRunID.length() > 0) ) {
        props.set ( "ModelRunID", ModelRunID );
    }
    String EnsembleName = __EnsembleName_JComboBox.getSelected();
    if ( (EnsembleName != null) && (EnsembleName.length() > 0) ) {
        props.set ( "EnsembleName", EnsembleName );
    }
    //String EnsembleTraceID = __EnsembleTraceID_JTextField.getText().trim();
    //if ( EnsembleTraceID.length() > 0 ) {
    //    props.set ( "EnsembleTraceID", EnsembleTraceID );
    //}
    String EnsembleModelName = __EnsembleModelName_JComboBox.getSelected();
    if ( (EnsembleModelName != null) && (EnsembleModelName.length() > 0) ) {
        props.set ( "EnsembleModelName", EnsembleModelName );
    }
    String EnsembleModelRunDate = __EnsembleModelRunDate_JComboBox.getSelected();
    if ( (EnsembleModelRunDate != null) && (EnsembleModelRunDate.length() > 0) ) {
        props.set ( "EnsembleModelRunDate", EnsembleModelRunDate );
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
    String DataType = __DataType_JComboBox.getSelected();
    String Interval = __Interval_JComboBox.getSelected();
	__command.setCommandParameter ( "DataStore", DataStore );
	__command.setCommandParameter ( "DataType", DataType );
	__command.setCommandParameter ( "Interval", Interval );
	String delim = ";";
	int numWhere = __inputFilter_JPanel.getNumFilterGroups();
	for ( int i = 1; i <= numWhere; i++ ) {
	    String where = getWhere ( i - 1 );
	    if ( where.startsWith(delim) ) {
	        where = "";
	    }
	    __command.setCommandParameter ( "Where" + i, where );
	}
    String SiteCommonName = __SiteCommonName_JComboBox.getSelected();
    __command.setCommandParameter ( "SiteCommonName", SiteCommonName );
    String DataTypeCommonName = __DataTypeCommonName_JComboBox.getSelected();
    __command.setCommandParameter ( "DataTypeCommonName", DataTypeCommonName );
    String SiteDataTypeID = __SiteDataTypeID_JComboBox.getSelected();
    __command.setCommandParameter ( "SiteDataTypeID", SiteDataTypeID );
    String ModelName = __ModelName_JComboBox.getSelected();
    __command.setCommandParameter ( "ModelName", ModelName );
    String ModelRunName = __ModelRunName_JComboBox.getSelected();
    __command.setCommandParameter ( "ModelRunName", ModelRunName );
    String ModelRunDate = __ModelRunDate_JComboBox.getSelected();
    __command.setCommandParameter ( "ModelRunDate", ModelRunDate );
    String HydrologicIndicator = __HydrologicIndicator_JComboBox.getSelected();
    __command.setCommandParameter ( "HydrologicIndicator", HydrologicIndicator );
    String ModelRunID = __ModelRunID_JComboBox.getSelected();
    __command.setCommandParameter ( "ModelRunID", ModelRunID );
    String EnsembleName = __EnsembleName_JComboBox.getSelected();
    __command.setCommandParameter ( "EnsembleName", EnsembleName );
    //String EnsembleTraceID = __EnsembleTraceID_JTextField.getText().trim();
    //__command.setCommandParameter ( "EnsembleTraceID", EnsembleTraceID );
    String EnsembleModelName = __EnsembleModelName_JComboBox.getSelected();
    __command.setCommandParameter ( "EnsembleModelName", EnsembleModelName );
    String EnsembleModelRunDate = __EnsembleModelRunDate_JComboBox.getSelected();
    __command.setCommandParameter ( "EnsembleModelRunDate", EnsembleModelRunDate );
	String InputStart = __InputStart_JTextField.getText().trim();
	__command.setCommandParameter ( "InputStart", InputStart );
	String InputEnd = __InputEnd_JTextField.getText().trim();
	__command.setCommandParameter ( "InputEnd", InputEnd );
	String Alias = __Alias_JTextField.getText().trim();
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
Return the ReclamationHDB_DMI that is currently being used for database interaction,
based on the selected datastore.
*/
private ReclamationHDB_DMI getReclamationHDB_DMI ()
{
    return __dmi;
}

/**
Return the selected datastore, used to provide intelligent parameter choices.
@return the selected datastore, or null if nothing selected (or none available)
*/
private ReclamationHDBDataStore getSelectedDataStore ()
{   
    List<DataStore> dataStoreList =
        ((TSCommandProcessor)__command.getCommandProcessor()).getDataStoresByType(
            ReclamationHDBDataStore.class );
    String dataStoreNameSelected = __DataStore_JComboBox.getSelected();
    if ( (dataStoreNameSelected != null) && !dataStoreNameSelected.equals("") ) {
        for ( DataStore dataStore : dataStoreList ) {
            if ( dataStore.getName().equalsIgnoreCase(dataStoreNameSelected) ) {
                return (ReclamationHDBDataStore)dataStore;
            }
        }
    }
    return null;
}

/**
Return the "WhereN" parameter for the requested input filter.
@return the "WhereN" parameter for the requested input filter.
@param ifg the Input filter to process (zero index).
*/
private String getWhere ( int ifg )
{
	// TODO SAM 2006-04-24 Need to enable other input filter panels
	String delim = ";";	// To separate input filter parts
	InputFilter_JPanel filter_panel = __inputFilter_JPanel;
	String where = filter_panel.toString(ifg,delim).trim();
	return where;
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, ReadReclamationHDB_Command command )
{	String routine = "ReadReclamationHDB_JDialog.initialize";
	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int yMain = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Read one or more time series, or an ensemble, from a Reclamation HDB database."),
    	0, yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Constrain the query by specifying time series metadata to match." ), 
    	0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Specify date/times using the format YYYY-MM-DD hh:mm:ss, to a precision appropriate for the data " +
        "interval (default=input period from SetInputPeriod())."),
        0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    __ignoreEvents = true; // So that a full pass of initialization can occur
   	
   	// List available datastores of the correct type
   	
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Datastore:"),
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStore_JComboBox = new SimpleJComboBox ( false );
    List<DataStore> dataStoreList = ((TSCommandProcessor)processor).getDataStoresByType(
        ReclamationHDBDataStore.class );
    for ( DataStore dataStore: dataStoreList ) {
        __DataStore_JComboBox.addItem ( dataStore.getName() );
    }
    if ( __DataStore_JComboBox.getItemCount() > 0 ) {
        __DataStore_JComboBox.select ( 0 );
    }
    __DataStore_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataStore_JComboBox,
        1, yMain, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - datastore containing data."), 
        3, yMain, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Intervals are hard-coded
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data interval:"),
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Interval_JComboBox = new SimpleJComboBox ( false );
    __Interval_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Interval_JComboBox,
        1, yMain, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - data interval (time step) for time series."), 
        3, yMain, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Top-level tabbed panel to separate filter input and specific choices
    __main_JTabbedPane = new JTabbedPane ();
    __main_JTabbedPane.setBorder(
        BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
        "Specify how to match HDB time series or ensemble" ));
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++yMain, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel to query time series using filter
    int yFilter = -1;
    JPanel filter_JPanel = new JPanel();
    filter_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Read 1+ time series using filter", filter_JPanel );
    
    JGUIUtil.addComponent(filter_JPanel, new JLabel (
        "Use these parameters when reading 1+ time series from HDB."), 
        0, ++yFilter, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Data types are particular to the datastore...
    // This is somewhat redundant with the CommonDataTypeName but need to keep bulk read separate
    // from single time series/ensemble read
    
    JGUIUtil.addComponent(filter_JPanel, new JLabel ( "Data type:"),
        0, ++yFilter, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataType_JComboBox = new SimpleJComboBox ( false );
    __DataType_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(filter_JPanel, __DataType_JComboBox,
        1, yFilter, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(filter_JPanel, new JLabel("Required - data type for time series."), 
        3, yFilter, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	
   	// Input filters
    // TODO SAM 2010-11-02 Need to use SetInputFilters() so the filters can change when a
    // datastore is selected.  For now it is OK because the input filters do not provide choices.

	int buffer = 3;
	Insets insets = new Insets(0,buffer,0,0);
	try {
	    // Add input filters for ReclamationHDB time series...
		__inputFilter_JPanel = new ReclamationHDB_TimeSeries_InputFilter_JPanel(
		    getSelectedDataStore(), __command.getNumFilterGroups() );
		JGUIUtil.addComponent(filter_JPanel, __inputFilter_JPanel,
			0, ++yFilter, 2, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST );
   		__inputFilter_JPanel.addEventListeners ( this );
   	    JGUIUtil.addComponent(filter_JPanel, new JLabel ( "Optional - query filters."),
   	        3, yFilter, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	}
	catch ( Exception e ) {
		Message.printWarning ( 2, routine, "Unable to initialize ReclamationHDB input filter." );
		Message.printWarning ( 2, routine, e );
	}

	int yInner = -1;
    JPanel inner_JPanel = new JPanel();
    inner_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Read single time series or ensemble", inner_JPanel );
    
    // Panel to control site_datatype_id selection - used for single time series and ensemble
    int ySiteDataType = -1;
    JPanel siteDataType_JPanel = new JPanel();
    siteDataType_JPanel.setLayout( new GridBagLayout() );
    siteDataType_JPanel.setBorder( BorderFactory.createTitledBorder (
        BorderFactory.createLineBorder(Color.black),
        "Specify how to match the HDB site_datatype_id (required for all time series and ensembles)" ));
    JGUIUtil.addComponent( inner_JPanel, siteDataType_JPanel,
        0, ++yInner, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(siteDataType_JPanel, new JLabel ("Site common name:"), 
        0, ++ySiteDataType, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SiteCommonName_JComboBox = new SimpleJComboBox (false);
    __SiteCommonName_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(siteDataType_JPanel, __SiteCommonName_JComboBox,
        1, ySiteDataType, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(siteDataType_JPanel, new JLabel (
        "Required - used with data type common name to determine site_datatype_id."),
        3, ySiteDataType, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(siteDataType_JPanel, new JLabel ("Data type common name:"), 
        0, ++ySiteDataType, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataTypeCommonName_JComboBox = new SimpleJComboBox (false);
    __DataTypeCommonName_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(siteDataType_JPanel, __DataTypeCommonName_JComboBox,
        1, ySiteDataType, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(siteDataType_JPanel, new JLabel (
        "Required - used with site common name to determine site_datatype_id."),
        3, ySiteDataType, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(siteDataType_JPanel, new JLabel ("Matching site_id:"), 
        0, ++ySiteDataType, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __selectedSiteID_JLabel = new JLabel ( "");
    JGUIUtil.addComponent(siteDataType_JPanel, __selectedSiteID_JLabel,
        1, ySiteDataType, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(siteDataType_JPanel, new JLabel (
        "Information - useful when comparing to database contents."),
        3, ySiteDataType, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(siteDataType_JPanel, new JLabel ("Matching site_datatype_id:"), 
        0, ++ySiteDataType, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __selectedSiteDataTypeID_JLabel = new JLabel ( "");
    JGUIUtil.addComponent(siteDataType_JPanel, __selectedSiteDataTypeID_JLabel,
        1, ySiteDataType, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(siteDataType_JPanel, new JLabel (
        "Information - useful when comparing to database contents."),
        3, ySiteDataType, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(siteDataType_JPanel, new JLabel ("Site data type ID:"), 
        0, ++ySiteDataType, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SiteDataTypeID_JComboBox = new SimpleJComboBox (false);
    __SiteDataTypeID_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(siteDataType_JPanel, __SiteDataTypeID_JComboBox,
        1, ySiteDataType, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(siteDataType_JPanel, new JLabel (
        "Optional - alternative to selecting above choices."),
        3, ySiteDataType, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    // Tabbed pane for model and ensemble data
    __inner_JTabbedPane = new JTabbedPane ();
    __inner_JTabbedPane.setBorder(
        BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
        "Specify how to match HDB model_run_id for single model time series or ensemble of model time series" ));
    JGUIUtil.addComponent(inner_JPanel, __inner_JTabbedPane,
        0, ++yInner, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
    // Panel to query single time series
    int yModel = -1;
    JPanel model_JPanel = new JPanel();
    model_JPanel.setLayout( new GridBagLayout() );
    __inner_JTabbedPane.addTab ( "Single model time series", model_JPanel );
    
    JGUIUtil.addComponent(model_JPanel, new JLabel (
        "Use these parameters to read a single model time series from HDB."), 
        0, ++yModel, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(model_JPanel, new JLabel ("Model name:"), 
        0, ++yModel, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ModelName_JComboBox = new SimpleJComboBox (false);
    __ModelName_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(model_JPanel, __ModelName_JComboBox,
        1, yModel, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(model_JPanel, new JLabel (
        "Required - used to determine the model_run_id."),
        3, yModel, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(model_JPanel, new JLabel ("Model run name:"), 
        0, ++yModel, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ModelRunName_JComboBox = new SimpleJComboBox (false);
    __ModelRunName_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(model_JPanel, __ModelRunName_JComboBox,
        1, yModel, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(model_JPanel, new JLabel (
        "Required - used to determine the model_run_id."),
        3, yModel, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(model_JPanel, new JLabel ("Model run date:"), 
        0, ++yModel, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ModelRunDate_JComboBox = new SimpleJComboBox (false);
    __ModelRunDate_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(model_JPanel, __ModelRunDate_JComboBox,
        1, yModel, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(model_JPanel, new JLabel (
        "Required - YYYY-MM-DD hh:mm, used to determine the model_run_id."),
        3, yModel, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(model_JPanel, new JLabel ("Hydrologic indicator:"), 
        0, ++yModel, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __HydrologicIndicator_JComboBox = new SimpleJComboBox (false);
    __HydrologicIndicator_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(model_JPanel, __HydrologicIndicator_JComboBox,
        1, yModel, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(model_JPanel, new JLabel (
        "Required - used to determine the model_run_id."),
        3, yModel, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(model_JPanel, new JLabel ("Selected model_id:"), 
        0, ++yModel, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __selectedModelID_JLabel = new JLabel ( "");
    JGUIUtil.addComponent(model_JPanel, __selectedModelID_JLabel,
        1, yModel, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(model_JPanel, new JLabel (
        "Information - useful when comparing to database contents."),
        3, yModel, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(model_JPanel, new JLabel ("Selected model_run_id:"), 
        0, ++yModel, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __selectedModelRunID_JLabel = new JLabel ( "");
    JGUIUtil.addComponent(model_JPanel, __selectedModelRunID_JLabel,
        1, yModel, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(model_JPanel, new JLabel (
        "Information - useful when comparing to database contents."),
        3, yModel, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(model_JPanel, new JLabel ("Model run ID:"), 
        0, ++yModel, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ModelRunID_JComboBox = new SimpleJComboBox (false);
    __ModelRunID_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(model_JPanel, __ModelRunID_JComboBox,
        1, yModel, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(model_JPanel, new JLabel (
        "Optional - alternative to selecting above choices."),
        3, yModel, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
    // Panel to query ensemble time series
    int yEnsemble = -1;
    JPanel ensemble_JPanel = new JPanel();
    ensemble_JPanel.setLayout( new GridBagLayout() );
    __inner_JTabbedPane.addTab ( "Ensemble of model time series", ensemble_JPanel );
    
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
        "Use these parameters to read an ensemble of model time series from HDB.  " +
        "If the run date is specified, the ensemble time series will be uniquely identified with the " +
        "run date (to the minute)."), 
        0, ++yEnsemble, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel ("Ensemble name:"), 
        0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EnsembleName_JComboBox = new SimpleJComboBox (false);
    __EnsembleName_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(ensemble_JPanel, __EnsembleName_JComboBox,
        1, yEnsemble, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
        "Required - used to determine the ensemble model_run_id."),
        3, yEnsemble, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel ("Ensemble model name:"), 
        0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EnsembleModelName_JComboBox = new SimpleJComboBox (false);
    // Set the size to handle example data - otherwise may have layout issues.
    __EnsembleModelName_JComboBox.setPrototypeDisplayValue("MMMMMMMMMMMMMMMMMMMMMMMMM");
    __EnsembleModelName_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(ensemble_JPanel, __EnsembleModelName_JComboBox,
        1, yEnsemble, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
        "Required - used to determine the ensemble model_run_id."),
        3, yEnsemble, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    /* TODO SAM 2013-04-02 In future allow option of 1-3 HDB trace identifier parts
     * For now default to integer trace number, compatible with TSTool design
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel("Ensemble trace ID:"),
        0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EnsembleTraceID_JTextField = new TSFormatSpecifiersJPanel(10);
    __EnsembleTraceID_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
    __EnsembleTraceID_JTextField.addKeyListener ( this );
    __EnsembleTraceID_JTextField.getDocument().addDocumentListener(this);
    __EnsembleTraceID_JTextField.setToolTipText("%L for location, %T for data type, ${TS:property} to use property.");
    JGUIUtil.addComponent(ensemble_JPanel, __EnsembleTraceID_JTextField,
        1, yEnsemble, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
        "Optional - use %L for location, etc. or ${TS:property} (default=no alias)."),
        3, yEnsemble, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        */
    
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel ("Ensemble model run date:"), 
        0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EnsembleModelRunDate_JComboBox = new SimpleJComboBox (false);
    __EnsembleModelRunDate_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(ensemble_JPanel, __EnsembleModelRunDate_JComboBox,
        1, yEnsemble, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
        "Optional - YYYY-MM-DD hh:mm, used to determine the ensemble model_run_id (default=run date not used)."),
        3, yEnsemble, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel ("Selected ensemble_id:"), 
        0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __selectedEnsembleID_JLabel = new JLabel ( "");
    JGUIUtil.addComponent(ensemble_JPanel, __selectedEnsembleID_JLabel,
        1, yEnsemble, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
        "Information - useful when comparing to database contents."),
        3, yEnsemble, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel ("Selected ensemble model_id:"), 
        0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __selectedEnsembleModelID_JLabel = new JLabel ( "");
    JGUIUtil.addComponent(ensemble_JPanel, __selectedEnsembleModelID_JLabel,
        1, yEnsemble, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
        "Information - useful when comparing to database contents."),
        3, yEnsemble, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel ("Selected ensemble model_run_id:"), 
        0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __selectedEnsembleModelRunID_JLabel = new JLabel ( "Determined for trace when command is run");
    JGUIUtil.addComponent(ensemble_JPanel, __selectedEnsembleModelRunID_JLabel,
        1, yEnsemble, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    //JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
    //    ""),
    //    3, yEnsemble, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    // General parameters...
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Input start:"), 
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputStart_JTextField = new JTextField (20);
    __InputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputStart_JTextField,
        1, yMain, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - override the global input start."),
        3, yMain, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input end:"), 
        0, ++yMain, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputEnd_JTextField = new JTextField (20);
    __InputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputEnd_JTextField,
        1, yMain, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - override the global input end."),
        3, yMain, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
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
        3, yMain, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
		0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (4,50);
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, yMain, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	
    // All of the components have been initialized above but now generate an event to populate...
    if ( __DataStore_JComboBox.getItemCount() > 0 ) {
        __DataStore_JComboBox.select(null);
        __DataStore_JComboBox.select(0);
    }

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

	// Refresh the contents...
    checkGUIState();
    refresh ();
    __ignoreEvents = false; // After initialization of components let events happen to dynamically cause cascade
    checkGUIState(); // Do this again because it may not have happened due to the special event handling
    updateSiteIDTextFields();
    updateModelIDTextFields();
    // Dialogs do not need to be resizable...
	setResizable ( true );
    pack();
    JGUIUtil.center( this );
    super.setVisible( true );
}

/**
Handle ItemListener events.
@param e item event
*/
public void itemStateChanged ( ItemEvent e )
{
    if ( __ignoreEvents ) {
        return; // Startup
    }

    checkGUIState();
    Object source = e.getSource();
    int sc = e.getStateChange();
    if ( (source == __DataStore_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected a datastore.
        actionPerformedDataStoreSelected ();
    }
    else if ( (source == __SiteCommonName_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected a site common name.
        actionPerformedSiteCommonNameSelected ();
    }
    else if ( (source == __DataTypeCommonName_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected a data type common name.
        actionPerformedDataTypeCommonNameSelected ();
    }
    else if ( (source == __ModelName_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected a model name.
        actionPerformedModelNameSelected ();
    }
    else if ( (source == __ModelRunName_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected a model run name.
        actionPerformedModelRunNameSelected ();
    }
    else if ( (source == __ModelRunDate_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected a model run date.
        actionPerformedModelRunDateSelected ();
    }
    else if ( (source == __HydrologicIndicator_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected a model name.
        actionPerformedHydrologicIndicatorSelected ();
    }
    else if ( (source == __EnsembleName_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected an ensemble name.
        actionPerformedEnsembleNameSelected ();
    }
    else if ( (source == __EnsembleModelName_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected an ensemble model name.
        actionPerformedEnsembleModelNameSelected ();
    }
 
    refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{
    if ( __ignoreEvents ) {
        return; // Startup.
    }
    int code = event.getKeyCode();
    if ( code == KeyEvent.VK_ENTER ) {
        refresh ();
        checkInput();
        if ( !__error_wait ) {
            response ( true );
        }
    }
}

/**
Need this to properly capture key events, especially deletes.
*/
public void keyReleased ( KeyEvent event )
{
    if ( __ignoreEvents ) {
        return; // Startup.
    }
    refresh();
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
Set the data type choices in response to a new datastore being selected.
The data types are used with the filters because DataTypeCommonName is used with the specific
data queries.
*/
private void populateDataTypeChoices ()
{   String routine = getClass().getName() + ".populateDataTypeChoices";
    ReclamationHDBDataStore hdbDataStore = getSelectedDataStore();
    ReclamationHDB_DMI dmi = (ReclamationHDB_DMI)hdbDataStore.getDMI();
    List<String> dataTypes = new Vector<String>();
    try {
        dataTypes = dmi.getObjectDataTypes ( true );
    }
    catch ( Exception e ) {
        // Hopefully should not happen
        Message.printWarning(2, routine, "Unable to get object types and associated data types for datastore \"" +
            __DataStore_JComboBox.getSelected() + "\" - no database connection?");
    }
    // Add a blank option if the filters are not being used
    dataTypes.add(0,"");
    // Add a wildcard option to get all data types
    dataTypes.add(1,"*");
    __DataType_JComboBox.setData ( dataTypes );
    __DataType_JComboBox.select ( 0 );
}

/**
Populate the data type choice list based on the selected site common name.
*/
private void populateDataTypeCommonNameChoices ( ReclamationHDB_DMI rdmi )
{   //String routine = getClass().getName() + ".populateDataTypeCommonNameChoices";
    if ( (rdmi == null) || (__DataTypeCommonName_JComboBox == null) ) {
        // Initialization
        return;
    }
    // Populate the data types from datatype that match the site_id via site_datatype_id
    // First find the site_id for the selected site
    String selectedSiteCommonName = __SiteCommonName_JComboBox.getSelected();
    List<String> dataTypeCommonNameStrings = new Vector<String>();
    dataTypeCommonNameStrings.add("");
    if ( selectedSiteCommonName != null ) {
        List<ReclamationHDB_SiteDataType> siteDataTypeList =
            rdmi.findSiteDataType(__siteDataTypeList, selectedSiteCommonName, null );
        for ( ReclamationHDB_SiteDataType siteDataType: siteDataTypeList ) {
            dataTypeCommonNameStrings.add ( siteDataType.getDataTypeCommonName() );
        }
        Collections.sort(dataTypeCommonNameStrings,String.CASE_INSENSITIVE_ORDER);
    }
    __DataTypeCommonName_JComboBox.removeAll ();
    __DataTypeCommonName_JComboBox.setData(dataTypeCommonNameStrings);
    // Select first choice (may get reset from existing parameter values).
    __DataTypeCommonName_JComboBox.select ( null );
    if ( __DataTypeCommonName_JComboBox.getItemCount() > 0 ) {
        __DataTypeCommonName_JComboBox.select ( 0 );
    }
}

/**
Populate the ensemble model name list based on the selected datastore.
The model names are the same as the non-ensemble list so reuse what was read from HDB.
*/
private void populateEnsembleModelNameChoices ( ReclamationHDB_DMI rdmi )
{   if ( (rdmi == null) || (__EnsembleModelName_JComboBox == null) ) {
        // Initialization
        return;
    }
    List<String> modelNameStrings = new Vector();
    modelNameStrings.add ( "" ); // Always add blank because user may not want model time series
    for ( ReclamationHDB_Model model: __modelList ) {
        modelNameStrings.add ( model.getModelName() );
    }
    Collections.sort(modelNameStrings,String.CASE_INSENSITIVE_ORDER);
    StringUtil.removeDuplicates(modelNameStrings, true, true);
    __EnsembleModelName_JComboBox.removeAll ();
    __EnsembleModelName_JComboBox.setData(modelNameStrings);
    // Select first choice (may get reset from existing parameter values).
    __EnsembleModelName_JComboBox.select ( null );
    if ( __EnsembleModelName_JComboBox.getItemCount() > 0 ) {
        __EnsembleModelName_JComboBox.select ( 0 );
    }
}

/**
Populate the model name list based on the selected datastore.
*/
private void populateEnsembleNameChoices ( ReclamationHDB_DMI rdmi )
{   String routine = getClass().getName() + ".populateEnsembleNameChoices";
    if ( (rdmi == null) || (__EnsembleName_JComboBox == null) ) {
        // Initialization
        return;
    }
    List<String> ensembleNameStrings = new Vector();
    ensembleNameStrings.add ( "" ); // Always add blank because user may not want ensemble time series
    ensembleNameStrings.add ( "Test" ); // Add so something is in the list, FIXME SAM 2013-03-23 remove when code tested
    try {
        readEnsembleList(rdmi);
        for ( ReclamationHDB_Ensemble ensemble: __ensembleList ) {
            ensembleNameStrings.add ( ensemble.getEnsembleName() );
        }
        Collections.sort(ensembleNameStrings,String.CASE_INSENSITIVE_ORDER);
        StringUtil.removeDuplicates(ensembleNameStrings, true, true);
    }
    catch ( Exception e ) {
        Message.printWarning(3, routine, "Error getting HDB ensemble list (" + e + ")." );
        ensembleNameStrings = new Vector();
    }
    __EnsembleName_JComboBox.removeAll ();
    __EnsembleName_JComboBox.setData(ensembleNameStrings);
    // Select first choice (may get reset from existing parameter values).
    __EnsembleName_JComboBox.select ( null );
    if ( __EnsembleName_JComboBox.getItemCount() > 0 ) {
        __EnsembleName_JComboBox.select ( 0 );
    }
}

/**
Populate the model hydrologic indicator list based on the selected datastore.
*/
private void populateHydrologicIndicatorChoices ( ReclamationHDB_DMI rdmi )
{   String routine = getClass().getName() + ".populateHydrologicIndicatorChoices";
    if ( (rdmi == null) || (__HydrologicIndicator_JComboBox == null) ) {
        // Initialization
        return;
    }
    String selectedModelName = __ModelName_JComboBox.getSelected();
    String selectedModelRunName = __ModelRunName_JComboBox.getSelected();
    String selectedModelRunDate = __ModelRunDate_JComboBox.getSelected();
    List<ReclamationHDB_Model> modelList = rdmi.findModel(__modelList, selectedModelName);
    List<String> hydrologicIndicatorStrings = new Vector();
    hydrologicIndicatorStrings.add ( "" ); // Always add blank because user may not want model time series
    if ( modelList.size() == 1 ) {
        ReclamationHDB_Model model = modelList.get(0);
        int modelID = model.getModelID();
        List<ReclamationHDB_ModelRun> modelRunList = rdmi.findModelRun(__modelRunList, modelID,
                selectedModelRunName,
                selectedModelRunDate,
                null); // Don't match on hydrologic indicator
        // Results should list unique hydrologic indicators
        for ( ReclamationHDB_ModelRun modelRun: modelRunList ) {
            hydrologicIndicatorStrings.add ( modelRun.getHydrologicIndicator() );
        }
        Collections.sort(hydrologicIndicatorStrings,String.CASE_INSENSITIVE_ORDER);
        StringUtil.removeDuplicates(hydrologicIndicatorStrings, true, true);
    }
    else {
        Message.printStatus ( 2, routine, "Have " + modelList.size() + " models matching name \"" +
            selectedModelName + "\" - unable to find matching model runs." );
    }
    __HydrologicIndicator_JComboBox.removeAll ();
    __HydrologicIndicator_JComboBox.setData(hydrologicIndicatorStrings);
    // Select first choice (may get reset from existing parameter values).
    __HydrologicIndicator_JComboBox.select ( null );
    if ( __HydrologicIndicator_JComboBox.getItemCount() > 0 ) {
        __HydrologicIndicator_JComboBox.select ( 0 );
    }
}

/**
TODO SAM 2013-04-06 Need to enable this to refresh based on datastore selection.
Set the input filters in response to a new datastore being selected.
*/
private void populateInputFilters ()
{

}

/**
Set the data interval choices in response to a new datastore being selected.
*/
private void populateIntervalChoices ()
{
    __Interval_JComboBox.removeAll();
    __Interval_JComboBox.add ( "Hour" );
    __Interval_JComboBox.add ( "2Hour" );
    __Interval_JComboBox.add ( "3Hour" );
    __Interval_JComboBox.add ( "4Hour" );
    __Interval_JComboBox.add ( "6Hour" );
    __Interval_JComboBox.add ( "12Hour" );
    __Interval_JComboBox.add ( "Day" );
    __Interval_JComboBox.add ( "Month" );
    __Interval_JComboBox.add ( "Year" );
    // FIXME SAM 2010-10-26 Could handle WY as YEAR, but need to think about it
    __Interval_JComboBox.add ( "Irregular" );
    __Interval_JComboBox.select ( 0 );
}

/**
Populate the model name list based on the selected datastore.
*/
private void populateModelNameChoices ( ReclamationHDB_DMI rdmi )
{   String routine = getClass().getName() + ".populateModelNameChoices";
    if ( (rdmi == null) || (__ModelName_JComboBox == null) ) {
        // Initialization
        return;
    }
    List<String> modelNameStrings = new Vector();
    modelNameStrings.add ( "" ); // Always add blank because user may not want model time series
    try {
        readModelList(rdmi);
        for ( ReclamationHDB_Model model: __modelList ) {
            modelNameStrings.add ( model.getModelName() );
        }
        Collections.sort(modelNameStrings,String.CASE_INSENSITIVE_ORDER);
        StringUtil.removeDuplicates(modelNameStrings, true, true);
    }
    catch ( Exception e ) {
        Message.printWarning(3, routine, "Error getting HDB model list (" + e + ")." );
        modelNameStrings = new Vector();
    }
    __ModelName_JComboBox.removeAll ();
    __ModelName_JComboBox.setData(modelNameStrings);
    // Select first choice (may get reset from existing parameter values).
    __ModelName_JComboBox.select ( null );
    if ( __ModelName_JComboBox.getItemCount() > 0 ) {
        __ModelName_JComboBox.select ( 0 );
    }
}

/**
Populate the model run date list based on the selected datastore.
*/
private void populateModelRunDateChoices ( ReclamationHDB_DMI rdmi )
{   String routine = getClass().getName() + ".populateModelRunDateChoices";
    if ( (rdmi == null) || (__ModelRunDate_JComboBox == null) ) {
        // Initialization
        return;
    }
    String selectedModelName = __ModelName_JComboBox.getSelected();
    String selectedModelRunName = __ModelRunName_JComboBox.getSelected();
    List<ReclamationHDB_Model> modelList = rdmi.findModel(__modelList, selectedModelName);
    List<String> runDateStrings = new Vector();
    runDateStrings.add ( "" ); // Always add blank because user may not want model time series
    if ( modelList.size() == 1 ) {
        ReclamationHDB_Model model = modelList.get(0);
        int modelID = model.getModelID();
        List<ReclamationHDB_ModelRun> modelRunList = rdmi.findModelRun(__modelRunList, modelID,
            selectedModelRunName,
            null, // Don't match on run date
            null); // Don't match on hydrologic indicator
        // Results should list unique hydrologic indicators
        // Model run date is formatted to minute
        Date d;
        DateTime dt;
        for ( ReclamationHDB_ModelRun modelRun: modelRunList ) {
            d = modelRun.getRunDate();
            dt = new DateTime(d);
            // Shows seconds and hundredths...
            //runDateStrings.add ( "" + modelRun.getRunDate() );
            runDateStrings.add ( "" + dt.toString(DateTime.FORMAT_YYYY_MM_DD_HH_mm) );
        }
        Collections.sort(runDateStrings,String.CASE_INSENSITIVE_ORDER);
        StringUtil.removeDuplicates(runDateStrings, true, true);
    }
    else {
        Message.printStatus ( 2, routine, "Have " + modelList.size() + " models matching name \"" +
            selectedModelName + "\" - unable to find matching model runs." );
    }
    __ModelRunDate_JComboBox.removeAll ();
    __ModelRunDate_JComboBox.setData(runDateStrings);
    // Select first choice (may get reset from existing parameter values).
    __ModelRunDate_JComboBox.select ( null );
    if ( __ModelRunDate_JComboBox.getItemCount() > 0 ) {
        __ModelRunDate_JComboBox.select ( 0 );
    }
}

/**
Populate the model run ID list based on the selected datastore.
*/
private void populateModelRunIDChoices ( ReclamationHDB_DMI rdmi )
{   String routine = getClass().getName() + ".populateModelRunIDhoices";
    if ( (rdmi == null) || (__ModelRunID_JComboBox == null) ) {
        // Initialization
        return;
    }
    List<String> modelRunIDStrings = new Vector();
    modelRunIDStrings.add ( "" ); // Always add blank because user may not want model time series
    try {
        // There may be no run names for the model id.
        List<ReclamationHDB_ModelRun> modelRunList = rdmi.readHdbModelRunListForModelID(-1);
        for ( ReclamationHDB_ModelRun modelRun: modelRunList ) {
            modelRunIDStrings.add ( "" + modelRun.getModelRunID() );
        }
        Collections.sort(modelRunIDStrings,String.CASE_INSENSITIVE_ORDER);
        StringUtil.removeDuplicates(modelRunIDStrings, true, true);
    }
    catch ( Exception e ) {
        Message.printWarning(3, routine, "Error getting HDB model run list (" + e + ")." );
        modelRunIDStrings = new Vector();
    }
    __ModelRunID_JComboBox.removeAll ();
    __ModelRunID_JComboBox.setData(modelRunIDStrings);
    // Select first choice (may get reset from existing parameter values).
    __ModelRunID_JComboBox.select ( null );
    if ( __ModelRunID_JComboBox.getItemCount() > 0 ) {
        __ModelRunID_JComboBox.select ( 0 );
    }
}

/**
Populate the model run name list based on the selected datastore.
*/
private void populateModelRunNameChoices ( ReclamationHDB_DMI rdmi )
{   //String routine = getClass().getName() + ".populateModelRunNameChoices";
    if ( (rdmi == null) || (__ModelRunName_JComboBox == null) ) {
        // Initialization
        return;
    }
    readModelRunListForSelectedModel(rdmi);
    List<String> modelRunNameStrings = new Vector();
    modelRunNameStrings.add ( "" );
    for ( ReclamationHDB_ModelRun modelRun: __modelRunList ) {
        modelRunNameStrings.add ( modelRun.getModelRunName() );
    }
    Collections.sort(modelRunNameStrings,String.CASE_INSENSITIVE_ORDER);
    StringUtil.removeDuplicates(modelRunNameStrings, true, true);
    __ModelRunName_JComboBox.removeAll ();
    __ModelRunName_JComboBox.setData(modelRunNameStrings);
    // Select first choice (may get reset from existing parameter values).
    __ModelRunName_JComboBox.select ( null );
    if ( __ModelRunName_JComboBox.getItemCount() > 0 ) {
        __ModelRunName_JComboBox.select ( 0 );
    }
}

/**
Populate the site common name list based on the selected datastore.
*/
private void populateSiteCommonNameChoices ( ReclamationHDB_DMI rdmi )
{   String routine = getClass().getName() + ".populateSiteCommonNameChoices";
    if ( (rdmi == null) || (__SiteCommonName_JComboBox == null) ) {
        // Initialization
        return;
    }
    List<String> siteCommonNameStrings = new Vector<String>();
    try {
        readSiteDataTypeList(rdmi);
        siteCommonNameStrings.add("");
        for ( ReclamationHDB_SiteDataType siteDataType: __siteDataTypeList ) {
            siteCommonNameStrings.add ( siteDataType.getSiteCommonName() );
        }
        Collections.sort(siteCommonNameStrings,String.CASE_INSENSITIVE_ORDER);
        StringUtil.removeDuplicates(siteCommonNameStrings, true, true);
    }
    catch ( Exception e ) {
        Message.printWarning(3, routine, "Error getting HDB site data type list (" + e + ")." );
        siteCommonNameStrings = new Vector<String>();
        siteCommonNameStrings.add("");
    }
    __SiteCommonName_JComboBox.removeAll ();
    __SiteCommonName_JComboBox.setData(siteCommonNameStrings);
    // Select first choice (may get reset from existing parameter values).
    __SiteCommonName_JComboBox.select ( null );
    if ( __SiteCommonName_JComboBox.getItemCount() > 0 ) {
        __SiteCommonName_JComboBox.select ( 0 );
    }
}

/**
Populate the site common name list based on the selected datastore.
*/
private void populateSiteDataTypeIDChoices ( ReclamationHDB_DMI rdmi )
{   String routine = getClass().getName() + ".populateSiteDataTypeIDChoices";
    if ( (rdmi == null) || (__SiteDataTypeID_JComboBox == null) ) {
        // Initialization
        return;
    }
    List<String> siteDataTypeIDStrings = new Vector();
    siteDataTypeIDStrings.add ( "" );
    try {
        List<ReclamationHDB_SiteDataType> siteDataTypeList = rdmi.readHdbSiteDataTypeList();
        for ( ReclamationHDB_SiteDataType siteDataType: siteDataTypeList ) {
            siteDataTypeIDStrings.add ( "" + siteDataType.getSiteDataTypeID() );
        }
        Collections.sort(siteDataTypeIDStrings,String.CASE_INSENSITIVE_ORDER);
        StringUtil.removeDuplicates(siteDataTypeIDStrings, true, true);
    }
    catch ( Exception e ) {
        Message.printWarning(3, routine, "Error getting HDB site data type list (" + e + ")." );
        siteDataTypeIDStrings = new Vector();
    }
    __SiteDataTypeID_JComboBox.removeAll ();
    __SiteDataTypeID_JComboBox.setData(siteDataTypeIDStrings);
    // Select first choice (may get reset from existing parameter values).
    __SiteDataTypeID_JComboBox.select ( null );
    if ( __SiteDataTypeID_JComboBox.getItemCount() > 0 ) {
        __SiteDataTypeID_JComboBox.select ( 0 );
    }
}

/**
Read the ensemble list and set for use in the editor.
*/
private void readEnsembleList ( ReclamationHDB_DMI rdmi )
throws Exception
{
    try {
        List<ReclamationHDB_Ensemble> modelList = rdmi.readRefEnsembleList();
        setEnsembleList(modelList);
    }
    catch ( Exception e ) {
        setEnsembleList(new Vector<ReclamationHDB_Ensemble>());
        throw e;
    }
}

/**
Read the model list and set for use in the editor.
*/
private void readModelList ( ReclamationHDB_DMI rdmi )
throws Exception
{
    try {
        List<ReclamationHDB_Model> modelList = rdmi.readHdbModelList();
        setModelList(modelList);
    }
    catch ( Exception e ) {
        setModelList(new Vector<ReclamationHDB_Model>());
        throw e;
    }
}

/**
Read the model run list for the selected model.
*/
private void readModelRunListForSelectedModel ( ReclamationHDB_DMI rdmi )
{   String routine = getClass().getName() + ".readModelRunList";
    String selectedModelName = __ModelName_JComboBox.getSelected();
    List<ReclamationHDB_Model> modelList = rdmi.findModel(__modelList, selectedModelName);
    List<String> modelRunNameStrings = new Vector();
    modelRunNameStrings.add ( "" ); // Always add blank because user may not want model time series
    if ( modelList.size() == 1 ) {
        ReclamationHDB_Model model = modelList.get(0);
        int modelID = model.getModelID();
        Message.printStatus ( 2, routine, "Model ID=" + modelID + " for model name \"" + selectedModelName + "\"" );
        try {
            // There may be no run names for the model id.
            List<ReclamationHDB_ModelRun> modelRunList = rdmi.readHdbModelRunListForModelID( modelID );
            // The following list matches the model_id and can be used for further filtering
            setModelRunList(modelRunList);
        }
        catch ( Exception e ) {
            Message.printWarning(3, routine, "Error getting HDB model run list (" + e + ")." );
            setModelRunList(new Vector<ReclamationHDB_ModelRun>());
        }
    }
    else {
        Message.printStatus ( 2, routine, "Have " + modelList.size() + " models matching name \"" +
            selectedModelName + "\" - unable to find matching model runs." );
    }
}

/**
Read the site_datatype list and set for use in the editor.
*/
private void readSiteDataTypeList ( ReclamationHDB_DMI rdmi )
{   String routine = getClass().getName() + ".readSiteDataTypeIdList";
    try {
        List<ReclamationHDB_SiteDataType> siteDataTypeList = rdmi.readHdbSiteDataTypeList();
        setSiteDataTypeList(siteDataTypeList);
    }
    catch ( Exception e ) {
        Message.printWarning(3, routine, "Error getting HDB site data type list (" + e + ")." );
        setSiteDataTypeList(new Vector<ReclamationHDB_SiteDataType>());
    }
}

/**
Refresh the command string from the dialog contents.
*/
private void refresh ()
{	String routine = getClass().getName() + ".refresh";
	__error_wait = false;
	String DataStore = "";
	String Interval = "";
	String DataType = "";
	String filter_delim = ";";
    String SiteCommonName = "";
    String DataTypeCommonName = "";
    String SiteDataTypeID = "";
    String ModelName = "";
    String ModelRunName = "";
    String HydrologicIndicator = "";
    String ModelRunDate = "";
    String ModelRunID = "";
    String EnsembleName = "";
    String EnsembleTraceID = "";
    String EnsembleModelName = "";
    String EnsembleModelRunDate = "";
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
	    DataType = props.getValue ( "DataType" );
        SiteCommonName = props.getValue ( "SiteCommonName" );
        DataTypeCommonName = props.getValue ( "DataTypeCommonName" );
        SiteDataTypeID = props.getValue ( "SiteDataTypeID" );
        ModelName = props.getValue ( "ModelName" );
        ModelRunName = props.getValue ( "ModelRunName" );
        HydrologicIndicator = props.getValue ( "HydrologicIndicator" );
        ModelRunDate = props.getValue ( "ModelRunDate" );
        ModelRunID = props.getValue ( "ModelRunID" );
        EnsembleName = props.getValue ( "EnsembleName" );
        EnsembleTraceID = props.getValue ( "EnsembleTraceID" );
        EnsembleModelName = props.getValue ( "EnsembleModelName" );
        EnsembleModelRunDate = props.getValue ( "EnsembleModelRunDate" );
		InputStart = props.getValue ( "InputStart" );
		InputEnd = props.getValue ( "InputEnd" );
		Alias = props.getValue ( "Alias" );
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataStore_JComboBox, DataStore, JGUIUtil.NONE, null, null ) ) {
            __DataStore_JComboBox.select ( DataStore );
            if ( __ignoreEvents ) {
                // Also need to make sure that the datastore and DMI are actually selected
                // Call manually because events are disabled at startup to allow cascade to work properly
                setDMIForSelectedDataStore();
            }
            if ( __ignoreEvents ) {
                // Also need to make sure that the __siteDataTypeList is populated
                // Call manually because events are disabled at startup to allow cascade to work properly
                readSiteDataTypeList(__dmi);
            }
            if ( __ignoreEvents ) {
                // Also need to make sure that the __modelList is populated
                // Call manually because events are disabled at startup to allow cascade to work properly
                try {
                    readModelList(__dmi);
                }
                catch ( Exception e ) {
                    // The above call will set the list to empty.
                }
            }
        }
        else {
            if ( (DataStore == null) || DataStore.equals("") ) {
                // New command...select the default...
                if ( __DataStore_JComboBox.getItemCount() > 0 ) {
                    __DataStore_JComboBox.select ( 0 );
                    if ( __ignoreEvents ) {
                        // Also need to make sure that the datastore and DMI are actually selected
                        // Call manually because events are disabled at startup to allow cascade to work properly
                        setDMIForSelectedDataStore();
                    }
                    if ( __ignoreEvents ) {
                        // Also need to make sure that the __siteDataTypeList is populated
                        // Call manually because events are disabled at startup to allow cascade to work properly
                        readSiteDataTypeList(__dmi);
                    }
                    if ( __ignoreEvents ) {
                        // Also need to make sure that the __modelList is populated
                        // Call manually because events are disabled at startup to allow cascade to work properly
                        try {
                            readModelList(__dmi);
                        }
                        catch ( Exception e ) {
                            // The above call will set the list to empty.
                        }
                    }
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataStore parameter \"" + DataStore + "\".  Select a\ndifferent value or Cancel." );
                // Select the first so at least something is visible to user
                __DataStore_JComboBox.select ( 0 );
                if ( __ignoreEvents ) {
                    // Also need to make sure that the datastore and DMI are actually selected
                    // Call manually because events are disabled at startup to allow cascade to work properly
                    setDMIForSelectedDataStore();
                }
                if ( __ignoreEvents ) {
                    // Also need to make sure that the __siteDataTypeList is populated
                    // Call manually because events are disabled at startup to allow cascade to work properly
                    readSiteDataTypeList(__dmi);
                }
                if ( __ignoreEvents ) {
                    // Also need to make sure that the __modelList is populated
                    // Call manually because events are disabled at startup to allow cascade to work properly
                    try {
                        readModelList(__dmi);
                    }
                    catch ( Exception e ) {
                        // The above call will set the list to empty.
                    }
                }
            }
        }
        // First populate the choices...
        populateIntervalChoices();
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
        // First populate the choices...
        populateDataTypeChoices();
        __main_JTabbedPane.setSelectedIndex(0); // Default unless SiteCommonName is specified
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataType_JComboBox, DataType, JGUIUtil.NONE, null, null ) ) {
            __DataType_JComboBox.select ( DataType );
        }
        else {
            if ( (DataType == null) || DataType.equals("") ) {
                // New command...select the default...
                __DataType_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataType parameter \"" + DataType + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        InputFilter_JPanel filter_panel = __inputFilter_JPanel;
        int nfg = filter_panel.getNumFilterGroups();
        String where;
        for ( int ifg = 0; ifg < nfg; ifg ++ ) {
            where = props.getValue ( "Where" + (ifg + 1) );
            if ( (where != null) && (where.length() > 0) ) {
                // Set the filter...
                try {
                    filter_panel.setInputFilter (ifg, where, filter_delim );
                }
                catch ( Exception e ) {
                    Message.printWarning ( 1, routine, "Error setting where information using \"" + where + "\"" );
                    Message.printWarning ( 3, routine, e );
                }
            }
        }
        // First populate the choices...
        populateSiteCommonNameChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__SiteCommonName_JComboBox, SiteCommonName, JGUIUtil.NONE, null, null ) ) {
            __SiteCommonName_JComboBox.select ( SiteCommonName );
            __main_JTabbedPane.setSelectedIndex(1);
        }
        else {
            if ( (SiteCommonName == null) || SiteCommonName.equals("") ) {
                // New command...select the default...
                if ( __SiteCommonName_JComboBox.getItemCount() > 0 ) {
                    __SiteCommonName_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "SiteCommonName parameter \"" + SiteCommonName + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateDataTypeCommonNameChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataTypeCommonName_JComboBox, DataTypeCommonName, JGUIUtil.NONE, null, null ) ) {
            __DataTypeCommonName_JComboBox.select ( DataTypeCommonName );
        }
        else {
            if ( (DataTypeCommonName == null) || DataTypeCommonName.equals("") ) {
                // New command...select the default...
                if ( __DataTypeCommonName_JComboBox.getItemCount() > 0 ) {
                    __DataTypeCommonName_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataTypeCommonName parameter \"" + DataTypeCommonName + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateSiteDataTypeIDChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__SiteDataTypeID_JComboBox, SiteDataTypeID, JGUIUtil.NONE, null, null ) ) {
            __SiteDataTypeID_JComboBox.select ( SiteDataTypeID );
        }
        else {
            if ( (SiteDataTypeID == null) || SiteDataTypeID.equals("") ) {
                // New command...select the default...
                if ( __SiteDataTypeID_JComboBox.getItemCount() > 0 ) {
                    __SiteDataTypeID_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "SateDataTypeID parameter \"" + SiteDataTypeID + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateModelNameChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__ModelName_JComboBox, ModelName, JGUIUtil.NONE, null, null ) ) {
            __ModelName_JComboBox.select ( ModelName );
            __main_JTabbedPane.setSelectedIndex(0);
            if ( __ignoreEvents ) {
                // Also need to make sure that the __modelRunList is populated
                // Call manually because events are disabled at startup to allow cascade to work properly
                readModelRunListForSelectedModel(__dmi);
            }
        }
        else {
            if ( (ModelName == null) || ModelName.equals("") ) {
                // New command...select the default...
                if ( __ModelName_JComboBox.getItemCount() > 0 ) {
                    __ModelName_JComboBox.select ( 0 );
                    if ( __ignoreEvents ) {
                        // Also need to make sure that the __modelRunList is populated
                        // Call manually because events are disabled at startup to allow cascade to work properly
                        readModelRunListForSelectedModel(__dmi);
                    }
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "ModelName parameter \"" + ModelName + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateModelRunNameChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__ModelRunName_JComboBox, ModelRunName, JGUIUtil.NONE, null, null ) ) {
            __ModelRunName_JComboBox.select ( ModelRunName );
        }
        else {
            if ( (ModelRunName == null) || ModelRunName.equals("") ) {
                // New command...select the default...
                if ( __ModelRunName_JComboBox.getItemCount() > 0 ) {
                    __ModelRunName_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "ModelRunName parameter \"" + ModelRunName + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateModelRunDateChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__ModelRunDate_JComboBox, ModelRunDate, JGUIUtil.NONE, null, null ) ) {
            __ModelRunDate_JComboBox.select ( ModelRunDate );
        }
        else {
            if ( (ModelRunDate == null) || ModelRunDate.equals("") ) {
                // New command...select the default...
                if ( __ModelRunDate_JComboBox.getItemCount() > 0 ) {
                    __ModelRunDate_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "ModelRunDate parameter \"" + ModelRunDate + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateHydrologicIndicatorChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__HydrologicIndicator_JComboBox, HydrologicIndicator, JGUIUtil.NONE, null, null ) ) {
            __HydrologicIndicator_JComboBox.select ( HydrologicIndicator );
        }
        else {
            if ( (HydrologicIndicator == null) || HydrologicIndicator.equals("") ) {
                // New command...select the default...
                if ( __HydrologicIndicator_JComboBox.getItemCount() > 0 ) {
                    __HydrologicIndicator_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "HydrologicIndicator parameter \"" + HydrologicIndicator + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateModelRunIDChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__ModelRunID_JComboBox, ModelRunID, JGUIUtil.NONE, null, null ) ) {
            __ModelRunID_JComboBox.select ( ModelRunID );
        }
        else {
            if ( (ModelRunID == null) || ModelRunID.equals("") ) {
                // New command...select the default...
                if ( __ModelRunID_JComboBox.getItemCount() > 0 ) {
                    __ModelRunID_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "SateDataTypeID parameter \"" + ModelRunID + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateEnsembleNameChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__EnsembleName_JComboBox, EnsembleName, JGUIUtil.NONE, null, null ) ) {
            __EnsembleName_JComboBox.select ( EnsembleName );
            __main_JTabbedPane.setSelectedIndex(1);
            if ( __ignoreEvents ) {
                // Also need to make sure that the __modelRunList is populated
                // Call manually because events are disabled at startup to allow cascade to work properly
                readModelRunListForSelectedModel(__dmi);
            }
        }
        else {
            if ( (EnsembleName == null) || EnsembleName.equals("") ) {
                // New command...select the default...
                if ( __EnsembleName_JComboBox.getItemCount() > 0 ) {
                    __EnsembleName_JComboBox.select ( 0 );
                    if ( __ignoreEvents ) {
                        // Also need to make sure that the __modelRunList is populated
                        // Call manually because events are disabled at startup to allow cascade to work properly
                        readModelRunListForSelectedModel(__dmi);
                    }
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "EnsembleName parameter \"" + EnsembleName + "\".  Select a different value or Cancel." );
            }
        }
        /* TODO SAM 2013-04-06 Enable in the future
        if ( EnsembleTraceID != null ) {
            __EnsembleTraceID_JTextField.setText ( EnsembleTraceID );
        }
        */
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
	DataType = __DataType_JComboBox.getSelected().trim();
	Interval = __Interval_JComboBox.getSelected().trim();
    props.add ( "DataStore=" + DataStore );
    props.add ( "DataType=" + DataType );
    props.add ( "Interval=" + Interval );
	// Add the where clause(s)...
	InputFilter_JPanel filter_panel = __inputFilter_JPanel;
	int nfg = filter_panel.getNumFilterGroups();
	String where;
	String delim = ";";	// To separate input filter parts
	for ( int ifg = 0; ifg < nfg; ifg ++ ) {
		where = filter_panel.toString(ifg,delim).trim();
		// Make sure there is a field that is being checked in a where clause...
		if ( (where.length() > 0) && !where.startsWith(delim) ) {
		    // FIXME SAM 2010-11-01 The following discards '=' in the quoted string
			//props.add ( "Where" + (ifg + 1) + "=" + where );
			props.set ( "Where" + (ifg + 1), where );
		}
	}
    // FIXME SAM 2011-10-03 Should be able to remove check for null if events and list population are
    // implemented correctly
    SiteCommonName = __SiteCommonName_JComboBox.getSelected();
    if ( SiteCommonName == null ) {
        SiteCommonName = "";
    }
    DataTypeCommonName = __DataTypeCommonName_JComboBox.getSelected();
    if ( DataTypeCommonName == null ) {
        DataTypeCommonName = "";
    }
    SiteDataTypeID = __SiteDataTypeID_JComboBox.getSelected();
    if ( SiteDataTypeID == null ) {
        SiteDataTypeID = "";
    }
    ModelName = __ModelName_JComboBox.getSelected();
    if ( ModelName == null ) {
        ModelName = "";
    }
    ModelRunName = __ModelRunName_JComboBox.getSelected();
    if ( ModelRunName == null ) {
        ModelRunName = "";
    }
    ModelRunDate = __ModelRunDate_JComboBox.getSelected();
    if ( ModelRunDate == null ) {
        ModelRunDate = "";
    }
    HydrologicIndicator = __HydrologicIndicator_JComboBox.getSelected();
    if ( HydrologicIndicator == null ) {
        HydrologicIndicator = "";
    }
    ModelRunID = __ModelRunID_JComboBox.getSelected();
    if ( ModelRunID == null ) {
        ModelRunID = "";
    }
    EnsembleName = __EnsembleName_JComboBox.getSelected();
    if ( EnsembleName == null ) {
        EnsembleName = "";
    }
    //EnsembleTraceID = __EnsembleTraceID_JTextField.getText().trim();
    EnsembleModelName = __EnsembleModelName_JComboBox.getSelected();
    if ( EnsembleModelName == null ) {
        EnsembleModelName = "";
    }
    EnsembleModelRunDate = __EnsembleModelRunDate_JComboBox.getSelected();
    if ( EnsembleModelRunDate == null ) {
        EnsembleModelRunDate = "";
    }
    props.add ( "SiteCommonName=" + SiteCommonName );
    props.add ( "DataTypeCommonName=" + DataTypeCommonName );
    props.add ( "SiteDataTypeID=" + SiteDataTypeID );
    props.add ( "ModelName=" + ModelName );
    props.add ( "ModelRunName=" + ModelRunName );
    props.add ( "ModelRunDate=" + ModelRunDate );
    props.add ( "HydrologicIndicator=" + HydrologicIndicator );
    props.add ( "ModelRunID=" + ModelRunID );
    props.add ( "EnsembleName=" + EnsembleName );
    //props.add ( "EnsembleTraceID=" + EnsembleTraceID );
    props.add ( "EnsembleModelName=" + EnsembleModelName );
    props.add ( "EnsembleModelRunDate=" + EnsembleModelRunDate );
	InputStart = __InputStart_JTextField.getText().trim();
	props.add ( "InputStart=" + InputStart );
	InputEnd = __InputEnd_JTextField.getText().trim();
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
Set the internal data based on the selected datastore.
*/
private void setDMIForSelectedDataStore()
{   __dataStore = getSelectedDataStore();
    __dmi = (ReclamationHDB_DMI)((DatabaseDataStore)__dataStore).getDMI();
}

/**
Set the HDB ensemble list corresponding to the displayed list.
*/
private void setEnsembleList ( List<ReclamationHDB_Ensemble> ensembleList )
{
    __ensembleList = ensembleList;
}

/**
Set the HDB model list corresponding to the displayed list.
*/
private void setModelList ( List<ReclamationHDB_Model> modelList )
{
    __modelList = modelList;
}

/**
Set the HDB model run list corresponding to the displayed list.
*/
private void setModelRunList ( List<ReclamationHDB_ModelRun> modelRunList )
{
    __modelRunList = modelRunList;
}

/**
Set the HDB site data type list corresponding to the displayed list.
*/
private void setSiteDataTypeList ( List<ReclamationHDB_SiteDataType> siteDataTypeList )
{
    __siteDataTypeList = siteDataTypeList;
}

/**
Update the ensemble information text fields.
*/
private void updateEnsembleIDTextFields ()
{   // Ensemble information...
    List<ReclamationHDB_Ensemble> ensembleList = null;
    try {
        ensembleList = __dmi.findEnsemble(__ensembleList, __EnsembleName_JComboBox.getSelected() );
    }
    catch ( Exception e ) {
        // Generally due to startup with bad datastore
        ensembleList = null;
    }
    if ( (ensembleList == null) || (ensembleList.size() == 0) ) {
        __selectedEnsembleID_JLabel.setText ( "No matches" );
    }
    else if ( ensembleList.size() == 1 ) {
        __selectedEnsembleID_JLabel.setText ( "" + ensembleList.get(0).getEnsembleID() );
    }
    else {
        __selectedEnsembleID_JLabel.setText ( "" + ensembleList.size() + " matches" );
    }
    // Model information...
    List<ReclamationHDB_Model> modelList = null;
    try {
        modelList = __dmi.findModel(__modelList, __EnsembleModelName_JComboBox.getSelected() );
    }
    catch ( Exception e ) {
        // Generally due to startup with bad datastore
        modelList = null;
    }
    if ( (modelList == null) || (modelList.size() == 0) ) {
        __selectedEnsembleModelID_JLabel.setText ( "No matches" );
    }
    else if ( modelList.size() == 1 ) {
        __selectedEnsembleModelID_JLabel.setText ( "" + modelList.get(0).getModelID() );
    }
    else {
        __selectedEnsembleModelID_JLabel.setText ( "" + modelList.size() + " matches" );
    }
}

/**
Update the model information text fields.
*/
private void updateModelIDTextFields ()
{   // Model information...
    List<ReclamationHDB_Model> modelList = null;
    try {
        modelList = __dmi.findModel(__modelList, __ModelName_JComboBox.getSelected() );
    }
    catch ( Exception e ) {
        // Generally due to startup with bad datastore
        modelList = null;
    }
    if ( (modelList == null) || (modelList.size() == 0) ) {
        __selectedModelID_JLabel.setText ( "No matches" );
    }
    else if ( modelList.size() == 1 ) {
        __selectedModelID_JLabel.setText ( "" + modelList.get(0).getModelID() );
    }
    else {
        __selectedModelID_JLabel.setText ( "" + modelList.size() + " matches" );
    }
    // Model run information...
    List<ReclamationHDB_ModelRun> modelRunList = null;
    try {
        modelRunList = new Vector();
    }
    catch ( Exception e ) {
        // Generally due to startup with bad datastore
        modelRunList = null;
    }
    // TODO SAM 2012-03-25 Need to enable
    //__dmi.findModelRun(__modelRunList,
    //    __ModelName_JComboBox.getSelected(),
    //    __ModelRunName_JComboBox.getSelected(),
    //    __ModelRunDate_JComboBox.getSelected(),
    //    __HydrologicIndicator_JTextField.getText().trim() );
    if ( (modelRunList == null) || (modelRunList.size() == 0) ) {
        __selectedModelRunID_JLabel.setText ( "No matches" );
    }
    else if ( modelRunList.size() == 1 ) {
        __selectedModelRunID_JLabel.setText ( "" + modelRunList.get(0).getModelRunID() );
    }
    else {
        __selectedModelRunID_JLabel.setText ( "" + modelRunList.size() + " matches" );
    }
}

/**
Update the model information text fields.
*/
private void updateSiteIDTextFields ()
{
    List<ReclamationHDB_SiteDataType> stdList = null;
    try {
        stdList = __dmi.findSiteDataType(__siteDataTypeList,
            __SiteCommonName_JComboBox.getSelected(), null );
    }
    catch ( Exception e ) {
        // Generally at startup with a bad datastore configuration
        stdList = null;
    }
    if ( (stdList == null) || (stdList.size() == 0) ) {
        __selectedSiteID_JLabel.setText ( "No matches" );
    }
    else if ( stdList.size() > 0 ) {
        __selectedSiteID_JLabel.setText ( "" + stdList.get(0).getSiteID() +
            " (" + stdList.size() + " matches)" );
    }
    else {
        __selectedSiteID_JLabel.setText ( "" + stdList.size() + " matches" );
    }

    try {
        stdList = __dmi.findSiteDataType(__siteDataTypeList,
            __SiteCommonName_JComboBox.getSelected(), __DataTypeCommonName_JComboBox.getSelected() );
    }
    catch ( Exception e ) {
        // Generally at startup with a bad datastore configuration
        stdList = null;
    }
    if ( (stdList == null) || (stdList.size() == 0) ) {
        __selectedSiteDataTypeID_JLabel.setText ( "No matches" );
    }
    else if ( stdList.size() == 1 ) {
        __selectedSiteDataTypeID_JLabel.setText ( "" + stdList.get(0).getSiteDataTypeID() );
    }
    else {
        __selectedSiteDataTypeID_JLabel.setText ( "" + stdList.size() + " matches" );
    }
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