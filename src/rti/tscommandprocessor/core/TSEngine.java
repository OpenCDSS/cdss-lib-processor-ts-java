//------------------------------------------------------------------------------
// TSEngine - class to process time series
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// Notes:	(1)	This class processes time series in bulk fashion and
//			understands tstool command-line options.
//------------------------------------------------------------------------------
// History:
//
// 07 Apr 1998	Steven A. Malers, RTi	Created class to handle bulk processing
//					of time series.
// 30 Jul 1998	SAM, RTi		Pass the requested time series to the
//					getTimeSeries calls.  The low-level
//					code will interpret correctly if the
//					values are zero.
// 31 Aug 1998	SAM, RTi		Finish the enhancements to frost dates
//					and time series filling.
//					For regression, base the regression on
//					the full period but only fill the
//					requested period.
// 22 Sep 1998	SAM, RTi		Add _missing for use with StateMod.
// 28 Sep 1998	SAM, RTi		Allow blank line in commands file.
//					Make so that if time series returned for
//					fillconst() is missing to allocate an
//					empty time series.
// 03 Dec 1998	SAM, RTi		Add the global _fatal_error_count to
//					help track errors.  Wrap each parse
//					option with exception handling.
// 02 Jan 1999	SAM, RTi		Finish up 3.07 enhancements, including
//					extending -fillhistave to monthly data
//					and adding the -averageperiod option.
// 07 Jan 1999	SAM, RTi		Make so -fillhistave works the same
//					for frost dates and other data.  Enable
//					the add() function for frost dates.
// 27 Jan 1999	SAM, RTi		Update to 3.09.  React to feedback from
//					users.
// 23 Feb 1999	SAM, CEN, RTi		Add features to support RGDSS.  Change
//					imports to only import needed packages.
// 06 Apr 1999	CEN, RTi		Added StateModDayTS print call and
//					changed StateMod*TS import to proper 
//					location.
// 17 May 1999	SAM, RTi		Test with Java 1.1.8.  Add genesis
//					output to StateMod output.  Update to
//					pass PropList for StateMod output and
//					include more information in the header.
//					Add the -detailedheader option.
// 09 Jun 1999	SAM, RTi		Add -ofilterdatacoverage option.
//					Allow reports to be exported without
//					preview.  Start adding support for file
//					formats other than StateMod.
// 27 Jul 1999	SAM, RTi		Add day_to_month_reservoir data filling.
// 28 Oct 1999	SAM, RTi		Add ignore <= zero feature for
//					averages.
// 01 Dec 1999	SAM, CEN, RTi		Add ability to fill diversions using
//					diversion comments.
// 27 Jan 2000	SAM, RTi		Add -missing Value1,Value2 to set the
//					range of missing values.  This will
//					allow data from the NWS to be properly
//					processed (-998 and -999 are typically
//					used for missing data).  Minor change
//					to free _tslist when done and tslist
//					before adding. 
// 19 Jul 2000	SAM, RTi		When writing a large number of StateMod
//					files, use a binary file to process.
//					Add -slist ability and binary command
//					line options.  Add ability to write
//					commands file.  Print list of time
//					series not found to the log file.
// 25 Sep 2000	SAM, RTi		Add -include_missing_ts functionality.
// 04 Oct 2000	SAM, RTi		Enable DateValue format.
// 11 Oct 2000	SAM, RTi		Add -oannual_traces_graph option.
//					Add -oyear_to_date_report.
// 25 Oct 2000	SAM, RTi		Enable -oscatter_plot.
// 30 Oct 2000	SAM, RTi		Enable -oduration_graph, -obar_graph.
// 01 Nov 2000	SAM, RTi		Add runInterpreter() to allow multi-step
//					processing.  Track Vector of dates to
//					simplify commands.
// 30 Nov 2000	SAM, RTi		Change -oscatter_plot to
//					-oxyscatter_plot.
// 10 Dec 2000	SAM, RTi		Change NWSHourTS to NWSCardTS.  Add
//					-onwscard to write NWS Card file.
//					Add processTimeSeries() to generate
//					output.  Add finalize() method.
//					Add support for new syntax for
//					fillRegression(), etc.  In most cases
//					treat old and new functionality
//					separately so that code can be migrated
//					more easily without mixing approaches.
// 31 Dec 2000	SAM, RTi		Remove old running average code.  A time
//					series can now be converted to a running
//					average and then output in any format.
// 02 Jan 2001	SAM, RTi		Begin reworking so that old options are
//					specifically noted as such to provide
//					backward compatibility while allowing
//					new code.  In particular, historic
//					averages are now computed as time series
//					are read and are saved with the time
//					series.  Therefore the processCommands()
// 07 Jan 2001	SAM, RTi		Change IO to IOUtil, GUI to GUIUtil.
// 15 Jan 2001	SAM, RTi		Add backward-compatibility features so
//					that time series not found in memory are
//					temporarily queried, used, and
//					discarded.  Enable this for old commands
//					so that old command files can be run as
//					they were before.  Add TEMPTS feature
//					as needed to support
//					backward-compatibility and streamline
//					processing.  Convert preference commands
//					to full expressions to allow toggle.
// 20 Feb 2001	SAM, RTi		Finalize SQL/Daily feature release.
//					Add:
//						TS X = normalize()
//						TS X = newTimeSerie()
//					Change _missing to -999.  This thing
//					has outlived its time.
// 15 Mar 2001	SAM, RTi		Update to allow warning messages in
//					the main processing loop to be
//					temporarily turned off to minimize user
//					mouse clicks.  Replace _isBatch with
//					IOUtil.isBatch().
// 09 May 2001	SAM, RTi		Add multiply(), divide().
// 23 Aug 2001	SAM, RTi		Add relativeDiff(), replaceValue(),
//					runProgram().  When parsing
//					setOutputPeriod(), do not use the space
//					as a delimiter since it can be used for
//					periods with times.  Trim the resulting
//					tokens.
// 30 Aug 2001	SAM, RTi		Fix bug where graphs were reverting to
//					last output time series list contents.
//					Verify that copy() works.  Fix problems
//					in TS library with clone().  Allow
//					missing time series to be automatically
//					inserted with missing data, even in the
//					GUI (this is used especially by the
//					createFromList() command).  Add
//					blend(), cumulate().  Add newGraph() and
//					graph property set commands.  To do so
//					need to wire together some
//					WindowListener code so that graphs can
//					be managed within a TSEngine.
//					Fix fillFrostDatesTSWithHistAve to
//					always fill with the historic average.
//					Previously, it would only do so if the
//					old -fillhistave option was used.
//					Add writeSummary() to support frost date
//					output.  For bar graphs, allow bars to
//					be drawn to the left or right of the
//					date, or centered on the date.
// 2001-11-01	SAM, RTi		Remove references to the old Visualize
//					graphing package.  It is now completely
//					phased out.
// 2001-11-20	SAM, RTi		Change a couple of String.indexOf()
//					calls to StringUtil.indexOfIgnoreCase()
//					to be more flexible.  Fix a bug where
//					the user-specified precision for
//					writeStateMod() was not being
//					recognized.
// 2002-01-14	SAM, RTi		Enable NWSRFS data source.  Use new
//					GRTS classes that will support Swing.
// 2002-01-31	SAM, RTi		Update to handle TS identifiers where
//					input sources can be specified after the
//					TSID:  TSID~input_type~input_name.
// 2002-02-09	SAM, RTi		Modify getTSIdentifiersFromExpressions()
//					to return only the part between the
//					input type and name, so that time series
//					commands only work on the identifiers.
//					Change so createTraces() commands sets
//					the scenario to include the sequence
//					number.
// 2002-02-25	SAM, RTi		Add fillMOVE2().
// 2002-03-22	SAM, RTi		Use analysis period for fillMOVE*()
//					commands.  Change some regression code
//					and parameters to be more generic (e.g.,
//					change RegressMonthly property to
//					AnalyzeMonthly).  Change some
//					"expression" methods to "command"
//					methods.  Overload processCommands() to
//					set properties for use in the TSTool
//					GUI.  Remove the determineQueryPeriod()
//					method since the query, output,
//					and analysis periods are now explicitly
//					defined.  Sort data declarations
//					alphabetically (had become
//					disorganized).  Change so flags are not
//					declared as static since they are only
//					used internally.  Remove code that
//					controlled computation of historic
//					averages - just do it always.
//					setIncludeMissingTS() was not being
//					handled by the command parser.
//					Add support for:
//					fillFromTS(),
//					readUsgsNwis(),
//					setAutoExtendPeriod(),
//					setBinaryTSPeriod(),
//					setFromTS(),
//					setQueryPeriod().
// 2002-04-15	SAM, RTi		Add support for shiftTimeByInterval().
//					Update scale() to accept analysis
//					period.
// 2002-04-17	SAM, RTi		Add support for ARMA().  Add
//					setRegressionPeriod() to help with
//					backward compatibility.
// 2002-04-18	SAM, RTi		Add disaggregate().
// 2002-04-19	SAM, RTi		Fix ARMA() to handle ARMA interval
//					(TSTool 05.05.06).
// 2002-04-22	SAM, RTi		Update to 05.05.07 - final cosmetic
//					changes on existing dialogs for public
//					release.
// 2002-04-23	SAM, RTi		Figure out why free() does not seem to
//					be working.  Had a problem in that the
//					do_XXX() methods were not properly
//					handling the resetting of time series.
//					Update to version 05.05.08.
// 2002-04-23	SAM, RTi		Update to version 05.05.09 - more
//					cosmetic changes.  Make replaceValue()
//					more robust. Add TS x = readDateValue().
//					Add processTSProduct() and consequently
//					implement TSSupplier.
// 2002-04-26	SAM, RTi		Update to version 05.05.10.  Fix bug
//					where readTimeSeries() was not properly
//					handling spaces in file names.
// 2002-04-26	SAM, RTi		Update to version 05.05.11.  Fix some
//					problems with dates in editor dialogs.
// 2002-04-26	SAM, RTi		Update to version 05.05.12.  Expand ARMA
//					features.  Support left and right legend
//					position in graphs.
// 2002-05-08	SAM, RTi		Update to version 05.05.14.  Fix
//					disaggregate(Ormsbee) to set result to
//					zero if the input is all zeros.
// 2002-05-09	SAM, RTi		Fix newTimeSeries() to allow * dates.
// 2002-05-12	SAM, RTi		Update to version 05.06.00.  Add
//					readNwsCard() and writeNwsCard()
//					commands.  Move the log file creation to
//					the tstool.java file (main program).
// 2002-05-26	SAM, RTi		Enable cancel in the warning dialog in
//					the main processing loop - use the new
//					MessageDialogListener feature.  Add
//					adjustExtremes().  Add
//					addTSViewWindowListener() to allow
//					TSTool to listing to TSViewFrame
//					closing.  Add support for RiverWare data
//					type with TS X = readRiverWare() and
//					writeRiverWare().
// 2002-06-05	SAM, RTi		Add monthly mean and summary reports.
// 2002-06-06	SAM, RTi		Add -oriverware support to allow GUI to
//					use File...Save as RiverWare.
// 2002-06-13	SAM, RTi		Add support for MODSIM.
// 2002-06-25	SAM, RTi		Add support to read RiversideDB time
//					series.
// 2002-07-25	SAM, RTi		Fix a bug in writeRiverWare().
// 2002-08-23	SAM, RTi		Update indexOf() to not care if the
//					alias is "" or not when checking the
//					identifiers.  This is because there are
//					cases where the alias is specified in
//					addition to the identifier but the ID
//					is used in commands.  Add addConstant()
//					command.
// 2002-08-27	SAM, RTi		Add TS X = newEndOfMonthTSFromDayTS() to
//					replace the day_to_month_reservoir()
//					command.  The former operates on a TSID
//					whereas the latter actually reads the
//					time series.
// 2002-10-11	SAM, RTi		Update ProcessManager to ProcessManager1
//					to allow transition to Java 1.4.x.
// 2002-10-17	SAM, RTi		Change back to ProcessManager since the
//					update version seems to work well for
//					java 1.1.8 and 1.4.0.
// 2002-11-25	SAM, RTi		Add support for Mexico CSMN time series.
// 2003-03-13	SAM, RTi		* Add support for StateModX time series.
//					* Fix bug in readUsgsNwis() command
//					  where spaces in file names were
//					  causing an error.
//					* Add convertDataUnits() command.
// 2003-04-04	SAM, RTi		* Add support for DIADvisor database.
// 2003-04-18	SAM, RTi		* Add support for RiverWare in the
//					  TSSupplier.
//					* Begin adding more do_XXX() methods -
//					  it appears that the
//					  processTimeSeriesCommands() method may
//					  be having some type of memory bound
//					  issues because of its size.
//					* Remove support for graph() command.
// 2003-05-14	SAM, RTi		* Allow fillRegression() to have an
//					  Intercept property.
// 2003-06-12	SAM, RTi		Update to version 06.00.00.
//					* Start using Swing components.
//					* Use new TS package (DateTime instead
//					  of TSDate, etc.).
//					* Use new HydroBaseDMI instead of HBDMI.
//					* Use new StateMod_TS.
//					* Add support for templates in time
//					  series products.
// 2003-07-29	SAM, RTi		Add ESPTraceEnsemble support.
// 2003-11-02	SAM, RTi		* Add SHEF A support.
// 2003-12-01	SAM, RTi		* Change to calendar year as the
//					  default year type.
//					* Default createFromList() to use
//					  a HydroBase input type.
// 2004-01-15	SAM, RTi		* Enabled writing StateMod files!
// 2004-01-29	SAM, RTi		* Update add() to take * for the list
//					  of time series identifiers.
//					* Remove support for old-style() add -
//					  the code is very old!
// 2004-01-31	SAM, RTi		Version 06.00.08 Beta.
//					* Enabled StateMod daily format write
//					  and optimized performance of that
//					  code some.
//					* Add a StopWatch to record how fast
//					  the commands are processed.
//					* Enable openHydroBase() command and
//					  deprecate setDatabaseEngine(),
//					  setDatabaseHost(), and
//					  setDataSource().
// 2004-02-05	SAM, RTi		Version 06.00.09 Beta.
//					* Enable reading StateCU input type time
//					  series.
//					* Add writeStateCU() command to write
//					  frost dates.
//					* Enable reading StateCU frost dates
//					  time series.
//					* Enable fillHistYearAverage() on any
//					  yearly time series.
//					* Enable statemodMax().
//					* Enable setIgnoreLEZero().
//					* Phase out setUseDiversionComments()
//					  and add fillUsingDiversionComments()
//					  for HydroBase where filling occurs
//					  when the command is executed.
// 2004-02-21	SAM, RTi		Version 06.01.00.
//					* Add fillRepeat().
//					* Unrecognized command was not resulting
//					  in a warning - fix.
//					* Add fillProrate().
//					* Add isTSID() to help identify pure
//					  time series identifiers.
//					* Throw an Exception of the period
//					  cannot be changed after a read - this
//					  was letting some errors through.
// 2004-03-15	SAM, RTi		* Add createYearStatisticsReport()
//					  command for testing bulk analysis of
//					  data.
//					* Enable the deselectTimeSeries() and
//					  selectTimeSeries() commands.
//					* Update writeDateValue() to use
//					  parameters.
//					* Update readStateModB() to take a
//					  free-format parameter list.
// 2004-03-22	SAM, RTi		* Make normal TS read and TSProcessor
//					  use the same readTimeSeries0() method
//					  to actually read time series - that
//					  way there is full support for
//					  TSProducts.
// 2004-03-28	SAM, RTi		* Update createYearStatisticsReport() to
//					  put in a second column with the
//					  time series description, to simplify
//					  reading the results and because the
//					  GeoView summary layer currently
//					  defaults to ID,Name,Values...
// 2004-04-13	SAM, RTi		* Change ESPTraceEnsemble to
//					  NWSRFS_ESPTraceEnsemble.
//					* Remove commented code - TSSupplier and
//					  normal code seem to be able to share
//					  code OK.
// 2004-05-11	Scott 	  		Changed the package to get
//		Townsend, RTi		NWSRFS_ESPTraceEnsemble from NWSRFS to
//					NWSRFS_DMI.
// 2004-05-21	SAM, RTi		* Change the ESPTraceEnsemble to use
//					  input type "NWSRFS_ESPTraceEnsemble".
//					* Enable support for HydroBase WIS time
//					  series.
//					* Fix problem where limits summary
//					  report is not printing properly
//					  because the newlines in the output
//					  are not handled.
//					* Change readESP... to readNWSRFSESP...,
//					  and similar for the write methods.
// 2004-05-28	SAM, RTi		* Expand selectTimeSeries() and
//					  deselectTimeSeries() to take time
//					  series position.
//					* Add TSList parameter to
//					  writeNWSRFSESPTraceEnsemble().
// 2004-07-11	SAM, RTi		* Update deselectTimeSeries() and
//					  selectTimeSeries() to have
//					  "(de)selectAllFirst" parameters to
//					  clear selections.
//					* Update free() to use parameter lists.
//					* Add readStateCU() that reads one or
//					  more time series with wildcards for
//					  the TSID.
// 2004-07-20	SAM, RTi		* For summary time series, if the input
//					  type is HydroBase, print the header
//					  from the comments.  Else, print the
//					  default header.  The __non_co_detected
//					  data member and is removed.
//					* Update the createFromList() command
//					  to use new free-format notation and
//					  use a standard table to read the
//					  file.  Strip the binary time series
//					  capability from the command since
//					  computers typically have enough
//					  memory now.
//					* Remove _graph_list - not used.
// 2004-07-29	SAM, RTi		* Fix bug where createFromList() with
//					  no input name was throwing a null
//					  pointer exception.
//					* Fix bug where writeDateValue was not
//					  recognizing OutputStart, OutputEnd
//					  parameters.
// 2004-08-12	SAM, RTi		* Update setConstant() to free-format
//					  parameters and allow monthly constant
//					  values and set period to be specified.
// 2004-08-25	SAM, RTi		* Add TS X = readHydroBase().
// 2004-09-07	SAM, RTi		* Add NWSRFS_DMI to constructor and
//					  add read commands for NWSRFS FS5Files.
// 2004-11-29	SAM, RTi		* Update to allow individual ESP
//					  traces to be read with an ID.
// 2005-02-17	SAM, RTi		Update to version 06.10.00.
//					* Reenable changeInterval() with generic
//					  functionality.
// 2005-03-14	SAM, RTi		* Add OutputFillMethod and
//					  HandleMissingInputHow parameters to
//					  changeInterval.
// 2005-04-05	SAM, RTi		* Start using the new message viewer
//					  with message tags.
//					* Start phasing in some StateDMI
//					  concepts like prepended commands and
//					  application status bar messages.
//					* Add getTimeSeriesList() to return the
//					  full list of time series.
//					* Deprecate getSelectedTimeSeries() in
//					  favor of getSelectedTimeSeriesList().
//					* Update openHydroBase() to include
//					  UseStoredProcedures and InputName
//					  parameters.
//					* Replace __hbdmi with __hbdmi_Vector to
//					  allow multiple instances of
//					  HydroBaseDMI to be open at the same
//					  time.  Test with the openHydroBase()
//					  method.
//					* When reading from HydroBase, request
//					  the HydroBaseDMI that matches the
//					  requested InputName.
//					* Fix typo in openHydroBase() where
//					  "BatchAndGUI" was used instead of
//					  "GUIAndBatch".
//					* Overload
//					  getTSIdentifiersFromCommands() to
//					  process the entire identifier.  All
//					  commands will likely transition to
//					  using full identifiers or aliases.
//					* Update fillPattern() to use new
//					  free-format parameters.
//					* Add point graph type -opointgraph.
//					* Implement CommandProcessor and start
//					  to prototype a more generalized
//					  handling of time series.
//					* Totally remove setRegressionPeriod(),
//					  other than obsolete warning.
//					* Add -oPredictedValue_graph and
//					  PredictedValueResidual_graph.
// 2005-05-17	SAM, RTi		* Phase out fillHistMonthAverage() code
//					  in favor of the separate command
//					  class.
//					* Change getTimeSeriesToProcess() from
//					  private to protected sine it is now
//					  being called from commands.
//					* Remove the boolean to track warnings
//					  about filling non-month data with
//					  monthly averages.
// 2005-05-18	SAM, RTi		* Phase out fillHistYearAverage() code
//					  in favor of the separate command
//					  class.
// 2005-05-19	SAM, RTi		* Migrate new Command-based code to
//					  RTi.Util.IO and RTi.TS to allow more
//					  general design and sharing with other
//					  applications like StateDMI.
// 2005-05-20	SAM, RTi		* Change getTSIdentifiersFromCommands()
//					  to support block-style comments.
//					* Add warning in add() when FrostDate
//					  time series are being promised, to
//					  help CDSS users transition.
//					* Add command_tag to a number of private
//					  methods to facilitate using the new
//					  log viewer.
//					* Convert TS X = changeInterval() to
//					  new commands.
// 2005-05-26	SAM, RTi		* Phase in new message command tags, to
//					  allow use of the new message log
//					  viewer.
// 2005-05-31	SAM, RTi		* Convert writeRiverWare() to new
//					  command design.
//					* Change getDateTime() to return null if
//					  a null string is passed.
// 2004-06-08	SAM, RTi		Update to version 06.10.02.
//					* Convert openHydroBase() to command and
//					  move to HydroBaseDMI package.
//					* Update readStateCU() to read the CDS
//					  and IPY files.
// 2005-07-05	SAM, RTi		* When creating TSViewJFrame, add the
//					  HydroBaseDMI and RiversideDB_DMI as
//					  interfaces that can save products.
// 2005-07-17	SAM, RTi		* Update selectTimeSeries() to allow the
//					  pattern and position to be specified,
//					  to allow more power for selects.
//					* Update fillProrate() to compute the
//					  factor based on an average of the
//					  independent time series.
// 2005-08-01	SAM, RTi		* Update the fillProrate() InitialValue
//					  parameter to include NearestBackward
//					  and NearestForward.  The default is
//					  now not to automatically look for an
//					  initial value.
// 2005-08-24	SAM, RTi		Update to version 06.10.07.
//					* Convert scale() to command class.
//					* Change TS X = ... to TS Alias = ...
//					  in comments etc.
//					* Convert TS Alias = copy() to command
//					  class.
// 2005-08-30	SAM, RTi		* Convert writeStateMod() to command
//					  class and enhance parameters to
//					  include the output period, missing
//					  data value, and the ability to select
//					  the time series to output.
//					* In getDateTime() return null if a
//					  named DateTime is requested and the
//					  matching object value is null.
//					* In getDateTime() add support for
//					  InputStart and InputEnd, equivalent to
//					  QueryStart and QueryEnd.
//					* Convert readStateMod() to a command
//					  class and enhance parameters to
//					  include the input period.
//					* Add public readTimeSeries2(Vector) to
//					  facilitate use by command classes.
// 2005-09-07	SAM, RTi		Update to version 06.10.08.
//					* Convert fillConstant() to a command
//					  class.
// 2005-09-20	SAM, RTi		* Convert newTimeSeries() to a command
//					  class.
//					* Add the newStatisticYearTS() command.
//					* Update openHydroBase() to take a
//					  database name.
// 2005-09-28	SAM, RTi		Update to version 06.10.08.
//					* Update cumulate() to command.
//					* Add more statistics to
//					  newStatisticYearTS().
//					* Convert readStateModB() to use the
//					  command class.
// 2005-10-18	SAM, RTi		Update to version 06.11.00.
//					* Fix so that processTSProduct() in
//					  batch mode can still preview.
//					* Add addTSViewTSProductAnnotation
//					  Providers().
//					* Add support for the ColoradoSMS
//					  database.  This is only used for
//					  annotating HydroBase data, so there is
//					  currently no need for most of the
//					  time-series logic for this input type.
//					* Convert processTSProduct() to a
//					  command class.
// 2005-11-13	SAM, RTi		* Disable ESP Trace Ensemble code if not
//					  in the jar file.
// ...					Update to version 06.14.00.
// 2005-12-06	J. Thomas Sapienza, RTi	* Moved readNwsCard() command out to
//					  a separate command class.
//					* Added code to print Exceptions thrown
//					  when running commands to the log file
//					  at Debug level 3, if being run with
//					  Debug on.  This is to speed up 
//					  debugging and development.
// 2005-12-14	SAM, RTi		* Convert setQueryPeriod() to the
//					  command class setInputPeriod().
// 2006-01-18	JTS, RTi		NWSCardTS is now in RTi.DMI.NWSRFS_DMI.
//					Note:  writeNwsCard() and read*ESP*()
//					have not been converted to a command
//					class yet.
// 2006-01-31	SAM, RTi		Update to version 06.16.00.
//					* Add NDFD command support.
// 2006-03-27	SAM, RTi		* For newEndOfMonthTSFromDayTS(), save
//					  the original data limits after
//					  creation.
// 2006-04-13	SAM, RTi		* Reenable MOVE2, which was accidentally
//					  commented out - use command classes.
// 2006-04-20	SAM, RTi		Update to version 06.17.00.
//					* Convert readHydroBase() commands to
//					  use a command class.
// 2006-05-18	SAM, RTi		Update to version 06.19.00.
//					* Fix bug where disaggregate was calling
//					  getTimeSeries() with the wrong
//					  parameters.
// 2006-01-19	SAM, RTi		* Change fillUsingDiversionComments() to
//					  automatically extend the data period
//					  if no output period has been
//					  specified.
//					* Change the output period data members
//					  from __output_date1/__output_date2 to
//					  __OutputStart_DateTime/
//					  __OutputEnd_DateTime to be more
//					  consistent with other code.
// 2006-07-13	SAM, RTi		* Manage NDFD Adapters.
// 2006-10-30   Kurt Tometich, RTi  * Commented out variables and packages
//                    pertaining to the legacy dataServices code
// 2006-11-02   KAT, RTi        * Fixed the newDayTSFromMonthAndDayTS
//                    to multiply the monthly volume by (1 / 1.9835).
//                    The old code was multiplying by 1.9385 and should 
//                    have been the reciprocal.  Algorithm is tested with
//                    with regression test commands.
// 2006-11-02   KAT, RTi    * Fixed bug where TS wasn't being ignored when 
//                    daily or monthly data was missing.  Fixed in the
//                    setUsingMonthAndDay method.  Tested by regression
//                    test commands under test/regression/commands.
// 2007-01-11   KAT, RTi    * Fixed a bug in do_readStateCU() where the
//                    file from the command was not taking into account
//                    the current working directory like all other commands.
// 2007-01-16   KAT, RTi    * Fixed a bug in do_writeStateCU() where the
//                    file from the command was not taking into account
//                    the current working directory like all other commands.
// 2007-01-25 	KAT, RTi	Deleted several import statements that were not
//							needed.
// 2007-01-26	KAT, RTi	Moved the do_fillUsingDiversionComments() method
//							to the HydroBase package.  Also deleted the old way
//							of handling this command in processCommands() by
//							allowing it to hit the generic code at the end and 
//							call the TSCommandFactory.  The command was copied
//							to HydroBase similar to the openHydroBase() command.
// 2007-02-08	SAM, RTi	Rename to TSCommandProcessor package.
//					Clean up code based on Eclipse feedback.
//					Change so TSEngine does not implement CommandProcessor -
//					force all interaction to go through the TSCommandProcessor,
//					which has an instance of TSEngine.
//					Change getPropContents() to protected since only
//					TSCommandProcessor should call.
//					Internally change QueryStart/End to InputStart/End -
//					need to transition code at command level as edits occur.
//					Remove setProp() and setPropContents() since these are
//					now handled by the TSCommandProcessor.
// 2007-03-01	SAM, RTi	Fix fillInterpolate() to handle TSID with spaces.
//
// EndHeader

package rti.tscommandprocessor.core;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.String;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

// Code that will be used when service code is fully implemented...
//import rti.common.transfer.TSIdentityTO;
//import rti.common.transfer.TSRequestTO;
//import rti.common.type.PersistenceType;
//import rti.domain.timeseries.TimeSeries;
//import rti.domain.timeseries.TimeSeriesFabricator;
//import rti.transfer.TimeSeriesIdBean;
//import rti.type.TimeSeriesInputType;

import DWR.DMI.HydroBaseDMI.HydroBaseDMI;

import rti.tscommandprocessor.commands.hecdss.HecDssAPI;

import DWR.DMI.SatMonSysDMI.SatMonSysDMI;

import DWR.StateCU.StateCU_BTS;
import DWR.StateCU.StateCU_CropPatternTS;
import DWR.StateCU.StateCU_IrrigationPracticeTS;
import DWR.StateCU.StateCU_TS;

import DWR.StateMod.StateMod_TS;
import DWR.StateMod.StateMod_BTS;

//import RTi.DataServices.Adapter.NDFD.Adapter;
import RTi.DMI.DIADvisorDMI.DIADvisorDMI;
import RTi.DMI.NWSRFS_DMI.NWSCardTS;
import RTi.DMI.NWSRFS_DMI.NWSRFS_ESPTraceEnsemble;
import RTi.DMI.NWSRFS_DMI.NWSRFS_DMI;

import RTi.DMI.RiversideDB_DMI.RiversideDB_DMI;

import RTi.GRTS.TSProductAnnotationProvider;
import RTi.GRTS.TSProductDMI;
import RTi.GRTS.TSViewJFrame;

import RTi.TS.DateValueTS;
import RTi.TS.IrregularTS;
import RTi.TS.MexicoCsmnTS;
import RTi.TS.ModsimTS;
import RTi.TS.MonthTS;
import RTi.TS.MonthTSLimits;
import RTi.TS.RiverWareTS;
import RTi.TS.ShefATS;
import RTi.TS.TS;
import RTi.TS.TSAnalyst;
import RTi.TS.TSEnsemble;
import RTi.TS.TSIdent;
import RTi.TS.TSLimits;
import RTi.TS.TSSupplier;
import RTi.TS.TSUtil;
import RTi.TS.TSUtil_ChangeInterval;
import RTi.TS.UsgsNwisTS;
import RTi.TS.YearTS;

import RTi.Util.GUI.ReportJFrame;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusProvider;
import RTi.Util.IO.CommandStatusProviderUtil;
import RTi.Util.IO.CommandStatusUtil;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.DataFormat;
import RTi.Util.IO.DataUnits;
import RTi.Util.IO.GenericCommand;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.ProcessManager;
import RTi.Util.IO.PropList;
import RTi.Util.IO.UnknownCommandException;
import RTi.Util.Math.MathUtil;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.StopWatch;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.TimeUtil;

public class TSEngine implements TSSupplier, WindowListener
{
	
/**
"ts_action" values for processTimeSeriesAction, indicating how to process
a time series from a command.  These will eventually go away when all processing
is handled in the commands.
*/
public final static int
			INSERT_TS = 1,	
			UPDATE_TS = 2,
			EXIT = 3,
			NONE = 4;

/**
Indicate that temporary time series should be retrieved within a command but not
managed in the processor.  This may go away due to limited use and issues with design.
*/
public final String TEMPTS = "TEMPTS";
public final String TEMPTS_SP="TEMPTS ";

public final int OUTPUT_NONE = 0;		// Initial value for _output_format.
public final int OUTPUT_STATEMOD = 1;		// Formats for outputting the time series.
public final int OUTPUT_SUMMARY = 2;		// Time series summary
public final int OUTPUT_LINEGRAPH = 3;		// Line graph
public final int OUTPUT_LINELOGYGRAPH = 4;	// Could find a way to modify
						// the graph but do like this
						// for now.
public final int OUTPUT_PORGRAPH = 5;		// Period of record graph.
//public final int OUTPUT_SUMMARY_NO_STATS = 8;	// Special output for Ayres
						// software.  Just remove the
						// statistics lines.
public final int OUTPUT_PERCENT_EXCEED_GRAPH =9;// Percent exceedance curve.
public final int OUTPUT_DOUBLE_MASS_GRAPH = 10; // Double mass graph.
public final int OUTPUT_DATEVALUE = 11;		// Output in DateValue format.
public final int OUTPUT_ANNUAL_TRACES_GRAPH =12;// Annual traces graph.
public final int OUTPUT_YEAR_TO_DATE_REPORT =13;// Year to date totals.
public final int OUTPUT_XY_SCATTER_GRAPH = 14;	// Scatter plot
public final int OUTPUT_DURATION_GRAPH = 15;	// Duration graph 
public final int OUTPUT_BAR_GRAPH = 16;		// Bar graph (parallel to each other)
public final int OUTPUT_NWSCARD_FILE = 17;	// Output in NWS Card format.
public final int OUTPUT_DATA_LIMITS_REPORT = 18;// Data limits report
public final int OUTPUT_DATA_COVERAGE_REPORT=19;// Data limits report
public final int OUTPUT_MONTH_MEAN_SUMMARY_REPORT=20;
public final int OUTPUT_MONTH_TOTAL_SUMMARY_REPORT=21; // Monthly summary reports.
public final int OUTPUT_RIVERWARE_FILE = 22;	// Output in RiverWare format.
public final int OUTPUT_SHEFA_FILE = 23;	// Output SHEF .A format.
public final int OUTPUT_NWSRFSESPTRACEENSEMBLE_FILE = 24; // Output NWSRFS ESP Trace Ensemble file
public final int OUTPUT_TABLE = 25;		// Output a table (curently only for display).
public final int OUTPUT_POINT_GRAPH = 26;	// Point graph
public final int OUTPUT_PredictedValue_GRAPH = 27;	// Predicted Value graph
public final int OUTPUT_PredictedValueResidual_GRAPH = 28;  // Predicted Value Residual graph

/**
Filter indicating that output should be data (default).
*/
public final int OUTPUT_FILTER_DATA = 1;
/**
Filter indicating that output should be data coverage (% non-missing).
*/
public final int OUTPUT_FILTER_DATA_COVERAGE = 2;

/**
Output in water year, for use with setOutputYearType().
*/
protected final int _WATER_YEAR = 1;

/**
Output in calendar year.
*/
protected final int _CALENDAR_YEAR = 2;

// Data members...

/**
If true, then if the output period is specified, time series will be extended to
at least include the output period.
*/
private boolean __AutoExtendPeriod_boolean = true;

/**
Start date/time for averaging.
*/
private DateTime __AverageStart_DateTime = null;

/**
End date for averaging.
*/
private DateTime __AverageEnd_DateTime = null;

/**
List of data tests that are defined.
*/
private Vector __datatestlist = null;

/**
List of DateTime initialized from commands.
*/
private Hashtable __datetime_Hashtable = new Hashtable ();

/**
DMI for DIADvisor operational database.
*/
private DIADvisorDMI __DIADvisor_dmi = null;

/**
DMI for DIADvisor archive database.
*/
private DIADvisorDMI __DIADvisor_archive_dmi = null;

// TODO 2007-11-16 - why can't this be local in the method that uses it?
/**
Count of errors.
*/
private int	_fatal_error_count = 0;

/**
HydroBase DMI instance Vector, to allow more than one database instance to
be open at a time.
*/
private Vector __hbdmi_Vector = new Vector();

/**
Indicates whether values <= 0 should be treated as missing when calculating
historical averages.
*/
private boolean __IgnoreLEZero_boolean = false;

/**
Indicates whether missing time series should be added automatically.
*/
private boolean __IncludeMissingTS_boolean = false;

/**
Start date for read.
*/
private DateTime __InputStart_DateTime = null;

/**
End date for read.
*/
private DateTime __InputEnd_DateTime = null;        

/**
Vector to save list of time series identifiers that are not found.
*/
private Vector	__missing_ts = new Vector();

// TODO SAM 2007-02-18 need to reenable NDFD
//private Vector __NDFDAdapter_Vector = new Vector();
						// NDFD Adapter instance
						// Vector, to allow more than
						// one database instance to
						// be open at a time, only
						// used with openNDFD().

/**
List of NWSRFS_DMI to use to read from NWSRFS FS5Files.
*/
private Vector __nwsrfs_dmi_Vector = new Vector();

/**
Default output file name.
*/
private String	__output_file = "tstool.out";

/**
Year type for output (calendar year is the default).
*/
private int __OutputYearType_int = _CALENDAR_YEAR;

/**
Global output start date/time.
*/
private DateTime __OutputStart_DateTime = null;

/**
Global output end date/time.
*/
private DateTime __OutputEnd_DateTime = null;

/**
Indicates whether exported output should be previewed.  The default is false.
 */
private	boolean	__PreviewExportedOutput_boolean = false;

/**
Properties generated/maintained by the processor.  These are initialized when calling
processCommands() and may be modified during command processing.  There are
methods to return key values such as the end result WorkingDir.  This is available to
allow the GUI to process setWorkingDir() commands prior to displaying a command editor,
which requires the working directory at that point of the workflow.
*/
private PropList __processor_PropList = null;

/**
RiversideDB DMI instance Vector, to allow more than one database instance to
be open at a time.
*/
private Vector __rdmi_Vector = new Vector();

/**
Reference date for year to date report (only use month and day).
*/
private DateTime __reference_date = null;

/**
DMI for DIADvisor archive database.
*/
private SatMonSysDMI __smsdmi = null;

/**
The TSCommandProcessor instance that is managing this TSEngine instance.
A valid instance should be passed during construction.
*/
private TSCommandProcessor __ts_processor = null;

/**
Vector of time series vector that is the result of processing.
This will always be non-null.
*/
private Vector __tslist = new Vector(50,50);

/**
WindowListener for TSViewJFrame objects, used when calling app wants to listen for
window events on lot windows.
*/
private WindowListener _tsview_window_listener = null;

/**
Construct a TSEngine to work in parallel with a TSCommandProcessor.
@param ts_processor TSCommandProcessor instance that is controlling processing.
*/
protected TSEngine ( TSCommandProcessor ts_processor )
{
	__ts_processor = ts_processor;
}

/**
Add the annotation provider property to TSView properties.  This examines
DMI instances to see if they implement TSProductAnnotationProvider.  If so,
call the TSViewJFrame.addTSProductAnnotationProvider() method with the instance.
*/
private void addTSViewTSProductAnnotationProviders ( TSViewJFrame view )
{	Vector ap_Vector = getTSProductAnnotationProviders();
	int size = ap_Vector.size();
	for ( int i = 0; i < size; i++ ) {
		view.addTSProductAnnotationProvider((TSProductAnnotationProvider)ap_Vector.elementAt(i), null );
	}
}

/**
Make the TSProeductDMI instances known to a TSViewJFrame.  This examines
DMI instances to see if they implement TSProductDMI.  If so, the
TSView.addTSProductDMI() method is called with the instance.
*/
private void addTSViewTSProductDMIs ( TSViewJFrame view )
{	// Check the HydroBase instances...
	int hsize = __hbdmi_Vector.size();
	HydroBaseDMI hbdmi = null;
	for ( int ih = 0; ih < hsize; ih++ ) {
		hbdmi = (HydroBaseDMI)__hbdmi_Vector.elementAt(ih);
		if ((hbdmi != null) && (hbdmi instanceof TSProductDMI)){
			view.addTSProductDMI(hbdmi);
		}
	}
	// Check the RiversideDB_DMI instances...
    int rsize = __rdmi_Vector.size();
    RiversideDB_DMI rdmi = null;
    for ( int ir = 0; ir < rsize; ir++ ) {
        rdmi = (RiversideDB_DMI)__rdmi_Vector.elementAt(ir);
        if ((rdmi != null) && (rdmi instanceof TSProductDMI)){
            view.addTSProductDMI(rdmi);
        }
    }
}

/**
Add a WindowListener for TSViewJFrame instances that are created.  Currently
only one listener can be set.  This is needed to be able to close down the
application when simple plot interfaces are displayed.
@param listener WindowListener to listen to TSViewJFrame WindowEvents.
*/
public void addTSViewWindowListener ( WindowListener listener )
{	_tsview_window_listener = listener;
}

/**
Append time series to the results list.
@param ts Time series to append
@throws Exception if there is an error appending the time series
*/
protected void appendTimeSeries ( TS ts )
throws Exception
{
	// Position is zero index so request one more than the actual size.
	int size = 0;
	if ( __tslist != null ) {
		size = __tslist.size();
	}
	setTimeSeries ( ts, size );
}

/**
Calculate the average values for a time series.
@return the average data for a time series using the averaging and
output period.  If a monthly time series, a MonthTSLimits will be returned.
Otherwise, a TSLimits will be returned.
The overloaded method is called with a time series counter having the return
value of getTimeSeriesSize().
@param ts Monthly time series to process.
@exception Exception if there is an error getting the limits.
*/
protected TSLimits calculateTSAverageLimits ( TS ts )
throws Exception
{	return calculateTSAverageLimits ( getTimeSeriesSize(), ts );
}

/**
Calculate the average data limits for a time series using the averaging period
if specified (otherwise use the available period).
@return the average data for a time series.
If a monthly time series, a MonthTSLimits will be returned.
@param i Counter for time series being processed (starting at zero), used to
control printing of messages.
@param ts Monthly time series to process.
Currently only limits for monthly time series are supported.
@exception Exception if there is an error getting the limits.
*/
private TSLimits calculateTSAverageLimits ( int i, TS ts )
throws Exception
{	String message, routine = "TSEngine.calculateTSAverageLimits";
	TSLimits average_limits = null;

	if ( ts == null ) {
		message = "Time series is null.  Can't calculate average limits.";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	if ( Message.isDebugOn ) {
		Message.printDebug ( 1, routine, "Trying to calculate limits for " + ts.getIdentifierString() );
	}
	if ( ts.getDataIntervalBase() == TimeInterval.MONTH ) {
		// Set the flag to pass to the limits code...
		int limits_flag = 0;
		if ( getIgnoreLEZero() ) {
			limits_flag = TSLimits.IGNORE_LESS_THAN_OR_EQUAL_ZERO;
		}
		try {
		    if ( haveAveragingPeriod() ) {
				// Get the average values from the averaging period...
				if ( i <= 0 ) {
					Message.printStatus ( 2, routine, "Specified averaging period is:  " +
					getAverageStart() + " to " + getAverageEnd() );
				}
				average_limits = new MonthTSLimits( (MonthTS)ts, getAverageStart(), getAverageEnd(), limits_flag);
			}
			else {
			    // Get the average values from the available period...
				if ( i <= 0 ) {
					// Print the message once...
					Message.printStatus ( 2, routine,
					"No averaging period specified.  Will use available period.");
				}
				average_limits = new MonthTSLimits( (MonthTS)ts, ts.getDate1(), ts.getDate2(), limits_flag );
			}
		}
		catch ( Exception e ) {
			message = "Error getting limits for time series.";
			Message.printWarning ( 2, routine, message );
			throw new Exception ( message );
		}
	}
	else if ( ts.getDataIntervalBase() == TimeInterval.YEAR ) {
		// Set the flag to pass to the limits code...
		int limits_flag = 0;
		if ( getIgnoreLEZero() ) {
			limits_flag = TSLimits.IGNORE_LESS_THAN_OR_EQUAL_ZERO;
		}
		try {
		    if ( haveAveragingPeriod() ) {
				// Get the average values from the averaging period...
				if ( i <= 0 ) {
					Message.printStatus ( 2, routine, "Specified averaging period is:  " +
					getAverageStart() + " to " + getAverageEnd().toString() );
				}
				average_limits = new TSLimits( ts, getAverageStart(), getAverageEnd(), limits_flag);
			}
			else {
			    // Get the average values from the available period...
				if ( i <= 0 ) {
					// Print the message once...
					Message.printStatus ( 2, routine,
					"No averaging period specified.  Will use available period.");
				}
				average_limits = new TSLimits ( ts,	ts.getDate1(), ts.getDate2(), limits_flag );
			}
		}
		catch ( Exception e ) {
			message = "Error getting limits for time series.";
			Message.printWarning ( 2, routine, message );
			throw new Exception ( message );
		}
	}
	else {
	    // For now we probably won't use average limits for other
		// time steps so to increase performance don't compute...
		// If historical averages are used for filling daily, then add daily at some point.
	    // FIXME SAM 2008-08-18 Need to at least compute overall max, min, etc. for any period, as long
	    // as it is not a performance hit.
		if ( Message.isDebugOn ) {
			Message.printStatus ( 2, routine,
			"Calculation of historic average limits for other " +
			"than monthly and yearly data is not enabled: " +
			ts.getIdentifierString() + ")" );
		}
	}
	return average_limits;
}

/**
Clear the time series results.  The commands will need to be rerun to regenerate the results.
*/
protected void clearTimeSeriesResults ( )
{
	if ( __tslist != null ) {
		__tslist.removeAllElements();
	} 
}

/**
Create data limits report.  Currently this creates a report for the available
period for the time series (it does not check the output period).
@param tslist list of time series to analyze.
*/
private Vector createDataLimitsReport ( Vector tslist )
{	int size = tslist.size();
	Vector report = new Vector ();

	report.addElement ( "" );
	report.addElement ( "DATA LIMITS REPORT" );
	report.addElement ( "" );

	TS ts = null;
	TSLimits limits = null;
	for ( int i = 0; i < size; i++ ) {
		ts = (TS)tslist.elementAt(i);
		if ( ts == null ) {
			continue;
		}
		if ( ts.getDataIntervalBase() == TimeInterval.MONTH ) {
			try {	limits = new MonthTSLimits ( (MonthTS)ts );
			}
			catch ( Exception e ) {
				limits = null;
			}
		}
		else {	limits = TSUtil.getDataLimits ( ts,
				ts.getDate1(), ts.getDate2() );
		}
		if ( limits == null ) {
			continue;
		}
		report.addElement ( "" );
		report.addElement ( ts.getDescription() );
		report.addElement ( ts.getIdentifierString() );
		report.addElement ( "" );
		// The limits come back as a string with line breaks.  This
		// apparenltly is displayed correctly but does not print
		// correctly so break into separate strings here in order to
		// keep the strings consistent...
		StringUtil.addListToStringList ( report,
			StringUtil.breakStringList(limits.toString(),
			"\n", 0) );
		report.addElement ( "" );
	}
	ts = null;
	limits = null;
	return report;
}

/**
Create monthly summary reports.  Currently this creates a report for the
available period for the time series (it does not check the output period).
@param tslist list of time series to analyze.
@param props Properties to control report (see TSUtil.createMonthSummary()).
*/
private Vector createMonthSummaryReport ( Vector tslist, PropList props )
{	int size = tslist.size();
	Vector report = new Vector ();
	String routine = "TSEngine.createMonthSummaryReport";

	String prop_val = props.getValue ( "DayType" );
	
	report.addElement ( "" );
	if ( (prop_val != null) && prop_val.equalsIgnoreCase("Total") ) {
		report.addElement ( "MONTHLY SUMMARY REPORT (Daily Totals)" );
	}
	else {
	    report.addElement ( "MONTHLY SUMMARY REPORT (Daily Means)" );
	}
	report.addElement ( "" );

	TS ts = null;
	Vector summary = null;
	int interval_base;
	int error_count = 0;
	for ( int i = 0; i < size; i++ ) {
		ts = (TS)tslist.elementAt(i);
		if ( ts == null ) {
			continue;
		}
		interval_base = ts.getDataIntervalBase();
		if ( (interval_base == TimeInterval.DAY) || (interval_base == TimeInterval.HOUR) ||
			(interval_base == TimeInterval.MINUTE) ) {
			try {
			    summary = TSUtil.createMonthSummary ( ts, ts.getDate1(), ts.getDate2(), props );
				if ( summary == null ) {
					continue;
				}
			}
			catch ( Exception e ) {
				Message.printWarning ( 2, "", e );
				++error_count;
				continue;
			}
			report.addElement ( "" );
			StringUtil.addListToStringList ( report, summary );
			report.addElement ( "" );
		}
		else {
		    report.addElement ( "" );
			report.addElement ( ts.getDescription() );
			report.addElement ( ts.getIdentifierString() );
			report.addElement ( "" );
			report.addElement ( "Summary report is not supported for time series interval." );
			report.addElement ( "" );
			++error_count;
		}
	}
	if ( error_count > 0 ) {
		Message.printWarning ( 1, routine,
		"There was an unexpected error creating the report.  See the log file for details." );
	}
	ts = null;
	summary = null;
	return report;
}

/**
Create a year to date report listing the total accumulation of water volume for
each year to the specified date.  Handle real-time and historic.  But only CFS units.
@param tslist list of time series to analyze.
@param end_date Ending date for annual total (precision day!).
@param props Properties to control output (currently the only property is
SortTotals=true/false).
*/
private Vector createYearToDateReport ( Vector tslist, DateTime end_date, PropList props )
{	int size = tslist.size();
	Vector report = new Vector ();

	report.addElement ( "" );
	report.addElement ( "Annual totals to date ending on " +
			TimeUtil.MONTH_ABBREVIATIONS[end_date.getMonth() - 1] + " " + end_date.getDay() );
	report.addElement ( "" );

	TS ts = null;
	DateTime start = null;
	DateTime end = null;
	int nyears = 0;
	double totals[] = null;
	int years[] = null;
	int missing[] = null;
	int ypos = 0;
	for ( int i = 0; i < size; i++ ) {
		ts = (TS)tslist.elementAt(i);
		if ( ts == null ) {
			continue;
		}
		if (	!(((ts.getDataIntervalBase() == TimeInterval.DAY) ||
			(ts.getDataIntervalBase() == TimeInterval.IRREGULAR)) &&
			ts.getDataUnits().equalsIgnoreCase("CFS")) ) {
			report.addElement ( "" );
			report.addElement ( ts.getIdentifierString() + " - " + ts.getDescription() + " Not CFS" );
			report.addElement ( "" );
			continue;
		}
		report.addElement ( "" );
		report = StringUtil.addListToStringList ( report,
			ts.getComments());
		report.addElement ( "" );
		report.addElement ( "         Total           Number" );
		report.addElement ( "         ACFT through    of missing" );
		report.addElement ( "Year     "
			+ TimeUtil.MONTH_ABBREVIATIONS[end_date.getMonth() - 1]
			+ " "
			+ StringUtil.formatString(end_date.getDay(),"%02d")
			+ "  days" );

		// If an irregular time series, convert to a daily...

		if ( ts.getDataIntervalBase() == TimeInterval.IRREGULAR ) {
			// Convert to a daily time series...
		    TSUtil_ChangeInterval tsu = new TSUtil_ChangeInterval ();
			ts = tsu.OLDchangeToDayTS ( (IrregularTS)ts, 1, null );
		}

		int interval_base = ts.getDataIntervalBase();
		int interval_mult = ts.getDataIntervalMult();
		start = new DateTime (ts.getDate1());	// Overall start
		start.setPrecision ( DateTime.PRECISION_DAY );
		int year1 = start.getYear();
		end = new DateTime(ts.getDate2());	// Overall end
		end.setPrecision ( DateTime.PRECISION_DAY );
		nyears = end.getYear() - start.getYear() + 1;
		// Should not hit the following now since put in a check above.
		if ( interval_base == TimeInterval.MONTH ) {
			// Should make comparisons work...
			end.setDay ( TimeUtil.numDaysInMonth(end.getMonth(), end.getYear()) );
		}
		// Memory for yearly values...
		totals = new double[nyears];
		missing = new int[nyears];
		years = new int[nyears];
		for ( int j = 0; j < nyears; j++ ) {
			totals[j] = 0.0;	// OK to initialize to zero since we are tracking the number of missing now.
			years[j] = year1 + j;
			missing[j] = 0;
		}
		double value = 0.0;
		// Loop using addInterval...
		DateTime date = new DateTime ( start );
		// Date to stop accumulating for the year, passed in...
		DateTime compare_date = new DateTime ( end_date );
		compare_date.setYear ( date.getYear());
		// Use next_year as a quick check for whether we have gone to the next year...
		int next_year = date.getYear() + 1;
		for ( ;
			date.lessThanOrEqualTo( end );
			date.addInterval(interval_base, interval_mult)){
			value = ts.getDataValue ( date );
			if (	date.lessThanOrEqualTo(compare_date) ) {
				// Got the data value...
				;
			}
			// This checks exactly for the next year, which will work with daily or other regular data data...
			else if ( date.getYear() == next_year ) {
				// Have read the next starting date (start of next year)...
				// Need to set the new compare date...
				compare_date.setYear ( date.getYear());
				++next_year;
				// Data value still in memory and will be checked below...
			}
			else {
			    // Period after compare_date and before the end of the year.  Ignore the data in the total...
				continue;
			}
			// Below here have a valid piece of data so check it for correctness...
			ypos = date.getYear() - year1;
			if ( ts.isDataMissing(value) ) {
				// Keep track of how many missing in the year...
				++missing[ypos];
			}
			else {	if ( interval_base == TimeInterval.DAY ) {
					// Convert daily average flow to ACFT...
					value = value*1.9835;
				}
				//Message.printStatus ( 1, "", "Adding " + value + " to year " +
				//years[ypos] + " " + date.toString());
				if ( totals[ypos] < 0.0 ) {
					// Initial value of total so set it...
					totals[ypos] = value;
				}
				else {
				    // Just add to existing total...
					totals[ypos] += value;
				}
			}
		}
		// Print the sorted totals...
		int sort_order[] = new int[nyears];
		MathUtil.sort ( totals, MathUtil.SORT_QUICK, MathUtil.SORT_ASCENDING, sort_order, true );
		for ( int j = 0; j < nyears; j++ ) {
			report.addElement ( years[sort_order[j]] + "  "
			+ StringUtil.formatString(totals[j], "%11.0f") +
			"          " +
			StringUtil.formatString(missing[sort_order[j]], "%5d"));
		}
		sort_order = null;
		totals = null;
		years = null;
		start = null;
		missing = null;
		end = null;
	}
	return report;
}

/**
Create the data coverage report dialog.
@return contents of the report.
@param orig_tslist Original list of time series to process.
@exception Exception if there is an error.
*/
private Vector createDataCoverageReport ( Vector orig_tslist )
throws Exception
{	String routine = "TSEngine.createDataCoverageReport";
	// If the output is to be a data coverage time series, convert each
	// time series to the monthly statistics format and feed into the
	// report.  This may be slower than before TSTool 05.00.xx but is
	// consistent with how TSTool handles time series in memory now.

	MonthTS newts = null;
	Message.printStatus ( 1, routine, "Starting report at:  " +	new DateTime(DateTime.DATE_CURRENT).toString() );
	TSAnalyst analyst = new TSAnalyst();
	// Start the data coverage report...
	try {	PropList props = new PropList ( "tsanalyst" );
		if ( getOutputYearTypeInt() == _WATER_YEAR ) {
			props.set( "CalendarType=WaterYear" );
		}
		else {
		    // Default...
		}
		if ( haveOutputPeriod() ) {
			// Use it...
			analyst.startDataCoverageReport ( __OutputStart_DateTime,	__OutputEnd_DateTime, props );
		}
		else {
		    // Figure out the maximum period and start the report with that...
			TSLimits report_limits = TSUtil.getPeriodFromTS ( __tslist, TSUtil.MAX_POR );
			analyst.startDataCoverageReport ( report_limits.getDate1(),	report_limits.getDate2(), props );
		}
		props = null;
	}
	catch ( Exception e ) {
		// Data coverage report was a failure, just ignore for now.
		throw new Exception ( "Error starting data coverage report." );
	}
	// Convert each time series to a monthly statistics time series.
	Message.printStatus ( 1, routine, "Changing time series to data coverage time series." );
	int ntslist = 0;
	if ( orig_tslist != null ) {
		ntslist = orig_tslist.size();
	}
	for ( int i = 0; i < ntslist; i++ ) {
		try {
		    newts = TSAnalyst.createStatisticMonthTS( (TS)orig_tslist.elementAt(i), null );
			analyst.appendToDataCoverageSummaryReport ( newts );
			// Don't actually need the time series...
			newts = null;
		}
		catch ( Exception e ) {
			Message.printWarning ( 2, routine, "Error creating Statistic Month TS for \"" +
			((TS)orig_tslist.elementAt(i)).getIdentifierString() + "\"" );
			continue;
		}
	}
	// Now return the report contents...
	Vector report = analyst.getDataCoverageReport();
	analyst = null;
	newts = null;
	return report;
}

// TODO SAM 2005-09-14 Evaluate how this works with other TSAnalyst capabilities
/**
Execute the following command:
<pre>
createYearStatisticsReport(OutputFile="x",TSOutputFile="x")
</pre>
@param command Command to parse.
@exception Exception if there is an error.
*/
private void do_createYearStatisticsReport ( String command )
throws Exception
{	String routine = "TSEngine.do_createYearStatisticsReport", message;
	Vector tokens = StringUtil.breakStringList ( command, "()", StringUtil.DELIM_SKIP_BLANKS );
	if ( (tokens == null) || (tokens.size() < 1) ) {
		// Should never happen because the command name was parsed before...
		throw new Exception ( "Bad command: \"" + command + "\"" );
	}
	// Get the input needed to process the file...
	PropList props = PropList.parse ( (String)tokens.elementAt(1), routine, "," );
	String OutputFile = props.getValue ( "OutputFile" );
	String TSOutputFile = props.getValue ( "TSOutputFile" );
	String MeasTimeScale = props.getValue ( "MeasTimeScale" );
	String Statistic = props.getValue ( "Statistic" );

	if ( OutputFile == null ) {
		message = "The output file for the report is not specified.";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}

	if ( MeasTimeScale == null ) {
		message = "The measurement time scale must be specified as Accm, Mean, or Inst.";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}

	if ( Statistic == null ) {
		message = "The statistic time must be specified as Mean or Sum";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}

	// Loop through the time series and convert to yearly...  Do it the brute force way right now...

	int nts = getTimeSeriesSize();
	Vector yts_Vector = new Vector(nts);
	YearTS yts = null;
	TS ts = null;
	TSIdent tsident = null;
	DateTime datetime, datetime2;
	double total = 0.0, value = 0.0;
	int count = 0;
	for ( int its = 0; its < nts; its++ ) {
		ts = getTimeSeries(its);	// Will throw Exception
		if ( ts.getDataIntervalBase() == TimeInterval.YEAR ) {
			// Just add to the list...
			yts = (YearTS)ts;
		}
		else if ( ts.getDataIntervalBase() == TimeInterval.MONTH ) {
			// Create a new time series and accumulate...
			tsident = new TSIdent ( ts.getIdentifier() );
			tsident.setInterval ( "Year" );
			yts = new YearTS ();
			yts.setIdentifier ( tsident );
			yts.setDescription ( ts.getDescription() );
			yts.setDate1 ( ts.getDate1() );
			yts.setDate2 ( ts.getDate2() );
			yts.setDataUnits ( ts.getDataUnits() );
			yts.allocateDataSpace();
			datetime = new DateTime(ts.getDate1());
			// Accumulate in calendar time...
			datetime.setMonth(1);
			datetime2 = ts.getDate2();
			datetime2.setMonth ( 12 );
			for ( ; datetime.lessThanOrEqualTo(datetime2);
				datetime.addMonth(1) ) {
				value = ts.getDataValue(datetime);
				if ( !ts.isDataMissing(value) ) {
					total += value;
					++count;
				}
				if ( datetime.getMonth() == 12 ) {
					// Transfer to year time series only if all data are available in month...
					if ( count == 12 ) {
						if ( MeasTimeScale.equalsIgnoreCase( "Mean") ) {
							yts.setDataValue( datetime, total/(double)count);
						}
						else if ( MeasTimeScale.equalsIgnoreCase( "Accm") ) {
							yts.setDataValue( datetime, total);
						}
					}
					// Reset the accumulators...
					total = 0.0;
					count = 0;
				}
			}
		}
		// Add to the list...
		yts_Vector.addElement ( yts );
	}
	// Now open the output file and average all the values...
	PrintWriter out = null;
	String full_filename = IOUtil.getPathUsingWorkingDir ( OutputFile );
	out = new PrintWriter(new FileOutputStream(full_filename));
	IOUtil.printCreatorHeader ( out, "#", 80, 0 );
	int size = yts_Vector.size();
	double statistic_val;
	double [] yts_data;
	for ( int i = 0; i < size; i++ ) {
		yts = (YearTS)yts_Vector.elementAt(i);
		yts_data = TSUtil.toArray ( yts, null, null );
		statistic_val = -999.0;
		if ( Statistic.equalsIgnoreCase("Mean") ) {
			statistic_val = MathUtil.mean ( yts_data.length, yts_data, yts.getMissing() );
		}
		else if ( Statistic.equalsIgnoreCase("Sum") ) {
			statistic_val = MathUtil.sum ( yts_data.length, yts_data, yts.getMissing() );
		}
		out.println("\"" + yts.getLocation() + "\",\"" +
			yts.getDescription() + "\"," + StringUtil.formatString(statistic_val,"%.6f") );
	}
	out.flush();
	out.close();
	// If the time series output file was specified, write out the time series that were analyzed...
	if ( TSOutputFile != null ) {
		DateValueTS.writeTimeSeriesList ( yts_Vector,
			IOUtil.getPathUsingWorkingDir ( TSOutputFile ),	null, null, null, true );
	}
}

/**
Helper method to execute the fillMixedStation() command.
@param command_tag Command number used for messaging.
@param command Command to process.
@exception Exception if there is an error processing the command.
*/
/* TODO SAM 2005-05-27 Delete when in the command class
private void do_fillMixedStation ( String command_tag, String command )
throws Exception
{	String message, routine = "TSEngine.do_fillMixedStation";
	Vector tokens = StringUtil.breakStringList ( command,
		"()", StringUtil.DELIM_SKIP_BLANKS );
	if ( (tokens == null) || tokens.size() < 2 ) {
		// Must have at least the command name and a TSID...
		message = "Bad command \"" + command + "\"";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	// Get the input needed to process the file...
	PropList props = PropList.parse (
		(String)tokens.elementAt(1), "fillMixedStation","," );
	String DependentTSList = props.getValue ( "DependentTSList" );
	String DependentTSID = props.getValue ( "DependentTSID" );
	String IndependentTSList = props.getValue ( "IndependentTSList" );
	String IndependentTSID = props.getValue ( "IndependentTSID" );

	if ( DependentTSList == null ) {
		DependentTSList = "AllTS";
	}
	if ( IndependentTSList == null ) {
		IndependentTSList = "AllTS";
	}

	// Most of the work is done by the MixedStationAnalysis object.
	// Determine the dependent and independent time series lists and pass
	// in all the properties...

	try {	Vector dependent_tslist = null;
		Vector independent_tslist = null;
		// Get the list of dependent time series to process...
		if (	DependentTSList.equalsIgnoreCase("AllTS") ||
			((DependentTSID != null) && DependentTSID.equals("*"))){
			// All the time series...
			dependent_tslist = getTimeSeriesList(null);
		}
		else if ( DependentTSList.equalsIgnoreCase("SelectedTS") ) {
			// Get the selected time series...
			dependent_tslist = getSelectedTimeSeries( false );
		}
		else if ( DependentTSList.equalsIgnoreCase("SpecifiedTS") ) {
			// Get the specified matching time series...
			dependent_tslist = getSpecifiedTimeSeries(
				command_tag,
				StringUtil.breakStringList(DependentTSID,
				",",StringUtil.DELIM_ALLOW_STRINGS),
				routine, command );
			// TODO SAM 2005-04-12 how should errors be handled?
		}
		// Get the list of independent time series to process...
		if (	IndependentTSList.equalsIgnoreCase("AllTS") ||
			((IndependentTSID != null) &&
			IndependentTSID.equals("*"))){
			// All the time series...
			independent_tslist = getTimeSeriesList(null);
		}
		else if ( IndependentTSList.equalsIgnoreCase("SelectedTS") ) {
			// Get the selected time series...
			independent_tslist = getSelectedTimeSeries( false );
		}
		else if ( IndependentTSList.equalsIgnoreCase("SpecifiedTS") ) {
			// Get the specified matching time series...
			independent_tslist = getSpecifiedTimeSeries(
				command_tag,
				StringUtil.breakStringList(IndependentTSID,
				",",StringUtil.DELIM_ALLOW_STRINGS),
				routine, command );
			// TODO SAM 2005-04-12 how should errors be handled?
		}
		// Create the analysis object and analyze...
		MixedStationAnalysis msa = new MixedStationAnalysis (
			dependent_tslist, independent_tslist, props );
		// TODO SAM 2005-04-12
		// Separate method to fill??
	}
	catch ( Exception e ) {
		message =
		"There were warnings performing the mixed station analysis.";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
}
*/

/**
Helper method to execute the fillProrate() command.
@param command Command to process.
@exception Exception if there is an error processing the command.
*/
private void do_fillProrate ( String command )
throws Exception
{	String message, routine = "TSEngine.do_fillProrate";
	Vector tokens = StringUtil.breakStringList ( command, "()", StringUtil.DELIM_SKIP_BLANKS );
	if ( (tokens == null) || tokens.size() < 2 ) {
		// Must have at least the command name and a TSID...
		message = "Bad command \"" + command + "\"";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	// Get the input needed to process the file...
	PropList props = PropList.parse ( (String)tokens.elementAt(1), "fillProrate","," );
	String TSID = props.getValue ( "TSID" );
	String IndependentTSID = props.getValue ( "IndependentTSID" );
	String FillStart = props.getValue ( "FillStart" );
	String FillEnd = props.getValue ( "FillEnd" );
	//String CalculateFactorHow = props.getValue ( "CalculateFactorHow" );
	String AnalysisStart = props.getValue ( "AnalysisStart" );
	String AnalysisEnd = props.getValue ( "AnalysisEnd" );
	String InitialValue = props.getValue ( "InitialValue" );
	String FillDirection = props.getValue ( "FillDirection" );
	//String FillFlag = props.getValue ( "FillFlag" );

	// The props are passed on to some methods...

	DateTime start = null;
	DateTime end = null;
	if ( FillStart != null ) {
		try {
		    start = DateTime.parse(FillStart);
		}
		catch ( Exception e ) {
			message = "Fill start is not a valid date/time...ignoring.";
			Message.printWarning ( 2, routine, message );
			throw new Exception ( message );
		}
	}
	if ( FillEnd != null ) {
		try {
		    end = DateTime.parse(FillEnd);
		}
		catch ( Exception e ) {
			message = "Fill end is not a valid date/time...ignoring.";
			Message.printWarning ( 1, routine, message );
			throw new Exception ( message );
		}
	}
	//DateTime AnalysisStart_DateTime = null;
	//DateTime AnalysisEnd_DateTime = null;
	if ( AnalysisStart != null ) {
		try {
		    start = DateTime.parse(AnalysisStart);
		}
		catch ( Exception e ) {
			message = "Analysis start is not a valid date/time...ignoring.";
			Message.printWarning ( 2, routine, message );
			throw new Exception ( message );
		}
	}
	if ( AnalysisEnd != null ) {
		try {
		    end = DateTime.parse(AnalysisEnd);
		}
		catch ( Exception e ) {
			message = "Analysis end is not a valid date/time...ignoring.";
			Message.printWarning ( 1, routine, message );
			throw new Exception ( message );
		}
	}
	// Set defaults if not specified...
	if ( InitialValue != null ) {
		if ( !InitialValue.equalsIgnoreCase("NearestBackward") &&
			!InitialValue.equalsIgnoreCase("NearestForeward") &&
			!StringUtil.isDouble(InitialValue) ) {
			message = "InitialValue \"" + InitialValue + "\" must be a number, NearestBackward or NearestForeward.";
			Message.printWarning ( 2, routine, message );
			throw new Exception ( message );
		}
	}
	if ( FillDirection == null ) {
		FillDirection = "Forward";
	}
	if ( !FillDirection.equalsIgnoreCase("Forward") && !FillDirection.equalsIgnoreCase("Backward") ) {
		message = "FillDirection \"" + FillDirection + "\" is not an Forward or Backward.";
		Message.printWarning ( 2, routine, message );
	}
	// Get the independent time series...
	int ts_pos = indexOf ( IndependentTSID );
	if ( ts_pos < 0 ) {
		message = "Unable to find independent time series \"" +
		IndependentTSID + "\".  Cannot fill.";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	TS independent_ts = getTimeSeries ( ts_pos );
	TS ts = null;	// Time series instance to update
	if ( TSID.equals("*") ) {
		// Fill everything in memory...
		int nts = getTimeSeriesSize();
		for ( int its = 0; its < nts; its++ ) {
			ts = getTimeSeries(its);	// Will throw Exception
			// Do not fill the independent time series...
			if ( ts == independent_ts ) {
				continue;
			}
			if ( InitialValue == null ) {
				TSUtil.fillProrate ( ts, independent_ts, start, end, props );
			}
			else {
			    TSUtil.fillProrate ( ts, independent_ts, start, end, props );
			}
			processTimeSeriesAction ( UPDATE_TS, ts, its );
		}
	}
	else {	// Fill one time series...
		ts_pos = indexOf ( TSID );
		if ( ts_pos >= 0 ) {
			ts = getTimeSeries ( ts_pos );
			if ( InitialValue == null ) {
				TSUtil.fillProrate ( ts, independent_ts, start, end, props );
			}
			else {
			    TSUtil.fillProrate ( ts, independent_ts, start, end, props );
			}
			processTimeSeriesAction ( UPDATE_TS, ts, ts_pos );
		}
		else {
		    message = "Unable to find time series to fill \"" + TSID + "\".  Cannot fill.";
			Message.printWarning ( 2, routine, message );
			throw new Exception ( message );
		}
	}
	tokens = null;
}

// TODO SAM 2005-05-19 Remove when MOVE2 code is transferred to a command.
// Currently fillRegression is run in a separate command class but the MOVE2 code is used below.
/**
Helper method for old and new style regression, and also MOVE1, and MOVE2.
@param command_string Command being evaluated.
@param ts_to_fill time series to fill.
@param ts_independent independent time series.
@param props Properties to use for regression, passed to the TSRegression
command.  In particular, the FillPeriodStart and FillPeriodEnd
propeties can be set to define the analysis period for filling.
@exception Exception if there is an error processing the time series.
*/
/*
private TS do_fillRegression ( String command_string )
throws Exception
{	String routine = "TSEngine.do_fillRegression";
	Command command = TSCommandFactory ( command_string ); 
	if (	(command_string.indexOf('=') < 0) ||
		(StringUtil.patternCount(command_string,"=") == 1) ) {
		// Old fixed-format syntax, which below has "new" style for
		// the case where Intercept=X is included at the end.
			// New-style regression...
			// Parse up front.  Don't parse with spaces because a
			// TEMPTS may be present.
			Vector v = StringUtil.breakStringList(expression,
				"(),\t", StringUtil.DELIM_SKIP_BLANKS |
				StringUtil.DELIM_ALLOW_STRINGS );
			int ntokens = 0;
			if ( v != null ) {
				ntokens = v.size();
			}
			if ( ntokens < 5 ) {
				Message.printWarning ( 2, routine,
				"Syntax error in \"" + expression + "\"" );
				++error_count;
				v = null;
				continue;
			}

			// Get the individual tokens of the expression...

			int ic = 0;
			String command = ((String)v.elementAt(ic++)).trim();
			alias = ((String)v.elementAt(ic++)).trim();
			String independent = ((String)v.elementAt(ic++)).trim();
			String num_equations=((String)v.elementAt(ic++)).trim();
			String transformation =
				((String)v.elementAt(ic++)).trim();
			String dep_analysis_period_start = "*";
			String intercept = null;
			String dep_analysis_period_end = "*";
			String ind_analysis_period_start = "*";
			String ind_analysis_period_end = "*";
			String fill_period_start = "*";	// All available
			String fill_period_end = "*";	// All available
			if ( command.equalsIgnoreCase("fillMOVE2") ) {
				// Have dependent and independent analysis
				// periods...
				dep_analysis_period_start =
					((String)v.elementAt(ic++)).trim();
				dep_analysis_period_end =
					((String)v.elementAt(ic++)).trim();
				ind_analysis_period_start =
					((String)v.elementAt(ic++)).trim();
				ind_analysis_period_end =
					((String)v.elementAt(ic++)).trim();
			}
			else {	// fillMOVE1() and fillRegression() may have
				// the dependent analysis period.  If they
				// have that they will also have the fill period
				// below...
				int icmax = ic + 1;
				if ( ntokens >= icmax ) {
					dep_analysis_period_start =
					((String)v.elementAt(ic++)).trim();
				}
				if ( ntokens >= (icmax + 1) ) {
					dep_analysis_period_end =
					((String)v.elementAt(ic++)).trim();
				}
			}
			// All others have the fill period...
			int icmax = ic + 1;
			if ( ntokens >= icmax ) {
				fill_period_start =
					((String)v.elementAt(ic++)).trim();
			}
			if ( ntokens >= (icmax + 1) ) {
				fill_period_end =
					((String)v.elementAt(ic++)).trim();
			}

			// Check for new-style properties...

			String token, token0;
			Vector v2;
			for ( ic = 0; ic < ntokens; ic++ ) {
				// Check for an '=' in the token...
				token = (String)v.elementAt(ic);
				if ( token.indexOf('=') < 0 ) {
					continue;
				}
				v2 = StringUtil.breakStringList (
					token, "=", 0 );
				if ( v2.size() < 2 ) {
					continue;
				}
				token0 = ((String)v2.elementAt(0)).trim();
				if ( token0.equalsIgnoreCase("Intercept") ) {
					intercept =
					((String)v2.elementAt(1)).trim();
				}
			}
			v = null;

			// Check for regression date override...

			if ( command.equalsIgnoreCase("fillRegression") ) {
				// Set the fill and analysis period if not
				// specified and the regression period is not
				// null...
				if (	(dep_analysis_period_start.equals("*")||
					dep_analysis_period_start.equals(""))&&
					_regression_date1 != null ) {
					dep_analysis_period_start =
					_regression_date1.toString();
				}
				if (	(dep_analysis_period_end.equals("*") ||
					dep_analysis_period_end.equals(""))&&
					_regression_date2 != null ) {
					dep_analysis_period_end =
					_regression_date2.toString();
				}
				if (	(fill_period_start.equals("*") ||
					fill_period_start.equals(""))&&
					_regression_date1 != null ) {
					fill_period_start =
					_regression_date1.toString();
				}
				if (	(fill_period_end.equals("*") ||
					fill_period_end.equals(""))&&
					_regression_date2 != null ) {
					fill_period_end =
					_regression_date2.toString();
				}
			}

			// Make sure there are time series available to
			// operate on...

			ts_pos = indexOf ( alias );
			TS firstTS = getTimeSeries ( ts_pos );
			if ( firstTS == null ) {
				Message.printWarning ( 1, routine,
				"Unable to find time series \"" + alias +
				"\" for " + command + "()." );
				++error_count;
				continue;
			}
			// The independent identifier may or may not have
			// TEMPTS at the front but is handled by getTimeSeries
			TS secondTS = getTimeSeries ( independent );
			if ( secondTS == null ) {
				Message.printWarning ( 1, routine,
				"Unable to find time series \"" + independent +
				"\" for " + command + "()." );
				++error_count;
				continue;
			}

			// Now set the fill properties...

			PropList props = new PropList ( "TS Analysis" );
			if (	num_equations.equalsIgnoreCase(
				"MonthlyEquations") ||
				num_equations.equalsIgnoreCase("OneEquation")) {
				props.set ( "NumberOfEquations", num_equations);
			}
			else {	Message.printWarning ( 1, routine,
				"Bad number of equations \"" + num_equations +
				"\" for \"" + expression + "\"" );
				++error_count;
				continue;
			}

			if (	transformation.equalsIgnoreCase( "Log") ||
				transformation.equalsIgnoreCase( "Linear") ||
				transformation.equalsIgnoreCase( "None") ){
				props.set ( "Transformation", transformation );
			}
			else {	Message.printWarning ( 1, routine,
				"Bad transformation type \"" + transformation +
				"\" for \"" + expression + "\"" );
				++error_count;
				continue;
			}

			// Check whether the MOVE2 algorithm should be used...

			if ( command.equalsIgnoreCase("fillMOVE1") ) {
				props.set ( "AnalysisMethod", "MOVE1" );
			}
			else if ( command.equalsIgnoreCase("fillMOVE2") ) {
				props.set ( "AnalysisMethod", "MOVE2" );
			}
			else {	// Ordinary least squares...
				props.set ( "AnalysisMethod", "OLSRegression" );
			}

			// Indicate that the analysis is being done for filling
			// (this property is used in the TSRegression class).
			// The default for TSRegression to compute RMSE to
			// compare time series (e.g., for calibration).  If
			// filling is indicated, then RMSE is computed from the
			// dependent and the estimated dependent...

			props.set ( "AnalyzeForFilling", "true" );

			// Set the analysis/fill periods...

			//if ( command.equalsIgnoreCase("fillMOVE2") ) {
				props.set ( "DependentAnalysisPeriodStart=" +
					dep_analysis_period_start );
				props.set ( "DependentAnalysisPeriodEnd=" +
					dep_analysis_period_end );
				props.set ( "IndependentAnalysisPeriodStart=" +
					ind_analysis_period_start );
				props.set ( "IndependentAnalysisPeriodEnd=" +
					ind_analysis_period_end );
			//}
			props.set ( "FillPeriodStart=" + fill_period_start );
			props.set ( "FillPeriodEnd=" + fill_period_end );
			if ( intercept != null ) {
				props.set ( "Intercept=" + intercept );
			}

			// Call the code that is used by both the old and new
			// version...

			ts = do_fillRegression (expression, firstTS,
						secondTS, props );
			ts_action = UPDATE_TS;
	// Figure out the dates to use for the analysis...
	DateTime fill_period_start = null;
	DateTime fill_period_end = null;
	String prop_val = props.getValue ( "FillPeriodStart" );
	if ( prop_val != null ) {
		try {	fill_period_start = getDateTime ( prop_val );
		}
		catch ( Exception e ) {
			fill_period_start = null;
		}
	}
	prop_val = props.getValue ( "FillPeriodEnd" );
	if ( prop_val != null ) {
		try {	fill_period_end = getDateTime ( prop_val );
		}
		catch ( Exception e ) {
			fill_period_end = null;
		}
	}
	// Fill the dependent time series...
	try {	TSRegression regress_results = TSUtil.fillRegress ( 
			ts_to_fill, ts_independent, fill_period_start,
			fill_period_end, props );
		// Print the results to the log file...
		if ( regress_results != null ) {
			Message.printStatus ( 1, routine,
			"Analysis results are..." );
			Message.printStatus ( 1, routine,
			regress_results.toString() );
		}
		else {	String message =
			"Null regression results for \"" + expression + "\"";
			Message.printWarning ( 2, routine, message );
			throw new Exception ( message );
		}
	}
	catch ( Exception e ) {
		String message =
		"Error performing regression for \"" + expression + "\"";
		Message.printWarning ( 2, routine, message );
		Message.printWarning ( 2, routine, e );
		throw new Exception ( message );
	}
	return ts_to_fill;
	return null;
}
*/

/**
Execute the shift() command.
@param command Command to parse.
@exception Exception if there is an error.
*/
private void do_shift ( String command )
throws Exception
{	String routine = "TSEngine.do_shift";
	Vector tokens = StringUtil.breakStringList ( command, " (,)", StringUtil.DELIM_SKIP_BLANKS );
	if ( tokens.size() != 4 ) {
		throw new Exception ( "Bad command \"" + command + "\"" );
	}
	// Parse the name and dates...
	String alias = (String)tokens.elementAt(1);
	DateTime olddate = (DateTime)__datetime_Hashtable.get( (String)tokens.elementAt(2));
	if ( olddate == null ) {
		Message.printStatus(1,routine, "Unable to look up date \"" +
		(String)tokens.elementAt(2) + "\"" );
		olddate = DateTime.parse ( (String)tokens.elementAt(2) );
	}
	DateTime newdate = (DateTime)__datetime_Hashtable.get((String)tokens.elementAt(3));
	if ( newdate == null ) {
		Message.printStatus(1,routine, "Unable to look up date \"" +
		(String)tokens.elementAt(3) + "\"" );
		newdate = DateTime.parse ( (String)tokens.elementAt(3) );
	}
	if ( Message.isDebugOn ) {
		Message.printDebug ( 1, routine, "Shifting TS \"" + alias +
		"\" from " + olddate.toString() + " to " + newdate.toString() );
	}
	int ts_pos = indexOf ( alias );
	if ( ts_pos >= 0 ) {
		TS ts = TSUtil.shift ( getTimeSeries(ts_pos), newdate, olddate ); //olddate, newdate );
		processTimeSeriesAction ( UPDATE_TS, ts, ts_pos );
	}
	else {
	    String message = "Unable to find time series \"" + alias + "\" for shift() command.";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
}

/**
Free memory for garbage collection.
@exception Throwable if there is an error.
*/
protected void finalize ()
throws Throwable
{	__AverageStart_DateTime = null;
	__AverageEnd_DateTime = null;
	__datetime_Hashtable = null;
	__OutputStart_DateTime = null;
	__OutputEnd_DateTime = null;
	__InputStart_DateTime = null;
	__InputEnd_DateTime = null;
	__hbdmi_Vector = null;
	__missing_ts = null;
	// TODO SAM 2007-02-18 Need to enable NDFD
	//__NDFDAdapter_Vector = null;
	__output_file = null;
	__reference_date = null;
	__tslist = null;
	super.finalize();
}

/**
Format comments for the header of output files.  The comments
include the commands file and database version information.
@param commands The commands that are being processed.
@param include_commands Indicate whether the commands should be included
in the comments.  IOUtil.printCreatorHeader will automatically include the
commands if a commands file has been set but is probably better to move
to a case where the commands are passed to the write methods.
*/
protected String [] formatOutputHeaderComments ( List commands )
{	Vector comments = new Vector();
	// Commands.  Show the file name but all commands may be in memory.
	comments.addElement ( "-----------------------------------------------------------------------" );
	String commands_filename = __ts_processor.getCommandsFileName();
	if ( commands_filename == null ) {
		comments.addElement ( "Command file name:  COMMANDS NOT SAVED TO FILE" );
	}
	else {
	    comments.addElement ( "Command file name: \"" + commands_filename + "\"" );
	}
	comments.addElement ( "Commands: " );
	int size_commands = commands.size();
	for ( int i = 0; i < size_commands; i++ ) {
		comments.add ( ((Command)commands.get(i)).toString() );
	}
	// Save information about data sources.
	HydroBaseDMI hbdmi = null;
	int hsize = __hbdmi_Vector.size();
	String db_comments[] = null;
	for ( int ih = 0; ih < hsize; ih++ ) {
		hbdmi = (HydroBaseDMI)__hbdmi_Vector.elementAt(ih);
		if ( hbdmi != null ) {
			try {
			    db_comments = hbdmi.getVersionComments ();
			}
			catch ( Exception e ) {
				db_comments = null;
			}
		}
		if ( db_comments != null ) {
			for ( int i = 0; i < db_comments.length; i++ ) {
				comments.addElement(db_comments[i]);
			}
		}
	}
	return StringUtil.toArray(comments);
}

/**
Indicate whether a time series' data period should automatically be extended
to the output period (to allow for filling).
@return True if the period should be automatically extended.
*/
protected boolean getAutoExtendPeriod()
{   return __AutoExtendPeriod_boolean;
}

/**
Return the average period end, or null if all available data are to be used.
@return the average period end, or null if all available data are to be used.
*/
protected DateTime getAverageEnd()
{   return __AverageEnd_DateTime;
}

/**
Return the average start, or null if all available data are to be used.
@return the average period start, or null if all available data are to be used.
*/
protected DateTime getAverageStart()
{   return __AverageStart_DateTime;
}

/**
Return the list of DataTest.
@return Vector of DataTest that can be used for data test commands.
*/
protected Vector getDataTestList ()
{   return __datatestlist;
}

/**
Get a date/time from a string.  This is done using the following rules:
<ol>
<li>	If the string is null, "*" or "", return null.</li>
<li>	If the string uses a standard name InputStart (QueryStart),
	InputEnd (QueryEnd), OutputStart, OutputEnd,
	return the corresponding DateTime.</li>
<li>	Check the date/time hash table for user-defined date/times.
<li>	Parse the string.
</ol>
@param date_string Date/time string to parse.
@exception if the date cannot be determined using the defined procedure.
*/
protected DateTime getDateTime ( String date_string )
throws Exception
{	if (	(date_string == null) ||
		date_string.equals("") || date_string.equals("*") ) {
		// Want to use all available...
		return null;
	}

	// Check for user DateTime instances...

	DateTime date = (DateTime)__datetime_Hashtable.get ( date_string );
	if ( date != null ) {
		// Found date in the hash table so use it...
		return date;
	}

	// Check for named DateTime instances...

	if (	date_string.equalsIgnoreCase("OutputEnd") ||
		date_string.equalsIgnoreCase("OutputPeriodEnd") ) {
		return __OutputEnd_DateTime;
	}
	else if(date_string.equalsIgnoreCase("OutputStart") ||
		date_string.equalsIgnoreCase("OutputPeriodStart") ) {
		return __OutputStart_DateTime;
	}
	else if(date_string.equalsIgnoreCase("InputEnd") ||
		date_string.equalsIgnoreCase("QueryEnd") ||
		date_string.equalsIgnoreCase("QueryPeriodEnd") ) {
		return __InputEnd_DateTime;
	}
	else if(date_string.equalsIgnoreCase("InputStart") ||
		date_string.equalsIgnoreCase("QueryStart") ||
		date_string.equalsIgnoreCase("QueryPeriodStart") ) {
		return __InputStart_DateTime;
	}

	// Else did not find a date time so try parse the string...

	return DateTime.parse ( date_string );
}

/**
Return the HydroBaseDMI that is being used.  Use a blank input name to get
the default.
@param input_name Input name for the DMI, can be blank.
@return the HydroBaseDMI that is being used (may return null).
*/
protected HydroBaseDMI getHydroBaseDMI ( String input_name )
{	int size = __hbdmi_Vector.size();
	if ( input_name == null ) {
		input_name = "";
	}
	HydroBaseDMI hbdmi = null;
	for ( int i = 0; i < size; i++ ) {
		hbdmi = (HydroBaseDMI)__hbdmi_Vector.elementAt(i);
		if ( hbdmi.getInputName().equalsIgnoreCase(input_name) ) {
			if ( Message.isDebugOn ) {
				Message.printDebug ( 1, "", "Returning HydroBaseDMI[" + i +"] InputName=\""+
				hbdmi.getInputName() + "\"" );
			}
			return hbdmi;
		}
	}
	return null;
}

/**
Return the list of HydroBaseDMI.
@return Vector of open HydroBaseDMI.
*/
protected Vector getHydroBaseDMIList ()
{	return __hbdmi_Vector;
}

/**
Indicate whether a values less than or equal zero should be excluded when computing historical averages.
@return True if values <= 0 should be excluded when computing historical averages.
*/
protected boolean getIgnoreLEZero()
{   return __IgnoreLEZero_boolean;
}

/**
Indicate whether missing time series should result in an empty time series (rather
than a warning and no time series).
@return True if blank time series should be generated when they cannot be read.
*/
protected boolean getIncludeMissingTS()
{   return __IncludeMissingTS_boolean;
}

/**
Return the input period end, or null if all available data are to be queried.
@return the input period end, or null if all available data are to be queried.
*/
protected DateTime getInputEnd()
{	return __InputEnd_DateTime;
}

/**
Return the input start, or null if all available data are to be read at input.
@return the input period start, or null if all available data are to be read.
*/
protected DateTime getInputStart()
{	return __InputStart_DateTime;
}

/**
Return the list of TSID strings for which time series could not be read.
@return the list of TSID strings for which time series could not be read.
*/
protected Vector getMissingTS()
{   return __missing_ts;
}

// TODO SAM 2006-07-13
// May need to fully qualify Adapter but for now there is no conflict with
// other packages.
/**
Return the NDFD Adapter that is being used.
@param input_name Input name for the adapter, can be blank.
@return the NDFD Adapter that is being used (may return null).
*/

// TODO KAT 2006-10-30
// Commenting this out for now to get it to compile
// Danny will be adding service stuff in the future
// which should replace this ...
/**
public Adapter getNDFDAdapter (String input_name )
{	int size = __NDFDAdapter_Vector.size();
	if ( input_name == null ) {
		input_name = "";
	}
	Adapter adapter = null;
	for ( int i = 0; i < size; i++ ) {
		adapter = (Adapter)__NDFDAdapter_Vector.elementAt(i);
		/* TODO SAM 2006-07-15
		Currently the adapters don't have and ID or name so for
		now return the first instance.
		if ( adapter.getInputName().equalsIgnoreCase(input_name) ) {
			if ( Message.isDebugOn ) {
				Message.printDebug ( 1, "",
				"Returning NDFD Adapter[" + i +"] InputName=\""+
				adapter.getInputName() + "\"" );
			}
			return adapter;
		}
		
		return adapter;
	}
	return null;
}
*/

/**
Return the NWSRFS_DMI that is being used.  Use a blank input name to get the default.
@param input_name Input name for the DMI, can be blank.  Typically is the path to the FS5Files.
@return the NWSRFS_DMI that is being used (may return null).
*/
protected NWSRFS_DMI getNWSRFSFS5FilesDMI ( String input_name, boolean open_if_not_found )
{	String routine = getClass().getName() + ".getNWSRFSFS5FilesDMI";
	int size = __nwsrfs_dmi_Vector.size();
	if ( input_name == null ) {
		input_name = "";
	}
	NWSRFS_DMI nwsrfs_dmi = null;
	for ( int i = 0; i < size; i++ ) {
		nwsrfs_dmi = (NWSRFS_DMI)__nwsrfs_dmi_Vector.elementAt(i);
		if ( nwsrfs_dmi.getInputName().equalsIgnoreCase(input_name) ) {
			if ( Message.isDebugOn ) {
				Message.printDebug ( 1, routine,
				"Returning NWSRFS_DMI[" + i +"] InputName=\""+ nwsrfs_dmi.getInputName() + "\"" );
			}
			return nwsrfs_dmi;
		}
	}
	if ( Message.isDebugOn ) {
		Message.printDebug ( 1, routine,
		"Could not find a matching NWSRFS FS5Files DMI for InputName=\""+ input_name + "\"" );
	}
	if ( open_if_not_found ) {
			try {	Message.printStatus( 2, routine,
					"Opening new NWSRFS FS5Files DMI using path \"" + input_name + "\"" );
					nwsrfs_dmi = new NWSRFS_DMI( input_name );
					nwsrfs_dmi.open();
					// Save so we can get to it again when we need it...
					setNWSRFSFS5FilesDMI ( nwsrfs_dmi, true );
					return nwsrfs_dmi;
			}
			catch ( Exception e ) {
				Message.printWarning ( 3, routine, "Could not open NWSRFS FS5Files." );
				Message.printWarning(3, routine, e);
			}
	}
	return null;
}

/**
Return the output period end, or null if all data are to be output.
@return the output period end, or null if all data are to be output.
*/
protected DateTime getOutputEnd()
{	return __OutputEnd_DateTime;
}

/**
Return the output period start, or null if all data are to be output.
@return the output period start, or null if all data are to be output.
*/
protected DateTime getOutputStart()
{	return __OutputStart_DateTime;
}

/**
Return the output year type, to be used for commands that create output.
@return the output year type, as "Calendar" or "Water".
*/
protected String getOutputYearType()
{	int OutputYearType_int = getOutputYearTypeInt();
	if ( OutputYearType_int == _CALENDAR_YEAR ) {
		return "Calendar";
	}
	else if ( OutputYearType_int == _WATER_YEAR ) {
		return "Water";
	}
	else {
		return "";
	}
}

/**
Return the output year type as an internal integer, to be used for commands that create output.
@return the output year type, as "Calendar" or "Water".
*/
private int getOutputYearTypeInt()
{   
    return __OutputYearType_int;
}

/**
Indicate whether output that is exported should be previewed first.
@return True if output that is exported should be previewed first..
*/
protected boolean getPreviewExportedOutput()
{   return __PreviewExportedOutput_boolean;
}

/**
Return the RiversideDB_DMI that is being used.  Use a blank input name to get the default.
@param input_name Input name for the DMI, can be blank.
@return the RiversideDB_DMI that is being used (may return null).
*/
protected RiversideDB_DMI getRiversideDB_DMI ( String input_name )
{   String routine = "TSEngine.getRiversideDB_DMI";
    Message.printStatus(2, routine, "Getting RiversideDB_DMI connection \"" + input_name + "\"" );
    int size = __rdmi_Vector.size();
    if ( input_name == null ) {
        input_name = "";
    }
    RiversideDB_DMI rdmi = null;
    for ( int i = 0; i < size; i++ ) {
        rdmi = (RiversideDB_DMI)__rdmi_Vector.elementAt(i);
        if ( rdmi.getInputName().equalsIgnoreCase(input_name) ) {
            if ( Message.isDebugOn ) {
                Message.printDebug ( 1, "", "Returning RiversideDB_DMI[" + i +"] InputName=\""+
                rdmi.getInputName() + "\"" );
            }
            return rdmi;
        }
    }
    return null;
}

/**
Return a time series from the __tslist vector.
The search is performed backwards in the
list, assuming that the commands are being processed sequentially and therefore
any reference to a duplicate ID would intuitively be referring to the latest
instance in the list.  For this version of the method, the trace (sequence number) is ignored.
@param id Time series identifier (either an alias or TSIdent string).
@return a time series from the requested position or null if none is available.
@exception Exception if there is an error getting the time series.
*/
protected TS getTimeSeries ( String command_tag, String id )
throws Exception
{	return getTimeSeries ( command_tag, id, -1 );
}

/**
Return a time series from either the __tslist vector.  The search is performed backwards in the
list, assuming that the commands are being processed sequentially and therefore
any reference to a duplicate ID would intuitively be referring to the latest
instance in the list.
@param id Time series identifier (either an alias or TSIdent string).
@param sequence_number If >= 0, the sequence number of the time series is
also checked to make a match.
@return a time series from the requested position or null if none is available.
@exception Exception if there is an error getting the time series.
*/
protected TS getTimeSeries ( String command_tag, String id, int sequence_number )
throws Exception
{	String tsident = id.trim();
	if ( tsident.regionMatches(true,0,TEMPTS,0,6) ) {
		// Temporary time series.  Read it, mark as temporary, and return...
		Message.printStatus ( 1, "TSEngine.getTimeSeries", "Reading temporary time series \"" +	tsident.substring(6).trim() );
		TS ts = readTimeSeries ( 2, command_tag, tsident.substring(6).trim(), true );
		if ( ts != null ) {
			ts.setStatus ( TEMPTS );
		}
		return ts;
	}
	else {
        // Expect the time series to be in memory or BinaryTS file...
		int pos = indexOf ( id, sequence_number );
		if ( pos < 0 ) {
			return null;
		}
		return getTimeSeries ( pos );
	}
}

/**
Return a time series from either the __tslist vector or the BinaryTS file,
as appropriate.  If a BinaryTS is returned, it is a new instance from the file
and should be set to null when done.
@param position Position in time series list (0 index).
@return a time series from the requested position or null if none is available.
@exception Exception if there is an error reading the time series.
*/
protected TS getTimeSeries ( int position )
throws Exception
{	if ( position < 0 ) {
		return null;
	}
    if ( __tslist == null ) {
		return null;
	}
	else if ( position > __tslist.size() ) {
		return null;
	}
    // Else return the requested time series.
	return (TS)__tslist.elementAt(position);
}

/**
Return the list of time series.  See also getSelectedTimeSeriesList().
@return the list of time series.  The list can be null or zero size.
@param indices a list of indices to return or null to return all.  Only indices
within the time series list size will be returned.
*/
protected Vector getTimeSeriesList ( int [] indices )
{	if ( indices == null ){
		return __tslist;
	}
	else {	// Return only the requested indices...
		Vector v = new Vector ();
		int size = 0;
		if ( __tslist != null ) {
			size = __tslist.size();
		}
		for ( int i = 0; i < indices.length; i++ ) {
			if ( indices[i] < size ) {
				v.addElement ( __tslist.elementAt(indices[i]) );
			}
		}
		return v;
	}
}

/**
Return number of time series that have been processed and are available for
output.
@return number of time series available for output.
*/
protected int getTimeSeriesSize ()
{	return __tslist.size();
}

/**
Return the list of time series to process, based on information that indicates
how the list can be determined.
@param TSList Indicates how the list of time series for processing is to be
determined, with one of the following values (see TSListType):
<ol>
<li>    "AllMatchingTSID" will use the TSID value to match time series.</li>
<li>	"AllTS" will result in true being returned.</li>
<li>    "EnsembleID" will return the list of time series associated with an ensemble.</li>
<li>    "FirstMatchingTSID" will use the TSID value to match time series,
        returning the first match.</li>
<li>	"LastMatchingTSID" will use the TSID value to match time series,
	    returning the last match.  This is necessary for backward compatibility.</li>
<li>	"SelectedTS" will return a list of time series that are selected.</li>
<li>    "SpecifiedTSID" will return a list of time series that match the specified identifiers
        (no wildcards, just a comma-separated list of identifier).</li>
<li>    "TSPosition" will return a list of time series matching the positions in the TSPosition
        value, which has the syntax 1-2,4,5-7 (specify single values or range, values are 1+) -
        internally the values are zero indexed.  In the future Python notation slices may
        be enabled.</li>
</ol>
@param TSID A time series indentifier (pattern) when used with TSList=AllMatchingTSID and
TSList=LastMatchingTSID, or a list of time series separated by commas when used with
TSList=SpecifiedTSID.
@param EnsembleID A time series ensemble identifier (no pattern currently allowed).
@return A Vector that has as its first element a Vector of TS to process and as
its second element an int[] indicating the positions in the time series list,
to be used to update the time series.  Use the size of the Vector (in the first
element) to determine the number of time series to process.  The order of the
time series will be from first to last.  A non-null list is guaranteed to be returned.
*/
protected Vector getTimeSeriesToProcess ( String TSList, String TSID, String EnsembleID, String TSPosition )
{	String routine = "TSEngine.getTimeSeriesToProcess";
    Vector tslist = new Vector();	// List of time series to process
	int nts = getTimeSeriesSize(); // OK to be zero for logic below - zero size array will result.
	int [] tspos = new int[nts];	// Positions of time series to process.
					// Size to match the full list but may
					// not be filled initially - trim before returning.
	// Setup the return data...
	Vector v = new Vector ( 2 );
	v.addElement ( tslist );
	v.addElement ( tspos );
	// Loop through the time series in memory...
	int count = 0;
	TS ts = null;
	Message.printStatus( 2, "", "TSList=\"" + TSList + "\" TSID=\"" + TSID +
            "\", EnsembleID=\"" + EnsembleID + "\", TSPosition=\"" + TSPosition + "\"" );
   if ( TSList.equalsIgnoreCase(TSListType.FIRST_MATCHING_TSID.toString()) ) {
        // Search forwards for the first single matching time series...
        for ( int its = 0; its < nts; its++ ) {
            try {
                ts = getTimeSeries ( its );
            }
            catch ( Exception e ) {
                // Don't add...
                continue;
            }
            if ( TSID.indexOf("~") > 0 ) {
                // Include the input type...
                if (ts.getIdentifier().matches(TSID,true,true)){
                    tslist.addElement ( ts );
                    tspos[count++] = its;
                    // Only return the single index...
                    int [] tspos2 = new int[1];
                    tspos2[0] = tspos[0];
                    v.setElementAt(tspos2,1);
                    // Only want one match...
                    return v;
                }
            }
            else {
                // Just check the main information...
                if(ts.getIdentifier().matches(TSID,true,false)){
                    tslist.addElement ( ts );
                    tspos[count++] = its;
                    // Only return the single index...
                    int [] tspos2 = new int[1];
                    tspos2[0] = tspos[0];
                    v.setElementAt(tspos2,1);
                    // Only want one match...
                    return v;
                }
            }
        }
    }
	else if ( TSList.equalsIgnoreCase(TSListType.LAST_MATCHING_TSID.toString()) ) {
		// Search backwards for the last single matching time series...
		for ( int its = (nts - 1); its >= 0; its-- ) {
			try {
                ts = getTimeSeries ( its );
			}
			catch ( Exception e ) {
				// Don't add...
				continue;
			}
			if ( TSID.indexOf("~") > 0 ) {
				// Include the input type...
				if (ts.getIdentifier().matches(TSID,true,true)){
					tslist.addElement ( ts );
					tspos[count++] = its;
					// Only return the single index...
					int [] tspos2 = new int[1];
					tspos2[0] = tspos[0];
					v.setElementAt(tspos2,1);
					// Only want one match...
					return v;
				}
			}
			else {
                // Just check the main information...
				if(ts.getIdentifier().matches(TSID,true,false)){
					tslist.addElement ( ts );
					tspos[count++] = its;
					// Only return the single index...
					int [] tspos2 = new int[1];
					tspos2[0] = tspos[0];
					v.setElementAt(tspos2,1);
					// Only want one match...
					return v;
				}
			}
		}
	}
    else if ( TSList.equalsIgnoreCase(TSListType.ENSEMBLE_ID.toString()) ) {
        // Return a list of all time series in an ensemble.
        TSEnsemble ensemble = __ts_processor.getEnsemble ( EnsembleID );
        if ( ensemble == null ) {
            Message.printStatus( 3, routine, "Unable to find ensemble \"" + EnsembleID + "\" to get time series.");
            return v;
        }
        else {
            int esize = ensemble.size();
            for ( int ie = 0; ie < esize; ie++ ) {
                // Set the time series instance (always what is included in the ensemble)...
                ts = ensemble.get (ie);
                tslist.addElement ( ts );
                // Figure out the index in the processor time series list by comparing the instance...
                TS ts2; // Time series in main list to compare against.
                boolean found = false;
                // Loop through the main list...
                for ( int its = 0; its < nts; its++ ) {
                    try {
                        ts2 = getTimeSeries ( its );
                    }
                    catch ( Exception e ) {
                        continue;
                    }
                    if ( ts == ts2 ) {
                        found = true;
                        tspos[count++] = its;
                        break;
                    }
                }
                if ( !found ) {
                    Message.printStatus( 3, routine, "Unable to find ensemble \"" + EnsembleID + "\" time series \"" +
                            ts.getIdentifier() + "\" - setting index to -1.");
                    tspos[count++] = -1;    // Should never happen - will trigger problem that need to fix other code
                }
            }
        }
        // Trim down the "tspos" array to only include matches so that other
        // code does not mistakenly iterate through a longer array...
        if ( count == 0 ) {
            v.setElementAt ( new int[0], 1 );
        }
        else {
            int [] tspos2 = new int[count];
            for ( int i = 0; i < count; i++ ) {
                tspos2[i] = tspos[i];
            }
            v.setElementAt ( tspos2, 1 );
        }
        return v;
    }
    else if ( TSList.equalsIgnoreCase(TSListType.SPECIFIED_TSID.toString()) ) {
        // Return a list of time series that match the provided identifiers.
        Vector tsid_Vector = StringUtil.breakStringList ( TSID, ",", StringUtil.DELIM_SKIP_BLANKS );
        int size_tsid = 0;
        if ( tsid_Vector != null ) {
            size_tsid = tsid_Vector.size();
        }
        for ( int itsid = 0; itsid < size_tsid; itsid++ ) {
            String tsid = (String)tsid_Vector.elementAt(itsid);
            Message.printStatus( 2, routine, "Trying to match \"" + tsid + "\"" );
            // Loop through the available time series and see if any match..
            boolean found = false;
            for ( int its = 0; its < nts; its++ ) {
                try {
                    ts = getTimeSeries ( its );
                }
                catch ( Exception e ) {
                    // Don't add...
                    continue;
                }
                // Compare the requested TSID with that in the time series list...
                if ( tsid.indexOf("~") > 0 ) {
                    // Include the input type...
                    if (ts.getIdentifier().matches(tsid,true,true)){
                        //Message.printStatus( 2, routine,
                        //        "Matched using input with TSID=\"" + ts.getIdentifier() + "\"" +
                        //        " Alias=\"" + ts.getAlias() + "\"");
                        found = true;
                    }
                }
                else {
                    // Just check the main information...
                    if(ts.getIdentifier().matches(tsid,true,false)){
                        //Message.printStatus( 2, routine,
                        //        "Matched not using input with TSID=\"" + ts.getIdentifier() + "\"" +
                        //        " Alias=\"" + ts.getAlias() + "\"");
                        found = true;
                    }
                }
                if ( found ) {
                    // Add the time series and increment the count...
                    tslist.addElement ( ts );
                    tspos[count++] = its;
                    // Found the specific time series so break out of the list.
                    // FIXME SAM 2008-02-05 What if user has the same ID more than once?
                    break;
                }
            }
        }
        // Trim down the "tspos" array to only include matches so that other
        // code does not mistakenly iterate through a longer array...
        Message.printStatus( 2, routine, "Matched " + count + " time series." );
        if ( count == 0 ) {
            v.setElementAt ( new int[0], 1 );
        }
        else {
            int [] tspos2 = new int[count];
            for ( int i = 0; i < count; i++ ) {
                tspos2[i] = tspos[i];
            }
            v.setElementAt ( tspos2, 1 );
        }
        return v;
    }
    else if ( TSList.equalsIgnoreCase(TSListType.TSPOSITION.toString()) ) {
        // Process the position string
        Vector tokens = StringUtil.breakStringList ( TSPosition,",", StringUtil.DELIM_SKIP_BLANKS );
        int npos = 0;
        if ( tokens != null ) {
            npos = tokens.size();
        }
        int tsposStart, tsposEnd;
        for ( int i = 0; i < npos; i++ ) {
            String token = (String)tokens.elementAt(i);
            if ( token.indexOf("-") >= 0 ) {
                // Range...
                String posString = StringUtil.getToken(token, "-",0,0).trim();
                tsposStart = Integer.parseInt( posString ) - 1;
                posString = StringUtil.getToken(token, "-",0,1).trim();
                tsposEnd = Integer.parseInt( posString ) - 1;
            }
            else {
                // Single value.  Treat as a range of 1.
                tsposStart = Integer.parseInt(token) - 1;
                tsposEnd = tsposStart;
            }
            for ( int itspos = tsposStart; itspos <= tsposEnd; itspos++ ) {
                try {
                    tslist.addElement ( getTimeSeries(itspos) );
                }
                catch ( Exception e ) {
                    // Don't add
                    // FIXME SAM 2008-07-07 Evaluate whether exception needs to be thrown
                    // for out of range index.
                }
                tspos[count++] = itspos;
            }
        }
        // Trim down the "tspos" array to only include matches so that other
        // code does not mistakenly iterate through a longer array...
        if ( count == 0 ) {
            v.setElementAt ( new int[0], 1 );
        }
        else {
            int [] tspos2 = new int[count];
            for ( int i = 0; i < count; i++ ) {
                tspos2[i] = tspos[i];
            }
            v.setElementAt ( tspos2, 1 );
        }
        return v;
    }
	// Else loop through all the time series from first to last and find matches...
	boolean found = false;
	for ( int its = 0; its < nts; its++ ) {
		found = false;
		try {
            ts = getTimeSeries ( its );
		}
		catch ( Exception e ) {
			// Don't add...
			continue;
		}
		if ( TSList.equalsIgnoreCase(TSListType.ALL_TS.toString()) ) {
			found = true;
		}
		else if( TSList.equalsIgnoreCase(TSListType.SELECTED_TS.toString()) && ts.isSelected() ) {
			found = true;
		}
		else if ( TSList.equalsIgnoreCase(TSListType.ALL_MATCHING_TSID.toString()) ) {
			if ( TSID.indexOf("~") > 0 ) {
				// Include the input type...
				if (ts.getIdentifier().matches(TSID,true,true)){
					found = true;
				}
			}
			else {
                // Just check the main information...
				if(ts.getIdentifier().matches(TSID,true,false)){
					found = true;
				}
			}
		}
		if ( found ) {
			// Add the time series and increment the count...
			tslist.addElement ( ts );
			tspos[count++] = its;
		}
	}
	// Trim down the "tspos" array to only include matches so that other
	// code does not mistakenly iterate through a longer array...
	if ( count == 0 ) {
		v.setElementAt ( new int[0], 1 );
	}
	else {
		int [] tspos2 = new int[count];
		for ( int i = 0; i < count; i++ ) {
			tspos2[i] = tspos[i];
		}
		v.setElementAt ( tspos2, 1 );
	}
	//Message.printStatus( 2, routine, tslist.toString() );
	return v;
}

/**
Get a list of time series identifiers for traces from a list of commands.
See documentation for fully loaded method.  The list is not sorted
@param commands Time series commands to search.
@return list of time series identifiers or an empty non-null Vector if nothing
found.
*/
protected static Vector getTraceIdentifiersFromCommands ( Vector commands )
{	return getTraceIdentifiersFromCommands ( commands, false );
}

/**
Get a list of time series identifiers for traces from a list of commands.
Time series identifiers from createTraces() commands are returned.
These strings are suitable for drop-down lists, etc.
@param commands Time series commands to search.
@param sort Should output be sorted by identifier.
@return list of time series identifiers or an empty non-null Vector if nothing
found.
*/
protected static Vector getTraceIdentifiersFromCommands ( Vector commands, boolean sort )
{	if ( commands == null ) {
		return new Vector();
	}
	Vector v = new Vector ( 10, 10 );
	int size = commands.size();
	String command = null, string = null;
	Vector tokens = null;
	for ( int i = 0; i < size; i++ ) {
		command = ((String)commands.elementAt(i)).trim();
		if (	(command == null) ||
			command.startsWith("#") ||
			(command.length() == 0) ) {
			// Make sure comments are ignored...
			continue;
		}
		tokens = StringUtil.breakStringList(
			command," =(),",
			StringUtil.DELIM_SKIP_BLANKS );
		string = ((String)tokens.elementAt(0)).trim();
		if ( !string.regionMatches ( true,0,"createTraces",0,12) ) {
			// Not a command we are looking for...
			continue;
		}
		string = ((String)tokens.elementAt(1)).trim();
		if ( string.regionMatches ( true,0,"tempts",0,6) ) {
			// Assume the next item is the identifier...
			v.addElement ( ((String)tokens.elementAt(2)).trim() );
		}
		else {	// Assume this item is the identifier...
			v.addElement ( ((String)tokens.elementAt(1)).trim() );
		}
	}
	tokens = null;
	return v;
}

/**
Return a Vector of objects (currently open DMI instances) that implement
TSProductAnnotationProvider. This is a helper method for other methods.
@return a non-null Vector of TSProductAnnotationProviders.
*/
protected Vector getTSProductAnnotationProviders ()
{	Vector ap_Vector = new Vector();
	// Check the HydroBase instances...
	int hsize = __hbdmi_Vector.size();
	HydroBaseDMI hbdmi = null;
	for ( int ih = 0; ih < hsize; ih++ ) {
		hbdmi = (HydroBaseDMI)__hbdmi_Vector.elementAt(ih);
		if ( (hbdmi != null) &&	(hbdmi instanceof TSProductAnnotationProvider)) {
			ap_Vector.addElement ( hbdmi );
		}
	}
	// Check the ColoradoSMS instances...
	if ( (__smsdmi != null) && (__smsdmi instanceof TSProductAnnotationProvider) ) {
		ap_Vector.addElement ( __smsdmi );
	}
	// Check the RiversideDB_DMI instances...
    int rsize = __rdmi_Vector.size();
    RiversideDB_DMI rdmi = null;
    for ( int ir = 0; ir < rsize; ir++ ) {
        rdmi = (RiversideDB_DMI)__rdmi_Vector.elementAt(ir);
        if ( (rdmi != null) && (rdmi instanceof TSProductAnnotationProvider)) {
            ap_Vector.addElement ( rdmi );
        }
    }

	return ap_Vector;
}

/**
Return the TSSupplier name.
@return the TSSupplier name ("TSEngine").
*/
public String getTSSupplierName()
{	return "TSEngine";
}

/**
Return the WindowListener that wants to track TSView windows.
This is used when the TSCommandProcessor is run as a supporting
tool (e.g., in TSTool with no main GUI).
@return the WindowListener for TSView windows.
*/
protected WindowListener getTSViewWindowListener ()
{	return _tsview_window_listener;
}

/**
Determine whether the averaging period is set (start and end dates need to be
non-null and years not equal to zero).
@return true if the averaging period has been specified (and we can use its
dates without fear of nulls or zero years).
*/
private boolean haveAveragingPeriod ()
{	if ( (getAverageStart() == null) || (getAverageEnd() == null) ) {
		return false;
	}
	if ( (getAverageStart().getYear() == 0) || (getAverageEnd().getYear() == 0) ) {
		return false;
	}
	return true;
}

/**
Indicate whether the input period has been specified.
@return true if the input period has been specified (and we can use its
dates without fear of nulls or zero years).
*/
private boolean haveInputPeriod ()
{	if ( (__InputStart_DateTime == null) ||
		(__InputEnd_DateTime == null) ) {
		return false;
	}
	if ( (__InputStart_DateTime.getYear() == 0) ||
			(__InputEnd_DateTime.getYear() == 0) ){
		return false;
	}
	return true;
}

/**
Indicate whether the output period has been specified.
@return true if the output period has been specified (and we can use its
dates without fear of nulls or zero years).
*/
protected boolean haveOutputPeriod ()
{	if ((__OutputStart_DateTime == null) || (__OutputEnd_DateTime == null)){
		return false;
	}
	if (	(__OutputStart_DateTime.getYear() == 0) ||
		(__OutputEnd_DateTime.getYear() == 0) ) {
		return false;
	}
	return true;
}

/**
Return the position of a time series from either the __tslist vector.
See the overloaded method for full documentation.  This version assumes that no sequence number is used.
@param string the alias and/or time series identifier to look for.
@return Position in time series list (0 index), or -1 if not in the list.
*/
protected int indexOf ( String string )
{	return indexOf ( string, -1 );
}

/**
Return the position of a time series from the __tslist vector.  The search is done as follows:
<ol>
<li>	If string matches a TS alias matches, return the TS index.  This is
	the most specific match where an alias is being specified in the search string.</li>
<li>	Else, if the string does match a series identifier (using
	TSIdent.equals()), return the index.  This is
	a more general search where the string is an TS identifier and therefore
	safely does NOT match an alias.</li>
<li>	Return -1.</li>
</ol>
The search is performed backwards in the list, assuming that the commands are
being processed sequentially and therefore any reference to a duplicate ID would
intuitively be referring to the latest instance in the list.
@param string the alias and/or time series identifier to look for.
@param sequence_number If specified as >= 0, the sequence number is also
checked to find a match.
@return Position in time series list (0 index), or -1 if not in the list.
*/
private int indexOf ( String string, int sequence_number )
{	// First search the aliases in the BinaryTS and in memory list...
	int pos = -1;
	if ( (string == null) || string.equals("") ) {
		return -1;
	}
	if ( sequence_number >= 0 ) {
	    pos = TSUtil.indexOf ( __tslist, string, "Alias", sequence_number, -1 );
	}
	else {
        pos = TSUtil.indexOf ( __tslist, string, "Alias", -1 );
	}
	if ( pos >= 0 ) {
		return pos;
	}
	// Now search the identifiers (can't totally rely on indexOf() because the alias must also be empty)...
	int size = 0;
	if ( __tslist == null ) {
		return -1;
	}
	else {
        TS ts = null;
		size = __tslist.size();
		for ( int i = (size - 1); i >= 0; i-- ) {
			ts = (TS)__tslist.elementAt(i);
			if ( ts == null ) {
				continue;
			}
			if ( ts.getIdentifier().equals(string) ) {//&& ts.getAlias().equals("") ) {}
				if ( sequence_number >= 0 ) {
					if ( ts.getSequenceNumber() == sequence_number ) {
						return i;
					}
				}
				else {
                    return i;
				}
			}		
		}
	}
	return -1;
}

/**
Determine whether a command parameter is valid.  This is used for common/shared parameters.
@param parameter Parameter name.
@param value Parameter value to check.
@return true if a parameter value is valid.
*/
public boolean isParameterValid ( String parameter, String value )
{	if ( parameter.equalsIgnoreCase("TSList") ) {
		if ( value.equalsIgnoreCase(TSListType.ALL_TS.toString()) ||
                value.equalsIgnoreCase(TSListType.ALL_MATCHING_TSID.toString()) ||
			value.equalsIgnoreCase(TSListType.LAST_MATCHING_TSID.toString()) ||
            value.equalsIgnoreCase(TSListType.SELECTED_TS.toString()) ) {
			return true;
		}
		else {
            return false;
		}
	}
	return false;
}

/**
Process the events from the MessageJDialog class.  If the "Cancel" button has
been pressed, then indicate that the time series processing should stop.
@param command If "Cancel", then a request will be made to cancel processing.
*/
public void messageJDialogAction ( String command )
{	if ( command.equalsIgnoreCase("Cancel") ) {
		__ts_processor.setCancelProcessingRequested(true);
	}
}

/**
Read an NWSRFS time series.  This code needs to be moved into the
RTi.DMI.NWSRFSDMI when it takes form.
The time series is generated by running the nwsrfssh script.  The nwsrfssh is
run via batch mode using the 'tsdata' command.  A sample command: <BR><I>
nwsrfssh batch "open ofs; tsdata PEQUENI QINE CMS; quit" </I>  
@return a time series matching the identifier.
@param tsident_string Time series identifier string.
@param req_date1 Requested start date for data.
@param req_date2 Requested end date for data.
@param req_units Requested data units.
@param read_data Indicates whether data should be read (true) or only header
information (false).
@exception Exception if there is an error reading the time series.
*/
public static TS NWSRFSDMI_readTimeSeries (	String tsident_string,
						DateTime req_date1,
						DateTime req_date2,
						String req_units,
						boolean read_data )
throws Exception
{	String routine = "NWSRFSDMI.readTimeSeries";
	int dl = 30;

	// Create DateValueTS data by running nwsrfssh.  Store the output in
	// a vector...

	if ( req_units == null ) {
		req_units = "";
	}
	TSIdent tsident = new TSIdent ( tsident_string );
	String shell_cmd = "nwsrfssh batch 'open ofs;tsdata " +
		tsident.getLocation() + " " + tsident.getType() + " " +
		req_units + ";quit'";
	tsident = null;

	if (Message.isDebugOn) {
		Message.printDebug(dl, routine, "nwsrfssh batch command: " +
		shell_cmd );
	}

	Vector dateValue_list = new Vector();
	try {	int exitstat = -999;
		ProcessManager pm = new ProcessManager(shell_cmd, 12000 );
		pm.saveOutput(true);
		pm.run();
		dateValue_list = pm.getOutputVector();
		exitstat = pm.getExitStatus();
		if (( exitstat != 0 ) || (dateValue_list.size()== 0))  {
			String message = "The \"" + shell_cmd +
				" command failed.";
			Message.printWarning(2, routine, message );
			throw new IOException ( message );
		}
		else  {	String firstline = null;
			firstline = (String)dateValue_list.firstElement();
			if (	(firstline == null) ||
				(firstline.indexOf("ERROR") != -1 ) ||
				(firstline.indexOf("WARN") != -1 ))  {
				String message = "The \"" + shell_cmd +
					" command failed.";
				Message.printWarning(2, routine, message );
				dateValue_list = null;
				throw new IOException ( message );
			}
		}
	}
	catch (Exception e ) {
		String message = "The \"" + shell_cmd + " command failed.";
		Message.printWarning(2, routine, message );
		dateValue_list = null;
		throw new IOException ( message );
	}
			
	// If the vector is still not null, remove that "Stop x" line at end of
	// the vector.

	if ( dateValue_list.size() > 0 ) {
		dateValue_list.removeElementAt(dateValue_list.size() - 1);
	}

	// Create a DateValue time series using the vector...

	TS ts = DateValueTS.readFromStringList ( dateValue_list, tsident_string,
						req_date1, req_date2, req_units,
						read_data );
	dateValue_list = null;
	// SAMX - should be done now...
	// Hard-code this for now - need to update the nwssrfssh to put NWSRFS
	// for the source...
	//if ( ts != null ) {
	//	ts.setIdentifier ( tsident_string );
	//}
	return ts;
}

//TODO SAM 2006-05-02
//Need to phase out app_PropList or make the exchange of control information more robust
/**
Process a list of time series commands, resulting in a vector of time series
(and/or setting data members and properties in memory).  The resulting time series are
saved in memory and can be output using the processTimeSeries() method).
<b>Filling with historic averages is handled for monthly time series
so that original data averages are used.</b>
@param commandList The Vector of Command from the TSCommandProcessor,
to be processed.  If null, process all.  Non-null is typically only used, for example,
if a user has selected commands in a GUI.
@param app_PropList if not null, then properties are set as the commands are
run.  This is typically used when running commands prior to using an edit
dialog in the TSTool GUI, as follows:
<table width=100% cellpadding=10 cellspacing=0 border=2>
<tr>
<td><b>Property</b></td>	<td><b>Description</b></td>
</tr>

<tr>
<td><b>CreateOutput</b></td>
<td>Indicate whether output files should be created.  False is faster but
results in incomplete products.
</td>
<td>True - create output files.</td>
</tr>

<tr>
<td><b>Recursive</b></td>
<td>If set to true, indicates that the commands string list being processed is
from a recursive call (e.g., when processing runCommands()).  Consequently, the
second commands list is processed, not the original one that started
processing.
</td>
<td>False</td>
</tr>

<tr>
<td><b>WorkingDir</b></td>
<td>Will be set if a setWorkingDir() command is encountered.
</td>
<td>Working directory will not be set.</td>
</tr>

</table>
*/
protected void processCommands ( List commandList,	PropList app_PropList )
throws Exception
{	String	message, routine = "TSEngine.processTimeSeriesCommands";
	String message_tag = "ProcessCommands"; // Tag used with messages generated in this method.
	int error_count = 0;	// For errors during time series retrieval
	int update_count = 0;	// For warnings about command updates
	if ( commandList == null ) {
		// Process all commands if a subset has not been provided.
		commandList = __ts_processor.getCommands();
	}
	
	// Save the passed in properties (formed in the TSCommandProcessor) request
	// call, so that they can be retrieved with other requests.
	
	if ( app_PropList == null ) {
		app_PropList = new PropList ( "TSEngine" );
	}
	// Save class version...
	__processor_PropList = app_PropList;

	// Initialize the working directory to the initial directory that is
	// passed in.  Do this because software may request the working directory that
	// is the result of processing and the initial directory may never have
	// been changed dynamically.
	
	// FIXME SAM 2008-07-09 Need to reset global properties to defaults before running
	// This includes output period, etc.  Otherwise, the settings will be those of the
	// previous run.  Probably need a parameter to control (do it by default) so that
	// when running RunCommands() it is possible to retain previously set values or clear.
	// This is done in the TSCommandProcessor instance before calling this method.
	
	/* TODO SAM 2007-10-13 Remove when test out.  The initial working dir is no
	 * longer dynamic but is a data member on the processor.
	String InitialWorkingDir = __processor_PropList.getValue ( "InitialWorkingDir" );
	*/
	String InitialWorkingDir = __ts_processor.getInitialWorkingDir();
	// FIXME SAM 2008-07-31 Remove redundant location of properties
	if ( InitialWorkingDir != null ) {
	    __processor_PropList.set ( "InitialWorkingDir", InitialWorkingDir );
		__processor_PropList.set ( "WorkingDir", InitialWorkingDir );
	    __ts_processor.setPropContents ( "WorkingDir", InitialWorkingDir );
	}
	Message.printStatus(2, routine,"InitialWorkingDir=" + __processor_PropList.getValue("InitialWorkingDir"));
	
	// Indicate whether output products/files should be created, or
	// just time series (to allow interactive graphing).
	boolean CreateOutput_boolean = __ts_processor.getCreateOutput().booleanValue();
	if ( __processor_PropList != null ) {
		String CreateOutput = app_PropList.getValue ( "CreateOutput" );
		if ( (CreateOutput != null) && CreateOutput.equalsIgnoreCase("False")){
			__ts_processor.setCreateOutput ( new Boolean(false));
			CreateOutput_boolean = false;
		}
		else {
			__ts_processor.setCreateOutput ( new Boolean(true));
		}
	}
	Message.printStatus(2, routine,"CreateOutput=" + __processor_PropList.getValue("CreateOutput") +
			" => " + CreateOutput_boolean );
	
	// Indicate whether time series should be cleared between runs.
	// If true, do not clear the time series between recursive
	// calls.  This is somewhat experimental to evaluate a master
	// commands file that runs other commands files.
	boolean AppendResults_boolean = false;
	// Indicate whether a recursive run of the processor is
	// being made (e.g., because runCommands() is used).
	boolean Recursive_boolean = false;
	if ( __processor_PropList != null ) {
		String Recursive = app_PropList.getValue ( "Recursive" );
		if ( (Recursive != null) && Recursive.equalsIgnoreCase("True")){
			Recursive_boolean = true;
			// Default for recursive runs is to NOT append results...
			AppendResults_boolean = false;
		}
	}
	Message.printStatus(2, routine,"Recursive=" + __processor_PropList.getValue("Recursive") +
			" => " + Recursive_boolean );

	int size = commandList.size();
	Message.printStatus ( 1, routine, "Processing " + size + " commands..." );
	StopWatch stopwatch = new StopWatch();
	stopwatch.start();
	//String expression = null;
	String command_String = null;

	TS ts = null;

	// Go through the expressions up front one time and set some important
	// flags to help performance, etc....

	boolean in_comment = false;
	Command command = null;	// The command to process
	CommandStatus command_status = null; // Put outside of main try to be able to use in catch.
	for ( int i = 0; i < size; i++ ) {
		ts = null;	// Initialize each time to allow for checks below.
		command = (Command)commandList.get(i);
		command_String = command.toString();
		if ( command_String == null ) {
			continue;
		}
		command_String = command_String.trim();
		if ( command_String.equals("") ) {
			continue;
		}
		// This is just a rough check to see if we have to go through
		// the effort of computing limits, which slows down the processing...
		if ( command_String.startsWith("/*") ) {
			in_comment = true;
			continue;
		}
		else if ( command_String.startsWith("*/") ) {
			in_comment = false;
			continue;
		}
		if ( in_comment ) {
			continue;
		}
	}

	// Change setting to allow warning messages to be turned off during the main loop.
    // This capability should not be needed if a command uses the new command status processing.

	int popup_warning_level = 2;		// Do not popup warnings (only to log)
	boolean popup_warning_dialog = false;// Do not popup warnings (handle in command status)
	if ( popup_warning_dialog ) {
		popup_warning_level = 1;
		Message.setPropValue ( "WarningDialogOKNoMoreButton=true" );
		Message.setPropValue ( "WarningDialogCancelButton=true" );
		Message.setPropValue ( "WarningDialogViewLogButton=true" );
	}
    else {
        // Turn off interactive warnings to pretent overload on user in loops.
        Message.setPropValue ( "ShowWarningDialog=false" );
    }
    
    // Clear any settings that may have been left over from the previous run and which
    // can impact the current run.
    
    processCommands_ResetDataForRunStart ( AppendResults_boolean );

	// Now loop through the expressions, query time series, and manipulate
	// to produce a list of final time series.  The following loop does the
	// initial queries.  A final loop does global post-processing compatible
	// with legacy command files (e.g., filling with historic averages)...
	// Use continue for commands that are not TS-related.
	//
	// The end of the loop calls the setTimeSeries() method that will either
	// add the time series to the list or replace what is already in the
	// list, depending on the value of "action".

	int ts_action = NONE;	// Action to take at end of loop (insert/update)
	int ts_pos = 0;		// Position of time series in list, for insert/update.
	in_comment = false;
	int i_for_message;	// This will be adjusted by
				// __num_prepended_commands - the user will
				// see command numbers in messages like (12),
				// indicating the twelfth command.

	String command_tag = null;	// String used in messages to allow
					// link back to the application
					// commands, for use with each command.
	//String message_tag = "ProcessCommands";
					// Tag used with messages generated in
					// this method.
	int i;	// Put here so can check count outside of end of loop
	boolean prev_command_complete_notified = false;// If previous command completion listeners were notified
										// May not occur if "continue" in loop.
	Command command_prev = null;	// previous command in loop
	// Indicat the state of the processor...
	__ts_processor.setIsRunning ( true );
	for ( i = 0; i < size; i++ ) {
		// 1-offset comand count for messages
		i_for_message = i + 1;
		command_tag = "" + i_for_message;	// Command number as integer 1+, for message/log handler.
		// If for some reason the previous command did not notify listeners of its completion (e.g., due to
		// continue in loop, do it now)...
		if ( !prev_command_complete_notified && (command_prev != null) ) {
			__ts_processor.notifyCommandProcessorListenersOfCommandCompleted ( (i - 1), size, command_prev );
		}
		prev_command_complete_notified = false;
		// Save the previous command before resetting to new command below.
		if ( i > 0 ) {
			command_prev = command;
		}
		// Check for a cancel, which would have been set by pressing
		// the cancel button on the warning dialog or by using the other TSTool menus...
		if ( __ts_processor.getCancelProcessingRequested() ) {
			// Set Warning dialog settings back to normal...
			if ( popup_warning_dialog ) {
				Message.setPropValue ( "WarningDialogViewLogButton=false" );
				Message.setPropValue ( "WarningDialogOKNoMoreButton=false" );
				Message.setPropValue ( "WarningDialogCancelButton=false" );
				Message.setPropValue ( "ShowWarningDialog=true" );
			}
            else {
                // Turn on interactive warnings again.
                Message.setPropValue ( "ShowWarningDialog=true" );
            }
			// Set flag so code interested in processor knows it is not running...
			__ts_processor.setIsRunning ( false );
			// Reset the cancel processing request and let interested code know that
			// processing has been cancelled.
			__ts_processor.setCancelProcessingRequested ( false );
			__ts_processor.notifyCommandProcessorListenersOfCommandCancelled (	i, size, command );
			return;
		}
		try {
		    // Catch errors in all the expressions.
			// Do not indent the body inside the try!
		ts = null;
		ts_action = NONE;
		//expression = (String)tsexpression_list.elementAt(i);
		command = (Command)commandList.get(i);
		command_String = command.toString();
		if ( command_String == null ) {
			continue;
		}
		command_String = command_String.trim();
		// All commands will implement CommandStatusProvider so get it...
		command_status = ((CommandStatusProvider)command).getCommandStatus();
		// Clear the run status (internally will set to UNKNOWN).
		command_status.clearLog(CommandPhaseType.RUN);
		Message.printStatus ( 1, routine, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		Message.printStatus ( 1, routine,
			"Start processing command " + (i + 1) + " of " + size + ": \"" + command_String + "\" " );
		// Notify any listeners that the command is running...
		__ts_processor.notifyCommandProcessorListenersOfCommandStarted ( i, size, command );

		if ( command_String.equals("") ) {
			// Empty line.  Mark as processing successful.
			command_status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
			command_status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
			continue;
		}
		if ( command_String.startsWith("#") ) {
			// Comment.  Mark as processing successful.
			command_status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
			command_status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
			continue;
		}
		else if ( command_String.startsWith("/*") ) {
			in_comment = true;
			command_status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
			command_status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
			continue;
		}
		else if ( command_String.startsWith("*/") ) {
			in_comment = false;
			command_status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
			command_status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
			continue;
		}
		if ( in_comment ) {
			command_status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
			command_status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
			continue;
		}
		// TODO SAM 2005-09-14 Evaluate how this works with other TSAnalyst capabilities
		else if ( command_String.regionMatches(true,0,"createYearStatisticsReport",0,26)){
			do_createYearStatisticsReport ( command_String );
			continue;
		}
		else if ( command_String.equalsIgnoreCase("end") ||	command_String.equalsIgnoreCase("exit") ||
                command_String.equalsIgnoreCase("exit()") ) {
			// Exit the processing, but do historic average filling...
			Message.printStatus ( 1, routine, "Exit - stop processing time series." );
			ts_action = EXIT;
			break;
		}
		else if ( command_String.regionMatches( true,0,"fillProrate",0,11)){
			// Fill missing data in the time series by prorating
			// one time series to another...
			do_fillProrate ( command_String );
			continue;
		}
        // FIXME SAM 2008-01-04 Is this command even supported/documented?
		else if ( command_String.regionMatches( true,0,"shift(",0,6) ||
                command_String.regionMatches( true,0,"shift (",0,7) ) {
			// Shift the time series from one date to another...
			do_shift ( command_String );
			continue;
		}
		// Detect a time series identifier, which needs to be processed to read the time series...
		else if ( TSCommandProcessorUtil.isTSID(command_String) ) {
			// Probably a raw time series identifier.  Try to read it...
			if ( Message.isDebugOn ) {
				Message.printDebug ( 1, routine, "Reading time series now..." );
			}
			ts = readTimeSeries ( popup_warning_level, command_tag, command_String, true );
			if ( Message.isDebugOn ) {
				Message.printDebug ( 1, routine, "...read TS" );
			}
			if ( ts == null ) {
				// Need to make sure we don't get redundant warnings.  Throw an exception and catch in the main loop...
				++error_count;
				throw new Exception ( "Null TS from read.  Unable to process command \"" + command_String + "\"" );
			}
			ts_action = INSERT_TS;
			// Append to the end of the time series list...
			ts_pos = getTimeSeriesSize();
		}
	
		// Check for obsolete commands (do this last to minimize the amount of processing through this code)...
		// Do this at the end because this logic may seldom be hit if valid commands are processed above.  
		
		else if ( processCommands_CheckForObsoleteCommands(command_String, (CommandStatusProvider)command, message_tag, i_for_message) ) {
			// Had a match so increment the counters.
			++update_count;
			++error_count;
		}
		// Command factory for remaining commands...
		else {
            // Try the Command class code...
			try {
                // Make sure the command is valid...
				// Initialize the command (parse)...
				// TODO SAM 2007-09-05 Need to evaluate where the
				// initialization occurs (probably the initial edit or load)?
				if ( Message.isDebugOn ) {
					Message.printDebug ( 1, routine, "Initializing the Command for \"" +	command_String + "\"" );
				}
				if ( command instanceof CommandStatusProvider ) {
					((CommandStatusProvider)command).getCommandStatus().clearLog(CommandPhaseType.INITIALIZATION);
				}
				if ( command instanceof CommandStatusProvider ) {
					((CommandStatusProvider)command).getCommandStatus().clearLog(CommandPhaseType.DISCOVERY);
				}
				command.initializeCommand ( command_String, __ts_processor, true );
				// TODO SAM 2005-05-11 Is this the best place for this or should it be in the
				// runCommand()?...
				// Check the command parameters...
				if ( Message.isDebugOn ) {
					Message.printDebug ( 1, routine, "Checking the parameters for command \"" + command_String + "\"" );
				}
				command.checkCommandParameters ( command.getCommandParameters(), command_tag, 2 );
				// Clear the run status for the command...
				if ( command instanceof CommandStatusProvider ) {
					((CommandStatusProvider)command).getCommandStatus().clearLog(CommandPhaseType.RUN);
				}
				// Run the command...
				if ( Message.isDebugOn ) {
					Message.printDebug ( 1, routine, "Running command through new code..." );
				}
				command.runCommand ( i_for_message );
				if ( Message.isDebugOn ) {
					Message.printDebug ( 1, routine, "...back from running command." );
				}
			}
			catch ( InvalidCommandSyntaxException e ) {
				message = "Unable to process command - invalid syntax (" + e + ").";
				if ( command instanceof CommandStatusProvider ) {
				       if (	CommandStatusUtil.getHighestSeverity((CommandStatusProvider)command).
                               greaterThan(CommandStatusType.UNKNOWN) ) {
				           // No need to print a message to the screen because a visual marker will be shown, but log...
				           Message.printWarning ( 2,
				                   MessageUtil.formatMessageTag(command_tag,
				                           ++error_count), routine, message );
                       }
                       if ( command instanceof GenericCommand ) {
                            // The command class will not have added a log record so do it here...
                            ((CommandStatusProvider)command).getCommandStatus().addToLog ( CommandPhaseType.RUN,
                                    new CommandLogRecord(CommandStatusType.FAILURE,
                                            message, "Check the log for more details." ) );
                       }
				}
				else {
				    // Command has not been updated to set warning/failure in status so show here
					Message.printWarning ( popup_warning_level,
						MessageUtil.formatMessageTag(command_tag,
						++error_count), routine, message );
				}
				// Log the exception.
				if (Message.isDebugOn) {
					Message.printDebug(3, routine, e);
				}
				continue;
			}
			catch ( InvalidCommandParameterException e ) {
				message = "Unable to process command - invalid parameter (" + e + ").";
				if ( command instanceof CommandStatusProvider ) {
				    if ( CommandStatusUtil.getHighestSeverity((CommandStatusProvider)command).
                            greaterThan(CommandStatusType.UNKNOWN) ) {
				        // No need to print a message to the screen because a visual marker will be shown, but log...
				        Message.printWarning ( 2,
				                MessageUtil.formatMessageTag(command_tag,
				                        ++error_count), routine, message );
                    }
                    if ( command instanceof GenericCommand ) {
                        // The command class will not have added a log record so do it here...
                        ((CommandStatusProvider)command).getCommandStatus().addToLog ( CommandPhaseType.RUN,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                        message, "Check the log for more details." ) );
                    }
				}
				else {
				    // Command has not been updated to set warning/failure in status so show here
					Message.printWarning ( popup_warning_level,
							MessageUtil.formatMessageTag(command_tag,
									++error_count), routine, message );
				}
				if (Message.isDebugOn) {
					Message.printWarning(3, routine, e);
				}
				continue;
			}
			catch ( CommandWarningException e ) {
				message = "Warnings were generated processing command - output may be incomplete (" + e + ").";
				if ( command instanceof CommandStatusProvider ) {
				    if ( CommandStatusUtil.getHighestSeverity((CommandStatusProvider)command).
                            greaterThan(CommandStatusType.UNKNOWN) ) {
				        // No need to print a message to the screen because a visual marker will be shown, but log...
				        Message.printWarning ( 2,
				                MessageUtil.formatMessageTag(command_tag,
				                        ++error_count), routine, message );
                    }
                    if ( command instanceof GenericCommand ) {
                        // The command class will not have added a log record so do it here...
                        ((CommandStatusProvider)command).getCommandStatus().addToLog ( CommandPhaseType.RUN,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                        message, "Check the log file for more details." ) );
                    }
				}
				else {	// Command has not been updated to set warning/failure in status so show here
					Message.printWarning ( popup_warning_level,
							MessageUtil.formatMessageTag(command_tag,
									++error_count), routine, message );
				}
				if (Message.isDebugOn) {
					Message.printDebug(3, routine, e);
				}
				continue;
			}
			catch ( CommandException e ) {
				message = "Error processing command - unable to complete command (" + e + ").";
				if ( command instanceof CommandStatusProvider ) {
				    if ( CommandStatusUtil.getHighestSeverity((CommandStatusProvider)command).
                            greaterThan(CommandStatusType.UNKNOWN) ) {
				        // No need to print a message to the screen because a visual marker will be shown, but log...
				        Message.printWarning ( 2,
				                MessageUtil.formatMessageTag(command_tag,++error_count), routine, message );
                    }
                    if ( command instanceof GenericCommand ) {
                        // The command class will not have added a log record so do it here.  Because the
                        // exception thrown by GenericCommand.runCommand() is not very specific, verify that
                        // this is a recognized command...
                        TSCommandFactory cf = new TSCommandFactory ();
                        try {
                            cf.newCommand(command.toString());
                        }
                        catch ( UnknownCommandException e2 ) {
                            ((CommandStatusProvider)command).getCommandStatus().addToLog ( CommandPhaseType.RUN,
                                    new CommandLogRecord(CommandStatusType.FAILURE,
                                            "Unknown command \"" + command.toString() + "\".",
                                            "Verify the spelling of the command name - " +
                                            "recreate with command editor if necessary." ) );
                        }
                        ((CommandStatusProvider)command).getCommandStatus().addToLog ( CommandPhaseType.RUN,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                        message, "Check the log file for more details." ) );
                    }
				}
				else {
				    // Command has not been updated to set warning/failure in status so show here
					Message.printWarning ( popup_warning_level,
						MessageUtil.formatMessageTag(command_tag,
						++error_count), routine, message );
				}
				if (Message.isDebugOn) {
					Message.printDebug(3, routine, e);
				}
				continue;
			}
			catch ( Exception e ) {
				message = "Unexpected error processing command - unable to complete command (" + e + ").";
				if ( command instanceof CommandStatusProvider ) {
					// Add to the log as a failure...
					Message.printWarning ( 2,
						MessageUtil.formatMessageTag(command_tag,++error_count), routine, message );
                    // Always add to the log because this type of exception is unexpected from a Command object.
					command_status.addToLog(CommandPhaseType.RUN,
							new CommandLogRecord(CommandStatusType.FAILURE,
									"Unexpected exception \"" + e.getMessage() + "\"",
									"See log file for details.") );
				}
				else {
					Message.printWarning ( popup_warning_level,
							MessageUtil.formatMessageTag(command_tag,
									++error_count), routine, message );
				}
				Message.printWarning ( 3, routine, e );
				continue;
			}
		}

		if ( ts_action == NONE ) {
			// Don't do anything...
			continue;
		}
		else {
            // Need to insert or update a time series...
			processTimeSeriesAction ( ts_action, ts, ts_pos );
		}

		Message.printStatus ( 1, routine,
		"Retrieved time series for \"" + command_String + "\" (" +  (i + 1) + " of " + size + " commands)" );

		} // Main catch
		catch ( Exception e ) {
			Message.printWarning ( popup_warning_level,
			MessageUtil.formatMessageTag(command_tag,
			++error_count), routine,
			"There was an error processing command: \"" +
			command_String + "\"" );
			Message.printWarning ( 3, routine, e );
			if ( command instanceof CommandStatusProvider ) {
				// Add to the command log as a failure...
				command_status.addToLog(CommandPhaseType.RUN,
						new CommandLogRecord(CommandStatusType.FAILURE,
								"Unexpected error \"" + e.getMessage() + "\"", "See log file for details.") );
			}
		}
		catch ( OutOfMemoryError e ) {
			Message.printWarning ( popup_warning_level,
			MessageUtil.formatMessageTag(command_tag,
			++error_count), routine,
			"The command processor ran out of memory.  Exit and restart.\n" +
			"Try increasing the -Xmx setting for the Java Runtime Environment." );
			if ( command instanceof CommandStatusProvider ) {
				// Add to the command log as a failure...
				command_status.addToLog(CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
						"Out of memory error \"" + e.getMessage() + "\"",
						"Try increasing JRE memory with -Xmx.  See log file for details.  See troubleshooting documentation.") );
			}
			Message.printWarning ( 2, routine, e );
			System.gc();
			// May be able to save commands.
		}
		finally {
			// Always want to get to here for each command.
		}
		// Notify any listeners that the command is done running...
		prev_command_complete_notified = true;
		__ts_processor.notifyCommandProcessorListenersOfCommandCompleted ( i, size, command );
	}
	// If necessary, do a final notify for the last command...
	if ( !prev_command_complete_notified ) {
		if ( i == size ) {
			--i;
		}
		__ts_processor.notifyCommandProcessorListenersOfCommandCompleted ( i, size, command );
	}
	
	// Indicate that processing is done and now there is no need to worry about cancelling.
	__ts_processor.setIsRunning ( false );
	if ( __ts_processor.getCancelProcessingRequested() ) {
		// Have gotten to here probably because the last command was processed
		// and need to notify the listeners.
		__ts_processor.notifyCommandProcessorListenersOfCommandCancelled (	i, size, command );
	}
	__ts_processor.setCancelProcessingRequested ( false );
	
	if ( popup_warning_dialog ) {
		// Change so from this point user always has to acknowledge the warnings...
		Message.setPropValue ( "WarningDialogOKNoMoreButton=false" );
		Message.setPropValue ( "WarningDialogCancelButton=false" );
		Message.setPropValue ( "WarningDialogViewLogButton=false" );
	}
    // Make sure that important warnings are shown to the user...
    Message.setPropValue ( "ShowWarningDialog=true" );

	Message.printStatus ( 1, routine, "Retrieved " + __tslist.size() + " time series." );

	size = getTimeSeriesSize();

	// Get the final time - note this includes intervening warnings if any occurred...

	stopwatch.stop();
	Message.printStatus ( 1, routine, "Processing took " +
		StringUtil.formatString(stopwatch.getSeconds(),"%.2f") + " seconds" );

	// Check for fatal errors (for Command classes, only warn if failures since
	// others are likely not a problem)...

	int ml = 2;	// Message level for cleanup warnings
	if ( popup_warning_dialog ) {
		Message.setPropValue ( "WarningDialogViewLogButton=true" );
		ml = 1;
	}
	CommandStatusType max_severity = CommandStatusProviderUtil.getHighestSeverity ( commandList );
	if ( (_fatal_error_count > 0) || (error_count > 0) || max_severity.greaterThan(CommandStatusType.WARNING)) {

		if ( IOUtil.isBatch() ) {
			size = getMissingTS().size();
			if ( size > 0 ) {
				// FIXME SAM 2007-11-06 Put in CheckTimeSeries()...
				Message.printWarning ( ml, routine,"The following time series were not found:" );
				for ( int i2 = 0; i2 < size; i2++ ) {
					Message.printWarning ( ml, routine,"   "+(String)getMissingTS().elementAt(i2));
				}
			}
			// The following should will be passed through TSCommandProcessor.runCommands() and should
			// be caught when using TSCommandProcessorThreadRunner.runCommands().
			message = "There were warnings or failures processing commands.  The output may be incomplete.";
			Message.printWarning ( ml, routine, message );
			throw new RuntimeException ( message );
		}
		else {
		    Message.printWarning ( ml, routine,
			"There were warnings processing commands.  The output may be incomplete.\n" +
			"See the log file for information." );
		}
	}
	if ( update_count > 0 ) {
		Message.printWarning ( ml, routine,
		"There were warnings printed for obsolete commands.\n" +
		"See the log file for information.  The output may be incomplete." );
	}
	if ( popup_warning_dialog ) {
		Message.setPropValue ( "WarningDialogViewLogButton=false" );
	}

	// Clean up...
	message = null;
	ts = null;
}

/**
Check for obsolete commands and print an appropriate message.  Handling of warning increments is done in the
calling code.
@param command_String Command as string to check.
@param command Command to check (GenericCommand).
@param message_tag Message tag for logging.
@param i_for_message Command number for messages (1+).
*/
private boolean processCommands_CheckForObsoleteCommands(
        String command_String, CommandStatusProvider command, String message_tag, int i_for_message)
throws Exception
{	String routine = getClass().getName() + ".processCommands_CheckForObsoleteCommands";
    String message = null;  // problem - non-null will indicate obsolete command for return value
    String suggest = null;  // Suggestion to fix
	// Print at level 1 or set in command status because these messages need to be addressed.
	
	if (command_String.regionMatches(true,0,"-averageperiod",0,14)){
		message = "-averageperiod is obsolete.";
		suggest = "Use SetAveragePeriod()";
	}
	else if ( command_String.regionMatches(true,0,"-batch",0,6)) {
		// Old syntax command.  Leave around because this functionality has never really been implemented but
		// needs to be (e.g., call TSTool from web site and tell it to create a plot?).
	    message = "-batch has never been enabled.";
	    suggest = "Use -commmands (and -nomaingui)";
		IOUtil.isBatch ( true );
	}
	else if ( command_String.regionMatches(true,0,"-binary_day_cutoff",0,18)) {
		message = "-binary_day_cutoff is obsolete.";
		suggest = "There is no need to use any command equivalent.  Remove the command.";
	}
	else if ( command_String.regionMatches(true,0,"-binary_ts_file",0,20)) {
		message = "-binary_ts_file is obsolete.";
		suggest = "There is no need to use any command equivalent.  Remove the command.";
	}
    else if ( command_String.regionMatches(true,0,"createTraces",0,12)) {
        message = "CreateTraces() is obsolete.";
        suggest = "Use CreateEnsemble().";
    }
	else if ( command_String.equalsIgnoreCase("-cy") ) {
		message = "-cy is obsolete.";
		suggest = "Use SetOutputYearType().";
	}
	else if(command_String.regionMatches(true,0,"-data_interval",0,14)){
		message = "-data_interval is obsolete.";
		suggest = "Use CreateFromList().";
	}
	else if ( command_String.regionMatches(true,0,"-datasource",0,11)) {
	    message = "-datasource is obsolete.";
	    suggest = "Use HydroBase login dialog or OpenHydroBase().";
	}
	else if(command_String.regionMatches(true,0,"-data_type",0,10)){
		message = "-data_type is obsolete.";
		suggest = "Use CreateFromList().";
	}
	else if ( command_String.regionMatches(true,0,"-detailedheader",0,15) ) {
		message = "-detailedheader is obsolete.";
		suggest = "If this feature is needed, it can be added to output commands.";
	}
	else if ( command_String.regionMatches(true,0,"-d",0,2) ) {
	    message = "-d is obsolete..";
        suggest = "Use SetDebugLevel().";
	}
    else if ( command_String.regionMatches(true,0,"day_to_month_reservoir",0,22) ) {
        message = "day_to_month_reservoir is obsolete.";
        suggest = "Use EndOfMonthTSToDayTS().";
    }
    else if ( command_String.regionMatches( true,0,"FillCarryForward",0,16) ) {
        message = "FillCarryForward() is obsolete.";
        suggest = "Use FillRepeat().";
    }
	else if ( command_String.regionMatches(true,0,"fillconst(",0,10) ) {
		message ="fillconst() is obsolete.";
		suggest = "Use FillConstant().";
	}
	else if ( command_String.regionMatches(	true,0,"-filldata",0,9) ) {
		message = "-filldata is obsolete.";
		suggest = "Use ReadPatternFile()";
	}
	else if ( command_String.regionMatches(true,0,"-fillhistave",0,12)){
		message = "-fillhistave is obsolete.  Automatically using appropriate new command.";
		suggest = "Use FillHistMonthAverage() or FillHistYearAverage().";
	}
	else if ( command_String.regionMatches( true,0,"fillpattern_setconstbefore",0,26)){
		message = "fillpattern_setconstbefore() is obsolete.";
		suggest = "Use Fillpattern() and SetConstant().";
	}
	else if ( command_String.regionMatches(true,0,"-fillUsingComments",0,18)){
		message = "-fillUsingComments is obsolete.";
		suggest = "Use FillUsingDiversionComments().";
	}
	else if(command_String.regionMatches(true,0,"-ignorelezero",0,13) ){
		message = "-ignorelezero is obsolete.  Automatically setting IgnoreLEZero = true.";
		suggest = "Use SetIgnoreLEZero().";
		setIgnoreLEZero ( true );
	}
	else if ( command_String.regionMatches(true,0,"-include_missing_ts",0,19)) {
		message = "-include_missing_ts is obsolete.";
		suggest = "Use SetIncludeMissingTS().";
	}
	else if ( command_String.regionMatches( true,0,"-missing",0,8) ) {
		message = "-missing is obsolete.  Automatically using SetMissingDataValue().";
        suggest = "The missing value is specific to the time series and may " +
            "be set when read/created or as a property.";
	}
	else if ( command_String.regionMatches( true,0,"-ostatemod",0,10) ){
		message = "-ostatemod is obsolete.";
		suggest = "Use WriteStateMod().";
	}
	else if ( command_String.regionMatches( true,0,"-osummary",0,9) ){
		message = "-osummary is obsolete.";
		suggest = "Use WriteSummary() or other output commands.";
	}
	else if ( command_String.regionMatches(true,0,"-osummarynostats",0,16) ){
		message = "-osummarynostats is obsolete.";
		suggest = "Use WriteSummary() or other output commands.";
	}
	// Put this after all the other -o options...
	else if ( command_String.regionMatches( true,0,"-o",0,2) ){
		// Output in StateMod format...
		message = "\"-o File\" is obsolete.";
		suggest = "Use WriteStateMod() or other output commands.";
	}
	else if ( command_String.regionMatches(true,0,"regress",0,7) ) {
		message = "regress() is obsolete.";
		suggest = "Use FillRegression().";
	}
    else if ( command_String.regionMatches( true,0,"setBinaryTSDayCutoff",0,20)) {
        message = "SetBinaryTSDayCutoff() is obsolete.";
        suggest = "There is no need for this command.";
    }
    else if ( command_String.regionMatches( true,0,"setBinaryTSPeriod",0,17) ) {
        message = "SetBinaryTSPeriod() is obsolete.";
        suggest = "There is no need for this command.";
    }
	else if ( command_String.regionMatches(true,0,"setconst(",0,9) ) {
		message = "setconst() is obsolete.";
		suggest = "Use SetConstant().";
	}
    else if ( command_String.regionMatches(true,0,"setConstantBefore",0,17) ) {
        message = "SetConstantBefore() is obsolete.";
        suggest = "Use SetConstant().";
    }
	else if(command_String.regionMatches(true,0,"setconstbefore",0,14)){
		message = "setconstbefore() is obsolete.";
		suggest = "Use SetConstant().";
	}
	else if ( command_String.regionMatches(true,0,"setDatabaseEngine", 0,17) ) {
		message = "setDatabaseEngine is obsolete.";
		suggest = "Use OpenHydroBase().";
	}
	else if ( command_String.regionMatches(true,0,"setDatabaseHost", 0,15) ) {
		message = "setDatabaseHost is obsolete.";
		suggest = "Use OpenHydroBase().";
	}
	else if (command_String.regionMatches(true,0,"setDataSource",0,13)){
		message = "setDataSource is obsolete.";
		suggest = "Use OpenHydroBase().";
	}
    else if ( command_String.regionMatches(true,0,"setMissingDataValue",0,19) ) {
        message = "setMissingDataValue is obsolete.";
        suggest = "The missing value is specific to the time series and may " +
        		"be set when read/created or as a property.";
    }
	else if ( command_String.regionMatches(true,0,"setUseDiversionComments", 0,23) ) {
		message = "setUseDiversionComments() is obsolete.";
		suggest = "Use FillUsingDiversionComments().";
	}
	else if ( command_String.regionMatches(true,0,"-slist",0,6) ) {
		message = "-slist is obsolete.";
		suggest = "Use CreateFromList().";
	}
	else if ( command_String.regionMatches(true,0,"-units",0,6) ) {
		message = "-units is obsolete.";
		suggest = "Remove command because other commands now handle units.";
	}
	else if ( command_String.equalsIgnoreCase("-wy") ) {
		message = "-wy is obsolete.";
		suggest = "Use SetOutputYearType().";
	}
	// Put after -wy...
	else if ( command_String.regionMatches(true,0,"-w",0,2) ) {
		message = "-w is obsolete.";
		suggest = "Use SetWarningLevel().";
	}
	else if ( command_String.regionMatches(true,0,"setRegressionPeriod",0,19) ) {
		message = "SetRegressionPeriod() is obsolete.";
		suggest = "Set dates in FillRegression() instead.";
	}
	else if ( TimeUtil.isDateTime ( StringUtil.getToken(command_String," \t", StringUtil.DELIM_SKIP_BLANKS,0) ) ) { 
		// Old-style date...
		message = "Setting output period with MM/YYYY MM/YYYY is obsolete.";
		suggest = "Use SetOutputPeriod().";
	}
	if ( message != null ) {
        Message.printWarning ( 2, routine, message );
        if ( suggest != null ) {
            command.getCommandStatus().addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE, message, suggest ) );
        }
        // An obsolete command was detected
        return true;
	}
	// The command is not obsolete
	return false;
}

/**
Reset all the data to initial values, to be controlled by the commands.
In particular, reset the dates that might have been set in a previous run and which
need to be reset for the current run.
@param AppendResults_boolean Indicates whether the results from the current run should be
appended to a previous run.
*/
private void processCommands_ResetDataForRunStart ( boolean AppendResults_boolean )
throws Exception
{
    // The following are the initial defaults...
    setAutoExtendPeriod ( true );
    __datatestlist = null;
    __datetime_Hashtable.clear();
    setIncludeMissingTS ( false );
    setIgnoreLEZero ( false );
    setInputStart ( null );
    setInputEnd ( null );
    getMissingTS().removeAllElements();
    setOutputStart ( null );
    setOutputEnd ( null );
    setOutputYearType ( _CALENDAR_YEAR );
    setPreviewExportedOutput ( false );
    __reference_date = null;
    // Free all data from the previous run...
    if ( !AppendResults_boolean ) {
        __ts_processor.clearResults ();
    }
}

/**
Process a list of time series to produce an output product.
The time series are typically generated from a previous call to
processCommands() or processTimeSeriesCommands().
@param ts_indices List of time series indices to process from the internal
time series list.  If null, all are processed.  The indices do not have to be in order.
@param proplist List of properties to define the output:
<table width=100% cellpadding=10 cellspacing=0 border=2>
<tr>
<td><b>Property</b></td>	<td><b>Description</b></td>	<td><b>Default</b></td>
</tr>

<tr>
<td><b>OutputFormat</b></td>
<td>
<ul>
<li>-ostatemod  Output StateMod file.</li>
</ul>
</td>
<td>No default.</td>
</tr>

<tr>
<td><b>OutputFile</b></td>
<td>Output file.</td>
<td>No default.</td>
</tr>
</table>
@param comments Comments to add to the to of output files.
@exception IOException if there is an error generating the results.
*/
protected void processTimeSeries ( int ts_indices[], PropList proplist )
throws IOException
{	String	message = null,	// Message string
		routine = "TSEngine.processTimeSeries";

    // List of time series to output, determined from the time series in memory
    // and a list of selected array positions (e.g., from a GUI.

    Vector tslist_output = null;

	// Define a local proplist to better deal with null...
	PropList props = proplist;
	if ( props == null ) {
		props = new PropList ( "" );
	}

    if ( (__tslist == null) || (__tslist.size() == 0) ) {
		message = "No time series to process.";
		Message.printWarning ( 1, routine, message );
		throw new IOException ( message );
	} 
	Message.printStatus ( 1, routine, "Creating output from previously queried time series..." );

	// Put together the Vector to output, given the requested ts_indices.
	// Need to do more work if BinaryTS, but hopefully if BinaryTS
	// batch mode will be used?!  Use a member __tslist_output so we can
	// manage memory and not leave used if an exception occurs (clean up the next time).

	int nts = __tslist.size();
	if ( ts_indices == null ) {
		// Use the entire list...
		tslist_output = __tslist;
	}
	else {
        int ts_indices_size = ts_indices.length;
		if ( tslist_output == null ) {
			tslist_output = new Vector ( ts_indices_size );
		}
		else {
		    // This does not work because if the Vector is passed
			// to TSViewJFrame, etc., the Vector is reused but the
			// contents will have changed.  Therefore, need to
			// create a new Vector for each output product.  The
			// time series themselves can be re-used.
			//__tslist_output.removeAllElements();
			tslist_output = new Vector ( ts_indices_size );
		}
		for ( int i = 0; i < ts_indices_size; i++ ) {
			if ( (ts_indices[i] >= 0) && (ts_indices[i] < nts) ) {
				tslist_output.addElement ( __tslist.elementAt(ts_indices[i]) );
			}
		}
	}

	// Figure out the output.  This method is going to be called with
	// legacy -o options (batch mode) as well as new PropList syntax.  Make
	// sure to support legacy first and then phase in new approach...

	int output_format = OUTPUT_NONE;
	String precision_string = "*";
	setPreviewExportedOutput ( false );

	String prop_value = props.getValue ( "OutputFormat" );
	Message.printStatus ( 1, routine,"Output format is \""+prop_value+"\"");
	String parameters = props.getValue ( "Parameters" );
	if ( prop_value != null ) {
		// Reports...
		if ( prop_value.equalsIgnoreCase("-odata_coverage_report") ) {
			output_format = OUTPUT_DATA_COVERAGE_REPORT;
		}
		else if ( prop_value.equalsIgnoreCase("-odata_limits_report")) {
			output_format = OUTPUT_DATA_LIMITS_REPORT;
		}
		else if ( prop_value.equalsIgnoreCase(
			"-omonth_mean_summary_report")) {
			output_format = OUTPUT_MONTH_MEAN_SUMMARY_REPORT;
		}
		else if ( prop_value.equalsIgnoreCase(
			"-omonth_total_summary_report")) {
			output_format = OUTPUT_MONTH_TOTAL_SUMMARY_REPORT;
		}
		else if ( prop_value.equalsIgnoreCase("-oyear_to_date_report")){
			output_format = OUTPUT_YEAR_TO_DATE_REPORT;
			try {
                __reference_date = DateTime.parse(parameters);
			}
			catch ( Exception e ) {
				__reference_date = null;
			}
		}

		// Time series file output...

		else if ( prop_value.equalsIgnoreCase("-odatevalue") ) {
			output_format = OUTPUT_DATEVALUE;
		}
		else if ( prop_value.equalsIgnoreCase("-onwsrfsesptraceensemble") ) {
			output_format = OUTPUT_NWSRFSESPTRACEENSEMBLE_FILE;
		}
		else if ( prop_value.equalsIgnoreCase("-onwscard") ) {
			output_format = OUTPUT_NWSCARD_FILE;
		}
		else if ( prop_value.equalsIgnoreCase("-oriverware") ) {
			output_format = OUTPUT_RIVERWARE_FILE;
		}
		else if ( prop_value.equalsIgnoreCase("-oshefa") ) {
			output_format = OUTPUT_SHEFA_FILE;
		}
		else if ( prop_value.equalsIgnoreCase("-ostatemod") ) {
			output_format = OUTPUT_STATEMOD;
		}
		else if ( prop_value.equalsIgnoreCase("-osummary") ) {
			output_format = OUTPUT_SUMMARY;
		}
		else if ( prop_value.equalsIgnoreCase("-otable") ) {
			output_format = OUTPUT_TABLE;
		}

		// Graph output...

		else if ( prop_value.equalsIgnoreCase("-oannual_traces_graph")){
			output_format = OUTPUT_ANNUAL_TRACES_GRAPH;
		}
		else if ( prop_value.equalsIgnoreCase("-obar_graph")){
			output_format = OUTPUT_BAR_GRAPH;
			// Use parameters for the position of the bars.
		}
		else if ( prop_value.equalsIgnoreCase("-odoublemass_graph")){
			output_format = OUTPUT_DOUBLE_MASS_GRAPH;
		}
		else if ( prop_value.equalsIgnoreCase("-oduration_graph")){
			output_format = OUTPUT_DURATION_GRAPH;
		}
		else if ( prop_value.equalsIgnoreCase("-olinegraph")){
			output_format = OUTPUT_LINEGRAPH;
		}
		else if ( prop_value.equalsIgnoreCase("-olinelogygraph")){
			output_format = OUTPUT_LINELOGYGRAPH;
		}
		else if ( prop_value.equalsIgnoreCase("-opointgraph")){
			output_format = OUTPUT_POINT_GRAPH;
		}
		else if ( prop_value.equalsIgnoreCase("-oporgraph")){
			output_format = OUTPUT_PORGRAPH;
		}
		else if (prop_value.equalsIgnoreCase("-oPredictedValue_graph")){
			output_format = OUTPUT_PredictedValue_GRAPH;
		}
		else if ( prop_value.equalsIgnoreCase("-oPredictedValueResidual_graph")){
			output_format = OUTPUT_PredictedValueResidual_GRAPH;
		}
		else if ( prop_value.equalsIgnoreCase("-oxyscatter_graph")){
			output_format = OUTPUT_XY_SCATTER_GRAPH;
		}
	}
	prop_value = props.getValue ( "OutputFile" );
	if ( prop_value != null ) {
		if ( prop_value.equalsIgnoreCase("-preview") ) {
			setPreviewExportedOutput ( true );
		}
		else {
            __output_file = prop_value;
		}
	}
	prop_value = props.getValue ( "Precision" );
	if ( prop_value != null ) {
		if ( prop_value.equals("*") || StringUtil.isInteger(prop_value) ) {
			precision_string = prop_value;
		}
		else {
            __output_file = prop_value;
		}
	}

	if ( output_format == OUTPUT_STATEMOD ) {
		try {
            writeStateModTS ( tslist_output, __output_file,
			precision_string, formatOutputHeaderComments(__ts_processor.getCommands()) );
		} catch ( Exception e ) {
			message = "Error writing StateMod file \"" + __output_file + "\"";
			Message.printWarning ( 1, routine, message );
			Message.printWarning ( 2, routine, e );
			throw new IOException ( message );
		}
	}
	else if ( output_format == OUTPUT_DATA_COVERAGE_REPORT ) {
		PropList reportProps = new PropList ("ReportJFrame.props");
		reportProps.set ( "HelpKey", "TSTool" );
		reportProps.set ( "TotalWidth", "750" );
		reportProps.set ( "TotalHeight", "550" );
		reportProps.set ( "Title", "Data Coverage Report" );
		reportProps.set ( "DisplayFont", "Courier" );
		reportProps.set ( "DisplaySize", "11" );
		// reportProps.set ( "DisplayStyle", Font.PLAIN );
		reportProps.set ( "PrintFont", "Courier" );
		// reportProps.set ( "PrintFont", Font.PLAIN );
		reportProps.set ( "PrintSize", "7" );
		reportProps.set ( "PageLength", "5000" );
		reportProps.set ( "Search", "true" );

		try {
		    // For now, put the code in here at the bottom of this file...
			Vector report = createDataCoverageReport ( tslist_output );
			new ReportJFrame ( report, reportProps );
		}
		catch ( Exception e ) {
			Message.printWarning ( 1, routine, "Error printing summary." );
			Message.printWarning ( 2, routine, e );
		}
	}
	else if ( output_format == OUTPUT_DATA_LIMITS_REPORT ) {
		PropList reportProps = new PropList ("ReportJFrame.props");
		reportProps.set ( "HelpKey", "TSTool" );
		reportProps.set ( "TotalWidth", "750" );
		reportProps.set ( "TotalHeight", "550" );
		reportProps.set ( "Title", "Data Limits Report" );
		reportProps.set ( "DisplayFont", "Courier" );
		reportProps.set ( "DisplaySize", "11" );
		// reportProps.set ( "DisplayStyle", Font.PLAIN );
		reportProps.set ( "PrintFont", "Courier" );
		// reportProps.set ( "PrintFont", Font.PLAIN );
		reportProps.set ( "PrintSize", "7" );
		reportProps.set ( "PageLength", "5000" );
		reportProps.set ( "Search", "true" );

		try {
		    // For now, put the code in here at the bottom of this file...
			Vector report = createDataLimitsReport(tslist_output);
			new ReportJFrame ( report, reportProps );
		}
		catch ( Exception e ) {
			Message.printWarning ( 1, routine, "Error printing summary." );
			Message.printWarning ( 2, routine, e );
		}
	}
	else if ( output_format == OUTPUT_DATEVALUE ) {
		try {
		TS	tspt = null;
		if ( (tslist_output != null) && (tslist_output.size() > 0)){
			tspt = (TS)tslist_output.elementAt(0);
		}
		String units = null;
		if ( tspt != null ) {
			units = tspt.getDataUnits();
			Message.printStatus ( 1, "", "Data units are " + units);
		}

		// Format the comments to add to the top of the file.  In this
		// case, add the commands used to generate the file...
		if ( TSUtil.intervalsMatch ( tslist_output )) {
			// Need date precision to be day...
			//DateTime date1 = _date1;
			// Why was this set here????
			// SAM - 2001-08-27
			//if ( _date1 != null ) {
			//	date1 = new DateTime ( _date1 );
			//	date1.setPrecision (DateTime.PRECISION_DAY);
			//	date1.setDay ( 1 );
			//}
			//DateTime date2 = _date2;
			//if ( _date2 != null ) {
			//	date2 = new DateTime ( _date2 );
			//	date2.setPrecision (DateTime.PRECISION_DAY);
			//	date2.setDay ( TimeUtil.numDaysInMonth(
			//		date2.getMonth(), date2.getYear() ));
			//}
			//DateValueTS.writeTimeSeries ( __tslist_output,
			//	_output_file,
			//	date1, date2, units, true );
			
			DateValueTS.writeTimeSeriesList ( tslist_output,
				__output_file, __OutputStart_DateTime, __OutputEnd_DateTime, units, true );
		}
		else {
            Message.printWarning ( 1, routine, "Unable to write DateValue time series of different intervals." );
		}
		} catch ( Exception e ) {
			message = "Error writing DateValue file \"" + __output_file + "\"";
			Message.printWarning ( 1, routine, message );
			Message.printWarning ( 2, routine, e );
			throw new IOException ( message );
		}
	}
	else if ( output_format == OUTPUT_NWSRFSESPTRACEENSEMBLE_FILE ) {
		try {	
			PropList esp_props = new PropList ( "esp" );
			NWSRFS_ESPTraceEnsemble esp = new NWSRFS_ESPTraceEnsemble (	tslist_output, esp_props );
			esp.writeESPTraceEnsembleFile ( __output_file );
		} catch ( Exception e ) {
			message = "Error writing NWSRFS ESP Trace Ensemble file \"" + __output_file + "\"";
			Message.printWarning ( 1, routine, message );
			Message.printWarning ( 2, routine, e );
			throw new IOException ( message );
		}
	}
	else if ( (output_format == OUTPUT_MONTH_MEAN_SUMMARY_REPORT) ||
		(output_format == OUTPUT_MONTH_TOTAL_SUMMARY_REPORT) ) {
		String daytype = "Mean";
		PropList reportProps = new PropList ("ReportJFrame.props");
		reportProps.set ( "HelpKey", "TSTool" );
		reportProps.set ( "TotalWidth", "750" );
		reportProps.set ( "TotalHeight", "550" );
		if ( output_format == OUTPUT_MONTH_MEAN_SUMMARY_REPORT ) {
			reportProps.set ( "Title", "Monthly Summary Report (Daily Means)" );
			daytype = "Mean";
		}
		else {
		    reportProps.set ( "Title", "Monthly Summary Report (Daily Totals)" );
			daytype = "Total";
		}
		reportProps.set ( "DisplayFont", "Courier" );
		reportProps.set ( "DisplaySize", "11" );
		// reportProps.set ( "DisplayStyle", Font.PLAIN );
		reportProps.set ( "PrintFont", "Courier" );
		// reportProps.set ( "PrintFont", Font.PLAIN );
		reportProps.set ( "PrintSize", "7" );
		reportProps.set ( "PageLength", "5000" );
		reportProps.set ( "Search", "true" );

		PropList sumprops = new PropList ( "" );
		sumprops.set ( "DayType", daytype );
		if ( getOutputYearTypeInt() == _WATER_YEAR ) {
			sumprops.set ( "CalendarType", "WaterYear" );
		}
		else {
		    sumprops.set ( "CalendarType", "CalendarYear" );
		}

		try {
		    // For now, put the code in here at the bottom of this file...
			Vector report = createMonthSummaryReport ( tslist_output, sumprops );
			new ReportJFrame ( report, reportProps );
		}
		catch ( Exception e ) {
			Message.printWarning ( 1, routine, "Error printing summary." );
			Message.printWarning ( 2, routine, e );
		}
	}
	else if ( output_format == OUTPUT_NWSCARD_FILE ) {
		// Write a NWS Card file containing a single time series.
		try {
		TS	tspt = null;
		int	list_size = 0;
		if ( (tslist_output != null) && (tslist_output.size() > 0)){
			tspt = (TS)tslist_output.elementAt(0);
			list_size = tslist_output.size();
		}
		// NWS Card files can only contain one time series...
		if ( list_size != 1 ) {
			message = "Only 1 time series can be written to a NWS Card file";
			Message.printWarning ( 1, routine, message );
			throw new Exception ( message );
		}
		String units = null;
		if ( tspt != null ) {
			units = tspt.getDataUnits();
			Message.printStatus ( 1, "", "Data units are " + units);
		}

		// Format the comments to add to the top of the file.  In this
		// case, add the commands used to generate the file...
		int interval_mult = 1;
		interval_mult = ((TS)tslist_output.elementAt(0)).getDataIntervalMult();
		// Need date precision to be hour and NWS uses hour 1 - 24 so adjust dates accordingly.
		DateTime date1 = __OutputStart_DateTime;
		if ( __OutputStart_DateTime != null ) {
			date1 = new DateTime ( __OutputStart_DateTime );
			date1.setPrecision (DateTime.PRECISION_HOUR);
			date1.setDay ( 1 );
			date1.setHour ( interval_mult );
		}
		DateTime date2 = __OutputEnd_DateTime;
		if ( __OutputEnd_DateTime != null ) {
			date2 = new DateTime ( __OutputEnd_DateTime );
			date2.setPrecision (DateTime.PRECISION_HOUR);
			date2.setDay ( TimeUtil.numDaysInMonth(	date2.getMonth(), date2.getYear() ));
			date2.addDay ( 1 );
			date2.setHour ( 0 );
		}
		NWSCardTS.writeTimeSeries ( (TS)tslist_output.elementAt(0),	__output_file, date1, date2, units, true );
		} catch ( Exception e ) {
			message = "Error writing NWS Card file \"" + __output_file + "\"";
			Message.printWarning ( 1, routine, message );
			Message.printWarning ( 2, routine, e );
			throw new IOException ( message );
		}
	}
	else if ( output_format == OUTPUT_RIVERWARE_FILE ) {
		// Write a RiverWare file containing a single time series.
		try {
		TS	tspt = null;
		int	list_size = 0;
		if ( (tslist_output != null) && (tslist_output.size() > 0)){
			tspt = (TS)tslist_output.elementAt(0);
			list_size = tslist_output.size();
		}
		// RiverWare files can only contain one time series...
		if ( list_size != 1 ) {
			message = "Only 1 time series can be written to a RiverWare file";
			Message.printWarning ( 1, routine, message );
			throw new Exception ( message );
		}
		String units = null;
		if ( tspt != null ) {
			units = tspt.getDataUnits();
			Message.printStatus ( 1, "", "Data units are " + units);
		}

		// Format the comments to add to the top of the file.  In this
		// case, add the commands used to generate the file...
		RiverWareTS.writeTimeSeries ( (TS)tslist_output.elementAt(0),
			__output_file, __OutputStart_DateTime, __OutputEnd_DateTime, units, 1.0, null, -1.0, true );
		} catch ( Exception e ) {
			message = "Error writing RiverWare file \"" + __output_file + "\"";
			Message.printWarning ( 1, routine, message );
			Message.printWarning ( 2, routine, e );
			throw new IOException ( message );
		}
	}
	else if ( output_format == OUTPUT_SHEFA_FILE ) {
		try {
			Vector units_Vector = null;
			Vector PE_Vector = ShefATS.getPEForTimeSeries (tslist_output );
			Vector Duration_Vector = null;
			Vector AltID_Vector = null;
			PropList shef_props = new PropList ( "SHEF" );
			shef_props.set ( "HourMax=24" );
			ShefATS.writeTimeSeriesList ( tslist_output,
				__output_file, __OutputStart_DateTime,
				__OutputEnd_DateTime,
				units_Vector, PE_Vector,
				Duration_Vector, AltID_Vector, shef_props );
		} catch ( Exception e ) {
			message = "Error writing SHEF A file \"" + __output_file + "\"";
			Message.printWarning ( 1, routine, message );
			Message.printWarning ( 2, routine, e );
			throw new IOException ( message );
		}
	}
	else if ( output_format == OUTPUT_SUMMARY ) {
		try {
		// First need to get the summary strings...
		PropList sumprops = new PropList ( "Summary" );
		sumprops.set ( "Format", "Summary" );
		if ( getOutputYearTypeInt() == _WATER_YEAR ) {
			sumprops.set ( "CalendarType", "WaterYear" );
		}
		else {
            sumprops.set ( "CalendarType", "CalendarYear" );
		}
		// Check the first time series.  If NWSCARD or DateValue, don't
		// use comments for header...
		/* TODO SAM 2004-07-20 - try default header always -
			transition to HydroBase comments being only additional information
		else {	// HydroBase, so use the comments for the header...
			sumprops.set ( "UseCommentsForHeader", "true" );
		}
		if ( _non_co_detected ) {
			// Data other that HydroBase, so format using standard
			// headers...
			sumprops.set ( "UseCommentsForHeader", "false" );
		}
		*/
		sumprops.set ( "PrintHeader", "true" );
		sumprops.set ( "PrintComments", "true" );
		if ( output_format == OUTPUT_SUMMARY ) {
			// Get the statistics...
			sumprops.set ( "PrintMinStats", "true" );
			sumprops.set ( "PrintMaxStats", "true" );
			sumprops.set ( "PrintMeanStats", "true" );
			sumprops.set ( "PrintNotes", "true" );
		}

		if ( IOUtil.isBatch() || !getPreviewExportedOutput() ) {
			try {
                Vector summary = TSUtil.formatOutput (__output_file, tslist_output, sumprops );	
				// Just write the summary to the given file...
				IOUtil.printStringList ( __output_file, summary);
			} catch ( Exception e ) {
				Message.printWarning ( 1, routine,"Unable to print summary to file \"" + __output_file + "\"" );
			}
		}
		else {
            PropList reportProps=new PropList("ReportJFrame.props");
			reportProps.set ( "HelpKey", "TSTool" );
			reportProps.set ( "TotalWidth", "750" );
			reportProps.set ( "TotalHeight", "550" );
			reportProps.set ( "Title", "Summary" );
			reportProps.set ( "DisplayFont", "Courier" );
			reportProps.set ( "DisplaySize", "11" );
			// reportProp.set ( "DisplayStyle", Font.PLAIN );
			reportProps.set ( "PrintFont", "Courier" );
			// reportProp.set ( "PrintFont", Font.PLAIN );
			reportProps.set ( "PrintSize", "7" );
			//reportProps.set ( "PageLength", "100" );
			reportProps.set ( "PageLength", "100000" );

			try {
				Vector summary = TSUtil.formatOutput ( tslist_output, sumprops );
				// Now display (the user can save as a file, etc.).
				new ReportJFrame ( summary, reportProps );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, routine, "Error printing summary." );
				Message.printWarning ( 2, routine, e );
			}
		}
		}
		catch ( Exception e ) {
			Message.printWarning ( 2, routine, e );
			message = "Error creating summary";
			Message.printWarning ( 1, routine, message );
			throw new IOException ( message );
		}
	}
	else if ( output_format == OUTPUT_TABLE ) {
		// A table output.  Just copy the graph code and change for table.  At some point,
        // need to initialize all the view data at the same time in case the user changes
        // views interactively after the initial view.
		// Temporary copy of data...
		Vector tslist = tslist_output;
		try {
		if ( IOUtil.isBatch() ) {
			Message.printWarning ( 1, routine, "Can only generate a table from GUI" );
			return;
		}

		PropList graphprops = new PropList ( "Table" );
		// Set graph properties for a simple graph...
		graphprops.set ( "ExtendedLegend", "true" );
		graphprops.set ( "HelpKey", "TSTool.TableMenu" );
		graphprops.set ( "DataUnits", ((TS)tslist.elementAt(0)).getDataUnits() );
		graphprops.set ( "YAxisLabelString", ((TS)tslist.elementAt(0)).getDataUnits() );
		if ( getOutputYearTypeInt() == _WATER_YEAR ) {
			graphprops.set ( "CalendarType", "WaterYear" );
		}
		else {
            graphprops.set ( "CalendarType", "CalendarYear" );
		}
		// Check the first time series.  If NWSCARD or DateValue, don't use comments for header...
		/* TODO SAM 2004-07-20 - try default header always -
			transition to HydroBase comments being only additional information
		if ( _non_co_detected ) {
			graphprops.set ( "UseCommentsForHeader", "false" );
		}
		else {
		    graphprops.set ( "UseCommentsForHeader", "true" );
		}
		*/
		// Set the total size of the graph window...
		graphprops.set ( "TotalWidth", "600" );
		graphprops.set ( "TotalHeight", "400" );

		// Default properties...
		graphprops.set("GraphType=Line");

		graphprops.set ( "InitialView", "Table" );
		// Summary properties for secondary displays (copy from summary output)...
		//graphprops.set ( "HelpKey", "TSTool.ExportMenu" );
		graphprops.set ( "TotalWidth", "600" );
		graphprops.set ( "TotalHeight", "400" );
		//graphprops.set ( "Title", "Summary" );
		graphprops.set ( "DisplayFont", "Courier" );
		graphprops.set ( "DisplaySize", "11" );
		graphprops.set ( "PrintFont", "Courier" );
		graphprops.set ( "PrintSize", "7" );
		graphprops.set ( "PageLength", "100" );
		TSViewJFrame view = new TSViewJFrame ( tslist, graphprops );
		addTSViewTSProductDMIs ( view );
		addTSViewTSProductAnnotationProviders ( view );
		// For garbage collection...
		view = null;
		tslist = null;
		}
		catch ( Exception e ) {
			Message.printWarning ( 2, routine, e );
			message = "Error creating table";
			Message.printWarning ( 1, routine, message );
			throw new IOException ( message );
		}
	}
	else if ( output_format == OUTPUT_YEAR_TO_DATE_REPORT ) {
		PropList reportProps = new PropList ("ReportJFrame.props");
		reportProps.set ( "HelpKey", "TSTool" );
		reportProps.set ( "TotalWidth", "750" );
		reportProps.set ( "TotalHeight", "550" );
		reportProps.set ( "Title", "Year to Date Report" );
		reportProps.set ( "DisplayFont", "Courier" );
		reportProps.set ( "DisplaySize", "11" );
		// reportProps.set ( "DisplayStyle", Font.PLAIN );
		reportProps.set ( "PrintFont", "Courier" );
		// reportProps.set ( "PrintFont", Font.PLAIN );
		reportProps.set ( "PrintSize", "7" );
		reportProps.set ( "PageLength", "5000" );
		reportProps.set ( "Search", "true" );

		try {
            Vector report = createYearToDateReport (tslist_output,__reference_date, null );
			new ReportJFrame ( report, reportProps );
		}
		catch ( Exception e ) {
			Message.printWarning ( 1, routine,"Error printing summary." );
			Message.printWarning ( 2, routine, e );
		}
	}
	else if (	(output_format == OUTPUT_ANNUAL_TRACES_GRAPH) ||
			(output_format == OUTPUT_BAR_GRAPH) ||
			(output_format == OUTPUT_DOUBLE_MASS_GRAPH) ||
			(output_format == OUTPUT_DURATION_GRAPH) ||
			(output_format == OUTPUT_LINEGRAPH) ||
			(output_format == OUTPUT_LINELOGYGRAPH) ||
			(output_format == OUTPUT_PERCENT_EXCEED_GRAPH) ||
			(output_format == OUTPUT_POINT_GRAPH) ||
			(output_format == OUTPUT_PORGRAPH) ||
			(output_format == OUTPUT_PredictedValue_GRAPH) ||
			(output_format == OUTPUT_PredictedValueResidual_GRAPH)||
			(output_format == OUTPUT_XY_SCATTER_GRAPH) ) {
		// A graph type.  Temporary copy of data...
		Vector tslist = tslist_output;
		try {
		if ( IOUtil.isBatch() ) {
			Message.printWarning ( 1, routine, "Can only graph from GUI" );
			return;
		}

		PropList graphprops = new PropList ( "Graph" );
		graphprops.set ( "ExtendedLegend", "true" );
		graphprops.set ( "HelpKey", "TSTool.GraphMenu" );
		graphprops.set ( "DataUnits", ((TS)tslist.elementAt(0)).getDataUnits() );
		graphprops.set ( "YAxisLabelString",((TS)tslist.elementAt(0)).getDataUnits() );
		if ( getOutputYearTypeInt() == _WATER_YEAR ) {
			graphprops.set ( "CalendarType", "WaterYear" );
		}
		else {
		    graphprops.set ( "CalendarType", "CalendarYear" );
		}
		// Check the first time series.  If NWSCARD or DateValue, don't
		// use comments for header...
		/* TODO SAM 2004-07-20 - try default header always -
			transition to HydroBase comments being only additional information
		if ( _non_co_detected ) {
			graphprops.set ( "UseCommentsForHeader", "false" );
		}
		else {	graphprops.set ( "UseCommentsForHeader", "true" );
		}
		*/
		// Set the total size of the graph window...
		graphprops.set ( "TotalWidth", "600" );
		graphprops.set ( "TotalHeight", "400" );

		if ( (tslist != null) && (output_format == OUTPUT_ANNUAL_TRACES_GRAPH) ) {
/* Currently disabled...
			// Go through each time series in the list and break
			// into annual traces, and then plot those results...
			Message.printStatus ( 1, routine, "Splitting time series into traces..." );
			int size = tslist.size();
			Vector new_tslist = new Vector ( size );
			Vector traces = null;
			DateTime reference_date = new DateTime ( DateTime.PRECISION_DAY );
			reference_date.setYear ( _reference_year );
			reference_date.setMonth ( 1 );
			reference_date.setDay ( 1 );
			size = tslist.size();
			for ( int i = 0; i < size; i++ ) {
				ts = (TS)tslist.elementAt(i);
				if ( ts == null ) {
					continue;
				}
				// Get the trace time series, using Jan 1 of the reference year.
				// This allows the raw data to temporally correct but is a pain to deal with in the
				// plot code...
				//traces = TSUtil.getTracesFromTS ( ts, reference_date, null );
				// Using this results in some funny labels but at least things plot...
				traces = TSUtil.getTracesFromTS ( ts, reference_date, reference_date);
				if ( traces == null ) {
					// If real-time data, add to the output...
					if ( ts.getDataIntervalBase() == TimeInterval.IRREGULAR ) {
						new_tslist.addElement ( ts );
					}
				}
				else {
				    // Add the traces to the tslist.  Set the legend explicitly because the
					// default is to print the period which is not useful...
					int traces_size = traces.size();
					Message.printStatus ( 1, routine, "Split " + ts.getIdentifier() + " into " + traces_size + " traces" );
					TS ts2 = null;
					for ( int j = 0; j < traces_size; j++ ){
						ts2 = (TS)traces.elementAt(j);
						ts2.setLegend ( "%D, %F" );
						new_tslist.addElement ( ts2 );
					}
				}
			}
			// Now replace the original list with the new one...
			tslist = new_tslist;
			// The offset only affects the graph.  All other data
			// are as if they were slices out of the original data...
			//graphprops.set ("ReferenceDate", reference_date.toString());
			graphprops.set ( "Title", "Annual Traces" );
			graphprops.set ( "XAxis.Format", "MM-DD" );
*/
		}
		else if ( output_format == OUTPUT_BAR_GRAPH ) {
			graphprops.set("GraphType=Bar");
			graphprops.set("BarPosition=" + parameters );
		}
		else if ( output_format == OUTPUT_DOUBLE_MASS_GRAPH ) {
			graphprops.set("GraphType=Double-Mass");
		}
		else if ( output_format == OUTPUT_DURATION_GRAPH ) {
			graphprops.set("GraphType=Duration");
		}
		else if ( output_format == OUTPUT_LINELOGYGRAPH ) {
			graphprops.set("YAxisType=Log");
			// Handle flags...
			/* TODO SAM 2006-05-22
			Can be very slow because blank labels are not ignored in low-level code.
			GRTS_Util.addDefaultPropertiesForDataFlags ( tslist, graphprops );
			*/
		}
		else if ( output_format == OUTPUT_PERCENT_EXCEED_GRAPH ) {
			graphprops.set("GraphType=PercentExceedance");
			graphprops.set("Title=Period Exceedance Curve");
			graphprops.set("XAxisLabelString=Percent of Time Exceeded");
		}
		else if ( output_format == OUTPUT_POINT_GRAPH ) {
			graphprops.set("GraphType=Point");
			// Handle flags...
			/* TODO SAM 2006-05-22
			Can be very slow because blank labels are not ignored in low-level code.
			GRTS_Util.addDefaultPropertiesForDataFlags ( tslist, graphprops );
			*/
		}
		else if ( output_format == OUTPUT_PORGRAPH ) {
			graphprops.set("GraphType=PeriodOfRecord");
			graphprops.set("LineWidth=Thick");
			graphprops.set("Title=Period of Record");
			graphprops.set("YAxisLabelString=Legend Index");
		}
		else if ( output_format == OUTPUT_PredictedValue_GRAPH ) {
			graphprops.set("GraphType=PredictedValue");
		}
		else if ( output_format == OUTPUT_PredictedValueResidual_GRAPH){
			graphprops.set("GraphType=PredictedValueResidual");
		}
		else if ( output_format == OUTPUT_XY_SCATTER_GRAPH ) {
			graphprops.set("GraphType=XY-Scatter");
		}
		else {	// Default properties...
			graphprops.set("GraphType=Line");
			// Handle flags...
			/* TODO SAM 2006-05-22
			Can be very slow because blank labels are not ignored in low-level code.
			GRTS_Util.addDefaultPropertiesForDataFlags ( tslist, graphprops );
			*/
		}

		// For now always use new graph...
		graphprops.set ( "InitialView", "Graph" );
		// Summary properties for secondary displays (copy from summary output)...
		//graphprops.set ( "HelpKey", "TSTool.ExportMenu" );
		graphprops.set ( "TotalWidth", "600" );
		graphprops.set ( "TotalHeight", "400" );
		//graphprops.set ( "Title", "Summary" );
		graphprops.set ( "DisplayFont", "Courier" );
		graphprops.set ( "DisplaySize", "11" );
		graphprops.set ( "PrintFont", "Courier" );
		graphprops.set ( "PrintSize", "7" );
		graphprops.set ( "PageLength", "100" );
		TSViewJFrame view = new TSViewJFrame ( tslist, graphprops );
		// Connect dynamic data objects...
		addTSViewTSProductDMIs ( view );
		addTSViewTSProductAnnotationProviders ( view );
		// For garbage collection...
		view = null;
		tslist = null;
		}
		catch ( Exception e ) {
			Message.printWarning ( 2, routine, e );
			message = "Error creating graph";
			Message.printWarning ( 1, routine, message );
			throw new IOException ( message );
		}
	}

	Message.printStatus ( 1, routine, "Ending time series output at:  " +
	new DateTime(DateTime.DATE_CURRENT).toString() );
}

/**
Process a time series action, meaning insert or update in the list.
@param ts_action INSERT_TS to insert at the position, UPDATE_TS to update at
the position, NONE to do to nothing.
@param ts Time series to act on.
@param ts_pos Position in the time series list to perform action.
@exception Exception if an error occurs.
*/
protected void processTimeSeriesAction ( int ts_action, TS ts, int ts_pos )
throws Exception
{	if ( ts_action == INSERT_TS ) {
		// Add new time series to the list...
		setTimeSeries ( ts, ts_pos );
		updateComments ( ts );
	}
	else if ( ts_action == UPDATE_TS ) {
		// Update in the time series list...
		setTimeSeries ( ts, ts_pos );
		updateComments ( ts );
	}
}

/**
Get a time series from the database/file using current date and units settings.
@param wl Warning level if the time series is not found.  Typically this will be 1 if
mimicing the old processing, and 2+ during transition to the new command status approach.
@param tsident_string Time series identifier.
@param readData indicate whether all the data should be read or false for only the header.
@return the time series.
@exception Exception if there is an error reading the time series.
*/
protected TS readTimeSeries ( int wl, String command_tag, String tsident_string, boolean readData )
throws Exception
{	return readTimeSeries ( wl, command_tag, tsident_string, false, readData );
}

/**
Read a time series from a database or file.  The following occur related to periods:
<ol>
<li>	If the query period has been specified, then it is used to limit the
	read/query.  Otherwise, the full period is retrieved.  Normally the
	query period is only specified to improve performance (e.g., to get
	a short period of real-time data or a short period from a long
	time series file).</li>
<li>	If the averaging period is specified, it will be used to compute
	averages.  Otherwise, the full period is used.</li>
<li>	If the output period is specified, then the time series that are read
	will be extended to the output period if necessary.  If the output
	period is within the returned data, the period is not changed (it is not
	shortened).</li>
</ol>
@param wl Warning level if the time series is not found.  Typically this will be 1 if
mimicing the old processing, and 2+ during transition to the new command status approach.
@param tsident_string Time series identifier for time series.
@param full_period If true, indicates that the full period is to be queried.
If false, the output period will be queried.
@param readData if true, read all the data.  If false, read only the header information.
SAMX - need to phase out "full_period".
@exception Exception if there is an error reading the time series.
*/
private TS readTimeSeries (	int wl, String command_tag, String tsident_string,
		boolean full_period, boolean readData )
throws Exception
{	TS	ts = null;
	String	routine = "TSEngine.readTimeSeries";
	
	// Figure out what dates to use for the query...

	DateTime query_date1 = null;	// Default is to query all data
	DateTime query_date2 = null;
	if ( haveInputPeriod() ) {
		// Use the query period...
		query_date1 = __InputStart_DateTime;
		query_date2 = __InputEnd_DateTime;
	}

	// Read the time series using the generic code.  The units are not specified.

    ts = readTimeSeries0 ( tsident_string, query_date1, query_date2, null, readData );

	if ( ts == null ) {
		if ( getIncludeMissingTS() && haveOutputPeriod() ) {
			// Even if time series is missing, create an empty one for output.
			ts = TSUtil.newTimeSeries ( tsident_string, true );
			// else leave null and ignore
			if ( ts != null ) {
				ts.setDate1 ( __OutputStart_DateTime );
				ts.setDate2 ( __OutputEnd_DateTime );
				// Leave original dates as is.  The following will fill with missing...
				if ( readData ) {
				    ts.allocateDataSpace();
				}
				Vector v = StringUtil.breakStringList (	tsident_string, "~", 0 );
				// Version without the input...
				String tsident_string2;
				tsident_string2 = (String)v.elementAt(0);
				ts.setIdentifier ( tsident_string2 );
				Message.printStatus ( 1, routine, "Created empty time series for \"" +
				tsident_string2 + "\" - not in DB and SetIncludeMissingTS(true) is specified." );
			}
		}
		else {
		    // Not able to query the time series and we are not supposed to create empty time series...
			String message = 
				"Null TS from read and not creating blank - unable to" +
				" process command:\n\""+ tsident_string +"\".\nYou must correct the command.  " +
				"Make sure that the data are in the database or\ninput file.";
			Message.printWarning ( wl,
			MessageUtil.formatMessageTag(command_tag,++_fatal_error_count), routine, message );
			getMissingTS().addElement(tsident_string);
			throw new TimeSeriesNotFoundException ( message );
		}
	}
	else {
        if ( Message.isDebugOn ) {
			Message.printDebug ( 1, routine, "Retrieved TS" );
		}
		// Now do the second set of processing on the time series (e.g.,
		// to guarantee a period that is at least as long as the output period.
		// TODO SAM - passing tsident_string2 causes problems - the input is lost...
		readTimeSeries2 ( ts, tsident_string, full_period );
	}
	return ts;
}

/**
Read a time series.  This method is called internally by TSEngine code and when
TSEngine serves as a TSSupplier when processing TSProducts.  It actually tries
to read a time series from a file or database.
@param tsident_string Time series identifier to read.
@param query_date1 First date to query.  If specified as null the entire period will be read.
@param query_date2 Last date to query.  If specified as null the entire period will be read.
@param units Requested units to return data.  If specified as null or an
empty string the units will not be converted.
@param read_data if true, the data will be read.  If false, only the time series header will be read.
@return the requested time series or null if an error.
@exception Exception if there is an error reading the time series.
*/
private TS readTimeSeries0 ( String tsident_string, DateTime query_date1, DateTime query_date2,
				String units, boolean read_data )
throws Exception
{	String routine = "TSEngine.readTimeSeries0";

	if ( Message.isDebugOn ) {
		Message.printDebug ( 10, routine, "Getting time series \"" + tsident_string + "\"" );
	}

	// Separate out the input from the TSID...

	Vector v = StringUtil.breakStringList ( tsident_string, "~", 0 );
	String tsident_string2;	// Version without the input...
	String input_type = null;
	String input_name = null;
    String input_name_full = null;  // input name with full path
	tsident_string2 = (String)v.elementAt(0);
	if ( v.size() == 2 ) {
		input_type = (String)v.elementAt(1);
	}
	else if ( v.size() == 3 ) {
		input_type = (String)v.elementAt(1);
		input_name = (String)v.elementAt(2);
        input_name_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(__ts_processor),input_name) );
	}

	// TSIdent uses only the first part of the identifier...
	// TODO SAM 2005-05-22 (why? to avoid confusing the following code?)

	TSIdent tsident = new TSIdent ( tsident_string2 );
	String source = tsident.getSource();

	// Now make a decision about which code to call to read the time
	// series.  Always check the new convention first.

	TS ts = null;
	if ( (input_type != null) && input_type.equalsIgnoreCase("DateValue") ) {
		// New style TSID~input_type~input_name
		try {
		    ts = DateValueTS.readTimeSeries ( tsident_string2,
				input_name_full, query_date1, query_date2, units, true );
		}
		catch ( Exception te ) {
			Message.printWarning ( 2, routine, te );
			ts = null;
		}
	}
	else if ( source.equalsIgnoreCase("DateValue") ) {
		// Old style (scenario may or may not be used to find the file)...
		try {
		    ts = DateValueTS.readTimeSeries ( tsident_string, query_date1, query_date2, units, true );
		}
		catch ( Exception e ) {
			Message.printWarning ( 2, routine, e );
			ts = null;
		}
	}
	/* TODO SAM Re-enable services code
	else if (	(input_type != null) &&
		input_type.equalsIgnoreCase(PersistenceType.DATE_VALUES.getTSIdentString()) ) {
		// Test new data services for DateValue file...
		try {	TSIdent tsident_full = new TSIdent ( tsident_string );
		        TSRequestTO tsRequestTO = new TSRequestTO ();// tsident_full 
		        TSIdentityTO tsIdentityTO = new TSIdentityTO();
		        tsIdentityTO.setPersistenceTypeStr(PersistenceType.DATE_VALUES.toString());
		        tsIdentityTO.setTsIdentString(tsident_full.toString());
		        tsRequestTO.setTsIdentityTO(tsIdentityTO);
				boolean GET_ALL_DATA = false;
                TimeSeries timeseries = new TimeSeriesFabricator().getDomainObject(tsRequestTO, GET_ALL_DATA );
				ts = timeseries.getTS();
		}
		catch ( Exception e ) {
			Message.printWarning ( 2, routine, e );
			ts = null;
		}
	}
*/
	else if ( (input_type != null) && input_type.equalsIgnoreCase("DIADvisor") ) {
		// New style TSID~input_type~input_name for DIADvisor...
		try {
		    ts = __DIADvisor_dmi.readTimeSeries ( tsident_string2, query_date1, query_date2, units, true );
		}
		catch ( Exception te ) {
			Message.printWarning ( 2, routine,
			"Error reading time series \"" + tsident_string2 + "\" from DIADvisor" );
			Message.printWarning ( 2, routine, te );
			Message.printWarning ( 2, routine, "Op:" +__DIADvisor_dmi.getLastSQLString() );
			Message.printWarning ( 2, routine, "Archive:" +	__DIADvisor_archive_dmi.getLastSQLString() );
			ts = null;
		}
		// For now, if the time series does not have data, set it to null...
		if ( ts != null ) {
			if ( !ts.hasData() ) {
				Message.printWarning ( 2, routine,
				"Time series \"" + tsident_string2 + "\" does not have data.  Treating as null." );
				ts = null;
			}
		}
	}
    else if ( source.equalsIgnoreCase("HEC-DSS") ) {
        // Old style (scenario may or may not be used to find the file)...
        try {
            ts = HecDssAPI.readTimeSeries ( tsident_string, query_date1, query_date2, units, true );
        }
        catch ( Exception e ) {
            Message.printWarning ( 2, routine, e );
            ts = null;
        }
    }
	else if ((input_type != null) && input_type.equalsIgnoreCase("HydroBase") ) {
		if ( Message.isDebugOn ) {
			Message.printDebug ( 10, routine, "Reading time series..." +
			tsident_string + "," + query_date1 + "," + query_date2);
		}
		try {
            HydroBaseDMI hbdmi = getHydroBaseDMI ( input_name );
			if ( hbdmi == null ) {
				Message.printWarning ( 2, routine, "Unable to get HydroBase connection for " +
				"input name \"" + input_name +	"\".  Unable to read time series." );
				ts = null;
			}
			else {
                ts = hbdmi.readTimeSeries ( tsident_string, query_date1, query_date2, units, true,
                    null );	// HydroBaseDMI read props Use defaults here
			}
			if ( Message.isDebugOn ) {
				Message.printStatus ( 10, routine, "...done reading time series." );
			}
		}
		catch ( Exception e ) {
			Message.printWarning ( 3, routine, "Error from HydroBaseDMI.readTimeSeries" );
			Message.printWarning ( 3, routine, e );
			ts = null;
		}
	}
	else if ((input_type != null) && input_type.equalsIgnoreCase("MexicoCSMN") ) {
		// New style TSID~input_type~input_name for MexicoCSMN...
		// Pass the front TSID as the first argument and the input_name file as the second...
		try {
            ts = MexicoCsmnTS.readTimeSeries ( tsident_string2,
				input_name_full, query_date1, query_date2, units, true );
		}
		catch ( Exception te ) {
			ts = null;
		}
	}
	else if ((input_type != null) && input_type.equalsIgnoreCase("MODSIM") ) {
		// New style TSID~input_type~input_name
		try {
            ts = ModsimTS.readTimeSeries ( tsident_string2,
				input_name_full, query_date1, query_date2, units, true );
		}
		catch ( Exception te ) {
			ts = null;
		}
	}
	else if ((input_type != null) && input_type.equalsIgnoreCase("NWSCARD") ) {
		// New style TSID~input_type~input_name for NWSCardTS...
		//Message.printStatus ( 1, routine, "Trying to read \"" +
		//		tsident_string2 + "\" \"" + input_name + "\"" );
		ts = NWSCardTS.readTimeSeries ( tsident_string2, input_name_full, query_date1, query_date2, units, true );
		//Message.printStatus ( 1, "", "SAMX TSEngine TS id = \"" +
			//ts.getIdentifier().toString(true) + "\"" );
	}
	else if ((input_type != null) && input_type.equalsIgnoreCase("NWSRFS_ESPTraceEnsemble") ) {
		// Binary ESP Trace Ensemble file...
		try {
            // For now read the file for each trace...
			// TODO SAM 2004-11-29 need to optimize so the file does not need to get reread...
			NWSRFS_ESPTraceEnsemble ensemble = new NWSRFS_ESPTraceEnsemble ( input_name_full, true );
			Vector tslist = ensemble.getTimeSeriesVector ();
			// Loop through and find a matching time series...
			int size = 0;
			boolean found = false;
			TS ts2 = null;
			ts = null;		// Value if not found.
			if ( tslist != null ) {
				size = tslist.size();
				for ( int i = 0; i < size; i++ ) {
					ts2 = (TS)tslist.elementAt(i);
					// This compares the sequence number but does not include the input
					// type/name since that was already used to read the file...
					if ( tsident.matches( ts2.getIdentifier().toString())){
						found = true;
						break;
					}
				}
			}
			if ( found ) {
				ts = ts2;
			}
		}
		catch ( Exception te ) {
			Message.printWarning ( 2, routine, "Error reading \"" +	tsident_string2 +
			"\" from ESP trace ensemble binary file." );
			Message.printWarning ( 2, routine, te );
			ts = null;
		}
	}
	else if ((input_type != null) && input_type.equalsIgnoreCase("NWSRFS_FS5Files") ) {
		NWSRFS_DMI nwsrfs_dmi = getNWSRFSFS5FilesDMI ( input_name_full, true );
		ts = nwsrfs_dmi.readTimeSeries ( tsident_string, query_date1, query_date2, units, true );
	}
	else if ((input_type != null) && input_type.equalsIgnoreCase("RiversideDB") ) {
		// New style TSID~input_type~input_name for RiversideDB...
        RiversideDB_DMI rdmi = getRiversideDB_DMI ( input_name );
        if ( rdmi == null ) {
            Message.printWarning ( 2, routine, "Unable to get RiversideDB connection for " +
            "input name \"" + input_name +  "\".  Unable to read time series." );
            ts = null;
        }
        else {
            try {
                ts = rdmi.readTimeSeries ( tsident_string2, query_date1, query_date2, units, true );
            }
			catch ( Exception te ) {
				Message.printWarning ( 2, routine,
				"Error reading time series \""+tsident_string2 + "\" from RiversideDB" );
				Message.printWarning ( 2, routine, te );
				ts = null;
			}
		}
		// Because there are other issues to resolve, for now, if the
		// time series does not have data, set it to null...
		if ( ts != null ) {
			if ( !ts.hasData() ) {
				Message.printWarning ( 2, routine,
				"Time series \"" + tsident_string2 + "\" does not have data.  Treating as null." );
				ts = null;
			}
		}
	}
	else if ((input_type != null) && input_type.equalsIgnoreCase("RiverWare") ) {
		// New style TSID~input_type~input_name for RiverWare...
		try {
            ts = RiverWareTS.readTimeSeries ( tsident_string2,
				input_name_full, query_date1, query_date2, units, true );
		}
		catch ( Exception te ) {
			ts = null;
		}
	}
	else if ((input_type != null) && input_type.equalsIgnoreCase("StateCU") ) {
		// New style TSID~input_type~input_name for StateCU...
		try {
            Vector ipy_types = StateCU_IrrigationPracticeTS.getTimeSeriesDataTypes(true, false);
            boolean is_ipy_type = false;
            int ipy_types_size = ipy_types.size();
            for ( int i_ipy = 0; i_ipy < ipy_types_size; i_ipy++ ) {
                if ( StringUtil.startsWithIgnoreCase(tsident.getType(), (String)ipy_types.elementAt(i_ipy))) {
                    is_ipy_type = true;
                    break;
                }
            }
            if ( is_ipy_type ) {
				ts = StateCU_IrrigationPracticeTS.readTimeSeries (
					tsident_string2, input_name_full, query_date1, query_date2, units, true );
			}
			else if(StringUtil.startsWithIgnoreCase(tsident.getType(), "CropArea-") &&
				!StringUtil.startsWithIgnoreCase(tsident.getType(), "CropArea-AllCrops") ) {
				// The second is in the StateCU_TS code.
				ts = StateCU_CropPatternTS.readTimeSeries (
					tsident_string2, input_name_full,query_date1, query_date2, units, true );
			}
			else {
                // Funnel through one class - the following will read StateCU output report and frost dates
				// input files...
				ts = StateCU_TS.readTimeSeries ( tsident_string2, input_name_full, query_date1, query_date2, units, true );
			}
		}
		catch ( Exception te ) {
			Message.printWarning ( 2, routine, "Error reading \"" + tsident_string2 + "\" from StateCU file." );
			Message.printWarning ( 2, routine, te );
			ts = null;
		}
	}
    else if ((input_type != null) && input_type.equalsIgnoreCase("StateCUB") ) {
        // New style TSID~input_type~input_name for StateCUB...
        try {
            ts = StateCU_BTS.readTimeSeries ( tsident_string2,
                input_name_full, query_date1, query_date2, units, true );
        }
        catch ( Exception te ) {
            Message.printWarning ( 2, routine, "Error reading \"" + tsident_string2 + "\" from StateCU binary file." );
            Message.printWarning ( 2, routine, te );
            ts = null;
        }
    }
	else if ((input_type != null) && input_type.equalsIgnoreCase("StateMod") ) {
		// New style TSID~input_type~input_name for StateMod...
		try {
            ts = StateMod_TS.readTimeSeries ( tsident_string2,
				input_name_full, query_date1, query_date2, units, true );
		}
		catch ( Exception te ) {
			Message.printWarning ( 2, routine, "Error reading \"" + tsident_string2 + "\" from StateMod file." );
			Message.printWarning ( 2, routine, te );
			ts = null;
		}
	}
	else if ((input_type != null) && input_type.equalsIgnoreCase("StateModB") ) {
		// New style TSID~input_type~input_name for StateModB...
		try {
            ts = StateMod_BTS.readTimeSeries ( tsident_string2,
				input_name_full, query_date1, query_date2, units, true );
		}
		catch ( Exception te ) {
			Message.printWarning ( 2, routine, "Error reading \"" + tsident_string2 + "\" from StateMod binary file." );
			Message.printWarning ( 2, routine, te );
			ts = null;
		}
	}
	else if ((input_type != null) &&
		input_type.equalsIgnoreCase("USGSNWIS") ) {
		// New style TSID~input_type
		try {
            ts = UsgsNwisTS.readTimeSeries ( tsident_string2, input_name, query_date1, query_date2, units, true );
		}
		catch ( Exception te ) {
			ts = null;
		}
	}
	return ts;
}

/**
Method for TSSupplier interface.
Read a time series given a time series identifier string.  The string may be
a file name if the time series are stored in files, or may be a true identifier
string if the time series is stored in a database.  The specified period is
read.  The data are converted to the requested units.
@param tsident_string Time series identifier or file name to read.
@param req_date1 First date to query.  If specified as null the entire period will be read.
@param req_date2 Last date to query.  If specified as null the entire period will be read.
@param req_units Requested units to return data.  If specified as null or an
empty string the units will not be converted.
@param read_data if true, the data will be read.  If false, only the time series header will be read.
@return Time series of appropriate type (e.g., MonthTS, HourTS).
@exception Exception if an error occurs during the read.
*/
public TS readTimeSeries (	String tsident_string,
				DateTime req_date1, DateTime req_date2,	String req_units, boolean read_data )
throws Exception
{	String routine = "TSEngine.readTimeSeries";
	Message.printStatus ( 1, routine,"Reading \"" + tsident_string + "\"" );

	// First look for time series in the in-memory list.  If called with
	// an alias, a match will be found in the aliases.  If called with
	// a TSID, a match will be found in the TSIDs.

	int size = 0;
	if ( __tslist != null ) {
		size = __tslist.size();
	}

	// If the tsident_string contains a ~, then use the full identifier with
	// input fields to search below.  Otherwise, just use the main part of the identifier...

	boolean full_tsid_check = false;
	if ( tsident_string.indexOf("~") >= 0 ) {
		full_tsid_check = true;
	}

	// If the tsident_string contains a [, then assume that the TSID is
	// actually a template TSID and a wildcard array is being used.  This
	// "trick" is used to avoid requiring another argument to the TSSupplier
	// interface, although an argument may be added at some point.

	// FIXME SAM 2007-06-21 This is a problem because TSIDs allow sequence numbers for ensembles.
	// TSIDs with sequence number have a [] at the end so do a
	// fix - if 3 "." are before the [], the assume the [] is for ensembles.
	boolean is_template = false;
	int pos = tsident_string.indexOf("[");
	int period_count = 0;
	for ( int i = 0; i < pos; i++ ) {
		if ( tsident_string.charAt(i) == '.' ) {
			++period_count;
		}
	}
	int array_index = -1;	// Indicates an array index of * (implement later).
	if ( (pos >= 0) && (period_count <3) ) {
		is_template = true;
		Message.printStatus ( 2, routine, "Requested TSID \"" + tsident_string + " is a template.");
		// Figure out the array position...
		String array_index_string = StringUtil.getToken ( tsident_string.substring(pos + 1), "]", 0, 0 );
		if ( array_index_string.equals("*") ) {
			array_index = -1;
		}
		else if ( StringUtil.isInteger(array_index_string) ) {
			array_index = StringUtil.atoi(array_index_string);
		}
		else {
            // Invalid...
			Message.printWarning ( 2, routine, "TSID \"" + tsident_string + "\" array index is invalid" );
			throw new Exception ( "TSID \"" + tsident_string + "\" array index is invalid" );
		}
		// Strip off the array information because it will confuse the following code...
		tsident_string = tsident_string.substring(0,pos);
	}

	if ( size != 0 ) {
		TS ts = null;
		TSIdent tsident = null;

		//  First try the aliases (not supported for templates)...

		if ( !is_template ) {
			for ( int i = 0; i < size; i++ ) {
				ts = (TS)__tslist.elementAt(i);
				if ( ts == null ) {
					continue;
				}
				if ( ts.getAlias().equalsIgnoreCase( tsident_string) ) {
					Message.printStatus ( 2, routine,"Matched alias." );
					return ts;
				}
			}
		}

		// Now try the TSIDs, including the input fields if necessary...

		int match_count = 0;
		for ( int i = 0; i < size; i++ ) {
			ts = (TS)__tslist.elementAt(i);
			if ( ts == null ) {
				continue;
			}
			tsident = ts.getIdentifier();
			if ( Message.isDebugOn ) {
				Message.printDebug ( 2, routine, "Checking tsid \"" + tsident.toString(true) + "\"" );
			}
			if ( is_template ) {
				if ( tsident.matches(tsident_string, true, full_tsid_check)){
					Message.printStatus ( 2, routine, "Matched TSID for template." );
					// See if this is the one we want - both are zero initial value...
					if ( (array_index < 0) || (match_count == array_index) ) {
						// TODO - need way to track * matches to not return the same TS each match
						return ts;
					}
					// Else increment...
					++match_count;
				}
			}
			else {	// Check the identifier strings using full identifiers.
				// The TSID being requested controls the level of comparison.
				// If it has the input fields, then they will be checked.
				if ( tsident.equals(tsident_string,full_tsid_check) ) {
					Message.printStatus ( 1, routine,"Matched TSID using TSID with input fields." );
					return ts;
				}
			}
		}
	}

	if ( is_template ) {
		// If a matching time series was not found, there has been
		// an error because there is no way to easily read a list of
		// time series with wildcards and get the list back in the order
		// that is expected by the template.  The calling code should
		// handle the exception and use a null time series if needed.
		Message.printWarning ( 2, routine,
		"TSID \"" + tsident_string + "\" could not be matched." );
		throw new Exception ( "TSID \"" + tsident_string + "\" could not be matched." );
	}
	else {
	    Message.printStatus ( 2, routine,
			"TSID \"" + tsident_string + "\" could not be matched in memory.  Trying to read using TSID." );
	}

	// If not found, try reading from a persistent source.  If called with
	// an alias, this will fail.  If called with a TSID, this should succeed...

	TS ts = readTimeSeries0 ( tsident_string, req_date1, req_date2,	req_units, read_data );
	if ( ts == null ) {
		Message.printStatus ( 2, routine,
				"TSID \"" + tsident_string + "\" could not read using TSID.  Not able to provide." );
	}
	else {
		Message.printStatus ( 2, routine, "TSID \"" + tsident_string + "\" successfully read using TSID." );
	}
	return ts;
}

/**
Method for TSSupplier interface.  Read a time series given an existing time series and a file name.
The specified period is read.  The data are converted to the requested units.
@param req_ts Requested time series to fill.  If null, return a new time series.
If not null, all data are reset, except for the identifier, which is assumed
to have been set in the calling code.  This can be used to query a single
time series from a file that contains multiple time series.
@param fname File name to read.
@param date1 First date to query.  If specified as null the entire period will be read.
@param date2 Last date to query.  If specified as null the entire period will be read.
@param req_units Requested units to return data.  If specified as null or an
empty string the units will not be converted.
@param read_data if true, the data will be read.  If false, only the time series header will be read.
@return Time series of appropriate type (e.g., MonthTS, HourTS).
@exception Exception if an error occurs during the read.
*/
public TS readTimeSeries (	TS req_ts, String fname, DateTime date1, DateTime date2,
				String req_units, boolean read_data )
throws Exception
{	return null;
}

/**
Method for TSSupplier interface.
Read a time series list from a file (this is typically used used where a time
series file can contain one or more time series).
The specified period is read.  The data are converted to the requested units.
@param fname File to read.
@param date1 First date to query.  If specified as null the entire period will be read.
@param date2 Last date to query.  If specified as null the entire period will be read.
@param req_units Requested units to return data.  If specified as null or an
empty string the units will not be converted.
@param read_data if true, the data will be read.  If false, only the time series
header will be read.
@return Vector of time series of appropriate type (e.g., MonthTS, HourTS).
@exception Exception if an error occurs during the read.
*/
public Vector readTimeSeriesList ( String fname, DateTime date1, DateTime date2,
					String req_units, boolean read_data )
throws Exception
{	return null;
}

/**
Method for TSSupplier interface.
Read a time series list from a file or database using the time series identifier
information as a query pattern.  The specified period is
read.  The data are converted to the requested units.
@param tsident A TSIdent instance that indicates which time series to query.
If the identifier parts are empty, they will be ignored in the selection.  If
set to "*", then any time series identifier matching the field will be selected.
If set to a literal string, the identifier field must match exactly to be selected.
@param fname File to read.
@param date1 First date to query.  If specified as null the entire period will be read.
@param date2 Last date to query.  If specified as null the entire period will be read.
@param req_units Requested units to return data.  If specified as null or an
empty string the units will not be converted.
@param read_data if true, the data will be read.  If false, only the time series
header will be read.
@return Vector of time series of appropriate type (e.g., MonthTS, HourTS).
@exception Exception if an error occurs during the read.
*/
public Vector readTimeSeriesList ( TSIdent tsident, String fname, DateTime date1, DateTime date2,
					String req_units, boolean read_data )
throws Exception {
	return null;
}

/**
Perform common actions on time series after reading.  This method should be called after:
<ol>
<li>	calling readTimeSeries()</li>
<li>	bulk reads are done indepent of readTimeSeries() (e.g., when calling
	readDateValue() directly to read multiple time series</li>
<li>	single reads are done independent of readTimeSeries() (e.g., when
	calling readUsgsNwis() directly to read a single time series</li>
</ol>
This method does the following:
<ol>
<li>	Sets the legend to "" (this unsets legend information that was
	previously required with the old graph package).</li>
<li>	If the description is not set, sets it to the location.</li>
<li>	If a missing data range has been set, indicate it to the time series.
	This may be phased out.</li>
<li>	If the time series identifier needs to be reset to something known to
	the read code, reset it (using the non-null tsident_string parameter
	that is passed in).</li>
<li>	Compute the historic averages for the raw data so that it is available
	later for filling.</li>
<li>	If the output period is specified, make sure that the time series
	period includes the output period.  For important time series, the
	available period may already include the output period.  For time series
	that are being filled, it is likely that the available period will need
	to be extended to include the output period.</li>
</ol>
@param ts Time series to process.
@param tsident_string Time series identifier string.  If null, take from the time series.
@param full_period If true, indicates that the full period is to be queried.
If false, the output period will be queried.
@exception Exception if there is an error processing the time series.
*/
private void readTimeSeries2 ( TS ts, String tsident_string, boolean full_period )
throws Exception
{	String routine = "TSEngine.readTimeSeries2";
	if ( ts == null ) {
		return;
	}

	// Do not want to use the abbreviated legend that was required with the old graph package...

	ts.setLegend ( "" );

	// If no description, set to the location...

	if ( ts.getDescription().length() == 0 ) {
		ts.setDescription ( ts.getLocation() );
	}

	// Compute the historical average here rather than having to put this code
	// in each clause in the processTimeSeriesCommands() method.

	try {
        ts.setDataLimitsOriginal (calculateTSAverageLimits(ts));
	}
	catch ( Exception e ) {
		Message.printWarning ( 2, routine,
		"Error getting original data limits for \"" + ts.getIdentifierString() + "\""  );
		Message.printWarning ( 3, routine, e );
	}

	// To ensure that new and old time series identifiers can be used, reset
	// the identifier in the queried string to that which was specified in the input commands.

	if ( tsident_string != null ) {
		ts.setIdentifier ( tsident_string );
	}

	// If the output period has been specified, make sure that the time
	// series has a period at least that long.  This will allow for data	
	// filling and other manipulation.  Do not change the interval if the
	// time series has irregular data or if the auto extend feature has been turned off.
	//
	// Check by getting the maximum overlapping period of the time series
	// and output period.  Then if the max period start is before the
	// time series start or the max period end is after the time series end
	// a change interval is needed...

	if ( haveOutputPeriod() && getAutoExtendPeriod() ) {
		Vector v = new Vector ( 2 );
		TSLimits limits = new TSLimits();
		limits.setDate1 ( ts.getDate1() );
		limits.setDate2 ( ts.getDate2() );
		v.addElement ( limits );
		limits = new TSLimits();
		limits.setDate1 ( __OutputStart_DateTime );
		limits.setDate2 ( __OutputEnd_DateTime );
		v.addElement ( limits );
		try {
            limits = TSUtil.getPeriodFromLimits( v, TSUtil.MAX_POR);
			if ( limits.getDate1().lessThan(ts.getDate1()) || limits.getDate2().greaterThan(ts.getDate2()) ) {
				ts.changePeriodOfRecord ( limits.getDate1(), limits.getDate2() );
			}
		}
		catch ( Exception e ) {
			String message = "Unable to extend period for \"" +
			ts.getIdentifierString() + "\" to output period.";
			Message.printWarning ( 2, routine, message );
			throw new Exception ( message );
		}
	}
}

/**
Call readTimeSeries2() for every time series in the Vector, with the
full_period parameter having a value of true.  This version is called by read commands.
@param tslist Vector of TS to process.
@exception Exception if there is an error processing the time series.
*/
protected void readTimeSeries2 ( Vector tslist )
throws Exception
{	readTimeSeries2 ( tslist, true );
}

/**
Call readTimeSeries2() for every time series in the Vector.
@param tslist Vector of TS to process.
@param full_period If true, indicates that the full period is to be queried.
If false, the output period will be queried.
@exception Exception if there is an error processing the time series.
*/
private void readTimeSeries2 ( Vector tslist, boolean full_period )
throws Exception
{	int size = 0;
	if ( tslist != null ) {
		size = tslist.size();
	}
	TS ts = null;
	for ( int i = 0; i < size; i++ ) {
		ts = (TS)tslist.elementAt(i);
		if ( ts == null ) {
			continue;
		}
		readTimeSeries2 ( ts, null, full_period );
	}
	ts = null;
}

/**
Remove all time series in the results list.
*/
protected void removeAllTimeSeries ()
{
    __tslist.removeAllElements();
}

/**
Remove the time series at the specified index.
@param index Index of the time series to remove.
*/
protected void removeTimeSeries ( int index )
{
    __tslist.removeElementAt ( index );
}

/**
Set the value of the AutoExtendPeriod property.  If true, the period for time series
will automatically be extended to the output period at read.
@param AutoExtendPeriod_boolean Value of property.
*/
protected void setAutoExtendPeriod ( boolean AutoExtendPeriod_boolean )
{
    __AutoExtendPeriod_boolean = AutoExtendPeriod_boolean;
}

/**
Set the average period end.
@param end Average period end.
*/
protected void setAverageEnd ( DateTime end )
{   __AverageEnd_DateTime = end;
}

/**
Set the average period start.
@param start Average period start.
*/
protected void setAverageStart ( DateTime start )
{   __AverageStart_DateTime = start;
}

/**
Set the list of DataTest, for example, when set with TSCommandsProcessor.
@param DataTest_Vector List of data test definitions, as Vector of DataTest.
*/
protected void setDataTestList ( Vector DataTest_Vector )
{	
	__datatestlist = (Vector)DataTest_Vector;
}

/**
Set a HydroBaseDMI instance in the Vector that is being maintained for use.
The input name in the DMI is used to lookup the instance.  If a match is found,
the old instance is optionally closed and the new instance is set in the same
location.  If a match is not found, the new instance is added at the end.
@param hbdmi HydroBaseDMI to add to the list.  Null will be ignored.
@param close_old If an old DMI instance is matched, close the DMI instance if
true.  The main issue is that if something else is using the DMI instance (e.g.,
the TSTool GUI) it may be necessary to leave the old instance open.
*/
protected void setHydroBaseDMI ( HydroBaseDMI hbdmi, boolean close_old )
{	if ( hbdmi == null ) {
		return;
	}
	int size = __hbdmi_Vector.size();
	HydroBaseDMI hbdmi2 = null;
	String input_name = hbdmi.getInputName();
	for ( int i = 0; i < size; i++ ) {
		hbdmi2 = (HydroBaseDMI)__hbdmi_Vector.elementAt(i);
		if ( hbdmi2.getInputName().equalsIgnoreCase(input_name)){
			// The input name of the current instance matches that of the instance in the Vector.
			// Replace the instance in the Vector by the new instance...
			if ( close_old ) {
				try {	hbdmi2.close();
				}
				catch ( Exception e ) {
					// Probably can ignore.
				}
			}
			__hbdmi_Vector.setElementAt ( hbdmi, i );
			return;
		}
	}
	// Add a new instance to the Vector...
	__hbdmi_Vector.addElement ( hbdmi );
}

/**
Set the list of HydroBaseDMI (e.g., when manipulated by an openHydroBase() command.
@param dmilist Vector of HydroBaseDMI.
*/
protected void setHydroBaseDMIList ( Vector dmilist )
{	__hbdmi_Vector = dmilist;
}

/**
Set the value of the IgnoreLEZero property.
@param IgnoreLEZero_boolean Value of property.
*/
protected void setIgnoreLEZero ( boolean IgnoreLEZero_boolean )
{
    __IgnoreLEZero_boolean = IgnoreLEZero_boolean;
}

/**
Set the value of the IncludeMissingTS property.
@param IncludeMissingTS_boolean Value of property.
*/
protected void setIncludeMissingTS ( boolean IncludeMissingTS_boolean )
{
    __IncludeMissingTS_boolean = IncludeMissingTS_boolean;
}

/**
Set the input period end.
@param end Input period end.
*/
protected void setInputEnd ( DateTime end )
{	__InputEnd_DateTime = end;
}

/**
Set the input period start.
@param start Input period start.
*/
protected void setInputStart ( DateTime start )
{	__InputStart_DateTime = start;
}

/**
Set an NDFD Adapter instance in the Vector that is being maintained for use.
The input name for the adapter is used to lookup the instance.  If a match is
found, the old instance is optionally closed and the new instance is set in the
same location.  If a match is not found, the new instance is added at the end.
@param adapter NDFD Adapter to add to the list.  Null will be ignored.
@param close_old If an old adapter is matched, remove the instance if true.
This is mainly for memory management since NDFD Adapters do not currently keep
a connection open.
*/

//TODO KAT 2006-10-30 Need to enable NDFD
//Commenting this out for now to get it to compile
/**
private void setNDFDAdapter ( Adapter adapter, boolean close_old )
{	if ( adapter == null ) {
		return;
	}
	int size = __NDFDAdapter_Vector.size();
	Adapter adapter2 = null;
	//TODO SAM 2006-07-15
	NDFD Adapters do not have names/identfiers so put in the first slot
	String input_name = adapter.getIdentifier();
	for ( int i = 0; i < size; i++ ) {
		adapter2 = (Adapter)__NDFDAdapter_Vector.elementAt(i);
		if ( adapter2.getIdentifier().equalsIgnoreCase(input_name)){
			// The identifier of the current instance matches that of the instance in the Vector.
			// Replace the instance in the Vector by the new instance...
			if ( close_old ) {
				/ * TODO SAM 2006-07-13
				Probably not needed - evaluate for later
				try {
				    hbdmi2.close();
				}
				catch ( Exception e ) {
					// Probably can ignore.
				}
				* /
			}
			__NDFDAdapter_Vector.setElementAt ( adapter, i );
			return;
		}
	}
	// Add a new instance to the Vector...
	__NDFDAdapter_Vector.addElement ( adapter );
	
	if ( __NDFDAdapter_Vector.size() == 0 ) {
		// Add as the first item...
		__NDFDAdapter_Vector.addElement ( adapter );
	}
	else {
	    __NDFDAdapter_Vector.setElementAt ( adapter, 0 );
	}
}

*/

/**
Set a NWSRFS_DMI (NWSRFS FS5Files DMI) instance in the Vector that is being maintained for use.
The input name in the DMI is used to lookup the instance.  If a match is found,
the old instance is optionally closed and the new instance is set in the same
location.  If a match is not found, the new instance is added at the end.
@param nwsrfs_dmi NWSRFS_DMI to add to the list.  Null will be ignored.
@param close_old If an old DMI instance is matched, close the DMI instance if
true.  The main issue is that if something else is using the DMI instance (e.g.,
the TSTool GUI) it may be necessary to leave the old instance open.
*/
protected void setNWSRFSFS5FilesDMI ( NWSRFS_DMI nwsrfs_dmi, boolean close_old )
{	if ( nwsrfs_dmi == null ) {
		return;
	}
	int size = __nwsrfs_dmi_Vector.size();
	NWSRFS_DMI nwsrfs_dmi2 = null;
	String input_name = nwsrfs_dmi.getInputName();
	for ( int i = 0; i < size; i++ ) {
		nwsrfs_dmi2 = (NWSRFS_DMI)__nwsrfs_dmi_Vector.elementAt(i);
		if ( nwsrfs_dmi2.getInputName().equalsIgnoreCase(input_name)){
			// The input name of the current instance matches that of the instance in the Vector.
			// Replace the instance in the Vector by the new instance...
			if ( close_old ) {
				try {
				    nwsrfs_dmi2.close();
				}
				catch ( Exception e ) {
					// Probably can ignore.
				}
			}
			__nwsrfs_dmi_Vector.setElementAt ( nwsrfs_dmi, i );
			return;
		}
	}
	// Add a new instance to the Vector...
	__nwsrfs_dmi_Vector.addElement ( nwsrfs_dmi );
}

/**
Set the output period end.
@param end Output period end.
*/
protected void setOutputEnd ( DateTime end )
{	__OutputEnd_DateTime = end;
}

/**
Set the output period start.
@param start Output period start.
*/
protected void setOutputStart ( DateTime start )
{	__OutputStart_DateTime = start;
}

// TODO SAM 2007-11-16 Convert to enumeration when moved to Java 1.5.
/**
Set the output year type.
@param OutputYearType_int Output year type as an integer.
*/
protected void setOutputYearType ( int OutputYearType_int )
{   __OutputYearType_int = OutputYearType_int;
}

/**
Set the value of the PreviewExportedOutput property.
@param PreviewExportedOutput_boolean Value of property.
*/
private void setPreviewExportedOutput ( boolean PreviewExportedOutput_boolean )
{
    __PreviewExportedOutput_boolean = PreviewExportedOutput_boolean;
}

/**
Set a RiversideDB_DMI instance in the Vector that is being maintained for use.
The input name in the DMI is used to lookup the instance.  If a match is found,
the old instance is optionally closed and the new instance is set in the same
location.  If a match is not found, the new instance is added at the end.
@param hbdmi RiversideDB_DMI to add to the list.  Null will be ignored.
@param close_old If an old DMI instance is matched, close the DMI instance if
true.  The main issue is that if something else is using the DMI instance (e.g.,
the TSTool GUI) it may be necessary to leave the old instance open.
*/
protected void setRiversideDB_DMI ( RiversideDB_DMI rdmi, boolean close_old )
{   String routine = "TSEngine.setRiversideDB_DMI";
    if ( rdmi == null ) {
        return;
    }
    int size = __rdmi_Vector.size();
    RiversideDB_DMI rdmi2 = null;
    String input_name = rdmi.getInputName();
    Message.printStatus(2, routine, "Saving RiversideDB_DMI connection \"" + input_name + "\"" );
    for ( int i = 0; i < size; i++ ) {
        rdmi2 = (RiversideDB_DMI)__rdmi_Vector.elementAt(i);
        if ( rdmi2.getInputName().equalsIgnoreCase(input_name)){
            // The input name of the current instance matches that of the instance in the Vector.
            // Replace the instance in the Vector by the new instance...
            if ( close_old ) {
                try {
                    rdmi2.close();
                }
                catch ( Exception e ) {
                    // Probably can ignore.
                }
            }
            __rdmi_Vector.setElementAt ( rdmi, i );
            return;
        }
    }

    // Add a new instance to the Vector...
    __rdmi_Vector.addElement ( rdmi );
}

/**
Set the time series in either the __tslist vector.
@param id Identifier for time series (alias or TSIdent string).
@exception Exception if there is an error saving the time series.
*/
protected void setTimeSeries ( String id, TS ts )
throws Exception
{	int position = indexOf ( id );
	setTimeSeries ( ts, position );
}

/**
Set the time series in either the __tslist vector.
@param ts time series to set.
@param position Position in time series list (0 index).
@exception Exception if there is an error saving the time series.
*/
protected void setTimeSeries ( TS ts, int position )
throws Exception
{	String routine = "TSEngine.setTimeSeries";

	if ( ts == null ) {
		Message.printStatus ( 2, routine, "Setting null time series at position " + (position + 1) +
                " (internal [" + position + "])");
	}
	else {
        Message.printStatus ( 2, routine,
		"Setting time series \"" + ts.getIdentifierString() + "\" at position " + (position + 1) +
        " (internal [" + position + "])");
	}
	if ( position < 0 ) {
		return;
	}

    if ( __tslist == null ) {
		// Create a new Vector.
		__tslist = new Vector ( 50, 50 );
	}
	// Position is zero index...
	if ( position >= __tslist.size() ) {
		// Append to the list.  Fill in intervening positions with null references...
		for ( int i = __tslist.size(); i <= position; i++ ) {
			__tslist.addElement ( (TS)null );
		}
	}
	// Now update at the requested position...
	__tslist.removeElementAt ( position );
	__tslist.insertElementAt ( ts, position );
}

/**
Set the time series list, for example, when being processed through
TSCommandsProcessor.
@param tslist List of time series results, as Vector of TS.
*/
protected void setTimeSeriesList ( Vector tslist )
{	__tslist = tslist;
}

/**
Update the comments in the time series in case some information has changed
(currently units and description only).  If this is not called, then looking at
a summary may show the wrong information.  This method is needed because the
comments are often used in the header and comments are generated from HydroBase.
Currently only the units are updated.  THIS METHOD IS HIGHLY
DEPENDENT ON THE SPECIFIC TIME SERIES COMMENTS USED WITH CDSS.
@param ts Time series to update.
*/
private void updateComments ( TS ts )
{	if ( ts == null ) {
        return;
    }
    Vector comments = ts.getComments();
	int size = 0;
	if ( comments != null ) {
		size = comments.size();
	}
	String comment = null;
	int pos = 0;
	for ( int i = 0; i < size; i++ ) {
		comment = (String)comments.elementAt(i);
		if ( comment.regionMatches(true,0,"Data units",0,10) ) {
			pos = comment.indexOf ( "=" );
			comments.setElementAt( comment.substring(0,pos + 2) + ts.getDataUnits(), i);
		}
		else if ( comment.regionMatches(true,0,"Description",0,11) ) {
			pos = comment.indexOf ( "=" );
			comments.setElementAt( comment.substring(0,pos + 2) + ts.getDescription(), i);
		}
	}
	comments = null;
	comment = null;
}

/**
Write a StateMod time series file given the current time series.  This can be
called using both the in-memory list of time series or the list to be output.
Other than the time series list and the filename, all other parameters are taken
from settings previously set.
@param tslist Vector of time series to write.
@param output_file Name of file to write.
@param precision_string If "*", then use the default rules that have been in
place for some time.  If an integer, use as the precision parameter for
StateMod.writePersistent().
@param comments Comments to include at the top of the StateMod file, consisting
of the commands as text and database version information.
*/
private void writeStateModTS ( Vector tslist, String output_file, String precision_string, String[] comments )
{	String routine = "TSEngine.writeStateModTS";
	// Type of calendar for output...
	String calendar = "";
	if ( getOutputYearTypeInt() == _WATER_YEAR ) {
		calendar = "WaterYear";
	}
	else {
	    calendar = "CalendarYear";
	}
	// Set the precision default precision for output (-2 generally works OK)...
	int	precision = -2;
	if ( !precision_string.equals("*") && !precision_string.equals("") &&
		StringUtil.isInteger(precision_string) ) {
		// Use the specified precision because the user has specified an integer precision...
		precision = StringUtil.atoi(precision_string);
	}
	else {
        // Precision is determined from units and possibly data type...
		// Default, get the precision from the units of the first time series...
	    int	list_size = 0;
		TS	tspt = null;
		if ( (tslist != null) && (tslist.size() > 0) ) {
			tspt = (TS)tslist.elementAt(0);
			list_size = tslist.size();
		}
		if ( tspt != null ) {
			String units = tspt.getDataUnits();
			Message.printStatus ( 2, "", "Data units are " + units );
			DataFormat outputformat = DataUnits.getOutputFormat(units,10);
			if ( outputformat != null ) {
				precision = outputformat.getPrecision();
				if ( precision > 0 ) {
					// Change to negative so output code will handle overflow...
					precision *= -1;
				}
			}
			outputformat = null;
			Message.printStatus ( 1, "", "Precision from units output format *-1 is " +	precision);
		}
		// Old code that we still need to support...
		// In year 2, we changed the precision to 0 for RSTO.  See if any of the TS in the list are RSTO...
		for ( int ilist = 0; ilist < list_size; ilist++ ) {
			tspt = (TS)tslist.elementAt(ilist);
			if ( tspt == null ) {
				continue;
			}
			if ( tspt.getIdentifier().getType().equalsIgnoreCase( "RSTO") ) {
				precision = 0;
				break;
			}
		}
	}

	if ( TSUtil.intervalsMatch ( tslist )) {
		// The time series to write have the same interval so write using the first interval....
		int interval = 0;
		interval = ((TS)tslist.elementAt(0)).getDataIntervalBase();
		if((interval == TimeInterval.DAY) ||(interval == TimeInterval.MONTH) ) {
			PropList smprops = new PropList ( "StateMod" );
			// Don't set input file since it is null...
			smprops.set ( "OutputFile", output_file );
			if ( comments != null ) {
				smprops.setUsingObject ( "NewComments", (Object)comments );
			}
			if ( __OutputStart_DateTime != null ) {
				smprops.set("OutputStart=" + __OutputStart_DateTime.toString());
			}
			if ( __OutputEnd_DateTime != null ) {
				smprops.set ( "OutputEnd=" + __OutputEnd_DateTime.toString());
			}
			double missing = -999.0;
			smprops.set ( "CalendarType", calendar );
			smprops.set ( "MissingDataValue", "" + missing );
			smprops.set ( "OutputPrecision", "" + precision );
			try {
                StateMod_TS.writeTimeSeriesList ( tslist,smprops );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, routine, "Unable to write StateMod file \"" + output_file + "\"" );
			}
		}
		else {
            Message.printWarning ( 1, routine, "Unable to write StateMod output for interval " + interval );
		}
		comments = null;
	}
	else {
        Message.printWarning ( 1, routine, "Unable to write StateMod time series of different intervals." );
	}
}

public void windowActivated ( WindowEvent e )
{
}

/**
TODO SAM 2004-07-22 - not needed since _graph_list was removed?
*/
public void windowClosed ( WindowEvent e )
{	
}

/**
*/
public void windowClosing ( WindowEvent e )
{
}

public void windowDeactivated ( WindowEvent e )
{
}

public void windowDeiconified ( WindowEvent e )
{
}

public void windowIconified ( WindowEvent e )
{
}

public void windowOpened ( WindowEvent e )
{
}

} // End of TSEngine
