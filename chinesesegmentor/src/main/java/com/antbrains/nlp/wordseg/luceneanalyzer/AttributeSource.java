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

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.WeakHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

 
public class AttributeSource {
	/**
	 * An AttributeFactory creates instances of {@link AttributeImpl}s.
	 */
	public static abstract class AttributeFactory {
 
		public abstract AttributeImpl createAttributeInstance(Class<? extends Attribute> attClass);
 
		public static final AttributeFactory DEFAULT_ATTRIBUTE_FACTORY = new DefaultAttributeFactory();

		private static final class DefaultAttributeFactory extends AttributeFactory {
			private static final WeakHashMap<Class<? extends Attribute>, WeakReference<Class<? extends AttributeImpl>>> attClassImplMap = new WeakHashMap<Class<? extends Attribute>, WeakReference<Class<? extends AttributeImpl>>>();

			private DefaultAttributeFactory() {
			}

			@Override
			public AttributeImpl createAttributeInstance(Class<? extends Attribute> attClass) {
				try {
					return getClassForInterface(attClass).newInstance();
				} catch (InstantiationException e) {
					throw new IllegalArgumentException(
							"Could not instantiate implementing class for " + attClass.getName());
				} catch (IllegalAccessException e) {
					throw new IllegalArgumentException(
							"Could not instantiate implementing class for " + attClass.getName());
				}
			}

			private static Class<? extends AttributeImpl> getClassForInterface(Class<? extends Attribute> attClass) {
				synchronized (attClassImplMap) {
					final WeakReference<Class<? extends AttributeImpl>> ref = attClassImplMap.get(attClass);
					Class<? extends AttributeImpl> clazz = (ref == null) ? null : ref.get();
					if (clazz == null) {
						try {
							attClassImplMap.put(attClass,
									new WeakReference<Class<? extends AttributeImpl>>(clazz = Class
											.forName(attClass.getName() + "Impl", true, attClass.getClassLoader())
											.asSubclass(AttributeImpl.class)));
						} catch (ClassNotFoundException e) {
							throw new IllegalArgumentException(
									"Could not find implementing class for " + attClass.getName());
						}
					}
					return clazz;
				}
			}
		}
	}
 
	public static final class State implements Cloneable {
		AttributeImpl attribute;
		State next;

		@Override
		public Object clone() {
			State clone = new State();
			clone.attribute = (AttributeImpl) attribute.clone();

			if (next != null) {
				clone.next = (State) next.clone();
			}

			return clone;
		}
	}

	// These two maps must always be in sync!!!
	// So they are private, final and read-only from the outside (read-only
	// iterators)
	private final Map<Class<? extends Attribute>, AttributeImpl> attributes;
	private final Map<Class<? extends AttributeImpl>, AttributeImpl> attributeImpls;
	private final State[] currentState;

	private AttributeFactory factory;
 
	public AttributeSource() {
		this(AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY);
	}
 
	public AttributeSource(AttributeSource input) {
		if (input == null) {
			throw new IllegalArgumentException("input AttributeSource must not be null");
		}
		this.attributes = input.attributes;
		this.attributeImpls = input.attributeImpls;
		this.currentState = input.currentState;
		this.factory = input.factory;
	}
 
	public AttributeSource(AttributeFactory factory) {
		this.attributes = new LinkedHashMap<Class<? extends Attribute>, AttributeImpl>();
		this.attributeImpls = new LinkedHashMap<Class<? extends AttributeImpl>, AttributeImpl>();
		this.currentState = new State[1];
		this.factory = factory;
	}
 
	public final AttributeFactory getAttributeFactory() {
		return this.factory;
	}
 
	public final Iterator<Class<? extends Attribute>> getAttributeClassesIterator() {
		return Collections.unmodifiableSet(attributes.keySet()).iterator();
	}
 
	public final Iterator<AttributeImpl> getAttributeImplsIterator() {
		final State initState = getCurrentState();
		if (initState != null) {
			return new Iterator<AttributeImpl>() {
				private State state = initState;

				public void remove() {
					throw new UnsupportedOperationException();
				}

				public AttributeImpl next() {
					if (state == null)
						throw new NoSuchElementException();
					final AttributeImpl att = state.attribute;
					state = state.next;
					return att;
				}

				public boolean hasNext() {
					return state != null;
				}
			};
		} else {
			return Collections.<AttributeImpl>emptySet().iterator();
		}
	}
 
	private static final WeakHashMap<Class<? extends AttributeImpl>, LinkedList<WeakReference<Class<? extends Attribute>>>> knownImplClasses = new WeakHashMap<Class<? extends AttributeImpl>, LinkedList<WeakReference<Class<? extends Attribute>>>>();

	static LinkedList<WeakReference<Class<? extends Attribute>>> getAttributeInterfaces(
			final Class<? extends AttributeImpl> clazz) {
		synchronized (knownImplClasses) {
			LinkedList<WeakReference<Class<? extends Attribute>>> foundInterfaces = knownImplClasses.get(clazz);
			if (foundInterfaces == null) {
				// we have a strong reference to the class instance holding all
				// interfaces in the list
				// (parameter "att"),
				// so all WeakReferences are never evicted by GC
				knownImplClasses.put(clazz,
						foundInterfaces = new LinkedList<WeakReference<Class<? extends Attribute>>>());
				// find all interfaces that this attribute instance implements
				// and that extend the Attribute interface
				Class<?> actClazz = clazz;
				do {
					for (Class<?> curInterface : actClazz.getInterfaces()) {
						if (curInterface != Attribute.class && Attribute.class.isAssignableFrom(curInterface)) {
							foundInterfaces.add(new WeakReference<Class<? extends Attribute>>(
									curInterface.asSubclass(Attribute.class)));
						}
					}
					actClazz = actClazz.getSuperclass();
				} while (actClazz != null);
			}
			return foundInterfaces;
		}
	}
 
	public final void addAttributeImpl(final AttributeImpl att) {
		final Class<? extends AttributeImpl> clazz = att.getClass();
		if (attributeImpls.containsKey(clazz))
			return;
		final LinkedList<WeakReference<Class<? extends Attribute>>> foundInterfaces = getAttributeInterfaces(clazz);

		// add all interfaces of this AttributeImpl to the maps
		for (WeakReference<Class<? extends Attribute>> curInterfaceRef : foundInterfaces) {
			final Class<? extends Attribute> curInterface = curInterfaceRef.get();
			assert (curInterface != null) : "We have a strong reference on the class holding the interfaces, so they should never get evicted";
			// Attribute is a superclass of this interface
			if (!attributes.containsKey(curInterface)) {
				// invalidate state to force recomputation in captureState()
				this.currentState[0] = null;
				attributes.put(curInterface, att);
				attributeImpls.put(clazz, att);
			}
		}
	}
 
	public final <A extends Attribute> A addAttribute(Class<A> attClass) {
		AttributeImpl attImpl = attributes.get(attClass);
		if (attImpl == null) {
			if (!(attClass.isInterface() && Attribute.class.isAssignableFrom(attClass))) {
				throw new IllegalArgumentException(
						"addAttribute() only accepts an interface that extends Attribute, but " + attClass.getName()
								+ " does not fulfil this contract.");
			}
			addAttributeImpl(attImpl = this.factory.createAttributeInstance(attClass));
		}
		return attClass.cast(attImpl);
	}
 
	public final boolean hasAttributes() {
		return !this.attributes.isEmpty();
	}

 
	public final boolean hasAttribute(Class<? extends Attribute> attClass) {
		return this.attributes.containsKey(attClass);
	}
 
	public final <A extends Attribute> A getAttribute(Class<A> attClass) {
		AttributeImpl attImpl = attributes.get(attClass);
		if (attImpl == null) {
			throw new IllegalArgumentException(
					"This AttributeSource does not have the attribute '" + attClass.getName() + "'.");
		}
		return attClass.cast(attImpl);
	}

	private State getCurrentState() {
		State s = currentState[0];
		if (s != null || !hasAttributes()) {
			return s;
		}
		State c = s = currentState[0] = new State();
		final Iterator<AttributeImpl> it = attributeImpls.values().iterator();
		c.attribute = it.next();
		while (it.hasNext()) {
			c.next = new State();
			c = c.next;
			c.attribute = it.next();
		}
		return s;
	}
 
	public final void clearAttributes() {
		for (State state = getCurrentState(); state != null; state = state.next) {
			state.attribute.clear();
		}
	}
 
	public final State captureState() {
		final State state = this.getCurrentState();
		return (state == null) ? null : (State) state.clone();
	}
 
	public final void restoreState(State state) {
		if (state == null)
			return;

		do {
			AttributeImpl targetImpl = attributeImpls.get(state.attribute.getClass());
			if (targetImpl == null) {
				throw new IllegalArgumentException("State contains AttributeImpl of type "
						+ state.attribute.getClass().getName() + " that is not in in this AttributeSource");
			}
			state.attribute.copyTo(targetImpl);
			state = state.next;
		} while (state != null);
	}

	@Override
	public int hashCode() {
		int code = 0;
		for (State state = getCurrentState(); state != null; state = state.next) {
			code = code * 31 + state.attribute.hashCode();
		}
		return code;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (obj instanceof AttributeSource) {
			AttributeSource other = (AttributeSource) obj;

			if (hasAttributes()) {
				if (!other.hasAttributes()) {
					return false;
				}

				if (this.attributeImpls.size() != other.attributeImpls.size()) {
					return false;
				}

				// it is only equal if all attribute impls are the same in the
				// same order
				State thisState = this.getCurrentState();
				State otherState = other.getCurrentState();
				while (thisState != null && otherState != null) {
					if (otherState.attribute.getClass() != thisState.attribute.getClass()
							|| !otherState.attribute.equals(thisState.attribute)) {
						return false;
					}
					thisState = thisState.next;
					otherState = otherState.next;
				}
				return true;
			} else {
				return !other.hasAttributes();
			}
		} else
			return false;
	}
 
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
 
	public final void reflectWith(AttributeReflector reflector) {
		for (State state = getCurrentState(); state != null; state = state.next) {
			state.attribute.reflectWith(reflector);
		}
	}
 
	public final AttributeSource cloneAttributes() {
		final AttributeSource clone = new AttributeSource(this.factory);

		if (hasAttributes()) {
			// first clone the impls
			for (State state = getCurrentState(); state != null; state = state.next) {
				clone.attributeImpls.put(state.attribute.getClass(), (AttributeImpl) state.attribute.clone());
			}

			// now the interfaces
			for (Entry<Class<? extends Attribute>, AttributeImpl> entry : this.attributes.entrySet()) {
				clone.attributes.put(entry.getKey(), clone.attributeImpls.get(entry.getValue().getClass()));
			}
		}

		return clone;
	}
 
	public final void copyTo(AttributeSource target) {
		for (State state = getCurrentState(); state != null; state = state.next) {
			final AttributeImpl targetImpl = target.attributeImpls.get(state.attribute.getClass());
			if (targetImpl == null) {
				throw new IllegalArgumentException("This AttributeSource contains AttributeImpl of type "
						+ state.attribute.getClass().getName() + " that is not in the target");
			}
			state.attribute.copyTo(targetImpl);
		}
	}

}
