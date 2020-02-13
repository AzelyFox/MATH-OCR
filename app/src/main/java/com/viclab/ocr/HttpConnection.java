package com.viclab.ocr;

import com.viclab.ocr.mathpix.Constant;
import okhttp3.*;

public class HttpConnection {

	private OkHttpClient client;
	private static HttpConnection instance = new HttpConnection();

	public static HttpConnection getInstance() {
		return instance;
	}

	private HttpConnection(){ this.client = new OkHttpClient(); }

	public void requestWebServer(String idx, String answer, Callback callback) {
		HttpUrl httpUrl = new HttpUrl.Builder()
				.scheme(Constant.URL_SCHEME)
				.host(Constant.URL_HOST)
				.addPathSegment(Constant.URL_PATH)
				.addQueryParameter(Constant.URL_QUERY_INDEX, idx)
				.addQueryParameter(Constant.URL_QUERY_ANSWER, answer)
				.build();
		Request request = new Request.Builder()
				.url(httpUrl)
				.build();
		client.newCall(request).enqueue(callback);
	}

}
