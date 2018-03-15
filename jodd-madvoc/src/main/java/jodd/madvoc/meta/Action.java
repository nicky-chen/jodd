// Copyright (c) 2003-present, Jodd Team (http://jodd.org)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
// this list of conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package jodd.madvoc.meta;

import jodd.madvoc.ActionConfig;
import jodd.madvoc.MadvocConfig;
import jodd.util.StringPool;

import java.lang.annotation.*;

/**
 * Marker for action methods. It is not necessary to mark a method, however, this annotation 
 * may be used to specify non-default action path. Moreover, this annotation may be used
 * to mark custom annotations!
 * @see jodd.madvoc.meta.ActionAnnotationData
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@ActionConfiguredBy(ActionConfig.class)
public @interface Action {

	/**
	 * Marker for empty action method or extension.
	 */
	String NONE = StringPool.HASH;

	// see: http://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol#Request_methods

	String ANY = "";
	String GET = "GET";
	String HEAD = "HEAD";
	String POST = "POST";
	String PUT = "PUT";
	String DELETE = "DELETE";
	String TRACE = "TRACE";
	String OPTIONS = "OPTIONS";
	String CONNECT = "CONNECT";
	String PATCH = "PATCH";

	/**
	 * Action path value. If equals to {@link #NONE} action method name
	 * will not be part of the created action path.
	 */
	String value() default "";

	/**
	 * Action path extension. If equals to {@link #NONE} extension will be not
	 * part of created action path. If empty, default extension will be used
	 * (defined in {@link MadvocConfig}.
	 */
	String extension() default "";

	/**
	 * Defines alias for this action.
	 */
	String alias() default "";

	/**
	 * Defines action method (such as HTTP request method: GET, POST....).
	 * Ignore it or use {@link #ANY} to ignore the method.
	 */
	String method() default "";

	/**
	 * Defines if action has to be called asynchronously
	 * using Servlets 3.0 API.
	 */
	boolean async() default false;

}
