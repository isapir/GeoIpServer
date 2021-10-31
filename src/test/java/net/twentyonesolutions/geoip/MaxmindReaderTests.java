package net.twentyonesolutions.geoip;

import com.maxmind.geoip2.DatabaseReader;
import org.checkerframework.checker.units.qual.C;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class MaxmindReaderTests {

    List<String[]> readTestCsv(String filename) throws IOException {
        List<String[]> lines = new ArrayList<>();

        InputStream in = this.getClass().getResourceAsStream(filename);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        while (br.ready()) {
            String line = br.readLine();
            if (line.isBlank() || line.charAt(0) == '#') {
                continue;
            }
            String[] parts = line.split(",");
            for (int i=0; i<parts.length; i++) {
                if (parts[i].contains("\""))
                    parts[i] = parts[i].replace("\"", "");
            }

            if (!Character.isDigit(parts[0].charAt(0)))
                continue;

            lines.add(parts);
        }

        return lines;
    }

    List<String[]> readTestData() throws IOException {
        String filename = "/test-data-geolite2-20211012.csv";
        List<String[]> lines = readTestCsv(filename);
        return lines;
    }

    @Test
    void createReader() throws IOException {
        DatabaseReader reader = MaxmindReader.createReaderFromClasspath(MaxmindReader.DEFAULT_DB_PATH);
        reader.close();
    }

    @Test
    void getIpInfo() throws IOException {
        // test data from https://www.pingdom.com/rss/probe_servers.xml
        MaxmindReader reader = new MaxmindReader();
        Map<String, String> ipInfo;

        ipInfo = reader.getIpInfo("176.229.12.0");
        assertEquals("IL", ipInfo.get("country"));

        ipInfo = reader.getIpInfo("178.162.206.244");
        assertEquals("DE", ipInfo.get("country"));

        ipInfo = reader.getIpInfo("2400:2411:4b00::");
        assertEquals("JP", ipInfo.get("country"));

        ipInfo = reader.getIpInfo("159.122.168.9");
        assertEquals("IT", ipInfo.get("country"));

        ipInfo = reader.getIpInfo("209.95.50.14");
        assertEquals("US", ipInfo.get("country"));
    }

    @Test
    void testCsvFileSingleThread() throws IOException {
        List<String[]> lines = readTestData();
        MaxmindReader reader = new MaxmindReader();
        Map<String, String> ipInfo;
        String ipAddr;

        for (String[] line : lines) {
            ipAddr = line[0]; // line[0].replace("\"", "");

            ipInfo = reader.getIpInfo(ipAddr);
            assertEquals(line[1], ipInfo.get("country"));

            System.out.println(ipAddr + "\t" + ipInfo.get("country"));
        }
    }

    @Test
    void testCsvFileMultiThread() throws IOException, InterruptedException {
        List<String[]> lines = readTestData();
        MaxmindReader reader;
//        reader = new MaxmindReader();
        reader = new CachedMaxmindReader("maximumSize=8192");

        int iterations = 10000;
        int numThreads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        AtomicInteger total   = new AtomicInteger();
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();

        long start = System.nanoTime();

        for (int iter=0; iter<iterations; iter++) {
            for (String[] line : lines) {
                Runnable task = () -> {
                    latch.countDown();
                    try {
                        latch.await();
                    } catch (InterruptedException ex) {
                        return;
                    }

                    total.incrementAndGet();

                    String ipAddr = line[0], countryCode = line[1];
                    Map<String, String> ipInfo = reader.getIpInfo(ipAddr);

//                    if (countryCode.equals("FR"))
//                        countryCode = "FF";

                    if (countryCode.equals(ipInfo.get("country"))) {
                        success.incrementAndGet();
                    } else {
                        failure.incrementAndGet();
                    }
                };

                pool.submit(task);
            }
        }

        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);

        long nanosTook = System.nanoTime() - start;

        System.out.println("Total: " + total.get() + "\tsuccess: " + success.get() + "\tfailure: " + failure.get()
                + "\tnanos: " + nanosTook + "\tnanosPerOp: " + nanosTook / total.get());
        assertEquals(0, failure.get());
        assertEquals(iterations * lines.size(), total.get());
    }

}
