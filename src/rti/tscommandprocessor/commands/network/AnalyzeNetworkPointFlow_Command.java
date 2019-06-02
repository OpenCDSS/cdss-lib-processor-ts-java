// AnalyzeNetworkPointFlow_Command - This class initializes, checks, and runs the AnalyzeNetworkPointFlow() command.

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

import cdss.domain.hydrology.network.HydrologyNode;
import cdss.domain.hydrology.network.HydrologyNodeNetwork;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSUtil;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
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
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the AnalyzeNetworkPointFlow() command.
*/
public class AnalyzeNetworkPointFlow_Command extends AbstractCommand implements CommandDiscoverable, ObjectListProvider
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
The first call should pass the most downstream node in the network for "nodeID"
and subsequent calls will follow the tributaries upstream recursively (called from this method).
@param table the table listing network nodes
@param network the node network that is being created from the network table
@param nodeID identifier for node that has already been added (treated as downstream nodes for other nodes to add)
@param nodeIdColumnNum table column number (0+) for node identifiers
@param nodeNameColumnNum table column number (0+) for node names
@param nodeTypeColumnNum table column number (0+) for node types
@param downstreamNodeIdColumnNum table column number (0+) for downstream node identifiers
@param problems a list of problem strings that will be made visible in command status messages
*/
private void addNodesUpstreamOfNode ( DataTable table, HydrologyNodeNetwork network, String nodeID,
    int nodeIdColumnNum, int nodeNameColumnNum, int nodeTypeColumnNum, int downstreamNodeIdColumnNum,
    List<String> problems )
{   String routine = "AnalyzeNetworkPointFlow_Command.addNodesUpstreamOfNode";
    // Find the table records that have "nodeID" as the downstream node
    List<TableRecord> records = findTableRecordsWithValue(table, downstreamNodeIdColumnNum, nodeID, false);
    String upstreamNodeID;
    String nodeName;
    HydrologyNode node;
    for ( TableRecord record : records ) {
        // Add the node...
        try {
            upstreamNodeID = (String)record.getFieldValue(nodeIdColumnNum);
            nodeName = (String)record.getFieldValue(nodeNameColumnNum);
        }
        catch ( Exception e ) {
            problems.add ( "Error getting upstream node ID - not adding upstream nodes (" + e + ").");
            continue;
        }
        Message.printStatus(2,routine,"Adding node \"" + upstreamNodeID + "\" upstream of \"" + nodeID + "\"." );
        node = network.addNode(upstreamNodeID, HydrologyNode.NODE_TYPE_UNKNOWN,
            null, // Upstream node ID is not yet known
            nodeID, // Downstream node ID
            false, // Not a natural flow node (not significant in general network)
            false ); // Not an import (not significant in general network)
        node.setDescription(nodeName);
        // Recursively add the nodes upstream of the node just added
        addNodesUpstreamOfNode ( table, network, upstreamNodeID, nodeIdColumnNum, nodeNameColumnNum,
            nodeTypeColumnNum, downstreamNodeIdColumnNum, problems );
    }
}

/**
Analyze the network point flow.
@param nodeID identifier for node that has already been added (treated as downstream nodes for other nodes to add)
@param nodeIdColumnNum table column number (0+) for node identifiers
@param nodeTypeColumnNum table column number (0+) for node types
@param nodeDistanceColumnNum table column number (0+) for node distance
@param nodeWeightColumnNum table column number (0+) for node distance
@param nodeAddTypes node types where time series should be added at the node
@param nodeAddDataTypes time series data types where time series should be added at the node
@param nodeSubtractTypes node types where time series should be subtracted at the node
@param nodeSubtractDataTypes time series data types where time series should be subtracted at the node
@param nodeOutflowTypes node types where time series should be set at the node
@param nodeOutflowDataTypes time series data types where time series should be set at the node
@param nodeFlowThroughTypes node types where node inflow should continue as outflow
@param network the node network that is was created from the network table
@param nodeInputTSIDsMap map of of input time series corresponding to each node
@param outputTSList list of time series created by the analysis, representing mass balance at each node
@param interval the time series interval for all time series in the analysis
@param gainMethod the gain method used when computing gains
@param analysisStart the date/time to start the analysis
@param analysisEnd the date/time to end the analysis
@param problems a list of problem strings that will be made visible in command status messages
*/
private void analyzeNetworkPointFlow ( DataTable table, int nodeIdColumnNum, int nodeTypeColumnNum,
    int nodeDistanceColumnNum, int nodeWeightColumnNum,
    String [] nodeAddTypes, String [] nodeAddDataTypes,
    String [] nodeSubtractTypes, String [] nodeSubtractDataTypes,
    String [] nodeOutflowTypes, String [] nodeOutflowDataTypes,
    String [] nodeFlowThroughTypes,
    HydrologyNodeNetwork network, HashMap<String, String[]> nodeInputTSIDsMap, List<TS> outputTSList,
    TimeInterval interval, NetworkGainMethodType gainMethod,
    DateTime analysisStart, DateTime analysisEnd, List<String> problems )
{   String routine = "AnalyzeNetworkPointFlow_Command.analyzeNetworkPointFlow";
    TS ts;
    String nodeID;
    String nodeType;
    List<TS> tslist;
    TS nodeInflowTS;
    TS nodeOutflowTS;
    TS nodeAddTS;
    TS nodeSubtractTS;
    TS nodeUpstreamGainTS;
    TS nodeUpstreamReachGainTS;
    TS nodeInflowWithGainTS;
    TS nodeOutflowWithGainTS;
    TS nodeStorageTS;
    String description;
    List<HydrologyNode> upstreamNodeList;
    for (HydrologyNode node = HydrologyNodeNetwork.getUpstreamNode(network.getNodeHead(), HydrologyNodeNetwork.POSITION_ABSOLUTE);
        node.getDownstreamNode() != null;
        node = HydrologyNodeNetwork.getDownstreamNode(node, HydrologyNodeNetwork.POSITION_COMPUTATIONAL)) {
        nodeID = node.getCommonID();
        nodeType = lookupTableString ( table, nodeIdColumnNum, nodeID, nodeTypeColumnNum );
        //nodeDistance = lookupTableString ( table, nodeIDColumnNum, nodeID, nodeTypeColumnNum );
        Message.printStatus(2,routine,"Analyzing node \"" + nodeID + "\", type \"" + nodeType + "\"" );
        if ( nodeType == null ) {
            // End node
            break;
        }
        // Get the output time series for the node, which were previously created by this command
        nodeInflowTS = lookupNodeOutputTimeSeries ( nodeID, "NodeInflow", interval, outputTSList );
        nodeOutflowTS = lookupNodeOutputTimeSeries ( nodeID, "NodeOutflow", interval, outputTSList );
        nodeAddTS = lookupNodeOutputTimeSeries ( nodeID, "NodeAdd", interval, outputTSList );
        nodeSubtractTS = lookupNodeOutputTimeSeries ( nodeID, "NodeSubtract", interval, outputTSList );
        nodeUpstreamGainTS = lookupNodeOutputTimeSeries ( nodeID, "NodeUpstreamGain", interval, outputTSList );
        nodeUpstreamReachGainTS = lookupNodeOutputTimeSeries ( nodeID, "NodeUpstreamReachGain", interval, outputTSList );
        nodeInflowWithGainTS = lookupNodeOutputTimeSeries ( nodeID, "NodeInflowWithGain", interval, outputTSList );
        nodeOutflowWithGainTS = lookupNodeOutputTimeSeries ( nodeID, "NodeOutflowWithGain", interval, outputTSList );
        nodeStorageTS = lookupNodeOutputTimeSeries ( nodeID, "NodeStorage", interval, outputTSList );
        // Save the description because want to reset at the end
        description = nodeInflowTS.getDescription();
        // Now process the time series based on the time series type
        if ( isNodeOfAnalysisType(nodeType, nodeOutflowTypes) ) {
            // Set the time series outflow to the matched input time series, for example for a stream gage
            tslist = lookupAnalysisInputTimeSeries ( nodeID, nodeOutflowDataTypes, interval, nodeInputTSIDsMap, problems );
            if ( tslist.size() != 1 ) {
                problems.add("Expecting 1 time series for node \"" + nodeID + "\" but have " +
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
                // Encountering these nodes also allow calculation of gain/loss up to the the previous known flow point
                //
                // Compute the reach gain/loss at the downstream known point flow as the error between
                // the upstream node's outflow, and the known flow.  This can be computed even if the gain/loss
                // for each node is not computed, and is used as input when distributing gain/loss to nodes in the reach
                // Set the reach gain at the known point flow to the known flow minus the outflow
                List<HydrologyNode> upstreamNodes = node.getUpstreamNodes();
                if ( (upstreamNodes != null) && (upstreamNodes.size() > 0) ) {
                    try {
                        Message.printStatus(2, routine, "Initializing known point flow \"" + node.getCommonID() +
                            "\" reach gain to the node's outflow.");
                        TSUtil.setFromTS(nodeUpstreamReachGainTS, nodeOutflowTS);
                    }
                    catch ( Exception e ) {
                        problems.add("Error initializing \"" + node.getCommonID() +
                           "\" reach gain/loss time series to known outflow (" + e + ") - not computing gain/loss.");
                        continue;
                    }
                    for ( HydrologyNode upstreamNode : upstreamNodes ) {
                        TS upstreamNodeOutflowTS = lookupNodeOutputTimeSeries ( upstreamNode.getCommonID(), "NodeOutflow",
                            interval, outputTSList );
                        // Subtract
                        List<TS> tslistSubtract = new Vector<TS>();
                        tslistSubtract.add(upstreamNodeOutflowTS);
                        try {
                            Message.printStatus(2, routine, "Subtracting upstream outflow \"" + upstreamNode.getCommonID() +
                                "\" from reach gain.");
                            Message.printStatus(2, routine, "For " + analysisStart + " known outflow from \"" +
                                node.getCommonID() + "=" + nodeOutflowTS.getDataValue(analysisStart));
                            Message.printStatus(2, routine, "For " + analysisStart + " upstream outflow from \"" +
                                upstreamNode.getCommonID() + "=" + upstreamNodeOutflowTS.getDataValue(analysisStart));
                            TSUtil.subtract(nodeUpstreamReachGainTS,tslistSubtract,TSUtil.SET_MISSING_IF_ANY_MISSING);
                            Message.printStatus(2, routine, "For " + analysisStart + " calculated error (gain/loss)=" +
                                nodeUpstreamReachGainTS.getDataValue(analysisStart));
                        }
                        catch ( Exception e ) {
                            problems.add("Error subtracting upstream node outflow to reach gain/loss time series (" + e +
                                ") - not computing gain/loss.");
                            continue;
                        }
                    }
                }
                if ( (gainMethod == NetworkGainMethodType.WEIGHT) || (gainMethod == NetworkGainMethodType.DISTANCE) ) {
                    // Redistribute the remainder error flowing into the node with known outflow.
                    // Since the network is being processed upstream to downstream, any
                    // occurrence of a known outflow node needs to be processed, compared to its upstream node.
                    // For now, only allow computing gain when there are no branches in the network
                    Message.printStatus(2, routine, "Processing gain/loss in reach above \"" + node.getCommonID() + "\"" );
                    // For now only support gains on linear reaches.
                    upstreamNodeList = node.getUpstreamNodes();
                    if ( (upstreamNodeList == null) || (upstreamNodeList.size() == 0) ) {
                        // Known flow at the "headwater" so no gain/loss
                        TSUtil.setConstant(nodeUpstreamGainTS, 0.0);
                        TSUtil.setConstant(nodeUpstreamReachGainTS, 0.0);
                    }
                    else if ( node.getUpstreamNodes().size() > 1 ) {
                        // Not yet supported, although logic below may be close if some additional intricacies of gain/loss
                        // on branches can be figured out
                        problems.add ( "Gain/loss calculations are not yet supported on branching networks.");
                    }
                    else {
                        // Single upstream node so able to compute gain/loss
                        // Get the list of upstream nodes, starting with the current node (works because above checks
                        // for branching network limit recursion).
                        int problemsSize = problems.size();
                        upstreamNodeList = analyzeNetworkPointFlow_GetUpstreamNodesForGainLoss ( null, node,
                            table, nodeIdColumnNum, nodeTypeColumnNum, nodeOutflowTypes, problems );
                        if ( problems.size() > problemsSize ) {
                            // Had issues getting the upstream node list so don't continue with the gain/loss calculations.
                            continue;
                        }
                        // Get the weights used to distribute the gain/loss.
                        double [] weights = analyzeNetworkPointFlow_GetReachNodeWeights( gainMethod,
                            upstreamNodeList, table, nodeIdColumnNum, nodeWeightColumnNum, nodeDistanceColumnNum, problems );
                        if ( weights != null ) {
                            analyzeNetworkPointFlow_CalculateReachGainLoss ( upstreamNodeList, gainMethod, weights,
                                outputTSList, interval, analysisStart, analysisEnd, problems );
                        }
                    }
                }
            }
        }
        else {
            // 1. First set the time series to the sum of the upstream outflow time series...
            //List<HydrologyNode> upstreamNodeList = new Vector();
            //network.findUpstreamFlowNodes(upstreamNodeList, node, true);
            upstreamNodeList = node.getUpstreamNodes();
            Message.printStatus(2,routine,"Setting node \"" + nodeID + "\" inflow to total of upstream node outflows.");
            if ( (upstreamNodeList == null) || (upstreamNodeList.size() == 0) ) {
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
                        "NodeOutflow", interval, outputTSList );
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
            // 2. Now perform specific actions to adjust the node's outflow
            if ( isNodeOfAnalysisType(nodeType, nodeAddTypes) ) {
                // Add the input time series at the node
                tslist = lookupAnalysisInputTimeSeries ( nodeID, nodeAddDataTypes, interval, nodeInputTSIDsMap, problems );
                if ( tslist.size() != 1 ) {
                    problems.add("Expecting 1 time series for node \"" + nodeID + "\" but have " +
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
                tslist = lookupAnalysisInputTimeSeries ( nodeID, nodeSubtractDataTypes, interval, nodeInputTSIDsMap, problems );
                if ( tslist.size() != 1 ) {
                    problems.add("Expecting 1 time series for node \"" + nodeID + "\" but have " +
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
         // Reset descriptions so as to not have all the extra process notes
        nodeInflowTS.setDescription(description);
        nodeOutflowTS.setDescription(description);
        nodeAddTS.setDescription(description);
        nodeSubtractTS.setDescription(description);
        nodeUpstreamGainTS.setDescription(description);
        nodeUpstreamReachGainTS.setDescription(description);
        nodeInflowWithGainTS.setDescription(description);
        nodeOutflowWithGainTS.setDescription(description);
        nodeStorageTS.setDescription(description); 
    }
}

/**
Calculate the gain/loss time series for nodes in a reach (between known point flows), for the entire analysis period.
@param reachUpstreamNodeList list of nodes determined from analyzeNetworkPointFlow_GetUpstreamNodesForGainLoss() method, which
includes the downstream known flow up to but not including upstream known flow points (since those nodes will be processed
as part of their upstream reaches).
@param gainMethod the gain method used when computing gains
@param weights the weights for each node to be used to distribute reach gain (will be normalized in this method
to balance the overall reach gain/loss)
@param outputTSList list of time series created by the analysis, representing mass balance at each node
@param interval the time series interval for all time series in the analysis
@param analysisStart the date/time to start the analysis
@param analysisEnd the date/time to end the analysis
@param problems a list of problem strings that will be made visible in command status messages
*/
private void analyzeNetworkPointFlow_CalculateReachGainLoss (
    List<HydrologyNode> reachUpstreamNodeList, NetworkGainMethodType gainMethod, double [] weights,
    List<TS> outputTSList, TimeInterval interval, DateTime analysisStart, DateTime analysisEnd, List<String> problems )
{   String routine = "analyzeNetworkPointFlow_CalculateReachGainLoss", message;
    double [] gainFraction = new double[reachUpstreamNodeList.size()];
    double weightsTotal = 0.0;
    int intervalBase = interval.getBase();
    int intervalMult = interval.getMultiplier();
    // Distance and set weights are now equivalent.  Normalize based on the total weights
    weightsTotal = 0.0;
    for ( int iw = 0; iw < weights.length; iw++ ) {
        weightsTotal += weights[iw];
    }
    for ( int iw = 0; iw < weights.length; iw++ ) {
        gainFraction[iw] = weights[iw]/weightsTotal;
    }
    for ( int iw = 0; iw < weights.length; iw++ ) {
        if ( gainMethod == NetworkGainMethodType.WEIGHT ) {
            Message.printStatus(2,routine,"Node \"" + reachUpstreamNodeList.get(iw).getCommonID() + "\" weight=" +
                StringUtil.formatString(weights[iw],"%.6f") +
                " gainFraction=" + StringUtil.formatString(gainFraction[iw],"%.6f") );
        }
        else if ( gainMethod == NetworkGainMethodType.DISTANCE ) {
            Message.printStatus(2,routine,"Node \"" + reachUpstreamNodeList.get(iw).getCommonID() + "\" delta distance=" +
                StringUtil.formatString(weights[iw],"%.6f") + " gainFraction=" +
                StringUtil.formatString(gainFraction[iw],"%.6f") );
        }
    }
    // Figure out the time series for each node corresponding to the weights,
    // which are in the same order as upstreamNodeList
    TS [] nodeGainTS = new TS[weights.length];
    TS [] reachGainTS = new TS[weights.length];
    TS [] inflowTS = new TS[weights.length];
    TS [] inflowWithGainTS = new TS[weights.length];
    TS [] outflowTS = new TS[weights.length];
    TS [] outflowWithGainTS = new TS[weights.length];
    int iNode = -1;
    for ( HydrologyNode node : reachUpstreamNodeList ) {
        ++iNode;
        inflowTS[iNode] = lookupNodeOutputTimeSeries ( node.getCommonID(), "NodeInflow", interval, outputTSList );
        nodeGainTS[iNode] = lookupNodeOutputTimeSeries ( node.getCommonID(), "NodeUpstreamGain", interval, outputTSList );
        reachGainTS[iNode] = lookupNodeOutputTimeSeries ( node.getCommonID(), "NodeUpstreamReachGain", interval, outputTSList );
        inflowWithGainTS[iNode] = lookupNodeOutputTimeSeries ( node.getCommonID(), "NodeInflowWithGain", interval, outputTSList );
        outflowTS[iNode] = lookupNodeOutputTimeSeries ( node.getCommonID(), "NodeOutflow", interval, outputTSList );
        outflowWithGainTS[iNode] = lookupNodeOutputTimeSeries ( node.getCommonID(), "NodeOutflowWithGain", interval, outputTSList );
    }
    // Have to process each timestep because the gain/loss will vary
    double totalGainValue, reachGainValue, nodeGainValue, inflowValue, outflowValue, newInflowValue, newOutflowValue;
    int iw, iwMax = reachUpstreamNodeList.size() - 1;
    HydrologyNode node;
    List<HydrologyNode> nodeUpstreamNodeList;
    int dl = 1;
    for ( DateTime dt = new DateTime(analysisStart); dt.lessThanOrEqualTo(analysisEnd);
        dt.addInterval(intervalBase,intervalMult) ) {
        // Starting with the knownFlowNode, go upstream to the top-most known point flow in the reach
        // TODO SAM 2013-07-08 This will work for non-branching reaches but need to figure out branches - recursion
        // 1. Get the reach gain (+) or loss (-) - this will be at the most downstream node (time series position 0)
        totalGainValue = reachGainTS[0].getDataValue(dt);
        if ( reachGainTS[0].isDataMissing(totalGainValue)) {
            message = "Reach gain above node " + reachGainTS[0].getLocation() + " is missing for " + dt +
                ".  Cannot distribute gain/loss.";
            problems.add(message);
            Message.printWarning(3,routine,message);
            continue;
        }
        if ( Message.isDebugOn ) {
            Message.printDebug(dl,routine,"Reach gain above node " + reachGainTS[0].getIdentifier() + "=" +
                StringUtil.formatString(totalGainValue,"%.2f") + " for " + dt + " - distributing");
        }
        reachGainValue = 0.0; // Cumulative for reach
        for ( iw = iwMax; iw >= 0; iw-- ) {
            // First set the gain/loss value distributed to each node, from most upstream in reach to downstream known
            // point flow (values will not be reset because computed previously as error)
            node = reachUpstreamNodeList.get(iw);
            nodeUpstreamNodeList = node.getUpstreamNodes();
            if ( nodeUpstreamNodeList.size() != 1 ) {
                problems.add ( "Logic is not enabled to compute gain/loss on branching networks." );
                return;
            }
            // Set the gain for the specific node, based on distance, weight, etc.
            nodeGainValue = totalGainValue*gainFraction[iw];
            nodeGainTS[iw].setDataValue(dt, nodeGainValue);
            reachGainValue += nodeGainValue;
            if ( Message.isDebugOn ) {
                Message.printDebug(1,routine,"Node \"" + node.getCommonID() + "\" gain value = " + totalGainValue + "*" +
                    StringUtil.formatString(gainFraction[iw],"%.6f") + "=" + StringUtil.formatString(nodeGainValue,"%.6f") +
                    ", reachGainValue=" + StringUtil.formatString(reachGainValue,"%.6f") );
            }
            if ( iw != 0 ) {
                // Only set for nodes upstream of known point flow
                reachGainTS[iw].setDataValue(dt,reachGainValue);
            }
            else {
                // Reach gain at bottom node was set previously
                // Do a check to see if the reach gain value is the original for known flow
                if ( Math.abs(reachGainValue - reachGainTS[iw].getDataValue(dt)) > .0001 ) {
                    // TODO SAM 2013-07-08 should add this to table output as cross-check
                    message = " Known point flow location \"" + reachGainTS[iw].getLocation() +
                    "\" " + dt + " calculated cumulative gain " + StringUtil.formatString(reachGainValue,"%.2f") +
                    " is not the same as the original error value " +
                    StringUtil.formatString(reachGainTS[iw].getDataValue(dt),"%.2f");
                    problems.add(message);
                    Message.printWarning(3,routine,message);
                }
            }
            // Adjust the previous inflow and outflow by the cumulative reach gain/loss to the node, essentially a shift
            inflowValue = inflowTS[iw].getDataValue(dt);
            if ( !reachGainTS[iw].isDataMissing(reachGainValue) && !inflowTS[iw].isDataMissing(inflowValue) ) {
                newInflowValue = inflowValue + reachGainValue;
                if ( iw == 0 ) {
                    // Do a check to see if the new inflow value is the original for known flow
                    /* Does not make sense
                    if ( Math.abs(newInflowValue - inflowValue) > .0001 ) {
                        // TODO SAM 2013-07-08 should add this to table output as cross-check
                        message = " Known point flow location \"" + inflowTS[iw].getLocation() +
                            "\" " + dt + " calculated inflow " + StringUtil.formatString(newInflowValue,"%.2f") +
                            " is not the same as the original value " + StringUtil.formatString(inflowValue,"%.2f");
                        problems.add(message);
                        Message.printWarning(3,routine,message);
                    }
                    */
                    // Value should be the same since known point
                    inflowWithGainTS[iw].setDataValue(dt,inflowValue);
                }
                else {
                    // Only set non-known flow values
                    inflowWithGainTS[iw].setDataValue(dt,newInflowValue);
                    if ( newInflowValue < 0.0 ) {
                        // TODO SAM 2013-07-08 should add this to table output as cross-check
                        //problems.add("Point flow location \"" + inflowTS[iw].getLocation() +
                        //    "\" " + dt + " calculated inflow " + newInflowValue + " is negative." );
                    }
                }
            }
            outflowValue = outflowTS[iw].getDataValue(dt);
            if ( !reachGainTS[iw].isDataMissing(reachGainValue) && !outflowTS[iw].isDataMissing(outflowValue) ) {
                newOutflowValue = outflowValue + reachGainValue;
                if ( iw == 0 ) {
                    // Do a check to see if the new outflow value is the original for known flow
                    /* Does not make sense
                    if ( Math.abs(newOutflowValue - outflowValue) > .0001 ) {
                        // TODO SAM 2013-07-08 should add this to table output as cross-check
                        message = " Known point flow location \"" + outflowTS[iw].getLocation() +
                            "\" " + dt + " calculated outflow " + StringUtil.formatString(newOutflowValue,"%.2f") +
                            " is not the same as the original value " + StringUtil.formatString(outflowValue,"%.2f");
                        problems.add(message);
                        Message.printWarning(3,routine,message);
                    }
                    */
                    // Value should be the same since a known point
                    outflowWithGainTS[iw].setDataValue(dt,outflowValue);
                }
                else {
                    // Only set non-known flow values
                    outflowWithGainTS[iw].setDataValue(dt,newOutflowValue);
                    if ( newOutflowValue < 0.0 ) {
                        // TODO SAM 2013-07-08 should add this to table output as cross-check
                        //problems.add("Point flow location \"" + outflowTS[iw].getLocation() +
                        //    "\" " + dt + " calculated outflow " + newOutflowValue + " is negative." );
                    }
                }
            }
        }
    }
}

/**
Get the weights to use for nodes upstream of a known flow point.
@param gainMethod the gain method used when computing gains
@param reachUpstreamNodeList list of nodes determined from analyzeNetworkPointFlow_GetUpstreamNodesForGainLoss() method,
which includes the downstream known flow up to but not including upstream known flow points (since those nodes
will be processed as part of their upstream reaches).
@param table the table listing network nodes
@param nodeIdColumnNum table column number (0+) for node identifiers
@param nodeWeightColumnNum table column number (0+) for node weight
@param nodeDistanceColumnNum table column number (0+) for node distance
@param problems a list of problem strings that will be made visible in command status messages
@return the weights for each node to be used to distribute reach gain - the weights are not yet normalized to a fraction
*/
private double [] analyzeNetworkPointFlow_GetReachNodeWeights ( NetworkGainMethodType gainMethod,
    List<HydrologyNode> reachUpstreamNodeList,
    DataTable table, int nodeIdColumnNum, int nodeWeightColumnNum, int nodeDistanceColumnNum,
    List<String> problems )
{   // If GainMethod=Weight and no NodeWeightColumn has been specified, the weight for each node will be 1.0
    double [] weights = new double[reachUpstreamNodeList.size()];
    double [] distance = new double[reachUpstreamNodeList.size()];
    int iNode = -1;
    TableRecord rec = null;
    for ( HydrologyNode nodeInReach : reachUpstreamNodeList ) {
        ++iNode;
        // Now figure out how to distribute the gain/loss among nodes in the network
        if ( (gainMethod == NetworkGainMethodType.WEIGHT) || (gainMethod == NetworkGainMethodType.DISTANCE_WEIGHT) ) {
            if ( nodeWeightColumnNum < 0 ) {
                weights[iNode] = 1.0;
            }
            else {
                // Get the weight from the table
                try {
                    rec = table.getRecord(nodeIdColumnNum,nodeInReach.getCommonID());
                    if ( rec == null ) {
                        problems.add ( "Cannot find network table row matching node \"" + nodeInReach.getCommonID() +
                            "\" - cannot get weight to calculate gain/loss).");
                        return null;
                    }
                }
                catch ( Exception e ) {
                    problems.add ( "Cannot find network table row matching node \"" + nodeInReach.getCommonID() +
                        "\" (" + e + ") - cannot get weight to calculate gain/loss).");
                    return null;
                }
                Object weightO = null;
                try {
                    weightO = rec.getFieldValue(nodeWeightColumnNum);
                    if ( weightO == null ) {
                        problems.add ( "Cannot determine weight for node \"" + nodeInReach.getCommonID() +
                            "\" - cannot calculate gain/loss).");
                        return null;
                    }
                    else {
                        weights[iNode] = (Double)weightO;
                    }
                }
                catch ( Exception e ) {
                    problems.add ( "Cannot determine weight for node \"" + nodeInReach.getCommonID() + "\" - cannot calculate gain/loss).");
                    return null;
                }
            }
        }
        else if ( (gainMethod == NetworkGainMethodType.DISTANCE) || (gainMethod == NetworkGainMethodType.DISTANCE_WEIGHT) ) {
            // The weight is the difference in distance between the current node and its upstream node
            double distance2, distance2up;
            // First get the distance for the current node
            try {
                rec = table.getRecord(nodeIdColumnNum,nodeInReach.getCommonID());
                if ( rec == null ) {
                    problems.add ( "Cannot find network table row matching node \"" + nodeInReach.getCommonID() +
                        "\" - cannot get distance to calculate gain/loss).");
                    return null;
                }
            }
            catch ( Exception e ) {
                problems.add ( "Cannot find network table row matching node \"" + nodeInReach.getCommonID() +
                    "\" (" + e + ") - cannot get distance to calculate gain/loss).");
                return null;
            }
            Object distanceO = null;
            try {
                distanceO = rec.getFieldValue(nodeDistanceColumnNum);
                if ( distanceO == null ) {
                    problems.add ( "Cannot determine distance from network table column [" + nodeDistanceColumnNum +
                        "] for node \"" + nodeInReach.getCommonID() + "\" - cannot calculate gain/loss).");
                    return null;
                }
                else {
                    distance2 = (Double)distanceO;
                }
            }
            catch ( Exception e ) {
                problems.add ( "Cannot determine distance from network table column [" + nodeDistanceColumnNum +
                    "] for node \"" + nodeInReach.getCommonID() + "\" (" + e + ") - cannot calculate gain/loss).");
                return null;
            }
            // Now get the distance for the upstream node
            try {
                List<HydrologyNode> upstreamNodes = nodeInReach.getUpstreamNodes();
                if ( upstreamNodes.size() != 1 ) {
                    problems.add ( "Node \"" + nodeInReach.getCommonID() +
                    "\" has " + upstreamNodes.size() + " upstream nodes (expecting 1) - " +
                        "calculating gain/loss on branching network is not enabled.");
                    return null;
                }
                HydrologyNode upstreamNode = upstreamNodes.get(0);
                rec = table.getRecord(nodeIdColumnNum,upstreamNode.getCommonID());
                if ( rec == null ) {
                    problems.add ( "Cannot find network table row matching node \"" + upstreamNode.getCommonID() +
                        "\" - cannot get distance to calculate gain/loss).");
                    return null;
                }
                distanceO = null;
                try {
                    distanceO = rec.getFieldValue(nodeDistanceColumnNum);
                    if ( distanceO == null ) {
                        problems.add ( "Cannot determine distance from table column [" + nodeDistanceColumnNum +
                            "] for upstream node \"" + upstreamNode.getCommonID() + "\" - cannot calculate gain/loss).");
                        return null;
                    }
                    else {
                        distance2up = (Double)distanceO;
                    }
                }
                catch ( Exception e ) {
                    problems.add ( "Cannot determine distance from table column [" + nodeDistanceColumnNum +
                        "] for upstream node \"" + upstreamNode.getCommonID() + "\" (" + e + ") - cannot calculate gain/loss).");
                    return null;
                }
            }
            catch ( Exception e ) {
                problems.add ( "Cannot find network table row matching node upstream of \"" + nodeInReach.getCommonID() +
                    "\" (" + e + ") - cannot get distance to calculate gain/loss).");
                return null;
            }
            distance[iNode] = Math.abs(distance2 - distance2up);
            if ( gainMethod == NetworkGainMethodType.DISTANCE ) {
                // Weight is the distance between node and upstream node
                weights[iNode] = distance[iNode];
            }
            else {
                // Weight is the product of distance between node and upstream node * weight from above
                weights[iNode] = distance[iNode]*weights[iNode];
            }
        }
    }
    return weights;
}

//Copied this from HydrologyNodeNetwork.findUpstreamFlowNodes() and modified as needed.
/**
Look for the nodes upstream of the specified node, following up any tributaries as necessary, and return the list of
nodes up to but NOT including the upstream known flow nodes.  This list of nodes can then be used to distribute gain/loss.
The order of the nodes will be computation order from downstream to up and consequently it is possible to distribute gains
by traversing the list in the opposite order and checking where there are multiple upstream nodes.
@param upstreamGainLossNodes a list that will be filled and used internally during recursion; pass as null initially;
same as the returned list
@param node the node from which to look upstream; if not a known flow node, it will be added to the list to return
(the first value passed will be the downstream known point flow node)
@param table the table listing network nodes
@param nodeIdColumnNum table column number (0+) for node identifiers
@param nodeTypeColumnNum table column number (0+) for node types
@param nodeOutflowTypes node types where time series should be set at the node
@param problems a list of problem strings, to be treated as warnings in calling code
@return the list of nodes including and upstream of "node", but not including upstream known point flow nodes.
*/
public List<HydrologyNode> analyzeNetworkPointFlow_GetUpstreamNodesForGainLoss(List<HydrologyNode> upstreamGainLossNodes,
    HydrologyNode node, DataTable table, int nodeIdColumnNum, int nodeTypeColumnNum, String [] nodeOutflowTypes,
    List<String> problems)
{
    String routine = "AnalyzeNetworkPointFlow.getUpstreamNodesForGainLoss";
    Message.printStatus(2,routine,"Getting upstream nodes, starting with \"" + node.getCommonID() + "\"" );

    if (upstreamGainLossNodes == null) {
        // Create the list
        upstreamGainLossNodes = new ArrayList<HydrologyNode>();
    }
    
    /*
    String nodeType = lookupTableString ( table, nodeIdColumnNum, node.getCommonID(), nodeTypeColumnNum );
    if ( nodeType == null ) {
        problems.add ( "Unable to determine node type for node \"" + node.getCommonID() + "\" - unable to get nodes for gain/loss." );
        return upstreamGainLossNodes;
    }
    if ( isNodeOfAnalysisType(nodeType,nodeOutflowTypes) ) {
        // Done processing the reach
        return upstreamGainLossNodes;
    }
    else {
        // Add to the list
        upstreamGainLossNodes.add(node);
        Message.printStatus(2, routine, "Added upstream node (first in reach) \"" + node.getCommonID() + "\"" );
    }
    */

    // The node passed in initially will be a node above a known flow node.  Continue processing upstream

    List<HydrologyNode> upstreamNodes;
    //for ( nodePt = HydrologyNodeNetwork.getUpstreamNode(node, HydrologyNodeNetwork.POSITION_COMPUTATIONAL),
    //    nodePrev = node;
    //    ; nodePrev = nodePt, nodePt = HydrologyNodeNetwork.getUpstreamNode(nodePt, HydrologyNodeNetwork.POSITION_COMPUTATIONAL)) {
    int nodesInReach = 0;
    String nodeType;
    while ( true ) {
        Message.printStatus(2, routine, "Checking nodes upstream of node \"" + node.getCommonID() + "\"" );
        upstreamNodes = node.getUpstreamNodes();
        if ( (upstreamNodes == null) || (upstreamNodes.size() == 0) ) {
            // Top node in the reach.  If not a known flow node this is a problem
            String nodeType2 = lookupTableString ( table, nodeIdColumnNum, node.getCommonID(), nodeTypeColumnNum );
            if ( nodeType2 == null ) {
                problems.add ( "Unable to determine node type for node \"" + node.getCommonID() + "\" - unable to get nodes for gain/loss." );
            }
            else if ( !isNodeOfAnalysisType(nodeType2,nodeOutflowTypes) ) {
                // Upstream node is not a know flow type so can't process gain/loss
                problems.add ( "Most upstream node in reach is not know flow type - unable to determine node type for node \"" +
                    node.getCommonID() + "\" - unable to get nodes for gain/loss." );
            }
            return upstreamGainLossNodes;
        }
        else if ( upstreamNodes.size() > 1 ) {
            // Recurse...
            for ( HydrologyNode upstreamNode : upstreamNodes ) {
                upstreamGainLossNodes = analyzeNetworkPointFlow_GetUpstreamNodesForGainLoss( upstreamGainLossNodes,
                    upstreamNode, table, nodeIdColumnNum, nodeTypeColumnNum, nodeOutflowTypes,
                    problems);
            }
            return upstreamGainLossNodes;
        }
        else {
            // Process the one node
            nodeType = lookupTableString ( table, nodeIdColumnNum, node.getCommonID(), nodeTypeColumnNum );
            if ( nodeType == null ) {
                problems.add ( "Unable to determine node type for node \"" + node.getCommonID() + "\" - unable to get nodes for gain/loss." );
                return upstreamGainLossNodes;
            }
            if ( isNodeOfAnalysisType(nodeType,nodeOutflowTypes) && (nodesInReach != 0) ) {
                // Done processing the reach
                return upstreamGainLossNodes;
            }
            else {
                // Add the node to the list and go to the next node
                upstreamGainLossNodes.add(node);
                ++nodesInReach;
                if ( nodesInReach == 1) {
                    Message.printStatus(2, routine, "Added upstream node (first node in reach) \"" + node.getCommonID() + "\"" );
                }
                else {
                    Message.printStatus(2, routine, "Added upstream node \"" + node.getCommonID() + "\"" );
                }
                // Will only be one upstream node at this point.
                node = node.getUpstreamNode();
            }
         }
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
    String NodeDistanceColumn = parameters.getValue ( "NodeDistanceColumn" );
    String Interval = parameters.getValue ( "Interval" );
    String AnalysisStart = parameters.getValue ( "AnalysisStart" );
    String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
    String OutputTableID = parameters.getValue ( "OutputTableID" );
    String GainMethod = parameters.getValue ( "GainMethod" );
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
    
    if ( (GainMethod != null) && (GainMethod.length() != 0) ) {
        NetworkGainMethodType gm = NetworkGainMethodType.valueOfIgnoreCase(GainMethod);
        if ( gm == null ) {
            message = "The gain method (" + GainMethod + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid gain method from choices." ) );
        }
        else {
            if ( (gm == NetworkGainMethodType.DISTANCE) &&
                ((NodeDistanceColumn == null) || (NodeDistanceColumn.length() == 0) ) ) {
                message = "Specifying GainMethod=" + gm + " requires specifying the node distance column.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the node distance column." ) );
            }
        }
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
	List<String> validList = new ArrayList<String>(21);
    validList.add ( "TableID" );
    validList.add ( "NodeIDColumn" );
    validList.add ( "NodeNameColumn" );
    validList.add ( "NodeTypeColumn" );
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
    validList.add ( "TSIDColumn" );
    validList.add ( "Interval" );
    validList.add ( "AnalysisStart" );
    validList.add ( "AnalysisEnd" );
    validList.add ( "Units" );
    validList.add ( "GainMethod" );
    validList.add ( "OutputTableID" );
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
private List<TableRecord> findTableRecordsWithValue ( DataTable table, int col, String value, boolean matchNull )
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
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{   DataTable table = getDiscoveryTable();
    if ( (table != null) && (c == table.getClass()) ) {
        // Table request
        List<T> v = new Vector<T>();
        v.add ( (T)table );
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
        return (List<T>)discovery_TS_Vector;
    }
    else {
        return null;
    }
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
    HydrologyNodeNetwork network, List<String> problems )
throws Exception
{   String routine = "AnalyzeNetworkPointFlow_Command.initializeNetworkFromTable";
    // First find a table node that has no downstream node
    List <TableRecord> records = findTableRecordsWithValue ( table, downstreamNodeIdColumnNum, "", true );
    if ( records.size() == 0 ) {
        problems.add ( "Cannot find end node (node with no downstream node) in table.  " +
            "Cannot initialize network from table." );
        return;
    }
    else if ( records.size() != 1 ) {
        problems.add ( "Found " + records.size() + " nodes with no downstream node in table (below). " +
            "Exactly one downstream node per node is expected.  Cannot initialize network from table." );
        for ( TableRecord rec: records ) {
            problems.add ( "Node with no downstream node: \"" + rec.getFieldValueString(downstreamNodeIdColumnNum) + "\"");
        }
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
Initialize mass balance output time series for each node in the network.
@param table the table listing network nodes
@param network the node network that is being created from the network table
@param interval the time series interval for all time series in the analysis
@param analysisStart the date/time to start the analysis
@param analysisEnd the date/time to end the analysis
@param units the data units for mass balance time series
@param problems a list of problem strings that will be made visible in command status messages
@param return the list of time series created by the analysis, representing mass balance at each node
*/
private List<TS> initializeNodeTimeSeries ( DataTable table, int nodeIdColumnNum, int nodeTypeColumnNum, HydrologyNodeNetwork network,
    TimeInterval interval, DateTime analysisStart, DateTime analysisEnd, String units, List<String> problems )
{   List<TS> tslist = new ArrayList<TS>();
    String [] dataTypes = {
        "NodeInflow",
        "NodeAdd",
        "NodeSubtract",
        "NodeUpstreamGain",
        "NodeOutflow",
        "NodeUpstreamReachGain",
        "NodeInflowWithGain",
        "NodeOutflowWithGain",
        "NodeStorage" };
    TS ts;
    String nodeID;
    String nodeType;
    for (HydrologyNode node = HydrologyNodeNetwork.getUpstreamNode(network.getNodeHead(), HydrologyNodeNetwork.POSITION_ABSOLUTE);
        node.getDownstreamNode() != null;
        node = HydrologyNodeNetwork.getDownstreamNode(node, HydrologyNodeNetwork.POSITION_COMPUTATIONAL)) {
        // Get network information that was in the original table
        nodeID = node.getCommonID();
        List<TableRecord> records = findTableRecordsWithValue(table, nodeIdColumnNum, nodeID, false);
        nodeType = null;
        if ( (records != null) && (records.size() > 0) ) {
            TableRecord rec = records.get(0);
            try {
                nodeType = rec.getFieldValueString(nodeTypeColumnNum);
            }
            catch ( Exception e ) {
                // Ignore for now... don't set node type
            }
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
                ts.setMissing(Double.NaN);
                // Set some properties based on the network
                ts.setProperty("NodeType",nodeType);
                // TODO SAM 2014-07-13 need to set below
                //ts.setProperty("NodeDist",nodeDist);
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
@param nodeType the node type
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
Return the list of input time series that are used by a node, using TSID=nodeID.*.tsDataTypes.interval, where the
data types are used one by one in the TSID pattern.  If the node has specific time series available via a TSID
in the network, use that.
@param nodeID the location identifier
@param tsDataTypes time series data types to match
@param interval time series interval to match
@param nodeInputTSIDsMap map of NodeID as key to array of input TSIDs for the node, used to look up
specific time series
@param problems a list of problem strings that will be made visible in command status messages
@return list of matching time series
*/
private List<TS> lookupAnalysisInputTimeSeries ( String nodeID, String [] tsDataTypes,
    TimeInterval interval, HashMap<String, String[]> nodeInputTSIDsMap, List<String> problems )
{   String routine = getClass().getSimpleName() + ".lookupAnalysisInputTimeSeries";
    TSCommandProcessor processor = (TSCommandProcessor)getCommandProcessor();
    List<TS> tslist = new ArrayList<TS>();
    // First try to match a requested TSID
    // Currently only allow one input TSID per node so just see if the input TSID list is not empty
    Object o = nodeInputTSIDsMap.get(nodeID);
    String [] nodeInputTSIDs = null;
    // Position of TSID specified for the node - in future pass as parameter when node
    // has multiple input time series.  For now the first time series is all that is requested.
    int nodeInputTSIDsPos = 0;
    if ( o != null ) {
        nodeInputTSIDs = (String [])o;
    }
    // If here need to try to locate the time series using the default
    String TSList = "" + TSListType.ALL_MATCHING_TSID;
    if ( (nodeInputTSIDs != null) && (nodeInputTSIDs.length > 0) ) {
        PropList request_params = new PropList ( "" );
        String tsid = nodeInputTSIDs[nodeInputTSIDsPos];
        // Replace %I with the analysis interval (e.g., "Day").
        // Can't use processor or time series method because %I is not known for those so do a simple string replace
        tsid = tsid.replace("%I", "" + interval);
        Message.printStatus(2,routine,"Looking up input time series for node \"" + nodeID + "\" matching TSID \"" + tsid + "\"" );
        // Try to get the time series from the list in memory
        request_params.set ( "TSList", TSList );
        request_params.set ( "TSID", tsid );
        request_params.set ( "EnsembleID", null );
        CommandProcessorRequestResultsBean bean = null;
        try {
            bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
        }
        catch ( Exception e ) {
            problems.add ( "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
            "\", TSID=\"" + tsid + ") from processor for requested TSID." );
        }
        if ( bean == null ) {
            problems.add("Unable to find requested time series \"" + tsid + "\" for node \"" + nodeID + "\"");
        }
        else {
            PropList bean_PropList = bean.getResultsPropList();
            Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
            if ( o_TSList == null ) {
                problems.add("Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
                "\" TSID=\"" + tsid + "\") - did not find requested TSID.");
            }
            else {
            	@SuppressWarnings("unchecked")
				List<TS> tslist0 = (List<TS>)o_TSList;
                tslist = tslist0;
            }
        }
        if ( tslist.size() == 0 ) {
            problems.add("No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
            "\" TSID=\"" + tsid + "\") - did not find requested TSID.");
        }
        else {
            Message.printStatus(2,routine,"Found input time series for node \"" + nodeID + "\" matching TSID \"" + tsid + "\"" );
        }
    }
    else {
        // Specific TSID was not given so use the default pattern with node data to form the TSID
        // Do a processor request for each pattern
        String tsid = null;
        List<String> problems2 = new ArrayList<String>();
        for ( int i = 0; i < tsDataTypes.length; i++ ) {
            // Don't specify data source here - should not be important if other parts were matched
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
            	@SuppressWarnings("unchecked")
				List<TS> tslist0 = (List<TS>)o_TSList;
                tslist = tslist0;
                if ( tslist.size() == 0 ) {
                    problems2.add("No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
                    "\" TSID=\"" + tsid + "\").");
                }
                else {
                    // Was able to read the time series
                    break;
                }
            }
        }
        if ( tslist.size() == 0 ) {
            problems.addAll(problems2);
        }
        else {
            Message.printStatus(2,routine,"Found input time series for node \"" + nodeID + "\" using default TSID \"" + tsid + "\"" );
        }
    }
    return tslist;
}

/**
Look up the node type from the node identifier by searching the input table.
@param table the table containing network information
@param lookupColumnNum the column number (0+) to match the string
@param lookupString the string to match in the lookup column
@param outputColumnNum the column number (0+) for the output value
@return the string from the output column where the input column string was matched
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
@param interval the data interval for output time series.
@param outputTSList the list of output time series created by the command, searched to match the requested time series
@return the specific output time series that matches (first match)
*/
private TS lookupNodeOutputTimeSeries ( String nodeID, String dataType, TimeInterval interval, List<TS> outputTSList )
{
    String intervalString = "" + interval;
    for ( TS ts : outputTSList ) {
        if ( ts.getLocation().equalsIgnoreCase(nodeID) && ts.getDataType().equalsIgnoreCase(dataType) &&
            ts.getIdentifier().getInterval().equalsIgnoreCase(intervalString)) {
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
{	String routine = "AnalyzeNetworkPointFlow_Command.runCommandInternal",message = "";
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
    String TSIDColumn = parameters.getValue ( "TSIDColumn" );
    String [] tsidColumns = new String[0];
    if ( (TSIDColumn != null) && !TSIDColumn.equals("") ) {
        tsidColumns = new String[1];
        tsidColumns[0] = TSIDColumn;
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
            // Make sure the analysis start and end are not null - this ensures that the time series overlap
            if ( (analysisStart == null) || (analysisEnd == null) ) {
                message = "The analysis start and end must be specified.";
                Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
                status.addToLog ( command_phase, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the analysis start and end." ) );
            }
            else {
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
                int nodeWeightColumnNum = -1;
                if ( (NodeWeightColumn != null) && !NodeWeightColumn.equals("") ) {
                    nodeWeightColumnNum = table.getFieldIndex(NodeWeightColumn);
                }
                int downstreamNodeIdColumnNum = table.getFieldIndex(DownstreamNodeIDColumn);
                // Get column numbers for TSID columns, in case used as input to the analysis
                int [] tsidColumnsNum = new int[tsidColumns.length];
                for ( int i = 0; i < tsidColumns.length; i++ ) {
                    tsidColumnsNum[i] = table.getFieldIndex(tsidColumns[i]);
                }
                // Initialize the network from table information using column numbers for primary data
                initializeNetworkFromTable ( table, nodeIdColumnNum, nodeNameColumnNum,
                    nodeTypeColumnNum, nodeDistanceColumnNum, downstreamNodeIdColumnNum, network, problems );
                // Create hashmap that indicates TSIDs to use as input
                HashMap<String, String[]> nodeInputTSIDsMap = createInputTSIDMap ( table, nodeIdColumnNum, tsidColumnsNum );
                // Create output time series for each node
                List<TS> outputTSList = initializeNodeTimeSeries ( table, nodeIdColumnNum, nodeTypeColumnNum, network, interval,
                    analysisStart, analysisEnd, Units, problems );
                // Do the point flow analysis
                analyzeNetworkPointFlow ( table, nodeIdColumnNum, nodeTypeColumnNum, nodeDistanceColumnNum, nodeWeightColumnNum,
                    nodeAddTypes, nodeAddDataTypes, nodeSubtractTypes, nodeSubtractDataTypes,
                    nodeOutflowTypes, nodeOutflowDataTypes, nodeFlowThroughTypes,
                    network, nodeInputTSIDsMap, outputTSList, interval, gainMethod, analysisStart, analysisEnd, problems );
                // Report problems
                for ( String problem : problems ) {
                    if ( problem.charAt(0) != ' ' ) {
                        // Message has not been logged before so log...
                        Message.printWarning ( 3, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, problem );
                    }
                    status.addToLog ( command_phase, new CommandLogRecord(CommandStatusType.WARNING,
                        problem, "Check log file for more details." ) );
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
    String NodeWeightColumn = props.getValue( "NodeWeightColumn" );
    String DownstreamNodeIDColumn = props.getValue( "DownstreamNodeIDColumn" );
    String NodeAddTypes = props.getValue( "NodeAddTypes" );
    String NodeAddDataTypes = props.getValue( "NodeAddDataTypes" );
	String NodeSubtractTypes = props.getValue( "NodeSubtractTypes" );
	String NodeSubtractDataTypes = props.getValue( "NodeSubtractDataTypes" );
    String NodeOutflowTypes = props.getValue( "NodeOutflowTypes" );
    String NodeOutflowDataTypes = props.getValue( "NodeOutflowDataTypes" );
    String NodeFlowThroughTypes = props.getValue( "NodeFlowThroughTypes" );
    String TSIDColumn = props.getValue( "TSIDColumn" );
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
    if ( (NodeWeightColumn != null) && (NodeWeightColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NodeWeightColumn=\"" + NodeWeightColumn + "\"" );
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
    if ( (TSIDColumn != null) && (TSIDColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TSIDColumn=\"" + TSIDColumn + "\"");
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
