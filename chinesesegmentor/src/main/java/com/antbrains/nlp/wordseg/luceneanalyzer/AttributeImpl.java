package com.antbrains.nlp.wordseg.luceneanalyzer;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.ref.WeakReference;
import java.util.LinkedList;

 
public abstract class AttributeImpl implements Cloneable, Attribute {
 
	public abstract void clear();
 
	public final String reflectAsString(final boolean prependAttClass) {
		final StringBuilder buffer = new StringBuilder();
		reflectWith(new AttributeReflector() {
			public void reflect(Class<? extends Attribute> attClass, String key, Object value) {
				if (buffer.length() > 0) {
					buffer.append(',');
				}
				if (prependAttClass) {
					buffer.append(attClass.getName()).append('#');
				}
				buffer.append(key).append('=').append((value == null) ? "null" : value);
			}
		});
		return buffer.toString();
	}
 
	public void reflectWith(AttributeReflector reflector) {
		final Class<? extends AttributeImpl> clazz = this.getClass();
		final LinkedList<WeakReference<Class<? extends Attribute>>> interfaces = AttributeSource
				.getAttributeInterfaces(clazz);
		if (interfaces.size() != 1) {
			throw new UnsupportedOperationException(clazz.getName()
					+ " implements more than one Attribute interface, the default reflectWith() implementation cannot handle this.");
		}
		final Class<? extends Attribute> interf = interfaces.getFirst().get();
		final Field[] fields = clazz.getDeclaredFields();
		try {
			for (int i = 0; i < fields.length; i++) {
				final Field f = fields[i];
				if (Modifier.isStatic(f.getModifiers()))
					continue;
				f.setAccessible(true);
				reflector.reflect(interf, f.getName(), f.get(this));
			}
		} catch (IllegalAccessException e) {
			// this should never happen, because we're just accessing fields
			// from 'this'
			throw new RuntimeException(e);
		}
	}
 
	public abstract void copyTo(AttributeImpl target);
 
	@Override
	public Object clone() {
		Object clone = null;
		try {
			clone = super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // shouldn't happen
		}
		return clone;
	}
}
