package com.antbrains.httpclientfetcher;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;

public class FileTools {
	protected static Logger logger = Logger.getLogger(FileTools.class);

	public static void appendLine(String filePath, String line) throws IOException {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, true));
			bw.write(line + "\n");
			bw.close();
		} catch (IOException e) {
			throw e;
		}
	}

	public static void writeFile(String filePath, String content) throws Exception {
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(filePath));
			bw.write(content);
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				if (bw != null)
					bw.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	public static void writeList(String filePath, List<String> list, String encoding) throws IOException {
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), encoding));
			for (String line : list) {
				bw.write(line + "\n");
			}
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if (bw != null)
					bw.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	public static void writeCollection(String filePath, Collection<String> list, String encoding) throws IOException {
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), encoding));
			for (String line : list) {
				bw.write(line + "\n");
			}
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if (bw != null)
					bw.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	public static void writeFile(String filePath, String content, String encoding) throws IOException {
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), encoding));
			bw.write(content);
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if (bw != null)
					bw.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	public static String getFileName(String shortName) {
		if (shortName == null)
			return null;
		int index = shortName.lastIndexOf(".");
		if (index == -1)
			return shortName;
		return shortName.substring(0, index);
	}

	public static String getFileName(File file) {
		return getFileName(file.getName());
	}

	public static String readFile(String filePath) throws IOException {
		StringBuilder sb = new StringBuilder("");
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filePath));
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}

		}
		return sb.toString();
	}

	public static List<String> read2List(InputStream in) throws Exception {
		BufferedReader br = null;
		List<String> lines = new ArrayList<String>();
		try {
			String line;
			br = new BufferedReader(new InputStreamReader(in));
			while ((line = br.readLine()) != null) {
				lines.add(line);
			}
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
		return lines;
	}

	public static List<String> read2List(InputStream in, String encoding) throws Exception {
		BufferedReader br = null;
		List<String> lines = new ArrayList<String>();
		try {
			String line;
			br = new BufferedReader(new InputStreamReader(in, encoding));
			while ((line = br.readLine()) != null) {
				lines.add(line);
			}
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
		return lines;
	}

	public static List<String> readFile2List(String filePath) throws IOException {
		List<String> lines = new ArrayList<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filePath));
			String line;
			while ((line = br.readLine()) != null) {
				lines.add(line);
			}
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}

		}
		return lines;
	}

	public static List<String> readFile2List(String filePath, String encoding) throws IOException {
		List<String> lines = new ArrayList<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), encoding));
			String line;
			while ((line = br.readLine()) != null) {
				lines.add(line);
			}
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}

		}
		return lines;
	}

	public static String readFile(String filePath, String encoding) throws IOException {
		StringBuilder sb = new StringBuilder("");
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), encoding));
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}

		}
		return sb.substring(0, sb.length() - 1);
	}

	public static void makeDirs(String path) {
		File dir = new File(path);
		dir.mkdirs();
	}
}
