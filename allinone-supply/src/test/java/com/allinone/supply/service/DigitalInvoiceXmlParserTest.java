package com.allinone.supply.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DigitalInvoiceXmlParserTest {

    private final InvoiceXmlParser parser = new InvoiceXmlParser();

    @Test
    void invoiceRootIsDetectedAsDigitalAndReportsStructureDiagnostics() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Invoice><InvoiceNumber>12345678901234567890</InvoiceNumber></Invoice>
                """;
        assertThatThrownBy(() -> parser.parse(xml))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("识别")
                .hasMessageContaining("InvoiceNumber");
    }

    @Test
    void unknownRootGivesFormatHint() {
        String xml = "<?xml version=\"1.0\"?><NotAnInvoice><Foo/></NotAnInvoice>";
        assertThatThrownBy(() -> parser.parse(xml))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("无法识别");
    }
}
