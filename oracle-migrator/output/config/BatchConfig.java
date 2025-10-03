package config;

import dto.PessoaPorteClienteRecord;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import service.OracleWrapperService;
import javax.sql.DataSource;
import java.time.LocalDate;

@Configuration
@EnableBatchProcessing
public class BatchConfig {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;
    private final OracleWrapperService oracleWrapperService;

    public BatchConfig(JobBuilderFactory jobBuilderFactory,
                       StepBuilderFactory stepBuilderFactory,
                       DataSource dataSource,
                       OracleWrapperService oracleWrapperService) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.dataSource = dataSource;
        this.oracleWrapperService = oracleWrapperService;
    }

    @Bean
    public TaskExecutor porteClienteTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("PorteCliente-");
        executor.initialize();
        return executor;
    }

    @Bean
    @Primary
    public Job atualizaPorteClienteJob(Step atualizaPorteClienteStep) {
        return jobBuilderFactory.get("atualizaPorteClienteJob")
                .incrementer(new RunIdIncrementer())
                .flow(atualizaPorteClienteStep)
                .end()
                .build();
    }

    @Bean
    public Step atualizaPorteClienteStep(JdbcPagingItemReader<PessoaPorteClienteRecord> reader,
                                         ItemProcessor<PessoaPorteClienteRecord, PessoaPorteClienteRecord> processor,
                                         ItemWriter<PessoaPorteClienteRecord> writer,
                                         TaskExecutor porteClienteTaskExecutor) {
        return stepBuilderFactory.get("atualizaPorteClienteStep")
                .<PessoaPorteClienteRecord, PessoaPorteClienteRecord>chunk(2000)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .taskExecutor(porteClienteTaskExecutor)
                .throttleLimit(8)
                .build();
    }

    @Bean
    @StepScope
    public JdbcPagingItemReader<PessoaPorteClienteRecord> pessoaPorteClienteReader(
            @Value("#{jobParameters['piDataBase']}") LocalDate piDataBase,
            @Value("#{jobParameters['vnSalarioMinimo']}") Double vnSalarioMinimo) throws Exception {
        JdbcPagingItemReader<PessoaPorteClienteRecord> reader = new JdbcPagingItemReader<>();
        reader.setDataSource(dataSource);
        reader.setPageSize(2000);
        SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();
        queryProvider.setDataSource(dataSource);
        queryProvider.setSelectClause("select pe.id_pessoa, pf.vlr_rendimento_mensal, pe.porte_cliente, sBcRetornaPorteClientePF(pf.vlr_rendimento_mensal, :vnSalarioMinimo) as novo_porte");
        queryProvider.setFromClause("from bc_pessoa_fisica pf, bc_pessoa pe");
        queryProvider.setWhereClause("pe.tipo_pessoa = 'F' and pe.ind_ativo = 'S' and pe.id_pessoa = pf.id_pessoa and pf.vlr_rendimento_mensal is not null and sBcRetornaPorteClientePF(pf.vlr_rendimento_mensal, :vnSalarioMinimo) <> pe.porte_cliente");
        queryProvider.setSortKey("pe.id_pessoa");
        reader.setQueryProvider(queryProvider.getObject());
        reader.setRowMapper((rs, rowNum) -> new PessoaPorteClienteRecord(
                rs.getLong("id_pessoa"),
                rs.getBigDecimal("vlr_rendimento_mensal"),
                rs.getString("porte_cliente"),
                rs.getString("novo_porte"),
                piDataBase
        ));
        return reader;
    }

    @Bean
    @StepScope
    public ItemProcessor<PessoaPorteClienteRecord, PessoaPorteClienteRecord> pessoaPorteClienteProcessor(
            @Value("#{jobParameters['piDataBase']}") LocalDate piDataBase) {
        return pessoa -> {
            // Business logic: create log entry, set fields, etc.
            pessoa = pessoa.withUsuario(oracleWrapperService.getUsuario());
            return pessoa;
        };
    }

    @Bean
    @StepScope
    public ItemWriter<PessoaPorteClienteRecord> pessoaPorteClienteWriter(JdbcBatchItemWriter<PessoaPorteClienteRecord> pessoaUpdateWriter,
                                                                        JdbcBatchItemWriter<PessoaPorteClienteRecord> pessoaLogWriter) {
        return items -> {
            pessoaUpdateWriter.write(items);
            pessoaLogWriter.write(items);
        };
    }

    @Bean
    @StepScope
    public JdbcBatchItemWriter<PessoaPorteClienteRecord> pessoaUpdateWriter() {
        JdbcBatchItemWriter<PessoaPorteClienteRecord> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setSql("update bc_pessoa set porte_cliente = :novoPorte where id_pessoa = :idPessoa");
        writer.setItemSqlParameterSourceProvider(item -> {
            var params = new org.springframework.jdbc.core.namedparam.MapSqlParameterSource();
            params.addValue("novoPorte", item.novoPorte());
            params.addValue("idPessoa", item.idPessoa());
            return params;
        });
        return writer;
    }

    @Bean
    @StepScope
    public JdbcBatchItemWriter<PessoaPorteClienteRecord> pessoaLogWriter() {
        JdbcBatchItemWriter<PessoaPorteClienteRecord> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setSql("insert into bc_pessoa_log_porte_cliente (id_pessoa, data_base, data_entrada, usuario, porte_anterior, porte_atual) values (:idPessoa, :dataBase, :dataEntrada, :usuario, :porteAnterior, :novoPorte)");
        writer.setItemSqlParameterSourceProvider(item -> {
            var params = new org.springframework.jdbc.core.namedparam.MapSqlParameterSource();
            params.addValue("idPessoa", item.idPessoa());
            params.addValue("dataBase", item.dataBase());
            params.addValue("dataEntrada", java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            params.addValue("usuario", item.usuario());
            params.addValue("porteAnterior", item.porteAnterior());
            params.addValue("novoPorte", item.novoPorte());
            return params;
        });
        return writer;
    }
}
