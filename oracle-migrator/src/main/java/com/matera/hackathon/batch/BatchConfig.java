package com.matera.hackathon.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    private final DataSource dataSource;
    private final PlatformTransactionManager transactionManager;
    private final JobRepository jobRepository;
    private final OracleLegacyService oracleLegacyService;

    public BatchConfig(DataSource dataSource, PlatformTransactionManager transactionManager, JobRepository jobRepository, OracleLegacyService oracleLegacyService) {
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
        this.jobRepository = jobRepository;
        this.oracleLegacyService = oracleLegacyService;
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("batch-thread-");
        executor.initialize();
        return executor;
    }

    @Bean
    @StepScope
    public JdbcPagingItemReader<PessoaFisicaPorte> pessoaFisicaPorteReader(
            @Value("#{jobParameters['piDataBase']}") LocalDate piDataBase,
            @Value("#{jobParameters['vnSalarioMinimo']}") BigDecimal vnSalarioMinimo) throws Exception {

        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

        String selectClause = "SELECT pe.id_pessoa, pf.vlr_rendimento_mensal, pe.porte_cliente, sBcRetornaPorteClientePF(pf.vlr_rendimento_mensal, :vnSalarioMinimo) as novo_porte";
        String fromClause = "FROM bc_pessoa_fisica pf, bc_pessoa pe";
        String whereClause = "WHERE pe.tipo_pessoa = 'F' AND pe.ind_ativo = 'S' AND pe.id_pessoa = pf.id_pessoa AND pf.vlr_rendimento_mensal IS NOT NULL AND sBcRetornaPorteClientePF(pf.vlr_rendimento_mensal, :vnSalarioMinimo) <> pe.porte_cliente";

        return new JdbcPagingItemReaderBuilder<PessoaFisicaPorte>()
                .name("pessoaFisicaPorteReader")
                .dataSource(dataSource)
                .fetchSize(2000)
                .queryProvider(new org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean() {{
                    setDataSource(dataSource);
                    setSelectClause(selectClause);
                    setFromClause(fromClause);
                    setWhereClause(whereClause);
                    setSortKeys(Collections.singletonMap("id_pessoa", Order.ASCENDING));
                }}.getObject())
                .parameterValues(Collections.singletonMap("vnSalarioMinimo", vnSalarioMinimo))
                .rowMapper(new PessoaFisicaPorte.PessoaFisicaPorteRowMapper())
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<PessoaFisicaPorte, PessoaLogPorteCliente> pessoaFisicaPorteProcessor(
            @Value("#{jobParameters['piDataBase']}") LocalDate piDataBase) {
        return item -> {
            // Business logic from PL/SQL loop
            // In PL/SQL, it was: vTbPessoaPorteClienteLog(vnIndex).id_pessoa := vTbPessoaIdPessoa(vnIndex);
            // vTbPessoaPorteClienteLog(vnindex).data_base := piDataBase ;
            // vTbPessoaPorteClienteLog(vnindex).data_entrada := sysdate;
            // vTbPessoaPorteClienteLog(vnindex).usuario := sSdBuscaUsuario;
            // vTbPessoaPorteClienteLog(vnindex).porte_anterior := vTbPessoaPorteClienteAnterior(vnIndex);
            // vTbPessoaPorteClienteLog(vnindex).porte_atual := vTbPessoaPorteClienteFuturo(vnIndex) ;

            return new PessoaLogPorteCliente(
                    item.idPessoa(),
                    piDataBase,
                    LocalDateTime.now(), // sysdate
                    oracleLegacyService.sSdBuscaUsuario(), // sSdBuscaUsuario
                    item.porteClienteAtual(),
                    item.novoPorteCliente()
            );
        };
    }

    @Bean
    public JdbcBatchItemWriter<PessoaFisicaPorte> updatePessoaPorteWriter() {
        return new JdbcBatchItemWriterBuilder<PessoaFisicaPorte>()
                .dataSource(dataSource)
                .sql("UPDATE bc_pessoa SET porte_cliente = :novoPorteCliente WHERE id_pessoa = :idPessoa")
                .itemSqlParameterSourceProvider(item -> (org.springframework.jdbc.core.namedparam.SqlParameterSource) new NamedParameterJdbcTemplate(dataSource).getJdbcOperations().queryForMap("SELECT :idPessoa as idPessoa, :novoPorteCliente as novoPorteCliente FROM DUAL", Map.of("idPessoa", item.idPessoa(), "novoPorteCliente", item.novoPorteCliente())))
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<PessoaLogPorteCliente> insertPessoaLogPorteClienteWriter() {
        return new JdbcBatchItemWriterBuilder<PessoaLogPorteCliente>()
                .dataSource(dataSource)
                .sql("INSERT INTO bc_pessoa_log_porte_cliente (id_pessoa, data_base, data_entrada, usuario, porte_anterior, porte_atual) VALUES (:idPessoa, :dataBase, :dataEntrada, :usuario, :porteAnterior, :porteAtual)")
                .itemSqlParameterSourceProvider(item -> (org.springframework.jdbc.core.namedparam.SqlParameterSource) new NamedParameterJdbcTemplate(dataSource).getJdbcOperations().queryForMap("SELECT :idPessoa as idPessoa, :dataBase as dataBase, :dataEntrada as dataEntrada, :usuario as usuario, :porteAnterior as porteAnterior, :porteAtual as porteAtual FROM DUAL", Map.of("idPessoa", item.idPessoa(), "dataBase", item.dataBase(), "dataEntrada", item.dataEntrada(), "usuario", item.usuario(), "porteAnterior", item.porteAnterior(), "porteAtual", item.porteAtual())))
                .build();
    }

    @Bean
    public ItemWriter<PessoaLogPorteCliente> compositePessoaWriter(
            JdbcBatchItemWriter<PessoaFisicaPorte> updatePessoaPorteWriter,
            JdbcBatchItemWriter<PessoaLogPorteCliente> insertPessoaLogPorteClienteWriter) {

        // The processor outputs PessoaLogPorteCliente, but updatePessoaPorteWriter expects PessoaFisicaPorte.
        // We need to adapt the updatePessoaPorteWriter to accept PessoaLogPorteCliente.
        ItemWriter<PessoaLogPorteCliente> adaptedUpdatePessoaPorteWriter = items -> {
            for (PessoaLogPorteCliente logItem : items) {
                updatePessoaPorteWriter.write((org.springframework.batch.item.Chunk<? extends PessoaFisicaPorte>) Collections.singletonList(new PessoaFisicaPorte(
                        logItem.idPessoa(), null, logItem.porteAnterior(), logItem.porteAtual())));
            }
        };

        return new CompositeItemWriterBuilder<PessoaLogPorteCliente>()
                .delegates(adaptedUpdatePessoaPorteWriter, insertPessoaLogPorteClienteWriter)
                .build();
    }

    @Bean
    public Step updatePorteClienteStep(
            JdbcPagingItemReader<PessoaFisicaPorte> pessoaFisicaPorteReader,
            ItemProcessor<PessoaFisicaPorte, PessoaLogPorteCliente> pessoaFisicaPorteProcessor,
            ItemWriter<PessoaLogPorteCliente> compositePessoaWriter,
            TaskExecutor taskExecutor) {
        return new StepBuilder("updatePorteClienteStep", jobRepository)
                .<PessoaFisicaPorte, PessoaLogPorteCliente>chunk(2000, transactionManager)
                .reader(pessoaFisicaPorteReader)
                .processor(pessoaFisicaPorteProcessor)
                .writer(compositePessoaWriter)
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    public Job updatePorteClienteJob(Step updatePorteClienteStep) {
        return new JobBuilder("updatePorteClienteJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .flow(updatePorteClienteStep)
                .end()
                .build();
    }
}

