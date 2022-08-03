/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The project Contributors require contributions 
 * made to this file be licensed under the Apache-2.0 
 * license or a compatible open source license.
 */

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.regions.Region;

class CommandLineArgs {
    private static final int SCREEN_WIDTH = 160;

    private Logger logger = LoggerFactory.getLogger(CommandLineArgs.class);
    protected String endpoint;
    protected Region region;

    CommandLineArgs(final String[] args) throws ParseException {
        parseOptions(args);
    }

    CommandLineArgs(final String endpoint, final Region region) {
        this.endpoint = endpoint;
        this.region = region;
    }

    protected void parseOptions(final String[] args) throws ParseException {
        Options options = new Options()
                .addRequiredOption(null, "endpoint", true, "OpenSearch endpoint")
                .addRequiredOption(null, "region", true, "AWS signing region");

        try {
            CommandLine cmd = new DefaultParser().parse(options, args);
            this.endpoint = cmd.getOptionValue("endpoint");
            this.region = Region.of(cmd.getOptionValue("region"));
        } catch (ParseException e) {
            logger.error("Invalid command line arguments.", e);
            new HelpFormatter().printHelp(
                    SCREEN_WIDTH,
                    String.join(" ",
                            "mvn",
                            "compile",
                            "exec:java",
                            "-Dexec.mainClass=Example",
                            "-Dexec.args=\"...\""
                    ),
                    null,
                    options,
                    null);
            throw e;
        }
    }
}