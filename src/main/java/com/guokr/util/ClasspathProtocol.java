package com.guokr.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.ProtectionDomain;
import java.security.CodeSource;
import java.util.Hashtable;
import java.util.Map;

import org.xeustechnologies.jcl.AbstractClassLoader;
import org.xeustechnologies.jcl.JarClassLoader;
import org.xeustechnologies.jcl.JclObjectFactory;

import com.guokr.util.DowngradeClassLoader;

public enum ClasspathProtocol {
    INSTANCE;

    ClasspathProtocol() {
        try {
            URL.setURLStreamHandlerFactory(new Factory("classpath", new Handler()));
        } catch (Exception e) {
            //NOOP
        }
    }

    static class Handler extends URLStreamHandler {
        private final ClassLoader loader;
        private ProtectionDomain domain = ClasspathProtocol.class.getProtectionDomain();
        private CodeSource source = domain.getCodeSource();
        private URL location = source.getLocation();
        private String path = (location != null ? location.getPath() : null);

        public Handler() {
            AbstractClassLoader loader = (path != null
                ? new JarClassLoader(new String[] { path })
                : new DowngradeClassLoader(getClass().getClassLoader()));
            this.loader = loader;
        }

        public Handler(ClassLoader loader) {
            this.loader = loader;
        }

        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            String path = u.getPath();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            final URL resourceUrl = loader.getResource(path);
            if (resourceUrl!=null) {
                return resourceUrl.openConnection();
            } else {
                return null;
            }
        }
    }

    static class Factory implements URLStreamHandlerFactory {
        private final Map<String, URLStreamHandler> handlers;

        public Factory(String protocol, URLStreamHandler urlHandler) {
            handlers = new Hashtable<String, URLStreamHandler>();
            addHandler(protocol, urlHandler);
        }

        public void addHandler(String protocol, URLStreamHandler urlHandler) {
            handlers.put(protocol, urlHandler);
        }

        public URLStreamHandler createURLStreamHandler(String protocol) {
            return handlers.get(protocol);
        }
    }
}


