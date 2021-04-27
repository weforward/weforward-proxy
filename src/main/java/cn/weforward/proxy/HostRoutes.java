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
import java.util.LinkedList;

import cn.weforward.common.util.StringUtil;

/**
 * 主机路由集合
 * 
 * @author daibo
 *
 */
public class HostRoutes {
	/** 名称 */
	protected String m_Name;
	/** 路由 */
	protected LinkedList<HostRoute> m_Routes;

	/**
	 * 构造
	 * 
	 * @param r 路由
	 */
	public HostRoutes(HostRoute r) {
		m_Name = r.getName();
		m_Routes = new LinkedList<HostRoute>();
		m_Routes.add(r);
	}

	public String getName() {
		return m_Name;
	}

	/**
	 * 添加路由
	 * 
	 * @param r 路由
	 */
	public void add(HostRoute r) {
		if (!StringUtil.eq(getName(), r.getName())) {
			throw new UnsupportedOperationException(getName() + "≠" + r.getName());
		}
		m_Routes.add(r);
	}

	/***
	 * 读取文件
	 * 
	 * @param uri 资源地址
	 * @return 资源
	 * @throws IOException IO异常
	 */
	public Resource findFile(String uri) throws IOException {
		for (HostRoute r : m_Routes) {
			if (r.matchPath(uri)) {
				return r.findFile(uri);
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return m_Name;
	}
}
