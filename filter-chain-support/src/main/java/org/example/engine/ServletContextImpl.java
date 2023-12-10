package org.example.engine;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.descriptor.JspConfigDescriptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.engine.mapping.FilterMapping;
import org.example.engine.mapping.ServletMapping;
import org.example.utils.AnnoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class ServletContextImpl implements ServletContext {

    final Logger logger = LoggerFactory.getLogger(getClass());

    List<ServletMapping> servletMappings = new ArrayList<>();
    Map<String, ServletRegistrationImpl> servletRegistrationMap = new HashMap<>();
    Map<String, Servlet> nameToServletMap = new HashMap<>();

    List<FilterMapping> filterMappings = new ArrayList<>();
    Map<String, FilterRegistrationImpl> filterRegistrationMap = new HashMap<>();
    Map<String, Filter> nameToFilterMap = new HashMap<>();

    public void process(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String uri = request.getServletPath();
        // search servlet:
        Servlet servlet = null;
        for (ServletMapping mapping : this.servletMappings) {
            if (mapping.matcher(uri)) {
                servlet = mapping.servlet;
                break;
            }
        }
        if (servlet == null) {
            // 404 Not Found:
            response.setContentType("text/html");
            PrintWriter pw = response.getWriter();
            pw.write("<h1>404 Not Found</h1><p>No mapping for URL: " + uri + "</p>");
            pw.close();
            return;
        }

        // servlet.service(request, response);

        // search filter:
        List<Filter> enabledFilters = new ArrayList<>();
        for (FilterMapping mapping : this.filterMappings) {
            if (mapping.matcher(uri)) {
                enabledFilters.add(mapping.filter);
            }
        }
        Filter[] filters = enabledFilters.toArray(Filter[]::new);
        logger.atDebug().log("process {} by filter {}, servlet {}", uri, Arrays.toString(filters), servlet);
        FilterChain chain = new FilterChainImpl(filters, servlet);

        try {
            chain.doFilter(request, response);
        } catch (ServletException e) {
            logger.error(e.getMessage(), e);
            throw new IOException(e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    public void initServlets(List<Class<?>> servletClasses) {
        for (Class<?> c : servletClasses) {
            WebServlet ws = c.getAnnotation(WebServlet.class);
            if (ws != null) {
                logger.info("auto register @WebServlet: {}", c.getName());
                @SuppressWarnings("unchecked")
                Class<? extends Servlet> servletClass = (Class<? extends Servlet>) c;
                ServletRegistration.Dynamic registration = this.addServlet(AnnoUtils.getServletName(servletClass), servletClass);
                registration.addMapping(AnnoUtils.getServletUrlPatterns(servletClass));
                registration.setInitParameters(AnnoUtils.getServletInitParams(servletClass));
            }
        }

        servletRegistrationMap.forEach((name, registration) -> {
            try {
                registration.servlet.init(registration.getServletConfig());
                this.nameToServletMap.put(name, registration.servlet);
                registration.getMappings().forEach(urlPattern -> {
                    this.servletMappings.add(new ServletMapping(urlPattern, registration.servlet));
                });
                registration.initialized = true;
            } catch (ServletException e) {
                logger.error("init servlet failed: " + name + " / " + registration.servlet.getClass().getName(), e);
            }
        });

        Collections.sort(this.servletMappings);
    }

    public void initFilters(List<Class<?>> filterClasses) {
        for (Class<?> c : filterClasses) {
            WebFilter wf = c.getAnnotation(WebFilter.class);
            if (wf != null) {
                logger.info("auto register @WebFilter: {}", c.getName());
                @SuppressWarnings("unchecked")
                Class<? extends Filter> clazz = (Class<? extends Filter>) c;
                FilterRegistration.Dynamic registration = this.addFilter(AnnoUtils.getFilterName(clazz), clazz);
                registration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, AnnoUtils.getFilterUrlPatterns(clazz));
                registration.setInitParameters(AnnoUtils.getFilterInitParams(clazz));
            }
        }

        // init filters:
        for (String name : this.filterRegistrationMap.keySet()) {
            var registration = this.filterRegistrationMap.get(name);
            try {
                registration.filter.init(registration.getFilterConfig());
                this.nameToFilterMap.put(name, registration.filter);
                for (String urlPattern : registration.getUrlPatternMappings()) {
                    this.filterMappings.add(new FilterMapping(urlPattern, registration.filter));
                }
                registration.initialized = true;
            } catch (ServletException e) {
                logger.error("init filter failed: " + name + " / " + registration.filter.getClass().getName(), e);
            }
        }
    }


    @SuppressWarnings("unchecked")
    private <T> T createInstance(String className) throws ServletException {
        Class<T> clazz;
        try {
            clazz = (Class<T>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Class not found.", e);
        }
        return createInstance(clazz);
    }

    private <T> T createInstance(Class<T> clazz) throws ServletException {
        try {
            Constructor<T> constructor = clazz.getConstructor();
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new ServletException("Cannot instantiate class " + clazz.getName(), e);
        }
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        return createInstance(clazz);
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String name, Servlet servlet) {
        if (name == null) {
            throw new IllegalArgumentException("name is null.");
        }
        if (servlet == null) {
            throw new IllegalArgumentException("servlet is null. name:{}" + name);
        }
        var registration = new ServletRegistrationImpl(this, name, servlet);
        this.servletRegistrationMap.put(name, registration);
        return registration;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String name, Class<? extends Servlet> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("class is null.");
        }
        Servlet servlet;
        try {
            servlet = createInstance(clazz);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        return addServlet(name, servlet);
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String name, String className) {
        if (className == null || className.isEmpty()) {
            throw new IllegalArgumentException("class name is null or empty.");
        }
        Servlet servlet;
        try {
            Class<? extends Servlet> clazz = createInstance(className);
            servlet = createInstance(clazz);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        return addServlet(name, servlet);
    }

    @Override
    public ServletRegistration getServletRegistration(String name) {
        return this.servletRegistrationMap.get(name);
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return Map.copyOf(this.servletRegistrationMap);
    }

    @Override
    public String getContextPath() {
        // only support root context path:
        return "";
    }

    @Override
    public ServletContext getContext(String uripath) {
        if ("".equals(uripath)) {
            return this;
        }
        // all others are not exist:
        return null;
    }

    @Override
    public String getMimeType(String type) {
        String defaultMime = "application/octet-stream";
        Map<String, String> mimes = Map.of(".html", "text/html", ".txt", "text/plain", ".png", "image/png", ".jpg", "image/jpeg");
        int n = type.lastIndexOf('.');
        if (n == -1) {
            return defaultMime;
        }
        String ext = type.substring(n);
        return mimes.getOrDefault(ext, defaultMime);
    }

    @Override
    public String getInitParameter(String s) {
        // no init parameters:
        return null;
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        // no init parameters:
        return Collections.emptyEnumeration();
    }

    @Override
    public boolean setInitParameter(String s, String s1) {
        throw new UnsupportedOperationException("setInitParameter");
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> aClass) throws ServletException {
        return createInstance(aClass);
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String name, Filter filter) {
        if (name == null) {
            throw new IllegalArgumentException("name is null.");
        }
        if (filter == null) {
            throw new IllegalArgumentException("filter is null.");
        }
        var registration = new FilterRegistrationImpl(this, name, filter);
        this.filterRegistrationMap.put(name, registration);
        return registration;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String name, Class<? extends Filter> aclass) {
        if (aclass == null) {
            throw new IllegalArgumentException("class is null.");
        }
        Filter filter = null;
        try {
            filter = createInstance(aclass);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        return addFilter(name, filter);
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String name, String className) {
        if (className == null || className.isEmpty()) {
            throw new IllegalArgumentException("class name is null or empty.");
        }
        Filter filter = null;
        try {
            Class<? extends Filter> clazz = createInstance(className);
            filter = createInstance(clazz);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        return addFilter(name, filter);
    }

    @Override
    public FilterRegistration getFilterRegistration(String name) {
        return this.filterRegistrationMap.get(name);
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return Map.copyOf(this.filterRegistrationMap);
    }

    // Servlet API version: 6.0.0

    @Override
    public int getMajorVersion() {
        return 6;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public int getEffectiveMajorVersion() {
        return 6;
    }

    @Override
    public int getEffectiveMinorVersion() {
        return 0;
    }

    // TODO ///////////////////////////////////////////////////////////////////

    @Override
    public Set<String> getResourcePaths(String s) {
        return null;
    }

    @Override
    public URL getResource(String s) throws MalformedURLException {
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String s) {
        return null;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        return null;
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String s) {
        return null;
    }

    @Override
    public void log(String s) {

    }

    @Override
    public void log(String s, Throwable throwable) {

    }

    @Override
    public String getRealPath(String s) {
        return null;
    }

    @Override
    public String getServerInfo() {
        return null;
    }

    @Override
    public Object getAttribute(String s) {
        return null;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return null;
    }

    @Override
    public void setAttribute(String s, Object o) {

    }

    @Override
    public void removeAttribute(String s) {

    }

    @Override
    public String getServletContextName() {
        return null;
    }

    @Override
    public ServletRegistration.Dynamic addJspFile(String s, String s1) {
        return null;
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return null;
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> set) {

    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return null;
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return null;
    }

    @Override
    public void addListener(String s) {

    }

    @Override
    public <T extends EventListener> void addListener(T t) {

    }

    @Override
    public void addListener(Class<? extends EventListener> aClass) {

    }

    @Override
    public <T extends EventListener> T createListener(Class<T> aClass) throws ServletException {
        return null;
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return null;
    }

    @Override
    public void declareRoles(String... strings) {

    }

    @Override
    public String getVirtualServerName() {
        return null;
    }

    @Override
    public int getSessionTimeout() {
        return 0;
    }

    @Override
    public void setSessionTimeout(int i) {

    }

    @Override
    public String getRequestCharacterEncoding() {
        return null;
    }

    @Override
    public void setRequestCharacterEncoding(String s) {

    }

    @Override
    public String getResponseCharacterEncoding() {
        return null;
    }

    @Override
    public void setResponseCharacterEncoding(String s) {

    }
}
