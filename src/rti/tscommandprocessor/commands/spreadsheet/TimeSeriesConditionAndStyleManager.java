package rti.tscommandprocessor.commands.spreadsheet;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;

import RTi.GR.GRColor;
import RTi.TS.TS;
import RTi.Util.Message.Message;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableRecord;

/**
Manager to handle styling of time series values when written to Excel cells.
*/
public class TimeSeriesConditionAndStyleManager
{

/**
Time series to be output.
*/
private List<TS> tslist = null;

/**
Data table with style data.
*/
private DataTable styleTable = null;

/**
Data table with condition data.
*/
private DataTable conditionTable = null;

/**
Column number for "Column" column in condition table.
*/
private int conditionTableColumnNum = -1;

/**
Column number for "Condition" column in condition table.
*/
private int conditionTableConditionNum = -1;

/**
Column number for "StyleID" column in condition table.
*/
private int conditionTableStyleIDNum = -1;

/**
Excel styles corresponding to defaults for the columns, will be augmented by cell styles.
Column styles are those defined by legacy command parameters and formatTable entries where condition is blank.
*/
private CellStyle [] columnStyles = null;

/**
Excel data formats corresponding to defaults for the columns, will be augmented by cell styles.
Column styles are those defined by legacy command parameters and formatTable entries where condition is blank.
*/
private DataFormat [] columnDataFormats = null;

/**
Excel styles corresponding to the style table rows.
The key is either "col" (0+) table column being output, which is the default style for the column
or "col-StyleID", which is the style for the column matching the StyleID.
*/
private Hashtable<String,CellStyle> cellStyleHash = new Hashtable<String,CellStyle>();

/**
Boolean to track whether cells styles have been generated.
This is checked when cell styles are requested.
Prior to these requests some column-based styles may be set.
*/
private boolean cellStylesInitialized = false;

/**
Workbook used to create styles.
*/
private Workbook wb = null;

/**
Constructor.
*/
public TimeSeriesConditionAndStyleManager ( List<TS> tslist, DataTable conditionTable, DataTable styleTable, Workbook wb )
{
	this.tslist = tslist;
	this.styleTable = styleTable;
	this.conditionTable = conditionTable;
	this.wb = wb;
	try {
		this.conditionTableColumnNum = conditionTable.getFieldIndex("Column");
	}
	catch ( Exception e ) {
		throw new RuntimeException("Format table does not include \"Column\" column (" + e + ")");
	}
	try {
		this.conditionTableConditionNum = conditionTable.getFieldIndex("Condition");
	}
	catch ( Exception e ) {
		throw new RuntimeException("Format table does not include \"Condition\" column (" + e + ")");
	}
	try {
		this.conditionTableStyleIDNum = conditionTable.getFieldIndex("StyleID");
	}
	catch ( Exception e ) {
		throw new RuntimeException("Format table does not include \"StyleID\" column (" + e + ")");
	}
	// Create styles for each time series
	this.columnStyles = new CellStyle[this.tslist.size()];
	this.columnDataFormats = new DataFormat[this.tslist.size()];
	for ( int i = 0; i < this.columnStyles.length; i++ ) {
		// The following creates default styles that can be modified by calls to:
		// - setColumnDataFormat().
		this.columnStyles[i] = wb.createCellStyle();
		this.columnDataFormats[i] = wb.createDataFormat();
	}
	// Cell styles will be a copy of the column style but with version for each format,
	// as initialized in initializeCellStyles() - don't do here because other methods
	// may be called to provide additional defaults.
}

/**
Return the CellStyle to use for the specified StyleID.
@param col output column (0+)
@param styleID style ID to look up
*/
private CellStyle getCellStyleForStyleID ( int col, String styleID )
throws Exception
{
	List<String> columnNames = new ArrayList<String>(1);
	columnNames.add("StyleID");
	List<Object> columnValues = new ArrayList<Object>(1);
	columnValues.add(styleID);
	if ( (styleID == null) || styleID.isEmpty() ) {
		return this.cellStyleHash.get("" + col);
	}
	else {
		return this.cellStyleHash.get("" + col + "-" + styleID.toUpperCase() );
	}
}

/**
Get the style to use for the requested column.
The format table is used to find rows with matching column.
@param ts output time series
@param its output time series 0+
@param value the data value for the time series
@param flag the data flag for the time series
*/
public CellStyle getStyle ( TS ts, int its, double value, String flag )
{	String routine = "getStyle";
	if ( !this.cellStylesInitialized ) {
		initializeCellStyles();
	}
	// See if the column name matches any rows in the format table
	List<TableRecord> matchedRowList = new ArrayList<TableRecord>();
	for ( int fRow = 0; fRow < this.conditionTable.getNumberOfRecords(); fRow++ ) {
		try {
			TableRecord row = this.conditionTable.getRecord(fRow);
			String column = row.getFieldValueString(this.conditionTableColumnNum);
			if ( ts.getIdentifier().matches(column)) {
				// Format table row matches
				matchedRowList.add(row);
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	// If here then matched the column in the format table with the requested column
	// Get the condition
	try {
		if ( matchedRowList.size() == 0 ) {
			// Return the default column style
			if ( Message.isDebugOn ) {
				Message.printDebug(1, routine, "Time series \"" + ts.getIdentifier().toStringAliasAndTSID() +
					"\" did not match any format table rows - defaulting to time series style");
			}
			return this.columnStyles[its];
		}
		else {
			if ( Message.isDebugOn ) {
				Message.printDebug(1, routine, "Time Series \"" + ts.getIdentifier().toStringAliasAndTSID() + "\" matched format row - evaluating conditions");
			}
		}
		boolean doValue = false; // Is formatting based on time series value?
		boolean doFlag = false; // Is formatting based on time series value?
		for ( TableRecord row : matchedRowList ) {
			doValue = false;
			doFlag = false;
			String cond = row.getFieldValueString(this.conditionTableConditionNum);
			String styleID = row.getFieldValueString(this.conditionTableStyleIDNum);
			// Evaluate the condition - if true return the cell style for the matching StyleID
			// The condition consists of Value1 Cond Value2 [AND Value3 Cond Value4] etc
			// Split the condition
			String [] parts = cond.trim().split(" ");
			for ( int iPart = 0; iPart < parts.length; iPart++ ) {
				String sValue1 = parts[iPart].trim();
				if ( (iPart > 0) && sValue1.equalsIgnoreCase("and") ) {
					// Continue so that the next clause will be evaluated.
					// If at any time the entire condition is false, nothing will be returned
					continue;
				}
				if ( sValue1.equalsIgnoreCase("${tsdata:value}") ) {
					doValue = true;
				}
				if ( sValue1.equalsIgnoreCase("${tsdata:flag}") ) {
					doFlag = true;
				}
				// TODO SAM 2015-05-09 in the future add other cell properties to be check, such as comments/annotations
				boolean valueMatch = false;
				if ( doValue ) {
					boolean valueIsMissing = ts.isDataMissing(value);
					ConditionOperatorType oper = ConditionOperatorType.valueOfIgnoreCase(parts[++iPart].trim());
					String sValue2 = parts[++iPart].trim();
					boolean value2Missing = false; // whether checking for missing
					if ( sValue2.equalsIgnoreCase("missing") || sValue2.equalsIgnoreCase("null")) {
						value2Missing = true;
					}
					double value2 = 0.0;
					if ( !value2Missing ) {
						value2 = Double.parseDouble(sValue2);
					}
					if ( Message.isDebugOn ) {
						Message.printDebug(1,routine,"Checking value \"" + value + "\" operator " + oper + " value2 " + value2 + " value2missing " + value2Missing );
					}
					if ( oper == ConditionOperatorType.EQUAL_TO ) {
						if ( value2Missing ) {
							if ( valueIsMissing ) {
								valueMatch = true;
							}
						}
						else if ( !valueIsMissing && (value == value2) ) {
							valueMatch = true;
						}
					}
					else if ( oper == ConditionOperatorType.NOT_EQUAL_TO ) {
						if ( value2Missing ) {
							if ( !valueIsMissing ) {
								valueMatch = true;
							}
						}
						else if ( !valueIsMissing && (value != value2) ) {
							valueMatch = true;
						}
					}
					else if ( oper == ConditionOperatorType.LESS_THAN ) {
						if ( !valueIsMissing && (value < value2) ) {
							valueMatch = true;
						}
					}
					else if ( oper == ConditionOperatorType.LESS_THAN_OR_EQUAL_TO ) {
						 if ( !valueIsMissing && (value <= value2) ) {
							valueMatch = true;
						}
					}
					else if ( oper == ConditionOperatorType.GREATER_THAN ) {
						if ( !valueIsMissing && (value > value2) ) {
							valueMatch = true;
						}
					}
					else if ( oper == ConditionOperatorType.GREATER_THAN_OR_EQUAL_TO ) {
						 if ( !valueIsMissing && (value >= value2) ) {
							valueMatch = true;
						}
					}
				}
				else if ( doFlag ) {
					ConditionOperatorType oper = ConditionOperatorType.valueOfIgnoreCase(parts[++iPart].trim());
					String value2 = parts[++iPart].trim();
					boolean value2Missing = false; // whether checking for missing
					if ( value2.equalsIgnoreCase("missing") || value2.equalsIgnoreCase("null")) {
						value2Missing = true;
					}
					if ( Message.isDebugOn ) {
						Message.printDebug(1,routine,"Checking value \"" + value + "\" operator " + oper + " value2 " + value2 + " value2missing " + value2Missing );
					}
					if ( oper == ConditionOperatorType.EQUAL_TO ) {
						if ( value2Missing ) {
							if ( (flag == null) || flag.isEmpty() ) {
								valueMatch = true;
							}
						}
						else if ( !(flag == null) && flag.equals(value2) ) {
							valueMatch = true;
						}
					}
				}
				// Only finish if there are no more clauses to evaluate
				if ( ((iPart + 1) == parts.length) && valueMatch ) {
					// The format table row matched for the value
					// Lookup the style and return 
					if ( Message.isDebugOn ) {
						Message.printDebug(1,routine,"Setting cell style for value " + value + " to " + styleID );
					}
					CellStyle cs = getCellStyleForStyleID ( its, styleID );
					//Message.printStatus(2,routine,"Cell style fill foreground color is " + cs.getFillForegroundColor());
                    //Message.printStatus(2,routine,"Cell style fill background color is " + cs.getFillBackgroundColor());
                    //Message.printStatus(2,routine,"Cell style fill pattern is " + cs.getFillPattern());
					return cs;
				}
			}
		}
	}
	catch (Exception e) {
		Message.printWarning(3,"getStyle",e);
		throw new RuntimeException(e);
	}
	try {
		if ( Message.isDebugOn ) {
			Message.printDebug(1,routine,"Setting cell style for value " + value + " to column default" );
		}
		return getCellStyleForStyleID(its, null); // Fall-through
	}
	catch ( Exception e ) {
		throw new RuntimeException ( e );
	}
}

/**
Initialize the cell styles for combinations of formats and styles.
The output is a hashtable that includes styles for each column:
<ol>
<li> for default column style, hash key is the string column number (0+)</li>
<li> for specific style, hash key is string column number + "-" + style ID</li>
</ol>
*/
private void initializeCellStyles ()
{	String routine = getClass().getSimpleName() + ".initializeCellStyles";
	// By this point the this.columnStyles will have been configured so set the styles for the columns to these defaults.
	// Get the style table columns
	// "StyleID" - the string that identifies the style
	int styleIDColNum = -1;
	try {
		styleIDColNum = this.styleTable.getFieldIndex("StyleID");
	}
	catch ( Exception e ) {
		styleIDColNum = -1;
	}
	// "FillForegroundColor" - the fill color for the cell
	int fillFGColorColNum = -1;
	try {
		fillFGColorColNum = this.styleTable.getFieldIndex("FillForegroundColor");
	}
	catch ( Exception e ) {
		fillFGColorColNum = -1;
	}
	String key;
	for ( int iColStyle = 0; iColStyle < this.columnStyles.length; iColStyle++ ) {
		key = "" + iColStyle;
		this.cellStyleHash.put(key, this.columnStyles[iColStyle]);
		//Message.printStatus(2, routine, "Initialized column cell style \"" + key + "\"");
		// Next, loop through the styles in the style table and add for each column
		// TODO SAM 2015-06-09 Need to filter these down using format table so only have styles for specific columns.
		//   Currently extra styles are added that may not be used - could be a an issue with large tables
		TableRecord styleRow = null;
		String styleID = null;
		String fillFGColor = null;
		for ( int iStyle = 0; iStyle < styleTable.getNumberOfRecords(); iStyle++ ) {
			try {
				styleRow = this.styleTable.getRecord(iStyle);
				styleID = styleRow.getFieldValueString(styleIDColNum);
				if ( fillFGColorColNum >= 0 ) {
					fillFGColor = styleRow.getFieldValueString(fillFGColorColNum);
				}
			}
			catch ( Exception e ) {
				continue;
			}
			// Create a new style that is a copy of the column style
			XSSFCellStyle cs = (XSSFCellStyle)this.wb.createCellStyle();
			cs.cloneStyleFrom(this.columnStyles[iColStyle]);
			// Now set more specific styles based on information in the style table
			if ( fillFGColorColNum >= 0 ) {
				// Color may have various forms so parse using GRColor method
				// Apparently foreground default is black
				// Setting background does nothing for solid since foreground is used.
				GRColor c = GRColor.parseColor(fillFGColor);
				//Message.printStatus(2,routine,"Color is \"" + fillFGColor + "\" " + c.getRed() + "," + c.getGreen() + "," + c.getBlue());
				cs.setFillForegroundColor(new XSSFColor(c));
				//cs.setFillBackgroundColor(new XSSFColor(c));
				//cs.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
				//cs.setFillBackgroundColor(IndexedColors.RED.getIndex());
				cs.setFillPattern(CellStyle.SOLID_FOREGROUND);
				//Message.printStatus(2,routine,"Setting style fill foreground color for " + fillFGColor + " to " + c);
				if ( Message.isDebugOn ) {
					Message.printDebug(1,routine,"Setting style fill foreground color for " + fillFGColor + " to " +
						cs.getFillForegroundColor() + " fill pattern " + cs.getFillPattern());
				}
			}
			else {
				if ( Message.isDebugOn ) {
					Message.printDebug(1,routine,"No fill foreground for style \"" + styleID + "\"");
				}
			}
			key = "" + iColStyle + "-" + styleID.toUpperCase();
			this.cellStyleHash.put(key, cs);
			if ( Message.isDebugOn ) {
				Message.printDebug(1, routine, "Initialized cell style \"" + key + "\"");
			}
		}
	}
	this.cellStylesInitialized = true;
}

/**
Set the data format for a column.
@param col position (0+) in includeColumns array
@param format Excel format (e.g., "0" for integer)
*/
public void setColumnDataFormat(int col, String format)
{
	this.columnStyles[col].setDataFormat(this.columnDataFormats[col].getFormat(format));
}

}