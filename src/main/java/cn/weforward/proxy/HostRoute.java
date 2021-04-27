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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import cn.weforward.common.util.AntPathPattern;
import cn.weforward.common.util.NumberUtil;
import cn.weforward.common.util.StringUtil;
import cn.weforward.common.util.UrlUtil;
import cn.weforward.proxy.exception.HttpException;
import cn.weforward.proxy.util.FileResources;

/**
 * 主机路由
 * 
 * @author daibo
 *
 */
public class HostRoute {
	/** 主机 */
	protected String m_Host;
	/** 根目录 */
	protected String m_Root;
	/** 路径 */
	protected String m_Path;
	/** 启用ant匹配 */
	private boolean m_AntEnable;
	/** 启用正则表达式匹配 */
	private boolean m_RegEnable;
	/** 指定版本 */
	protected String m_Version = "latest";
	/** 主页后缀 */
	private static List<String> INDEX_SUFFIXS = Arrays.asList(".jsp", ".jspx");
	/** 自动根据路径映射 */
	protected boolean m_AutoMappper;
	/** 变量 */
	protected List<Val> m_Vals;

	/**
	 * 构造
	 * 
	 * @param host 主机
	 * @param root 根路径
	 */
	public HostRoute(String host, String root) {
		m_Host = host;
		m_Root = root;
		m_Vals = new ArrayList<>();
		if (null != root) {
			for (int i = 0; i < root.length(); i++) {
				char ch = root.charAt(i);
				if (ch == '$' && i + 1 < root.length() && root.charAt(i + 1) == '{') {
					StringBuilder sb = new StringBuilder();
					for (int j = i + 2; j < root.length(); j++) {
						char childch = root.charAt(j);
						if (childch == '}') {
							break;
						}
						sb.append(childch);
					}
					try {
						int num = Integer.parseInt(sb.toString());
						m_Vals.add(new Val(num));
					} catch (NumberFormatException e) {

					}
				}
			}
		}
	}

	/**
	 * 名称
	 * 
	 * @return 名称
	 */
	public String getName() {
		return m_Host;
	}

	/**
	 * 指定版本
	 * 
	 * @param version 版本号
	 */
	public void setVersion(String version) {
		m_Version = version;
	}

	/**
	 * 匹配的路径，默认前缀匹配，可通过set方法启用ant匹配或正则表达式匹配
	 * 
	 * @param path 路径
	 */
	public void setPath(String path) {
		m_Path = path;
	}

	/**
	 * 启用ant匹配
	 * 
	 * @param enable 是否启用
	 * 
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
	 * 启用自动映射
	 * 
	 * @param enable 是否启用
	 */
	public void setAutoMappper(boolean enable) {
		m_AutoMappper = enable;
	}

	/**
	 * 查找文件
	 * 
	 * @param uri 资源地址
	 * @return 资源
	 * @throws IOException IO异常
	 */
	public Resource findFile(String uri) throws IOException {
		if (StringUtil.isEmpty(uri)) {
			return null;
		}
		String root;
		if (m_AutoMappper) {
			int index = uri.indexOf("/", 1);
			if (index == -1) {
				return null;
			}
			String path = uri.substring(0, index + 1);
			uri = uri.substring(index + 1);
			root = m_Root + path;
		} else {
			if (!StringUtil.isEmpty(m_Path) && !m_AntEnable && !m_RegEnable) {
				int offset = m_Path.length();
				uri = uri.substring(offset);
			}
			root = m_Root;
			if (!m_Vals.isEmpty()) {
				String[] arr = uri.split("/");
				for (Val v : m_Vals) {
					if (v.m_Index < arr.length) {
						root = root.replace(v.m_Name, arr[v.m_Index]);
					}

				}
			}

		}
		if (!FileResources.get(root).exists()) {
			throw new HttpException(410, "未找到根目录");
		}
		if (isResource(uri)) {
			uri = UrlUtil.decodeUrl(uri);
			if (uri.charAt(0) == '/') {
				uri = uri.substring(1);
			}
			if (isVersionStart(uri)) {
				return FileResources.get(root + uri);
			} else {
				return FileResources.get(root + m_Version + "/" + uri);
			}
		} else {
			uri = "index.html";
			if (StringUtil.isEmpty(m_Version)) {
				return FileResources.get(root + "/" + uri);
			} else {
				return FileResources.get(root + m_Version + "/" + uri);
			}
		}

	}

	private boolean isVersionStart(String uri) {
		int index = uri.indexOf("/");
		if (index < 0) {
			return false;
		}
		for (int i = 0; i < index; i++) {
			char ch = uri.charAt(i);
			if (!NumberUtil.isNumber(ch) && ch != '.') {
				return false;
			}
		}
		return true;
	}

	/**
	 * 是否匹配路径
	 * 
	 * @param uri 资源地址
	 * @return 是否匹配
	 */
	public boolean matchPath(String uri) {
		if (StringUtil.isEmpty(m_Path)) {
			return true;
		}
		if (m_RegEnable) {
			return Pattern.matches(m_Path, uri);
		} else if (m_AntEnable) {
			return AntPathPattern.match(m_Path, uri);
		} else {
			return uri.startsWith(m_Path);
		}
	}

	/***
	 * 是否为资源
	 * 
	 * @param uri 资源地址
	 * @return 是否匹配
	 */
	private boolean isResource(String uri) {
		String suffix = HtmlServer.getSuffix(uri);
		if (suffix == null) {
			return false;
		}
		return !INDEX_SUFFIXS.contains(suffix);
	}

	static class Val {
		String m_Name;

		int m_Index;

		public Val(int num) {
			m_Name = "${" + num + "}";
			m_Index = num;
		}

	}

	@Override
	public String toString() {
		return m_Host + (StringUtil.isEmpty(m_Path) ? "/*" : m_Path);
	}
}
