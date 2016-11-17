package com.mx.cttic.card;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class PbocCardInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4088047101955055171L;

	// 卡号
	public String pan;
	// 电子现金余额
	public String balance;
	// 卡片启用日期 YYMMDD
	public String startDate;
	// 卡片停用日期 YYMMDD
	public String endDate;
	// 电子现金的交易记录
	List<Map<String, String>> listCardECRecords;

	public String getPan() {
		return pan;
	}

	public void setPan(String pan) {
		this.pan = pan;
	}

	public String getBalance() {
		return balance;
	}

	public void setBalance(String balance) {
		this.balance = balance;
	}

	public List<Map<String, String>> getListCardECRecords() {
		return listCardECRecords;
	}

	public void setListCardECRecords(List<Map<String, String>> listCardECRecords) {
		this.listCardECRecords = listCardECRecords;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
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

	@Override
	public String toString() {
		return "PbocCardInfo [pan=" + pan + ", balance=" + balance
				+ ", startDate=" + startDate + ", endDate=" + endDate
				+ ", listCardECRecords=" + listCardECRecords + "]";
	}

}
