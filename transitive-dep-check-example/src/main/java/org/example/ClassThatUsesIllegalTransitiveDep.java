package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 */
public class ClassThatUsesIllegalTransitiveDep {
    private static final Logger logger = LoggerFactory.getLogger(ClassThatUsesIllegalTransitiveDep.class);

    public static void main(String[] args) {
        logger.info("Hello World!");
    }

}
