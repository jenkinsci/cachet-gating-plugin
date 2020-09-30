package com.redhat.jenkins.plugins.cachet.matrix;

import hudson.Extension;
import hudson.matrix.MatrixProject;
import hudson.model.Action;
import jenkins.model.TransientActionFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;

@Extension
public class CachetMatrixActionFactory extends TransientActionFactory<MatrixProject> {

    @Override
    public Class<MatrixProject> type() {
        return MatrixProject.class;
    }

    @Override
    public @Nonnull Collection<? extends Action> createFor(@Nonnull MatrixProject project) {
        return Collections.singleton(new CachetMatrixAction(project));
    }
}
