//package com.atlassian.jira.plugins.dvcs.net;
//
//import java.util.Map;
//
//import com.atlassian.jira.plugins.dvcs.auth.Authentication;
//import com.atlassian.jira.plugins.dvcs.net.ExtendedResponseHandler.ExtendedResponse;
//import com.atlassian.sal.api.net.Request;
//import com.atlassian.sal.api.net.ResponseException;
//
//public interface RequestHelper
//{
//    public String get(Authentication auth, String urlPath, Map<String, Object> params, String apiBaseUrl) throws ResponseException;
//
//    public ExtendedResponseHandler.ExtendedResponse getExtendedResponse(Authentication auth, String urlPath, Map<String, Object> params, String apiBaseUrl)
//        throws ResponseException;
//
//    public String post(Authentication auth, String urlPath, String postData, String apiBaseUrl) throws ResponseException;
//
//    public void delete(Authentication auth, String apiUrl, String urlPath) throws ResponseException;
//
//	public abstract String runRequest(Request.MethodType methodType, String apiBaseUrl, String urlPath, Authentication auth, Map<String, Object> params, String postData)
//			throws ResponseException;
//
//	public abstract ExtendedResponse runRequestGetExtendedResponse(Request.MethodType methodType, String apiBaseUrl, String urlPath, Authentication auth,
//			Map<String, Object> params, String postData) throws ResponseException;
//}//TODO meno factory nesedi...
////TODO payload bol vymazany, cize refactoring
////TODO difference between request with and without payload