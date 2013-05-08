/*
 * #%L
 * SciJava Common shared library for SciJava software.
 * %%
 * Copyright (C) 2009 - 2013 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package org.scijava.plugin;

import java.util.HashMap;
import java.util.List;

import net.java.sezpoz.Index;
import net.java.sezpoz.IndexItem;

/**
 * Default SciJava plugin discovery mechanism.
 * <p>
 * It works by using SezPoz to scan the classpath for {@link Plugin}
 * annotations.
 * </p>
 * 
 * @author Curtis Rueden
 */
public class DefaultPluginFinder implements PluginFinder {

	/** Class loader to use when querying SezPoz. */
	private final ClassLoader customClassLoader;

	// -- Constructors --

	public DefaultPluginFinder() {
		this(null);
	}

	public DefaultPluginFinder(final ClassLoader classLoader) {
		customClassLoader = classLoader;
	}

	// -- PluginFinder methods --

	@Override
	public HashMap<String, Throwable> findPlugins(
		final List<PluginInfo<?>> plugins)
	{
		final HashMap<String, Throwable> exceptions =
			new HashMap<String, Throwable>();

		// load the SezPoz index
		final ClassLoader classLoader = getClassLoader();
		final Index<Plugin, SciJavaPlugin> sezPozIndex =
			Index.load(Plugin.class, SciJavaPlugin.class, classLoader);

		// create a PluginInfo object for each item in the index
		for (final IndexItem<Plugin, SciJavaPlugin> item : sezPozIndex) {
			try {
				final PluginInfo<?> info = createInfo(item, classLoader);
				plugins.add(info);
			}
			catch (final Throwable t) {
				exceptions.put(item.className(), t);
			}
		}

		return exceptions;
	}

	// -- Helper methods --

	private PluginInfo<SciJavaPlugin> createInfo(
		final IndexItem<Plugin, SciJavaPlugin> item, final ClassLoader classLoader)
	{
		final String className = item.className();
		final Plugin plugin = item.annotation();

		@SuppressWarnings("unchecked")
		final Class<SciJavaPlugin> pluginType =
			(Class<SciJavaPlugin>) plugin.type();

		return new PluginInfo<SciJavaPlugin>(className, pluginType, plugin, classLoader);
	}

	private ClassLoader getClassLoader() {
		if (customClassLoader != null) return customClassLoader;

		/*
		 * If not even the current class can be found by the current
		 * Thread's context class loader, chances are that the plugins
		 * the caller tries to discover using this plugin finder cannot
		 * be found, either. Therefore let's use the current class'
		 * class loader in that case. This is not completely
		 * fool-proof, but better than nothing.
		 */
		final ClassLoader thisLoader = getClass().getClassLoader();
		final ClassLoader contextLoader =
			Thread.currentThread().getContextClassLoader();
		for (ClassLoader loader = contextLoader;
				loader != null;
				loader = loader.getParent()) {
			if (thisLoader == loader) return contextLoader;
		}
		return thisLoader;
	}

}
