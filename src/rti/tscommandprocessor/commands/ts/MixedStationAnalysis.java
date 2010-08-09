package rti.tscommandprocessor.commands.ts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Vector;

import RTi.Util.Math.BestFitIndicatorType;
import RTi.Util.Math.DataTransformationType;
import RTi.Util.Math.MathUtil;
import RTi.Util.Math.NumberOfEquationsType;
import RTi.Util.Math.RegressionType;
import RTi.TS.TSRegression;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeUtil;
import RTi.TS.TS;
import RTi.TS.TSDataFlagMetadata;
import RTi.TS.TSUtil;
import RTi.Util.IO.IOUtil;
import RTi.Util.Time.StopWatch;

/**
This class executes the Mixed Station Analysis computing a series of
time series regressions between the dependent and independent time series.
This class relies on the TSRegression object for the regression analysis.
Multiple time series can be analyzed at the same time to promote bulk analysis, and
a report can be created to summarize the results.
*/
public class MixedStationAnalysis
{
    
/**
Value used to indicate no best fit statistic.  This works because all errors will be >= 0 and

*/
private double __MISSING = -999.0;

/**
Dependent time series to be analyzed.
*/
private List<TS> __dependentTSList = null;

/**
Independent time series to be analyzed.
*/
private List<TS> __independentTSList = null;

/**
For each dependent time series, this is a list of TSRegression that were
created for the dependent time series during analysis.  Each TSRegression
is either one equation or monthly equations (but not both).
*/
private List<List<TSRegression>> __dependentTSRegressionList = null;

/**
Regression analysis methods to use (e.g., OLS, MOVE2).
*/
private List<RegressionType> __regressionTypeList = null;

/**
Transformations to use (see TRANSFORM*).
*/
private List<DataTransformationType> __transformationList = null;

/**
Number of equations to use (can be either one or monthly).
*/
private NumberOfEquationsType __numberOfEquations = null;

/**
Analysis start date/time.
*/
private DateTime __analysisStart = null;

/**
Analysis end date/time.
*/
private DateTime __analysisEnd = null;

/**
Confidence level >0 and <100 %, used to discard estimated values.  If null, all estimated values are used.
*/
private Double __confidenceLevel = null;

/**
Best fit indicator.
*/
private BestFitIndicatorType __bestFitIndicator  = null;

/**
Fill flag to pass to fill code.
*/
private String __fillFlag = null;

/**
Fill Start date/time.
*/
private DateTime __fillStart = null;

/**
Fill End date/time.
*/
private DateTime __fillEnd = null;

/**
Intercept when using OLS regression (null or 0 is allowed).
*/
private Double __intercept = null;

/**
Minimum overlapping data count required for acceptable analysis.
*/
private Integer __minimumDataCount = null;

/**
Minimum correlation coefficient value required for acceptable analysis.
*/
private Double __minimumR = null;

String __format12p2f = "%12.2f";
String __format8p2f = "%8.2f";
String __format8p4f = "%8.4f";
String __format7d = "%7d";
String __format12d = "%12d";
String __format7p2f = "%7.2f";

// Member populated by the method rank() and used by the method
// createReportSummary() (and potentially others) to access the ranked
// TSRegressions for each dependent time series.  The dimensions are:
// [dependent][month][independent], where month is 0 for
// evaluating the period, and 0-11 if monthly relationships
private int [][][] __sortedOrder = null;

/**
Construct a MixedStationAnalysis object.  Use analyizeAndRank() and createReport() for full execution.
@param bestFitIndicator the indicator to use when determining which relationship is the best fit.
@param analysisMethodList list of regression analysis methods that should be tried when finding a best fit -
if specified as null, OLS regression will be used.
@param numberOfEquations number of equations to compute, either one or monthly. 
n the future support for seasonal equations may be added.
@param analysisStart Date/time indicating the analysis period start.
@param analysisEnd Date/time indicating the analysis period end.
@param fillStart Date/time indicating the fill period start (used if filled results are generated).
@param fillEnd Date/time indicating the fill period end (used if filled results are generated).
@param transformationList list of data transformations that should be tried when finding a best fit -
if specified as null, then data will be used as is to ensure that some analysis occurs.
@param intercept the intercept y-axis intercept ("A" value in best fit equation) that
should be forced when analyzing the data.  This is currently only implemented
for a transformation of None for OLS regression and can currently only have a
value of 0.  If specified as blank, no intercept is used.  This feature is
typically only used when analyzing data for filling.
@param minimumDataCount the minimum number of overlapping data points that are required for a valid
analysis (N1 in FillRegression() and FillMOVE2() documentation).
If the minimum count is not met, then the independent time series is ignored
for the specific combination of parameters.  For example, if monthly equations
are used, the independent time series may be ignored for the specific month;
however, it may still be analyzed for other months.
@param minimumR minimum correlation coefficient that should be considered a good fit.
@param confidenceLevel the confidence level that is required for filled values.  In other words, if an estimated value
falls outside of the mean +- the confidence level, then don't fill with the value.
@param fillFlag fill flag passed to regression code, to indicate values that are filled
(can be null, one-character, or "Auto").
*/
public MixedStationAnalysis ( List<TS> dependentTSList, List<TS> independentTSList,
    BestFitIndicatorType bestFitIndicator,
    List<RegressionType> regressionTypeList, NumberOfEquationsType numberOfEquations,
    DateTime analysisStart, DateTime analysisEnd,
    DateTime fillStart, DateTime fillEnd,
    List<DataTransformationType> transformationList, Double intercept,
    Integer minimumDataCount, Double minimumR, Double confidenceLevel, String fillFlag )
{
	super ();

	__dependentTSList = dependentTSList;
	__independentTSList = independentTSList;
    __bestFitIndicator = bestFitIndicator;
    __regressionTypeList = regressionTypeList;
    if ( (__regressionTypeList == null) || (__regressionTypeList.size() == 0) ) {
        // Default is OLS regression
        __regressionTypeList = new Vector();
        __regressionTypeList.add ( RegressionType.OLS_REGRESSION );
    }
    __numberOfEquations = numberOfEquations;
	__analysisStart = analysisStart;
	__analysisEnd = analysisEnd;
	__fillStart = fillStart;
	__fillEnd = fillEnd;
	__transformationList = transformationList;
	if ( (__transformationList == null) || (__transformationList.size() == 0) ) {
	    // Default is None
	    __transformationList = new Vector();
	    __transformationList.add ( DataTransformationType.NONE );
	}
	__intercept = intercept;
	__minimumDataCount = minimumDataCount;
	__minimumR = minimumR;
	__confidenceLevel = confidenceLevel;
	__fillFlag = fillFlag;
}

/**
Perform the mixed station analysis, for each dependent time series,
using all independent time series (except the dependent, if in the list), for
all Analysis Methods and Transformations.
@throws Exception in case of error.
*/
private void analyze()
{
	String mthd = "MixedStationAnalysis.analyze", mssg;
    printMemoryStats(mthd,"Before analyzing");

	StopWatch sw = new StopWatch();
	double previousSecCount;
	double currentSecCount;

	int warningCount = 0;

	int nDependentTS = __dependentTSList.size();
	int nIndependentTS = __independentTSList.size();
	int nAnalysisMethods = __regressionTypeList.size();
	int nTransformations = __transformationList.size();

	TS independentTS = null;
	TS dependentTS = null;

	__dependentTSRegressionList = new Vector( nDependentTS );

	List dependentResults = null;

	int initialCapacity = nIndependentTS * nAnalysisMethods * nTransformations;
	
	StringBuffer warningText = new StringBuffer();

	// Loop for each one of the dependent time series
	for ( int iDependentTS = 0; iDependentTS < nDependentTS; iDependentTS++ ) {
	    dependentTS = __dependentTSList.get(iDependentTS);
	    dependentResults = new Vector( initialCapacity );
	    // FIXME SAM 2009-08-30 need process listener
	    //showStatus ( "Analyzing dependent time series \"" + dependentTS.getIdentifierString() +
	     //   "\" (" + (iDependentTS + 1) + " of " + nDependentTS + ")." );

	    // Loop for each one of the independent time series, but skip the the dependent times series
	    // if present in the independent list so that a self-comparison does not occur.
	    for ( int iIndependentTS = 0; iIndependentTS < nIndependentTS; iIndependentTS++ ) {
	    	independentTS = (TS) __independentTSList.get(iIndependentTS);
	    	// Make sure the time series are not the same.
			if ( independentTS == dependentTS) {
				continue;
			}

			// Make sure that both time series have the same interval
			if ( independentTS.getIdentifier().getInterval() != dependentTS.getIdentifier().getInterval() ) {
		     	continue;
			}

 			// Loop for each analysis method, resetting controlling parameters to specify a unique combination.
	    	for ( int iAnalysisMethod = 0; iAnalysisMethod < nAnalysisMethods; iAnalysisMethod++ ) {
	    		// Reset properties for AnalysisMethod,
	    		RegressionType analysisMethod = __regressionTypeList.get(iAnalysisMethod);

	    		// Loop for each transformation...
	    		for ( int iTransformation = 0; iTransformation < nTransformations; iTransformation++ ) {
	    		    DataTransformationType transformation = __transformationList.get(iTransformation);

				    // Only use the intercept if OLS and linear
	    		    Double intercept = null;
				    if ( (analysisMethod == RegressionType.OLS_REGRESSION) &&
				        (transformation == DataTransformationType.NONE) ) {
				        intercept = __intercept;
				    }

				    // Status message
				    mssg = "Computing regression - dependent (" + (iDependentTS+1) + " out of "
			    	+ nDependentTS + ") X Independent (" + (iIndependentTS+1) + " out of " + nIndependentTS + ")";
		    	    setParentStatusText ( mssg );
		    	    Message.printStatus(2, mthd, mssg);

		    	    // Process the TSRegression - note that for monthly all months are processed
		    	    // Later when ranking for monthly, the TSRegression may be ranked differently depending on the
		    	    // month being used.
		    	    previousSecCount = sw.getSeconds();
		    	    sw.start();
		    	    try {
		    	        TSRegression tsRegression = new TSRegression (independentTS, dependentTS,
                            true, // analyze for filling
                            __confidenceLevel,
                            analysisMethod,
                            intercept, __numberOfEquations,
                            null, // include all months in analysis
                            transformation,
                            __analysisStart, __analysisEnd, // Dependent analysis
                            __analysisStart, __analysisEnd, // Independent analysis (same as dependent)
                            __fillStart, __fillEnd );
		    	         if ( analyzeOkToAddResults(tsRegression, __numberOfEquations, __minimumDataCount,
		    	             __minimumR, true) ) {
    		         	  	 dependentResults.add ( tsRegression );
    		    	    	 // Free resources that won't be used - this is necessary because sometimes when
    		    	    	 // processing many time series the memory runs out.
    		    	    	 tsRegression.freeResources();
		    	         }
		    	    }
		    	    catch ( Exception e ) {
		    	    	Message.printWarning( 3, mthd, e );
		    	    	warningText.append ( "\n" + e );
		    	    	++warningCount;
		    	    }
		    	    sw.stop();
		    	    currentSecCount = sw.getSeconds();
		    	    double elapsed = currentSecCount - previousSecCount;
		    	    Message.printStatus ( 2, mthd, "Dependent \"" + dependentTS.getIdentifierString() +
		    	        "\" (" + (iDependentTS + 1) + " of " + nDependentTS + "), Independent \"" +
		    	        independentTS.getIdentifierString() + "\" ( " + (iIndependentTS + 1) + " of " +
		    	        nIndependentTS + "), Elapsed (sec) " + StringUtil.formatString(elapsed,"%.3f") );
		    	}
	    	}
	    }
	    __dependentTSRegressionList.add ( dependentResults ); // List of TSRegression for the dependent
	    Message.printStatus ( 2, mthd, "Have " + dependentResults.size() +
	        " results to rank for dependent TS \"" + dependentTS.getIdentifierString() + "\"" );
	}
	
	printMemoryStats(mthd,"After analyzing");

	if ( warningCount > 0 ) {
		// Try to save the partial report and throw an exception.
		mssg = warningCount + " error(s) performing Mixed Station Analysis (" + warningText
		    + ").  Check the log file for additional information.";
		throw new RuntimeException (mssg);
	}

	sw = null;
}

/**
Analyze and rank the results.
*/
public void analyzeAndRank ()
{   analyze();
    rank();
}

/**
Evaluate whether a TSRegression relationship is OK to add to results or otherwise process, given filter criteria.
@param partialOk if monthly an any values meet the cutoff, then add.  This is because monthly results are
stored in one TSRegression object and good results will need to be extracted for specific months.
*/
public boolean analyzeOkToAddResults(TSRegression tsRegression, NumberOfEquationsType numberOfEquations,
    Integer minimumDataCount, Double minimumR, boolean partialOk )
{
    boolean okToAdd = true;
    // Use only if regression was properly analyzed
    int monthOkCount = 0;
    if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
        for ( int imon = 1; imon <= 12; imon++ ) {
            if ( tsRegression.isAnalyzed( imon ) ) {
                ++monthOkCount;
            }
        }
        if ( (partialOk && monthOkCount == 0) || (!partialOk && monthOkCount != 12) ) {
            // Not enough data
            okToAdd = false;
        }
    }
    else {
        if ( !tsRegression.isAnalyzed() ) {
            okToAdd = false;
        }
    }
    if ( !okToAdd ) {
        return false;
    }
    
    // Fill only if the minimum R is satisfied.
    
    monthOkCount = 0;
    if ( minimumR != null  ) {
        if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
            for ( int imon = 1; imon <= 12; imon++ ) {
                try {
                    if ( tsRegression.getCorrelationCoefficient( imon ) >= minimumR.doubleValue()) {
                        ++monthOkCount;
                    }
                }
                catch ( Exception e ) {
                    // Don't increment
                }
            }
            if ( (partialOk && monthOkCount == 0) || (!partialOk && monthOkCount != 12) ) {
                // Not enough data
                okToAdd = false;
            }
        }
        else {
            if ( tsRegression.getCorrelationCoefficient() < minimumR.doubleValue() ) {
                okToAdd = false;
            }
        }
    }
    if ( !okToAdd ) {
        return false;
    }

    // Use only if the number of data points used in the analysis was >= to MinimumDataCount

    monthOkCount = 0;
    if ( minimumDataCount != null ) {
        if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
            for ( int imon = 1; imon <= 12; imon++ ) {
                try {
                    if ( tsRegression.getN1( imon ) >= minimumDataCount.intValue()) {
                        ++monthOkCount;
                    }
                }
                catch ( Exception e ) {
                    // Don't increment OK count
                }
            }
            if ( (partialOk && monthOkCount == 0) || (!partialOk && monthOkCount != 12) ) {
                // Not enough data
                okToAdd = false;
            }
        }
        else {
            if ( tsRegression.getN1() < minimumDataCount.intValue()) {
                okToAdd = false;
            }
        }
    }
    return okToAdd;
}

// FIXME SAM 2009-08-29 Need to evaluate this - need to pass in the equation coefficients directly because
// using multiple fill commands on the same time series will cause the coefficients to recompute, each time
// considering more filled data, rather than the original raw data values.
/**
Creates the FillRegression() and FillMOVE2() commands for each combination of
dependent, independent, transformation and analysis method, for the ranked relationships.
This is a brute force way of applying the filling.  However, the problem is that it cannot be implemented as
simply as this because each command recomputes relationships based on the previous fill information and
therefore does not use the original data.  The FillMixedStation() command that handles all the relationships
internally does use the originally-computed relationships.
@return a list containing command strings.
*/
public List<String> createFillCommands()
{
	String mthd = "MixedStationAnalysis.createFillCommands";

	TS IndependentTS = null;
	TS DependentTS   = null;
	String Transformation = "";
	String NumberOfEquations = "";
	String DependentAnalysisStart = "";
	String DependentAnalysisEnd = "";
	String IndependentAnalysisStart = "";
	String IndependentAnalysisEnd = "";
	String FillStart = "";
	String FillEnd = "";
	String Intercept = "";

	// __MinimumDataCount as integer.
	int MinimumDataCount = __minimumDataCount.intValue();

	// __MinimumR as integer.
	double MinimumR = __minimumR.doubleValue();

	List<String> commands_Vector = new Vector();

    try {
    	// Loop for each one of the dependent time series
    	int nDependent = __dependentTSRegressionList.size();
    
    	String fillCommand = null;
    
    	for ( int dep = 0; dep < nDependent; dep++ ) {
     		// Using the same code to deal with monthly and single equation
    		// For single equation set the variable nMonth to 1.
    		int nMonth = 1;
    		if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
    		    nMonth = 12;
    		}
    
    		for ( int month = 1; month <= nMonth; month++ ) {
                // Get the independent list (regressions) for this dependent - these are in the order of the
                // independent time series (not ranked
                List<TSRegression> independentList = getRankedTSRegressionList(dep, (month - 1) );

    			for ( TSRegression tsRegression : independentList ) {
    
    	    		// Use only if regression was properly analyzed
    	    		if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
    	    			if ( !tsRegression.isAnalyzed( month ) ) {
    	    			    continue;
    	    			}
    				}
    	    		else {
    					if ( !tsRegression.isAnalyzed() ) {
    					    continue;
    					}
    				}
    
    				// Use only if the number of data points used in the analysis was >= to MinimumDataCount
    				int n;
    				try {
    	    	    	if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
    						n = tsRegression.getN1( month );
    					}
    	    	    	else {
    						n = tsRegression.getN1();
    					}
    				} catch ( Exception e ) {
    				    // FIXME SAM 2009-06-15 Handle better
    					continue;
    				}
    				if ( n < MinimumDataCount ) {
    					continue;
    				}
    
    				// Use only if the correlation coefficient is greater than the MinimumR
    				double r;
    				try {
    	    	    	if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
    						r = tsRegression.getCorrelationCoefficient( month );
    					}
    	    	    	else {
    						r = tsRegression.getCorrelationCoefficient();
    					}
    				} catch ( Exception e ) {
    				    // FIXME SAM 2009-06-15 Handle better
    					continue;
    				}
    				if ( r < MinimumR ) {
    					continue;
    				}
    
    				// Get the Dependent time series
    		    	DependentTS = tsRegression.getDependentTS();
    		    	String DependentTSID = DependentTS.getAlias();
    		    	if ( DependentTSID.length() == 0 ) {
    		    		DependentTSID = DependentTS.getIdentifierString();
    		    	}
    
    				// Get the Independent TS
      	    		IndependentTS = tsRegression.getIndependentTS();
    	    		String IndependentTSID=IndependentTS.getAlias();
    	    		if ( IndependentTSID.length() == 0 ) {
    	    			IndependentTSID = IndependentTS.getIdentifierString();
    	    		}
    
    	    	    // Number of equations
    			  	NumberOfEquations = "" + tsRegression.getNumberOfEquations();
    
    	    		// Get the Transformation
    	    		Transformation = "" + tsRegression.getTransformation();
    
    	  		  	// Get AnalysisMethod
      	    		RegressionType analysisMethod = tsRegression.getAnalysisMethod();
    
      	    		// Get the dependent analysis period start and end
      	    		DependentAnalysisStart = "" + tsRegression.getDependentAnalysisStart();
      	    		DependentAnalysisEnd = "" + tsRegression.getDependentAnalysisEnd();
    
      	    		// Get the independent analysis period start and end
      	    		IndependentAnalysisStart = "" + tsRegression.getIndependentAnalysisStart();
      	    		IndependentAnalysisEnd = "" + tsRegression.getIndependentAnalysisEnd();
    
      	    		// Get the fill period start and end
      	    		FillStart = "" + tsRegression.getFillStart();
      	    		FillEnd = "" + tsRegression.getFillEnd();
    
      	    		// Building the command (FillRegression or FillMOVE2)
    				if ( analysisMethod == RegressionType.OLS_REGRESSION ) {
      	    			fillCommand = "FillRegression";
      	    		}
    				else if ( analysisMethod == RegressionType.MOVE2 ) {
    				    fillCommand = "FillMOVE2";
      	    		}
    
    				// StringBuffer for the command parameters
    				StringBuffer b = new StringBuffer();
    
    				// Adding the DependentTSID
    				if ( DependentTSID.length() > 0 ) {
    					if ( b.length() > 0 ) {
    						b.append ( "," );
    					}
    					b.append ( "DependentTSID=\"" + DependentTSID + "\"" );
    				}
    
    				// Adding the IndependentTSID
    				if ( IndependentTSID.length() > 0 ) {
    					if ( b.length() > 0 ) {
    						b.append ( "," );
    					}
    					b.append ( "IndependentTSID=\"" + IndependentTSID + "\"" );
    				}
    
    				// Adding the NumberOfEquations
    				if ( NumberOfEquations.length() > 0 ) {
    					if ( b.length() > 0 ) {
    						b.append ( "," );
    					}
    					b.append ( "NumberOfEquations=" + NumberOfEquations );
    				}
    
    				// Adding the AnalysisMonth
    				if ( b.length() > 0 ) {
    					b.append ( "," );
    				}
    				b.append ( "AnalysisMonth=" + String.valueOf(month) );
    
    				// Adding the Transformation
    				if ( Transformation.length() > 0 ) {
    					if ( b.length() > 0 ) {
    						b.append ( "," );
    					}
    					b.append ( "Transformation=" + Transformation );
    				}
    
    				if ( analysisMethod == RegressionType.OLS_REGRESSION ) {
    
    				    // Adding the AnalysisStart
    				    if ( DependentAnalysisStart.length() > 0 ) {
    				    	if ( b.length() > 0 ) {
    				    		b.append ( "," );
    				    	}
    				    	b.append ( "AnalysisStart=\"" + DependentAnalysisStart + "\"" );
    				    }
    
    				    // Adding the AnalysisEnd
    				    if ( DependentAnalysisEnd.length() > 0 ) {
    				    	if ( b.length() > 0 ) {
    				    		b.append ( "," );
    				    	}
    				    	b.append ( "AnalysisEnd=\"" + DependentAnalysisEnd + "\"");
    				    }
    
    				}
    				else {
    				    // Adding the DependentAnalysisStart
    				    if ( DependentAnalysisStart.length() > 0 ) {
    				    	if ( b.length() > 0 ) {
    				    		b.append ( "," );
    				    	}
    				    	b.append ( "DependentAnalysisStart=\"" + DependentAnalysisStart + "\"");
    				    }
    
    				    // Adding the DependentAnalysisEnd
    				    if ( DependentAnalysisEnd.length() > 0 ) {
    				    	if ( b.length() > 0 ) {
    				    		b.append ( "," );
    				    	}
    				    	b.append ( "DependentAnalysisEnd=\"" + DependentAnalysisEnd + "\"");
    				    }
    				    // Adding the IndependentAnalysisStart
    				    if ( IndependentAnalysisStart.length()>0 ) {
    				    	if ( b.length() > 0 ) {
    				    		b.append ( "," );
    				    	}
    				    	b.append ( "IndependentAnalysisStart=\"" + IndependentAnalysisStart + "\"");
    				    }
    
    				    // Adding the IndependentAnalysisEnd
    				    if ( IndependentAnalysisEnd.length() > 0 ) {
    				    	if ( b.length() > 0 ) {
    				    		b.append ( "," );
    				    	}
    				    	b.append ( "IndependentAnalysisEnd=\"" + IndependentAnalysisEnd + "\"");
    				    }
    				}
    
    				// Adding the FillStart
    				if ( FillStart.length() > 0 ) {
    					if ( b.length() > 0 ) {
    						b.append ( "," );
    					}
    					b.append ( "FillStart=\"" + FillStart + "\"");
    				}
    
    				// Adding the FillEnd
    				if ( FillEnd.length() > 0 ) {
    					if ( b.length() > 0 ) {
    						b.append ( "," );
    					}
    					b.append ( "FillEnd=\"" + FillEnd + "\"");
    				}
    
    				// Adding the Intercept
    				if ( Intercept.length() > 0 ) {
    					if ( b.length() > 0 ) {
    						b.append ( "," );
    					}
    					b.append ( "Intercept=" + Intercept );
    				}
    
    	  	    	// Build the command, update the __command_Vector
    				String command = fillCommand + "(" + b.toString() + ")";
    				commands_Vector.add ( command );
    				Message.printStatus ( 2, mthd, command );
    
      	    		// Done with this month (or all)
      	    		break;
    			}
    	    }
    	}
    }
    catch ( Exception e ) {
    	Message.printWarning( 3, mthd, e );
    }

    return commands_Vector;
}

/**
Create the report with results.
@param outputFileFull path to output file to create.
@param outputCommentsList list of comments to add to the top of the header (typically information about
database versions, etc. used to supply data).
@param maxResultsPerIndependent maximum number of results to show for each independent time series (to limit
the report to a reasonable length and complexity).
*/
public void createReport ( File outputFileFull, List<String>outputCommentsList, Integer maxResultsPerIndependent )
throws FileNotFoundException
{
    // Original code was written to create parts of the report as string lists and then save at the end
    
    String nl = System.getProperty ( "line.separator" );
    StringBuffer header = createReportHeaderText( maxResultsPerIndependent, nl );
    StringBuffer statistics = createReportUnrankedText(nl);
    StringBuffer summary = createReportRankedText( maxResultsPerIndependent, nl );
    saveReportText( outputFileFull, outputCommentsList, header, statistics, summary, nl);
    
    // TODO SAM 2009-08-30 if implemented to use HTML, will write as formatting occurs
}

/**
@param buffer the report buffer to append to
@param depend identifier for dependent time series
@param nl newline to use, based on environment
*/
private void createReportAppendTableHeader ( StringBuffer buffer, String depend, TSRegression tsRegression,
    String nl )
{
    String DependentAnalysisStart = null;
    String DependentAnalysisEnd = null;
    String FillEnd = null;
    String FillStart = null;
    String Intercept = null;
    String NumberOfEquations = null;

    buffer.append ( "Dependent Time Series: " + depend + nl );

    // Dependent Period.
    DependentAnalysisStart = "" + tsRegression.getDependentAnalysisStart();
    DependentAnalysisEnd = "" + tsRegression.getDependentAnalysisEnd();
    buffer.append ( "Dependent analysis period: from \""
        + DependentAnalysisStart + "\" to \"" + DependentAnalysisEnd   + "\"" + nl );

    // Fill Period.
    FillStart = "" + tsRegression.getFillStart();
    FillEnd = "" + tsRegression.getFillEnd();
    buffer.append ( "Fill period: from \"" + FillStart + "\" to \"" + FillEnd + "\"" + nl );

    // Intercept
    Intercept = "" + tsRegression.getIntercept();
    buffer.append ( "Intercept (only if OLS no transformation): ");
    if ( (tsRegression.getIntercept() == null) || Intercept.equalsIgnoreCase( "-999.0" ) ) {
        buffer.append ( nl );
    }
    else {
        buffer.append ( Intercept + nl );
    }

    // Number of equations
    NumberOfEquations = "" + tsRegression.getNumberOfEquations();
    buffer.append ( "Number of equations: " + NumberOfEquations + nl);

    // Minimum data count
    // Not available from tsRegression since added by this class
    buffer.append ( "Minimum data count (N1): " + __minimumDataCount + nl);

    // Minimum R
    // Not available from tsRegression since added by this class - should always have a value
    buffer.append ( "Minimum r: " + __minimumR + nl);

    // Best fit indicator (used here only)
    // Not available from tsRegression since added by this class.
    buffer.append ( "Best fit indicator: " + __bestFitIndicator + nl + nl);
}

/**
Create the report header, containing the description of the sections in the report
@param maxResultsPerIndependent maximum number of results per independent time series to show
@param nl newline to use, based on environment
*/
private StringBuffer createReportHeaderText ( Integer maxResultsPerIndependent, String nl )
{
	String mthd = "MixedStationAnalysis.createReportHeader";
    StringBuffer header = new StringBuffer ();
    String cnl = "#" + nl;
    try {
		// Description of the reports.
		header.append ( cnl + cnl );
		header.append ( "# Mixed Station Analysis Report Format:" + nl );
		header.append ( "#" + nl );
		header.append ( "# Mixed Station Analysis Summary" + nl );
		if ( maxResultsPerIndependent == null ) {
		    header.append ( "#       Summary, listed by dependent time series" );
		}
		else {
		    header.append ( "#       Summary, listed by dependent time series, with the top " + maxResultsPerIndependent );
		}
		if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
			header.append ( nl + "#       best fit results listed per dependent and month." + nl);
		}
		else {
			header.append ( nl + "#       best fit results listed per dependent." + nl);
		}
		header.append ( cnl );
		header.append ( "# Mixed Station Analysis Details" + nl );
		if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
			header.append ( "#       This section contains three tables per dependent time series:" + nl
			+ "#          The first lists the number of points used in the analysis." + nl
			+ "#          The second lists the correlation coeficients." + nl
			+ "#          The third lists the SEP values." + nl
			+ "#       Each table lists the results per combination of independent," + nl
			+ "#       transformation and analysis method." + nl );
		}
		else {
			header.append (
			  "#       This section contains one table per dependent time series." + nl
			+ "#       It lists the results per combination of independent," + nl
			+ "#       transformation and analysis method." + nl );
		}

		header.append ( nl + nl );
		return header;
	}
	catch ( Exception e ) {
		Message.printWarning( 3, mthd, e );
	}
	return header;
}

/**
Create the section of the report containing the unranked results from the regressions, which is listed in the
order of independent time series and variations on regression parameters.
@param nl newline to use, based on environment
*/
private StringBuffer createReportUnrankedText ( String nl )
{
	String mthd = "MixedStationAnalysis.createReportStatistics";

	// TSRegression properties
	String IndependentAnalysisEnd = null;
	String IndependentAnalysisStart	= null;

	List independentList = null;
	TSRegression tsRegression = null;
	TS dependentTS = null;
	TS independentTS = null;

	String line;

    StringBuffer statistics = new StringBuffer ();
    
	try {

		StringBuffer sDataCount = null;
		StringBuffer sCorrelation = null;
		StringBuffer sRMSE = null;

		int dependentStatisticsSize = __dependentTSRegressionList.size();

		for ( int dp = 0; dp < dependentStatisticsSize; dp++ ) {
			sDataCount = new StringBuffer ();
			sCorrelation = new StringBuffer ();
			sRMSE = new StringBuffer ();

			// Get the regression list for this dependent time series.
			independentList = (List)__dependentTSRegressionList.get( dp );

			// Find out the number of regression analysis for this dependent time series
			int independentListSize = independentList.size();

			// Loop through the regression for this dependent time series and create the report.
			tsRegression = null;
			boolean firstTime = true;
			tsRegression = null;

			sDataCount.append ( nl + nl );
			sCorrelation.append ( nl  );
			sRMSE.append ( nl  );

			String previousIndependent = "";

			String endLine3Data = "";
			String endLine3Corr = "";
	    	String endLine3RMSE = "";

			for ( int rl = 0; rl < independentListSize; rl++ ) {

    			// Alternative to the report below.
    			//sCorrelation.append(tsRegression.toString());

				// Get the regression object.
				tsRegression = (TSRegression)independentList.get(rl);
			    NumberOfEquationsType numberOfEquations = tsRegression.getNumberOfEquations();
			    RegressionType analysisMethod = tsRegression.getAnalysisMethod();
			    DataTransformationType transformation;

    			// If first time create the header
				if ( firstTime ) {
					// Using the TSRegression stored in the __dependentStatisticsVector
					// create the analysis summary, saving it in the output file.
					statistics.append( nl +
					    "# Mixed Station Analysis Details - in order of independent time series, not ranked");

    				// Dependent time series
    				dependentTS = tsRegression.getDependentTS();
    				String depend = dependentTS.getIdentifierString();
	    			//	String depend = dependentTS.getAlias();
	    			//	if ( depend.length() == 0 ) {
	    			//		depend = dependentTS.getLocation();
	    			//	}

		  	    	// Append the report header
    				createReportAppendTableHeader (sDataCount, depend, tsRegression, nl );

	   				// Preparing for table headers
    				String monthsA = " ";
    				for ( int i = 1; i <= 12; i++ ) {
    					monthsA += "   " + TimeUtil.monthAbbreviation(i) + " ";
    				}
    				String monthsB = " ";
    				for ( int i = 1; i <= 12; i++ ) {
    					monthsB += "        " + TimeUtil.monthAbbreviation(i) + " ";
    				}

    				String endLine1Data = "";
    				String endLine2Data = "";
    				String endLine1Corr = "";
    				String endLine2Corr = "";
    				String endLine1RMSE = "";
    				String endLine2RMSE = "";

    				if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
  	    				endLine1Data =  "    " + "Number of points (N1)" + nl;
  	    				endLine2Data = monthsA + nl;
  	    				endLine3Data = "-------------------------------------------------"
    				    + "------------------------------------------------------------------"
    				    + "-------------------------------------" + nl;

    				   	endLine1Corr = "    Correlation Coefficients (r)" + nl;
  	    				endLine2Corr = monthsA + nl;
  	    				endLine3Corr = "------------------------------------------------------"
    				    + "------------------------------------------------------------------"
    				    + "--------------------------------" + nl;

  	    				endLine1RMSE =  "    SEP" + nl;
  	    				endLine2RMSE = monthsB + "|   SEP Total" + nl;
  	    				endLine3RMSE = "-------------------------------------------------"
    				    + "------------------------------------------------------------------"
    				    + "----------------------------------------------------------" + nl;
  	    			}
    				else {
  	    				endLine1Data = " # of Points | Correlation |   SEP Total" + nl;
    					endLine2Data = "        (N1) |  Coeff. (r) |            " + nl;
    					endLine3Data = "----------------------------------------------"
    				    	+ "--------------------------------------------------------------" + nl;
  	    			}

    				// --------- sDataCount Table header
    				// First line
    				sDataCount.append( "  Independent | Transfor-|      Analysis |   Analysis |   Analysis |" + endLine1Data );
    				// Second line
    				sDataCount.append( "  Time Series |   mation |        Method |      Start |        End |" + endLine2Data );

    				if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
  	    			    // ------- sCorrelation Table header
  	    			    // First line
    				    sCorrelation.append ( "  Independent | Transfor-|      Analysis |   Analysis |   Analysis |" + endLine1Corr );
    				    // Second line
    				    sCorrelation.append ( "  Time Series |   mation |        Method |      Start |        End |" + endLine2Corr );

    				    // -------------- sRMSE Table header
    				    // First line
    				    sRMSE.append ( "  Independent |" + endLine1RMSE );
    				    // Second line
    				    sRMSE.append ( "  Time Series |" + endLine2RMSE );
    				}

    				firstTime = false;
	    		}

  	    		// Get the independent TS
  	    		independentTS = tsRegression.getIndependentTS();
	    		String indep = independentTS.getAlias();
	    		if ( indep.length() == 0 ) {
	    			indep = independentTS.getLocation();
	    		}
	    	   	if(!previousIndependent.equalsIgnoreCase(indep)) {
				    previousIndependent = indep;
        			// Table header 3rd line & group divider
        			sDataCount.append ( endLine3Data );
    				sCorrelation.append ( endLine3Corr );
    				sRMSE.append ( endLine3RMSE );
    			}

    	    	// Add the independent TS to the line.
  	    		line = StringUtil.formatString( indep,"%13.13s" ) + " |";

				// Add Transformation to the correlation table
                transformation = tsRegression.getTransformation();
	  		  	line += StringUtil.formatString(""+transformation,"   %6.6s |");

	  		  	// Get the AnalysisMethod

  	    		// Add the Analysis Method to the correlation table
                analysisMethod = tsRegression.getAnalysisMethod();
                line += StringUtil.formatString(""+analysisMethod," %13.13s |");

				// Add the independent time series period
  	    		IndependentAnalysisStart = "" + tsRegression.getIndependentAnalysisStart();
  	    		IndependentAnalysisEnd = "" + tsRegression.getIndependentAnalysisEnd();
  	    		line += StringUtil.formatString(IndependentAnalysisStart," %10.10s |");
	  		  	line += StringUtil.formatString(IndependentAnalysisEnd," %10.10s |");

				// Save this portion of the line for use also by the SDataCount table.
				String dataCountLine = line;

  	    		if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
  	    			// Adding Data Count
  	    			for ( int m=1; m<=12; m++ ) {
  	    			    try {
  	    			        int n1 = tsRegression.getN1(m);
  	    				    dataCountLine += StringUtil.formatString(n1, __format7d );
  	    			    }
  	    			    catch ( Exception e ) {
  	    			        dataCountLine += "    ***";
  	    			    }
  	    			}
  	    			dataCountLine += nl;
  	    			sDataCount.append (dataCountLine);

  	    			// Adding Correlations Coefficients
  	    			for ( int m=1; m<=12; m++ ) {
  	    			    try {
  	    			        if (tsRegression.isAnalyzed(m)) {
      	    			        double cc = tsRegression.getCorrelationCoefficient(m);
      	    				    line += StringUtil.formatString(cc, __format7p2f );
	  	    			    }
	  	    			    else {
	  	    			      	line += "    ...";
	  	    			    }
  	    			    }
  	    			    catch ( Exception e ) {
  	    			        line += "    ***";
  	    			    }
  	    			}
  	    			line += nl;
  	    			sCorrelation.append (line);

  	    			// Adding the independent TS to the line for the RMSE table.
  	    			line = StringUtil.formatString(indep, "%13.13s") + " |";

  	    			// Adding the monthly RMSE
  	    		    for ( int m=1; m<=12; m++ ) {
  	    			    try {
  	    			        if (tsRegression.isAnalyzed(m)) {
	  	    				    double rmse = tsRegression.getRMSE(m);
	  	    				    line += StringUtil.formatString(rmse, __format12p2f );
	  	    			    }
  	    			        else {
  	    			            line += "         ...";
  	    			        }
  	    			    }
  	    			    catch ( Exception e ) {
  	    			        line += "         ***";
  	    			    }
  	    		    }

    		        line += " |";

    		        // Adding the total RMSE
    		        try {
    		           if (tsRegression.isAnalyzed()) {
    		               double rmse = tsRegression.getRMSE();
    		               line += StringUtil.formatString(rmse, __format12p2f );
    		           }
    		           else {
    		               line += "         ...";
    		           }
    		        }
    		        catch ( Exception e ) {
    		            line += "         ***";
    		        }

  	    			line += nl;
	    			sRMSE.append (line);
	  	    	}
	  	    	else {
					String f = "";

  	    			// Adding the Data Count
  	    			try {
  	    				f = __format12d +" |";
  	    			    int n1 = tsRegression.getN1();
  	    			    line += StringUtil.formatString(n1, f );
  	    			}
  	    			catch ( Exception e ) {
  	    			    line += "         *** |";
  	    			}
  	    			sDataCount.append (line);

  	    			// Adding Correlation Coefficient
  	    			line="";
  	    			try {
  	    			    if ( tsRegression.isAnalyzed() ) {
  	    		 	        f = __format12p2f + " |";
  	    			        double cc = tsRegression.getCorrelationCoefficient();
  	    			        line += StringUtil.formatString( cc, f );
  	    			    }
  	    			    else {
  	    			  	    line += "         ... |";
  	    			    }
  	    			}
  	    			catch ( Exception e ) {
  	    			    line += "         *** |";
  	    			}
  	    			sDataCount.append (line);

  	    			// Adding the RMSE
    		    	line = "";
    		    	try {
    		    	    if ( tsRegression.isAnalyzed () ) {
    		    	        double rmse = tsRegression.getRMSE();
    		    	        line += StringUtil.formatString(rmse, __format12p2f ) + nl;
  	    			    }
  	    		    	else {
  	    			    	line += "         ..." + nl;
  	    			    }
    		    	}
    		    	catch ( Exception e ) {
    		    	    line += "         ***" + nl;
    		    	}
    		    	sDataCount.append (line);
  	    		}
			}

			// Append sDataCount to the __sStatistics table
			// For the "all" case all the information is in the
			// temporary sDataCount. for the monthly case the
			// temporary sCorrelation and sRMSE need to be added.
	    	statistics.append ( sDataCount );
			if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
	    		statistics.append ( sCorrelation );
	    		statistics.append ( sRMSE );
			}
			statistics.append ( nl );
		}
	}
	catch ( Exception e ) {
	    // FIXME SAM 2009-06-15 Log it
		Message.printWarning( 3, mthd, e );
	}

	statistics.append ( nl + nl );
	
	return statistics;
}

/**
Create the third section of the report, containing the final summary.
@param maxResultsPerIndependent maximum number of regression results per independent to print.
@param nl newline to use, based on environment
*/
private StringBuffer createReportRankedText( Integer maxResultsPerIndependent, String nl )
{
	String mthd = "MixedStationAnalysis.createReportSummary", mssg;

	// __MinimumDataCount as integer.
	int MinimumDataCount = __minimumDataCount.intValue();

	// __MinimumR as double.
	double MinimumR = __minimumR.doubleValue();

	// TSRegression properties
	RegressionType analysisMethod;
	DateTime IndependentAnalysisEnd;
	DateTime IndependentAnalysisStart;
	NumberOfEquationsType numberOfEquations;
	DataTransformationType transformation;

	TS dependentTS = null;
	TS independentTS = null;

	String line;
	
    StringBuffer summary = new StringBuffer ();

    try {
    	int nDependent = __dependentTSRegressionList.size();
    
    	String previousDependent = "";
    
    	// Loop the dependent objects
    	for ( int dep = 0; dep < nDependent; dep++ ) {
    		// Using the same code to deal with monthly and single equation
    		// For single equation set the variable nMonth to 1.
    		int nEquation = 1;
    		if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
    		    nEquation = 12;
    		}
    
    		int previousMonth = 0;
    		boolean firstTime = true;
    
    		mssg = nl + "# Mixed Station Analysis Summary";
            if ( maxResultsPerIndependent != null ) {
                mssg += nl + "# Top " + maxResultsPerIndependent +
                    " independent best fit relationships are listed per dependent";
    		}
    		else {
    			mssg += nl + "# All independent best fit relationships are listed per dependent";
    		}
    		if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
    			mssg += " and month" + nl;
    		}
    		else {
    			mssg += nl;
    		}
    		mssg += nl;
    
    		summary.append ( mssg );
    
    		// Using TSRegression stored in the __dependentStatisticsVector
    		// create the analysis summary, saving it in the output file.
    		for ( int iEquation = 1; iEquation <= nEquation; iEquation++ ) {
    			// Loop through the regression for this dependent time series and create the report.
    
    			boolean firstIndependent = true;
    			int independentCount = 0;
    			
    			// Get the list of ranked regression relationships
    			List<TSRegression> tsRegressionList = getRankedTSRegressionList ( dep, (iEquation - 1) );

    			int rank = 0;
    			for ( TSRegression tsRegression : tsRegressionList ) {
    			    ++rank;
    				// Dependent time series
			    	dependentTS = tsRegression.getDependentTS();
			    	String depend = dependentTS.getIdentifierString();
    			    //	String depend = dependentTS.getAlias();
    			    // if ( depend.length() == 0 ) {
    			    //		depend = dependentTS.getLocation();
    			    //	}
    
			    	// Append the report header
    				if ( !previousDependent.equalsIgnoreCase(depend ) ) {
    					previousDependent = depend;
    					createReportAppendTableHeader ( summary, depend, tsRegression, nl );
    			    }
    
    			    // Number of equations
    			  	numberOfEquations = tsRegression.getNumberOfEquations();
    
    		 		// If first time create the header
    				if ( firstTime ) {
	    				// ---------- __sSummary Table header
	    				// First line
	    				summary.append ( "    |     |      |  Independent | Transfor-|      Analysis |   Analysis |   Analysis |" +
	    				    "                    Analysis Statistics                    |" + nl );

	    				// __sSummary Table Second line
	    				summary.append ( "Rank|     | Flag |  Time Series |   mation |        Method |      Start |        End |" +
	    					"     N1     N2       r         SEP           A           B |   SEP Total" + nl);
	    				firstTime = false;
    	    		}
    	    		// Table header 3rd line or month divider
    	    		if ( previousMonth != iEquation ) {
    	    		    summary.append ( "--------------------------------------------------------------------------------------" +
    	    		    	"------------------------------------------------------------------------" + nl);
    			    	previousMonth = iEquation;
    	    		}
    
        	    	// List this result only if not constrained by a maximum count on output results
    				if ( (maxResultsPerIndependent != null) &&
    				    (independentCount >= maxResultsPerIndependent.intValue()) ) {
    					continue;
    				}
        
        	    	// List this independent only if the regression was properly analyzed.
    	    		if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
    	    			if ( !tsRegression.isAnalyzed( iEquation ) ) {
    	    			    continue;
    	    			}
        			}
    	    		else {
    					if ( !tsRegression.isAnalyzed() ) {
    					    continue;
    					}
    				}
        
        			// List this independent only if the number of data points used to compute the regression
    	    		// was greater or equal to MinimumDataCount
    	    		if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
        				try {
        					int n = tsRegression.getN1(iEquation );
        					if ( n < MinimumDataCount ) {
        						continue;
        					}
        				}
        				catch ( Exception e ) {
        				    // FIXME SAM 2009-06-15 log
        					continue;
        				}
    				}
    	    		else {
    					try {
    						int n = tsRegression.getN1();
    						if ( n < MinimumDataCount ) {
    							continue;
    						}
    					}
    					catch ( Exception e ) {
                            // FIXME SAM 2009-06-15 log
    						continue;
    					}
    				}
    
    				// Use only if the correlation coefficient is greater than the MinimumR
    				double r;
    				try {
    	    			if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
	    	    			if(tsRegression.isAnalyzed( iEquation )) {
        						r = tsRegression.getCorrelationCoefficient(	iEquation );
        					}
    	    	    		else {
        					  	continue;
        					}
    					}
    	    			else {
    					    if(tsRegression.isAnalyzed()) {
    						    r = tsRegression.getCorrelationCoefficient();
    					    }
    					    else {
    					  	    continue;
    					    }
    					}
    				}
    				catch ( Exception e ) {
    					continue;
    				}
    				if ( r < MinimumR ) {
    					continue;
    				}
    
    				// If here then output should be created
                    
                    line = "";
                    
                    // Rank, since the results have been sorted.
    				line += StringUtil.formatString(rank,"%4d|");
    
    	    		// Month or Year
    	    		if ( firstIndependent ) {
    					if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
    						line += " " + StringUtil.formatString(TimeUtil.monthAbbreviation(iEquation),"%3.3s") + " |";
	    				}
    					else {
	    					line += " All |";
	    				}
	    				firstIndependent = false;
	    			}
    	    		else {
	    				line += "     |";
	    			}
    	    		
    	    		// Flag for regression - used to provide data flag meta-data for time series
    	    		
                    if ( (__fillFlag == null) || !__fillFlag.equalsIgnoreCase("Auto") ) {
                        line += StringUtil.formatString(__fillFlag,"%-6.6s") + "|";
                    }
                    else {
    	    		    line += StringUtil.formatString(determineFillFlag(nEquation, iEquation, rank),"%-6.6s") + "|";
    	    		}
    
      	    		// Get the independent TS
      	    		independentTS = tsRegression.getIndependentTS();
    	    		String indep = independentTS.getAlias();
    	    		if ( indep.length() == 0 ) {
    	    			indep = independentTS.getLocation();
    	    		}

    	    		// Add the independent TS to the table
    	    		line += StringUtil.formatString( indep,"%13.13s") + " |";
    
    				// Get the Transformation
    	    		transformation = tsRegression.getTransformation();
    				// Add Transformation to the table
    	  		  	line += StringUtil.formatString(""+transformation,"   %6.6s |");
    
    	  		  	// Add the Analysis Method
	  	    		analysisMethod = tsRegression.getAnalysisMethod();
	  	    		line += StringUtil.formatString(""+analysisMethod," %13.13s |");
    
    				// Add the independent time series period to the table
	  	    		IndependentAnalysisStart = tsRegression.getIndependentAnalysisStart();
	  	    		IndependentAnalysisEnd = tsRegression.getIndependentAnalysisEnd();
	  	    		line += StringUtil.formatString(""+IndependentAnalysisStart," %10.10s |");
    	  		  	line += StringUtil.formatString(""+IndependentAnalysisEnd," %10.10s |");
    
	  	    		// Add the statistics (N1, r, SEP, A, B, SEP Total)
	  	    		if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
	  	    			line += getStatistics(tsRegression, iEquation, nl );
	  	    		}
	  	    		else {
	  	    			line += getStatistics(tsRegression, nl );
	  	    		}
    
	  	    		// Done with this independent time series for the correlation table.
	  	    		summary.append (line);

	  	    		independentCount++;
    			}
    			// This is the one	__sSummary.append ( __nl );
    	    }
    	}
    }
    catch ( Exception e ) {
    	Message.printWarning( 3, mthd, e );
    }
	summary.append ( nl + nl );
	return summary;
}

/**
Determine the flag to tag the filled time series values.
@param numEquations 1 or 12 if monthly equations
@param iEquation equation being processed (1+).
@param rank rank for independent time series relationship.
@return string flag to tag time series
*/
private String determineFillFlag ( int numEquations, int iEquation, int rank )
{
    if ( numEquations == 1 ) {
        return "" + rank;
    }
    else {
        // Monthly
        return TimeUtil.monthAbbreviation(iEquation) + rank;
    }
}

/**
Fill the dependent time series using the dependent time series and regression
parameter defined during the analysis.  This method will typically be called
from the FillMixedStationAnalysis() command after the MixedStationAnalysis is run and results are ranked.
All the information needed to fill the dependent time series can be obtained
from the regressions. Notice yet that only one independent time series will
be used to fill part of a dependent. The first one in the list satisfying the properties requirement will be used.
*/
public void fill ( )
{   String mthd = "MixedStationAnalysis.fill", mssg;

	Message.printStatus ( 2, mthd, "Start filling time series..." );
	printMemoryStats(mthd,"Before filling");
	
	if ( !rankCompleted() ) {
	    mssg = "Mixed Station Analysis results have not been ranked.  Can't use for filling.";
	    throw new RuntimeException ( mssg );
	}

	TS independentTS = null;
	TS dependentTS = null;
	DataTransformationType transformation;
	NumberOfEquationsType numberOfEquations;
	RegressionType analysisMethod;
	Double confidenceLevel = null;
	Double intercept;
	DateTime dependentAnalysisStart;
	DateTime dependentAnalysisEnd;
	DateTime independentAnalysisStart;
	DateTime independentAnalysisEnd;
	DateTime fillStart;
	DateTime fillEnd;
	String fillFlag = __fillFlag;

	// __MinimumDataCount as integer.
	int MinimumDataCount = __minimumDataCount.intValue();

	String fillFlag2 = null; // Reset below based on whether fill flag is "auto" is set for fill flag
	
	try {
    	// Loop for each one of the dependent time series
    	int nDependent = __dependentTSRegressionList.size();
    	int [] monthArray = new int[1]; // Used to call missingCountForMonths() when processing monthly relationships
    	String equationString = ""; // For messaging
    	for ( int iDepResult = 0; iDepResult < nDependent; iDepResult++ ) {
    		dependentTS = __dependentTSList.get(iDepResult);
    
    		// Using the same code to deal with monthly and single equation
    		// For single equation set the variable nMonth to 1.
    		int nMonth = 1;
    		if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
    		    nMonth = 12;
    		}
    
    		for ( int month = 1; month <= nMonth; month++ ) {
    			// Loop through the regressions for this dependent, find the first best fit,
    		    // fill the dependent and set the break from the independent loop.
    		    monthArray[0] = month;
    		    List<TSRegression> depResultList = getRankedTSRegressionList(iDepResult, (month - 1) );
    		    int nDepResult = depResultList.size();
    		    if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
    		        equationString = "";
    		    }
    		    else {
    		        equationString = " (" + TimeUtil.monthAbbreviation(month) + ")";
    		    }
    		    Message.printStatus(2, mthd, "Dependent time series \"" + dependentTS.getIdentifierString() + "\" " +
    		        equationString + "has " + depResultList.size() + " combinations of regression results to use for filling." );
    	        int rank = 0;
    			for ( TSRegression tsRegression : depResultList ) {
    			    ++rank;
    				analysisMethod = tsRegression.getAnalysisMethod();
    				numberOfEquations = tsRegression.getNumberOfEquations();
    				transformation = tsRegression.getTransformation();
    				intercept = tsRegression.getIntercept();
    
    		  		// Dependent time series from regression object
			    	dependentTS = tsRegression.getDependentTS();
			    	String depend = dependentTS.getAlias();
			    	if ( depend.length() == 0 ) {
			    		depend = dependentTS.getLocation();
			    	}
			    	
			    	// If the dependent time series does not have any missing data in the fill period, then
			    	// no need to continue filling
			    	
			    	int nMissing = 0;
			    	if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
			    	    nMissing = TSUtil.missingCountForMonths(dependentTS, tsRegression.getFillStart(), tsRegression.getFillEnd(),
			    	        monthArray );
			    	}
			    	else {
			    	    nMissing = TSUtil.missingCount(dependentTS, tsRegression.getFillStart(), tsRegression.getFillEnd() );
			    	}
			    	    
			    	if ( nMissing == 0 ) {
			    	    Message.printStatus(2, mthd, "Dependent time series \"" +
                            dependentTS.getIdentifierString() + "\" " + equationString + "has " + nMissing +
                            " missing values... no need to continue filling (used " +
                            rank + " of " + nDepResult + " relationships).");
			    	    break;
			    	}
			    	else {
			    	    Message.printStatus(2, mthd, "Dependent time series \"" +
			    	        dependentTS.getIdentifierString() + "\" " + equationString + "has " + nMissing +
			    	        " missing values... will try to fill with regression relationship " +
			    	        rank + " of " + nDepResult );
			    	}

    	    		// Use only if regression was properly analyzed
    	    		if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
    	    			if ( !tsRegression.isAnalyzed( month ) ) {
                            Message.printStatus(2, mthd, "Dependent time series \"" +
                                dependentTS.getIdentifierString() + "\" " + equationString +
                                "is not analyzed for regression relationship " +
                                rank + " of " + nDepResult + "...skipping..." );
    	    			    continue;
    	    			}
    				}
    	    		else {
    					if ( !tsRegression.isAnalyzed() ) {
    	                      Message.printStatus(2, mthd, "Dependent time series \"" +
	                                dependentTS.getIdentifierString() + "\" " + equationString +
	                                "is not analyzed for regression relationship " +
	                                rank + " of " + nDepResult + "...skipping..." );
    					    continue;
    					}
    				}
    	    		
    	    		// Fill only if the minimum R is satisfied.
    	    		
                    if ( __minimumR != null  ) {
                        if ( (numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS) &&
                            (tsRegression.getCorrelationCoefficient( month ) < __minimumR.doubleValue())) {
                            Message.printStatus(2, mthd, "Dependent time series \"" +
                                dependentTS.getIdentifierString() + "\" " + equationString +  "has R " +
                                tsRegression.getCorrelationCoefficient(month) +
                                " less than required (" + __minimumR + ") for regression relationship " +
                                rank + " of " + nDepResult + "...skipping..." );
                            continue;
                        }
                    }
                    else {
                        if ( tsRegression.getCorrelationCoefficient() < __minimumR.doubleValue() ) {
                            Message.printStatus(2, mthd, "Dependent time series \"" +
                                dependentTS.getIdentifierString() + "\" " + equationString + "has R " +
                                tsRegression.getCorrelationCoefficient() +
                                " less than required (" + __minimumR + ") for regression relationship " +
                                rank + " of " + nDepResult + "...skipping..." );
                        continue;
                        }
                    }

    				// Use only if the number of data points used in the analysis was >= to MinimumDataCount
    
    				int n1;
    				try {
    	    	    	if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
    						n1 = tsRegression.getN1( month );
    					}
    	    	    	else {
    						n1 = tsRegression.getN1();
    					}
    				}
    				catch ( Exception e ) {
    					continue;
    				}
    				if ( n1 < MinimumDataCount ) {
    				    Message.printStatus(2, mthd, "Dependent time series \"" +
                            dependentTS.getIdentifierString() + "\" " + equationString + "has " + n1 +
                            " non-missing values n1 (need " + MinimumDataCount + ") for regression relationship " +
                            rank + " of " + nDepResult + "...skipping..." );
    					continue;
    				}
    				
                    // Use only if the number of data points in the independent and not in the dependent (number able to
    				// fill) is > 0
    			    
                    int n2;
                    try {
                        if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
                            n2 = tsRegression.getN2( month );
                        }
                        else {
                            n2 = tsRegression.getN2();
                        }
                    }
                    catch ( Exception e ) {
                        continue;
                    }
                    if ( n2 == 0 ) {
                        Message.printStatus(2, mthd, "Dependent time series \"" +
                            dependentTS.getIdentifierString() + "\" " + equationString +
                            "has 0 non-missing values n2 for regression relationship " +
                            rank + " of " + nDepResult + "...skipping..." );
                        continue;
                    }
    
    				// Get the independent TS
      	    		independentTS = tsRegression.getIndependentTS();
    	    		String indep = independentTS.getAlias();
    	    		if ( indep.length() == 0 ) {
    	    			indep = independentTS.getLocation();
    	    		}

    	    		dependentAnalysisStart = tsRegression.getDependentAnalysisStart();
    	    		dependentAnalysisEnd = tsRegression.getDependentAnalysisEnd();
    	    		independentAnalysisStart = tsRegression.getIndependentAnalysisStart();
    	    		independentAnalysisEnd = tsRegression.getIndependentAnalysisEnd();
	  	    		fillStart = tsRegression.getFillStart();
	  	    		fillEnd = tsRegression.getFillEnd();
	  	    		
	  	    		// FIXME SAM 2009-09-01 Need to make sure that only the high-ranking month is filled
	  	    		// in the following by passing analysisMonths with one month.

	  	    		// TODO SAM 2010-06-04 Are the following comments valid since tsRegression is being passed in?
    				// The following will recompute the statistics.  This is OK for now because it causes
	  	    		// normal logging, etc. to occur.
	  	    		// TODO SAM 2009-08-30 In the future, perhaps pass a TSRegression object so that the
	  	    		// analysis does not need to be performed again.
	  	    		fillFlag2 = fillFlag; // Default to use (can be null)
	  	    		if ( (fillFlag != null) && fillFlag.equalsIgnoreCase("auto") ) {
	  	    		    // Use the found relationship for the fill flag
	  	    		    fillFlag2 = determineFillFlag(nMonth, month, rank);
	  	    		}
	  	    		if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
	                    // Fill only the month of interest...
                        TSUtil.fillRegress(dependentTS, independentTS,
                            tsRegression, // Use this directly since don't want to recompute from filled data
                            confidenceLevel, // Confidence level
                            analysisMethod, numberOfEquations,
                            intercept,
                            monthArray, // only the month of interest
                            transformation,
                            dependentAnalysisStart, dependentAnalysisEnd,
                            independentAnalysisStart, independentAnalysisEnd,
                            fillStart, fillEnd,
                            fillFlag2,
                            null ); // No description string	    
	  	    		}
	  	    		else {
	  	    		    // Fill the entire time series...
    	  	    		TSUtil.fillRegress(dependentTS, independentTS,
    	  	    		    tsRegression, // Use this directly since don't want to recompute from filled data
    	  	    		    confidenceLevel, // Confidence level
    	  	    		    analysisMethod, numberOfEquations,
    	  	    		    intercept,
    	  	    		    null, // no analysis months specified
    	  	    		    transformation,
    	  	    		    dependentAnalysisStart, dependentAnalysisEnd,
    	  	    		    independentAnalysisStart, independentAnalysisEnd,
    	  	    		    fillStart, fillEnd,
    	  	    		    fillFlag2,
    	  	    		    null ); // No description string
	  	    		}
	  	    		// TODO SAM 2010-06-06 Is there a way to do this check without calling missingCount
	  	    		// above and here?  The problem is the loop "continue" statements
                    int nMissingAfter = 0;
                    if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
                        nMissingAfter = TSUtil.missingCountForMonths(dependentTS, tsRegression.getFillStart(),
                            tsRegression.getFillEnd(), monthArray );
                    }
                    else {
                        nMissingAfter = TSUtil.missingCount(dependentTS, tsRegression.getFillStart(),
                            tsRegression.getFillEnd() );
                    }
                    int nFilled = nMissing - nMissingAfter;
                    if ( nFilled != 0 ) {
                        // Some data were filled so save the fill flag metadata
                        String rFormatted = null;
                        if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
                            rFormatted = StringUtil.formatString ( tsRegression.getCorrelationCoefficient(month), "%.4f" );
                        }
                        else {
                            rFormatted = StringUtil.formatString ( tsRegression.getCorrelationCoefficient(), "%.4f" );
                        }
                        String aFormatted = null;
                        if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
                            aFormatted = StringUtil.formatString ( tsRegression.getA(month), "%.4f" );
                        }
                        else {
                            aFormatted = StringUtil.formatString ( tsRegression.getA(), "%.4f" );
                        }
                        String bFormatted = null;
                        if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
                            bFormatted = StringUtil.formatString ( tsRegression.getB(month), "%.4f" );
                        }
                        else {
                            bFormatted = StringUtil.formatString ( tsRegression.getB(), "%.4f" );
                        }
                        String sepFormatted = null;
                        if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
                            sepFormatted = StringUtil.formatString ( tsRegression.getRMSE(month), "%.4f" );
                        }
                        else {
                            sepFormatted = StringUtil.formatString ( tsRegression.getRMSE(), "%.4f" );
                        }
                        /*
                        String sepTotalFormatted = null;
                        if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
                            sepTotalFormatted = StringUtil.formatString ( tsRegression.getRMSE(month), "%.4f" );
                        }
                        else {
                            sepTotalFormatted = StringUtil.formatString ( tsRegression.getRMSE(), "%.4f" );
                        }
                        */
                        dependentTS.addDataFlagMetadata(
                            new TSDataFlagMetadata(fillFlag2,"Filled " + nFilled + " values " + equationString +
                                "using independent \"" +
                                independentTS.getIdentifierString() + "\", best fit indicator=" + __bestFitIndicator +
                                ", analysis method=" + analysisMethod +
                                ", number of equations=" + nMonth + ", transformation=" + transformation +
                                ", R=" + rFormatted + ", number overlapping=" + n1 + ", A=" + aFormatted +
                                ", B=" + bFormatted + ", SEP=" + sepFormatted + 
                                //", SEPTotal=" + sepTotalFormatted +
                                ", remaining missing" + equationString + "=" + nMissing) );
                    }
    			}
	        }
    	}
    }
    catch ( Exception e ) {
        // TODO SAM 2009-08-31 Evaluate error handling - log individual errors but process as much as possible
    	Message.printWarning( 3, mthd, e );
    }
    printMemoryStats(mthd,"After filling");
}

/**
Get the ranked TSRegression relationships for the requested dependent time series and month.
@param iDep dependent time series position (0+).
@param iEquation equation position (0+).
*/
private List<TSRegression> getRankedTSRegressionList ( int iDep, int iEquation )
{
    List<TSRegression> rankedTSRegressionList = new Vector();
    // The regression relationships for the dependent time series, in original order
    List<TSRegression> dependentTSRegressionList = __dependentTSRegressionList.get(iDep);
    int size = dependentTSRegressionList.size();
    // Add relationships in the order indicated by the sort array
    for ( int i = 0; i < size; i++ ) {
        rankedTSRegressionList.add ( dependentTSRegressionList.get(__sortedOrder[iDep][iEquation][i]) );
    }
    return rankedTSRegressionList;
}

/**
Returns a string containing the number of data point used in the analysis,
the correlation coefficient, the SEP, A and B values.  This string is used
by the method createReportSummary to prepare report lines.
@param tsRegression the reference to the regression to get the statistics from.
Returns the statistics string part of the report for a single month's relationship.
*/
private String getStatistics ( TSRegression tsRegression, int month, String nl )
{
	String retString = "";

	if ( tsRegression.isAnalyzed( month ) ) {
		// Number of overlapping points in the analysis
		try {
			int N1 = tsRegression.getN1(month);
			retString += StringUtil.formatString( N1, __format7d );
		}
		catch ( Exception e ) {
			retString += "    ???";
		}
		
	    // Excluding n1, number of points in the independent but missing in the dependent
        try {
            int N2 = tsRegression.getN2(month);
            retString += StringUtil.formatString( N2, __format7d );
        }
        catch ( Exception e ) {
            retString += "    ???";
        }

		// Coefficient of correlation (r)
		try {
			double cc = tsRegression.getCorrelationCoefficient(month );
			retString += StringUtil.formatString (cc, __format8p4f );
		}
		catch ( Exception e ) {
			retString += "     ???";
		}

		// SEP (RMSE)
		try {
			double rmse = tsRegression.getRMSE( month );
			retString += StringUtil.formatString(rmse, __format12p2f );
		}
		catch ( Exception e ) {
			retString += "         ???";
		}

		// A
		try {
			double a = tsRegression.getA(month);
			retString += StringUtil.formatString(a, __format12p2f );
		}
		catch ( Exception e ) {
			retString += "         ???";
		}

		// B
		try {
			double b = tsRegression.getB(month);
			retString += StringUtil.formatString( b, __format12p2f );
		}
		catch ( Exception e ) {
			retString += "         ???";
		}
	}
	else {
		// The analysis was not performed for this month.
		retString += "    ..."; // N1
		retString += "    ..."; // N2
		retString += "     ..."; // r
		retString += "         ..."; // SEP RMSE
		retString += "         ..."; // A
		retString += "         ..."; // B
	}

	// Sep total
	retString += " |";
	try {
		if ( tsRegression.isAnalyzed() ) {
			double b = tsRegression.getRMSE();
			retString += StringUtil.formatString(b, __format12p2f );
		}
		else {
			retString += "         ...";
		}
	}
	catch ( Exception e ) {
		retString += "         ???";
	}

	retString += nl;

	return retString;
}

/**
Returns a string containing the number of data point used in the analysis,
the correlation coefficient, the SEP, A and B values.  This string is used
by the method createReportSummary to prepare report lines.
@param tsRegression the reference to the regression to get the statistics from.
Returns the statistics string part of the report for single equation regressions.
*/
private String getStatistics( TSRegression tsRegression, String nl )
{
	String retString = "";

	if ( tsRegression.isAnalyzed() ) {

		// Number of overlapping points in the analysis
		try {
			int n1 = tsRegression.getN1();
			retString += StringUtil.formatString( n1, __format7d );
		}
		catch ( Exception e ) {
			retString += "    ???";
		}
		
	    // Excluding n1, number of non-missing points in the independent, and missing in the dependent
        try {
            int n2 = tsRegression.getN2();
            retString += StringUtil.formatString( n2, __format7d );
        }
        catch ( Exception e ) {
            retString += "    ???";
        }

		// Correlation Coefficient
		try {
			double cc = tsRegression.getCorrelationCoefficient();
			retString += StringUtil.formatString( cc, __format8p4f );
		}
		catch ( Exception e ) {
			retString += "     ???";
		}

		// SEP (RMSE)
		try {
			double rmse = tsRegression.getRMSE();
			retString += StringUtil.formatString( rmse, __format12p2f );
		}
		catch ( Exception e ) {
			retString += "         ???";
		}

		// A
		try {
			double a = tsRegression.getA();
			retString += StringUtil.formatString( a, __format12p2f );
		}
		catch ( Exception e ) {
			retString += "         ???";
		}

		// B
		try {
			double b = tsRegression.getB();
			retString += StringUtil.formatString( b, __format12p2f );
		}
		catch ( Exception e ) {
			retString += "         ???";
		}

		// Sep total
		retString += " |";
		try {
			double b = tsRegression.getRMSE();
			retString += StringUtil.formatString(b, __format12p2f);
		}
		catch ( Exception e ) {
			retString += "         ???";
		}

	}
	else {
		// The analysis was not performed for this month.
		retString += "    ..."; // N1
		retString += "    ..."; // N2
		retString += "     ..."; // r
		retString += "         ..."; // SEP RMSE
		retString += "         ..."; // A
		retString += "         ..."; // B
		retString += " |";
		retString += "         ..."; // SEP Total
	}

	retString += nl;

	return retString;
}

/**
Print memory statistics to evaluate performance.
*/
private void printMemoryStats ( String routine, String message )
{
    Runtime runtime = Runtime.getRuntime();

    long maxMemory = runtime.maxMemory();
    long allocatedMemory = runtime.totalMemory();
    long freeMemory = runtime.freeMemory();
    long kb2mb = 1024*1024;

    Message.printStatus(2,routine,message + " Free memory (MB): " + (freeMemory / kb2mb) );
    Message.printStatus(2,routine,message + " Allocated memory (MB): " + (allocatedMemory / kb2mb) );
    Message.printStatus(2,routine,message + " Current max memory (MB): " + (maxMemory / kb2mb) );
    Message.printStatus(2,routine,message + " Upper limit on free memory (MB): " + ((freeMemory + (maxMemory - allocatedMemory)) / kb2mb) );
}

/**
Rank the TSRegression in ascending order based on the best fit indicator (best first first).
*/
private void rank()
{
	String mthd = "MixedStationAnalysis.rank", mssg;

	List independentList = null;
	TSRegression tsRegression = null;

	double[] values;

	int nDependent = __dependentTSRegressionList.size();
	__sortedOrder = new int [nDependent][][];
	
	Message.printStatus ( 2, mthd, "Ranking fill results for " + nDependent + " dependent time series.");
    printMemoryStats(mthd,"Before ranking");

    try {
    	// Loop over the dependent objects, which each have a list of statistics, stored in TSRegression objects.
        for ( int dep = 0; dep < nDependent; dep++ ) {
            // Get the independent list ( regressions ) for this independent
            independentList = (List)__dependentTSRegressionList.get( dep );
            int nIndependent = independentList.size();
            values = new double [nIndependent];
            int sortDirection = 0; // SEP is ascending, R is descending
            
            if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
                // Result for each month are sorted (12 months)
                __sortedOrder[dep] = new int [12][nIndependent];
            
            	for ( int month = 1; month <= 12; month++ ) {
               		// Loop the independent objects, processing correlation coefficient or RMSE values.
            		for ( int ind = 0; ind < nIndependent; ind++ ) {
            		    // Get the regression object.
                		tsRegression = null;
                		tsRegression = (TSRegression)independentList.get(ind);
            
                		try {
                		    switch ( __bestFitIndicator ) {
                		        case R:
                			        values[ind]= tsRegression.getCorrelationCoefficient ( month );
                			        sortDirection = MathUtil.SORT_DESCENDING;
                			        break;
                		        case SEP:
                		            values[ind]= tsRegression.getRMSE( month );
                		            sortDirection = MathUtil.SORT_ASCENDING;
                		            break;
                		        case SEP_TOTAL:
                		            // TODO[LT 2005-04-30] For total we could do this just once.
                		            // The sort will be the same for all months.  Keeping as is for now!
                		            values[ind]= tsRegression.getRMSE();
                		            sortDirection = MathUtil.SORT_ASCENDING;
                		            break;
                		    }
                	    }
                		catch (Exception e ) {
            	    		// If something went wrong with the TSRegression, the getRMSE
            	    		// and getCorrelationCoefficient will throw and exception, but
            	    		// still need to keep the independent in the list of values to be able to relate
            	    		// the sorted indexes to the TSRegressions references in the independentList vectors,
            	    		// so set it to -999.99.
                			values[ind] = __MISSING;
                		}
                	}
                	try {
                    	MathUtil.sort ( values, MathUtil.SORT_QUICK, sortDirection, __sortedOrder[dep][month-1], true );
                	}
                	catch ( Exception e ) {
                		Message.printWarning (3,"",e);
                		mssg = "Error sorting!";
              	    	rankMessage (tsRegression,mssg);
              	    }
              	}
            }
            else {
            	// Year ( 1 equation )
                __sortedOrder[dep] = new int [1][nIndependent];
            
            	// Loop the independent objects, processing correlation coefficient or RMSE values.
            	for ( int ind = 0; ind < nIndependent; ind++ ) {
                	// Get the regression object.
                	tsRegression = null;
                	tsRegression = (TSRegression)independentList.get(ind);
                
                	try {
                		switch ( __bestFitIndicator ) {
                		    case R:
                		        values[ind] = tsRegression.getCorrelationCoefficient();
                		        sortDirection = MathUtil.SORT_DESCENDING;
                		        break;
                		    case SEP:
                		    	values[ind] = tsRegression.getRMSE();
                		    	sortDirection = MathUtil.SORT_ASCENDING;
                		    	break;
                		    case SEP_TOTAL:
                		        values[ind] = tsRegression.getRMSE();
                		        sortDirection = MathUtil.SORT_ASCENDING;
                		        break;
                		}
                	}
                	catch (Exception e ) {
                		// If something went wrong with the TSRegression, the getRMSE
            	    	// and getCorrelationCoefficient will throw an exception, but
            	    	// still need to keep the independent in the list of values to be able to relate
            	    	// the sorted indexes to the TSRegressions references in the independentList vectors,
            	    	// so set it to -999.99.
                		values[ind]= __MISSING;
                	}
                }
                try {
                   	MathUtil.sort ( values, MathUtil.SORT_QUICK, sortDirection, __sortedOrder[dep][0], true );
                }
                catch ( Exception e ) {
                    Message.printWarning (3,"",e);
                    mssg = "Error sorting!";
            		rankMessage (tsRegression,mssg);
    		  	}
    		}
    	}
    }
    catch ( Exception e ) {
    		Message.printWarning( 3, mthd, e );
    }
    printMemoryStats(mthd,"After ranking");
}

/**
Indicate whether the ranking process has been completed.
*/
private boolean rankCompleted ()
{
    if ( __dependentTSRegressionList != null ) {
        return true;
    }
    else {
        return false;
    }
}

/**
Update the status bar of the calling object, if that object instantiate this
class using the construct overload and pass in a valid not null reference to itself (this).
*/
private void rankMessage ( TSRegression tsRegression, String text )
{
	String mssg = "TSRegression (";

	// Dependent time series
	TS dependentTS = tsRegression.getDependentTS();
	String dependent = dependentTS.getAlias();
	if ( dependent.length() == 0 ) {
		dependent = dependentTS.getLocation();
	}
	mssg = mssg + dependent + ", ";

	// Independent time series
	TS independentTS = tsRegression.getIndependentTS();
	String independent = independentTS.getAlias();
	if ( independent.length() == 0 ) {
		independent = independentTS.getLocation();
	}
	mssg = mssg + independent + ", ";

	// Transformation
	DataTransformationType transformation = tsRegression.getTransformation();
	// Add Transformation to the correlation table
	mssg = mssg + transformation + ", ";

	// AnalysisMethod
	RegressionType analysisMethod = tsRegression.getAnalysisMethod();
	mssg = mssg + analysisMethod + ") - " + text + "\n";
}

/**
Save all parts of the report created by different stages of the analysis.
@param nl newline to use, based on environment
*/
private void saveReportText( File outputFileFull, List<String>outputCommentsList,
    StringBuffer header, StringBuffer statistics, StringBuffer summary, String nl )
throws FileNotFoundException
{
	PrintWriter pw = null;
	String cnl = "#" + nl;
	try {
		FileOutputStream fos = new FileOutputStream ( outputFileFull );
		pw = new PrintWriter (fos);

		IOUtil.printCreatorHeader ( pw, "#", 80, 0);
		if ( outputCommentsList != null ) {
		    for ( int i = 0; i < outputCommentsList.size(); i++ ) {
		        pw.print( "# " + outputCommentsList.get(i) + nl );
		    }
		}

		// Report header section
		if ( header != null ) {
			pw.print (header.toString());
		}
		else {
			pw.print(cnl+cnl+"Mixed Station Analysis report header section is empty - an error has occurred.");
		}

		// Summary section
		if ( summary != null ) {
			pw.print (summary.toString());
		}
		else {
			pw.print(cnl+cnl+"Mixed Station Analysis report summary section is empty - an error has occurred.");
		}

		// Statistics section
		if ( statistics != null ) {
			pw.print (statistics.toString());
		}
		else {
			pw.print(cnl+cnl+"Mixed Station Analysis report statistics section is empty - an error has occurred.");
		}
	}
	finally {
	    if ( pw != null ) {
            pw.flush();
            pw.close(); 
	    }
	}
}

// TODO [2005-04-20] This will not work if this class is moved to the TS
// library, because it should not be allowed to import the class
// fillMixedStation_JDialog, which resides in the TSTool application.
// The solution may be to implement a listener with interface as it is done in C++.
/**
Update the status bar of the calling object, if that object instantiate this
class using the construct overload and pass in a valid not null reference to itself (this).
*/
private void setParentStatusText( String text )
{
	// FIXME SAM 2009-08-30 Need to implement listener
}

}