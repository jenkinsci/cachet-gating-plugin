package com.redhat.jenkins.plugins.cachet.matrix;

import com.redhat.jenkins.plugins.cachet.CachetJobProperty;
import com.redhat.jenkins.plugins.cachet.ResourceProvider;
import hudson.matrix.Combination;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
import hudson.model.Action;
import hudson.model.Item;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CachetMatrixAction implements Action, StaplerProxy {
    private MatrixProject project;

    public CachetMatrixAction(MatrixProject project) {
        this.project = project;
    }

    @Override
    public String getIconFileName() {
        return this.project.hasPermission(Item.CONFIGURE) ?
                "plugin/cachet-gating/images/cachet-icon.png" : null;
    }

    @Override
    public String getDisplayName() {
        return "Cachet Configuration";
    }

    @Override
    public String getUrlName() {
        return "cachet-matrix";
    }

    @Override
    public Object getTarget() {
        this.project.checkPermission(Item.CONFIGURE);
        return this;
    }

    @Restricted(DoNotUse.class) // For UI purposes - has the user checked this combination?
    public boolean isChecked(String combination) {
        CachetJobProperty jobProperty = getCachetJobPropertyFromCombination(combination);
        if (jobProperty != null) return jobProperty.getRequiredResources();
        return false;
    }

    @Restricted(DoNotUse.class) // For UI purposes - has the user selected this resource?
    public boolean isSelected(String combination, String resource) {
        return getCachetResources(combination).contains(resource);
    }

    @Restricted(DoNotUse.class) // For UI purposes - get all the resources
    public List<String> getResourceNames() {
        return ResourceProvider.SINGLETON.getResourceNames();
    }

    /**
     * @return all the possible combinations
     */
    public Iterable<Combination> getAxis() {
        return project.getAxes().list();
    }

    /**
     * Help method to avoid code duplication
     * @return CachetJobProperty or null if not exist
     */
    private CachetJobProperty getCachetJobPropertyFromCombination(String combination){
        MatrixConfiguration matrixConfiguration = project.getItem(combination);
        assert matrixConfiguration != null;
        return matrixConfiguration.getProperty(CachetJobProperty.class);
    }

    /**
     * Help method to convert resources from JSONArray into List of Strings
     * @return List of resources
     */
    private List<String> getResourcesFromCombination(Combination combination, JSONObject obj) {
        List<String> resources = new ArrayList<>();
        JSONObject resourcesObj = obj.getJSONObject(combination.toString());
        JSONArray resourcesArray = (JSONArray) resourcesObj.get("resources");
        resourcesArray.forEach(resource -> resources.add(resource.toString()));
        return resources;
    }

    /**
     * Get the right resources for some combination
     * @return List of resources or empty list if there is no such JobProperty
     */
    private List<String> getCachetResources(String combination) throws NullPointerException {
        CachetJobProperty jobProperty = getCachetJobPropertyFromCombination(combination);
        if (jobProperty != null) return jobProperty.getResources();
        else return Collections.emptyList();
    }

    /**
     * Taking the resources for some combination from the UI into the correct configuration.
     */
    protected void setCachetProperty(Combination combination, List<String> resources) throws IOException {
        MatrixConfiguration matrixConfiguration = project.getItem(combination);
        matrixConfiguration.removeProperty(CachetJobProperty.class);
        if (!resources.isEmpty())
            matrixConfiguration.addProperty(new CachetJobProperty(true, resources));
    }

    // Handle Cachet configuration
    public void doSave(StaplerRequest req, StaplerResponse rsp) throws IOException {
        Map parametersMap = req.getParameterMap();

        for (Combination combination : getAxis()) {
            List<String> resources = null;
            if (parametersMap.containsKey(combination.toString())) {
                String[] jsonString = (String[]) parametersMap.get("json");
                JSONArray jsonArray = JSONArray.fromObject(jsonString);
                if (!jsonArray.isEmpty()) {
                    resources = getResourcesFromCombination(combination,
                            JSONObject.fromObject(jsonArray.get(0)));
                }
            } else resources = Collections.emptyList();
            assert resources != null;
            setCachetProperty(combination, resources);
        }

        if (parametersMap.containsKey("save"))
            rsp.sendRedirect("../");
        else if (parametersMap.containsKey("apply")) rsp.sendRedirect(".");
    }
}
