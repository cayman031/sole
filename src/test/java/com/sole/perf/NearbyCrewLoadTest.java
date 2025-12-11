package com.sole.perf;

import com.sole.domain.crew.entity.RunningCrew;
import com.sole.domain.crew.repository.RunningCrewRepository;
import com.sole.domain.region.entity.Region;
import com.sole.domain.region.repository.RegionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sole.domain.crew.dto.NearbyCrewResponse;
import com.sole.domain.user.entity.PreferredLevel;
import com.sole.domain.user.entity.User;
import com.sole.domain.user.repository.UserRepository;
import com.sole.global.util.DistanceCalculator;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NearbyCrewLoadTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RegionRepository regionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RunningCrewRepository runningCrewRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    void setUpData() {
        if (!Boolean.getBoolean("perf.enabled")) {
            Assumptions.assumeTrue(false, "Set -Dperf.enabled=true to run this manual perf test.");
        }

        runningCrewRepository.deleteAll();
        userRepository.deleteAll();
        regionRepository.deleteAll();

        Region region = regionRepository.save(new Region("서울시", "중구"));
        User host = userRepository.save(User.builder()
                .email("host@test.com")
                .password(passwordEncoder.encode("password"))
                .nickname("host")
                .region(region)
                .preferredLevel(PreferredLevel.BEGINNER)
                .build());

        List<RunningCrew> crews = new ArrayList<>();
        // 고정 시드 데이터: 반경 5km 이내 300건, 반경 밖(약 10km) 200건
        java.util.Random random = new java.util.Random(42);
        for (int i = 0; i < 300; i++) {
            double lat = 37.55 + (random.nextDouble() - 0.5) * 0.04; // 약 ±2.2km
            double lng = 126.97 + (random.nextDouble() - 0.5) * 0.04;
            crews.add(buildCrew(host, region, i, lat, lng));
        }
        for (int i = 300; i < 500; i++) {
            double lat = 37.55 + (random.nextDouble() - 0.5) * 0.16; // 약 ±8.9km
            double lng = 126.97 + (random.nextDouble() - 0.5) * 0.16;
            crews.add(buildCrew(host, region, i, lat, lng));
        }
        runningCrewRepository.saveAll(crews);
    }

    private RunningCrew buildCrew(User host, Region region, int idx, double lat, double lng) {
        return RunningCrew.builder()
                .title("크루" + idx)
                .description("seeded crew")
                .host(host)
                .region(region)
                .meetingTime(LocalDateTime.now().plusDays(idx % 30))
                .place("서울역 인근")
                .latitude(lat)
                .longitude(lng)
                .maxParticipants(10 + (idx % 10))
                .level(PreferredLevel.values()[idx % PreferredLevel.values().length])
                .build();
    }

    @AfterAll
    void tearDown() {
        runningCrewRepository.deleteAll();
        userRepository.deleteAll();
        regionRepository.deleteAll();
    }

    @Test
    void loadNearbyEndpoint() throws Exception {
        int totalRequests = Integer.parseInt(System.getProperty("perf.requests", "500"));
        int concurrency = Integer.parseInt(System.getProperty("perf.concurrency", "50"));
        String query = System.getProperty("perf.query",
                "/api/v1/crews/nearby?latitude=37.55&longitude=126.97&radiusKm=5");
        Duration requestTimeout = Duration.ofSeconds(Long.parseLong(
                System.getProperty("perf.requestTimeoutSeconds", "5")));

        URI target = URI.create("http://localhost:" + port + query);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        List<Long> latenciesNanos = Collections.synchronizedList(new ArrayList<>());
        LongAdder success = new LongAdder();
        LongAdder failure = new LongAdder();
        CountDownLatch latch = new CountDownLatch(totalRequests);

        List<NearbyCrewResponse> sampleResponseHolder = Collections.synchronizedList(new ArrayList<>());
        java.util.concurrent.atomic.AtomicBoolean captured = new java.util.concurrent.atomic.AtomicBoolean(false);
        for (int i = 0; i < totalRequests; i++) {
            pool.submit(() -> {
                long start = System.nanoTime();
                try {
                    HttpRequest request = HttpRequest.newBuilder(target)
                            .timeout(requestTimeout)
                            .GET()
                            .build();
                    HttpResponse<byte[]> response = client.send(request,
                            HttpResponse.BodyHandlers.ofByteArray());
                    if (response.statusCode() == 200) {
                        success.increment();
                        if (captured.compareAndSet(false, true)) {
                            ResponseWrapper parsed = parseResponse(response.body());
                            sampleResponseHolder.addAll(parsed.data);
                        }
                    } else {
                        failure.increment();
                    }
                } catch (Exception e) {
                    failure.increment();
                } finally {
                    latenciesNanos.add(System.nanoTime() - start);
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(2, TimeUnit.MINUTES);
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);

        List<Long> sorted = new ArrayList<>(latenciesNanos);
        Collections.sort(sorted);

        double avgMs = sorted.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0) / 1_000_000.0;

        System.out.printf("""
                        === NearbyCrew baseline ===
                        target: %s
                        completed: %s
                        total: %d, concurrency: %d, success: %d, failure: %d
                        avg: %.2f ms, p50: %.2f ms, p90: %.2f ms, p95: %.2f ms, p99: %.2f ms
                        """,
                target, completed,
                totalRequests, concurrency, success.sum(), failure.sum(),
                avgMs,
                percentile(sorted, 50),
                percentile(sorted, 90),
                percentile(sorted, 95),
                percentile(sorted, 99)
        );

        if (success.sum() == 0) {
            throw new AssertionError("No successful responses; check endpoint or parameters.");
        }

        // 기본 정합성 검증: 응답이 존재하고, 거리/정렬이 요구사항을 만족하는지 확인
        List<NearbyCrewResponse> responses = new ArrayList<>(sampleResponseHolder);
        if (responses.isEmpty()) {
            throw new AssertionError("Empty response payload.");
        }
        double radiusKm = 5.0;
        for (NearbyCrewResponse r : responses) {
            double dist = DistanceCalculator.haversineKm(37.55, 126.97, r.latitude(), r.longitude());
            // 허용 오차를 넉넉히 적용해 부동소수점/정렬 오차 허용
            if (Math.abs(dist - r.distanceKm()) > 0.5) {
                throw new AssertionError("Distance mismatch for crew " + r.crewId());
            }
            if (r.distanceKm() > radiusKm + 0.5) {
                throw new AssertionError("Crew outside radius: " + r.crewId());
            }
        }
        double epsilon = 1e-3; // 1m 허용
        for (int i = 1; i < responses.size(); i++) {
            if (responses.get(i).distanceKm() + epsilon < responses.get(i - 1).distanceKm()) {
                throw new AssertionError("Responses not sorted by distance.");
            }
        }
    }

    private double percentile(List<Long> sortedNanos, int percentile) {
        if (sortedNanos.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil((percentile / 100.0) * sortedNanos.size()) - 1;
        index = Math.min(Math.max(index, 0), sortedNanos.size() - 1);
        return sortedNanos.get(index) / 1_000_000.0;
    }

    private ResponseWrapper parseResponse(byte[] body) throws Exception {
        return objectMapper.readValue(new String(body, StandardCharsets.UTF_8), ResponseWrapper.class);
    }

    private record ResponseWrapper(
            boolean success,
            String code,
            String message,
            List<NearbyCrewResponse> data
    ) {
    }
}
