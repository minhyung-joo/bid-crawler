CREATE TABLE `letsrunbidinfo` (
  `공고번호` varchar(13) NOT NULL,
  `개찰일시` datetime DEFAULT NULL,
  `입찰구분` varchar(10) DEFAULT '-',
  `예비가격기초금액` bigint(20) DEFAULT '0',
  `예정가격` bigint(20) DEFAULT '0',
  `투찰금액` bigint(20) DEFAULT '0',
  `복수1` bigint(20) DEFAULT '0',
  `복수2` bigint(20) DEFAULT '0',
  `복수3` bigint(20) DEFAULT '0',
  `복수4` bigint(20) DEFAULT '0',
  `복수5` bigint(20) DEFAULT '0',
  `복수6` bigint(20) DEFAULT '0',
  `복수7` bigint(20) DEFAULT '0',
  `복수8` bigint(20) DEFAULT '0',
  `복수9` bigint(20) DEFAULT '0',
  `복수10` bigint(20) DEFAULT '0',
  `복수11` bigint(20) DEFAULT '0',
  `복수12` bigint(20) DEFAULT '0',
  `복수13` bigint(20) DEFAULT '0',
  `복수14` bigint(20) DEFAULT '0',
  `복수15` bigint(20) DEFAULT '0',
  `복참1` int(11) DEFAULT '0',
  `복참2` int(11) DEFAULT '0',
  `복참3` int(11) DEFAULT '0',
  `복참4` int(11) DEFAULT '0',
  `복참5` int(11) DEFAULT '0',
  `복참6` int(11) DEFAULT '0',
  `복참7` int(11) DEFAULT '0',
  `복참8` int(11) DEFAULT '0',
  `복참9` int(11) DEFAULT '0',
  `복참10` int(11) DEFAULT '0',
  `복참11` int(11) DEFAULT '0',
  `복참12` int(11) DEFAULT '0',
  `복참13` int(11) DEFAULT '0',
  `복참14` int(11) DEFAULT '0',
  `복참15` int(11) DEFAULT '0',
  `참여수` int(11) DEFAULT '0',
  `입찰마감` datetime DEFAULT NULL,
  `공고상태` varchar(10) DEFAULT '-',
  `계약방법` varchar(10) DEFAULT '-',
  `입찰방식` varchar(10) DEFAULT '-',
  `낙찰자선정방법` varchar(20) DEFAULT '-',
  `예정가격방식` varchar(10) DEFAULT '-',
  `개찰상태` varchar(10) DEFAULT '-',
  `낙찰하한금액` bigint(20) DEFAULT '0',
  `낙찰하한율` varchar(8) CHARACTER SET big5 COLLATE big5_chinese_ci DEFAULT '-',
  `완료` int(11) DEFAULT '0',
  `공고` int(11) DEFAULT NULL,
  `사업장` varchar(20) DEFAULT NULL,
  `A값` decimal(20,2) DEFAULT NULL,
  PRIMARY KEY (`공고번호`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
