// ExcelUtil - Utility package for Excel

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

package rti.tscommandprocessor.commands.spreadsheet;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Workbook;

/**
Utility package for Excel.  This class contains static methods.
Use the ExcelToolkit class for most other methods that don't require static data.
*/
public class ExcelUtil
{

/**
Static list of open Excel files, to allow Excel manipulations to span multiple commands.
*/
private static List<WorkbookFileMetadata> openWorkbooks = new ArrayList<WorkbookFileMetadata>();

/**
Return an open Excel workbook.
@return the matching Excel workbook or null if a match is not found.
@param wbfile name of workbook file to look up - a case-independent search will be performed.
*/
public static WorkbookFileMetadata getOpenWorkbook ( String wbfile )
{
	for ( WorkbookFileMetadata m : openWorkbooks ) {
		if ( m.getFilename().equalsIgnoreCase(wbfile) ) {
			// Found existing
			return m;
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
	for ( WorkbookFileMetadata m : openWorkbooks ) {
		if ( m.getFilename().equalsIgnoreCase(wbfile) ) {
			// Found existing, remove with new workbook
			openWorkbooks.remove(m);
			break;
		}
	}
}

/**
Set the open workbook in the cache so that it can be accessed later during processing.
The filename is treated case-independent when getOpenWorkbook() is called.
@param wbfile workbook filename
@param mode mode that file was opened, "r" for read and "w" for write.  In both cases it
is possible to write the file later but the modes give an indication of the initial action.
*/
public static void setOpenWorkbook(String wbfile, String mode, Workbook wb)
{
	// Search for an existing workbook
	int i = -1;
	for ( WorkbookFileMetadata m : openWorkbooks ) {
		++i;
		if ( m.getFilename().equalsIgnoreCase(wbfile) ) {
			// Found existing, replace with new workbook
			openWorkbooks.set(i, new WorkbookFileMetadata(wbfile,mode,wb));
			return;
		}
	}
	// If here need to add to the list
	openWorkbooks.add(new WorkbookFileMetadata(wbfile,mode,wb));
}
    
}
