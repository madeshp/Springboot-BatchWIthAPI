batch-processor/
│
├── pom.xml
│
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── example/
│       │           └── batchprocessor/
│       │               ├── BatchProcessorApplication.java
│       │               ├── config/
│       │               │   ├── BatchConfig.java
│       │               │   └── SSLConfiguration.java
│       │               ├── model/
│       │               │   ├── InputData.java
│       │               │   ├── ApiResponse.java
│       │               │   └── ProcessedResult.java
│       │               ├── batch/
│       │               │   ├── InputDataReader.java
│       │               │   ├── ApiProcessor.java
│       │               │   └── CsvWriter.java
│       │               ├── controller/
│       │               │   └── BatchController.java
│       │               └── scheduler/
│       │                   └── JobScheduler.java
│       │
│       └── resources/
│           ├── application.yml
│           ├── keystore.p12
│           └── truststore.jks
