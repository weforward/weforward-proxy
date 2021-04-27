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
import java.io.InputStream;
import java.util.Date;

import cn.weforward.common.util.TimeUtil;
import cn.weforward.proxy.Resource;

/**
 * 字符串资源
 * 
 * @author daibo
 *
 */
public class StringResource implements Resource {
	/** 名称 */
	protected String m_Name;
	/** 最后修改时间 */
	protected String m_LastModified;
	/** 数据 */
	protected byte[] m_Data;

	public StringResource(String name, String data) {
		m_Name = name;
		m_LastModified = TimeUtil.formatGMT(new Date());
		m_Data = data.getBytes();
	}

	@Override
	public String getName() {
		return m_Name;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public String getLastModified() {
		return m_LastModified;
	}

	@Override
	public InputStream getStream() throws IOException {
		return new ByteArrayInputStreamExt(m_Data);
	}

}
