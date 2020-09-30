package com.redhat.jenkins.plugins.cachet.matrix;

import com.google.common.collect.Sets;
import hudson.Extension;
import hudson.matrix.AxisList;
import hudson.matrix.Combination;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
import hudson.matrix.listeners.MatrixBuildListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

@Extension
public class CachetMatrixListener extends MatrixBuildListener {
    private static final Logger log = Logger.getLogger(CachetMatrixListener.class.getName());

    /**
     * Sets the Cachet job property before every build if needed
     * See {@link MatrixBuildListener}
     */
    @Override
    public boolean doBuildConfiguration(MatrixBuild matrixBuild, MatrixConfiguration matrixConfiguration) {
        MatrixProject matrixProject = matrixConfiguration.getParent();
        AxisList axisList = matrixProject.getAxes().subList(CachetAxis.class);

        if (!axisList.isEmpty()){
            CachetMatrixAction matrixAction = new CachetMatrixAction(matrixProject);

            List<String> axisNames = new ArrayList<>();
            axisList.forEach(axis -> axisNames.add(axis.getName()));

            Combination combination = matrixConfiguration.getCombination();
            List<String> resources = getResourcesFromCombination(combination, axisNames);

            try {
                matrixAction.setCachetProperty(combination, resources);
            } catch (IOException e){
                log.severe(e.getMessage());
            }
        }
        return true;
    }

    /**
     * Help method to manipulate a combination into a list of resources
     * @return list of resources
     */
    private List<String> getResourcesFromCombination(Combination combination, List<String> axisNames){
        Set<String> resources = Sets.newHashSet();
        String combString = combination.toString();
        String[] axisList = combString.split(",");

        Arrays.stream(axisList).forEach(axis -> {
            String[] nameAndResource = axis.split("=");
            String name = nameAndResource[0];
            String resource = nameAndResource[1];
            if (axisNames.contains(name)){
                resources.add(resource);
            }
        });
        return !resources.isEmpty() ? new ArrayList<>(resources) : Collections.emptyList();
    }
}
