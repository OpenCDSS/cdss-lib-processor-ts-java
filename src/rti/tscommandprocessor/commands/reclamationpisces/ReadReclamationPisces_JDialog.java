package rti.tscommandprocessor.commands.reclamationpisces;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JDialog;
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

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Editor for the ReadReclamationPisces() command.
*/
public class ReadReclamationPisces_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private ReadReclamationPisces_Command __command = null;
private SimpleJComboBox __DataStore_JComboBox = null;
private SimpleJComboBox __DataType_JComboBox;
private SimpleJComboBox __Interval_JComboBox;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
//private JPanel __multipleTS_JPanel = null;
private JTextField __InputStart_JTextField;
private JTextField __InputEnd_JTextField;	
private SimpleJComboBox __IfMissing_JComboBox;
			
private JTextArea __command_JTextArea = null;
// Contains all input filter panels.  Use the ReclamationPiscesDataStore name/description and data type for each to
// figure out which panel is active at any time.
private List<InputFilter_JPanel> __inputFilterJPanelList = new Vector<InputFilter_JPanel>();
private ReclamationPiscesDataStore __dataStore = null; // selected ReclamatinPiscesDataStore
private boolean __error_wait = false; // Is there an error to be cleared up
private boolean __first_time = true;
private boolean __ok = false; // Was OK pressed when closing the dialog.
/*
Number of visible where fields.
*/
private int __numWhere = 4;

private boolean __ignoreEvents = false; // Used to ignore cascading events when initializing the components

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadReclamationPisces_JDialog ( JFrame parent, ReadReclamationPisces_Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	if ( __ignoreEvents ) {
        return; // Startup.
    }
    Object o = event.getSource();
    
    if ( o == __cancel_JButton ) {
        response ( false );
    }
    else if ( o == __ok_JButton ) {
        refresh ();
        checkInput ();
        if ( !__error_wait ) {
            response ( true );
        }
    }
    else {
        // combo boxes...
        refresh();
    }
}

/**
Refresh the data type choices in response to the currently selected Pisces datastore.
@param value if non-null, then the selection is from the command initialization, in which case the
specified data type should be selected
*/
private void actionPerformedDataStoreSelected ( )
{
    if ( __DataStore_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    setDataStoreForSelectedInput();
    Message.printStatus(2, "", "Selected data store " + __dataStore );
    // Now populate the data type choices corresponding to the data store
    populateDataTypeChoices ( getDMI() );
}

/**
Refresh the query choices for the currently selected Pisces datastore.
@param value if non-null, then the selection is from the command initialization, in which case the
specified data type should be selected
*/
private void actionPerformedDataTypeSelected ( )
{
    if ( __DataType_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // Now populate the interval choices corresponding to the data type
    populateIntervalChoices ( getDMI() );
}

/**
Set visible the appropriate input filter, based on the interval and other previous selections.
*/
private void actionPerformedIntervalSelected ( )
{
    if ( __Interval_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // Now populate the filters corresponding to the data type and interval
    selectInputFilter ( getDataStore() );
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
{	// If "AllMatchingTSID", enable the list.
	// Otherwise, clear and disable...
	if ( __DataType_JComboBox != null ) {
		String DataType = getSelectedDataType();
		if ( DataType == null ) {
		    // Initialization
		    DataType = "";
		}
	}
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	if ( __ignoreEvents ) {
        return; // Startup.
    }
    // Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	__error_wait = false;
	// Check parameters for the two command versions...
    String DataStore = __DataStore_JComboBox.getSelected();
    if ( DataStore.length() > 0 ) {
        props.set ( "DataStore", DataStore );
    }
	String Alias = __Alias_JTextField.getText().trim();
	if ( Alias.length() > 0 ) {
		props.set ( "Alias", Alias );
	}
    String DataType = getSelectedDataType();
	if ( DataType.length() > 0 ) {
		props.set ( "DataType", DataType );
	}
	String Interval = __Interval_JComboBox.getSelected();
	if ( Interval.length() > 0 ) {
		props.set ( "Interval", Interval );
	}
	InputFilter_JPanel filterPanel = getVisibleInputFilterPanel();
	if ( filterPanel != null ) {
    	for ( int i = 1; i <= filterPanel.getNumFilterGroups(); i++ ) {
    	    String where = getWhere ( i - 1 );
    	    if ( where.length() > 0 ) {
    	        props.set ( "Where" + i, where );
    	    }
        }
	}
	// Both command types use these...
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
	if ( InputStart.length() > 0 ) {
		props.set ( "InputStart", InputStart );
	}
	if ( InputEnd.length() > 0 ) {
		props.set ( "InputEnd", InputEnd );
	}
    String IfMissing = __IfMissing_JComboBox.getSelected();
    if ( IfMissing.length() > 0 ) {
        props.set ("IfMissing",IfMissing);
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
{   String DataStore = __DataStore_JComboBox.getSelected();
    __command.setCommandParameter ( "DataStore", DataStore );
	String Alias = __Alias_JTextField.getText().trim();
	__command.setCommandParameter ( "Alias", Alias );
	String DataType = getSelectedDataType();
	__command.setCommandParameter ( "DataType", DataType );
	String Interval = __Interval_JComboBox.getSelected();
	__command.setCommandParameter ( "Interval", Interval );
	String delim = ";";
	InputFilter_JPanel filterPanel = getVisibleInputFilterPanel();
	if ( filterPanel != null ) {
		for ( int i = 1; i <= filterPanel.getNumFilterGroups(); i++ ) {
		    String where = getWhere ( i - 1 );
		    if ( where.startsWith(delim) ) {
		        where = "";
		    }
		    __command.setCommandParameter ( "Where" + i, where );
		}
	}
	String InputStart = __InputStart_JTextField.getText().trim();
	__command.setCommandParameter ( "InputStart", InputStart );
	String InputEnd = __InputEnd_JTextField.getText().trim();
	__command.setCommandParameter ( "InputEnd", InputEnd );
    String IfMissing = __IfMissing_JComboBox.getSelected();
    __command.setCommandParameter ( "IfMissing", IfMissing );
}

/**
Return the datastore that is in effect.
*/
private ReclamationPiscesDataStore getDataStore()
{
    return __dataStore;
}

/**
Return the DMI that is in effect.
*/
private ReclamationPiscesDMI getDMI()
{
    if ( __dataStore == null ) {
        return null;
    }
    else {
        return (ReclamationPiscesDMI)__dataStore.getDMI();
    }
}

/**
Get the input filter list.
*/
private List<InputFilter_JPanel> getInputFilterJPanelList ()
{
    return __inputFilterJPanelList;
}

/**
Get the selected data store from the processor.
*/
private ReclamationPiscesDataStore getSelectedDataStore ()
{   String routine = getClass().getSimpleName() + ".getSelectedDataStore";
    String DataStore = __DataStore_JComboBox.getSelected();
    ReclamationPiscesDataStore dataStore = (ReclamationPiscesDataStore)((TSCommandProcessor)
        __command.getCommandProcessor()).getDataStoreForName( DataStore, ReclamationPiscesDataStore.class );
    if ( dataStore != null ) {
        //Message.printStatus(2, routine, "Selected data store is \"" + dataStore.getName() + "\"." );
    }
    else {
        Message.printStatus(2, routine, "Cannot get data store for \"" + DataStore + "\"." );
    }
    return dataStore;
}

/**
Return the selected data type, omitting the leading "group - dataType".
*/
private String getSelectedDataType()
{
    if ( __DataType_JComboBox == null ) {
        return null;
    }
    String dataType = __DataType_JComboBox.getSelected();
    return dataType;
}

/**
Return the visible input filter panel, or null if none visible.
*/
private InputFilter_JPanel getVisibleInputFilterPanel()
{
    List<InputFilter_JPanel> panelList = getInputFilterJPanelList();
    String panelName;
    for ( InputFilter_JPanel panel : panelList ) {
        // Skip default
        panelName = panel.getName();
        if ( (panelName != null) && panelName.equalsIgnoreCase("Default") ) {
            continue;
        }
        if ( panel.isVisible() ) {
            return panel;
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
	String delim = ";";	// To separate input filter parts
	InputFilter_JPanel filter_panel = getVisibleInputFilterPanel();
    String where = "";
    if ( filter_panel != null ) {
        where = filter_panel.toString(ifg,delim).trim();
    }
	return where;
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, ReadReclamationPisces_Command command )
{	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();
	addWindowListener( this );
    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Read 1+ time series from a Reclamation Pisces database."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Refer to the Pisces documentation for information about data types." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specifying the period will limit data that are available for later commands but can increase performance." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
   	
   	__ignoreEvents = true; // So that a full pass of initialization can occur
   	
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Datastore:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStore_JComboBox = new SimpleJComboBox ( false );
    TSCommandProcessor tsProcessor = (TSCommandProcessor)processor;
    List<DataStore> dataStoreList = tsProcessor.getDataStoresByType( ReclamationPiscesDataStore.class );
    // Add a blank because the datastore needs to be selected to trigger other events
    __DataStore_JComboBox.addItem ( "" );
    for ( DataStore dataStore: dataStoreList ) {
        __DataStore_JComboBox.addItem ( dataStore.getName() );
    }
    __DataStore_JComboBox.select ( 0 );
    __DataStore_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataStore_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - Pisces datastore to read."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data type (parameter):"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DataType_JComboBox = new SimpleJComboBox ( false );
	// Seta  prototype so that the dialog layout behaves when a datastore is selected
	__DataType_JComboBox.setPrototypeDisplayValue("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW");
	__DataType_JComboBox.setMaximumRowCount(20);
	__DataType_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __DataType_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required - data type for time series"),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Data interval:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Interval_JComboBox = new SimpleJComboBox ();
	__Interval_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Interval_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required - data interval (time step) for time series."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("Use filters (\"where\" clauses) to limit result size and " +
        "increase performance.  Filters are AND'ed."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    // Initialize all the filters (selection will be based on data store)
    initializeInputFilters ( main_JPanel, ++y, dataStoreList );
	
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
    __InputStart_JTextField.setToolTipText("Specify the input start using a date/time string or ${Property} notation");
    __InputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputStart_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - overrides the global input start."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input end:"), 
        0, ++y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputEnd_JTextField = new JTextField (20);
    __InputEnd_JTextField.setToolTipText("Specify the input end using a date/time string or ${Property} notation");
    __InputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputEnd_JTextField,
        1, y, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - overrides the global input end."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "If missing:"),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    List<String> IfMissing_Vector = new Vector ( 3 );
    IfMissing_Vector.add ( "" );
    IfMissing_Vector.add ( __command._Ignore );
    IfMissing_Vector.add ( __command._Warn );
    __IfMissing_JComboBox = new SimpleJComboBox ( false );
    __IfMissing_JComboBox.setData ( IfMissing_Vector);
    __IfMissing_JComboBox.select ( 0 );
    __IfMissing_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __IfMissing_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - how to handle missing time series (blank=" + __command._Warn + ")."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (4,50);
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	// Refresh the contents (still ignoring events)...
	refresh ();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__cancel_JButton = new SimpleJButton( "Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );

	setTitle ( "Edit " + __command.getCommandName() + " Command" );

	setResizable ( true );
    // Because it is necessary to select the proper input filter during initialization (to transfer an old command's
    // parameter values), the selected input filter may not be desirable for dialog sizing.  Therefore, manually set
    // all panels to visible and then determine the preferred size as the maximum.  Then reselect the appropriate input
    // filter before continuing.
    setAllFiltersVisible();
    // All filters are visible at this point so pack chooses good sizes...
    pack();
    setPreferredSize(getSize()); // Will reflect all filters being visible
    //__multipleTS_JPanel.setPreferredSize(__multipleTS_JPanel.getSize()); // So initial height is maximum height
    selectInputFilter( getDataStore()); // Now go back to the filter for the selected input type and interval
    JGUIUtil.center( this );
    __ignoreEvents = false; // After initialization of components let events happen to dynamically cause cascade
    // Now refresh once more
	refresh();
	checkGUIState(); // Do this again because it may not have happened due to the special event handling
    super.setVisible( true );
}

/**
Initialize input filters for all of the available Pisces datastores.
The input filter panels will be layered on top of each other, but only one will be set visible, based on the
other visible selections.
@param parent_JPanel the panel to receive the input filter panels
@param dataStoreList the list of available ReclamationPiscesDataStore
*/
private void initializeInputFilters ( JPanel parent_JPanel, int y, List<DataStore> dataStoreList )
{   
    // Loop through data stores and add filters for all data groups
    for ( DataStore ds : dataStoreList ) {
        initializeInputFilters_OneFilter ( parent_JPanel, y, (ReclamationPiscesDataStore)ds);
    }
}

/**
Initialize input filters for one Pisces datastore.
@param parent_JPanel the panel to receive the input filter panels
@param y for layout
@param dataStore datastore to use with the filter
*/
private void initializeInputFilters_OneFilter ( JPanel parent_JPanel, int y, ReclamationPiscesDataStore dataStore )
{   String routine = getClass().getSimpleName() + ".initializeInputFilters_OneFilter";
    int buffer = 3;
    Insets insets = new Insets(1,buffer,1,buffer);
    List<InputFilter_JPanel> inputFilterJPanelList = getInputFilterJPanelList();

    boolean visibility = true; // Set this so that the layout manager will figure out the size of the dialog at startup
    int x = 0; // Position in layout manager, same for all since overlap
    try {
        ReclamationPisces_TimeSeries_InputFilter_JPanel panel = new
        	ReclamationPisces_TimeSeries_InputFilter_JPanel ( dataStore, __numWhere );
        JGUIUtil.addComponent(parent_JPanel, panel,
            x, y, 7, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
        inputFilterJPanelList.add ( panel );
        panel.addEventListeners ( this );
        panel.setVisible ( visibility );
    }
    catch ( Exception e ) {
        Message.printWarning ( 2, routine,
        "Unable to initialize input filter for Reclamation Pisces (" + e + ")." );
        Message.printWarning ( 3, routine, e );
    }
}

/**
Respond to ItemEvents.
*/
public void itemStateChanged ( ItemEvent event )
{
    if ( __ignoreEvents ) {
        return; // Startup
    }
    if ( (event.getSource() == __DataStore_JComboBox) && (event.getStateChange() == ItemEvent.SELECTED) ) {
        // User has selected a data store.
        actionPerformedDataStoreSelected ();
    }
    else if ( (event.getSource() == __DataType_JComboBox) && (event.getStateChange() == ItemEvent.SELECTED) ) {
        // User has selected a data type.
        actionPerformedDataTypeSelected ();
    }
    else if ( (event.getSource() == __Interval_JComboBox) && (event.getStateChange() == ItemEvent.SELECTED) ) {
        // User has selected an interval
        actionPerformedIntervalSelected ();
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
Set the data type choices in response to a new data store being selected.
This should match the main TSTool interface
*/
private void populateDataTypeChoices ( ReclamationPiscesDMI dmi )
{	if ( dmi == null ) {
		return;
	}
    // Get the list of valid object/data types from the database
    List<ReclamationPisces_Ref_Parameter> parameters = dmi.getParameterList();
    // Add a wildcard option to get all data types
    List<String> dataTypes = new ArrayList<String>();
    dataTypes.add(0,"*");
    for ( ReclamationPisces_Ref_Parameter p : parameters ) {
    	dataTypes.add(p.getID());
    }
    __DataType_JComboBox.setData ( dataTypes );
    // Select the default...
    __DataType_JComboBox.select(0);
    // Also populate the intervals
    populateIntervalChoices(dmi);
}

/**
Populate the data interval choices in response to a new data type being selected.
This code matches the TSTool main interface code
*/
private void populateIntervalChoices ( ReclamationPiscesDMI dmi )
{   if ( dmi == null ) {
		return;
	}
	__Interval_JComboBox.setData ( dmi.getDataIntervalStringsForParameter(getSelectedDataType()));
    __Interval_JComboBox.select ( 0 );
}

/**
Refresh the command string from the dialog contents.
*/
private void refresh ()
{	String routine = getClass().getSimpleName() + ".refresh";
	String Alias = "";
	__error_wait = false;
	String DataStore = "";
	String DataType = "";
	String Interval = "";
	String filterDelim = ";";
	String InputStart = "";
	String InputEnd = "";
	String IfMissing = "";
	PropList props = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		props = __command.getCommandParameters();
	    DataStore = props.getValue ( "DataStore" );
	    DataType = props.getValue ( "DataType" );
	    Interval = props.getValue ( "Interval" );
		InputStart = props.getValue ( "InputStart" );
		Alias = props.getValue ( "Alias" );
		InputEnd = props.getValue ( "InputEnd" );
		IfMissing = props.getValue ( "IfMissing" );
        // The datastore list is set up in initialize() but is selected here
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataStore_JComboBox, DataStore, JGUIUtil.NONE, null, null ) ) {
            __DataStore_JComboBox.select ( DataStore );
        }
        else {
            if ( (DataStore == null) || DataStore.equals("") ) {
                // New command...select the default...
                __DataStore_JComboBox.select ( null ); // To ensure that following causes an event
                __DataStore_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataStore parameter \"" + DataStore + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        // 
        // Also need to make sure that the input type and DMI are actually selected
        // Call manually because events are disabled at startup to allow cascade to work properly
        setDataStoreForSelectedInput();
        // First populate the data type choices...
        populateDataTypeChoices(getDMI() );
        // Then set to the value from the command.
        //Message.printStatus(2,routine,"Checking to see if DataType=\"" + DataType + "\" is a choice.");
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataType_JComboBox, DataType, JGUIUtil.NONE, null, null ) ) {
            // Existing command so select the matching choice
            //Message.printStatus(2,routine,"DataType=\"" + DataType + "\" was a choice, selecting index " + index[0] + "...");
        	__DataType_JComboBox.select ( DataType );
        }
        else {
            Message.printStatus(2,routine,"DataType=\"" + DataType + "\" was not a choice.");
            if ( (DataType == null) || DataType.equals("") ) {
                // New command...select the default...
                // Populating the list above selects the default that is appropriate so no need to do here
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                    "DataType parameter \"" + DataType + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        // Populate the interval choices based on the selected data type...
        populateIntervalChoices(getDMI() );
        // Now select what the command had previously (if specified)...
        if ( JGUIUtil.isSimpleJComboBoxItem(__Interval_JComboBox, Interval, JGUIUtil.NONE, null, null ) ) {
            __Interval_JComboBox.select ( Interval );
        }
        else {
            if ( (Interval == null) || Interval.equals("") ) {
                // New command...select the default (done when populating the list so no need to do here)...
                //__Interval_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                    "Interval parameter \"" + DataStore + "\".  Select a\ndifferent value or Cancel." );
            }
        }
	    if ( Alias != null ) {
		    __Alias_JTextField.setText ( Alias );
	    }
		// Selecting the data type and interval will result in the corresponding filter group being selected.
		selectInputFilter(getDataStore());
		InputFilter_JPanel filterPanel = getVisibleInputFilterPanel();
		if ( filterPanel != null ) {
    		int nfg = filterPanel.getNumFilterGroups();
    		String where;
    		for ( int ifg = 0; ifg < nfg; ifg++ ) {
    			where = props.getValue ( "Where" + (ifg + 1) );
    			if ( (where != null) && (where.length() > 0) ) {
    				// Set the filter...
    				try {
    				    //Message.printStatus(2,routine,"Setting filter Where" + (ifg + 1) + "=\"" + where + "\" from panel " +
    				    //    filterPanel );
    				    filterPanel.setInputFilter (ifg, where, filterDelim );
    				}
    				catch ( Exception e ) {
    					Message.printWarning ( 1, routine,
    					"Error setting where information using \"" + where + "\"" );
    					Message.printWarning ( 3, routine, e );
    				}
    			}
    		}
		}
		if ( InputStart != null ) {
			__InputStart_JTextField.setText ( InputStart );
		}
		if ( InputEnd != null ) {
			__InputEnd_JTextField.setText ( InputEnd );
		}
        if ( IfMissing == null ) {
            // Select default...
            __IfMissing_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem(
                __IfMissing_JComboBox, IfMissing, JGUIUtil.NONE, null, null ) ) {
                __IfMissing_JComboBox.select ( IfMissing);
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\n" +
                "IfMissing value \"" + IfMissing +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
	}
	// Regardless, reset the command from the fields...
    DataStore = __DataStore_JComboBox.getSelected();
    if ( DataStore == null ) {
        DataStore = "";
    }
	Alias = __Alias_JTextField.getText().trim();
    DataType = getSelectedDataType();
    Interval = __Interval_JComboBox.getSelected();
	// Regardless, reset the command from the fields...
	props = new PropList ( __command.getCommandName() );
    props.add ( "DataStore=" + DataStore );
	props.add ( "Alias=" + Alias );
    props.add ( "DataType=" + DataType );
	props.add ( "Interval=" + Interval );
	// Set the where clauses...
	// Since numbers may cause problems, first unset and then set
	InputFilter_JPanel filterPanel = getVisibleInputFilterPanel();
	if ( filterPanel != null ) {
    	int nfg = filterPanel.getNumFilterGroups();
    	String where;
    	for ( int ifg = 0; ifg < nfg; ifg ++ ) {
    		where = filterPanel.toString(ifg,filterDelim).trim();
    		// Make sure there is a field that is being checked in a where clause...
    		props.unSet("Where" + (ifg + 1) );
    		if ( (where.length() > 0) && !where.startsWith(filterDelim) ) {
                // FIXME SAM 2010-11-01 The following discards '=' in the quoted string
                //props.add ( "Where" + (ifg + 1) + "=" + where );
                props.set ( "Where" + (ifg + 1), where );
                //Message.printStatus(2,routine,"Setting command parameter from visible input filter:  Where" +
                //    (ifg + 1) + "=\"" + where + "\"" );
    		}
    	}
	}
	InputStart = __InputStart_JTextField.getText().trim();
	props.add ( "InputStart=" + InputStart );
	InputEnd = __InputEnd_JTextField.getText().trim();
	props.add ( "InputEnd=" + InputEnd );
	IfMissing = __IfMissing_JComboBox.getSelected();
    props.add ( "IfMissing=" + IfMissing );
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
Select (set visible) the appropriate input filter based on the other data choices.
@param dataStore the data store from the DataStore and InputName parameters. 
*/
private void selectInputFilter ( ReclamationPiscesDataStore dataStore )
{   String routine = getClass().getSimpleName() + ".selectInputFilter";
    // Selected datastore name...
    if ( dataStore == null ) {
        return;
    }
    String dataStoreName = dataStore.getName();
    // Selected data type and interval must be converted to HydroBase internal convention
    // The following lookups are currently hard coded and not read from HydroBase
    List<InputFilter_JPanel> inputFilterJPanelList = getInputFilterJPanelList();
    // Loop through all available input filters and match the datastore name.
    Message.printStatus(2, routine, "Trying to match selected datastore name \"" + dataStoreName + "\"" );
    InputFilter_JPanel panelMatch = null;
    for ( InputFilter_JPanel panel : inputFilterJPanelList ) {
        if ( ((ReclamationPisces_TimeSeries_InputFilter_JPanel)panel).getDataStore().getName().equalsIgnoreCase(dataStoreName) ) {
            panelMatch = panel;
            panel.setVisible(true);
        }
        else {
        	panel.setVisible(false);
        }
    }
    if ( panelMatch == null ) {
        // No normal panels were matched so enable the generic panel, which will be last panel in list
        InputFilter_JPanel panel = inputFilterJPanelList.get(inputFilterJPanelList.size() - 1);
        panel.setVisible(true);
        Message.printStatus(2, routine, "Setting default input filter panel visible.");
    }
}

/**
Set all the filters visible, necessary to help compute layout dimensions and dialog size.
*/
private void setAllFiltersVisible()
{
    List<InputFilter_JPanel> panelList = getInputFilterJPanelList();
    for ( InputFilter_JPanel panel : panelList ) {
        panel.setVisible(true);
    }
}

/**
Set the datastore to use for queries based on the selected data store and input name.
*/
private void setDataStoreForSelectedInput()
{
    // Data store will be used if set.  Otherwise input name is used.
    String dataStoreString = __DataStore_JComboBox.getSelected();
    if ( dataStoreString == null ) {
        dataStoreString = "";
    }
    if ( !dataStoreString.isEmpty() ) {
        // Use the selected data store
        __dataStore = getSelectedDataStore();
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