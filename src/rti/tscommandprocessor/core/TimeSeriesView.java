// TimeSeriesView - Interface for time series views, which are collections of time series data.

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

package rti.tscommandprocessor.core;

/**
Interface for time series views, which are collections of time series data.
An example of a view is the TimeSeriesTreeView, which lists time series in a hierarchical tree, suitable for
drill-down viewing.
*/
public interface TimeSeriesView
{
    /**
     * Get the view identifier.
     */
    public String getViewID ();
    
    /**
     * Set the view identifier.
     */
    public void setViewID ( String viewID );
}
