@Component
@RequiredArgsConstructor
public class InputDataReader implements ItemReader<InputData> {
    private final List<InputData> inputData;
    private int currentIndex = 0;

    @Override
    public InputData read() {
        if (currentIndex < inputData.size()) {
            return inputData.get(currentIndex++);
        }
        return null;
    }
}

@Component
@RequiredArgsConstructor
public class ApiProcessor implements ItemProcessor<InputData, ProcessedResult> {
    private final WebClient webClient;

    @Override
    public ProcessedResult process(InputData item) {
        ApiResponse response = webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/endpoint")
                .queryParam("startDate", item.getStartDate())
                .queryParam("endDate", item.getEndDate())
                .queryParam("vtype", item.getVtype())
                .build())
            .retrieve()
            .bodyToMono(ApiResponse.class)
            .block();

        return new ProcessedResult(item.getVtype(), response.getCount());
    }
}

@Component
@RequiredArgsConstructor
public class CsvWriter implements ItemWriter<ProcessedResult> {
    @Value("${output.directory}")
    private String outputDirectory;

    @Override
    public void write(List<? extends ProcessedResult> items) throws Exception {
        String fileName = "results_" + System.currentTimeMillis() + ".csv";
        File outputFile = new File(outputDirectory, fileName);

        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            // Write header
            writer.writeNext(new String[]{"VType", "Count"});
            
            // Write data
            for (ProcessedResult item : items) {
                writer.writeNext(new String[]{
                    item.getVtype(),
                    String.valueOf(item.getCount())
                });
            }
        }
    }
}
