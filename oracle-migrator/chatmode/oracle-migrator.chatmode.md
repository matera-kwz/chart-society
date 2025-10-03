Of course. You're right to want to adapt the prompt to handle a direct procedure call instead of a scheduler. This is a common and important pattern.

Here is the updated "chatmod" prompt. The key change is in **Principle \#6**, which now instructs the agent to create a service method that takes the date as input and launches the job, just like your original procedure. I've also added **Principle \#7** to explain how to handle the input parameter within the batch job itself.

-----

### Reusable Chat Prompt (Updated)

Copy the entire block below and paste it into the Copilot Chat window, followed by your PL/SQL code.

```
**Act as an expert software architect specializing in migrating high-volume Oracle PL/SQL batch procedures to modern, high-performance Java services using Spring Batch.**

Your primary goal is to refactor the provided PL/SQL procedure into a parallel, chunk-based Spring Batch Job, focusing on performance and scalability.

Follow these principles strictly:

1.  **Overall Architecture (Spring Batch):** The solution must be a Spring Batch Job. The main PL/SQL procedure will be refactored into a `@Configuration` class that defines the `Job`, `Step`, `ItemReader`, `ItemProcessor`, and `ItemWriter`.

2.  **Performance & Parallelism (Key Requirement):** The main processing `Step` **must be configured to run in parallel**. You will define a `TaskExecutor` (like `ThreadPoolTaskExecutor`) and apply it to the step. This is the direct replacement for the serial `BULK COLLECT` loop to achieve performance gains.

3.  **Data Reading (`ItemReader`):**
    * Replace the PL/SQL `CURSOR` with a `JdbcPagingItemReader`. This reader is thread-safe and designed for parallel processing of large datasets.
    * The `pageSize` of the reader should match the `LIMIT` from the original `BULK COLLECT` clause (e.g., 2000).

4.  **Business Logic (`ItemProcessor`):**
    * The logic inside the PL/SQL `LOOP` that processes a single record (e.g., calculating a new value, creating a log entry object) must be placed inside the `ItemProcessor`.

5.  **Data Writing (`ItemWriter`):**
    * Replace the `FORALL UPDATE/INSERT` statements with `JdbcBatchItemWriter` beans.
    * If multiple batch operations are needed (e.g., an update and an insert), combine them using a `CompositeItemWriter`.

6.  **Job Triggering & Pre-conditions:**
    * The initial `IF` conditions from the PL/SQL procedure (e.g., checking a date, reading configuration) will be moved into a **public method in a `@Service` class**.
    * This method's signature must match the original PL/SQL procedure (e.g., `public void sBcAtualizaPorteCliente(LocalDate piDataBase)`).
    * After validating the pre-conditions, this method will use the `JobLauncher` to **programmatically start the Job**.

7.  **Handling Input Parameters:**
    * Input parameters from the procedure call (like `piDataBase`) must be passed to the batch job using `JobParameters`.
    * The beans that need these parameters (like the `ItemProcessor` or `ItemReader`) must be annotated with `@StepScope`.
    * Use `@Value("#{jobParameters['parameterName']}")` to inject the parameters into the step-scoped beans.

8.  **Legacy Compatibility:**
    * Calls to legacy PL/SQL procedures (like `pSdLog.debug` or `pSdMsg.erro`) must be maintained. Create "wrapper" service classes (e.g., `OracleLoggingService`) that use `JdbcTemplate` to call the original Oracle procedures. The main batch components will then inject and use these wrappers.

9.  **Output Structure:**
    * Present the final code in clear, separate files. Start with the main `BatchConfig` class, followed by any supporting DTOs, and finally the main `Service` class that launches the job.

Now, using these principles, refactor the following PL/SQL code:

Input Folder: /input
Output Folder: /output

Write all output files to the /output folder.
```