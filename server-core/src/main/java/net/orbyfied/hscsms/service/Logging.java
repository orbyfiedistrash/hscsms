package net.orbyfied.hscsms.service;

import net.orbyfied.hscsms.util.worker.LoopWorker;
import net.orbyfied.hscsms.util.worker.SafeWorker;
import net.orbyfied.j8.util.logging.LogHandler;
import net.orbyfied.j8.util.logging.LogText;
import net.orbyfied.j8.util.logging.Logger;
import net.orbyfied.j8.util.logging.LoggerGroup;
import net.orbyfied.j8.util.logging.io.LogOutput;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logging {

    // the main logger group
    private static final LoggerGroup GROUP = new LoggerGroup("HSCSMS");

    public static final PrintStream ERR;
    private static SafeWorker errWorker;
    public static final LogOutput FORMAT_CONTROLLED_STDOUT = LogOutput.STDOUT; // TODO: maybe later

    private static boolean formatted;

    public static boolean isFormatted() {
        return formatted;
    }

    public static void setFormatted(boolean f) {
        formatted = f;
        if (FORMAT_CONTROLLED_STDOUT != null)
            FORMAT_CONTROLLED_STDOUT.setFormatted(f);
    }

    static {
        /*
         * Bulk Error Stream
         */

        ByteArrayOutputStream baos  = new ByteArrayOutputStream();
        BufferedOutputStream stream = new BufferedOutputStream(baos);

        ERR = new PrintStream(stream, false) {
            @Override
            public synchronized void flush() {
                try {
                    stream.flush();
                    System.err.write(baos.toByteArray());
                    baos.reset();
                } catch (IOException e) {
                    e.printStackTrace(Logging.ERR);
                }
            }
        };

        errWorker = new LoopWorker("ErrorStreamWorker", dt -> {
            // flushes the error stream 20 times per second
            ERR.flush();
        }).setTargetUps(20f).commence();

        /*
         * Time appending.
         */

        final DateFormat format = new SimpleDateFormat("hh:mm:ss.SSSS");

        GROUP.addConfigure((group1, logger1) -> {
            logger1.prePipeline()
                    .addLast(LogHandler.of((pipeline, record) -> {
                        record.carry("time", new Date());
                    }).named("set-time"));

            logger1.pipeline()
                    .addLast(LogHandler.of((pipeline, record) -> {
                        Date date = record.carried("time");
                        LogText text  = record.getText();
                        LogText tTime = text.sub("time", 0);
                        tTime.put("(");
                        tTime.put("time-value", format.format(date));
                        tTime.put(")");
                    }).named("format-time"));
        });
    }

    /**
     * Get the main logger group.
     * @return The logger group.
     */
    public static LoggerGroup getGroup() {
        return GROUP;
    }

    /**
     * Gets a logger or creates a new
     * one with the specified name.
     * @param name The name.
     * @return The logger.
     */
    public static Logger getLogger(String name) {
        Logger logger;
        if ((logger = GROUP.getByName(name)) != null)
            return logger;

        logger = GROUP.create(name);
        return logger;
    }

}
