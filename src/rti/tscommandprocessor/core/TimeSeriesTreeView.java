// TimeSeriesTreeView - Implementation of TimeSeriesView that organizes time series in an hierarchical tree.

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

package rti.tscommandprocessor.core;

import java.io.File;
import java.io.IOException;
import java.util.List;

import RTi.TS.TS;
import RTi.Util.GUI.SimpleJTree_Node;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

/**
Implementation of TimeSeriesView that organizes time series in an hierarchical tree.
*/
public class TimeSeriesTreeView implements TimeSeriesView {

/**
The view identifier.
*/
private String __viewID = "";

/**
The list of TreeViewNode objects.
*/
private SimpleJTree_Node __rootNode;
//private DefaultMutableTreeNode __rootNode;

/**
Construct a tree view with the given identifier.
@param viewID a string identifier for the view
*/
public TimeSeriesTreeView ( String viewID ) {
    setViewID ( viewID );
}

/**
Create the time series tree view from a file.  The file is of format:
<pre>
Label:  String label
    TS:  TSID-pattern
</pre>
specifically:
<ol>
<li>    Comments in the file are indicated by lines starting with #.</li>
<li>    There is one top-level label.</li>
<li>    Tab indicates indentations/levels in the tree.</li>
<li>    TS indicates that a time series identifier list will follow (standard command TSID matching).</li>
<li>    Additional objects (e.g., tables) and view specifications may be added later.</li>
</ol>
*/
public void createViewFromFile ( TSCommandProcessor processor, File viewFile, List<String> problems )
throws IOException {
   String routine = getClass().getSimpleName() + ".createViewFromFile";
    // TODO SAM 2010-07-16 This code could use recursion to perhaps read the file more
    // elegantly but for now keep the logic here and just locate the proper list by counting tabs.
    String filename = viewFile.getCanonicalPath();
    File f = new File(filename);
    List<String> fileLines = IOUtil.fileToStringList(filename);
    boolean rootFound = false;
    int ntab = 0; // Number of tabs in current line.
    int ntabPrev = 0; // Number of tabs in previous non-comment line.
    SimpleJTree_Node folderNode = null; // Active node (a "folder").
    SimpleJTree_Node nodePrev = null; // The node processed in the previous row.
    int lineCount = 0;
    for ( String fileLine : fileLines ) {
        ++lineCount;
        String fileLineTrimmed = fileLine.trim();
        // Allow comments to be indented.
        if ( fileLineTrimmed.startsWith("#") || fileLineTrimmed.equals("") ) {
            continue;
        }
        // Determine how many tabs at the start of the current line.
        ntabPrev = ntab; // From previous line.
        ntab = 0;
        for ( int i = 0; i < fileLine.length(); i++ ) {
            if ( fileLine.charAt(i) == '\t' ) {
                ++ntab;
            }
            else {
                break;
            }
        }
        // First make sure that the root node is properly handled.
        if ( !rootFound && StringUtil.startsWithIgnoreCase(fileLineTrimmed, "Label:") ) {
            String nodeLabel = fileLineTrimmed.substring(6).trim();
            // Simple label node.
            if ( ntab > 0 ) {
                throw new IOException ( "No root Label: in \"" + filename + "\" (add before any tabbed content)." );
            }
            else if ( ntab == 0 ) {
                // Create the root node.
            	if ( Message.isDebugOn ) {
            		Message.printDebug(1, routine, "Adding root label \"" + nodeLabel + "\"." );
            	}
                __rootNode = new SimpleJTree_Node(nodeLabel);
                folderNode = __rootNode;
                nodePrev = folderNode;
                rootFound = true;
                //ntab = 1; // This will correspond to using the root as the folder.
                continue;
            }
        }
        // Other lines are added to the root or appropriate nodes under the root.
        if ( rootFound && (ntab == 0) ) {
            throw new IOException ( "Can only have one root Label: in \"" + filename +
            "\" (need to insert tab(s) near line " + lineCount + "?)." );
        }
        else {
            // First determine the parent folder under which the node will be added.
            if ( ntab == ntabPrev ) {
                // Are at the same level as the previous line so the current "folder" node remains.
                // No action needed.
            }
            else if ( ntab == (ntabPrev + 1)) {
                // Have added another tab so need to add under the last row.
                folderNode = nodePrev;
            }
            else if ( ntab > (ntabPrev + 1)) {
                // Not allowed to jump more than one indent over the previous.
                throw new IOException ( "Cannot jump more than one tab forward from previous line.  Check \"" + filename +
                "\" near line " + lineCount + "." );
            }
            else {
                // Tab level is decreasing so set the folder back to a previous parent folder.
                int nshift = ntabPrev - ntab;
                for ( int i = 0; i < nshift; i++ ) {
                    folderNode = (SimpleJTree_Node)folderNode.getParent();
                }
            }
            // Now add the node depending on the node type.
            if ( StringUtil.startsWithIgnoreCase(fileLineTrimmed, "Label:") ) {
                String nodeLabel = fileLineTrimmed.substring(6).trim();
                if ( Message.isDebugOn ) {
	                Message.printDebug(1, routine, "Adding node label \"" + nodeLabel + "\" to folder node \"" + folderNode +
	                    "\" from file line " + lineCount );
                }
                nodePrev = new SimpleJTree_Node(nodeLabel);
                folderNode.add(nodePrev );
            }
            else if ( StringUtil.startsWithIgnoreCase(fileLineTrimmed, "Product:") ) {
                List<String> tokens = StringUtil.breakStringList(fileLineTrimmed.substring(8).trim(), " ", StringUtil.DELIM_ALLOW_STRINGS | StringUtil.DELIM_SKIP_BLANKS );
                String nodeLabel = tokens.get(0);
                if ( Message.isDebugOn ) {
	                Message.printDebug(1, routine, "Adding node graph \"" + nodeLabel + "\" to folder node \"" + folderNode +
	                    "\" from file line " + lineCount );
                }
                nodePrev = new SimpleJTree_Node(nodeLabel);
                // Filename is data for node.
                nodePrev.setData(IOUtil.toAbsolutePath(f.getParent(),tokens.get(1)));
                folderNode.add(nodePrev );
            }
            else if ( StringUtil.startsWithIgnoreCase(fileLineTrimmed, "TS:") ) {
                // List of time series to match in availableTS.
                String TSID = fileLineTrimmed.substring(3).trim();
                String TSList = "" + TSListType.ALL_MATCHING_TSID;
                String EnsembleID = null;
                PropList request_params = new PropList ( "" );
                request_params.set ( "TSList", TSList );
                request_params.set ( "TSID", TSID );
                CommandProcessorRequestResultsBean bean = null;
                try {
                    bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
                }
                catch ( Exception e ) {
                    problems.add ( "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
                    "\", TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\") from processor. )" );
                }
                if ( bean == null ) {
                    Message.printStatus ( 2, routine, "Bean is null.");
                }
                PropList bean_PropList = bean.getResultsPropList();
                Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
                List<TS> tslist = null;
                if ( o_TSList == null ) {
                    problems.add( "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
                    "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\")." );
                }
                else {
                	@SuppressWarnings("unchecked")
					List<TS> tslist0 = (List<TS>)o_TSList;
                    tslist = tslist0;
                    if ( tslist.size() == 0 ) {
                        problems.add ( "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
                        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\")." );
                    }
                }
                if ( (tslist != null) && (tslist.size() > 0) ) {
                    for ( TS ts: tslist ) {
                    	String tsid = ts.getIdentifierString();
                    	if ( Message.isDebugOn ) {
	                        Message.printDebug(1, routine, "Adding node TS \"" + tsid + "\" to folder node \"" + folderNode +
	                            "\" from file line " + lineCount );
                    	}
                        nodePrev = new SimpleJTree_Node( tsid );
                        nodePrev.setData ( ts );
                        folderNode.add(nodePrev );
                    }
                }
                else {
                    nodePrev = new SimpleJTree_Node("Unable to match TS: " + TSID );
                    folderNode.add(nodePrev );
                }
            }
        }
    }
}

/**
Get the root node.
*/
//public TimeSeriesTreeViewNode getRootNode ()
public SimpleJTree_Node getRootNode () {
    return __rootNode;
}

/**
Get the view identifier.
@return the view identifier.
*/
public String getViewID() {
    return __viewID;
}

/**
Set the view identifier.
@param viewID the view identifier.
*/
public void setViewID(String viewID) {
    __viewID = viewID;
}

}