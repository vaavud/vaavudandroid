package com.vaavud.android.network.request;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.vaavud.android.network.json.GsonDateTypeAdapter;
import com.vaavud.android.network.json.GsonFloatArrayTypeAdapter;
import com.vaavud.util.HTTPUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.zip.GZIPOutputStream;
 
/**
 * Volley adapter for JSON requests that will be parsed into Java objects by Gson.
 */
public class GsonRequest<T> extends Request<T> {
	
    private static final String PROTOCOL_CHARSET = "utf-8";
    private static final String PROTOCOL_CONTENT_TYPE = String.format("application/json; charset=%s", PROTOCOL_CHARSET);
    private static final String PROTOCOL_CONTENT_TYPE_GZIP = "application/octet-stream";

    private final Gson gson;
    private final String requestBody;
    private final Class<T> responseClass;
    private final Listener<T> listener;
    private final boolean enableGzip;
    private int mStatusCode;
 
    /**
     * Make a POST or GET request and return a parsed object from JSON.
     */
    public GsonRequest(String url, Object postObject, Class<T> responseClass,
            Listener<T> listener, ErrorListener errorListener) {
    	this(url, postObject, responseClass, listener, errorListener, true);
    }
    
    /**
     * Make a POST or GET request and return a parsed object from JSON.
     */
    public GsonRequest(String url, Object postObject, Class<T> responseClass,
            Listener<T> listener, ErrorListener errorListener, boolean enableGzip) {
    	
        super((postObject == null) ? Method.GET : Method.POST, enableGzip ? HTTPUtil.appendQueryParameter(url, "gzip", "true") : url, errorListener);

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Date.class, new GsonDateTypeAdapter());
        gsonBuilder.registerTypeAdapter(Float[].class, new GsonFloatArrayTypeAdapter());
        gson = gsonBuilder.create();
        
        this.enableGzip = enableGzip;
        this.requestBody = (postObject == null) ? null : gson.toJson(postObject);
        this.responseClass = responseClass;
        this.listener = listener;
    }
    
    @Override
    public String getBodyContentType() {
        return enableGzip ? PROTOCOL_CONTENT_TYPE_GZIP : PROTOCOL_CONTENT_TYPE;
    }

    @Override
    public byte[] getBody() {
    	if (requestBody == null) {
//    		Log.e("GsonRequest", "Body request null");
    		return null;
    	}
//    	Log.d("GsonRequest","Body is not null, continue");
        try {
            byte[] bytes = requestBody.getBytes(PROTOCOL_CHARSET);
            
            if (enableGzip) {
            	int sizeBefore = bytes.length;
            	ByteArrayOutputStream byteOut = new ByteArrayOutputStream(1024);
            	GZIPOutputStream gzipOut = new GZIPOutputStream(byteOut);
            	gzipOut.write(bytes);
            	gzipOut.close();
            	bytes = byteOut.toByteArray();
//            	Log.i("GsonRequest", "Compressed " + sizeBefore + "->" + bytes.length + " calling: " + getUrl());
            }
            
            return bytes;
        }
        catch (UnsupportedEncodingException e) {
//            Log.e("GsonRequest", "Unsupported encoding", e);
            return null;
        }
        catch (IOException e) {
//            Log.e("GsonRequest", "Error GZIP compressing", e);
            return null;
		}
    }

    @Override
    protected void deliverResponse(T response) {
        listener.onResponse(response);
    }
 
	@Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            String json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            return Response.success(gson.fromJson(json, responseClass), HttpHeaderParser.parseCacheHeaders(response));
        }
        catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        }
        catch (JsonSyntaxException e) {
            return Response.error(new ParseError(e));
        }
    }
}
