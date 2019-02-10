// MixedStationAnalysis - This class executes the Mixed Station Analysis

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2019 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.ts;

import java.util.List;
import java.util.Vector;

import RTi.Util.Math.BestFitIndicatorType;
import RTi.Util.Math.DataTransformationType;
import RTi.Util.Math.NumberOfEquationsType;
import RTi.Util.Math.RegressionType;
import RTi.Util.Message.Message;
import RTi.Util.Table.DataTable;
import RTi.Util.Time.DateTime;
import RTi.TS.TS;
import RTi.TS.TSRegressionAnalysis;
import RTi.TS.TSUtil_FillRegression;

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
Dependent time series to be analyzed.
*/
private List<TS> __dependentTSList = null;

/**
Independent time series to be analyzed.
*/
private List<TS> __independentTSList = null;

/**
Regression analysis methods to use (e.g., OLS, MOVE2).
*/
private List<RegressionType> __regressionTypeList = null;

/**
Transformations to use (see TRANSFORM*).
*/
private List<DataTransformationType> __transformationList = null;

/**
Number of equations to use (can be one and/or monthly).
*/
private List<NumberOfEquationsType> __numberOfEquations = null;

/**
Which month(s) to analyze.
*/
private int[] __analysisMonths = null;

/**
Analysis start date/time.
*/
private DateTime __analysisStart = null;

/**
Analysis end date/time.
*/
private DateTime __analysisEnd = null;

/**
Confidence interval >0 and <100 %, used to discard estimated values.  If null, all estimated values are used.
*/
private Double __confidenceInterval = null;

/**
Best fit indicator.
*/
private BestFitIndicatorType __bestFitIndicator  = null;

/**
Fill flag to pass to fill code.
*/
private String __fillFlag = null;

/**
Description of fill flag to pass to fill code.
*/
private String __fillFlagDesc = null;

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
Data value substituted when log transform is used and original value is <= 0.
*/
private String __leZeroLogValue = null;

/**
Minimum overlapping data count required for acceptable analysis.
*/
private Integer __minimumDataCount = null;

/**
Minimum correlation coefficient value required for acceptable analysis.
*/
private Double __minimumR = null;

/**
Description string to append to time series description, rather than using default.
 */
private String __descriptionString = null;

/**
List that holds the results of the analysis.
*/
private List<TSUtil_FillRegression> __analysisResults = new Vector<TSUtil_FillRegression>();

/**
DataTable that statistics can be written to.
*/
private DataTable __table = new DataTable();

/**
String that holds table column name for TSID.
*/
private String __tableTSIDColumnName;

/**
String that holds table TSID format.
*/
private String __tableTSIDFormat;

/**
Construct a MixedStationAnalysis object.
@param bestFitIndicator the indicator to use when determining which relationship is the best fit.
@param numberOfEquations number of equations to compute, one and/or monthly. 
In the future support for seasonal equations may be added.
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
analysis (N1 in FillRegression() documentation).
If the minimum count is not met, then the independent time series is ignored
for the specific combination of parameters.  For example, if monthly equations
are used, the independent time series may be ignored for the specific month;
however, it may still be analyzed for other months.
@param minimumR minimum correlation coefficient that should be considered a good fit.
@param confidenceLevel the confidence level that is required for filled values.  In other words, if an estimated value
falls outside of the mean +- the confidence level, then don't fill with the value.
@param fillFlag fill flag passed to regression code, to indicate values that are filled
(can be null, one-character, or "Auto").
@param table the table to put statistics in
*/
public MixedStationAnalysis ( List<TS> dependentTSList, List<TS> independentTSList,
    BestFitIndicatorType bestFitIndicator,
    List<RegressionType> regressionTypeList, List<NumberOfEquationsType> numberOfEquations,
    int[] analysisMonths, DateTime analysisStart, DateTime analysisEnd,
    DateTime fillStart, DateTime fillEnd,
    List<DataTransformationType> transformationList, String LEZeroLogValue, Double intercept,
    Integer minimumDataCount, Double minimumR, Double confidenceLevel, String fillFlag,
    String fillFlagDesc, DataTable table, String tableTSIDColumnName, String tableTSIDFormat )
{
	super ();

	__dependentTSList = dependentTSList;
	__independentTSList = independentTSList;
    __bestFitIndicator = bestFitIndicator;
    __regressionTypeList = regressionTypeList;
    if ( (__regressionTypeList == null) || (__regressionTypeList.size() == 0) ) {
        // Default is OLS regression
        __regressionTypeList = new Vector<RegressionType>();
        __regressionTypeList.add ( RegressionType.OLS_REGRESSION );
    }
    __numberOfEquations = numberOfEquations;
    __analysisMonths = analysisMonths;
	__analysisStart = analysisStart;
	__analysisEnd = analysisEnd;
	__fillStart = fillStart;
	__fillEnd = fillEnd;
	__transformationList = transformationList;
	if ( (__transformationList == null) || (__transformationList.size() == 0) ) {
	    // Default is None
	    __transformationList = new Vector<DataTransformationType>();
	    __transformationList.add ( DataTransformationType.NONE );
	}
	__leZeroLogValue = LEZeroLogValue;
	__intercept = intercept;
	__minimumDataCount = minimumDataCount;
	__minimumR = minimumR;
	__confidenceInterval = confidenceLevel;
	__fillFlag = fillFlag;
	__fillFlagDesc = fillFlagDesc;
	__table = table;
	__tableTSIDColumnName = tableTSIDColumnName;
	__tableTSIDFormat = tableTSIDFormat;
}

/**
Perform the mixed station analysis, for each dependent time series,
using all independent time series (except the dependent, if in the list), for
all Analysis Methods and Transformations.
@throws Exception in case of error.
*/
public void analyze()
{	String routine = "MixedStationAnalysis.analyze";
	//copy time series so we don't get messed up with filling
	//doing the copy all at once initially is more memory efficient
	List<TS> copyTS = new Vector<TS>();
	for (TS independent: __independentTSList) {
		TS copy = (TS) independent.clone();
		copyTS.add(copy);
		//make 0 an unacceptable value if so desired by the user
		if (__leZeroLogValue != null && __leZeroLogValue.equalsIgnoreCase("Missing")) {
			double[] missing = copy.getMissingRange();
			//replace the one closer to 0 with 0....
			if (Math.abs(missing[0] - 0) < Math.abs(missing[1] - 0)) {
				missing[0] = 0;
			}
			else {
				missing[1] = 0;
			}
			copy.setMissingRange(missing);
		}
	}
	
	List<TSUtil_FillRegression> finalAnalyses = new Vector<TSUtil_FillRegression>();
	for (TS dependent : __dependentTSList) {
		//go through each dependent....
		//copy so filling does not mess up analysis....
		TS newDependent = (TS) dependent.clone();
		
		if (__leZeroLogValue != null && __leZeroLogValue.equalsIgnoreCase("Missing")) {
			double[] missing = newDependent.getMissingRange();
			//replace the one closer to 0 with 0....
			if (Math.abs(missing[0] - 0) < Math.abs(missing[1] - 0)) {
				missing[0] = 0;
			}
			else {
				missing[1] = 0;
			}
			newDependent.setMissingRange(missing);
		}
		
		//Is there a way to check that the dependent actually has data missing?
		//If so, it would be great to exclude dependents that aren't missing data
		
		List<TSRegressionAnalysis> analysisList = new Vector<TSRegressionAnalysis>();
		for (int i = 0; i < copyTS.size(); i++) {
			TS independent = __independentTSList.get(i);
			TS copy = copyTS.get(i);
			//dependent and independent must be different
			//independent must have data
			//the two must overlap
			if ( independent.equals(dependent) ) {
				continue;
			}
			Message.printStatus(2, routine, "Processing dependent time series \"" + dependent.getIdentifierString() + "\", independent time series \"" +
			independent.getIdentifierString() + "\"" );
			if ( !independent.hasData() ) {
				Message.printStatus(2,routine,"Independent time series does not have data.");
				continue;
			}
			if ( !(independent.getNonMissingDataDate1().between(dependent.getNonMissingDataDate1(), dependent.getNonMissingDataDate2()) ||
				independent.getNonMissingDataDate2().between(dependent.getNonMissingDataDate1(), dependent.getNonMissingDataDate2()))) {
				Message.printStatus(2,routine,"Dependent and independent time series do not overlap.");
				continue;
			}
			
			for (DataTransformationType transformation : __transformationList) {
				for (NumberOfEquationsType numEquations : __numberOfEquations) {
					//use all requested combinations....
					boolean analyzeSingle = false;
					boolean analyzeMonthly = false;
					if (numEquations == NumberOfEquationsType.ONE_EQUATION) {
						analyzeSingle = true;
					}
					if (numEquations == NumberOfEquationsType.MONTHLY_EQUATIONS) {
						analyzeMonthly = true;
					}
					//do the analysis....
					TSRegressionAnalysis analysis = new TSRegressionAnalysis(copy, newDependent,
						RegressionType.OLS_REGRESSION, analyzeSingle, analyzeMonthly, __analysisMonths,
						transformation, __leZeroLogValue, __intercept, __analysisStart, __analysisEnd,
						__analysisStart, __analysisEnd, __confidenceInterval);
					analysis.analyzeForFilling(__minimumDataCount, __minimumR, __confidenceInterval);

					//if it isn't OK, don't add it to the list to use or bother cleaning up.
					if (!analysis.isOk()) continue;
					
					analysisList.add(analysis);

					//clean up data....
					//If it isn't ok, it'll just get cleaned up by having no references
					//only clean up if not needed for table
					if (__table == null) {
						analysis.removeDependent(); //not needed - object with all has dependent....
						if (analyzeSingle) {
							//transformed and untransformed data exists no matter what....
							analysis.getTSRegressionData().getSingleEquationRegressionData().cleanMemory();
							analysis.getTSRegressionDataTransformed().
								getSingleEquationRegressionData().cleanMemory();

							//not so with results and errors
							if (transformation == DataTransformationType.NONE) {
								//data saved in results
								analysis.getTSRegressionResults().
									getSingleEquationRegressionResults().get__data().cleanMemory();
								//estimated values
								analysis.getTSRegressionEstimateErrors().
									getSingleEquationRegressionErrors().clearY1est();
								//data saved in errors
								analysis.getTSRegressionEstimateErrors().
									getSingleEquationRegressionErrors().getRegressionData().cleanMemory();
								//data saved in results saved in errors
								analysis.getTSRegressionEstimateErrors().getSingleEquationRegressionErrors().
									getRegressionResults().get__data().cleanMemory();
							}
							else {
								//data saved in results
								analysis.getTSRegressionResultsTransformed().
									getSingleEquationRegressionResults().get__data().cleanMemory();
								//estimated values
								analysis.getTSRegressionErrorsTransformed().
									getSingleEquationRegressionErrors().clearY1est();
								//data saved in errors
								analysis.getTSRegressionErrorsTransformed().
									getSingleEquationRegressionErrors().getRegressionData().cleanMemory();
								//data saved in results saved in errors
								analysis.getTSRegressionErrorsTransformed().getSingleEquationRegressionErrors().
									getRegressionResults().get__data().cleanMemory();
							}
						}
						if (analyzeMonthly) {
							for (int j = 1; j <= 12; j++) {
								//transformed and untransformed data exists no matter what....
								analysis.getTSRegressionData().getMonthlyEquationRegressionData(j).cleanMemory();
								analysis.getTSRegressionDataTransformed().
									getMonthlyEquationRegressionData(j).cleanMemory();

								//not so with results and errors
								if (transformation == DataTransformationType.NONE) {
									//data saved in results
									analysis.getTSRegressionResults().
										getMonthlyEquationRegressionResults(j).get__data().cleanMemory();
									//estimated values
									analysis.getTSRegressionEstimateErrors().
										getMonthlyEquationRegressionErrors(j).clearY1est();
									//data saved in errors
									analysis.getTSRegressionEstimateErrors().
										getMonthlyEquationRegressionErrors(j).getRegressionData().cleanMemory();
									//data saved in results saved in errors
									analysis.getTSRegressionEstimateErrors().getMonthlyEquationRegressionErrors(j).
										getRegressionResults().get__data().cleanMemory();
								}
								else {
									//data saved in results
									analysis.getTSRegressionResultsTransformed().
										getMonthlyEquationRegressionResults(j).get__data().cleanMemory();
									//estimated values
									analysis.getTSRegressionErrorsTransformed().
										getMonthlyEquationRegressionErrors(j).clearY1est();
									//data saved in errors
									analysis.getTSRegressionErrorsTransformed().
										getMonthlyEquationRegressionErrors(j).getRegressionData().cleanMemory();
									//data saved in results saved in errors
									analysis.getTSRegressionErrorsTransformed().getMonthlyEquationRegressionErrors(j).
										getRegressionResults().get__data().cleanMemory();
								}
							}
						}
					}
				}
			}
		}
		//make the object that actually does filling....
		finalAnalyses.add(new TSUtil_FillRegression(dependent,
				RegressionType.OLS_REGRESSION, __analysisMonths, __leZeroLogValue,
				__intercept, __analysisStart, __analysisEnd, __analysisStart, __analysisEnd,
				__minimumDataCount, __minimumR, __confidenceInterval, __fillStart, __fillEnd,
				__fillFlag, __fillFlagDesc, __descriptionString, __bestFitIndicator, analysisList));
	}
	__analysisResults = finalAnalyses;

	//table....
	if (__table != null) {
		for (TSUtil_FillRegression analysis : finalAnalyses) {
			try {
				analysis.saveStatisticsToTable(analysis.getTSToFill(), __table, __tableTSIDColumnName,
					__tableTSIDFormat, RegressionType.OLS_REGRESSION,
					analysis.getNumberOfEquations());
			} catch (Exception e) {
				// Something went wrong with saving statistics....
				Message.printWarning(3, "MixedStationAnalysis", "Unable to save statistics to table");
			}
		}
	}
	
}

// FIXME SAM 2009-08-29 Need to evaluate this - need to pass in the equation coefficients directly because
// using multiple fill commands on the same time series will cause the coefficients to recompute, each time
// considering more filled data, rather than the original raw data values.
//NOTE: as of 7-5-2013, this probably won't work due to changing the classes used to calculate the regression
/**
Creates the FillRegression() and FillMOVE2() commands for each combination of
dependent, independent, transformation and analysis method, for the ranked relationships.
This is a brute force way of applying the filling.  However, the problem is that it cannot be implemented as
simply as this because each command recomputes relationships based on the previous fill information and
therefore does not use the original data.  The FillMixedStation() command that handles all the relationships
internally does use the originally-computed relationships.
@return a list containing command strings.
*/
/*public List<String> createFillCommands()
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

	List<String> commands_Vector = new Vector<String>();

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
}*/

/**
Actually fill the dependent time series....
*/
public void fill() {
	//once we're done analyzing, we can fill without worrying about compromising data integrity....
	for (TSUtil_FillRegression analysis : __analysisResults) {
		analysis.fill();
	}
}

}
