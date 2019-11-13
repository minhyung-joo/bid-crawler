CREATE TABLE `dapabidinfo` (
  `공고번호` varchar(7) NOT NULL,
  `차수` int(11) DEFAULT '0',
  `개찰일시` datetime DEFAULT NULL,
  `면허명칭` varchar(255) DEFAULT '-',
  `기초예비가격` bigint(20) DEFAULT '0',
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
  `발주기관` varchar(64) DEFAULT '-',
  `계약방법` varchar(15) DEFAULT '-',
  `입찰방법` varchar(10) DEFAULT '-',
  `기초예가적용여부` varchar(10) DEFAULT '-',
  `사전심사` varchar(10) DEFAULT '-',
  `낙찰자결정방법` varchar(64) DEFAULT '-',
  `입찰서제출마감일시` datetime DEFAULT NULL,
  `입찰결과` varchar(10) DEFAULT '-',
  `사정률` varchar(16) DEFAULT '-',
  `낙찰하한율` varchar(8) DEFAULT '0',
  `완료` int(11) DEFAULT '0',
  `분류` varchar(5) DEFAULT NULL,
  `공고` int(11) DEFAULT '0',
  `공고종류` varchar(10) DEFAULT NULL,
  `공사번호` varchar(20) DEFAULT NULL,
  `기초예가공개` varchar(20) DEFAULT NULL,
  `입찰종류` varchar(2) DEFAULT NULL,
  `협상형태` varchar(20) DEFAULT NULL,
  `항목번호` varchar(3) DEFAULT NULL,
  `결과` int(11) DEFAULT '0',
  `연도` int(11) DEFAULT NULL,
  `상한` decimal(5,2) DEFAULT NULL,
  `하한` decimal(5,2) DEFAULT NULL,
  `통합참조번호` varchar(255) DEFAULT NULL,
  UNIQUE KEY `search index` (`공고번호`,`차수`,`공사번호`,`항목번호`,`연도`,`통합참조번호`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;