package rti.tscommandprocessor.commands.spreadsheet;

import org.apache.poi.ss.usermodel.Workbook;

/**
This class stores metadata about Excel workbook files, in particular the name and how opened.
This is used to manage the list of open workbooks in the ExcelUtil class.
*/
public class WorkbookFileMetadata
{

/**
Name of the workbook file (generally full path).
*/
private String filename = "";

/**
Mode that the file was original opened (and managed), either "r" or "w".
*/
private String mode = "";

/**
Workbook instance that is being managed, as created with POI.
*/
private Workbook wb = null;

/**
Constructor with metadata.
*/
public WorkbookFileMetadata ( String filename, String mode, Workbook wb )
{
	this.filename = filename;
	this.mode = mode;
	this.wb = wb;
}

/**
Return the workbook filename.
*/
public String getFilename ()
{
	return this.filename;
}

/**
Return the mode that the workbook was opened.
*/
public String getMode ()
{
	return this.mode;
}

/**
Return the Workbook instance.
*/
public Workbook getWorkbook ()
{
	return this.wb;
}

}