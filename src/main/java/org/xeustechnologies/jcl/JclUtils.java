/**
 *  JCL (Jar Class Loader)
 *
 *  Copyright (C) 2011  Kamran Zafar
 *
 *  This file is part of Jar Class Loader (JCL).
 *  Jar Class Loader (JCL) is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  JarClassLoader is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JCL.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  @author Kamran Zafar
 *
 *  Contact Info:
 *  Email:  xeus.man@gmail.com
 *  Web:    http://xeustech.blogspot.com
 */

package org.xeustechnologies.jcl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.xeustechnologies.jcl.exception.JclException;
import org.xeustechnologies.jcl.proxy.ProxyProviderFactory;

/**
 * This class has some important utility methods commonly required when using
 * JCL
 *
 * @author Kamran
 *
 */
@SuppressWarnings("unchecked")
public class JclUtils {

    public static Object createProxy(Object object, Class superClass, Class[] interfaces, ClassLoader cl) {
        return ProxyProviderFactory.create().createProxy( object, superClass, interfaces, cl );
    }

    /**
     * Casts the object ref to the passed interface class ref. It actually
     * returns a dynamic proxy for the passed object
     *
     * @param object
     * @param clazz
     * @return castable
     */
    public static Object toCastable(Object object, Class clazz) {
        return createProxy( object, clazz, new Class[] { clazz }, null );
    }

    /**
     * Casts the object ref to the passed interface class ref. It actually
     * returns a dynamic proxy for the passed object
     *
     * @param object
     * @param clazz
     *            []
     * @return castable
     */
    public static Object toCastable(Object object, Class[] clazz) {
        return createProxy( object, clazz[0], clazz, null );
    }

    /**
     * Casts the object ref to the passed interface class ref
     *
     * @param object
     * @param clazz
     * @param cl
     * @return castable
     */
    public static Object toCastable(Object object, Class clazz, ClassLoader cl) {
        return createProxy( object, clazz, new Class[] { clazz }, cl );
    }

    /**
     * Casts the object ref to the passed interface class ref
     *
     * @param object
     * @param clazz
     *            []
     * @param cl
     * @return castable
     */
    public static Object toCastable(Object object, Class[] clazz, ClassLoader cl) {
        return createProxy( object, clazz[0], clazz, cl );
    }

    /**
     * Casts the object ref to the passed interface class ref and returns it
     *
     * @param object
     * @param clazz
     * @return T reference
     * @return casted
     */
    public static <T> T cast(Object object, Class<T> clazz) {
        return (T) toCastable( object, clazz, null );
    }

    /**
     * Casts the object ref to the passed interface class ref and returns it
     *
     * @param object
     * @param clazz
     * @param cl
     * @return T reference
     * @return casted
     */
    public static <T> T cast(Object object, Class<T> clazz, ClassLoader cl) {
        return (T) toCastable( object, clazz, cl );
    }

}