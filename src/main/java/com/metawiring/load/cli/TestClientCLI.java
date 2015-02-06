package com.metawiring.load.cli;

import com.metawiring.load.core.Result;
import com.metawiring.load.core.TestPhaseClient;

public class TestClientCLI
{
    public static void main( String[] args ) throws Exception {
        Result result = new TestPhaseClient().configure(args).call();
        result.reportTo(System.out);
    }
}
