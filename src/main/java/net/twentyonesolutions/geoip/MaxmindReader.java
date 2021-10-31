package net.twentyonesolutions.geoip;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.*;

public class MaxmindReader implements Closeable {

    public static final String DEFAULT_DB_PATH = "/GeoLite2-City_20211012/GeoLite2-City.mmdb";
    public static final Map<String, String> RECORD_NOT_FOUND;
    public static final String RECORD_NOT_FOUND_JSON;
    private static final Gson gson;
    private final DatabaseReader reader;

    static {

        gson = new Gson();

        RECORD_NOT_FOUND = Map.ofEntries(
                Map.entry("found", "false"),
                Map.entry("continent", ""),
                Map.entry("continent_name", ""),
                Map.entry("country", ""),
                Map.entry("country_name", ""),
                Map.entry("is_eu", "false"),
                Map.entry("subdivision", ""),
                Map.entry("subdivision_name", ""),
                Map.entry("city", ""),
                Map.entry("postal_code", ""),
                Map.entry("accuracy_radius", ""),
                Map.entry("latitude", ""),
                Map.entry("longitude", ""),
                Map.entry("timezone", "")
        );

        RECORD_NOT_FOUND_JSON = gson.toJson(RECORD_NOT_FOUND);
    }

    public MaxmindReader(String path) {

        reader = createReaderFromClasspath(path);
        if (reader == null) {
            throw new IllegalArgumentException("Failed to create reader from " + path);
        }
    }


    public MaxmindReader() {
        this(DEFAULT_DB_PATH);
    }


    public Map<String, String> getIpInfo(String addr) {

        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(addr);
        }
        catch (UnknownHostException ex) {
            return RECORD_NOT_FOUND;
        }

        Optional<CityResponse> optResponse = null;
        try {
            optResponse = this.reader.tryCity(inetAddress);
        } catch (IOException | GeoIp2Exception ex) {
            ex.printStackTrace();
        }

        if (optResponse == null || !optResponse.isPresent()) {
            return RECORD_NOT_FOUND;
        }

        CityResponse cityResponse = optResponse.get();

        Continent continent = cityResponse.getContinent();
        Country country = cityResponse.getCountry();
        Subdivision subdivision = cityResponse.getMostSpecificSubdivision();
        City city = cityResponse.getCity();
        Postal postal = cityResponse.getPostal();
        Location location = cityResponse.getLocation();

        String countryCode = emptyIfNull(country.getIsoCode());
        if (countryCode.isEmpty()) {
            return RECORD_NOT_FOUND;
        }

        Map<String, String> result = Map.ofEntries(
            Map.entry("found", "true"),

            Map.entry("continent", emptyIfNull(continent.getCode())),
            Map.entry("continent_name", emptyIfNull(continent.getName())),
            Map.entry("country", countryCode),
            Map.entry("country_name", emptyIfNull(country.getName())),
            Map.entry("is_eu", emptyIfNull(country.isInEuropeanUnion())),
            Map.entry("subdivision", emptyIfNull(subdivision.getIsoCode())),           // state code
            Map.entry("subdivision_name", emptyIfNull(subdivision.getName())),   // state code
            Map.entry("city", emptyIfNull(city.getName())),
            Map.entry("postal_code", emptyIfNull(postal.getCode())),
            Map.entry("accuracy_radius", emptyIfNull(String.valueOf(location.getAccuracyRadius()))),
            Map.entry("latitude", emptyIfNull(String.valueOf(location.getLatitude()))),
            Map.entry("longitude", emptyIfNull(String.valueOf(location.getLongitude()))),
            Map.entry("timezone", emptyIfNull(location.getTimeZone()))
        );

        return result;
    }


    public String getPathInfoJson(String addr) {
        Map<String, String> map = this.getIpInfo(addr);
        String result = this.gson.toJson(map);
        return result;
    }


    /**
     * Creates a reusable, thread-safe, DatabaseReader object from a file on the classpath
     *
     * @param filename - e.g. "/maxmind/GeoLite2-City.mmdb"
     * @return
     * @throws IOException
     */
    public static DatabaseReader createReaderFromClasspath(String filename) {
        DatabaseReader reader = null;
        try {
            InputStream is = MaxmindReader.class.getResourceAsStream(filename);
            reader = new DatabaseReader.Builder(is).build();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }

        return reader;
    }


    /**
     * Creates a reusable, thread-safe, DatabaseReader object from a file on the file system
     *
     * @param path
     * @return
     * @throws IOException
     */
    public static DatabaseReader createReaderFromFile(String path) {
        DatabaseReader reader = null;
        try {
            File file = new File(path);
            reader = new DatabaseReader.Builder(file).build();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }

        return reader;
    }

    /**
     * Closes this stream and releases any system resources associated
     * with it. If the stream is already closed then invoking this
     * method has no effect.
     *
     * <p> As noted in {@link AutoCloseable#close()}, cases where the
     * close may fail require careful attention. It is strongly advised
     * to relinquish the underlying resources and to internally
     * <em>mark</em> the {@code Closeable} as closed, prior to throwing
     * the {@code IOException}.
     */
    @Override
    public void close() {
        if (reader != null) {
            try {
                reader.close();
            }
            catch (IOException ex) {}
        }
    }

    private static String emptyIfNull(Object o) {
        if (o == null) {
            return "";
        }

        return String.valueOf(o);
    }
}
