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

package jodd.mail;

import jodd.io.FileNameUtil;
import jodd.io.FileUtil;
import jodd.io.StreamUtil;
import jodd.mail.att.ByteArrayAttachment;
import jodd.mail.att.FileAttachment;
import jodd.mail.att.InputStreamAttachment;
import jodd.util.net.MimeTypes;

import java.io.*;

/**
 * Helper class for convenient attachment creation.
 */
public class EmailAttachmentBuilder {

	private byte[] sourceBytes;
	private File sourceFile;
	private InputStream sourceInputStream;

	private String contentType;
	private String name;
	private String contentId;
	private boolean inline;

	protected EmailAttachmentBuilder() {}

	// ---------------------------------------------------------------- bytes

	public EmailAttachmentBuilder bytes(byte[] bytes) {
		checkIfSourceSpecified();
		sourceBytes = bytes;
		return this;
	}

	public EmailAttachmentBuilder bytes(InputStream inputStream) {
		checkIfSourceSpecified();
		try {
			sourceBytes = StreamUtil.readBytes(inputStream);
		} catch (IOException ioex) {
			throw new MailException(ioex);
		}
		return this;
	}

	public EmailAttachmentBuilder bytes(File file) {
		checkIfSourceSpecified();
		try {
			sourceBytes = FileUtil.readBytes(file);
			name = file.getName();
		} catch (IOException ioex) {
			throw new MailException(ioex);
		}
		return this;
	}

	// ---------------------------------------------------------------- file

	public EmailAttachmentBuilder file(File file) {
		checkIfSourceSpecified();
		sourceFile = file;
		name = file.getName();
		return this;
	}

	public EmailAttachmentBuilder file(String fileName) {
		file(new File(fileName));
		return this;
	}

	// ---------------------------------------------------------------- stream

	public EmailAttachmentBuilder stream(InputStream inputStream) {
		checkIfSourceSpecified();
		sourceInputStream = inputStream;
		return this;
	}

	public EmailAttachmentBuilder stream(File file) {
		checkIfSourceSpecified();
		try {
			sourceInputStream = new FileInputStream(file);
			name = file.getName();
		} catch (FileNotFoundException fnfex) {
			throw new MailException(fnfex);
		}
		return this;
	}

	// ---------------------------------------------------------------- properties

	public EmailAttachmentBuilder setName(String name) {
		this.name = name;
		return this;
	}

	public EmailAttachmentBuilder setContentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	public EmailAttachmentBuilder setContentId(String contentId) {
		this.contentId = contentId;
		return this;
	}

	protected EmailAttachmentBuilder setContentIdFromNameIfMissing() {
		if (this.contentId == null) {
			if (name != null) {
				this.contentId = FileNameUtil.getName(name);
			}
		}
		return this;
	}

	public EmailAttachmentBuilder setInline(boolean inline) {
		this.inline = inline;
		return this;
	}

	// ---------------------------------------------------------------- factory

	/**
	 * Creates {@link EmailAttachment}.
	 */
	public EmailAttachment create() {
		if (sourceBytes != null) {
			return createByteArrayAttachment();
		}
		if (sourceInputStream != null) {
			return createInputStreamAttachment();
		}
		if (sourceFile != null) {
			return createFileAttachment();
		}
		throw new MailException("No source");
	}

	/**
	 * Checks if no source content is specified. Throws
	 * an exception if content is already specified.
	 */
	protected void checkIfSourceSpecified() {
		int count = 0;
		if (sourceFile != null) {
			count++;
		}
		if (sourceBytes != null) {
			count++;
		}
		if (sourceInputStream != null) {
			count++;
		}

		if (count > 0) {
			throw new MailException("Attachment source already specified");
		}
	}

	/**
	 * Creates {@link ByteArrayAttachment}.
	 */
	protected ByteArrayAttachment createByteArrayAttachment() {
		String name = this.name;
		String contentType = resolveContentType();

		return new ByteArrayAttachment(sourceBytes, contentType, name, contentId, inline);
	}

	/**
	 * Creates {@link InputStreamAttachment}.
	 */
	protected InputStreamAttachment createInputStreamAttachment() {
		String name = this.name;
		String contentType = resolveContentType();

		return new InputStreamAttachment(sourceInputStream, contentType, name, contentId, inline);
	}

	/**
	 * Creates {@link FileAttachment}. Content type is ignored,
	 * as it is set by <code>javax.mail</code>.
	 */
	protected FileAttachment createFileAttachment() {
		String name = this.name;

		return new FileAttachment(sourceFile, name, contentId, inline);
	}

	// ---------------------------------------------------------------- tools

	/**
	 * Resolves content type from all data.
	 */
	protected String resolveContentType() {
		if (contentType != null) {
			return contentType;
		}
		if (name == null) {
			return MimeTypes.MIME_APPLICATION_OCTET_STREAM;
		}

		String extension = FileNameUtil.getExtension(name);
		return MimeTypes.getMimeType(extension);
	}

}
