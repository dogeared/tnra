package com.afitnerd.tnra.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "tnra-cli",
    description = "TNRA group provisioning tool",
    subcommands = { ProvisionCommand.class },
    mixinStandardHelpOptions = true,
    version = "1.0.0"
)
public class TnraCli {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new TnraCli()).execute(args);
        System.exit(exitCode);
    }
}
