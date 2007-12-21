package rti.tscommandprocessor.commands.custom;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.DayTS;
import RTi.TS.TS;
import RTi.TS.TSUtil;

import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.FileGenerator;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;

/**
<p>
This class initializes, checks, and runs the CustomCommand() command.
</p>
*/
public class CustomCommand_Command extends AbstractCommand implements Command, FileGenerator
{
    
/**
Tests being performed.
*/
private final int __TEST_NO_CHANGE = 0;
private final int __TEST_DIRECTION = 1;
private final int __TEST_CHANGE = 2;
private final int __TEST_VALUE = 3;
private final int __TEST_FINAL = 4;
    
/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Constructor.
*/
public CustomCommand_Command ()
{	super();
	setCommandName ( "CustomCommand" );
}

/**
Convert a boolean to "YES" or "NO".
*/
private String booleanToYesNo ( boolean b )
{
    if ( b ) {
        return "YES";
    }
    else {
        return "NO";
    }
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor
dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	String warning = "";
	String routine = getCommandName() + ".checkCommandParameters";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
    CommandProcessor processor = getCommandProcessor();
    
    String CurrentRForecastTSID = parameters.getValue("CurrentRForecastTSID");
    String CurrentNForecastTSID = parameters.getValue ( "CurrentNForecastTSID" );
    String PreviousNForecastTSID = parameters.getValue ( "PreviousNForecastTSID" );
    String ForecastStart = parameters.getValue ( "ForecastStart" );
    String NoiseThreshold = parameters.getValue ( "NoiseThreshold" );
    String ChangeCriteria  = parameters.getValue("ChangeCriteria");
    String ValueCriteria = parameters.getValue("ValueCriteria");
    String OutputFile = parameters.getValue("OutputFile");
    
    if ( (CurrentRForecastTSID == null) || (CurrentRForecastTSID.length() == 0) ) {
        message = "A CurrentRForecast time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a time series identifier." ) );
    }
    if ( (CurrentNForecastTSID == null) || (CurrentNForecastTSID.length() == 0) ) {
        message = "A CurrentNForecast time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a time series identifier." ) );
    }
    if ( (PreviousNForecastTSID == null) || (PreviousNForecastTSID.length() == 0) ) {
        message = "A PreviousNForecast time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a time series identifier." ) );
    }
   
    if ( (ForecastStart != null) && !ForecastStart.equals("") ) {
        try {
            DateTime date = DateTime.parse(ForecastStart);
            if ( date.getWeekDay() != 2 ) {
                message = "The forecast start date/time \"" + ForecastStart + "\" must be a Tuesday.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify a forecast start that is a Tuesday." ) );
            }
        }
        catch ( Exception e ) {
            message = "The forecast start date/time \"" + ForecastStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time." ) );
        }
    }
    
    if ( (NoiseThreshold == null) || (NoiseThreshold.length() == 0) ) {
        message = "An noise threshold must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a value for the noise threshold." ) );
    }
    else if ( !StringUtil.isDouble(NoiseThreshold ) ) {
        message = "The noise threshold (" + NoiseThreshold + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a percent value for the noise threshold." ) );
    }
    
    if ( (ChangeCriteria == null) || (ChangeCriteria.length() == 0) ) {
        message = "An change criteria must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a value for the change criteria." ) );
    }
    else if ( !StringUtil.isDouble(ChangeCriteria ) ) {
        message = "The change criteria (" + ChangeCriteria + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a percent value for the change criteria." ) );
    }
    
    if ( (ValueCriteria == null) || (ValueCriteria.length() == 0) ) {
        message = "An value criteria must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a value for the change criteria." ) );
    }
    else if ( !StringUtil.isDouble(ValueCriteria ) ) {
        message = "The value criteria (" + ValueCriteria + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a percent value for the value criteria." ) );
    }
    
    if ( (OutputFile == null) || (OutputFile.length() == 0) ) {
        message = "The output file: \"" + OutputFile + "\" must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an output file." ) );
    }
    else {
        String working_dir = null;
        try { Object o = processor.getPropContents ( "WorkingDir" );
            if ( o != null ) {
                working_dir = (String)o;
            }
        }
        catch ( Exception e ) {
            message = "Error requesting WorkingDir from processor.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Software error - report the problem to support." ) );
        }

        try {
            String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir, OutputFile));
            File f = new File ( adjusted_path );
            File f2 = new File ( f.getParent() );
            if ( !f2.exists() ) {
                message = "The output file parent directory does " +
                "not exist: \"" + adjusted_path + "\".";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Create the output directory." ) );
            }
            f = null;
            f2 = null;
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

	// Check for invalid parameters...
	Vector valid_Vector = new Vector();
    valid_Vector.add ( "CurrentRForecastTSID" );
    valid_Vector.add ( "CurrentNForecastTSID" );
    valid_Vector.add ( "PreviousNForecastTSID" );
    valid_Vector.add ( "ForecastStart" );
    valid_Vector.add ( "NoiseThreshold" );
	valid_Vector.add ( "ChangeCriteria" );
	valid_Vector.add ( "ValueCriteria" );
    valid_Vector.add ( "OutputFile" );
	warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level), routine, warning );
		throw new InvalidCommandParameterException ( warning );
	}
	status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Process the custom command.
*/
private int customCommand (
        DayTS CurrentRForecastTSID_TS,
        DayTS CurrentNForecastTSID_TS,
        DayTS PreviousNForecastTSID_TS,
        DateTime ForecastStart_DateTime,
        double NoiseThreshold_double,
        double ChangeCriteria_double,
        double ValueCriteria_double,
        String OutputFile_full,
        CommandStatus status )
{   String routine = "CustomCommand.customCommand";
    int warning_count = 0;
    
    // Start on the forecast date and loop through the data to fill bins of data to fill the
    // following bins.

    double CurrentRForecast_value = 0.0;
    double CurrentNForecast_value = 0.0;
    double PreviousNForecast_value = 0.0;

    double CurrentRForecast_total_thisweek = 0.0;
    double CurrentNForecast_total_thisweek = 0.0;
    double PreviousNForecast_total_thisweek = 0.0;
    
    double CurrentRForecast_total_nextweek = 0.0;
    double CurrentNForecast_total_nextweek = 0.0;
    double PreviousNForecast_total_nextweek = 0.0;
    
    double CurrentRForecast_total_thismonth = 0.0;
    double CurrentNForecast_total_thismonth = 0.0;
    double PreviousNForecast_total_thismonth = 0.0;
    
    double CurrentRForecast_total_month1 = 0.0;
    double CurrentNForecast_total_month1 = 0.0;
    double PreviousNForecast_total_month1 = 0.0;
    
    double CurrentRForecast_total_month2 = 0.0;
    double CurrentNForecast_total_month2 = 0.0;
    double PreviousNForecast_total_month2 = 0.0;
    
    double CurrentRForecast_total_month3 = 0.0;
    double CurrentNForecast_total_month3 = 0.0;
    double PreviousNForecast_total_month3 = 0.0;
    
    double CurrentRForecast_total_end = 0.0;
    double CurrentNForecast_total_end = 0.0;
    double PreviousNForecast_total_end = 0.0;
    
    // For iterator...
    DateTime date = new DateTime(ForecastStart_DateTime);
    
    double ValueCriteria_high = 1.0 + ValueCriteria_double/100.0;
    double ValueCriteria_low = 1.0 - ValueCriteria_double/100.0;
    double ChangeCriteria_high = 1.0 + ChangeCriteria_double/100.0;
    double ChangeCriteria_low = 1.0 - ChangeCriteria_double/100.0;
    
    boolean in_thisweek = true;     // Always true to start
    boolean in_nextweek = false;
    boolean in_thismonth = true;    // Always true to start
    boolean in_month1 = false;
    boolean in_month2 = false;
    boolean in_month3 = false;
    boolean in_end = false;
    int day_of_week;
    //final int day_sunday = 0;
    final int day_monday = 1;
    final int day_tuesday = 2;
    //final int day_wednesday = 3;
    //final int day_thursday = 4;
    final int day_friday = 5;
    final int day_saturday = 6;
    String message;
    CommandPhaseType command_phase = CommandPhaseType.RUN;
    DateTime end = new DateTime(CurrentRForecastTSID_TS.getDate2());
    Message.printStatus ( 2, routine, "Generating report starting on " + ForecastStart_DateTime + " through " + end );
    for ( int i = 0; date.lessThanOrEqualTo(end); date.addDay(1), i++ ) {
        CurrentRForecast_value = CurrentRForecastTSID_TS.getDataValue ( date );
        if ( CurrentRForecastTSID_TS.isDataMissing(CurrentRForecast_value) ) {
            message = "CurrentRForecast(" + date + ") is missing - treat as zero.";
            Message.printWarning( 3, routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify input time series." ) );
            CurrentRForecast_value = 0.0;
            ++warning_count;
        }
        else if ( CurrentRForecast_value <= 0.0 ) {
            message = "CurrentRForecast(" + date + ") is negative (" +
            CurrentRForecast_value + ") - will decrease total.";
            Message.printWarning( 3, routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify input time series." ) );
            ++warning_count;
        }
        CurrentNForecast_value = CurrentNForecastTSID_TS.getDataValue ( date );
        if ( CurrentNForecastTSID_TS.isDataMissing(CurrentNForecast_value) ) {
            message = "CurrentNForecast(" + date + ") is missing - treat as zero.";
            Message.printWarning( 3, routine, message );
            CurrentNForecast_value = 0.0;
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify input time series." ) );
            ++warning_count;
        }
        else if ( CurrentNForecast_value <= 0.0 ) {
            message = "CurrentNForecast(" + date + ") is negative (" +
            CurrentNForecast_value + ") - will decrease total.";
            Message.printWarning( 3, routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify input time series." ) );
            ++warning_count;
        }
        PreviousNForecast_value = PreviousNForecastTSID_TS.getDataValue ( date );
        if ( PreviousNForecastTSID_TS.isDataMissing(PreviousNForecast_value) ) {
            message = "PreviousNForecast(" + date + ") is missing - treat as zero.";
            Message.printWarning( 3, routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify input time series." ) );
            PreviousNForecast_value = 0.0;
            ++warning_count;
        }
        else if ( PreviousNForecast_value <= 0.0 ) {
            message = "PreviousNForecast(" + date + ") is negative (" +
            PreviousNForecast_value + ") - will decrease total.";
            Message.printWarning( 3, routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify input time series." ) );
            ++warning_count;
        }
        day_of_week = date.getWeekDay();
        // Adjust the bin that the value should go into...
        if ( i == 0 ) {
            // Always in this week with no further adjustments necessary
            in_thisweek = true;
        }
        else if ( in_thisweek ) {
            // Switch to next week if Saturday and not the first week being processed
            if ( day_of_week == day_saturday ) {
                in_thisweek = false;
                in_nextweek = true;
            }
        }
        else if ( in_nextweek ) {
            // Be done with week when get to saturday
            if ( day_of_week == day_saturday ) {
                in_nextweek = false;
            }
        }
        if ( i == 0 ) {
            // Always in this month with no further adjustments necessary
            in_thismonth = true;
        }
        else if ( in_thismonth ) {
            // Switch to next month if not the first day being processed and the day is 1
            if ( date.getDay() == 1 ) {
                in_thismonth = false;
                in_month1 = true;
            }
        }
        else if ( in_month1 ) {
            if ( date.getDay() == 1 ) {
                in_month1 = false;
                in_month2 = true;
            }
        }
        else if ( in_month2 ) {
            if ( date.getDay() == 1 ) {
                in_month2 = false;
                in_month3 = true;
            }
        }
        else if ( in_month3 ) {
            if ( date.getDay() == 1 ) {
                // Done processing...
                in_month3 = false;
                in_end = true;
            }
        }
        // Now accually do the processing...
        // Weekly metrics...
        if ( in_thisweek && (day_of_week >= day_tuesday) && (day_of_week <= day_friday) ) {
            CurrentRForecast_total_thisweek += CurrentRForecast_value;
            CurrentNForecast_total_thisweek += CurrentNForecast_value;
            PreviousNForecast_total_thisweek += PreviousNForecast_value;
        }
        else if ( in_nextweek && (day_of_week >= day_monday) && (day_of_week <= day_friday)) {
            CurrentRForecast_total_nextweek += CurrentRForecast_value;
            CurrentNForecast_total_nextweek += CurrentNForecast_value;
            PreviousNForecast_total_nextweek += PreviousNForecast_value;
        }
        // Monthly metrics are computed separately...
        if ( in_thismonth ) {
            CurrentRForecast_total_thismonth += CurrentRForecast_value;
            CurrentNForecast_total_thismonth += CurrentNForecast_value;
            PreviousNForecast_total_thismonth += PreviousNForecast_value;
        }
        else if ( in_month1 ) {
            CurrentRForecast_total_month1 += CurrentRForecast_value;
            CurrentNForecast_total_month1 += CurrentNForecast_value;
            PreviousNForecast_total_month1 += PreviousNForecast_value;
        }
        else if ( in_month2 ) {
            CurrentRForecast_total_month2 += CurrentRForecast_value;
            CurrentNForecast_total_month2 += CurrentNForecast_value;
            PreviousNForecast_total_month2 += PreviousNForecast_value;
        }
        else if ( in_month3 ) {
            CurrentRForecast_total_month3 += CurrentRForecast_value;
            CurrentNForecast_total_month3 += CurrentNForecast_value;
            PreviousNForecast_total_month3 += PreviousNForecast_value;
        }
        else if ( in_month3 ) {
            CurrentRForecast_total_end += CurrentRForecast_value;
            CurrentNForecast_total_end += CurrentNForecast_value;
            PreviousNForecast_total_end += PreviousNForecast_value;
        }
    }
    
    // Write the results with the pass fail results.
    
    PrintWriter fout = null;
    String comment = "#";
    String delim = ",";
    try {
         fout = new PrintWriter ( new FileOutputStream ( OutputFile_full ) );
         IOUtil.printCreatorHeader( fout, comment, 120, 0);
         String format_param = "%-22.22s";
         String format_percent = "%12.2f";
         String format_text = "%12.12s";
         String format_value = "%12.0f";
         String format_heading = "%12.12s";
         fout.println ( comment );
         fout.println ( comment + " units of output are cumulative " + CurrentRForecastTSID_TS.getDataUnits());
         fout.println ( comment );
         fout.println ( comment + " Noise threshold (%):  " + StringUtil.formatString(NoiseThreshold_double,format_percent));
         fout.println ( comment + " Change criteria (%):  " + StringUtil.formatString(ChangeCriteria_double,format_percent));
         fout.println ( comment + " Value criteria (%):   " + StringUtil.formatString(ValueCriteria_double,format_percent));
         fout.println ( comment );
         /*
         fout.println ( comment + " For noise to pass:  (|R - Np|/Np < NoiseThreshold) and (|Nc - Np|/Np < NoiseThreshold.");
         fout.println ( comment + " For direction to pass R - Np sign must equal Nc - Np sign.");
         fout.println ( comment + " For magnitude to pass 100*abs(R - Np)/Np must be <= " +
                 StringUtil.formatString(ValueCriteria_double,"%.2f") + "*abs(Nc - Np)/Np.");
         fout.println ( comment + " For no change to pass - THIS IS UNDEFINED AND WILL ALWAYS PASS.");
         */
         fout.println ( comment );
         fout.println (
                 StringUtil.formatString("Parameter",format_param) + delim +
                 StringUtil.formatString("This week",format_heading) + delim +
                 StringUtil.formatString("Next week",format_heading) + delim +
                 StringUtil.formatString("This month",format_heading) + delim +
                 StringUtil.formatString("Month + 1",format_heading) + delim +
                 StringUtil.formatString("Month + 2",format_heading) + delim +
                 StringUtil.formatString("Month + 3",format_heading) + delim +
                 StringUtil.formatString("End",format_heading));
         // Raw N values....
         fout.println (
                 StringUtil.formatString("PreviousNForecast (Np)",format_param) + delim +
                 StringUtil.formatString(PreviousNForecast_total_thisweek,format_value) + delim +
                 StringUtil.formatString(PreviousNForecast_total_nextweek,format_value) + delim +
                 StringUtil.formatString(PreviousNForecast_total_thismonth,format_value) + delim +
                 StringUtil.formatString(PreviousNForecast_total_month1,format_value) + delim +
                 StringUtil.formatString(PreviousNForecast_total_month2,format_value) + delim +
                 StringUtil.formatString(PreviousNForecast_total_month3,format_value) + delim +
                 StringUtil.formatString(PreviousNForecast_total_end,format_value) );
         fout.println (
                 StringUtil.formatString("CurrentNForecast (Nc)",format_param) + delim +
                 StringUtil.formatString(CurrentNForecast_total_thisweek,format_value) + delim +
                 StringUtil.formatString(CurrentNForecast_total_nextweek,format_value) + delim +
                 StringUtil.formatString(CurrentNForecast_total_thismonth,format_value) + delim +
                 StringUtil.formatString(CurrentNForecast_total_month1,format_value) + delim +
                 StringUtil.formatString(CurrentNForecast_total_month2,format_value) + delim +
                 StringUtil.formatString(CurrentNForecast_total_month3,format_value) + delim +
                 StringUtil.formatString(CurrentNForecast_total_end,format_value));
         fout.println (
                 StringUtil.formatString("Value bound (high)",format_param) + delim +
                 StringUtil.formatString(CurrentNForecast_total_thisweek*ValueCriteria_high,format_value) + delim +
                 StringUtil.formatString(CurrentNForecast_total_nextweek*ValueCriteria_high,format_value) + delim +
                 StringUtil.formatString(CurrentNForecast_total_thismonth*ValueCriteria_high,format_value) + delim +
                 StringUtil.formatString(CurrentNForecast_total_month1*ValueCriteria_high,format_value) + delim +
                 StringUtil.formatString(CurrentNForecast_total_month2*ValueCriteria_high,format_value) + delim +
                 StringUtil.formatString(CurrentNForecast_total_month3*ValueCriteria_high,format_value) + delim +
                 StringUtil.formatString(CurrentNForecast_total_end*ValueCriteria_high,format_value));
         fout.println (
                 StringUtil.formatString("Value bound (low)",format_param) + delim +
                 StringUtil.formatString(CurrentNForecast_total_thisweek*ValueCriteria_low,format_value) + delim +
                 StringUtil.formatString(CurrentNForecast_total_nextweek*ValueCriteria_low,format_value) + delim +
                 StringUtil.formatString(CurrentNForecast_total_thismonth*ValueCriteria_low,format_value) + delim +
                 StringUtil.formatString(CurrentNForecast_total_month1*ValueCriteria_low,format_value) + delim +
                 StringUtil.formatString(CurrentNForecast_total_month2*ValueCriteria_low,format_value) + delim +
                 StringUtil.formatString(CurrentNForecast_total_month3*ValueCriteria_low,format_value) + delim +
                 StringUtil.formatString(CurrentNForecast_total_end*ValueCriteria_low,format_value));
         // Difference and percent difference of N values...
         double Ndiff_thisweek = CurrentNForecast_total_thisweek - PreviousNForecast_total_thisweek;
         double Ndiff_nextweek = CurrentNForecast_total_nextweek - PreviousNForecast_total_nextweek;
         double Ndiff_thismonth = CurrentNForecast_total_thismonth - PreviousNForecast_total_thismonth;
         double Ndiff_month1 = CurrentNForecast_total_month1 - PreviousNForecast_total_month1;
         double Ndiff_month2 = CurrentNForecast_total_month2 - PreviousNForecast_total_month2;
         double Ndiff_month3 = CurrentNForecast_total_month3 - PreviousNForecast_total_month3;
         double Ndiff_end = CurrentNForecast_total_end - PreviousNForecast_total_end;
         fout.println (
                 StringUtil.formatString("Nc - Np Diff",format_param) + delim +
                 StringUtil.formatString(Ndiff_thisweek,format_value) + delim +
                 StringUtil.formatString(Ndiff_nextweek,format_value) + delim +
                 StringUtil.formatString(Ndiff_thismonth,format_value) + delim +
                 StringUtil.formatString(Ndiff_month1,format_value) + delim +
                 StringUtil.formatString(Ndiff_month2,format_value) + delim +
                 StringUtil.formatString(Ndiff_month3,format_value) + delim +
                 StringUtil.formatString(Ndiff_end,format_value));
         double Ndiff_percent_thisweek = 100.0*Ndiff_thisweek/PreviousNForecast_total_thisweek;
         double Ndiff_percent_nextweek = 100.0*Ndiff_nextweek/PreviousNForecast_total_nextweek;
         double Ndiff_percent_thismonth = 100.0*Ndiff_thismonth/PreviousNForecast_total_thismonth;
         double Ndiff_percent_month1 = 100.0*Ndiff_month1/PreviousNForecast_total_month1;
         double Ndiff_percent_month2 = 100.0*Ndiff_month2/PreviousNForecast_total_month2;
         double Ndiff_percent_month3 = 100.0*Ndiff_month3/PreviousNForecast_total_month3;
         double Ndiff_percent_end = 100.0*Ndiff_end/PreviousNForecast_total_end;
         fout.println (
                 StringUtil.formatString("Nc - Np Diff %",format_param) + delim +
                 StringUtil.formatString(Ndiff_percent_thisweek,format_percent) + delim +
                 StringUtil.formatString(Ndiff_percent_nextweek,format_percent) + delim +
                 StringUtil.formatString(Ndiff_percent_thismonth,format_percent) + delim +
                 StringUtil.formatString(Ndiff_percent_month1,format_percent) + delim +
                 StringUtil.formatString(Ndiff_percent_month2,format_percent) + delim +
                 StringUtil.formatString(Ndiff_percent_month3,format_percent) + delim +
                 StringUtil.formatString(Ndiff_percent_end,format_percent));
         // Raw R values...
         fout.println (
                 StringUtil.formatString("CurrentRForecast (R)",format_param) + delim +
                 StringUtil.formatString(CurrentRForecast_total_thisweek,format_value) + delim +
                 StringUtil.formatString(CurrentRForecast_total_nextweek,format_value) + delim +
                 StringUtil.formatString(CurrentRForecast_total_thismonth,format_value) + delim +
                 StringUtil.formatString(CurrentRForecast_total_month1,format_value) + delim +
                 StringUtil.formatString(CurrentRForecast_total_month2,format_value) + delim +
                 StringUtil.formatString(CurrentRForecast_total_month3,format_value) + delim +
                 StringUtil.formatString(CurrentRForecast_total_end,format_value));
         // R minus N previous...
         // Difference and percent difference of N values...
         double RNpdiff_thisweek = CurrentRForecast_total_thisweek - PreviousNForecast_total_thisweek;
         double RNpdiff_nextweek = CurrentRForecast_total_nextweek - PreviousNForecast_total_nextweek;
         double RNpdiff_thismonth = CurrentRForecast_total_thismonth - PreviousNForecast_total_thismonth;
         double RNpdiff_month1 = CurrentRForecast_total_month1 - PreviousNForecast_total_month1;
         double RNpdiff_month2 = CurrentRForecast_total_month2 - PreviousNForecast_total_month2;
         double RNpdiff_month3 = CurrentRForecast_total_month3 - PreviousNForecast_total_month3;
         double RNpdiff_end = CurrentRForecast_total_end - PreviousNForecast_total_end;
         fout.println (
                 StringUtil.formatString("R - Np Diff",format_param) + delim +
                 StringUtil.formatString(RNpdiff_thisweek,format_value) + delim +
                 StringUtil.formatString(RNpdiff_nextweek,format_value) + delim +
                 StringUtil.formatString(RNpdiff_thismonth,format_value) + delim +
                 StringUtil.formatString(RNpdiff_month1,format_value) + delim +
                 StringUtil.formatString(RNpdiff_month2,format_value) + delim +
                 StringUtil.formatString(RNpdiff_month3,format_value) + delim +
                 StringUtil.formatString(RNpdiff_end,format_value));
         double RNpdiff_percent_thisweek = 100.0*RNpdiff_thisweek/PreviousNForecast_total_thisweek;
         double RNpdiff_percent_nextweek = 100.0*RNpdiff_nextweek/PreviousNForecast_total_nextweek;
         double RNpdiff_percent_thismonth = 100.0*RNpdiff_thismonth/PreviousNForecast_total_thismonth;
         double RNpdiff_percent_month1 = 100.0*RNpdiff_month1/PreviousNForecast_total_month1;
         double RNpdiff_percent_month2 = 100.0*RNpdiff_month2/PreviousNForecast_total_month2;
         double RNpdiff_percent_month3 = 100.0*RNpdiff_month3/PreviousNForecast_total_month3;
         double RNpdiff_percent_end = 100.0*RNpdiff_end/PreviousNForecast_total_end;
         fout.println (
                 StringUtil.formatString("R - Np Diff %",format_param) + delim +
                 StringUtil.formatString(RNpdiff_percent_thisweek,format_percent) + delim +
                 StringUtil.formatString(RNpdiff_percent_nextweek,format_percent) + delim +
                 StringUtil.formatString(RNpdiff_percent_thismonth,format_percent) + delim +
                 StringUtil.formatString(RNpdiff_percent_month1,format_percent) + delim +
                 StringUtil.formatString(RNpdiff_percent_month2,format_percent) + delim +
                 StringUtil.formatString(RNpdiff_percent_month3,format_percent) + delim +
                 StringUtil.formatString(RNpdiff_percent_end,format_percent) );
         // R minus N current...
         // Difference and percent difference of N values...
         double RNcdiff_thisweek = CurrentRForecast_total_thisweek - CurrentNForecast_total_thisweek;
         double RNcdiff_nextweek = CurrentRForecast_total_nextweek - CurrentNForecast_total_nextweek;
         double RNcdiff_thismonth = CurrentRForecast_total_thismonth - CurrentNForecast_total_thismonth;
         double RNcdiff_month1 = CurrentRForecast_total_month1 - CurrentNForecast_total_month1;
         double RNcdiff_month2 = CurrentRForecast_total_month2 - CurrentNForecast_total_month2;
         double RNcdiff_month3 = CurrentRForecast_total_month3 - CurrentNForecast_total_month3;
         double RNcdiff_end = CurrentRForecast_total_end - CurrentNForecast_total_end;
         fout.println (
                 StringUtil.formatString("R - Nc Diff",format_param) + delim +
                 StringUtil.formatString(RNcdiff_thisweek,format_value) + delim +
                 StringUtil.formatString(RNcdiff_nextweek,format_value) + delim +
                 StringUtil.formatString(RNcdiff_thismonth,format_value) + delim +
                 StringUtil.formatString(RNcdiff_month1,format_value) + delim +
                 StringUtil.formatString(RNcdiff_month2,format_value) + delim +
                 StringUtil.formatString(RNcdiff_month3,format_value) + delim +
                 StringUtil.formatString(RNcdiff_end,format_value));
         double RNcdiff_percent_thisweek = 100.0*RNcdiff_thisweek/CurrentNForecast_total_thisweek;
         double RNcdiff_percent_nextweek = 100.0*RNcdiff_nextweek/CurrentNForecast_total_nextweek;
         double RNcdiff_percent_thismonth = 100.0*RNcdiff_thismonth/CurrentNForecast_total_thismonth;
         double RNcdiff_percent_month1 = 100.0*RNcdiff_month1/CurrentNForecast_total_month1;
         double RNcdiff_percent_month2 = 100.0*RNcdiff_month2/CurrentNForecast_total_month2;
         double RNcdiff_percent_month3 = 100.0*RNcdiff_month3/CurrentNForecast_total_month3;
         double RNcdiff_percent_end = 100.0*RNcdiff_end/CurrentNForecast_total_end;
         fout.println (
                 StringUtil.formatString("R - Nc Diff %",format_param) + delim +
                 StringUtil.formatString(RNcdiff_percent_thisweek,format_percent) + delim +
                 StringUtil.formatString(RNcdiff_percent_nextweek,format_percent) + delim +
                 StringUtil.formatString(RNcdiff_percent_thismonth,format_percent) + delim +
                 StringUtil.formatString(RNcdiff_percent_month1,format_percent) + delim +
                 StringUtil.formatString(RNcdiff_percent_month2,format_percent) + delim +
                 StringUtil.formatString(RNcdiff_percent_month3,format_percent) + delim +
                 StringUtil.formatString(RNcdiff_percent_end,format_percent) );
         // Do the final tests
         String [] pass_test_thisweek = test ( 
                 PreviousNForecast_total_thisweek, CurrentNForecast_total_thisweek, CurrentRForecast_total_thisweek,
                 Ndiff_thisweek, RNpdiff_thisweek, RNcdiff_thisweek, ValueCriteria_double, ChangeCriteria_double, NoiseThreshold_double );
         String [] pass_test_nextweek = test ( 
                 PreviousNForecast_total_nextweek, CurrentNForecast_total_nextweek, CurrentRForecast_total_nextweek,
                 Ndiff_nextweek, RNpdiff_nextweek, RNcdiff_nextweek, ValueCriteria_double, ChangeCriteria_double, NoiseThreshold_double );
         String [] pass_test_thismonth = test (
                 PreviousNForecast_total_thismonth, CurrentNForecast_total_thismonth, CurrentRForecast_total_thismonth,
                 Ndiff_thismonth, RNpdiff_thismonth, RNcdiff_thismonth, ValueCriteria_double, ChangeCriteria_double, NoiseThreshold_double );
         String [] pass_test_month1 = test (
                 PreviousNForecast_total_month1, CurrentNForecast_total_month1, CurrentRForecast_total_month1,
                 Ndiff_month1, RNpdiff_month1, RNcdiff_month1, ValueCriteria_double, ChangeCriteria_double, NoiseThreshold_double );
         String [] pass_test_month2 = test (
                 PreviousNForecast_total_month2, CurrentNForecast_total_month2, CurrentRForecast_total_month2,
                 Ndiff_month2, RNpdiff_month2, RNcdiff_month2, ValueCriteria_double, ChangeCriteria_double, NoiseThreshold_double );
         String [] pass_test_month3 = test (
                 PreviousNForecast_total_month3, CurrentNForecast_total_month3, CurrentRForecast_total_month3,
                 Ndiff_month3, RNpdiff_month3, RNcdiff_month3, ValueCriteria_double, ChangeCriteria_double, NoiseThreshold_double );
         String [] pass_test_end = test ( 
                 PreviousNForecast_total_end, CurrentNForecast_total_end, CurrentRForecast_total_end,
                 Ndiff_end, RNpdiff_end, RNcdiff_end, ValueCriteria_double, ChangeCriteria_double, NoiseThreshold_double );
         
         fout.println (
                 StringUtil.formatString("Direction Pass?",format_param) + delim +
                 StringUtil.formatString(pass_test_thisweek[__TEST_DIRECTION],format_text) + delim +
                 StringUtil.formatString(pass_test_nextweek[__TEST_DIRECTION],format_text) + delim +
                 StringUtil.formatString(pass_test_thismonth[__TEST_DIRECTION],format_text) + delim +
                 StringUtil.formatString(pass_test_month1[__TEST_DIRECTION],format_text) + delim +
                 StringUtil.formatString(pass_test_month2[__TEST_DIRECTION],format_text) + delim +
                 StringUtil.formatString(pass_test_month3[__TEST_DIRECTION],format_text) + delim +
                 StringUtil.formatString(pass_test_end[__TEST_DIRECTION],format_text));
         fout.println (
                 StringUtil.formatString("Change Pass?",format_param) + delim +
                 StringUtil.formatString(pass_test_thisweek[__TEST_CHANGE],format_text) + delim +
                 StringUtil.formatString(pass_test_nextweek[__TEST_CHANGE],format_text) + delim +
                 StringUtil.formatString(pass_test_thismonth[__TEST_CHANGE],format_text) + delim +
                 StringUtil.formatString(pass_test_month1[__TEST_CHANGE],format_text) + delim +
                 StringUtil.formatString(pass_test_month2[__TEST_CHANGE],format_text) + delim +
                 StringUtil.formatString(pass_test_month3[__TEST_CHANGE],format_text) + delim +
                 StringUtil.formatString(pass_test_end[__TEST_CHANGE],format_text));
         fout.println (
                 StringUtil.formatString("Value Pass?",format_param) + delim +
                 StringUtil.formatString(pass_test_thisweek[__TEST_VALUE],format_text) + delim +
                 StringUtil.formatString(pass_test_nextweek[__TEST_VALUE],format_text) + delim +
                 StringUtil.formatString(pass_test_thismonth[__TEST_VALUE],format_text) + delim +
                 StringUtil.formatString(pass_test_month1[__TEST_VALUE],format_text) + delim +
                 StringUtil.formatString(pass_test_month2[__TEST_VALUE],format_text) + delim +
                 StringUtil.formatString(pass_test_month3[__TEST_VALUE],format_text) + delim +
                 StringUtil.formatString(pass_test_end[__TEST_VALUE],format_text));
         
         fout.println (
                 StringUtil.formatString("No change Pass?",format_param) + delim +
                 StringUtil.formatString(pass_test_thisweek[__TEST_NO_CHANGE],format_text) + delim +
                 StringUtil.formatString(pass_test_nextweek[__TEST_NO_CHANGE],format_text) + delim +
                 StringUtil.formatString(pass_test_thismonth[__TEST_NO_CHANGE],format_text) + delim +
                 StringUtil.formatString(pass_test_month1[__TEST_NO_CHANGE],format_text) + delim +
                 StringUtil.formatString(pass_test_month2[__TEST_NO_CHANGE],format_text) + delim +
                 StringUtil.formatString(pass_test_month3[__TEST_NO_CHANGE],format_text) + delim +
                 StringUtil.formatString(pass_test_end[__TEST_NO_CHANGE],format_text));
         
         fout.println (
                 StringUtil.formatString("Final Pass?",format_param) + delim +
                 StringUtil.formatString(pass_test_thisweek[__TEST_FINAL],format_text) + delim +
                 StringUtil.formatString(pass_test_nextweek[__TEST_FINAL],format_text) + delim +
                 StringUtil.formatString(pass_test_thismonth[__TEST_FINAL],format_text) + delim +
                 StringUtil.formatString(pass_test_month1[__TEST_FINAL],format_text) + delim +
                 StringUtil.formatString(pass_test_month2[__TEST_FINAL],format_text) + delim +
                 StringUtil.formatString(pass_test_month3[__TEST_FINAL],format_text) + delim +
                 StringUtil.formatString(pass_test_end[__TEST_FINAL],format_text));

         
         // Save the output file name...
         fout.close();
         setOutputFile ( new File(OutputFile_full));
    }
    catch ( Exception e ) {
        message = "Unexpected error processing data.";
        Message.printWarning( 3, routine, message );
        Message.printWarning ( 3, routine, e);
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.WARNING,
                        message, "Report the problem to software support." ) );
    }

    return warning_count;
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new CustomCommand_JDialog ( parent, this )).ok();
}

/**
Return the list of files that were created by this command.
*/
public List getGeneratedFileList ()
{
    Vector list = new Vector();
    if ( getOutputFile() != null ) {
        list.addElement ( getOutputFile() );
    }
    return list;
}

/**
Return the output file generated by this file.  This method is used internally.
*/
private File getOutputFile ()
{
    return __OutputFile_File;
}

// Use base class parseCommand()

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "CustomCommand_Command.runCommandInternal", message;
	int warning_level = 2;
    int log_level = 3;  // Non-user warning level
	String command_tag = "" + command_number;
	int warning_count = 0;

    CommandPhaseType command_phase = CommandPhaseType.RUN;
    CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(command_phase);
    
	PropList parameters = getCommandParameters();
    String CurrentRForecastTSID = parameters.getValue("CurrentRForecastTSID");
    String CurrentNForecastTSID = parameters.getValue ( "CurrentNForecastTSID" );
    String PreviousNForecastTSID = parameters.getValue ( "PreviousNForecastTSID" );
    String ForecastStart = parameters.getValue ( "ForecastStart" );
    String NoiseThreshold = parameters.getValue ( "NoiseThreshold" );
    String ChangeCriteria  = parameters.getValue("ChangeCriteria");
    String ValueCriteria = parameters.getValue("ValueCriteria");
    String OutputFile = parameters.getValue("OutputFile");

    // Get the time series to process.  The time series list is searched backwards until the first match...

    PropList request_params = new PropList ( "" );
    request_params.set ( "CommandTag", command_tag );
    request_params.set ( "TSID", CurrentRForecastTSID );
    CommandProcessorRequestResultsBean bean = null;
    try {
        bean = processor.processRequest( "GetTimeSeriesForTSID", request_params);
    }
    catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + CurrentRForecastTSID +
        "\") from processor.";
        Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
    }
    PropList bean_PropList = bean.getResultsPropList();
    Object o_TS = bean_PropList.getContents ( "TS");
    TS CurrentRForecastTSID_TS = null;
    if ( o_TS == null ) {
        message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + CurrentRForecastTSID +
        "\") from processor.";
        Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
    }
    else {
        CurrentRForecastTSID_TS = (TS)o_TS;
    }
     
    if ( CurrentRForecastTSID_TS == null ) {
        message = "Unable to find time series to analyze using TSID \"" + CurrentRForecastTSID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
        new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
        throw new CommandWarningException ( message );
    }
    
    // CurrentNForecastTSID...
    
    request_params = new PropList ( "" );
    request_params.set ( "CommandTag", command_tag );
    request_params.set ( "TSID", CurrentNForecastTSID );
    try {
        bean = processor.processRequest( "GetTimeSeriesForTSID", request_params);
    }
    catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + CurrentNForecastTSID +
        "\") from processor.";
        Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
    }
    bean_PropList = bean.getResultsPropList();
    o_TS = bean_PropList.getContents ( "TS");
    TS CurrentNForecastTSID_TS = null;
    if ( o_TS == null ) {
        message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + CurrentNForecastTSID +
        "\") from processor.";
        Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
    }
    else {
        CurrentNForecastTSID_TS = (TS)o_TS;
    }
     
    if ( CurrentNForecastTSID_TS == null ) {
        message = "Unable to find time series to analyze using TSID \"" + CurrentNForecastTSID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
        new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
        throw new CommandWarningException ( message );
    }
    
    // PreviousNForecastTSID...
    
    request_params.set ( "CommandTag", command_tag );
    request_params.set ( "TSID", PreviousNForecastTSID );
    try {
        bean = processor.processRequest( "GetTimeSeriesForTSID", request_params);
    }
    catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + PreviousNForecastTSID +
        "\") from processor.";
        Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
    }
    bean_PropList = bean.getResultsPropList();
    o_TS = bean_PropList.getContents ( "TS");
    TS PreviousNForecastTSID_TS = null;
    if ( o_TS == null ) {
        message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + PreviousNForecastTSID +
        "\") from processor.";
        Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
    }
    else {
        PreviousNForecastTSID_TS = (TS)o_TS;
    }
     
    if ( PreviousNForecastTSID_TS == null ) {
        message = "Unable to find time series to analyze using TSID \"" + PreviousNForecastTSID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
        new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
        throw new CommandWarningException ( message );
    }
    
    // Check the intervals...
    
    if ( CurrentRForecastTSID_TS.getDataIntervalBase() != TimeInterval.DAY ) {
        message = "CurrentRForecast time series does not have interval of Day";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
        new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a day interval time series." ) );
        throw new CommandWarningException ( message );
    }
    if ( CurrentNForecastTSID_TS.getDataIntervalBase() != TimeInterval.DAY ) {
        message = "CurrentNForecast time series does not have interval of Day";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
        new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a day interval time series." ) );
        throw new CommandWarningException ( message );
    }
    if ( PreviousNForecastTSID_TS.getDataIntervalBase() != TimeInterval.DAY ) {
        message = "PreviousNForecast time series does not have interval of Day";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
        new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a day interval time series." ) );
        throw new CommandWarningException ( message );
    }
    
    // Forecast start
    
    DateTime ForecastStart_DateTime = null;
    if ( (ForecastStart != null) && (ForecastStart.length() > 0) ) {
        request_params = new PropList ( "" );
        request_params.set ( "DateTime", ForecastStart );
        try {
            bean = processor.processRequest( "DateTime", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting DateTime(DateTime=" + ForecastStart + ") from processor.";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Report problem to software support." ) );
        }
        bean_PropList = bean.getResultsPropList();
        Object prop_contents = bean_PropList.getContents ( "DateTime" );
        if ( prop_contents == null ) {
            message = "Null value for DateTime(DateTime=" + ForecastStart + "\") returned from processor.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Verify that a valid InputStart string has been specified." ) );
        }
        else {
            ForecastStart_DateTime = (DateTime)prop_contents;
        }
    }
    else {  // Get from the processor...
        try {
            Object o = processor.getPropContents ( "ForecastStart" );
            if ( o != null ) {
                ForecastStart_DateTime = (DateTime)o;
            }
        }
        catch ( Exception e ) {
            message = "Error requesting ForecastStart from processor.";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                           message, "Report problem to software support." ) );
        }
    }
    
    // Check units...
    
    Vector tslist = new Vector(3);
    tslist.addElement ( CurrentRForecastTSID_TS );
    tslist.addElement ( CurrentNForecastTSID_TS );
    tslist.addElement ( PreviousNForecastTSID_TS );
    if ( !TSUtil.areUnitsCompatible(tslist,true) ) {
        message = "Units for time series are different.";
        Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Verify that the time series units are the same." ) );
            throw new CommandException ( message );
    }
    
    // Parameters for the analysis...
    
    double NoiseThreshold_double = StringUtil.atod ( NoiseThreshold );
    double ChangeCriteria_double = StringUtil.atod ( ChangeCriteria );
    double ValueCriteria_double = StringUtil.atod ( ValueCriteria );
    
	// Now try to process.
    
    try {
        String OutputFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),OutputFile));
        warning_count += customCommand (
                (DayTS)CurrentRForecastTSID_TS,
                (DayTS)CurrentNForecastTSID_TS,
                (DayTS)PreviousNForecastTSID_TS,
                ForecastStart_DateTime,
                NoiseThreshold_double,
                ChangeCriteria_double,
                ValueCriteria_double,
                OutputFile_full,
                status
                );
    }
    catch ( Exception e ) {
        message = "Unexpected error processing command";
        Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
        Message.printWarning ( 3, routine, e );
        status.addToLog ( command_phase,
			new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Check log file for details." ) );
        throw new CommandException ( message );
    }

    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " warnings processing the command.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
            command_tag, ++warning_count),
            routine,message);
        throw new CommandWarningException ( message );
    }
	
	status.refreshPhaseSeverity(command_phase,CommandStatusType.SUCCESS);
}

/**
Set the output file that is created by this command.  This is only used internally.
*/
private void setOutputFile ( File file )
{
    __OutputFile_File = file;
}

/**
Perform the test on the forecast to determine whether it passes or fails the criteria.
The returned values are "YES" (pass), "NO" (fail), or " - " (not applicable).  Returned
test results are for:
<ol>
<li>    [0] - 
</ol>
*/
private String [] test ( 
        double PreviousNForecast_total, double CurrentNForecast_total,
        double CurrentRForecast_total,
        double Ndiff_percent, double RNpdiff_percent, double RNcdiff_percent,
        double ValueCriteria_double, double ChangeCriteria_double, double NoiseThreshold_double )
{   double Ndiff = Ndiff_percent/100.0;
    double RNpdiff = RNpdiff_percent/100.0;
    String [] test_results = new String[5];
    String YES = "YES";
    String NO = "NO";
    String NA = "-";
    // Initialize to fail and reset to PASS or NA with logic below

    test_results[__TEST_NO_CHANGE] = "NO";    // Noise case
    test_results[__TEST_DIRECTION] = "NO";
    test_results[__TEST_CHANGE] = "NO";
    test_results[__TEST_VALUE] = "NO";
    test_results[__TEST_FINAL] = "NO";
    if ( testNoise( NoiseThreshold_double, Ndiff_percent, RNpdiff_percent) ) {
        // FIXME SAM 2007-12-20 Need to define what happens when in the noise condition
        test_results[__TEST_DIRECTION] = NA;
        test_results[__TEST_CHANGE] = NA;
        test_results[__TEST_VALUE] = NA;
        test_results[__TEST_NO_CHANGE] = YES;
    }
    else {
        test_results[__TEST_NO_CHANGE] = NA;   // Not noise.
        // Check the direction of RTi's forecast.
        if ( CurrentNForecast_total > PreviousNForecast_total ) {
            // Positive change
            if ( (CurrentRForecast_total - PreviousNForecast_total) >= 0.0 ) {
                // RTi matches.
                test_results[__TEST_DIRECTION] = YES;
            }
            else {
                // RTi does not match.
                test_results[__TEST_DIRECTION] = NO;
            }
        }
        else {
            // Negative change
            if ( (CurrentRForecast_total - PreviousNForecast_total) <= 0.0 ) {
                // RTi matches.
                test_results[__TEST_DIRECTION] = YES;
            }
            else {
                // RTi does not match.
                test_results[__TEST_DIRECTION] = NO;
            }
        }
        // Check the magnitude test
        if ( Math.abs(RNcdiff_percent) < ValueCriteria_double ) {
            // RTi forecast is within the magnitude tolerance (but direction may be off - see the other test).
            test_results[__TEST_VALUE] = YES;
        }
        else {
            test_results[__TEST_VALUE] = NO;
        }
        // Check the change test
        if ( Math.abs((RNpdiff - Ndiff)/Ndiff)*100.0 < ValueCriteria_double ) {
            // RTi forecast is within the magnitude tolerance (but direction may be off - see the other test).
            test_results[__TEST_VALUE] = YES;
        }
        else {
            test_results[__TEST_VALUE] = NO;
        }
    }
    if ( (test_results[__TEST_DIRECTION].equals(YES) || test_results[__TEST_DIRECTION].equals(NA)) &&
            (test_results[__TEST_NO_CHANGE].equals(YES) || test_results[__TEST_NO_CHANGE].equals(NA)) &&
            (test_results[__TEST_CHANGE].equals(YES) || test_results[__TEST_CHANGE].equals(NA)) &&
            (test_results[__TEST_VALUE].equals(YES) || test_results[__TEST_VALUE].equals(NA)) ) {
        // Overall success
        test_results[__TEST_FINAL] = YES;
    }
    else {
        test_results[__TEST_FINAL] = NO;
    }
    return test_results;
}

/**
Indicate whether the data pass the noise test (difference of R and N forecasts) are less than the
noise tolerance.
@param Ndiff_percent the Noise threshold, as percent (0 to 100).
@param Ndiff_percent the difference between the N current forecast and N previous forecast, as percent.
@param RNpdiff_percent the difference between the R current forecast and N previous forecast, as percent.
*/
private boolean testNoise ( double NoiseThreshold_double, double Ndiff_percent, double RNpdiff_percent )
{
    if ( (Math.abs(Ndiff_percent) < NoiseThreshold_double) && (Math.abs(RNpdiff_percent) < NoiseThreshold_double) ) {
        return true;
    }
    else {
        return false;
    }
}

/**
Return the string representation of the command.
@param parameters Command parameters as strings.
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
    String CurrentRForecastTSID = parameters.getValue("CurrentRForecastTSID");
    String CurrentNForecastTSID = parameters.getValue ( "CurrentNForecastTSID" );
    String PreviousNForecastTSID = parameters.getValue ( "PreviousNForecastTSID" );
    String ForecastStart = parameters.getValue ( "ForecastStart" );
    String NoiseThreshold = parameters.getValue ( "NoiseThreshold" );
    String ChangeCriteria  = parameters.getValue("ChangeCriteria");
    String ValueCriteria = parameters.getValue("ValueCriteria");
    String OutputFile = parameters.getValue("OutputFile");
    
	StringBuffer b = new StringBuffer ();
	if ( (CurrentRForecastTSID != null) && (CurrentRForecastTSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "CurrentRForecastTSID=\"" + CurrentRForecastTSID + "\"" );
	}
    if ( (CurrentNForecastTSID != null) && (CurrentNForecastTSID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "CurrentNForecastTSID=\"" + CurrentNForecastTSID + "\"" );
    }
    if ( (PreviousNForecastTSID != null) && (PreviousNForecastTSID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "PreviousNForecastTSID=\"" + PreviousNForecastTSID + "\"" );
    }
    if ( (ForecastStart != null) && (ForecastStart.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ForecastStart=\"" + ForecastStart + "\"" );
    }
    if ( (NoiseThreshold != null) && (NoiseThreshold.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NoiseThreshold=" + NoiseThreshold );
    }
    if ( (ChangeCriteria != null) && (ChangeCriteria.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ChangeCriteria=" + ChangeCriteria );
    }
    if ( (ValueCriteria != null) && (ValueCriteria.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ValueCriteria=" + ValueCriteria );
    }
    if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputFile=\"" + OutputFile + "\"");
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}
