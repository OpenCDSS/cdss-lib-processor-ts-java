// CreateNetworkFromTable_JDialog - editor for CreateNetworkFromTable command

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

package rti.tscommandprocessor.commands.network;

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

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

@SuppressWarnings("serial")
public class CreateNetworkFromTable_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{

private boolean __error_wait = false; // To track errors
private boolean __first_time = true; // Indicate first time display
private JTextArea __command_JTextArea = null;
private JTextField __NetworkID_JTextField = null;
private JTextField __NetworkName_JTextField = null;
private JTextField __DefaultDownstreamNodeID_JTextField = null;
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __NodeIDColumn_JTextField = null;
private JTextField __NodeNameColumn_JTextField = null;
private JTextField __NodeTypeColumn_JTextField = null;
private JTextField __NodeGroupColumn_JTextField = null;
private JTextField __NodeDistanceColumn_JTextField = null;
private JTextField __NodeWeightColumn_JTextField = null;
private JTextField __DownstreamNodeIDColumn_JTextField = null;
private JTextField __NodeAddTypes_JTextField = null;
private JTextField __NodeAddDataTypes_JTextField = null;
private JTextField __NodeSubtractTypes_JTextField = null;
private JTextField __NodeSubtractDataTypes_JTextField = null;
private JTextField __NodeOutflowTypes_JTextField = null;
private JTextField __NodeOutflowDataTypes_JTextField = null;
private JTextField __NodeFlowThroughTypes_JTextField = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private CreateNetworkFromTable_Command __command = null;
private boolean __ok = false;

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of table identifiers to provide as choices
*/
public CreateNetworkFromTable_JDialog ( JFrame parent, CreateNetworkFromTable_Command command, List<String> tableIDChoices )
{	super(parent, true);
	initialize ( parent, command, tableIDChoices );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed(ActionEvent event)
{	Object o = event.getSource();

    if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "CreateNetworkFromTable");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput ();
		if ( !__error_wait ) {
			// Command has been edited...
			response ( true );
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
	String NetworkID = __NetworkID_JTextField.getText().trim();
	String NetworkName = __NetworkName_JTextField.getText().trim();
	String DefaultDownstreamNodeID = __DefaultDownstreamNodeID_JTextField.getText().trim();
	String TableID = __TableID_JComboBox.getSelected();
	String NodeIDColumn = __NodeIDColumn_JTextField.getText().trim();
	String NodeNameColumn = __NodeNameColumn_JTextField.getText().trim();
	String NodeTypeColumn = __NodeTypeColumn_JTextField.getText().trim();
	String NodeGroupColumn = __NodeGroupColumn_JTextField.getText().trim();
	String NodeDistanceColumn = __NodeDistanceColumn_JTextField.getText().trim();
	String NodeWeightColumn = __NodeWeightColumn_JTextField.getText().trim();
	String DownstreamNodeIDColumn = __DownstreamNodeIDColumn_JTextField.getText().trim();
	String NodeAddTypes = __NodeAddTypes_JTextField.getText().trim();
	String NodeAddDataTypes = __NodeAddDataTypes_JTextField.getText().trim();
	String NodeSubtractTypes = __NodeSubtractTypes_JTextField.getText().trim();
	String NodeSubtractDataTypes = __NodeSubtractTypes_JTextField.getText().trim();
	String NodeOutflowTypes = __NodeOutflowTypes_JTextField.getText().trim();
	String NodeOutflowDataTypes = __NodeOutflowDataTypes_JTextField.getText().trim();
	String NodeFlowThroughTypes = __NodeFlowThroughTypes_JTextField.getText().trim();
	__error_wait = false;

    if ( NetworkID.length() > 0 ) {
        props.set ( "NetworkID", NetworkID );
    }
    if ( NetworkName.length() > 0 ) {
        props.set ( "NetworkName", NetworkName );
    }
    if ( DefaultDownstreamNodeID.length() > 0 ) {
        props.set ( "DefaultDownstreamNodeID", DefaultDownstreamNodeID );
    }
    if ( TableID.length() > 0 ) {
        props.set ( "TableID", TableID );
    }
    if ( NodeIDColumn.length() > 0 ) {
        props.set ( "NodeIDColumn", NodeIDColumn );
    }
    if ( NodeNameColumn.length() > 0 ) {
        props.set ( "NodeNameColumn", NodeNameColumn );
    }
    if ( NodeTypeColumn.length() > 0 ) {
        props.set ( "NodeTypeColumn", NodeTypeColumn );
    }
    if ( NodeGroupColumn.length() > 0 ) {
        props.set ( "NodeGroupColumn", NodeGroupColumn );
    }
    if ( NodeDistanceColumn.length() > 0 ) {
        props.set ( "NodeDistanceColumn", NodeDistanceColumn );
    }
    if ( NodeWeightColumn.length() > 0 ) {
        props.set ( "NodeWeightColumn", NodeWeightColumn );
    }
    if ( DownstreamNodeIDColumn.length() > 0 ) {
        props.set ( "DownstreamNodeIDColumn", DownstreamNodeIDColumn );
    }
	if ( NodeAddTypes.length() > 0 ) {
		props.set ( "NodeAddTypes", NodeAddTypes );
	}
    if ( NodeAddDataTypes.length() > 0 ) {
        props.set ( "NodeAddDataTypes", NodeAddDataTypes );
    }
    if ( NodeSubtractTypes.length() > 0 ) {
        props.set ( "NodeSubtractTypes", NodeSubtractTypes );
    }
    if ( NodeSubtractDataTypes.length() > 0 ) {
        props.set ( "NodeSubtractDataTypes", NodeSubtractDataTypes );
    }
    if ( NodeOutflowTypes.length() > 0 ) {
        props.set ( "NodeOutflowTypes", NodeOutflowTypes );
    }
    if ( NodeOutflowDataTypes.length() > 0 ) {
        props.set ( "NodeOutflowDataTypes", NodeOutflowDataTypes );
    }
    if ( NodeFlowThroughTypes.length() > 0 ) {
        props.set ( "NodeFlowThroughTypes", NodeFlowThroughTypes );
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
{	String NetworkID = __NetworkID_JTextField.getText().trim();
	String NetworkName = __NetworkName_JTextField.getText().trim();
	String DefaultDownstreamNodeID = __DefaultDownstreamNodeID_JTextField.getText().trim();
	String TableID = __TableID_JComboBox.getSelected();
    String NodeIDColumn = __NodeIDColumn_JTextField.getText().trim();
    String NodeNameColumn = __NodeNameColumn_JTextField.getText().trim();
    String NodeTypeColumn = __NodeTypeColumn_JTextField.getText().trim();
    String NodeGroupColumn = __NodeGroupColumn_JTextField.getText().trim();
    String NodeDistanceColumn = __NodeDistanceColumn_JTextField.getText().trim();
    String NodeWeightColumn = __NodeWeightColumn_JTextField.getText().trim();
    String DownstreamNodeIDColumn = __DownstreamNodeIDColumn_JTextField.getText().trim();
    String NodeAddTypes = __NodeAddTypes_JTextField.getText().trim();
    String NodeAddDataTypes = __NodeAddDataTypes_JTextField.getText().trim();
    String NodeSubtractTypes = __NodeSubtractTypes_JTextField.getText().trim();
    String NodeSubtractDataTypes = __NodeSubtractDataTypes_JTextField.getText().trim();
    String NodeOutflowTypes = __NodeOutflowTypes_JTextField.getText().trim();
    String NodeOutflowDataTypes = __NodeOutflowDataTypes_JTextField.getText().trim();
    String NodeFlowThroughTypes = __NodeFlowThroughTypes_JTextField.getText().trim();
    __command.setCommandParameter ( "NetworkID", NetworkID );
    __command.setCommandParameter ( "NetworkName", NetworkName );
    __command.setCommandParameter ( "DefaultDownstreamNodeID", DefaultDownstreamNodeID );
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "NodeIDColumn", NodeIDColumn );
    __command.setCommandParameter ( "NodeNameColumn", NodeNameColumn );
    __command.setCommandParameter ( "NodeTypeColumn", NodeTypeColumn );
    __command.setCommandParameter ( "NodeGroupColumn", NodeGroupColumn );
    __command.setCommandParameter ( "NodeDistanceColumn", NodeDistanceColumn );
    __command.setCommandParameter ( "NodeWeightColumn", NodeWeightColumn );
    __command.setCommandParameter ( "DownstreamNodeIDColumn", DownstreamNodeIDColumn );
	__command.setCommandParameter ( "NodeAddTypes", NodeAddTypes );
	__command.setCommandParameter ( "NodeAddDataTypes", NodeAddDataTypes );
	__command.setCommandParameter ( "NodeSubtractTypes", NodeSubtractTypes );
	__command.setCommandParameter ( "NodeSubtractDataTypes", NodeSubtractDataTypes );
    __command.setCommandParameter ( "NodeOutflowTypes", NodeOutflowTypes );
    __command.setCommandParameter ( "NodeOutflowDataTypes", NodeOutflowDataTypes );
    __command.setCommandParameter ( "NodeFlowThroughTypes", NodeFlowThroughTypes );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit and possibly run.
*/
private void initialize ( JFrame parent, CreateNetworkFromTable_Command command, List<String> tableIDChoices )
{	__command = command;

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
        "This command creates a node network from a table.  Each network node has one \"downstream\" node but can have multiple \"upstream\" nodes."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(paragraph, new JLabel (
        "A node network typically represents points (e.g., stations) or areas (e.g., basins)."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "The network can be used for simple modeling such as mass balance.  See also the AnalyzeNetworkPointFlow() command."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "The network can also be used for data model navigation and to select time series for further processing, for example select time series for nodes upstream."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "<html><b>Command features are experimental.  Initial features are intended to provide basic network initialization and support data navigation.</b></html>."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "<html><b>Features of this command and other network commands, including AnalyzeNetworkPointFlow() will be integrated in the future.</b></html>."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	JGUIUtil.addComponent(main_JPanel, paragraph,
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
    // Tabbed pane for parameters
	 
    JTabbedPane main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel to specify general data
    
    JPanel gen_JPanel = new JPanel ();
    gen_JPanel.setLayout( new GridBagLayout() );
    int yGen = -1;
    JGUIUtil.addComponent(main_JPanel, gen_JPanel,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    main_JTabbedPane.addTab ( "General", gen_JPanel );
    
    JGUIUtil.addComponent(gen_JPanel, new JLabel (
        "Specify general information about the network that is created."),
        0, ++yGen, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(gen_JPanel, new JLabel (
        "There must be one downstream node that is the endpoint of the basin.  Use the DefaultDownstreamNodeID to provide an identifier for this node."),
        0, ++yGen, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(gen_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yGen, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(gen_JPanel, new JLabel ("Network ID:"), 
        0, ++yGen, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NetworkID_JTextField = new JTextField (15);
    __NetworkID_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(gen_JPanel, __NetworkID_JTextField,
        1, yGen, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(gen_JPanel, new JLabel ("Required - network identifier."),
        3, yGen, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(gen_JPanel, new JLabel ("Network name:"), 
        0, ++yGen, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NetworkName_JTextField = new JTextField (10);
    __NetworkName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(gen_JPanel, __NetworkName_JTextField,
        1, yGen, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(gen_JPanel, new JLabel ("Required - network name."),
        3, yGen, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(gen_JPanel, new JLabel ("Default downstream node ID:"), 
        0, ++yGen, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DefaultDownstreamNodeID_JTextField = new JTextField (15);
    __DefaultDownstreamNodeID_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(gen_JPanel, __DefaultDownstreamNodeID_JTextField,
        1, yGen, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(gen_JPanel, new JLabel ("Required - needed to allow network navigation."),
        3, yGen, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel to map table columns to network definition

    JPanel table_JPanel = new JPanel ();
    table_JPanel.setLayout( new GridBagLayout() );
    int yTable = -1;
    JGUIUtil.addComponent(main_JPanel, table_JPanel,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    main_JTabbedPane.addTab ( "Map Table Columns to Network Nodes", table_JPanel );
    JGUIUtil.addComponent(table_JPanel, new JLabel (
        "Specify table columns that provide data to define the network:"),
        0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel (
        "   NodeID - is typically a location identifier (and is used to match time series in SelectTimeSeries())"),
        0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel (
        "   NodeType - is used to define analysis behavior (see \"Define Node Type Behavior\" tab) - currently not used"),
        0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel (
        "   NodeDistance and NodeWeight - are used to distribute reach gain/loss back to nodes in the reach - currently not used"),
        0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table ID:" ), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true );    // Allow edit
    tableIDChoices.add(0,""); // Add blank to ignore table
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(table_JPanel, __TableID_JComboBox,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel( "Required - table containing network node information."), 
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(table_JPanel, new JLabel ("Node ID column:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NodeIDColumn_JTextField = new JTextField (10);
    __NodeIDColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __NodeIDColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Required - column name for node IDs."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Node name column:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NodeNameColumn_JTextField = new JTextField (10);
    __NodeNameColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __NodeNameColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Optional - column name for node names."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Node type column:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NodeTypeColumn_JTextField = new JTextField (10);
    __NodeTypeColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __NodeTypeColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Required - column name for node types."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Node group column:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NodeGroupColumn_JTextField = new JTextField (10);
    __NodeGroupColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __NodeGroupColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Optional - for example identifier for stream or larger basin."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Node distance column:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NodeDistanceColumn_JTextField = new JTextField (10);
    __NodeDistanceColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __NodeDistanceColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Optional - used if GainMethod requires distance."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Node weight column:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NodeWeightColumn_JTextField = new JTextField (10);
    __NodeWeightColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __NodeWeightColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Optional - used if GainMethod requires weight."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Downstream node ID column:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DownstreamNodeIDColumn_JTextField = new JTextField (10);
    __DownstreamNodeIDColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __DownstreamNodeIDColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Required - column name for downstream node IDs."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JPanel nodeType_JPanel = new JPanel ();
    nodeType_JPanel.setLayout( new GridBagLayout() );
    int yNodeType = -1;
    main_JTabbedPane.addTab ( "Define Node Type Behavior", nodeType_JPanel );
    JGUIUtil.addComponent(nodeType_JPanel, new JLabel (
        "<html><b>These parameters are not currently enabled and may be removed.</b></html>."),
        0, ++yNodeType, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(nodeType_JPanel, new JLabel (
        "Specify node type behavior for the point flow analysis.  Each node type indicates how mass balance is calculated for the type."),
        0, ++yNodeType, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(nodeType_JPanel, new JLabel ("Time series for each node by default are matched as follows " +
        "(however, specifying TSID/alias via the \"TSID/Alias\" tab is recommended):"),
        0, ++yNodeType, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(nodeType_JPanel, new JLabel ("    Location ID - match Node ID column"),
        0, ++yNodeType, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(nodeType_JPanel, new JLabel ("    Data source - currently not matched"),
        0, ++yNodeType, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(nodeType_JPanel, new JLabel ("    Data type - match data types listed below, " +
        "specific to node type (separate multiple values with commas)"),
        0, ++yNodeType, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(nodeType_JPanel, new JLabel ("    Interval - match Interval parameter"),
        0, ++yNodeType, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(nodeType_JPanel, new JLabel ("In the future additional node behaviors will be added, " +
        "for example to handle reservoirs."),
        0, ++yNodeType, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(nodeType_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yNodeType, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(nodeType_JPanel, new JLabel ("Node types that add:"), 
        0, ++yNodeType, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NodeAddTypes_JTextField = new JTextField (10);
    __NodeAddTypes_JTextField.setToolTipText("Separate node types with commas.");
    __NodeAddTypes_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(nodeType_JPanel, __NodeAddTypes_JTextField,
        1, yNodeType, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(nodeType_JPanel, new JLabel ("Optional - node types that add."),
        3, yNodeType, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(nodeType_JPanel, new JLabel ("Default node time series data types that add flow:"), 
        0, ++yNodeType, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NodeAddDataTypes_JTextField = new JTextField (10);
    __NodeAddDataTypes_JTextField.setToolTipText("Separate data types with commas.");
    __NodeAddDataTypes_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(nodeType_JPanel, __NodeAddDataTypes_JTextField,
        1, yNodeType, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(nodeType_JPanel, new JLabel ("Optional - node time series data types that add."),
        3, yNodeType, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(nodeType_JPanel, new JSeparator(JSeparator.HORIZONTAL), 
            0, ++yNodeType, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    
    JGUIUtil.addComponent(nodeType_JPanel, new JLabel ("Node types that subtract flow:"), 
        0, ++yNodeType, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NodeSubtractTypes_JTextField = new JTextField (10);
    __NodeSubtractTypes_JTextField.setToolTipText("Separate node types with commas.");
    __NodeSubtractTypes_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(nodeType_JPanel, __NodeSubtractTypes_JTextField,
        1, yNodeType, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(nodeType_JPanel, new JLabel ("Optional - node types that subtract."),
        3, yNodeType, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(nodeType_JPanel, new JLabel ("Default node time series data types that subtract:"), 
        0, ++yNodeType, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NodeSubtractDataTypes_JTextField = new JTextField (10);
    __NodeSubtractDataTypes_JTextField.setToolTipText("Separate data types with commas.");
    __NodeSubtractDataTypes_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(nodeType_JPanel, __NodeSubtractDataTypes_JTextField,
        1, yNodeType, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(nodeType_JPanel, new JLabel ("Optional - node time series data types that subtract."),
        3, yNodeType, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(nodeType_JPanel, new JSeparator(JSeparator.HORIZONTAL), 
            0, ++yNodeType, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    
    JGUIUtil.addComponent(nodeType_JPanel, new JLabel ("Node types that set outflow:"), 
        0, ++yNodeType, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NodeOutflowTypes_JTextField = new JTextField (10);
    __NodeOutflowTypes_JTextField.setToolTipText("Separate node types with commas.");
    __NodeOutflowTypes_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(nodeType_JPanel, __NodeOutflowTypes_JTextField,
        1, yNodeType, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(nodeType_JPanel, new JLabel ("Optional - node types that set outflow."),
        3, yNodeType, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(nodeType_JPanel, new JLabel ("Defalt node time series data types that set outflow:"), 
        0, ++yNodeType, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NodeOutflowDataTypes_JTextField = new JTextField (10);
    __NodeOutflowDataTypes_JTextField.setToolTipText("Separate data types with commas.");
    __NodeOutflowDataTypes_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(nodeType_JPanel, __NodeOutflowDataTypes_JTextField,
        1, yNodeType, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(nodeType_JPanel, new JLabel ("Optional - node time series data types that set outflow."),
        3, yNodeType, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(nodeType_JPanel, new JSeparator(JSeparator.HORIZONTAL), 
            0, ++yNodeType, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    
    JGUIUtil.addComponent(nodeType_JPanel, new JLabel ("Node types with no change:"), 
        0, ++yNodeType, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NodeFlowThroughTypes_JTextField = new JTextField (10);
    __NodeFlowThroughTypes_JTextField.setToolTipText("Separate node types with commas.");
    __NodeFlowThroughTypes_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(nodeType_JPanel, __NodeFlowThroughTypes_JTextField,
        1, yNodeType, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(nodeType_JPanel, new JLabel ("Optional - node types where inflow=outflow."),
        3, yNodeType, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Command:"), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (7,60);
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
 
	__ok_JButton = new SimpleJButton("OK", this);
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add (__ok_JButton);
	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add (__cancel_JButton);
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + " Command");
    pack();
    JGUIUtil.center(this);
	refresh();	// Sets the __path_JButton status
	setResizable (false);
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
	String NetworkID = "";
	String NetworkName = "";
	String DefaultDownstreamNodeID = "";
    String TableID = "";
    String NodeIDColumn = "";
    String NodeNameColumn = "";
    String NodeTypeColumn = "";
    String NodeGroupColumn = "";
    String NodeDistanceColumn = "";
    String NodeWeightColumn = "";
    String DownstreamNodeIDColumn = "";
    String NodeAddTypes = "";
    String NodeAddDataTypes = "";
    String NodeSubtractTypes = "";
    String NodeSubtractDataTypes = "";
    String NodeOutflowTypes = "";
    String NodeOutflowDataTypes = "";
    String NodeFlowThroughTypes = "";
	PropList props = __command.getCommandParameters();
	if (__first_time) {
		__first_time = false;
		NetworkID = props.getValue ( "NetworkID" );
		NetworkName = props.getValue ( "NetworkName" );
		DefaultDownstreamNodeID = props.getValue ( "DefaultDownstreamNodeID" );
        TableID = props.getValue ( "TableID" );
        NodeIDColumn = props.getValue ( "NodeIDColumn" );
        NodeNameColumn = props.getValue ( "NodeNameColumn" );
        NodeTypeColumn = props.getValue ( "NodeTypeColumn" );
        NodeGroupColumn = props.getValue ( "NodeGroupColumn" );
        NodeDistanceColumn = props.getValue ( "NodeDistanceColumn" );
        NodeWeightColumn = props.getValue ( "NodeWeightColumn" );
        DownstreamNodeIDColumn = props.getValue ( "DownstreamNodeIDColumn" );
        NodeAddTypes = props.getValue ( "NodeAddTypes" );
        NodeAddDataTypes = props.getValue ( "NodeAddDataTypes" );
        NodeSubtractTypes = props.getValue ( "NodeSubtractTypes" );
        NodeSubtractDataTypes = props.getValue ( "NodeSubtractDataTypes" );
        NodeOutflowTypes = props.getValue ( "NodeOutflowTypes" );
        NodeOutflowDataTypes = props.getValue ( "NodeOutflowDataTypes" );
        NodeFlowThroughTypes = props.getValue ( "NodeFlowThroughTypes" );
        if ( NetworkID != null ) {
            __NetworkID_JTextField.setText ( NetworkID );
        }
        if ( NetworkName != null ) {
            __NetworkName_JTextField.setText ( NetworkName );
        }
        if ( DefaultDownstreamNodeID != null ) {
            __DefaultDownstreamNodeID_JTextField.setText ( DefaultDownstreamNodeID );
        }
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
        if ( NodeIDColumn != null ) {
            __NodeIDColumn_JTextField.setText ( NodeIDColumn );
        }
        if ( NodeNameColumn != null ) {
            __NodeNameColumn_JTextField.setText ( NodeNameColumn );
        }
        if ( NodeTypeColumn != null ) {
            __NodeTypeColumn_JTextField.setText ( NodeTypeColumn );
        }
        if ( NodeGroupColumn != null ) {
            __NodeGroupColumn_JTextField.setText ( NodeGroupColumn );
        }
        if ( NodeDistanceColumn != null ) {
            __NodeDistanceColumn_JTextField.setText ( NodeDistanceColumn );
        }
        if ( NodeWeightColumn != null ) {
            __NodeWeightColumn_JTextField.setText ( NodeWeightColumn );
        }
        if ( DownstreamNodeIDColumn != null ) {
            __DownstreamNodeIDColumn_JTextField.setText ( DownstreamNodeIDColumn );
        }
        if ( NodeAddTypes != null ) {
            __NodeAddTypes_JTextField.setText ( NodeAddTypes );
        }
        if ( NodeAddDataTypes != null ) {
            __NodeAddDataTypes_JTextField.setText ( NodeAddDataTypes );
        }
        if ( NodeSubtractTypes != null ) {
            __NodeSubtractTypes_JTextField.setText ( NodeSubtractTypes );
        }
        if ( NodeSubtractDataTypes != null ) {
            __NodeSubtractDataTypes_JTextField.setText ( NodeSubtractDataTypes );
        }
        if ( NodeOutflowTypes != null ) {
            __NodeOutflowTypes_JTextField.setText ( NodeOutflowTypes );
        }
        if ( NodeOutflowDataTypes != null ) {
            __NodeOutflowDataTypes_JTextField.setText ( NodeOutflowDataTypes );
        }
        if ( NodeFlowThroughTypes != null ) {
            __NodeFlowThroughTypes_JTextField.setText ( NodeFlowThroughTypes );
        }
	}
	// Regardless, reset the command from the fields...
	NetworkID = __NetworkID_JTextField.getText().trim();
	NetworkName = __NetworkName_JTextField.getText().trim();
	DefaultDownstreamNodeID = __DefaultDownstreamNodeID_JTextField.getText().trim();
	TableID = __TableID_JComboBox.getSelected();
	NodeIDColumn = __NodeIDColumn_JTextField.getText().trim();
	NodeNameColumn = __NodeNameColumn_JTextField.getText().trim();
	NodeTypeColumn = __NodeTypeColumn_JTextField.getText().trim();
	NodeGroupColumn = __NodeGroupColumn_JTextField.getText().trim();
	NodeDistanceColumn = __NodeDistanceColumn_JTextField.getText().trim();
	NodeWeightColumn = __NodeWeightColumn_JTextField.getText().trim();
	DownstreamNodeIDColumn = __DownstreamNodeIDColumn_JTextField.getText().trim();
	NodeAddTypes = __NodeAddTypes_JTextField.getText().trim();
	NodeAddDataTypes = __NodeAddDataTypes_JTextField.getText().trim();
	NodeSubtractTypes = __NodeSubtractTypes_JTextField.getText().trim();
	NodeSubtractDataTypes = __NodeSubtractDataTypes_JTextField.getText().trim();
    NodeOutflowTypes = __NodeOutflowTypes_JTextField.getText().trim();
    NodeOutflowDataTypes = __NodeOutflowDataTypes_JTextField.getText().trim();
    NodeFlowThroughTypes = __NodeFlowThroughTypes_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "NetworkID=" + NetworkID );
	props.add ( "NetworkName=" + NetworkName );
	props.add ( "DefaultDownstreamNodeID=" + DefaultDownstreamNodeID );
    props.add ( "TableID=" + TableID );
    props.add ( "NodeIDColumn=" + NodeIDColumn );
    props.add ( "NodeNameColumn=" + NodeNameColumn );
    props.add ( "NodeTypeColumn=" + NodeTypeColumn );
    props.add ( "NodeGroupColumn=" + NodeGroupColumn );
    props.add ( "NodeDistanceColumn=" + NodeDistanceColumn );
    props.add ( "NodeWeightColumn=" + NodeWeightColumn );
    props.add ( "DownstreamNodeIDColumn=" + DownstreamNodeIDColumn );
    props.add ( "NodeAddTypes=" + NodeAddTypes );
    props.add ( "NodeAddDataTypes=" + NodeAddDataTypes );
    props.add ( "NodeSubtractTypes=" + NodeSubtractTypes );
    props.add ( "NodeSubtractDataTypes=" + NodeSubtractDataTypes );
    props.add ( "NodeOutflowTypes=" + NodeOutflowTypes );
    props.add ( "NodeOutflowDataTypes=" + NodeOutflowDataTypes );
    props.add ( "NodeFlowThroughTypes=" + NodeFlowThroughTypes );
	__command_JTextArea.setText( __command.toString ( props ).trim() );
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
public void windowActivated(WindowEvent evt)	{}
public void windowClosed(WindowEvent evt)	{}
public void windowDeactivated(WindowEvent evt)	{}
public void windowDeiconified(WindowEvent evt)	{}
public void windowIconified(WindowEvent evt)	{}
public void windowOpened(WindowEvent evt)	{}

}
