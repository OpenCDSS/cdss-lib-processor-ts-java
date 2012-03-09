
package org.cuahsi.waterml._1;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SampleMediumEnum.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="SampleMediumEnum">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Surface Water"/>
 *     &lt;enumeration value="Ground Water"/>
 *     &lt;enumeration value="Sediment"/>
 *     &lt;enumeration value="Soil"/>
 *     &lt;enumeration value="Air"/>
 *     &lt;enumeration value="Tissue"/>
 *     &lt;enumeration value="Precipitation"/>
 *     &lt;enumeration value="Unknown"/>
 *     &lt;enumeration value="Other"/>
 *     &lt;enumeration value="Snow"/>
 *     &lt;enumeration value="Not Relevant"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "SampleMediumEnum")
@XmlEnum
public enum SampleMediumEnum {

    @XmlEnumValue("Surface Water")
    SURFACE_WATER("Surface Water"),
    @XmlEnumValue("Ground Water")
    GROUND_WATER("Ground Water"),
    @XmlEnumValue("Sediment")
    SEDIMENT("Sediment"),
    @XmlEnumValue("Soil")
    SOIL("Soil"),
    @XmlEnumValue("Air")
    AIR("Air"),
    @XmlEnumValue("Tissue")
    TISSUE("Tissue"),
    @XmlEnumValue("Precipitation")
    PRECIPITATION("Precipitation"),
    @XmlEnumValue("Unknown")
    UNKNOWN("Unknown"),
    @XmlEnumValue("Other")
    OTHER("Other"),
    @XmlEnumValue("Snow")
    SNOW("Snow"),
    @XmlEnumValue("Not Relevant")
    NOT_RELEVANT("Not Relevant");
    private final String value;

    SampleMediumEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static SampleMediumEnum fromValue(String v) {
        for (SampleMediumEnum c: SampleMediumEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
