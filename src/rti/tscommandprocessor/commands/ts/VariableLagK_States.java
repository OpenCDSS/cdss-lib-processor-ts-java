// VariableLagK_States - VariableLagK states, used to serialize/deserialize JSON strings using GSON.

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

/**
 * VariableLagK states, used to serialize/deserialize JSON strings using GSON.
 * @author sam
 *
 */
public class VariableLagK_States {

	/**
	 * Lag interval (interval of input time series corresponding to states), to allow data check.
	 */
	private String lagInterval = "";
	/**
	 * Data units for states, consistent with inflow and outflow time series, to allow data check.
	 */
	private String units = "";
	/**
	 * Lagged inflow at the state save date/time.
	 */
	private Double currentLaggedInflow = 0.0;
	/**
	 * Outflow at the state save date/time.
	 */
	private Double currentOutflow = 0.0;
	/**
	 * Storage at the state save date/time.
	 */
	private Double currentStorage = 0.0;
	/**
	 * Array of flow values from the QT lagged inflow array, where last value is at save date/time.
	 */
	private double [] qtLag;
	
	/**
	Default constructor, required by GSON.
	*/
	public VariableLagK_States () {
	}
	
	/**
	 * Get the current lagged inflow.
	 * @return the current lagged inflow.
	 */
	public Double getCurrentLaggedInflow() {
		return this.currentLaggedInflow;
	}
	
	/**
	 * Get the current outflow
	 * @return the current outflow
	 */
	public Double getCurrentOutflow() {
		return this.currentOutflow;
	}
	
	/**
	 * Get the current storage
	 * @return the current storage
	 */
	public Double getCurrentStorage() {
		return this.currentStorage;
	}
	
	/**
	 * Get the lag interval, should match the original input time series
	 * @return the lag interval
	 */
	public String getLagInterval() {
		return this.lagInterval;
	}
	
	/**
	 * Get the QTLag array, where the last value corresponds to the state save date/time
	 * @return the QTLag array
	 */
	public double [] getQtLag() {
		return this.qtLag;
	}
	
	/**
	 * Get the units for the flow values.
	 * @return the units for the flow values
	 */
	public String getUnits() {
		return this.units;
	}
	
	/**
	 * Set the current lagged inflow.
	 * @param the current lagged inflow.
	 */
	public void setCurrentLaggedInflow ( Double currentLaggedInflow ) {
		this.currentLaggedInflow = currentLaggedInflow;
	}
	
	/**
	 * Set the current outflow
	 * @param the current outflow
	 */
	public void setCurrentOutflow ( Double currentOutflow ) {
		this.currentOutflow = currentOutflow;
	}
	
	/**
	 * Set the current storage
	 * @param the current storage
	 */
	public void setCurrentStorage ( Double setCurrentStorage ) {
		this.currentStorage = setCurrentStorage;
	}
	
	/**
	 * Set the lag interval, should match the original input time series
	 * @param the lag interval
	 */
	public void setLagInterval ( String lagInterval ) {
		this.lagInterval = lagInterval;
	}
	
	/**
	 * Set the QTLag array, where the last value corresponds to the state save date/time.
	 * @param the QTLag array
	 */
	public void setQtLag ( double [] qtLag ) {
		this.qtLag = qtLag;
	}
	
	/**
	 * Set the units for the flow values.
	 * @param the units for the flow values
	 */
	public String setUnits ( String units ) {
		return this.units = units;
	}
}
