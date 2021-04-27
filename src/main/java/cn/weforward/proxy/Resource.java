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
import java.io.InputStream;

/**
 * 资源
 * 
 * @author daibo
 *
 */
public interface Resource {
	/**
	 * 名称
	 * 
	 * @return 名称
	 */
	String getName();

	/**
	 * 是否存在
	 * 
	 * @return 是否存在
	 */
	boolean exists();

	/**
	 * 最后修改时间
	 * 
	 * @return 时间
	 */
	String getLastModified();

	/**
	 * 流
	 * 
	 * @return 流
	 * @throws IOException IO异常
	 */
	InputStream getStream() throws IOException;

}
