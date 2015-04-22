/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.rt.coverage.main;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import com.intellij.rt.coverage.util.URLsUtil;

/**
 * @author anna
 * @since 25-Feb-2010
 */
public class CoveragePremain {

  public static void premain(String argsString, Instrumentation instrumentation) throws Exception {
    final File lib = new File(getArchivePath()).getParentFile();
    final URL[] urls = new URL[3];
    urls[0] = fileToURL(new File(lib, "coverage-instrumenter.jar"));
    urls[1] = fileToURL(new File(lib, "consulo-internal-asm-all.jar"));
    urls[2] = fileToURL(new File(lib, "consulo-internal-trove4j.jar"));

    final Class instrumentator = Class.forName("com.intellij.rt.coverage.instrumentation.Instrumentator", true, new URLClassLoader(urls) {
      protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (this) {
          Class result = findLoadedClass(name);
          if (result == null) {
            try {
              result = findClass(name);
            } catch (ClassNotFoundException e) {
              //ignore, will try to find class in parent
            }
          }

          if (result != null && resolve) {
            resolveClass(result);
          }

          if (result != null) {
            return result;
          }
        }

        return getParent().loadClass(name);
      }
    });
    final Method premainMethod = instrumentator.getDeclaredMethod("premain", new Class[]{String.class, Instrumentation.class});
    premainMethod.invoke(null, new Object[] {argsString, instrumentation});
  }

  private static URL fileToURL(final File file) throws MalformedURLException {
    return file.getAbsoluteFile().toURI().toURL();
  }

  private static String getArchivePath() {
    final String className = CoveragePremain.class.getName().replace('.', '/') + ".class";
    URL resourceURL = CoveragePremain.class.getResource("/" + className);
    if (resourceURL == null) {
      resourceURL = ClassLoader.getSystemResource(className);
    }
    return URLsUtil.extractRoot(resourceURL, "/" + className);
  }
}