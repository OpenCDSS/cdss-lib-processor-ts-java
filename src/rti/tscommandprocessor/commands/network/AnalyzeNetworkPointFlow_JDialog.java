// AnalyzeNetworkPointFlow_JDialog - editor for AnalyzeNetworkPointFlow command

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2023 Colorado Department of Natural Resources

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

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Time.TimeInterval;

@SuppressWarnings("serial")
public class AnalyzeNetworkPointFlow_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{

private boolean __error_wait = false; // To track errors.
private boolean __first_time = true; // Indicate first time display.
private JTextArea __command_JTextArea = null;
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __NodeIDColumn_JTextField = null;
private JTextField __NodeNameColumn_JTextField = null;
private JTextField __NodeTypeColumn_JTextField = null;
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
private JTextField __TSIDColumn_JTextField = null;
private SimpleJComboBox __Interval_JComboBox = null;
private JTextField __AnalysisStart_JTextField = null;
private JTextField __AnalysisEnd_JTextField = null;
private JTextField __Units_JTextField = null;
private SimpleJComboBox __GainMethod_JComboBox = null;
private JTextField __OutputTableID_JTextField = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private AnalyzeNetworkPointFlow_Command __command = null;
private boolean __ok = false;

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of table identifiers to provide as choices
*/
public AnalyzeNetworkPointFlow_JDialog ( JFrame parent, AnalyzeNetworkPointFlow_Command command, List<String> tableIDChoices ) {
	super(parent, true);
	initialize ( parent, command, tableIDChoices );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed(ActionEvent event) {
	Object o = event.getSource();

    if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "AnalyzeNetworkPointFlow");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput ();
		if ( !__error_wait ) {
			// Command has been edited.
			response ( true );
		}
	}
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag to true.
This should be called before response() is allowed to complete.
*/
private void checkInput () {
	// Put together a list of parameters to check.
	PropList props = new PropList ( "" );
	String TableID = __TableID_JComboBox.getSelected();
	String NodeIDColumn = __NodeIDColumn_JTextField.getText().trim();
	String NodeNameColumn = __NodeNameColumn_JTextField.getText().trim();
	String NodeTypeColumn = __NodeTypeColumn_JTextField.getText().trim();
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
	String TSIDColumn = __TSIDColumn_JTextField.getText().trim();
    String Interval = __Interval_JComboBox.getSelected();
    String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
    String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
    String Units = __Units_JTextField.getText().trim();
    String GainMethod = __GainMethod_JComboBox.getSelected();
    String OutputTableID = __OutputTableID_JTextField.getText().trim();
	__error_wait = false;

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
    if ( TSIDColumn.length() > 0 ) {
        props.set ( "TSIDColumn", TSIDColumn );
    }
    if ( Interval.length() > 0 ) {
        props.set ( "Interval", Interval );
    }
    if ( AnalysisStart.length() > 0 ) {
        props.set ( "AnalysisStart", AnalysisStart );
    }
    if ( AnalysisEnd.length() > 0 ) {
        props.set ( "AnalysisEnd", AnalysisEnd );
    }
    if ( Units.length() > 0 ) {
        props.set ( "Units", Units );
    }
    if ( GainMethod.length() > 0 ) {
        props.set ( "GainMethod", GainMethod );
    }
    if ( OutputTableID.length() > 0 ) {
        props.set ( "OutputTableID", OutputTableID );
    }
	try {
	    // This will warn the user.
		__command.checkCommandParameters ( props, null, 1 );
	}
	catch ( Exception e ) {
        Message.printWarning(2,"", e);
		// The warning would have been printed in the check code.
		__error_wait = true;
	}
}

/**
Commit the edits to the command.
In this case the command parameters have already been checked and no errors were detected.
*/
private void commitEdits () {
	String TableID = __TableID_JComboBox.getSelected();
    String NodeIDColumn = __NodeIDColumn_JTextField.getText().trim();
    String NodeNameColumn = __NodeNameColumn_JTextField.getText().trim();
    String NodeTypeColumn = __NodeTypeColumn_JTextField.getText().trim();
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
    String TSIDColumn = __TSIDColumn_JTextField.getText().trim();
    String Interval = __Interval_JComboBox.getSelected();
    String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
    String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
    String Units = __Units_JTextField.getText().trim();
    String GainMethod = __GainMethod_JComboBox.getSelected();
    String OutputTableID = __OutputTableID_JTextField.getText().trim();
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "NodeIDColumn", NodeIDColumn );
    __command.setCommandParameter ( "NodeNameColumn", NodeNameColumn );
    __command.setCommandParameter ( "NodeTypeColumn", NodeTypeColumn );
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
    __command.setCommandParameter ( "TSIDColumn", TSIDColumn );
	__command.setCommandParameter ( "Interval", Interval );
    __command.setCommandParameter ( "AnalysisStart", AnalysisStart );
    __command.setCommandParameter ( "AnalysisEnd", AnalysisEnd );
    __command.setCommandParameter ( "Units", Units );
    __command.setCommandParameter ( "GainMethod", GainMethod );
    __command.setCommandParameter ( "OutputTableID", OutputTableID );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit and possibly run.
*/
private void initialize ( JFrame parent, AnalyzeNetworkPointFlow_Command command, List<String> tableIDChoices ) {
	__command = command;

	addWindowListener(this);

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel.

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout(new GridBagLayout());
	getContentPane().add ("North", main_JPanel);
	int y = -1;

	JPanel paragraph = new JPanel();
	paragraph.setLayout(new GridBagLayout());
	int yy = -1;

   	JGUIUtil.addComponent(paragraph, new JLabel (
        "This command analyzes a flow network to compute point flows at all nodes in the network."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "It is assumed that math operations can occur in the same timestep (no routing)."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "Consequently, the analysis provides an approximation of system behavior at a point in time."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	JGUIUtil.addComponent(main_JPanel, paragraph,
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Tabbed pane for parameters.

    JTabbedPane main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

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
        "   NodeID - is used for the time series location"),
        0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel (
        "   NodeType - is used to define analysis behavior (see \"Define Node Type Behavior\" tab)"),
        0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel (
        "   NodeDistance and NodeWeight - are used to distribute reach gain/loss back to nodes in the reach (see the \"Analysis\" tab)"),
        0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table ID:" ),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true );    // Allow edit.
    tableIDChoices.add(0,""); // Add blank to ignore table.
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    __TableID_JComboBox.getJTextComponent().addKeyListener ( this );
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

    JPanel analysis_JPanel = new JPanel ();
    analysis_JPanel.setLayout( new GridBagLayout() );
    int yAnalysis = -1;
    main_JTabbedPane.addTab ( "Analysis", analysis_JPanel );
    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
        "The following parameters control the analysis."),
        0, ++yAnalysis, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
        "Interval - will be used to find time series for each node (TSIDColumn parameter with %I will substitue this interval)."),
        0, ++yAnalysis, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Data interval:"),
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Interval_JComboBox = new SimpleJComboBox ( false );
    List<String> intervalChoices = TimeInterval.getTimeIntervalChoices(TimeInterval.MINUTE, TimeInterval.YEAR, false, -1);
    __Interval_JComboBox.setData(intervalChoices);
    __Interval_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __Interval_JComboBox,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel("Required - data interval (time step) for time series."),
        3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Analysis start:" ),
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AnalysisStart_JTextField = new JTextField ( "", 20 );
    __AnalysisStart_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __AnalysisStart_JTextField,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
        "Optional - analysis start date/time (default=full time series period)."),
        3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Analysis end:" ),
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AnalysisEnd_JTextField = new JTextField ( "", 20 );
    __AnalysisEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __AnalysisEnd_JTextField,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
        "Optional - analysis end date/time (default=full time series period)."),
        3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Data units:" ),
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Units_JTextField = new JTextField ( "", 10 );
    JGUIUtil.addComponent(analysis_JPanel, __Units_JTextField,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __Units_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
        "Optional - units for output time series (default=no units)."),
        3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Gain method:"),
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __GainMethod_JComboBox = new SimpleJComboBox ( false );
    List<String> gainChoices = new ArrayList<>();
    gainChoices.add ( "" );
    gainChoices.add ( "" + NetworkGainMethodType.NONE );
    gainChoices.add ( "" + NetworkGainMethodType.DISTANCE );
    gainChoices.add ( "" + NetworkGainMethodType.DISTANCE_WEIGHT );
    gainChoices.add ( "" + NetworkGainMethodType.WEIGHT );
    __GainMethod_JComboBox.setData(gainChoices);
    __GainMethod_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __GainMethod_JComboBox,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel("Optional - how to compute gains (default=" + NetworkGainMethodType.NONE + ")."),
        3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ("Output table ID:"),
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputTableID_JTextField = new JTextField (10);
    __OutputTableID_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(analysis_JPanel, __OutputTableID_JTextField,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel ("Optional - identifier for output summary table."),
        3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JPanel ts_JPanel = new JPanel ();
    ts_JPanel.setLayout( new GridBagLayout() );
    int yTs = -1;
    main_JTabbedPane.addTab ( "TSID/Alias", ts_JPanel );
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
        "Time series for each node are by default determined by constructing a time series identifier (TSID) using " +
        "node identifier and data types associated with node types."),
        0, ++yTs, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
        "Time series that are available are then matched and used for the analysis."),
        0, ++yTs, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
        "However, to avoid confusion, it is recommended that the TSID or alias for each time series is " +
        "specified in a column of the network."),
        0, ++yTs, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
        "Currently node type behavior is simple enough that only one input time series is needed at a node."),
        0, ++yTs, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
        "Use the following parameter to indicate the column containing the TSID or alias to override default time series."),
        0, ++yTs, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
        "Specify %I in the TSID or alias in the network column to substitute the data interval (e.g., substitue \"Day\") at runtime."),
        0, ++yTs, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "TSIDColumn:" ),
        0, ++yTs, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TSIDColumn_JTextField = new JTextField ( "", 20 );
    __TSIDColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __TSIDColumn_JTextField,
        1, yTs, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel(
        "Recommended - time series identifier column in network table (default=construct TSID from time series data type column)."),
        3, yTs, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Command:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (7,60);
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable (false);
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents.
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
	refresh();	// Sets the __path_JButton status.
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

public void keyTyped (KeyEvent event) {
}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user canceled.
*/
public boolean ok () {
	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh () {
	String routine = getClass().getSimpleName() + ".refresh";
    String TableID = "";
    String NodeIDColumn = "";
    String NodeNameColumn = "";
    String NodeTypeColumn = "";
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
    String TSIDColumn = "";
    String Interval = "";
    String AnalysisStart = "";
    String AnalysisEnd = "";
    String Units = "";
    String GainMethod = "";
    String OutputTableID = "";
	PropList props = __command.getCommandParameters();
	if (__first_time) {
		__first_time = false;
        TableID = props.getValue ( "TableID" );
        NodeIDColumn = props.getValue ( "NodeIDColumn" );
        NodeNameColumn = props.getValue ( "NodeNameColumn" );
        NodeTypeColumn = props.getValue ( "NodeTypeColumn" );
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
        TSIDColumn = props.getValue ( "TSIDColumn" );
        Interval = props.getValue ( "Interval" );
        AnalysisStart = props.getValue ( "AnalysisStart" );
        AnalysisEnd = props.getValue ( "AnalysisEnd" );
        Units = props.getValue ( "Units" );
        GainMethod = props.getValue ( "GainMethod" );
        OutputTableID = props.getValue ( "OutputTableID" );
        if ( TableID == null ) {
            // Select default.
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
        if ( TSIDColumn != null ) {
            __TSIDColumn_JTextField.setText ( TSIDColumn );
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
        if ( AnalysisStart != null ) {
            __AnalysisStart_JTextField.setText( AnalysisStart );
        }
        if ( AnalysisEnd != null ) {
            __AnalysisEnd_JTextField.setText ( AnalysisEnd );
        }
        if ( Units != null ) {
            __Units_JTextField.setText( Units );
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(__GainMethod_JComboBox, GainMethod, JGUIUtil.NONE, null, null ) ) {
            __GainMethod_JComboBox.select ( GainMethod );
        }
        else {
            if ( (GainMethod == null) || GainMethod.equals("") ) {
                // New command...select the default.
                __GainMethod_JComboBox.select ( 0 );
            }
            else {
                // Bad user command.
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "GainMethod parameter \"" + GainMethod + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        if ( OutputTableID != null ) {
            __OutputTableID_JTextField.setText ( OutputTableID );
        }
	}
	// Regardless, reset the command from the fields.
	TableID = __TableID_JComboBox.getSelected();
	NodeIDColumn = __NodeIDColumn_JTextField.getText().trim();
	NodeNameColumn = __NodeNameColumn_JTextField.getText().trim();
	NodeTypeColumn = __NodeTypeColumn_JTextField.getText().trim();
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
    TSIDColumn = __TSIDColumn_JTextField.getText().trim();
	Interval = __Interval_JComboBox.getSelected();
    AnalysisStart = __AnalysisStart_JTextField.getText().trim();
    AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
    Units = __Units_JTextField.getText().trim();
    GainMethod = __GainMethod_JComboBox.getSelected();
    OutputTableID = __OutputTableID_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
    props.add ( "TableID=" + TableID );
    props.add ( "NodeIDColumn=" + NodeIDColumn );
    props.add ( "NodeNameColumn=" + NodeNameColumn );
    props.add ( "NodeTypeColumn=" + NodeTypeColumn );
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
    props.add ( "TSIDColumn=" + TSIDColumn );
	props.add ( "Interval=" + Interval );
    props.add ( "AnalysisStart=" + AnalysisStart );
    props.add ( "AnalysisEnd=" + AnalysisEnd );
    props.add ( "Units=" + Units );
    props.add ( "GainMethod=" + GainMethod );
    props.add ( "OutputTableID=" + OutputTableID );
	__command_JTextArea.setText( __command.toString ( props ).trim() );
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
Responds to WindowEvents.
@param event WindowEvent object
*/
public void windowClosing(WindowEvent event) {
	response ( false );
}

// The following methods are all necessary because this class implements WindowListener.
public void windowActivated(WindowEvent evt) {
}

public void windowClosed(WindowEvent evt) {
}

public void windowDeactivated(WindowEvent evt) {
}

public void windowDeiconified(WindowEvent evt) {
}

public void windowIconified(WindowEvent evt) {
}

public void windowOpened(WindowEvent evt) {
}

}