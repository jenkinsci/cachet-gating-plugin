package com.redhat.jenkins.plugins.cachet.matrix;

import hudson.Extension;
import hudson.matrix.MatrixProject;
import hudson.model.Action;
import jenkins.model.TransientActionFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;

/*
*
TransientActionFactory can be used to add any number of actions to
a given instance of an Actionable subtype. TransientActionFactory defines:

    1. Which subtype of Actionable it applies to

    2. Which kinds of Action it creates
* */

@Extension
public class CachetMatrixActionFactory extends TransientActionFactory<MatrixProject> {

    @Override
    public Class<MatrixProject> type() {
        return MatrixProject.class;
    }

    @Nonnull
    @Override
    public Collection<? extends Action> createFor(@Nonnull MatrixProject project) {
        return Collections.singleton(new CachetMatrixAction(project));
    }
}
