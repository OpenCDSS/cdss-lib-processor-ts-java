package rti.tscommandprocessor.commands.spreadsheet;

import javax.swing.JFrame;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormat;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.FileGenerator;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
//import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.IO.IOUtil;
import RTi.Util.String.StringDictionary;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the FormatExcelTable() command, using Apache POI.
A useful link is:  http://poi.apache.org/spreadsheet/quick-guide.html
*/
public class FormatExcelTable_Command extends AbstractCommand implements Command
{

/**
Possible values for ExcelColumnNames parameter.
*/
protected final String _FirstRowInRange = "FirstRowInRange";
protected final String _RowBeforeRange = "RowBeforeRange";

/**
Possible values for KeepOpen parameter.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
Constructor.
*/
public FormatExcelTable_Command ()
{	super();
	setCommandName ( "FormatExcelTable" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	String StyleTableID = parameters.getValue ( "StyleTableID" );
    String OutputFile = parameters.getValue ( "OutputFile" );
    String ExcelColumnNames = parameters.getValue ( "ExcelColumnNames" );
	String KeepOpen = parameters.getValue ( "KeepOpen" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	// If the input file does not exist, warn the user...

	//String working_dir = null;
	
	CommandProcessor processor = getCommandProcessor();
	
    if ( (StyleTableID == null) || (StyleTableID.length() == 0) ) {
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
			//working_dir = (String)o;
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
	/** TODO SAM 2014-01-12 Evaluate whether to only do this check at run-time
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
	*/
	
    if ( ExcelColumnNames != null &&
        !ExcelColumnNames.equalsIgnoreCase(_FirstRowInRange) &&
        !ExcelColumnNames.equalsIgnoreCase(_RowBeforeRange) && !ExcelColumnNames.equalsIgnoreCase("")) {
        message = "ExcelColumnNames is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "ExcelColumnNames must be " + _FirstRowInRange +
                " (default), or " + _RowBeforeRange ) );
    }
    
    if ( KeepOpen != null && !KeepOpen.equalsIgnoreCase(_True) && 
        !KeepOpen.equalsIgnoreCase(_False) && !KeepOpen.equalsIgnoreCase("") ) {
        message = "KeepOpen is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "KeepOpen must be specified as " + _False + " (default) or " + _True ) );
    }

	// TODO SAM 2005-11-18 Check the format.
    
	//  Check for invalid parameters...
	List<String> validList = new ArrayList<String>(14);
    validList.add ( "OutputFile" );
    validList.add ( "Worksheet" );
    validList.add ( "ExcelAddress" );
    validList.add ( "ExcelNamedRange" );
    validList.add ( "ExcelTableName" );
    validList.add ( "ExcelColumnNames" );
    validList.add ( "KeepOpen" );
    validList.add ( "IncludeColumns" );
    validList.add ( "ExcludeColumns" );
    validList.add ( "ColumnIncludeFilters" );
    validList.add ( "ColumnExcludeFilters" );
    validList.add ( "StyleTableID" );
    validList.add ( "Formula" );
    validList.add ( "Style" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );    

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
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
    List<String> tableIDChoices = TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)getCommandProcessor(), this);
	return (new FormatExcelTable_JDialog ( parent, this, tableIDChoices )).ok();
}

/**
Format the table from an Excel worksheet.  The cells must be specified by a contiguous address block specified
by one of the parameters excelAddress, excelNamedRange, excelTableName.
@param workbookFile the name of the workbook file (*.xls or *.xlsx)
@param sheetName the name of the sheet in the workbook
@param excelAddress Excel address range (e.g., A1:D10 or $A1:$D10 or variant)
@param excelNamedRange a named range
@param excelTableName a table name
@param excelColumnNames indicate how to determine column names from the Excel worksheet
@param columnIncludeFiltersMap a map indicating patterns for column values, to include rows
@param columnExcludeFiltersMap a map indicating patterns for column values, to exclude rows
@param comment character that if at start of first column indicates row is a comment
@param excelIntegerColumns names of columns that should be treated as integers, or null if none
@param numberPrecision digits after decimal for floating point numbers (can't yet determine from Excel)
@param readAllAsText if True, treat all data as text values
@param problems list of problems encountered during read, for formatted logging in calling code
@return a DataTable with the Excel contents
*/
/*
private void formatExcelTable ( String workbookFile, String sheetName, boolean keepOpen,
    String excelAddress, String excelNamedRange, String excelTableName, ExcelColumnNameRowType excelColumnNames,
    StringDictionary columnIncludeFiltersMap, StringDictionary columnExcludeFiltersMap,
    String comment,
    String [] excelIntegerColumns, String [] excelDateTimeColumns, String [] excelTextColumns,
    int numberPrecision, boolean readAllAsText, List<String> problems )
throws FileNotFoundException, IOException
{   String routine = getClass().getSimpleName() + ".formatExcelTable", message;
    DataTable table = new DataTable();
    if ( (comment != null) && (comment.trim().length() == 0) ) {
        // Set to null to simplify logic below
        comment = null;
    }

    ExcelToolkit tk = new ExcelToolkit();
    Workbook wb = null;
    InputStream inp = null;
    try {
        // Only operate on an open workbook by the same name exists
        wb = ExcelUtil.getOpenWorkbook(workbookFile);
        if ( wb == null ) {
            problems.add ( "Unable to find open workbook file \"" + workbookFile + "\"." );
            return;
        }
        Sheet sheet = null;
        // TODO SAM 2013-02-22 In the future sheet may be determined from named address (e.g., named ranges
        // are global in workbook)
        if ( (sheetName == null) || sheetName.isEmpty() ) {
            // Default is to use the first sheet
            sheet = wb.getSheetAt(0);
            if ( sheet == null ) {
                problems.add ( "Workbook does not include any worksheets" );
                return;
            }
        }
        else {
            sheet = wb.getSheet(sheetName);
            if ( sheet == null ) {
                problems.add ( "Workbook does not include worksheet named \"" + sheetName + "\"" );
                return;
            }
        }
        // Get the contiguous block of data to process by evaluating user input - this is mainly used to get column names
        AreaReference area = tk.getAreaReference ( wb, sheet, excelAddress, excelNamedRange, excelTableName );
        if ( area == null ) {
            problems.add ( "Unable to get worksheet area reference from address information." );
            return;
        }
        Message.printStatus(2,routine,"Excel address block to process: " + area );
        // Create a temporary table based on the first row of the area to know what the column data types are from utility method
        Object [] o = tk.createTableColumns ( table, wb, sheet, area, excelColumnNames, comment,
            excelIntegerColumns, excelDateTimeColumns, excelTextColumns, numberPrecision, readAllAsText, problems );
        String [] columnNames = (String [])o[0];
        int [] tableColumnTypes = table.getFieldDataTypes();
        // Read the data from the area, check against the formula, and set the cell style accordingly.
        Row row;
        Cell cell;
        int rowStart = (Integer)o[1]; // Set in createTableColumns()
        int rowEnd = area.getLastCell().getRow();
        int colStart = area.getFirstCell().getCol();
        int colEnd = area.getLastCell().getCol();
        String numberFormat = "%." + numberPrecision + "f"; // Used to format numeric to text to avoid Java exponential notation
        Message.printStatus(2, routine, "Cell range is [" + rowStart + "][" + colStart + "] to [" + rowEnd + "][" + colEnd + "]");
        int cellType;
        int iRowOut = -1, iColOut;
        String cellValueString;
        Object cellValueObject = null; // Generic cell object for logging
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
            if ( Message.isDebugOn ) {
                Message.printDebug(1, routine, "Processing row [" + iRow + "] end at [" + rowEnd + "]" );
            }
            int updateDelta = nRowsToRead/20;
            if ( updateDelta == 0 ) {
                updateDelta = 2;
            }
            if ( (iRow == rowStart) || (iRow == rowEnd) || (iRow%updateDelta == 0) ) {
                // Update the progress bar every 5%
                message = "Reading row " + (iRow - rowStart + 1) + " of " + nRowsToRead;
                notifyCommandProgressListeners ( (iRow - rowStart), nRowsToRead, (float)-1.0, message );
            }
            if ( (comment != null) && tk.isRowComment(sheet, iRow, comment) ) {
                // No need to process the row.
                continue;
            }
            // Determine whether row should be included...
            if ( !tk.rowShouldBeIncluded(row,columnIncludeFiltersMap,columnExcludeFiltersMap) ) {
            	continue;
            }
            // If here the row should be included, although some columns may not be
            ++iRowOut;
            needToSkipRow = false;
            iColOut = -1;
            for ( int iCol = colStart; iCol <= colEnd; iCol++ ) {
                ++iColOut;
                cell = row.getCell(iCol);
                try {
                    if ( cell == null ) {
                        if ( Message.isDebugOn ) {
                            Message.printDebug(1, routine, "Cell [" + iRow + "][" + iCol + "]= \"" + cell + "\"" );
                        }
                        String cellValue = null;
                        if ( tableColumnTypes[iColOut] == TableField.DATA_TYPE_STRING ) {
                            cellValue = "";
                        }
                        table.setFieldValue(iRowOut, iColOut, cellValue, true);
                        if ( (tableColumnTypes[iColOut] == TableField.DATA_TYPE_STRING) &&
                            !tk.cellMatchesIncludeFilter(columnNames[iCol - colStart], cellValue, columnIncludeFiltersMap) ) {
                            // Row was added but will remove at the end after all columns are processed
                            needToSkipRow = true;
                        }
                        if ( (tableColumnTypes[iColOut] == TableField.DATA_TYPE_STRING) &&
                            tk.cellMatchesExcludeFilter(columnNames[iCol - colStart], cellValue, columnExcludeFiltersMap) ) {
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
                    cellValueObject = null; // For try/catch
                    cellType = cell.getCellType();
                    if ( Message.isDebugOn ) {
                        Message.printDebug(1, routine, "Cell [" + iRow + "][" + iCol + "]= \"" + cell + "\" type=" +
                            cellType + " " + tk.lookupExcelCellType(cellType));
                    }
                    cellIsFormula = false;
                    if ( cellType == Cell.CELL_TYPE_FORMULA ) {
                        // Have to evaluate the cell and get the value as the result
                        cellIsFormula = true;
                        try {
                            FormulaEvaluator formulaEval = wb.getCreationHelper().createFormulaEvaluator();
                            formulaCellValue = formulaEval.evaluate(cell);
                            // Reset cellType for following code
                            cellType = formulaCellValue.getCellType();
                            if ( Message.isDebugOn ) {
                                Message.printDebug(1, routine, "Detected formula, new cellType=" + cellType +
                                    ", cell value=\"" + formulaCellValue + "\"" );
                            }
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
                        cellValueObject = cellValueString; // For try/catch
                        if ( !tk.cellMatchesIncludeFilter(columnNames[iCol], cellValueString,columnIncludeFiltersMap) ) {
                             // Add the row but will remove at the end after all columns are processed
                             needToSkipRow = true;
                        }
                        if ( tk.cellMatchesExcludeFilter(columnNames[iCol], cellValueString,columnExcludeFiltersMap) ) {
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
                            cellValueObject = cellValueDate; // For try/catch
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
                            cellValueObject = cellValueDouble; // For try/catch
                            if ( tableColumnTypes[iColOut] == TableField.DATA_TYPE_DOUBLE ) {
                                // Double to double
                                table.setFieldValue(iRowOut, iColOut, new Double(cellValueDouble), true);
                            }
                            else if ( tableColumnTypes[iColOut] == TableField.DATA_TYPE_STRING ) {
                                // Double to string - have to format number because Java will use exponential notation
                                table.setFieldValue(iRowOut, iColOut, StringUtil.formatString(cellValueDouble,numberFormat), true);
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
                        cellValueObject = cellValueBoolean; // For try/catch
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
                        if ( !tk.cellMatchesIncludeFilter(columnNames[iColOut],"",columnIncludeFiltersMap) ) {
                            // Add the row but will remove at the end after all columns are processed
                            needToSkipRow = true;
                        }
                        if ( tk.cellMatchesExcludeFilter(columnNames[iColOut],"",columnExcludeFiltersMap) ) {
                            // Add the row but will remove at the end after all columns are processed
                            needToSkipRow = true;
                        }
                        cellValueObject = "blank"; // For try/catch
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
                        cellValueObject = "error"; // For try/catch
                    }
                    else {
                        table.setFieldValue(iRowOut, iColOut, null, true);
                    }
                }
                catch ( Exception e ) {
                    problems.add ( "Error processing Excel [" + iRow + "][" + iCol + "] = " +
                        cellValueObject + " (as string) skipping cell (" + e + ")." );
                    Message.printWarning(3,routine,e);
                }
            }
        }
    }
    catch ( Exception e ) {
        problems.add ( "Error formatting workbook \"" + workbookFile + "\" (" + e + ")." );
        Message.printWarning(3,routine,e);
    }
    finally {
        // If keeping open skip because it will be written by a later command.
        if ( keepOpen ) {
            // Save the open workbook for other commands to use
            ExcelUtil.setOpenWorkbook(workbookFile,wb);
        }
        else {
            if ( inp != null ) {
                inp.close();
            }
            ExcelUtil.removeOpenWorkbook(workbookFile);
        }
    }
}
*/

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException
{	String routine = getClass().getSimpleName() + ".runCommand", message = "";
	int warning_level = 2;
	String command_tag = "" + command_number;	
	int warning_count = 0;
    
    CommandStatus status = getCommandStatus();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
    status.clearLog(commandPhase);
    
	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

	String OutputFile = parameters.getValue ( "OutputFile" );
	String Worksheet = parameters.getValue ( "Worksheet" );
	String ExcelAddress = parameters.getValue ( "ExcelAddress" );
	String ExcelNamedRange = parameters.getValue ( "ExcelNamedRange" );
	String ExcelTableName = parameters.getValue ( "ExcelTableName" );
	String ExcelColumnNames = parameters.getValue ( "ExcelColumnNames" );
	ExcelColumnNameRowType excelColumnNames = ExcelColumnNameRowType.COLUMN_N; // Default
	if ( (ExcelColumnNames != null) & !ExcelColumnNames.isEmpty() ) {
	    excelColumnNames = ExcelColumnNameRowType.valueOfIgnoreCase(ExcelColumnNames);  
	}
    String KeepOpen = parameters.getValue ( "KeepOpen" );
    boolean keepOpen = false; // default
    if ( (KeepOpen != null) && KeepOpen.equalsIgnoreCase("True") ) {
        keepOpen = true;
    }
    String IncludeColumns = parameters.getValue ( "IncludeColumns" );
    String [] includeColumns = null;
    if ( (IncludeColumns != null) && !IncludeColumns.isEmpty() ) {
        // Use the provided columns
        includeColumns = IncludeColumns.split(",");
        for ( int i = 0; i < includeColumns.length; i++ ) {
            includeColumns[i] = includeColumns[i].trim();
        }
    }
    String ExcludeColumns = parameters.getValue ( "ExcludeColumns" );
    String [] excludeColumns = null;
    if ( (ExcludeColumns != null) && !ExcludeColumns.isEmpty() ) {
        // Use the provided columns
        excludeColumns = ExcludeColumns.split(",");
        for ( int i = 0; i < excludeColumns.length; i++ ) {
            excludeColumns[i] = excludeColumns[i].trim();
        }
    }
    String ColumnIncludeFilters = parameters.getValue ( "ColumnIncludeFilters" );
    StringDictionary columnIncludeFilters = new StringDictionary(ColumnIncludeFilters,":",",");
    // Expand the filter information
    if ( (ColumnIncludeFilters != null) && (ColumnIncludeFilters.indexOf("${") >= 0) ) {
        LinkedHashMap<String, String> map = columnIncludeFilters.getLinkedHashMap();
        String key = null;
        for ( Map.Entry<String,String> entry : map.entrySet() ) {
            key = entry.getKey();
            String key2 = TSCommandProcessorUtil.expandParameterValue(processor,this,key);
            map.put(key2, TSCommandProcessorUtil.expandParameterValue(processor,this,map.get(key)));
            map.remove(key);
        }
    }
    String ColumnExcludeFilters = parameters.getValue ( "ColumnExcludeFilters" );
    StringDictionary columnExcludeFilters = new StringDictionary(ColumnExcludeFilters,":",",");
    if ( (ColumnExcludeFilters != null) && (ColumnExcludeFilters.indexOf("${") >= 0) ) {
        LinkedHashMap<String, String> map = columnExcludeFilters.getLinkedHashMap();
        String key = null;
        for ( Map.Entry<String,String> entry : map.entrySet() ) {
            key = entry.getKey();
            String key2 = TSCommandProcessorUtil.expandParameterValue(processor,this,key);
            map.put(key2, TSCommandProcessorUtil.expandParameterValue(processor,this,map.get(key)));
            map.remove(key);
        }
    }
    String StyleTableID = parameters.getValue ( "StyleTableID" );
    String Formula = parameters.getValue ( "Formula" );
    String Style = parameters.getValue ( "Style" );
	
	// Get the table to process
	
    PropList request_params = new PropList ( "" );
    request_params.set ( "TableID", StyleTableID );
    CommandProcessorRequestResultsBean bean = null;
    try {
        bean = processor.processRequest( "GetTable", request_params);
    }
    catch ( Exception e ) {
        message = "Error requesting GetTable(TableID=\"" + StyleTableID + "\") from processor.";
        Message.printWarning(warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Report problem to software support." ) );
    }
    PropList bean_PropList = bean.getResultsPropList();
    Object o_Table = bean_PropList.getContents ( "Table" );
    DataTable table = null;
    if ( o_Table == null ) {
        message = "Unable to find table to process using TableID=\"" + StyleTableID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Verify that a table exists with the requested ID." ) );
    }
    else {
        table = (DataTable)o_Table;
    }

	String OutputFile_full = IOUtil.verifyPathForOS(
        IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),OutputFile) );
	if ( (ExcelUtil.getOpenWorkbook(OutputFile_full) == null) && !IOUtil.fileExists(OutputFile_full) ) {
		message += "\nThe Excel workbook file \"" + OutputFile_full + "\" is not open from a previous command and does not exist.";
		++warning_count;
        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Verify that the Excel workbook file is open in memory or exists as a file." ) );
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}

	// Now process the file...

	List<String> problems = new ArrayList<String>();
	try {
        // Check that named ranges match columns
	    int [] includeColumnNumbers = null;
	    if ( (includeColumns != null) && (includeColumns.length > 0) ) {
	        // Get the column numbers to output
	        includeColumnNumbers = new int[includeColumns.length];
	        for ( int i = 0; i < includeColumns.length; i++ ) {
	            try {
	                includeColumnNumbers[i] = table.getFieldIndex(includeColumns[i]);
	            }
	            catch ( Exception e ) {
	                message = "Table column to include in output \"" + includeColumns[i] + "\" does not exist in table.";
	                Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
	                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
	                    message, "Check the table column names." ) );
	                includeColumnNumbers[i] = -1;
	            }
	        }
	    }
	    else {
	        // Output all the columns
	        includeColumnNumbers = new int[table.getNumberOfFields()];
	        for ( int i = 0; i < includeColumnNumbers.length; i++ ) {
	            includeColumnNumbers[i] = i;
	        }
	    }
	    // Now remove output columns that are to be excluded.  Do so by setting column numbers for excluded columns to -1
	    if ( (excludeColumns != null) && (excludeColumns.length > 0) ) {
	        // Get the column numbers to exclude
	        for ( int i = 0; i < excludeColumns.length; i++ ) {
	            try {
	                int excludeColumnNumber = table.getFieldIndex(excludeColumns[i]);
	                // See if it exists in the array
	                for ( int j = 0; j < includeColumnNumbers.length; j++ ) {
	                	if ( includeColumnNumbers[j] == excludeColumnNumber ) {
	                		includeColumnNumbers[j] = -1;
	                	}
	                }
	            }
	            catch ( Exception e ) {
	                message = "Table column to exclude in output \"" + excludeColumns[i] + "\" does not exist in table.";
	                Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
	                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
	                    message, "Check the table column names." ) );
	                includeColumnNumbers[i] = -1;
	            }
	        }
	    }
	    // Finally, remove column numbers -1 so only valid columns that are requested are output
        int count = 0;
        for ( int i = 0; i < includeColumnNumbers.length; i++ ) {
            if ( includeColumnNumbers[i] >= 0 ) {
                ++count;
            }
        }
        int [] includeColumnNumbers2 = new int[count];
        count = 0;
        for ( int i = 0; i < includeColumnNumbers.length; i++ ) {
            if ( includeColumnNumbers[i] >= 0 ) {
                includeColumnNumbers2[count++] = includeColumnNumbers[i];
            }
        }
        includeColumnNumbers = includeColumnNumbers2;
        // FIXME SAM 2015-05-21 Dummy these in for now
        String [] excelIntegerColumns = new String[0];
        String [] excelDateTimeColumns = new String[0];
        String [] excelTextColumns = new String[0];
        int numberPrecision = 6;
        String Comment = "";
        boolean readAllAsText = false;
        /* FIXME SAM 2015-06-03 Need to enable
        formatExcelTable ( OutputFile_full, Worksheet, keepOpen,
    	    ExcelAddress, ExcelNamedRange, ExcelTableName, excelColumnNames,
    	    columnIncludeFilters, columnExcludeFilters,
    	    Comment,
    	    excelIntegerColumns, excelDateTimeColumns, excelTextColumns,
    	    numberPrecision, readAllAsText, problems );
    	    */
        for ( String problem: problems ) {
            Message.printWarning ( 3, routine, problem );
            message = "Error formatting Excel table: " + problem;
            Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Check the log file for exceptions." ) );
        }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error writing table to Excel workbook file \"" + OutputFile_full + "\" (" + e + ").";
		Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Verify that the file exists and is writeable." ) );
		throw new CommandWarningException ( message );
	}
	
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
		throw new CommandWarningException ( message );
	}
    
    status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
	String OutputFile = props.getValue( "OutputFile" );
	String Worksheet = props.getValue( "Worksheet" );
	String ExcelAddress = props.getValue("ExcelAddress");
	String ExcelNamedRange = props.getValue("ExcelNamedRange");
	String ExcelTableName = props.getValue("ExcelTableName");
	String ExcelColumnNames = props.getValue("ExcelColumnNames");
	String KeepOpen = props.getValue("KeepOpen");
    String IncludeColumns = props.getValue( "IncludeColumns" );
    String ExcludeColumns = props.getValue( "ExcludeColumns" );
	String ColumnIncludeFilters = props.getValue("ColumnIncludeFilters");
	String ColumnExcludeFilters = props.getValue("ColumnExcludeFilters");
    String StyleTableID = props.getValue( "StyleTableID" );
    String Formula = props.getValue( "Formula" );
    String Style = props.getValue( "Style" );
	StringBuffer b = new StringBuffer ();
	if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "OutputFile=\"" + OutputFile + "\"" );
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
    if ( (KeepOpen != null) && (KeepOpen.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "KeepOpen=" + KeepOpen );
    }
    if ( (IncludeColumns != null) && (IncludeColumns.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IncludeColumns=\"" + IncludeColumns + "\"" );
    }
    if ( (ExcludeColumns != null) && (ExcludeColumns.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ExcludeColumns=\"" + ExcludeColumns + "\"" );
    }
    if ( (ColumnIncludeFilters != null) && (ColumnIncludeFilters.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ColumnIncludeFilters=\"" + ColumnIncludeFilters + "\"" );
    }
    if ( (ColumnExcludeFilters != null) && (ColumnExcludeFilters.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ColumnExcludeFilters=\"" + ColumnExcludeFilters + "\"" );
    }
    if ( (StyleTableID != null) && (StyleTableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "StyleTableID=\"" + StyleTableID + "\"" );
    }
    if ( (Formula != null) && (Formula.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Formula=\"" + Formula + "\"" );
    }
    if ( (Style != null) && (Style.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Style=\"" + Style + "\"" );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}