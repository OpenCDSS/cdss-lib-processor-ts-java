package rti.tscommandprocessor.commands.hecdss;

import java.io.File;
import java.util.List;
import java.util.Vector;

import RTi.Util.GUI.InputFilter;
import RTi.Util.GUI.InputFilter_JPanel;
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
{	List<InputFilter> inputFilters = new Vector<InputFilter>(6);
    inputFilters.add ( new InputFilter (
        "", "",
        StringUtil.TYPE_STRING,
        null, null, true ) );   // Blank to disable filter (no filter active)
    // Initialize filters with empty choice lists and allow to be editable.  This will allow choices to be
    // dynamically added later but strings to be manually typed in if code is not in place to fully populate
    // the choices.
    inputFilters.add ( __aPartInputFilter = new InputFilter (
        "A part", "A part",
        StringUtil.TYPE_STRING,
        new Vector<String>(), null, true ) );
    inputFilters.add ( __bPartInputFilter = new InputFilter (
        "B part", "B part",
        StringUtil.TYPE_STRING,
        new Vector<String>(), null, true ) );
    inputFilters.add ( __cPartInputFilter = new InputFilter (
        "C part", "C part",
        StringUtil.TYPE_STRING,
        new Vector<String>(), null, true ) );
    inputFilters.add ( __ePartInputFilter = new InputFilter (
        "E part", "E part",
        StringUtil.TYPE_STRING,
        new Vector<String>(), null, true ) );
    inputFilters.add ( __fPartInputFilter = new InputFilter (
        "F part", "F part",
        StringUtil.TYPE_STRING,
        new Vector<String>(), null, true ) );
	setToolTipText ( "<html>HEC-DSS queries can be filtered <br>based on time series metadata.</html>" );
	setInputFilters ( inputFilters, numFilterGroups, -1 );
}

/**
Refresh the A part list choices, using the currently specified HEC-DSS file.
@param clearFirst if true, then all the choices are cleared and all lists are populated with the unique parts with
no dependencies.
TODO SAM 2009-01-08  If false, what then?  Want to have the filter utilize other selections but this may take
some time to implement.
*/
public void refreshChoices ( boolean clearFirst )
{   // Get the unique list of A parts from the file
    if ( __hecDssFile == null ) {
        return;
    }
    if ( clearFirst ) {
        // Clear all the choices
        __aPartInputFilter.setChoices( new Vector<String>(), null, true);
        __bPartInputFilter.setChoices( new Vector<String>(), null, true);
        __cPartInputFilter.setChoices( new Vector<String>(), null, true);
        __ePartInputFilter.setChoices( new Vector<String>(), null, true);
        __fPartInputFilter.setChoices( new Vector<String>(), null, true);
        // Since the A part is the first in the path, it is the most generic
        List<String> aPartList = HecDssAPI.getUniqueAPartList(__hecDssFile,null,null,null,null);
        // Now set it in the choices
        __aPartInputFilter.setChoices ( aPartList, null, true ); // No internal list of choices, list is editable
        // redraw
        invalidate();
    }
}

/**
Set the HEC-DSS file that is being read from.  Filter choices will be determined by reading catalog information
from the file.
@param hecDssFile HEC-DSS file that is being read from.
*/
public void setHecDssFile ( File hecDssFile )
{
    __hecDssFile = hecDssFile;
}

}