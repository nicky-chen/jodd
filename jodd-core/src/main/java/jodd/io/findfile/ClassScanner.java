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

package jodd.io.findfile;

import jodd.io.FileNameUtil;
import jodd.io.FileUtil;
import jodd.io.StreamUtil;
import jodd.io.ZipUtil;
import jodd.util.ArraysUtil;
import jodd.util.ClassLoaderUtil;
import jodd.util.Consumers;
import jodd.util.StringUtil;
import jodd.util.inex.InExRules;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static jodd.util.inex.InExRuleMatcher.WILDCARD_PATH_RULE_MATCHER;
import static jodd.util.inex.InExRuleMatcher.WILDCARD_RULE_MATCHER;


/**
 * Super class scanner.
 */
public class ClassScanner {

	private static final String CLASS_FILE_EXT = ".class";
	private static final String JAR_FILE_EXT = ".jar";

	public static ClassScanner get() {
		return new ClassScanner();
	}


	// ---------------------------------------------------------------- excluded jars

	/**
	 * Array of system jars that are excluded from the search.
	 * By default, these paths are common for linux, windows and mac.
	 */
	protected static String[] systemJars = new String[] {
		"**/jre/lib/*.jar",
		"**/jre/lib/ext/*.jar",
		"**/Java/Extensions/*.jar",
		"**/Classes/*.jar"
	};

	protected final InExRules<String, String, String> rulesJars = createJarRules();

	/**
	 * Creates JAR rules. By default, excludes all system jars.
	 */
	protected InExRules<String, String, String> createJarRules() {
		InExRules<String, String, String> rulesJars = new InExRules<>(WILDCARD_PATH_RULE_MATCHER);

		for (String systemJar : systemJars) {
			rulesJars.exclude(systemJar);
		}

		return rulesJars;
	}

	/**
	 * Specify excluded jars.
	 */
	public ClassScanner excludeJars(String... excludedJars) {
		for (String excludedJar : excludedJars) {
			rulesJars.exclude(excludedJar);
		}
		return this;
	}

	/**
	 * Specify included jars.
	 */
	public ClassScanner includeJars(String... includedJars) {
		for (String includedJar : includedJars) {
			rulesJars.include(includedJar);
		}
		return this;
	}

	/**
	 * Sets white/black list mode for jars.
	 */
	public ClassScanner includeAllJars(boolean blacklist) {
		if (blacklist) {
			rulesJars.blacklist();
		} else {
			rulesJars.whitelist();
		}
		return this;
	}

	/**
	 * Sets white/black list mode for jars.
	 */
	public ClassScanner excludeAllJars(boolean whitelist) {
		if (whitelist) {
			rulesJars.whitelist();
		} else {
			rulesJars.blacklist();
		}
		return this;
	}

	// ---------------------------------------------------------------- included entries

	protected final InExRules<String, String, String> rulesEntries = createEntriesRules();

	protected InExRules<String, String, String> createEntriesRules() {
		return new InExRules<>(WILDCARD_RULE_MATCHER);
	}

	/**
	 * Sets included set of names that will be considered during configuration.
	 * @see InExRules
	 */
	public ClassScanner includeEntries(String... includedEntries) {
		for (String includedEntry : includedEntries) {
			rulesEntries.include(includedEntry);
		}
		return this;
	}

	/**
	 * Sets white/black list mode for entries.
	 */
	public ClassScanner includeAllEntries(boolean blacklist) {
		if (blacklist) {
			rulesEntries.blacklist();
		} else {
			rulesEntries.whitelist();
		}
		return this;
	}
	/**
	 * Sets white/black list mode for entries.
	 */
	public ClassScanner excludeAllEntries(boolean whitelist) {
		if (whitelist) {
			rulesEntries.whitelist();
		} else {
			rulesEntries.blacklist();
		}
		return this;
	}

	/**
	 * Sets excluded names that narrows included set of packages.
	 * @see InExRules
	 */
	public ClassScanner excludeEntries(String... excludedEntries) {
		for (String excludedEntry : excludedEntries) {
			rulesEntries.exclude(excludedEntry);
		}
		return this;
	}

	public ClassScanner smartModeEntries() {
		rulesEntries.smartMode();
		return this;
	}

	// ---------------------------------------------------------------- implementation

	/**
	 * If set to <code>true</code> all files will be scanned and not only classes.
	 */
	protected boolean includeResources;
	/**
	 * If set to <code>true</code> exceptions for entry scans are ignored.
	 */
	protected boolean ignoreException;

	public ClassScanner includeResources(boolean includeResources) {
		this.includeResources = includeResources;
		return this;
	}

	/**
	 * Sets if exceptions during scanning process should be ignored or not.
	 */
	public ClassScanner ignoreException(boolean ignoreException) {
		this.ignoreException = ignoreException;
		return this;
	}

	// ---------------------------------------------------------------- scan


	/**
	 * Returns <code>true</code> if some JAR file has to be accepted.
	 */
	protected boolean acceptJar(File jarFile) {
		String path = jarFile.getAbsolutePath();
		path = FileNameUtil.separatorsToUnix(path);

		return rulesJars.match(path);
	}

	/**
	 * Scans single URL for classes and jar files.
	 * Callback {@link #onEntry(EntryData)} is called on
	 * each class name.
	 */
	protected void scanUrl(URL url) {
		File file = FileUtil.toFile(url);
		if (file == null) {
			if (!ignoreException) {
				throw new FindFileException("URL is not a valid file: " + url);
			}
		}
		scanPath(file);
	}

	/**
	 * Scans single path.
	 */
	protected void scanPath(File file) {
		String path = file.getAbsolutePath();

		if (StringUtil.endsWithIgnoreCase(path, JAR_FILE_EXT)) {
			if (!acceptJar(file)) {
				return;
			}
			scanJarFile(file);
		} else if (file.isDirectory()) {
			scanClassPath(file);
		}
	}

	// ---------------------------------------------------------------- internal

	/**
	 * Scans classes inside single JAR archive. Archive is scanned as a zip file.
	 * @see #onEntry(EntryData)
	 */
	protected void scanJarFile(File file) {
		ZipFile zipFile;
		try {
			zipFile = new ZipFile(file);
		} catch (IOException ioex) {
			if (!ignoreException) {
				throw new FindFileException("Invalid zip: " + file.getName(), ioex);
			}
			return;
		}
		Enumeration entries = zipFile.entries();
		while (entries.hasMoreElements()) {
			ZipEntry zipEntry = (ZipEntry) entries.nextElement();
			String zipEntryName = zipEntry.getName();
			try {
				if (StringUtil.endsWithIgnoreCase(zipEntryName, CLASS_FILE_EXT)) {
					String entryName = prepareEntryName(zipEntryName, true);
					EntryData entryData = new EntryData(entryName, zipFile, zipEntry);
					try {
						scanEntry(entryData);
					} finally {
						entryData.closeInputStream();
					}
				} else if (includeResources) {
					String entryName = prepareEntryName(zipEntryName, false);
					EntryData entryData = new EntryData(entryName, zipFile, zipEntry);
					try {
						scanEntry(entryData);
					} finally {
						entryData.closeInputStream();
					}
				}
			} catch (RuntimeException rex) {
				if (!ignoreException) {
					ZipUtil.close(zipFile);
					throw rex;
				}
			}
		}
		ZipUtil.close(zipFile);
	}

	/**
	 * Scans single classpath directory.
	 * @see #onEntry(EntryData)
	 */
	protected void scanClassPath(File root) {
		String rootPath = root.getAbsolutePath();
		if (!rootPath.endsWith(File.separator)) {
			rootPath += File.separatorChar;
		}

		FindFile ff = new FindFile().includeDirs(false).recursive(true).searchPath(rootPath);
		File file;
		while ((file = ff.nextFile()) != null) {
			String filePath = file.getAbsolutePath();
			try {
				if (StringUtil.endsWithIgnoreCase(filePath, CLASS_FILE_EXT)) {
					scanClassFile(filePath, rootPath, file, true);
				} else if (includeResources) {
					scanClassFile(filePath, rootPath, file, false);
				}
			} catch (RuntimeException rex) {
				if (!ignoreException) {
					throw rex;
				}
			}
		}
	}

	protected void scanClassFile(String filePath, String rootPath, File file, boolean isClass) {
		if (StringUtil.startsWithIgnoreCase(filePath, rootPath)) {
			String entryName = prepareEntryName(filePath.substring(rootPath.length()), isClass);
			EntryData entryData = new EntryData(entryName, file);
			try {
				scanEntry(entryData);
			} finally {
				entryData.closeInputStream();
			}
		}
	}

	/**
	 * Prepares resource and class names. For classes, it strips '.class' from the end and converts
	 * all (back)slashes to dots. For resources, it replaces all backslashes to slashes.
	 */
	protected String prepareEntryName(String name, boolean isClass) {
		String entryName = name;
		if (isClass) {
			entryName = name.substring(0, name.length() - 6);		// 6 == ".class".length()
			entryName = StringUtil.replaceChar(entryName, '/', '.');
			entryName = StringUtil.replaceChar(entryName, '\\', '.');
		} else {
			entryName = '/' + StringUtil.replaceChar(entryName, '\\', '/');
		}
		return entryName;
	}

	/**
	 * Returns <code>true</code> if some entry name has to be accepted.
	 * @see #prepareEntryName(String, boolean)
	 * @see #scanEntry(EntryData)
	 */
	protected boolean acceptEntry(String entryName) {
		return rulesEntries.match(entryName);
	}

	/**
	 * If entry name is {@link #acceptEntry(String) accepted} invokes {@link #onEntry(EntryData)} a callback}.
	 */
	protected void scanEntry(EntryData entryData) {
		if (!acceptEntry(entryData.name())) {
			return;
		}
		try {
			onEntry(entryData);
		} catch (Exception ex) {
			throw new FindFileException("Scan entry error: " + entryData, ex);
		}
	}


	// ---------------------------------------------------------------- callback

	private Consumers<EntryData> entryDataConsumers = Consumers.empty();

	public ClassScanner onEntry(Consumer<EntryData> entryDataConsumer) {
		entryDataConsumers.add(entryDataConsumer);
		return this;
	}

	/**
	 * Called during classpath scanning when class or resource is found.
	 * <ul>
	 * <li>Class name is java-alike class name (pk1.pk2.class) that may be immediately used
	 * for dynamic loading.</li>
	 * <li>Resource name starts with '\' and represents either jar path (\pk1/pk2/res) or relative file path (\pk1\pk2\res).</li>
	 * </ul>
	 *
	 * <code>InputStream</code> is provided by InputStreamProvider and opened lazy.
	 * Once opened, input stream doesn't have to be closed - this is done by this class anyway.
	 */
	protected void onEntry(EntryData entryData) {
		entryDataConsumers.accept(entryData);
	}

	// ---------------------------------------------------------------- utilities

	/**
	 * Returns type signature bytes used for searching in class file.
	 */
	public static byte[] bytecodeSignatureOfType(Class type) {
		String name = 'L' + type.getName().replace('.', '/') + ';';
		return name.getBytes();
	}

	/**
	 * Loads class by its name. If {@link #ignoreException} is set,
	 * no exception is thrown, but <code>null</code> is returned.
	 */
	public Class loadClass(String className) throws ClassNotFoundException {
		try {
			return ClassLoaderUtil.loadClass(className);
		} catch (ClassNotFoundException | Error cnfex) {
			if (ignoreException) {
				return null;
			}
			throw cnfex;
		}
	}

	// ---------------------------------------------------------------- provider

	/**
	 * Provides input stream on demand. Input stream is not open until get().
	 */
	public static class EntryData {

		private final File file;
		private final ZipFile zipFile;
		private final ZipEntry zipEntry;
		private final String name;

		EntryData(String name, ZipFile zipFile, ZipEntry zipEntry) {
			this.name = name;
			this.zipFile = zipFile;
			this.zipEntry = zipEntry;
			this.file = null;
			inputStream = null;
		}
		EntryData(String name, File file) {
			this.name = name;
			this.file = file;
			this.zipEntry = null;
			this.zipFile = null;
			inputStream = null;
		}

		private InputStream inputStream;

		/**
		 * Returns entry name.
		 */
		public String name() {
			return name;
		}

		/**
		 * Returns <code>true</code> if archive.
		 */
		public boolean isArchive() {
			return zipFile != null;
		}

		/**
		 * Returns archive name or <code>null</code> if entry is not inside archived file.
		 */
		public String archiveName() {
			if (zipFile != null) {
				return zipFile.getName();
			}
			return null;
		}

		/**
		 * Returns <code>true</code> if class contains {@link #bytecodeSignatureOfType(Class) type signature}.
		 * It searches the class content for bytecode signature. This is the fastest way of finding if come
		 * class uses some type. Please note that if signature exists it still doesn't means that class uses
		 * it in expected way, therefore, class should be loaded to complete the scan.
		 */
		public boolean isTypeSignatureInUse(byte[] bytes) {
			openInputStream();

			try {
				byte[] data = StreamUtil.readBytes(inputStream);
				int index = ArraysUtil.indexOf(data, bytes);
				return index != -1;
			} catch (IOException ioex) {
				throw new FindFileException("Read error", ioex);
			}
		}

		/**
		 * Opens zip entry or plain file and returns its input stream.
		 */
		public InputStream openInputStream() {
			if (inputStream != null) {
				return inputStream;
			}
			if (zipFile != null && zipEntry != null) {
				try {
					inputStream = zipFile.getInputStream(zipEntry);
					return inputStream;
				} catch (IOException ioex) {
					throw new FindFileException("Input stream error: '" + zipFile.getName()
						+ "', entry: '" + zipEntry.getName() + "'." , ioex);
				}
			}
			if (file != null) {
				try {
					inputStream = new FileInputStream(file);
					return inputStream;
				} catch (FileNotFoundException fnfex) {
					throw new FindFileException("Unable to open: " + file.getAbsolutePath(), fnfex);
				}
			}
			throw new FindFileException("Unable to open stream: " + name());
		}

		/**
		 * Closes input stream if opened.
		 */
		public void closeInputStream() {
			if (inputStream == null) {
				return;
			}
			StreamUtil.close(inputStream);
			inputStream = null;
		}

		@Override
		public String toString() {
			return "EntryData{" + name + '\'' +'}';
		}
	}

	// ---------------------------------------------------------------- public scanning

	/**
	 * Scans URLs. If (#ignoreExceptions} is set, exceptions
	 * per one URL will be ignored and loops continues.
	 */
	public void scan(URL... urls) {
		for (URL path : urls) {
			scanUrl(path);
		}
	}

	/**
	 * Scans {@link jodd.util.ClassLoaderUtil#getDefaultClasspath() default class path}.
	 */
	public void scanDefaultClasspath() {
		scan(ClassLoaderUtil.getDefaultClasspath());
	}

	/**
	 * Scans provided paths.
	 */
	public void scan(File... paths) {
		for (File path : paths) {
			scanPath(path);
		}
	}

	/**
	 * Scans provided paths.
	 */
	public void scan(String... paths) {
		for (String path : paths) {
			scanPath(new File(path));
		}
	}

}
