package com.btc.ep.plugins.embeddedplatform.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.jenkinsci.plugins.workflow.steps.BodyInvoker;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import com.google.common.util.concurrent.ListenableFuture;

import hudson.FilePath;
import hudson.model.Result;
import hudson.model.TaskListener;

/**
 * Fake StepContext for testing purposes
 */
public class StepContextStub extends StepContext {

    private static StepContext instance;

    public static StepContext getInstance() {
        if (instance == null) {
            instance = new StepContextStub();
        }
        return instance;
    }

    private static final long serialVersionUID = 1L;

    @Override
    public void onFailure(Throwable t) {
        t.printStackTrace();
    }

    @Override
    public void setResult(Result r) {
    }

    @Override
    public ListenableFuture<Void> saveState() {
        return null;

    }

    @Override
    public void onSuccess(Object result) {
        System.out.println("Step finished with result: " + result);
    }

    @Override
    public BodyInvoker newBodyInvoker() throws IllegalStateException {
        return null;

    }

    @SuppressWarnings ("unchecked")
    @Override
    public <T> T get(Class<T> key) throws IOException, InterruptedException {
        if (FilePath.class.equals(key)) {
            return (T)new FilePath(new File("E:/workspace"));
        } else if (TaskListener.class.equals(key)) {
            TaskListener t = new TaskListener() {

                private static final long serialVersionUID = 1L;

                @Override
                public PrintStream getLogger() {
                    return System.out;

                }
            };
            return (T)t;
        }
        return null;

    }

    @Override
    public boolean equals(Object o) {
        return false;

    }

    @Override
    public boolean isReady() {
        return false;

    }

    @Override
    public int hashCode() {
        return 1337;

    }

}
