package org.bidcrawler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;

public class LetsParserTest {
    private static LetsParser letsParser;
    private static void initParser() throws SQLException, ClassNotFoundException {
        letsParser = new LetsParser("2019-11-01", "2019-11-30", null, null, null);
    }

    @BeforeAll
    static void init() {
        try {
            initParser();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail("Failed to init");
        }
    }

    @Test
    void testAnnListPageFormat() {
        try {
            letsParser.setOption("공고");
            Document doc = letsParser.getListPage(1);
            Elements annRows = doc.getElementsByTag("table").get(1).getElementsByTag("tr");
            assert(doc != null);
            assert(doc.html().length() > 0);
            HashMap<String, String> listItems = letsParser.parseAnnListRow(annRows.get(1));
            assert(listItems.get("bidno").length() > 0);
            String bidno = listItems.get("bidno");
            Document annInfoPage = letsParser.getAnnInfoPage(bidno);
            Elements captions = annInfoPage.getElementsByTag("caption");
            assert(captions.size() > 0);
            HashMap<String, String> annInfoTable = new HashMap<>();

            for (Element caption : captions) {
                Elements infos = null;
                if (caption.parent() != null) {
                    infos = caption.parent().getElementsByTag("th"); // Headers for table of details
                }

                if (caption.text().equals("입찰공고 계약전체 표")) {
                    for (Element info : infos) {
                        String key = info.text();
                        if (key.equals("사업장")) {
                            annInfoTable.put("place", info.nextElementSibling().text());
                        } else if (key.equals("전자입찰여부")) {
                            annInfoTable.put("bidMethod", info.nextElementSibling().text());
                        } else if (key.equals("입찰구분")) {
                            annInfoTable.put("workType", info.nextElementSibling().text());
                        } else if (key.equals("낙찰자결정방법")) {
                            annInfoTable.put("selectMethod", info.nextElementSibling().text());
                        }
                    }
                } else if (caption.text().equals("입찰진행순서표")) {
                    for (Element info : infos) {
                        String key = info.text();
                        if (key.equals("입찰마감")) {
                            annInfoTable.put("deadline", info.nextElementSibling().text());
                        }
                        else if (key.equals("개찰일시")) {
                            annInfoTable.put("openDate", info.nextElementSibling().text());
                        }
                    }
                } else if (caption.text().equals("예정가격정보표")) {
                    for (Element info : infos) {
                        String key = info.text();
                        if (key.equals("예정가격방식")) {
                            annInfoTable.put("priceMethod", info.nextElementSibling().text());
                        }
                        else if (key.equals("예정금액")) {
                            String expPrice = info.nextElementSibling().text();
                            expPrice = expPrice.replaceAll("[^\\d]", "");
                            if (expPrice.equals("")) {
                                expPrice = "0";
                            }

                            annInfoTable.put("expPrice", expPrice);
                        }
                    }
                }
            }

            assert(annInfoTable.containsKey("place"));
            assert(annInfoTable.containsKey("bidMethod"));
            assert(annInfoTable.containsKey("workType"));
            assert(annInfoTable.containsKey("selectMethod"));
            assert(annInfoTable.containsKey("deadline"));
            assert(annInfoTable.containsKey("openDate"));
            assert(annInfoTable.containsKey("priceMethod"));
            assert(annInfoTable.containsKey("expPrice"));
            assert(annInfoPage.html().length() > 0);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            fail("Failed due to IOException");
        }
    }

    @Test
    void testResListPageFormat() {
        try {
            letsParser.setOption("결과");
            Document doc = letsParser.getListPage(1);
            Elements resRows = doc.getElementsByTag("table").get(1).getElementsByTag("tr");
            assert(doc != null);
            assert(doc.html().length() > 0);
            HashMap<String, String> listItems = letsParser.parseResListRow(resRows.get(1));
            assert(listItems.get("bidno").length() > 0);
            String bidno = listItems.get("bidno");
            Document resultInfoPage = letsParser.getResultInfoPage(bidno);
            assert(resultInfoPage.html().length() > 0);
            Elements captions = resultInfoPage.getElementsByTag("caption");
            assert(captions.size() > 0);
            HashMap<String, String> resultInfoTable = new HashMap<>();

            for (Element caption : captions) {
                Elements infos = null;
                if (caption.parent() != null) {
                    infos = caption.parent().getElementsByTag("th"); // Headers for table of details
                }
                if (caption.text().equals("입찰개요")) {
                    for (Element info : infos) {
                        String key = info.text();
                        if (key.equals("낙찰자선정방법")) {
                            resultInfoTable.put("selectMethod", info.nextElementSibling().text());
                        }
                    }
                }
                if (caption.text().equals("예정가격정보")) {
                    for (Element info : infos) {
                        String key = info.text();
                        if (key.equals("예정가격")) {
                            String expPrice = info.nextElementSibling().text();
                            expPrice = expPrice.replaceAll("[^\\d]", "");
                            if (expPrice.equals("")) {
                                expPrice = "0";
                            }
                            resultInfoTable.put("expPrice", expPrice);
                        } else if (key.equals("예비가격기초금액")) {
                            String basePrice = info.nextElementSibling().text();
                            basePrice = basePrice.replaceAll("[^\\d]", "");
                            if (basePrice.equals("")) {
                                basePrice = "0";
                            }
                            resultInfoTable.put("basePrice", basePrice);
                        }
                    }
                }
                if (caption.text().contains("복수 예비가격별 선택상황")) {
                    Elements rows = caption.parent().getElementsByTag("tr");
                    for (Element row : rows) {
                        System.out.println(row.html());
                    }
                }
                if (caption.text().equals("제한적 최저가") || caption.text().equals("적격심사")) {
                    for (Element info : infos) {
                        String key = info.text();
                        if (key.equals("낙찰하한율")) {
                            resultInfoTable.put("rate", info.nextElementSibling().text());
                        } else if (key.equals("낙찰하한금액")) {
                            String boundPrice = info.nextElementSibling().text();
                            boundPrice = boundPrice.replaceAll("[^\\d]", "");
                            if (boundPrice.equals("")) {
                                boundPrice = "0";
                            }

                            resultInfoTable.put("boundPrice", boundPrice);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail("Res list page test failed");
        }
    }
}
