package com.helion3.prism.libs.org.apache.tomcat.jdbc.naming;

import com.helion3.prism.libs.org.apache.juli.logging.Log;
import com.helion3.prism.libs.org.apache.juli.logging.LogFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

public class GenericNamingResourcesFactory implements ObjectFactory {
   private static final Log log = LogFactory.getLog(GenericNamingResourcesFactory.class);

   public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable environment) throws Exception {
      if (obj != null && obj instanceof Reference) {
         Reference ref = (Reference)obj;
         Enumeration refs = ref.getAll();
         String type = ref.getClassName();
         Object o = Class.forName(type).newInstance();

         while(refs.hasMoreElements()) {
            RefAddr addr = (RefAddr)refs.nextElement();
            String param = addr.getType();
            String value = null;
            if (addr.getContent() != null) {
               value = addr.getContent().toString();
            }

            if (!setProperty(o, param, value, false)) {
               log.debug("Property not configured[" + param + "]. No setter found on[" + o + "].");
            }
         }

         return o;
      } else {
         return null;
      }
   }

   public static boolean setProperty(Object o, String name, String value, boolean invokeSetProperty) {
      if (log.isDebugEnabled()) {
         log.debug("IntrospectionUtils: setProperty(" + o.getClass() + " " + name + "=" + value + ")");
      }

      String setter = "set" + capitalize(name);

      try {
         Method[] methods = o.getClass().getMethods();
         Method setPropertyMethodVoid = null;
         Method setPropertyMethodBool = null;

         int i;
         for(i = 0; i < methods.length; ++i) {
            Class[] paramT = methods[i].getParameterTypes();
            if (setter.equals(methods[i].getName()) && paramT.length == 1 && "java.lang.String".equals(paramT[0].getName())) {
               methods[i].invoke(o, value);
               return true;
            }
         }

         for(i = 0; i < methods.length; ++i) {
            boolean ok = true;
            if (setter.equals(methods[i].getName()) && methods[i].getParameterTypes().length == 1) {
               Class paramType = methods[i].getParameterTypes()[0];
               Object[] params = new Object[1];
               if (!"java.lang.Integer".equals(paramType.getName()) && !"int".equals(paramType.getName())) {
                  if (!"java.lang.Long".equals(paramType.getName()) && !"long".equals(paramType.getName())) {
                     if (!"java.lang.Boolean".equals(paramType.getName()) && !"boolean".equals(paramType.getName())) {
                        if ("java.net.InetAddress".equals(paramType.getName())) {
                           try {
                              params[0] = InetAddress.getByName(value);
                           } catch (UnknownHostException var16) {
                              if (log.isDebugEnabled()) {
                                 log.debug("IntrospectionUtils: Unable to resolve host name:" + value);
                              }

                              ok = false;
                           }
                        } else if (log.isDebugEnabled()) {
                           log.debug("IntrospectionUtils: Unknown type " + paramType.getName());
                        }
                     } else {
                        params[0] = Boolean.valueOf(value);
                     }
                  } else {
                     try {
                        params[0] = new Long(value);
                     } catch (NumberFormatException var13) {
                        ok = false;
                     }
                  }
               } else {
                  try {
                     params[0] = new Integer(value);
                  } catch (NumberFormatException var14) {
                     ok = false;
                  }
               }

               if (ok) {
                  methods[i].invoke(o, params);
                  return true;
               }
            }

            if ("setProperty".equals(methods[i].getName())) {
               if (methods[i].getReturnType() == Boolean.TYPE) {
                  setPropertyMethodBool = methods[i];
               } else {
                  setPropertyMethodVoid = methods[i];
               }
            }
         }

         if (setPropertyMethodBool != null || setPropertyMethodVoid != null) {
            Object[] params = new Object[]{name, value};
            if (setPropertyMethodBool != null) {
               try {
                  return (Boolean)setPropertyMethodBool.invoke(o, params);
               } catch (IllegalArgumentException var15) {
                  if (setPropertyMethodVoid != null) {
                     setPropertyMethodVoid.invoke(o, params);
                     return true;
                  }

                  throw var15;
               }
            }

            setPropertyMethodVoid.invoke(o, params);
            return true;
         }
      } catch (IllegalArgumentException var17) {
         log.warn("IAE " + o + " " + name + " " + value, var17);
      } catch (SecurityException var18) {
         if (log.isDebugEnabled()) {
            log.debug("IntrospectionUtils: SecurityException for " + o.getClass() + " " + name + "=" + value + ")", var18);
         }
      } catch (IllegalAccessException var19) {
         if (log.isDebugEnabled()) {
            log.debug("IntrospectionUtils: IllegalAccessException for " + o.getClass() + " " + name + "=" + value + ")", var19);
         }
      } catch (InvocationTargetException var20) {
         Throwable cause = var20.getCause();
         if (cause instanceof ThreadDeath) {
            throw (ThreadDeath)cause;
         }

         if (cause instanceof VirtualMachineError) {
            throw (VirtualMachineError)cause;
         }

         if (log.isDebugEnabled()) {
            log.debug("IntrospectionUtils: InvocationTargetException for " + o.getClass() + " " + name + "=" + value + ")", var20);
         }
      }

      return false;
   }

   public static String capitalize(String name) {
      if (name != null && name.length() != 0) {
         char[] chars = name.toCharArray();
         chars[0] = Character.toUpperCase(chars[0]);
         return new String(chars);
      } else {
         return name;
      }
   }
}
