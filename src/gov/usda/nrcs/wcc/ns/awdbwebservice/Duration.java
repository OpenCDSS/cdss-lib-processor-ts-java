
package gov.usda.nrcs.wcc.ns.awdbwebservice;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for duration.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="duration">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="DAILY"/>
 *     &lt;enumeration value="MONTHLY"/>
 *     &lt;enumeration value="SEMIMONTHLY"/>
 *     &lt;enumeration value="INSTANTANEOUS"/>
 *     &lt;enumeration value="ANNUAL"/>
 *     &lt;enumeration value="SEASONAL"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "duration")
@XmlEnum
public enum Duration {

    DAILY,
    MONTHLY,
    SEMIMONTHLY,
    INSTANTANEOUS,
    ANNUAL,
    SEASONAL;

    public String value() {
        return name();
    }

    public static Duration fromValue(String v) {
        return valueOf(v);
    }

}
