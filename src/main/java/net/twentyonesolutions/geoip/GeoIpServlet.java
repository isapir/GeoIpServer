package net.twentyonesolutions.geoip;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


public class GeoIpServlet extends HttpServlet {

    public static final String PREFIX = "/ip/";
    public static final String BANNER = """
            GeoIpServer by Igal Sapir          https://github.com/isapir/GeoIpServer
            Powered by Apache Tomcat           https://tomcat.apache.org/
            Using Maxmind data, available from https://www.maxmind.com/
            To retrieve IP Geolocation data, send a request to
                /ip/<ip-address>
            """;

    private static final String version;

    static {
        // version would be picked up from META-INF if running from JAR built with Maven
        String v = GeoIpServlet.class.getPackage().getImplementationVersion();
        version = (v == null) ? "Unknown" : v;
        System.out.println("GeoIpServlet version: " + version);
        System.out.print(BANNER);
    }

    private MaxmindReader maxmindReader;
    private DateTimeFormatter isoTimeFormatter;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {

        PrintWriter writer = res.getWriter();
        String pathInfo = req.getPathInfo();
        if (pathInfo.startsWith(PREFIX)) {

            long start = System.nanoTime();

            String ipAddr = pathInfo.substring(PREFIX.length());
            String ipInfo = maxmindReader.getPathInfoJson(ipAddr);

            double millis = (System.nanoTime() - start) / 1_000_000.0;
            res.addHeader("X-Time-Took", String.format("%.3f", millis) + "ms");
            res.addHeader("X-Server-Time", isoTimeFormatter.format(Instant.now()));
            res.addHeader("Content-Type", "application/json");

            writer.println(ipInfo);
        }
        else {

            writer.println(BANNER);
        }
    }


    /**
     * Called by the servlet container to indicate to a servlet that the servlet
     * is being placed into service. See {@link Servlet#init}.
     * <p>
     * This implementation stores the {@link ServletConfig} object it receives
     * from the servlet container for later use. When overriding this form of
     * the method, call <code>super.init(config)</code>.
     *
     * @param config the <code>ServletConfig</code> object that contains
     *               configuration information for this servlet
     * @throws ServletException if an exception occurs that interrupts the servlet's
     *                          normal operation
     * @see UnavailableException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {

        isoTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                .withZone(ZoneId.systemDefault());

        String initParam;
        initParam = config.getInitParameter("cache-spec");
        if (initParam != null) {
            System.out.println("Using cache spec: " + initParam);
            maxmindReader = new CachedMaxmindReader(initParam);
        }
        else {
            maxmindReader = new MaxmindReader();
        }
    }


    /**
     * Called by the servlet container to indicate to a servlet that the servlet
     * is being taken out of service. See {@link Servlet#destroy}.
     */
    @Override
    public void destroy() {
        maxmindReader.close();
    }
}
