package com.matera.hackathon.batch;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Service
public class AtualizaPorteClienteService {

    private final JobLauncher jobLauncher;
    private final BatchConfig batchConfig;
    private final OracleLegacyService oracleLegacyService;

    public AtualizaPorteClienteService(JobLauncher jobLauncher, BatchConfig batchConfig, OracleLegacyService oracleLegacyService) {
        this.jobLauncher = jobLauncher;
        this.batchConfig = batchConfig;
        this.oracleLegacyService = oracleLegacyService;
    }

    public void sBcAtualizaPorteCliente(LocalDate piDataBase) throws JobParametersInvalidException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {
        oracleLegacyService.pSdLogDebug("SDBANCO", "sBcAtualizaPorteCliente", "sBcAtualizaPorteCliente(piDataBase => " + piDataBase + ");", "InicioProcessamento");

        // Busca configuração se atualiza Porte Cliente e Valor do Salário Mínimo na bc_configuracao
        // This part needs to be adapted as the original PL/SQL does a SELECT INTO.
        // For simplicity, let's assume we have a method in OracleLegacyService to get these values.
        // In a real scenario, you might have a dedicated configuration service or repository.
        String vsIndAtualiza = "S"; // Placeholder, assuming it's active for now
        BigDecimal vnSalarioMinimo = BigDecimal.valueOf(1200.00); // Placeholder

        // In a real application, you would fetch these from bc_configuracao
        // For example:
        // Map<String, Object> config = jdbcTemplate.queryForMap("SELECT ind_atualiza_porte_cliente, salario_minimo FROM bc_configuracao");
        // vsIndAtualiza = (String) config.get("ind_atualiza_porte_cliente");
        // vnSalarioMinimo = (BigDecimal) config.get("salario_minimo");

        LocalDate vPrimeiroDiaUtilMes = oracleLegacyService.sBcRoundDiaUtil(piDataBase.withDayOfMonth(1));

        if ("S".equals(vsIndAtualiza) &&
            piDataBase.isEqual(vPrimeiroDiaUtilMes) &&
            vnSalarioMinimo != null &&
            vnSalarioMinimo.compareTo(BigDecimal.ZERO) > 0) {

            JobParameters jobParameters = new JobParametersBuilder()
                    .addDate("run.date", new Date())
                    .addJobParameter("piDataBase", new JobParameter<>(piDataBase, LocalDate.class))
                    .addJobParameter("vnSalarioMinimo", new JobParameter<>(vnSalarioMinimo, BigDecimal.class))
                    .toJobParameters();

            jobLauncher.run(batchConfig.updatePorteClienteJob(null), jobParameters);
        } else {
            oracleLegacyService.pSdLogDebug("SDBANCO", "sBcAtualizaPorteCliente", "Job not launched due to pre-conditions not met.", "FimProcessamento");
        }
    }
}

