/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.CoreConnectionPNames;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.http.client.ClientHttpRequestFactory} implementation that uses
 * <a href="http://hc.apache.org/httpcomponents-client-ga/httpclient/">Http Components HttpClient</a> to create requests.
 *
 * <p>Allows to use a pre-configured {@link HttpClient} instance -
 * potentially with authentication, HTTP connection pooling, etc.
 *
 * @author Oleg Kalnichevski
 * @author Arjen Poutsma
 * @since 3.1
 */
public class HttpComponentsClientHttpRequestFactory implements ClientHttpRequestFactory, DisposableBean {

	private static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 100;

	private static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 5;

	private static final int DEFAULT_READ_TIMEOUT_MILLISECONDS = (60 * 1000);

	private HttpClient httpClient;

	/**
	 * Create a new instance of the {@code HttpComponentsClientHttpRequestFactory} with a default {@link HttpClient} that
	 * uses a default {@link org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager}
	 */
	public HttpComponentsClientHttpRequestFactory() {
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
		schemeRegistry.register(new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));

		ThreadSafeClientConnManager connectionManager = new ThreadSafeClientConnManager(schemeRegistry);
		connectionManager.setMaxTotal(DEFAULT_MAX_TOTAL_CONNECTIONS);
		connectionManager.setDefaultMaxPerRoute(DEFAULT_MAX_CONNECTIONS_PER_ROUTE);

		httpClient = new DefaultHttpClient(connectionManager);
		this.setReadTimeout(DEFAULT_READ_TIMEOUT_MILLISECONDS);
	}

	/**
	 * Create a new instance of the {@code HttpComponentsClientHttpRequestFactory} with the given {@link HttpClient}
	 * instance.
	 *
	 * @param httpClient the HttpClient instance to use for this factory
	 */
	public HttpComponentsClientHttpRequestFactory(HttpClient httpClient) {
		Assert.notNull(httpClient, "httpClient must not be null");
		this.httpClient = httpClient;
	}

	/**
	 * Set the {@code HttpClient} used by this factory.
	 */
	public void setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	/**
	 * Set the socket read timeout for the underlying HttpClient. A value of 0 means <em>never</em> timeout.
	 *
	 * @param timeout the timeout value in milliseconds
	 * @see org.apache.commons.httpclient.params.HttpConnectionManagerParams#setSoTimeout(int)
	 */
	public void setReadTimeout(int timeout) {
		if (timeout < 0) {
			throw new IllegalArgumentException("timeout must be a non-negative value");
		}
		getHttpClient().getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT, timeout);
	}

	/**
	 * Return the {@code HttpClient} used by this factory.
	 */
	public HttpClient getHttpClient() {
		return this.httpClient;
	}

	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		HttpUriRequest httpRequest = createHttpUriRequest(httpMethod, uri);
		postProcessHttpRequest(httpRequest);
		return new HttpComponentsClientHttpRequest(getHttpClient(), httpRequest);
	}

	/**
	 * Create a Commons HttpMethodBase object for the given HTTP method and URI specification.
	 *
	 * @param httpMethod the HTTP method
	 * @param uri the URI
	 * @return the Commons HttpMethodBase object
	 */
	protected HttpUriRequest createHttpUriRequest(HttpMethod httpMethod, URI uri) {
		switch (httpMethod) {
			case GET:
				return new HttpGet(uri);
			case DELETE:
				return new HttpDelete(uri);
			case HEAD:
				return new HttpHead(uri);
			case OPTIONS:
				return new HttpOptions(uri);
			case POST:
				return new HttpPost(uri);
			case PUT:
				return new HttpPut(uri);
			case TRACE:
				return new HttpTrace(uri);
			default:
				throw new IllegalArgumentException("Invalid HTTP method: " + httpMethod);
		}
	}

	/**
	 * Template method that allows for manipulating the {@link HttpUriRequest} before it is returned as part of a {@link
	 * HttpComponentsClientHttpRequest}.
	 * <p>The default implementation is empty.
	 *
	 * @param request the request to process
	 */
	protected void postProcessHttpRequest(HttpUriRequest request) {
	}

	/**
	 * Shutdown hook that closes the underlying {@link org.apache.http.conn.ClientConnectionManager
	 * ClientConnectionManager}'s connection pool, if any.
	 */
	public void destroy() {
		getHttpClient().getConnectionManager().shutdown();
	}
}
