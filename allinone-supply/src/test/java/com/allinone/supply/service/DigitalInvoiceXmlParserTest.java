package com.allinone.supply.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.allinone.supply.domain.ParsedInvoice;
import com.allinone.supply.domain.ParsedInvoiceLine;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DigitalInvoiceXmlParserTest {

    private final InvoiceXmlParser parser = new InvoiceXmlParser();

    @Test
    void parsesMaskedDigitalInvoiceFixture() throws Exception {
        ParsedInvoice result = parser.parse(fixtureXml());

        assertThat(result.getInvoiceNumber()).isEqualTo("20260000000000000001");
        assertThat(result.getInvoiceType()).isEqualTo("增值税专用发票");
        assertThat(result.getIssueDate()).isEqualTo("2026-09-07");
        assertThat(result.getSellerName()).isEqualTo("演示销售方有限公司");
        assertThat(result.getSellerTaxId()).isEqualTo("DEMOSELLERDEMO0001");
        assertThat(result.getBuyerName()).isEqualTo("演示购买方有限公司");
        assertThat(result.getBuyerTaxId()).isEqualTo("DEMOBUYERDEMO0001");
        assertThat(result.getAmount()).isEqualTo("29726.85");
        assertThat(result.getTaxAmount()).isEqualTo("3864.49");
        assertThat(result.getTotalAmount()).isEqualTo("33591.34");

        assertThat(result.getLines()).hasSize(1);
        ParsedInvoiceLine line = result.getLines().get(0);
        assertThat(line.getName()).contains("低温真空袋");
        assertThat(line.getUnit()).isEqualTo("只");
        assertThat(line.getQuantity()).isEqualTo("69982");
        assertThat(line.getUnitPrice()).isEqualTo("0.4247785081524");
        assertThat(line.getTaxRate()).isEqualTo("0.13");
        assertThat(line.getAmount()).isEqualTo("29726.85");
        assertThat(line.getTaxAmount()).isEqualTo("3864.49");
    }

    @Test
    void rejectsInvoiceNumberWithWrongLength() throws Exception {
        String xml = fixtureXml().replace("20260000000000000001", "2026000000000000000");
        assertThatThrownBy(() -> parser.parse(xml))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("20 位");
    }

    @Test
    void unknownRootGivesFormatHint() {
        String xml = "<?xml version=\"1.0\"?><NotAnInvoice><Foo/></NotAnInvoice>";
        assertThatThrownBy(() -> parser.parse(xml))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("无法识别");
    }

    private String fixtureXml() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/invoices/digital-invoice-01.fixture.xml")) {
            if (input == null) throw new IllegalStateException("测试夹具 digital-invoice-01.fixture.xml 缺失");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
