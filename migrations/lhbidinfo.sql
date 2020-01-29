CREATE TABLE `lhbidinfo` (
  `공고번호` varchar(8) NOT NULL,
  `개찰일시` datetime DEFAULT NULL,
  `업무` varchar(10) DEFAULT NULL,
  `기초금액` bigint(20) DEFAULT NULL,
  `예정금액` bigint(20) DEFAULT NULL,
  `투찰금액` bigint(20) DEFAULT NULL,
  `복수1` bigint(20) DEFAULT NULL,
  `복수2` bigint(20) DEFAULT NULL,
  `복수3` bigint(20) DEFAULT NULL,
  `복수4` bigint(20) DEFAULT NULL,
  `복수5` bigint(20) DEFAULT NULL,
  `복수6` bigint(20) DEFAULT NULL,
  `복수7` bigint(20) DEFAULT NULL,
  `복수8` bigint(20) DEFAULT NULL,
  `복수9` bigint(20) DEFAULT NULL,
  `복수10` bigint(20) DEFAULT NULL,
  `복수11` bigint(20) DEFAULT NULL,
  `복수12` bigint(20) DEFAULT NULL,
  `복수13` bigint(20) DEFAULT NULL,
  `복수14` bigint(20) DEFAULT NULL,
  `복수15` bigint(20) DEFAULT NULL,
  `복참1` bigint(20) DEFAULT NULL,
  `복참2` bigint(20) DEFAULT NULL,
  `복참3` bigint(20) DEFAULT NULL,
  `복참4` bigint(20) DEFAULT NULL,
  `복참5` bigint(20) DEFAULT NULL,
  `복참6` bigint(20) DEFAULT NULL,
  `복참7` bigint(20) DEFAULT NULL,
  `복참8` bigint(20) DEFAULT NULL,
  `복참9` bigint(20) DEFAULT NULL,
  `복참10` bigint(20) DEFAULT NULL,
  `복참11` bigint(20) DEFAULT NULL,
  `복참12` bigint(20) DEFAULT NULL,
  `복참13` bigint(20) DEFAULT NULL,
  `복참14` bigint(20) DEFAULT NULL,
  `복참15` bigint(20) DEFAULT NULL,
  `참가수` int(11) DEFAULT NULL,
  `입찰마감일자` datetime DEFAULT NULL,
  `지역본부` varchar(20) DEFAULT NULL,
  `분류` varchar(20) DEFAULT NULL,
  `계약방법` varchar(20) DEFAULT NULL,
  `공고종류` varchar(20) DEFAULT NULL,
  `업종유형` varchar(20) DEFAULT NULL,
  `입찰방법` varchar(20) DEFAULT NULL,
  `입찰방식` varchar(20) DEFAULT NULL,
  `낙찰자선정방법` varchar(20) DEFAULT NULL,
  `재입찰` varchar(20) DEFAULT NULL,
  `개찰내역` varchar(20) DEFAULT NULL,
  `선택가격1` bigint(20) DEFAULT NULL,
  `선택가격2` bigint(20) DEFAULT NULL,
  `선택가격3` bigint(20) DEFAULT NULL,
  `선택가격4` bigint(20) DEFAULT NULL,
  `완료` int(11) DEFAULT NULL,
  `공고` int(11) DEFAULT NULL,
  `기존예정가격` bigint(20) DEFAULT NULL,
  `공고현황` varchar(20) DEFAULT NULL,
  `A값` decimal(20,2) DEFAULT NULL,
  `요구면허` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`공고번호`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
