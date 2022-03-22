// ReadHydroBase_JDialog - Editor for the ReadHydroBase() command.

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

package rti.tscommandprocessor.commands.hydrobase;

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
import java.util.ArrayList;
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

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.TS.TSIdent;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import DWR.DMI.HydroBaseDMI.HydroBaseDMI;
import DWR.DMI.HydroBaseDMI.HydroBaseDataStore;
import DWR.DMI.HydroBaseDMI.HydroBase_GUI_AgriculturalCASSCropStats_InputFilter_JPanel;
import DWR.DMI.HydroBaseDMI.HydroBase_GUI_AgriculturalCASSLivestockStats_InputFilter_JPanel;
import DWR.DMI.HydroBaseDMI.HydroBase_GUI_AgriculturalNASSCropStats_InputFilter_JPanel;
import DWR.DMI.HydroBaseDMI.HydroBase_GUI_CUPopulation_InputFilter_JPanel;
import DWR.DMI.HydroBaseDMI.HydroBase_GUI_GroundWater_InputFilter_JPanel;
import DWR.DMI.HydroBaseDMI.HydroBase_GUI_StationGeolocMeasType_InputFilter_JPanel;
import DWR.DMI.HydroBaseDMI.HydroBase_GUI_StructureGeolocStructMeasType_InputFilter_JPanel;
import DWR.DMI.HydroBaseDMI.HydroBase_GUI_StructureIrrigSummaryTS_InputFilter_JPanel;
import DWR.DMI.HydroBaseDMI.HydroBase_Util;

/**
Editor for the ReadHydroBase() command.
*/
@SuppressWarnings("serial")
public class ReadHydroBase_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private ReadHydroBase_Command __command = null;
private SimpleJComboBox __InputName_JComboBox; // Legacy
private SimpleJComboBox __DataStore_JComboBox = null; // New approach
private SimpleJComboBox __DataType_JComboBox;
private JTextField __WaterClass_JTextField;
private SimpleJComboBox __Interval_JComboBox;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private JTabbedPane __tsInfo_JTabbedPane = null;
private JPanel __multipleTS_JPanel = null;
private JTextField __Location_JTextField;
private JTextField __DataSource_JTextField;
private JTextField __TSID_JTextField;
private JTextField __InputStart_JTextField;
private JTextField __InputEnd_JTextField;	
			/* TODO SAM 2006-04-28 Review code
			As per Ray Bennett always do the fill
			__FillDailyDivFlag_JTextField,
						// Flag to use indicating
						// daily diversion values filled
						// with carry forward.
			*/
private SimpleJComboBox __FillDivRecordsCarryForward_JComboBox;
private JTextField __FillDivRecordsCarryForwardFlag_JTextField;
private JTextField __FillUsingDivCommentsFlag_JTextField;
			/* TODO SAM 2006-04-28 Review code
			As per Ray Bennett always do the fill
private SimpleJComboBox	__FillDailyDiv_JComboBox,
						// Indicate whether to fill
						// daily diversions using the
						// HydroBase carry forward
						// algorithm.
			*/
private SimpleJComboBox	__FillUsingDivComments_JComboBox;
private SimpleJComboBox __IfMissing_JComboBox;
			
private JTextArea __command_JTextArea = null;
// Contains all input filter panels.  Use the HydroBaseDataStore name/description and data type for each to
// figure out which panel is active at any time.
private List<InputFilter_JPanel> __inputFilterJPanelList = new ArrayList<>();
private HydroBaseDataStore __dataStore = null; // selected HydroBaseDataStore
private boolean __error_wait = false; // Is there an error to be cleared up
private boolean __first_time = true;
private boolean __ok = false; // Was OK pressed when closing the dialog.
/*
Number of visible where fields to not overload stored procedure
Number of filters is the maximum - 2 (data type and interval)
*/
private int __numWhere = (HydroBaseDMI.getSPFlexMaxParameters() - 2);

private boolean __ignoreEvents = false; // Used to ignore cascading events when initializing the components

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadHydroBase_JDialog ( JFrame parent, ReadHydroBase_Command command )
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
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "ReadHydroBase");
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
Refresh the data type choices in response to the currently selected HydroBase data store.
@param value if non-null, then the selection is from the command initialization, in which case the
specified data type should be selected
*/
private void actionPerformedDataStoreSelected ( ) {
    if ( __DataStore_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    setDataStoreForSelectedInput();
    //Message.printStatus(2, "", "Selected data store " + __dataStore + " __dmi=" + __dmi );
    // Now populate the data type choices corresponding to the data store
    populateDataTypeChoices ( getDMI() );
}

/**
Refresh the query choices for the currently selected HydroBase data store.
@param value if non-null, then the selection is from the command initialization, in which case the
specified data type should be selected
*/
private void actionPerformedDataTypeSelected ( ) {
    if ( __DataType_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // Now populate the interval choices corresponding to the data type
    populateIntervalChoices ( getDMI() );
}

/**
Refresh the data type choices in response to the currently selected HydroBase input name.
@param value if non-null, then the selection is from the command initialization, in which case the
specified data type should be selected
*/
private void actionPerformedInputNameSelected ( ) {
    if ( __InputName_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    setDataStoreForSelectedInput();
    //Message.printStatus(2, "", "Selected data store " + __dataStore + " __dmi=" + __dmi );
    // Now populate the data type choices corresponding to the data store
    populateDataTypeChoices ( getDMI() );
}

/**
Set visible the appropriate input filter, based on the interval and other previous selections.
*/
private void actionPerformedIntervalSelected ( ) {
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
		// TODO SAM 2007-02-17 Add checks for interval
		//String Interval = "";
		//if (  __Interval_JTextField != null ) {
		//	Interval = __Interval_JTextField.getText().trim();
		//}
		// TODO SAM 2006-04-25 Remove hard-coded types
		// Should not need to hard-code these data types but there
		// is no better way to do it at the moment.
		if ( DataType.equalsIgnoreCase("DivTotal") ||
			DataType.equalsIgnoreCase("DivClass") ||
			DataType.equalsIgnoreCase("RelTotal") ||
			DataType.equalsIgnoreCase("RelClass") ) {
			/* TODO SAM 2006-04-28 Review code
			As per Ray Bennett always do the fill
			However, this may change so allow user to change.
			*/
			JGUIUtil.setEnabled ( __FillDivRecordsCarryForward_JComboBox, true );
			JGUIUtil.setEnabled ( __FillDivRecordsCarryForwardFlag_JTextField, true );
			JGUIUtil.setEnabled ( __FillUsingDivComments_JComboBox, true );
			JGUIUtil.setEnabled ( __FillUsingDivCommentsFlag_JTextField, true );
		}
		else {
		    /* TODO SAM 2006-04-28 Review code
			As per Ray Bennett always do the fill
			However, this may change so allow user to change.
			*/
			JGUIUtil.setEnabled ( __FillDivRecordsCarryForward_JComboBox, false );
			JGUIUtil.setEnabled ( __FillDivRecordsCarryForwardFlag_JTextField, false );
			JGUIUtil.setEnabled ( __FillUsingDivComments_JComboBox, false );
			JGUIUtil.setEnabled ( __FillUsingDivCommentsFlag_JTextField, false );
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
    String InputName = __InputName_JComboBox.getSelected();
    if ( InputName.length() > 0 ) {
        props.set ( "InputName", InputName );
    }
    String DataStore = __DataStore_JComboBox.getSelected();
    if ( DataStore.length() > 0 ) {
        props.set ( "DataStore", DataStore );
    }
	String Alias = __Alias_JTextField.getText().trim();
	if ( Alias.length() > 0 ) {
		props.set ( "Alias", Alias );
	}
	String TSID = __TSID_JTextField.getText().trim();
	if ( TSID.length() > 0 ) {
		props.set ( "TSID", TSID );
	}
    String DataType = getSelectedDataType();
	if ( DataType.length() > 0 ) {
		props.set ( "DataType", DataType );
	}
	String WaterClass = __WaterClass_JTextField.getText().trim();
	if ( WaterClass.length() > 0 ) {
		props.set ( "WaterClass", WaterClass );
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
	// Additional parameters used to help provide additional data...
	/* TODO SAM 2006-04-28 Review code
	As per Ray Bennett always do the fill
	*/
	String FillDivRecordsCarryForward = __FillDivRecordsCarryForward_JComboBox.getSelected();
	if ( FillDivRecordsCarryForward.length() > 0 ) {
		props.set ( "FillDivRecordsCarryForward", FillDivRecordsCarryForward );
	}
	String FillDivRecordsCarryForwardFlag = __FillDivRecordsCarryForwardFlag_JTextField.getText();
	if ( FillDivRecordsCarryForwardFlag.length() > 0 ) {
		props.set ( "FillDivRecordsCarryForwardFlag", FillDivRecordsCarryForwardFlag );
	}
	String FillUsingDivComments = __FillUsingDivComments_JComboBox.getSelected();
	if ( FillUsingDivComments.length() > 0 ) {
		props.set ( "FillUsingDivComments", FillUsingDivComments );
	}
	String FillUsingDivCommentsFlag = __FillUsingDivCommentsFlag_JTextField.getText().trim();
	if ( FillUsingDivCommentsFlag.length() > 0 ) {
		props.set ("FillUsingDivCommentsFlag",FillUsingDivCommentsFlag);
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
{   String InputName = __InputName_JComboBox.getSelected();
    __command.setCommandParameter ( "InputName", InputName );
    String DataStore = __DataStore_JComboBox.getSelected();
    __command.setCommandParameter ( "DataStore", DataStore );
	String Alias = __Alias_JTextField.getText().trim();
	__command.setCommandParameter ( "Alias", Alias );
	String TSID = __TSID_JTextField.getText().trim();
	__command.setCommandParameter ( "TSID", TSID );
	String DataType = getSelectedDataType();
	__command.setCommandParameter ( "DataType", DataType );
	String WaterClass = __WaterClass_JTextField.getText().trim();
	__command.setCommandParameter ( "WaterClass", WaterClass );
	String Interval = __Interval_JComboBox.getSelected();
	__command.setCommandParameter ( "Interval", Interval );
	String delim = ";";
	InputFilter_JPanel filterPanel = getVisibleInputFilterPanel();
	for ( int i = 1; i <= filterPanel.getNumFilterGroups(); i++ ) {
	    String where = getWhere ( i - 1 );
	    if ( where.startsWith(delim) ) {
	        where = "";
	    }
	    __command.setCommandParameter ( "Where" + i, where );
	}
	// Both versions of the commands use these...
	String InputStart = __InputStart_JTextField.getText().trim();
	__command.setCommandParameter ( "InputStart", InputStart );
	String InputEnd = __InputEnd_JTextField.getText().trim();
	__command.setCommandParameter ( "InputEnd", InputEnd );
	/* TODO SAM 2006-04-28 Review code
	As per Ray Bennett always do the fill
	String FillDailyDiv = __FillDailyDiv_JComboBox.getSelected();
	__command.setCommandParameter ( "FillDailyDiv", FillDailyDiv );
	String FillDailyDivFlag =__FillDailyDivFlag_JTextField.getText().trim();
	__command.setCommandParameter ( "FillDailyDivFlag", FillDailyDivFlag );
	*/
	String FillDivRecordsCarryForward = __FillDivRecordsCarryForward_JComboBox.getSelected();
	__command.setCommandParameter (	"FillDivRecordsCarryForward", FillDivRecordsCarryForward );
	String FillDivRecordsCarryForwardFlag = __FillDivRecordsCarryForwardFlag_JTextField.getText();
	__command.setCommandParameter (	"FillDivRecordsCarryForwardFlag", FillDivRecordsCarryForwardFlag );
	String FillUsingDivComments = __FillUsingDivComments_JComboBox.getSelected();
	__command.setCommandParameter (	"FillUsingDivComments", FillUsingDivComments );
	String FillUsingDivCommentsFlag = __FillUsingDivCommentsFlag_JTextField.getText().trim();
	__command.setCommandParameter (	"FillUsingDivCommentsFlag", FillUsingDivCommentsFlag );
    String IfMissing = __IfMissing_JComboBox.getSelected();
    __command.setCommandParameter ( "IfMissing", IfMissing );
}

/**
Return the datastore that is in effect.
*/
private HydroBaseDataStore getDataStore() {
    return __dataStore;
}

/**
Return the DMI that is in effect.
*/
private HydroBaseDMI getDMI() {
    if ( __dataStore == null ) {
        return null;
    }
    else {
        return (HydroBaseDMI)__dataStore.getDMI();
    }
}

/**
Get the input filter list.
*/
private List<InputFilter_JPanel> getInputFilterJPanelList () {
    return __inputFilterJPanelList;
}

/**
Get the input name to use for the TSID.
*/
private String getInputNameForTSID() {
    // Use the data store name if specified
    String DataStore = __DataStore_JComboBox.getSelected();
    if ( (DataStore != null) && !DataStore.equals("") ) {
        return DataStore;
    }
    else {
        String InputName = __InputName_JComboBox.getSelected();
        if ( (InputName != null) && !InputName.equals("") ) {
            return InputName;
        }
        else {
            return "HydroBase"; // Default
        }
    }
}

/**
Get the selected data store from the processor.
*/
private HydroBaseDataStore getSelectedDataStore ()
{   String routine = getClass().getName() + ".getSelectedDataStore";
    String DataStore = __DataStore_JComboBox.getSelected();
    HydroBaseDataStore dataStore = (HydroBaseDataStore)((TSCommandProcessor)
        __command.getCommandProcessor()).getDataStoreForName( DataStore, HydroBaseDataStore.class );
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
private String getSelectedDataType() {
    if ( __DataType_JComboBox == null ) {
        return null;
    }
    String dataType = __DataType_JComboBox.getSelected();
    if ( dataType.indexOf("-") > 0 ) {
        dataType = StringUtil.getToken(dataType,"-",0,1).trim();
    }
    else {
        dataType = dataType.trim();
    }
    return dataType;
}

/**
Get the datastore that matches the selected input name, or return null if not matched/available.
*/
private HydroBaseDataStore getSelectedDataStoreFromInputName ()
{   String routine = getClass().getSimpleName() + ".getSelectedDataStoreFromInputName";
    String inputName = __InputName_JComboBox.getSelected();
    HydroBaseDMI dmi = null;
    try {
        Object o = __command.getCommandProcessor().getPropContents("HydroBaseDMIList");
        if ( o != null ) {
            // Loop through and match the input name
            @SuppressWarnings("unchecked")
			List<HydroBaseDMI> dmiList = (List<HydroBaseDMI>)o;
            String inputName2;
            for ( HydroBaseDMI dmi2 : dmiList ) {
                inputName2 = dmi2.getInputName();
                if ( inputName2 == null ) {
                    inputName2 = "";
                }
                if ( (inputName != null) && inputName.equalsIgnoreCase(inputName2) ) {
                    dmi = dmi2;
                    break;
                }
            }
        }
    }
    catch ( Exception e ){
    	Message.printWarning(3,routine,e);
        dmi = null;
    }
    if ( dmi != null ) {
        //Message.printStatus(2, routine, "Selected input name DMI is \"" + dmi.getInputName() + "\"." );
        return new HydroBaseDataStore("HydroBase", "State of Colorado HydroBase", dmi, true );
    }
    else {
        Message.printStatus(2, routine, "Cannot get DMI for input name \"" + inputName + "\"." );
        return null;
    }
}

/**
Return the visible input filter panel, or null if none visible.
*/
private InputFilter_JPanel getVisibleInputFilterPanel() {
    List<InputFilter_JPanel> panelList = getInputFilterJPanelList();
    String panelName;
    for ( InputFilter_JPanel panel : panelList ) {
        // Skip default
        panelName = panel.getName();
        if ( (panelName != null) && panelName.equalsIgnoreCase("Default") ) {
            continue;
        }
        if ( panel.isVisible() ) {
        	Message.printStatus(2,"","Visible filter panel name is \"" + panelName + "\"");
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
private String getWhere ( int ifg ) {
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
private void initialize ( JFrame parent, ReadHydroBase_Command command )
{	String routine = getClass().getSimpleName() + ".initialize";
	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();
	addWindowListener( this );
    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Read 1+ time series from a HydroBase database, using options from the parameter groups below."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "<html><b>The datastore design is being phased in and the legacy input name " +
        "parameter will be phased out in the future.</b></html>"),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "<html><b>If the Where parameter values do not properly display, resize the dialog larger - this is a display bug.</b></html>"),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Refer to the HydroBase documentation for information about data types." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specifying the period will limit data that are available " +
		"for later commands but can increase performance." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	
   	__ignoreEvents = true; // So that a full pass of initialization can occur
   	
   	JPanel dbJPanel = new JPanel();
   	int ydb = -1;
   	dbJPanel.setLayout(new GridBagLayout());
    dbJPanel.setBorder(BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
        "Specify database to read (datastore name will take precedence if specified)"));

    List<HydroBaseDMI> legacyDmiList = new ArrayList<>();
    try {
        Object o = processor.getPropContents("HydroBaseDMIList");
        if ( o != null ) {
            // Use the first HydroBaseDMI instance, since input filter
            // information should be relatively consistent...
        	@SuppressWarnings("unchecked")
			List<HydroBaseDMI> legacyDmiList0 = (List<HydroBaseDMI>)o;
            legacyDmiList = legacyDmiList0;
        }
    }
    catch ( Exception e ){
        // Not fatal, but of use to developers.
        String message = "No HydroBase connections (non-datastores) are available to use with command editing.\n" +
            "Make sure that HydroBase is open.";
        Message.printWarning(1, routine, message );
    }
    JGUIUtil.addComponent(main_JPanel, dbJPanel,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dbJPanel, new JLabel ("Input name:"),
        0, ++ydb, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputName_JComboBox = new SimpleJComboBox ( false );
    // Add a blank because the input type/name is not required
    List<String> nameChoices = new ArrayList<>();
    nameChoices.add ( "" );
    for ( HydroBaseDMI hbdmi: legacyDmiList ) {
        String inputName = hbdmi.getInputName();
        if ( (inputName == null) || inputName.equals("") ) {
            inputName = "HydroBase"; // Default
        }
        nameChoices.add ( inputName );
    }
    __InputName_JComboBox.setData(nameChoices);
    __InputName_JComboBox.select ( 0 );
    __InputName_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(dbJPanel, __InputName_JComboBox,
        1, ydb, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dbJPanel, new JLabel (
        "Optional - HydroBase connection name (default=HydroBase if no datastore specified)."),
        3, ydb, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(dbJPanel, new JLabel ( "Datastore:"),
        0, ++ydb, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStore_JComboBox = new SimpleJComboBox ( false );
    TSCommandProcessor tsProcessor = (TSCommandProcessor)processor;
    List<DataStore> dataStoreList = tsProcessor.getDataStoresByType( HydroBaseDataStore.class );
    // Add a blank because the data store is not required
    List<String> datastoreChoices = new ArrayList<>();
    datastoreChoices.add ( "" );
    for ( DataStore dataStore: dataStoreList ) {
    	datastoreChoices.add ( dataStore.getName() );
    }
    __DataStore_JComboBox.setData(datastoreChoices);
    __DataStore_JComboBox.select ( 0 );
    __DataStore_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(dbJPanel, __DataStore_JComboBox,
        1, ydb, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dbJPanel, new JLabel("Optional - HydroBase datastore to read (phasing in datastores)."), 
        3, ydb, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	
    //JGUIUtil.addComponent(main_JPanel, inputFilterJPanel,
    //    0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data type:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DataType_JComboBox = new SimpleJComboBox ( false );
	__DataType_JComboBox.setMaximumRowCount(20);
	__DataType_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __DataType_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required - data type for time series"),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Water class:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __WaterClass_JTextField = new JTextField ( "", 20 );
    __WaterClass_JTextField.setToolTipText("Required if a single specific DivClass or RelClass time series is read, provide SFUT string.");
    __WaterClass_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __WaterClass_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - use for specific DivClass & RelClass."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Data interval:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Interval_JComboBox = new SimpleJComboBox ();
	__Interval_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Interval_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required - data interval (time step) for time series."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    __tsInfo_JTabbedPane = new JTabbedPane ();
    __tsInfo_JTabbedPane.setBorder(
        BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
        "Indicate how to match time series in HydroBase" ));
    JGUIUtil.addComponent(main_JPanel, __tsInfo_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JPanel singleTS_JPanel = new JPanel();
    singleTS_JPanel.setLayout(new GridBagLayout());
    __tsInfo_JTabbedPane.addTab ( "Match Single Time Series", singleTS_JPanel );

    int ySingle = -1;
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ( "Location:"),
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
        __Location_JTextField = new JTextField ( "", 20 );
        __Location_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(singleTS_JPanel, __Location_JTextField,
        1, ySingle, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ( "For example, station ID or structure WDID."),
        3, ySingle, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ( "Data source:"),
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
        __DataSource_JTextField = new JTextField ( "", 20 );
        __DataSource_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(singleTS_JPanel, __DataSource_JTextField,
        1, ySingle, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ( "For example: USGS, DWR."),
        3, ySingle, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ( "TSID (full):"),
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TSID_JTextField = new JTextField ( "" );
    __TSID_JTextField.setEditable ( false );
    __TSID_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(singleTS_JPanel, __TSID_JTextField,
        1, ySingle, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ( "Created from above parameters."),
        3, ySingle, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __multipleTS_JPanel = new JPanel();
    __multipleTS_JPanel.setLayout(new GridBagLayout());
    __tsInfo_JTabbedPane.addTab ( "Match 1+ Time Series", __multipleTS_JPanel );
    // Note to warn about performance
    int yMult = -1;
    JGUIUtil.addComponent(__multipleTS_JPanel, new JLabel("Use filters (\"where\" clauses) to limit result size and " +
        "increase performance.  Filters are AND'ed."),
        0, ++yMult, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    // Initialize all the filters (selection will be based on data store)
    initializeInputFilters ( __multipleTS_JPanel, ++yMult, legacyDmiList, dataStoreList );
	
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

	/* TODO SAM 2006-04-27
	As per Ray Bennett this should always be done.
	*/
    
    JPanel divJPanel = new JPanel();
    int ydiv = -1;
    divJPanel.setLayout(new GridBagLayout());
    divJPanel.setBorder(BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
        "Specify how to fill missing values in HydroBase diversion records"));
    JGUIUtil.addComponent(main_JPanel, divJPanel,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(divJPanel, new JLabel ( "Fill daily diversion records using carry forward:"),
		0, ++ydiv, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    List<String> FillDivRecordsCarryForward_List = new ArrayList<>( 3 );
	FillDivRecordsCarryForward_List.add ( "" );
	FillDivRecordsCarryForward_List.add ( __command._False );
	FillDivRecordsCarryForward_List.add ( __command._True );
	__FillDivRecordsCarryForward_JComboBox = new SimpleJComboBox ( false );
	__FillDivRecordsCarryForward_JComboBox.setToolTipText("Fill daily diversion record missing zero values within each irrigation year (Nov-Oct) using carry-forward, and zeros at start.");
	__FillDivRecordsCarryForward_JComboBox.setEnabled(false);
	__FillDivRecordsCarryForward_JComboBox.setData ( FillDivRecordsCarryForward_List);
	__FillDivRecordsCarryForward_JComboBox.select ( 0 );
	__FillDivRecordsCarryForward_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(divJPanel, __FillDivRecordsCarryForward_JComboBox,
		1, ydiv, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(divJPanel, new JLabel (
		"Optional - fill daily diversion records using carry forward (default=" + __command._True + ")."),
		3, ydiv, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(divJPanel, new JLabel ( "Flag for diversion carry forward filled values:"),
		0, ++ydiv, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__FillDivRecordsCarryForwardFlag_JTextField = new JTextField ( "", 5 );
	__FillDivRecordsCarryForwardFlag_JTextField.setToolTipText("Flag for daily values that are filled using carry forward logic, default is \"c\".");
	//__FillDivRecordsCarryForwardFlag_JTextField.setEnabled(false);
	__FillDivRecordsCarryForwardFlag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(divJPanel,
		__FillDivRecordsCarryForwardFlag_JTextField,
		1, ydiv, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(divJPanel, new JLabel (
		"Optional - flag for filled carry forward values (default=\"c\")."),
		3, ydiv, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(divJPanel, new JLabel ( "Fill diversion records using comments:"),
		0, ++ydiv, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    List<String> FillUsingDivComments_List = new ArrayList<>( 3 );
	FillUsingDivComments_List.add ( "" );
	FillUsingDivComments_List.add ( __command._False );
	FillUsingDivComments_List.add ( __command._True );
	__FillUsingDivComments_JComboBox = new SimpleJComboBox ( false );
	__FillUsingDivComments_JComboBox.setToolTipText("Fill diversion record missing values using irrigation year (Nov-Oct) comments, sets missing to zero.");
	__FillUsingDivComments_JComboBox.setData ( FillUsingDivComments_List);
	__FillUsingDivComments_JComboBox.select ( 0 );
	__FillUsingDivComments_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(divJPanel, __FillUsingDivComments_JComboBox,
		1, ydiv, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(divJPanel, new JLabel (
		"Optional - fill diversion records using annual comments (default=" + __command._False + ")."),
		3, ydiv, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(divJPanel, new JLabel ( "Flag for diversion comment filled values:"),
		0, ++ydiv, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__FillUsingDivCommentsFlag_JTextField = new JTextField ( "", 5 );
	__FillUsingDivCommentsFlag_JTextField.setToolTipText("Flag for values that are filled using diversion comments, use \"Auto\" to use the \"notUsed\" value.");
	__FillUsingDivCommentsFlag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(divJPanel,
		__FillUsingDivCommentsFlag_JTextField,
		1, ydiv, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(divJPanel, new JLabel (
		"Optional - flag for filled diversion comment values (default=\"Auto\" to use \"notUsed\" value)."),
		3, ydiv, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "If missing:"),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    List<String> IfMissing_List = new ArrayList<>( 3 );
    IfMissing_List.add ( "" );
    IfMissing_List.add ( __command._Ignore );
    IfMissing_List.add ( __command._Warn );
    __IfMissing_JComboBox = new SimpleJComboBox ( false );
    __IfMissing_JComboBox.setData ( IfMissing_List);
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

	__ok_JButton = new SimpleJButton("OK", this);
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add ( __ok_JButton );
	__cancel_JButton = new SimpleJButton( "Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + " Command" );

    // Because it is necessary to select the proper input filter during initialization (to transfer an old command's
    // parameter values), the selected input filter may not be desirable for dialog sizing.  Therefore, manually set
    // all panels to visible and then determine the preferred size as the maximum.  Then reselect the appropriate input
    // filter before continuing.
    setAllFiltersVisible();
    // All filters are visible at this point so pack chooses good sizes...
    pack();
    setPreferredSize(getSize()); // Will reflect all filters being visible
    __multipleTS_JPanel.setPreferredSize(__multipleTS_JPanel.getSize()); // So initial height is maximum height
    selectInputFilter( getDataStore()); // Now go back to the filter for the selected input type and intern
    //setSize(810,740); // TODO SAM 2012-09-25 Need to not hard-code size.  However, in order to properly initialize the
    // input filter have to select the correct one at initialization - this may lead to NOT the biggest one being
    // selected - needs more TLC - DivTotal is likely the biggest filter panel
    JGUIUtil.center( this );
    __ignoreEvents = false; // After initialization of components let events happen to dynamically cause cascade
    // Now refresh once more
	refresh();
	checkGUIState(); // Do this again because it may not have happened due to the special event handling
	setResizable ( false );
    super.setVisible( true );
}

/**
Initialize input filters for all of the available HydroBase input/names and data stores.
The input filter panels will be layered on top of each other, but only one will be set visible, based on the
other visible selections.
@param parent_JPanel the panel to receive the input filter panels
@param legacyDMIList the list of available legacy HydroBaseDMI that are handled outside of data stores
@param dataStoreList the list of available HydroBaseDataStore
*/
private void initializeInputFilters ( JPanel parent_JPanel, int y, List<HydroBaseDMI> legacyDMIList,
    List<DataStore> dataStoreList ) {   
    // Loop through data stores and add filters for all data groups
    for ( DataStore ds : dataStoreList ) {
        initializeInputFilters_OneFilter ( parent_JPanel, y, (HydroBaseDataStore)ds);
    }
    
    // Loop through the legacy DMIs and add filters for all data groups.  Put after new datastores
    // because datastores take precedence over the DMI approach
    
    String name;
    for ( HydroBaseDMI dmi : legacyDMIList ) {
        name = dmi.getInputName();
        if ( name.equals("") ) {
            name = "HydroBase";
        }
        initializeInputFilters_OneFilter ( parent_JPanel, y,
            new HydroBaseDataStore(name, "State of Colorado HydroBase", dmi, true ) );
    }
    
    // Blank panel indicating data type was not matched (software problem or unknown HydroBase features?)
    // Add in the same position as the other filter panels

    int buffer = 3;
    Insets insets = new Insets(1,buffer,1,buffer);
    List<InputFilter_JPanel> ifPanelList = getInputFilterJPanelList();
    InputFilter_JPanel panel = new InputFilter_JPanel("Data type and interval have no input filters.");
    panel.setName("Default");
    JGUIUtil.addComponent(parent_JPanel, panel,
        0, y, 7, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
    ifPanelList.add ( panel );
}

/**
Initialize input filters for one HydroBase datastore.
@param parent_JPanel the panel to receive the input filter panels
@param y for layout
@param dataStore datastore to use with the filter
*/
private void initializeInputFilters_OneFilter ( JPanel parent_JPanel, int y, HydroBaseDataStore dataStore )
{   String routine = getClass().getSimpleName() + ".initializeInputFilters_OneFilter";
    int buffer = 3;
    Insets insets = new Insets(1,buffer,1,buffer);
    List<InputFilter_JPanel> inputFilterJPanelList = getInputFilterJPanelList();

    boolean visibility = true; // Set this so that the layout manager will figure out the size of the dialog at startup
    int x = 0; // Position in layout manager, same for all since overlap
    int numVisibleChoices = -1; // For the combobox choices, -1 means size to data list size
    try {
        // Stations...
        HydroBase_GUI_StationGeolocMeasType_InputFilter_JPanel panel = new
            HydroBase_GUI_StationGeolocMeasType_InputFilter_JPanel ( dataStore );
        panel.setName(dataStore.getName() + "_Stations" );
        JGUIUtil.addComponent(parent_JPanel, panel,
            x, y, 7, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
        inputFilterJPanelList.add ( panel );
        panel.addEventListeners ( this );
        panel.setVisible ( visibility );
    }
    catch ( Exception e ) {
        Message.printWarning ( 2, routine,
        "Unable to initialize input filter for HydroBase stations (" + e + ")." );
        Message.printWarning ( 3, routine, e );
    }

    try {
        // Structure with SFUT...
        // Number of filters is the maximum - 2 (data type and interval)
        boolean enableSFUT = true;
        HydroBase_GUI_StructureGeolocStructMeasType_InputFilter_JPanel panel = new
            HydroBase_GUI_StructureGeolocStructMeasType_InputFilter_JPanel (
            dataStore, enableSFUT, __numWhere, numVisibleChoices );
        panel.setName(dataStore.getName() + "_StructureWithSFUT" );
        JGUIUtil.addComponent(parent_JPanel, panel,
            x, y, 7, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL,
            GridBagConstraints.WEST );
        inputFilterJPanelList.add ( panel );
        panel.addEventListeners ( this );
        panel.setVisible ( visibility );
    }
    catch ( Exception e ) {
        Message.printWarning ( 2, routine,
            "Unable to initialize input filter for HydroBase structures with SFUT (" + e + ")." );
        Message.printWarning ( 3, routine, e );
    }
    
    try {
        // Structure total (no SFUT)...
        boolean enableSFUT = false;
        HydroBase_GUI_StructureGeolocStructMeasType_InputFilter_JPanel panel = new
            HydroBase_GUI_StructureGeolocStructMeasType_InputFilter_JPanel ( dataStore, enableSFUT, __numWhere,
                numVisibleChoices );
        panel.setName(dataStore.getName() + "_Structure" );
        JGUIUtil.addComponent(parent_JPanel, panel,
            x, y, 7, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL,
            GridBagConstraints.WEST );
        inputFilterJPanelList.add ( panel );
        panel.addEventListeners ( this );
        panel.setVisible ( visibility );
    }
    catch ( Exception e ) {
        Message.printWarning ( 2, routine,
        "Unable to initialize input filter for HydroBase structures (" + e + ")." );
        Message.printWarning ( 3, routine, e );
    }

    try {
        // CASS agricultural statistics...

        HydroBase_GUI_AgriculturalCASSCropStats_InputFilter_JPanel panel = new
            HydroBase_GUI_AgriculturalCASSCropStats_InputFilter_JPanel ( dataStore );
        panel.setName(dataStore.getName() + "_CASS" );
        JGUIUtil.addComponent(parent_JPanel, panel,
            x, y, 7, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
        inputFilterJPanelList.add ( panel );
        panel.addEventListeners ( this );
        panel.setVisible(visibility );
    }
    catch ( Exception e ) {
        // Agricultural_CASS_crop_stats probably not in HydroBase...
        Message.printWarning ( 2, routine,
        "Unable to initialize input filter for HydroBase agricultural_CASS_crop_stats (" + e + ")." );
        Message.printWarning ( 3, routine, e );
    }

    try {
        // CASS livestock statistics...

        HydroBase_GUI_AgriculturalCASSLivestockStats_InputFilter_JPanel panel = new
            HydroBase_GUI_AgriculturalCASSLivestockStats_InputFilter_JPanel ( dataStore );
        panel.setName(dataStore.getName() + "_CASSLivestock" );
        JGUIUtil.addComponent(parent_JPanel, panel,
            x, y, 7, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
        inputFilterJPanelList.add ( panel );
        panel.addEventListeners ( this );
        panel.setVisible(visibility );
    }
    catch ( Exception e ) {
        // Agricultural_CASS_livestock_stats probably not in HydroBase...
        Message.printWarning ( 2, routine,
        "Unable to initialize input filter for HydroBase agricultural_CASS_livestock_stats (" + e + ")." );
        Message.printWarning ( 3, routine, e );
    }

    try {
        HydroBase_GUI_CUPopulation_InputFilter_JPanel panel = new
            HydroBase_GUI_CUPopulation_InputFilter_JPanel( dataStore );
        panel.setName(dataStore.getName() + "_CUPopulation" );
        JGUIUtil.addComponent(parent_JPanel, panel,
            x, y, 7, 1, 1.0, 0.0, insets, GridBagConstraints.HORIZONTAL,
            GridBagConstraints.WEST );
        panel.setName("HydroBase.CUPopulationInputFilterPanel");
        inputFilterJPanelList.add ( panel );
        panel.addEventListeners ( this );
        panel.setVisible( visibility);
    }
    catch ( Exception e ) {
        // CUPopulation probably not in HydroBase...
        Message.printWarning ( 2, routine,
                "Unable to initialize input filter for HydroBase CU population data (" + e + ")." );
        Message.printWarning ( 3, routine, e );
    }
    
    // NASS agricultural statistics...

    try {
        HydroBase_GUI_AgriculturalNASSCropStats_InputFilter_JPanel panel = new
            HydroBase_GUI_AgriculturalNASSCropStats_InputFilter_JPanel ( dataStore );
        panel.setName(dataStore.getName() + "_NASS" );
        JGUIUtil.addComponent(parent_JPanel, panel,
            x, y, 7, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL,
            GridBagConstraints.WEST );
        inputFilterJPanelList.add ( panel );
        panel.addEventListeners ( this );
        panel.setVisible( visibility);
    }
    catch ( Exception e ) {
        // Agricultural_NASS_crop_stats probably not in HydroBase...
        Message.printWarning ( 2, routine,
        "Unable to initialize input filter for HydroBase agricultural_NASS_crop_stats (" + e + ")." );
        Message.printWarning ( 3, routine, e );
    }

    try {
        // Structure irrig summary TS...
        HydroBase_GUI_StructureIrrigSummaryTS_InputFilter_JPanel panel = new
            HydroBase_GUI_StructureIrrigSummaryTS_InputFilter_JPanel ( dataStore );
        panel.setName(dataStore.getName() + "_IrrigSummary" );
        JGUIUtil.addComponent(parent_JPanel, panel,
            x, y, 7, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL,
            GridBagConstraints.WEST );
        inputFilterJPanelList.add ( panel );
        panel.addEventListeners ( this );
        panel.setVisible ( visibility );
    }
    catch ( Exception e ) {
        Message.printWarning ( 2, routine,
        "Unable to initialize input filter for HydroBase irrigation summary time series (" + e + ")." );
        Message.printWarning ( 3, routine, e );
    }

    try {
        HydroBase_GUI_GroundWater_InputFilter_JPanel panel =
            new HydroBase_GUI_GroundWater_InputFilter_JPanel ( dataStore, null, true);
        panel.setName(dataStore.getName() + "_Groundwater" );
        JGUIUtil.addComponent(parent_JPanel,panel,
            x, y, 7, 1, 1.0, 0.0, insets, GridBagConstraints.HORIZONTAL,
            GridBagConstraints.WEST);
        panel.setName("HydroBase.WellsInputFilterPanel");
        inputFilterJPanelList.add( panel);
        panel.addEventListeners ( this );
        panel.setVisible( visibility);
    }
    catch ( Exception e ) {
        Message.printWarning ( 2, routine,
            "Unable to initialize input filter for HydroBase wells - old database?" );
        Message.printWarning ( 3, routine, e );
    }
}

/**
Respond to ItemEvents.
*/
public void itemStateChanged ( ItemEvent event ) {
    if ( __ignoreEvents ) {
        return; // Startup
    }
    if ( (event.getSource() == __InputName_JComboBox) && (event.getStateChange() == ItemEvent.SELECTED) ) {
        // User has selected an input name.
        actionPerformedInputNameSelected ();
    }
    else if ( (event.getSource() == __DataStore_JComboBox) && (event.getStateChange() == ItemEvent.SELECTED) ) {
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

//TODO SAM 2012-09-13 Evaluate if can share code with TSTool main interface
/**
Set the data type choices in response to a new data store being selected.
This should match the main TSTool interface
*/
private void populateDataTypeChoices ( HydroBaseDMI dmi ) {
    List<String> dataTypes =
        HydroBase_Util.getTimeSeriesDataTypes (dmi,
        HydroBase_Util.DATA_TYPE_AGRICULTURE |
        HydroBase_Util.DATA_TYPE_DEMOGRAPHICS_ALL |
        HydroBase_Util.DATA_TYPE_HARDWARE |
        HydroBase_Util.DATA_TYPE_STATION_ALL |
        HydroBase_Util.DATA_TYPE_STRUCTURE_ALL,
        true ); // Add notes
    __DataType_JComboBox.setData ( dataTypes );
    // Select the default...
    __DataType_JComboBox.select(HydroBase_Util.getDefaultTimeSeriesDataType(dmi, true ) );
}

/**
Populate the data interval choices in response to a new data type being selected.
This code matches the TSTool main interface code
*/
private void populateIntervalChoices ( HydroBaseDMI dmi )
{   String selectedDataType = getSelectedDataType();
    //Message.printStatus ( 2, "", "Populating intervals for selected data type \"" + selectedDataType + "\"" );
    List<String> timeSteps = HydroBase_Util.getTimeSeriesTimeSteps ( dmi,
        selectedDataType,
        HydroBase_Util.DATA_TYPE_AGRICULTURE |
        HydroBase_Util.DATA_TYPE_DEMOGRAPHICS_ALL |
        HydroBase_Util.DATA_TYPE_HARDWARE |
        HydroBase_Util.DATA_TYPE_STATION_ALL |
        HydroBase_Util.DATA_TYPE_STRUCTURE_ALL );
    __Interval_JComboBox.setData ( timeSteps );
    // Select monthly as the default if available...
    if ( JGUIUtil.isSimpleJComboBoxItem(__Interval_JComboBox,"Month", JGUIUtil.NONE, null, null ) ) {
        __Interval_JComboBox.select ( "Month" );
    }
    else {
        // Select the first item...
        try {
            __Interval_JComboBox.select ( null ); // To force event
            __Interval_JComboBox.select ( 0 );
        }
        catch ( Exception e ) {
            // Cases when for some reason no choice is available.
            __Interval_JComboBox.add ( "" );
            __Interval_JComboBox.select ( 0 );
        }
    }
}

/**
Refresh the command string from the dialog contents.
*/
private void refresh ()
{	String routine = getClass().getSimpleName() + ".refresh";
	String Alias = "";
	__error_wait = false;
	String DataStore = "";
	String InputName = "";
	String DataType = "";
	String WaterClass = "";
	String Interval = "";
	String TSID = "";
	String filterDelim = ";";
	String InputStart = "";
	String InputEnd = "";
	/* TODO SAM 2006-04-28 Review code
	As per Ray Bennett always do the fill
	*/
	String FillDivRecordsCarryForward = "";
	String FillDivRecordsCarryForwardFlag = "";
	String FillUsingDivComments = "";
	String FillUsingDivCommentsFlag = "";
	String IfMissing = "";
	PropList props = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		props = __command.getCommandParameters();
	    InputName = props.getValue ( "InputName" );
	    DataStore = props.getValue ( "DataStore" );
	    DataType = props.getValue ( "DataType" );
	    WaterClass = props.getValue ( "WaterClass" );
	    Interval = props.getValue ( "Interval" );
		Alias = props.getValue ( "Alias" );
		TSID = props.getValue ( "TSID" );
		InputStart = props.getValue ( "InputStart" );
		InputEnd = props.getValue ( "InputEnd" );
		/* TODO SAM 2006-04-28 Review code
		As per Ray Bennett always do the fill
		*/
		FillDivRecordsCarryForward = props.getValue ( "FillDivRecordsCarryForward" );
		FillDivRecordsCarryForwardFlag = props.getValue ( "FillDivRecordsCarryForwardFlag" );
		FillUsingDivComments = props.getValue ( "FillUsingDivComments" );
		FillUsingDivCommentsFlag = props.getValue ( "FillUsingDivCommentsFlag" );
		IfMissing = props.getValue ( "IfMissing" );
        // The input name list is set up in initialize() but is selected here
		// It may also be reset below from the TSID
        if ( JGUIUtil.isSimpleJComboBoxItem(__InputName_JComboBox, InputName, JGUIUtil.NONE, null, null ) ) {
            __InputName_JComboBox.select ( null ); // To ensure that following causes an event
            __InputName_JComboBox.select ( InputName ); // This will trigger getting the DMI for use in the editor
        }
        else {
            if ( (InputName == null) || InputName.equals("") ) {
                // New command...select the default...
                __InputName_JComboBox.select ( null ); // To ensure that following causes an event
                __InputName_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "InputName parameter \"" + InputName + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        // The data store list is set up in initialize() but is selected here
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataStore_JComboBox, DataStore, JGUIUtil.NONE, null, null ) ) {
            __DataStore_JComboBox.select ( null ); // To ensure that following causes an event
            __DataStore_JComboBox.select ( DataStore ); // This will trigger getting the DMI for use in the editor
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
        // Data types as displayed are verbose:  "Climate - Precip"
        // Therefore, select in the list based only on the second token (position 2 since two spaces around dash)
        // First populate the data type choices...
        populateDataTypeChoices(getDMI() );
        // Then set to the value from the command.
        int [] index = new int[1];
        //Message.printStatus(2,routine,"Checking to see if DataType=\"" + DataType + "\" is a choice.");
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataType_JComboBox, DataType, JGUIUtil.CHECK_SUBSTRINGS, " ", 2, index, true ) ) {
            // Existing command so select the matching choice
            //Message.printStatus(2,routine,"DataType=\"" + DataType + "\" was a choice, selecting index " + index[0] + "...");
            __DataType_JComboBox.select(index[0]);
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
	    if ( WaterClass != null ) {
		    __WaterClass_JTextField.setText ( WaterClass );
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
		if ( (TSID != null) && !TSID.equals("") ) {
		    // Input type is selected from the TSID
			try {
			    TSIdent tsident = new TSIdent ( TSID );
				if ( __Location_JTextField != null ) {
					__Location_JTextField.setText (	tsident.getLocation() );
				}
				if ( __DataSource_JTextField != null ) {
					__DataSource_JTextField.setText(tsident.getSource() );
				}
				String dataType = tsident.getType();
				if ( dataType.startsWith("WaterClass") || dataType.startsWith("'WaterClass") ) {
					// DataType choice is only "WaterClass"
					// - set after processing out the water class
					// WaterClass is the part after "WaterClass-"
					int pos = dataType.indexOf("-");
					if ( pos > 0 ) {
						// Remove single quotes in the water class that the user sees
						String waterClass = dataType.substring(pos + 1).replace("'", "");
						__WaterClass_JTextField.setText ( waterClass );
					}
					else {
						__WaterClass_JTextField.setText ( "" );
					}
					// Will set the data type below
					dataType = "WaterClass";
				}
				__DataType_JComboBox.setText ( dataType );
				__Interval_JComboBox.setText ( tsident.getInterval() );
				__InputName_JComboBox.select ( tsident.getInputName() );
			    // Make the single time series tab visible
			    __tsInfo_JTabbedPane.setSelectedIndex(0);
			}
			catch ( Exception e ) {
				// For now do nothing.
			}
		}
		else {
            // Make the multiple time series tab visible
            __tsInfo_JTabbedPane.setSelectedIndex(1);
		}
		// Selecting the data type and interval will result in the corresponding filter group being selected.
		selectInputFilter(getDataStore());
		InputFilter_JPanel filterPanel = getVisibleInputFilterPanel();
		if ( filterPanel == null ) {
			Message.printWarning(1, routine, "Trouble finding visible input filter panel for selected HydroBase - contact software support." );
		}
		else {
    		int nfg = filterPanel.getNumFilterGroups();
    		String where;
    		for ( int ifg = 0; ifg < nfg; ifg++ ) {
    			where = props.getValue ( "Where" + (ifg + 1) );
    			if ( (where != null) && (where.length() > 0) ) {
    				// Set the filter...
    				try {
    				    Message.printStatus(2,routine,"Setting filter Where" + (ifg + 1) + "=\"" + where + "\" from panel " + filterPanel );
    				    filterPanel.setInputFilter (ifg, where, filterDelim );
    				}
    				catch ( Exception e ) {
    					Message.printWarning ( 1, routine,
    					"Error setting where information using \"" + where + "\"" );
    					Message.printWarning ( 3, routine, e );
    				}
    			}
    		}
		    // For some reason the values do not always show up so invalidate the component to force redraw
		    // TODO SAM 2016-08-20 This still does not work
    		Message.printStatus(2,routine,"Revalidating component to force redraw.");
		    filterPanel.revalidate();
		    //filterPanel.repaint();
		}
		if ( InputStart != null ) {
			__InputStart_JTextField.setText ( InputStart );
		}
		if ( InputEnd != null ) {
			__InputEnd_JTextField.setText ( InputEnd );
		}
		/* TODO SAM 2006-04-28 Review code
		As per Ray Bennett always do the fill
		*/
		if ( FillDivRecordsCarryForward == null ) {
			// Select default...
			__FillDivRecordsCarryForward_JComboBox.select ( 0 );
		}
		else {
		    if ( JGUIUtil.isSimpleJComboBoxItem( __FillDivRecordsCarryForward_JComboBox,
				FillDivRecordsCarryForward, JGUIUtil.NONE, null, null ) ) {
				__FillDivRecordsCarryForward_JComboBox.select ( FillDivRecordsCarryForward);
			}
			else {
			    Message.printWarning ( 1, routine,
				"Existing command references an invalid FillDivRecordsCarryForward value \"" +
				FillDivRecordsCarryForward + "\".  Select a different value or Cancel.");
				__error_wait = true;
			}
		}
		if ( FillDivRecordsCarryForwardFlag != null ) {
			__FillDivRecordsCarryForwardFlag_JTextField.setText(FillDivRecordsCarryForwardFlag);
		}
		if ( FillUsingDivComments == null ) {
			// Select default...
			__FillUsingDivComments_JComboBox.select ( 0 );
		}
		else {
		    if ( JGUIUtil.isSimpleJComboBoxItem( __FillUsingDivComments_JComboBox,
				FillUsingDivComments, JGUIUtil.NONE, null, null ) ) {
				__FillUsingDivComments_JComboBox.select ( FillUsingDivComments);
			}
			else {
			    Message.printWarning ( 1, routine,
				"Existing command references an invalid FillUsingDivComments value \"" +
				FillUsingDivComments + "\".  Select a different value or Cancel.");
				__error_wait = true;
			}
		}
		if ( FillUsingDivCommentsFlag != null ) {
			__FillUsingDivCommentsFlag_JTextField.setText(FillUsingDivCommentsFlag);
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
	InputName = __InputName_JComboBox.getSelected();
    if ( InputName == null ) {
        InputName = "";
    }
    DataStore = __DataStore_JComboBox.getSelected();
    if ( DataStore == null ) {
        DataStore = "";
    }
	Alias = __Alias_JTextField.getText().trim();
	String Location = __Location_JTextField.getText().trim();
	String DataSource = __DataSource_JTextField.getText().trim();
    DataType = getSelectedDataType();
	WaterClass = __WaterClass_JTextField.getText().trim();
    Interval = __Interval_JComboBox.getSelected();
    boolean doQuote = false;
	if ( (WaterClass != null) && !WaterClass.isEmpty() ) {
		if ( WaterClass.indexOf(".") > 0 ) {
			doQuote = true;
		}
	}
	StringBuffer b = new StringBuffer();
	b.append ( Location );
	b.append ( "." );
	b.append ( DataSource );
	b.append ( "." );
	if ( doQuote ) {
		b.append("'");
	}
	b.append ( DataType );
	if ( (WaterClass != null) && !WaterClass.isEmpty() ) {
		b.append("-" + WaterClass);
	}
	if ( doQuote ) {
		b.append("'");
	}
	b.append ( "." );
	b.append ( Interval );
	b.append ( "~" + getInputNameForTSID() );
	TSID = b.toString();
	if ( Location.equals("") || DataSource.equals("") || (DataType == null) || DataType.equals("") ||
	    (Interval == null) || Interval.equals("")) {
	    // Not enough information so assume using the where filters
	    TSID = "";
	}
	__TSID_JTextField.setText ( TSID );
	// Regardless, reset the command from the fields...
	props = new PropList ( __command.getCommandName() );
    props.add ( "InputName=" + InputName );
    props.add ( "DataStore=" + DataStore );
	props.add ( "Alias=" + Alias );
	props.add ( "TSID=" + TSID );
    props.add ( "DataType=" + DataType );
	props.add ( "WaterClass=" + WaterClass );
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
	/* TODO SAM 2006-04-28 Review code
	As per Ray Bennett always do the fill
	*/
	FillDivRecordsCarryForward = __FillDivRecordsCarryForward_JComboBox.getSelected();
	props.add ( "FillDivRecordsCarryForward=" + FillDivRecordsCarryForward );
	FillDivRecordsCarryForwardFlag = __FillDivRecordsCarryForwardFlag_JTextField.getText();
	props.add ( "FillDivRecordsCarryForwardFlag=" + FillDivRecordsCarryForwardFlag );
	FillUsingDivComments = __FillUsingDivComments_JComboBox.getSelected();
	props.add ( "FillUsingDivComments=" + FillUsingDivComments );
	FillUsingDivCommentsFlag =__FillUsingDivCommentsFlag_JTextField.getText().trim();
	props.add ( "FillUsingDivCommentsFlag=" + FillUsingDivCommentsFlag );
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
private void selectInputFilter ( HydroBaseDataStore dataStore )
{   String routine = getClass().getSimpleName() + ".selectInputFilter";
    // Selected datastore name...
    if ( dataStore == null ) {
        return;
    }
    String dataStoreName = dataStore.getName();
    // Selected data type and interval must be converted to HydroBase internal convention
    // The following lookups are currently hard coded and not read from HydroBase
    String selectedDataType = getSelectedDataType();
    String selectedTimeStep = __Interval_JComboBox.getSelected();
    String [] hb_mt = HydroBase_Util.convertToHydroBaseMeasType( selectedDataType, selectedTimeStep );
    String hbMeasType = hb_mt[0];
    List<InputFilter_JPanel> inputFilterJPanelList = getInputFilterJPanelList();
    // Loop through all available input filters and match the data store name, type (whether legacy or new design),
    // and filter for the data type.  If matched, set to visible and otherwise not visible.
    boolean matched;
    int matchCount = 0;
    HydroBaseDMI hbdmi;
    Message.printStatus(2, routine, "Trying to set visible the input filter given selected datastore name \"" + dataStoreName +
        "\" selectedDataType=\"" + selectedDataType + "\" hbMeasType=\"" + hbMeasType + "\" selectedTimeStep=\"" +
        selectedTimeStep + "\"" );
    for ( InputFilter_JPanel panel : inputFilterJPanelList ) {
        matched = false; // Does selected data store name match the filter data store & does data type match
        if ( panel instanceof HydroBase_GUI_StationGeolocMeasType_InputFilter_JPanel ) {
            // Station time series
            HydroBase_GUI_StationGeolocMeasType_InputFilter_JPanel hbpanel =
                (HydroBase_GUI_StationGeolocMeasType_InputFilter_JPanel)panel;
            hbdmi = (HydroBaseDMI)hbpanel.getDataStore().getDMI();
            if ( hbpanel.getDataStore().getName().equalsIgnoreCase(dataStoreName) ) {
                if ( HydroBase_Util.isGroundWaterWellTimeSeriesDataType ( hbdmi, hbMeasType) &&
                    selectedTimeStep.equalsIgnoreCase("Irregular")) {
                    matched = true;
                    Message.printStatus(2, routine, "Setting station (groundwater) input filter panel visible.");
                }
                else if ( HydroBase_Util.isStationTimeSeriesDataType ( hbdmi, hbMeasType) ) {
                    matched = true;
                    Message.printStatus(2, routine, "Setting station input filter panel visible.");
                }
            }
        }
        else if ( panel instanceof HydroBase_GUI_StructureGeolocStructMeasType_InputFilter_JPanel ) {
            HydroBase_GUI_StructureGeolocStructMeasType_InputFilter_JPanel hbpanel =
                (HydroBase_GUI_StructureGeolocStructMeasType_InputFilter_JPanel)panel;
            hbdmi = (HydroBaseDMI)hbpanel.getDataStore().getDMI();
            if ( hbpanel.getDataStore().getName().equalsIgnoreCase(dataStoreName) ) {
                //Message.printStatus(2, routine, "Panel includeSFUT=" + hbpanel.getIncludeSFUT());
                if ( !hbpanel.getIncludeSFUT() && HydroBase_Util.isStructureTimeSeriesDataType ( hbdmi, hbMeasType) &&
                    !HydroBase_Util.isStructureSFUTTimeSeriesDataType ( hbdmi, hbMeasType) ) {
                    // Normal structure time series (not SFUT)
                    matched = true;
                    Message.printStatus(2, routine, "Setting structure (no SFUT) input filter panel visible.");
                }
                else if ( hbpanel.getIncludeSFUT() && HydroBase_Util.isStructureSFUTTimeSeriesDataType ( hbdmi, hbMeasType) ) {
                    // SFUT time series
                    matched = true;
                    Message.printStatus(2, routine, "Setting structure SFUT input filter panel visible.");
                }
            }
        }
        else if ( panel instanceof HydroBase_GUI_AgriculturalCASSCropStats_InputFilter_JPanel ) {
            // CASS crop time series
            HydroBase_GUI_AgriculturalCASSCropStats_InputFilter_JPanel hbpanel =
                (HydroBase_GUI_AgriculturalCASSCropStats_InputFilter_JPanel)panel;
            hbdmi = (HydroBaseDMI)hbpanel.getDataStore().getDMI();
            if (hbpanel.getDataStore().getName().equalsIgnoreCase(dataStoreName) &&
                HydroBase_Util.isAgriculturalCASSCropStatsTimeSeriesDataType ( hbdmi, hbMeasType) ) {
                matched = true;
                Message.printStatus(2, routine, "Setting CASS crop stats input filter panel visible.");
            }
        }
        else if ( panel instanceof HydroBase_GUI_AgriculturalCASSLivestockStats_InputFilter_JPanel ) {
            // CASS livestock time series
            HydroBase_GUI_AgriculturalCASSLivestockStats_InputFilter_JPanel hbpanel =
                (HydroBase_GUI_AgriculturalCASSLivestockStats_InputFilter_JPanel)panel;
            hbdmi = (HydroBaseDMI)hbpanel.getDataStore().getDMI();
            if (hbpanel.getDataStore().getName().equalsIgnoreCase(dataStoreName) &&
                HydroBase_Util.isAgriculturalCASSLivestockStatsTimeSeriesDataType ( hbdmi, hbMeasType) ) {
                matched = true;
                Message.printStatus(2, routine, "Setting CASS livestock stats input filter panel visible.");
            }
        }
        else if ( panel instanceof HydroBase_GUI_CUPopulation_InputFilter_JPanel ) {
            // CU population time series
            HydroBase_GUI_CUPopulation_InputFilter_JPanel hbpanel =
                (HydroBase_GUI_CUPopulation_InputFilter_JPanel)panel;
            hbdmi = (HydroBaseDMI)hbpanel.getDataStore().getDMI();
            if (hbpanel.getDataStore().getName().equalsIgnoreCase(dataStoreName) &&
                HydroBase_Util.isCUPopulationTimeSeriesDataType ( hbdmi, hbMeasType) ) {
                matched = true;
                Message.printStatus(2, routine, "Setting CU population input filter panel visible.");
            }
        }
        else if ( panel instanceof HydroBase_GUI_AgriculturalNASSCropStats_InputFilter_JPanel ) {
            // NASS time series
            HydroBase_GUI_AgriculturalNASSCropStats_InputFilter_JPanel hbpanel =
                (HydroBase_GUI_AgriculturalNASSCropStats_InputFilter_JPanel)panel;
            hbdmi = (HydroBaseDMI)hbpanel.getDataStore().getDMI();
            if (hbpanel.getDataStore().getName().equalsIgnoreCase(dataStoreName) &&
                HydroBase_Util.isAgriculturalNASSCropStatsTimeSeriesDataType ( hbdmi, hbMeasType) ) {
                matched = true;
                Message.printStatus(2, routine, "Setting NASS crop stats input filter panel visible.");
            }
        }
        else if ( panel instanceof HydroBase_GUI_StructureIrrigSummaryTS_InputFilter_JPanel ) {
            // Irrig summary time series
            HydroBase_GUI_StructureIrrigSummaryTS_InputFilter_JPanel hbpanel =
                (HydroBase_GUI_StructureIrrigSummaryTS_InputFilter_JPanel)panel;
            hbdmi = (HydroBaseDMI)hbpanel.getDataStore().getDMI();
            if (hbpanel.getDataStore().getName().equalsIgnoreCase(dataStoreName) &&
                HydroBase_Util.isIrrigSummaryTimeSeriesDataType ( hbdmi, hbMeasType) ) {
                matched = true;
                Message.printStatus(2, routine, "Setting irrig summary input filter panel visible.");
            }
        }
        else if ( panel instanceof HydroBase_GUI_GroundWater_InputFilter_JPanel ) {
            // Groundwater wells time series
            // Note that irregular data are matched under the station time series above
            HydroBase_GUI_GroundWater_InputFilter_JPanel hbpanel =
                (HydroBase_GUI_GroundWater_InputFilter_JPanel)panel;
            hbdmi = (HydroBaseDMI)hbpanel.getDataStore().getDMI();
            if (hbpanel.getDataStore().getName().equalsIgnoreCase(dataStoreName) &&
                HydroBase_Util.isGroundWaterWellTimeSeriesDataType ( hbdmi, hbMeasType) ) {
                matched = true;
                Message.printStatus(2, routine, "Setting groundwater wells input filter panel visible.");
            }
        }
        // If the panel was matched, set it visible...
        panel.setVisible(matched);
        if ( matched ) {
            ++matchCount;
        }
    }
    // No normal panels were matched enable the generic panel, which will be last panel in list
    InputFilter_JPanel defaultPanel = inputFilterJPanelList.get(inputFilterJPanelList.size() - 1);
    if ( matchCount == 0 ) {
        defaultPanel.setVisible(true);
        Message.printStatus(2, routine, "Setting default input filter panel visible.");
    }
    else {
        defaultPanel.setVisible(false);
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
    String inputNameString = __InputName_JComboBox.getSelected();
    if ( inputNameString == null ) {
        inputNameString = "";
    }
    if ( !dataStoreString.equals("") ) {
        // Use the selected data store
        __dataStore = getSelectedDataStore();
    }
    else {
        // Use the DMI from the input name
        __dataStore = getSelectedDataStoreFromInputName();
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