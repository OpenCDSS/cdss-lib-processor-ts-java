
package org.cuahsi.waterml._1;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


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
 *         &lt;element name="siteInfo" type="{http://www.cuahsi.org/waterML/1.0/}SiteInfoType"/>
 *         &lt;element name="seriesCatalog" type="{http://www.cuahsi.org/waterML/1.0/}seriesCatalogType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{http://www.cuahsi.org/waterML/1.0/}extension" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "siteInfo",
    "seriesCatalog",
    "extension"
})
@XmlRootElement(name = "site")
public class Site {

    @XmlElement(required = true)
    protected SiteInfoType siteInfo;
    protected List<SeriesCatalogType> seriesCatalog;
    protected Object extension;

    /**
     * Gets the value of the siteInfo property.
     * 
     * @return
     *     possible object is
     *     {@link SiteInfoType }
     *     
     */
    public SiteInfoType getSiteInfo() {
        return siteInfo;
    }

    /**
     * Sets the value of the siteInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link SiteInfoType }
     *     
     */
    public void setSiteInfo(SiteInfoType value) {
        this.siteInfo = value;
    }

    /**
     * Gets the value of the seriesCatalog property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the seriesCatalog property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSeriesCatalog().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SeriesCatalogType }
     * 
     * 
     */
    public List<SeriesCatalogType> getSeriesCatalog() {
        if (seriesCatalog == null) {
            seriesCatalog = new ArrayList<SeriesCatalogType>();
        }
        return this.seriesCatalog;
    }

    /**
     * In order to simplify comprehension, data sources are encouraged to put additional informaiton in the extension area, using thier own namespace. Clients need not understand information in extension element
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getExtension() {
        return extension;
    }

    /**
     * Sets the value of the extension property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setExtension(Object value) {
        this.extension = value;
    }

}
