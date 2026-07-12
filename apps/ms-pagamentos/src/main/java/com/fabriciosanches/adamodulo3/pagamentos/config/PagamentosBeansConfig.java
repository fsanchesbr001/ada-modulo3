package com.fabriciosanches.adamodulo3.pagamentos.config;

import com.fabriciosanches.adamodulo3.pagamentos.adapter.in.messaging.ComprovanteGeradoConsumer;
import com.fabriciosanches.adamodulo3.pagamentos.adapter.in.messaging.PagarEventConsumer;
import com.fabriciosanches.adamodulo3.pagamentos.adapter.out.gatewaymock.MockGatewayClient;
import com.fabriciosanches.adamodulo3.pagamentos.adapter.out.persistence.mysql.JdbcPagamentoRepository;
import com.fabriciosanches.adamodulo3.pagamentos.application.CompensatePagamentoUseCase;
import com.fabriciosanches.adamodulo3.pagamentos.application.ProcessPagamentoUseCase;
import com.fabriciosanches.adamodulo3.pagamentos.application.port.out.PagamentoRepository;
import com.fabriciosanches.adamodulo3.pagamentos.domain.PagamentoFinalityPolicy;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class PagamentosBeansConfig {

    @Bean
    PagamentoRepository pagamentoRepository(DataSource dataSource) {
        return new JdbcPagamentoRepository(new JdbcTemplate(dataSource));
    }

    @Bean
    MockGatewayClient mockGatewayClient() {
        return new MockGatewayClient();
    }

    @Bean
    PagamentoFinalityPolicy pagamentoFinalityPolicy() {
        return new PagamentoFinalityPolicy();
    }

    @Bean
    CompensatePagamentoUseCase compensatePagamentoUseCase(
            PagamentoRepository repository,
            PagamentosObservability observability) {
        return new CompensatePagamentoUseCase(repository, observability);
    }

    @Bean
    ProcessPagamentoUseCase processPagamentoUseCase(
            PagamentoRepository repository,
            MockGatewayClient mockGatewayClient,
            CompensatePagamentoUseCase compensatePagamentoUseCase,
            PagamentoFinalityPolicy pagamentoFinalityPolicy,
            PagamentosObservability observability) {
        return new ProcessPagamentoUseCase(
                repository,
                mockGatewayClient,
                compensatePagamentoUseCase,
                pagamentoFinalityPolicy,
                observability);
    }

    @Bean
    PagarEventConsumer pagarEventConsumer(ProcessPagamentoUseCase processPagamentoUseCase) {
        return new PagarEventConsumer(processPagamentoUseCase);
    }

    @Bean
    ComprovanteGeradoConsumer comprovanteGeradoConsumer(ProcessPagamentoUseCase processPagamentoUseCase) {
        return new ComprovanteGeradoConsumer(processPagamentoUseCase);
    }
}
