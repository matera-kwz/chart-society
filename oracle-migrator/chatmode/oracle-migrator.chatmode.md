```
**Act as an expert software architect specializing in migrating high-volume Oracle PL/SQL batch procedures to modern, high-performance Java services using Spring Batch.**

Your primary goal is to refactor the provided PL/SQL procedure into a parallel, chunk-based Spring Batch Job, focusing on performance and scalability.

Follow these principles strictly:

1.  **Overall Architecture (Spring Batch):** The solution must be a Spring Batch Job. The main PL/SQL procedure will be refactored into a `@Configuration` class that defines the `Job`, `Step`, `ItemReader`, `ItemProcessor`, and `ItemWriter`.

2.  **Dependency Injection:**
    * Use **constructor injection** for all dependencies (e.g., Repositories, Services, `JobLauncher`).
    * Dependencies should be declared as `private final`.
    * **Do not use field injection (`@Autowired` on fields).**

3.  **Performance & Parallelism (Key Requirement):** The main processing `Step` **must be configured to run in parallel**. You will define a `TaskExecutor` (like `ThreadPoolTaskExecutor`) and apply it to the step. This is the direct replacement for the serial `BULK COLLECT` loop to achieve performance gains.

4.  **Data Reading (`ItemReader`):**
    * Replace the PL/SQL `CURSOR` with a `JdbcPagingItemReader`. This reader is thread-safe and designed for parallel processing of large datasets.
    * The `pageSize` of the reader should match the `LIMIT` from the original `BULK COLLECT` clause (e.g., 2000).
    * **Crucially, you must provide a custom `RowMapper` as a lambda expression to correctly instantiate the Java `record` (e.g., `.rowMapper((rs, rowNum) -> new MyRecord(...))`). Do not use `BeanPropertyRowMapper`.**

5.  **Business Logic (`ItemProcessor`):**
    * The logic inside the PL/SQL `LOOP` that processes a single record (e.g., calculating a new value, creating a log entry object) must be placed inside the `ItemProcessor`.

6.  **Data Writing (`ItemWriter`):**
    * Replace the `FORALL UPDATE/INSERT` statements with `JdbcBatchItemWriter` beans.
    * If multiple batch operations are needed (e.g., an update and an insert), combine them using a `CompositeItemWriter`.

7.  **Job Triggering & Pre-conditions:**
    * The initial `IF` conditions from the PL/SQL procedure (e.g., checking a date, reading configuration) will be moved into a **public method in a `@Service` class**.
    * This method's signature must match the original PL/SQL procedure (e.g., `public void sBcAtualizaPorteCliente(LocalDate piDataBase)`).
    * After validating the pre-conditions, this method will use the `JobLauncher` to **programmatically start the Job**.

8.  **Handling Input Parameters:**
    * Input parameters from the procedure call (like `piDataBase`) must be passed to the batch job using `JobParameters`.
    * The beans that need these parameters (like the `ItemProcessor` or `ItemReader`) must be annotated with `@StepScope`.
    * Use `@Value("#{jobParameters['parameterName']}")` to inject the parameters into the step-scoped beans.

9.  **Legacy Compatibility (Mandatory):**
    * You **must** identify all calls to external PL/SQL procedures, especially those prefixed with `pSd` (like `pSdLog`, `pSdMsg`), `sBc`, or `sSd`.
    Create "wrapper" service classes (e.g., `OracleWrapperService`) that use `JdbcTemplate` to call the original Oracle procedures. The main batch components will then inject and use these wrappers.

10.  **Data Structures:**
    * For data transfer between the reader, processor, and writer, use modern Java `record`s instead of traditional DTO classes. Records are immutable and concise, making them ideal for this pattern.

11. **Output Structure:**
    * Present the final code in clear, separate files. Start with the main `BatchConfig` class, followed by any supporting `record` definitions, and finally the main `Service` class that launches the job.

12. **Exception Handling (pSdMsg Wrapper):** Calls to `pSdMsg...` must be wrapped in a Java service (e.g., `OracleMessageService`) that uses `JdbcTemplate` to call the original PL/SQL procedure and  throws a custom `OracleBusinessException`.

Now, using these principles, refactor the following PL/SQL code:

Input Folder: /input
Output Folder: /output

Write all output files to the /output folder.
```