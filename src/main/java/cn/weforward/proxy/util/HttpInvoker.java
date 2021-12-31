/**
 * Copyright (c) 2019,2020 honintech
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package cn.weforward.proxy.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import cn.weforward.common.util.NumberUtil;
import cn.weforward.common.util.StringUtil;

/**
 * HTTP调用器
 * 
 * @author daibo
 *
 */
public class HttpInvoker {
	/**
	 * 最大链接数
	 */
	public static int DEFAULT_MAXTOTAL = NumberUtil
			.toInt(System.getProperty("cn.weforward.devops.util.http.defaultmaxtotal"), 20);
	/**
	 * 最大路由链接数，根据连接到的主机对MaxTotal的一个细分
	 */
	public static int DEFAULT_MAXPERROUTE = NumberUtil
			.toInt(System.getProperty("cn.weforward.devops.util.http.defaultmaxperroute"), 20);

	/**
	 * HTTP连接管理
	 */
	protected PoolingHttpClientConnectionManager m_HttpClientManager;
	/**
	 * HTTP参数
	 */
	protected RequestConfig m_HttpParams;
	/** 用户名 */
	protected String m_UserName;
	/** 密码 */
	protected String m_Password;
	/** 供应商 */
	protected CredentialsProvider m_Provider;

	/**
	 * 构造
	 * 
	 * @param connectionSecond 链接超时时间，单位秒
	 * @param soSecond         读取超时时间，单位秒
	 * @throws IOException
	 */
	public HttpInvoker(int connectionSecond, int soSecond) throws IOException {
		this(connectionSecond, soSecond, null);
	}

	/**
	 * 构造
	 * 
	 * @param connectionSecond 链接超时时间，单位秒
	 * @param soSecond         读取超时时间，单位秒
	 * @param sslcontext       SSL内容
	 * @throws IOException
	 */
	public HttpInvoker(int connectionSecond, int soSecond, SSLContext sslcontext) throws IOException {
		this(connectionSecond, soSecond, sslcontext, new String[] { "TLSv1", "TLSv1.2" }, null);
	}

	/**
	 * 构造
	 * 
	 * @param connectionSecond      链接超时时间，单位秒
	 * @param soSecond              读取超时时间，单位秒
	 * @param sslcontext            SSL内容
	 * @param supportedProtocols    支持的协议
	 * @param supportedCipherSuites 支持的密码套件
	 * @throws IOException
	 */
	public HttpInvoker(int connectionSecond, int soSecond, SSLContext sslcontext, String[] supportedProtocols,
			String[] supportedCipherSuites) throws IOException {
		org.apache.http.config.Registry<ConnectionSocketFactory> registry;
		try {
			registry = createRegister(sslcontext, supportedProtocols, supportedCipherSuites);
		} catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | CertificateException
				| KeyStoreException e) {
			throw new IOException("初始化证书失败", e);
		}
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
		// 将最大连接数增加
		cm.setMaxTotal(DEFAULT_MAXTOTAL);
		// 将每个路由基础的连接增加
		cm.setDefaultMaxPerRoute(DEFAULT_MAXPERROUTE);
		m_HttpClientManager = cm;
		m_HttpParams = RequestConfig.custom().setConnectionRequestTimeout(connectionSecond * 1000)
				.setSocketTimeout(soSecond * 1000).build();
	}

	/* 创建注册 */
	private Registry<ConnectionSocketFactory> createRegister(SSLContext sslcontent, String[] supportedProtocols,
			String[] supportedCipherSuites) throws NoSuchAlgorithmException, CertificateException, IOException,
			KeyStoreException, KeyManagementException, UnrecoverableKeyException {
		RegistryBuilder<ConnectionSocketFactory> build = RegistryBuilder.<ConnectionSocketFactory>create();
		ConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
		build.register("http", plainsf);
		LayeredConnectionSocketFactory sslsf;
		if (null == sslcontent) {
			sslsf = SSLConnectionSocketFactory.getSocketFactory();
		} else {

			sslsf = new SSLConnectionSocketFactory(sslcontent, supportedProtocols, supportedCipherSuites,
					NoopHostnameVerifier.INSTANCE);
		}
		return build.register("https", sslsf).build();
	}

	/**
	 * 设置超时时间
	 * 
	 * @param connectionSecond
	 * @param soSecond
	 */
	public void setTimeout(int connectionSecond, int soSecond) {
		m_HttpParams = RequestConfig.custom().setConnectionRequestTimeout(connectionSecond * 1000)
				.setSocketTimeout(soSecond * 1000).build();
	}

	/** 用户名 */
	public void setUserName(String v) {
		m_UserName = v;
	}

	/** 密码 */
	public void setPassword(String v) {
		m_Password = v;
	}

	private CredentialsProvider getProvider() {
		if (null != m_Provider) {
			return m_Provider;
		}
		CredentialsProvider provider = null;
		if (!StringUtil.isEmpty(m_UserName) && !StringUtil.isEmpty(m_Password)) {
			provider = new BasicCredentialsProvider();
			provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(m_UserName, m_Password));
		}
		m_Provider = provider;
		return m_Provider;
	}

	/* 获取客户端 */
	private HttpClient getHttpClient() {
		m_HttpClientManager.closeExpiredConnections();
		m_HttpClientManager.closeIdleConnections(1, TimeUnit.HOURS);
		// 设置认证
		CredentialsProvider provider = getProvider();
		HttpClientBuilder builder = HttpClientBuilder.create().setConnectionManager(m_HttpClientManager)
				.setDefaultRequestConfig(m_HttpParams);
		if (null != provider) {
			builder.setDefaultCredentialsProvider(provider);
		}
		return builder.build();
	}

	/**
	 * 执行请求
	 * 
	 * @param request
	 * @return
	 * @throws IOException
	 */
	public HttpResponse execute(HttpUriRequest request) throws IOException {
		return getHttpClient().execute(request);
	}

	/**
	 * 执行请求
	 * 
	 * @param request
	 * @param context
	 * @return
	 * @throws IOException
	 */
	public HttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException {
		return getHttpClient().execute(request, context);
	}

	/**
	 * post请求
	 * 
	 * @param url
	 * @param form
	 * @param charset
	 * @return
	 * @throws IOException
	 */
	public String post(String url, String content, Charset charset) throws IOException {
		HttpPost post = new HttpPost(url);
		post.setEntity(new StringEntity(content, charset));
		HttpResponse response = null;
		try {
			response = execute(post);
			StatusLine status = response.getStatusLine();
			if (status.getStatusCode() != HttpStatus.SC_OK) {
				throw new IOException("status返回异常状态码:" + status);
			}
			return EntityUtils.toString(response.getEntity(), charset);
		} finally {
			if (null != response) {
				EntityUtils.consume(response.getEntity());
			}
		}
	}

	/**
	 * get请求
	 * 
	 * @param url
	 * @param charset
	 * @return
	 * @throws IOException
	 */
	public String get(String url, Charset charset) throws IOException {
		HttpGet post = new HttpGet(url);
		HttpResponse response = null;
		try {
			response = execute(post);
			StatusLine status = response.getStatusLine();
			if (status.getStatusCode() != HttpStatus.SC_OK) {
				throw new IOException("status返回异常状态码:" + status);
			}
			return EntityUtils.toString(response.getEntity(), charset);
		} finally {
			if (null != response) {
				EntityUtils.consume(response.getEntity());
			}
		}
	}

	public static void consume(HttpResponse response) throws IOException {
		if (null == response) {
			return;
		}
		EntityUtils.consume(response.getEntity());
	}

}
