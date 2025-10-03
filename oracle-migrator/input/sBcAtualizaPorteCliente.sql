/*
===============================================================================================
 Objetivo: Atualização automática do porte cliente
===============================================================================================*/
--
create or replace procedure SDBANCO.sBcAtualizaPorteCliente
( piDataBase     IN  date ) is
  --
  TYPE tpTbPessoaIdPessoa      is table of bc_pessoa.id_pessoa%TYPE index by binary_integer;
  vTbPessoaIdPessoa            tpTbPessoaIdPessoa;
  --
  TYPE tpTbPessoaVlrFatMensal  is table of bc_pessoa.vlr_faturamento_mensal%TYPE index by binary_integer;
  vTbPessoaVlrFatMensal            tpTbPessoaVlrFatMensal;
  --
  TYPE tpTbPessoaPorteCliente  is table of bc_pessoa.porte_cliente%TYPE index by binary_integer;
  vTbPessoaPorteClienteAnterior       tpTbPessoaPorteCliente;
  vTbPessoaPorteClienteFuturo         tpTbPessoaPorteCliente;
  --
  TYPE tpTbPessoaPorteClienteLog  is table of bc_pessoa_log_porte_cliente%ROWTYPE ;
  vTbPessoaPorteClienteLog            tpTbPessoaPorteClienteLog;
  --
  vsIndAtualiza   bc_configuracao.ind_atualiza_porte_cliente%TYPE;
  vPrimeiroDiaUtilMes date;
  vnSalarioMinimo     number (18,2);
  --
  cntnLimiteFetchCursorPessoa  constant number(5) := 2000;
  --
  -- Cursor de todas as pessoas que precisam atualizar o rang
  -- 
  cursor curPessoa is
    select pe.id_pessoa , 
           pf.vlr_rendimento_mensal,
           pe.porte_cliente,
           sBcRetornaPorteClientePF(pf.vlr_rendimento_mensal, vnSalarioMinimo) novo_porte
    from   bc_pessoa_fisica pf,
           bc_pessoa pe
    where  pe.tipo_pessoa = 'F'        and
           pe.ind_ativo = 'S'          and           
           pe.id_pessoa = pf.id_pessoa and
           pf.vlr_rendimento_mensal is not null and
           sBcRetornaPorteClientePF(pf.vlr_rendimento_mensal, vnSalarioMinimo) <> pe.porte_cliente;
   --
begin
  --
  pSdLog.debug('SDBANCO' ,'sBcAtualizaPorteCliente', 'sBcAtualizaPorteCliente('|| chr(13)||
                                                     'piDataBase  => '|| piDataBase      || 
                                                     ');' , pSdLog.cntsMarcaInicioProcessamento );
  --
  -- Busca configuração se atualiza Porte Cliente e Valor do Salário Mínimo na bc_configuracao
  --
  select nvl(bc.ind_atualiza_porte_cliente, 'N'), 
         salario_minimo 
  into   vsIndAtualiza, 
         vnSalarioMinimo
  from   bc_configuracao bc;
  --
  vPrimeiroDiaUtilMes :=  sBcRoundDiaUtil( trunc ( piDataBase, 'MM' ) );
  --
  -- Verifica se a configuração para atualizar o porte cliente automáticamente
  -- está ativa, se a data é o primeiro dia do mês, se o salário mínimo está
  -- cadastrado e é um valor válido
  --
  if  vsIndAtualiza = 'S'              and 
      piDataBase = vPrimeiroDiaUtilMes and
      vnSalarioMinimo is not null      and
      vnSalarioMinimo > 0              then
    --
    open curPessoa ;
      --
      loop
        --
        fetch curPessoa BULK COLLECT into  vTbPessoaIdPessoa, 
                                           vTbPessoaVlrFatMensal, 
                                           vTbPessoaPorteClienteAnterior,
                                           vTbPessoaPorteClienteFuturo LIMIT cntnLimiteFetchCursorPessoa;
          --
          if vTbPessoaIdPessoa.first is not null then
            --
            -- Limpa tabela de histórico
            --
            vTbPessoaPorteClienteLog:=tpTbPessoaPorteClienteLog();
            --
            for vnIndex in vTbPessoaIdPessoa.first..vTbPessoaIdPessoa.last loop
              --
              -- inserir na tabela de log vTbPessoaPorteClienteLog
              --
              vTbPessoaPorteClienteLog.extend;
              --
              vTbPessoaPorteClienteLog(vnIndex).id_pessoa      := vTbPessoaIdPessoa(vnIndex);     
              vTbPessoaPorteClienteLog(vnindex).data_base      := piDataBase ;
              vTbPessoaPorteClienteLog(vnindex).data_entrada   := sysdate;
              vTbPessoaPorteClienteLog(vnindex).usuario        := sSdBuscaUsuario;
              vTbPessoaPorteClienteLog(vnindex).porte_anterior := vTbPessoaPorteClienteAnterior(vnIndex);
              vTbPessoaPorteClienteLog(vnindex).porte_atual    := vTbPessoaPorteClienteFuturo(vnIndex)  ;  
              --
            end loop;
            --
            -- Atualizando porte cliente na bc_pessoa
            --
            pBcStatusPessoa.vStatus := 'S';
            --
            forall vnContador in 1 .. vTbPessoaIdPessoa.count
            update bc_pessoa
            set    porte_cliente  =  vTbPessoaPorteClienteFuturo(vnContador)
            where  id_pessoa = vTbPessoaIdPessoa(vnContador);
            --
            -- Gravando log de alteração na bc_pessoa_log_porte_cliente
            --
            forall vnContador in 1 .. vTbPessoaPorteClienteLog.count
            insert into bc_pessoa_log_porte_cliente
            values vTbPessoaPorteClienteLog(vnContador);
            --
            -- commitando alterações parciais
            --
            commit;
            --
            pBcStatusPessoa.vStatus := 'N';
            --
          end if;
          --
        exit when curPessoa%notfound; 
        --    
      end loop;     
  --
  end if;
  --
exception
  when others then
    pSdMsg.trataExcecao( sqlcode, sqlerrm, 'sBcAtualizaPorteCliente' );
  --
end;
/