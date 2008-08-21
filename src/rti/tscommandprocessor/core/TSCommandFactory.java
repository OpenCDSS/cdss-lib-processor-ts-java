//------------------------------------------------------------------------------
// TSCommandFactory - a factory to create Commands given a command name
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2005-04-29	Steven A. Malers, RTi	Initial version with fillRegression.
// 2005-05-09	SAM, RTi		Add sortTimeSeries.
// 2005-05-10	SAM, RTi		Add compareTimeSeries.
// 2005-05-13	SAM, RTi		Add startLog.
// 2005-05-15	SAM, RTi		Add readNWSCard.
// 2005-05-17	SAM, RTi		Add fillHistMonthAverage.
// 2005-05-18	SAM, RTi		Add fillHistYearAverage.
// 2005-05-19	SAM, RTi		Move from TSTool package.
// 2005-05-23	SAM, RTi		Add analyzePattern.
// 2005-05-25	SAM, RTi		Add changeInterval.
// 2005-05-25	Luiz Teixeira, RTi	Add fillMixedStation.
// 2005-05-30	SAM, RTi		Add writeRiverWare.
// 2005-06-08	SAM, RTi		Add openHydroBase.
// 2005-07-11	SAM, RTi		Add lagK.
// 2005-08-24	SAM, RTi		Add scale.
// 2005-08-25	SAM, RTi		Add copy.
// 2005-08-30	SAM, RTi		Add writeStateMod.  This depends on the
//					DWR.StateMod package.
// 2005-09-02	SAM, RTi		Add readStateMod.  This depends on the
//					DWR.StateMod package.
// 2005-09-08	SAM, RTi		Add fillConstant.
//					Add newStatisticYearTS.
// 2005-09-20	SAM, RTi		Add newTimeSeries.
// 2005-09-29	SAM, RTi		Add cumulate, readStateModB.
// 2005-10-18	SAM, RTi		Add processTSProduct.
// 2005-12-06	J. Thomas Sapienza, RTi	Add TS Alias = readNwsCard().
// 2005-12-14	SAM, RTi		Add setInputPeriod.
// 2006-01-17	JTS, RTi		Add writeNWSRFSESPTraceEnsemble.
// 2006-01-18	JTS, RTi		readNWSCard_* moved into the
//					RTi.DMI.NWSRFS_DMI package.
// 2006-01-31	SAM, RTi		Add TS Alias = readNDFD() and
//					openNDFD() commands (dummy in
//					until the code is ready).
// 2006-04-13	SAM, RTi		Add fillMOVE2().
// 2006-04-19	SAM, RTi		Add compareFiles().
// 2006-04-21	SAM, RTi		Add readHydroBase().
// 2006-05-02	SAM, RTi		Add runCommands().
//------------------------------------------------------------------------------
// EndHeader

package rti.tscommandprocessor.core;

// TODO SAM 2005-07-10 Need to evaluate where command classes should live to
// avoid mixing with lower-level code.
//import RTi.DataServices.Adapter.NDFD.openNDFD_Command;
//import RTi.DataServices.Adapter.NDFD.readNDFD_Command;

import RTi.Util.IO.Command;
import RTi.Util.IO.CommandFactory;
import RTi.Util.IO.GenericCommand;
import RTi.Util.IO.UnknownCommandException;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

// Data checks (for now keep separate from logging and data tests)
import rti.tscommandprocessor.commands.check.OpenCheckFile_Command;

// DataTest commands
// FIXME SAM 2007-08-30 Need to work with Ian to pull in new data test features

import rti.tscommandprocessor.commands.datatest.newDataTest_Command;
import rti.tscommandprocessor.commands.datatest.runDataTest_Command;

// DateValue commands

import rti.tscommandprocessor.commands.datevalue.ReadDateValue_Command;
import rti.tscommandprocessor.commands.datevalue.WriteDateValue_Command;

// Delimited time series file commands

import rti.tscommandprocessor.commands.delimited.ReadDelimitedFile_Command;

// GRTS commands (time series products).

import rti.tscommandprocessor.commands.products.processTSProduct_Command;

// HydroBase commands.

import rti.tscommandprocessor.commands.hydrobase.fillUsingDiversionComments_Command;
import rti.tscommandprocessor.commands.hydrobase.openHydroBase_Command;
import rti.tscommandprocessor.commands.hydrobase.readHydroBase_Command;

// Logging commands.

import rti.tscommandprocessor.commands.logging.SetDebugLevel_Command;
import rti.tscommandprocessor.commands.logging.SetWarningLevel_Command;
import rti.tscommandprocessor.commands.logging.startLog_Command;

// NWSRFS commands.

import rti.tscommandprocessor.commands.nwsrfs.readNwsCard_Command;
import rti.tscommandprocessor.commands.nwsrfs.ReadNwsrfsEspTraceEnsemble_Command;
import rti.tscommandprocessor.commands.nwsrfs.SetPropertyFromNwsrfsAppDefault_Command;
import rti.tscommandprocessor.commands.nwsrfs.WriteNwsCard_Command;
import rti.tscommandprocessor.commands.nwsrfs.writeNWSRFSESPTraceEnsemble_Command;

// RiverWare commands

import rti.tscommandprocessor.commands.riverware.writeRiverWare_Command;

// SHEF commands.

import rti.tscommandprocessor.commands.shef.WriteSHEF_Command;

// StateCU commands.

import rti.tscommandprocessor.commands.statecu.readStateCU_Command;

// StateMod commands.

import rti.tscommandprocessor.commands.statemod.writeStateMod_Command;
import rti.tscommandprocessor.commands.statemod.readStateMod_Command;
import rti.tscommandprocessor.commands.statemod.readStateModB_Command;

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
import rti.tscommandprocessor.commands.ts.analyzePattern_Command;
import rti.tscommandprocessor.commands.ts.Blend_Command;
import rti.tscommandprocessor.commands.ts.changeInterval_Command;
import rti.tscommandprocessor.commands.ts.ChangePeriod_Command;
import rti.tscommandprocessor.commands.ts.compareTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.ComputeErrorTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.ConvertDataUnits_Command;
import rti.tscommandprocessor.commands.ts.copy_Command;
import rti.tscommandprocessor.commands.ts.CopyEnsemble_Command;
import rti.tscommandprocessor.commands.ts.CreateEnsemble_Command;
import rti.tscommandprocessor.commands.ts.cumulate_Command;
import rti.tscommandprocessor.commands.ts.DeselectTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.fillConstant_Command;
import rti.tscommandprocessor.commands.ts.FillFromTS_Command;
import rti.tscommandprocessor.commands.ts.fillHistMonthAverage_Command;
import rti.tscommandprocessor.commands.ts.fillHistYearAverage_Command;
import rti.tscommandprocessor.commands.ts.fillMixedStation_Command;
import rti.tscommandprocessor.commands.ts.fillMOVE2_Command;
import rti.tscommandprocessor.commands.ts.fillRegression_Command;
import rti.tscommandprocessor.commands.ts.FillRepeat_Command;
import rti.tscommandprocessor.commands.ts.Free_Command;
//import rti.tscommandprocessor.commands.ts.FreeEnsemble_Command;
import rti.tscommandprocessor.commands.ts.lagK_Command;
import rti.tscommandprocessor.commands.ts.NewStatisticTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.NewStatisticTimeSeriesFromEnsemble_Command;
import rti.tscommandprocessor.commands.ts.newStatisticYearTS_Command;
import rti.tscommandprocessor.commands.ts.NewPatternTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.newTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.ResequenceTimeSeriesData_Command;
import rti.tscommandprocessor.commands.ts.RunningAverage_Command;
import rti.tscommandprocessor.commands.ts.scale_Command;
import rti.tscommandprocessor.commands.ts.SelectTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.SetAutoExtendPeriod_Command;
import rti.tscommandprocessor.commands.ts.SetConstant_Command;
import rti.tscommandprocessor.commands.ts.SetFromTS_Command;
import rti.tscommandprocessor.commands.ts.SetIncludeMissingTS_Command;
import rti.tscommandprocessor.commands.ts.SetIgnoreLEZero_Command;
import rti.tscommandprocessor.commands.ts.SetTimeSeriesProperty_Command;
import rti.tscommandprocessor.commands.ts.ShiftTimeByInterval_Command;
import rti.tscommandprocessor.commands.ts.sortTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.Subtract_Command;
import rti.tscommandprocessor.commands.ts.WeightTraces_Command;
import rti.tscommandprocessor.commands.ts.WriteTimeSeriesProperty_Command;

// Utility commands.

import rti.tscommandprocessor.commands.util.compareFiles_Command;
import rti.tscommandprocessor.commands.util.CreateRegressionTestCommandFile_Command;
import rti.tscommandprocessor.commands.util.mergeListFileColumns_Command;
import rti.tscommandprocessor.commands.util.RemoveFile_Command;
import rti.tscommandprocessor.commands.util.FTPGet_Command;
import rti.tscommandprocessor.commands.util.runCommands_Command;
import rti.tscommandprocessor.commands.util.RunProgram_Command;
import rti.tscommandprocessor.commands.util.RunPython_Command;
import rti.tscommandprocessor.commands.util.SetProperty_Command;
import rti.tscommandprocessor.commands.util.SetWorkingDir_Command;
import rti.tscommandprocessor.commands.util.StartRegressionTestResultsReport_Command;
import rti.tscommandprocessor.commands.util.testCommand_Command;
import rti.tscommandprocessor.commands.util.WriteProperty_Command;

/**
This class instantiates Commands for time series processing.
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
Return a new time series command, based on the command name.  DO NOT create a
GenericCommand if the command is not recognized.
@return a new time series command, based on the command name.
@param command_string The command string to process.
@throws UnknownCommandException if the command name is not recognized.
*/
public Command newCommand ( String command_string )
throws UnknownCommandException
{
	return newCommand ( command_string, false );
}

/**
Return a new time series command, based on the command name.
@return a new time series command, based on the command name.
@param command_string The command string to process.
@param create_generic_command_if_not_recognized If true and the command is
not recognized, create a GenericCommand instance that holds the command string.
This is useful for code that is being migrated to the full command class design.
@throws UnknownCommandException if the command name is not recognized.
*/
public Command newCommand ( String command_string, boolean create_generic_command_if_not_recognized )
throws UnknownCommandException
{	command_string = command_string.trim();
    String routine = "TSCommandFactory.newCommand";

	// Parse out arguments for TS alias = foo() commands to be able to handle nulls here

	boolean isTScommand = false;	// Whether TS alias = foo() command.
	boolean isDataTest_command = false;	// Whether DataTest alias = foo() command.
	String TScommand = "";			// Don't use null, to allow string compares below
	String DataTest_command = "";
	String token0 = StringUtil.getToken(command_string,"( =",StringUtil.DELIM_SKIP_BLANKS,0);
	if ( (token0 != null) && token0.equalsIgnoreCase( "TS") ) {
		isTScommand = true;
		TScommand = StringUtil.getToken(command_string,"( =",StringUtil.DELIM_SKIP_BLANKS,2);
		if ( TScommand == null ) {
			TScommand = "";
		}
	}
	else if ( (token0 != null) && token0.equalsIgnoreCase( "DataTest") ) {
		isDataTest_command = true;
		DataTest_command = StringUtil.getToken(command_string,"( =",StringUtil.DELIM_SKIP_BLANKS,2);
		if ( DataTest_command == null ) {
			DataTest_command = "";
		}
	}

	// "A" commands...

    // Put the following before "Add"
    if ( StringUtil.startsWithIgnoreCase(command_string,"AddConstant") ) {
        return new AddConstant_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(command_string,"Add") ) {
        return new Add_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(command_string,"AnalyzePattern") ) {
		return new analyzePattern_Command ();
	}
    
    // "B" commands...
    
    else if ( StringUtil.startsWithIgnoreCase(command_string,"Blend") ) {
        return new Blend_Command ();
    }

	// "C" commands...

    else if ( StringUtil.startsWithIgnoreCase(command_string,"ChangePeriod") ) {
        return new ChangePeriod_Command ();
    }
	else if ( isTScommand && TScommand.equalsIgnoreCase("ChangeInterval") ) {
		return new changeInterval_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"CompareFiles") ) {
		return new compareFiles_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"CompareTimeSeries") ) {
		return new compareTimeSeries_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(command_string,"ComputeErrorTimeSeries") ) {
        return new ComputeErrorTimeSeries_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(command_string,"ConvertDataUnits") ) {
        return new ConvertDataUnits_Command ();
    }
    // Put CopyEnsemble() before the shorter Copy() command
    else if ( StringUtil.startsWithIgnoreCase(command_string,"CopyEnsemble") ) {
        return new CopyEnsemble_Command ();
    }
	else if ( isTScommand && TScommand.equalsIgnoreCase("Copy") ) {
		return new copy_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(command_string,"CreateEnsemble") ) {
        return new CreateEnsemble_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(command_string,"CreateRegressionTestCommandFile") ) {
		return new CreateRegressionTestCommandFile_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"Cumulate") ) {
		return new cumulate_Command ();
	}
    
    // "D" commands...
    
    else if ( StringUtil.startsWithIgnoreCase(command_string,"DeselectTimeSeries") ) {
        return new DeselectTimeSeries_Command ();
    }
	
	// "E" commands...
	
	else if ( StringUtil.startsWithIgnoreCase(command_string,"Exit") ) {
		Command command = new GenericCommand();
		command.setCommandString("Exit");
		return command;
	}

	// "F" commands...

	else if ( StringUtil.startsWithIgnoreCase(command_string,"FillConstant") ) {
		return new fillConstant_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(command_string,"FillFromTS") ) {
        return new FillFromTS_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(command_string,"FillHistMonthAverage") ) {
		return new fillHistMonthAverage_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"FillHistYearAverage") ) {
		return new fillHistYearAverage_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"FillMixedStation") ) {
		return new fillMixedStation_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"FillMOVE2") ) {
		return new fillMOVE2_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"FillRegression") ) {
		return new fillRegression_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(command_string,"FillRepeat") ) {
        return new FillRepeat_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(command_string,"FillUsingDiversionComments") ) {
		return new fillUsingDiversionComments_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(command_string,"Free(") ||
            StringUtil.startsWithIgnoreCase(command_string,"Free (")) {
        return new Free_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(command_string,"FTPGet") ) {
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
		return new lagK_Command ();
	}
	
	// "M" commands...
	
	else if ( StringUtil.startsWithIgnoreCase(command_string,"MergeListFileColumns") ) {
		return new mergeListFileColumns_Command ();
	}

	// "N" commands...

	else if ( isDataTest_command && DataTest_command.equalsIgnoreCase("NewDataTest") ) {
		return new newDataTest_Command();
	}
    // Put the following before the shorter NewStatisticTimeSeries() command.
    else if ( isTScommand && TScommand.equalsIgnoreCase("NewStatisticTimeSeriesFromEnsemble") ) {
        return new NewStatisticTimeSeriesFromEnsemble_Command ();
    }
	else if ( isTScommand && TScommand.equalsIgnoreCase("NewStatisticTimeSeries") ) {
		return new NewStatisticTimeSeries_Command ();
	}
	else if ( isTScommand && TScommand.equalsIgnoreCase("NewStatisticYearTS") ) {
		return new newStatisticYearTS_Command ();
	}
	else if ( isTScommand && TScommand.equalsIgnoreCase("NewPatternTimeSeries") ) {
		return new NewPatternTimeSeries_Command ();
	}
	else if ( isTScommand && TScommand.equalsIgnoreCase("NewTimeSeries") ) {
		return new newTimeSeries_Command ();
	}

	// "O" commands...

    else if ( StringUtil.startsWithIgnoreCase(command_string,"OpenCheckFile") ) {
        return new OpenCheckFile_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(command_string,"OpenHydroBase") ) {
		return new openHydroBase_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"OpenNDFD") ) {
		//return new openNDFD_Command ();
	}

	// "P" commands...

	else if ( StringUtil.startsWithIgnoreCase(command_string,"ProcessTSProduct") ) {
		return new processTSProduct_Command ();
	}

	// "R" commands...

    else if ( StringUtil.startsWithIgnoreCase(command_string,"ReadDateValue") ) {
        return new ReadDateValue_Command ();
    }
    else if ( isTScommand && TScommand.equalsIgnoreCase("ReadDateValue") ) {
        return new ReadDateValue_Command();
    }
    else if ( StringUtil.startsWithIgnoreCase(command_string,"ReadDelimitedFile") ) {
        return new ReadDelimitedFile_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(command_string,"ReadHydroBase") ) {
		return new readHydroBase_Command ();
	}
	else if ( isTScommand && TScommand.equalsIgnoreCase("ReadHydroBase") ) {
		return new readHydroBase_Command();
	}
	else if ( isTScommand && TScommand.equalsIgnoreCase("ReadNDFD") ) {
		//return new readNDFD_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"ReadNwsCard") ) {
		return new readNwsCard_Command ();
	}
	else if ( isTScommand && TScommand.equalsIgnoreCase("ReadNwsCard") ) {
		return new readNwsCard_Command();
	}
    else if ( StringUtil.startsWithIgnoreCase(command_string,"ReadNwsrfsEspTraceEnsemble") ) {
        return new ReadNwsrfsEspTraceEnsemble_Command ();
    }
	// Put before shorter command name...
	else if ( StringUtil.startsWithIgnoreCase(command_string,"ReadStateModB") ) {
		return new readStateModB_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"ReadStateCU") ) {
		return new readStateCU_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"ReadStateMod") ) {
		return new readStateMod_Command ();
	}
	else if ( isTScommand && TScommand.equalsIgnoreCase("ReadStateMod") ) {
		return new readStateMod_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(command_string,"ReadTableFromDelimitedFile") ) {
        return new ReadTableFromDelimitedFile_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(command_string,"RemoveFile") ) {
        return new RemoveFile_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(command_string,"ResequenceTimeSeriesData") ) {
        return new ResequenceTimeSeriesData_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(command_string,"RunCommands") ) {
		return new runCommands_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(command_string,"RunProgram") ) {
        return new RunProgram_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(command_string,"RunPython") ) {
        return new RunPython_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(command_string,"RunDataTest") ) {
		return new runDataTest_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(command_string,"RunningAverage") ) {
        return new RunningAverage_Command ();
    }

	// "S" commands...

	else if ( StringUtil.startsWithIgnoreCase(command_string,"Scale") ) {
		return new scale_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(command_string,"SelectTimeSeries") ) {
        return new SelectTimeSeries_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(command_string,"SetAutoExtendPeriod") ) {
        return new SetAutoExtendPeriod_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(command_string,"SetAveragePeriod") ) {
        return new SetAveragePeriod_Command ();
    }
    // Do a check for the obsolete SetConst() and SetConstantBefore().
    // If encountered, they will be handled as an unknown command.
    else if ( StringUtil.startsWithIgnoreCase(command_string,"SetConstant") &&
            !StringUtil.startsWithIgnoreCase(command_string,"SetConstantBefore") &&
            !StringUtil.startsWithIgnoreCase(command_string,"SetConst ") &&
            !StringUtil.startsWithIgnoreCase(command_string,"SetConst(") ) {
        return new SetConstant_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(command_string,"SetDebugLevel") ) {
        return new SetDebugLevel_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(command_string,"SetFromTS") ) {
        return new SetFromTS_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(command_string,"SetIgnoreLEZero") ) {
        return new SetIgnoreLEZero_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(command_string,"SetIncludeMissingTS") ) {
        return new SetIncludeMissingTS_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(command_string,"SetInputPeriod") ) {
		return new SetInputPeriod_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"SetOutputPeriod") ) {
		return new SetOutputPeriod_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(command_string,"SetOutputYearType") ) {
        return new SetOutputYearType_Command ();
    }
    // Put this before the shorter SetProperty() to avoid ambiguity.
    else if ( StringUtil.startsWithIgnoreCase(command_string,"SetPropertyFromNwsrfsAppDefault") ) {
        return new SetPropertyFromNwsrfsAppDefault_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(command_string,"SetProperty") ) {
        return new SetProperty_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(command_string,"SetQueryPeriod") ) {
		// Phasing into new syntax...
		return new SetInputPeriod_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"SetTimeSeriesProperty") ) {
		return new SetTimeSeriesProperty_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(command_string,"SetWarningLevel") ) {
        return new SetWarningLevel_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(command_string,"SetWorkingDir") ) {
        return new SetWorkingDir_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(command_string,"ShiftTimeByInterval") ) {
        return new ShiftTimeByInterval_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(command_string,"SortTimeSeries") ) {
        return new sortTimeSeries_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase( command_string,"StartLog") ){
		return new startLog_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase( command_string,"StartRegressionTestResultsReport") ){
        return new StartRegressionTestResultsReport_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(command_string,"Subtract") ) {
        return new Subtract_Command ();
    }
	
	// "T" commands...

	else if ( StringUtil.startsWithIgnoreCase(command_string,"TestCommand") ) {
		return new testCommand_Command ();
	}

	// "W" commands...

    else if ( isTScommand && TScommand.equalsIgnoreCase("WeightTraces") ) {
        return new WeightTraces_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(command_string,"WriteDateValue") ) {
		return new WriteDateValue_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(command_string, "WriteNwsCard")) {
        return new WriteNwsCard_Command();
    }
	else if ( StringUtil.startsWithIgnoreCase(command_string, "WriteNWSRFSESPTraceEnsemble")) {
		return new writeNWSRFSESPTraceEnsemble_Command();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"WriteProperty") ) {
		return new WriteProperty_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(command_string,"WriteTimeSeriesProperty") ) {
        return new WriteTimeSeriesProperty_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(command_string,"WriteRiverWare") ) {
		return new writeRiverWare_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(command_string,"WriteSHEF") ) {
        return new WriteSHEF_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(command_string,"WriteStateMod") ) {
		return new writeStateMod_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"WriteSummary") ) {
		return new WriteSummary_Command ();
	}

	// Did not match a command...

	if ( create_generic_command_if_not_recognized ) {
		// Create a generic command...
		Command c = new GenericCommand ();
		c.setCommandString( command_string );
        Message.printStatus ( 2, routine, "Creating GenericCommand for unknown command \"" +
                command_string + "\"" );
		return c;
	}
	else {
		// Throw an exception if the command is not recognized.
		throw new UnknownCommandException ( "Unknown command \"" + command_string + "\"" );
	}
}

}
