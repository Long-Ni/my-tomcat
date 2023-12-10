package org.example.engine;

import jakarta.servlet.*;
import org.example.engine.support.InitParameters;

import java.util.*;

public class FilterRegistrationImpl implements FilterRegistration.Dynamic {

    final ServletContext servletContext;
    final String name;
    final Filter filter;
    final List<String> urlPatterns = new ArrayList<>(4);
    boolean initialized = false;
    final InitParameters initParameters = new InitParameters();

    public FilterRegistrationImpl(ServletContext servletContext, String name, Filter filter) {
        this.servletContext = servletContext;
        this.name = name;
        this.filter = filter;
    }

    public FilterConfig getFilterConfig() {
        return new FilterConfig() {
            @Override
            public String getFilterName() {
                return FilterRegistrationImpl.this.name;
            }

            @Override
            public ServletContext getServletContext() {
                return FilterRegistrationImpl.this.servletContext;
            }

            @Override
            public String getInitParameter(String s) {
                return FilterRegistrationImpl.this.initParameters.getInitParameter(name);
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return FilterRegistrationImpl.this.initParameters.getInitParameterNames();
            }
        };
    }

    @Override
    public void addMappingForServletNames(EnumSet<DispatcherType> enumSet, boolean b, String... strings) {
        throw new UnsupportedOperationException("addMappingForServletNames");
    }

    @Override
    public Collection<String> getServletNameMappings() {
        return List.of();
    }

    @Override
    public void addMappingForUrlPatterns(EnumSet<DispatcherType> enumSet, boolean isMatchAfter, String... urlPatterns) {
        checkNotInitialized("addMappingForUrlPatterns");
        if (!enumSet.contains(DispatcherType.REQUEST) || enumSet.size() != 1) {
            throw new IllegalArgumentException("Only support DispatcherType.REQUEST.");
        }
        if (urlPatterns == null || urlPatterns.length == 0) {
            throw new IllegalArgumentException("Missing urlPatterns.");
        }
        Collections.addAll(this.urlPatterns, urlPatterns);
    }

    @Override
    public Collection<String> getUrlPatternMappings() {
        return this.urlPatterns;
    }

    @Override
    public void setAsyncSupported(boolean isAsyncSupported) {
        checkNotInitialized("setInitParameter");
        if (isAsyncSupported) {
            throw new UnsupportedOperationException("Async is not supported.");
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getClassName() {
        return filter.getClass().getName();
    }

    // proxy to InitParameters:

    @Override
    public boolean setInitParameter(String name, String value) {
        checkNotInitialized(name);
        return this.initParameters.setInitParameter(name, value);
    }

    @Override
    public String getInitParameter(String name) {
        return this.initParameters.getInitParameter(name);
    }

    @Override
    public Set<String> setInitParameters(Map<String, String> map) {
        checkNotInitialized("setInitParameter");
        return this.initParameters.setInitParameters(map);
    }

    @Override
    public Map<String, String> getInitParameters() {
        return this.initParameters.getInitParameters();
    }

    private void checkNotInitialized(String name) {
        if (this.initialized) {
            throw new IllegalStateException("Cannot call " + name + " after initialization.");
        }
    }
}
