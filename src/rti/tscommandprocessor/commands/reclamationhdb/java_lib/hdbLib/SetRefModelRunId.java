//package java_lib.hdbLib;
// Provided by Dave King, Reclamation
package rti.tscommandprocessor.commands.reclamationhdb.java_lib.hdbLib;

import java.sql.Connection;
import java.sql.SQLException;

import rti.tscommandprocessor.commands.reclamationhdb.java_lib.dmiLib.ModelDateTime;

// this object and methods establish ref model run id

// import classes stored in libraries


/**
* A class of objects and methods to establish ref model run id
*/

public class SetRefModelRunId
{
	public boolean IdIsValid;
	public int model_run_id;
	private int user_mrid;
	private int model_id;
	private String model_run_name;
	private ModelDateTime startDate;
	private ModelDateTime endDate;
	private String hydroIndicator;

	public SetRefModelRunId(Connection con, String userID, int umrid, int mid,
		String mrunname, String hydInd, ModelDateTime sDate, ModelDateTime eDate)
	{
		user_mrid = umrid;
		model_id = mid;
		model_run_name = mrunname;
		startDate = sDate;
		endDate = eDate;
		hydroIndicator = hydInd;

		try
		{
			// verify model id

			if (VerifyRecords.verifyHdbModel(con, model_id))
			{
				// determine if we are using user model run id or are going to make one up

				if (user_mrid < 1 && ! userID.equals("app_user"))
				{
					// make one up - get maximum existing model run id and add 1

					model_run_id = HdbDmiUtils.getMaxModelRun(con) + 1;
					System.out.println("New model run id is " + model_run_id);
					if(HdbDmiUtils.createRefModelRunRPC(con, model_id, model_run_id, model_run_name,
							startDate, endDate, hydroIndicator))
					{
						IdIsValid = true;
					}
					else
					{
						IdIsValid = false;
					}
				}
				else
				{
					// recycle user's model run id if it is valid

					if (VerifyRecords.verifyRefModelRun(con, user_mrid))
					{

						model_run_id = user_mrid;
						System.out.println("Model run id " + model_run_id + " is valid");
						if(HdbDmiUtils.updateRefModelRunRPC(con, model_run_id, model_run_name,
								startDate, endDate, hydroIndicator))
						{
							IdIsValid = true;
						}
						else
						{
							IdIsValid = false;
						}
					}
   		       		else
					{
						System.out.println("Model run id " + user_mrid + " is invalid");
						System.out.println("I am going to make one up.");
						model_run_id = HdbDmiUtils.getMaxModelRun(con) + 1;
						System.out.println("New model run id is " + model_run_id);
						if(HdbDmiUtils.createRefModelRunRPC(con, model_id, model_run_id, model_run_name,
								startDate, endDate, hydroIndicator))
						{
							IdIsValid = true;
						}
						else
						{
							IdIsValid = false;
						}
					}
				}
			}
			else
			{
				System.out.println("Model id " + model_id + " is invalid");
				IdIsValid = false;
			}
		}
		catch (SQLException e)
		{
			IdIsValid = false;
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			IdIsValid = false;
		}
	}
}  // end class
