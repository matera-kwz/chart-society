package service;

import config.BatchConfig;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import service.OracleWrapperService;
import service.OracleMessageService;
import java.time.LocalDate;
import javax.sql.DataSource;

@Service
public class AtualizaPorteClienteService {
    private final JobLauncher jobLauncher;
    private final Job atualizaPorteClienteJob;
    private final OracleWrapperService oracleWrapperService;
    private final OracleMessageService oracleMessageService;
    private final DataSource dataSource;

    public AtualizaPorteClienteService(JobLauncher jobLauncher,
                                       Job atualizaPorteClienteJob,
                                       OracleWrapperService oracleWrapperService,
                                       OracleMessageService oracleMessageService,
                                       DataSource dataSource) {
        this.jobLauncher = jobLauncher;
        this.atualizaPorteClienteJob = atualizaPorteClienteJob;
        this.oracleWrapperService = oracleWrapperService;
        this.oracleMessageService = oracleMessageService;
        this.dataSource = dataSource;
    }

    @Transactional
    public void sBcAtualizaPorteCliente(LocalDate piDataBase) {
        try {
            oracleWrapperService.logDebug("SDBANCO", "sBcAtualizaPorteCliente", "sBcAtualizaPorteCliente(piDataBase => " + piDataBase + ")");
            var config = oracleWrapperService.getConfiguracao();
            String vsIndAtualiza = config.indAtualizaPorteCliente();
            Double vnSalarioMinimo = config.salarioMinimo();
            LocalDate vPrimeiroDiaUtilMes = oracleWrapperService.roundDiaUtil(piDataBase.withDayOfMonth(1));
            if ("S".equals(vsIndAtualiza) &&
                piDataBase.equals(vPrimeiroDiaUtilMes) &&
                vnSalarioMinimo != null &&
                vnSalarioMinimo > 0) {
                var jobParameters = new JobParametersBuilder()
                        .addString("piDataBase", piDataBase.toString())
                        .addDouble("vnSalarioMinimo", vnSalarioMinimo)
                        .toJobParameters();
                jobLauncher.run(atualizaPorteClienteJob, jobParameters);
            }
        } catch (Exception e) {
            oracleMessageService.trataExcecao(e, "sBcAtualizaPorteCliente");
        }
    }
}
