package com.redhat.jenkins.plugins.cachet;

import hudson.Extension;
import hudson.model.PeriodicWork;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;

@Extension
public class ResourceUpdater extends PeriodicWork {
    private static final Logger log = Logger.getLogger(ResourceUpdater.class.getName());

    private static final long INTERVAL_MINUTES = 1;

    private static final String PER_PAGE = "per_page=100";
    private static final String LIST_COMPONENTS = "components";
    private static final String ATTRIBUTE_DATA = "data";
    private static final String ATTRIBUTE_LINKS = "links";
    private static final String ATTRIBUTE_META = "meta";
    private static final String ATTRIBUTE_NAME = "name";
    private static final String ATTRIBUTE_NEXT_PAGE = "next_page";
    private static final String ATTRIBUTE_PAGINATION = "pagination";

    @Override
    public long getRecurrencePeriod() {
        return TimeUnit.MINUTES.toMillis(INTERVAL_MINUTES);
    }

    @Override
    protected void doRun() {
        GlobalCachetConfiguration gcc = GlobalCachetConfiguration.get();
        List<SourceTemplate> sources = gcc.getSources();
        String cachetUrl = gcc.getCachetUrl();
        Map<String, JsonNode> rmap = new TreeMap<>();

        if (!StringUtils.isEmpty(cachetUrl)){
            rmap.putAll(getResources(new SourceTemplate(cachetUrl, gcc.getLabel(), gcc.isIgnoreSSL())));
        }
        if (!sources.isEmpty()) {
            sources.forEach(source -> {
                Map<String, JsonNode> tmpMap = getResources(source);

                tmpMap.forEach((resourceName, resourceData) -> {
                    if (rmap.containsKey(resourceName) && !rmap.get(resourceName).equals(resourceData)){
                        if (!StringUtils.isEmpty(source.getLabel()))
                            resourceName = source.getLabel() + ": " + resourceName;
                        else log.warning("Resource " + resourceName + " will be overwritten with " +
                                "new data, to avoid this please add a label for " + source.getCachetUrl());
                    }
                    rmap.put(resourceName, resourceData);
                });
            });
        }
        ResourceProvider.SINGLETON.setResources(rmap);
        log.info("Cachet Resources: " + (!rmap.isEmpty() ? rmap.keySet().toString() : "<none>"));
    }

    private static SSLContext buildAllowAnythingSSLContext() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
            return SSLContexts.custom().loadTrustMaterial(null, (TrustStrategy) (chain, authType) -> true).build();
    }

    private static CloseableHttpClient createHTTPClient(boolean ignoreSSL)
            throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {

        if (ignoreSSL) {
            SSLContext sslContext = buildAllowAnythingSSLContext();

            SSLConnectionSocketFactory sslConnectionSocketFactory =
                    new SSLConnectionSocketFactory(sslContext, null, null,
                            NoopHostnameVerifier.INSTANCE);

            PoolingHttpClientConnectionManager connectionManager = new
                    PoolingHttpClientConnectionManager(RegistryBuilder.
                    <ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslConnectionSocketFactory).build());

            // finally create the HttpClient using HttpClient factory methods and assign the ssl socket factory
            return HttpClients
                    .custom()
                    .setConnectionManager(connectionManager)
                    .build();
        } else {
            return HttpClients.createDefault();
        }
    }

    public static Map<String, JsonNode> getResources(SourceTemplate sourceTemplate) {
        String api = sourceTemplate.getCachetUrl();
        boolean ignoreSSL = BooleanUtils.isTrue(sourceTemplate.isIgnoreSSL());
        log.info("Refreshing resources from " + api);

        Map<String, JsonNode> rmap = null;
        if (!StringUtils.isEmpty(api)) {
            try (CloseableHttpClient httpClient = createHTTPClient(ignoreSSL)) {
                rmap = new TreeMap<>();
                String link = StringUtils.appendIfMissing(api, "/") + LIST_COMPONENTS + "?" + PER_PAGE;
                while (link != null  && !link.equals("null")) {
                    HttpGet get = new HttpGet(link);
                    try (CloseableHttpResponse response = httpClient.execute(get)) {
                        if (response != null) {
                            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                                ObjectMapper om = new ObjectMapper();
                                JsonNode root = om.readTree(response.getEntity().getContent());
                                ArrayNode resources = (ArrayNode) root.get(ATTRIBUTE_DATA);
                                if (resources != null && resources.size() > 0) {
                                    for (JsonNode n : resources) {
                                        String label = sourceTemplate.getLabel();
                                        if (!StringUtils.isEmpty(label)) {
                                            rmap.put(label + ": " + n.get(ATTRIBUTE_NAME).asText(), n);
                                        } else {
                                            rmap.put(n.get(ATTRIBUTE_NAME).asText(), n);
                                        }
                                    }
                                }
                                link = root.get(ATTRIBUTE_META).get(ATTRIBUTE_PAGINATION).get(ATTRIBUTE_LINKS).get(ATTRIBUTE_NEXT_PAGE).asText();
                            } else {
                                log.severe("Failed to retrieve Cachet component list - " + response.getStatusLine().getStatusCode() + ".");
                                link = null;
                            }
                        } else {
                            log.severe("Failed to retrieve Cachet component list, null response returned.");
                            link = null;
                        }
                    }
                }
            } catch (IOException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                log.log(Level.SEVERE, "Unhandled exception retrieving Cachet component list.", e);
                rmap = null;
            }
        } else {
            log.warning("No Cachet URL set.");
        }
        return rmap;
    }
}
