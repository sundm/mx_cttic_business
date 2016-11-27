package com.mx.cttic.card;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class CtticCardInfo implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7494738077574930034L;
	// 卡号
	public String cardId;
	// 卡片启用日期 YYMMDD
	public String startDate;
	// 卡片停用日期 YYMMDD
	public String endDate;
	// 卡片类型 蚌埠 平顶山
	public String type = "";
	// 可用余额
	public String useBalance;
	// 透支限额
	public String limitBalance;
	// 已透支金额
	public String usedLimitAmt;
	// 电子现金的交易记录
	List<Map<String, String>> listCardECRecords;
	// 电子钱包的交易记录
	List<String> listCardEPRecords;

	public String getCardId() {
		return cardId;
	}

	public void setCardId(String cardId) {
		this.cardId = cardId;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public String getUseBalance() {
		return useBalance;
	}

	public void setUseBalance(String useBalance) {
		this.useBalance = useBalance;
	}

	public String getLimitBalance() {
		return limitBalance;
	}

	public void setLimitBalance(String limitBalance) {
		this.limitBalance = limitBalance;
	}

	public String getUsedLimitAmt() {
		return usedLimitAmt;
	}

	public void setUsedLimitAmt(String usedLimitAmt) {
		this.usedLimitAmt = usedLimitAmt;
	}

	public List<Map<String, String>> getListCardECRecords() {
		return listCardECRecords;
	}

	public void setListCardECRecords(List<Map<String, String>> listCardECRecords) {
		this.listCardECRecords = listCardECRecords;
	}

	public List<String> getListCardEPRecords() {
		return listCardEPRecords;
	}

	public void setListCardEPRecords(List<String> listCardEPRecords) {
		this.listCardEPRecords = listCardEPRecords;
	}

	@Override
	public String toString() {
		return "CtticCardInfo [cardId=" + cardId + ", startDate=" + startDate + ", endDate=" + endDate
				+ ", useBalance=" + useBalance + ", limitBalance=" + limitBalance + ", usedLimitAmt=" + usedLimitAmt
				+ ", listCardECRecords=" + listCardECRecords + ", listCardEPRecords=" + listCardEPRecords + "]";
	}

}
