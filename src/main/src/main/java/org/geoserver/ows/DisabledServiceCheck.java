/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Logger;
import org.geoserver.config.ServiceInfo;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geotools.util.logging.Logging;

/**
 * Intercepts requests to all OWS services ensuring that the service is enabled.
 *
 * @author Justin Deoliveira, OpenGEO
 */
public class DisabledServiceCheck implements DispatcherCallback {

    static final Logger LOGGER = Logging.getLogger(DisabledServiceCheck.class);

    @Override
    public Request init(Request request) {
        return request;
    }

    @Override
    public Service serviceDispatched(Request request, Service service) {
        // first get serviceInfo object from service
        Object s = service.getService();

        // get the getServiceInfo() method
        Method m = null;

        // if this object is actually proxied, we need to a big more work
        if (s instanceof Proxy) {
            Class<?>[] interfaces = s.getClass().getInterfaces();
            for (int i = 0; m == null && i < interfaces.length; i++) {
                m = OwsUtils.getter(interfaces[i], "serviceInfo", ServiceInfo.class);
            }
        } else {
            m = OwsUtils.getter(s.getClass(), "serviceInfo", ServiceInfo.class);
        }

        if (m != null) {
            try {
                ServiceInfo info = (ServiceInfo) m.invoke(s, null);

                if (info == null) {
                    // log a warning, we could not perform an important check
                    LOGGER.warning(
                            "Could not get a ServiceInfo for service "
                                    + service.getId()
                                    + " even if the service implements ServiceInfo, thus could not check if the service is enabled");
                } else {
                    // check if the service is enabled
                    if (!info.isEnabled()) {
                        throw new ServiceException(
                                "Service " + info.getName() + " is disabled", ServiceException.SERVICE_UNAVAILABLE);
                    }
                }
            } catch (Exception e) {
                // TODO: log this
                if (e instanceof ServiceException) {
                    throw (ServiceException) e;
                }
                throw new ServiceException(e);
            }
        } else {
            // log a warning, we could not perform an important check
            LOGGER.warning("Could not get a ServiceInfo for service "
                    + service.getId()
                    + " thus could not check if the service is enabled");
        }

        return service;
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        return operation;
    }

    @Override
    public Object operationExecuted(Request request, Operation operation, Object result) {
        return result;
    }

    @Override
    public Response responseDispatched(Request request, Operation operation, Object result, Response response) {
        return response;
    }

    @Override
    public void finished(Request request) {}
}
