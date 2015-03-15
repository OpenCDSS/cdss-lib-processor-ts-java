package rti.tscommandprocessor.commands.spreadsheet;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.apache.poi.ss.usermodel.Workbook;

/**
Utility package for Excel.  This class contains static methods.
Use the ExcelToolkit class for most other methods that don't require static data.
*/
public class ExcelUtil
{

/**
Static hashtable of open Excel files, to allow Excel manipulations to span multiple commands.
*/
private static Hashtable<String,Workbook> openWorkbooks = new Hashtable<String,Workbook> ();

/**
Return an open Excel workbook.
@return the matching Excel workbook or null if a match is not found.
@param wbfile name of workbook file to look up
*/
public static Workbook getOpenWorkbook ( String wbfile )
{
    String wbfileUpper = wbfile.toUpperCase();
    // Iterate through the hashtable and compare filename ignoring case
    Iterator<Map.Entry<String,Workbook>> it = openWorkbooks.entrySet().iterator();
    while ( it.hasNext() ) {
        Map.Entry<String,Workbook> entry = it.next();
        if ( entry.getKey().toUpperCase().equals(wbfileUpper) ) {
            return entry.getValue();
        }
    }
    return null;
}

/**
Remove an open Excel workbook from the list.
@param wbfile name of workbook file to remove
*/
public static void removeOpenWorkbook ( String wbfile )
{
    String wbfileUpper = wbfile.toUpperCase();
    // Iterate through the hashtable and compare filename ignoring case
    Iterator<Map.Entry<String,Workbook>> it = openWorkbooks.entrySet().iterator();
    while ( it.hasNext() ) {
        Map.Entry<String,Workbook> entry = it.next();
        if ( entry.getKey().toUpperCase().equals(wbfileUpper) ) {
            openWorkbooks.remove(entry.getKey());
            break;
        }
    }
}

/**
Set the open workbook in the cache so that it can be accessed later during processing.
The filename is treated case-independent.
*/
public static void setOpenWorkbook(String wbfile, Workbook wb)
{
    openWorkbooks.put(wbfile,wb);
}
    
}