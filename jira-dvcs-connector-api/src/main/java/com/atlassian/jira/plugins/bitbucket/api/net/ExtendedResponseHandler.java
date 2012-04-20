package com.atlassian.jira.plugins.bitbucket.api.net;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicReference;

import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseHandler;

/**
 * Default implementation of ResponseHandler throws ResponseException if the response is not successful. 
 * But the unfortunately exception doesn't provide the error code. We need to know if error is UNAUTHORISED or NOT_FOUND. 
 * This handler keeps the response including error code and provides it for later use. 
 */
public class ExtendedResponseHandler implements ResponseHandler<Response>
{
    private final AtomicReference<ExtendedResponse> extendedResponse = new AtomicReference<ExtendedResponse>();
    @Override
    public void handle(Response response) throws ResponseException
    {
        ExtendedResponse er = new ExtendedResponse(response.isSuccessful(), response.getStatusCode(), response.getResponseBodyAsString());
        extendedResponse.set(er);
    }
    
    public ExtendedResponse getExtendedResponse()
    {
        return extendedResponse.get();
    }
    
    public static class ExtendedResponse
    {
        private final boolean successful;
        private final int statusCode;
        private final String responseString;

        public ExtendedResponse(boolean successful, int statusCode, String responseString)
        {
            this.successful = successful;
            this.statusCode = statusCode;
            this.responseString = responseString;
        }

        public boolean isSuccessful()
        {
            return successful;
        }

        public int getStatusCode()
        {
            return statusCode;
        }

        public String getResponseString()
        {
            return responseString;
        }
        
        @Override
        public String toString()
        {
            return MessageFormat.format("successful: {0}, statusCode: {1}, responseString: {2}", successful, statusCode, responseString);
        }
    }
}