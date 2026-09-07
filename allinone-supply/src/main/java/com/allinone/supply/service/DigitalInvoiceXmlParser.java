package com.allinone.supply.service;

import com.allinone.common.exception.ServiceException;
import com.allinone.supply.domain.ParsedInvoice;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 数电票(蓝字)XML 适配器骨架。
 *
 * 真实税务 schema 待用户提供脱敏票样后校准(见 doc/M3_数电票解析_契约.md §4/§6)；
 * 当前仅做识别与结构诊断——结构不匹配时返回可操作错误,不返回未经确认的字段映射,
 * 避免把演示/臆测结果当作真实解析结果。
 */
public final class DigitalInvoiceXmlParser {
    /** 待真实票样确认的期望元素(语义字段,按公共知识预置,回填映射时替换)。 */
    private static final List<String> EXPECTED = List.of(
            "InvoiceNumber", "InvoiceType", "IssueDate",
            "SellerName", "SellerTaxId", "BuyerName", "BuyerTaxId",
            "Amount", "TaxAmount", "TotalAmount");

    private DigitalInvoiceXmlParser() {
    }

    public static ParsedInvoice parse(Document doc) {
        Set<String> present = elementNames(doc);
        List<String> missing = EXPECTED.stream().filter(name -> !containsName(present, name)).collect(Collectors.toList());
        String names = present.stream().limit(60).collect(Collectors.joining(", "));
        String message = "已识别为数电票(蓝字)XML,但字段结构未匹配当前登记的 schema"
                + (missing.isEmpty() ? "。" : "，缺少期望元素：[" + String.join("、", missing) + "]。")
                + " 文件中出现的元素：[" + names + (present.size() > 60 ? ", …]" : "]")
                + "。请提供真实数电票 XML 以校准映射(见 doc/M3_数电票解析_契约.md §4/§6)。";
        throw new ServiceException(message);
    }

    private static Set<String> elementNames(Document doc) {
        Set<String> set = new LinkedHashSet<>();
        NodeList all = doc.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Node node = all.item(i);
            if (node instanceof Element element) {
                set.add(localName(element));
            }
        }
        return set;
    }

    private static boolean containsName(Set<String> names, String expected) {
        return names.contains(expected) || names.contains(expected.toLowerCase(Locale.ROOT));
    }

    private static String localName(Element element) {
        String tag = element.getTagName();
        int idx = tag.indexOf(':');
        return idx >= 0 ? tag.substring(idx + 1) : tag;
    }
}
