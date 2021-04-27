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
package cn.weforward.proxy.exception;

import java.io.IOException;

import cn.weforward.protocol.aio.http.HttpConstants;

public class HttpException extends IOException {
	private static final long serialVersionUID = 1L;
	protected int m_Code;

	public HttpException(String message, Throwable cause) {
		super(message, cause);
	}

	public HttpException(int code, String message) {
		super(message);
		m_Code = code;
	}

	public HttpException(String message) {
		super(message);
	}

	public HttpException(Throwable cause) {
		super(cause);
	}

	public int getCode() {
		return m_Code;
	}

	/**
	 * 系统是否不可用/不可达
	 * 
	 * @return true/false
	 */
	public boolean isUnavailable() {
		return (HttpConstants.BAD_GATEWAY == m_Code || HttpConstants.SERVICE_UNAVAILABLE == m_Code);
	}

	@Override
	public String toString() {
		if (0 != m_Code) {
			String msg = getLocalizedMessage();
			if (null != msg && msg.length() > 0) {
				StringBuilder sb = new StringBuilder(msg.length() + 4 + 1);
				sb.append(m_Code).append(' ').append(msg);
				return sb.toString();
			}
			return String.valueOf(m_Code);
		}
		return getLocalizedMessage();
	}
}
