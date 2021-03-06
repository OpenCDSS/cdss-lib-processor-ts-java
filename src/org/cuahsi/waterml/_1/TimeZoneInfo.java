
package org.cuahsi.waterml._1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="defaultTimeZone" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attGroup ref="{http://www.cuahsi.org/waterML/1.0/}timeZoneAttr"/>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="daylightSavingsTimeZone" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attGroup ref="{http://www.cuahsi.org/waterML/1.0/}timeZoneAttr"/>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="siteUsesDaylightSavingsTime" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "defaultTimeZone",
    "daylightSavingsTimeZone"
})
@XmlRootElement(name = "timeZoneInfo")
public class TimeZoneInfo {

    protected TimeZoneInfo.DefaultTimeZone defaultTimeZone;
    protected TimeZoneInfo.DaylightSavingsTimeZone daylightSavingsTimeZone;
    @XmlAttribute
    protected Boolean siteUsesDaylightSavingsTime;

    /**
     * Gets the value of the defaultTimeZone property.
     * 
     * @return
     *     possible object is
     *     {@link TimeZoneInfo.DefaultTimeZone }
     *     
     */
    public TimeZoneInfo.DefaultTimeZone getDefaultTimeZone() {
        return defaultTimeZone;
    }

    /**
     * Sets the value of the defaultTimeZone property.
     * 
     * @param value
     *     allowed object is
     *     {@link TimeZoneInfo.DefaultTimeZone }
     *     
     */
    public void setDefaultTimeZone(TimeZoneInfo.DefaultTimeZone value) {
        this.defaultTimeZone = value;
    }

    /**
     * Gets the value of the daylightSavingsTimeZone property.
     * 
     * @return
     *     possible object is
     *     {@link TimeZoneInfo.DaylightSavingsTimeZone }
     *     
     */
    public TimeZoneInfo.DaylightSavingsTimeZone getDaylightSavingsTimeZone() {
        return daylightSavingsTimeZone;
    }

    /**
     * Sets the value of the daylightSavingsTimeZone property.
     * 
     * @param value
     *     allowed object is
     *     {@link TimeZoneInfo.DaylightSavingsTimeZone }
     *     
     */
    public void setDaylightSavingsTimeZone(TimeZoneInfo.DaylightSavingsTimeZone value) {
        this.daylightSavingsTimeZone = value;
    }

    /**
     * Gets the value of the siteUsesDaylightSavingsTime property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isSiteUsesDaylightSavingsTime() {
        if (siteUsesDaylightSavingsTime == null) {
            return false;
        } else {
            return siteUsesDaylightSavingsTime;
        }
    }

    /**
     * Sets the value of the siteUsesDaylightSavingsTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setSiteUsesDaylightSavingsTime(Boolean value) {
        this.siteUsesDaylightSavingsTime = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attGroup ref="{http://www.cuahsi.org/waterML/1.0/}timeZoneAttr"/>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class DaylightSavingsTimeZone {

        @XmlAttribute(name = "ZoneAbbreviation")
        @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
        @XmlSchemaType(name = "normalizedString")
        protected String zoneAbbreviation;
        @XmlAttribute(name = "ZoneOffset", required = true)
        protected String zoneOffset;

        /**
         * Gets the value of the zoneAbbreviation property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getZoneAbbreviation() {
            return zoneAbbreviation;
        }

        /**
         * Sets the value of the zoneAbbreviation property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setZoneAbbreviation(String value) {
            this.zoneAbbreviation = value;
        }

        /**
         * Gets the value of the zoneOffset property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getZoneOffset() {
            return zoneOffset;
        }

        /**
         * Sets the value of the zoneOffset property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setZoneOffset(String value) {
            this.zoneOffset = value;
        }

    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attGroup ref="{http://www.cuahsi.org/waterML/1.0/}timeZoneAttr"/>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class DefaultTimeZone {

        @XmlAttribute(name = "ZoneAbbreviation")
        @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
        @XmlSchemaType(name = "normalizedString")
        protected String zoneAbbreviation;
        @XmlAttribute(name = "ZoneOffset", required = true)
        protected String zoneOffset;

        /**
         * Gets the value of the zoneAbbreviation property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getZoneAbbreviation() {
            return zoneAbbreviation;
        }

        /**
         * Sets the value of the zoneAbbreviation property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setZoneAbbreviation(String value) {
            this.zoneAbbreviation = value;
        }

        /**
         * Gets the value of the zoneOffset property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getZoneOffset() {
            return zoneOffset;
        }

        /**
         * Sets the value of the zoneOffset property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setZoneOffset(String value) {
            this.zoneOffset = value;
        }

    }

}
