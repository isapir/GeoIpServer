package net.twentyonesolutions.geoip;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.Context;
import org.apache.catalina.Server;

public class GeoIpServer {

    static int port = 63019;

    public static void main(String[] args) throws IOException, LifecycleException, URISyntaxException {

        String s;

        String webxml = GeoIpServer.class.getResource("/web.xml").getPath();

        String clsSrc = GeoIpServer.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        System.out.printf("Class loaded from %s\n", clsSrc);

        File clsDir = new File(clsSrc).getCanonicalFile();
        if (!clsDir.isDirectory())
            clsDir = clsDir.getParentFile();

        Path path;
        path = Paths.get(clsDir.toURI().getPath(), "../web.xml");
        if (Files.exists(path)) {
            webxml = path.toString();
        }
        else {
            path = Paths.get(clsDir.toURI().getPath(), "web.xml");
            if (Files.exists(path))
                webxml = path.toString();
        }

        File deployDescriptor = new File(webxml);
        System.out.printf("webxml: %s\n", deployDescriptor.getCanonicalPath());
        if (!deployDescriptor.exists())
            System.err.println("web.xml not found");

        s = "/tmp/GeoIpServer";
        String appBase = (new File(s)).getCanonicalPath().replace('\\', '/');
        String docBase = appBase + "/webroot";

        (new File(docBase)).mkdirs();

        System.out.println("Setting appBase: " + appBase);
        System.out.println("Setting docBase: " + docBase);

        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(appBase);
    //  tomcat.setHostname("localhost");
        tomcat.setPort(port);
        tomcat.setAddDefaultWebXmlToWebapp(false);

        Context context = tomcat.addWebapp("", docBase);
        context.setAltDDName(webxml);

    //  context.setLogEffectiveWebXml(true);

    //  context.setResourceOnlyServlets("net.twentyonesolutions.geoip.GeoIpServlet");

        System.out.println(tomcat.getConnector());

        tomcat.start();

        Server server = tomcat.getServer();
        server.await();
    }
}
