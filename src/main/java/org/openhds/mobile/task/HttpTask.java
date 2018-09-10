package org.openhds.mobile.task;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.conn.ConnectTimeoutException;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

public class HttpTask<Params, Progress> extends	AsyncTask<Params, Progress, HttpTask.EndResult> {
	private static final int UNAUTHORIZED_STATUS_CODE = 401;
	private static final int SUCCESS_STATUS_CODE = 200;
	private static final int NO_CONTENT_CODE = 204;

	protected RequestContext requestContext;
	protected TaskListener listener;	
	protected Context ctx;

	public HttpTask(Context ctx, RequestContext requestContext, TaskListener listener) {
		this(ctx, requestContext);
		this.listener = listener;
	}

	public HttpTask(Context ctx, RequestContext requestContext) {
		this.ctx = ctx;
		this.requestContext = requestContext;
	}

	public static enum EndResult {
		BAD_AUTHENTICATION, CONNECTION_ERROR, CONNECTION_TIMEOUT, SUCCESS, FAILURE, NO_CONTENT
	}

	public interface TaskListener {
		void onFailedAuthentication();

		void onConnectionError();

		void onConnectionTimeout();

		void onSuccess();

		void onFailure();

		void onNoContent();
	}

	public static class RequestContext {
		URL url;
		String user;
		String password;

		public RequestContext url(URL url) {
			this.url = url;
			return this;
		}

		public RequestContext user(String user) {
			this.user = user;
			return this;
		}

		public RequestContext password(String password) {
			this.password = password;
			return this;
		}
	}

	@Override
	protected EndResult doInBackground(Params... params) {
		
		try {
		
			HttpURLConnection connection = createConnection(requestContext.url, requestContext.user, requestContext.password);
						
			
			switch (connection.getResponseCode()) {
			case SUCCESS_STATUS_CODE:
				return handleResponseData(connection);
			case NO_CONTENT_CODE:
				return EndResult.NO_CONTENT;
			case UNAUTHORIZED_STATUS_CODE:
				return EndResult.BAD_AUTHENTICATION;
			default:
				return EndResult.CONNECTION_ERROR;
			}
		} catch (java.net.ProtocolException e) {
			return EndResult.CONNECTION_ERROR;
		} catch (ConnectTimeoutException e) {
			return EndResult.CONNECTION_TIMEOUT;
		} catch (IOException e) {
			return EndResult.CONNECTION_ERROR;
		} catch(Exception e){
			return EndResult.CONNECTION_ERROR;
		}
	}
			
	private HttpURLConnection createConnection(String strUrl, String username, String password) throws Exception {
		URL url = new URL(strUrl);
		
		return createConnection(url, username, password);
	}
	
	private HttpURLConnection createConnection(URL url, String username, String password) throws Exception {
		String basicAuth = "Basic " + new String(Base64.encode((username+":"+password).getBytes(),Base64.NO_WRAP ));

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setReadTimeout(10000);
		connection.setConnectTimeout(15000);
		connection.setDoInput(true);
		connection.setRequestProperty("Authorization", basicAuth);

		Log.d("exec-url", ""+url);

		connection.connect();
		
		return connection;
	}

	@Override
	protected void onPostExecute(EndResult result) {
		switch (result) {
		case BAD_AUTHENTICATION:
			listener.onFailedAuthentication();
			break;
		case FAILURE:
			listener.onFailure();
			break;
		case CONNECTION_ERROR:
			listener.onConnectionError();
			break;
		case CONNECTION_TIMEOUT:
			listener.onConnectionTimeout();
			break;
		case SUCCESS:
			listener.onSuccess();
			break;
		case NO_CONTENT:
			listener.onNoContent();
			break;
		}
	}

	protected EndResult handleResponseData(HttpURLConnection response) {
		return EndResult.SUCCESS;
	}
}
