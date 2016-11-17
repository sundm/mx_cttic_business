package com.mx.cttic.card;

import java.io.Serializable;

public class CtticTradeResult implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4832288341041696011L;
	public static final int SUCCESS = 0;
	public static final int PAY_SDK_ERROR = 1;
	public static final int PAY_SDK_FAIL = 2;
	public static final int PAY_SDK_CANCEL = 3;
	public static final int LOADINIT_ERROR = 4;
	public static final int DEVICE_POWER_ON_ERROR = 5;
	public static final int DO_LOAD_ERROR = 6;
	public static final int SELECT_ERROR = 7;
	public static final int READBINARY_ERROR = 8;
	public static final int GET_BALANCE_ERROR = 9;
	public static final int CONNECT_ERROR = 10;
	public static final int TIMEOUT_ERROR = 11;
	public static final int TRANSPORT_ERROR = 12;
	public static final int APPLYORDER_ERROR = 13;
	// 交易序号
	String tradeId;
	// 交易结果
	int resultCode;
	// 错误描述，如果交易结果为失败
	String errInfo;
	// 交易金额，分为单位
	String tradeAmt;

	public CtticTradeResult() {

	}

	public CtticTradeResult(String tradeId, int resultCode, String errInfo) {
		this.tradeId = tradeId;
		this.resultCode = resultCode;
		this.errInfo = errInfo;
	}

	public String getTradeId() {
		return tradeId;
	}

	public void setTradeId(String tradeId) {
		this.tradeId = tradeId;
	}

	public int getResultCode() {
		return resultCode;
	}

	public void setResultCode(int resultCode) {
		this.resultCode = resultCode;
	}

	public String getErrInfo() {
		return errInfo;
	}

	public void setErrInfo(String errInfo) {
		this.errInfo = errInfo;
	}

	public String getTradeAmt() {
		return tradeAmt;
	}

	public void setTradeAmt(String tradeAmt) {
		this.tradeAmt = tradeAmt;
	}

	@Override
	public String toString() {
		return "CtticTradeResult [tradeId=" + tradeId + ", resultCode=" + resultCode + ", errInfo=" + errInfo
				+ ", tradeAmt=" + tradeAmt + "]";
	}

}
