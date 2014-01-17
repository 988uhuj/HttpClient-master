package me.risky.library.function;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import me.risky.library.base.ImageUtil;
import me.risky.library.base.JsonUtil;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONStringer;

import android.graphics.Bitmap;
import android.util.Log;

public class HttpService {
	private static final String CHARSET = HTTP.UTF_8;
	private static final String USER_AGENT = "mobile";
	
	private static DefaultHttpClient customerHttpClient;
	
//	private static String SESSION_ID = null;
//	private static String SESSION_ID_KEY = "JSESSIONID";
	
	public static String TAG = "HttpService";
	public String get(String url) {
		try {
			HttpGet request = new HttpGet(url);
			HttpClient client = getHttpClient();

			HttpResponse response = client.execute(request);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				HttpEntity resEntity = response.getEntity();
				return (resEntity == null) ? null : EntityUtils.toString(
						resEntity, CHARSET);
			}
			return null;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	
	public String post(String url, Map<String, Object> map, boolean isAsJson) {
		if(isAsJson == true){
			return post(url, map);
		}
		HttpPost request = new HttpPost(url);
		request.setHeader("Accept", "application/json");
		if (map != null) {
			try {
				List<NameValuePair> params = new ArrayList<NameValuePair>();
				for (String key : map.keySet()) {
					params.add(new BasicNameValuePair(key, map.get(key)
							.toString()));
				}
				request.setEntity(new UrlEncodedFormEntity(params, CHARSET));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}

		return execute(request);

	}

	public static synchronized DefaultHttpClient getHttpClient() {
		if (null == customerHttpClient) {
			HttpParams params = new BasicHttpParams();
			
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, CHARSET);
			HttpProtocolParams.setUseExpectContinue(params, true);
			HttpProtocolParams.setUserAgent(params, USER_AGENT);
		
			ConnManagerParams.setTimeout(params, 1000);
			HttpConnectionParams.setConnectionTimeout(params, 2000);
			HttpConnectionParams.setSoTimeout(params, 4000);

			SchemeRegistry schReg = new SchemeRegistry();
			schReg.register(new Scheme("http", PlainSocketFactory
					.getSocketFactory(), 80));
			schReg.register(new Scheme("https", SSLSocketFactory
					.getSocketFactory(), 443));

			ClientConnectionManager conMgr = new ThreadSafeClientConnManager(
					params, schReg);
			customerHttpClient = new DefaultHttpClient(conMgr, params);
			
			DefaultHttpRequestRetryHandler dhr = new DefaultHttpRequestRetryHandler(3,true);  
			((DefaultHttpClient)customerHttpClient).setHttpRequestRetryHandler(dhr);
		}
		return customerHttpClient;
	}

	public String post(String url, Map<String, Object> jsonMap) {
		HttpPost request = new HttpPost(url);

		request.setHeader("Accept", "application/json");
		if (jsonMap != null) {
			try {
				JSONStringer json = new JSONStringer();
				json.object();
				for (String key : jsonMap.keySet()) {
					json.key(key).value(jsonMap.get(key));
				}
				json.endObject();

				List<NameValuePair> params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair("param", json.toString()));

				request.setEntity(new UrlEncodedFormEntity(params, CHARSET));
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}

		return execute(request);
	}

	

	public void clearCookie() {
		Log.d(TAG, "SESSION CLEAR");
//		SESSION_ID = null;
		// TODO Unfinish
	}

	public String post(String url, Map<String, Object> map, Bitmap bmp, String fileName) {
		HttpPost request = new HttpPost(url);
		request.setHeader("Accept", "application/json");
		try {
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			String mapStr = JsonUtil.mapToJson(map);
			if(mapStr != null){
				builder.addTextBody("params", JsonUtil.mapToJson(map));
			}
			builder.addPart("file", new ByteArrayBody(ImageUtil.bitmapToByte(bmp), fileName));
			request.setEntity(builder.build());
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
			return null;
		}
		return execute(request);
	}
	public String post(String url, Map<String, Object> map, File file) {
		HttpPost request = new HttpPost(url);
		request.setHeader("Accept", "application/json");
		try {
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.addTextBody("params", JsonUtil.mapToJson(map));
			builder.addPart("file", new FileBody(file));
			request.setEntity(builder.build());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return execute(request);
		
	}
	
	private String execute(HttpPost request){
		try {
			HttpClient client = getHttpClient();
			// session_id
			HttpResponse response = client.execute(request);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				HttpEntity resEntity = response.getEntity();
				return (resEntity == null) ? null : EntityUtils.toString(resEntity, CHARSET);
			}
			return null;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
