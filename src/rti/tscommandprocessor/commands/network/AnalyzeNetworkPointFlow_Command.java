package rti.tscommandprocessor.commands.network;

import javax.swing.JFrame;

import cdss.domain.hydrology.network.HydrologyNode;
import cdss.domain.hydrology.network.HydrologyNodeNetwork;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSUtil;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the CopyTable() command.
*/
public class AnalyzeNetworkPointFlow_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

/**
The table that is created.
*/
private DataTable __table = null;

/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discoveryTSList = null;

/**
Constructor.
*/
public AnalyzeNetworkPointFlow_Command ()
{	super();
	setCommandName ( "AnalyzeNetworkPointFlow" );
}

/**
Add nodes upstream of the node with the given identifier (which has already been added to the network).
*/
private void addNodesUpstreamOfNode ( DataTable table, HydrologyNodeNetwork network, String nodeID,
    int nodeIdColumn, int nodeNameColumn, int nodeTypeColumn, int downstreamNodeIdColumn,
    List<String> problems )
{   String routine = getClass().getName() + ".addNodesUpstreamOfNode";
    // Find the table records that have "nodeID" as the downstream node
    List<TableRecord> records = findTableRecordsWithValue(table, downstreamNodeIdColumn, nodeID, false);
    String upstreamNodeID;
    String nodeName;
    HydrologyNode node;
    for ( TableRecord record : records ) {
        // Add the node...
        try {
            upstreamNodeID = (String)record.getFieldValue(nodeIdColumn);
            nodeName = (String)record.getFieldValue(nodeNameColumn);
        }
        catch ( Exception e ) {
            problems.add ( "Error getting upstream node ID - not adding upstream nodes (" + e + ").");
            continue;
        }
        Message.printStatus(2,routine,"Adding node \"" + upstreamNodeID + "\" upstream of \"" +
            nodeID + "\"." );
        node = network.addNode(upstreamNodeID, HydrologyNode.NODE_TYPE_UNKNOWN,
            null, // Upstream node ID is not yet known
            nodeID, // Downstream node ID
            false, // Not a natural flow node (not significant in general network)
            false ); // Not an import (not significant in general network)
        node.setDescription(nodeName);
        // Recursively add the nodes upstream of the node just added
        addNodesUpstreamOfNode ( table, network, upstreamNodeID, nodeIdColumn, nodeNameColumn,
            nodeTypeColumn, downstreamNodeIdColumn, problems );
    }
}

/**
Analyze the network point flow.
*/
private void analyzeNetworkPointFlow ( DataTable table, int nodeIDColumnNum, int nodeTypeColumnNum, int nodeDistanceColumn,
    String [] nodeAddTypes, String [] nodeAddDataTypes,
    String [] nodeSubtractTypes, String [] nodeSubtractDataTypes,
    String [] nodeOutflowTypes, String [] nodeOutflowDataTypes,
    String [] nodeFlowThroughTypes,
    HydrologyNodeNetwork network, List<TS> outputTSList, TimeInterval interval, NetworkGainMethodType gainMethod,
    DateTime analysisStart, DateTime analysisEnd, List<String> problems )
{   String routine = getClass().getName() + ".analyzeNetworkPointFlow";
    TS ts;
    String nodeID;
    String nodeType;
    Double nodeDistance;
    List<TS> tslist;
    TS nodeInflowTS;
    TS nodeOutflowTS;
    TS nodeAddTS;
    TS nodeSubtractTS;
    TS nodeUpstreamGainTS;
    TS nodeStorageTS;
    String description;
    List<HydrologyNode> upstreamNodeList;
    List<HydrologyNode> reachNodeList;
    int intervalBase = interval.getBase();
    int intervalMult = interval.getMultiplier();
    double gainToDistribute; // Error between known flow (stream gage) and upstream node outflow - correct with gain/loss
    for (HydrologyNode node = HydrologyNodeNetwork.getUpstreamNode(network.getNodeHead(), HydrologyNodeNetwork.POSITION_ABSOLUTE);
        node.getDownstreamNode() != null;
        node = HydrologyNodeNetwork.getDownstreamNode(node, HydrologyNodeNetwork.POSITION_COMPUTATIONAL)) {
        if (node == null) {
            break;
        }
        nodeID = node.getCommonID();
        nodeType = lookupTableString ( table, nodeIDColumnNum, nodeID, nodeTypeColumnNum );
        //nodeDistance = lookupTableString ( table, nodeIDColumnNum, nodeID, nodeTypeColumnNum );
        Message.printStatus(2,routine,"Analyzing node \"" + nodeID + "\", type \"" + nodeType + "\"" );
        if ( nodeType == null ) {
            // End node
            break;
        }
        // Get the output time series for the node, which were previously created by this command
        nodeInflowTS = lookupNodeOutputTimeSeries ( nodeID, "NodeInflow", outputTSList );
        nodeOutflowTS = lookupNodeOutputTimeSeries ( nodeID, "NodeOutflow", outputTSList );
        nodeAddTS = lookupNodeOutputTimeSeries ( nodeID, "NodeAdd", outputTSList );
        nodeSubtractTS = lookupNodeOutputTimeSeries ( nodeID, "NodeSubtract", outputTSList );
        nodeUpstreamGainTS = lookupNodeOutputTimeSeries ( nodeID, "NodeUpstreamGain", outputTSList );
        nodeStorageTS = lookupNodeOutputTimeSeries ( nodeID, "NodeStorage", outputTSList );
        // Save the description because want to reset at the end
        description = nodeInflowTS.getDescription();
        // Now process the time series based on the time series type
        if ( isNodeOfAnalysisType(nodeType, nodeOutflowTypes) ) {
            // Set the time series outflow to the matched input time series
            tslist = lookupAnalysisInputTimeSeries ( nodeID, nodeOutflowDataTypes, interval, problems );
            if ( tslist.size() != 1 ) {
                problems.add("Expecting 1 set time series for node \"" + nodeID + "\" but have " +
                    tslist.size() + " - unable to set outflow." );
            }
            else {
                ts = tslist.get(0);
                Message.printStatus(2,routine,"Setting node \"" + nodeID + "\" (" + nodeType + ") inflow and outflow: " +
                    ts.getIdentifier().toStringAliasAndTSID());
                try {
                    TSUtil.setFromTS(nodeInflowTS, ts);
                }
                catch ( Exception e ) {
                    problems.add ( "Error setting node \"" + nodeID + "\" inflow time series (" + e + ").");
                }
                try {
                    TSUtil.setFromTS(nodeOutflowTS, ts);
                }
                catch ( Exception e ) {
                    problems.add ( "Error setting node \"" + nodeID + "\" outflow time series (" + e + ").");
                }
                TSUtil.setConstant(nodeAddTS, 0.0);
                TSUtil.setConstant(nodeSubtractTS, 0.0);
                TSUtil.setConstant(nodeStorageTS, 0.0);
            }
        }
        else {
            // First set the time series to the sum of the upstream outflow time series...
            //List<HydrologyNode> upstreamNodeList = new Vector();
            //network.findUpstreamFlowNodes(upstreamNodeList, node, true);
            upstreamNodeList = node.getUpstreamNodes();
            Message.printStatus(2,routine,"Setting node \"" + nodeID + "\" inflow to total of upstream node outflows.");
            if ( upstreamNodeList.size() == 0 ) {
                // Headwater node so no upstream outflow, therefore no inflow
                try {
                    Message.printStatus(2,routine,"Setting node \"" + nodeID + "\" inflow zero since headwater node.");
                    TSUtil.setConstant(nodeInflowTS, 0.0);
                }
                catch ( Exception e ) {
                    problems.add ( "Error setting node \"" + nodeID + "\" inflow time series to zero (" + e + ").");
                }
            }
            else {
                for ( int i = 0; i < upstreamNodeList.size(); i++ ) {
                    TS upstreamOutflowTS = lookupNodeOutputTimeSeries ( upstreamNodeList.get(i).getCommonID(),
                        "NodeOutflow", outputTSList );
                    if ( i == 0 ) {
                        // Want to make sure missing is reset
                        try {
                            Message.printStatus(2,routine,"Setting node \"" + nodeID + "\" inflow to first upstream node \"" +
                                upstreamNodeList.get(i).getCommonID() + "\" outflows.");
                            TSUtil.setFromTS(nodeInflowTS, upstreamOutflowTS);
                        }
                        catch ( Exception e ) {
                            problems.add ( "Error setting node \"" + nodeID + "\" inflow time series to upstream outflow time series to intialize inflow (" + e + ").");
                        }
                    }
                    else {
                        try {
                            Message.printStatus(2,routine,"Adding to node \"" + nodeID + "\" inflow time series using upstream node \"" +
                                upstreamNodeList.get(i).getCommonID() + "\" outflows.");
                            List<TS> addTSList = new Vector<TS>();
                            addTSList.add(upstreamOutflowTS);
                            TSUtil.add(nodeInflowTS, addTSList, TSUtil.SET_MISSING_IF_ANY_MISSING);
                        }
                        catch ( Exception e ) {
                            problems.add("Error adding upstream outflow time series to node \"" + nodeID + "\" inflow (" + e + ")." );
                        }
                    }
                }
            }
            TSUtil.setConstant(nodeStorageTS, 0.0);
            // Copy inflow to outflow before doing additional calculations
            Message.printStatus(2,routine,"Copy node \"" + nodeID + "\" inflow time series to outflow.");
            try {
                TSUtil.setFromTS(nodeOutflowTS, nodeInflowTS);
            }
            catch ( Exception e ) {
                problems.add ( "Error setting node \"" + nodeID + "\" outflow time series to inflow time series (" + e + ").");
            }
            // Now perform specific actions to adjust the node's outflow
            if ( isNodeOfAnalysisType(nodeType, nodeAddTypes) ) {
                // Add the input time series at the node
                tslist = lookupAnalysisInputTimeSeries ( nodeID, nodeAddDataTypes, interval, problems );
                if ( tslist.size() != 1 ) {
                    problems.add("Expecting 1 set time series for node \"" + nodeID + "\" but have " +
                        tslist.size() + " - unable to add to outflow." );
                }
                else {
                    ts = tslist.get(0);
                    List<TS> addTSList = new Vector<TS>();
                    addTSList.add ( ts );
                    Message.printStatus(2,routine, "Adding to node \"" + nodeID + "\" outflow (" +
                        nodeType + "): " + ts.getIdentifier().toStringAliasAndTSID());
                    try {
                        TSUtil.add(nodeOutflowTS, addTSList, TSUtil.SET_MISSING_IF_ANY_MISSING );
                        TSUtil.setFromTS(nodeAddTS, ts);
                        TSUtil.setConstant(nodeSubtractTS, 0.0);
                    }
                    catch ( Exception e ) {
                        problems.add("Error adding time series for node \"" + nodeID + "\" (" + e + ")." );
                    }
                }
            }
            else if ( isNodeOfAnalysisType(nodeType, nodeSubtractTypes) ) {
                // Subtract the input time series at the node
                tslist = lookupAnalysisInputTimeSeries ( nodeID, nodeSubtractDataTypes, interval, problems );
                if ( tslist.size() != 1 ) {
                    problems.add("Expecting 1 set time series for node \"" + nodeID + "\" but have " +
                        tslist.size() + " - unable to subtract from outflow." );
                }
                else {
                    ts = tslist.get(0);
                    List<TS> subtractTSList = new Vector<TS>();
                    subtractTSList.add ( ts );
                    Message.printStatus(2,routine,"Subtracting from node \"" + nodeID + "\" outflow (" +
                        nodeType + "): " + ts.getIdentifier().toStringAliasAndTSID());
                    try {
                        TSUtil.subtract(nodeOutflowTS, subtractTSList, TSUtil.SET_MISSING_IF_ANY_MISSING);
                        TSUtil.setFromTS(nodeSubtractTS, ts);
                        TSUtil.setConstant(nodeAddTS, 0.0);
                    }
                    catch ( Exception e ) {
                        problems.add("Error subtracting time series for node \"" + nodeID + "\" (" + e + ")." );
                    }
                }
            }
            else if ( isNodeOfAnalysisType(nodeType, nodeFlowThroughTypes) ) {
                // Set outflow to inflow - no need to do any more because it was done above
                TSUtil.setConstant(nodeAddTS, 0.0);
                TSUtil.setConstant(nodeSubtractTS, 0.0);
            }
            else {
                problems.add("Node \"" + nodeID +
                    "\" type \"" + nodeType + "\" does not match any recognized node type for point flow analysis - check network." );
            }
        }
        // Now compute the gain/loss and adjust each node.  This is a matter of redistributing the remainder error
        // flowing into nodes with known outflow.  Since the network is being processed upstream to downstream, any
        // occurrence of a known outflow node needs to be processed, compared to its upstream node.  For now, only allow
        // computing gain when there are no branches in the network
        /*
        if ( gainMethod == NetworkGainMethodType.DISTANCE ) {
            if ( isNodeOfAnalysisType(nodeType, nodeOutflowTypes) ) {
                upstreamNodeList = node.getUpstreamNodes();
                for ( HydrologyNode node2 : upstreamNodeList ) {
                    x
                }
                // Have to process each timestep because the gain/loss with vary
                for ( DateTime dt = new DateTime(analysisStart); dt.lessThanOrEqualTo(analysisEnd);
                    dt.addInterval(intervalBase,intervalMult) ) {
                    // Get the outflow from the upstream nodes
                    gainToDistribute = 0.0;
                    for ( HydrologyNode node2 : upstreamNodeList ) {
                        
                    }
                    reachNodeList = getNodesUpstreamToKnownFlows ( network, node, problems );
                }
            }
        }
        */
        // Reset descriptions so as to not have all the extra process notes
        nodeInflowTS.setDescription(description);
        nodeOutflowTS.setDescription(description);
        nodeAddTS.setDescription(description);
        nodeSubtractTS.setDescription(description);
        nodeUpstreamGainTS.setDescription(description);
        nodeStorageTS.setDescription(description); 
    }

}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	String TableID = parameters.getValue ( "TableID" );
    String NodeIDColumn = parameters.getValue ( "NodeIDColumn" );
    String NodeTypeColumn = parameters.getValue ( "NodeTypeColumn" );
    String DownstreamNodeIDColumn = parameters.getValue ( "DownstreamNodeIDColumn" );
    String Interval = parameters.getValue ( "Interval" );
    String AnalysisStart = parameters.getValue ( "AnalysisStart" );
    String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
    String OutputTableID = parameters.getValue ( "OutputTableID" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (TableID == null) || (TableID.length() == 0) ) {
        message = "The table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the table identifier." ) );
    }
    
    if ( (TableID != null) && (TableID.length() != 0) && (OutputTableID != null) && (OutputTableID.length() != 0) &&
        TableID.equalsIgnoreCase(OutputTableID) ) {
        message = "The input and output table identifiers are the same.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the output table identifier different from the input table identifier." ) );
    }
    
    if ( (NodeIDColumn == null) || (NodeIDColumn.length() == 0) ) {
        message = "The node ID column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the node ID column." ) );
    }
    
    if ( (NodeTypeColumn == null) || (NodeTypeColumn.length() == 0) ) {
        message = "The node type column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the node type column." ) );
    }
    
    if ( (DownstreamNodeIDColumn == null) || (DownstreamNodeIDColumn.length() == 0) ) {
        message = "The downstream node identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the downstream node identifier." ) );
    }
    
    if ( (Interval == null) || (Interval.length() == 0) ) {
        message = "The interval must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify interval." ) );
    }
    else {
        try {
            TimeInterval.parseInterval(Interval);
        }
        catch ( Exception e2 ) {
            message = "The interval (" + Interval + ") is not a valid interval.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid time series interval." ) );
        }
    }
    
    if ( (AnalysisStart != null) && !AnalysisStart.equals("") &&
        !AnalysisStart.equalsIgnoreCase("OutputStart") && !AnalysisStart.equalsIgnoreCase("OutputEnd") ) {
        try {
            DateTime.parse(AnalysisStart);
        }
        catch ( Exception e ) {
            message = "The analysis start date/time \"" + AnalysisStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid date/time, OutputStart, or output end." ) );
        }
    }
    if ( (AnalysisEnd != null) && !AnalysisEnd.equals("") &&
        !AnalysisEnd.equalsIgnoreCase("OutputStart") && !AnalysisEnd.equalsIgnoreCase("OutputEnd") ) {
        try {
            DateTime.parse( AnalysisEnd );
        }
        catch ( Exception e ) {
            message = "The analysis end date/time \"" + AnalysisEnd + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
        }
    }
 
	// Check for invalid parameters...
	List<String> valid_Vector = new Vector<String>();
    valid_Vector.add ( "TableID" );
    valid_Vector.add ( "NodeIDColumn" );
    valid_Vector.add ( "NodeNameColumn" );
    valid_Vector.add ( "NodeTypeColumn" );
    valid_Vector.add ( "NodeDistanceColumn" );
    valid_Vector.add ( "DownstreamNodeIDColumn" );
    valid_Vector.add ( "NodeAddTypes" );
    valid_Vector.add ( "NodeAddDataTypes" );
    valid_Vector.add ( "NodeSubtractTypes" );
    valid_Vector.add ( "NodeSubtractDataTypes" );
    valid_Vector.add ( "NodeOutflowTypes" );
    valid_Vector.add ( "NodeOutflowDataTypes" );
    valid_Vector.add ( "NodeFlowThroughTypes" );
    valid_Vector.add ( "Interval" );
    valid_Vector.add ( "AnalysisStart" );
    valid_Vector.add ( "AnalysisEnd" );
    valid_Vector.add ( "Units" );
    valid_Vector.add ( "GainMethod" );
    valid_Vector.add ( "OutputTableID" );
    warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );    

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),warning );
		throw new InvalidCommandParameterException ( warning );
	}
    
    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
    // The command will be modified if changed...
	return (new AnalyzeNetworkPointFlow_JDialog ( parent, this, tableIDChoices )).ok();
}

// TODO SAM 2013-05-20 Perhaps move this to the DataTable class
/**
Find the records matching a string table value, ignoring case.  All matching records are returned.
@param table table to search
@param col column number
@param value string value to match
@param matchNull if true, match null value
*/
List<TableRecord> findTableRecordsWithValue ( DataTable table, int col, String value, boolean matchNull )
{
    int nRows = table.getNumberOfRecords();
    List<TableRecord> records = new Vector<TableRecord>();
    TableRecord rec;
    Object o;
    for ( int iRow = 0; iRow < nRows; iRow++ ) {
        try {
            rec = table.getRecord(iRow);
            o = rec.getFieldValue(col);
            if ( (o == null) ) {
                if ( matchNull ) {
                    records.add(rec);
                }
            }
            else if ( ((String)o).equalsIgnoreCase(value) ) {
                records.add(rec);
            }
        }
        catch ( Exception e ) {
            break;
        }
    }
    return records;
}

/**
Return the table that is read by this class when run in discovery mode.
*/
private DataTable getDiscoveryTable()
{
    return __table;
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList ()
{
    return __discoveryTSList;
}

/**
Get the nodes upstream of the given node inclusive of upstream known outflow nodes.
The nodes can then be used to 
*/
List<HydrologyNode> getNodesUpstreamToKnownFlows ( HydrologyNodeNetwork network, HydrologyNode startingNode, List<String>problems )
{
    List<HydrologyNode> upstreamNodes = new Vector();
    return upstreamNodes;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
*/
public List getObjectList ( Class c )
{   DataTable table = getDiscoveryTable();
    if ( (table != null) && (c == table.getClass()) ) {
        // Table request
        List v = new Vector();
        v.add ( table );
        return v;
    }
    // Check for time series
    List<TS> discovery_TS_Vector = getDiscoveryTSList ();
    if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
        return null;
    }
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS)
    TS datats = discovery_TS_Vector.get(0);
    // Use the most generic for the base class...
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return discovery_TS_Vector;
    }
    else {
        return null;
    }
}

/**
Initialize the network from a table of information.
@param table table containing network information
@param nodeIdColumnNum the table column number containing node identifiers
@param nodeNameColumnNum the table column number for node names
@param nodeTypeColumnNum the table column number for node types
@param downstreamNodeIdColumnNum the table column number containing downstream node identifiers
@param network being initialized
@param problems the list of problems from processing
*/
private void initializeNetworkFromTable ( DataTable table, int nodeIdColumnNum, int nodeNameColumnNum,
    int nodeTypeColumnNum, int nodeDistanceColumnNum, int downstreamNodeIdColumnNum,
    HydrologyNodeNetwork network, List<String> problems )
throws Exception
{   String routine = getClass().getName() + ".initializeNetworkFromTable";
    // First find a table node that has no downstream node
    List <TableRecord> records = findTableRecordsWithValue ( table, downstreamNodeIdColumnNum, "", true );
    if ( records.size() == 0 ) {
        problems.add ( "Cannot find end node (node with no downstream node) in table.  " +
            "Cannot initialize network from table." );
        return;
    }
    else if ( records.size() != 1 ) {
        problems.add ( "Found " + records.size() + " nodes with no downstream node in table. " +
            "Only one is expected.  Cannot initialize network from table." );
        return;
    }
    TableRecord rec = records.get(0);
    //network.getNodeHead().addUpstreamNodeID((String)rec.getFieldValue(nodeIdColumn));
    String nodeID = (String)rec.getFieldValue(nodeIdColumnNum);
    String nodeNameString = (String)rec.getFieldValue(nodeNameColumnNum);
    String downstreamNodeID = network.getNodeHead().getCommonID();
    // Use generic node type for now since table type will control behavior
    int nodeType = HydrologyNode.NODE_TYPE_UNKNOWN; //lookupNodeType ();
    // The following does all the work of determining computational order, etc.
    HydrologyNode node = network.addNode(nodeID, nodeType, null, // Upstream node ID is not yet known
        downstreamNodeID, // Most downstream node ID
        false, // Not a natural flow node (not significant in general network)
        false ); // Not an import (not significant in general network)
    node.setDescription(nodeNameString);
    if ( nodeDistanceColumnNum >= 0 ) {
        Double distance = (Double)rec.getFieldValue(nodeDistanceColumnNum);
        if ( distance != null ) {
            node.setStreamMile(distance);
        }
    }
    // Recursively add every node in the network moving upstream
    addNodesUpstreamOfNode ( table, network, nodeID, nodeIdColumnNum, nodeNameColumnNum,
        nodeTypeColumnNum, downstreamNodeIdColumnNum, problems );
    Message.printStatus(2, routine, "Initialized network with " + network.getNodeList().size() + " nodes.");
}

/**
Initialize output time series for each node.
*/
private List<TS> initializeNodeTimeSeries ( DataTable table, HydrologyNodeNetwork network,
    TimeInterval interval, DateTime analysisStart, DateTime analysisEnd, String units, List<String> problems )
{   List<TS> tslist = new Vector<TS>();
    String [] dataTypes = { "NodeInflow", "NodeAdd",
        "NodeSubtract", "NodeOutflow", "NodeUpstreamGain", "NodeStorage" };
    TS ts;
    for (HydrologyNode node = HydrologyNodeNetwork.getUpstreamNode(network.getNodeHead(), HydrologyNodeNetwork.POSITION_ABSOLUTE);
        node.getDownstreamNode() != null;
        node = HydrologyNodeNetwork.getDownstreamNode(node, HydrologyNodeNetwork.POSITION_COMPUTATIONAL)) {
        if (node == null) {
            break;
        }
        // Create output time series for each node.
        for ( int i = 0; i < dataTypes.length; i++ ) {
            try {
                String tsid = node.getCommonID() + ".." + dataTypes[i] + "." + interval;
                ts = TSUtil.newTimeSeries(tsid, true);
                ts.setIdentifier(tsid);
                ts.setDescription(node.getDescription());
                ts.setDate1(analysisStart);
                ts.setDate2(analysisEnd);
                ts.setDate1Original(analysisStart);
                ts.setDate2Original(analysisEnd);
                ts.setDataUnits(units);
                ts.setDataUnitsOriginal(units);
                ts.allocateDataSpace();
                tslist.add(ts);
            }
            catch ( Exception e ) {
                problems.add("Error initializing time series (" + e + ")." );
            }
        }
    }
    return tslist;
}

/**
Indicate whether the node type is of the analysis type (simple lookup in the array).
@param nodeAnalysisType the node type
@param nodeAnalysisTypes the node types that indicate how a node should be treated
@return true if the node type is matched in the array, false if not
*/
private boolean isNodeOfAnalysisType ( String nodeType, String [] nodeAnalysisTypes )
{
    for ( int i = 0; i < nodeAnalysisTypes.length; i++ ) {
        if ( nodeType.equalsIgnoreCase(nodeAnalysisTypes[i]) ) {
            return true;
        }
    }
    return false;
}

/**
Return the list of input time series that are used by a node.
*/
private List<TS> lookupAnalysisInputTimeSeries ( String nodeID, String [] tsDataTypes,
    TimeInterval interval, List<String> problems )
{   TSCommandProcessor processor = (TSCommandProcessor)getCommandProcessor();
    List<TS> tslist = new Vector<TS>();
    // Do a processor request for each pattern
    String tsid;
    String TSList = "" + TSListType.ALL_MATCHING_TSID;
    for ( int i = 0; i < tsDataTypes.length; i++ ) {
        tsid = nodeID + ".*." + tsDataTypes[i] + "." + interval;
        // Get the time series to process.  Allow TSID to be a pattern or specific time series...

        PropList request_params = new PropList ( "" );
        request_params.set ( "TSList", TSList );
        request_params.set ( "TSID", tsid );
        request_params.set ( "EnsembleID", null );
        CommandProcessorRequestResultsBean bean = null;
        try {
            bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
        }
        catch ( Exception e ) {
            problems.add ( "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
            "\", TSID=\"" + tsid + ") from processor." );
        }
        PropList bean_PropList = bean.getResultsPropList();
        Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
        if ( o_TSList == null ) {
            problems.add("Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
            "\" TSID=\"" + tsid + "\").");
        }
        else {
            tslist = (List)o_TSList;
            if ( tslist.size() == 0 ) {
                problems.add("No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
                "\" TSID=\"" + tsid + "\").");
            }
        }
    }
    return tslist;
}

/**
Look up the node type from the node identifier by searching the input table.
@param table the table containing network information
@param lookupColumnNum the column number for node identifiers
@param outputColumnNum the node type column number
@param lookupString the string to match in the lookup column
*/
private String lookupTableString ( DataTable table, int lookupColumnNum, String lookupString, int outputColumnNum )
{
    List<TableRecord> records = findTableRecordsWithValue ( table, lookupColumnNum, lookupString, false );
    if ( records.size() != 1 ) {
        return null;
    }
    else {
        try {
            return (String)(records.get(0).getFieldValue(outputColumnNum));
        }
        catch ( Exception e ) {
            return null;
        }
    }
}

// Use base class parseCommand()

/**
Lookup the output time series for the node for the requested data type.
@param nodeID node identifier, to match location in time series
@param dataType data type for output time series.
@return the output time series that matches
*/
private TS lookupNodeOutputTimeSeries ( String nodeID, String dataType, List<TS> outputTSList )
{
    for ( TS ts : outputTSList ) {
        if ( ts.getLocation().equals(nodeID) && ts.getDataType().equals(dataType) ) {
            return ts;
        }
    }
    return null;
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{   
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{
    runCommandInternal ( command_number, CommandPhaseType.DISCOVERY );
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
private void runCommandInternal ( int command_number, CommandPhaseType command_phase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = getClass().getName() + ".runCommandInternal",message = "";
	int warning_level = 2;
	String command_tag = "" + command_number;	
	int warning_count = 0;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(command_phase);
    if ( command_phase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTable ( null );
        setDiscoveryTSList( null );
    }

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

    String TableID = parameters.getValue ( "TableID" );
    String OutputTableID = parameters.getValue ( "OutputTableID" );
    String NodeIDColumn = parameters.getValue ( "NodeIDColumn" );
    String NodeNameColumn = parameters.getValue ( "NodeNameColumn" );
    String NodeTypeColumn = parameters.getValue ( "NodeTypeColumn" );
    String NodeDistanceColumn = parameters.getValue ( "NodeDistanceColumn" );
    String DownstreamNodeIDColumn = parameters.getValue ( "DownstreamNodeIDColumn" );
    String NodeAddTypes = parameters.getValue ( "NodeAddTypes" );
    String [] nodeAddTypes = new String[0];
    if ( (NodeAddTypes != null) && !NodeAddTypes.equals("") ) {
        if ( NodeAddTypes.indexOf(",") < 0 ) {
            nodeAddTypes = new String[1];
            nodeAddTypes[0] = NodeAddTypes.trim();
        }
        else {
            nodeAddTypes = NodeAddTypes.split(",");
            for ( int i = 0; i < nodeAddTypes.length; i++ ) {
                nodeAddTypes[i] = nodeAddTypes[i].trim();
            }
        }
    }
    String NodeAddDataTypes = parameters.getValue ( "NodeAddDataTypes" );
    String [] nodeAddDataTypes = new String[0];
    if ( (NodeAddDataTypes != null) && !NodeAddDataTypes.equals("") ) {
        if ( NodeAddDataTypes.indexOf(",") < 0 ) {
            nodeAddDataTypes = new String[1];
            nodeAddDataTypes[0] = NodeAddDataTypes.trim();
        }
        else {
            nodeAddDataTypes = NodeAddDataTypes.split(",");
            for ( int i = 0; i < nodeAddDataTypes.length; i++ ) {
                nodeAddDataTypes[i] = nodeAddDataTypes[i].trim();
            }
        }
    }
    String NodeSubtractTypes = parameters.getValue ( "NodeSubtractTypes" );
    String [] nodeSubtractTypes = new String[0];
    if ( (NodeSubtractTypes != null) && !NodeSubtractTypes.equals("") ) {
        if ( NodeSubtractTypes.indexOf(",") < 0 ) {
            nodeSubtractTypes = new String[1];
            nodeSubtractTypes[0] = NodeSubtractTypes.trim();
        }
        else {
            nodeSubtractTypes = NodeSubtractTypes.split(",");
            for ( int i = 0; i < nodeSubtractTypes.length; i++ ) {
                nodeSubtractTypes[i] = nodeSubtractTypes[i].trim();
            }
        }
    }
    String NodeSubtractDataTypes = parameters.getValue ( "NodeSubtractDataTypes" );
    String [] nodeSubtractDataTypes = new String[0];
    if ( (NodeSubtractDataTypes != null) && !NodeSubtractDataTypes.equals("") ) {
        if ( NodeSubtractDataTypes.indexOf(",") < 0 ) {
            nodeSubtractDataTypes = new String[1];
            nodeSubtractDataTypes[0] = NodeSubtractDataTypes.trim();
        }
        else {
            nodeSubtractDataTypes = NodeSubtractDataTypes.split(",");
            for ( int i = 0; i < nodeSubtractDataTypes.length; i++ ) {
                nodeSubtractDataTypes[i] = nodeSubtractDataTypes[i].trim();
            }
        }
    }
    String NodeOutflowTypes = parameters.getValue ( "NodeOutflowTypes" );
    String [] nodeOutflowTypes = new String[0];
    if ( (NodeOutflowTypes != null) && !NodeOutflowTypes.equals("") ) {
        if ( NodeOutflowTypes.indexOf(",") < 0 ) {
            nodeOutflowTypes = new String[1];
            nodeOutflowTypes[0] = NodeOutflowTypes.trim();
        }
        else {
            nodeOutflowTypes = NodeOutflowTypes.split(",");
            for ( int i = 0; i < nodeOutflowTypes.length; i++ ) {
                nodeOutflowTypes[i] = nodeOutflowTypes[i].trim();
            }
        }
    }
    String NodeOutflowDataTypes = parameters.getValue ( "NodeOutflowDataTypes" );
    String [] nodeOutflowDataTypes = new String[0];
    if ( (NodeOutflowDataTypes != null) && !NodeOutflowDataTypes.equals("") ) {
        if ( NodeOutflowDataTypes.indexOf(",") < 0 ) {
            nodeOutflowDataTypes = new String[1];
            nodeOutflowDataTypes[0] = NodeOutflowDataTypes.trim();
        }
        else {
            nodeOutflowDataTypes = NodeOutflowDataTypes.split(",");
            for ( int i = 0; i < nodeOutflowDataTypes.length; i++ ) {
                nodeOutflowDataTypes[i] = nodeOutflowDataTypes[i].trim();
            }
        }
    }
    String NodeFlowThroughTypes = parameters.getValue ( "NodeFlowThroughTypes" );
    String [] nodeFlowThroughTypes = new String[0];
    if ( (NodeFlowThroughTypes != null) && !NodeFlowThroughTypes.equals("") ) {
        if ( NodeFlowThroughTypes.indexOf(",") < 0 ) {
            nodeFlowThroughTypes = new String[1];
            nodeFlowThroughTypes[0] = NodeFlowThroughTypes.trim();
        }
        else {
            nodeFlowThroughTypes = NodeFlowThroughTypes.split(",");
            for ( int i = 0; i < nodeFlowThroughTypes.length; i++ ) {
                nodeFlowThroughTypes[i] = nodeFlowThroughTypes[i].trim();
            }
        }
    }
    String Interval = parameters.getValue ( "Interval" );
    TimeInterval interval = TimeInterval.parseInterval(Interval);
    String GainMethod = parameters.getValue ( "GainMethod" );
    NetworkGainMethodType gainMethod = NetworkGainMethodType.NONE;
    if ( (GainMethod != null) && !GainMethod.equals("") ) {
        gainMethod = NetworkGainMethodType.valueOfIgnoreCase(GainMethod);
        if ( gainMethod == null ) {
            gainMethod = NetworkGainMethodType.NONE;
        }
    }
    String AnalysisStart = parameters.getValue ( "AnalysisStart" );
    String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
    String Units = parameters.getValue ( "Units" );
    if ( Units == null ) {
        Units = "";
    }
    
    // Figure out the dates to use for the analysis.
    // Default of null means to analyze the full period.
    DateTime analysisStart = null;
    DateTime analysisEnd = null;
    
    try {
        if ( AnalysisStart != null ) {
            PropList request_params = new PropList ( "" );
            request_params.set ( "DateTime", AnalysisStart );
            CommandProcessorRequestResultsBean bean = null;
            try {
                bean = processor.processRequest( "DateTime", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting AnalysisStart DateTime(DateTime=" + AnalysisStart + ") from processor.";
                Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),routine, message );
                status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
                throw new InvalidCommandParameterException ( message );
            }
    
            PropList bean_PropList = bean.getResultsPropList();
            Object prop_contents = bean_PropList.getContents ( "DateTime" );
            if ( prop_contents == null ) {
                message = "Null value for AnalysisStart DateTime(DateTime=" +
                AnalysisStart + ") returned from processor.";
                Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
                throw new InvalidCommandParameterException ( message );
            }
            else {
                analysisStart = (DateTime)prop_contents;
            }
        }
    }
    catch ( Exception e ) {
        message = "AnalysisStart \"" + AnalysisStart + "\" is invalid.";
        Message.printWarning(warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
        throw new InvalidCommandParameterException ( message );
    }
    
    try {
        if ( AnalysisEnd != null ) {
            PropList request_params = new PropList ( "" );
            request_params.set ( "DateTime", AnalysisEnd );
            CommandProcessorRequestResultsBean bean = null;
            try {
                bean = processor.processRequest( "DateTime", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting AnalysisEnd DateTime(DateTime=" + AnalysisEnd + ") from processor.";
                Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
                throw new InvalidCommandParameterException ( message );
            }
    
            PropList bean_PropList = bean.getResultsPropList();
            Object prop_contents = bean_PropList.getContents ( "DateTime" );
            if ( prop_contents == null ) {
                message = "Null value for AnalysisStart DateTime(DateTime=" +
                AnalysisStart + "\") returned from processor.";
                Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
                throw new InvalidCommandParameterException ( message );
            }
            else {
                analysisEnd = (DateTime)prop_contents;
            }
        }
    }
    catch ( Exception e ) {
        message = "AnalysisEnd \"" + AnalysisEnd + "\" is invalid.";
        Message.printWarning(warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
        throw new InvalidCommandParameterException ( message );
    }
       
    // Get the table to process.

    DataTable table = null;
    if ( command_phase == CommandPhaseType.RUN ) {
        PropList request_params = null;
        CommandProcessorRequestResultsBean bean = null;
        if ( (TableID != null) && !TableID.equals("") ) {
            // Get the network table
            request_params = new PropList ( "" );
            request_params.set ( "TableID", TableID );
            try {
                bean = processor.processRequest( "GetTable", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting GetTable(TableID=\"" + TableID + "\") from processor.";
                Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report problem to software support." ) );
            }
            PropList bean_PropList = bean.getResultsPropList();
            Object o_Table = bean_PropList.getContents ( "Table" );
            if ( o_Table == null ) {
                message = "Unable to find table to process using TableID=\"" + TableID + "\".";
                Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that a table exists with the requested ID." ) );
            }
            else {
                table = (DataTable)o_Table;
            }
        }
    }

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}

	try {
    	// Process the network and create the output table...
        if ( command_phase == CommandPhaseType.RUN ) {
            // Create a new network from the table
            HydrologyNodeNetwork network = new HydrologyNodeNetwork(true);
            List<String> problems = new Vector<String>();
            int nodeIdColumnNum = table.getFieldIndex(NodeIDColumn);
            int nodeNameColumnNum = table.getFieldIndex(NodeNameColumn);
            int nodeTypeColumnNum = table.getFieldIndex(NodeTypeColumn);
            int nodeDistanceColumnNum = -1;
            if ( (NodeDistanceColumn != null) && !NodeDistanceColumn.equals("") ) {
                nodeDistanceColumnNum = table.getFieldIndex(NodeDistanceColumn);
            }
            int downstreamNodeIdColumnNum = table.getFieldIndex(DownstreamNodeIDColumn);
            initializeNetworkFromTable ( table, nodeIdColumnNum, nodeNameColumnNum,
                nodeTypeColumnNum, nodeDistanceColumnNum, downstreamNodeIdColumnNum, network, problems );
            // Create output time series for each node
            List<TS> outputTSList = initializeNodeTimeSeries ( table, network, interval,
                analysisStart, analysisEnd, Units, problems );
            // Do the point flow analysis
            analyzeNetworkPointFlow ( table, nodeIdColumnNum, nodeTypeColumnNum, nodeDistanceColumnNum,
                nodeAddTypes, nodeAddDataTypes, nodeSubtractTypes, nodeSubtractDataTypes,
                nodeOutflowTypes, nodeOutflowDataTypes, nodeFlowThroughTypes,
                network, outputTSList, interval, gainMethod, analysisStart, analysisEnd, problems );
            // Report problems
            for ( String problem : problems ) {
                Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, problem );
                status.addToLog ( command_phase, new CommandLogRecord(CommandStatusType.WARNING,
                    problem, "Report problem to software support." ) );
            }
            // Add the time series to the processor
            TSCommandProcessorUtil.appendTimeSeriesListToResultsList(processor, this, outputTSList);
            /*
            DataTable newTable = table.createCopy ( table, NewTableID, includeColumns,
                distinctColumns, columnMap );
            
            // Set the table in the processor...
            
            PropList request_params = new PropList ( "" );
            request_params.setUsingObject ( "Table", newTable );
            try {
                processor.processRequest( "SetTable", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting SetTable(Table=...) from processor.";
                Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Report problem to software support." ) );
            }
            */
        }
        else if ( command_phase == CommandPhaseType.DISCOVERY ) {
            // Create an empty output table and set the ID
            if ( (OutputTableID != null) && !OutputTableID.equals("") ) {
                table = new DataTable();
                table.setTableID ( OutputTableID );
                setDiscoveryTable ( table );
            }
            // Create an empty output time series list
            // TODO SAM 2013-05-22 Need to enable a "deep discovery" mode for commands where
            // table is always read and used to create time series with metadata
            List<TS> tslist = new Vector<TS>();
            setDiscoveryTSList(tslist);
        }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error analyzing point flow (" + e + ").";
		Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
        status.addToLog ( command_phase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Report problem to software support." ) );
		throw new CommandWarningException ( message );
	}
	
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
		throw new CommandWarningException ( message );
	}

    status.refreshPhaseSeverity(command_phase,CommandStatusType.SUCCESS);
}

/**
Set the table that is read by this class in discovery mode.
*/
private void setDiscoveryTable ( DataTable table )
{
    __table = table;
}

/**
Set the list of time series read in discovery phase.
*/
private void setDiscoveryTSList ( List<TS> discoveryTSList )
{
    __discoveryTSList = discoveryTSList;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
    String TableID = props.getValue( "TableID" );
    String NodeIDColumn = props.getValue( "NodeIDColumn" );
    String NodeNameColumn = props.getValue( "NodeNameColumn" );
    String NodeTypeColumn = props.getValue( "NodeTypeColumn" );
    String NodeDistanceColumn = props.getValue( "NodeDistanceColumn" );
    String DownstreamNodeIDColumn = props.getValue( "DownstreamNodeIDColumn" );
    String NodeAddTypes = props.getValue( "NodeAddTypes" );
    String NodeAddDataTypes = props.getValue( "NodeAddDataTypes" );
	String NodeSubtractTypes = props.getValue( "NodeSubtractTypes" );
	String NodeSubtractDataTypes = props.getValue( "NodeSubtractDataTypes" );
    String NodeOutflowTypes = props.getValue( "NodeOutflowTypes" );
    String NodeOutflowDataTypes = props.getValue( "NodeOutflowDataTypes" );
    String NodeFlowThroughTypes = props.getValue( "NodeFlowThroughTypes" );
	String Interval = props.getValue( "Interval" );
    String AnalysisStart = props.getValue( "AnalysisStart" );
    String AnalysisEnd = props.getValue( "AnalysisEnd" );
    String Units = props.getValue( "Units" );
    String GainMethod = props.getValue( "GainMethod" );
    String OutputTableID = props.getValue( "OutputTableID" );
	StringBuffer b = new StringBuffer ();
    if ( (TableID != null) && (TableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableID=\"" + TableID + "\"" );
    }
    if ( (NodeIDColumn != null) && (NodeIDColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NodeIDColumn=\"" + NodeIDColumn + "\"" );
    }
    if ( (NodeNameColumn != null) && (NodeNameColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NodeNameColumn=\"" + NodeNameColumn + "\"" );
    }
    if ( (NodeTypeColumn != null) && (NodeTypeColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NodeTypeColumn=\"" + NodeTypeColumn + "\"" );
    }
    if ( (NodeDistanceColumn != null) && (NodeDistanceColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NodeDistanceColumn=\"" + NodeDistanceColumn + "\"" );
    }
    if ( (DownstreamNodeIDColumn != null) && (DownstreamNodeIDColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DownstreamNodeIDColumn=\"" + DownstreamNodeIDColumn + "\"" );
    }
    if ( (NodeAddTypes != null) && (NodeAddTypes.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NodeAddTypes=\"" + NodeAddTypes + "\"" );
    }
    if ( (NodeAddDataTypes != null) && (NodeAddDataTypes.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NodeAddDataTypes=\"" + NodeAddDataTypes + "\"" );
    }
	if ( (NodeSubtractTypes != null) && (NodeSubtractTypes.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "NodeSubtractTypes=\"" + NodeSubtractTypes + "\"" );
	}
    if ( (NodeSubtractDataTypes != null) && (NodeSubtractDataTypes.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NodeSubtractDataTypes=\"" + NodeSubtractDataTypes + "\"" );
    }
    if ( (NodeOutflowTypes != null) && (NodeOutflowTypes.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NodeOutflowTypes=\"" + NodeOutflowTypes + "\"" );
    }
    if ( (NodeOutflowDataTypes != null) && (NodeOutflowDataTypes.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NodeOutflowDataTypes=\"" + NodeOutflowDataTypes + "\"" );
    }
    if ( (NodeFlowThroughTypes != null) && (NodeFlowThroughTypes.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NodeFlowThroughTypes=\"" + NodeFlowThroughTypes + "\"" );
    }
    if ( (Interval != null) && (Interval.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Interval=" + Interval );
    }
    if ( (AnalysisStart != null) && (AnalysisStart.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "AnalysisStart=\"" + AnalysisStart + "\"" );
    }
    if ( (AnalysisEnd != null) && (AnalysisEnd.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "AnalysisEnd=\"" + AnalysisEnd + "\"" );
    }
    if ( (Units != null) && (Units.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Units=\"" + Units + "\"" );
    }
    if ( (GainMethod != null) && (GainMethod.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "GainMethod=\"" + GainMethod + "\"" );
    }
    if ( (OutputTableID != null) && (OutputTableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputTableID=\"" + OutputTableID + "\"" );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}