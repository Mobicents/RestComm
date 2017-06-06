package org.restcomm.connect.extension.api;

import java.util.ArrayList;
import java.util.Map;

import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.commons.telephony.CreateCallType;

public interface IExtensionCreateCallRequest extends IExtensionRequest {

     String getFrom();

     String getTo();

     Sid getAccountId();

     boolean isFromApi();

     boolean isParentCallSidExists();

     /**
      * @return the CreateCallType
      */
     CreateCallType getType();

    /**
     * @return the outboundProxy
     */
    String getOutboundProxy();

    /**
     * @param outboundProxy the outboundProxy to set
     */
    void setOutboundProxy(String outboundProxy);

    /**
     * @return the outboundProxyUsername
     */
    String getOutboundProxyUsername();

    /**
     * @param outboundProxyUsername the outboundProxyUsername to set
     */
    void setOutboundProxyUsername(String outboundProxyUsername);

    /**
     * @return the outboundProxyPassword
     */
    String getOutboundProxyPassword();

    /**
     * @param outboundProxyPassword the outboundProxyPassword to set
     */
    void setOutboundProxyPassword(String outboundProxyPassword);

    /**
     * @return the outboundProxyHeaders
     */
    Map<String,ArrayList<String>> getOutboundProxyHeaders();

    /**
     * @param outboundProxyHeaders the outboundProxyHeaders to set
     */
    void setOutboundProxyHeaders(Map<String,ArrayList<String>> outboundProxyHeaders);

    /**
     * @return the Request URI
     */
    String getRequestURI();
}
