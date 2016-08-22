package com.mobvoi.knowledgegraph.sc;

import org.apache.commons.codec.digest.DigestUtils;

import com.antbrains.sc.tools.ByteArrayWrapper;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration.Strategy;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

public class TestCache {
	public static boolean exist(String url, Cache cache) {

		byte[] md5 = DigestUtils.md5(url);
		ByteArrayWrapper key = new ByteArrayWrapper(md5);
		if (cache.get(key) == null) {
			cache.put(new Element(key, null));
			return false;
		} else {
			return true;
		}

	}

	public static void main(String[] args) {
		{
			CacheManager manager = CacheManager.create();

			// Create a Cache specifying its configuration.
			Cache testCache = new Cache(
					new CacheConfiguration("testCache", 10).memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LFU)
							.eternal(true).timeToLiveSeconds(0).timeToIdleSeconds(0).diskExpiryThreadIntervalSeconds(0)
							.persistence(new PersistenceConfiguration().strategy(Strategy.NONE)));
			manager.addCache(testCache);
			Cache cch = manager.getCache("testCache");
			for (int i = 0; i < 10; i++) {
				cch.put(new Element("" + i, null));
			}

			for (int i = 0; i < 10; i++) {
				Element elt = cch.get("" + i);
				if (elt == null) {
					System.err.println("err");
				}
			}
			for (int i = 0; i < 3; i++) {
				cch.get("" + i);
			}
			for (int i = 4; i < 10; i++) {
				cch.get("" + i);
			}
			System.out.println(cch.getSize());
			cch.put(new Element("20", "20"));
			System.out.println(cch.getSize());
			Element el = cch.get("3");
			if (el != null) {
				System.err.println("err");
			}

			String url = "url1";
			if (exist(url, cch)) {
				System.out.println("err");
			}
			if (!exist(url, cch)) {
				System.out.println("err");
			}
			manager.shutdown();
		}
	}

}
