// TSCommandFactory - This class instantiates Commands for time series processing.

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

// General imports.
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import RTi.Util.IO.Command;
import RTi.Util.IO.CommandFactory;
import RTi.Util.IO.UnknownCommand;
import RTi.Util.IO.UnknownCommandException;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

// Access database commands.
import rti.tscommandprocessor.commands.access.NewAccessDatabase_Command;

// Check time series commands (separate from testing).
import rti.tscommandprocessor.commands.check.CheckTimeSeriesStatistic_Command;
import rti.tscommandprocessor.commands.check.CheckTimeSeries_Command;
import rti.tscommandprocessor.commands.check.WriteCheckFile_Command;

// Datastore general commands.
import rti.tscommandprocessor.commands.datastore.CloseDataStore_Command;
import rti.tscommandprocessor.commands.datastore.CreateDataStoreDataDictionary_Command;
import rti.tscommandprocessor.commands.datastore.DeleteDataStoreTableRows_Command;
import rti.tscommandprocessor.commands.datastore.OpenDataStore_Command;
import rti.tscommandprocessor.commands.datastore.ReadTableFromDataStore_Command;
import rti.tscommandprocessor.commands.datastore.ReadTimeSeriesFromDataStore_Command;
import rti.tscommandprocessor.commands.datastore.RunSql_Command;
import rti.tscommandprocessor.commands.datastore.SetPropertyFromDataStore_Command;
import rti.tscommandprocessor.commands.datastore.WriteTableToDataStore_Command;
import rti.tscommandprocessor.commands.datastore.WriteTimeSeriesToDataStore_Command;

// Datastream commands.
import rti.tscommandprocessor.commands.datastream.WriteTimeSeriesToDataStream_Command;

// DateValue commands.
import rti.tscommandprocessor.commands.datevalue.ReadDateValue_Command;
import rti.tscommandprocessor.commands.datevalue.WriteDateValue_Command;

// Delft FEWS commands.
import rti.tscommandprocessor.commands.delftfews.ReadDelftFewsPiXml_Command;
import rti.tscommandprocessor.commands.delftfews.WriteDelftFewsPiXml_Command;

// Delimited time series file commands.
import rti.tscommandprocessor.commands.delimited.ReadDelimitedFile_Command;
import rti.tscommandprocessor.commands.delimited.WriteDelimitedFile_Command;

// Derby database commands.
import rti.tscommandprocessor.commands.derby.NewDerbyDatabase_Command;

// Email commands.
import rti.tscommandprocessor.commands.email.SendEmailMessage_Command;

// Ensemble commands.
import rti.tscommandprocessor.commands.ensemble.CopyEnsemble_Command;
import rti.tscommandprocessor.commands.ensemble.CreateEnsembleFromOneTimeSeries_Command;
import rti.tscommandprocessor.commands.ensemble.InsertTimeSeriesIntoEnsemble_Command;
import rti.tscommandprocessor.commands.ensemble.NewEnsemble_Command;
import rti.tscommandprocessor.commands.ensemble.NewStatisticEnsemble_Command;
import rti.tscommandprocessor.commands.ensemble.NewStatisticTimeSeriesFromEnsemble_Command;
import rti.tscommandprocessor.commands.ensemble.SetEnsembleProperty_Command;

// File handling commands.
import rti.tscommandprocessor.commands.check.CheckFile_Command;

// HEC-DSS datastore commands.
import rti.tscommandprocessor.commands.hecdss.ReadHecDss_Command;
import rti.tscommandprocessor.commands.hecdss.WriteHecDss_Command;

// HydroBase datastore commands.
import rti.tscommandprocessor.commands.hydrobase.FillUsingDiversionComments_Command;
import rti.tscommandprocessor.commands.hydrobase.OpenHydroBase_Command;
import rti.tscommandprocessor.commands.hydrobase.ReadHydroBase_Command;
import rti.tscommandprocessor.commands.json.FreeObject_Command;
import rti.tscommandprocessor.commands.json.NewObject_Command;

// HydroBase REST datastore commands.
import cdss.dmi.hydrobase.rest.commands.ReadColoradoHydroBaseRest_Command;

// HydroJSON commands.
import rti.tscommandprocessor.commands.hydrojson.WriteTimeSeriesToHydroJSON_Command;

// JSON object commands.
import rti.tscommandprocessor.commands.json.ReadTableFromJSON_Command;
import rti.tscommandprocessor.commands.json.SetObjectPropertiesFromTable_Command;
import rti.tscommandprocessor.commands.json.SetObjectProperty_Command;
import rti.tscommandprocessor.commands.json.SetPropertyFromObject_Command;
import rti.tscommandprocessor.commands.json.WriteObjectToJSON_Command;
import rti.tscommandprocessor.commands.json.WriteTimeSeriesToJson_Command;

// Logging commands.
import rti.tscommandprocessor.commands.logging.ConfigureLogging_Command;
import rti.tscommandprocessor.commands.logging.Message_Command;
import rti.tscommandprocessor.commands.logging.SetDebugLevel_Command;
import rti.tscommandprocessor.commands.logging.SetWarningLevel_Command;
import rti.tscommandprocessor.commands.logging.StartLog_Command;

// MODSIM commands.
import rti.tscommandprocessor.commands.modsim.ReadMODSIM_Command;

// Network commands.
import rti.tscommandprocessor.commands.network.AnalyzeNetworkPointFlow_Command;
import rti.tscommandprocessor.commands.network.CreateNetworkFromTable_Command;

// NRCS web service datastore commands
import rti.tscommandprocessor.commands.nrcs.awdb.ReadNrcsAwdb_Command;

// NWSRFS datastore commands.
import rti.tscommandprocessor.commands.nwsrfs.ReadNwsCard_Command;
import rti.tscommandprocessor.commands.nwsrfs.ReadNwsrfsEspTraceEnsemble_Command;
import rti.tscommandprocessor.commands.nwsrfs.ReadNwsrfsFS5Files_Command;
import rti.tscommandprocessor.commands.nwsrfs.SetPropertyFromNwsrfsAppDefault_Command;
import rti.tscommandprocessor.commands.nwsrfs.WriteNwsCard_Command;
import rti.tscommandprocessor.commands.nwsrfs.WriteNWSRFSESPTraceEnsemble_Command;

// PDF commands.
import rti.tscommandprocessor.commands.pdf.PDFMerge_Command;

// R (statistics) commands.
import rti.tscommandprocessor.commands.r.RunR_Command;

// RCC ACIS datastore commands.
import rti.tscommandprocessor.commands.rccacis.ReadRccAcis_Command;

// Reclamation HDB commands.
import rti.tscommandprocessor.commands.reclamationhdb.ReadReclamationHDB_Command;
import rti.tscommandprocessor.commands.reclamationhdb.WriteReclamationHDB_Command;

//Reclamation Pisces commands.
import rti.tscommandprocessor.commands.reclamationpisces.ReadReclamationPisces_Command;

// RiverWare commands.
import rti.tscommandprocessor.commands.riverware.ReadRiverWare_Command;
import rti.tscommandprocessor.commands.riverware.WriteRiverWare_Command;

// SHEF commands.
import rti.tscommandprocessor.commands.shef.WriteSHEF_Command;

// Spatial commands.
import rti.tscommandprocessor.commands.spatial.WriteTableToGeoJSON_Command;
import rti.tscommandprocessor.commands.spatial.WriteTableToKml_Command;
import rti.tscommandprocessor.commands.spatial.WriteTableToShapefile_Command;
import rti.tscommandprocessor.commands.spatial.WriteTimeSeriesToGeoJSON_Command;
import rti.tscommandprocessor.commands.spatial.WriteTimeSeriesToKml_Command;

// Spreadsheet (Excel) commands.
import rti.tscommandprocessor.commands.spreadsheet.CloseExcelWorkbook_Command;
import rti.tscommandprocessor.commands.spreadsheet.NewExcelWorkbook_Command;
import rti.tscommandprocessor.commands.spreadsheet.ReadExcelWorkbook_Command;
import rti.tscommandprocessor.commands.spreadsheet.ReadPropertiesFromExcel_Command;
import rti.tscommandprocessor.commands.spreadsheet.ReadTableCellsFromExcel_Command;
import rti.tscommandprocessor.commands.spreadsheet.ReadTableFromExcel_Command;
import rti.tscommandprocessor.commands.spreadsheet.SetExcelCell_Command;
import rti.tscommandprocessor.commands.spreadsheet.SetExcelWorksheetViewProperties_Command;
import rti.tscommandprocessor.commands.spreadsheet.WriteTableCellsToExcel_Command;
import rti.tscommandprocessor.commands.spreadsheet.WriteTableToExcel_Command;
import rti.tscommandprocessor.commands.spreadsheet.WriteTimeSeriesToExcelBlock_Command;
import rti.tscommandprocessor.commands.spreadsheet.WriteTimeSeriesToExcel_Command;

// SQLite commands.

import rti.tscommandprocessor.commands.sqlite.NewSQLiteDatabase_Command;

// StateCU commands.
import rti.tscommandprocessor.commands.statecu.ReadStateCU_Command;
import rti.tscommandprocessor.commands.statecu.ReadStateCUB_Command;
import rti.tscommandprocessor.commands.statecu.WriteStateCU_Command;

// StateMod commands.
import rti.tscommandprocessor.commands.statemod.ReadStateMod_Command;
import rti.tscommandprocessor.commands.statemod.ReadStateModB_Command;
import rti.tscommandprocessor.commands.statemod.StateModMax_Command;
import rti.tscommandprocessor.commands.statemod.WriteStateMod_Command;

// Summary commands.
import rti.tscommandprocessor.commands.summary.WriteSummary_Command;

// Table commands.
import rti.tscommandprocessor.commands.table.AppendTable_Command;
import rti.tscommandprocessor.commands.table.CompareTables_Command;
import rti.tscommandprocessor.commands.table.CopyPropertiesToTable_Command;
import rti.tscommandprocessor.commands.table.CopyTable_Command;
import rti.tscommandprocessor.commands.table.CopyTimeSeriesPropertiesToTable_Command;
import rti.tscommandprocessor.commands.table.CreateTimeSeriesEventTable_Command;
import rti.tscommandprocessor.commands.table.DeleteTableColumns_Command;
import rti.tscommandprocessor.commands.table.DeleteTableRows_Command;
import rti.tscommandprocessor.commands.table.FormatTableDateTime_Command;
import rti.tscommandprocessor.commands.table.FormatTableString_Command;
import rti.tscommandprocessor.commands.table.FreeTable_Command;
import rti.tscommandprocessor.commands.table.InsertTableColumn_Command;
import rti.tscommandprocessor.commands.table.InsertTableRow_Command;
import rti.tscommandprocessor.commands.table.JoinTables_Command;
import rti.tscommandprocessor.commands.table.ManipulateTableString_Command;
import rti.tscommandprocessor.commands.table.NewTable_Command;
import rti.tscommandprocessor.commands.table.ReadTableFromDBF_Command;
import rti.tscommandprocessor.commands.table.ReadTableFromDelimitedFile_Command;
import rti.tscommandprocessor.commands.table.ReadTableFromFixedFormatFile_Command;
import rti.tscommandprocessor.commands.table.ReadTableFromXML_Command;
import rti.tscommandprocessor.commands.table.RenameTableColumns_Command;
import rti.tscommandprocessor.commands.table.SetPropertyFromTable_Command;
import rti.tscommandprocessor.commands.table.SetTableColumnProperties_Command;
import rti.tscommandprocessor.commands.table.SetTableValues_Command;
import rti.tscommandprocessor.commands.table.SetTimeSeriesPropertiesFromTable_Command;
import rti.tscommandprocessor.commands.table.SortTable_Command;
import rti.tscommandprocessor.commands.table.SplitTableColumn_Command;
import rti.tscommandprocessor.commands.table.SplitTableRow_Command;
import rti.tscommandprocessor.commands.table.TableMath_Command;
import rti.tscommandprocessor.commands.table.TableTimeSeriesMath_Command;
import rti.tscommandprocessor.commands.table.TableToTimeSeries_Command;
import rti.tscommandprocessor.commands.table.TimeSeriesToTable_Command;
import rti.tscommandprocessor.commands.table.WriteTableToDelimitedFile_Command;
import rti.tscommandprocessor.commands.table.WriteTableToHTML_Command;
import rti.tscommandprocessor.commands.table.WriteTableToMarkdown_Command;

// Template commands.
import rti.tscommandprocessor.commands.template.ExpandTemplateFile_Command;

// Time-related commands.
import rti.tscommandprocessor.commands.time.SetAveragePeriod_Command;
import rti.tscommandprocessor.commands.time.SetInputPeriod_Command;
import rti.tscommandprocessor.commands.time.SetOutputPeriod_Command;
import rti.tscommandprocessor.commands.time.SetOutputYearType_Command;

// Time series general commands.
import rti.tscommandprocessor.commands.ts.Add_Command;
import rti.tscommandprocessor.commands.ts.AddConstant_Command;
import rti.tscommandprocessor.commands.ts.AdjustExtremes_Command;
import rti.tscommandprocessor.commands.ts.AnalyzePattern_Command;
import rti.tscommandprocessor.commands.ts.ARMA_Command;
import rti.tscommandprocessor.commands.ts.Blend_Command;
import rti.tscommandprocessor.commands.ts.CalculateTimeSeriesStatistic_Command;
import rti.tscommandprocessor.commands.ts.ChangeIntervalIrregularToRegular_Command;
import rti.tscommandprocessor.commands.ts.ChangeIntervalToLarger_Command;
import rti.tscommandprocessor.commands.ts.ChangeIntervalRegularToIrregular_Command;
import rti.tscommandprocessor.commands.ts.ChangeIntervalToSmaller_Command;
import rti.tscommandprocessor.commands.ts.ChangeInterval_Command;
import rti.tscommandprocessor.commands.ts.ChangePeriod_Command;
import rti.tscommandprocessor.commands.ts.ChangeTimeZone_Command;
import rti.tscommandprocessor.commands.ts.CompareTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.ComputeErrorTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.ConvertDataUnits_Command;
import rti.tscommandprocessor.commands.ts.Copy_Command;
import rti.tscommandprocessor.commands.ts.CreateFromList_Command;
import rti.tscommandprocessor.commands.ts.Cumulate_Command;
import rti.tscommandprocessor.commands.ts.DeselectTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.Disaggregate_Command;
import rti.tscommandprocessor.commands.ts.Divide_Command;
import rti.tscommandprocessor.commands.ts.FillConstant_Command;
import rti.tscommandprocessor.commands.ts.FillDayTSFrom2MonthTSAnd1DayTS_Command;
import rti.tscommandprocessor.commands.ts.FillFromTS_Command;
import rti.tscommandprocessor.commands.ts.FillHistMonthAverage_Command;
import rti.tscommandprocessor.commands.ts.FillHistYearAverage_Command;
import rti.tscommandprocessor.commands.ts.FillInterpolate_Command;
import rti.tscommandprocessor.commands.ts.FillMixedStation_Command;
import rti.tscommandprocessor.commands.ts.FillPrincipalComponentAnalysis_Command;
import rti.tscommandprocessor.commands.ts.FillMOVE2_Command;
import rti.tscommandprocessor.commands.ts.FillPattern_Command;
import rti.tscommandprocessor.commands.ts.FillProrate_Command;
import rti.tscommandprocessor.commands.ts.FillRegression_Command;
import rti.tscommandprocessor.commands.ts.FillRepeat_Command;
import rti.tscommandprocessor.commands.ts.Free_Command;
//import rti.tscommandprocessor.commands.ts.FreeEnsemble_Command;
import rti.tscommandprocessor.commands.ts.Delta_Command;
import rti.tscommandprocessor.commands.ts.LagK_Command;
import rti.tscommandprocessor.commands.ts.LookupTimeSeriesFromTable_Command;
import rti.tscommandprocessor.commands.ts.Multiply_Command;
import rti.tscommandprocessor.commands.ts.NewDayTSFromMonthAndDayTS_Command;
import rti.tscommandprocessor.commands.ts.NewEndOfMonthTSFromDayTS_Command;
import rti.tscommandprocessor.commands.ts.NewStatisticMonthTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.ReadTimeSeriesList_Command;
import rti.tscommandprocessor.commands.ts.RunningStatisticTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.NewStatisticTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.NewStatisticYearTS_Command;
import rti.tscommandprocessor.commands.ts.NewPatternTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.NewTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.Normalize_Command;
import rti.tscommandprocessor.commands.ts.ReadPatternFile_Command;
import rti.tscommandprocessor.commands.ts.ReadTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.RelativeDiff_Command;
import rti.tscommandprocessor.commands.ts.ReplaceValue_Command;
import rti.tscommandprocessor.commands.ts.ResequenceTimeSeriesData_Command;
import rti.tscommandprocessor.commands.ts.RunningAverage_Command;
import rti.tscommandprocessor.commands.ts.Scale_Command;
import rti.tscommandprocessor.commands.ts.SelectTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.SetAutoExtendPeriod_Command;
import rti.tscommandprocessor.commands.ts.SetConstant_Command;
import rti.tscommandprocessor.commands.ts.SetDataValue_Command;
import rti.tscommandprocessor.commands.ts.SetFromTS_Command;
import rti.tscommandprocessor.commands.ts.SetIncludeMissingTS_Command;
import rti.tscommandprocessor.commands.ts.SetIgnoreLEZero_Command;
import rti.tscommandprocessor.commands.ts.SetTimeSeriesProperty_Command;
import rti.tscommandprocessor.commands.ts.SetTimeSeriesValuesFromLookupTable_Command;
import rti.tscommandprocessor.commands.ts.SetTimeSeriesValuesFromTable_Command;
import rti.tscommandprocessor.commands.ts.SetToMax_Command;
import rti.tscommandprocessor.commands.ts.SetToMin_Command;
import rti.tscommandprocessor.commands.ts.ShiftTimeByInterval_Command;
import rti.tscommandprocessor.commands.ts.SortTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.Subtract_Command;
import rti.tscommandprocessor.commands.ts.TSID_Command;
import rti.tscommandprocessor.commands.ts.VariableLagK_Command;
import rti.tscommandprocessor.commands.ts.WeightTraces_Command;
import rti.tscommandprocessor.commands.ts.WriteTimeSeriesPropertiesToFile_Command;
import rti.tscommandprocessor.commands.ts.WriteTimeSeriesProperty_Command;

// USGS commands.
import rti.tscommandprocessor.commands.usgs.nwis.rdb.ReadUsgsNwisRdb_Command;
import rti.tscommandprocessor.commands.usgs.nwis.daily.ReadUsgsNwisDaily_Command;
import rti.tscommandprocessor.commands.usgs.nwis.groundwater.ReadUsgsNwisGroundwater_Command;
import rti.tscommandprocessor.commands.usgs.nwis.instantaneous.ReadUsgsNwisInstantaneous_Command;

// Utility commands.
import rti.tscommandprocessor.commands.util.AppendFile_Command;
import rti.tscommandprocessor.commands.util.Break_Command;
import rti.tscommandprocessor.commands.util.Comment_Command;
import rti.tscommandprocessor.commands.util.CommentBlockStart_Command;
import rti.tscommandprocessor.commands.util.CommentBlockEnd_Command;
import rti.tscommandprocessor.commands.util.CompareFiles_Command;
import rti.tscommandprocessor.commands.util.Continue_Command;
import rti.tscommandprocessor.commands.util.CopyFile_Command;
import rti.tscommandprocessor.commands.util.CreateFolder_Command;
import rti.tscommandprocessor.commands.util.CreateRegressionTestCommandFile_Command;
import rti.tscommandprocessor.commands.util.Empty_Command;
import rti.tscommandprocessor.commands.util.EndFor_Command;
import rti.tscommandprocessor.commands.util.EndIf_Command;
import rti.tscommandprocessor.commands.util.Exit_Command;
import rti.tscommandprocessor.commands.util.FTPGet_Command;
import rti.tscommandprocessor.commands.util.For_Command;
import rti.tscommandprocessor.commands.util.FormatDateTimeProperty_Command;
import rti.tscommandprocessor.commands.util.FormatFile_Command;
import rti.tscommandprocessor.commands.util.FormatStringProperty_Command;
import rti.tscommandprocessor.commands.util.If_Command;
import rti.tscommandprocessor.commands.util.ListFiles_Command;
import rti.tscommandprocessor.commands.util.MergeListFileColumns_Command;
import rti.tscommandprocessor.commands.util.PrintTextFile_Command;
import rti.tscommandprocessor.commands.util.ProfileCommands_Command;
import rti.tscommandprocessor.commands.util.ReadPropertiesFromFile_Command;
import rti.tscommandprocessor.commands.util.RemoveFile_Command;
import rti.tscommandprocessor.commands.util.RemoveFolder_Command;
import rti.tscommandprocessor.commands.util.RunCommands_Command;
import rti.tscommandprocessor.commands.util.RunDSSUTL_Command;
import rti.tscommandprocessor.commands.util.RunProgram_Command;
import rti.tscommandprocessor.commands.util.RunPython_Command;
import rti.tscommandprocessor.commands.util.SetPropertyFromEnsemble_Command;
import rti.tscommandprocessor.commands.util.SetPropertyFromTimeSeries_Command;
import rti.tscommandprocessor.commands.util.SetProperty_Command;
import rti.tscommandprocessor.commands.util.SetWorkingDir_Command;
import rti.tscommandprocessor.commands.util.StartRegressionTestResultsReport_Command;
import rti.tscommandprocessor.commands.util.TestCommand_Command;
import rti.tscommandprocessor.commands.util.TextEdit_Command;
import rti.tscommandprocessor.commands.util.UnzipFile_Command;
import rti.tscommandprocessor.commands.util.Wait_Command;
import rti.tscommandprocessor.commands.util.WebGet_Command;
import rti.tscommandprocessor.commands.util.WritePropertiesToFile_Command;
import rti.tscommandprocessor.commands.util.WriteProperty_Command;
import rti.tscommandprocessor.commands.view.NewTreeView_Command;

// Visualization commands (time series products).
import rti.tscommandprocessor.commands.products.ProcessRasterGraph_Command;
import rti.tscommandprocessor.commands.products.ProcessTSProduct_Command;

// WaterML datastore commands.
import rti.tscommandprocessor.commands.waterml.ReadWaterML_Command;
import rti.tscommandprocessor.commands.waterml.WriteWaterML_Command;
import rti.tscommandprocessor.commands.waterml2.ReadWaterML2_Command;
import rti.tscommandprocessor.commands.waterml2.WriteWaterML2_Command;

// WaterOneFlow datastore commands
import rti.tscommandprocessor.commands.wateroneflow.ws.ReadWaterOneFlow_Command;

/**
This class instantiates Commands for time series processing.
The full command name is required, but parameters are not because parsing does not occur.
*/
public class TSCommandFactory implements CommandFactory
{

/**
 * List of classes for plugin commands.
 * OK to be empty.
 */
@SuppressWarnings("rawtypes")
List<Class> pluginCommandClassList = new ArrayList<>();

/**
Constructor.
*/
public TSCommandFactory () {
    super();
}

/**
Constructor.
*/
@SuppressWarnings("rawtypes")
public TSCommandFactory ( List<Class> pluginCommandClassList ) {
    super();
    this.pluginCommandClassList = pluginCommandClassList;
}
	
/**
Return a new command, based on the command name.
DO NOT create an UnknownCommand if the command is not recognized.
@return a new command, based on the command name.
@param command_string The command string to process.
@throws UnknownCommandException if the command name is not recognized.
*/
public Command newCommand ( String command_string )
throws UnknownCommandException {
	return newCommand ( command_string, false );
}

/**
Return a new command, based on the command name.
@return a new command, based on the command name.
@param commandString The command string to process.
The command string can contain parameters but they are not parsed.
At a minimum, the command string needs to be of the form "CommandName()" or "TS Alias = CommandName()".
@param createUnknownCommandIfNotRecognized If true and the command is not recognized,
create an UnknownCommand instance that holds the command string.
This is useful for code that is being migrated to the full command class design.
@throws UnknownCommandException if the command name is not recognized
(and createUnknownCommandIfNotRecognized=false).
*/
public Command newCommand ( String commandString, boolean createUnknownCommandIfNotRecognized )
throws UnknownCommandException {
	commandString = commandString.trim(); // Full command string, with no surrounding white space.
	// Upper case is used for checks.
	String commandStringUpper = commandString.toUpperCase();
    String commandName = ""; // Command name without "(...parameters...)".
    String routine = getClass().getSimpleName() + ".newCommand";

	// Parse out arguments for TS alias = foo() commands to be able to handle nulls here.

	String token0 = StringUtil.getToken(commandString,"( =",StringUtil.DELIM_SKIP_BLANKS,0);

	if ( (token0 != null) && token0.equalsIgnoreCase( "TS") ) {
		// This allows aliases with spaces.
		commandName = StringUtil.getToken(commandString,"(=",StringUtil.DELIM_SKIP_BLANKS,1);
		if ( commandName == null ) {
		    commandName = "";
		}
		else {
			// Trim so that the indent does not impact the command lookup below.
		    commandName = commandName.trim();
		}
	}
	else {
	    // Get the potential command name, which is the text prior to the (.
	    // However, it could be that a TSID string contains () so if the command name
	    // is not matched below and it fits the TSID pattern, treat as a TSID.
	    int pos = commandString.indexOf("(");
	    if ( pos > 0 ) {
			// Trim so that the indent does not impact the command lookup below.
	        commandName = commandString.substring(0,pos).trim();
	    }
	    else {
	        // The command name is the entire string (e.g., new commands will not have the ()).
			// Trim so that the indent does not impact the command lookup below.
	        commandName = commandString.trim();
	    }
	}
	if ( Message.isDebugOn ) {
	    Message.printDebug(1,routine,"commandName=\"" + commandName + "\"" );
	}

	// Get the upper case command name:
	// - then comparisons below don't have to repeatedly use equalIgnoreCase on 'commandName'
	// - this speeds performance
	String commandNameUpper = commandName.toUpperCase();
	
	// The following checks for a match for specific command name and if so an appropriate command instance is created and returned.
	// If nothing is matched and the command string is a TSID, a TSID command instance will be returned.
	
	// Comment commands.
	
	if ( commandString.startsWith("#") ) {
        return new Comment_Command ();
    }
    else if ( commandString.startsWith("/*") ) {
        return new CommentBlockStart_Command ();
    }
    else if ( commandString.startsWith("*/") ) {
        return new CommentBlockEnd_Command ();
    }

	// "A" commands.

    // Put the following before "Add".
    else if ( commandNameUpper.equals("ADDCONSTANT") ) { // "AddConstant"
        return new AddConstant_Command ();
    }
    else if ( commandNameUpper.equals("ADD") ) { // "Add"
        return new Add_Command ();
    }
    else if ( commandNameUpper.equals("ADJUSTEXTREMES") ) { // "AdjustExtremes"
        return new AdjustExtremes_Command ();
    }
    else if ( commandNameUpper.equals("ANALYZENETWORKPOINTFLOW") ) { // "AnalyzeNetworkPointFlow"
        return new AnalyzeNetworkPointFlow_Command ();
    }
    else if ( commandNameUpper.equals("ANALYZEPATTERN") ) { // "AnalyzePattern"
		return new AnalyzePattern_Command ();
	}
    else if ( commandNameUpper.equals("APPENDFILE") ) { // "AppendFile"
        return new AppendFile_Command ();
    }
    else if ( commandNameUpper.equals("APPENDTABLE") ) { // "AppendTable"
        return new AppendTable_Command ();
    }
    else if ( commandNameUpper.equals("ARMA") ) { // "ARMA"
        return new ARMA_Command ();
    }

    // "B" commands.

    else if ( commandNameUpper.equals("BLEND") ) { // "Blend"
        return new Blend_Command ();
    }
    else if ( commandNameUpper.equals("BREAK") ) { // "Break"
        return new Break_Command ();
    }

	// "C" commands.

    else if ( commandNameUpper.equals("CALCULATETIMESERIESSTATISTIC") ) { // "CalculateTimeSeriesStatistic"
        return new CalculateTimeSeriesStatistic_Command ();
    }
	else if ( commandNameUpper.equals("CHANGEINTERVAL") ) { // "ChangeInterval"
		return new ChangeInterval_Command ();
	}
	else if ( commandNameUpper.equals("CHANGEINTERVALIRREGULARTOREGULAR") ) { // "ChangeIntervalIrregularToRegular"
		return new ChangeIntervalIrregularToRegular_Command ();
	}
	else if ( commandNameUpper.equals("CHANGEINTERVALREGULARTOIRREGULAR") ) { // "ChangeIntervalRegularToIrregular"
		return new ChangeIntervalRegularToIrregular_Command ();
	}
	else if ( commandNameUpper.equals("CHANGEINTERVALTOLARGER") ) { // "ChangeIntervalToLarger"
		return new ChangeIntervalToLarger_Command ();
	}
	else if ( commandNameUpper.equals("CHANGEINTERVALTOSMALLER") ) { // "ChangeIntervalToSmaller"
		return new ChangeIntervalToSmaller_Command ();
	}
    else if ( commandNameUpper.equals("CHANGEPERIOD") ) { // "ChangePeriod"
        return new ChangePeriod_Command ();
    }
    else if ( commandNameUpper.equals("CHECKFILE") ) { // "CheckFile"
        return new CheckFile_Command ();
    }
    else if ( commandNameUpper.equals("CHANGETIMEZONE") ) { // "ChangeTimeZone"
        return new ChangeTimeZone_Command ();
    }
    else if ( commandNameUpper.equals("CHECKTIMESERIES") ) { // "CheckTimeSeries"
        return new CheckTimeSeries_Command ();
    }
    else if ( commandNameUpper.equals("CHECKTIMESERIESSTATISTIC") ) { // "CheckTimeSeriesStatistic"
        return new CheckTimeSeriesStatistic_Command ();
    }
	else if ( commandNameUpper.equals("CLOSEDATASTORE") ) { // "CloseDataStore"
		return new CloseDataStore_Command ();
	}
	else if ( commandNameUpper.equals("CLOSEEXCELWORKBOOK") ) { // "CloseExcelWorkbook"
		return new CloseExcelWorkbook_Command ();
	}
	else if ( commandNameUpper.equals("COMPAREFILES") ) { // "CompareFiles"
		return new CompareFiles_Command ();
	}
    else if ( commandNameUpper.equals("COMPARETABLES") ) { // "CompareTables"
        return new CompareTables_Command ();
    }
	else if ( commandNameUpper.equals("COMPARETIMESERIES") ) { // "CompareTimeSeries"
		return new CompareTimeSeries_Command ();
	}
    else if ( commandNameUpper.equals("COMPUTEERRORTIMESERIES") ) { // "ComputeErrorTimeSeries"
        return new ComputeErrorTimeSeries_Command ();
    }
    else if ( commandNameUpper.equals("CONFIGURELOGGING") ) { // "ConfigureLogging"
        return new ConfigureLogging_Command ();
    }
    else if ( commandNameUpper.equals("CONTINUE") ) { // "Continue"
        return new Continue_Command ();
    }
    else if ( commandNameUpper.equals("CONVERTDATAUNITS") ) { // "ConvertDataUnits"
        return new ConvertDataUnits_Command ();
    }
	else if ( commandNameUpper.equals("COPY") ) { // "Copy"
		return new Copy_Command ();
	}
    else if ( commandNameUpper.equals("COPYENSEMBLE") ) { // "CopyEnsemble"
        return new CopyEnsemble_Command ();
    }
    else if ( commandNameUpper.equals("COPYFILE") ) { // "CopyFile"
        return new CopyFile_Command ();
    }
    else if ( commandNameUpper.equals("COPYPROPERTIESTOTABLE") ) { // "CopyPropertiesToTable"
        return new CopyPropertiesToTable_Command ();
    }
    else if ( commandNameUpper.equals("COPYTABLE") ) { // "CopyTable"
        return new CopyTable_Command ();
    }
    else if ( commandNameUpper.equals("COPYTIMESERIESPROPERTIESTOTABLE") ) { // "CopyTimeSeriesPropertiesToTable"
        return new CopyTimeSeriesPropertiesToTable_Command ();
    }
    else if ( commandNameUpper.equals("CREATEDATASTOREDATADICTIONARY") ) { // "CreateDataStoreDataDictionary"
        return new CreateDataStoreDataDictionary_Command ();
    }
    else if ( commandNameUpper.equals("CREATEENSEMBLEFROMONETIMESERIES") || // "CreateEnsembleFromOneTimeSeries"
        commandNameUpper.equals("CREATEENSEMBLE") ) { // "CreateEnsemble"
        // The command name changed.
        return new CreateEnsembleFromOneTimeSeries_Command ();
    }
    else if ( commandNameUpper.equals("CREATEFOLDER") ) { // "CreateFolder"
        return new CreateFolder_Command ();
    }
    else if ( commandNameUpper.equals("CREATEFROMLIST") ) { // "CreateFromList"
        return new CreateFromList_Command ();
    }
    else if ( commandNameUpper.equals("CREATENETWORKFROMTABLE") ) { // "CreateNetworkFromTable"
        return new CreateNetworkFromTable_Command ();
    }
    else if ( commandNameUpper.equals("CREATETIMESERIESEVENTTABLE") ) { // "CreateTimeSeriesEventTable"
        return new CreateTimeSeriesEventTable_Command ();
    }
	else if ( commandNameUpper.equals("CREATEREGRESSIONTESTCOMMANDFILE") ) { // "CreateRegressionTestCommandFile"
		return new CreateRegressionTestCommandFile_Command ();
	}
	else if ( commandNameUpper.equals("CUMULATE") ) { // "Cumulate"
		return new Cumulate_Command ();
	}

    // "D" commands.

    else if ( commandNameUpper.equals("DELETEDATASTORETABLEROWS") ) { // "DeleteDataStoreTableRows"
        return new DeleteDataStoreTableRows_Command ();
    }
    else if ( commandNameUpper.equals("DELETETABLECOLUMNS") ) { // "DeleteTableColumns"
        return new DeleteTableColumns_Command ();
    }
    else if ( commandNameUpper.equals("DELETETABLEROWS") ) { // "DeleteTableRows"
        return new DeleteTableRows_Command ();
    }
    else if ( commandNameUpper.equals("DELTA") ) { // "Delta"
        return new Delta_Command ();
    }
	else if ( commandNameUpper.equals("DESELECTTIMESERIES") ) { // "DeselectTimeSeries"
        return new DeselectTimeSeries_Command ();
    }
    else if ( commandNameUpper.equals("DISAGGREGATE") ) { // "Disaggregate"
        return new Disaggregate_Command ();
    }
    else if ( commandNameUpper.equals("DIVIDE") ) { // "Divide"
        return new Divide_Command ();
    }

	// "E" commands.

    else if ( commandNameUpper.equals("ENDFOR") ) { // "EndFor"
        return new EndFor_Command ();
    }
    else if ( commandNameUpper.equals("ENDIF") ) { // "EndIf"
        return new EndIf_Command ();
    }
	else if ( commandStringUpper.startsWith("EXIT") ) {
		return new Exit_Command ();
	}
    else if ( commandNameUpper.equals("EXPANDTEMPLATEFILE") ) { // "ExpandTemplateFile"
        return new ExpandTemplateFile_Command ();
    }

	// "F" commands.

	else if ( commandNameUpper.equals("FILLCONSTANT") ) { // "FillConstant"
		return new FillConstant_Command ();
	}
    else if ( commandNameUpper.equals("FILLDAYTSFROM2MONTHTSAND1DAYTS") ) { // "FillDayTSFrom2MonthTSAnd1DayTS"
        return new FillDayTSFrom2MonthTSAnd1DayTS_Command ();
    }
    else if ( commandNameUpper.equals("FILLFROMTS") ) { // "FillFromTS"
        return new FillFromTS_Command ();
    }
	else if ( commandNameUpper.equals("FILLHISTMONTHAVERAGE") ) { // "FillHistMonthAverage"
		return new FillHistMonthAverage_Command ();
	}
	else if ( commandNameUpper.equals("FILLHISTYEARAVERAGE") ) { // "FillHistYearAverage"
		return new FillHistYearAverage_Command ();
	}
    else if ( commandNameUpper.equals("FILLINTERPOLATE") ) { // "FillInterpolate"
        return new FillInterpolate_Command ();
    }
	else if ( commandNameUpper.equals("FILLMIXEDSTATION") ) { // "FillMixedStation"
		return new FillMixedStation_Command ();
	}
	else if ( commandNameUpper.equals("FILLMOVE2") ) { // "FillMOVE2"
		return new FillMOVE2_Command ();
	}
    else if ( commandNameUpper.equals("FILLPATTERN") ) { // "FillPattern"
        return new FillPattern_Command ();
    }
	else if ( commandNameUpper.equals("FILLPRINCIPALCOMPONENTANALYSIS") ) { // "FillPrincipalComponentAnalysis"
		return new FillPrincipalComponentAnalysis_Command ();
	}
    else if ( commandNameUpper.equals("FILLPRORATE") ) { // "FillProrate"
        return new FillProrate_Command ();
    }
	else if ( commandNameUpper.equals("FILLREGRESSION") ) { // "FillRegression"
		return new FillRegression_Command ();
	}
    else if ( commandNameUpper.equals("FILLREPEAT") ) { // "FillRepeat"
        return new FillRepeat_Command ();
    }
	else if ( commandNameUpper.equals("FILLUSINGDIVERSIONCOMMENTS") ) { // "FillUsingDiversionComments"
		return new FillUsingDiversionComments_Command ();
	}
    else if ( commandNameUpper.equals("FOR") ) { // "For"
        return new For_Command ();
    }
    else if ( commandNameUpper.equals("FORMATDATETIMEPROPERTY") ) { // "FormatDateTimeProperty"
        return new FormatDateTimeProperty_Command ();
    }
    else if ( commandNameUpper.equals("FORMATFILE") ) { // "FormatFile"
        return new FormatFile_Command ();
    }
    else if ( commandNameUpper.equals("FORMATSTRINGPROPERTY") ) { // "FormatStringProperty"
        return new FormatStringProperty_Command ();
    }
    else if ( commandNameUpper.equals("FORMATTABLEDATETIME") ) { // "FormatTableDateTime"
        return new FormatTableDateTime_Command ();
    }
    else if ( commandNameUpper.equals("FORMATTABLESTRING") ) { // "FormatTableString"
        return new FormatTableString_Command ();
    }
    else if ( commandNameUpper.equals("FREE") ) { // "Free"
        return new Free_Command ();
    }
    else if ( commandNameUpper.equals("FREEOBJECT") ) { // "FreeObject"
        return new FreeObject_Command ();
    }
    else if ( commandNameUpper.equals("FREETABLE") ) { // "FreeTable"
        return new FreeTable_Command ();
    }
    else if ( commandNameUpper.equals("FTPGET") ) { // "FTPGet"
        return new FTPGet_Command ();
    }
    /*
    else if ( commandNameUpper.equals("FREEENSEMBLE) ) { // "FreeEnsemble"
        return new FreeEnsemble_Command ();
    }
    */

    // "I" commands.

    else if ( commandNameUpper.equals("IF") ) { // "If"
        return new If_Command ();
    }
    else if ( commandNameUpper.equals("INSERTTABLECOLUMN") ) { // "InsertTableColumn"
        return new InsertTableColumn_Command ();
    }
    else if ( commandNameUpper.equals("INSERTTABLEROW") ) { // "InsertTableRow"
        return new InsertTableRow_Command ();
    }
    else if ( commandNameUpper.equals("INSERTTIMESERIESINTOENSEMBLE") ) { // "InsertTimeSeriesIntoEnsemble"
        return new InsertTimeSeriesIntoEnsemble_Command ();
    }

    // "J" commands.

    else if ( commandNameUpper.equals("JOINTABLES") ) { // "JoinTables"
        return new JoinTables_Command ();
    }
	
	// "L" commands.

	else if ( commandNameUpper.equals("LAGK") ) { // "LagK"
		return new LagK_Command ();
	}
    else if ( commandNameUpper.equals("LISTFILES") ) { // "ListFiles"
        return new ListFiles_Command ();
    }
    else if ( commandNameUpper.equals("LOOKUPTIMESERIESFROMTABLE") ) { // "LookupTimeSeriesFromTable"
        return new LookupTimeSeriesFromTable_Command ();
    }
	
	// "M" commands.
	
    else if ( commandNameUpper.equals("MANIPULATETABLESTRING") ) { // "ManipulateTableString"
        return new ManipulateTableString_Command ();
    }
	else if ( commandNameUpper.equals("MERGELISTFILECOLUMNS") ) { // "MergeListFileColumns"
		return new MergeListFileColumns_Command ();
	}
    else if ( commandNameUpper.equals("MESSAGE") ) { // "Message"
        return new Message_Command ();
    }
    else if ( commandNameUpper.equals("MULTIPLY") ) { // "Multiply"
        return new Multiply_Command ();
    }

	// "N" commands.

    else if ( commandNameUpper.equals("NEWACCESSDATABASE") ) { // "NewAccessDatabase"
        return new NewAccessDatabase_Command ();
    }
    else if ( commandNameUpper.equals("NEWDAYTSFROMMONTHANDDAYTS") ) { // "NewDayTSFromMonthAndDayTS"
        return new NewDayTSFromMonthAndDayTS_Command ();
    }
    else if ( commandNameUpper.equals("NEWDERBYDATABASE") ) { // "NewDerbyDatabase"
        return new NewDerbyDatabase_Command ();
    }
    else if ( commandNameUpper.equals("NEWENDOFMONTHTSFROMDAYTS") ) { // "NewEndOfMonthTSFromDayTS"
        return new NewEndOfMonthTSFromDayTS_Command ();
    }
    else if ( commandNameUpper.equals("NEWENSEMBLE") ) { // "NewEnsemble"
        return new NewEnsemble_Command ();
    }
    else if ( commandNameUpper.equals("NEWEXCELWORKBOOK") ) { // "NewExcelWorkbook"
        return new NewExcelWorkbook_Command ();
    }
    else if ( commandNameUpper.equals("NEWOBJECT") ) { // "NewObject"
        return new NewObject_Command ();
    }
    else if ( commandNameUpper.equals("NEWPATTERNTIMESERIES") ) { // "NewPatternTimeSeries"
        return new NewPatternTimeSeries_Command ();
    }
    else if ( commandNameUpper.equals("NEWSQLITEDATABASE") ) { // "NewSQLiteDatabase"
        return new NewSQLiteDatabase_Command ();
    }
    else if ( commandNameUpper.equals("NEWSTATISTICENSEMBLE") ) { // "NewStatisticEnsemble"
        return new NewStatisticEnsemble_Command ();
    }
    else if ( commandNameUpper.equals("NEWSTATISTICMONTHTIMESERIES") ) { // "NewStatisticMonthTimeSeries"
        return new NewStatisticMonthTimeSeries_Command ();
    }
    else if ( commandNameUpper.equals("NEWSTATISTICTIMESERIESFROMENSEMBLE") ) { // "NewStatisticTimeSeriesFromEnsemble"
        return new NewStatisticTimeSeriesFromEnsemble_Command ();
    }
	else if ( commandNameUpper.equals("NEWSTATISTICTIMESERIES") ) { // "NewStatisticTimeSeries"
		return new NewStatisticTimeSeries_Command ();
	}
	else if ( commandNameUpper.equals("NEWSTATISTICYEARTS") ) { // "NewStatisticYearTS"
		return new NewStatisticYearTS_Command ();
	}
    else if ( commandNameUpper.equals("NEWTABLE") ) { // "NewTable"
        return new NewTable_Command ();
    }
	else if ( commandNameUpper.equals("NEWTIMESERIES") ) { // "NewTimeSeries"
		return new NewTimeSeries_Command ();
	}
    else if ( commandNameUpper.equals("NEWTREEVIEW") ) { // "NewTreeView"
        return new NewTreeView_Command ();
    }
    else if ( commandNameUpper.equals("NORMALIZE") ) { // "Normalize"
        return new Normalize_Command ();
    }

	// "O" commands.

	else if ( commandNameUpper.equals("OPENDATASTORE") ) { // "OpenDataStore"
		return new OpenDataStore_Command ();
	}
	else if ( commandNameUpper.equals("OPENHYDROBASE") ) { // "OpenHydroBase"
		return new OpenHydroBase_Command ();
	}

	// "P" commands.

    else if ( commandNameUpper.equals("PDFMERGE") ) { // "PDFMerge"
        return new PDFMerge_Command ();
    }
    else if ( commandNameUpper.equals("PRINTTEXTFILE") ) { // "PrintTextFile"
        return new PrintTextFile_Command ();
    }
    else if ( commandNameUpper.equals("PROCESSRASTERGRAPH") ) { // "ProcessRasterGraph"
        return new ProcessRasterGraph_Command ();
    }
	else if ( commandNameUpper.equals("PROCESSTSPRODUCT") ) { // "ProcessTSProduct"
		return new ProcessTSProduct_Command ();
	}
    else if ( commandNameUpper.equals("PROFILECOMMANDS") ) { // "ProfileCommands"
        return new ProfileCommands_Command ();
    }

	// "R" commands.

	else if ( commandNameUpper.equals("READCOLORADOHYDROBASEREST") ) { // "ReadColoradoHydroBaseRest"
		return new ReadColoradoHydroBaseRest_Command ();
	}
    else if ( commandNameUpper.equals("READDATEVALUE") ) { // "ReadDateValue"
        return new ReadDateValue_Command ();
    }
    else if ( commandNameUpper.equals("READDELFTFEWSPIXML") ) { // "ReadDelftFewsPiXml"
        return new ReadDelftFewsPiXml_Command ();
    }
    else if ( commandNameUpper.equals("READDELIMITEDFILE") ) { // "ReadDelimitedFile"
        return new ReadDelimitedFile_Command ();
    }
    else if ( commandNameUpper.equals("READEXCELWORKBOOK") ) { // "ReadExcelWorkbook"
        return new ReadExcelWorkbook_Command ();
    }
    else if ( commandNameUpper.equals("READHECDSS") ) { // "ReadHecDss"
        return new ReadHecDss_Command ();
    }
	else if ( commandNameUpper.equals("READHYDROBASE") ) { // "ReadHydroBase"
		return new ReadHydroBase_Command ();
	}
    else if ( commandNameUpper.equals("READMODSIM") ) { // "ReadMODSIM"
        return new ReadMODSIM_Command ();
    }
	else if ( commandNameUpper.equals("READNDFD") ) { // "ReadNDFD"
		//return new readNDFD_Command ();
	}
	else if ( commandNameUpper.equals("READNRCSAWDB") ) { // "ReadNrcsAwdb"
		return new ReadNrcsAwdb_Command ();
	}
    else if ( commandNameUpper.equals("READNWSCARD") ) { // "ReadNwsCard"
        return new ReadNwsCard_Command ();
    }
    else if ( commandNameUpper.equals("READNWSRFSFS5FILES") ) { // "ReadNwsrfsFS5Files"
        return new ReadNwsrfsFS5Files_Command();
    }
    else if ( commandNameUpper.equals("READNWSRFSESPTRACEENSEMBLE") ) { // "ReadNwsrfsEspTraceEnsemble"
        return new ReadNwsrfsEspTraceEnsemble_Command ();
    }
    else if ( commandNameUpper.equals("READPATTERNFILE") ) { // "ReadPatternFile"
        return new ReadPatternFile_Command ();
    }
    else if ( commandNameUpper.equals("READPROPERTIESFROMEXCEL") ) { // "ReadPropertiesFromExcel"
        return new ReadPropertiesFromExcel_Command ();
    }
    else if ( commandNameUpper.equals("READPROPERTIESFROMFILE") ) { // "ReadPropertiesFromFile"
        return new ReadPropertiesFromFile_Command ();
    }
    else if ( commandNameUpper.equals("READRCCACIS") ) { // "ReadRccAcis"
        return new ReadRccAcis_Command ();
    }
    else if ( commandNameUpper.equals("READRECLAMATIONHDB") ) { // "ReadReclamationHDB"
        return new ReadReclamationHDB_Command ();
    }
    else if ( commandNameUpper.equals("READRECLAMATIONPISCES") ) { // "ReadReclamationPisces"
        return new ReadReclamationPisces_Command ();
    }
    else if ( commandNameUpper.equals("READRIVERWARE") ) { // "ReadRiverWare"
        return new ReadRiverWare_Command ();
    }
	else if ( commandNameUpper.equals("READSTATECU") ) { // "ReadStateCU"
		return new ReadStateCU_Command ();
	}
    else if ( commandNameUpper.equals("READSTATECUB") ) { // "ReadStateCUB"
        return new ReadStateCUB_Command ();
    }
	else if ( commandNameUpper.equals("READSTATEMOD") ) { // "ReadStateMod"
		return new ReadStateMod_Command ();
	}
    else if ( commandNameUpper.equals("READSTATEMODB") ) { // "ReadStateModB"
        return new ReadStateModB_Command ();
    }
    else if ( commandNameUpper.equals("READTABLECELLSFROMEXCEL") ) { // "ReadTableCellsFromExcel"
        return new ReadTableCellsFromExcel_Command ();
    }
    else if ( commandNameUpper.equals("READTABLEFROMDATASTORE") ) { // "ReadTableFromDataStore"
        return new ReadTableFromDataStore_Command ();
    }
    else if ( commandNameUpper.equals("READTABLEFROMDBF") ) { // "ReadTableFromDBF"
        return new ReadTableFromDBF_Command ();
    }
    else if ( commandNameUpper.equals("READTABLEFROMDELIMITEDFILE") ) { // "ReadTableFromDelimitedFile"
        return new ReadTableFromDelimitedFile_Command ();
    }
    else if ( commandNameUpper.equals("READTABLEFROMEXCEL") ) { // "ReadTableFromExcel"
        return new ReadTableFromExcel_Command ();
    }
    else if ( commandNameUpper.equals("READTABLEFROMFIXEDFORMATFILE") ) { // "ReadTableFromFixedFormatFile"
        return new ReadTableFromFixedFormatFile_Command ();
    }
    else if ( commandNameUpper.equals("READTABLEFROMJSON") ) { // "ReadTableFromJSON"
        return new ReadTableFromJSON_Command ();
    }
    else if ( commandNameUpper.equals("READTABLEFROMXML") ) { // "ReadTableFromXML"
        return new ReadTableFromXML_Command ();
    }
    else if ( commandNameUpper.equals("READTIMESERIES") ) { // "ReadTimeSeries"
        return new ReadTimeSeries_Command ();
    }
    else if ( commandNameUpper.equals("READTIMESERIESFROMDATASTORE") ) { // "ReadTimeSeriesFromDataStore"
        return new ReadTimeSeriesFromDataStore_Command ();
    }
    else if ( commandNameUpper.equals("READTIMESERIESLIST") ) { // "ReadTimeSeriesList"
        return new ReadTimeSeriesList_Command ();
    }
    else if ( commandNameUpper.equals("READUSGSNWIS") // "ReadUsgsNwis"
    	|| commandNameUpper.equals("READUSGSNWISRDB") ) { // "ReadUsgsNwisRdb"
        // Automatically convert legacy command to new name.
        return new ReadUsgsNwisRdb_Command ();
    }
    else if ( commandNameUpper.equals("READUSGSNWISDAILY") ) { // "ReadUsgsNwisDaily"
        return new ReadUsgsNwisDaily_Command ();
    }
    else if ( commandNameUpper.equals("READUSGSNWISGROUNDWATER") ) { // "ReadUsgsNwisGroundwater"
        return new ReadUsgsNwisGroundwater_Command ();
    }
    else if ( commandNameUpper.equals("READUSGSNWISINSTANTANEOUS") ) { // "ReadUsgsNwisInstantaneous"
        return new ReadUsgsNwisInstantaneous_Command ();
    }
    else if ( commandNameUpper.equals("READWATERML") ) { // "ReadWaterML"
        return new ReadWaterML_Command ();
    }
    else if ( commandNameUpper.equals("READWATERML2") ) { // "ReadWaterML2"
        return new ReadWaterML2_Command ();
    }
    else if ( commandNameUpper.equals("READWATERONEFLOW") ) { // "ReadWaterOneFlow"
        return new ReadWaterOneFlow_Command ();
    }
    else if ( commandNameUpper.equals("RELATIVEDIFF") ) { // "RelativeDiff"
        return new RelativeDiff_Command ();
    }
    else if ( commandNameUpper.equals("REMOVEDATASTORETABLEROWS") || // "RemoveDataStoreTableRows"
   		commandNameUpper.equals("DELETEDATASTORETABLEROWS") ) { // "DeleteDataStoreTableRows"
        // Automatically change the name.
        return new DeleteDataStoreTableRows_Command ();
    }
    else if ( commandNameUpper.equals("REMOVEFILE") ) { // "RemoveFile"
        return new RemoveFile_Command ();
    }
    else if ( commandNameUpper.equals("REMOVEFOLDER") ) { // "RemoveFolder"
        return new RemoveFolder_Command ();
    }
    else if ( commandNameUpper.equals("REMOVETABLEROWSFROMDATASTORE") ) { // "RemoveTableRowsFromDataStore"
        // Automatically change the name.
        return new DeleteDataStoreTableRows_Command ();
    }
    else if ( commandNameUpper.equals("RENAMETABLECOLUMNS") ) { // "RenameTableColumns"
        return new RenameTableColumns_Command ();
    }
    else if ( commandNameUpper.equals("REPLACEVALUE") ) { // "ReplaceValue"
        return new ReplaceValue_Command ();
    }
    else if ( commandNameUpper.equals("RESEQUENCETIMESERIESDATA") ) { // "ResequenceTimeSeriesData"
        return new ResequenceTimeSeriesData_Command ();
    }
	else if ( commandNameUpper.equals("RUNCOMMANDS") ) { // "RunCommands"
		return new RunCommands_Command ();
	}
    else if ( commandNameUpper.equals("RUNDSSUTL") ) { // "RunDSSUTL"
        return new RunDSSUTL_Command ();
    }
    else if ( commandNameUpper.equals("RUNNINGAVERAGE") ) { // "RunningAverage"
        return new RunningAverage_Command ();
    }
    else if ( commandNameUpper.equals("RUNNINGSTATISTICTIMESERIES") ) { // "RunningStatisticTimeSeries"
        return new RunningStatisticTimeSeries_Command ();
    }
    else if ( commandNameUpper.equals("RUNPROGRAM") ) { // "RunProgram"
        return new RunProgram_Command ();
    }
    else if ( commandNameUpper.equals("RUNPYTHON") ) { // "RunPython"
        return new RunPython_Command ();
    }
    else if ( commandNameUpper.equals("RUNR") ) { // "RunR"
        return new RunR_Command ();
    }
    else if ( commandNameUpper.equals("RUNSQL") ) { // "RunSql"
        return new RunSql_Command ();
    }

	// "S" commands.

	else if ( commandNameUpper.equals("SCALE") ) { // "Scale"
		return new Scale_Command ();
	}
    else if ( commandNameUpper.equals("SELECTTIMESERIES") ) { // "SelectTimeSeries"
        return new SelectTimeSeries_Command ();
    }
    else if ( commandNameUpper.equals("SENDEMAILMESSAGE") ) { // "SendEmailMessage"
        return new SendEmailMessage_Command ();
    }
    else if ( commandNameUpper.equals("SETAUTOEXTENDPERIOD") ) { // "SetAutoExtendPeriod"
        return new SetAutoExtendPeriod_Command ();
    }
    else if ( commandNameUpper.equals("SETAVERAGEPERIOD") ) { // "SetAveragePeriod"
        return new SetAveragePeriod_Command ();
    }
    // Obsolete SetConst() and SetConstantBefore() will be handled as an unknown command.
    else if ( commandNameUpper.equals("SETCONSTANT") ) { // "SetConstant"
        return new SetConstant_Command ();
    }
    else if ( commandNameUpper.equals("SETDATAVALUE") ) { // "SetDataValue"
        return new SetDataValue_Command ();
    }
    else if ( commandNameUpper.equals("SETDEBUGLEVEL") ) { // "SetDebugLevel"
        return new SetDebugLevel_Command ();
    }
    else if ( commandNameUpper.equals("SETENSEMBLEPROPERTY") ) { // "SetEnsembleProperty"
        return new SetEnsembleProperty_Command ();
    }
    else if ( commandNameUpper.equals("SETEXCELCELL") ) { // "SetExcelCell"
        return new SetExcelCell_Command ();
    }
    else if ( commandNameUpper.equals("SETEXCELWORKSHEETVIEWPROPERTIES") ) { // "SetExcelWorksheetViewProperties"
        return new SetExcelWorksheetViewProperties_Command ();
    }
    else if ( commandNameUpper.equals("SETFROMTS") ) { // "SetFromTS"
        return new SetFromTS_Command ();
    }
    else if ( commandNameUpper.equals("SETIGNORELEZERO") ) { // "SetIgnoreLEZero"
        return new SetIgnoreLEZero_Command ();
    }
    else if ( commandNameUpper.equals("SETINCLUDEMISSINGTS") ) { // "SetIncludeMissingTS"
        return new SetIncludeMissingTS_Command ();
    }
	else if ( commandNameUpper.equals("SETINPUTPERIOD") ) { // "SetInputPeriod"
		return new SetInputPeriod_Command ();
	}
	else if ( commandNameUpper.equals("SETOBJECTPROPERTY") ) { // "SetObjectProperty"
		return new SetObjectProperty_Command ();
	}
	else if ( commandNameUpper.equals("SETOBJECTPROPERTIESFROMTABLE") ) { // "SetObjectPropertiesFromTable"
		return new SetObjectPropertiesFromTable_Command ();
	}
	else if ( commandNameUpper.equals("SETOUTPUTPERIOD") ) { // "SetOutputPeriod"
		return new SetOutputPeriod_Command ();
	}
    else if ( commandNameUpper.equals("SETOUTPUTYEARTYPE") ) { // "SetOutputYearType"
        return new SetOutputYearType_Command ();
    }
    else if ( commandNameUpper.equals("SETPATTERNFILE") ) { // "SetPatternFile"
        // Automatically convert to ReadPatternFile.
        return new ReadPatternFile_Command ();
    }
    // Put this before the shorter SetProperty() to avoid ambiguity.
    else if ( commandNameUpper.equals("SETPROPERTYFROMDATASTORE") ) { // "SetPropertyFromDataStore"
        return new SetPropertyFromDataStore_Command ();
    }
    else if ( commandNameUpper.equals("SETPROPERTYFROMENSEMBLE") ) { // "SetPropertyFromEnsemble"
        return new SetPropertyFromEnsemble_Command ();
    }
    else if ( commandNameUpper.equals("SETPROPERTYFROMNWSRFSAPPDEFAULT") ) { // "SetPropertyFromNwsrfsAppDefault"
        return new SetPropertyFromNwsrfsAppDefault_Command ();
    }
    // Put this before the shorter SetProperty() to avoid ambiguity.
    else if ( commandNameUpper.equals("SETPROPERTYFROMTABLE") ) { // "SetPropertyFromTable"
        return new SetPropertyFromTable_Command ();
    }
    else if ( commandNameUpper.equals("SETPROPERTYFROMOBJECT") ) { // "SetPropertyFromObject"
        return new SetPropertyFromObject_Command ();
    }
    else if ( commandNameUpper.equals("SETPROPERTYFROMTIMESERIES") ) { // "SetPropertyFromTimeSeries"
        return new SetPropertyFromTimeSeries_Command ();
    }
    else if ( commandNameUpper.equals("SETPROPERTY") ) { // "SetProperty"
        return new SetProperty_Command ();
    }
	else if ( commandNameUpper.equals("SETQUERYPERIOD") ) { // "SetQueryPeriod"
		// Use new syntax.
		return new SetInputPeriod_Command ();
	}
    else if ( commandNameUpper.equals("SETTABLECOLUMNPROPERTIES") ) { // "SetTableColumnProperties"
        return new SetTableColumnProperties_Command ();
    }
    else if ( commandNameUpper.equals("SETTABLEVALUES") ) { // "SetTableValues"
        return new SetTableValues_Command ();
    }
    else if ( commandNameUpper.equals("SETTIMESERIESPROPERTIESFROMTABLE") ) { // "SetTimeSeriesPropertiesFromTable"
        return new SetTimeSeriesPropertiesFromTable_Command ();
    }
	else if ( commandNameUpper.equals("SETTIMESERIESPROPERTY") ) { // "SetTimeSeriesProperty"
		return new SetTimeSeriesProperty_Command ();
	}
    else if ( commandNameUpper.equals("SETTIMESERIESVALUESFROMLOOKUPTABLE") ) { // "SetTimeSeriesValuesFromLookupTable"
        return new SetTimeSeriesValuesFromLookupTable_Command ();
    }
    else if ( commandNameUpper.equals("SETTIMESERIESVALUESFROMTABLE") ) { // "SetTimeSeriesValuesFromTable"
        return new SetTimeSeriesValuesFromTable_Command ();
    }
    else if ( commandNameUpper.equals("SETMAX") // "SetMax")
    	|| commandNameUpper.equals("SETTOMAX") ) { // "SetToMax"
        // Legacy is "SetMax" so translate on the fly.
        return new SetToMax_Command ();
    }
    else if ( commandNameUpper.equals("SETTOMIN") ) { // "SetToMin"
        return new SetToMin_Command ();
    }
    else if ( commandNameUpper.equals("SETWARNINGLEVEL") ) { // "SetWarningLevel"
        return new SetWarningLevel_Command ();
    }
    else if ( commandNameUpper.equals("SETWORKINGDIR") ) { // "SetWorkingDir"
        return new SetWorkingDir_Command ();
    }
    else if ( commandNameUpper.equals("SHIFTTIMEBYINTERVAL") ) { // "ShiftTimeByInterval"
        return new ShiftTimeByInterval_Command ();
    }
    else if ( commandNameUpper.equals("SORTTABLE") ) { // "SortTable"
        return new SortTable_Command ();
    }
    else if ( commandNameUpper.equals("SORTTIMESERIES") ) { // "SortTimeSeries"
        return new SortTimeSeries_Command ();
    }
    else if ( commandNameUpper.equals("SPLITTABLECOLUMN") ) { // "SplitTableColumn"
        return new SplitTableColumn_Command ();
    }
    else if ( commandNameUpper.equals("SPLITTABLEROW") ) { // "SplitTableRow"
        return new SplitTableRow_Command ();
    }
	else if ( commandNameUpper.equals("STARTLOG") ) { // "StartLog"
		return new StartLog_Command ();
	}
    else if ( commandNameUpper.equals("STARTREGRESSIONTESTRESULTSREPORT") ) { // "StartRegressionTestResultsReport"
        return new StartRegressionTestResultsReport_Command ();
    }
    else if ( commandNameUpper.equals("STATEMODMAX") ) { // "StateModMax"
        return new StateModMax_Command ();
    }
    else if ( commandNameUpper.equals("SUBTRACT") ) { // "Subtract"
        return new Subtract_Command ();
    }
	
	// "T" commands.

    else if ( commandNameUpper.equals("TABLEMATH") ) { // "TableMath"
        return new TableMath_Command ();
    }
    else if ( commandNameUpper.equals("TABLETIMESERIESMATH") ) { // "TableTimeSeriesMath"
        return new TableTimeSeriesMath_Command ();
    }
    else if ( commandNameUpper.equals("TABLETOTIMESERIES") ) { // "TableToTimeSeries"
        return new TableToTimeSeries_Command ();
    }
    else if ( commandNameUpper.equals("TESTCOMMAND") ) { // "TestCommand"
		return new TestCommand_Command ();
	}
    else if ( commandNameUpper.equals("TEXTEDIT") ) { // "TextEdit"
		return new TextEdit_Command ();
	}
    else if ( commandNameUpper.equals("TIMESERIESTOTABLE") ) { // "TimeSeriesToTable"
        return new TimeSeriesToTable_Command ();
    }

    // "U" commands.

    else if ( commandNameUpper.equals("UNZIPFILE") ) { // "UnzipFile"
        return new UnzipFile_Command ();
    }

    // "V" commands.

    else if ( commandNameUpper.equals("VARIABLELAGK") ) { // "VariableLagK"
        return new VariableLagK_Command ();
    }

	// "W" commands.

    else if ( commandNameUpper.equals("WAIT") ) { // "Wait"
        return new Wait_Command ();
    }
    else if ( commandNameUpper.equals("WEBGET") ) { // "WebGet"
        return new WebGet_Command ();
    }
    else if ( commandNameUpper.equals("WEIGHTTRACES") ) { // "WeightTraces"
        return new WeightTraces_Command ();
    }
    else if ( commandNameUpper.equals("WRITECHECKFILE") ) { // "WriteCheckFile"
        return new WriteCheckFile_Command ();
    }
	else if ( commandNameUpper.equals("WRITEDATEVALUE") ) { // "WriteDateValue"
		return new WriteDateValue_Command ();
	}
    else if ( commandNameUpper.equals("WRITEDELFTFEWSPIXML") ) { // "WriteDelftFewsPiXml"
        return new WriteDelftFewsPiXml_Command ();
    }
    else if ( commandNameUpper.equals("WRITEDELIMITEDFILE") ) { // "WriteDelimitedFile"
        return new WriteDelimitedFile_Command ();
    }
    else if ( commandNameUpper.equals("WRITEHECDSS") ) { // "WriteHecDss"
        return new WriteHecDss_Command ();
    }
    else if ( commandNameUpper.equals("WRITENWSCARD") ) { // "WriteNwsCard"
        return new WriteNwsCard_Command();
    }
	else if ( commandNameUpper.equals("WRITENWSRFSESPTRACEENSEMBLE") ) { // "WriteNWSRFSESPTraceEnsemble"
		return new WriteNWSRFSESPTraceEnsemble_Command();
	}
    else if ( commandNameUpper.equals("WRITEOBJECTTOJSON") ) { // "WriteObjectToJSON"
        return new WriteObjectToJSON_Command();
    }
    else if ( commandNameUpper.equals("WRITEPROPERTIESTOFILE") ) { // "WritePropertiesToFile"
        return new WritePropertiesToFile_Command ();
    }
	else if ( commandNameUpper.equals("WRITEPROPERTY") ) { // "WriteProperty"
		return new WriteProperty_Command ();
	}
    else if ( commandNameUpper.equals("WRITERECLAMATIONHDB") ) { // "WriteReclamationHDB"
        return new WriteReclamationHDB_Command ();
    }
	else if ( commandNameUpper.equals("WRITERIVERWARE") ) { // "WriteRiverWare"
		return new WriteRiverWare_Command ();
	}
    else if ( commandNameUpper.equals("WRITESHEF") ) { // "WriteSHEF"
        return new WriteSHEF_Command ();
    }
    else if ( commandNameUpper.equals("WRITESTATECU") ) { // "WriteStateCU"
        return new WriteStateCU_Command ();
    }
	else if ( commandNameUpper.equals("WRITESTATEMOD") ) { // "WriteStateMod"
		return new WriteStateMod_Command ();
	}
	else if ( commandNameUpper.equals("WRITESUMMARY") ) { // "WriteSummary"
		return new WriteSummary_Command ();
	}
    else if ( commandNameUpper.equals("WRITETABLETODATASTORE") ) { // "WriteTableToDataStore"
        return new WriteTableToDataStore_Command ();
    }
    else if ( commandNameUpper.equals("WRITETABLETODELIMITEDFILE") ) { // "WriteTableToDelimitedFile"
        return new WriteTableToDelimitedFile_Command ();
    }
    else if ( commandNameUpper.equals("WRITETABLETOEXCEL") ) { // "WriteTableToExcel"
        return new WriteTableToExcel_Command ();
    }
    else if ( commandNameUpper.equals("WRITETABLECELLSTOEXCEL") ) { // "WriteTableCellsToExcel"
        return new WriteTableCellsToExcel_Command ();
    }
    else if ( commandNameUpper.equals("WRITETABLETOGEOJSON") ) { // "WriteTableToGeoJSON"
        return new WriteTableToGeoJSON_Command ();
    }
    else if ( commandNameUpper.equals("WRITETABLETOHTML") ) { // "WriteTableToHTML"
        return new WriteTableToHTML_Command ();
    }
    else if ( commandNameUpper.equals("WRITETABLETOMARKDOWN") ) { // "WriteTableToMarkdown"
        return new WriteTableToMarkdown_Command ();
    }
    else if ( commandNameUpper.equals("WRITETABLETOKML") ) { // "WriteTableToKml"
        return new WriteTableToKml_Command ();
    }
    else if ( commandNameUpper.equals("WRITETABLETOSHAPEFILE") ) { // "WriteTableToShapefile"
        return new WriteTableToShapefile_Command ();
    }
    else if ( commandNameUpper.equals("WRITETIMESERIESPROPERTIESTOFILE") ) { // "WriteTimeSeriesPropertiesToFile"
        return new WriteTimeSeriesPropertiesToFile_Command ();
    }
    else if ( commandNameUpper.equals("WRITETIMESERIESPROPERTY") ) { // "WriteTimeSeriesProperty"
        return new WriteTimeSeriesProperty_Command ();
    }
    else if ( commandNameUpper.equals("WRITETIMESERIESTODATASTORE") ) { // "WriteTimeSeriesToDataStore"
        return new WriteTimeSeriesToDataStore_Command ();
    }
    else if ( commandNameUpper.equals("WRITETIMESERIESTODATASTREAM") ) { // "WriteTimeSeriesToDataStream"
        return new WriteTimeSeriesToDataStream_Command ();
    }
    else if ( commandNameUpper.equals("WRITETIMESERIESTOEXCEL") ) { // "WriteTimeSeriesToExcel"
        return new WriteTimeSeriesToExcel_Command ();
    }
    else if ( commandNameUpper.equals("WRITETIMESERIESTOEXCELBLOCK") ) { // "WriteTimeSeriesToExcelBlock"
        return new WriteTimeSeriesToExcelBlock_Command ();
    }
    else if ( commandNameUpper.equals("WRITETIMESERIESTOHYDROJSON") ) { // "WriteTimeSeriesToHydroJSON"
        return new WriteTimeSeriesToHydroJSON_Command ();
    }
    else if ( commandNameUpper.equals("WRITETIMESERIESTOJSON") ) { // "WriteTimeSeriesToJson"
        return new WriteTimeSeriesToJson_Command ();
    }
    else if ( commandNameUpper.equals("WRITETIMESERIESTOGEOJSON") ) { // "WriteTimeSeriesToGeoJSON"
        return new WriteTimeSeriesToGeoJSON_Command ();
    }
    else if ( commandNameUpper.equals("WRITETIMESERIESTOKML") ) { // "WriteTimeSeriesToKml"
        return new WriteTimeSeriesToKml_Command ();
    }
    else if ( commandNameUpper.equals("WRITEWATERML") ) { // "WriteWaterML"
        return new WriteWaterML_Command ();
    }
    else if ( commandNameUpper.equals("WRITEWATERML2") ) { // "WriteWaterML2"
        return new WriteWaterML2_Command ();
    }

    // TODO smalers 2016-04-02 Figure out more elegant approach for getting command name.
    // Check for plugin commands - for now brute force based on naming convention.

	if ( Message.isDebugOn ) {
		Message.printDebug(1,routine,"Did not match built-in command, checking plugin command classes.");
	}
    if ( this.pluginCommandClassList.size() > 0 ) {
    	if ( Message.isDebugOn ) {
    		Message.printDebug(1,routine,"Checking " + this.pluginCommandClassList.size() + " plugin classes for matching command.");
    	}
    	for ( @SuppressWarnings("rawtypes") Class c : this.pluginCommandClassList ) {
	    	String nameFromClass = c.getSimpleName(); // Should be like:  CommandName_Command
	    	int pos = nameFromClass.indexOf("_Command");
	    	if ( pos > 0 ) {
	    		nameFromClass = nameFromClass.substring(0,pos);
	    		if ( Message.isDebugOn ) {
	    			Message.printDebug(1,routine,"Checking plugin command \"" + nameFromClass + "\" against command name \"" + commandName + "\"");
	    		}
	    		if ( nameFromClass.equals(commandName) ) {
	    	    	// Construct the command instance.
	    	    	try {
	    	    		@SuppressWarnings("unchecked")
						Constructor<?> constructor = c.getConstructor();
	    	    		Object command = constructor.newInstance();
	    	    		// The object must be a Command if it follows implementation requirements.
	    	    		return (Command)command;
	    	    	}
	    	    	catch ( NoSuchMethodException e ) {
	    	    		Message.printWarning(2,routine,"Error getting constructor for plugin command class \"" + nameFromClass + "\"");
	    	    	}
	    	    	catch ( IllegalAccessException e ) {
	    	    		Message.printWarning(2,routine,"Error creating instance of command for plugin command class \"" + nameFromClass + "\"");
	    	    	}
	    	    	catch ( InstantiationException e ) {
	    	    		Message.printWarning(2,routine,"Error creating instance of command for plugin command class \"" + nameFromClass + "\"");
	    	    	}
	    	    	catch ( InvocationTargetException e ) {
	    	    		Message.printWarning(2,routine,"Error creating instance of command for plugin command class \"" + nameFromClass + "\"");
	    	    	}
	    	    	// No need to keep searching.
    	    		break;
	    		}
	    	}
    	}
    }
    else {
  		if ( Message.isDebugOn ) {
  			Message.printDebug(1,routine,"Plugin class list size is 0.");
  		}
    }

    // Check for time series identifier:
    // - this is the fall through if no command was matched above but the string matches a TSID pattern

	if ( TSCommandProcessorUtil.isTSID(commandString) ) {
	    return new TSID_Command ();
	}

	// Check for blank line, which will result in Empty command.
	
    if ( commandName.equalsIgnoreCase("") ) {
        return new Empty_Command ();
    }

	// Did not match a command or TSID.

	if ( createUnknownCommandIfNotRecognized ) {
		// Create an unknown command.
		Command c = new UnknownCommand ();
		c.setCommandString( commandString );
        Message.printStatus ( 2, routine, "Creating UnknownCommand for unknown command \"" + commandString + "\"" );
		return c;
	}
	else {
		// Throw an exception if the command is not recognized.
		throw new UnknownCommandException ( "Unknown command \"" + commandString + "\"" );
	}
}

}