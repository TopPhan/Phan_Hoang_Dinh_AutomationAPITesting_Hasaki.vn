package com.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LogUtils {

    private static final Logger log = LogManager.getLogger(LogUtils.class);

    public static void info(String message)  { log.info(message);  }
    public static void info(Object object)   { log.info(object);   }
    public static void warn(String message)  { log.warn(message);  }
    public static void warn(Object object)   { log.warn(object);   }
    public static void error(String message) { log.error(message); }
    public static void error(Object object)  { log.error(object);  }
    public static void fatal(String message) { log.fatal(message); }
    public static void debug(String message) { log.debug(message); }
    public static void debug(Object object)  { log.debug(object);  }
}
