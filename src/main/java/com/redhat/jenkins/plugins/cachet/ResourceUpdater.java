package com.redhat.jenkins.plugins.cachet;

import hudson.Extension;
import hudson.model.PeriodicWork;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

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
    protected void doRun() throws Exception {
        if (!StringUtils.isEmpty(GlobalCachetConfiguration.get().getCachetUrl())) {
            setResources();
        }
    }

    public static void setResources() {
        String api = GlobalCachetConfiguration.get().getCachetUrl();
        log.info("Refreshing resources from " + api);

        if (!StringUtils.isEmpty(api)) {
            Map<String, JsonNode> rmap = null;
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
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
                                    Iterator<JsonNode> i = resources.iterator();
                                    while (i.hasNext()) {
                                        JsonNode n = i.next();
                                        rmap.put(n.get(ATTRIBUTE_NAME).asText(), n);
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
            } catch (IOException e) {
                log.log(Level.SEVERE, "Unhandled exception retrieving Cachet component list.", e);
                rmap = null;
            } finally {
                ResourceProvider.SINGLETON.setResources(rmap);
                log.info("Resources: " + (rmap != null ? rmap.keySet().toString() : "<none>"));
            }
        } else {
            log.warning("No Cachet URL set.");
        }
    }
}
