package com.allinone.supply.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import com.allinone.supply.domain.Payment;
import com.allinone.supply.domain.PaymentLine;
import com.allinone.supply.mapper.PaymentMapper;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Date;
import java.util.UUID;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

class PaymentMapperIntegrationTest {
    @Test void persistsHeaderLinesAndSubmissionUsingActualMybatisBindings() throws Exception {
        JdbcDataSource ds = new JdbcDataSource(); ds.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        try (Connection connection = ds.getConnection(); var sql = connection.createStatement()) {
            sql.execute("create table payments(id bigint primary key,number varchar(64),supplier_id bigint,amount_cents bigint,threshold_exceeded char(1) not null,status varchar(20),current_node varchar(32),creator_id bigint,creation_key varchar(80),revision int,remark varchar(500),review_comment varchar(500),director_comment varchar(500),create_time timestamp not null,update_time timestamp not null,deleted char(1))");
            sql.execute("create table payment_lines(id bigint primary key,payment_id bigint,invoice_id bigint,allocated_cents bigint,create_time timestamp not null)");
            sql.execute("create table invoices(document_id bigint primary key,invoice_number varchar(32),total_cents bigint)");
            sql.execute("create table suppliers(document_id bigint primary key,name varchar(100))");
        }
        Configuration config = new Configuration(new Environment("test", new JdbcTransactionFactory(), ds));
        String resource = "mapper/supply/PaymentMapper.xml";
        try (InputStream xml = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(xml, config, resource, config.getSqlFragments()).parse();
        }
        try (SqlSession session = new SqlSessionFactoryBuilder().build(config).openSession()) {
            PaymentMapper mapper = session.getMapper(PaymentMapper.class);
            Payment p = new Payment(); p.setId(1L); p.setNumber("PAY-TEST"); p.setSupplierId(2L); p.setAmountCents(10_000_001L);
            p.setThresholdExceeded("1"); p.setStatus("DRAFT"); p.setCreatorId(3L); p.setCreationKey("test");
            p.setCreateTime(new Date()); p.setUpdateTime(new Date()); p.setRevision(0);
            assertThat(mapper.insertPayment(p)).isEqualTo(1);
            PaymentLine line = new PaymentLine(); line.setId(4L); line.setPaymentId(1L); line.setInvoiceId(5L); line.setAllocatedCents(10_000_001L);
            assertThat(mapper.insertPaymentLine(line)).isEqualTo(1);
            assertThat(mapper.selectLines(1L)).hasSize(1);
            assertThat(mapper.submitPayment(p)).isEqualTo(1);
            Payment stored = mapper.selectById(1L);
            assertThat(stored.getStatus()).isEqualTo("IN_REVIEW");
            assertThat(stored.getThresholdExceeded()).isEqualTo("1");
            assertThat(mapper.submitPayment(p)).isZero();
            session.rollback();
        }
    }
}
