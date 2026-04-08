package com.kowshik.advanced;
import com.kowshik.patterns.RateLimiter;

import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parallel Web Crawler Demo - Asynchronous Web Scraping
 *
 * INTERVIEW PREP - Key Topics:
 * =============================
 * 1. Web Crawler Design:
 * - Breadth-first vs depth-first traversal
 * - URL deduplication
 * - Rate limiting to avoid overwhelming servers
 * - Depth limiting to control crawl scope
 * - Politeness (robots.txt, crawl delay)
 *
 * 2. Common Interview Questions:
 * - How to design a scalable web crawler?
 * - How to handle duplicate URLs?
 * - How to implement rate limiting?
 * - How to handle errors and retries?
 * - CompletableFuture vs traditional threading?
 * - How to prevent infinite loops in crawling?
 *
 * 3. Concurrency Patterns:
 * - CompletableFuture for async operations
 * - ConcurrentHashMap for thread-safe deduplication
 * - ThreadPoolExecutor for controlled parallelism
 * - Rate limiter for request throttling
 *
 * 4. Scalability Considerations:
 * - Distributed crawling across machines
 * - URL frontier (queue management)
 * - DNS caching
 * - Connection pooling
 * - Politeness policies
 *
 * 5. Real-world Applications:
 * - Search engine indexing (Google, Bing)
 * - Price comparison sites
 * - SEO analysis tools
 * - Data aggregation services
 *
 * Demonstrates a parallel web crawler using CompletableFuture for asynchronous
 * operations.
 * Features:
 * - Parallel page processing
 * - Depth-limited crawling
 * - Rate limiting
 * - Resource cleanup
 * - Error handling and retry mechanism
 */
public class ParallelWebCrawlerDemo {
    private final ExecutorService executorService;
    private final Set<String> visitedUrls;
    private final int maxDepth;
    private final int maxConcurrentRequests;
    private final RateLimiter rateLimiter;
    private static final Pattern URL_PATTERN = Pattern.compile("href=[\"'](http[s]?://.*?)[\"']");

    /**
     * Creates a new parallel web crawler.
     *
     * @param maxDepth              maximum crawl depth
     * @param maxConcurrentRequests maximum parallel requests
     * @param requestsPerSecond     rate limit (requests per second)
     */
    public ParallelWebCrawlerDemo(int maxDepth, int maxConcurrentRequests, int requestsPerSecond) {
        this.maxDepth = maxDepth;
        this.maxConcurrentRequests = maxConcurrentRequests;
        this.executorService = new ThreadPoolExecutor(
                4,
                maxConcurrentRequests,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy());
        this.visitedUrls = ConcurrentHashMap.newKeySet();
        this.rateLimiter = new RateLimiter(requestsPerSecond);
    }

    /**
     * Starts crawling from the given URL.
     *
     * @param startUrl the starting URL
     * @return a CompletableFuture containing all discovered URLs
     */
    public CompletableFuture<Set<String>> crawl(String startUrl) {
        return crawlPage(startUrl, 0)
                .thenApply(ignored -> visitedUrls)
                .exceptionally(throwable -> {
                    System.err.println("Crawling failed: " + throwable.getMessage());
                    return Collections.emptySet();
                });
    }

    private CompletableFuture<Void> crawlPage(String url, int depth) {
        if (depth >= maxDepth || !visitedUrls.add(url)) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            rateLimiter.acquire(); // Rate limit the requests
            return downloadPage(url);
        }, executorService)
                .thenApply(this::extractUrls)
                .thenCompose(urls -> {
                    List<CompletableFuture<Void>> futures = urls.stream()
                            .map(newUrl -> crawlPage(newUrl, depth + 1))
                            .collect(Collectors.toList());
                    return CompletableFuture.allOf(
                            futures.toArray(new CompletableFuture[0]));
                })
                .exceptionally(throwable -> {
                    System.err.println("Error crawling " + url + ": " + throwable.getMessage());
                    return null;
                });
    }

    private String downloadPage(String urlString) {
        try {
            URL url = new URL(urlString);
            // Simulated page download
            Thread.sleep(100); // Simulate network delay
            return "Sample page content with links: " +
                    "href='https://example.com' href='https://example.org'";
        } catch (Exception e) {
            throw new RuntimeException("Failed to download: " + urlString, e);
        }
    }

    private Set<String> extractUrls(String content) {
        Set<String> urls = new HashSet<>();
        Matcher matcher = URL_PATTERN.matcher(content);
        while (matcher.find()) {
            urls.add(matcher.group(1));
        }
        return urls;
    }

    /**
     * Shuts down the crawler and releases resources.
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        rateLimiter.shutdown();
    }

    /**
     * Example usage of the parallel web crawler.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        ParallelWebCrawlerDemo crawler = new ParallelWebCrawlerDemo(
                10, // maxDepth
                10, // maxConcurrentRequests
                5 // requestsPerSecond
        );

        try {
            CompletableFuture<Set<String>> crawlFuture = crawler.crawl("https://leetcode.com");
            Set<String> urls = crawlFuture.get(5, TimeUnit.MINUTES);
            System.out.println("Crawled URLs: " + urls);
        } catch (Exception e) {
            System.err.println("Crawling failed: " + e.getMessage());
        } finally {
            crawler.shutdown();
        }
    }
}
