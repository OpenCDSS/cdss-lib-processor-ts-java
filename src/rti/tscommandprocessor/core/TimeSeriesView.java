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