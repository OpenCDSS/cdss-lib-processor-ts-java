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

// Colorado IPP commands

import rti.tscommandprocessor.commands.ipp.ReadColoradoIPP_Command;

// Data checks (for now keep separate from logging and data tests)
import rti.tscommandprocessor.commands.check.CheckTimeSeries_Command;
import rti.tscommandprocessor.commands.check.OpenCheckFile_Command;
import rti.tscommandprocessor.commands.check.WriteCheckFile_Command;

// DataTest commands
// FIXME SAM 2007-08-30 Need to work with Ian to pull in new data test features

import rti.tscommandprocessor.commands.datatest.NewDataTest_Command;
import rti.tscommandprocessor.commands.datatest.RunDataTest_Command;

// DateValue commands

import rti.tscommandprocessor.commands.datevalue.ReadDateValue_Command;
import rti.tscommandprocessor.commands.datevalue.WriteDateValue_Command;

// Delimited time series file commands

import rti.tscommandprocessor.commands.delimited.ReadDelimitedFile_Command;

// GRTS commands (time series products).

import rti.tscommandprocessor.commands.products.ProcessTSProduct_Command;

//HEC-DSS commands.

import rti.tscommandprocessor.commands.hecdss.ReadHecDss_Command;
import rti.tscommandprocessor.commands.hecdss.WriteHecDss_Command;

// HydroBase commands.

import rti.tscommandprocessor.commands.hydrobase.FillUsingDiversionComments_Command;
import rti.tscommandprocessor.commands.hydrobase.OpenHydroBase_Command;
import rti.tscommandprocessor.commands.hydrobase.ReadHydroBase_Command;

// Logging commands.

import rti.tscommandprocessor.commands.logging.SetDebugLevel_Command;
import rti.tscommandprocessor.commands.logging.SetWarningLevel_Command;
import rti.tscommandprocessor.commands.logging.StartLog_Command;

// MODSIM commands

import rti.tscommandprocessor.commands.modsim.ReadMODSIM_Command;

// NWSRFS commands.

import rti.tscommandprocessor.commands.nwsrfs.ReadNwsCard_Command;
import rti.tscommandprocessor.commands.nwsrfs.ReadNwsrfsEspTraceEnsemble_Command;
import rti.tscommandprocessor.commands.nwsrfs.ReadNwsrfsFS5Files_Command;
import rti.tscommandprocessor.commands.nwsrfs.SetPropertyFromNwsrfsAppDefault_Command;
import rti.tscommandprocessor.commands.nwsrfs.WriteNwsCard_Command;
import rti.tscommandprocessor.commands.nwsrfs.WriteNWSRFSESPTraceEnsemble_Command;

// RiverWare commands

import rti.tscommandprocessor.commands.riverware.ReadRiverWare_Command;
import rti.tscommandprocessor.commands.riverware.WriteRiverWare_Command;

// SHEF commands.

import rti.tscommandprocessor.commands.shef.WriteSHEF_Command;

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

import rti.tscommandprocessor.commands.table.ReadTableFromDelimitedFile_Command;

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
import rti.tscommandprocessor.commands.ts.ChangeInterval_Command;
import rti.tscommandprocessor.commands.ts.ChangePeriod_Command;
import rti.tscommandprocessor.commands.ts.CompareTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.ComputeErrorTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.ConvertDataUnits_Command;
import rti.tscommandprocessor.commands.ts.Copy_Command;
import rti.tscommandprocessor.commands.ts.CopyEnsemble_Command;
import rti.tscommandprocessor.commands.ts.CreateEnsemble_Command;
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
import rti.tscommandprocessor.commands.ts.FillMOVE2_Command;
import rti.tscommandprocessor.commands.ts.FillPattern_Command;
import rti.tscommandprocessor.commands.ts.FillProrate_Command;
import rti.tscommandprocessor.commands.ts.FillRegression_Command;
import rti.tscommandprocessor.commands.ts.FillRepeat_Command;
import rti.tscommandprocessor.commands.ts.Free_Command;
//import rti.tscommandprocessor.commands.ts.FreeEnsemble_Command;
import rti.tscommandprocessor.commands.ts.LagK_Command;
import rti.tscommandprocessor.commands.ts.Multiply_Command;
import rti.tscommandprocessor.commands.ts.NewDayTSFromMonthAndDayTS_Command;
import rti.tscommandprocessor.commands.ts.NewEndOfMonthTSFromDayTS_Command;
import rti.tscommandprocessor.commands.ts.NewStatisticTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.NewStatisticTimeSeriesFromEnsemble_Command;
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
import rti.tscommandprocessor.commands.ts.SetToMax_Command;
import rti.tscommandprocessor.commands.ts.SetToMin_Command;
import rti.tscommandprocessor.commands.ts.ShiftTimeByInterval_Command;
import rti.tscommandprocessor.commands.ts.SortTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.Subtract_Command;
import rti.tscommandprocessor.commands.ts.TSID_Command;
import rti.tscommandprocessor.commands.ts.VariableLagK_Command;
import rti.tscommandprocessor.commands.ts.WeightTraces_Command;
import rti.tscommandprocessor.commands.ts.WriteTimeSeriesProperty_Command;

// USGS commands

import rti.tscommandprocessor.commands.usgs.ReadUsgsNwis_Command;

// Utility commands.

import rti.tscommandprocessor.commands.util.Comment_Command;
import rti.tscommandprocessor.commands.util.CommentBlockStart_Command;
import rti.tscommandprocessor.commands.util.CommentBlockEnd_Command;
import rti.tscommandprocessor.commands.util.CompareFiles_Command;
import rti.tscommandprocessor.commands.util.CreateRegressionTestCommandFile_Command;
import rti.tscommandprocessor.commands.util.Exit_Command;
import rti.tscommandprocessor.commands.util.FTPGet_Command;
import rti.tscommandprocessor.commands.util.MergeListFileColumns_Command;
import rti.tscommandprocessor.commands.util.RemoveFile_Command;
import rti.tscommandprocessor.commands.util.RunCommands_Command;
import rti.tscommandprocessor.commands.util.RunDSSUTL_Command;
import rti.tscommandprocessor.commands.util.RunProgram_Command;
import rti.tscommandprocessor.commands.util.RunPython_Command;
import rti.tscommandprocessor.commands.util.SetProperty_Command;
import rti.tscommandprocessor.commands.util.SetWorkingDir_Command;
import rti.tscommandprocessor.commands.util.StartRegressionTestResultsReport_Command;
import rti.tscommandprocessor.commands.util.TestCommand_Command;
import rti.tscommandprocessor.commands.util.WriteProperty_Command;

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
{	commandString = commandString.trim();
    String routine = "TSCommandFactory.newCommand";

	// Parse out arguments for TS alias = foo() commands to be able to handle nulls here

	boolean isTScommand = false;	// Whether TS alias = foo() command.
	boolean isDataTest_command = false;	// Whether DataTest alias = foo() command.
	String TScommand = "";			// Don't use null, to allow string compares below
	String DataTest_command = "";
	String token0 = StringUtil.getToken(commandString,"( =",StringUtil.DELIM_SKIP_BLANKS,0);
	if ( (token0 != null) && token0.equalsIgnoreCase( "TS") ) {
		isTScommand = true;
		TScommand = StringUtil.getToken(commandString,"( =",StringUtil.DELIM_SKIP_BLANKS,2);
		if ( TScommand == null ) {
			TScommand = "";
		}
	}
	else if ( (token0 != null) && token0.equalsIgnoreCase( "DataTest") ) {
		isDataTest_command = true;
		DataTest_command = StringUtil.getToken(commandString,"( =",StringUtil.DELIM_SKIP_BLANKS,2);
		if ( DataTest_command == null ) {
			DataTest_command = "";
		}
	}
	
	// Comment commands...
	
    if ( commandString.trim().startsWith("#") ) {
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
    else if ( StringUtil.startsWithIgnoreCase(commandString,"AddConstant") ) {
        return new AddConstant_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"Add") ) {
        return new Add_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"AdjustExtremes") ) {
        return new AdjustExtremes_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"AnalyzePattern") ) {
		return new AnalyzePattern_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(commandString,"ARMA") ) {
        return new ARMA_Command ();
    }
    
    // "B" commands...
    
    else if ( StringUtil.startsWithIgnoreCase(commandString,"Blend") ) {
        return new Blend_Command ();
    }

	// "C" commands...

    else if ( StringUtil.startsWithIgnoreCase(commandString,"ChangePeriod") ) {
        return new ChangePeriod_Command ();
    }
	else if ( isTScommand && TScommand.equalsIgnoreCase("ChangeInterval") ) {
		return new ChangeInterval_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(commandString,"CheckTimeSeries") ) {
        return new CheckTimeSeries_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(commandString,"CompareFiles") ) {
		return new CompareFiles_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(commandString,"CompareTimeSeries") ) {
		return new CompareTimeSeries_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(commandString,"ComputeErrorTimeSeries") ) {
        return new ComputeErrorTimeSeries_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"ConvertDataUnits") ) {
        return new ConvertDataUnits_Command ();
    }
    // Put CopyEnsemble() before the shorter Copy() command
    else if ( StringUtil.startsWithIgnoreCase(commandString,"CopyEnsemble") ) {
        return new CopyEnsemble_Command ();
    }
	else if ( isTScommand && TScommand.equalsIgnoreCase("Copy") ) {
		return new Copy_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(commandString,"CreateEnsemble") ) {
        return new CreateEnsemble_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"CreateFromList") ) {
        return new CreateFromList_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(commandString,"CreateRegressionTestCommandFile") ) {
		return new CreateRegressionTestCommandFile_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(commandString,"Cumulate") ) {
		return new Cumulate_Command ();
	}
    
    // "D" commands...
    
    else if ( StringUtil.startsWithIgnoreCase(commandString,"DeselectTimeSeries") ) {
        return new DeselectTimeSeries_Command ();
    }
    else if ( isTScommand && TScommand.equalsIgnoreCase("Disaggregate") ) {
        return new Disaggregate_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"Divide") ) {
        return new Divide_Command ();
    }
	
	// "E" commands...
	
	else if ( StringUtil.startsWithIgnoreCase(commandString,"Exit") ||
	    StringUtil.startsWithIgnoreCase(commandString,"Exit")) {
		return new Exit_Command ();
	}

	// "F" commands...

	else if ( StringUtil.startsWithIgnoreCase(commandString,"FillConstant") ) {
		return new FillConstant_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(commandString,"FillDayTSFrom2MonthTSAnd1DayTS") ) {
        return new FillDayTSFrom2MonthTSAnd1DayTS_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"FillFromTS") ) {
        return new FillFromTS_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(commandString,"FillHistMonthAverage") ) {
		return new FillHistMonthAverage_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(commandString,"FillHistYearAverage") ) {
		return new FillHistYearAverage_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(commandString,"FillInterpolate") ) {
        return new FillInterpolate_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(commandString,"FillMixedStation") ) {
		return new FillMixedStation_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(commandString,"FillMOVE2") ) {
		return new FillMOVE2_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(commandString,"FillPattern") ) {
        return new FillPattern_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"FillProrate") ) {
        return new FillProrate_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(commandString,"FillRegression") ) {
		return new FillRegression_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(commandString,"FillRepeat") ) {
        return new FillRepeat_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(commandString,"FillUsingDiversionComments") ) {
		return new FillUsingDiversionComments_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(commandString,"Free(") ||
            StringUtil.startsWithIgnoreCase(commandString,"Free (")) {
        return new Free_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"FTPGet") ) {
        return new FTPGet_Command ();
    }
    /*
    else if ( StringUtil.startsWithIgnoreCase(command_string,"FreeEnsemble") ||
            StringUtil.startsWithIgnoreCase(command_string,"FreeEnsemble")) {
        return new FreeEnsemble_Command ();
    }
    */
	
	// "L" commands...

	else if ( isTScommand && TScommand.equalsIgnoreCase("LagK") ) {
		return new LagK_Command ();
	}
	
	// "M" commands...
	
	else if ( StringUtil.startsWithIgnoreCase(commandString,"MergeListFileColumns") ) {
		return new MergeListFileColumns_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(commandString,"Multiply") ) {
        return new Multiply_Command ();
    }

	// "N" commands...

	else if ( isDataTest_command && DataTest_command.equalsIgnoreCase("NewDataTest") ) {
		return new NewDataTest_Command();
	}
    else if ( isTScommand && TScommand.equalsIgnoreCase("NewDayTSFromMonthAndDayTS") ) {
        return new NewDayTSFromMonthAndDayTS_Command ();
    }
    else if ( isTScommand && TScommand.equalsIgnoreCase("NewEndOfMonthTSFromDayTS") ) {
        return new NewEndOfMonthTSFromDayTS_Command ();
    }
    // Put the following before the shorter NewStatisticTimeSeries() command.
    else if ( isTScommand && TScommand.equalsIgnoreCase("NewStatisticTimeSeriesFromEnsemble") ) {
        return new NewStatisticTimeSeriesFromEnsemble_Command ();
    }
	else if ( isTScommand && TScommand.equalsIgnoreCase("NewStatisticTimeSeries") ) {
		return new NewStatisticTimeSeries_Command ();
	}
	else if ( isTScommand && TScommand.equalsIgnoreCase("NewStatisticYearTS") ) {
		return new NewStatisticYearTS_Command ();
	}
	else if ( isTScommand && TScommand.equalsIgnoreCase("NewPatternTimeSeries") ) {
		return new NewPatternTimeSeries_Command ();
	}
	else if ( isTScommand && TScommand.equalsIgnoreCase("NewTimeSeries") ) {
		return new NewTimeSeries_Command ();
	}
    else if ( isTScommand && TScommand.equalsIgnoreCase("Normalize") ) {
        return new Normalize_Command ();
    }

	// "O" commands...

    else if ( StringUtil.startsWithIgnoreCase(commandString,"OpenCheckFile") ) {
        return new OpenCheckFile_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(commandString,"OpenHydroBase") ) {
		return new OpenHydroBase_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(commandString,"OpenNDFD") ) {
		//return new openNDFD_Command ();
	}

	// "P" commands...

	else if ( StringUtil.startsWithIgnoreCase(commandString,"ProcessTSProduct") ) {
		return new ProcessTSProduct_Command ();
	}

	// "R" commands...

    else if ( StringUtil.startsWithIgnoreCase(commandString,"ReadColoradoIPP") ) {
        return new ReadColoradoIPP_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"ReadDateValue") ) {
        return new ReadDateValue_Command ();
    }
    else if ( isTScommand && TScommand.equalsIgnoreCase("ReadDateValue") ) {
        return new ReadDateValue_Command();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"ReadDelimitedFile") ) {
        return new ReadDelimitedFile_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"ReadHecDss") ) {
        return new ReadHecDss_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(commandString,"ReadHydroBase") ) {
		return new ReadHydroBase_Command ();
	}
	else if ( isTScommand && TScommand.equalsIgnoreCase("ReadHydroBase") ) {
		return new ReadHydroBase_Command();
	}
    else if ( StringUtil.startsWithIgnoreCase(commandString,"ReadMODSIM") ) {
        return new ReadMODSIM_Command ();
    }
    else if ( isTScommand && TScommand.equalsIgnoreCase("ReadMODSIM") ) {
        return new ReadMODSIM_Command();
    }
	else if ( isTScommand && TScommand.equalsIgnoreCase("ReadNDFD") ) {
		//return new readNDFD_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(commandString,"ReadNwsCard") ) {
		return new ReadNwsCard_Command ();
	}
	else if ( isTScommand && TScommand.equalsIgnoreCase("ReadNwsCard") ) {
		return new ReadNwsCard_Command();
	}
    else if ( isTScommand && TScommand.equalsIgnoreCase("ReadNwsrfsFS5Files") ) {
        return new ReadNwsrfsFS5Files_Command();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"ReadNwsrfsEspTraceEnsemble") ) {
        return new ReadNwsrfsEspTraceEnsemble_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"ReadPatternFile") ) {
        return new ReadPatternFile_Command ();
    }
    else if ( isTScommand && TScommand.equalsIgnoreCase("ReadRiverWare") ) {
        return new ReadRiverWare_Command ();
    }
    // Put before shorter command name...
    else if ( StringUtil.startsWithIgnoreCase(commandString,"ReadStateCUB") ) {
        return new ReadStateCUB_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(commandString,"ReadStateCU") ) {
		return new ReadStateCU_Command ();
	}
    // Put before shorter command name...
    else if ( StringUtil.startsWithIgnoreCase(commandString,"ReadStateModB") ) {
        return new ReadStateModB_Command ();
    }
    else if ( isTScommand && TScommand.equalsIgnoreCase("ReadStateMod") ) {
        return new ReadStateMod_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(commandString,"ReadStateMod") ) {
		return new ReadStateMod_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(commandString,"ReadTableFromDelimitedFile") ) {
        return new ReadTableFromDelimitedFile_Command ();
    }
    else if ( isTScommand && TScommand.equalsIgnoreCase("ReadTimeSeries") ) {
        return new ReadTimeSeries_Command ();
    }
    else if ( isTScommand && TScommand.equalsIgnoreCase("ReadUsgsNwis") ) {
        return new ReadUsgsNwis_Command ();
    }
    else if ( isTScommand && TScommand.equalsIgnoreCase("RelativeDiff") ) {
        return new RelativeDiff_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"RemoveFile") ) {
        return new RemoveFile_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"ReplaceValue") ) {
        return new ReplaceValue_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"ResequenceTimeSeriesData") ) {
        return new ResequenceTimeSeriesData_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(commandString,"RunCommands") ) {
		return new RunCommands_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(commandString,"RunDSSUTL") ) {
        return new RunDSSUTL_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"RunProgram") ) {
        return new RunProgram_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"RunPython") ) {
        return new RunPython_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(commandString,"RunDataTest") ) {
		return new RunDataTest_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(commandString,"RunningAverage") ) {
        return new RunningAverage_Command ();
    }

	// "S" commands...

	else if ( StringUtil.startsWithIgnoreCase(commandString,"Scale") ) {
		return new Scale_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(commandString,"SelectTimeSeries") ) {
        return new SelectTimeSeries_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"SetAutoExtendPeriod") ) {
        return new SetAutoExtendPeriod_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"SetAveragePeriod") ) {
        return new SetAveragePeriod_Command ();
    }
    // Do a check for the obsolete SetConst() and SetConstantBefore().
    // If encountered, they will be handled as an unknown command.
    else if ( StringUtil.startsWithIgnoreCase(commandString,"SetConstant") &&
            !StringUtil.startsWithIgnoreCase(commandString,"SetConstantBefore") &&
            !StringUtil.startsWithIgnoreCase(commandString,"SetConst ") &&
            !StringUtil.startsWithIgnoreCase(commandString,"SetConst(") ) {
        return new SetConstant_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"SetDataValue") ) {
        return new SetDataValue_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"SetDebugLevel") ) {
        return new SetDebugLevel_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"SetFromTS") ) {
        return new SetFromTS_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"SetIgnoreLEZero") ) {
        return new SetIgnoreLEZero_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"SetIncludeMissingTS") ) {
        return new SetIncludeMissingTS_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(commandString,"SetInputPeriod") ) {
		return new SetInputPeriod_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(commandString,"SetOutputPeriod") ) {
		return new SetOutputPeriod_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(commandString,"SetOutputYearType") ) {
        return new SetOutputYearType_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"SetPatternFile") ) {
        // Automatically convert to ReadPatternFile
        return new ReadPatternFile_Command ();
    }
    // Put this before the shorter SetProperty() to avoid ambiguity.
    else if ( StringUtil.startsWithIgnoreCase(commandString,"SetPropertyFromNwsrfsAppDefault") ) {
        return new SetPropertyFromNwsrfsAppDefault_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"SetProperty") ) {
        return new SetProperty_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(commandString,"SetQueryPeriod") ) {
		// Phasing into new syntax...
		return new SetInputPeriod_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(commandString,"SetTimeSeriesProperty") ) {
		return new SetTimeSeriesProperty_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(commandString,"SetMax") ||
            StringUtil.startsWithIgnoreCase(commandString,"SetToMax") ) {
        // Legacy is "SetMax" so translate on the fly.
        return new SetToMax_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"SetToMin") ) {
        return new SetToMin_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"SetWarningLevel") ) {
        return new SetWarningLevel_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"SetWorkingDir") ) {
        return new SetWorkingDir_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"ShiftTimeByInterval") ) {
        return new ShiftTimeByInterval_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"SortTimeSeries") ) {
        return new SortTimeSeries_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase( commandString,"StartLog") ){
		return new StartLog_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase( commandString,"StartRegressionTestResultsReport") ){
        return new StartRegressionTestResultsReport_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase( commandString,"StateModMax") ){
        return new StateModMax_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"Subtract") ) {
        return new Subtract_Command ();
    }
	
	// "T" commands...

	else if ( StringUtil.startsWithIgnoreCase(commandString,"TestCommand") ) {
		return new TestCommand_Command ();
	}
    
    // "V" commands...

    else if ( StringUtil.startsWithIgnoreCase(commandString,"VariableLagK") ) {
        return new VariableLagK_Command ();
    }

	// "W" commands...

    else if ( isTScommand && TScommand.equalsIgnoreCase("WeightTraces") ) {
        return new WeightTraces_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"WriteCheckFile") ) {
        return new WriteCheckFile_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(commandString,"WriteDateValue") ) {
		return new WriteDateValue_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(commandString,"WriteHecDss") ) {
        return new WriteHecDss_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString, "WriteNwsCard")) {
        return new WriteNwsCard_Command();
    }
	else if ( StringUtil.startsWithIgnoreCase(commandString, "WriteNWSRFSESPTraceEnsemble")) {
		return new WriteNWSRFSESPTraceEnsemble_Command();
	}
	else if ( StringUtil.startsWithIgnoreCase(commandString,"WriteProperty") ) {
		return new WriteProperty_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(commandString,"WriteRiverWare") ) {
		return new WriteRiverWare_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(commandString,"WriteSHEF") ) {
        return new WriteSHEF_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(commandString,"WriteStateCU") ) {
        return new WriteStateCU_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(commandString,"WriteStateMod") ) {
		return new WriteStateMod_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(commandString,"WriteSummary") ) {
		return new WriteSummary_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(commandString,"WriteTimeSeriesProperty") ) {
        return new WriteTimeSeriesProperty_Command ();
    }
    
    // Check for time series identifier.
    
	else if ( TSCommandProcessorUtil.isTSID(commandString) ) {
	    return new TSID_Command ();
	}

	// Did not match a command...

	if ( createUnknownCommandIfNotRecognized ) {
		// Create an unknown command...
		Command c = new UnknownCommand ();
		c.setCommandString( commandString );
        Message.printStatus ( 2, routine, "Creating UnknownCommand for unknown command \"" +
                commandString + "\"" );
		return c;
	}
	else {
		// Throw an exception if the command is not recognized.
		throw new UnknownCommandException ( "Unknown command \"" + commandString + "\"" );
	}
}

}
