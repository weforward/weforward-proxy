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
package cn.weforward.proxy;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.Dictionary;
import cn.weforward.common.restful.RestfulRequest;
import cn.weforward.common.restful.RestfulResponse;
import cn.weforward.common.restful.RestfulService;
import cn.weforward.common.sys.VmStat;
import cn.weforward.common.util.AntPathPattern;
import cn.weforward.common.util.FreezedList;
import cn.weforward.common.util.StringUtil;
import cn.weforward.protocol.aio.http.RestfulServer;
import cn.weforward.protocol.aio.netty.NettyHttpServer;
import cn.weforward.proxy.exception.HttpException;
import cn.weforward.proxy.util.ByteArrayInputStreamExt;
import cn.weforward.proxy.util.VersionUtil;

/**
 * HTML服务器
 * 
 * @author daibo
 *
 */
public class HtmlServer extends NettyHttpServer implements RestfulService {
	/** 日志 */
	private static final Logger _Logger = LoggerFactory.getLogger(HtmlServer.class);
	/** 默认主机路由Host值 */
	private static final String DEFAULT_HOST = "*";
	/** 启用ant匹配 */
	private boolean m_AntEnable;
	/** 启用正则表达式匹配 */
	private boolean m_RegEnable;
	/** 服务id */
	protected String m_Serverid;
	/** 版本号 */
	private String m_Version;
	/** 默认主机路由 */
	protected HostRoutes m_DefaultRoute;
	/** 路由映射表 */
	protected ConcurrentMap<String, HostRoutes> m_Routes;
	/** 不使用缓存的文件后缀 */
	private List<String> m_NoCacheFiles = Collections.emptyList();
	/** 特殊配置 */
	private Map<String, Resource> m_Configs = new HashMap<String, Resource>();

	/**
	 * 构造
	 * 
	 * @param name 名称
	 * @param port 端口
	 * @throws Exception 异常
	 */
	public HtmlServer(String name, int port) throws Exception {
		super(port);
		setName(name);
		setAcceptThreads(VmStat._cpus);
		setWorkThreads(50);
		RestfulServer server = new RestfulServer(this);
		setHandlerFactory(server);
	}

	public void setExecutor(Executor executor) {
		((RestfulServer) m_HandlerFactory).setExecutor(executor);
	}

	/**
	 * 启用ant匹配
	 * 
	 * @param enable 是否启用
	 */
	public void setRegEnable(boolean enable) {
		m_RegEnable = enable;
	}

	/**
	 * 启用正则表达式匹配
	 * 
	 * @param enable 是否启用
	 */
	public void setAntEnable(boolean enable) {
		m_AntEnable = enable;
	}

	/**
	 * 设置服务id
	 * 
	 * @param id 服务id
	 */
	public void setServerid(String id) {
		m_Serverid = id;
	}

	/**
	 * 设置不使用缓存的文件后缀列表
	 * 
	 * @param list 不使用缓存的文件后缀列表
	 */
	public void setNoCacheFiles(List<String> list) {
		m_NoCacheFiles = FreezedList.freezed(list);
	}

	/**
	 * 添加配置资源
	 * 
	 * @param config 配置资源
	 */
	public void addConfig(Resource config) {
		if (null == config) {
			return;
		}
		m_Configs.put(config.getName(), config);
	}

	/**
	 * 设置路由
	 * 
	 * @param routes 路由集合
	 * @throws Exception 异常
	 */
	public void setRoutes(List<HostRoute> routes) throws Exception {
		m_Routes = new ConcurrentHashMap<>();
		for (HostRoute r : routes) {
			if (StringUtil.eq(r.getName(), DEFAULT_HOST)) {
				if (null == m_DefaultRoute) {
					m_DefaultRoute = new HostRoutes(r);
				} else {
					m_DefaultRoute.add(r);
				}
			} else {
				HostRoutes rs = m_Routes.get(r.getName());
				if (null == rs) {
					rs = new HostRoutes(r);
				} else {
					rs.add(r);
				}
				m_Routes.put(r.getName(), rs);
			}
		}
		start();
	}

	@Override
	public void precheck(RestfulRequest request, RestfulResponse response) throws IOException {
		if (!RestfulRequest.GET.equals(request.getVerb())) {
			// 只支持GET方法
			response.setStatus(RestfulResponse.STATUS_METHOD_NOT_ALLOWED);
			response.openOutput().close();
		}
	}

	@Override
	public void service(RestfulRequest request, RestfulResponse response) throws IOException {
		String host = getHost(request);
		String uri = request.getUri();
		response.setHeader("wf-srv", m_Serverid);
		response.setHeader("wf-version", getVersion());
		Resource file = null;
		if (null == file) {
			try {
				file = readFile(host, uri);
			} catch (HttpException e) {
				response.setStatus(e.getCode());
				response.openOutput().close();
				return;
			}
		}
		if (null == file || !file.exists()) {
			// 配置
			int index = uri.lastIndexOf('/');
			if (index > 0) {
				file = m_Configs.get(uri.substring(index + 1));
			}
		}
		if (_Logger.isTraceEnabled()) {
			_Logger.trace(host + uri + "=>" + file);
		}
		if (null == file || !file.exists()) {
			response.setStatus(RestfulResponse.STATUS_NOT_FOUND);
			response.openOutput().close();
			return;
		}
		String suffix = getSuffix(file.getName());
		String type = ContentType.get(suffix);
		if (!m_NoCacheFiles.contains(suffix)) {
			String modified = getHeader(request, "if-modified-since");
			if (!StringUtil.isEmpty(modified)) {
				if (StringUtil.eq(modified, file.getLastModified())) {
					response.setStatus(RestfulResponse.STATUS_NOT_MODIFIED);
					response.openOutput().close();
					return;
				}
			}
			response.setHeader("Last-Modified", file.getLastModified());
		}
		if (!StringUtil.isEmpty(type)) {
			response.setHeader("Content-Type", type);
		}
		response.setStatus(RestfulResponse.STATUS_OK);
		try (OutputStream out = response.openOutput(); InputStream in = file.getStream()) {
			if (in instanceof FileInputStream && out instanceof WritableByteChannel) {
				// NIO transfer
				FileChannel channel = ((FileInputStream) in).getChannel();
				channel.transferTo(0, channel.size(), (WritableByteChannel) out);
			} else if (in instanceof ByteArrayInputStreamExt) {
				// bytes
				@SuppressWarnings("resource")
				ByteArrayInputStreamExt bytes = (ByteArrayInputStreamExt) in;
				out.write(bytes.getBytes(), bytes.getPos(), bytes.available());
			} else {
				byte[] bs = new byte[1024];
				int l;
				while (-1 != (l = in.read(bs))) {
					out.write(bs, 0, l);
				}
			}
		}
	}

	@Override
	public void timeout(RestfulRequest request, RestfulResponse response) throws IOException {

	}

	private String getVersion() {
		if (null == m_Version) {
			m_Version = VersionUtil.getMainVersionByJar(getClass());
			if (StringUtil.isEmpty(m_Version)) {
				m_Version = VersionUtil.getMainVersionByPom();
			}
		}
		return m_Version;
	}

	public static String getSuffix(String name) {
		int index = name.lastIndexOf('.');
		if (index < 0) {
			return null;
		}
		int split = name.lastIndexOf('\\');
		if (split > index) {
			return null;
		}
		return name.substring(index);
	}

	/* 获取主机 */
	private static String getHost(RestfulRequest request) {
		return getHeader(request, "Host");
	}

	/* 获取头信息 */
	private static String getHeader(RestfulRequest request, String name) {
		Dictionary<String, String> headers = request.getHeaders();
		if (null == headers) {
			return "";
		}
		return headers.get(name);
	}

	/* 读取文件 */
	private Resource readFile(String host, String uri) throws IOException {
		HostRoutes r = get(m_Routes, host);
		if (null == r) {
			r = m_DefaultRoute;
		}
		if (null == r) {
			return null;
		}
		return r.findFile(uri);
	}

	/* 获取route */
	private HostRoutes get(Map<String, HostRoutes> routes, String host) {
		if (m_RegEnable) {
			for (HostRoutes r : routes.values()) {
				if (Pattern.matches(r.getName(), host)) {
					return r;
				}
			}
			return null;
		} else if (m_AntEnable) {
			for (HostRoutes r : routes.values()) {
				if (AntPathPattern.match(r.getName(), host)) {
					return r;
				}
			}
			return null;
		} else {
			return routes.get(StringUtil.toString(host));
		}
	}

}
