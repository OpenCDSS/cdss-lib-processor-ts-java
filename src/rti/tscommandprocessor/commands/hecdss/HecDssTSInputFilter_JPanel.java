// HecDssTSInputFilter_JPanel - Input filter panel for HEC-DSS time series files.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2022 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.hecdss;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import RTi.Util.GUI.InputFilter;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

// TODO Figure out if the HEC-DSS API should be in a more generic location separate from commands
import rti.tscommandprocessor.commands.hecdss.HecDssAPI;

/**
Input filter panel for HEC-DSS time series files.  The filter contents are refreshed interactively based on
user selections.
*/
@SuppressWarnings("serial")
public class HecDssTSInputFilter_JPanel extends InputFilter_JPanel
{

	/**
	 * Labels that are used to look up the filters.
	 */
    public static final String A_PART_LABEL = "A part (e.g., basin)";
    public static final String B_PART_LABEL = "B part (location)";
    public static final String C_PART_LABEL = "C part (data type)";
    public static final String E_PART_LABEL = "E part (time step)";
    public static final String F_PART_LABEL = "F part (scenario)";

/**
HEC-DSS file that is being read from.  The file will be read to determine filter choices.
*/
private File __hecDssFile = null;

/**
Input filter for A part.
*/
private InputFilter __aPartInputFilter = null;

/**
Input filter for B part.
*/
private InputFilter __bPartInputFilter = null;

/**
Input filter for C part.
*/
private InputFilter __cPartInputFilter = null;

/**
Input filter for E part.
*/
private InputFilter __ePartInputFilter = null;

/**
Input filter for F part.
*/
private InputFilter __fPartInputFilter = null;

/**
Create an InputFilter_JPanel for creating where clauses for HED-DSS time series filters.  This is used by TSTool.
Choices are available for the A, B, C, E, and F parts of the time series.
@param numFilterGroups the number of filter groups to create
@return a JPanel containing InputFilter instances for HEC-DSS time series queries.
@exception Exception if there is an error.
*/
public HecDssTSInputFilter_JPanel ( int numFilterGroups )
throws Exception
{	List<InputFilter> inputFilters = new Vector<>(5);
    inputFilters.add ( new InputFilter (
        "", "",
        StringUtil.TYPE_STRING,
        null, null, true ) );   // Blank to disable filter (no filter active).
    // Initialize filters with empty choice lists and allow to be editable.
    // This will allow choices to be dynamically added later but strings to be manually typed in if code
    // is not in place to fully populate the choices.
    inputFilters.add ( __aPartInputFilter = new InputFilter (
        A_PART_LABEL, "A part",
        StringUtil.TYPE_STRING,
        new ArrayList<String>(), null, true ) );
    inputFilters.add ( __bPartInputFilter = new InputFilter (
        B_PART_LABEL, "B part",
        StringUtil.TYPE_STRING,
        new ArrayList<String>(), null, true ) );
    inputFilters.add ( __cPartInputFilter = new InputFilter (
        C_PART_LABEL, "C part",
        StringUtil.TYPE_STRING,
        new ArrayList<String>(), null, true ) );
    inputFilters.add ( __ePartInputFilter = new InputFilter (
        E_PART_LABEL, "E part",
        StringUtil.TYPE_STRING,
        new ArrayList<String>(), null, true ) );
    inputFilters.add ( __fPartInputFilter = new InputFilter (
        F_PART_LABEL, "F part",
        StringUtil.TYPE_STRING,
        new ArrayList<String>(), null, true ) );
	setToolTipText ( "<html>HEC-DSS queries can be filtered <br>based on time series metadata.</html>" );
	setInputFilters ( inputFilters, numFilterGroups, -1 );
}

/**
Refresh the filter choices using the currently specified HEC-DSS file.
This is called when a new HEC-DSS file is selected.
Each part is populated with unique values from the full list of time series.
*/
public void refreshChoices ()
{   // Get the unique list of A parts from the file.
	String routine = getClass().getSimpleName() + "refreshChoices";
    if ( this.__hecDssFile == null ) {
        return;
    }
    boolean clearFirst = false;
    if ( clearFirst ) {
        // Clear all the choices.
        //__aPartInputFilter.setChoices( new ArrayList<String>(), null, true);
        //__bPartInputFilter.setChoices( new ArrayList<String>(), null, true);
        //__cPartInputFilter.setChoices( new ArrayList<String>(), null, true);
        //__ePartInputFilter.setChoices( new ArrayList<String>(), null, true);
        //__fPartInputFilter.setChoices( new ArrayList<String>(), null, true);
        // Since the A part is the first in the path, it is the most generic.
    }
    List<String> aPartList = HecDssAPI.getUniquePartList(__hecDssFile, "A");
    Message.printStatus(2,routine,"Have " + aPartList.size() + " unique A parts" );
    List<String> bPartList = HecDssAPI.getUniquePartList(__hecDssFile, "B");
    Message.printStatus(2,routine,"Have " + bPartList.size() + " unique B parts" );
    List<String> cPartList = HecDssAPI.getUniquePartList(__hecDssFile, "C");
    Message.printStatus(2,routine,"Have " + cPartList.size() + " unique C parts" );
    List<String> ePartList = HecDssAPI.getUniquePartList(__hecDssFile, "E");
    Message.printStatus(2,routine,"Have " + ePartList.size() + " unique E parts" );
    List<String> fPartList = HecDssAPI.getUniquePartList(__hecDssFile, "F");
    Message.printStatus(2,routine,"Have " + fPartList.size() + " unique F parts" );
    // Clear input filter selections since setting new choices below.
    this.clearInput();
    // Now set it in the choices.
    this.__aPartInputFilter.setChoices ( aPartList, null, true ); // Set labels but no internal list of choices, list is editable.
    this.__bPartInputFilter.setChoices ( bPartList, null, true );
    this.__cPartInputFilter.setChoices ( cPartList, null, true );
    this.__ePartInputFilter.setChoices ( ePartList, null, true );
    this.__fPartInputFilter.setChoices ( fPartList, null, true );
    // Refresh the input filter panel with the new data:
    // - use the internal data
    refreshInputFilters ();
    // Redraw the UI.
    //invalidate();
}

/**
Set the HEC-DSS file that is being read from.  Filter choices will be determined by reading catalog information
from the file.
@param hecDssFile HEC-DSS file that is being read from.
*/
public void setHecDssFile ( File hecDssFile ) {
    __hecDssFile = hecDssFile;
}

}