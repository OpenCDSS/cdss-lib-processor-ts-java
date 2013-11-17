package rti.tscommandprocessor.commands.spreadsheet;

import javax.swing.JFrame;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
//import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
//import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
//import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.IO.IOUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableColumnType;
import RTi.Util.Table.TableField;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the WriteTimeSeriesToExcel() command, using Apache POI.
A useful link is:  http://poi.apache.org/spreadsheet/quick-guide.html
*/
public class WriteTimeSeriesToExcel_Command extends AbstractCommand implements Command
// TODO SAM 2013-11-03 Decide if new Excel file is allowed to be created
//, CommandDiscoverable, ObjectListProvider
{

/**
Possible values for ExcelColumnNames parameter.
*/
protected final String _ColumnN = "ColumnN";
protected final String _FirstRowInRange = "FirstRowInRange";
protected final String _RowBeforeRange = "RowBeforeRange";

/**
Possible values for WriteAllAsText parameter.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
The table that is read.
*/
private DataTable __table = null;

/**
The first data row from the range.  Save this because it is possible that comment lines
are skipped so not as easy as assuming the header line is before or at start of range.
*/
private int __firstDataRow = 0;

/**
Constructor.
*/
public WriteTimeSeriesToExcel_Command ()
{	super();
	setCommandName ( "WriteTimeSeriesToExcel" );
}

// TODO SAM 2013-08-12 This can be optimized to not have to check column name and do upper-case conversions
/**
Evaluate whether a cell value matches an exclude pattern.
@param columnName name of Excel column being checked
@param cellValue cell value as string, to check
@param filtersMap map of column 
@return true if the cell matches a filter
*/
private boolean cellMatchesFilter ( String columnName, String cellValue, Hashtable<String,String> filtersMap )
{
    if ( filtersMap == null ) {
        return false;
    }
    Enumeration keys = filtersMap.keys();
    String key = null;
    // Compare as upper case to treat as case insensitive
    String cellValueUpper = null;
    if ( cellValue != null ) {
        cellValueUpper = cellValue.toUpperCase();
    }
    String columnNameUpper = columnName.toUpperCase();
    String pattern;
    while ( keys.hasMoreElements() ) {
        key = (String)keys.nextElement();
        pattern = filtersMap.get(key);
        //Message.printStatus(2,"","Checking column \"" + columnNameUpper + "\" against key \"" + key +
        //    "\" for cell value \"" + cellValueUpper + "\" and pattern \"" + pattern + "\"" );
        if ( columnNameUpper.equals(key) ) {
            if ( ((cellValue == null) || (cellValue.length() == 0)) &&
                ((pattern == null) || (pattern.length() == 0)) ) {
                // Blank cell should be ignored
                return true;
            }
            else if ( (cellValueUpper != null) && cellValueUpper.matches(pattern) ) {
                return true;
            }
        }
    }
    return false;
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
    String OutputFile = parameters.getValue ( "OutputFile" );
    String ExcelColumnNames = parameters.getValue ( "ExcelColumnNames" );
    String NumberPrecision = parameters.getValue ( "NumberPrecision" );
	String WriteAllAsText = parameters.getValue ( "WriteAllAsText" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	// If the input file does not exist, warn the user...

	String working_dir = null;
	
	CommandProcessor processor = getCommandProcessor();
	
    if ( (TableID == null) || (TableID.length() == 0) ) {
        message = "The table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the table identifier." ) );
    }
	try {
	    Object o = processor.getPropContents ( "WorkingDir" );
		// Working directory is available so use it...
		if ( o != null ) {
			working_dir = (String)o;
		}
	}
	catch ( Exception e ) {
        message = "Error requesting WorkingDir from processor.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
	}
	
	if ( (OutputFile == null) || (OutputFile.length() == 0) ) {
        message = "The Excel output file must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an existing Excel output file." ) );
	}
	else {
        try {
            String adjusted_path = IOUtil.verifyPathForOS (IOUtil.adjustPath ( working_dir, OutputFile) );
			File f = new File ( adjusted_path );
			if ( !f.exists() ) {
                message = "The Excel output file does not exist:  \"" + adjusted_path + "\".";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that the Excel output file exists - may be OK if created at run time." ) );
			}
			f = null;
		}
		catch ( Exception e ) {
            message = "The output file:\n" +
            "    \"" + OutputFile +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that output file and working directory paths are compatible." ) );
		}
	}
	
    if ( ExcelColumnNames != null && !ExcelColumnNames.equalsIgnoreCase(_ColumnN) && 
        !ExcelColumnNames.equalsIgnoreCase(_FirstRowInRange) &&
        !ExcelColumnNames.equalsIgnoreCase(_RowBeforeRange) && !ExcelColumnNames.equalsIgnoreCase("")) {
        message = "ExcelColumnNames is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "ExcelColumnNames must " + _False + " (default) or " + _True ) );
    }
    
    if ( NumberPrecision != null && !NumberPrecision.equals("") ) {
        int numberPrecision = 0;
        boolean bad = false;
        try {
            numberPrecision = Integer.parseInt(NumberPrecision);
            if ( numberPrecision < 0 ) {
                bad = true;
            }
        }
        catch ( NumberFormatException e ) {
            bad = true;
        }
        if ( bad ) {
            message = "The NumberPrecision value (" + NumberPrecision + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify NumberPrecision as a positive integer." ) );
        }
    }
	
	if ( WriteAllAsText != null && !WriteAllAsText.equalsIgnoreCase(_True) && 
        !WriteAllAsText.equalsIgnoreCase(_False) && !WriteAllAsText.equalsIgnoreCase("") ) {
        message = "WriteAllAsText is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "WriteAllAsText must " + _False + " (default) or " + _True ) );
    }

	// TODO SAM 2005-11-18 Check the format.
    
	//  Check for invalid parameters...
	List<String> validList = new ArrayList<String>();
    validList.add ( "TableID" );
    validList.add ( "OutputFile" );
    validList.add ( "Worksheet" );
    validList.add ( "ExcelAddress" );
    validList.add ( "ExcelNamedRange" );
    validList.add ( "ExcelTableName" );
    validList.add ( "ExcelColumnNames" );
    validList.add ( "ColumnExcludeFilters" );
    //validList.add ( "Comment" );
    //validList.add ( "ExcelIntegerColumns" );
    //validList.add ( "ExcelDateTimeColumns" );
    validList.add ( "NumberPrecision" );
    validList.add ( "WriteAllAsText" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );    

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),warning );
		throw new InvalidCommandParameterException ( warning );
	}
    
    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Create table columns from the first row of the area.
@param table new table that will have columns added
@param workbook the workbook being read
@param sheet the worksheet being read
@param area are being read into table
@param excelColumnNames indicate how to determine column names from the Excel worksheet
@param comment if non-null indicates character(s) that indicate comment lines
@param excelIntegerColumns names of columns that should be treated as integers, or null if none
@param excelDateTimeColumns names of columns that should be treated as dates, or null if none
@param precisionForFloats number of digits after decimal for double columns
@param WriteAllAsText if True, treat all data as text values
@param problems list of problems encountered during processing
@return the names of the output columns or null if cannot create
*/
private String [] createTableColumns ( DataTable table, Workbook wb, Sheet sheet,
    AreaReference area, String excelColumnNames, String comment, String [] excelIntegerColumns,
    String [] excelDateTimeColumns, int precisionForFloats, boolean WriteAllAsText, List<String> problems )
{   String routine = "WriteTimeSeriesToExcel_Command.createTableColumns";
    Row dataRow; // First row of data
    Row headerRow = null; // Row containing column headings
    Cell cell;
    int iRow = area.getFirstCell().getRow();
    int firstDataRow = iRow; // Default before checking ExcelColumnNames parameter
    int rowEnd = area.getLastCell().getRow();
    if ( excelColumnNames.equalsIgnoreCase(_FirstRowInRange) ) {
        if ( comment == null ) {
            // Comments are not used so header is first row and first data row is next
            headerRow = sheet.getRow(iRow);
            firstDataRow = iRow + 1;
        }
        else {
            // Loop through first column cells.  If string and starts with comment, skip row
            boolean foundFirstDataRow = false;
            for ( ; iRow <= rowEnd; iRow++ ) {
                if ( rowIsComment ( sheet, iRow, comment ) ) {
                    continue;
                }
                else {
                    headerRow = sheet.getRow(iRow);
                    // Now find the first data row (could have more comments)
                    for ( ++iRow; iRow <= rowEnd; iRow++ ) {
                        if ( rowIsComment ( sheet, iRow, comment ) ) {
                            continue;
                        }
                        else {
                            foundFirstDataRow = true;
                            firstDataRow = iRow;
                            break;
                        }
                    }
                }
                if ( foundFirstDataRow ) {
                    break;
                }
            }
        }
    }
    else if ( excelColumnNames.equalsIgnoreCase(_RowBeforeRange) ) {
        // Loop backwards and skip comments
        for ( --iRow; iRow >= 0; iRow-- ) {
            if ( rowIsComment ( sheet, iRow, comment ) ) {
                continue;
            }
            else {
                headerRow = sheet.getRow(iRow);
                if ( headerRow == null ) {
                    problems.add ( "Specified ExcelColumnNames=" + _RowBeforeRange +
                        " but this results in row not on sheet.  Check address range." );
                    return null;
                }
            }
        }
    }
    setFirstDataRow(firstDataRow);
    Message.printStatus(2, routine, "Determined first data row to be [" + firstDataRow + "]");
    dataRow = sheet.getRow(firstDataRow);
    int colStart = area.getFirstCell().getCol();
    int colEnd = area.getLastCell().getCol();
    int columnIndex = -1;
    // First get the column names
    String [] columnNames = new String[colEnd - colStart + 1];
    int cellType;
    for ( int iCol = colStart; iCol <= colEnd; iCol++ ) {
        ++columnIndex;
        if ( excelColumnNames.equalsIgnoreCase(_ColumnN) ) {
            columnNames[columnIndex] = "Column" + (columnIndex + 1);
        }
        else {
            // Column names taken from header row - text value
            cell = headerRow.getCell(iCol);
            if ( cell == null ) {
                // Default...
                columnNames[columnIndex] = "Column" + (columnIndex + 1);
            }
            else {
                cellType = cell.getCellType();
                if ( cellType == Cell.CELL_TYPE_FORMULA ) {
                    // Have to evaluate the formula to get the result, which can be used as the column name
                    FormulaEvaluator formulaEval = wb.getCreationHelper().createFormulaEvaluator();
                    columnNames[columnIndex] = formulaEval.evaluate(cell).formatAsString();
                }
                else if ( (cellType == Cell.CELL_TYPE_BLANK) || (cellType == Cell.CELL_TYPE_ERROR) ) {
                 // Default...
                    columnNames[columnIndex] = "Column" + (columnIndex + 1);
                }
                else {
                    columnNames[columnIndex] = "" + cell;
                }
            }
        }
    }
    // Now loop through and determine the column data type from the data row
    // and add columns to the table
    columnIndex = -1;
    //CellStyle style = null;
    for ( int iCol = colStart; iCol <= colEnd; iCol++ ) {
        cell = dataRow.getCell(iCol);
        ++columnIndex;
        if ( WriteAllAsText ) {
            table.addField ( new TableField(TableField.DATA_TYPE_STRING, columnNames[columnIndex], -1, -1), null );
        }
        else {
            // See if the column name matches the integer columns
            boolean isInteger = false;
            if ( excelIntegerColumns != null ) {
                for ( int i = 0; i < excelIntegerColumns.length; i++ ) {
                    if ( columnNames[columnIndex].equalsIgnoreCase(excelIntegerColumns[i]) ) {
                        // Treat as an integer
                        isInteger = true;
                        break;
                    }
                }
            }
            boolean isDate = false;
            if ( excelDateTimeColumns != null ) {
                for ( int i = 0; i < excelDateTimeColumns.length; i++ ) {
                    if ( columnNames[columnIndex].equalsIgnoreCase(excelDateTimeColumns[i]) ) {
                        // Treat as a date
                        isDate = true;
                        break;
                    }
                }
            }
            // Interpret the first row cell types to determine column types
            if ( isInteger ) {
                Message.printStatus(2,routine,"Creating table column [" + iCol + "]=" + TableColumnType.valueOf(TableField.DATA_TYPE_INT));
                table.addField ( new TableField(TableField.DATA_TYPE_INT, columnNames[columnIndex], -1, -1), null );
                continue;
            }
            else if ( isDate ) {
                Message.printStatus(2,routine,"Creating table column [" + iCol + "]=" + TableColumnType.valueOf(TableField.DATA_TYPE_DATETIME));
                table.addField ( new TableField(TableField.DATA_TYPE_DATETIME, columnNames[columnIndex], -1, -1), null );
                continue;
            }
            cellType = Cell.CELL_TYPE_BLANK;
            if ( cell != null ) {
                cellType = cell.getCellType();
            }
            if ( (cell == null) || (cellType == Cell.CELL_TYPE_BLANK) || (cellType == Cell.CELL_TYPE_ERROR) ) {
                Message.printStatus(2,routine,"Table column [" + iCol + "] cell type from Excel is BLANK or ERROR.  " +
                	"Examining rows to find data to determine column type.");
                // Look forward to other rows to see if there is a non-null value that can be used to determine the type
                Row dataRow2;
                for ( int iRowSearch = (firstDataRow + 1); iRowSearch <= rowEnd; iRowSearch++ ) {
                    dataRow2 = sheet.getRow(iRowSearch);
                    if ( dataRow2 != null ) {
                        cell = dataRow2.getCell(iCol);
                        if ( cell != null ) {
                            cellType = cell.getCellType();
                            if ( (cellType == Cell.CELL_TYPE_STRING) || (cellType == Cell.CELL_TYPE_NUMERIC) ||
                                (cellType == Cell.CELL_TYPE_BOOLEAN) || (cellType == Cell.CELL_TYPE_FORMULA)) {
                                // Break out and interpret below
                                break;
                            }
                        }
                    }
                }
            }
            if ( cellType == Cell.CELL_TYPE_STRING ) {
                Message.printStatus(2,routine,"Table column [" + iCol + "] cell type from Excel is STRING." );
                Message.printStatus(2,routine,"Creating table column [" + iCol + "]=" + TableColumnType.valueOf(TableField.DATA_TYPE_STRING));
                table.addField ( new TableField(TableField.DATA_TYPE_STRING, columnNames[columnIndex], -1, -1), null );
            }
            else if ( cellType == Cell.CELL_TYPE_NUMERIC ) {
                Message.printStatus(2,routine,"Table column [" + iCol + "] cell type from Excel is NUMERIC");
                if (DateUtil.isCellDateFormatted(cell)) {
                    // TODO SAM 2013-05-12 Evaluate whether to use DATA_TYPE_DATETIME
                    //table.addField ( new TableField(TableField.DATA_TYPE_STRING, columnNames[columnIndex], -1, -1), null );
                    Message.printStatus(2,routine,"Creating table column [" + iCol + "]=" + TableColumnType.valueOf(TableField.DATA_TYPE_DATE));
                    table.addField ( new TableField(TableField.DATA_TYPE_DATE, columnNames[columnIndex], -1, -1), null );
                }
                else {
                    // TODO SAM 2013-02-26 Need to figure out the precision from formatting
                    // For now always set the column to a double with the precision from formatting
                    // Could default to integer for 0-precision but could guess wrong
                    //style = cell.getCellStyle();
                    //short format = style.getDataFormat();
                    //CellStyle style2 = wb.getCellStyleAt(format);
                    Message.printStatus(2,routine,"Creating table column [" + iCol + "]=" + TableColumnType.valueOf(TableField.DATA_TYPE_DOUBLE));
                    table.addField ( new TableField(TableField.DATA_TYPE_DOUBLE, columnNames[columnIndex], -1, precisionForFloats), null );
                }
            }
            else if ( cellType == Cell.CELL_TYPE_BOOLEAN ) {
                Message.printStatus(2,routine,"Table column [" + iCol + "] cell type from Excel is BOOLEAN.  Treat as integer 0/1.");
                // Use integer for boolean
                Message.printStatus(2,routine,"Creating table column [" + iCol + "]=" + TableColumnType.valueOf(TableField.DATA_TYPE_INT));
                table.addField ( new TableField(TableField.DATA_TYPE_INT, columnNames[columnIndex], -1, -1), null );
            }
            else if ( cellType == Cell.CELL_TYPE_FORMULA ) {
                Message.printStatus(2,routine,"Table column [" + iCol + "] cell type from Excel is FORMULA.  Treat as String.");
                Message.printStatus(2,routine,"Creating table column [" + iCol + "]=" + TableColumnType.valueOf(TableField.DATA_TYPE_STRING));
                table.addField ( new TableField(TableField.DATA_TYPE_STRING, columnNames[columnIndex], -1, -1), null );
            }
            else {
                // Default is to treat as a string
                Message.printStatus(2,routine,"Table column [" + iCol + "] cell type from Excel (" + cellType + ") is unknown.  Treating as string.");
                Message.printStatus(2,routine,"Creating table column [" + iCol + "]=" + TableColumnType.valueOf(TableField.DATA_TYPE_STRING));
                table.addField ( new TableField(TableField.DATA_TYPE_STRING, columnNames[columnIndex], -1, -1), null );
            }
        }
    }
    return columnNames;
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new WriteTimeSeriesToExcel_JDialog ( parent, this )).ok();
}

/**
Get the array of cell ranges based on one of the input address methods.
@param wb the Excel workbook object
@param sheet the sheet in the workbook, read in entirety if no other address information is given
@param excelAddress Excel address range (e.g., A1:D10 or $A1:$D10 or variant)
@param excelNamedRange a named range
@param excelTableName a table name, treated as named range
@return null if no area reference can be determined
*/
private AreaReference getAreaReference ( Workbook wb, Sheet sheet,
    String excelAddress, String excelNamedRange, String excelTableName )
{   String routine = "WriteTimeSeriesToExcel_Command.getAreaReference";
    if ( (excelTableName != null) && (excelTableName.length() > 0) ) {
        // Table name takes precedence as range name
        excelNamedRange = excelTableName;
    }
    // If sheet is specified but excelAddress, String excelNamedRange, String excelTableName are not,
    // read the entire sheet
    if ( ((excelAddress == null) || (excelAddress.length() == 0)) &&
        ((excelNamedRange == null) || (excelNamedRange.length() == 0)) ) {
        // Examine the sheet for blank columns/cells.  POI provides methods for the rows...
        int firstRow = sheet.getFirstRowNum();
        int lastRow = sheet.getLastRowNum();
        Message.printStatus(2, routine, "Sheet firstRow=" + firstRow + ", lastRow=" + lastRow );
        // ...but have to iterate through the rows as per:
        //  http://stackoverflow.com/questions/2194284/how-to-get-the-last-column-index-reading-excel-file
        Row row;
        int firstCol = -1;
        int lastCol = -1;
        int cellNum; // Index of cell in row (not column number?)
        int col;
        for ( int iRow = firstRow; iRow <= lastRow; iRow++ ) {
            row = sheet.getRow(iRow);
            if ( row == null ) {
                // TODO SAM 2013-06-28 Sometimes this happens with extra rows at the end of a worksheet?
                continue;
            }
            cellNum = row.getFirstCellNum(); // Not sure what this returns if no columns.  Assume -1
            if ( cellNum >= 0 ) {
                col = row.getCell(cellNum).getColumnIndex();
                if ( firstCol < 0 ) {
                    firstCol = col;
                }
                else {
                    firstCol = Math.min(firstCol, col);
                }
            }
            cellNum = row.getLastCellNum() - 1; // NOTE -1, as per API docs
            if ( cellNum >= 0 ) {
                col = row.getCell(cellNum).getColumnIndex();
                if ( lastCol < 0 ) {
                    lastCol = col;
                }
                else {
                    lastCol = Math.max(lastCol, col);
                }
            }
            Message.printStatus(2, routine, "row " + iRow + ", firstCol=" + firstCol + ", lastCol=" + lastCol );
        }
        // Return null if the any of the row column limits were not determined
        if ( (firstRow < 0) || (firstCol < 0) || (lastRow < 0) || (lastCol < 0) ) {
            return null;
        }
        else {
            return new AreaReference(new CellReference(firstRow,firstCol), new CellReference(lastRow,lastCol));
        }
    }
    if ( (excelAddress != null) && (excelAddress.length() > 0) ) {
        return new AreaReference(excelAddress);
    }
    else if ( (excelNamedRange != null) && (excelNamedRange.length() > 0) ) {
        int namedCellIdx = wb.getNameIndex(excelNamedRange);
        if ( namedCellIdx < 0 ) {
            Message.printWarning(3, routine, "Unable to get Excel internal index for named range \"" +
                excelNamedRange + "\"" );
            return null;
        }
        Name aNamedCell = wb.getNameAt(namedCellIdx);

        // Retrieve the cell at the named range and test its contents
        // Will get back one AreaReference for C10, and
        //  another for D12 to D14
        AreaReference[] arefs = AreaReference.generateContiguous(aNamedCell.getRefersToFormula());
        // Can only handle one area
        if ( arefs.length != 1 ) {
            return null;
        }
        else {
            return arefs[0];
        }
    }
    else {
        return null;
    }
}

/**
Return the table that is read by this class when run in discovery mode.
*/
private DataTable getDiscoveryTable()
{
    return __table;
}

/**
Return the row (0+) of the first data row to process.
*/
private int getFirstDataRow ()
{
    return __firstDataRow;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
*/
public List getObjectList ( Class c )
{   DataTable table = getDiscoveryTable();
    List v = null;
    if ( (table != null) && (c == table.getClass()) ) {
        v = new Vector();
        v.add ( table );
    }
    return v;
}

// Use base class parseCommand()

/**
Read the table from an Excel worksheet.  The cells must be specified by a contiguous address block specified
by one of the parameters excelAddress, excelNamedRange, excelTableName.
@param workbookFile the name of the workbook file (*.xls or *.xlsx)
@param sheetName the name of the sheet in the workbook
@param excelAddress Excel address range (e.g., A1:D10 or $A1:$D10 or variant)
@param excelNamedRange a named range
@param excelTableName a table name
@param excelColumnNames indicate how to determine column names from the Excel worksheet
@param columnExcludeFiltersMap a map indicating patters for column values, to exclude rows
@param comment character that if at start of first column indicates row is a comment
@param excelIntegerColumns names of columns that should be treated as integers, or null if none
@param numberPrecision digits after decimal for floating point numbers (can't yet determine from Excel)
@param WriteAllAsText if True, treat all data as text values
@param problems list of problems encountered during read, for formatted logging in calling code
@return a DataTable with the Excel contents
*/
private DataTable WriteTimeSeriesToExcelFile ( String workbookFile, String sheetName,
    String excelAddress, String excelNamedRange, String excelTableName, String excelColumnNames,
    Hashtable columnExcludeFiltersMap, String comment, String [] excelIntegerColumns, String [] excelDateTimeColumns,
    int numberPrecision, boolean WriteAllAsText, List<String> problems )
throws FileNotFoundException, IOException
{   String routine = "WriteTimeSeriesToExcel_Command.WriteTimeSeriesToExcelFile", message;
    DataTable table = new DataTable();
    if ( (comment != null) && (comment.trim().length() == 0) ) {
        // Set to null to simplify logic below
        comment = null;
    }
    
    Workbook wb = null;
    InputStream inp = null;
    try {
        try {
            inp = new FileInputStream(workbookFile);
        }
        catch ( IOException e ) {
            problems.add ( "Error opening workbook file \"" + workbookFile + "\" (" + e + ")." );
            return null;
        }
        try {
            wb = WorkbookFactory.create(inp);
        }
        catch ( InvalidFormatException e ) {
            problems.add ( "Error creating workbook object from \"" + workbookFile + "\" (" + e + ")." );
            return null;
        }
        Sheet sheet = null;
        // TODO SAM 2013-02-22 In the future sheet may be determined from named address (e.g., named ranges
        // are global in workbook)
        if ( (sheetName == null) || (sheetName.length() == 0) ) {
            // Default is to use the first sheet
            sheet = wb.getSheetAt(0);
            if ( sheet == null ) {
                problems.add ( "Workbook does not include any worksheets" );
                return null;
            }
        }
        else {
            sheet = wb.getSheet(sheetName);
            if ( sheet == null ) {
                problems.add ( "Workbook does not include worksheet named \"" + sheetName + "\"" );
                return null;
            }
        }
        // Get the contiguous block of data to process by evaluating user input
        AreaReference area = getAreaReference ( wb, sheet, excelAddress, excelNamedRange, excelTableName );
        if ( area == null ) {
            problems.add ( "Unable to get worksheet area reference from address information." );
            return null;
        }
        Message.printStatus(2,routine,"Excel address block to read: " + area );
        // Create the table based on the first row of the area
        String [] columnNames = createTableColumns ( table, wb, sheet, area, excelColumnNames, comment,
            excelIntegerColumns, excelDateTimeColumns, numberPrecision, WriteAllAsText, problems );
        int [] tableColumnTypes = table.getFieldDataTypes();
        // Read the data from the area and transfer to the table.
        Row row;
        Cell cell;
        int rowStart = getFirstDataRow(); // Set in createTableColumns()
        int rowEnd = area.getLastCell().getRow();
        int colStart = area.getFirstCell().getCol();
        int colEnd = area.getLastCell().getCol();
        Message.printStatus(2, routine, "Cell range is [" + rowStart + "][" + colStart + "] to [" + rowEnd + "][" + colEnd + "]");
        int cellType;
        int iRowOut = -1, iColOut;
        String cellValueString;
        boolean cellValueBoolean;
        double cellValueDouble;
        Date cellValueDate;
        CellValue formulaCellValue = null; // Cell value after formula evaluation
        DateTime dt;
        boolean cellIsFormula; // Used to know when the evaluate cell formula to get output object
        boolean needToSkipRow = false; // Whether a row should be skipped
        int nRowsToRead = rowEnd - rowStart + 1;
        for ( int iRow = rowStart; iRow <= rowEnd; iRow++ ) {
            row = sheet.getRow(iRow);
            if ( row == null ) {
                // Seems to happen at bottom of worksheets where there are extra junk rows
                continue;
            }
            ++iRowOut;
            Message.printStatus(2, routine, "Processing row [" + iRow + "] end at [" + rowEnd + "]" );
            if ( (iRow == rowStart) || (iRow == rowEnd) || (iRow%25 == 0) ) {
                // Update the progress bar every 5%
                message = "Reading row " + (iRow - rowStart + 1) + " of " + nRowsToRead;
                notifyCommandProgressListeners ( (iRow - rowStart), nRowsToRead, (float)-1.0, message );
            }
            if ( (comment != null) && rowIsComment(sheet, iRow, comment) ) {
                // No need to process the row.
                continue;
            }
            needToSkipRow = false;
            iColOut = -1;
            for ( int iCol = colStart; iCol <= colEnd; iCol++ ) {
                ++iColOut;
                cell = row.getCell(iCol);
                if ( cell == null ) {
                    Message.printStatus(2, routine, "Cell [" + iRow + "][" + iCol + "]= \"" + cell + "\"" );
                    if ( tableColumnTypes[iColOut] == TableField.DATA_TYPE_STRING ) {
                        table.setFieldValue(iRowOut, iColOut, "", true);
                    }
                    else {
                        table.setFieldValue(iRowOut, iColOut, null, true);
                    }
                    if ( (columnExcludeFiltersMap != null) &&
                        cellMatchesFilter(columnNames[iCol], null, columnExcludeFiltersMap) ) {
                        // Row was added but will remove at the end after all columns are processed
                        needToSkipRow = true;
                    }
                    continue;
                }
                // First get the data using the type indicated for the cell.  Then translate to
                // the appropriate type in the data table.  Handling at cell level is needed because
                // the Excel worksheet might have cell values that are mixed type in the column.
                // The checks are exhaustive, so list in the order that is most likely (string, double,
                // boolean, blank, error, formula).
                cellType = cell.getCellType();
                Message.printStatus(2, routine, "Cell [" + iRow + "][" + iCol + "]= \"" + cell + "\" type=" + cellType );
                cellIsFormula = false;
                if ( cellType == Cell.CELL_TYPE_FORMULA ) {
                    // Have to evaluate the cell and get the value as the result
                    cellIsFormula = true;
                    try {
                        FormulaEvaluator formulaEval = wb.getCreationHelper().createFormulaEvaluator();
                        formulaCellValue = formulaEval.evaluate(cell);
                        // Reset cellType for following code
                        cellType = formulaCellValue.getCellType();
                        Message.printStatus(2, routine, "Detected formula, new cellType=" + cellType +
                            ", cell value=\"" + formulaCellValue + "\"" );
                    }
                    catch ( Exception e ) {
                        // Handle as an error in processing below.
                        problems.add ( "Error evaluating formula for row [" + iRow + "][" + iCol + "] \"" +
                            cell + "\" - setting to error cell type (" + e + ")");
                        cellType = Cell.CELL_TYPE_ERROR;
                    }
                }
                if ( cellType == Cell.CELL_TYPE_STRING ) {
                    if ( cellIsFormula ) {
                        cellValueString = formulaCellValue.getStringValue();
                    }
                    else {
                        cellValueString = cell.getStringCellValue();
                    }
                    if ( (columnExcludeFiltersMap != null) &&
                        cellMatchesFilter(columnNames[iCol], cellValueString,columnExcludeFiltersMap) ) {
                            // Add the row but will remove at the end after all columns are processed
                            needToSkipRow = true;
                    }
                    if ( tableColumnTypes[iColOut] == TableField.DATA_TYPE_STRING ) {
                        // Just set
                        table.setFieldValue(iRowOut, iColOut, cellValueString, true);
                    }
                    else if ( tableColumnTypes[iColOut] == TableField.DATA_TYPE_DOUBLE ) {
                        // Parse to the double
                        try {
                            table.setFieldValue(iRowOut, iColOut, new Double(cellValueString), true);
                        }
                        catch ( NumberFormatException e ) {
                            // Set to NaN
                            table.setFieldValue(iRowOut, iColOut, Double.NaN, true);
                        }
                    }
                    else if ( tableColumnTypes[iColOut] == TableField.DATA_TYPE_INT ) {
                        // Parse to the boolean
                        if ( cellValueString.equalsIgnoreCase("True") || cellValueString.equals("1") ) {
                            table.setFieldValue(iRowOut, iColOut, new Integer(1), true);
                        }
                        else {
                            // Set to null
                            table.setFieldValue(iRowOut, iColOut, null, true);
                        }
                    }
                    else if ( tableColumnTypes[iColOut] == TableField.DATA_TYPE_DATE ) {
                        // Try to parse to a date/time string
                        try {
                            dt = DateTime.parse(cellValueString);
                            table.setFieldValue(iRowOut, iColOut, dt.getDate(), true);
                        }
                        catch ( Exception e ) {
                            // Set to null
                            table.setFieldValue(iRowOut, iColOut, null, true);
                        }
                    }
                    else if ( tableColumnTypes[iColOut] == TableField.DATA_TYPE_DATETIME ) {
                        // Try to parse to a date/time string
                        try {
                            dt = DateTime.parse(cellValueString);
                            table.setFieldValue(iRowOut, iColOut, dt, true);
                        }
                        catch ( Exception e ) {
                            // Set to null
                            table.setFieldValue(iRowOut, iColOut, null, true);
                        }
                    }
                    else {
                        // Other cell types don't translate
                        table.setFieldValue(iRowOut, iColOut, null, true);
                    }
                }
                else if ( cellType == Cell.CELL_TYPE_NUMERIC ) {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        if ( cellIsFormula ) {
                            // TODO SAM 2013-02-25 Does not seem to method to return date 
                            cellValueDate = null;
                        }
                        else {
                            cellValueDate = cell.getDateCellValue();
                        }
                        if ( tableColumnTypes[iColOut] == TableField.DATA_TYPE_DATE ) {
                            // date to date
                            table.setFieldValue(iRowOut, iColOut, cellValueDate, true);
                        }
                        else if ( tableColumnTypes[iColOut] == TableField.DATA_TYPE_DATETIME ) {
                            // date to date/time
                            table.setFieldValue(iRowOut, iColOut, new DateTime(cellValueDate), true);
                        }
                        else if ( tableColumnTypes[iColOut] == TableField.DATA_TYPE_STRING ) {
                            // date to string
                            try {
                                dt = new DateTime ( cellValueDate );
                                table.setFieldValue(iRowOut, iColOut, dt.toString(), true);
                            }
                            catch ( Exception e ) {
                                table.setFieldValue(iRowOut, iColOut, null, true);
                            }
                        }
                        else {
                            table.setFieldValue(iRowOut, iColOut, null, true);
                        }
                    }
                    else {
                        // Floating point value
                        if ( cellIsFormula ) {
                            cellValueDouble = formulaCellValue.getNumberValue();
                        }
                        else {
                            cellValueDouble = cell.getNumericCellValue();
                        }
                        if ( tableColumnTypes[iColOut] == TableField.DATA_TYPE_DOUBLE ) {
                            // Double to double
                            table.setFieldValue(iRowOut, iColOut, new Double(cellValueDouble), true);
                        }
                        else if ( tableColumnTypes[iColOut] == TableField.DATA_TYPE_STRING ) {
                            // Double to string
                            table.setFieldValue(iRowOut, iColOut, "" + cellValueDouble, true);
                        }
                        else if ( tableColumnTypes[iColOut] == TableField.DATA_TYPE_INT ) {
                            // Double to integer - use an offset to help make sure integer value is correct
                            if ( cellValueDouble >= 0.0 ) {
                                table.setFieldValue(iRowOut, iColOut, new Integer((int)(cellValueDouble + .0001)), true);
                            }
                            else {
                                table.setFieldValue(iRowOut, iColOut, new Integer((int)(cellValueDouble - .0001)), true);
                            }
                        }
                        else {
                            table.setFieldValue(iRowOut, iColOut, null, true);
                        }
                    }
                }
                else if ( cellType == Cell.CELL_TYPE_BOOLEAN ) {
                    if ( cellIsFormula ) {
                        cellValueBoolean = formulaCellValue.getBooleanValue();
                    }
                    else {
                        cellValueBoolean = cell.getBooleanCellValue();
                    }
                    if ( tableColumnTypes[iColOut] == TableField.DATA_TYPE_INT ) {
                        table.setFieldValue(iRowOut, iColOut, cellValueBoolean, true);
                    }
                    else if ( tableColumnTypes[iColOut] == TableField.DATA_TYPE_STRING ) {
                        // Just set
                        table.setFieldValue(iRowOut, iColOut, "" + cellValueBoolean, true);
                    }
                    else if ( tableColumnTypes[iColOut] == TableField.DATA_TYPE_DOUBLE ) {
                        if ( cellValueBoolean ) {
                            table.setFieldValue(iRowOut, iColOut, new Double(1.0), true);
                        }
                        else {
                            table.setFieldValue(iRowOut, iColOut, new Double(0.0), true);
                        }
                    }
                    else {
                        // Not able to convert
                        table.setFieldValue(iRowOut, iColOut, null, true);
                    }
                }
                else if ( cellType == Cell.CELL_TYPE_BLANK ) {
                    // Null works for all object types.  If truly a blank string in text cell, use "" as text
                    if ( cellMatchesFilter(columnNames[iCol],"",columnExcludeFiltersMap) ) {
                        // Add the row but will remove at the end after all columns are processed
                        needToSkipRow = true;
                    }
                    if ( tableColumnTypes[iColOut] == TableField.DATA_TYPE_STRING ) {
                        table.setFieldValue(iRowOut, iColOut, "", true);
                    }
                    else {
                        table.setFieldValue(iRowOut, iColOut, null, true);
                    }
                }
                else if ( cellType == Cell.CELL_TYPE_ERROR ) {
                    if ( tableColumnTypes[iColOut] == TableField.DATA_TYPE_STRING ) {
                        table.setFieldValue(iRowOut, iColOut, "", true);
                    }
                    else {
                        table.setFieldValue(iRowOut, iColOut, null, true);
                    }
                }
                else {
                    table.setFieldValue(iRowOut, iColOut, null, true);
                }
            }
            if ( needToSkipRow ) {
                // Because columns are added individually, need to remove rows that were added but should not have because
                // an exclude filter was matched
                table.deleteRecord(iRowOut);
                --iRowOut; // Will be incremented for next row
                continue;
            }
        }
    }
    catch ( Exception e ) {
        problems.add ( "Error reading workbook \"" + workbookFile + "\" (" + e + ")." );
        Message.printWarning(3,routine,e);
    }
    finally {
        inp.close();
    }
    return table;
}

/**
Is the row a comment?
@param sheet sheet being read
@param iRow row in sheet (0+)
@param comment if not null, character at start of row that indicates comment
*/
private boolean rowIsComment ( Sheet sheet, int iRow, String comment )
{   Row dataRow = sheet.getRow(iRow);
    Cell cell = dataRow.getCell(0);
    if ( (cell != null) && (cell.getCellType() == Cell.CELL_TYPE_STRING) ) {
        String cellValue = cell.getStringCellValue();
        if ( (cellValue != null) && (cellValue.length() > 0) &&
            cellValue.substring(0,1).equals(comment) ) {
            return true;
        }
    }
    return false;
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
{	String routine = "WriteTimeSeriesToExcelFile_Command.runCommand",message = "";
	int warning_level = 2;
	String command_tag = "" + command_number;	
	int warning_count = 0;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(command_phase);
    if ( command_phase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTable ( null );
    }

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

    String TableID = parameters.getValue ( "TableID" );
	String OutputFile = parameters.getValue ( "OutputFile" );
	String Worksheet = parameters.getValue ( "Worksheet" );
	String ExcelAddress = parameters.getValue ( "ExcelAddress" );
	String ExcelNamedRange = parameters.getValue ( "ExcelNamedRange" );
	String ExcelTableName = parameters.getValue ( "ExcelTableName" );
	String ExcelColumnNames = parameters.getValue ( "ExcelColumnNames" );
	if ( (ExcelColumnNames == null) || ExcelColumnNames.equals("") ) {
	    ExcelColumnNames = _ColumnN; // Default
	}
    String ColumnExcludeFilters = parameters.getValue ( "ColumnExcludeFilters" );
    Hashtable<String,String> columnExcludeFiltersMap = null;
    if ( (ColumnExcludeFilters != null) && (ColumnExcludeFilters.length() > 0) && (ColumnExcludeFilters.indexOf(":") > 0) ) {
        columnExcludeFiltersMap = new Hashtable<String,String>();
        // First break map pairs by comma
        List<String>pairs = StringUtil.breakStringList(ColumnExcludeFilters, ",", 0 );
        // Now break pairs and put in hashtable
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            String tableColumn = parts[0].trim().toUpperCase();
            String pattern = "";
            if ( parts.length > 1 ) {
                // Use upper-case to facilitate case-independent comparisons, and replace * globbing with internal Java notation
                pattern = parts[1].trim().toUpperCase().replace("*", ".*");
            }
            columnExcludeFiltersMap.put(tableColumn, pattern );
        }
    }
    /*
	String Comment = parameters.getValue ( "Comment" );
	String comment = null;
	if ( (Comment != null) && Comment.length() > 0 ) {
	    comment = Comment;
	}
	String ExcelIntegerColumns = parameters.getValue ( "ExcelIntegerColumns" );
	String [] excelIntegerColumns = null;
	if ( (ExcelIntegerColumns != null) && !ExcelIntegerColumns.equals("") ) {
	    excelIntegerColumns = ExcelIntegerColumns.split(",");
	    for ( int i = 0; i < excelIntegerColumns.length; i++ ) {
	        excelIntegerColumns[i] = excelIntegerColumns[i].trim();
	    }
	}
    String ExcelDateTimeColumns = parameters.getValue ( "ExcelDateTimeColumns" );
    String [] excelDateTimeColumns = null;
    if ( (ExcelDateTimeColumns != null) && !ExcelDateTimeColumns.equals("") ) {
        excelDateTimeColumns = ExcelDateTimeColumns.split(",");
        for ( int i = 0; i < excelDateTimeColumns.length; i++ ) {
            excelDateTimeColumns[i] = excelDateTimeColumns[i].trim();
        }
    }
    */
	String NumberPrecision = parameters.getValue ( "NumberPrecision" );
	int numberPrecision = 6; // default
	try {
	    numberPrecision = Integer.parseInt(NumberPrecision);
	}
	catch ( NumberFormatException e ) {
	    numberPrecision = 6;
	}
	String WriteAllAsText = parameters.getValue ( "WriteAllAsText" );
	boolean writeAllAsText = false;
	if ( (WriteAllAsText != null) && WriteAllAsText.equalsIgnoreCase("True") ) {
	    writeAllAsText = true;
	}

	String InputFile_full = IOUtil.verifyPathForOS(
        IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),OutputFile) );
	if ( !IOUtil.fileExists(InputFile_full) ) {
		message += "\nThe Excel workbook file \"" + InputFile_full + "\" does not exist.";
		++warning_count;
        status.addToLog ( command_phase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Verify that the delimited table file exists." ) );
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}

	// Now process the file...

	DataTable table = null;
	List<String> problems = new Vector<String>();
	try {
	    if ( command_phase == CommandPhaseType.RUN ) {
	        // TODO SAM 2013-0=11-03 Need to enable this
            //table = WriteTimeSeriesToExcelFile ( InputFile_full, Worksheet,
            //    ExcelAddress, ExcelNamedRange, ExcelTableName, ExcelColumnNames, columnExcludeFiltersMap,
            //    //comment, excelIntegerColumns, excelDateTimeColumns,
            //    numberPrecision, writeAllAsText, problems );
            for ( String problem: problems ) {
                Message.printWarning ( 3, routine, problem );
                message = "Error writing to Excel: " + problem;
                Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
                status.addToLog ( command_phase, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Check the log file for exceptions." ) );
            }
            // Set the table identifier...
            if ( table == null ) {
                // Create an empty table to have something in output for user
                table = new DataTable();
            }
            table.setTableID ( TableID );
	    }
	    else if ( command_phase == CommandPhaseType.DISCOVERY ) {
	        // Create an empty table.
	        table = new DataTable();
	        table.setTableID ( TableID );
	    }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error read table from delimited file \"" + InputFile_full + "\" (" + e + ").";
		Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
        status.addToLog ( command_phase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Verify that the file exists and is readable." ) );
		throw new CommandWarningException ( message );
	}
	
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
		throw new CommandWarningException ( message );
	}
    
    // Set the table in the processor...
    
    if ( command_phase == CommandPhaseType.RUN ) {
        PropList request_params = new PropList ( "" );
        request_params.setUsingObject ( "Table", table );
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
    }
    else if ( command_phase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTable ( table );
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
Set the first data row (0+).
*/
private void setFirstDataRow ( int row )
{
    __firstDataRow = row;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
    String TableID = props.getValue( "TableID" );
	String InputFile = props.getValue( "InputFile" );
	String Worksheet = props.getValue( "Worksheet" );
	String ExcelAddress = props.getValue("ExcelAddress");
	String ExcelNamedRange = props.getValue("ExcelNamedRange");
	String ExcelTableName = props.getValue("ExcelTableName");
	String ExcelColumnNames = props.getValue("ExcelColumnNames");
	String ColumnExcludeFilters = props.getValue("ColumnExcludeFilters");
	//String Comment = props.getValue("Comment");
	//String ExcelIntegerColumns = props.getValue("ExcelIntegerColumns");
	//String ExcelDateTimeColumns = props.getValue("ExcelDateTimeColumns");
	String NumberPrecision = props.getValue("NumberPrecision");
	String WriteAllAsText = props.getValue("WriteAllAsText");
	StringBuffer b = new StringBuffer ();
    if ( (TableID != null) && (TableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableID=\"" + TableID + "\"" );
    }
	if ( (InputFile != null) && (InputFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputFile=\"" + InputFile + "\"" );
	}
    if ( (Worksheet != null) && (Worksheet.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Worksheet=\"" + Worksheet + "\"" );
    }
	if ( (ExcelAddress != null) && (ExcelAddress.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "ExcelAddress=\"" + ExcelAddress + "\"" );
	}
	if ( (ExcelNamedRange != null) && (ExcelNamedRange.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "ExcelNamedRange=\"" + ExcelNamedRange + "\"" );
	}
	if ( (ExcelTableName != null) && (ExcelTableName.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "ExcelTableName=\"" + ExcelTableName + "\"" );
	}
    if ( (ExcelColumnNames != null) && (ExcelColumnNames.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ExcelColumnNames=" + ExcelColumnNames );
    }
    if ( (ColumnExcludeFilters != null) && (ColumnExcludeFilters.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ColumnExcludeFilters=\"" + ColumnExcludeFilters + "\"" );
    }
    /*
    if ( (Comment != null) && (Comment.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Comment=\"" + Comment + "\"" );
    }
    if ( (ExcelIntegerColumns != null) && (ExcelIntegerColumns.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ExcelIntegerColumns=\"" + ExcelIntegerColumns + "\"" );
    }
    if ( (ExcelDateTimeColumns != null) && (ExcelDateTimeColumns.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ExcelDateTimeColumns=\"" + ExcelDateTimeColumns + "\"" );
    }
    */
    if ( (NumberPrecision != null) && (NumberPrecision.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NumberPrecision=" + NumberPrecision );
    }
    if ( (WriteAllAsText != null) && (WriteAllAsText.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "WriteAllAsText=" + WriteAllAsText );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}