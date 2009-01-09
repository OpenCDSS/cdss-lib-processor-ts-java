package hec.heclib.dss;

import java.util.Vector;

//  hec imports
// FIXME SAM 2009-01-07 Avoid wildcard because can't see specific dependencies
//import hec.heclib.util.*;
import hec.heclib.util.HecTime;
import hec.heclib.util.stringContainer;
import hec.heclib.util.Heclib;
import java.io.Serializable;
//FIXME SAM 2009-01-07 Avoid wildcard because can't see specific dependencies
//import java.util.*;
import java.util.LinkedList;
import java.util.Arrays;


public class xCondensedReference implements Serializable
{

		protected String nominalPathname=null;;
		protected String[] pathnameList = null;

		public xCondensedReference()
		{}

		public xCondensedReference(String nominalPath)
		{
			nominalPathname = nominalPath;
		}

			public void setPathnameArray(String[] paths, int number)
		{
			if (pathnameList == null) {
				pathnameList = java.util.Arrays.copyOf(paths, number);
			}
			else {
				//  This is pretty rare, paths must be kept in order
				LinkedList list = new LinkedList(Arrays.asList(pathnameList));
				String p[] = java.util.Arrays.copyOf(paths, number);
				LinkedList l = new LinkedList(Arrays.asList(p));
				list.addAll(l);
				pathnameList = (String[])list.toArray(new String[0]);
			}
		}


		public int size()
		{
			if (pathnameList == null)
				return 0;

			return pathnameList.length;
		}

		public void complete(HecDataManager dataManager, String[] paths, int number, boolean querryTimes)
		{
			if (number > 0)
				setPathnameArray(paths, number);

			if (pathnameList == null)
				return;

			String first = getFirstPathname();
			String last = getLastPathname();
			nominalPathname = first;

			//  Determine the location of the slash following the "D" part
			int partPositions[] = new int[7];
			int stat = DSSPathname.parsePathname(first, partPositions);
			if (stat != 0) DSSPathname.parsePathname("///////", partPositions);

			//  Does this look like this might be a time series pathname?
			//  The D part must be 9 characters long and the E part must be > 3
			if (((partPositions[4] - partPositions[3]) == 10) &&
				((partPositions[5] - partPositions[4]) > 3)){

			  HecTime start = new HecTime();
			  HecTime end = new HecTime();
			  if (querryTimes) {
				  getDataTimes(dataManager, start, end);
			  }

			  if ( (size() == 1) && (!start.isDefined())) {
				return;
			  }

			  if (start.isDefined() && end.isDefined()) {
				nominalPathname = first.substring(0, (partPositions[3])) +
					start.date(4) + " - " + end.date(4) +
					first.substring((partPositions[4] - 1), partPositions[6]);
			  }
			  else {
				//  Build a nominal path with the D part containing the
				//  date of the first and last pathname
				nominalPathname = first.substring(0, (partPositions[4] - 1)) + " - " +
					last.substring(partPositions[3], partPositions[6]);
			  }
			}

		}
		public String getNominalPathname()
		{
			if (nominalPathname == null)
				return "";

			return nominalPathname;
		}

		public String getFirstPathname()
		{
			if (pathnameList == null)
				return "";
			return pathnameList[0];
		}

		public String getLastPathname()
		{
			if (pathnameList == null)
				return "";
			return pathnameList[pathnameList.length - 1];
		}

		public String getPathname(int number){
			if (pathnameList == null)
				return "";
			return pathnameList[number];
		}

		public boolean removePathname(int element)
		{
			if (pathnameList == null)
				return false;
			if (element >= pathnameList.length)
				return false;
			if (pathnameList.length == 1) {
				pathnameList = null;
				return true;
			}
			String newList[] = new String[pathnameList.length-1];
			int count = 0;
			for (int i=0; i<pathnameList.length; i++) {
				if (i != element) {
					newList[count++] = pathnameList[i];
				}
			}
			pathnameList = newList;
			return true;
		}

		public String toString()
		{
			return getNominalPathname();
		}


		public void getPathnameTimes (Object dataManager, HecTime start, HecTime end)
		{
			start.setUndefined();
			end.setUndefined();
			if (size() == 0)
				return;

			String path = pathnameList[0];
			if (path == null)
				return;

			int partPositions[] = new int[7];
			int stat = DSSPathname.parsePathname(path, partPositions);
			if (stat != 0)
				return;
			start.set (path.substring(partPositions[3],(partPositions[4]-1)), "0001");
			int interval = HecTimeSeries.getIntervalFromEPart(path.substring(partPositions[4],(partPositions[5]-1)));
			if (interval > 0)
				start.adjustToIntervalOffset(interval, 0);

			if (dataManager != null) {
				if (dataManager instanceof HecDataManager) {
					path = ((HecDataManager)dataManager).nextTimeSeriesPathname(getLastPathname());
				}
				else if (dataManager instanceof HecDataManagerRemote) {
					path = ((HecDataManagerRemote)dataManager).nextTimeSeriesPathname(getLastPathname());
				}
			}
			if ((path != null) && (path.length() > 10) && ((partPositions[4]-1) > partPositions[3])) {
				//  Don't have to re-parse path, since all parts same length
				end.set (path.substring(partPositions[3], (partPositions[4]-1)), "0000");
				end.cleanTime();
				if (interval > 0)
					end.adjustToIntervalOffset(interval, 0);
			}
		}

		public void getDataTimes (HecDataManager dataManager, HecTime start, HecTime end)
		{
			start.setUndefined();
			end.setUndefined();
			if (size() == 0)
				return;

			String path = pathnameList[0];
			if (path == null)
				return;

			stringContainer units = new stringContainer();
			stringContainer type = new stringContainer();
			if (dataManager != null) {
				// FIXME SAM 2009-01-07 Different parameters than code that was emailed?
			    //dataManager.getTSRecordInfo(path, start, end, units, type);
			    dataManager.getTSRecordInfo( start, end, units, type);
				if (size() > 1) {
				  path = getLastPathname();
				  HecTime temp = new HecTime();
				  // FIXME SAM 2009-01-07 Different parameters than code that was emailed?
				  //dataManager.getTSRecordInfo(path, temp, end, units, type);
				  dataManager.getTSRecordInfo(temp, end, units, type);
				}
			  }
		  }

		  public int generatePathnameList(HecDataManager dataManager, boolean checkPathnames)
		  {
			  pathnameList = generatePathnameList(dataManager, getNominalPathname(), checkPathnames);
			  if(pathnameList == null) {
				  return -1;
			  }
			  return 0;
		  }

		  protected String[] generatePathnameList(HecDataManager dataManager, String condensedPath,
											 boolean checkPathnames)
		{
			if(condensedPath == null)
				return null;

			Vector pathList = new Vector();

			HecTime start = new HecTime();
			HecTime end = new HecTime();
			int interval=0;

			DSSPathname path = new DSSPathname(condensedPath);
			//  Be sure that this is a condensed data set
			if (path.dPart().length() != 21)
				return null;
			start.set(path.dPart().substring(0,9), "2400");
			if (!start.isDefined())
				return null;
			end.set(path.dPart().substring(12,21), "2400");
			if (!end.isDefined())
				return null;

			String ePart = path.ePart();
			if(ePart.length() == 0)
				return null;

			int nvals[] = new int[1];
			int intl[] = new int[1];
			int istat[] = new int[1];
			istat[0] = 1;
			Heclib.zgintl(intl, ePart, nvals, istat);
			if(istat[0] == -1)
				return null;
			if(istat[0] == 0)
				interval = intl[0];
			//  If irregular interval, set at 0
			if(istat[0] == 1)
				interval = 0;

			int jul[] = new int[1];
			int iblock[] = new int[1];
			int iyr[] = new int[1];
			int imon[] = new int[1];
			int iday[] = new int[1];

			//  Get the DSS block, year, month, and day
			jul[0] = start.julian();
			int stat = getBlockStartDate(dataManager, interval, ePart, jul[0], iblock, iyr, imon, iday);
			if(stat != 0)
				return null;
			start.setYearMonthDay(iyr[0], imon[0], iday[0], 1440);

			while (true) {
				path.setDPart(start.date(104));
				String pathname = path.pathname();
				if (checkPathnames) {
					if (dataManager.recordExists(pathname))
						pathList.add(path.pathname());
				}
				else {
					pathList.add(path.pathname());
				}
				//  Increment to the next block
				Heclib.zincbk(iblock, jul, iyr, imon, iday);
				start.setJulian(jul[0], 1, 0);
				if(start.greaterThan(end))
					break;
			}
			return (String[])pathList.toArray(new String[0]);
		}

		protected int getBlockStartDate(HecDataManager dataManager, int interval, String ePart, int jul,
										int block[], int year[], int month[], int day[])
		{
			if(interval > 0) {
				//  Get the file version for zbegdt
				int ver;
				ver = dataManager.zinqir("FVER");
				//  Determine the block used by DSS
				Heclib.zbegdt(jul, interval, year, month, day, block, ver);
			}
			else {
				return dataManager.getIrregBeginningDate(ePart, jul, year, month, day, block);
			}
			return 0;
		}

	}
