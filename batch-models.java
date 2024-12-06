@Data
@AllArgsConstructor
@NoArgsConstructor
public class InputData {
    private LocalDate startDate;
    private LocalDate endDate;
    private String vtype;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse {
    private int count;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessedResult {
    private String vtype;
    private int count;
}
