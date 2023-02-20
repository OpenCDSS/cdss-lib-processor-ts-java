// CreateNetworkFromTable_Command - This class initializes, checks, and runs the CreateNetworkFromTable() command.

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

import javax.swing.JFrame;

import org.openwaterfoundation.network.NodeNetwork;

import cdss.domain.hydrology.network.HydrologyNode;
import cdss.domain.hydrology.network.HydrologyNodeNetwork;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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

/**
This class initializes, checks, and runs the CreateNetworkFromTable() command.
*/
public class CreateNetworkFromTable_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{
	
	/**
	 * NodeNetwork that is used for the network (is actually a HydrologyNodeNetwork)
	 */
	NodeNetwork network = null;

/**
Constructor.
*/
public CreateNetworkFromTable_Command ()
{	super();
	setCommandName ( "CreateNetworkFromTable" );
}

/**
Add nodes upstream of the node with the given identifier (which has already been added to the network).
The first call should pass the most downstream node in the network for "nodeID"
and subsequent calls will follow the tributaries upstream recursively (called from this method).
@param table the table listing network nodes
@param network the node network that is being created from the network table
@param nodeID identifier for node that has already been added (treated as downstream nodes for other nodes to add)
@param nodeIdColumnNum table column number (0+) for node identifiers
@param nodeNameColumnNum table column number (0+) for node names
@param nodeTypeColumnNum table column number (0+) for node types
@param downstreamNodeIdColumnNum table column number (0+) for downstream node identifiers
@param addedNodeRecordPosList position of added nodes in the network table, to allow check whether all were added
@param problems a list of problem strings that will be made visible in command status messages
*/
private void addNodesUpstreamOfNode ( DataTable table, HydrologyNodeNetwork network, String nodeID,
    int nodeIdColumnNum, int nodeNameColumnNum, int nodeTypeColumnNum, int downstreamNodeIdColumnNum,
    List<Integer> addedNodeRecordPosList, List<String> problems )
{   String routine = getClass().getSimpleName() + ".addNodesUpstreamOfNode";
    // Find the table records that have "nodeID" as the downstream node
	List<Integer> foundNodeRecords = new ArrayList<Integer>();
    List<TableRecord> records = findTableRecordsWithValue(table, downstreamNodeIdColumnNum, nodeID, false, foundNodeRecords);
    String upstreamNodeID = null;
    String nodeName;
    HydrologyNode node;
    int irec = -1;
    boolean isInstreamFlow = false;
    boolean isImport = false;
    for ( TableRecord record : records ) {
        // Add the node...
    	++irec;
    	nodeName = "";
        try {
            upstreamNodeID = (String)record.getFieldValue(nodeIdColumnNum);
            if ( nodeNameColumnNum >= 0 ) {
            	nodeName = ((String)record.getFieldValue(nodeNameColumnNum)).trim().replace("\r\n"," ").replace("\r"," ");
            }
        }
        catch ( Exception e ) {
            problems.add ( "Error getting upstream node ID - not adding upstream node \"" + upstreamNodeID + "\" (" + e + ").");
            continue;
        }
        node = network.addNode(upstreamNodeID, HydrologyNode.NODE_TYPE_UNKNOWN,
            null, // Upstream node ID is not yet known
            nodeID, // Downstream node ID
            isInstreamFlow, // Not a natural flow node (not significant in general network)
            isImport ); // Not an import (not significant in general network)
        node.setDescription(nodeName);
        // TODO sam 2017-05-31 add node group and stream mile
        addedNodeRecordPosList.add(foundNodeRecords.get(irec));
        Message.printStatus(2,routine,"Added node \"" + upstreamNodeID + "\" upstream of \"" + nodeID + "\", table record " + foundNodeRecords.get(irec) );
        // Recursively add the nodes upstream of the node just added
        addNodesUpstreamOfNode ( table, network, upstreamNodeID, nodeIdColumnNum, nodeNameColumnNum,
            nodeTypeColumnNum, downstreamNodeIdColumnNum, addedNodeRecordPosList, problems );
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
{	String NetworkID = parameters.getValue ( "NetworkID" );
	String NetworkName = parameters.getValue ( "NetworkName" );
	String DefaultDownstreamNodeID = parameters.getValue ( "DefaultDownstreamNodeID" );
	String TableID = parameters.getValue ( "TableID" );
    String NodeIDColumn = parameters.getValue ( "NodeIDColumn" );
    //String NodeTypeColumn = parameters.getValue ( "NodeTypeColumn" );
    String DownstreamNodeIDColumn = parameters.getValue ( "DownstreamNodeIDColumn" );
    //String NodeDistanceColumn = parameters.getValue ( "NodeDistanceColumn" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
    
    if ( (NetworkID == null) || (NetworkID.length() == 0) ) {
        message = "The network identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the network identifier." ) );
    }
    
    if ( (NetworkName == null) || (NetworkName.length() == 0) ) {
        message = "The network name must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the network name." ) );
    }
    
    if ( (DefaultDownstreamNodeID == null) || (DefaultDownstreamNodeID.length() == 0) ) {
        message = "The default downstream node identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the default downstream node identifier." ) );
    }

    if ( (TableID == null) || (TableID.length() == 0) ) {
        message = "The table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the table identifier." ) );
    }
    
    if ( (NodeIDColumn == null) || (NodeIDColumn.length() == 0) ) {
        message = "The node ID column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the node ID column." ) );
    }
    
    /* NodeTypeColumn is not currently required for basic network creation
    if ( (NodeTypeColumn == null) || (NodeTypeColumn.length() == 0) ) {
        message = "The node type column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the node type column." ) );
    }
    */
    
    if ( (DownstreamNodeIDColumn == null) || (DownstreamNodeIDColumn.length() == 0) ) {
        message = "The downstream node identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the downstream node identifier." ) );
    }
 
	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>(18);
	validList.add ( "NetworkID" );
	validList.add ( "NetworkName" );
	validList.add ( "DefaultDownstreamNodeID" );
    validList.add ( "TableID" );
    validList.add ( "NodeIDColumn" );
    validList.add ( "NodeNameColumn" );
    validList.add ( "NodeTypeColumn" );
    validList.add ( "NodeGroupColumn" );
    validList.add ( "NodeDistanceColumn" );
    validList.add ( "NodeWeightColumn" );
    validList.add ( "DownstreamNodeIDColumn" );
    validList.add ( "NodeAddTypes" );
    validList.add ( "NodeAddDataTypes" );
    validList.add ( "NodeSubtractTypes" );
    validList.add ( "NodeSubtractDataTypes" );
    validList.add ( "NodeOutflowTypes" );
    validList.add ( "NodeOutflowDataTypes" );
    validList.add ( "NodeFlowThroughTypes" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );    

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),warning );
		throw new InvalidCommandParameterException ( warning );
	}
    
    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Create the hash map that contains the NodeID and the array of input TSID for the node.
*/
HashMap<String, String[]> createInputTSIDMap ( DataTable table, int nodeidColumnNum, int [] tsidColumnsNum )
{   HashMap<String, String[]> nodeInputTSIDsMap = new HashMap<String,String []>();
    if ( tsidColumnsNum.length > 0 ) {
        TableRecord rec;
        ArrayList<String> nodeTsidColumns = new ArrayList<String>();
        String nodeID;
        for ( int i = 0; i < table.getNumberOfRecords(); i++ ) {
            try {
                nodeTsidColumns.clear();
                rec = table.getRecord(i);
                nodeID = rec.getFieldValueString(nodeidColumnNum);
                for ( int j = 0; j < tsidColumnsNum.length; j++ ) {
                    String tsid = rec.getFieldValueString(tsidColumnsNum[j]);
                    if ( (tsid != null) && (tsid.trim().length() != 0) ) {
                        // Have a TSID for the node
                        nodeTsidColumns.add(tsid);
                    }
                }
            }
            catch ( Exception e ) {
                continue;
            }
            if ( nodeTsidColumns.size() > 0 ) {
                // TODO SAM 2014-07-13 why doesn't the following work?
                //nodeInputTSIDsMap.put(nodeID, (String [])(nodeTsidColumns.toArray()));
                String [] a = new String[nodeTsidColumns.size()];
                for ( int j = 0; j < nodeTsidColumns.size(); j++ ) {
                    a[j] = nodeTsidColumns.get(j);
                }
                nodeInputTSIDsMap.put(nodeID,a);
            } 
        }
    }
    return nodeInputTSIDsMap;
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
	return (new CreateNetworkFromTable_JDialog ( parent, this, tableIDChoices )).ok();
}

// TODO SAM 2013-05-20 Perhaps move this to the DataTable class
/**
Find the records matching a string table value, ignoring case.  All matching records are returned.
@param table table to search
@param col column number
@param value string value to match
@param matchNull if true, match null value
*/
private List<TableRecord> findTableRecordsWithValue ( DataTable table, int col, String value, boolean matchNull, List<Integer> recordPosList )
{
    int nRows = table.getNumberOfRecords();
    List<TableRecord> records = new ArrayList<TableRecord>();
    TableRecord rec;
    Object o;
    for ( int iRow = 0; iRow < nRows; iRow++ ) {
        try {
            rec = table.getRecord(iRow);
            o = rec.getFieldValue(col);
            if ( (o == null) ) {
                if ( matchNull ) {
                    records.add(rec);
                    if ( recordPosList != null ) {
                    	recordPosList.add(new Integer(iRow));
                    }
                }
            }
            else if ( ((String)o).equalsIgnoreCase(value) ) {
                records.add(rec);
                if ( recordPosList != null ) {
                	recordPosList.add(new Integer(iRow));
                }
            }
        }
        catch ( Exception e ) {
            break;
        }
    }
    return records;
}

/**
Return the network that is read by this class when run in discovery mode.
*/
private NodeNetwork getDiscoveryNetwork()
{
    return this.network;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{   NodeNetwork network = getDiscoveryNetwork();
    if ( (network != null) && (c == network.getClass()) ) {
        // Network request
        List<T> v = new ArrayList<T>();
        v.add ( (T)network );
        return v;
    }
	return null;
}

/**
Initialize the network from a table of network node information.
@param table table containing network information
@param nodeIdColumnNum the table column number (0+) containing node identifiers
@param nodeNameColumnNum the table column number (0+) containing node names
@param nodeTypeColumnNum the table column number (0+) containing node types
@param nodeDistanceColumnNum the table column number (0+) containing node distance
@param downstreamNodeIdColumnNum the table column number (0+) containing downstream node identifiers
@param network network being initialized
@param problems the list of problems from processing
*/
private void initializeNetworkFromTable ( DataTable table, int nodeIdColumnNum, int nodeNameColumnNum,
    int nodeTypeColumnNum, int nodeDistanceColumnNum, int downstreamNodeIdColumnNum,
    HydrologyNodeNetwork network, String defaultDownstreamNodeId, List<String> problems )
throws Exception
{   String routine = "AnalyzeNetworkPointFlow_Command.initializeNetworkFromTable";
    // First find a table node that has no downstream node, which will be the end node
	List <Integer> recordPosList = new ArrayList<Integer>(); // position of found records
    List <TableRecord> records = findTableRecordsWithValue ( table, downstreamNodeIdColumnNum, "", true, recordPosList );
    String networkDownstreamNodeId = null; // Downstream node ID that was added, start of network creation
    List<Integer> addedNodeRecordPosList = new ArrayList<Integer>(); // Table records for added nodes, to do check
    if ( records.size() == 0 ) {
        problems.add ( "Cannot find end node (node with no downstream node) in table.  " +
            "Cannot initialize network from table." );
        return;
    }
    else if ( records.size() == 1 ) {
    	// Single end node found so use it as the downstream node without adding a default end node
    	// - this will result in the network, if output, having only the original nodes
        Integer endNodeRecordPos = recordPosList.get(0);
        TableRecord rec = records.get(endNodeRecordPos);
        //network.getNodeHead().addUpstreamNodeID((String)rec.getFieldValue(nodeIdColumn));
        String nodeID = "";
        if ( nodeIdColumnNum >= 0 ) {
        	nodeID = ((String)rec.getFieldValue(nodeIdColumnNum)).trim();
        }        
        String nodeNameString = "";
        if ( nodeNameColumnNum >= 0 ) {
        	nodeNameString = ((String)rec.getFieldValue(nodeNameColumnNum)).trim().replace("\r\n"," ").replace("\r"," ");
        }
        // Use generic node type for now since table type will control behavior
        //int nodeType = HydrologyNode.NODE_TYPE_UNKNOWN; //lookupNodeType ();
        // TODO SAM 2017-05-31 set the node type if features are added to enable (not sure if explicit end node will be needed)
        int nodeType = HydrologyNode.NODE_TYPE_END; //lookupNodeType ();
        // The following does all the work of determining computational order, etc.
        //HydrologyNode node = network.addNode(nodeID, nodeType, null, // Upstream node ID is not yet known
        networkDownstreamNodeId = nodeID;
        String upstreamNodeId = null; // Upstream node ID is not yet known
        String downstreamNodeId = network.getNodeHead().getCommonID(); // Connect to internal downstream network node
        boolean isNaturalFlowNode = false;
        boolean isImportNode = false;
        HydrologyNode node = network.addNode(networkDownstreamNodeId, nodeType, upstreamNodeId,
        	downstreamNodeId, // This is the most downstream node so no further downstream node
        	isNaturalFlowNode, // Not a natural flow node (not significant in general network)
        	isImportNode ); // Not an import (not significant in general network)
        node.setDescription(nodeNameString);
        if ( nodeDistanceColumnNum >= 0 ) {
            Double distance = (Double)rec.getFieldValue(nodeDistanceColumnNum);
            if ( distance != null ) {
                node.setStreamMile(distance);
            }
        }
    }
    else {
    	// Multiple nodes that don't have downstream and could be considered the end node.
    	// - set all downstream node ID to the default end node
    	// - add the default end node, and all will point to it via the downstream node
    	boolean doInsertDefaultEndNode = true;
    	if ( doInsertDefaultEndNode ) {
    		// Use the default end node specified by the command parameter
	        for ( TableRecord rec: records ) {
	            Message.printStatus ( 2, routine, "Node with no downstream node: \"" + rec.getFieldValueString(nodeIdColumnNum) + "\" - setting downstream to default \"" + defaultDownstreamNodeId + "\"");
	        	rec.setFieldValue(downstreamNodeIdColumnNum, defaultDownstreamNodeId);
	        }
	        String nodeNameString = "Default downstream end node inserted for end of network";
	        // Use unknown since it is a generated node and no risk of conflict with actual node type
	        int nodeType = HydrologyNode.NODE_TYPE_UNKNOWN; //lookupNodeType ();
	        networkDownstreamNodeId = defaultDownstreamNodeId;
	        String upstreamNodeId = null;
	        String downstreamNodeId = network.getNodeHead().getCommonID(); // Connect to internal downstream network node;
	        boolean isNaturalFlowNode = false;
	        boolean isImportNode = false;
	        // The following does all the work of determining computational order, etc.
	        // - no stream mile set since don't know for default end node
	        HydrologyNode node = network.addNode(networkDownstreamNodeId, nodeType, upstreamNodeId, // Upstream node ID is not yet known
	            downstreamNodeId, // Most downstream node ID
	            isNaturalFlowNode, // Not a natural flow node (not significant in general network)
	            isImportNode ); // Not an import (not significant in general network)
	        node.setDescription(nodeNameString);
		    // Add nodes above the specified node
	        // - Should cross-reference OK since the downstream node was connected above
		    // - Keep track of nodes that are added so that warning can be printed (only check for nodes in the table)
		    addNodesUpstreamOfNode ( table, network, networkDownstreamNodeId, nodeIdColumnNum, nodeNameColumnNum,
		        nodeTypeColumnNum, downstreamNodeIdColumnNum, addedNodeRecordPosList, problems );
    	}
    	else {
    		// Use the default end node that is already in the network from creation
    		// TODO sam 2017-05-31 enable this as the default if no DefauleDownstreamNodeID parameter is specified
    	}
    }
    Message.printStatus(2, routine, "Initialized network with " + network.getNodeList().size() + " nodes.");
    // Check whether all the table rows were added
    // - first sort the table record list
    Collections.sort(addedNodeRecordPosList);
    Integer addedNodeRecordPos = null, addedNodeRecordPosPrev = -1;
    for ( int irec = 0; irec < addedNodeRecordPosList.size(); irec++ ) {
    	addedNodeRecordPos = addedNodeRecordPosList.get(irec);
    	if ( (addedNodeRecordPos.intValue() - addedNodeRecordPosPrev.intValue()) > 1 ) {
    		// Have skipped nodes so generate warning for skipped records
        	for ( int jrec = (addedNodeRecordPosPrev.intValue() + 1); jrec < addedNodeRecordPos.intValue(); jrec++ ) {
        		problems.add("Table record " + (jrec + 1) + " with network node ID \"" + table.getRecord(jrec).getFieldValueString(nodeIdColumnNum) + "\" was not added to network.");
        	}
    	}
    	// Keep track of the previous record position that was added for check in the next iteration
    	addedNodeRecordPosPrev = addedNodeRecordPos;
    }
    if ( addedNodeRecordPos < (table.getNumberOfRecords() - 1) ) {
    	// Some records were left off at the end so generate warning
    	for ( int irec = (addedNodeRecordPos + 1); irec < table.getNumberOfRecords(); irec++ ) {
    		problems.add("Table record " + (irec + 1) + " with network node ID \"" + table.getRecord(irec).getFieldValueString(nodeIdColumnNum) + "\" was not added to network.");
    	}
    }
}

// Use base class parseCommand()

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
{	String routine = getClass().getSimpleName() + ".runCommandInternal", message = "";
	int warning_level = 2;
	String command_tag = "" + command_number;	
	int warning_count = 0;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(command_phase);
    if ( command_phase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryNetwork ( null );
    }

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

	String NetworkID = parameters.getValue ( "NetworkID" );
	String NetworkName = parameters.getValue ( "NetworkName" );
	String DefaultDownstreamNodeID = parameters.getValue ( "DefaultDownstreamNodeID" );
    String TableID = parameters.getValue ( "TableID" );
    String NodeIDColumn = parameters.getValue ( "NodeIDColumn" );
    String NodeNameColumn = parameters.getValue ( "NodeNameColumn" );
    String NodeTypeColumn = parameters.getValue ( "NodeTypeColumn" );
    String NodeDistanceColumn = parameters.getValue ( "NodeDistanceColumn" );
    String NodeWeightColumn = parameters.getValue ( "NodeWeightColumn" );
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
        	// - create an end node so there is something to connect downstream nodes
        	boolean createEndNode = true;
            HydrologyNodeNetwork network = new HydrologyNodeNetwork(NetworkID, NetworkName, createEndNode);
            List<String> problems = new ArrayList<String>();
            int nodeIdColumnNum = table.getFieldIndex(NodeIDColumn);
            int nodeNameColumnNum = table.getFieldIndex(NodeNameColumn);
            int nodeTypeColumnNum = -1;
            if ( (NodeTypeColumn != null) && !NodeTypeColumn.isEmpty() ) {
            	nodeTypeColumnNum = table.getFieldIndex(NodeTypeColumn);
            }
            int nodeDistanceColumnNum = -1;
            if ( (NodeDistanceColumn != null) && !NodeDistanceColumn.equals("") ) {
                nodeDistanceColumnNum = table.getFieldIndex(NodeDistanceColumn);
            }
            //int nodeWeightColumnNum = -1;
            if ( (NodeWeightColumn != null) && !NodeWeightColumn.equals("") ) {
                //nodeWeightColumnNum = table.getFieldIndex(NodeWeightColumn);
            }
            int downstreamNodeIdColumnNum = table.getFieldIndex(DownstreamNodeIDColumn);
            // Initialize the network from table information using column numbers for primary data
            initializeNetworkFromTable ( table, nodeIdColumnNum, nodeNameColumnNum,
                nodeTypeColumnNum, nodeDistanceColumnNum, downstreamNodeIdColumnNum, network, DefaultDownstreamNodeID, problems );
            // Report problems
            for ( String problem : problems ) {
                if ( problem.charAt(0) != ' ' ) {
                    // Message has not been logged before so log...
                    Message.printWarning ( 3, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, problem );
                }
                status.addToLog ( command_phase, new CommandLogRecord(CommandStatusType.WARNING,
                    problem, "Check log file for more details." ) );
            }
            // Add the network to the processor...
            
            PropList request_params = new PropList ( "" );
            request_params.setUsingObject ( "Network", network );
            try {
                processor.processRequest( "SetNetwork", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting SetNetwork(Network=...) from processor.";
                Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Report problem to software support." ) );
            }
        }
        else if ( command_phase == CommandPhaseType.DISCOVERY ) {
            // Create an empty output table and set the ID
            if ( (NetworkID != null) && !NetworkID.equals("") ) {
                HydrologyNodeNetwork network = new HydrologyNodeNetwork(NetworkID, NetworkName);
                setDiscoveryNetwork ( network );
            }
        }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error analyzing point flow (" + e + ").";
		Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
        status.addToLog ( command_phase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Check log file for details." ) );
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
Set the network that is created by this class in discovery mode (empty network with identifier and name).
*/
private void setDiscoveryNetwork ( NodeNetwork network )
{
    this.network = network;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		"NetworkID",
		"NetworkName",
		"DefaultDownstreamNodeID",
    	"TableID",
    	"NodeIDColumn",
    	"NodeNameColumn",
    	"NodeTypeColumn",
    	"NodeGroupColumn",
    	"NodeDistanceColumn",
    	"NodeWeightColumn",
    	"DownstreamNodeIDColumn",
    	"NodeAddTypes",
    	"NodeAddDataTypes",
		"NodeSubtractTypes",
		"NodeSubtractDataTypes",
    	"NodeOutflowTypes",
    	"NodeOutflowDataTypes",
    	"NodeFlowThroughTypes"
	};
	return this.toString(parameters, parameterOrder);
}

}