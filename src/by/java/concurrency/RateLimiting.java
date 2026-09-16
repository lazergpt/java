package by.java.concurrency;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class RateLimiting {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final int MAX_PARALLEL = 10;
    private static final Semaphore gate = new Semaphore(MAX_PARALLEL);
    private static final String API_URL =
            "https://api.chucknorris.io/jokes/random";

    public static void main(String[] args) throws Exception {
        Instant start = Instant.now();
        List<String> jokes = fetchJokes(50);
        long ms = Duration.between(start, Instant.now()).toMillis();
        System.out.printf("Fetched %d jokes in %d ms (avg %d ms)%n",
                jokes.size(), ms, ms / jokes.size());
        jokes.stream().limit(3).forEach(j -> System.out.println("• " + j));
    }

    private static List<String> fetchJokes(int n) throws Exception {
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = IntStream.range(0, n)
                    .mapToObj(i -> pool.submit(RateLimiting::fetchJoke))
                    .toList();
            return futures.stream()
                    .map(RateLimiting::join).toList();
        }
    }


    private static String fetchJoke() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(API_URL))
                .GET()
                .timeout(Duration.ofSeconds(30)).build();
        try {
            gate.acquire(); // только MAX_PARALLEL одновременно
            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200)
                throw new RuntimeException("API error " + res.statusCode());
            return parseJoke(res.body());
        } finally {
            gate.release();
        }
    }

    private static String parseJoke(String json) {
        int s = json.indexOf("\"value\":\"") + 9;
        int e = json.indexOf('"', s);
        return json.substring(s, e).replace("\\\"", "\"");
    }

    private static <T> T join(Future<T> f) {
        try {
            return f.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException(e);
        } catch (ExecutionException e) {
            throw new CompletionException(e.getCause());
        }
    }
}
