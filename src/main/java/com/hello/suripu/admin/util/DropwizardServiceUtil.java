package com.hello.suripu.admin.util;

import io.dropwizard.jersey.DropwizardResourceConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.ws.rs.ext.ExceptionMapper;

public class DropwizardServiceUtil {

    public static void deregisterDWSingletons(DropwizardResourceConfig jerseyConfig) {
        final Set<Object> dwSingletons = jerseyConfig.getSingletons();
        final List<Object> singletonsToRemove = new ArrayList<Object>();

        for (final Object s : dwSingletons) {
            if (s instanceof ExceptionMapper && (s.getClass().getName().startsWith("io.dropwizard.jersey.") ||
                    s.getClass().getName().startsWith("io.dropwizard.auth")) || s.getClass().getName().startsWith("io.dropwizard.jdbi")) {
                singletonsToRemove.add(s);
            }
        }

        //jerseyConfig.getSingletons().removeAll(singletonsToRemove);
    }
}
