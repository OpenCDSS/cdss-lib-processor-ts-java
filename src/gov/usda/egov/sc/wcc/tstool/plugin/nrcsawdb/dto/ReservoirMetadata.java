
package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Class to store reservoir metadata.
 * <pre>
   "reservoirMetadata": {
      "capacity": 153000,
      "elevationAtCapacity": 350,
      "usableCapacity": 148640
    },
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReservoirMetadata {

	// Alphabetize the items.

    /**
     * Capacity (ACFT).
     */
    private Double capacity = null;

    /**
     * Elevation at capacity (FT).
     */
    private Double elevationAtCapacity = null;

    /**
     * USable capacity (ACFT).
     */
    private Double usableCapacity = null;

    /**
     * Constructor.
     */
    public ReservoirMetadata () {
    }

    /**
     * Gets the capacity.
     * @return the capacity
     */
    public Double getCapacity() {
        return this.capacity;
    }

    /**
     * Gets the elevation at capacity.
     * @return the elevation at capacity
     */
    public Double getElevationAtCapacity() {
        return this.elevationAtCapacity;
    }

    /**
     * Gets the usable capacity.
     * @return the usable capacity
     */
    public Double getUsableCapacity() {
        return this.usableCapacity;
    }

    /**
     * Set the capacity.
     * @param capcity capacity
     */
    public void setCapacity ( Double capacity ) {
        this.capacity = capacity;
    }

    /**
     * Set the elevation at capacity.
     * @param elevationAtCapcity elevation at capacity
     */
    public void setElevationAtCapacity ( Double elevationAtCapacity ) {
        this.elevationAtCapacity = elevationAtCapacity;
    }

    /**
     * Set the usable capacity.
     * @param usableCapcity usable capacity
     */
    public void setUsableCapacity ( Double usableCapacity ) {
        this.usableCapacity = usableCapacity;
    }

}