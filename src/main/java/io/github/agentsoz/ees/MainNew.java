package io.github.agentsoz.ees;

import java.util.HashMap;
import java.util.Map;

public class MainNew {

    public static final String OPT_CONFIG = "--config";
    public static final String USAGE = "usage:\n"
            + String.format("%10s %-6s %s\n", OPT_CONFIG, "FILE", "EES config file (v2)")
            ;

    /**
     * Parse the command line options
     * @param args command line options
     * @return key value pairs of known options
     */
    private static Map<String,String> parse(String[] args) {
        Map<String,String> opts = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case OPT_CONFIG:
                    if (i + 1 < args.length) {
                        i++;
                        opts.put(args[i-1],args[i]);
                    }
                    break;
                default:
                    throw new RuntimeException("unknown config option: " + args[i]) ;
            }
        }
        if (opts.isEmpty() || !opts.containsKey(OPT_CONFIG)) {
            throw new RuntimeException(USAGE);
        }
        return opts;
    }

    public static void main(String[] args) {
        Map<String,String> opts = null;
        opts = parse(args);
        Config cfg = new Config();
        cfg.loadFromFile(opts.get(OPT_CONFIG));
    }
}
