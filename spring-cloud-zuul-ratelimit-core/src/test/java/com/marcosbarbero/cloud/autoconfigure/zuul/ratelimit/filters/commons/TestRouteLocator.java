package com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.filters.commons;

import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;

import java.util.Collection;
import java.util.List;

/**
 * @author Marcos Barbero
 * @since 2017-06-23
 */
public class TestRouteLocator implements RouteLocator {

    private final Collection<String> ignoredPaths;
    private final List<Route> routes;

    public TestRouteLocator(Collection<String> ignoredPaths, List<Route> routes) {
        this.ignoredPaths = ignoredPaths;
        this.routes = routes;
    }

    @Override
    public Collection<String> getIgnoredPaths() {
        return this.ignoredPaths;
    }

    @Override
    public List<Route> getRoutes() {
        return this.routes;
    }

    @Override
    public Route getMatchingRoute(String path) {
        return this.routes.stream()
                .filter(route -> path.startsWith(route.getPrefix()))
                .findFirst()
                .orElse(null);
    }
}
