package com.tw.go.plugin.cmd;

import java.util.List;

public interface StreamConsumer {
    public void consumeLine(String line);

    public List<String> asList();
}
