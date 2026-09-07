package com.allinone.supply.service;

import static org.junit.jupiter.api.Assertions.*;

import com.allinone.supply.domain.ParsedInvoice;
import org.junit.jupiter.api.Test;

class InvoiceXmlParserTest {
    private final InvoiceXmlParser parser = new InvoiceXmlParser();

    @Test
    void parsesDemoInvoiceV1() {
        ParsedInvoice result = parser.parse("""
                <DemoInvoice schemaVersion="1.0"><Header><InvoiceNumber>0001</InvoiceNumber><InvoiceType>DEMO_GENERAL</InvoiceType><IssueDate>2026-09-06</IssueDate></Header>
                <Seller><Name>供应商</Name><TaxId>SELLER</TaxId></Seller><Buyer><Name>采购公司</Name><TaxId>BUYER</TaxId></Buyer>
                <Lines><Line><Name>商品A</Name><Unit>个</Unit><Quantity>2</Quantity><UnitPrice>10</UnitPrice><TaxRate>0.13</TaxRate></Line></Lines>
                <Totals><Amount>20.00</Amount><TaxAmount>2.60</TaxAmount><TotalAmount>22.60</TotalAmount></Totals></DemoInvoice>
                """);
        assertEquals("0001", result.getInvoiceNumber());
        assertEquals("供应商", result.getSellerName());
        assertEquals(1, result.getLines().size());
        assertEquals("22.60", result.getTotalAmount());
    }

    @Test
    void rejectsDoctype() {
        assertThrows(RuntimeException.class, () -> parser.parse("<!DOCTYPE DemoInvoice [<!ENTITY xxe SYSTEM 'file:///etc/passwd'>]><DemoInvoice schemaVersion=\"1.0\"><Header/></DemoInvoice>"));
    }
}
