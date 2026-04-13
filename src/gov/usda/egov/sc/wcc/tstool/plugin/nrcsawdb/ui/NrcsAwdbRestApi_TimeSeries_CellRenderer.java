// NrcsAwdbRestApiTimeSeries_CellRenderer - renderer for the time series catalog table model

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2025 Colorado Department of Natural Resources

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

package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.ui;

import RTi.Util.GUI.JWorksheet_AbstractExcelCellRenderer;

/**
This class is used to render cells for NrcsAwdbRestApi_TimeSeries_TableModel data.
*/
@SuppressWarnings("serial")
public class NrcsAwdbRestApi_TimeSeries_CellRenderer extends JWorksheet_AbstractExcelCellRenderer {

	private NrcsAwdbRestApi_TimeSeries_TableModel tableModel = null;

	/**
	Constructor.
	@param tableModel The NrcsAwdbRestApi_TimeSeries_TableModel to render.
	*/
	public NrcsAwdbRestApi_TimeSeries_CellRenderer ( NrcsAwdbRestApi_TimeSeries_TableModel tableModel ) {
		this.tableModel = tableModel;
	}

	/**
	Returns the format for a given column.
	@param column the column for which to return the format.
	@return the column format as used by StringUtil.formatString().
	*/
	public String getFormat(int column) {
		return this.tableModel.getFormat(column);	
	}

	/**
	Returns the widths of the columns in the table.
	@return an integer array of the widths of the columns in the table.
	*/
	public int[] getColumnWidths() {
		return this.tableModel.getColumnWidths();
	}

}