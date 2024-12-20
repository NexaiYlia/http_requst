package com.nexai.http_request;

import feign.Feign;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.springframework.web.client.RestClient;



@EnableFeignClients
@Service
public class SpaceService {

    private static final String API_URL = "http://api.open-notify.org/astros.json";

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        SpaceService spaceService = new SpaceService();
        spaceService.run();
    }

    public void run() throws InterruptedException, ExecutionException {
        // Запуск параллельных запросов
        CompletableFuture<Map.Entry<String, Long>> webClientFuture = CompletableFuture.supplyAsync(() -> webClientRequest());
        CompletableFuture<Map.Entry<String, Long>> restTemplateFuture = CompletableFuture.supplyAsync(() -> restTemplateRequest());
        CompletableFuture<Map.Entry<String, Long>> httpClientFuture = CompletableFuture.supplyAsync(() -> httpClientRequest());
        CompletableFuture<Map.Entry<String, Long>> restClientFuture = CompletableFuture.supplyAsync(() -> restClientRequest());
        CompletableFuture<Map.Entry<String, Long>> openFeignFuture = CompletableFuture.supplyAsync(() -> openFeignRequest());


        CompletableFuture.allOf(webClientFuture, restTemplateFuture, httpClientFuture, restClientFuture, openFeignFuture).join();

        List<Map.Entry<String, Long>> results = Arrays.asList(
                webClientFuture.get(),
                restTemplateFuture.get(),
                httpClientFuture.get(),
                restClientFuture.get(),
                openFeignFuture.get()
        );


        results.forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue() + " ms"));


        Map.Entry<String, Long> fastest = results.stream().min(Comparator.comparingLong(Map.Entry::getValue)).orElseThrow();
        Map.Entry<String, Long> slowest = results.stream().max(Comparator.comparingLong(Map.Entry::getValue)).orElseThrow();

        System.out.println("Самый долгий запрос: " + slowest.getKey() + " (" + slowest.getValue() + " ms)");
        System.out.println("Самый быстрый запрос: " + fastest.getKey() + " (" + fastest.getValue() + " ms)");
    }

    private Map.Entry<String, Long> webClientRequest() {
        long startTime = System.currentTimeMillis();
        WebClient client = WebClient.create(API_URL);
        String response = client.get().retrieve().bodyToMono(String.class).block();
        long duration = System.currentTimeMillis() - startTime;
        return new AbstractMap.SimpleEntry<>("WebClient", duration);
    }

    private Map.Entry<String, Long> restTemplateRequest() {
        long startTime = System.currentTimeMillis();
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(API_URL, HttpMethod.GET, null, String.class);
        long duration = System.currentTimeMillis() - startTime;
        return new AbstractMap.SimpleEntry<>("RestTemplate", duration);
    }

    private Map.Entry<String, Long> httpClientRequest() {
        try {
            long startTime = System.currentTimeMillis();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            long duration = System.currentTimeMillis() - startTime;
            return new AbstractMap.SimpleEntry<>("HttpClient", duration);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map.Entry<String, Long> restClientRequest() {
        long startTime = System.currentTimeMillis();
        RestClient restClient = RestClient.create();
        String response = restClient.get(API_URL);
        long duration = System.currentTimeMillis() - startTime;
        return new AbstractMap.SimpleEntry<>("RestClient", duration);
    }

    private Map.Entry<String, Long> openFeignRequest() {
        long startTime = System.currentTimeMillis();
        // Создаем и используем OpenFeign для отправки запроса
        OpenFeignClient feignClient = Feign.builder()
                .target(OpenFeignClient.class, API_URL);
        String response = feignClient.getAstros();
        long duration = System.currentTimeMillis() - startTime;
        return new AbstractMap.SimpleEntry<>("OpenFeign", duration);
    }

    @FeignClient(name = "openFeignClient", url = "http://api.open-notify.org")
    interface OpenFeignClient {
        @RequestMapping(method = RequestMethod.GET, value = "/astros.json")
        String getAstros();
    }

}
