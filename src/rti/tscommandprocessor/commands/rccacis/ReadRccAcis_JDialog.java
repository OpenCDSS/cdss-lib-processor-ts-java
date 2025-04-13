// ReadRccAcis_JDialog extends JDialog - Editor for he ReadRccAcis() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2025 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.rccacis;

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
import java.util.Vector;

import javax.swing.BorderFactory;
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
import rti.tscommandprocessor.core.TSCommandProcessor;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

/**
Editor for he ReadRccAcis() command.
*/
@SuppressWarnings("serial")
public class ReadRccAcis_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private ReadRccAcis_Command __command = null;
private SimpleJComboBox __DataStore_JComboBox = null;
private SimpleJComboBox __DataType_JComboBox = null;
private SimpleJComboBox __Interval_JComboBox = null;
private JTextField __SiteID_JTextField;
private JTextField __InputStart_JTextField;
private JTextField __InputEnd_JTextField;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private JTabbedPane __tsInfo_JTabbedPane = null;

private JTextArea __command_JTextArea = null;
private InputFilter_JPanel __inputFilter_JPanel =null;
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK was pressed when closing the dialog.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadRccAcis_JDialog ( JFrame parent, ReadRccAcis_Command command ) {
	super(parent, true);

	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event ) {
	Object o = event.getSource();

	if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "ReadRccAcis");
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
public void changedUpdate ( DocumentEvent e ) {
    checkGUIState();
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void insertUpdate ( DocumentEvent e ) {
    checkGUIState();
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void removeUpdate ( DocumentEvent e ) {
    checkGUIState();
    refresh();
}

// ...End event handlers for DocumentListener.

/**
Check the state of the dialog, disabling/enabling components as appropriate.
*/
private void checkGUIState() {
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag to true.
This should be called before response() is allowed to complete.
*/
private void checkInput () {
	// Put together a list of parameters to check.
	PropList props = new PropList ( "" );
	__error_wait = false;
	String DataStore = __DataStore_JComboBox.getSelected();
	if ( DataStore.length() > 0 ) {
		props.set ( "DataStore", DataStore );
	}
	String DataType = StringUtil.getToken(__DataType_JComboBox.getSelected().trim(), " ", 0, 0 );
    if ( DataType.length() > 0 ) {
        props.set ( "DataType", DataType );
    }
    String Interval = __Interval_JComboBox.getSelected();
    if ( Interval.length() > 0 ) {
        props.set ( "Interval", Interval );
    }
    String SiteID = __SiteID_JTextField.getText().trim();
    if ( SiteID.length() > 0 ) {
        props.set ( "SiteID", SiteID );
    }
	int numWhere = __inputFilter_JPanel.getNumFilterGroups();
	for ( int i = 1; i <= numWhere; i++ ) {
	    String where = getWhere ( i - 1 );
	    if ( where.length() > 0 ) {
	        props.set ( "Where" + i, where );
	    }
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
	    // This will warn the user.
		__command.checkCommandParameters ( props, null, 1 );
	}
	catch ( Exception e ) {
		// The warning would have been printed in the check code.
		__error_wait = true;
	}
}

/**
Commit the edits to the command.
In this case the command parameters have already been checked and no errors were detected.
*/
private void commitEdits () {
	String DataStore = __DataStore_JComboBox.getSelected();
    String DataType = StringUtil.getToken(__DataType_JComboBox.getSelected().trim(), " ", 0, 0 );
    String Interval = __Interval_JComboBox.getSelected();
	__command.setCommandParameter ( "DataStore", DataStore );
	__command.setCommandParameter ( "DataType", DataType );
	__command.setCommandParameter ( "Interval", Interval );
    String SiteID = __SiteID_JTextField.getText().trim();
    __command.setCommandParameter ( "SiteID", SiteID );
	String delim = ";";
	int numWhere = __inputFilter_JPanel.getNumFilterGroups();
	for ( int i = 1; i <= numWhere; i++ ) {
	    String where = getWhere ( i - 1 );
	    if ( where.startsWith(delim) ) {
	        where = "";
	    }
	    __command.setCommandParameter ( "Where" + i, where );
	}
	String InputStart = __InputStart_JTextField.getText().trim();
	__command.setCommandParameter ( "InputStart", InputStart );
	String InputEnd = __InputEnd_JTextField.getText().trim();
	__command.setCommandParameter ( "InputEnd", InputEnd );
	String Alias = __Alias_JTextField.getText().trim();
    __command.setCommandParameter ( "Alias", Alias );
}

/**
Get the selected data store.
*/
private RccAcisDataStore getSelectedDataStore () {
    String routine = getClass().getName() + ".getSelectedDataStore";
    String DataStore = __DataStore_JComboBox.getSelected();
    RccAcisDataStore dataStore = (RccAcisDataStore)((TSCommandProcessor)
        __command.getCommandProcessor()).getDataStoreForName(
        DataStore, RccAcisDataStore.class );
    if ( dataStore == null ) {
        Message.printStatus(2, routine, "Selected data store is \"" + DataStore + "\"." );
    }
    return dataStore;
}

/**
Return the "WhereN" parameter for the requested input filter.
@return the "WhereN" parameter for the requested input filter.
@param ifg the Input filter to process (zero index).
*/
private String getWhere ( int ifg ) {
	// TODO SAM 2006-04-24 Need to enable other input filter panels.
	String delim = ";";	// To separate input filter parts.
	InputFilter_JPanel filter_panel = __inputFilter_JPanel;
	String where = filter_panel.toString(ifg,delim).trim();
	return where;
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, ReadRccAcis_Command command ) {
	String routine = getClass().getSimpleName() + ".initialize";
	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Read one or more time series from the RCC ACIS web service."),
    	0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "<html><b>WARNING - This command can be slow.  " +
        "It is recommended that the Where filters be used to limit queries when reading multiple time series.</b></html>"),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Refer to the RCC ACIS Data Store documentation for more information." ),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"If not specified, the input period defaults to the input period from SetInputPeriod()."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

   	// List available data stores of the correct type.

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data store:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStore_JComboBox = new SimpleJComboBox ( false );
    TSCommandProcessor tsProcessor = (TSCommandProcessor)processor;
    List<DataStore> dataStoreList = tsProcessor.getDataStoresByType( RccAcisDataStore.class );
    List<String> datastoreChoices = new ArrayList<String>();
    for ( DataStore dataStore: dataStoreList ) {
    	datastoreChoices.add ( dataStore.getName() );
    }
    __DataStore_JComboBox.setData(datastoreChoices);
    __DataStore_JComboBox.select ( 0 );
    __DataStore_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataStore_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - data store containing data."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Data types are particular to the data store.

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data type:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataType_JComboBox = new SimpleJComboBox ( false );
    setDataTypeChoices();
    __DataType_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataType_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - data type for time series."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Intervals are hard-coded.

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data interval:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Interval_JComboBox = new SimpleJComboBox ( false );
    setIntervalChoices();
    __Interval_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Interval_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - data interval (time step) for time series."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __tsInfo_JTabbedPane = new JTabbedPane ();
    __tsInfo_JTabbedPane.setBorder(
        BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
        "Indicate how to match time series in ACIS" ));
    JGUIUtil.addComponent(main_JPanel, __tsInfo_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JPanel singleTS_JPanel = new JPanel();
    singleTS_JPanel.setLayout(new GridBagLayout());
    __tsInfo_JTabbedPane.addTab ( "Match Single Time Series", singleTS_JPanel );

    int ySingle = -1;
    JGUIUtil.addComponent(singleTS_JPanel,
        new JLabel ("Specify a site ID when a specific time series is being processed."),
        0, ++ySingle, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ("Site ID:"),
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SiteID_JTextField = new JTextField (20);
    __SiteID_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(singleTS_JPanel, __SiteID_JTextField,
        1, ySingle, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel,
        new JLabel ("Required - site type (optional) and identifier (e.g., COOP:052454)."),
        3, ySingle, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JPanel multipleTS_JPanel = new JPanel();
    multipleTS_JPanel.setLayout(new GridBagLayout());
    __tsInfo_JTabbedPane.addTab ( "Match 1+ Time Series Using Filter", multipleTS_JPanel );

    int yMultiple = -1;

   	// Input filters
    // TODO SAM 2010-11-02 Need to use SetInputFilters() so the filters can change when a data store is selected.
    // For now it is OK because the input filters do not provide choices.

	int buffer = 3;
	Insets insets = new Insets(0,buffer,0,0);
	try {
	    JGUIUtil.addComponent(multipleTS_JPanel,
            new JLabel ("Specify filters when multiple time series are being processed (a location constraint must be specified)."),
            0, ++yMultiple, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	    // Add input filters for RCC-ACIS time series.
		__inputFilter_JPanel = new RccAcis_TimeSeries_InputFilter_JPanel(
		    getSelectedDataStore(), __command.getNumFilterGroups() );
		JGUIUtil.addComponent(multipleTS_JPanel, __inputFilter_JPanel,
			0, ++yMultiple, 3, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST );
   		__inputFilter_JPanel.addEventListeners ( this );
   	    JGUIUtil.addComponent(multipleTS_JPanel, new JLabel ( "Optional - query filters."),
   	        3, yMultiple, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	}
	catch ( Exception e ) {
		Message.printWarning ( 2, routine, "Unable to initialize RCC ACIS input filter." );
		Message.printWarning ( 2, routine, e );
	}

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Input start:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputStart_JTextField = new JTextField (20);
    __InputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputStart_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - YYYY-MM-DD, override the global input start."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input end:"),
        0, ++y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputEnd_JTextField = new JTextField (20);
    __InputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputEnd_JTextField,
        1, y, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - YYYY-MM-DD, override the global input end."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

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

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (4,50);
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	// Refresh the contents.
	refresh ();

	// Panel for buttons.
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

    pack();
    JGUIUtil.center( this );
	refresh();	// Sets the __path_JButton status.
	// Dialogs do not need to be resizable.
	setResizable ( false );
    super.setVisible( true );
}

/**
Handle ItemListener events.
*/
public void itemStateChanged ( ItemEvent event ) {
    // If a new data store has been selected, update the data type, interval, list and the input filter.
    if ( event.getSource() == __DataStore_JComboBox ) {
        setDataTypeChoices();
        setIntervalChoices();
        setInputFilters();
    }
    refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event ) {
	refresh();
}

/**
Need this to properly capture key events, especially deletes.
*/
public void keyReleased ( KeyEvent event ) {
	refresh();
}

public void keyTyped ( KeyEvent event ) {
}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user canceled.
*/
public boolean ok () {
	return __ok;
}

/**
Refresh the command string from the dialog contents.
*/
private void refresh () {
	String routine = getClass().getSimpleName() + ".refresh";
	__error_wait = false;
	String DataStore = "";
	String DataType = "";
	String Interval = "";
	String SiteID = "";
	String filter_delim = ";";
	String InputStart = "";
	String InputEnd = "";
	String Alias = "";
	PropList props = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command.
		props = __command.getCommandParameters();
		DataStore = props.getValue ( "DataStore" );
		DataType = props.getValue ( "DataType" );
		Interval = props.getValue ( "Interval" );
		SiteID = props.getValue ( "SiteID" );
		InputStart = props.getValue ( "InputStart" );
		InputEnd = props.getValue ( "InputEnd" );
		Alias = props.getValue ( "Alias" );
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataStore_JComboBox, DataStore, JGUIUtil.NONE, null, null ) ) {
            __DataStore_JComboBox.select ( DataStore );
        }
        else {
            if ( (DataStore == null) || DataStore.equals("") ) {
                // New command...select the default.
                __DataStore_JComboBox.select ( 0 );
            }
            else {
                // Bad user command.
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataStore parameter \"" + DataStore + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        // Data types as displayed are verbose:
        // "4 - Precipitation (daily)" but parameter uses only "4" to ensure uniqueness.
        // Therefore, select in the list based only on the first token.
        int [] index = new int[1];
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataType_JComboBox, DataType, JGUIUtil.CHECK_SUBSTRINGS, " ", 0, index, true ) ) {
            __DataType_JComboBox.select ( index[0] );
        }
        else {
            if ( (DataType == null) || DataType.equals("") ) {
                // New command...select the default.
                __DataType_JComboBox.select ( 0 );
            }
            else {
                // Bad user command.
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataType parameter \"" + DataType + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(__Interval_JComboBox, Interval, JGUIUtil.NONE, null, null ) ) {
            __Interval_JComboBox.select ( Interval );
        }
        else {
            if ( (Interval == null) || Interval.equals("") ) {
                // New command...select the default.
                __Interval_JComboBox.select ( 0 );
            }
            else {
                // Bad user command.
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "Interval parameter \"" + Interval + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        InputFilter_JPanel filter_panel = __inputFilter_JPanel;
        int nfg = filter_panel.getNumFilterGroups();
        String where;
        int numSet = 0;
        for ( int ifg = 0; ifg < nfg; ifg ++ ) {
            where = props.getValue ( "Where" + (ifg + 1) );
            if ( (where != null) && (where.length() > 0) ) {
                // Set the filter.
                try {
                    filter_panel.setInputFilter (ifg, where, filter_delim );
                    ++numSet;
                }
                catch ( Exception e ) {
                    Message.printWarning ( 1, routine, "Error setting where information using \"" + where + "\"" );
                    Message.printWarning ( 3, routine, e );
                }
            }
        }
        if ( numSet > 0 ) {
            __tsInfo_JTabbedPane.setSelectedIndex(1);
        }
        // Put this after filters because if SiteID is specified the tab should be shown.
        if ( SiteID != null ) {
            __SiteID_JTextField.setText ( SiteID );
            if ( !SiteID.equals("") ) {
                __tsInfo_JTabbedPane.setSelectedIndex(0);
            }
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
	// Regardless, reset the command from the fields.
	Alias = __Alias_JTextField.getText().trim();
	// Regardless, reset the command from the fields.
	props = new PropList ( __command.getCommandName() );
	DataStore = __DataStore_JComboBox.getSelected().trim();
	// Only save the major variable number because parentheses cause problems in properties.
	DataType = StringUtil.getToken(__DataType_JComboBox.getSelected().trim(), " ", 0, 0 );
	Interval = __Interval_JComboBox.getSelected().trim();
    props.add ( "DataStore=" + DataStore );
    props.add ( "DataType=" + DataType );
    props.add ( "Interval=" + Interval );
    SiteID = __SiteID_JTextField.getText().trim();
    props.add ( "SiteID=" + SiteID );
	// Add the where clause(s).
	InputFilter_JPanel filter_panel = __inputFilter_JPanel;
	int nfg = filter_panel.getNumFilterGroups();
	String where;
	String delim = ";";	// To separate input filter parts.
	for ( int ifg = 0; ifg < nfg; ifg ++ ) {
		where = filter_panel.toString(ifg,delim).trim();
		// Make sure there is a field that is being checked in a where clause.
		if ( (where.length() > 0) && !where.startsWith(delim) ) {
		    // FIXME SAM 2010-11-01 The following discards '=' in the quoted string.
			//props.add ( "Where" + (ifg + 1) + "=" + where );
			props.set ( "Where" + (ifg + 1), where );
		}
	}
	InputStart = __InputStart_JTextField.getText().trim();
	props.add ( "InputStart=" + InputStart );
	InputEnd = __InputEnd_JTextField.getText().trim();
	props.add ( "InputEnd=" + InputEnd );
	props.add ( "Alias=" + Alias );
	__command_JTextArea.setText( __command.toString ( props ).trim() );

	// Check the GUI state to determine whether some controls should be disabled.

	checkGUIState();
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
*/
private void response ( boolean ok ) {
	__ok = ok;	// Save to be returned by ok().
	if ( ok ) {
		// Commit the changes.
		commitEdits ();
		if ( __error_wait ) {
			// Not ready to close out.
			return;
		}
	}
	// Now close out.
	setVisible( false );
	dispose();
}

/**
Set the data type choices in response to a new data store being selected.
*/
private void setDataTypeChoices () {
    String routine = getClass().getName() + ".setDataTypeChoices";
    RccAcisDataStore ds = getSelectedDataStore();
    List<String> dataTypes = new Vector<String>();
    try {
        dataTypes = ds.getDataTypeStrings ( true, true );
    }
    catch ( Exception e ) {
        // Hopefully should not happen.
        Message.printWarning(2, routine, "Unable to get data types for data store \"" +
            ds.getName() + "\" - web service unavailable?");
    }
    __DataType_JComboBox.setData ( dataTypes );
    __DataType_JComboBox.select ( 0 );
}

/**
Set the input filters in response to a new data store being selected.
*/
private void setInputFilters () {
}

/**
Set the data interval choices in response to a new data store being selected.
*/
private void setIntervalChoices () {
    __Interval_JComboBox.removeAll();
    __Interval_JComboBox.add ( "Day" );
    __Interval_JComboBox.select ( 0 );
}

/**
Responds to WindowEvents.
@param event WindowEvent object
*/
public void windowClosing( WindowEvent event ) {
	response ( false );
}

public void windowActivated( WindowEvent evt ) {
}

public void windowClosed( WindowEvent evt ) {
}

public void windowDeactivated( WindowEvent evt ) {
}

public void windowDeiconified( WindowEvent evt ) {
}

public void windowIconified( WindowEvent evt ) {
}

public void windowOpened( WindowEvent evt ) {
}

}