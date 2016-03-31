package rti.tscommandprocessor.core;

// TODO SAM 2005-07-10 Need to evaluate where command classes should live to
// avoid mixing with lower-level code.
//import RTi.DataServices.Adapter.NDFD.openNDFD_Command;
//import RTi.DataServices.Adapter.NDFD.readNDFD_Command;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandFactory;
import RTi.Util.IO.UnknownCommand;
import RTi.Util.IO.UnknownCommandException;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

// Data checks (for now keep separate from logging and data tests)
import rti.tscommandprocessor.commands.check.CheckTimeSeriesStatistic_Command;
import rti.tscommandprocessor.commands.check.CheckTimeSeries_Command;
import rti.tscommandprocessor.commands.check.WriteCheckFile_Command;

// Datastream commands
import rti.tscommandprocessor.commands.datastream.WriteTimeSeriesToDataStream_Command;
import rti.tscommandprocessor.commands.datastore.CloseDataStore_Command;
// DateValue commands
import rti.tscommandprocessor.commands.datastore.CreateDataStoreDataDictionary_Command;
import rti.tscommandprocessor.commands.datastore.DeleteDataStoreTableRows_Command;
import rti.tscommandprocessor.commands.datastore.ReadTimeSeriesFromDataStore_Command;
import rti.tscommandprocessor.commands.datastore.RunSql_Command;
import rti.tscommandprocessor.commands.datastore.WriteTimeSeriesToDataStore_Command;
import rti.tscommandprocessor.commands.datevalue.ReadDateValue_Command;
import rti.tscommandprocessor.commands.datevalue.WriteDateValue_Command;
import rti.tscommandprocessor.commands.delftfews.ReadDelftFewsPiXml_Command;
import rti.tscommandprocessor.commands.delftfews.WriteDelftFewsPiXml_Command;
// Delimited time series file commands
import rti.tscommandprocessor.commands.delimited.ReadDelimitedFile_Command;
import rti.tscommandprocessor.commands.delimited.WriteDelimitedFile_Command;
import rti.tscommandprocessor.commands.derby.NewDerbyDatabase_Command;

// Ensemble commands
import rti.tscommandprocessor.commands.ensemble.CopyEnsemble_Command;
import rti.tscommandprocessor.commands.ensemble.CreateEnsembleFromOneTimeSeries_Command;
import rti.tscommandprocessor.commands.ensemble.InsertTimeSeriesIntoEnsemble_Command;
import rti.tscommandprocessor.commands.ensemble.NewEnsemble_Command;
import rti.tscommandprocessor.commands.ensemble.NewStatisticEnsemble_Command;
import rti.tscommandprocessor.commands.ensemble.NewStatisticTimeSeriesFromEnsemble_Command;
import rti.tscommandprocessor.commands.ensemble.SetEnsembleProperty_Command;

// GRTS commands (time series products).
import rti.tscommandprocessor.commands.products.ProcessRasterGraph_Command;
import rti.tscommandprocessor.commands.products.ProcessTSProduct_Command;

// HEC-DSS commands.
import rti.tscommandprocessor.commands.hecdss.ReadHecDss_Command;
import rti.tscommandprocessor.commands.hecdss.WriteHecDss_Command;

// HydroBase commands.
import rti.tscommandprocessor.commands.hydrobase.FillUsingDiversionComments_Command;
import rti.tscommandprocessor.commands.hydrobase.OpenHydroBase_Command;
import rti.tscommandprocessor.commands.hydrobase.ReadHydroBase_Command;
import rti.tscommandprocessor.commands.hydrojson.WriteTimeSeriesToHydroJSON_Command;
import rti.tscommandprocessor.commands.table.ReadTableFromJSON_Command;
import rti.tscommandprocessor.commands.json.WriteTimeSeriesToJson_Command;

// Logging commands.
import rti.tscommandprocessor.commands.logging.Message_Command;
import rti.tscommandprocessor.commands.logging.SetDebugLevel_Command;
import rti.tscommandprocessor.commands.logging.SetWarningLevel_Command;
import rti.tscommandprocessor.commands.logging.StartLog_Command;

// MODSIM commands
import rti.tscommandprocessor.commands.modsim.ReadMODSIM_Command;

// Network commands
import rti.tscommandprocessor.commands.network.AnalyzeNetworkPointFlow_Command;

// NRCS commands
import rti.tscommandprocessor.commands.nrcs.awdb.ReadNrcsAwdb_Command;

// NWSRFS commands.
import rti.tscommandprocessor.commands.nwsrfs.ReadNwsCard_Command;
import rti.tscommandprocessor.commands.nwsrfs.ReadNwsrfsEspTraceEnsemble_Command;
import rti.tscommandprocessor.commands.nwsrfs.ReadNwsrfsFS5Files_Command;
import rti.tscommandprocessor.commands.nwsrfs.SetPropertyFromNwsrfsAppDefault_Command;
import rti.tscommandprocessor.commands.nwsrfs.WriteNwsCard_Command;
import rti.tscommandprocessor.commands.nwsrfs.WriteNWSRFSESPTraceEnsemble_Command;

// RCC ACIS commands
import rti.tscommandprocessor.commands.rccacis.ReadRccAcis_Command;

// Reclamation HDB commands
import rti.tscommandprocessor.commands.reclamationhdb.ReadReclamationHDB_Command;
import rti.tscommandprocessor.commands.reclamationhdb.WriteReclamationHDB_Command;

//Reclamation Pisces commands
import rti.tscommandprocessor.commands.reclamationpisces.ReadReclamationPisces_Command;

// RiversideDB commands
import rti.tscommandprocessor.commands.riversidedb.ReadRiversideDB_Command;
import rti.tscommandprocessor.commands.riversidedb.WriteRiversideDB_Command;

// RiverWare commands
import rti.tscommandprocessor.commands.riverware.ReadRiverWare_Command;
import rti.tscommandprocessor.commands.riverware.WriteRiverWare_Command;

// SHEF commands.
import rti.tscommandprocessor.commands.shef.WriteSHEF_Command;
import rti.tscommandprocessor.commands.spatial.WriteTableToGeoJSON_Command;
// Spatial commands.
import rti.tscommandprocessor.commands.spatial.WriteTableToKml_Command;
import rti.tscommandprocessor.commands.spatial.WriteTableToShapefile_Command;
import rti.tscommandprocessor.commands.spatial.WriteTimeSeriesToGeoJSON_Command;
import rti.tscommandprocessor.commands.spatial.WriteTimeSeriesToKml_Command;
import rti.tscommandprocessor.commands.spreadsheet.CloseExcelWorkbook_Command;
// Spreadsheet commands
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
import rti.tscommandprocessor.commands.table.FormatTableDateTime_Command;
import rti.tscommandprocessor.commands.table.FormatTableString_Command;
import rti.tscommandprocessor.commands.table.FreeTable_Command;
import rti.tscommandprocessor.commands.table.InsertTableColumn_Command;
import rti.tscommandprocessor.commands.table.InsertTableRow_Command;
import rti.tscommandprocessor.commands.table.JoinTables_Command;
import rti.tscommandprocessor.commands.table.ManipulateTableString_Command;
import rti.tscommandprocessor.commands.table.NewTable_Command;
import rti.tscommandprocessor.commands.table.ReadTableFromDBF_Command;
import rti.tscommandprocessor.commands.table.ReadTableFromDataStore_Command;
import rti.tscommandprocessor.commands.table.ReadTableFromDelimitedFile_Command;
import rti.tscommandprocessor.commands.table.ReadTableFromFixedFormatFile_Command;
import rti.tscommandprocessor.commands.table.ReadTableFromXML_Command;
import rti.tscommandprocessor.commands.table.SetPropertyFromTable_Command;
import rti.tscommandprocessor.commands.table.SetTableValues_Command;
import rti.tscommandprocessor.commands.table.SetTimeSeriesPropertiesFromTable_Command;
import rti.tscommandprocessor.commands.table.SortTable_Command;
import rti.tscommandprocessor.commands.table.SplitTableColumn_Command;
import rti.tscommandprocessor.commands.table.SplitTableRow_Command;
import rti.tscommandprocessor.commands.table.TableMath_Command;
import rti.tscommandprocessor.commands.table.TableTimeSeriesMath_Command;
import rti.tscommandprocessor.commands.table.TableToTimeSeries_Command;
import rti.tscommandprocessor.commands.table.TimeSeriesToTable_Command;
import rti.tscommandprocessor.commands.table.WriteTableToDataStore_Command;
import rti.tscommandprocessor.commands.table.WriteTableToDelimitedFile_Command;
import rti.tscommandprocessor.commands.table.WriteTableToHTML_Command;

// Template commands
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
import rti.tscommandprocessor.commands.ts.ChangeIntervalLarger_Command;
import rti.tscommandprocessor.commands.ts.ChangeIntervalRegularToIrregular_Command;
import rti.tscommandprocessor.commands.ts.ChangeIntervalSmaller_Command;
import rti.tscommandprocessor.commands.ts.ChangeInterval_Command;
import rti.tscommandprocessor.commands.ts.ChangePeriod_Command;
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

// USGS commands
import rti.tscommandprocessor.commands.usgs.nwis.rdb.ReadUsgsNwisRdb_Command;
import rti.tscommandprocessor.commands.usgs.nwis.daily.ReadUsgsNwisDaily_Command;
import rti.tscommandprocessor.commands.usgs.nwis.groundwater.ReadUsgsNwisGroundwater_Command;
import rti.tscommandprocessor.commands.usgs.nwis.instantaneous.ReadUsgsNwisInstantaneous_Command;

// Utility commands.
import rti.tscommandprocessor.commands.util.AppendFile_Command;
import rti.tscommandprocessor.commands.util.Comment_Command;
import rti.tscommandprocessor.commands.util.CommentBlockStart_Command;
import rti.tscommandprocessor.commands.util.CommentBlockEnd_Command;
import rti.tscommandprocessor.commands.util.CompareFiles_Command;
import rti.tscommandprocessor.commands.util.CopyFile_Command;
import rti.tscommandprocessor.commands.util.CreateRegressionTestCommandFile_Command;
import rti.tscommandprocessor.commands.util.Empty_Command;
import rti.tscommandprocessor.commands.util.EndFor_Command;
import rti.tscommandprocessor.commands.util.EndIf_Command;
import rti.tscommandprocessor.commands.util.Exit_Command;
import rti.tscommandprocessor.commands.util.FTPGet_Command;
import rti.tscommandprocessor.commands.util.For_Command;
import rti.tscommandprocessor.commands.util.FormatDateTimeProperty_Command;
import rti.tscommandprocessor.commands.util.FormatStringProperty_Command;
import rti.tscommandprocessor.commands.util.If_Command;
import rti.tscommandprocessor.commands.util.ListFiles_Command;
import rti.tscommandprocessor.commands.util.MergeListFileColumns_Command;
import rti.tscommandprocessor.commands.util.PrintTextFile_Command;
import rti.tscommandprocessor.commands.util.ProfileCommands_Command;
import rti.tscommandprocessor.commands.util.ReadPropertiesFromFile_Command;
import rti.tscommandprocessor.commands.util.RemoveFile_Command;
import rti.tscommandprocessor.commands.util.RunCommands_Command;
import rti.tscommandprocessor.commands.util.RunDSSUTL_Command;
import rti.tscommandprocessor.commands.util.RunProgram_Command;
import rti.tscommandprocessor.commands.util.RunPython_Command;
import rti.tscommandprocessor.commands.util.SetProperty_Command;
import rti.tscommandprocessor.commands.util.SetWorkingDir_Command;
import rti.tscommandprocessor.commands.util.StartRegressionTestResultsReport_Command;
import rti.tscommandprocessor.commands.util.TestCommand_Command;
import rti.tscommandprocessor.commands.util.UnzipFile_Command;
import rti.tscommandprocessor.commands.util.Wait_Command;
import rti.tscommandprocessor.commands.util.WebGet_Command;
import rti.tscommandprocessor.commands.util.WritePropertiesToFile_Command;
import rti.tscommandprocessor.commands.util.WriteProperty_Command;
import rti.tscommandprocessor.commands.view.NewTreeView_Command;

// WaterML commands
import rti.tscommandprocessor.commands.waterml.ReadWaterML_Command;
import rti.tscommandprocessor.commands.waterml.WriteWaterML_Command;

// WaterOneFlow commands
import rti.tscommandprocessor.commands.wateroneflow.ws.ReadWaterOneFlow_Command;

/**
This class instantiates Commands for time series processing.  The full command name
is required, but parameters are not because parsing does not occur.
*/
public class TSCommandFactory implements CommandFactory
{
    
/**
Constructor.
*/
public TSCommandFactory ()
{
    super();
}
	
/**
Return a new command, based on the command name.  DO NOT create an
UnknownCommand if the command is not recognized.
@return a new command, based on the command name.
@param command_string The command string to process.
@throws UnknownCommandException if the command name is not recognized.
*/
public Command newCommand ( String command_string )
throws UnknownCommandException
{
	return newCommand ( command_string, false );
}

/**
Return a new command, based on the command name.
@return a new command, based on the command name.
@param commandString The command string to process.  The command string can
contain parameters but they are not parsed.  At a minimum, the command string needs
to be of the form "CommandName()" or "TS Alias = CommandName()".
@param createUnknownCommandIfNotRecognized If true and the command is
not recognized, create an UnknownCommand instance that holds the command string.
This is useful for code that is being migrated to the full command class design.
@throws UnknownCommandException if the command name is not recognized
(and createUnknownCommandIfNotRecognized=false).
*/
public Command newCommand ( String commandString, boolean createUnknownCommandIfNotRecognized )
throws UnknownCommandException
{	commandString = commandString.trim(); // Full command string, with no surrounding white space
    String commandName = ""; // Command name without "(...parameters...)"
    String routine = "TSCommandFactory.newCommand";

	// Parse out arguments for TS alias = foo() commands to be able to handle nulls here

	String token0 = StringUtil.getToken(commandString,"( =",StringUtil.DELIM_SKIP_BLANKS,0);
    if ( (token0 != null) && token0.equalsIgnoreCase( "TS") ) {
		// This allows aliases with spaces...
		commandName = StringUtil.getToken(commandString,"(=",StringUtil.DELIM_SKIP_BLANKS,1);
		if ( commandName == null ) {
		    commandName = "";
		}
		else {
		    commandName = commandName.trim();
		}
	}
	else {
	    // Get the potential command name, which is the text prior to the (
	    // However, it could be that a TSID string contains () so if the command name
	    // is not matched below and it fits the TSID pattern, treat as a TSID
	    int pos = commandString.indexOf("(");
	    if ( pos > 0 ) {
	        commandName = commandString.substring(0,pos).trim();
	    }
	    else {
	        // The command name is the entire string (e.g., new commands will not have the ())
	        commandName = commandString;
	    }
	}
	if ( Message.isDebugOn ) {
	    Message.printDebug(1,routine,"commandName=\"" + commandName + "\"" );
	}
	
	// The following checks for a match for specific command name and if so an appropriate
	// command instance is created and returned.  If nothing is matched and the command string is a TSID,
	// a TSID command instance will be returned.
	
	// Comment commands...
	
    if ( commandString.startsWith("#") ) {
        return new Comment_Command ();
    }
    else if ( commandString.startsWith("/*") ) {
        return new CommentBlockStart_Command ();
    }
    else if ( commandString.startsWith("*/") ) {
        return new CommentBlockEnd_Command ();
    }

	// "A" commands...

    // Put the following before "Add"
    else if ( commandName.equalsIgnoreCase("AddConstant") ) {
        return new AddConstant_Command ();
    }
    else if ( commandName.equalsIgnoreCase("Add") ) {
        return new Add_Command ();
    }
    else if ( commandName.equalsIgnoreCase("AdjustExtremes") ) {
        return new AdjustExtremes_Command ();
    }
    else if ( commandName.equalsIgnoreCase("AnalyzeNetworkPointFlow") ) {
        return new AnalyzeNetworkPointFlow_Command ();
    }
    else if ( commandName.equalsIgnoreCase("AnalyzePattern") ) {
		return new AnalyzePattern_Command ();
	}
    else if ( commandName.equalsIgnoreCase("AppendFile") ) {
        return new AppendFile_Command ();
    }
    else if ( commandName.equalsIgnoreCase("AppendTable") ) {
        return new AppendTable_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ARMA") ) {
        return new ARMA_Command ();
    }
    
    // "B" commands...
    
    else if ( commandName.equalsIgnoreCase("Blend") ) {
        return new Blend_Command ();
    }

	// "C" commands...

    else if ( commandName.equalsIgnoreCase("CalculateTimeSeriesStatistic") ) {
        return new CalculateTimeSeriesStatistic_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ChangePeriod") ) {
        return new ChangePeriod_Command ();
    }
	else if ( commandName.equalsIgnoreCase("ChangeInterval") ) {
		return new ChangeInterval_Command ();
	}
	else if ( commandName.equalsIgnoreCase("ChangeIntervalIrregularToRegular") ) {
		return new ChangeIntervalIrregularToRegular_Command ();
	}
	else if ( commandName.equalsIgnoreCase("ChangeIntervalLarger") ) {
		return new ChangeIntervalLarger_Command ();
	}
	else if ( commandName.equalsIgnoreCase("ChangeIntervalRegularToIrregular") ) {
		return new ChangeIntervalRegularToIrregular_Command ();
	}
	else if ( commandName.equalsIgnoreCase("ChangeIntervalSmaller") ) {
		return new ChangeIntervalSmaller_Command ();
	}
    else if ( commandName.equalsIgnoreCase("CheckTimeSeries") ) {
        return new CheckTimeSeries_Command ();
    }
    else if ( commandName.equalsIgnoreCase("CheckTimeSeriesStatistic") ) {
        return new CheckTimeSeriesStatistic_Command ();
    }
	else if ( commandName.equalsIgnoreCase("CloseDataStore") ) {
		return new CloseDataStore_Command ();
	}
	else if ( commandName.equalsIgnoreCase("CloseExcelWorkbook") ) {
		return new CloseExcelWorkbook_Command ();
	}
	else if ( commandName.equalsIgnoreCase("CompareFiles") ) {
		return new CompareFiles_Command ();
	}
    else if ( commandName.equalsIgnoreCase("CompareTables") ) {
        return new CompareTables_Command ();
    }
	else if ( commandName.equalsIgnoreCase("CompareTimeSeries") ) {
		return new CompareTimeSeries_Command ();
	}
    else if ( commandName.equalsIgnoreCase("ComputeErrorTimeSeries") ) {
        return new ComputeErrorTimeSeries_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ConvertDataUnits") ) {
        return new ConvertDataUnits_Command ();
    }
	else if ( commandName.equalsIgnoreCase("Copy") ) {
		return new Copy_Command ();
	}
    else if ( commandName.equalsIgnoreCase("CopyEnsemble") ) {
        return new CopyEnsemble_Command ();
    }
    else if ( commandName.equalsIgnoreCase("CopyFile") ) {
        return new CopyFile_Command ();
    }
    else if ( commandName.equalsIgnoreCase("CopyPropertiesToTable") ) {
        return new CopyPropertiesToTable_Command ();
    }
    else if ( commandName.equalsIgnoreCase("CopyTable") ) {
        return new CopyTable_Command ();
    }
    else if ( commandName.equalsIgnoreCase("CopyTimeSeriesPropertiesToTable") ) {
        return new CopyTimeSeriesPropertiesToTable_Command ();
    }
    else if ( commandName.equalsIgnoreCase("CreateDataStoreDataDictionary") ) {
        return new CreateDataStoreDataDictionary_Command ();
    }
    else if ( commandName.equalsIgnoreCase("CreateEnsembleFromOneTimeSeries") ||
        commandName.equalsIgnoreCase("CreateEnsemble")) {
        // The command name changed...
        return new CreateEnsembleFromOneTimeSeries_Command ();
    }
    else if ( commandName.equalsIgnoreCase("CreateFromList") ) {
        return new CreateFromList_Command ();
    }
    else if ( commandName.equalsIgnoreCase("CreateTimeSeriesEventTable") ) {
        return new CreateTimeSeriesEventTable_Command ();
    }
	else if ( commandName.equalsIgnoreCase("CreateRegressionTestCommandFile") ) {
		return new CreateRegressionTestCommandFile_Command ();
	}
	else if ( commandName.equalsIgnoreCase("Cumulate") ) {
		return new Cumulate_Command ();
	}
    
    // "D" commands...

    else if ( commandName.equalsIgnoreCase("DeleteDataStoreTableRows") ) {
        // Automatically change the name
        return new DeleteDataStoreTableRows_Command ();
    }
    else if ( commandName.equalsIgnoreCase("Delta") ) {
        return new Delta_Command ();
    }
	else if ( commandName.equalsIgnoreCase("DeselectTimeSeries") ) {
        return new DeselectTimeSeries_Command ();
    }
    else if ( commandName.equalsIgnoreCase("Disaggregate") ) {
        return new Disaggregate_Command ();
    }
    else if ( commandName.equalsIgnoreCase("Divide") ) {
        return new Divide_Command ();
    }
	
	// "E" commands...

    else if ( commandName.equalsIgnoreCase("EndFor") ) {
        return new EndFor_Command ();
    }
    else if ( commandName.equalsIgnoreCase("EndIf") ) {
        return new EndIf_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(commandString,"Exit") ||
	    StringUtil.startsWithIgnoreCase(commandString,"Exit")) {
		return new Exit_Command ();
	}
    else if ( commandName.equalsIgnoreCase("ExpandTemplateFile") ) {
        return new ExpandTemplateFile_Command ();
    }

	// "F" commands...

	else if ( commandName.equalsIgnoreCase("FillConstant") ) {
		return new FillConstant_Command ();
	}
    else if ( commandName.equalsIgnoreCase("FillDayTSFrom2MonthTSAnd1DayTS") ) {
        return new FillDayTSFrom2MonthTSAnd1DayTS_Command ();
    }
    else if ( commandName.equalsIgnoreCase("FillFromTS") ) {
        return new FillFromTS_Command ();
    }
	else if ( commandName.equalsIgnoreCase("FillHistMonthAverage") ) {
		return new FillHistMonthAverage_Command ();
	}
	else if ( commandName.equalsIgnoreCase("FillHistYearAverage") ) {
		return new FillHistYearAverage_Command ();
	}
    else if ( commandName.equalsIgnoreCase("FillInterpolate") ) {
        return new FillInterpolate_Command ();
    }
	else if ( commandName.equalsIgnoreCase("FillMixedStation") ) {
		return new FillMixedStation_Command ();
	}
	else if ( commandName.equalsIgnoreCase("FillMOVE2") ) {
		return new FillMOVE2_Command ();
	}
    else if ( commandName.equalsIgnoreCase("FillPattern") ) {
        return new FillPattern_Command ();
    }
	else if ( commandName.equalsIgnoreCase("FillPrincipalComponentAnalysis") ) {
		return new FillPrincipalComponentAnalysis_Command ();
	}
    else if ( commandName.equalsIgnoreCase("FillProrate") ) {
        return new FillProrate_Command ();
    }
	else if ( commandName.equalsIgnoreCase("FillRegression") ) {
		return new FillRegression_Command ();
	}
    else if ( commandName.equalsIgnoreCase("FillRepeat") ) {
        return new FillRepeat_Command ();
    }
	else if ( commandName.equalsIgnoreCase("FillUsingDiversionComments") ) {
		return new FillUsingDiversionComments_Command ();
	}
    else if ( commandName.equalsIgnoreCase("For") ) {
        return new For_Command ();
    }
    else if ( commandName.equalsIgnoreCase("FormatDateTimeProperty") ) {
        return new FormatDateTimeProperty_Command ();
    }
    else if ( commandName.equalsIgnoreCase("FormatStringProperty") ) {
        return new FormatStringProperty_Command ();
    }
    else if ( commandName.equalsIgnoreCase("FormatTableDateTime") ) {
        return new FormatTableDateTime_Command ();
    }
    else if ( commandName.equalsIgnoreCase("FormatTableString") ) {
        return new FormatTableString_Command ();
    }
    else if ( commandName.equalsIgnoreCase("Free") ) {
        return new Free_Command ();
    }
    else if ( commandName.equalsIgnoreCase("FreeTable") ) {
        return new FreeTable_Command ();
    }
    else if ( commandName.equalsIgnoreCase("FTPGet") ) {
        return new FTPGet_Command ();
    }
    /*
    else if ( commandName.equalsIgnoreCase("FreeEnsemble") ) {
        return new FreeEnsemble_Command ();
    }
    */
    
    // "I" commands...

    else if ( commandName.equalsIgnoreCase("If") ) {
        return new If_Command ();
    }
    else if ( commandName.equalsIgnoreCase("InsertTableColumn") ) {
        return new InsertTableColumn_Command ();
    }
    else if ( commandName.equalsIgnoreCase("InsertTableRow") ) {
        return new InsertTableRow_Command ();
    }
    else if ( commandName.equalsIgnoreCase("InsertTimeSeriesIntoEnsemble") ) {
        return new InsertTimeSeriesIntoEnsemble_Command ();
    }
    
    // "J" commands...

    else if ( commandName.equalsIgnoreCase("JoinTables") ) {
        return new JoinTables_Command ();
    }
	
	// "L" commands...

	else if ( commandName.equalsIgnoreCase("LagK") ) {
		return new LagK_Command ();
	}
    else if ( commandName.equalsIgnoreCase("ListFiles") ) {
        return new ListFiles_Command ();
    }
    else if ( commandName.equalsIgnoreCase("LookupTimeSeriesFromTable") ) {
        return new LookupTimeSeriesFromTable_Command ();
    }
	
	// "M" commands...
	
    else if ( commandName.equalsIgnoreCase("ManipulateTableString") ) {
        return new ManipulateTableString_Command ();
    }
	else if ( commandName.equalsIgnoreCase("MergeListFileColumns") ) {
		return new MergeListFileColumns_Command ();
	}
    else if ( commandName.equalsIgnoreCase("Message") ) {
        return new Message_Command ();
    }
    else if ( commandName.equalsIgnoreCase("Multiply") ) {
        return new Multiply_Command ();
    }

	// "N" commands...

    else if ( commandName.equalsIgnoreCase("NewDayTSFromMonthAndDayTS") ) {
        return new NewDayTSFromMonthAndDayTS_Command ();
    }
    else if ( commandName.equalsIgnoreCase("NewDerbyDatabase") ) {
        return new NewDerbyDatabase_Command ();
    }
    else if ( commandName.equalsIgnoreCase("NewEndOfMonthTSFromDayTS") ) {
        return new NewEndOfMonthTSFromDayTS_Command ();
    }
    else if ( commandName.equalsIgnoreCase("NewEnsemble") ) {
        return new NewEnsemble_Command ();
    }
    else if ( commandName.equalsIgnoreCase("NewExcelWorkbook") ) {
        return new NewExcelWorkbook_Command ();
    }
    else if ( commandName.equalsIgnoreCase("NewPatternTimeSeries") ) {
        return new NewPatternTimeSeries_Command ();
    }
    else if ( commandName.equalsIgnoreCase("NewStatisticEnsemble") ) {
        return new NewStatisticEnsemble_Command ();
    }
    else if ( commandName.equalsIgnoreCase("NewStatisticMonthTimeSeries") ) {
        return new NewStatisticMonthTimeSeries_Command ();
    }
    else if ( commandName.equalsIgnoreCase("NewStatisticTimeSeriesFromEnsemble") ) {
        return new NewStatisticTimeSeriesFromEnsemble_Command ();
    }
	else if ( commandName.equalsIgnoreCase("NewStatisticTimeSeries") ) {
		return new NewStatisticTimeSeries_Command ();
	}
	else if ( commandName.equalsIgnoreCase("NewStatisticYearTS") ) {
		return new NewStatisticYearTS_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(commandString,"NewTable") ) {
        return new NewTable_Command ();
    }
	else if ( commandName.equalsIgnoreCase("NewTimeSeries") ) {
		return new NewTimeSeries_Command ();
	}
    else if ( commandName.equalsIgnoreCase("NewTreeView") ) {
        return new NewTreeView_Command ();
    }
    else if ( commandName.equalsIgnoreCase("Normalize") ) {
        return new Normalize_Command ();
    }

	// "O" commands...

	else if ( commandName.equalsIgnoreCase("OpenHydroBase") ) {
		return new OpenHydroBase_Command ();
	}

	// "P" commands...

    else if ( commandName.equalsIgnoreCase("PrintTextFile") ) {
        return new PrintTextFile_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ProcessRasterGraph") ) {
        return new ProcessRasterGraph_Command ();
    }
	else if ( commandName.equalsIgnoreCase("ProcessTSProduct") ) {
		return new ProcessTSProduct_Command ();
	}
    else if ( commandName.equalsIgnoreCase("ProfileCommands") ) {
        return new ProfileCommands_Command ();
    }

	// "R" commands...

    else if ( commandName.equalsIgnoreCase("ReadDateValue") ) {
        return new ReadDateValue_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadDelftFewsPiXml") ) {
        return new ReadDelftFewsPiXml_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadDelimitedFile") ) {
        return new ReadDelimitedFile_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadExcelWorkbook") ) {
        return new ReadExcelWorkbook_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadHecDss") ) {
        return new ReadHecDss_Command ();
    }
	else if ( commandName.equalsIgnoreCase("ReadHydroBase") ) {
		return new ReadHydroBase_Command ();
	}
    else if ( commandName.equalsIgnoreCase("ReadMODSIM") ) {
        return new ReadMODSIM_Command ();
    }
	else if ( commandName.equalsIgnoreCase("ReadNDFD") ) {
		//return new readNDFD_Command ();
	}
	else if ( commandName.equalsIgnoreCase("ReadNrcsAwdb") ) {
		return new ReadNrcsAwdb_Command ();
	}
    else if ( commandName.equalsIgnoreCase("ReadNwsCard") ) {
        return new ReadNwsCard_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadNwsrfsFS5Files") ) {
        return new ReadNwsrfsFS5Files_Command();
    }
    else if ( commandName.equalsIgnoreCase("ReadNwsrfsEspTraceEnsemble") ) {
        return new ReadNwsrfsEspTraceEnsemble_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadPatternFile") ) {
        return new ReadPatternFile_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadPropertiesFromExcel") ) {
        return new ReadPropertiesFromExcel_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadPropertiesFromFile") ) {
        return new ReadPropertiesFromFile_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadRccAcis") ) {
        return new ReadRccAcis_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadReclamationHDB") ) {
        return new ReadReclamationHDB_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadReclamationPisces") ) {
        return new ReadReclamationPisces_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadRiversideDB") ) {
        return new ReadRiversideDB_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadRiverWare") ) {
        return new ReadRiverWare_Command ();
    }
    // Put before shorter command name...
    else if ( commandName.equalsIgnoreCase("ReadStateCUB") ) {
        return new ReadStateCUB_Command ();
    }
	else if ( commandName.equalsIgnoreCase("ReadStateCU") ) {
		return new ReadStateCU_Command ();
	}
    // Put before shorter command name...
    else if ( commandName.equalsIgnoreCase("ReadStateModB") ) {
        return new ReadStateModB_Command ();
    }
	else if ( commandName.equalsIgnoreCase("ReadStateMod") ) {
		return new ReadStateMod_Command ();
	}
    else if ( commandName.equalsIgnoreCase("ReadTableCellsFromExcel") ) {
        return new ReadTableCellsFromExcel_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadTableFromDataStore") ) {
        return new ReadTableFromDataStore_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadTableFromDBF") ) {
        return new ReadTableFromDBF_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadTableFromDelimitedFile") ) {
        return new ReadTableFromDelimitedFile_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadTableFromExcel") ) {
        return new ReadTableFromExcel_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadTableFromFixedFormatFile") ) {
        return new ReadTableFromFixedFormatFile_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadTableFromJSON") ) {
        return new ReadTableFromJSON_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadTableFromXML") ) {
        return new ReadTableFromXML_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadTimeSeries") ) {
        return new ReadTimeSeries_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadTimeSeriesFromDataStore") ) {
        return new ReadTimeSeriesFromDataStore_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadTimeSeriesList") ) {
        return new ReadTimeSeriesList_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadUsgsNwis") || commandName.equalsIgnoreCase("ReadUsgsNwisRdb")) {
        // Automatically convert legacy command to new name
        return new ReadUsgsNwisRdb_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadUsgsNwisDaily") ) {
        return new ReadUsgsNwisDaily_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadUsgsNwisGroundwater") ) {
        return new ReadUsgsNwisGroundwater_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadUsgsNwisInstantaneous") ) {
        return new ReadUsgsNwisInstantaneous_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadWaterML") ) {
        return new ReadWaterML_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReadWaterOneFlow") ) {
        return new ReadWaterOneFlow_Command ();
    }
    else if ( commandName.equalsIgnoreCase("RelativeDiff") ) {
        return new RelativeDiff_Command ();
    }
    else if ( commandName.equalsIgnoreCase("RemoveFile") ) {
        return new RemoveFile_Command ();
    }
    else if ( commandName.equalsIgnoreCase("RemoveDataStoreTableRows") ) {
        // Automatically change the name
        return new DeleteDataStoreTableRows_Command ();
    }
    else if ( commandName.equalsIgnoreCase("RemoveTableRowsFromDataStore") ) {
        // Automatically change the name
        return new DeleteDataStoreTableRows_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ReplaceValue") ) {
        return new ReplaceValue_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ResequenceTimeSeriesData") ) {
        return new ResequenceTimeSeriesData_Command ();
    }
	else if ( commandName.equalsIgnoreCase("RunCommands") ) {
		return new RunCommands_Command ();
	}
    else if ( commandName.equalsIgnoreCase("RunDSSUTL") ) {
        return new RunDSSUTL_Command ();
    }
    else if ( commandName.equalsIgnoreCase("RunProgram") ) {
        return new RunProgram_Command ();
    }
    else if ( commandName.equalsIgnoreCase("RunPython") ) {
        return new RunPython_Command ();
    }
    else if ( commandName.equalsIgnoreCase("RunningAverage") ) {
        return new RunningAverage_Command ();
    }
    else if ( commandName.equalsIgnoreCase("RunningStatisticTimeSeries") ) {
        return new RunningStatisticTimeSeries_Command ();
    }
    else if ( commandName.equalsIgnoreCase("RunSql") ) {
        return new RunSql_Command ();
    }

	// "S" commands...

	else if ( commandName.equalsIgnoreCase("Scale") ) {
		return new Scale_Command ();
	}
    else if ( commandName.equalsIgnoreCase("SelectTimeSeries") ) {
        return new SelectTimeSeries_Command ();
    }
    else if ( commandName.equalsIgnoreCase("SetAutoExtendPeriod") ) {
        return new SetAutoExtendPeriod_Command ();
    }
    else if ( commandName.equalsIgnoreCase("SetAveragePeriod") ) {
        return new SetAveragePeriod_Command ();
    }
    // Obsolete SetConst() and SetConstantBefore() will be handled as an unknown command.
    else if ( commandName.equalsIgnoreCase("SetConstant") ) {
        return new SetConstant_Command ();
    }
    else if ( commandName.equalsIgnoreCase("SetDataValue") ) {
        return new SetDataValue_Command ();
    }
    else if ( commandName.equalsIgnoreCase("SetDebugLevel") ) {
        return new SetDebugLevel_Command ();
    }
    else if ( commandName.equalsIgnoreCase("SetEnsembleProperty") ) {
        return new SetEnsembleProperty_Command ();
    }
    else if ( commandName.equalsIgnoreCase("SetExcelCell") ) {
        return new SetExcelCell_Command ();
    }
    else if ( commandName.equalsIgnoreCase("SetExcelWorksheetViewProperties") ) {
        return new SetExcelWorksheetViewProperties_Command ();
    }
    else if ( commandName.equalsIgnoreCase("SetFromTS") ) {
        return new SetFromTS_Command ();
    }
    else if ( commandName.equalsIgnoreCase("SetIgnoreLEZero") ) {
        return new SetIgnoreLEZero_Command ();
    }
    else if ( commandName.equalsIgnoreCase("SetIncludeMissingTS") ) {
        return new SetIncludeMissingTS_Command ();
    }
	else if ( commandName.equalsIgnoreCase("SetInputPeriod") ) {
		return new SetInputPeriod_Command ();
	}
	else if ( commandName.equalsIgnoreCase("SetOutputPeriod") ) {
		return new SetOutputPeriod_Command ();
	}
    else if ( commandName.equalsIgnoreCase("SetOutputYearType") ) {
        return new SetOutputYearType_Command ();
    }
    else if ( commandName.equalsIgnoreCase("SetPatternFile") ) {
        // Automatically convert to ReadPatternFile
        return new ReadPatternFile_Command ();
    }
    // Put this before the shorter SetProperty() to avoid ambiguity.
    else if ( commandName.equalsIgnoreCase("SetPropertyFromNwsrfsAppDefault") ) {
        return new SetPropertyFromNwsrfsAppDefault_Command ();
    }
    // Put this before the shorter SetProperty() to avoid ambiguity.
    else if ( commandName.equalsIgnoreCase("SetPropertyFromTable") ) {
        return new SetPropertyFromTable_Command ();
    }
    else if ( commandName.equalsIgnoreCase("SetProperty") ) {
        return new SetProperty_Command ();
    }
	else if ( commandName.equalsIgnoreCase("SetQueryPeriod") ) {
		// Phasing into new syntax...
		return new SetInputPeriod_Command ();
	}
    else if ( commandName.equalsIgnoreCase("SetTableValues") ) {
        return new SetTableValues_Command ();
    }
    else if ( commandName.equalsIgnoreCase("SetTimeSeriesPropertiesFromTable") ) {
        return new SetTimeSeriesPropertiesFromTable_Command ();
    }
	else if ( commandName.equalsIgnoreCase("SetTimeSeriesProperty") ) {
		return new SetTimeSeriesProperty_Command ();
	}
    else if ( commandName.equalsIgnoreCase("SetTimeSeriesValuesFromLookupTable") ) {
        return new SetTimeSeriesValuesFromLookupTable_Command ();
    }
    else if ( commandName.equalsIgnoreCase("SetTimeSeriesValuesFromTable") ) {
        return new SetTimeSeriesValuesFromTable_Command ();
    }
    else if ( commandName.equalsIgnoreCase("SetMax") || commandName.equalsIgnoreCase("SetToMax") ) {
        // Legacy is "SetMax" so translate on the fly.
        return new SetToMax_Command ();
    }
    else if ( commandName.equalsIgnoreCase("SetToMin") ) {
        return new SetToMin_Command ();
    }
    else if ( commandName.equalsIgnoreCase("SetWarningLevel") ) {
        return new SetWarningLevel_Command ();
    }
    else if ( commandName.equalsIgnoreCase("SetWorkingDir") ) {
        return new SetWorkingDir_Command ();
    }
    else if ( commandName.equalsIgnoreCase("ShiftTimeByInterval") ) {
        return new ShiftTimeByInterval_Command ();
    }
    else if ( commandName.equalsIgnoreCase("SortTable") ) {
        return new SortTable_Command ();
    }
    else if ( commandName.equalsIgnoreCase("SortTimeSeries") ) {
        return new SortTimeSeries_Command ();
    }
    else if ( commandName.equalsIgnoreCase("SplitTableColumn") ) {
        return new SplitTableColumn_Command ();
    }
    else if ( commandName.equalsIgnoreCase("SplitTableRow") ) {
        return new SplitTableRow_Command ();
    }
	else if ( commandName.equalsIgnoreCase("StartLog") ){
		return new StartLog_Command ();
	}
    else if ( commandName.equalsIgnoreCase("StartRegressionTestResultsReport") ){
        return new StartRegressionTestResultsReport_Command ();
    }
    else if ( commandName.equalsIgnoreCase("StateModMax") ){
        return new StateModMax_Command ();
    }
    else if ( commandName.equalsIgnoreCase("Subtract") ) {
        return new Subtract_Command ();
    }
	
	// "T" commands...

    else if ( commandName.equalsIgnoreCase("TableMath") ) {
        return new TableMath_Command ();
    }
    else if ( commandName.equalsIgnoreCase("TableTimeSeriesMath") ) {
        return new TableTimeSeriesMath_Command ();
    }
    else if ( commandName.equalsIgnoreCase("TableToTimeSeries") ) {
        return new TableToTimeSeries_Command ();
    }
    else if ( commandName.equalsIgnoreCase("TestCommand") ) {
		return new TestCommand_Command ();
	}
    else if ( commandName.equalsIgnoreCase("TimeSeriesToTable") ) {
        return new TimeSeriesToTable_Command ();
    }
    
    // "U" commands...
    
    else if ( commandName.equalsIgnoreCase("UnzipFile") ) {
        return new UnzipFile_Command ();
    }
    
    // "V" commands...

    else if ( commandName.equalsIgnoreCase("VariableLagK") ) {
        return new VariableLagK_Command ();
    }

	// "W" commands...

    else if ( commandName.equalsIgnoreCase("Wait") ) {
        return new Wait_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WebGet") ) {
        return new WebGet_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WeightTraces") ) {
        return new WeightTraces_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WriteCheckFile") ) {
        return new WriteCheckFile_Command ();
    }
	else if ( commandName.equalsIgnoreCase("WriteDateValue") ) {
		return new WriteDateValue_Command ();
	}
    else if ( commandName.equalsIgnoreCase("WriteDelftFewsPiXml") ) {
        return new WriteDelftFewsPiXml_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WriteDelimitedFile") ) {
        return new WriteDelimitedFile_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WriteHecDss") ) {
        return new WriteHecDss_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WriteNwsCard")) {
        return new WriteNwsCard_Command();
    }
	else if ( commandName.equalsIgnoreCase("WriteNWSRFSESPTraceEnsemble")) {
		return new WriteNWSRFSESPTraceEnsemble_Command();
	}
    else if ( commandName.equalsIgnoreCase("WritePropertiesToFile") ) {
        return new WritePropertiesToFile_Command ();
    }
	else if ( commandName.equalsIgnoreCase("WriteProperty") ) {
		return new WriteProperty_Command ();
	}
    else if ( commandName.equalsIgnoreCase("WriteReclamationHDB") ) {
        return new WriteReclamationHDB_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WriteRiversideDB") ) {
        return new WriteRiversideDB_Command ();
    }
	else if ( commandName.equalsIgnoreCase("WriteRiverWare") ) {
		return new WriteRiverWare_Command ();
	}
    else if ( commandName.equalsIgnoreCase("WriteSHEF") ) {
        return new WriteSHEF_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WriteStateCU") ) {
        return new WriteStateCU_Command ();
    }
	else if ( commandName.equalsIgnoreCase("WriteStateMod") ) {
		return new WriteStateMod_Command ();
	}
	else if ( commandName.equalsIgnoreCase("WriteSummary") ) {
		return new WriteSummary_Command ();
	}
    else if ( commandName.equalsIgnoreCase("WriteTableToDataStore") ) {
        return new WriteTableToDataStore_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WriteTableToDelimitedFile") ) {
        return new WriteTableToDelimitedFile_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WriteTableToExcel") ) {
        return new WriteTableToExcel_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WriteTableCellsToExcel") ) {
        return new WriteTableCellsToExcel_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WriteTableToGeoJSON") ) {
        return new WriteTableToGeoJSON_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WriteTableToHTML") ) {
        return new WriteTableToHTML_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WriteTableToKml") ) {
        return new WriteTableToKml_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WriteTableToShapefile") ) {
        return new WriteTableToShapefile_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WriteTimeSeriesPropertiesToFile") ) {
        return new WriteTimeSeriesPropertiesToFile_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WriteTimeSeriesProperty") ) {
        return new WriteTimeSeriesProperty_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WriteTimeSeriesToDataStore") ) {
        return new WriteTimeSeriesToDataStore_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WriteTimeSeriesToDataStream") ) {
        return new WriteTimeSeriesToDataStream_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WriteTimeSeriesToExcel") ) {
        return new WriteTimeSeriesToExcel_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WriteTimeSeriesToExcelBlock") ) {
        return new WriteTimeSeriesToExcelBlock_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WriteTimeSeriesToHydroJSON") ) {
        return new WriteTimeSeriesToHydroJSON_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WriteTimeSeriesToJson") ) {
        return new WriteTimeSeriesToJson_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WriteTimeSeriesToGeoJSON") ) {
        return new WriteTimeSeriesToGeoJSON_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WriteTimeSeriesToKml") ) {
        return new WriteTimeSeriesToKml_Command ();
    }
    else if ( commandName.equalsIgnoreCase("WriteWaterML") ) {
        return new WriteWaterML_Command ();
    }
    
    // Check for time series identifier
    // This is the fall through if no command was matched above but the string matches a TSID pattern
    
	if ( TSCommandProcessorUtil.isTSID(commandString) ) {
	    return new TSID_Command ();
	}

	// Check for blank line, which will result in Empty command
	
    if ( commandName.equalsIgnoreCase("") ) {
        return new Empty_Command ();
    }

	// Did not match a command or TSID...

	if ( createUnknownCommandIfNotRecognized ) {
		// Create an unknown command...
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