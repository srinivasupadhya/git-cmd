package com.tw.go.plugin.cmd;

import com.tw.go.plugin.util.ListUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InMemoryConsumer implements StreamConsumer {
    private Queue<String> lines = new ConcurrentLinkedQueue<String>();

    public void consumeLine(String line) {
        try {
            lines.add(line);
        } catch (RuntimeException e) {
            // LOG.error("Problem consuming line [" + line + "]", e);
        }
    }

    public List<String> asList() {
        return new ArrayList<String>(lines);
    }

    public String toString() {
        return ListUtil.join(asList(), "\n");
    }
}
