CREATE TABLE `exbidinfo` (
  `공고번호` varchar(12) NOT NULL,
  `개찰일시` datetime DEFAULT NULL,
  `업종제한사항` varchar(10) DEFAULT NULL,
  `설계금액` decimal(20,2) DEFAULT NULL,
  `예정가격` decimal(20,2) DEFAULT NULL,
  `투찰금액` decimal(20,2) DEFAULT NULL,
  `복수1` decimal(20,2) DEFAULT NULL,
  `복수2` decimal(20,2) DEFAULT NULL,
  `복수3` decimal(20,2) DEFAULT NULL,
  `복수4` decimal(20,2) DEFAULT NULL,
  `복수5` decimal(20,2) DEFAULT NULL,
  `복수6` decimal(20,2) DEFAULT NULL,
  `복수7` decimal(20,2) DEFAULT NULL,
  `복수8` decimal(20,2) DEFAULT NULL,
  `복수9` decimal(20,2) DEFAULT NULL,
  `복수10` decimal(20,2) DEFAULT NULL,
  `복수11` decimal(20,2) DEFAULT NULL,
  `복수12` decimal(20,2) DEFAULT NULL,
  `복수13` decimal(20,2) DEFAULT NULL,
  `복수14` decimal(20,2) DEFAULT NULL,
  `복수15` decimal(20,2) DEFAULT NULL,
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
  `지역` varchar(10) DEFAULT NULL,
  `공고일자` datetime DEFAULT NULL,
  `복수예가여부` varchar(10) DEFAULT NULL,
  `재입찰허용여부` varchar(20) DEFAULT NULL,
  `전자입찰여부` varchar(10) DEFAULT NULL,
  `공동수급가능여부` varchar(20) DEFAULT NULL,
  `현장설명실시여부` varchar(20) DEFAULT NULL,
  `공동수급의무여부` varchar(20) DEFAULT NULL,
  `예비가격` bigint(20) DEFAULT NULL,
  `분류` varchar(10) DEFAULT NULL,
  `공고` int(11) DEFAULT NULL,
  `완료` int(11) DEFAULT NULL,
  `계약방법` varchar(10) DEFAULT NULL,
  `공고상태` varchar(10) DEFAULT NULL,
  `결과상태` varchar(10) DEFAULT NULL,
  `중복번호` varchar(10) DEFAULT NULL,
  KEY `index` (`공고번호`,`중복번호`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;