@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
@Slf4j
public class BatchController {
    private final JobLauncher jobLauncher;
    private final Job processDataJob;

    @PostMapping("/start")
    public ResponseEntity<String> startJob(@RequestBody List<InputData> inputData) {
        try {
            JobParameters parameters = new JobParametersBuilder()
                .addString("JobID", String.valueOf(System.currentTimeMillis()))
                .addString("inputData", new ObjectMapper().writeValueAsString(inputData))
                .toJobParameters();

            CompletableFuture.runAsync(() -> {
                try {
                    jobLauncher.run(processDataJob, parameters);
                } catch (Exception e) {
                    log.error("Error running job", e);
                }
            });

            return ResponseEntity.ok("Job started successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error starting job: " + e.getMessage());
        }
    }
}

@Component
@RequiredArgsConstructor
public class JobScheduler {
    private final JobLauncher jobLauncher;
    private final Job processDataJob;

    @Scheduled(cron = "${scheduler.cron}")
    public void scheduleJob() {
        try {
            JobParameters parameters = new JobParametersBuilder()
                .addString("JobID", String.valueOf(System.currentTimeMillis()))
                .toJobParameters();

            jobLauncher.run(processDataJob, parameters);
        } catch (Exception e) {
            log.error("Error in scheduled job", e);
        }
    }
}
