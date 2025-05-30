
package gov.usda.nrcs.wcc.ns.awdbwebservice;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

// Java 8.
//import com.sun.xml.internal.ws.client.BindingProviderProperties;
// Java 11:
// - ChatGPT says that the following will work:  com.sun.xml.ws.developer.JAXWSProperties
import com.sun.xml.ws.client.BindingProviderProperties;

/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.6 in JDK 6
 * Generated source version: 2.1
 * 
 */
//@WebServiceClient(name = "AwdbWebService", targetNamespace = "https://wcc.sc.egov.usda.gov/awdbWebService", wsdlLocation = "https://wcc.sc.egov.usda.gov/awdbWebService/services?WSDL")
//@WebServiceClient(name = "AwdbWebService", targetNamespace = "http://www.wcc.nrcs.usda.gov/ns/awdbWebService", wsdlLocation = "https://www.wcc.nrcs.usda.gov/awdbWebService/services?WSDL")
@WebServiceClient(name = "AwdbWebService", targetNamespace = "http://www.wcc.nrcs.usda.gov/ns/awdbWebService", wsdlLocation = "https://wcc.sc.egov.usda.gov/awdbWebService/services?WSDL")
public class AwdbWebService_Service
    extends Service
{

    private final static URL AWDBWEBSERVICE_WSDL_LOCATION;
    private final static Logger logger = Logger.getLogger(gov.usda.nrcs.wcc.ns.awdbwebservice.AwdbWebService_Service.class.getName());

    //static String wsdlURL = "https://www.wcc.nrcs.usda.gov/awdbWebService/services?WSDL";
    static String wsdlURL = "https://wcc.sc.egov.usda.gov/awdbWebService/services?WSDL";
    static {
        URL url = null;
        try {
            URL baseUrl;
            baseUrl = gov.usda.nrcs.wcc.ns.awdbwebservice.AwdbWebService_Service.class.getResource(".");
            url = new URL(baseUrl, wsdlURL );
        } catch (MalformedURLException e) {
            logger.warning("Failed to create URL for the wsdl Location: '" + wsdlURL + "', retrying as a local file");
            logger.warning(e.getMessage());
        }
        AWDBWEBSERVICE_WSDL_LOCATION = url;
    }
         
    /**
    Construct a SOAP object to allow API interaction.
    This version is called when constructing a datastore.
    @param wsdlLocation the WSDL location for the AwdbWebService web service
    @param connectTimeout milliseconds for connection timeout, or -1 to keep default
    @param requestTimeout milliseconds for receive timeout, or -1 to keep default
    @throws MalformedURLException
    */
    public AwdbWebService_Service(String wsdlLocation, int connectTimeout, int requestTimeout )
    throws MalformedURLException
    {
        super(new URL(wsdlLocation), new QName("http://www.wcc.nrcs.usda.gov/ns/awdbWebService", "AwdbWebService"));
    	//super(new URL(wsdlLocation), new QName("https://www.wcc.nrcs.usda.gov/ns/awdbWebService", "AwdbWebService"));
        //super(new URL(wsdlLocation), new QName("https://wcc.sc.nrcs.usda.gov/ns/awdbWebService", "AwdbWebService"));
        //super(new URL(wsdlLocation), new QName("https://wcc.sc.egov.usda.gov/awdbWebService", "AwdbWebService"));
        //String routine = "AdwdWebService_Service";
        // Set the timeout for web service requests for this web service instance - impacts all calls
        // First get the web service port
        AwdbWebService ws = getAwdbWebServiceImplPort();
        // Debug - figure out what properties are set for the request context
        //Map<String,Object> map = ((BindingProvider)ws).getRequestContext();
        //for ( Map.Entry<String, Object> entry: map.entrySet() ) {
        //	Message.printStatus(2, routine, entry.getKey() + "=\"" + entry.getValue() + "\"");
        //}
        //Message.printStatus(2,routine,"CONNECT_TIMEOUT="+BindingProviderProperties.CONNECT_TIMEOUT);
        //Message.printStatus(2,routine,"REQUEST_TIMEOUT="+BindingProviderProperties.REQUEST_TIMEOUT);
        if ( connectTimeout >= 0 ) {
            // Set the connection timeout so web service call does not hang
        	// CONNECT_TIMEOUT = com.sun.xml.internal.ws.connect.timeout
        	// NOT the following...
        	// javax.xml.ws.client.connectionTimeout
        	// com.sun.xml.ws.connect.timeout
        	//Message.printStatus(2,routine,"Setting NRCS AWDB datastore connectTimeout=" + connectTimeout +
        	//	" (initial value=" + ((BindingProvider)ws).getRequestContext().get(BindingProviderProperties.CONNECT_TIMEOUT) + ").");
        	((BindingProvider)ws).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, "" + connectTimeout );
        }
        if ( requestTimeout >= 0 ) {
        	// Set the receive timeout so web service call does not hang
        	// REQUEST_TIMEOUT = com.sun.xml.internal.ws.request.timeout
        	// NOT the following...
        	// javax.xml.ws.client.receiveTimeout
        	// com.sun.xml.ws.request.timeout
        	//Message.printStatus(2,routine,"Setting NRCS AWDB datastore requestTimeout=" + requestTimeout +
        	//	" (initial value=" + ((BindingProvider)ws).getRequestContext().get(BindingProviderProperties.REQUEST_TIMEOUT) + ").");
        	((BindingProvider)ws).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, "" + requestTimeout );
        }
    }   
    
    public AwdbWebService_Service(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public AwdbWebService_Service() {
        super(AWDBWEBSERVICE_WSDL_LOCATION, new QName("http://www.wcc.nrcs.usda.gov/ns/awdbWebService", "AwdbWebService"));
        //super(AWDBWEBSERVICE_WSDL_LOCATION, new QName("https://www.wcc.nrcs.usda.gov/ns/awdbWebService", "AwdbWebService"));
        //super(AWDBWEBSERVICE_WSDL_LOCATION, new QName("https://wcc.sc.egov.usda.gov/awdbWebService", "AwdbWebService"));
    }

    /**
     * 
     * @return
     *     returns AwdbWebService
     */
    @WebEndpoint(name = "AwdbWebServiceImplPort")
    public AwdbWebService getAwdbWebServiceImplPort() {
        return super.getPort(new QName("http://www.wcc.nrcs.usda.gov/ns/awdbWebService", "AwdbWebServiceImplPort"), AwdbWebService.class);
        //return super.getPort(new QName("https://www.wcc.nrcs.usda.gov/ns/awdbWebService", "AwdbWebServiceImplPort"), AwdbWebService.class);
        //return super.getPort(new QName("https://wcc.sc.egov.usda.gov/awdbWebService", "AwdbWebServiceImplPort"), AwdbWebService.class);
    }
    
    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns AwdbWebService
     */
    @WebEndpoint(name = "AwdbWebServiceImplPort")
    public AwdbWebService getAwdbWebServiceImplPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://www.wcc.nrcs.usda.gov/ns/awdbWebService", "AwdbWebServiceImplPort"), AwdbWebService.class, features);
        //return super.getPort(new QName("https://www.wcc.nrcs.usda.gov/ns/awdbWebService", "AwdbWebServiceImplPort"), AwdbWebService.class, features);
        //return super.getPort(new QName("https://wcc.sc.egov.usda.gov/awdbWebService", "AwdbWebServiceImplPort"), AwdbWebService.class, features);
    }

}