package org.restcomm.perfcorder.collector;

import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * PerfCorder collects info about an external process through JMX beans
 *
 *
 */
public class JMX2CSVApp {

    private Double delay_ = 1.0;

    private int maxIterations_ = -1;

    private static Logger logger;

    private static OptionParser createOptionParser() {
        OptionParser parser = new OptionParser();
        parser.acceptsAll(Arrays.asList(new String[]{"help", "?", "h"}),
                "shows this help").forHelp();
        parser
                .acceptsAll(Arrays.asList(new String[]{"n", "iteration"}),
                        "perfcorder will exit after n output iterations").withRequiredArg()
                .ofType(Integer.class);
        parser
                .acceptsAll(Arrays.asList(new String[]{"d", "delay"}),
                        "delay between each output iteration").withRequiredArg()
                .ofType(Double.class);
        parser
                .acceptsAll(Arrays.asList(new String[]{"m", "meta"}),
                        "Path to meta file").withRequiredArg().ofType(String.class);
        parser
                .acceptsAll(Arrays.asList(new String[]{"p", "pid"}),
                        "PID to connect to").withOptionalArg().ofType(Integer.class);
      

        return parser;
    }

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);

        logger = Logger.getLogger("JVMStatApp");

        OptionParser parser = createOptionParser();
        OptionSet a = parser.parse(args);

        if (a.has("help")) {
            System.out.println("perfcorder - java monitoring for the command-line");
            System.out.println("Usage: perfcorder.sh [options...] [PID]");
            System.out.println("");
            parser.printHelpOn(System.out);
            System.exit(0);
        }

        Integer pid = null;

        double delay = 1.0;

        Integer iterations = -1;

        if (a.hasArgument("delay")) {
            delay = (Double) (a.valueOf("delay"));
            if (delay < 0.1d) {
                throw new IllegalArgumentException("Delay cannot be set below 0.1");
            }
        }

        if (a.hasArgument("n")) {
            iterations = (Integer) a.valueOf("n");
        }

        //to support PID as non option argument
        if (a.nonOptionArguments().size() > 0) {
            pid = Integer.valueOf((String) a.nonOptionArguments().get(0));
        }

        InputStream resourceAsStream = new FileInputStream((String) a.valueOf("meta"));
        JAXBContext targetsContext = JAXBContext.newInstance(JMX2CSVDescriptor.class);
        JMX2CSVDescriptor descriptor = (JMX2CSVDescriptor) targetsContext.createUnmarshaller().unmarshal(resourceAsStream);        

        JMX2CSVView vmDetailStatView = null;
        if (a.hasArgument("pid")) {
            pid =(Integer) a.valueOf("pid");
            vmDetailStatView = new JMX2CSVView(pid, descriptor);
        } else {
            vmDetailStatView = new JMX2CSVView(0, descriptor);
        }
        
        JMX2CSVApp collector = new JMX2CSVApp();
        collector.setDelay(delay);
        collector.setMaxIterations(iterations);
        
        System.out.println(vmDetailStatView.printHeader());
        collector.run(vmDetailStatView);
    }

    public int getMaxIterations() {
        return maxIterations_;
    }

    public void setMaxIterations(int iterations) {
        maxIterations_ = iterations;
    }

    protected void run(ConsoleView view) throws Exception {
        try {
            System.setOut(new PrintStream(new BufferedOutputStream(
                    new FileOutputStream(FileDescriptor.out)), false));
            int iterations = 0;
            while (!view.shouldExit()) {
                System.out.println(view.printView());
                System.out.flush();
                iterations++;
                if (iterations >= maxIterations_ && maxIterations_ > 0) {
                    break;
                }
                view.sleep((int) (delay_ * 1000));
            }
        } catch (NoClassDefFoundError e) {
            e.printStackTrace(System.err);

            System.err.println("");
            System.err.println("ERROR: Some JDK classes cannot be found.");
            System.err
                    .println("       Please check if the JAVA_HOME environment variable has been set to a JDK path.");
            System.err.println("");
        }
    }

    public JMX2CSVApp() {
    }

    public Double getDelay() {
        return delay_;
    }

    public void setDelay(Double delay) {
        delay_ = delay;
    }

}
