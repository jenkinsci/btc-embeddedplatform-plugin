package com.btc.ep.plugins.embeddedplatform.util;

import java.io.PrintStream;

public class ProgressPrinter {

    private PrintStream out;
    private double done = 0;
    private String topic;

    public ProgressPrinter(PrintStream out) {
        this.out = out;
    }

    public ProgressPrinter(String topic, PrintStream out) {
        this.out = out;
        this.topic = topic;
    }

    public void reset() {
        this.done = 0;
    }

    public void displayScala() {
        String scala = topic != null ? "| [" + topic + " progress]" : "|";

        int remainingLength = (99 - scala.length());
        System.out.println(scala.length());
        System.out.println(remainingLength);

        for (int i = 0; i < remainingLength; i++) {
            scala += " ";
        }
        scala += "|";
        out.println(scala);
    }

    public void progress(double totalProgress) {
        if (totalProgress > done) {
            if (done == 0) {
                displayScala();
            }
            double value = Math.min(100 - done, totalProgress - done);
            String progressBarString = "";
            for (int i = 0; i < value; i++) {
                progressBarString += "°";
            }
            out.print(progressBarString);
            done = totalProgress;
            if (totalProgress >= 100d) {
                out.println();
            }
        }
    }

}
