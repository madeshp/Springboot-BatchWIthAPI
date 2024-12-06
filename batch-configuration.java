@Configuration
@EnableBatchProcessing
@EnableScheduling
@EnableAsync
@Slf4j
public class BatchConfig {

    @Bean
    public Job processDataJob(
            JobRepository jobRepository,
            Step processDataStep) {
        return new JobBuilder("processDataJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(processDataStep)
            .build();
    }

    @Bean
    public Step processDataStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<InputData> reader,
            ItemProcessor<InputData, ProcessedResult> processor,
            ItemWriter<ProcessedResult> writer) {
        return new StepBuilder("processDataStep", jobRepository)
            .<InputData, ProcessedResult>chunk(10, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean
    public WebClient webClient(@Value("${api.base-url}") String baseUrl) {
        return WebClient.builder()
            .baseUrl(baseUrl)
            .build();
    }
}
