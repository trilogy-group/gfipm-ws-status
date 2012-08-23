/*
 * Copyright (c) 2012, Georgia Institute of Technology. All Rights Reserved.
 * This code was developed by Georgia Tech Research Institute (GTRI) under
 * a grant from the U.S. Dept. of Justice, Bureau of Justice Assistance.
 */
package net.gfipm.ws.status.cvc.service;

import com.sun.xml.wss.SubjectAccessor;
import com.sun.xml.wss.XWSSecurityException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.ws.WebServiceContext;
import net.gfipm.trustfabric.TrustFabric;
import net.gfipm.trustfabric.TrustFabricFactory;

/**
 * Class provides a sample implementation of the authorization access control decisions
 * based on the WSC (requestor service) CTF GFIPM attributes, included in the GFIPM SAML Assertion.
 */
public class GFIPMAuthorizationProvider {

    private static final boolean DEBUG = true;
    private static final Logger logger = Logger.getLogger(GFIPMAuthorizationProvider.class.getName());
    private static TrustFabric tf;

    static {
        tf = TrustFabricFactory.getInstance("http://ref.gfipm.net/gfipm-signed-ws-metadata.xml");
    }

    public static boolean isServiceAuthorized(String methodName, WebServiceContext wsContext) {
        boolean isAuthorized = false;

        if (DEBUG) {
            logger.log(Level.FINEST, "GFIPMAuthorizationProvider::isServiceAuthorized::Method:: " + methodName);
        }
        try {
            if (DEBUG) {
                logger.log(Level.FINEST, "GFIPMAuthorizationProvider::isServiceAuthorized::Subject Accessor::" + SubjectAccessor.getRequesterSubject(wsContext));
            }
            if (SubjectAccessor.getRequesterSubject(wsContext) != null) {
                for (Iterator<Object> it = SubjectAccessor.getRequesterSubject(wsContext).getPublicCredentials().iterator(); it.hasNext();) {
                    Object publicCredentialsObject = it.next();
//                        logger.log(Level.FINEST, "Public CredentialsObject::" + publicCredentialsObject +" class: " + publicCredentialsObject.getClass().getCanonicalName());
                    if (publicCredentialsObject instanceof X509Certificate) {
                        X509Certificate subjectX509Certificate = (X509Certificate) publicCredentialsObject;
                        //Delegate ID is determined from Entity Certificate.
                        String wscId = tf.getEntityId(subjectX509Certificate);
                        if (DEBUG) {
                            logger.log(Level.FINEST, "GFIPMAuthorizationProvider::isServiceAuthorized::Got the following WSC entity :: " + wscId + " using public Certificate ::" + subjectX509Certificate.getSubjectDN().getName());
                        }
                        //Provide authorization decision for the WSC to execute method.
                        if (tf.isWebServiceConsumer(wscId) && "net.gfipm.ws.status.cvc.service.GFIPMWebServicesStatusWebServiceImpl.getSystemStatus".equals(methodName)) {
                            //In this example any WSC from the CTF is authorized to execute method. 
                            isAuthorized = true;
                        }
                    } else {
                        if (DEBUG) {
                            logger.log(Level.FINEST, "GFIPMAuthorizationProvider::isServiceAuthorized::Object in public credentials :: " + publicCredentialsObject.getClass().getCanonicalName());
                        }
                    }
                }
            }
        } catch (XWSSecurityException ex) {
            logger.log(Level.SEVERE, "Unable to get UserPrincipal", ex);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unknown exception", e);
        }

        return isAuthorized;
    }
    
    public static String getCurrentMethodName() {
        StackTraceElement stackTraceElements[] = (new Throwable()).getStackTrace();
        return stackTraceElements[1].toString().split("\\(")[0];
    }
}