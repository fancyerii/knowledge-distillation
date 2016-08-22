package com.antbrains.sc.tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class ConfigReader {
	static Properties props;
	protected static Logger logger = Logger.getLogger(ConfigReader.class);
	static {
		reload(null);
	}

	public static void reload(String confDir) {
		props = new Properties();
		if (confDir == null)
			return;
		InputStream is = ConfigReader.class.getClassLoader().getResourceAsStream(confDir + "/cfg.properties");
		if (is == null) {
			logger.error("Can't find cfg.properties in classpath " + confDir);
			return;
		}

		try {
			props.load(is);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static String getProp(String key) {
		return getProp(key, null);
	}

	public static String getProp(String key, String defaultValue) {
		String v = (String) props.get(key);
		if (v == null)
			return defaultValue;
		return v;
	}

	public static void main(String[] args) {
		String s = ConfigReader.getProp("baikeInfoPath");
		System.out.println(s);
	}
}
