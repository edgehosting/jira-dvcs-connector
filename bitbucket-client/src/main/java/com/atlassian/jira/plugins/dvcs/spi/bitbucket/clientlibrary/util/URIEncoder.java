//package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.util;
//
//import java.util.Map;
//
//import org.apache.commons.httpclient.URIException;
//import org.apache.commons.httpclient.util.URIUtil;
//
//import com.google.common.base.Joiner;
//
///**
// * URLWithParamsEncoder
// *
// * @author Martin Skurla mskurla@atlassian.com
// */
//public final class URIEncoder
//{
//    public static final String UTF_8_ENCODING = "UTF-8";
//
//
//    private URIEncoder() {}
//
//
//    public static String encodeURI(String uriPath, Map<String, String> parameters, String encoding)
//    {
//        String encodedUriPath;
//
//        try
//        {
//            encodedUriPath = URIUtil.encodePath(uriPath, encoding);
//        }
//        catch (URIException e)
//        {
//            throw new IllegalArgumentException("Required encoding not found", e);
//        }
//
//        if (parameters.isEmpty())
//        {
//            return encodedUriPath;
//        }
//        else
//        {
//            boolean urlAlreadyHasParams = uriPath.contains("?");
//
//            return encodedUriPath + (urlAlreadyHasParams ?  "&" : "?") + encodeHttpParameters(parameters, encoding);
//        }
//    }
//
//	public static String encodeHttpParameters(Map<String, String> parameters, String encoding)
//	{
//		if (parameters != null && !parameters.isEmpty())
//		{
//            Joiner.MapJoiner mapJoiner = Joiner.on("&").withKeyValueSeparator("=");
//
//            return mapJoiner.join(MapEncodingURLStringValues.fromMap(parameters, encoding));
//		}
//		return "";
//	}
//}
