package io.micronaut.http.server.netty;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.vavr.collection.List;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies to all requests and adds ignore-encoding header to the response if the request
 * contains one of the "/swagger", "/info", "/lineupdashboardservice" OR if the user
 * sent Ignore-Encoding in the request headers.
 */
@Filter("/**")
public class IgnoreEncodingFilter implements HttpServerFilter {
    private static final Logger logger = LoggerFactory.getLogger(IgnoreEncodingFilter.class);
    private static final List<String> ignoreEncodingForTheseURLs = List.of("/swagger", "/info", "/lineupdashboardservice");

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {

        if (isAcceptEncodingHeaderPresent(request)) {

            logger.debug("Applying io.micronaut.http.server.netty.IgnoreEncodingFilter");
            return Publishers.map(
                    chain.proceed(request),
                    mutableHttpResponse -> {
                        if (ignoreEncoding(request)) {
                            logger.debug("Adding ignore-encoding filter.");
                            mutableHttpResponse.header("ignore-encoding", "true");
                        }
                        return mutableHttpResponse;
                    }
            );
        }

        return chain.proceed(request);
    }

    /**
     * Returns true if micronaut should ignore encoding (i.e. compressing the response)
     *
     * @param request http request
     * @return true if micronaut should ignore encoding (i.e. compressing the response)
     */
    private boolean ignoreEncoding(HttpRequest<?> request) {

        return doesRequestUrlContainIgnoreEncodingUrls(request) || requestHasIgnoreEncodingHeader(request);
    }

    /**
     * Always ignore encoding for swagger url(s), lineupdashboardservice, /info call i.e. all non-client relate calls.
     *
     * @param request http request
     * @return if request url contains any of ignoreEncodingForTheseURLs urls.
     */
    private boolean doesRequestUrlContainIgnoreEncodingUrls(final HttpRequest<?> request) {

        String requestUrl = request.getUri().toString();
        return !ignoreEncodingForTheseURLs.filter(requestUrl::contains).isEmpty();
    }

    /**
     * If the user sent ignore-encoding header in the request, then don't compress.
     *
     * @param request http request
     * @return true if user sent ignore-encoding header.
     */
    private boolean requestHasIgnoreEncodingHeader(final HttpRequest<?> request) {

        return request.getHeaders().get("Ignore-Encoding", String.class).isPresent();
    }

    /**
     * Returns true if the user sent Accept-Encoding header.
     *
     * @param request http request
     * @return true if the user sent Accept-Encoding header.
     */
    private boolean isAcceptEncodingHeaderPresent(final HttpRequest<?> request) {

        return StringUtils.isNotBlank(request.getHeaders().get("Accept-Encoding"));
    }
}
