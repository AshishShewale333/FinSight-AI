package com.finsight.news.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsight.news.domain.FinancialNewsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HexFormat;

@Component
@ConditionalOnProperty(name = "finsight.news.provider", havingValue = "gdelt-rss")
public class LiveFinancialNewsProvider implements FinancialNewsProvider {
    private static final Logger log = LoggerFactory.getLogger(LiveFinancialNewsProvider.class);
    private static final String GDELT_URL = "https://api.gdeltproject.org/api/v2/doc/doc";
    private static final String GOOGLE_RSS_URL = "https://news.google.com/rss/search?q=";
    private static final List<String> GOOGLE_QUERIES = List.of(
            "NSE OR BSE OR Nifty OR Sensex stock market",
            "Indian banking sector stocks",
            "Reliance Industries stock"
    );
    private static final DateTimeFormatter GDELT_DATE = DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmssX")
            .withZone(ZoneOffset.UTC);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ObjectMapper mapper;

    public LiveFinancialNewsProvider(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<NewsItem> latestMarketNews() {
        Map<String, NewsItem> unique = new LinkedHashMap<>();
        try {
            fetchGdelt().forEach(item -> unique.putIfAbsent(dedupeKey(item), item));
        } catch (Exception exception) {
            log.warn("GDELT news request failed; trying Google News RSS", exception);
        }
        for (String query : GOOGLE_QUERIES) {
            try {
                fetchGoogleRss(query).forEach(item -> unique.putIfAbsent(dedupeKey(item), item));
            } catch (Exception exception) {
                log.warn("Google News RSS request failed query={}", query, exception);
            }
        }
        return unique.values().stream().limit(40).toList();
    }

    @Override
    public String name() {
        return "GDELT + Google News RSS";
    }

    private List<NewsItem> fetchGdelt() throws Exception {
        String query = "sourcecountry:india (NSE OR BSE OR Nifty OR Sensex OR \"stock market\" OR banking OR \"Reliance Industries\")";
        String url = GDELT_URL + "?query=" + encode(query)
                + "&mode=ArtList&format=json&maxrecords=20&sort=DateDesc&timespan=1d";
        JsonNode root = mapper.readTree(get(url, "application/json"));
        JsonNode articles = root.path("articles");
        if (!articles.isArray()) return List.of();

        List<NewsItem> result = new ArrayList<>();
        for (JsonNode article : articles) {
            String articleUrl = article.path("url").asText("");
            String title = article.path("title").asText("");
            if (!isHttpUrl(articleUrl) || title.isBlank()) continue;
            String source = article.path("domain").asText(host(articleUrl));
            result.add(new NewsItem(source, hash(articleUrl), title, articleUrl,
                    gdeltDate(article.path("seendate").asText(null)), null));
        }
        return result;
    }

    private List<NewsItem> fetchGoogleRss(String query) throws Exception {
        String url = GOOGLE_RSS_URL + encode(query) + "&hl=en-IN&gl=IN&ceid=IN:en";
        byte[] body = get(url, "application/rss+xml, application/xml, text/xml");
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);

        List<NewsItem> result = new ArrayList<>();
        try (ByteArrayInputStream input = new ByteArrayInputStream(body)) {
            XMLStreamReader reader = factory.createXMLStreamReader(input, StandardCharsets.UTF_8.name());
            boolean inItem = false;
            String title = null, link = null, source = null, published = null;
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String element = reader.getLocalName().toLowerCase(Locale.ROOT);
                    if (element.equals("item") || element.equals("entry")) {
                        inItem = true;
                        title = null;
                        link = null;
                        source = null;
                        published = null;
                    } else if (inItem && element.equals("link")) {
                        String href = reader.getAttributeValue(null, "href");
                        link = href == null ? readElementText(reader) : href;
                    } else if (inItem && (element.equals("title") || element.equals("source")
                            || element.equals("pubdate") || element.equals("published") || element.equals("updated"))) {
                        String value = readElementText(reader);
                        switch (element) {
                            case "title" -> title = value;
                            case "source" -> source = value;
                            case "pubdate", "published", "updated" -> published = value;
                            default -> { }
                        }
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT
                        && reader.getLocalName().equalsIgnoreCase("item")) {
                    if (title != null && !title.isBlank() && isHttpUrl(link)) {
                        String publisher = source == null || source.isBlank() ? host(link) : source;
                        result.add(new NewsItem(publisher, hash(link), title, link, rssDate(published), null));
                    }
                    inItem = false;
                }
            }
            reader.close();
        }
        return result;
    }

    private byte[] get(String url, String accept) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(java.time.Duration.ofSeconds(15))
                .header("Accept", accept)
                .header("User-Agent", "FinSight-AI/0.1 (financial news headlines; links to publishers)")
                .GET().build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("News provider returned HTTP " + response.statusCode());
        }
        return response.body();
    }

    private String readElementText(XMLStreamReader reader) throws Exception {
        return reader.getElementText().trim();
    }

    private Instant gdeltDate(String raw) {
        if (raw == null || raw.isBlank()) return Instant.now();
        try { return Instant.from(GDELT_DATE.parse(raw)); }
        catch (Exception ignored) {
            try { return Instant.parse(raw); }
            catch (Exception alsoIgnored) { return Instant.now(); }
        }
    }

    private Instant rssDate(String raw) {
        if (raw == null || raw.isBlank()) return Instant.now();
        try { return ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant(); }
        catch (Exception ignored) {
            try { return OffsetDateTime.parse(raw).toInstant(); }
            catch (Exception alsoIgnored) { return Instant.now(); }
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isHttpUrl(String value) {
        if (value == null) return false;
        try {
            URI uri = URI.create(value);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (Exception ignored) { return false; }
    }

    private String host(String url) {
        try { return URI.create(url).getHost(); }
        catch (Exception ignored) { return "News publisher"; }
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private String dedupeKey(NewsItem item) {
        return item.url().toLowerCase(Locale.ROOT);
    }
}
