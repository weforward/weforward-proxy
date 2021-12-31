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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.crypto.Base64;
import cn.weforward.common.util.FileUtil;
import cn.weforward.common.util.ListUtil;
import cn.weforward.common.util.NumberUtil;
import cn.weforward.common.util.StringUtil;
import cn.weforward.proxy.util.StringResource;
import cn.weforward.proxy.util.VersionUtil;

/**
 * 服务器入口
 * 
 * @author daibo
 *
 */
public class ServerBoot {
	/** 日志 */
	private static final Logger _Logger = LoggerFactory.getLogger(HtmlServer.class);
	/** 默认模板 */
	private static final String DEFAULT_SERVER_TEMPLATE = "{\"server\": [{\"root\":\"./html/\",\"host\": \"*\",\"routes\": [{\"auto\": \"true\"}]}]}";
	/** 默认模板对象 */
	private static final JSONObject DEFAULT_SERVER = new JSONObject(DEFAULT_SERVER_TEMPLATE);
	/** 默认端口 */
	private static final int DEFAULT_PORT = NumberUtil.toInt(System.getProperty("weforward.port"), 8080);
	/** 默认管理端口 */
	private static final int DEFAULT_MANAGE_PORT = NumberUtil.toInt(System.getProperty("weforward.port2"), 18080);
	/** 默认服务id */
	private static final String DEFAULT_SERVERID = System.getProperty("weforward.serverid", "x00ff");

	private static final String SERVICE_ACCESSID = System.getProperty("weforward.service.accessId");
	private static final String SERVICE_ACCESSKEY = System.getProperty("weforward.service.accessKey");

	/** 不使用缓存的文件后缀 */
	private static List<String> NO_CACHES_FILES = toList(System.getenv("WEFORWARD_NO_CACHES_FILES"), ".html");
	/** 配置链接 */
	private static List<String> CONFIG_URLS = toList(System.getenv("WEFORWARD_CONFIG_URLS"), null);
	/** 配置帐号 */
	private static String CONFIG_USERNAME = System.getenv("WEFORWARD_CONFIG_USERNAME");
	/** 配置密码 */
	private static String CONFIG_PASSWORD = System.getenv("WEFORWARD_CONFIG_PASSWORD");
	/** 主机 host */
	private static final String HOSTS = System.getenv("WEFORWARD_HOSTS");

	/** hyconfig文件 */
	private static Resource WFCONFIG;

	/**
	 * 主入口
	 * 
	 * @param args configfile or configfiledir
	 * @throws Exception 异常
	 */
	public static void main(String[] args) throws Exception {
		String configpath;
		if (null == args || args.length == 0) {
			configpath = "config";
		} else {
			configpath = args[0];
		}
		File file = new File(FileUtil.getAbsolutePath(configpath, null));
		List<File> configs;
		if (!file.isFile()) {
			loadconfig(file);
		}
		if (file.exists()) {
			if (file.isDirectory()) {
				configs = new ArrayList<>();
				for (File f : file.listFiles(JSON_FILTER)) {
					configs.add(f);
				}
			} else {
				configs = Collections.singletonList(file);
			}
		} else {
			configs = Collections.emptyList();
		}
		if (configs.isEmpty()) {
			_Logger.info("no confdir create default");
			Config c = new Config();
			JSONArray val = DEFAULT_SERVER.optJSONArray("server");
			if (null == val) {
				return;
			}
			for (int i = 0; i < val.length(); i++) {
				createServer(val.optJSONObject(i), c);
			}
		}
		for (File f : configs) {
			JSONObject json = load(new FileInputStream(f));
			Config c = new Config();
			JSONArray val = json.optJSONArray("server");
			if (null == val) {
				continue;
			}
			for (int i = 0; i < val.length(); i++) {
				createServer(val.optJSONObject(i), c);
			}
		}
		_Logger.info("start success " + getVersion());
	}

	private static void loadconfig(File dir) {
		if (ListUtil.isEmpty(CONFIG_URLS)) {
			return;
		}
		dir.mkdirs();
		String username = CONFIG_USERNAME;
		String password = CONFIG_PASSWORD;
		for (String url : CONFIG_URLS) {
			if (url.startsWith("http") && url.endsWith(".json")) {
				try {
					String authorization;
					// 生成Basic验证
					if (!StringUtil.isEmpty(username) && !StringUtil.isEmpty(password)) {
						authorization = "Basic " + Base64.encode((username + ":" + password).getBytes());
					} else {
						authorization = "";
					}
					File file = downconfig(dir, url, authorization);
					_Logger.info("成功加载 " + file.getName() + " 配置");
				} catch (Throwable e) {
					_Logger.error("加载" + url + "出错", e);
				}
			}
		}

	}

	private static File downconfig(File dir, String url, String authorization) throws IOException {
		String name = url.substring(url.lastIndexOf('/') + 1);
		HttpURLConnection http = (HttpURLConnection) new URL(url).openConnection();
		http.setConnectTimeout(3 * 1000);
		http.setReadTimeout(10 * 1000);
		http.setRequestMethod("GET");
		if (!StringUtil.isEmpty(authorization)) {
			http.setRequestProperty("authorization", authorization);
		}
		int code = http.getResponseCode();
		if (code != 200) {
			throw new IOException("响应异常:" + code);
		}
		File file = new File(dir, name);
		try (OutputStream out = new FileOutputStream(file); InputStream in = http.getInputStream()) {
			byte[] bs = new byte[1024];
			int l;
			while (-1 != (l = in.read(bs))) {
				out.write(bs, 0, l);
			}
			out.flush();
			out.close();
		}
		return file;

	}

	private static List<String> toList(String value, String defaultValue) {
		if (StringUtil.isEmpty(value)) {
			value = defaultValue;
		}
		if (StringUtil.isEmpty(value)) {
			return Collections.emptyList();
		}
		return Arrays.asList(value.split(";"));
	}

	private static Resource getWfConfig() throws IOException {
		if (null == WFCONFIG && !StringUtil.isEmpty(HOSTS)) {
			StringBuilder sb = new StringBuilder();
			String[] arr = HOSTS.split(";");
			if (arr.length == 1) {
				sb.append("\"");
				sb.append(arr[0]);
				sb.append("\"");
			} else {
				sb.append("\"");
				sb.append(arr[0]);
				sb.append("\"");
				for (int i = 1; i < arr.length; i++) {
					sb.append(",\"");
					sb.append(arr[i]);
					sb.append("\"");
				}
			}
			/** wfconfig文件模板 */
			WFCONFIG = new StringResource("wfconfig.js",
					"window._WEFORWARD_CONFIG={\"hosts\":[" + sb.toString() + "]};");
		}
		return WFCONFIG;
	}

	private static String getVersion() {
		String v = VersionUtil.getImplementationVersionByJar(ServerBoot.class);
		if (StringUtil.isEmpty(v)) {
			v = VersionUtil.getImplementationVersionByPom();
		}
		return v;
	}

	/* json文件过滤 */
	private static final FilenameFilter JSON_FILTER = new FilenameFilter() {

		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(".json");
		}
	};

	/* 创建服务 */
	private static void createServer(JSONObject val, Config config) throws Exception {
		if (null == val) {
			return;
		}
		int port = val.optInt("port", DEFAULT_PORT);
		int managetPort = val.optInt("managePort", DEFAULT_MANAGE_PORT);
		String type = val.optString("type");
		if (StringUtil.isEmpty(type)) {
			type = "html";
		}
		String name = val.optString("name");
		String manageName = val.optString("manageName");
		if (StringUtil.isEmpty(name)) {
			name = type + "-" + port;
		}
		if (StringUtil.isEmpty(manageName)) {
			manageName = "mange-" + port;
		}
		config.root = val.optString("root");
		config.host = val.optString("host");
		List<HostRoute> routes = createRoutes(val.optJSONArray("routes"), config);
		if ("html".equalsIgnoreCase(type)) {
			HtmlServer s = new HtmlServer(name, port);
			s.addConfig(getWfConfig());
			s.setServerid(DEFAULT_SERVERID);
			s.setNoCacheFiles(NO_CACHES_FILES);
			s.setAntEnable(val.optBoolean("ant", false));
			s.setRegEnable(val.optBoolean("reg", false));
			s.setGzipEnabled(val.optBoolean("gzip", false));
			int gms = val.optInt("gzipMinSize", -1);
			if (gms >= 0) {
				s.setGzipMinSize(gms);
			}
			int at = val.optInt("acceptThreads", -1);
			if (at >= 0) {
				s.setAcceptThreads(at);
			}
			int wt = val.optInt("workThreads", -1);
			if (wt >= 0) {
				s.setWorkThreads(wt);
			}
			int idle = val.optInt("idle", -1);
			if (idle >= 0) {
				s.setIdle(idle);
			}
			int bl = val.optInt("backlog", -1);
			if (bl >= 0) {
				s.setBacklog(bl);
			}
			int mhs = val.optInt("maxHttpSize", 0);
			if (mhs > 0) {
				s.setMaxHttpSize(mhs);
			}
			String ka = val.optString("keepAlive");
			if (!StringUtil.isEmpty(ka)) {
				s.setKeepAlive(ka);
			}
			s.setRoutes(routes);
			MangeServer ms = new MangeServer(manageName, managetPort, config.root, SERVICE_ACCESSID, SERVICE_ACCESSKEY);
			ms.start();
		} else {
			throw new IllegalArgumentException("不支持的类型" + type);
		}

	}

	/* 创建路由 */
	private static List<HostRoute> createRoutes(JSONArray array, Config config) {
		if (null == array) {
			return Collections.emptyList();
		}
		List<HostRoute> list = new ArrayList<>();
		for (int i = 0; i < array.length(); i++) {
			List<HostRoute> rs = createRoute(array.optJSONObject(i), config);
			for (HostRoute r : rs) {
				if (null != r) {
					list.add(r);
				}
			}
		}
		return list;
	}

	/* 创建路由 */
	private static List<HostRoute> createRoute(JSONObject val, Config config) {
		if (null == val) {
			return Collections.emptyList();
		}
		String host = val.optString("host");
		String root = val.optString("root");
		String path = val.optString("path");
		if (StringUtil.isEmpty(root)) {
			root = config.root + StringUtil.toString(path);
		} else if (!root.startsWith("/")) {
			root = config.root + "/" + root;
		}
		if (StringUtil.isEmpty(host)) {
			host = config.host;
		}
		String[] hosts = host.split(",");
		List<HostRoute> rs = new ArrayList<>();
		for (String h : hosts) {
			HostRoute r = new HostRoute(h, root);
			if (!StringUtil.isEmpty(path)) {
				r.setPath(path);
			}
			String version = val.optString("version", null);
			if (null != version) {
				r.setVersion(version);
			}
			r.setAntEnable(val.optBoolean("ant", false));
			r.setRegEnable(val.optBoolean("reg", false));
			r.setAutoMappper(val.optBoolean("auto", false));
			rs.add(r);
		}
		return rs;
	}

	/* 加载文件 */
	private static JSONObject load(InputStream in) throws IOException {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(in));
			String line;
			StringBuilder sb = new StringBuilder();
			while (null != (line = reader.readLine())) {
				sb.append(line);
			}
			return new JSONObject(sb.toString());
		} finally {
			if (null != reader) {
				reader.close();
			}
			if (null != in) {
				in.close();
			}
		}
	}

	/**
	 * 配置
	 * 
	 * @author daibo
	 *
	 */
	private static class Config {
		/** 根目录 */
		private String root;
		/** 主机 */
		private String host;

	}
}
