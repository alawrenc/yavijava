package com.vmware.vim25.ws;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Calendar;
import javax.net.ssl.SSLHandshakeException;

import com.vmware.vim25.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.utility.LoadVcenterProps;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.util.PropertyCollectorUtil;

import javax.net.ssl.HttpsURLConnection;

public class WSClientIntTest {
    SoapClient wsClient = null;


    @Before
    public void setUp() throws Exception {
        if (null == LoadVcenterProps.url || null == LoadVcenterProps.userName
                || null == LoadVcenterProps.password
                || null == LoadVcenterProps.secondUrl
                || null == LoadVcenterProps.badUrl
                || "".equals(LoadVcenterProps.url.trim())
                || "".equals(LoadVcenterProps.secondUrl.trim())
                || "".equals(LoadVcenterProps.badUrl.trim())
                || "".equals(LoadVcenterProps.userName.trim())
                || "".equals(LoadVcenterProps.password.trim())) {
            throw new Exception("Vcenter credentials not loaded");
        }

        ServiceInstance si = null;
        try {
            si = new ServiceInstance(new URL(LoadVcenterProps.url),
                    LoadVcenterProps.userName, LoadVcenterProps.password, true);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        
        wsClient = new WSClient(LoadVcenterProps.url, true);

        wsClient.setVimNameSpace(ServiceInstance.VIM25_NAMESPACE);
        wsClient.setSoapActionOnApiVersion("5.5");
        wsClient.setCookie(si.getSessionManager().getServerConnection()
                .getVimService().getWsc().getCookie());
    }

    /**
     * This method will test that you can ignore ssl to a vcenter but it doesnt trust
     * every cert on the net and will fail trying to connect to my jenkins server
     */
    @Test(expected = SSLHandshakeException.class)
    public void testIgnoreSslDoesNotTrustAllCertsOnline() throws Exception {
        ServiceInstance si = new ServiceInstance(new URL(LoadVcenterProps.url), LoadVcenterProps.userName, LoadVcenterProps.password, true);
        // if we get here we were successful ignoring the self signed cert from vcenter
        assert si.getServerClock() instanceof Calendar;
        URL badUrl = new URL(LoadVcenterProps.badUrl);
        HttpsURLConnection myURLConnection = (HttpsURLConnection) badUrl.openConnection();
        // this should throw a handshake exception
        myURLConnection.connect();
    }

    /**
     * This method will test that you can ignore ssl to a vcenter but it doesnt trust
     * every cert on the net and will fail trying to connect to my jenkins server
     */
    @Test
    public void testIgnoreSslAllowsMultiplevCentersToBeIgnored() throws Exception {
        ServiceInstance si = new ServiceInstance(new URL(LoadVcenterProps.url), LoadVcenterProps.userName, LoadVcenterProps.password, true);
        // if we get here we were successful ignoring the self signed cert from vcenter
        assert si.getServerClock() instanceof Calendar;
        ServiceInstance serviceInstance = new ServiceInstance(new URL(LoadVcenterProps.secondUrl), LoadVcenterProps.userName, LoadVcenterProps.password, true);
        assert serviceInstance.currentTime() instanceof Calendar;
    }

    /**
     * This method should fail with ssl handshake exception
     * the vcenter used in your properties file should be running on ssl
     * and you should not have its cert imported in your keystore. For these
     * tests I rely on a vCenter Server Appliance running simulator.
     */
    @Test
    public void testDoNotIgnoreSslFailsOnSelfSignedCertNotInKeyStore() throws Exception {
        Throwable t = null;
        try {
            ServiceInstance si = new ServiceInstance(new URL(LoadVcenterProps.url), LoadVcenterProps.userName, LoadVcenterProps.password, false);
        }
        catch (RemoteException re) {
            t = re;
        }
        assert t.getCause() instanceof SSLHandshakeException;
    }

    /**
     * This test method will bring all the host systems under particular
     * vCenter.
     */
    @Test
    public void testGetHosts() {

        ObjectContent[] hostSystems = null;
        try {
            hostSystems = (ObjectContent[]) wsClient.invoke(
                    "RetrieveProperties", buildGetHostsArgs(),
                    "ObjectContent[]");
            if (hostSystems == null) {
                hostSystems = (ObjectContent[]) wsClient.invoke(
                        "RetrieveProperties", buildGetHostsArgs(),
                        "ObjectContent[]");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        Assert.assertNotNull(hostSystems);
        Assert.assertNotEquals(0, hostSystems.length);
    }

    /**
     * This test method will test marshalling feature of the Soap Client.
     */
    @Test
    public void testReqMarshall() {

        String soapPayload = wsClient.marshall("RetrieveProperties",
                buildGetHostsArgs());
        Assert.assertNotNull(soapPayload);
    }

    /**
     * This method will build the request payload.
     * 
     * @return Argument[]
     */
    private Argument[] buildGetHostsArgs() {
        Argument[] paras = new Argument[2];
        SelectionSpec[] selectionSpecs = null;

        ManagedObjectReference mor = new ManagedObjectReference();
        mor.setType("PropertyCollector");
        mor.setVal("propertyCollector");

        selectionSpecs = PropertyCollectorUtil.buildFullTraversalV4();

        // Need to set the vcenter specific details here.
        ManagedObjectReference vcenterMor = new ManagedObjectReference();
        vcenterMor.setType("Folder");
        vcenterMor.setVal("group-d1");

        ObjectSpec os = new ObjectSpec();
        os.setObj(vcenterMor);
        os.setSkip(Boolean.FALSE);
        os.setSelectSet(selectionSpecs);

        String[][] typeinfo = new String[][] { new String[] { "HostSystem",
                "name", }, };
        PropertySpec[] propspecary = PropertyCollectorUtil
                .buildPropertySpecArray(typeinfo);

        PropertyFilterSpec spec = new PropertyFilterSpec();
        spec.setObjectSet(new ObjectSpec[] { os });
        spec.setPropSet(propspecary);

        paras[0] = new Argument("_this", "ManagedObjectReference", mor);
        paras[1] = new Argument("specSet", "PropertyFilterSpec[]",
                new PropertyFilterSpec[] { spec });

        return paras;
    }
}
